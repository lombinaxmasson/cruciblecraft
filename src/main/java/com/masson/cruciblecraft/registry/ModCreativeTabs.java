package com.masson.cruciblecraft.registry;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.machine.MachineDurabilityComponent;
import com.masson.cruciblecraft.machine.MachineMaterialRules;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
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
                        output.accept(ModItems.SMITHING_HAMMER.get().variant("bronze"));
                        output.accept(ModItems.SMITHING_HAMMER.get().variant("iron"));
                        output.accept(ModItems.SMITHING_HAMMER.get().variant("steel"));
                        java.util.List.of(
                                ModItems.COPPER_ORE, ModItems.DEEPSLATE_COPPER_ORE,
                                ModItems.TIN_ORE, ModItems.DEEPSLATE_TIN_ORE,
                                ModItems.IRON_ORE, ModItems.DEEPSLATE_IRON_ORE,
                                ModItems.GOLD_ORE, ModItems.DEEPSLATE_GOLD_ORE,
                                ModItems.ZINC_ORE, ModItems.DEEPSLATE_ZINC_ORE,
                                ModItems.LEAD_ORE, ModItems.DEEPSLATE_LEAD_ORE,
                                ModItems.NICKEL_ORE, ModItems.DEEPSLATE_NICKEL_ORE)
                                .forEach(item -> output.accept(item.get()));
                        var materialItems = java.util.Collections.newSetFromMap(
                                new java.util.IdentityHashMap<net.minecraft.world.item.Item, Boolean>());
                        MaterialCatalog.values().forEach(material -> material.forms().forEach(form ->
                                MaterialLookup.item(material.id(), form).ifPresent(item -> {
                                    if (materialItems.add(item)) {
                                        output.accept(item);
                                    }
                                })));
                    })
                    .build());

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
