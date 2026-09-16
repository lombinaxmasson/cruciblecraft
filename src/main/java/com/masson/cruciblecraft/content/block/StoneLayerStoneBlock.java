package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.worldgen.StoneLayerStones;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6 {@code BlockStones} meta 0/1/2 cube used by {@code WorldgenStoneLayers}.
 * Distinct from the smelter {@code GtStoneCatalog} brick identities.
 */
public final class StoneLayerStoneBlock extends Block {
    private final StoneLayerStones.Cube cube;

    public StoneLayerStoneBlock(
            StoneLayerStones.Cube cube, Properties properties) {
        super(properties);
        this.cube = cube;
    }

    public StoneLayerStones.Cube cube() {
        return cube;
    }

    public String materialId() {
        return cube.material();
    }

    public StoneLayerStones.Role role() {
        return cube.role();
    }

    @Override
    public boolean canEntityDestroy(
            BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        if (cube.harvestLevel() >= 3 && entity instanceof WitherBoss) {
            return false;
        }
        return super.canEntityDestroy(state, level, pos, entity);
    }
}
