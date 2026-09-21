package com.masson.cruciblecraft.energy.largedynamo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

class LargeDynamoConversionTest {
    private static LargeDynamoCatalog.Profile stainless() {
        return LargeDynamoCatalog.require(ResourceLocation.parse(
                "cruciblecraft:stainless_steel/dynamo_main_housing"));
    }

    @Test
    void fourProfilesMatchGt6RuToEu() {
        long[][] expected = {
                {17221, 4096L, 3072L, 8192L},
                {17222, 8192L, 6144L, 16384L},
                {17223, 16384L, 12288L, 32768L},
                {17224, 131072L, 98304L, 262144L}
        };
        int index = 0;
        for (LargeDynamoCatalog.Profile profile : LargeDynamoCatalog.profiles()) {
            assertEquals(expected[index][0], profile.sourceId());
            assertEquals(expected[index][1], profile.inputRec());
            assertEquals(expected[index][2], profile.outputRec());
            assertEquals(expected[index][3], profile.capacity());
            index++;
        }
        assertEquals(4, index);
    }

    @Test
    void profileWindowsMatchGt6EnergyStats() {
        LargeDynamoCatalog.Profile stainless = stainless();
        assertEquals(2048L, stainless.inputRec() / 2L);
        assertEquals(4096L, stainless.inputRec());
        assertEquals(8192L, stainless.inputMax());
        assertEquals(1536L, stainless.outputMin());
        assertEquals(6144L, stainless.outputMax());
    }

    @Test
    void stainlessFullInputEmits3072EuThenWastes() {
        LargeDynamoCatalog.Profile stainless = stainless();
        LargeDynamoConversion.Tick tick = LargeDynamoConversion.emit(4096L, stainless);
        assertTrue(tick.canEmit());
        assertFalse(tick.overloaded());
        assertEquals(3072L, tick.packetSize());
        assertEquals(0L, tick.storedAfterWaste());
    }

    @Test
    void stainlessAtOutputMaxDoesNotOverload() {
        LargeDynamoCatalog.Profile stainless = stainless();
        LargeDynamoConversion.Tick tick = LargeDynamoConversion.emit(8192L, stainless);
        assertTrue(tick.canEmit());
        assertFalse(tick.overloaded());
        assertEquals(6144L, tick.packetSize());
        assertEquals(0L, tick.storedAfterWaste());
    }

    @Test
    void stainlessAboveOutputMaxOverloads() {
        LargeDynamoCatalog.Profile stainless = stainless();
        LargeDynamoConversion.Tick tick = LargeDynamoConversion.emit(16384L, stainless);
        assertFalse(tick.canEmit());
        assertTrue(tick.overloaded());
        assertEquals(0L, tick.storedAfterWaste());
    }
}
