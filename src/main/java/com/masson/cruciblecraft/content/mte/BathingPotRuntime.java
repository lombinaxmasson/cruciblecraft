package com.masson.cruciblecraft.content.mte;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.masson.cruciblecraft.heat.HotIngotProcessing;
import com.masson.cruciblecraft.machine.component.RecipeProcessor;
import com.masson.cruciblecraft.machine.processing.LayoutAwareItemStackHandler;
import com.masson.cruciblecraft.machine.processing.MachineTransaction;
import com.masson.cruciblecraft.machine.processing.ParallelRecipeOperations;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.machine.processing.SidedFluidHandler;
import com.masson.cruciblecraft.machine.processing.SidedItemHandler;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.GTRecipeQuery;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.registry.ModProcessingMachines;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * GT6 bathing pot / table: click-to-process {@code RM.Bath}, no GUI.
 *
 * <p>12 item slots (6 in / 6 out) and 1+3 tanks. Wood holds 4000 mB; stainless
 * 8000 mB. TIME energy, so progress does not wait for KU/HU.
 */
public final class BathingPotRuntime implements MachineTransaction.ResourceAccess {
    public static final int ITEM_SLOTS = 12;
    public static final int TANK_COUNT = 4;
    public static final List<Integer> INPUT_SLOTS = List.of(0, 1, 2, 3, 4, 5);
    public static final List<Integer> OUTPUT_SLOTS = List.of(6, 7, 8, 9, 10, 11);

    private final MteInPlaceSpec spec;
    private final Runnable mutation;
    private final LayoutAwareItemStackHandler items;
    private final List<FluidTank> tanks;
    private final List<ProcessingMachineSpec.TankSpec> inputTanks;
    private final List<ProcessingMachineSpec.TankSpec> outputTanks;
    private final RecipeProcessor processor = new RecipeProcessor();
    private List<ItemStack> rolledOutputs = List.of();

    public static boolean hosts(MteInPlaceSpec spec) {
        return spec.registryPath().contains("bathing_pot");
    }

    public static boolean table(MteInPlaceSpec spec) {
        return spec.registryPath().contains("bathing_pot_table");
    }

    public static boolean wooden(MteInPlaceSpec spec) {
        return spec.registryPath().contains("wooden");
    }

    public static String materialId(MteInPlaceSpec spec) {
        return wooden(spec) ? "wood" : "stainless_steel";
    }

    public static int tankCapacityMb(MteInPlaceSpec spec) {
        return wooden(spec) ? 4_000 : 8_000;
    }

    public BathingPotRuntime(MteInPlaceSpec spec, Runnable mutation) {
        this.spec = spec;
        this.mutation = mutation;
        int capacity = tankCapacityMb(spec);
        this.items = new LayoutAwareItemStackHandler(
                ITEM_SLOTS,
                (slot, stack) -> slot < 6,
                ignored -> mutation.run());
        List<FluidTank> created = new ArrayList<>();
        for (int index = 0; index < TANK_COUNT; index++) {
            created.add(new FluidTank(capacity) {
                @Override
                protected void onContentsChanged() {
                    mutation.run();
                }
            });
        }
        this.tanks = List.copyOf(created);
        this.inputTanks = List.of(new ProcessingMachineSpec.TankSpec(0, capacity));
        this.outputTanks = List.of(
                new ProcessingMachineSpec.TankSpec(1, capacity),
                new ProcessingMachineSpec.TankSpec(2, capacity),
                new ProcessingMachineSpec.TankSpec(3, capacity));
    }

    public RecipeProcessor processor() {
        return processor;
    }

    public FluidTank tank(int index) {
        return tanks.get(index);
    }

    public ItemStackHandler items() {
        return items;
    }

    public IItemHandler itemHandler(Direction front, Direction side) {
        ProcessingMachineSpec.CapabilityAccess access =
                ModProcessingMachines.BATH.sidedIo().items().resolve(front, side);
        if (access == ProcessingMachineSpec.CapabilityAccess.NONE) {
            return null;
        }
        return new SidedItemHandler(
                items,
                access == ProcessingMachineSpec.CapabilityAccess.OUTPUT
                        ? List.of() : INPUT_SLOTS,
                access == ProcessingMachineSpec.CapabilityAccess.INPUT
                        ? List.of() : OUTPUT_SLOTS,
                access,
                mutation,
                slot -> slot < 6);
    }

    public IFluidHandler fluidHandler(Direction front, Direction side) {
        ProcessingMachineSpec.CapabilityAccess access =
                ModProcessingMachines.BATH.sidedIo().fluids().resolve(front, side);
        if (access == ProcessingMachineSpec.CapabilityAccess.NONE) {
            return null;
        }
        return new SidedFluidHandler(
                tanks,
                access == ProcessingMachineSpec.CapabilityAccess.OUTPUT
                        ? List.of() : List.of(0),
                access == ProcessingMachineSpec.CapabilityAccess.INPUT
                        ? List.of() : List.of(1, 2, 3),
                access,
                mutation);
    }

    public IFluidHandler playerFluids() {
        return new SidedFluidHandler(
                tanks,
                List.of(0),
                List.of(1, 2, 3),
                ProcessingMachineSpec.CapabilityAccess.BOTH,
                mutation);
    }

