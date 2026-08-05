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
}
