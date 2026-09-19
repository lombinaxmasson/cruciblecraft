package com.masson.cruciblecraft.content.mte;

import com.masson.cruciblecraft.api.energy.EnergyType;

/**
 * Discriminator for GT6 MTE identities that keep their dummy modern id and
 * become live BlockItems in place. These are not pipe covers, KU axles,
 * or vanilla tools.
 */
public enum MteInPlaceKind {
    FAUCET,
    TAP,
    FUNNEL,
    NOZZLE,
    CAP_NOZZLE,
    TANK_EXTENDER,
    TANK_BRIDGE,
    STEAM_TURBINE,
    BATTERY_BOX,
    WOOD_PANEL,
    ROPE,
    CHEST,
    SAFE,
    CRAFTING_TABLE,
    SCAFFOLD,
    BARREL,
    BOOKSHELF,
    BOTTLE_CRATE,
    DRAWER,
    LOCKER,
    MASS_STORAGE,
    AXLE,
    GEARBOX,
    ROTATION_ENGINE,
    ROTATION_TRANSFORMER,
    MISC_TOOL,
    MULTIBLOCK_PART,
    CRUCIBLE_FOUNDRY;

    public boolean attachment() {
        return this == FAUCET
                || this == TAP
                || this == FUNNEL
                || this == NOZZLE
                || this == CAP_NOZZLE;
    }

    public boolean extender() {
        return this == TANK_EXTENDER || this == TANK_BRIDGE;
    }

    public boolean inventory() {
        return switch (this) {
            case CHEST,
                    SAFE,
                    CRAFTING_TABLE,
                    BARREL,
                    BOOKSHELF,
                    BOTTLE_CRATE,
                    DRAWER,
                    LOCKER,
                    MASS_STORAGE -> true;
            default -> false;
        };
    }

    public int slots() {
        return switch (this) {
            case CHEST -> 54;
            case SAFE -> 15;
            case BARREL -> 27;
            case CRAFTING_TABLE, BOTTLE_CRATE -> 9;
            case BOOKSHELF -> 28;
            case DRAWER -> 144;
            case LOCKER -> 4;
            case MASS_STORAGE -> 1;
            default -> 0;
        };
    }

    public boolean playerInventoryGui() {
        return switch (this) {
            case CHEST, SAFE, BOOKSHELF, BOTTLE_CRATE, DRAWER -> true;
            default -> false;
        };
    }

    public boolean storageTab() {
        return switch (this) {
            case CHEST,
                    SAFE,
                    BOOKSHELF,
                    BOTTLE_CRATE,
                    DRAWER,
                    LOCKER,
                    MASS_STORAGE -> true;
            default -> false;
        };
    }

    public boolean foundryTank() {
        return this == CRUCIBLE_FOUNDRY;
    }

    public boolean energy() {
        return this == STEAM_TURBINE
                || this == BATTERY_BOX
                || drive();
    }

    public boolean drive() {
        return this == AXLE
                || this == GEARBOX
                || this == ROTATION_ENGINE
                || this == ROTATION_TRANSFORMER;
    }

    public EnergyType energyType() {
        if (this == BATTERY_BOX) {
            return EnergyType.ELECTRIC;
        }
        if (this == STEAM_TURBINE || drive()) {
            return EnergyType.KINETIC_ROTATION;
        }
        return EnergyType.HEAT;
    }

    public boolean decorative() {
        return this == WOOD_PANEL || this == ROPE || this == SCAFFOLD;
    }
}
