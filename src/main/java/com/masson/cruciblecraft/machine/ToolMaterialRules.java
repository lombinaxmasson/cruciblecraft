package com.masson.cruciblecraft.machine;

import java.util.Collection;
import java.util.Set;

import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.GT6MaterialMetadata.ToolStats;
import com.masson.cruciblecraft.material.def.MaterialDefinition;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;

/**
 * Runtime projection of the checked-in tool policy.
 *
 * <p>The authoritative source is GregTech6 revision
 * {@value #GT6_SOURCE_REVISION}: prefix gates come from {@code OP.java}
 * lines 232-248, listener gates from {@code Loader_Tools.java} lines
 * 295-327, and full-tool gates from {@code Loader_Tools.java} lines 333-350.
 * {@code tools/t4_tool_policy.json} records the exact line for every tool and
 * the source/reason for every shared predicate.
 *
 * <p>Two intentional asymmetries are source observations, not local guesses:
 * the Wrench and Monkey Wrench listeners at {@code Loader_Tools.java:310-311}
 * omit {@code COATED}, while other relevant listeners include it;
 * Screwdriver's prefix/listener pair at
 * {@code OP.java:248}/{@code Loader_Tools.java:306} omits the
 * {@code BOUNCY}/{@code STRETCHY} exclusions used by File, Chisel, Saw, and
 * Wrench.
 */
public final class ToolMaterialRules {
    public static final String GT6_SOURCE_REVISION =
            "3703e40308c8c030763fd6297dea8b210d2a77b1";
    public static final String TOOL_DOMAIN_TAG = "PROPERTIES.HAS_TOOL_STATS";
    public static final String ANTIMATTER_TAG = "ATOMIC.ANTIMATTER";
    public static final String COATED_TAG = "COMPOUNDS.COATED";
    public static final String WOOD_TAG = "PROPERTIES.WOOD";
    public static final String BOUNCY_TAG = "PROPERTIES.BOUNCY";
    public static final String STRETCHY_TAG = "PROPERTIES.STRETCHY";
    public static final float MIN_MINING_SPEED = 0.1F;
    public static final float MAX_MINING_SPEED = 20.0F;
    private static final String GENERIC_WOOD_ID = "wood";

    private ToolMaterialRules() {}

    public static boolean isAllowed(ToolKind kind, String materialId) {
        if (materialId == null
                || materialId.isBlank()
                || !MaterialCatalog.isBootstrapped()
                || !MaterialCatalog.contains(materialId)) {
            return false;
        }
        MaterialDefinition material = MaterialCatalog.require(materialId);
        return material.gt6Metadata()
                .filter(metadata -> kind.isEligible(
                        material.id(),
                        metadata.tool(),
                        metadata.materialTags()))
                .isPresent();
    }

    public static ToolStats requireStats(ToolKind kind, String materialId) {
        if (!isAllowed(kind, materialId)) {
            throw new IllegalArgumentException(
                    "Unsupported " + kind.serializedName()
                            + " material: " + materialId);
        }
        return MaterialCatalog.require(materialId)
                .gt6Metadata()
                .orElseThrow()
                .tool();
    }

    public static int durability(ToolKind kind, String materialId) {
        long value = requireStats(kind, materialId).durability();
        return (int) Math.max(1L, Math.min((long) Integer.MAX_VALUE, value));
    }

    public static Tier miningTier(ToolKind kind, String materialId) {
        int quality = requireStats(kind, materialId).quality();
        return switch (quality) {
            case 0 -> Tiers.WOOD;
            case 1 -> Tiers.STONE;
            case 2 -> Tiers.IRON;
            case 3 -> Tiers.DIAMOND;
            default -> Tiers.NETHERITE;
        };
    }

    public static Tier miningTier(String materialId) {
        return miningTier(ToolKind.PICKAXE, materialId);
    }

    public static float miningSpeed(ToolKind kind, String materialId) {
        double speed = requireStats(kind, materialId).speed();
        return (float) Math.max(
                MIN_MINING_SPEED,
                Math.min((double) MAX_MINING_SPEED, speed));
    }

