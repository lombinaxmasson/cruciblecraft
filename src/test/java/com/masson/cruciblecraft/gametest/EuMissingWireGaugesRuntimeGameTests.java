package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.CableBlock;
import com.masson.cruciblecraft.content.item.CableBlockItem;
import com.masson.cruciblecraft.energy.cable.ElectricalConductorCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated GT6 missing EU wire-gauge runtime. Run with
 * {@code -PgameTestGrid=energy}.
 */
@GameTestHolder(EuMissingWireGaugesRuntimeGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class EuMissingWireGaugesRuntimeGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_energy";
    private static final String TEMPLATE = "empty";

    private EuMissingWireGaugesRuntimeGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void missingGaugeWiresAreLiveCableBlocks(
            GameTestHelper helper) {
        helper.assertTrue(
                ElectricalConductorCatalog.wires().size()
                        == ElectricalConductorCatalog.EXPECTED_WIRE_BLOCKS
                        && ElectricalConductorCatalog.EXPECTED_WIRE_BLOCKS
                                == 473,
                "missing-gauge wire census drifted: "
                        + ElectricalConductorCatalog.wires().size());
        var septuple = ElectricalConductorCatalog.require(
                "tin", MaterialPrefixes.SEPTUPLE_WIRE);
        helper.assertTrue(
                "wireGt07".equals(septuple.sourceSpecification())
                        && septuple.electrical().maxAmperage() == 7L
                        && ElectricalConductorCatalog.widthPixels("wireGt07")
                                == 8
                        && live("tin", MaterialPrefixes.SEPTUPLE_WIRE),
                "tin wireGt07 is not a live 7-amp CableBlock");
        helper.assertTrue(
                live("tin", MaterialPrefixes.NONUPLE_WIRE)
                        && live("tin", MaterialPrefixes.PENTADECUPLE_WIRE)
                        && ElectricalConductorCatalog.widthPixels("wireGt15")
                                == 15,
                "tin 9x/15x missing gauges were not registered");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void missingGaugesAreNotMappedWireAlias(
            GameTestHelper helper) {
        var septuple = ElectricalConductorCatalog.require(
                "tin", MaterialPrefixes.SEPTUPLE_WIRE);
        var octuple = ElectricalConductorCatalog.require(
                "tin", MaterialPrefixes.OCTUPLE_WIRE);
        helper.assertTrue(
                septuple.electrical().maxAmperage() == 7L
                        && octuple.electrical().maxAmperage() == 8L
                        && !septuple.form().equals(octuple.form())
                        && ElectricalConductorCatalog.widthPixels("wireGt07")
                                == ElectricalConductorCatalog.widthPixels(
                                        "wireGt08"),
                "wireGt07 was aliased onto octuple_wire");
        helper.assertTrue(
                !ElectricalConductorCatalog.contains(
                        "red_alloy", MaterialPrefixes.SEPTUPLE_WIRE)
                        && !ElectricalConductorCatalog.contains(
                                "signalum", MaterialPrefixes.SEPTUPLE_WIRE)
                        && !ElectricalConductorCatalog.contains(
                                "lumium", MaterialPrefixes.SEPTUPLE_WIRE),
                "redstone materials entered missing-gauge EU catalog");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void missingGaugeDummiesFoldOntoLiveHost(
            GameTestHelper helper) {
        helper.assertTrue(
                live("tin", MaterialPrefixes.SEPTUPLE_WIRE)
                        && live("hslasteel", MaterialPrefixes.SEPTUPLE_WIRE)
                        && live("gold", MaterialPrefixes.DECUPLE_WIRE),
                "gated missing-gauge BlockItems disappeared");
        helper.assertTrue(
                withdrawn("electric_wire/7x_tin_wire")
                        && withdrawn("electric_wire/7x_hsla_steel_wire")
                        && withdrawn("electric_wire/9x_tin_wire")
                        && withdrawn("electric_wire/15x_tin_wire"),
                "folded missing-gauge dummies are still registered");
        helper.assertTrue(
                withdrawn("electric_wire/2x_blue_alloy_wire")
                        && live("blue_alloy", MaterialPrefixes.DOUBLE_WIRE),
                "upgrade_live_item dummy is still registered beside its CableBlock");
        helper.succeed();
    }

    private static boolean live(String material, MaterialPrefix form) {
        return ElectricalConductorCatalog.contains(material, form)
                && ModBlocks.electricalConductorBlock(material, form).get()
                        instanceof CableBlock
                && ModItems.materialItem(material, form).get()
                        instanceof CableBlockItem;
    }

    private static boolean withdrawn(String path) {
        return !BuiltInRegistries.ITEM.containsKey(
                ResourceLocation.fromNamespaceAndPath("cruciblecraft", path));
    }
}
