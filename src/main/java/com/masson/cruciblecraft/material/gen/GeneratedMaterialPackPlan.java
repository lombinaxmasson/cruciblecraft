package com.masson.cruciblecraft.material.gen;

/** Pure distribution plan; it does not reference client-only Minecraft types. */
public record GeneratedMaterialPackPlan(boolean serverData, boolean clientAssets) {
    public static GeneratedMaterialPackPlan forDistribution(boolean clientDistribution) {
        return new GeneratedMaterialPackPlan(true, clientDistribution);
    }
}
