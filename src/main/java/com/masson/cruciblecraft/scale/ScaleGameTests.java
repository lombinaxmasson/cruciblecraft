package com.masson.cruciblecraft.scale;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Dedicated scale GameTests. The holder namespace is not the mod id, so
 * ordinary {@code neoforge.enabledGameTestNamespaces=cruciblecraft} keeps the
 * daily grid log. Measurement runs set that property to
 * {@link #NAMESPACE} and register this single test.
 */
@GameTestHolder(ScaleGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class ScaleGameTests {
    public static final String NAMESPACE = "cruciblecraft_scale";
    private static final String TEMPLATE = "empty";

    private ScaleGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 70_000)
    public static void scaleMeasurement(GameTestHelper helper) {
        String scenario = System.getProperty("cruciblecraft.scale", "");
        if (scenario.isBlank()) {
            helper.succeed();
            return;
        }
        ScaleHarness harness = new ScaleHarness(helper, scenario);
        harness.build();
        var sequence = helper.startSequence()
                .thenIdle(harness.scenario().warmupTicks());
        for (int sample = 0; sample < harness.sampleCount(); sample++) {
            final int index = sample;
            sequence = sequence
                    .thenExecute(() -> harness.beginSample(index))
                    .thenIdle(harness.scenario().samplingTicks())
                    .thenExecute(() -> harness.endSample(index));
        }
        sequence.thenExecute(() -> {
            try {
                harness.finish();
            } catch (Exception failure) {
                throw new RuntimeException(failure);
            }
        }).thenSucceed();
    }
}
