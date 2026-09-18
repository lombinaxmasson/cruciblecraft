package com.masson.cruciblecraft.content.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.loading.LoadingModList;

class StorageFaceDisplayTest {
    @BeforeAll
    static void bootstrap() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void countFormatCompactsThousands() {
        assertEquals("64", StorageCountFormat.format(64));
        assertEquals("9999", StorageCountFormat.format(9_999));
        assertEquals("12k", StorageCountFormat.format(12_345));
        assertEquals("1M", StorageCountFormat.format(1_000_000));
    }

    @Test
    void bookDisplayMapsVanillaEnchantedWrittenAndGt() {
        assertEquals(StorageBookDisplay.EMPTY, StorageBookDisplay.index(ItemStack.EMPTY));
        assertEquals(
                StorageBookDisplay.VANILLA,
                StorageBookDisplay.index(new ItemStack(Items.BOOK)));
        assertEquals(
                StorageBookDisplay.ENCHANTED,
                StorageBookDisplay.index(new ItemStack(Items.ENCHANTED_BOOK)));
        assertEquals(
                StorageBookDisplay.WRITTEN,
                StorageBookDisplay.index(new ItemStack(Items.WRITTEN_BOOK)));
        assertEquals(
                StorageBookDisplay.GT,
                StorageBookDisplay.index(new ItemStack(Items.KNOWLEDGE_BOOK)));
        assertEquals("book_vanilla", StorageBookDisplay.textureStem(StorageBookDisplay.VANILLA));
        assertEquals("book_enchanted", StorageBookDisplay.textureStem(StorageBookDisplay.ENCHANTED));
        assertEquals("book_colored", StorageBookDisplay.textureStem(StorageBookDisplay.WRITTEN));
        assertEquals("book_gt", StorageBookDisplay.textureStem(StorageBookDisplay.GT));
        float[] front = StorageBookDisplay.box(0);
        assertEquals(1.0F / 16.0F, front[0], 1.0e-6F);
        assertEquals(9.0F / 16.0F, front[1], 1.0e-6F);
        assertEquals(2.0F / 16.0F, front[2], 1.0e-6F);
        float[] back = StorageBookDisplay.box(14);
        assertEquals(13.0F / 16.0F, back[0], 1.0e-6F);
        assertEquals(9.0F / 16.0F, back[2], 1.0e-6F);
    }

    @Test
    void bottleDisplayTintsFluidAndLeavesGlassEmpty() {
        ItemStack glass = new ItemStack(Items.GLASS_BOTTLE);
        ItemStack honey = new ItemStack(Items.HONEY_BOTTLE);
        assertTrue(StorageBottleDisplay.present(glass));
        assertFalse(StorageBottleDisplay.hasFluid(glass));
        assertTrue(StorageBottleDisplay.hasFluid(honey));
        assertEquals(StorageBottleDisplay.HONEY, StorageBottleDisplay.fluidColor(honey));
        assertEquals(StorageBottleDisplay.DRAGON_BREATH,
                StorageBottleDisplay.fluidColor(new ItemStack(Items.DRAGON_BREATH)));
        assertEquals(StorageBottleDisplay.EXPERIENCE,
                StorageBottleDisplay.fluidColor(new ItemStack(Items.EXPERIENCE_BOTTLE)));
        float[] fluid = StorageBottleDisplay.fluidBox(0);
        assertTrue(fluid[0] > 1.0F / 16.0F);
        assertTrue(fluid[3] < 5.0F / 16.0F);
    }
}
