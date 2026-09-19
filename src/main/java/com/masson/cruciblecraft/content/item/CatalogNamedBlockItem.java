package com.masson.cruciblecraft.content.item;

import java.util.Objects;

import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.mte.MteInPlaceDisplayNames;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** Block item whose catalog names survive a stale datagen language file. */
public final class CatalogNamedBlockItem extends BlockItem {
    private final String englishName;
    private final String chineseName;

    public CatalogNamedBlockItem(
            Block block,
            Properties properties,
            String englishName,
            String chineseName) {
        super(block, properties);
        this.englishName = Objects.requireNonNull(englishName, "englishName");
        this.chineseName = Objects.requireNonNull(chineseName, "chineseName");
    }

    @Override
    public Component getName(ItemStack stack) {
        if (!(getBlock() instanceof MteInPlaceBlock inplace)) {
            return CatalogDisplayNames.itemName(
                    getDescriptionId(stack), englishName, chineseName);
        }
        String path = inplace.spec().registryPath();
        return CatalogDisplayNames.itemName(
                getDescriptionId(stack),
                MteInPlaceDisplayNames.english(englishName, path),
                chineseName,
                CatalogDisplayNames.composedChinese(path));
    }
}
