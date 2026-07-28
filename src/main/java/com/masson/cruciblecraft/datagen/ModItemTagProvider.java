package com.masson.cruciblecraft.datagen;

import java.util.concurrent.CompletableFuture;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialForm;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.material.MaterialCatalog;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public final class ModItemTagProvider extends ItemTagsProvider {
    public ModItemTagProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            ExistingFileHelper existingFileHelper) {
        super(
                output,
                lookupProvider,
                CompletableFuture.completedFuture(TagsProvider.TagLookup.empty()),
                CrucibleCraft.MODID,
                existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        for (var material : MaterialCatalog.values()) {
            // This is a semantic material tag, not a "CrucibleCraft-owned
            // items" tag: canonical form_items such as vanilla ingots belong
            // here intentionally.
            var materialTag = tag(itemTag(
                    CrucibleCraft.MODID,
                    "materials/" + material.id()));
            for (MaterialForm form : material.forms()) {
                ResourceLocation itemId = MaterialLookup.itemId(material.id(), form).orElseThrow();
                TagKey<Item> specific = itemTag(
                        "c",
                        tagDirectory(form) + "/" + material.tagName());
                tag(specific).addOptional(itemId);
                tag(itemTag("c", tagDirectory(form))).addTag(specific);
                materialTag.addOptional(itemId);
            }
        }
    }

    private static TagKey<Item> itemTag(String namespace, String path) {
        return TagKey.create(
                Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    private static String tagDirectory(MaterialForm form) {
        return switch (form) {
            case SMALL_DUST -> "small_dusts";
            default -> form.serializedName() + "s";
        };
    }
}
