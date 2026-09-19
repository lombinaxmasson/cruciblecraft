package com.masson.cruciblecraft.content.item.tool;

import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.content.block.Gt6StyleConnections;
import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ItemPipeBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Tool clicks on vanilla blocks and foreign capability tanks. CrucibleCraft
 * machines answer {@link com.masson.cruciblecraft.api.tool.ToolInteractable}
 * instead of living here.
 *
 * <p>Wrench / screwdriver / pincer vanilla table is {@code ToolCompat} in
 * {@code gt6_code/gregtech6/.../gregapi/block/ToolCompat.java}.
 */
final class VanillaToolAdapters {
    static final int PLUNGER_TANK_MB = 1_000;

    private VanillaToolAdapters() {}

    static ToolResult use(ToolAction action, UseOnContext context) {
        return switch (action) {
            case PLUNGER -> plunger(context);
            case CROWBAR -> crowbarRails(context);
            case SOFT_HAMMER -> softHammer(context);
            case WRENCH, MONKEY_WRENCH -> wrench(context);
            case SCREWDRIVER -> screwdriver(context);
            case PINCERS -> pincers(context);
            case PROSPECTOR -> ToolProspecting.use(context);
            case SHEARS, WIRE_CUTTER -> tripwire(context);
            case IGNITER -> igniter(context);
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

    private static ToolResult wrench(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof BaseRailBlock
                || block instanceof DiodeBlock
                || block instanceof net.minecraft.world.level.block.piston
                        .PistonHeadBlock) {
            return ToolResult.PASS;
        }
        if (block == Blocks.CRAFTING_TABLE || block == Blocks.BOOKSHELF) {
            return dismantle(context);
        }
        Direction target = Gt6StyleConnections.sideFromHit(ToolClick.hit(context));
        if (block instanceof ChestBlock
                || block instanceof EnderChestBlock
                || block == Blocks.FURNACE
                || block == Blocks.BLAST_FURNACE
                || block == Blocks.SMOKER
                || block == Blocks.CARVED_PUMPKIN
                || block == Blocks.JACK_O_LANTERN) {
            if (!state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                return ToolResult.PASS;
            }
            Direction current = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            if (!target.getAxis().isHorizontal()) {
                return ToolResult.PASS;
            }
            if (target == current) {
                return dismantle(context);
            }
            return setFacing(
                    context,
                    state.setValue(BlockStateProperties.HORIZONTAL_FACING, target));
        }
        if (block instanceof PistonBaseBlock
                || block instanceof DispenserBlock) {
            if (!state.hasProperty(BlockStateProperties.FACING)) {
                return ToolResult.PASS;
            }
            Direction current = state.getValue(BlockStateProperties.FACING);
            if (target == current) {
                return dismantle(context);
            }
            return setFacing(
                    context, state.setValue(BlockStateProperties.FACING, target));
        }
        if (block instanceof HopperBlock) {
            if (!state.hasProperty(BlockStateProperties.FACING_HOPPER)) {
                return ToolResult.PASS;
            }
            Direction current = state.getValue(BlockStateProperties.FACING_HOPPER);
            if (target == Direction.UP) {
                return ToolResult.PASS;
            }
            if (target == current) {
                return dismantle(context);
            }
            return setFacing(
                    context,
                    state.setValue(BlockStateProperties.FACING_HOPPER, target));
        }
        if (state.hasProperty(BlockStateProperties.AXIS)) {
            return setFacing(context, state.cycle(BlockStateProperties.AXIS));
        }
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                && target.getAxis().isHorizontal()) {
            Direction current =
                    state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            if (target == current) {
                return ToolResult.PASS;
            }
            return setFacing(
                    context,
                    state.setValue(BlockStateProperties.HORIZONTAL_FACING, target));
        }
        if (state.hasProperty(BlockStateProperties.FACING)) {
            Direction current = state.getValue(BlockStateProperties.FACING);
            if (target == current) {
                return ToolResult.PASS;
            }
            return setFacing(
                    context, state.setValue(BlockStateProperties.FACING, target));
        }
        return ToolResult.PASS;
    }

    private static ToolResult screwdriver(UseOnContext context) {
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        if (!(state.getBlock() instanceof DiodeBlock)
                || !state.hasProperty(BlockStateProperties.DELAY)) {
            return ToolResult.PASS;
        }
        if (context.getLevel().isClientSide) {
            return ToolResult.SUCCESS;
        }
        BlockState next = state.cycle(BlockStateProperties.DELAY);
        if (next == state) {
            return ToolResult.PASS;
        }
        context.getLevel().setBlock(
                context.getClickedPos(), next, Block.UPDATE_ALL);
        ToolClick.hurt(context);
        return ToolResult.SUCCESS;
    }

    private static ToolResult pincers(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.getBlockState(pos).is(Blocks.DRAGON_EGG)) {
            return ToolResult.PASS;
        }
        Player player = context.getPlayer();
        if (player == null) {
            return ToolResult.PASS;
        }
        if (level.isClientSide) {
            return ToolResult.SUCCESS;
        }
        level.removeBlock(pos, false);
        ToolClick.give(player, new ItemStack(Items.DRAGON_EGG));
        ToolClick.hurt(context);
        return ToolResult.SUCCESS;
    }

    private static ToolResult tripwire(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof TripWireBlock)
                || !state.hasProperty(TripWireBlock.DISARMED)) {
            return ToolResult.PASS;
        }
        if (level.isClientSide) {
            return ToolResult.SUCCESS;
        }
        BlockState disarmed = state.setValue(TripWireBlock.DISARMED, true);
        level.setBlock(pos, disarmed, 4);
        if (!level.destroyBlock(pos, true, context.getPlayer())) {
            return ToolResult.PASS;
        }
        ToolClick.hurt(context);
        level.playSound(
                null,
                pos,
                SoundEvents.SHEEP_SHEAR,
                SoundSource.BLOCKS,
                1.0F,
                1.0F);
        return ToolResult.SUCCESS;
    }

    private static ToolResult igniter(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof TntBlock tnt)) {
            return ToolResult.PASS;
        }
        if (level.isClientSide) {
            return ToolResult.SUCCESS;
        }
        Player player = context.getPlayer();
        tnt.onCaughtFire(
                state, level, pos, context.getClickedFace(), player);
        level.removeBlock(pos, false);
        ToolClick.hurt(context);
        return ToolResult.SUCCESS;
    }

    private static ToolResult dismantle(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return ToolResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (!level.destroyBlock(pos, true, player)) {
            return ToolResult.PASS;
        }
        ToolClick.hurt(context);
        return ToolResult.SUCCESS;
    }

    private static ToolResult setFacing(UseOnContext context, BlockState next) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return ToolResult.SUCCESS;
        }
        if (!level.setBlock(context.getClickedPos(), next, Block.UPDATE_ALL)) {
            return ToolResult.PASS;
        }
        ToolClick.hurt(context);
        return ToolResult.SUCCESS;
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
