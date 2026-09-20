package com.masson.cruciblecraft.content.item.tool;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

/**
 * GT6 {@code Loader_Tools} 156–174 electric {@code addTool} identities and
 * {@code IToolStats} numbers from {@code gregtech/items/tools/electric}.
 */
public enum ElectricToolCatalog {
    MINING_DRILL_LV(
            ToolKind.MINING_DRILL_LV,
            Voltage.LV,
            Family.MINING_DRILL,
            RecipeGrid.SHARED_HEAD,
            "tool_head_drill",
            25,
            100,
            200,
            0,
            2.0F,
            3.0F,
            1.0F,
            "采矿钻头（LV）",
            "Mining Drill (LV)"),
    MINING_DRILL_MV(
            ToolKind.MINING_DRILL_MV,
            Voltage.MV,
            Family.MINING_DRILL,
            RecipeGrid.SHARED_HEAD,
            "tool_head_drill",
            100,
            3200,
            800,
            1,
            2.5F,
            6.0F,
            2.0F,
            "采矿钻头（MV）",
            "Mining Drill (MV)"),
    MINING_DRILL_HV(
            ToolKind.MINING_DRILL_HV,
            Voltage.HV,
            Family.MINING_DRILL,
            RecipeGrid.SHARED_HEAD,
            "tool_head_drill",
            400,
            12800,
            3200,
            1,
            3.0F,
            9.0F,
            4.0F,
            "采矿钻头（HV）",
            "Mining Drill (HV)"),
    CHAINSAW_LV(
            ToolKind.CHAINSAW_LV,
            Voltage.LV,
            Family.CHAINSAW,
            RecipeGrid.SHARED_HEAD,
            "tool_head_chainsaw",
            50,
            200,
            800,
            1,
            3.0F,
            2.0F,
            1.0F,
            "链锯（LV）",
            "Chainsaw (LV)"),
    CHAINSAW_MV(
            ToolKind.CHAINSAW_MV,
            Voltage.MV,
            Family.CHAINSAW,
            RecipeGrid.SHARED_HEAD,
            "tool_head_chainsaw",
            200,
            200,
            3200,
            1,
            3.5F,
            3.0F,
            2.0F,
            "链锯（MV）",
            "Chainsaw (MV)"),
    CHAINSAW_HV(
            ToolKind.CHAINSAW_HV,
            Voltage.HV,
            Family.CHAINSAW,
            RecipeGrid.SHARED_HEAD,
            "tool_head_chainsaw",
            800,
            200,
            12800,
            1,
            4.0F,
            4.0F,
            4.0F,
            "链锯（HV）",
            "Chainsaw (HV)"),
    WRENCH_LV(
            ToolKind.WRENCH_LV,
            Voltage.LV,
            Family.WRENCH,
            RecipeGrid.SHARED_HEAD,
            "tool_head_wrench",
            50,
            800,
            200,
            0,
            1.0F,
            2.0F,
            1.0F,
            "扳手（LV）",
            "Wrench (LV)"),
    WRENCH_MV(
            ToolKind.WRENCH_MV,
            Voltage.MV,
            Family.WRENCH,
            RecipeGrid.SHARED_HEAD,
            "tool_head_wrench",
            200,
            3200,
            800,
            1,
            1.5F,
            3.0F,
            2.0F,
            "扳手（MV）",
            "Wrench (MV)"),
    WRENCH_HV(
            ToolKind.WRENCH_HV,
            Voltage.HV,
            Family.WRENCH,
            RecipeGrid.SHARED_HEAD,
            "tool_head_wrench",
            800,
            12800,
            3200,
            1,
            2.0F,
            4.0F,
            4.0F,
            "扳手（HV）",
            "Wrench (HV)"),
    JACKHAMMER_HV(
            ToolKind.JACKHAMMER_HV,
            Voltage.HV,
            Family.JACKHAMMER,
            RecipeGrid.JACKHAMMER,
            "tool_head_drill",
            200,
            3200,
            800,
            1,
            3.0F,
            12.0F,
            2.0F,
            "风镐（HV）",
            "JackHammer (HV, Normal Mode)"),
    JACKHAMMER_HV_NO_ORES(
            ToolKind.JACKHAMMER_HV_NO_ORES,
            Voltage.HV,
            Family.JACKHAMMER_NO_ORES,
            RecipeGrid.NONE,
            "tool_head_drill",
            200,
            3200,
            800,
            1,
            3.0F,
            12.0F,
            2.0F,
            "风镐（HV，跳过矿石）",
            "JackHammer (HV, No Ores Mode)"),
    BUZZSAW_LV(
            ToolKind.BUZZSAW_LV,
            Voltage.LV,
            Family.BUZZSAW,
            RecipeGrid.BUZZSAW,
            "tool_head_buzzsaw",
            50,
            100,
            300,
            0,
            1.0F,
            1.0F,
            1.0F,
            "圆锯（LV）",
            "Buzzsaw (LV)"),
    SCREWDRIVER_LV(
            ToolKind.SCREWDRIVER_LV,
            Voltage.LV,
            Family.SCREWDRIVER,
            RecipeGrid.SCREWDRIVER,
            "tool_head_screwdriver",
            200,
            200,
            200,
            0,
            1.5F,
            1.0F,
            1.0F,
            "螺丝刀（LV）",
            "Screwdriver (LV)"),
    HAND_DRILL_LV(
            ToolKind.HAND_DRILL_LV,
            Voltage.LV,
            Family.HAND_DRILL,
            RecipeGrid.HAND_DRILL,
            "tool_head_drill",
            200,
            100,
            400,
            0,
            1.5F,
            1.0F,
            1.0F,
            "手钻（LV）",
            "Hand Drill (LV)"),
    MIXER_LV(
            ToolKind.MIXER_LV,
            Voltage.LV,
            Family.MIXER,
            RecipeGrid.MIXER,
            "tool_head_drill",
            200,
            100,
            400,
            0,
            1.5F,
            1.0F,
            1.0F,
            "手持搅拌机（LV）",
            "Hand Mixer (LV)"),
    MONKEY_WRENCH_LV(
            ToolKind.MONKEY_WRENCH_LV,
            Voltage.LV,
            Family.MONKEY_WRENCH,
            RecipeGrid.NONE,
            "tool_head_wrench",
            50,
            800,
            200,
            0,
            1.0F,
            2.0F,
            1.0F,
            "活动扳手（LV）",
            "Monkey Wrench (LV)"),
    MONKEY_WRENCH_MV(
            ToolKind.MONKEY_WRENCH_MV,
            Voltage.MV,
            Family.MONKEY_WRENCH,
            RecipeGrid.NONE,
            "tool_head_wrench",
            200,
            3200,
            800,
            1,
            1.5F,
            3.0F,
            2.0F,
            "活动扳手（MV）",
            "Monkey Wrench (MV)"),
    MONKEY_WRENCH_HV(
            ToolKind.MONKEY_WRENCH_HV,
            Voltage.HV,
            Family.MONKEY_WRENCH,
            RecipeGrid.NONE,
            "tool_head_wrench",
            800,
            12800,
            3200,
            1,
            2.0F,
            4.0F,
            4.0F,
            "活动扳手（HV）",
            "Monkey Wrench (HV)"),
    TRIMMER_LV(
            ToolKind.TRIMMER_LV,
            Voltage.LV,
            Family.TRIMMER,
            RecipeGrid.TRIMMER,
            "tool_head_sword",
            100,
            100,
            100,
            0,
            2.0F,
            0.25F,
            1.0F,
            "修剪机（LV）",
            "Trimmer (LV)");

