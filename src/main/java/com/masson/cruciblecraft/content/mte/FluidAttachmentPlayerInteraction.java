package com.masson.cruciblecraft.content.mte;

import java.util.Optional;

import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/**
 * GT6-style no-GUI click handling for fluid attachments.
 *
 * <p>Redstone automation remains in {@link MteInPlaceBlockEntity}; this class
 * owns player/container interaction only.
 */
public final class FluidAttachmentPlayerInteraction {
    private FluidAttachmentPlayerInteraction() {}

    public static ItemInteractionResult useItemOn(
            MteInPlaceSpec spec,
            ItemStack stack,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (!MteFluidAttachmentProfile.contains(spec)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        Optional<IFluidHandlerItem> container =
                FluidUtil.getFluidHandler(stack);
        if (container.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        MteFluidAttachmentProfile profile =
                MteFluidAttachmentProfile.require(spec);
        IFluidHandler host = host(level, pos, hit.getBlockPos(), hit.getDirection());
        if (host == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        IFluidHandler filtered =
                FluidAttachmentTransfer.filtered(host, profile);
        int moved = switch (spec.kind()) {
            case TAP, NOZZLE -> FluidAttachmentTransfer.move(
                    filtered,
                    container.orElseThrow(),
                    profile,
                    Integer.MAX_VALUE);
            case FUNNEL, CAP_NOZZLE -> FluidAttachmentTransfer.move(
                    container.orElseThrow(),
                    filtered,
                    profile,
                    Integer.MAX_VALUE);
            default -> 0;
        };
        return moved <= 0
                ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                : replaceContainer(player, hand, container.orElseThrow());
    }

    public static InteractionResult useWithoutItem(
            MteInPlaceSpec spec,
            Level level,
            BlockPos pos,
            BlockState state,
            Player player,
            BlockHitResult hit) {
        if (!spec.kind().attachment()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof MteInPlaceBlockEntity host) {
            if (spec.kind() == MteInPlaceKind.TAP
                    || spec.kind() == MteInPlaceKind.FAUCET) {
                host.transferOnce();
            }
        }
        // GT6 consumes an empty-hand click on Tap/Nozzle even when no
        // compatible output is available. Nozzle has no world-spray target.
        return InteractionResult.CONSUME;
    }

    private static IFluidHandler host(
            Level level,
            BlockPos attachmentPos,
            BlockPos hitPos,
            Direction hitDirection) {
        if (level.getBlockEntity(attachmentPos)
                instanceof MteInPlaceBlockEntity attachment) {
            Direction facing = attachment.getBlockState()
                    .getValue(MteInPlaceBlock.FACING);
            return level.getCapability(
                    Capabilities.FluidHandler.BLOCK,
                    attachmentPos.relative(facing),
                    facing.getOpposite());
        }
        return level.getCapability(
                Capabilities.FluidHandler.BLOCK,
                hitPos.relative(hitDirection),
                hitDirection.getOpposite());
    }

    private static ItemInteractionResult replaceContainer(
            Player player,
            InteractionHand hand,
            IFluidHandlerItem container) {
        player.setItemInHand(hand, container.getContainer());
        return ItemInteractionResult.SUCCESS;
    }
}
