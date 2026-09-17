package com.masson.cruciblecraft.worldgen;

import java.util.Random;

import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.BedrockOreBlock;
import com.masson.cruciblecraft.content.block.GtHostedOreBlock;
import com.masson.cruciblecraft.content.block.GtIndicatorFlowerBlock;
import com.masson.cruciblecraft.content.block.GtSmallOreBlock;
import com.masson.cruciblecraft.content.block.OreStoneHost;
import com.masson.cruciblecraft.content.blockentity.GtSurfaceRockBlockEntity;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.worldgen.OreHostVariantCatalog.Host;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6 {@code WorldgenOresBedrock#generateVein} plus indicator 32757 pebbles
 * carrying {@code oreRaw} and FlowersA/B. Floor Y is {@code minBuildHeight}
 * (1.7.10 Y=0).
 */
public final class BedrockOreVeins {
    public static final int[] MUFFIN_MIN = {5, 4, 2, 1, 0, 2, 5};
    public static final int[] MUFFIN_MAX = {11, 12, 14, 15, 16, 14, 11};

    private BedrockOreVeins() {}

    /**
     * GT6 rolls every {@code WorldgenOresBedrock} independently. The first
     * success fills the muffin ({@code GENERATED_NO_BEDROCK_ORE}); later
     * successes still place floor/muffin/tendril ores. Returning after the
     * first hit would let a rare early row suppress chalcopyrite-tier veins.
     */
    public static boolean generate(WorldGenLevel level, BlockPos origin, Random random) {
        int minX = origin.getX() & ~15;
        int minZ = origin.getZ() & ~15;
        int floorY = level.getMinBuildHeight();
        boolean nether = level.getLevel().dimension() == Level.NETHER;
        if (!isBedrockFloor(level.getBlockState(new BlockPos(minX + 8, floorY, minZ + 8)))) {
            return false;
        }
        boolean generated = false;
        boolean fillMuffin = true;
        for (BedrockOreCatalog.Vein vein : BedrockOreCatalog.forNether(nether)) {
            if (random.nextInt(vein.probability()) != 0) {
                continue;
            }
            if (!generateVein(
                    level, minX, minZ, floorY, vein.materialId(), random, fillMuffin)) {
                continue;
            }
            placeIndicators(level, minX, minZ, vein, random);
            generated = true;
            fillMuffin = false;
        }
        return generated;
    }

    public static boolean generateVein(
            WorldGenLevel level,
            int minX,
            int minZ,
            int floorY,
            String materialId,
            Random random) {
        return generateVein(level, minX, minZ, floorY, materialId, random, true);
    }

    public static boolean generateVein(
            WorldGenLevel level,
            int minX,
            int minZ,
            int floorY,
            String materialId,
            Random random,
            boolean fillMuffin) {
        if (!isBedrockFloor(level.getBlockState(new BlockPos(minX + 8, floorY, minZ + 8)))) {
            return false;
        }
        for (int tX = 5; tX < 11; tX++) {
            for (int tZ = 5; tZ < 11; tZ++) {
                switch (random.nextInt(6)) {
                    case 0 -> placeBedrockOre(
                            level, minX + tX, floorY, minZ + tZ, materialId, false);
                    case 1, 2 -> placeBedrockOre(
                            level, minX + tX, floorY, minZ + tZ, materialId, true);
                    default -> {
                    }
                }
            }
        }
        placeBedrockOre(
                level,
                minX + 6 + random.nextInt(4),
                floorY,
                minZ + 6 + random.nextInt(4),
                materialId,
                false);
        boolean nether = level.getLevel().dimension() == Level.NETHER;
        BlockState fill = nether
                ? Blocks.NETHERRACK.defaultBlockState()
                : Blocks.DEEPSLATE.defaultBlockState();
        for (int tY = 1; tY < MUFFIN_MIN.length; tY++) {
            for (int tX = MUFFIN_MIN[tY]; tX < MUFFIN_MAX[tY]; tX++) {
                for (int tZ = MUFFIN_MIN[tY]; tZ < MUFFIN_MAX[tY]; tZ++) {
                    BlockPos pos = new BlockPos(minX + tX, floorY + tY, minZ + tZ);
                    if (fillMuffin) {
                        level.setBlock(pos, fill, Block.UPDATE_CLIENTS);
                    }
                    switch (random.nextInt(6)) {
                        case 0 -> placeNormalOre(level, pos, materialId);
                        case 1, 2 -> placeSmallOre(level, pos, materialId);
                        default -> {
                        }
                    }
                }
            }
        }
        int water = nether ? 31 : Math.max(1, level.getSeaLevel() - 1);
        for (int i = 5 + random.nextInt(3); i > 0; i--) {
            int tX = 5 + random.nextInt(6);
            int tZ = 5 + random.nextInt(6);
            for (int tY = MUFFIN_MIN.length; tY < water - floorY; tY++) {
                switch (random.nextInt(7)) {
                    case 0 -> tX++;
                    case 1 -> tX--;
                    case 2 -> tZ++;
                    case 3 -> tZ--;
                    default -> {
                    }
                }
                BlockPos pos = new BlockPos(minX + tX, floorY + tY, minZ + tZ);
                if (tX <= 0 || tX >= 15 || tZ <= 0 || tZ >= 15) {
                    placeSmallOre(level, pos, materialId);
                    break;
                }
                if (random.nextInt(3) != 0) {
                    placeSmallOre(level, pos, materialId);
                }
            }
        }
        return true;
    }

    public static void placeIndicators(
            WorldGenLevel level,
            int minX,
            int minZ,
            BedrockOreCatalog.Vein vein,
            Random random) {
        boolean flowers = vein.flower().isPresent();
        boolean rocks = MaterialLookup.item(vein.materialId(), MaterialPrefixes.RAW_ORE)
                .isPresent();
        if (!flowers && !rocks) {
            return;
        }
        int sea = level.getSeaLevel();
        int tMinHeight = Math.min(level.getMaxBuildHeight() - 2, sea - 1);
        int tMaxHeight = Math.min(
                level.getMaxBuildHeight() - 1, tMinHeight * 2 + 16);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int tD = 4; tD <= 16; tD *= 2) {
            for (int i = 0; i < tD; i++) {
                int tX = minX + random.nextInt(tD * 2) + 8 - tD;
                int tZ = minZ + random.nextInt(tD * 2) + 8 - tD;
                for (int tY = tMaxHeight; tY > tMinHeight; tY--) {
                    cursor.set(tX, tY, tZ);
                    BlockState contact = level.getBlockState(cursor);
                    if (!contact.getFluidState().isEmpty()
                            || contact.is(Blocks.FARMLAND)) {
                        break;
                    }
                    if (!contact.isSolidRender(level, cursor)
                            || contact.is(BlockTags.LOGS)
                            || contact.is(BlockTags.LEAVES)) {
                        continue;
                    }
                    BlockPos above = cursor.above();
                    BlockState existing = level.getBlockState(above);
                    if (!SurfaceRockFeature.easyRep(existing)) {
                        break;
                    }
                    if (flowers
                            && !contact.is(Blocks.DIRT)
                            && (!rocks || random.nextInt(4) > 0)) {
                        if (placeFlower(level, above, vein.flower().orElseThrow())) {
                            break;
                        }
                    }
                    if (rocks && isIndicatorGround(contact)) {
                        placeIndicator(level, above, contact, vein.materialId());
                    }
                    break;
                }
            }
        }
    }

    public static boolean isBedrockFloor(BlockState state) {
        return state.is(Blocks.BEDROCK)
                || state.getBlock() instanceof BedrockOreBlock;
    }

    static boolean placeBedrockOre(
            WorldGenLevel level,
            int x,
            int y,
            int z,
            String materialId,
            boolean small) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState placed = (small
                ? ModBlocks.GT_SMALL_BEDROCK_ORE
                : ModBlocks.GT_BEDROCK_ORE)
                .get()
                .defaultBlockState();
        if (!level.setBlock(pos, placed, Block.UPDATE_CLIENTS)) {
            return false;
        }
        return BedrockOreBlock.placeMaterial(level, pos, materialId);
    }

    public static boolean placeNormalOre(
            WorldGenLevel level, BlockPos pos, String materialId) {
        BlockState replaced = level.getBlockState(pos);
        if (!isOreHost(replaced)) {
            return false;
        }
        OreStoneHost host = hostOf(replaced);
        if (host == OreStoneHost.NETHERRACK) {
            return placeHostedOre(level, pos, materialId, host);
        }
        Host catalogHost = host == OreStoneHost.DEEPSLATE ? Host.DEEPSLATE : Host.STONE;
        if (!ModBlocks.hasOreBlock(materialId, catalogHost)
                && ModBlocks.hasOreBlock(materialId, Host.STONE)) {
            catalogHost = Host.STONE;
        }
        if (ModBlocks.hasOreBlock(materialId, catalogHost)) {
            return level.setBlock(
                    pos,
                    ModBlocks.oreBlock(materialId, catalogHost).get().defaultBlockState(),
                    Block.UPDATE_CLIENTS);
        }
        return placeHostedOre(level, pos, materialId, host);
    }

    public static boolean placeSmallOre(
            WorldGenLevel level, BlockPos pos, String materialId) {
        BlockState replaced = level.getBlockState(pos);
        if (!isOreHost(replaced)) {
            return false;
        }
        BlockState placed = ModBlocks.GT_SMALL_ORE.get().defaultBlockState()
                .setValue(GtSmallOreBlock.HOST, hostOf(replaced));
        if (!level.setBlock(pos, placed, Block.UPDATE_CLIENTS)) {
            return false;
        }
        return BedrockOreBlock.placeMaterial(level, pos, materialId);
    }

    public static boolean placeHostedOre(
            WorldGenLevel level, BlockPos pos, String materialId, OreStoneHost host) {
        BlockState placed = ModBlocks.GT_HOSTED_ORE.get().defaultBlockState()
                .setValue(GtHostedOreBlock.HOST, host);
        if (!level.setBlock(pos, placed, Block.UPDATE_CLIENTS)) {
            return false;
        }
        return BedrockOreBlock.placeMaterial(level, pos, materialId);
    }

    /** GT6 {@code WD.setOre} / {@code setSmallOre} stone hosts; never air or bedrock. */
    static boolean isOreHost(BlockState state) {
        if (state.isAir()
                || !state.getFluidState().isEmpty()
                || isBedrockFloor(state)) {
            return false;
        }
        return state.is(Blocks.STONE)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.NETHERRACK)
                || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.BASE_STONE_NETHER)
                || state.is(BlockTags.STONE_ORE_REPLACEABLES)
                || state.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
    }

    static OreStoneHost hostOf(BlockState replaced) {
        if (replaced.is(Blocks.NETHERRACK)
                || replaced.is(BlockTags.BASE_STONE_NETHER)) {
            return OreStoneHost.NETHERRACK;
        }
        if (replaced.is(Blocks.DEEPSLATE)
                || replaced.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)) {
            return OreStoneHost.DEEPSLATE;
        }
        return OreStoneHost.STONE;
    }

    private static boolean isIndicatorGround(BlockState contact) {
        return contact.is(BlockTags.DIRT)
                || contact.is(BlockTags.SAND)
                || contact.is(Blocks.GRAVEL)
                || contact.is(BlockTags.BASE_STONE_OVERWORLD)
                || contact.is(BlockTags.BASE_STONE_NETHER)
                || contact.is(BlockTags.STONE_ORE_REPLACEABLES)
                || contact.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)
                || contact.is(Blocks.NETHERRACK);
    }

    private static boolean placeFlower(
            WorldGenLevel level, BlockPos pos, IndicatorFlower flower) {
        BlockState placed = ModBlocks.GT_INDICATOR_FLOWER.get().defaultBlockState()
                .setValue(GtIndicatorFlowerBlock.FLOWER, flower);
        if (!level.setBlock(pos, placed, Block.UPDATE_CLIENTS)) {
            return false;
        }
        if (!placed.canSurvive(level, pos)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            return false;
        }
        return true;
    }

    private static boolean placeIndicator(
            WorldGenLevel level,
            BlockPos pos,
            BlockState contact,
            String materialId) {
        BlockState pebble = ModBlocks.GT_SURFACE_ROCK.get().defaultBlockState()
                .setValue(
                        SurfaceRockAppearance.PROPERTY,
                        SurfaceRockFeature.appearance(contact))
                .setValue(SurfaceRockContents.PROPERTY, SurfaceRockContents.EMPTY);
        if (!level.setBlock(pos, pebble, Block.UPDATE_CLIENTS)) {
            return false;
        }
        if (level.getBlockEntity(pos) instanceof GtSurfaceRockBlockEntity rock) {
            rock.setMaterial(materialId);
            rock.setRawOre(true);
        }
        return true;
    }
}
