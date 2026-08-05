package com.masson.cruciblecraft.material.prefix;

import java.util.List;
import java.util.Set;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Immutable structural definition loaded before any material or item exists. */
public record MaterialPrefixDefinition(
        MaterialPrefix prefix,
        String serializedPath,
        int units,
        String generationFlag,
        String tagDirectory,
        String tagNamespace,
        String modelTemplate,
        String modelTexture,
        List<String> aliases,
        List<String> impliedPrefixes,
        double heatDamage,
        Set<String> capabilities) {

    public static final Codec<MaterialPrefixDefinition> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.fieldOf("id")
                            .xmap(MaterialPrefix::new, MaterialPrefix::id)
                            .forGetter(MaterialPrefixDefinition::prefix),
                    Codec.STRING.fieldOf("serialized_path")
                            .forGetter(MaterialPrefixDefinition::serializedPath),
                    Codec.INT.fieldOf("units").forGetter(MaterialPrefixDefinition::units),
                    Codec.STRING.fieldOf("generation_flag")
                            .forGetter(MaterialPrefixDefinition::generationFlag),
                    Codec.STRING.fieldOf("tag_directory")
                            .forGetter(MaterialPrefixDefinition::tagDirectory),
                    Codec.STRING.optionalFieldOf("tag_namespace", "cruciblecraft")
                            .forGetter(MaterialPrefixDefinition::tagNamespace),
                    Codec.STRING.optionalFieldOf("model_template", "minecraft:item/generated")
                            .forGetter(MaterialPrefixDefinition::modelTemplate),
                    Codec.STRING.fieldOf("model_texture")
                            .forGetter(MaterialPrefixDefinition::modelTexture),
                    Codec.STRING.listOf().optionalFieldOf("aliases", List.of())
                            .forGetter(MaterialPrefixDefinition::aliases),
                    Codec.STRING.listOf().optionalFieldOf("implied_prefixes", List.of())
                            .forGetter(MaterialPrefixDefinition::impliedPrefixes),
                    Codec.DOUBLE.optionalFieldOf("heat_damage", 0.0)
                            .forGetter(MaterialPrefixDefinition::heatDamage),
                    Codec.STRING.listOf().xmap(Set::copyOf, List::copyOf)
                            .optionalFieldOf("capabilities", Set.of())
                            .forGetter(MaterialPrefixDefinition::capabilities)
            ).apply(instance, MaterialPrefixDefinition::new));

    /**
     * Source-compatible startup integration constructor for prefix definitions
     * authored before implied-prefix closure was introduced.
     */
    public MaterialPrefixDefinition(
            MaterialPrefix prefix,
            String serializedPath,
            int units,
            String generationFlag,
            String tagDirectory,
            String tagNamespace,
            String modelTemplate,
            String modelTexture,
            List<String> aliases,
            Set<String> capabilities) {
        this(
                prefix,
                serializedPath,
                units,
                generationFlag,
                tagDirectory,
                tagNamespace,
                modelTemplate,
                modelTexture,
                aliases,
                List.of(),
                0.0,
                capabilities);
    }

    /** Source-compatible constructor for definitions without prefix facts. */
    public MaterialPrefixDefinition(
            MaterialPrefix prefix,
            String serializedPath,
            int units,
            String generationFlag,
            String tagDirectory,
            String tagNamespace,
            String modelTemplate,
            String modelTexture,
            List<String> aliases,
            List<String> impliedPrefixes,
            Set<String> capabilities) {
        this(
                prefix,
                serializedPath,
                units,
                generationFlag,
                tagDirectory,
                tagNamespace,
                modelTemplate,
                modelTexture,
                aliases,
                impliedPrefixes,
                0.0,
                capabilities);
    }

    public MaterialPrefixDefinition {
        if (prefix == null || generationFlag == null) {
            throw new IllegalArgumentException("Prefix id and generation flag are required");
        }
        if (!serializedPath.matches("[a-z0-9][a-z0-9_./-]*")) {
            throw new IllegalArgumentException("Invalid serialized prefix path: " + serializedPath);
        }
        if (units <= 0) {
            throw new IllegalArgumentException("Prefix units must be positive: " + prefix.id());
        }
        if (!Double.isFinite(heatDamage)) {
            throw new IllegalArgumentException(
                    "Prefix heat damage must be finite: " + prefix.id());
        }
        if (!tagDirectory.matches("[a-z0-9][a-z0-9_./-]*")) {
            throw new IllegalArgumentException("Invalid prefix tag directory: " + tagDirectory);
        }
        if (tagNamespace == null || !tagNamespace.matches("[a-z0-9_.-]+")) {
            throw new IllegalArgumentException("Invalid prefix tag namespace: " + tagNamespace);
        }
        if (!modelTemplate.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")
                || !modelTexture.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("Invalid model hint for prefix " + prefix.id());
        }
        aliases = List.copyOf(aliases);
        impliedPrefixes = List.copyOf(impliedPrefixes);
        capabilities = Set.copyOf(capabilities);
        if (!generationFlag.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")
                || capabilities.stream().anyMatch(
                        value -> !value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))) {
            throw new IllegalArgumentException("Generation flags and capabilities must be namespaced");
        }
        if (aliases.stream().anyMatch(alias -> !alias.matches("[a-z0-9_.:/-]+"))) {
            throw new IllegalArgumentException("Invalid alias for prefix " + prefix.id());
        }
        if (aliases.stream().distinct().count() != aliases.size()) {
            throw new IllegalArgumentException("Duplicate alias for prefix " + prefix.id());
        }
        if (impliedPrefixes.stream().anyMatch(
                implied -> !implied.matches("[a-z0-9_.:/-]+"))) {
            throw new IllegalArgumentException(
                    "Invalid implied prefix for " + prefix.id());
        }
        if (impliedPrefixes.stream().distinct().count() != impliedPrefixes.size()) {
            throw new IllegalArgumentException(
                    "Duplicate implied prefix for " + prefix.id());
        }
    }

    public boolean hasCapability(String capability) {
        return capabilities.contains(capability);
    }
}
