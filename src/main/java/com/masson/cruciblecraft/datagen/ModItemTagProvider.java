package com.masson.cruciblecraft.datagen;

import java.util.concurrent.CompletableFuture;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.block.SmelteryHosts;
import com.masson.cruciblecraft.content.item.ExtruderShapeCatalog;
import com.masson.cruciblecraft.registry.ModItemTags;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public final class ModItemTagProvider extends TagsProvider<Item> {
    public ModItemTagProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            ExistingFileHelper existingFileHelper) {
        super(
                output,
                net.minecraft.core.registries.Registries.ITEM,
                lookupProvider,
                CrucibleCraft.MODID,
                existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        var shapes = tag(ModItemTags.EXTRUDER_SHAPES);
        ExtruderShapeCatalog.DEFINITIONS.forEach(shape ->
                shapes.add(ModItems.extruderShape(shape.id()).getKey()));
        var smeltingCrucibles = tag(ModItemTags.SMELTING_CRUCIBLES);
        ModItems.mteInPlaceItemsById().values().forEach(holder -> {
            if (holder.get().getBlock() instanceof MteInPlaceBlock inplace
                    && SmelteryHosts.isSmeltery(inplace.spec())) {
                smeltingCrucibles.add(holder.getKey());
            }
        });
        var logs = tag(ItemTags.LOGS);
        var logsThatBurn = tag(ItemTags.LOGS_THAT_BURN);
        var leaves = tag(ItemTags.LEAVES);
        var saplings = tag(ItemTags.SAPLINGS);
        for (var species : com.masson.cruciblecraft.worldgen.tree.prep.GtTreeSpecies.ALL) {
            logs.add(ModItems.treeLogItem(species).getKey());
            logsThatBurn.add(ModItems.treeLogItem(species).getKey());
            leaves.add(ModItems.treeLeavesItem(species).getKey());
            saplings.add(ModItems.treeSaplingItem(species).getKey());
        }
    }
}
