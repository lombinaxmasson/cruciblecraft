package com.masson.cruciblecraft.content.storage;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class StorageFilters {
    private StorageFilters() {}

    public static boolean book(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.is(Items.BOOK)
                        || stack.is(Items.WRITABLE_BOOK)
                        || stack.is(Items.WRITTEN_BOOK)
                        || stack.is(Items.ENCHANTED_BOOK)
                        || stack.is(Items.KNOWLEDGE_BOOK));
    }

    public static boolean enchantedBook(ItemStack stack) {
        return stack.is(Items.ENCHANTED_BOOK);
    }

    public static boolean bottle(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.is(Items.GLASS_BOTTLE)
                        || stack.is(Items.POTION)
                        || stack.is(Items.SPLASH_POTION)
                        || stack.is(Items.LINGERING_POTION)
                        || stack.is(Items.EXPERIENCE_BOTTLE)
                        || stack.is(Items.HONEY_BOTTLE)
                        || stack.is(Items.DRAGON_BREATH));
    }
}
