package com.masson.cruciblecraft.content.mte;

import com.masson.cruciblecraft.api.energy.EnergyType;

import net.minecraft.core.Direction;

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
    GAS_TURBINE,
    LARGE_DYNAMO,
    LARGE_BOILER,
    LIGHTNING_ROD,
    MATTER_FABRICATOR,
    VON_DA_GRAAGG,
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
                    MASS_STORAGE,
                    BATTERY_BOX -> true;
            default -> false;
        };
    }

    public int slots() {
        return switch (this) {
            case CHEST -> 54;
            case SAFE -> 15;
            case CRAFTING_TABLE, BOTTLE_CRATE -> 9;
            case BOOKSHELF -> 28;
            case DRAWER -> 144;
            case LOCKER -> 4;
            case BARREL, MASS_STORAGE -> 1;
            case BATTERY_BOX -> 4;
            default -> 0;
        };
    }

    /**
     * GT6 {@code MultiTileEntityMassStorage} and {@code MassStorageBarrel} share
     * the single-slot bulk store. Furniture BARREL is not a 27-slot chest.
     */
    public boolean massStorage() {
        return this == MASS_STORAGE || this == BARREL;
    }

    public boolean playerInventoryGui() {
        return switch (this) {
            case CHEST, SAFE, BOOKSHELF, BOTTLE_CRATE, DRAWER, BATTERY_BOX -> true;
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
                    BARREL,
                    MASS_STORAGE -> true;
            default -> false;
        };
    }

    public boolean dedicatedController() {
        return this == GAS_TURBINE
                || this == LARGE_DYNAMO
                || this == LARGE_BOILER
                || this == LIGHTNING_ROD
                || this == MATTER_FABRICATOR
                || this == VON_DA_GRAAGG;
    }

    public boolean energy() {
        return this == STEAM_TURBINE
                || this == BATTERY_BOX
                || this == LARGE_DYNAMO
                || this == LARGE_BOILER
                || this == LIGHTNING_ROD
                || this == VON_DA_GRAAGG
                || drive();
    }

    public boolean drive() {
        return driveTransmit() || rotationEngine();
    }

    /** RU pass-through axles and gearboxes. Rotation engines convert instead. */
    public boolean driveTransmit() {
        return this == AXLE || this == GEARBOX || this == ROTATION_TRANSFORMER;
    }

    public boolean rotationEngine() {
        return this == ROTATION_ENGINE;
    }

    public boolean foundryTank() {
        return this == CRUCIBLE_FOUNDRY;
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

    /**
     * GT6 {@code allowCovers}: chests/lockers/mass storage accept every face;
     * safes and drawers skip the front; bookshelves skip the facing axis;
     * crafting tables skip the top and the facing axis; bottle crates and
     * attachments refuse covers. Steam turbines and RU drive hosts keep the
     * default true from {@code TileEntityBase06Covers}.
     */
    public boolean allowCover(Direction facing, Direction side) {
        if (side == null) {
            return false;
        }
        return switch (this) {
            case CHEST, BARREL, MASS_STORAGE, LOCKER -> true;
            case SAFE, DRAWER -> facing == null || side != facing;
            case BOOKSHELF -> facing == null
                    || side.getAxis() != facing.getAxis();
            case CRAFTING_TABLE -> side != Direction.UP
                    && (facing == null || side.getAxis() != facing.getAxis());
            case STEAM_TURBINE,
                    GAS_TURBINE,
                    LARGE_DYNAMO,
                    LARGE_BOILER,
                    LIGHTNING_ROD,
                    MATTER_FABRICATOR,
                    VON_DA_GRAAGG,
                    AXLE,
                    GEARBOX,
                    ROTATION_ENGINE,
                    ROTATION_TRANSFORMER -> true;
            default -> false;
        };
    }
}
