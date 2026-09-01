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
import com.masson.cruciblecraft.recipe.gt.CompactRecipeFamilyProvider;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModFeatures;
import com.masson.cruciblecraft.registry.ModProcessingMachines;
import com.masson.cruciblecraft.registry.ModRecipeMaps;
import com.masson.cruciblecraft.worldgen.ItemScatterConfiguration;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated T46 compact-family runtime gate. Run with {@code -Pt46Recipes}.
 * Relation-level equivalence lives in JUnit; these tests are representative.
 */
@GameTestHolder(T46RecipeGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class T46RecipeGameTests {
    public static final String NAMESPACE = "cruciblecraft_t46";
    private static final String TEMPLATE = "empty";
    private static final Direction FRONT = Direction.EAST;
    private static final int LOCKED_RELATIONS = 1517;
    private static final int FLUID_SUPPORT_RECIPES = 37;
    private static final ResourceLocation PUBLICATION_GROUP =
            ResourceLocation.fromNamespaceAndPath("cruciblecraft", "t46_bath_mte");
    private static final ResourceLocation SCATTER_FEATURE =
            ResourceLocation.fromNamespaceAndPath("cruciblecraft", "t46_bath_mte_scatter");
    private static final TagKey<Item> SCATTER_ITEMS = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("cruciblecraft", "t46_bath_mte_items"));

    private T46RecipeGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void t46CompactFamiliesPublished(GameTestHelper helper) {
        Set<ResourceLocation> published = t46StableIds();
        helper.assertTrue(
                published.size() == LOCKED_RELATIONS,
                "T46 compact ids missing: " + published.size());
        RecipeMap.RecipeFamily family = ModRecipeMaps.BATH
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.BATH.id(), PUBLICATION_GROUP))
                .orElse(null);
        helper.assertTrue(
                family != null && family.logicalRecipeCount() == LOCKED_RELATIONS,
                "Bath MTE compact family is not the 1517 locked relations: "
                        + (family == null ? "missing" : family.logicalRecipeCount()));
        helper.assertTrue(
                family instanceof CompactRecipeFamilyProvider.Snapshot snapshot
                        && snapshot.shardCount() == LOCKED_RELATIONS
                        && snapshot.overflowRelationCount() == 0,
                "T46 live shard count drifted from the 1517 pair manifest");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void t46LockedSupportPublished(GameTestHelper helper) {
        long gtSupport = ModRecipeMaps.ALL.stream()
                .flatMap(map -> map.entries().stream())
                .filter(entry -> entry.id().getPath()
                        .startsWith("t46_player_path_support/"))
                .count();
        long craftingSupport = helper.getLevel().getRecipeManager()
                .getAllRecipesFor(RecipeType.CRAFTING)
                .stream()
                .filter(holder -> holder.id().getPath()
                        .startsWith("t46_player_path_support/"))
                .count();
        helper.assertTrue(
                craftingSupport == 0,
                "T46 B1 item scatter must not publish crafting recipes: "
                        + craftingSupport);
        helper.assertTrue(
                gtSupport == FLUID_SUPPORT_RECIPES,
                "T46 fluid support recipes drifted: " + gtSupport);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void t46ItemScatterPlacesFromRuntimeTag(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var registry = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        ConfiguredFeature<?, ?> configured = registry.get(
                ResourceKey.create(Registries.CONFIGURED_FEATURE, SCATTER_FEATURE));
        helper.assertTrue(
                configured != null
                        && configured.config() instanceof ItemScatterConfiguration,
                "Runtime registry lacks decoded t46_bath_mte_scatter");
        ItemScatterConfiguration config = (ItemScatterConfiguration) configured.config();
        helper.assertTrue(config.rarity() == 128, "T46 item scatter rarity drifted");
        helper.assertTrue(
                config.itemTag().equals(SCATTER_ITEMS),
                "T46 item scatter tag drifted");
        ItemStack acquired = scatterOneTaggedItem(helper);
        helper.assertTrue(
                acquired.is(SCATTER_ITEMS),
                "Scatter placed an item outside cruciblecraft:t46_bath_mte_items");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void t46OilSupportExecutes(GameTestHelper helper) {
        executeSupportFluid(
                helper,
                "t46_player_path_support/cruciblecraft_sunflower_oil");
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void t46DyeSupportExecutes(GameTestHelper helper) {
        executeSupportFluid(
                helper,
                "t46_player_path_support/cruciblecraft_dye_flower_red");
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void t46ExactRelationExecutes(GameTestHelper helper) {
        executeAcquiredRepresentative(helper, true);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void t46ExactMultiRelationExecutes(GameTestHelper helper) {
        executeAcquiredRepresentative(helper, false);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void t46WrongIdentityDoesNotStart(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity bath = placeBath(helper);
        helper.assertTrue(
                bath.spec().energy().type() == EnergyType.TIME
                        && bath.spec().energy().mode()
                                == ProcessingMachineSpec.EnergyMode.BUFFERED,
                "Bath is not a buffered TIME host");
        GTRecipe recipe = firstT46Recipe(candidate -> !candidate.fluidInputs().isEmpty());
        ItemStack sample = recipe.itemInputs().getFirst().getItems()[0].copy();
        sample.setCount(Math.max(1, recipe.itemInputCounts().getFirst()));
        bath.inventory().setStackInSlot(bath.spec().items().inputs().getFirst(), sample);
        bath.tanks().get(bath.spec().fluids().inputs().getFirst().index())
                .setFluid(new FluidStack(Fluids.WATER, recipe.fluidInputs().getFirst().getAmount()));
        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> helper.assertTrue(
                        bath.duration() == 0 && bath.workProgressLong() == 0L,
                        "Wrong fluid started a T46 Bath recipe: "
                                + bath.pausedReason()))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void t46WrongItemDoesNotStart(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity bath = placeBath(helper);
        GTRecipe recipe = firstT46Recipe(candidate -> !candidate.fluidInputs().isEmpty());
        bath.inventory().setStackInSlot(
                bath.spec().items().inputs().getFirst(),
                new ItemStack(Items.DIRT));
        bath.tanks().get(bath.spec().fluids().inputs().getFirst().index())
                .setFluid(recipe.fluidInputs().getFirst().copy());
        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> helper.assertTrue(
                        bath.duration() == 0 && bath.workProgressLong() == 0L,
                        "Wrong MTE started a T46 Bath recipe: "
                                + bath.pausedReason()))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void t46StableIdsSurviveReload(GameTestHelper helper) {
        Set<ResourceLocation> first = t46StableIds();
        helper.assertTrue(
                first.size() == LOCKED_RELATIONS,
                "T46 locked ids missing before re-enumeration");
        RecipeMap.RecipeFamily family = ModRecipeMaps.BATH
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.BATH.id(), PUBLICATION_GROUP))
                .orElseThrow();
        helper.assertTrue(
                family.epoch() == ModRecipeMaps.BATH.runtimeEpoch()
                        && family.logicalRecipeCount() == LOCKED_RELATIONS,
                "T46 Bath compact family epoch/count drifted on the live map");
        helper.assertTrue(
                family instanceof CompactRecipeFamilyProvider.Snapshot snapshot
                        && snapshot.shardCount() == LOCKED_RELATIONS
                        && snapshot.overflowRelationCount() == 0,
                "T46 shard count drifted after re-enumeration");
        helper.assertTrue(
                t46StableIds().equals(first),
                "T46 stable ids did not survive a second entries() enumeration");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void t46EmiPlanIncludesFamilies(GameTestHelper helper) {
        Set<ResourceLocation> published = t46StableIds();
        ProcessingEmiRegistrationPlan plan = ProcessingEmiRegistrationPlan.create(
                ModProcessingMachines.CONFIGURED_MACHINES);
        Set<ResourceLocation> emi = plan.recipes().stream()
                .filter(recipe -> recipe.machine().recipeMap().id()
                        .equals(ModRecipeMaps.BATH.id()))
                .map(ProcessingEmiRegistrationPlan.RecipeRegistration::id)
                .collect(Collectors.toCollection(TreeSet::new));
        helper.assertTrue(
                emi.containsAll(published),
                "EMI categories are missing T46 recipe ids: "
                        + published.stream()
                                .filter(id -> !emi.contains(id))
                                .toList());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void t2AndT5BathRecipesRemain(GameTestHelper helper) {
        helper.assertTrue(
                ModRecipeMaps.BATH.entries().stream().anyMatch(entry ->
                        entry.id().getPath().startsWith("bath/crushed_to_washed")),
                "T2 bath/crushed_to_washed is missing after T46 publication");
        helper.assertTrue(
                ModRecipeMaps.BATH.entries().stream().anyMatch(entry ->
                        entry.id().getPath().startsWith("t5/bath/")),
                "T5 Bath recipes are missing after T46 publication");
        helper.succeed();
    }

    private static void executeSupportFluid(GameTestHelper helper, String path) {
        GTRecipe recipe = ModRecipeMaps.BATH.entries().stream()
                .filter(entry -> entry.id().getPath().equals(path))
                .map(RecipeMap.Entry::recipe)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing T46 support recipe " + path));
        helper.assertTrue(
                !recipe.fluidOutputs().isEmpty(),
                "Support recipe has no fluid output: " + path);
        executeRepresentative(helper, recipe);
    }

    private static void executeAcquiredRepresentative(
            GameTestHelper helper, boolean firstExact) {
        ItemStack acquired = scatterOneTaggedItem(helper);
        GTRecipe recipe = acquiredRecipe(acquired, firstExact);
        executeRepresentative(helper, recipe, acquired);
    }

    private static GTRecipe acquiredRecipe(ItemStack acquired, boolean firstExact) {
        List<GTRecipe> matches = ModRecipeMaps.BATH.entries().stream()
                .filter(entry -> entry.id().getPath().startsWith("t46/"))
                .map(RecipeMap.Entry::recipe)
                .filter(candidate -> usesItem(candidate, acquired)
                        && !candidate.fluidInputs().isEmpty())
                .toList();
        if (matches.isEmpty()) {
            throw new IllegalStateException(
                    "Scatter item has no T46 Bath recipe: " + acquired);
        }
        if (firstExact || matches.size() == 1) {
            return matches.getFirst();
        }
        return matches.get(1);
    }

    private static boolean usesItem(GTRecipe recipe, ItemStack acquired) {
        if (recipe.itemInputs().isEmpty()) {
            return false;
        }
        ItemStack[] items = recipe.itemInputs().getFirst().getItems();
        for (ItemStack stack : items) {
            if (stack.is(acquired.getItem())) {
                return true;
            }
        }
        return false;
    }

    private static ItemStack scatterOneTaggedItem(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos chunkOrigin = chunkAlignedOrigin(helper);
        int surfaceY = 64;
        prepareItemScatterPad(level, chunkOrigin, surfaceY);
        ConfiguredFeature<ItemScatterConfiguration, ?> forced =
                new ConfiguredFeature<>(
                        ModFeatures.T46_BATH_MTE_SCATTER.get(),
                        new ItemScatterConfiguration(1, SCATTER_ITEMS));
        helper.assertTrue(
                forced.place(
                        level,
                        level.getChunkSource().getGenerator(),
                        RandomSource.create(1L),
                        chunkOrigin),
                "T46 item scatter did not place any item entity");
        AABB box = new AABB(
                chunkOrigin.getX(),
                surfaceY,
                chunkOrigin.getZ(),
                chunkOrigin.getX() + 16,
                surfaceY + 4,
                chunkOrigin.getZ() + 16);
        List<ItemEntity> entities = level.getEntitiesOfClass(ItemEntity.class, box);
        helper.assertTrue(
                !entities.isEmpty(),
                "T46 item scatter placed no ItemEntity");
        return entities.getFirst().getItem().copy();
    }

    private static BlockPos chunkAlignedOrigin(GameTestHelper helper) {
        BlockPos anchor = helper.absolutePos(BlockPos.ZERO);
        return new BlockPos(
                (anchor.getX() >> 4) << 4,
                0,
                (anchor.getZ() >> 4) << 4);
    }

    private static void prepareItemScatterPad(
            ServerLevel level,
            BlockPos chunkOrigin,
            int surfaceY) {
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                BlockPos surface = chunkOrigin.offset(dx, surfaceY, dz);
                level.setBlock(
                        surface.below(),
                        Blocks.STONE.defaultBlockState(),
                        Block.UPDATE_ALL);
                level.setBlock(
                        surface,
                        Blocks.DIRT.defaultBlockState(),
                        Block.UPDATE_ALL);
                level.setBlock(
                        surface.above(),
                        Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_ALL);
            }
        }
    }

    private static void executeRepresentative(GameTestHelper helper, GTRecipe recipe) {
        executeRepresentative(helper, recipe, ItemStack.EMPTY);
    }

    private static void executeRepresentative(
            GameTestHelper helper, GTRecipe recipe, ItemStack acquiredItem) {
        ConfiguredProcessingMachineBlockEntity bath = placeBath(helper);
        helper.assertTrue(
                bath.spec().energy().type() == EnergyType.TIME
                        && bath.spec().energy().mode()
                                == ProcessingMachineSpec.EnergyMode.BUFFERED,
                "Bath is not a buffered TIME host");
        loadRecipeInputs(bath, recipe, acquiredItem);
        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    helper.assertTrue(
                            bath.workProgressLong() > 0L
                                    || bath.duration() > 0
                                    || hasAnyOutput(bath),
                            "T46 representative Bath recipe was not selected: "
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

    private static GTRecipe firstT46Recipe(
            java.util.function.Predicate<GTRecipe> filter) {
        return ModRecipeMaps.BATH.entries().stream()
                .filter(entry -> entry.id().getPath().startsWith("t46/"))
                .map(RecipeMap.Entry::recipe)
                .filter(filter)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing matching T46 Bath recipe"));
    }

    private static Set<ResourceLocation> t46StableIds() {
        Set<ResourceLocation> ids = new TreeSet<>();
        ModRecipeMaps.BATH.entries().stream()
                .map(RecipeMap.Entry::id)
                .filter(id -> id.getPath().startsWith("t46/"))
                .forEach(ids::add);
        return ids;
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
            if (i == 0 && !acquiredItem.isEmpty()
                    && usesItem(recipe, acquiredItem)) {
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
