package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.CableBlock;
import com.masson.cruciblecraft.content.block.FluidPipeBlock;
import com.masson.cruciblecraft.content.item.CableBlockItem;
import com.masson.cruciblecraft.content.item.CatalogNamedItem;
import com.masson.cruciblecraft.content.item.PipeBlockItem;
import com.masson.cruciblecraft.logistics.pipe.PipeCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated HSLA connector alias repair. Run with
 * {@code -PwaveRecipes=content/gt6-connector-alias-repair}.
 */
@GameTestHolder(ConnectorAliasRepairGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class ConnectorAliasRepairGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_content_gt6_connector_alias_repair";
    private static final String TEMPLATE = "empty";

    private ConnectorAliasRepairGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void hslaSteelFiveGaugePipesFoldOntoLiveHosts(
            GameTestHelper helper) {
        helper.assertTrue(
                liveFluid("tiny_fluid_pipe")
                        && liveFluid("small_fluid_pipe")
                        && liveFluid("fluid_pipe")
                        && liveFluid("large_fluid_pipe")
                        && liveFluid("huge_fluid_pipe"),
                "hslasteel five-gauge fluid pipes lost their BlockItems");
        helper.assertTrue(
                withdrawn("fluid_pipe_tile/tiny_hsla_steel_fluid_pipe")
                        && withdrawn("fluid_pipe_tile/small_hsla_steel_fluid_pipe")
                        && withdrawn("fluid_pipe_tile/hslasteel")
                        && withdrawn("fluid_pipe_tile/large_hsla_steel_fluid_pipe")
                        && withdrawn("fluid_pipe_tile/huge_hsla_steel_fluid_pipe"),
                "folded HSLA fluid-pipe dummies are still registered");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void hslaSteelMappedWiresFoldOntoLiveHosts(
            GameTestHelper helper) {
        helper.assertTrue(
                liveWire(MaterialPrefixes.WIRE)
                        && liveWire(MaterialPrefixes.TRIPLE_WIRE)
                        && liveWire(MaterialPrefixes.QUINTUPLE_WIRE)
                        && liveWire(MaterialPrefixes.SEXTUPLE_WIRE),
                "hslasteel mapped wires lost their CableBlockItems");
        helper.assertTrue(
                withdrawn("electric_wire/1x_hsla_steel_wire")
                        && withdrawn("electric_wire/3x_hsla_steel_wire")
                        && withdrawn("electric_wire/5x_hsla_steel_wire")
                        && withdrawn("electric_wire/6x_hsla_steel_wire"),
                "folded HSLA wire dummies are still registered");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void hslaSteelUngatedGaugesStayDummy(GameTestHelper helper) {
        Item seven = item("electric_wire/7x_hsla_steel_wire");
        helper.assertTrue(
                seven instanceof CatalogNamedItem
                        && !(seven instanceof CableBlockItem),
                "ungated HSLA 7x wire disappeared or became a live CableBlockItem");
        helper.assertTrue(
                liveFluid("tiny_fluid_pipe")
                        && ModItems.hasMaterialItem(
                                "hslasteel",
                                MaterialPrefixes.QUADRUPLE_FLUID_PIPE)
                        && ModItems.materialItem(
                                        "hslasteel",
                                        MaterialPrefixes.QUADRUPLE_FLUID_PIPE)
                                .get() instanceof PipeBlockItem,
                "HSLA combo pipes were not folded onto live hslasteel hosts");
        helper.assertTrue(
                withdrawn("fluid_pipe_tile/quadruple_hsla_steel_fluid_pipe")
                        && withdrawn("fluid_pipe_tile/nonuple_hsla_steel_fluid_pipe"),
                "folded HSLA combo dummies are still registered");
        helper.assertTrue(
                !ModBlocks.hasPipeBlock(
                        "hslasteel",
                        MaterialPrefixes.FLUID_PIPE,
                        PipeCatalog.Kind.ITEM),
                "hslasteel fluid prefix leaked into the item catalog");
        helper.succeed();
    }

    private static boolean liveFluid(String form) {
        var prefix = switch (form) {
            case "tiny_fluid_pipe" -> MaterialPrefixes.TINY_FLUID_PIPE;
            case "small_fluid_pipe" -> MaterialPrefixes.SMALL_FLUID_PIPE;
            case "fluid_pipe" -> MaterialPrefixes.FLUID_PIPE;
            case "large_fluid_pipe" -> MaterialPrefixes.LARGE_FLUID_PIPE;
            case "huge_fluid_pipe" -> MaterialPrefixes.HUGE_FLUID_PIPE;
            default -> throw new IllegalArgumentException(form);
        };
        return ModBlocks.hasPipeBlock("hslasteel", prefix, PipeCatalog.Kind.FLUID)
                && ModBlocks.pipeBlock(
                                "hslasteel", prefix, PipeCatalog.Kind.FLUID)
                        .get() instanceof FluidPipeBlock
                && ModItems.hasMaterialItem("hslasteel", prefix)
                && ModItems.materialItem("hslasteel", prefix).get()
                        instanceof PipeBlockItem;
    }

    private static boolean liveWire(
            com.masson.cruciblecraft.api.material.MaterialPrefix prefix) {
        return ModBlocks.hasElectricalConductorBlock("hslasteel", prefix)
                && ModBlocks.electricalConductorBlock("hslasteel", prefix).get()
                        instanceof CableBlock
                && ModItems.hasMaterialItem("hslasteel", prefix)
                && ModItems.materialItem("hslasteel", prefix).get()
                        instanceof CableBlockItem;
    }

    private static boolean withdrawn(String path) {
        return !BuiltInRegistries.ITEM.containsKey(
                ResourceLocation.fromNamespaceAndPath("cruciblecraft", path));
    }

    private static Item item(String path) {
        return BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("cruciblecraft", path));
    }
}
