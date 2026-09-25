package com.masson.cruciblecraft.recipe.gt;

import java.util.ArrayList;
import java.util.List;

import com.masson.cruciblecraft.CrucibleCraft;

import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * T41 assembler-wood tokens are single oak planks shaped as crafting
 * recipes. That grid is the GT6 wood small fluid pipe, so the tokens
 * must not stay in the crafting map.
 */
public final class PipeCraftingGridGuard {
    private static final String TOKEN_PREFIX = "player_path_support/assembler_wood/";

    private PipeCraftingGridGuard() {}

    private static boolean isPipeAcquisition(RecipeHolder<?> holder) {
        return CrucibleCraft.MODID.equals(holder.id().getNamespace())
                && holder.id().getPath().startsWith("pipe_acquisition/")
                && holder.value().getType() == RecipeType.CRAFTING;
    }

    public static void dropTokenRecipes(RecipeManager manager) {
        List<RecipeHolder<?>> kept = new ArrayList<>();
        boolean removed = false;
        for (RecipeHolder<?> holder : manager.getOrderedRecipes()) {
            if (CrucibleCraft.MODID.equals(holder.id().getNamespace())
                    && holder.id().getPath().startsWith(TOKEN_PREFIX)
                    && holder.value().getType() == RecipeType.CRAFTING) {
                removed = true;
                continue;
            }
            kept.add(holder);
        }
        if (removed) {
            kept.sort((left, right) -> Boolean.compare(
                    !isPipeAcquisition(left),
                    !isPipeAcquisition(right)));
            manager.replaceRecipes(kept);
        }
    }
}
