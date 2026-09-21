package com.masson.cruciblecraft.recipe.rule;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.material.def.GT6MaterialMetadata;
import com.masson.cruciblecraft.material.def.ThermalProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.fml.loading.LoadingModList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaterialRuleExpansionTest {
    private static final ResourceLocation CRUSHER =
            ResourceLocation.fromNamespaceAndPath("cruciblecraft", "crusher");
    private static final ResourceLocation ANVIL =
            ResourceLocation.fromNamespaceAndPath("cruciblecraft", "anvil");

    @Test
    void currentCrusherAndAnvilParityAndPrefixCandidateFiltering() {
        List<MaterialDefinition> materials = List.of(
                material("iron", 2, MaterialPrefixes.RAW_ORE, MaterialPrefixes.CRUSHED_ORE,
                        MaterialPrefixes.INGOT, MaterialPrefixes.PLATE),
                material("lead", 1, MaterialPrefixes.RAW_ORE, MaterialPrefixes.CRUSHED_ORE),
                material("tin", 1, MaterialPrefixes.INGOT));
        MaterialRule crusher = new MaterialRule(
                Optional.of(CRUSHER),
                List.of(prefix(MaterialPrefixes.RAW_ORE, "1", "10000")),
                List.of(prefix(MaterialPrefixes.CRUSHED_ORE, "2", "10000")),
                List.of(), List.of(),
                "128", "16", "0", true, Optional.empty(),
                Map.of("lead", overrideDuration("256")),
                List.of(), Optional.empty(), List.of());

        List<MaterialRuleExpansion.Plan> crushed =
                MaterialRuleExpansion.expandFactualPlans(
                        id("crusher/raw"), crusher, materials);
        assertEquals(2, crushed.size(), "only materials with both referenced prefixes expand");
        assertEquals(
                List.of("crusher/raw/iron", "crusher/raw/lead"),
                crushed.stream().map(value -> value.id().getPath()).toList());
        MaterialRuleExpansion.Plan lead = crushed.get(1);
        assertEquals(
                Optional.of(MaterialPrefixes.RAW_ORE.serializedId()),
                lead.itemInputs().getFirst().resource().prefix());
        assertEquals(
                Optional.of(MaterialPrefixes.CRUSHED_ORE.serializedId()),
                lead.itemOutputs().getFirst().resource().prefix());
        assertEquals(1, lead.itemInputs().getFirst().amount());
        assertEquals(2, lead.itemOutputs().getFirst().amount());
        assertEquals(256, lead.duration());
        assertEquals(16, lead.eut());
        assertEquals(0, lead.specialValue());
        assertEquals(10_000, lead.itemOutputs().getFirst().chance());

        bootstrapVanillaItems();
        var projectedCrusher = MaterialRuleExpansion.expand(
                id("crusher/raw"),
                crusher,
                materials,
                resolver(Map.of(MaterialPrefixes.CRUSHED_ORE, Items.IRON_INGOT)),
                MaterialRuleExpansion.FormIndexes.factualOnly(materials));
        assertEquals(List.of(id("crusher/raw/iron"), id("crusher/raw/lead")),
                projectedCrusher.stream().map(MaterialRuleExpansion.Expanded::id).toList());
        var leadRecipe = projectedCrusher.get(1).recipe();
        assertEquals(1, leadRecipe.itemInputs().size());
        assertTrue(leadRecipe.itemInputs().getFirst().test(new ItemStack(Items.STONE)));
        assertEquals(List.of(1), leadRecipe.itemInputCounts());
        assertSame(Items.IRON_INGOT, leadRecipe.itemOutputs().getFirst().getItem());
        assertEquals(2, leadRecipe.itemOutputs().getFirst().getCount());
        assertEquals(List.of(10_000), leadRecipe.outputChances());
        assertEquals(256, leadRecipe.duration());
        assertEquals(16, leadRecipe.eut());
        assertEquals(0, leadRecipe.specialValue());

        MaterialRule anvil = new MaterialRule(
                Optional.of(ANVIL),
                List.of(prefix(MaterialPrefixes.INGOT, "1", "10000")),
                List.of(prefix(MaterialPrefixes.PLATE, "1", "10000")),
                List.of(), List.of(),
                "1", "10000", "4", true, Optional.empty(),
                Map.of(), List.of(), Optional.empty(), List.of());
        var plates = MaterialRuleExpansion.expandFactualPlans(
                id("anvil/plate"), anvil, materials);
        assertEquals(1, plates.size());
        assertEquals("anvil/plate/iron", plates.getFirst().id().getPath());
        assertEquals(
                Optional.of(MaterialPrefixes.INGOT.serializedId()),
                plates.getFirst().itemInputs().getFirst().resource().prefix());
        assertEquals(
                Optional.of(MaterialPrefixes.PLATE.serializedId()),
                plates.getFirst().itemOutputs().getFirst().resource().prefix());
        assertEquals(1, plates.getFirst().duration());
        assertEquals(10_000, plates.getFirst().eut());
        assertEquals(4, plates.getFirst().specialValue());
        assertEquals(10_000, plates.getFirst().itemOutputs().getFirst().chance());
        var projectedAnvil = MaterialRuleExpansion.expand(
                id("anvil/plate"),
                anvil,
                materials,
                resolver(Map.of(MaterialPrefixes.PLATE, Items.GOLD_INGOT)),
                MaterialRuleExpansion.FormIndexes.factualOnly(materials));
        assertEquals(1, projectedAnvil.size());
        assertEquals(id("anvil/plate/iron"), projectedAnvil.getFirst().id());
        var anvilRecipe = projectedAnvil.getFirst().recipe();
        assertEquals(List.of(1), anvilRecipe.itemInputCounts());
        assertSame(Items.GOLD_INGOT, anvilRecipe.itemOutputs().getFirst().getItem());
        assertEquals(1, anvilRecipe.itemOutputs().getFirst().getCount());
        assertEquals(List.of(10_000), anvilRecipe.outputChances());
        assertEquals(1, anvilRecipe.duration());
        assertEquals(10_000, anvilRecipe.eut());
        assertEquals(4, anvilRecipe.specialValue());
    }

    @Test
    void factualAndRegisteredFormPredicatesRemainDistinct() {
        MaterialDefinition stone = material(
                "stone", 1, MaterialPrefixes.PLATE, MaterialPrefixes.ROD);
        List<MaterialDefinition> materials = List.of(stone);
        MaterialRule rule = new MaterialRule(
                Optional.of(CRUSHER),
                List.of(prefix(MaterialPrefixes.ROD, "1", "10000")),
                List.of(prefix(MaterialPrefixes.ROD, "1", "10000")),
                List.of(),
                List.of(),
                "1",
                "1",
                "0",
                true,
                Optional.of("stone"),
                Map.of(),
                List.of(
                        "material.is(stone)",
                        "has_form(plate)",
                        "!has_registered(plate)",
                        "has_registered(rod)"),
                Optional.empty(),
                List.of());

        assertEquals(
                1,
                MaterialRuleExpansion.expandPlansWithRegisteredForms(
                                id("registered_forms"),
                                rule,
                                materials,
                                Map.of("stone", List.of(MaterialPrefixes.ROD)))
                        .size());
        assertTrue(MaterialRuleExpansion.expandFactualPlans(
                id("factual_only"), rule, materials).isEmpty());
        IllegalArgumentException unknownMaterial = assertThrows(
                IllegalArgumentException.class,
                () -> MaterialRuleExpansion.expandFactualPlans(
                        id("unknown_material"),
                        withConditions(rule, List.of("material.is(stnoe)")),
                        materials));
        assertTrue(unknownMaterial.getMessage().contains("test:unknown_material"));
        assertTrue(unknownMaterial.getMessage().contains("stnoe"));
    }

    @Test
    void tagThermalAndDamageFactsDriveExpansionWithMissingMetadataDefaults() {
        MaterialDefinition reactive = material(
                "reactive", 0, MaterialPrefixes.INGOT, MaterialPrefixes.DUST)
                .withImportedMetadata(metadata(
                        List.of(),
                        Map.of(),
                        List.of("PROCESSING.MORTAR_GRINDABLE"),
                        2.5,
                        4.0));
        MaterialDefinition plain = material(
                "plain", 0, MaterialPrefixes.INGOT, MaterialPrefixes.DUST);
        MaterialRule rule = new MaterialRule(
                Optional.of(CRUSHER),
                List.of(prefix(MaterialPrefixes.INGOT, "1", "10000")),
                List.of(prefix(MaterialPrefixes.DUST, "1", "10000")),
                List.of(), List.of(), "20", "8", "0", true,
                Optional.empty(), Map.of(),
                List.of(
                        "material.tag(\"PROCESSING.MORTAR_GRINDABLE\")",
                        "material.thermal.melting_point >= 1000",
                        "material.explosion_damage == 2.5",
                        "material.heat_damage == 4",
                        "has_registered(ingot) && has_registered(dust)"),
                Optional.empty(), List.of());
        Map<String, List<MaterialPrefix>> registered = Map.of(
                "reactive", List.of(MaterialPrefixes.INGOT, MaterialPrefixes.DUST),
                "plain", List.of(MaterialPrefixes.INGOT, MaterialPrefixes.DUST));

        List<MaterialRuleExpansion.Plan> plans =
                MaterialRuleExpansion.expandPlansWithRegisteredForms(
                        id("material_facts"), rule, List.of(reactive, plain), registered);
        assertEquals(1, plans.size());
        assertEquals("reactive", plans.getFirst().materialId());

        MaterialRule missingDefaults = withConditions(
                rule,
                List.of(
                        "!material.tag(\"PROCESSING.MORTAR_GRINDABLE\")",
                        "material.explosion_damage == 0",
                        "material.heat_damage == 0"));
        plans = MaterialRuleExpansion.expandPlansWithRegisteredForms(
                id("missing_metadata_defaults"),
                missingDefaults,
                List.of(reactive, plain),
                registered);
        assertEquals(1, plans.size());
        assertEquals("plain", plans.getFirst().materialId());
    }

    @Test
    void pipeRecipePredicateSelectsTheExactGaugeAndRejectsAnyGaugeAlias() {
        GT6MaterialMetadata.FluidPipeProperties enabled =
                new GT6MaterialMetadata.FluidPipeProperties(
                        100, 400, false, false, false, false,
                        false, false, true, true);
        GT6MaterialMetadata.FluidPipeProperties disabled =
                new GT6MaterialMetadata.FluidPipeProperties(
                        200, 400, false, false, false, false,
                        false, false, false, true);
        MaterialDefinition mixed = material(
                "mixed",
                1,
                MaterialPrefixes.PLATE,
                MaterialPrefixes.TINY_FLUID_PIPE,
                MaterialPrefixes.SMALL_FLUID_PIPE)
                .withImportedMetadata(new GT6MaterialMetadata(
                        1,
                        "Mixed",
                        List.of(),
                        "solid",
                        Optional.empty(),
                        new GT6MaterialMetadata.SourceThermal(
                                1000, 726.85, 2000, 1726.85,
                                3000, 2726.85, 7),
                        GT6MaterialMetadata.ToolStats.EMPTY,
                        List.of(),
                        Map.of(),
                        List.of(),
                        List.of(),
                        0,
                        0,
                        Optional.empty(),
                        Map.of(),
                        new GT6MaterialMetadata.PipeProperties(
                                Map.of(
                                        "pipeTiny", enabled,
                                        "pipeSmall", disabled),
                                Map.of())));
        MaterialRule tiny = pipeRule(
                MaterialPrefixes.TINY_FLUID_PIPE,
                "fluid_pipe_recipe(tiny_fluid_pipe) == 1");
        MaterialRule small = pipeRule(
                MaterialPrefixes.SMALL_FLUID_PIPE,
                "fluid_pipe_recipe(small_fluid_pipe) == 1");

        assertEquals(
                1,
                MaterialRuleExpansion.expandFactualPlans(
                                id("pipe/tiny"), tiny, List.of(mixed))
                        .size());
        assertTrue(MaterialRuleExpansion.expandFactualPlans(
                id("pipe/small"), small, List.of(mixed)).isEmpty());
        assertThrows(
                IllegalArgumentException.class,
                () -> MaterialRuleExpansion.expandFactualPlans(
                        id("pipe/unknown"),
                        pipeRule(
                                MaterialPrefixes.TINY_FLUID_PIPE,
                                "fluid_pipe_recipe(pipeTypo) == 1"),
                        List.of(mixed)));
        assertThrows(
                IllegalArgumentException.class,
                () -> MaterialRuleExpansion.expandFactualPlans(
                        id("pipe/forbidden_any"),
                        pipeRule(
                                MaterialPrefixes.TINY_FLUID_PIPE,
                                "material.pipe.fluid_recipe == 1"),
                        List.of(mixed)));
    }

    @Test
    void unknownTagsAndRequiredResourcesFailLoudly() {
        MaterialDefinition material = material(
                "iron", 1, MaterialPrefixes.INGOT, MaterialPrefixes.DUST)
                .withImportedMetadata(metadata(
                        List.of(),
                        Map.of(),
                        List.of("PROCESSING.MORTAR_GRINDABLE"),
                        0.0,
                        0.0));
        MaterialRule generic = new MaterialRule(
                Optional.of(CRUSHER),
                List.of(prefix(MaterialPrefixes.INGOT, "1", "10000")),
                List.of(prefix(MaterialPrefixes.DUST, "1", "10000")),
                List.of(), List.of(), "20", "8", "0", true,
                Optional.empty(), Map.of(),
                List.of("material.tag(\"PROCESSING.MORTAR_GRINDIBLE\")"),
                Optional.empty(), List.of());

        IllegalArgumentException unknownTag = assertThrows(
                IllegalArgumentException.class,
                () -> MaterialRuleExpansion.expandFactualPlans(
                        id("unknown_tag"), generic, List.of(material)));
        assertTrue(unknownTag.getMessage().contains("unknown_tag"));
        assertTrue(unknownTag.getMessage().contains(
                "PROCESSING.MORTAR_GRINDIBLE"));

        MaterialRule explicit = new MaterialRule(
                generic.target(),
                generic.itemInputs(),
                generic.itemOutputs(),
                generic.fluidInputs(),
                generic.fluidOutputs(),
                generic.duration(),
                generic.eut(),
                generic.specialValue(),
                generic.canBeBuffered(),
                Optional.of("iron"),
                generic.materialOverrides(),
                List.of(),
                generic.tuning(),
                generic.unification());
        IllegalArgumentException missingForm = assertThrows(
                IllegalArgumentException.class,
                () -> MaterialRuleExpansion.expandPlansWithRegisteredForms(
                        id("missing_explicit_form"),
                        explicit,
                        List.of(material),
                        Map.of("iron", List.of(MaterialPrefixes.INGOT))));
        assertTrue(missingForm.getMessage().contains("missing_explicit_form"));
        assertTrue(missingForm.getMessage().contains("cruciblecraft:dust"));

        bootstrapVanillaItems();
        IllegalArgumentException unresolvedOutput = assertThrows(
                IllegalArgumentException.class,
                () -> MaterialRuleExpansion.expand(
                        id("unresolved_required_output"),
                        explicit,
                        List.of(material),
                        resolver(Map.of()),
                        MaterialRuleExpansion.FormIndexes.factualOnly(
                                List.of(material))));
        assertTrue(unresolvedOutput.getMessage().contains(
                "required item output"));
        assertTrue(unresolvedOutput.getMessage().contains(
                "cruciblecraft:dust"));
    }

    @Test
    void conditionsAndArbitraryMultiIoExpandGenerically() {
        List<MaterialDefinition> materials = List.of(
                material("iron", 2, MaterialPrefixes.INGOT, MaterialPrefixes.DUST,
                        MaterialPrefixes.PLATE, MaterialPrefixes.ROD),
                material("tin", 1, MaterialPrefixes.INGOT, MaterialPrefixes.DUST,
                        MaterialPrefixes.PLATE, MaterialPrefixes.ROD));
        MaterialRule rule = new MaterialRule(
                Optional.of(CRUSHER),
                List.of(
                        prefix(MaterialPrefixes.INGOT, "2", "10000"),
                        prefix(MaterialPrefixes.DUST, "1", "10000")),
                List.of(
                        prefix(MaterialPrefixes.PLATE, "1", "10000"),
                        prefix(MaterialPrefixes.ROD, "2", "5000"),
                        new MaterialRule.ItemResource(
                                Optional.empty(),
                                Optional.of(ResourceLocation.withDefaultNamespace("flint")),
                                "1",
                                "2500")),
                List.of(), List.of(),
                "material.tier*10", "8", "0", false, Optional.empty(),
                Map.of(),
                List.of("material.tier >= 2 && has_form(ingot)"),
                Optional.empty(), List.of());

        bootstrapVanillaItems();
        Item input = Items.STONE;
        Item plate = Items.IRON_INGOT;
        Item rod = Items.STICK;
        Item flint = Items.FLINT;
        MaterialRuleExpansion.ResourceResolver resolver =
                new MaterialRuleExpansion.ResourceResolver() {
                    @Override
                    public Optional<Ingredient> itemInput(
                            MaterialDefinition material,
                            Optional<MaterialPrefix> prefix,
                            Optional<ResourceLocation> fixed) {
                        return Optional.of(Ingredient.of(input));
                    }

                    @Override
                    public Optional<Item> itemOutput(
                            MaterialDefinition material,
                            Optional<MaterialPrefix> prefix,
                            Optional<ResourceLocation> fixed) {
                        if (fixed.isPresent()) return Optional.of(flint);
                        if (prefix.orElseThrow().equals(MaterialPrefixes.PLATE)) {
                            return Optional.of(plate);
                        }
                        if (prefix.orElseThrow().equals(MaterialPrefixes.ROD)) {
                            return Optional.of(rod);
                        }
                        return Optional.empty();
                    }

                    @Override
                    public Optional<Fluid> fluid(
                            MaterialDefinition material,
                            Optional<MaterialPrefix> prefix,
                            Optional<ResourceLocation> fixed,
                            Optional<String> materialFluid) {
                        return Optional.empty();
                    }
                };
        List<MaterialRuleExpansion.Expanded> expanded =
                MaterialRuleExpansion.expand(
                        id("multi"),
                        rule,
                        materials,
                        resolver,
                        MaterialRuleExpansion.FormIndexes.factualOnly(materials));
        assertEquals(1, expanded.size(), "condition filters the lower-tier material");
        MaterialRuleExpansion.Expanded result = expanded.getFirst();
        assertEquals(id("multi/iron"), result.id());
        assertEquals(CRUSHER, result.target());
        var recipe = result.recipe();
        assertEquals(2, recipe.itemInputs().size());
        assertTrue(recipe.itemInputs().stream()
                .allMatch(ingredient -> ingredient.test(new ItemStack(input))));
        assertEquals(List.of(2, 1),
                recipe.itemInputCounts());
        assertEquals(3, recipe.itemOutputs().size());
        assertSame(plate, recipe.itemOutputs().get(0).getItem());
        assertSame(rod, recipe.itemOutputs().get(1).getItem());
        assertSame(flint, recipe.itemOutputs().get(2).getItem());
        assertEquals(List.of(1, 2, 1),
                recipe.itemOutputs().stream().map(stack -> stack.getCount()).toList());
        assertEquals(List.of(10_000, 5_000, 2_500), recipe.outputChances());
        assertEquals(20, recipe.duration());
        assertEquals(8, recipe.eut());
        assertEquals(0, recipe.specialValue());
        assertFalse(recipe.canBeBuffered());
    }

    @Test
    void processingTargetAndOrderedOptionalByproductsResolveGenerically() {
        MaterialDefinition target = material("target", 1, MaterialPrefixes.INGOT);
        MaterialDefinition first = material("first", 1, MaterialPrefixes.DUST);
        MaterialDefinition source = material(
                "source", 1, MaterialPrefixes.CRUSHED_ORE).withImportedMetadata(metadata(
                        List.of(new GT6MaterialMetadata.MaterialReference("first", 2, "First")),
                        Map.of("smelting", new GT6MaterialMetadata.MaterialAmount(
                                "target", 3, "Target", 648_648_000L, Optional.of(144L)))));
        List<MaterialDefinition> materials = List.of(source, first, target);
        MaterialRule rule = new MaterialRule(
                Optional.of(CRUSHER),
                List.of(prefix(MaterialPrefixes.CRUSHED_ORE, "1", "10000")),
                List.of(
                        new MaterialRule.ItemResource(
                                Optional.of(MaterialPrefixes.INGOT.serializedId()),
                                Optional.empty(), "1", "10000",
                                Optional.of("processing_target:smelting"), false),
                        new MaterialRule.ItemResource(
                                Optional.of(MaterialPrefixes.DUST.serializedId()),
                                Optional.empty(), "1", "2500",
                                Optional.of("byproduct:0"), true),
                        new MaterialRule.ItemResource(
                                Optional.of(MaterialPrefixes.DUST.serializedId()),
                                Optional.empty(), "1", "1000",
                                Optional.of("byproduct:1"), true)),
                List.of(), List.of(), "20", "8", "0", true,
                Optional.of("source"), Map.of(), List.of(), Optional.empty(), List.of());
        bootstrapVanillaItems();
        var expanded = MaterialRuleExpansion.expand(
                id("selectors"), rule, materials, new MaterialRuleExpansion.ResourceResolver() {
                    @Override public Optional<Ingredient> itemInput(
                            MaterialDefinition material, Optional<MaterialPrefix> prefix,
                            Optional<ResourceLocation> fixed) {
                        return Optional.of(Ingredient.of(Items.STONE));
                    }
                    @Override public Optional<Item> itemOutput(
                            MaterialDefinition material, Optional<MaterialPrefix> prefix,
                            Optional<ResourceLocation> fixed) {
                        return Optional.of(material.id().equals("target")
                                ? Items.IRON_INGOT : Items.REDSTONE);
                    }
                    @Override public Optional<Fluid> fluid(
                            MaterialDefinition material, Optional<MaterialPrefix> prefix,
                            Optional<ResourceLocation> fixed,
                            Optional<String> materialFluid) {
                        return Optional.empty();
                    }
                }, MaterialRuleExpansion.FormIndexes.factualOnly(materials));
        assertEquals(1, expanded.size());
        assertEquals(List.of(Items.IRON_INGOT, Items.REDSTONE),
                expanded.getFirst().recipe().itemOutputs().stream()
                        .map(ItemStack::getItem).toList());
        assertEquals(List.of(10_000, 2_500),
                expanded.getFirst().recipe().outputChances());
        assertEquals(List.of("target", "first"),
                MaterialRuleExpansion.expandFactualPlans(
                                id("selectors"), rule, materials).getFirst()
                        .itemOutputs().stream().map(value -> value.resource().materialId()).toList());

        MaterialDefinition noTarget = material(
                "no_target", 1, MaterialPrefixes.CRUSHED_ORE, MaterialPrefixes.INGOT);
        MaterialRule missingRequiredTarget = new MaterialRule(
                rule.target(), rule.itemInputs(), rule.itemOutputs(),
                rule.fluidInputs(), rule.fluidOutputs(), rule.duration(), rule.eut(),
                rule.specialValue(), rule.canBeBuffered(), Optional.of("no_target"),
                rule.materialOverrides(), rule.conditions(), rule.tuning(),
                rule.unification());
        IllegalArgumentException missing = assertThrows(
                IllegalArgumentException.class,
                () -> MaterialRuleExpansion.expandFactualPlans(
                        id("missing_selector_target"),
                        missingRequiredTarget,
                        List.of(noTarget, target, first)));
        assertTrue(missing.getMessage().contains("required item output"));
        assertTrue(missing.getMessage().contains("missing_selector_target"));

        MaterialRule guarded = new MaterialRule(
                rule.target(), rule.itemInputs(), rule.itemOutputs(),
                rule.fluidInputs(), rule.fluidOutputs(), rule.duration(), rule.eut(),
                rule.specialValue(), rule.canBeBuffered(), rule.material(),
                rule.materialOverrides(),
                List.of(
                        "has_registered_for("
                                + "\"processing_target:smelting\", ingot)"),
                rule.tuning(), rule.unification());
        assertTrue(MaterialRuleExpansion.expandPlansWithRegisteredForms(
                        id("guarded_selector_target"),
                        guarded,
                        materials,
                        Map.of(
                                "source", List.of(MaterialPrefixes.CRUSHED_ORE),
                                "first", List.of(MaterialPrefixes.DUST),
                                "target", List.of()))
                .isEmpty());
    }

    @Test
    void candidateResolverUsesSameReloadPreferenceSnapshotForOutputs() {
        bootstrapVanillaItems();
        MaterialDefinition iron = material("iron", 1, MaterialPrefixes.INGOT);
        MaterialRule rule = new MaterialRule(
                Optional.of(CRUSHER),
                List.of(new MaterialRule.ItemResource(
                        Optional.empty(),
                        Optional.of(ResourceLocation.withDefaultNamespace("stone")),
                        "1", "10000")),
                List.of(prefix(MaterialPrefixes.INGOT, "1", "10000")),
                List.of(), List.of(), "20", "8", "0", true,
                Optional.of("iron"), Map.of(), List.of(), Optional.empty(), List.of());
        String key = "iron/" + MaterialPrefixes.INGOT.serializedId();

        var firstResolver = MaterialRuleExpansion.candidateResolver(
                Map.of(key, "test:first"),
                (material, prefix, preferences) -> Optional.of(
                        preferences.get(key).equals("test:first")
                                ? Items.GOLD_INGOT : Items.DIAMOND));
        var secondResolver = MaterialRuleExpansion.candidateResolver(
                Map.of(key, "test:second"),
                (material, prefix, preferences) -> Optional.of(
                        preferences.get(key).equals("test:second")
                                ? Items.DIAMOND : Items.GOLD_INGOT));
        List<MaterialDefinition> materials = List.of(iron);
        MaterialRuleExpansion.FormIndexes forms =
                MaterialRuleExpansion.FormIndexes.factualOnly(materials);

        assertSame(
                Items.GOLD_INGOT,
                MaterialRuleExpansion.expand(
                        id("candidate_first"), rule, materials, firstResolver, forms)
                        .getFirst().recipe().itemOutputs().getFirst().getItem());
        assertSame(
                Items.DIAMOND,
                MaterialRuleExpansion.expand(
                        id("candidate_second"), rule, materials, secondResolver, forms)
                        .getFirst().recipe().itemOutputs().getFirst().getItem());
    }

    @Test
    void explicitMaterialSelectorResolvesGlobalInsulationAtomically() {
        MaterialDefinition copper = material(
                "copper", 1, MaterialPrefixes.WIRE, MaterialPrefixes.CABLE);
        MaterialDefinition rubber = material("rubber", 1, MaterialPrefixes.PLATE);
        MaterialRule rule = new MaterialRule(
                Optional.of(ResourceLocation.fromNamespaceAndPath(
                        "cruciblecraft", "assembler")),
                List.of(
                        prefix(MaterialPrefixes.WIRE, "1", "10000"),
                        new MaterialRule.ItemResource(
                                Optional.of(MaterialPrefixes.PLATE.serializedId()),
                                Optional.empty(), "1", "10000",
                                Optional.of("material:rubber"), false)),
                List.of(prefix(MaterialPrefixes.CABLE, "1", "10000")),
                List.of(), List.of(), "120", "32", "0", true,
                Optional.of("copper"), Map.of(), List.of(), Optional.empty(), List.of());

        var plan = MaterialRuleExpansion.expandFactualPlans(
                id("cable"), rule, List.of(copper, rubber)).getFirst();
        assertEquals(List.of("copper", "rubber"),
                plan.itemInputs().stream()
                        .map(input -> input.resource().materialId()).toList());
        assertEquals(List.of(1, 1),
                plan.itemInputs().stream()
                        .map(MaterialRuleExpansion.PlannedResource::amount).toList());
        assertEquals("copper", plan.itemOutputs().getFirst().resource().materialId());
    }

    @Test
    void fixedItemCountZeroExpandsAsExactPresenceRequirement() {
        bootstrapVanillaItems();
        MaterialDefinition iron = material(
                "iron", 1, MaterialPrefixes.INGOT, MaterialPrefixes.ROD);
        MaterialRule rule = new MaterialRule(
                Optional.of(ResourceLocation.fromNamespaceAndPath(
                        "cruciblecraft", "extruder")),
                List.of(
                        prefix(MaterialPrefixes.INGOT, "1", "10000"),
                        new MaterialRule.ItemResource(
                                Optional.empty(),
                                Optional.of(ResourceLocation.withDefaultNamespace("flint")),
                                "0",
                                "10000")),
                List.of(prefix(MaterialPrefixes.ROD, "2", "10000")),
                List.of(), List.of(), "20", "16", "0", true,
                Optional.of("iron"), Map.of(), List.of(), Optional.empty(), List.of());
        MaterialRuleExpansion.ResourceResolver resolver =
                new MaterialRuleExpansion.ResourceResolver() {
                    @Override public Optional<Ingredient> itemInput(
                            MaterialDefinition material,
                            Optional<MaterialPrefix> prefix,
                            Optional<ResourceLocation> fixed) {
                        return Optional.of(Ingredient.of(
                                fixed.isPresent() ? Items.FLINT : Items.IRON_INGOT));
                    }
                    @Override public Optional<Item> itemOutput(
                            MaterialDefinition material,
                            Optional<MaterialPrefix> prefix,
                            Optional<ResourceLocation> fixed) {
                        return Optional.of(Items.IRON_NUGGET);
                    }
                    @Override public Optional<Fluid> fluid(
                            MaterialDefinition material,
                            Optional<MaterialPrefix> prefix,
                            Optional<ResourceLocation> fixed,
                            Optional<String> materialFluid) {
                        return Optional.empty();
                    }
                };

        List<MaterialDefinition> materials = List.of(iron);
        var plan = MaterialRuleExpansion.expandFactualPlans(
                id("extruder/rod"), rule, materials).getFirst();
        assertEquals(List.of(1, 0), plan.itemInputs().stream()
                .map(MaterialRuleExpansion.PlannedResource::amount).toList());
        var recipe = MaterialRuleExpansion.expand(
                id("extruder/rod"),
                rule,
                materials,
                resolver,
                MaterialRuleExpansion.FormIndexes.factualOnly(materials))
                .getFirst().recipe();
        assertEquals(List.of(1, 0), recipe.itemInputCounts());
        assertTrue(recipe.itemInputs().get(1).test(new ItemStack(Items.FLINT)));
        assertFalse(recipe.itemInputs().get(1).test(new ItemStack(Items.COAL)));
    }

    @Test
    void targetUnitsProduceExactReducedBatchesWithoutRounding() {
        MaterialDefinition ingotTarget = material("ingot_target", 1, MaterialPrefixes.INGOT);
        MaterialDefinition zircon = material("zircon", 1, MaterialPrefixes.DUST)
                .withImportedMetadata(metadata(List.of(), Map.of(
                        "smelting", new GT6MaterialMetadata.MaterialAmount(
                                "ingot_target", 3, "Target", 72_072_000L, Optional.of(16L)))));
        MaterialDefinition multi = material("multi", 1, MaterialPrefixes.DUST)
                .withImportedMetadata(metadata(List.of(), Map.of(
                        "smelting", new GT6MaterialMetadata.MaterialAmount(
                                "ingot_target", 3, "Target", 1_297_296_000L, Optional.of(288L)))));
        MaterialDefinition overCapacity = material("over_capacity", 1, MaterialPrefixes.DUST)
                .withImportedMetadata(metadata(List.of(), Map.of(
                        "smelting", new GT6MaterialMetadata.MaterialAmount(
                                "ingot_target", 3, "Target", 4_504_500L, Optional.of(1L)))));
        MaterialRule smelter = MaterialChainRules.ALL.stream()
                .filter(definition -> definition.path().startsWith("smelter/"))
                .findFirst().orElseThrow().rule();
        var plans = MaterialRuleExpansion.expandFactualPlans(
                id("smelter/exact"), smelter,
                List.of(zircon, multi, overCapacity, ingotTarget));
        assertEquals(List.of("multi", "zircon"), plans.stream()
                .map(MaterialRuleExpansion.Plan::materialId).toList());
        assertEquals(2, plans.getFirst().itemOutputs().getFirst().amount());
        assertEquals(1, plans.getFirst().itemInputs().getFirst().amount());
        assertEquals(800, plans.getFirst().duration());
        assertEquals(9, plans.get(1).itemInputs().getFirst().amount());
        assertEquals(1, plans.get(1).itemOutputs().getFirst().amount());
        assertEquals(7_200, plans.get(1).duration());
    }

    @Test
    void crusherTurnsAnyMaterialWithIngotAndDustFormsIntoDust() {
        MaterialRule crusher = MaterialChainRules.ALL.stream()
                .filter(definition -> definition.path().equals("crusher/ingot_to_dust"))
                .findFirst().orElseThrow().rule();
        var plans = MaterialRuleExpansion.expandFactualPlans(
                id("crusher/ingot_to_dust"),
                crusher,
                List.of(
                        material("tungsten", 0,
                                MaterialPrefixes.INGOT, MaterialPrefixes.DUST),
                        material("ingot_only", 0, MaterialPrefixes.INGOT)));

        assertEquals(1, plans.size());
        assertEquals("tungsten", plans.getFirst().materialId());
        assertEquals(MaterialPrefixes.INGOT.serializedId(),
                plans.getFirst().itemInputs().getFirst().resource().prefix().orElseThrow());
        assertEquals(MaterialPrefixes.DUST.serializedId(),
                plans.getFirst().itemOutputs().getFirst().resource().prefix().orElseThrow());
        assertEquals(1, plans.getFirst().itemInputs().getFirst().amount());
        assertEquals(1, plans.getFirst().itemOutputs().getFirst().amount());
    }

    @Test
    void rockPulverExpandsToNineSmallDustOfThePulverTarget() {
        MaterialPrefix rock = new MaterialPrefix("cruciblecraft:rock");
        MaterialDefinition chalcopyrite = material(
                "chalcopyrite",
                0,
                rock,
                MaterialPrefixes.SMALL_DUST)
                .withImportedMetadata(metadata(
                        List.of(),
                        Map.of("pulver", new GT6MaterialMetadata.MaterialAmount(
                                "chalcopyrite",
                                9111,
                                "Chalcopyrite",
                                648_648_000L,
                                Optional.of(144L))),
                        List.of(),
                        0,
                        0));
        MaterialDefinition antimatterHost = material(
                "antimatter_host",
                0,
                MaterialPrefixes.DUST)
                .withImportedMetadata(metadata(
                        List.of(),
                        Map.of(),
                        List.of("ATOMIC.ANTIMATTER"),
                        0,
                        0));
        var plans = MaterialRuleExpansion.expandFactualPlans(
                id("crusher/rock_to_pulver_dust"),
                MaterialChainRules.CRUSHER_ROCK_TO_PULVER,
                List.of(chalcopyrite, antimatterHost));
        assertEquals(1, plans.size());
        assertEquals("chalcopyrite", plans.getFirst().materialId());
        assertEquals(rock.serializedId(),
                plans.getFirst().itemInputs().getFirst().resource().prefix().orElseThrow());
        assertEquals(MaterialPrefixes.SMALL_DUST.serializedId(),
                plans.getFirst().itemOutputs().getFirst().resource().prefix().orElseThrow());
        assertEquals("chalcopyrite",
                plans.getFirst().itemOutputs().getFirst().resource().materialId());
        assertEquals(9, plans.getFirst().itemOutputs().getFirst().amount());
        assertEquals(36, plans.getFirst().duration());
        assertEquals(16, plans.getFirst().eut());
    }

    private static GT6MaterialMetadata metadata(
            List<GT6MaterialMetadata.MaterialReference> byproducts,
            Map<String, GT6MaterialMetadata.MaterialAmount> targets) {
        return metadata(byproducts, targets, List.of(), 0, 0);
    }

    private static GT6MaterialMetadata metadata(
            List<GT6MaterialMetadata.MaterialReference> byproducts,
            Map<String, GT6MaterialMetadata.MaterialAmount> targets,
            List<String> materialTags,
            double explosionDamage,
            double heatDamage) {
        return new GT6MaterialMetadata(
                1, "Source", List.of(), "solid", Optional.empty(),
                new GT6MaterialMetadata.SourceThermal(
                        1000, 726.85, 2000, 1726.85, 3000, 2726.85, 7),
                GT6MaterialMetadata.ToolStats.EMPTY,
                byproducts,
                targets,
                materialTags, List.of(), explosionDamage, heatDamage,
                Optional.empty(), Map.of(),
                GT6MaterialMetadata.PipeProperties.EMPTY);
    }

    private static MaterialDefinition material(
            String id,
            int tier,
            MaterialPrefix... forms) {
        return new MaterialDefinition(
                id,
                id,
                Optional.empty(),
                tier,
                "#808080",
                "metallic",
                List.of(forms),
                Map.of(),
                new ThermalProperties(1000, 2000, 7.0),
                false,
                Map.of(),
                false);
    }

    private static MaterialRule.ItemResource prefix(
            MaterialPrefix prefix,
            String count,
            String chance) {
        return new MaterialRule.ItemResource(
                Optional.of(prefix.serializedId()), Optional.empty(), count, chance);
    }

    private static MaterialRule pipeRule(
            MaterialPrefix output, String recipeCondition) {
        return new MaterialRule(
                Optional.of(CRUSHER),
                List.of(prefix(MaterialPrefixes.PLATE, "1", "10000")),
                List.of(prefix(output, "1", "10000")),
                List.of(),
                List.of(),
                "1",
                "1",
                "0",
                true,
                Optional.of("mixed"),
                Map.of(),
                List.of(recipeCondition),
                Optional.empty(),
                List.of());
    }

    private static MaterialRule.MaterialOverride overrideDuration(String duration) {
        return new MaterialRule.MaterialOverride(
                Optional.of(duration), Optional.empty(), Optional.empty(),
                Map.of(), Map.of(), Map.of(), Map.of());
    }

    private static MaterialRule withConditions(
            MaterialRule rule,
            List<String> conditions) {
        return new MaterialRule(
                rule.target(),
                rule.itemInputs(),
                rule.itemOutputs(),
                rule.fluidInputs(),
                rule.fluidOutputs(),
                rule.duration(),
                rule.eut(),
                rule.specialValue(),
                rule.canBeBuffered(),
                rule.material(),
                rule.materialOverrides(),
                conditions,
                rule.tuning(),
                rule.unification());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }

    private static void bootstrapVanillaItems() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static MaterialRuleExpansion.ResourceResolver resolver(
            Map<MaterialPrefix, Item> outputs) {
        return new MaterialRuleExpansion.ResourceResolver() {
            @Override
            public Optional<Ingredient> itemInput(
                    MaterialDefinition material,
                    Optional<MaterialPrefix> prefix,
                    Optional<ResourceLocation> fixed) {
                return Optional.of(Ingredient.of(Items.STONE));
            }

            @Override
            public Optional<Item> itemOutput(
                    MaterialDefinition material,
                    Optional<MaterialPrefix> prefix,
                    Optional<ResourceLocation> fixed) {
                return Optional.ofNullable(outputs.get(prefix.orElse(null)));
            }

            @Override
            public Optional<Fluid> fluid(
                    MaterialDefinition material,
                    Optional<MaterialPrefix> prefix,
                    Optional<ResourceLocation> fixed,
                    Optional<String> materialFluid) {
                return Optional.empty();
            }
        };
    }

}
