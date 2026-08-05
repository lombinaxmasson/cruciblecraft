package com.masson.cruciblecraft.machine.component;

import com.masson.cruciblecraft.machine.MachineMaterialRules;
import com.masson.cruciblecraft.machine.MachineMaterialRules.Device;

/** Material-backed casing state with all derived machine limits in one place. */
public final class MachineCasing {
    private final Device device;
    private final double volumeCm3;
    private String materialId;
    private String quarantinedMaterialId = "";

    public MachineCasing(Device device, double volumeCm3) {
        if (!Double.isFinite(volumeCm3) || volumeCm3 < 0.0) {
            throw new IllegalArgumentException("Casing volume must be finite and non-negative");
        }
        this.device = device;
        this.volumeCm3 = volumeCm3;
        this.materialId = MachineMaterialRules.defaultMaterial(device);
    }

    public boolean setMaterialId(String requestedId) {
        String material = MachineMaterialRules.requireAllowed(device, requestedId);
        if (material.equals(materialId) && quarantinedMaterialId.isEmpty()) {
            return false;
        }
        materialId = material;
        quarantinedMaterialId = "";
        return true;
    }

    public boolean restoreMaterialId(String requestedId) {
        var resolution = MachineMaterialRules.resolveExternal(device, requestedId);
        String quarantined = resolution.quarantinedMaterial().orElse("");
        if (resolution.effectiveMaterial().equals(materialId)
                && quarantined.equals(quarantinedMaterialId)) {
            return false;
        }
        materialId = resolution.effectiveMaterial();
        quarantinedMaterialId = quarantined;
        return true;
    }

    public String materialId() {
        return materialId;
    }

    public String persistedMaterialId() {
        return quarantinedMaterialId.isEmpty()
                ? materialId
                : quarantinedMaterialId;
    }

    public boolean quarantined() {
        return !quarantinedMaterialId.isEmpty();
    }

    public String quarantinedMaterialId() {
        return quarantinedMaterialId;
    }

    public int materialTier() {
        return MachineMaterialRules.materialTier(materialId);
    }

    public int processingTier() {
        return MachineMaterialRules.processingTier(device, materialId);
    }

    public float maxTemperature() {
        if (device != Device.CRUCIBLE) {
            throw new IllegalStateException("Temperature limit is only defined for crucible casings");
        }
        return MachineMaterialRules.crucibleMaxTemperature(materialId);
    }

    public double massGrams() {
        if (device != Device.CRUCIBLE) {
            throw new IllegalStateException("Casing mass is only defined for crucible casings");
        }
        return MachineMaterialRules.casingMassGrams(materialId, volumeCm3);
    }
}
