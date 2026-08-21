package com.masson.cruciblecraft.content.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.content.item.CableBlockItem;
import com.masson.cruciblecraft.content.item.MaterialFormItem;

class CableBlockStateTest {
    @Test
    void eachConductorHasExactlySixConnectionBooleans() {
        assertEquals(6, CableBlock.PROPERTY_BY_DIRECTION.size());
        CableBlock.PROPERTY_BY_DIRECTION.values().forEach(property ->
                assertEquals(2, property.getPossibleValues().size()));
        assertEquals(
                64,
                1 << CableBlock.PROPERTY_BY_DIRECTION.size());
    }

    @Test
    void conductorBlockItemUsesTheCommonMaterialFormIdentity() {
        assertTrue(MaterialFormItem.class.isAssignableFrom(
                CableBlockItem.class));
    }

    @Test
    void cablesAndPipesUseGt6StylePlacementInsteadOfAutoConnect()
            throws Exception {
        String cables = java.nio.file.Files.readString(
                java.nio.file.Path.of(
                        "src/main/java/com/masson/cruciblecraft/content/block/CableBlock.java"));
        String pipes = java.nio.file.Files.readString(
                java.nio.file.Path.of(
                        "src/main/java/com/masson/cruciblecraft/content/block/AbstractPipeBlock.java"));
        String items = java.nio.file.Files.readString(
                java.nio.file.Path.of(
                        "src/main/java/com/masson/cruciblecraft/content/item/CableBlockItem.java"));
        assertTrue(cables.contains("return defaultBlockState();"));
        assertTrue(pipes.contains("return defaultBlockState();"));
        // GT6 getFacingTool split: cables use the wire cutter, pipes the wrench.
        assertTrue(cables.contains("Gt6StyleConnections.cutter"));
        assertTrue(pipes.contains("Gt6StyleConnections.wrench"));
        assertTrue(items.contains("Gt6StyleConnections.placeBlock"));
        assertTrue(cables.contains("return state;"));
        assertTrue(cables.contains("interactionShape"));
        assertTrue(pipes.contains("interactionShape"));
    }
}
