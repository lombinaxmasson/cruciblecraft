package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.CableBlock;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.blockentity.CableBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineIoFaces;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModCapabilities;
import com.masson.cruciblecraft.registry.ModProcessingMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Locks the GT6 energy face at the cable terminal, rather than relying on a
 * direct machine {@code insert} call.
 */
@GameTestHolder(ProcessingMachineEnergyCableGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class ProcessingMachineEnergyCableGameTests {
    public static final String NAMESPACE = "cruciblecraft_machines";
    private static final String TEMPLATE = "empty";

    private ProcessingMachineEnergyCableGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void electrolyzerReceivesEuThroughBottomCable(
            GameTestHelper helper) {
        BlockPos machinePos = new BlockPos(4, 2, 4);
        Direction facing = Direction.NORTH;
        helper.setBlock(
                machinePos,
                ModBlocks.ELECTROLYZER.get()
                        .defaultBlockState()
                        .setValue(ProcessingMachineBlock.FACING, facing));
        ConfiguredProcessingMachineBlockEntity machine =
                helper.getBlockEntity(machinePos);
        Direction energyFace = ProcessingMachineIoFaces.energy(
                machine.spec(), facing);
        helper.assertTrue(
                energyFace == Direction.DOWN,
                "Opening electrolyzer energy face drifted to " + energyFace);

        BlockPos cablePos = machinePos.relative(energyFace);
        CableBlock cable = ModBlocks.electricalConductorBlock(
                "copper", MaterialPrefixes.CABLE).get();
        BlockState cableState = cable.defaultBlockState()
                .setValue(
                        CableBlock.PROPERTY_BY_DIRECTION.get(Direction.DOWN),
                        true)
                .setValue(
                        CableBlock.PROPERTY_BY_DIRECTION.get(
                                energyFace.getOpposite()),
                        true);
        helper.setBlock(cablePos, cableState);
        CableBlockEntity endpoint = helper.getBlockEntity(cablePos);

        helper.assertTrue(
                helper.getLevel().getCapability(
                                ModCapabilities.ENERGY,
                                helper.absolutePos(machinePos),
                                Direction.SOUTH)
                        == null,
                "Processing machine exposed an ENERGY capability on a non-input face");

        long nominal = machine.variant().tierBand().inputNominal();
        long offered = Math.addExact(
                nominal, cable.transportProperties().lossPerMeter());
        helper.assertTrue(
                endpoint.insert(
                                EnergyType.ELECTRIC,
                                offered,
                                1L,
                                Direction.DOWN,
                                false)
                        == 1L
                        && machine.stored(EnergyType.ELECTRIC) == nominal,
                "EU cable delivery did not reach the GT6 bottom input face");
        helper.succeed();
    }
}
