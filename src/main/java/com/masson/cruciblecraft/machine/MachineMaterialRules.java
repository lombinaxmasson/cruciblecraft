package com.masson.cruciblecraft.machine;

import java.util.Map;
import java.util.Set;

import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.MaterialDefinition;

public final class MachineMaterialRules {
    public static final String DEFAULT_CRUCIBLE_MATERIAL = "ceramic";
    public static final String DEFAULT_ANVIL_MATERIAL = "iron";
    public static final String DEFAULT_HAMMER_MATERIAL = "iron";
    public static final double CRUCIBLE_TEMPERATURE_FACTOR = 1.25;
    public static final double KELVIN_OFFSET = 273.15;
    public static final long STONE_ANVIL_DURABILITY = 10_000L;
    public static final long BRONZE_ANVIL_DURABILITY = 1_000_000L;
    /*
     * Local legacy iron sits midway through the bronze -> steel progression.
     * GT6 has no directly equivalent iron value for this migrated block.
     */
    public static final long IRON_ANVIL_DURABILITY = 5_000_000L;
    public static final long STEEL_ANVIL_DURABILITY = 10_000_000L;
    public static final int BRONZE_HAMMER_DURABILITY = 44_800;
    public static final int IRON_HAMMER_DURABILITY = 48_000;
    public static final int STEEL_HAMMER_DURABILITY = 51_200;

    private static final Set<String> CRUCIBLE_MATERIALS = Set.of("ceramic", "bronze", "steel");
    private static final Set<String> ANVIL_MATERIALS = Set.of("stone", "iron", "bronze", "steel");
    private static final Set<String> HAMMER_MATERIALS = Set.of("iron", "bronze", "steel");

    /*
     * This is a device capability, not the casing material tier. Ceramic is
     * intentionally capable of tier-2 charges to preserve the existing
     * copper/bronze and iron/carbon steelmaking progression.
     */
    private static final Map<String, Integer> CRUCIBLE_PROCESSING_TIERS =
            Map.of("ceramic", 2, "bronze", 2, "steel", 3);
    private static final Map<String, Integer> ANVIL_PROCESSING_TIERS =
            Map.of("stone", 0, "bronze", 1, "iron", 2, "steel", 3);
    private static final Map<String, Integer> HAMMER_PROCESSING_TIERS =
            Map.of("bronze", 1, "iron", 2, "steel", 3);

    public enum Device {
        CRUCIBLE,
        ANVIL,
        HAMMER
    }

    public static String defaultMaterial(Device device) {
        return switch (device) {
            case CRUCIBLE -> DEFAULT_CRUCIBLE_MATERIAL;
            case ANVIL -> DEFAULT_ANVIL_MATERIAL;
            case HAMMER -> DEFAULT_HAMMER_MATERIAL;
        };
    }

    public static boolean isAllowed(Device device, String materialId) {
        if (materialId == null) {
            return false;
        }
        return switch (device) {
            case CRUCIBLE -> CRUCIBLE_MATERIALS.contains(materialId);
            case ANVIL -> ANVIL_MATERIALS.contains(materialId);
            case HAMMER -> HAMMER_MATERIALS.contains(materialId);
        };
    }

    public static String sanitize(Device device, String materialId) {
        return isAllowed(device, materialId) ? materialId : defaultMaterial(device);
    }

    public static int materialTier(String materialId) {
        if ("stone".equals(materialId)) {
            return 0;
        }
        return MaterialCatalog.require(materialId).tier();
    }

    public static long anvilMaxDurability(String materialId) {
        return switch (sanitize(Device.ANVIL, materialId)) {
            case "stone" -> STONE_ANVIL_DURABILITY;
            case "bronze" -> BRONZE_ANVIL_DURABILITY;
            case "steel" -> STEEL_ANVIL_DURABILITY;
            default -> IRON_ANVIL_DURABILITY;
        };
    }

    public static int hammerMaxDurability(String materialId) {
        return switch (sanitize(Device.HAMMER, materialId)) {
            case "bronze" -> BRONZE_HAMMER_DURABILITY;
            case "steel" -> STEEL_HAMMER_DURABILITY;
            default -> IRON_HAMMER_DURABILITY;
        };
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
        String safeMaterial = sanitize(device, materialId);
        return switch (device) {
            case CRUCIBLE -> CRUCIBLE_PROCESSING_TIERS.get(safeMaterial);
            case ANVIL -> ANVIL_PROCESSING_TIERS.get(safeMaterial);
            case HAMMER -> HAMMER_PROCESSING_TIERS.get(safeMaterial);
        };
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
        String safeMaterial = sanitize(Device.CRUCIBLE, casingMaterialId);
        return maxTemperature(MaterialCatalog.require(safeMaterial).thermal().meltingPoint());
    }

    public static float maxTemperature(double meltingPointCelsius) {
        if (!Double.isFinite(meltingPointCelsius) || meltingPointCelsius < 0.0) {
            throw new IllegalArgumentException("Melting point must be finite and non-negative");
        }
        return (float) Math.floor(
                (meltingPointCelsius + KELVIN_OFFSET) * CRUCIBLE_TEMPERATURE_FACTOR
                        - KELVIN_OFFSET);
    }

    public static double casingMassGrams(String casingMaterialId, double volumeCm3) {
        if (!Double.isFinite(volumeCm3) || volumeCm3 < 0.0) {
            throw new IllegalArgumentException("Casing volume must be finite and non-negative");
        }
        String safeMaterial = sanitize(Device.CRUCIBLE, casingMaterialId);
        return MaterialCatalog.require(safeMaterial).thermal().density() * volumeCm3;
    }

    private MachineMaterialRules() {}
}
