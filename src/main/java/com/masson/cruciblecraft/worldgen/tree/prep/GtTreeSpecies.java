package com.masson.cruciblecraft.worldgen.tree.prep;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import net.minecraft.resources.ResourceLocation;

/**
 * The nine {@code WorldgenTree*} identities. Not registered.
 */
public enum GtTreeSpecies {
    RUBBER(
            "rubber",
            "rubber",
            "tree.rubber",
            "WorldgenTreeRubber",
            1,
            5,
            TreeHoleMode.GROW_RUBBER,
            32762,
            "rubber_resin",
            "FL.Resin_Rubber",
            Set.of(
                    "minecraft:taiga",
                    "minecraft:snowy_taiga",
                    "minecraft:old_growth_pine_taiga",
                    "minecraft:old_growth_spruce_taiga")),
    MAPLE(
            "maple",
            "maple",
            "tree.maple",
            "WorldgenTreeMaple",
            1,
            5,
            TreeHoleMode.DRILL_MAPLE,
            32761,
            "",
            "FL.Sap_Maple",
            Set.of(
                    "minecraft:forest",
                    "minecraft:flower_forest",
                    "minecraft:birch_forest",
                    "minecraft:old_growth_birch_forest")),
    WILLOW(
            "willow",
            "willow",
            "tree.willow",
            "WorldgenTreeWillow",
            1,
            4,
            TreeHoleMode.NONE,
            0,
            "",
            "",
            Set.of("minecraft:swamp", "minecraft:mangrove_swamp")),
    BLUE_MAHOE(
            "blue_mahoe",
            "bluemahoe",
            "tree.bluemahoe",
            "WorldgenTreeBlueMahoe",
            1,
            3,
            TreeHoleMode.NONE,
            0,
            "",
            "",
            Set.of(
                    "minecraft:jungle",
                    "minecraft:sparse_jungle",
                    "minecraft:bamboo_jungle")),
    HAZEL(
            "hazel",
            "hazel",
            "tree.hazel",
            "WorldgenTreeHazel",
            1,
            32,
            TreeHoleMode.NONE,
            0,
            "",
            "",
            Set.of(
                    "minecraft:plains",
                    "minecraft:sunflower_plains",
                    "minecraft:meadow")),
    CINNAMON(
            "cinnamon",
            "cinnamon",
            "tree.cinnamon",
            "WorldgenTreeCinnamon",
            1,
            3,
            TreeHoleMode.NONE,
            0,
            "",
            "",
            Set.of(
                    "minecraft:jungle",
                    "minecraft:sparse_jungle",
                    "minecraft:bamboo_jungle")),
    COCONUT(
            "coconut",
            "coconut",
            "tree.coconut",
            "WorldgenTreeCoconut",
            1,
            1,
            TreeHoleMode.NONE,
            0,
            "",
            "",
            Set.of("minecraft:beach")),
    RAINBOWOOD(
            "rainbowood",
            "rainbowood",
            "tree.rainbowood",
            "WorldgenTreeRainbowood",
            1,
            4,
            TreeHoleMode.DRILL_RAINBOWOOD,
            32760,
            "",
            "FL.Sap_Rainbow",
            Set.of()),
    BLUE_SPRUCE(
            "blue_spruce",
            "bluespruce",
            "tree.bluespruce",
            "WorldgenTreeBlueSpruce",
            1,
            32,
            TreeHoleMode.NONE,
            0,
            "",
            "",
            Set.of(
                    "minecraft:windswept_hills",
                    "minecraft:windswept_forest",
                    "minecraft:windswept_gravelly_hills",
                    "minecraft:stony_peaks",
                    "minecraft:jagged_peaks",
                    "minecraft:stony_shore"));

    public static final List<GtTreeSpecies> ALL = List.of(values());
    public static final int RAINBOWOOD_RARE_CHANCE = 8192;
    public static final int HOLE_FLUID_MILLIBUCKETS = 250;

    private final String id;
    private final String gt6TextureKey;
    private final String featureName;
    private final String gt6Class;
    private final int amount;
    private final int probability;
    private final TreeHoleMode holeMode;
    private final int holeSourceId;
    private final String holeItemId;
    private final String holeFluidToken;
    private final Set<String> overworldBiomes;

