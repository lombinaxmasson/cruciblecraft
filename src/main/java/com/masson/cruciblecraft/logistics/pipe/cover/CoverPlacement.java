package com.masson.cruciblecraft.logistics.pipe.cover;

import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.content.block.Gt6StyleConnections;
import com.masson.cruciblecraft.content.blockentity.CableBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ItemPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.RedstoneWireBlockEntity;
import com.masson.cruciblecraft.content.item.PipeCoverItem;
import com.masson.cruciblecraft.content.item.ProgrammedCircuitItem;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;

/**
 * GT6 cover targeting for {@code ITileEntityCoverable}.
 *
 * <p>{@code TileEntityBase10ConnectorRendered.usePipePlacementMode} is true,
 * so pipes/cables/wires place and wrench via
 * {@code UT.Code.getSideWrenching} (the 3×3 grid). Machines keep the clicked
 * face. {@code isUsingWrenchingOverlay} also returns true for any
 * {@code CoverRegistry} stack, and {@code shrunkBox} becomes a full cube
 * once any cover is present.
 */
public final class CoverPlacement {
    private CoverPlacement() {}

    public static boolean isCoverStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof PipeCoverItem
                || stack.getItem() instanceof ProgrammedCircuitItem
                || stack.is(Items.REDSTONE_TORCH)
                || stack.is(Items.REPEATER)) {
            return true;
        }
        if (DecorativeCovers.fromItem(stack) != null) {
            return true;
        }
        return MaterialUnits.resolve(stack)
                .map(entry -> PlateCovers.isCoverForm(entry.form()))
                .orElse(false);
    }

    public static boolean holdingCover(CollisionContext context) {
        if (!(context instanceof EntityCollisionContext entityContext)) {
            return false;
        }
        Entity entity = entityContext.getEntity();
        return entity instanceof Player player
                && isCoverStack(player.getMainHandItem());
    }

    public static boolean hasAnyCover(BlockEntity blockEntity) {
        return CoverCollision.any(blockEntity);
    }

    public static boolean hasCover(BlockEntity blockEntity, Direction side) {
        return side != null
                && CoverCollision.coversOn(blockEntity).containsKey(side);
    }

    /**
     * GT6 connectors (pipes, cables, redstone wires) use the 9-grid.
     * Machines and other hosts use the clicked face.
     */
    public static boolean usesPipePlacement(BlockEntity blockEntity) {
        return blockEntity instanceof ItemPipeBlockEntity
                || blockEntity instanceof FluidPipeBlockEntity
                || blockEntity instanceof CableBlockEntity
                || blockEntity instanceof RedstoneWireBlockEntity;
    }

    public static Direction connectorPlaceSide(BlockHitResult hit) {
        return Gt6StyleConnections.sideFromHit(hit);
    }

    public static Direction placeSide(
            BlockEntity blockEntity, BlockHitResult hit) {
        if (hit == null) {
            return null;
        }
        if (usesPipePlacement(blockEntity)) {
            return connectorPlaceSide(hit);
        }
        return hit.getDirection();
    }

    public static Direction placeSide(UseOnContext context) {
        if (context == null) {
            return null;
        }
        return placeSide(
                context.getLevel().getBlockEntity(context.getClickedPos()),
                hit(context));
    }

    /**
     * GT6 {@code onToolClick}: if the clicked face already has a cover, that
     * face wins; otherwise connectors fall through to the 9-grid side.
     */
    public static Direction interactSide(
            BlockEntity blockEntity, BlockHitResult hit) {
        if (hit == null) {
            return null;
        }
        Direction clicked = hit.getDirection();
        if (usesPipePlacement(blockEntity) && !hasCover(blockEntity, clicked)) {
            return connectorPlaceSide(hit);
        }
        return clicked;
    }

    public static Direction interactSide(UseOnContext context) {
        if (context == null) {
            return null;
        }
        return interactSide(
                context.getLevel().getBlockEntity(context.getClickedPos()),
                hit(context));
    }

    public static BlockHitResult hit(UseOnContext context) {
        return new BlockHitResult(
                context.getClickLocation(),
                context.getClickedFace(),
                context.getClickedPos(),
                context.isInside());
    }
}
