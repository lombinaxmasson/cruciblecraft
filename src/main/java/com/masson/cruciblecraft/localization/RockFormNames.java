package com.masson.cruciblecraft.localization;

import net.minecraft.network.chat.Component;

/**
 * GT6 {@code LanguageHandler} specials for {@code OP.rockGt} plus the item
 * tooltip from {@code OreDictListenerItem_Rocks}.
 */
public final class RockFormNames {
    private RockFormNames() {}

    public static Component specialName(String materialId) {
        if (materialId == null) {
            return null;
        }
        return switch (materialId) {
            case "stone" -> Component.translatableWithFallback(
                    "item.cruciblecraft.rock.stone", "Rock");
            case "netherrack" -> Component.translatableWithFallback(
                    "item.cruciblecraft.rock.netherrack", "Nether Rock");
            case "endstone" -> Component.translatableWithFallback(
                    "item.cruciblecraft.rock.endstone", "End Rock");
            case "meteoric_iron", "meteorite" -> Component.translatableWithFallback(
                    "item.cruciblecraft.rock.meteorite", "Meteorite");
            default -> null;
        };
    }

    public static boolean indicatesOccurrence(String materialId) {
        if (materialId == null) {
            return false;
        }
        return switch (materialId) {
            case "meteoric_iron", "meteorite", "ancient_debris", "obsidian",
                    "ambrosium", "glowstone" -> false;
            default -> true;
        };
    }
}
