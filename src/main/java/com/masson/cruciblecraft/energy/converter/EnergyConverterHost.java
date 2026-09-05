package com.masson.cruciblecraft.energy.converter;

import net.minecraft.resources.ResourceLocation;

/** Block that hosts one catalog converter variant. */
public interface EnergyConverterHost {
    ResourceLocation converterId();

    default EnergyConverterProfile converterProfile() {
        return EnergyConverterCatalog.require(converterId());
    }
}
