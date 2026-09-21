package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.api.tool.ToolInteractable;
import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.content.item.GtBlockObjectCatalog;
import com.masson.cruciblecraft.content.item.tool.DrillReinforce;

import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

/** Solid GT block-object identity (cfoam and other non-special solids). */
public final class GtBlockObjectBlock extends Block implements ToolInteractable {
    private final GtBlockObjectCatalog.Variant variant;

    public GtBlockObjectBlock(
            GtBlockObjectCatalog.Variant variant, Properties properties) {
        super(properties);
        this.variant = variant;
    }

    public GtBlockObjectCatalog.Variant variant() {
        return variant;
    }

    @Override
    public ToolResult useTool(ToolAction action, UseOnContext context) {
        if (action != ToolAction.DRILL) {
            return ToolResult.PASS;
        }
        return DrillReinforce.use(context);
    }
}
