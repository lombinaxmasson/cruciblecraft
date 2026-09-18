package com.masson.cruciblecraft.gametest;

import java.util.List;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.ElectricEngineBlock;
import com.masson.cruciblecraft.content.block.FuelGeneratorBlock;
import com.masson.cruciblecraft.content.block.FluidPipeBlock;
import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.block.SolidBurningBoxBlock;
import com.masson.cruciblecraft.content.block.SteamEngineBlock;
import com.masson.cruciblecraft.content.blockentity.BoilerBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ElectricEngineBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FuelGeneratorBlockEntity;
import com.masson.cruciblecraft.content.blockentity.LargeBoilerBlockEntity;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.blockentity.SolidBurningBoxBlockEntity;
import com.masson.cruciblecraft.content.blockentity.SteamEngineBlockEntity;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.energy.steam.SteamTurbineCatalog;
import com.masson.cruciblecraft.energy.converter.EnergyConverterCatalog;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCover;
import com.masson.cruciblecraft.logistics.pipe.PipeCatalog;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModFluids;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.registry.ModRecipeMaps;
import com.masson.cruciblecraft.steam.SteamConversion;
import com.masson.cruciblecraft.verification.PlayerCompleteSmoke;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
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
        var plate = MaterialLookup.stack("bronze", MaterialPrefixes.DOUBLE_PLATE);
        List<ItemStack> slots = List.of(
                ItemStack.EMPTY,
                plate,
                ItemStack.EMPTY,
                plate.copy(),
                ItemStack.EMPTY,
                plate.copy(),
                plate.copy(),
                ItemStack.EMPTY,
                plate.copy());
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

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void steamEnginePushesDistilledWaterToSides(
            GameTestHelper helper) {
        BlockPos enginePos = new BlockPos(3, 1, 3);
        BlockPos boilerPos = enginePos.north();
        helper.setBlock(
                enginePos,
                ModBlocks.BRONZE_STEAM_ENGINE.get().defaultBlockState()
                        .setValue(SteamEngineBlock.FACING, Direction.EAST));
        helper.setBlock(boilerPos, ModBlocks.BRONZE_BOILER.get());
        SteamEngineBlockEntity engine = helper.getBlockEntity(enginePos);
        BoilerBlockEntity boiler = helper.getBlockEntity(boilerPos);
        IFluidHandler steamIn = engine.fluids(Direction.WEST);
        helper.assertTrue(
                steamIn != null
                        && steamIn.fill(
                                new FluidStack(
                                        ModFluids.STEAM_SOURCE.get(),
                                        SteamConversion.ENGINE_STEAM_PER_BATCH),
                                IFluidHandler.FluidAction.EXECUTE)
                                == SteamConversion.ENGINE_STEAM_PER_BATCH,
                "Could not fill steam into the engine back");
        SteamEngineBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(enginePos),
                helper.getBlockState(enginePos),
                engine);
        helper.assertTrue(
                engine.stored() >= SteamConversion.KU_PER_ENGINE_BATCH
                        && boiler.waterAmount()
                                == SteamConversion.EXHAUST_WATER_PER_BATCH
                        && engine.exhaustAmount() == 0,
                "Steam engine did not convert steam to DistW and push it to a side");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void steamEngineSoftHammerStopsSteamAndKu(
            GameTestHelper helper) {
        BlockPos enginePos = new BlockPos(3, 1, 3);
        helper.setBlock(
                enginePos,
                ModBlocks.BRONZE_STEAM_ENGINE.get().defaultBlockState()
                        .setValue(SteamEngineBlock.FACING, Direction.EAST));
        SteamEngineBlockEntity engine = helper.getBlockEntity(enginePos);
        IFluidHandler steamIn = engine.fluids(Direction.WEST);
        helper.assertTrue(steamIn != null, "Steam engine back input missing");
        helper.assertTrue(
                steamIn.fill(
                        new FluidStack(
                                ModFluids.STEAM_SOURCE.get(),
                                SteamConversion.ENGINE_STEAM_PER_BATCH),
                        IFluidHandler.FluidAction.EXECUTE)
                        == SteamConversion.ENGINE_STEAM_PER_BATCH,
                "Could not prime steam before stop");
        helper.assertTrue(!engine.toggleStopped() && engine.stopped(),
                "Soft-hammer toggle did not stop the engine");
        helper.assertTrue(
                steamIn.fill(
                        new FluidStack(
                                ModFluids.STEAM_SOURCE.get(), 200),
                        IFluidHandler.FluidAction.EXECUTE)
                        == 0,
                "Stopped steam engine still accepted steam");
        SteamEngineBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(enginePos),
                helper.getBlockState(enginePos),
                engine);
        helper.assertTrue(
                engine.steamAmount() == SteamConversion.ENGINE_STEAM_PER_BATCH
                        && engine.stored() == 0L
                        && engine.exhaustAmount() == 0
                        && "stopped".equals(engine.status()),
                "Stopped steam engine still converted steam");
        helper.assertTrue(
                engine.toggleStopped() && !engine.stopped(),
                "Soft-hammer toggle did not restart the engine");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void steamEngineTrashesUnpushedDistW(
            GameTestHelper helper) {
        BlockPos enginePos = new BlockPos(3, 1, 3);
        helper.setBlock(
                enginePos,
                ModBlocks.BRONZE_STEAM_ENGINE.get().defaultBlockState()
                        .setValue(SteamEngineBlock.FACING, Direction.EAST));
        SteamEngineBlockEntity engine = helper.getBlockEntity(enginePos);
        IFluidHandler steamIn = engine.fluids(Direction.WEST);
        helper.assertTrue(
                steamIn.fill(
                        new FluidStack(
                                ModFluids.STEAM_SOURCE.get(),
                                SteamConversion.ENGINE_STEAM_PER_BATCH),
                        IFluidHandler.FluidAction.EXECUTE)
                        == SteamConversion.ENGINE_STEAM_PER_BATCH,
                "Could not fill steam into an isolated engine");
        SteamEngineBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(enginePos),
                helper.getBlockState(enginePos),
                engine);
        helper.assertTrue(
                engine.steamAmount() == 0
                        && engine.stored()
                                >= SteamConversion.KU_PER_ENGINE_BATCH
                        && engine.exhaustAmount() == 0
                        && "running".equals(engine.status()),
                "Isolated steam engine did not trash unpushed DistW");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fuelEnginePushesCarbonDioxideToBack(
            GameTestHelper helper) {
        BlockPos enginePos = new BlockPos(3, 1, 3);
        BlockPos exhaustPos = enginePos.west();
        helper.setBlock(
                enginePos,
                ModBlocks.BRONZE_FUEL_ENGINE.get().defaultBlockState()
                        .setValue(FuelGeneratorBlock.FACING, Direction.EAST));
        FluidPipeBlock pipeBlock = (FluidPipeBlock) ModBlocks.pipeBlock(
                "copper",
                MaterialPrefixes.TINY_FLUID_PIPE,
                PipeCatalog.Kind.FLUID).get();
        helper.setBlock(exhaustPos, pipeBlock);
        helper.getLevel().setBlock(
                helper.absolutePos(exhaustPos),
                helper.getBlockState(exhaustPos)
                        .setValue(FluidPipeBlock.EAST, true),
                Block.UPDATE_CLIENTS);
        FuelGeneratorBlockEntity engine = helper.getBlockEntity(enginePos);
        FluidPipeBlockEntity pipe = helper.getBlockEntity(exhaustPos);
        GTRecipe fuel = requireFuelOil();
        FluidStack required = fuel.fluidInputs().getFirst();
        IFluidHandler input = engine.fluids(Direction.UP);
        helper.assertTrue(
                input != null
                        && engine.fluids(Direction.WEST) != null
                        && input.fill(
                                required, IFluidHandler.FluidAction.EXECUTE)
                                == required.getAmount(),
                "Could not fill fuel into a side of the fuel engine");
        helper.startSequence()
                .thenIdle(fuel.duration() + 2)
                .thenExecute(() -> helper.assertTrue(
                        engine.outputAmount(0) == 0
                                && pipe.storedFluid().getAmount()
                                        == fuel.fluidOutputs()
                                                .getFirst().getAmount()
                                && pipe.storedFluid().is(
                                        fuel.fluidOutputs()
                                                .getFirst().getFluid()),
                        "Fuel engine did not push recipe CO2 to its back"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fuelEngineVentsCarbonDioxideIntoAir(
            GameTestHelper helper) {
        BlockPos enginePos = new BlockPos(3, 1, 3);
        helper.setBlock(
                enginePos,
                ModBlocks.BRONZE_FUEL_ENGINE.get().defaultBlockState()
                        .setValue(FuelGeneratorBlock.FACING, Direction.EAST));
        FuelGeneratorBlockEntity engine = helper.getBlockEntity(enginePos);
        GTRecipe fuel = requireFuelOil();
        helper.assertTrue(
                engine.fluids(Direction.UP).fill(
                                fuel.fluidInputs().getFirst(),
                                IFluidHandler.FluidAction.EXECUTE)
                        == fuel.fluidInputs().getFirst().getAmount(),
                "Could not fill fuel into an isolated fuel engine");
        helper.startSequence()
                .thenIdle(fuel.duration() + 2)
                .thenExecute(() -> helper.assertTrue(
                        engine.outputAmount(0) == 0,
                        "Isolated fuel engine did not vent leftover CO2 into air"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void steamEngineCoversOnlyAlongAxis(GameTestHelper helper) {
        BlockPos enginePos = new BlockPos(3, 1, 3);
        helper.setBlock(
                enginePos,
                ModBlocks.BRONZE_STEAM_ENGINE.get().defaultBlockState()
                        .setValue(SteamEngineBlock.FACING, Direction.EAST));
        SteamEngineBlockEntity engine = helper.getBlockEntity(enginePos);
        helper.assertTrue(
                engine.setCover(Direction.EAST, redstoneController()),
                "Steam engine rejected an along-axis cover");
        helper.assertFalse(
                engine.setCover(Direction.NORTH, redstoneController()),
                "Steam engine accepted an off-axis cover");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void steamEngineControllerCoverStopsConversion(
            GameTestHelper helper) {
        BlockPos enginePos = new BlockPos(3, 1, 3);
        helper.setBlock(
                enginePos,
                ModBlocks.BRONZE_STEAM_ENGINE.get().defaultBlockState()
                        .setValue(SteamEngineBlock.FACING, Direction.EAST));
        SteamEngineBlockEntity engine = helper.getBlockEntity(enginePos);
        IFluidHandler steamIn = engine.fluids(Direction.WEST);
        helper.assertTrue(
                steamIn.fill(
                        new FluidStack(
                                ModFluids.STEAM_SOURCE.get(),
                                SteamConversion.ENGINE_STEAM_PER_BATCH),
                        IFluidHandler.FluidAction.EXECUTE)
                        == SteamConversion.ENGINE_STEAM_PER_BATCH,
                "Could not fill steam before controller cover");
        helper.assertTrue(
                engine.setCover(Direction.EAST, redstoneController()),
                "Could not install steam-engine controller");
        SteamEngineBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(enginePos),
                helper.getBlockState(enginePos),
                engine);
        helper.assertTrue(
                engine.stopped()
                        && engine.steamAmount()
                                == SteamConversion.ENGINE_STEAM_PER_BATCH
                        && engine.stored() == 0L,
                "Controller cover did not stop the steam engine");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fuelEngineControllerCoverStopsNewFuel(
            GameTestHelper helper) {
        BlockPos enginePos = new BlockPos(3, 1, 3);
        helper.setBlock(
                enginePos,
                ModBlocks.BRONZE_FUEL_ENGINE.get().defaultBlockState()
                        .setValue(FuelGeneratorBlock.FACING, Direction.EAST));
        FuelGeneratorBlockEntity engine = helper.getBlockEntity(enginePos);
        GTRecipe fuel = requireFuelOil();
        helper.assertTrue(
                engine.fluids(Direction.UP).fill(
                                fuel.fluidInputs().getFirst(),
                                IFluidHandler.FluidAction.EXECUTE)
                        == fuel.fluidInputs().getFirst().getAmount(),
                "Could not fill fuel before controller cover");
        helper.assertTrue(
                engine.setCover(Direction.NORTH, redstoneController()),
                "Could not install fuel-engine controller");
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        engine.stopped()
                                && engine.progress() == 0
                                && engine.inputAmount()
                                        == fuel.fluidInputs()
                                                .getFirst().getAmount(),
                        "Controller cover did not keep the fuel engine from consuming fuel"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void gasBurningBoxControllerCoverExtinguishes(
            GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        FuelGeneratorBlockEntity box =
                GameTestHeatSources.placeHuSourceBlock(helper, pos);
        box.ignite();
        helper.assertTrue(box.burning(), "Gas box did not ignite");
        helper.assertTrue(
                box.setCover(Direction.NORTH, redstoneController()),
                "Could not install gas-box controller");
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> helper.assertFalse(
                        box.burning(),
                        "Controller cover did not extinguish the gas burning box"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void electricEngineControllerCoverStops(
            GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft",
                "steel_galvanized_electric_engine");
        helper.setBlock(
                pos,
                ModBlocks.converterBlocksById().get(id).get()
                        .defaultBlockState()
                        .setValue(ElectricEngineBlock.FACING, Direction.EAST));
        ElectricEngineBlockEntity engine = helper.getBlockEntity(pos);
        helper.assertFalse(
                engine.setCover(Direction.NORTH, redstoneController()),
                "Electric engine accepted an off-axis cover");
        helper.assertTrue(
                engine.insert(
                                EnergyType.ELECTRIC,
                                32L,
                                1L,
                                Direction.WEST,
                                false)
                        == 1L,
                "Electric engine rejected nominal EU");
        helper.assertTrue(
                engine.setCover(Direction.EAST, redstoneController()),
                "Could not install electric-engine controller");
        ElectricEngineBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(pos),
                helper.getBlockState(pos),
                engine);
        helper.assertTrue(
                engine.stopped()
                        && !engine.active()
                        && engine.stored() == 32L,
                "Controller cover did not stop the electric engine");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void fuelEngineVoidsLeftoverFuelAfter64Ticks(
            GameTestHelper helper) {
        BlockPos enginePos = new BlockPos(3, 1, 3);
        helper.setBlock(
                enginePos,
                ModBlocks.BRONZE_FUEL_ENGINE.get().defaultBlockState()
                        .setValue(FuelGeneratorBlock.FACING, Direction.EAST));
        FuelGeneratorBlockEntity engine = helper.getBlockEntity(enginePos);
        helper.assertTrue(
                engine.fluids(Direction.UP).fill(
                                new FluidStack(
                                        ModFluids.materialFluid("lubricant")
                                                .orElseThrow(),
                                        1),
                                IFluidHandler.FluidAction.EXECUTE)
                        == 1,
                "Could not fill leftover lubricant into the fuel engine");
        helper.startSequence()
                .thenIdle(63)
                .thenExecute(() -> helper.assertTrue(
                        engine.inputAmount() == 1
                                && "invalid_fuel".equals(engine.status()),
                        "Fuel engine voided leftover lubricant before 64 ticks"))
                .thenIdle(1)
                .thenExecute(() -> helper.assertTrue(
                        engine.inputAmount() == 0
                                && "idle".equals(engine.status()),
                        "Fuel engine did not void leftover lubricant after 64 ticks"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void gasBurningBoxVoidsLeftoverFuelWhileBurning(
            GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        FuelGeneratorBlockEntity box =
                GameTestHeatSources.placeHuSourceBlock(helper, pos);
        helper.assertTrue(
                box.fluids(Direction.WEST).fill(
                                new FluidStack(
                                        ModFluids.materialFluid("methane")
                                                .orElseThrow(),
                                        1),
                                IFluidHandler.FluidAction.EXECUTE)
                        == 1,
                "Could not fill leftover methane into the gas box");
        box.ignite();
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        box.inputAmount() == 0,
                        "Burning gas box did not void leftover methane"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void gasBurningBoxKeepsLeftoverFuelWhenCold(
            GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        FuelGeneratorBlockEntity box =
                GameTestHeatSources.placeHuSourceBlock(helper, pos);
        helper.assertTrue(
                box.fluids(Direction.WEST).fill(
                                new FluidStack(
                                        ModFluids.materialFluid("methane")
                                                .orElseThrow(),
                                        1),
                                IFluidHandler.FluidAction.EXECUTE)
                        == 1,
                "Could not fill leftover methane into a cold gas box");
        helper.startSequence()
                .thenIdle(5)
                .thenExecute(() -> helper.assertTrue(
                        box.inputAmount() == 1 && !box.burning(),
                        "Cold gas box voided leftover methane"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fuelEnginePlungerTrashesExhaustThenFuel(
            GameTestHelper helper) {
        BlockPos enginePos = new BlockPos(3, 1, 3);
        helper.setBlock(
                enginePos,
                ModBlocks.BRONZE_FUEL_ENGINE.get().defaultBlockState()
                        .setValue(FuelGeneratorBlock.FACING, Direction.EAST));
        helper.setBlock(enginePos.west(), Blocks.STONE);
        FuelGeneratorBlockEntity engine = helper.getBlockEntity(enginePos);
        GTRecipe fuel = requireFuelOil();
        FluidStack required = fuel.fluidInputs().getFirst();
        int filled = engine.fluids(Direction.UP).fill(
                required.copyWithAmount(required.getAmount() * 2),
                IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(
                filled == required.getAmount() * 2,
                "Could not fill two fuel batches into the fuel engine");
        helper.startSequence()
                .thenIdle(fuel.duration())
                .thenExecute(() -> {
                    helper.assertTrue(
                            engine.outputAmount(0) > 0
                                    && engine.inputAmount()
                                            == required.getAmount(),
                            "Fuel engine did not keep leftover fuel behind blocked CO2");
                    clickPlunger(helper, enginePos);
                    helper.assertTrue(
                            engine.outputAmount(0) == 0
                                    && engine.inputAmount()
                                            == required.getAmount(),
                            "Plunger did not trash fuel-engine exhaust first");
                    clickPlunger(helper, enginePos);
                    helper.assertTrue(
                            engine.outputAmount(0) == 0
                                    && engine.inputAmount() == 0,
                            "Second plunger click did not trash leftover fuel");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void gasBurningBoxPlungerTrashesWholeFuelTank(
            GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        FuelGeneratorBlockEntity box =
                GameTestHeatSources.placeHuSourceBlock(helper, pos);
        IFluidHandler input = box.fluids(Direction.WEST);
        int amount = Math.min(2_000, input.getTankCapacity(0));
        helper.assertTrue(
                amount > 1_000,
                "Gas box tank is too small to distinguish whole-tank plunger from 1000 mB");
        helper.assertTrue(
                input.fill(
                                new FluidStack(
                                        ModFluids.materialFluid("methane")
                                                .orElseThrow(),
                                        amount),
                                IFluidHandler.FluidAction.EXECUTE)
                        == amount,
                "Could not fill methane into the gas box for the plunger");
        clickPlunger(helper, pos);
        helper.assertTrue(
                box.inputAmount() == 0,
                "Plunger did not trash the whole gas-box fuel tank");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void boilerPlungerTrashesWaterThenSteam(
            GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(
                pos,
                ModBlocks.BRONZE_BOILER.get().defaultBlockState()
                        .setValue(com.masson.cruciblecraft.content.block
                                .BoilerBlock.FACING, Direction.EAST));
        BoilerBlockEntity boiler = helper.getBlockEntity(pos);
        helper.assertTrue(
                boiler.fluids(Direction.NORTH).fill(
                                new FluidStack(Fluids.WATER, 2_000),
                                IFluidHandler.FluidAction.EXECUTE)
                        == 2_000
                        && boiler.fillSteam(2_000),
                "Could not fill boiler water and steam for the plunger");
        GameTestPlunger.click(helper, pos);
        helper.assertTrue(
                boiler.waterAmount() == 0 && boiler.steamAmount() == 2_000,
                "Plunger did not trash boiler water first");
        GameTestPlunger.click(helper, pos);
        helper.assertTrue(
                boiler.waterAmount() == 0 && boiler.steamAmount() == 0,
                "Second plunger click did not trash boiler steam");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void largeBoilerPlungerTrashesWaterThenSteam(
            GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(
                pos,
                ModBlocks.LARGE_BOILER.get().defaultBlockState()
                        .setValue(ProcessingMachineBlock.FACING, Direction.EAST));
        LargeBoilerBlockEntity boiler = helper.getBlockEntity(pos);
        helper.assertTrue(
                boiler.waterTank().fill(
                                new FluidStack(Fluids.WATER, 2_000),
                                IFluidHandler.FluidAction.EXECUTE)
                        == 2_000
                        && boiler.steamTank().fill(
                                new FluidStack(
                                        ModFluids.STEAM_SOURCE.get(), 2_000),
                                IFluidHandler.FluidAction.EXECUTE)
                        == 2_000,
                "Could not fill large-boiler tanks for the plunger");
        GameTestPlunger.click(helper, pos);
        helper.assertTrue(
                boiler.waterTank().getFluidAmount() == 0
                        && boiler.steamTank().getFluidAmount() == 2_000,
                "Plunger did not trash large-boiler water first");
        GameTestPlunger.click(helper, pos);
        helper.assertTrue(
                boiler.waterTank().isEmpty() && boiler.steamTank().isEmpty(),
                "Second plunger click did not trash large-boiler steam");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void steamTurbinePlungerTrashesSteamTank(
            GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        MteInPlaceBlock turbine = ModBlocks.mteInPlaceBlocksById()
                .get(ResourceLocation.fromNamespaceAndPath(
                        "cruciblecraft", "steam/turbine_bronze"))
                .get();
        helper.setBlock(
                pos,
                turbine.defaultBlockState()
                        .setValue(MteInPlaceBlock.FACING, Direction.EAST));
        MteInPlaceBlockEntity be = helper.getBlockEntity(pos);
        int steam = Math.min(2_000, be.tank().getCapacity());
        helper.assertTrue(
                be.spec().kind() == MteInPlaceKind.STEAM_TURBINE
                        && steam > 0
                        && be.tank().fill(
                                new FluidStack(
                                        ModFluids.STEAM_SOURCE.get(), steam),
                                IFluidHandler.FluidAction.EXECUTE)
                        == steam,
                "Could not fill bronze steam turbine for the plunger");
        GameTestPlunger.click(helper, pos);
        helper.assertTrue(
                be.tank().isEmpty(),
                "Plunger did not trash the steam-turbine steam tank");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void largeSteamTurbinePlungerTrashesSteamThenDistilled(
            GameTestHelper helper) {
        SteamTurbineCatalog.Profile large = SteamTurbineCatalog.profiles()
                .stream()
                .filter(SteamTurbineCatalog.Profile::large)
                .findFirst()
                .orElseThrow();
        BlockPos pos = new BlockPos(2, 2, 3);
        MteInPlaceBlock turbine = ModBlocks.mteInPlaceBlocksById()
                .get(large.id())
                .get();
        helper.setBlock(
                pos,
                turbine.defaultBlockState()
                        .setValue(MteInPlaceBlock.FACING, Direction.EAST));
        MteInPlaceBlockEntity be = helper.getBlockEntity(pos);
        helper.assertTrue(
                be.tank().fill(
                                new FluidStack(
                                        ModFluids.STEAM_SOURCE.get(), 2_000),
                                IFluidHandler.FluidAction.EXECUTE)
                        == 2_000
                        && be.distilledTank().fill(
                                new FluidStack(
                                        ModFluids.chemical("water_distilled")
                                                .orElseThrow()
                                                .source()
                                                .get(),
                                        2_000),
                                IFluidHandler.FluidAction.EXECUTE)
                        == 2_000,
                "Could not fill large steam turbine tanks for the plunger");
        GameTestPlunger.click(helper, pos);
        helper.assertTrue(
                be.tank().isEmpty()
                        && be.distilledTank().getFluidAmount() == 2_000,
                "Plunger did not trash large-turbine steam first");
        GameTestPlunger.click(helper, pos);
        helper.assertTrue(
                be.tank().isEmpty() && be.distilledTank().isEmpty(),
                "Second plunger click did not trash large-turbine DistW");
        helper.succeed();
    }

    private static void clickPlunger(GameTestHelper helper, BlockPos pos) {
        GameTestPlunger.click(helper, pos);
    }

    private static PipeCover redstoneController() {
        return PipeCover.of("cruciblecraft:controller_redstone");
    }

    private static GTRecipe requireFuelOil() {
        return ModRecipeMaps.FUELS_ENGINE.entry(
                        ResourceLocation.parse(
                                "cruciblecraft:hydrocarbon/fuels_engine/fuel_oil"))
                .map(RecipeMap.Entry::recipe)
                .orElseThrow(() -> new IllegalStateException(
                        "Missing live recipe hydrocarbon/fuels_engine/fuel_oil"));
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
