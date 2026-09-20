package com.masson.cruciblecraft.api.tool;

import net.minecraft.world.item.context.UseOnContext;

/**
 * A block that answers typed tool clicks. New machines implement this
 * instead of checking concrete item classes in {@code useItemOn}.
 */
public interface ToolInteractable {
    ToolResult useTool(ToolAction action, UseOnContext context);

    /**
     * GT6 {@code IBlockToolable.onToolClick} remaining durability / quality.
     * Auto-tool hammers pass {@code stored KU * 10}. Default ignores both.
     */
    default ToolResult useTool(
            ToolAction action,
            UseOnContext context,
            long remainingDurability,
            long quality) {
        return useTool(action, context);
    }
}
