package com.masson.cruciblecraft.nuclear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.material.HotFluidRegistrationGate;

class NuclearHotFluidCatalogTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path MATRIX = ROOT.resolve(
            "tools/waves/runtime/fission-hot-fluids/d0_hot_fluid_source_matrix.json");
    private static final Path GATE = ROOT.resolve(
            "src/main/resources/data/cruciblecraft/hot_fluid_gate.json");
    private static final Path CELLS = ROOT.resolve(
            "src/main/resources/data/cruciblecraft/cell_content_gate.json");
    private static final Path MANIFEST = ROOT.resolve(
            "src/main/resources/assets/cruciblecraft/gt6_fission_hot_fluids_art_manifest.json");
    private static final Path ASSETS = ROOT.resolve(
            "src/main/resources/assets/cruciblecraft");
    private static final String WAVE_NS =
            "src/main/resources/data/cruciblecraft_wave_runtime_fission_hot_fluids";
    private static final Set<String> HOT_IDS = Set.of(
            "hot_molten_tin",
            "hot_molten_sodium",
            "hot_semiheavy_water",
            "hot_heavy_water",
            "hot_tritiated_water",
            "hot_molten_licl",
            "hot_carbon_dioxide",
            "hot_helium");

    @Test
    void sourceMatrixLocksElevenByNineByEight() throws Exception {
        JsonObject matrix = parse(MATRIX);
        assertEquals(11, matrix.get("source_contract_rows").getAsInt());
        assertEquals(9, matrix.get("cc_owned_conversion_rows").getAsInt());
        assertEquals(8, matrix.get("hot_output_identity_rows").getAsInt());
        assertEquals(2, matrix.get("core_identity_count").getAsInt());
        assertEquals(0, matrix.get("missing").getAsInt());
        assertEquals(0, matrix.get("extra").getAsInt());
        assertEquals(0, matrix.get("duplicate").getAsInt());
        assertEquals(0, matrix.get("hot_identity_aliases").getAsInt());
        assertEquals(0, matrix.get("stand_in_fluids").getAsInt());
        assertTrue(matrix.get("1x1_2x2_branches_identical").getAsBoolean());
        assertEquals(
                2,
                matrix.getAsJsonObject("temperature_contract")
                        .get("choice")
                        .getAsInt());
        assertEquals(
                "HU",
                matrix.getAsJsonObject("temperature_contract")
                        .get("heat_unit")
                        .getAsString());
        assertEquals(
                "blocked",
                matrix.getAsJsonObject("temperature_contract")
                        .get("temperature_k")
                        .getAsString());
        JsonArray branches = matrix.getAsJsonArray("branches");
        assertEquals(11, branches.size());
        int ready = 0;
        Set<String> hotOutputs = new HashSet<>();
        for (JsonElement element : branches) {
            JsonObject row = element.getAsJsonObject();
            String disposition = row.get("disposition").getAsString();
            if ("ready".equals(disposition)) {
                ready++;
                String output = row.get("hot_output_registry_identity").getAsString();
                if (!"cruciblecraft:steam".equals(output)) {
                    assertTrue(hotOutputs.add(output), "duplicate hot " + output);
                }
            } else {
                assertTrue(
                        "blocked".equals(disposition)
                                || "out_of_scope_external".equals(disposition),
                        "unowned row is not blocked: " + row.get("branch"));
            }
        }
        assertEquals(9, ready);
        assertEquals(8, hotOutputs.size());
    }

    @Test
    void gateRegistersEightIndependentIdentities() {
        assertEquals(8, HotFluidRegistrationGate.load().size());
        Set<String> ids = new HashSet<>();
        for (HotFluidRegistrationGate.Entry entry : HotFluidRegistrationGate.load()) {
            assertTrue(ids.add(entry.id()), "duplicate gate id " + entry.id());
            assertTrue(HOT_IDS.contains(entry.id()), "unexpected " + entry.id());
            assertFalse(entry.worldPlaceable());
            assertFalse(entry.bucket());
            assertTrue(entry.id().startsWith("hot_"));
            assertFalse(entry.id().equals(entry.sourceMaterialId()));
        }
        assertEquals(HOT_IDS, ids);
    }

    @Test
    void cellGateAndArtManifestCoverEightHotFluids() throws Exception {
        JsonArray cells = parse(CELLS).getAsJsonArray("fluids");
        Set<String> cellIds = new HashSet<>();
        for (JsonElement element : cells) {
            String id = element.getAsJsonObject().get("id").getAsString();
            if (id.startsWith("cruciblecraft:hot_")) {
                cellIds.add(id.substring("cruciblecraft:".length()));
            }
        }
        assertEquals(HOT_IDS, cellIds);
        JsonObject manifest = parse(MANIFEST);
        JsonArray imports = manifest.getAsJsonArray("imports");
        assertTrue(imports.size() >= 8);
        Set<String> destinations = new HashSet<>();
        for (JsonElement element : imports) {
            String destination = element.getAsJsonObject()
                    .get("destination")
                    .getAsString();
            Path file = ASSETS.resolve(destination.substring(
                    "assets/cruciblecraft/".length()));
            assertTrue(Files.isRegularFile(file), "missing art " + destination);
            if (destination.endsWith(".png") && !destination.endsWith(".mcmeta")) {
                String name = Path.of(destination).getFileName().toString();
                destinations.add(name.substring(0, name.length() - 4));
            }
        }
        assertEquals(HOT_IDS, destinations);
        assertTrue(Files.isRegularFile(Path.of(WAVE_NS + "/structure/empty.nbt")));
        assertTrue(Files.isRegularFile(Path.of(
                WAVE_NS + "/gametest/structure/empty.nbt")));
        assertTrue(Files.isRegularFile(GATE));
    }

    private static JsonObject parse(Path path) throws Exception {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
}
