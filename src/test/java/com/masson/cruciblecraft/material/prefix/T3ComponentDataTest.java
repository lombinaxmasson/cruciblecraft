package com.masson.cruciblecraft.material.prefix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.JsonParser;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.material.MaterialRegistrationGate;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.material.def.MaterialLoader;
import com.masson.cruciblecraft.recipe.rule.MaterialRuleExpansion;
import com.masson.cruciblecraft.recipe.rule.T3ComponentRules;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.registry.ModProcessingMachines;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.fluids.FluidStack;

class T3ComponentDataTest {
    @BeforeAll static void bootstrapMinecraft() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static final Set<MaterialPrefix> COMPONENT_FORMS = Set.of(
            MaterialPrefixes.LONG_ROD, MaterialPrefixes.SCREW, MaterialPrefixes.RING,
            MaterialPrefixes.SPRING, MaterialPrefixes.SMALL_SPRING,
            MaterialPrefixes.GEAR, MaterialPrefixes.SMALL_GEAR, MaterialPrefixes.ROTOR,
            MaterialPrefixes.FOIL, MaterialPrefixes.DOUBLE_PLATE,
            MaterialPrefixes.TRIPLE_PLATE, MaterialPrefixes.QUADRUPLE_PLATE,
            MaterialPrefixes.QUINTUPLE_PLATE, MaterialPrefixes.DENSE_PLATE,
            MaterialPrefixes.FINE_WIRE, MaterialPrefixes.WIRE,
            MaterialPrefixes.DOUBLE_WIRE, MaterialPrefixes.QUADRUPLE_WIRE,
            MaterialPrefixes.OCTUPLE_WIRE, MaterialPrefixes.DODECUPLE_WIRE,
            MaterialPrefixes.HEXADECUPLE_WIRE, MaterialPrefixes.CABLE,
            MaterialPrefixes.DOUBLE_CABLE, MaterialPrefixes.QUADRUPLE_CABLE,
            MaterialPrefixes.OCTUPLE_CABLE, MaterialPrefixes.DODECUPLE_CABLE);
    private static final Map<String, MaterialPrefix> SOURCES = Map.ofEntries(
            Map.entry("plate", MaterialPrefixes.PLATE),
            Map.entry("stick", MaterialPrefixes.ROD),
            Map.entry("bolt", MaterialPrefixes.BOLT),
            Map.entry("stickLong", MaterialPrefixes.LONG_ROD),
            Map.entry("screw", MaterialPrefixes.SCREW),
            Map.entry("ring", MaterialPrefixes.RING),
            Map.entry("spring", MaterialPrefixes.SPRING),
            Map.entry("springSmall", MaterialPrefixes.SMALL_SPRING),
            Map.entry("gearGt", MaterialPrefixes.GEAR),
            Map.entry("gearGtSmall", MaterialPrefixes.SMALL_GEAR),
            Map.entry("rotor", MaterialPrefixes.ROTOR),
            Map.entry("foil", MaterialPrefixes.FOIL),
            Map.entry("plateDouble", MaterialPrefixes.DOUBLE_PLATE),
            Map.entry("plateTriple", MaterialPrefixes.TRIPLE_PLATE),
            Map.entry("plateQuadruple", MaterialPrefixes.QUADRUPLE_PLATE),
            Map.entry("plateQuintuple", MaterialPrefixes.QUINTUPLE_PLATE),
            Map.entry("plateDense", MaterialPrefixes.DENSE_PLATE),
            Map.entry("wireFine", MaterialPrefixes.FINE_WIRE),
            Map.entry("wireGt01", MaterialPrefixes.WIRE),
            Map.entry("wireGt02", MaterialPrefixes.DOUBLE_WIRE),
            Map.entry("wireGt04", MaterialPrefixes.QUADRUPLE_WIRE),
            Map.entry("wireGt08", MaterialPrefixes.OCTUPLE_WIRE),
            Map.entry("wireGt12", MaterialPrefixes.DODECUPLE_WIRE),
            Map.entry("wireGt16", MaterialPrefixes.HEXADECUPLE_WIRE),
            Map.entry("cableGt01", MaterialPrefixes.CABLE),
            Map.entry("cableGt02", MaterialPrefixes.DOUBLE_CABLE),
            Map.entry("cableGt04", MaterialPrefixes.QUADRUPLE_CABLE),
            Map.entry("cableGt08", MaterialPrefixes.OCTUPLE_CABLE),
            Map.entry("cableGt12", MaterialPrefixes.DODECUPLE_CABLE));

