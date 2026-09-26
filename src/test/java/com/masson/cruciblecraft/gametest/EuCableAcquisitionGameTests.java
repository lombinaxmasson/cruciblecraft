package com.masson.cruciblecraft.gametest;

import java.util.ArrayList;
import java.util.List;

import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
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
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated GT6 EU cable acquisition. Run with
 * {@code -PgameTestGrid=energy}.
 */
@GameTestHolder(EuCableAcquisitionGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class EuCableAcquisitionGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_energy";
    private static final String TEMPLATE = "empty";

    private EuCableAcquisitionGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void copperPlateCraftsWireWithCutter(GameTestHelper helper) {
        ItemStack plate = MaterialLookup.stack("copper", MaterialPrefixes.PLATE);
        Item wire = ModItems.materialItem(
                "copper", MaterialPrefixes.WIRE).get();
        ItemStack assembled = assembleNamed(
                helper,
                "cable/table/copper/wire",
                2,
                1,
                List.of(
                        plate,
                        new ItemStack(ModItems.MATERIAL_WIRE_CUTTER.get())));
        helper.assertTrue(
                assembled.is(wire) && assembled.getCount() == 1,
                "copper wire did not craft from plate + cutter: " + assembled);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void copperCableCraftsFromWireAndRubberPlate(
            GameTestHelper helper) {
        Item wire = ModItems.materialItem(
                "copper", MaterialPrefixes.WIRE).get();
        ItemStack rubber = MaterialLookup.stack(
                "rubber", MaterialPrefixes.PLATE);
        Item cable = ModItems.materialItem(
                "copper", MaterialPrefixes.CABLE).get();
        ItemStack assembled = assembleNamed(
                helper,
                "cable/shapeless/copper/cable",
                2,
                1,
                List.of(new ItemStack(wire), rubber));
        helper.assertTrue(
                assembled.is(cable) && assembled.getCount() == 1,
                "copper cable did not craft from wire + rubber plate: "
                        + assembled);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void copperDoubleWirePacksFromTwoSingles(
            GameTestHelper helper) {
        Item wire = ModItems.materialItem(
                "copper", MaterialPrefixes.WIRE).get();
        Item doubled = ModItems.materialItem(
                "copper", MaterialPrefixes.DOUBLE_WIRE).get();
        ItemStack assembled = assembleNamed(
                helper,
                "cable/pack/copper/double_wire_from_wire",
                2,
                1,
                List.of(new ItemStack(wire), new ItemStack(wire)));
        helper.assertTrue(
                assembled.is(doubled) && assembled.getCount() == 1,
                "copper double wire did not pack from two singles: "
                        + assembled);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void copperUnpackDoubleReturnsTwoSingles(
            GameTestHelper helper) {
        Item wire = ModItems.materialItem(
                "copper", MaterialPrefixes.WIRE).get();
        Item doubled = ModItems.materialItem(
                "copper", MaterialPrefixes.DOUBLE_WIRE).get();
        ItemStack assembled = assembleNamed(
                helper,
                "cable/unpack/copper/wire_from_double_wire",
                1,
                1,
                List.of(new ItemStack(doubled)));
        helper.assertTrue(
                assembled.is(wire) && assembled.getCount() == 2,
                "copper double wire did not unpack to two singles: "
                        + assembled);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void plate2wireDoesNotUseProgrammedCircuit(
            GameTestHelper helper) {
        Item circuit = ModItems.PROGRAMMED_CIRCUIT.get();
        ItemStack circuitStack = new ItemStack(circuit);
        RecipeHolder<?> holder = helper.getLevel()
                .getRecipeManager()
                .byKey(id("cable/table/copper/wire"))
                .orElseThrow();
        helper.assertTrue(
                holder.value() instanceof ShapedCatalystRecipe,
                "copper plate2wire is not a shaped catalyst recipe");
        ShapedCatalystRecipe shaped = (ShapedCatalystRecipe) holder.value();
        for (Ingredient ingredient : shaped.ingredients().values()) {
            helper.assertFalse(
                    ingredient.test(circuitStack),
                    "copper plate2wire used programmed_circuit");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void redAlloyHasNoEuPlate2wire(GameTestHelper helper) {
        helper.assertFalse(
                helper.getLevel()
                        .getRecipeManager()
                        .byKey(id("cable/table/red_alloy/wire"))
                        .isPresent(),
                "red_alloy received an EU plate2wire craft");
        helper.assertFalse(
                helper.getLevel()
                        .getRecipeManager()
                        .byKey(id("cable/shapeless/red_alloy/cable"))
                        .isPresent(),
                "red_alloy received an EU shapeless cable craft");
        helper.succeed();
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
