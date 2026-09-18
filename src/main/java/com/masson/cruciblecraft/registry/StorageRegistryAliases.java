package com.masson.cruciblecraft.registry;

import com.masson.cruciblecraft.CrucibleCraft;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Old T44 {@code mass_storage_barrel_*} / {@code mass_storage_box_*} ids become
 * Item Barrel / Plastic Storage Box. Worlds keep the placed blocks.
 */
@EventBusSubscriber(modid = CrucibleCraft.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class StorageRegistryAliases {
    private static final String[] BARRELS = {
            "6983", "6984", "6985", "6986", "6987", "6988", "6989",
            "6990", "6991", "6992", "6997", "6998", "6999"
    };
    private static final String[] BOXES = {"6993", "6994", "6995", "6996"};

    private StorageRegistryAliases() {}

    @SubscribeEvent
    public static void addAliases(RegisterEvent event) {
        aliasRegistry(event.getRegistry(Registries.BLOCK));
        aliasRegistry(event.getRegistry(Registries.ITEM));
    }

    private static void aliasRegistry(Registry<?> registry) {
        if (registry == null) {
            return;
        }
        for (String meta : BARRELS) {
            registry.addAlias(
                    id("mass_storage_barrel_" + meta),
                    id("item_barrel_" + meta));
        }
        for (String meta : BOXES) {
            registry.addAlias(
                    id("mass_storage_box_" + meta),
                    id("plastic_storage_box_" + meta));
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, path);
    }
}
