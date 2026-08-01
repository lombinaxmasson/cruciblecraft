package com.masson.cruciblecraft.api.material;

import com.masson.cruciblecraft.CrucibleCraft;

/** Well-known built-in prefix ids. The registry remains open to addon ids. */
public final class MaterialPrefixes {
    public static final MaterialPrefix BLOCK = builtin("block");
    public static final MaterialPrefix ORE = builtin("ore");
    public static final MaterialPrefix RAW_ORE = builtin("raw_ore");
    public static final MaterialPrefix CRUSHED_ORE = builtin("crushed_ore");
    public static final MaterialPrefix TINY_CRUSHED_ORE = builtin("tiny_crushed_ore");
    public static final MaterialPrefix WASHED_CRUSHED_ORE = builtin("washed_crushed_ore");
    public static final MaterialPrefix CENTRIFUGED_CRUSHED_ORE =
            builtin("centrifuged_crushed_ore");
    public static final MaterialPrefix PURIFIED_DUST = builtin("purified_dust");
    public static final MaterialPrefix INGOT = builtin("ingot");
    public static final MaterialPrefix DUST = builtin("dust");
    public static final MaterialPrefix PLATE = builtin("plate");
    public static final MaterialPrefix ROD = builtin("rod");
    public static final MaterialPrefix LONG_ROD = builtin("long_rod");
    public static final MaterialPrefix SMALL_DUST = builtin("small_dust");
    public static final MaterialPrefix BOLT = builtin("bolt");
    public static final MaterialPrefix SCREW = builtin("screw");
    public static final MaterialPrefix RING = builtin("ring");
    public static final MaterialPrefix SPRING = builtin("spring");
    public static final MaterialPrefix SMALL_SPRING = builtin("small_spring");
    public static final MaterialPrefix GEAR = builtin("gear");
    public static final MaterialPrefix SMALL_GEAR = builtin("small_gear");
    public static final MaterialPrefix ROTOR = builtin("rotor");
    public static final MaterialPrefix FOIL = builtin("foil");
    public static final MaterialPrefix DOUBLE_PLATE = builtin("double_plate");
    public static final MaterialPrefix TRIPLE_PLATE = builtin("triple_plate");
    public static final MaterialPrefix QUADRUPLE_PLATE = builtin("quadruple_plate");
    public static final MaterialPrefix QUINTUPLE_PLATE = builtin("quintuple_plate");
    public static final MaterialPrefix DENSE_PLATE = builtin("dense_plate");
    public static final MaterialPrefix FINE_WIRE = builtin("fine_wire");
    public static final MaterialPrefix WIRE = builtin("wire");
    public static final MaterialPrefix DOUBLE_WIRE = builtin("double_wire");
    public static final MaterialPrefix QUADRUPLE_WIRE = builtin("quadruple_wire");
    public static final MaterialPrefix OCTUPLE_WIRE = builtin("octuple_wire");
    public static final MaterialPrefix DODECUPLE_WIRE = builtin("dodecuple_wire");
    public static final MaterialPrefix HEXADECUPLE_WIRE = builtin("hexadecuple_wire");
    public static final MaterialPrefix CABLE = builtin("cable");
    public static final MaterialPrefix DOUBLE_CABLE = builtin("double_cable");
    public static final MaterialPrefix QUADRUPLE_CABLE = builtin("quadruple_cable");
    public static final MaterialPrefix OCTUPLE_CABLE = builtin("octuple_cable");
    public static final MaterialPrefix DODECUPLE_CABLE = builtin("dodecuple_cable");
    public static final MaterialPrefix NUGGET = builtin("nugget");
    public static final MaterialPrefix GEM = builtin("gem");
    public static final MaterialPrefix TINY_DUST = builtin("tiny_dust");

    private MaterialPrefixes() {}

    private static MaterialPrefix builtin(String path) {
        return new MaterialPrefix(CrucibleCraft.MODID + ":" + path);
    }
}
