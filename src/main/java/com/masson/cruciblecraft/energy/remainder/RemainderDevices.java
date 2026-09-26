package com.masson.cruciblecraft.energy.remainder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.energy.cable.GT6VoltageTiers;
import com.masson.cruciblecraft.energy.remainder.RemainderDevice.GridPart;
import com.masson.cruciblecraft.energy.remainder.RemainderDevice.Kind;
import com.masson.cruciblecraft.energy.remainder.RemainderDevice.ObtainKind;
import com.masson.cruciblecraft.energy.remainder.RemainderDevice.ObtainPlan;

import net.minecraft.resources.ResourceLocation;

/**
 * GT6 remainder energy hosts from {@code Loader_MultiTileEntities}.
 * Crystal chargers set only {@code NBT_ENERGY_EMITTED=LU}, so they accept
 * and emit LU. Large battery boxes center on transformer {@code 10040+tier}.
 */
public final class RemainderDevices {
    public static final int HOST_COUNT = 43;
    public static final int NEW_BLOCK_COUNT = 41;
    private static final String[] VOLTAGE = {
            "ULV", "LV", "MV", "HV", "EV", "IV", "LuV", "ZPM", "UV", "PUV1"
    };
    private static final String[] ELECTRIC = {
            "tin_alloy", "steel_galvanized", "aluminium", "stainless_steel",
            "chromium", "titanium", "iridium", "osmium_elemental",
            "trinitanium", "trinaquadalloy"
    };
    private static final String[] CONDUCTOR = {
            "lead", "tin", "copper", "gold", "aluminium", "platinum",
            "graphene", "graphene", "graphene", "graphene"
    };
    private static final String[] CIRCUITS = {
            null,
            "circuit_basic",
            "circuit_good",
            "circuit_advanced",
            "circuit_elite",
            "circuit_master",
            "circuit_ultimate",
            "circuit_quantum",
            "circuit_quantum",
            "circuit_quantum"
    };
    private static final String[] FIELD_GENERATORS = {
            "compact_force_field_emitter_ulv",
            "compact_force_field_emitter_lv",
            "compact_force_field_emitter_mv",
            "compact_force_field_emitter_hv",
            "compact_force_field_emitter_ev",
            "compact_force_field_emitter_iv",
            "compact_force_field_emitter_luv",
            "compact_force_field_emitter_zpm",
            "compact_force_field_emitter_uv",
            "compact_force_field_emitter_puv1"
    };

    private static final List<RemainderDevice> DEVICES = build();
    private static final Map<ResourceLocation, RemainderDevice> BY_ID = index();
    private static final Map<Integer, RemainderDevice> BY_SOURCE = sourceIndex();

    private RemainderDevices() {}

    public static List<RemainderDevice> devices() {
        return DEVICES;
    }

    public static RemainderDevice require(ResourceLocation id) {
        RemainderDevice device = BY_ID.get(id);
        if (device == null) {
            throw new IllegalArgumentException("Unknown remainder device " + id);
        }
        return device;
    }

    public static RemainderDevice requireMeta(int sourceId) {
        RemainderDevice device = BY_SOURCE.get(sourceId);
        if (device == null) {
            throw new IllegalArgumentException(
                    "Unknown remainder source " + sourceId);
        }
        return device;
    }

    public static List<RemainderDevice> placeable() {
        return DEVICES.stream().filter(device -> !device.legacy()).toList();
    }

    public static List<RemainderDevice> ofKind(Kind kind) {
        return DEVICES.stream().filter(device -> device.kind() == kind).toList();
    }

    public static ObtainPlan obtain(RemainderDevice device) {
        if (device.legacy()) {
            return new ObtainPlan(
                    ObtainKind.ALREADY_LIVE,
                    "LuV and ZPM small boxes already have source-exact recipes.",
                    List.of("WCW", "WCW", "XMX"),
                    List.of());
        }
        if (device.kind() == Kind.MAGIC_ABSORBER) {
            return new ObtainPlan(
                    ObtainKind.BLOCKED,
                    "IL.Circuit_Magic is unmapped. Twilight Forest trophies "
                            + "are absent; dragon egg offers QU and vanilla "
                            + "skulls offer TU.",
                    List.of("GOG", "LBL", "CMC"),
                    List.of());
        }
        if (device.circuitIndex() == 0) {
            return new ObtainPlan(
                    ObtainKind.BLOCKED,
                    "OD_CIRCUITS[0] is the primitive circuit and is unmapped.",
                    pattern(device),
                    List.of());
        }
        if (device.kind() == Kind.BATTERY_BOX_LARGE && device.tier() == 9) {
            return new ObtainPlan(
                    ObtainKind.BLOCKED,
                    "Large battery box tier 9 centers on transformer 10049, "
                            + "which GT6 does not register.",
                    List.of("WCW", "WCW", "XMX"),
                    List.of());
        }
        return new ObtainPlan(
                ObtainKind.SHAPED,
                "source-exact",
                pattern(device),
                parts(device));
    }

