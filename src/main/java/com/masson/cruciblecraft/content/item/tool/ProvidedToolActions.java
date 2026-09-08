package com.masson.cruciblecraft.content.item.tool;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

/** Maps recipe/material {@link ToolKind}s onto world-click {@link ToolAction}s. */
public final class ProvidedToolActions {
    private static final Map<ToolKind, Set<ToolAction>> BY_KIND;

    static {
        EnumMap<ToolKind, Set<ToolAction>> map = new EnumMap<>(ToolKind.class);
        map.put(ToolKind.WRENCH, Set.of(ToolAction.WRENCH));
        map.put(ToolKind.MONKEY_WRENCH, Set.of(ToolAction.MONKEY_WRENCH));
        map.put(ToolKind.WIRE_CUTTER, Set.of(ToolAction.WIRE_CUTTER));
        map.put(ToolKind.SCREWDRIVER, Set.of(ToolAction.SCREWDRIVER));
        map.put(ToolKind.CROWBAR, Set.of(ToolAction.CROWBAR));
        map.put(ToolKind.PLUNGER, Set.of(ToolAction.PLUNGER));
        map.put(ToolKind.SOFT_HAMMER, Set.of(ToolAction.SOFT_HAMMER));
        map.put(ToolKind.PINCERS, Set.of(ToolAction.PINCERS));
        map.put(ToolKind.UNIVERSAL_SPADE, Set.of(ToolAction.CROWBAR));
        BY_KIND = Collections.unmodifiableMap(map);
    }

    private ProvidedToolActions() {}

    public static Set<ToolAction> of(ToolKind kind) {
        return BY_KIND.getOrDefault(kind, Set.of());
    }
}