    public static float miningSpeed(String materialId) {
        return miningSpeed(ToolKind.PICKAXE, materialId);
    }

    public enum ToolKind {
        PICKAXE("pickaxe", 1L, 0, Integer.MAX_VALUE),
        SHOVEL("shovel", 1L, 0, Integer.MAX_VALUE),
        AXE("axe", 1L, 0, Integer.MAX_VALUE),
        HOE("hoe", 1L, 0, Integer.MAX_VALUE),
        SWORD("sword", 1L, 0, Integer.MAX_VALUE),
        SMITHING_HAMMER(
                "smithing_hammer",
                1L,
                1,
                Integer.MAX_VALUE,
                WOOD_TAG,
                BOUNCY_TAG,
                STRETCHY_TAG),
        FILE("file", 2L, 0, 2, BOUNCY_TAG, STRETCHY_TAG),
        CHISEL(
                "chisel",
                2L,
                0,
                Integer.MAX_VALUE,
                BOUNCY_TAG,
                STRETCHY_TAG),
        SAW(
                "saw",
                2L,
                0,
                Integer.MAX_VALUE,
                BOUNCY_TAG,
                STRETCHY_TAG),
        SCREWDRIVER("screwdriver", 2L, 0, Integer.MAX_VALUE),
        WRENCH(
                "wrench",
                2L,
                1,
                Integer.MAX_VALUE,
                BOUNCY_TAG,
                STRETCHY_TAG),
        MONKEY_WRENCH(
                "monkey_wrench",
                2L,
                1,
                Integer.MAX_VALUE,
                BOUNCY_TAG,
                STRETCHY_TAG),
        // GT6 Loader_Tools.java:324 listener And(ANTIMATTER.NOT, Wood.NOT,
        // BOUNCY.NOT, STRETCHY.NOT, typemin(2)) — no qualmin, COATED allowed.
        WIRE_CUTTER(
                "wire_cutter",
                2L,
                0,
                Integer.MAX_VALUE,
                BOUNCY_TAG,
                STRETCHY_TAG),
        // GT6 Loader_Tools.java:322 Knife: And(ANTIMATTER.NOT, Wood.NOT).
        // Finished tool in the grid ({"fP","hH"}); COATED is not excluded.
        KNIFE("knife", 1L, 0, Integer.MAX_VALUE),
        // GT6 Loader_Tools.java:329 Club: And(ANTIMATTER.NOT, Wood.NOT).
        CLUB("club", 1L, 0, Integer.MAX_VALUE),
        SPADE("spade", 1L, 0, Integer.MAX_VALUE),
        DOUBLE_AXE(
                "double_axe",
                2L,
                0,
                Integer.MAX_VALUE),
        SENSE("sense", 2L, 0, Integer.MAX_VALUE),
        PLOW("plow", 2L, 0, Integer.MAX_VALUE),
        CONSTRUCTION_PICK(
                "construction_pick",
                2L,
                0,
                Integer.MAX_VALUE),
        GEM_PICK("gem_pick", 1L, 0, Integer.MAX_VALUE),
        BUILDER_WAND("builder_wand", 2L, 0, Integer.MAX_VALUE),
        UNIVERSAL_SPADE("universal_spade", 2L, 0, Integer.MAX_VALUE),
        CROWBAR("crowbar", 1L, 0, Integer.MAX_VALUE),
        PLUNGER("plunger", 1L, 0, Integer.MAX_VALUE),
        SCOOP("scoop", 1L, 0, Integer.MAX_VALUE),
        BUTCHERY_KNIFE(
                "butchery_knife",
                2L,
                0,
                Integer.MAX_VALUE,
                BOUNCY_TAG,
                STRETCHY_TAG),
        BRANCH_CUTTER(
                "branch_cutter",
                2L,
                0,
                Integer.MAX_VALUE,
                BOUNCY_TAG,
                STRETCHY_TAG),
        SCISSORS(
                "scissors",
                2L,
                0,
                Integer.MAX_VALUE,
                BOUNCY_TAG,
                STRETCHY_TAG),
        PINCERS("pincers", 2L, 0, Integer.MAX_VALUE),
        // GT6 Loader_Tools.java:328: Or(WOOD, BOUNCY, STRETCHY) and COATED.NOT.
        SOFT_HAMMER("soft_hammer", 1L, 0, Integer.MAX_VALUE),
        BENDING_CYLINDER(
                "bending_cylinder",
                2L,
                0,
                Integer.MAX_VALUE),
        BENDING_CYLINDER_SMALL(
                "bending_cylinder_small",
                2L,
                0,
                Integer.MAX_VALUE),
        HAND_DRILL(
                "hand_drill",
                2L,
                2,
                Integer.MAX_VALUE,
                WOOD_TAG,
                BOUNCY_TAG,
                STRETCHY_TAG),
        ROLLING_PIN("rolling_pin", 1L, 0, Integer.MAX_VALUE),
        FLINT_AND_TINDER("flint_and_tinder", 1L, 0, Integer.MAX_VALUE),
        POCKET_MULTITOOL(
                "pocket_multitool",
                3L,
                1,
                Integer.MAX_VALUE,
                WOOD_TAG,
                BOUNCY_TAG,
                STRETCHY_TAG);

