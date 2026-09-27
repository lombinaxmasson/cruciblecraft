package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.content.block.AutoclaveWalls;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.blockentity.LargeAutoclaveBlockEntity;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Isolated GT6 Large Autoclave 17112 gate. */
@GameTestHolder(LargeAutoclaveGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class LargeAutoclaveGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_multiblock";
    private static final String TEMPLATE = "empty";
    private static final BlockPos CONTROLLER = new BlockPos(2, 2, 2);
    private static final Direction FACING = Direction.NORTH;

    private LargeAutoclaveGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void controllerIsLive(GameTestHelper helper) {
        helper.assertTrue(
                ModBlocks.LARGE_AUTOCLAVE.get() != null,
                "Large autoclave block is missing");
        helper.assertTrue(
                AutoclaveWalls.wall() != null,
                "Dense stainless steel wall 18022 is missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void bareControllerIsNotFormed(GameTestHelper helper) {
        helper.setBlock(
                CONTROLLER,
                ModBlocks.LARGE_AUTOCLAVE.get()
                        .defaultBlockState()
                        .setValue(ProcessingMachineBlock.FACING, FACING));
        LargeAutoclaveBlockEntity be = helper.getBlockEntity(CONTROLLER);
        helper.assertTrue(be != null, "Missing large autoclave");
        LargeAutoclaveBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(CONTROLLER),
                helper.getBlockState(CONTROLLER),
                be);
        helper.assertTrue(
                !be.structureValid(),
                "Bare large autoclave reported a formed structure");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void formedHollowCubeBindsItemFluidEnergyWalls(
            GameTestHelper helper) {
        LargeAutoclaveBlockEntity be = placeFormed(helper);
        helper.assertTrue(be.structureValid(), "3x3x3 autoclave did not form");
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.LARGE_AUTOCLAVE.structureId());
        helper.assertTrue(
                structure.portCount(PortType.ITEM_FLUID_ENERGY) == 25,
                "Hollow 18022 ITEM_FLUID_ENERGY count drifted");
        int walls = 0;
        int alias = 0;
        int items = 0;
        int fluids = 0;
        int time = 0;
        for (var element : structure.structure()) {
            if (structure.predicate(element).kind() != PredicateKind.PORT) {
                continue;
            }
            var entity = helper.getBlockEntity(structure.worldPosition(
                    CONTROLLER, FACING, element.offset()));
            if (!(entity instanceof MteInPlaceBlockEntity wall)) {
                continue;
            }
            walls++;
            if (!wall.ownsIndependentPortStore()) {
                alias++;
            }
            if (wall.itemHandler(Direction.UP) != null) {
                items++;
            }
            if (wall.fluidHandler(Direction.UP) != null) {
                fluids++;
            }
            if (wall.handles(EnergyType.TIME, Direction.UP)) {
                time++;
            }
        }
        helper.assertTrue(
                walls == 25
                        && alias == 25
                        && items == 25
                        && fluids == 25
                        && time == 25,
                "18022 alias walls: found=" + walls
                        + " alias=" + alias
                        + " item=" + items
                        + " fluid=" + fluids
                        + " tu=" + time);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void survivalRecipeIsPresent(GameTestHelper helper) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft", "machines/large_autoclave");
        boolean found = helper.getLevel()
                .getRecipeManager()
                .getAllRecipesFor(RecipeType.CRAFTING)
                .stream()
                .map(RecipeHolder::id)
                .anyMatch(id::equals);
        helper.assertTrue(found, "Large autoclave survival recipe is missing");
        helper.assertTrue(
                ModItems.LARGE_AUTOCLAVE.get() != null,
                "Large autoclave item is missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void controllerHullAcceptsAnyFaceIo(GameTestHelper helper) {
        LargeAutoclaveBlockEntity be = placeFormed(helper);
        helper.assertTrue(be.structureValid(), "3x3x3 autoclave did not form");
        for (Direction side : Direction.values()) {
            helper.assertTrue(
                    be.items(side) != null,
                    "17112 hull rejected items on " + side);
            helper.assertTrue(
                    be.fluids(side) != null,
                    "17112 hull rejected fluids on " + side);
            helper.assertTrue(
                    be.handles(EnergyType.TIME, side),
                    "17112 hull rejected TU on " + side);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void autoOutputsDownWithoutAutoInput(GameTestHelper helper) {
        LargeAutoclaveBlockEntity be = placeFormed(helper);
        helper.assertTrue(be.structureValid(), "3x3x3 autoclave did not form");
        BlockPos below = CONTROLLER.below();
        helper.setBlock(below, Blocks.CHEST);
        int output = be.spec().items().outputs().getFirst();
        be.inventory().setStackInSlot(output, new ItemStack(Items.DIRT, 8));
        LargeAutoclaveBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(CONTROLLER),
                helper.getBlockState(CONTROLLER),
                be);
        ChestBlockEntity chest = helper.getBlockEntity(below);
        helper.assertTrue(chest != null, "Missing auto-out chest");
        helper.assertTrue(
                chest.getItem(0).is(Items.DIRT) && chest.getItem(0).getCount() == 8,
                "17112 did not auto-output into the inventory below");
        helper.assertTrue(
                be.inventory().getStackInSlot(output).isEmpty(),
                "17112 kept items after bottom auto-out");
        helper.succeed();
    }

    private static LargeAutoclaveBlockEntity placeFormed(
            GameTestHelper helper) {
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.LARGE_AUTOCLAVE.structureId());
        helper.setBlock(
                CONTROLLER,
                ModBlocks.LARGE_AUTOCLAVE.get()
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
        LargeAutoclaveBlockEntity be = helper.getBlockEntity(CONTROLLER);
        helper.assertTrue(be != null, "Missing large autoclave");
        LargeAutoclaveBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(CONTROLLER),
                helper.getBlockState(CONTROLLER),
                be);
        return be;
    }
}
