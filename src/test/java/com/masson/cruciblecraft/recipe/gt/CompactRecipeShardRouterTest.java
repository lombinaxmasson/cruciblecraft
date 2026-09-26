package com.masson.cruciblecraft.recipe.gt;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.content.block.GtBrokenOreBlock;
import com.masson.cruciblecraft.content.block.OreStoneHost;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.masson.cruciblecraft.registry.ModComponents;
import com.masson.cruciblecraft.registry.ModRecipeMaps;
import com.masson.cruciblecraft.test.MinecraftTestBootstrap;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.neoforged.neoforge.fluids.FluidStack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompactRecipeShardRouterTest {
    private static ResourceLocation group;

    @BeforeAll
    static void bootstrapMinecraft(@TempDir Path configDirectory) {
        MinecraftTestBootstrap.bootstrap();
        CompactGTRecipeFamilyGeneratedSupport.installPrefixMaterialRouting();
        if (!MaterialPrefixCatalog.isBootstrapped()) {
            MaterialPrefixCatalog.bootstrap(null);
        }
        if (!MaterialCatalog.isBootstrapped()) {
            MaterialCatalog.bootstrap(configDirectory);
        }
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
                        "centrifuge-router-test",
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


    @Test
    void slashIdRewriteUsesTightPrefixMaterialAndDoesNotOverflow() {
        ResourceLocation slashId = ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft", "iron/crushed_ore");
        Ingredient rewritten = PrefixMaterialItemCodecs.rewriteIngredient(slashId)
                .orElseThrow();
        ComponentIngredientIndex.Extraction extraction =
                ComponentIngredientIndex.extract(rewritten);
        assertTrue(extraction.supported());
        assertEquals(1, extraction.keys().size());
        ComponentIngredientIndex.Key key = extraction.keys().get(0);
        assertEquals(
                ResourceLocation.fromNamespaceAndPath("cruciblecraft", "crushed_ore"),
                BuiltInRegistries.ITEM.getKey(key.item()));
        assertEquals(ModComponents.PREFIX_MATERIAL.getId(), key.componentId());
        assertEquals("iron", key.value());

        Item crushed = BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("cruciblecraft", "crushed_ore"));
        ItemStack fatStack = new ItemStack(crushed);
        fatStack.set(ModComponents.PREFIX_MATERIAL, "iron");
        fatStack.set(DataComponents.CUSTOM_NAME, Component.literal("fat-default-patch"));
        Ingredient fat = DataComponentIngredient.of(false, fatStack);
        assertFalse(ComponentIngredientIndex.extract(fat).supported());
        Ingredient tightenedLive = PrefixMaterialItemCodecs.tightenLiveIngredient(fat);
        assertTrue(ComponentIngredientIndex.extract(tightenedLive).supported());

        CompactGTRecipeFamilyDefinition.Relation tight = relation(
                "centrifuge/compact/prefix_tight",
                rewritten,
                List.of(new FluidStack(Fluids.WATER, 250)),
                0);
        CompactRecipeShardRouter tightRouter = router(List.of(tight));
        assertEquals(0, tightRouter.overflowCount());
        assertEquals(
                "component:cruciblecraft:crushed_ore/"
                        + ModComponents.PREFIX_MATERIAL.getId()
                        + "=iron",
                tightRouter.routeKey(tight.stableId()).orElseThrow());

        JsonObject slashJson = new JsonObject();
        slashJson.addProperty("item", slashId.toString());
        Ingredient decoded = CompactRelationItemCodecs.INGREDIENT.parse(
                JsonOps.INSTANCE, slashJson).getOrThrow();
        assertTrue(ComponentIngredientIndex.extract(decoded).supported());
        CompactGTRecipeFamilyDefinition.Relation decodedRelation = relation(
                "centrifuge/compact/prefix_decoded",
                decoded,
                List.of(new FluidStack(Fluids.WATER, 250)),
                1);
        CompactRecipeShardRouter decodedRouter = router(List.of(decodedRelation));
        assertEquals(0, decodedRouter.overflowCount());
        assertEquals(
                tightRouter.routeKey(tight.stableId()).orElseThrow(),
                decodedRouter.routeKey(decodedRelation.stableId()).orElseThrow());
    }

    @Test
    void fatPrefixMaterialJsonTightensOnDecodeAndDoesNotOverflow() {
        JsonObject fatJson = new JsonObject();
        fatJson.addProperty("type", "neoforge:components");
        fatJson.addProperty("items", "cruciblecraft:crushed_ore");
        JsonObject components = new JsonObject();
        components.addProperty("cruciblecraft:prefix_material", "iron");
        components.addProperty("minecraft:max_stack_size", 64);
        fatJson.add("components", components);
        Ingredient decoded = PrefixMaterialItemCodecs.INGREDIENT.parse(
                JsonOps.INSTANCE, fatJson).getOrThrow();
        ComponentIngredientIndex.Extraction extraction =
                ComponentIngredientIndex.extract(decoded);
        assertTrue(extraction.supported());
        assertEquals(1, extraction.keys().size());
        assertEquals("iron", extraction.keys().get(0).value());
        CompactGTRecipeFamilyDefinition.Relation relation = relation(
                "centrifuge/compact/prefix_fat_json",
                decoded,
                List.of(new FluidStack(Fluids.WATER, 250)),
                0);
        CompactRecipeShardRouter router = router(List.of(relation));
        assertEquals(0, router.overflowCount());
    }

    @Test
    void prefixMaterialIngredientIgnoresUnrelatedStackComponents() {
        Item crushed = BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("cruciblecraft", "crushed_ore"));
        Ingredient tight = MaterialLookup.prefixMaterialIngredient(crushed, "iron");
        ItemStack offered = new ItemStack(crushed);
        offered.set(ModComponents.PREFIX_MATERIAL, "iron");
        offered.set(DataComponents.CUSTOM_NAME, Component.literal("named"));
        assertTrue(tight.test(offered));
        ItemStack other = new ItemStack(crushed);
        other.set(ModComponents.PREFIX_MATERIAL, "copper");
        assertFalse(tight.test(other));
    }

    @Test
    void fatPrefixMaterialLiveIngredientDoesNotOverflowInRouter() {
        Item crushed = BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("cruciblecraft", "crushed_ore"));
        ItemStack fatStack = new ItemStack(crushed);
        fatStack.set(ModComponents.PREFIX_MATERIAL, "iron");
        fatStack.set(DataComponents.CUSTOM_NAME, Component.literal("fat-default-patch"));
        Ingredient fat = DataComponentIngredient.of(false, fatStack);
        assertFalse(ComponentIngredientIndex.extract(fat).supported());
        CompactGTRecipeFamilyDefinition.Relation relation = relation(
                "centrifuge/compact/prefix_fat_live",
                fat,
                List.of(new FluidStack(Fluids.WATER, 250)),
                0);
        CompactRecipeShardRouter router = router(List.of(relation));
        assertEquals(0, router.overflowCount());
        assertEquals(
                "component:cruciblecraft:crushed_ore/"
                        + ModComponents.PREFIX_MATERIAL.getId()
                        + "=iron",
                router.routeKey(relation.stableId()).orElseThrow());
    }

    @Test
    void publicExchangeSlashDecodesToUniqueItemNotTag() {
        ResourceLocation dustId = ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft", "iron/dust");
        JsonObject json = new JsonObject();
        json.addProperty("item", dustId.toString());
        Ingredient decoded = CompactRelationItemCodecs.INGREDIENT.parse(
                JsonOps.INSTANCE, json).getOrThrow();
        assertTrue(decoded.isSimple());
        assertEquals(1, decoded.getItems().length);
        assertFalse(java.util.Arrays.stream(decoded.getValues())
                .anyMatch(value -> value instanceof Ingredient.TagValue));
        assertEquals(dustId, BuiltInRegistries.ITEM.getKey(decoded.getItems()[0].getItem()));
        CompactGTRecipeFamilyDefinition.Relation relation = relation(
                "centrifuge/compact/unique_dust",
                decoded,
                List.of(),
                0);
        CompactRecipeShardRouter router = router(List.of(relation));
        assertEquals(0, router.overflowCount());
        assertEquals("item:" + dustId, router.routeKey(relation.stableId()).orElseThrow());
    }

    @Test
    void leftoverPublicExchangeComponentRewritesToUniqueItem() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "neoforge:components");
        json.addProperty("items", "cruciblecraft:dust");
        JsonObject components = new JsonObject();
        components.addProperty("cruciblecraft:prefix_material", "iron");
        json.add("components", components);
        Ingredient decoded = CompactRelationItemCodecs.INGREDIENT.parse(
                JsonOps.INSTANCE, json).getOrThrow();
        assertTrue(decoded.isSimple());
        assertEquals(1, decoded.getItems().length);
        assertFalse(java.util.Arrays.stream(decoded.getValues())
                .anyMatch(value -> value instanceof Ingredient.TagValue));
        assertEquals(
                ResourceLocation.fromNamespaceAndPath("cruciblecraft", "iron/dust"),
                BuiltInRegistries.ITEM.getKey(decoded.getItems()[0].getItem()));
        CompactGTRecipeFamilyDefinition.Relation relation = relation(
                "centrifuge/compact/leftover_dust_component",
                decoded,
                List.of(new FluidStack(Fluids.WATER, 250)),
                0);
        CompactRecipeShardRouter router = router(List.of(relation));
        assertEquals(0, router.overflowCount());
    }

    @Test
    void publicExchangeTagStaysTagAndDoesNotCollapseOntoUniqueItem() {
        JsonObject tagJson = new JsonObject();
        tagJson.addProperty("tag", "c:dusts/iron");
        Ingredient tagged = PrefixMaterialItemCodecs.INGREDIENT.parse(
                JsonOps.INSTANCE, tagJson).getOrThrow();
        assertTrue(java.util.Arrays.stream(tagged.getValues())
                .anyMatch(value -> value instanceof Ingredient.TagValue));

        JsonObject itemJson = new JsonObject();
        itemJson.addProperty("item", "cruciblecraft:iron/dust");
        Ingredient unique = CompactRelationItemCodecs.INGREDIENT.parse(
                JsonOps.INSTANCE, itemJson).getOrThrow();
        assertFalse(java.util.Arrays.stream(unique.getValues())
                .anyMatch(value -> value instanceof Ingredient.TagValue));

        CompactGTRecipeFamilyDefinition.Relation taggedRelation = relation(
                "centrifuge/compact/tag_dust",
                tagged,
                List.of(),
                0);
        CompactGTRecipeFamilyDefinition.Relation uniqueRelation = relation(
                "centrifuge/compact/unique_dust_shadow",
                unique,
                List.of(),
                1);
        CompactRecipeShardRouter router = router(List.of(taggedRelation, uniqueRelation));
        assertEquals(1, router.overflowCount());
        assertEquals(
                router.overflowShardId(),
                router.shardId(taggedRelation.stableId()).orElseThrow());
        assertEquals(
                "item:cruciblecraft:iron/dust",
                router.routeKey(uniqueRelation.stableId()).orElseThrow());
    }

    private static CompactRecipeShardRouter router(
            List<CompactGTRecipeFamilyDefinition.Relation> relations) {
        return new CompactRecipeShardRouter(
                ModRecipeMaps.CENTRIFUGE.id(), group, relations);
    }

    @Test
    void oreHostBlockStateEncodesTheHostProperty() {
        BlockItemStateProperties state = BlockItemStateProperties.EMPTY.with(
                GtBrokenOreBlock.HOST, OreStoneHost.ANDESITE);
        JsonElement encoded = BlockItemStateProperties.CODEC
                .encodeStart(JsonOps.INSTANCE, state)
                .getOrThrow();
        JsonObject expected = new JsonObject();
        expected.addProperty("host", "andesite");
        assertEquals(expected, encoded);
        assertEquals(
                "andesite",
                BlockItemStateProperties.CODEC
                        .parse(JsonOps.INSTANCE, expected)
                        .getOrThrow()
                        .properties()
                        .get("host"));
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
                        Optional.of("gt.recipe.centrifuge#router")));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
