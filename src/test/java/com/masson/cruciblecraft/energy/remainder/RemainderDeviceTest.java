package com.masson.cruciblecraft.energy.remainder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.energy.remainder.RemainderDevice.ObtainKind;

class RemainderDeviceTest {
    @Test
    void catalogMatchesGt6LoaderRows() {
        assertEquals(RemainderDevices.HOST_COUNT, RemainderDevices.devices().size());
        assertEquals(
                RemainderDevices.NEW_BLOCK_COUNT,
                RemainderDevices.placeable().size());
        RemainderDevice silicon = RemainderDevices.requireMeta(10050);
        RemainderDevice germanium = RemainderDevices.requireMeta(10051);
        // gt6-source: Loader_MultiTileEntities solar output 8 and 16
        assertEquals(8L, silicon.output());
        assertEquals(16L, germanium.output());
        assertEquals(EnergyType.ELECTRIC, silicon.energy());
        RemainderDevice crystal = RemainderDevices.requireMeta(10130);
        assertEquals(EnergyType.LU, crystal.energy());
        assertEquals(4, crystal.slots());
        RemainderDevice largeCrystal = RemainderDevices.requireMeta(10140);
        assertEquals(16, largeCrystal.slots());
        RemainderDevice largeBox = RemainderDevices.requireMeta(10091);
        assertTrue(RemainderDevices.obtain(largeBox).parts().stream()
                .anyMatch(part -> part.token().equals("transformer:10041")));
        assertEquals(
                ObtainKind.BLOCKED,
                RemainderDevices.obtain(RemainderDevices.requireMeta(10080)).kind());
        assertEquals(
                ObtainKind.BLOCKED,
                RemainderDevices.obtain(RemainderDevices.requireMeta(10099)).kind());
        assertEquals(
                ObtainKind.BLOCKED,
                RemainderDevices.obtain(RemainderDevices.requireMeta(10180)).kind());
        assertEquals(
                ObtainKind.ALREADY_LIVE,
                RemainderDevices.obtain(RemainderDevices.requireMeta(10086)).kind());
        assertEquals(
                ObtainKind.SHAPED,
                RemainderDevices.obtain(RemainderDevices.requireMeta(10050)).kind());
        assertTrue(RemainderDevices.devices().stream()
                .map(device -> device.kind().gt6Class())
                .distinct()
                .count() == RemainderDevice.Kind.values().length);
    }

    @Test
    void solarOfferFollowsSkyWeather() {
        // gt6-source: MultiTileEntitySolarPanelElectric.generateEnergy
        assertEquals(8L, SolarGeneration.offer(8L, true, false, true, false, true));
        assertEquals(1L, SolarGeneration.offer(8L, true, false, true, true, true));
        assertEquals(1L, SolarGeneration.offer(8L, true, false, false, false, true));
        assertEquals(0L, SolarGeneration.offer(8L, true, false, false, true, true));
        assertEquals(0L, SolarGeneration.offer(8L, true, true, true, true, true));
        assertEquals(0L, SolarGeneration.offer(8L, false, false, true, false, false));
        assertEquals(8L, SolarGeneration.offer(8L, true, false, true, true, false));
        assertTrue(SolarGeneration.active(1L, 8L));
        assertTrue(!SolarGeneration.active(0L, 8L));
    }

    @Test
    void batBoxBandMatchesBind3() {
        // gt6-source: UT.Code.bind3 and TileEntityBase10EnergyBatBox onTick2
        assertEquals(0, BatBoxMath.bind3(-1L));
        assertEquals(7, BatBoxMath.bind3(9L));
        assertEquals(0, BatBoxMath.band(0L, 8L, 4));
        assertEquals(7, BatBoxMath.band(7L * 8L * 40L * 4L, 8L, 4));
        assertEquals(40, BatBoxMath.packets(0));
        assertEquals(20, BatBoxMath.packets(1));
        assertEquals(0, BatBoxMath.packets(3));
        assertEquals(-20, BatBoxMath.packets(6));
        assertEquals(-40, BatBoxMath.packets(7));
        assertEquals(2, BatBoxMath.emitCount(0, 2));
        assertEquals(1, BatBoxMath.emitCount(1, 4));
        assertEquals(0, BatBoxMath.visual(0L, 8L, 8L, 4));
        assertEquals(2, BatBoxMath.visual(8L, 8L, 8L, 4));
        assertEquals(1, BatBoxMath.visual(
                8L * BatBoxMath.FULL_VISUAL_FACTOR * 4L, 8L, 8L, 4));
    }

    @Test
    void shapedBatteryRecipeUsesTheGt6Grid() throws Exception {
        Path recipe = Path.of(
                "src/generated/resources/data/cruciblecraft/recipe/steel_galvanized_battery_box.json");
        assertTrue(Files.exists(recipe), "datagen did not emit the LV battery box");
        String json = Files.readString(recipe);
        assertTrue(json.contains("\"WCW\""));
        assertTrue(json.contains("\"XMX\""));
        assertTrue(json.contains("cruciblecraft:steel_galvanized/machine_casing"));
        assertTrue(json.contains("cruciblecraft:circuit_basic"));
        Path blocked = Path.of(
                "src/generated/resources/data/cruciblecraft/recipe/tin_alloy_battery_box.json");
        assertTrue(Files.notExists(blocked), "primitive-circuit battery box was emitted");
        Path magic = Path.of(
                "src/generated/resources/data/cruciblecraft/recipe/palladium_magic_field_absorber.json");
        assertTrue(Files.notExists(magic), "magic absorber recipe was emitted");
    }
}
