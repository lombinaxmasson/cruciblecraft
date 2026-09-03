package com.masson.cruciblecraft.content.item;

import java.util.Objects;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Identity item whose catalog names survive a stale datagen language file. */
public final class CatalogNamedItem extends Item {
    private final String englishName;
    private final String chineseName;

    public CatalogNamedItem(
            Properties properties, String englishName, String chineseName) {
        super(properties);
        this.englishName = Objects.requireNonNull(englishName, "englishName");
        this.chineseName = Objects.requireNonNull(chineseName, "chineseName");
    }

    @Override
    public Component getName(ItemStack stack) {
        return CatalogDisplayNames.itemName(
                getDescriptionId(stack), englishName, chineseName);
    }
}
