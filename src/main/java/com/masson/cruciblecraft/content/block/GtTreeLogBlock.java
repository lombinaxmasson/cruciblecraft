package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.api.tool.ToolInteractable;
import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.content.item.tool.ToolClick;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.worldgen.tree.prep.GtTreeSpecies;
import com.masson.cruciblecraft.worldgen.tree.prep.GtTreeSpecies.TreeHoleMode;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

/**
 * Growing GT log. Maple and rainbowood convert to a tree hole when a
 * {@link ToolAction#DRILL} clicks a horizontal face. Axes strip to the
 * species beam.
 */
public final class GtTreeLogBlock extends RotatedPillarBlock
        implements ToolInteractable {
    private final GtTreeSpecies species;

    public GtTreeLogBlock(GtTreeSpecies species, Properties properties) {
        super(properties);
        this.species = species;
    }

    public GtTreeSpecies species() {
        return species;
    }

    @Override
    public BlockState getToolModifiedState(
            BlockState state,
            UseOnContext context,
            ItemAbility itemAbility,
            boolean simulate) {
        if (itemAbility == ItemAbilities.AXE_STRIP
                && context.getItemInHand().canPerformAction(itemAbility)) {
            return WoodDebark.beamBlock(species)
                    .defaultBlockState()
                    .setValue(AXIS, state.getValue(AXIS));
        }
        return super.getToolModifiedState(state, context, itemAbility, simulate);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        return ToolClick.useItemOn(stack, level, player, hand, hit);
    }

    @Override
    public ToolResult useTool(ToolAction action, UseOnContext context) {
        if (action != ToolAction.DRILL) {
            return ToolResult.PASS;
        }
        Direction face = context.getClickedFace();
        if (!face.getAxis().isHorizontal() || !canDrillHole()) {
            return ToolResult.PASS;
        }
        Level level = context.getLevel();
        if (!level.isClientSide) {
            BlockState hole = ModBlocks.treeHole(species)
                    .get()
                    .defaultBlockState()
                    .setValue(GtTreeHoleBlock.FACING, face)
                    .setValue(GtTreeHoleBlock.HAS_PRODUCT, Boolean.FALSE);
            level.setBlock(context.getClickedPos(), hole, Block.UPDATE_ALL);
            ToolClick.hurt(context);
        }
        return ToolResult.SUCCESS;
    }

    private boolean canDrillHole() {
        TreeHoleMode mode = species.holeMode();
        return mode == TreeHoleMode.DRILL_MAPLE || mode == TreeHoleMode.DRILL_RAINBOWOOD;
    }
}
