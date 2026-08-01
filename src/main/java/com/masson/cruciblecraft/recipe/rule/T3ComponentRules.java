package com.masson.cruciblecraft.recipe.rule;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.resources.ResourceLocation;

/** Compact metadata-gated transformations executed by the configured T3 machines. */
public final class T3ComponentRules {
    public static final List<Definition> ALL = List.of(
            simple("extruder/ingot_to_long_rod", ModRecipeMaps.EXTRUDER.id(), "working",
                    MaterialPrefixes.INGOT, MaterialPrefixes.LONG_ROD, 100, 32),
            simple("cutter/plate_to_foil", ModRecipeMaps.CUTTER.id(), "cutting",
                    MaterialPrefixes.PLATE, MaterialPrefixes.FOIL, 80, 24),
            simple("cutter/gear_to_small_gears", ModRecipeMaps.CUTTER.id(), "cutting",
                    MaterialPrefixes.GEAR, MaterialPrefixes.SMALL_GEAR, 120, 32),
            simple("lathe/ingot_to_rods", ModRecipeMaps.LATHE.id(), "working",
                    MaterialPrefixes.INGOT, MaterialPrefixes.ROD, 120, 24),
            simple("lathe/bolts_to_screws", ModRecipeMaps.LATHE.id(), "working",
                    MaterialPrefixes.BOLT, MaterialPrefixes.SCREW, 64, 16),
            simple("rollingmill/ingot_to_plate", ModRecipeMaps.ROLLINGMILL.id(), "bending",
                    MaterialPrefixes.INGOT, MaterialPrefixes.PLATE, 100, 32),
            simple("rollbender/rod_to_rings", ModRecipeMaps.ROLLBENDER.id(), "bending",
                    MaterialPrefixes.ROD, MaterialPrefixes.RING, 80, 24),
            simple("wiremill/ingot_to_wire", ModRecipeMaps.WIREMILL.id(), "working",
                    MaterialPrefixes.INGOT, MaterialPrefixes.WIRE, 120, 32),
            simple("wiremill/foil_to_fine_wire", ModRecipeMaps.WIREMILL.id(), "working",
                    MaterialPrefixes.FOIL, MaterialPrefixes.FINE_WIRE, 80, 24),
            simple("wiremill/wire_to_double_wire", ModRecipeMaps.WIREMILL.id(), "working",
                    MaterialPrefixes.WIRE, MaterialPrefixes.DOUBLE_WIRE, 80, 24),
            simple("wiremill/double_to_quadruple_wire", ModRecipeMaps.WIREMILL.id(), "working",
                    MaterialPrefixes.DOUBLE_WIRE, MaterialPrefixes.QUADRUPLE_WIRE, 100, 32),
            simple("wiremill/quadruple_to_octuple_wire", ModRecipeMaps.WIREMILL.id(), "working",
                    MaterialPrefixes.QUADRUPLE_WIRE, MaterialPrefixes.OCTUPLE_WIRE, 120, 32),
            multi("wiremill/octuple_and_quadruple_to_dodecuple_wire",
                    ModRecipeMaps.WIREMILL.id(), "working",
                    List.of(new Input(MaterialPrefixes.OCTUPLE_WIRE, 1),
                            new Input(MaterialPrefixes.QUADRUPLE_WIRE, 1)),
                    MaterialPrefixes.DODECUPLE_WIRE, 864, 160, 48),
            multi("wiremill/octuple_to_hexadecuple_wire",
                    ModRecipeMaps.WIREMILL.id(), "working",
                    List.of(new Input(MaterialPrefixes.OCTUPLE_WIRE, 2)),
                    MaterialPrefixes.HEXADECUPLE_WIRE, 1152, 200, 48),
            simple("bender/long_rod_to_spring", ModRecipeMaps.BENDER.id(), "bending",
                    MaterialPrefixes.LONG_ROD, MaterialPrefixes.SPRING, 120, 32),
            simple("bender/rod_to_small_springs", ModRecipeMaps.BENDER.id(), "bending",
                    MaterialPrefixes.ROD, MaterialPrefixes.SMALL_SPRING, 80, 24),
            multi("assembler/plates_and_ring_to_rotor", ModRecipeMaps.ASSEMBLER.id(), "working",
                    List.of(new Input(MaterialPrefixes.PLATE, 4),
                            new Input(MaterialPrefixes.RING, 1)),
                    MaterialPrefixes.ROTOR, 612, 300, 64),
            multi("assembler/plates_to_gear", ModRecipeMaps.ASSEMBLER.id(), "working",
                    List.of(new Input(MaterialPrefixes.PLATE, 4)),
                    MaterialPrefixes.GEAR, 576, 240, 48),
            cable("assembler/wire_and_rubber_to_cable",
                    MaterialPrefixes.WIRE, MaterialPrefixes.CABLE, 1, 120, 32),
            cable("assembler/double_wire_and_rubber_to_double_cable",
                    MaterialPrefixes.DOUBLE_WIRE, MaterialPrefixes.DOUBLE_CABLE, 1, 140, 32),
            cable("assembler/quadruple_wire_and_rubber_to_quadruple_cable",
                    MaterialPrefixes.QUADRUPLE_WIRE, MaterialPrefixes.QUADRUPLE_CABLE, 2, 180, 48),
            cable("assembler/octuple_wire_and_rubber_to_octuple_cable",
                    MaterialPrefixes.OCTUPLE_WIRE, MaterialPrefixes.OCTUPLE_CABLE, 3, 240, 64),
            cable("assembler/dodecuple_wire_and_rubber_to_dodecuple_cable",
                    MaterialPrefixes.DODECUPLE_WIRE, MaterialPrefixes.DODECUPLE_CABLE, 4, 300, 64),
            multi("welder/plates_to_double_plate", ModRecipeMaps.WELDER.id(), "working",
                    List.of(new Input(MaterialPrefixes.PLATE, 1),
                            new Input(MaterialPrefixes.PLATE, 1)),
                    MaterialPrefixes.DOUBLE_PLATE, 288, 160, 48),
            multi("press/double_and_plate_to_triple_plate",
                    ModRecipeMaps.PRESS.id(), "working",
                    List.of(new Input(MaterialPrefixes.DOUBLE_PLATE, 1),
                            new Input(MaterialPrefixes.PLATE, 1)),
                    MaterialPrefixes.TRIPLE_PLATE, 432, 180, 48),
            multi("press/double_plates_to_quadruple_plate",
                    ModRecipeMaps.PRESS.id(), "working",
                    List.of(new Input(MaterialPrefixes.DOUBLE_PLATE, 2)),
                    MaterialPrefixes.QUADRUPLE_PLATE, 576, 220, 64),
            multi("press/quadruple_and_plate_to_quintuple_plate",
                    ModRecipeMaps.PRESS.id(), "working",
                    List.of(new Input(MaterialPrefixes.QUADRUPLE_PLATE, 1),
                            new Input(MaterialPrefixes.PLATE, 1)),
                    MaterialPrefixes.QUINTUPLE_PLATE, 720, 260, 64),
            multi("press/triple_plates_to_dense_plate",
                    ModRecipeMaps.PRESS.id(), "working",
                    List.of(new Input(MaterialPrefixes.TRIPLE_PLATE, 3)),
                    MaterialPrefixes.DENSE_PLATE, 1296, 400, 96),
            rubberCompatibilityBridge());

