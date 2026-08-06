package com.masson.cruciblecraft.recipe.rule;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonParser;
import com.masson.cruciblecraft.recipe.gt.ItemInputAction;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.neoforged.fml.loading.LoadingModList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaterialRuleCodecTest {
    @Test
    void generatedComponentRuleUsesTheRuntimeRecipeResourcePath() throws Exception {
        String path = "data/cruciblecraft/recipe/extruder/compact/normal_long_rod.json";
        var stream = MaterialRuleCodecTest.class.getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, path);
        try (stream; var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            var json = JsonParser.parseReader(reader).getAsJsonObject();
            assertEquals("cruciblecraft:material_rule", json.remove("type").getAsString());
            MaterialRule decoded = MaterialRule.CODEC.codec()
                    .parse(JsonOps.INSTANCE, json).getOrThrow();
            assertEquals(Optional.of(id("extruder")), decoded.target());
            assertTrue(decoded.itemInputs().isEmpty());
            MaterialRule.SparseTable sparse = decoded.sparse().orElseThrow();
            assertEquals(
                    Optional.of(id("extruder_shape_long_rod")),
                    Optional.of(sparse.shapeItem()));
            MaterialRule.SparseRelation iron = sparse.relations().stream()
                    .filter(relation -> relation.material().equals("iron"))
                    .findFirst()
                    .orElseThrow();
            assertEquals(id("extruder/long_rod/iron/iron"), iron.stableId());
            assertEquals(iron, sparse.findMatch(
                    "iron", iron.input().prefix(), iron.input().count()).orElseThrow());
        }
    }

    @Test
    void generatedCableRuleUsesSourceBackedAnyRubberTag()
            throws Exception {
        String path = "data/cruciblecraft/recipe/assembler/"
                + "wire_and_rubber_to_cable.json";
        var stream = MaterialRuleCodecTest.class.getClassLoader()
                .getResourceAsStream(path);
        assertNotNull(stream, path);
        try (stream;
                var reader = new InputStreamReader(
                        stream, StandardCharsets.UTF_8)) {
            var json = JsonParser.parseReader(reader).getAsJsonObject();
            json.remove("type");
            MaterialRule decoded = MaterialRule.CODEC.codec()
                    .parse(JsonOps.INSTANCE, json)
                    .getOrThrow();
            assertEquals(
                    Optional.of(id("any_rubber_plates")),
                    decoded.itemInputs().get(1).tag());
            assertTrue(decoded.itemInputs().get(1).item().isEmpty());
            assertTrue(
                    decoded.itemInputs().get(1)
                            .materialSelector().isEmpty());
        }
        String tagPath =
                "data/cruciblecraft/tags/item/any_rubber_plates.json";
        var tagStream = MaterialRuleCodecTest.class.getClassLoader()
                .getResourceAsStream(tagPath);
        assertNotNull(tagStream, tagPath);
        try (tagStream;
                var reader = new InputStreamReader(
                        tagStream, StandardCharsets.UTF_8)) {
            var values = JsonParser.parseReader(reader)
                    .getAsJsonObject()
                    .getAsJsonArray("values");
            assertEquals("#c:plates/rubber", values.get(0).getAsString());
        }
    }

    @Test
    void codecAndNetworkRoundTripActualRule() throws Exception {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        MaterialRuleRecipe original = new MaterialRuleRecipe(sampleRule());
        MaterialRuleSerializer serializer = new MaterialRuleSerializer();

        var json = serializer.codec().codec()
                .encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow();
        MaterialRuleRecipe decoded = serializer.codec().codec()
                .parse(JsonOps.INSTANCE, json)
                .getOrThrow();
        assertEquals(original, decoded);

        RegistryFriendlyByteBuf buffer =
                new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        serializer.streamCodec().encode(buffer, original);
        assertEquals(original, serializer.streamCodec().decode(buffer));
        assertEquals(0, buffer.readableBytes());
        MaterialRule.ItemResource catalyst =
                decoded.rule().itemInputs().get(1);
        assertEquals(
                Map.of(id("tool_material"), "iron"),
                catalyst.stringComponents());
        assertEquals(
                Optional.of(ItemInputAction.wear(1)),
                catalyst.inputAction());

        MaterialRuleRecipe compact = new MaterialRuleRecipe(loadGeneratedRule(
                "data/cruciblecraft/recipe/extruder/compact/normal_plate.json"));
        RegistryFriendlyByteBuf compactBuffer =
                new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        serializer.streamCodec().encode(compactBuffer, compact);
        assertEquals(compact, serializer.streamCodec().decode(compactBuffer));
        assertEquals(0, compactBuffer.readableBytes());
        assertEquals(226, compact.rule().sparse().orElseThrow().relations().size());
    }

    @Test
    void fixedOnlyProcessingRuleRequiresGtRecipe() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> new MaterialRule(
                        Optional.of(id("crusher")),
                        List.of(new MaterialRule.ItemResource(
                                Optional.empty(),
                                Optional.of(ResourceLocation.withDefaultNamespace("stone")),
                                "1",
                                "10000")),
                        List.of(new MaterialRule.ItemResource(
                                Optional.empty(),
                                Optional.of(ResourceLocation.withDefaultNamespace("gravel")),
                                "1",
                                "10000")),
                        List.of(),
                        List.of(),
                        "1",
                        "1",
                        "0",
                        true,
                        Optional.empty(),
                        Map.of(),
                        List.of(),
                        Optional.empty(),
                        List.of()));
        assertTrue(error.getMessage().contains("gt_recipe"));
    }

    @Test
    void materialFluidSelectorIsExplicitAndCodecRoundTrips() {
        MaterialRule.FluidResource chemical = new MaterialRule.FluidResource(
                Optional.empty(),
                Optional.empty(),
                Optional.of("chemical"),
                "material.quality * 1000");
        var encoded = MaterialRule.FluidResource.CODEC.codec()
                .encodeStart(JsonOps.INSTANCE, chemical)
                .getOrThrow();
        assertEquals(
                chemical,
                MaterialRule.FluidResource.CODEC.codec()
                        .parse(JsonOps.INSTANCE, encoded)
                        .getOrThrow());
        assertThrows(
                IllegalArgumentException.class,
                () -> new MaterialRule.FluidResource(
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of("ambient"),
                        "1000"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new MaterialRule.FluidResource(
                        Optional.of("dust"),
                        Optional.empty(),
                        Optional.of("chemical"),
                        "1000"));
    }

    @Test
    void optionalOutputsRemainRestrictedToByproductSelectors() {
        MaterialRule template = sampleRule();
        assertTrue(template.itemOutputs().get(1).optional());
        MaterialRule.ItemResource optionalSelf = new MaterialRule.ItemResource(
                Optional.of("dust"),
                Optional.empty(),
                "1",
                "10000",
                Optional.empty(),
                true);
        assertThrows(IllegalArgumentException.class, () -> new MaterialRule(
                template.target(),
                template.itemInputs(),
                List.of(optionalSelf),
                template.fluidInputs(),
                template.fluidOutputs(),
                template.duration(),
                template.eut(),
                template.specialValue(),
                template.canBeBuffered(),
                template.material(),
                template.materialOverrides(),
                template.conditions(),
                template.tuning(),
                template.unification()));
    }

    private static MaterialRule sampleRule() {
        return new MaterialRule(
                Optional.of(id("crusher")),
                List.of(
                        prefix("raw_ore", "2", "10000"),
                        new MaterialRule.ItemResource(
                                Optional.empty(),
                                Optional.of(ResourceLocation.withDefaultNamespace("flint")),
                                "0",
                                "10000",
                                Optional.empty(),
                                false,
                                Map.of(id("tool_material"), "iron"),
                                Optional.of(ItemInputAction.wear(1)))),
                List.of(
                        prefix("crushed_ore", "2", "10000"),
                        new MaterialRule.ItemResource(
                                Optional.of("dust"),
                                Optional.empty(),
                                "1",
                                "5000",
                                Optional.of("byproduct:0"),
                                true)),
                List.of(),
                List.of(),
                "max(20, material.tier*10)",
                "tier_voltage(material.tier)",
                "3",
                false,
                Optional.of("iron"),
                Map.of("iron", new MaterialRule.MaterialOverride(
                        Optional.of("40"),
                        Optional.empty(),
                        Optional.empty(),
                        Map.of("1", "0"),
                        Map.of(),
                        Map.of("1", "2500"),
                        Map.of())),
                List.of("has_form(ingot)"),
                Optional.of(new MaterialRule.Tuning(
                        "iron",
                        Optional.of(3),
                        Optional.of("#ABCDEF"),
                        Optional.of(1500.0),
                        Optional.empty(),
                        Optional.of(8.0))),
                List.of(new MaterialRule.UnificationPreference(
                        "iron",
                        "ingot",
                        ResourceLocation.withDefaultNamespace("iron_ingot"),
                        10)));
    }

    private static MaterialRule.ItemResource prefix(
            String prefix,
            String count,
            String chance) {
        return new MaterialRule.ItemResource(
                Optional.of(prefix), Optional.empty(), count, chance);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }

    private static MaterialRule loadGeneratedRule(String path) throws Exception {
        var stream = MaterialRuleCodecTest.class.getClassLoader()
                .getResourceAsStream(path);
        assertNotNull(stream, path);
        try (stream;
                var reader = new InputStreamReader(
                        stream, StandardCharsets.UTF_8)) {
            var json = JsonParser.parseReader(reader).getAsJsonObject();
            assertEquals(
                    "cruciblecraft:material_rule",
                    json.remove("type").getAsString());
            return MaterialRule.CODEC.codec()
                    .parse(JsonOps.INSTANCE, json)
                    .getOrThrow();
        }
    }
}
