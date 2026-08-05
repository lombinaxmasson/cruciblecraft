package com.masson.cruciblecraft.client.tooltip;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.material.def.GT6MaterialMetadata;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.material.def.ThermalProperties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaterialMetadataTooltipTest {
    @Test
    void exposesImportedFormulaAndTrimsWhitespace() {
        MaterialDefinition material = material().withImportedMetadata(metadata("  Fe  "));

        assertEquals(
                Optional.of("Fe"),
                MaterialMetadataTooltip.formula(material));
    }

    @Test
    void omitsMissingOrBlankFormula() {
        assertTrue(MaterialMetadataTooltip.formula(material()).isEmpty());
        assertTrue(MaterialMetadataTooltip.formula(
                material().withImportedMetadata(metadata("   "))).isEmpty());
    }

    private static MaterialDefinition material() {
        return new MaterialDefinition(
                "iron",
                "iron",
                Optional.empty(),
                0,
                "#808080",
                "metallic",
                List.of(MaterialPrefixes.INGOT),
                Map.of(),
                new ThermalProperties(1538, 2861, 7.874),
                false,
                Map.of(),
                false);
    }

    private static GT6MaterialMetadata metadata(String formula) {
        return new GT6MaterialMetadata(
                260,
                "Iron",
                List.of(),
                "solid",
                Optional.of(formula),
                new GT6MaterialMetadata.SourceThermal(
                        1811, 1537.85, 3134, 2860.85,
                        313400, 313126.85, 7.874),
                GT6MaterialMetadata.ToolStats.EMPTY,
                List.of(),
                Map.of(),
                List.of(),
                List.of(),
                0,
                0,
                Optional.empty(),
                Map.of(),
                GT6MaterialMetadata.PipeProperties.EMPTY);
    }
}