    GtTreeSpecies(
            String id,
            String gt6TextureKey,
            String featureName,
            String gt6Class,
            int amount,
            int probability,
            TreeHoleMode holeMode,
            int holeSourceId,
            String holeItemId,
            String holeFluidToken,
            Set<String> overworldBiomes) {
        this.id = id;
        this.gt6TextureKey = gt6TextureKey;
        this.featureName = featureName;
        this.gt6Class = gt6Class;
        this.amount = amount;
        this.probability = probability;
        this.holeMode = holeMode;
        this.holeSourceId = holeSourceId;
        this.holeItemId = holeItemId;
        this.holeFluidToken = holeFluidToken;
        this.overworldBiomes = overworldBiomes;
    }

    public String id() {
        return id;
    }

    public String gt6TextureKey() {
        return gt6TextureKey;
    }

    public String featureName() {
        return featureName;
    }

    public String gt6Class() {
        return gt6Class;
    }

    public int amount() {
        return amount;
    }

    public int probability() {
        return probability;
    }

    public TreeHoleMode holeMode() {
        return holeMode;
    }

    public int holeSourceId() {
        return holeSourceId;
    }

    public String holeItemId() {
        return holeItemId;
    }

    public String holeFluidToken() {
        return holeFluidToken;
    }

    public Optional<ResourceLocation> holeFluidId() {
        return switch (this) {
            case MAPLE -> Optional.of(
                    ResourceLocation.fromNamespaceAndPath(
                            "cruciblecraft", "maplesap"));
            case RUBBER -> Optional.of(
                    ResourceLocation.fromNamespaceAndPath(
                            "cruciblecraft", "rubber_tree_sap"));
            case RAINBOWOOD -> Optional.of(
                    ResourceLocation.fromNamespaceAndPath(
                            "cruciblecraft", "rainbow_sap"));
            default -> Optional.empty();
        };
    }

    public Set<String> overworldBiomes() {
        return overworldBiomes;
    }

    public boolean hasHole() {
        return holeMode != TreeHoleMode.NONE;
    }

    public String saplingPath() {
        return "tree/" + id + "_sapling";
    }

    public String logPath() {
        return "tree/" + id + "_log";
    }

    public String beamPath() {
        return "tree/" + id + "_beam";
    }

    public String leavesPath() {
        return "tree/" + id + "_leaves";
    }

    public String holePath() {
        return "tree/" + id + "_hole";
    }

    public String placedFeaturePath() {
        return "tree_" + id;
    }

    public String englishName() {
        return switch (this) {
            case RUBBER -> "Rubber";
            case MAPLE -> "Maple";
            case WILLOW -> "Willow";
            case BLUE_MAHOE -> "Blue Mahoe";
            case HAZEL -> "Hazel";
            case CINNAMON -> "Cinnamon";
            case COCONUT -> "Coconut";
            case RAINBOWOOD -> "Rainbowood";
            case BLUE_SPRUCE -> "Blue Spruce";
        };
    }

    public String chineseName() {
        return switch (this) {
            case RUBBER -> "橡胶";
            case MAPLE -> "枫";
            case WILLOW -> "柳";
            case BLUE_MAHOE -> "蓝木槿";
            case HAZEL -> "榛";
            case CINNAMON -> "肉桂";
            case COCONUT -> "椰";
            case RAINBOWOOD -> "彩虹";
            case BLUE_SPRUCE -> "蓝云杉";
        };
    }

    public String englishHoleName() {
        return switch (this) {
            case RUBBER -> "Rubber Resin Hole";
            case MAPLE -> "Tapped Maple";
            case RAINBOWOOD -> "Tapped Rainbowood";
            default -> englishName() + " Hole";
        };
    }

    public String chineseHoleName() {
        return switch (this) {
            case RUBBER -> "橡胶树脂孔";
            case MAPLE -> "枫糖孔";
            case RAINBOWOOD -> "彩虹树液孔";
            default -> chineseName() + "树洞";
        };
    }

    public enum TreeHoleMode {
        NONE,
        GROW_RUBBER,
        DRILL_MAPLE,
        DRILL_RAINBOWOOD
    }
}
