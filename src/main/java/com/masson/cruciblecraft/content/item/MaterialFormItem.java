package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.MaterialDefinition;

import net.minecraft.network.chat.Component;

/** Shared identity for generated material items and material block items. */
public interface MaterialFormItem {
    String materialId();

    MaterialPrefix form();

    default MaterialDefinition material() {
        return MaterialCatalog.require(materialId());
    }

    default int units() {
        return form().units();
    }

    default Component materialFormName() {
        MaterialDefinition material = material();
        Component materialName = material.nameKey()
                .<Component>map(Component::translatable)
                .orElseGet(() -> Component.literal(title(material.id())));
        String formKey = "item.cruciblecraft.material_form." + form().serializedName();
        String fallback = "%s " + title(form().serializedName());
        return Component.translatableWithFallback(formKey, fallback, materialName);
    }

    private static String title(String id) {
        String spaced = id.replace('_', ' ');
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }
}
