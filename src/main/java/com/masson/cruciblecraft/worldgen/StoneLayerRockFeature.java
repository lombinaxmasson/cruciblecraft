package com.masson.cruciblecraft.worldgen;

import java.util.List;

import com.masson.cruciblecraft.content.blockentity.GtSurfaceRockBlockEntity;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.worldgen.OreHostVariantCatalog.Host;
import com.masson.cruciblecraft.worldgen.StoneLayerCatalog.Layer;
import com.masson.cruciblecraft.worldgen.StoneLayerCatalog.Ore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * GT6 {@code WorldgenStoneLayers}: 7-cell scan replaces stone/cobble/vanilla
 * ores with the noise-selected cube or {@code StoneLayerOres}, then 1/128
 * 32757 pebbles using {@code tLastRock} / indicator ore.
 */
public class StoneLayerRockFeature extends Feature<StoneLayerRockConfiguration> {
    private static final int SCAN = 7;

    public StoneLayerRockFeature() {
        super(StoneLayerRockConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<StoneLayerRockConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        int minX = origin.getX() & ~15;
        int minZ = origin.getZ() & ~15;
        int chunkX = minX >> 4;
        int chunkZ = minZ >> 4;
        long chunkSeed = level.getSeed()
                ^ (chunkX * 341873128712L + chunkZ * 132897987541L)
                ^ 32757L;
        RandomSource random = RandomSource.create(chunkSeed);
        int minY = level.getMinBuildHeight() + 1;
        int maxY = level.getMaxBuildHeight();
        int minBuildHeight = level.getMinBuildHeight();
        StoneLayerNoise noise = new StoneLayerNoise((int) level.getSeed(), 0);
        boolean placed = false;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                placed |= decorateColumn(
                        level,
                        minX + dx,
                        minZ + dz,
                        minY,
                        maxY,
                        minBuildHeight,
                        noise,
                        random,
                        context.config().probability());
            }
        }
        return placed;
    }

    public static boolean decorateColumn(
            WorldGenLevel level,
            int x,
            int z,
            int minY,
            int maxY,
            int minBuildHeight,
            StoneLayerNoise noise,
            RandomSource random,
            int probability) {
        Layer[] scan = new Layer[SCAN];
        for (int k = 0; k < SCAN; k++) {
            scan[k] = StoneLayerCatalog.layerAt(noise, x, k - 2, z, minBuildHeight);
        }
        String lastRock = StoneLayerCatalog.DEEPSLATE;
        String lastOre = null;
        boolean canPlace = false;
        boolean placed = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        Holder<Biome> biome = level.getBiome(
                new BlockPos(x, level.getSeaLevel(), z));
        int chance = Math.max(1, probability);
        for (int y = minY; y < maxY; y++) {
            cursor.set(x, y, z);
            BlockState state = level.getBlockState(cursor);
            Layer current = scan[3];
            if (state.is(Blocks.BEDROCK)) {
                canPlace = true;
            } else if (state.isAir()) {
                if (canPlace && random.nextInt(chance) == 0) {
                    placed |= tryPlace(
                            level,
                            cursor.immutable(),
                            pebbleMaterial(random, lastOre, lastRock));
                }
                lastOre = null;
                canPlace = false;
            } else if (isReplaceableStone(state) || isVanillaOre(state)) {
                canPlace = true;
                String indicator = null;
                if (scan[5] == scan[1]) {
                    indicator = tryLayerOres(
                            level,
                            cursor.immutable(),
                            current,
                            current.ores(),
                            false,
                            y,
                            random,
                            biome,
                            minBuildHeight,
                            scan[6] == scan[0]);
                } else {
                    indicator = tryLayerOres(
                            level,
                            cursor.immutable(),
                            current,
                            StoneLayerCatalog.boundaryOres(scan[5], scan[1]),
                            true,
                            y,
                            random,
                            biome,
                            minBuildHeight,
                            false);
                }
                if (indicator == null
                        && scan[4] != scan[2]
                        && !StoneLayerCatalog.randomSmallGems().isEmpty()
                        && random.nextInt(100) == 0
                        && tryPlaceOre(
                                level,
                                cursor.immutable(),
                                gemOre(StoneLayerCatalog.pickRandomGem(random), y),
                                current)) {
                    indicator = "";
                }
                if (indicator == null) {
                    lastRock = current.material();
                    placed |= tryReplace(
                            level,
                            cursor.immutable(),
                            current.material(),
                            StoneLayerStones.Role.STONE);
                } else {
                    if (!indicator.isEmpty()) {
                        lastOre = indicator;
                    }
                    placed = true;
                }
            } else if (isCobble(state)) {
                canPlace = true;
                lastRock = current.material();
                placed |= tryReplace(
                        level,
                        cursor.immutable(),
                        current.material(),
                        StoneLayerStones.Role.COBBLE);
            } else if (isMossyCobble(state)) {
                canPlace = true;
                lastRock = current.material();
                placed |= tryReplace(
                        level,
                        cursor.immutable(),
                        current.material(),
                        StoneLayerStones.Role.MOSSY_COBBLE);
            } else if (StoneLayerStones.isNaturalLayerCube(state)) {
                canPlace = true;
            } else if (SurfaceRockFeature.easyRep(state)
                    && state.getFluidState().isEmpty()) {
                if (canPlace && random.nextInt(chance) == 0) {
                    placed |= tryPlace(
                            level,
                            cursor.immutable(),
                            pebbleMaterial(random, lastOre, lastRock));
                }
                lastOre = null;
                canPlace = false;
            } else if (state.isSolidRender(level, cursor)) {
                canPlace = isGroundSurface(state);
            } else {
                lastOre = null;
                canPlace = false;
            }
            for (int t = 0; t < SCAN - 1; t++) {
                scan[t] = scan[t + 1];
            }
            scan[SCAN - 1] = StoneLayerCatalog.layerAt(
                    noise, x, y - 2 + SCAN - 1, z, minBuildHeight);
        }
        return placed;
    }

    public static boolean tryPlaceOre(
            WorldGenLevel level,
            BlockPos pos,
            Ore ore,
            Layer layer) {
        return tryPlaceOre(level, pos, ore, layer, true);
    }

    public static boolean tryPlaceOre(
            WorldGenLevel level,
            BlockPos pos,
            Ore ore,
            Layer layer,
            @SuppressWarnings("unused") boolean normal) {
        if (ore.vanillaBlock() != null) {
            ResourceLocation id = ResourceLocation.parse(ore.vanillaBlock());
            if (!BuiltInRegistries.BLOCK.containsKey(id)) {
                return false;
            }
            return level.setBlock(
                    pos,
                    BuiltInRegistries.BLOCK.get(id).defaultBlockState(),
                    Block.UPDATE_CLIENTS);
        }
        if (ore.material() == null || ore.material().isEmpty()) {
            return false;
        }
        Host host = StoneLayerCatalog.DEEPSLATE.equals(layer.material())
                ? Host.DEEPSLATE
                : Host.STONE;
        if (!ModBlocks.hasOreBlock(ore.material(), host)) {
            return false;
        }
        // GT6 PrefixBlock ore vs oreSmall. CC has one stone/deepslate host pair.
        return level.setBlock(
                pos,
                ModBlocks.oreBlock(ore.material(), host).get().defaultBlockState(),
                Block.UPDATE_CLIENTS);
    }

    public static boolean tryReplace(
            WorldGenLevel level,
            BlockPos pos,
            String material,
            StoneLayerStones.Role role) {
        BlockState target = StoneLayerStones.cube(material, role);
        BlockState existing = level.getBlockState(pos);
        if (existing.getBlock() == target.getBlock()) {
            return false;
        }
        return level.setBlock(pos, target, Block.UPDATE_CLIENTS);
    }

    public static boolean tryPlace(
            WorldGenLevel level,
            BlockPos pos,
            String material) {
        if (material == null || material.isEmpty()) {
            return false;
        }
        BlockState existing = level.getBlockState(pos);
        if (!existing.getFluidState().isEmpty()) {
            return false;
        }
        if (!existing.isAir() && !SurfaceRockFeature.easyRep(existing)) {
            return false;
        }
        BlockState pebble = ModBlocks.GT_SURFACE_ROCK.get().defaultBlockState()
                .setValue(
                        SurfaceRockAppearance.PROPERTY,
                        SurfaceRockFeature.appearance(level.getBlockState(pos.below())))
                .setValue(SurfaceRockContents.PROPERTY, SurfaceRockContents.EMPTY);
        if (!level.setBlock(pos, pebble, Block.UPDATE_CLIENTS)) {
            return false;
        }
        if (level.getBlockEntity(pos) instanceof GtSurfaceRockBlockEntity rock) {
            rock.setMaterial(material);
        }
        return true;
    }

    static boolean isReplaceableStone(BlockState state) {
        return state.is(Blocks.STONE)
                || state.is(Blocks.INFESTED_STONE)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.INFESTED_DEEPSLATE)
                || state.is(Blocks.GRANITE)
                || state.is(Blocks.DIORITE)
                || state.is(Blocks.ANDESITE)
                || state.is(Blocks.TUFF);
    }

    static boolean isCobble(BlockState state) {
        return state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.COBBLED_DEEPSLATE);
    }

    static boolean isMossyCobble(BlockState state) {
        return state.is(Blocks.MOSSY_COBBLESTONE);
    }

    public static boolean isVanillaOre(BlockState state) {
        return state.is(BlockTags.COAL_ORES)
                || state.is(BlockTags.IRON_ORES)
                || state.is(BlockTags.GOLD_ORES)
                || state.is(BlockTags.DIAMOND_ORES)
                || state.is(BlockTags.EMERALD_ORES)
                || state.is(BlockTags.LAPIS_ORES)
                || state.is(BlockTags.REDSTONE_ORES)
                || state.is(BlockTags.COPPER_ORES);
    }

    static boolean isGroundSurface(BlockState state) {
        return state.is(BlockTags.DIRT)
                || state.is(BlockTags.SAND)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.CLAY)
                || state.is(BlockTags.TERRACOTTA);
    }

    /**
     * @return indicator material, empty string if placed without indicators,
     *         or {@code null} if nothing placed
     */
    private static String tryLayerOres(
            WorldGenLevel level,
            BlockPos pos,
            Layer layer,
            List<Ore> ores,
            boolean boundary,
            int y,
            RandomSource random,
            Holder<Biome> biome,
            int minBuildHeight,
            boolean normal) {
        for (Ore ore : ores) {
            if (!StoneLayerCatalog.check(ore, y, random, biome, minBuildHeight)) {
                continue;
            }
            if (boundary && y != ore.minY() && y != ore.maxY()) {
                random.nextBoolean();
            }
            if (!tryPlaceOre(level, pos, ore, layer, normal)) {
                continue;
            }
            return ore.indicators() && ore.material() != null ? ore.material() : "";
        }
        return null;
    }

    private static Ore gemOre(String material, int y) {
        return new Ore(
                material,
                StoneLayerCatalog.UNIT,
                y,
                y,
                false,
                null,
                List.of(),
                false);
    }

    private static String pebbleMaterial(
            RandomSource random, String lastOre, String lastRock) {
        if (lastOre != null
                && random.nextBoolean()
                && ModBlocks.hasRockBlock(lastOre)) {
            return lastOre;
        }
        return lastRock;
    }
}
