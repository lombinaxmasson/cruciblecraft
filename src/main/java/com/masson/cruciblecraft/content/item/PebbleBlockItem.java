package com.masson.cruciblecraft.content.item;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

/**
 * GT6 small rocks: standing right-click picks up / interacts; sneak places.
 */
public class PebbleBlockItem extends BlockItem {
    public PebbleBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        return super.useOn(context);
    }
}
