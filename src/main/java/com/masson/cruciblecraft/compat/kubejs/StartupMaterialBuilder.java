package com.masson.cruciblecraft.compat.kubejs;

import java.util.Optional;

import com.masson.cruciblecraft.material.def.MaterialTuning;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StartupMaterialBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(StartupMaterialBuilder.class);
    private final String id;
    private Integer tier;
    private String color;
    private Double meltingPoint;
    private Double boilingPoint;
    private Double density;

    public StartupMaterialBuilder(String id) {
        this.id = id;
    }

    public StartupMaterialBuilder tagName(String tagName) {
        return structuralOverride("tagName");
    }

    public StartupMaterialBuilder nameKey(String nameKey) {
        return structuralOverride("nameKey");
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
        return structuralOverride("tintStyle");
    }

    public StartupMaterialBuilder forms(String... forms) {
        return structuralOverride("forms");
    }

    public StartupMaterialBuilder formItem(String form, String itemId) {
        return structuralOverride("formItem");
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
        return structuralOverride("moltenFluid");
    }

    public StartupMaterialBuilder component(String materialId, int parts) {
        return structuralOverride("component");
    }

    public StartupMaterialBuilder noDecompose(boolean noDecompose) {
        return structuralOverride("noDecompose");
    }

    public MaterialTuning build() {
        return new MaterialTuning(
                id,
                Optional.ofNullable(tier),
                Optional.ofNullable(color),
                Optional.ofNullable(meltingPoint),
                Optional.ofNullable(boilingPoint),
                Optional.ofNullable(density));
    }

    private StartupMaterialBuilder structuralOverride(String field) {
        LOGGER.warn(
                "Ignoring KubeJS material field '{}' for {}; registry shape must come from bundled or addon-mod definitions",
                field,
                id);
        return this;
    }
}
