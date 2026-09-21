package com.masson.cruciblecraft.energy.steam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

class SteamTurbineConversionTest {
    private static SteamTurbineCatalog.Profile bronze() {
        return SteamTurbineCatalog.require(
                ResourceLocation.parse("cruciblecraft:steam/turbine_bronze"));
    }

    private static SteamTurbineCatalog.Profile magnalium() {
        return SteamTurbineCatalog.require(ResourceLocation.parse(
                "cruciblecraft:magnalium/steam_turbine_main_housing"));
    }

    @Test
    void bronzeCapacitorAndTankMatchGt6Input() {
        SteamTurbineCatalog.Profile bronze = bronze();
        assertEquals(48, bronze.steamInputMax());
        assertEquals(16L, bronze.ruOutput());
        assertEquals(96L, bronze.energyCapacity());
        assertEquals(192, bronze.tankCapacityMb());
        assertEquals(48, SteamTurbineConversion.conversionThresholdMb(bronze));
        assertEquals(8L, SteamTurbineConversion.outputMin(bronze));
        assertEquals(32L, SteamTurbineConversion.outputMax(bronze));
    }

    @Test
    void bronzeMinDumpEmitsEightRuThenWastes() {
        SteamTurbineCatalog.Profile bronze = bronze();
        long ru = SteamTurbineConversion.ruFromSteam(48, bronze);
        assertEquals(24L, ru);
        SteamTurbineConversion.Tick tick = SteamTurbineConversion.emit(ru, bronze);
        assertTrue(tick.canEmit());
        assertFalse(tick.overloaded());
        assertEquals(8L, tick.packetSize());
        assertEquals(0L, tick.storedAfterWaste());
        assertFalse(tick.fast());
    }

    @Test
    void bronzeFullTankSecondTickOverloads() {
        SteamTurbineCatalog.Profile bronze = bronze();
        long first = SteamTurbineConversion.ruFromSteam(192, bronze);
        assertEquals(96L, first);
        SteamTurbineConversion.Tick tick1 = SteamTurbineConversion.emit(first, bronze);
        assertTrue(tick1.canEmit());
        assertEquals(32L, tick1.packetSize());
        assertTrue(tick1.fast());
        assertEquals(48L, tick1.storedAfterWaste());
        SteamTurbineConversion.Tick tick2 = SteamTurbineConversion.emit(
                tick1.storedAfterWaste() + first, bronze);
        assertTrue(tick2.overloaded());
        assertFalse(tick2.canEmit());
        assertEquals(0L, tick2.storedAfterWaste());
        assertEquals(5, SteamTurbinePresentation.yawDelta(false, false));
        assertEquals(-10, SteamTurbinePresentation.yawDelta(true, true));
        assertEquals(
                SteamTurbinePresentation.OVERLOAD_EXPLOSION_THRESHOLD, 100);
    }

    @Test
    void magnaliumThresholdIsSteamInputMax() {
        SteamTurbineCatalog.Profile large = magnalium();
        assertEquals(12_288, large.steamInputMax());
        assertEquals(170, large.steamPerWater());
        assertEquals(24_576L, large.energyCapacity());
        assertEquals(49_152, large.tankCapacityMb());
        assertTrue(large.large());
    }

    @Test
    void steamHatchCountsMatchGt6Horizontal() {
        assertTrue(
                SteamTurbineHatches.counts(
                                new BlockPos(1, 2, 2), Direction.WEST)
                        .matchesGt6Horizontal());
    }
}
