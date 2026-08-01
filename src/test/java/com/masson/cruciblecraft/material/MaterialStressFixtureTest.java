package com.masson.cruciblecraft.material;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MaterialStressFixtureTest {
    private static final String SCENARIO =
            "cruciblecraft.materialStressScenario";
    private static final String OVERRIDE =
            "cruciblecraft.materialStressAllowProduction";
    private static final String COUNT =
            "cruciblecraft.materialStressCount";

    private String originalScenario;
    private String originalOverride;
    private String originalCount;

    @BeforeEach
    void rememberProperties() {
        originalScenario = System.getProperty(SCENARIO);
        originalOverride = System.getProperty(OVERRIDE);
        originalCount = System.getProperty(COUNT);
    }

    @AfterEach
    void restoreProperties() {
        restore(SCENARIO, originalScenario);
        restore(OVERRIDE, originalOverride);
        restore(COUNT, originalCount);
    }

    @Test
    void productionRejectsAnyConfiguredScenarioEvenWithOverrideProperty() {
        System.setProperty(SCENARIO, "metadata_only");
        System.setProperty(OVERRIDE, "true");

        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> MaterialStressFixture.installFromSystemProperties(true));

        assertTrue(failure.getMessage().contains("production jars"));
        assertTrue(failure.getMessage().contains(SCENARIO));
    }

    @Test
    void blankScenarioIsANoOpInProduction() {
        System.setProperty(SCENARIO, "   ");

        assertDoesNotThrow(
                () -> MaterialStressFixture.installFromSystemProperties(true));
        assertFalse(MaterialStressFixture.validateScenarioForEnvironment(" ", true));
    }

    @Test
    void developmentAllowsSupportedScenarios() {
        assertTrue(MaterialStressFixture.validateScenarioForEnvironment(
                "metadata_only", false));
        assertTrue(MaterialStressFixture.validateScenarioForEnvironment(
                "single_dust", false));
    }

    @Test
    void rejectsNonIntegerConfiguredCountWithItsValue() {
        System.setProperty(SCENARIO, "metadata_only");
        System.setProperty(COUNT, "not-a-number");

        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> MaterialStressFixture.installFromSystemProperties(false));

        assertTrue(failure.getMessage().contains("not-a-number"));
    }

    private static void restore(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
