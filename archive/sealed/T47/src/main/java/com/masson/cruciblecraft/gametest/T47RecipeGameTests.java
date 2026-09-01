package com.masson.cruciblecraft.gametest;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.compat.emi.ProcessingEmiRegistrationPlan;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.content.item.T47BathFluidCatalog;
import com.masson.cruciblecraft.content.item.T47BlockObjectCatalog;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.recipe.gt.CompactGTRecipeFamilyDefinition;
import com.masson.cruciblecraft.recipe.gt.CompactRecipeFamilyProvider;
import com.masson.cruciblecraft.recipe.gt.CompactRecipeShardRouter;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.GTRecipeQuery;
import com.masson.cruciblecraft.recipe.gt.ItemInputAction;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModItems;
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
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated T47 compact-family runtime gate. Run with {@code -Pt47Recipes}.
 * Relation-level equivalence lives in JUnit; these tests are representative.
 */
@GameTestHolder(T47RecipeGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class T47RecipeGameTests {
    public static final String NAMESPACE = "cruciblecraft_t47";
    private static final String TEMPLATE = "empty";
    private static final Direction FRONT = Direction.EAST;
    private static final int T46_LOCKED_RELATIONS = 1517;
    private static final int LOCKED_RELATIONS = 13708;
    private static final int EXACT_RELATIONS = 189;
    private static final int EXACT_MULTI_RELATIONS = 13519;
    private static final int FLUID_SUPPORT_RECIPES = 10;
    private static final ResourceLocation T46_PUBLICATION_GROUP =
            ResourceLocation.fromNamespaceAndPath("cruciblecraft", "t46_bath_mte");
    private static final ResourceLocation EXACT_GROUP =
            CompactGTRecipeFamilyDefinition.T47_BATH_EXACT_PUBLICATION_GROUP;
    private static final ResourceLocation EXACT_MULTI_GROUP =
            CompactGTRecipeFamilyDefinition.T47_BATH_EXACT_MULTI_PUBLICATION_GROUP;

    private T47RecipeGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void lockedCounts(GameTestHelper helper) {
        helper.assertTrue(
                T47BlockObjectCatalog.VARIANT_COUNT == 283
                        && T47BlockObjectCatalog.variants().size() == 283,
                "T47 block-object catalog drifted from 283");
        helper.assertTrue(
                T47BathFluidCatalog.FLUID_COUNT == 3
                        && T47BathFluidCatalog.fluids().size() == 3,
                "T47 Bath fluid overlay drifted from 3");
        helper.assertTrue(
                ModItems.t47BlockObjectItemsById().size() == 283,
                "T47 block-object items drifted: "
                        + ModItems.t47BlockObjectItemsById().size());
        helper.assertTrue(
                t47StableIds().size() == LOCKED_RELATIONS,
                "T47 Bath compact ids drifted from 13708: " + t47StableIds().size());
        RecipeMap.RecipeFamily exactFamily = bathFamily(EXACT_GROUP);
        RecipeMap.RecipeFamily multiFamily = bathFamily(EXACT_MULTI_GROUP);
        helper.assertTrue(
                exactFamily != null && exactFamily.logicalRecipeCount() == EXACT_RELATIONS,
                "T47 exact compact family is not the 189 locked relations: "
                        + (exactFamily == null ? "missing" : exactFamily.logicalRecipeCount()));
        helper.assertTrue(
                multiFamily != null && multiFamily.logicalRecipeCount() == EXACT_MULTI_RELATIONS,
                "T47 exact_multi compact family is not the 13519 locked relations: "
                        + (multiFamily == null ? "missing" : multiFamily.logicalRecipeCount()));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void bathExactExecution(GameTestHelper helper) {
        executeRepresentative(helper, firstExactFamilyRecipe(true));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void bathExactMultiExecution(GameTestHelper helper) {
        executeRepresentative(helper, firstExactFamilyRecipe(false));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void t46GroupUnchanged(GameTestHelper helper) {
        Set<ResourceLocation> t46 = new TreeSet<>();
        ModRecipeMaps.BATH.entries().stream()
                .map(RecipeMap.Entry::id)
                .filter(id -> id.getPath().startsWith("t46/"))
                .forEach(t46::add);
        helper.assertTrue(
                t46.size() == T46_LOCKED_RELATIONS,
                "T46 Bath MTE compact ids drifted after T47: " + t46.size());
        RecipeMap.RecipeFamily family = ModRecipeMaps.BATH
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.BATH.id(), T46_PUBLICATION_GROUP))
                .orElse(null);
        helper.assertTrue(
                family != null && family.logicalRecipeCount() == T46_LOCKED_RELATIONS,
                "T46 Bath MTE compact family is not the 1517 locked relations: "
                        + (family == null ? "missing" : family.logicalRecipeCount()));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void emiPlanIncludesT47(GameTestHelper helper) {
        Set<ResourceLocation> published = t47StableIds();
        helper.assertTrue(
                !published.isEmpty(),
                "T47 compact ids are missing from the live Bath map");
        ProcessingEmiRegistrationPlan plan = ProcessingEmiRegistrationPlan.create(
                ModProcessingMachines.CONFIGURED_MACHINES);
        Set<ResourceLocation> emi = plan.recipes().stream()
                .filter(recipe -> recipe.machine().recipeMap().id()
                        .equals(ModRecipeMaps.BATH.id()))
                .map(ProcessingEmiRegistrationPlan.RecipeRegistration::id)
                .collect(Collectors.toCollection(TreeSet::new));
        helper.assertTrue(
                emi.containsAll(published),
                "EMI categories are missing T47 recipe ids: "
                        + published.stream()
                                .filter(id -> !emi.contains(id))
                                .toList());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void shardHardGate(GameTestHelper helper) {
        for (ResourceLocation group : List.of(EXACT_GROUP, EXACT_MULTI_GROUP)) {
            RecipeMap.RecipeFamily family = bathFamily(group);
            helper.assertTrue(
                    family instanceof CompactRecipeFamilyProvider.Snapshot snapshot
                            && snapshot.overflowRelationCount() == 0
                            && snapshot.overflowRelationCount()
                                    <= CompactRecipeShardRouter.HARD_SHARD_CEILING,
                    "T47 live shard overflow exceeded the hard ceiling for " + group);
            CompactRecipeFamilyProvider.Snapshot snapshot =
                    (CompactRecipeFamilyProvider.Snapshot) family;
            helper.assertTrue(
                    snapshot.shardCount() == snapshot.recipeIds().size(),
                    "T47 shard_count != live relation count for " + group
                            + ": " + snapshot.shardCount()
                            + " vs " + snapshot.recipeIds().size());
            for (ResourceLocation id : snapshot.recipeIds()) {
                GTRecipe recipe = snapshot.entry(id).orElseThrow().recipe();
                helper.assertTrue(
                        snapshot.shardRouter().indexedCandidateCount(queryOf(recipe))
                                <= CompactRecipeShardRouter.HARD_SHARD_CEILING,
                        "T47 indexed too many candidates for " + id);
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void b1SupportReachable(GameTestHelper helper) {
        helper.assertTrue(
                ModItems.t47BlockObjectItemsById().size() == 283,
                "T47 B1 identities are not registered");
        long gtSupport = ModRecipeMaps.ALL.stream()
                .flatMap(map -> map.entries().stream())
                .filter(entry -> entry.id().getPath()
                        .startsWith("t47_player_path_support/"))
                .count();
        long craftingSupport = helper.getLevel().getRecipeManager()
                .getAllRecipesFor(RecipeType.CRAFTING)
                .stream()
                .filter(holder -> holder.id().getPath()
                        .startsWith("t47_player_path_support/"))
                .count();
        helper.assertTrue(
                craftingSupport == 0,
                "T47 B1 must not publish crafting recipes: " + craftingSupport);
        helper.assertTrue(
                gtSupport == FLUID_SUPPORT_RECIPES,
                "T47 fluid support recipes drifted: " + gtSupport);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void reloadRootsStable(GameTestHelper helper) {
        Set<ResourceLocation> first = t47StableIds();
        helper.assertTrue(
                first.size() == LOCKED_RELATIONS,
                "T47 locked ids missing before re-enumeration");
        RecipeMap.RecipeFamily exactFamily = bathFamily(EXACT_GROUP);
        RecipeMap.RecipeFamily multiFamily = bathFamily(EXACT_MULTI_GROUP);
        helper.assertTrue(
                exactFamily != null
                        && multiFamily != null
                        && exactFamily.epoch() == ModRecipeMaps.BATH.runtimeEpoch()
                        && multiFamily.epoch() == ModRecipeMaps.BATH.runtimeEpoch()
                        && exactFamily.logicalRecipeCount() == EXACT_RELATIONS
                        && multiFamily.logicalRecipeCount() == EXACT_MULTI_RELATIONS,
                "T47 compact family epochs/counts drifted on the live map");
        Set<ResourceLocation> union = new TreeSet<>(exactFamily.recipeIds());
        union.addAll(multiFamily.recipeIds());
        helper.assertTrue(
                union.equals(first),
                "T47 compact family recipe ids do not cover the stable id set");
        helper.assertTrue(
                t47StableIds().equals(first),
                "T47 stable ids did not survive a second entries() enumeration");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void consumePreserveCatalyst(GameTestHelper helper) {
        boolean anyPreserve = ModRecipeMaps.BATH.entries().stream()
                .filter(entry -> entry.id().getPath().startsWith("t47/"))
                .map(RecipeMap.Entry::recipe)
                .anyMatch(candidate -> candidate.itemInputActions().stream()
                        .anyMatch(action ->
                                action.kind() == ItemInputAction.Kind.PRESERVE));
        helper.assertTrue(
                !anyPreserve,
                "T47 remainder lock is consume-only; PRESERVE leaked into t47/");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void wrongIdentityRejected(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity bath = placeBath(helper);
        helper.assertTrue(
                bath.spec().energy().type() == EnergyType.TIME
                        && bath.spec().energy().mode()
                                == ProcessingMachineSpec.EnergyMode.BUFFERED,
                "Bath is not a buffered TIME host");
        GTRecipe recipe = firstT47Recipe(candidate -> !candidate.fluidInputs().isEmpty());
        ItemStack sample = recipe.itemInputs().getFirst().getItems()[0].copy();
        sample.setCount(Math.max(1, recipe.itemInputCounts().getFirst()));
        bath.inventory().setStackInSlot(bath.spec().items().inputs().getFirst(), sample);
        bath.tanks().get(bath.spec().fluids().inputs().getFirst().index())
                .setFluid(new FluidStack(Fluids.WATER, recipe.fluidInputs().getFirst().getAmount()));
        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> helper.assertTrue(
                        bath.duration() == 0 && bath.workProgressLong() == 0L,
                        "Wrong fluid started a T47 Bath recipe: "
                                + bath.pausedReason()))
                .thenSucceed();
    }

    private static GTRecipe firstExactFamilyRecipe(boolean exact) {
        ResourceLocation group = exact ? EXACT_GROUP : EXACT_MULTI_GROUP;
        RecipeMap.RecipeFamily family = bathFamily(group);
        if (family == null || family.recipeIds().isEmpty()) {
            throw new IllegalStateException("Missing T47 Bath group " + group);
        }
        return family.recipeIds().stream()
                .map(id -> family.entry(id).orElseThrow().recipe())
                .filter(candidate -> !candidate.fluidInputs().isEmpty())
                .findFirst()
                .orElseGet(() -> family.entry(family.recipeIds().getFirst())
                        .orElseThrow()
                        .recipe());
    }

    private static RecipeMap.RecipeFamily bathFamily(ResourceLocation publicationGroup) {
        return ModRecipeMaps.BATH
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.BATH.id(), publicationGroup))
                .orElse(null);
    }

    private static GTRecipe firstT47Recipe(
            java.util.function.Predicate<GTRecipe> filter) {
        return ModRecipeMaps.BATH.entries().stream()
                .filter(entry -> entry.id().getPath().startsWith("t47/"))
                .map(RecipeMap.Entry::recipe)
                .filter(filter)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing matching T47 Bath recipe"));
    }

    private static Set<ResourceLocation> t47StableIds() {
        Set<ResourceLocation> ids = new TreeSet<>();
        ModRecipeMaps.BATH.entries().stream()
                .map(RecipeMap.Entry::id)
                .filter(id -> id.getPath().startsWith("t47/"))
                .forEach(ids::add);
        return ids;
    }

    private static void executeRepresentative(GameTestHelper helper, GTRecipe recipe) {
        ConfiguredProcessingMachineBlockEntity bath = placeBath(helper);
        helper.assertTrue(
                bath.spec().energy().type() == EnergyType.TIME
                        && bath.spec().energy().mode()
                                == ProcessingMachineSpec.EnergyMode.BUFFERED,
                "Bath is not a buffered TIME host");
        loadRecipeInputs(bath, recipe, ItemStack.EMPTY);
        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    helper.assertTrue(
                            bath.workProgressLong() > 0L
                                    || bath.duration() > 0
                                    || hasAnyOutput(bath),
                            "T47 representative Bath recipe was not selected: "
                                    + bath.pausedReason());
                    forceLastTick(helper, bath);
                })
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        hasAnyOutput(bath),
                        "Bath did not produce any output: "
                                + bath.pausedReason()))
                .thenSucceed();
    }

    private static ConfiguredProcessingMachineBlockEntity placeBath(GameTestHelper helper) {
        return place(
                helper,
                new BlockPos(3, 2, 3),
                ModBlocks.BATH.get(),
                ModProcessingMachines.BATH);
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
            ConfiguredProcessingMachineBlockEntity machine,
            GTRecipe recipe,
            ItemStack acquiredItem) {
        for (int i = 0; i < recipe.itemInputs().size(); i++) {
            ItemStack sample;
            if (i == 0 && !acquiredItem.isEmpty()) {
                sample = acquiredItem.copy();
            } else {
                sample = recipe.itemInputs().get(i).getItems()[0].copy();
            }
            sample.setCount(Math.max(1, recipe.itemInputCounts().get(i)));
            machine.inventory().setStackInSlot(
                    machine.spec().items().inputs().get(i), sample);
        }
        for (int i = 0; i < recipe.fluidInputs().size(); i++) {
            machine.tanks().get(machine.spec().fluids().inputs().get(i).index())
                    .setFluid(recipe.fluidInputs().get(i).copy());
        }
    }

    private static GTRecipeQuery queryOf(GTRecipe recipe) {
        java.util.ArrayList<ItemStack> stacks = new java.util.ArrayList<>();
        for (int index = 0; index < recipe.itemInputs().size(); index++) {
            ItemStack[] items = recipe.itemInputs().get(index).getItems();
            ItemStack stack = items[0].copy();
            stack.setCount(Math.max(1, recipe.itemInputCounts().get(index)));
            stacks.add(stack);
        }
        return new GTRecipeQuery(
                stacks,
                recipe.fluidInputs().stream()
                        .map(FluidStack::copy)
                        .toList());
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
