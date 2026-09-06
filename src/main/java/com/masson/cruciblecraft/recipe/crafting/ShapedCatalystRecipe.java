package com.masson.cruciblecraft.recipe.crafting;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.masson.cruciblecraft.registry.ModRecipes;

import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * A fixed 3x3 crafting recipe with GT6-style non-consuming tool slots.
 *
 * <p>GT6's {@code CR.DEF_NCC} recipes put tools in lower-case pattern slots.
 * Vanilla shaped recipes cannot preserve those tools, so this recipe keeps
 * the lower-case slots in the grid and returns their stacks unchanged.
 */
public final class ShapedCatalystRecipe implements CraftingRecipe {
    public static final MapCodec<ShapedCatalystRecipe> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.STRING.listOf()
                            .fieldOf("pattern")
                            .forGetter(ShapedCatalystRecipe::pattern),
                    Codec.unboundedMap(Codec.STRING, Ingredient.CODEC_NONEMPTY)
                            .optionalFieldOf("ingredients", Map.of())
                            .forGetter(ShapedCatalystRecipe::ingredients),
                    Codec.unboundedMap(Codec.STRING, Ingredient.CODEC_NONEMPTY)
                            .optionalFieldOf("catalysts", Map.of())
                            .forGetter(ShapedCatalystRecipe::catalysts),
                    ItemStack.STRICT_CODEC
                            .fieldOf("result")
                            .forGetter(ShapedCatalystRecipe::result))
                    .apply(instance, ShapedCatalystRecipe::new));

    private final List<String> pattern;
    private final Map<String, Ingredient> ingredients;
    private final Map<String, Ingredient> catalysts;
    private final ItemStack result;

    public ShapedCatalystRecipe(
            List<String> pattern,
            Map<String, Ingredient> ingredients,
            Map<String, Ingredient> catalysts,
            ItemStack result) {
        if (pattern.size() != 3
                || pattern.stream().anyMatch(row -> row.length() != 3)) {
            throw new IllegalArgumentException(
                    "Shaped catalyst recipes must have a 3x3 pattern");
        }
        this.pattern = List.copyOf(pattern);
        this.ingredients = Map.copyOf(new LinkedHashMap<>(ingredients));
        this.catalysts = Map.copyOf(new LinkedHashMap<>(catalysts));
        this.result = result.copy();
        validateSymbols();
    }

    public List<String> pattern() {
        return pattern;
    }

    public Map<String, Ingredient> ingredients() {
        return ingredients;
    }

    public Map<String, Ingredient> catalysts() {
        return catalysts;
    }

    public ItemStack result() {
        return result.copy();
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.width() != 3 || input.height() != 3) {
            return false;
        }
        for (int row = 0; row < 3; row++) {
            String line = pattern.get(row);
            for (int column = 0; column < 3; column++) {
                String symbol = String.valueOf(line.charAt(column));
                Ingredient ingredient = ingredientFor(symbol);
                ItemStack stack = input.getItem(row * 3 + column);
                if (ingredient == null) {
                    if (!stack.isEmpty()) {
                        return false;
                    }
                } else if (!ingredient.test(stack)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(
            CraftingInput input,
            HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining =
                NonNullList.withSize(input.size(), ItemStack.EMPTY);
        if (input.width() != 3 || input.height() != 3) {
            return remaining;
        }
        for (int row = 0; row < 3; row++) {
            String line = pattern.get(row);
            for (int column = 0; column < 3; column++) {
                String symbol = String.valueOf(line.charAt(column));
                if (catalysts.containsKey(symbol)) {
                    remaining.set(
                            row * 3 + column,
                            input.getItem(row * 3 + column).copy());
                }
            }
        }
        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.SHAPED_CATALYST_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    private Ingredient ingredientFor(String symbol) {
        if (" ".equals(symbol)) {
            return null;
        }
        Ingredient catalyst = catalysts.get(symbol);
        Ingredient ingredient = ingredients.get(symbol);
        if (catalyst != null && ingredient != null) {
            throw new IllegalStateException(
                    "Shaped catalyst symbol is both material and catalyst: " + symbol);
        }
        return catalyst != null ? catalyst : ingredient;
    }

    private void validateSymbols() {
        for (Map.Entry<String, Ingredient> entry : ingredients.entrySet()) {
            validateSymbol(entry.getKey());
            Objects.requireNonNull(entry.getValue(), "ingredient");
        }
        for (Map.Entry<String, Ingredient> entry : catalysts.entrySet()) {
            validateSymbol(entry.getKey());
            Objects.requireNonNull(entry.getValue(), "catalyst");
        }
        for (String row : pattern) {
            for (int index = 0; index < row.length(); index++) {
                String symbol = String.valueOf(row.charAt(index));
                if (!" ".equals(symbol) && ingredientFor(symbol) == null) {
                    throw new IllegalArgumentException(
                            "Shaped catalyst pattern symbol has no ingredient: " + symbol);
                }
            }
        }
    }

    private static void validateSymbol(String symbol) {
        if (symbol == null || symbol.length() != 1 || " ".equals(symbol)) {
            throw new IllegalArgumentException(
                    "Shaped catalyst symbols must be one non-space character");
        }
    }
}
