package com.masson.cruciblecraft.recipe.rule;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.masson.cruciblecraft.recipe.gt.ComponentIngredientIndex;
import com.masson.cruciblecraft.recipe.gt.ItemInputAction;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/** Synced declarative processing rule and material metadata envelope. */
public record MaterialRule(
        Optional<ResourceLocation> target,
        List<ItemResource> itemInputs,
        List<ItemResource> itemOutputs,
        List<FluidResource> fluidInputs,
        List<FluidResource> fluidOutputs,
        String duration,
        String eut,
        String specialValue,
        boolean canBeBuffered,
        Optional<String> material,
        Map<String, MaterialOverride> materialOverrides,
        List<String> conditions,
        Optional<Tuning> tuning,
        List<UnificationPreference> unification,
        Optional<SparseTable> sparse) {

    public static final MapCodec<MaterialRule> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    ResourceLocation.CODEC.optionalFieldOf("target")
                            .forGetter(MaterialRule::target),
                    ItemResource.CODEC.codec().listOf().optionalFieldOf("item_inputs", List.of())
                            .forGetter(MaterialRule::itemInputs),
                    ItemResource.CODEC.codec().listOf().optionalFieldOf("item_outputs", List.of())
                            .forGetter(MaterialRule::itemOutputs),
                    FluidResource.CODEC.codec().listOf().optionalFieldOf("fluid_inputs", List.of())
                            .forGetter(MaterialRule::fluidInputs),
                    FluidResource.CODEC.codec().listOf().optionalFieldOf("fluid_outputs", List.of())
                            .forGetter(MaterialRule::fluidOutputs),
                    Codec.STRING.optionalFieldOf("duration", "1").forGetter(MaterialRule::duration),
                    Codec.STRING.optionalFieldOf("eut", "0").forGetter(MaterialRule::eut),
                    Codec.STRING.optionalFieldOf("special_value", "0")
                            .forGetter(MaterialRule::specialValue),
                    Codec.BOOL.optionalFieldOf("can_be_buffered", true)
                            .forGetter(MaterialRule::canBeBuffered),
                    Codec.STRING.optionalFieldOf("material").forGetter(MaterialRule::material),
                    Codec.unboundedMap(Codec.STRING, MaterialOverride.CODEC.codec())
                            .optionalFieldOf("material_overrides", Map.of())
                            .forGetter(MaterialRule::materialOverrides),
                    Codec.STRING.listOf().optionalFieldOf("conditions", List.of())
                            .forGetter(MaterialRule::conditions),
                    Tuning.CODEC.codec().optionalFieldOf("tuning")
                            .forGetter(MaterialRule::tuning),
                    UnificationPreference.CODEC.codec().listOf()
                            .optionalFieldOf("unification", List.of())
                            .forGetter(MaterialRule::unification),
                    SparseTable.CODEC.codec().optionalFieldOf("sparse")
                            .forGetter(MaterialRule::sparse)
            ).apply(instance, MaterialRule::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, MaterialRule> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());

    public MaterialRule {
        target = target == null ? Optional.empty() : target;
        itemInputs = List.copyOf(itemInputs);
        itemOutputs = List.copyOf(itemOutputs);
        fluidInputs = List.copyOf(fluidInputs);
        fluidOutputs = List.copyOf(fluidOutputs);
        material = material == null ? Optional.empty() : material;
        materialOverrides = Map.copyOf(materialOverrides);
        conditions = List.copyOf(conditions);
        tuning = tuning == null ? Optional.empty() : tuning;
        unification = List.copyOf(unification);
        sparse = sparse == null ? Optional.empty() : sparse;
        if (target.isEmpty() && tuning.isEmpty() && unification.isEmpty()) {
            throw new IllegalArgumentException(
                    "A material_rule must declare target, tuning, or unification metadata");
        }
        if (target.isPresent()
                && sparse.isEmpty()
                && (itemInputs.isEmpty() && fluidInputs.isEmpty()
                        || itemOutputs.isEmpty() && fluidOutputs.isEmpty())) {
            throw new IllegalArgumentException(
                    "A processing material_rule requires at least one input and output");
        }
        if (sparse.isPresent()
                && (target.isEmpty()
                        || !itemInputs.isEmpty()
                        || !itemOutputs.isEmpty()
                        || !fluidInputs.isEmpty()
                        || !fluidOutputs.isEmpty()
                        || material.isPresent()
                        || !materialOverrides.isEmpty()
                        || !conditions.isEmpty())) {
            throw new IllegalArgumentException(
                    "A sparse material_rule must declare only target and sparse relations");
        }
        boolean hasPrefixDependency = itemInputs.stream().anyMatch(resource -> resource.prefix().isPresent())
                || itemOutputs.stream().anyMatch(resource -> resource.prefix().isPresent())
                || fluidInputs.stream().anyMatch(resource -> resource.prefix().isPresent())
                || fluidOutputs.stream().anyMatch(resource -> resource.prefix().isPresent());
        if (target.isPresent() && sparse.isEmpty()
                && material.isEmpty() && !hasPrefixDependency) {
            throw new IllegalArgumentException(
                    "A fixed-only processing recipe must use cruciblecraft:gt_recipe; "
                            + "material_rule requires a prefix dependency or explicit material");
        }
        material.ifPresent(id -> requireMaterialId(id, "material"));
        materialOverrides.keySet().forEach(id -> requireMaterialId(id, "material override"));
        if (itemInputs.stream().anyMatch(ItemResource::optional)) {
            throw new IllegalArgumentException("Material-rule inputs cannot be optional");
        }
        if (itemOutputs.stream().anyMatch(resource -> resource.inputAction().isPresent())) {
            throw new IllegalArgumentException(
                    "Material-rule output resources cannot declare input_action");
        }
        if (itemOutputs.stream().anyMatch(resource -> resource.tag().isPresent())) {
            throw new IllegalArgumentException(
                    "Material-rule output resources cannot declare a tag");
        }
        itemOutputs.stream().filter(ItemResource::optional).forEach(resource -> {
            if (resource.materialSelector().isEmpty()
                    || !resource.materialSelector().get().startsWith("byproduct:")) {
                throw new IllegalArgumentException(
                        "Only byproduct outputs may be optional");
            }
        });
    }

    public MaterialRule(
            Optional<ResourceLocation> target,
            List<ItemResource> itemInputs,
            List<ItemResource> itemOutputs,
            List<FluidResource> fluidInputs,
            List<FluidResource> fluidOutputs,
            String duration,
            String eut,
            String specialValue,
            boolean canBeBuffered,
            Optional<String> material,
            Map<String, MaterialOverride> materialOverrides,
            List<String> conditions,
            Optional<Tuning> tuning,
            List<UnificationPreference> unification) {
        this(
                target,
                itemInputs,
                itemOutputs,
                fluidInputs,
                fluidOutputs,
                duration,
                eut,
                specialValue,
                canBeBuffered,
                material,
                materialOverrides,
                conditions,
                tuning,
                unification,
                Optional.empty());
    }

    static void requireMaterialId(String value, String field) {
        if (!value.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException("Invalid " + field + ": " + value);
        }
    }

    private static void requireMaterialSelector(String selector) {
        if (selector.equals("self")) {
            return;
        }
        if (selector.matches("processing_target:[a-z0-9_]+")
                || selector.matches("byproduct:[0-9]+")
                || selector.matches("material:[a-z0-9_]+")) {
            return;
        }
        throw new IllegalArgumentException("Invalid material selector: " + selector);
    }

    public record ItemResource(
            Optional<String> prefix,
            Optional<ResourceLocation> item,
            Optional<ResourceLocation> tag,
            String count,
            String chance,
            Optional<String> materialSelector,
            boolean optional,
            Map<ResourceLocation, String> stringComponents,
            Optional<ItemInputAction> inputAction) {
        public static final MapCodec<ItemResource> CODEC =
                RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Codec.STRING.optionalFieldOf("prefix").forGetter(ItemResource::prefix),
                        ResourceLocation.CODEC.optionalFieldOf("item").forGetter(ItemResource::item),
                        ResourceLocation.CODEC.optionalFieldOf("tag")
                                .forGetter(ItemResource::tag),
                        Codec.STRING.optionalFieldOf("count", "1").forGetter(ItemResource::count),
                        Codec.STRING.optionalFieldOf("chance", "10000").forGetter(ItemResource::chance),
                        Codec.STRING.optionalFieldOf("material_selector")
                                .forGetter(ItemResource::materialSelector),
                        Codec.BOOL.optionalFieldOf("optional", false)
                                .forGetter(ItemResource::optional),
                        Codec.unboundedMap(ResourceLocation.CODEC, Codec.STRING)
                                .optionalFieldOf("string_components", Map.of())
                                .forGetter(ItemResource::stringComponents),
                        ItemInputAction.CODEC.optionalFieldOf("input_action")
                                .forGetter(ItemResource::inputAction)
                ).apply(instance, ItemResource::new));

        public ItemResource {
            prefix = prefix == null ? Optional.empty() : prefix;
            item = item == null ? Optional.empty() : item;
            tag = tag == null ? Optional.empty() : tag;
            count = Objects.requireNonNull(count, "count");
            chance = Objects.requireNonNull(chance, "chance");
            materialSelector = materialSelector == null ? Optional.empty() : materialSelector;
            stringComponents = Map.copyOf(stringComponents);
            inputAction = inputAction == null ? Optional.empty() : inputAction;
            if ((prefix.isPresent() ? 1 : 0)
                    + (item.isPresent() ? 1 : 0)
                    + (tag.isPresent() ? 1 : 0) != 1) {
                throw new IllegalArgumentException(
                        "Item resource must declare exactly one of prefix, item, or tag");
            }
            materialSelector.ifPresent(MaterialRule::requireMaterialSelector);
            if ((item.isPresent() || tag.isPresent())
                    && materialSelector.isPresent()) {
                throw new IllegalArgumentException(
                        "Fixed item/tag resources cannot select another material");
            }
            if (!item.isPresent() && !stringComponents.isEmpty()) {
                throw new IllegalArgumentException(
                        "String component predicates require a fixed item resource");
            }
            stringComponents.forEach((componentId, value) -> {
                if (!ComponentIngredientIndex.isIndexableStringComponent(componentId)) {
                    throw new IllegalArgumentException(
                            "Unsupported indexed string component: " + componentId);
                }
                if (!value.equals("$material") && !value.matches("[a-z0-9_]+")) {
                    throw new IllegalArgumentException(
                            "Invalid string component value: " + value);
                }
            });
        }

        public ItemResource(
                Optional<String> prefix,
                Optional<ResourceLocation> item,
                String count,
                String chance,
                Optional<String> materialSelector,
                boolean optional,
                Map<ResourceLocation, String> stringComponents,
                Optional<ItemInputAction> inputAction) {
            this(
                    prefix,
                    item,
                    Optional.empty(),
                    count,
                    chance,
                    materialSelector,
                    optional,
                    stringComponents,
                    inputAction);
        }

        public ItemResource(
                Optional<String> prefix,
                Optional<ResourceLocation> item,
                String count,
                String chance,
                Optional<String> materialSelector,
                boolean optional) {
            this(
                    prefix,
                    item,
                    Optional.empty(),
                    count,
                    chance,
                    materialSelector,
                    optional,
                    Map.of(),
                    Optional.empty());
        }

        public ItemResource(
                Optional<String> prefix,
                Optional<ResourceLocation> item,
                String count,
                String chance) {
            this(
                    prefix,
                    item,
                    Optional.empty(),
                    count,
                    chance,
                    Optional.empty(),
                    false,
                    Map.of(),
                    Optional.empty());
        }
    }

    public record FluidResource(
            Optional<String> prefix,
            Optional<ResourceLocation> fluid,
            Optional<String> materialFluid,
            String amount) {
        public static final MapCodec<FluidResource> CODEC =
                RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Codec.STRING.optionalFieldOf("prefix").forGetter(FluidResource::prefix),
                        ResourceLocation.CODEC.optionalFieldOf("fluid").forGetter(FluidResource::fluid),
                        Codec.STRING.optionalFieldOf("material_fluid")
                                .forGetter(FluidResource::materialFluid),
                        Codec.STRING.optionalFieldOf("amount", "1").forGetter(FluidResource::amount)
                ).apply(instance, FluidResource::new));

        public FluidResource {
            prefix = prefix == null ? Optional.empty() : prefix;
            fluid = fluid == null ? Optional.empty() : fluid;
            materialFluid = materialFluid == null ? Optional.empty() : materialFluid;
            amount = Objects.requireNonNull(amount, "amount");
            long selectors = java.util.stream.Stream.of(
                            prefix, fluid, materialFluid)
                    .filter(Optional::isPresent)
                    .count();
            if (selectors != 1) {
                throw new IllegalArgumentException(
                        "Fluid resource must declare exactly one of prefix, fluid, "
                                + "or material_fluid");
            }
            if (materialFluid.isPresent()
                    && !java.util.Set.of("chemical", "molten")
                            .contains(materialFluid.orElseThrow())) {
                throw new IllegalArgumentException(
                        "Unsupported material_fluid selector: "
                                + materialFluid.orElseThrow());
            }
        }

        public FluidResource(
                Optional<String> prefix,
                Optional<ResourceLocation> fluid,
                String amount) {
            this(prefix, fluid, Optional.empty(), amount);
        }
    }

    public record MaterialOverride(
            Optional<String> duration,
            Optional<String> eut,
            Optional<String> specialValue,
            Map<String, String> itemInputCounts,
            Map<String, String> itemOutputCounts,
            Map<String, String> outputChances,
            Map<String, String> fluidAmounts) {
        public static final MapCodec<MaterialOverride> CODEC =
                RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Codec.STRING.optionalFieldOf("duration").forGetter(MaterialOverride::duration),
                        Codec.STRING.optionalFieldOf("eut").forGetter(MaterialOverride::eut),
                        Codec.STRING.optionalFieldOf("special_value").forGetter(MaterialOverride::specialValue),
                        Codec.unboundedMap(Codec.STRING, Codec.STRING)
                                .optionalFieldOf("item_input_counts", Map.of())
                                .forGetter(MaterialOverride::itemInputCounts),
                        Codec.unboundedMap(Codec.STRING, Codec.STRING)
                                .optionalFieldOf("item_output_counts", Map.of())
                                .forGetter(MaterialOverride::itemOutputCounts),
                        Codec.unboundedMap(Codec.STRING, Codec.STRING)
                                .optionalFieldOf("output_chances", Map.of())
                                .forGetter(MaterialOverride::outputChances),
                        Codec.unboundedMap(Codec.STRING, Codec.STRING)
                                .optionalFieldOf("fluid_amounts", Map.of())
                                .forGetter(MaterialOverride::fluidAmounts)
                ).apply(instance, MaterialOverride::new));

        public MaterialOverride {
            duration = duration == null ? Optional.empty() : duration;
            eut = eut == null ? Optional.empty() : eut;
            specialValue = specialValue == null ? Optional.empty() : specialValue;
            itemInputCounts = Map.copyOf(itemInputCounts);
            itemOutputCounts = Map.copyOf(itemOutputCounts);
            outputChances = Map.copyOf(outputChances);
            fluidAmounts = Map.copyOf(fluidAmounts);
        }

        public static MaterialOverride empty() {
            return new MaterialOverride(
                    Optional.empty(), Optional.empty(), Optional.empty(),
                    Map.of(), Map.of(), Map.of(), Map.of());
        }
    }

    /** Exact per-material relation table used only by proven sparse families. */
    public record SparseTable(
            ResourceLocation shapeItem,
            String shape,
            int shapeMeta,
            String templateId,
            String heatMode,
            List<SparseRelation> relations) {
        public static final MapCodec<SparseTable> CODEC =
                RecordCodecBuilder.mapCodec(instance -> instance.group(
                        ResourceLocation.CODEC.fieldOf("shape_item")
                                .forGetter(SparseTable::shapeItem),
                        Codec.STRING.fieldOf("shape").forGetter(SparseTable::shape),
                        Codec.INT.fieldOf("shape_meta").forGetter(SparseTable::shapeMeta),
                        Codec.STRING.fieldOf("template_id").forGetter(SparseTable::templateId),
                        Codec.STRING.fieldOf("heat_mode").forGetter(SparseTable::heatMode),
                        SparseRelation.CODEC.codec().listOf().fieldOf("relations")
                                .forGetter(SparseTable::relations)
                ).apply(instance, SparseTable::new));

        public SparseTable {
            Objects.requireNonNull(shapeItem, "shapeItem");
            requireMaterialId(shape, "sparse shape");
            if (shapeMeta <= 0) {
                throw new IllegalArgumentException("Sparse shape_meta must be positive");
            }
            if (!templateId.matches("sha256:[0-9a-f]{64}")) {
                throw new IllegalArgumentException(
                        "Invalid sparse template_id: " + templateId);
            }
            requireHeatMode(heatMode);
            relations = List.copyOf(relations);
            if (relations.isEmpty()) {
                throw new IllegalArgumentException(
                        "A sparse material rule requires exact relations");
            }
            java.util.HashSet<ResourceLocation> ids = new java.util.HashSet<>();
            java.util.HashSet<String> materials = new java.util.HashSet<>();
            int previousOrder = -1;
            for (SparseRelation relation : relations) {
                if (!relation.heatMode().equals(heatMode)) {
                    throw new IllegalArgumentException(
                            "Sparse relation heat_mode differs from its table");
                }
                if (!ids.add(relation.stableId())) {
                    throw new IllegalArgumentException(
                            "Duplicate sparse stable id " + relation.stableId());
                }
                if (!materials.add(relation.material())) {
                    throw new IllegalArgumentException(
                            "Ambiguous sparse material " + relation.material());
                }
                if (relation.shadowOrder() <= previousOrder) {
                    throw new IllegalArgumentException(
                            "Sparse shadow_order must be strictly increasing at "
                                    + relation.stableId());
                }
                previousOrder = relation.shadowOrder();
            }
        }

        public Optional<SparseRelation> findMatch(
                String material,
                String inputPrefix,
                int inputCount) {
            List<SparseRelation> matches = relations.stream()
                    .filter(relation -> relation.material().equals(material))
                    .filter(relation -> relation.input().prefix().equals(inputPrefix))
                    .filter(relation -> relation.input().count() == inputCount)
                    .toList();
            if (matches.size() > 1) {
                throw new IllegalStateException(
                        "Ambiguous sparse relation match for " + material);
            }
            return matches.stream().findFirst();
        }
    }

    public record SparseResource(String prefix, int count) {
        public static final MapCodec<SparseResource> CODEC =
                RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Codec.STRING.fieldOf("prefix").forGetter(SparseResource::prefix),
                        Codec.INT.fieldOf("count").forGetter(SparseResource::count)
                ).apply(instance, SparseResource::new));

        public SparseResource {
            requireMaterialId(prefix, "sparse resource prefix");
            if (count <= 0 || count > 64) {
                throw new IllegalArgumentException(
                        "Sparse resource count must be in [1, 64]");
            }
        }
    }

    public record SparseRelation(
            ResourceLocation stableId,
            String material,
            SparseResource input,
            SparseResource output,
            int duration,
            long eut,
            String fallback,
            Optional<String> forgingTarget,
            boolean plateGem,
            String heatMode,
            int shadowOrder) {
        public static final MapCodec<SparseRelation> CODEC =
                RecordCodecBuilder.mapCodec(instance -> instance.group(
                        ResourceLocation.CODEC.fieldOf("stable_id")
                                .forGetter(SparseRelation::stableId),
                        Codec.STRING.fieldOf("material").forGetter(SparseRelation::material),
                        SparseResource.CODEC.codec().fieldOf("input")
                                .forGetter(SparseRelation::input),
                        SparseResource.CODEC.codec().fieldOf("output")
                                .forGetter(SparseRelation::output),
                        Codec.INT.fieldOf("duration").forGetter(SparseRelation::duration),
                        Codec.LONG.fieldOf("eut").forGetter(SparseRelation::eut),
                        Codec.STRING.fieldOf("fallback").forGetter(SparseRelation::fallback),
                        Codec.STRING.optionalFieldOf("forging_target")
                                .forGetter(SparseRelation::forgingTarget),
                        Codec.BOOL.fieldOf("plate_gem").forGetter(SparseRelation::plateGem),
                        Codec.STRING.fieldOf("heat_mode").forGetter(SparseRelation::heatMode),
                        Codec.INT.fieldOf("shadow_order").forGetter(SparseRelation::shadowOrder)
                ).apply(instance, SparseRelation::new));

        public SparseRelation {
            Objects.requireNonNull(stableId, "stableId");
            requireMaterialId(material, "sparse material");
            Objects.requireNonNull(input, "input");
            Objects.requireNonNull(output, "output");
            if (duration <= 0 || eut <= 0) {
                throw new IllegalArgumentException(
                        "Sparse duration and EU/t must be positive");
            }
            if (!Set.of("none", "dust").contains(fallback)
                    || fallback.equals("dust") != input.prefix().equals("dust")) {
                throw new IllegalArgumentException(
                        "Sparse fallback must exactly describe its input prefix");
            }
            forgingTarget = forgingTarget == null ? Optional.empty() : forgingTarget;
            forgingTarget.ifPresent(value ->
                    requireMaterialId(value, "sparse forging target"));
            requireHeatMode(heatMode);
            if (shadowOrder < 0) {
                throw new IllegalArgumentException(
                        "Sparse shadow_order cannot be negative for "
                                + stableId);
            }
            String expectedSuffix = "/" + material;
            if (!stableId.getPath().startsWith("extruder/")
                    || !stableId.getPath().endsWith(expectedSuffix)) {
                throw new IllegalArgumentException(
                        "Sparse stable id does not end in its material: " + stableId);
            }
        }
    }

    private static void requireHeatMode(String heatMode) {
        if (!Set.of("normal", "low_heat").contains(heatMode)) {
            throw new IllegalArgumentException(
                    "Invalid sparse heat_mode: " + heatMode);
        }
    }

    public record Tuning(
            String material,
            Optional<Integer> tier,
            Optional<String> color,
            Optional<Double> meltingPoint,
            Optional<Double> boilingPoint,
            Optional<Double> density) {
        public static final MapCodec<Tuning> CODEC =
                RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Codec.STRING.fieldOf("material").forGetter(Tuning::material),
                        Codec.INT.optionalFieldOf("tier").forGetter(Tuning::tier),
                        Codec.STRING.optionalFieldOf("color").forGetter(Tuning::color),
                        Codec.DOUBLE.optionalFieldOf("melting_point").forGetter(Tuning::meltingPoint),
                        Codec.DOUBLE.optionalFieldOf("boiling_point").forGetter(Tuning::boilingPoint),
                        Codec.DOUBLE.optionalFieldOf("density").forGetter(Tuning::density)
                ).apply(instance, Tuning::new));

        public Tuning {
            requireMaterialId(material, "tuning material");
            tier = tier == null ? Optional.empty() : tier;
            color = color == null ? Optional.empty() : color;
            meltingPoint = meltingPoint == null ? Optional.empty() : meltingPoint;
            boilingPoint = boilingPoint == null ? Optional.empty() : boilingPoint;
            density = density == null ? Optional.empty() : density;
        }
    }

    public record UnificationPreference(
            String material,
            String prefix,
            ResourceLocation item,
            int priority) {
        public static final MapCodec<UnificationPreference> CODEC =
                RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Codec.STRING.fieldOf("material").forGetter(UnificationPreference::material),
                        Codec.STRING.fieldOf("prefix").forGetter(UnificationPreference::prefix),
                        ResourceLocation.CODEC.fieldOf("item").forGetter(UnificationPreference::item),
                        Codec.INT.optionalFieldOf("priority", 0).forGetter(UnificationPreference::priority)
                ).apply(instance, UnificationPreference::new));

        public UnificationPreference {
            requireMaterialId(material, "unification material");
            Objects.requireNonNull(prefix, "prefix");
            Objects.requireNonNull(item, "item");
        }
    }
}
