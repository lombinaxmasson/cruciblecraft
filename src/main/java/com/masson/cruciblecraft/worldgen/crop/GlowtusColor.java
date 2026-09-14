package com.masson.cruciblecraft.worldgen.crop;

import java.util.List;

/**
 * GT6 {@code BlockGlowtus} meta 0–15, {@code DYE_NAMES} order. Meta 0 is black.
 */
public enum GlowtusColor {
    BLACK("black", "Black"),
    RED("red", "Red"),
    GREEN("green", "Green"),
    BROWN("brown", "Brown"),
    BLUE("blue", "Blue"),
    PURPLE("purple", "Purple"),
    CYAN("cyan", "Cyan"),
    LIGHT_GRAY("light_gray", "Light Gray"),
    GRAY("gray", "Gray"),
    PINK("pink", "Pink"),
    LIME("lime", "Lime"),
    YELLOW("yellow", "Yellow"),
    LIGHT_BLUE("light_blue", "Light Blue"),
    MAGENTA("magenta", "Magenta"),
    ORANGE("orange", "Orange"),
    WHITE("white", "White");

    public static final List<GlowtusColor> ALL = List.of(values());

    private final String id;
    private final String englishName;

    GlowtusColor(String id, String englishName) {
        this.id = id;
        this.englishName = englishName;
    }

    public String id() {
        return id;
    }

    public String englishName() {
        return englishName;
    }

    public String blockPath() {
        return "plant/glowtus_" + id;
    }

    public String texturePath() {
        return "block/gt6/iconsets/glowtus_" + id;
    }

    public static GlowtusColor byMeta(int meta) {
        return ALL.get(Math.floorMod(meta, ALL.size()));
    }
}
