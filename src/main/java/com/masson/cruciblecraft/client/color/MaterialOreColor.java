package com.masson.cruciblecraft.client.color;

import java.util.Optional;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ore-fleck tint for per-material ore blocks (tintindex 0 only). The stone
 * base stays untinted; the fleck overlay takes the material's styled ingot
 * color so ore blocks read like their ingots at a glance.
 */
public final class MaterialOreColor {
    private MaterialOreColor() {}

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

    public static Block[] oreBlocks() {
        return ModBlocks.oreBlocks().stream()
                .map(holder -> (Block) holder.get())
                .toArray(Block[]::new);
    }

    public static Item[] oreBlockItems() {
        return ModItems.oreItems().stream()
                .map(holder -> (Item) holder.get())
                .toArray(Item[]::new);
    }

    /** Registry path ({@code copper_ore} / {@code deepslate_copper_ore}) to material id. */
    static Optional<String> materialIdFor(String registryPath) {
        if (!registryPath.endsWith("_ore")) {
            return Optional.empty();
        }
        String materialId =
                registryPath.substring(0, registryPath.length() - "_ore".length());
        if (materialId.startsWith("deepslate_")) {
            materialId = materialId.substring("deepslate_".length());
        }
        return materialId.isEmpty() ? Optional.empty() : Optional.of(materialId);
    }

    private static int colorFor(Block block) {
        var id = BuiltInRegistries.BLOCK.getKey(block);
        if (id == null || !CrucibleCraft.MODID.equals(id.getNamespace())) {
            return 0xFFFFFFFF;
        }
        return materialIdFor(id.getPath())
                .flatMap(MaterialCatalog::find)
                .map(MaterialOreColor::colorFor)
                .orElse(0xFFFFFFFF);
    }

    static int colorFor(MaterialDefinition material) {
        return 0xFF000000
                | MaterialItemColor.styleColor(material.colorRgb(), material.tintStyle());
    }
}
