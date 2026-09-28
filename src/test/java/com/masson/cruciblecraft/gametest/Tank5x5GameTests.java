package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.blockentity.TankBlockEntity;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureCatalog;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PredicateKind;
import com.masson.cruciblecraft.content.multiblock.TankControllerProfiles;
import com.masson.cruciblecraft.content.multiblock.TankControllerProfiles.Profile;
import com.masson.cruciblecraft.gametest.support.GameTestRequirements;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** GT6 5×5×5 metal tank. The empty template is 24×12×24. */
@GameTestHolder(Tank5x5GameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class Tank5x5GameTests {
    public static final String NAMESPACE = "cruciblecraft_multiblock";
    private static final String TEMPLATE = "empty";
    private static final BlockPos CONTROLLER = new BlockPos(6, 4, 4);
    private static final Direction FACING = Direction.NORTH;
    private static final int LARGE_STAINLESS = 17042;
    private static final int LARGE_DENSE_ADAMANTIUM = 17065;

    private Tank5x5GameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void tank5x5x5Fidelity(GameTestHelper helper) {
        var structure = MultiblockStructureCatalog.require(
                GameTestRequirements.requirePresent(
                        helper,
                        TankControllerProfiles.findControllerMeta(LARGE_STAINLESS),
                        "missing large stainless tank profile")
                        .structureId());
        long ports = structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.PORT)
                .count();
        long air = structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.AIR)
                .count();
        long controllers = structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.CONTROLLER)
                .count();
        helper.assertTrue(
                structure.structure().size() == 125
                        && ports == 97
                        && air == 27
                        && controllers == 1,
                "5x5x5 tank geometry drifted: "
                        + structure.structure().size()
                        + " positions, "
                        + ports
                        + " ports, "
                        + air
                        + " air");
        var source = GameTestRequirements.requirePresent(
                helper,
                structure.source(),
                "5x5x5 tank has no source provenance");
        helper.assertTrue(
                source.className().endsWith("MultiTileEntityTank5x5x5")
                        && "3703e40308c8c030763fd6297dea8b210d2a77b1"
                                .equals(source.revision()),
                "5x5x5 tank source provenance drifted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void tank5x5x5BareControllerDoesNotForm(
            GameTestHelper helper) {
        Profile profile = GameTestRequirements.requirePresent(
                helper,
                TankControllerProfiles.findControllerMeta(LARGE_STAINLESS),
                "missing large stainless tank profile");
        helper.setBlock(
                CONTROLLER,
                block(profile.controllerId())
                        .defaultBlockState()
                        .setValue(MteInPlaceBlock.FACING, FACING));
        TankBlockEntity tank = helper.getBlockEntity(CONTROLLER);
        TankBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(CONTROLLER),
                helper.getBlockState(CONTROLLER),
                tank);
        helper.assertTrue(
                !tank.structureValid(),
                "Bare large tank valve reported a formed structure");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void tank5x5x5Formation(GameTestHelper helper) {
        TankBlockEntity tank = place(helper, LARGE_STAINLESS);
        BlockPos port = firstPort(helper, tank);
        helper.startSequence()
                .thenIdle(25)
                .thenExecute(() -> {
                    helper.assertTrue(
                            tank.structureValid(),
                            "Large stainless tank did not form");
                    helper.assertTrue(
                            tank.contents().getCapacity() == 8_000_000,
                            "Large stainless capacity drifted: "
                                    + tank.contents().getCapacity());
                    MteInPlaceBlockEntity wall =
                            helper.getBlockEntity(port);
                    int filled = wall.fluidHandler(FACING).fill(
                            new FluidStack(Fluids.WATER, 10_000),
                            IFluidHandler.FluidAction.EXECUTE);
                    helper.assertTrue(
                            filled == 10_000
                                    && tank.contents().getFluidAmount()
                                            == 10_000,
                            "Large tank did not accept fluid through a wall");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void tank5x5x5DenseAdamantiumForms(
            GameTestHelper helper) {
        TankBlockEntity tank = place(helper, LARGE_DENSE_ADAMANTIUM);
        helper.startSequence()
                .thenIdle(25)
                .thenExecute(() -> {
                    helper.assertTrue(
                            tank.structureValid(),
                            "Large dense adamantium tank did not form");
                    helper.assertTrue(
                            tank.contents().getCapacity() == 2_048_000_000,
                            "Large dense adamantium capacity drifted: "
                                    + tank.contents().getCapacity());
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void tank5x5x5Teardown(GameTestHelper helper) {
        TankBlockEntity tank = place(helper, LARGE_STAINLESS);
        BlockPos port = firstPort(helper, tank);
        tank.contents().fill(
                new FluidStack(Fluids.WATER, 10_000),
                IFluidHandler.FluidAction.EXECUTE);
        helper.startSequence()
                .thenIdle(25)
                .thenExecute(() -> {
                    helper.assertTrue(
                            tank.structureValid(),
                            "Large tank was not formed before teardown");
                    helper.setBlock(port, Blocks.AIR.defaultBlockState());
                })
                .thenIdle(25)
                .thenExecute(() -> {
                    helper.assertTrue(
                            !tank.structureValid(),
                            "Breaking a large tank wall left it formed");
                    helper.assertTrue(
                            tank.contents().getFluidAmount() == 10_000,
                            "Large tank contents were lost on teardown");
                })
                .thenSucceed();
    }

    private static TankBlockEntity place(GameTestHelper helper, int meta) {
        Profile profile = GameTestRequirements.requirePresent(
                helper,
                TankControllerProfiles.findControllerMeta(meta),
                "missing tank profile " + meta);
        var structure = MultiblockStructureCatalog.require(
                profile.structureId());
        helper.setBlock(
                CONTROLLER,
                block(profile.controllerId())
                        .defaultBlockState()
                        .setValue(MteInPlaceBlock.FACING, FACING));
        Block wall = block(profile.wallId());
        structure.structure().forEach(element -> {
            BlockPos position = structure.worldPosition(
                    CONTROLLER, FACING, element.offset());
            switch (structure.predicate(element).kind()) {
                case PORT -> helper.setBlock(
                        position, wall.defaultBlockState());
                case AIR -> helper.setBlock(
                        position, Blocks.AIR.defaultBlockState());
                default -> {
                }
            }
        });
        return helper.getBlockEntity(CONTROLLER);
    }

    private static BlockPos firstPort(GameTestHelper helper, TankBlockEntity tank) {
        var structure = MultiblockStructureCatalog.require(
                tank.structureId());
        return GameTestRequirements.requirePresent(
                helper,
                structure.structure().stream()
                        .filter(element -> structure.predicate(element).kind()
                                == PredicateKind.PORT)
                        .map(element -> structure.worldPosition(
                                CONTROLLER, FACING, element.offset()))
                        .findFirst(),
                "large tank structure has no port");
    }

    private static Block block(ResourceLocation id) {
        return ModBlocks.mteInPlaceBlocksById()
                .get(ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID, id.getPath()))
                .get();
    }
}
