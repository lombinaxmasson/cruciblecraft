package com.masson.cruciblecraft.api.food;

/**
 * GT6 {@code gregapi.item.multiitem.food.FoodStat} subset used by the optional
 * Foods addon. Juicer/Fermenter hosts stay on their own cards.
 */
public record FoodStat(
        int nutrition,
        float saturation,
        boolean alwaysEdible) {
    public FoodStat {
        if (nutrition < 0 || !Float.isFinite(saturation) || saturation < 0.0F) {
            throw new IllegalArgumentException("FoodStat values must be finite and non-negative");
        }
    }
}
