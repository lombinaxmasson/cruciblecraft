package com.masson.cruciblecraft.logistics.machinecover;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.masson.cruciblecraft.content.item.PipeCoverItem;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverDefinition;
import com.masson.cruciblecraft.logistics.pipe.cover.DecorativeCovers;
import com.masson.cruciblecraft.logistics.pipe.cover.PlateCovers;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredItem;

/** GT6 MultiItemTechnological 1000-1030 remainder covers. */
public final class MachineCoverKinds {
    public static final int DEFINITION_COUNT = 31;
    public static final int ITEM_COUNT = 28;

    public record ItemCover(
            String definitionPath,
            String itemPath,
            int gt6Id,
            String behaviorPath,
            String english,
            String chinese,
            CoverDefinition.Medium medium) {
        public ResourceLocation definitionId() {
            return id(definitionPath);
        }

        public ResourceLocation behaviorId() {
            return id(behaviorPath);
        }
    }

    public record ExtraDefinition(
            String definitionPath,
            String behaviorPath,
            CoverDefinition.Medium medium) {
        public ResourceLocation definitionId() {
            return id(definitionPath);
        }
    }

    public static final List<ItemCover> ITEMS = List.of(
            item("cover_blank", 1000, "cover_blank",
                    "Blank Cover", "空白覆盖板",
                    CoverDefinition.Medium.BOTH),
            item("cover_crafting", 1001, "cover_crafting",
                    "Crafting Table Cover", "工作台覆盖板",
                    CoverDefinition.Medium.BOTH),
            item("controller_display", 1002, "controller_display",
                    "Machine Status Display Cover", "状态显示覆盖板",
                    CoverDefinition.Medium.BOTH),
            item("controller_auto", 1003, "controller_auto",
                    "Automatic Machine Switch", "自动开关",
                    CoverDefinition.Medium.BOTH),
            item("display_energy", 1004, "display_energy",
                    "Energy Display Cover", "能量显示面板",
                    CoverDefinition.Medium.BOTH),
            item("controller_redstone", 1005, "controller_redstone",
                    "Redstone Machine Switch", "红石机器开关",
                    CoverDefinition.Medium.BOTH),
            item("controller_auto_redstone", 1006, "controller_auto_redstone",
                    "Auto Redstone Machine Switch", "自动式红石机器开关",
                    CoverDefinition.Medium.BOTH),
            item("selector_redstone", 1007, "selector_redstone",
                    "Redstone Selector", "红石选择面板",
                    CoverDefinition.Medium.BOTH),
            item("controller_auto_timer_1m", 1009, "controller_auto_timer",
                    "Auto Reboot Switch (1 min)", "自动重启开关(1分钟)",
                    CoverDefinition.Medium.BOTH),
            item("controller_auto_timer_5m", 1010, "controller_auto_timer",
                    "Auto Reboot Switch (5 mins)", "自动重启开关(5分钟)",
                    CoverDefinition.Medium.BOTH),
            item("controller_auto_timer_10m", 1011, "controller_auto_timer",
                    "Auto Reboot Switch (10 mins)", "自动重启开关(10分钟)",
                    CoverDefinition.Medium.BOTH),
            item("controller_auto_timer_20m", 1012, "controller_auto_timer",
                    "Auto Reboot Switch (20 mins)", "自动重启开关(20分钟)",
                    CoverDefinition.Medium.BOTH),
            item("controller_auto_timer_30m", 1013, "controller_auto_timer",
                    "Auto Reboot Switch (30 mins)", "自动重启开关(30分钟)",
                    CoverDefinition.Medium.BOTH),
            item("scale_energy", 1014, "scale_energy",
                    "Energy Sensor", "能量传感器",
                    CoverDefinition.Medium.BOTH),
            item("detector_running_possible", 1015, "detector_running",
                    "Activity Detector (Possible)", "活动传感器(可运行)",
                    CoverDefinition.Medium.BOTH),
            item("detector_running_passively", 1016, "detector_running",
                    "Activity Detector (Running)", "活动传感器(待机)",
                    CoverDefinition.Medium.BOTH),
            item("detector_running_actively", 1017, "detector_running",
                    "Activity Detector (Processing)", "活动传感器(加工中)",
                    CoverDefinition.Medium.BOTH),
            item("scale_progress", 1018, "scale_progress",
                    "Progress Sensor", "进度传感器",
                    CoverDefinition.Medium.BOTH),
            item("detector_running_successfully", 1019, "detector_running",
                    "Activity Detector (Success)", "活动传感器(成功)",
                    CoverDefinition.Medium.BOTH),
            item("cover_drain", 1020, "cover_drain",
                    "Drain", "排液口",
                    CoverDefinition.Medium.FLUID),
            item("redstone_emitter", 1021, "redstone_emitter",
                    "Redstone Emitter", "红石信号发射器",
                    CoverDefinition.Medium.BOTH),
            item("vent", 1022, "vent",
                    "Air Vent", "通风口",
                    CoverDefinition.Medium.FLUID),
            item("filter_fluid", 1024, "filter_fluid",
                    "Fluid Filter", "流体过滤覆盖板",
                    CoverDefinition.Medium.FLUID),
            item("controller_covers", 1025, "controller_covers",
                    "Cover Controller", "控制器覆盖板",
                    CoverDefinition.Medium.BOTH),
            item("selector_button_panel", 1027, "selector_button_panel",
                    "Button Panel Selector", "按钮覆盖板选择器",
                    CoverDefinition.Medium.BOTH),
            item("cover_warning", 1028, "cover_warning",
                    "Warning Cover", "警告标志覆盖板",
                    CoverDefinition.Medium.BOTH),
            item("redstone_conductor_in", 1029, "redstone_conductor_in",
                    "Redstone Conductor Cover (Accept)", "红石传导覆盖板(输入)",
                    CoverDefinition.Medium.BOTH),
            item("redstone_conductor_out", 1030, "redstone_conductor_out",
                    "Redstone Conductor Cover (Emit)", "红石传导覆盖板(输出)",
                    CoverDefinition.Medium.BOTH));

