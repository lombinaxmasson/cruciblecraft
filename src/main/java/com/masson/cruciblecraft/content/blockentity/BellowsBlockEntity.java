package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.air.AirOutputModel;
import com.masson.cruciblecraft.air.PerTickAirLimiter;
import com.masson.cruciblecraft.api.air.IAirSource;
import com.masson.cruciblecraft.content.block.BellowsBlock;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class BellowsBlockEntity extends BlockEntity implements IAirSource {
    private long activeUntil = Long.MIN_VALUE;
    private int loadedRemainingTicks;
    private final PerTickAirLimiter extractionLimiter = new PerTickAirLimiter();

    public BellowsBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.BELLOWS.get(), pos, blockState);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            BellowsBlockEntity bellows) {
        bellows.initializeDeadline();
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
    public float outputRate() {
        return isActive() ? AirOutputModel.BELLOWS_AIR_PER_TICK : 0.0F;
    }

    @Override
    public float extractAir(float maxAmount, boolean simulate) {
        if (!Float.isFinite(maxAmount) || maxAmount <= 0.0F || !isActive()) {
            return 0.0F;
        }
        boolean effectiveSimulation = simulate || level == null || level.isClientSide;
        long gameTime = level == null ? Long.MIN_VALUE : level.getGameTime();
        return extractionLimiter.extract(
                gameTime,
                outputRate(),
                maxAmount,
                effectiveSimulation);
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
