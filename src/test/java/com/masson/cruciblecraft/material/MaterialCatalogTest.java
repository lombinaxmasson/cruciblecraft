package com.masson.cruciblecraft.material;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.material.def.MaterialLoader;
import com.masson.cruciblecraft.material.def.MaterialTuning;
import com.masson.cruciblecraft.material.def.ThermalProperties;
import com.masson.cruciblecraft.fluid.CrucibleTransferCoordinator;
import com.masson.cruciblecraft.fluid.MoltenTransferMath;
import com.masson.cruciblecraft.machine.MachineMaterialRules.Device;
import com.masson.cruciblecraft.machine.component.CompositionTank;
import com.masson.cruciblecraft.machine.component.MachineCasing;
import com.masson.cruciblecraft.machine.component.SteelmakingController;
import com.masson.cruciblecraft.recipe.gt.GTRecipeRuntimeEpoch;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.neoforged.fml.loading.LoadingModList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaterialCatalogTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void loadsConfigJsonDecomposesBronzeAndIndexesAlloy(@TempDir Path configDirectory) throws Exception {
        MaterialCatalog.resetForTests();
        Files.writeString(configDirectory.resolve("lead.json"), """
                {
                  "id": "lead",
                  "forms": ["dust"],
                  "thermal": { "melting_point": 327.5 }
                }
                """);
        Files.writeString(configDirectory.resolve("broken_alloy.json"), """
                {
                  "id": "broken_alloy",
                  "forms": ["ingot"],
                  "thermal": { "melting_point": 700 },
                  "composition": { "missing_component": 1 }
                }
                """);
        assertTrue(MaterialCatalog.addStartupMaterial(new MaterialDefinition(
                "zinc",
                "zinc",
                Optional.empty(),
                1,
                "#B8C4C2",
                "metallic",
                List.of(MaterialPrefixes.DUST),
                Map.of(),
                new ThermalProperties(419.5),
                false,
                Map.of(),
                false)));
        assertTrue(MaterialCatalog.addStartupMaterial(new MaterialDefinition(
                "nested_bronze",
                "nested_bronze",
                Optional.empty(),
                1,
                "#AA7744",
                "metallic",
                List.of(MaterialPrefixes.DUST),
                Map.of(),
                new ThermalProperties(950),
                false,
                Map.of("bronze", 1, "copper", 1),
                false)));
        MaterialCatalog.bootstrap(configDirectory);

        assertEquals(MaterialLoader.load(configDirectory).size() + 1,
                MaterialCatalog.values().size());
        assertTrue(MaterialCatalog.prefixIndex().get(MaterialPrefixes.WIRE).stream()
                .allMatch(material -> material.forms().contains(MaterialPrefixes.WIRE)));
        assertTrue(MaterialCatalog.prefixIndex().get(MaterialPrefixes.WIRE).size()
                < MaterialCatalog.values().size());
        assertSame(
                MaterialCatalog.canonicalItemMappings(),
                MaterialCatalog.canonicalItemMappings());
        assertEquals(
                "minecraft:copper_ingot",
                MaterialCatalog.canonicalItemMappings().get("copper/ingot"));
        assertEquals(
                "cruciblecraft:copper/dust",
                MaterialCatalog.canonicalItemMappings().get("copper/dust"));
        assertEquals(
                "cruciblecraft:copper_ore",
                MaterialCatalog.canonicalItemMappings().get("copper/ore"));
        var copper = MaterialCatalog.require("copper");
        assertTrue(MaterialFormHosts.isUniqueInventoryForm(
                copper, MaterialPrefixes.DUST));
        assertFalse(MaterialFormHosts.isSharedInventoryForm(
                copper, MaterialPrefixes.DUST));
        assertTrue(MaterialFormHosts.isSharedInventoryForm(
                copper, MaterialPrefixes.CRUSHED_ORE));
        assertEquals(
                ResourceLocation.fromNamespaceAndPath("cruciblecraft", "copper_ore"),
                MaterialLookup.itemId("copper", MaterialPrefixes.ORE).orElseThrow());
        MaterialDefinition tungsten = MaterialCatalog.require("tungsten");
        assertTrue(tungsten.forms().containsAll(List.of(
                MaterialPrefixes.ORE,
                MaterialPrefixes.RAW_ORE,
                MaterialPrefixes.CRUSHED_ORE,
                MaterialPrefixes.WASHED_CRUSHED_ORE,
                MaterialPrefixes.CENTRIFUGED_CRUSHED_ORE,
                MaterialPrefixes.PURIFIED_DUST,
                MaterialPrefixes.INGOT,
                MaterialPrefixes.PLATE,
                MaterialPrefixes.DUST,
                MaterialPrefixes.BLOCK)));
        assertTrue(MaterialCatalog.registeredForms(tungsten).containsAll(List.of(
                MaterialPrefixes.ORE,
                MaterialPrefixes.RAW_ORE,
                MaterialPrefixes.CRUSHED_ORE,
                MaterialPrefixes.WASHED_CRUSHED_ORE,
                MaterialPrefixes.CENTRIFUGED_CRUSHED_ORE,
                MaterialPrefixes.PURIFIED_DUST,
                MaterialPrefixes.INGOT,
                MaterialPrefixes.PLATE,
                MaterialPrefixes.DUST,
                MaterialPrefixes.BLOCK)));
        MaterialDefinition almond = MaterialCatalog.require("almond");
        assertTrue(almond.forms().contains(MaterialPrefixes.DUST));
        assertTrue(MaterialCatalog.registeredForms(almond).isEmpty());
        assertTrue(MaterialCatalog.require("oxygen").metadataOnly());
        assertTrue(MaterialCatalog.registeredForms("oxygen").isEmpty());
        assertEquals("tungsten/ingot", tungsten.registryName(MaterialPrefixes.INGOT));
        assertEquals(
                "iron/dust",
                MaterialCatalog.require("iron").registryName(MaterialPrefixes.DUST));
        assertThrows(
                IllegalStateException.class,
                () -> MaterialCatalog.validateRegistryNames(List.of(
                        MaterialCatalog.require("iron"),
                        MaterialCatalog.require("iron"))));
        assertTrue(MaterialCatalog.values().stream()
                .map(MaterialDefinition::id)
                .toList()
                .containsAll(List.of(
                        "copper", "tin", "bronze", "clay", "ceramic", "iron",
                        "carbon", "steel", "gold", "zinc", "lead", "nickel",
                        "nested_bronze")));
        assertTrue(MaterialCatalog.require("oxygen").forms().isEmpty());
        assertTrue(MaterialCatalog.require("water").forms().isEmpty());
        assertEquals("gas",
                MaterialCatalog.require("oxygen").gt6Metadata().orElseThrow().state());
        assertEquals("liquid",
                MaterialCatalog.require("water").gt6Metadata().orElseThrow().state());
        assertTrue(MaterialCatalog.require("lead").forms().containsAll(
                java.util.List.of(
                        MaterialPrefixes.RAW_ORE,
                        MaterialPrefixes.CRUSHED_ORE,
                        MaterialPrefixes.INGOT,
                        MaterialPrefixes.DUST,
                        MaterialPrefixes.PLATE,
                        MaterialPrefixes.ROD,
                        MaterialPrefixes.BOLT,
                        MaterialPrefixes.TINY_CRUSHED_ORE,
                        MaterialPrefixes.WASHED_CRUSHED_ORE,
                        MaterialPrefixes.CENTRIFUGED_CRUSHED_ORE,
                        MaterialPrefixes.PURIFIED_DUST)));
        assertFalse(MaterialCatalog.values().stream()
                .anyMatch(material -> material.id().equals("broken_alloy")));
        assertEquals("zinc", MaterialCatalog.require("zinc").id());
        assertEquals(8.96, MaterialCatalog.require("copper").thermal().density());
        assertEquals(327.5, MaterialCatalog.require("lead").thermal().meltingPoint());
        assertEquals(1749.0, MaterialCatalog.require("lead").thermal().boilingPoint());
        assertEquals(11.34, MaterialCatalog.require("lead").thermal().density());
        assertEquals(2, MaterialCatalog.require("lead").tier());
        assertEquals("#5C6274", MaterialCatalog.require("lead").color());
        assertEquals(2562.0, MaterialCatalog.require("copper").thermal().boilingPoint());
        assertEquals(1538.0, MaterialCatalog.require("iron").thermal().meltingPoint());
        assertEquals(3527.0, MaterialCatalog.require("carbon").thermal().meltingPoint());
        assertEquals(1773.0, MaterialCatalog.require("steel").thermal().meltingPoint());
        assertTrue(MaterialCatalog.require("steel").noDecompose());
        assertTrue(MaterialCatalog.require("steel").composition().isEmpty());
        assertEquals(
                Map.of("copper", 108, "tin", 36),
                MaterialCatalog.decompose(
                        MaterialCatalog.require("bronze"),
                        MaterialPrefixes.INGOT.units()));
        assertTrue(MaterialCatalog.contains("bronze"));
        assertFalse(MaterialCatalog.contains("missing"));
        assertEquals(4, MaterialCatalog.decompositionQuantum("bronze"));
        assertEquals(1, MaterialCatalog.decompositionQuantum("steel"));
        assertEquals(Map.of("copper", 3, "tin", 1), MaterialCatalog.decompositionRatio("bronze"));
        assertEquals(8, MaterialCatalog.decompositionQuantum("nested_bronze"));
        assertEquals(
                Map.of("copper", 7, "tin", 1),
                MaterialCatalog.decompositionRatio("nested_bronze"));
        assertSame(
                MaterialCatalog.decompositionRatio("bronze"),
                MaterialCatalog.decompositionRatio(MaterialCatalog.require("bronze")));

        var bronze = MaterialCatalog.alloys().match(Map.of("copper", 432, "tin", 144));
        assertTrue(bronze.isPresent());
        assertEquals("bronze", bronze.orElseThrow().result().id());
        assertFalse(MaterialCatalog.alloys().match(Map.of("copper", 500, "tin", 144)).isPresent());
        assertEquals(
                "bronze",
                CrucibleTransferCoordinator.resolveCurrentMaterial(
                                Map.of("copper", 432, "tin", 144))
                        .orElseThrow()
                        .id());
        var bronzeDrain = MoltenTransferMath.planDrain(
                Map.of("copper", 432, "tin", 144),
                MaterialCatalog.decompositionRatio("bronze"),
                144);
        assertEquals(144, bronzeDrain.orElseThrow().amount());
        assertEquals(Map.of("copper", 108, "tin", 36), bronzeDrain.orElseThrow().removals());
        assertTrue(CrucibleTransferCoordinator.resolveCurrentMaterial(
                        Map.of("copper", 500, "tin", 144))
                .isEmpty());

        var simulated = CrucibleTransferCoordinator.planFill(
                MaterialCatalog.require("steel"),
                MaterialPrefixes.INGOT.units(),
                0,
                MaterialPrefixes.INGOT.units() * 8,
                "ceramic",
                false);
        assertTrue(simulated.isEmpty(), "ceramic must reject steel in both simulation and execution");

        CompositionTank tank = new CompositionTank();
        tank.addAll(Map.of("copper", 3));
        assertEquals(3, tank.totalUnits());
        tank.addAll(Map.of("tin", 1));
        assertEquals(4, tank.totalUnits(), "composition mutation must invalidate totals");
        assertEquals("bronze", tank.alloy().orElseThrow().result().id());

        MachineCasing casing = new MachineCasing(Device.CRUCIBLE, 800.0);
        assertTrue(
                casing.massGrams() + tank.contentsWeightGrams(0.15) > casing.massGrams(),
                "thermal mass must include both casing and contents");
        assertTrue(tank.removeBoiling(3_000.0F));
        assertEquals(0, tank.totalUnits(), "boiling must invalidate composition caches");

        CompositionTank unknownTank = new CompositionTank();
        unknownTank.replace(Map.of("removed_material", 144));
        assertTrue(unknownTank.unknownMaterials());
        assertTrue(Double.isInfinite(unknownTank.moltenThreshold()));

        CompositionTank steelCharge = new CompositionTank();
        steelCharge.addAll(Map.of("iron", 432, "carbon", 144));
        SteelmakingController steelmaking = new SteelmakingController();
        assertEquals(
                SteelmakingController.InjectionResult.STARTED,
                steelmaking.insertAir(10L, steelCharge.composition(), 1538.0F));
        assertTrue(steelmaking.blocksFluidTransfer());
        steelCharge.addAll(Map.of("iron", 1));
        assertTrue(steelmaking.onCompositionChanged(), "adding material must reset steelmaking");
        assertFalse(steelmaking.blocksFluidTransfer());

        var startupIron = MaterialCatalog.startupValues().stream()
                .filter(material -> material.id().equals("iron"))
                .findFirst()
                .orElseThrow();
        String startupStructure = MaterialFingerprint.structure(MaterialCatalog.startupValues());
        CompositionTank runtimeTank = new CompositionTank();
        runtimeTank.replace(Map.of("iron", MaterialPrefixes.INGOT.units()));
        double startupThreshold = runtimeTank.moltenThreshold();
        int startupColor = runtimeTank.moltenColor();
        long initialRevision = MaterialCatalog.runtimeRevision();
        MaterialCatalog.publishRuntime(
                List.of(new MaterialTuning(
                        "iron",
                        Optional.of(7),
                        Optional.of("#010203"),
                        Optional.of(1600.0),
                        Optional.empty(),
                        Optional.empty())),
                Map.of("iron/" + MaterialPrefixes.INGOT.serializedId(), "example:preferred_iron"));
        assertEquals(initialRevision + 1, MaterialCatalog.runtimeRevision());
        assertEquals(7, MaterialCatalog.require("iron").tier());
        assertEquals(startupIron.forms(), MaterialCatalog.require("iron").forms());
        assertEquals(startupIron.gt6Metadata(), MaterialCatalog.require("iron").gt6Metadata(),
                "runtime tuning must preserve source thermal metadata including plasma");
        assertEquals(startupStructure, MaterialFingerprint.structure(MaterialCatalog.values()));
        assertEquals(
                Optional.of("example:preferred_iron"),
                MaterialCatalog.preferredItem("iron", MaterialPrefixes.INGOT));
        assertEquals(1600.0, runtimeTank.moltenThreshold(),
                "derived caches must observe tuning revision changes");
        assertEquals(0x010203, runtimeTank.moltenColor());
        assertTrue(startupThreshold != runtimeTank.moltenThreshold());
        assertTrue(startupColor != runtimeTank.moltenColor());
        long publishedRevision = MaterialCatalog.runtimeRevision();
        assertThrows(
                IllegalArgumentException.class,
                () -> MaterialCatalog.publishRuntime(
                        List.of(new MaterialTuning(
                                "removed_material",
                                Optional.of(1),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty())),
                        Map.of()));
        assertEquals(publishedRevision, MaterialCatalog.runtimeRevision(),
                "rejected publication must not advance revision");
        assertEquals(7, MaterialCatalog.require("iron").tier(),
                "a rejected reload must leave the previous snapshot intact");
        assertEquals(
                Optional.of("example:preferred_iron"),
                MaterialCatalog.preferredItem("iron", MaterialPrefixes.INGOT));
        MaterialCatalog.publishRuntime(List.of(), Map.of());
        assertEquals(publishedRevision + 1, MaterialCatalog.runtimeRevision());
        assertEquals(startupIron.tier(), MaterialCatalog.require("iron").tier());
        assertTrue(MaterialCatalog.preferredItem("iron", MaterialPrefixes.INGOT).isEmpty());
        assertEquals(startupThreshold, runtimeTank.moltenThreshold());
        assertEquals(startupColor, runtimeTank.moltenColor());

        RecipeMap stagedMap = new RecipeMap(
                net.minecraft.resources.ResourceLocation.parse("test:transaction"));
        stagedMap.replaceRecipes(List.of());
        Item firstOutput = Items.GOLD_INGOT;
        Item secondOutput = Items.DIAMOND;
        Item failedOutput = Items.EMERALD;
        String preferenceKey = "iron/" + MaterialPrefixes.INGOT.serializedId();
        MaterialCatalog.RuntimePreview firstPreview = MaterialCatalog.previewRuntime(
                List.of(), Map.of(preferenceKey, "test:first"));
        GTRecipeRuntimeEpoch.publish(firstPreview, List.of(stagedMap.prepareRecipes(List.of(
                new RecipeMap.Entry(
                        net.minecraft.resources.ResourceLocation.parse("test:first"),
                        outputRecipe(firstOutput))))));
        assertEquals(
                Optional.of("test:first"),
                MaterialCatalog.preferredItem("iron", MaterialPrefixes.INGOT));
        assertSame(
                firstOutput,
                stagedMap.recipes().getFirst().itemOutputs().getFirst().getItem());

        MaterialCatalog.RuntimePreview secondPreview = MaterialCatalog.previewRuntime(
                List.of(), Map.of(preferenceKey, "test:second"));
        GTRecipeRuntimeEpoch.publish(secondPreview, List.of(stagedMap.prepareRecipes(List.of(
                new RecipeMap.Entry(
                        net.minecraft.resources.ResourceLocation.parse("test:second"),
                        outputRecipe(secondOutput))))));
        assertEquals(
                Optional.of("test:second"),
                MaterialCatalog.preferredItem("iron", MaterialPrefixes.INGOT));
        assertSame(
                secondOutput,
                stagedMap.recipes().getFirst().itemOutputs().getFirst().getItem());

        RecipeMap.Prepared validMap = stagedMap.prepareRecipes(List.of(
                new RecipeMap.Entry(
                        net.minecraft.resources.ResourceLocation.parse("test:failed"),
                        outputRecipe(failedOutput))));
        RecipeMap staleOwner = new RecipeMap(
                net.minecraft.resources.ResourceLocation.parse("test:stale_transaction"));
        staleOwner.replaceRecipes(List.of());
        RecipeMap.Prepared staleMap = staleOwner.prepareRecipes(List.of());
        staleOwner.replaceRecipes(List.of());
        long revisionBeforeRejectedEpoch = MaterialCatalog.runtimeRevision();
        long mapRevisionBeforeRejectedEpoch = stagedMap.revision();
        List<RecipeMap.Entry> entriesBeforeRejectedEpoch = stagedMap.entries();
        long staleRevisionBeforeRejectedEpoch = staleOwner.revision();
        long epochBeforeRejectedEpoch = GTRecipeRuntimeEpoch.epoch();
        MaterialCatalog.RuntimePreview preview = MaterialCatalog.previewRuntime(
                List.of(), Map.of(preferenceKey, "test:failed"));
        RecipeMap.Prepared acceptedCandidate = validMap;
        assertThrows(
                IllegalStateException.class,
                () -> GTRecipeRuntimeEpoch.publish(
                        preview, List.of(acceptedCandidate, staleMap)));
        assertEquals(revisionBeforeRejectedEpoch, MaterialCatalog.runtimeRevision(),
                "failed central publication must not publish material tuning");
        assertEquals(mapRevisionBeforeRejectedEpoch, stagedMap.revision(),
                "failed central publication must not replace an earlier valid map");
        assertSame(entriesBeforeRejectedEpoch, stagedMap.entries(),
                "failed reload must retain the exact prior map snapshot");
        assertEquals(staleRevisionBeforeRejectedEpoch, staleOwner.revision(),
                "failed central publication must not replace the stale map");
        assertEquals(epochBeforeRejectedEpoch, GTRecipeRuntimeEpoch.epoch(),
                "failed central publication must not advance the epoch");
        assertEquals(
                Optional.of("test:second"),
                MaterialCatalog.preferredItem("iron", MaterialPrefixes.INGOT));
        assertSame(
                secondOutput,
                stagedMap.recipes().getFirst().itemOutputs().getFirst().getItem());
    }

    @Test
    void memoizesDiamondDependencySubtreeAcrossTheCatalog() {
        MaterialDefinition leaf = material("memo_leaf", Map.of());
        MaterialDefinition shared = material("memo_shared", Map.of("memo_leaf", 1));
        MaterialDefinition left = material("memo_left", Map.of("memo_shared", 1));
        MaterialDefinition right = material("memo_right", Map.of("memo_shared", 1));
        MaterialDefinition root = material(
                "memo_root",
                Map.of("memo_left", 1, "memo_right", 1));
        CountingDefinitions definitions = new CountingDefinitions();
        definitions.put(root.id(), root);
        definitions.put(left.id(), left);
        definitions.put(right.id(), right);
        definitions.put(shared.id(), shared);
        definitions.put(leaf.id(), leaf);

        Map<String, Integer> quanta = MaterialCatalog.calculateQuanta(definitions);

        assertEquals(2, quanta.get(root.id()));
        assertEquals(1, definitions.getCount(leaf.id()),
                "the shared subtree must be traversed only once");
        assertEquals(
                Map.of("memo_leaf", 2),
                MaterialCatalog.decompose(root, 2, definitions));
    }

    @Test
    void rejectsUnitsNotDivisibleByDecompositionQuantum() {
        MaterialDefinition copper = material("exact_copper", Map.of());
        MaterialDefinition tin = material("exact_tin", Map.of());
        MaterialDefinition bronze = material(
                "exact_bronze",
                Map.of("exact_copper", 3, "exact_tin", 1));
        Map<String, MaterialDefinition> definitions = new LinkedHashMap<>();
        definitions.put(copper.id(), copper);
        definitions.put(tin.id(), tin);
        definitions.put(bronze.id(), bronze);

        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> MaterialCatalog.decompose(bronze, 3, definitions));

        assertTrue(failure.getMessage().contains(
                "exact_bronze cannot decompose 3 units exactly"));
    }

    @Test
    void rejectsDirectCyclesThatBypassTheMaterialLoaderWithFullPath() {
        MaterialDefinition a = material("cycle_a", Map.of("cycle_b", 1));
        MaterialDefinition b = material("cycle_b", Map.of("cycle_a", 1));
        Map<String, MaterialDefinition> definitions = new LinkedHashMap<>();
        definitions.put(a.id(), a);
        definitions.put(b.id(), b);

        IllegalArgumentException quantumFailure = assertThrows(
                IllegalArgumentException.class,
                () -> MaterialCatalog.calculateQuanta(definitions));
        assertTrue(quantumFailure.getMessage().contains(
                "cycle_a -> cycle_b -> cycle_a"));

        IllegalArgumentException decompositionFailure = assertThrows(
                IllegalArgumentException.class,
                () -> MaterialCatalog.decompose(a, 1, definitions));
        assertTrue(decompositionFailure.getMessage().contains(
                "cycle_a -> cycle_b -> cycle_a"));
    }

    @Test
    void rejectsMissingFormItemTargetsAfterRegistriesPopulate() {
        MaterialDefinition missing = new MaterialDefinition(
                "missing_override",
                "missing_override",
                Optional.empty(),
                0,
                "#808080",
                "matte",
                List.of(MaterialPrefixes.INGOT),
                Map.of(MaterialPrefixes.INGOT, "missingmod:not_installed"),
                new ThermalProperties(1.0),
                false,
                Map.of(),
                false);

        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> MaterialCatalog.validateFormItemMappings(List.of(missing)));

        assertTrue(failure.getMessage().contains("missing_override"));
        assertTrue(failure.getMessage().contains(MaterialPrefixes.INGOT.serializedId()));
        assertTrue(failure.getMessage().contains("missingmod:not_installed"));
        assertTrue(failure.getMessage().contains("install the providing mod"));
    }

    private static MaterialDefinition material(
            String id,
            Map<String, Integer> composition) {
        return new MaterialDefinition(
                id,
                id,
                Optional.empty(),
                0,
                "#808080",
                "matte",
                List.of(MaterialPrefixes.DUST),
                Map.of(),
                new ThermalProperties(1.0),
                false,
                composition,
                false);
    }

    private static GTRecipe outputRecipe(Item output) {
        return new GTRecipe(
                List.of(Ingredient.of(Items.STONE)),
                List.of(1),
                List.of(new ItemStack(output)),
                List.of(),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                20,
                8,
                0,
                true);
    }

    private static final class CountingDefinitions
            extends LinkedHashMap<String, MaterialDefinition> {
        private final Map<String, Integer> getCounts = new java.util.HashMap<>();

        @Override
        public MaterialDefinition get(Object key) {
            getCounts.merge(String.valueOf(key), 1, Integer::sum);
            return super.get(key);
        }

        private int getCount(String id) {
            return getCounts.getOrDefault(id, 0);
        }
    }
}
