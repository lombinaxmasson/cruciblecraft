package com.masson.cruciblecraft.content.blockentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.masson.cruciblecraft.api.tool.MagnifyingInspectable;
import com.masson.cruciblecraft.heat.HotIngotProcessing;
import com.masson.cruciblecraft.machine.processing.LayoutAwareItemStackHandler;
import com.masson.cruciblecraft.machine.processing.MachineTransaction;
import com.masson.cruciblecraft.machine.processing.ParallelRecipeOperations;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.machine.processing.SidedFluidHandler;
import com.masson.cruciblecraft.machine.processing.SidedItemHandler;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.GTRecipeQuery;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * GT6 {@code MultiTileEntityMixingBowl}: 6 in / 1 out items, 6 in / 2 out
 * tanks at 8000 mB, no GUI. MIXER and empty-hand mix only on the top face.
 */
public final class MixingBowlBlockEntity extends BlockEntity
        implements MachineTransaction.ResourceAccess, MagnifyingInspectable {
    public static final int ITEM_SLOTS = 7;
    public static final int TANK_COUNT = 8;
    public static final int TANK_CAPACITY = 8_000;
    public static final List<Integer> INPUT_SLOTS = List.of(0, 1, 2, 3, 4, 5);
    public static final List<Integer> OUTPUT_SLOTS = List.of(6);
    public static final List<Integer> INPUT_TANKS = List.of(0, 1, 2, 3, 4, 5);
    public static final List<Integer> OUTPUT_TANKS = List.of(6, 7);

    private final LayoutAwareItemStackHandler items;
    private final List<FluidTank> tanks;
    private final List<ProcessingMachineSpec.TankSpec> inputTankSpecs;
    private final List<ProcessingMachineSpec.TankSpec> outputTankSpecs;

    public MixingBowlBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MIXING_BOWL.get(), pos, state);
        this.items = new LayoutAwareItemStackHandler(
                ITEM_SLOTS,
                (slot, stack) -> slot < 6,
                ignored -> setChanged());
        List<FluidTank> created = new ArrayList<>();
        for (int index = 0; index < TANK_COUNT; index++) {
            created.add(new FluidTank(TANK_CAPACITY) {
                @Override
                protected void onContentsChanged() {
                    setChanged();
                }
            });
        }
        this.tanks = List.copyOf(created);
        this.inputTankSpecs = INPUT_TANKS.stream()
                .map(index -> new ProcessingMachineSpec.TankSpec(index, TANK_CAPACITY))
                .toList();
        this.outputTankSpecs = OUTPUT_TANKS.stream()
                .map(index -> new ProcessingMachineSpec.TankSpec(index, TANK_CAPACITY))
                .toList();
    }

    public IItemHandler itemHandler(Direction side) {
        return new SidedItemHandler(
                items,
                INPUT_SLOTS,
                OUTPUT_SLOTS,
                ProcessingMachineSpec.CapabilityAccess.BOTH,
                this::setChanged,
                slot -> slot < 6);
    }

    public IFluidHandler fluidHandler(Direction side) {
        return playerFluids();
    }

    public IFluidHandler playerFluids() {
        return new SidedFluidHandler(
                tanks,
                INPUT_TANKS,
                OUTPUT_TANKS,
                ProcessingMachineSpec.CapabilityAccess.BOTH,
                this::setChanged);
    }

    public boolean insertFromHand(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ItemStack remaining = stack.copy();
        for (int slot : INPUT_SLOTS) {
            if (remaining.isEmpty()) {
                break;
            }
            remaining = items.insertItem(slot, remaining, false);
        }
        if (remaining.getCount() == stack.getCount()) {
            return false;
        }
        stack.setCount(remaining.getCount());
        return true;
    }

    public boolean extract(Player player) {
        ItemStack taken = items.extractItem(6, 64, false);
        if (taken.isEmpty()) {
            for (int slot : INPUT_SLOTS) {
                taken = items.extractItem(slot, 64, false);
                if (!taken.isEmpty()) {
                    break;
                }
            }
        }
        if (taken.isEmpty()) {
            return false;
        }
        if (!player.addItem(taken)) {
            player.drop(taken, false);
        }
        return true;
    }

    public boolean mix(Player player, boolean exhaustHunger) {
        if (level == null || level.isClientSide) {
            return false;
        }
        List<ItemStack> offeredItems = new ArrayList<>();
        for (int slot : INPUT_SLOTS) {
            offeredItems.add(items.getStackInSlot(slot));
        }
        List<FluidStack> offeredFluids = new ArrayList<>();
        for (int tank : INPUT_TANKS) {
            offeredFluids.add(tanks.get(tank).getFluid());
        }
        Optional<RecipeMap.Match> found = ModRecipeMaps.MIXER.findMatch(
                new GTRecipeQuery(offeredItems, offeredFluids));
        if (found.isEmpty()) {
            return false;
        }
        GTRecipe recipe = found.orElseThrow().recipe();
        List<ItemStack> rolled = HotIngotProcessing.prepareOutputs(
                ParallelRecipeOperations.rollItemOutputs(
                        recipe,
                        1,
                        bound -> bound <= 0 ? 0 : level.random.nextInt(bound)),
                level.getGameTime());
        Optional<MachineTransaction> transaction = MachineTransaction.prepare(
                recipe,
                snapshotItems(),
                INPUT_SLOTS,
                OUTPUT_SLOTS,
                snapshotFluids(),
                inputTankSpecs,
                outputTankSpecs,
                rolled);
        if (transaction.isEmpty() || !transaction.orElseThrow().commit(this)) {
            return false;
        }
        if (exhaustHunger && player != null && !player.getAbilities().instabuild) {
            long power = Math.max(1L, recipe.eut()) * Math.max(1, recipe.duration());
            player.causeFoodExhaustion(power / 250.0F);
        }
        setChanged();
        return true;
    }

    public boolean plunger() {
        for (int tank : OUTPUT_TANKS) {
            if (dump(tank, 1_000)) {
                return true;
            }
        }
        for (int tank : INPUT_TANKS) {
            if (dump(tank, 1_000)) {
                return true;
            }
        }
        return false;
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            MixingBowlBlockEntity bowl) {
        if (level.isClientSide || level.getGameTime() % 600L != 10L) {
            return;
        }
        if (!level.isRainingAt(pos.above())) {
            return;
        }
        var biome = level.getBiome(pos).value();
        if (!biome.hasPrecipitation() || biome.getBaseTemperature() < 0.2F) {
            return;
        }
        BlockState above = level.getBlockState(pos.above());
        if (above.isFaceSturdy(level, pos.above(), Direction.DOWN)
                || above.isFaceSturdy(level, pos.above(), Direction.UP)) {
            return;
        }
        int amount = Math.max(1, 200 * (level.isThundering() ? 2 : 1));
        bowl.tanks.get(0).fill(
                new FluidStack(Fluids.WATER, amount),
                IFluidHandler.FluidAction.EXECUTE);
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int slot = 0; slot < items.getSlots(); slot++) {
            Containers.dropItemStack(
                    level,
                    pos.getX(),
                    pos.getY(),
                    pos.getZ(),
                    items.getStackInSlot(slot));
            items.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    @Override
    public List<Component> magnifyingInspect(UseOnContext context) {
        List<Component> lines = new ArrayList<>();
        for (int tank : INPUT_TANKS) {
            FluidStack fluid = tanks.get(tank).getFluid();
            if (!fluid.isEmpty()) {
                lines.add(Component.translatable(
                        "message.cruciblecraft.inspect.input_fluid",
                        fluid.getHoverName(),
                        fluid.getAmount()));
            }
        }
        for (int tank : OUTPUT_TANKS) {
            FluidStack fluid = tanks.get(tank).getFluid();
            if (!fluid.isEmpty()) {
                lines.add(Component.translatable(
                        "message.cruciblecraft.inspect.output_fluid",
                        fluid.getHoverName(),
                        fluid.getAmount()));
            }
        }
        if (lines.isEmpty()) {
            lines.add(Component.translatable(
                    "message.cruciblecraft.inspect.no_fluids"));
        }
        return List.copyOf(lines);
    }

    public LayoutAwareItemStackHandler items() {
        return items;
    }

    public List<FluidTank> tanks() {
        return tanks;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", items.serializeNBT(registries));
        for (int index = 0; index < tanks.size(); index++) {
            tag.put("Tank" + index, tanks.get(index).writeToNBT(
                    registries, new CompoundTag()));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Items")) {
            items.deserializeForLayout(registries, tag.getCompound("Items"));
        }
        for (int index = 0; index < tanks.size(); index++) {
            String key = "Tank" + index;
            if (tag.contains(key)) {
                tanks.get(index).readFromNBT(registries, tag.getCompound(key));
            }
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

    private boolean dump(int tank, int amount) {
        FluidStack drained = tanks.get(tank).drain(
                amount, IFluidHandler.FluidAction.EXECUTE);
        return !drained.isEmpty();
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
