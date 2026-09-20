package com.masson.cruciblecraft.client.color;

import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.content.block.DistillationTowerParts;
import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.block.MultiblockPortBlock;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PortType;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6 18102 is StainlessSteel; 18101 heat transmitters are Invar. Overlay
 * faces stay white ({@code tintindex} 0 only).
 */
public final class DistillationTowerPartColor {
    private DistillationTowerPartColor() {}

    public static int blockColor(
            BlockState state,
            net.minecraft.world.level.BlockAndTintGetter level,
            net.minecraft.core.BlockPos pos,
            int tintIndex) {
        if (tintIndex != 0) {
            return 0xFFFFFFFF;
        }
        String material = materialId(state);
        if (material == null) {
            return 0xFFFFFFFF;
        }
        return MaterialLookup.byId(material)
                .map(entry -> 0xFF000000 | entry.colorRgb())
                .orElse(0xFFFFFFFF);
    }

    public static int itemColor(ItemStack stack, int tintIndex) {
        return blockColor(
                Block.byItem(stack.getItem()).defaultBlockState(),
                null,
                null,
                tintIndex);
    }

    private static String materialId(BlockState state) {
        if (state.getBlock() instanceof MteInPlaceBlock inplace
                && DistillationTowerParts.isLivePort(inplace.spec())) {
            return DistillationTowerParts.isHeatTransmitter(inplace.spec())
                    ? "invar"
                    : "stainless_steel";
        }
        if (!state.hasProperty(MultiblockPortBlock.TOWER_SKIN)
                || !state.getValue(MultiblockPortBlock.TOWER_SKIN)
                || !(state.getBlock() instanceof MultiblockPortBlock port)) {
            return null;
        }
        return port.portType() == PortType.ENERGY_INPUT
                ? "invar"
                : "stainless_steel";
    }

    public static Block[] tintedBlocks() {
        return new Block[] {
                ModBlocks.MULTIBLOCK_ITEM_FLUID_PORT.get(),
                ModBlocks.MULTIBLOCK_ENERGY_INPUT_PORT.get(),
                ModBlocks.MULTIBLOCK_FLUID_OUT_PORT.get(),
                DistillationTowerParts.heatTransmitter(),
                DistillationTowerParts.towerPart()
        };
    }
}