    public static final List<ExtraDefinition> EXTRA = List.of(
            new ExtraDefinition(
                    "selector_tag", "selector_tag", CoverDefinition.Medium.BOTH),
            new ExtraDefinition(
                    "redstone_torch", "redstone_torch", CoverDefinition.Medium.BOTH),
            new ExtraDefinition(
                    "redstone_repeater", "redstone_repeater",
                    CoverDefinition.Medium.BOTH));

    public static final Set<String> BEHAVIOR_PATHS = Set.of(
            "cover_blank",
            "cover_crafting",
            "cover_drain",
            "cover_warning",
            "controller_auto",
            "controller_auto_redstone",
            "controller_auto_timer",
            "controller_covers",
            "controller_display",
            "controller_redstone",
            "detector_running",
            "display_energy",
            "filter_fluid",
            "redstone_conductor_in",
            "redstone_conductor_out",
            "redstone_emitter",
            "redstone_repeater",
            "redstone_torch",
            "scale_energy",
            "scale_progress",
            "selector_button_panel",
            "selector_redstone",
            "selector_tag",
            "vent");

    private static final Map<Integer, Integer> TIMER_PERIODS = Map.of(
            1009, 1200,
            1010, 6000,
            1011, 12000,
            1012, 24000,
            1013, 36000);
    private static final Map<String, ItemCover> BY_DEFINITION;
    private static final Map<String, ItemCover> BY_ITEM;

    static {
        if (ITEMS.size() != ITEM_COUNT) {
            throw new IllegalStateException("Machine cover item table drifted");
        }
        LinkedHashMap<String, ItemCover> byDefinition = new LinkedHashMap<>();
        LinkedHashMap<String, ItemCover> byItem = new LinkedHashMap<>();
        for (ItemCover cover : ITEMS) {
            if (byDefinition.put(cover.definitionPath(), cover) != null
                    || byItem.put(cover.itemPath(), cover) != null) {
                throw new IllegalStateException(
                        "Duplicate machine cover " + cover.definitionPath());
            }
        }
        BY_DEFINITION = Map.copyOf(byDefinition);
        BY_ITEM = Map.copyOf(byItem);
        if (EXTRA.size() + ITEMS.size() != DEFINITION_COUNT) {
            throw new IllegalStateException("Machine cover definition count drifted");
        }
    }

