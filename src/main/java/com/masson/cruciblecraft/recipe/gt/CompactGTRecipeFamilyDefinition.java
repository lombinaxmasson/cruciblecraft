package com.masson.cruciblecraft.recipe.gt;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Host-neutral compact family datapack contract. Compact publication materializes exact
 * relations from inline {@link #relations()} or {@link #matrix()} template rows.
 * Unknown {@link #parameterized()} templates stay fail-closed.
 */
public record CompactGTRecipeFamilyDefinition(
        String familyId,
        ResourceLocation targetMap,
        String sourceRevision,
        List<Relation> relations,
        Optional<ParameterizedSpec> parameterized,
        Optional<ResourceLocation> publicationGroup,
        Optional<AuthoredMatrixV1> matrix) {

    private static final ResourceLocation ASSEMBLER_TARGET =
            ResourceLocation.fromNamespaceAndPath("cruciblecraft", "assembler");
    private static final ResourceLocation ROASTER_TARGET =
            ResourceLocation.fromNamespaceAndPath("cruciblecraft", "roaster");
    private static final Codec<JsonElement> JSON_ELEMENT = Codec.PASSTHROUGH.xmap(
            dynamic -> dynamic.convert(JsonOps.INSTANCE).getValue(),
            json -> new Dynamic<>(JsonOps.INSTANCE, json));

    public static final MapCodec<CompactGTRecipeFamilyDefinition> MAP_CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.STRING.fieldOf("family_id")
                            .forGetter(CompactGTRecipeFamilyDefinition::familyId),
                    ResourceLocation.CODEC.fieldOf("target_map")
                            .forGetter(CompactGTRecipeFamilyDefinition::targetMap),
                    Codec.STRING.fieldOf("source_revision")
                            .forGetter(CompactGTRecipeFamilyDefinition::sourceRevision),
                    Relation.CODEC.listOf()
                            .optionalFieldOf("relations", List.of())
                            .forGetter(CompactGTRecipeFamilyDefinition::relations),
                    ParameterizedSpec.CODEC.optionalFieldOf("parameterized")
                            .forGetter(CompactGTRecipeFamilyDefinition::parameterized),
                    ResourceLocation.CODEC.optionalFieldOf("publication_group")
                            .forGetter(CompactGTRecipeFamilyDefinition::publicationGroup),
                    Codec.STRING.optionalFieldOf("authored_form")
                            .forGetter(definition -> definition.matrix().isPresent()
                                    ? Optional.of(CompactAuthoredMatrix.AUTHORED_FORM_MATRIX_V1)
                                    : Optional.empty()),
                    AuthoredMatrixV1.CODEC.optionalFieldOf("matrix")
                            .forGetter(CompactGTRecipeFamilyDefinition::matrix)
            ).apply(instance, CompactGTRecipeFamilyDefinition::create));
    public static final Codec<CompactGTRecipeFamilyDefinition> CODEC = MAP_CODEC.codec();

    public CompactGTRecipeFamilyDefinition {
        Objects.requireNonNull(familyId, "familyId");
        Objects.requireNonNull(targetMap, "targetMap");
        Objects.requireNonNull(sourceRevision, "sourceRevision");
        relations = List.copyOf(Objects.requireNonNull(relations, "relations"));
        parameterized = Objects.requireNonNull(parameterized, "parameterized");
        publicationGroup = Objects.requireNonNull(
                publicationGroup, "publicationGroup");
        Optional<AuthoredMatrixV1> resolvedMatrix =
                Objects.requireNonNull(matrix, "matrix");
        matrix = resolvedMatrix;
        if (familyId.isBlank()) {
            throw new IllegalArgumentException("Compact family_id must not be blank");
        }
        if (sourceRevision.isBlank()) {
            throw new IllegalArgumentException(
                    "Compact source_revision must not be blank");
        }
        publicationGroup.ifPresent(group -> {
            if (group.getNamespace().isBlank() || group.getPath().isBlank()) {
                throw new IllegalArgumentException(
                        "Compact publication_group must not be blank");
            }
        });
        if (resolvedMatrix.isPresent() && !relations.isEmpty()) {
            throw new IllegalArgumentException(
                    "Compact family " + familyId
                            + " cannot mix matrix with inline relations");
        }
        parameterized.ifPresent(spec -> {
            if (CompactAuthoredMatrix.TEMPLATE_COMPACT_MATRIX_V1.equals(spec.template())
                    && resolvedMatrix.isEmpty()) {
                throw new IllegalArgumentException(
                        "Parameterized template "
                                + CompactAuthoredMatrix.TEMPLATE_COMPACT_MATRIX_V1
                                + " requires matrix");
            }
        });
    }

    private static CompactGTRecipeFamilyDefinition create(
            String familyId,
            ResourceLocation targetMap,
            String sourceRevision,
            List<Relation> relations,
            Optional<ParameterizedSpec> parameterized,
            Optional<ResourceLocation> publicationGroup,
            Optional<String> authoredForm,
            Optional<AuthoredMatrixV1> matrix) {
        authoredForm.ifPresent(form -> {
            if (CompactAuthoredMatrix.AUTHORED_FORM_MATRIX_V1.equals(form)) {
                if (matrix.isEmpty()) {
                    throw new IllegalArgumentException(
                            "authored_form matrix_v1 requires matrix");
                }
            } else if (CompactAuthoredMatrix.AUTHORED_FORM_INLINE.equals(form)) {
                if (matrix.isPresent()) {
                    throw new IllegalArgumentException(
                            "authored_form inline forbids matrix");
                }
            } else {
                throw new IllegalArgumentException("Unknown authored_form: " + form);
            }
        });
        return new CompactGTRecipeFamilyDefinition(
                familyId,
                targetMap,
                sourceRevision,
                relations,
                parameterized,
                publicationGroup,
                matrix);
    }

    public CompactGTRecipeFamilyDefinition(
            String familyId,
            ResourceLocation targetMap,
            String sourceRevision,
            List<Relation> relations) {
        this(
                familyId,
                targetMap,
                sourceRevision,
                relations,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    public CompactGTRecipeFamilyDefinition(
            String familyId,
            ResourceLocation targetMap,
            String sourceRevision,
            List<Relation> relations,
            Optional<ParameterizedSpec> parameterized) {
        this(
                familyId,
                targetMap,
                sourceRevision,
                relations,
                parameterized,
                Optional.empty(),
                Optional.empty());
    }

    public CompactGTRecipeFamilyDefinition(
            String familyId,
            ResourceLocation targetMap,
            String sourceRevision,
            List<Relation> relations,
            Optional<ParameterizedSpec> parameterized,
            Optional<ResourceLocation> publicationGroup) {
        this(
                familyId,
                targetMap,
                sourceRevision,
                relations,
                parameterized,
                publicationGroup,
                Optional.empty());
    }

    public CompactGTRecipeFamilyDefinition(
            String familyId,
            ResourceLocation targetMap,
            String sourceRevision,
            List<Relation> relations,
            ResourceLocation publicationGroup) {
        this(
                familyId,
                targetMap,
                sourceRevision,
                relations,
                Optional.empty(),
                Optional.of(Objects.requireNonNull(
                        publicationGroup, "publicationGroup")),
                Optional.empty());
    }

    public static ResourceLocation historicalPublicationGroup(
            ResourceLocation targetMap) {
        Objects.requireNonNull(targetMap, "targetMap");
        if (ASSEMBLER_TARGET.equals(targetMap)) {
            return CompactPublicationGroups.ASSEMBLER_COMPACT;
        }
        if (ROASTER_TARGET.equals(targetMap)) {
            return CompactPublicationGroups.ROASTER_COMPACT;
        }
        throw new IllegalArgumentException(
                "Compact target map " + targetMap
                        + " must declare publication_group");
    }

    public ResourceLocation resolvedPublicationGroup() {
        return publicationGroup.orElseGet(
                () -> historicalPublicationGroup(targetMap));
    }

    public List<Relation> authoredRelations() {
        return CompactAuthoredMatrix.expand(this);
    }

    /**
     * Lightweight exact relation. The lazy definition does not retain a
     * {@link GTRecipe}; callers materialize on eager publish, lookup, or
     * enumeration.
     */
    public record Relation(
            ResourceLocation stableId,
            List<Ingredient> itemInputs,
            List<Integer> itemInputCounts,
            List<ItemInputAction> itemInputActions,
            List<ItemStack> itemOutputs,
            List<FluidStack> fluidInputs,
            List<FluidStack> fluidOutputs,
            List<Integer> outputChances,
            int duration,
            long eut,
            long specialValue,
            boolean canBeBuffered,
            int shadowOrder,
            GTRecipeProvenance provenance) {

        public static final Codec<Relation> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        ResourceLocation.CODEC.fieldOf("stable_id")
                                .forGetter(Relation::stableId),
                        CompactRelationItemCodecs.ITEM_INPUTS
                                .optionalFieldOf("item_inputs", List.of())
                                .forGetter(Relation::itemInputs),
                        Codec.INT.listOf()
                                .optionalFieldOf("item_input_counts", List.of())
                                .forGetter(Relation::itemInputCounts),
                        ItemInputAction.CODEC.listOf()
                                .optionalFieldOf("item_input_actions", List.of())
                                .forGetter(Relation::itemInputActions),
                        CompactRelationItemCodecs.ITEM_OUTPUTS
                                .optionalFieldOf("item_outputs", List.of())
                                .forGetter(Relation::itemOutputs),
                        FluidStack.CODEC.listOf()
                                .optionalFieldOf("fluid_inputs", List.of())
                                .forGetter(Relation::fluidInputs),
                        FluidStack.CODEC.listOf()
                                .optionalFieldOf("fluid_outputs", List.of())
                                .forGetter(Relation::fluidOutputs),
                        Codec.INT.listOf()
                                .optionalFieldOf("output_chances", List.of())
                                .forGetter(Relation::outputChances),
                        Codec.INT.fieldOf("duration").forGetter(Relation::duration),
                        Codec.LONG.optionalFieldOf("eut", 0L).forGetter(Relation::eut),
                        Codec.LONG.optionalFieldOf("special_value", 0L)
                                .forGetter(Relation::specialValue),
                        Codec.BOOL.optionalFieldOf("can_be_buffered", true)
                                .forGetter(Relation::canBeBuffered),
                        Codec.INT.optionalFieldOf("shadow_order", 0)
                                .forGetter(Relation::shadowOrder),
                        GTRecipeProvenance.CODEC.fieldOf("provenance")
                                .forGetter(Relation::provenance)
                ).apply(instance, Relation::new));

        public Relation {
            Objects.requireNonNull(stableId, "stableId");
            Objects.requireNonNull(provenance, "provenance");
            itemInputs = List.copyOf(Objects.requireNonNull(itemInputs, "itemInputs"));
            itemInputCounts = List.copyOf(
                    Objects.requireNonNull(itemInputCounts, "itemInputCounts"));
            itemInputActions = List.copyOf(
                    Objects.requireNonNull(itemInputActions, "itemInputActions"));
            itemOutputs = Objects.requireNonNull(itemOutputs, "itemOutputs").stream()
                    .map(ItemStack::copy)
                    .toList();
            fluidInputs = Objects.requireNonNull(fluidInputs, "fluidInputs").stream()
                    .map(FluidStack::copy)
                    .toList();
            fluidOutputs = Objects.requireNonNull(fluidOutputs, "fluidOutputs").stream()
                    .map(FluidStack::copy)
                    .toList();
            outputChances = List.copyOf(
                    Objects.requireNonNull(outputChances, "outputChances"));
            if (shadowOrder < 0) {
                throw new IllegalArgumentException(
                        "Compact relation shadow_order must not be negative: "
                                + stableId);
            }
            try {
                new GTRecipe(
                        itemInputs,
                        itemInputCounts,
                        itemInputActions,
                        itemOutputs,
                        fluidInputs,
                        fluidOutputs,
                        outputChances,
                        duration,
                        eut,
                        specialValue,
                        canBeBuffered,
                        Optional.of(provenance));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        stableId + ": " + exception.getMessage(), exception);
            }
        }

        public GTRecipe materialize() {
            return new GTRecipe(
                    itemInputs,
                    itemInputCounts,
                    itemInputActions,
                    itemOutputs,
                    fluidInputs,
                    fluidOutputs,
                    outputChances,
                    duration,
                    eut,
                    specialValue,
                    canBeBuffered,
                    Optional.of(provenance));
        }
    }

    /**
     * Reserved parameterized family body. Known {@code compact_matrix_v1} must
     * pair with {@link #matrix()}; unknown templates stay fail-closed.
     */
    public record ParameterizedSpec(
            String template,
            Map<String, String> parameters) {

        public static final Codec<ParameterizedSpec> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.STRING.fieldOf("template")
                                .forGetter(ParameterizedSpec::template),
                        Codec.unboundedMap(Codec.STRING, Codec.STRING)
                                .optionalFieldOf("parameters", Map.of())
                                .forGetter(ParameterizedSpec::parameters)
                ).apply(instance, ParameterizedSpec::new));

        public ParameterizedSpec {
            Objects.requireNonNull(template, "template");
            parameters = Map.copyOf(Objects.requireNonNull(parameters, "parameters"));
            if (template.isBlank()) {
                throw new IllegalArgumentException(
                        "Parameterized compact template must not be blank");
            }
        }
    }

    public record SharedSpec(
            int duration,
            long eut,
            long specialValue,
            boolean canBeBuffered,
            List<Integer> itemInputCounts,
            List<ItemInputAction> itemInputActions,
            List<Integer> outputChances,
            String sourceKind,
            String selectedSourceRecipe) {

        public static final Codec<SharedSpec> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.INT.fieldOf("duration").forGetter(SharedSpec::duration),
                        Codec.LONG.optionalFieldOf("eut", 0L).forGetter(SharedSpec::eut),
                        Codec.LONG.optionalFieldOf("special_value", 0L)
                                .forGetter(SharedSpec::specialValue),
                        Codec.BOOL.optionalFieldOf("can_be_buffered", true)
                                .forGetter(SharedSpec::canBeBuffered),
                        Codec.INT.listOf()
                                .optionalFieldOf("item_input_counts", List.of())
                                .forGetter(SharedSpec::itemInputCounts),
                        ItemInputAction.CODEC.listOf()
                                .optionalFieldOf("item_input_actions", List.of())
                                .forGetter(SharedSpec::itemInputActions),
                        Codec.INT.listOf()
                                .optionalFieldOf("output_chances", List.of())
                                .forGetter(SharedSpec::outputChances),
                        Codec.STRING.fieldOf("source_kind")
                                .forGetter(SharedSpec::sourceKind),
                        Codec.STRING.fieldOf("selected_source_recipe")
                                .forGetter(SharedSpec::selectedSourceRecipe)
                ).apply(instance, SharedSpec::new));

        public SharedSpec {
            itemInputCounts = List.copyOf(
                    Objects.requireNonNull(itemInputCounts, "itemInputCounts"));
            itemInputActions = List.copyOf(
                    Objects.requireNonNull(itemInputActions, "itemInputActions"));
            outputChances = List.copyOf(
                    Objects.requireNonNull(outputChances, "outputChances"));
            Objects.requireNonNull(sourceKind, "sourceKind");
            Objects.requireNonNull(selectedSourceRecipe, "selectedSourceRecipe");
            if (sourceKind.isBlank()) {
                throw new IllegalArgumentException("Matrix source_kind must not be blank");
            }
            if (selectedSourceRecipe.isBlank()) {
                throw new IllegalArgumentException(
                        "Matrix selected_source_recipe must not be blank");
            }
            if (itemInputCounts.size() != itemInputActions.size()) {
                throw new IllegalArgumentException(
                        "Matrix shared item_input_counts and actions must match");
            }
            if (itemInputCounts.size() > CompactRecipeWireLimits.MAX_IO_PER_RELATION
                    || outputChances.size() > CompactRecipeWireLimits.MAX_IO_PER_RELATION) {
                throw new IllegalArgumentException(
                        "Matrix shared IO exceeds "
                                + CompactRecipeWireLimits.MAX_IO_PER_RELATION);
            }
        }
    }

    public record FluidIo(
            List<FluidStack> fluidInputs,
            List<FluidStack> fluidOutputs) {

        public static final Codec<FluidIo> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        FluidStack.CODEC.listOf()
                                .optionalFieldOf("fluid_inputs", List.of())
                                .forGetter(FluidIo::fluidInputs),
                        FluidStack.CODEC.listOf()
                                .optionalFieldOf("fluid_outputs", List.of())
                                .forGetter(FluidIo::fluidOutputs)
                ).apply(instance, FluidIo::new));

        public FluidIo {
            fluidInputs = Objects.requireNonNull(fluidInputs, "fluidInputs").stream()
                    .map(FluidStack::copy)
                    .toList();
            fluidOutputs = Objects.requireNonNull(fluidOutputs, "fluidOutputs").stream()
                    .map(FluidStack::copy)
                    .toList();
            if (fluidInputs.size() > CompactRecipeWireLimits.MAX_IO_PER_RELATION
                    || fluidOutputs.size() > CompactRecipeWireLimits.MAX_IO_PER_RELATION) {
                throw new IllegalArgumentException(
                        "Matrix fluid IO exceeds "
                                + CompactRecipeWireLimits.MAX_IO_PER_RELATION);
            }
        }
    }

    public record MatrixDicts(
            List<List<Ingredient>> itemInputs,
            List<List<ItemStack>> itemOutputs,
            List<FluidIo> fluids) {

        public static final Codec<MatrixDicts> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        CompactRelationItemCodecs.ITEM_INPUT_DICTS
                                .optionalFieldOf("item_inputs", List.of())
                                .forGetter(MatrixDicts::itemInputs),
                        CompactRelationItemCodecs.ITEM_OUTPUT_DICTS
                                .optionalFieldOf("item_outputs", List.of())
                                .forGetter(MatrixDicts::itemOutputs),
                        FluidIo.CODEC.listOf()
                                .optionalFieldOf("fluids", List.of())
                                .forGetter(MatrixDicts::fluids)
                ).apply(instance, MatrixDicts::new));

        public MatrixDicts {
            itemInputs = copyIngredientLists(
                    Objects.requireNonNull(itemInputs, "itemInputs"));
            itemOutputs = copyStackLists(
                    Objects.requireNonNull(itemOutputs, "itemOutputs"));
            fluids = List.copyOf(Objects.requireNonNull(fluids, "fluids"));
            if (itemInputs.size() > CompactRecipeWireLimits.MAX_DICTIONARY_ENTRIES
                    || itemOutputs.size() > CompactRecipeWireLimits.MAX_DICTIONARY_ENTRIES
                    || fluids.size() > CompactRecipeWireLimits.MAX_DICTIONARY_ENTRIES) {
                throw new IllegalArgumentException(
                        "Matrix dictionary exceeds "
                                + CompactRecipeWireLimits.MAX_DICTIONARY_ENTRIES);
            }
        }

        private static List<List<Ingredient>> copyIngredientLists(
                List<List<Ingredient>> values) {
            return values.stream()
                    .map(list -> List.copyOf(Objects.requireNonNull(list, "itemInputs")))
                    .toList();
        }

        private static List<List<ItemStack>> copyStackLists(List<List<ItemStack>> values) {
            return values.stream()
                    .map(list -> Objects.requireNonNull(list, "itemOutputs").stream()
                            .map(ItemStack::copy)
                            .toList())
                    .toList();
        }
    }

    public record MatrixRow(
            int inputIdx,
            int outputIdx,
            int fluidIdx,
            ResourceLocation stableId,
            int shadowOrder) {

        public static final Codec<MatrixRow> CODEC =
                JSON_ELEMENT.comapFlatMap(MatrixRow::read, MatrixRow::write);

        public MatrixRow {
            Objects.requireNonNull(stableId, "stableId");
            if (inputIdx < 0 || outputIdx < 0 || fluidIdx < 0) {
                throw new IllegalArgumentException(
                        "Matrix row indices must not be negative: " + stableId);
            }
            if (shadowOrder < 0) {
                throw new IllegalArgumentException(
                        "Compact relation shadow_order must not be negative: "
                                + stableId);
            }
        }

        private static DataResult<MatrixRow> read(JsonElement json) {
            if (json == null || !json.isJsonArray()) {
                return DataResult.error(() -> "Matrix row must be a 5-tuple array");
            }
            JsonArray array = json.getAsJsonArray();
            if (array.size() != 5 && array.size() != 6) {
                return DataResult.error(() -> "Matrix row must be a 5-tuple array");
            }
            try {
                int shadowOrder = array.size() == 6
                        ? array.get(5).getAsInt()
                        : array.get(4).getAsInt();
                return DataResult.success(new MatrixRow(
                        array.get(0).getAsInt(),
                        array.get(1).getAsInt(),
                        array.get(2).getAsInt(),
                        ResourceLocation.parse(array.get(3).getAsString()),
                        shadowOrder));
            } catch (RuntimeException failure) {
                return DataResult.error(
                        () -> "Invalid matrix row: " + failure.getMessage());
            }
        }

        private static JsonElement write(MatrixRow row) {
            JsonArray array = new JsonArray();
            array.add(new JsonPrimitive(row.inputIdx()));
            array.add(new JsonPrimitive(row.outputIdx()));
            array.add(new JsonPrimitive(row.fluidIdx()));
            array.add(new JsonPrimitive(row.stableId().toString()));
            array.add(new JsonPrimitive(row.shadowOrder()));
            return array;
        }
    }

    public record AuthoredMatrixV1(
            SharedSpec shared,
            MatrixDicts dicts,
            List<MatrixRow> rows) {

        public static final Codec<AuthoredMatrixV1> CODEC =
                Codec.lazyInitialized(() -> RecordCodecBuilder.create(instance -> instance.group(
                        SharedSpec.CODEC.fieldOf("shared")
                                .forGetter(AuthoredMatrixV1::shared),
                        MatrixDicts.CODEC.fieldOf("dicts")
                                .forGetter(AuthoredMatrixV1::dicts),
                        MatrixRow.CODEC.listOf().fieldOf("rows")
                                .forGetter(AuthoredMatrixV1::rows)
                ).apply(instance, AuthoredMatrixV1::new)));

        public AuthoredMatrixV1 {
            Objects.requireNonNull(shared, "shared");
            Objects.requireNonNull(dicts, "dicts");
            rows = List.copyOf(Objects.requireNonNull(rows, "rows"));
            if (rows.size() > CompactRecipeWireLimits.DECODE_RELATIONS_CEILING) {
                throw new IllegalArgumentException(
                        "Matrix row count "
                                + rows.size()
                                + " exceeds "
                                + CompactRecipeWireLimits.DECODE_RELATIONS_CEILING);
            }
            for (MatrixRow row : rows) {
                requireIndex(row.inputIdx(), dicts.itemInputs().size(), "item_inputs");
                requireIndex(row.outputIdx(), dicts.itemOutputs().size(), "item_outputs");
                requireIndex(row.fluidIdx(), dicts.fluids().size(), "fluids");
            }
        }

        private static void requireIndex(int index, int size, String label) {
            if (index < 0 || index >= size) {
                throw new IllegalArgumentException(
                        "Matrix " + label + " index " + index
                                + " out of range " + size);
            }
        }
    }
}
