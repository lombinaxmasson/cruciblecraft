package com.masson.cruciblecraft.api.tool;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.context.UseOnContext;

/**
 * Read-only magnifying-glass inspection. Implementations must not mutate the
 * world; empty lists mean this target has no source-backed status to report.
 */
public interface MagnifyingInspectable {
    List<Component> magnifyingInspect(UseOnContext context);
}
