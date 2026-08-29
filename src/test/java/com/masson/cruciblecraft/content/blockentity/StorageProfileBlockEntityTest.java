package com.masson.cruciblecraft.content.blockentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.content.storage.StorageVariant;
import com.masson.cruciblecraft.content.storage.StorageVariantCatalog;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.fml.loading.LoadingModList;

class StorageProfileBlockEntityTest {
    private static RegistryAccess registries;

    @BeforeAll
    static void bootstrap() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        registries = new RegistryAccess.ImmutableRegistryAccess(
                BuiltInRegistries.REGISTRY.stream().toList());
    }

    @Test
    void bookshelfRejectsIngotsAndPersistsBooks() {
        BookshelfBlockEntity shelf = bookshelf();
        assertTrue(shelf.inventory().insertItem(
                0, new ItemStack(Items.BOOK), false).isEmpty());
        assertEquals(
                1,
                shelf.inventory().insertItem(
                        0, new ItemStack(Items.IRON_INGOT), false).getCount());
        assertTrue(shelf.enchantPower() > 0.0F);
        CompoundTag tag = shelf.saveForTest(registries);
        BookshelfBlockEntity restored = bookshelf();
        restored.loadForTest(tag, registries);
        assertTrue(restored.inventory().getStackInSlot(0).is(Items.BOOK));
    }

    @Test
    void chargingLockerAcceptsElectricAndRejectsHeat() {
        LockerBlockEntity locker = locker(true);
        LockerBlockEntity plain = locker(false);
        assertTrue(locker.handles(EnergyType.ELECTRIC, Direction.UP));
        assertFalse(locker.handles(EnergyType.HEAT, Direction.UP));
        assertFalse(plain.handles(EnergyType.ELECTRIC, Direction.UP));
    }

    @Test
    void logisticsExposesAdapterAndStandardDoesNot() {
        MassStorageBlockEntity standard = massStorage(
                "cruciblecraft:mass_storage_6000");
        MassStorageBlockEntity logistics = massStorage(
                "cruciblecraft:mass_storage_logistics_6200");
        assertNull(standard.logisticsStorage());
        assertEquals(0, standard.getLogisticsPriorityItem());
        assertEquals(logistics, logistics.logisticsStorage());
        assertEquals(2, logistics.getLogisticsPriorityItem());
        logistics.inventory().insertAll(new ItemStack(Items.IRON_INGOT, 8), false);
        assertTrue(
                logistics.getLogisticsFilterItem().is(Items.IRON_INGOT));
        CompoundTag tag = logistics.saveForTest(registries);
        MassStorageBlockEntity restored = massStorage(
                "cruciblecraft:mass_storage_logistics_6200");
        restored.loadForTest(tag, registries);
        assertEquals(8, restored.inventory().stored());
    }

    private static BookshelfBlockEntity bookshelf() {
        StorageVariant variant = StorageVariantCatalog.require(
                ResourceLocation.parse("cruciblecraft:bookshelf_7100"));
        return new BookshelfBlockEntity(
                BlockEntityType.FURNACE,
                BlockPos.ZERO,
                Blocks.FURNACE.defaultBlockState(),
                variant);
    }

    private static LockerBlockEntity locker(boolean charging) {
        StorageVariant variant = StorageVariantCatalog.require(
                ResourceLocation.parse(
                        charging
                                ? "cruciblecraft:charging_locker_7500"
                                : "cruciblecraft:locker_7300"));
        return new LockerBlockEntity(
                BlockEntityType.FURNACE,
                BlockPos.ZERO,
                Blocks.FURNACE.defaultBlockState(),
                variant);
    }

    private static MassStorageBlockEntity massStorage(String id) {
        StorageVariant variant = StorageVariantCatalog.require(
                ResourceLocation.parse(id));
        return new MassStorageBlockEntity(
                BlockEntityType.FURNACE,
                BlockPos.ZERO,
                Blocks.FURNACE.defaultBlockState(),
                variant);
    }
}
