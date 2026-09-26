package com.masson.cruciblecraft.gametest;

import java.util.List;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.content.block.ElectricEngineBlock;
import com.masson.cruciblecraft.content.block.ElectricHeaterBlock;
import com.masson.cruciblecraft.content.blockentity.ElectricEngineBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ElectricHeaterBlockEntity;
import com.masson.cruciblecraft.energy.converter.EnergyConverterCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Runtime and survival gates for the merged EU conversion card. */
@GameTestHolder(ElectricConversionGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class ElectricConversionGameTests {
    public static final String NAMESPACE = "cruciblecraft_energy";
    private static final String TEMPLATE = "empty";
    private static final List<String> VARIANTS = List.of(
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

    private ElectricConversionGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void catalogContainsTenElectricConversions(
            GameTestHelper helper) {
        helper.assertTrue(
                EnergyConverterCatalog.profiles().stream()
                                .filter(profile ->
                                        profile.stage().startsWith("electric_"))
                                .count()
                        == 10L,
                "Electric conversion catalog must contain ten source rows");
        for (String variant : VARIANTS) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", variant);
            helper.assertTrue(
                    EnergyConverterCatalog.require(id) != null,
                    "Missing electric conversion profile " + variant);
            helper.assertTrue(
                    helper.getLevel().getRecipeManager().byKey(id).isPresent(),
                    "Missing survival recipe " + variant);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void heaterConsumesEuWhenHuIsBlocked(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(
                pos,
                ModBlocks.converterBlocksById()
                        .get(ResourceLocation.fromNamespaceAndPath(
                                "cruciblecraft",
                                "steel_galvanized_electric_heater"))
                        .get()
                        .defaultBlockState()
                        .setValue(ElectricHeaterBlock.FACING, Direction.UP));
        ElectricHeaterBlockEntity heater = helper.getBlockEntity(pos);
        helper.assertTrue(
                heater.insert(
                                EnergyType.ELECTRIC,
                                32L,
                                1L,
                                Direction.DOWN,
                                false)
                        == 1L,
                "Electric heater rejected nominal EU");
        ElectricHeaterBlockEntity.serverTick(
                helper.getLevel(),
                pos,
                helper.getBlockState(pos),
                heater);
        helper.assertTrue(heater.active(), "Heater did not become active");
        helper.assertTrue(
                heater.stored() == 0L,
                "WASTE_ENERGY heater retained EU with blocked HU output");
        helper.assertTrue(
                "blocked".equals(heater.status()),
                "Heater did not report blocked HU output");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void enginePreservesStateAndPistonSemantics(
            GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft",
                "steel_galvanized_electric_engine");
        helper.setBlock(
                pos,
                ModBlocks.converterBlocksById().get(id).get()
                        .defaultBlockState()
                        .setValue(ElectricEngineBlock.FACING, Direction.NORTH));
        ElectricEngineBlockEntity engine = helper.getBlockEntity(pos);
        helper.assertTrue(
                engine.insert(
                                EnergyType.ELECTRIC,
                                32L,
                                1L,
                                Direction.SOUTH,
                                false)
                        == 1L,
                "Electric engine rejected nominal EU");
        helper.assertTrue(
                engine.currentInput() == 32L
                        && engine.currentOutput() == 16L,
                "Default electric-engine state is not nominal");
        engine.cycleState();
        helper.assertTrue(
                engine.state() == 16
                        && engine.currentInput() == 34L
                        && engine.currentOutput() == 17L,
                "Screwdriver state did not preserve GT6 state scaling");
        helper.assertTrue(
                engine.piston() >= 0 && engine.piston() < 4,
                "Electric-engine piston phase escaped its four-state range");
        ElectricEngineBlockEntity.serverTick(
                helper.getLevel(),
                pos,
                helper.getBlockState(pos),
                engine);
        helper.assertTrue(
                engine.stored() == 32L,
                "Engine consumed EU before meeting its selected state input");
        helper.succeed();
    }
}
