package com.masson.cruciblecraft.worldgen;

import java.util.List;

/**
 * GT6 {@code Loader_Worldgen} {@code WorldgenFluidSpring} rows that run without
 * other mods: overworld plus nether. Atum/Erebus/Betweenlands/Twilight stay out.
 * Infinite oil/gas defaults match {@code GenerateInfiniteOilSources}/{@code
 * GenerateInfiniteGasSources} = true.
 */
public final class FluidSpringCatalog {
    public static final List<Vein> VEINS = List.of(
            overworld("overworld.fluid.oil.extraheavy", SpringFluidKind.OIL_EXTRA_HEAVY, 400, 2, 6000),
            overworld("overworld.fluid.oil.heavy", SpringFluidKind.OIL_HEAVY, 400, 2, 6000),
            overworld("overworld.fluid.oil.medium", SpringFluidKind.OIL_MEDIUM, 400, 2, 6000),
            overworld("overworld.fluid.oil.light", SpringFluidKind.OIL_LIGHT, 400, 2, 6000),
            overworld("overworld.fluid.gas.natural", SpringFluidKind.NATURAL_GAS, 200, 1, 3000),
            overworld("overworld.fluid.water", SpringFluidKind.WATER_GEOTHERMAL, 100, 3, 500),
            overworld("overworld.fluid.lava", SpringFluidKind.LAVA, 200, 1, 1000),
            nether("nether.fluid.lava", SpringFluidKind.LAVA, 100, 1, 500));

    public static final int OVERWORLD_COUNT = 7;
    public static final int NETHER_COUNT = 1;

    private FluidSpringCatalog() {}

    public static List<Vein> forNether(boolean nether) {
        return VEINS.stream().filter(vein -> vein.nether() == nether).toList();
    }

    public static Vein overworldLava() {
        return VEINS.get(6);
    }

    private static Vein overworld(
            String gt6Name,
            SpringFluidKind fluid,
            int probability,
            int indicatorType,
            int springAmount) {
        return new Vein(gt6Name, fluid, probability, indicatorType, springAmount, false);
    }

    private static Vein nether(
            String gt6Name,
            SpringFluidKind fluid,
            int probability,
            int indicatorType,
            int springAmount) {
        return new Vein(gt6Name, fluid, probability, indicatorType, springAmount, true);
    }

    public record Vein(
            String gt6Name,
            SpringFluidKind fluid,
            int probability,
            int indicatorType,
            int springAmount,
            boolean nether) {}
}
