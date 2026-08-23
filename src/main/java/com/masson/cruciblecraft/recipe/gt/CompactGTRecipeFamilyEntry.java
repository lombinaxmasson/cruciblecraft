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

/** Vanilla datapack envelope around a compact GT recipe family definition. */
public record CompactGTRecipeFamilyEntry(CompactGTRecipeFamilyDefinition definition)
        implements Recipe<RecipeInput> {
    public static final MapCodec<CompactGTRecipeFamilyEntry> CODEC =
            CompactGTRecipeFamilyDefinition.MAP_CODEC.xmap(
                    CompactGTRecipeFamilyEntry::new,
                    CompactGTRecipeFamilyEntry::definition);

    public CompactGTRecipeFamilyEntry {
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
        if (definition.relations().isEmpty()) {
            return ItemStack.EMPTY;
        }
        var outputs = definition.relations().getFirst().itemOutputs();
        return outputs.isEmpty() ? ItemStack.EMPTY : outputs.getFirst().copy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.COMPACT_GT_RECIPE_FAMILY_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.COMPACT_GT_RECIPE_FAMILY_TYPE.get();
    }
}
