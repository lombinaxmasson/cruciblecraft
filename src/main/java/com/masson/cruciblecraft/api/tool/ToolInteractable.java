package com.masson.cruciblecraft.api.tool;

import net.minecraft.world.item.context.UseOnContext;

/**
 * A block that answers typed tool clicks. New machines implement this
 * instead of checking concrete item classes in {@code useItemOn}.
 */
public interface ToolInteractable {
    ToolResult useTool(ToolAction action, UseOnContext context);
}