    private static List<String> pattern(RemainderDevice device) {
        return switch (device.kind()) {
            case SOLAR -> List.of("SWS", "CMC", "SWS");
            case BATTERY_BOX, BATTERY_BOX_LARGE -> List.of("WCW", "WCW", "XMX");
            case CRYSTAL_CHARGER, CRYSTAL_CHARGER_LARGE ->
                    List.of("FCF", "FCF", "PMP");
            case MAGIC_ABSORBER -> List.of("GOG", "LBL", "CMC");
        };
    }

    private static List<GridPart> parts(RemainderDevice device) {
        return switch (device.kind()) {
            case SOLAR -> List.of(
                    new GridPart('S', material(
                            device.plateMaterial(), "plate_gem")),
                    new GridPart('W', material(device.cableMaterial(), "cable")),
                    new GridPart('C', item(CIRCUITS[device.circuitIndex()])),
                    new GridPart('M', material(
                            device.material(), "machine_casing")));
            case BATTERY_BOX, BATTERY_BOX_LARGE -> batteryParts(device);
            case CRYSTAL_CHARGER, CRYSTAL_CHARGER_LARGE -> crystalParts(device);
            case MAGIC_ABSORBER -> List.of();
        };
    }

    private static List<GridPart> batteryParts(RemainderDevice device) {
        boolean large = device.kind() == Kind.BATTERY_BOX_LARGE;
        boolean insulated = device.tier() <= 5;
        String cablePrefix = large
                ? (insulated ? "quadruple_cable" : "quadruple_wire")
                : (insulated ? "cable" : "wire");
        String wirePrefix = large ? "quadruple_wire" : "wire";
        String conductor = CONDUCTOR[device.tier()];
        String center = large
                ? "transformer:" + (10040 + device.tier())
                : material(device.material(), "machine_casing");
        return List.of(
                new GridPart('C', material(conductor, cablePrefix)),
                new GridPart('W', material(conductor, wirePrefix)),
                new GridPart('X', item(CIRCUITS[device.circuitIndex()])),
                new GridPart('M', center));
    }

    private static List<GridPart> crystalParts(RemainderDevice device) {
        boolean large = device.kind() == Kind.CRYSTAL_CHARGER_LARGE;
        String center = large
                ? "remainder:" + device.material() + "_crystal_charger"
                : material(device.material(), "machine_casing");
        return List.of(
                new GridPart('F', item(FIELD_GENERATORS[device.tier()])),
                new GridPart('C', item(CIRCUITS[device.circuitIndex()])),
                new GridPart('P', item("processor_crystal_emerald")),
                new GridPart('M', center));
    }

    private static String material(String material, String prefix) {
        return "material:" + material + "/" + prefix;
    }

    private static String item(String path) {
        return "item:" + path;
    }

    private static List<RemainderDevice> build() {
        List<RemainderDevice> devices = new ArrayList<>();
        devices.add(solar(
                10050, "tin_alloy", 8L, 1, "silicon", "copper",
                "Solar Panel (Silicon)", "太阳能板（硅）"));
        devices.add(solar(
                10051, "aluminium", 16L, 6, "germanium", "annealed_copper",
                "Solar Panel (Germanium)", "太阳能板（锗）"));
        for (int tier = 0; tier < VOLTAGE.length; tier++) {
            devices.add(battery(Kind.BATTERY_BOX, 10080 + tier, tier, 4));
            devices.add(battery(
                    Kind.BATTERY_BOX_LARGE, 10090 + tier, tier, 16));
            devices.add(charger(Kind.CRYSTAL_CHARGER, 10130 + tier, tier, 4));
            devices.add(charger(
                    Kind.CRYSTAL_CHARGER_LARGE, 10140 + tier, tier, 16));
        }
        devices.add(new RemainderDevice(
                id("palladium_magic_field_absorber"),
                Kind.MAGIC_ABSORBER,
                10180,
                -1,
                "palladium",
                EnergyType.QUANTUM,
                0L,
                64L,
                0,
                false,
                -1,
                null,
                null,
                "Magic Field Absorber",
                "魔法场吸收器"));
        if (devices.size() != HOST_COUNT) {
            throw new IllegalStateException(
                    "Remainder host count drifted: " + devices.size());
        }
        long fresh = devices.stream().filter(device -> !device.legacy()).count();
        if (fresh != NEW_BLOCK_COUNT) {
            throw new IllegalStateException(
                    "Remainder block count drifted: " + fresh);
        }
        return List.copyOf(devices);
    }

