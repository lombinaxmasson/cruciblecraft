package com.masson.cruciblecraft.material;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;

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

    public static Plan plan(
            String itemId,
            Map<String, String> canonicalItems,
            Predicate<String> itemExists) {
        if (itemId == null || itemId.equals(UNKNOWN_ITEM_ID)
                || !itemId.startsWith(MOD_ID + ":")) {
            return Plan.unchanged(itemId);
        }
        if (itemExists.test(itemId)) {
            return Plan.unchanged(itemId);
        }
        Optional<Identity> identity = parseIdentity(
                itemId.substring(MOD_ID.length() + 1));
        if (identity.isEmpty()) {
            return Plan.unchanged(itemId);
        }
        Identity value = identity.get();
        String canonical = canonicalItems.get(value.key());
        if (canonical == null) {
            return new Plan(
                    Kind.UNKNOWN, UNKNOWN_ITEM_ID, value.materialId(), value.form(), false);
        }
        if (canonical.equals(itemId)) {
            return Plan.unchanged(itemId);
        }
        boolean writePrefixMaterial = prefixItemPath(canonical).equals(value.form());
        return new Plan(
                Kind.CANONICAL, canonical, value.materialId(), value.form(), writePrefixMaterial);
    }

    static Optional<Identity> parseIdentity(String path) {
        Optional<Identity> legacy = parseLegacyIdentity(path);
        if (legacy.isPresent()) {
            return legacy;
        }
        int slash = path.indexOf('/');
        if (slash > 0 && slash < path.length() - 1) {
            return Optional.of(new Identity(
                    path.substring(0, slash),
                    path.substring(slash + 1)));
        }
        return Optional.empty();
    }

    static Optional<Identity> parseLegacyIdentity(String path) {
        for (var entry : MaterialPrefixCatalog.legacySuffixesLongestFirst()) {
            String suffix = "_" + entry.value();
            if (path.endsWith(suffix) && path.length() > suffix.length()) {
                return Optional.of(new Identity(
                        path.substring(0, path.length() - suffix.length()),
                        entry.canonicalPath()));
            }
        }
        return Optional.empty();
    }

    private static String prefixItemPath(String itemId) {
        int colon = itemId.indexOf(':');
        return colon < 0 ? itemId : itemId.substring(colon + 1);
    }

    private static String key(String materialId, String form) {
        return materialId + "/" + form;
    }

    public enum Kind {
        UNCHANGED,
        CANONICAL,
        UNKNOWN
    }

    public record Plan(
            Kind kind,
            String targetItemId,
            String materialId,
            String form,
            boolean writePrefixMaterial) {
        private static Plan unchanged(String itemId) {
            return new Plan(Kind.UNCHANGED, itemId, "", "", false);
        }

        public Plan(Kind kind, String targetItemId, String materialId, String form) {
            this(kind, targetItemId, materialId, form, false);
        }
    }

    record Identity(String materialId, String form) {
        private String key() {
            return MissingMaterialStackRewriter.key(materialId, form);
        }
    }
}
