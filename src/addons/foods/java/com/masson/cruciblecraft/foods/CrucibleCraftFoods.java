package com.masson.cruciblecraft.foods;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(CrucibleCraftFoods.MODID)
public final class CrucibleCraftFoods {
    public static final String MODID = "cruciblecraft_foods";

    public CrucibleCraftFoods(IEventBus modEventBus) {
        FoodItems.register(modEventBus);
    }
}
