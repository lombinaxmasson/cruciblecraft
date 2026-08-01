package com.masson.cruciblecraft.material;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.material.def.ThermalProperties;

import net.neoforged.fml.loading.FMLEnvironment;

/** System-property-only startup fixture for manual full-server stress runs. */
public final class MaterialStressFixture {
    private static final String SCENARIO_PROPERTY =
            "cruciblecraft.materialStressScenario";
    private static final String COUNT_PROPERTY =
            "cruciblecraft.materialStressCount";

    private MaterialStressFixture() {}

    public static void installFromSystemProperties() {
        installFromSystemProperties(FMLEnvironment.production);
    }

    static void installFromSystemProperties(boolean production) {
        String scenario = System.getProperty(SCENARIO_PROPERTY, "");
        if (!validateScenarioForEnvironment(scenario, production)) {
            return;
        }
        String configuredCount = System.getProperty(COUNT_PROPERTY);
        int count = 20_000;
        if (configuredCount != null) {
            try {
                count = Integer.parseInt(configuredCount);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(
                        "Invalid material stress count: " + configuredCount,
                        exception);
            }
        }
        if (count < 1 || count > 20_000) {
            throw new IllegalArgumentException(
                    "Material stress count must be in [1, 20000]");
        }
        for (int index = 0; index < count; index++) {
            MaterialDefinition definition = definition(scenario, index);
            if (!MaterialCatalog.addStartupMaterial(definition)) {
                throw new IllegalStateException(
                        "Duplicate stress material: " + definition.id());
            }
        }
        CrucibleCraft.LOGGER.warn(
                "Installed manual material stress fixture: scenario={} count={}",
                scenario,
                count);
    }

    static boolean validateScenarioForEnvironment(String scenario, boolean production) {
        if (scenario == null || scenario.isBlank()) {
            return false;
        }
        if (production) {
            throw new IllegalStateException(
                    "Material stress fixture is forbidden in production jars; "
                            + "remove the " + SCENARIO_PROPERTY + " system property");
        }
        if (!scenario.equals("metadata_only") && !scenario.equals("single_dust")) {
            throw new IllegalArgumentException(
                    "Unknown material stress scenario: " + scenario);
        }
        return true;
    }

    private static MaterialDefinition definition(String scenario, int index) {
        String id = "stress_" + String.format("%05d", index);
        if (scenario.equals("metadata_only")) {
            return MaterialDefinition.metadataOnly(
                    id,
                    id,
                    Optional.empty(),
                    0,
                    "#808080",
                    "matte",
                    new ThermalProperties(1.0),
                    false,
                    Map.of(),
                    true);
        }
        return new MaterialDefinition(
                id,
                id,
                Optional.empty(),
                0,
                "#808080",
                "matte",
                List.of(MaterialPrefixes.DUST),
                Map.of(),
                new ThermalProperties(1.0),
                false,
                Map.of(),
                true);
    }
}
