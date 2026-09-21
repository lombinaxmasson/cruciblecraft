package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.content.block.CrusherWheels;
import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.blockentity.LargeCrusherBlockEntity;
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

/** GT6 Large Crusher 17108. */
@GameTestHolder(CrucibleCraftGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class LargeCrusherGameTests {
    private static final String TEMPLATE = "empty";
    private static final BlockPos CONTROLLER = new BlockPos(6, 2, 6);
    private static final Direction FACING = Direction.NORTH;

    private LargeCrusherGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void controllerIsLive(GameTestHelper helper) {
        helper.assertTrue(
                ModBlocks.LARGE_CRUSHER.get() != null,
                "Large crusher block is missing");
        helper.assertTrue(
                CrusherWheels.part() != null,
                "Crusher wheels 18107 are missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void bareControllerIsNotFormed(GameTestHelper helper) {
        helper.setBlock(
                CONTROLLER,
                ModBlocks.LARGE_CRUSHER.get()
                        .defaultBlockState()
                        .setValue(ProcessingMachineBlock.FACING, FACING));
        LargeCrusherBlockEntity be = helper.getBlockEntity(CONTROLLER);
        helper.assertTrue(be != null, "Missing large crusher");
        LargeCrusherBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(CONTROLLER),
                helper.getBlockState(CONTROLLER),
                be);
        helper.assertTrue(
                !be.structureValid(),
                "Bare large crusher reported a formed structure");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void formedBasinBindsWheelInAndBottomOut(
            GameTestHelper helper) {
        LargeCrusherBlockEntity be = placeFormed(helper);
        helper.assertTrue(be.structureValid(), "5x5x3 crusher did not form");
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.LARGE_CRUSHER.structureId());
        helper.assertTrue(
                structure.portCount(PortType.ITEM_FLUID_IN) == 9,
                "Wheel in ports drifted");
        helper.assertTrue(
                structure.portCount(PortType.ITEM_FLUID_OUT) == 24,
                "Bottom out ports drifted");
        helper.assertTrue(
                structure.portCount(PortType.ENERGY_INPUT) == 2,
                "Energy in ports drifted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void formedBasinBindsFillWheels(GameTestHelper helper) {
        placeFormed(helper);
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.LARGE_CRUSHER.structureId());
        boolean bound = structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.BLOCK)
                .map(element -> structure.worldPosition(
                        CONTROLLER, FACING, element.offset()))
                .anyMatch(pos -> {
                    if (!(helper.getBlockEntity(pos)
                            instanceof MteInPlaceBlockEntity wheel)) {
                        return false;
                    }
                    return CrusherWheels.isPart(wheel.spec())
                            && wheel.mixerControllerPosition().isPresent();
                });
        helper.assertTrue(bound, "Fill crusher wheels were not bound for walk damage");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void formedIdleWheelsUseZAxisOverlay(GameTestHelper helper) {
        placeFormed(helper);
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.LARGE_CRUSHER.structureId());
        long painted = structure.structure().stream()
                .filter(element -> structure.predicate(element).block()
                        .filter(CrusherWheels.PART_ID::equals)
                        .isPresent())
                .map(element -> structure.worldPosition(
                        CONTROLLER, FACING, element.offset()))
                .filter(pos -> helper.getBlockState(pos)
                        .getValue(MteInPlaceBlock.WHEEL_DESIGN) == 0)
                .count();
        helper.assertTrue(
                painted == 18,
                "Idle north crusher wheels should use GT6 overlay 0");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void survivalRecipeIsPresent(GameTestHelper helper) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft", "machines/large_crusher");
        boolean found = helper.getLevel()
                .getRecipeManager()
                .getAllRecipesFor(RecipeType.CRAFTING)
                .stream()
                .map(RecipeHolder::id)
                .anyMatch(id::equals);
        helper.assertTrue(found, "Large crusher survival recipe is missing");
        helper.assertTrue(
                ModItems.LARGE_CRUSHER.get() != null,
                "Large crusher item is missing");
        helper.succeed();
    }

    private static LargeCrusherBlockEntity placeFormed(GameTestHelper helper) {
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.LARGE_CRUSHER.structureId());
        helper.setBlock(
                CONTROLLER,
                ModBlocks.LARGE_CRUSHER.get()
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
        LargeCrusherBlockEntity be = helper.getBlockEntity(CONTROLLER);
        helper.assertTrue(be != null, "Missing large crusher");
        LargeCrusherBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(CONTROLLER),
                helper.getBlockState(CONTROLLER),
                be);
        return be;
    }
}
