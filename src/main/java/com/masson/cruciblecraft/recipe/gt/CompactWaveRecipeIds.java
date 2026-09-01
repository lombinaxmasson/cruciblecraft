package com.masson.cruciblecraft.recipe.gt;

import java.util.List;

import com.masson.cruciblecraft.CrucibleCraft;

import net.minecraft.resources.ResourceLocation;

/**
 * Path-prefix registry for compact host recipes. New waves must not add
 * milestone-token helpers or milestone-numbered recipe paths.
 */
public final class CompactWaveRecipeIds {
    public static final List<String> RECIPE_PREFIXES = List.of(
            "assembler/compact/",
            "roaster/compact/",
            "centrifuge/compact/",
            "electrolyzer/compact/",
            "assembler/wood/",
            "smelter/stone/",
            "smelter/block/",
            "drying/block/",
            "bath/mte/",
            "bath/remainder/",
            "bath/identity/",
            "bath/tiny_purified/",
            "smelter/ordinary_closure/",
            "mixer/ordinary_closure/",
            "drying/ordinary_closure/",
            "electrolyzer/ordinary_closure/",
            "centrifuge/ordinary_closure/",
            "autoclave/ordinary_closure/",
            "compressor/ordinary_closure/",
            "smelter/deferred_recycling/");
    public static final List<String> SUPPORT_PREFIXES = List.of(
            "player_path_support/centrifuge/",
            "player_path_support/electrolyzer/",
            "player_path_support/assembler_wood/",
            "player_path_support/smelter_stone/",
            "player_path_support/block_object/",
            "player_path_support/bath_mte/",
            "player_path_support/bath_remainder/",
            "player_path_support/bath_identity/",
            "player_path_support/bath_tiny_purified/",
            "smelter_ordinary_closure_player_path_support/",
            "mixer_ordinary_closure_player_path_support/",
            "drying_ordinary_closure_player_path_support/",
            "electrolyzer_ordinary_closure_player_path_support/",
            "centrifuge_ordinary_closure_player_path_support/",
            "autoclave_ordinary_closure_player_path_support/",
            "compressor_ordinary_closure_player_path_support/");
    public static final List<String> BATH_REMAINDER_PREFIXES = List.of(
            "bath/remainder/",
            "bath/identity/",
            "bath/tiny_purified/");
    public static final List<String> BLOCK_OBJECT_PREFIXES = List.of(
            "smelter/block/",
            "drying/block/");

    private CompactWaveRecipeIds() {}

    public static boolean isCompactHostRecipe(ResourceLocation id) {
        return matchesAny(id, RECIPE_PREFIXES) || matchesAny(id, SUPPORT_PREFIXES);
    }

    public static boolean isSemanticWaveRecipe(ResourceLocation id) {
        return pathStartsWith(id, "smelter/ordinary_closure/")
                || pathStartsWith(id, "mixer/ordinary_closure/")
                || pathStartsWith(id, "drying/ordinary_closure/")
                || pathStartsWith(id, "electrolyzer/ordinary_closure/")
                || pathStartsWith(id, "centrifuge/ordinary_closure/")
                || pathStartsWith(id, "autoclave/ordinary_closure/")
                || pathStartsWith(id, "compressor/ordinary_closure/")
                || pathStartsWith(id, "smelter/deferred_recycling/")
                || pathStartsWith(id, "smelter_ordinary_closure_player_path_support/")
                || pathStartsWith(id, "mixer_ordinary_closure_player_path_support/")
                || pathStartsWith(id, "drying_ordinary_closure_player_path_support/")
                || pathStartsWith(id, "electrolyzer_ordinary_closure_player_path_support/")
                || pathStartsWith(id, "centrifuge_ordinary_closure_player_path_support/")
                || pathStartsWith(id, "autoclave_ordinary_closure_player_path_support/")
                || pathStartsWith(id, "compressor_ordinary_closure_player_path_support/");
    }

    public static boolean isBathRemainderCompactRecipe(ResourceLocation id) {
        return matchesAny(id, BATH_REMAINDER_PREFIXES);
    }

    public static boolean isCentrifugeCompactRecipe(ResourceLocation id) {
        return pathStartsWith(id, "centrifuge/compact/")
                || pathStartsWith(id, "player_path_support/centrifuge/");
    }

    public static boolean isSmelterDeferredRecyclingRecipe(ResourceLocation id) {
        return pathStartsWith(id, "smelter/deferred_recycling/");
    }

    public static boolean isRoasterRecoveryRecipe(ResourceLocation id) {
        return pathStartsWith(id, "player_path_recovery/roaster/");
    }

    public static boolean matchesAny(ResourceLocation id, List<String> prefixes) {
        if (id == null || !CrucibleCraft.MODID.equals(id.getNamespace())) {
            return false;
        }
        String path = id.getPath();
        for (String prefix : prefixes) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    public static boolean pathStartsWith(ResourceLocation id, String prefix) {
        return id != null
                && CrucibleCraft.MODID.equals(id.getNamespace())
                && id.getPath().startsWith(prefix);
    }

    public static String gradleNamespace(String waveSlug) {
        if (waveSlug == null || waveSlug.isBlank() || waveSlug.indexOf('/') < 0) {
            throw new IllegalArgumentException(
                    "semantic wave slug required, not " + waveSlug);
        }
        if (waveSlug.length() >= 2
                && (waveSlug.charAt(0) == 'T' || waveSlug.charAt(0) == 't')
                && Character.isDigit(waveSlug.charAt(1))) {
            throw new IllegalArgumentException(
                    "semantic schema rejects milestone token " + waveSlug);
        }
        return "cruciblecraft_wave_"
                + waveSlug.replace('/', '_').replace('-', '_');
    }
}
