package com.masson.cruciblecraft.crops;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(CrucibleCraftCrops.MODID)
public final class CrucibleCraftCrops {
    public static final String MODID = "cruciblecraft_crops";

    public CrucibleCraftCrops(IEventBus modEventBus) {
        CropRegistries.register(modEventBus);
        CropCatalog.installOverlay();
        if (!CropCatalog.overlayInstalled()) {
            throw new IllegalStateException("Crop named plant-form overlay failed");
        }
    }
}
