package com.masson.cruciblecraft.crops;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CropCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(
                    net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB,
                    CrucibleCraftCrops.MODID);

    static {
        TABS.register(
                "crops",
                () -> CreativeModeTab.builder()
                        .title(Component.translatable("itemGroup.cruciblecraft_crops.crops"))
                        .icon(() -> CropRegistries.CROP_STICK_ITEM.get().getDefaultInstance())
                        .displayItems((parameters, output) ->
                                output.accept(CropRegistries.CROP_STICK_ITEM.get()))
                        .build());
    }

    private CropCreativeTabs() {}
}
