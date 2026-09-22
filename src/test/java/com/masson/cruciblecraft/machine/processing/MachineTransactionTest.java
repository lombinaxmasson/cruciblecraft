package com.masson.cruciblecraft.machine.processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import com.masson.cruciblecraft.TestExtruderShapes;
import com.masson.cruciblecraft.content.item.ExtruderShapeCatalog;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.GTRecipeCache;
import com.masson.cruciblecraft.recipe.gt.ItemInputAction;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;

import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.fluids.FluidStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MachineTransactionTest {
    @BeforeAll
    static void bootstrap() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void multiSlotItemAndFluidTransactionCommitsAllResources() {
        GTRecipe recipe = recipe();
        MemoryResources resources = resources();
        MachineTransaction transaction = MachineTransaction.prepare(
                recipe,
                resources.items,
                List.of(0, 1),
                List.of(2, 3),
                resources.fluids,
                List.of(new ProcessingMachineSpec.TankSpec(0, 1000)),
                List.of(new ProcessingMachineSpec.TankSpec(1, 1000)),
                recipe.itemOutputs()).orElseThrow();

        assertTrue(transaction.commit(resources));
        assertTrue(resources.items.get(0).isEmpty());
        assertTrue(resources.items.get(1).isEmpty());
        assertEquals(2, resources.items.get(2).getCount());
        assertEquals(1, resources.items.get(3).getCount());
        assertTrue(resources.fluids.get(0).isEmpty());
        assertEquals(500, resources.fluids.get(1).getAmount());
    }

    @Test
    void indexedOutputsStayInTheirGt6Slots() {
        GTRecipe duplicateOutputs = new GTRecipe(
                List.of(Ingredient.of(Items.COAL)),
                List.of(1),
                List.of(new ItemStack(Items.DIAMOND), new ItemStack(Items.DIAMOND)),
                List.of(),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE, GTRecipe.GUARANTEED_CHANCE),
                20,
                16,
                0,
                true);
        MemoryResources resources = new MemoryResources(
                List.of(new ItemStack(Items.COAL), ItemStack.EMPTY, ItemStack.EMPTY),
                List.of());

        MachineTransaction transaction = MachineTransaction.prepareIndexed(
                duplicateOutputs,
                resources.items,
                List.of(0),
                List.of(1, 2),
                resources.fluids,
                List.of(),
                List.of(),
                duplicateOutputs.itemOutputs()).orElseThrow();

        assertTrue(transaction.commit(resources));
        assertEquals(1, resources.items.get(1).getCount());
        assertEquals(1, resources.items.get(2).getCount());

        MemoryResources blocked = new MemoryResources(
                List.of(
                        new ItemStack(Items.COAL),
                        new ItemStack(Items.DIAMOND, 64),
                        ItemStack.EMPTY),
                List.of());
        assertTrue(MachineTransaction.prepareIndexed(
                duplicateOutputs,
                blocked.items,
                List.of(0),
                List.of(1, 2),
                blocked.fluids,
                List.of(),
                List.of(),
                duplicateOutputs.itemOutputs()).isEmpty());
        assertEquals(1, blocked.items.get(0).getCount());
        assertTrue(blocked.items.get(2).isEmpty());
    }

    @Test
    void indexedChanceRollPreservesEmptyAndDuplicateOutputPositions() {
        GTRecipe duplicateOutputs = new GTRecipe(
                List.of(Ingredient.of(Items.COAL)),
                List.of(1),
                List.of(new ItemStack(Items.DIAMOND), new ItemStack(Items.DIAMOND)),
                List.of(),
                List.of(),
                List.of(0, GTRecipe.GUARANTEED_CHANCE),
                20,
                16,
                0,
                true);

        List<ItemStack> rolled =
                ParallelRecipeOperations.rollItemOutputsIndexed(
                        duplicateOutputs, 1, bound -> 0);

        assertEquals(2, rolled.size());
        assertTrue(rolled.get(0).isEmpty());
        assertEquals(Items.DIAMOND, rolled.get(1).getItem());
    }

    @Test
    void capacityChangeRejectsCommitWithoutConsumingInputs() {
        GTRecipe recipe = recipe();
        MemoryResources resources = resources();
        MachineTransaction transaction = MachineTransaction.prepare(
                recipe,
                resources.items,
                List.of(0, 1),
                List.of(2, 3),
                resources.fluids,
                List.of(new ProcessingMachineSpec.TankSpec(0, 1000)),
                List.of(new ProcessingMachineSpec.TankSpec(1, 1000)),
                recipe.itemOutputs()).orElseThrow();
        resources.items.set(2, new ItemStack(Items.DIAMOND, 64));

        assertFalse(transaction.commit(resources));
        assertEquals(2, resources.items.get(0).getCount());
        assertEquals(1, resources.items.get(1).getCount());
        assertEquals(1000, resources.fluids.get(0).getAmount());
    }

    @Test
    void assemblerCableInputsCommitAtomicallyAndRollbackWhenOutputChanges() {
        GTRecipe cable = new GTRecipe(
                List.of(Ingredient.of(Items.COPPER_INGOT), Ingredient.of(Items.SLIME_BALL)),
                List.of(1, 1),
                List.of(new ItemStack(Items.REDSTONE)),
                List.of(), List.of(), List.of(10_000), 120, 32, 0, true);
        MemoryResources resources = new MemoryResources(
                List.of(
                        new ItemStack(Items.COPPER_INGOT),
                        new ItemStack(Items.SLIME_BALL),
                        ItemStack.EMPTY),
                List.of());
        MachineTransaction blocked = MachineTransaction.prepare(
                cable, resources.items, List.of(0, 1), List.of(2),
                resources.fluids, List.of(), List.of(), cable.itemOutputs()).orElseThrow();
        resources.items.set(2, new ItemStack(Items.REDSTONE, 64));
        assertFalse(blocked.commit(resources));
        assertEquals(1, resources.items.get(0).getCount());
        assertEquals(1, resources.items.get(1).getCount());

        resources.items.set(2, ItemStack.EMPTY);
        MachineTransaction ready = MachineTransaction.prepare(
                cable, resources.items, List.of(0, 1), List.of(2),
                resources.fluids, List.of(), List.of(), cable.itemOutputs()).orElseThrow();
        assertTrue(ready.commit(resources));
        assertTrue(resources.items.get(0).isEmpty());
        assertTrue(resources.items.get(1).isEmpty());
        assertEquals(Items.REDSTONE, resources.items.get(2).getItem());
    }

    @Test
    void extruderPresenceToolSurvivesSuccessAndBlockedOutputWithComponents() {
        ItemStack shape = TestExtruderShapes.stack();
        shape.set(
                net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                net.minecraft.network.chat.Component.literal("kept"));
        GTRecipe recipe = new GTRecipe(
                List.of(Ingredient.of(Items.IRON_INGOT), Ingredient.of(shape.getItem())),
                List.of(1, 0),
                List.of(new ItemStack(Items.IRON_NUGGET, 2)),
                List.of(), List.of(), List.of(10_000), 20, 16, 0, true);
        MemoryResources ready = new MemoryResources(
                List.of(new ItemStack(Items.IRON_INGOT), shape, ItemStack.EMPTY),
                List.of());

        MachineTransaction transaction = MachineTransaction.prepare(
                recipe, ready.items, List.of(0, 1), List.of(2),
                ready.fluids, List.of(), List.of(), recipe.itemOutputs()).orElseThrow();
        assertTrue(transaction.commit(ready));
        assertTrue(ready.items.get(0).isEmpty());
        assertEquals(1, ready.items.get(1).getCount());
        assertTrue(ItemStack.isSameItemSameComponents(shape, ready.items.get(1)));
        assertEquals(1, ready.items.stream()
                .filter(ExtruderShapeCatalog::isShape)
                .mapToInt(ItemStack::getCount)
                .sum(), "successful transaction must not copy the presence-only shape");
        assertFalse(ExtruderShapeCatalog.isShape(ready.items.get(2)));

        MemoryResources blocked = new MemoryResources(
                List.of(
                        new ItemStack(Items.IRON_INGOT),
                        shape,
                        new ItemStack(Items.DIAMOND, 64)),
                List.of());
        assertTrue(MachineTransaction.prepare(
                recipe, blocked.items, List.of(0, 1), List.of(2),
                blocked.fluids, List.of(), List.of(), recipe.itemOutputs()).isEmpty());
        assertEquals(1, blocked.items.get(0).getCount());
        assertTrue(ItemStack.isSameItemSameComponents(shape, blocked.items.get(1)));
        assertEquals(1, blocked.items.stream()
                .filter(ExtruderShapeCatalog::isShape)
                .mapToInt(ItemStack::getCount)
                .sum(), "blocked output must leave exactly the original shape");
        assertEquals(Items.DIAMOND, blocked.items.get(2).getItem());
    }

    @Test
    void wearActionDamagesOnceAndRejectsBreakingOrBlockedTransactions() {
        GTRecipe wearing = new GTRecipe(
                List.of(
                        Ingredient.of(Items.IRON_INGOT),
                        Ingredient.of(Items.IRON_PICKAXE)),
                List.of(1, 0),
                List.of(ItemInputAction.CONSUME, ItemInputAction.wear(1)),
                List.of(new ItemStack(Items.IRON_NUGGET)),
                List.of(),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                20,
                16,
                0,
                true,
                Optional.empty());
        ItemStack tool = new ItemStack(Items.IRON_PICKAXE);
        MemoryResources ready = new MemoryResources(
                List.of(new ItemStack(Items.IRON_INGOT), tool, ItemStack.EMPTY),
                List.of());
        MachineTransaction transaction = MachineTransaction.prepare(
                wearing,
                ready.items,
                List.of(0, 1),
                List.of(2),
                ready.fluids,
                List.of(),
                List.of(),
                wearing.itemOutputs()).orElseThrow();
        assertEquals(0, ready.items.get(1).getDamageValue());
        assertTrue(transaction.commit(ready));
        assertEquals(1, ready.items.get(1).getCount());
        assertEquals(1, ready.items.get(1).getDamageValue());

        MemoryResources blocked = new MemoryResources(
                List.of(
                        new ItemStack(Items.IRON_INGOT),
                        tool,
                        new ItemStack(Items.DIAMOND, 64)),
                List.of());
        assertTrue(MachineTransaction.prepare(
                wearing,
                blocked.items,
                List.of(0, 1),
                List.of(2),
                blocked.fluids,
                List.of(),
                List.of(),
                wearing.itemOutputs()).isEmpty());
        assertEquals(0, blocked.items.get(1).getDamageValue());

        ItemStack almostBroken = new ItemStack(Items.IRON_PICKAXE);
        almostBroken.setDamageValue(almostBroken.getMaxDamage() - 1);
        MemoryResources breaking = new MemoryResources(
                List.of(
                        new ItemStack(Items.IRON_INGOT),
                        almostBroken,
                        ItemStack.EMPTY),
                List.of());
        assertTrue(MachineTransaction.prepare(
                wearing,
                breaking.items,
                List.of(0, 1),
                List.of(2),
                breaking.fluids,
                List.of(),
                List.of(),
                wearing.itemOutputs()).isEmpty());
        assertEquals(1, breaking.items.get(0).getCount());
        assertEquals(almostBroken.getMaxDamage() - 1,
                breaking.items.get(1).getDamageValue());
        assertTrue(breaking.items.get(2).isEmpty());
    }

    @Test
    void multipleWearAndPreservedPatternCommitOrRollBackTogether() {
        GTRecipe recipe = new GTRecipe(
                List.of(
                        Ingredient.of(Items.IRON_INGOT),
                        Ingredient.of(Items.IRON_PICKAXE),
                        Ingredient.of(Items.IRON_AXE),
                        Ingredient.of(Items.TRIAL_KEY)),
                List.of(1, 0, 0, 0),
                List.of(
                        ItemInputAction.CONSUME,
                        ItemInputAction.wear(2),
                        ItemInputAction.wear(3),
                        ItemInputAction.PRESERVE),
                List.of(new ItemStack(Items.IRON_NUGGET)),
                List.of(),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                20,
                16,
                0,
                true,
                Optional.empty());
        ItemStack pattern = new ItemStack(Items.TRIAL_KEY);
        pattern.set(
                net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                net.minecraft.network.chat.Component.literal("preserved"));
        MemoryResources ready = new MemoryResources(
                List.of(
                        new ItemStack(Items.IRON_INGOT),
                        new ItemStack(Items.IRON_PICKAXE),
                        new ItemStack(Items.IRON_AXE),
                        pattern,
                        ItemStack.EMPTY),
                List.of());

        MachineTransaction transaction = MachineTransaction.prepare(
                recipe,
                ready.items,
                List.of(0, 1, 2, 3),
                List.of(4),
                ready.fluids,
                List.of(),
                List.of(),
                recipe.itemOutputs()).orElseThrow();
        assertTrue(transaction.commit(ready));
        assertTrue(ready.items.get(0).isEmpty());
        assertEquals(2, ready.items.get(1).getDamageValue());
        assertEquals(3, ready.items.get(2).getDamageValue());
        assertTrue(ItemStack.isSameItemSameComponents(pattern, ready.items.get(3)));
        assertEquals(Items.IRON_NUGGET, ready.items.get(4).getItem());

        ItemStack failingAxe = new ItemStack(Items.IRON_AXE);
        failingAxe.setDamageValue(failingAxe.getMaxDamage() - 2);
        MemoryResources failing = new MemoryResources(
                List.of(
                        new ItemStack(Items.IRON_INGOT),
                        new ItemStack(Items.IRON_PICKAXE),
                        failingAxe,
                        pattern,
                        ItemStack.EMPTY),
                List.of());
        assertTrue(MachineTransaction.prepare(
                recipe,
                failing.items,
                List.of(0, 1, 2, 3),
                List.of(4),
                failing.fluids,
                List.of(),
                List.of(),
                recipe.itemOutputs()).isEmpty());
        assertEquals(1, failing.items.get(0).getCount());
        assertEquals(0, failing.items.get(1).getDamageValue());
        assertEquals(failingAxe.getMaxDamage() - 2,
                failing.items.get(2).getDamageValue());
        assertTrue(ItemStack.isSameItemSameComponents(pattern, failing.items.get(3)));
        assertTrue(failing.items.get(4).isEmpty());
    }

    @Test
    void multiTankFluidInputAndOutputMergeSplitConserveExactAmounts() {
        GTRecipe recipe = splitFluidRecipe();
        MemoryResources resources = splitFluidResources();
        int waterBefore = fluidAmount(resources.fluids, Fluids.WATER);
        int lavaBefore = fluidAmount(resources.fluids, Fluids.LAVA);

        MachineTransaction transaction = MachineTransaction.prepare(
                recipe,
                resources.items,
                List.of(0),
                List.of(1),
                resources.fluids,
                List.of(
                        new ProcessingMachineSpec.TankSpec(0, 1000),
                        new ProcessingMachineSpec.TankSpec(1, 1000)),
                List.of(
                        new ProcessingMachineSpec.TankSpec(2, 1000),
                        new ProcessingMachineSpec.TankSpec(3, 500)),
                recipe.itemOutputs()).orElseThrow();

        assertTrue(transaction.commit(resources));
        assertTrue(resources.items.get(0).isEmpty());
        assertEquals(Items.IRON_NUGGET, resources.items.get(1).getItem());
        assertTrue(resources.fluids.get(0).isEmpty());
        assertEquals(300, resources.fluids.get(1).getAmount());
        assertEquals(1000, resources.fluids.get(2).getAmount());
        assertEquals(500, resources.fluids.get(3).getAmount());
        assertEquals(1000,
                waterBefore - fluidAmount(resources.fluids, Fluids.WATER));
        assertEquals(700,
                fluidAmount(resources.fluids, Fluids.LAVA) - lavaBefore);
    }

    @Test
    void blockedMultiTankFluidOutputsRejectWithoutConsumingAnything() {
        GTRecipe recipe = splitFluidRecipe();
        MemoryResources blocked = new MemoryResources(
                List.of(new ItemStack(Items.COAL), ItemStack.EMPTY),
                List.of(
                        new FluidStack(Fluids.WATER, 600),
                        new FluidStack(Fluids.WATER, 700),
                        new FluidStack(Fluids.LAVA, 1000),
                        new FluidStack(Fluids.WATER, 500)));
        List<ItemStack> beforeItems = blocked.items.stream().map(ItemStack::copy).toList();
        List<FluidStack> beforeFluids =
                blocked.fluids.stream().map(FluidStack::copy).toList();

        assertTrue(MachineTransaction.prepare(
                recipe,
                blocked.items,
                List.of(0),
                List.of(1),
                blocked.fluids,
                List.of(
                        new ProcessingMachineSpec.TankSpec(0, 1000),
                        new ProcessingMachineSpec.TankSpec(1, 1000)),
                List.of(
                        new ProcessingMachineSpec.TankSpec(2, 1000),
                        new ProcessingMachineSpec.TankSpec(3, 500)),
                recipe.itemOutputs()).isEmpty());

        assertResourceState(beforeItems, beforeFluids, blocked);
        assertEquals(1300,
                blocked.fluids.get(0).getAmount()
                        + blocked.fluids.get(1).getAmount());
        assertEquals(1000, fluidAmount(blocked.fluids, Fluids.LAVA));
    }

    @Test
    void staleLiveFluidRejectsCommitWithoutApplyingAnyPreparedDelta() {
        GTRecipe recipe = splitFluidRecipe();
        MemoryResources resources = splitFluidResources();
        MachineTransaction transaction = MachineTransaction.prepare(
                recipe,
                resources.items,
                List.of(0),
                List.of(1),
                resources.fluids,
                List.of(
                        new ProcessingMachineSpec.TankSpec(0, 1000),
                        new ProcessingMachineSpec.TankSpec(1, 1000)),
                List.of(
                        new ProcessingMachineSpec.TankSpec(2, 1000),
                        new ProcessingMachineSpec.TankSpec(3, 500)),
                recipe.itemOutputs()).orElseThrow();
        resources.fluids.set(1, new FluidStack(Fluids.WATER, 699));
        List<ItemStack> staleItems = resources.items.stream().map(ItemStack::copy).toList();
        List<FluidStack> staleFluids =
                resources.fluids.stream().map(FluidStack::copy).toList();

        assertFalse(transaction.commit(resources));
        assertResourceState(staleItems, staleFluids, resources);
        assertEquals(1299, fluidAmount(resources.fluids, Fluids.WATER));
        assertEquals(800, fluidAmount(resources.fluids, Fluids.LAVA));
    }

    @Test
    void chanceOutputsRollExactlyOnceWithDeterministicRng() {
        AtomicInteger calls = new AtomicInteger();
        List<ItemStack> rolled = ChanceOutputs.roll(
                List.of(
                        new ItemStack(Items.IRON_INGOT),
                        new ItemStack(Items.GOLD_INGOT),
                        new ItemStack(Items.DIAMOND)),
                List.of(10_000, 5_000, 0),
                bound -> {
                    calls.incrementAndGet();
                    return 4_999;
                });
        assertEquals(1, calls.get());
        assertEquals(List.of(Items.IRON_INGOT, Items.GOLD_INGOT),
                rolled.stream().map(ItemStack::getItem).toList());
    }

    @Test
    void stackChanceKeepsWholeCountAndSkipsRngAtBounds() {
        AtomicInteger calls = new AtomicInteger();
        ItemStack template = new ItemStack(Items.DIAMOND, 7);
        assertTrue(ChanceOutputs.roll(
                template, 0, bound -> calls.incrementAndGet()).isEmpty());
        assertEquals(7, ChanceOutputs.roll(
                template, 10_000, bound -> calls.incrementAndGet()).getCount());
        assertEquals(0, calls.get());
        assertEquals(7, ChanceOutputs.roll(
                template, 5_000, bound -> {
                    calls.incrementAndGet();
                    return 4_999;
                }).getCount());
        assertEquals(1, calls.get());
    }

    @Test
    void selectedChanceRollRoundTripsIncludingValidEmptyOutcome() {
        ChanceOutputState selected = new ChanceOutputState(
                true,
                "test:chance",
                "fingerprint",
                List.of(new ItemStack(Items.DIAMOND, 4)));
        CompoundTag tag = new CompoundTag();
        selected.write(tag, RegistryAccess.EMPTY);
        ChanceOutputState restored = ChanceOutputState.read(tag, RegistryAccess.EMPTY);
        assertTrue(restored.valid());
        assertEquals("test:chance", restored.recipeId());
        assertEquals(4, restored.outputs().getFirst().getCount());
        assertTrue(restored.matches("test:chance", "fingerprint"));

        ChanceOutputState empty = new ChanceOutputState(
                true, "test:failed_chance", "empty", List.of());
        CompoundTag emptyTag = new CompoundTag();
        empty.write(emptyTag, RegistryAccess.EMPTY);
        ChanceOutputState restoredEmpty =
                ChanceOutputState.read(emptyTag, RegistryAccess.EMPTY);
        assertTrue(restoredEmpty.valid());
        assertTrue(restoredEmpty.outputs().isEmpty());

        ChanceOutputState indexed = new ChanceOutputState(
                true,
                "test:indexed",
                "indexed",
                List.of(ItemStack.EMPTY, new ItemStack(Items.DIAMOND)));
        CompoundTag indexedTag = new CompoundTag();
        indexed.write(indexedTag, RegistryAccess.EMPTY);
        ChanceOutputState restoredIndexed =
                ChanceOutputState.read(indexedTag, RegistryAccess.EMPTY);
        assertEquals(2, restoredIndexed.outputs().size());
        assertTrue(restoredIndexed.outputs().get(0).isEmpty());
        assertEquals(Items.DIAMOND, restoredIndexed.outputs().get(1).getItem());
    }

    @Test
    void invalidChanceStateRoundTripsAndCorruptCountsInvalidateConservatively() {
        ChanceOutputState invalid = new ChanceOutputState(
                false,
                "test:invalid",
                "fingerprint",
                List.of(new ItemStack(Items.DIAMOND, 2)));
        CompoundTag invalidTag = new CompoundTag();
        invalid.write(invalidTag, RegistryAccess.EMPTY);
        ChanceOutputState restored =
                ChanceOutputState.read(invalidTag, RegistryAccess.EMPTY);
        assertFalse(restored.valid());
        assertEquals(2, restored.outputs().getFirst().getCount());

        for (int count : List.of(-1, ChanceOutputState.MAX_SAVED_OUTPUTS + 1)) {
            CompoundTag corrupt = new CompoundTag();
            corrupt.putBoolean("rolled_outputs_valid", true);
            corrupt.putString("selected_recipe_id", "test:corrupt");
            corrupt.putString("selected_recipe_fingerprint", "fingerprint");
            corrupt.putInt("rolled_output_count", count);
            ChanceOutputState recovered =
                    ChanceOutputState.read(corrupt, RegistryAccess.EMPTY);
            assertFalse(recovered.valid());
            assertTrue(recovered.outputs().isEmpty());
        }
    }

    @Test
    void chanceStateRejectsMoreOutputsThanTheReaderCanRestore() {
        List<ItemStack> outputs = java.util.Collections.nCopies(
                ChanceOutputState.MAX_SAVED_OUTPUTS + 1,
                new ItemStack(Items.DIAMOND));

        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> new ChanceOutputState(
                        true,
                        "test:oversized",
                        "fingerprint",
                        outputs));

        assertTrue(failure.getMessage().contains(
                Integer.toString(ChanceOutputState.MAX_SAVED_OUTPUTS + 1)));
        assertTrue(failure.getMessage().contains(
                Integer.toString(ChanceOutputState.MAX_SAVED_OUTPUTS)));
    }

    @Test
    void recipeMapRevisionInvalidatesCachedMatch() {
        ResourceLocation mapId = ResourceLocation.fromNamespaceAndPath("test", "processing");
        ResourceLocation recipeId = ResourceLocation.fromNamespaceAndPath("test", "coal");
        RecipeMap map = new RecipeMap(mapId);
        map.replaceRecipes(List.of(new RecipeMap.Entry(recipeId, recipe())));
        GTRecipeCache cache = new GTRecipeCache(map);
        assertTrue(cache.find(
                List.of(new ItemStack(Items.COAL, 2), new ItemStack(Items.STICK)),
                List.of(new FluidStack(Fluids.WATER, 1000))).isPresent());
        map.replaceRecipes(List.of());
        assertTrue(cache.find(
                List.of(new ItemStack(Items.COAL, 2), new ItemStack(Items.STICK)),
                List.of(new FluidStack(Fluids.WATER, 1000))).isEmpty());
    }

    private static GTRecipe recipe() {
        return new GTRecipe(
                List.of(Ingredient.of(Items.COAL), Ingredient.of(Items.STICK)),
                List.of(2, 1),
                List.of(
                        new ItemStack(Items.IRON_INGOT, 2),
                        new ItemStack(Items.GOLD_INGOT)),
                List.of(new FluidStack(Fluids.WATER, 1000)),
                List.of(new FluidStack(Fluids.LAVA, 500)),
                List.of(10_000, 10_000),
                2,
                16,
                0,
                true);
    }

    private static MemoryResources resources() {
        return new MemoryResources(
                List.of(
                        new ItemStack(Items.COAL, 2),
                        new ItemStack(Items.STICK),
                        ItemStack.EMPTY,
                        ItemStack.EMPTY),
                List.of(new FluidStack(Fluids.WATER, 1000), FluidStack.EMPTY));
    }

    private static GTRecipe splitFluidRecipe() {
        return new GTRecipe(
                List.of(Ingredient.of(Items.COAL)),
                List.of(1),
                List.of(new ItemStack(Items.IRON_NUGGET)),
                List.of(new FluidStack(Fluids.WATER, 1000)),
                List.of(new FluidStack(Fluids.LAVA, 700)),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                20,
                16,
                0,
                true);
    }

    private static MemoryResources splitFluidResources() {
        return new MemoryResources(
                List.of(new ItemStack(Items.COAL), ItemStack.EMPTY),
                List.of(
                        new FluidStack(Fluids.WATER, 600),
                        new FluidStack(Fluids.WATER, 700),
                        new FluidStack(Fluids.LAVA, 800),
                        FluidStack.EMPTY));
    }

    private static int fluidAmount(
            List<FluidStack> fluids,
            net.minecraft.world.level.material.Fluid fluid) {
        return fluids.stream()
                .filter(stack -> stack.is(fluid))
                .mapToInt(FluidStack::getAmount)
                .sum();
    }

    private static void assertResourceState(
            List<ItemStack> expectedItems,
            List<FluidStack> expectedFluids,
            MemoryResources actual) {
        for (int slot = 0; slot < expectedItems.size(); slot++) {
            ItemStack expected = expectedItems.get(slot);
            ItemStack observed = actual.items.get(slot);
            assertEquals(expected.getCount(), observed.getCount());
            assertTrue(expected.isEmpty() && observed.isEmpty()
                    || ItemStack.isSameItemSameComponents(expected, observed));
        }
        for (int tank = 0; tank < expectedFluids.size(); tank++) {
            FluidStack expected = expectedFluids.get(tank);
            FluidStack observed = actual.fluids.get(tank);
            assertEquals(expected.getAmount(), observed.getAmount());
            assertTrue(expected.isEmpty() && observed.isEmpty()
                    || FluidStack.isSameFluidSameComponents(expected, observed));
        }
    }

    private static final class MemoryResources implements MachineTransaction.ResourceAccess {
        private final List<ItemStack> items;
        private final List<FluidStack> fluids;

        private MemoryResources(List<ItemStack> items, List<FluidStack> fluids) {
            this.items = new ArrayList<>(items.stream().map(ItemStack::copy).toList());
            this.fluids = new ArrayList<>(fluids.stream().map(FluidStack::copy).toList());
        }
        @Override public int itemCount() { return items.size(); }
        @Override public ItemStack item(int slot) { return items.get(slot).copy(); }
        @Override public void setItem(int slot, ItemStack stack) { items.set(slot, stack.copy()); }
        @Override public int fluidCount() { return fluids.size(); }
        @Override public FluidStack fluid(int tank) { return fluids.get(tank).copy(); }
        @Override public void setFluid(int tank, FluidStack stack) {
            fluids.set(tank, stack.copy());
        }
    }
}
