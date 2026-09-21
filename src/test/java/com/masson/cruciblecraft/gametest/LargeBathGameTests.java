package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.blockentity.LargeBathBlockEntity;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureCatalog;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PortType;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PredicateKind;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModMultiblockControllers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** GT6 Large Bathing Vat 17104 formation against MultiTileEntityBath. */
@GameTestHolder(CrucibleCraftGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class LargeBathGameTests {
    private static final String TEMPLATE = "empty";

    private LargeBathGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void largeBathingVatFidelity(GameTestHelper helper) {
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.LARGE_BATH.structureId());
        long itemFluid = structure.portCount(PortType.ITEM_FLUID);
        long energy = structure.portCount(PortType.ENERGY_INPUT);
        long controllers = structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.CONTROLLER)
                .count();
        helper.assertTrue(
                structure.structure().size() == 50
                        && structure.scanVolume() == 50
                        && itemFluid == 49
                        && energy == 0
                        && controllers == 1,
                "Bath geometry drifted from GT6 5x5x2: "
                        + structure.structure().size()
                        + " positions, "
                        + itemFluid
                        + " item/fluid");
        var source = structure.source().orElseThrow();
        helper.assertTrue(
                "gregtech.tileentity.multiblocks.MultiTileEntityBath"
                        .equals(source.className())
                        && "checkStructure2".equals(source.method()),
                "Bath structure source class drifted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void largeBathingVatFormation(GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        Direction facing = Direction.NORTH;
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.LARGE_BATH.structureId());
        helper.setBlock(
                controllerPos,
                ModBlocks.LARGE_BATH.get()
                        .defaultBlockState()
                        .setValue(ProcessingMachineBlock.FACING, facing));
        structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.PORT)
                .forEach(element -> helper.setBlock(
                        structure.worldPosition(
                                controllerPos, facing, element.offset()),
                        structurePaletteBlock(structure.predicate(element))));
        LargeBathBlockEntity bath = helper.getBlockEntity(controllerPos);
        helper.startSequence()
                .thenIdle(25)
                .thenExecute(() -> helper.assertTrue(
                        bath.structureValid(),
                        "Large bathing vat structure was not recognized"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void largeBathingVatSurvivalRecipeIsPresent(
            GameTestHelper helper) {
        net.minecraft.resources.ResourceLocation id =
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                        "cruciblecraft", "machines/large_bath");
        boolean found = helper.getLevel()
                .getRecipeManager()
                .getAllRecipesFor(
                        net.minecraft.world.item.crafting.RecipeType.CRAFTING)
                .stream()
                .map(net.minecraft.world.item.crafting.RecipeHolder::id)
                .anyMatch(id::equals);
        helper.assertTrue(found, "Large bathing vat survival recipe is missing");
        helper.succeed();
    }

    private static net.minecraft.world.level.block.Block structurePaletteBlock(
            MultiblockStructureDefinition.PalettePredicate predicate) {
        return BuiltInRegistries.BLOCK.getOptional(predicate.block().orElseThrow())
                .orElseThrow();
    }
}
