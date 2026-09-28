package com.masson.cruciblecraft.client.render;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.sensor.SensorKind;
import com.masson.cruciblecraft.content.sensor.SensorMode;

import net.minecraft.resources.ResourceLocation;

/**
 * GT6 {@code MultiTileEntitySensorTE.getCharacterIcon} and
 * {@code getCharacterColor}: six slots, left to right.
 */
final class SensorDisplayLayout {
    static final int WHITE = 0xFFFFFFFF;
    static final int RED_192 = 0xFFC00000;
    private static final int RED = 0xFFFF0000;
    private static final int GREEN = 0xFF00FF00;
    private static final int BLUE = 0xFF0000FF;
    private static final int LIGHT_BLUE = 0xFF8080FF;
    private static final int YELLOW = 0xFFFFFF00;
    private static final int LIGHT_YELLOW = 0xFFFFFF80;
    private static final int GRAY = 0xFFC0C0C0;

    private SensorDisplayLayout() {}

    record Glyph(String icon, int argb) {}

    static Glyph[] glyphs(
            SensorKind kind,
            SensorMode mode,
            boolean hexadecimal,
            long number) {
        long value = number & 0xFFFFL;
        Glyph[] cells = new Glyph[SensorCharacterCells.COUNT];
        if (mode == SensorMode.FULL || mode == SensorMode.NOT_FULL) {
            cells[0] = glyph(mode == SensorMode.FULL ? "equal" : "smaller", RED_192);
            cells[1] = glyph("1", RED_192);
            cells[2] = glyph("0", RED_192);
            cells[3] = glyph("0", RED_192);
            cells[4] = glyph("percent", RED_192);
            cells[5] = unit(kind);
            return cells;
        }
        cells[0] = switch (mode) {
            case GREATER -> glyph("greater", WHITE);
            case EQUAL -> glyph("equal", WHITE);
            case SMALLER -> glyph("smaller", WHITE);
            case SCALE -> glyph("scale", WHITE);
            case DISPLAY, PERCENT -> hexadecimal
                    ? glyph("hex", WHITE)
                    : glyph(decimal(value, 4), WHITE);
            case FULL, NOT_FULL -> throw new IllegalStateException(mode.name());
        };
        for (int index = 1; index <= 4; index++) {
            int place = 4 - index;
            cells[index] = glyph(
                    hexadecimal ? hexadecimal(value, place) : decimal(value, place),
                    WHITE);
        }
        cells[5] = mode == SensorMode.PERCENT ? glyph("percent", WHITE) : unit(kind);
        return cells;
    }

    static ResourceLocation sprite(String icon) {
        return ResourceLocation.fromNamespaceAndPath(
                CrucibleCraft.MODID, "block/gt6_import/sensor_character/" + icon);
    }

    private static Glyph glyph(String icon, int argb) {
        return new Glyph(icon, argb);
    }

    private static String decimal(long value, int place) {
        long scale = 1L;
        for (int i = 0; i < place; i++) {
            scale *= 10L;
        }
        return Long.toString((value / scale) % 10L);
    }

    private static String hexadecimal(long value, int place) {
        int digit = (int) ((value >> (place * 4)) & 0xFL);
        return "0x" + Integer.toHexString(digit);
    }

    /** GT6 {@code getSymbolIcon}. Item and stack meters leave the cell empty. */
    private static Glyph unit(SensorKind kind) {
        return switch (kind) {
            case THERMOMETER -> glyph("kelvin", RED);
            case GIBBLOMETER, KILO_GIBBLOMETER -> glyph("gibbl", YELLOW);
            case LUMINOMETER -> glyph("lumin", LIGHT_YELLOW);
            case CHRONOMETER -> glyph("clock", GREEN);
            case TPS_METER -> glyph("clock", RED);
            case ITEMOMETER, STACKOMETER -> null;
            case FLUIDOMETER -> glyph("liter", BLUE);
            case BUCKETOMETER -> glyph("cubicmeter", BLUE);
            case KILO_BUCKETOMETER -> glyph("cubicdecameter", BLUE);
            case LIGHT_WEIGHTOMETER -> glyph("gramm", GRAY);
            case MEDIUM_WEIGHTOMETER -> glyph("kilogramm", GRAY);
            case HEAVY_WEIGHTOMETER -> glyph("ton", GRAY);
            case SUPER_HEAVY_WEIGHTOMETER -> glyph("kiloton", GRAY);
            case ELECTROMETER -> glyph("eu", RED);
            case PLAYER_COUNTER -> glyph("greg", LIGHT_BLUE);
            case PROGRESS_METER -> glyph("scale", LIGHT_BLUE);
            case TACHOMETER -> glyph("ru", GREEN);
            case GEIGER_COUNTER -> glyph("neutron", GREEN);
            case LASEROMETER -> glyph("lu", YELLOW);
        };
    }
}
