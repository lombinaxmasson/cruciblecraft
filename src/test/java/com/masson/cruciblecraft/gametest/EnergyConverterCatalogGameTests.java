package com.masson.cruciblecraft.gametest;

import java.util.List;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.FuelGeneratorBlock;
import com.masson.cruciblecraft.content.block.FluidPipeBlock;
import com.masson.cruciblecraft.content.block.SolidBurningBoxBlock;
import com.masson.cruciblecraft.content.block.SteamEngineBlock;
import com.masson.cruciblecraft.content.blockentity.BoilerBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FuelGeneratorBlockEntity;
import com.masson.cruciblecraft.content.blockentity.SolidBurningBoxBlockEntity;
import com.masson.cruciblecraft.content.blockentity.SteamEngineBlockEntity;
import com.masson.cruciblecraft.energy.converter.EnergyConverterCatalog;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
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
                        && (engine.exhaustAmount() == 0
                                || SteamConversion.isDistilledWater(
                                        engine.exhaustFluid())),
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
    public static void steamEngineKeepsDistWWhenExhaustIsBlocked(
            GameTestHelper helper) {
        BlockPos enginePos = new BlockPos(3, 1, 3);
        helper.setBlock(
                enginePos,
                ModBlocks.BRONZE_STEAM_ENGINE.get().defaultBlockState()
                        .setValue(SteamEngineBlock.FACING, Direction.EAST));
        SteamEngineBlockEntity engine = helper.getBlockEntity(enginePos);
        var registries = helper.getLevel().registryAccess();
        CompoundTag tag = engine.saveWithoutMetadata(registries);
        FluidTank full = new FluidTank(SteamEngineBlockEntity.EXHAUST_CAPACITY);
        full.setFluid(SteamConversion.distilledExhaust(
                SteamEngineBlockEntity.EXHAUST_CAPACITY));
        tag.put("exhaust", full.writeToNBT(registries, new CompoundTag()));
        engine.loadWithComponents(tag, registries);
        IFluidHandler steamIn = engine.fluids(Direction.WEST);
        helper.assertTrue(
                steamIn.fill(
                        new FluidStack(
                                ModFluids.STEAM_SOURCE.get(),
                                SteamConversion.ENGINE_STEAM_PER_BATCH),
                        IFluidHandler.FluidAction.EXECUTE)
                        == SteamConversion.ENGINE_STEAM_PER_BATCH,
                "Could not fill steam into a DistW-full engine");
        SteamEngineBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(enginePos),
                helper.getBlockState(enginePos),
                engine);
        helper.assertTrue(
                engine.steamAmount() == SteamConversion.ENGINE_STEAM_PER_BATCH
                        && engine.stored() == 0L
                        && engine.exhaustAmount()
                                == SteamEngineBlockEntity.EXHAUST_CAPACITY
                        && "exhaust_full".equals(engine.status()),
                "Full DistW tank consumed steam or voided exhaust");
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
    public static void fuelEngineBuffersCarbonDioxideWithoutNeighbor(
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
                        engine.outputAmount(0)
                                == fuel.fluidOutputs().getFirst().getAmount(),
                        "Isolated fuel engine voided recipe CO2 instead of buffering"))
                .thenSucceed();
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
