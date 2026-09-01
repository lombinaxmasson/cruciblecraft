package com.masson.cruciblecraft.gametest;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.compat.emi.ProcessingEmiRegistrationPlan;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.recipe.gt.CompactRecipeFamilyProvider;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated assembler/compact compact-family runtime gate. Run with {@code -PwaveRecipes=assembler/compact}.
 */
@GameTestHolder(AssemblerCompactGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class AssemblerCompactGameTests {
    public static final String NAMESPACE = "cruciblecraft_wave_assembler_compact";
    private static final String TEMPLATE = "empty";
    private static final Direction FRONT = Direction.EAST;
    private static final Direction ENERGY_SIDE = Direction.WEST;

    private AssemblerCompactGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void compactFamiliesPublished(GameTestHelper helper) {
        Set<ResourceLocation> published = assemblerCompactStableIds();
        helper.assertTrue(
                published.size() == 50,
                "Assembler is missing assembler/compact compact ids: " + published.size());
        RecipeMap.RecipeFamily family = ModRecipeMaps.ASSEMBLER
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.ASSEMBLER.id()))
                .orElse(null);
        helper.assertTrue(
                family != null && family.logicalRecipeCount() == 50,
                "Assembler compact family is not the 50 assembler/compact relations: "
                        + (family == null ? "missing" : family.logicalRecipeCount()));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void assemblerExecutesOakButton(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity assembler = place(
                helper,
                new BlockPos(3, 2, 3),
                ModBlocks.ASSEMBLER.get(),
                ModProcessingMachines.ASSEMBLER);
        RecipeMap.Entry oakButton = ModRecipeMaps.ASSEMBLER.entries().stream()
                .filter(entry -> entry.id().getPath().startsWith("assembler/compact/"))
                .filter(entry -> entry.recipe().itemOutputs().stream()
                        .anyMatch(stack -> stack.is(Items.OAK_BUTTON)))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing assembler/compact oak_planks + programmed_circuit oak_button recipe"));
        loadRecipeInputs(assembler, oakButton.recipe());
        helper.assertTrue(
                assembler.insert(
                        EnergyType.KINETIC,
                        assembler.variant().tierBand().inputNominal(),
                        4L,
                        ENERGY_SIDE,
                        false) > 0L,
                "Assembler rejected KU");
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(
                            assembler.workProgressLong() > 0L
                                    || assembler.duration() > 0
                                    || hasAnyOutput(assembler),
                            "assembler/compact oak_button recipe was not selected: "
                                    + assembler.pausedReason());
                    forceLastTick(helper, assembler);
                    assembler.insert(
                            EnergyType.KINETIC,
                            assembler.variant().tierBand().inputNominal(),
                            4L,
                            ENERGY_SIDE,
                            false);
                })
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        hasAnyOutput(assembler)
                                && assembler.spec().items().outputs().stream()
                                        .map(assembler.inventory()::getStackInSlot)
                                        .anyMatch(stack -> stack.is(Items.OAK_BUTTON)),
                        "Assembler did not produce oak_button: "
                                + assembler.pausedReason()))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void assemblerCompactStableIdsSurviveReload(GameTestHelper helper) {
        Set<ResourceLocation> first = assemblerCompactStableIds();
        helper.assertTrue(first.size() == 50, "assembler/compact ids missing before re-enumeration");
        RecipeMap.RecipeFamily family = ModRecipeMaps.ASSEMBLER
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.ASSEMBLER.id()))
                .orElseThrow();
        helper.assertTrue(
                family.epoch() == ModRecipeMaps.ASSEMBLER.runtimeEpoch()
                        && family.logicalRecipeCount() == 50
                        && new TreeSet<>(family.recipeIds()).equals(first),
                "assembler/compact compact family epoch/ids drifted on the live map");
        helper.assertTrue(
                assemblerCompactStableIds().equals(first),
                "assembler/compact stable ids did not survive a second entries() enumeration");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void emiPlanIncludesAssemblerFamilies(GameTestHelper helper) {
        Set<ResourceLocation> published = assemblerCompactStableIds();
        ProcessingEmiRegistrationPlan plan = ProcessingEmiRegistrationPlan.create(
                ModProcessingMachines.CONFIGURED_MACHINES);
        Set<ResourceLocation> assemblerEmi = plan.recipes().stream()
                .filter(recipe -> recipe.machine().recipeMap().id()
                        .equals(ModRecipeMaps.ASSEMBLER.id()))
                .map(ProcessingEmiRegistrationPlan.RecipeRegistration::id)
                .collect(Collectors.toCollection(TreeSet::new));
        helper.assertTrue(
                assemblerEmi.containsAll(published),
                "EMI assembler category is missing assembler/compact recipe ids: "
                        + published.stream()
                                .filter(id -> !assemblerEmi.contains(id))
                                .toList());
        helper.succeed();
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
            com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec spec) {
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
