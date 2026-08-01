package com.masson.cruciblecraft.recipe.gt;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Fully resolved machine-recipe data. Matching/indexing policy lives outside
 * this value in {@link RecipeMap} and {@link RecipeHandler}.
 */
public record GTRecipe(
        List<Ingredient> itemInputs,
        List<Integer> itemInputCounts,
        List<ItemStack> itemOutputs,
        List<FluidStack> fluidInputs,
        List<FluidStack> fluidOutputs,
        List<Integer> outputChances,
        int duration,
        long eut,
        long specialValue,
        boolean canBeBuffered,
        Optional<GTRecipeProvenance> provenance) {

    public static final int GUARANTEED_CHANCE = 10_000;

    public static final MapCodec<GTRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.listOf()
                    .optionalFieldOf("item_inputs", List.of())
                    .forGetter(GTRecipe::itemInputs),
            Codec.INT.listOf()
                    .optionalFieldOf("item_input_counts", List.of())
                    .forGetter(GTRecipe::itemInputCounts),
            ItemStack.STRICT_CODEC.listOf()
                    .optionalFieldOf("item_outputs", List.of())
                    .forGetter(GTRecipe::itemOutputs),
            FluidStack.CODEC.listOf()
                    .optionalFieldOf("fluid_inputs", List.of())
                    .forGetter(GTRecipe::fluidInputs),
            FluidStack.CODEC.listOf()
                    .optionalFieldOf("fluid_outputs", List.of())
                    .forGetter(GTRecipe::fluidOutputs),
            Codec.INT.listOf()
                    .optionalFieldOf("output_chances", List.of())
                    .forGetter(GTRecipe::outputChances),
            Codec.INT.fieldOf("duration").forGetter(GTRecipe::duration),
            Codec.LONG.optionalFieldOf("eut", 0L).forGetter(GTRecipe::eut),
            Codec.LONG.optionalFieldOf("special_value", 0L).forGetter(GTRecipe::specialValue),
            Codec.BOOL.optionalFieldOf("can_be_buffered", true).forGetter(GTRecipe::canBeBuffered),
            GTRecipeProvenance.CODEC.optionalFieldOf("provenance")
                    .forGetter(GTRecipe::provenance)
    ).apply(instance, GTRecipe::new));
    public static final Codec<GTRecipe> CODEC = MAP_CODEC.codec();

    public GTRecipe {
        itemInputs = immutable(itemInputs, "itemInputs");
        itemInputCounts = immutable(itemInputCounts, "itemInputCounts");
        itemOutputs = copyItems(itemOutputs);
        fluidInputs = copyFluids(fluidInputs, "fluidInputs");
        fluidOutputs = copyFluids(fluidOutputs, "fluidOutputs");
        outputChances = immutable(outputChances, "outputChances");
        provenance = Objects.requireNonNull(provenance, "provenance");

        if (itemInputCounts.isEmpty() && !itemInputs.isEmpty()) {
            itemInputCounts = Collections.nCopies(itemInputs.size(), 1);
        }
        if (outputChances.isEmpty() && !itemOutputs.isEmpty()) {
            outputChances = Collections.nCopies(itemOutputs.size(), GUARANTEED_CHANCE);
        }
        if (itemInputCounts.size() != itemInputs.size()) {
            throw new IllegalArgumentException(
                    "Every item input must have exactly one input count");
        }
        if (outputChances.size() != itemOutputs.size()) {
            throw new IllegalArgumentException(
                    "Every item output must have exactly one output chance");
        }
        if (itemInputCounts.stream().anyMatch(count -> count == null || count < 0)) {
            throw new IllegalArgumentException("Item input counts must not be negative");
        }
        if (outputChances.stream().anyMatch(
                chance -> chance == null || chance < 0 || chance > GUARANTEED_CHANCE)) {
            throw new IllegalArgumentException("Output chances must be between 0 and 10000");
        }
        if (itemInputs.stream().anyMatch(
                ingredient -> ingredient == null || ingredient.isEmpty())) {
            throw new IllegalArgumentException("Item inputs must not contain empty ingredients");
        }
        if (itemOutputs.stream().anyMatch(ItemStack::isEmpty)) {
            throw new IllegalArgumentException("Item outputs must not contain empty stacks");
        }
        if (itemOutputs.stream().anyMatch(
                stack -> stack.getCount() > stack.getMaxStackSize())) {
            throw new IllegalArgumentException(
                    "Item output counts must fit ItemStack.STRICT_CODEC");
        }
        if (fluidInputs.stream().anyMatch(FluidStack::isEmpty)
                || fluidOutputs.stream().anyMatch(FluidStack::isEmpty)) {
            throw new IllegalArgumentException("Fluid stacks must not be empty");
        }
        if (itemInputs.isEmpty() && fluidInputs.isEmpty()) {
            throw new IllegalArgumentException("A GT recipe must have at least one input");
        }
        if (!RecipeConsumptionRules.consumesAnything(
                itemInputCounts,
                !fluidInputs.isEmpty())) {
            throw new IllegalArgumentException("A GT recipe must consume at least one input");
        }
        if (itemOutputs.isEmpty() && fluidOutputs.isEmpty()) {
            throw new IllegalArgumentException("A GT recipe must have at least one output");
        }
        if (duration <= 0) {
            throw new IllegalArgumentException("Recipe duration must be positive");
        }
    }

    public GTRecipe(
            List<Ingredient> itemInputs,
            List<Integer> itemInputCounts,
            List<ItemStack> itemOutputs,
            List<FluidStack> fluidInputs,
            List<FluidStack> fluidOutputs,
            List<Integer> outputChances,
            int duration,
            long eut,
            long specialValue) {
        this(
                itemInputs,
                itemInputCounts,
                itemOutputs,
                fluidInputs,
                fluidOutputs,
                outputChances,
                duration,
                eut,
                specialValue,
                true,
                Optional.empty());
    }

    public GTRecipe(
            List<Ingredient> itemInputs,
            List<Integer> itemInputCounts,
            List<ItemStack> itemOutputs,
            List<FluidStack> fluidInputs,
            List<FluidStack> fluidOutputs,
            List<Integer> outputChances,
            int duration,
            long eut,
            long specialValue,
            boolean canBeBuffered) {
        this(
                itemInputs,
                itemInputCounts,
                itemOutputs,
                fluidInputs,
                fluidOutputs,
                outputChances,
                duration,
                eut,
                specialValue,
                canBeBuffered,
                Optional.empty());
    }

    /** Returns the semantic recipe value used by cache/fingerprint matching. */
    public GTRecipe withoutProvenance() {
        if (provenance.isEmpty()) {
            return this;
        }
        return new GTRecipe(
                itemInputs,
                itemInputCounts,
                itemOutputs,
                fluidInputs,
                fluidOutputs,
                outputChances,
                duration,
                eut,
                specialValue,
                canBeBuffered,
                Optional.empty());
    }

    public boolean matches(GTRecipeQuery query) {
        return RecipeResourceMatcher.matchesItems(
                        itemInputs,
                        itemInputCounts,
                        query.itemInputsView())
                && RecipeResourceMatcher.matchesFluids(
                        fluidInputs,
                        query.fluidInputsView());
    }

    /** Allocation rows are recipe inputs and columns are offered inventory slots. */
    public Optional<long[][]> itemAllocation(List<ItemStack> offered) {
        return RecipeResourceMatcher.solveItems(itemInputs, itemInputCounts, offered);
    }

    @Override
    public List<ItemStack> itemOutputs() {
        return itemOutputs.stream().map(ItemStack::copy).toList();
    }

    @Override
    public List<FluidStack> fluidInputs() {
        return fluidInputs.stream().map(FluidStack::copy).toList();
    }

    @Override
    public List<FluidStack> fluidOutputs() {
        return fluidOutputs.stream().map(FluidStack::copy).toList();
    }

    /** Package-private zero-copy view for trusted index/matching code. */
    List<FluidStack> fluidInputsView() {
        return fluidInputs;
    }

    private static <T> List<T> immutable(List<T> values, String name) {
        if (values == null) {
            throw new NullPointerException(name);
        }
        return List.copyOf(values);
    }

    private static List<ItemStack> copyItems(List<ItemStack> stacks) {
        return immutable(stacks, "itemOutputs").stream().map(ItemStack::copy).toList();
    }

    private static List<FluidStack> copyFluids(List<FluidStack> stacks, String name) {
        return immutable(stacks, name).stream().map(FluidStack::copy).toList();
    }
}
