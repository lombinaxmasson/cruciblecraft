package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.air.AirOutputModel;
import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.block.BellowsBlock;
import com.masson.cruciblecraft.energy.EnergyEmitter;
import com.masson.cruciblecraft.energy.PerTickEnergyBudget;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class BellowsBlockEntity extends BlockEntity implements IEnergyHandler {
    private long activeUntil = Long.MIN_VALUE;
    private int loadedRemainingTicks;
    private final PerTickEnergyBudget outputBudget = new PerTickEnergyBudget();

    public BellowsBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.BELLOWS.get(), pos, blockState);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            BellowsBlockEntity bellows) {
        bellows.initializeDeadline();
        if (bellows.isActive()) {
            EnergyEmitter.emit(
                    level,
                    pos,
                    bellows,
                    EnergyType.AIR,
                    state.getValue(BellowsBlock.FACING));
        }
        if (state.getValue(BellowsBlock.ACTIVE) && !bellows.isActive()) {
            level.setBlock(
                    pos,
                    state.setValue(BellowsBlock.ACTIVE, false),
                    Block.UPDATE_CLIENTS);
            bellows.setChanged();
        }
    }

    public boolean startStroke() {
        if (level == null || level.isClientSide || isActive()) {
            return false;
        }
        activeUntil = level.getGameTime() + AirOutputModel.BELLOWS_STROKE_TICKS;
        loadedRemainingTicks = 0;
        if (!getBlockState().getValue(BellowsBlock.ACTIVE)) {
            level.setBlock(
                    worldPosition,
                    getBlockState().setValue(BellowsBlock.ACTIVE, true),
                    Block.UPDATE_CLIENTS);
        }
        setChanged();
        return true;
    }

    public boolean isActive() {
        initializeDeadline();
        return level != null && level.getGameTime() < activeUntil;
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        BlockState state = getBlockState();
        return type == EnergyType.AIR
                && side != null
                && state.hasProperty(BellowsBlock.FACING)
                && side == state.getValue(BellowsBlock.FACING);
    }

    @Override
    public long outputSize(EnergyType type, Direction side) {
        return handles(type, side)
                        && isActive()
                        && outputBudget.claim(
                                gameTime(),
                                AirOutputModel.BELLOWS_AIR_PER_TICK,
                                1L,
                                true) > 0L
                ? 1L
                : 0L;
    }

    @Override
    public long extract(
            EnergyType type,
            long size,
            long maxAmount,
            Direction side,
            boolean simulate) {
        if (!handles(type, side) || size != 1L || maxAmount <= 0L || !isActive()) {
            return 0L;
        }
        boolean effectiveSimulation = simulate || level == null || level.isClientSide;
        return outputBudget.claim(
                gameTime(),
                AirOutputModel.BELLOWS_AIR_PER_TICK,
                maxAmount,
                effectiveSimulation);
    }

    private long gameTime() {
        return level == null ? Long.MIN_VALUE : level.getGameTime();
    }

    private void initializeDeadline() {
        if (activeUntil == Long.MIN_VALUE && level != null) {
            activeUntil = level.getGameTime() + loadedRemainingTicks;
            loadedRemainingTicks = 0;
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadedRemainingTicks = Math.max(
                0,
                Math.min(
                        AirOutputModel.BELLOWS_STROKE_TICKS,
                        tag.getInt("stroke_ticks_remaining")));
        activeUntil = Long.MIN_VALUE;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        int remaining = loadedRemainingTicks;
        if (level != null && activeUntil != Long.MIN_VALUE) {
            remaining = (int) Math.max(
                    0L,
                    Math.min(
                            AirOutputModel.BELLOWS_STROKE_TICKS,
                            activeUntil - level.getGameTime()));
        }
        tag.putInt("stroke_ticks_remaining", remaining);
    }
}
