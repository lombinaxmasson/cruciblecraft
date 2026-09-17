package com.masson.cruciblecraft.content.block;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.content.blockentity.BedrockOreBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6 {@code BlocksGT.oreBedrock} / {@code oreSmallBedrock}: unbreakable, no
 * drops, PrefixBlock material on the block entity. Drill-only.
 */
public final class BedrockOreBlock extends Block implements EntityBlock {
    private final boolean small;

    public BedrockOreBlock(boolean small, Properties properties) {
        super(properties);
        this.small = small;
    }

    public boolean small() {
        return small;
    }

    public static Optional<String> materialAt(BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof BedrockOreBlockEntity ore
                && ore.hasMaterial()) {
            return Optional.of(ore.materialId());
        }
        return Optional.empty();
    }

    public static boolean placeMaterial(
            net.minecraft.world.level.LevelAccessor level,
            BlockPos pos,
            String materialId) {
        if (level.getBlockEntity(pos) instanceof BedrockOreBlockEntity ore) {
            ore.setMaterialId(materialId);
            return true;
        }
        return false;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BedrockOreBlockEntity(pos, state);
    }
}
