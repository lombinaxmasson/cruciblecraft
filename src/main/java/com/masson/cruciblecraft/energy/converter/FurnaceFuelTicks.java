package com.masson.cruciblecraft.energy.converter;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.google.gson.annotations.SerializedName;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;

import net.minecraft.world.item.ItemStack;

/**
 * GT6 {@code mFurnaceBurnTime} per unit, scaled onto BURNABLE prefixes.
 *
 * <p>Vanilla furnace ticks stay tick-based. Burning boxes convert with
 * {@code EU_PER_FURNACE_TICK} in {@link FurnaceFuelAdapter}.
 */
final class FurnaceFuelTicks {
    static final int EU_PER_FURNACE_TICK = 25;
    static final int MATERIAL_UNIT = 144;
    static final int MAX_FURNACE_TICKS = 32_000;
    private static final String RESOURCE =
            "/data/cruciblecraft/furnace_burn_ticks.json";
    private static final FurnaceFuelTicks BUNDLED = loadBundled();

    private final Map<String, Integer> ticksPerUnit;
    private final Set<String> burnableKeys;

    private FurnaceFuelTicks(
            Map<String, Integer> ticksPerUnit, Set<String> burnableKeys) {
        this.ticksPerUnit = Map.copyOf(ticksPerUnit);
        this.burnableKeys = Set.copyOf(burnableKeys);
    }

    static int ticks(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        return MaterialUnits.resolve(stack)
                .map(entry -> ticks(entry.materialId(), entry.form()))
                .orElse(0);
    }

    static int ticks(String materialId, MaterialPrefix form) {
        if (materialId == null || materialId.isBlank() || form == null) {
            return 0;
        }
        if (!MaterialPrefixCatalog.isBootstrapped()) {
            return 0;
        }
        int perUnit = BUNDLED.ticksPerUnit.getOrDefault(materialId, 0);
        if (perUnit <= 0) {
            return 0;
        }
        String serialized = form.serializedName();
        if ("raw_ore".equals(serialized)) {
            return bind(perUnit);
        }
        if ("block_raw".equals(serialized)) {
            return bind((long) perUnit * 10L);
        }
        if (!isBurnable(form)) {
            return 0;
        }
        long scaled = (long) perUnit * form.units() / MATERIAL_UNIT;
        if (isWood(materialId)) {
            if ("rod".equals(serialized)) {
                scaled = Math.max(100L, scaled);
            } else if ("long_rod".equals(serialized)) {
                scaled = Math.max(200L, scaled);
            } else if ("storage_plate".equals(serialized)) {
                scaled = Math.max(2_700L, scaled);
            }
        }
        return bind(scaled);
    }

    static int ticksPerUnit(String materialId) {
        return BUNDLED.ticksPerUnit.getOrDefault(materialId, 0);
    }

    private static boolean isBurnable(MaterialPrefix form) {
        Set<String> keys = new HashSet<>();
        addKey(keys, form.serializedName());
        for (String alias : MaterialPrefixCatalog.definition(form).aliases()) {
            addKey(keys, alias);
        }
        for (String key : keys) {
            if (BUNDLED.burnableKeys.contains(key)) {
                return true;
            }
        }
        return false;
    }

    private static void addKey(Set<String> keys, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        keys.add(lower);
        keys.add(lower.replace("_", ""));
    }

    private static boolean isWood(String materialId) {
        if (!MaterialCatalog.isBootstrapped()) {
            return false;
        }
        return MaterialCatalog.find(materialId)
                .map(material -> material.hasMaterialTag("PROPERTIES.WOOD"))
                .orElse(false);
    }

    private static int bind(long ticks) {
        if (ticks <= 0L) {
            return 0;
        }
        return (int) Math.min(MAX_FURNACE_TICKS, ticks);
    }

    private static FurnaceFuelTicks loadBundled() {
        Document document = CatalogJson.readBundled(
                FurnaceFuelTicks.class, RESOURCE, Document.class);
        if (document.schemaVersion != 1
                || document.ticksPerUnit == null
                || document.ticksPerUnit.isEmpty()
                || document.gt6BurnablePrefixes == null
                || document.gt6BurnablePrefixes.isEmpty()) {
            throw new IllegalStateException("Invalid furnace burn-tick catalog");
        }
        LinkedHashMap<String, Integer> ticks = new LinkedHashMap<>();
        document.ticksPerUnit.forEach((materialId, value) -> {
            if (materialId == null || materialId.isBlank()
                    || value == null || value <= 0) {
                throw new IllegalStateException(
                        "Invalid furnace burn ticks for " + materialId);
            }
            ticks.put(materialId, value);
        });
        Set<String> burnable = new HashSet<>();
        addKey(burnable, "block");
        for (String prefix : document.gt6BurnablePrefixes) {
            addKey(burnable, prefix);
        }
        return new FurnaceFuelTicks(ticks, burnable);
    }

    private static final class Document {
        @SerializedName("schema_version")
        int schemaVersion;
        @SerializedName("ticks_per_unit")
        Map<String, Integer> ticksPerUnit;
        @SerializedName("gt6_burnable_prefixes")
        List<String> gt6BurnablePrefixes;
    }
}