    @BeforeEach void bootstrapPrefixes() {
        MaterialPrefixTestFixture.reset();
        MaterialPrefixTestFixture.bootstrapBuiltins();
    }

    @AfterEach void restorePrefixes() {
        MaterialPrefixTestFixture.reset();
        MaterialPrefixTestFixture.bootstrapBuiltins();
    }

    @Test
    void exactPrefixUnitsAndGenerationEvidenceMatchNormalizedGt6(@TempDir Path config)
            throws Exception {
        var normalized = JsonParser.parseString(Files.readString(
                Path.of("tools/gt6_oredict_prefixes_normalized.json"))).getAsJsonObject();
        Map<String, Set<String>> registered = new LinkedHashMap<>();
        for (var value : normalized.getAsJsonArray("records")) {
            var record = value.getAsJsonObject();
            String source = record.get("source_name").getAsString();
            MaterialPrefix prefix = SOURCES.get(source);
            if (prefix == null) continue;
            var amount = record.getAsJsonObject("amount");
            assertTrue(amount.get("integral_cc_units").getAsBoolean(), source);
            assertEquals(prefix.units(), amount.get("cc_units").getAsInt(), source);
            assertEquals((long) prefix.units() * 4_504_500L,
                    amount.get("numerator_u").getAsLong(), source);
            assertTrue(record.get("registered_item_count").getAsInt() > 0, source);
            registered.put(source, record.getAsJsonArray("registered_materials").asList().stream()
                    .map(entry -> entry.getAsString()).collect(Collectors.toUnmodifiableSet()));
        }
        assertEquals(SOURCES.keySet(), registered.keySet());

        var materials = MaterialLoader.load(config).values();
        var normalizedMaterials = JsonParser.parseString(Files.readString(
                Path.of("tools/gt6_oredict_materials_normalized.json"))).getAsJsonObject();
        Map<String, String> electricalEvidence = new LinkedHashMap<>();
        for (var value : normalizedMaterials.getAsJsonArray("records")) {
            var record = value.getAsJsonObject();
            electricalEvidence.put(
                    record.get("source_name").getAsString(),
                    record.getAsJsonObject("electrical_by_specification").toString());
        }
        Map<MaterialPrefix, Set<String>> evidenceByPrefix = SOURCES.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getValue,
                        entry -> registered.get(entry.getKey())));
        for (MaterialDefinition material : materials) {
            if (material.gt6Metadata().isEmpty()) continue;
            String source = material.gt6Metadata().get().sourceName();
            assertEquals(electricalEvidence.get(source),
                    new com.google.gson.Gson().toJson(
                            material.gt6Metadata().get().electricalBySpecification()),
                    "electrical metadata must remain source-evidenced per specification");
            evidenceByPrefix.forEach((prefix, evidence) ->
                    assertFalse(material.forms().contains(prefix)
                                    && !evidence.contains(source),
                            "unsupported generated form: " + source + " / "
                                    + prefix.serializedId()));
        }
        assertTrue(materials.stream().anyMatch(material ->
                material.forms().contains(MaterialPrefixes.SCREW)));
        assertTrue(materials.stream().anyMatch(material ->
                material.forms().contains(MaterialPrefixes.WIRE)));
        for (MaterialPrefix prefix : List.of(
                MaterialPrefixes.FINE_WIRE,
                MaterialPrefixes.WIRE,
                MaterialPrefixes.DOUBLE_WIRE,
                MaterialPrefixes.CABLE,
                MaterialPrefixes.DOUBLE_CABLE)) {
            assertFalse(MaterialPrefixCatalog.definition(prefix).capabilities()
                    .contains("cruciblecraft:electrical"));
        }
    }

    @Test
    void componentRulesConserveUnitsRemainBoundedAndHaveNoDuplicates(@TempDir Path config) {
        var materials = MaterialLoader.load(config).values();
        var registeredForms = MaterialRegistrationGate.load(materials);
        List<MaterialRuleExpansion.Plan> plans = assertTimeoutPreemptively(
                Duration.ofSeconds(5),
                () -> T3ComponentRules.ALL.stream().flatMap(definition ->
                        MaterialRuleExpansion.expandPlansWithForms(
                                ResourceLocation.fromNamespaceAndPath(
                                        "cruciblecraft", definition.path()),
                                definition.rule(),
                                materials,
                                registeredForms).stream()).toList());
        assertEquals(29, T3ComponentRules.ALL.size());
        assertFalse(plans.isEmpty());
        assertTrue(plans.size() < 10_000, "T3 reload expansion budget");

        Map<String, MaterialDefinition> byId = materials.stream()
                .collect(Collectors.toMap(MaterialDefinition::id, value -> value));
        for (MaterialRuleExpansion.Plan plan : plans) {
            MaterialDefinition source = byId.get(plan.materialId());
            var outputRule = T3ComponentRules.ALL.stream()
                    .filter(definition -> plan.id().getPath().startsWith(definition.path() + "/"))
                    .findFirst().orElseThrow().rule().itemOutputs().getFirst();
            String selector = outputRule.materialSelector().orElse("self");
            long inputUnits = plan.itemInputs().stream().mapToLong(resource ->
                    resource.resource().materialId().equals(source.id())
                                    && resource.resource().prefix().isPresent()
                            ? (long) MaterialPrefixCatalog.require(
                                    resource.resource().prefix().orElseThrow()).units()
                                    * resource.amount()
                            : 0L).sum();
            long outputUnits = plan.itemOutputs().stream().mapToLong(resource ->
                    (long) MaterialPrefixCatalog.require(
                            resource.resource().prefix().orElseThrow()).units()
                            * resource.amount()).sum();
            boolean compatibilityBridge = plan.id().getPath()
                    .equals("press/slime_ball_to_rubber_plate/rubber");
            if (compatibilityBridge) {
                assertEquals(0L, inputUnits);
                assertEquals(144L, outputUnits);
            } else if (selector.startsWith("processing_target:")) {
                String targetKey = selector.substring("processing_target:".length());
                var target = source.gt6Metadata().orElseThrow()
                        .processingTargets().get(targetKey);
                assertEquals(target.material(),
                        plan.itemOutputs().getFirst().resource().materialId());
                assertEquals(inputUnits * target.ccUnits().orElseThrow(),
                        outputUnits * 144L, plan.id().toString());
            } else {
                assertEquals(source.id(),
                        plan.itemOutputs().getFirst().resource().materialId());
                assertEquals(inputUnits, outputUnits,
                        "embedded insulation is tracked separately: " + plan.id());
            }
            assertTrue(plan.duration() > 0 && plan.eut() > 0);
        }

        Map<ResourceLocation, Long> perMap = plans.stream().collect(Collectors.groupingBy(
                MaterialRuleExpansion.Plan::target, Collectors.counting()));
        assertEquals(10, perMap.size());
        assertTrue(perMap.values().stream().allMatch(count -> count > 0));
        assertEquals(Map.of(
                id("extruder"), 330L,
                id("cutter"), 651L,
                id("lathe"), 929L,
                id("rollingmill"), 336L,
                id("rollbender"), 438L,
                id("wiremill"), 280L,
                id("bender"), 638L,
                id("assembler"), 568L,
                id("welder"), 321L,
                id("press"), 1191L), perMap);
        Map<ResourceLocation, Long> maxEut = plans.stream().collect(Collectors.groupingBy(
                MaterialRuleExpansion.Plan::target,
                Collectors.collectingAndThen(
                        Collectors.maxBy(java.util.Comparator.comparingLong(
                                MaterialRuleExpansion.Plan::eut)),
                        value -> value.orElseThrow().eut())));
        assertEquals(Map.of(
                id("extruder"), 32L,
                id("cutter"), 32L,
                id("lathe"), 24L,
                id("rollingmill"), 32L,
                id("rollbender"), 24L,
                id("wiremill"), 48L,
                id("bender"), 32L,
                id("assembler"), 64L,
                id("welder"), 48L,
                id("press"), 96L), maxEut);
        for (MaterialRuleExpansion.Plan plan : plans) {
            var spec = ModProcessingMachines.forRecipeMap(plan.target()).orElseThrow();
            assertTrue(plan.itemInputs().size() <= spec.items().inputs().size(), plan.id().toString());
            assertTrue(plan.itemOutputs().size() <= spec.items().outputs().size(), plan.id().toString());
            assertTrue(plan.fluidInputs().size() <= spec.fluids().inputs().size(), plan.id().toString());
            assertTrue(plan.fluidOutputs().size() <= spec.fluids().outputs().size(), plan.id().toString());
            assertTrue(plan.eut() > 0 && plan.eut() <= spec.energy().maxPacket(), plan.id().toString());
            assertTrue(plan.itemOutputs().stream().allMatch(output -> output.chance() > 0),
                    plan.id().toString());
            assertTrue(spec.validator().validate(validationRecipe(plan)).isEmpty(),
                    plan.id().toString());
        }
        Set<String> ids = plans.stream().map(plan -> plan.id().toString())
                .collect(Collectors.toSet());
        assertEquals(plans.size(), ids.size());
        Set<String> signatures = plans.stream().map(T3ComponentDataTest::shadowSignature)
                .collect(Collectors.toSet());
        assertEquals(plans.size(), signatures.size());

        Set<String> produced = plans.stream()
                .flatMap(plan -> plan.itemOutputs().stream())
                .filter(output -> COMPONENT_FORMS.contains(
                        MaterialPrefixCatalog.require(
                                output.resource().prefix().orElseThrow())))
                .map(output -> output.resource().materialId() + "/"
                        + output.resource().prefix().orElseThrow())
                .collect(Collectors.toSet());
        Set<String> retained = materials.stream().flatMap(material ->
                registeredForms.get(material.id()).stream()
                        .filter(COMPONENT_FORMS::contains)
                        .map(prefix -> material.id() + "/" + prefix.serializedId()))
                .collect(Collectors.toSet());
        assertTrue(retained.containsAll(produced),
                "component recipes must not output unregistered forms");
        Set<String> registered = materials.stream().flatMap(material ->
                registeredForms.get(material.id()).stream()
                        .map(prefix -> material.id() + "/" + prefix.serializedId()))
                .collect(Collectors.toSet());
        assertTrue(plans.stream()
                .flatMap(plan -> java.util.stream.Stream.concat(
                        plan.itemInputs().stream(), plan.itemOutputs().stream()))
                .filter(resource -> resource.resource().prefix().isPresent())
                .allMatch(resource -> registered.contains(
                        resource.resource().materialId() + "/"
                                + resource.resource().prefix().orElseThrow())),
                "every generated recipe operand must be registered by the same gate");
        Map<MaterialPrefix, Long> cables = materials.stream()
                .flatMap(material -> registeredForms.get(material.id()).stream())
                .filter(prefix -> prefix.serializedName().contains("cable"))
                .collect(Collectors.groupingBy(prefix -> prefix, Collectors.counting()));
        assertEquals(26L, cables.get(MaterialPrefixes.CABLE));
        assertEquals(23L, cables.get(MaterialPrefixes.DOUBLE_CABLE));
        assertEquals(23L, cables.get(MaterialPrefixes.QUADRUPLE_CABLE));
        assertEquals(23L, cables.get(MaterialPrefixes.OCTUPLE_CABLE));
        assertEquals(23L, cables.get(MaterialPrefixes.DODECUPLE_CABLE));
        assertEquals(List.of(MaterialPrefixes.PLATE),
                registeredForms.get(byId.get("rubber").id()));
        for (MaterialRuleExpansion.Plan cablePlan : plans.stream()
                .filter(plan -> plan.itemOutputs().getFirst().resource().prefix()
                        .orElseThrow().contains("cable"))
                .toList()) {
            assertEquals(2, cablePlan.itemInputs().size());
            var insulation = cablePlan.itemInputs().get(1);
            assertEquals("rubber", insulation.resource().materialId());
            assertEquals(MaterialPrefixes.PLATE.serializedId(),
                    insulation.resource().prefix().orElseThrow());
            assertTrue(insulation.amount() >= 1 && insulation.amount() <= 4);
        }

        var screw = plans.stream().filter(plan ->
                plan.id().getPath().equals("lathe/bolts_to_screws/copper"))
                .findFirst().orElseThrow();
        assertEquals(8, screw.itemInputs().getFirst().amount());
        assertEquals(9, screw.itemOutputs().getFirst().amount());
        var fineWire = plans.stream().filter(plan ->
                plan.id().getPath().equals("wiremill/foil_to_fine_wire/copper"))
                .findFirst().orElseThrow();
        assertEquals("cruciblecraft:foil",
                fineWire.itemInputs().getFirst().resource().prefix().orElseThrow());
        assertEquals(2, fineWire.itemOutputs().getFirst().amount());
        var rotor = plans.stream().filter(plan ->
                plan.id().getPath().equals("assembler/plates_and_ring_to_rotor/copper"))
                .findFirst().orElseThrow();
        assertEquals(List.of(4, 1),
                rotor.itemInputs().stream().map(
                        MaterialRuleExpansion.PlannedResource::amount).toList());

        assertTrue(plans.stream().anyMatch(plan ->
                plan.target().getPath().equals("assembler") && plan.itemInputs().size() == 2));
        assertEquals(2, plans.stream()
                .filter(plan -> plan.target().getPath().equals("assembler"))
                .mapToInt(plan -> plan.itemInputs().size()).max().orElseThrow());
        assertTrue(plans.stream().anyMatch(plan ->
                plan.target().getPath().equals("welder") && plan.itemInputs().size() == 2));

        var rubberBridge = plans.stream().filter(plan ->
                plan.id().getPath().equals("press/slime_ball_to_rubber_plate/rubber"))
                .findFirst().orElseThrow();
        assertEquals(ResourceLocation.withDefaultNamespace("slime_ball"),
                rubberBridge.itemInputs().getFirst().resource().fixed().orElseThrow());
        assertEquals("rubber", rubberBridge.itemOutputs().getFirst().resource().materialId());
        assertEquals(MaterialPrefixes.PLATE.serializedId(),
                rubberBridge.itemOutputs().getFirst().resource().prefix().orElseThrow());
        assertTrue(plans.stream().anyMatch(plan ->
                plan.id().getPath().equals("wiremill/ingot_to_wire/copper")));
        assertTrue(plans.stream().anyMatch(plan ->
                plan.id().getPath().equals("assembler/wire_and_rubber_to_cable/copper")
                        && plan.itemInputs().stream().anyMatch(input ->
                                input.resource().materialId().equals("rubber")
                                        && input.resource().prefix().orElse("").equals(
                                                MaterialPrefixes.PLATE.serializedId()))));
    }

    private static String shadowSignature(MaterialRuleExpansion.Plan plan) {
        return plan.target() + "|" + plan.itemInputs() + "|" + plan.fluidInputs();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }

    private static GTRecipe validationRecipe(MaterialRuleExpansion.Plan plan) {
        return new GTRecipe(
                plan.itemInputs().stream().map(input -> Ingredient.of(Items.STONE)).toList(),
                plan.itemInputs().stream().map(
                        MaterialRuleExpansion.PlannedResource::amount).toList(),
                plan.itemOutputs().stream().map(output ->
                        new ItemStack(Items.COBBLESTONE, output.amount())).toList(),
                plan.fluidInputs().stream().map(input ->
                        new FluidStack(Fluids.WATER, input.amount())).toList(),
                plan.fluidOutputs().stream().map(output ->
                        new FluidStack(Fluids.WATER, output.amount())).toList(),
                plan.itemOutputs().stream().map(
                        MaterialRuleExpansion.PlannedResource::chance).toList(),
                plan.duration(),
                plan.eut(),
                plan.specialValue(),
                plan.canBeBuffered());
    }
}
