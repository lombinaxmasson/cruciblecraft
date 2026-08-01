package com.masson.cruciblecraft.material;

import java.util.Map;
import java.util.function.Predicate;

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
        return rewrite(
                input,
                MaterialCatalog.canonicalItemMappings(),
                MissingMaterialStackCodec::itemExists);
    }

    static CompoundTag rewrite(
            CompoundTag input,
            Map<String, String> canonicalMappings,
            Predicate<String> itemExists) {
        String itemId = input.getString("id");
        if (!itemId.startsWith("cruciblecraft:")
                || itemId.equals(MissingMaterialStackRewriter.UNKNOWN_ITEM_ID)) {
            return input;
        }
        var plan = MissingMaterialStackRewriter.plan(
                itemId,
                canonicalMappings,
                itemExists);
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
}
