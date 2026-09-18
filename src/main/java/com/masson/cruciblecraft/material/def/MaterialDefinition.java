package com.masson.cruciblecraft.material.def;

import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.material.MaterialColors;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Immutable material definition with prefix structure resolved at startup. */
public final class MaterialDefinition {
    private static final java.util.Set<String> TINT_STYLES =
            java.util.Set.of("metallic", "matte", "shiny");
    private static final Codec<String> COLOR_CODEC = Codec.STRING.comapFlatMap(
            color -> MaterialColors.isValid(color)
                    ? DataResult.success(color)
                    : DataResult.error(() ->
                            "Material color must use strict #RRGGBB format: " + color),
            color -> color);

    private static final MapCodec<MaterialDefinition> BASE_CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.STRING.fieldOf("id").forGetter(MaterialDefinition::id),
                    Codec.STRING.optionalFieldOf("tag_name", "")
                            .forGetter(MaterialDefinition::tagName),
                    Codec.STRING.optionalFieldOf("name_key")
                            .forGetter(MaterialDefinition::nameKey),
                    Codec.INT.optionalFieldOf("tier", 0).forGetter(MaterialDefinition::tier),
                    COLOR_CODEC.optionalFieldOf("color", "#FFFFFF")
                            .forGetter(MaterialDefinition::color),
                    Codec.STRING.optionalFieldOf("tint_style", "metallic")
                            .forGetter(MaterialDefinition::tintStyle),
                    Codec.STRING.listOf().optionalFieldOf("generation_flags", List.of())
                            .forGetter(MaterialDefinition::generationFlagIds),
                    Codec.STRING.listOf().optionalFieldOf("include_prefixes", List.of())
                            .forGetter(MaterialDefinition::includedPrefixIds),
                    Codec.STRING.listOf().optionalFieldOf("exclude_prefixes", List.of())
                            .forGetter(MaterialDefinition::excludedPrefixIds),
                    Codec.STRING.listOf().optionalFieldOf("forms", List.of("dust"))
                            .forGetter(MaterialDefinition::legacyFormsForEncoding),
                    Codec.unboundedMap(Codec.STRING, Codec.STRING)
                            .optionalFieldOf("form_items", Map.of())
                            .forGetter(MaterialDefinition::formItemIds),
                    ThermalProperties.CODEC.fieldOf("thermal")
                            .forGetter(MaterialDefinition::thermal),
                    Codec.BOOL.optionalFieldOf("molten_fluid", false)
                            .forGetter(MaterialDefinition::moltenFluid),
                    Codec.unboundedMap(Codec.STRING, Codec.INT)
                            .optionalFieldOf("composition", Map.of())
                            .forGetter(MaterialDefinition::composition),
                    Codec.BOOL.optionalFieldOf("no_decompose", false)
                            .forGetter(MaterialDefinition::noDecompose),
                    Codec.BOOL.optionalFieldOf("metadata_only", false)
                            .forGetter(MaterialDefinition::metadataOnly)
            ).apply(instance, MaterialDefinition::decode));
    private static final MapCodec<MaterialDefinition> FULL_CODEC =
            new MapCodec<>() {
                @Override
                public <T> DataResult<MaterialDefinition> decode(
                        DynamicOps<T> ops, MapLike<T> input) {
                    return BASE_CODEC.decode(ops, input).flatMap(definition -> {
                        T metadataValue = input.get("gt6_metadata");
                        if (metadataValue == null) {
                            return DataResult.success(definition);
                        }
                        return GT6MaterialMetadata.CODEC.parse(ops, metadataValue)
                                .map(definition::withGT6Metadata);
                    });
                }

                @Override
                public <T> RecordBuilder<T> encode(
                        MaterialDefinition input,
                        DynamicOps<T> ops,
                        RecordBuilder<T> prefix) {
                    RecordBuilder<T> result = BASE_CODEC.encode(input, ops, prefix);
                    input.gt6Metadata.ifPresent(metadata ->
                            result.add(
                                    "gt6_metadata",
                                    GT6MaterialMetadata.CODEC.encodeStart(ops, metadata)));
                    return result;
                }

                @Override
                public <T> Stream<T> keys(DynamicOps<T> ops) {
                    return Stream.concat(
                            BASE_CODEC.keys(ops),
                            Stream.of(ops.createString("gt6_metadata")));
                }
            };
    public static final Codec<MaterialDefinition> CODEC = FULL_CODEC.codec();

    private final String id;
    private final String tagName;
    private final Optional<String> nameKey;
    private final int tier;
    private final String color;
    private final int colorRgb;
    private final String tintStyle;
    private final List<String> generationFlagIds;
    private final BitSet generationFlags;
    private final List<String> includedPrefixIds;
    private final List<String> excludedPrefixIds;
    private final List<MaterialPrefix> forms;
    private final Map<MaterialPrefix, String> formItems;
    private final ThermalProperties thermal;
    private final boolean moltenFluid;
    private final Map<String, Integer> composition;
    private final boolean noDecompose;
    private final boolean metadataOnly;
    private final Optional<GT6MaterialMetadata> gt6Metadata;

    private static MaterialDefinition decode(
            String id,
            String tagName,
            Optional<String> nameKey,
            int tier,
            String color,
            String tintStyle,
            List<String> generationFlags,
            List<String> includePrefixes,
            List<String> excludePrefixes,
            List<String> legacyForms,
            Map<String, String> formItems,
            ThermalProperties thermal,
            boolean moltenFluid,
            Map<String, Integer> composition,
            boolean noDecompose,
            boolean metadataOnly) {
        List<MaterialPrefix> resolved = MaterialPrefixCatalog.resolve(
                generationFlags, includePrefixes, excludePrefixes, legacyForms, metadataOnly);
        LinkedHashMap<MaterialPrefix, String> resolvedItems = new LinkedHashMap<>();
        formItems.forEach((prefix, item) ->
                resolvedItems.put(MaterialPrefixCatalog.require(prefix), item));
        List<String> structuralIncludes = generationFlags.isEmpty()
                        && includePrefixes.isEmpty()
                        && excludePrefixes.isEmpty()
                ? resolved.stream().map(MaterialPrefix::serializedId).toList()
                : includePrefixes;
        return new MaterialDefinition(
                id, tagName, nameKey, tier, color, tintStyle,
                generationFlags, structuralIncludes, excludePrefixes,
                resolved, resolvedItems, thermal, moltenFluid, composition, noDecompose,
                metadataOnly,
                Optional.empty());
    }

    /**
     * Compatibility constructor for Java integrations. Its forms become
     * explicit includes in the new structural schema.
     */
    public MaterialDefinition(
            String id,
            String tagName,
            Optional<String> nameKey,
            int tier,
            String color,
            String tintStyle,
            List<MaterialPrefix> forms,
            Map<MaterialPrefix, String> formItems,
            ThermalProperties thermal,
            boolean moltenFluid,
            Map<String, Integer> composition,
            boolean noDecompose) {
        this(
                id, tagName, nameKey, tier, color, tintStyle,
                List.of(),
                forms.stream().map(MaterialPrefix::serializedId).toList(),
                List.of(),
                forms,
                formItems,
                thermal,
                moltenFluid,
                composition,
                noDecompose,
                false,
                Optional.empty());
    }

    /** Creates a strict metadata-only definition for startup integrations and tooling. */
    public static MaterialDefinition metadataOnly(
            String id,
            String tagName,
            Optional<String> nameKey,
            int tier,
            String color,
            String tintStyle,
            ThermalProperties thermal,
            boolean moltenFluid,
            Map<String, Integer> composition,
            boolean noDecompose) {
        return new MaterialDefinition(
                id,
                tagName,
                nameKey,
                tier,
                color,
                tintStyle,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Map.of(),
                thermal,
                moltenFluid,
                composition,
                noDecompose,
                true,
                Optional.empty());
    }

    private MaterialDefinition(
            String id,
            String tagName,
            Optional<String> nameKey,
            int tier,
            String color,
            String tintStyle,
            List<String> generationFlagIds,
            List<String> includedPrefixIds,
            List<String> excludedPrefixIds,
            List<MaterialPrefix> forms,
            Map<MaterialPrefix, String> formItems,
            ThermalProperties thermal,
            boolean moltenFluid,
            Map<String, Integer> composition,
            boolean noDecompose,
            boolean metadataOnly,
            Optional<GT6MaterialMetadata> gt6Metadata) {
        if (id == null || !id.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException("Invalid material id: " + id);
        }
        this.id = id;
        this.tagName = tagName == null || tagName.isBlank() ? id : tagName;
        this.nameKey = nameKey == null ? Optional.empty() : nameKey;
        this.tier = tier;
        this.color = MaterialColors.requireValid(color);
        this.colorRgb = Integer.parseInt(this.color.substring(1), 16);
        this.tintStyle = tintStyle;
        this.generationFlagIds = List.copyOf(generationFlagIds);
        this.generationFlags = MaterialPrefixCatalog.compileGenerationFlags(generationFlagIds);
        this.includedPrefixIds = List.copyOf(includedPrefixIds);
        this.excludedPrefixIds = List.copyOf(excludedPrefixIds);
        this.forms = List.copyOf(forms);
        this.formItems = Map.copyOf(formItems);
        this.thermal = thermal;
        this.moltenFluid = moltenFluid;
        this.composition = Map.copyOf(composition);
        this.noDecompose = noDecompose;
        this.metadataOnly = metadataOnly;
        this.gt6Metadata = gt6Metadata == null ? Optional.empty() : gt6Metadata;
        validate();
    }

    private void validate() {
        if (forms.isEmpty() != metadataOnly) {
            if (metadataOnly) {
                throw new IllegalArgumentException(
                        "Metadata-only material " + id + " must not define prefixes");
            }
            throw new IllegalArgumentException("Material " + id + " must define at least one prefix");
        }
        if (metadataOnly && (!generationFlagIds.isEmpty()
                || !includedPrefixIds.isEmpty()
                || !excludedPrefixIds.isEmpty()
                || !formItems.isEmpty())) {
            throw new IllegalArgumentException(
                    "Metadata-only material " + id + " must not define item structure");
        }
        if (forms.stream().distinct().count() != forms.size()) {
            throw new IllegalArgumentException("Material " + id + " contains duplicate prefixes");
        }
        if (!forms.containsAll(formItems.keySet())) {
            throw new IllegalArgumentException("Material " + id + " overrides an unavailable prefix");
        }
        if (!TINT_STYLES.contains(tintStyle)) {
            throw new IllegalArgumentException(
                    "Material " + id + " has unsupported tint_style: " + tintStyle);
        }
        if (formItems.values().stream()
                .anyMatch(item -> !item.matches("[a-z0-9_.-]+:[a-z0-9/._-]+"))) {
            throw new IllegalArgumentException("Material " + id + " has an invalid prefix item id");
        }
        if (thermal == null
                || !Double.isFinite(thermal.meltingPoint())
                || !Double.isFinite(thermal.density())
                || thermal.density() <= 0.0
                || !Double.isFinite(thermal.boilingPoint())
                || thermal.boilingPoint() <= thermal.meltingPoint()) {
            throw new IllegalArgumentException("Invalid thermal properties for " + id);
        }
        if (composition.values().stream().anyMatch(value -> value == null || value <= 0)) {
            throw new IllegalArgumentException("Composition ratios must be positive for " + id);
        }
    }

    public String id() { return id; }
    public String tagName() { return tagName; }
    public Optional<String> nameKey() { return nameKey; }
    public int tier() { return tier; }
    public String color() { return color; }
    public int colorRgb() { return colorRgb; }
    public String tintStyle() { return tintStyle; }
    public List<String> generationFlagIds() { return generationFlagIds; }
    public BitSet generationFlags() { return (BitSet) generationFlags.clone(); }
    public List<String> includedPrefixIds() { return includedPrefixIds; }
    public List<String> excludedPrefixIds() { return excludedPrefixIds; }
    public List<MaterialPrefix> forms() { return forms; }
    public Map<MaterialPrefix, String> formItems() { return formItems; }
    public ThermalProperties thermal() { return thermal; }
    public boolean moltenFluid() { return moltenFluid; }
    public Map<String, Integer> composition() { return composition; }
    public boolean noDecompose() { return noDecompose; }
    public boolean metadataOnly() { return metadataOnly; }
    public Optional<GT6MaterialMetadata> gt6Metadata() { return gt6Metadata; }

    /** Returns an immutable structural copy enriched with resolved imported metadata. */
    public MaterialDefinition withImportedMetadata(GT6MaterialMetadata metadata) {
        return withGT6Metadata(java.util.Objects.requireNonNull(metadata, "metadata"));
    }

    public String registryName(MaterialPrefix prefix) {
        return id + "/" + prefix.serializedName();
    }

    public String translationKey() {
        return nameKey.orElse("material.cruciblecraft." + id);
    }

    public boolean hasMaterialTag(String tag) {
        return gt6Metadata()
                .map(metadata -> metadata.materialTags().contains(tag))
                .orElse(false);
    }

    /**
     * GT6 {@code Loader_Recipes_Furnace} only smelts
     * {@code TD.Processing.FURNACE} materials that are not unused.
     */
    public boolean furnaceSmeltable() {
        return hasMaterialTag("PROCESSING.FURNACE")
                && !hasMaterialTag("PROPERTIES.UNUSED_MATERIAL");
    }

    MaterialDefinition withTuning(int tunedTier, String tunedColor, ThermalProperties tunedThermal) {
        return new MaterialDefinition(
                id,
                tagName,
                nameKey,
                tunedTier,
                tunedColor,
                tintStyle,
                generationFlagIds,
                includedPrefixIds,
                excludedPrefixIds,
                forms,
                formItems,
                tunedThermal,
                moltenFluid,
                composition,
                noDecompose,
                metadataOnly,
                gt6Metadata);
    }

    private MaterialDefinition withGT6Metadata(GT6MaterialMetadata metadata) {
        return new MaterialDefinition(
                id,
                tagName,
                nameKey,
                tier,
                color,
                tintStyle,
                generationFlagIds,
                includedPrefixIds,
                excludedPrefixIds,
                forms,
                formItems,
                thermal,
                moltenFluid,
                composition,
                noDecompose,
                metadataOnly,
                Optional.of(metadata));
    }

    private List<String> legacyFormsForEncoding() {
        // Equals the codec default so generated output uses include_prefixes.
        return List.of("dust");
    }

    private Map<String, String> formItemIds() {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        formItems.forEach((prefix, item) -> result.put(prefix.serializedId(), item));
        return Map.copyOf(result);
    }
}
