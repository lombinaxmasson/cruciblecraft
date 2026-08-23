package com.masson.cruciblecraft.machine.processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.ItemInputAction;
import com.masson.cruciblecraft.registry.ModMachineVariants;
import com.masson.cruciblecraft.registry.ModMultiblockControllers;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.fml.loading.LoadingModList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class T12MachineTierArchitectureTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(
                List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void bundledCatalogDeclaresEverySelectedKindAcrossThreeTiers() {
        assertTrue(ModMachineVariants.ALL.size() > 33);
        assertEquals(27, ModMachineVariants.KINDS.size());
        for (var kind : ModMachineVariants.T16_SELECTED_KINDS) {
            List<MachineVariant> variants =
                    ModMachineVariants.T16_SELECTED.stream()
                            .filter(variant -> variant.kind().id().equals(kind.id()))
                            .toList();
            assertEquals(3, variants.size());
            variants.forEach(variant -> {
                assertSame(kind.behavior().items(),
                        variant.runtimeSpec().items());
                assertSame(kind.behavior().fluids(),
                        variant.runtimeSpec().fluids());
                assertSame(kind.behavior().validator(),
                        variant.runtimeSpec().validator());
                assertSame(kind.behavior().sidedIo(),
                        variant.runtimeSpec().sidedIo());
                assertSame(kind.behavior().ui(),
                        variant.runtimeSpec().ui());
            });
        }
        for (var kind : ModMachineVariants.KINDS) {
            List<MachineVariant> variants =
                    ModMachineVariants.forKind(kind.id());
            assertFalse(variants.isEmpty(), kind.id().toString());
            variants.forEach(variant -> {
                assertSame(kind.behavior().items(),
                        variant.runtimeSpec().items());
                assertSame(kind.behavior().fluids(),
                        variant.runtimeSpec().fluids());
            });
        }
        assertEquals(
                ModMachineVariants.CENTRIFUGE.recipeMapId(),
                ModMultiblockControllers.LARGE_CENTRIFUGE_KIND
                        .recipeMapId());
        assertFalse(ModMachineVariants.CENTRIFUGE.id().equals(
                ModMultiblockControllers.LARGE_CENTRIFUGE_KIND.id()));
    }

    @Test
    void sourceWindowProducesStandardOverclockAndParallelDuration() {
        GTRecipe recipe = recipe(4L, 100);
        MachineVariant centrifuge = ModMachineVariants.ALL.stream()
                .filter(variant -> variant.id().getPath()
                        .equals("centrifuge"))
                .findFirst()
                .orElseThrow();
        MachineExecutionPlan single = MachineExecutionPlan.create(
                        recipe,
                        centrifuge.kind(),
                        centrifuge.tierBand(),
                        1)
                .orElseThrow();
        assertEquals(16L, single.minimumPower());
        assertEquals(32L, single.nominalPower());
        assertEquals(800L, single.totalWork());
        assertEquals(25, single.effectiveDuration());
        assertEquals(1, single.overclockSteps());

        MachineVariant sifter = ModMachineVariants.ALL.stream()
                .filter(variant -> variant.id().getPath()
                        .equals("sifter"))
                .findFirst()
                .orElseThrow();
        MachineExecutionPlan parallel = MachineExecutionPlan.create(
                        recipe,
                        sifter.kind(),
                        sifter.tierBand(),
                        4)
                .orElseThrow();
        assertEquals(4, parallel.operations());
        assertEquals(3_200L, parallel.totalWork());
        assertEquals(100, parallel.effectiveDuration());
        assertTrue(MachineExecutionPlan.create(
                recipe(65L, 10),
                centrifuge.kind(),
                centrifuge.tierBand(),
                1).isEmpty());
    }

    @Test
    void tierBandSharesCapabilitiesButVariantOwnsParallelAndIdentity() {
        MachineVariant centrifuge = ModMachineVariants.require(
                net.minecraft.resources.ResourceLocation.parse(
                        "cruciblecraft:steel_centrifuge"));
        MachineVariant lathe = ModMachineVariants.require(
                net.minecraft.resources.ResourceLocation.parse(
                        "cruciblecraft:steel_lathe"));
        TierProfile centrifugeBand = centrifuge.tierBand();
        TierProfile latheBand = lathe.tierBand();

        assertEquals(
                centrifugeBand.tierBandId(),
                latheBand.tierBandId());
        assertEquals(centrifugeBand.materialId(), latheBand.materialId());
        assertEquals(centrifugeBand.energyType(), latheBand.energyType());
        assertEquals(
                centrifugeBand.inputMinimum(),
                latheBand.inputMinimum());
        assertEquals(
                centrifugeBand.inputNominal(),
                latheBand.inputNominal());
        assertEquals(
                centrifugeBand.inputMaximum(),
                latheBand.inputMaximum());
        assertEquals(
                centrifugeBand.energyCapacity(),
                latheBand.energyCapacity());
        assertEquals(centrifugeBand.efficiency(), latheBand.efficiency());
        assertNotEquals(
                centrifugeBand.parallelLimit(),
                latheBand.parallelLimit());
        assertNotEquals(centrifuge.id(), lathe.id());
    }

    @Test
    void parallelProjectionScalesConsumptionAndWearAtomically() {
        GTRecipe scaled = ParallelRecipeOperations.scale(
                recipe(4L, 100), 3);
        assertEquals(List.of(3), scaled.itemInputCounts());
        assertEquals(4L, scaled.eut());
        assertEquals(100, scaled.duration());
        List<ItemStack> maximum = ParallelRecipeOperations.maximumItemOutputs(
                recipeWithOutput(), 3);
        assertEquals(1, maximum.size());
        assertEquals(3, maximum.getFirst().getCount());
    }

    private static GTRecipe recipe(long eut, int duration) {
        return new GTRecipe(
                List.of(Ingredient.of(Items.COBBLESTONE)),
                List.of(1),
                List.of(ItemInputAction.CONSUME),
                List.of(new ItemStack(Items.GRAVEL)),
                List.of(),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                duration,
                eut,
                0L,
                true,
                Optional.empty());
    }

    private static GTRecipe recipeWithOutput() {
        return new GTRecipe(
                List.of(Ingredient.of(Items.COBBLESTONE)),
                List.of(1),
                List.of(ItemInputAction.CONSUME),
                List.of(new ItemStack(Items.GRAVEL)),
                List.of(),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                10,
                4L,
                0L,
                true,
                Optional.empty());
    }
}
