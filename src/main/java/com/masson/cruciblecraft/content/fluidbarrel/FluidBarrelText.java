package com.masson.cruciblecraft.content.fluidbarrel;

import com.masson.cruciblecraft.localization.LanguageNames;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Catalog names resolved on the client, matching other slash-id blocks. */
public final class FluidBarrelText {
    private FluidBarrelText() {}

    public static MutableComponent name(FluidBarrelProfile profile) {
        if (hanLanguage()
                && LanguageNames.hasCjk(profile.chineseName())
                && !LanguageNames.isEnglishCopy(
                        profile.chineseName(), profile.englishName())) {
            return Component.literal(profile.chineseName());
        }
        return Component.literal(
                hanLanguage() ? profile.chineseName() : profile.englishName());
    }

    private static boolean hanLanguage() {
        Language language = Language.getInstance();
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