    private T3ComponentRules() {}

    private static Definition simple(
            String path,
            ResourceLocation map,
            String target,
            MaterialPrefix input,
            MaterialPrefix output,
            int duration,
            long eut) {
        return multi(path, map, target, List.of(new Input(input, 1)),
                output, input.units(), duration, eut);
    }

    private static Definition multi(
            String path,
            ResourceLocation map,
            String target,
            List<Input> inputs,
            MaterialPrefix output,
            int sourceUnits,
            int duration,
            long eut) {
        String numerator = "target_units(" + target + ") * " + sourceUnits;
        String denominator = "144 * prefix_units(" + output.serializedName() + ")";
        String divisor = "gcd(" + numerator + ", " + denominator + ")";
        String batchInputs = denominator + " / " + divisor;
        String batchOutputs = numerator + " / " + divisor;
        List<MaterialRule.ItemResource> itemInputs = inputs.stream()
                .map(input -> item(input.prefix(), input.count() + " * (" + batchInputs + ")"))
                .toList();
        MaterialRule.ItemResource itemOutput = new MaterialRule.ItemResource(
                Optional.of(output.serializedId()),
                Optional.empty(),
                batchOutputs,
                "10000",
                Optional.of("processing_target:" + target),
                false);
        return new Definition(path, new MaterialRule(
                Optional.of(map),
                itemInputs,
                List.of(itemOutput),
                List.of(),
                List.of(),
                duration + " * (" + batchInputs + ")",
                Long.toString(eut),
                "0",
                true,
                Optional.empty(),
                Map.of(),
                List.of(),
                Optional.empty(),
                List.of()));
    }

    private static MaterialRule.ItemResource item(MaterialPrefix prefix, String count) {
        return new MaterialRule.ItemResource(
                Optional.of(prefix.serializedId()),
                Optional.empty(),
                count,
                "10000");
    }

    private static Definition cable(
            String path,
            MaterialPrefix wire,
            MaterialPrefix cable,
            int rubberPlates,
            int duration,
            long eut) {
        MaterialRule.ItemResource insulation = new MaterialRule.ItemResource(
                Optional.of(MaterialPrefixes.PLATE.serializedId()),
                Optional.empty(),
                Integer.toString(rubberPlates),
                "10000",
                Optional.of("material:rubber"),
                false);
        return new Definition(path, new MaterialRule(
                Optional.of(ModRecipeMaps.ASSEMBLER.id()),
                List.of(item(wire, "1"), insulation),
                List.of(item(cable, "1")),
                List.of(),
                List.of(),
                Integer.toString(duration),
                Long.toString(eut),
                "0",
                true,
                Optional.empty(),
                Map.of(),
                List.of(),
                Optional.empty(),
                List.of()));
    }

    /**
     * Intentional survival bridge until GT rubber-tree/chemical processing is
     * ported: one obtainable vanilla slime ball is pressed into one Rubber plate.
     */
    private static Definition rubberCompatibilityBridge() {
        MaterialRule.ItemResource slime = new MaterialRule.ItemResource(
                Optional.empty(),
                Optional.of(ResourceLocation.withDefaultNamespace("slime_ball")),
                "1",
                "10000");
        return new Definition("press/slime_ball_to_rubber_plate", new MaterialRule(
                Optional.of(ModRecipeMaps.PRESS.id()),
                List.of(slime),
                List.of(item(MaterialPrefixes.PLATE, "1")),
                List.of(),
                List.of(),
                "120",
                "32",
                "0",
                true,
                Optional.of("rubber"),
                Map.of(),
                List.of(),
                Optional.empty(),
                List.of()));
    }

    private record Input(MaterialPrefix prefix, int count) {}
    public record Definition(String path, MaterialRule rule) {}
}
