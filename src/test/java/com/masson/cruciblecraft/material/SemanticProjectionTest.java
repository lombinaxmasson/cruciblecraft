package com.masson.cruciblecraft.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class SemanticProjectionTest {
    @Test
    void committedGateSemanticRootMatchesPythonSidecar() throws Exception {
        Path gatePath = Path.of(
                "src/main/resources/data/cruciblecraft/material_registration_gate.json");
        Path sidecarPath = Path.of("tools/material_registration_gate.currentness.json");
        JsonObject gate = JsonParser.parseString(
                Files.readString(gatePath, StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject sidecar = JsonParser.parseString(
                Files.readString(sidecarPath, StandardCharsets.UTF_8)).getAsJsonObject();
        String actual = SemanticProjection.semanticRootSha256(gate);
        assertEquals(
                sidecar.get("semantic_root_sha256").getAsString(),
                actual,
                "Java gate projection must match tools/semantic_projection.py");
        assertEquals(1, sidecar.get("semantic_projection_version").getAsInt());
        assertEquals(
                SemanticProjection.KIND_GATE,
                sidecar.get("semantic_projection_kind").getAsString());
        assertFalse(actual.isBlank());
    }
}
