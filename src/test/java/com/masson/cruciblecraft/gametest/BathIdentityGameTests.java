package com.masson.cruciblecraft.gametest;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.compat.emi.ProcessingEmiRegistrationPlan;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.content.item.BathIdentityCatalog;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.recipe.gt.CompactGTRecipeFamilyDefinition;
import com.masson.cruciblecraft.recipe.gt.CompactPublicationGroups;
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
 * Isolated bath/identity compact-family runtime gate. Run with {@code -PwaveRecipes=bath/identity}.
 * Relation-level equivalence lives in JUnit; these tests are representative.
 */
@GameTestHolder(BathIdentityGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class BathIdentityGameTests {
    public static final String NAMESPACE = "cruciblecraft_wave_bath_identity";
    private static final String TEMPLATE = "empty";
    private static final Direction FRONT = Direction.EAST;
    private static final int BATH_MTE_LOCKED_RELATIONS = 1517;
    private static final int LOCKED_RELATIONS = 34091;
    private static final int EXACT_RELATIONS = 47;
    private static final int EXACT_MULTI_RELATIONS = 12422;
    private static final int TOOL_HEAD_RELATIONS = 21622;
    private static final int IDENTITY_COUNT = 71;
    private static final int FLUID_SUPPORT_RECIPES = 2;
    private static final int BATH_REMAINDER_LOCKED_RELATIONS = 13708;
    private static final ResourceLocation BATH_MTE_GROUP =
            ResourceLocation.fromNamespaceAndPath("cruciblecraft", "bath/mte");
    private static final ResourceLocation BATH_REMAINDER_EXACT_GROUP =
            CompactPublicationGroups.BATH_REMAINDER_EXACT;
    private static final ResourceLocation BATH_REMAINDER_EXACT_MULTI_GROUP =
            CompactPublicationGroups.BATH_REMAINDER_EXACT_MULTI;
    private static final ResourceLocation EXACT_GROUP =
            CompactPublicationGroups.BATH_IDENTITY_EXACT;
    private static final ResourceLocation EXACT_MULTI_GROUP =
            CompactPublicationGroups.BATH_IDENTITY_EXACT_MULTI;
    private static final ResourceLocation TOOL_HEAD_GROUP =
            CompactPublicationGroups.BATH_IDENTITY_TOOL_HEAD;

    private BathIdentityGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void lockedCounts(GameTestHelper helper) {
        helper.assertTrue(
                BathIdentityCatalog.VARIANT_COUNT == IDENTITY_COUNT
                        && BathIdentityCatalog.identities().size() == IDENTITY_COUNT,
                "bath/identity identity catalog drifted from 71");
        helper.assertTrue(
                ModItems.bathIdentityItemsById().size() == IDENTITY_COUNT,
                "bath/identity identity items drifted: "
                        + ModItems.bathIdentityItemsById().size());
        helper.assertTrue(
                bathIdentityStableIds().size() == LOCKED_RELATIONS,
                "bath/identity Bath compact ids drifted from 34091: " + bathIdentityStableIds().size());
        RecipeMap.RecipeFamily exactFamily = bathFamily(EXACT_GROUP);
        RecipeMap.RecipeFamily multiFamily = bathFamily(EXACT_MULTI_GROUP);
        RecipeMap.RecipeFamily toolFamily = bathFamily(TOOL_HEAD_GROUP);
        helper.assertTrue(
                exactFamily != null && exactFamily.logicalRecipeCount() == EXACT_RELATIONS,
                "bath/identity exact compact family is not the 47 locked relations: "
                        + (exactFamily == null ? "missing" : exactFamily.logicalRecipeCount()));
        helper.assertTrue(
                multiFamily != null && multiFamily.logicalRecipeCount() == EXACT_MULTI_RELATIONS,
                "bath/identity exact_multi compact family is not the 12422 locked relations: "
                        + (multiFamily == null ? "missing" : multiFamily.logicalRecipeCount()));
        helper.assertTrue(
                toolFamily != null && toolFamily.logicalRecipeCount() == TOOL_HEAD_RELATIONS,
                "bath/identity tool_head compact family is not the 21622 locked relations: "
                        + (toolFamily == null ? "missing" : toolFamily.logicalRecipeCount()));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void bathExactExecution(GameTestHelper helper) {
        executeRepresentative(helper, firstExactFamilyRecipe(EXACT_GROUP));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void bathExactMultiExecution(GameTestHelper helper) {
        executeRepresentative(helper, firstExactFamilyRecipe(EXACT_MULTI_GROUP));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void bathToolHeadExecution(GameTestHelper helper) {
        executeRepresentative(helper, firstExactFamilyRecipe(TOOL_HEAD_GROUP));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void bathMteGroupUnchanged(GameTestHelper helper) {
        Set<ResourceLocation> bathMte = new TreeSet<>();
        ModRecipeMaps.BATH.entries().stream()
                .map(RecipeMap.Entry::id)
                .filter(id -> id.getPath().startsWith("bath/mte/"))
                .forEach(bathMte::add);
        helper.assertTrue(
                bathMte.size() == BATH_MTE_LOCKED_RELATIONS,
                "bath/mte Bath MTE compact ids drifted after bath/identity: " + bathMte.size());
        RecipeMap.RecipeFamily family = ModRecipeMaps.BATH
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.BATH.id(), BATH_MTE_GROUP))
                .orElse(null);
        helper.assertTrue(
                family != null && family.logicalRecipeCount() == BATH_MTE_LOCKED_RELATIONS,
                "bath/mte Bath MTE compact family is not the 1517 locked relations: "
                        + (family == null ? "missing" : family.logicalRecipeCount()));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void bathRemainderGroupsUnchanged(GameTestHelper helper) {
        Set<ResourceLocation> bathRemainder = new TreeSet<>();
        ModRecipeMaps.BATH.entries().stream()
                .map(RecipeMap.Entry::id)
                .filter(id -> id.getPath().startsWith("bath/remainder/"))
                .forEach(bathRemainder::add);
        helper.assertTrue(
                bathRemainder.size() == BATH_REMAINDER_LOCKED_RELATIONS,
                "bath/remainder Bath compact ids drifted after bath/identity: " + bathRemainder.size());
        RecipeMap.RecipeFamily exact = bathFamily(BATH_REMAINDER_EXACT_GROUP);
        RecipeMap.RecipeFamily multi = bathFamily(BATH_REMAINDER_EXACT_MULTI_GROUP);
        helper.assertTrue(
                exact != null && exact.logicalRecipeCount() == 189,
                "bath/remainder exact compact family drifted after bath/identity: "
                        + (exact == null ? "missing" : exact.logicalRecipeCount()));
        helper.assertTrue(
                multi != null && multi.logicalRecipeCount() == 13519,
                "bath/remainder exact_multi compact family drifted after bath/identity: "
                        + (multi == null ? "missing" : multi.logicalRecipeCount()));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void emiPlanIncludesIdentity(GameTestHelper helper) {
        Set<ResourceLocation> published = bathIdentityStableIds();
        helper.assertTrue(
                !published.isEmpty(),
                "bath/identity compact ids are missing from the live Bath map");
        ProcessingEmiRegistrationPlan plan = ProcessingEmiRegistrationPlan.create(
                ModProcessingMachines.CONFIGURED_MACHINES);
        Set<ResourceLocation> emi = plan.recipes().stream()
                .filter(recipe -> recipe.machine().recipeMap().id()
                        .equals(ModRecipeMaps.BATH.id()))
                .map(ProcessingEmiRegistrationPlan.RecipeRegistration::id)
                .collect(Collectors.toCollection(TreeSet::new));
        helper.assertTrue(
                emi.containsAll(published),
                "EMI categories are missing bath/identity recipe ids: "
                        + published.stream()
                                .filter(id -> !emi.contains(id))
                                .toList());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 4000)
    public static void shardHardGate(GameTestHelper helper) {
        for (ResourceLocation group : List.of(EXACT_GROUP, EXACT_MULTI_GROUP, TOOL_HEAD_GROUP)) {
            RecipeMap.RecipeFamily family = bathFamily(group);
            helper.assertTrue(
                    family instanceof CompactRecipeFamilyProvider.Snapshot snapshot
                            && snapshot.overflowRelationCount()
                                    <= CompactRecipeShardRouter.HARD_SHARD_CEILING,
                    "bath/identity live shard overflow exceeded the hard ceiling for " + group);
            CompactRecipeFamilyProvider.Snapshot snapshot =
                    (CompactRecipeFamilyProvider.Snapshot) family;
            int expectedOverflow = group.equals(EXACT_GROUP) ? 3 : 0;
            helper.assertTrue(
                    snapshot.overflowRelationCount() == expectedOverflow,
                    "bath/identity live shard overflow drifted for " + group
                            + ": " + snapshot.overflowRelationCount());
            helper.assertTrue(
                    snapshot.shardCount() > 0
                            && snapshot.shardCount() <= snapshot.recipeIds().size(),
                    "bath/identity shard_count drifted for " + group
                            + ": " + snapshot.shardCount()
                            + " vs " + snapshot.recipeIds().size());
            for (ResourceLocation id : snapshot.recipeIds()) {
                GTRecipe recipe = snapshot.entry(id).orElseThrow().recipe();
                helper.assertTrue(
                        snapshot.shardRouter().indexedCandidateCount(queryOf(recipe))
                                <= CompactRecipeShardRouter.HARD_SHARD_CEILING,
                        "bath/identity indexed too many candidates for " + id);
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void b1SupportReachable(GameTestHelper helper) {
        helper.assertTrue(
                ModItems.bathIdentityItemsById().size() == IDENTITY_COUNT,
                "bath/identity B1 identities are not registered");
        long gtSupport = ModRecipeMaps.ALL.stream()
                .flatMap(map -> map.entries().stream())
                .filter(entry -> entry.id().getPath()
                        .startsWith("player_path_support/bath_identity/"))
                .count();
        long craftingSupport = helper.getLevel().getRecipeManager()
                .getAllRecipesFor(RecipeType.CRAFTING)
                .stream()
                .filter(holder -> holder.id().getPath()
                        .startsWith("player_path_support/bath_identity/"))
                .count();
        helper.assertTrue(
                craftingSupport == 0,
                "bath/identity B1 must not publish crafting recipes: " + craftingSupport);
        helper.assertTrue(
                gtSupport == FLUID_SUPPORT_RECIPES,
                "bath/identity fluid support recipes drifted: " + gtSupport);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void reloadRootsStable(GameTestHelper helper) {
        Set<ResourceLocation> first = bathIdentityStableIds();
        helper.assertTrue(
                first.size() == LOCKED_RELATIONS,
                "bath/identity locked ids missing before re-enumeration");
        RecipeMap.RecipeFamily exactFamily = bathFamily(EXACT_GROUP);
        RecipeMap.RecipeFamily multiFamily = bathFamily(EXACT_MULTI_GROUP);
        RecipeMap.RecipeFamily toolFamily = bathFamily(TOOL_HEAD_GROUP);
        helper.assertTrue(
                exactFamily != null
                        && multiFamily != null
                        && toolFamily != null
                        && exactFamily.epoch() == ModRecipeMaps.BATH.runtimeEpoch()
                        && multiFamily.epoch() == ModRecipeMaps.BATH.runtimeEpoch()
                        && toolFamily.epoch() == ModRecipeMaps.BATH.runtimeEpoch()
                        && exactFamily.logicalRecipeCount() == EXACT_RELATIONS
                        && multiFamily.logicalRecipeCount() == EXACT_MULTI_RELATIONS
                        && toolFamily.logicalRecipeCount() == TOOL_HEAD_RELATIONS,
                "bath/identity compact family epochs/counts drifted on the live map");
        Set<ResourceLocation> union = new TreeSet<>(exactFamily.recipeIds());
        union.addAll(multiFamily.recipeIds());
        union.addAll(toolFamily.recipeIds());
        helper.assertTrue(
                union.equals(first),
                "bath/identity compact family recipe ids do not cover the stable id set");
        helper.assertTrue(
                bathIdentityStableIds().equals(first),
                "bath/identity stable ids did not survive a second entries() enumeration");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void consumePreserveCatalyst(GameTestHelper helper) {
        for (RecipeMap.Entry entry : ModRecipeMaps.BATH.entries()) {
            if (!entry.id().getPath().startsWith("bath/identity/")) {
                continue;
            }
            GTRecipe recipe = entry.recipe();
            for (int index = 0; index < recipe.itemInputActions().size(); index++) {
                ItemInputAction.Kind kind = recipe.itemInputActions().get(index).kind();
                int count = recipe.itemInputCounts().get(index);
                if (kind == ItemInputAction.Kind.PRESERVE) {
                    helper.assertTrue(
                            count == 0,
                            "bath/identity PRESERVE must use count 0: " + entry.id());
                    boolean circuit = java.util.Arrays.stream(
                                    recipe.itemInputs().get(index).getItems())
                            .anyMatch(stack ->
                                    stack.is(ModItems.PROGRAMMED_CIRCUIT.get()));
                    helper.assertTrue(
                            circuit,
                            "bath/identity PRESERVE is only for programmed_circuit: "
                                    + entry.id());
                } else if (kind == ItemInputAction.Kind.CONSUME) {
                    helper.assertTrue(
                            count > 0,
                            "bath/identity CONSUME must use a positive count: "
                                    + entry.id());
                }
            }
        }
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
        GTRecipe recipe = firstBathIdentityRecipe(candidate -> !candidate.fluidInputs().isEmpty());
        ItemStack sample = recipe.itemInputs().getFirst().getItems()[0].copy();
        sample.setCount(Math.max(1, recipe.itemInputCounts().getFirst()));
        bath.inventory().setStackInSlot(bath.spec().items().inputs().getFirst(), sample);
        bath.tanks().get(bath.spec().fluids().inputs().getFirst().index())
                .setFluid(new FluidStack(Fluids.WATER, recipe.fluidInputs().getFirst().getAmount()));
        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> helper.assertTrue(
                        bath.duration() == 0 && bath.workProgressLong() == 0L,
                        "Wrong fluid started a bath/identity Bath recipe: "
                                + bath.pausedReason()))
                .thenSucceed();
    }

    private static GTRecipe firstExactFamilyRecipe(ResourceLocation group) {
        RecipeMap.RecipeFamily family = bathFamily(group);
        if (family == null || family.recipeIds().isEmpty()) {
            throw new IllegalStateException("Missing bath/identity Bath group " + group);
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

    private static GTRecipe firstBathIdentityRecipe(
            java.util.function.Predicate<GTRecipe> filter) {
        return ModRecipeMaps.BATH.entries().stream()
                .filter(entry -> entry.id().getPath().startsWith("bath/identity/"))
                .map(RecipeMap.Entry::recipe)
                .filter(filter)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing matching bath/identity Bath recipe"));
    }

    private static Set<ResourceLocation> bathIdentityStableIds() {
        Set<ResourceLocation> ids = new TreeSet<>();
        ModRecipeMaps.BATH.entries().stream()
                .map(RecipeMap.Entry::id)
                .filter(id -> id.getPath().startsWith("bath/identity/"))
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
                            "bath/identity representative Bath recipe was not selected: "
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
