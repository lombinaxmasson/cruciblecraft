package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.content.block.DistillationTowerParts;
import com.masson.cruciblecraft.content.block.LargeFermenterBlock;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.block.StainlessSteelMixerWalls;
import com.masson.cruciblecraft.content.blockentity.LargeFermenterAutoOutput;
import com.masson.cruciblecraft.content.blockentity.LargeFermenterBlockEntity;
import com.masson.cruciblecraft.content.blockentity.LargeFermenterPartVisuals;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureCatalog;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.Offset;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PredicateKind;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PortType;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.registry.ModMultiblockControllers;
import com.masson.cruciblecraft.registry.ModRecipeMaps;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Isolated GT6 Large Fermenter 17113 gate. */
@GameTestHolder(LargeFermenterGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class LargeFermenterGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_machines_large_fermenter";
    private static final String TEMPLATE = "empty";
    private static final BlockPos CONTROLLER = new BlockPos(4, 2, 2);
    private static final Direction FACING = Direction.NORTH;

    private LargeFermenterGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void controllerIsLive(GameTestHelper helper) {
        helper.assertTrue(
                ModBlocks.LARGE_FERMENTER.get() != null,
                "Large fermenter block is missing");
        helper.assertTrue(
                DistillationTowerParts.heatTransmitter() != null,
                "Heat transmitter 18101 is missing");
        helper.assertTrue(
                StainlessSteelMixerWalls.wall() != null,
                "Stainless steel wall 18002 is missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void bareControllerIsNotFormed(GameTestHelper helper) {
        helper.setBlock(
                CONTROLLER,
                ModBlocks.LARGE_FERMENTER.get()
                        .defaultBlockState()
                        .setValue(ProcessingMachineBlock.FACING, FACING));
        LargeFermenterBlockEntity be = helper.getBlockEntity(CONTROLLER);
        helper.assertTrue(be != null, "Missing large fermenter");
        LargeFermenterBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(CONTROLLER),
                helper.getBlockState(CONTROLLER),
                be);
        helper.assertTrue(
                !be.structureValid(),
                "Bare large fermenter reported a formed structure");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void formedStructureBindsWallsAndHeat(
            GameTestHelper helper) {
        LargeFermenterBlockEntity be = placeFormed(helper);
        helper.assertTrue(be.structureValid(), "5x5x3 fermenter did not form");
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.LARGE_FERMENTER.structureId());
        helper.assertTrue(
                structure.portCount(PortType.ITEM_FLUID) == 49,
                "Bidirectional wall ports drifted");
        helper.assertTrue(
                structure.portCount(PortType.ENERGY_INPUT) == 25,
                "Heat-transmitter ports drifted");
        assertDesignHole(helper, true);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void biomassRunUsesHeatAndKeepsCircuit(
            GameTestHelper helper) {
        LargeFermenterBlockEntity be = placeFormed(helper);
        GTRecipe recipe = ModRecipeMaps.FERMENTER.entries().stream()
                .filter(entry -> entry.id().getPath().equals(
                        "fermenter/biomass_to_methane"))
                .map(entry -> entry.recipe())
                .findFirst()
                .orElseThrow();
        helper.assertTrue(
                recipe.itemInputCounts().getFirst() == 0,
                "Biomass row must preserve GT6 circuit 0");
        for (int i = 0; i < recipe.itemInputs().size(); i++) {
            ItemStack sample = recipe.itemInputs().get(i).getItems()[0].copy();
            sample.setCount(Math.max(1, recipe.itemInputCounts().get(i)));
            be.inventory().setStackInSlot(
                    be.spec().items().inputs().get(i), sample);
        }
        for (int i = 0; i < recipe.fluidInputs().size(); i++) {
            be.tanks().get(be.spec().fluids().inputs().get(i).index())
                    .setFluid(recipe.fluidInputs().get(i).copy());
        }
        helper.assertTrue(
                be.insertFromMultiblockPort(
                        EnergyType.HEAT, 16L, 4_096L, false) > 0L,
                "Fermenter rejected port heat");
        for (int tick = 0; tick < 40; tick++) {
            LargeFermenterBlockEntity.serverTick(
                    helper.getLevel(),
                    helper.absolutePos(CONTROLLER),
                    helper.getBlockState(CONTROLLER),
                    be);
        }
        FluidStack methane = recipe.fluidOutputs().getFirst();
        helper.assertTrue(
                be.tanks().stream().anyMatch(tank ->
                        tank.getFluid().is(methane.getFluid())
                                && tank.getFluidAmount() >= methane.getAmount()),
                "Biomass did not yield methane");
        helper.assertTrue(
                be.getBlockState().getValue(LargeFermenterBlock.LIT),
                "Powered fermenter stayed on the idle overlay");
        helper.assertTrue(
                LargeFermenterAutoOutput.destinationSide(FACING)
                        == Direction.SOUTH,
                "Auto-out queried the near face");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void survivalRecipeIsPresent(GameTestHelper helper) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft", "machines/large_fermenter");
        boolean found = helper.getLevel()
                .getRecipeManager()
                .getAllRecipesFor(RecipeType.CRAFTING)
                .stream()
                .map(RecipeHolder::id)
                .anyMatch(id::equals);
        helper.assertTrue(found, "Large fermenter survival recipe is missing");
        helper.assertTrue(
                ModItems.LARGE_FERMENTER.get() != null,
                "Large fermenter item is missing");
        helper.succeed();
    }

    private static LargeFermenterBlockEntity placeFormed(
            GameTestHelper helper) {
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.LARGE_FERMENTER.structureId());
        helper.setBlock(
                CONTROLLER,
                ModBlocks.LARGE_FERMENTER.get()
                        .defaultBlockState()
                        .setValue(ProcessingMachineBlock.FACING, FACING));
        structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.PORT)
                .forEach(element -> {
                    var predicate = structure.predicate(element);
                    helper.setBlock(
                            structure.worldPosition(
                                    CONTROLLER, FACING, element.offset()),
                            BuiltInRegistries.BLOCK.get(
                                    predicate.block().orElseThrow()));
                });
        LargeFermenterBlockEntity be = helper.getBlockEntity(CONTROLLER);
        helper.assertTrue(be != null, "Missing large fermenter");
        LargeFermenterBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(CONTROLLER),
                helper.getBlockState(CONTROLLER),
                be);
        return be;
    }

    private static void assertDesignHole(
            GameTestHelper helper, boolean formed) {
        BlockPos lower = structureOffset(LargeFermenterPartVisuals.LOWER_BACK);
        BlockPos upper = structureOffset(LargeFermenterPartVisuals.UPPER_BACK);
        BlockPos side = structureOffset(new Offset(-2, 0, 0));
        helper.assertTrue(
                helper.getBlockState(lower).getValue(
                        StainlessSteelMixerWalls.DESIGN_HOLE) == formed,
                "Lower back wall design 7 drifted");
        helper.assertTrue(
                helper.getBlockState(upper).getValue(
                        StainlessSteelMixerWalls.DESIGN_HOLE) == formed,
                "Upper back wall design 7 drifted");
        helper.assertTrue(
                !helper.getBlockState(side).getValue(
                        StainlessSteelMixerWalls.DESIGN_HOLE),
                "Side wall picked up design 7");
    }

    private static BlockPos structureOffset(Offset offset) {
        return MultiblockStructureCatalog.require(
                ModMultiblockControllers.LARGE_FERMENTER.structureId())
                .worldPosition(CONTROLLER, FACING, offset);
    }
}
