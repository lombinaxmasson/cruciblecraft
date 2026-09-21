package com.masson.cruciblecraft.foods;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class FoodCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(
                    net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB,
                    CrucibleCraftFoods.MODID);

    static {
        TABS.register(
                "foods",
                () -> CreativeModeTab.builder()
                        .title(Component.translatable("itemGroup.cruciblecraft_foods.foods"))
                        .icon(() -> FoodItems.require("tomato").getDefaultInstance())
                        .displayItems((parameters, output) -> {
                            for (var item : FoodItems.registered()) {
                                output.accept(item.get());
                            }
                        })
                        .build());
    }

    private FoodCreativeTabs() {}
}
