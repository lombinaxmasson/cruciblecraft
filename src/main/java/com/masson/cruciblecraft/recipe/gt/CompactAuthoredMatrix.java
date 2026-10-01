package com.masson.cruciblecraft.recipe.gt;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Expands {@code matrix_v1} compact families into exact relations. Datapack and
 * wire bodies stay compact; publication, dedup, and {@code prepare*} consume
 * the expanded list.
 */
public final class CompactAuthoredMatrix {
    public static final String AUTHORED_FORM_MATRIX_V1 = "matrix_v1";
    public static final String AUTHORED_FORM_INLINE = "inline";
    public static final String TEMPLATE_COMPACT_MATRIX_V1 = "compact_matrix_v1";

    private static final ThreadLocal<Map<CompactGTRecipeFamilyDefinition,
            List<CompactGTRecipeFamilyDefinition.Relation>>> EXPANDED =
            ThreadLocal.withInitial(IdentityHashMap::new);
    private static final List<ItemStack> NO_ITEMS = List.of();
    private static final List<FluidStack> NO_FLUIDS = List.of();

    private CompactAuthoredMatrix() {}

    /** Drops relation lists retained for the current reload. */
    public static void clearExpandCache() {
        EXPANDED.get().clear();
    }

    public static List<CompactGTRecipeFamilyDefinition.Relation> expand(
            CompactGTRecipeFamilyDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        Map<CompactGTRecipeFamilyDefinition,
                List<CompactGTRecipeFamilyDefinition.Relation>> cache = EXPANDED.get();
        List<CompactGTRecipeFamilyDefinition.Relation> cached = cache.get(definition);
        if (cached != null) {
            return cached;
        }
        List<CompactGTRecipeFamilyDefinition.Relation> expanded =
                expandUncached(definition);
        cache.put(definition, expanded);
        return expanded;
    }

    private static List<CompactGTRecipeFamilyDefinition.Relation> expandUncached(
            CompactGTRecipeFamilyDefinition definition) {
        Optional<CompactGTRecipeFamilyDefinition.ParameterizedSpec> parameterized =
                definition.parameterized();
        if (parameterized.isPresent()) {
            String template = parameterized.orElseThrow().template();
            if (!TEMPLATE_COMPACT_MATRIX_V1.equals(template)) {
                if (definition.matrix().isEmpty()) {
                    return definition.relations();
                }
                throw new IllegalArgumentException(
                        "Parameterized compact families are not implemented: template "
                                + template);
            }
            if (definition.matrix().isEmpty()) {
                throw new IllegalArgumentException(
                        "Parameterized template "
                                + TEMPLATE_COMPACT_MATRIX_V1
                                + " requires matrix");
            }
        }
        if (definition.matrix().isEmpty()) {
            return definition.relations();
        }
        return expandMatrix(definition.matrix().orElseThrow());
    }

    private static List<CompactGTRecipeFamilyDefinition.Relation> expandMatrix(
            CompactGTRecipeFamilyDefinition.AuthoredMatrixV1 matrix) {
        CompactGTRecipeFamilyDefinition.SharedSpec shared = matrix.shared();
        assertSharedShape(shared);
        CompactGTRecipeFamilyDefinition.MatrixDicts dicts = matrix.dicts();
        List<List<Ingredient>> itemInputs = dicts.itemInputs();
        List<List<ItemStack>> itemOutputs = copyOutputDictionaries(dicts.itemOutputs());
        List<List<FluidStack>> fluidInputs = new ArrayList<>(dicts.fluids().size());
        List<List<FluidStack>> fluidOutputs = new ArrayList<>(dicts.fluids().size());
        boolean[] consumes = new boolean[dicts.fluids().size()];
        boolean[] hasOutput = new boolean[dicts.fluids().size()];
        boolean requireOutput = shared.eut() >= 0L;
        for (int index = 0; index < dicts.fluids().size(); index++) {
            CompactGTRecipeFamilyDefinition.FluidIo fluids = dicts.fluids().get(index);
            List<FluidStack> inputs = copyFluids(fluids.fluidInputs());
            List<FluidStack> outputs = copyFluids(fluids.fluidOutputs());
            fluidInputs.add(inputs);
            fluidOutputs.add(outputs);
            consumes[index] = RecipeConsumptionRules.consumesAnything(
                    shared.itemInputCounts(), !inputs.isEmpty());
            hasOutput[index] = !shared.outputChances().isEmpty() || !outputs.isEmpty();
        }
        assertInputDictionaries(itemInputs);
        GTRecipeProvenance provenance = new GTRecipeProvenance(
                shared.sourceKind(),
                Optional.of(shared.selectedSourceRecipe()));
        List<CompactGTRecipeFamilyDefinition.Relation> relations =
                new ArrayList<>(matrix.rows().size());
        CompactGTRecipeFamilyDefinition.beginInternedMatrixRows();
        try {
            for (CompactGTRecipeFamilyDefinition.MatrixRow row : matrix.rows()) {
                List<Ingredient> inputs = lookup(
                        itemInputs, row.inputIdx(), "item_inputs");
                List<ItemStack> outputs = lookup(
                        itemOutputs, row.outputIdx(), "item_outputs");
                List<FluidStack> rowFluidInputs = lookup(
                        fluidInputs, row.fluidIdx(), "fluids");
                List<FluidStack> rowFluidOutputs = lookup(
                        fluidOutputs, row.fluidIdx(), "fluids");
                if (inputs.size() != shared.itemInputCounts().size()
                        || inputs.size() != shared.itemInputActions().size()) {
                    throw new IllegalArgumentException(
                            "Matrix row "
                                    + row.stableId()
                                    + " item input arity drifted from shared counts/actions");
                }
                if (outputs.size() != shared.outputChances().size()) {
                    throw new IllegalArgumentException(
                            "Matrix row "
                                    + row.stableId()
                                    + " output arity drifted from shared chances");
                }
                if (inputs.size() > CompactRecipeWireLimits.MAX_IO_PER_RELATION
                        || outputs.size() > CompactRecipeWireLimits.MAX_IO_PER_RELATION
                        || rowFluidInputs.size()
                                > CompactRecipeWireLimits.MAX_IO_PER_RELATION
                        || rowFluidOutputs.size()
                                > CompactRecipeWireLimits.MAX_IO_PER_RELATION) {
                    throw new IllegalArgumentException(
                            "Matrix row "
                                    + row.stableId()
                                    + " exceeds "
                                    + CompactRecipeWireLimits.MAX_IO_PER_RELATION
                                    + " IO slots");
                }
                if (inputs.isEmpty() && rowFluidInputs.isEmpty()) {
                    throw new IllegalArgumentException(
                            row.stableId() + ": A GT recipe must have at least one input");
                }
                if (!consumes[row.fluidIdx()]) {
                    throw new IllegalArgumentException(
                            row.stableId()
                                    + ": A GT recipe must consume at least one input");
                }
                if (requireOutput && !hasOutput[row.fluidIdx()]) {
                    throw new IllegalArgumentException(
                            row.stableId() + ": A GT recipe must have at least one output");
                }
                relations.add(new CompactGTRecipeFamilyDefinition.Relation(
                        row.stableId(),
                        inputs,
                        shared.itemInputCounts(),
                        shared.itemInputActions(),
                        outputs,
                        rowFluidInputs,
                        rowFluidOutputs,
                        shared.outputChances(),
                        shared.duration(),
                        shared.eut(),
                        shared.specialValue(),
                        shared.canBeBuffered(),
                        row.shadowOrder(),
                        provenance));
            }
        } finally {
            CompactGTRecipeFamilyDefinition.endInternedMatrixRows();
        }
        return List.copyOf(relations);
    }