    private static final Map<ToolKind, ElectricToolCatalog> BY_KIND;

    static {
        EnumMap<ToolKind, ElectricToolCatalog> map = new EnumMap<>(ToolKind.class);
        for (ElectricToolCatalog spec : values()) {
            map.put(spec.kind, spec);
        }
        BY_KIND = Map.copyOf(map);
    }

    private final ToolKind kind;
    private final Voltage voltage;
    private final Family family;
    private final RecipeGrid recipeGrid;
    private final String headPrefix;
    private final int damagePerBlock;
    private final int damagePerCraft;
    private final int damagePerEntity;
    private final int baseQuality;
    private final float baseDamage;
    private final float speedMultiplier;
    private final float durabilityMultiplier;
    private final String langZh;
    private final String langEn;

    ElectricToolCatalog(
            ToolKind kind,
            Voltage voltage,
            Family family,
            RecipeGrid recipeGrid,
            String headPrefix,
            int damagePerBlock,
            int damagePerCraft,
            int damagePerEntity,
            int baseQuality,
            float baseDamage,
            float speedMultiplier,
            float durabilityMultiplier,
            String langZh,
            String langEn) {
        this.kind = kind;
        this.voltage = voltage;
        this.family = family;
        this.recipeGrid = recipeGrid;
        this.headPrefix = headPrefix;
        this.damagePerBlock = damagePerBlock;
        this.damagePerCraft = damagePerCraft;
        this.damagePerEntity = damagePerEntity;
        this.baseQuality = baseQuality;
        this.baseDamage = baseDamage;
        this.speedMultiplier = speedMultiplier;
        this.durabilityMultiplier = durabilityMultiplier;
        this.langZh = langZh;
        this.langEn = langEn;
    }

