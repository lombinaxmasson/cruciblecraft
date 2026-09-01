package com.masson.cruciblecraft.gametest;

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
import com.masson.cruciblecraft.recipe.gt.CompactRecipeFamilyProvider;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.GTRecipeProvenance;
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
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated T40 compact-family runtime gate. Run with {@code -Pt40Recipes}.
 */
@GameTestHolder(T40RecipeGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class T40RecipeGameTests {
    public static final String NAMESPACE = "cruciblecraft_t40";
    private static final String TEMPLATE = "empty";
    private static final Direction FRONT = Direction.EAST;

    private T40RecipeGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void t40CompactFamiliesPublished(GameTestHelper helper) {
        Set<ResourceLocation> published = t40StableIds();
        helper.assertTrue(
                published.size() == 22,
                "Electrolyzer is missing T40 compact ids: " + published.size());
        RecipeMap.RecipeFamily singletonFamily = ModRecipeMaps.ELECTROLYZER
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.ELECTROLYZER.id(),
                        CompactGTRecipeFamilyDefinition
                                .T40_ELECTROLYZER_SINGLETON_PUBLICATION_GROUP))
                .orElse(null);
        RecipeMap.RecipeFamily multiFamily = ModRecipeMaps.ELECTROLYZER
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.ELECTROLYZER.id(),
                        CompactGTRecipeFamilyDefinition
                                .T40_ELECTROLYZER_MULTI_PUBLICATION_GROUP))
                .orElse(null);
        helper.assertTrue(
                singletonFamily != null
                        && singletonFamily.logicalRecipeCount() == 11,
                "Electrolyzer singleton compact family is not the 11 locked relations: "
                        + (singletonFamily == null
                                ? "missing"
                                : singletonFamily.logicalRecipeCount()));
        helper.assertTrue(
                multiFamily != null && multiFamily.logicalRecipeCount() == 11,
                "Electrolyzer multi compact family is not the 11 locked relations: "
                        + (multiFamily == null
                                ? "missing"
                                : multiFamily.logicalRecipeCount()));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void t40LockedSupportPublished(GameTestHelper helper) {
        long gtSupport = ModRecipeMaps.ALL.stream()
                .flatMap(map -> map.entries().stream())
                .filter(entry -> entry.id().getPath()
                        .startsWith("t40_player_path_support/"))
                .count();
        long craftingSupport = helper.getLevel().getRecipeManager()
                .getAllRecipesFor(RecipeType.CRAFTING)
                .stream()
                .filter(holder -> holder.id().getPath()
                        .startsWith("t40_player_path_support/"))
                .count();
        helper.assertTrue(
                gtSupport + craftingSupport == 0,
                "Locked T40 support is not the 0 production-lock routes: "
                        + gtSupport
                        + " gt + "
                        + craftingSupport
                        + " crafting");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void t40ElectrolyzerExecutesRepresentative(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity electrolyzer = place(
                helper,
                new BlockPos(3, 2, 3),
                ModBlocks.STAINLESS_STEEL_ELECTROLYZER.get(),
                ModProcessingMachines.ELECTROLYZER);
        helper.assertTrue(
                electrolyzer.spec().energy().type() == EnergyType.ELECTRIC
                        && electrolyzer.spec().energy().mode()
                                == ProcessingMachineSpec.EnergyMode.BUFFERED,
                "Stainless steel electrolyzer is not a buffered EU host");
        RecipeMap.Entry representative = ModRecipeMaps.ELECTROLYZER.entries().stream()
                .filter(entry -> entry.id().getPath().startsWith("t40/"))
                .filter(entry -> !entry.recipe().itemInputs().isEmpty()
                        && entry.recipe().fluidInputs().isEmpty())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing T40 electrolyzer recipe with item-only inputs"));
        fillEnergy(helper, electrolyzer);
        loadRecipeInputs(electrolyzer, representative.recipe());
        fillEnergy(helper, electrolyzer);
        helper.startSequence()
                .thenExecuteFor(4, () -> fillEnergy(helper, electrolyzer))
                .thenExecute(() -> {
                    helper.assertTrue(
                            electrolyzer.workProgressLong() > 0L
                                    || electrolyzer.duration() > 0
                                    || hasAnyOutput(electrolyzer),
                            "T40 representative electrolyzer recipe was not selected: "
                                    + electrolyzer.pausedReason());
                    forceLastTick(helper, electrolyzer);
                    fillEnergy(helper, electrolyzer);
                })
                .thenExecuteFor(2, () -> fillEnergy(helper, electrolyzer))
                .thenExecute(() -> helper.assertTrue(
                        hasAnyOutput(electrolyzer),
                        "Electrolyzer did not produce any output: "
                                + electrolyzer.pausedReason()))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void t40SingletonFamilyExecutes(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity electrolyzer = place(
                helper,
                new BlockPos(3, 2, 3),
                ModBlocks.STAINLESS_STEEL_ELECTROLYZER.get(),
                ModProcessingMachines.ELECTROLYZER);
        RecipeMap.Entry singleton = ModRecipeMaps.ELECTROLYZER.entries().stream()
                .filter(entry -> entry.id().getPath().startsWith("t40/"))
                .filter(entry -> entry.recipe().provenance()
                        .flatMap(GTRecipeProvenance::selectedSourceRecipe)
                        .filter("gt.recipe.electrolyzer#0031"::equals)
                        .isPresent())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing locked T40 singleton family gt.recipe.electrolyzer#0031"));
        fillEnergy(helper, electrolyzer);
        loadRecipeInputs(electrolyzer, singleton.recipe());
        fillEnergy(helper, electrolyzer);
        helper.startSequence()
                .thenExecuteFor(4, () -> fillEnergy(helper, electrolyzer))
                .thenExecute(() -> {
                    helper.assertTrue(
                            electrolyzer.workProgressLong() > 0L
                                    || electrolyzer.duration() > 0
                                    || hasAnyOutput(electrolyzer),
                            "T40 singleton electrolyzer recipe was not selected: "
                                    + electrolyzer.pausedReason());
                    forceLastTick(helper, electrolyzer);
                    fillEnergy(helper, electrolyzer);
                })
                .thenExecuteFor(2, () -> fillEnergy(helper, electrolyzer))
                .thenExecute(() -> helper.assertTrue(
                        hasAnyOutput(electrolyzer),
                        "Singleton electrolyzer family did not produce output: "
                                + electrolyzer.pausedReason()))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void t40MultiFamilyExecutes(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity electrolyzer = place(
                helper,
                new BlockPos(3, 2, 3),
                ModBlocks.STAINLESS_STEEL_ELECTROLYZER.get(),
                ModProcessingMachines.ELECTROLYZER);
        RecipeMap.Entry multi = ModRecipeMaps.ELECTROLYZER.entries().stream()
                .filter(entry -> entry.id().getPath().startsWith("t40/"))
                .filter(entry -> entry.recipe().provenance()
                        .flatMap(GTRecipeProvenance::selectedSourceRecipe)
                        .filter("gt.recipe.electrolyzer#0002"::equals)
                        .isPresent())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing locked T40 multi family gt.recipe.electrolyzer#0002"));
        fillEnergy(helper, electrolyzer);
        loadRecipeInputs(electrolyzer, multi.recipe());
        fillEnergy(helper, electrolyzer);
        helper.startSequence()
                .thenExecuteFor(4, () -> fillEnergy(helper, electrolyzer))
                .thenExecute(() -> {
                    helper.assertTrue(
                            electrolyzer.workProgressLong() > 0L
                                    || electrolyzer.duration() > 0
                                    || hasAnyOutput(electrolyzer),
                            "T40 multi electrolyzer recipe was not selected: "
                                    + electrolyzer.pausedReason());
                    forceLastTick(helper, electrolyzer);
                    fillEnergy(helper, electrolyzer);
                })
                .thenExecuteFor(2, () -> fillEnergy(helper, electrolyzer))
                .thenExecute(() -> helper.assertTrue(
                        hasAnyOutput(electrolyzer),
                        "Multi electrolyzer family did not produce output: "
                                + electrolyzer.pausedReason()))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void t40StableIdsSurviveReload(GameTestHelper helper) {
        Set<ResourceLocation> first = t40StableIds();
        helper.assertTrue(first.size() == 22, "T40 locked ids missing before re-enumeration");
        RecipeMap.RecipeFamily singletonFamily = ModRecipeMaps.ELECTROLYZER
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.ELECTROLYZER.id(),
                        CompactGTRecipeFamilyDefinition
                                .T40_ELECTROLYZER_SINGLETON_PUBLICATION_GROUP))
                .orElseThrow();
        RecipeMap.RecipeFamily multiFamily = ModRecipeMaps.ELECTROLYZER
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.ELECTROLYZER.id(),
                        CompactGTRecipeFamilyDefinition
                                .T40_ELECTROLYZER_MULTI_PUBLICATION_GROUP))
                .orElseThrow();
        helper.assertTrue(
                singletonFamily.epoch() == ModRecipeMaps.ELECTROLYZER.runtimeEpoch()
                        && singletonFamily.logicalRecipeCount() == 11
                        && multiFamily.epoch() == ModRecipeMaps.ELECTROLYZER.runtimeEpoch()
                        && multiFamily.logicalRecipeCount() == 11,
                "T40 compact family epochs/counts drifted on the live map");
        Set<ResourceLocation> union = new TreeSet<>(singletonFamily.recipeIds());
        union.addAll(multiFamily.recipeIds());
        helper.assertTrue(
                union.equals(first),
                "T40 compact family recipe ids do not cover the stable id set");
        helper.assertTrue(
                t40StableIds().equals(first),
                "T40 stable ids did not survive a second entries() enumeration");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void t40EmiPlanIncludesElectrolyzerFamilies(GameTestHelper helper) {
        Set<ResourceLocation> published = t40StableIds();
        ProcessingEmiRegistrationPlan plan = ProcessingEmiRegistrationPlan.create(
                ModProcessingMachines.CONFIGURED_MACHINES);
        Set<ResourceLocation> electrolyzerEmi = plan.recipes().stream()
                .filter(recipe -> recipe.machine().recipeMap().id()
                        .equals(ModRecipeMaps.ELECTROLYZER.id()))
                .map(ProcessingEmiRegistrationPlan.RecipeRegistration::id)
                .collect(Collectors.toCollection(TreeSet::new));
        helper.assertTrue(
                electrolyzerEmi.containsAll(published),
                "EMI electrolyzer category is missing T40 recipe ids: "
                        + published.stream()
                                .filter(id -> !electrolyzerEmi.contains(id))
                                .toList());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void t40ThreeVariantsAcceptEu(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity base = place(
                helper,
                new BlockPos(2, 2, 2),
                ModBlocks.ELECTROLYZER.get(),
                ModProcessingMachines.ELECTROLYZER);
        ConfiguredProcessingMachineBlockEntity aluminium = place(
                helper,
                new BlockPos(4, 2, 2),
                ModBlocks.ALUMINIUM_ELECTROLYZER.get(),
                ModProcessingMachines.ELECTROLYZER);
        ConfiguredProcessingMachineBlockEntity stainless = place(
                helper,
                new BlockPos(6, 2, 2),
                ModBlocks.STAINLESS_STEEL_ELECTROLYZER.get(),
                ModProcessingMachines.ELECTROLYZER);
        fillEnergy(helper, base);
        fillEnergy(helper, aluminium);
        fillEnergy(helper, stainless);
        helper.succeed();
    }

    private static Set<ResourceLocation> t40StableIds() {
        return ModRecipeMaps.ELECTROLYZER.entries().stream()
                .map(RecipeMap.Entry::id)
                .filter(id -> id.getPath().startsWith("t40/"))
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
