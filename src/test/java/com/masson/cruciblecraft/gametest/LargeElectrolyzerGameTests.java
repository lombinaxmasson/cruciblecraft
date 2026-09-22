package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.content.block.ElectrolyzerParts;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.blockentity.LargeElectrolyzerBlockEntity;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureCatalog;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.Offset;
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
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
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
        helper.assertTrue(
                be.structureValid(),
                "3x3x2 electrolyzer did not form: "
                        + (be.lastValidation() == null
                                ? "no validation"
                                : be.lastValidation().diagnostics()));
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.LARGE_ELECTROLYZER.structureId());
        helper.assertTrue(
                structure.portCount(PortType.ITEM_FLUID_ENERGY_IN) == 8,
                "Bottom in ports drifted");
        helper.assertTrue(
                structure.portCount(PortType.ITEM_FLUID_OUT) == 9,
                "Top out ports drifted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void formedPortsExposeGt6InputOnlyCapabilities(
            GameTestHelper helper) {
        placeFormed(helper);
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.LARGE_ELECTROLYZER.structureId());
        MteInPlaceBlockEntity bottom = helper.getBlockEntity(
                structure.worldPosition(
                        CONTROLLER, FACING, new Offset(-1, 0, 0)));
        MteInPlaceBlockEntity top = helper.getBlockEntity(
                structure.worldPosition(
                        CONTROLLER, FACING, new Offset(-1, 1, 0)));
        helper.assertTrue(bottom != null, "Missing bottom electrolyzer part");
        helper.assertTrue(top != null, "Missing top electrolyzer part");
        helper.assertTrue(
                bottom.mixerPortType() == PortType.ITEM_FLUID_ENERGY_IN,
                "Bottom part did not bind ONLY_ITEM_FLUID_ENERGY_IN");
        helper.assertTrue(
                top.mixerPortType() == PortType.ITEM_FLUID_OUT,
                "Top part did not bind ONLY_ITEM_FLUID_OUT");
        helper.assertTrue(
                bottom.handles(EnergyType.ELECTRIC, Direction.UP),
                "Bottom part does not accept Electric energy");
        helper.assertTrue(
                ElectrolyzerParts.insertEnergy(
                                bottom,
                                EnergyType.ELECTRIC,
                                512L,
                                512L,
                                true)
                        > 0L,
                "Bottom part rejected Electric energy input");
        helper.assertTrue(
                !top.handles(EnergyType.ELECTRIC, Direction.UP)
                        && ElectrolyzerParts.insertEnergy(
                                        top,
                                        EnergyType.ELECTRIC,
                                        512L,
                                        512L,
                                        true)
                                == 0L,
                "Top part exposed an energy input");

        ItemStack bottomExtract = ElectrolyzerParts.items(bottom)
                .extractItem(0, 1, true);
        ItemStack topInsert = ElectrolyzerParts.items(top)
                .insertItem(0, new ItemStack(Items.COBBLESTONE), true);
        helper.assertTrue(
                bottomExtract.isEmpty() && !topInsert.isEmpty(),
                "Electrolyzer item directions are not input-only/output-only");

        FluidStack water = new FluidStack(Fluids.WATER, 1_000);
        helper.assertTrue(
                ElectrolyzerParts.fluids(bottom)
                                .drain(1_000, IFluidHandler.FluidAction.SIMULATE)
                        .isEmpty()
                        && ElectrolyzerParts.fluids(top).fill(
                                        water,
                                        IFluidHandler.FluidAction.SIMULATE)
                                == 0,
                "Electrolyzer fluid directions are not input-only/output-only");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void unformedPartRejectsEnergy(GameTestHelper helper) {
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.LARGE_ELECTROLYZER.structureId());
        helper.setBlock(
                CONTROLLER,
                ModBlocks.LARGE_ELECTROLYZER.get()
                        .defaultBlockState()
                        .setValue(ProcessingMachineBlock.FACING, FACING));
        BlockPos bottomPosition = structure.worldPosition(
                CONTROLLER, FACING, new Offset(-1, 0, 0));
        helper.setBlock(bottomPosition, ElectrolyzerParts.part().defaultBlockState());
        MteInPlaceBlockEntity bottom = helper.getBlockEntity(bottomPosition);
        helper.assertTrue(bottom != null, "Missing unformed electrolyzer part");
        helper.assertTrue(
                !bottom.handles(EnergyType.ELECTRIC, Direction.UP)
                        && ElectrolyzerParts.insertEnergy(
                                        bottom,
                                        EnergyType.ELECTRIC,
                                        512L,
                                        512L,
                                        true)
                                == 0L,
                "Unformed electrolyzer part accepted Electric energy");
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
