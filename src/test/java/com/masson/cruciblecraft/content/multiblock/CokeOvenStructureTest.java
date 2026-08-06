package com.masson.cruciblecraft.content.multiblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;

import com.google.gson.JsonParser;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.Offset;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PredicateKind;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.Direction;

import org.junit.jupiter.api.Test;

class CokeOvenStructureTest {
    @Test
    void eachHorizontalFacingRequiresTwentyFiveUniqueFirebricks() {
        MultiblockStructureDefinition definition = definition();
        var localFirebricks = definition.structure().stream()
                .filter(element -> definition.predicate(element).kind()
                        == PredicateKind.BLOCK)
                .map(MultiblockStructureDefinition.Element::offset)
                .toList();
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            var offsets = localFirebricks.stream()
                    .map(offset -> offset.rotate(facing))
                    .toList();
            assertEquals(25, offsets.size());
            assertEquals(25, new HashSet<>(offsets).size());
            assertFalse(offsets.contains(Offset.ZERO));
            assertFalse(offsets.contains(
                    definition.anchors().get("center").rotate(facing)));
        }
    }

    @Test
    void controllerOccupiesTheOutwardMiddleOfTheShell() {
        MultiblockStructureDefinition definition = definition();
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            Offset center = definition.anchors().get("center").rotate(facing);
            assertEquals(-facing.getStepX(), center.x());
            assertEquals(-facing.getStepZ(), center.z());
        }
    }

    @Test
    void heatSourceIsBelowTheCenterOfTheBottomLayer() {
        MultiblockStructureDefinition definition = definition();
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            Offset center = definition.anchors().get("center").rotate(facing);
            assertEquals(
                    new Offset(center.x(), -2, center.z()),
                    definition.anchors().get("heat_source").rotate(facing));
        }
    }

    @Test
    void jsonPreservesTheLegacyTwentySevenBlockScanVolume() {
        MultiblockStructureDefinition definition = definition();
        assertEquals(27, definition.structure().size());
        assertEquals(27, definition.scanVolume());
        var source = document().getAsJsonObject("source");
        assertEquals(
                "behavior_migration:NO_BEHAVIOR_DRIFT",
                source.get("method").getAsString());
        assertTrue(source.get("revision").getAsString()
                .contains("t12a_machine_readiness.json"));
        assertTrue(source.get("class").getAsString()
                .contains("CokeOvenStructure"));
    }

    private static MultiblockStructureDefinition definition() {
        return MultiblockStructureDefinition.CODEC
                .parse(JsonOps.INSTANCE, document())
                .getOrThrow(AssertionError::new);
    }

    private static com.google.gson.JsonObject document() {
        var stream = CokeOvenStructureTest.class.getResourceAsStream(
                "/data/cruciblecraft/multiblock_structures/coke_oven.json");
        if (stream == null) {
            throw new AssertionError("Missing coke_oven.json");
        }
        try (var reader = new InputStreamReader(
                stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (java.io.IOException exception) {
            throw new AssertionError(exception);
        }
    }
}
