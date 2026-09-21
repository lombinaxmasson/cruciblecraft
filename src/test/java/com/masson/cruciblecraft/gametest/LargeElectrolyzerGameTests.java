package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.content.block.ElectrolyzerParts;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.blockentity.LargeElectrolyzerBlockEntity;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureCatalog;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PredicateKind;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PortType;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.registry.ModMultiblockControllers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Isolated GT6 Large Electrolyzer 17103 gate. */
@GameTestHolder(LargeElectrolyzerGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class LargeElectrolyzerGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_machines_large_electrolyzer";
    private static final String TEMPLATE = "empty";
    private static final BlockPos CONTROLLER = new BlockPos(2, 2, 2);
    private static final Direction FACING = Direction.NORTH;

    private LargeElectrolyzerGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void controllerIsLive(GameTestHelper helper) {
        helper.assertTrue(
                ModBlocks.LARGE_ELECTROLYZER.get() != null,
                "Large electrolyzer block is missing");
        helper.assertTrue(
                ElectrolyzerParts.part() != null,
                "Electrolyzer part 18105 is missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void bareControllerIsNotFormed(GameTestHelper helper) {
        helper.setBlock(
                CONTROLLER,
                ModBlocks.LARGE_ELECTROLYZER.get()
                        .defaultBlockState()
                        .setValue(ProcessingMachineBlock.FACING, FACING));
        LargeElectrolyzerBlockEntity be = helper.getBlockEntity(CONTROLLER);
        helper.assertTrue(be != null, "Missing large electrolyzer");
        LargeElectrolyzerBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(CONTROLLER),
                helper.getBlockState(CONTROLLER),
                be);
        helper.assertTrue(
                !be.structureValid(),
                "Bare large electrolyzer reported a formed structure");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void formedStructureBindsBottomInAndTopOut(
            GameTestHelper helper) {
        LargeElectrolyzerBlockEntity be = placeFormed(helper);
        helper.assertTrue(be.structureValid(), "3x3x2 electrolyzer did not form");
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.LARGE_ELECTROLYZER.structureId());
        helper.assertTrue(
                structure.portCount(PortType.ITEM_FLUID_IN) == 8,
                "Bottom in ports drifted");
        helper.assertTrue(
                structure.portCount(PortType.ITEM_FLUID_OUT) == 9,
                "Top out ports drifted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void survivalRecipeIsPresent(GameTestHelper helper) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft", "machines/large_electrolyzer");
        boolean found = helper.getLevel()
                .getRecipeManager()
                .getAllRecipesFor(RecipeType.CRAFTING)
                .stream()
                .map(RecipeHolder::id)
                .anyMatch(id::equals);
        helper.assertTrue(found, "Large electrolyzer survival recipe is missing");
        helper.assertTrue(
                ModItems.LARGE_ELECTROLYZER.get() != null,
                "Large electrolyzer item is missing");
        helper.succeed();
    }

    private static LargeElectrolyzerBlockEntity placeFormed(
            GameTestHelper helper) {
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.LARGE_ELECTROLYZER.structureId());
        helper.setBlock(
                CONTROLLER,
                ModBlocks.LARGE_ELECTROLYZER.get()
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
        LargeElectrolyzerBlockEntity be = helper.getBlockEntity(CONTROLLER);
        helper.assertTrue(be != null, "Missing large electrolyzer");
        LargeElectrolyzerBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(CONTROLLER),
                helper.getBlockState(CONTROLLER),
                be);
        return be;
    }
}
