package com.masson.cruciblecraft.energy.remainder;

import com.masson.cruciblecraft.api.energy.EnergyType;

import net.minecraft.resources.ResourceLocation;

/**
 * One GT6 remainder energy host: solar, battery box, crystal charger, or
 * the magic field absorber.
 */
public record RemainderDevice(
        ResourceLocation id,
        RemainderDevice.Kind kind,
        int sourceId,
        int tier,
        String material,
        EnergyType energy,
        long input,
        long output,
        int slots,
        boolean legacy,
        int circuitIndex,
        String plateMaterial,
        String cableMaterial,
        String englishName,
        String chineseName) {
    public enum Kind {
        SOLAR("MultiTileEntitySolarPanelElectric", "solar_panel"),
        BATTERY_BOX("MultiTileEntityBatteryBox", "battery_box"),
        BATTERY_BOX_LARGE("MultiTileEntityBatteryBoxLarge", "battery_box_large"),
        CRYSTAL_CHARGER("MultiTileEntityCrystalCharger", "crystal_charger"),
        CRYSTAL_CHARGER_LARGE(
                "MultiTileEntityCrystalChargerLarge", "crystal_charger_large"),
        MAGIC_ABSORBER("MultiTileEntityMagicFieldAbsorber", "magic_absorber");

        private final String gt6Class;
        private final String texture;

        Kind(String gt6Class, String texture) {
            this.gt6Class = gt6Class;
            this.texture = texture;
        }

        public String gt6Class() {
            return gt6Class;
        }

        public String texture() {
            return texture;
        }
    }

    public enum ObtainKind {
        SHAPED,
        ALREADY_LIVE,
        BLOCKED
    }

    public record GridPart(char symbol, String token) {}

    public record ObtainPlan(
            ObtainKind kind,
            String reason,
            java.util.List<String> pattern,
            java.util.List<GridPart> parts) {}
}
