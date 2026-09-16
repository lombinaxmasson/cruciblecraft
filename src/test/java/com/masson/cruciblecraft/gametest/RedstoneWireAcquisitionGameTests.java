package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.redstonewire.RedstoneWireKind;
import com.masson.cruciblecraft.recipe.gt.GTRecipeEntry;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated GT6 insulated-redstone acquisition. Run with
 * {@code -PwaveRecipes=content/gt6-redstone-wire-acquisition}.
 */
@GameTestHolder(RedstoneWireAcquisitionGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class RedstoneWireAcquisitionGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_content_gt6_redstone_wire_acquisition";
    private static final String TEMPLATE = "empty";

    private RedstoneWireAcquisitionGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void redAlloyCableLaminatesFromRubberPlate(
            GameTestHelper helper) {
        assertPlateRecipe(helper, "red_alloy");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void signalumCableLaminatesFromRubberPlate(
            GameTestHelper helper) {
        assertPlateRecipe(helper, "signalum");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void lumiumCableLaminatesFromRubberPlate(
            GameTestHelper helper) {
        assertPlateRecipe(helper, "lumium");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void foilRecipeGatedOnLiveRubberFoil(GameTestHelper helper) {
        boolean foilLive = MaterialLookup.item(
                "rubber", MaterialPrefixes.FOIL).isPresent();
        RecipeHolder<?> holder = helper.getLevel()
                .getRecipeManager()
                .byKey(id("redstone/laminator/red_alloy/cable_from_foil"))
                .orElse(null);
        if (foilLive) {
            helper.assertTrue(
                    holder != null,
                    "rubber/foil is live but red_alloy foil laminator is missing");
            GTRecipeEntry entry = requireGt(helper, holder);
            helper.assertTrue(
                    entry.recipe().itemInputCounts().get(0) == 4,
                    "foil recipe is not four foils");
        } else {
            helper.assertTrue(
                    holder == null,
                    "red_alloy foil laminator was invented without rubber/foil");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void insulatedCablesAreNotEuTinOutputs(
            GameTestHelper helper) {
        Item tinCable = ModItems.materialItem(
                "tin", MaterialPrefixes.CABLE).get();
        for (RedstoneWireKind kind : RedstoneWireKind.insulatedKinds()) {
            GTRecipeEntry entry = requireGt(
                    helper,
                    helper.getLevel()
                            .getRecipeManager()
                            .byKey(id("redstone/laminator/"
                                    + kind.materialId()
                                    + "/cable_from_plate"))
                            .orElseThrow());
            helper.assertTrue(
                    entry.map().equals(ModRecipeMaps.LAMINATOR.id()),
                    kind.materialId() + " was not a laminator recipe");
            ItemStack output = entry.recipe().itemOutputs().getFirst();
            helper.assertFalse(
                    output.is(tinCable),
                    kind.materialId() + " folded onto tin/cable");
            helper.assertTrue(
                    output.is(ModItems.materialItem(
                            kind.materialId(),
                            MaterialPrefixes.CABLE).get()),
                    kind.materialId() + " laminator result drifted");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void noProgrammedCircuitInLaminator(GameTestHelper helper) {
        Item circuit = ModItems.PROGRAMMED_CIRCUIT.get();
        ItemStack circuitStack = new ItemStack(circuit);
        for (RedstoneWireKind kind : RedstoneWireKind.insulatedKinds()) {
            GTRecipeEntry entry = requireGt(
                    helper,
                    helper.getLevel()
                            .getRecipeManager()
                            .byKey(id("redstone/laminator/"
                                    + kind.materialId()
                                    + "/cable_from_plate"))
                            .orElseThrow());
            for (Ingredient ingredient : entry.recipe().itemInputs()) {
                helper.assertFalse(
                        ingredient.test(circuitStack),
                        kind.materialId() + " used programmed_circuit");
            }
        }
        helper.succeed();
    }

    private static void assertPlateRecipe(
            GameTestHelper helper, String material) {
        Item plate = MaterialLookup.item(
                "rubber", MaterialPrefixes.PLATE).orElseThrow();
        Item wire = ModItems.materialItem(
                material, MaterialPrefixes.WIRE).get();
        Item cable = ModItems.materialItem(
                material, MaterialPrefixes.CABLE).get();
        GTRecipeEntry entry = requireGt(
                helper,
                helper.getLevel()
                        .getRecipeManager()
                        .byKey(id("redstone/laminator/"
                                + material
                                + "/cable_from_plate"))
                        .orElseThrow());
        helper.assertTrue(
                entry.map().equals(ModRecipeMaps.LAMINATOR.id()),
                material + " plate recipe is not laminator");
        helper.assertTrue(
                entry.recipe().itemInputs().get(0).test(new ItemStack(plate)),
                material + " plate recipe missing rubber/plate");
        helper.assertTrue(
                entry.recipe().itemInputs().get(1).test(new ItemStack(wire)),
                material + " plate recipe missing matching wire");
        helper.assertTrue(
                entry.recipe().itemOutputs().getFirst().is(cable),
                material + " plate recipe result drifted");
    }

    private static GTRecipeEntry requireGt(
            GameTestHelper helper, RecipeHolder<?> holder) {
        helper.assertTrue(
                holder.value() instanceof GTRecipeEntry,
                "recipe is not a GT recipe: " + holder.id());
        return (GTRecipeEntry) holder.value();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
