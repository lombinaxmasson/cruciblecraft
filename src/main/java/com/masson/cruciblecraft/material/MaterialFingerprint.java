package com.masson.cruciblecraft.material;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.HexFormat;
import java.util.Map;

import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixDefinition;

/** Deterministic structural and tuning digests for material definitions. */
public final class MaterialFingerprint {
    private MaterialFingerprint() {}

    /** Registry/save compatibility fields. Changes here warrant a player warning. */
    public static String structure(Collection<MaterialDefinition> definitions) {
        return structure(definitions, MaterialPrefixCatalog.definitions());
    }

    /** Explicit-table variant useful for validating prospective startup structure. */
    public static String structure(
            Collection<MaterialDefinition> definitions,
            Collection<MaterialPrefixDefinition> prefixes) {
        StringBuilder canonical = new StringBuilder();
        appendPrefixTable(canonical, prefixes);
        definitions.stream()
                .sorted(java.util.Comparator.comparing(MaterialDefinition::id))
                .forEach(material ->
                        canonical.append(materialCanonical(material)).append('\n'));
        return digest(canonical);
    }

    /** Per-material structural digests used to explain multiplayer mismatches. */
    public static Map<String, String> structureByMaterial(
            Collection<MaterialDefinition> definitions) {
        LinkedHashMap<String, String> fingerprints = new LinkedHashMap<>();
        definitions.stream()
                .sorted(java.util.Comparator.comparing(MaterialDefinition::id))
                .forEach(material -> fingerprints.put(
                        material.id(),
                        digest(materialCanonical(material))));
        return Map.copyOf(fingerprints);
    }

    /** Detailed handshake entries for both prefix and material structure. */
    public static Map<String, String> structureEntries(
            Collection<MaterialDefinition> definitions) {
        return structureEntries(definitions, MaterialPrefixCatalog.definitions());
    }

    /** Explicit-table variant for preflight checks and deterministic tests. */
    public static Map<String, String> structureEntries(
            Collection<MaterialDefinition> definitions,
            Collection<MaterialPrefixDefinition> prefixes) {
        LinkedHashMap<String, String> fingerprints = new LinkedHashMap<>();
        prefixes.stream()
                .sorted(java.util.Comparator.comparing(
                        definition -> definition.prefix().id()))
                .forEach(prefix -> fingerprints.put(
                        "@prefix/" + prefix.prefix().serializedId(),
                        digest(prefixCanonical(prefix))));
        fingerprints.putAll(structureByMaterial(definitions));
        return Map.copyOf(fingerprints);
    }

    /** Expected balance/display tuning. Changes are logged but do not warn players. */
    public static String tuning(Collection<MaterialDefinition> definitions) {
        StringBuilder canonical = new StringBuilder();
        definitions.stream()
                .sorted(java.util.Comparator.comparing(MaterialDefinition::id))
                .forEach(material -> canonical.append(material.id()).append('|')
                        .append(material.nameKey().orElse("")).append('|')
                        .append(material.tier()).append('|')
                        .append(material.color()).append('|')
                        .append(material.tintStyle()).append('|')
                        .append(material.thermal().meltingPoint()).append('|')
                        .append(material.thermal().boilingPoint()).append('|')
                        .append(material.thermal().density()).append('\n'));
        return digest(canonical);
    }

    /** Compatibility alias for callers that only need save/registry structure. */
    public static String compute(Collection<MaterialDefinition> definitions) {
        return structure(definitions);
    }

    private static String digest(CharSequence canonical) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private static void appendPrefixTable(
            StringBuilder canonical,
            Collection<MaterialPrefixDefinition> prefixes) {
        prefixes.stream()
                .sorted(java.util.Comparator.comparing(
                        definition -> definition.prefix().id()))
                .forEach(prefix -> canonical.append(prefixCanonical(prefix)).append('\n'));
    }

    private static String prefixCanonical(MaterialPrefixDefinition prefix) {
        StringBuilder canonical = new StringBuilder("@prefix|")
                .append(prefix.prefix().serializedId()).append('|')
                .append(prefix.serializedPath()).append('|')
                .append(prefix.units()).append('|')
                .append(prefix.generationFlag()).append('|')
                .append(prefix.tagDirectory()).append('|')
                .append(prefix.modelTemplate()).append('|')
                .append(prefix.modelTexture()).append('|');
        prefix.aliases().stream().sorted()
                .forEach(alias -> canonical.append(alias).append(','));
        canonical.append('|');
        prefix.impliedPrefixes().stream().sorted()
                .forEach(implied -> canonical.append(implied).append(','));
        canonical.append('|');
        prefix.capabilities().stream().sorted()
                .forEach(capability -> canonical.append(capability).append(','));
        return canonical.toString();
    }

    private static String materialCanonical(MaterialDefinition material) {
        StringBuilder canonical = new StringBuilder(material.id()).append('|')
                .append(material.tagName()).append('|');
        material.forms().stream()
                .sorted()
                .forEach(form -> canonical.append(form.serializedId()).append(','));
        canonical.append('|');
        material.formItems().entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByKey())
                .forEach(entry -> canonical.append(entry.getKey().serializedId())
                        .append('=').append(entry.getValue()).append(','));
        canonical.append('|').append(material.metadataOnly()).append('|')
                .append(material.moltenFluid()).append('|');
        material.composition().entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByKey())
                .forEach(entry -> canonical.append(entry.getKey())
                        .append('=').append(entry.getValue()).append(','));
        return canonical.append('|').append(material.noDecompose()).toString();
    }
}
