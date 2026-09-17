package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.FluidBedBurningBoxBlock;
import com.masson.cruciblecraft.content.block.FuelGeneratorBlock;
import com.masson.cruciblecraft.content.blockentity.FluidBedBurningBoxBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FuelGeneratorBlockEntity;
import com.masson.cruciblecraft.energy.converter.BurningBoxWorldEffects;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModFluids;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
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
                box.insertFuel(new ItemStack(
                        MaterialLookup.item(
                                        "peat", MaterialPrefixes.DUST)
                                .orElseThrow())),
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
                box.insertFuel(new ItemStack(
                        MaterialLookup.item(
                                        "peat", MaterialPrefixes.STORAGE_DUST)
                                .orElseThrow())),
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
                box.insertFuel(new ItemStack(
                        MaterialLookup.item(
                                        "petroleum_coke",
                                        MaterialPrefixes.DUST_DIV72)
                                .orElseThrow())),
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
