package com.masson.cruciblecraft.logistics.pipe.cover;

import java.util.Optional;

import net.minecraft.resources.ResourceLocation;

/** Legacy pipe-cover ids retained only as a definition migration adapter. */
public enum PipeCoverType {
    FILTER("filter", "filter"),
    ONE_WAY_VALVE("one_way_valve", "shutter"),
    OUTPUT_PUMP("output_pump", "pump");

    private final String id;
    private final ResourceLocation definitionId;

    PipeCoverType(String id, String definitionPath) {
        this.id = id;
        this.definitionId = ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft", definitionPath);
    }

    public String id() {
        return id;
    }

    public ResourceLocation definitionId() {
        return definitionId;
    }

    public static PipeCoverType decode(String id) {
        for (PipeCoverType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown pipe cover type " + id);
    }

    public static Optional<PipeCoverType> fromDefinition(
            ResourceLocation definitionId) {
        for (PipeCoverType type : values()) {
            if (type.definitionId.equals(definitionId)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
