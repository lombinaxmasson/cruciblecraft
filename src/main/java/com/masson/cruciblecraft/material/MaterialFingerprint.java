package com.masson.cruciblecraft.material;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HexFormat;

import com.masson.cruciblecraft.material.def.MaterialDefinition;

/** Deterministic digest of all persistence-relevant material definitions. */
public final class MaterialFingerprint {
    private MaterialFingerprint() {}

    public static String compute(Collection<MaterialDefinition> definitions) {
        StringBuilder canonical = new StringBuilder();
        definitions.stream()
                .sorted(java.util.Comparator.comparing(MaterialDefinition::id))
                .forEach(material -> {
                    canonical.append(material.id()).append('|')
                            .append(material.tagName()).append('|')
                            .append(material.nameKey().orElse("")).append('|')
                            .append(material.tier()).append('|')
                            .append(material.color()).append('|')
                            .append(material.tintStyle()).append('|');
                    material.forms().stream()
                            .sorted(java.util.Comparator.comparing(Enum::name))
                            .forEach(form -> canonical.append(form.serializedName()).append(','));
                    canonical.append('|');
                    material.formItems().entrySet().stream()
                            .sorted(java.util.Map.Entry.comparingByKey())
                            .forEach(entry -> canonical.append(entry.getKey().serializedName())
                                    .append('=').append(entry.getValue()).append(','));
                    canonical.append('|')
                            .append(material.thermal().meltingPoint()).append('|')
                            .append(material.thermal().boilingPoint()).append('|')
                            .append(material.thermal().density()).append('|')
                            .append(material.moltenFluid()).append('|');
                    material.composition().entrySet().stream()
                            .sorted(java.util.Map.Entry.comparingByKey())
                            .forEach(entry -> canonical.append(entry.getKey())
                                    .append('=').append(entry.getValue()).append(','));
                    canonical.append('|').append(material.noDecompose()).append('\n');
                });
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
