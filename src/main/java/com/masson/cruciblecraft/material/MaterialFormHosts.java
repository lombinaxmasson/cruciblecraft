package com.masson.cruciblecraft.material;

import java.util.Set;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.redstonewire.RedstoneWireKind;
import com.masson.cruciblecraft.energy.cable.ElectricalConductorCatalog;
import com.masson.cruciblecraft.logistics.pipe.PipeCatalog;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;

/**
 * Splits live material forms into unique hosted BlockItems, public-exchange
 * unique Items, and shared inventory prefix items. Call after pipe/conductor
 * catalogs initialize.
 */
public final class MaterialFormHosts {
    /**
     * Frozen public-exchange prefix paths. Rewrite cards must not add or
     * remove entries without a new DESIGN_POLICY confirmation.
     */
    public static final Set<String> PUBLIC_EXCHANGE_PREFIX_PATHS = Set.of(
            "ingot",
            "nugget",
            "dust",
            "small_dust",
            "tiny_dust",
            "plate",
            "rod",
            "long_rod",
            "bolt",
            "screw",
            "ring",
            "gear",
            "small_gear",
            "gem",
            "foil",
            "fine_wire");

    private MaterialFormHosts() {}

    public static boolean isPublicExchangePrefix(MaterialPrefix form) {
        return form != null && isPublicExchangePrefixPath(form.serializedName());
    }

    public static boolean isPublicExchangePrefixPath(String path) {
        return path != null && PUBLIC_EXCHANGE_PREFIX_PATHS.contains(path);
    }

    public static boolean isSharedInventoryForm(
            MaterialDefinition material, MaterialPrefix form) {
        if (form.equals(MaterialPrefixes.ORE)
                || material.formItems().containsKey(form)
                || isPublicExchangePrefix(form)
                || !MaterialCatalog.isFormRegistered(material, form)) {
            return false;
        }
        return !isUniqueHostedForm(material, form);
    }

    public static boolean isUniqueInventoryForm(
            MaterialDefinition material, MaterialPrefix form) {
        if (!isPublicExchangePrefix(form)
                || material.formItems().containsKey(form)
                || !MaterialCatalog.isFormRegistered(material, form)) {
            return false;
        }
        return !isUniqueHostedForm(material, form);
    }

    public static boolean isUniqueHostedForm(
            MaterialDefinition material, MaterialPrefix form) {
        if (form.equals(MaterialPrefixes.ORE)) {
            return true;
        }
        if (isPlaceableStorage(material, form)
                || isPlaceableCasing(material, form)
                || isRock(material, form)
                || RedstoneWireKind.owns(material.id(), form)
                || isElectricalConductor(material, form)
                || isPipe(material, form)) {
            return true;
        }
        return false;
    }

    public static String prefixItemPath(MaterialPrefix form) {
        return form.serializedName();
    }

    /**
     * Prefix-level unique-hosted denylist used by mill emit and EMI
     * projections. Live registration still uses {@link #isUniqueHostedForm}
     * after catalogs initialize.
     */
    public static boolean isUniqueHostedPrefixPath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        if (path.equals("ore")
                || path.equals("block")
                || path.equals("storage_dust")
                || path.equals("rock")
                || path.equals("machine_casing")
                || path.equals("machine_casing_double")
                || path.equals("machine_casing_quadruple")
                || path.equals("machine_casing_dense")) {
            return true;
        }
        if (path.equals("fine_wire")) {
            return false;
        }
        return path.equals("wire")
                || path.equals("cable")
                || path.endsWith("_wire")
                || path.endsWith("_cable")
                || path.endsWith("_pipe");
    }

    private static boolean isPlaceableStorage(
            MaterialDefinition material, MaterialPrefix form) {
        return (form.equals(MaterialPrefixes.BLOCK)
                        || form.equals(MaterialPrefixes.STORAGE_DUST))
                && !material.formItems().containsKey(form)
                && MaterialCatalog.isFormRegistered(material, form);
    }

    private static boolean isPlaceableCasing(
            MaterialDefinition material, MaterialPrefix form) {
        return (form.equals(MaterialPrefixes.MACHINE_CASING)
                || form.equals(MaterialPrefixes.MACHINE_CASING_DOUBLE)
                || form.equals(MaterialPrefixes.MACHINE_CASING_QUADRUPLE)
                || form.equals(MaterialPrefixes.MACHINE_CASING_DENSE))
                && !material.formItems().containsKey(form)
                && MaterialCatalog.isFormRegistered(material, form);
    }

    private static boolean isRock(MaterialDefinition material, MaterialPrefix form) {
        MaterialPrefix rock = MaterialPrefixCatalog.require("rock");
        return form.equals(rock)
                && !material.formItems().containsKey(form)
                && MaterialCatalog.isFormRegistered(material, form);
    }

    private static boolean isElectricalConductor(
            MaterialDefinition material, MaterialPrefix form) {
        if (ElectricalConductorCatalog.isInitialized()) {
            return ElectricalConductorCatalog.contains(material.id(), form);
        }
        String specification = ElectricalConductorCatalog.specificationFor(form);
        if (specification == null) {
            return false;
        }
        return material.gt6Metadata()
                .map(metadata -> metadata.electricalBySpecification()
                        .containsKey(specification))
                .orElse(false);
    }

    private static boolean isPipe(MaterialDefinition material, MaterialPrefix form) {
        if (PipeCatalog.isInitialized()) {
            return PipeCatalog.contains(
                            material.id(), form, PipeCatalog.Kind.FLUID)
                    || PipeCatalog.contains(
                            material.id(), form, PipeCatalog.Kind.ITEM);
        }
        return false;
    }
}