    private MachineCoverKinds() {}

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }

    public static Optional<ItemCover> byDefinition(ResourceLocation definitionId) {
        if (definitionId == null
                || !"cruciblecraft".equals(definitionId.getNamespace())) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_DEFINITION.get(definitionId.getPath()));
    }

    public static Optional<ItemCover> byItemPath(String itemPath) {
        return Optional.ofNullable(BY_ITEM.get(itemPath));
    }

    public static String itemPath(String definitionPath) {
        ItemCover cover = BY_DEFINITION.get(definitionPath);
        if (cover == null) {
            throw new IllegalStateException(
                    "Unknown machine cover definition " + definitionPath);
        }
        return cover.itemPath();
    }

    public static DeferredItem<PipeCoverItem> itemFor(
            ResourceLocation definitionId) {
        return byDefinition(definitionId)
                .map(cover -> ModItems.machineCover(cover.itemPath()))
                .orElse(null);
    }

    public static int timerPeriod(ResourceLocation definitionId) {
        ItemCover cover = byDefinition(definitionId).orElse(null);
        if (cover == null) {
            return 0;
        }
        return TIMER_PERIODS.getOrDefault(cover.gt6Id(), 0);
    }

    public static boolean isRemainder(ResourceLocation definitionId) {
        if (definitionId == null) {
            return false;
        }
        String path = definitionId.getPath();
        return BY_DEFINITION.containsKey(path) || "selector_tag".equals(path);
    }

    public static boolean isCoverController(ResourceLocation definitionId) {
        return definitionId != null
                && "controller_covers".equals(definitionId.getPath());
    }

    public static boolean isBlank(ResourceLocation definitionId) {
        return definitionId != null && "cover_blank".equals(definitionId.getPath());
    }

    public static boolean isTorch(ResourceLocation definitionId) {
        return definitionId != null
                && "redstone_torch".equals(definitionId.getPath());
    }

    public static boolean isWireOnlyCover(ResourceLocation definitionId) {
        if (definitionId == null) {
            return false;
        }
        String path = definitionId.getPath();
        return "redstone_torch".equals(path) || "redstone_repeater".equals(path);
    }

    /**
     * GT6 redstone connectors are {@code ITileEntitySwitchableMode} and
     * {@code ITileEntityProgress}. Torch/repeater stay wire-only; selectors,
     * emitter, conductor, progress scale, blank, decorative plates, crafting,
     * and warning attach here too. Controllers, detectors, vent, drain, and
     * energy covers still need a machine or fluid host.
     */
    public static boolean attachesToRedstoneWire(ResourceLocation definitionId) {
        if (definitionId == null) {
            return false;
        }
        if (isBlank(definitionId)
                || isWireOnlyCover(definitionId)
                || PlateCovers.isPlate(definitionId)
                || DecorativeCovers.isDecorative(definitionId)) {
            return true;
        }
        String path = definitionId.getPath();
        return "selector_redstone".equals(path)
                || "selector_tag".equals(path)
                || "selector_button_panel".equals(path)
                || "redstone_emitter".equals(path)
                || "redstone_conductor_in".equals(path)
                || "redstone_conductor_out".equals(path)
                || "scale_progress".equals(path)
                || "cover_crafting".equals(path)
                || "cover_warning".equals(path);
    }

    public static boolean isBlockedWireHost(ResourceLocation definitionId) {
        return isWireOnlyCover(definitionId);
    }

    public static boolean requiresMachineHost(ResourceLocation definitionId) {
        if (definitionId == null) {
            return false;
        }
        if (isBlank(definitionId) || isWireOnlyCover(definitionId)) {
            return false;
        }
        String path = definitionId.getPath();
        if ("cover_crafting".equals(path)
                || "cover_warning".equals(path)
                || "cover_drain".equals(path)
                || "filter_fluid".equals(path)) {
            return false;
        }
        return BY_DEFINITION.containsKey(path) || "selector_tag".equals(path);
    }

    public static boolean requiresEnergy(ResourceLocation definitionId) {
        if (definitionId == null) {
            return false;
        }
        String path = definitionId.getPath();
        return "display_energy".equals(path) || "scale_energy".equals(path);
    }

    public static boolean requiresFluids(ResourceLocation definitionId) {
        if (definitionId == null) {
            return false;
        }
        String path = definitionId.getPath();
        return "vent".equals(path) || "cover_drain".equals(path);
    }

    private static ItemCover item(
            String definitionPath,
            int gt6Id,
            String behaviorPath,
            String english,
            String chinese,
            CoverDefinition.Medium medium) {
        return new ItemCover(
                definitionPath,
                definitionPath + "_cover",
                gt6Id,
                behaviorPath,
                english,
                chinese,
                medium);
    }
}
