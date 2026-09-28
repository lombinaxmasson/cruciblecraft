package com.masson.cruciblecraft.recipe.gt;

import java.util.List;
import java.util.Objects;

import com.masson.cruciblecraft.registry.ModRecipes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * Release-only envelope containing several independent compact family
 * definitions. The GT recipe loader unwraps members before semantic
 * publication; this recipe never participates in crafting itself.
 */
public record CompactGTRecipeFamilyBundle(
        List<CompactGTRecipeFamilyBundleEntry> families)
        implements Recipe<RecipeInput> {
    public static final MapCodec<CompactGTRecipeFamilyBundle> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    CompactGTRecipeFamilyBundleEntry.CODEC.codec()
                            .listOf()
                            .fieldOf("families")
                            .forGetter(CompactGTRecipeFamilyBundle::families)
            ).apply(instance, CompactGTRecipeFamilyBundle::new));

    public CompactGTRecipeFamilyBundle {
        families = List.copyOf(Objects.requireNonNull(families, "families"));
        if (families.isEmpty()) {
            throw new IllegalArgumentException(
                    "Compact family bundle must contain at least one family");
        }
        if (families.size() > CompactRecipeWireLimits.MAX_BUNDLE_FAMILIES) {
            throw new IllegalArgumentException(
                    "Compact family bundle contains "
                            + families.size() + " families; maximum is "
                            + CompactRecipeWireLimits.MAX_BUNDLE_FAMILIES);
        }
    }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(
            RecipeInput input,
            HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        CompactGTRecipeFamilyDefinition definition =
                families.getFirst().definition();
        return new CompactGTRecipeFamilyEntry(definition)
                .getResultItem(registries);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.COMPACT_GT_RECIPE_FAMILY_BUNDLE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.COMPACT_GT_RECIPE_FAMILY_BUNDLE_TYPE.get();
    }
}
