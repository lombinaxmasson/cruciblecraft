package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Placeable GT6 {@code OP.blockDust} PrefixBlock: sand-like, shovel, gravity.
 */
public final class MaterialDustBlock extends FallingBlock {
    public static final MapCodec<MaterialDustBlock> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.STRING.fieldOf("material")
                            .forGetter(MaterialDustBlock::materialId),
                    propertiesCodec())
                    .apply(instance, MaterialDustBlock::new));
    private final String materialId;

    public MaterialDustBlock(String materialId, Properties properties) {
        super(properties);
        this.materialId = materialId;
    }

    public String materialId() {
        return materialId;
    }

    @Override
    public MapCodec<MaterialDustBlock> codec() {
        return CODEC;
    }

    @Override
    public int getDustColor(BlockState state, BlockGetter level, BlockPos pos) {
        return MaterialCatalog.find(materialId)
                .map(MaterialDefinition::colorRgb)
                .orElse(0xFFC6C6C6);
    }
}
