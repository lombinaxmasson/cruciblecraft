package com.masson.cruciblecraft.content.item.tool;

import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ItemPipeBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Tool clicks on vanilla blocks and foreign capability tanks. CrucibleCraft
 * machines answer {@link com.masson.cruciblecraft.api.tool.ToolInteractable}
 * instead of living here.
 */
final class VanillaToolAdapters {
    static final int PLUNGER_TANK_MB = 1_000;

    private VanillaToolAdapters() {}

    static ToolResult use(ToolAction action, UseOnContext context) {
        return switch (action) {
            case PLUNGER -> plunger(context);
            case CROWBAR -> crowbarRails(context);
            case SOFT_HAMMER -> softHammer(context);
            default -> ToolResult.PASS;
        };
    }

    private static ToolResult plunger(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof FluidPipeBlockEntity
                || be instanceof ItemPipeBlockEntity) {
            return ToolResult.REJECT;
        }
        IFluidHandler handler = level.getCapability(
                Capabilities.FluidHandler.BLOCK,
                pos,
                context.getClickedFace());
        if (handler == null) {
            return ToolResult.PASS;
        }
        if (level.isClientSide) {
            return ToolResult.SUCCESS;
        }
        var simulated = handler.drain(
                PLUNGER_TANK_MB, IFluidHandler.FluidAction.SIMULATE);
        if (simulated.isEmpty()) {
            return ToolResult.PASS;
        }
        handler.drain(PLUNGER_TANK_MB, IFluidHandler.FluidAction.EXECUTE);
        ToolClick.hurt(context);
        level.playSound(
                null,
                pos,
                SoundEvents.BUCKET_EMPTY,
                SoundSource.BLOCKS,
                0.6F,
                0.6F);
        return ToolResult.SUCCESS;
    }

    private static ToolResult crowbarRails(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BaseRailBlock)) {
            return ToolResult.PASS;
        }
        if (level.isClientSide) {
            return ToolResult.SUCCESS;
        }
        BlockState rotated = state.rotate(level, pos, Rotation.CLOCKWISE_90);
        if (rotated == state || !level.setBlock(pos, rotated, Block.UPDATE_ALL)) {
            return ToolResult.PASS;
        }
        ToolClick.hurt(context);
        return ToolResult.SUCCESS;
    }

    private static ToolResult softHammer(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (level.isClientSide) {
            return canSoftHammer(state)
                    ? ToolResult.SUCCESS
                    : ToolResult.PASS;
        }
        BlockState next = softHammerResult(state);
        if (next == null || next == state) {
            return ToolResult.PASS;
        }
        level.setBlock(pos, next, Block.UPDATE_ALL);
        ToolClick.hurt(context);
        level.playSound(
                null,
                pos,
                SoundEvents.SLIME_BLOCK_HIT,
                SoundSource.BLOCKS,
                0.6F,
                1.2F);
        return ToolResult.SUCCESS;
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
}
