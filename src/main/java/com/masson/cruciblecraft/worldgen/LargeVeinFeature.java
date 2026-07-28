package com.masson.cruciblecraft.worldgen;

import java.util.List;

import com.masson.cruciblecraft.worldgen.LargeVeinConfiguration.WeightedState;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * One deterministic anchor chunk is selected in each region. A placed feature
 * may therefore be referenced for every chunk without duplicating the vein.
 * Selection is a pure hash of world seed and region coordinates, so generation
 * order cannot change deposits.
 */
public final class LargeVeinFeature extends Feature<LargeVeinConfiguration> {
    public LargeVeinFeature() {
        super(LargeVeinConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<LargeVeinConfiguration> context) {
        LargeVeinConfiguration config = context.config();
        ChunkPos chunk = new ChunkPos(context.origin());
        long seed = context.level().getSeed();
        int regionX = Math.floorDiv(chunk.x, config.regionSizeChunks());
        int regionZ = Math.floorDiv(chunk.z, config.regionSizeChunks());
        LargeVeinLayout.Anchor selected =
                LargeVeinLayout.anchor(seed, regionX, regionZ, config.regionSizeChunks(), config.salt());
        ChunkPos anchor = new ChunkPos(selected.x(), selected.z());
        if (!chunk.equals(anchor)
                || LargeVeinLayout.generationRoll(seed, regionX, regionZ, config.salt())
                        >= config.generationChance()) {
            return false;
        }

        long veinSeed = LargeVeinLayout.veinSeed(seed, selected, config.salt());
        int centerX = anchor.getMinBlockX() + 8;
        int centerZ = anchor.getMinBlockZ() + 8;
        int centerY = LargeVeinLayout.centerY(veinSeed, config.minY(), config.maxY());
        int placed = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int dx = -config.horizontalRadius(); dx <= config.horizontalRadius(); dx++) {
            for (int dz = -config.horizontalRadius(); dz <= config.horizontalRadius(); dz++) {
                double radial = (dx * dx + dz * dz)
                        / (double) (config.horizontalRadius() * config.horizontalRadius());
                if (radial > 1.0) {
                    continue;
                }
                for (int dy = -config.verticalRadius(); dy <= config.verticalRadius(); dy++) {
                    double vertical = dy / (double) config.verticalRadius();
                    double shape = radial + vertical * vertical;
                    if (shape > 1.0) {
                        continue;
                    }
                    long cell = LargeVeinLayout.hash(veinSeed, centerX + dx, centerY + dy, centerZ + dz);
                    double edgeAdjustedDensity = config.density() * (1.0 - shape * 0.45);
                    if (LargeVeinLayout.unit(cell) >= edgeAdjustedDensity) {
                        continue;
                    }
                    cursor.set(centerX + dx, centerY + dy, centerZ + dz);
                    BlockState replaced = context.level().getBlockState(cursor);
                    if (!replaced.is(config.replaceable())) {
                        continue;
                    }
                    List<WeightedState> role = role(config, vertical, radial, cell);
                    if (role.isEmpty()) {
                        continue;
                    }
                    var adapted = OreHostStateAdapter.adapt(
                            pick(role, LargeVeinLayout.mix(cell)), replaced);
                    if (adapted.isEmpty()) {
                        continue;
                    }
                    context.level().setBlock(cursor, adapted.orElseThrow(), 2);
                    placed++;
                }
            }
        }
        return placed > 0;
    }

    private static List<WeightedState> role(
            LargeVeinConfiguration config, double vertical, double radial, long cell) {
        return switch (LargeVeinLayout.role(vertical, radial, cell)) {
            case "spread" -> config.spread();
            case "between" -> config.between();
            case "top" -> config.top();
            default -> config.bottom();
        };
    }

    private static net.minecraft.world.level.block.state.BlockState pick(List<WeightedState> states, long value) {
        int total = states.stream().mapToInt(WeightedState::weight).sum();
        int selected = Math.floorMod((int) value, total);
        for (WeightedState entry : states) {
            selected -= entry.weight();
            if (selected < 0) {
                return entry.state();
            }
        }
        return states.getLast().state();
    }

}
