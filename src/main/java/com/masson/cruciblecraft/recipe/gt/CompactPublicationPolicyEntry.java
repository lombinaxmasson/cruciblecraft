package com.masson.cruciblecraft.recipe.gt;

import java.util.Objects;

import com.masson.cruciblecraft.registry.ModRecipes;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/** Vanilla datapack envelope around a compact publication policy. */
public record CompactPublicationPolicyEntry(CompactPublicationPolicyDefinition definition)
        implements Recipe<RecipeInput> {
    public static final MapCodec<CompactPublicationPolicyEntry> CODEC =
            CompactPublicationPolicyDefinition.MAP_CODEC.xmap(
                    CompactPublicationPolicyEntry::new,
                    CompactPublicationPolicyEntry::definition);

    public CompactPublicationPolicyEntry {
        Objects.requireNonNull(definition, "definition");
    }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.COMPACT_PUBLICATION_POLICY_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.COMPACT_PUBLICATION_POLICY_TYPE.get();
    }
}
