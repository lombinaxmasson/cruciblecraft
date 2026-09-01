package com.masson.cruciblecraft.scale;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Scale workload identity. Counts, seed and ticks come only from
 * {@code tools/t24_workload_manifest.json}; this class does not keep a
 * second 500/2,000 constant table.
 */
public final class ScaleWorkloadSpec {
    public static final String MANIFEST_RELATIVE =
            "tools/t24_workload_manifest.json";

    private final String workloadIdentity;
    private final Map<String, Scenario> scenarios;

    private ScaleWorkloadSpec(
            String workloadIdentity, Map<String, Scenario> scenarios) {
        this.workloadIdentity = workloadIdentity;
        this.scenarios = Map.copyOf(scenarios);
    }

    public static ScaleWorkloadSpec load() {
        return load(findRepoRoot().resolve(MANIFEST_RELATIVE));
    }

    public static ScaleWorkloadSpec load(Path manifest) {
        Objects.requireNonNull(manifest, "manifest");
        try {
            JsonObject root = JsonParser.parseString(
                    Files.readString(manifest, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            String identity = root.get("workload_identity").getAsString();
            JsonObject rows = root.getAsJsonObject("scenarios");
            LinkedHashMap<String, Scenario> scenarios = new LinkedHashMap<>();
            for (String name : rows.keySet()) {
                scenarios.put(name, Scenario.from(name, rows.getAsJsonObject(name)));
            }
            return new ScaleWorkloadSpec(identity, scenarios);
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "Cannot read scale workload manifest at " + manifest, failure);
        }
    }

    public String workloadIdentity() {
        return workloadIdentity;
    }

    public Scenario scenario(String name) {
        Scenario scenario = scenarios.get(name);
        if (scenario == null) {
            throw new IllegalArgumentException("Unknown scenario: " + name);
        }
        return scenario;
    }

    public Map<String, Scenario> scenarios() {
        return scenarios;
    }

    public static Path findRepoRoot() {
        Path dir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (dir != null) {
            if (Files.isRegularFile(dir.resolve("gradle.properties"))
                    && Files.isRegularFile(dir.resolve(MANIFEST_RELATIVE))) {
                return dir;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException(
                "Cannot locate repository root from " + System.getProperty("user.dir"));
    }

    public record Scenario(
            String name,
            int fluidPipes,
            int itemPipes,
            int covers,
            int energyConverters,
            int processingMachines,
            int multiblocks,
            int petroleumChains,
            int warmupTicks,
            int samplingTicks,
            long seed,
            int loadedChunksX,
            int loadedChunksZ,
            int pipesDuePerTick,
            int pipesTotal) {
        static Scenario from(String name, JsonObject row) {
            JsonObject counts = row.getAsJsonObject("counts");
            JsonObject ticks = row.getAsJsonObject("ticks");
            JsonObject chunks = row.getAsJsonObject("loaded_chunks");
            JsonObject derived = row.getAsJsonObject("derived");
            int fluid = counts.get("fluid_pipes").getAsInt();
            int item = counts.get("item_pipes").getAsInt();
            int pipesTotal = derived.get("pipes_total").getAsInt();
            if (pipesTotal != fluid + item) {
                throw new IllegalStateException(
                        name + " pipes_total does not match fluid+item counts");
            }
            return new Scenario(
                    name,
                    fluid,
                    item,
                    counts.get("covers").getAsInt(),
                    counts.get("energy_converters").getAsInt(),
                    counts.get("processing_machines").getAsInt(),
                    counts.get("multiblocks").getAsInt(),
                    counts.get("petroleum_chains").getAsInt(),
                    ticks.get("warmup").getAsInt(),
                    ticks.get("sampling").getAsInt(),
                    row.get("seed").getAsLong(),
                    chunks.get("x").getAsInt(),
                    chunks.get("z").getAsInt(),
                    derived.get("pipes_due_per_tick").getAsInt(),
                    pipesTotal);
        }
    }
}
