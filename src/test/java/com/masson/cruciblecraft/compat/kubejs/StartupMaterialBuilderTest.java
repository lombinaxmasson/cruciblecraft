package com.masson.cruciblecraft.compat.kubejs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StartupMaterialBuilderTest {
    @Test
    void buildsNonStructuralMaterialTuning() {
        var tuning = new StartupMaterialBuilder("copper")
                .tier(3)
                .color("#D5A93E")
                .meltingPoint(930)
                .boilingPoint(2_500)
                .density(8.5)
                .build();

        assertEquals(3, tuning.tier().orElseThrow());
        assertEquals("#D5A93E", tuning.color().orElseThrow());
        assertEquals(930, tuning.meltingPoint().orElseThrow());
        assertEquals(2_500, tuning.boilingPoint().orElseThrow());
        assertEquals(8.5, tuning.density().orElseThrow());
    }

    @Test
    void ignoresRegistryShapingFromKubeJs() {
        StartupMaterialBuilder builder = new StartupMaterialBuilder("copper");
        builder.forms("ingot")
                .formItem("ingot", "minecraft:copper_ingot")
                .moltenFluid(true)
                .component("tin", 1);
        var tuning = builder.build();
        assertTrue(tuning.tier().isEmpty());
        assertTrue(tuning.color().isEmpty());
        assertTrue(tuning.meltingPoint().isEmpty());
    }
}
