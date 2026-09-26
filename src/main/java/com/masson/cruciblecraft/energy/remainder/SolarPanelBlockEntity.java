package com.masson.cruciblecraft.energy.remainder;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.energy.EnergyEmitter;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Sky-facing EU solar panel. The offer is replaced every tick. */
public final class SolarPanelBlockEntity extends BlockEntity implements IEnergyHandler {
    private final RemainderDevice device;
    private long offer;

    public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLAR_PANEL.get(), pos, state);
        if (!(state.getBlock() instanceof SolarPanelBlock block)) {
            throw new IllegalStateException("Solar panel bound to " + state.getBlock());
        }
        this.device = block.device();
    }

    public RemainderDevice device() {
        return device;
    }

    public long offer() {
        return offer;
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            SolarPanelBlockEntity panel) {
        panel.generate(level.canSeeSky(pos.above()));
    }

    public void generate(boolean sky) {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        Biome biome = level.getBiome(worldPosition).value();
        boolean rainfall = biome.hasPrecipitation()
                && biome.getModifiedClimateSettings().downfall() > 0.0F;
        offer = SolarGeneration.offer(
                device.output(),
                sky,
                level.isThundering(),
                level.isDay(),
                level.isRaining(),
                rainfall);
        boolean active = SolarGeneration.active(offer, device.output());
        if (active) {
            Direction facing = getBlockState().getValue(SolarPanelBlock.FACING);
            long accepted = EnergyEmitter.pushToSide(
                    level,
                    worldPosition,
                    EnergyType.ELECTRIC,
                    offer,
                    1L,
                    facing);
            if (accepted > 0L) {
                offer = 0L;
            }
        }
        BlockState state = getBlockState();
        if (state.getValue(SolarPanelBlock.LIT) != active) {
            level.setBlock(
                    worldPosition,
                    state.setValue(SolarPanelBlock.LIT, active),
                    Block.UPDATE_ALL);
        }
    }

    @Override
    public long outputSize(EnergyType type, Direction side) {
        if (type != EnergyType.ELECTRIC) {
            return 0L;
        }
        Direction facing = getBlockState().getValue(SolarPanelBlock.FACING);
        if (side != null && side != facing) {
            return 0L;
        }
        return offer;
    }

    @Override
    public long stored(EnergyType type) {
        return type == EnergyType.ELECTRIC ? offer : 0L;
    }

    @Override
    public long capacity(EnergyType type) {
        return type == EnergyType.ELECTRIC ? device.output() : 0L;
    }
}
