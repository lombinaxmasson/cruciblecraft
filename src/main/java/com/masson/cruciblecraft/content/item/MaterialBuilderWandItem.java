package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.content.item.tool.InventoryBlockPlacer;
import com.masson.cruciblecraft.content.item.tool.ToolClick;
import com.masson.cruciblecraft.content.multiblock.MultiblockBuilderInteraction;
import com.masson.cruciblecraft.machine.ToolMaterialRules;
import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6 {@code Behavior_Builderwand}: copy the clicked block onto the clicked
 * face across a quality+1 radius in the face plane, consuming matching
 * inventory stacks.
 */
public final class MaterialBuilderWandItem extends MaterialToolItem {
    public MaterialBuilderWandItem(Properties properties) {
        super(
                properties,
                ToolKind.BUILDER_WAND,
                "item.cruciblecraft.material_builder_wand");
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (canApplyDurabilityDamage(context.getItemInHand())) {
            InteractionResult multiblock =
                    MultiblockBuilderInteraction.useOn(context);
            if (multiblock.consumesAction()) {
                return multiblock;
            }
        }
        InteractionResult tool = super.useOn(context);
        if (tool.consumesAction()) {
            return tool;
        }
        return placeCopies(context);
    }

    private InteractionResult placeCopies(UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack wand = context.getItemInHand();
        if (player == null || !canApplyDurabilityDamage(wand)) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockPos origin = context.getClickedPos();
        BlockState sample = level.getBlockState(origin);
        if (sample.isAir() || sample.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) {
            return InteractionResult.PASS;
        }
        int distance = material(wand)
                .map(materialId -> ToolMaterialRules.requireStats(
                        kind(), materialId).quality() + 1)
                .orElse(1);
        Direction face = context.getClickedFace();
        boolean placed = false;
        for (int dx = face.getAxis() == Direction.Axis.X ? 0 : -distance;
                dx <= (face.getAxis() == Direction.Axis.X ? 0 : distance);
                dx++) {
            for (int dy = face.getAxis() == Direction.Axis.Y ? 0 : -distance;
                    dy <= (face.getAxis() == Direction.Axis.Y ? 0 : distance);
                    dy++) {
                for (int dz = face.getAxis() == Direction.Axis.Z ? 0 : -distance;
                        dz <= (face.getAxis() == Direction.Axis.Z ? 0 : distance);
                        dz++) {
                    BlockPos source = origin.offset(dx, dy, dz);
                    BlockState found = level.getBlockState(source);
                    if (!sameBuildBlock(sample, found)) {
                        continue;
                    }
                    if (placeAgainst(context, player, source, face, sample.getBlock())) {
                        placed = true;
                        if (!player.getAbilities().instabuild) {
                            ToolClick.hurt(wand, player, context.getHand());
                            if (!canApplyDurabilityDamage(wand)) {
                                return InteractionResult.SUCCESS;
                            }
                        }
                    }
                }
            }
        }
        if (placed && !level.isClientSide) {
            level.playSound(
                    null,
                    origin,
                    SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.BLOCKS,
                    0.4F,
                    1.2F);
        }
        return placed
                ? InteractionResult.sidedSuccess(level.isClientSide)
                : InteractionResult.PASS;
    }

    private static boolean sameBuildBlock(BlockState sample, BlockState found) {
        return found.getBlock() == sample.getBlock();
    }

    private static boolean placeAgainst(
            UseOnContext context,
            Player player,
            BlockPos source,
            Direction face,
            Block expected) {
        Level level = context.getLevel();
        BlockPos target = source.relative(face);
        if (!level.getBlockState(target).canBeReplaced()
                || !player.mayInteract(level, target)) {
            return false;
        }
        Inventory inventory = player.getInventory();
        for (int index = inventory.getContainerSize() - 1; index >= 0; index--) {
            ItemStack candidate = inventory.getItem(index);
            if (!(candidate.getItem() instanceof BlockItem blockItem)
                    || blockItem.getBlock() != expected) {
                continue;
            }
            BlockPlaceContext place = new BlockPlaceContext(
                    context.getLevel(),
                    player,
                    context.getHand(),
                    candidate,
                    ToolClick.hit(new UseOnContext(
                            level,
                            player,
                            context.getHand(),
                            candidate,
                            new net.minecraft.world.phys.BlockHitResult(
                                    context.getClickLocation(),
                                    face,
                                    source,
                                    context.isInside()))));
            if (blockItem.place(place).consumesAction()) {
                return true;
            }
        }
        return InventoryBlockPlacer.place(
                        new UseOnContext(
                                level,
                                player,
                                context.getHand(),
                                context.getItemInHand(),
                                new net.minecraft.world.phys.BlockHitResult(
                                        context.getClickLocation(),
                                        face,
                                        source,
                                        context.isInside())),
                        stack -> stack.getItem() instanceof BlockItem blockItem
                                && blockItem.getBlock() == expected)
                .consumesAction();
    }
}
