package com.masson.cruciblecraft.content.blockentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.machine.processing.GTRecipeFingerprint;
import com.masson.cruciblecraft.machine.processing.MachineExecutionPlan;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.fml.loading.LoadingModList;

class ProcessingMachineSelectionTest {
    private static final ResourceLocation MAP_ID = id("selection_map");
    private static final ResourceLocation RECIPE_ID = id("selection_recipe");
    private static final MachineExecutionPlan PLAN =
            new MachineExecutionPlan(8L, 8L, 32L, 160L, 20, 1, 0);
    private static RegistryAccess registries;

    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(
                List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        registries = new RegistryAccess.ImmutableRegistryAccess(
                BuiltInRegistries.REGISTRY.stream().toList());
    }

    @Test
    void nonBufferableRematerializedRecipeKeepsProgressByFingerprint() {
        TestMachine machine = new TestMachine();
        GTRecipe first = recipe(Items.IRON_NUGGET);
        GTRecipe rematerialized = recipe(Items.IRON_NUGGET);
        String fingerprint = fingerprint(first);

        assertFalse(first.canBeBuffered());
        assertNotSame(first, rematerialized);
        assertEquals(fingerprint, fingerprint(rematerialized));

        machine.select(
                new RecipeMap.Match(RECIPE_ID, first),
                1,
                PLAN,
                () -> Optional.of(fingerprint));
        machine.runtime().restore(
                RECIPE_ID.toString(), 7, PLAN.effectiveDuration(), "");

        machine.select(
                new RecipeMap.Match(RECIPE_ID, rematerialized),
                1,
                PLAN,
                () -> Optional.of(fingerprint));

        assertEquals(RECIPE_ID.toString(),
                machine.runtime().processor().activeId());
        assertEquals(7, machine.progress());
        assertEquals(
                fingerprint,
                machine.saveForTest()
                        .getString("selected_recipe_fingerprint"));
    }

    @Test
    void sameIdWithDifferentFingerprintResetsProgress() {
        TestMachine machine = new TestMachine();
        GTRecipe first = recipe(Items.IRON_NUGGET);
        GTRecipe changed = recipe(Items.GOLD_NUGGET);
        String firstFingerprint = fingerprint(first);
        String changedFingerprint = fingerprint(changed);
        assertNotEquals(firstFingerprint, changedFingerprint);

        selectAndRestore(machine, first, firstFingerprint);
        machine.select(
                new RecipeMap.Match(RECIPE_ID, changed),
                1,
                PLAN,
                () -> Optional.of(changedFingerprint));

        assertEquals("", machine.runtime().processor().activeId());
        assertEquals(0, machine.progress());
    }

    @Test
    void mapRevisionOperationsAndPlanAllParticipateInIdentity() {
        GTRecipe recipe = recipe(Items.IRON_NUGGET);
        String fingerprint = fingerprint(recipe);

        TestMachine revised = new TestMachine();
        selectAndRestore(revised, recipe, fingerprint);
        revised.map.replaceRecipes(List.of());
        revised.select(
                new RecipeMap.Match(RECIPE_ID, recipe),
                1,
                PLAN,
                () -> Optional.of(fingerprint));
        assertEquals(0, revised.progress());

        TestMachine parallel = new TestMachine();
        selectAndRestore(parallel, recipe, fingerprint);
        parallel.select(
                new RecipeMap.Match(RECIPE_ID, recipe),
                2,
                PLAN,
                () -> Optional.of(fingerprint));
        assertEquals(0, parallel.progress());

        TestMachine replanned = new TestMachine();
        selectAndRestore(replanned, recipe, fingerprint);
        MachineExecutionPlan changedPlan =
                new MachineExecutionPlan(
                        8L, 8L, 32L, 168L, 21, 1, 0);
        replanned.select(
                new RecipeMap.Match(RECIPE_ID, recipe),
                1,
                changedPlan,
                () -> Optional.of(fingerprint));
        assertEquals(0, replanned.progress());
    }

