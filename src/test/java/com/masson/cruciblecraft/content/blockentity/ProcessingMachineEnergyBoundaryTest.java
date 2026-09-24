package com.masson.cruciblecraft.content.blockentity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.machine.processing.MachineKindSpec;
import com.masson.cruciblecraft.machine.processing.MachineVariant;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.machine.processing.TierProfile;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.fml.loading.LoadingModList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ProcessingMachineEnergyBoundaryTest {
    private static final ResourceLocation MACHINE_ID = id("ru_boundary_machine");
    private static final ResourceLocation MAP_ID = id("ru_boundary_map");

    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(
                List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void bufferedMachineAccumulatesBelowOperatingVoltage() {
        TestMachine machine = new TestMachine();

        assertEquals(
                1L,
                machine.insertFromMultiblockPort(
                        EnergyType.KINETIC_ROTATION, 511L, 1L, false));
        assertEquals(
                511L,
                machine.stored(EnergyType.KINETIC_ROTATION));

        assertEquals(
                1L,
                machine.insertFromMultiblockPort(
                        EnergyType.KINETIC_ROTATION, 512L, 1L, false));
        assertEquals(
                1_023L,
                machine.stored(EnergyType.KINETIC_ROTATION));

        assertEquals(
                1L,
                machine.insertFromMultiblockPort(
                        EnergyType.KINETIC_ROTATION, 4_097L, 1L, false));
        assertEquals(
                1_023L,
                machine.stored(EnergyType.KINETIC_ROTATION));
    }

    @Test
    void stoppedBufferedMachineRejectsPortEnergy() {
        TestMachine machine = new TestMachine();
        machine.setCoverEnabled(false);

        assertEquals(
                0L,
                machine.insertFromMultiblockPort(
                        EnergyType.KINETIC_ROTATION, 512L, 1L, false));
        assertEquals(
                0L,
                machine.stored(EnergyType.KINETIC_ROTATION));

        machine.setCoverEnabled(true);
        assertEquals(
                1L,
                machine.insertFromMultiblockPort(
                        EnergyType.KINETIC_ROTATION, 512L, 1L, false));
    }

    private static ProcessingMachineSpec spec() {
        return new ProcessingMachineSpec(
                MACHINE_ID,
                MAP_ID,
                () -> new RecipeMap(MAP_ID),
                new ProcessingMachineSpec.SlotLayout(
                        2, List.of(0), List.of(1)),
                new ProcessingMachineSpec.TankLayout(List.of(), List.of()),
                new ProcessingMachineSpec.EnergySpec(
                        EnergyType.KINETIC_ROTATION,
                        ProcessingMachineSpec.EnergyMode.BUFFERED,
                        4_096L,
                        4_096L),
                new ProcessingMachineSpec.SidedIoPolicy(
                        (front, side) ->
                                ProcessingMachineSpec.CapabilityAccess.NONE,
                        (front, side) ->
                                ProcessingMachineSpec.CapabilityAccess.NONE,
                        (front, side) ->
                                ProcessingMachineSpec.CapabilityAccess.NONE),
                recipe -> Optional.empty(),
                ProcessingMachineSpec.BufferPolicy.PAUSE,
                new ProcessingMachineSpec.UiLayout(
                        List.of(
                                new ProcessingMachineSpec.SlotPosition(0, 0),
                                new ProcessingMachineSpec.SlotPosition(18, 0)),
                        new ProcessingMachineSpec.ProgressBar(0, 18, 16, 4),
                        List.of(),
                        List.of()));
    }

    private static MachineVariant variant() {
        ProcessingMachineSpec spec = spec();
        TierProfile tier = new TierProfile(
                id("ru_boundary_tier"),
                "cruciblecraft:test",
                EnergyType.KINETIC_ROTATION,
                512L,
                512L,
                4_096L,
                4_096L,
                64,
                5_000);
        return new MachineVariant(
                id("ru_boundary_variant"),
                new MachineKindSpec(
                        id("ru_boundary_kind"),
                        spec,
                        MachineKindSpec.OverclockPolicy.CHEAP,
                        true),
                tier);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }

    private static final class TestMachine
            extends ProcessingMachineBlockEntity {
        private TestMachine() {
            super(
                    BlockEntityType.FURNACE,
                    BlockPos.ZERO,
                    Blocks.FURNACE.defaultBlockState(),
                    ProcessingMachineEnergyBoundaryTest.variant());
        }

        @Override
        protected Direction machineFront() {
            return Direction.NORTH;
        }
    }
}
