package com.masson.cruciblecraft.content.blockentity;

import java.util.Optional;

import com.masson.cruciblecraft.material.HydrocarbonRuntimePolicy;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Persistent subsurface spring.
 *
 * <p>The reserved amount remains migration/diagnostic data. Production uses an
 * independent non-depleting, rate-stable accumulator.
 */
public final class SubsurfaceFluidDepositBlockEntity extends BlockEntity {
    private static final int SCHEMA_VERSION = 2;
    private ResourceLocation material;
    private ResourceLocation replacedHost;
    private long initialAmountMb;
    private long remainingAmountMb;
    private int productionAmountMb;
    private int productionIntervalTicks;
    private int accumulationCapMb;
    private boolean ventOverflow;
    private int availableProductionMb;
    private long lastProductionGameTime = -1L;
    private long productionRevision;

    public SubsurfaceFluidDepositBlockEntity(
            BlockPos pos, BlockState state) {
        super(ModBlockEntities.SUBSURFACE_FLUID_DEPOSIT.get(), pos, state);
    }

    public void initialize(
            ResourceLocation material,
            long amountMb,
            ResourceLocation replacedHost,
            int productionAmountMb,
            int productionIntervalTicks,
            int accumulationCapMb,
            boolean ventOverflow) {
        if (material == null || replacedHost == null || amountMb <= 0L) {
            throw new IllegalArgumentException(
                    "Fluid deposit initialization requires material, host, "
                            + "and a positive reserve");
        }
        ResourceLocation migrated = HydrocarbonRuntimePolicy.migrate(material);
        HydrocarbonRuntimePolicy.Production policy =
                HydrocarbonRuntimePolicy.production(migrated);
        if (productionAmountMb != policy.amountMb()
                || productionIntervalTicks != policy.intervalTicks()
                || accumulationCapMb != policy.accumulationCapMb()
                || ventOverflow != policy.ventOverflow()) {
            throw new IllegalArgumentException(
                    "Fluid deposit production does not match hydrocarbon runtime policy");
        }
        if (this.material != null) {
            if (!this.material.equals(migrated)
                    || !this.replacedHost.equals(replacedHost)
                    || initialAmountMb != amountMb
                    || this.productionAmountMb != productionAmountMb
                    || this.productionIntervalTicks != productionIntervalTicks
                    || this.accumulationCapMb != accumulationCapMb
                    || this.ventOverflow != ventOverflow) {
                throw new IllegalStateException(
                        "Fluid deposit cannot be initialized with different data");
            }
            return;
        }
        this.material = migrated;
        this.replacedHost = replacedHost;
        this.initialAmountMb = amountMb;
        this.remainingAmountMb = amountMb;
        this.productionAmountMb = productionAmountMb;
        this.productionIntervalTicks = productionIntervalTicks;
        this.accumulationCapMb = accumulationCapMb;
        this.ventOverflow = ventOverflow;
        this.availableProductionMb = productionAmountMb;
        setChanged();
    }

    /**
     * Simulates or executes one extraction against a time-derived accumulator.
     * Simulation is pure; execution recalculates from the current revision.
     */
    public int extract(int maximumMb, long gameTime, boolean simulate) {
        if (material == null || maximumMb <= 0 || gameTime < 0L) {
            return 0;
        }
        ProductionProjection projection = projection(gameTime);
        int extracted = Math.min(maximumMb, projection.availableMb());
        if (simulate || extracted <= 0) {
            return extracted;
        }
        availableProductionMb = projection.availableMb() - extracted;
        lastProductionGameTime = projection.lastGameTime();
        productionRevision = Math.incrementExact(productionRevision);
        setChanged();
        return extracted;
    }

    public int availableProduction(long gameTime) {
        return material == null || gameTime < 0L
                ? 0
                : projection(gameTime).availableMb();
    }

    public boolean ventsOverflow() {
        return material != null && ventOverflow;
    }

    public ResourceLocation materialId() {
        if (material == null) {
            throw new IllegalStateException("Fluid deposit is not initialized");
        }
        return material;
    }

    public Optional<Snapshot> snapshot() {
        return material == null
                ? Optional.empty()
                : Optional.of(new Snapshot(
                        material,
                        replacedHost,
                        initialAmountMb,
                        remainingAmountMb,
                        productionAmountMb,
                        productionIntervalTicks,
                        accumulationCapMb,
                        ventOverflow,
                        availableProductionMb,
                        lastProductionGameTime,
                        productionRevision));
    }

