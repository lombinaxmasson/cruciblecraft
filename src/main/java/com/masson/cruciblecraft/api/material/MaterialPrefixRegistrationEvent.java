package com.masson.cruciblecraft.api.material;

import java.util.Objects;

import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixDefinition;

import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

/** Dedicated startup event fired before material definitions are decoded. */
public final class MaterialPrefixRegistrationEvent extends Event implements IModBusEvent {
    public boolean register(MaterialPrefixDefinition definition) {
        return MaterialPrefixCatalog.addStartupPrefix(
                Objects.requireNonNull(definition, "definition"));
    }
}
