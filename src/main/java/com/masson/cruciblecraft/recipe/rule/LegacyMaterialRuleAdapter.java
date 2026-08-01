package com.masson.cruciblecraft.recipe.rule;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.recipe.AnvilRecipe;
import com.masson.cruciblecraft.recipe.CrusherRecipe;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

/** One-cycle compatibility adapters feeding the generic rule projector. */
public final class LegacyMaterialRuleAdapter {
    private LegacyMaterialRuleAdapter() {}

    public static boolean replacedByConcreteOreChain(CrusherRecipe recipe) {
        return recipe.input().equals(MaterialPrefixes.RAW_ORE)
                && recipe.output().equals(MaterialPrefixes.CRUSHED_ORE);
    }

    public static MaterialRule fromCrusher(CrusherRecipe recipe) {
        LinkedHashMap<String, MaterialRule.MaterialOverride> overrides = new LinkedHashMap<>();
        recipe.materialDurations().forEach((material, duration) ->
                overrides.put(material, new MaterialRule.MaterialOverride(
                        Optional.of(Integer.toString(duration)),
                        Optional.empty(),
                        Optional.empty(),
                        Map.of(),
                        Map.of(),
                        Map.of(),
                        Map.of())));
        return new MaterialRule(
                Optional.of(ModRecipeMaps.CRUSHER.id()),
                List.of(item(recipe.input().serializedId(), "1", "10000")),
                List.of(item(
                        recipe.output().serializedId(),
                        Integer.toString(recipe.outputCount()),
                        "10000")),
                List.of(),
                List.of(),
                Integer.toString(recipe.duration()),
                Integer.toString(recipe.power()),
                "0",
                true,
                Optional.empty(),
                overrides,
                List.of(),
                Optional.empty(),
                List.of());
    }

    public static MaterialRule fromAnvil(AnvilRecipe recipe) {
        List<MaterialRule.ItemResource> inputs = new java.util.ArrayList<>();
        inputs.add(item(
                recipe.input().serializedId(),
                Integer.toString(recipe.inputCount()),
                "10000"));
        recipe.secondInput().ifPresent(prefix -> inputs.add(item(
                prefix.serializedId(),
                Integer.toString(recipe.secondInputCount()),
                "10000")));
        List<MaterialRule.ItemResource> outputs = new java.util.ArrayList<>();
        outputs.add(item(
                recipe.output().serializedId(),
                Integer.toString(recipe.outputCount()),
                "10000"));
        recipe.secondaryOutput().ifPresent(prefix -> outputs.add(item(
                prefix.serializedId(),
                Integer.toString(recipe.secondaryOutputCount()),
                Integer.toString((int) Math.round(
                        recipe.secondaryChance() * com.masson.cruciblecraft.recipe.gt.GTRecipe
                                .GUARANTEED_CHANCE)))));
        return new MaterialRule(
                Optional.of(ModRecipeMaps.anvil(recipe.mode()).id()),
                inputs,
                outputs,
                List.of(),
                List.of(),
                "1",
                Long.toString(recipe.recipePower()),
                Integer.toString(recipe.hits()),
                true,
                recipe.material(),
                Map.of(),
                List.of(),
                Optional.empty(),
                List.of());
    }

    private static MaterialRule.ItemResource item(
            String prefix,
            String count,
            String chance) {
        return new MaterialRule.ItemResource(
                Optional.of(prefix), Optional.empty(), count, chance);
    }
}
