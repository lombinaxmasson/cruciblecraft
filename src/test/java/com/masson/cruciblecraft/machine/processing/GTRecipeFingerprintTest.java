package com.masson.cruciblecraft.machine.processing;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.GTRecipeProvenance;
import com.mojang.serialization.JsonOps;

import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class GTRecipeFingerprintTest {
    private static RegistryAccess registries;

    @BeforeAll
    static void bootstrap() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        bindComponentIngredientType();
        registries = new RegistryAccess.ImmutableRegistryAccess(
                BuiltInRegistries.REGISTRY.stream().toList());
    }

    @Test
    void componentIngredientPredicateChangesFingerprintForSameItemId() {
        ItemStack first = namedStone("first");
        ItemStack second = namedStone("second");
        GTRecipe firstRecipe = recipe(DataComponentIngredient.of(true, first), plainOutput());
        GTRecipe secondRecipe = recipe(DataComponentIngredient.of(true, second), plainOutput());

        assertNotEquals(fingerprint(firstRecipe), fingerprint(secondRecipe));
    }

    @Test
    void outputComponentsAndMapIdChangeFingerprint() {
        ItemStack plain = plainOutput();
        ItemStack named = plainOutput();
        named.set(DataComponents.CUSTOM_NAME, Component.literal("selected output"));
        GTRecipe plainRecipe = recipe(Ingredient.of(Items.STONE), plain);
        GTRecipe namedRecipe = recipe(Ingredient.of(Items.STONE), named);

        assertNotEquals(fingerprint(plainRecipe), fingerprint(namedRecipe));
        String otherMap = GTRecipeFingerprint.recipe(
                ResourceLocation.fromNamespaceAndPath("test", "other"),
                plainRecipe,
                registries).orElseThrow();
        assertNotEquals(fingerprint(plainRecipe), otherMap);
    }

    @Test
    void provenanceRoundTripsThroughTheRuntimeCodec() {
        GTRecipe original = recipeWithProvenance("gt6_evidence");
        var ops = RegistryOps.create(JsonOps.INSTANCE, registries);
        var encoded = GTRecipe.CODEC.encodeStart(ops, original).getOrThrow();

        assertTrue(encoded.getAsJsonObject().has("provenance"));
        GTRecipe decoded = GTRecipe.CODEC.parse(ops, encoded).getOrThrow();
        assertEquals(original.provenance(), decoded.provenance());
    }

    @Test
    void provenanceDoesNotChangeTheSemanticFingerprint() {
        GTRecipe gt6 = recipeWithProvenance("gt6_evidence");
        GTRecipe topology = recipeWithProvenance("topology_fallback");

        assertEquals(fingerprint(gt6), fingerprint(topology));
    }

    private static String fingerprint(GTRecipe recipe) {
        var result = GTRecipeFingerprint.recipe(
                ResourceLocation.fromNamespaceAndPath("test", "machine"),
                recipe,
                registries);
        assertTrue(result.isPresent(), "full registry-aware recipe encoding must succeed");
        return result.orElseThrow();
    }

    private static GTRecipe recipe(Ingredient input, ItemStack output) {
        return new GTRecipe(
                List.of(input),
                List.of(1),
                List.of(output),
                List.of(),
                List.of(),
                List.of(5_000),
                20,
                16,
                3,
                false);
    }

    private static GTRecipe recipeWithProvenance(String sourceKind) {
        return new GTRecipe(
                List.of(Ingredient.of(Items.STONE)),
                List.of(1),
                List.of(plainOutput()),
                List.of(),
                List.of(),
                List.of(5_000),
                20,
                16,
                3,
                false,
                Optional.of(new GTRecipeProvenance(
                        sourceKind,
                        Optional.of("source-hash"),
                        List.of("evidence-hash"))));
    }

    private static ItemStack namedStone(String name) {
        ItemStack stack = new ItemStack(Items.STONE);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    private static ItemStack plainOutput() {
        return new ItemStack(Items.IRON_INGOT, 2);
    }

    private static void bindComponentIngredientType() {
        try {
            if (!NeoForgeRegistries.INGREDIENT_TYPES.containsKey(
                    NeoForgeMod.DATA_COMPONENT_INGREDIENT_TYPE.getId())) {
                Registry.register(
                        NeoForgeRegistries.INGREDIENT_TYPES,
                        NeoForgeMod.DATA_COMPONENT_INGREDIENT_TYPE.getId(),
                        new IngredientType<>(DataComponentIngredient.CODEC));
            }
            var holder = DeferredHolder.class.getDeclaredField("holder");
            holder.setAccessible(true);
            holder.set(
                    NeoForgeMod.DATA_COMPONENT_INGREDIENT_TYPE,
                    NeoForgeRegistries.INGREDIENT_TYPES.getHolderOrThrow(
                            NeoForgeMod.DATA_COMPONENT_INGREDIENT_TYPE.getKey()));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Unable to install test ingredient type", exception);
        }
    }
}
