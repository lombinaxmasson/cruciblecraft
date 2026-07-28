package com.masson.cruciblecraft.api.fluid;

import java.util.Optional;

import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.registry.ModFluids;

import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;

public final class MoltenFluidLookup {
    private MoltenFluidLookup() {}

    public static Optional<FlowingFluid> fluid(String materialId) {
        return ModFluids.molten(materialId).map(entry -> entry.source().get());
    }

    public static Optional<FlowingFluid> fluid(MaterialDefinition material) {
        return fluid(material.id());
    }

    public static Optional<MaterialDefinition> material(Fluid fluid) {
        return ModFluids.material(fluid);
    }
}
