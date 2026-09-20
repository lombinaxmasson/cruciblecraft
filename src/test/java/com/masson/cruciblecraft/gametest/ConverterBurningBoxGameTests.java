package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.compat.jade.observation.ConverterObservation;
import com.masson.cruciblecraft.content.block.BoilerBlock;
import com.masson.cruciblecraft.content.block.FluidBedBurningBoxBlock;
import com.masson.cruciblecraft.content.block.FuelGeneratorBlock;
import com.masson.cruciblecraft.content.block.SolidBurningBoxBlock;
import com.masson.cruciblecraft.content.blockentity.BoilerBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FluidBedBurningBoxBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FuelGeneratorBlockEntity;
import com.masson.cruciblecraft.content.blockentity.SolidBurningBoxBlockEntity;
import com.masson.cruciblecraft.energy.converter.BurningBoxWorldEffects;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModFluids;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Molten-calcite obtain and GT6 {@code FL.gas} fill split. Runs on
 * {@link CrucibleCraftGameTests#NAMESPACE}.
 */
@GameTestHolder(CrucibleCraftGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class ConverterBurningBoxGameTests {
    private static final String TEMPLATE = "empty";

    private ConverterBurningBoxGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fluidCellAcceptsMoltenCalcite(GameTestHelper helper) {
        ItemStack fluidCell = new ItemStack(ModItems.FLUID_CELL.get());
        var fluidHandler =
                fluidCell.getCapability(Capabilities.FluidHandler.ITEM);
        ItemStack gasCell = new ItemStack(ModItems.GAS_CELL.get());
        var gasHandler = gasCell.getCapability(Capabilities.FluidHandler.ITEM);
        FluidStack calcite = moltenCalcite(1_000);
        helper.assertTrue(
                fluidHandler != null && gasHandler != null,
                "Generic cell item capabilities are missing");
        helper.assertTrue(
                fluidHandler.fill(
                        calcite, IFluidHandler.FluidAction.EXECUTE)
                        == 1_000,
                "Fluid cell rejected molten calcite");
        helper.assertTrue(
                gasHandler.fill(
                        calcite, IFluidHandler.FluidAction.SIMULATE)
                        == 0,
                "Gas cell accepted molten calcite");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fluidBedBurnsPeatWithMoltenCalcite(
            GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(
                pos,
                ModBlocks.BRONZE_BURNING_BOX_FLUID_BED.get()
                        .defaultBlockState()
                        .setValue(
                                FluidBedBurningBoxBlock.FACING,
                                Direction.EAST));
        FluidBedBurningBoxBlockEntity box = helper.getBlockEntity(pos);
        helper.assertTrue(
                box.fluids(Direction.WEST).fill(
                        moltenCalcite(72),
                        IFluidHandler.FluidAction.EXECUTE)
                        == 72,
                "Fluid-bed tank rejected molten calcite");
        helper.assertTrue(
                box.insertFuel(MaterialLookup.stack("peat", MaterialPrefixes.DUST)),
                "Fluid-bed fuel slot rejected peat dust");
        box.ignite();
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        box.energyStored() > 0L,
                        "Peat + molten calcite did not produce HU"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fluidBedBurnsPeatStorageDust(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(
                pos,
                ModBlocks.BRONZE_BURNING_BOX_FLUID_BED.get()
                        .defaultBlockState()
                        .setValue(
                                FluidBedBurningBoxBlock.FACING,
                                Direction.EAST));
        FluidBedBurningBoxBlockEntity box = helper.getBlockEntity(pos);
        helper.assertTrue(
                box.fluids(Direction.WEST).fill(
                        moltenCalcite(648),
                        IFluidHandler.FluidAction.EXECUTE)
                        == 648,
                "Fluid-bed tank rejected molten calcite");
        helper.assertTrue(
                box.insertFuel(MaterialLookup.stack(
                        "peat", MaterialPrefixes.STORAGE_DUST)),
                "Fluid-bed fuel slot rejected peat dust block");
        box.ignite();
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        box.energyStored() > 0L,
                        "Peat dust block + molten calcite did not produce HU"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fluidBedBurnsOutputlessDiv72(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(
                pos,
                ModBlocks.BRONZE_BURNING_BOX_FLUID_BED.get()
                        .defaultBlockState()
                        .setValue(
                                FluidBedBurningBoxBlock.FACING,
                                Direction.EAST));
        FluidBedBurningBoxBlockEntity box = helper.getBlockEntity(pos);
        helper.assertTrue(
                box.fluids(Direction.WEST).fill(
                        moltenCalcite(1),
                        IFluidHandler.FluidAction.EXECUTE)
                        == 1,
                "Fluid-bed tank rejected molten calcite");
        helper.assertTrue(
                box.insertFuel(MaterialLookup.stack(
                        "petroleum_coke",
                        MaterialPrefixes.DUST_DIV72)),
                "Fluid-bed fuel slot rejected petroleum coke 1/72 dust");
        box.ignite();
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        box.energyStored() > 0L,
                        "Outputless 1/72 dust fuel did not produce HU"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void gasBoxCatalogFacesAcceptNaturalGasAndShowAmount(
            GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(
                pos,
                ModBlocks.BRONZE_BURNING_BOX_GAS.get()
                        .defaultBlockState()
                        .setValue(FuelGeneratorBlock.FACING, Direction.EAST));
        FuelGeneratorBlockEntity gas = helper.getBlockEntity(pos);
        FluidStack naturalGas = chemical("natural_gas", 40);
        IFluidHandler north = gas.fluids(Direction.NORTH);
        helper.assertTrue(
                north != null
                        && north.fill(
                                naturalGas, IFluidHandler.FluidAction.EXECUTE)
                                == 40
                        && gas.inputAmount() == 40,
                "Catalog north face rejected natural gas: filled="
                        + (north == null
                                ? "null"
                                : gas.inputAmount()));
        helper.assertTrue(
                north.drain(1_000, IFluidHandler.FluidAction.EXECUTE)
                        .isEmpty()
                        && gas.inputAmount() == 40,
                "North-face drain stole fuel instead of exhaust");
        CompoundTag data = new CompoundTag();
        ConverterObservation.writeServerData(data, gas);
        ConverterObservation observation =
                ConverterObservation.fromServerData(data);
        helper.assertTrue(
                !observation.tanks().isEmpty()
                        && observation.tanks().getFirst().amount() == 40
                        && observation.tanks().getFirst().fluidId()
                                .contains("natural_gas")
                        && observation.tanks().getFirst().capacity()
                                == gas.inputCapacity(),
                "Jade did not report natural-gas input amount: "
                        + observation.tanks());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void gasAndLiquidBoxesSplitFmBurnPhase(
            GameTestHelper helper) {
        BlockPos gasPos = new BlockPos(1, 1, 1);
        BlockPos liquidPos = new BlockPos(3, 1, 1);
        helper.setBlock(
                gasPos,
                ModBlocks.BRONZE_BURNING_BOX_GAS.get()
                        .defaultBlockState()
                        .setValue(FuelGeneratorBlock.FACING, Direction.EAST));
        helper.setBlock(
                liquidPos,
                ModBlocks.BRONZE_BURNING_BOX_LIQUID.get()
                        .defaultBlockState()
                        .setValue(FuelGeneratorBlock.FACING, Direction.EAST));
        FuelGeneratorBlockEntity gas = helper.getBlockEntity(gasPos);
        FuelGeneratorBlockEntity liquid = helper.getBlockEntity(liquidPos);
        FluidStack methane = chemical("methane", 16);
        FluidStack diesel = chemical("diesel", 16);
        helper.assertTrue(
                fill(gas, methane, IFluidHandler.FluidAction.SIMULATE) == 16,
                "Gas burning box rejected methane");
        helper.assertTrue(
                fill(gas, diesel, IFluidHandler.FluidAction.SIMULATE) == 0,
                "Gas burning box accepted diesel");
        helper.assertTrue(
                fill(liquid, diesel, IFluidHandler.FluidAction.SIMULATE) == 16,
                "Liquid burning box rejected diesel");
        helper.assertTrue(
                fill(liquid, methane, IFluidHandler.FluidAction.SIMULATE) == 0,
                "Liquid burning box accepted methane");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void gasBoxNeedsFrontIgnition(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(
                pos,
                ModBlocks.BRONZE_BURNING_BOX_GAS.get()
                        .defaultBlockState()
                        .setValue(FuelGeneratorBlock.FACING, Direction.EAST));
        FuelGeneratorBlockEntity gas = helper.getBlockEntity(pos);
        helper.assertTrue(
                fill(
                        gas,
                        chemical("methane", 16),
                        IFluidHandler.FluidAction.EXECUTE)
                        == 16,
                "Gas burning box rejected methane");
        helper.startSequence()
                .thenIdle(8)
                .thenExecute(() -> helper.assertTrue(
                        gas.energyGenerated() == 0L && !gas.burning(),
                        "Gas box burned without ignition"))
                .thenExecute(gas::ignite)
                .thenIdle(8)
                .thenExecute(() -> helper.assertTrue(
                        gas.energyGenerated() > 0L,
                        "Ignited gas box produced no HU"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void burningBoxBurnsFlammableFront(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        BlockPos front = pos.east();
        helper.setBlock(pos.below(), Blocks.NETHERRACK);
        helper.setBlock(front.below(), Blocks.NETHERRACK);
        helper.setBlock(
                pos,
                ModBlocks.BRONZE_BURNING_BOX_GAS.get()
                        .defaultBlockState()
                        .setValue(FuelGeneratorBlock.FACING, Direction.EAST));
        helper.setBlock(front, Blocks.WHITE_WOOL);
        FuelGeneratorBlockEntity gas = helper.getBlockEntity(pos);
        gas.ignite();
        BurningBoxWorldEffects.burnFront(
                helper.getLevel(), helper.absolutePos(front));
        helper.assertTrue(
                helper.getBlockState(front.above()).is(Blocks.FIRE)
                        || helper.getBlockState(front).is(Blocks.FIRE),
                "Front wool did not catch fire");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void steelBoilerWaterFillStopsAtGt6Capacity(
            GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(
                pos,
                ModBlocks.converterBlocksById()
                        .get(ResourceLocation.fromNamespaceAndPath(
                                "cruciblecraft", "steel_boiler"))
                        .get()
                        .defaultBlockState()
                        .setValue(BoilerBlock.FACING, Direction.EAST));
        BoilerBlockEntity boiler = helper.getBlockEntity(pos);
        IFluidHandler water = boiler.fluids(Direction.NORTH);
        helper.assertTrue(
                boiler.waterCapacity() == 4_000,
                "Steel boiler water tank is not GT6 4000 L: "
                        + boiler.waterCapacity());
        helper.assertTrue(
                water != null
                        && water.fill(
                                new FluidStack(Fluids.WATER, 16_000),
                                IFluidHandler.FluidAction.EXECUTE)
                                == 4_000
                        && boiler.waterAmount() == 4_000,
                "Steel boiler accepted water past capacity: "
                        + boiler.waterAmount());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void gasBoxFrontBlockStopsFuel(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(
                pos,
                ModBlocks.BRONZE_BURNING_BOX_GAS.get()
                        .defaultBlockState()
                        .setValue(FuelGeneratorBlock.FACING, Direction.EAST));
        helper.setBlock(pos.east(), Blocks.STONE);
        FuelGeneratorBlockEntity gas = helper.getBlockEntity(pos);
        helper.assertTrue(
                fill(
                        gas,
                        chemical("methane", 16),
                        IFluidHandler.FluidAction.EXECUTE)
                        == 16,
                "Gas burning box rejected methane");
        gas.ignite();
        helper.startSequence()
                .thenIdle(8)
                .thenExecute(() -> helper.assertTrue(
                        gas.energyGenerated() == 0L
                                && !gas.burning()
                                && gas.inputAmount() == 16,
                        "Blocked-front gas box kept burning: generated="
                                + gas.energyGenerated()
                                + " burning="
                                + gas.burning()
                                + " fuel="
                                + gas.inputAmount()))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void gasBoxIgniteWithoutFuelProducesNoHu(
            GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(
                pos,
                ModBlocks.BRONZE_BURNING_BOX_GAS.get()
                        .defaultBlockState()
                        .setValue(FuelGeneratorBlock.FACING, Direction.EAST));
        FuelGeneratorBlockEntity gas = helper.getBlockEntity(pos);
        gas.ignite();
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        !gas.burning() && gas.energyGenerated() == 0L,
                        "Empty gas box stayed lit or emitted HU: burning="
                                + gas.burning()
                                + " generated="
                                + gas.energyGenerated()))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void solidBoxFrontBlockStopsFuel(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(
                pos,
                ModBlocks.BRONZE_BURNING_BOX_SOLID.get()
                        .defaultBlockState()
                        .setValue(SolidBurningBoxBlock.FACING, Direction.EAST));
        helper.setBlock(pos.east(), Blocks.STONE);
        SolidBurningBoxBlockEntity box = helper.getBlockEntity(pos);
        helper.assertTrue(
                box.insertFuel(new ItemStack(Items.COAL)),
                "Solid burning box rejected coal");
        box.ignite();
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        box.energyStored() == 0L,
                        "Blocked-front solid box consumed fuel: HU="
                                + box.energyStored()))
                .thenSucceed();
    }

    private static int fill(
            FuelGeneratorBlockEntity generator,
            FluidStack stack,
            IFluidHandler.FluidAction action) {
        IFluidHandler handler = generator.fluids(Direction.WEST);
        return handler == null ? 0 : handler.fill(stack, action);
    }

    private static FluidStack moltenCalcite(int amount) {
        return new FluidStack(
                ModFluids.molten("calcite").orElseThrow().source().get(),
                amount);
    }

    private static FluidStack chemical(String material, int amount) {
        return new FluidStack(
                ModFluids.materialFluid(material).orElseThrow(), amount);
    }
}
