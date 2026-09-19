package com.masson.cruciblecraft.machine;

import java.util.Locale;
import java.util.Optional;

import com.masson.cruciblecraft.machine.processing.DeviceMaterialCatalog;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.MaterialDefinition;

public final class MachineMaterialRules {
    public static final String DEFAULT_CRUCIBLE_MATERIAL =
            DeviceMaterialCatalog.require(Device.CRUCIBLE).defaultMaterial();
    /** The survival-crafted starter anvil is the GT6 Stone Anvil (meta 32025). */
    public static final String DEFAULT_ANVIL_MATERIAL =
            DeviceMaterialCatalog.require(Device.ANVIL).defaultMaterial();
    public static final String DEFAULT_HAMMER_MATERIAL =
            DeviceMaterialCatalog.require(Device.HAMMER).defaultMaterial();
    public static final double CRUCIBLE_TEMPERATURE_FACTOR = 1.25;
    public static final double KELVIN_OFFSET = 273.15;
    public static final long STONE_ANVIL_DURABILITY =
            durability(Device.ANVIL, "stone");
    public static final long BRONZE_ANVIL_DURABILITY =
            durability(Device.ANVIL, "bronze");
    /*
     * Local legacy iron sits midway through the bronze -> steel progression.
     * GT6 has no directly equivalent iron value for this migrated block.
     */
    public static final long IRON_ANVIL_DURABILITY =
            durability(Device.ANVIL, "iron");
    public static final long STEEL_ANVIL_DURABILITY =
            durability(Device.ANVIL, "steel");
    public static final int BRONZE_HAMMER_DURABILITY =
            (int) durability(Device.HAMMER, "bronze");
    public static final int IRON_HAMMER_DURABILITY =
            (int) durability(Device.HAMMER, "iron");
    public static final int STEEL_HAMMER_DURABILITY =
            (int) durability(Device.HAMMER, "steel");

    public enum Device {
        CRUCIBLE,
        ANVIL,
        HAMMER
    }

    public record MaterialResolution(
            String effectiveMaterial,
            Optional<String> quarantinedMaterial) {
        public MaterialResolution {
            quarantinedMaterial = quarantinedMaterial == null
                    ? Optional.empty()
                    : quarantinedMaterial;
        }

        public boolean quarantined() {
            return quarantinedMaterial.isPresent();
        }
    }

    public static String defaultMaterial(Device device) {
        return DeviceMaterialCatalog.require(device).defaultMaterial();
    }

    public static boolean isAllowed(Device device, String materialId) {
        if (materialId == null) {
            return false;
        }
        return DeviceMaterialCatalog.require(device).isAllowed(materialId);
    }

    /**
     * Validates trusted, internally constructed state and fails loudly on a
     * programming error. Never call this directly on NBT, item components, a
     * network payload, or any other external input; use
     * {@link #resolveExternal(Device, String)} at those boundaries.
     */
    public static String requireAllowed(Device device, String materialId) {
        DeviceMaterialCatalog.require(device).require(materialId);
        return materialId;
    }

    /**
     * Required boundary for untrusted persisted, component, or network input.
     * It never throws: invalid identity is retained as quarantine metadata
     * while calculations use the device's safe default.
     */
    public static MaterialResolution resolveExternal(
            Device device,
            String materialId) {
        if (isAllowed(device, materialId)) {
            return new MaterialResolution(materialId, Optional.empty());
        }
        return new MaterialResolution(
                defaultMaterial(device),
                Optional.ofNullable(materialId).filter(id -> !id.isBlank()));
    }

    public static int materialTier(String materialId) {
        if ("stone".equals(materialId)) {
            return 0;
        }
        return MaterialCatalog.require(materialId).tier();
    }

    public static long anvilMaxDurability(String materialId) {
        return durability(Device.ANVIL, requireAllowed(Device.ANVIL, materialId));
    }

    public static int hammerMaxDurability(String materialId) {
        return (int) durability(
                Device.HAMMER, requireAllowed(Device.HAMMER, materialId));
    }

    /**
     * GT6 wear is expressed in 10,000-point durability quanta. Stored values
     * retain their published internal scale, so one minimum-power completion
     * consumes one stored point rather than an entire stone anvil.
     */
    public static long anvilWear(long recipePower) {
        long gtWear = Math.max(10_000L, ceilDiv(Math.max(1L, recipePower), 4L));
        return ceilDiv(gtWear, 10_000L);
    }

    private static long ceilDiv(long value, long divisor) {
        return (value + divisor - 1L) / divisor;
    }

    public static int processingTier(Device device, String materialId) {
        if (device == Device.HAMMER
                && !DeviceMaterialCatalog.require(device).isAllowed(materialId)
                && ToolMaterialRules.isAllowed(
                        ToolMaterialRules.ToolKind.SMITHING_HAMMER,
                        materialId)) {
            return Math.max(0, MaterialCatalog.require(materialId).tier());
        }
        return DeviceMaterialCatalog.require(device)
                .require(materialId)
                .processingTier();
    }

    public static boolean canCrucibleProcess(String casingMaterialId, MaterialDefinition material) {
        return canCrucibleProcessTier(casingMaterialId, material.tier());
    }

    public static boolean canCrucibleProcessTier(String casingMaterialId, int materialTier) {
        return supportsTier(Device.CRUCIBLE, casingMaterialId, materialTier);
    }

    public static boolean supportsTier(Device device, String deviceMaterialId, int requiredTier) {
        return requiredTier >= 0 && requiredTier <= processingTier(device, deviceMaterialId);
    }

    public static float crucibleMaxTemperature(String casingMaterialId) {
        String safeMaterial = requireAllowed(Device.CRUCIBLE, casingMaterialId);
        return maxTemperature(MaterialCatalog.require(safeMaterial).thermal().meltingPoint());
    }

    public static float maxTemperature(double meltingPointCelsius) {
        return maxTemperature(meltingPointCelsius, CRUCIBLE_TEMPERATURE_FACTOR);
    }

    public static float maxTemperature(double meltingPointCelsius, double heatResistanceBonus) {
        if (!Double.isFinite(meltingPointCelsius) || meltingPointCelsius < 0.0) {
            throw new IllegalArgumentException("Melting point must be finite and non-negative");
        }
        if (!Double.isFinite(heatResistanceBonus) || heatResistanceBonus <= 0.0) {
            throw new IllegalArgumentException("Heat resistance bonus must be a positive finite value");
        }
        return (float) Math.floor(
                (meltingPointCelsius + KELVIN_OFFSET) * heatResistanceBonus
                        - KELVIN_OFFSET);
    }

    public static double casingMassGrams(String casingMaterialId, double volumeCm3) {
        if (!Double.isFinite(volumeCm3) || volumeCm3 < 0.0) {
            throw new IllegalArgumentException("Casing volume must be finite and non-negative");
        }
        String safeMaterial = requireAllowed(Device.CRUCIBLE, casingMaterialId);
        return MaterialCatalog.require(safeMaterial).thermal().density() * volumeCm3;
    }

    private static long durability(Device device, String materialId) {
        return DeviceMaterialCatalog.require(device)
                .require(materialId)
                .durability()
                .orElseThrow(() -> new IllegalStateException(
                        device.name().toLowerCase(Locale.ROOT)
                                + " material " + materialId
                                + " is missing durability"));
    }

    private MachineMaterialRules() {}
}
