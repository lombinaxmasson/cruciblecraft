package com.masson.cruciblecraft.api.unit;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialForm;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.content.item.MaterialItem;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.MaterialDefinition;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

@EventBusSubscriber(modid = CrucibleCraft.MODID)
public final class MaterialUnits {
    private static volatile Map<Item, Entry> externalItems = Map.of();

    private MaterialUnits() {}

    public static Optional<Entry> resolve(ItemStack stack) {
        if (stack.getItem() instanceof MaterialItem materialItem) {
            return Optional.of(new Entry(
                    materialItem.material(),
                    materialItem.form(),
                    materialItem.units()));
        }
        for (MaterialDefinition material : MaterialCatalog.values()) {
            for (var override : material.formItems().entrySet()) {
                if (MaterialLookup.item(material.id(), override.getKey())
                        .filter(item -> item == stack.getItem())
                        .isPresent()) {
                    return Optional.of(new Entry(
                            material,
                            override.getKey(),
                            override.getKey().units()));
                }
            }
        }
        return Optional.ofNullable(externalItems.get(stack.getItem()));
    }

    @SubscribeEvent
    public static void tagsUpdated(TagsUpdatedEvent event) {
        IdentityHashMap<Item, Entry> rebuilt = new IdentityHashMap<>();
        for (MaterialDefinition material : MaterialCatalog.values()) {
            for (MaterialForm form : material.forms()) {
                TagKey<Item> tag = TagKey.create(
                        Registries.ITEM,
                        ResourceLocation.fromNamespaceAndPath(
                                "c",
                                tagPath(form) + "/" + material.tagName()));
                BuiltInRegistries.ITEM.getTag(tag).ifPresent(holders ->
                        holders.forEach(holder -> {
                            if (!(holder.value() instanceof MaterialItem)) {
                                rebuilt.putIfAbsent(
                                        holder.value(),
                                        new Entry(material, form, form.units()));
                            }
                        }));
            }
        }
        externalItems = Map.copyOf(rebuilt);
    }

    private static String tagPath(MaterialForm form) {
        return switch (form) {
            case SMALL_DUST -> "small_dusts";
            default -> form.serializedName() + "s";
        };
    }

    public record Entry(MaterialDefinition material, MaterialForm form, int units) {}
}
