package com.masson.cruciblecraft.content.sensor;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.masson.cruciblecraft.CrucibleCraft;

import net.minecraft.resources.ResourceLocation;

/**
 * GT6 Sensor MTE 31000–31023 (gaps 31008, 31009, 31014). One CC block
 * identity per source id. {@code compact_sensor_*} parts are not this catalog.
 */
public enum SensorKind {
    THERMOMETER(
            31000,
            "thermometer",
            "thermometer",
            "Thermometer Sensor",
            "温度计传感器",
            new String[] {"WRW", "RXR", "WPW"},
            "thermometer",
            null,
            false,
            Behavior.LIVE,
            1L),
    GIBBLOMETER(
            31001,
            "gibblometer",
            "gibblometer",
            "Gibbl-O-Meter Sensor",
            "吉伯计传感器",
            new String[] {"WPW", "BXB", "WPW"},
            "sio2_gem",
            null,
            false,
            Behavior.LIVE,
            1L),
    LUMINOMETER(
            31002,
            "luminometer",
            "luminometer",
            "Luminometer Sensor",
            "光度计传感器",
            new String[] {"WGW", "YXY", "WPW"},
            "silicon_plate",
            "copper_fine_wire",
            false,
            Behavior.LIVE,
            1L),
    CHRONOMETER(
            31003,
            "chronometer",
            "chronometer",
            "Chronometer Sensor",
            "计时计传感器",
            new String[] {"WGW", "GXG", "WPW"},
            "clock",
            null,
            false,
            Behavior.LIVE,
            1L),
    ITEMOMETER(
            31004,
            "itemometer",
            "itemometer",
            "Item-O-Meter Sensor",
            "物品计传感器",
            new String[] {"WYW", "BXB", "WPW"},
            "gold_pressure_plate",
            "chest",
            false,
            Behavior.LIVE,
            1L),
    STACKOMETER(
            31005,
            "stackometer",
            "stackometer",
            "Stack-O-Meter Sensor",
            "组数计传感器",
            new String[] {"WYW", "BXB", "WPW"},
            "iron_pressure_plate",
            "chest",
            false,
            Behavior.LIVE,
            1L),
    FLUIDOMETER(
            31006,
            "fluidometer",
            "fluidometer",
            "Fluid-O-Meter Sensor",
            "流体计传感器",
            new String[] {"WYW", "BXB", "WPW"},
            "stone_pressure_plate",
            "bucket",
            false,
            Behavior.LIVE,
            1L),
    BUCKETOMETER(
            31007,
            "bucketometer",
            "bucketometer",
            "Bucket-O-Meter Sensor",
            "桶计传感器",
            new String[] {"WYW", "BXB", "WPW"},
            "gold_pressure_plate",
            "bucket",
            false,
            Behavior.LIVE,
            1L),
    LIGHT_WEIGHTOMETER(
            31010,
            "light_weightometer",
            "lightweightometer",
            "Light Weight-O-Meter Sensor",
            "轻量计传感器",
            new String[] {"WPW", "BXB", "WPW"},
            "wood_pressure_plate",
            null,
            false,
            Behavior.LIVE,
            1L),
    MEDIUM_WEIGHTOMETER(
            31011,
            "medium_weightometer",
            "mediumweightometer",
            "Medium Weight-O-Meter Sensor",
            "中量计传感器",
            new String[] {"WPW", "BXB", "WPW"},
            "stone_pressure_plate",
            null,
            false,
            Behavior.LIVE,
            1L),
    HEAVY_WEIGHTOMETER(
            31012,
            "heavy_weightometer",
            "heavyweightometer",
            "Heavy Weight-O-Meter Sensor",
            "重量计传感器",
            new String[] {"WPW", "BXB", "WPW"},
            "gold_pressure_plate",
            null,
            false,
            Behavior.LIVE,
            1L),
    SUPER_HEAVY_WEIGHTOMETER(
            31013,
            "super_heavy_weightometer",
            "superheavyweightometer",
            "Super Heavy Weight-O-Meter Sensor",
            "超重量计传感器",
            new String[] {"WPW", "BXB", "WPW"},
            "iron_pressure_plate",
            null,
            false,
            Behavior.LIVE,
            1L),
    ELECTROMETER(
            31015,
            "electrometer",
            "electrometer",
            "Electrometer Sensor",
            "电量计传感器",
            new String[] {"WGW", "YXY", "WPW"},
            "electro_meter",
            "copper_wire",
            false,
            Behavior.LIVE,
            1L),
    TPS_METER(
            31016,
            "tps_meter",
            "tpsmeter",
            "TPS Sensor",
            "TPS 传感器",
            new String[] {"WGW", "XXX", "WPW"},
            "clock",
            null,
            false,
            Behavior.LIVE,
            20L),
    PLAYER_COUNTER(
            31017,
            "player_counter",
            "playercounter",
            "Player Counter Sensor",
            "玩家计数传感器",
            new String[] {"WGW", "CXC", "WPW"},
            "stone_pressure_plate",
            null,
            false,
            Behavior.LIVE,
            1L),
    PROGRESS_METER(
            31018,
            "progress_meter",
            "progressmeter",
            "Progress Sensor",
            "进度传感器",
            new String[] {"WGW", "CXC", "WPW"},
            "brass_small_gear",
            null,
            false,
            Behavior.LIVE,
            1L),
    TACHOMETER(
            31019,
            "tachometer",
            "tachometer",
            "Tachometer Sensor",
            "转速计传感器",
            new String[] {"WGW", "YXY", "WPW"},
            "tacho_meter",
            "brass_gear",
            false,
            Behavior.LIVE,
            1L),
    GEIGER_COUNTER(
            31020,
            "geiger_counter",
            "geigercounter",
            "Geiger Counter Sensor",
            "盖革计数传感器",
            new String[] {"WGW", "YXY", "WPW"},
            "geiger_counter",
            "lead_double_plate",
            false,
            Behavior.LIVE,
            1L),
    LASEROMETER(
            31021,
            "laserometer",
            "laserometer",
            "Laser-O-Meter Sensor",
            "激光计传感器",
            new String[] {"WGW", "YXY", "WPW"},
            "compact_sensor_lv",
            "copper_fine_wire",
            false,
            Behavior.LIVE,
            1L),
    KILO_BUCKETOMETER(
            31022,
            "kilo_bucketometer",
            "kilobucketometer",
            "Kilo-Bucket-O-Meter Sensor",
            "千桶计传感器",
            new String[] {"WYW", "BXB", "WPW"},
            "iron_pressure_plate",
            "bucket",
            false,
            Behavior.LIVE,
            1L),
    KILO_GIBBLOMETER(
            31023,
            "kilo_gibblometer",
            "kilogibblometer",
            "Kilo-Gibbl-O-Meter Sensor",
            "千吉伯计传感器",
            new String[] {"WPW", "BXB", "WPW"},
            "diamond_gem",
            null,
            false,
            Behavior.LIVE,
            1L);

