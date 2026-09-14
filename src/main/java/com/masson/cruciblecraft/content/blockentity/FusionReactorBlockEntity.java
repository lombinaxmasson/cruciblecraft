package com.masson.cruciblecraft.content.blockentity;

import java.util.ArrayList;
import java.util.List;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.block.FusionReactorBlock;
import com.masson.cruciblecraft.energy.EnergyPackets;
import com.masson.cruciblecraft.fusion.FusionRecipeCatalog;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Dual TU/LU fusion host. Eighteen source-backed rows execute from the FUSION
 * map; the CC_EXTENSION neutral-matter bootstrap lives on FUSION_EXTENSION.
 */
public final class FusionReactorBlockEntity
        extends BlockEntity implements IEnergyHandler {
    public static final long TIME_MINIMUM = 1L;
    public static final long TIME_MAXIMUM = 16_384L;
    public static final long EU_PACKET = 8_192L;
    private static final int TANK_CAPACITY = 64_000;
    private static final long LU_CAPACITY =
            FusionRecipeCatalog.entries().getLast().luStart();

    private final MachineEnergyBuffer time =
            new MachineEnergyBuffer(TIME_MAXIMUM * 64L, TIME_MAXIMUM);
    private final MachineEnergyBuffer lu =
            new MachineEnergyBuffer(LU_CAPACITY, Long.MAX_VALUE / 4L);
    private final MachineEnergyBuffer eu =
            new MachineEnergyBuffer(EU_PACKET * 64L, EU_PACKET);
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
    private boolean forceFormed;
    private boolean formed;
    private int progress;
    private int duration;
    private ResourceLocationKey active;

    public FusionReactorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUSION_REACTOR.get(), pos, state);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            FusionReactorBlockEntity reactor) {
        if (!reactor.forceFormed) {
            reactor.formed = FusionStructure.check(
                    level,
                    pos,
                    state.getValue(FusionReactorBlock.FACING));
        }
        if (reactor.formed()) {
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

    public IFluidHandler fluids(Direction side) {
        return formed() ? fluids : null;
    }

    public net.neoforged.neoforge.items.IItemHandler items(Direction side) {
        return formed() ? circuit : null;
    }

    private void tickRecipe() {
        RecipeMap.Match match = findRecipe();
        if (match == null) {
            progress = 0;
            duration = 0;
            active = null;
            return;
        }
        GTRecipe recipe = match.recipe();
        if (lu.stored() < recipe.specialValue()) {
            return;
        }
        if (active == null || !active.id.equals(match.id().toString())) {
            active = new ResourceLocationKey(match.id().toString());
            progress = 0;
            duration = recipe.duration();
        }
        long eut = Math.abs(recipe.eut());
        if (recipe.eut() > 0L) {
            if (!time.canConsume(eut)) {
                return;
            }
            time.consume(eut);
        } else if (recipe.eut() < 0L) {
            eu.insert(EU_PACKET, EnergyPackets.packetsForUnits(EU_PACKET, eut), false);
        }
        progress++;
        if (progress >= duration) {
            if (!consumeInputs(recipe, true) || !commitOutputs(recipe, true)) {
                progress = duration - 1;
                return;
            }
            consumeInputs(recipe, false);
            commitOutputs(recipe, false);
            progress = 0;
            active = null;
        }
        setChanged();
    }

    public void skipToCompletionForTest() {
        RecipeMap.Match match = findRecipe();
        if (match == null) {
            return;
        }
        active = new ResourceLocationKey(match.id().toString());
        duration = match.recipe().duration();
        progress = Math.max(0, duration - 1);
        setChanged();
    }

    private RecipeMap.Match findRecipe() {
        GTRecipeQuery query = new GTRecipeQuery(itemInputs(), fluidInputs());
        return ModRecipeMaps.FUSION.findMatch(query)
                .or(() -> ModRecipeMaps.FUSION_EXTENSION.findMatch(query))
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

    @Override
    public boolean handles(EnergyType type, Direction side) {
        if (!formed()) {
            return false;
        }
        return type == EnergyType.TIME
                || type == EnergyType.LU
                || type == EnergyType.ELECTRIC;
    }

    @Override
    public long insert(
            EnergyType type,
            long size,
            long amount,
            Direction side,
            boolean simulate) {
        if (!formed()) {
            return 0L;
        }
        long accepted = switch (type) {
            case TIME -> size < TIME_MINIMUM || size > TIME_MAXIMUM
                    ? 0L
                    : time.insert(size, amount, simulate);
            case LU -> lu.insert(size, amount, simulate);
            default -> 0L;
        };
        if (!simulate && accepted > 0L) {
            setChanged();
        }
        return accepted;
    }

    @Override
    public long outputSize(EnergyType type, Direction side) {
        if (!formed() || type != EnergyType.ELECTRIC) {
            return 0L;
        }
        return eu.stored() >= EU_PACKET ? EU_PACKET : 0L;
    }

    @Override
    public long extract(
            EnergyType type,
            long size,
            long maxAmount,
            Direction side,
            boolean simulate) {
        if (!formed()
                || type != EnergyType.ELECTRIC
                || size != EU_PACKET
                || maxAmount <= 0L) {
            return 0L;
        }
        long packets = Math.min(
                maxAmount, EnergyPackets.packetsForUnits(size, eu.stored()));
        if (!simulate && packets > 0L && eu.consume(EnergyPackets.units(size, packets))) {
            setChanged();
        }
        return packets;
    }

    @Override
    public long stored(EnergyType type) {
        return switch (type) {
            case TIME -> time.stored();
            case LU -> lu.stored();
            case ELECTRIC -> eu.stored();
            default -> 0L;
        };
    }

    @Override
    public long capacity(EnergyType type) {
        return switch (type) {
            case TIME -> time.capacity();
            case LU -> lu.capacity();
            case ELECTRIC -> eu.capacity();
            default -> 0L;
        };
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("gt.tu", time.stored());
        tag.putLong("gt.lu", lu.stored());
        tag.putLong("gt.eu", eu.stored());
        tag.putBoolean("gt.formed", formed);
        tag.putBoolean("gt.force_formed", forceFormed);
        tag.putInt("gt.progress", progress);
        tag.putInt("gt.duration", duration);
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
        lu.restore(tag.getLong("gt.lu"));
        eu.restore(tag.getLong("gt.eu"));
        formed = tag.getBoolean("gt.formed");
        forceFormed = tag.getBoolean("gt.force_formed");
        progress = tag.getInt("gt.progress");
        duration = tag.getInt("gt.duration");
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

    private record ResourceLocationKey(String id) {}

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
