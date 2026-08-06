package com.masson.cruciblecraft.recipe.gt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.material.def.ThermalProperties;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.masson.cruciblecraft.recipe.rule.MaterialRule;
import com.masson.cruciblecraft.recipe.rule.MaterialRuleExpansion;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.fml.loading.LoadingModList;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtruderRecipeFamilyProviderTest {
    private static final ResourceLocation EXTRUDER =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "extruder");
    private static final String TEMPLATE_ID = "sha256:" + "0".repeat(64);
    private static final MaterialRuleExpansion.ResourceResolver RESOLVER =
            new MaterialRuleExpansion.ResourceResolver() {
                @Override
                public Optional<Ingredient> itemInput(
                        MaterialDefinition material,
                        Optional<MaterialPrefix> prefix,
                        Optional<ResourceLocation> fixed) {
                    return Optional.of(Ingredient.of(Items.IRON_INGOT));
                }

                @Override
                public Optional<Item> itemOutput(
                        MaterialDefinition material,
                        Optional<MaterialPrefix> prefix,
                        Optional<ResourceLocation> fixed) {
                    return Optional.of(Items.IRON_NUGGET);
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

    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        if (!MaterialPrefixCatalog.isBootstrapped()) {
            MaterialPrefixCatalog.bootstrap(null);
        }
    }

    @Test
    void nineteenSourcesEnumerateIdenticallyOnServerAndDedicatedClient() {
        List<MaterialDefinition> materials = materials(19);
        List<ExtruderRecipeFamilyProvider.Source> sources =
                oneRelationSources(materials);

        var server = prepare(
                sources,
                materials,
                ExtruderRecipeFamilyProvider.RuntimeSide.SERVER);
        var client = prepare(
                sources,
                materials,
                ExtruderRecipeFamilyProvider.RuntimeSide.DEDICATED_CLIENT);

        assertEquals(19, server.logicalRecipeCount());
        assertEquals(server.recipeIds(), client.recipeIds());
        assertEquals(server.stableFingerprint(), client.stableFingerprint());
        for (int index = 0; index < server.logicalRecipeCount(); index++) {
            assertEquals(
                    server.recipeIds().get(index),
                    server.enumerationEntry(index).id());
            assertEquals(
                    client.recipeIds().get(index),
                    client.enumerationEntry(index).id());
        }
    }

    @Test
    void relationGapsRemainStableAndAreNotRenumbered() {
        List<MaterialDefinition> materials = materials(2);
        List<MaterialRule.SparseRelation> relations = List.of(
                relation(materials.get(0).id(), 4),
                relation(materials.get(1).id(), 17));
        var snapshot = prepare(
                List.of(source(0, relations)),
                materials,
                ExtruderRecipeFamilyProvider.RuntimeSide.SERVER);

        assertEquals(
                relations.stream()
                        .map(MaterialRule.SparseRelation::stableId)
                        .toList(),
                snapshot.recipeIds());
        assertEquals(2, snapshot.logicalRecipeCount());
        assertDoesNotThrow(() -> snapshot.enumerationEntry(0));
        assertDoesNotThrow(() -> snapshot.enumerationEntry(1));
    }

    @Test
    void fullyDisabledFamilyIsEnumerableOnBothRuntimeSides() {
        var server = prepare(
                List.of(),
                List.of(),
                ExtruderRecipeFamilyProvider.RuntimeSide.SERVER);
        var client = prepare(
                List.of(),
                List.of(),
                ExtruderRecipeFamilyProvider.RuntimeSide.DEDICATED_CLIENT);

        assertEquals(0, server.logicalRecipeCount());
        assertEquals(List.of(), server.recipeIds());
        assertEquals(server.recipeIds(), client.recipeIds());
        assertEquals(server.stableFingerprint(), client.stableFingerprint());
        assertNotEquals(
                server.stableFingerprint(),
                prepare(
                        oneRelationSources(materials(1)),
                        materials(1),
                        ExtruderRecipeFamilyProvider.RuntimeSide.SERVER)
                        .stableFingerprint());
    }

    @Test
    void extruderFamilyRejectsStandaloneReplacementBeforeMutation() {
        List<MaterialDefinition> materials = materials(1);
        var snapshot = prepare(
                oneRelationSources(materials),
                materials,
                ExtruderRecipeFamilyProvider.RuntimeSide.SERVER);
        RecipeMap map = new RecipeMap(EXTRUDER);
        map.prepareRecipes(List.of(), List.of(snapshot), 1L).publish();
        long revision = map.revision();
        long runtimeEpoch = map.runtimeEpoch();
        List<RecipeMap.Entry> logical = List.copyOf(map.entries());

        assertThrows(
                UnsupportedOperationException.class,
                () -> map.replaceRecipes(List.of(
                        snapshot.enumerationEntry(0))));

        assertEquals(revision, map.revision());
        assertEquals(runtimeEpoch, map.runtimeEpoch());
        assertEquals(logical, map.entries());
        assertSame(
                snapshot,
                map.family(ExtruderRecipeFamilyProvider.FAMILY_ID)
                        .orElseThrow());
    }

    @Test
    void ordinaryMapStillSupportsStandaloneReplacement() {
        List<MaterialDefinition> materials = materials(1);
        RecipeMap.Entry entry = prepare(
                oneRelationSources(materials),
                materials,
                ExtruderRecipeFamilyProvider.RuntimeSide.SERVER)
                .enumerationEntry(0);
        RecipeMap map = new RecipeMap(id("ordinary_map"));

        map.replaceRecipes(List.of(entry));

        assertEquals(1L, map.revision());
        assertEquals(0L, map.runtimeEpoch());
        assertEquals(List.of(entry), map.entries());
        assertTrue(map.family(
                ExtruderRecipeFamilyProvider.FAMILY_ID).isEmpty());
    }

    @Test
    void lookupIdentityIgnoresInputCountButFingerprintRetainsIt() {
        MaterialDefinition material = materials(1).getFirst();
        MaterialRule.SparseRelation countOne = countedRelation(
                material.id(), "count_one", 1, 1);
        MaterialRule.SparseRelation countTwo = countedRelation(
                material.id(), "count_two", 2, 2);

        IllegalArgumentException duplicateLookup = assertThrows(
                IllegalArgumentException.class,
                () -> prepare(
                        List.of(
                                source(0, List.of(countOne)),
                                source(1, List.of(countTwo))),
                        List.of(material),
                        ExtruderRecipeFamilyProvider.RuntimeSide.SERVER));
        assertTrue(duplicateLookup.getMessage().contains(
                "Duplicate compact input signature"));

        MaterialRule.SparseRelation sameIdCountOne = countedRelation(
                material.id(), "fingerprint", 1, 1);
        MaterialRule.SparseRelation sameIdCountTwo = countedRelation(
                material.id(), "fingerprint", 2, 1);
        String firstFingerprint = prepare(
                List.of(source(0, List.of(sameIdCountOne))),
                List.of(material),
                ExtruderRecipeFamilyProvider.RuntimeSide.SERVER)
                .stableFingerprint();
        String secondFingerprint = prepare(
                List.of(source(0, List.of(sameIdCountTwo))),
                List.of(material),
                ExtruderRecipeFamilyProvider.RuntimeSide.SERVER)
                .stableFingerprint();
        assertNotEquals(firstFingerprint, secondFingerprint);
    }

    @Test
    void lazyLruEvictionRematerializesNonBufferableRecipe() {
        List<MaterialDefinition> materials = materials(641);
        List<MaterialRule.SparseRelation> relations =
                new ArrayList<>(materials.size());
        Map<String, Item> inputItems = new HashMap<>();
        List<Item> uniqueInputs = BuiltInRegistries.ITEM.stream()
                .filter(item -> item != Items.AIR)
                .limit(materials.size())
                .toList();
        assertEquals(materials.size(), uniqueInputs.size());
        for (int index = 0; index < materials.size(); index++) {
            MaterialDefinition material = materials.get(index);
            relations.add(relation(material.id(), index + 1));
            inputItems.put(
                    material.id(),
                    uniqueInputs.get(index));
        }
        MaterialRuleExpansion.ResourceResolver resolver =
                new MaterialRuleExpansion.ResourceResolver() {
                    @Override
                    public Optional<Ingredient> itemInput(
                            MaterialDefinition material,
                            Optional<MaterialPrefix> prefix,
                            Optional<ResourceLocation> fixed) {
                        return Optional.of(Ingredient.of(
                                fixed.<Item>flatMap(
                                                BuiltInRegistries.ITEM
                                                        ::getOptional)
                                        .orElseGet(() -> inputItems.get(
                                                material.id()))));
                    }

                    @Override
                    public Optional<Item> itemOutput(
                            MaterialDefinition material,
                            Optional<MaterialPrefix> prefix,
                            Optional<ResourceLocation> fixed) {
                        return Optional.of(Items.IRON_NUGGET);
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
        var snapshot = ExtruderRecipeFamilyProvider.prepare(
                List.of(new ExtruderRecipeFamilyProvider.Source(
                        id("extruder/compact/lru"),
                        sparseRule(0, relations, false))),
                materials,
                resolver,
                MaterialRuleExpansion.FormIndexes.factualOnly(materials),
                1L,
                ExtruderRecipeFamilyProvider.RuntimeSide.SERVER);

        RecipeMap.Entry first = null;
        for (int index = 0; index < relations.size(); index++) {
            if (relations.get(index).shadowOrder()
                    % ExtruderRecipeFamilyProvider.HOT_MODULO == 0) {
                continue;
            }
            int relationIndex = index;
            RecipeMap.Entry found = snapshot.findLazy(
                    GTRecipeQuery.items(
                            new ItemStack(inputItems.get(
                                    materials.get(index).id())),
                            new ItemStack(Items.STICK)))
                    .orElseThrow(() -> new AssertionError(
                            "Missing lazy relation " + relationIndex
                                    + " for " + BuiltInRegistries.ITEM.getKey(
                                            inputItems.get(materials.get(
                                                    relationIndex).id()))));
            if (first == null) {
                first = found;
            }
        }
        assertEquals(
                ExtruderRecipeFamilyProvider.CACHE_CEILING,
                snapshot.cacheSize());

        RecipeMap.Entry rematerialized = snapshot.findLazy(
                GTRecipeQuery.items(
                        new ItemStack(inputItems.get(
                                materials.getFirst().id())),
                        new ItemStack(Items.STICK)))
                .orElseThrow();
        assertNotSame(first, rematerialized);
        assertNotSame(first.recipe(), rematerialized.recipe());
        assertFalse(rematerialized.recipe().canBeBuffered());
    }

    @Test
    void sourceAndRelationCeilingsRemainFailClosed() {
        List<MaterialDefinition> twentyOneMaterials = materials(21);
        IllegalArgumentException sourceOverflow = assertThrows(
                IllegalArgumentException.class,
                () -> prepare(
                        oneRelationSources(twentyOneMaterials),
                        twentyOneMaterials,
                        ExtruderRecipeFamilyProvider.RuntimeSide.SERVER));
        assertTrue(sourceOverflow.getMessage().contains("source_20"));

        List<MaterialDefinition> tooManyMaterials =
                materials(ExtruderRecipeFamilyProvider.LOGICAL_RELATIONS + 1);
        List<MaterialRule.SparseRelation> tooManyRelations =
                new ArrayList<>(tooManyMaterials.size());
        for (int index = 0; index < tooManyMaterials.size(); index++) {
            tooManyRelations.add(relation(
                    tooManyMaterials.get(index).id(), index));
        }
        IllegalArgumentException relationOverflow = assertThrows(
                IllegalArgumentException.class,
                () -> prepare(
                        List.of(source(0, tooManyRelations)),
                        tooManyMaterials,
                        ExtruderRecipeFamilyProvider.RuntimeSide.SERVER));
        assertTrue(relationOverflow.getMessage().contains("source_0"));
        assertTrue(relationOverflow.getMessage().contains(
                tooManyRelations.getLast().stableId().toString()));
        assertTrue(relationOverflow.getMessage().contains("2782"));
    }

    @Test
    void duplicateNegativeAndInvalidRelationsRemainRejectedWithIdentity() {
        List<MaterialDefinition> duplicateMaterials = materials(2);
        List<ExtruderRecipeFamilyProvider.Source> duplicateOrder = List.of(
                source(0, List.of(relation(
                        duplicateMaterials.get(0).id(), 7))),
                source(1, List.of(relation(
                        duplicateMaterials.get(1).id(), 7))));
        IllegalArgumentException duplicate = assertThrows(
                IllegalArgumentException.class,
                () -> prepare(
                        duplicateOrder,
                        duplicateMaterials,
                        ExtruderRecipeFamilyProvider.RuntimeSide.SERVER));
        assertTrue(duplicate.getMessage().contains("source_0"));
        assertTrue(duplicate.getMessage().contains("source_1"));
        assertTrue(duplicate.getMessage().contains("shadow_order"));

        MaterialRule.SparseRelation valid =
                relation(duplicateMaterials.getFirst().id(), 0);
        IllegalArgumentException negative = assertThrows(
                IllegalArgumentException.class,
                () -> new MaterialRule.SparseRelation(
                        valid.stableId(),
                        valid.material(),
                        valid.input(),
                        valid.output(),
                        valid.duration(),
                        valid.eut(),
                        valid.fallback(),
                        valid.forgingTarget(),
                        valid.plateGem(),
                        valid.heatMode(),
                        -1));
        assertTrue(negative.getMessage().contains(
                valid.stableId().toString()));

        MaterialRule.SparseRelation invalidLookup =
                new MaterialRule.SparseRelation(
                        valid.stableId(),
                        valid.material(),
                        new MaterialRule.SparseResource(
                                "not_a_registered_prefix", 1),
                        valid.output(),
                        valid.duration(),
                        valid.eut(),
                        valid.fallback(),
                        valid.forgingTarget(),
                        valid.plateGem(),
                        valid.heatMode(),
                        valid.shadowOrder());
        ResourceLocation sourceId = id("extruder/compact/source_9");
        IllegalArgumentException invalid = assertThrows(
                IllegalArgumentException.class,
                () -> prepare(
                        List.of(new ExtruderRecipeFamilyProvider.Source(
                                sourceId,
                                sparseRule(9, List.of(invalidLookup)))),
                        List.of(duplicateMaterials.getFirst()),
                        ExtruderRecipeFamilyProvider.RuntimeSide.SERVER));
        assertTrue(invalid.getMessage().contains(sourceId.toString()));
        assertTrue(invalid.getMessage().contains(
                invalidLookup.stableId().toString()));
        assertTrue(invalid.getMessage().contains("not_a_registered_prefix"));
    }

    private static ExtruderRecipeFamilyProvider.Snapshot prepare(
            List<ExtruderRecipeFamilyProvider.Source> sources,
            List<MaterialDefinition> materials,
            ExtruderRecipeFamilyProvider.RuntimeSide side) {
        return ExtruderRecipeFamilyProvider.prepare(
                sources,
                materials,
                RESOLVER,
                MaterialRuleExpansion.FormIndexes.factualOnly(materials),
                1L,
                side);
    }

    private static List<ExtruderRecipeFamilyProvider.Source>
            oneRelationSources(List<MaterialDefinition> materials) {
        List<ExtruderRecipeFamilyProvider.Source> sources =
                new ArrayList<>(materials.size());
        for (int index = 0; index < materials.size(); index++) {
            sources.add(source(
                    index,
                    List.of(relation(materials.get(index).id(), index))));
        }
        return List.copyOf(sources);
    }

    private static ExtruderRecipeFamilyProvider.Source source(
            int index, List<MaterialRule.SparseRelation> relations) {
        return new ExtruderRecipeFamilyProvider.Source(
                id("extruder/compact/source_" + index),
                sparseRule(index, relations));
    }

    private static MaterialRule sparseRule(
            int index, List<MaterialRule.SparseRelation> relations) {
        return sparseRule(index, relations, true);
    }

    private static MaterialRule sparseRule(
            int index,
            List<MaterialRule.SparseRelation> relations,
            boolean canBeBuffered) {
        MaterialRule.SparseTable table = new MaterialRule.SparseTable(
                ResourceLocation.withDefaultNamespace("stick"),
                "shape_" + index,
                index + 1,
                TEMPLATE_ID,
                "normal",
                relations);
        return new MaterialRule(
                Optional.of(EXTRUDER),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                "1",
                "0",
                "0",
                canBeBuffered,
                Optional.empty(),
                Map.of(),
                List.of(),
                Optional.empty(),
                List.of(),
                Optional.of(table));
    }

    private static MaterialRule.SparseRelation relation(
            String materialId, int shadowOrder) {
        return countedRelation(
                materialId, materialId, 1, shadowOrder);
    }

    private static MaterialRule.SparseRelation countedRelation(
            String materialId,
            String stableSuffix,
            int inputCount,
            int shadowOrder) {
        return new MaterialRule.SparseRelation(
                id("extruder/plate/" + stableSuffix + "/" + materialId),
                materialId,
                new MaterialRule.SparseResource("ingot", inputCount),
                new MaterialRule.SparseResource("plate", 1),
                20,
                8L,
                "none",
                Optional.empty(),
                false,
                "normal",
                shadowOrder);
    }

    private static List<MaterialDefinition> materials(int count) {
        List<MaterialDefinition> materials = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            String materialId = "material_" + index;
            materials.add(new MaterialDefinition(
                    materialId,
                    materialId,
                    Optional.empty(),
                    1,
                    "#808080",
                    "metallic",
                    List.of(MaterialPrefixes.INGOT, MaterialPrefixes.PLATE),
                    Map.of(),
                    new ThermalProperties(1_000),
                    false,
                    Map.of(),
                    false));
        }
        return List.copyOf(materials);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }
}
