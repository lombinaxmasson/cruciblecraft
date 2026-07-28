package com.masson.cruciblecraft.compat.kubejs;

import java.util.function.Consumer;

import com.masson.cruciblecraft.material.MaterialCatalog;

import dev.latvian.mods.kubejs.event.KubeEvent;

public final class MaterialRegistrationEventJS implements KubeEvent {
    /**
     * Adds a material for this startup. Existing ids are never replaced.
     *
     * @return true when the id was accepted
     */
    public boolean add(String id, Consumer<StartupMaterialBuilder> configurator) {
        StartupMaterialBuilder builder = new StartupMaterialBuilder(id);
        configurator.accept(builder);
        return MaterialCatalog.addStartupMaterial(builder.build());
    }
}
