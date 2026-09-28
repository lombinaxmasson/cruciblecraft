package com.masson.cruciblecraft.compat.emi.multiblock;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;

/**
 * Read-only stand-in world for one preview scene, like GTCEu's
 * {@code SchemaLevel}: neighbours cull faces and feed ambient occlusion.
 * Light is always full; block entities never join a real level.
 */
public final class PreviewBlockGetter implements BlockAndTintGetter {
    private final Map<BlockPos, BlockState> states;
    private final Map<BlockPos, BlockEntity> blockEntities;
    @Nullable
    private final Biome biome;

    private PreviewBlockGetter(
            Map<BlockPos, BlockState> states,
            Map<BlockPos, BlockEntity> blockEntities,
            @Nullable Biome biome) {
        this.states = states;
        this.blockEntities = blockEntities;
        this.biome = biome;
    }

    public static PreviewBlockGetter of(PreviewScene scene) {
        Map<BlockPos, BlockState> states = new LinkedHashMap<>();
        Map<BlockPos, BlockEntity> blockEntities = new LinkedHashMap<>();
        scene.cells().forEach((pos, cell) -> {
            BlockState state = BuiltInRegistries.BLOCK.getOptional(cell.block())
                    .map(block -> block.defaultBlockState())
                    .orElse(Blocks.AIR.defaultBlockState());
            if (state.isAir()) {
                return;
            }
            states.put(pos, state);
            if (state.getBlock() instanceof EntityBlock entityBlock) {
                try {
                    BlockEntity blockEntity = entityBlock.newBlockEntity(pos, state);
                    if (blockEntity != null) {
                        blockEntities.put(pos, blockEntity);
                    }
                } catch (RuntimeException ignored) {
                    // Some block entities need a live level to construct.
                }
            }
        });
        return new PreviewBlockGetter(states, blockEntities, plains());
    }

    public Map<BlockPos, BlockState> states() {
        return states;
    }

    public Map<BlockPos, BlockEntity> blockEntities() {
        return blockEntities;
    }

    /**
     * The same scene shifted onto a placed structure, so ghost rendering
     * reads neighbours at the world positions it draws at.
     */
    public PreviewBlockGetter placed(BlockPos origin, Direction facing) {
        Map<BlockPos, BlockState> moved = new LinkedHashMap<>();
        states.forEach((pos, state) -> moved.put(
                origin.offset(facingLocal(pos, facing)), state));
        return new PreviewBlockGetter(moved, Map.of(), biome);
    }

    private static BlockPos facingLocal(BlockPos pos, Direction facing) {
        return switch (facing) {
            case NORTH -> pos;
            case EAST -> new BlockPos(-pos.getZ(), pos.getY(), pos.getX());
            case SOUTH -> new BlockPos(-pos.getX(), pos.getY(), -pos.getZ());
            case WEST -> new BlockPos(pos.getZ(), pos.getY(), -pos.getX());
            default -> pos;
        };
    }

    @Nullable
    private static Biome plains() {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }
        return level.registryAccess()
                .registryOrThrow(Registries.BIOME)
                .getOptional(Biomes.PLAINS)
                .orElse(null);
    }

    @Override
    public float getShade(Direction direction, boolean shade) {
        if (!shade) {
            return 1f;
        }
        return switch (direction) {
            case DOWN -> 0.5f;
            case UP -> 1f;
            case NORTH, SOUTH -> 0.8f;
            case WEST, EAST -> 0.6f;
        };
    }

    @Override
    public LevelLightEngine getLightEngine() {
        throw new UnsupportedOperationException("Preview light is constant");
    }

    @Override
    public int getBrightness(LightLayer lightType, BlockPos blockPos) {
        return 15;
    }

    @Override
    public int getRawBrightness(BlockPos blockPos, int amount) {
        return 15;
    }

    @Override
    public boolean canSeeSky(BlockPos blockPos) {
        return true;
    }

    @Override
    public int getBlockTint(BlockPos blockPos, ColorResolver colorResolver) {
        if (biome == null) {
            return 0xFFFFFF;
        }
        return colorResolver.getColor(biome, blockPos.getX(), blockPos.getZ());
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return blockEntities.get(pos);
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return states.getOrDefault(pos, Blocks.AIR.defaultBlockState());
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }

    @Override
    public int getHeight() {
        return 384;
    }

    @Override
    public int getMinBuildHeight() {
        return -64;
    }
}