    public static final int EXPECTED_SIZE = 21;

    public enum Behavior {
        LIVE,
        FAIL_CLOSED
    }

    private static final Map<ResourceLocation, SensorKind> BY_ID = new LinkedHashMap<>();
    private static final Map<Integer, SensorKind> BY_SOURCE = new LinkedHashMap<>();

    static {
        for (SensorKind kind : values()) {
            if (BY_ID.put(kind.id, kind) != null) {
                throw new IllegalStateException("Duplicate sensor id " + kind.id);
            }
            if (BY_SOURCE.put(kind.sourceId, kind) != null) {
                throw new IllegalStateException(
                        "Duplicate sensor source id " + kind.sourceId);
            }
        }
        if (BY_ID.size() != EXPECTED_SIZE) {
            throw new IllegalStateException(
                    "Sensor catalog drifted from 21 GT6 identities");
        }
    }

    private final int sourceId;
    private final ResourceLocation id;
    private final String textureFolder;
    private final String langEn;
    private final String langZh;
    private final String[] grid;
    private final String specialX;
    private final String specialY;
    private final boolean d0Blocked;
    private final Behavior behavior;
    private final long tickRate;

    SensorKind(
            int sourceId,
            String path,
            String textureFolder,
            String langEn,
            String langZh,
            String[] grid,
            String specialX,
            String specialY,
            boolean d0Blocked,
            Behavior behavior,
            long tickRate) {
        this.sourceId = sourceId;
        this.id = ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, path);
        this.textureFolder = textureFolder;
        this.langEn = langEn;
        this.langZh = langZh;
        this.grid = grid;
        this.specialX = specialX;
        this.specialY = specialY;
        this.d0Blocked = d0Blocked;
        this.behavior = behavior;
        this.tickRate = tickRate;
    }

    public int sourceId() {
        return sourceId;
    }

    public ResourceLocation id() {
        return id;
    }

    public String path() {
        return id.getPath();
    }

    public String textureFolder() {
        return textureFolder;
    }

    public String langEn() {
        return langEn;
    }

    public String langZh() {
        return langZh;
    }

    public String[] grid() {
        return grid;
    }

    public String specialX() {
        return specialX;
    }

    public String specialY() {
        return specialY;
    }

    public boolean d0Blocked() {
        return d0Blocked;
    }

    public Behavior behavior() {
        return behavior;
    }

    public boolean failClosed() {
        return behavior == Behavior.FAIL_CLOSED;
    }

    public long tickRate() {
        return tickRate;
    }

    public static List<SensorKind> all() {
        return Arrays.asList(values());
    }

    public static SensorKind require(ResourceLocation id) {
        SensorKind kind = BY_ID.get(id);
        if (kind == null) {
            throw new IllegalArgumentException("Unknown sensor " + id);
        }
        return kind;
    }
}
