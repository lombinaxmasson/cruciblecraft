package com.masson.cruciblecraft.recipe.gt;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import net.minecraft.world.item.ItemStack;
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

    private CompactAuthoredMatrix() {}

    public static List<CompactGTRecipeFamilyDefinition.Relation> expand(
            CompactGTRecipeFamilyDefinition definition) {
        Objects.requireNonNull(definition, "definition");
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
        CompactGTRecipeFamilyDefinition.MatrixDicts dicts = matrix.dicts();
        List<CompactGTRecipeFamilyDefinition.Relation> relations =
                new ArrayList<>(matrix.rows().size());
        for (CompactGTRecipeFamilyDefinition.MatrixRow row : matrix.rows()) {
            List<net.minecraft.world.item.crafting.Ingredient> itemInputs =
                    List.copyOf(lookup(
                            dicts.itemInputs(),
                            row.inputIdx(),
                            "item_inputs"));
            List<ItemStack> itemOutputs = lookup(
                    dicts.itemOutputs(),
                    row.outputIdx(),
                    "item_outputs")
                    .stream()
                    .map(ItemStack::copy)
                    .toList();
            CompactGTRecipeFamilyDefinition.FluidIo fluids = lookup(
                    dicts.fluids(),
                    row.fluidIdx(),
                    "fluids");
            if (itemInputs.size() != shared.itemInputCounts().size()
                    || itemInputs.size() != shared.itemInputActions().size()) {
                throw new IllegalArgumentException(
                        "Matrix row "
                                + row.stableId()
                                + " item input arity drifted from shared counts/actions");
            }
            if (itemOutputs.size() != shared.outputChances().size()) {
                throw new IllegalArgumentException(
                        "Matrix row "
                                + row.stableId()
                                + " output arity drifted from shared chances");
            }
            if (itemInputs.size() > CompactRecipeWireLimits.MAX_IO_PER_RELATION
                    || itemOutputs.size() > CompactRecipeWireLimits.MAX_IO_PER_RELATION
                    || fluids.fluidInputs().size()
                            > CompactRecipeWireLimits.MAX_IO_PER_RELATION
                    || fluids.fluidOutputs().size()
                            > CompactRecipeWireLimits.MAX_IO_PER_RELATION) {
                throw new IllegalArgumentException(
                        "Matrix row "
                                + row.stableId()
                                + " exceeds "
                                + CompactRecipeWireLimits.MAX_IO_PER_RELATION
                                + " IO slots");
            }
            relations.add(new CompactGTRecipeFamilyDefinition.Relation(
                    row.stableId(),
                    itemInputs,
                    shared.itemInputCounts(),
                    shared.itemInputActions(),
                    itemOutputs,
                    fluids.fluidInputs().stream().map(FluidStack::copy).toList(),
                    fluids.fluidOutputs().stream().map(FluidStack::copy).toList(),
                    shared.outputChances(),
                    shared.duration(),
                    shared.eut(),
                    shared.specialValue(),
                    shared.canBeBuffered(),
                    row.shadowOrder(),
                    new GTRecipeProvenance(
                            shared.sourceKind(),
                            Optional.of(shared.selectedSourceRecipe()))));
        }
        return List.copyOf(relations);
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
