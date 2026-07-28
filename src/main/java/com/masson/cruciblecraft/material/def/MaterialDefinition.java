package com.masson.cruciblecraft.material.def;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.api.material.MaterialForm;
import com.masson.cruciblecraft.material.MaterialColors;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record MaterialDefinition(
        String id,
        String tagName,
        Optional<String> nameKey,
        int tier,
        String color,
        String tintStyle,
        List<MaterialForm> forms,
        Map<MaterialForm, String> formItems,
        ThermalProperties thermal,
        boolean moltenFluid,
        Map<String, Integer> composition,
        boolean noDecompose) {
    private static final java.util.Set<String> TINT_STYLES =
            java.util.Set.of("metallic", "matte", "shiny");
    private static final Codec<String> COLOR_CODEC = Codec.STRING.comapFlatMap(
            color -> MaterialColors.isValid(color)
                    ? DataResult.success(color)
                    : DataResult.error(() ->
                            "Material color must use strict #RRGGBB format: " + color),
            color -> color);

    public static final Codec<MaterialDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(MaterialDefinition::id),
            Codec.STRING.optionalFieldOf("tag_name", "").forGetter(MaterialDefinition::tagName),
            Codec.STRING.optionalFieldOf("name_key").forGetter(MaterialDefinition::nameKey),
            Codec.INT.optionalFieldOf("tier", 0).forGetter(MaterialDefinition::tier),
            COLOR_CODEC.optionalFieldOf("color", "#FFFFFF").forGetter(MaterialDefinition::color),
            Codec.STRING.optionalFieldOf("tint_style", "metallic").forGetter(MaterialDefinition::tintStyle),
            MaterialForm.CODEC.listOf().optionalFieldOf("forms", List.of(MaterialForm.DUST))
                    .forGetter(MaterialDefinition::forms),
            Codec.unboundedMap(MaterialForm.CODEC, Codec.STRING)
                    .optionalFieldOf("form_items", Map.of())
                    .forGetter(MaterialDefinition::formItems),
            ThermalProperties.CODEC.fieldOf("thermal").forGetter(MaterialDefinition::thermal),
            Codec.BOOL.optionalFieldOf("molten_fluid", false).forGetter(MaterialDefinition::moltenFluid),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("composition", Map.of())
                    .forGetter(MaterialDefinition::composition),
            Codec.BOOL.optionalFieldOf("no_decompose", false).forGetter(MaterialDefinition::noDecompose)
    ).apply(instance, MaterialDefinition::new));

    public MaterialDefinition {
        if (!id.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException("Invalid material id: " + id);
        }
        color = MaterialColors.requireValid(color);
        tagName = tagName.isBlank() ? id : tagName;
        forms = List.copyOf(forms);
        formItems = Map.copyOf(formItems);
        composition = Map.copyOf(composition);
        if (forms.isEmpty()) {
            throw new IllegalArgumentException("Material " + id + " must define at least one form");
        }
        if (forms.stream().distinct().count() != forms.size()) {
            throw new IllegalArgumentException("Material " + id + " contains duplicate forms");
        }
        if (!forms.containsAll(formItems.keySet())) {
            throw new IllegalArgumentException("Material " + id + " overrides an unavailable form");
        }
        if (!TINT_STYLES.contains(tintStyle)) {
            throw new IllegalArgumentException(
                    "Material " + id + " has unsupported tint_style: " + tintStyle);
        }
        if (formItems.values().stream()
                .anyMatch(item -> !item.matches("[a-z0-9_.-]+:[a-z0-9/._-]+"))) {
            throw new IllegalArgumentException("Material " + id + " has an invalid form item id");
        }
        if (!Double.isFinite(thermal.meltingPoint()) || thermal.meltingPoint() < 0.0) {
            throw new IllegalArgumentException("Invalid thermal properties for " + id);
        }
        if (!Double.isFinite(thermal.density()) || thermal.density() <= 0.0) {
            throw new IllegalArgumentException("Material " + id + " must have a finite positive density");
        }
        if (!Double.isFinite(thermal.boilingPoint())
                || thermal.boilingPoint() <= thermal.meltingPoint()) {
            throw new IllegalArgumentException(
                    "Material " + id + " boiling_point must exceed melting_point");
        }
        if (composition.values().stream().anyMatch(value -> value <= 0)) {
            throw new IllegalArgumentException("Composition ratios must be positive for " + id);
        }
    }

    public String registryName(MaterialForm form) {
        return id + "_" + form.serializedName();
    }

    public String translationKey() {
        return nameKey.orElse("material.cruciblecraft." + id);
    }
}
