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
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.loading.LoadingModList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LargeVeinConfigurationTest {
    @BeforeAll
    static void bootstrap() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void codecAcceptsSafeSymmetricRadiusAndRejectsOnePastIt() {
        assertTrue(LargeVeinConfiguration.CODEC
                .encodeStart(JsonOps.INSTANCE, configuration(23))
                .result()
                .isPresent());
        assertTrue(LargeVeinConfiguration.CODEC
                .encodeStart(JsonOps.INSTANCE, configuration(24))
                .error()
                .isPresent());
    }

    private static LargeVeinConfiguration configuration(int horizontalRadius) {
        var state = new LargeVeinConfiguration.WeightedState(
                Blocks.IRON_ORE.defaultBlockState(), 1);
        TagKey<Block> replaceable = TagKey.create(
                Registries.BLOCK,
                ResourceLocation.fromNamespaceAndPath(
                        "cruciblecraft", "large_vein_replaceables"));
        return new LargeVeinConfiguration(
                List.of(state),
                List.of(state),
                List.of(state),
                List.of(state),
                -48,
                64,
                horizontalRadius,
                9,
                0.25F,
                replaceable,
                7,
                0.78F,
                1229737806);
    }
}
