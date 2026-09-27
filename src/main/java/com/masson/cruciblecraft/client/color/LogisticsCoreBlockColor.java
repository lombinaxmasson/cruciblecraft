package com.masson.cruciblecraft.client.color;

import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** steel_galvanized tint for dual-layer Logistics Core cubes (tintindex 0). */
public final class LogisticsCoreBlockColor {
    private static final String MATERIAL_ID = "steel_galvanized";

    private LogisticsCoreBlockColor() {}

    public static int blockColor(
            BlockState state,
            net.minecraft.world.level.BlockAndTintGetter level,
            net.minecraft.core.BlockPos pos,
            int tintIndex) {
        if (tintIndex != 0) {
            return 0xFFFFFFFF;
        }
        return color();
    }

    public static int itemColor(ItemStack stack, int tintIndex) {
        if (tintIndex != 0) {
            return 0xFFFFFFFF;
        }
        return color();
    }

    public static Block[] tintedBlocks() {
        return new Block[] {
            ModBlocks.LOGISTICS_CORE.get(),
            ModBlocks.VENTILATION_UNIT.get(),
            ModBlocks.VERSATILE_PROCESSOR_UNIT.get(),
            ModBlocks.LOGIC_PROCESSOR_UNIT.get(),
            ModBlocks.CONTROL_PROCESSOR_UNIT.get(),
            ModBlocks.STORAGE_PROCESSOR_UNIT.get(),
            ModBlocks.CONVERSION_PROCESSOR_UNIT.get()
        };
    }

    private static int color() {
        return MaterialLookup.byId(MATERIAL_ID)
                .map(material -> 0xFF000000 | material.colorRgb())
                .orElse(0xFFFAF0F0);
    }
}
