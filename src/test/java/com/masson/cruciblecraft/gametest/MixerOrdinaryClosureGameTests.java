package com.masson.cruciblecraft.gametest;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.compat.emi.ProcessingEmiRegistrationPlan;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineEnergyPlacement;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.recipe.gt.CompactPublicationGroups;
import com.masson.cruciblecraft.recipe.gt.CompactRecipeFamilyProvider;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.GTRecipeQuery;
import com.masson.cruciblecraft.recipe.gt.GTRecipeMapLoader;
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
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated Mixer ordinary-closure runtime gate. Run with
 * {@code -PgameTestGrid=machines}.
 */
@GameTestHolder(MixerOrdinaryClosureGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class MixerOrdinaryClosureGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_machines";
    private static final String TEMPLATE = "empty";
    private static final Direction FRONT = Direction.EAST;
    private static final String RECIPE_PREFIX = "mixer/ordinary_closure/";
    private static final List<ResourceLocation> GROUPS = List.of(
            CompactPublicationGroups.MIXER_ORDINARY_CONSTRUCTION_FOAM,
            CompactPublicationGroups.MIXER_ORDINARY_MATERIAL_MATRIX,
            CompactPublicationGroups.MIXER_ORDINARY_OPAQUE);

    private MixerOrdinaryClosureGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void mixerReleasePerformanceGates(GameTestHelper helper) {
        var metrics = GTRecipeMapLoader.lastPublicationMetrics();
        var lookup = GTRecipeMapLoader.benchmarkCompactLoadLookupsForVerification();
        var capacity = GTRecipeMapLoader.lastCapacityReport();
        helper.assertTrue(
                metrics.eagerPublishedRecipes()
                        >= ModProcessingMachines.VERIFIED_OPENING_EAGER_PUBLISHED,
                "Mixer ordinary-closure eager drifted below opening 16980: "
                        + metrics.eagerPublishedRecipes());
        CrucibleCraft.LOGGER.info(
                "mixer/ordinary-closure measured capacity={} metrics={} lookup={}",
                capacity,
                metrics,
                lookup);
        GTRecipeMapLoader.verifyReleasePerformance(
                metrics, lookup, null, null);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void mixerOrdinaryFamiliesPublished(GameTestHelper helper) {
        helper.assertTrue(
                !publishedIds().isEmpty(),
                "Mixer ordinary-closure compact ids missing");
        helper.assertTrue(
                ModProcessingMachines.MIXER.items().inputs().size() == 6
                        && ModProcessingMachines.MIXER.fluids().inputs().size() == 6
                        && ModProcessingMachines.MIXER.fluids().outputs().get(0)
                                .capacity()
                        == CompactPublicationGroups.GT6_PANEL_TANK_CAPACITY,
                "Mixer inventory is not the GT6 6/1/6/2 panel");
        helper.assertTrue(
                CompactPublicationGroups.ENVELOPE_GT6_PANEL.equals(
                        CompactPublicationGroups.executionEnvelope(
                                CompactPublicationGroups.MIXER_ORDINARY_MATERIAL_MATRIX)),
                "Mixer ordinary-closure envelope is not gt6_panel");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void mixerOrdinaryGroupsExecute(GameTestHelper helper) {
        java.util.ArrayList<ConfiguredProcessingMachineBlockEntity> machines =
                new java.util.ArrayList<>();
        java.util.ArrayList<ResourceLocation> executed = new java.util.ArrayList<>();
        int column = 0;
        for (ResourceLocation group : GROUPS) {
            RecipeMap.Entry representative = shortestRunnable(group);
            if (representative == null) {
                continue;
            }
            ConfiguredProcessingMachineBlockEntity mixer = placePowered(
                    helper,
                    new BlockPos(2 + column * 2, 2, 3),
                    ModBlocks.MIXER.get(),
                    ModProcessingMachines.MIXER,
                    representative.recipe());
            loadRecipeInputs(mixer, representative.recipe());
            assertRecipeMatches(helper, representative.recipe());
            machines.add(mixer);
            executed.add(group);
            column++;
        }
        helper.assertTrue(!machines.isEmpty(), "No Mixer ordinary-closure group to execute");
        helper.startSequence()
                .thenExecuteFor(8, () -> machines.forEach(machine ->
                        fillEnergy(helper, machine)))
                .thenExecute(() -> {
                    for (int index = 0; index < machines.size(); index++) {
                        helper.assertTrue(
                                hasAnyOutput(machines.get(index)),
                                "Mixer group " + executed.get(index)
                                        + " produced no output: "
                                        + machines.get(index).pausedReason()
                                        + " stored="
                                        + machines.get(index).stored(
                                                EnergyType.KINETIC_ROTATION)
                                        + " work="
                                        + machines.get(index).workProgressLong());
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void mixerDurationEutAndConservation(GameTestHelper helper) {
        RecipeMap.Entry representative = firstOrdinary();
        GTRecipe recipe = representative.recipe();
        helper.assertTrue(recipe.duration() > 0, "Ordinary Mixer duration is 0");
        helper.assertTrue(recipe.eut() > 0L, "Ordinary Mixer EUt is 0");
        helper.assertTrue(
                recipe.duration() <= 16,
                "Conservation representative is too long for the bronze buffer: "
                        + recipe.duration());
        ConfiguredProcessingMachineBlockEntity mixer = placePowered(
                helper,
                new BlockPos(3, 2, 3),
                ModBlocks.MIXER.get(),
                ModProcessingMachines.MIXER,
                recipe);
        loadRecipeInputs(mixer, recipe);
        assertRecipeMatches(helper, recipe);
        helper.startSequence()
                .thenExecuteFor(8, () -> fillEnergy(helper, mixer))
                .thenExecute(() -> helper.assertTrue(
                        hasAnyOutput(mixer),
                        "Conservation run produced no output: "
                                + mixer.pausedReason()
                                + " stored="
                                + mixer.stored(EnergyType.KINETIC_ROTATION)
                                + " work="
                                + mixer.workProgressLong()))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void mixerStableIdsSurviveReload(GameTestHelper helper) {
        Set<ResourceLocation> first = publishedIds();
        helper.assertTrue(!first.isEmpty(), "Ordinary Mixer ids missing before re-enumeration");
        helper.assertTrue(
                publishedIds().equals(first),
                "Ordinary Mixer stable ids drifted on a second entries() pass");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void mixerEmiPlanIncludesFamilies(GameTestHelper helper) {
        Set<ResourceLocation> published = publishedIds();
        ProcessingEmiRegistrationPlan plan = ProcessingEmiRegistrationPlan.create(
                ModProcessingMachines.CONFIGURED_MACHINES);
        Set<ResourceLocation> emi = plan.recipes().stream()
                .filter(recipe -> recipe.machine().recipeMap().id()
                        .equals(ModRecipeMaps.MIXER.id()))
                .map(ProcessingEmiRegistrationPlan.RecipeRegistration::id)
                .collect(Collectors.toCollection(TreeSet::new));
        helper.assertTrue(
                emi.containsAll(published),
                "EMI categories are missing ordinary Mixer recipe ids");
        helper.succeed();
    }

    private static Set<ResourceLocation> publishedIds() {
        Set<ResourceLocation> ids = new TreeSet<>();
        ModRecipeMaps.MIXER.entries().stream()
                .map(RecipeMap.Entry::id)
                .filter(id -> id.getPath().startsWith(RECIPE_PREFIX))
                .forEach(ids::add);
        return ids;
    }

    private static RecipeMap.Entry firstOrdinary() {
        for (ResourceLocation group : GROUPS) {
            RecipeMap.Entry entry = shortestRunnable(group);
            if (entry != null) {
                return entry;
            }
        }
        throw new IllegalStateException("Missing Mixer ordinary-closure recipe");
    }

    private static RecipeMap.Entry shortestRunnable(ResourceLocation group) {
        RecipeMap.RecipeFamily family = ModRecipeMaps.MIXER
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.MIXER.id(), group))
                .orElse(null);
        if (family == null || family.logicalRecipeCount() <= 0) {
            return null;
        }
        RecipeMap.Entry best = null;
        for (int index = 0; index < family.logicalRecipeCount(); index++) {
            RecipeMap.Entry entry = family.enumerationEntry(index);
            GTRecipe recipe = entry.recipe();
            if (!inputsResolvable(recipe) || !hasDeclaredOutput(recipe)) {
                continue;
            }
            if (recipe.itemInputs().isEmpty() && recipe.duration() <= 16) {
                return entry;
            }
            if (best == null
                    || recipe.duration() < best.recipe().duration()
                    || (recipe.duration() == best.recipe().duration()
                            && recipe.itemInputs().size()
                                    < best.recipe().itemInputs().size())) {
                best = entry;
            }
            if (best.recipe().duration() <= 8 && best.recipe().itemInputs().isEmpty()) {
                return best;
            }
        }
        return best;
    }

    private static boolean inputsResolvable(GTRecipe recipe) {
        for (var ingredient : recipe.itemInputs()) {
            if (ingredient.getItems().length == 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasDeclaredOutput(GTRecipe recipe) {
        return !recipe.itemOutputs().isEmpty()
                || !recipe.fluidOutputs().isEmpty();
    }

    private static void fillEnergy(
            GameTestHelper helper,
            ConfiguredProcessingMachineBlockEntity machine) {
        EnergyType type = machine.spec().energy().type();
        Direction front = machine.getBlockState().getValue(
                ProcessingMachineBlock.FACING);
        Direction back = ProcessingMachineEnergyPlacement
                .connection(machine.spec(), front)
                .providerOffset();
        long packet = 32L;
        Direction side = back;
        if (machine.insert(type, packet, 4L, back, false) <= 0L) {
            for (Direction candidate : Direction.values()) {
                if (machine.insert(type, packet, 4L, candidate, false) > 0L) {
                    side = candidate;
                    break;
                }
            }
        }
        for (int i = 0; i < 8 && machine.stored(type) < machine.capacity(type); i++) {
            if (machine.insert(type, packet, 4L, side, false) <= 0L) {
                break;
            }
        }
        helper.assertTrue(
                machine.stored(type) > 0L,
                "Mixer stored no " + type + " on " + side);
    }

    private static void assertRecipeMatches(GameTestHelper helper, GTRecipe recipe) {
        java.util.ArrayList<ItemStack> items = new java.util.ArrayList<>();
        for (int index = 0; index < recipe.itemInputs().size(); index++) {
            ItemStack[] options = recipe.itemInputs().get(index).getItems();
            helper.assertTrue(options.length > 0, "Mixer representative item input is empty");
            ItemStack sample = options[0].copy();
            sample.setCount(Math.max(1, recipe.itemInputCounts().get(index)));
            items.add(sample);
        }
        helper.assertTrue(
                ModRecipeMaps.MIXER.findMatch(
                        new GTRecipeQuery(items, recipe.fluidInputs())).isPresent(),
                "Loaded Mixer representative is not findable");
    }

    private static ConfiguredProcessingMachineBlockEntity placePowered(
            GameTestHelper helper,
            BlockPos pos,
            Block block,
            ProcessingMachineSpec spec,
            GTRecipe recipe) {
        ConfiguredProcessingMachineBlockEntity machine = place(helper, pos, block, spec);
        fillEnergy(helper, machine);
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
            ItemStack[] options = recipe.itemInputs().get(i).getItems();
            if (options.length == 0) {
                continue;
            }
            ItemStack sample = options[0].copy();
            sample.setCount(Math.max(1, recipe.itemInputCounts().get(i)));
            machine.inventory().setStackInSlot(
                    machine.spec().items().inputs().get(i), sample);
        }
        for (int i = 0; i < recipe.fluidInputs().size(); i++) {
            machine.tanks().get(machine.spec().fluids().inputs().get(i).index())
                    .setFluid(recipe.fluidInputs().get(i).copy());
        }
    }

    private static boolean hasAnyOutput(
            ConfiguredProcessingMachineBlockEntity machine) {
        return machine.spec().items().outputs().stream().anyMatch(slot ->
                !machine.inventory().getStackInSlot(slot).isEmpty())
                || machine.spec().fluids().outputs().stream().anyMatch(tank ->
                !machine.tanks().get(tank.index()).getFluid().isEmpty());
    }
}
