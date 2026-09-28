package com.masson.cruciblecraft.recipe.gt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.test.MinecraftTestBootstrap;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.connection.ConnectionType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Release-bundle contract: physical packing must preserve the complete
 * authored family/stable-id set, and every generated bundle must fit the
 * existing per-entry network ceiling.
 */
class CompactFamilyBundleLoaderTest {
    private static final Path SOURCE_ROOT = Path.of(
            "src/recipe_generated/resources/data/cruciblecraft/recipe");
    private static final Path BUNDLE_ROOT = Path.of(
            "build/generated/release-compact-family-bundles/data/cruciblecraft/recipe");

    private static RegistryAccess registries;

    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.bootstrap();
        registries = new RegistryAccess.ImmutableRegistryAccess(
                BuiltInRegistries.REGISTRY.stream().toList());
    }

    @Test
    @Timeout(180)
    void bundlesPreserveFamilyMembershipAndWireCeiling() throws IOException {
        assertTrue(Files.isDirectory(BUNDLE_ROOT), BUNDLE_ROOT.toString());
        List<Path> bundleFiles;
        try (Stream<Path> paths = Files.walk(BUNDLE_ROOT)) {
            bundleFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList();
        }
        assertTrue(!bundleFiles.isEmpty(), "release bundle task emitted no bundles");

        Set<String> sourceIds = new HashSet<>();
        Set<String> expectedSemanticKeys = new HashSet<>();
        int expectedRelations = 0;
        try (Stream<Path> paths = Files.walk(SOURCE_ROOT)) {
            for (Path path : paths
                    .filter(Files::isRegularFile)
                    .filter(candidate -> candidate.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList()) {
                JsonObject document = read(path);
                if (!"cruciblecraft:compact_gt_recipe_family".equals(
                        string(document, "type"))) {
                    continue;
                }
                String sourceId = sourceId(path);
                sourceIds.add(sourceId);
                expectedRelations += addSemanticKeys(
                        document,
                        expectedSemanticKeys);
            }
        }

        Set<String> bundledSourceIds = new HashSet<>();
        Set<String> bundledSemanticKeys = new HashSet<>();
        int bundledRelations = 0;
        int maxWireBytes = 0;
        for (Path bundleFile : bundleFiles) {
            JsonObject bundle = read(bundleFile);
            assertEquals(
                    "cruciblecraft:compact_gt_recipe_family_bundle",
                    string(bundle, "type"),
                    bundleFile.toString());
            JsonArray members = bundle.getAsJsonArray("families");
            assertTrue(members.size() >= 2, bundleFile.toString());

            List<CompactGTRecipeFamilyBundleEntry> entries =
                    new ArrayList<>(members.size());
            for (JsonElement memberElement : members) {
                JsonObject member = memberElement.getAsJsonObject();
                ResourceLocation sourceId =
                        ResourceLocation.parse(string(member, "source_id"));
                assertTrue(
                        bundledSourceIds.add(sourceId.toString()),
                        "duplicate bundled source " + sourceId);
                JsonObject definition = member.getAsJsonObject("definition");
                bundledRelations += addSemanticKeys(
                        definition,
                        bundledSemanticKeys);
                entries.add(new CompactGTRecipeFamilyBundleEntry(
                        sourceId,
                        CompactGTRecipeFamilyGeneratedSupport
                                .sourceFromGenerated(
                                        sourcePath(sourceId),
                                        definition,
                                        registries)
                                .definition()));
            }

            CompactGTRecipeFamilyBundle bundleValue =
                    new CompactGTRecipeFamilyBundle(entries);
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                    Unpooled.buffer(),
                    registries,
                    ConnectionType.NEOFORGE);
            CompactGTRecipeFamilyStreamCodec.BUNDLE.encode(buffer, bundleValue);
            int wireBytes = buffer.writerIndex();
            maxWireBytes = Math.max(maxWireBytes, wireBytes);
            assertTrue(
                    wireBytes <= CompactRecipeWireLimits.MAX_RECIPE_ENTRY_WIRE_BYTES,
                    bundleFile + " wire bytes=" + wireBytes);
            CompactGTRecipeFamilyBundle decoded =
                    CompactGTRecipeFamilyStreamCodec.BUNDLE.decode(buffer);
            assertEquals(entries.size(), decoded.families().size(), bundleFile.toString());
            assertEquals(0, buffer.readableBytes(), bundleFile.toString());
        }

        assertTrue(
                sourceIds.containsAll(bundledSourceIds),
                "bundle contains an unknown source id");
        assertEquals(
                expectedRelations,
                bundledRelations + countUnbundledRelations(sourceIds, bundledSourceIds),
                "bundle relation coverage");
        assertEquals(
                expectedSemanticKeys,
                bundledSemanticKeysWithUnbundled(
                        bundledSemanticKeys,
                        bundledSourceIds),
                "bundle family/stable-id coverage");
        assertTrue(
                maxWireBytes <= CompactRecipeWireLimits.MAX_RECIPE_ENTRY_WIRE_BYTES);
    }

    private static Set<String> bundledSemanticKeysWithUnbundled(
            Set<String> bundledSemanticKeys,
            Set<String> bundledSourceIds) throws IOException {
        Set<String> keys = new HashSet<>(bundledSemanticKeys);
        try (Stream<Path> paths = Files.walk(SOURCE_ROOT)) {
            for (Path path : paths
                    .filter(Files::isRegularFile)
                    .filter(candidate -> candidate.getFileName().toString().endsWith(".json"))
                    .toList()) {
                JsonObject document = read(path);
                if (!"cruciblecraft:compact_gt_recipe_family".equals(
                        string(document, "type"))) {
                    continue;
                }
                String id = sourceId(path);
                if (!bundledSourceIds.contains(id)) {
                    addSemanticKeys(document, keys);
                }
            }
        }
        return keys;
    }

    private static int countUnbundledRelations(
            Set<String> sourceIds,
            Set<String> bundledSourceIds) throws IOException {
        int count = 0;
        try (Stream<Path> paths = Files.walk(SOURCE_ROOT)) {
            for (Path path : paths
                    .filter(Files::isRegularFile)
                    .filter(candidate -> candidate.getFileName().toString().endsWith(".json"))
                    .toList()) {
                JsonObject document = read(path);
                if ("cruciblecraft:compact_gt_recipe_family".equals(
                        string(document, "type"))
                        && !bundledSourceIds.contains(sourceId(path))) {
                    count += relationCount(document);
                }
            }
        }
        return count;
    }

    private static int addSemanticKeys(
            JsonObject document,
            Set<String> output) {
        String familyId = string(document, "family_id");
        int relations = 0;
        if (document.has("matrix")) {
            for (JsonElement row
                    : document.getAsJsonObject("matrix").getAsJsonArray("rows")) {
                output.add(familyId + "|" + row.getAsJsonArray().get(3).getAsString());
                relations++;
            }
        } else if (document.has("relations")) {
            for (JsonElement relation : document.getAsJsonArray("relations")) {
                output.add(familyId + "|"
                        + relation.getAsJsonObject().get("stable_id").getAsString());
                relations++;
            }
        }
        return relations;
    }

    private static int relationCount(JsonObject document) {
        return document.has("matrix")
                ? document.getAsJsonObject("matrix").getAsJsonArray("rows").size()
                : document.has("relations")
                        ? document.getAsJsonArray("relations").size()
                        : 0;
    }

    private static JsonObject read(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }

    private static String string(JsonObject object, String key) {
        return object.get(key).getAsString();
    }

    private static String sourceId(Path path) {
        String relative = SOURCE_ROOT.relativize(path).toString()
                .replace('\\', '/');
        return "cruciblecraft:" + relative.substring(0, relative.length() - 5);
    }

    private static Path sourcePath(ResourceLocation sourceId) {
        return SOURCE_ROOT.resolve(sourceId.getPath() + ".json");
    }
}
