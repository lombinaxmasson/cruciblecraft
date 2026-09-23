package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.block.SluiceParts;
import com.masson.cruciblecraft.content.blockentity.LargeSluiceBlockEntity;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
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

/** GT6 Large Sluice 17107. */
@GameTestHolder(CrucibleCraftGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class LargeSluiceGameTests {
    private static final String TEMPLATE = "empty";
    private static final BlockPos CONTROLLER = new BlockPos(6, 2, 6);
    private static final Direction FACING = Direction.NORTH;

    private LargeSluiceGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void controllerIsLive(GameTestHelper helper) {
        helper.assertTrue(
                ModBlocks.LARGE_SLUICE.get() != null,
                "Large Sluice block is missing");
        helper.assertTrue(
                SluiceParts.part() != null,
                "Sluice Part 18106 is missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void bareControllerIsNotFormed(GameTestHelper helper) {
        helper.setBlock(
                CONTROLLER,
                ModBlocks.LARGE_SLUICE.get()
                        .defaultBlockState()
                        .setValue(ProcessingMachineBlock.FACING, FACING));
        LargeSluiceBlockEntity be = helper.getBlockEntity(CONTROLLER);
        helper.assertTrue(be != null, "Missing Large Sluice");
        LargeSluiceBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(CONTROLLER),
                helper.getBlockState(CONTROLLER),
                be);
        helper.assertTrue(
                !be.structureValid(),
                "Bare Large Sluice reported a formed structure");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void formedStructureHasSourceExactCounts(
            GameTestHelper helper) {
        LargeSluiceBlockEntity be = placeFormed(helper);
        helper.assertTrue(be.structureValid(), "3x7x3 Sluice did not form");
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.LARGE_SLUICE.structureId());
        helper.assertTrue(structure.structure().size() == 63, "Cell count drifted");
        helper.assertTrue(
                structure.portCount(PortType.ITEM_FLUID_IN) == 3,
                "Far-side input count drifted");
        helper.assertTrue(
                structure.portCount(PortType.ITEM_FLUID_OUT) == 2,
                "Close-side output count drifted");
        helper.assertTrue(
                structure.portCount(PortType.ENERGY_INPUT) == 2,
                "Adjacent RU count drifted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void formedStructureBindsInputParts(GameTestHelper helper) {
        placeFormed(helper);
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.LARGE_SLUICE.structureId());
        boolean boundPort = structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.PORT)
                .map(element -> structure.worldPosition(
                        CONTROLLER, FACING, element.offset()))
                .anyMatch(pos -> helper.getBlockEntity(pos)
                        instanceof MteInPlaceBlockEntity part
                        && SluiceParts.isPart(part.spec())
                        && part.mixerControllerPosition().isPresent());
        helper.assertTrue(boundPort, "Sluice input parts were not bound");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void physicalPortsOwnIndependentStores(
            GameTestHelper helper) {
        placeFormed(helper);
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.LARGE_SLUICE.structureId());
        MteInPlaceBlockEntity input = null;
        MteInPlaceBlockEntity output = null;
        for (var element : structure.structure()) {
            var predicate = structure.predicate(element);
            if (predicate.port().isEmpty()) {
                continue;
            }
            var be = helper.getBlockEntity(
                    structure.worldPosition(CONTROLLER, FACING, element.offset()));
            if (!(be instanceof MteInPlaceBlockEntity part)) {
                continue;
            }
            if (predicate.port().orElseThrow() == PortType.ITEM_FLUID_IN) {
                input = part;
            } else if (predicate.port().orElseThrow()
                    == PortType.ITEM_FLUID_OUT) {
                output = part;
            }
        }
        helper.assertTrue(input != null, "Missing Sluice input store");
        helper.assertTrue(output != null, "Missing Sluice output store");
        helper.assertTrue(
                input.portStore().configured()
                        && output.portStore().configured(),
                "Physical Sluice stores were not configured");
        helper.assertTrue(
                input.portStore() != output.portStore(),
                "Sluice ports still alias one store");
        helper.assertTrue(
                !input.portStore().assignment().itemInputLocals().isEmpty(),
                "Input store has no input slot");
        helper.assertTrue(
                !output.portStore().assignment().itemOutputLocals().isEmpty(),
                "Output store has no output slot");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void activeStateUsesSourceDesigns(GameTestHelper helper) {
        placeFormed(helper);
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.LARGE_SLUICE.structureId());
        long idle = structure.structure().stream()
                .filter(element -> structure.predicate(element).block()
                        .filter(SluiceParts.PART_ID::equals)
                        .isPresent())
                .map(element -> structure.worldPosition(
                        CONTROLLER, FACING, element.offset()))
                .filter(pos -> helper.getBlockState(pos)
                        .getValue(MteInPlaceBlock.SLUICE_DESIGN) == 0)
                .count();
        helper.assertTrue(idle > 0, "Idle north Sluice design did not paint");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void survivalRecipeIsPresent(GameTestHelper helper) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft", "machines/large_sluice");
        boolean found = helper.getLevel()
                .getRecipeManager()
                .getAllRecipesFor(RecipeType.CRAFTING)
                .stream()
                .map(RecipeHolder::id)
                .anyMatch(id::equals);
        helper.assertTrue(found, "Large Sluice survival recipe is missing");
        helper.assertTrue(
                ModItems.LARGE_SLUICE.get() != null,
                "Large Sluice item is missing");
        helper.succeed();
    }

    private static LargeSluiceBlockEntity placeFormed(
            GameTestHelper helper) {
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.LARGE_SLUICE.structureId());
        helper.setBlock(
                CONTROLLER,
                ModBlocks.LARGE_SLUICE.get()
                        .defaultBlockState()
                        .setValue(ProcessingMachineBlock.FACING, FACING));
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
                                    CONTROLLER, FACING, element.offset()),
                            BuiltInRegistries.BLOCK.getOptional(
                                    predicate.block().orElseThrow())
                                    .orElseThrow());
                });
        LargeSluiceBlockEntity be = helper.getBlockEntity(CONTROLLER);
        helper.assertTrue(be != null, "Missing Large Sluice");
        LargeSluiceBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(CONTROLLER),
                helper.getBlockState(CONTROLLER),
                be);
        return be;
    }
}
