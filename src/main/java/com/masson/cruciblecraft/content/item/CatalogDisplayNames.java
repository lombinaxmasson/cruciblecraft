package com.masson.cruciblecraft.content.item;

import java.util.Optional;

import com.masson.cruciblecraft.content.mte.MteInPlaceDisplayNames;
import com.masson.cruciblecraft.localization.LanguageNames;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

final class CatalogDisplayNames {
    private CatalogDisplayNames() {}

    static Component itemName(String key, String englishName, String chineseName) {
        return itemName(key, englishName, chineseName, Optional.empty());
    }

    static Component itemName(
            String key,
            String englishName,
            String chineseName,
            Optional<String> composedZh) {
        Language language = Language.getInstance();
        boolean han = hanLanguage(language);
        if (han && composedZh.isPresent()) {
            return Component.literal(composedZh.get());
        }
        if (han
                && LanguageNames.hasCjk(chineseName)
                && !LanguageNames.isEnglishCopy(chineseName, englishName)) {
            return Component.literal(chineseName);
        }
        if (language.has(key)) {
            return Component.translatable(key);
        }
        return Component.literal(han ? chineseName : englishName);
    }

    static boolean hanLanguage() {
        return hanLanguage(Language.getInstance());
    }

    static Optional<String> composedChinese(String registryPath) {
        return MteInPlaceDisplayNames.chinese(registryPath);
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
