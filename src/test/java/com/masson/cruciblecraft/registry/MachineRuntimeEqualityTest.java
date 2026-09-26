package com.masson.cruciblecraft.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.machine.processing.MachineTierCatalog;

import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.neoforged.fml.loading.LoadingModList;

class MachineRuntimeEqualityTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void catalogIdsMatchFrozenTargetAndOpeningSubset() {
        JsonObject document = loadCatalog();
        Set<String> catalog = ids(document, false);
        Set<String> generic = ids(document, true);
        assertEquals(298, catalog.size());
        assertEquals(296, generic.size());
        assertTrue(catalog.contains("cruciblecraft:bronze_crusher"));
        assertTrue(catalog.contains("cruciblecraft:steel_roaster"));
        assertTrue(catalog.contains("cruciblecraft:coagulator"));
        assertTrue(catalog.contains("cruciblecraft:canner"));
        assertEquals(33, catalog.stream().filter(MachineRuntimeEqualityTest::isOpeningId).count());
        Set<String> variants = ModMachineVariants.ALL.stream()
                .map(variant -> variant.id().toString())
                .collect(Collectors.toCollection(TreeSet::new));
        assertEquals(generic, variants);
        assertEquals(generic.size(), MachineTierCatalog.entries().size());
        assertTrue(ModMachineVariants.isOpening(
                ResourceLocation.parse("cruciblecraft:centrifuge")));
        assertTrue(catalog.contains("cruciblecraft:chromium_electrolyzer"));
    }

    @Test
    void machineGameTestsAreIsolatedFromDailyGrid() throws Exception {
        String daily = Files.readString(Path.of(
                "src/test/java/com/masson/cruciblecraft/gametest/"
                        + "CrucibleCraftGameTests.java"));
        assertTrue(
                !daily.contains("MachineRuntimeGameTests")
                        && !daily.contains("cruciblecraft_machines"),
                "daily GameTest grid must not own the machine runtime namespace");
        String holder = Files.readString(Path.of(
                "src/test/java/com/masson/cruciblecraft/gametest/"
                        + "MachineRuntimeGameTests.java"));
        assertTrue(holder.contains("@GameTestHolder"));
        assertTrue(holder.contains("NAMESPACE = \"cruciblecraft_machines\""));
        assertTrue(holder.contains("targetCatalogAndRuntimeIdsMatch"));
        assertTrue(Files.isRegularFile(Path.of(
                "src/main/resources/data/cruciblecraft_machines/structure/empty.nbt")));
        assertTrue(Files.isRegularFile(Path.of(
                "src/main/resources/data/cruciblecraft_machines/gametest/structure/empty.nbt")));
    }

    private static boolean isOpeningId(String id) {
        return ModMachineVariants.isOpening(ResourceLocation.parse(id));
    }

    private static JsonObject loadCatalog() {
        try (var stream = MachineRuntimeEqualityTest.class.getClassLoader()
                .getResourceAsStream("data/cruciblecraft/machine_tiers.json")) {
            assertTrue(stream != null, "machine_tiers.json is missing");
            return JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }

    private static Set<String> ids(JsonObject document, boolean skipGeneric) {
        return document.getAsJsonArray("variants").asList().stream()
                .map(row -> row.getAsJsonObject())
                .filter(row -> {
                    if (!skipGeneric) {
                        return true;
                    }
                    var profile = row.getAsJsonObject("resourceProfile");
                    return profile == null
                            || !profile.has("skipGenericRegistration")
                            || !profile.get("skipGenericRegistration")
                                    .getAsBoolean();
                })
                .map(row -> row.get("id").getAsString())
                .collect(Collectors.toCollection(TreeSet::new));
    }
}
