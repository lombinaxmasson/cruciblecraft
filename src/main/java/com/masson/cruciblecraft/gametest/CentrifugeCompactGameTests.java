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
import com.masson.cruciblecraft.recipe.gt.CompactPublicationGroups;
import com.masson.cruciblecraft.recipe.gt.CompactRecipeFamilyProvider;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.GTRecipeProvenance;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModProcessingMachines;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated centrifuge/compact compact-family runtime gate. Run with {@code -PwaveRecipes=centrifuge/compact}.
 */
@GameTestHolder(CentrifugeCompactGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class CentrifugeCompactGameTests {
    public static final String NAMESPACE = "cruciblecraft_wave_centrifuge_compact";
    private static final String TEMPLATE = "empty";
    private static final Direction FRONT = Direction.EAST;
    private static final ResourceLocation TUNGSTENSTEEL_CENTRIFUGE =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "tungstensteel_centrifuge");

    private CentrifugeCompactGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void compactFamiliesPublished(GameTestHelper helper) {
        Set<ResourceLocation> published = centrifugeCompactStableIds();
        helper.assertTrue(
                published.size() == 32,
                "Centrifuge is missing centrifuge/compact compact ids: " + published.size());
        RecipeMap.RecipeFamily singletonFamily = ModRecipeMaps.CENTRIFUGE
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.CENTRIFUGE.id(),
                        CompactPublicationGroups.CENTRIFUGE_SINGLETON))
                .orElse(null);
        RecipeMap.RecipeFamily multiFamily = ModRecipeMaps.CENTRIFUGE
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.CENTRIFUGE.id(),
                        CompactPublicationGroups.CENTRIFUGE_MULTI))
                .orElse(null);
        helper.assertTrue(
                singletonFamily != null
                        && singletonFamily.logicalRecipeCount() == 19,
                "Centrifuge singleton compact family is not the 19 locked relations: "
                        + (singletonFamily == null
                                ? "missing"
                                : singletonFamily.logicalRecipeCount()));
        helper.assertTrue(
                multiFamily != null && multiFamily.logicalRecipeCount() == 13,
                "Centrifuge multi compact family is not the 13 locked relations: "
                        + (multiFamily == null
                                ? "missing"
                                : multiFamily.logicalRecipeCount()));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void lockedSupportPublished(GameTestHelper helper) {
        long gtSupport = ModRecipeMaps.ALL.stream()
                .flatMap(map -> map.entries().stream())
                .filter(entry -> entry.id().getPath()
                        .startsWith("player_path_support/centrifuge/"))
                .count();
        long craftingSupport = helper.getLevel().getRecipeManager()
                .getAllRecipesFor(RecipeType.CRAFTING)
                .stream()
                .filter(holder -> holder.id().getPath()
                        .startsWith("player_path_support/centrifuge/"))
                .count();
        helper.assertTrue(
                gtSupport + craftingSupport == 34,
                "Locked centrifuge/compact support is not the 34 production-lock routes: "
                        + gtSupport
                        + " gt + "
                        + craftingSupport
                        + " crafting");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void centrifugeExecutesRepresentative(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity centrifuge = place(
                helper,
                new BlockPos(3, 2, 3),
                ModBlocks.STEEL_CENTRIFUGE.get(),
                ModProcessingMachines.CENTRIFUGE);
        helper.assertTrue(
                centrifuge.spec().energy().type() == EnergyType.KINETIC_ROTATION
                        && centrifuge.spec().energy().mode()
                                == ProcessingMachineSpec.EnergyMode.BUFFERED,
                "Steel centrifuge is not a buffered KU host");
        RecipeMap.Entry representative = ModRecipeMaps.CENTRIFUGE.entries().stream()
                .filter(entry -> entry.id().getPath().startsWith("centrifuge/compact/"))
                .filter(entry -> !entry.recipe().itemInputs().isEmpty()
                        && entry.recipe().fluidInputs().isEmpty())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing centrifuge/compact centrifuge recipe with item-only inputs"));
        fillKu(helper, centrifuge);
        loadRecipeInputs(centrifuge, representative.recipe());
        fillKu(helper, centrifuge);
        helper.startSequence()
                .thenExecuteFor(4, () -> fillKu(helper, centrifuge))
                .thenExecute(() -> {
                    helper.assertTrue(
                            centrifuge.workProgressLong() > 0L
                                    || centrifuge.duration() > 0
                                    || hasAnyOutput(centrifuge),
                            "centrifuge/compact representative centrifuge recipe was not selected: "
                                    + centrifuge.pausedReason());
                    forceLastTick(helper, centrifuge);
                    fillKu(helper, centrifuge);
                })
                .thenExecuteFor(2, () -> fillKu(helper, centrifuge))
                .thenExecute(() -> helper.assertTrue(
                        hasAnyOutput(centrifuge),
                        "Centrifuge did not produce any output: "
                                + centrifuge.pausedReason()))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void singletonFamilyExecutes(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity centrifuge = place(
                helper,
                new BlockPos(3, 2, 3),
                ModBlocks.TITANIUM_CENTRIFUGE.get(),
                ModProcessingMachines.CENTRIFUGE);
        RecipeMap.Entry singleton = ModRecipeMaps.CENTRIFUGE.entries().stream()
                .filter(entry -> entry.id().getPath().startsWith("centrifuge/compact/"))
                .filter(entry -> entry.recipe().provenance()
                        .flatMap(GTRecipeProvenance::selectedSourceRecipe)
                        .filter("gt.recipe.centrifuge#0055"::equals)
                        .isPresent())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing locked centrifuge/compact singleton family gt.recipe.centrifuge#0055"));
        fillKu(helper, centrifuge);
        loadRecipeInputs(centrifuge, singleton.recipe());
        fillKu(helper, centrifuge);
        helper.startSequence()
                .thenExecuteFor(4, () -> fillKu(helper, centrifuge))
                .thenExecute(() -> {
                    helper.assertTrue(
                            centrifuge.workProgressLong() > 0L
                                    || centrifuge.duration() > 0
                                    || hasAnyOutput(centrifuge),
                            "centrifuge/compact singleton centrifuge recipe was not selected: "
                                    + centrifuge.pausedReason());
                    forceLastTick(helper, centrifuge);
                    fillKu(helper, centrifuge);
                })
                .thenExecuteFor(2, () -> fillKu(helper, centrifuge))
                .thenExecute(() -> helper.assertTrue(
                        hasAnyOutput(centrifuge),
                        "Singleton centrifuge family did not produce output: "
                                + centrifuge.pausedReason()))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void multiFamilyExecutes(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity centrifuge = place(
                helper,
                new BlockPos(3, 2, 3),
                ModBlocks.TITANIUM_CENTRIFUGE.get(),
                ModProcessingMachines.CENTRIFUGE);
        RecipeMap.Entry multi = ModRecipeMaps.CENTRIFUGE.entries().stream()
                .filter(entry -> entry.id().getPath().startsWith("centrifuge/compact/"))
                .filter(entry -> entry.recipe().provenance()
                        .flatMap(GTRecipeProvenance::selectedSourceRecipe)
                        .filter("gt.recipe.centrifuge#0008"::equals)
                        .isPresent())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing locked centrifuge/compact multi family gt.recipe.centrifuge#0008"));
        fillKu(helper, centrifuge);
        loadRecipeInputs(centrifuge, multi.recipe());
        fillKu(helper, centrifuge);
        helper.startSequence()
                .thenExecuteFor(4, () -> fillKu(helper, centrifuge))
                .thenExecute(() -> {
                    helper.assertTrue(
                            centrifuge.workProgressLong() > 0L
                                    || centrifuge.duration() > 0
                                    || hasAnyOutput(centrifuge),
                            "centrifuge/compact multi centrifuge recipe was not selected: "
                                    + centrifuge.pausedReason());
                    forceLastTick(helper, centrifuge);
                    fillKu(helper, centrifuge);
                })
                .thenExecuteFor(2, () -> fillKu(helper, centrifuge))
                .thenExecute(() -> helper.assertTrue(
                        hasAnyOutput(centrifuge),
                        "Multi centrifuge family did not produce output: "
                                + centrifuge.pausedReason()))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void centrifugeCompactStableIdsSurviveReload(GameTestHelper helper) {
        Set<ResourceLocation> first = centrifugeCompactStableIds();
        helper.assertTrue(first.size() == 32, "centrifuge/compact locked ids missing before re-enumeration");
        RecipeMap.RecipeFamily singletonFamily = ModRecipeMaps.CENTRIFUGE
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.CENTRIFUGE.id(),
                        CompactPublicationGroups.CENTRIFUGE_SINGLETON))
                .orElseThrow();
        RecipeMap.RecipeFamily multiFamily = ModRecipeMaps.CENTRIFUGE
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.CENTRIFUGE.id(),
                        CompactPublicationGroups.CENTRIFUGE_MULTI))
                .orElseThrow();
        helper.assertTrue(
                singletonFamily.epoch() == ModRecipeMaps.CENTRIFUGE.runtimeEpoch()
                        && singletonFamily.logicalRecipeCount() == 19
                        && multiFamily.epoch() == ModRecipeMaps.CENTRIFUGE.runtimeEpoch()
                        && multiFamily.logicalRecipeCount() == 13,
                "centrifuge/compact compact family epochs/counts drifted on the live map");
        Set<ResourceLocation> union = new TreeSet<>(singletonFamily.recipeIds());
        union.addAll(multiFamily.recipeIds());
        helper.assertTrue(
                union.equals(first),
                "centrifuge/compact compact family recipe ids do not cover the stable id set");
        helper.assertTrue(
                centrifugeCompactStableIds().equals(first),
                "centrifuge/compact stable ids did not survive a second entries() enumeration");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void emiPlanIncludesCentrifugeFamilies(GameTestHelper helper) {
        Set<ResourceLocation> published = centrifugeCompactStableIds();
        ProcessingEmiRegistrationPlan plan = ProcessingEmiRegistrationPlan.create(
                ModProcessingMachines.CONFIGURED_MACHINES);
        Set<ResourceLocation> centrifugeEmi = plan.recipes().stream()
                .filter(recipe -> recipe.machine().recipeMap().id()
                        .equals(ModRecipeMaps.CENTRIFUGE.id()))
                .map(ProcessingEmiRegistrationPlan.RecipeRegistration::id)
                .collect(Collectors.toCollection(TreeSet::new));
        helper.assertTrue(
                centrifugeEmi.containsAll(published),
                "EMI centrifuge category is missing centrifuge/compact recipe ids: "
                        + published.stream()
                                .filter(id -> !centrifugeEmi.contains(id))
                                .toList());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fourVariantsAcceptKu(GameTestHelper helper) {
        Block tungstensteelBlock = BuiltInRegistries.BLOCK.get(TUNGSTENSTEEL_CENTRIFUGE);
        ConfiguredProcessingMachineBlockEntity base = place(
                helper,
                new BlockPos(2, 2, 2),
                ModBlocks.CENTRIFUGE.get(),
                ModProcessingMachines.CENTRIFUGE);
        ConfiguredProcessingMachineBlockEntity steel = place(
                helper,
                new BlockPos(4, 2, 2),
                ModBlocks.STEEL_CENTRIFUGE.get(),
                ModProcessingMachines.CENTRIFUGE);
        ConfiguredProcessingMachineBlockEntity titanium = place(
                helper,
                new BlockPos(6, 2, 2),
                ModBlocks.TITANIUM_CENTRIFUGE.get(),
                ModProcessingMachines.CENTRIFUGE);
        fillKu(helper, base);
        fillKu(helper, steel);
        fillKu(helper, titanium);
        helper.assertTrue(
                tungstensteelBlock != null,
                "Missing tungstensteel_centrifuge block registration");
        ConfiguredProcessingMachineBlockEntity tungstensteel = place(
                helper,
                new BlockPos(8, 2, 2),
                tungstensteelBlock,
                ModProcessingMachines.CENTRIFUGE);
        fillKu(helper, tungstensteel);
        helper.succeed();
    }

    private static Set<ResourceLocation> centrifugeCompactStableIds() {
        return ModRecipeMaps.CENTRIFUGE.entries().stream()
                .map(RecipeMap.Entry::id)
                .filter(id -> id.getPath().startsWith("centrifuge/compact/"))
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

    private static void fillKu(
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
