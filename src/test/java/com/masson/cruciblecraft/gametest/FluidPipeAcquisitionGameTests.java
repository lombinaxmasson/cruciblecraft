package com.masson.cruciblecraft.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.logistics.pipe.PipeAcquisitionRecipeCatalog;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated GT6 fluid-pipe acquisition. Run with
 * {@code -PwaveRecipes=content/gt6-fluid-pipe-acquisition}.
 */
@GameTestHolder(FluidPipeAcquisitionGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class FluidPipeAcquisitionGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_content_gt6_fluid_pipe_acquisition";
    private static final String TEMPLATE = "empty";

    private FluidPipeAcquisitionGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void comboQuadrupleCraftsFromMedium(GameTestHelper helper) {
        Item medium = ModItems.materialItem(
                "copper", MaterialPrefixes.FLUID_PIPE).get();
        Item quad = ModItems.materialItem(
                "copper", MaterialPrefixes.QUADRUPLE_FLUID_PIPE).get();
        ItemStack assembled = craft(
                helper,
                2,
                2,
                List.of(
                        new ItemStack(medium),
                        new ItemStack(medium),
                        new ItemStack(medium),
                        new ItemStack(medium)));
        helper.assertTrue(
                assembled.is(quad) && assembled.getCount() == 1,
                "copper quadruple did not craft from four medium pipes: "
                        + assembled);
        helper.assertTrue(
                helper.getLevel().getRecipeManager()
                        .byKey(id("pipe/combo/copper/quadruple_fluid_pipe"))
                        .isPresent(),
                "missing copper quadruple pack recipe");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void comboUnpackReturnsMedium(GameTestHelper helper) {
        Item medium = ModItems.materialItem(
                "copper", MaterialPrefixes.FLUID_PIPE).get();
        Item quad = ModItems.materialItem(
                "copper", MaterialPrefixes.QUADRUPLE_FLUID_PIPE).get();
        ItemStack assembled = craft(
                helper,
                1,
                1,
                List.of(new ItemStack(quad)));
        helper.assertTrue(
                assembled.is(medium) && assembled.getCount() == 4,
                "copper quadruple did not unpack to 4 medium: " + assembled);
        helper.assertTrue(
                helper.getLevel().getRecipeManager()
                        .byKey(id("pipe/combo/copper/unpack_quadruple_fluid_pipe"))
                        .isPresent(),
                "missing copper quadruple unpack recipe");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void copperHugeTableCraftsFromDoublePlate(
            GameTestHelper helper) {
        Item plate = ModItems.materialItem(
                "copper", MaterialPrefixes.DOUBLE_PLATE).get();
        Item huge = ModItems.materialItem(
                "copper", MaterialPrefixes.HUGE_FLUID_PIPE).get();
        ItemStack assembled = craft(
                helper,
                3,
                3,
                List.of(
                        new ItemStack(plate),
                        new ItemStack(plate),
                        new ItemStack(plate),
                        new ItemStack(ModItems.MATERIAL_WRENCH.get()),
                        new ItemStack(ModItems.MATERIAL_FILE.get()),
                        new ItemStack(ModItems.SMITHING_HAMMER.get()),
                        new ItemStack(plate),
                        new ItemStack(plate),
                        new ItemStack(plate)));
        helper.assertTrue(
                assembled.is(huge) && assembled.getCount() == 1,
                "copper huge did not craft from double plates: " + assembled);
        helper.assertTrue(
                helper.getLevel().getRecipeManager()
                        .byKey(id("pipe/table/copper/huge_fluid_pipe"))
                        .isPresent(),
                "missing copper huge table recipe");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void copperTinyTableCraftsFromCurvedPlate(
            GameTestHelper helper) {
        Item curved = ModItems.materialItem(
                "copper", MaterialPrefixes.CURVED_PLATE).get();
        Item tiny = ModItems.materialItem(
                "copper", MaterialPrefixes.TINY_FLUID_PIPE).get();
        ItemStack assembled = craft(
                helper,
                3,
                2,
                List.of(
                        new ItemStack(ModItems.MATERIAL_SAW.get()),
                        new ItemStack(curved),
                        ItemStack.EMPTY,
                        new ItemStack(ModItems.MATERIAL_WRENCH.get()),
                        new ItemStack(ModItems.MATERIAL_FILE.get()),
                        new ItemStack(ModItems.SMITHING_HAMMER.get())));
        helper.assertTrue(
                assembled.is(tiny) && assembled.getCount() == 1,
                "copper tiny did not craft from curved plate: " + assembled);
        helper.assertTrue(
                helper.getLevel().getRecipeManager()
                        .byKey(id("pipe/table/copper/tiny_fluid_pipe"))
                        .isPresent(),
                "missing copper tiny table recipe");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fiveGaugeTableDoesNotUseFlatPlate(
            GameTestHelper helper) {
        Item plate = MaterialLookup.item(
                "copper", MaterialPrefixes.PLATE).orElseThrow();
        ItemStack plateStack = new ItemStack(plate);
        var access = helper.getLevel().registryAccess();
        for (RecipeHolder<?> holder : helper.getLevel()
                .getRecipeManager()
                .getAllRecipesFor(RecipeType.CRAFTING)) {
            ItemStack result = holder.value().getResultItem(access);
            if (!isCopperFiveGauge(result)) {
                continue;
            }
            for (Ingredient ingredient : holder.value().getIngredients()) {
                helper.assertFalse(
                        ingredient.test(plateStack),
                        "copper five-gauge craft used plate as a curved_plate "
                                + "stand-in: "
                                + holder.id());
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void nonmetalCatalogStaysTwentyFive(GameTestHelper helper) {
        Set<ResourceLocation> expectedIds =
                PipeAcquisitionRecipeCatalog.ALL.stream()
                        .map(PipeAcquisitionRecipeCatalog.RecipeSpec::id)
                        .collect(Collectors.toSet());
        Set<ResourceLocation> publishedIds = helper.getLevel()
                .getRecipeManager()
                .getAllRecipesFor(RecipeType.CRAFTING)
                .stream()
                .map(RecipeHolder::id)
                .filter(expectedIds::contains)
                .collect(Collectors.toSet());
        helper.assertTrue(
                publishedIds.equals(expectedIds) && publishedIds.size() == 25,
                "nonmetal pipe catalog drifted from 25 recipes");
        helper.succeed();
    }

    private static boolean isCopperFiveGauge(ItemStack result) {
        return result.is(ModItems.materialItem(
                        "copper", MaterialPrefixes.TINY_FLUID_PIPE).get())
                || result.is(ModItems.materialItem(
                        "copper", MaterialPrefixes.SMALL_FLUID_PIPE).get())
                || result.is(ModItems.materialItem(
                        "copper", MaterialPrefixes.FLUID_PIPE).get())
                || result.is(ModItems.materialItem(
                        "copper", MaterialPrefixes.LARGE_FLUID_PIPE).get())
                || result.is(ModItems.materialItem(
                        "copper", MaterialPrefixes.HUGE_FLUID_PIPE).get());
    }

    private static ItemStack craft(
            GameTestHelper helper,
            int width,
            int height,
            List<ItemStack> slots) {
        List<ItemStack> copy = new ArrayList<>(slots);
        CraftingInput input = CraftingInput.of(width, height, copy);
        return helper.getLevel()
                .getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .map(holder -> holder.value().assemble(
                        input, helper.getLevel().registryAccess()))
                .orElse(ItemStack.EMPTY);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
