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

/** Vanilla datapack envelope around a compact dedup rule. */
public record CompactDedupRuleEntry(CompactDedupRuleDefinition definition)
        implements Recipe<RecipeInput> {
    public static final MapCodec<CompactDedupRuleEntry> CODEC =
            CompactDedupRuleDefinition.MAP_CODEC.xmap(
                    CompactDedupRuleEntry::new,
                    CompactDedupRuleEntry::definition);

    public CompactDedupRuleEntry {
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
        return ModRecipes.COMPACT_DEDUP_RULE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.COMPACT_DEDUP_RULE_TYPE.get();
    }
}
