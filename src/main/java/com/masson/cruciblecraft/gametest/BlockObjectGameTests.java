package com.masson.cruciblecraft.gametest;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.compat.emi.ProcessingEmiRegistrationPlan;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FireboxBlockEntity;
import com.masson.cruciblecraft.heat.FuelDefinition;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.recipe.gt.CompactGTRecipeFamilyDefinition;
import com.masson.cruciblecraft.recipe.gt.CompactPublicationGroups;
import com.masson.cruciblecraft.recipe.gt.CompactRecipeFamilyProvider;
import com.masson.cruciblecraft.recipe.gt.CompactWaveRecipeIds;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
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
 * Isolated block-object compact-family runtime gate. Run with {@code -PwaveRecipes=block/object}.
 * Relation-level equivalence lives in JUnit; these tests are representative.
 */
@GameTestHolder(BlockObjectGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class BlockObjectGameTests {
    public static final String NAMESPACE = "cruciblecraft_wave_block_object";
    private static final String TEMPLATE = "empty";
    private static final Direction FRONT = Direction.EAST;
    private static final int LOCKED_RELATIONS = 379;
    private static final int SMELTER_RELATIONS = 271;
    private static final int DRYING_RELATIONS = 108;

    private BlockObjectGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void compactFamiliesPublished(GameTestHelper helper) {
        Set<ResourceLocation> published = blockObjectStableIds();
        helper.assertTrue(
                published.size() == LOCKED_RELATIONS,
                "block-object compact ids missing: " + published.size());
        RecipeMap.RecipeFamily smelterFamily = ModRecipeMaps.SMELTER
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.SMELTER.id(),
                        CompactPublicationGroups.SMELTER_BLOCK))
                .orElse(null);
        helper.assertTrue(
                smelterFamily != null
                        && smelterFamily.logicalRecipeCount() == SMELTER_RELATIONS,
                "Smelter block compact family is not the 271 locked relations: "
                        + (smelterFamily == null
                                ? "missing"
                                : smelterFamily.logicalRecipeCount()));
        RecipeMap.RecipeFamily dryingFamily = ModRecipeMaps.DRYING
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.DRYING.id(),
                        CompactPublicationGroups.DRYING_BLOCK))
                .orElse(null);
        helper.assertTrue(
                dryingFamily != null
                        && dryingFamily.logicalRecipeCount() == DRYING_RELATIONS,
                "Drying block compact family is not the 108 locked relations: "
                        + (dryingFamily == null
                                ? "missing"
                                : dryingFamily.logicalRecipeCount()));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void lockedSupportPublished(GameTestHelper helper) {
        long gtSupport = ModRecipeMaps.ALL.stream()
                .flatMap(map -> map.entries().stream())
                .filter(entry -> entry.id().getPath()
                        .startsWith("player_path_support/block_object/"))
                .count();
        long craftingSupport = helper.getLevel().getRecipeManager()
                .getAllRecipesFor(RecipeType.CRAFTING)
                .stream()
                .filter(holder -> holder.id().getPath()
                        .startsWith("player_path_support/block_object/"))
                .count();
        helper.assertTrue(
                gtSupport + craftingSupport == 0,
                "Locked block-object support must not be vanilla/GT recipes: "
                        + gtSupport
                        + " gt + "
                        + craftingSupport
                        + " crafting");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void smelterExecutesRepresentative(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity smelter = placePowered(
                helper,
                new BlockPos(3, 2, 3),
                ModBlocks.SMELTER.get(),
                ModProcessingMachines.SMELTER);
        RecipeMap.Entry representative = ModRecipeMaps.SMELTER.entries().stream()
                .filter(entry -> CompactWaveRecipeIds.pathStartsWith(
                        entry.id(), "smelter/block/"))
                .filter(entry -> !entry.recipe().itemInputs().isEmpty())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing block-object smelter recipe with item inputs"));
        loadRecipeInputs(smelter, representative.recipe());
        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    helper.assertTrue(
                            smelter.workProgressLong() > 0L
                                    || smelter.duration() > 0
                                    || hasAnyOutput(smelter),
                            "block-object representative smelter recipe was not selected: "
                                    + smelter.pausedReason());
                    forceLastTick(helper, smelter);
                })
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        hasAnyOutput(smelter),
                        "Smelter did not produce any output: "
                                + smelter.pausedReason()))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void dryingExecutesRepresentative(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity drying = placePowered(
                helper,
                new BlockPos(3, 2, 3),
                ModBlocks.DRYING.get(),
                ModProcessingMachines.DRYING);
        RecipeMap.Entry representative = ModRecipeMaps.DRYING.entries().stream()
                .filter(entry -> CompactWaveRecipeIds.pathStartsWith(
                        entry.id(), "drying/block/"))
                .filter(entry -> !entry.recipe().itemInputs().isEmpty())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing block-object drying recipe with item inputs"));
        loadRecipeInputs(drying, representative.recipe());
        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    helper.assertTrue(
                            drying.workProgressLong() > 0L
                                    || drying.duration() > 0
                                    || hasAnyOutput(drying),
                            "block-object representative drying recipe was not selected: "
                                    + drying.pausedReason());
                    forceLastTick(helper, drying);
                })
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        hasAnyOutput(drying),
                        "Drying did not produce any output: "
                                + drying.pausedReason()))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void blockObjectStableIdsSurviveReload(GameTestHelper helper) {
        Set<ResourceLocation> first = blockObjectStableIds();
        helper.assertTrue(
                first.size() == LOCKED_RELATIONS,
                "block-object locked ids missing before re-enumeration");
        RecipeMap.RecipeFamily smelterFamily = ModRecipeMaps.SMELTER
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.SMELTER.id(),
                        CompactPublicationGroups.SMELTER_BLOCK))
                .orElseThrow();
        helper.assertTrue(
                smelterFamily.epoch() == ModRecipeMaps.SMELTER.runtimeEpoch()
                        && smelterFamily.logicalRecipeCount() == SMELTER_RELATIONS,
                "block-object smelter compact family epoch/count drifted on the live map");
        RecipeMap.RecipeFamily dryingFamily = ModRecipeMaps.DRYING
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.DRYING.id(),
                        CompactPublicationGroups.DRYING_BLOCK))
                .orElseThrow();
        helper.assertTrue(
                dryingFamily.epoch() == ModRecipeMaps.DRYING.runtimeEpoch()
                        && dryingFamily.logicalRecipeCount() == DRYING_RELATIONS,
                "block-object drying compact family epoch/count drifted on the live map");
        helper.assertTrue(
                blockObjectStableIds().equals(first),
                "block-object stable ids did not survive a second entries() enumeration");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void emiPlanIncludesFamilies(GameTestHelper helper) {
        Set<ResourceLocation> published = blockObjectStableIds();
        ProcessingEmiRegistrationPlan plan = ProcessingEmiRegistrationPlan.create(
                ModProcessingMachines.CONFIGURED_MACHINES);
        Set<ResourceLocation> emi = plan.recipes().stream()
                .filter(recipe -> {
                    ResourceLocation mapId = recipe.machine().recipeMap().id();
                    return mapId.equals(ModRecipeMaps.SMELTER.id())
                            || mapId.equals(ModRecipeMaps.DRYING.id());
                })
                .map(ProcessingEmiRegistrationPlan.RecipeRegistration::id)
                .collect(Collectors.toCollection(TreeSet::new));
        helper.assertTrue(
                emi.containsAll(published),
                "EMI categories are missing block-object recipe ids: "
                        + published.stream()
                                .filter(id -> !emi.contains(id))
                                .toList());
        helper.succeed();
    }

    private static Set<ResourceLocation> blockObjectStableIds() {
        Set<ResourceLocation> ids = new TreeSet<>();
        ModRecipeMaps.SMELTER.entries().stream()
                .map(RecipeMap.Entry::id)
                .filter(id -> CompactWaveRecipeIds.pathStartsWith(id, "smelter/block/"))
                .forEach(ids::add);
        ModRecipeMaps.DRYING.entries().stream()
                .map(RecipeMap.Entry::id)
                .filter(id -> CompactWaveRecipeIds.pathStartsWith(id, "drying/block/"))
                .forEach(ids::add);
        return ids;
    }

    private static ConfiguredProcessingMachineBlockEntity placePowered(
            GameTestHelper helper,
            BlockPos pos,
            Block block,
            ProcessingMachineSpec spec) {
        helper.setBlock(pos.below(), ModBlocks.FIREBOX.get());
        ConfiguredProcessingMachineBlockEntity machine = place(helper, pos, block, spec);
        FireboxBlockEntity firebox = helper.getBlockEntity(pos.below());
        helper.assertTrue(
                firebox.addFuel(FuelDefinition.COAL_COKE),
                "Could not fuel the adjacent HU host");
        helper.assertTrue(
                machine.spec().energy().type() == EnergyType.HEAT
                        && machine.spec().energy().mode()
                                == ProcessingMachineSpec.EnergyMode.ADJACENT,
                "Host is not an adjacent HU machine");
        return machine;
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
}
