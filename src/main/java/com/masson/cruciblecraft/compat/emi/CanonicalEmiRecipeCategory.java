package com.masson.cruciblecraft.compat.emi;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiRenderable;
import net.minecraft.resources.ResourceLocation;

/**
 * EMI's {@link EmiRecipeCategory} has identity {@code equals}/{@code hashCode}.
 * {@code List.contains} then treats a second instance with the same id as
 * unregistered and logs every recipe in that category.
 */
final class CanonicalEmiRecipeCategory extends EmiRecipeCategory {
    CanonicalEmiRecipeCategory(ResourceLocation id, EmiRenderable icon) {
        super(id, icon);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof EmiRecipeCategory category
                && getId().equals(category.getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
