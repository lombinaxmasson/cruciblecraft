package com.masson.cruciblecraft.logistics.pipe.cover;

import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredItem;

/** Maps a persisted cover definition to the player-facing cover item. */
public final class PipeCoverItems {
    private PipeCoverItems() {}

    public static ItemStack stackFor(PipeCover cover) {
        if (cover == null) {
            return ItemStack.EMPTY;
        }
        DeferredItem<?> item = itemFor(cover.definitionId());
        return item == null ? ItemStack.EMPTY : new ItemStack(item.get());
    }

    public static DeferredItem<?> itemFor(ResourceLocation definitionId) {
        if (definitionId == null
                || !"cruciblecraft".equals(definitionId.getNamespace())) {
            return null;
        }
        return switch (definitionId.getPath()) {
            case "filter" -> ModItems.PIPE_FILTER_COVER;
            case "shutter" -> ModItems.PIPE_VALVE_COVER;
            case "pump" -> ModItems.PIPE_PUMP_COVER;
            case "conveyor", "conveyor_fast" -> ModItems.CONVEYOR_COVER;
            case "retriever_item" -> ModItems.RETRIEVER_ITEM_COVER;
            case "robot_arm" -> ModItems.ROBOT_ARM_COVER;
            case "pressure_valve" -> ModItems.PRESSURE_VALVE_COVER;
            case "selector_manual" -> ModItems.SELECTOR_MANUAL_COVER;
            case "logistics_item_storage" ->
                    ModItems.LOGISTICS_ITEM_STORAGE_COVER;
            case "logistics_item_import" ->
                    ModItems.LOGISTICS_ITEM_IMPORT_COVER;
            case "logistics_item_export" ->
                    ModItems.LOGISTICS_ITEM_EXPORT_COVER;
            case "logistics_fluid_storage" ->
                    ModItems.LOGISTICS_FLUID_STORAGE_COVER;
            case "logistics_fluid_import" ->
                    ModItems.LOGISTICS_FLUID_IMPORT_COVER;
            case "logistics_fluid_export" ->
                    ModItems.LOGISTICS_FLUID_EXPORT_COVER;
            default -> null;
        };
    }
}
