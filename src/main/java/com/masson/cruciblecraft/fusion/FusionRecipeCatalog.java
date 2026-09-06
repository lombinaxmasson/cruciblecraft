package com.masson.cruciblecraft.fusion;

import java.util.List;

/** Pinned GT6 {@code RM.Fusion} rows from Loader_Recipes_Other 949-966. */
public final class FusionRecipeCatalog {
    public static final long LU_PACKET = 8192L;
    public static final long LU_START_SCALE = 16L;
    public static final int GAS_MB_PER_U = 1_000;
    public static final int LIQUID_MB_PER_U = 144;

    public record FluidIo(String material, int milliBuckets, boolean molten) {}

    public record ItemIo(String material, String prefix, int count) {}

    public record Entry(
            String id,
            int circuit,
            int duration,
            long eut,
            long luCoefficient,
            List<FluidIo> fluidInputs,
            List<FluidIo> fluidOutputs,
            List<ItemIo> itemOutputs) {
        public long luStart() {
            return luCoefficient * LU_PACKET * LU_START_SCALE;
        }
    }

    private static final List<Entry> ENTRIES = List.of(
            row("deuterium_split", 1, 730, -8192L, 730L,
                    fluids(gas("deuterium", 2_000)),
                    fluids(gas("helium3", 500), gas("tritium", 500))),
            row("tritium_to_helium", 1, 1130, -8192L, 1130L,
                    fluids(gas("tritium", 2_000)),
                    fluids(gas("helium", 1_000))),
            row("helium3_to_helium", 1, 1290, -8192L, 1290L,
                    fluids(gas("helium3", 2_000)),
                    fluids(gas("helium", 1_000))),
            row("helium_to_beryllium8", 1, 1890, 0L, 1890L,
                    fluids(gas("helium", 2_000)),
                    fluids(liquid("beryllium8", 144))),
            row("beryllium8_to_oxygen", 1, 3214, 0L, 3214L,
                    fluids(liquid("beryllium8", 288)),
                    fluids(gas("oxygen", 1_000))),
            row("proton_boron11", 2, 546, -8192L, 8469L,
                    fluids(gas("hydrogen", 1_000), liquid("boron11", 144)),
                    fluids(gas("helium", 3_000))),
            row("proton_carbon", 2, 315, -8192L, 315L,
                    fluids(gas("hydrogen", 1_000), liquid("carbon", 144)),
                    fluids(liquid("carbon13", 144))),
            row("proton_carbon13", 2, 754, -8192L, 754L,
                    fluids(gas("hydrogen", 1_000), liquid("carbon13", 144)),
                    fluids(gas("nitrogen", 1_000))),
            row("proton_nitrogen", 2, 1404, -8192L, 1404L,
                    fluids(gas("hydrogen", 2_000), gas("nitrogen", 1_000)),
                    fluids(gas("helium", 500), liquid("carbon", 72), gas("oxygen", 500))),
            row("proton_oxygen", 2, 455, -8192L, 455L,
                    fluids(gas("hydrogen", 2_000), gas("oxygen", 1_000)),
                    fluids(gas("helium", 500), gas("fluorine", 500), gas("nitrogen", 500))),
            row("deuterium_tritium", 2, 1760, -8192L, 1760L,
                    fluids(gas("deuterium", 1_000), gas("tritium", 1_000)),
                    fluids(gas("helium", 1_000))),
            row("deuterium_helium3", 2, 1830, -8192L, 1830L,
                    fluids(gas("deuterium", 1_000), gas("helium3", 1_000)),
                    fluids(gas("helium", 1_000))),
            row("tritium_helium3", 2, 2640, -8192L, 2640L,
                    fluids(gas("tritium", 1_000), gas("helium3", 1_000)),
                    fluids(gas("helium", 750), gas("deuterium", 250))),
            row("deuterium_lithium6", 2, 3336, -8192L, 3336L,
                    fluids(gas("deuterium", 1_000), liquid("lithium6", 144)),
                    fluids(
                            gas("helium", 375),
                            gas("helium3", 125),
                            liquid("lithium", 18),
                            liquid("beryllium7", 18))),
            row("helium3_lithium6", 2, 1690, -8192L, 1690L,
                    fluids(gas("helium3", 1_000), liquid("lithium6", 144)),
                    fluids(gas("helium", 2_000))),
            row("helium_beryllium8", 2, 736, -8192L, 736L,
                    fluids(gas("helium", 1_000), liquid("beryllium8", 144)),
                    fluids(liquid("carbon", 144))),
            row("helium_carbon", 2, 716, -8192L, 716L,
                    fluids(gas("helium", 1_000), liquid("carbon", 144)),
                    fluids(gas("oxygen", 1_000))),
            new Entry(
                    "adamantium_beryllium7",
                    2,
                    1956,
                    -8192L,
                    94956L,
                    fluids(liquid("adamantium", 144), liquid("beryllium7", 144)),
                    fluids(
                            liquid("tungsten", 144),
                            gas("helium", 16_000),
                            gas("helium3", 24_000),
                            gas("tritium", 24_000)),
                    List.of(new ItemIo("vibranium", "dust", 1))));

    private FusionRecipeCatalog() {}

    public static List<Entry> entries() {
        return ENTRIES;
    }

    private static Entry row(
            String id,
            int circuit,
            int duration,
            long eut,
            long luCoefficient,
            List<FluidIo> inputs,
            List<FluidIo> outputs) {
        return new Entry(
                id, circuit, duration, eut, luCoefficient, inputs, outputs, List.of());
    }

    private static FluidIo gas(String material, int milliBuckets) {
        return new FluidIo(material, milliBuckets, false);
    }

    private static FluidIo liquid(String material, int milliBuckets) {
        return new FluidIo(material, milliBuckets, true);
    }

    private static List<FluidIo> fluids(FluidIo... values) {
        return List.of(values);
    }

    static {
        if (ENTRIES.size() != 18) {
            throw new IllegalStateException(
                    "Fusion catalog drifted from 18 GT6 rows: " + ENTRIES.size());
        }
    }
}
