package com.masson.cruciblecraft.worldgen;

import java.util.Random;

import com.masson.cruciblecraft.content.block.BedrockOreBlock;
import com.masson.cruciblecraft.content.block.GtIndicatorGrassBlock;
import com.masson.cruciblecraft.content.blockentity.FluidSpringBlockEntity;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6 {@code WorldgenFluidSpring} no-mod rows. Competing rolls share the
 * bedrock-ore mutex (one spring per chunk). Floor Y is {@code minBuildHeight}.
 */
public final class FluidSpringVeins {
    private FluidSpringVeins() {}

    public static boolean generate(WorldGenLevel level, BlockPos origin, Random random) {
        int minX = origin.getX() & ~15;
        int minZ = origin.getZ() & ~15;
        int floorY = level.getMinBuildHeight();
        if (hasBedrockOre(level, minX, minZ, floorY)) {
            return false;
        }
        boolean nether = level.getLevel().dimension() == Level.NETHER;
        boolean claimed = false;
        boolean placed = false;
        for (FluidSpringCatalog.Vein vein : FluidSpringCatalog.forNether(nether)) {
            if (claimed) {
                break;
            }
            if (random.nextInt(vein.probability()) != 0) {
                continue;
            }
            claimed = true;
            placed = placeSpring(level, minX, minZ, floorY, vein, random);
        }
        return placed;
    }

    public static boolean placeSpring(
            WorldGenLevel level, int minX, int minZ, int floorY) {
        return placeSpring(
                level,
                minX,
                minZ,
                floorY,
                FluidSpringCatalog.overworldLava(),
                new Random(1L));
    }

    public static boolean placeSpring(
            WorldGenLevel level,
            int minX,
            int minZ,
            int floorY,
            FluidSpringCatalog.Vein vein,
            Random random) {
        if (!BedrockOreVeins.isBedrockFloor(
                level.getBlockState(new BlockPos(minX + 8, floorY, minZ + 8)))) {
            return false;
        }
        if (hasBedrockOre(level, minX, minZ, floorY)) {
            return false;
        }
        boolean nether = level.getLevel().dimension() == Level.NETHER;
        BlockState fill = nether
                ? Blocks.NETHERRACK.defaultBlockState()
                : Blocks.DEEPSLATE.defaultBlockState();
        BlockState fluid = vein.fluid().sourceState();
        for (int i = 0; i <= 6; i++) {
            for (int tX = i; tX <= 15 - i; tX++) {
                for (int tZ = i; tZ <= 15 - i; tZ++) {
                    BlockPos cap = new BlockPos(minX + tX, floorY + i + 1, minZ + tZ);
                    BlockState capState = level.getBlockState(cap);
                    if (!capState.isSolidRender(level, cap)) {
                        level.setBlock(cap, fill, Block.UPDATE_CLIENTS);
                    }
                    if (i > 0) {
                        level.setBlock(
                                new BlockPos(minX + tX, floorY + i, minZ + tZ),
                                fluid,
                                Block.UPDATE_CLIENTS);
                    }
                    if (vein.springAmount() > 0
                            && i > 2
                            && random.nextInt(16) == 0) {
                        BlockPos floor = new BlockPos(minX + tX, floorY, minZ + tZ);
                        if (level.getBlockState(floor).is(Blocks.BEDROCK)) {
                            placeSpringTile(level, floor, vein);
                        }
                    }
                }
            }
        }
        placeIndicators(level, minX, minZ, vein, random);
        return true;
    }

    public static void placeSpringTile(
            WorldGenLevel level, BlockPos pos, FluidSpringCatalog.Vein vein) {
        BlockState placed = ModBlocks.GT_FLUID_SPRING.get().defaultBlockState();
        if (!level.setBlock(pos, placed, Block.UPDATE_CLIENTS)) {
            return;
        }
        if (level.getBlockEntity(pos) instanceof FluidSpringBlockEntity spring) {
            spring.configure(vein.fluid().fluidId().toString(), vein.springAmount());
        }
    }

    public static void placeIndicators(
            WorldGenLevel level,
            int minX,
            int minZ,
            FluidSpringCatalog.Vein vein,
            Random random) {
        if (vein.indicatorType() < 1 || vein.indicatorType() > 3) {
            return;
        }
        IndicatorGrass grass = IndicatorGrass.ofIndicatorType(vein.indicatorType());
        BlockState dyed = ModBlocks.GT_INDICATOR_GRASS.get().defaultBlockState()
                .setValue(GtIndicatorGrassBlock.GRASS, grass);
        int minHeight = Math.min(level.getMaxBuildHeight() - 2, level.getSeaLevel() - 1);
        int maxHeight = Math.min(level.getMaxBuildHeight() - 1, minHeight * 2 + 16);
        for (int i = 0; i < 6; i++) {
            int tX = minX + 4 + random.nextInt(8);
            int tZ = minZ + 4 + random.nextInt(8);
            for (int tY = maxHeight; tY > minHeight; tY--) {
                BlockPos pos = new BlockPos(tX, tY, tZ);
                BlockState contact = level.getBlockState(pos);
                if (contact.liquid() || contact.is(Blocks.FARMLAND)) {
                    break;
                }
                if (!contact.isSolidRender(level, pos)
                        || contact.is(BlockTags.LOGS)
                        || contact.is(BlockTags.LEAVES)) {
                    continue;
                }
                if (!GtIndicatorGrassBlock.isPlantableGrass(contact)) {
                    break;
                }
                for (int a = -1; a <= 1; a++) {
                    for (int b = -1; b <= 1; b++) {
                        if (!random.nextBoolean()) {
                            continue;
                        }
                        BlockPos dyedPos = new BlockPos(tX + a, tY, tZ + b);
                        if (GtIndicatorGrassBlock.isPlantableGrass(
                                level.getBlockState(dyedPos))) {
                            level.setBlock(dyedPos, dyed, Block.UPDATE_CLIENTS);
                        }
                    }
                }
                break;
            }
        }
    }

    public static boolean hasBedrockOre(
            WorldGenLevel level, int minX, int minZ, int floorY) {
        for (int tX = 5; tX < 11; tX++) {
            for (int tZ = 5; tZ < 11; tZ++) {
                if (level.getBlockState(new BlockPos(minX + tX, floorY, minZ + tZ))
                        .getBlock() instanceof BedrockOreBlock) {
                    return true;
                }
            }
        }
        return false;
    }
}
