package com.masson.cruciblecraft.content.blockentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.logistics.hopper.HopperKind;
import com.masson.cruciblecraft.logistics.hopper.HopperSidedHandler;
import com.masson.cruciblecraft.logistics.hopper.HopperTransferCore;
import com.masson.cruciblecraft.logistics.hopper.HopperVariantCatalog;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
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

class HopperBlockEntityTest {
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
    void leadOneSlotPersistsModeExactAndInventory() {
        HopperBlockEntity hopper = hopper("lead_hopper");
        assertEquals(1, hopper.inventory().getSlots());
        hopper.inventory().setStackInSlot(0, iron(7));
        hopper.cycleMode(false);
        hopper.toggleExactMode();
        CompoundTag tag = hopper.saveForTest(registries);
        HopperBlockEntity restored = hopper("lead_hopper");
        restored.loadForTest(tag, registries);
        assertEquals(7, restored.inventory().getStackInSlot(0).getCount());
        assertEquals(1, restored.mode());
        assertTrue(restored.exactMode());
        assertFalse(restored.failClosed());
    }

    @Test
    void queueSlotLimitAndUnknownSchemaFailClosedIntoOverflow() {
        HopperBlockEntity queue = hopper("bismuth_queue_hopper");
        assertEquals(2, queue.inventory().getSlots());
        queue.cycleMode(true);
        assertEquals(63, queue.mode());
        assertEquals(63, queue.inventory().getSlotLimit(0));
        queue.inventory().setStackInSlot(0, iron(8));
        queue.inventory().setStackInSlot(1, iron(4));
        CompoundTag tag = queue.saveForTest(registries);
        tag.putInt("cc_hopper_schema", 99);
        HopperBlockEntity restored = hopper("bismuth_queue_hopper");
        restored.loadForTest(tag, registries);
        assertTrue(restored.failClosed());
        assertTrue(restored.inventory().getStackInSlot(0).isEmpty());
        assertTrue(restored.inventory().getStackInSlot(1).isEmpty());
        assertEquals(
                12,
                restored.overflow().stream().mapToInt(ItemStack::getCount).sum());
    }

    @Test
    void sidedViewsHideFacingInsertAndQueueShowsOnlyEnds() {
        HopperBlockEntity hopper = hopper("steel_hopper");
        hopper.inventory().setStackInSlot(0, iron(16));
        HopperSidedHandler facing = new HopperSidedHandler(
                hopper.inventory(), HopperKind.HOPPER, true, false);
        assertEquals(iron(16).getCount(), facing.getStackInSlot(0).getCount());
        assertEquals(8, facing.insertItem(0, iron(8), true).getCount());
        HopperSidedHandler output = new HopperSidedHandler(
                hopper.inventory(), HopperKind.HOPPER, true, true);
        assertEquals(8, output.extractItem(0, 8, true).getCount());

        HopperBlockEntity queue = hopper("infinity_queue_hopper");
        assertEquals(36, queue.inventory().getSlots());
        queue.inventory().setStackInSlot(0, iron(3));
        queue.inventory().setStackInSlot(35, iron(5));
        HopperSidedHandler ends = new HopperSidedHandler(
                queue.inventory(), HopperKind.QUEUE_HOPPER, false, false);
        assertEquals(2, ends.getSlots());
        assertEquals(3, ends.getStackInSlot(0).getCount());
        assertEquals(5, ends.getStackInSlot(1).getCount());
        assertEquals(1, ends.insertItem(1, iron(1), true).getCount());
        assertTrue(ends.extractItem(0, 1, true).isEmpty());
        assertEquals(5, ends.extractItem(1, 5, true).getCount());
    }

    @Test
    void exactDivisibleUsesTheSharedTransferCore() {
        var source = HopperTransferCore.limitedInventory(1, 64);
        source.setStackInSlot(0, iron(10));
        var dest = HopperTransferCore.limitedInventory(1, 64);
        dest.setStackInSlot(0, iron(62));
        assertEquals(0, HopperTransferCore.push(source, dest, 8, false, false));
        assertEquals(10, source.getStackInSlot(0).getCount());
        dest.setStackInSlot(0, ItemStack.EMPTY);
        assertEquals(8, HopperTransferCore.push(source, dest, 8, true, false));
        assertEquals(2, source.getStackInSlot(0).getCount());
    }

    private static HopperBlockEntity hopper(String path) {
        var variant = HopperVariantCatalog.require(
                ResourceLocation.fromNamespaceAndPath("cruciblecraft", path));
        return new HopperBlockEntity(
                BlockEntityType.FURNACE,
                BlockPos.ZERO,
                Blocks.FURNACE.defaultBlockState(),
                variant);
    }

    private static ItemStack iron(int count) {
        return new ItemStack(Items.IRON_INGOT, count);
    }
}
