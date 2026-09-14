package com.masson.cruciblecraft.recipe.gt;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.registry.ModRecipeMaps;
import com.masson.cruciblecraft.test.MinecraftTestBootstrap;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.network.connection.ConnectionType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompactTransportFragmentsTest {
    private static final String FAMILY_ID = "gt.recipe.sharpener#0000";
    private static final String SOURCE_REVISION =
            "3703e40308c8c030763fd6297dea8b210d2a77b1";
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
    void unorderedFragmentsReassembleStableIdsAndDigest() {
        List<CompactGTRecipeFamilyDefinition.Relation> all = List.of(
                itemRelation("sanding/a", 0),
                itemRelation("sanding/b", 1),
                itemRelation("sanding/c", 2));
        CompactRecipeFamilySource first = fragment(0, 2, all.subList(0, 2), all);
        CompactRecipeFamilySource second = fragment(1, 2, all.subList(2, 3), all);
        List<CompactRecipeFamilySource> assembled = CompactTransportFragments.reassemble(
                List.of(second, first));
        assertEquals(1, assembled.size());
        assertEquals(
                id("sanding/sanding/gt_recipe_sharpener_0000"),
                assembled.getFirst().id());
        assertEquals(FAMILY_ID, assembled.getFirst().definition().familyId());
        assertTrue(assembled.getFirst().definition().transportFragment().isEmpty());
        List<CompactGTRecipeFamilyDefinition.Relation> relations =
                assembled.getFirst().authoredRelations();
        assertEquals(3, relations.size());
        assertEquals(id("sanding/a"), relations.get(0).stableId());
        assertEquals(id("sanding/b"), relations.get(1).stableId());
        assertEquals(id("sanding/c"), relations.get(2).stableId());
    }

    @Test
    void missingFragmentFailsClosed() {
        List<CompactGTRecipeFamilyDefinition.Relation> all = List.of(
                itemRelation("sanding/a", 0),
                itemRelation("sanding/b", 1));
        CompactRecipeFamilySource first = fragment(0, 2, all.subList(0, 1), all);
        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> CompactTransportFragments.reassemble(List.of(first)));
        assertTrue(thrown.getMessage().contains("Missing transport fragment"));
    }

    @Test
    void duplicateIndexFailsClosed() {
        List<CompactGTRecipeFamilyDefinition.Relation> all = List.of(
                itemRelation("sanding/a", 0),
                itemRelation("sanding/b", 1));
        CompactRecipeFamilySource first = fragment(0, 2, all.subList(0, 1), all);
        CompactRecipeFamilySource duplicate = fragment(0, 2, all.subList(1, 2), all);
        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> CompactTransportFragments.reassemble(List.of(first, duplicate)));
        assertTrue(thrown.getMessage().contains("Duplicate transport fragment"));
    }

    @Test
    void digestDriftFailsClosed() {
        List<CompactGTRecipeFamilyDefinition.Relation> all = List.of(
                itemRelation("sanding/a", 0),
                itemRelation("sanding/b", 1));
        CompactRecipeFamilySource first = fragment(0, 2, all.subList(0, 1), all);
        CompactGTRecipeFamilyDefinition.TransportFragment drifted =
                new CompactGTRecipeFamilyDefinition.TransportFragment(
                        1,
                        2,
                        2,
                        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        CompactRecipeFamilySource second = new CompactRecipeFamilySource(
                id("sanding/sanding/gt_recipe_sharpener_0000_fragment_0001"),
                new CompactGTRecipeFamilyDefinition(
                        FAMILY_ID,
                        ModRecipeMaps.ASSEMBLER.id(),
                        SOURCE_REVISION,
                        List.of(all.get(1)),
                        Optional.empty(),
                        Optional.of(id("sanding/pilot/sanding")),
                        Optional.empty(),
                        Optional.of(drifted)));
        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> CompactTransportFragments.reassemble(List.of(first, second)));
        assertTrue(thrown.getMessage().contains("headers drifted"));
    }

    @Test
    void mixedFragmentAndWholeHolderFailsClosed() {
        List<CompactGTRecipeFamilyDefinition.Relation> all = List.of(
                itemRelation("sanding/a", 0),
                itemRelation("sanding/b", 1));
        CompactRecipeFamilySource fragment = fragment(0, 2, all.subList(0, 1), all);
        CompactRecipeFamilySource whole = new CompactRecipeFamilySource(
                id("sanding/sanding/gt_recipe_sharpener_0000"),
                new CompactGTRecipeFamilyDefinition(
                        FAMILY_ID,
                        ModRecipeMaps.ASSEMBLER.id(),
                        SOURCE_REVISION,
                        List.of(all.get(1)),
                        Optional.empty(),
                        Optional.of(id("sanding/pilot/sanding"))));
        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> CompactTransportFragments.reassemble(List.of(fragment, whole)));
        assertTrue(thrown.getMessage().contains("mixes transport fragments"));
    }

    @Test
    void reassembledRelationCountDriftFailsClosed() {
        List<CompactGTRecipeFamilyDefinition.Relation> all = List.of(
                itemRelation("sanding/a", 0),
                itemRelation("sanding/b", 1));
        String digest = CompactTransportFragments.semanticDigest(
                FAMILY_ID,
                ModRecipeMaps.ASSEMBLER.id(),
                SOURCE_REVISION,
                Optional.of(id("sanding/pilot/sanding")),
                all);
        CompactRecipeFamilySource first = fragmentWithEnvelope(
                0,
                2,
                3,
                digest,
                all.subList(0, 1));
        CompactRecipeFamilySource second = fragmentWithEnvelope(
                1,
                2,
                3,
                digest,
                all.subList(1, 2));
        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> CompactTransportFragments.reassemble(List.of(first, second)));
        assertTrue(thrown.getMessage().contains("reassembled relation count"));
    }

    @Test
    void semanticDigestDriftFailsClosed() {
        List<CompactGTRecipeFamilyDefinition.Relation> all = List.of(
                itemRelation("sanding/a", 0),
                itemRelation("sanding/b", 1));
        CompactRecipeFamilySource first = fragment(0, 2, all.subList(0, 1), all);
        CompactRecipeFamilySource second = fragment(
                1,
                2,
                List.of(itemRelation("sanding/c", 1)),
                all);
        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> CompactTransportFragments.reassemble(List.of(first, second)));
        assertTrue(thrown.getMessage().contains("semantic digest drifted"));
    }

    @Test
    void fragmentRoundTripsOnDedicatedClientWire() {
        List<CompactGTRecipeFamilyDefinition.Relation> all = List.of(
                itemRelation("sanding/a", 0),
                itemRelation("sanding/b", 1));
        CompactRecipeFamilySource source = fragment(0, 2, all.subList(0, 1), all);
        CompactGTRecipeFamilyEntry original =
                new CompactGTRecipeFamilyEntry(source.definition());
        RegistryFriendlyByteBuf buffer = buffer();
        serializer.streamCodec().encode(buffer, original);
        assertTrue(
                buffer.writerIndex()
                        <= CompactRecipeWireLimits.MAX_RECIPE_ENTRY_WIRE_BYTES);
        CompactGTRecipeFamilyEntry decoded = serializer.streamCodec().decode(buffer);
        assertEquals(0, buffer.readableBytes());
        assertEquals(
                original.definition().transportFragment(),
                decoded.definition().transportFragment());
        assertEquals(
                original.definition().familyId(),
                decoded.definition().familyId());
        assertEquals(
                original.definition().authoredRelations().getFirst().stableId(),
                decoded.definition().authoredRelations().getFirst().stableId());
    }

    @Test
    void decodeRejectsOversizeFragmentCount() {
        RegistryFriendlyByteBuf payload = buffer();
        payload.writeUtf("gt.recipe.test#0001", CompactRecipeWireLimits.MAX_FAMILY_ID_LENGTH);
        ResourceLocation.STREAM_CODEC.encode(
                payload, ResourceLocation.fromNamespaceAndPath("cruciblecraft", "bath"));
        payload.writeUtf("rev", CompactRecipeWireLimits.MAX_SOURCE_REVISION_LENGTH);
        payload.writeBoolean(false);
        payload.writeBoolean(true);
        payload.writeVarInt(0);
        payload.writeVarInt(CompactRecipeWireLimits.MAX_TRANSPORT_FRAGMENTS + 1);
        payload.writeVarInt(8);
        payload.writeUtf("a".repeat(64), CompactRecipeWireLimits.MAX_SEMANTIC_DIGEST_LENGTH);
        RegistryFriendlyByteBuf framed = frame(payload);
        DecoderException thrown = assertThrows(
                DecoderException.class,
                () -> serializer.streamCodec().decode(framed));
        assertTrue(thrown.getMessage().contains("transport fragments"));
    }

    private static CompactRecipeFamilySource fragment(
            int index,
            int count,
            List<CompactGTRecipeFamilyDefinition.Relation> slice,
            List<CompactGTRecipeFamilyDefinition.Relation> all) {
        Optional<ResourceLocation> group = Optional.of(id("sanding/pilot/sanding"));
        String digest = CompactTransportFragments.semanticDigest(
                FAMILY_ID,
                ModRecipeMaps.ASSEMBLER.id(),
                SOURCE_REVISION,
                group,
                all);
        return new CompactRecipeFamilySource(
                id("sanding/sanding/gt_recipe_sharpener_0000_fragment_%04d"
                        .formatted(index)),
                new CompactGTRecipeFamilyDefinition(
                        FAMILY_ID,
                        ModRecipeMaps.ASSEMBLER.id(),
                        SOURCE_REVISION,
                        slice,
                        Optional.empty(),
                        group,
                        Optional.empty(),
                        Optional.of(new CompactGTRecipeFamilyDefinition.TransportFragment(
                                index, count, all.size(), digest))));
    }

    private static CompactRecipeFamilySource fragmentWithEnvelope(
            int index,
            int count,
            int totalRelations,
            String digest,
            List<CompactGTRecipeFamilyDefinition.Relation> slice) {
        Optional<ResourceLocation> group = Optional.of(id("sanding/pilot/sanding"));
        return new CompactRecipeFamilySource(
                id("sanding/sanding/gt_recipe_sharpener_0000_fragment_%04d"
                        .formatted(index)),
                new CompactGTRecipeFamilyDefinition(
                        FAMILY_ID,
                        ModRecipeMaps.ASSEMBLER.id(),
                        SOURCE_REVISION,
                        slice,
                        Optional.empty(),
                        group,
                        Optional.empty(),
                        Optional.of(new CompactGTRecipeFamilyDefinition.TransportFragment(
                                index, count, totalRelations, digest))));
    }

    private static CompactGTRecipeFamilyDefinition.Relation itemRelation(
            String path, int shadowOrder) {
        return new CompactGTRecipeFamilyDefinition.Relation(
                id(path),
                List.of(Ingredient.of(Items.IRON_INGOT)),
                List.of(1),
                List.of(ItemInputAction.CONSUME),
                List.of(new ItemStack(Items.IRON_NUGGET)),
                List.of(),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                32,
                16L,
                0L,
                true,
                shadowOrder,
                new GTRecipeProvenance("SOURCE_DERIVED", Optional.of(FAMILY_ID)));
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

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
