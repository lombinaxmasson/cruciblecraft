package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.api.tool.ToolInteractable;
import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.content.item.GtStoneCatalog;
import com.masson.cruciblecraft.content.item.tool.DrillReinforce;

import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

/** Full GT stone identity. Each (source item, meta) is a distinct block. */
public final class GtStoneBlock extends Block implements ToolInteractable {
    private final GtStoneCatalog.Variant variant;

    public GtStoneBlock(GtStoneCatalog.Variant variant, Properties properties) {
        super(properties);
        this.variant = variant;
    }

    public GtStoneCatalog.Variant variant() {
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
