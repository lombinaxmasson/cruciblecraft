package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Forwards EU packets into the bound Logistics Core controller. */
public final class LogisticsCoreWallBlockEntity extends BlockEntity
        implements IEnergyHandler {
    private BlockPos controllerPos;

    public LogisticsCoreWallBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LOGISTICS_CORE_WALL.get(), pos, state);
    }

    public void bindController(BlockPos pos) {
        controllerPos = pos == null ? null : pos.immutable();
        setChanged();
    }

    public void unbind() {
        bindController(null);
    }

    public BlockPos controllerPos() {
        return controllerPos;
    }

    private LogisticsCoreBlockEntity controller() {
        if (level == null || controllerPos == null) {
            return null;
        }
        if (level.getBlockEntity(controllerPos)
                instanceof LogisticsCoreBlockEntity core) {
            return core;
        }
        return null;
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        return type == EnergyType.ELECTRIC && controller() != null;
    }

    @Override
    public long insert(
            EnergyType type,
            long size,
            long amount,
            Direction side,
            boolean simulate) {
        LogisticsCoreBlockEntity core = controller();
        if (core == null) {
            return 0L;
        }
        return core.insert(type, size, amount, side, simulate);
    }

    @Override
    public long stored(EnergyType type) {
        LogisticsCoreBlockEntity core = controller();
        return core == null ? 0L : core.stored(type);
    }

    @Override
    public long capacity(EnergyType type) {
        LogisticsCoreBlockEntity core = controller();
        return core == null ? 0L : core.capacity(type);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (controllerPos != null) {
            tag.putInt("cx", controllerPos.getX());
            tag.putInt("cy", controllerPos.getY());
            tag.putInt("cz", controllerPos.getZ());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("cx")) {
            controllerPos = new BlockPos(
                    tag.getInt("cx"), tag.getInt("cy"), tag.getInt("cz"));
        } else {
            controllerPos = null;
        }
    }
}
