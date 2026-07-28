package com.masson.cruciblecraft.material;

import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.api.material.MaterialForm;

/**
 * Rewrites the raw 1.21.1 ItemStack NBT before its item id is decoded.
 *
 * <p>This class intentionally has no Minecraft runtime dependencies, allowing
 * migrations to be exhaustively unit-tested.
 */
public final class MissingMaterialStackRewriter {
    private static final String MOD_ID = "cruciblecraft";
    public static final String UNKNOWN_ITEM_ID = MOD_ID + ":unknown_material";

    private MissingMaterialStackRewriter() {}

    public static Plan plan(String itemId, Map<String, String> canonicalItems) {
        if (itemId == null || itemId.equals(UNKNOWN_ITEM_ID)
                || !itemId.startsWith(MOD_ID + ":")) {
            return Plan.unchanged(itemId);
        }
        Optional<Identity> identity = parseLegacyIdentity(
                itemId.substring(MOD_ID.length() + 1));
        if (identity.isEmpty()) {
            return Plan.unchanged(itemId);
        }
        Identity value = identity.get();
        String canonical = canonicalItems.get(value.key());
        if (canonical == null) {
            return new Plan(Kind.UNKNOWN, UNKNOWN_ITEM_ID, value.materialId(), value.form());
        }
        if (canonical.equals(itemId)) {
            return Plan.unchanged(itemId);
        }
        return new Plan(Kind.CANONICAL, canonical, value.materialId(), value.form());
    }

    static Optional<Identity> parseLegacyIdentity(String path) {
        for (MaterialForm form : java.util.Arrays.stream(MaterialForm.values())
                .sorted(java.util.Comparator.comparingInt(
                        (MaterialForm value) -> value.serializedName().length()).reversed())
                .toList()) {
            String suffix = "_" + form.serializedName();
            if (path.endsWith(suffix) && path.length() > suffix.length()) {
                return Optional.of(new Identity(
                        path.substring(0, path.length() - suffix.length()),
                        form.serializedName()));
            }
        }
        return Optional.empty();
    }

    private static String key(String materialId, String form) {
        return materialId + "/" + form;
    }

    public enum Kind {
        UNCHANGED,
        CANONICAL,
        UNKNOWN
    }

    public record Plan(Kind kind, String targetItemId, String materialId, String form) {
        private static Plan unchanged(String itemId) {
            return new Plan(Kind.UNCHANGED, itemId, "", "");
        }
    }

    record Identity(String materialId, String form) {
        private String key() {
            return MissingMaterialStackRewriter.key(materialId, form);
        }
    }
}
