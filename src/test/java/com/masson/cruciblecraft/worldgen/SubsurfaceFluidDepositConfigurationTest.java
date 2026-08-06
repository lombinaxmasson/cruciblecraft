package com.masson.cruciblecraft.worldgen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import com.mojang.serialization.JsonOps;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.loading.LoadingModList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SubsurfaceFluidDepositConfigurationTest {
    @BeforeAll
    static void bootstrap() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void codecAcceptsFiniteReserveAndRejectsReversedBounds() {
        assertTrue(SubsurfaceFluidDepositConfiguration.CODEC
                .encodeStart(JsonOps.INSTANCE, configuration(1_000L, 4_000L))
                .result()
                .isPresent());
        assertTrue(SubsurfaceFluidDepositConfiguration.CODEC
                .encodeStart(JsonOps.INSTANCE, configuration(4_000L, 1_000L))
                .error()
                .isPresent());
    }

    @Test
    void deterministicReserveAlwaysStaysInsideDeclaredRange() {
        var configuration = configuration(1_000_000L, 4_000_000L);
        for (long seed : new long[] {
                Long.MIN_VALUE, -1L, 0L, 1L, Long.MAX_VALUE
        }) {
            long reserve = SubsurfaceFluidDepositFeature.reserveAmount(
                    configuration, seed);
            assertTrue(
                    reserve >= configuration.minAmountMb()
                            && reserve <= configuration.maxAmountMb(),
                    Long.toString(seed));
        }
    }

    private static SubsurfaceFluidDepositConfiguration configuration(
            long minimum, long maximum) {
        TagKey<Block> replaceable = TagKey.create(
                Registries.BLOCK,
                ResourceLocation.fromNamespaceAndPath(
                        "cruciblecraft",
                        "subsurface_fluid_deposit_replaceables"));
        return new SubsurfaceFluidDepositConfiguration(
                ResourceLocation.fromNamespaceAndPath(
                        "cruciblecraft", "crude_oil"),
                minimum,
                maximum,
                -48,
                16,
                12,
                replaceable,
                16,
                0.5F,
                1330194521,
                25,
                20,
                1_000,
                false);
    }
}
