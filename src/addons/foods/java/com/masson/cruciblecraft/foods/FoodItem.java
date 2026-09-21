package com.masson.cruciblecraft.foods;

import com.masson.cruciblecraft.api.food.FoodStat;

import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class FoodItem extends Item {
    private final FoodIdentity identity;

    public FoodItem(FoodIdentity identity) {
        super(properties(identity.stat()));
        this.identity = identity;
    }

    public FoodIdentity identity() {
        return identity;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId());
    }

    private static Properties properties(FoodStat stat) {
        Properties properties = new Properties();
        if (stat.nutrition() <= 0) {
            return properties;
        }
        FoodProperties.Builder food = new FoodProperties.Builder()
                .nutrition(stat.nutrition())
                .saturationModifier(stat.saturation());
        if (stat.alwaysEdible()) {
            food.alwaysEdible();
        }
        return properties.food(food.build());
    }
}
