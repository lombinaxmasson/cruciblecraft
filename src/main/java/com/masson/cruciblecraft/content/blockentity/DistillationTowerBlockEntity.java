package com.masson.cruciblecraft.content.blockentity;

import java.util.List;

import com.masson.cruciblecraft.content.multiblock.MultiblockControllerSpec;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModMultiblockControllers;
import com.masson.cruciblecraft.registry.ModMultiblockPlugins;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6 {@code MultiTileEntityDistillationTower} 17101. Own recipe map, HU
 * buffer, height-routed backside auto-output.
 */
public final class DistillationTowerBlockEntity
        extends AbstractDistillationTowerBlockEntity {
    public DistillationTowerBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModBlockEntities.DISTILLATION_TOWER.get(),
                pos,
                state,
                ModMultiblockControllers.DISTILLATION_TOWER
                        .requireVariant());
    }

    @Override
    protected DistillationTowerFluidRouting.Kind routing() {
        return DistillationTowerFluidRouting.Kind.HOT;
    }

    @Override
    protected List<ResourceLocation> plugins() {
        return ModMultiblockPlugins.DISTILLATION_TOWER_PLUGINS;
    }

    @Override
    protected String translationKey() {
        return "block.cruciblecraft.distillation_tower";
    }

    @Override
    public MultiblockControllerSpec controllerSpec() {
        return ModMultiblockControllers.DISTILLATION_TOWER;
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            DistillationTowerBlockEntity tower) {
        AbstractDistillationTowerBlockEntity.serverTick(level, pos, state, tower);
    }
}
