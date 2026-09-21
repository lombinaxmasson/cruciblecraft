package com.masson.cruciblecraft.recipe.crafting;

import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.masson.cruciblecraft.registry.ModRecipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * GT6 {@code AdvancedCraftingXToY} / {@code AdvancedCrafting1ToY} for dust,
 * nugget, and plate family packing. One recipe covers every material that has
 * both prefixes registered; vanilla {@code formItems()} stacks resolve through
 * {@link MaterialUnits}.
 *
 * <p>When several 1-to-Y unpacks share an input prefix, GT6 disambiguates by
 * leading empty slots ({@code tEmpty % size == index}). Slot 0 is the first
 * registered unpack.
 */
public final class PrefixPackRecipe implements CraftingRecipe {
    public static final MapCodec<PrefixPackRecipe> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.STRING
                            .fieldOf("input_prefix")
                            .xmap(
                                    MaterialPrefixCatalog::require,
                                    MaterialPrefix::serializedName)
                            .forGetter(PrefixPackRecipe::inputPrefix),
                    Codec.INT.fieldOf("input_count")
                            .forGetter(PrefixPackRecipe::inputCount),
                    Codec.STRING
                            .fieldOf("output_prefix")
                            .xmap(
                                    MaterialPrefixCatalog::require,
                                    MaterialPrefix::serializedName)
                            .forGetter(PrefixPackRecipe::outputPrefix),
                    Codec.INT.fieldOf("output_count")
                            .forGetter(PrefixPackRecipe::outputCount),
                    Codec.INT.optionalFieldOf("unpack_index", 0)
                            .forGetter(PrefixPackRecipe::unpackIndex),
                    Codec.INT.optionalFieldOf("unpack_modulus", 1)
                            .forGetter(PrefixPackRecipe::unpackModulus))
                    .apply(instance, PrefixPackRecipe::new));

    private final MaterialPrefix inputPrefix;
    private final int inputCount;
    private final MaterialPrefix outputPrefix;
    private final int outputCount;
    private final int unpackIndex;
    private final int unpackModulus;

    public PrefixPackRecipe(
            MaterialPrefix inputPrefix,
            int inputCount,
            MaterialPrefix outputPrefix,
            int outputCount,
            int unpackIndex,
            int unpackModulus) {
        if (inputCount < 1 || outputCount < 1) {
            throw new IllegalArgumentException(
                    "Prefix pack counts must be positive");
        }
        if (unpackModulus < 1 || unpackIndex < 0 || unpackIndex >= unpackModulus) {
            throw new IllegalArgumentException(
                    "Invalid unpack slot disambiguation "
                            + unpackIndex + "/" + unpackModulus);
        }
        this.inputPrefix = inputPrefix;
        this.inputCount = inputCount;
        this.outputPrefix = outputPrefix;
        this.outputCount = outputCount;
        this.unpackIndex = unpackIndex;
        this.unpackModulus = unpackModulus;
    }

    public MaterialPrefix inputPrefix() {
        return inputPrefix;
    }

    public int inputCount() {
        return inputCount;
    }

    public MaterialPrefix outputPrefix() {
        return outputPrefix;
    }

    public int outputCount() {
        return outputCount;
    }

    public int unpackIndex() {
        return unpackIndex;
    }

    public int unpackModulus() {
        return unpackModulus;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return materialFrom(input) != null;
    }

    @Override
    public ItemStack assemble(
            CraftingInput input,
            HolderLookup.Provider registries) {
        String material = materialFrom(input);
        if (material == null) {
            return ItemStack.EMPTY;
        }
        return MaterialLookup.tryStack(material, outputPrefix, outputCount)
                .orElse(ItemStack.EMPTY);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= inputCount;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.create();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.PREFIX_PACK_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    private String materialFrom(CraftingInput input) {
        String material = null;
        int used = 0;
        int leadingEmpty = 0;
        boolean seenItem = false;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                if (!seenItem) {
                    leadingEmpty++;
                }
                continue;
            }
            seenItem = true;
            var entry = MaterialUnits.resolve(stack);
            if (entry.isEmpty() || !entry.orElseThrow().form().equals(inputPrefix)) {
                return null;
            }
            String id = entry.orElseThrow().materialId();
            if (material == null) {
                material = id;
            } else if (!material.equals(id)) {
                return null;
            }
            used++;
        }
        if (used != inputCount || material == null) {
            return null;
        }
        if (inputCount == 1
                && unpackModulus > 1
                && leadingEmpty % unpackModulus != unpackIndex) {
            return null;
        }
        if (MaterialCatalog.find(material)
                .filter(definition ->
                        MaterialCatalog.isFormRegistered(definition, outputPrefix))
                .isEmpty()) {
            return null;
        }
        return material;
    }
}
