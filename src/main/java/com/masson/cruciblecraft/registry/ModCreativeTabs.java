package com.masson.cruciblecraft.registry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.machine.MachineDurabilityComponent;
import com.masson.cruciblecraft.machine.MachineMaterialRules;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CrucibleCraft.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = CREATIVE_MODE_TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.cruciblecraft"))
                    .icon(() -> ModItems.FIREBRICK.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.FIREBRICK.get());
                        output.accept(ModItems.FIREBOX.get());
                        output.accept(machineVariant(ModItems.CRUCIBLE.get(), "ceramic"));
                        output.accept(machineVariant(ModItems.CRUCIBLE.get(), "bronze"));
                        output.accept(machineVariant(ModItems.CRUCIBLE.get(), "steel"));
                        output.accept(machineVariant(ModItems.ANVIL.get(), "stone"));
                        output.accept(machineVariant(ModItems.ANVIL.get(), "iron"));
                        output.accept(machineVariant(ModItems.ANVIL.get(), "bronze"));
                        output.accept(machineVariant(ModItems.ANVIL.get(), "steel"));
                        output.accept(ModItems.COKE_OVEN.get());
                        output.accept(ModItems.RAW_CERAMIC_CRUCIBLE.get());
                        output.accept(ModItems.RAW_CERAMIC_MOLD.get());
                        output.accept(ModItems.RAW_INGOT_MOLD.get());
                        output.accept(ModItems.RAW_PLATE_MOLD.get());
                        output.accept(ModItems.RAW_ROD_MOLD.get());
                        output.accept(ModItems.RAW_BOLT_MOLD.get());
                        output.accept(ModItems.INGOT_MOLD.get());
                        output.accept(ModItems.PLATE_MOLD.get());
                        output.accept(ModItems.ROD_MOLD.get());
                        output.accept(ModItems.BOLT_MOLD.get());
                        output.accept(ModItems.COAL_COKE.get());
                        output.accept(ModItems.BELLOWS.get());
                        output.accept(ModItems.CREOSOTE_BUCKET.get());
                        output.accept(ModItems.STEAM_BUCKET.get());
                        output.accept(ModItems.BRONZE_BOILER.get());
                        output.accept(ModItems.BRONZE_STEAM_ENGINE.get());
                        output.accept(ModItems.BRONZE_CRUSHER.get());
                        output.accept(ModItems.SLUICE.get());
                        output.accept(ModItems.BATH.get());
                        output.accept(ModItems.CENTRIFUGE.get());
                        output.accept(ModItems.SHREDDER.get());
                        output.accept(ModItems.SIFTER.get());
                        output.accept(ModItems.SMELTER.get());
                        output.accept(ModItems.MORTAR.get());
                        output.accept(ModItems.EXTRUDER.get());
                        output.accept(ModItems.CUTTER.get());
                        output.accept(ModItems.LATHE.get());
                        output.accept(ModItems.ROLLINGMILL.get());
                        output.accept(ModItems.ROLLBENDER.get());
                        output.accept(ModItems.WIREMILL.get());
                        output.accept(ModItems.BENDER.get());
                        output.accept(ModItems.ASSEMBLER.get());
                        output.accept(ModItems.WELDER.get());
                        output.accept(ModItems.PRESS.get());
                        output.accept(ModItems.SMITHING_HAMMER.get().variant("bronze"));
                        output.accept(ModItems.SMITHING_HAMMER.get().variant("iron"));
                        output.accept(ModItems.SMITHING_HAMMER.get().variant("steel"));
                    })
                    .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ORES =
            materialTab(
                    MaterialCreativeTab.ORES,
                    () -> ModItems.BRONZE_CRUSHER.get().getDefaultInstance());
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ORE_PROCESSING =
            materialTab(
                    MaterialCreativeTab.ORE_PROCESSING,
                    () -> ModItems.SLUICE.get().getDefaultInstance());
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> DUSTS =
            materialTab(
                    MaterialCreativeTab.DUSTS,
                    () -> ModItems.MORTAR.get().getDefaultInstance());
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> METALS_GEMS =
            materialTab(
                    MaterialCreativeTab.METALS_GEMS,
                    () -> machineVariant(ModItems.ANVIL.get(), "iron"));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PLATES =
            materialTab(
                    MaterialCreativeTab.PLATES,
                    () -> ModItems.PLATE_MOLD.get().getDefaultInstance());
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PARTS =
            materialTab(
                    MaterialCreativeTab.PARTS,
                    () -> ModItems.LATHE.get().getDefaultInstance());
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MECHANICAL_PARTS =
            materialTab(
                    MaterialCreativeTab.MECHANICAL_PARTS,
                    () -> ModItems.ROLLBENDER.get().getDefaultInstance());
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> WIRES =
            materialTab(
                    MaterialCreativeTab.WIRES,
                    () -> ModItems.WIREMILL.get().getDefaultInstance());
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CABLES =
            materialTab(
                    MaterialCreativeTab.CABLES,
                    () -> ModItems.ASSEMBLER.get().getDefaultInstance());
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MISC =
            materialTab(
                    MaterialCreativeTab.MISC,
                    () -> ModItems.UNKNOWN_MATERIAL.get().getDefaultInstance());

    private static DeferredHolder<CreativeModeTab, CreativeModeTab> materialTab(
            MaterialCreativeTab tab,
            Supplier<ItemStack> icon) {
        return CREATIVE_MODE_TABS.register(
                tab.registryName(),
                () -> CreativeModeTab.builder()
                        .title(Component.translatable(tab.translationKey()))
                        .icon(icon)
                        .displayItems((parameters, output) ->
                                materialEntryPlan().get(tab).forEach(itemId ->
                                        output.accept(requirePlannedItem(itemId))))
                        .build());
    }

    private static Map<MaterialCreativeTab, List<String>> materialEntryPlan() {
        var materials = MaterialCatalog.startupValues();
        Map<String, List<MaterialPrefix>> registeredForms = new LinkedHashMap<>();
        materials.forEach(material -> registeredForms.put(
                material.id(),
                MaterialCatalog.registeredForms(material)));
        return MaterialCreativeTab.planEntryIds(
                materials,
                registeredForms,
                MaterialCatalog.runtimePreferences());
    }

    static Item requirePlannedItem(String itemId) {
        ResourceLocation location = ResourceLocation.tryParse(itemId);
        if (location != null) {
            var item = BuiltInRegistries.ITEM.getOptional(location);
            if (item.isPresent()) {
                return item.orElseThrow();
            }
        }
        CrucibleCraft.LOGGER.error(
                "Material creative-tab plan references missing item {}", itemId);
        throw new IllegalStateException(
                "Material creative-tab plan references missing item " + itemId);
    }

    private static ItemStack machineVariant(net.minecraft.world.item.Item item, String materialId) {
        ItemStack stack = new ItemStack(item);
        stack.set(ModComponents.MACHINE_MATERIAL, materialId);
        if (item == ModItems.ANVIL.get()) {
            long max = MachineMaterialRules.anvilMaxDurability(materialId);
            stack.set(ModComponents.MACHINE_DURABILITY, new MachineDurabilityComponent(max, max));
        }
        return stack;
    }

    private ModCreativeTabs() {}
}
