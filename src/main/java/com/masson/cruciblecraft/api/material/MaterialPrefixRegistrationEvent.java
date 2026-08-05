package com.masson.cruciblecraft.api.material;

import java.util.Objects;

import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixDefinition;

import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

/**
 * Dedicated startup event fired by CrucibleCraft during its own construct
 * callback, before material definitions are decoded.
 *
 * <p>Because mod construction is serial, an addon listener is only reliable
 * when an explicit NeoForge dependency orders the addon before CrucibleCraft.
 * Unordered or later-constructed addons must not assume they will observe this
 * event; see the material API README beside this class.
 */
public final class MaterialPrefixRegistrationEvent extends Event implements IModBusEvent {
    public boolean register(MaterialPrefixDefinition definition) {
        return MaterialPrefixCatalog.addStartupPrefix(
                Objects.requireNonNull(definition, "definition"));
    }
}
