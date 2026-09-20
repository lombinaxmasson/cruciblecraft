package com.masson.cruciblecraft.gametest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.CableBlock;
import com.masson.cruciblecraft.content.block.FluidPipeBlock;
import com.masson.cruciblecraft.content.item.CableBlockItem;
import com.masson.cruciblecraft.content.item.PipeBlockItem;
import com.masson.cruciblecraft.logistics.pipe.PipeCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated GT6 connector art. Run with
 * {@code -PwaveRecipes=content/gt6-connector-art}.
 */
@GameTestHolder(ConnectorArtGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class ConnectorArtGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_content_gt6_connector_art";
    private static final String TEMPLATE = "empty";

    private ConnectorArtGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void connectorArtManifestResolvesLocalGt6(
            GameTestHelper helper) {
        String fluid = resource(
                "/assets/cruciblecraft/gt6_fluid_pipe_art_manifest.json");
        String item = resource(
                "/assets/cruciblecraft/gt6_item_pipe_art_manifest.json");
        String eu = resource(
                "/assets/cruciblecraft/gt6_eu_wire_cable_art_manifest.json");
        helper.assertTrue(
                fluid.contains("materialicons/copper/pipetiny")
                        && item.contains("iconsets/pipe_restrictor")
                        && eu.contains("iconsets/insulation_tiny")
                        && !eu.contains("cable.png\""),
                "connector art manifests drifted");
        helper.assertTrue(
                classpathExists(
                        "/assets/cruciblecraft/textures/block/gt6_import/"
                                + "materialicons/copper/pipetiny.png")
                        && classpathExists(
                                "/assets/cruciblecraft/textures/block/gt6_import/"
                                        + "iconsets/insulation_tiny.png")
                        && classpathExists(
                                "/assets/cruciblecraft/textures/block/gt6_import/"
                                        + "materialicons/copper/wire.png"),
                "GT6 connector iconset was not imported");
        helper.assertTrue(
                ModBlocks.pipeBlock(
                                "copper",
                                MaterialPrefixes.TINY_FLUID_PIPE,
                                PipeCatalog.Kind.FLUID)
                        .get() instanceof FluidPipeBlock
                        && ModItems.materialItem(
                                        "copper",
                                        MaterialPrefixes.TINY_FLUID_PIPE)
                                .get() instanceof PipeBlockItem,
                "live tiny fluid pipe lost its BlockItem");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void foldedRowsCopyZeroPng(GameTestHelper helper) {
        Item tinWire = ModItems.materialItem(
                "tin", MaterialPrefixes.WIRE).get();
        helper.assertTrue(
                tinWire instanceof CableBlockItem
                        && ModBlocks.electricalConductorBlock(
                                        "tin", MaterialPrefixes.WIRE)
                                .get() instanceof CableBlock,
                "folded tin wireGt01 is not the live CableBlockItem");
        ResourceLocation dummy = ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft", "electric_wire/1x_tin_wire");
        helper.assertTrue(
                !BuiltInRegistries.ITEM.containsKey(dummy),
                "folded tin wireGt01 dummy was kept as a second art host");
        String eu = resource(
                "/assets/cruciblecraft/gt6_eu_wire_cable_art_manifest.json");
        helper.assertTrue(
                eu.contains("\"copied\": false")
                        && eu.contains("materialicons/copper/wire.png"),
                "EU recopy of the shared copper wire iconset");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void inPlaceRowsHaveNoIronIngotModel(
            GameTestHelper helper) {
        ResourceLocation dummy = ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft", "electric_wire/2x_blue_alloy_wire");
        helper.assertTrue(
                !BuiltInRegistries.ITEM.containsKey(dummy)
                        && ModItems.materialItem(
                                        "blue_alloy", MaterialPrefixes.DOUBLE_WIRE)
                                .get() instanceof CableBlockItem
                        && ModBlocks.electricalConductorBlock(
                                        "blue_alloy", MaterialPrefixes.DOUBLE_WIRE)
                                .get() instanceof CableBlock,
                "live-host 2x blue alloy dummy was kept beside its CableBlock");
        helper.assertTrue(
                ConnectorArtGameTests.class.getResource(
                                "/assets/cruciblecraft/models/item/electric_wire/"
                                        + "2x_blue_alloy_wire.json")
                        == null,
                "folded 2x blue alloy dummy model was kept");
        helper.succeed();
    }

    private static boolean classpathExists(String path) {
        return ConnectorArtGameTests.class.getResource(path) != null;
    }

    private static String resource(String path) {
        try (InputStream stream =
                ConnectorArtGameTests.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("missing " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(path, failure);
        }
    }
}