    private static void assertSharedShape(
            CompactGTRecipeFamilyDefinition.SharedSpec shared) {
        if (shared.duration() <= 0) {
            throw new IllegalArgumentException("Recipe duration must be positive");
        }
        List<Integer> counts = shared.itemInputCounts();
        List<ItemInputAction> actions = shared.itemInputActions();
        for (int index = 0; index < counts.size(); index++) {
            Integer count = counts.get(index);
            if (count == null || count < 0) {
                throw new IllegalArgumentException(
                        "Item input counts must not be negative");
            }
            ItemInputAction action = actions.get(index);
            if (action == null) {
                throw new IllegalArgumentException(
                        "Item input actions must not be null");
            }
            if (action.kind() == ItemInputAction.Kind.CONSUME && count <= 0) {
                throw new IllegalArgumentException(
                        "CONSUME item inputs must have a positive count");
            }
            if (action.kind() != ItemInputAction.Kind.CONSUME && count != 0) {
                throw new IllegalArgumentException(
                        "PRESERVE and WEAR item inputs must use count zero");
            }
        }
        for (Integer chance : shared.outputChances()) {
            if (chance == null
                    || chance < 0
                    || chance > GTRecipe.GUARANTEED_CHANCE) {
                throw new IllegalArgumentException(
                        "Output chances must be between 0 and 10000");
            }
        }
    }

    private static void assertInputDictionaries(List<List<Ingredient>> dictionaries) {
        for (int index = 0; index < dictionaries.size(); index++) {
            for (Ingredient ingredient : dictionaries.get(index)) {
                if (ingredient == null || ingredient.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Matrix item_inputs[" + index
                                    + "]: Item inputs must not contain empty ingredients");
                }
            }
        }
    }

    private static List<List<ItemStack>> copyOutputDictionaries(
            List<List<ItemStack>> dictionaries) {
        List<List<ItemStack>> copied = new ArrayList<>(dictionaries.size());
        for (int index = 0; index < dictionaries.size(); index++) {
            List<ItemStack> entry = dictionaries.get(index);
            if (entry.isEmpty()) {
                copied.add(NO_ITEMS);
                continue;
            }
            List<ItemStack> stacks = new ArrayList<>(entry.size());
            for (ItemStack stack : entry) {
                ItemStack copy = stack.copy();
                if (copy.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Matrix item_outputs[" + index
                                    + "]: Item outputs must not contain empty stacks");
                }
                if (copy.getCount() > copy.getMaxStackSize()) {
                    throw new IllegalArgumentException(
                            "Matrix item_outputs[" + index
                                    + "]: Item output counts must fit "
                                    + "ItemStack.STRICT_CODEC");
                }
                stacks.add(copy);
            }
            copied.add(List.copyOf(stacks));
        }
        return List.copyOf(copied);
    }

    private static List<FluidStack> copyFluids(List<FluidStack> stacks) {
        if (stacks.isEmpty()) {
            return NO_FLUIDS;
        }
        List<FluidStack> copied = new ArrayList<>(stacks.size());
        for (FluidStack stack : stacks) {
            FluidStack copy = stack.copy();
            if (copy.isEmpty()) {
                throw new IllegalArgumentException("Fluid stacks must not be empty");
            }
            copied.add(copy);
        }
        return List.copyOf(copied);
    }

    private static <T> T lookup(List<T> dictionary, int index, String label) {
        if (index < 0 || index >= dictionary.size()) {
            throw new IllegalArgumentException(
                    "Matrix " + label + " index " + index
                            + " out of range " + dictionary.size());
        }
        return dictionary.get(index);
    }
}
