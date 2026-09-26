package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.block.CableBlock;
import com.masson.cruciblecraft.content.block.RedstoneWireBlock;
import com.masson.cruciblecraft.content.blockentity.RedstoneWireBlockEntity;
import com.masson.cruciblecraft.content.item.CableBlockItem;
import com.masson.cruciblecraft.content.item.RedstoneWireBlockItem;
import com.masson.cruciblecraft.content.redstonewire.RedstoneWireKind;
import com.masson.cruciblecraft.content.redstonewire.RedstoneWireSinks;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModCapabilities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated GT6 redstone-wire behavior correction. Run with
 * {@code -PgameTestGrid=content}.
 */
@GameTestHolder(RedstoneWireCorrectionGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class RedstoneWireCorrectionGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_content";
    private static final String TEMPLATE = "empty";

    private RedstoneWireCorrectionGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void redstoneMixedMaterialUsesSenderLoss(GameTestHelper helper) {
        BlockPos source = new BlockPos(4, 2, 2);
        BlockPos alloyPos = new BlockPos(3, 2, 2);
        BlockPos signalumPos = new BlockPos(2, 2, 2);
        helper.setBlock(source, Blocks.REDSTONE_BLOCK);
        helper.setBlock(
                alloyPos,
                connected(
                        RedstoneWireKind.RED_ALLOY,
                        Direction.EAST,
                        Direction.WEST));
        helper.setBlock(
                signalumPos,
                connected(RedstoneWireKind.SIGNALUM, Direction.EAST));
        helper.startSequence()
                .thenExecuteAfter(8, () -> {
                    RedstoneWireBlockEntity alloy = wireAt(helper, alloyPos);
                    RedstoneWireBlockEntity signalum = wireAt(helper, signalumPos);
                    helper.assertTrue(
                            alloy.kind() == RedstoneWireKind.RED_ALLOY
                                    && signalum.kind() == RedstoneWireKind.SIGNALUM,
                            "mixed hop kinds drifted");
                    helper.assertTrue(
                            alloy.redstoneValue() > 0L,
                            "red alloy did not accept vanilla input");
                    long senderLoss = alloy.minusLoss();
                    long receiverLoss =
                            alloy.redstoneValue() - signalum.kind().loss();
                    helper.assertTrue(
                            senderLoss != receiverLoss,
                            "sender and receiver loss collapsed; mixed hop is not observable");
                    helper.assertTrue(
                            signalum.redstoneValue() == senderLoss,
                            "mixed hop used receiver loss: signalum="
                                    + signalum.redstoneValue()
                                    + " sender="
                                    + senderLoss
                                    + " receiver="
                                    + receiverLoss);
                    helper.assertTrue(
                            signalum.redstoneValue() != receiverLoss,
                            "mixed hop matched receiver loss");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void redstoneVanillaInputCachedPerTick(GameTestHelper helper) {
        BlockPos source = new BlockPos(3, 2, 2);
        BlockPos wirePos = new BlockPos(2, 2, 2);
        helper.setBlock(source, Blocks.REDSTONE_BLOCK);
        helper.setBlock(
                wirePos,
                connected(RedstoneWireKind.RED_ALLOY, Direction.EAST));
        helper.startSequence()
                .thenExecuteAfter(8, () -> {
                    RedstoneWireBlockEntity wire = wireAt(helper, wirePos);
                    helper.assertTrue(
                            wire.vanillaSideCache(Direction.EAST) > 0,
                            "vanilla east cache stayed empty");
                    helper.assertTrue(
                            wire.vanillaSideCache(Direction.WEST) < 0,
                            "unconnected west was cached");
                    helper.assertTrue(
                            wire.redstoneValue() > 0L,
                            "cached vanilla input did not raise redstone");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void redstoneSinksIgnored(GameTestHelper helper) {
        helper.assertTrue(
                RedstoneWireSinks.isSink(Blocks.DROPPER)
                        && RedstoneWireSinks.isSink(Blocks.DISPENSER)
                        && RedstoneWireSinks.isSink(Blocks.TNT)
                        && RedstoneWireSinks.isSink(Blocks.NOTE_BLOCK)
                        && RedstoneWireSinks.isSink(Blocks.REDSTONE_LAMP)
                        && RedstoneWireSinks.isSink(Blocks.POWERED_RAIL)
                        && RedstoneWireSinks.isSink(Blocks.IRON_DOOR)
                        && RedstoneWireSinks.isSink(Blocks.OAK_TRAPDOOR)
                        && RedstoneWireSinks.isSink(Blocks.PISTON),
                "GT6 REDSTONE_SINKS mapping shrank");
        helper.assertFalse(
                RedstoneWireSinks.isSink(Blocks.REDSTONE_BLOCK),
                "redstone block is not a sink");
        BlockPos source = new BlockPos(4, 2, 2);
        BlockPos dropper = new BlockPos(3, 2, 2);
        BlockPos wirePos = new BlockPos(2, 2, 2);
        helper.setBlock(source, Blocks.REDSTONE_BLOCK);
        helper.setBlock(dropper, Blocks.DROPPER);
        helper.setBlock(
                wirePos,
                connected(RedstoneWireKind.RED_ALLOY, Direction.EAST));
        helper.startSequence()
                .thenExecuteAfter(8, () -> {
                    RedstoneWireBlockEntity wire = wireAt(helper, wirePos);
                    helper.assertTrue(
                            wire.redstoneValue() <= 0L && wire.visual() == 0,
                            "dropper sink leaked vanilla redstone into the wire");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void redstoneNotCableBlock(GameTestHelper helper) {
        BlockPos wirePos = new BlockPos(2, 2, 2);
        helper.setBlock(
                wirePos,
                connected(RedstoneWireKind.RED_ALLOY, Direction.NORTH));
        for (RedstoneWireKind kind : RedstoneWireKind.all()) {
            Item item = BuiltInRegistries.ITEM.get(kind.id());
            Block block = ModBlocks.redstoneWireBlocksById().get(kind.id()).get();
            helper.assertTrue(
                    item instanceof RedstoneWireBlockItem
                            && !(item instanceof CableBlockItem)
                            && !(block instanceof CableBlock)
                            && !(block instanceof RedStoneWireBlock),
                    "redstone identity became an EU cable or vanilla dust: "
                            + kind.path());
        }
        helper.assertTrue(
                helper.getBlockEntity(wirePos) instanceof RedstoneWireBlockEntity,
                "redstone BE missing");
        IEnergyHandler energy = helper.getLevel().getCapability(
                ModCapabilities.ENERGY,
                helper.absolutePos(wirePos),
                Direction.NORTH);
        helper.assertTrue(
                energy == null,
                "redstone wire exposed ENERGY");
        helper.succeed();
    }

    private static RedstoneWireBlockEntity wireAt(
            GameTestHelper helper, BlockPos pos) {
        if (!(helper.getBlockEntity(pos) instanceof RedstoneWireBlockEntity wire)) {
            helper.fail("missing redstone wire at " + pos);
            throw new IllegalStateException("unreachable");
        }
        return wire;
    }

    private static BlockState connected(
            RedstoneWireKind kind, Direction... sides) {
        BlockState state = ModBlocks.redstoneWireBlocksById()
                .get(kind.id())
                .get()
                .defaultBlockState();
        for (Direction side : sides) {
            state = state.setValue(
                    RedstoneWireBlock.PROPERTY_BY_DIRECTION.get(side), true);
        }
        return state;
    }
}
