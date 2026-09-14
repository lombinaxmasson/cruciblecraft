package com.masson.cruciblecraft.gametest;

import java.util.ArrayList;
import java.util.List;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.mte.MteInPlaceAcquisitionCatalog;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated in-place MTE source-exact obtain. Run with
 * {@code -PwaveRecipes=content/gt6-mte-inplace-acquisition}.
 */
@GameTestHolder(MteInPlaceAcquisitionGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class MteInPlaceAcquisitionGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_content_gt6_mte_inplace_acquisition";
    private static final String TEMPLATE = "empty";

    private MteInPlaceAcquisitionGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void sourceExactRecipesAreRegistered(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        List<MteInPlaceAcquisitionCatalog.Recipe> catalog =
                MteInPlaceAcquisitionCatalog.recipes();
        helper.assertTrue(
                !catalog.isEmpty(),
                "in-place MTE acquisition catalog is empty");
        for (MteInPlaceAcquisitionCatalog.Recipe recipe : catalog) {
            helper.assertTrue(
                    recipes.byKey(id(recipe.path())).isPresent(),
                    "missing source-exact recipe " + recipe.path());
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void extenderTankExtenderCrafts(GameTestHelper helper) {
        Item pipe = ModItems.materialItem(
                "steel", MaterialPrefixes.FLUID_PIPE).get();
        Item casing = ModItems.materialItem(
                "steel", MaterialPrefixes.MACHINE_CASING).get();
        Item extender = MteInPlaceGameTestSupport.item("extender/tank_extender");
        ItemStack assembled = craft(
                helper,
                3,
                3,
                List.of(
                        new ItemStack(pipe),
                        new ItemStack(ModItems.SMITHING_HAMMER.get()),
                        ItemStack.EMPTY,
                        ItemStack.EMPTY,
                        new ItemStack(casing),
                        ItemStack.EMPTY,
                        ItemStack.EMPTY,
                        new ItemStack(ModItems.MATERIAL_WRENCH.get()),
                        new ItemStack(pipe)));
        helper.assertTrue(
                assembled.is(extender) && assembled.getCount() == 1,
                "tank extender did not craft from GT6 grid: " + assembled);
        helper.assertTrue(
                helper.getLevel().getRecipeManager()
                        .byKey(id("extender/tank_extender"))
                        .isPresent(),
                "missing extender/tank_extender recipe");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void noProgrammedCircuitStandIn(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        for (RecipeHolder<?> holder : helper.getLevel()
                .getRecipeManager()
                .getAllRecipesFor(RecipeType.CRAFTING)) {
            if (!MteInPlaceAcquisitionCatalog.recipes().stream()
                    .anyMatch(recipe -> holder.id().equals(id(recipe.path())))) {
                continue;
            }
            ItemStack result = holder.value().getResultItem(access);
            helper.assertTrue(
                    !result.is(ModItems.PROGRAMMED_CIRCUIT.get()),
                    holder.id() + " result is programmed_circuit");
            for (var ingredient : holder.value().getIngredients()) {
                helper.assertTrue(
                        !ingredient.test(new ItemStack(ModItems.PROGRAMMED_CIRCUIT.get())),
                        holder.id() + " uses programmed_circuit");
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void sourceExactRecipesAreCraftingType(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        for (MteInPlaceAcquisitionCatalog.Recipe recipe
                : MteInPlaceAcquisitionCatalog.recipes()) {
            var holder = manager.byKey(id(recipe.path()));
            helper.assertTrue(
                    holder.isPresent(),
                    "missing source-exact recipe " + recipe.path());
            helper.assertTrue(
                    holder.get().value().getType() == RecipeType.CRAFTING,
                    recipe.path() + " is not RecipeType.CRAFTING; EMI vanilla craft cannot see it");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void blockedWoodPanelHasNoRecipe(GameTestHelper helper) {
        helper.assertTrue(
                helper.getLevel().getRecipeManager()
                        .byKey(id("panel/wood_239"))
                        .isEmpty(),
                "wood panel PlankData.PLANKS recipe must stay uncraftable");
        helper.succeed();
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
