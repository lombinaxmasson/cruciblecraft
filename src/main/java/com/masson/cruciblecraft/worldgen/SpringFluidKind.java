package com.masson.cruciblecraft.worldgen;

import java.util.Optional;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6 {@code WorldgenFluidSpring} fill fluids for no-mod rows. Oils are
 * dedicated world fluids; gas and geothermal wrap the existing tank identities.
 */
public enum SpringFluidKind {
    OIL_EXTRA_HEAVY("oil_extra_heavy"),
    OIL_HEAVY("oil_heavy"),
    OIL_MEDIUM("oil_medium"),
    OIL_LIGHT("oil_light"),
    NATURAL_GAS("natural_gas"),
    WATER_GEOTHERMAL("water_geothermal"),
    LAVA("lava");

    private final String path;

    SpringFluidKind(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }

    public ResourceLocation fluidId() {
        if (this == LAVA) {
            return ResourceLocation.withDefaultNamespace("lava");
        }
        return ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, path);
    }

    public BlockState sourceState() {
        return switch (this) {
            case OIL_EXTRA_HEAVY -> ModBlocks.OIL_EXTRA_HEAVY.get().fullState();
            case OIL_HEAVY -> ModBlocks.OIL_HEAVY.get().fullState();
            case OIL_MEDIUM -> ModBlocks.OIL_MEDIUM.get().fullState();
            case OIL_LIGHT -> ModBlocks.OIL_LIGHT.get().fullState();
            case NATURAL_GAS -> ModBlocks.NATURAL_GAS.get().fullState();
            case WATER_GEOTHERMAL -> ModBlocks.WATER_GEOTHERMAL.get().fullState();
            case LAVA -> Blocks.LAVA.defaultBlockState();
        };
    }

    public static Optional<SpringFluidKind> byFluidId(ResourceLocation id) {
        for (SpringFluidKind kind : values()) {
            if (kind.fluidId().equals(id)) {
                return Optional.of(kind);
            }
        }
        return Optional.empty();
    }
}
