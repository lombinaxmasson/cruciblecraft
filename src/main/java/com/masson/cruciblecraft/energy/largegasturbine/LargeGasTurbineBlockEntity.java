package com.masson.cruciblecraft.energy.largegasturbine;

import java.util.List;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.mte.MteInPlaceSpec;
import com.masson.cruciblecraft.energy.EnergyEmitter;
import com.masson.cruciblecraft.energy.drive.RotationEngineConversion;
import com.masson.cruciblecraft.energy.steam.SteamTurbineStructure;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.GTRecipeQuery;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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
 * GT6 {@code MultiTileEntityLargeTurbineGas}: FM.Gas → HU capacitor → RU
 * from the far wall. Controller itself does not accept HU.
 */
public final class LargeGasTurbineBlockEntity extends BlockEntity
        implements IEnergyHandler {
    private static final int OUTPUT_TANKS = 3;

    private final LargeGasTurbineCatalog.Profile profile;
    private final FluidTank input;
    private final FluidTank[] outputs = new FluidTank[OUTPUT_TANKS];
    private final IFluidHandler inputView = new InputHandler();
    private final IFluidHandler outputView = new OutputHandler();
    private long storedHu;
    private boolean formed;
    private boolean stopped;
    private GTRecipe lastRecipe;
    private boolean clientSyncPending;
    private long lastClientSyncGameTime = Long.MIN_VALUE;

    public LargeGasTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LARGE_GAS_TURBINE.get(), pos, state);
        this.profile = LargeGasTurbineCatalog.require(specOf(state).id());
        this.input = new FluidTank(
                profile.inputCapacityMb(), this::acceptsFuel) {
            @Override
            protected void onContentsChanged() {
                markPersistentMutation();
            }
        };
        for (int index = 0; index < OUTPUT_TANKS; index++) {
            outputs[index] = new FluidTank(profile.outputCapacityMb()) {
                @Override
                protected void onContentsChanged() {
                    markPersistentMutation();
                }
            };
        }
    }

    public LargeGasTurbineCatalog.Profile profile() {
        return profile;
    }

    public boolean formed() {
        return formed;
    }

    public boolean stopped() {
        return stopped;
    }

    public boolean toggleStopped() {
        stopped = !stopped;
        markPersistentMutation();
        return !stopped;
    }

    /**
     * GT6 plunger: trash exhaust 0, then 1, then 2, then the fuel tank.
     */
    public boolean trashWithPlunger() {
        for (FluidTank tank : outputs) {
            if (!tank.isEmpty()) {
                tank.setFluid(FluidStack.EMPTY);
                markPersistentMutation();
                return true;
            }
        }
        if (!input.isEmpty()) {
            input.setFluid(FluidStack.EMPTY);
            markPersistentMutation();
            return true;
        }
        return false;
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

    public int outputAmount(int tank) {
        return tank >= 0 && tank < OUTPUT_TANKS
                ? outputs[tank].getFluidAmount()
                : 0;
    }

    public boolean fillOutput(int tank, FluidStack stack) {
        if (tank < 0 || tank >= OUTPUT_TANKS) {
            return false;
        }
        int filled = outputs[tank].fill(stack, IFluidHandler.FluidAction.EXECUTE);
        if (filled > 0) {
            markPersistentMutation();
        }
        return filled == stack.getAmount();
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            LargeGasTurbineBlockEntity turbine) {
        Direction facing = state.getValue(MteInPlaceBlock.FACING);
        turbine.formed = SteamTurbineStructure.check(
                level, pos, facing, turbine.profile.wallId());
        if (turbine.formed) {
            turbine.doConversion(facing);
        }
        turbine.flushClientSync(level.getGameTime());
    }

    /**
     * GT6 {@code MultiTileEntityLargeTurbineGas.doConversion}.
     */
    private void doConversion(Direction facing) {
        if (storedHu >= profile.inputMax()) {
            long keep = storedHu;
            storedHu = profile.inputMax();
            convertHu(facing);
            storedHu = Math.max(0L, keep - profile.inputMax());
            markPersistentMutation();
            return;
        }
        if (!stopped
                && !input.isEmpty()
                && underHalf(outputs[0])
                && underHalf(outputs[1])
                && underHalf(outputs[2])) {
            RecipeMap.Match match = findFuel();
            if (match != null) {
                lastRecipe = match.recipe();
                if (consumeFuel(match.recipe())) {
                    convertHu(facing);
                    return;
                }
            }
        }
        storedHu = Math.max(0L, storedHu - profile.inputMax());
        convertHu(facing);
        markPersistentMutation();
    }

    private RecipeMap.Match findFuel() {
        GTRecipeQuery query = new GTRecipeQuery(List.of(), List.of(input.getFluid()));
        if (lastRecipe != null && lastRecipe.matches(query)) {
            return new RecipeMap.Match(
                    ResourceLocation.parse("cruciblecraft:cached"), lastRecipe);
        }
        return ModRecipeMaps.FUELS_GAS_TURBINE.findMatch(query).orElse(null);
    }

    /**
     * @return true when at least one parallel ran
     */
    private boolean consumeFuel(GTRecipe recipe) {
        if (recipe.eut() >= 0L
                || recipe.duration() <= 0
                || recipe.fluidInputs().isEmpty()) {
            return false;
        }
        FluidStack required = recipe.fluidInputs().getFirst();
        if (required.isEmpty() || required.getAmount() <= 0) {
            return false;
        }
        long huPer = Math.multiplyExact(-recipe.eut(), recipe.duration());
        if (huPer <= 0L) {
            return false;
        }
        long room = Math.max(0L, profile.inputMax() - storedHu);
        int tMax = bindInt(divup(room, huPer));
        int byFuel = input.getFluidAmount() / required.getAmount();
        int parallel = Math.min(tMax, byFuel);
        if (parallel < tMax) {
            input.setFluid(FluidStack.EMPTY);
        }
        if (parallel <= 0) {
            return false;
        }
        if (parallel == tMax) {
            FluidStack drained = input.drain(
                    required.copyWithAmount(required.getAmount() * parallel),
                    IFluidHandler.FluidAction.EXECUTE);
            if (drained.getAmount() != required.getAmount() * parallel) {
                throw new IllegalStateException(
                        "Gas turbine fuel changed after simulation");
            }
        }
        storedHu += huPer * (long) parallel;
        List<FluidStack> exhaust = recipe.fluidOutputs();
        for (int index = 0; index < exhaust.size() && index < OUTPUT_TANKS; index++) {
            FluidStack one = exhaust.get(index);
            if (one.isEmpty()) {
                continue;
            }
            FluidStack offered = one.copyWithAmount(
                    Math.multiplyExact(one.getAmount(), parallel));
            if (outputs[index].fill(offered, IFluidHandler.FluidAction.EXECUTE)
                    != offered.getAmount()) {
                storedHu = 0L;
            }
        }
        markPersistentMutation();
        return true;
    }

    private void convertHu(Direction facing) {
        if (level == null || level.isClientSide) {
            return;
        }
        long tOutput = RotationEngineConversion.units(
                storedHu, profile.inputRec(), profile.outputRec(), false);
        if (tOutput < profile.outputMin()) {
            return;
        }
        if (tOutput > profile.outputMax()) {
            tOutput = profile.outputMax();
        }
        long packets = EnergyEmitter.pushToSide(
                level,
                worldPosition.relative(facing.getOpposite(), 3),
                EnergyType.KINETIC_ROTATION,
                tOutput,
                1L,
                facing.getOpposite());
        if (packets > 0L) {
            long cost = RotationEngineConversion.units(
                    packets * tOutput,
                    profile.outputRec(),
                    profile.inputRec(),
                    true);
            storedHu = Math.max(0L, storedHu - cost);
            markPersistentMutation();
        }
    }

    public IFluidHandler fluids(Direction side) {
        if (side == null) {
            return null;
        }
        Direction facing = getBlockState().getValue(MteInPlaceBlock.FACING);
        if (side == facing) {
            return stopped ? null : inputView;
        }
        return outputView;
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        return false;
    }

    @Override
    public long stored(EnergyType type) {
        return type == EnergyType.HEAT ? storedHu : 0L;
    }

    @Override
    public long capacity(EnergyType type) {
        return type == EnergyType.HEAT ? profile.capacitor() : 0L;
    }

    private boolean acceptsFuel(FluidStack stack) {
        if (stopped || stack.isEmpty()) {
            return false;
        }
        return ModRecipeMaps.FUELS_GAS_TURBINE.hasFluidCandidate(stack.getFluid());
    }

    private static boolean underHalf(FluidTank tank) {
        return tank.getFluidAmount() < tank.getCapacity() / 2;
    }

    private static int bindInt(long value) {
        if (value <= 0L) {
            return 0;
        }
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private static long divup(long number, long divider) {
        if (divider <= 0L) {
            return 0L;
        }
        return number / divider + (number % divider == 0L ? 0L : 1L);
    }

    private static MteInPlaceSpec specOf(BlockState state) {
        if (!(state.getBlock() instanceof MteInPlaceBlock block)) {
            throw new IllegalStateException("Large gas turbine host is not MTE in-place");
        }
        return block.spec();
    }

    private void markPersistentMutation() {
        setChanged();
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
        ListTag outputList = new ListTag();
        for (FluidTank tank : outputs) {
            outputList.add(tank.writeToNBT(registries, new CompoundTag()));
        }
        tag.put("outputs", outputList);
        tag.putLong("hu", storedHu);
        tag.putBoolean("formed", formed);
        tag.putBoolean("stopped", stopped);
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("input")) {
            input.readFromNBT(registries, tag.getCompound("input"));
        }
        if (tag.contains("outputs", Tag.TAG_LIST)) {
            ListTag outputList = tag.getList("outputs", Tag.TAG_COMPOUND);
            for (int index = 0; index < OUTPUT_TANKS && index < outputList.size(); index++) {
                outputs[index].readFromNBT(registries, outputList.getCompound(index));
            }
        }
        storedHu = Math.max(0L, tag.getLong("hu"));
        formed = tag.getBoolean("formed");
        stopped = tag.getBoolean("stopped");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("hu", storedHu);
        tag.putBoolean("formed", formed);
        tag.putBoolean("stopped", stopped);
        return tag;
    }

    @Override
    public void handleUpdateTag(
            CompoundTag tag, HolderLookup.Provider registries) {
        storedHu = Math.max(0L, tag.getLong("hu"));
        formed = tag.getBoolean("formed");
        stopped = tag.getBoolean("stopped");
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
            return OUTPUT_TANKS;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank >= 0 && tank < OUTPUT_TANKS
                    ? outputs[tank].getFluid()
                    : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank >= 0 && tank < OUTPUT_TANKS
                    ? outputs[tank].getCapacity()
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
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return FluidStack.EMPTY;
            }
            for (FluidTank tank : outputs) {
                if (FluidStack.isSameFluidSameComponents(tank.getFluid(), resource)) {
                    return tank.drain(resource, action);
                }
            }
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maximum, FluidAction action) {
            if (level == null || maximum <= 0) {
                return FluidStack.EMPTY;
            }
            int start = (int) ((level.getGameTime() / 20L) % OUTPUT_TANKS);
            for (int offset = 0; offset < OUTPUT_TANKS; offset++) {
                FluidTank tank = outputs[(start + offset) % OUTPUT_TANKS];
                if (!tank.isEmpty()) {
                    return tank.drain(maximum, action);
                }
            }
            return FluidStack.EMPTY;
        }
    }
}
