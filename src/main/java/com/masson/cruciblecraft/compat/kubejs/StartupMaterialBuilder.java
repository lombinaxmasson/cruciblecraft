package com.masson.cruciblecraft.compat.kubejs;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.api.material.MaterialForm;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.material.def.ThermalProperties;

public final class StartupMaterialBuilder {
    private final String id;
    private String tagName;
    private String nameKey;
    private int tier;
    private String color = "#FFFFFF";
    private String tintStyle = "metallic";
    private final List<MaterialForm> forms = new ArrayList<>(List.of(MaterialForm.DUST));
    private final Map<MaterialForm, String> formItems = new LinkedHashMap<>();
    private double meltingPoint;
    private Double boilingPoint;
    private double density = 1.0;
    private boolean moltenFluid;
    private final Map<String, Integer> composition = new LinkedHashMap<>();
    private boolean noDecompose;

    public StartupMaterialBuilder(String id) {
        this.id = id;
        this.tagName = id;
    }

    public StartupMaterialBuilder tagName(String tagName) {
        this.tagName = tagName;
        return this;
    }

    public StartupMaterialBuilder nameKey(String nameKey) {
        this.nameKey = nameKey;
        return this;
    }

    public StartupMaterialBuilder tier(int tier) {
        this.tier = tier;
        return this;
    }

    public StartupMaterialBuilder color(String color) {
        this.color = color;
        return this;
    }

    public StartupMaterialBuilder tintStyle(String tintStyle) {
        this.tintStyle = tintStyle;
        return this;
    }

    public StartupMaterialBuilder forms(String... forms) {
        this.forms.clear();
        for (String form : forms) {
            this.forms.add(MaterialForm.parse(form));
        }
        return this;
    }

    public StartupMaterialBuilder formItem(String form, String itemId) {
        this.formItems.put(MaterialForm.parse(form), itemId);
        return this;
    }

    public StartupMaterialBuilder meltingPoint(double meltingPoint) {
        this.meltingPoint = meltingPoint;
        return this;
    }

    public StartupMaterialBuilder boilingPoint(double boilingPoint) {
        this.boilingPoint = boilingPoint;
        return this;
    }

    public StartupMaterialBuilder density(double density) {
        this.density = density;
        return this;
    }

    public StartupMaterialBuilder moltenFluid(boolean moltenFluid) {
        this.moltenFluid = moltenFluid;
        return this;
    }

    public StartupMaterialBuilder component(String materialId, int parts) {
        this.composition.put(materialId, parts);
        return this;
    }

    public StartupMaterialBuilder noDecompose(boolean noDecompose) {
        this.noDecompose = noDecompose;
        return this;
    }

    public MaterialDefinition build() {
        return new MaterialDefinition(
                id,
                tagName,
                Optional.ofNullable(nameKey),
                tier,
                color,
                tintStyle,
                forms,
                formItems,
                new ThermalProperties(
                        meltingPoint,
                        boilingPoint == null ? meltingPoint * 2.0 : boilingPoint,
                        density),
                moltenFluid,
                composition,
                noDecompose);
    }
}
