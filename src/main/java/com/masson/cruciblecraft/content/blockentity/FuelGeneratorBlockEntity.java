package com.masson.cruciblecraft.content.blockentity;

import java.util.ArrayList;
import java.util.List;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.block.FuelGeneratorBlock;
import com.masson.cruciblecraft.energy.EnergyEmitter;
import com.masson.cruciblecraft.energy.PerTickEnergyBudget;
import com.masson.cruciblecraft.machine.generation.FuelGeneratorSpec;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.GTRecipeQuery;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Signed-eut fuel consumer. Exhaust capacity gates operation start and
 * unaccepted electric packets remain buffered instead of escaping the tick.
 */
public final class FuelGeneratorBlockEntity extends BlockEntity
        implements IEnergyHandler {
    public static final long ENERGY_CAPACITY = 65_536L;
    private final FuelGeneratorSpec spec;
    private final FluidTank input;
    private final List<FluidTank> outputs;
    private final IFluidHandler inputView = new InputHandler();
    private final List<IFluidHandler> outputViews;
    private final PerTickEnergyBudget outputBudget =
            new PerTickEnergyBudget();
    private long energyStored;
    private long packetSize = 64L;
    private ResourceLocation activeRecipe;
    private int progress;
    private int duration;
    private String status = "idle";
    private boolean clientSyncPending;
    private long lastClientSyncGameTime = Long.MIN_VALUE;

    public FuelGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUEL_GENERATOR.get(), pos, state);
        if (!(state.getBlock() instanceof FuelGeneratorBlock block)) {
            throw new IllegalArgumentException(
                    "Fuel generator block entity requires a configured block");
        }
        spec = block.spec();
        input = new FluidTank(
                spec.inputCapacityMb(), spec::acceptsInput) {
            @Override
            protected void onContentsChanged() {
                markPersistentMutation();
            }
        };
        List<FluidTank> created = new ArrayList<>();
        for (int index = 0; index < spec.outputTanks(); index++) {
            created.add(new FluidTank(spec.outputCapacityMb()) {
                @Override
                protected void onContentsChanged() {
                    markPersistentMutation();
                }
            });
        }
        outputs = List.copyOf(created);
        List<IFluidHandler> views = new ArrayList<>();
        for (int index = 0; index < outputs.size(); index++) {
            views.add(new OutputHandler(index));
        }
        outputViews = List.copyOf(views);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            FuelGeneratorBlockEntity generator) {
        generator.tickGeneration();
        long delivered = EnergyEmitter.emit(
                level,
                pos,
                generator,
                EnergyType.ELECTRIC,
                state.getValue(FuelGeneratorBlock.FACING));
        if (delivered == 0L
                && generator.energyStored >= ENERGY_CAPACITY) {
            generator.setStatus("energy_output_blocked");
        }
        generator.flushClientSync(level.getGameTime());
    }

    private void tickGeneration() {
        if (level == null || level.isClientSide) {
            return;
        }
        GTRecipe recipe;
        if (activeRecipe == null) {
            if (input.isEmpty()) {
                reset("idle");
                return;
            }
            RecipeMap.Match match = spec.requireRecipeMap().findMatch(
                            new GTRecipeQuery(
                                    List.of(),
                                    List.of(input.getFluid())))
                    .orElse(null);
            if (match == null) {
                reset("invalid_fuel");
                return;
            }
            recipe = match.recipe();
            if (spec.validate(recipe).isPresent()) {
                reset("invalid_recipe");
                return;
            }
            if (!hasOutputRoom(recipe)) {
                setStatus("exhaust_blocked");
                return;
            }
            consumeFuel(recipe);
            activeRecipe = match.id();
            progress = 0;
            duration = recipe.duration();
            packetSize = Math.abs(recipe.eut());
            markPersistentMutation();
        } else {
            recipe = spec.requireRecipeMap().entry(activeRecipe)
                    .map(RecipeMap.Entry::recipe)
                    .orElse(null);
            if (recipe == null || spec.validate(recipe).isPresent()) {
                reset("invalid_recipe");
                return;
            }
        }
        if (!hasOutputRoom(recipe)) {
            setStatus("exhaust_blocked");
            return;
        }
        if (ENERGY_CAPACITY - energyStored < packetSize) {
            setStatus("energy_output_blocked");
            return;
        }
        energyStored += packetSize;
        progress++;
        setStatus("running");
        if (progress >= duration) {
            commitOutputs(recipe);
            progress = 0;
            activeRecipe = null;
            duration = 0;
        }
        markTickMutation();
    }

    private boolean hasOutputRoom(GTRecipe recipe) {
        for (int index = 0;
                index < recipe.fluidOutputs().size();
                index++) {
            FluidStack output = recipe.fluidOutputs().get(index);
            if (outputs.get(index).fill(
                            output, IFluidHandler.FluidAction.SIMULATE)
                    != output.getAmount()) {
                return false;
            }
        }
        return true;
    }

    private void consumeFuel(GTRecipe recipe) {
        FluidStack required = recipe.fluidInputs().getFirst();
        FluidStack drained = input.drain(
                required, IFluidHandler.FluidAction.EXECUTE);
        if (drained.getAmount() != required.getAmount()
                || !FluidStack.isSameFluidSameComponents(
                        drained, required)) {
            throw new IllegalStateException(
                    "Fuel changed after generator simulation");
        }
    }

    private void commitOutputs(GTRecipe recipe) {
        for (int index = 0;
                index < recipe.fluidOutputs().size();
                index++) {
            FluidStack output = recipe.fluidOutputs().get(index);
            if (outputs.get(index).fill(
                            output, IFluidHandler.FluidAction.EXECUTE)
                    != output.getAmount()) {
                throw new IllegalStateException(
                        "Generator exhaust changed after simulation");
            }
        }
    }

    private void reset(String nextStatus) {
        boolean changed = activeRecipe != null
                || progress != 0
                || duration != 0
                || !status.equals(nextStatus);
        activeRecipe = null;
        progress = 0;
        duration = 0;
        status = nextStatus;
        if (changed) {
            markPersistentMutation();
        }
    }

    public IFluidHandler fluids(Direction side) {
        Direction front = front();
        if (side == null || front == null) {
            return null;
        }
        if (side == Direction.UP) {
            return outputViews.getFirst();
        }
        if (side == Direction.DOWN && outputViews.size() > 1) {
            return outputViews.get(1);
        }
        return side == front ? null : inputView;
    }

    public String status() {
        return status;
    }

    public int progress() {
        return progress;
    }

    public int inputAmount() {
        return input.getFluidAmount();
    }

    public int outputAmount(int index) {
        return outputs.get(index).getFluidAmount();
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        return type == EnergyType.ELECTRIC
                && side != null
                && side == front();
    }

    @Override
    public long outputSize(EnergyType type, Direction side) {
        return handles(type, side)
                        && packetSize > 0L
                        && energyStored >= packetSize
                        && outputBudget.claim(
                                        gameTime(), 1L, 1L, true)
                                > 0L
                ? packetSize
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
                || size != packetSize
                || maximum <= 0L
                || energyStored < packetSize
                || outputBudget.claim(
                                gameTime(), 1L, 1L, true)
                        <= 0L) {
            return 0L;
        }
        if (!simulate && level != null && !level.isClientSide) {
            long claimed = outputBudget.claim(
                    gameTime(), 1L, 1L, false);
            if (claimed != 1L || energyStored < packetSize) {
                throw new IllegalStateException(
                        "Generator output changed after simulation");
            }
            energyStored -= packetSize;
            markPersistentMutation();
        }
        return 1L;
    }

    @Override
    public long stored(EnergyType type) {
        return type == EnergyType.ELECTRIC ? energyStored : 0L;
    }

    @Override
    public long capacity(EnergyType type) {
        return type == EnergyType.ELECTRIC
                ? ENERGY_CAPACITY
                : 0L;
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
        return state.hasProperty(FuelGeneratorBlock.FACING)
                ? state.getValue(FuelGeneratorBlock.FACING)
                : null;
    }

    private void setStatus(String nextStatus) {
        if (!status.equals(nextStatus)) {
            status = nextStatus;
            markTickMutation();
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
        tag.put("input", input.writeToNBT(
                registries, new CompoundTag()));
        for (int index = 0; index < outputs.size(); index++) {
            tag.put("output_" + index, outputs.get(index).writeToNBT(
                    registries, new CompoundTag()));
        }
        tag.putLong("energy", energyStored);
        tag.putLong("packet_size", packetSize);
        if (activeRecipe != null) {
            tag.putString("active_recipe", activeRecipe.toString());
        }
        tag.putInt("progress", progress);
        tag.putInt("duration", duration);
        tag.putString("status", status);
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("input")) {
            input.readFromNBT(registries, tag.getCompound("input"));
        }
        for (int index = 0; index < outputs.size(); index++) {
            if (tag.contains("output_" + index)) {
                outputs.get(index).readFromNBT(
                        registries,
                        tag.getCompound("output_" + index));
            }
        }
        energyStored = Math.max(
                0L, Math.min(ENERGY_CAPACITY, tag.getLong("energy")));
        packetSize = Math.max(
                1L, Math.min(1_024L, tag.getLong("packet_size")));
        activeRecipe = ResourceLocation.tryParse(
                tag.getString("active_recipe"));
        progress = Math.max(0, tag.getInt("progress"));
        duration = Math.max(0, tag.getInt("duration"));
        status = tag.getString("status");
        if (status.isBlank()) {
            status = "idle";
        }
    }

    @Override
    public CompoundTag getUpdateTag(
            HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("energy", energyStored);
        tag.putString("status", status);
        tag.putInt("progress", progress);
        tag.putInt("duration", duration);
        return tag;
    }

    @Override
    public void handleUpdateTag(
            CompoundTag tag, HolderLookup.Provider registries) {
        energyStored = Math.max(
                0L, Math.min(ENERGY_CAPACITY, tag.getLong("energy")));
        status = tag.getString("status");
        progress = Math.max(0, tag.getInt("progress"));
        duration = Math.max(0, tag.getInt("duration"));
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
        public FluidStack drain(
                FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maximum, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }

    private final class OutputHandler implements IFluidHandler {
        private final int outputIndex;

        private OutputHandler(int outputIndex) {
            this.outputIndex = outputIndex;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0
                    ? outputs.get(outputIndex).getFluid()
                    : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0
                    ? outputs.get(outputIndex).getCapacity()
                    : 0;
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
        public FluidStack drain(
                FluidStack resource, FluidAction action) {
            return outputs.get(outputIndex).drain(resource, action);
        }

        @Override
        public FluidStack drain(int maximum, FluidAction action) {
            return outputs.get(outputIndex).drain(maximum, action);
        }
    }
}
