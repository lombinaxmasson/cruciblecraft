package com.masson.cruciblecraft.machine.processing;

import java.util.Objects;

import com.masson.cruciblecraft.api.energy.EnergyType;

import net.minecraft.resources.ResourceLocation;

/**
 * Resolves acquisition template and casing from catalogs. Java adapters are
 * keyed by template, never by variant path.
 */
public final class MachineAcquisition {
    public static Resolved resolve(MachineVariant variant) {
        return resolve(
                variant,
                MachineKindCatalog.bundled(),
                MachineCasingCatalog.bundled(),
                MachineAcquisitionCatalog.bundled());
    }

    public static Resolved resolve(
            MachineVariant variant,
            MachineKindCatalog kinds,
            MachineCasingCatalog casings,
            MachineAcquisitionCatalog acquisition) {
        Objects.requireNonNull(variant, "variant");
        return resolve(
                variant.id(),
                variant.kind().id(),
                variant.tierBand().materialId(),
                variant.tierBand().energyType(),
                kinds,
                casings,
                acquisition);
    }

    public static Resolved resolve(
            ResourceLocation variantId,
            ResourceLocation kindId,
            String materialId,
            EnergyType energy,
            MachineKindCatalog kinds,
            MachineCasingCatalog casings,
            MachineAcquisitionCatalog acquisition) {
        Objects.requireNonNull(variantId, "variantId");
        MachineKindCatalog.Kind kind = kinds.requireKind(kindId);
        MachineAcquisitionCatalog.Override override =
                acquisition.overrideOf(variantId);
        String template = CatalogJson.nonBlank(override.acquisitionTemplate())
                ? override.acquisitionTemplate()
                : kind.acquisitionTemplate();
        if (!CatalogJson.nonBlank(template) || "none".equals(template)) {
            throw new IllegalStateException(
                    "T36 variant " + variantId.getPath()
                            + " has no source-backed acquisition");
        }
        if ("machine_generic".equals(template)) {
            return new Resolved(
                    variantId,
                    kindId,
                    template,
                    MachineCasingCatalog.materialPath(materialId),
                    null,
                    null,
                    null);
        }
        ResourceLocation casingItem = override.casingItem();
        if (casingItem == null) {
            String family = MachineCasingCatalog.energyFamily(energy);
            if (family == null) {
                throw new IllegalStateException(
                        "No kinetic/heat casing for "
                                + MachineCasingCatalog.materialPath(materialId));
            }
            casingItem = casings.requireCasing(
                    materialId, family).id();
        }
        String materialPath = MachineCasingCatalog.materialPath(materialId);
        String cable = null;
        MachineCasingCatalog.DistilleryWire wire = null;
        if ("electrolyzer".equals(template)) {
            String casingMaterial = MachineCasingCatalog.materialPath(
                    casings.requireCasing(casingItem).materialId().toString());
            cable = casings.electrolyzerCableMaterial(casingMaterial);
        }
        if ("t17_heat".equals(template) && "distillery".equals(kindId.getPath())) {
            wire = casings.distilleryWire(materialPath);
        }
        return new Resolved(
                variantId,
                kindId,
                template,
                materialPath,
                casingItem,
                cable,
                wire);
    }

    public record Resolved(
            ResourceLocation variantId,
            ResourceLocation kindId,
            String template,
            String materialPath,
            ResourceLocation casingItem,
            String electrolyzerCableMaterial,
            MachineCasingCatalog.DistilleryWire distilleryWire) {
        public Resolved {
            Objects.requireNonNull(variantId, "variantId");
            Objects.requireNonNull(kindId, "kindId");
            Objects.requireNonNull(template, "template");
            Objects.requireNonNull(materialPath, "materialPath");
        }

        public String t16Kind() {
            String kind = kindId.getPath();
            if ("bronze_crusher".equals(kind)) {
                return "shredder";
            }
            return kind;
        }
    }

    private MachineAcquisition() {}
}
