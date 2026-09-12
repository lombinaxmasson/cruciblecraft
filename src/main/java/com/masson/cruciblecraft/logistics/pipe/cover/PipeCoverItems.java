package com.masson.cruciblecraft.logistics.pipe.cover;

import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverKinds;
import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredItem;

/** Maps a persisted cover definition to the player-facing cover item. */
public final class PipeCoverItems {
    private PipeCoverItems() {}

    public static ItemStack stackFor(PipeCover cover) {
        if (cover == null) {
            return ItemStack.EMPTY;
        }
        String path = cover.definitionId().getPath();
        if ("selector_tag".equals(path)) {
            ItemStack stack = new ItemStack(ModItems.PROGRAMMED_CIRCUIT.get());
            stack.set(
                    ModComponents.CIRCUIT_CONFIG.get(),
                    Math.max(1, Math.min(
                            com.masson.cruciblecraft.content.item.ProgrammedCircuitItem.MAX_CONFIG,
                            cover.config().redstone() + 1)));
            return stack;
        }
        if ("redstone_torch".equals(path)) {
            return new ItemStack(Items.REDSTONE_TORCH);
        }
        if ("redstone_repeater".equals(path)) {
            return new ItemStack(Items.REPEATER);
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
            case "logistics_generic_storage" ->
                    ModItems.LOGISTICS_GENERIC_STORAGE_COVER;
            case "logistics_generic_import" ->
                    ModItems.LOGISTICS_GENERIC_IMPORT_COVER;
            case "logistics_generic_export" ->
                    ModItems.LOGISTICS_GENERIC_EXPORT_COVER;
            case "logistics_generic_dump" ->
                    ModItems.LOGISTICS_GENERIC_DUMP_COVER;
            case "logistics_display_cpu_logic" ->
                    ModItems.LOGISTICS_DISPLAY_CPU_LOGIC_COVER;
            case "logistics_display_cpu_control" ->
                    ModItems.LOGISTICS_DISPLAY_CPU_CONTROL_COVER;
            case "logistics_display_cpu_storage" ->
                    ModItems.LOGISTICS_DISPLAY_CPU_STORAGE_COVER;
            case "logistics_display_cpu_conversion" ->
                    ModItems.LOGISTICS_DISPLAY_CPU_CONVERSION_COVER;
            default -> {
                var compact = CoverComponentTiers.findByDefinition(definitionId)
                        .map(entry -> ModItems.compactElectricCover(
                                entry.itemPath()));
                if (compact.isPresent()) {
                    yield compact.orElseThrow();
                }
                yield MachineCoverKinds.itemFor(definitionId);
            }
        };
    }
}
