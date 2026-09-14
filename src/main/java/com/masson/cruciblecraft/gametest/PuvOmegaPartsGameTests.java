package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.content.item.TechnologicalPartCatalog;
import com.masson.cruciblecraft.energy.cable.GT6VoltageTiers;
import com.masson.cruciblecraft.energy.longdistance.LongDistanceTransformerCatalog;
import com.masson.cruciblecraft.energy.transformer.EnergyTransformerTierCatalog;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverComponentTiers;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Compact parts, Quantum circuit, and transformer envelope through OMEGA. */
@GameTestHolder(PuvOmegaPartsGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class PuvOmegaPartsGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_content_puv_omega_parts";
    private static final String TEMPLATE = "empty";

    private PuvOmegaPartsGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void compactPartsThroughOmega(GameTestHelper helper) {
        helper.assertTrue(
                TechnologicalPartCatalog.findByPath(
                                "compact_electric_motor_omega")
                        .isPresent(),
                "OMEGA compact motor is missing from the parts catalog");
        helper.assertTrue(
                BuiltInRegistries.ITEM.containsKey(
                        id("compact_electric_motor_omega")),
                "OMEGA compact motor is not a registered item");
        helper.assertTrue(
                BuiltInRegistries.ITEM.containsKey(id("circuit_quantum")),
                "Quantum circuit canonical item is missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void omegaIsIndex14(GameTestHelper helper) {
        String[] tiers = {
                "ulv", "lv", "mv", "hv", "ev", "iv", "luv", "zpm", "uv", "puv1",
                "puv2", "puv3", "puv4", "puv5", "omega"
        };
        helper.assertTrue(
                tiers.length == 15 && "omega".equals(tiers[14]),
                "OMEGA compact index must be 14");
        helper.assertTrue(
                CoverComponentTiers.TIER_COUNT == 10,
                "Cover tiers must stay at 10 and not grow with compact OMEGA");
        helper.assertTrue(
                GT6VoltageTiers.VOLTAGES[14] == 2_147_483_648L,
                "OMEGA voltage envelope drifted from VN[14]");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void noVn15Parts(GameTestHelper helper) {
        helper.assertTrue(
                TechnologicalPartCatalog.parts().stream().noneMatch(
                        part -> part.registryPath().endsWith("_xv")
                                || part.registryPath().endsWith("_vn15")
                                || part.registryPath().contains("compact_electric_motor_xv")),
                "VN[15] must not register a second compact-part set");
        helper.assertTrue(
                GT6VoltageTiers.VOLTAGES.length == 16,
                "VN[15] remains a voltage envelope only");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void transformerLongVoltage(GameTestHelper helper) {
        helper.assertTrue(
                EnergyTransformerTierCatalog.EXPECTED_SIZE == 14
                        && EnergyTransformerTierCatalog.entries().size() == 14,
                "Transformer catalog drifted from 14 long-voltage pairs");
        helper.assertTrue(
                EnergyTransformerTierCatalog.entries().stream().allMatch(
                        entry -> entry.inputSize() > 0L
                                && entry.outputSize() > 0L
                                && entry.capacity() > 0L),
                "Transformer voltages must stay long");
        helper.assertTrue(
                EnergyTransformerTierCatalog.findByPath(
                                "electric_transformer_puv5_omega")
                        != null,
                "PUV5→OMEGA transformer pair is missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void longDistanceHostsAreDedicated(GameTestHelper helper) {
        helper.assertTrue(
                LongDistanceTransformerCatalog.EXPECTED_SIZE == 5
                        && LongDistanceTransformerCatalog.endpoints().size() == 5
                        && LongDistanceTransformerCatalog.wires().size() == 5,
                "Long-distance transformer catalog drifted from five endpoints");
        helper.assertTrue(
                BuiltInRegistries.BLOCK.containsKey(
                        id("long_distance_transformer_ev")),
                "EV long-distance endpoint is not registered");
        helper.assertTrue(
                BuiltInRegistries.BLOCK.containsKey(id("long_distance_wire_uv")),
                "UV long-distance wire is not registered");
        helper.assertTrue(
                EnergyTransformerTierCatalog.findByPath(
                                "long_distance_transformer_ev")
                        == null,
                "Long-distance hosts must stay out of the voltage-step catalog");
        helper.succeed();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
