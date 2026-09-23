package com.masson.cruciblecraft.content.multiblock;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

class PortStoreRegistryTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        net.neoforged.fml.loading.LoadingModList.of(
                List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void broadcastsInputsAndRoundRobinsOutputs() {
        FakeHost host = new FakeHost();
        BlockPos inputA = new BlockPos(1, 0, 0);
        BlockPos inputB = new BlockPos(2, 0, 0);
        BlockPos outputA = new BlockPos(3, 0, 0);
        BlockPos outputB = new BlockPos(4, 0, 0);
        List<MultiblockStructureValidator.MatchedPort> ports = List.of(
                new MultiblockStructureValidator.MatchedPort(
                        inputA, MultiblockStructureDefinition.PortType.ITEM_FLUID_IN),
                new MultiblockStructureValidator.MatchedPort(
                        inputB, MultiblockStructureDefinition.PortType.ITEM_FLUID_IN),
                new MultiblockStructureValidator.MatchedPort(
                        outputA, MultiblockStructureDefinition.PortType.ITEM_FLUID_OUT),
                new MultiblockStructureValidator.MatchedPort(
                        outputB, MultiblockStructureDefinition.PortType.ITEM_FLUID_OUT));

        Map<BlockPos, PortStore.Assignment> assignments =
                PortStoreRegistry.assignments(ports, host);

        assertEquals(List.of(0, 1), assignments.get(inputA).itemInputGlobals());
        assertEquals(List.of(0, 1), assignments.get(inputB).itemInputGlobals());
        assertEquals(List.of(2, 4), assignments.get(outputA).itemOutputGlobals());
        assertEquals(List.of(3), assignments.get(outputB).itemOutputGlobals());
        assertEquals(List.of(0), assignments.get(inputA).fluidInputGlobals());
        assertEquals(List.of(1), assignments.get(outputA).fluidOutputGlobals());
    }

    private static final class FakeHost implements MultiblockPortHost {
        private final ItemStackHandler items = new ItemStackHandler(5);
        private final List<FluidTank> tanks = List.of(
                new FluidTank(4_000),
                new FluidTank(4_000));

        @Override public ItemStackHandler inventory() { return items; }
        @Override public List<FluidTank> tanks() { return tanks; }
        @Override public List<Integer> itemInputSlots() { return List.of(0, 1); }
        @Override public List<Integer> itemOutputSlots() { return List.of(2, 3, 4); }
        @Override public List<Integer> fluidInputTanks() { return List.of(0); }
        @Override public List<Integer> fluidOutputTanks() { return List.of(1); }
        @Override public net.minecraft.world.level.block.state.BlockState blockState() {
            return Blocks.AIR.defaultBlockState();
        }
    }
}
