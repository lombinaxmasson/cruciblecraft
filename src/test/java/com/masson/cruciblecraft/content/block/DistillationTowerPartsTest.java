package com.masson.cruciblecraft.content.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.content.mte.MteInPlaceSpec;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PortType;

import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.neoforged.fml.loading.LoadingModList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DistillationTowerPartsTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void catalogIdsStayGt6Meta() {
        assertEquals(
                "multiblock/heat_transmitter",
                DistillationTowerParts.HEAT_TRANSMITTER.getPath());
        assertEquals(
                "multiblock/distillation_tower_part",
                DistillationTowerParts.TOWER_PART.getPath());
    }

    @Test
    void oneTowerPartAcceptsBothItemFluidAndFluidOut() {
        assertTrue(DistillationTowerParts.accepts(
                fakeSpec(18101), PortType.ENERGY_INPUT));
        assertFalse(DistillationTowerParts.accepts(
                fakeSpec(18101), PortType.ITEM_FLUID));
        assertTrue(DistillationTowerParts.accepts(
                fakeSpec(18102), PortType.ITEM_FLUID));
        assertTrue(DistillationTowerParts.accepts(
                fakeSpec(18102), PortType.FLUID_OUT));
        assertFalse(DistillationTowerParts.accepts(
                fakeSpec(18102), PortType.ENERGY_INPUT));
    }

    private static MteInPlaceSpec fakeSpec(int meta) {
        ResourceLocation id = meta == 18101
                ? DistillationTowerParts.HEAT_TRANSMITTER
                : DistillationTowerParts.TOWER_PART;
        return new MteInPlaceSpec(
                id,
                id.getPath(),
                meta,
                MteInPlaceKind.MULTIBLOCK_PART,
                "family",
                "english",
                "chinese",
                "gt6");
    }
}
