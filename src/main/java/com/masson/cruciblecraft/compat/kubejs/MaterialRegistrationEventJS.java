package com.masson.cruciblecraft.compat.kubejs;

import java.util.function.Consumer;

import com.masson.cruciblecraft.material.MaterialCatalog;

import dev.latvian.mods.kubejs.event.KubeEvent;

public final class MaterialRegistrationEventJS implements KubeEvent {
    /**
     * Tunes an existing bundled or addon-provided material for this startup.
     * Registry ids, forms, item mappings, fluids, and composition cannot change.
     *
     * @return true when this id did not already have a queued KubeJS tuning
     */
    public boolean add(String id, Consumer<StartupMaterialBuilder> configurator) {
        StartupMaterialBuilder builder = new StartupMaterialBuilder(id);
        configurator.accept(builder);
        return MaterialCatalog.addStartupTuning(builder.build());
    }
}
