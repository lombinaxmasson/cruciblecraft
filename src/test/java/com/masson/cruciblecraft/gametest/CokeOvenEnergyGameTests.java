package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.content.blockentity.CokeOvenBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FirebrickBlockEntity;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureCatalog;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PredicateKind;
import com.masson.cruciblecraft.energy.EnergyPackets;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** GT6 Coke Oven TU and firebrick-part routing. */
@GameTestHolder(CrucibleCraftGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class CokeOvenEnergyGameTests {
    private static final String TEMPLATE = "empty";
    private static final BlockPos CONTROLLER = new BlockPos(6, 3, 6);

    private CokeOvenEnergyGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void cokeOvenFirebrickAcceptsTuEnergy(
            GameTestHelper helper) {
        helper.setBlock(CONTROLLER, ModBlocks.COKE_OVEN.get());
        var structure = MultiblockStructureCatalog.require(
                CokeOvenBlockEntity.STRUCTURE_ID);
        structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.PORT)
                .forEach(element -> helper.setBlock(
                        structure.worldPosition(
                                CONTROLLER,
                                Direction.NORTH,
                                element.offset()),
                        ModBlocks.FIREBRICK.get()));
        CokeOvenBlockEntity oven = helper.getBlockEntity(CONTROLLER);
        helper.assertTrue(oven != null, "Coke oven controller is missing");
        oven.requestBuilderRecheck();
        helper.assertTrue(
                oven.structureValid(),
                "Coke oven did not form with energy-capable firebricks");

        BlockPos brickPos = structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.PORT)
                .map(element -> structure.worldPosition(
                        CONTROLLER, Direction.NORTH, element.offset()))
                .findFirst()
                .orElseThrow();
        FirebrickBlockEntity brick = helper.getBlockEntity(brickPos);
        helper.assertTrue(brick != null, "Coke oven firebrick is missing");
        var energy = brick.energy(Direction.NORTH);
        helper.assertTrue(
                energy != null
                        && energy.handles(EnergyType.TIME, Direction.NORTH),
                "Firebrick did not expose GT6 TU input");
        long accepted = energy.insert(
                EnergyType.TIME,
                EnergyPackets.magnitude(16L),
                1L,
                Direction.NORTH,
                false);
        helper.assertTrue(
                accepted == 1L
                        && oven.stored(EnergyType.TIME) >= 16L,
                "Coke oven did not retain injected TU");
        helper.succeed();
    }
}
