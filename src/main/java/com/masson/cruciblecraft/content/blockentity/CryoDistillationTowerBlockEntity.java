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
 * GT6 {@code MultiTileEntityCryoDistillationTower} 17111. Same parts, CU
 * instead of HU.
 */
public final class CryoDistillationTowerBlockEntity
        extends AbstractDistillationTowerBlockEntity {
    public CryoDistillationTowerBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModBlockEntities.CRYO_DISTILLATION_TOWER.get(),
                pos,
                state,
                ModMultiblockControllers.CRYO_DISTILLATION_TOWER
                        .requireVariant());
    }

    @Override
    protected DistillationTowerFluidRouting.Kind routing() {
        return DistillationTowerFluidRouting.Kind.CRYO;
    }

    @Override
    protected List<ResourceLocation> plugins() {
        return ModMultiblockPlugins.DISTILLATION_TOWER_PLUGINS;
    }

    @Override
    protected String translationKey() {
        return "block.cruciblecraft.cryo_distillation_tower";
    }

    @Override
    public MultiblockControllerSpec controllerSpec() {
        return ModMultiblockControllers.CRYO_DISTILLATION_TOWER;
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            CryoDistillationTowerBlockEntity tower) {
        AbstractDistillationTowerBlockEntity.serverTick(level, pos, state, tower);
    }
}
