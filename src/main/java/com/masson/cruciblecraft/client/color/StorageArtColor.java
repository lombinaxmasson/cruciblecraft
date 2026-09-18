package com.masson.cruciblecraft.client.color;

import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.block.StorageHostBlock;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.content.mte.MteInPlaceMaterials;
import com.masson.cruciblecraft.content.mte.MteInPlaceSpec;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** tintindex 0 only; GT6 overlay faces stay white. */
public final class StorageArtColor {
    private StorageArtColor() {}

    public static int blockColor(
            BlockState state,
            net.minecraft.world.level.BlockAndTintGetter level,
            net.minecraft.core.BlockPos pos,
            int tintIndex) {
        if (tintIndex != 0) {
            return 0xFFFFFFFF;
        }
        return colorFor(state.getBlock());
    }

    public static int itemColor(ItemStack stack, int tintIndex) {
        if (tintIndex != 0) {
            return 0xFFFFFFFF;
        }
        return colorFor(Block.byItem(stack.getItem()));
    }

    public static Block[] tintedBlocks() {
        java.util.ArrayList<Block> blocks = new java.util.ArrayList<>();
        ModBlocks.variantStorageBlocks().forEach(holder -> blocks.add(holder.get()));
        ModBlocks.mteInPlaceBlocksById().values().forEach(holder -> {
            if (storageKind(holder.get().spec().kind())) {
                blocks.add(holder.get());
            }
        });
        return blocks.toArray(Block[]::new);
    }

    static boolean storageKind(MteInPlaceKind kind) {
        return switch (kind) {
            case BARREL, BOOKSHELF, BOTTLE_CRATE, DRAWER, LOCKER, MASS_STORAGE, CHEST, SAFE -> true;
            default -> false;
        };
    }

    public static int materialColor(MteInPlaceSpec spec) {
        return MaterialCatalog.find(MteInPlaceMaterials.id(spec))
                .map(material -> MaterialItemColor.styleColor(
                        material.colorRgb(), material.tintStyle()))
                .orElse(0x00CD7F32);
    }

    static String materialId(MteInPlaceSpec spec) {
        return MteInPlaceMaterials.id(spec);
    }

    private static int colorFor(Block block) {
        String materialId = "steel";
        if (block instanceof StorageHostBlock storage) {
            materialId = storage.variant().tintMaterial();
        } else if (block instanceof MteInPlaceBlock inplace
                && storageKind(inplace.spec().kind())) {
            materialId = MteInPlaceMaterials.id(inplace.spec());
        } else {
            return 0xFFFFFFFF;
        }
        return MaterialCatalog.find(materialId)
                .map(material -> 0xFF000000
                        | MaterialItemColor.styleColor(
                                material.colorRgb(), material.tintStyle()))
                .orElse(0xFFCD7F32);
    }
}