    private ProductionProjection projection(long gameTime) {
        if (productionAmountMb <= 0
                || productionIntervalTicks <= 0
                || accumulationCapMb < productionAmountMb) {
            throw new IllegalStateException(
                    "Fluid deposit has invalid production state");
        }
        if (lastProductionGameTime < 0L) {
            return new ProductionProjection(
                    Math.max(availableProductionMb, productionAmountMb),
                    gameTime);
        }
        if (gameTime <= lastProductionGameTime) {
            return new ProductionProjection(
                    availableProductionMb, lastProductionGameTime);
        }
        long cycles = (gameTime - lastProductionGameTime)
                / productionIntervalTicks;
        if (cycles <= 0L) {
            return new ProductionProjection(
                    availableProductionMb, lastProductionGameTime);
        }
        long room = accumulationCapMb - (long) availableProductionMb;
        long usefulCycles = Math.min(
                cycles,
                (room + productionAmountMb - 1L) / productionAmountMb);
        long produced = Math.min(
                accumulationCapMb,
                availableProductionMb
                        + usefulCycles * productionAmountMb);
        return new ProductionProjection(
                Math.toIntExact(produced),
                lastProductionGameTime
                        + cycles * productionIntervalTicks);
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (material == null) {
            return;
        }
        tag.putString("material", material.toString());
        tag.putString("replaced_host", replacedHost.toString());
        tag.putLong("initial_amount_mb", initialAmountMb);
        tag.putLong("remaining_amount_mb", remainingAmountMb);
        tag.putInt("t11_schema_version", SCHEMA_VERSION);
        tag.putInt("production_amount_mb", productionAmountMb);
        tag.putInt("production_interval_ticks", productionIntervalTicks);
        tag.putInt("accumulation_cap_mb", accumulationCapMb);
        tag.putBoolean("vent_overflow", ventOverflow);
        tag.putInt("available_production_mb", availableProductionMb);
        tag.putLong("last_production_game_time", lastProductionGameTime);
        tag.putLong("production_revision", productionRevision);
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ResourceLocation loadedMaterial =
                ResourceLocation.tryParse(tag.getString("material"));
        ResourceLocation loadedHost =
                ResourceLocation.tryParse(tag.getString("replaced_host"));
        long loadedInitial = tag.getLong("initial_amount_mb");
        long loadedRemaining = tag.getLong("remaining_amount_mb");
        if (loadedMaterial == null
                || loadedHost == null
                || loadedInitial <= 0L
                || loadedRemaining < 0L
                || loadedRemaining > loadedInitial) {
            clear();
            return;
        }
        material = HydrocarbonRuntimePolicy.migrate(loadedMaterial);
        replacedHost = loadedHost;
        initialAmountMb = loadedInitial;
        remainingAmountMb = loadedRemaining;
        HydrocarbonRuntimePolicy.Production policy =
                HydrocarbonRuntimePolicy.production(material);
        int loadedVersion = tag.getInt("t11_schema_version");
        if (loadedVersion >= SCHEMA_VERSION) {
            productionAmountMb = tag.getInt("production_amount_mb");
            productionIntervalTicks =
                    tag.getInt("production_interval_ticks");
            accumulationCapMb = tag.getInt("accumulation_cap_mb");
            ventOverflow = tag.getBoolean("vent_overflow");
            availableProductionMb = tag.getInt("available_production_mb");
            lastProductionGameTime =
                    tag.getLong("last_production_game_time");
            productionRevision = Math.max(
                    0L, tag.getLong("production_revision"));
            if (productionAmountMb != policy.amountMb()
                    || productionIntervalTicks != policy.intervalTicks()
                    || accumulationCapMb != policy.accumulationCapMb()
                    || ventOverflow != policy.ventOverflow()
                    || availableProductionMb < 0
                    || availableProductionMb > accumulationCapMb
                    || lastProductionGameTime < -1L) {
                clear();
            }
            return;
        }
        productionAmountMb = policy.amountMb();
        productionIntervalTicks = policy.intervalTicks();
        accumulationCapMb = policy.accumulationCapMb();
        ventOverflow = policy.ventOverflow();
        availableProductionMb = productionAmountMb;
        lastProductionGameTime = -1L;
        productionRevision = 0L;
    }

    private void clear() {
        material = null;
        replacedHost = null;
        initialAmountMb = 0L;
        remainingAmountMb = 0L;
        productionAmountMb = 0;
        productionIntervalTicks = 0;
        accumulationCapMb = 0;
        ventOverflow = false;
        availableProductionMb = 0;
        lastProductionGameTime = -1L;
        productionRevision = 0L;
    }

    public record Snapshot(
            ResourceLocation material,
            ResourceLocation replacedHost,
            long initialAmountMb,
            long remainingAmountMb,
            int productionAmountMb,
            int productionIntervalTicks,
            int accumulationCapMb,
            boolean ventOverflow,
            int availableProductionMb,
            long lastProductionGameTime,
            long productionRevision) {}

    private record ProductionProjection(
            int availableMb, long lastGameTime) {}
}
