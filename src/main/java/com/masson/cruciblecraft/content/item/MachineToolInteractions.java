package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.content.block.Gt6StyleConnections;
import com.masson.cruciblecraft.content.block.MassStorageBlock;
import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ItemPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.MassStorageBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * GT6 machine-tool clicks that are not vanilla harvest: plunger on pipes
 * and tanks, crowbar on covers / mass-storage barrels / rails, soft hammer
 * on lamps and powered rails.
 */
public final class MachineToolInteractions {
    public static final int PLUNGER_TANK_MB = 1_000;

    private MachineToolInteractions() {}

    public static boolean isCrowbar(ItemStack stack) {
        return stack.getItem() instanceof MaterialCrowbarItem
                || stack.getItem() instanceof MaterialUniversalSpadeItem;
    }

    public static boolean isPlunger(ItemStack stack) {
        return stack.getItem() instanceof MaterialPlungerItem;
    }

    public static boolean isSoftHammer(ItemStack stack) {
        return stack.getItem() instanceof MaterialSoftHammerItem;
    }

    public static boolean expandsPipeGrid(ItemStack stack) {
        return stack.getItem() instanceof MaterialWrenchItem
                || isCrowbar(stack)
                || isPlunger(stack);
    }

    public static InteractionResult plunger(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        InteractionHand hand = context.getHand();
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof FluidPipeBlockEntity pipe) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            if (pipe.trashContents()) {
                hurt(stack, player, hand);
                level.playSound(
                        null,
                        pos,
                        SoundEvents.BUCKET_EMPTY,
                        SoundSource.BLOCKS,
                        0.6F,
                        0.6F);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
        if (be instanceof ItemPipeBlockEntity pipe) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            if (pipe.ejectRecovery()) {
                hurt(stack, player, hand);
                level.playSound(
                        null,
                        pos,
                        SoundEvents.ITEM_PICKUP,
                        SoundSource.BLOCKS,
                        0.6F,
                        0.6F);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
        IFluidHandler handler = level.getCapability(
                Capabilities.FluidHandler.BLOCK,
                pos,
                context.getClickedFace());
        if (handler == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        var simulated = handler.drain(
                PLUNGER_TANK_MB, IFluidHandler.FluidAction.SIMULATE);
        if (simulated.isEmpty()) {
            return InteractionResult.PASS;
        }
        handler.drain(PLUNGER_TANK_MB, IFluidHandler.FluidAction.EXECUTE);
        hurt(stack, player, hand);
        level.playSound(
                null,
                pos,
                SoundEvents.BUCKET_EMPTY,
                SoundSource.BLOCKS,
                0.6F,
                0.6F);
        return InteractionResult.SUCCESS;
    }

    public static InteractionResult crowbar(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        InteractionHand hand = context.getHand();
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof MassStorageBlock) {
            return pickUpMassStorage(context);
        }
        BlockEntity be = level.getBlockEntity(pos);
        Direction side = Gt6StyleConnections.sideFromHit(hit(context));
        if (be instanceof ItemPipeBlockEntity pipe) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            if (pipe.removeCover(side, player)) {
                hurt(stack, player, hand);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
        if (be instanceof FluidPipeBlockEntity pipe) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            if (pipe.removeCover(side, player)) {
                hurt(stack, player, hand);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
        if (state.getBlock() instanceof BaseRailBlock) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            BlockState rotated = state.rotate(
                    level, pos, Rotation.CLOCKWISE_90);
            if (rotated != state && level.setBlock(pos, rotated, Block.UPDATE_ALL)) {
                hurt(stack, player, hand);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
        return InteractionResult.PASS;
    }

    public static InteractionResult pickUpMassStorage(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof MassStorageBlockEntity storage)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockState state = level.getBlockState(pos);
        ItemStack packed = new ItemStack(state.getBlock());
        storage.saveToItem(packed, level.registryAccess());
        storage.clearContents();
        level.removeBlock(pos, false);
        Player player = context.getPlayer();
        if (player == null || !player.addItem(packed)) {
            Block.popResource(level, pos, packed);
        }
        hurt(context.getItemInHand(), player, context.getHand());
        level.playSound(
                null,
                pos,
                SoundEvents.WOOD_BREAK,
                SoundSource.BLOCKS,
                1.0F,
                1.0F);
        return InteractionResult.SUCCESS;
    }

    public static InteractionResult softHammer(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (level.isClientSide) {
            return canSoftHammer(state)
                    ? InteractionResult.SUCCESS
                    : InteractionResult.PASS;
        }
        BlockState next = softHammerResult(state);
        if (next == null || next == state) {
            return InteractionResult.PASS;
        }
        level.setBlock(pos, next, Block.UPDATE_ALL);
        hurt(context.getItemInHand(), context.getPlayer(), context.getHand());
        level.playSound(
                null,
                pos,
                SoundEvents.SLIME_BLOCK_HIT,
                SoundSource.BLOCKS,
                0.6F,
                1.2F);
        return InteractionResult.SUCCESS;
    }

    private static boolean canSoftHammer(BlockState state) {
        return softHammerResult(state) != null;
    }

    private static BlockState softHammerResult(BlockState state) {
        if (state.getBlock() instanceof RedstoneLampBlock
                && state.hasProperty(RedstoneLampBlock.LIT)) {
            return state.cycle(RedstoneLampBlock.LIT);
        }
        if (state.getBlock() instanceof PoweredRailBlock
                && state.hasProperty(PoweredRailBlock.POWERED)) {
            return state.cycle(PoweredRailBlock.POWERED);
        }
        if (state.hasProperty(BlockStateProperties.POWERED)
                && (state.hasProperty(BlockStateProperties.RAIL_SHAPE)
                        || state.hasProperty(
                                BlockStateProperties.RAIL_SHAPE_STRAIGHT))) {
            return state.cycle(BlockStateProperties.POWERED);
        }
        BlockState rotated = rotateHorizontal(state);
        return rotated == state ? null : rotated;
    }

    private static BlockState rotateHorizontal(BlockState state) {
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.setValue(
                    BlockStateProperties.HORIZONTAL_FACING,
                    state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                            .getClockWise());
        }
        if (state.hasProperty(BlockStateProperties.FACING)) {
            Direction facing = state.getValue(BlockStateProperties.FACING);
            if (facing.getAxis().isHorizontal()) {
                return state.setValue(
                        BlockStateProperties.FACING, facing.getClockWise());
            }
        }
        Property<?> axis = state.getProperties().stream()
                .filter(property -> "axis".equals(property.getName()))
                .findFirst()
                .orElse(null);
        if (axis != null) {
            BlockState cycled = state.cycle(axis);
            if (cycled != state) {
                return cycled;
            }
        }
        return state;
    }

    private static BlockHitResult hit(UseOnContext context) {
        return new BlockHitResult(
                context.getClickLocation(),
                context.getClickedFace(),
                context.getClickedPos(),
                context.isInside());
    }

    static void hurt(
            ItemStack stack, Player player, InteractionHand hand) {
        if (player == null || player.getAbilities().instabuild) {
            return;
        }
        if (stack.getItem() instanceof MaterialToolItem tool
                && !tool.canApplyDurabilityDamage(stack)) {
            return;
        }
        stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
    }
}