    public boolean useItem(Level level, Player player, InteractionHand hand) {
        if (FluidUtil.interactWithFluidHandler(
                player, hand, playerFluids())) {
            return true;
        }
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            return false;
        }
        ItemStack remaining = held.copy();
        for (int slot : INPUT_SLOTS) {
            if (remaining.isEmpty()) {
                break;
            }
            remaining = items.insertItem(slot, remaining, false);
        }
        if (remaining.getCount() == held.getCount()) {
            return false;
        }
        if (!level.isClientSide) {
            player.setItemInHand(hand, remaining);
        }
        return true;
    }

    public boolean extract(Player player) {
        for (int slot = ITEM_SLOTS - 1; slot >= 0; slot--) {
            ItemStack taken = items.extractItem(slot, 64, false);
            if (taken.isEmpty()) {
                continue;
            }
            if (!player.addItem(taken)) {
                player.drop(taken, false);
            }
            return true;
        }
        return false;
    }

    public void serverTick(Level level, BlockPos pos) {
        if (level.isClientSide) {
            return;
        }
        if (level.isRainingAt(pos.above())) {
            tanks.get(0).fill(new FluidStack(Fluids.WATER, 10), IFluidHandler.FluidAction.EXECUTE);
        }
        List<ItemStack> offeredItems = new ArrayList<>();
        for (int slot : INPUT_SLOTS) {
            offeredItems.add(items.getStackInSlot(slot));
        }
        Optional<RecipeMap.Match> found = ModRecipeMaps.BATH.findMatch(
                new GTRecipeQuery(offeredItems, List.of(tanks.get(0).getFluid())));
        if (found.isEmpty()) {
            if (processor.reset()) {
                mutation.run();
            }
            rolledOutputs = List.of();
            return;
        }
        RecipeMap.Match match = found.get();
        GTRecipe recipe = match.recipe();
        int duration = Math.max(1, recipe.duration());
        boolean changed = processor.select(match.id().toString(), duration);
        if (changed) {
            rolledOutputs = HotIngotProcessing.prepareOutputs(
                    ParallelRecipeOperations.rollItemOutputs(
                            recipe,
                            1,
                            bound -> bound <= 0 ? 0 : level.random.nextInt(bound)),
                    level.getGameTime());
            mutation.run();
        }
        if (processor.complete()) {
            Optional<MachineTransaction> transaction = MachineTransaction.prepare(
                    recipe,
                    snapshotItems(),
                    INPUT_SLOTS,
                    OUTPUT_SLOTS,
                    snapshotFluids(),
                    inputTanks,
                    outputTanks,
                    rolledOutputs);
            if (transaction.isPresent() && transaction.get().commit(this)) {
                processor.reset();
                rolledOutputs = List.of();
                mutation.run();
            }
            return;
        }
        if (processor.advance()) {
            mutation.run();
        }
    }

    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("BathItems", items.serializeNBT(registries));
        for (int index = 0; index < tanks.size(); index++) {
            tag.put("BathTank" + index, tanks.get(index).writeToNBT(
                    registries, new CompoundTag()));
        }
        tag.putString("BathRecipe", processor.activeId());
        tag.putInt("BathProgress", processor.progress());
        tag.putInt("BathDuration", processor.duration());
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains("BathItems")) {
            items.deserializeForLayout(registries, tag.getCompound("BathItems"));
        }
        for (int index = 0; index < tanks.size(); index++) {
            String key = "BathTank" + index;
            if (tag.contains(key)) {
                tanks.get(index).readFromNBT(registries, tag.getCompound(key));
            }
        }
        processor.restore(
                tag.getString("BathRecipe"),
                tag.getInt("BathProgress"),
                tag.getInt("BathDuration"));
    }

    public void drop(Level level, BlockPos pos) {
        for (int slot = 0; slot < items.getSlots(); slot++) {
            net.minecraft.world.Containers.dropItemStack(
                    level,
                    pos.getX(),
                    pos.getY(),
                    pos.getZ(),
                    items.getStackInSlot(slot));
            items.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    @Override
    public int itemCount() {
        return ITEM_SLOTS;
    }

    @Override
    public ItemStack item(int slot) {
        return items.getStackInSlot(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.setStackInSlot(slot, stack);
    }

    @Override
    public int fluidCount() {
        return TANK_COUNT;
    }

    @Override
    public FluidStack fluid(int tank) {
        return tanks.get(tank).getFluid();
    }

    @Override
    public void setFluid(int tank, FluidStack stack) {
        tanks.get(tank).setFluid(stack);
    }

    public MteInPlaceSpec spec() {
        return spec;
    }

    private List<ItemStack> snapshotItems() {
        List<ItemStack> snapshot = new ArrayList<>(ITEM_SLOTS);
        for (int slot = 0; slot < ITEM_SLOTS; slot++) {
            snapshot.add(items.getStackInSlot(slot).copy());
        }
        return snapshot;
    }

    private List<FluidStack> snapshotFluids() {
        List<FluidStack> snapshot = new ArrayList<>(TANK_COUNT);
        for (FluidTank tank : tanks) {
            snapshot.add(tank.getFluid().copy());
        }
        return snapshot;
    }
}
