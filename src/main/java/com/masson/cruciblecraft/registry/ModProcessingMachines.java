package com.masson.cruciblecraft.registry;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.content.item.ExtruderShapeCatalog;
import com.masson.cruciblecraft.machine.processing.CraftingCatalystPolicy;
import com.masson.cruciblecraft.machine.processing.Gt6SidedIo;
import com.masson.cruciblecraft.machine.processing.Gt6BasicMachineGui;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.recipe.gt.CompactPublicationGroups;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.ItemInputAction;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;

import net.minecraft.resources.ResourceLocation;

/** Registry-safe processing specifications for currently implemented machines. */
public final class ModProcessingMachines {
    private static final List<String> PROCESSING_STATUSES = List.of(
            "idle",
            "running",
            "invalid_recipe",
            "recipe_power_exceeded",
            "overcharged",
            "output_blocked",
            "underpowered",
            "unsupported_version",
            "inventory_layout_quarantined",
            "material_quarantined",
            "unknown");

    public static final ProcessingMachineSpec CRUSHER = new ProcessingMachineSpec(
            id("bronze_crusher"),
            id("crusher"),
            () -> ModRecipeMaps.CRUSHER,
            new ProcessingMachineSpec.SlotLayout(2, List.of(0), List.of(1)),
            new ProcessingMachineSpec.TankLayout(List.of(), List.of()),
            new ProcessingMachineSpec.EnergySpec(
                    EnergyType.KINETIC_PUSH,
                    ProcessingMachineSpec.EnergyMode.BUFFERED,
                    1_024L,
                    1_024L),
            Gt6SidedIo.policy("crusher"),
            ModProcessingMachines::validateCrusher,
            ProcessingMachineSpec.BufferPolicy.PAUSE,
            Gt6BasicMachineGui.ui(
                    1, 12, 0, 0,
                    1, 1, 0, 0,
                    PROCESSING_STATUSES));
    public static final ProcessingMachineSpec SLUICE =
            mechanical(
                    "sluice",
                    () -> ModRecipeMaps.SLUICE,
                    true,
                    EnergyType.KINETIC_ROTATION,
                    1, 9, 1, 1);
    public static final ProcessingMachineSpec BATH =
            reusedChemicalSpec("bath", () -> ModRecipeMaps.BATH,
                    6, 6, 1, 3, 4_000, 8_000,
                    6, 6, 1, 3,
                    EnergyType.TIME,
                    ProcessingMachineSpec.EnergyMode.BUFFERED);
    public static final ProcessingMachineSpec CENTRIFUGE =
            reusedChemicalSpec("centrifuge", () -> ModRecipeMaps.CENTRIFUGE,
                    1, 6, 1, 6,
                    CompactPublicationGroups.GT6_PANEL_TANK_CAPACITY,
                    CompactPublicationGroups.GT6_PANEL_TANK_CAPACITY,
                    1, 6, 1, 6,
                    EnergyType.KINETIC_ROTATION,
                    ProcessingMachineSpec.EnergyMode.BUFFERED);
    public static final ProcessingMachineSpec SHREDDER =
            mechanical(
                    "shredder",
                    () -> ModRecipeMaps.SHREDDER,
                    false,
                    EnergyType.KINETIC_ROTATION,
                    1, 12, 0, 0);
    public static final ProcessingMachineSpec SIFTER =
            mechanical(
                    "sifter",
                    () -> ModRecipeMaps.SIFTER,
                    false,
                    EnergyType.KINETIC_PUSH,
                    1, 12, 0, 0);
    public static final ProcessingMachineSpec MORTAR =
            mechanical(
                    "mortar",
                    () -> ModRecipeMaps.MORTAR,
                    false,
                    EnergyType.KINETIC,
                    1, 2, 0, 0);
    public static final ProcessingMachineSpec SMELTER =
            reusedChemicalSpec("smelter", () -> ModRecipeMaps.SMELTER,
                    1, 4, 0, 1, 4_000, 8_000,
                    1, 1, 1, 1,
                    EnergyType.HEAT,
                    ProcessingMachineSpec.EnergyMode.ADJACENT);
    public static final ProcessingMachineSpec MELTER = melterSpec();
    public static final List<ProcessingMachineSpec> PRIMARY_MACHINES = List.of(
            SLUICE, BATH, CENTRIFUGE, SHREDDER, SIFTER, SMELTER, MELTER, MORTAR);
    public static final ProcessingMachineSpec EXTRUDER =
            componentSpec(
                    "extruder",
                    () -> ModRecipeMaps.EXTRUDER,
                    2,
                    false,
                    true,
                    EnergyType.HEAT,
                    2, 2, 0, 0);
    public static final ProcessingMachineSpec CUTTER =
            componentSpec(
                    "cutter",
                    () -> ModRecipeMaps.CUTTER,
                    1,
                    false,
                    false,
                    EnergyType.KINETIC_ROTATION,
                    1, 3, 1, 0);
    public static final ProcessingMachineSpec LATHE =
            componentSpec(
                    "lathe",
                    () -> ModRecipeMaps.LATHE,
                    1,
                    false,
                    false,
                    EnergyType.KINETIC_ROTATION,
                    1, 2, 0, 0);
    public static final ProcessingMachineSpec ROLLINGMILL =
            componentSpec(
                    "rollingmill",
                    () -> ModRecipeMaps.ROLLINGMILL,
                    1,
                    false,
                    false,
                    EnergyType.KINETIC_ROTATION,
                    1, 1, 0, 0);
    public static final ProcessingMachineSpec ROLLFORMER = rollFormerSpec();
    public static final ProcessingMachineSpec SANDING = sandingSpec();
    public static final ProcessingMachineSpec OVEN = ovenSpec();
    public static final ProcessingMachineSpec LARGE_OVEN = largeOvenSpec();
    public static final ProcessingMachineSpec LARGE_CRUSHER = largeCrusherSpec();
    public static final ProcessingMachineSpec LARGE_SHREDDER = largeShredderSpec();
    public static final ProcessingMachineSpec LARGE_MATTER_FABRICATOR =
            largeMatterFabricatorSpec();
    public static final ProcessingMachineSpec CLUSTERMILL = clusterMillSpec();
    public static final ProcessingMachineSpec SLICER = slicerSpec();
    public static final ProcessingMachineSpec LAMINATOR = laminatorSpec();
    public static final ProcessingMachineSpec PRESSUREWASHER =
            pressureWasherSpec();
    public static final ProcessingMachineSpec LOOM = loomSpec();
    public static final ProcessingMachineSpec ELECTRICLOOM =
            electricLoomSpec();
    public static final ProcessingMachineSpec INJECTOR = injectorSpec();
    public static final ProcessingMachineSpec NANOFAB = nanofabSpec();
    public static final ProcessingMachineSpec ROLLBENDER =
            componentSpec(
                    "rollbender",
                    () -> ModRecipeMaps.ROLLBENDER,
                    1,
                    false,
                    false,
                    EnergyType.KINETIC_ROTATION,
                    1, 1, 0, 0);
    public static final ProcessingMachineSpec WIREMILL =
            componentSpec(
                    "wiremill",
                    () -> ModRecipeMaps.WIREMILL,
                    2,
                    false,
                    false,
                    EnergyType.KINETIC_ROTATION,
                    1, 1, 0, 0);
    public static final ProcessingMachineSpec BENDER =
            componentSpec(
                    "bender",
                    () -> ModRecipeMaps.BENDER,
                    1,
                    false,
                    false,
                    EnergyType.KINETIC,
                    1, 1, 0, 0);
    public static final ProcessingMachineSpec ASSEMBLER =
            componentSpec(
                    "assembler",
                    () -> ModRecipeMaps.ASSEMBLER,
                    6,
                    true,
                    false,
                    EnergyType.KINETIC,
                    2, 1, 1, 0);
    public static final ProcessingMachineSpec PRESS =
            componentSpec(
                    "press",
                    () -> ModRecipeMaps.PRESS,
                    3,
                    false,
                    false,
                    EnergyType.KINETIC_PUSH,
                    3, 1, 0, 0);
    public static final List<ProcessingMachineSpec> COMPONENT_MACHINES = List.of(
            EXTRUDER, CUTTER, LATHE, ROLLINGMILL, ROLLBENDER,
            WIREMILL, BENDER, ASSEMBLER, PRESS);
    public static final ProcessingMachineSpec ELECTROLYZER =
            chemicalSpec("electrolyzer", () -> ModRecipeMaps.ELECTROLYZER,
                    2, 6, 2, 3, 32_000, 32_000,
                    2, 6, 2, 6,
                    EnergyType.ELECTRIC);
    public static final ProcessingMachineSpec MIXER =
            mixer("mixer", () -> ModRecipeMaps.MIXER,
                    EnergyType.KINETIC_ROTATION);
    public static final ProcessingMachineSpec DISTILLERY =
            reusedChemicalSpec("distillery", () -> ModRecipeMaps.DISTILLERY,
                    2, 2, 2, 3, 8_000, 8_000,
                    1, 2, 1, 2,
                    EnergyType.HEAT,
                    ProcessingMachineSpec.EnergyMode.ADJACENT);
    public static final ProcessingMachineSpec DISTILLATION_TOWER =
            portFedTowerSpec(
                    "distillation_tower",
                    () -> ModRecipeMaps.DISTILLATION_TOWER,
                    EnergyType.HEAT);
    public static final ProcessingMachineSpec CRYO_DISTILLATION_TOWER =
            portFedTowerSpec(
                    "cryo_distillation_tower",
                    () -> ModRecipeMaps.CRYO_DISTILLATION_TOWER,
                    EnergyType.CU);
    public static final ProcessingMachineSpec FERMENTER =
            reusedChemicalSpec("fermenter", () -> ModRecipeMaps.FERMENTER,
                    1, 1, 1, 1, 32_000, 32_000,
                    1, 1, 1, 1,
                    EnergyType.HEAT,
                    ProcessingMachineSpec.EnergyMode.BUFFERED);
    public static final ProcessingMachineSpec AUTOCLAVE =
            reusedChemicalSpec("autoclave", () -> ModRecipeMaps.AUTOCLAVE,
                    2, 3, 1, 1, 4_000_000, 512_000,
                    2, 3, 1, 1,
                    EnergyType.TIME,
                    ProcessingMachineSpec.EnergyMode.BUFFERED);
    public static final ProcessingMachineSpec DRYING =
            reusedChemicalSpec("drying", () -> ModRecipeMaps.DRYING,
                    1, 1, 1, 1, 32_000, 32_000,
                    1, 1, 1, 3,
                    EnergyType.HEAT,
                    ProcessingMachineSpec.EnergyMode.ADJACENT);
    public static final ProcessingMachineSpec COMPRESSOR =
            chemicalSpec("compressor", () -> ModRecipeMaps.COMPRESSOR, 1, 1, 0, 0,
                    32_000, 32_000,
                    1, 1, 0, 0,
                    EnergyType.KINETIC_PUSH);
    public static final ProcessingMachineSpec GENERIFIER =
            new ProcessingMachineSpec(
                    id("generifier"),
                    id("generifier"),
                    () -> ModRecipeMaps.GENERIFIER,
                    new ProcessingMachineSpec.SlotLayout(
                            0, List.of(), List.of()),
                    new ProcessingMachineSpec.TankLayout(
                            List.of(new ProcessingMachineSpec.TankSpec(
                                    0, 8_000)),
                            List.of(new ProcessingMachineSpec.TankSpec(
                                    1, 8_000))),
                    new ProcessingMachineSpec.EnergySpec(
                            EnergyType.TIME,
                            ProcessingMachineSpec.EnergyMode.ADJACENT,
                            0L,
                            1L),
                    Gt6SidedIo.policy("generifier"),
                    recipe -> recipe.itemInputs().isEmpty()
                                    && recipe.itemOutputs().isEmpty()
                                    && recipe.fluidInputs().size() == 1
                                    && recipe.fluidOutputs().size() == 1
                                    && recipe.eut() == 0L
                                    && recipe.duration() == 1
                            ? Optional.empty()
                            : Optional.of("generifier_recipe_shape"),
                    ProcessingMachineSpec.BufferPolicy.PAUSE,
                    Gt6BasicMachineGui.ui(
                            1, 1, 1, 1,
                            0, 0, 1, 1,
                            PROCESSING_STATUSES));
    public static final ProcessingMachineSpec ROASTER =
            reusedChemicalSpec("roaster", () -> ModRecipeMaps.ROASTER,
                    1, 3, 1, 1, 72_000, 72_000,
                    1, 3, 1, 1,
                    EnergyType.HEAT,
                    ProcessingMachineSpec.EnergyMode.ADJACENT);
    public static final ProcessingMachineSpec COAGULATOR =
            chemicalSpec("coagulator", () -> ModRecipeMaps.COAGULATOR, 0, 1, 1, 0,
                    16_000, 16_000,
                    0, 1, 1, 0,
                    EnergyType.TIME);
    public static final ProcessingMachineSpec CANNER =
            chemicalSpec("canner", () -> ModRecipeMaps.CANNER,
                    2, 1, 1, 1, 128_000, 128_000,
                    2, 1, 1, 1,
                    EnergyType.ELECTRIC);
    public static final ProcessingMachineSpec SQUEEZER = squeezerSpec();
    public static final ProcessingMachineSpec LASER_ENGRAVER = laserSpec();
    public static final ProcessingMachineSpec LASER_WELDER = laserWelderSpec();
    public static final ProcessingMachineSpec PRINTER =
            chemicalSpec("printer", id("printer"), () -> ModRecipeMaps.PRINTER,
                    2, 1, 1, 0, 32_000, 32_000, 2, 1, 1, 0,
                    EnergyType.ELECTRIC, 1_024L);
    public static final ProcessingMachineSpec SCANNER =
            chemicalSpec("scanner", id("scanner"), () -> ModRecipeMaps.SCANNER,
                    1, 1, 0, 0, 32_000, 32_000, 1, 1, 0, 0,
                    EnergyType.ELECTRIC, 1_024L);
    public static final ProcessingMachineSpec AUTOCRAFTER =
            chemicalSpec(
                    "autocrafter",
                    id("autocrafter"),
                    () -> ModRecipeMaps.AUTOCRAFTER,
                    1, 1, 0, 0, 32_000, 32_000, 1, 1, 0, 0,
                    EnergyType.ELECTRIC, 1_024L);
    public static final ProcessingMachineSpec ELECTRIC_MIXER =
            chemicalSpec(
                    "electric_mixer",
                    id("mixer"),
                    () -> ModRecipeMaps.MIXER,
                    6, 1, 6, 2,
                    CompactPublicationGroups.GT6_PANEL_TANK_CAPACITY,
                    CompactPublicationGroups.GT6_PANEL_TANK_CAPACITY,
                    6, 1, 6, 2,
                    EnergyType.ELECTRIC, 1_024L);
    public static final ProcessingMachineSpec BOXINATOR =
            chemicalSpec(
                    "boxinator",
                    id("boxinator"),
                    () -> ModRecipeMaps.BOXINATOR,
                    1, 1, 0, 0, 32_000, 32_000, 1, 1, 0, 0,
                    EnergyType.ELECTRIC, 1_024L);
    public static final ProcessingMachineSpec LIGHTNING =
            chemicalSpec(
                    "lightning",
                    id("lightning"),
                    () -> ModRecipeMaps.LIGHTNING,
                    2, 1, 1, 1, 32_000, 32_000, 2, 1, 1, 1,
                    EnergyType.ELECTRIC, 8_192L);
    public static final ProcessingMachineSpec PLANTALYZER =
            chemicalSpec(
                    "plantalyzer",
                    id("plantalyzer"),
                    () -> ModRecipeMaps.PLANTALYZER,
                    1, 1, 1, 0, 32_000, 32_000, 1, 1, 1, 0,
                    EnergyType.ELECTRIC, 1_024L);
    public static final ProcessingMachineSpec BUMBLELYZER =
            chemicalSpec(
                    "bumblelyzer",
                    id("bumblelyzer"),
                    () -> ModRecipeMaps.BUMBLELYZER,
                    1, 1, 1, 0, 32_000, 32_000, 1, 1, 1, 0,
                    EnergyType.ELECTRIC, 1_024L);
    public static final ProcessingMachineSpec MASSFAB =
            chemicalSpec(
                    "massfab",
                    id("massfab"),
                    () -> ModRecipeMaps.MASSFAB,
                    1, 1, 1, 1, 32_000, 32_000, 1, 1, 1, 1,
                    EnergyType.QUANTUM, 8_192L);
    public static final ProcessingMachineSpec REPLICATOR =
            chemicalSpec(
                    "replicator",
                    id("replicator"),
                    () -> ModRecipeMaps.REPLICATOR,
                    1, 1, 1, 1, 32_000, 32_000, 1, 1, 1, 1,
                    EnergyType.QUANTUM, 8_192L);
    public static final ProcessingMachineSpec FREEZER =
            chemicalSpec(
                    "freezer",
                    id("freezer"),
                    () -> ModRecipeMaps.FREEZER,
                    1, 1, 1, 1, 32_000, 32_000, 1, 1, 1, 1,
                    EnergyType.CU, 1_024L);
    public static final ProcessingMachineSpec CRYO_MIXER =
            chemicalSpec(
                    "cryo_mixer",
                    id("cryo_mixer"),
                    () -> ModRecipeMaps.CRYO_MIXER,
                    6, 1, 6, 2,
                    CompactPublicationGroups.GT6_PANEL_TANK_CAPACITY,
                    CompactPublicationGroups.GT6_PANEL_TANK_CAPACITY,
                    6, 1, 6, 2,
                    EnergyType.CU, 1_024L);
    public static final ProcessingMachineSpec POLARIZER =
            chemicalSpec(
                    "polarizer",
                    id("polarizer"),
                    () -> ModRecipeMaps.POLARIZER,
                    1, 1, 0, 0, 32_000, 32_000, 1, 1, 0, 0,
                    EnergyType.MU, 1_024L);
    public static final ProcessingMachineSpec MAGNETIC_SEPARATOR =
            chemicalSpec(
                    "magnetic_separator",
                    id("magnetic_separator"),
                    () -> ModRecipeMaps.MAGNETIC_SEPARATOR,
                    1, 6, 1, 1, 32_000, 32_000, 1, 6, 1, 1,
                    EnergyType.MU, 1_024L);
    public static final List<ProcessingMachineSpec> PUV_OMEGA_HOST_MACHINES =
            List.of(
                    PRINTER,
                    SCANNER,
                    AUTOCRAFTER,
                    ELECTRIC_MIXER,
                    BOXINATOR,
                    LIGHTNING,
                    PLANTALYZER,
                    BUMBLELYZER,
                    MASSFAB,
                    REPLICATOR,
                    FREEZER,
                    CRYO_MIXER,
                    POLARIZER,
                    MAGNETIC_SEPARATOR);
    public static final List<ProcessingMachineSpec> HYDROCARBON_PROCESSING_MACHINES =
            List.of(GENERIFIER);
    /**
     * Multiblock processing hosts with menus/EMI, kept off
     * {@link #CONFIGURED_MACHINES} so kinetic EMI baselines stay exact.
     */
    public static final List<ProcessingMachineSpec> MULTIBLOCK_MENU_HOSTS =
            List.of(
                    DISTILLATION_TOWER,
                    CRYO_DISTILLATION_TOWER,
                    FERMENTER,
                    LARGE_OVEN,
                    LARGE_CRUSHER,
                    LARGE_SHREDDER,
                    LARGE_MATTER_FABRICATOR);
    /** Chemical recipes can publish into reused maps as well as dedicated maps. */
    public static final List<ProcessingMachineSpec> CHEMICAL_HOST_MACHINES = List.of(
            BATH, CENTRIFUGE, SMELTER, ASSEMBLER,
            ELECTROLYZER, MIXER, DISTILLERY, AUTOCLAVE, DRYING, COMPRESSOR,
            ROASTER, COAGULATOR, CANNER);
    /** Dedicated chemical maps which intentionally have no pre-chemical recipe population. */
    public static final List<ProcessingMachineSpec> CHEMICAL_DEDICATED_MACHINES = List.of(
            ELECTROLYZER, MIXER, DISTILLERY, AUTOCLAVE, DRYING, COMPRESSOR,
            ROASTER, COAGULATOR, CANNER);
    /** Original component envelope: 5,352 non-extruder + 4,648 extruder. */
    public static final int COMPONENT_EXPANSION_BUDGET = 10_000;
    /** Tool rules are budgeted separately from the closed component content set. */
    public static final int TOOL_EXPANSION_BUDGET = 4_000;
    /**
     * Live-map policy envelope for the complete flattened tool projection.
     * The exact policy projects 8,141 component + 3,452 tool = 11,593 recipes. A 10%
     * margin rounded to the next thousand gives 13,000 and remains below the
     * measured reload and index budgets below.
     */
    public static final int LIVE_COMPONENT_MAP_RECIPE_BUDGET = 13_000;
    /**
     * The pinned projection publishes 145 chemical recipes. This final gate keeps
     * measured growth room without weakening the frozen component/tool budgets.
     */
    public static final int CHEMICAL_RECIPE_BUDGET = 200;
    /**
     * Mortar rules publish 220 tag-driven expansions. This independent envelope
     * prevents authored material facts from consuming the global margin
     * unnoticed.
     */
    public static final int MORTAR_MATERIAL_RULE_BUDGET = 256;
    /**
     * Pipe rules publish 257 source-backed fluid/item pipe expansions.
     */
    public static final int PIPE_MATERIAL_RULE_BUDGET = 320;
    /**
     * Known hot- and multi-ingot routes project 1,288 expansions. This
     * provisional ingot-form envelope applies the established 10% margin and rounds
     * up to the next hundred. Container routes remain design-required and must
     * trigger an explicit re-projection rather than consume assumed headroom.
     */
    public static final int INGOT_FORM_MATERIAL_RULE_BUDGET = 1_500;
    /**
     * Hydrocarbon processing is a fixed-row fluid projection. Material-wide authored expansion is
     * forbidden so a source-row route cannot silently turn into a guessed
     * composition rule.
     */
    public static final int HYDROCARBON_MATERIAL_RULE_BUDGET = 0;
    /**
     * Tier profiles bind machine kinds to source-backed numeric profiles. They do
     * not publish recipes or authorize material-wide recipe expansion.
     */
    public static final int TIER_PROFILE_MATERIAL_RULE_BUDGET = 0;
    /** Gunpowder Mixer family: 4 authored entries. Budget 8 allows doubling. */
    public static final int GUNPOWDER_MATERIAL_RULE_BUDGET = 8;
    /** Petroleum: 3 authored recipes (distillery, generifier, fuels_engine). */
    public static final int PETROLEUM_MATERIAL_RULE_BUDGET = 3;
    /** Bounded machine-bootstrap recipes (roaster coal dust, coagulator water). */
    public static final int MACHINE_BOOTSTRAP_MATERIAL_RULE_BUDGET = 2;
    public static final Map<Integer, Integer> AUTHORED_MATERIAL_RULE_BUDGETS =
            Map.of(
                    7, MORTAR_MATERIAL_RULE_BUDGET,
                    8, PIPE_MATERIAL_RULE_BUDGET,
                    10, INGOT_FORM_MATERIAL_RULE_BUDGET,
                    11, HYDROCARBON_MATERIAL_RULE_BUDGET,
                    12, TIER_PROFILE_MATERIAL_RULE_BUDGET,
                    21, GUNPOWDER_MATERIAL_RULE_BUDGET,
                    22, PETROLEUM_MATERIAL_RULE_BUDGET,
                    36, MACHINE_BOOTSTRAP_MATERIAL_RULE_BUDGET);
    /**
     * Verified opening eager scale, not a runtime crash ceiling.
     * Concrete datapack eager 16,966 plus compact eager 14 equals 16,980.
     * The historical 21,000 figure was a 10% margin over a pre-ordinary
     * projection and must not abort player reload.
     */
    public static final int VERIFIED_OPENING_CONCRETE_EAGER = 16_966;
    public static final int VERIFIED_OPENING_COMPACT_EAGER = 14;
    public static final int VERIFIED_OPENING_EAGER_PUBLISHED = 16_980;
    public static final int VERIFIED_OPENING_LAZY_LOGICAL = 50_652;
    public static final int VERIFIED_OPENING_CACHE_CEILING = 876;
    public static final int VERIFIED_OPENING_AUTHORED_ENTRIES = 6_269;
    /**
     * @deprecated Use {@link #VERIFIED_OPENING_EAGER_PUBLISHED} for telemetry
     *     and {@link #TEMPORARY_EAGER_COMPATIBILITY_CEILING} only as a
     *     documented compatibility watermark. Count overage is
     *     {@code UNVERIFIED_SCALE}, never a runtime throw.
     */
    @Deprecated
    public static final int ALL_PUBLISHED_RECIPE_BUDGET = 21_000;
    /**
     * Temporary compatibility watermark for Smelter+Mixer eager
     * (~36,965) plus the historical 10% margin, rounded up. Not a
     * runtime failure. Remove once release performance gates are the
     * only closeout hard door.
     */
    public static final int TEMPORARY_EAGER_COMPATIBILITY_CEILING = 41_000;
    /** Compact-load hybrid soft envelope for recipes retained eagerly in map indexes. */
    public static final int ALL_EAGER_PUBLICATION_SOFT_BUDGET = 18_000;
    /** Compact-load measured 20x hard envelope for logical rows kept by lazy families. */
    public static final int ALL_LAZY_LOGICAL_RECIPE_HARD_CEILING = 56_000;
    /** Aggregate per-epoch cache ceiling for currently selected lazy families. */
    public static final int ALL_LAZY_RECIPE_CACHE_HARD_CEILING = 4_096;
    public static final long RECIPE_RELOAD_BUDGET_MS = 10_000L;
    /**
     * GameTest / {@code runGameTestServer --no-daemon} host envelope.
     * Production still warns at {@link #RECIPE_RELOAD_BUDGET_MS}. Cold-JVM
     * verification recorded 10718 ms and 12732 ms while a warm daemon
     * reload was 3132 ms; publication counts were unchanged.
     */
    public static final long VERIFICATION_RECIPE_RELOAD_BUDGET_MS = 15_000L;
    public static final long RECIPE_INDEX_BUILD_BUDGET_MS = 1_000L;
    public static final long CLIENT_RECIPE_RELOAD_BUDGET_MS = 10_000L;
    public static final long CLIENT_RECIPE_INDEX_BUILD_BUDGET_MS = 3_000L;
    public static final long RECIPE_SYNC_BUDGET_BYTES =
            64L * 1_024L * 1_024L;
    public static final long RECIPE_LOOKUP_P95_BUDGET_NS = 2_000_000L;
    /**
     * GameTest / {@code --no-daemon} lookup p95 envelope. Production still
     * uses {@link #RECIPE_LOOKUP_P95_BUDGET_NS}. Cold-JVM verification
     * recorded 2.152 ms.
     */
    public static final long VERIFICATION_RECIPE_LOOKUP_P95_BUDGET_NS = 3_000_000L;
    public static final long RECIPE_LOOKUP_P95_CANDIDATE_BUDGET = 64L;
    public static final long RECIPE_LOOKUP_MAX_CANDIDATE_HARD_CEILING = 128L;
    public static final List<ProcessingMachineSpec> CONFIGURED_MACHINES =
            java.util.stream.Stream.concat(
                            java.util.stream.Stream.of(
                                            PRIMARY_MACHINES,
                                            COMPONENT_MACHINES,
                                            CHEMICAL_HOST_MACHINES,
                                            HYDROCARBON_PROCESSING_MACHINES)
                                    .flatMap(List::stream),
                            java.util.stream.Stream.concat(
                                    java.util.stream.Stream.of(
                                            LASER_ENGRAVER,
                                            SQUEEZER,
                                            LASER_WELDER,
                                            ROLLFORMER,
                                            SANDING,
                                            OVEN,
                                            CLUSTERMILL,
                                            SLICER,
                                            LAMINATOR,
                                            PRESSUREWASHER,
                                            LOOM,
                                            ELECTRICLOOM,
                                            INJECTOR,
                                            NANOFAB),
                                    PUV_OMEGA_HOST_MACHINES.stream()))
                    .distinct()
                    .toList();
    private static final Map<ResourceLocation, ProcessingMachineSpec> BY_ID =
            CONFIGURED_MACHINES.stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                    ProcessingMachineSpec::id, value -> value));
    private static final Map<ResourceLocation, List<ProcessingMachineSpec>>
            BY_MAP =
            java.util.stream.Stream.concat(
                    java.util.stream.Stream.of(
                            CRUSHER, DISTILLATION_TOWER, CRYO_DISTILLATION_TOWER,
                            FERMENTER),
                    CONFIGURED_MACHINES.stream())
                    .collect(java.util.stream.Collectors.groupingBy(
                            ProcessingMachineSpec::recipeMapId,
                            java.util.stream.Collectors
                                    .toUnmodifiableList()));

    private ModProcessingMachines() {}

    private static Optional<String> validateCrusher(GTRecipe recipe) {
        if (recipe.itemInputs().size() != 1
                || recipe.itemOutputs().size() != 1
                || recipe.outputChances().getFirst() != GTRecipe.GUARANTEED_CHANCE
                || !recipe.fluidInputs().isEmpty()
                || !recipe.fluidOutputs().isEmpty()
                || recipe.eut() <= 0L
                || recipe.eut() > 1_024L) {
            return Optional.of("crusher_recipe_shape");
        }
        return Optional.empty();
    }

    private static Optional<String> validateLargeCrusher(GTRecipe recipe) {
        if (recipe.itemInputs().size() != 1
                || recipe.itemOutputs().size() != 1
                || recipe.outputChances().getFirst() != GTRecipe.GUARANTEED_CHANCE
                || !recipe.fluidInputs().isEmpty()
                || !recipe.fluidOutputs().isEmpty()
                || recipe.eut() <= 0L
                || recipe.eut() > 4_096L) {
            return Optional.of("crusher_recipe_shape");
        }
        return Optional.empty();
    }

    private static Optional<String> validateLargeShredder(GTRecipe recipe) {
        if (recipe.itemInputs().size() != 1
                || recipe.itemOutputs().isEmpty()
                || recipe.itemOutputs().size() > 12
                || !recipe.fluidInputs().isEmpty()
                || !recipe.fluidOutputs().isEmpty()
                || recipe.eut() <= 0L
                || recipe.eut() > 4_096L
                || recipe.outputChances().size() != recipe.itemOutputs().size()
                || recipe.outputChances().stream().anyMatch(
                        chance -> chance <= 0 || chance > GTRecipe.GUARANTEED_CHANCE)) {
            return Optional.of("shredder_recipe_shape");
        }
        return Optional.empty();
    }

    public static ProcessingMachineSpec require(ResourceLocation id) {
        ProcessingMachineSpec spec = BY_ID.get(id);
        if (spec == null) throw new IllegalArgumentException("Unknown processing machine " + id);
        return spec;
    }

    public static Optional<ProcessingMachineSpec> forRecipeMap(ResourceLocation id) {
        return allForRecipeMap(id).stream().findFirst();
    }

    public static List<ProcessingMachineSpec> allForRecipeMap(
            ResourceLocation id) {
        return BY_MAP.getOrDefault(id, List.of());
    }

    private static ProcessingMachineSpec mechanical(
            String id,
            Supplier<RecipeMap> map,
            boolean waterInput,
            EnergyType energyType,
            int gt6InItems,
            int gt6OutItems,
            int gt6InFluids,
            int gt6OutFluids) {
        return spec(
                id,
                map,
                waterInput,
                energyType,
                ProcessingMachineSpec.EnergyMode.BUFFERED,
                gt6InItems,
                gt6OutItems,
                gt6InFluids,
                gt6OutFluids);
    }

    private static ProcessingMachineSpec spec(
            String path,
            Supplier<RecipeMap> map,
            boolean waterInput,
            EnergyType energyType,
            ProcessingMachineSpec.EnergyMode energyMode,
            int gt6InItems,
            int gt6OutItems,
            int gt6InFluids,
            int gt6OutFluids) {
        ProcessingMachineSpec.TankLayout tanks = waterInput
                ? new ProcessingMachineSpec.TankLayout(
                        List.of(new ProcessingMachineSpec.TankSpec(0, 4_000)), List.of())
                : new ProcessingMachineSpec.TankLayout(List.of(), List.of());
        return new ProcessingMachineSpec(
                id(path),
                id(path),
                map,
                new ProcessingMachineSpec.SlotLayout(5, List.of(0), List.of(1, 2, 3, 4)),
                tanks,
                new ProcessingMachineSpec.EnergySpec(
                        energyType,
                        energyMode,
                        energyMode == ProcessingMachineSpec.EnergyMode.BUFFERED ? 4_096L : 0L,
                        1_024L),
                Gt6SidedIo.policy(path),
                recipe -> validateConfigured(recipe, waterInput),
                ProcessingMachineSpec.BufferPolicy.PAUSE,
                Gt6BasicMachineGui.ui(
                        gt6InItems,
                        gt6OutItems,
                        gt6InFluids,
                        gt6OutFluids,
                        1,
                        4,
                        waterInput ? 1 : 0,
                        0,
                        PROCESSING_STATUSES));
    }

    private static Optional<String> validateConfigured(GTRecipe recipe, boolean waterInput) {
        if (recipe.itemInputs().size() > 1
                || recipe.itemOutputs().size() > 4
                || recipe.fluidInputs().size() > (waterInput ? 1 : 0)
                || !recipe.fluidOutputs().isEmpty()
                || recipe.eut() <= 0L
                || recipe.eut() > 1_024L) {
            return Optional.of("configured_recipe_shape");
        }
        return Optional.empty();
    }

    private static ProcessingMachineSpec componentSpec(
            String path,
            Supplier<RecipeMap> map,
            int itemInputs,
            boolean futureFluidInput,
            boolean extruderTool,
            EnergyType energyType,
            int gt6InItems,
            int gt6OutItems,
            int gt6InFluids,
            int gt6OutFluids) {
        List<Integer> inputs = java.util.stream.IntStream.range(0, itemInputs).boxed().toList();
        int outputSlot = itemInputs;
        boolean assemblerCatalysts = "assembler".equals(path);
        boolean pressPreserve = "press".equals(path);
        var tanks = futureFluidInput
                ? new ProcessingMachineSpec.TankLayout(
                        List.of(new ProcessingMachineSpec.TankSpec(0, 4_000)), List.of())
                : new ProcessingMachineSpec.TankLayout(List.of(), List.of());
        ProcessingMachineSpec.SlotLayout itemLayout = extruderTool
                ? new ProcessingMachineSpec.SlotLayout(
                        3,
                        List.of(0, 1),
                        List.of(2),
                        Map.of(
                                0, ProcessingMachineSpec.SlotRole.MATERIAL,
                                1, ProcessingMachineSpec.SlotRole.TOOL,
                                2, ProcessingMachineSpec.SlotRole.OUTPUT),
                        (slot, stack) -> slot == 1
                                ? ExtruderShapeCatalog.isShape(stack)
                                : !ExtruderShapeCatalog.isShape(stack))
                : assemblerCatalysts
                        ? new ProcessingMachineSpec.SlotLayout(
                                7,
                                List.of(0, 1, 2, 3, 4, 5),
                                List.of(6),
                                Map.of(
                                        0, ProcessingMachineSpec.SlotRole.MATERIAL,
                                        1, ProcessingMachineSpec.SlotRole.MATERIAL,
                                        2, ProcessingMachineSpec.SlotRole.MATERIAL,
                                        3, ProcessingMachineSpec.SlotRole.TOOL,
                                        4, ProcessingMachineSpec.SlotRole.TOOL,
                                        5, ProcessingMachineSpec.SlotRole.TOOL,
                                        6, ProcessingMachineSpec.SlotRole.OUTPUT),
                                (slot, stack) -> slot < 3
                                        ? CraftingCatalystPolicy.acceptsMaterialSlot(stack)
                                        : CraftingCatalystPolicy.acceptsToolSlot(stack))
                : new ProcessingMachineSpec.SlotLayout(
                        itemInputs + 1, inputs, List.of(outputSlot));
        ProcessingMachineSpec spec = new ProcessingMachineSpec(
                id(path),
                id(path),
                map,
                itemLayout,
                tanks,
                new ProcessingMachineSpec.EnergySpec(
                        energyType,
                        ProcessingMachineSpec.EnergyMode.BUFFERED,
                        4_096L,
                        256L),
                Gt6SidedIo.policy(path),
                recipe -> validateComponentRecipe(
                        recipe,
                        itemInputs,
                        futureFluidInput,
                        extruderTool,
                        assemblerCatalysts,
                        pressPreserve),
                ProcessingMachineSpec.BufferPolicy.PAUSE,
                Gt6BasicMachineGui.ui(
                        gt6InItems,
                        gt6OutItems,
                        gt6InFluids,
                        gt6OutFluids,
                        extruderTool ? 1 : itemInputs,
                        1,
                        futureFluidInput ? 1 : 0,
                        0,
                        extruderTool ? 1 : -1,
                        PROCESSING_STATUSES));
        return spec;
    }

    private static Optional<String> validateComponentRecipe(
            GTRecipe recipe,
            int itemInputs,
            boolean fluidInput,
            boolean extruderTool,
            boolean assemblerCatalysts,
            boolean pressPreserve) {
        if (recipe.itemInputs().size() > itemInputs
                || recipe.itemOutputs().size() > 1
                || recipe.fluidInputs().size() > (fluidInput ? 1 : 0)
                || !recipe.fluidOutputs().isEmpty()) {
            return Optional.of("component_recipe_shape");
        }
        if (extruderTool
                && (recipe.itemInputs().size() != 2
                || recipe.itemInputCounts().size() != 2
                || recipe.itemInputCounts().get(0) <= 0
                || recipe.itemInputCounts().get(1) != 0
                || java.util.Arrays.stream(recipe.itemInputs().get(1).getItems())
                        .noneMatch(ExtruderShapeCatalog::isShape)
                || java.util.Arrays.stream(recipe.itemInputs().get(1).getItems())
                        .anyMatch(stack -> !ExtruderShapeCatalog.isShape(stack)))) {
            return Optional.of("extruder_tool_shape");
        }
        if (assemblerCatalysts) {
            int materialInputs = 0;
            int catalystInputs = 0;
            int preservedPatterns = 0;
            for (int index = 0; index < recipe.itemInputActions().size(); index++) {
                ItemInputAction action = recipe.itemInputActions().get(index);
                if (!CraftingCatalystPolicy.acceptsIngredient(
                        recipe.itemInputs().get(index), action.kind())) {
                    return Optional.of("assembler_catalyst_shape");
                }
                if (action.kind() == ItemInputAction.Kind.CONSUME) {
                    materialInputs++;
                } else {
                    catalystInputs++;
                    if (action.kind() == ItemInputAction.Kind.PRESERVE) {
                        preservedPatterns++;
                    }
                }
            }
            if (materialInputs > 3 || catalystInputs > 3 || preservedPatterns > 1) {
                return Optional.of("assembler_catalyst_shape");
            }
        } else if (recipe.itemInputActions().stream()
                .anyMatch(action -> action.kind() == ItemInputAction.Kind.WEAR)) {
            return Optional.of("wear_tool_shape");
        }
        // The 4,096-unit buffer is storage; the real per-recipe maxPacket policy is 256.
        if (recipe.eut() <= 0L || recipe.eut() > 256L) {
            return Optional.of("component_recipe_energy");
        }
        if (recipe.itemInputCounts().stream().anyMatch(count ->
                        count < 0 || count > 64)
                || java.util.stream.IntStream.range(
                                0, recipe.itemInputCounts().size())
                        .anyMatch(index -> recipe.itemInputCounts().get(index) == 0
                                && !(extruderTool
                                && recipe.itemInputActions().get(index).kind()
                                        == ItemInputAction.Kind.PRESERVE)
                                && !(assemblerCatalysts
                                && recipe.itemInputActions().get(index).kind()
                                        != ItemInputAction.Kind.CONSUME)
                                && !(pressPreserve
                                && recipe.itemInputActions().get(index).kind()
                                        == ItemInputAction.Kind.PRESERVE))
                || recipe.itemOutputs().stream().anyMatch(stack ->
                        stack.isEmpty() || stack.getCount() <= 0)
                || recipe.outputChances().stream().anyMatch(
                        chance -> chance <= 0 || chance > GTRecipe.GUARANTEED_CHANCE)
                || recipe.fluidInputs().stream().anyMatch(
                        stack -> stack.isEmpty() || stack.getAmount() > 4_000)) {
            return Optional.of("component_recipe_amount");
        }
        return Optional.empty();
    }

    private static ProcessingMachineSpec reusedChemicalSpec(
            String path,
            Supplier<RecipeMap> map,
            int itemInputs,
            int itemOutputs,
            int fluidInputs,
            int fluidOutputs,
            int fluidInputCapacity,
            int fluidOutputCapacity,
            int gt6InItems,
            int gt6OutItems,
            int gt6InFluids,
            int gt6OutFluids,
            EnergyType energyType,
            ProcessingMachineSpec.EnergyMode energyMode) {
        ProcessingMachineSpec layout = chemicalSpec(
                path,
                map,
                itemInputs,
                itemOutputs,
                fluidInputs,
                fluidOutputs,
                fluidInputCapacity,
                fluidOutputCapacity,
                gt6InItems,
                gt6OutItems,
                gt6InFluids,
                gt6OutFluids,
                energyType);
        return new ProcessingMachineSpec(
                layout.id(),
                layout.recipeMapId(),
                map,
                layout.items(),
                layout.fluids(),
                new ProcessingMachineSpec.EnergySpec(
                        energyType,
                        energyMode,
                        energyMode == ProcessingMachineSpec.EnergyMode.BUFFERED
                                ? 4_096L
                                : 0L,
                        1_024L),
                layout.sidedIo(),
                recipe -> validateChemicalRecipe(
                        recipe,
                        itemInputs,
                        itemOutputs,
                        fluidInputs,
                        fluidOutputs,
                        fluidInputCapacity,
                        fluidOutputCapacity,
                        chemicalAllowsPreserveCatalyst(path),
                        energyType,
                        "centrifuge".equals(path) || "fermenter".equals(path)
                                ? 4_096L
                                : 1_024L),
                layout.buffering(),
                layout.ui());
    }

    /**
     * GT6 towers accept energy only through the nine heat transmitters.
     * Hull energy faces stay NONE; ports call {@code insertFromMultiblockPort}.
     */
    private static ProcessingMachineSpec portFedTowerSpec(
            String path,
            Supplier<RecipeMap> map,
            EnergyType energyType) {
        ProcessingMachineSpec layout = reusedChemicalSpec(
                path,
                map,
                1, 3, 1, 9, 8_000, 8_000,
                1, 3, 1, 9,
                energyType,
                ProcessingMachineSpec.EnergyMode.BUFFERED);
        return new ProcessingMachineSpec(
                layout.id(),
                layout.recipeMapId(),
                map,
                layout.items(),
                layout.fluids(),
                layout.energy(),
                layout.sidedIo(),
                layout.validator(),
                layout.buffering(),
                layout.ui());
    }

    /**
     * Mixer keeps the platinum catalyst; electrolyzer and autoclave keep GT6
     * programmed_circuit (count-0 PRESERVE). Other chemical hosts remain CONSUME-only.
     */
    /**
     * Mixer inventory uses the GT6 panel layout (6/1/6/2). Bronze chemical-map
     * recipes remain valid as a subset; mixer ordinary-closure groups declare
     * {@code gt6_panel} via {@link CompactPublicationGroups}.
     */
    private static ProcessingMachineSpec mixer(
            String path,
            Supplier<RecipeMap> map,
            EnergyType energyType) {
        ProcessingMachineSpec layout = chemicalSpec(
                path,
                map,
                6, 1, 6, 2,
                CompactPublicationGroups.GT6_PANEL_TANK_CAPACITY,
                CompactPublicationGroups.GT6_PANEL_TANK_CAPACITY,
                6, 1, 6, 2,
                energyType);
        return new ProcessingMachineSpec(
                layout.id(),
                layout.recipeMapId(),
                map,
                layout.items(),
                layout.fluids(),
                layout.energy(),
                layout.sidedIo(),
                recipe -> validateMixer(recipe),
                layout.buffering(),
                layout.ui());
    }

    private static Optional<String> validateMixer(GTRecipe recipe) {
        if (fitsEnvelope(recipe, 4, 1, 3, 2)
                && fluidAmountsFit(
                        recipe,
                        CompactPublicationGroups.BRONZE_TANK_CAPACITY,
                        CompactPublicationGroups.BRONZE_TANK_CAPACITY)) {
            return validateChemicalRecipe(
                    recipe,
                    4, 1, 3, 2,
                    CompactPublicationGroups.BRONZE_TANK_CAPACITY,
                    CompactPublicationGroups.BRONZE_TANK_CAPACITY,
                    true,
                    EnergyType.KINETIC_ROTATION,
                    1_024L);
        }
        return validateChemicalRecipe(
                recipe,
                6, 1, 6, 2,
                CompactPublicationGroups.GT6_PANEL_TANK_CAPACITY,
                CompactPublicationGroups.GT6_PANEL_TANK_CAPACITY,
                true,
                EnergyType.KINETIC_ROTATION,
                1_024L);
    }

    private static boolean fitsEnvelope(
            GTRecipe recipe,
            int itemInputs,
            int itemOutputs,
            int fluidInputs,
            int fluidOutputs) {
        return recipe.itemInputs().size() <= itemInputs
                && recipe.itemOutputs().size() <= itemOutputs
                && recipe.fluidInputs().size() <= fluidInputs
                && recipe.fluidOutputs().size() <= fluidOutputs;
    }

    private static boolean fluidAmountsFit(
            GTRecipe recipe,
            int fluidInputCapacity,
            int fluidOutputCapacity) {
        return recipe.fluidInputs().stream().allMatch(
                stack -> stack.getAmount() <= fluidInputCapacity)
                && recipe.fluidOutputs().stream().allMatch(
                stack -> stack.getAmount() <= fluidOutputCapacity);
    }

    private static boolean chemicalAllowsPreserveCatalyst(String path) {
        return "mixer".equals(path)
                || "electric_mixer".equals(path)
                || "cryo_mixer".equals(path)
                || "electrolyzer".equals(path)
                || "autoclave".equals(path)
                || "fermenter".equals(path)
                || "lightning".equals(path)
                || "nanofab".equals(path)
                || "canner".equals(path)
                || "press".equals(path)
                || "freezer".equals(path)
                || "replicator".equals(path);
    }

    private static ProcessingMachineSpec chemicalSpec(
            String path,
            Supplier<RecipeMap> map,
            int itemInputs,
            int itemOutputs,
            int fluidInputs,
            int fluidOutputs,
            int fluidInputCapacity,
            int fluidOutputCapacity,
            int gt6InItems,
            int gt6OutItems,
            int gt6InFluids,
            int gt6OutFluids,
            EnergyType energyType) {
        return chemicalSpec(
                path,
                id(path),
                map,
                itemInputs,
                itemOutputs,
                fluidInputs,
                fluidOutputs,
                fluidInputCapacity,
                fluidOutputCapacity,
                gt6InItems,
                gt6OutItems,
                gt6InFluids,
                gt6OutFluids,
                energyType,
                1_024L);
    }

    private static ProcessingMachineSpec chemicalSpec(
            String path,
            ResourceLocation recipeMapId,
            Supplier<RecipeMap> map,
            int itemInputs,
            int itemOutputs,
            int fluidInputs,
            int fluidOutputs,
            int fluidInputCapacity,
            int fluidOutputCapacity,
            int gt6InItems,
            int gt6OutItems,
            int gt6InFluids,
            int gt6OutFluids,
            EnergyType energyType,
            long energyMax) {
        int layoutItemOutputs = itemOutputs;
        int layoutFluidOutputs = fluidOutputs;
        List<Integer> inputSlots =
                java.util.stream.IntStream.range(0, itemInputs).boxed().toList();
        List<Integer> outputSlots = java.util.stream.IntStream
                .range(itemInputs, itemInputs + layoutItemOutputs)
                .boxed()
                .toList();
        List<ProcessingMachineSpec.TankSpec> inputTanks = java.util.stream.IntStream
                .range(0, fluidInputs)
                .mapToObj(index ->
                        new ProcessingMachineSpec.TankSpec(index, fluidInputCapacity))
                .toList();
        List<ProcessingMachineSpec.TankSpec> outputTanks = java.util.stream.IntStream
                .range(fluidInputs, fluidInputs + layoutFluidOutputs)
                .mapToObj(index ->
                        new ProcessingMachineSpec.TankSpec(index, fluidOutputCapacity))
                .toList();
        return new ProcessingMachineSpec(
                id(path),
                recipeMapId,
                map,
                new ProcessingMachineSpec.SlotLayout(
                        itemInputs + layoutItemOutputs, inputSlots, outputSlots),
                new ProcessingMachineSpec.TankLayout(inputTanks, outputTanks),
                new ProcessingMachineSpec.EnergySpec(
                        energyType,
                        ProcessingMachineSpec.EnergyMode.BUFFERED,
                        "massfab".equals(path) ? 1_048_576L : 65_536L,
                        Math.max(1_024L, energyMax)),
                Gt6SidedIo.policy(path),
                recipe -> validateChemicalRecipe(
                        recipe,
                        itemInputs,
                        itemOutputs,
                        fluidInputs,
                        fluidOutputs,
                        fluidInputCapacity,
                        fluidOutputCapacity,
                        chemicalAllowsPreserveCatalyst(path),
                        energyType,
                        energyMax),
                ProcessingMachineSpec.BufferPolicy.PAUSE,
                Gt6BasicMachineGui.ui(
                        gt6InItems,
                        gt6OutItems,
                        gt6InFluids,
                        gt6OutFluids,
                        itemInputs,
                        layoutItemOutputs,
                        fluidInputs,
                        layoutFluidOutputs,
                        PROCESSING_STATUSES));
    }

    /**
     * GT6 centrifuge GUI envelope used by steel/titanium/tungstensteel
     * variants. Bronze {@link #CENTRIFUGE} stays on {@link #validateChemicalRecipe}.
     */
    public static Optional<String> validateCentrifugeCompactEnvelope(GTRecipe recipe) {
        if (recipe.itemInputs().size() > 1
                || recipe.itemOutputs().size() > 6
                || recipe.fluidInputs().size() > 1
                || recipe.fluidOutputs().size() > 6) {
            return Optional.of("centrifuge_compact_shape");
        }
        if (recipe.itemInputActions().stream()
                .anyMatch(action -> action.kind() != ItemInputAction.Kind.CONSUME)) {
            return Optional.of("centrifuge_compact_input_action");
        }
        if (recipe.eut() <= 0L || recipe.eut() > 4_096L) {
            return Optional.of("centrifuge_compact_energy");
        }
        if (recipe.itemInputCounts().stream().anyMatch(count -> count < 0)
                || recipe.itemOutputs().stream().anyMatch(
                        stack -> stack.isEmpty() || stack.getCount() <= 0)
                || recipe.outputChances().stream().anyMatch(
                        chance -> chance <= 0 || chance > GTRecipe.GUARANTEED_CHANCE)
                || recipe.fluidInputs().stream().anyMatch(
                        stack -> stack.isEmpty() || stack.getAmount() > 100_000)
                || recipe.fluidOutputs().stream().anyMatch(
                        stack -> stack.isEmpty() || stack.getAmount() > 8_000)) {
            return Optional.of("centrifuge_compact_amount");
        }
        return Optional.empty();
    }

    private static Optional<String> validateChemicalRecipe(
            GTRecipe recipe,
            int itemInputs,
            int itemOutputs,
            int fluidInputs,
            int fluidOutputs,
            int fluidInputCapacity,
            int fluidOutputCapacity,
            boolean allowPreserveCatalyst,
            EnergyType energyType,
            long energyMax) {
        if (recipe.itemInputs().size() > itemInputs
                || recipe.itemOutputs().size() > itemOutputs
                || recipe.fluidInputs().size() > fluidInputs
                || recipe.fluidOutputs().size() > fluidOutputs) {
            return Optional.of("chemical_recipe_shape");
        }
        if (invalidChemicalInputActions(recipe, allowPreserveCatalyst)) {
            return Optional.of("chemical_recipe_input_action");
        }
        boolean timeZeroEut = energyType == EnergyType.TIME
                && recipe.eut() == 0L
                && recipe.duration() > 0;
        if (!timeZeroEut && (recipe.eut() <= 0L || recipe.eut() > energyMax)) {
            return Optional.of("chemical_recipe_energy");
        }
        if (recipe.itemInputCounts().stream().anyMatch(count -> count > 64)
                || recipe.itemOutputs().stream().anyMatch(
                        stack -> stack.isEmpty() || stack.getCount() <= 0)
                || recipe.outputChances().stream().anyMatch(
                        chance -> chance <= 0 || chance > GTRecipe.GUARANTEED_CHANCE)
                || recipe.fluidInputs().stream().anyMatch(
                        stack -> stack.isEmpty()
                                || stack.getAmount() > fluidInputCapacity)
                || recipe.fluidOutputs().stream().anyMatch(
                        stack -> stack.isEmpty()
                                || stack.getAmount() > fluidOutputCapacity)) {
            return Optional.of("chemical_recipe_amount");
        }
        return Optional.empty();
    }

    private static boolean invalidChemicalInputActions(
            GTRecipe recipe,
            boolean allowPreserveCatalyst) {
        for (int index = 0; index < recipe.itemInputActions().size(); index++) {
            ItemInputAction.Kind kind = recipe.itemInputActions().get(index).kind();
            int count = recipe.itemInputCounts().get(index);
            if (kind == ItemInputAction.Kind.CONSUME && count > 0) {
                continue;
            }
            if (allowPreserveCatalyst
                    && kind == ItemInputAction.Kind.PRESERVE
                    && count == 0) {
                continue;
            }
            return true;
        }
        return false;
    }

    private static ProcessingMachineSpec rollFormerSpec() {
        return new ProcessingMachineSpec(
                id("rollformer"),
                id("rollformer"),
                () -> ModRecipeMaps.ROLLFORMER,
                new ProcessingMachineSpec.SlotLayout(2, List.of(0), List.of(1)),
                new ProcessingMachineSpec.TankLayout(List.of(), List.of()),
                new ProcessingMachineSpec.EnergySpec(
                        EnergyType.KINETIC_ROTATION,
                        ProcessingMachineSpec.EnergyMode.BUFFERED,
                        4_096L,
                        256L),
                Gt6SidedIo.policy("rollformer"),
                recipe -> validateComponentRecipe(
                        recipe, 1, false, false, false, false),
                ProcessingMachineSpec.BufferPolicy.PAUSE,
                Gt6BasicMachineGui.ui(
                        1, 1, 0, 0,
                        1, 1, 0, 0,
                        PROCESSING_STATUSES));
    }

    private static ProcessingMachineSpec largeOvenSpec() {
        return new ProcessingMachineSpec(
                id("large_oven"),
                id("oven"),
                () -> ModRecipeMaps.OVEN,
                new ProcessingMachineSpec.SlotLayout(2, List.of(0), List.of(1)),
                new ProcessingMachineSpec.TankLayout(List.of(), List.of()),
                new ProcessingMachineSpec.EnergySpec(
                        EnergyType.ELECTRIC,
                        ProcessingMachineSpec.EnergyMode.BUFFERED,
                        4_096L,
                        4_096L),
                Gt6SidedIo.policy("large_oven"),
                ModProcessingMachines::validateOvenRecipe,
                ProcessingMachineSpec.BufferPolicy.PAUSE,
                Gt6BasicMachineGui.ui(
                        1, 1, 0, 0,
                        1, 1, 0, 0,
                        PROCESSING_STATUSES));
    }

    private static ProcessingMachineSpec largeMatterFabricatorSpec() {
        return new ProcessingMachineSpec(
                id("large_matter_fabricator"),
                id("massfab"),
                () -> ModRecipeMaps.MASSFAB,
                new ProcessingMachineSpec.SlotLayout(2, List.of(0), List.of(1)),
                new ProcessingMachineSpec.TankLayout(
                        List.of(new ProcessingMachineSpec.TankSpec(0, 32_000)),
                        List.of(new ProcessingMachineSpec.TankSpec(1, 32_000))),
                new ProcessingMachineSpec.EnergySpec(
                        EnergyType.QUANTUM,
                        ProcessingMachineSpec.EnergyMode.BUFFERED,
                        2_097_152L,
                        2_097_152L),
                Gt6SidedIo.policy("large_matter_fabricator"),
                recipe -> validateChemicalRecipe(
                        recipe, 1, 1, 1, 1, 32_000, 32_000,
                        chemicalAllowsPreserveCatalyst("massfab"),
                        EnergyType.QUANTUM,
                        2_097_152L),
                ProcessingMachineSpec.BufferPolicy.PAUSE,
                Gt6BasicMachineGui.ui(
                        1, 1, 1, 1,
                        1, 1, 1, 1,
                        PROCESSING_STATUSES));
    }

    private static ProcessingMachineSpec largeCrusherSpec() {
        return new ProcessingMachineSpec(
                id("large_crusher"),
                id("crusher"),
                () -> ModRecipeMaps.CRUSHER,
                new ProcessingMachineSpec.SlotLayout(2, List.of(0), List.of(1)),
                new ProcessingMachineSpec.TankLayout(List.of(), List.of()),
                new ProcessingMachineSpec.EnergySpec(
                        EnergyType.KINETIC_ROTATION,
                        ProcessingMachineSpec.EnergyMode.BUFFERED,
                        4_096L,
                        4_096L),
                Gt6SidedIo.policy("large_crusher"),
                ModProcessingMachines::validateLargeCrusher,
                ProcessingMachineSpec.BufferPolicy.PAUSE,
                Gt6BasicMachineGui.ui(
                        1, 12, 0, 0,
                        1, 1, 0, 0,
                        PROCESSING_STATUSES));
    }

    private static ProcessingMachineSpec largeShredderSpec() {
        return new ProcessingMachineSpec(
                id("large_shredder"),
                id("shredder"),
                () -> ModRecipeMaps.SHREDDER,
                new ProcessingMachineSpec.SlotLayout(
                        13,
                        List.of(0),
                        List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)),
                new ProcessingMachineSpec.TankLayout(List.of(), List.of()),
                new ProcessingMachineSpec.EnergySpec(
                        EnergyType.KINETIC_ROTATION,
                        ProcessingMachineSpec.EnergyMode.BUFFERED,
                        4_096L,
                        4_096L),
                Gt6SidedIo.policy("large_shredder"),
                ModProcessingMachines::validateLargeShredder,
                ProcessingMachineSpec.BufferPolicy.PAUSE,
                Gt6BasicMachineGui.ui(
                        1, 12, 0, 0,
                        1, 12, 0, 0,
                        PROCESSING_STATUSES));
    }

    private static ProcessingMachineSpec ovenSpec() {
        return new ProcessingMachineSpec(
                id("oven"),
                id("oven"),
                () -> ModRecipeMaps.OVEN,
                new ProcessingMachineSpec.SlotLayout(2, List.of(0), List.of(1)),
                new ProcessingMachineSpec.TankLayout(List.of(), List.of()),
                new ProcessingMachineSpec.EnergySpec(
                        EnergyType.HEAT,
                        ProcessingMachineSpec.EnergyMode.ADJACENT,
                        0L,
                        8_192L),
                Gt6SidedIo.policy("oven"),
                ModProcessingMachines::validateOvenRecipe,
                ProcessingMachineSpec.BufferPolicy.PAUSE,
                Gt6BasicMachineGui.ui(
                        1, 1, 0, 0,
                        1, 1, 0, 0,
                        PROCESSING_STATUSES));
    }

    private static Optional<String> validateOvenRecipe(GTRecipe recipe) {
        if (recipe.itemInputs().size() != 1
                || recipe.itemOutputs().size() != 1
                || !recipe.fluidInputs().isEmpty()
                || !recipe.fluidOutputs().isEmpty()
                || recipe.eut() != 16L
                || recipe.duration() != 16) {
            return Optional.of("oven_recipe_shape");
        }
        if (recipe.itemInputCounts().size() != 1
                || recipe.itemInputCounts().getFirst() <= 0
                || recipe.itemOutputs().getFirst().isEmpty()
                || recipe.itemOutputs().getFirst().getCount() <= 0) {
            return Optional.of("oven_recipe_amount");
        }
        return Optional.empty();
    }

    private static ProcessingMachineSpec sandingSpec() {
        return new ProcessingMachineSpec(
                id("sanding"),
                id("sanding"),
                () -> ModRecipeMaps.SANDING,
                new ProcessingMachineSpec.SlotLayout(3, List.of(0), List.of(1, 2)),
                new ProcessingMachineSpec.TankLayout(List.of(), List.of()),
                new ProcessingMachineSpec.EnergySpec(
                        EnergyType.KINETIC_ROTATION,
                        ProcessingMachineSpec.EnergyMode.BUFFERED,
                        4_096L,
                        256L),
                Gt6SidedIo.policy("sanding"),
                ModProcessingMachines::validateSandingRecipe,
                ProcessingMachineSpec.BufferPolicy.PAUSE,
                Gt6BasicMachineGui.ui(
                        1, 2, 0, 0,
                        1, 2, 0, 0,
                        PROCESSING_STATUSES));
    }

    private static Optional<String> validateSandingRecipe(GTRecipe recipe) {
        if (recipe.itemInputs().size() != 1
                || recipe.itemOutputs().isEmpty()
                || recipe.itemOutputs().size() > 2
                || !recipe.fluidInputs().isEmpty()
                || !recipe.fluidOutputs().isEmpty()
                || recipe.eut() != 16L) {
            return Optional.of("sanding_recipe_shape");
        }
        if (recipe.itemInputCounts().size() != 1
                || recipe.itemInputCounts().getFirst() <= 0
                || recipe.itemOutputs().getFirst().isEmpty()
                || recipe.itemOutputs().getFirst().getCount() <= 0) {
            return Optional.of("sanding_recipe_amount");
        }
        return Optional.empty();
    }

    private static ProcessingMachineSpec clusterMillSpec() {
        return new ProcessingMachineSpec(
                id("clustermill"),
                id("clustermill"),
                () -> ModRecipeMaps.CLUSTERMILL,
                new ProcessingMachineSpec.SlotLayout(2, List.of(0), List.of(1)),
                new ProcessingMachineSpec.TankLayout(List.of(), List.of()),
                new ProcessingMachineSpec.EnergySpec(
                        EnergyType.KINETIC_ROTATION,
                        ProcessingMachineSpec.EnergyMode.BUFFERED,
                        4_096L,
                        256L),
                Gt6SidedIo.policy("clustermill"),
                recipe -> validateComponentRecipe(
                        recipe, 1, false, false, false, false),
                ProcessingMachineSpec.BufferPolicy.PAUSE,
                Gt6BasicMachineGui.ui(
                        1, 1, 0, 0,
                        1, 1, 0, 0,
                        PROCESSING_STATUSES));
    }

    private static ProcessingMachineSpec laminatorSpec() {
        return new ProcessingMachineSpec(
                id("laminator"),
                id("laminator"),
                () -> ModRecipeMaps.LAMINATOR,
                new ProcessingMachineSpec.SlotLayout(3, List.of(0, 1), List.of(2)),
                new ProcessingMachineSpec.TankLayout(List.of(), List.of()),
                new ProcessingMachineSpec.EnergySpec(
                        EnergyType.HEAT,
                        ProcessingMachineSpec.EnergyMode.ADJACENT,
                        0L,
                        8_192L),
                Gt6SidedIo.policy("laminator"),
                ModProcessingMachines::validateLaminatorRecipe,
                ProcessingMachineSpec.BufferPolicy.PAUSE,
                Gt6BasicMachineGui.ui(
                        2, 1, 0, 0,
                        2, 1, 0, 0,
                        PROCESSING_STATUSES));
    }

    private static Optional<String> validateLaminatorRecipe(GTRecipe recipe) {
        if (recipe.itemInputs().size() > 2
                || recipe.itemOutputs().size() != 1
                || !recipe.fluidInputs().isEmpty()
                || !recipe.fluidOutputs().isEmpty()
                || recipe.eut() <= 0L
                || recipe.eut() > 8_192L) {
            return Optional.of("laminator_recipe_shape");
        }
        return Optional.empty();
    }

    private static ProcessingMachineSpec melterSpec() {
        return new ProcessingMachineSpec(
                id("melter"),
                id("melter"),
                () -> ModRecipeMaps.MELTER,
                new ProcessingMachineSpec.SlotLayout(2, List.of(0), List.of(1)),
                new ProcessingMachineSpec.TankLayout(
                        List.of(new ProcessingMachineSpec.TankSpec(0, 4_000)),
                        List.of(new ProcessingMachineSpec.TankSpec(1, 8_000))),
                new ProcessingMachineSpec.EnergySpec(
                        EnergyType.HEAT,
                        ProcessingMachineSpec.EnergyMode.ADJACENT,
                        0L,
                        1_024L),
                Gt6SidedIo.policy("melter"),
                ModProcessingMachines::validateMelterRecipe,
                ProcessingMachineSpec.BufferPolicy.PAUSE,
                Gt6BasicMachineGui.ui(
                        1, 1, 1, 1,
                        1, 1, 1, 1,
                        PROCESSING_STATUSES));
    }

    private static Optional<String> validateMelterRecipe(GTRecipe recipe) {
        if (recipe.itemInputs().size() > 1
                || recipe.itemOutputs().size() > 1
                || recipe.fluidInputs().size() > 1
                || recipe.fluidOutputs().size() > 1
                || recipe.eut() <= 0L
                || recipe.eut() > 1_024L
                || (recipe.itemInputs().isEmpty() && recipe.fluidInputs().isEmpty())
                || (recipe.itemOutputs().isEmpty() && recipe.fluidOutputs().isEmpty())) {
            return Optional.of("melter_recipe_shape");
        }
        if (recipe.fluidInputs().stream()
                        .anyMatch(stack -> stack.getAmount() > 4_000)
                || recipe.fluidOutputs().stream()
                        .anyMatch(stack -> stack.getAmount() > 8_000)) {
            return Optional.of("melter_recipe_amount");
        }
        return Optional.empty();
    }

    private static ProcessingMachineSpec pressureWasherSpec() {
        return new ProcessingMachineSpec(
                id("pressurewasher"),
                id("pressurewasher"),
                () -> ModRecipeMaps.PRESSUREWASHER,
                new ProcessingMachineSpec.SlotLayout(3, List.of(0), List.of(1, 2)),
                new ProcessingMachineSpec.TankLayout(
                        List.of(new ProcessingMachineSpec.TankSpec(0, 4_000)),
                        List.of()),
                new ProcessingMachineSpec.EnergySpec(
                        EnergyType.KINETIC_ROTATION,
                        ProcessingMachineSpec.EnergyMode.BUFFERED,
                        4_096L,
                        256L),
                Gt6SidedIo.policy("pressurewasher"),
                ModProcessingMachines::validatePressureWasherRecipe,
                ProcessingMachineSpec.BufferPolicy.PAUSE,
                Gt6BasicMachineGui.ui(
                        1, 2, 1, 0,
                        1, 2, 1, 0,
                        PROCESSING_STATUSES));
    }

    private static Optional<String> validatePressureWasherRecipe(GTRecipe recipe) {
        if (recipe.itemInputs().size() > 1
                || recipe.itemOutputs().size() > 2
                || recipe.fluidInputs().size() > 1
                || !recipe.fluidOutputs().isEmpty()
                || recipe.eut() <= 0L
                || recipe.eut() > 256L) {
            return Optional.of("pressurewasher_recipe_shape");
        }
        if (recipe.fluidInputs().stream()
                .anyMatch(stack -> stack.getAmount() > 4_000)) {
            return Optional.of("pressurewasher_recipe_amount");
        }
        return Optional.empty();
    }

    private static ProcessingMachineSpec loomSpec() {
        return loomSpec(
                "loom",
                EnergyType.KINETIC_ROTATION,
                ProcessingMachineSpec.EnergyMode.BUFFERED,
                4_096L,
                256L);
    }

    private static ProcessingMachineSpec electricLoomSpec() {
        return loomSpec(
                "electricloom",
                EnergyType.ELECTRIC,
                ProcessingMachineSpec.EnergyMode.BUFFERED,
                65_536L,
                8_192L);
    }

    private static ProcessingMachineSpec loomSpec(
            String id,
            EnergyType energyType,
            ProcessingMachineSpec.EnergyMode energyMode,
            long capacity,
            long maxPacket) {
        return new ProcessingMachineSpec(
                id(id),
                id("loom"),
                () -> ModRecipeMaps.LOOM,
                new ProcessingMachineSpec.SlotLayout(
                        7, List.of(0, 1, 2, 3, 4, 5), List.of(6)),
                new ProcessingMachineSpec.TankLayout(List.of(), List.of()),
                new ProcessingMachineSpec.EnergySpec(
                        energyType, energyMode, capacity, maxPacket),
                Gt6SidedIo.policy(id),
                ModProcessingMachines::validateLoomRecipe,
                ProcessingMachineSpec.BufferPolicy.PAUSE,
                Gt6BasicMachineGui.ui(
                        6, 1, 0, 0,
                        6, 1, 0, 0,
                        PROCESSING_STATUSES));
    }

    private static Optional<String> validateLoomRecipe(GTRecipe recipe) {
        if (recipe.itemInputs().isEmpty()
                || recipe.itemInputs().size() > 6
                || recipe.itemOutputs().size() != 1
                || !recipe.fluidInputs().isEmpty()
                || !recipe.fluidOutputs().isEmpty()
                || recipe.eut() <= 0L
                || recipe.eut() > 8_192L) {
            return Optional.of("loom_recipe_shape");
        }
        return Optional.empty();
    }

    private static ProcessingMachineSpec injectorSpec() {
        return new ProcessingMachineSpec(
                id("injector"),
                id("injector"),
                () -> ModRecipeMaps.INJECTOR,
                new ProcessingMachineSpec.SlotLayout(3, List.of(0, 1), List.of(2)),
                new ProcessingMachineSpec.TankLayout(
                        List.of(
                                new ProcessingMachineSpec.TankSpec(0, 4_000),
                                new ProcessingMachineSpec.TankSpec(1, 4_000)),
                        List.of(new ProcessingMachineSpec.TankSpec(2, 4_000))),
                new ProcessingMachineSpec.EnergySpec(
                        EnergyType.ELECTRIC,
                        ProcessingMachineSpec.EnergyMode.BUFFERED,
                        65_536L,
                        8_192L),
                Gt6SidedIo.policy("injector"),
                ModProcessingMachines::validateInjectorRecipe,
                ProcessingMachineSpec.BufferPolicy.PAUSE,
                Gt6BasicMachineGui.ui(
                        2, 1, 2, 1,
                        2, 1, 2, 1,
                        PROCESSING_STATUSES));
    }

    private static Optional<String> validateInjectorRecipe(GTRecipe recipe) {
        if (recipe.itemInputs().size() > 2
                || recipe.itemOutputs().size() > 1
                || recipe.fluidInputs().size() > 2
                || recipe.fluidOutputs().size() > 1
                || recipe.eut() <= 0L
                || recipe.eut() > 8_192L
                || (recipe.itemInputs().isEmpty() && recipe.fluidInputs().isEmpty())
                || (recipe.itemOutputs().isEmpty() && recipe.fluidOutputs().isEmpty())) {
            return Optional.of("injector_recipe_shape");
        }
        if (recipe.fluidInputs().stream()
                        .anyMatch(stack -> stack.getAmount() > 4_000)
                || recipe.fluidOutputs().stream()
                        .anyMatch(stack -> stack.getAmount() > 4_000)) {
            return Optional.of("injector_recipe_amount");
        }
        return Optional.empty();
    }

    private static ProcessingMachineSpec nanofabSpec() {
        return new ProcessingMachineSpec(
                id("nanofab"),
                id("nanofab"),
                () -> ModRecipeMaps.NANOFAB,
                new ProcessingMachineSpec.SlotLayout(3, List.of(0, 1), List.of(2)),
                new ProcessingMachineSpec.TankLayout(
                        List.of(new ProcessingMachineSpec.TankSpec(0, 4_000)),
                        List.of(new ProcessingMachineSpec.TankSpec(1, 4_000))),
                new ProcessingMachineSpec.EnergySpec(
                        EnergyType.ELECTRIC,
                        ProcessingMachineSpec.EnergyMode.BUFFERED,
                        65_536L,
                        8_192L),
                Gt6SidedIo.policy("nanofab"),
                ModProcessingMachines::validateNanofabRecipe,
                ProcessingMachineSpec.BufferPolicy.PAUSE,
                Gt6BasicMachineGui.ui(
                        2, 1, 1, 1,
                        2, 1, 1, 1,
                        PROCESSING_STATUSES));
    }

    private static Optional<String> validateNanofabRecipe(GTRecipe recipe) {
        if (recipe.itemInputs().size() > 2
                || recipe.itemOutputs().size() > 1
                || recipe.fluidInputs().size() > 1
                || recipe.fluidOutputs().size() > 1
                || recipe.eut() <= 0L
                || recipe.eut() > 8_192L) {
            return Optional.of("nanofab_recipe_shape");
        }
        if (recipe.fluidInputs().stream()
                        .anyMatch(stack -> stack.getAmount() > 4_000)
                || recipe.fluidOutputs().stream()
                        .anyMatch(stack -> stack.getAmount() > 4_000)) {
            return Optional.of("nanofab_recipe_amount");
        }
        return Optional.empty();
    }

    private static ProcessingMachineSpec slicerSpec() {
        return new ProcessingMachineSpec(
                id("slicer"),
                id("slicer"),
                () -> ModRecipeMaps.SLICER,
                new ProcessingMachineSpec.SlotLayout(4, List.of(0, 1), List.of(2, 3)),
                new ProcessingMachineSpec.TankLayout(List.of(), List.of()),
                new ProcessingMachineSpec.EnergySpec(
                        EnergyType.ELECTRIC,
                        ProcessingMachineSpec.EnergyMode.BUFFERED,
                        65_536L,
                        8_192L),
                Gt6SidedIo.policy("slicer"),
                ModProcessingMachines::validateSlicerRecipe,
                ProcessingMachineSpec.BufferPolicy.PAUSE,
                Gt6BasicMachineGui.ui(
                        2, 2, 0, 0,
                        2, 2, 0, 0,
                        PROCESSING_STATUSES));
    }

    private static Optional<String> validateSlicerRecipe(GTRecipe recipe) {
        if (recipe.itemInputs().size() > 2
                || recipe.itemOutputs().size() > 2
                || !recipe.fluidInputs().isEmpty()
                || !recipe.fluidOutputs().isEmpty()
                || recipe.eut() <= 0L
                || recipe.eut() > 8_192L) {
            return Optional.of("slicer_recipe_shape");
        }
        return Optional.empty();
    }

    private static ProcessingMachineSpec squeezerSpec() {
        return new ProcessingMachineSpec(
                id("squeezer"),
                id("squeezer"),
                () -> ModRecipeMaps.SQUEEZER,
                new ProcessingMachineSpec.SlotLayout(
                        3, List.of(0), List.of(1, 2)),
                new ProcessingMachineSpec.TankLayout(
                        List.of(),
                        List.of(new ProcessingMachineSpec.TankSpec(0, 8_000))),
                new ProcessingMachineSpec.EnergySpec(
                        EnergyType.KINETIC_PUSH,
                        ProcessingMachineSpec.EnergyMode.BUFFERED,
                        32_768L,
                        4_096L),
                Gt6SidedIo.policy("squeezer"),
                ModProcessingMachines::validateSqueezerRecipe,
                ProcessingMachineSpec.BufferPolicy.PAUSE,
                Gt6BasicMachineGui.ui(
                        1, 2, 0, 1,
                        1, 2, 0, 1,
                        PROCESSING_STATUSES));
    }

    private static Optional<String> validateSqueezerRecipe(GTRecipe recipe) {
        if (recipe.itemInputs().size() > 1
                || recipe.itemOutputs().size() > 2
                || !recipe.fluidInputs().isEmpty()
                || recipe.fluidOutputs().size() > 1
                || recipe.eut() <= 0L
                || recipe.eut() > 4_096L) {
            return Optional.of("squeezer_recipe_shape");
        }
        if (recipe.fluidOutputs().stream()
                        .anyMatch(fluid -> fluid.getAmount() > 8_000)) {
            return Optional.of("squeezer_fluid_amount");
        }
        return Optional.empty();
    }

    private static ProcessingMachineSpec laserSpec() {
        return new ProcessingMachineSpec(
                id("laser_engraver"),
                id("laser_engraver"),
                () -> ModRecipeMaps.LASER_ENGRAVER,
                new ProcessingMachineSpec.SlotLayout(
                        3, List.of(0, 1), List.of(2)),
                new ProcessingMachineSpec.TankLayout(List.of(), List.of()),
                new ProcessingMachineSpec.EnergySpec(
                        EnergyType.LU,
                        ProcessingMachineSpec.EnergyMode.BUFFERED,
                        32_768L,
                        256L),
                Gt6SidedIo.policy("laser_engraver"),
                ModProcessingMachines::validateLaserRecipe,
                ProcessingMachineSpec.BufferPolicy.PAUSE,
                Gt6BasicMachineGui.ui(2, 1, 0, 0, 2, 1, 0, 0, PROCESSING_STATUSES));
    }

    private static ProcessingMachineSpec laserWelderSpec() {
        return new ProcessingMachineSpec(
                id("laser_welder"),
                id("welder"),
                () -> ModRecipeMaps.WELDER,
                new ProcessingMachineSpec.SlotLayout(
                        10,
                        List.of(0, 1, 2, 3, 4, 5, 6, 7, 8),
                        List.of(9)),
                new ProcessingMachineSpec.TankLayout(
                        List.of(new ProcessingMachineSpec.TankSpec(0, 4_000)),
                        List.of()),
                new ProcessingMachineSpec.EnergySpec(
                        EnergyType.LU,
                        ProcessingMachineSpec.EnergyMode.BUFFERED,
                        32_768L,
                        16_384L),
                Gt6SidedIo.policy("laser_welder"),
                ModProcessingMachines::validateLaserWelderRecipe,
                ProcessingMachineSpec.BufferPolicy.PAUSE,
                Gt6BasicMachineGui.ui(
                        9, 1, 1, 0,
                        9, 1, 1, 0,
                        PROCESSING_STATUSES));
    }

    private static Optional<String> validateLaserWelderRecipe(GTRecipe recipe) {
        if (recipe.itemInputs().size() > 9
                || recipe.itemOutputs().size() > 1
                || recipe.fluidInputs().size() > 1
                || !recipe.fluidOutputs().isEmpty()
                || recipe.eut() <= 0L
                || recipe.eut() > 16_384L) {
            return Optional.of("laser_welder_recipe_shape");
        }
        if (recipe.fluidInputs().stream()
                        .anyMatch(fluid -> fluid.getAmount() > 4_000)) {
            return Optional.of("laser_welder_fluid_amount");
        }
        return Optional.empty();
    }

    private static Optional<String> validateLaserRecipe(GTRecipe recipe) {
        if (recipe.itemInputs().size() != 2
                || recipe.itemOutputs().size() != 1
                || !recipe.fluidInputs().isEmpty()
                || !recipe.fluidOutputs().isEmpty()
                || (recipe.eut() != 16L && recipe.eut() != 256L)
                || recipe.duration() != 64) {
            return Optional.of("laser_recipe_shape");
        }
        if (recipe.itemInputCounts().size() != 2
                || recipe.itemInputCounts().get(0) <= 0
                || recipe.itemInputCounts().get(1) != 0
                || recipe.itemInputActions().get(0).kind()
                        != ItemInputAction.Kind.CONSUME
                || recipe.itemInputActions().get(1).kind()
                        != ItemInputAction.Kind.PRESERVE) {
            return Optional.of("laser_recipe_lens");
        }
        return Optional.empty();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, path);
    }
}
