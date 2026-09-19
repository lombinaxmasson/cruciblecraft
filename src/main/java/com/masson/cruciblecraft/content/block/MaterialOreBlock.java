package com.masson.cruciblecraft.content.block;

import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Unique {@code generates_ore} cube with a PrefixBlock-style host so layer
 * granite/marble/coal (and the rest) keep their rock texture under the flecks.
 * Deepslate stays a separate block id.
 */
public final class MaterialOreBlock extends DropExperienceBlock {
    public static final EnumProperty<OreStoneHost> HOST =
            EnumProperty.create("host", OreStoneHost.class, OreStoneHost::uniqueOverworld);

    public MaterialOreBlock(IntProvider xpRange, Properties properties) {
        super(xpRange, properties);
        registerDefaultState(stateDefinition.any().setValue(HOST, OreStoneHost.STONE));
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HOST);
    }
}
