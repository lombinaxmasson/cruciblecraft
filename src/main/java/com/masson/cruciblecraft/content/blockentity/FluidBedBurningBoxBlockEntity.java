package com.masson.cruciblecraft.content.blockentity;

import java.util.List;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.block.FluidBedBurningBoxBlock;
import com.masson.cruciblecraft.energy.EnergyEmitter;
import com.masson.cruciblecraft.energy.PerTickEnergyBudget;
import com.masson.cruciblecraft.energy.converter.BurningBoxWorldEffects;
import com.masson.cruciblecraft.energy.converter.EnergyConverterHost;
import com.masson.cruciblecraft.energy.converter.EnergyConverterProfile;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/** FM.FluidBed burning box: item + fluid recipe to HU. */
public final class FluidBedBurningBoxBlockEntity extends BlockEntity
        implements IEnergyHandler {
    private static final int FUEL_SLOT = 0;
    private static final int ASH_SLOT = 1;
    private final EnergyConverterProfile profile;
    private final FuelGeneratorEnergy energy;
    private final FluidTank input;
    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == FUEL_SLOT) {
                return ModRecipeMaps.FUELS_FLUIDBED.hasCandidate(stack);
            }
            return slot == ASH_SLOT && !stack.isEmpty();
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final PerTickEnergyBudget outputBudget = new PerTickEnergyBudget();
    private final IItemHandler hopperView = new HopperView();
    private ResourceLocation activeRecipe;
    private int progress;
    private int duration;
    private boolean burning;

    public FluidBedBurningBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_BED_BURNING_BOX.get(), pos, state);
        if (!(state.getBlock() instanceof EnergyConverterHost host)) {
            throw new IllegalArgumentException(
                    "Fluid-bed burning box requires a catalog block");
        }
        profile = host.converterProfile();
        energy = new FuelGeneratorEnergy(
                profile.outputPacket().size(),
                Math.max(
                        profile.outputPacket().size(),
                        profile.outputCapacity()));
        input = new FluidTank(
                Math.max(1, profile.inputCapacity()),
                stack -> ModRecipeMaps.FUELS_FLUIDBED.hasFluidCandidate(
                        stack.getFluid())) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            FluidBedBurningBoxBlockEntity box) {
        box.tickGeneration();
        emitHeat(level, pos, box);
        box.updateLitState();
    }

    private static void emitHeat(
            Level level, BlockPos pos, FluidBedBurningBoxBlockEntity box) {
        long rate = box.rate();
        if (box.energy.stored() >= rate) {
            BurningBoxWorldEffects.trySpreadFlame(
                    level, pos, box.profile.efficiencyBps());
        }
        if (box.burning && box.energy.stored() < rate * 2L) {
            BurningBoxWorldEffects.burnFront(
                    level, pos.relative(box.frontOrNorth()));
        }
        if (box.energy.stored() < rate) {
            return;
        }
        long offered = Math.min(rate, box.energy.stored());
        EnergyEmitter.pushToSide(
                level, pos, EnergyType.HEAT, 1L, offered, Direction.UP);
        box.energy.discardUnits(rate);
        box.setChanged();
    }

    private void tickGeneration() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (!burning) {
            if (BurningBoxWorldEffects.tryAutoIgnite(
                    level, worldPosition.relative(frontOrNorth()))) {
                burning = true;
                setChanged();
            } else {
                return;
            }
        }
        RecipeMap map = ModRecipeMaps.FUELS_FLUIDBED;
        GTRecipe recipe;
        if (activeRecipe == null) {
            if (inventory.getStackInSlot(FUEL_SLOT).isEmpty()
                    || input.isEmpty()
                    || !hasOxygen()) {
                if (energy.stored() < rate()) {
                    burning = false;
                    setChanged();
                }
                return;
            }
            RecipeMap.Match match = map.findMatch(
                            new GTRecipeQuery(
                                    List.of(inventory.getStackInSlot(FUEL_SLOT)),
                                    List.of(input.getFluid())))
                    .orElse(null);
            if (match == null) {
                return;
            }
            recipe = match.recipe();
            if (!hasAshRoom(recipe)) {
                return;
            }
            long first = generatedEnergyAtTick(recipe, 0);
            if (first > 0L && !energy.canGenerate(first)) {
                return;
            }
            consumeInputs(recipe);
            activeRecipe = match.id();
            progress = 0;
            duration = recipe.duration();
            setChanged();
        } else {
            recipe = map.entry(activeRecipe)
                    .map(RecipeMap.Entry::recipe)
                    .orElse(null);
            if (recipe == null) {
                activeRecipe = null;
                return;
            }
        }
        long generated = generatedEnergyAtTick(recipe, progress);
        if (generated > 0L && !energy.canGenerate(generated)) {
            return;
        }
        boolean completes = progress + 1 >= duration;
        if (completes && !hasAshRoom(recipe)) {
            return;
        }
        if (generated > 0L) {
            energy.generate(generated);
        }
        progress++;
        if (completes) {
            commitAsh(recipe);
            progress = 0;
            duration = 0;
            activeRecipe = null;
        }
        setChanged();
        if (energy.stored() < rate() && activeRecipe == null) {
            burning = false;
        }
    }

    private long generatedEnergyAtTick(GTRecipe recipe, int tick) {
        int efficiency = profile.efficiencyBps() == null
                ? 10_000
                : profile.efficiencyBps();
        long total = Math.abs(recipe.eut())
                * recipe.duration()
                * efficiency
                / 10_000L;
        if (tick < 0 || tick >= recipe.duration() || total <= 0L) {
            return 0L;
        }
        long base = total / recipe.duration();
        long remainder = total % recipe.duration();
        return base + (tick < remainder ? 1L : 0L);
    }

    private boolean hasAshRoom(GTRecipe recipe) {
        if (recipe.itemOutputs().isEmpty()) {
            return true;
        }
        ItemStack ash = recipe.itemOutputs().getFirst();
        return inventory.insertItem(ASH_SLOT, ash, true).isEmpty();
    }

    private void consumeInputs(GTRecipe recipe) {
        int count = recipe.itemInputCounts().isEmpty()
                ? 1
                : recipe.itemInputCounts().getFirst();
        inventory.extractItem(FUEL_SLOT, count, false);
        if (!recipe.fluidInputs().isEmpty()) {
            input.drain(
                    recipe.fluidInputs().getFirst(),
                    IFluidHandler.FluidAction.EXECUTE);
        }
    }

    private void commitAsh(GTRecipe recipe) {
        if (recipe.itemOutputs().isEmpty()) {
            return;
        }
        inventory.insertItem(
                ASH_SLOT, recipe.itemOutputs().getFirst(), false);
    }

    public boolean interact(
            Player player,
            InteractionHand hand,
            Direction hitFace,
            ItemStack stack) {
        Direction front = frontOrNorth();
        if (hitFace != front) {
            return false;
        }
        if (stack.is(Items.FLINT_AND_STEEL)) {
            burning = true;
            stack.hurtAndBreak(
                    1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));
            if (level != null) {
                level.playSound(
                        null,
                        worldPosition,
                        SoundEvents.FLINTANDSTEEL_USE,
                        SoundSource.BLOCKS,
                        1.0F,
                        1.0F);
            }
            setChanged();
            return true;
        }
        if (burning) {
            return true;
        }
        if (stack.isEmpty()) {
            ItemStack ash = inventory.extractItem(ASH_SLOT, 64, false);
            if (!ash.isEmpty()) {
                player.setItemInHand(hand, ash);
                return true;
            }
            ItemStack fuel = inventory.extractItem(FUEL_SLOT, 64, false);
            if (!fuel.isEmpty()) {
                player.setItemInHand(hand, fuel);
                return true;
            }
            return false;
        }
        ItemStack remainder = inventory.insertItem(FUEL_SLOT, stack, false);
        player.setItemInHand(hand, remainder);
        return remainder.getCount() != stack.getCount();
    }

    public boolean insertFuel(ItemStack stack) {
        ItemStack remainder = inventory.insertItem(FUEL_SLOT, stack, false);
        return remainder.getCount() < stack.getCount();
    }

    public void ignite() {
        burning = true;
        setChanged();
    }

    public long energyStored() {
        return energy.stored();
    }

    public int inputAmount() {
        return input.getFluidAmount();
    }

    public IFluidHandler fluids(Direction side) {
        if (side == null || side == Direction.UP) {
            return null;
        }
        return input;
    }

    public IItemHandler items(Direction side) {
        Direction front = frontOrNorth();
        if (side == null || side == front || side == Direction.UP) {
            return null;
        }
        return hopperView;
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            Containers.dropItemStack(
                    level,
                    pos.getX(),
                    pos.getY(),
                    pos.getZ(),
                    inventory.getStackInSlot(slot));
        }
    }

    private boolean hasOxygen() {
        if (level == null) {
            return false;
        }
        BlockState ahead = level.getBlockState(
                worldPosition.relative(frontOrNorth()));
        return ahead.isAir() && ahead.getFluidState().isEmpty();
    }

    private long rate() {
        return Math.max(1L, profile.outputPacket().maxAmountPerTick());
    }

    private void updateLitState() {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        boolean lit = burning && (activeRecipe != null || energy.stored() > 0L);
        if (state.hasProperty(FluidBedBurningBoxBlock.LIT)
                && state.getValue(FluidBedBurningBoxBlock.LIT) != lit) {
            level.setBlock(
                    worldPosition,
                    state.setValue(FluidBedBurningBoxBlock.LIT, lit),
                    Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        return type == EnergyType.HEAT && side == Direction.UP;
    }

    @Override
    public long outputSize(EnergyType type, Direction side) {
        long packet = profile.outputPacket().size();
        return handles(type, side)
                        && energy.stored() >= packet
                        && outputBudget.claim(gameTime(), 1L, rate(), true) > 0L
                ? packet
                : 0L;
    }

    @Override
    public long extract(
            EnergyType type,
            long size,
            long maximum,
            Direction side,
            boolean simulate) {
        long packet = profile.outputPacket().size();
        if (!handles(type, side)
                || size != packet
                || maximum <= 0L
                || energy.stored() < packet
                || outputBudget.claim(gameTime(), maximum, rate(), true) <= 0L) {
            return 0L;
        }
        long available = energy.extract(
                size,
                outputBudget.claim(gameTime(), maximum, rate(), true),
                true);
        if (!simulate && level != null && !level.isClientSide) {
            long claimed = outputBudget.claim(gameTime(), available, rate(), false);
            if (claimed != available
                    || energy.extract(size, available, false) != available) {
                throw new IllegalStateException(
                        "Fluid-bed output changed after simulation");
            }
            setChanged();
        }
        return available;
    }

    @Override
    public long stored(EnergyType type) {
        return type == EnergyType.HEAT ? energy.stored() : 0L;
    }

    @Override
    public long capacity(EnergyType type) {
        return type == EnergyType.HEAT ? energy.capacity() : 0L;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
        tag.put("input", input.writeToNBT(registries, new CompoundTag()));
        tag.putLong("energy", energy.stored());
        tag.putBoolean("burning", burning);
        if (activeRecipe != null) {
            tag.putString("active_recipe", activeRecipe.toString());
        }
        tag.putInt("progress", progress);
        tag.putInt("duration", duration);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        if (tag.contains("input")) {
            input.readFromNBT(registries, tag.getCompound("input"));
        }
        long stored = Math.max(
                0L, Math.min(energy.capacity(), tag.getLong("energy")));
        energy.restore(new FuelGeneratorEnergy.State(stored, stored, 0L));
        burning = tag.getBoolean("burning");
        activeRecipe = ResourceLocation.tryParse(tag.getString("active_recipe"));
        progress = Math.max(0, tag.getInt("progress"));
        duration = Math.max(0, tag.getInt("duration"));
    }

    private long gameTime() {
        if (level == null) {
            outputBudget.reset();
            return 0L;
        }
        return level.getGameTime();
    }

    private Direction frontOrNorth() {
        BlockState state = getBlockState();
        return state.hasProperty(FluidBedBurningBoxBlock.FACING)
                ? state.getValue(FluidBedBurningBoxBlock.FACING)
                : Direction.NORTH;
    }

    private final class HopperView implements IItemHandler {
        @Override
        public int getSlots() {
            return inventory.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == FUEL_SLOT
                    ? inventory.insertItem(slot, stack, simulate)
                    : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == ASH_SLOT
                    ? inventory.extractItem(slot, amount, simulate)
                    : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == FUEL_SLOT;
        }
    }
}
