package com.masson.cruciblecraft.recipe.rule;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonParser;
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
        String path = "data/cruciblecraft/recipe/extruder/long_rod/iron.json";
        var stream = MaterialRuleCodecTest.class.getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, path);
        try (stream; var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            var json = JsonParser.parseReader(reader).getAsJsonObject();
            assertEquals("cruciblecraft:material_rule", json.remove("type").getAsString());
            MaterialRule decoded = MaterialRule.CODEC.codec()
                    .parse(JsonOps.INSTANCE, json).getOrThrow();
            assertEquals(Optional.of(id("extruder")), decoded.target());
            assertEquals(2, decoded.itemInputs().size());
            assertEquals("0", decoded.itemInputs().get(1).count());
            assertEquals(
                    Optional.of(id("extruder_shape_long_rod")),
                    decoded.itemInputs().get(1).item());
        }
    }

    @Test
    void codecAndNetworkRoundTripActualRule() {
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

    private static MaterialRule sampleRule() {
        return new MaterialRule(
                Optional.of(id("crusher")),
                List.of(
                        prefix("raw_ore", "2", "10000"),
                        new MaterialRule.ItemResource(
                                Optional.empty(),
                                Optional.of(ResourceLocation.withDefaultNamespace("flint")),
                                "1",
                                "10000")),
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
                List.of("has_prefix(ingot)"),
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
}
