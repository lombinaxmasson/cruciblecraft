package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.content.item.MaterialWorkshopToolItem;
import com.masson.cruciblecraft.content.item.tool.ToolClick;
import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;
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

/**
 * Growing GT log. Maple and rainbowood convert to a tree hole when a
 * {@link ToolKind#HAND_DRILL} clicks a horizontal face. Axe-to-beam stays
 * out of this card.
 */
public final class GtTreeLogBlock extends RotatedPillarBlock {
    private final GtTreeSpecies species;

    public GtTreeLogBlock(GtTreeSpecies species, Properties properties) {
        super(properties);
        this.species = species;
    }

    public GtTreeSpecies species() {
        return species;
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
        if (!(stack.getItem() instanceof MaterialWorkshopToolItem tool)
                || tool.kind() != ToolKind.HAND_DRILL) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        Direction face = hit.getDirection();
        if (!face.getAxis().isHorizontal() || !canDrillHole()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            BlockState hole = ModBlocks.treeHole(species)
                    .get()
                    .defaultBlockState()
                    .setValue(GtTreeHoleBlock.FACING, face)
                    .setValue(GtTreeHoleBlock.HAS_PRODUCT, Boolean.FALSE);
            level.setBlock(pos, hole, Block.UPDATE_ALL);
            ToolClick.hurt(new UseOnContext(level, player, hand, stack, hit));
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private boolean canDrillHole() {
        TreeHoleMode mode = species.holeMode();
        return mode == TreeHoleMode.DRILL_MAPLE || mode == TreeHoleMode.DRILL_RAINBOWOOD;
    }
}
