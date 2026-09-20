package com.masson.cruciblecraft.datagen;

import java.util.concurrent.CompletableFuture;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.block.SmelteryHosts;
import com.masson.cruciblecraft.content.item.ExtruderShapeCatalog;
import com.masson.cruciblecraft.content.item.SemanticObjectCatalog;
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
        SemanticObjectCatalog.identities().stream()
                .filter(ExtruderShapeCatalog::isSimpleExIdentity)
                .forEach(identity -> {
                    var holder = ModItems.semanticIdentityItemsById()
                            .get(identity.id());
                    if (holder != null) {
                        shapes.add(holder.getKey());
                    }
                });
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
        var planks = tag(ItemTags.PLANKS);
        for (var species : com.masson.cruciblecraft.worldgen.tree.prep.GtTreeSpecies.ALL) {
            logs.add(ModItems.treeLogItem(species).getKey());
            logsThatBurn.add(ModItems.treeLogItem(species).getKey());
            leaves.add(ModItems.treeLeavesItem(species).getKey());
            saplings.add(ModItems.treeSaplingItem(species).getKey());
        }
        ModItems.gtWoods().forEach(holder -> {
            if (!holder.getId().getPath().endsWith("/crate")) {
                planks.add(holder.getKey());
            }
        });
        var woodenSlabs = tag(ItemTags.WOODEN_SLABS);
        ModItems.bathRemainderBlockObjectItems().forEach(holder -> {
            var variant = com.masson.cruciblecraft.content.item
                    .BathRemainderBlockObjectCatalog.require(holder.getId());
            if (!variant.treePlanks2()) {
                return;
            }
            if (variant.slab()) {
                woodenSlabs.add(holder.getKey());
            } else {
                planks.add(holder.getKey());
            }
        });
    }
}
