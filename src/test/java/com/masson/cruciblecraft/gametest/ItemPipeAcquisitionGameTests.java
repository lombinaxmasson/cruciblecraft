package com.masson.cruciblecraft.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.logistics.pipe.PipeAcquisitionRecipeCatalog;
import com.masson.cruciblecraft.recipe.crafting.ShapedCatalystRecipe;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated GT6 item-pipe acquisition. Run with
 * {@code -PwaveRecipes=content/gt6-item-pipe-acquisition}.
 */
@GameTestHolder(ItemPipeAcquisitionGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class ItemPipeAcquisitionGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_content_gt6_item_pipe_acquisition";
    private static final String TEMPLATE = "empty";

    private ItemPipeAcquisitionGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void copperMediumTableCraftsFromCurvedPlate(
            GameTestHelper helper) {
        ItemStack curved = MaterialLookup.stack(
                "copper", MaterialPrefixes.CURVED_PLATE);
        Item pipe = ModItems.materialItem(
                "copper", MaterialPrefixes.ITEM_PIPE).get();
        ItemStack assembled = assembleNamed(
                helper,
                "pipe/item_table/copper/item_pipe",
                3,
                2,
                List.of(
                        curved,
                        curved.copy(),
                        curved.copy(),
                        new ItemStack(ModItems.MATERIAL_WRENCH.get()),
                        new ItemStack(ModItems.MATERIAL_FILE.get()),
                        new ItemStack(ModItems.SMITHING_HAMMER.get())));
        helper.assertTrue(
                assembled.is(pipe) && assembled.getCount() == 1,
                "copper medium item pipe did not craft from curved plate: "
                        + assembled);
        helper.assertTrue(
                helper.getLevel().getRecipeManager()
                        .byKey(id("pipe/item_table/copper/item_pipe"))
                        .isPresent(),
                "missing copper medium item-pipe table recipe");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void copperHugeItemTableCraftsFromDoublePlate(
            GameTestHelper helper) {
        ItemStack plate = MaterialLookup.stack(
                "copper", MaterialPrefixes.DOUBLE_PLATE);
        Item huge = ModItems.materialItem(
                "copper", MaterialPrefixes.HUGE_ITEM_PIPE).get();
        ItemStack assembled = assembleNamed(
                helper,
                "pipe/item_table/copper/huge_item_pipe",
                3,
                3,
                List.of(
                        plate,
                        plate.copy(),
                        plate.copy(),
                        new ItemStack(ModItems.MATERIAL_WRENCH.get()),
                        new ItemStack(ModItems.MATERIAL_FILE.get()),
                        new ItemStack(ModItems.SMITHING_HAMMER.get()),
                        plate.copy(),
                        plate.copy(),
                        plate.copy()));
        helper.assertTrue(
                assembled.is(huge) && assembled.getCount() == 1,
                "copper huge item pipe did not craft from double plates: "
                        + assembled);
        helper.assertTrue(
                helper.getLevel().getRecipeManager()
                        .byKey(id("pipe/item_table/copper/huge_item_pipe"))
                        .isPresent(),
                "missing copper huge item-pipe table recipe");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void itemTableDoesNotUseFlatPlate(GameTestHelper helper) {
        ItemStack plateStack = MaterialLookup.stack(
                "copper", MaterialPrefixes.PLATE);
        for (RecipeHolder<?> holder : helper.getLevel()
                .getRecipeManager()
                .getAllRecipesFor(RecipeType.CRAFTING)) {
            if (!(holder.value() instanceof ShapedCatalystRecipe shaped)) {
                continue;
            }
            if (!isCopperOrdinaryItemPipe(shaped.getResultItem(
                    helper.getLevel().registryAccess()))) {
                continue;
            }
            Ingredient operand = shaped.ingredients().get("P");
            helper.assertTrue(
                    operand != null,
                    "copper item-pipe table missing P: " + holder.id());
            helper.assertFalse(
                    operand.test(plateStack),
                    "copper item-pipe craft used plate as a curved_plate "
                            + "stand-in: "
                            + holder.id());
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void copperRestrictiveCraftsFromMediumAndSteelRing(
            GameTestHelper helper) {
        Item medium = ModItems.materialItem(
                "copper", MaterialPrefixes.ITEM_PIPE).get();
        ItemStack ring = MaterialLookup.stack("steel", MaterialPrefixes.RING);
        Item restrictive = ModItems.materialItem(
                "copper", MaterialPrefixes.RESTRICTIVE_ITEM_PIPE).get();
        ItemStack assembled = assembleNamed(
                helper,
                "pipe/restrictive/copper/restrictive_item_pipe",
                3,
                3,
                List.of(
                        ItemStack.EMPTY,
                        new ItemStack(ModItems.SMITHING_HAMMER.get()),
                        ItemStack.EMPTY,
                        ring,
                        new ItemStack(medium),
                        ring.copy(),
                        ItemStack.EMPTY,
                        ring.copy(),
                        ItemStack.EMPTY));
        helper.assertTrue(
                assembled.is(restrictive) && assembled.getCount() == 1,
                "copper restrictive did not craft from medium + steel ring: "
                        + assembled);
        helper.assertTrue(
                helper.getLevel().getRecipeManager()
                        .byKey(id(
                                "pipe/restrictive/copper/restrictive_item_pipe"))
                        .isPresent(),
                "missing copper restrictive recipe");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void restrictiveUsesSteelRingNotInvented(
            GameTestHelper helper) {
        ItemStack steelRingStack = MaterialLookup.stack(
                "steel", MaterialPrefixes.RING);
        Item circuit = ModItems.PROGRAMMED_CIRCUIT.get();
        ItemStack circuitStack = new ItemStack(circuit);
        int seen = 0;
        for (RecipeHolder<?> holder : helper.getLevel()
                .getRecipeManager()
                .getAllRecipesFor(RecipeType.CRAFTING)) {
            if (!(holder.value() instanceof ShapedCatalystRecipe shaped)) {
                continue;
            }
            if (!isCopperRestrictive(shaped.getResultItem(
                    helper.getLevel().registryAccess()))) {
                continue;
            }
            seen++;
            Ingredient ring = shaped.ingredients().get("R");
            helper.assertTrue(
                    ring != null && ring.test(steelRingStack),
                    "copper restrictive did not use steel/ring: "
                            + holder.id());
            for (Ingredient ingredient : shaped.ingredients().values()) {
                helper.assertFalse(
                        ingredient.test(circuitStack),
                        "copper restrictive used programmed_circuit: "
                                + holder.id());
            }
        }
        helper.assertTrue(seen > 0, "no copper restrictive crafts were published");
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

    private static boolean isCopperOrdinaryItemPipe(ItemStack result) {
        return result.is(ModItems.materialItem(
                        "copper", MaterialPrefixes.ITEM_PIPE).get())
                || result.is(ModItems.materialItem(
                        "copper", MaterialPrefixes.LARGE_ITEM_PIPE).get())
                || result.is(ModItems.materialItem(
                        "copper", MaterialPrefixes.HUGE_ITEM_PIPE).get());
    }

    private static boolean isCopperRestrictive(ItemStack result) {
        return result.is(ModItems.materialItem(
                        "copper", MaterialPrefixes.RESTRICTIVE_ITEM_PIPE).get())
                || result.is(ModItems.materialItem(
                        "copper",
                        MaterialPrefixes.LARGE_RESTRICTIVE_ITEM_PIPE).get())
                || result.is(ModItems.materialItem(
                        "copper",
                        MaterialPrefixes.HUGE_RESTRICTIVE_ITEM_PIPE).get());
    }

    private static ItemStack assembleNamed(
            GameTestHelper helper,
            String path,
            int width,
            int height,
            List<ItemStack> slots) {
        RecipeHolder<?> holder = helper.getLevel()
                .getRecipeManager()
                .byKey(id(path))
                .orElseThrow();
        helper.assertTrue(
                holder.value() instanceof CraftingRecipe,
                "named recipe is not crafting: " + path);
        CraftingRecipe recipe = (CraftingRecipe) holder.value();
        CraftingInput input = CraftingInput.of(
                width, height, new ArrayList<>(slots));
        helper.assertTrue(
                recipe.matches(input, helper.getLevel()),
                "named recipe did not match: " + path);
        return recipe.assemble(
                input, helper.getLevel().registryAccess());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
