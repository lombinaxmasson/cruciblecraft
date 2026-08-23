package com.masson.cruciblecraft.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;

import com.mojang.serialization.JsonOps;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

/** Codec round-trip for the surface rock scatter configuration. */
class SurfaceRockFeatureConfigTest {
    private static final Path SURFACE_SCATTER_DECLARATION = Path.of(
            "src/main/resources/data/cruciblecraft/worldgen_catalog"
                    + "/surface_scatter.json");
    private static final Path CONFIGURED_RUNTIME = Path.of(
            "src/main/resources/data/cruciblecraft/worldgen"
                    + "/configured_feature/surface_rock_scatter.json");

    @Test
    void codecRoundTripsRarityAndRockTag() throws Exception {
        JsonObject declaration = JsonParser.parseString(
                Files.readString(SURFACE_SCATTER_DECLARATION))
                .getAsJsonObject()
                .getAsJsonObject("config");
        int rarity = declaration.get("rarity").getAsInt();
        ResourceLocation rockTag = ResourceLocation.parse(
                declaration.get("rock_tag").getAsString());
        SurfaceRockConfiguration config = new SurfaceRockConfiguration(
                rarity,
                TagKey.create(Registries.BLOCK, rockTag));
        var encoded = SurfaceRockConfiguration.CODEC
                .encodeStart(JsonOps.INSTANCE, config)
                .getOrThrow();
        SurfaceRockConfiguration decoded = SurfaceRockConfiguration.CODEC
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();
        assertEquals(config, decoded);
        assertEquals(rarity, decoded.rarity());
        assertEquals(rockTag, decoded.rockTag().location());
        JsonObject json = (JsonObject) encoded;
        assertEquals(rockTag.toString(), json.get("rock_tag").getAsString());

        JsonObject runtime = JsonParser.parseString(
                Files.readString(CONFIGURED_RUNTIME))
                .getAsJsonObject()
                .getAsJsonObject("config");
        assertEquals(
                runtime.get("rarity").getAsInt(),
                decoded.rarity());
        assertEquals(
                runtime.get("rock_tag").getAsString(),
                decoded.rockTag().location().toString());
    }

    @Test
    void declarationConfigMustMatchConfiguredRuntimeResource() throws Exception {
        JsonObject declaration = JsonParser.parseString(
                Files.readString(SURFACE_SCATTER_DECLARATION))
                .getAsJsonObject();
        JsonObject runtime = JsonParser.parseString(
                Files.readString(CONFIGURED_RUNTIME))
                .getAsJsonObject();
        assertEquals(
                declaration.get("feature_type").getAsString(),
                runtime.get("type").getAsString());
        assertEquals(
                declaration.getAsJsonObject("config").toString(),
                runtime.getAsJsonObject("config").toString());
    }
}
