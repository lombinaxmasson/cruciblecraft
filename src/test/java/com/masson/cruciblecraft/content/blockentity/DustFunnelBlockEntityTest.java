package com.masson.cruciblecraft.content.blockentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.logistics.hopper.DustAmountLedger.Form;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.fml.loading.LoadingModList;

class DustFunnelBlockEntityTest {
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
    void absorbRejectsMixedMaterialAndModeDoesNotChangeUnits() {
        DustFunnelBlockEntity funnel = funnel();
        assertTrue(funnel.absorb("cruciblecraft:iron", Form.TINY_DUST, 9));
        assertEquals(36, funnel.ledger().units());
        assertFalse(funnel.absorb("cruciblecraft:copper", Form.DUST, 1));
        assertEquals(36, funnel.ledger().units());
        Form next = funnel.ledger().cycleOutputMode(false);
        assertEquals(Form.SMALL_DUST, next);
        assertEquals(36, funnel.ledger().units());
        funnel.cycleMode(true);
        assertEquals(Form.DUST, funnel.ledger().outputMode());
        assertEquals(36, funnel.ledger().units());
        funnel.dropLedger();
        assertEquals(0, funnel.ledger().units());
        assertTrue(funnel.ledger().isEmpty());
    }

    @Test
    void blockedOutputLeavesUnitsUnchangedAndSaveLoadRoundTrips() {
        DustFunnelBlockEntity funnel = funnel();
        assertTrue(funnel.absorb("cruciblecraft:iron", Form.DUST, 1));
        funnel.ledger().setOutputMode(Form.TINY_DUST);
        assertEquals(9, funnel.ledger().outputItemCount());
        assertFalse(funnel.ledger().tryEmit(8));
        assertEquals(36, funnel.ledger().units());
        CompoundTag tag = funnel.saveForTest(registries);
        DustFunnelBlockEntity restored = funnel();
        restored.loadForTest(tag, registries);
        assertEquals("cruciblecraft:iron", restored.ledger().materialId());
        assertEquals(36, restored.ledger().units());
        assertEquals(Form.TINY_DUST, restored.ledger().outputMode());
        assertFalse(restored.failClosed());
    }

    @Test
    void unknownSchemaFailClosedKeepsInventoryAndLeftoverUnits() {
        DustFunnelBlockEntity funnel = funnel();
        assertTrue(funnel.absorb("cruciblecraft:iron", Form.TINY_DUST, 3));
        CompoundTag tag = funnel.saveForTest(registries);
        tag.putInt("cc_dust_funnel_schema", 9);
        DustFunnelBlockEntity restored = funnel();
        restored.loadForTest(tag, registries);
        assertTrue(restored.failClosed());
        assertEquals(12, restored.ledger().units());
        assertEquals(3, restored.ledger().decompose().tinyDust());
        assertEquals(0, restored.ledger().decompose().leftoverUnits());
    }

    private static DustFunnelBlockEntity funnel() {
        return new DustFunnelBlockEntity(
                BlockEntityType.FURNACE,
                BlockPos.ZERO,
                Blocks.FURNACE.defaultBlockState());
    }
}
