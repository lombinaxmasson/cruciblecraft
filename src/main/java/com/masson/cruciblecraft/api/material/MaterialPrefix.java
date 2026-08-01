package com.masson.cruciblecraft.api.material;

import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.mojang.serialization.Codec;

/**
 * Stable, open identifier for a material prefix.
 *
 * <p>All structural properties live in the startup-frozen prefix catalog. This
 * value is deliberately only an id, so addons can refer to their prefixes
 * without extending a closed Java type.
 */
public record MaterialPrefix(String id) implements Comparable<MaterialPrefix> {
    public static final Codec<MaterialPrefix> CODEC = Codec.STRING.comapFlatMap(
            MaterialPrefixCatalog::decode,
            MaterialPrefix::serializedId);

    public MaterialPrefix {
        if (id == null || !id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("Invalid material prefix id: " + id);
        }
    }

    public String serializedId() {
        return id;
    }

    public String serializedName() {
        return MaterialPrefixCatalog.definition(this).serializedPath();
    }

    public int units() {
        return MaterialPrefixCatalog.definition(this).units();
    }

    public String tagDirectory() {
        return MaterialPrefixCatalog.definition(this).tagDirectory();
    }

    public String tagNamespace() {
        return MaterialPrefixCatalog.definition(this).tagNamespace();
    }

    @Override
    public int compareTo(MaterialPrefix other) {
        return id.compareTo(other.id);
    }
}
