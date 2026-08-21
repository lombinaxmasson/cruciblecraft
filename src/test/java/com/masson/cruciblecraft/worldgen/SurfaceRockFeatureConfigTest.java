package com.masson.cruciblecraft.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.mojang.serialization.JsonOps;

import com.google.gson.JsonObject;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/** Codec round-trip for the surface rock scatter configuration. */
class SurfaceRockFeatureConfigTest {
    @Test
    void codecRoundTripsRarityAndRockTag() {
        SurfaceRockConfiguration config = new SurfaceRockConfiguration(
                128,
                TagKey.create(
                        Registries.BLOCK,
                        ResourceLocation.fromNamespaceAndPath("c", "rocks")));
        var encoded = SurfaceRockConfiguration.CODEC
                .encodeStart(JsonOps.INSTANCE, config)
                .getOrThrow();
        SurfaceRockConfiguration decoded = SurfaceRockConfiguration.CODEC
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();
        assertEquals(config, decoded);
        assertEquals(128, decoded.rarity());
        assertEquals(
                ResourceLocation.fromNamespaceAndPath("c", "rocks"),
                decoded.rockTag().location());
        JsonObject json = (JsonObject) encoded;
        assertEquals("c:rocks", json.get("rock_tag").getAsString());
    }
}
