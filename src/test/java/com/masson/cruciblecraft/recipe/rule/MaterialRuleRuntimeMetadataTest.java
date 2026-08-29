package com.masson.cruciblecraft.recipe.rule;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.material.def.ThermalProperties;
import com.masson.cruciblecraft.test.MinecraftTestBootstrap;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaterialRuleRuntimeMetadataTest {
    @BeforeAll
    static void ensureCatalog(@TempDir Path configDirectory) {
        MinecraftTestBootstrap.bootstrap();
        if (!MaterialCatalog.isBootstrapped()) {
            MaterialCatalog.bootstrap(configDirectory);
        }
    }

    @Test
    void deterministicPriorityFallbackAndTuningSelection() {
        MaterialDefinition iron = new MaterialDefinition(
                "iron", "iron", Optional.empty(), 2, "#AAAAAA", "metallic",
                List.of(MaterialPrefixes.INGOT), Map.of(),
                new ThermalProperties(1538, 2861, 7.8), true, Map.of(), false);
        MaterialRule low = metadata(
                Optional.of(tuning(3)),
                List.of(preference("test:low", 1), preference("test:invalid", 100)));
        MaterialRule high = metadata(
                Optional.empty(),
                List.of(preference("test:high", 10)));
        MaterialRule tiedLater = metadata(
                Optional.empty(),
                List.of(preference("test:tie_later", 10)));

        MaterialRuleRuntimeMetadata.Publication publication =
                MaterialRuleRuntimeMetadata.build(
                        List.of(
                                source("z_later", tiedLater),
                                source("b_high", high),
                                source("a_low", low)),
                        List.of(iron),
                        (material, prefix, item) -> !item.getPath().equals("invalid"));

        assertEquals(1, publication.tunings().size());
        assertEquals(3, publication.tunings().getFirst().tier().orElseThrow());
        assertEquals(
                "test:high",
                publication.preferences().get(
                        "iron/" + MaterialPrefixes.INGOT.serializedId()),
                "highest priority wins and source id breaks ties deterministically");
        assertEquals(1, publication.diagnostics().size());
        assertTrue(publication.diagnostics().getFirst().contains("invalid"));
    }

    private static MaterialRule metadata(
            Optional<MaterialRule.Tuning> tuning,
            List<MaterialRule.UnificationPreference> preferences) {
        return new MaterialRule(
                Optional.empty(), List.of(), List.of(), List.of(), List.of(),
                "1", "0", "0", true, Optional.empty(), Map.of(), List.of(),
                tuning, preferences);
    }

    private static MaterialRule.Tuning tuning(int tier) {
        return new MaterialRule.Tuning(
                "iron", Optional.of(tier), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty());
    }

    private static MaterialRule.UnificationPreference preference(
            String item,
            int priority) {
        return new MaterialRule.UnificationPreference(
                "iron", "ingot", ResourceLocation.parse(item), priority);
    }

    private static MaterialRuleRuntimeMetadata.Source source(
            String path,
            MaterialRule rule) {
        return new MaterialRuleRuntimeMetadata.Source(
                ResourceLocation.fromNamespaceAndPath("test", path), rule);
    }
}
