package com.masson.cruciblecraft.content.mold;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * GT6 {@code IL.Ceramic_*_Mold_Raw} metadata 900–929 and the matching
 * {@code IL.Ceramic_Mold} firing NBT. Blank clay / ceramic molds stay
 * outside this list.
 */
public final class CeramicMoldCatalog {
    public static final List<Variant> SHAPED = List.of(
            variant("ingot", 900, 0b0_01110_01110_01110_01110_01110,
                    "Ingot Mold", "锭模具",
                    "Clay Ingot Mold", "黏土锭模具"),
            variant("chunk", 901, 0b0_11000_11000_00000_00000_00000,
                    "Chunk Mold", "块模具",
                    "Clay Chunk Mold", "黏土块模具"),
            variant("plate", 902, 0b0_11111_11111_11111_11111_11111,
                    "Plate Mold", "板模具",
                    "Clay Plate Mold", "黏土板模具"),
            variant("tiny_plate", 903, 0b0_00000_01110_01110_01110_00000,
                    "Tiny Plate Mold", "微型板模具",
                    "Clay Tiny Plate Mold", "黏土微型板模具"),
            variant("bolt", 904, 0b0_00000_00000_00100_00100_00000,
                    "Bolt Mold", "螺栓模具",
                    "Clay Bolt Mold", "黏土螺栓模具"),
            variant("rod", 905, 0b0_00000_00000_11111_00000_00000,
                    "Rod Mold", "杆模具",
                    "Clay Rod Mold", "黏土杆模具"),
            variant("long_rod", 906, 0b0_10000_01000_00100_00010_00001,
                    "Long Rod Mold", "长杆模具",
                    "Clay Long Rod Mold", "黏土长杆模具"),
            variant("small_casing", 907, 0b0_11101_11101_11101_00001_11100,
                    "Item Casing Mold", "物品外壳模具",
                    "Clay Item Casing Mold", "黏土物品外壳模具"),
            variant("ring", 908, 0b0_00000_01110_01010_01110_00000,
                    "Ring Mold", "环模具",
                    "Clay Ring Mold", "黏土环模具"),
            variant("gear", 909, 0b0_10101_01110_11011_01110_10101,
                    "Gear Mold", "齿轮模具",
                    "Clay Gear Mold", "黏土齿轮模具"),
            variant("small_gear", 910, 0b0_01010_11111_01010_11111_01010,
                    "Small Gear Mold", "小齿轮模具",
                    "Clay Small Gear Mold", "黏土小齿轮模具"),
            variant("sword", 911, 0b0_00100_01110_01110_01110_01110,
                    "Sword Mold", "剑模具",
                    "Clay Sword Mold", "黏土剑模具"),
            variant("pickaxe", 912, 0b0_00000_01110_10001_00000_00000,
                    "Pickaxe Mold", "镐模具",
                    "Clay Pickaxe Mold", "黏土镐模具"),
            variant("spade", 913, 0b0_01110_01110_01110_01010_00000,
                    "Spade Mold", "锹模具",
                    "Clay Spade Mold", "黏土锹模具"),
            variant("shovel", 914, 0b0_00100_01110_01110_01110_00000,
                    "Shovel Mold", "铲模具",
                    "Clay Shovel Mold", "黏土铲模具"),
            variant("universal_spade", 915, 0b0_00100_01110_01100_01110_00000,
                    "Universal Spade Mold", "万能锹模具",
                    "Clay Universal Spade Mold", "黏土万能锹模具"),
            variant("axe", 916, 0b0_00000_01110_01110_01000_00000,
                    "Axe Mold", "斧模具",
                    "Clay Axe Mold", "黏土斧模具"),
            variant("double_axe", 917, 0b0_00000_11111_11111_10001_00000,
                    "Double Axe Mold", "双刃斧模具",
                    "Clay Double Axe Mold", "黏土双刃斧模具"),
            variant("saw", 918, 0b0_00000_11111_11111_00000_00000,
                    "Saw Mold", "锯模具",
                    "Clay Saw Mold", "黏土锯模具"),
            variant("hammer", 919, 0b0_01110_01110_01010_01110_01110,
                    "Hammer Mold", "锤模具",
                    "Clay Hammer Mold", "黏土锤模具"),
            variant("file", 920, 0b0_01110_01110_01110_00100_00100,
                    "File Mold", "锉模具",
                    "Clay File Mold", "黏土锉模具"),
            variant("screwdriver", 921, 0b0_00000_00100_00100_00100_00100,
                    "Screwdriver Mold", "螺丝刀模具",
                    "Clay Screwdriver Mold", "黏土螺丝刀模具"),
            variant("chisel", 922, 0b0_01110_00100_00100_00100_00100,
                    "Chisel Mold", "凿模具",
                    "Clay Chisel Mold", "黏土凿模具"),
            variant("arrow", 923, 0b0_00000_00100_00100_01110_00000,
                    "Arrow Mold", "箭模具",
                    "Clay Arrow Mold", "黏土箭模具"),
            variant("hoe", 924, 0b0_00000_00110_01110_00000_00000,
                    "Hoe Mold", "锄模具",
                    "Clay Hoe Mold", "黏土锄模具"),
            variant("sense", 925, 0b0_00000_01111_11111_00000_00000,
                    "Sense Mold", "镰刀模具",
                    "Clay Sense Mold", "黏土镰刀模具"),
            variant("plow", 926, 0b0_11111_11111_11111_11111_00100,
                    "Plow Mold", "犁模具",
                    "Clay Plow Mold", "黏土犁模具"),
            variant("builderwand", 927, 0b0_00000_00100_11111_01110_01010,
                    "Builder's Wand Mold", "建筑师权杖模具",
                    "Clay Builder's Wand Mold", "黏土建筑师权杖模具"),
            variant("nugget", 928, 0b0_00000_00000_00100_00000_00000,
                    "Nugget Mold", "粒模具",
                    "Clay Nugget Mold", "黏土粒模具"),
            variant("billet", 929, 0b0_01100_11110_11110_01100_00000,
                    "Billet Mold", "坯模具",
                    "Clay Billet Mold", "黏土坯模具"));

    private static final Map<String, Variant> BY_ID = SHAPED.stream()
            .collect(Collectors.toUnmodifiableMap(Variant::id, Function.identity()));
    private static final Map<Integer, Variant> BY_PATTERN = SHAPED.stream()
            .collect(Collectors.toUnmodifiableMap(
                    Variant::firedPattern, Function.identity()));

    private CeramicMoldCatalog() {}

    public static Variant require(String id) {
        Variant variant = BY_ID.get(id);
        if (variant == null) {
            throw new IllegalArgumentException("Unknown ceramic mold " + id);
        }
        return variant;
    }

    public static Variant findByPattern(int pattern) {
        return BY_PATTERN.get(pattern & ((1 << MoldRecipes.CELL_COUNT) - 1));
    }

    public static String rawItemId(Variant variant) {
        return "raw_" + variant.id() + "_mold";
    }

    public static String firedItemId(Variant variant) {
        return variant.id() + "_mold";
    }

    private static Variant variant(
            String id,
            int gt6Meta,
            int firedPattern,
            String englishFired,
            String chineseFired,
            String englishRaw,
            String chineseRaw) {
        return new Variant(
                id,
                gt6Meta,
                firedPattern,
                englishFired,
                chineseFired,
                englishRaw,
                chineseRaw);
    }

    public record Variant(
            String id,
            int gt6Meta,
            int firedPattern,
            String englishFired,
            String chineseFired,
            String englishRaw,
            String chineseRaw) {}
}
