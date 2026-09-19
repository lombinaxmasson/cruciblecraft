package com.masson.cruciblecraft.client.color;

import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.content.block.GtBushBlock;
import com.masson.cruciblecraft.content.blockentity.GtBushBlockEntity;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6 {@code MultiTileEntityBush} dual-layer tint: bush at tintindex 0, berries
 * at tintindex 1. Overlay faces stay white.
 */
public final class GtBushColor {
    private static final int EMPTY_BUSH = 0xFFFF00FF;
    private static final int PLANT_BERRY_BUSH = 0xFF009000;
    private static final int PLANT_BERRY_STAGE1 = 0xFFFF9090;
    private static final int PLANT_BERRY_STAGE2 = 0xFF80FF80;
    /** BushesGT default leaf green when the berry is not a plantGtBerry. */
    private static final int DEFAULT_BUSH = 0xFF22CC22;
    private static final int SWEET_STAGE1 = 0xFFFFCCCC;
    private static final int SWEET_STAGE2 = 0xFF664444;
    private static final int SWEET_RIPE = 0xFFC41E3A;
    private static final int GLOW_STAGE1 = 0xFFFFE080;
    private static final int GLOW_STAGE2 = 0xFFFFCC66;
    private static final int GLOW_RIPE = 0xFFFFCC33;

    private GtBushColor() {}

    public static int blockColor(
            BlockState state,
            BlockAndTintGetter level,
            BlockPos pos,
            int tintIndex) {
        if (tintIndex != 0 && tintIndex != 1) {
            return 0xFFFFFFFF;
        }
        ItemStack berry = GtBushBlockEntity.DEFAULT_BERRY;
        int stage = state.getValue(GtBushBlock.STAGE);
        if (level != null && pos != null
                && level.getBlockEntity(pos) instanceof GtBushBlockEntity bush) {
            berry = bush.berry();
            stage = bush.stage();
        }
        return color(berry, stage, tintIndex);
    }

    public static int itemColor(ItemStack stack, int tintIndex) {
        if (tintIndex != 0 && tintIndex != 1) {
            return 0xFFFFFFFF;
        }
        return color(GtBushBlockEntity.DEFAULT_BERRY, GtBushBlock.MAX_STAGE, tintIndex);
    }

    public static Block[] tintedBlocks() {
        return new Block[] {ModBlocks.GT_BUSH.get()};
    }

    static int color(ItemStack berry, int stage, int tintIndex) {
        if (berry == null || berry.isEmpty()) {
            return tintIndex == 0 ? EMPTY_BUSH : 0xFFFFFFFF;
        }
        var units = MaterialUnits.resolve(berry);
        if (units.isPresent()
                && "cruciblecraft:plant_gt_berry".equals(units.get().form().id())) {
            if (tintIndex == 0) {
                return PLANT_BERRY_BUSH;
            }
            return switch (stage) {
                case 1 -> PLANT_BERRY_STAGE1;
                case 2 -> PLANT_BERRY_STAGE2;
                case 3 -> MaterialCatalog.find(units.get().materialId())
                        .map(material -> 0xFF000000 | material.colorRgb())
                        .orElse(PLANT_BERRY_STAGE2);
                default -> 0xFFFFFFFF;
            };
        }
        int bush;
        int bloom;
        int immature;
        int ripe;
        if (berry.is(Items.GLOW_BERRIES)) {
            bush = DEFAULT_BUSH;
            bloom = GLOW_STAGE1;
            immature = GLOW_STAGE2;
            ripe = GLOW_RIPE;
        } else {
            bush = DEFAULT_BUSH;
            bloom = SWEET_STAGE1;
            immature = SWEET_STAGE2;
            ripe = SWEET_RIPE;
        }
        if (tintIndex == 0) {
            return bush;
        }
        return switch (stage) {
            case 1 -> bloom;
            case 2 -> immature;
            case 3 -> ripe;
            default -> 0xFFFFFFFF;
        };
    }
}
