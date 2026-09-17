package com.masson.cruciblecraft.worldgen;

import net.minecraft.util.StringRepresentable;

/** GT6 {@code BlocksGT.FlowersA} / {@code FlowersB} metas used by bedrock veins. */
public enum IndicatorFlower implements StringRepresentable {
    ALTERED_ANDESITE_BUCKWHEAT(
            "altered_andesite_buckwheat",
            Family.A,
            0,
            false,
            "Altered Andesite Buckwheat",
            "蚀变安山荞麦",
            "Indicates presence of a Gold Deposit nearby",
            "附近有金矿脉"),
    CROSBY_BUCKWHEAT(
            "crosby_buckwheat",
            Family.A,
            1,
            false,
            "Crosby Buckwheat",
            "克罗斯比荞麦",
            "Indicates presence of a Silver Deposit nearby",
            "附近有银矿脉"),
    ALPINE_CATCHFLY(
            "alpine_catchfly",
            Family.A,
            2,
            false,
            "Alpine Catchfly",
            "高山捕虫瞿麦",
            "Indicates presence of a Copper Deposit nearby",
            "附近有铜矿脉"),
    VIOLA_CALAMINARIA(
            "viola_calaminaria",
            Family.A,
            3,
            false,
            "Viola Calaminaria",
            "锌堇菜",
            "Indicates presence of a Zinc Deposit nearby",
            "附近有锌矿脉"),
    THLASPI_LERESCHIANUM(
            "thlaspi_lereschianum",
            Family.A,
            4,
            false,
            "Thlaspi Lereschianum",
            "勒雷西遏蓝菜",
            "Indicates presence of a Nickel Deposit nearby",
            "附近有镍矿脉"),
    TUFTED_EVENING_PRIMROSE(
            "tufted_evening_primrose",
            Family.A,
            5,
            false,
            "Tufted Evening Primrose",
            "丛生月见草",
            "Indicates presence of an Uranium Deposit nearby",
            "附近有铀矿脉"),
    NARCISSUS_SHELDONIA(
            "narcissus_sheldonia",
            Family.A,
            6,
            false,
            "Narcissus Sheldonia",
            "谢尔登水仙",
            "Indicates presence of a Platinum Deposit nearby",
            "附近有铂矿脉"),
    ORECHID(
            "orechid",
            Family.A,
            7,
            false,
            "Orechid",
            "矿兰",
            "Indicates presence of an Ore Deposit nearby",
            "附近有矿脉"),
    HEXALILY(
            "hexalily",
            Family.A,
            8,
            false,
            "Hexalily",
            "六色百合",
            "Indicates presence of a Hexorium Deposit nearby",
            "附近有六色矿脉"),
    VINDICATOR_FLOWER(
            "vindicator_flower",
            Family.A,
            9,
            false,
            "Vindicator Flower",
            "维迪花",
            "Vindicates presence of a Rare Earth Deposit nearby",
            "附近有稀土矿脉"),
    SAGEBRUSH(
            "sagebrush",
            Family.B,
            0,
            true,
            "Artemisia Tridentata",
            "三齿蒿",
            "Indicates presence of an Arsenic Deposit nearby",
            "附近有砷矿脉"),
    FOUR_WING_SALTBUSH(
            "four_wing_saltbush",
            Family.B,
            1,
            true,
            "Atriplex Canescens",
            "灰毛滨藜",
            "Indicates presence of an Antimony Deposit nearby",
            "附近有锑矿脉"),
    DESERT_TRUMPET(
            "desert_trumpet",
            Family.B,
            2,
            true,
            "Desert Trumpet",
            "沙漠喇叭花",
            "Indicates presence of a Gold Deposit nearby",
            "附近有金矿脉"),
    COPPER_PLANT(
            "copper_plant",
            Family.B,
            3,
            true,
            "Becium Homblei",
            "铜花",
            "Indicates presence of a Copper Deposit nearby",
            "附近有铜矿脉"),
    PRINCE_S_PLUME(
            "prince_s_plume",
            Family.B,
            4,
            true,
            "Prince's Plume",
            "王子羽",
            "Indicates presence of a Redstone Deposit nearby",
            "附近有红石矿脉"),
    THOMPSONS_LOCOWEED(
            "thompsons_locoweed",
            Family.B,
            5,
            true,
            "Thompsons Locoweed",
            "汤普森疯草",
            "Indicates presence of an Uranium Deposit nearby",
            "附近有铀矿脉"),
    PANDANUS_CANDELABRUM(
            "pandanus_candelabrum",
            Family.B,
            6,
            true,
            "Pandanus Candelabrum",
            "烛台露兜树",
            "Indicates presence of a Diamond Deposit nearby",
            "附近有金刚石矿脉"),
    TUNGSTUS(
            "tungstus",
            Family.B,
            7,
            true,
            "Tungstus",
            "钨花",
            "Indicates presence of a Tungsten Deposit nearby",
            "附近有钨矿脉");

    private final String serialized;
    private final Family family;
    private final int meta;
    private final boolean desert;
    private final String englishName;
    private final String chineseName;
    private final String tooltipEnglish;
    private final String tooltipChinese;

    IndicatorFlower(
            String serialized,
            Family family,
            int meta,
            boolean desert,
            String englishName,
            String chineseName,
            String tooltipEnglish,
            String tooltipChinese) {
        this.serialized = serialized;
        this.family = family;
        this.meta = meta;
        this.desert = desert;
        this.englishName = englishName;
        this.chineseName = chineseName;
        this.tooltipEnglish = tooltipEnglish;
        this.tooltipChinese = tooltipChinese;
    }

    public Family family() {
        return family;
    }

    public int meta() {
        return meta;
    }

    public boolean desert() {
        return desert;
    }

    public String englishName() {
        return englishName;
    }

    public String chineseName() {
        return chineseName;
    }

    public String tooltipEnglish() {
        return tooltipEnglish;
    }

    public String tooltipChinese() {
        return tooltipChinese;
    }

    public String texturePath() {
        return "block/gt6/iconsets/flower_" + serialized;
    }

    public static IndicatorFlower of(Family family, int meta) {
        for (IndicatorFlower flower : values()) {
            if (flower.family == family && flower.meta == meta) {
                return flower;
            }
        }
        throw new IllegalArgumentException(family + " meta " + meta);
    }

    @Override
    public String getSerializedName() {
        return serialized;
    }

    public enum Family {
        A,
        B
    }
}
