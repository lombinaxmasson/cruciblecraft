package com.masson.cruciblecraft.energy.flux;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.masson.cruciblecraft.api.energy.EnergyType;

import net.minecraft.resources.ResourceLocation;

/** One GT6 flux heater / engine / motor / magnet / laser / dynamo. */
public record FluxProfile(
        ResourceLocation id,
        int sourceId,
        int sourceLine,
        String gt6Class,
        String kind,
        String material,
        int nbtInput,
        int nbtOutput,
        String accepts,
        String emits,
        float hardness,
        float resistance,
        String langEn,
        String langZh,
        String textureFolder,
        ResourceLocation hostId,
        boolean recipeLive,
        Recipe recipe) {
    public static final int EXPECTED_SIZE = 30;
    public static final String HEATER = "heater";
    public static final String ENGINE = "engine";
    public static final String MOTOR = "motor";
    public static final String MAGNET = "magnet";
    public static final String LASER = "laser";
    public static final String DYNAMO = "dynamo";
    public static final String RF = "RF";
    public static final String HU = "HU";
    public static final String KU = "KU";
    public static final String RU = "RU";
    public static final String MU = "MU";
    public static final String LU = "LU";

    public FluxProfile {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(gt6Class, "gt6Class");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(accepts, "accepts");
        Objects.requireNonNull(emits, "emits");
        Objects.requireNonNull(langEn, "langEn");
        Objects.requireNonNull(langZh, "langZh");
        Objects.requireNonNull(textureFolder, "textureFolder");
        Objects.requireNonNull(hostId, "hostId");
        Objects.requireNonNull(recipe, "recipe");
        if (sourceId <= 0
                || sourceLine <= 0
                || nbtInput <= 0
                || nbtOutput <= 0
                || hardness <= 0.0F
                || resistance <= 0.0F) {
            throw new IllegalArgumentException(
                    "Flux converter source and rates must be valid");
        }
        String expectedClass = switch (kind) {
            case HEATER -> "MultiTileEntityHeaterFlux";
            case ENGINE -> "MultiTileEntityEngineFlux";
            case MOTOR -> "MultiTileEntityMotorFlux";
            case MAGNET -> "MultiTileEntityMagnetFlux";
            case LASER -> "MultiTileEntityLaserFlux";
            case DYNAMO -> "MultiTileEntityDynamoFlux";
            default -> throw new IllegalArgumentException(
                    "Unknown flux kind " + kind);
        };
        if (!expectedClass.equals(gt6Class)) {
            throw new IllegalArgumentException(
                    "Flux row class drifted " + id);
        }
        if (kind.equals(DYNAMO)) {
            if (!RU.equals(accepts) || !RF.equals(emits)) {
                throw new IllegalArgumentException(
                        "Flux dynamo must accept RU and emit RF " + id);
            }
        } else if (!RF.equals(accepts)) {
            throw new IllegalArgumentException(
                    "Flux converter must accept RF " + id);
        }
    }

    public boolean heater() {
        return HEATER.equals(kind);
    }

    public boolean engine() {
        return ENGINE.equals(kind);
    }

    public boolean motor() {
        return MOTOR.equals(kind);
    }

    public boolean magnet() {
        return MAGNET.equals(kind);
    }

    public boolean laser() {
        return LASER.equals(kind);
    }

    public boolean dynamo() {
        return DYNAMO.equals(kind);
    }

    public boolean fluxInput() {
        return RF.equals(accepts);
    }

    public boolean fluxOutput() {
        return RF.equals(emits);
    }

    public boolean converterMode() {
        return heater() || motor() || magnet() || laser();
    }

    public boolean wasteEnergy() {
        return !engine();
    }

    public boolean noOcclusion() {
        return engine();
    }

    public long energyCapacity() {
        return Math.multiplyExact((long) nbtInput, 2L);
    }

    public long inputMaximum() {
        return energyCapacity();
    }

    public long outputMinimum() {
        return Math.max(1L, nbtOutput / 2L);
    }

    public long outputMaximum() {
        return Math.multiplyExact((long) nbtOutput, 2L);
    }

    public EnergyType gregOutputType() {
        return switch (emits) {
            case HU -> EnergyType.HEAT;
            case KU -> EnergyType.KINETIC_PUSH;
            case RU -> EnergyType.KINETIC_ROTATION;
            case MU -> EnergyType.MU;
            case LU -> EnergyType.LU;
            default -> null;
        };
    }

    public EnergyType gregInputType() {
        return RU.equals(accepts) ? EnergyType.KINETIC_ROTATION : null;
    }

    public boolean sizeIrrelevantOutput() {
        EnergyType type = gregOutputType();
        return type != null && type.sizeIrrelevant();
    }

    public record Recipe(
            List<String> pattern,
            Map<String, Ingredient> keys,
            List<String> catalysts) {
        public Recipe {
            pattern = List.copyOf(pattern);
            keys = Map.copyOf(keys);
            catalysts = List.copyOf(catalysts);
        }
    }

    public record Ingredient(
            String item, String prefix, String material) {
        public Ingredient {
            boolean hasItem = item != null && !item.isBlank();
            boolean hasPart = prefix != null && material != null;
            if (hasItem == hasPart) {
                throw new IllegalArgumentException(
                        "Flux recipe key needs item or prefix+material");
            }
            if (hasPart && (prefix.isBlank() || material.isBlank())) {
                throw new IllegalArgumentException(
                        "Flux recipe key needs prefix+material");
            }
        }
    }
}
