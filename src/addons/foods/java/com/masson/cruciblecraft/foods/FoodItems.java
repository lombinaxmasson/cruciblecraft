package com.masson.cruciblecraft.foods;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class FoodItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(CrucibleCraftFoods.MODID);
    private static final Map<String, DeferredItem<FoodItem>> BY_PATH = new LinkedHashMap<>();

    static {
        for (FoodIdentity identity : FoodCatalog.items()) {
            if (!identity.registersItem()) {
                continue;
            }
            BY_PATH.put(
                    identity.path(),
                    ITEMS.register(identity.path(), () -> new FoodItem(identity)));
        }
    }

    private FoodItems() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        FoodCreativeTabs.TABS.register(bus);
    }

    public static Item require(String path) {
        DeferredItem<FoodItem> item = BY_PATH.get(path);
        if (item == null) {
            throw new IllegalArgumentException("Unknown food item " + path);
        }
        return item.get();
    }

    static Iterable<DeferredItem<FoodItem>> registered() {
        return BY_PATH.values();
    }
}
