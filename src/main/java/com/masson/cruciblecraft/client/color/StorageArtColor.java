package com.masson.cruciblecraft.client.color;

import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.block.StorageHostBlock;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
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
            case BARREL, BOOKSHELF, BOTTLE_CRATE, DRAWER, LOCKER, MASS_STORAGE -> true;
            default -> false;
        };
    }

    static String materialId(MteInPlaceSpec spec) {
        String path = spec.registryPath();
        if (path.contains("wooden_item_barrel_cheap")
                || "wooden_item_barrel_cheap".equals(path)) {
            return "wood";
        }
        if (path.endsWith("wooden_item_barrel")) {
            return "wood_treated";
        }
        int slash = path.indexOf('/');
        if (slash > 0) {
            String first = path.substring(0, slash);
            if (MaterialCatalog.contains(first)) {
                return first;
            }
            String elemental = first + "_elemental";
            if (MaterialCatalog.contains(elemental)) {
                return elemental;
            }
        }
        int under = path.lastIndexOf('_');
        if (under >= 0 && under < path.length() - 1) {
            String last = path.substring(under + 1);
            if (MaterialCatalog.contains(last)) {
                return last;
            }
            String elemental = last + "_elemental";
            if (MaterialCatalog.contains(elemental)) {
                return elemental;
            }
        }
        if (spec.kind() == MteInPlaceKind.BARREL
                || spec.kind() == MteInPlaceKind.BOOKSHELF
                || spec.kind() == MteInPlaceKind.BOTTLE_CRATE) {
            return "wood";
        }
        return "steel";
    }

    private static int colorFor(Block block) {
        String materialId = "steel";
        if (block instanceof StorageHostBlock storage) {
            materialId = storage.variant().tintMaterial();
        } else if (block instanceof MteInPlaceBlock inplace
                && storageKind(inplace.spec().kind())) {
            materialId = materialId(inplace.spec());
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
