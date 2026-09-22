package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PortType;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.world.level.block.Block;

/** Resolves the live registry block for one JSON multiblock port type. */
public final class MultiblockPortBlocks {
    private MultiblockPortBlocks() {}

    public static Block of(PortType type) {
        return switch (type) {
            case ITEM_FLUID -> ModBlocks.MULTIBLOCK_ITEM_FLUID_PORT.get();
            case ITEM_FLUID_IN, ITEM_FLUID_OUT ->
                    StainlessSteelMixerWalls.wall();
            case ITEM_FLUID_ENERGY_IN -> ElectrolyzerParts.part();
            case ITEM_FLUID_ENERGY -> AutoclaveWalls.wall();
            case ENERGY_INPUT -> ModBlocks.MULTIBLOCK_ENERGY_INPUT_PORT.get();
            case FLUID_OUT -> ModBlocks.MULTIBLOCK_FLUID_OUT_PORT.get();
        };
    }
}
