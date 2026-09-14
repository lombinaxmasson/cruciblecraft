package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.AbstractPipeBlock;
import com.masson.cruciblecraft.content.block.FluidPipeBlock;
import com.masson.cruciblecraft.content.block.Gt6StyleConnections;
import com.masson.cruciblecraft.content.block.ItemPipeBlock;
import com.masson.cruciblecraft.content.block.RedstoneWireBlock;
import com.masson.cruciblecraft.content.blockentity.ItemPipeBlockEntity;
import com.masson.cruciblecraft.content.item.PipeBlockItem;
import com.masson.cruciblecraft.content.redstonewire.RedstoneWireKind;
import com.masson.cruciblecraft.logistics.pipe.PipeCatalog;
import com.masson.cruciblecraft.logistics.pipe.PipeTransferPhase;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Isolated GT6 ordinary item-pipe runtime. Run with
 * {@code -PwaveRecipes=content/gt6-item-pipe-runtime}.
 */
@GameTestHolder(ItemPipeRuntimeGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class ItemPipeRuntimeGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_content_gt6_item_pipe_runtime";
    private static final String TEMPLATE = "empty";

    private ItemPipeRuntimeGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void itemPipeHasInternalInventory(GameTestHelper helper) {
        ItemPipeBlockEntity pipe = placePipe(
                helper,
                new BlockPos(2, 2, 2),
                itemPipe("brass", MaterialPrefixes.ITEM_PIPE),
                Direction.WEST,
                Direction.EAST);
        helper.assertTrue(
                pipe.inventorySize() == 1,
                "brass medium invSize drifted: " + pipe.inventorySize());
        IItemHandler handler = pipe.itemHandler(Direction.WEST);
        helper.assertTrue(handler != null, "missing sided handler");
        ItemStack leftover = handler.insertItem(
                0, new ItemStack(Items.IRON_INGOT, 8), false);
        helper.assertTrue(leftover.isEmpty(), "in-pipe insert was rejected");
        helper.assertTrue(
                !handler.getStackInSlot(0).isEmpty()
                        && handler.getStackInSlot(0).getCount() == 8,
                "getStackInSlot stayed empty after a live insert");
        helper.assertTrue(
                pipe.stackInSlot(0).getCount() == 8,
                "persisted inventory did not hold the inserted stack");
        ItemStack extracted = handler.extractItem(0, 3, false);
        helper.assertTrue(
                extracted.getCount() == 3
                        && pipe.stackInSlot(0).getCount() == 5,
                "extractItem did not read the in-pipe inventory");
        helper.assertTrue(
                PipeTransferPhase.INTERVAL == 5
                        && ItemPipeBlockEntity.SEND_INTERVAL == 10,
                "cover/send cadence drifted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void itemPipeDisabledInputsOutputs(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        ItemPipeBlockEntity pipe = placePipe(
                helper,
                pos,
                itemPipe("brass", MaterialPrefixes.ITEM_PIPE),
                Direction.WEST,
                Direction.EAST);
        helper.assertTrue(
                pipe.cycleDisabledIo(Direction.EAST),
                "first monkey-wrench cycle failed");
        helper.assertTrue(
                !pipe.isInputDisabled(Direction.EAST)
                        && pipe.isOutputDisabled(Direction.EAST),
                "first cycle did not disable only output");
        IItemHandler west = pipe.itemHandler(Direction.WEST);
        west.insertItem(0, new ItemStack(Items.IRON_INGOT, 4), false);
        helper.assertTrue(
                pipe.stackInSlot(0).getCount() == 4,
                "disabled output blocked incoming on another face");
        helper.assertTrue(
                pipe.cycleDisabledIo(Direction.WEST),
                "input-disable cycle failed");
        helper.assertTrue(
                pipe.cycleDisabledIo(Direction.WEST),
                "second input cycle failed");
        helper.assertTrue(
                pipe.isInputDisabled(Direction.WEST),
                "WEST input was not disabled");
        ItemStack rejected = west.insertItem(
                0, new ItemStack(Items.GOLD_INGOT, 1), false);
        helper.assertTrue(
                rejected.getCount() == 1
                        && pipe.stackInSlot(0).getItem() == Items.IRON_INGOT,
                "disabled input still accepted a foreign stack");
        helper.setBlock(
                pos.west(),
                pipeState(
                        itemPipe("brass", MaterialPrefixes.ITEM_PIPE),
                        Direction.EAST));
        helper.assertTrue(
                helper.getBlockState(pos.west()).getBlock()
                        instanceof ItemPipeBlock,
                "neighbor pipe missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void itemPipeTenTickSend(GameTestHelper helper) {
        BlockPos pipePos = new BlockPos(2, 2, 2);
        BlockPos chestPos = new BlockPos(3, 2, 2);
        ItemPipeBlockEntity pipe = placePipe(
                helper,
                pipePos,
                itemPipe("brass", MaterialPrefixes.ITEM_PIPE),
                Direction.WEST,
                Direction.EAST);
        helper.setBlock(chestPos, Blocks.CHEST);
        IItemHandler handler = pipe.itemHandler(Direction.WEST);
        handler.insertItem(0, new ItemStack(Items.IRON_INGOT, 16), false);
        helper.assertTrue(
                pipe.stackInSlot(0).getCount() == 16,
                "ten-tick source did not store");
        helper.startSequence()
                .thenExecuteAfter(1, () -> {
                    helper.assertTrue(
                            pipe.stackInSlot(0).getCount() == 16,
                            "in-pipe inventory sent on the 5-tick cover phase");
                    ChestBlockEntity chest = helper.getBlockEntity(chestPos);
                    helper.assertTrue(
                            chest.getItem(0).isEmpty(),
                            "chest received items before the 10-tick send");
                })
                .thenIdle(30)
                .thenExecute(() -> {
                    ChestBlockEntity chest = helper.getBlockEntity(chestPos);
                    helper.assertTrue(
                            chest.getItem(0).getCount() == 16
                                    && pipe.stackInSlot(0).isEmpty(),
                            "10-tick send did not empty the pipe into the chest");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void itemPipeRestrictiveStepSize(GameTestHelper helper) {
        ItemPipeBlock brass = itemPipe("brass", MaterialPrefixes.ITEM_PIPE);
        helper.assertTrue(
                brass.pipe().item().stepSize() == 32768L,
                "ordinary brass stepSize drifted: "
                        + brass.pipe().item().stepSize());
        helper.assertTrue(
                itemPipe("brass", MaterialPrefixes.LARGE_ITEM_PIPE)
                                .pipe()
                                .item()
                                .stepSize()
                        == 16384L,
                "ordinary large brass stepSize drifted");
        helper.assertTrue(
                itemPipe("brass", MaterialPrefixes.HUGE_ITEM_PIPE)
                                .pipe()
                                .item()
                                .stepSize()
                        == 8192L,
                "ordinary huge brass stepSize drifted");
        helper.assertTrue(
                !BuiltInRegistries.ITEM.containsKey(
                        ResourceLocation.fromNamespaceAndPath(
                                "cruciblecraft",
                                "item_pipe_tile/restrictive_elementium_item_pipe")),
                "folded restrictive dummy is still registered");
        Item live = ModBlocks.pipeBlock(
                "brass",
                MaterialPrefixes.ITEM_PIPE,
                PipeCatalog.Kind.ITEM).get().asItem();
        helper.assertTrue(
                live instanceof PipeBlockItem,
                "live brass item pipe is not a PipeBlockItem");
        ResourceLocation dummy = ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft",
                "item_pipe_tile/brass");
        helper.assertTrue(
                !BuiltInRegistries.ITEM.containsKey(dummy),
                "folded brass dummy item is still registered");
        BlockPos itemPos = new BlockPos(2, 2, 2);
        helper.setBlock(
                itemPos,
                pipeState(brass, Direction.EAST));
        helper.setBlock(
                new BlockPos(3, 2, 2),
                pipeState(
                        fluidPipe("copper", MaterialPrefixes.FLUID_PIPE),
                        Direction.WEST));
        helper.setBlock(
                new BlockPos(2, 2, 3),
                ModBlocks.electricalConductorBlock(
                        "tin", MaterialPrefixes.WIRE).get().defaultBlockState());
        RedstoneWireBlock redstone =
                (RedstoneWireBlock) ModBlocks.redstoneWireBlocksById()
                        .get(RedstoneWireKind.RED_ALLOY.id())
                        .get();
        helper.setBlock(new BlockPos(2, 2, 1), redstone.defaultBlockState());
        BlockState itemState = helper.getBlockState(itemPos);
        helper.assertTrue(
                !Gt6StyleConnections.sameNetwork(
                        itemState, helper.getBlockState(new BlockPos(3, 2, 2))),
                "item and fluid pipes shared a network");
        helper.assertTrue(
                !Gt6StyleConnections.sameNetwork(
                        itemState, helper.getBlockState(new BlockPos(2, 2, 3))),
                "item pipe joined the EU cable network");
        helper.assertTrue(
                !Gt6StyleConnections.sameNetwork(
                        itemState, helper.getBlockState(new BlockPos(2, 2, 1))),
                "item pipe joined the redstone wire network");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void itemPipeFullDoesNotVoid(GameTestHelper helper) {
        ItemPipeBlockEntity pipe = placePipe(
                helper,
                new BlockPos(2, 2, 2),
                itemPipe("brass", MaterialPrefixes.ITEM_PIPE),
                Direction.WEST);
        IItemHandler handler = pipe.itemHandler(Direction.WEST);
        ItemStack first = handler.insertItem(
                0, new ItemStack(Items.IRON_INGOT, 64), false);
        helper.assertTrue(first.isEmpty(), "full-stack insert failed");
        ItemStack extra = handler.insertItem(
                0, new ItemStack(Items.IRON_INGOT, 16), false);
        helper.assertTrue(
                extra.getCount() == 16
                        && pipe.stackInSlot(0).getCount() == 64,
                "full pipe voided the overflow stack");
        helper.succeed();
    }

    private static ItemPipeBlockEntity placePipe(
            GameTestHelper helper,
            BlockPos pos,
            ItemPipeBlock block,
            Direction... connections) {
        helper.setBlock(pos, pipeState(block, connections));
        return helper.getBlockEntity(pos);
    }

    private static ItemPipeBlock itemPipe(
            String material,
            com.masson.cruciblecraft.api.material.MaterialPrefix form) {
        return (ItemPipeBlock) ModBlocks.pipeBlock(
                material, form, PipeCatalog.Kind.ITEM).get();
    }

    private static FluidPipeBlock fluidPipe(
            String material,
            com.masson.cruciblecraft.api.material.MaterialPrefix form) {
        return (FluidPipeBlock) ModBlocks.pipeBlock(
                material, form, PipeCatalog.Kind.FLUID).get();
    }

    private static BlockState pipeState(
            AbstractPipeBlock block, Direction... connections) {
        BlockState state = block.defaultBlockState();
        for (Direction direction : connections) {
            state = state.setValue(
                    AbstractPipeBlock.PROPERTY_BY_DIRECTION.get(direction),
                    true);
        }
        return state;
    }
}
