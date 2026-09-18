package com.masson.cruciblecraft.content.storage;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

/**
 * GT6 bottle-crate display: empty glass has no fluid core; potions tint the
 * inner voxel. Boxes are north-local pixels.
 */
public final class StorageBottleDisplay {
    public static final int SLOTS = 9;
    public static final int WATER = 0x3F76E4;
    public static final int HONEY = 0xFBBD23;
    public static final int DRAGON_BREATH = 0xA44DCB;
    public static final int EXPERIENCE = 0x88FF88;
    private static final float OFFSET = 0.005F;

    private StorageBottleDisplay() {}

    public static boolean present(ItemStack stack) {
        return StorageFilters.bottle(stack);
    }

    public static boolean hasFluid(ItemStack stack) {
        return present(stack) && !stack.is(Items.GLASS_BOTTLE);
    }

    public static int fluidColor(ItemStack stack) {
        if (!hasFluid(stack)) {
            return 0;
        }
        if (stack.is(Items.HONEY_BOTTLE)) {
            return HONEY;
        }
        if (stack.is(Items.DRAGON_BREATH)) {
            return DRAGON_BREATH;
        }
        if (stack.is(Items.EXPERIENCE_BOTTLE)) {
            return EXPERIENCE;
        }
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents != null) {
            return contents.getColor();
        }
        return WATER;
    }

    public static float[] fluidBox(int slot) {
        return bottleBox(slot, OFFSET * 2.0F, px(1), px(12));
    }

    public static float[] glassBox(int slot) {
        return bottleBox(slot, OFFSET, px(1), px(13));
    }

    public static float[] capBox(int slot) {
        int column = slot % 3;
        int row = slot / 3;
        float x0 = px(2 + column * 5);
        float z0 = px(2 + row * 5);
        return new float[] {x0, px(13), z0, x0 + px(2), px(16), z0 + px(2)};
    }

    private static float[] bottleBox(int slot, float inset, float y0, float y1) {
        int column = slot % 3;
        int row = slot / 3;
        float x0 = px(1 + column * 5) + inset;
        float z0 = px(1 + row * 5) + inset;
        float x1 = px(5 + column * 5) - inset;
        float z1 = px(5 + row * 5) - inset;
        return new float[] {x0, y0, z0, x1, y1, z1};
    }

    private static float px(int pixels) {
        return pixels / 16.0F;
    }
}
