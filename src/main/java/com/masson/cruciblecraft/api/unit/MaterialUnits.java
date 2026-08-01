package com.masson.cruciblecraft.api.unit;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.content.item.MaterialItem;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.MaterialDefinition;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
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
                    materialItem.materialId(),
                    materialItem.form(),
                    materialItem.units()));
        }
        return Optional.ofNullable(externalItems.get(stack.getItem()));
    }

    @SubscribeEvent
    public static void tagsUpdated(TagsUpdatedEvent event) {
        // In integrated play the server and client share this static map, so the
        // server rebuilds it and NeoForge suppresses the duplicate client update.
        if (!event.shouldUpdateStaticData()) {
            CrucibleCraft.LOGGER.info(
                    "Material unit index kept after tag update (cause {}, static data shared; {} entries)",
                    event.getUpdateCause(),
                    externalItems.size());
            return;
        }
        IdentityHashMap<Item, Entry> rebuilt = new IdentityHashMap<>();
        var itemRegistry = event.getRegistryAccess().lookupOrThrow(Registries.ITEM);
        for (MaterialDefinition material : MaterialCatalog.values()) {
            for (var override : material.formItems().entrySet()) {
                itemRegistry.get(ResourceKey.create(
                                Registries.ITEM,
                                ResourceLocation.parse(override.getValue())))
                        .map(holder -> holder.value())
                        .filter(item -> !(item instanceof MaterialItem))
                        .ifPresent(item -> rebuilt.putIfAbsent(
                                item,
                                new Entry(material.id(), override.getKey(), override.getKey().units())));
            }
            for (MaterialPrefix form : MaterialCatalog.registeredForms(material)) {
                TagKey<Item> tag = TagKey.create(
                        Registries.ITEM,
                        ResourceLocation.fromNamespaceAndPath(
                                form.tagNamespace(),
                                form.tagDirectory() + "/" + material.tagName()));
                itemRegistry.get(tag).ifPresent(holders ->
                        holders.forEach(holder -> {
                            if (!(holder.value() instanceof MaterialItem)) {
                                rebuilt.putIfAbsent(
                                        holder.value(),
                                        new Entry(material.id(), form, form.units()));
                            }
                        }));
            }
        }
        externalItems = Map.copyOf(rebuilt);
        CrucibleCraft.LOGGER.info(
                "Material unit index rebuilt after tag update (cause {}, {} entries)",
                event.getUpdateCause(),
                externalItems.size());
    }

    public record Entry(String materialId, MaterialPrefix form, int units) {
        public MaterialDefinition material() {
            return MaterialCatalog.require(materialId);
        }
    }
}
