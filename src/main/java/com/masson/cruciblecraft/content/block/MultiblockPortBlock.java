package com.masson.cruciblecraft.content.block;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.api.tool.ToolInteractable;
import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.content.blockentity.MultiblockPortBlockEntity;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PortType;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverBlockInteraction;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/** One registry block for one capability-safe multiblock port type. */
public final class MultiblockPortBlock extends Block
        implements EntityBlock, ToolInteractable {
    /**
     * Formed distillation-tower parts use GT6 {@code distillationtowerparts}
     * / {@code heatacceptor} cubes instead of the generic port texture.
     */
    public static final BooleanProperty TOWER_SKIN =
            BooleanProperty.create("tower_skin");
    /**
     * GT6 18102 design 1: backside-center hole of each 18102 layer.
     */
    public static final BooleanProperty BACK_HOLE =
            BooleanProperty.create("back_hole");

    private final PortType portType;

    public MultiblockPortBlock(PortType portType, Properties properties) {
        super(properties);
        this.portType = portType;
        registerDefaultState(stateDefinition.any()
                .setValue(TOWER_SKIN, false)
                .setValue(BACK_HOLE, false));
    }

    public PortType portType() {
        return portType;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TOWER_SKIN, BACK_HOLE);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MultiblockPortBlockEntity(pos, state);
    }

    @Override
    public ToolResult useTool(ToolAction action, UseOnContext context) {
        return DistillationTowerParts.useTool(action, context);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit) {
        return DistillationTowerParts.useWithoutItem(level, pos, player, hit);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            Direction direction) {
        return MachineCoverBlockInteraction.weakRedstone(
                level, pos, direction);
    }

    @Override
    protected int getDirectSignal(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            Direction direction) {
        return MachineCoverBlockInteraction.directRedstone(
                level, pos, direction);
    }

    @Override
    public boolean canConnectRedstone(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            @Nullable Direction direction) {
        return MachineCoverBlockInteraction.canConnectRedstone(
                level, pos, direction);
    }

    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState next,
            boolean moved) {
        if (!state.is(next.getBlock())) {
            DistillationTowerParts.dropCovers(level, pos);
        }
        super.onRemove(state, level, pos, next, moved);
    }
}
