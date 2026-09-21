package com.masson.cruciblecraft.recipe.rule;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.recipe.crafting.RockGtProcessing;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.resources.ResourceLocation;

/** Authoritative metadata-driven ore-chain rule set. */
public final class MaterialChainRules {
    private static final String SMELTING_GCD =
            "gcd(target_units(smelting), prefix_units(ingot))";
    private static final String SMELTING_INPUT_COUNT =
            "prefix_units(ingot) / " + SMELTING_GCD;
    private static final String SMELTING_OUTPUT_COUNT =
            "target_units(smelting) / " + SMELTING_GCD;
    private static final String ROCK_PULVER_COUNT =
            "prefix_units(rock) * target_units(pulver) / prefix_units(ingot)"
                    + " / prefix_units(small_dust)";
    private static final String ROCK_PULVER_DURATION =
            "16 * prefix_units(rock) * (1 + material.tool.quality)"
                    + " / prefix_units(ingot)";
    private static final List<String> ROCK_PULVER_CONDITIONS = List.of(
            "!material.tag(\"ATOMIC.ANTIMATTER\")",
            "has_registered_for(\"processing_target:pulver\", small_dust)");
    public static final MaterialRule ANVIL_RAW_TO_CRUSHED = new MaterialRule(
            Optional.of(ModRecipeMaps.ANVIL.id()),
            List.of(item(MaterialPrefixes.RAW_ORE, 1)),
            List.of(
                    item(MaterialPrefixes.CRUSHED_ORE, 1),
                    item(MaterialPrefixes.TINY_CRUSHED_ORE, 6)),
            List.of(), List.of(), "1", "10000", "4", true,
            Optional.empty(), Map.of(), List.of(), Optional.empty(), List.of());
    public static final MaterialRule CRUSHER_RAW_TO_CRUSHED = new MaterialRule(
            Optional.of(ModRecipeMaps.CRUSHER.id()),
            List.of(item(MaterialPrefixes.RAW_ORE, 1)),
            List.of(item(MaterialPrefixes.CRUSHED_ORE, 2)),
            List.of(), List.of(), "128", "16", "0", true,
            Optional.empty(),
            Map.of(
                    "lead", durationOverride(256),
                    "nickel", durationOverride(384)),
            List.of(), Optional.empty(), List.of());
    public static final MaterialRule ANVIL_ROCK_TO_PULVER = rockPulver(
            ModRecipeMaps.ANVIL.id());
    public static final MaterialRule CRUSHER_ROCK_TO_PULVER = rockPulver(
            ModRecipeMaps.CRUSHER.id());
    public static final MaterialRule MORTAR_ROCK_TO_PULVER = rockPulver(
            ModRecipeMaps.MORTAR.id());

    public static final List<Definition> ALL = List.of(
            new Definition("crusher/ingot_to_dust",
                    rule(ModRecipeMaps.CRUSHER.id(), MaterialPrefixes.INGOT,
                            List.of(item(MaterialPrefixes.DUST, 1)),
                            List.of(), 128, 16)),
            new Definition("crusher/rock_to_pulver_dust", CRUSHER_ROCK_TO_PULVER),
            new Definition("sluice/crushed_to_washed",
                    washing(ModRecipeMaps.SLUICE.id(), 250, 240, 16)),
            new Definition("bath/crushed_to_washed",
                    washing(ModRecipeMaps.BATH.id(), 100, 160, 24)),
            new Definition("centrifuge/washed_to_centrifuged",
                    rule(ModRecipeMaps.CENTRIFUGE.id(),
                            MaterialPrefixes.WASHED_CRUSHED_ORE,
                            byproducts(MaterialPrefixes.CENTRIFUGED_CRUSHED_ORE),
                            List.of(), 300, 32)),
            new Definition("shredder/centrifuged_to_purified_dust",
                    rule(ModRecipeMaps.SHREDDER.id(),
                            MaterialPrefixes.CENTRIFUGED_CRUSHED_ORE,
                            byproducts(MaterialPrefixes.PURIFIED_DUST),
                            List.of(), 180, 24)),
            new Definition("sifter/purified_to_dust",
                    rule(ModRecipeMaps.SIFTER.id(), MaterialPrefixes.PURIFIED_DUST,
                            List.of(item(MaterialPrefixes.DUST, 1)),
                            List.of(), 160, 16)),
            new Definition("smelter/dust_to_ingot",
                    rule(ModRecipeMaps.SMELTER.id(), MaterialPrefixes.DUST,
                            SMELTING_INPUT_COUNT,
                            List.of(selected(MaterialPrefixes.INGOT,
                                    SMELTING_OUTPUT_COUNT, 10_000,
                                    "processing_target:smelting", false)),
                            List.of(), "800 * (" + SMELTING_INPUT_COUNT + ")", 8,
                            List.of(
                                    "has_registered_for(\"processing_target:smelting\", ingot)"))),
            new Definition("mortar/crushed_to_dust",
                    rule(ModRecipeMaps.MORTAR.id(), MaterialPrefixes.CRUSHED_ORE,
                            List.of(item(MaterialPrefixes.DUST, 1)),
                            List.of(), 120, 12)),
            new Definition("mortar/rock_to_pulver_dust", MORTAR_ROCK_TO_PULVER));
    public static final Set<String> CONCRETE_ORE_CHAIN_PATHS = Set.of(
            "sluice/crushed_to_washed",
            "centrifuge/washed_to_centrifuged",
            "shredder/centrifuged_to_purified_dust",
            "sifter/purified_to_dust",
            "smelter/dust_to_ingot");

