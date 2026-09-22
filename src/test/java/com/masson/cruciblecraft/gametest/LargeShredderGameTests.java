package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.content.block.ShredderBlades;
import com.masson.cruciblecraft.content.block.ShredderDamage;
import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.blockentity.LargeShredderBlockEntity;
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

/** GT6 Large Shredder 17109. */
@GameTestHolder(CrucibleCraftGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class LargeShredderGameTests {
    private static final String TEMPLATE = "empty";
    private static final BlockPos CONTROLLER = new BlockPos(6, 2, 6);
    private static final Direction FACING = Direction.NORTH;

    private LargeShredderGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void controllerIsLive(GameTestHelper helper) {
        helper.assertTrue(
                ModBlocks.LARGE_SHREDDER.get() != null,
                "Large shredder block is missing");
        helper.assertTrue(
                ShredderBlades.part() != null,
                "Shredder blades 18108 are missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void bareControllerIsNotFormed(GameTestHelper helper) {
        helper.setBlock(
                CONTROLLER,
                ModBlocks.LARGE_SHREDDER.get()
                        .defaultBlockState()
                        .setValue(ProcessingMachineBlock.FACING, FACING));
        LargeShredderBlockEntity be = helper.getBlockEntity(CONTROLLER);
        helper.assertTrue(be != null, "Missing large shredder");
        LargeShredderBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(CONTROLLER),
                helper.getBlockState(CONTROLLER),
                be);
        helper.assertTrue(
                !be.structureValid(),
                "Bare large shredder reported a formed structure");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void formedBasinBindsBladeInAndBottomOut(
            GameTestHelper helper) {
        LargeShredderBlockEntity be = placeFormed(helper);
        helper.assertTrue(be.structureValid(), "5x5x3 shredder did not form");
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.LARGE_SHREDDER.structureId());
        helper.assertTrue(
                structure.portCount(PortType.ITEM_FLUID_IN) == 9,
                "Blade in ports drifted");
        helper.assertTrue(
                structure.portCount(PortType.ITEM_FLUID_OUT) == 24,
                "Bottom out ports drifted");
        helper.assertTrue(
                structure.portCount(PortType.ENERGY_INPUT) == 2,
                "Energy in ports drifted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void formedStructureCountsFiftySixWallsAndTwoEnergyPorts(
            GameTestHelper helper) {
        placeFormed(helper);
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.LARGE_SHREDDER.structureId());
        long wallBlocks = structure.structure().stream()
                .filter(element -> structure.predicate(element).block()
                        .filter(ResourceLocation.fromNamespaceAndPath(
                                "cruciblecraft", "tungstensteel/wall")::equals)
                        .isPresent())
                .count();
        helper.assertTrue(wallBlocks == 56, "Wall count drifted from GT6 56");
        helper.assertTrue(
                structure.portCount(PortType.ENERGY_INPUT) == 2,
                "Large Shredder must have two RU holes");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void formedStructureBindsNineInputAndNineStructureBlades(
            GameTestHelper helper) {
        placeFormed(helper);
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.LARGE_SHREDDER.structureId());
        long input = structure.portCount(PortType.ITEM_FLUID_IN);
        long structureOnly = structure.structure().stream()
                .filter(element -> structure.predicate(element).block()
                        .filter(ShredderBlades.PART_ID::equals)
                        .isPresent())
                .count();
        helper.assertTrue(input == 9, "GT6 input blade count drifted");
        helper.assertTrue(
                structureOnly == 9,
                "GT6 structure-only blade count drifted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void formedBasinBindsFillBlades(GameTestHelper helper) {
        placeFormed(helper);
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.LARGE_SHREDDER.structureId());
        boolean bound = structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.BLOCK)
                .map(element -> structure.worldPosition(
                        CONTROLLER, FACING, element.offset()))
                .anyMatch(pos -> {
                    if (!(helper.getBlockEntity(pos)
                            instanceof MteInPlaceBlockEntity blade)) {
                        return false;
                    }
                    return ShredderBlades.isPart(blade.spec())
                            && blade.mixerControllerPosition().isPresent();
                });
        helper.assertTrue(bound, "Fill shredder blades were not bound for walk damage");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void runningShredderHurtsFive(GameTestHelper helper) {
        helper.assertTrue(
                ShredderDamage.AMOUNT == 5.0F,
                "GT6 Shredder walk-over damage drifted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void formedIdleBladesUseZAxisOverlay(GameTestHelper helper) {
        placeFormed(helper);
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.LARGE_SHREDDER.structureId());
        long painted = structure.structure().stream()
                .filter(element -> structure.predicate(element).block()
                        .filter(ShredderBlades.PART_ID::equals)
                        .isPresent())
                .map(element -> structure.worldPosition(
                        CONTROLLER, FACING, element.offset()))
                .filter(pos -> helper.getBlockState(pos)
                        .getValue(MteInPlaceBlock.WHEEL_DESIGN) == 0)
                .count();
        helper.assertTrue(
                painted == 18,
                "Idle north shredder blades should use GT6 overlay 0");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void survivalRecipeIsPresent(GameTestHelper helper) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft", "machines/large_shredder");
        boolean found = helper.getLevel()
                .getRecipeManager()
                .getAllRecipesFor(RecipeType.CRAFTING)
                .stream()
                .map(RecipeHolder::id)
                .anyMatch(id::equals);
        helper.assertTrue(found, "Large shredder survival recipe is missing");
        helper.assertTrue(
                ModItems.LARGE_SHREDDER.get() != null,
                "Large shredder item is missing");
        helper.succeed();
    }

    private static LargeShredderBlockEntity placeFormed(GameTestHelper helper) {
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.LARGE_SHREDDER.structureId());
        helper.setBlock(
                CONTROLLER,
                ModBlocks.LARGE_SHREDDER.get()
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
        LargeShredderBlockEntity be = helper.getBlockEntity(CONTROLLER);
        helper.assertTrue(be != null, "Missing large shredder");
        LargeShredderBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(CONTROLLER),
                helper.getBlockState(CONTROLLER),
                be);
        return be;
    }
}
