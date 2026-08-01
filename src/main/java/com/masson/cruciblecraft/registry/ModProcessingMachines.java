package com.masson.cruciblecraft.registry;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;

import net.minecraft.resources.ResourceLocation;

/** Registry-safe processing specifications for currently implemented machines. */
public final class ModProcessingMachines {
    public static final ProcessingMachineSpec CRUSHER = new ProcessingMachineSpec(
            id("bronze_crusher"),
            id("crusher"),
            () -> ModRecipeMaps.CRUSHER,
            new ProcessingMachineSpec.SlotLayout(2, List.of(0), List.of(1)),
            new ProcessingMachineSpec.TankLayout(List.of(), List.of()),
            new ProcessingMachineSpec.EnergySpec(
                    EnergyType.KINETIC,
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
            new ProcessingMachineSpec.UiLayout(
                    List.of(
                            new ProcessingMachineSpec.SlotPosition(56, 35),
                            new ProcessingMachineSpec.SlotPosition(116, 35)),
                    new ProcessingMachineSpec.ProgressBar(79, 34, 24, 6),
                    List.of(),
                    List.of("idle", "invalid_recipe", "output_blocked", "underpowered")));
    public static final ProcessingMachineSpec SLUICE =
            mechanical("sluice", () -> ModRecipeMaps.SLUICE, true);
    public static final ProcessingMachineSpec BATH =
            mechanical("bath", () -> ModRecipeMaps.BATH, true);
    public static final ProcessingMachineSpec CENTRIFUGE =
            mechanical("centrifuge", () -> ModRecipeMaps.CENTRIFUGE, false);
    public static final ProcessingMachineSpec SHREDDER =
            mechanical("shredder", () -> ModRecipeMaps.SHREDDER, false);
    public static final ProcessingMachineSpec SIFTER =
            mechanical("sifter", () -> ModRecipeMaps.SIFTER, false);
    public static final ProcessingMachineSpec MORTAR =
            mechanical("mortar", () -> ModRecipeMaps.MORTAR, false);
    public static final ProcessingMachineSpec SMELTER =
            spec("smelter", () -> ModRecipeMaps.SMELTER, false, EnergyType.HEAT,
                    ProcessingMachineSpec.EnergyMode.ADJACENT);
    public static final List<ProcessingMachineSpec> T2_MACHINES = List.of(
            SLUICE, BATH, CENTRIFUGE, SHREDDER, SIFTER, SMELTER, MORTAR);
    public static final ProcessingMachineSpec EXTRUDER =
            t3("extruder", () -> ModRecipeMaps.EXTRUDER, 1, false);
    public static final ProcessingMachineSpec CUTTER =
            t3("cutter", () -> ModRecipeMaps.CUTTER, 1, false);
    public static final ProcessingMachineSpec LATHE =
            t3("lathe", () -> ModRecipeMaps.LATHE, 1, false);
    public static final ProcessingMachineSpec ROLLINGMILL =
            t3("rollingmill", () -> ModRecipeMaps.ROLLINGMILL, 1, false);
    public static final ProcessingMachineSpec ROLLBENDER =
            t3("rollbender", () -> ModRecipeMaps.ROLLBENDER, 1, false);
    public static final ProcessingMachineSpec WIREMILL =
            t3("wiremill", () -> ModRecipeMaps.WIREMILL, 2, false);
    public static final ProcessingMachineSpec BENDER =
            t3("bender", () -> ModRecipeMaps.BENDER, 1, false);
    public static final ProcessingMachineSpec ASSEMBLER =
            t3("assembler", () -> ModRecipeMaps.ASSEMBLER, 2, true);
    public static final ProcessingMachineSpec WELDER =
            t3("welder", () -> ModRecipeMaps.WELDER, 2, true);
    public static final ProcessingMachineSpec PRESS =
            t3("press", () -> ModRecipeMaps.PRESS, 2, false);
    public static final List<ProcessingMachineSpec> T3_MACHINES = List.of(
            EXTRUDER, CUTTER, LATHE, ROLLINGMILL, ROLLBENDER,
            WIREMILL, BENDER, ASSEMBLER, WELDER, PRESS);
    public static final int T3_EXPANSION_BUDGET = 10_000;
    public static final List<ProcessingMachineSpec> CONFIGURED_MACHINES =
            java.util.stream.Stream.concat(T2_MACHINES.stream(), T3_MACHINES.stream()).toList();
    private static final Map<ResourceLocation, ProcessingMachineSpec> BY_ID =
            CONFIGURED_MACHINES.stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                    ProcessingMachineSpec::id, value -> value));
    private static final Map<ResourceLocation, ProcessingMachineSpec> BY_MAP =
            java.util.stream.Stream.concat(
                    java.util.stream.Stream.of(CRUSHER),
                    CONFIGURED_MACHINES.stream())
                    .collect(java.util.stream.Collectors.toUnmodifiableMap(
                            ProcessingMachineSpec::recipeMapId, value -> value));

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
        return Optional.ofNullable(BY_MAP.get(id));
    }

    private static ProcessingMachineSpec mechanical(
            String id,
            Supplier<RecipeMap> map,
            boolean waterInput) {
        return spec(id, map, waterInput, EnergyType.KINETIC,
                ProcessingMachineSpec.EnergyMode.BUFFERED);
    }

    private static ProcessingMachineSpec spec(
            String path,
            Supplier<RecipeMap> map,
            boolean waterInput,
            EnergyType energyType,
            ProcessingMachineSpec.EnergyMode energyMode) {
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
                new ProcessingMachineSpec.UiLayout(
                        List.of(
                                new ProcessingMachineSpec.SlotPosition(38, 35),
                                new ProcessingMachineSpec.SlotPosition(98, 17),
                                new ProcessingMachineSpec.SlotPosition(116, 17),
                                new ProcessingMachineSpec.SlotPosition(98, 53),
                                new ProcessingMachineSpec.SlotPosition(116, 53)),
                        new ProcessingMachineSpec.ProgressBar(68, 34, 24, 6),
                        waterInput
                                ? List.of(new ProcessingMachineSpec.TankPosition(0, 20, 17, 12, 52))
                                : List.of(),
                        List.of("idle", "running", "invalid_recipe", "output_blocked", "underpowered")));
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
            boolean futureFluidInput) {
        List<Integer> inputs = java.util.stream.IntStream.range(0, itemInputs).boxed().toList();
        int outputSlot = itemInputs;
        var tanks = futureFluidInput
                ? new ProcessingMachineSpec.TankLayout(
                        List.of(new ProcessingMachineSpec.TankSpec(0, 4_000)), List.of())
                : new ProcessingMachineSpec.TankLayout(List.of(), List.of());
        List<ProcessingMachineSpec.SlotPosition> positions = itemInputs == 1
                ? List.of(
                        new ProcessingMachineSpec.SlotPosition(56, 35),
                        new ProcessingMachineSpec.SlotPosition(116, 35))
                : List.of(
                        new ProcessingMachineSpec.SlotPosition(38, 35),
                        new ProcessingMachineSpec.SlotPosition(56, 35),
                        new ProcessingMachineSpec.SlotPosition(116, 35));
        ProcessingMachineSpec spec = new ProcessingMachineSpec(
                id(path),
                id(path),
                map,
                new ProcessingMachineSpec.SlotLayout(
                        itemInputs + 1, inputs, List.of(outputSlot)),
                tanks,
                new ProcessingMachineSpec.EnergySpec(
                        EnergyType.KINETIC,
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
                recipe -> validateT3(recipe, itemInputs, futureFluidInput),
                ProcessingMachineSpec.BufferPolicy.PAUSE,
                new ProcessingMachineSpec.UiLayout(
                        positions,
                        new ProcessingMachineSpec.ProgressBar(79, 34, 24, 6),
                        futureFluidInput
                                ? List.of(new ProcessingMachineSpec.TankPosition(0, 20, 17, 12, 52))
                                : List.of(),
                        List.of("idle", "running", "invalid_recipe", "output_blocked", "underpowered")));
        return spec;
    }

    private static Optional<String> validateT3(
            GTRecipe recipe,
            int itemInputs,
            boolean fluidInput) {
        if (recipe.itemInputs().size() > itemInputs
                || recipe.itemOutputs().size() > 1
                || recipe.fluidInputs().size() > (fluidInput ? 1 : 0)
                || !recipe.fluidOutputs().isEmpty()) {
            return Optional.of("t3_recipe_shape");
        }
        if (recipe.eut() <= 0L || recipe.eut() > 256L || recipe.eut() > 4_096L) {
            return Optional.of("t3_recipe_energy");
        }
        if (recipe.itemInputCounts().stream().anyMatch(count -> count <= 0 || count > 64)
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

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, path);
    }
}
