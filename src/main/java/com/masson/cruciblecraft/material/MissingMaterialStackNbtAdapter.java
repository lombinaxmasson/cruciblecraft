package com.masson.cruciblecraft.material;

import java.util.LinkedHashMap;
import java.util.Map;

import com.masson.cruciblecraft.api.material.MaterialForm;
import com.masson.cruciblecraft.api.material.MaterialLookup;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** Minecraft 1.21.1 NBT adapter around the pure migration planner. */
public final class MissingMaterialStackNbtAdapter {
    private static final String COMPONENT_ID = "cruciblecraft:missing_material";

    private MissingMaterialStackNbtAdapter() {}

    public static Tag rewrite(Tag input) {
        return input instanceof CompoundTag stackTag ? rewrite(stackTag) : input;
    }

    public static CompoundTag rewrite(CompoundTag input) {
        String itemId = input.getString("id");
        if (!itemId.startsWith("cruciblecraft:")
                || itemId.equals(MissingMaterialStackRewriter.UNKNOWN_ITEM_ID)
                || !MaterialCatalog.isBootstrapped()) {
            return input;
        }
        var plan = MissingMaterialStackRewriter.plan(itemId, canonicalMappings());
        if (plan.kind() == MissingMaterialStackRewriter.Kind.UNCHANGED) {
            return input;
        }

        CompoundTag rewritten = input.copy();
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

    private static Map<String, String> canonicalMappings() {
        Map<String, String> mappings = new LinkedHashMap<>();
        for (var material : MaterialCatalog.values()) {
            for (MaterialForm form : material.forms()) {
                MaterialLookup.itemId(material.id(), form).ifPresent(id ->
                        mappings.put(
                                material.id() + "/" + form.serializedName(),
                                id.toString()));
            }
        }
        return mappings;
    }
}