    @Test
    void identicalRecipeReferenceSkipsFingerprintWork() {
        TestMachine machine = new TestMachine();
        GTRecipe recipe = recipe(Items.IRON_NUGGET);
        String fingerprint = fingerprint(recipe);
        machine.select(
                new RecipeMap.Match(RECIPE_ID, recipe),
                1,
                PLAN,
                () -> Optional.of(fingerprint));
        machine.runtime().restore(
                RECIPE_ID.toString(), 7, PLAN.effectiveDuration(), "");

        AtomicInteger fingerprintCalls = new AtomicInteger();
        machine.select(
                new RecipeMap.Match(RECIPE_ID, recipe),
                1,
                PLAN,
                () -> {
                    fingerprintCalls.incrementAndGet();
                    return Optional.of(fingerprint);
                });

        assertEquals(0, fingerprintCalls.get());
        assertEquals(7, machine.progress());
    }

    @Test
    void rematerializedRecipePaysFingerprintCostOnce() {
        TestMachine machine = new TestMachine();
        GTRecipe first = recipe(Items.IRON_NUGGET);
        GTRecipe rematerialized = recipe(Items.IRON_NUGGET);
        String fingerprint = fingerprint(first);
        selectAndRestore(machine, first, fingerprint);

        AtomicInteger fingerprintCalls = new AtomicInteger();
        machine.select(
                new RecipeMap.Match(RECIPE_ID, rematerialized),
                1,
                PLAN,
                () -> {
                    fingerprintCalls.incrementAndGet();
                    return Optional.of(fingerprint);
                });

        assertEquals(1, fingerprintCalls.get());
        assertEquals(7, machine.progress());
    }

    private static void selectAndRestore(
            TestMachine machine,
            GTRecipe recipe,
            String fingerprint) {
        machine.select(
                new RecipeMap.Match(RECIPE_ID, recipe),
                1,
                PLAN,
                () -> Optional.of(fingerprint));
        machine.runtime().restore(
                RECIPE_ID.toString(), 7, PLAN.effectiveDuration(), "");
    }

    private static GTRecipe recipe(net.minecraft.world.item.Item output) {
        return new GTRecipe(
                List.of(Ingredient.of(Items.IRON_INGOT)),
                List.of(1),
                List.of(new ItemStack(output)),
                List.of(),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                20,
                8L,
                0L,
                false);
    }

    private static String fingerprint(GTRecipe recipe) {
        return GTRecipeFingerprint.recipe(MAP_ID, recipe, registries)
                .orElseThrow();
    }

    private static ProcessingMachineSpec spec(RecipeMap map) {
        return new ProcessingMachineSpec(
                id("selection_machine"),
                MAP_ID,
                () -> map,
                new ProcessingMachineSpec.SlotLayout(
                        2, List.of(0), List.of(1)),
                new ProcessingMachineSpec.TankLayout(
                        List.of(), List.of()),
                new ProcessingMachineSpec.EnergySpec(
                        EnergyType.KINETIC_PUSH,
                        ProcessingMachineSpec.EnergyMode.ADJACENT,
                        0L,
                        32L),
                new ProcessingMachineSpec.SidedIoPolicy(
                        (front, side) ->
                                ProcessingMachineSpec.CapabilityAccess.NONE,
                        (front, side) ->
                                ProcessingMachineSpec.CapabilityAccess.NONE,
                        (front, side) ->
                                ProcessingMachineSpec.CapabilityAccess.NONE),
                recipe -> Optional.empty(),
                ProcessingMachineSpec.BufferPolicy.PAUSE,
                new ProcessingMachineSpec.UiLayout(
                        List.of(
                                new ProcessingMachineSpec.SlotPosition(0, 0),
                                new ProcessingMachineSpec.SlotPosition(18, 0)),
                        new ProcessingMachineSpec.ProgressBar(0, 18, 16, 4),
                        List.of(),
                        List.of()));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }

    private static final class TestMachine
            extends ProcessingMachineBlockEntity {
        private final RecipeMap map;

        private TestMachine() {
            this(new RecipeMap(MAP_ID));
        }

        private TestMachine(RecipeMap map) {
            super(
                    BlockEntityType.FURNACE,
                    BlockPos.ZERO,
                    Blocks.FURNACE.defaultBlockState(),
                    ProcessingMachineSelectionTest.spec(map));
            this.map = map;
        }

        private CompoundTag saveForTest() {
            CompoundTag tag = new CompoundTag();
            saveAdditional(tag, registries);
            return tag;
        }

        @Override
        protected Direction machineFront() {
            return Direction.NORTH;
        }
    }
}
