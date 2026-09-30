package com.masson.cruciblecraft.gametest;

import java.util.List;

import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.item.TechnologicalPartCatalog;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Remaining GT6 T2–T6 circuit identities and the Redstone Alloy boule cut chain.
 * Lives on the opt-in default grid ({@link CrucibleCraftGameTests#NAMESPACE}).
 */
@GameTestHolder(CrucibleCraftGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class CircuitTierGameTests {
    private static final String TEMPLATE = "empty";
    private static final List<String> NEW_RECIPES = List.of(
            "machine/laser_engraver/circuit_wire_gold",
            "machine/press/circuit_plate_gold",
            "machine/press/circuit_part_good",
            "machine/press/circuit_part_advanced",
            "machine/press/circuit_part_elite",
            "machine/press/circuit_part_ultimate",
            "machine/press/circuit_board_good",
            "machine/press/circuit_board_advanced",
            "machine/press/circuit_board_elite",
            "machine/press/circuit_board_ultimate",
            "machine/bath/circuit_good_molten_tin",
            "machine/bath/circuit_good_molten_soldering_alloy",
            "machine/bath/circuit_advanced_molten_soldering_alloy",
            "machine/bath/circuit_elite_molten_soldering_alloy",
            "machine/bath/circuit_ultimate_molten_soldering_alloy",
            "prefix/boule2plate_gem/redstone_alloy",
            "prefix/plate_gem2tiny/redstone_alloy",
            "compact_electric_conveyor_lv",
            "compact_electric_conveyor_ev",
            "compact_electric_piston_lv",
            "compact_electric_motor_ulv_iron_magnetic",
            "compact_electric_motor_ulv_steel_magnetic",
            "compact_electric_motor_mv",
            "compact_electric_motor_hv",
            "compact_electric_motor_iv",
            "compact_electric_piston_ulv",
            "compact_electric_piston_mv",
            "compact_electric_piston_hv",
            "compact_electric_piston_iv",
            "compact_electric_conveyor_ulv",
            "compact_electric_conveyor_mv",
            "compact_electric_conveyor_hv",
            "compact_electric_conveyor_iv",
            "compact_signal_emitter_ulv",
            "compact_signal_emitter_lv",
            "compact_signal_emitter_mv",
            "compact_signal_emitter_hv",
            "compact_signal_emitter_ev",
            "compact_signal_emitter_iv",
            "compact_sensor_ulv",
            "compact_sensor_lv",
            "compact_sensor_mv",
            "compact_sensor_hv",
            "compact_sensor_ev",
            "compact_sensor_iv",
            "compact_electric_pump_ulv",
            "compact_electric_pump_lv",
            "compact_electric_pump_mv",
            "compact_electric_pump_hv",
            "compact_electric_pump_ev",
            "compact_electric_pump_iv",
            "compact_electric_robot_arm_ulv",
            "compact_electric_robot_arm_lv",
            "compact_electric_robot_arm_mv",
            "compact_electric_robot_arm_hv",
            "compact_electric_robot_arm_ev",
            "compact_electric_robot_arm_iv",
            "compact_force_field_emitter_ulv",
            "compact_force_field_emitter_lv",
            "compact_force_field_emitter_mv",
            "compact_force_field_emitter_hv",
            "compact_force_field_emitter_ev",
            "compact_force_field_emitter_iv");

    private CircuitTierGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void remainingCircuitTiersAreRegistered(GameTestHelper helper) {
        helper.assertTrue(
                TechnologicalPartCatalog.parts().size()
                        == TechnologicalPartCatalog.PART_COUNT,
                "Technological parts catalog drifted from "
                        + TechnologicalPartCatalog.PART_COUNT
                        + " identities");
        for (String path : List.of(
                "compact_electric_conveyor_lv",
                "compact_electric_conveyor_ev",
                "compact_electric_piston_lv",
                "circuit_good",
                "circuit_advanced",
                "circuit_elite",
                "circuit_ultimate")) {
            helper.assertTrue(
                    ModItems.technologicalPart(path).get() != null,
                    "Missing technological part " + path);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void remainingCircuitRecipesArePresent(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        for (String path : NEW_RECIPES) {
            helper.assertTrue(
                    recipes.byKey(id(path)).isPresent(),
                    "Missing source-backed circuit recipe " + path);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void redstoneAlloyBouleCutsToTinyPlateGem(GameTestHelper helper) {
        ItemStack plates = craft(
                helper,
                List.of(
                        new ItemStack(ModItems.MATERIAL_SAW.get()),
                        ItemStack.EMPTY,
                        ItemStack.EMPTY,
                        ItemStack.EMPTY,
                        MaterialLookup.stack(
                                "redstone_alloy", MaterialPrefixes.BOULE),
                        ItemStack.EMPTY,
                        ItemStack.EMPTY,
                        ItemStack.EMPTY,
                        ItemStack.EMPTY));
        helper.assertTrue(
                MaterialLookup.matches(
                        plates,
                        "redstone_alloy",
                        MaterialPrefixes.PLATE_GEM)
                        && plates.getCount() == 3,
                "Redstone alloy boule did not saw into 3 crystalline plates");
        ItemStack tinies = craft(
                helper,
                List.of(
                        new ItemStack(ModItems.MATERIAL_SAW.get()),
                        ItemStack.EMPTY,
                        ItemStack.EMPTY,
                        ItemStack.EMPTY,
                        MaterialLookup.stack(
                                "redstone_alloy", MaterialPrefixes.PLATE_GEM),
                        ItemStack.EMPTY,
                        ItemStack.EMPTY,
                        ItemStack.EMPTY,
                        ItemStack.EMPTY));
        helper.assertTrue(
                MaterialLookup.matches(
                        tinies,
                        "redstone_alloy",
                        MaterialPrefixes.TINY_PLATE_GEM)
                        && tinies.getCount() == 8,
                "Redstone alloy crystalline plate did not saw into 8 tiny plates");
        helper.succeed();
    }

    private static ItemStack craft(GameTestHelper helper, List<ItemStack> slots) {
        CraftingInput input = CraftingInput.of(3, 3, slots);
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
