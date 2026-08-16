package com.masson.cruciblecraft.content.multiblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import com.google.gson.JsonParser;
import com.masson.cruciblecraft.registry.ModMultiblockPlugins;

import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MultiblockControllerPluginRegistryTest {
    @BeforeAll
    static void registerWhitelistedPlugins() {
        ModMultiblockPlugins.register();
    }

    @Test
    void whitelistIdSetIsFrozen() {
        assertEquals(
                Set.of(
                        ModMultiblockPlugins.PROCESSING_HOST,
                        ModMultiblockPlugins.SHARED_PORT_SUPPLY,
                        ModMultiblockPlugins.HEAT_ENERGY_INPUT,
                        ModMultiblockPlugins.STEAM_CONVERSION,
                        ModMultiblockPlugins.STORAGE_HOST),
                MultiblockControllerPluginRegistry.whitelist().keySet());
    }

    @Test
    void duplicateRegistrationIsRejected() {
        assertThrows(
                IllegalStateException.class,
                () -> MultiblockControllerPluginRegistry.register(
                        () -> ModMultiblockPlugins.PROCESSING_HOST));
    }

    @Test
    void unknownPluginIdFailsClosed() {
        ResourceLocation unknown = ResourceLocation.parse(
                "cruciblecraft:not_a_plugin");
        assertFalse(
                MultiblockControllerPluginRegistry.known(unknown));
        assertThrows(
                IllegalStateException.class,
                () -> MultiblockControllerPluginRegistry.require(unknown));
    }

    @Test
    void diskMirrorIsBidirectionallyEqualToTheRegistry() {
        var stream = MultiblockControllerPluginRegistryTest.class
                .getResourceAsStream(
                        "/data/cruciblecraft/multiblock_plugins.json");
        assertTrue(stream != null, "missing multiblock_plugins.json");
        try (var reader = new InputStreamReader(
                stream, StandardCharsets.UTF_8)) {
            var root = JsonParser.parseReader(reader).getAsJsonObject();
            Set<String> diskIds = new java.util.HashSet<>();
            for (var element : root.getAsJsonArray("plugins")) {
                var plugin = element.getAsJsonObject();
                diskIds.add(plugin.get("id").getAsString());
                assertTrue(
                        plugin.getAsJsonArray("consumers").size() > 0,
                        "plugin without a consumer: "
                                + plugin.get("id"));
            }
            Set<String> registryIds = new java.util.HashSet<>();
            for (ResourceLocation id
                    : MultiblockControllerPluginRegistry.whitelist()
                            .keySet()) {
                registryIds.add(id.toString());
            }
            assertEquals(
                    registryIds, diskIds,
                    "registry and disk mirror disagree on plugin ids");
        } catch (java.io.IOException exception) {
            throw new AssertionError(exception);
        }
    }
}
