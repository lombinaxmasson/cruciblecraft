package com.masson.cruciblecraft.content.item.tool;

import java.util.function.Predicate;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;

/**
 * GT6 {@code Behavior_Place_Sapling} / {@code Behavior_Place_Workbench} /
 * {@code Behavior_Place_Torch}: consume a matching inventory stack, not the
 * tool itself.
 */
public final class InventoryBlockPlacer {
    private InventoryBlockPlacer() {}

    public static InteractionResult placeSaplingOrWorkbench(UseOnContext context) {
        InteractionResult sapling = placeSapling(context);
        if (sapling.consumesAction()) {
            return sapling;
        }
        return placeCraftingTable(context);
    }

    public static InteractionResult placeSapling(UseOnContext context) {
        return place(context, stack -> stack.is(ItemTags.SAPLINGS)
                && stack.getItem() instanceof BlockItem);
    }

    public static InteractionResult placeCraftingTable(UseOnContext context) {
        return place(context, stack -> stack.is(Items.CRAFTING_TABLE));
    }

    public static InteractionResult placeTorch(UseOnContext context) {
        return place(context, stack -> stack.is(Items.TORCH)
                || stack.is(Items.SOUL_TORCH)
                || stack.is(Blocks.TORCH.asItem()));
    }

    public static InteractionResult plugLeak(UseOnContext context) {
        if (!nextToLiquid(context)) {
            return InteractionResult.PASS;
        }
        return place(context, InventoryBlockPlacer::leakPlugStack);
    }

    private static boolean nextToLiquid(UseOnContext context) {
        var level = context.getLevel();
        var destination = context.getClickedPos().relative(context.getClickedFace());
        for (var direction : net.minecraft.core.Direction.values()) {
            if (!level.getFluidState(destination.relative(direction)).isEmpty()) {
                return true;
            }
        }
        return !level.getFluidState(destination).isEmpty();
    }

    private static boolean leakPlugStack(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }
        var block = blockItem.getBlock();
        var state = block.defaultBlockState();
        if (!state.canOcclude()
                || block instanceof net.minecraft.world.level.block.EntityBlock
                || block instanceof net.minecraft.world.level.block.InfestedBlock
                || state.is(net.neoforged.neoforge.common.Tags.Blocks.ORES)
                || state.is(net.neoforged.neoforge.common.Tags.Blocks.STORAGE_BLOCKS)
                || state.is(BlockTags.NEEDS_DIAMOND_TOOL)
                || state.is(Blocks.OBSIDIAN)
                || state.is(Blocks.BEDROCK)) {
            return false;
        }
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE)
                || state.is(BlockTags.MINEABLE_WITH_SHOVEL);
    }

    public static InteractionResult place(
            UseOnContext context, Predicate<ItemStack> match) {
        Player player = context.getPlayer();
        if (player == null || !player.mayBuild()) {
            return InteractionResult.PASS;
        }
        Inventory inventory = player.getInventory();
        for (int index = inventory.getContainerSize() - 1; index >= 0; index--) {
            ItemStack candidate = inventory.getItem(index);
            if (candidate.isEmpty() || !match.test(candidate)) {
                continue;
            }
            ItemStack copy = candidate.copy();
            UseOnContext placed = new UseOnContext(
                    context.getLevel(),
                    player,
                    context.getHand(),
                    candidate,
                    ToolClick.hit(context));
            InteractionResult result = candidate.useOn(placed);
            if (result.consumesAction()) {
                if (!player.getAbilities().instabuild && candidate.isEmpty()) {
                    inventory.setItem(index, ItemStack.EMPTY);
                }
                return result;
            }
            if (ItemStack.matches(copy, candidate)) {
                continue;
            }
            return result;
        }
        return InteractionResult.PASS;
    }
}
