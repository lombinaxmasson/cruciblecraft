package com.masson.cruciblecraft.gametest;

import java.util.List;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.FuelGeneratorBlock;
import com.masson.cruciblecraft.content.block.SolidBurningBoxBlock;
import com.masson.cruciblecraft.content.blockentity.FuelGeneratorBlockEntity;
import com.masson.cruciblecraft.content.blockentity.SolidBurningBoxBlockEntity;
import com.masson.cruciblecraft.energy.converter.EnergyConverterCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.verification.PlayerCompleteSmoke;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated converter-catalog gate. Run with
 * {@code -PwaveRecipes=runtime/converter-catalog}.
 */
@GameTestHolder(EnergyConverterCatalogGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class EnergyConverterCatalogGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_runtime_converter_catalog";
    private static final String TEMPLATE = "empty";
    private static final String CAPABILITY = "energy/converter-catalog";
    private static final List<String> SIGNOFF_ITEMS = List.of(
            "bronze_burning_box_solid",
            "bronze_burning_box_gas",
            "bronze_boiler",
            "bronze_steam_engine",
            "bronze_fuel_engine",
            "bronze_dynamo",
            "steel_galvanized_electric_motor",
            "steel_galvanized_electric_heater",
            "aluminium_electric_heater",
            "stainless_steel_electric_heater",
            "chromium_electric_heater",
            "titanium_electric_heater",
            "steel_galvanized_electric_engine",
            "aluminium_electric_engine",
            "stainless_steel_electric_engine",
            "chromium_electric_engine",
            "titanium_electric_engine");

    private EnergyConverterCatalogGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void gasBurningBoxFeedsAdjacentHu(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        FuelGeneratorBlockEntity generator =
                GameTestHeatSources.placeHuSource(helper, pos);
        helper.assertTrue(
                helper.getBlockState(pos).is(ModBlocks.BRONZE_BURNING_BOX_GAS.get()),
                "Adjacent HU fixture is not bronze_burning_box_gas");
        helper.assertTrue(
                helper.getBlockState(pos).getValue(FuelGeneratorBlock.FACING)
                        == Direction.EAST,
                "Bronze gas burning box did not face east");
        helper.assertTrue(
                generator.stored(EnergyType.HEAT) > 0L,
                "Bronze gas burning box stored no HU");
        helper.startSequence()
                .thenExecute(() -> helper.assertTrue(
                        generator.extract(
                                EnergyType.HEAT,
                                1L,
                                24L,
                                Direction.UP,
                                false)
                                > 0L,
                        "Bronze gas burning box did not emit HU upward"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void playerSurfaceIsRegistered(GameTestHelper helper) {
        helper.assertTrue(
                EnergyConverterCatalog.profiles().size() == 179,
                "Converter catalog drifted from 179 loader rows");
        helper.assertTrue(
                ModItems.BRONZE_BURNING_BOX_GAS.get() != null,
                "Bronze gas burning box item missing");
        PlayerCompleteSmoke.writeIfConfigured("gameTestServer", CAPABILITY);
        helper.assertTrue(
                PlayerCompleteSmoke.snapshot("gameTestServer", CAPABILITY)
                        .get("status")
                        .getAsString()
                        .equals("PASS"),
                "Player-complete registry snapshot failed");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void representativeRecipesAreSurvivalCraftable(
            GameTestHelper helper) {
        for (String path : SIGNOFF_ITEMS) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", path);
            helper.assertTrue(
                    helper.getLevel().getRecipeManager().byKey(id).isPresent(),
                    "Missing survival recipe " + path);
        }
        var plate = MaterialLookup.item("bronze", MaterialPrefixes.DOUBLE_PLATE)
                .orElseThrow();
        List<ItemStack> slots = List.of(
                ItemStack.EMPTY,
                new ItemStack(plate),
                ItemStack.EMPTY,
                new ItemStack(plate),
                ItemStack.EMPTY,
                new ItemStack(plate),
                new ItemStack(plate),
                ItemStack.EMPTY,
                new ItemStack(plate));
        ItemStack assembled = craft(helper, 3, 3, slots);
        helper.assertTrue(
                assembled.is(ModItems.BRONZE_BOILER.get())
                        && assembled.getCount() == 1,
                "Bronze boiler loader recipe missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void solidBurningBoxBurnsCoal(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(
                pos,
                ModBlocks.BRONZE_BURNING_BOX_SOLID.get()
                        .defaultBlockState()
                        .setValue(SolidBurningBoxBlock.FACING, Direction.EAST));
        SolidBurningBoxBlockEntity box = helper.getBlockEntity(pos);
        helper.assertTrue(
                box.insertFuel(new ItemStack(Items.COAL)),
                "Solid burning box rejected coal");
        box.ignite();
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        box.energyStored() > 0L,
                        "Coal did not produce HU in the bronze solid burning box"))
                .thenSucceed();
    }

    private static ItemStack craft(
            GameTestHelper helper, int width, int height, List<ItemStack> slots) {
        CraftingInput input = CraftingInput.of(width, height, slots);
        return helper.getLevel()
                .getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .map(holder -> holder.value().assemble(
                        input, helper.getLevel().registryAccess()))
                .orElse(ItemStack.EMPTY);
    }
}
