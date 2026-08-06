package com.masson.cruciblecraft.worldgen;

import com.masson.cruciblecraft.content.block.SubsurfaceFluidDepositBlock;
import com.masson.cruciblecraft.content.blockentity.SubsurfaceFluidDepositBlockEntity;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * Places one persistent, finite reserve at a deterministic region anchor.
 */
public final class SubsurfaceFluidDepositFeature
        extends Feature<SubsurfaceFluidDepositConfiguration> {
    public SubsurfaceFluidDepositFeature() {
        super(SubsurfaceFluidDepositConfiguration.CODEC);
    }

    @Override
    public boolean place(
            FeaturePlaceContext<SubsurfaceFluidDepositConfiguration> context) {
        SubsurfaceFluidDepositConfiguration config = context.config();
        ChunkPos chunk = new ChunkPos(context.origin());
        long worldSeed = context.level().getSeed();
        int regionX = Math.floorDiv(chunk.x, config.regionSizeChunks());
        int regionZ = Math.floorDiv(chunk.z, config.regionSizeChunks());
        LargeVeinLayout.Anchor selected = LargeVeinLayout.anchor(
                worldSeed,
                regionX,
                regionZ,
                config.regionSizeChunks(),
                config.salt());
        if (chunk.x != selected.x()
                || chunk.z != selected.z()
                || LargeVeinLayout.generationRoll(
                                worldSeed,
                                regionX,
                                regionZ,
                                config.salt())
                        >= config.generationChance()) {
            return false;
        }

        long depositSeed =
                LargeVeinLayout.veinSeed(worldSeed, selected, config.salt());
        int targetY = LargeVeinLayout.centerY(
                depositSeed, config.minY(), config.maxY());
        int x = selected.x() * 16 + 8;
        int z = selected.z() * 16 + 8;
        BlockPos target = findHost(context, x, targetY, z);
        if (target == null) {
            return false;
        }
        BlockState replaced = context.level().getBlockState(target);
        ResourceLocation replacedHost =
                BuiltInRegistries.BLOCK.getKey(replaced.getBlock());
        boolean deepslate =
                replaced.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
        BlockState marker = ModBlocks.SUBSURFACE_FLUID_DEPOSIT.get()
                .defaultBlockState()
                .setValue(
                        SubsurfaceFluidDepositBlock.DEEPSLATE,
                        deepslate);
        context.level().setBlock(target, marker, 2);
        if (!(context.level().getBlockEntity(target)
                instanceof SubsurfaceFluidDepositBlockEntity deposit)) {
            throw new IllegalStateException(
                    "Subsurface deposit marker did not create its block entity");
        }
        deposit.initialize(
                config.material(),
                reserveAmount(config, depositSeed),
                replacedHost,
                config.productionAmountMb(),
                config.productionIntervalTicks(),
                config.accumulationCapMb(),
                config.ventOverflow());
        return true;
    }

    private static BlockPos findHost(
            FeaturePlaceContext<SubsurfaceFluidDepositConfiguration> context,
            int x,
            int targetY,
            int z) {
        SubsurfaceFluidDepositConfiguration config = context.config();
        for (int step = 0; step <= config.searchRange() * 2; step++) {
            int magnitude = (step + 1) / 2;
            int offset = step == 0
                    ? 0
                    : step % 2 == 1 ? magnitude : -magnitude;
            int y = targetY + offset;
            if (y < config.minY() || y > config.maxY()) {
                continue;
            }
            BlockPos candidate = new BlockPos(x, y, z);
            if (context.level().getBlockState(candidate)
                    .is(config.replaceable())) {
                return candidate;
            }
        }
        return null;
    }

    static long reserveAmount(
            SubsurfaceFluidDepositConfiguration config, long depositSeed) {
        long span = config.maxAmountMb() - config.minAmountMb() + 1L;
        return config.minAmountMb()
                + Math.floorMod(LargeVeinLayout.mix(depositSeed), span);
    }
}
