package com.masson.cruciblecraft.worldgen;

import net.minecraft.util.StringRepresentable;

/**
 * GT6 {@code BlocksGT.Grass} metas 0–5. Worldgen uses type 1 → yellow (4),
 * type 2 → brown (5), type 3 → medium (0).
 */
public enum IndicatorGrass implements StringRepresentable {
    MEDIUM("medium", 0, "Grass", "草"),
    LIGHT("light", 1, "Grass", "草"),
    DARK("dark", 2, "Grass", "草"),
    NORMAL("normal", 3, "Grass", "草"),
    YELLOW("yellow", 4, "Grass", "草"),
    BROWN("brown", 5, "Grass", "草");

    private final String serializedName;
    private final int meta;
    private final String englishName;
    private final String chineseName;

    IndicatorGrass(String serializedName, int meta, String englishName, String chineseName) {
        this.serializedName = serializedName;
        this.meta = meta;
        this.englishName = englishName;
        this.chineseName = chineseName;
    }

    public int meta() {
        return meta;
    }

    public String englishName() {
        return englishName;
    }

    public String chineseName() {
        return chineseName;
    }

    public String tooltipEnglish() {
        return "Does not spread, get eaten, change color nor need light";
    }

    public String tooltipChinese() {
        return "不会蔓延、被吃掉、变色，也不需要光照";
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    public static IndicatorGrass ofIndicatorType(int indicatorType) {
        return switch (indicatorType) {
            case 1 -> YELLOW;
            case 2 -> BROWN;
            case 3 -> MEDIUM;
            default -> NORMAL;
        };
    }
}
