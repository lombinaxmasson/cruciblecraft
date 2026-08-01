package com.masson.cruciblecraft.recipe.rule;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class MaterialRuleSerializer implements RecipeSerializer<MaterialRuleRecipe> {
    private static final StreamCodec<RegistryFriendlyByteBuf, MaterialRuleRecipe> STREAM_CODEC =
            MaterialRule.STREAM_CODEC.map(MaterialRuleRecipe::new, MaterialRuleRecipe::rule);

    @Override
    public com.mojang.serialization.MapCodec<MaterialRuleRecipe> codec() {
        return MaterialRule.CODEC.xmap(MaterialRuleRecipe::new, MaterialRuleRecipe::rule);
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, MaterialRuleRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
