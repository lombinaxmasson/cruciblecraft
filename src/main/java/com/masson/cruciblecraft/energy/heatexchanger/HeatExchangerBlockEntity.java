package com.masson.cruciblecraft.energy.heatexchanger;

import java.util.List;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.energy.EnergyEmitter;
import com.masson.cruciblecraft.energy.PerTickEnergyBudget;
import com.masson.cruciblecraft.machine.generation.FuelGeneratorEnergy;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.GTRecipeQuery;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * GT6 hot-fluid generator: emit HU (packet size 1, up to {@code mRate}
 * packets) from the top, then consume {@code FM.Hot} into the buffer, then
 * push exhaust out the front. Unused HU stays in the buffer.
 */
public final class HeatExchangerBlockEntity extends BlockEntity
        implements IEnergyHandler {
    private final HeatExchangerProfile profile;
    private final FuelGeneratorEnergy energy;
    private final FluidTank input;
    private final FluidTank output;
    private final IFluidHandler inputView = new InputHandler();
    private final IFluidHandler outputView = new OutputHandler();
    private final PerTickEnergyBudget outputBudget = new PerTickEnergyBudget();
    private String status = "idle";
    private boolean clientSyncPending;
    private long lastClientSyncGameTime = Long.MIN_VALUE;

    public HeatExchangerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEAT_EXCHANGER.get(), pos, state);
        if (!(state.getBlock() instanceof HeatExchangerBlock block)) {
            throw new IllegalArgumentException(
                    "Heat exchanger block entity requires a configured block");
        }
        profile = block.profile();
        energy = new FuelGeneratorEnergy(
                profile.packetSize(), profile.energyCapacity());
        input = new FluidTank(
                profile.inputCapacityMb(), this::acceptsInput) {
            @Override
            protected void onContentsChanged() {
                markPersistentMutation();
            }
        };
        output = new FluidTank(profile.outputCapacityMb()) {
            @Override
            protected void onContentsChanged() {
                markPersistentMutation();
            }
        };
    }

    public HeatExchangerProfile profile() {
        return profile;
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            HeatExchangerBlockEntity exchanger) {
        if (exchanger.energy.stored() > 0L) {
            EnergyEmitter.emit(
                    level,
                    pos,
                    exchanger,
                    EnergyType.HEAT,
                    Direction.UP);
        }
        exchanger.consumeHotFluid();
        exchanger.pushExhaust();
        exchanger.updateLitState();
        exchanger.flushClientSync(level.getGameTime());
    }

    private void consumeHotFluid() {
        if (level == null || level.isClientSide) {
            return;
        }
        boolean consumed = false;
        while (energy.stored() < profile.consumeThreshold()
                && output.getFluidAmount() < output.getCapacity()
                && !input.isEmpty()) {
            RecipeMap.Match match = ModRecipeMaps.FUELS_HOT.findMatch(
                            new GTRecipeQuery(
                                    List.of(),
                                    List.of(input.getFluid())))
                    .orElse(null);
            if (match == null) {
                if (!consumed) {
                    setStatus("invalid_fuel");
                }
                break;
            }
            GTRecipe recipe = match.recipe();
            if (recipe.fluidInputs().isEmpty()
                    || recipe.fluidOutputs().isEmpty()) {
                setStatus("invalid_recipe");
                break;
            }
            long produced = profile.huFromRecipe(recipe);
            if (produced <= 0L || !energy.canGenerate(produced)) {
                setStatus("energy_output_blocked");
                break;
            }
            FluidStack exhaust = recipe.fluidOutputs().getFirst();
            if (output.fill(exhaust, IFluidHandler.FluidAction.SIMULATE)
                    != exhaust.getAmount()) {
                setStatus("exhaust_blocked");
                break;
            }
            FluidStack required = recipe.fluidInputs().getFirst();
            FluidStack drained = input.drain(
                    required, IFluidHandler.FluidAction.EXECUTE);
            if (drained.getAmount() != required.getAmount()
                    || !FluidStack.isSameFluidSameComponents(
                            drained, required)) {
                throw new IllegalStateException(
                        "Hot fluid changed after heat-exchanger simulation");
            }
            if (output.fill(exhaust, IFluidHandler.FluidAction.EXECUTE)
                    != exhaust.getAmount()) {
                throw new IllegalStateException(
                        "Heat-exchanger exhaust changed after simulation");
            }
            energy.generate(produced);
            consumed = true;
            setStatus("running");
            markPersistentMutation();
        }
        if (!consumed && energy.stored() == 0L && input.isEmpty()) {
            setStatus("idle");
        }
    }

    private void pushExhaust() {
        if (level == null || level.isClientSide || output.isEmpty()) {
            return;
        }
        Direction front = front();
        if (front == null) {
            return;
        }
        BlockPos target = worldPosition.relative(front);
        if (!level.hasChunkAt(target)) {
            return;
        }
        IFluidHandler neighbor = level.getCapability(
                Capabilities.FluidHandler.BLOCK,
                target,
                front.getOpposite());
        if (neighbor == null) {
            return;
        }
        FluidStack moved = FluidUtil.tryFluidTransfer(
                neighbor, outputView, output.getFluidAmount(), true);
        if (!moved.isEmpty()) {
            markPersistentMutation();
        }
    }

    public IFluidHandler fluids(Direction side) {
        if (side == null || front() == null) {
            return null;
        }
        if (side == Direction.UP) {
            return null;
        }
        if (side == front()) {
            return outputView;
        }
        return inputView;
    }

    public boolean fillInput(FluidStack stack) {
        int filled = input.fill(stack, IFluidHandler.FluidAction.EXECUTE);
        if (filled > 0) {
            markPersistentMutation();
        }
        return filled == stack.getAmount();
    }

    public int inputAmount() {
        return input.getFluidAmount();
    }

    public int outputAmount() {
        return output.getFluidAmount();
    }

    public FluidStack outputFluid() {
        return output.getFluid().copy();
    }

    public long energyStored() {
        return energy.stored();
    }

    public String status() {
        return status;
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        return type == EnergyType.HEAT && side == Direction.UP;
    }

    @Override
    public long outputSize(EnergyType type, Direction side) {
        return handles(type, side)
                        && energy.stored() >= profile.packetSize()
                        && outputBudget.claim(
                                        gameTime(),
                                        1L,
                                        profile.maximumOutputPacketsPerTick(),
                                        true)
                                > 0L
                ? profile.packetSize()
                : 0L;
    }

    @Override
    public long extract(
            EnergyType type,
            long size,
            long maximum,
            Direction side,
            boolean simulate) {
        if (!handles(type, side)
                || size != profile.packetSize()
                || maximum <= 0L
                || energy.stored() < profile.packetSize()
                || outputBudget.claim(
                                gameTime(),
                                maximum,
                                profile.maximumOutputPacketsPerTick(),
                                true)
                        <= 0L) {
            return 0L;
        }
        long available = energy.extract(
                size,
                outputBudget.claim(
                        gameTime(),
                        maximum,
                        profile.maximumOutputPacketsPerTick(),
                        true),
                true);
        if (!simulate && level != null && !level.isClientSide) {
            long claimed = outputBudget.claim(
                    gameTime(),
                    available,
                    profile.maximumOutputPacketsPerTick(),
                    false);
            if (claimed != available
                    || energy.extract(size, available, false) != available) {
                throw new IllegalStateException(
                        "Heat-exchanger output changed after simulation");
            }
            markPersistentMutation();
        }
        return available;
    }

    @Override
    public long stored(EnergyType type) {
        return type == EnergyType.HEAT ? energy.stored() : 0L;
    }

    @Override
    public long capacity(EnergyType type) {
        return type == EnergyType.HEAT ? profile.energyCapacity() : 0L;
    }

    private boolean acceptsInput(FluidStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return ModRecipeMaps.FUELS_HOT.hasFluidCandidate(stack.getFluid());
    }

    private long gameTime() {
        if (level == null) {
            outputBudget.reset();
            return 0L;
        }
        return level.getGameTime();
    }

    private Direction front() {
        BlockState state = getBlockState();
        return state.hasProperty(HeatExchangerBlock.FACING)
                ? state.getValue(HeatExchangerBlock.FACING)
                : null;
    }

    private void setStatus(String nextStatus) {
        if (!status.equals(nextStatus)) {
            status = nextStatus;
            markTickMutation();
        }
    }

    private void updateLitState() {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        boolean lit = energy.stored() > 0L || "running".equals(status);
        if (state.hasProperty(HeatExchangerBlock.LIT)
                && state.getValue(HeatExchangerBlock.LIT) != lit) {
            level.setBlock(
                    worldPosition,
                    state.setValue(HeatExchangerBlock.LIT, lit),
                    Block.UPDATE_CLIENTS);
        }
    }

    private void markPersistentMutation() {
        setChanged();
        markTickMutation();
    }

    private void markTickMutation() {
        clientSyncPending = true;
    }

    private void flushClientSync(long gameTime) {
        if (!clientSyncPending
                || lastClientSyncGameTime != Long.MIN_VALUE
                        && gameTime - lastClientSyncGameTime < 20L) {
            return;
        }
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(
                    worldPosition,
                    getBlockState(),
                    getBlockState(),
                    Block.UPDATE_CLIENTS);
        }
        clientSyncPending = false;
        lastClientSyncGameTime = gameTime;
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("input", input.writeToNBT(registries, new CompoundTag()));
        tag.put("output", output.writeToNBT(registries, new CompoundTag()));
        FuelGeneratorEnergy.State energyState = energy.snapshot();
        tag.putLong("energy", energyState.stored());
        tag.putLong("energy_generated", energyState.generated());
        tag.putLong("energy_extracted", energyState.extracted());
        tag.putString("status", status);
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("input")) {
            input.readFromNBT(registries, tag.getCompound("input"));
        }
        if (tag.contains("output")) {
            output.readFromNBT(registries, tag.getCompound("output"));
        }
        long stored = Math.max(
                0L,
                Math.min(profile.energyCapacity(), tag.getLong("energy")));
        long generated = tag.contains(
                        "energy_generated", Tag.TAG_ANY_NUMERIC)
                ? Math.max(stored, tag.getLong("energy_generated"))
                : stored;
        long extracted = Math.max(0L, tag.getLong("energy_extracted"));
        energy.restore(new FuelGeneratorEnergy.State(
                stored, generated, extracted));
        status = tag.getString("status");
        if (status.isBlank()) {
            status = stored > 0L ? "running" : "idle";
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("energy", energy.stored());
        tag.putString("status", status);
        return tag;
    }

    @Override
    public void handleUpdateTag(
            CompoundTag tag, HolderLookup.Provider registries) {
        energy.restore(new FuelGeneratorEnergy.State(
                Math.max(
                        0L,
                        Math.min(
                                profile.energyCapacity(),
                                tag.getLong("energy"))),
                energy.generated(),
                energy.extracted()));
        status = tag.getString("status");
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(
            Connection connection,
            ClientboundBlockEntityDataPacket packet,
            HolderLookup.Provider registries) {
        if (packet.getTag() != null) {
            handleUpdateTag(packet.getTag(), registries);
        }
    }

    private final class InputHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? input.getFluid() : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? input.getCapacity() : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && input.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return input.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maximum, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }

    private final class OutputHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? output.getFluid() : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? output.getCapacity() : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return output.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maximum, FluidAction action) {
            return output.drain(maximum, action);
        }
    }
}
