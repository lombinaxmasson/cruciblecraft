package com.masson.cruciblecraft.recipe.gt;

import net.minecraft.resources.ResourceLocation;

/**
 * Host/cohort publication-group registry. Historical compact groups and
 * semantic-wave groups share this class; active source must not keep
 * milestone-token aliases.
 */
public final class CompactPublicationGroups {
    public static final ResourceLocation ASSEMBLER_COMPACT =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "assembler/compact");
    public static final ResourceLocation ROASTER_COMPACT =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "roaster/compact");
    public static final ResourceLocation CENTRIFUGE_SINGLETON =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "centrifuge/singleton");
    public static final ResourceLocation CENTRIFUGE_MULTI =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "centrifuge/multi");
    public static final ResourceLocation ELECTROLYZER_SINGLETON =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "electrolyzer/singleton");
    public static final ResourceLocation ELECTROLYZER_MULTI =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "electrolyzer/multi");
    public static final ResourceLocation ASSEMBLER_PLANKS =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "assembler/planks");
    public static final ResourceLocation ASSEMBLER_FIREPROOF =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "assembler/fireproof");
    public static final ResourceLocation ASSEMBLER_PLANKS2 =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "assembler/planks2");
    public static final ResourceLocation SMELTER_STONE =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "smelter/stone");
    public static final ResourceLocation SMELTER_BLOCK =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "smelter/block");
    public static final ResourceLocation DRYING_BLOCK =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "drying/block");
    public static final ResourceLocation BATH_MTE =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "bath/mte");
    public static final ResourceLocation BATH_REMAINDER_EXACT =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "bath/remainder/exact");
    public static final ResourceLocation BATH_REMAINDER_EXACT_MULTI =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "bath/remainder/exact_multi");
    public static final ResourceLocation BATH_IDENTITY_EXACT =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "bath/identity/exact");
    public static final ResourceLocation BATH_IDENTITY_EXACT_MULTI =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "bath/identity/exact_multi");
    public static final ResourceLocation BATH_IDENTITY_TOOL_HEAD =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "bath/identity/tool_head");
    public static final ResourceLocation BATH_TINY_PURIFIED_EXACT_MULTI =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "bath/tiny_purified/exact_multi");

    public static final ResourceLocation SMELTER_ORDINARY_SINGLETON =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "smelter/ordinary_closure/singleton");
    public static final ResourceLocation SMELTER_ORDINARY_MULTIITEM =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "smelter/ordinary_closure/multiitem");
    public static final ResourceLocation SMELTER_ORDINARY_TOOL_HEAD =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "smelter/ordinary_closure/tool_head");
    public static final ResourceLocation SMELTER_ORDINARY_MATERIAL_FORM =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "smelter/ordinary_closure/material_form");
    public static final ResourceLocation SMELTER_ORDINARY_GT_PREFIX =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "smelter/ordinary_closure/gt_prefix");
    public static final ResourceLocation SMELTER_ORDINARY_UNMAPPED =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "smelter/ordinary_closure/unmapped");
    public static final ResourceLocation SMELTER_ORDINARY_ACQUISITION =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "smelter/ordinary_closure/acquisition");
    public static final ResourceLocation MIXER_ORDINARY_CONSTRUCTION_FOAM =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft",
                    "mixer/ordinary_closure/construction_foam_matrix");
    public static final ResourceLocation MIXER_ORDINARY_MATERIAL_MATRIX =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "mixer/ordinary_closure/material_matrix");
    public static final ResourceLocation MIXER_ORDINARY_OPAQUE =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "mixer/ordinary_closure/opaque");

    public static final String ENVELOPE_BRONZE = "bronze";
    public static final String ENVELOPE_GT6_PANEL = "gt6_panel";
    public static final int BRONZE_TANK_CAPACITY = 32_000;
    /** Covers the largest Mixer ordinary-closure fluid amount (33750 mB). */
    public static final int GT6_PANEL_TANK_CAPACITY = 64_000;

    private CompactPublicationGroups() {}

    public static String executionEnvelope(ResourceLocation publicationGroup) {
        if (publicationGroup == null) {
            throw new IllegalArgumentException("publicationGroup");
        }
        String path = publicationGroup.getPath();
        if (path.startsWith("mixer/ordinary_closure/")
                || path.startsWith("centrifuge/")) {
            return ENVELOPE_GT6_PANEL;
        }
        if (path.startsWith("drying/ordinary_closure/")
                || path.startsWith("electrolyzer/ordinary_closure/")
                || path.startsWith("autoclave/ordinary_closure/")
                || path.startsWith("compressor/ordinary_closure/")) {
            return ENVELOPE_BRONZE;
        }
        return ENVELOPE_BRONZE;
    }
}
