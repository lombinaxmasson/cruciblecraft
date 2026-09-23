package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.blockentity.LargeSqueezerBlockEntity;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureCatalog;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PredicateKind;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PortType;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModMultiblockControllers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Source-contract tests for GT6 Large Squeezer 17114. */
@GameTestHolder(CrucibleCraftGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class LargeSqueezerGameTests {
    private static final String TEMPLATE = "empty";
    private static final BlockPos CONTROLLER = new BlockPos(6, 2, 6);

    private LargeSqueezerGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void controllerIsLive(GameTestHelper helper) {
        helper.assertTrue(ModBlocks.LARGE_SQUEEZER.get() != null,
                "Large Squeezer block is missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void bareControllerIsNotFormed(GameTestHelper helper) {
        helper.setBlock(CONTROLLER, ModBlocks.LARGE_SQUEEZER.get()
                .defaultBlockState()
                .setValue(ProcessingMachineBlock.FACING, Direction.NORTH));
        LargeSqueezerBlockEntity be = helper.getBlockEntity(CONTROLLER);
        helper.assertTrue(be != null, "Missing Large Squeezer block entity");
        LargeSqueezerBlockEntity.serverTick(
                helper.getLevel(), helper.absolutePos(CONTROLLER),
                helper.getBlockState(CONTROLLER), be);
        helper.assertTrue(!be.structureValid(),
                "Bare Large Squeezer reported a formed structure");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void structureContractHasSourceExactCounts(
            GameTestHelper helper) {
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.LARGE_SQUEEZER.structureId());
        helper.assertTrue(structure.structure().size() == 75,
                "Large Squeezer cell count drifted");
        helper.assertTrue(structure.portCount(PortType.ITEM_FLUID_IN) == 25,
                "Large Squeezer input count drifted");
        helper.assertTrue(structure.portCount(PortType.ITEM_FLUID_OUT) == 24,
                "Large Squeezer output count drifted");
        helper.assertTrue(structure.portCount(PortType.ENERGY_INPUT) == 2,
                "Large Squeezer RU count drifted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void controllerUsesGt6RuProcessingContract(
            GameTestHelper helper) {
        var variant = ModMultiblockControllers.LARGE_SQUEEZER.requireVariant();
        helper.assertTrue(
                variant.tierBand().energyType()
                        == com.masson.cruciblecraft.api.energy.EnergyType.KINETIC_ROTATION,
                "Large Squeezer is not a RU host");
        helper.assertTrue(variant.tierBand().inputMinimum() == 512L,
                "Large Squeezer input minimum drifted");
        helper.assertTrue(variant.tierBand().inputMaximum() == 4096L,
                "Large Squeezer input maximum drifted");
        helper.assertTrue(variant.tierBand().parallelLimit() == 64,
                "Large Squeezer parallel limit drifted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void formedStructureBindsSteelWallPorts(
            GameTestHelper helper) {
        LargeSqueezerBlockEntity be = placeFormed(helper);
        helper.assertTrue(be.structureValid(),
                "Large Squeezer did not form from the source structure");
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.LARGE_SQUEEZER.structureId());
        long bound = structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.PORT)
                .map(element -> structure.worldPosition(
                        CONTROLLER, Direction.NORTH, element.offset()))
                .filter(pos -> helper.getBlockEntity(pos)
                        instanceof MteInPlaceBlockEntity part
                        && part.mixerControllerPosition().isPresent()
                        && part.portStore().configured())
                .count();
        helper.assertTrue(bound == 51,
                "Large Squeezer Steel Wall ports were not all bound: " + bound);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void survivalRecipeIsPresent(GameTestHelper helper) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft", "machines/large_squeezer");
        boolean found = helper.getLevel().getRecipeManager()
                .getAllRecipesFor(RecipeType.CRAFTING)
                .stream()
                .map(RecipeHolder::id)
                .anyMatch(id::equals);
        helper.assertTrue(found, "Large Squeezer survival recipe is missing");
        helper.succeed();
    }

    private static LargeSqueezerBlockEntity placeFormed(
            GameTestHelper helper) {
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.LARGE_SQUEEZER.structureId());
        helper.setBlock(
                CONTROLLER,
                ModBlocks.LARGE_SQUEEZER.get().defaultBlockState()
                        .setValue(ProcessingMachineBlock.FACING, Direction.NORTH));
        structure.structure().stream()
                .filter(element -> {
                    var kind = structure.predicate(element).kind();
                    return kind == PredicateKind.PORT
                            || kind == PredicateKind.BLOCK;
                })
                .forEach(element -> {
                    var predicate = structure.predicate(element);
                    helper.setBlock(
                            structure.worldPosition(
                                    CONTROLLER, Direction.NORTH, element.offset()),
                            BuiltInRegistries.BLOCK.getOptional(
                                    predicate.block().orElseThrow()).orElseThrow());
                });
        LargeSqueezerBlockEntity be = helper.getBlockEntity(CONTROLLER);
        helper.assertTrue(be != null, "Missing Large Squeezer");
        LargeSqueezerBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(CONTROLLER),
                helper.getBlockState(CONTROLLER),
                be);
        return be;
    }
}
