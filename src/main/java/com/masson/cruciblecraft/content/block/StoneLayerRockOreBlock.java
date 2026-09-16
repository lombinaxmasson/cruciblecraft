package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.worldgen.StoneLayerStones;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6 {@code BlockRockOres} dense cube used as a {@code StoneLayer} surface.
 * Drops {@code oreRaw} via loot tables; silk harvest keeps the cube.
 */
public final class StoneLayerRockOreBlock extends Block {
    private final StoneLayerStones.Cube cube;

    public StoneLayerRockOreBlock(
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

    @Override
    public int getFlammability(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            Direction direction) {
        return cube.flammability();
    }

    @Override
    public int getFireSpreadSpeed(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            Direction direction) {
        return 0;
    }

    @Override
    protected void spawnAfterBreak(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            ItemStack stack,
            boolean dropExperience) {
        super.spawnAfterBreak(state, level, pos, stack, dropExperience);
        if (dropExperience && level.random.nextInt(8) == 0) {
            this.tryDropExperience(level, pos, stack, ConstantInt.of(1));
        }
    }
}
