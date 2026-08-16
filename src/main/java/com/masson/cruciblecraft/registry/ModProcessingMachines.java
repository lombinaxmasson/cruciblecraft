package com.masson.cruciblecraft.registry;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.content.item.ExtruderShapeCatalog;
import com.masson.cruciblecraft.machine.processing.CraftingCatalystPolicy;
import com.masson.cruciblecraft.machine.processing.Gt6BasicMachineGui;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
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
            new ProcessingMachineSpec.SidedIoPolicy(
                    (front, side) -> front != null && side == front
                            ? ProcessingMachineSpec.CapabilityAccess.OUTPUT
                            : ProcessingMachineSpec.CapabilityAccess.INPUT,
                    (front, side) -> ProcessingMachineSpec.CapabilityAccess.NONE,
                    (front, side) -> front != null && side == front.getOpposite()
                            ? ProcessingMachineSpec.CapabilityAccess.INPUT
                            : ProcessingMachineSpec.CapabilityAccess.NONE),
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
            reusedT5("bath", () -> ModRecipeMaps.BATH,
                    1, 4, 1, 1, 4_000, 8_000,
                    6, 6, 1, 3,
                    EnergyType.KINETIC,
                    ProcessingMachineSpec.EnergyMode.BUFFERED);
    public static final ProcessingMachineSpec CENTRIFUGE =
            reusedT5("centrifuge", () -> ModRecipeMaps.CENTRIFUGE,
                    1, 6, 1, 2, 4_000, 8_000,
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
            reusedT5("smelter", () -> ModRecipeMaps.SMELTER,
                    1, 4, 0, 1, 4_000, 8_000,
                    1, 1, 1, 1,
                    EnergyType.HEAT,
                    ProcessingMachineSpec.EnergyMode.ADJACENT);
    public static final List<ProcessingMachineSpec> T2_MACHINES = List.of(
            SLUICE, BATH, CENTRIFUGE, SHREDDER, SIFTER, SMELTER, MORTAR);
    public static final ProcessingMachineSpec EXTRUDER =
            t3(
                    "extruder",
                    () -> ModRecipeMaps.EXTRUDER,
                    2,
                    false,
                    true,
                    EnergyType.KINETIC,
                    2, 2, 0, 0);
    public static final ProcessingMachineSpec CUTTER =
            t3(
                    "cutter",
                    () -> ModRecipeMaps.CUTTER,
                    1,
                    false,
                    false,
                    EnergyType.KINETIC,
                    1, 3, 1, 0);
    public static final ProcessingMachineSpec LATHE =
            t3(
                    "lathe",
                    () -> ModRecipeMaps.LATHE,
                    1,
                    false,
                    false,
                    EnergyType.KINETIC_ROTATION,
                    1, 2, 0, 0);
    public static final ProcessingMachineSpec ROLLINGMILL =
            t3(
                    "rollingmill",
                    () -> ModRecipeMaps.ROLLINGMILL,
                    1,
                    false,
                    false,
                    EnergyType.KINETIC_ROTATION,
                    1, 1, 0, 0);
    public static final ProcessingMachineSpec ROLLBENDER =
            t3(
                    "rollbender",
                    () -> ModRecipeMaps.ROLLBENDER,
                    1,
                    false,
                    false,
                    EnergyType.KINETIC,
                    1, 1, 0, 0);
    public static final ProcessingMachineSpec WIREMILL =
            t3(
                    "wiremill",
                    () -> ModRecipeMaps.WIREMILL,
                    2,
                    false,
                    false,
                    EnergyType.KINETIC_ROTATION,
                    1, 1, 0, 0);
    public static final ProcessingMachineSpec BENDER =
            t3(
                    "bender",
                    () -> ModRecipeMaps.BENDER,
                    1,
                    false,
                    false,
                    EnergyType.KINETIC,
                    1, 1, 0, 0);
    public static final ProcessingMachineSpec ASSEMBLER =
            t3(
                    "assembler",
                    () -> ModRecipeMaps.ASSEMBLER,
                    6,
                    true,
                    false,
                    EnergyType.KINETIC,
                    2, 1, 1, 0);
    public static final ProcessingMachineSpec WELDER =
            t3(
                    "welder",
                    () -> ModRecipeMaps.WELDER,
                    2,
                    true,
                    false,
                    EnergyType.KINETIC,
                    9, 1, 1, 0);
    public static final ProcessingMachineSpec PRESS =
            t3(
                    "press",
                    () -> ModRecipeMaps.PRESS,
                    2,
                    false,
                    false,
                    EnergyType.KINETIC_PUSH,
                    3, 1, 0, 0);
    public static final List<ProcessingMachineSpec> T3_MACHINES = List.of(
            EXTRUDER, CUTTER, LATHE, ROLLINGMILL, ROLLBENDER,
            WIREMILL, BENDER, ASSEMBLER, WELDER, PRESS);
    public static final ProcessingMachineSpec ELECTROLYZER =
            t5("electrolyzer", () -> ModRecipeMaps.ELECTROLYZER,
                    2, 6, 2, 3, 16_000, 16_000,
                    2, 6, 2, 6,
                    EnergyType.ELECTRIC);
    public static final ProcessingMachineSpec MIXER =
            t5("mixer", () -> ModRecipeMaps.MIXER,
                    4, 1, 3, 2, 32_000, 32_000,
                    6, 1, 6, 2,
                    EnergyType.ELECTRIC);
    public static final ProcessingMachineSpec DISTILLERY =
            reusedT5("distillery", () -> ModRecipeMaps.DISTILLERY,
                    2, 2, 2, 3, 8_000, 8_000,
                    1, 2, 1, 2,
                    EnergyType.HEAT,
                    ProcessingMachineSpec.EnergyMode.ADJACENT);
    public static final ProcessingMachineSpec AUTOCLAVE =
            t5("autoclave", () -> ModRecipeMaps.AUTOCLAVE, 2, 3, 1, 1,
                    2_500_000, 16_000,
                    2, 3, 1, 1,
                    EnergyType.ELECTRIC);
    public static final ProcessingMachineSpec DRYING =
            reusedT5("drying", () -> ModRecipeMaps.DRYING,
                    1, 1, 0, 1, 32_000, 32_000,
                    1, 1, 1, 3,
                    EnergyType.HEAT,
                    ProcessingMachineSpec.EnergyMode.ADJACENT);
    public static final ProcessingMachineSpec COMPRESSOR =
            t5("compressor", () -> ModRecipeMaps.COMPRESSOR, 1, 1, 0, 0,
                    32_000, 32_000,
                    1, 1, 0, 0,
                    EnergyType.ELECTRIC);
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
                            EnergyType.ELECTRIC,
                            ProcessingMachineSpec.EnergyMode.ADJACENT,
                            0L,
                            1L),
                    new ProcessingMachineSpec.SidedIoPolicy(
                            (front, side) ->
                                    ProcessingMachineSpec.CapabilityAccess.NONE,
                            (front, side) -> side == null
                                    ? ProcessingMachineSpec.CapabilityAccess.NONE
                                    : side == front
                                            ? ProcessingMachineSpec
                                                    .CapabilityAccess.OUTPUT
                                            : ProcessingMachineSpec
                                                    .CapabilityAccess.INPUT,
                            (front, side) ->
                                    ProcessingMachineSpec.CapabilityAccess.NONE),
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
    public static final List<ProcessingMachineSpec> T11_PROCESSING_MACHINES =
            List.of(GENERIFIER);
    /** T5 can publish into reused maps as well as its new dedicated maps. */
    public static final List<ProcessingMachineSpec> T5_MACHINES = List.of(
            BATH, CENTRIFUGE, SMELTER, ASSEMBLER,
            ELECTROLYZER, MIXER, DISTILLERY, AUTOCLAVE, DRYING, COMPRESSOR);
    /** New T5 maps which intentionally have no pre-T5 recipe population. */
    public static final List<ProcessingMachineSpec> T5_DEDICATED_MACHINES = List.of(
            ELECTROLYZER, MIXER, DISTILLERY, AUTOCLAVE, DRYING, COMPRESSOR);
    /** Original T3 component envelope: 5,352 non-extruder + 4,648 extruder. */
    public static final int T3_COMPONENT_EXPANSION_BUDGET = 10_000;
    /** T4 tool rules are budgeted separately from the closed T3 content set. */
    public static final int T4_TOOL_EXPANSION_BUDGET = 4_000;
    /**
     * Live-map policy envelope for the complete flattened T4 projection.
     * The exact policy projects 8,141 T3 + 3,452 T4 = 11,593 recipes. A 10%
     * margin rounded to the next thousand gives 13,000 and remains below the
     * measured reload and index budgets below.
     */
    public static final int LIVE_T3_MAP_RECIPE_BUDGET = 13_000;
    /**
     * The pinned projection publishes 145 T5 recipes. This final gate keeps
     * measured growth room without weakening the frozen T3/T4 budgets.
     */
    public static final int T5_CHEMICAL_RECIPE_BUDGET = 200;
    /**
     * T7 publishes 220 tag-driven mortar expansions. This independent envelope
     * prevents authored material facts from consuming the global margin
     * unnoticed.
     */
    public static final int T7_AUTHORED_MATERIAL_RULE_BUDGET = 256;
    /**
     * T8 publishes 257 source-backed fluid/item pipe material-rule expansions.
     */
    public static final int T8_PIPE_MATERIAL_RULE_BUDGET = 320;
    /**
     * T10's known hot- and multi-ingot routes project 1,288 expansions. This
     * provisional T10a envelope applies the established 10% margin and rounds
     * up to the next hundred. Container routes remain design-required and must
     * trigger an explicit re-projection rather than consume assumed headroom.
     */
    public static final int T10_AUTHORED_MATERIAL_RULE_BUDGET = 1_500;
    /**
     * T11 is a fixed-row fluid projection. Material-wide authored expansion is
     * forbidden so a source-row route cannot silently turn into a guessed
     * composition rule.
     */
    public static final int T11_AUTHORED_MATERIAL_RULE_BUDGET = 0;
    /**
     * T12 tiers bind machine kinds to source-backed numeric profiles. They do
     * not publish recipes or authorize material-wide recipe expansion.
     */
    public static final int T12_AUTHORED_MATERIAL_RULE_BUDGET = 0;
    /** T21 gunpowder Mixer family: 4 authored entries. Budget 8 allows doubling. */
    public static final int T21_AUTHORED_MATERIAL_RULE_BUDGET = 8;
    /** T22 petroleum: 3 authored recipes (distillery, generifier, fuels_engine). */
    public static final int T22_AUTHORED_MATERIAL_RULE_BUDGET = 3;
    public static final Map<Integer, Integer> AUTHORED_MATERIAL_RULE_BUDGETS =
            Map.of(
                    7, T7_AUTHORED_MATERIAL_RULE_BUDGET,
                    8, T8_PIPE_MATERIAL_RULE_BUDGET,
                    10, T10_AUTHORED_MATERIAL_RULE_BUDGET,
                    11, T11_AUTHORED_MATERIAL_RULE_BUDGET,
                    12, T12_AUTHORED_MATERIAL_RULE_BUDGET,
                    21, T21_AUTHORED_MATERIAL_RULE_BUDGET,
                    22, T22_AUTHORED_MATERIAL_RULE_BUDGET);
    /**
     * The verified post-T8 publication is 17,583 after adding the high-version
     * ore-block crusher ingress. The pinned T10 preflight projects 642
     * hot-ingot and 646 multi-ingot routes, for 18,871 known
     * recipes. Applying the established 10% margin and rounding up to the next
     * thousand yields 21,000.
     */
    public static final int ALL_PUBLISHED_RECIPE_BUDGET = 21_000;
    /** T14 Hybrid soft envelope for recipes retained eagerly in map indexes. */
    public static final int ALL_EAGER_PUBLICATION_SOFT_BUDGET = 18_000;
    /** T14 measured 20x hard envelope for logical rows kept by lazy families. */
    public static final int ALL_LAZY_LOGICAL_RECIPE_HARD_CEILING = 56_000;
    /** Aggregate per-epoch cache ceiling for currently selected lazy families. */
    public static final int ALL_LAZY_RECIPE_CACHE_HARD_CEILING = 4_096;
    public static final long RECIPE_RELOAD_BUDGET_MS = 10_000L;
    public static final long RECIPE_INDEX_BUILD_BUDGET_MS = 1_000L;
    public static final long CLIENT_RECIPE_RELOAD_BUDGET_MS = 10_000L;
    public static final long CLIENT_RECIPE_INDEX_BUILD_BUDGET_MS = 3_000L;
    public static final long RECIPE_SYNC_BUDGET_BYTES =
            64L * 1_024L * 1_024L;
    public static final long RECIPE_LOOKUP_P95_BUDGET_NS = 2_000_000L;
    public static final long RECIPE_LOOKUP_P95_CANDIDATE_BUDGET = 64L;
    public static final long RECIPE_LOOKUP_MAX_CANDIDATE_HARD_CEILING = 128L;
    public static final List<ProcessingMachineSpec> CONFIGURED_MACHINES =
            java.util.stream.Stream.of(
                            T2_MACHINES,
                            T3_MACHINES,
                            T5_MACHINES,
                            T11_PROCESSING_MACHINES)
                    .flatMap(List::stream)
                    .distinct()
                    .toList();
    private static final Map<ResourceLocation, ProcessingMachineSpec> BY_ID =
            CONFIGURED_MACHINES.stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                    ProcessingMachineSpec::id, value -> value));
    private static final Map<ResourceLocation, List<ProcessingMachineSpec>>
            BY_MAP =
            java.util.stream.Stream.concat(
                    java.util.stream.Stream.of(CRUSHER),
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
                new ProcessingMachineSpec.SidedIoPolicy(
                        (front, side) -> side == null
                                ? ProcessingMachineSpec.CapabilityAccess.NONE
                                : side == front
                                        ? ProcessingMachineSpec.CapabilityAccess.OUTPUT
                                        : ProcessingMachineSpec.CapabilityAccess.INPUT,
                        (front, side) -> waterInput && side != null && side != front
                                ? ProcessingMachineSpec.CapabilityAccess.INPUT
                                : ProcessingMachineSpec.CapabilityAccess.NONE,
                        (front, side) -> energyMode == ProcessingMachineSpec.EnergyMode.BUFFERED
                                && side != null && side == front.getOpposite()
                                        ? ProcessingMachineSpec.CapabilityAccess.INPUT
                                        : ProcessingMachineSpec.CapabilityAccess.NONE),
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

    private static ProcessingMachineSpec t3(
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
                new ProcessingMachineSpec.SidedIoPolicy(
                        (front, side) -> side == null
                                ? ProcessingMachineSpec.CapabilityAccess.NONE
                                : side == front
                                        ? ProcessingMachineSpec.CapabilityAccess.OUTPUT
                                        : ProcessingMachineSpec.CapabilityAccess.INPUT,
                        (front, side) -> futureFluidInput && side != null && side != front
                                ? ProcessingMachineSpec.CapabilityAccess.INPUT
                                : ProcessingMachineSpec.CapabilityAccess.NONE,
                        (front, side) -> side != null && side == front.getOpposite()
                                ? ProcessingMachineSpec.CapabilityAccess.INPUT
                                : ProcessingMachineSpec.CapabilityAccess.NONE),
                recipe -> validateT3(
                        recipe,
                        itemInputs,
                        futureFluidInput,
                        extruderTool,
                        assemblerCatalysts),
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

    private static Optional<String> validateT3(
            GTRecipe recipe,
            int itemInputs,
            boolean fluidInput,
            boolean extruderTool,
            boolean assemblerCatalysts) {
        if (recipe.itemInputs().size() > itemInputs
                || recipe.itemOutputs().size() > 1
                || recipe.fluidInputs().size() > (fluidInput ? 1 : 0)
                || !recipe.fluidOutputs().isEmpty()) {
            return Optional.of("t3_recipe_shape");
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
            return Optional.of("t3_extruder_tool_shape");
        }
        if (assemblerCatalysts) {
            int materialInputs = 0;
            int catalystInputs = 0;
            int preservedPatterns = 0;
            for (int index = 0; index < recipe.itemInputActions().size(); index++) {
                ItemInputAction action = recipe.itemInputActions().get(index);
                if (!CraftingCatalystPolicy.acceptsIngredient(
                        recipe.itemInputs().get(index), action.kind())) {
                    return Optional.of("t3_assembler_catalyst_shape");
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
                return Optional.of("t3_assembler_catalyst_shape");
            }
        } else if (recipe.itemInputActions().stream()
                .anyMatch(action -> action.kind() == ItemInputAction.Kind.WEAR)) {
            return Optional.of("t4_wear_tool_shape");
        }
        // The 4,096-unit buffer is storage; the real per-recipe maxPacket policy is 256.
        if (recipe.eut() <= 0L || recipe.eut() > 256L) {
            return Optional.of("t3_recipe_energy");
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
                                        != ItemInputAction.Kind.CONSUME))
                || recipe.itemOutputs().stream().anyMatch(stack ->
                        stack.isEmpty() || stack.getCount() <= 0)
                || recipe.outputChances().stream().anyMatch(
                        chance -> chance <= 0 || chance > GTRecipe.GUARANTEED_CHANCE)
                || recipe.fluidInputs().stream().anyMatch(
                        stack -> stack.isEmpty() || stack.getAmount() > 4_000)) {
            return Optional.of("t3_recipe_amount");
        }
        return Optional.empty();
    }

    private static ProcessingMachineSpec reusedT5(
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
        ProcessingMachineSpec layout = t5(
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
                new ProcessingMachineSpec.SidedIoPolicy(
                        layout.sidedIo().items(),
                        layout.sidedIo().fluids(),
                        (front, side) -> energyMode
                                        == ProcessingMachineSpec.EnergyMode.BUFFERED
                                && side != null
                                && side == front.getOpposite()
                                        ? ProcessingMachineSpec.CapabilityAccess.INPUT
                                        : ProcessingMachineSpec.CapabilityAccess.NONE),
                recipe -> validateT5(
                        recipe,
                        itemInputs,
                        itemOutputs,
                        fluidInputs,
                        fluidOutputs,
                        fluidInputCapacity,
                        fluidOutputCapacity),
                layout.buffering(),
                layout.ui());
    }

    private static ProcessingMachineSpec t5(
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
        List<Integer> inputSlots =
                java.util.stream.IntStream.range(0, itemInputs).boxed().toList();
        List<Integer> outputSlots = java.util.stream.IntStream
                .range(itemInputs, itemInputs + itemOutputs)
                .boxed()
                .toList();
        List<ProcessingMachineSpec.TankSpec> inputTanks = java.util.stream.IntStream
                .range(0, fluidInputs)
                .mapToObj(index ->
                        new ProcessingMachineSpec.TankSpec(index, fluidInputCapacity))
                .toList();
        List<ProcessingMachineSpec.TankSpec> outputTanks = java.util.stream.IntStream
                .range(fluidInputs, fluidInputs + fluidOutputs)
                .mapToObj(index ->
                        new ProcessingMachineSpec.TankSpec(index, fluidOutputCapacity))
                .toList();
        return new ProcessingMachineSpec(
                id(path),
                id(path),
                map,
                new ProcessingMachineSpec.SlotLayout(
                        itemInputs + itemOutputs, inputSlots, outputSlots),
                new ProcessingMachineSpec.TankLayout(inputTanks, outputTanks),
                new ProcessingMachineSpec.EnergySpec(
                        energyType,
                        ProcessingMachineSpec.EnergyMode.BUFFERED,
                        65_536L,
                        1_024L),
                new ProcessingMachineSpec.SidedIoPolicy(
                        (front, side) -> side == null
                                ? ProcessingMachineSpec.CapabilityAccess.NONE
                                : side == front
                                        ? ProcessingMachineSpec.CapabilityAccess.OUTPUT
                                        : ProcessingMachineSpec.CapabilityAccess.INPUT,
                        (front, side) -> side == null
                                ? ProcessingMachineSpec.CapabilityAccess.NONE
                                : side == front && fluidOutputs > 0
                                        ? ProcessingMachineSpec.CapabilityAccess.OUTPUT
                                        : side != front && fluidInputs > 0
                                                ? ProcessingMachineSpec.CapabilityAccess.INPUT
                                                : ProcessingMachineSpec.CapabilityAccess.NONE,
                        (front, side) -> side != null && side == front.getOpposite()
                                ? ProcessingMachineSpec.CapabilityAccess.INPUT
                                : ProcessingMachineSpec.CapabilityAccess.NONE),
                recipe -> validateT5(
                        recipe,
                        itemInputs,
                        itemOutputs,
                        fluidInputs,
                        fluidOutputs,
                        fluidInputCapacity,
                        fluidOutputCapacity),
                ProcessingMachineSpec.BufferPolicy.PAUSE,
                Gt6BasicMachineGui.ui(
                        gt6InItems,
                        gt6OutItems,
                        gt6InFluids,
                        gt6OutFluids,
                        itemInputs,
                        itemOutputs,
                        fluidInputs,
                        fluidOutputs,
                        PROCESSING_STATUSES));
    }

    private static Optional<String> validateT5(
            GTRecipe recipe,
            int itemInputs,
            int itemOutputs,
            int fluidInputs,
            int fluidOutputs,
            int fluidInputCapacity,
            int fluidOutputCapacity) {
        if (recipe.itemInputs().size() > itemInputs
                || recipe.itemOutputs().size() > itemOutputs
                || recipe.fluidInputs().size() > fluidInputs
                || recipe.fluidOutputs().size() > fluidOutputs) {
            return Optional.of("t5_recipe_shape");
        }
        if (recipe.itemInputActions().stream()
                .anyMatch(action -> action.kind() != ItemInputAction.Kind.CONSUME)) {
            return Optional.of("t5_recipe_input_action");
        }
        if (recipe.eut() <= 0L || recipe.eut() > 1_024L) {
            return Optional.of("t5_recipe_energy");
        }
        if (recipe.itemInputCounts().stream().anyMatch(count -> count <= 0 || count > 64)
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
            return Optional.of("t5_recipe_amount");
        }
        return Optional.empty();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, path);
    }
}
