package com.masson.cruciblecraft.material;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.api.material.MaterialForm;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.material.def.ThermalProperties;
import com.masson.cruciblecraft.fluid.CrucibleTransferCoordinator;
import com.masson.cruciblecraft.fluid.MoltenTransferMath;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaterialCatalogTest {
    @Test
    void loadsConfigJsonDecomposesBronzeAndIndexesAlloy(@TempDir Path configDirectory) throws Exception {
        Files.writeString(configDirectory.resolve("lead.json"), """
                {
                  "id": "lead",
                  "forms": ["dust"],
                  "thermal": { "melting_point": 327.5 }
                }
                """);
        Files.writeString(configDirectory.resolve("broken_alloy.json"), """
                {
                  "id": "broken_alloy",
                  "forms": ["ingot"],
                  "thermal": { "melting_point": 700 },
                  "composition": { "missing_component": 1 }
                }
                """);
        assertTrue(MaterialCatalog.addStartupMaterial(new MaterialDefinition(
                "zinc",
                "zinc",
                Optional.empty(),
                1,
                "#B8C4C2",
                "metallic",
                List.of(MaterialForm.DUST),
                Map.of(),
                new ThermalProperties(419.5),
                false,
                Map.of(),
                false)));
        assertTrue(MaterialCatalog.addStartupMaterial(new MaterialDefinition(
                "nested_bronze",
                "nested_bronze",
                Optional.empty(),
                1,
                "#AA7744",
                "metallic",
                List.of(MaterialForm.DUST),
                Map.of(),
                new ThermalProperties(950),
                false,
                Map.of("bronze", 1, "copper", 1),
                false)));
        MaterialCatalog.bootstrap(configDirectory);

        assertEquals(13, MaterialCatalog.values().size());
        assertEquals(
                List.of(
                        "copper", "tin", "bronze", "clay", "ceramic", "iron",
                        "carbon", "steel", "gold", "zinc", "lead", "nickel"),
                MaterialCatalog.values().stream()
                        .limit(12)
                        .map(MaterialDefinition::id)
                        .toList());
        assertEquals(
                java.util.List.of(MaterialForm.DUST),
                MaterialCatalog.require("lead").forms());
        assertFalse(MaterialCatalog.values().stream()
                .anyMatch(material -> material.id().equals("broken_alloy")));
        assertEquals("zinc", MaterialCatalog.require("zinc").id());
        assertEquals(8.96, MaterialCatalog.require("copper").thermal().density());
        assertEquals(2562.0, MaterialCatalog.require("copper").thermal().boilingPoint());
        assertEquals(1538.0, MaterialCatalog.require("iron").thermal().meltingPoint());
        assertEquals(3527.0, MaterialCatalog.require("carbon").thermal().meltingPoint());
        assertEquals(1773.0, MaterialCatalog.require("steel").thermal().meltingPoint());
        assertTrue(MaterialCatalog.require("steel").noDecompose());
        assertTrue(MaterialCatalog.require("steel").composition().isEmpty());
        assertEquals(
                Map.of("copper", 108, "tin", 36),
                MaterialCatalog.decompose(
                        MaterialCatalog.require("bronze"),
                        MaterialForm.INGOT.units()));
        assertTrue(MaterialCatalog.contains("bronze"));
        assertFalse(MaterialCatalog.contains("missing"));
        assertEquals(4, MaterialCatalog.decompositionQuantum("bronze"));
        assertEquals(1, MaterialCatalog.decompositionQuantum("steel"));
        assertEquals(Map.of("copper", 3, "tin", 1), MaterialCatalog.decompositionRatio("bronze"));
        assertEquals(8, MaterialCatalog.decompositionQuantum("nested_bronze"));
        assertEquals(
                Map.of("copper", 7, "tin", 1),
                MaterialCatalog.decompositionRatio("nested_bronze"));
        assertSame(
                MaterialCatalog.decompositionRatio("bronze"),
                MaterialCatalog.decompositionRatio(MaterialCatalog.require("bronze")));

        var bronze = MaterialCatalog.alloys().match(Map.of("copper", 432, "tin", 144));
        assertTrue(bronze.isPresent());
        assertEquals("bronze", bronze.orElseThrow().result().id());
        assertFalse(MaterialCatalog.alloys().match(Map.of("copper", 500, "tin", 144)).isPresent());
        assertEquals(
                "bronze",
                CrucibleTransferCoordinator.resolveCurrentMaterial(
                                Map.of("copper", 432, "tin", 144))
                        .orElseThrow()
                        .id());
        var bronzeDrain = MoltenTransferMath.planDrain(
                Map.of("copper", 432, "tin", 144),
                MaterialCatalog.decompositionRatio("bronze"),
                144);
        assertEquals(144, bronzeDrain.orElseThrow().amount());
        assertEquals(Map.of("copper", 108, "tin", 36), bronzeDrain.orElseThrow().removals());
        assertTrue(CrucibleTransferCoordinator.resolveCurrentMaterial(
                        Map.of("copper", 500, "tin", 144))
                .isEmpty());

        var simulated = CrucibleTransferCoordinator.planFill(
                MaterialCatalog.require("steel"),
                MaterialForm.INGOT.units(),
                0,
                MaterialForm.INGOT.units() * 8,
                "ceramic",
                false);
        assertTrue(simulated.isEmpty(), "ceramic must reject steel in both simulation and execution");
    }
}
