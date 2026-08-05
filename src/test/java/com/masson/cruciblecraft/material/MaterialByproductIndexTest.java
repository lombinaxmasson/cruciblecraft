package com.masson.cruciblecraft.material;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.material.def.GT6MaterialMetadata;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.material.def.ThermalProperties;

class MaterialByproductIndexTest {
    @Test
    void publishesOrderedImmutableResolvedSnapshot() {
        MaterialDefinition source = new MaterialDefinition(
                "source", "source", Optional.empty(), 0, "#808080", "metallic",
                List.of(MaterialPrefixes.DUST), Map.of(),
                new ThermalProperties(1000, 2000, 7), false, Map.of(), false)
                .withImportedMetadata(new GT6MaterialMetadata(
                        1, "Source", List.of(), "solid", Optional.empty(),
                        new GT6MaterialMetadata.SourceThermal(
                                1000, 726.85, 2000, 1726.85, 3000, 2726.85, 7),
                        GT6MaterialMetadata.ToolStats.EMPTY,
                        List.of(
                                new GT6MaterialMetadata.MaterialReference("copper", 2, "Copper"),
                                new GT6MaterialMetadata.MaterialReference("gold", 3, "Gold")),
                        Map.of(), List.of(), List.of(), 0, 0, Optional.empty(),
                        Map.of(), GT6MaterialMetadata.PipeProperties.EMPTY));
        MaterialByproductIndex.publish(List.of(source), 17);
        assertEquals(17, MaterialByproductIndex.snapshot().revision());
        assertEquals(List.of("copper", "gold"), MaterialByproductIndex.byproducts("source"));
        assertEquals(List.of(), MaterialByproductIndex.byproducts("missing"));
    }
}
