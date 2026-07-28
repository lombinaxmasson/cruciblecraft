package com.masson.cruciblecraft.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;

public final class CrucibleCraftKubeJSPlugin implements KubeJSPlugin {
    public static final EventGroup EVENTS = EventGroup.of("CrucibleCraftMaterials");
    public static final EventHandler ADD =
            EVENTS.startup("add", () -> MaterialRegistrationEventJS.class);

    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(EVENTS);
    }
}
