package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.block.ImplosionCompressorWalls;
import com.masson.cruciblecraft.content.blockentity.ImplosionCompressorBlockEntity;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureCatalog;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PredicateKind;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PortType;
import com.masson.cruciblecraft.api.energy.EnergyType;
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

/** GT6 Implosion Compressor 17110. */
@GameTestHolder(ImplosionCompressorGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class ImplosionCompressorGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_multiblock";
    private static final String TEMPLATE = "empty";
    private static final BlockPos CONTROLLER = new BlockPos(6, 2, 6);
    private static final Direction FACING = Direction.NORTH;

    private ImplosionCompressorGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void controllerIsLive(GameTestHelper helper) {
        helper.assertTrue(
                ModBlocks.IMPLOSION_COMPRESSOR.get() != null,
                "Implosion Compressor block is missing");
        helper.assertTrue(
                ModItems.IMPLOSION_COMPRESSOR.get() != null,
                "Implosion Compressor item is missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void bareControllerIsNotFormed(GameTestHelper helper) {
        helper.setBlock(
                CONTROLLER,
                ModBlocks.IMPLOSION_COMPRESSOR.get()
                        .defaultBlockState()
                        .setValue(ProcessingMachineBlock.FACING, FACING));
        ImplosionCompressorBlockEntity be =
                helper.getBlockEntity(CONTROLLER);
        helper.assertTrue(be != null, "Missing Implosion Compressor");
        be.requestBuilderRecheck();
        ImplosionCompressorBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(CONTROLLER),
                helper.getBlockState(CONTROLLER),
                be);
        helper.assertTrue(!be.structureValid(), "Bare controller formed");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void formedStructureBindsAllWallPorts(
            GameTestHelper helper) {
        ImplosionCompressorBlockEntity be = placeFormed(helper);
        helper.assertTrue(
                be.structureValid(),
                "Implosion structure did not form: "
                        + (be.lastValidation() == null
                                ? "no validation"
                                : be.lastValidation().diagnostics()));
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.IMPLOSION_COMPRESSOR.structureId());
        helper.assertTrue(
                structure.portCount(PortType.ITEM_FLUID_ENERGY) == 25,
                "Implosion wall port count drifted");
        helper.assertTrue(
                structure.structure().stream()
                        .filter(element -> structure.predicate(element).kind()
                                == PredicateKind.PORT)
                        .allMatch(element -> helper.getBlockEntity(
                                structure.worldPosition(
                                        CONTROLLER,
                                        FACING,
                                        element.offset()))
                                instanceof MteInPlaceBlockEntity port
                                && port.mixerControllerPosition().isPresent()),
                "Implosion wall ports were not bound");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void structureHasTwentyFiveDenseTungstensteelWalls(
            GameTestHelper helper) {
        placeFormed(helper);
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.IMPLOSION_COMPRESSOR.structureId());
        long walls = structure.structure().stream()
                .filter(element -> structure.predicate(element).block()
                        .filter(ResourceLocation.fromNamespaceAndPath(
                                "cruciblecraft",
                                "multiblock/dense_tungstensteel_wall")::equals)
                        .isPresent())
                .count();
        helper.assertTrue(walls == 25, "Dense wall count drifted");
        helper.assertTrue(
                structure.portCount(PortType.ITEM_FLUID_ENERGY) == 25,
                "All walls must expose item/fluid/TU ports");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void everyWallExposesGt6ItemFluidAndTimePort(
            GameTestHelper helper) {
        ImplosionCompressorBlockEntity be = placeFormed(helper);
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.IMPLOSION_COMPRESSOR.structureId());
        int bound = 0;
        int item = 0;
        int fluid = 0;
        int time = 0;
        int kinetic = 0;
        int host = 0;
        int typed = 0;
        int forward = 0;
        int packet = 0;
        for (var element : structure.structure()) {
            if (structure.predicate(element).kind() != PredicateKind.PORT) {
                continue;
            }
            var entity = helper.getBlockEntity(structure.worldPosition(
                    CONTROLLER,
                    FACING,
                    element.offset()));
            if (!(entity instanceof MteInPlaceBlockEntity port)) {
                continue;
            }
            if (port.mixerControllerPosition().isPresent()) {
                bound++;
            }
            if (port.mixerPortType() == PortType.ITEM_FLUID_ENERGY) {
                typed++;
            }
            if (ImplosionCompressorWalls.hostReady(port)) {
                host++;
            }
            if (ImplosionCompressorWalls.forwardsEnergy(port)) {
                forward++;
            }
            if (port.itemHandler(Direction.NORTH) != null) {
                item++;
            }
            if (port.fluidHandler(Direction.NORTH) != null) {
                fluid++;
            }
            if (port.handles(EnergyType.TIME, Direction.NORTH)) {
                time++;
            }
            if (port.handles(EnergyType.KINETIC_ROTATION, Direction.NORTH)) {
                kinetic++;
            }
            if (port.insert(
                    EnergyType.TIME,
                    1L,
                    1L,
                    Direction.NORTH,
                    true) == 1L) {
                packet++;
            }
        }
        helper.assertTrue(
                bound == 25
                        && item == 25
                        && fluid == 25
                        && time == 25
                        && kinetic == 0
                        && packet == 25,
                "Implosion wall capability counts: bound=" + bound
                        + " item=" + item
                        + " fluid=" + fluid
                        + " time=" + time
                        + " kinetic=" + kinetic
                        + " typed=" + typed
                        + " host=" + host
                        + " forward=" + forward
                        + " packet=" + packet);
        long capable = structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.PORT)
                .map(element -> structure.worldPosition(
                        CONTROLLER,
                        FACING,
                        element.offset()))
                .filter(pos -> helper.getBlockEntity(pos)
                        instanceof MteInPlaceBlockEntity port
                        && port.itemHandler(Direction.NORTH) != null
                        && port.fluidHandler(Direction.NORTH) != null
                        && port.handles(EnergyType.TIME, Direction.NORTH)
                        && !port.handles(
                                EnergyType.KINETIC_ROTATION,
                                Direction.NORTH))
                .count();
        helper.assertTrue(
                capable == 25,
                "Not every 18023 wall exposes GT6 all-purpose port capabilities: "
                        + capable + "/25");
        helper.assertTrue(be.structureValid(), "Port host lost structure validity");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void survivalRecipeIsPresent(GameTestHelper helper) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft", "machines/implosion_compressor");
        boolean found = helper.getLevel()
                .getRecipeManager()
                .getAllRecipesFor(RecipeType.CRAFTING)
                .stream()
                .map(RecipeHolder::id)
                .anyMatch(id::equals);
        helper.assertTrue(found, "Implosion Compressor crafting recipe is missing");
        helper.succeed();
    }

    private static ImplosionCompressorBlockEntity placeFormed(
            GameTestHelper helper) {
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.IMPLOSION_COMPRESSOR.structureId());
        helper.setBlock(
                CONTROLLER,
                ModBlocks.IMPLOSION_COMPRESSOR.get()
                        .defaultBlockState()
                        .setValue(ProcessingMachineBlock.FACING, FACING));
        structure.structure().stream()
                .filter(element -> {
                    var kind = structure.predicate(element).kind();
                    return kind == PredicateKind.PORT
                            || kind == PredicateKind.BLOCK;
                })
                .forEach(element -> helper.setBlock(
                        structure.worldPosition(
                                CONTROLLER,
                                FACING,
                                element.offset()),
                        BuiltInRegistries.BLOCK.getOptional(
                                structure.predicate(element).block()
                                        .orElseThrow())
                                .orElseThrow()));
        ImplosionCompressorBlockEntity be =
                helper.getBlockEntity(CONTROLLER);
        helper.assertTrue(be != null, "Missing Implosion Compressor");
        be.requestBuilderRecheck();
        ImplosionCompressorBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(CONTROLLER),
                helper.getBlockState(CONTROLLER),
                be);
        return be;
    }
}
