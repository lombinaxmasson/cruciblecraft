package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.api.heat.IHeatSource;
import com.masson.cruciblecraft.content.block.FireboxBlock;
import com.masson.cruciblecraft.heat.FireboxHeatBuffer;
import com.masson.cruciblecraft.heat.FuelDefinition;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.machine.CheckpointDecisions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class FireboxBlockEntity extends BlockEntity implements IHeatSource {
    private FireboxHeatBuffer heat = new FireboxHeatBuffer();
    private boolean dirtySinceCheckpoint;

    public FireboxBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.FIREBOX.get(), pos, blockState);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            FireboxBlockEntity firebox) {
        firebox.updateLitState();
        if (CheckpointDecisions.shouldCheckpoint(
                firebox.dirtySinceCheckpoint,
                level.getGameTime(),
                20)) {
            firebox.setChanged();
            firebox.syncToClient();
            firebox.dirtySinceCheckpoint = false;
        }
    }

    public boolean addFuel(FuelDefinition fuel) {
        if (!heat.deposit(fuel)) {
            return false;
        }
        setChanged();
        dirtySinceCheckpoint = false;
        updateLitState();
        syncToClient();
        return true;
    }

    public boolean isBurning() {
        return heat.hasHeat();
    }

    /** Compatibility display value derived from buffered HU. */
    public int burnTicks() {
        return heat.equivalentTicks();
    }

    /** Compatibility display value; heat is only spent through extraction. */
    public float energyPerTick() {
        return outputRate();
    }

    public int remainingSeconds() {
        return heat.equivalentSeconds();
    }

    public String fuelId() {
        return heat.fuelId();
    }

    @Override
    public double extractHeat(double maxAmount, boolean simulate) {
        boolean effectiveSimulation = simulate || (level != null && level.isClientSide);
        boolean wasBurning = heat.hasHeat();
        double extracted = heat.extract(maxAmount, effectiveSimulation);
        if (extracted > 0.0 && !effectiveSimulation) {
            dirtySinceCheckpoint = true;
            updateLitState();
            if (wasBurning && !heat.hasHeat()) {
                setChanged();
                syncToClient();
                dirtySinceCheckpoint = false;
            }
        }
        return extracted;
    }

    @Override
    public float outputRate() {
        return heat.outputRate();
    }

    @Override
    public double storedHeat() {
        return heat.storedHeat();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("stored_hu", Tag.TAG_ANY_NUMERIC)) {
            heat = new FireboxHeatBuffer(
                    tag.getDouble("stored_hu"),
                    tag.getFloat("output_rate"),
                    tag.getString("fuel_id"));
        } else {
            int legacyTicks = tag.getInt("burn_ticks");
            float legacyRate = tag.contains("energy_per_tick", Tag.TAG_ANY_NUMERIC)
                    ? tag.getFloat("energy_per_tick")
                    : FuelDefinition.CHARCOAL.energyPerTick();
            String legacyFuel = tag.contains("fuel_id", Tag.TAG_STRING)
                    ? tag.getString("fuel_id")
                    : FuelDefinition.CHARCOAL.id();
            heat = FireboxHeatBuffer.migrateLegacy(legacyTicks, legacyRate, legacyFuel);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putDouble("stored_hu", heat.storedHeat());
        tag.putFloat("output_rate", heat.outputRate());
        tag.putString("fuel_id", heat.fuelId());
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    private void updateLitState() {
        if (level == null || getBlockState().getValue(FireboxBlock.LIT) == heat.hasHeat()) {
            return;
        }
        level.setBlock(
                worldPosition,
                getBlockState().setValue(FireboxBlock.LIT, heat.hasHeat()),
                Block.UPDATE_ALL);
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }
}
