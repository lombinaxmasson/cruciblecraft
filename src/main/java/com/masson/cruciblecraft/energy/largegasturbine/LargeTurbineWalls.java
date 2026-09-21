package com.masson.cruciblecraft.energy.largegasturbine;

import java.util.Map;

import com.masson.cruciblecraft.content.mte.MteInPlaceSpec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * GT6 {@code MultiTileEntityLargeTurbine.checkStructure2} design 3 on the
 * far-wall energy-out cell. Dense walls 18022/18026/18023/18025 share
 * {@code metalwalldense}.
 */
public final class LargeTurbineWalls {
    public static final BooleanProperty OUTLET = BooleanProperty.create("outlet");

    private static final Map<String, String> TINT = Map.of(
            "multiblock/dense_stainless_steel_wall", "stainless_steel",
            "multiblock/dense_titanium_wall", "titanium",
            "multiblock/dense_tungstensteel_wall", "tungstensteel",
            "multiblock/dense_adamantium_wall", "adamantium");

    private LargeTurbineWalls() {}

    public static boolean usesOutletState(MteInPlaceSpec spec) {
        return spec != null && TINT.containsKey(spec.registryPath());
    }

    public static String tintMaterial(String path) {
        return path == null ? null : TINT.get(path);
    }

    public static void applyOutlet(Level level, BlockPos pos, boolean outlet) {
        if (level == null || pos == null) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(OUTLET) || state.getValue(OUTLET) == outlet) {
            return;
        }
        level.setBlock(pos, state.setValue(OUTLET, outlet), Block.UPDATE_CLIENTS);
    }

    public static boolean outlet(BlockState state) {
        return state.hasProperty(OUTLET) && state.getValue(OUTLET);
    }
}
