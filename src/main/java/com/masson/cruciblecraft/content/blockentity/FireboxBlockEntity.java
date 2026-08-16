package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.block.FireboxBlock;
import com.masson.cruciblecraft.energy.EnergyEmitter;
import com.masson.cruciblecraft.energy.PerTickEnergyBudget;
import com.masson.cruciblecraft.energy.converter.EnergyConverterCatalog;
import com.masson.cruciblecraft.energy.converter.EnergyConverterProfile;
import com.masson.cruciblecraft.heat.FireboxHeatBuffer;
import com.masson.cruciblecraft.heat.FuelDefinition;
import com.masson.cruciblecraft.machine.component.CheckpointTracker;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.machine.CheckpointDecisions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class FireboxBlockEntity extends BlockEntity implements IEnergyHandler {
    private static final EnergyConverterProfile PROFILE =
            EnergyConverterCatalog.require(
                    "cruciblecraft:bronze_firebox");
    private FireboxHeatBuffer heat = new FireboxHeatBuffer();
    private final PerTickEnergyBudget outputBudget = new PerTickEnergyBudget();
    private final CheckpointTracker checkpoint = new CheckpointTracker();
    private String status = "no_fuel";

    public FireboxBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.FIREBOX.get(), pos, blockState);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            FireboxBlockEntity firebox) {
        long delivered = EnergyEmitter.emit(
                level, pos, firebox, EnergyType.HEAT, Direction.UP);
        firebox.setStatus(
                !firebox.heat.hasHeat()
                        ? "no_fuel"
                        : delivered > 0L
                                ? "running"
                                : "heat_output_blocked");
        firebox.updateLitState();
        long phaseKey = CheckpointDecisions.phaseKey(pos.getX(), pos.getY(), pos.getZ());
        if (firebox.checkpoint.shouldCheckpoint(level.getGameTime(), phaseKey, 20)) {
            firebox.setChanged();
            firebox.syncToClient();
            firebox.checkpoint.checkpointed();
            firebox.checkpoint.synced();
        }
    }

    public boolean addFuel(FuelDefinition fuel) {
        return addFuel(
                fuel,
                PROFILE.outputPacket().maxAmountPerTick(),
                PROFILE.efficiencyBps());
    }

    /**
     * Explicit controlled-source hook used by higher-tier adjacent-energy
     * GameTests; player fuel insertion always uses the fixed bronze profile.
     */
    public boolean addControlledFuel(
            FuelDefinition fuel, long outputRate) {
        return addFuel(fuel, outputRate, 10_000);
    }

    private boolean addFuel(
            FuelDefinition fuel,
            long outputRate,
            int efficiencyBps) {
        if (!heat.deposit(
                fuel,
                outputRate,
                efficiencyBps)) {
            return false;
        }
        setStatus("running");
        setChanged();
        checkpoint.checkpointed();
        updateLitState();
        syncToClient();
        checkpoint.synced();
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
    public long energyPerTick() {
        return heat.outputRate();
    }

    public int remainingSeconds() {
        return heat.equivalentSeconds();
    }

    public String fuelId() {
        return heat.fuelId();
    }

    public String status() {
        return status;
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        return type == EnergyType.HEAT && side == Direction.UP;
    }

    @Override
    public long outputSize(EnergyType type, Direction side) {
        return handles(type, side)
                        && heat.hasHeat()
                        && outputBudget.claim(gameTime(), heat.outputRate(), 1L, true) > 0L
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
        if (!handles(type, side) || size != 1L || maxAmount <= 0L) {
            return 0L;
        }
        boolean effectiveSimulation = simulate || level == null || level.isClientSide;
        long available = heat.extract(maxAmount, true);
        long offered = outputBudget.claim(
                gameTime(),
                heat.outputRate(),
                available,
                true);
        if (effectiveSimulation) {
            return offered;
        }
        long claimed = outputBudget.claim(
                gameTime(),
                heat.outputRate(),
                offered,
                false);
        long extracted = heat.extract(claimed, false);
        if (extracted > 0L && !effectiveSimulation) {
            checkpoint.markDirty();
        }
        return extracted;
    }

    private long gameTime() {
        if (level == null) {
            outputBudget.reset();
            return 0L;
        }
        return level.getGameTime();
    }

    public long outputRate() {
        return heat.outputRate();
    }

    public long storedHeat() {
        return heat.storedHeat();
    }

    @Override
    public long stored(EnergyType type) {
        return type == EnergyType.HEAT ? heat.storedHeat() : 0L;
    }

    @Override
    public long capacity(EnergyType type) {
        return type == EnergyType.HEAT
                ? FireboxHeatBuffer.capacity(Math.max(
                        PROFILE.outputPacket().maxAmountPerTick(),
                        heat.outputRate()))
                : 0L;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("stored_hu", Tag.TAG_ANY_NUMERIC)) {
            heat = new FireboxHeatBuffer(
                    Math.max(0L, tag.getLong("stored_hu")),
                    PROFILE.outputPacket().maxAmountPerTick(),
                    tag.getString("fuel_id"));
        } else {
            int legacyTicks = tag.getInt("burn_ticks");
            float legacyRate = tag.contains("energy_per_tick", Tag.TAG_ANY_NUMERIC)
                    ? tag.getFloat("energy_per_tick")
                    : FuelDefinition.CHARCOAL.energyPerTick();
            String legacyFuel = tag.contains("fuel_id", Tag.TAG_STRING)
                    ? tag.getString("fuel_id")
                    : FuelDefinition.CHARCOAL.id();
            FireboxHeatBuffer migrated = FireboxHeatBuffer.migrateLegacy(
                    legacyTicks, legacyRate, legacyFuel);
            heat = new FireboxHeatBuffer(
                    migrated.storedHeat(),
                    PROFILE.outputPacket().maxAmountPerTick(),
                    migrated.fuelId());
        }
        status = tag.contains("status", Tag.TAG_STRING)
                ? tag.getString("status")
                : heat.hasHeat() ? "running" : "no_fuel";
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("stored_hu", heat.storedHeat());
        tag.putLong("output_rate", heat.outputRate());
        tag.putString("fuel_id", heat.fuelId());
        tag.putString("status", status);
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
                Block.UPDATE_CLIENTS);
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private void setStatus(String next) {
        if (!status.equals(next)) {
            status = next;
            checkpoint.markDirty();
        }
    }
}
