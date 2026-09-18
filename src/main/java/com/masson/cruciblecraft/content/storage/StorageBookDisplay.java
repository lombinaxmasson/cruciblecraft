package com.masson.cruciblecraft.content.storage;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * GT6 {@code BooksGT} display ids used by this port: vanilla / enchanted /
 * white written / GT knowledge. Boxes are north-local pixels; blockstate Y
 * rotation orients them.
 */
public final class StorageBookDisplay {
    public static final int EMPTY = 0;
    public static final int VANILLA = 1;
    public static final int ENCHANTED = 2;
    public static final int WRITTEN = 4;
    public static final int GT = 12;
    public static final int SLOTS = 28;

    private StorageBookDisplay() {}

    public static int index(ItemStack stack) {
        if (!StorageFilters.book(stack)) {
            return EMPTY;
        }
        if (stack.is(Items.ENCHANTED_BOOK)) {
            return ENCHANTED;
        }
        if (stack.is(Items.WRITTEN_BOOK)) {
            return WRITTEN;
        }
        if (stack.is(Items.KNOWLEDGE_BOOK)) {
            return GT;
        }
        return VANILLA;
    }

    public static String textureStem(int index) {
        return switch (index) {
            case ENCHANTED -> "book_enchanted";
            case WRITTEN -> "book_colored";
            case GT -> "book_gt";
            default -> "book_vanilla";
        };
    }

    /**
     * North-local pixel box {@code {x0,y0,z0,x1,y1,z1}} for GT6
     * {@code SIDE_Z_NEG} book voxels.
     */
    public static float[] box(int slot) {
        int column = slot % 7;
        boolean lower = (slot % 14) >= 7;
        boolean back = slot >= 14;
        float x0;
        float x1;
        float z0;
        float z1;
        if (back) {
            x0 = px(13 - column * 2);
            x1 = px(15 - column * 2);
            z0 = px(9);
            z1 = px(14);
        } else {
            x0 = px(1 + column * 2);
            x1 = px(3 + column * 2);
            z0 = px(2);
            z1 = px(7);
        }
        float y0 = lower ? px(1) : px(9);
        float y1 = lower ? px(7) : px(15);
        return new float[] {x0, y0, z0, x1, y1, z1};
    }

    private static float px(int pixels) {
        return pixels / 16.0F;
    }
}
