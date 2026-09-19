package com.masson.cruciblecraft.compat.emi;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.loading.LoadingModList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class EmiIndexDedupeTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void laterIdenticalItemAndPatchIsACopy() {
        Set<EmiIndexDedupe.ItemIndexKey> seen = new HashSet<>();
        ItemStack first = new ItemStack(Items.DIAMOND);
        ItemStack second = new ItemStack(Items.DIAMOND);
        assertFalse(EmiIndexDedupe.isLaterCopy(seen, first));
        assertTrue(EmiIndexDedupe.isLaterCopy(seen, second));
    }

    @Test
    void differentComponentPatchesStayDistinct() {
        Set<EmiIndexDedupe.ItemIndexKey> seen = new HashSet<>();
        ItemStack empty = new ItemStack(Items.DIAMOND);
        ItemStack named = new ItemStack(Items.DIAMOND);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("named"));
        assertFalse(EmiIndexDedupe.isLaterCopy(seen, empty));
        assertFalse(EmiIndexDedupe.isLaterCopy(seen, named));
    }

    @Test
    void emptyStacksAreNotIndexCopies() {
        Set<EmiIndexDedupe.ItemIndexKey> seen = new HashSet<>();
        assertFalse(EmiIndexDedupe.isLaterCopy(seen, ItemStack.EMPTY));
        assertFalse(EmiIndexDedupe.isLaterCopy(seen, ItemStack.EMPTY));
    }
}
