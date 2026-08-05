package com.masson.cruciblecraft.recipe.rule;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.resources.ResourceLocation;

/** Authoritative metadata-driven T2 ore-chain rule set. */
public final class T2ChainRules {
    private static final String SMELTING_GCD =
            "gcd(target_units(smelting), prefix_units(ingot))";
    private static final String SMELTING_INPUT_COUNT =
            "prefix_units(ingot) / " + SMELTING_GCD;
    private static final String SMELTING_OUTPUT_COUNT =
            "target_units(smelting) / " + SMELTING_GCD;
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

    public static final List<Definition> ALL = List.of(
            new Definition("crusher/ingot_to_dust",
                    rule(ModRecipeMaps.CRUSHER.id(), MaterialPrefixes.INGOT,
                            List.of(item(MaterialPrefixes.DUST, 1)),
                            List.of(), 128, 16)),
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
                            List.of(), 120, 12)));
    public static final Set<String> CONCRETE_ORE_CHAIN_PATHS = Set.of(
            "sluice/crushed_to_washed",
            "centrifuge/washed_to_centrifuged",
            "shredder/centrifuged_to_purified_dust",
            "sifter/purified_to_dust",
            "smelter/dust_to_ingot");

    private T2ChainRules() {}

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
