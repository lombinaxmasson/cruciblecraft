package com.masson.cruciblecraft.content.blockentity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * GT6 {@code MultiTileEntityDistillationTower.doOutputFluids} /
 * {@code CryoDistillationTower.doOutputFluids} height table.
 */
public final class DistillationTowerFluidRouting {
    public enum Kind {
        HOT,
        CRYO
    }

    private DistillationTowerFluidRouting() {}

    public static int localY(Kind kind, FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 1;
        }
        return localY(kind, stack.getFluid());
    }

    public static int localY(Kind kind, Fluid fluid) {
        String path = BuiltInRegistries.FLUID.getKey(fluid).getPath();
        return kind == Kind.CRYO ? cryoY(path) : hotY(path);
    }

    private static int hotY(String path) {
        if (named(path, "propane", "methane")) {
            return 7;
        }
        if (named(path, "butane")) {
            return 6;
        }
        if (named(path, "petrol", "gasoline", "ethanol", "bioethanol")) {
            return 5;
        }
        if (named(path, "kerosene", "kerosine", "glycerol")) {
            return 4;
        }
        if (named(path, "diesel", "biodiesel")) {
            return 3;
        }
        if (named(path, "fuel", "fueloil", "biofuel")) {
            return 2;
        }
        return 1;
    }

    private static int cryoY(String path) {
        if (named(path, "helium")) {
            return 7;
        }
        if (named(path, "neon")) {
            return 6;
        }
        if (named(path, "nitrogen")) {
            return 5;
        }
        if (named(path, "oxygen")) {
            return 4;
        }
        if (named(path, "argon")) {
            return 3;
        }
        if (named(path, "carbondioxide", "carbon_dioxide",
                "sulfurdioxide", "sulfur_dioxide")) {
            return 2;
        }
        return 1;
    }

    private static boolean named(String path, String... aliases) {
        for (String alias : aliases) {
            if (path.equals(alias)) {
                return true;
            }
        }
        return false;
    }
}