    public static Optional<ElectricToolCatalog> of(ToolKind kind) {
        return Optional.ofNullable(BY_KIND.get(kind));
    }

    public static boolean isElectric(ToolKind kind) {
        return BY_KIND.containsKey(kind);
    }

    public ToolKind kind() {
        return kind;
    }

    public Voltage voltage() {
        return voltage;
    }

    public Family family() {
        return family;
    }

    public RecipeGrid recipeGrid() {
        return recipeGrid;
    }

    public String headPrefix() {
        return headPrefix;
    }

    public int damagePerBlock() {
        return damagePerBlock;
    }

    public int damagePerCraft() {
        return damagePerCraft;
    }

    public int damagePerEntity() {
        return damagePerEntity;
    }

    public int baseQuality() {
        return baseQuality;
    }

    public float baseDamage() {
        return baseDamage;
    }

    public float speedMultiplier() {
        return speedMultiplier;
    }

    public float durabilityMultiplier() {
        return durabilityMultiplier;
    }

    public String langZh() {
        return langZh;
    }

    public String langEn() {
        return langEn;
    }

    public String itemPath() {
        return "material_" + kind.serializedName();
    }

    public String nameKey() {
        return "item.cruciblecraft." + itemPath();
    }

    public Optional<ElectricToolCatalog> switchPartner() {
        return Optional.ofNullable(switch (this) {
            case WRENCH_LV -> MONKEY_WRENCH_LV;
            case WRENCH_MV -> MONKEY_WRENCH_MV;
            case WRENCH_HV -> MONKEY_WRENCH_HV;
            case MONKEY_WRENCH_LV -> WRENCH_LV;
            case MONKEY_WRENCH_MV -> WRENCH_MV;
            case MONKEY_WRENCH_HV -> WRENCH_HV;
            case JACKHAMMER_HV -> JACKHAMMER_HV_NO_ORES;
            case JACKHAMMER_HV_NO_ORES -> JACKHAMMER_HV;
            default -> null;
        });
    }

    public boolean checkSwitchTarget() {
        return family == Family.JACKHAMMER
                || family == Family.JACKHAMMER_NO_ORES;
    }

    public enum Voltage {
        LV(1, 32L, "lv", 0xFF7F00, "steel_galvanized"),
        MV(2, 128L, "mv", 0xFF0000, "aluminium"),
        HV(3, 512L, "hv", 0x0040FF, "stainless_steel");

        private final int index;
        private final long packet;
        private final String path;
        private final int handleColor;
        private final String hullMaterial;

        Voltage(
                int index,
                long packet,
                String path,
                int handleColor,
                String hullMaterial) {
            this.index = index;
            this.packet = packet;
            this.path = path;
            this.handleColor = handleColor;
            this.hullMaterial = hullMaterial;
        }

        public int index() {
            return index;
        }

        public long packet() {
            return packet;
        }

        public String path() {
            return path;
        }

        public int handleColor() {
            return handleColor;
        }

        public String hullMaterial() {
            return hullMaterial;
        }

        public String motorId() {
            return "cruciblecraft:compact_electric_motor_" + path;
        }

        public String pistonId() {
            return "cruciblecraft:compact_electric_piston_" + path;
        }
    }

    public enum Family {
        MINING_DRILL,
        CHAINSAW,
        WRENCH,
        MONKEY_WRENCH,
        JACKHAMMER,
        JACKHAMMER_NO_ORES,
        BUZZSAW,
        SCREWDRIVER,
        HAND_DRILL,
        MIXER,
        TRIMMER
    }

    public enum RecipeGrid {
        SHARED_HEAD,
        MIXER,
        HAND_DRILL,
        SCREWDRIVER,
        BUZZSAW,
        TRIMMER,
        JACKHAMMER,
        NONE
    }
}
