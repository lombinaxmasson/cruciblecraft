package com.masson.cruciblecraft.logistics.pipe.cover;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.resources.ResourceLocation;

/**
 * Ten GT6 VN compact-electric cover families. Numeric policy is in the
 * datapack sidecar; this table is the Java companion for items and recipes.
 */
public final class CoverComponentTiers {
    public static final int TIER_COUNT = 10;
    public static final String[] VN_PATH = {
            "ulv", "lv", "mv", "hv", "ev", "iv", "luv", "zpm", "uv", "puv1"};
    public static final String[] VN_LABEL = {
            "ULV", "LV", "MV", "HV", "EV", "IV", "LuV", "ZPM", "UV", "PUV1"};

    public enum Family {
        CONVEYOR(
                "conveyor",
                "compact_electric_conveyor",
                "conveyor",
                CoverDefinition.Medium.ITEM),
        ROBOT_ARM(
                "robot_arm",
                "compact_electric_robot_arm",
                "robot_arm",
                CoverDefinition.Medium.ITEM),
        PUMP(
                "pump",
                "compact_electric_pump",
                "pump_adapter",
                CoverDefinition.Medium.FLUID);

        private final String definitionPrefix;
        private final String itemPrefix;
        private final String behaviorPath;
        private final CoverDefinition.Medium medium;

        Family(
                String definitionPrefix,
                String itemPrefix,
                String behaviorPath,
                CoverDefinition.Medium medium) {
            this.definitionPrefix = definitionPrefix;
            this.itemPrefix = itemPrefix;
            this.behaviorPath = behaviorPath;
            this.medium = medium;
        }

        public String definitionPath(int tier) {
            return definitionPrefix + "_" + VN_PATH[tier];
        }

        public String itemPath(int tier) {
            return itemPrefix + "_" + VN_PATH[tier];
        }

        public ResourceLocation definitionId(int tier) {
            return ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", definitionPath(tier));
        }

        public ResourceLocation behaviorId() {
            return ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", behaviorPath);
        }

        public CoverDefinition.Medium medium() {
            return medium;
        }

        public String englishName(int tier) {
            String kind = switch (this) {
                case CONVEYOR -> "Compact Electric Conveyor";
                case ROBOT_ARM -> "Compact Electric Robot Arm";
                case PUMP -> "Compact Electric Pump";
            };
            return kind + " (" + VN_LABEL[tier] + ")";
        }

        public String chineseName(int tier) {
            String kind = switch (this) {
                case CONVEYOR -> "紧凑电动传送带";
                case ROBOT_ARM -> "紧凑电动机械臂";
                case PUMP -> "紧凑电动泵";
            };
            return kind + "（" + VN_LABEL[tier] + "）";
        }
    }

    public record Entry(
            Family family,
            int tier,
            ResourceLocation definitionId,
            String itemPath,
            int rate,
            int interval) {}

    private static final List<Entry> ENTRIES = build();

    public static List<Entry> entries() {
        return ENTRIES;
    }

    public static Set<String> definitionIds() {
        return ENTRIES.stream()
                .map(entry -> entry.definitionId().toString())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static Optional<Entry> findByDefinition(ResourceLocation id) {
        if (id == null) {
            return Optional.empty();
        }
        return ENTRIES.stream()
                .filter(entry -> entry.definitionId().equals(id))
                .findFirst();
    }

    public static Optional<Entry> findByItemPath(String itemPath) {
        if (itemPath == null || itemPath.isBlank()) {
            return Optional.empty();
        }
        return ENTRIES.stream()
                .filter(entry -> entry.itemPath().equals(itemPath))
                .findFirst();
    }

    public static int conveyorInterval(int tier) {
        return Math.max(1, 512 >> tier);
    }

    public static int pumpRate(int tier) {
        return 250 << (2 * tier);
    }

    private static List<Entry> build() {
        ArrayList<Entry> rows = new ArrayList<>();
        for (Family family : Family.values()) {
            for (int tier = 0; tier < TIER_COUNT; tier++) {
                int rate;
                int interval;
                if (family == Family.PUMP) {
                    rate = pumpRate(tier);
                    interval = 20;
                } else {
                    rate = CoverDefinition.MAX_ITEM_RATE;
                    interval = conveyorInterval(tier);
                }
                rows.add(new Entry(
                        family,
                        tier,
                        family.definitionId(tier),
                        family.itemPath(tier),
                        rate,
                        interval));
            }
        }
        return List.copyOf(rows);
    }

    private CoverComponentTiers() {}
}
