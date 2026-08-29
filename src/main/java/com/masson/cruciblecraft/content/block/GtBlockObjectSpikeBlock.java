package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.content.item.GtBlockObjectCatalog;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** GT spike identity. Damages living entities that step on it. */
public final class GtBlockObjectSpikeBlock extends Block {
    private final GtBlockObjectCatalog.Variant variant;

    public GtBlockObjectSpikeBlock(
            GtBlockObjectCatalog.Variant variant, Properties properties) {
        super(properties);
        this.variant = variant;
    }

    public GtBlockObjectCatalog.Variant variant() {
        return variant;
    }

    @Override
    public void stepOn(
            Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!entity.isSteppingCarefully()
                && entity instanceof LivingEntity living) {
            living.hurt(level.damageSources().cactus(), 1.0F);
        }
        super.stepOn(level, pos, state, entity);
    }
}
