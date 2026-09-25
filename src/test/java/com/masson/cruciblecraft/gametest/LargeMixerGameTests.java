package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.blockentity.LargeMixerBlockEntity;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureCatalog;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PortType;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PredicateKind;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModMultiblockControllers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** GT6 Large Batch Mixer 17102. */
@GameTestHolder(CrucibleCraftGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class LargeMixerGameTests {
    private static final String TEMPLATE = "empty";
    private static final BlockPos CONTROLLER = new BlockPos(6, 2, 6);
    private static final Direction FACING = Direction.NORTH;

    private LargeMixerGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void largeMixerFormsAndBindsPorts(GameTestHelper helper) {
        LargeMixerBlockEntity mixer = placeFormed(helper);
        helper.startSequence()
                .thenIdle(25)
                .thenExecute(() -> {
                    helper.assertTrue(
                            mixer.structureValid(),
                            "Large mixer 3x3x2 structure did not form");
                    var structure = MultiblockStructureCatalog.require(
                            ModMultiblockControllers.LARGE_MIXER.structureId());
                    MteInPlaceBlockEntity input = portEntity(
                            helper, structure, PortType.ITEM_FLUID_IN);
                    MteInPlaceBlockEntity output = portEntity(
                            helper, structure, PortType.ITEM_FLUID_OUT);
                    MteInPlaceBlockEntity energy = portEntity(
                            helper, structure, PortType.ENERGY_INPUT);
                    BlockPos controller = helper.absolutePos(CONTROLLER);
                    helper.assertTrue(
                            input.mixerControllerPosition()
                                    .filter(controller::equals)
                                    .isPresent()
                                    && input.itemHandler(Direction.NORTH) != null,
                            "Mixer input wall was not bound as item/fluid input");
                    helper.assertTrue(
                            output.mixerControllerPosition()
                                    .filter(controller::equals)
                                    .isPresent()
                                    && output.itemHandler(Direction.NORTH) != null,
                            "Mixer output wall was not bound as item/fluid output");
                    helper.assertTrue(
                            energy.handles(
                                    com.masson.cruciblecraft.api.energy.EnergyType
                                            .KINETIC_ROTATION,
                                    Direction.NORTH),
                            "Mixer energy wall did not expose RU input");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void largeMixerRejectsBareController(GameTestHelper helper) {
        helper.setBlock(
                CONTROLLER,
                ModBlocks.LARGE_MIXER.get()
                        .defaultBlockState()
                        .setValue(ProcessingMachineBlock.FACING, FACING));
        LargeMixerBlockEntity mixer = helper.getBlockEntity(CONTROLLER);
        helper.assertTrue(mixer != null, "Large mixer block entity is missing");
        LargeMixerBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(CONTROLLER),
                helper.getBlockState(CONTROLLER),
                mixer);
        helper.assertTrue(
                !mixer.structureValid(),
                "Bare large mixer reported a formed structure");
        helper.succeed();
    }

    private static LargeMixerBlockEntity placeFormed(GameTestHelper helper) {
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.LARGE_MIXER.structureId());
        helper.setBlock(
                CONTROLLER,
                ModBlocks.LARGE_MIXER.get()
                        .defaultBlockState()
                        .setValue(ProcessingMachineBlock.FACING, FACING));
        structure.structure().stream()
                .filter(element -> {
                    PredicateKind kind =
                            structure.predicate(element).kind();
                    return kind == PredicateKind.PORT
                            || kind == PredicateKind.BLOCK;
                })
                .forEach(element -> helper.setBlock(
                        structure.worldPosition(CONTROLLER, FACING, element.offset()),
                        BuiltInRegistries.BLOCK.getOptional(
                                structure.predicate(element).block()
                                        .orElseThrow())
                                .orElseThrow()));
        LargeMixerBlockEntity mixer = helper.getBlockEntity(CONTROLLER);
        helper.assertTrue(mixer != null, "Large mixer block entity is missing");
        return mixer;
    }

    private static MteInPlaceBlockEntity portEntity(
            GameTestHelper helper,
            com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition
                    structure,
            PortType type) {
        return structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.PORT)
                .filter(element -> structure.predicate(element).port()
                        .filter(type::equals)
                        .isPresent())
                .map(element -> helper.getBlockEntity(
                        structure.worldPosition(CONTROLLER, FACING, element.offset())))
                .filter(MteInPlaceBlockEntity.class::isInstance)
                .map(MteInPlaceBlockEntity.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "missing mixer port " + type));
    }
}
