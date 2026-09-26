package com.masson.cruciblecraft.energy.remainder;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.energy.EnergyEmitter;
import com.masson.cruciblecraft.machine.autotool.AutomaticHammerCatalog;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Four-slot or sixteen-slot GT6 battery box / crystal charger. */
public final class EnergyBatBoxBlockEntity extends BlockEntity
        implements IEnergyHandler, MenuProvider {
    private final RemainderDevice device;
    private final ItemStackHandler items;
    private final BatBoxEngine engine;

    public EnergyBatBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_BAT_BOX.get(), pos, state);
        if (!(state.getBlock() instanceof EnergyBatBoxBlock block)) {
            throw new IllegalStateException(
                    "Energy bat box bound to " + state.getBlock());
        }
        this.device = block.device();
        this.items = new ItemStackHandler(device.slots()) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return BatteryItemEnergy.matches(stack, device.energy());
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }

            @Override
            protected void onContentsChanged(int slot) {
                engine.markDirty();
                setChanged();
            }
        };
        this.engine = new BatBoxEngine(device, items);
    }

    public RemainderDevice device() {
        return device;
    }

    public ItemStackHandler items() {
        return items;
    }

    public BatBoxEngine engine() {
        return engine;
    }

    public void restoreBuffer(long stored) {
        engine.restore(stored, engine.mode());
        setChanged();
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            EnergyBatBoxBlockEntity box) {
        box.tickAt(level.getGameTime());
    }

    public void tickAt(long gameTime) {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        int packets = engine.beginTick(gameTime, false);
        if (packets > 0) {
            Direction facing = getBlockState().getValue(EnergyBatBoxBlock.FACING);
            long accepted = EnergyEmitter.pushToSide(
                    level,
                    worldPosition,
                    device.energy(),
                    engine.output(),
                    packets,
                    facing);
            engine.consumeEmitted(accepted);
        }
        syncCharge(level);
        setChanged();
    }

    public void dropContents() {
        Level level = getLevel();
        if (level == null) {
            return;
        }
        for (int slot = 0; slot < items.getSlots(); slot++) {
            Containers.dropItemStack(
                    level,
                    worldPosition.getX(),
                    worldPosition.getY(),
                    worldPosition.getZ(),
                    items.getStackInSlot(slot));
        }
    }

    public boolean stillValid(Player player) {
        return level != null
                && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(
                        worldPosition.getX() + 0.5,
                        worldPosition.getY() + 0.5,
                        worldPosition.getZ() + 0.5)
                        <= 64.0;
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(
            int id, Inventory inventory, Player player) {
        return new com.masson.cruciblecraft.content.menu.StorageMenu(
                ModMenus.REMAINDER_BAT_BOX.get(),
                id,
                inventory,
                items,
                items.getSlots(),
                0,
                worldPosition,
                this::stillValid);
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        if (type != device.energy()) {
            return false;
        }
        if (side == null) {
            return true;
        }
        return side != getBlockState().getValue(EnergyBatBoxBlock.FACING);
    }

    @Override
    public long outputSize(EnergyType type, Direction side) {
        if (type != device.energy()) {
            return 0L;
        }
        Direction facing = getBlockState().getValue(EnergyBatBoxBlock.FACING);
        if (side != null && side != facing) {
            return 0L;
        }
        return engine.buffer() >= device.output() ? device.output() : 0L;
    }

    @Override
    public long insert(
            EnergyType type,
            long size,
            long amount,
            Direction side,
            boolean simulate) {
        if (!handles(type, side) || amount <= 0L) {
            return 0L;
        }
        BatBoxEngine.Intake intake = engine.insert(size, amount, simulate);
        if (intake.overcharge()) {
            overcharge(size);
        } else if (!simulate && intake.consumed() > 0L) {
            setChanged();
        }
        return intake.consumed();
    }

    @Override
    public long stored(EnergyType type) {
        return type == device.energy() ? engine.buffer() : 0L;
    }

    @Override
    public long capacity(EnergyType type) {
        return type == device.energy() ? engine.networkCapacity() : 0L;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", items.serializeNBT(registries));
        tag.putLong("buffer", engine.buffer());
        tag.putInt("mode", engine.mode());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            items.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        engine.restore(tag.getLong("buffer"), tag.getInt("mode"));
    }

    private void syncCharge(Level level) {
        BlockState state = getBlockState();
        int charge = BatBoxMath.visual(
                engine.buffer(), device.input(), device.output(), device.slots());
        if (state.getValue(EnergyBatBoxBlock.CHARGE) != charge) {
            level.setBlock(
                    worldPosition,
                    state.setValue(EnergyBatBoxBlock.CHARGE, charge),
                    Block.UPDATE_ALL);
        }
    }

    private void overcharge(long size) {
        Level level = getLevel();
        if (level == null) {
            return;
        }
        BlockPos pos = worldPosition;
        float strength = AutomaticHammerCatalog.overchargeExplosionStrength(size);
        level.removeBlock(pos, false);
        if (strength >= 1.0F && level instanceof ServerLevel server) {
            server.explode(
                    null,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    strength,
                    Level.ExplosionInteraction.TNT);
        }
    }
}
