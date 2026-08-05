package com.masson.cruciblecraft.logistics.pipe.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

class ItemPipeTransferPlanTest {
    @BeforeAll
    static void bootstrap() {
        LoadingModList.of(
                List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void equalEphemeralWrappersAreRevalidatedByInventoryBehavior() {
        SimpleContainer inventory = new SimpleContainer(1);
        IItemHandler simulated = new InvWrapper(inventory);
        IItemHandler executed = new InvWrapper(inventory);

        assertNotSame(simulated, executed);
        assertEquals(simulated, executed);
        assertEquals(
                3,
                ItemPipeTransferPlan.revalidateInsert(
                        executed,
                        new ItemStack(Items.IRON_INGOT, 3),
                        3));
    }

    @Test
    void unequalReplacementHandlerIsAcceptedOnlyWhenItStillFits() {
        IItemHandler simulated =
                new InvWrapper(new SimpleContainer(1));
        SimpleContainer replacementInventory = new SimpleContainer(1);
        IItemHandler replacement = new InvWrapper(replacementInventory);

        assertNotEquals(simulated, replacement);
        assertEquals(
                3,
                ItemPipeTransferPlan.revalidateInsert(
                        replacement,
                        new ItemStack(Items.IRON_INGOT, 3),
                        3));

        replacementInventory.setItem(
                0, new ItemStack(Items.IRON_INGOT, 64));
        assertEquals(
                0,
                ItemPipeTransferPlan.revalidateInsert(
                        replacement,
                        new ItemStack(Items.IRON_INGOT, 3),
                        3));
    }
}
