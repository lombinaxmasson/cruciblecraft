package com.masson.cruciblecraft.compat.emi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.neoforged.fml.loading.LoadingModList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class EmiIdsTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void prefixesRecipeMapIdsAsSynthetic() {
        ResourceLocation logical = ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft",
                "mixer/ordinary_closure/material_matrix/sha256_ab");
        ResourceLocation synthetic = EmiIds.synthetic(logical);
        assertEquals("cruciblecraft", synthetic.getNamespace());
        assertEquals(
                "/mixer/ordinary_closure/material_matrix/sha256_ab",
                synthetic.getPath());
        assertTrue(synthetic.getPath().startsWith("/"));
        assertEquals(synthetic, EmiIds.synthetic(synthetic));
    }

    @Test
    void prefixesSharedRecipeMapsByMachineCategory() {
        ResourceLocation mixer = ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft", "mixer");
        ResourceLocation electric = ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft", "electric_mixer");
        ResourceLocation recipe = ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft", "mixer/ordinary_closure/deadbeef");
        ResourceLocation mixerEmi = EmiIds.synthetic(mixer, recipe);
        ResourceLocation electricEmi = EmiIds.synthetic(electric, recipe);
        assertEquals("/mixer/mixer/ordinary_closure/deadbeef", mixerEmi.getPath());
        assertEquals(
                "/electric_mixer/mixer/ordinary_closure/deadbeef",
                electricEmi.getPath());
        assertTrue(!mixerEmi.equals(electricEmi));
    }

    @Test
    void leavesAlloyAndMoldIdsUnchanged() {
        ResourceLocation alloy = ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft", "/alloy/bronze");
        ResourceLocation mold = ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft", "/mold_casting/copper/ingot");
        assertEquals(alloy, EmiIds.synthetic(alloy));
        assertEquals(mold, EmiIds.synthetic(mold));
    }

    @Test
    void internedCategoriesCompareById() {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft", "mixer");
        var first = new CanonicalEmiRecipeCategory(id, (draw, x, y, delta) -> {});
        var second = new CanonicalEmiRecipeCategory(id, (draw, x, y, delta) -> {});
        assertEquals(first, second);
        assertTrue(List.of(first).contains(second));
    }
}
