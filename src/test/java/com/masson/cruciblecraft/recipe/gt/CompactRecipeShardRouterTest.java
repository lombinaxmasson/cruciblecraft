package com.masson.cruciblecraft.recipe.gt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.registry.ModRecipeMaps;
import com.masson.cruciblecraft.test.MinecraftTestBootstrap;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompactRecipeShardRouterTest {
    private static ResourceLocation group;

    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.bootstrap();
        group = CompactPublicationGroups.CENTRIFUGE_SINGLETON;
    }

    @Test
    void rareItemsBeatSharedFluidAndRouteSelectively() {
        CompactGTRecipeFamilyDefinition.Relation iron = fluidItemRelation(
                "centrifuge/compact/router_iron", Items.IRON_INGOT, 0);
        CompactGTRecipeFamilyDefinition.Relation gold = fluidItemRelation(
                "centrifuge/compact/router_gold", Items.GOLD_INGOT, 1);
        CompactRecipeShardRouter router = router(List.of(iron, gold));

        String ironShard = router.shardId(iron.stableId()).orElseThrow();
        String goldShard = router.shardId(gold.stableId()).orElseThrow();
        assertNotEquals(ironShard, goldShard);
        assertEquals(
                "item:" + BuiltInRegistries.ITEM.getKey(Items.IRON_INGOT),
                router.routeKey(iron.stableId()).orElseThrow());
        assertEquals(
                "item:" + BuiltInRegistries.ITEM.getKey(Items.GOLD_INGOT),
                router.routeKey(gold.stableId()).orElseThrow());

        GTRecipeQuery rareItemOnly =
                GTRecipeQuery.items(new ItemStack(Items.IRON_INGOT));
        assertTrue(router.routedShardIds(rareItemOnly).contains(ironShard));
        assertFalse(router.routedShardIds(rareItemOnly).contains(goldShard));
        assertEquals(
                List.of(iron.stableId()),
                router.routedCandidates(rareItemOnly).stream()
                        .map(CompactGTRecipeFamilyDefinition.Relation::stableId)
                        .toList());
        assertEquals(1, router.indexedCandidateCount(rareItemOnly));
    }

    @Test
    void overflowIsExplicitAndSelectiveLookupSkipsOtherShards() {
        CompactGTRecipeFamilyDefinition.Relation iron = fluidItemRelation(
                "centrifuge/compact/selective_iron", Items.IRON_INGOT, 0);
        CompactGTRecipeFamilyDefinition.Relation gold = fluidItemRelation(
                "centrifuge/compact/selective_gold", Items.GOLD_INGOT, 1);
        CompactGTRecipeFamilyDefinition.Relation overflow = relation(
                "centrifuge/compact/selective_overflow",
                Ingredient.of(ItemTags.PLANKS),
                List.of(),
                2);
        List<CompactGTRecipeFamilyDefinition.Relation> relations =
                List.of(iron, gold, overflow);
        CompactRecipeShardRouter router = router(relations);

        assertEquals(1, router.overflowCount());
        assertEquals(
                router.overflowShardId(),
                router.shardId(overflow.stableId()).orElseThrow());
        assertEquals(
                List.of(overflow.stableId()),
                router.shardMembers(router.overflowShardId()));

        GTRecipeQuery ironQuery = new GTRecipeQuery(
                List.of(new ItemStack(Items.IRON_INGOT)),
                List.of(new FluidStack(Fluids.WATER, 250)));
        String goldShard = router.shardId(gold.stableId()).orElseThrow();
        assertFalse(router.routedShardIds(ironQuery).contains(goldShard));
        assertEquals(2, router.indexedCandidateCount(ironQuery));
        assertTrue(router.indexedCandidateCount(ironQuery) < relations.size());

        CompactRecipeFamilySource source = new CompactRecipeFamilySource(
                id("authored/router_selective"),
                new CompactGTRecipeFamilyDefinition(
                        "gt.recipe.centrifuge#router_selective",
                        ModRecipeMaps.CENTRIFUGE.id(),
                        "t39-router-test",
                        relations,
                        group));
        CompactRecipeFamilyProvider.Snapshot snapshot =
                CompactRecipeFamilyProvider.prepare(
                        ModRecipeMaps.CENTRIFUGE,
                        List.of(source),
                        39L,
                        CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                        CompactRecipeFamilyProvider.MaterializationPolicy
                                .onDemand(4));
        assertEquals(2, snapshot.indexedLazyCandidateCount(ironQuery));
        assertEquals(
                iron.stableId(),
                snapshot.findLazy(ironQuery).orElseThrow().id());
        assertFalse(snapshot.routedShardIds(ironQuery).contains(goldShard));
    }

    @Test
    void overflowAboveHardCeilingFailsClosed() {
        List<CompactGTRecipeFamilyDefinition.Relation> relations =
                new ArrayList<>();
        for (int index = 0;
                index <= CompactRecipeShardRouter.HARD_SHARD_CEILING;
                index++) {
            relations.add(relation(
                    "centrifuge/compact/overflow_" + index,
                    Ingredient.of(ItemTags.PLANKS),
                    List.of(),
                    index));
        }

        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> router(relations));
        assertTrue(thrown.getMessage().contains("hard ceiling"));
        assertTrue(thrown.getMessage().contains("129"));
    }

    private static CompactRecipeShardRouter router(
            List<CompactGTRecipeFamilyDefinition.Relation> relations) {
        return new CompactRecipeShardRouter(
                ModRecipeMaps.CENTRIFUGE.id(), group, relations);
    }

    private static CompactGTRecipeFamilyDefinition.Relation fluidItemRelation(
            String path,
            net.minecraft.world.item.Item item,
            int shadowOrder) {
        return relation(
                path,
                Ingredient.of(item),
                List.of(new FluidStack(Fluids.WATER, 250)),
                shadowOrder);
    }

    private static CompactGTRecipeFamilyDefinition.Relation relation(
            String path,
            Ingredient ingredient,
            List<FluidStack> fluids,
            int shadowOrder) {
        return new CompactGTRecipeFamilyDefinition.Relation(
                id(path),
                List.of(ingredient),
                List.of(1),
                List.of(ItemInputAction.CONSUME),
                List.of(new ItemStack(Items.IRON_NUGGET)),
                fluids,
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                20,
                8L,
                0L,
                true,
                shadowOrder,
                new GTRecipeProvenance(
                        "SOURCE_BACKED",
                        Optional.of("gt.recipe.centrifuge#router"),
                        List.of("router-test")));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
