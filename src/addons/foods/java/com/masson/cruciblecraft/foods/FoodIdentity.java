package com.masson.cruciblecraft.foods;

import java.util.List;

import com.masson.cruciblecraft.api.food.FoodStat;

public record FoodIdentity(
        String path,
        String il,
        String english,
        String chinese,
        int nutrition,
        float saturation,
        boolean alwaysEdible,
        List<String> traits,
        String reuseCanonical) {
    public boolean registersItem() {
        return reuseCanonical == null || reuseCanonical.isBlank();
    }

    public FoodStat stat() {
        return new FoodStat(Math.max(0, nutrition), Math.max(0.0F, saturation), alwaysEdible);
    }
}
