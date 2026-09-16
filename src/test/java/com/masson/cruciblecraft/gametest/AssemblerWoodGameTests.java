package com.masson.cruciblecraft.gametest;

import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.compat.emi.ProcessingEmiRegistrationPlan;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineEnergyPlacement;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.recipe.gt.CompactGTRecipeFamilyDefinition;
import com.masson.cruciblecraft.recipe.gt.CompactPublicationGroups;
import com.masson.cruciblecraft.recipe.gt.CompactRecipeFamilyProvider;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.GTRecipeProvenance;
import com.masson.cruciblecraft.recipe.gt.GTRecipeQuery;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModProcessingMachines;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated assembler/wood compact-family runtime gate. Run with {@code -PwaveRecipes=assembler/wood}.
 */
@GameTestHolder(AssemblerWoodGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class AssemblerWoodGameTests {
    public static final String NAMESPACE = "cruciblecraft_wave_assembler_wood";
    private static final String TEMPLATE = "empty";
    private static final Direction FRONT = Direction.EAST;
    private static final int ASSEMBLER_WOOD_LOCKED_RELATIONS = 242;
    private static final int ASSEMBLER_PLANKS_RELATIONS = 85;
    private static final int ASSEMBLER_FIREPROOF_RELATIONS = 144;
    private static final int ASSEMBLER_PLANKS2_LIVE_RELATIONS = 13;
    private static final int ASSEMBLER_COMPACT_LOCKED_RELATIONS = 50;

    private AssemblerWoodGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void compactFamiliesPublished(GameTestHelper helper) {
        Set<ResourceLocation> published = assemblerWoodStableIds();
        helper.assertTrue(
                published.size() == ASSEMBLER_WOOD_LOCKED_RELATIONS,
                "Assembler is missing assembler/wood compact ids: " + published.size());
        RecipeMap.RecipeFamily planks = family(
                CompactPublicationGroups.ASSEMBLER_PLANKS);
        RecipeMap.RecipeFamily fireproof = family(
                CompactPublicationGroups.ASSEMBLER_FIREPROOF);
        RecipeMap.RecipeFamily planks2 = family(
                CompactPublicationGroups.ASSEMBLER_PLANKS2);
        int logical = logicalCount(planks) + logicalCount(fireproof) + logicalCount(planks2);
        helper.assertTrue(
                planks != null && fireproof != null && planks2 != null
                        && logicalCount(planks) == ASSEMBLER_PLANKS_RELATIONS
                        && logicalCount(fireproof) == ASSEMBLER_FIREPROOF_RELATIONS
                        && logicalCount(planks2) == ASSEMBLER_PLANKS2_LIVE_RELATIONS
                        && logical == ASSEMBLER_WOOD_LOCKED_RELATIONS,
                "Assembler assembler/wood compact families are not the 242 live relations: "
                        + logicalCount(planks)
                        + "/"
                        + logicalCount(fireproof)
                        + "/"
                        + logicalCount(planks2));
        helper.assertTrue(
                ModRecipeMaps.ASSEMBLER.entries().stream().noneMatch(
                        entry -> combinatorialAssembler(entry.recipe())),
                "Combinatorial assembler #0000/#0001 leaked into the live map");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void assemblerCompactFamiliesRemain(GameTestHelper helper) {
        Set<ResourceLocation> assemblerCompact = assemblerCompactStableIds();
        helper.assertTrue(
                assemblerCompact.size() == ASSEMBLER_COMPACT_LOCKED_RELATIONS,
                "assembler/wood stole assembler/compact assembler compact ids: "
                        + assemblerCompact.size());
        RecipeMap.RecipeFamily historical = ModRecipeMaps.ASSEMBLER
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.ASSEMBLER.id()))
                .orElse(null);
        helper.assertTrue(
                historical != null && historical.logicalRecipeCount() == ASSEMBLER_COMPACT_LOCKED_RELATIONS,
                "assembler/compact assembler compact family is not the original 50 relations: "
                        + (historical == null ? "missing" : historical.logicalRecipeCount()));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void assemblerExecutesRepresentative(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity assembler = place(
                helper,
                new BlockPos(3, 2, 3),
                ModBlocks.ASSEMBLER.get(),
                ModProcessingMachines.ASSEMBLER);
        RecipeMap.Entry representative = ModRecipeMaps.ASSEMBLER.entries().stream()
                .filter(entry -> entry.id().getPath().startsWith("assembler/wood/"))
                .filter(entry -> !entry.recipe().itemInputs().isEmpty())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing assembler/wood assembler recipe with item inputs"));
        fillEnergy(helper, assembler);
        loadRecipeInputs(assembler, representative.recipe());
        fillEnergy(helper, assembler);
        helper.startSequence()
                .thenExecuteFor(4, () -> fillEnergy(helper, assembler))
                .thenExecute(() -> {
                    helper.assertTrue(
                            assembler.workProgressLong() > 0L
                                    || assembler.duration() > 0
                                    || hasAnyOutput(assembler),
                            "assembler/wood representative assembler recipe was not selected: "
                                    + assembler.pausedReason());
                    forceLastTick(helper, assembler);
                    fillEnergy(helper, assembler);
                })
                .thenExecuteFor(2, () -> fillEnergy(helper, assembler))
                .thenExecute(() -> helper.assertTrue(
                        hasAnyOutput(assembler),
                        "Assembler did not produce any output: "
                                + assembler.pausedReason()))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void planksFamilyExecutes(GameTestHelper helper) {
        executeFirstOfGroup(
                helper,
                CompactPublicationGroups.ASSEMBLER_PLANKS,
                "assembler/wood planks assembler family");
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void fireproofFamilyExecutes(GameTestHelper helper) {
        executeFirstOfGroup(
                helper,
                CompactPublicationGroups.ASSEMBLER_FIREPROOF,
                "assembler/wood fireproof assembler family");
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void assemblerWoodStableIdsSurviveReload(GameTestHelper helper) {
        Set<ResourceLocation> first = assemblerWoodStableIds();
        helper.assertTrue(
                first.size() == ASSEMBLER_WOOD_LOCKED_RELATIONS,
                "assembler/wood locked ids missing before re-enumeration");
        RecipeMap.RecipeFamily planks = ModRecipeMaps.ASSEMBLER
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.ASSEMBLER.id(),
                        CompactPublicationGroups.ASSEMBLER_PLANKS))
                .orElseThrow();
        RecipeMap.RecipeFamily fireproof = ModRecipeMaps.ASSEMBLER
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.ASSEMBLER.id(),
                        CompactPublicationGroups.ASSEMBLER_FIREPROOF))
                .orElseThrow();
        RecipeMap.RecipeFamily planks2 = ModRecipeMaps.ASSEMBLER
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.ASSEMBLER.id(),
                        CompactPublicationGroups.ASSEMBLER_PLANKS2))
                .orElseThrow();
        helper.assertTrue(
                planks.epoch() == ModRecipeMaps.ASSEMBLER.runtimeEpoch()
                        && fireproof.epoch() == ModRecipeMaps.ASSEMBLER.runtimeEpoch()
                        && planks2.epoch() == ModRecipeMaps.ASSEMBLER.runtimeEpoch(),
                "assembler/wood compact family epochs drifted on the live map");
        Set<ResourceLocation> union = new TreeSet<>(planks.recipeIds());
        union.addAll(fireproof.recipeIds());
        union.addAll(planks2.recipeIds());
        helper.assertTrue(
                union.equals(first),
                "assembler/wood compact family recipe ids do not cover the stable id set");
        helper.assertTrue(
                assemblerCompactStableIds().size() == ASSEMBLER_COMPACT_LOCKED_RELATIONS,
                "assembler/compact assembler ids drifted after assembler/wood re-enumeration");
        helper.assertTrue(
                assemblerWoodStableIds().equals(first),
                "assembler/wood stable ids did not survive a second entries() enumeration");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void emiPlanIncludesAssemblerFamilies(GameTestHelper helper) {
        Set<ResourceLocation> published = assemblerWoodStableIds();
        ProcessingEmiRegistrationPlan plan = ProcessingEmiRegistrationPlan.create(
                ModProcessingMachines.CONFIGURED_MACHINES);
        Set<ResourceLocation> assemblerEmi = plan.recipes().stream()
                .filter(recipe -> recipe.machine().recipeMap().id()
                        .equals(ModRecipeMaps.ASSEMBLER.id()))
                .map(ProcessingEmiRegistrationPlan.RecipeRegistration::id)
                .collect(Collectors.toCollection(TreeSet::new));
        helper.assertTrue(
                assemblerEmi.containsAll(published),
                "EMI assembler category is missing assembler/wood recipe ids: "
                        + published.stream()
                                .filter(id -> !assemblerEmi.contains(id))
                                .toList());
        helper.assertTrue(
                assemblerEmi.containsAll(assemblerCompactStableIds()),
                "EMI assembler category dropped assembler/compact recipe ids");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void rejectsNonMatchingInputs(GameTestHelper helper) {
        Optional<RecipeMap.Match> dirt = ModRecipeMaps.ASSEMBLER.findMatch(
                GTRecipeQuery.items(new ItemStack(Items.DIRT)));
        helper.assertTrue(
                dirt.isEmpty()
                        || !dirt.orElseThrow().id().getPath().startsWith("assembler/wood/"),
                "Assembler matched a assembler/wood compact recipe from dirt-only input");
        helper.succeed();
    }

    private static void executeFirstOfGroup(
            GameTestHelper helper,
            ResourceLocation publicationGroup,
            String label) {
        ConfiguredProcessingMachineBlockEntity assembler = place(
                helper,
                new BlockPos(3, 2, 3),
                ModBlocks.ASSEMBLER.get(),
                ModProcessingMachines.ASSEMBLER);
        RecipeMap.RecipeFamily group = family(publicationGroup);
        helper.assertTrue(group != null && group.logicalRecipeCount() > 0, label + " is missing");
        RecipeMap.Entry first = ModRecipeMaps.ASSEMBLER
                .entry(group.recipeIds().getFirst())
                .orElseThrow(() -> new IllegalStateException(
                        "Missing first " + label + " recipe"));
        fillEnergy(helper, assembler);
        loadRecipeInputs(assembler, first.recipe());
        fillEnergy(helper, assembler);
        helper.startSequence()
                .thenExecuteFor(4, () -> fillEnergy(helper, assembler))
                .thenExecute(() -> {
                    helper.assertTrue(
                            assembler.workProgressLong() > 0L
                                    || assembler.duration() > 0
                                    || hasAnyOutput(assembler),
                            label + " recipe was not selected: "
                                    + assembler.pausedReason());
                    forceLastTick(helper, assembler);
                    fillEnergy(helper, assembler);
                })
                .thenExecuteFor(2, () -> fillEnergy(helper, assembler))
                .thenExecute(() -> helper.assertTrue(
                        hasAnyOutput(assembler),
                        label + " did not produce output: "
                                + assembler.pausedReason()))
                .thenSucceed();
    }

    private static RecipeMap.RecipeFamily family(ResourceLocation publicationGroup) {
        return ModRecipeMaps.ASSEMBLER
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.ASSEMBLER.id(),
                        publicationGroup))
                .orElse(null);
    }

    private static int logicalCount(RecipeMap.RecipeFamily family) {
        return family == null ? 0 : family.logicalRecipeCount();
    }

    private static boolean combinatorialAssembler(GTRecipe recipe) {
        return recipe.provenance()
                .flatMap(GTRecipeProvenance::selectedSourceRecipe)
                .filter(id -> "gt.recipe.assembler#0000".equals(id)
                        || "gt.recipe.assembler#0001".equals(id))
                .isPresent();
    }

    private static Set<ResourceLocation> assemblerWoodStableIds() {
        return ModRecipeMaps.ASSEMBLER.entries().stream()
                .map(RecipeMap.Entry::id)
                .filter(id -> id.getPath().startsWith("assembler/wood/"))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static Set<ResourceLocation> assemblerCompactStableIds() {
        return ModRecipeMaps.ASSEMBLER.entries().stream()
                .map(RecipeMap.Entry::id)
                .filter(id -> id.getPath().startsWith("assembler/compact/"))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static ConfiguredProcessingMachineBlockEntity place(
            GameTestHelper helper,
            BlockPos pos,
            Block block,
            ProcessingMachineSpec spec) {
        helper.setBlock(
                pos,
                block.defaultBlockState().setValue(
                        ProcessingMachineBlock.FACING, FRONT));
        ConfiguredProcessingMachineBlockEntity machine = helper.getBlockEntity(pos);
        helper.assertTrue(
                machine.spec() == spec
                        || machine.variant().kind().behavior() == spec,
                "Placed block resolved wrong machine kind");
        return machine;
    }

    private static void loadRecipeInputs(
            ConfiguredProcessingMachineBlockEntity machine, GTRecipe recipe) {
        for (int i = 0; i < recipe.itemInputs().size(); i++) {
            ItemStack sample = recipe.itemInputs().get(i).getItems()[0].copy();
            sample.setCount(Math.max(1, recipe.itemInputCounts().get(i)));
            machine.inventory().setStackInSlot(
                    machine.spec().items().inputs().get(i), sample);
        }
        for (int i = 0; i < recipe.fluidInputs().size(); i++) {
            machine.tanks().get(machine.spec().fluids().inputs().get(i).index())
                    .setFluid(recipe.fluidInputs().get(i).copy());
        }
    }

    private static void forceLastTick(
            GameTestHelper helper,
            ConfiguredProcessingMachineBlockEntity machine) {
        helper.assertTrue(
                machine.duration() > 0,
                machine.spec().id() + " has no selected recipe: "
                        + machine.pausedReason());
        machine.runtime().processor().setProgress(machine.duration() - 1);
    }

    private static boolean hasAnyOutput(
            ConfiguredProcessingMachineBlockEntity machine) {
        return machine.spec().items().outputs().stream().anyMatch(slot ->
                !machine.inventory().getStackInSlot(slot).isEmpty())
                || machine.spec().fluids().outputs().stream().anyMatch(tank ->
                !machine.tanks().get(tank.index()).getFluid().isEmpty());
    }

    private static void fillEnergy(
            GameTestHelper helper, ConfiguredProcessingMachineBlockEntity machine) {
        EnergyType type = machine.spec().energy().type();
        Direction front = machine.getBlockState().getValue(ProcessingMachineBlock.FACING);
        Direction back = ProcessingMachineEnergyPlacement
                .connection(machine.spec(), front)
                .providerOffset();
        long packet = Math.max(1L, machine.spec().energy().maxPacket());
        long target = machine.capacity(type);
        Direction side = back;
        if (machine.stored(type) <= 0L) {
            long accepted = machine.insert(type, packet, 1L, back, false);
            if (accepted <= 0L) {
                for (Direction candidate : Direction.values()) {
                    if (candidate == back) {
                        continue;
                    }
                    accepted = machine.insert(type, packet, 1L, candidate, false);
                    if (accepted > 0L) {
                        side = candidate;
                        break;
                    }
                }
            }
        }
        for (int i = 0; i < 16 && machine.stored(type) < target; i++) {
            if (machine.insert(type, packet, 1L, side, false) <= 0L) {
                break;
            }
        }
        helper.assertTrue(
                machine.stored(type) > 0L,
                "Placed machine stored no "
                        + type
                        + " on "
                        + side
                        + ": "
                        + machine.spec().id());
    }
}
