package com.masson.cruciblecraft.gametest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import com.masson.cruciblecraft.content.block.BedrockOreBlock;
import com.masson.cruciblecraft.content.block.GtSmallOreBlock;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModFeatures;
import com.masson.cruciblecraft.worldgen.SmallOreCatalog;
import com.masson.cruciblecraft.worldgen.SmallOreFeature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated GT6 WorldgenOresSmall + WorldgenColtan. Run with
 * {@code -PgameTestGrid=worldgen}.
 */
@GameTestHolder(GtSmallOresGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class GtSmallOresGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_worldgen";
    private static final String TEMPLATE = "empty";
    private static final BlockPos POS = new BlockPos(8, 2, 8);

    private GtSmallOresGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void smallOreCatalogMatchesLoaderWorldgen(GameTestHelper helper) {
        helper.assertTrue(
                SmallOreCatalog.OVERWORLD_COUNT == 37,
                "GEN_GT small-ore count drifted");
        helper.assertTrue(
                SmallOreCatalog.NETHER_COUNT == 19,
                "GEN_NETHER small-ore count drifted");
        helper.assertTrue(
                SmallOreCatalog.UNIQUE_NAME_COUNT == 41
                        && SmallOreCatalog.ENTRIES.size() == 41,
                "unique WorldgenOresSmall names drifted");
        helper.assertTrue(
                SmallOreCatalog.ENTRIES.stream()
                        .anyMatch(entry ->
                                "ore.small.rocksalt".equals(entry.gt6Name())
                                        && "sylvite".equals(entry.materialId())),
                "rocksalt must map to sylvite");
        helper.assertTrue(
                SmallOreCatalog.ENTRIES.stream()
                        .noneMatch(entry ->
                                entry.gt6Name().contains("nikolite")
                                        || entry.gt6Name().contains("ancientdebris")
                                        || entry.gt6Name().contains("custom")),
                "mod-gated / hidden / custom small ores must stay out");
        helper.assertTrue(
                SmallOreCatalog.COLTAN.amount() == 32
                        && SmallOreCatalog.COLTAN.range() == 480
                        && SmallOreCatalog.COLTAN.seedOffset() == 5,
                "WorldgenColtan constants drifted");
        helper.assertTrue(
                ModFeatures.SMALL_ORES.get() != null
                        && ModBlocks.GT_SMALL_ORE.get() != null,
                "small_ores registry missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void smallOrePlacesGtSmallOreBlock(GameTestHelper helper) {
        helper.setBlock(POS, Blocks.STONE);
        helper.assertTrue(
                SmallOreFeature.tryPlace(
                        helper.getLevel(), helper.absolutePos(POS), "copper"),
                "small copper must replace stone");
        helper.assertTrue(
                helper.getBlockState(POS).getBlock() instanceof GtSmallOreBlock,
                "must place gt_small_ore, must not reuse T20 large-vein ellipsoids");
        helper.assertTrue(
                BedrockOreBlock.materialAt(
                                helper.getLevel(), helper.absolutePos(POS))
                        .orElse("")
                        .equals("copper"),
                "small ore material NBT must be copper");
        helper.setBlock(POS, Blocks.AIR);
        helper.assertTrue(
                !SmallOreFeature.tryPlace(
                        helper.getLevel(), helper.absolutePos(POS), "copper"),
                "small ores must not place in air");
        helper.assertTrue(
                helper.getBlockState(POS).is(Blocks.AIR),
                "air must stay air");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void smallOreCountFollowsGt6Amount(GameTestHelper helper) {
        Random random = new Random(0L);
        int amount = 16;
        int expected = Math.max(1, amount / 2 + random.nextInt(1 + amount) / 2);
        helper.assertTrue(
                SmallOreFeature.placeCount(16, new Random(0L)) == expected
                        && expected >= 1,
                "j = max(1, amount/2 + random.nextInt(1+amount)/2)");
        helper.assertTrue(
                SmallOreFeature.placeCount(1, new Random(1L)) >= 1,
                "amount 1 still places at least one attempt");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void coltanHotspotUsesSeedPlusFive(GameTestHelper helper) {
        Random seeded = new Random(1L + 5L);
        int x = (int) (seeded.nextGaussian() * 1500);
        int z = (int) (seeded.nextGaussian() * 1500);
        helper.assertTrue(
                SmallOreFeature.coltanCenter(1L).getX() == x
                        && SmallOreFeature.coltanCenter(1L).getZ() == z,
                "WorldgenColtan center is new Random(seed+5) gaussian*1500");
        helper.assertTrue(
                SmallOreCatalog.COLTAN.pickMaterial(0).equals("columbite")
                        && SmallOreCatalog.COLTAN.pickMaterial(1).equals("tantalite")
                        && SmallOreCatalog.COLTAN.pickMaterial(2).equals("coltan")
                        && SmallOreCatalog.COLTAN.pickMaterial(4).equals("coltan"),
                "coltan switch is 3:1:1 default/columbite/tantalite");
        helper.setBlock(POS, Blocks.STONE);
        helper.assertTrue(
                SmallOreFeature.tryPlace(
                        helper.getLevel(), helper.absolutePos(POS), "coltan"),
                "coltan small ore must place");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void remainingOverworldLargeVeinsAreRemoved(GameTestHelper helper) {
        String remaining = resource(
                "/data/cruciblecraft/neoforge/biome_modifier/"
                        + "remove_remaining_overworld_large_veins.json");
        helper.assertTrue(
                remaining.contains("neoforge:remove_features")
                        && remaining.contains("cruciblecraft:large_coal_vein")
                        && remaining.contains("cruciblecraft:large_amber_vein")
                        && remaining.contains("underground_ores")
                        && remaining.contains("#minecraft:is_overworld"),
                "GENERATE_STONE must remove remaining catalog large veins");
        String five = resource(
                "/data/cruciblecraft/neoforge/biome_modifier/"
                        + "remove_overworld_large_veins.json");
        helper.assertTrue(
                five.contains("cruciblecraft:large_iron_vein")
                        && five.contains("cruciblecraft:large_copper_vein"),
                "closed stone-layer 5-vein list must stay");
        String add = resource(
                "/data/cruciblecraft/neoforge/biome_modifier/add_small_ores.json");
        helper.assertTrue(
                add.contains("#minecraft:is_overworld")
                        && add.contains("cruciblecraft:small_ores")
                        && add.contains("fluid_springs"),
                "independent small ores must hang after stone layers");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void smallOreDoesNotDumpCatalog(GameTestHelper helper) {
        helper.assertTrue(
                BuiltInRegistries.FEATURE.getKey(ModFeatures.SMALL_ORES.get())
                        .toString()
                        .equals("cruciblecraft:small_ores"),
                "feature id drifted");
        helper.assertTrue(
                !BuiltInRegistries.FEATURE.containsKey(
                        net.minecraft.resources.ResourceLocation.parse(
                                "cruciblecraft:gt_item_scatter")),
                "retired catalog scatter must stay unregistered");
        helper.assertTrue(
                BuiltInRegistries.FEATURE.containsKey(
                        net.minecraft.resources.ResourceLocation.parse(
                                "cruciblecraft:large_vein")),
                "T20 LargeVeinFeature type stays registered but overworld veins are removed");
        helper.succeed();
    }

    private static String resource(String path) {
        try (InputStream stream =
                GtSmallOresGameTests.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("missing " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(path, failure);
        }
    }
}
