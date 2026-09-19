package com.masson.cruciblecraft.gametest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.masson.cruciblecraft.content.blockentity.AnvilBlockEntity;
import com.masson.cruciblecraft.content.item.CatalogNamedBlockItem;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(MteMiscToolRuntimeGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class MteMiscToolRuntimeGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_content_gt6_mte_misc_tool_runtime";
    private static final String TEMPLATE = "empty";

    private MteMiscToolRuntimeGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void gt6AnvilIsNotVanillaAnvil(GameTestHelper helper) {
        MteInPlaceGameTestSupport.assertLive(
                helper, "stone/anvil", MteInPlaceKind.MISC_TOOL);
        helper.assertTrue(
                MteInPlaceGameTestSupport.item(
                        "stone/anvil") != Items.ANVIL
                        && ((CatalogNamedBlockItem) MteInPlaceGameTestSupport.item(
                                "stone/anvil")).getBlock()
                                != ModBlocks.ANVIL.get()
                        && ((CatalogNamedBlockItem) MteInPlaceGameTestSupport.item(
                                "stone/anvil")).getBlock()
                                != Blocks.ANVIL,
                "stone/anvil aliased an anvil");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void gt6AnvilUsesGt6VoxelsNotCube(GameTestHelper helper) {
        String stone = resource("/assets/cruciblecraft/models/block/stone/anvil.json");
        String bronze = resource("/assets/cruciblecraft/models/block/bronze/anvil.json");
        String ironwood = resource("/assets/cruciblecraft/models/block/ironwood/anvil.json");
        String live = resource("/assets/cruciblecraft/models/block/anvil.json");
        helper.assertTrue(
                stone.contains("anvil_gt6")
                        && stone.contains("materialicons/stone/blocksolid")
                        && bronze.contains("mte/faucet")
                        && ironwood.contains("materialicons/wood/blocksolid")
                        && live.contains("anvil_gt6")
                        && !live.contains("rough_block_solid")
                        && !stone.contains("mte_inplace_misc_tool")
                        && !bronze.contains("cube_all"),
                "unique anvils are still misc-tool cubes");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void uniqueAnvilHostsAnvilBlockEntity(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(
                pos,
                ModBlocks.mteInPlaceBlocksById()
                        .get(MteInPlaceGameTestSupport.id("stone/anvil"))
                        .get()
                        .defaultBlockState());
        helper.assertTrue(
                helper.getBlockEntity(pos) instanceof AnvilBlockEntity anvil
                        && "stone".equals(anvil.materialId())
                        && anvil.maxDurability() == 10_000L,
                "stone/anvil did not host AnvilBlockEntity");
        helper.succeed();
    }

    private static String resource(String path) {
        try (InputStream stream =
                MteMiscToolRuntimeGameTests.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("missing " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(path, failure);
        }
    }
}
