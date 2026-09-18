package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.localization.LanguageNames;

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
        return formName(materialId(), form());
    }

    static Component formName(String materialId, MaterialPrefix form) {
        Component materialName = materialDisplayName(materialId);
        String formKey = "item.cruciblecraft.material_form." + form.serializedName();
        String fallback = LanguageNames.englishFormTemplate(
                LanguageNames.formatEnglishId(form.serializedName()));
        return Component.translatableWithFallback(formKey, fallback, materialName);
    }

    static Component materialDisplayName(String materialId) {
        return MaterialCatalog.find(materialId)
                .map(material -> Component.translatable(material.translationKey()))
                .orElseGet(() -> Component.literal(
                        materialId == null || materialId.isEmpty()
                                ? ""
                                : LanguageNames.formatEnglishId(materialId)));
    }
}
