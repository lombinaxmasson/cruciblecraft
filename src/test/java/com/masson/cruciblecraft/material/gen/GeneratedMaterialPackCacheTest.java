package com.masson.cruciblecraft.material.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.material.def.ThermalProperties;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixDefinition;

class GeneratedMaterialPackCacheTest {
    @Test
    void plansServerAndClientOutputsWithoutClientClasses() {
        assertTrue(GeneratedMaterialPackPlan.forDistribution(false).serverData());
        assertFalse(GeneratedMaterialPackPlan.forDistribution(false).clientAssets());
        assertTrue(GeneratedMaterialPackPlan.forDistribution(true).serverData());
        assertTrue(GeneratedMaterialPackPlan.forDistribution(true).clientAssets());
    }

    @Test
    void fastHitSkipsRewriteAndStrictModeRepairsTampering(@TempDir Path root)
            throws Exception {
        Map<String, String> files = Map.of(
                "pack.mcmeta", "metadata",
                "data/test/tags/item/example.json", "tag");
        AtomicInteger planCalls = new AtomicInteger();
        assertTrue(GeneratedMaterialPackCache.ensure(root, "structure-a", () -> {
            planCalls.incrementAndGet();
            return files;
        }));
        assertEquals(1, planCalls.get());
        Path tag = root.resolve("data/test/tags/item/example.json");
        FileTime firstWrite = Files.getLastModifiedTime(tag);
        Thread.sleep(20L);
        planCalls.set(0);
        assertFalse(GeneratedMaterialPackCache.ensure(root, "structure-a", () -> {
            planCalls.incrementAndGet();
            return files;
        }));
        assertEquals(0, planCalls.get(), "hot cache hit must not construct the file plan");
        assertTrue(GeneratedMaterialPackCache.valid(root, "structure-a", files));
        assertTrue(firstWrite.equals(Files.getLastModifiedTime(tag)),
                "cache hit must not rewrite generated files");

        Files.writeString(tag, "bad");
        assertFalse(GeneratedMaterialPackCache.valid(root, "structure-a", files));
        assertFalse(GeneratedMaterialPackCache.ensure(root, "structure-a", files));
        assertTrue(GeneratedMaterialPackCache.ensureStrict(root, "structure-a", files));
        assertTrue(Files.readString(tag).equals("tag"));

        Files.writeString(root.resolve("unexpected.json"), "stale");
        assertFalse(GeneratedMaterialPackCache.valid(root, "structure-a", files));
        assertFalse(GeneratedMaterialPackCache.ensure(root, "structure-a", files));
        assertTrue(GeneratedMaterialPackCache.ensureStrict(root, "structure-a", files));
        assertFalse(Files.exists(root.resolve("unexpected.json")));

        assertTrue(GeneratedMaterialPackCache.ensure(root, "structure-b", files));
    }

    @Test
    void recreateRejectsKeysEscapingTheNormalizedRoot(@TempDir Path temporary)
            throws Exception {
        Path root = temporary.resolve("pack");
        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> GeneratedMaterialPackCache.ensure(
                        root,
                        "escape",
                        Map.of("../escaped.json", "nope")));

        assertTrue(exception.getMessage().contains("../escaped.json"));
        assertTrue(exception.getMessage().contains(root.toAbsolutePath().normalize().toString()));
        assertFalse(Files.exists(temporary.resolve("escaped.json")));
    }

    @Test
    void generationFingerprintIncludesEveryOutputShapingInput() {
        MaterialDefinition tierOne = material(1);
        MaterialDefinition tierTwo = material(2);
        Map<String, List<MaterialPrefix>> ingotGate =
                Map.of(tierOne.id(), List.of(MaterialPrefixes.INGOT));
        Map<String, List<MaterialPrefix>> plateGate =
                Map.of(tierOne.id(), List.of(MaterialPrefixes.PLATE));
        List<MaterialPrefixDefinition> prefixes =
                new ArrayList<>(MaterialPrefixCatalog.definitions());
        List<MaterialPrefixDefinition> changedModel = new ArrayList<>(prefixes);
        MaterialPrefixDefinition ingot =
                MaterialPrefixCatalog.definition(MaterialPrefixes.INGOT);
        changedModel.set(changedModel.indexOf(ingot), new MaterialPrefixDefinition(
                ingot.prefix(),
                ingot.serializedPath(),
                ingot.units(),
                ingot.generationFlag(),
                ingot.tagDirectory(),
                ingot.tagNamespace(),
                ingot.modelTemplate(),
                "minecraft:item/diamond",
                ingot.aliases(),
                ingot.impliedPrefixes(),
                ingot.capabilities()));

        String base = GeneratedMaterialPack.generationFingerprint(
                "server", List.of(tierOne), ingotGate, prefixes, 48, "generator-a");
        assertNotEquals(base, GeneratedMaterialPack.generationFingerprint(
                "client", List.of(tierOne), ingotGate, prefixes, 48, "generator-a"));
        assertNotEquals(base, GeneratedMaterialPack.generationFingerprint(
                "server", List.of(tierOne), plateGate, prefixes, 48, "generator-a"));
        assertNotEquals(base, GeneratedMaterialPack.generationFingerprint(
                "server", List.of(tierTwo), ingotGate, prefixes, 48, "generator-a"));
        assertNotEquals(base, GeneratedMaterialPack.generationFingerprint(
                "server", List.of(tierOne), ingotGate, changedModel, 48, "generator-a"));
        assertNotEquals(base, GeneratedMaterialPack.generationFingerprint(
                "server", List.of(tierOne), ingotGate, prefixes, 49, "generator-a"));
        assertNotEquals(base, GeneratedMaterialPack.generationFingerprint(
                "server", List.of(tierOne), ingotGate, prefixes, 48, "generator-b"));
    }

    @Test
    void generatedRootsRequireExpectedSideSuffix(@TempDir Path temporary) {
        Path server = temporary.resolve(".generated-material-pack").resolve("server");
        assertEquals(
                server.toAbsolutePath().normalize(),
                GeneratedMaterialPack.requireGeneratedRoot(server, "server"));
        assertThrows(
                IllegalArgumentException.class,
                () -> GeneratedMaterialPack.requireGeneratedRoot(
                        temporary.resolve("server"),
                        "server"));
    }

    @Test
    void generatorIdentityFailsWhenClassBytesAreUnavailable() {
        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> GeneratedMaterialPack.outputGeneratorIdentity(null));

        assertTrue(failure.getMessage().contains("class bytes are unavailable"));
    }

    private static MaterialDefinition material(int tier) {
        return new MaterialDefinition(
                "testium",
                "testium",
                Optional.empty(),
                tier,
                "#FFFFFF",
                "metallic",
                List.of(MaterialPrefixes.INGOT, MaterialPrefixes.PLATE),
                Map.of(),
                new ThermalProperties(1_000, 2_000, 8),
                false,
                Map.of(),
                false);
    }
}
