package com.masson.cruciblecraft.gametest;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.compat.emi.ProcessingEmiRegistrationPlan;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.recipe.gt.CompactPublicationGroups;
import com.masson.cruciblecraft.recipe.gt.CompactRecipeFamilyProvider;
import com.masson.cruciblecraft.recipe.gt.CompactRecipeShardRouter;
import com.masson.cruciblecraft.recipe.gt.CompactWaveRecipeIds;
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
 * Isolated Bath tiny-purified compact-family runtime gate. Run with
 * {@code -PgameTestGrid=machines}.
 * Relation-level equivalence lives in JUnit; these tests are representative.
 */
@GameTestHolder(BathTinyPurifiedGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class BathTinyPurifiedGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_machines";
    private static final String TEMPLATE = "empty";
    private static final Direction FRONT = Direction.EAST;
    private static final int BATH_MTE_LOCKED_RELATIONS = 1517;
    private static final int BATH_REMAINDER_LOCKED_RELATIONS = 13708;
    private static final int BATH_IDENTITY_LOCKED_RELATIONS = 34091;
    private static final int LOCKED_RELATIONS = 95;
    private static final int FLUID_SUPPORT_RECIPES = 0;
    private static final ResourceLocation BATH_MTE_GROUP =
            CompactPublicationGroups.BATH_MTE;
    private static final ResourceLocation BATH_REMAINDER_EXACT_GROUP =
            CompactPublicationGroups.BATH_REMAINDER_EXACT;
    private static final ResourceLocation BATH_REMAINDER_EXACT_MULTI_GROUP =
            CompactPublicationGroups.BATH_REMAINDER_EXACT_MULTI;
    private static final ResourceLocation BATH_IDENTITY_EXACT_GROUP =
            CompactPublicationGroups.BATH_IDENTITY_EXACT;
    private static final ResourceLocation BATH_IDENTITY_EXACT_MULTI_GROUP =
            CompactPublicationGroups.BATH_IDENTITY_EXACT_MULTI;
    private static final ResourceLocation BATH_IDENTITY_TOOL_HEAD_GROUP =
            CompactPublicationGroups.BATH_IDENTITY_TOOL_HEAD;
    private static final ResourceLocation EXACT_MULTI_GROUP =
            CompactPublicationGroups.BATH_TINY_PURIFIED_EXACT_MULTI;

    private BathTinyPurifiedGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void lockedCounts(GameTestHelper helper) {
        helper.assertTrue(
                tinyPurifiedStableIds().size() == LOCKED_RELATIONS,
                "tiny-purified Bath compact ids drifted from 95: " + tinyPurifiedStableIds().size());
        RecipeMap.RecipeFamily multiFamily = bathFamily(EXACT_MULTI_GROUP);
        helper.assertTrue(
                multiFamily != null && multiFamily.logicalRecipeCount() == LOCKED_RELATIONS,
                "tiny-purified exact_multi compact family is not the 95 locked relations: "
                        + (multiFamily == null ? "missing" : multiFamily.logicalRecipeCount()));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void bathExactMultiExecution(GameTestHelper helper) {
        executeRepresentative(helper, firstExactFamilyRecipe(EXACT_MULTI_GROUP));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void bathMteGroupUnchanged(GameTestHelper helper) {
        Set<ResourceLocation> bathMte = prefixIds("bath/mte/");
        helper.assertTrue(
                bathMte.size() == BATH_MTE_LOCKED_RELATIONS,
                "Bath MTE compact ids drifted after tiny-purified: " + bathMte.size());
        RecipeMap.RecipeFamily family = bathFamily(BATH_MTE_GROUP);
        helper.assertTrue(
                family != null && family.logicalRecipeCount() == BATH_MTE_LOCKED_RELATIONS,
                "Bath MTE compact family is not the 1517 locked relations: "
                        + (family == null ? "missing" : family.logicalRecipeCount()));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void bathRemainderGroupsUnchanged(GameTestHelper helper) {
        Set<ResourceLocation> bathRemainder = prefixIds("bath/remainder/");
        helper.assertTrue(
                bathRemainder.size() == BATH_REMAINDER_LOCKED_RELATIONS,
                "Bath remainder compact ids drifted after tiny-purified: " + bathRemainder.size());
        RecipeMap.RecipeFamily exact = bathFamily(BATH_REMAINDER_EXACT_GROUP);
        RecipeMap.RecipeFamily multi = bathFamily(BATH_REMAINDER_EXACT_MULTI_GROUP);
        helper.assertTrue(
                exact != null && multi != null
                        && exact.logicalRecipeCount() + multi.logicalRecipeCount()
                                == BATH_REMAINDER_LOCKED_RELATIONS,
                "Bath remainder compact families drifted after tiny-purified");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void bathIdentityGroupsUnchanged(GameTestHelper helper) {
        Set<ResourceLocation> bathIdentity = prefixIds("bath/identity/");
        helper.assertTrue(
                bathIdentity.size() == BATH_IDENTITY_LOCKED_RELATIONS,
                "Bath identity compact ids drifted after tiny-purified: " + bathIdentity.size());
        RecipeMap.RecipeFamily exact = bathFamily(BATH_IDENTITY_EXACT_GROUP);
        RecipeMap.RecipeFamily multi = bathFamily(BATH_IDENTITY_EXACT_MULTI_GROUP);
        RecipeMap.RecipeFamily tool = bathFamily(BATH_IDENTITY_TOOL_HEAD_GROUP);
        helper.assertTrue(
                exact != null && multi != null && tool != null
                        && exact.logicalRecipeCount()
                                + multi.logicalRecipeCount()
                                + tool.logicalRecipeCount()
                                == BATH_IDENTITY_LOCKED_RELATIONS,
                "Bath identity compact families drifted after tiny-purified");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void emiPlanIncludesTinyPurified(GameTestHelper helper) {
        Set<ResourceLocation> published = tinyPurifiedStableIds();
        helper.assertTrue(
                !published.isEmpty(),
                "tiny-purified compact ids are missing from the live Bath map");
        ProcessingEmiRegistrationPlan plan = ProcessingEmiRegistrationPlan.create(
                ModProcessingMachines.CONFIGURED_MACHINES);
        Set<ResourceLocation> emi = plan.recipes().stream()
                .filter(recipe -> recipe.machine().recipeMap().id()
                        .equals(ModRecipeMaps.BATH.id()))
                .map(ProcessingEmiRegistrationPlan.RecipeRegistration::id)
                .collect(Collectors.toCollection(TreeSet::new));
        helper.assertTrue(
                emi.containsAll(published),
                "EMI categories are missing tiny-purified recipe ids: "
                        + published.stream()
                                .filter(id -> !emi.contains(id))
                                .toList());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void shardHardGate(GameTestHelper helper) {
        RecipeMap.RecipeFamily family = bathFamily(EXACT_MULTI_GROUP);
        helper.assertTrue(
                family instanceof CompactRecipeFamilyProvider.Snapshot snapshot
                        && snapshot.overflowRelationCount() == 0
                        && snapshot.overflowRelationCount()
                                <= CompactRecipeShardRouter.HARD_SHARD_CEILING,
                "tiny-purified live shard overflow exceeded the hard ceiling");
        CompactRecipeFamilyProvider.Snapshot snapshot =
                (CompactRecipeFamilyProvider.Snapshot) family;
        helper.assertTrue(
                snapshot.shardCount() == snapshot.recipeIds().size(),
                "tiny-purified shard_count != live relation count: " + snapshot.shardCount()
                        + " vs " + snapshot.recipeIds().size());
        for (ResourceLocation id : snapshot.recipeIds()) {
            GTRecipe recipe = snapshot.entry(id).orElseThrow().recipe();
            helper.assertTrue(
                    snapshot.shardRouter().indexedCandidateCount(queryOf(recipe))
                            <= CompactRecipeShardRouter.HARD_SHARD_CEILING,
                    "tiny-purified indexed too many candidates for " + id);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void b1SupportReachable(GameTestHelper helper) {
        long gtSupport = ModRecipeMaps.ALL.stream()
                .flatMap(map -> map.entries().stream())
                .filter(entry -> entry.id().getPath()
                        .startsWith("player_path_support/bath_tiny_purified/"))
                .count();
        long craftingSupport = helper.getLevel().getRecipeManager()
                .getAllRecipesFor(RecipeType.CRAFTING)
                .stream()
                .filter(holder -> holder.id().getPath()
                        .startsWith("player_path_support/bath_tiny_purified/"))
                .count();
        helper.assertTrue(
                craftingSupport == 0,
                "tiny-purified B1 must not publish crafting recipes: " + craftingSupport);
        helper.assertTrue(
                gtSupport == FLUID_SUPPORT_RECIPES,
                "tiny-purified fluid support recipes drifted: " + gtSupport);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void reloadRootsStable(GameTestHelper helper) {
        Set<ResourceLocation> first = tinyPurifiedStableIds();
        helper.assertTrue(
                first.size() == LOCKED_RELATIONS,
                "tiny-purified locked ids missing before re-enumeration");
        RecipeMap.RecipeFamily multiFamily = bathFamily(EXACT_MULTI_GROUP);
        helper.assertTrue(
                multiFamily != null
                        && multiFamily.epoch() == ModRecipeMaps.BATH.runtimeEpoch()
                        && multiFamily.logicalRecipeCount() == LOCKED_RELATIONS,
                "tiny-purified compact family epochs/counts drifted on the live map");
        helper.assertTrue(
                new TreeSet<>(multiFamily.recipeIds()).equals(first),
                "tiny-purified compact family recipe ids do not cover the stable id set");
        helper.assertTrue(
                tinyPurifiedStableIds().equals(first),
                "tiny-purified stable ids did not survive a second entries() enumeration");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void consumePreserveCatalyst(GameTestHelper helper) {
        for (RecipeMap.Entry entry : ModRecipeMaps.BATH.entries()) {
            if (!CompactWaveRecipeIds.isBathTinyPurifiedRecipe(entry.id())) {
                continue;
            }
            GTRecipe recipe = entry.recipe();
            for (int index = 0; index < recipe.itemInputActions().size(); index++) {
                ItemInputAction.Kind kind = recipe.itemInputActions().get(index).kind();
                int count = recipe.itemInputCounts().get(index);
                if (kind == ItemInputAction.Kind.PRESERVE) {
                    helper.assertTrue(
                            count == 0,
                            "tiny-purified PRESERVE must use count 0: " + entry.id());
                    boolean circuit = java.util.Arrays.stream(
                                    recipe.itemInputs().get(index).getItems())
                            .anyMatch(stack ->
                                    stack.is(ModItems.PROGRAMMED_CIRCUIT.get()));
                    helper.assertTrue(
                            circuit,
                            "tiny-purified PRESERVE is only for programmed_circuit: "
                                    + entry.id());
                } else if (kind == ItemInputAction.Kind.CONSUME) {
                    helper.assertTrue(
                            count > 0,
                            "tiny-purified CONSUME must use a positive count: "
                                    + entry.id());
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void wrongFluidRejected(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity bath = placeBath(helper);
        helper.assertTrue(
                bath.spec().energy().type() == EnergyType.TIME
                        && bath.spec().energy().mode()
                                == ProcessingMachineSpec.EnergyMode.BUFFERED,
                "Bath is not a buffered TIME host");
        GTRecipe recipe = firstTinyPurifiedRecipe(candidate -> !candidate.fluidInputs().isEmpty());
        ItemStack sample = recipe.itemInputs().getFirst().getItems()[0].copy();
        sample.setCount(Math.max(1, recipe.itemInputCounts().getFirst()));
        bath.inventory().setStackInSlot(bath.spec().items().inputs().getFirst(), sample);
        bath.tanks().get(bath.spec().fluids().inputs().getFirst().index())
                .setFluid(new FluidStack(Fluids.WATER, recipe.fluidInputs().getFirst().getAmount()));
        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> helper.assertTrue(
                        bath.duration() == 0 && bath.workProgressLong() == 0L,
                        "Wrong fluid started a tiny-purified Bath recipe: "
                                + bath.pausedReason()))
                .thenSucceed();
    }

    private static GTRecipe firstExactFamilyRecipe(ResourceLocation group) {
        RecipeMap.RecipeFamily family = bathFamily(group);
        if (family == null || family.recipeIds().isEmpty()) {
            throw new IllegalStateException("Missing tiny-purified Bath group " + group);
        }
        ProcessingMachineSpec bath = ModProcessingMachines.BATH;
        return family.recipeIds().stream()
                .map(id -> family.entry(id).orElseThrow().recipe())
                .filter(candidate -> fitsLiveBath(bath, candidate))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No tiny-purified Bath recipe fits the live Bath host"));
    }

    private static boolean fitsLiveBath(ProcessingMachineSpec spec, GTRecipe recipe) {
        if (recipe.fluidInputs().isEmpty()) {
            return false;
        }
        if (recipe.itemInputs().size() > spec.items().inputs().size()
                || recipe.itemOutputs().size() > spec.items().outputs().size()
                || recipe.fluidInputs().size() > spec.fluids().inputs().size()
                || recipe.fluidOutputs().size() > spec.fluids().outputs().size()) {
            return false;
        }
        for (int index = 0; index < recipe.fluidInputs().size(); index++) {
            if (recipe.fluidInputs().get(index).getAmount()
                    > spec.fluids().inputs().get(index).capacity()) {
                return false;
            }
        }
        for (int index = 0; index < recipe.fluidOutputs().size(); index++) {
            if (recipe.fluidOutputs().get(index).getAmount()
                    > spec.fluids().outputs().get(index).capacity()) {
                return false;
            }
        }
        return true;
    }

    private static RecipeMap.RecipeFamily bathFamily(ResourceLocation publicationGroup) {
        return ModRecipeMaps.BATH
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.BATH.id(), publicationGroup))
                .orElse(null);
    }

    private static GTRecipe firstTinyPurifiedRecipe(
            java.util.function.Predicate<GTRecipe> filter) {
        return ModRecipeMaps.BATH.entries().stream()
                .filter(entry -> CompactWaveRecipeIds.isBathTinyPurifiedRecipe(entry.id()))
                .map(RecipeMap.Entry::recipe)
                .filter(filter)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing matching tiny-purified Bath recipe"));
    }

    private static Set<ResourceLocation> tinyPurifiedStableIds() {
        Set<ResourceLocation> ids = new TreeSet<>();
        ModRecipeMaps.BATH.entries().stream()
                .map(RecipeMap.Entry::id)
                .filter(CompactWaveRecipeIds::isBathTinyPurifiedRecipe)
                .forEach(ids::add);
        return ids;
    }

    private static Set<ResourceLocation> prefixIds(String prefix) {
        Set<ResourceLocation> ids = new TreeSet<>();
        ModRecipeMaps.BATH.entries().stream()
                .map(RecipeMap.Entry::id)
                .filter(id -> id.getPath().startsWith(prefix))
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
        loadRecipeInputs(bath, recipe);
        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    helper.assertTrue(
                            bath.workProgressLong() > 0L
                                    || bath.duration() > 0
                                    || hasAnyOutput(bath),
                            "tiny-purified representative Bath recipe was not selected: "
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
            GTRecipe recipe) {
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
