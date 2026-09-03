package com.masson.cruciblecraft.content.item;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

final class CatalogDisplayNames {
    private CatalogDisplayNames() {}

    static Component itemName(String key, String englishName, String chineseName) {
        Language language = Language.getInstance();
        if (language.has(key)) {
            return Component.translatable(key);
        }
        return Component.literal(hanLanguage(language) ? chineseName : englishName);
    }

    private static boolean hanLanguage(Language language) {
        String sample = language.getOrDefault("language.code");
        if (sample != null && sample.startsWith("zh")) {
            return true;
        }
        String group = language.getOrDefault("itemGroup.cruciblecraft");
        return group != null && group.codePoints().anyMatch(
                codePoint -> Character.UnicodeScript.of(codePoint)
                        == Character.UnicodeScript.HAN);
    }
}