        private final String serializedName;
        private final long minTypes;
        private final int minQuality;
        private final int maxQuality;
        private final Set<String> excludedTags;

        ToolKind(
                String serializedName,
                long minTypes,
                int minQuality,
                int maxQuality,
                String... excludedTags) {
            this.serializedName = serializedName;
            this.minTypes = minTypes;
            this.minQuality = minQuality;
            this.maxQuality = maxQuality;
            this.excludedTags = Set.of(excludedTags);
        }

        public String serializedName() {
            return serializedName;
        }

        public long minTypes() {
            return minTypes;
        }

        public int minQuality() {
            return minQuality;
        }

        /**
         * Tests the imported GT6 listener and prefix intersection for this
         * concrete tool. This intentionally does not use NO_ADVANCED_TOOLS:
         * GT6 defines each tool's domain from the source fields below.
         */
        public boolean isEligible(
                String materialId,
                ToolStats stats,
                Collection<String> materialTags) {
            if (materialId == null
                    || stats == null
                    || materialTags == null
                    || GENERIC_WOOD_ID.equals(materialId)
                    || !materialTags.contains(TOOL_DOMAIN_TAG)
                    || materialTags.contains(ANTIMATTER_TAG)
                    || (!allowsCoated() && materialTags.contains(COATED_TAG))
                    || stats.types() < minTypes
                    || stats.quality() < minQuality
                    || stats.quality() > maxQuality) {
                return false;
            }
            if (this == SOFT_HAMMER
                    && !materialTags.contains(WOOD_TAG)
                    && !materialTags.contains(BOUNCY_TAG)
                    && !materialTags.contains(STRETCHY_TAG)) {
                return false;
            }
            return excludedTags.stream().noneMatch(materialTags::contains);
        }

        /**
         * GT6 listeners that omit {@code COATED.NOT}. Harvest heads keep the
         * coated exclusion used by pickaxe/shovel/axe.
         */
        public boolean allowsCoated() {
            return this == WRENCH
                    || this == MONKEY_WRENCH
                    || this == WIRE_CUTTER
                    || this == KNIFE
                    || this == CLUB
                    || this == CROWBAR
                    || this == PLUNGER
                    || this == SCOOP
                    || this == UNIVERSAL_SPADE
                    || this == BUILDER_WAND
                    || this == GEM_PICK
                    || this == PINCERS
                    || this == BENDING_CYLINDER
                    || this == BENDING_CYLINDER_SMALL
                    || this == HAND_DRILL
                    || this == BUTCHERY_KNIFE
                    || this == BRANCH_CUTTER
                    || this == SCISSORS
                    || this == ROLLING_PIN
                    || this == FLINT_AND_TINDER
                    || this == POCKET_MULTITOOL;
        }
    }
}
