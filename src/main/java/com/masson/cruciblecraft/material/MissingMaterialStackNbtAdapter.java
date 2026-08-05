package com.masson.cruciblecraft.material;

import java.util.Map;
import java.util.function.Predicate;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** Minecraft 1.21.1 NBT adapter around the pure migration planner. */
public final class MissingMaterialStackNbtAdapter {
    private static final String COMPONENT_ID = "cruciblecraft:missing_material";
    public static final String TOOL_MATERIAL_COMPONENT_ID =
            MaterialComponentPolicy.TOOL_MATERIAL_COMPONENT_ID;
    public static final String MACHINE_MATERIAL_COMPONENT_ID =
            MaterialComponentPolicy.MACHINE_MATERIAL_COMPONENT_ID;
    public static final String TOOL_COMPONENT_FORM =
            MaterialComponentPolicy.TOOL_COMPONENT_FORM;
    public static final String MACHINE_COMPONENT_FORM =
            MaterialComponentPolicy.MACHINE_COMPONENT_FORM;

    private MissingMaterialStackNbtAdapter() {}

    public static Tag rewrite(Tag input) {
        return input instanceof CompoundTag stackTag ? rewrite(stackTag) : input;
    }

    public static CompoundTag rewrite(CompoundTag input) {
        String itemId = input.getString("id");
        if (itemId.equals(MissingMaterialStackRewriter.UNKNOWN_ITEM_ID)
                || !MaterialCatalog.isBootstrapped()) {
            return input;
        }
        return rewrite(
                input,
                MaterialCatalog.canonicalItemMappings(),
                MissingMaterialStackCodec::itemExists,
                MaterialCatalog::contains);
    }

    static CompoundTag rewrite(
            CompoundTag input,
            Map<String, String> canonicalMappings,
            Predicate<String> itemExists) {
        return rewrite(input, canonicalMappings, itemExists, ignored -> true);
    }

    static CompoundTag rewrite(
            CompoundTag input,
            Map<String, String> canonicalMappings,
            Predicate<String> itemExists,
            Predicate<String> materialExists) {
        CompoundTag normalized = canonicalizeItemId(input, canonicalMappings);
        String itemId = normalized.getString("id");
        if (itemId.equals(MissingMaterialStackRewriter.UNKNOWN_ITEM_ID)) {
            return normalized;
        }
        CompoundTag missingComponent = rewriteMissingComponentMaterial(
                normalized, materialExists);
        if (missingComponent != null) {
            return missingComponent;
        }
        if (!itemId.startsWith("cruciblecraft:")) {
            return normalized;
        }
        var plan = MissingMaterialStackRewriter.plan(
                itemId,
                canonicalMappings,
                itemExists);
        if (plan.kind() == MissingMaterialStackRewriter.Kind.UNCHANGED) {
            return normalized;
        }

        CompoundTag rewritten = normalized.copy();
        rewritten.putString("id", plan.targetItemId());
        if (plan.kind() == MissingMaterialStackRewriter.Kind.UNKNOWN) {
            CompoundTag components = rewritten.contains("components", Tag.TAG_COMPOUND)
                    ? rewritten.getCompound("components").copy()
                    : new CompoundTag();
            CompoundTag missing = new CompoundTag();
            missing.putString("original_item_id", itemId);
            missing.putString("material_id", plan.materialId());
            missing.putString("form", plan.form());
            components.put(COMPONENT_ID, missing);
            rewritten.put("components", components);
        }
        return rewritten;
    }

    private static CompoundTag canonicalizeItemId(
            CompoundTag input,
            Map<String, String> canonicalMappings) {
        String itemId = input.getString("id");
        String canonical = canonicalMappings.getOrDefault(itemId, itemId);
        if (canonical.equals(itemId)) {
            return input;
        }
        CompoundTag rewritten = input.copy();
        rewritten.putString("id", canonical);
        return rewritten;
    }

    private static CompoundTag rewriteMissingComponentMaterial(
            CompoundTag input,
            Predicate<String> materialExists) {
        CompoundTag components = input.contains("components", Tag.TAG_COMPOUND)
                ? input.getCompound("components")
                : new CompoundTag();
        CompoundTag rewritten = rewriteMissingComponentMaterial(
                input,
                components,
                TOOL_MATERIAL_COMPONENT_ID,
                TOOL_COMPONENT_FORM,
                materialExists);
        if (rewritten != null) {
            return rewritten;
        }
        return rewriteMissingComponentMaterial(
                input,
                components,
                MACHINE_MATERIAL_COMPONENT_ID,
                MACHINE_COMPONENT_FORM,
                materialExists);
    }

    private static CompoundTag rewriteMissingComponentMaterial(
            CompoundTag input,
            CompoundTag components,
            String componentId,
            String componentForm,
            Predicate<String> materialExists) {
        String materialId = components.getString(componentId);
        if (materialId.isBlank()
                || MaterialComponentPolicies.isValid(
                        input.getString("id"),
                        componentId,
                        materialId,
                        materialExists)) {
            return null;
        }
        CompoundTag rewritten = input.copy();
        rewritten.putString("id", MissingMaterialStackRewriter.UNKNOWN_ITEM_ID);
        CompoundTag outputComponents = components.copy();
        CompoundTag missing = new CompoundTag();
        missing.putString("original_item_id", input.getString("id"));
        missing.putString("material_id", materialId);
        missing.putString("form", componentForm);
        outputComponents.put(COMPONENT_ID, missing);
        rewritten.put("components", outputComponents);
        return rewritten;
    }

    public static boolean isComponentMaterialValid(
            String itemId,
            String componentId,
            String materialId) {
        return MaterialCatalog.isBootstrapped()
                && isComponentMaterialValid(
                        itemId,
                        componentId,
                        materialId,
                        MaterialCatalog::contains);
    }

    static boolean isComponentMaterialValid(
            String itemId,
            String componentId,
            String materialId,
            Predicate<String> materialExists) {
        return MaterialComponentPolicies.isValid(
                itemId, componentId, materialId, materialExists);
    }
}
