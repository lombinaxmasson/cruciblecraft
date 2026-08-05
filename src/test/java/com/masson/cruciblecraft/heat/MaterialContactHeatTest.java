package com.masson.cruciblecraft.heat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.item.MaterialFormItem;
import com.masson.cruciblecraft.material.def.GT6MaterialMetadata;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.material.def.ThermalProperties;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.loading.LoadingModList;

class MaterialContactHeatTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void combinesPrefixAndMaterialSourceFactsWithoutThermalInference() {
        MaterialDefinition ordinary = material();
        MaterialDefinition hotMaterial = ordinary.withImportedMetadata(metadata(2.0));

        assertEquals(
                0.0,
                MaterialContactHeat.damage(
                        MaterialPrefixCatalog.definition(MaterialPrefixes.INGOT),
                        ordinary));
        assertEquals(
                3.0,
                MaterialContactHeat.damage(
                        MaterialPrefixCatalog.definition(MaterialPrefixes.INGOT_HOT),
                        ordinary));
        assertEquals(
                5.0,
                MaterialContactHeat.damage(
                        MaterialPrefixCatalog.definition(MaterialPrefixes.INGOT_HOT),
                        hotMaterial));
    }

    @Test
    void nonMaterialAndUnknownMaterialStacksAreInert() {
        assertEquals(0.0, MaterialContactHeat.damage(new ItemStack(Items.STONE)));
        assertEquals(
                0.0,
                MaterialContactHeat.damage(new UnknownMaterialForm()));
    }

    private static MaterialDefinition material() {
        return new MaterialDefinition(
                "testium",
                "testium",
                Optional.empty(),
                0,
                "#808080",
                "metallic",
                List.of(MaterialPrefixes.INGOT),
                Map.of(),
                new ThermalProperties(1_000, 2_000, 7),
                false,
                Map.of(),
                false);
    }

    private static GT6MaterialMetadata metadata(double heatDamage) {
        return new GT6MaterialMetadata(
                1,
                "Testium",
                List.of(),
                "solid",
                Optional.empty(),
                new GT6MaterialMetadata.SourceThermal(
                        1_273.15, 1_000, 2_273.15, 2_000, 3_273.15, 3_000, 7),
                GT6MaterialMetadata.ToolStats.EMPTY,
                List.of(),
                Map.of(),
                List.of(),
                List.of(),
                0.0,
                heatDamage,
                Optional.empty(),
                Map.of(),
                GT6MaterialMetadata.PipeProperties.EMPTY);
    }

    private static final class UnknownMaterialForm implements MaterialFormItem {
        @Override
        public String materialId() {
            return "missing";
        }

        @Override
        public MaterialPrefix form() {
            return new MaterialPrefix("example:missing");
        }
    }
}
