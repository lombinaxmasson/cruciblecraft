package com.masson.cruciblecraft.compat.kubejs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import com.masson.cruciblecraft.api.material.MaterialForm;

import org.junit.jupiter.api.Test;

class StartupMaterialBuilderTest {
    @Test
    void buildsAdditiveMaterialDefinition() {
        var definition = new StartupMaterialBuilder("brass")
                .color("#D5A93E")
                .forms("ingot", "plate")
                .formItem("ingot", "minecraft:gold_ingot")
                .meltingPoint(930)
                .boilingPoint(2_500)
                .density(8.5)
                .component("copper", 3)
                .component("zinc", 1)
                .build();

        assertEquals(List.of(MaterialForm.INGOT, MaterialForm.PLATE), definition.forms());
        assertEquals(Map.of("copper", 3, "zinc", 1), definition.composition());
        assertEquals(
                "minecraft:gold_ingot",
                definition.formItems().get(MaterialForm.INGOT));
        assertEquals(930, definition.thermal().meltingPoint());
        assertEquals(2_500, definition.thermal().boilingPoint());
        assertEquals(8.5, definition.thermal().density());
    }
}
