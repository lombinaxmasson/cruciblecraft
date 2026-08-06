package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.content.blockentity.MultiblockPortBlockEntity;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PortType;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** One registry block for one capability-safe multiblock port type. */
public final class MultiblockPortBlock extends Block implements EntityBlock {
    private final PortType portType;

    public MultiblockPortBlock(PortType portType, Properties properties) {
        super(properties);
        this.portType = portType;
    }

    public PortType portType() {
        return portType;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MultiblockPortBlockEntity(pos, state);
    }
}