    private static RemainderDevice solar(
            int sourceId,
            String material,
            long output,
            int circuit,
            String plate,
            String cable,
            String english,
            String chinese) {
        return new RemainderDevice(
                id(material + "_solar_panel"),
                Kind.SOLAR,
                sourceId,
                sourceId == 10050 ? 0 : 2,
                material,
                EnergyType.ELECTRIC,
                0L,
                output,
                0,
                false,
                circuit,
                plate,
                cable,
                english,
                chinese);
    }

    private static RemainderDevice battery(
            Kind kind, int sourceId, int tier, int slots) {
        boolean legacy = kind == Kind.BATTERY_BOX && (tier == 6 || tier == 7);
        String path = legacy
                ? (tier == 6 ? "boxwood/battery_luv" : "boxwood/battery_zpm")
                : ELECTRIC[tier] + "_" + kind.texture();
        String voltage = VOLTAGE[tier];
        String english = kind == Kind.BATTERY_BOX
                ? "Battery Box (" + voltage + ")"
                : "Large Battery Box (" + voltage + ")";
        String chinese = kind == Kind.BATTERY_BOX
                ? "电池箱（" + voltage + "）"
                : "大型电池箱（" + voltage + "）";
        return electric(
                kind, path, sourceId, tier, slots, legacy, english, chinese);
    }

    private static RemainderDevice charger(
            Kind kind, int sourceId, int tier, int slots) {
        String path = ELECTRIC[tier] + "_" + kind.texture();
        String english = kind == Kind.CRYSTAL_CHARGER
                ? "Crystal Charger (T" + tier + ")"
                : "Large Crystal Charger (T" + tier + ")";
        String chinese = kind == Kind.CRYSTAL_CHARGER
                ? "晶体充能器（T" + tier + "）"
                : "大型晶体充能器（T" + tier + "）";
        long voltage = GT6VoltageTiers.VOLTAGES[tier];
        return new RemainderDevice(
                id(path),
                kind,
                sourceId,
                tier,
                ELECTRIC[tier],
                EnergyType.LU,
                voltage,
                voltage,
                slots,
                false,
                tier,
                null,
                null,
                english,
                chinese);
    }

    private static RemainderDevice electric(
            Kind kind,
            String path,
            int sourceId,
            int tier,
            int slots,
            boolean legacy,
            String english,
            String chinese) {
        long voltage = GT6VoltageTiers.VOLTAGES[tier];
        return new RemainderDevice(
                id(path),
                kind,
                sourceId,
                tier,
                ELECTRIC[tier],
                EnergyType.ELECTRIC,
                voltage,
                voltage,
                slots,
                legacy,
                tier,
                null,
                null,
                english,
                chinese);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, path);
    }

    private static Map<ResourceLocation, RemainderDevice> index() {
        Map<ResourceLocation, RemainderDevice> map = new LinkedHashMap<>();
        for (RemainderDevice device : DEVICES) {
            if (map.put(device.id(), device) != null) {
                throw new IllegalStateException("Duplicate remainder " + device.id());
            }
        }
        return Map.copyOf(map);
    }

    private static Map<Integer, RemainderDevice> sourceIndex() {
        Map<Integer, RemainderDevice> map = new LinkedHashMap<>();
        for (RemainderDevice device : DEVICES) {
            if (map.put(device.sourceId(), device) != null) {
                throw new IllegalStateException(
                        "Duplicate remainder source " + device.sourceId());
            }
        }
        RemainderDevice luv = map.get(10086);
        RemainderDevice zpm = map.get(10087);
        if (luv == null || !luv.legacy() || zpm == null || !zpm.legacy()) {
            throw new IllegalStateException("Legacy battery boxes drifted");
        }
        return Map.copyOf(map);
    }
}
