package com.masson.cruciblecraft.material.prefix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.material.MaterialRegistrationGate;
import com.masson.cruciblecraft.material.def.MaterialLoader;
import com.masson.cruciblecraft.recipe.rule.MaterialRuleExpansion;
import com.masson.cruciblecraft.recipe.rule.T2ChainReachability;
import com.masson.cruciblecraft.recipe.rule.T2ChainRules;

import net.minecraft.resources.ResourceLocation;

/** Production-rule integration harness; block-world GameTests remain a full-verification task. */
class T2ChainIntegrationTest {
    @BeforeEach void bootstrapPrefixes() {
        MaterialPrefixTestFixture.reset();
        MaterialPrefixTestFixture.bootstrapBuiltins();
    }

    @AfterEach void restorePrefixes() {
        MaterialPrefixTestFixture.reset();
        MaterialPrefixTestFixture.bootstrapBuiltins();
    }

    @Test
    void importedFormsExpandEveryMapAndReachIngot(@TempDir Path config) {
        var materials = MaterialLoader.load(config).values();
        var registeredForms = MaterialRegistrationGate.load(materials);
        var report = T2ChainReachability.analyze(materials);
        assertEquals(8, report.recipesPerMap().size());
        assertTrue(report.recipesPerMap().values().stream().allMatch(count -> count > 0),
                "every configured T2 map must have a reachable production rule");
        assertFalse(report.reachableMaterials().isEmpty());
        assertTrue(report.reachableMaterials().contains("copper"));
        assertTrue(report.reachableMaterials().contains("zircon"));
        assertTrue(report.byproductOutputs() > 0);
        assertEquals(report.reachableMaterials(), report.pathsByMaterial().keySet());
        assertTrue(java.util.Collections.disjoint(
                report.reachableMaterials(), report.unreachableOres().keySet()));
        long activeOres = materials.stream()
                .filter(material -> registeredForms.get(material.id())
                        .contains(MaterialPrefixes.RAW_ORE))
                .count();
        assertEquals(activeOres,
                report.reachableMaterials().size() + report.unreachableOres().size());
        assertTrue(report.pathsByMaterial().values().stream().allMatch(path ->
                !path.isEmpty()
                        && path.getFirst().getPath().startsWith("crusher/")
                        && path.getLast().getPath().startsWith("smelter/")));

        var byId = materials.stream().collect(java.util.stream.Collectors.toMap(
                material -> material.id(), material -> material));
        T2ChainRules.Definition smelter = T2ChainRules.ALL.stream()
                .filter(definition -> definition.path().startsWith("smelter/"))
                .findFirst().orElseThrow();
        var smeltingPlans = MaterialRuleExpansion.expandPlansWithRegisteredForms(
                ResourceLocation.fromNamespaceAndPath("cruciblecraft", smelter.path()),
                smelter.rule(), materials, registeredForms);
        assertTrue(smeltingPlans.stream().allMatch(plan -> {
            long targetUnits = byId.get(plan.materialId()).gt6Metadata().orElseThrow()
                    .processingTargets().get("smelting").ccUnits().orElseThrow();
            long inputUnits = (long) plan.itemInputs().getFirst().amount() * targetUnits;
            long outputUnits = (long) plan.itemOutputs().getFirst().amount()
                    * MaterialPrefixes.INGOT.units();
            return inputUnits == outputUnits
                    && (long) plan.duration() * plan.eut()
                    == (long) plan.itemInputs().getFirst().amount() * 6_400L;
        }), "every expanded smelting output must exactly conserve target units");
        var zirconBatch = smeltingPlans.stream()
                .filter(plan -> plan.materialId().equals("zircon"))
                .findFirst().orElseThrow();
        assertEquals(9, zirconBatch.itemInputs().getFirst().amount());
        assertEquals(1, zirconBatch.itemOutputs().getFirst().amount());
        assertEquals("zirconium",
                zirconBatch.itemOutputs().getFirst().resource().materialId());
        assertEquals(7_200, zirconBatch.duration());

        var anvil = MaterialRuleExpansion.expandPlansWithRegisteredForms(
                ResourceLocation.fromNamespaceAndPath(
                        "cruciblecraft", "anvil/raw_ore_to_crushed_ore"),
                T2ChainRules.ANVIL_RAW_TO_CRUSHED,
                materials,
                registeredForms);
        assertFalse(anvil.isEmpty());
        assertEquals(1, anvil.getFirst().itemOutputs().get(0).amount());
        assertEquals(MaterialPrefixes.CRUSHED_ORE.serializedId(),
                anvil.getFirst().itemOutputs().get(0).resource().prefix().orElseThrow());
        assertEquals(6, anvil.getFirst().itemOutputs().get(1).amount());
        assertEquals(MaterialPrefixes.TINY_CRUSHED_ORE.serializedId(),
                anvil.getFirst().itemOutputs().get(1).resource().prefix().orElseThrow());
        assertEquals(144 + 6 * 16,
                MaterialPrefixes.CRUSHED_ORE.units()
                        + 6 * MaterialPrefixes.TINY_CRUSHED_ORE.units());

        T2ChainRules.ALL.stream()
                .filter(definition -> definition.path().startsWith("sluice/"))
                .findFirst().orElseThrow().rule().fluidInputs().stream()
                .findFirst().ifPresent(fluid -> {
                    assertEquals("minecraft:water", fluid.fluid().orElseThrow().toString());
                    assertEquals("250", fluid.amount());
                });
    }
}