    private MaterialChainRules() {}

    /**
     * GT6 crusher / mortar {@code RecipeMapHandlerPrefix(rockGt)} and anvil
     * {@code RecipeMapHandlerPrefixShredding(rockGt → 9 dustSmall)}.
     */
    private static MaterialRule rockPulver(ResourceLocation target) {
        return new MaterialRule(
                Optional.of(target),
                List.of(item(RockGtProcessing.ROCK, 1)),
                List.of(selected(
                        MaterialPrefixes.SMALL_DUST,
                        ROCK_PULVER_COUNT,
                        10_000,
                        "processing_target:pulver",
                        false)),
                List.of(),
                List.of(),
                ROCK_PULVER_DURATION,
                "16",
                "0",
                true,
                Optional.empty(),
                Map.of(),
                ROCK_PULVER_CONDITIONS,
                Optional.empty(),
                List.of());
    }

    private static MaterialRule.MaterialOverride durationOverride(int duration) {
        return new MaterialRule.MaterialOverride(
                Optional.of(Integer.toString(duration)),
                Optional.empty(),
                Optional.empty(),
                Map.of(), Map.of(), Map.of(), Map.of());
    }

    private static MaterialRule washing(
            ResourceLocation target, int water, int duration, long eut) {
        return rule(
                target,
                MaterialPrefixes.CRUSHED_ORE,
                byproducts(MaterialPrefixes.WASHED_CRUSHED_ORE),
                List.of(new MaterialRule.FluidResource(
                        Optional.empty(),
                        Optional.of(ResourceLocation.withDefaultNamespace("water")),
                        Integer.toString(water))),
                duration,
                eut);
    }

    private static List<MaterialRule.ItemResource> byproducts(MaterialPrefix primary) {
        return List.of(
                item(primary, 1),
                selected(MaterialPrefixes.DUST, 1, 2_500, "byproduct:0", true),
                selected(MaterialPrefixes.DUST, 1, 1_000, "byproduct:1", true));
    }

    private static MaterialRule rule(
            ResourceLocation target,
            MaterialPrefix input,
            List<MaterialRule.ItemResource> outputs,
            List<MaterialRule.FluidResource> fluids,
            int duration,
            long eut) {
        return rule(target, input, "1", outputs, fluids, Integer.toString(duration), eut);
    }

    private static MaterialRule rule(
            ResourceLocation target,
            MaterialPrefix input,
            String inputCount,
            List<MaterialRule.ItemResource> outputs,
            List<MaterialRule.FluidResource> fluids,
            String duration,
            long eut) {
        return rule(
                target,
                input,
                inputCount,
                outputs,
                fluids,
                duration,
                eut,
                List.of());
    }

    private static MaterialRule rule(
            ResourceLocation target,
            MaterialPrefix input,
            String inputCount,
            List<MaterialRule.ItemResource> outputs,
            List<MaterialRule.FluidResource> fluids,
            String duration,
            long eut,
            List<String> conditions) {
        return new MaterialRule(
                Optional.of(target),
                List.of(item(input, inputCount)),
                outputs,
                fluids,
                List.of(),
                duration,
                Long.toString(eut),
                "0",
                true,
                Optional.empty(),
                Map.of(),
                conditions,
                Optional.empty(),
                List.of());
    }

    public static MaterialRule.ItemResource item(MaterialPrefix prefix, int count) {
        return item(prefix, Integer.toString(count));
    }

    public static MaterialRule.ItemResource item(MaterialPrefix prefix, String count) {
        return new MaterialRule.ItemResource(
                Optional.of(prefix.serializedId()), Optional.empty(),
                count, "10000");
    }

    public static MaterialRule.ItemResource selected(
            MaterialPrefix prefix, int count, int chance, String selector, boolean optional) {
        return selected(prefix, Integer.toString(count), chance, selector, optional);
    }

    public static MaterialRule.ItemResource selected(
            MaterialPrefix prefix, String count, int chance, String selector, boolean optional) {
        return new MaterialRule.ItemResource(
                Optional.of(prefix.serializedId()), Optional.empty(),
                count, Integer.toString(chance),
                Optional.of(selector), optional);
    }

    public record Definition(String path, MaterialRule rule) {}
}
