package com.masson.cruciblecraft.gametest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.masson.cruciblecraft.content.item.CatalogNamedBlockItem;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated GT6 foundry art. Run with
 * {@code -PwaveRecipes=content/gt6-foundry-art}.
 */
@GameTestHolder(FoundryArtGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class FoundryArtGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_content_gt6_foundry_art";
    private static final String TEMPLATE = "empty";

    private FoundryArtGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void foundryArtManifestResolvesLocalGt6(GameTestHelper helper) {
        String manifest = resource(
                "/assets/cruciblecraft/gt6_foundry_art_manifest.json");
        helper.assertTrue(
                manifest.contains("materialicons/STONE/blocksolid.png")
                        && manifest.contains("materialicons/METALLIC/blocksolid.png")
                        && manifest.contains("\"copied\": false")
                        && !manifest.contains("multiblockmains/crucible"),
                "foundry art manifest drifted from local GT6 iconsets");
        helper.assertTrue(
                classpathExists(
                        "/assets/cruciblecraft/textures/block/gt6_import/"
                                + "materialicons/stone/blocksolid.png")
                        && classpathExists(
                                "/assets/cruciblecraft/textures/block/gt6_import/"
                                        + "mte/faucet.png"),
                "GT6 foundry iconset was not imported");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void foundryArtNotLargeCrucibleCube(GameTestHelper helper) {
        MteInPlaceGameTestSupport.assertLive(
                helper, "foundry/smelting_crucible_invar", MteInPlaceKind.CRUCIBLE_FOUNDRY);
        helper.assertTrue(
                ((CatalogNamedBlockItem) MteInPlaceGameTestSupport.item(
                        "foundry/smelting_crucible_invar")).getBlock()
                        != ModBlocks.CRUCIBLE.get(),
                "foundry/smelting_crucible_invar aliased the ceramic crucible");
        String smeltery = resource(
                "/assets/cruciblecraft/models/block/foundry/smelting_crucible_invar.json");
        String mold = resource(
                "/assets/cruciblecraft/models/block/foundry/mold_stone.json");
        helper.assertTrue(
                smeltery.contains("mte_foundry_smeltery")
                        && !smeltery.contains("cube_all")
                        && !smeltery.contains("crucible_foundry")
                        && mold.contains("mte_foundry_mold")
                        && !mold.contains("cube_all"),
                "foundry models still use the large-crucible cube");
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(
                pos,
                ModBlocks.mteInPlaceBlocksById()
                        .get(MteInPlaceGameTestSupport.id(
                                "foundry/smelting_crucible_invar"))
                        .get()
                        .defaultBlockState());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void foundryArtSharedParentsNotPerMaterialPng(
            GameTestHelper helper) {
        String steel = resource(
                "/assets/cruciblecraft/models/block/foundry/mold_steel.json");
        String stone = resource(
                "/assets/cruciblecraft/models/block/foundry/mold_stone.json");
        helper.assertTrue(
                steel.contains("mte_foundry_mold")
                        && stone.contains("mte_foundry_mold")
                        && steel.contains("gt6_import/mte/faucet")
                        && stone.contains("materialicons/stone/blocksolid")
                        && !classpathExists(
                                "/assets/cruciblecraft/textures/block/"
                                        + "foundry/mold_steel.png"),
                "foundry molds are not sharing the GT6 mold parent");
        helper.succeed();
    }

    private static boolean classpathExists(String path) {
        return FoundryArtGameTests.class.getResource(path) != null;
    }

    private static String resource(String path) {
        try (InputStream stream =
                FoundryArtGameTests.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("missing " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(path, failure);
        }
    }
}
