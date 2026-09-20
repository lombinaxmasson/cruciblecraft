package com.masson.cruciblecraft.content.blockentity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.block.FusionReactorBlock;
import com.masson.cruciblecraft.energy.EnergyEmitter;
import com.masson.cruciblecraft.energy.EnergyPackets;
import com.masson.cruciblecraft.fusion.FusionHatchRole;
import com.masson.cruciblecraft.fusion.FusionStructure;
import com.masson.cruciblecraft.machine.processing.MachineEnergyBuffer;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.GTRecipeQuery;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * GT6 fusion 17198. TU auto-ticks and advances work; LU fills the start-charge
 * requirement; EU is pushed from the four electric interfaces.
 */
public final class FusionReactorBlockEntity
        extends BlockEntity implements IEnergyHandler {
    public static final long TIME_MINIMUM = 1L;
    public static final long TIME_MAXIMUM = 16_384L;
    public static final long EU_PACKET = 8_192L;
    private static final int TANK_CAPACITY = 64_000;
    private static final int STRUCTURE_PERIOD = 20;
    private static final List<Direction> ENERGY_OUT_ORDER = List.of(
            Direction.NORTH,
            Direction.SOUTH,
            Direction.WEST,
            Direction.EAST);

    private final MachineEnergyBuffer time =
            new MachineEnergyBuffer(TIME_MAXIMUM, TIME_MAXIMUM);
    private final FluidTank[] inputs = {
            new FluidTank(TANK_CAPACITY), new FluidTank(TANK_CAPACITY)};
    private final FluidTank[] outputs = {
            new FluidTank(TANK_CAPACITY),
            new FluidTank(TANK_CAPACITY),
            new FluidTank(TANK_CAPACITY),
            new FluidTank(TANK_CAPACITY)};
    private final ItemStackHandler circuit = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final ItemStackHandler items = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final IFluidHandler fluids = new FusionFluids();
    private final IItemHandler inventory = new FusionItems();
    private final Set<BlockPos> boundHatches = new LinkedHashSet<>();
    private boolean forceFormed;
    private boolean formed;
    private boolean structureChecked;
    private long progress;
    private int duration;
    private long chargeRequirement;
    private ResourceLocation active;

    public FusionReactorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUSION_REACTOR.get(), pos, state);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            FusionReactorBlockEntity reactor) {
        if (!reactor.forceFormed) {
            if (!reactor.structureChecked
                    || level.getGameTime() % STRUCTURE_PERIOD == 0) {
                boolean ok = FusionStructure.check(
                        level,
                        pos,
                        state.getValue(FusionReactorBlock.FACING));
                reactor.structureChecked = true;
                reactor.updateFormed(ok);
            }
        }
        if (reactor.formed()) {
            if (reactor.time.stored() < reactor.time.capacity()) {
                reactor.time.restore(reactor.time.stored() + 1L);
            }
            reactor.tickRecipe();
        }
        reactor.syncIfNeeded();
    }

    public void forceFormedForTest() {
        forceFormed = true;
        formed = true;
        setChanged();
    }

    public boolean formed() {
        return formed || forceFormed;
    }

    public void setCircuitForTest(ItemStack stack) {
        circuit.setStackInSlot(0, stack);
    }

    public boolean fillInput(FluidStack stack) {
        for (FluidTank tank : inputs) {
            if (tank.isEmpty() || FluidStack.isSameFluidSameComponents(
                    tank.getFluid(), stack)) {
                int filled = tank.fill(stack, IFluidHandler.FluidAction.EXECUTE);
                if (filled > 0) {
                    setChanged();
                }
                return filled == stack.getAmount();
            }
        }
        return false;
    }

    public int outputAmount(int tank) {
        return tank >= 0 && tank < outputs.length
                ? outputs[tank].getFluidAmount()
                : 0;
    }

    public FluidStack outputFluid(int tank) {
        return tank >= 0 && tank < outputs.length
                ? outputs[tank].getFluid().copy()
                : FluidStack.EMPTY;
    }

    public ItemStack itemOutput() {
        return items.getStackInSlot(0).copy();
    }

    public long chargeRemaining() {
        return chargeRequirement;
    }

    public long progress() {
        return progress;
    }

    public IFluidHandler fluids(Direction side) {
        return formed() ? fluids : null;
    }

    public IItemHandler items(Direction side) {
        return formed() ? inventory : null;
    }

    public void bindHatchForTest(
            BlockPos pos, FusionHatchRole role, Direction outward) {
        bindHatch(pos, role, outward);
    }

    private void tickRecipe() {
        if (active == null && time.stored() >= TIME_MINIMUM) {
            tryStart();
        }
        if (active != null && chargeRequirement <= 0L) {
            GTRecipe recipe = activeRecipe();
            if (recipe == null) {
                resetRecipe();
            } else {
                long tu = Math.min(TIME_MAXIMUM, time.stored());
                if (tu >= TIME_MINIMUM) {
                    if (recipe.eut() < 0L) {
                        emitElectric();
                    }
                    progress += tu;
                    if (progress >= duration) {
                        if (!commitOutputs(recipe, false)) {
                            progress = duration;
                        } else {
                            resetRecipe();
                        }
                    }
                }
            }
        }
        time.restore(0L);
        setChanged();
    }

    public void skipToCompletionForTest() {
        if (active == null) {
            tryStart();
        }
        if (active == null) {
            return;
        }
        chargeRequirement = 0L;
        progress = Math.max(0L, duration - 1L);
        setChanged();
    }

    private void tryStart() {
        RecipeMap.Match match = findRecipe();
        if (match == null) {
            return;
        }
        GTRecipe recipe = match.recipe();
        if (!commitOutputs(recipe, true) || !consumeInputs(recipe, true)) {
            return;
        }
        consumeInputs(recipe, false);
        active = match.id();
        progress = 0L;
        duration = recipe.duration();
        chargeRequirement = Math.max(0L, recipe.specialValue());
        setChanged();
    }

    private RecipeMap.Match findRecipe() {
        GTRecipeQuery query = new GTRecipeQuery(itemInputs(), fluidInputs());
        return ModRecipeMaps.FUSION.findMatch(query)
                .or(() -> ModRecipeMaps.FUSION_EXTENSION.findMatch(query))
                .orElse(null);
    }

    private GTRecipe activeRecipe() {
        if (active == null) {
            return null;
        }
        return ModRecipeMaps.FUSION.entry(active)
                .or(() -> ModRecipeMaps.FUSION_EXTENSION.entry(active))
                .map(RecipeMap.Entry::recipe)
                .orElse(null);
    }

    private List<ItemStack> itemInputs() {
        ItemStack stack = circuit.getStackInSlot(0);
        return stack.isEmpty() ? List.of() : List.of(stack.copy());
    }

    private List<FluidStack> fluidInputs() {
        List<FluidStack> stacks = new ArrayList<>();
        for (FluidTank tank : inputs) {
            if (!tank.isEmpty()) {
                stacks.add(tank.getFluid().copy());
            }
        }
        return stacks;
    }

    private boolean consumeInputs(GTRecipe recipe, boolean simulate) {
        IFluidHandler.FluidAction action = simulate
                ? IFluidHandler.FluidAction.SIMULATE
                : IFluidHandler.FluidAction.EXECUTE;
        for (FluidStack required : recipe.fluidInputs()) {
            int remaining = required.getAmount();
            for (FluidTank tank : inputs) {
                if (remaining <= 0) {
                    break;
                }
                FluidStack drained = tank.drain(
                        new FluidStack(required.getFluid(), remaining),
                        action);
                remaining -= drained.getAmount();
            }
            if (remaining > 0) {
                return false;
            }
        }
        return true;
    }

    private boolean commitOutputs(GTRecipe recipe, boolean simulate) {
        for (FluidStack produced : recipe.fluidOutputs()) {
            if (fillOutput(produced, true) != produced.getAmount()) {
                return false;
            }
        }
        if (!recipe.itemOutputs().isEmpty()) {
            ItemStack leftover = items.insertItem(
                    0, recipe.itemOutputs().getFirst().copy(), true);
            if (!leftover.isEmpty()) {
                return false;
            }
        }
        if (simulate) {
            return true;
        }
        for (FluidStack produced : recipe.fluidOutputs()) {
            fillOutput(produced, false);
        }
        if (!recipe.itemOutputs().isEmpty()) {
            items.insertItem(0, recipe.itemOutputs().getFirst().copy(), false);
        }
        return true;
    }

    private int fillOutput(FluidStack produced, boolean simulate) {
        int remaining = produced.getAmount();
        for (FluidTank tank : outputs) {
            if (remaining <= 0) {
                break;
            }
            FluidStack slice = produced.copy();
            slice.setAmount(remaining);
            remaining -= tank.fill(
                    slice,
                    simulate
                            ? IFluidHandler.FluidAction.SIMULATE
                            : IFluidHandler.FluidAction.EXECUTE);
        }
        return produced.getAmount() - remaining;
    }

    private void resetRecipe() {
        active = null;
        progress = 0L;
        duration = 0;
        chargeRequirement = 0L;
    }

    private void updateFormed(boolean ok) {
        if (ok) {
            formed = true;
            bindHatches();
        } else if (formed) {
            formed = false;
            unbindHatches();
            setChanged();
        }
    }

    private void bindHatches() {
        if (level == null || forceFormed) {
            return;
        }
        Direction facing = getBlockState().getValue(FusionReactorBlock.FACING);
        Set<BlockPos> desired = new LinkedHashSet<>();
        for (FusionStructure.Hatch hatch
                : FusionStructure.hatches(worldPosition, facing)) {
            desired.add(hatch.pos());
            bindHatch(hatch.pos(), hatch.role(), hatch.outward());
        }
        for (BlockPos previous : List.copyOf(boundHatches)) {
            if (!desired.contains(previous)) {
                unbindHatch(previous);
            }
        }
    }

    private void unbindHatches() {
        for (BlockPos previous : List.copyOf(boundHatches)) {
            unbindHatch(previous);
        }
    }

    private void bindHatch(
            BlockPos pos, FusionHatchRole role, Direction outward) {
        if (level == null || !level.hasChunkAt(pos)) {
            return;
        }
        if (level.getBlockEntity(pos) instanceof FusionHullBlockEntity hull) {
            hull.bind(worldPosition, role, outward);
            boundHatches.add(pos.immutable());
        }
    }

    private void unbindHatch(BlockPos pos) {
        boundHatches.remove(pos);
        if (level != null
                && level.hasChunkAt(pos)
                && level.getBlockEntity(pos) instanceof FusionHullBlockEntity hull) {
            hull.unbind();
        }
    }

    private void emitElectric() {
        if (level == null) {
            return;
        }
        for (Direction outward : ENERGY_OUT_ORDER) {
            for (BlockPos pos : boundHatches) {
                if (!(level.getBlockEntity(pos)
                        instanceof FusionHullBlockEntity hull)
                        || hull.role() != FusionHatchRole.ENERGY_OUT
                        || hull.outward() != outward) {
                    continue;
                }
                if (EnergyEmitter.pushToSide(
                        level,
                        pos,
                        EnergyType.ELECTRIC,
                        EU_PACKET,
                        1L,
                        outward)
                        > 0L) {
                    return;
                }
            }
        }
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        if (!formed()) {
            return false;
        }
        return type == EnergyType.TIME || type == EnergyType.LU;
    }

    @Override
    public long insert(
            EnergyType type,
            long size,
            long amount,
            Direction side,
            boolean simulate) {
        if (!formed() || amount <= 0L) {
            return 0L;
        }
        long accepted = switch (type) {
            case TIME -> size < TIME_MINIMUM || size > TIME_MAXIMUM
                    ? 0L
                    : time.insert(size, amount, simulate);
            case LU -> insertLaser(size, amount, simulate);
            default -> 0L;
        };
        if (!simulate && accepted > 0L) {
            setChanged();
        }
        return accepted;
    }

    private long insertLaser(long size, long amount, boolean simulate) {
        if (chargeRequirement <= 0L
                || size < TIME_MINIMUM
                || size > TIME_MAXIMUM) {
            return 0L;
        }
        if (!simulate) {
            long absorbed = EnergyPackets.units(size, amount);
            chargeRequirement = chargeRequirement > absorbed
                    ? chargeRequirement - absorbed
                    : 0L;
        }
        return amount;
    }

    @Override
    public long outputSize(EnergyType type, Direction side) {
        return 0L;
    }

    @Override
    public long extract(
            EnergyType type,
            long size,
            long maxAmount,
            Direction side,
            boolean simulate) {
        return 0L;
    }

    @Override
    public long stored(EnergyType type) {
        return switch (type) {
            case TIME -> time.stored();
            case LU, ELECTRIC -> 0L;
            default -> 0L;
        };
    }

    @Override
    public long capacity(EnergyType type) {
        return switch (type) {
            case TIME -> time.capacity();
            case LU -> chargeRequirement;
            default -> 0L;
        };
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("gt.tu", time.stored());
        tag.putBoolean("gt.formed", formed);
        tag.putBoolean("gt.force_formed", forceFormed);
        tag.putLong("gt.progress", progress);
        tag.putInt("gt.duration", duration);
        tag.putLong("gt.charge", chargeRequirement);
        if (active != null) {
            tag.putString("gt.active", active.toString());
        }
        tag.put("gt.circuit", circuit.serializeNBT(registries));
        tag.put("gt.items", items.serializeNBT(registries));
        for (int index = 0; index < inputs.length; index++) {
            tag.put("gt.in" + index, inputs[index].writeToNBT(
                    registries, new CompoundTag()));
        }
        for (int index = 0; index < outputs.length; index++) {
            tag.put("gt.out" + index, outputs[index].writeToNBT(
                    registries, new CompoundTag()));
        }
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        time.restore(tag.getLong("gt.tu"));
        formed = tag.getBoolean("gt.formed");
        forceFormed = tag.getBoolean("gt.force_formed");
        progress = tag.getLong("gt.progress");
        duration = tag.getInt("gt.duration");
        chargeRequirement = tag.getLong("gt.charge");
        active = tag.contains("gt.active")
                ? ResourceLocation.tryParse(tag.getString("gt.active"))
                : null;
        structureChecked = false;
        if (tag.contains("gt.circuit")) {
            circuit.deserializeNBT(registries, tag.getCompound("gt.circuit"));
        }
        if (tag.contains("gt.items")) {
            items.deserializeNBT(registries, tag.getCompound("gt.items"));
        }
        for (int index = 0; index < inputs.length; index++) {
            if (tag.contains("gt.in" + index)) {
                inputs[index].readFromNBT(registries, tag.getCompound("gt.in" + index));
            }
        }
        for (int index = 0; index < outputs.length; index++) {
            if (tag.contains("gt.out" + index)) {
                outputs[index].readFromNBT(
                        registries, tag.getCompound("gt.out" + index));
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(
            CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
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

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void syncIfNeeded() {
        if (level == null || level.isClientSide) {
            return;
        }
        level.sendBlockUpdated(
                worldPosition,
                getBlockState(),
                getBlockState(),
                Block.UPDATE_CLIENTS);
    }

    private final class FusionItems implements IItemHandler {
        @Override
        public int getSlots() {
            return 2;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot == 0) {
                return circuit.getStackInSlot(0);
            }
            return slot == 1 ? items.getStackInSlot(0) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == 0
                    ? circuit.insertItem(0, stack, simulate)
                    : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == 1
                    ? items.extractItem(0, amount, simulate)
                    : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && !stack.isEmpty();
        }
    }

    private final class FusionFluids implements IFluidHandler {
        @Override
        public int getTanks() {
            return inputs.length + outputs.length;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            if (tank < inputs.length) {
                return inputs[tank].getFluid();
            }
            int output = tank - inputs.length;
            return output >= 0 && output < outputs.length
                    ? outputs[output].getFluid()
                    : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank >= 0 && tank < getTanks() ? TANK_CAPACITY : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank >= 0 && tank < inputs.length && !stack.isEmpty();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }
            for (FluidTank tank : inputs) {
                if (tank.isEmpty() || FluidStack.isSameFluidSameComponents(
                        tank.getFluid(), resource)) {
                    return tank.fill(resource, action);
                }
            }
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            for (FluidTank tank : outputs) {
                FluidStack drained = tank.drain(resource, action);
                if (!drained.isEmpty()) {
                    return drained;
                }
            }
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maximum, FluidAction action) {
            for (FluidTank tank : outputs) {
                FluidStack drained = tank.drain(maximum, action);
                if (!drained.isEmpty()) {
                    return drained;
                }
            }
            return FluidStack.EMPTY;
        }
    }
}
