package com.masson.cruciblecraft.api.material;

import java.util.Objects;

import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.MaterialDefinition;

import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

/**
 * Convenience event fired by CrucibleCraft during its own construct callback.
 *
 * <p>Prefix definitions must be registered earlier through
 * {@link MaterialPrefixRegistrationEvent}; this event is intentionally kept
 * separate so material decoding always sees a frozen prefix table.
 *
 * <p>Addons should prefer {@link MaterialCatalog#addStartupMaterial} from an
 * explicitly ordered startup hook. Event listeners are only reliable when mod
 * ordering guarantees that the listener exists before this event is posted.
 */
public final class MaterialRegistrationEvent extends Event implements IModBusEvent {
    public boolean register(MaterialDefinition definition) {
        return MaterialCatalog.addStartupMaterial(
                Objects.requireNonNull(definition, "definition"));
    }
}
