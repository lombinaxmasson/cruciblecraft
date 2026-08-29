package com.masson.cruciblecraft.recipe.gt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;
import com.masson.cruciblecraft.registry.ModRecipeMaps;
import com.mojang.serialization.JsonOps;

import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.neoforged.neoforge.fluids.FluidStack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompactRecipeFamilyProviderTest {
    private static RegistryAccess registries;

    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        registries = new RegistryAccess.ImmutableRegistryAccess(
                BuiltInRegistries.REGISTRY.stream().toList());
    }

    @Test
    void codecRoundTripsExactRelationContract() {
        CompactGTRecipeFamilyDefinition original = assemblerFamily(
                "gt.recipe.assembler#0002",
                List.of(itemRelation(
                        "t37/assembler_row",
                        Ingredient.of(Items.IRON_INGOT),
                        new ItemStack(Items.IRON_NUGGET),
                        0)));
        var ops = RegistryOps.create(JsonOps.INSTANCE, registries);
        var encoded = CompactGTRecipeFamilyDefinition.CODEC
                .encodeStart(ops, original)
                .getOrThrow();
        CompactGTRecipeFamilyDefinition decoded =
                CompactGTRecipeFamilyDefinition.CODEC.parse(ops, encoded).getOrThrow();

        assertEquals(original.familyId(), decoded.familyId());
        assertEquals(original.targetMap(), decoded.targetMap());
        assertEquals(original.sourceRevision(), decoded.sourceRevision());
        assertEquals(1, decoded.relations().size());
        assertEquals(
                original.relations().getFirst().stableId(),
                decoded.relations().getFirst().stableId());
        assertSameRecipe(
                original.relations().getFirst().materialize(),
                decoded.relations().getFirst().materialize());
        assertTrue(decoded.parameterized().isEmpty());
        assertTrue(decoded.publicationGroup().isEmpty());
        assertEquals(
                CompactGTRecipeFamilyDefinition
                        .T37_ASSEMBLER_PUBLICATION_GROUP,
                decoded.resolvedPublicationGroup());
    }

    @Test
    void unknownTargetMapFailsClosedBeforeSnapshot() {
        RecipeMap assembler = ModRecipeMaps.ASSEMBLER;
        CompactRecipeFamilySource source = new CompactRecipeFamilySource(
                id("authored/unknown_map"),
                new CompactGTRecipeFamilyDefinition(
                        "gt.recipe.missing#0001",
                        id("missing_map"),
                        "3703e40308c8c030763fd6297dea8b210d2a77b1",
                        List.of(itemRelation(
                                "t37/missing",
                                Ingredient.of(Items.IRON_INGOT),
                                new ItemStack(Items.IRON_NUGGET),
                                0))));
        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> CompactRecipeFamilyProvider.prepareByTarget(
                        List.of(source),
                        Map.of(assembler.id(), assembler),
                        1L,
                        CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                        Map.of(
                                assembler.id(),
                                CompactRecipeFamilyProvider.MaterializationPolicy.immediate())));
        assertTrue(thrown.getMessage().contains("Unknown compact family target map"));
        assertTrue(thrown.getMessage().contains("missing_map"));
    }

    @Test
    void assemblerAndRoasterUseIsolatedTargetPolicies() {
        RecipeMap assembler = ModRecipeMaps.ASSEMBLER;
        RecipeMap roaster = ModRecipeMaps.ROASTER;
        CompactRecipeFamilySource assemblerSource = new CompactRecipeFamilySource(
                id("authored/assembler"),
                assemblerFamily(
                        "gt.recipe.assembler#0002",
                        List.of(itemRelation(
                                "t37/assembler_iron",
                                Ingredient.of(Items.IRON_INGOT),
                                new ItemStack(Items.IRON_NUGGET),
                                0))));
        CompactRecipeFamilySource roasterSource = new CompactRecipeFamilySource(
                id("authored/roaster"),
                new CompactGTRecipeFamilyDefinition(
                        "gt.recipe.roaster#0001",
                        roaster.id(),
                        "3703e40308c8c030763fd6297dea8b210d2a77b1",
                        List.of(
                                fluidRelation(
                                        "t38/roaster_water",
                                        new FluidStack(Fluids.WATER, 250),
                                        new ItemStack(Items.GLASS_BOTTLE),
                                        0),
                                itemRelation(
                                        "t38/roaster_iron",
                                        Ingredient.of(Items.IRON_INGOT),
                                        new ItemStack(Items.IRON_NUGGET),
                                        1))));
        Map<ResourceLocation, RecipeMap> known = new HashMap<>();
        known.put(assembler.id(), assembler);
        known.put(roaster.id(), roaster);

        var snapshots = CompactRecipeFamilyProvider.prepareByTarget(
                List.of(assemblerSource, roasterSource),
                known,
                9L,
                CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                Map.of(
                        assembler.id(),
                        CompactRecipeFamilyProvider.MaterializationPolicy.hybrid(
                                8, CompactRecipeFamilyProvider.EagerSelector.NONE),
                        roaster.id(),
                        CompactRecipeFamilyProvider.MaterializationPolicy.onDemand(16)));

        assertEquals(2, snapshots.size());
        assertTrue(snapshots.containsKey(assembler.id()));
        assertTrue(snapshots.containsKey(roaster.id()));
        assertEquals(
                CompactRecipeFamilyProvider.familyId(assembler.id()),
                snapshots.get(assembler.id()).familyId());
        assertEquals(
                CompactRecipeFamilyProvider.familyId(roaster.id()),
                snapshots.get(roaster.id()).familyId());
        assertNotEquals(
                snapshots.get(assembler.id()).familyId(),
                snapshots.get(roaster.id()).familyId());
        assertEquals(
                id("t37/assembler_iron"),
                snapshots.get(assembler.id()).recipeIds().getFirst());
        assertEquals(
                CompactRecipeFamilyProvider.MaterializationStrategy.HYBRID,
                snapshots.get(assembler.id()).policy().strategy());
        assertEquals(8, snapshots.get(assembler.id()).cacheCeiling());
        assertEquals(
                CompactRecipeFamilyProvider.MaterializationStrategy.ON_DEMAND,
                snapshots.get(roaster.id()).policy().strategy());
        assertEquals(16, snapshots.get(roaster.id()).cacheCeiling());
        assertEquals(2, snapshots.get(roaster.id()).logicalRecipeCount());
        assertEquals(0, snapshots.get(roaster.id()).eagerRecipeCount());
        assertEquals(2, snapshots.get(roaster.id()).lazyRecipeCount());
        assertEquals(
                id("t38/roaster_water"),
                snapshots.get(roaster.id()).recipeIds().getFirst());
    }

    @Test
    void centrifugePublicationGroupsUseDistinctPoliciesAndFamilyIds() {
        RecipeMap centrifuge = ModRecipeMaps.CENTRIFUGE;
        ResourceLocation singletonGroup = CompactGTRecipeFamilyDefinition
                .T39_CENTRIFUGE_SINGLETON_PUBLICATION_GROUP;
        ResourceLocation multiGroup = CompactGTRecipeFamilyDefinition
                .T39_CENTRIFUGE_MULTI_PUBLICATION_GROUP;
        CompactRecipeFamilySource singleton = source(
                centrifuge,
                singletonGroup,
                "gt.recipe.centrifuge#0001",
                itemRelation(
                        "t39/centrifuge_singleton",
                        Ingredient.of(Items.IRON_INGOT),
                        new ItemStack(Items.IRON_NUGGET),
                        0));
        CompactRecipeFamilySource multi = source(
                centrifuge,
                multiGroup,
                "gt.recipe.centrifuge#0002",
                itemRelation(
                        "t39/centrifuge_multi",
                        Ingredient.of(Items.GOLD_INGOT),
                        new ItemStack(Items.GOLD_NUGGET),
                        1));
        PublicationGroupKey singletonKey = new PublicationGroupKey(
                centrifuge.id(), singletonGroup);
        PublicationGroupKey multiKey = new PublicationGroupKey(
                centrifuge.id(), multiGroup);

        var snapshots =
                CompactRecipeFamilyProvider.prepareByPublicationGroup(
                        List.of(multi, singleton),
                        Map.of(centrifuge.id(), centrifuge),
                        39L,
                        CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                        Map.of(
                                singletonKey,
                                CompactRecipeFamilyProvider
                                        .MaterializationPolicy.immediate(),
                                multiKey,
                                CompactRecipeFamilyProvider
                                        .MaterializationPolicy.onDemand(5)));

        assertEquals(List.of(multiKey, singletonKey), snapshots.keySet().stream()
                .sorted()
                .toList());
        assertEquals(39L, snapshots.get(singletonKey).epoch());
        assertEquals(39L, snapshots.get(multiKey).epoch());
        assertEquals(
                CompactRecipeFamilyProvider.familyId(
                        centrifuge.id(), singletonGroup),
                snapshots.get(singletonKey).familyId());
        assertEquals(
                CompactRecipeFamilyProvider.familyId(
                        centrifuge.id(), multiGroup),
                snapshots.get(multiKey).familyId());
        assertEquals(
                CompactRecipeFamilyProvider.MaterializationStrategy.IMMEDIATE,
                snapshots.get(singletonKey).policy().strategy());
        assertEquals(
                CompactRecipeFamilyProvider.MaterializationStrategy.ON_DEMAND,
                snapshots.get(multiKey).policy().strategy());
        assertEquals(5, snapshots.get(multiKey).cacheCeiling());
    }

    @Test
    void assemblerT41GroupsKeepT37HistoricalIdentity() {
        RecipeMap assembler = ModRecipeMaps.ASSEMBLER;
        CompactRecipeFamilySource historical = source(
                assembler,
                "gt.recipe.assembler#0002",
                itemRelation(
                        "t37/historical_oak",
                        Ingredient.of(Items.OAK_PLANKS),
                        new ItemStack(Items.OAK_BUTTON),
                        0));
        CompactRecipeFamilySource planks = source(
                assembler,
                CompactGTRecipeFamilyDefinition.T41_ASSEMBLER_PLANKS_PUBLICATION_GROUP,
                "gt.recipe.assembler#0052",
                itemRelation(
                        "t41/planks_row",
                        Ingredient.of(Items.SPRUCE_PLANKS),
                        new ItemStack(Items.SPRUCE_BUTTON),
                        1));
        CompactRecipeFamilySource fireproof = source(
                assembler,
                CompactGTRecipeFamilyDefinition.T41_ASSEMBLER_FIREPROOF_PUBLICATION_GROUP,
                "gt.recipe.assembler#0137",
                itemRelation(
                        "t41/fireproof_row",
                        Ingredient.of(Items.BIRCH_PLANKS),
                        new ItemStack(Items.BIRCH_BUTTON),
                        2));
        CompactRecipeFamilySource planks2 = source(
                assembler,
                CompactGTRecipeFamilyDefinition.T41_ASSEMBLER_PLANKS2_PUBLICATION_GROUP,
                "gt.recipe.assembler#0218",
                itemRelation(
                        "t41/planks2_row",
                        Ingredient.of(Items.JUNGLE_PLANKS),
                        new ItemStack(Items.JUNGLE_BUTTON),
                        3));
        PublicationGroupKey t37Key = new PublicationGroupKey(
                assembler.id(),
                CompactGTRecipeFamilyDefinition.T37_ASSEMBLER_PUBLICATION_GROUP);
        PublicationGroupKey planksKey = new PublicationGroupKey(
                assembler.id(),
                CompactGTRecipeFamilyDefinition.T41_ASSEMBLER_PLANKS_PUBLICATION_GROUP);
        PublicationGroupKey fireproofKey = new PublicationGroupKey(
                assembler.id(),
                CompactGTRecipeFamilyDefinition.T41_ASSEMBLER_FIREPROOF_PUBLICATION_GROUP);
        PublicationGroupKey planks2Key = new PublicationGroupKey(
                assembler.id(),
                CompactGTRecipeFamilyDefinition.T41_ASSEMBLER_PLANKS2_PUBLICATION_GROUP);

        var snapshots = CompactRecipeFamilyProvider.prepareByPublicationGroup(
                List.of(historical, planks, fireproof, planks2),
                Map.of(assembler.id(), assembler),
                41L,
                CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                Map.of(
                        t37Key,
                        CompactRecipeFamilyProvider.MaterializationPolicy.hybrid(
                                8, CompactRecipeFamilyProvider.EagerSelector.NONE),
                        planksKey,
                        CompactRecipeFamilyProvider.MaterializationPolicy.hybrid(
                                16, CompactRecipeFamilyProvider.EagerSelector.NONE),
                        fireproofKey,
                        CompactRecipeFamilyProvider.MaterializationPolicy.hybrid(
                                16, CompactRecipeFamilyProvider.EagerSelector.NONE),
                        planks2Key,
                        CompactRecipeFamilyProvider.MaterializationPolicy.onDemand(16)));

        assertEquals(4, snapshots.size());
        assertEquals(
                CompactRecipeFamilyProvider.familyId(assembler.id()),
                snapshots.get(t37Key).familyId());
        assertEquals(
                CompactRecipeFamilyProvider.familyId(
                        assembler.id(),
                        CompactGTRecipeFamilyDefinition.T41_ASSEMBLER_PLANKS_PUBLICATION_GROUP),
                snapshots.get(planksKey).familyId());
        assertEquals(1, snapshots.get(t37Key).logicalRecipeCount());
        assertEquals(1, snapshots.get(planksKey).logicalRecipeCount());
        assertEquals(1, snapshots.get(fireproofKey).logicalRecipeCount());
        assertEquals(1, snapshots.get(planks2Key).logicalRecipeCount());
        assertEquals(16, snapshots.get(planksKey).cacheCeiling());
        assertEquals(16, snapshots.get(fireproofKey).cacheCeiling());
        assertEquals(16, snapshots.get(planks2Key).cacheCeiling());
        assertEquals(
                CompactRecipeFamilyProvider.MaterializationStrategy.HYBRID,
                snapshots.get(planksKey).policy().strategy());
        assertEquals(
                CompactRecipeFamilyProvider.MaterializationStrategy.HYBRID,
                snapshots.get(fireproofKey).policy().strategy());
        assertEquals(
                CompactRecipeFamilyProvider.MaterializationStrategy.ON_DEMAND,
                snapshots.get(planks2Key).policy().strategy());
        assertEquals(0, snapshots.get(planksKey).eagerRecipeCount());
        assertEquals(0, snapshots.get(fireproofKey).eagerRecipeCount());
        assertNotEquals(
                snapshots.get(t37Key).familyId(),
                snapshots.get(planksKey).familyId());
    }

    @Test
    void missingPublicationGroupPolicyFailsClosed() {
        RecipeMap centrifuge = ModRecipeMaps.CENTRIFUGE;
        ResourceLocation singletonGroup = CompactGTRecipeFamilyDefinition
                .T39_CENTRIFUGE_SINGLETON_PUBLICATION_GROUP;
        CompactRecipeFamilySource source = source(
                centrifuge,
                singletonGroup,
                "gt.recipe.centrifuge#0003",
                itemRelation(
                        "t39/missing_group_policy",
                        Ingredient.of(Items.IRON_INGOT),
                        new ItemStack(Items.IRON_NUGGET),
                        0));

        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> CompactRecipeFamilyProvider.prepareByPublicationGroup(
                        List.of(source),
                        Map.of(centrifuge.id(), centrifuge),
                        1L,
                        CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                        Map.of()));

        assertTrue(thrown.getMessage().contains(
                "Missing compact materialization policy"));
        assertTrue(thrown.getMessage().contains(singletonGroup.toString()));
    }

    @Test
    void centrifugeWithoutPublicationGroupFailsClosed() {
        RecipeMap centrifuge = ModRecipeMaps.CENTRIFUGE;
        CompactRecipeFamilySource source = new CompactRecipeFamilySource(
                id("authored/centrifuge_missing_group"),
                new CompactGTRecipeFamilyDefinition(
                        "gt.recipe.centrifuge#0004",
                        centrifuge.id(),
                        "3703e40308c8c030763fd6297dea8b210d2a77b1",
                        List.of(itemRelation(
                                "t39/missing_group",
                                Ingredient.of(Items.IRON_INGOT),
                                new ItemStack(Items.IRON_NUGGET),
                                0))));

        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> CompactRecipeFamilyProvider.prepareByPublicationGroup(
                        List.of(source),
                        Map.of(centrifuge.id(), centrifuge),
                        1L,
                        CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                        Map.of()));

        assertTrue(thrown.getMessage().contains("publication_group"));
        assertTrue(thrown.getMessage().contains(centrifuge.id().toString()));
    }

    @Test
    void duplicateStableIdAcrossPublicationGroupsFailsClosed() {
        RecipeMap centrifuge = ModRecipeMaps.CENTRIFUGE;
        ResourceLocation singletonGroup = CompactGTRecipeFamilyDefinition
                .T39_CENTRIFUGE_SINGLETON_PUBLICATION_GROUP;
        ResourceLocation multiGroup = CompactGTRecipeFamilyDefinition
                .T39_CENTRIFUGE_MULTI_PUBLICATION_GROUP;
        CompactRecipeFamilySource singleton = source(
                centrifuge,
                singletonGroup,
                "gt.recipe.centrifuge#0005",
                itemRelation(
                        "t39/duplicate_across_groups",
                        Ingredient.of(Items.IRON_INGOT),
                        new ItemStack(Items.IRON_NUGGET),
                        0));
        CompactRecipeFamilySource multi = source(
                centrifuge,
                multiGroup,
                "gt.recipe.centrifuge#0006",
                itemRelation(
                        "t39/duplicate_across_groups",
                        Ingredient.of(Items.GOLD_INGOT),
                        new ItemStack(Items.GOLD_NUGGET),
                        1));

        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> CompactRecipeFamilyProvider.prepareByPublicationGroup(
                        List.of(singleton, multi),
                        Map.of(centrifuge.id(), centrifuge),
                        1L,
                        CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                        Map.of(
                                new PublicationGroupKey(
                                        centrifuge.id(), singletonGroup),
                                CompactRecipeFamilyProvider
                                        .MaterializationPolicy.immediate(),
                                new PublicationGroupKey(
                                        centrifuge.id(), multiGroup),
                                CompactRecipeFamilyProvider
                                        .MaterializationPolicy.immediate())));

        assertTrue(thrown.getMessage().contains(
                "Duplicate compact stable id"));
        assertTrue(thrown.getMessage().contains("across publication groups"));
    }

    @Test
    void missingOrUnknownTargetPolicyFailsClosed() {
        RecipeMap assembler = ModRecipeMaps.ASSEMBLER;
        RecipeMap roaster = ModRecipeMaps.ROASTER;
        CompactRecipeFamilySource roasterSource = source(
                roaster,
                "gt.recipe.roaster#0002",
                itemRelation(
                        "t38/missing_policy",
                        Ingredient.of(Items.IRON_INGOT),
                        new ItemStack(Items.IRON_NUGGET),
                        0));

        IllegalArgumentException missing = assertThrows(
                IllegalArgumentException.class,
                () -> CompactRecipeFamilyProvider.prepareByTarget(
                        List.of(roasterSource),
                        Map.of(assembler.id(), assembler, roaster.id(), roaster),
                        1L,
                        CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                        Map.of(
                                assembler.id(),
                                CompactRecipeFamilyProvider.MaterializationPolicy.immediate())));
        assertTrue(missing.getMessage().contains("Missing compact materialization policy"));
        assertTrue(missing.getMessage().contains(roaster.id().toString()));

        IllegalArgumentException unknown = assertThrows(
                IllegalArgumentException.class,
                () -> CompactRecipeFamilyProvider.prepareByTarget(
                        List.of(),
                        Map.of(assembler.id(), assembler),
                        1L,
                        CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                        Map.of(
                                id("unregistered_compact_target"),
                                CompactRecipeFamilyProvider.MaterializationPolicy.immediate())));
        assertTrue(unknown.getMessage().contains("Unknown compact family policy target map"));
        assertTrue(unknown.getMessage().contains("unregistered_compact_target"));
    }

    @Test
    void t37FirstTenUsesAssemblerSourcesOnly() {
        RecipeMap assembler = ModRecipeMaps.ASSEMBLER;
        RecipeMap roaster = ModRecipeMaps.ROASTER;
        List<CompactRecipeFamilySource> assemblerSources = new ArrayList<>();
        for (int number = 1; number <= 11; number++) {
            assemblerSources.add(source(
                    assembler,
                    "gt.recipe.assembler#%04d".formatted(number),
                    itemRelation(
                            "t37/assembler_boundary_" + number,
                            Ingredient.of(Items.IRON_INGOT),
                            new ItemStack(Items.IRON_NUGGET),
                            number - 1)));
        }
        CompactRecipeFamilySource roasterSource = source(
                roaster,
                "aaa.roaster#0001",
                itemRelation(
                        "t38/roaster_boundary",
                        Ingredient.of(Items.GOLD_INGOT),
                        new ItemStack(Items.GOLD_NUGGET),
                        0));
        List<CompactRecipeFamilySource> allSources = new ArrayList<>(assemblerSources);
        allSources.add(roasterSource);

        var snapshots = CompactRecipeFamilyProvider.prepareByTarget(
                allSources,
                Map.of(assembler.id(), assembler, roaster.id(), roaster),
                1L,
                CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                Map.of(
                        assembler.id(),
                        CompactHybridFixturePolicies.t37Selector(assemblerSources),
                        roaster.id(),
                        CompactRecipeFamilyProvider.MaterializationPolicy.onDemand(16)));

        var t37 = snapshots.get(assembler.id());
        assertEquals(11, t37.logicalRecipeCount());
        assertEquals(10, t37.eagerRecipeCount());
        assertEquals(1, t37.lazyRecipeCount());
        assertEquals(8, t37.cacheCeiling());
        assertEquals(
                id("t37/assembler_boundary_11"),
                t37.recipeIds().getLast());
    }

    @Test
    void concreteRowsAndCompactFamiliesCoexist() {
        RecipeMap map = new RecipeMap(id("coexist"));
        RecipeMap.Entry concrete = new RecipeMap.Entry(
                id("concrete/cobble"),
                new GTRecipe(
                        List.of(Ingredient.of(Items.COBBLESTONE)),
                        List.of(1),
                        List.of(new ItemStack(Items.STONE)),
                        List.of(),
                        List.of(),
                        List.of(GTRecipe.GUARANTEED_CHANCE),
                        20,
                        8L,
                        0L));
        var family = prepare(
                map,
                List.of(source(
                        map,
                        "coexist.family#0001",
                        itemRelation(
                                "t37/coexist_iron",
                                Ingredient.of(Items.IRON_INGOT),
                                new ItemStack(Items.IRON_NUGGET),
                                0))),
                CompactRecipeFamilyProvider.MaterializationPolicy.immediate(),
                3L);
        map.prepareRecipes(List.of(concrete), List.of(family), 3L).publish();

        assertEquals(
                id("concrete/cobble"),
                map.findMatch(GTRecipeQuery.items(new ItemStack(Items.COBBLESTONE)))
                        .orElseThrow()
                        .id());
        assertEquals(
                id("t37/coexist_iron"),
                map.findMatch(GTRecipeQuery.items(new ItemStack(Items.IRON_INGOT)))
                        .orElseThrow()
                        .id());
        assertEquals(2, map.entries().size());
    }

    @Test
    void epochPublishIsAtomicAndStaleViewsFail() {
        RecipeMap map = new RecipeMap(id("epoch"));
        var first = prepare(
                map,
                List.of(source(
                        map,
                        "epoch.family#0001",
                        itemRelation(
                                "t37/epoch_a",
                                Ingredient.of(Items.IRON_INGOT),
                                new ItemStack(Items.IRON_NUGGET),
                                0))),
                CompactRecipeFamilyProvider.MaterializationPolicy.immediate(),
                1L);
        map.prepareRecipes(List.of(), List.of(first), 1L).publish();
        List<RecipeMap.Entry> stale = map.entries();
        assertEquals(1, stale.size());

        var second = prepare(
                map,
                List.of(source(
                        map,
                        "epoch.family#0001",
                        itemRelation(
                                "t37/epoch_b",
                                Ingredient.of(Items.GOLD_INGOT),
                                new ItemStack(Items.GOLD_NUGGET),
                                0))),
                CompactRecipeFamilyProvider.MaterializationPolicy.immediate(),
                2L);
        map.prepareRecipes(List.of(), List.of(second), 2L).publish();
        assertThrows(IllegalStateException.class, stale::size);
        assertEquals(
                id("t37/epoch_b"),
                map.entry(id("t37/epoch_b")).orElseThrow().id());
        assertFalse(map.entry(id("t37/epoch_a")).isPresent());
    }

    @Test
    void enumerationDoesNotGrowOnDemandCache() {
        RecipeMap map = new RecipeMap(id("enum_cache"));
        var snapshot = prepare(
                map,
                List.of(source(
                        map,
                        "enum.family#0001",
                        itemRelation(
                                "t37/enum_a",
                                Ingredient.of(Items.IRON_INGOT),
                                new ItemStack(Items.IRON_NUGGET),
                                0),
                        itemRelation(
                                "t37/enum_b",
                                Ingredient.of(Items.GOLD_INGOT),
                                new ItemStack(Items.GOLD_NUGGET),
                                1),
                        itemRelation(
                                "t37/enum_c",
                                Ingredient.of(Items.COPPER_INGOT),
                                new ItemStack(Items.COPPER_BLOCK),
                                2))),
                CompactRecipeFamilyProvider.MaterializationPolicy.onDemand(8));
        assertEquals(0, snapshot.eagerRecipeCount());
        assertEquals(3, snapshot.lazyRecipeCount());
        int cacheBefore = snapshot.cacheSize();
        for (int index = 0; index < snapshot.logicalRecipeCount(); index++) {
            assertEquals(
                    snapshot.recipeIds().get(index),
                    snapshot.enumerationEntry(index).id());
        }
        assertEquals(cacheBefore, snapshot.cacheSize());
        assertEquals(0, snapshot.cacheSize());
    }

    @Test
    void immediateAndOnDemandMaterializeTheSameRecipe() {
        RecipeMap map = new RecipeMap(id("strategy"));
        List<CompactRecipeFamilySource> sources = List.of(source(
                map,
                "strategy.family#0001",
                itemRelation(
                        "t37/strategy_row",
                        Ingredient.of(Items.IRON_INGOT),
                        new ItemStack(Items.IRON_NUGGET),
                        0)));
        var immediate = CompactRecipeFamilyProvider.prepare(
                map,
                sources,
                4L,
                CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                CompactRecipeFamilyProvider.MaterializationPolicy.immediate());
        var onDemand = CompactRecipeFamilyProvider.prepare(
                map,
                sources,
                4L,
                CompactRecipeFamilyProvider.RuntimeSide.DEDICATED_CLIENT,
                CompactRecipeFamilyProvider.MaterializationPolicy.onDemand(4));
        var hybrid = CompactRecipeFamilyProvider.prepare(
                map,
                sources,
                4L,
                CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                CompactRecipeFamilyProvider.MaterializationPolicy.hybrid(
                        4, (index, relation) -> index == 0));

        ResourceLocation stableId = id("t37/strategy_row");
        GTRecipe expected = immediate.entry(stableId).orElseThrow().recipe();
        assertSameRecipe(expected, onDemand.entry(stableId).orElseThrow().recipe());
        assertSameRecipe(expected, hybrid.entry(stableId).orElseThrow().recipe());
        assertEquals(immediate.recipeIds(), onDemand.recipeIds());
        assertEquals(immediate.stableFingerprint(), onDemand.stableFingerprint());
        assertEquals(immediate.stableFingerprint(), hybrid.stableFingerprint());
        assertEquals(1, immediate.eagerRecipeCount());
        assertEquals(0, onDemand.eagerRecipeCount());
        assertEquals(1, hybrid.eagerRecipeCount());
    }

    @Test
    void integratedClientRefusesToBuild() {
        RecipeMap map = new RecipeMap(id("integrated"));
        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> CompactRecipeFamilyProvider.prepare(
                        map,
                        List.of(),
                        1L,
                        CompactRecipeFamilyProvider.RuntimeSide.INTEGRATED_CLIENT,
                        CompactRecipeFamilyProvider.MaterializationPolicy.immediate()));
        assertTrue(thrown.getMessage().contains("Integrated clients"));
    }

    @Test
    void supportsActionsFluidsChanceAndUnindexedFallback() {
        RecipeMap map = new RecipeMap(id("coverage"));
        ItemStack named = new ItemStack(Items.STICK);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("catalyst"));
        CompactGTRecipeFamilyDefinition.Relation wearAndChance =
                new CompactGTRecipeFamilyDefinition.Relation(
                        id("t37/coverage_multi"),
                        List.of(
                                Ingredient.of(Items.IRON_INGOT),
                                Ingredient.of(Items.IRON_PICKAXE),
                                Ingredient.of(ItemTags.PLANKS),
                                DataComponentIngredient.of(false, named)),
                        List.of(2, 0, 1, 0),
                        List.of(
                                ItemInputAction.CONSUME,
                                ItemInputAction.wear(1),
                                ItemInputAction.CONSUME,
                                ItemInputAction.PRESERVE),
                        List.of(
                                new ItemStack(Items.IRON_NUGGET, 3),
                                new ItemStack(Items.REDSTONE)),
                        List.of(new FluidStack(Fluids.WATER, 500)),
                        List.of(new FluidStack(Fluids.LAVA, 50)),
                        List.of(GTRecipe.GUARANTEED_CHANCE, 2500),
                        64,
                        32L,
                        7L,
                        true,
                        3,
                        provenance("gt.recipe.coverage#0001"));
        var snapshot = prepare(
                map,
                List.of(source(map, "coverage.family#0001", wearAndChance)),
                CompactRecipeFamilyProvider.MaterializationPolicy.onDemand(2));

        RecipeMap.Entry byId = snapshot.entry(id("t37/coverage_multi")).orElseThrow();
        assertEquals(2, byId.recipe().itemOutputs().size());
        assertEquals(2500, byId.recipe().outputChances().get(1));
        assertEquals(ItemInputAction.Kind.WEAR, byId.recipe().itemInputActions().get(1).kind());
        assertEquals(1, snapshot.unindexedRelationCount());
        RecipeMap.Entry byIdAgain = snapshot.entry(id("t37/coverage_multi")).orElseThrow();
        assertEquals(id("t37/coverage_multi"), byIdAgain.id());
        assertEquals(0, snapshot.cacheSize());
        snapshot.findLazy(new GTRecipeQuery(
                List.of(
                        new ItemStack(Items.IRON_INGOT, 2),
                        new ItemStack(Items.IRON_PICKAXE),
                        new ItemStack(Items.OAK_PLANKS),
                        named.copy()),
                List.of(new FluidStack(Fluids.WATER, 500))));
        assertTrue(snapshot.cacheSize() <= snapshot.cacheCeiling());
    }

    @Test
    void parameterizedDefinitionIsRejected() {
        RecipeMap map = new RecipeMap(id("parameterized"));
        CompactGTRecipeFamilyDefinition definition = new CompactGTRecipeFamilyDefinition(
                "future.bath#0001",
                map.id(),
                "3703e40308c8c030763fd6297dea8b210d2a77b1",
                List.of(),
                Optional.of(new CompactGTRecipeFamilyDefinition.ParameterizedSpec(
                        "bath_template",
                        Map.of("axis", "material"))),
                Optional.of(id("test_parameterized")));
        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> CompactRecipeFamilyProvider.prepare(
                        map,
                        List.of(new CompactRecipeFamilySource(
                                id("authored/parameterized"), definition)),
                        1L,
                        CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                        CompactRecipeFamilyProvider.MaterializationPolicy.immediate()));
        assertTrue(thrown.getMessage().contains("not implemented"));
        assertTrue(thrown.getMessage().contains("bath_template"));
    }

    @Test
    void duplicateAuthoredAndStableIdsFailClosed() {
        RecipeMap map = new RecipeMap(id("duplicate_ids"));
        CompactRecipeFamilySource first = source(
                map,
                "duplicate.authored#0001",
                itemRelation(
                        "t38/duplicate_authored_first",
                        Ingredient.of(Items.IRON_INGOT),
                        new ItemStack(Items.IRON_NUGGET),
                        0));
        CompactRecipeFamilySource duplicateAuthored = new CompactRecipeFamilySource(
                first.id(),
                new CompactGTRecipeFamilyDefinition(
                        "duplicate.authored#0002",
                        map.id(),
                        "3703e40308c8c030763fd6297dea8b210d2a77b1",
                        List.of(itemRelation(
                                "t38/duplicate_authored_second",
                                Ingredient.of(Items.GOLD_INGOT),
                                new ItemStack(Items.GOLD_NUGGET),
                                1)),
                        id("test_duplicate_ids")));
        IllegalArgumentException authored = assertThrows(
                IllegalArgumentException.class,
                () -> prepare(
                        map,
                        List.of(first, duplicateAuthored),
                        CompactRecipeFamilyProvider.MaterializationPolicy.immediate()));
        assertTrue(authored.getMessage().contains("Duplicate compact authored id"));

        CompactRecipeFamilySource duplicateStable = source(
                map,
                "duplicate.stable#0002",
                itemRelation(
                        "t38/duplicate_stable",
                        Ingredient.of(Items.GOLD_INGOT),
                        new ItemStack(Items.GOLD_NUGGET),
                        1));
        CompactRecipeFamilySource originalStable = source(
                map,
                "duplicate.stable#0001",
                itemRelation(
                        "t38/duplicate_stable",
                        Ingredient.of(Items.IRON_INGOT),
                        new ItemStack(Items.IRON_NUGGET),
                        0));
        IllegalArgumentException stable = assertThrows(
                IllegalArgumentException.class,
                () -> prepare(
                        map,
                        List.of(originalStable, duplicateStable),
                        CompactRecipeFamilyProvider.MaterializationPolicy.immediate()));
        assertTrue(stable.getMessage().contains("Duplicate compact stable id"));
    }

    @Test
    void jsonContractParsesPinnedAssemblerShape() {
        String json = """
                {
                  "family_id": "gt.recipe.assembler#0002",
                  "target_map": "cruciblecraft:assembler",
                  "source_revision": "3703e40308c8c030763fd6297dea8b210d2a77b1",
                  "relations": [
                    {
                      "stable_id": "cruciblecraft:t37/contract_row",
                      "item_inputs": [{"item": "minecraft:iron_ingot"}],
                      "item_input_counts": [1],
                      "item_input_actions": [{"kind": "consume"}],
                      "item_outputs": [{"id": "minecraft:iron_nugget", "count": 1}],
                      "fluid_inputs": [],
                      "fluid_outputs": [],
                      "output_chances": [10000],
                      "duration": 32,
                      "eut": 16,
                      "special_value": 0,
                      "can_be_buffered": true,
                      "shadow_order": 0,
                      "provenance": {
                        "source_kind": "SOURCE_BACKED",
                        "selected_source_recipe": "gt.recipe.assembler#0002",
                        "evidence_hashes": ["abc"]
                      }
                    }
                  ]
                }
                """;
        var ops = RegistryOps.create(JsonOps.INSTANCE, registries);
        CompactGTRecipeFamilyDefinition decoded =
                CompactGTRecipeFamilyDefinition.CODEC
                        .parse(ops, JsonParser.parseString(json))
                        .getOrThrow();
        assertEquals("gt.recipe.assembler#0002", decoded.familyId());
        assertEquals(ModRecipeMaps.ASSEMBLER.id(), decoded.targetMap());
        assertTrue(decoded.publicationGroup().isEmpty());
        assertEquals(
                CompactGTRecipeFamilyDefinition
                        .T37_ASSEMBLER_PUBLICATION_GROUP,
                decoded.resolvedPublicationGroup());
        assertEquals(32, decoded.relations().getFirst().duration());
        assertEquals(16L, decoded.relations().getFirst().eut());
        assertEquals(
                ItemInputAction.CONSUME,
                decoded.relations().getFirst().itemInputActions().getFirst());
    }

    @Test
    void fiftySingletonRowsMaterializeUnderFrozenT37Policies() {
        int[] durations = {16, 32, 96, 48, 64, 64, 128, 64, 32};
        List<CompactRecipeFamilySource> sources = new ArrayList<>();
        RecipeMap map = new RecipeMap(id("t37_workload"));
        for (int number = 2; number <= 51; number++) {
            String familyId = "gt.recipe.assembler#%04d".formatted(number);
            int duration = durations[(number - 2) % durations.length];
            sources.add(source(
                    map,
                    familyId,
                    new CompactGTRecipeFamilyDefinition.Relation(
                            id("t37/row_" + number),
                            List.of(Ingredient.of(Items.IRON_INGOT)),
                            List.of(1),
                            List.of(ItemInputAction.CONSUME),
                            List.of(new ItemStack(Items.IRON_NUGGET)),
                            List.of(),
                            List.of(),
                            List.of(GTRecipe.GUARANTEED_CHANCE),
                            duration,
                            16L,
                            0L,
                            true,
                            number - 2,
                            provenance(familyId))));
        }
        CompactRecipeFamilyProvider.MaterializationPolicy frozenT37Policy =
                CompactHybridFixturePolicies.t37Selector(sources);

        var immediate = CompactRecipeFamilyProvider.prepare(
                map,
                sources,
                7L,
                CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                CompactRecipeFamilyProvider.MaterializationPolicy.immediate());
        var onDemand = CompactRecipeFamilyProvider.prepare(
                map,
                sources,
                7L,
                CompactRecipeFamilyProvider.RuntimeSide.DEDICATED_CLIENT,
                CompactRecipeFamilyProvider.MaterializationPolicy.onDemand(8));
        var hybrid = CompactRecipeFamilyProvider.prepare(
                map,
                sources,
                7L,
                CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                frozenT37Policy);

        assertEquals(50, immediate.logicalRecipeCount());
        assertEquals(50, immediate.eagerRecipeCount());
        assertEquals(0, immediate.lazyRecipeCount());
        assertEquals(0, immediate.cacheCeiling());
        assertEquals(0, onDemand.eagerRecipeCount());
        assertEquals(50, onDemand.lazyRecipeCount());
        assertEquals(8, onDemand.cacheCeiling());
        assertEquals(14, hybrid.eagerRecipeCount());
        assertEquals(36, hybrid.lazyRecipeCount());
        assertEquals(8, hybrid.cacheCeiling());
        assertEquals(immediate.recipeIds(), onDemand.recipeIds());
        assertEquals(immediate.stableFingerprint(), onDemand.stableFingerprint());
        assertEquals(immediate.stableFingerprint(), hybrid.stableFingerprint());

        int cacheBefore = onDemand.cacheSize();
        for (int index = 0; index < onDemand.logicalRecipeCount(); index++) {
            assertEquals(
                    onDemand.recipeIds().get(index),
                    onDemand.enumerationEntry(index).id());
        }
        assertEquals(cacheBefore, onDemand.cacheSize());
        assertTrue(onDemand.cacheSize() <= onDemand.cacheCeiling());
        Optional<RecipeMap.Entry> lookedUp = onDemand.findLazy(
                GTRecipeQuery.items(new ItemStack(Items.IRON_INGOT)));
        assertTrue(lookedUp.isPresent());
        assertTrue(onDemand.cacheSize() <= onDemand.cacheCeiling());
        assertTrue(hybrid.syncPayloadBytes() > 0L);
        assertEquals(
                CompactRecipeFamilyProvider.RuntimeSide.DEDICATED_CLIENT,
                onDemand.side());
        IllegalArgumentException integrated = assertThrows(
                IllegalArgumentException.class,
                () -> CompactRecipeFamilyProvider.prepare(
                        map,
                        sources,
                        7L,
                        CompactRecipeFamilyProvider.RuntimeSide.INTEGRATED_CLIENT,
                        CompactRecipeFamilyProvider.MaterializationPolicy.immediate()));
        assertTrue(integrated.getMessage().contains("Integrated clients"));
    }

    private static CompactRecipeFamilyProvider.Snapshot prepare(
            RecipeMap map,
            List<CompactRecipeFamilySource> sources,
            CompactRecipeFamilyProvider.MaterializationPolicy policy) {
        return prepare(map, sources, policy, 1L);
    }

    private static CompactRecipeFamilyProvider.Snapshot prepare(
            RecipeMap map,
            List<CompactRecipeFamilySource> sources,
            CompactRecipeFamilyProvider.MaterializationPolicy policy,
            long epoch) {
        return CompactRecipeFamilyProvider.prepare(
                map,
                sources,
                epoch,
                CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                policy);
    }

    private static CompactRecipeFamilySource source(
            RecipeMap map,
            String familyId,
            CompactGTRecipeFamilyDefinition.Relation... relations) {
        CompactGTRecipeFamilyDefinition definition;
        if (map.id().equals(ModRecipeMaps.ASSEMBLER.id())
                || map.id().equals(ModRecipeMaps.ROASTER.id())) {
            definition = new CompactGTRecipeFamilyDefinition(
                    familyId,
                    map.id(),
                    "3703e40308c8c030763fd6297dea8b210d2a77b1",
                    List.of(relations));
        } else {
            definition = new CompactGTRecipeFamilyDefinition(
                    familyId,
                    map.id(),
                    "3703e40308c8c030763fd6297dea8b210d2a77b1",
                    List.of(relations),
                    id("test_" + map.id().getPath().replace('/', '_')));
        }
        return new CompactRecipeFamilySource(
                id("authored/" + familyId.replace('#', '_')),
                definition);
    }

    private static CompactRecipeFamilySource source(
            RecipeMap map,
            ResourceLocation publicationGroup,
            String familyId,
            CompactGTRecipeFamilyDefinition.Relation... relations) {
        return new CompactRecipeFamilySource(
                id("authored/" + familyId.replace('#', '_')),
                new CompactGTRecipeFamilyDefinition(
                        familyId,
                        map.id(),
                        "3703e40308c8c030763fd6297dea8b210d2a77b1",
                        List.of(relations),
                        publicationGroup));
    }

    private static CompactGTRecipeFamilyDefinition assemblerFamily(
            String familyId,
            List<CompactGTRecipeFamilyDefinition.Relation> relations) {
        return new CompactGTRecipeFamilyDefinition(
                familyId,
                ModRecipeMaps.ASSEMBLER.id(),
                "3703e40308c8c030763fd6297dea8b210d2a77b1",
                relations);
    }

    private static CompactGTRecipeFamilyDefinition.Relation itemRelation(
            String path,
            Ingredient input,
            ItemStack output,
            int shadowOrder) {
        return new CompactGTRecipeFamilyDefinition.Relation(
                id(path),
                List.of(input),
                List.of(1),
                List.of(ItemInputAction.CONSUME),
                List.of(output),
                List.of(),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                32,
                16L,
                0L,
                true,
                shadowOrder,
                provenance("gt.recipe.assembler#0002"));
    }

    private static CompactGTRecipeFamilyDefinition.Relation fluidRelation(
            String path,
            FluidStack input,
            ItemStack output,
            int shadowOrder) {
        return new CompactGTRecipeFamilyDefinition.Relation(
                id(path),
                List.of(),
                List.of(),
                List.of(),
                List.of(output),
                List.of(input),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                20,
                8L,
                0L,
                true,
                shadowOrder,
                provenance("synthetic.family#0001"));
    }

    private static GTRecipeProvenance provenance(String selected) {
        return new GTRecipeProvenance(
                "SOURCE_BACKED",
                Optional.of(selected),
                List.of("abc"));
    }

    private static void assertSameRecipe(GTRecipe expected, GTRecipe actual) {
        assertEquals(expected.itemInputCounts(), actual.itemInputCounts());
        assertEquals(expected.itemInputActions(), actual.itemInputActions());
        assertEquals(expected.outputChances(), actual.outputChances());
        assertEquals(expected.duration(), actual.duration());
        assertEquals(expected.eut(), actual.eut());
        assertEquals(expected.specialValue(), actual.specialValue());
        assertEquals(expected.canBeBuffered(), actual.canBeBuffered());
        assertEquals(expected.provenance(), actual.provenance());
        assertEquals(expected.itemInputs().size(), actual.itemInputs().size());
        assertEquals(expected.itemOutputs().size(), actual.itemOutputs().size());
        for (int index = 0; index < expected.itemOutputs().size(); index++) {
            ItemStack left = expected.itemOutputs().get(index);
            ItemStack right = actual.itemOutputs().get(index);
            assertEquals(left.getItem(), right.getItem());
            assertEquals(left.getCount(), right.getCount());
        }
        assertEquals(expected.fluidInputs().size(), actual.fluidInputs().size());
        assertEquals(expected.fluidOutputs().size(), actual.fluidOutputs().size());
        for (int index = 0; index < expected.fluidInputs().size(); index++) {
            assertEquals(
                    expected.fluidInputs().get(index).getFluid(),
                    actual.fluidInputs().get(index).getFluid());
            assertEquals(
                    expected.fluidInputs().get(index).getAmount(),
                    actual.fluidInputs().get(index).getAmount());
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
