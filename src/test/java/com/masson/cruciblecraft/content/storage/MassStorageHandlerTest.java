package com.masson.cruciblecraft.content.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.loading.LoadingModList;

class MassStorageHandlerTest {
    @BeforeAll
    static void bootstrap() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void locksTypeRejectsOverflowAndKeepsFilterWhenEmpty() {
        MassStorageHandler handler = new MassStorageHandler(10);
        assertTrue(handler.insertAll(iron(8), false).isEmpty());
        assertEquals(8, handler.stored());
        assertEquals(4, handler.insertAll(gold(4), false).getCount());
        ItemStack leftover = handler.insertAll(iron(4), false);
        assertEquals(2, leftover.getCount());
        assertEquals(10, handler.stored());
        assertEquals(10, handler.extractItem(0, 64, false).getCount());
        assertEquals(0, handler.stored());
        assertTrue(handler.filter().is(Items.IRON_INGOT));
        assertEquals(3, handler.insertAll(gold(3), false).getCount());
        handler.setKeepFilterWhenEmpty(false);
        assertTrue(handler.filter().isEmpty());
        assertTrue(handler.insertAll(gold(3), false).isEmpty());
        assertTrue(handler.sameType(gold(1)));
        assertFalse(handler.sameType(iron(1)));
    }

    @Test
    void booksAndBottlesMatchSourceFilters() {
        assertTrue(StorageFilters.book(new ItemStack(Items.BOOK)));
        assertTrue(StorageFilters.book(new ItemStack(Items.ENCHANTED_BOOK)));
        assertFalse(StorageFilters.book(iron(1)));
        assertTrue(StorageFilters.bottle(new ItemStack(Items.GLASS_BOTTLE)));
        assertTrue(StorageFilters.bottle(new ItemStack(Items.POTION)));
        assertFalse(StorageFilters.bottle(iron(1)));
    }

    @Test
    void clickMapMatchesGt6FaceButtons() {
        assertEquals(1, MassStorageClicks.amount(new float[] {0.125F, 0.8125F}));
        assertEquals(4, MassStorageClicks.amount(new float[] {0.125F, 0.625F}));
        assertEquals(8, MassStorageClicks.amount(new float[] {0.125F, 0.4375F}));
        assertEquals(16, MassStorageClicks.amount(new float[] {0.875F, 0.8125F}));
        assertEquals(32, MassStorageClicks.amount(new float[] {0.875F, 0.625F}));
        assertEquals(64, MassStorageClicks.amount(new float[] {0.875F, 0.4375F}));
        assertEquals(-1, MassStorageClicks.amount(new float[] {0.5F, 0.5F}));
        assertTrue(MassStoragePrefixUnits.familyOf(
                com.masson.cruciblecraft.api.material.MaterialPrefixes.INGOT)
                .contains(com.masson.cruciblecraft.api.material.MaterialPrefixes.NUGGET));
        assertTrue(MassStoragePrefixUnits.familyOf(
                com.masson.cruciblecraft.api.material.MaterialPrefixes.INGOT)
                .contains(com.masson.cruciblecraft.api.material.MaterialPrefixes.BLOCK));
    }

    @Test
    void dustPartialsPreferGt6LargestDenomination() {
        assertEquals(
                MaterialPrefixes.STORAGE_DUST,
                MassStoragePrefixUnits.dustDenomination(72L * MassStoragePrefixUnits.U));
        assertEquals(
                MaterialPrefixes.DUST,
                MassStoragePrefixUnits.dustDenomination(MassStoragePrefixUnits.U));
        assertEquals(
                MaterialPrefixes.DUST,
                MassStoragePrefixUnits.dustDenomination(16L * MassStoragePrefixUnits.U));
        assertEquals(
                MaterialPrefixes.SMALL_DUST,
                MassStoragePrefixUnits.dustDenomination(MassStoragePrefixUnits.U4));
        assertEquals(
                MaterialPrefixes.TINY_DUST,
                MassStoragePrefixUnits.dustDenomination(MassStoragePrefixUnits.U9));
        assertEquals(
                MaterialPrefixes.DUST_DIV72,
                MassStoragePrefixUnits.dustDenomination(MassStoragePrefixUnits.U72));
        assertEquals(
                MaterialPrefixes.SMALL_DUST,
                MassStoragePrefixUnits.dustDenomination(MassStoragePrefixUnits.U + MassStoragePrefixUnits.U4));
        assertNull(MassStoragePrefixUnits.dustDenomination(1L));
        assertEquals(
                MaterialPrefixes.INGOT,
                MassStoragePrefixUnits.ingotDenomination(MassStoragePrefixUnits.U));
        assertEquals(
                MaterialPrefixes.NUGGET,
                MassStoragePrefixUnits.ingotDenomination(MassStoragePrefixUnits.U9));
        assertEquals("12k", StorageCountFormat.format(12_345));
        assertEquals("32", StorageCountFormat.face(32, 64, "mass_storage_barrel"));
        assertEquals("100%", StorageCountFormat.face(64, 64, "mass_storage_barrel"));
        assertEquals("12k", StorageCountFormat.face(12_345, 1_000_000, "mass_storage_standard"));
    }

    @Test
    void overflowBonusExtendsCapacityAndRejectsWhenFull() {
        MassStorageHandler handler = new MassStorageHandler(10);
        handler.setOverflowBonus(MassStorageHandler.OVERFLOW_BONUS);
        assertEquals(266, handler.maxContent());
        assertTrue(handler.insertAll(iron(10), false).isEmpty());
        assertEquals(44, handler.insertAll(iron(300), false).getCount());
        assertEquals(266, handler.stored());
        assertFalse(handler.isItemValid(0, iron(1)));
    }

    private static ItemStack iron(int count) {
        return new ItemStack(Items.IRON_INGOT, count);
    }

    private static ItemStack gold(int count) {
        return new ItemStack(Items.GOLD_INGOT, count);
    }
}
