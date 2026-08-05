package com.masson.cruciblecraft.material;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/** Shared syntax contract for persisted CrucibleCraft material identities. */
public final class MaterialId {
    public static final Codec<String> CODEC = Codec.STRING.validate(MaterialId::validate);

    private MaterialId() {}

    public static String requireValid(String id) {
        if (id == null || !id.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException("Invalid material id: " + id);
        }
        return id;
    }

    private static DataResult<String> validate(String id) {
        try {
            return DataResult.success(requireValid(id));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(exception::getMessage);
        }
    }
}
