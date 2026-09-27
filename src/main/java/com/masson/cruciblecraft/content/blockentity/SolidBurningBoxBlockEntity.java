package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.block.SolidBurningBoxBlock;
import com.masson.cruciblecraft.energy.EnergyEmitter;
import com.masson.cruciblecraft.energy.PerTickEnergyBudget;
import com.masson.cruciblecraft.energy.converter.BurningBoxWorldEffects;
import com.masson.cruciblecraft.energy.converter.EnergyConverterHost;
import com.masson.cruciblecraft.energy.converter.EnergyConverterProfile;
import com.masson.cruciblecraft.energy.converter.FurnaceFuelAdapter;
import com.masson.cruciblecraft.machine.generation.FuelGeneratorEnergy;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/** FM.Furnace solid/brick burning box: furnace fuel to HU. */
public final class SolidBurningBoxBlockEntity extends BlockEntity
        implements IEnergyHandler {
    private static final int FUEL_SLOT = 0;
    private static final int ASH_SLOT = 1;
    private final EnergyConverterProfile profile;
    private final FuelGeneratorEnergy energy;
    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == FUEL_SLOT) {
                return FurnaceFuelAdapter.isFuel(stack);
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
    private boolean burning;

    public SolidBurningBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLID_BURNING_BOX.get(), pos, state);
        if (!(state.getBlock() instanceof EnergyConverterHost host)) {
            throw new IllegalArgumentException(
                    "Solid burning box requires a catalog block");
        }
        profile = host.converterProfile();
        energy = FuelGeneratorEnergy.unbounded(profile.outputPacket().size());
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            SolidBurningBoxBlockEntity box) {
        box.tickBurning();
        emitHeat(level, pos, box);
        box.updateLitState();
    }

    private static void emitHeat(
            Level level, BlockPos pos, SolidBurningBoxBlockEntity box) {
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

    private void tickBurning() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (!burning) {
            BlockState front = level.getBlockState(
                    worldPosition.relative(frontOrNorth()));
            if (front.is(Blocks.FIRE) || front.is(Blocks.SOUL_FIRE)) {
                burning = true;
                setChanged();
            }
            return;
        }
        long rate = rate();
        if (energy.stored() < rate * 2L) {
            consumeFuel();
        }
        if (energy.stored() < rate) {
            burning = false;
            setChanged();
        }
    }

    private void consumeFuel() {
        if (!hasOxygen()) {
            return;
        }
        ItemStack fuel = inventory.getStackInSlot(FUEL_SLOT);
        long heat = FurnaceFuelAdapter.heatUnits(
                fuel, profile.efficiencyBps());
        if (heat <= 0L || !energy.canGenerate(heat)) {
            return;
        }
        ItemStack ash = FurnaceFuelAdapter.ashFor(fuel);
        if (!ash.isEmpty()
                && !inventory.insertItem(ASH_SLOT, ash, true).isEmpty()) {
            return;
        }
        inventory.extractItem(FUEL_SLOT, 1, false);
        if (!ash.isEmpty()) {
            inventory.insertItem(ASH_SLOT, ash, false);
        }
        energy.generate(heat);
        setChanged();
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
        if (stack.isEmpty()) {
            ItemStack ash = inventory.getStackInSlot(ASH_SLOT);
            if (!ash.isEmpty()) {
                player.setItemInHand(hand, inventory.extractItem(ASH_SLOT, 64, false));
                return true;
            }
            if (!burning) {
                ItemStack fuel = inventory.getStackInSlot(FUEL_SLOT);
                if (!fuel.isEmpty()) {
                    player.setItemInHand(hand, inventory.extractItem(FUEL_SLOT, 64, false));
                    return true;
                }
            }
            return false;
        }
        if (FurnaceFuelAdapter.isFuel(stack)) {
            ItemStack remainder = inventory.insertItem(FUEL_SLOT, stack, false);
            player.setItemInHand(hand, remainder);
            return remainder.getCount() != stack.getCount();
        }
        return false;
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
        return BurningBoxWorldEffects.hasFrontAir(
                level, worldPosition.relative(frontOrNorth()));
    }

    private long rate() {
        return Math.max(1L, profile.outputPacket().maxAmountPerTick());
    }

    private void updateLitState() {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        if (state.hasProperty(SolidBurningBoxBlock.LIT)
                && state.getValue(SolidBurningBoxBlock.LIT) != burning) {
            level.setBlock(
                    worldPosition,
                    state.setValue(SolidBurningBoxBlock.LIT, burning),
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
                        "Solid burning box output changed after simulation");
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
        tag.putLong("energy", energy.stored());
        tag.putBoolean("burning", burning);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        long stored = Math.max(
                0L, Math.min(energy.capacity(), tag.getLong("energy")));
        energy.restore(new FuelGeneratorEnergy.State(stored, stored, 0L));
        burning = tag.getBoolean("burning");
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
        return state.hasProperty(SolidBurningBoxBlock.FACING)
                ? state.getValue(SolidBurningBoxBlock.FACING)
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
            return slot == FUEL_SLOT && inventory.isItemValid(slot, stack);
        }
    }
}
