package com.masson.cruciblecraft.recipe.gt;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.recipe.rule.MaterialRule;
import com.masson.cruciblecraft.recipe.rule.MaterialRuleRecipe;
import com.masson.cruciblecraft.recipe.rule.MaterialRuleSerializer;
import com.masson.cruciblecraft.registry.ModRecipeMaps;
import com.masson.cruciblecraft.test.MinecraftTestBootstrap;
import com.mojang.serialization.JsonOps;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.network.connection.ConnectionType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class CompactGTRecipeFamilySyncSizeTest {
    /** Committed compact RecipeHolders under recipe_generated, excluding off-tree bath waves. */
    private static final int EXPECTED_COMMITTED_COMPACT_ENTRIES = 14986;
    private static final int EXPECTED_LARGE_JSON_ENTRIES = 42;
    private static final int EXPECTED_SANDING_FRAGMENTS = 9;
    private static final int EXPECTED_SANDING_RELATIONS = 7_637;
    private static final long ONE_MIB = 1024L * 1024L;
    private static final String BATH_0025_FAMILY_ID = "gt.recipe.bath#0025";

    private static RegistryAccess registries;
    private static CompactGTRecipeFamilySerializer serializer;

    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.bootstrap();
        registries = new RegistryAccess.ImmutableRegistryAccess(
                BuiltInRegistries.REGISTRY.stream().toList());
        serializer = new CompactGTRecipeFamilySerializer();
    }

    @Test
    void streamCodecIsBoundedBinaryNotNbt() {
        assertSame(
                CompactGTRecipeFamilyStreamCodec.INSTANCE,
                serializer.streamCodec());
    }

    @Test
    void oreHostBulkFamiliesStayUnderWireCeiling() throws IOException {
        List<Path> files = new ArrayList<>();
        for (String directory : List.of(
                "src/recipe_generated/resources/data/cruciblecraft/recipe/crusher/prefix_regular",
                "src/recipe_generated/resources/data/cruciblecraft/recipe/sifter/prefix_regular")) {
            try (Stream<Path> paths = Files.list(Path.of(directory))) {
                paths.filter(path -> path.getFileName().toString().endsWith(".json"))
                        .sorted()
                        .forEach(files::add);
            }
        }
        List<String> report = new ArrayList<>();
        int maxBytes = 0;
        String maxFamily = "";
        for (Path path : files) {
            CompactRecipeFamilySource source =
                    CompactGTRecipeFamilyGeneratedSupport.sourceFromGenerated(
                            path, readJson(path), registries);
            RegistryFriendlyByteBuf buffer = buffer();
            String familyId = source.definition().familyId();
            try {
                serializer.streamCodec().encode(
                        buffer, new CompactGTRecipeFamilyEntry(source.definition()));
            } catch (EncoderException failure) {
                report.add(familyId + " FAIL " + failure.getMessage());
                continue;
            }
            int wireBytes = buffer.writerIndex();
            if (wireBytes > maxBytes) {
                maxBytes = wireBytes;
                maxFamily = familyId;
            }
            if (wireBytes > CompactRecipeWireLimits.MAX_RECIPE_ENTRY_WIRE_BYTES) {
                report.add(familyId + " bytes=" + wireBytes);
                continue;
            }
            serializer.streamCodec().decode(buffer);
            if (buffer.readableBytes() != 0) {
                report.add(familyId + " leftover=" + buffer.readableBytes());
            }
        }
        String summary = String.join("\n", report)
                + "\nmax " + maxBytes + " at " + maxFamily;
        assertTrue(report.isEmpty(), summary);
        assertTrue(
                maxBytes > 0 && maxBytes <= CompactRecipeWireLimits.MAX_RECIPE_ENTRY_WIRE_BYTES,
                summary);
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.MINUTES)
    void allLiveCompactEntriesEncodeUnderCeiling() throws IOException {
        List<Path> compactFiles = listCompactFamilyFiles();
        long committed = compactFiles.stream()
                .filter(path -> !isOffTreeBathWave(path))
                .count();
        assertEquals(
                EXPECTED_COMMITTED_COMPACT_ENTRIES,
                committed,
                "committed compact RecipeHolder count drifted");
        int largeJson = 0;
        int maxBytes = 0;
        String maxFamily = "";
        for (Path path : compactFiles) {
            long jsonBytes = Files.size(path);
            if (jsonBytes > ONE_MIB) {
                largeJson++;
            }
            CompactRecipeFamilySource source =
                    CompactGTRecipeFamilyGeneratedSupport.sourceFromGenerated(
                            path, readJson(path), registries);
            CompactGTRecipeFamilyEntry original =
                    new CompactGTRecipeFamilyEntry(source.definition());
            RegistryFriendlyByteBuf buffer = buffer();
            try {
                serializer.streamCodec().encode(buffer, original);
            } catch (EncoderException failure) {
                fail(path.toString().replace('\\', '/')
                        + " family "
                        + source.definition().familyId()
                        + ": "
                        + failure.getMessage());
            }
            int wireBytes = buffer.writerIndex();
            assertTrue(
                    wireBytes <= CompactRecipeWireLimits.MAX_RECIPE_ENTRY_WIRE_BYTES,
                    () -> source.definition().familyId()
                            + " wire size " + wireBytes
                            + " exceeds "
                            + CompactRecipeWireLimits.MAX_RECIPE_ENTRY_WIRE_BYTES);
            if (wireBytes > maxBytes) {
                maxBytes = wireBytes;
                maxFamily = source.definition().familyId();
            }
            CompactGTRecipeFamilyEntry decoded = serializer.streamCodec().decode(buffer);
            assertEquals(0, buffer.readableBytes(), source.definition().familyId());
            assertSameFamily(original.definition(), decoded.definition());
        }
        assertEquals(
                EXPECTED_LARGE_JSON_ENTRIES,
                largeJson,
                "JSON > 1 MiB family count drifted");
        assertTrue(
                maxBytes > 0,
                "encoded compact families; max " + maxBytes + " bytes at " + maxFamily);
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.MINUTES)
    void sandingFragmentsEncodeAndReassembleOnDedicatedClientPath() throws IOException {
        List<Path> fragments = listCompactFamilyFiles().stream()
                .filter(path -> path.getFileName().toString().startsWith(
                        "gt_recipe_sharpener_0000_fragment_"))
                .toList();
        assertEquals(EXPECTED_SANDING_FRAGMENTS, fragments.size());
        List<CompactRecipeFamilySource> sources = new ArrayList<>();
        for (Path path : fragments) {
            CompactRecipeFamilySource source =
                    CompactGTRecipeFamilyGeneratedSupport.sourceFromGenerated(
                            path, readJson(path), registries);
            assertEquals("gt.recipe.sharpener#0000", source.definition().familyId());
            assertTrue(source.definition().transportFragment().isPresent());
            CompactGTRecipeFamilyEntry original =
                    new CompactGTRecipeFamilyEntry(source.definition());
            RegistryFriendlyByteBuf buffer = buffer();
            serializer.streamCodec().encode(buffer, original);
            int wireBytes = buffer.writerIndex();
            assertTrue(
                    wireBytes <= CompactRecipeWireLimits.MAX_RECIPE_ENTRY_WIRE_BYTES,
                    () -> source.id() + " wire size " + wireBytes);
            CompactGTRecipeFamilyEntry decoded = serializer.streamCodec().decode(buffer);
            assertEquals(0, buffer.readableBytes(), source.id().toString());
            assertSameFamily(original.definition(), decoded.definition());
            sources.add(new CompactRecipeFamilySource(source.id(), decoded.definition()));
        }
        java.util.Collections.reverse(sources);
        List<CompactRecipeFamilySource> assembled =
                CompactTransportFragments.reassemble(sources);
        assertEquals(1, assembled.size());
        assertEquals(
                EXPECTED_SANDING_RELATIONS,
                assembled.getFirst().authoredRelations().size());
        assertTrue(assembled.getFirst().definition().transportFragment().isEmpty());
        var server = CompactRecipeFamilyProvider.prepare(
                ModRecipeMaps.SANDING,
                assembled,
                1L,
                CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                CompactRecipeFamilyProvider.MaterializationPolicy.immediate());
        var dedicated = CompactRecipeFamilyProvider.prepare(
                ModRecipeMaps.SANDING,
                assembled,
                1L,
                CompactRecipeFamilyProvider.RuntimeSide.DEDICATED_CLIENT,
                CompactRecipeFamilyProvider.MaterializationPolicy.immediate());
        assertEquals(EXPECTED_SANDING_RELATIONS, server.logicalRecipeCount());
        assertEquals(server.stableFingerprint(), dedicated.stableFingerprint());
        assertEquals(
                server.recipeIds(),
                dedicated.recipeIds());
    }

    @Test
    void bath0025RoundTripsFieldsAndFingerprint() throws IOException {
        Assumptions.assumeTrue(
                CompactGTRecipeFamilyGeneratedSupport.hasGeneratedFamiliesRecursive(
                        CompactGTRecipeFamilyGeneratedSupport.bathIdentityGeneratedRoot()),
                "bath identity generated compact families are not available");
        Path path = compactFileNamed("gt_recipe_bath_0025.json");
        CompactRecipeFamilySource originalSource =
                CompactGTRecipeFamilyGeneratedSupport.sourceFromGenerated(
                        path, readJson(path), registries);
        assertEquals(BATH_0025_FAMILY_ID, originalSource.definition().familyId());
        CompactGTRecipeFamilyEntry original =
                new CompactGTRecipeFamilyEntry(originalSource.definition());
        assertTrue(original.definition().matrix().isPresent());
        assertEquals(0, original.definition().relations().size());
        assertEquals(1116, original.definition().authoredRelations().size());
        RegistryFriendlyByteBuf buffer = buffer();
        serializer.streamCodec().encode(buffer, original);
        int wireBytes = buffer.writerIndex();
        assertTrue(
                wireBytes <= CompactRecipeWireLimits.MAX_RECIPE_ENTRY_WIRE_BYTES,
                "bath#0025 wire size " + wireBytes);
        assertTrue(
                wireBytes < 300_000,
                "bath#0025 matrix wire should be far below the expanded table: "
                        + wireBytes);
        CompactGTRecipeFamilyEntry decoded = serializer.streamCodec().decode(buffer);
        assertSameFamily(original.definition(), decoded.definition());
        CompactRecipeFamilySource decodedSource = new CompactRecipeFamilySource(
                originalSource.id(), decoded.definition());
        var originalSnapshot = CompactRecipeFamilyProvider.prepare(
                ModRecipeMaps.BATH,
                List.of(originalSource),
                1L,
                CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                CompactRecipeFamilyProvider.MaterializationPolicy.immediate());
        var decodedSnapshot = CompactRecipeFamilyProvider.prepare(
                ModRecipeMaps.BATH,
                List.of(decodedSource),
                1L,
                CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                CompactRecipeFamilyProvider.MaterializationPolicy.immediate());
        assertEquals(
                originalSnapshot.stableFingerprint(),
                decodedSnapshot.stableFingerprint());
        assertEquals(1116, originalSnapshot.logicalRecipeCount());
        assertEquals(1116, decodedSnapshot.logicalRecipeCount());
        assertEquals(0, original.definition().relations().size());
        assertEquals(0, decoded.definition().relations().size());
    }

    @Test
    void decodeRejectsOversizePayloadBeforeBody() {
        RegistryFriendlyByteBuf buffer = buffer();
        buffer.writeByte(CompactRecipeWireLimits.WIRE_VERSION);
        buffer.writeVarInt(CompactRecipeWireLimits.MAX_RECIPE_ENTRY_WIRE_BYTES + 1);
        DecoderException thrown = assertThrows(
                DecoderException.class,
                () -> serializer.streamCodec().decode(buffer));
        assertTrue(thrown.getMessage().contains("payload size"));
    }

    @Test
    void decodeRejectsOversizeRelationCountBeforeAllocating() {
        RegistryFriendlyByteBuf payload = buffer();
        payload.writeUtf("gt.recipe.test#0001", CompactRecipeWireLimits.MAX_FAMILY_ID_LENGTH);
        ResourceLocation.STREAM_CODEC.encode(
                payload, ResourceLocation.fromNamespaceAndPath("cruciblecraft", "bath"));
        payload.writeUtf("rev", CompactRecipeWireLimits.MAX_SOURCE_REVISION_LENGTH);
        payload.writeBoolean(false);
        payload.writeBoolean(false);
        payload.writeBoolean(false);
        payload.writeByte(CompactRecipeWireLimits.WIRE_FORM_INLINE);
        for (int index = 0; index < 6; index++) {
            payload.writeVarInt(0);
        }
        payload.writeByte(0);
        payload.writeVarInt(CompactRecipeWireLimits.DECODE_RELATIONS_CEILING + 1);
        RegistryFriendlyByteBuf framed = frame(payload);
        DecoderException thrown = assertThrows(
                DecoderException.class,
                () -> serializer.streamCodec().decode(framed));
        assertTrue(thrown.getMessage().contains("relations"));
    }

    @Test
    void decodeRejectsOversizeFamilyId() {
        String tooLong = "x".repeat(CompactRecipeWireLimits.MAX_FAMILY_ID_LENGTH + 1);
        RegistryFriendlyByteBuf bounded = buffer();
        EncoderException encodeThrown = assertThrows(
                EncoderException.class,
                () -> bounded.writeUtf(
                        tooLong, CompactRecipeWireLimits.MAX_FAMILY_ID_LENGTH));
        assertTrue(encodeThrown.getMessage().contains("too big"));

        RegistryFriendlyByteBuf payload = buffer();
        payload.writeUtf(tooLong);
        ResourceLocation.STREAM_CODEC.encode(
                payload, ResourceLocation.fromNamespaceAndPath("cruciblecraft", "bath"));
        payload.writeUtf("rev", CompactRecipeWireLimits.MAX_SOURCE_REVISION_LENGTH);
        payload.writeBoolean(false);
        payload.writeBoolean(false);
        payload.writeBoolean(false);
        payload.writeByte(CompactRecipeWireLimits.WIRE_FORM_INLINE);
        for (int index = 0; index < 6; index++) {
            payload.writeVarInt(0);
        }
        payload.writeByte(0);
        payload.writeVarInt(0);
        RegistryFriendlyByteBuf framed = frame(payload);
        assertThrows(
                DecoderException.class,
                () -> serializer.streamCodec().decode(framed));
    }

    @Test
    void decodeRejectsUnknownWireForm() {
        RegistryFriendlyByteBuf payload = buffer();
        payload.writeUtf("gt.recipe.test#0001", CompactRecipeWireLimits.MAX_FAMILY_ID_LENGTH);
        ResourceLocation.STREAM_CODEC.encode(
                payload, ResourceLocation.fromNamespaceAndPath("cruciblecraft", "bath"));
        payload.writeUtf("rev", CompactRecipeWireLimits.MAX_SOURCE_REVISION_LENGTH);
        payload.writeBoolean(false);
        payload.writeBoolean(false);
        payload.writeBoolean(false);
        payload.writeByte(99);
        RegistryFriendlyByteBuf framed = frame(payload);
        DecoderException thrown = assertThrows(
                DecoderException.class,
                () -> serializer.streamCodec().decode(framed));
        assertTrue(thrown.getMessage().contains("wire form"));
    }

    @Test
    void siblingSerializersStayUnderCeiling() throws Exception {
        GTRecipeEntry recipeEntry = new GTRecipeEntry(
                ResourceLocation.fromNamespaceAndPath("cruciblecraft", "bath"),
                new GTRecipe(
                        List.of(Ingredient.of(Items.IRON_INGOT)),
                        List.of(1),
                        List.of(ItemInputAction.CONSUME),
                        List.of(new ItemStack(Items.IRON_NUGGET)),
                        List.of(),
                        List.of(),
                        List.of(GTRecipe.GUARANTEED_CHANCE),
                        20,
                        0L,
                        0L,
                        true,
                        Optional.of(new GTRecipeProvenance(
                                "SOURCE_DERIVED",
                                Optional.of("gt.recipe.bath#0000")))));
        assertUnderCeiling(
                "GTRecipeEntry",
                new GTRecipeEntrySerializer(),
                recipeEntry);

        MaterialRuleRecipe materialRule = new MaterialRuleRecipe(loadGeneratedMaterialRule(
                "extruder_compact_rule_fixture/recipe/extruder/compact/normal_plate.json"));
        assertUnderCeiling(
                "MaterialRule",
                new MaterialRuleSerializer(),
                materialRule);

        CompactPublicationPolicyEntry policy = new CompactPublicationPolicyEntry(
                new CompactPublicationPolicyDefinition(
                        ResourceLocation.fromNamespaceAndPath("cruciblecraft", "bath"),
                        ResourceLocation.parse("cruciblecraft:bath/identity/tool_head"),
                        "immediate",
                        0,
                        List.of(),
                        CompactPublicationPolicyDefinition.ROUTING_SCHEMA_VERSION,
                        Optional.of("a".repeat(64)),
                        Optional.of(1),
                        Optional.of(1)));
        assertUnderCeiling(
                "CompactPublicationPolicy",
                new CompactPublicationPolicySerializer(),
                policy);

        CompactDedupRuleEntry dedup = new CompactDedupRuleEntry(
                new CompactDedupRuleDefinition(
                        ResourceLocation.fromNamespaceAndPath(
                                "cruciblecraft", "bath_identity_vs_remainder"),
                        "bath/identity",
                        CompactDedupRuleDefinition.PHASE_PRE_SNAPSHOT,
                        ResourceLocation.fromNamespaceAndPath("cruciblecraft", "bath"),
                        CompactDedupRuleDefinition.MODE_LOGICAL,
                        true,
                        new CompactDedupRuleDefinition.Selector(
                                CompactDedupRuleDefinition.Selector.KIND_GROUP,
                                List.of(ResourceLocation.parse(
                                        "cruciblecraft:bath/identity/tool_head")),
                                List.of()),
                        new CompactDedupRuleDefinition.Selector(
                                CompactDedupRuleDefinition.Selector.KIND_GROUP,
                                List.of(ResourceLocation.parse(
                                        "cruciblecraft:bath/remainder/tool_head")),
                                List.of())));
        assertUnderCeiling(
                "CompactDedupRule",
                new CompactDedupRuleSerializer(),
                dedup);
    }

    private static <T extends Recipe<?>> void assertUnderCeiling(
            String label,
            RecipeSerializer<T> sibling,
            T value) {
        RegistryFriendlyByteBuf buffer = buffer();
        sibling.streamCodec().encode(buffer, value);
        int bytes = buffer.writerIndex();
        assertTrue(
                bytes <= CompactRecipeWireLimits.MAX_RECIPE_ENTRY_WIRE_BYTES,
                () -> label + " wire size " + bytes + " exceeds 512 KiB");
        sibling.streamCodec().decode(buffer);
        assertEquals(0, buffer.readableBytes(), label);
    }

    private static void assertSameFamily(
            CompactGTRecipeFamilyDefinition expected,
            CompactGTRecipeFamilyDefinition actual) {
        assertEquals(expected.familyId(), actual.familyId());
        assertEquals(expected.targetMap(), actual.targetMap());
        assertEquals(expected.sourceRevision(), actual.sourceRevision());
        assertEquals(expected.publicationGroup(), actual.publicationGroup());
        assertEquals(expected.transportFragment(), actual.transportFragment());
        assertEquals(expected.parameterized(), actual.parameterized());
        assertEquals(expected.matrix().isPresent(), actual.matrix().isPresent());
        assertEquals(expected.relations().size(), actual.relations().size());
        List<CompactGTRecipeFamilyDefinition.Relation> expectedRows =
                expected.authoredRelations();
        List<CompactGTRecipeFamilyDefinition.Relation> actualRows =
                actual.authoredRelations();
        assertEquals(expectedRows.size(), actualRows.size());
        for (int index = 0; index < expectedRows.size(); index++) {
            CompactGTRecipeFamilyDefinition.Relation left = expectedRows.get(index);
            CompactGTRecipeFamilyDefinition.Relation right = actualRows.get(index);
            assertEquals(left.stableId(), right.stableId());
            assertEquals(left.itemInputCounts(), right.itemInputCounts());
            assertEquals(left.itemInputActions(), right.itemInputActions());
            assertEquals(left.outputChances(), right.outputChances());
            assertEquals(left.duration(), right.duration());
            assertEquals(left.eut(), right.eut());
            assertEquals(left.specialValue(), right.specialValue());
            assertEquals(left.canBeBuffered(), right.canBeBuffered());
            assertEquals(left.shadowOrder(), right.shadowOrder());
            assertEquals(left.provenance(), right.provenance());
            assertEquals(left.itemInputs().size(), right.itemInputs().size());
            for (int input = 0; input < left.itemInputs().size(); input++) {
                assertSameIngredient(
                        left.itemInputs().get(input),
                        right.itemInputs().get(input));
            }
            assertEquals(left.itemOutputs().size(), right.itemOutputs().size());
            for (int output = 0; output < left.itemOutputs().size(); output++) {
                ItemStack expectedStack = left.itemOutputs().get(output);
                ItemStack actualStack = right.itemOutputs().get(output);
                assertEquals(expectedStack.getItem(), actualStack.getItem());
                assertEquals(expectedStack.getCount(), actualStack.getCount());
            }
            assertEquals(left.fluidInputs().size(), right.fluidInputs().size());
            assertEquals(left.fluidOutputs().size(), right.fluidOutputs().size());
            for (int fluid = 0; fluid < left.fluidInputs().size(); fluid++) {
                assertEquals(
                        left.fluidInputs().get(fluid).getFluid(),
                        right.fluidInputs().get(fluid).getFluid());
                assertEquals(
                        left.fluidInputs().get(fluid).getAmount(),
                        right.fluidInputs().get(fluid).getAmount());
            }
            for (int fluid = 0; fluid < left.fluidOutputs().size(); fluid++) {
                assertEquals(
                        left.fluidOutputs().get(fluid).getFluid(),
                        right.fluidOutputs().get(fluid).getFluid());
                assertEquals(
                        left.fluidOutputs().get(fluid).getAmount(),
                        right.fluidOutputs().get(fluid).getAmount());
            }
        }
    }

    private static void assertSameIngredient(Ingredient expected, Ingredient actual) {
        ItemStack[] left = expected.getItems();
        ItemStack[] right = actual.getItems();
        assertEquals(left.length, right.length);
        for (int index = 0; index < left.length; index++) {
            assertEquals(left[index].getItem(), right[index].getItem());
            assertEquals(left[index].getCount(), right[index].getCount());
        }
    }

    private static RegistryFriendlyByteBuf frame(RegistryFriendlyByteBuf payload) {
        RegistryFriendlyByteBuf framed = buffer();
        framed.writeByte(CompactRecipeWireLimits.WIRE_VERSION);
        framed.writeVarInt(payload.readableBytes());
        framed.writeBytes(payload);
        return framed;
    }

    private static RegistryFriendlyByteBuf buffer() {
        return new RegistryFriendlyByteBuf(
                Unpooled.buffer(), registries, ConnectionType.NEOFORGE);
    }

    private static List<Path> listCompactFamilyFiles() throws IOException {
        Path root = Path.of(System.getProperty("user.dir"))
                .resolve("src/recipe_generated/resources/data/cruciblecraft/recipe");
        if (root == null || !Files.isDirectory(root)) {
            fail("missing generated compact recipe root");
        }
        List<Path> files = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(path -> path.getFileName().toString().endsWith(".json"))
                    .filter(Files::isRegularFile)
                    .sorted()
                    .forEach(path -> {
                        JsonObject document = readJson(path);
                        if ("cruciblecraft:compact_gt_recipe_family".equals(
                                document.has("type")
                                        ? document.get("type").getAsString()
                                        : "")) {
                            files.add(path);
                        }
                    });
        }
        return files;
    }

    private static boolean isOffTreeBathWave(Path path) {
        String normalized = path.toString().replace('\\', '/');
        return normalized.contains("/recipe/bath/");
    }

    private static Path compactFileNamed(String fileName) throws IOException {
        for (Path path : listCompactFamilyFiles()) {
            if (path.getFileName().toString().equals(fileName)) {
                return path;
            }
        }
        throw new IOException("missing compact family file " + fileName);
    }

    private static JsonObject readJson(Path path) {
        try {
            return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        } catch (IOException exception) {
            throw new IllegalStateException(path.toString(), exception);
        }
    }

    private static MaterialRule loadGeneratedMaterialRule(String path) throws Exception {
        var stream = CompactGTRecipeFamilySyncSizeTest.class.getClassLoader()
                .getResourceAsStream(path);
        if (stream == null) {
            throw new IOException("missing " + path);
        }
        try (stream; var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            var json = JsonParser.parseReader(reader).getAsJsonObject();
            json.remove("type");
            return MaterialRule.CODEC.codec()
                    .parse(JsonOps.INSTANCE, json)
                    .getOrThrow();
        }
    }
}
