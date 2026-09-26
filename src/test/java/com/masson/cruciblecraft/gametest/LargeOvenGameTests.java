package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.content.block.InvarOvenWalls;
import com.masson.cruciblecraft.content.block.LargeOvenBlock;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.blockentity.LargeOvenBlockEntity;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureCatalog;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.Offset;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PortType;
import com.masson.cruciblecraft.content.multiblock.CoilHosts;
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
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Isolated GT6 Large Electric Oven 17106 gate. */
@GameTestHolder(LargeOvenGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class LargeOvenGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_multiblock";
    private static final String TEMPLATE = "empty";
    private static final BlockPos CONTROLLER = new BlockPos(2, 2, 2);
    private static final Direction FACING = Direction.NORTH;

    private LargeOvenGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void controllerIsLive(GameTestHelper helper) {
        helper.assertTrue(
                ModBlocks.LARGE_OVEN.get() != null,
                "Large oven block is missing");
        helper.assertTrue(
                InvarOvenWalls.wall() != null,
                "Invar wall 18007 is missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void bareControllerIsNotFormed(GameTestHelper helper) {
        helper.setBlock(
                CONTROLLER,
                ModBlocks.LARGE_OVEN.get()
                        .defaultBlockState()
                        .setValue(ProcessingMachineBlock.FACING, FACING));
        LargeOvenBlockEntity be = helper.getBlockEntity(CONTROLLER);
        helper.assertTrue(be != null, "Missing large oven");
        LargeOvenBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(CONTROLLER),
                helper.getBlockState(CONTROLLER),
                be);
        helper.assertTrue(
                !be.structureValid(),
                "Bare large oven reported a formed structure");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void formedStructureBindsWallsAndNichromeCoils(
            GameTestHelper helper) {
        LargeOvenBlockEntity be = placeFormed(helper, nichromeCoil());
        helper.assertTrue(be.structureValid(), "3x3x3 oven did not form");
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.LARGE_OVEN.structureId());
        helper.assertTrue(
                structure.portCount(PortType.ITEM_FLUID_ENERGY) == 17,
                "Invar ITEM_FLUID_ENERGY wall count drifted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void carborundumCoilsForm(GameTestHelper helper) {
        LargeOvenBlockEntity be = placeFormed(
                helper, CoilHosts.block(CoilHosts.CARBORUNDUM));
        helper.assertTrue(
                be.structureValid(),
                "All-carborundum coil ring did not form");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void mixedCoilsDoNotForm(GameTestHelper helper) {
        LargeOvenBlockEntity be = placeFormed(helper, nichromeCoil());
        helper.assertTrue(be.structureValid(), "Nichrome oven did not form");
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.LARGE_OVEN.structureId());
        helper.setBlock(
                structure.worldPosition(
                        CONTROLLER, FACING, new Offset(0, 1, 0)),
                CoilHosts.block(CoilHosts.CARBORUNDUM));
        tick(helper, be);
        helper.assertTrue(
                !be.structureValid(),
                "Mixed nichrome and carborundum coils formed");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void coilsHaveNoItemIo(GameTestHelper helper) {
        LargeOvenBlockEntity be = placeFormed(helper, nichromeCoil());
        helper.assertTrue(be.structureValid(), "Oven did not form for coil IO");
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.LARGE_OVEN.structureId());
        MteInPlaceBlockEntity coil = helper.getBlockEntity(
                structure.worldPosition(
                        CONTROLLER, FACING, new Offset(0, 1, 0)));
        helper.assertTrue(coil != null, "Missing oven coil");
        helper.assertTrue(
                !coil.accepts(PortType.ITEM_FLUID_ENERGY)
                        && InvarOvenWalls.items(coil) == null,
                "Oven coil accepted ITEM_FLUID_ENERGY IO");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void wallPortCooksCobbleToStone(GameTestHelper helper) {
        LargeOvenBlockEntity be = placeFormed(helper, nichromeCoil());
        helper.assertTrue(be.structureValid(), "Oven did not form before cooking");
        MteInPlaceBlockEntity wall = wall(helper);
        ItemStack leftover = InvarOvenWalls.items(wall).insertItem(
                0, new ItemStack(Items.COBBLESTONE), false);
        helper.assertTrue(leftover.isEmpty(), "Wall did not accept cobblestone");
        long accepted = InvarOvenWalls.insertEnergy(
                wall, EnergyType.ELECTRIC, 4_096L, 4_096L, false);
        helper.assertTrue(accepted > 0L, "Wall did not accept EU");
        helper.startSequence()
                .thenExecuteFor(8, () -> tick(helper, be))
                .thenExecute(() -> {
                    ItemStack out = InvarOvenWalls.items(wall)
                            .extractItem(1, 1, false);
                    helper.assertTrue(
                            out.is(Items.STONE),
                            "Wall did not extract stone: " + out);
                    helper.assertTrue(
                            helper.getBlockState(CONTROLLER)
                                    .getValue(LargeOvenBlock.LIT),
                            "Controller stayed unlit after EU");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void survivalRecipeIsPresent(GameTestHelper helper) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft", "machines/large_oven");
        boolean found = helper.getLevel()
                .getRecipeManager()
                .getAllRecipesFor(RecipeType.CRAFTING)
                .stream()
                .map(RecipeHolder::id)
                .anyMatch(id::equals);
        helper.assertTrue(found, "Large oven survival recipe is missing");
        helper.assertTrue(
                ModItems.LARGE_OVEN.get() != null,
                "Large oven item is missing");
        boolean coil = helper.getLevel()
                .getRecipeManager()
                .getAllRecipesFor(RecipeType.CRAFTING)
                .stream()
                .map(RecipeHolder::id)
                .anyMatch(ResourceLocation.fromNamespaceAndPath(
                        "cruciblecraft",
                        "multiblock/large_carborundum_coil")::equals);
        helper.assertTrue(coil, "Carborundum coil recipe is missing");
        helper.succeed();
    }

    private static LargeOvenBlockEntity placeFormed(
            GameTestHelper helper, Block coil) {
        MultiblockStructureDefinition structure =
                MultiblockStructureCatalog.require(
                        ModMultiblockControllers.LARGE_OVEN.structureId());
        helper.setBlock(
                CONTROLLER,
                ModBlocks.LARGE_OVEN.get()
                        .defaultBlockState()
                        .setValue(ProcessingMachineBlock.FACING, FACING));
        for (var element : structure.structure()) {
            var predicate = structure.predicate(element);
            Block block = switch (predicate.kind()) {
                case PORT -> BuiltInRegistries.BLOCK.get(
                        predicate.block().orElseThrow());
                case TAG -> coil;
                default -> null;
            };
            if (block == null) {
                continue;
            }
            helper.setBlock(
                    structure.worldPosition(
                            CONTROLLER, FACING, element.offset()),
                    block);
        }
        LargeOvenBlockEntity be = helper.getBlockEntity(CONTROLLER);
        helper.assertTrue(be != null, "Missing large oven");
        tick(helper, be);
        return be;
    }

    private static void tick(GameTestHelper helper, LargeOvenBlockEntity be) {
        LargeOvenBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(CONTROLLER),
                helper.getBlockState(CONTROLLER),
                be);
    }

    private static Block nichromeCoil() {
        return ModBlocks.mteInPlaceBlocksById().get(
                ResourceLocation.fromNamespaceAndPath(
                        "cruciblecraft",
                        "multiblock/large_nichrome_coil")).get();
    }

    private static MteInPlaceBlockEntity wall(GameTestHelper helper) {
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.LARGE_OVEN.structureId());
        return helper.getBlockEntity(structure.worldPosition(
                CONTROLLER, FACING, new Offset(-1, 0, 0)));
    }
}
