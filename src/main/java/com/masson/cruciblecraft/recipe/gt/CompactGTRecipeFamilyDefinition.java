package com.masson.cruciblecraft.recipe.gt;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Host-neutral compact family datapack contract. T37 materializes exact
 * relations only; {@link #parameterized()} is a fail-closed extension point
 * for later Bath/Mixer/Smelter templates.
 */
public record CompactGTRecipeFamilyDefinition(
        String familyId,
        ResourceLocation targetMap,
        String sourceRevision,
        List<Relation> relations,
        Optional<ParameterizedSpec> parameterized) {

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
                            .forGetter(CompactGTRecipeFamilyDefinition::parameterized)
            ).apply(instance, CompactGTRecipeFamilyDefinition::new));
    public static final Codec<CompactGTRecipeFamilyDefinition> CODEC = MAP_CODEC.codec();

    public CompactGTRecipeFamilyDefinition {
        Objects.requireNonNull(familyId, "familyId");
        Objects.requireNonNull(targetMap, "targetMap");
        Objects.requireNonNull(sourceRevision, "sourceRevision");
        relations = List.copyOf(Objects.requireNonNull(relations, "relations"));
        parameterized = Objects.requireNonNull(parameterized, "parameterized");
        if (familyId.isBlank()) {
            throw new IllegalArgumentException("Compact family_id must not be blank");
        }
        if (sourceRevision.isBlank()) {
            throw new IllegalArgumentException(
                    "Compact source_revision must not be blank");
        }
    }

    public CompactGTRecipeFamilyDefinition(
            String familyId,
            ResourceLocation targetMap,
            String sourceRevision,
            List<Relation> relations) {
        this(familyId, targetMap, sourceRevision, relations, Optional.empty());
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
                        Ingredient.CODEC_NONEMPTY.listOf()
                                .optionalFieldOf("item_inputs", List.of())
                                .forGetter(Relation::itemInputs),
                        Codec.INT.listOf()
                                .optionalFieldOf("item_input_counts", List.of())
                                .forGetter(Relation::itemInputCounts),
                        ItemInputAction.CODEC.listOf()
                                .optionalFieldOf("item_input_actions", List.of())
                                .forGetter(Relation::itemInputActions),
                        ItemStack.STRICT_CODEC.listOf()
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
     * Reserved parameterized family body. T37 rejects any present value so
     * later cards can attach templates without changing the envelope type.
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
}
