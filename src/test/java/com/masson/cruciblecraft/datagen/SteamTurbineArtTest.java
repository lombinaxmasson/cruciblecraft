package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class SteamTurbineArtTest {
    private static final Path ROOT = Path.of("src/main/resources");

    @Test
    void manifestCopiesRotationSteamAndLargeFan() throws Exception {
        var manifest = JsonParser.parseString(Files.readString(
                ROOT.resolve(
                        "assets/cruciblecraft/gt6_steam_turbine_art_manifest.json")))
                .getAsJsonObject();
        assertTrue(manifest.get("source_present").getAsBoolean());
        var imports = manifest.getAsJsonArray("imports");
        assertTrue(imports.size() >= 37);
        String blob = imports.toString();
        assertTrue(blob.contains("turbines/rotation_steam/overlay_active_rs/front.png"));
        assertTrue(blob.contains("multiblockmains/largeturbine/colored_front/side.png"));
        assertTrue(blob.contains("multiblockmains/turbine_active.png"));
        assertTrue(Files.isRegularFile(ROOT.resolve(
                "assets/cruciblecraft/textures/block/machine/steam_turbine/overlay_active_rs/front.png")));
        assertTrue(Files.isRegularFile(ROOT.resolve(
                "assets/cruciblecraft/textures/block/machine/large_steam_turbine/fan_active.png")));
    }

    @Test
    void bronzeBlockstateSelectsActiveOverlay() throws Exception {
        JsonObject variants = JsonParser.parseString(Files.readString(
                ROOT.resolve(
                        "assets/cruciblecraft/blockstates/steam/turbine_bronze.json")))
                .getAsJsonObject()
                .getAsJsonObject("variants");
        assertEquals(
                "cruciblecraft:block/mte_inplace_steam_turbine",
                variants.getAsJsonObject(
                                "facing=north,lit=false,fast=false,counterclockwise=false")
                        .get("model")
                        .getAsString());
        assertEquals(
                "cruciblecraft:block/mte_inplace_steam_turbine_active_lf",
                variants.getAsJsonObject(
                                "facing=north,lit=true,fast=true,counterclockwise=true")
                        .get("model")
                        .getAsString());
    }
}
