package com.masson.cruciblecraft.machine.processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.masson.cruciblecraft.TestExtruderShapes;
import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.registry.ModProcessingMachines;
import com.masson.cruciblecraft.recipe.rule.T2ChainRules;
import com.masson.cruciblecraft.content.block.ProcessingMachineInteractions;
import com.masson.cruciblecraft.content.menu.ProcessingMenuRanges;
import com.masson.cruciblecraft.content.menu.ConfiguredProcessingMachineMenu;
import com.masson.cruciblecraft.steam.MachineSideRules;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ProcessingAdaptersTest {
    private static RegistryAccess registries;

    @BeforeAll
    static void bootstrap() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        registries = new RegistryAccess.ImmutableRegistryAccess(
                BuiltInRegistries.REGISTRY.stream().toList());
    }

    @Test
    void sidedItemAndFluidViewsEnforceDirection() {
        ItemStackHandler items = new ItemStackHandler(2);
        AtomicInteger mutations = new AtomicInteger();
        SidedItemHandler input = new SidedItemHandler(
                items, List.of(0), ProcessingMachineSpec.CapabilityAccess.INPUT,
                mutations::incrementAndGet);
        SidedItemHandler output = new SidedItemHandler(
                items, List.of(1), ProcessingMachineSpec.CapabilityAccess.OUTPUT,
                mutations::incrementAndGet);
        assertTrue(input.insertItem(0, new ItemStack(Items.COAL), true).isEmpty());
        assertEquals(0, mutations.get(), "simulation must not dirty");
        assertTrue(input.insertItem(0, new ItemStack(Items.COAL), false).isEmpty());
        assertEquals(1, mutations.get());
        assertTrue(input.extractItem(0, 1, false).isEmpty());
        assertEquals(1, items.getStackInSlot(0).getCount());
        assertEquals(1, output.insertItem(0, new ItemStack(Items.DIAMOND), false).getCount());
        items.setStackInSlot(1, new ItemStack(Items.IRON_INGOT));
        assertEquals(1, output.extractItem(0, 1, false).getCount());

        FluidTank inputTank = new FluidTank(1000);
        FluidTank outputTank = new FluidTank(1000);
        outputTank.setFluid(new FluidStack(Fluids.LAVA, 500));
        SidedFluidHandler fluidInput = new SidedFluidHandler(
                List.of(inputTank, outputTank),
                List.of(0),
                ProcessingMachineSpec.CapabilityAccess.INPUT,
                mutations::incrementAndGet);
        SidedFluidHandler fluidOutput = new SidedFluidHandler(
                List.of(inputTank, outputTank),
                List.of(1),
                ProcessingMachineSpec.CapabilityAccess.OUTPUT,
                mutations::incrementAndGet);
        assertEquals(1000, fluidInput.fill(
                new FluidStack(Fluids.WATER, 1000),
                IFluidHandler.FluidAction.SIMULATE));
        assertEquals(2, mutations.get(), "item output extraction was the second mutation");
        assertEquals(1000, fluidInput.fill(
                new FluidStack(Fluids.WATER, 1000),
                IFluidHandler.FluidAction.EXECUTE));
        assertEquals(3, mutations.get());
        assertTrue(fluidInput.drain(100, IFluidHandler.FluidAction.EXECUTE).isEmpty());
        assertEquals(0, fluidOutput.fill(
                new FluidStack(Fluids.LAVA, 100),
                IFluidHandler.FluidAction.EXECUTE));
        assertEquals(250, fluidOutput.drain(
                250, IFluidHandler.FluidAction.EXECUTE).getAmount());
    }

    @Test
    void multiSlotItemSimulationNeverMarksMutation() {
        ItemStackHandler items = new ItemStackHandler(2);
        items.setStackInSlot(1, new ItemStack(Items.COAL, 2));
        AtomicInteger mutations = new AtomicInteger();
        SidedItemHandler input = new SidedItemHandler(
                items,
                List.of(0, 1),
                ProcessingMachineSpec.CapabilityAccess.INPUT,
                mutations::incrementAndGet);

        assertTrue(input.insertItem(0, new ItemStack(Items.COAL), true).isEmpty());
        assertTrue(input.insertItem(1, new ItemStack(Items.COAL), true).isEmpty());
        assertEquals(0, mutations.get());
        assertTrue(items.getStackInSlot(0).isEmpty());
        assertEquals(2, items.getStackInSlot(1).getCount());
    }

    @Test
    void itemAutomationPartialTransfersKeepSimulateAndExecuteConsistent() {
        ItemStackHandler items = new ItemStackHandler(2);
        items.setStackInSlot(0, new ItemStack(Items.COAL, 60));
        items.setStackInSlot(1, new ItemStack(Items.IRON_INGOT, 10));
        AtomicInteger mutations = new AtomicInteger();
        SidedItemHandler input = new SidedItemHandler(
                items, List.of(0), ProcessingMachineSpec.CapabilityAccess.INPUT,
                mutations::incrementAndGet);
        SidedItemHandler output = new SidedItemHandler(
                items, List.of(1), ProcessingMachineSpec.CapabilityAccess.OUTPUT,
                mutations::incrementAndGet);

        ItemStack simulatedRemainder =
                input.insertItem(0, new ItemStack(Items.COAL, 10), true);
        assertEquals(6, simulatedRemainder.getCount());
        assertEquals(60, items.getStackInSlot(0).getCount());
        assertEquals(0, mutations.get());
        ItemStack executedRemainder =
                input.insertItem(0, new ItemStack(Items.COAL, 10), false);
        assertEquals(simulatedRemainder.getCount(), executedRemainder.getCount());
        assertEquals(64, items.getStackInSlot(0).getCount());
        assertEquals(1, mutations.get());

        ItemStack simulatedExtraction = output.extractItem(0, 6, true);
        assertEquals(6, simulatedExtraction.getCount());
        assertEquals(10, items.getStackInSlot(1).getCount());
        assertEquals(1, mutations.get());
        ItemStack executedExtraction = output.extractItem(0, 6, false);
        assertTrue(ItemStack.isSameItemSameComponents(
                simulatedExtraction, executedExtraction));
        assertEquals(simulatedExtraction.getCount(), executedExtraction.getCount());
        assertEquals(4, items.getStackInSlot(1).getCount());
        assertEquals(2, mutations.get());
    }

    @Test
    void fluidStackDrainAggregatesWithoutSimulationMutationAndMutatesOnce() {
        FluidTank first = new FluidTank(1000);
        FluidTank second = new FluidTank(1000);
        first.setFluid(new FluidStack(Fluids.WATER, 400));
        second.setFluid(new FluidStack(Fluids.WATER, 600));
        AtomicInteger mutations = new AtomicInteger();
        SidedFluidHandler output = new SidedFluidHandler(
                List.of(first, second),
                List.of(0, 1),
                ProcessingMachineSpec.CapabilityAccess.OUTPUT,
                mutations::incrementAndGet);
        FluidStack request = new FluidStack(Fluids.WATER, 700);
        int before = first.getFluidAmount() + second.getFluidAmount();

        FluidStack simulated = output.drain(
                request, IFluidHandler.FluidAction.SIMULATE);
        assertEquals(700, simulated.getAmount());
        assertEquals(before, first.getFluidAmount() + second.getFluidAmount());
        assertEquals(0, mutations.get());

        FluidStack executed = output.drain(
                request, IFluidHandler.FluidAction.EXECUTE);
        int after = first.getFluidAmount() + second.getFluidAmount();
        assertEquals(700, executed.getAmount());
        assertEquals(executed.getAmount(), before - after);
        assertEquals(1, mutations.get());
    }

    @Test
    void incompatibleFluidAggregationFailsBeforeAnyTankIsChanged() {
        FluidTank water = new FluidTank(1000);
        water.setFluid(new FluidStack(Fluids.WATER, 250));
        FluidTank incompatible = new FluidTank(1000) {
            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                return super.drain(
                        getFluid().copyWithAmount(resource.getAmount()),
                        action);
            }
        };
        incompatible.setFluid(new FluidStack(Fluids.LAVA, 250));
        AtomicInteger mutations = new AtomicInteger();
        SidedFluidHandler output = new SidedFluidHandler(
                List.of(water, incompatible),
                List.of(0, 1),
                ProcessingMachineSpec.CapabilityAccess.OUTPUT,
                mutations::incrementAndGet);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> output.drain(
                        new FluidStack(Fluids.WATER, 400),
                        IFluidHandler.FluidAction.EXECUTE));
        assertTrue(error.getMessage().contains("minecraft:water"));
        assertTrue(error.getMessage().contains("minecraft:lava"));
        assertTrue(error.getMessage().contains("tank 1"));
        assertEquals(250, water.getFluidAmount());
        assertEquals(250, incompatible.getFluidAmount());
        assertEquals(0, mutations.get());
    }

    @Test
    void specIsRegistryLazyAndRejectsOverlappingLayouts() {
        AtomicBoolean supplied = new AtomicBoolean();
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("test", "machine");
        ProcessingMachineSpec spec = new ProcessingMachineSpec(
                id,
                id,
                () -> {
                    supplied.set(true);
                    return new RecipeMap(id);
                },
                new ProcessingMachineSpec.SlotLayout(2, List.of(0), List.of(1)),
                new ProcessingMachineSpec.TankLayout(List.of(), List.of()),
                new ProcessingMachineSpec.EnergySpec(
                        EnergyType.KINETIC,
                        ProcessingMachineSpec.EnergyMode.BUFFERED,
                        100,
                        16),
                new ProcessingMachineSpec.SidedIoPolicy(
                        (front, side) -> ProcessingMachineSpec.CapabilityAccess.INPUT,
                        (front, side) -> ProcessingMachineSpec.CapabilityAccess.NONE,
                        (front, side) -> ProcessingMachineSpec.CapabilityAccess.INPUT),
                recipe -> java.util.Optional.empty(),
                ProcessingMachineSpec.BufferPolicy.PAUSE,
                new ProcessingMachineSpec.UiLayout(
                        List.of(
                                new ProcessingMachineSpec.SlotPosition(0, 0),
                                new ProcessingMachineSpec.SlotPosition(1, 0)),
                        new ProcessingMachineSpec.ProgressBar(0, 0, 1, 1),
                        List.of(),
                        List.of("idle")));
        assertFalse(supplied.get());
        assertEquals(id, spec.requireRecipeMap().id());
        assertTrue(supplied.get());

        assertThrows(IllegalArgumentException.class, () ->
                new ProcessingMachineSpec.SlotLayout(2, List.of(0), List.of(0)));
        assertThrows(IllegalArgumentException.class, () ->
                new ProcessingMachineSpec.TankLayout(
                        List.of(new ProcessingMachineSpec.TankSpec(0, 100)),
                        List.of(new ProcessingMachineSpec.TankSpec(0, 100))));
    }

    @Test
    void crusherSpecPreservesLegacyShapePowerAndSides() {
        ProcessingMachineSpec crusher = ModProcessingMachines.CRUSHER;
        assertEquals(List.of(0), crusher.items().inputs());
        assertEquals(List.of(1), crusher.items().outputs());
        assertEquals(1024, crusher.energy().capacity());
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.OUTPUT,
                crusher.sidedIo().items().resolve(Direction.NORTH, Direction.NORTH));
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.INPUT,
                crusher.sidedIo().items().resolve(Direction.NORTH, null));
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.INPUT,
                crusher.sidedIo().energy().resolve(Direction.NORTH, Direction.SOUTH));
        GTRecipe valid = new GTRecipe(
                List.of(Ingredient.of(Items.COAL)),
                List.of(1),
                List.of(new ItemStack(Items.DIAMOND, 2)),
                List.of(),
                List.of(),
                List.of(10_000),
                128,
                16,
                0,
                true);
        assertTrue(crusher.validator().validate(valid).isEmpty());
        GTRecipe oversizedPower = new GTRecipe(
                valid.itemInputs(),
                valid.itemInputCounts(),
                valid.itemOutputs(),
                valid.fluidInputs(),
                valid.fluidOutputs(),
                valid.outputChances(),
                valid.duration(),
                1_025,
                valid.specialValue(),
                true);
        assertTrue(crusher.validator().validate(oversizedPower).isPresent());
    }

    @Test
    void everyT2MachineSpecFitsWorstRuleAndExposesConfiguredCapabilities() {
        assertEquals(7, ModProcessingMachines.T2_MACHINES.size());
        for (ProcessingMachineSpec spec : ModProcessingMachines.T2_MACHINES) {
            assertEquals(spec.recipeMapId(), spec.requireRecipeMap().id());
            assertEquals(1, spec.items().inputs().size());
            assertTrue(spec.items().outputs().size() >= 3);
            assertEquals(
                    ProcessingMachineSpec.CapabilityAccess.OUTPUT,
                    spec.sidedIo().items().resolve(Direction.NORTH, Direction.NORTH));
            assertEquals(
                    ProcessingMachineSpec.CapabilityAccess.NONE,
                    spec.sidedIo().items().resolve(Direction.NORTH, null));
            if (spec == ModProcessingMachines.SLUICE || spec == ModProcessingMachines.BATH) {
                assertEquals(1, spec.fluids().inputs().size());
                assertTrue(spec.fluids().inputs().getFirst().capacity() >= 250);
            }
        }
        assertEquals(ProcessingMachineSpec.EnergyMode.ADJACENT,
                ModProcessingMachines.SMELTER.energy().mode());
        assertEquals(EnergyType.HEAT, ModProcessingMachines.SMELTER.energy().type());
        var placement = ProcessingMachineEnergyPlacement.connection(
                ModProcessingMachines.SMELTER, Direction.NORTH);
        assertEquals(Direction.DOWN, placement.providerOffset());
        assertEquals(Direction.UP, placement.providerFace());
        var smelterRule = T2ChainRules.ALL.stream()
                .filter(definition -> definition.path().startsWith("smelter/"))
                .findFirst().orElseThrow().rule();
        assertTrue(smelterRule.duration().startsWith("800 * ("));
        assertEquals("8", smelterRule.eut());
    }

    @Test
    void everyT3MachineSpecUsesSharedKuPlacementAndExactLayouts() {
        assertEquals(10, ModProcessingMachines.T3_MACHINES.size());
        for (ProcessingMachineSpec spec : ModProcessingMachines.T3_MACHINES) {
            assertEquals(spec.recipeMapId(), spec.requireRecipeMap().id());
            assertEquals(EnergyType.KINETIC, spec.energy().type());
            assertEquals(ProcessingMachineSpec.EnergyMode.BUFFERED, spec.energy().mode());
            assertEquals(4_096L, spec.energy().capacity());
            assertEquals(256L, spec.energy().maxPacket());
            assertEquals(spec.items().slotCount(), spec.ui().machineSlots().size());
            assertEquals(1, spec.items().outputs().size());
            assertEquals(
                    ProcessingMachineSpec.CapabilityAccess.OUTPUT,
                    spec.sidedIo().items().resolve(Direction.NORTH, Direction.NORTH));
            assertEquals(
                    ProcessingMachineSpec.CapabilityAccess.INPUT,
                    spec.sidedIo().items().resolve(Direction.NORTH, Direction.WEST));
            assertEquals(
                    ProcessingMachineSpec.CapabilityAccess.NONE,
                    spec.sidedIo().items().resolve(Direction.NORTH, null));
            assertEquals(
                    ProcessingMachineSpec.CapabilityAccess.INPUT,
                    spec.sidedIo().energy().resolve(Direction.NORTH, Direction.SOUTH));
            assertEquals(
                    ProcessingMachineSpec.CapabilityAccess.NONE,
                    spec.sidedIo().energy().resolve(Direction.NORTH, null));
            var placement = ProcessingMachineEnergyPlacement.connection(spec, Direction.NORTH);
            assertEquals(Direction.SOUTH, placement.providerOffset());
            assertEquals(Direction.NORTH, placement.providerFace());
            assertTrue(MachineSideRules.engineExposesKinetic(
                    Direction.NORTH, placement.providerFace()));
            assertEquals(spec, ModProcessingMachines.require(spec.id()));
            var ranges = ProcessingMenuRanges.forSpec(spec);
            assertEquals(0, ranges.machineStart());
            assertEquals(spec.items().slotCount(), ranges.machineEnd());
            assertEquals(ranges.machineEnd(), ranges.playerStart());
            assertEquals(ranges.playerStart() + 36, ranges.playerEnd());
            assertEquals(spec.items().inputs(), ranges.inputSlots());
        }
        for (ProcessingMachineSpec spec : List.of(
                ModProcessingMachines.EXTRUDER,
                ModProcessingMachines.WIREMILL,
                ModProcessingMachines.ASSEMBLER,
                ModProcessingMachines.WELDER,
                ModProcessingMachines.PRESS)) {
            assertEquals(2, spec.items().inputs().size());
        }
        assertEquals(1, ModProcessingMachines.ASSEMBLER.fluids().inputs().size());
        assertEquals(1, ModProcessingMachines.WELDER.fluids().inputs().size());
        assertFalse(ProcessingMachineFluidPolicy.accepts(
                ModProcessingMachines.ASSEMBLER, 0, new FluidStack(Fluids.LAVA, 250)));
        assertFalse(ProcessingMachineFluidPolicy.accepts(
                ModProcessingMachines.ASSEMBLER, 1, new FluidStack(Fluids.LAVA, 250)));
    }

    @Test
    void extruderRolesAndAutomationEnforceMaterialAndToolSlots() {
        ProcessingMachineSpec spec = ModProcessingMachines.EXTRUDER;
        ItemStack shape = TestExtruderShapes.stack();
        ItemStack material = new ItemStack(Items.IRON_INGOT);
        assertEquals(ProcessingMachineSpec.SlotRole.MATERIAL, spec.items().role(0));
        assertEquals(ProcessingMachineSpec.SlotRole.TOOL, spec.items().role(1));
        assertEquals(ProcessingMachineSpec.SlotRole.OUTPUT, spec.items().role(2));
        assertFalse(spec.items().accepts(0, shape));
        assertTrue(spec.items().accepts(0, material));
        assertTrue(spec.items().accepts(1, shape));
        assertFalse(spec.items().accepts(1, material));

        ItemStackHandler inventory = new ItemStackHandler(3) {
            @Override public boolean isItemValid(int slot, ItemStack stack) {
                return spec.items().accepts(slot, stack);
            }
        };
        SidedItemHandler automation = new SidedItemHandler(
                inventory,
                spec.items().inputs(),
                ProcessingMachineSpec.CapabilityAccess.INPUT);
        assertEquals(1, automation.insertItem(0, shape.copy(), false).getCount());
        assertTrue(automation.insertItem(1, shape.copy(), false).isEmpty());
        assertEquals(1, automation.insertItem(1, material.copy(), false).getCount());
        assertTrue(automation.insertItem(0, material.copy(), false).isEmpty());

        GTRecipe valid = new GTRecipe(
                List.of(Ingredient.of(Items.IRON_INGOT), Ingredient.of(shape.getItem())),
                List.of(1, 0),
                List.of(new ItemStack(Items.IRON_NUGGET)),
                List.of(), List.of(), List.of(10_000), 20, 24, 0, true);
        assertTrue(spec.validator().validate(valid).isEmpty());
        GTRecipe nonShapeTool = new GTRecipe(
                List.of(Ingredient.of(Items.IRON_INGOT), Ingredient.of(Items.STICK)),
                List.of(1, 0),
                List.of(new ItemStack(Items.IRON_NUGGET)),
                List.of(), List.of(), List.of(10_000), 20, 24, 0, true);
        assertTrue(spec.validator().validate(nonShapeTool).isPresent());
    }

    @Test
    void t3MachineValidatorRejectsOversizeAndInvalidChance() {
        GTRecipe oversize = new GTRecipe(
                List.of(
                        Ingredient.of(Items.COAL),
                        Ingredient.of(Items.IRON_INGOT),
                        Ingredient.of(Items.GOLD_INGOT)),
                List.of(1, 1, 1),
                List.of(new ItemStack(Items.DIAMOND)),
                List.of(), List.of(), List.of(10_000), 20, 24, 0, true);
        assertTrue(ModProcessingMachines.ASSEMBLER.validator().validate(oversize).isPresent());
        GTRecipe zeroChance = new GTRecipe(
                List.of(Ingredient.of(Items.COAL)),
                List.of(1),
                List.of(new ItemStack(Items.DIAMOND)),
                List.of(), List.of(), List.of(0), 20, 24, 0, true);
        assertTrue(ModProcessingMachines.EXTRUDER.validator().validate(zeroChance).isPresent());
    }

    @Test
    void configuredFluidInteractionUsesRecipeFluidsAndPlayerDrainMode() {
        assertTrue(ProcessingMachineInteractions.shouldTransferFluid(
                ModProcessingMachines.SLUICE,
                Direction.NORTH,
                Direction.WEST,
                false,
                true));
        assertFalse(ProcessingMachineInteractions.shouldTransferFluid(
                ModProcessingMachines.SLUICE,
                Direction.NORTH,
                Direction.NORTH,
                false,
                true));
        assertFalse(ProcessingMachineInteractions.shouldTransferFluid(
                ModProcessingMachines.SLUICE,
                Direction.NORTH,
                Direction.WEST,
                true,
                true), "sneaking deterministically falls through to menu opening");
        assertEquals(ProcessingMachineInteractions.FluidTransfer.FILL,
                ProcessingMachineInteractions.fluidTransfer(
                        ModProcessingMachines.BATH, Direction.NORTH, Direction.WEST,
                        false, true, true));
        assertEquals(ProcessingMachineInteractions.FluidTransfer.DRAIN,
                ProcessingMachineInteractions.fluidTransfer(
                        ModProcessingMachines.BATH, Direction.NORTH, Direction.WEST,
                        false, true, false));

        RecipeMap bath = ModProcessingMachines.BATH.requireRecipeMap();
        List<RecipeMap.Entry> previous = bath.entries();
        GTRecipe waterRecipe = new GTRecipe(
                List.of(Ingredient.of(Items.COAL)),
                List.of(1),
                List.of(new ItemStack(Items.DIAMOND)),
                List.of(new FluidStack(Fluids.WATER, 250)),
                List.of(),
                List.of(10_000),
                20, 24, 0, true);
        try {
            bath.replaceRecipes(List.of(new RecipeMap.Entry(
                    ResourceLocation.fromNamespaceAndPath("test", "bath_water"),
                    waterRecipe)));
            assertTrue(ProcessingMachineFluidPolicy.accepts(
                    ModProcessingMachines.BATH, 0, new FluidStack(Fluids.WATER, 1000)));
            assertFalse(ProcessingMachineFluidPolicy.accepts(
                    ModProcessingMachines.BATH, 0, new FluidStack(Fluids.LAVA, 1000)));
            assertFalse(ProcessingMachineFluidPolicy.accepts(
                    ModProcessingMachines.BATH, 1, new FluidStack(Fluids.WATER, 1000)));
        } finally {
            bath.replaceRecipes(previous);
        }

        FluidTank recoverable = new FluidTank(1000);
        recoverable.setFluid(new FluidStack(Fluids.WATER, 750));
        SidedFluidHandler maintenance = new SidedFluidHandler(
                List.of(recoverable), List.of(0),
                ProcessingMachineSpec.CapabilityAccess.OUTPUT);
        assertEquals(250, maintenance.drain(
                250, IFluidHandler.FluidAction.EXECUTE).getAmount());
        assertEquals(500, recoverable.getFluidAmount());
        assertEquals(6, ConfiguredProcessingMachineMenu.DATA_COUNT);
        assertEquals(1, ProcessingMachineDisplayData.statusIndex(
                ModProcessingMachines.ASSEMBLER, ""));
        assertEquals(500, ProcessingMachineDisplayData.tankAmount(List.of(recoverable)));
        assertEquals(1000, ProcessingMachineDisplayData.tankCapacity(List.of(recoverable)));
    }

    @Test
    void crusherLegacyHintAdoptsRecipeIdOnlyForMatchingOutput() {
        ResourceLocation recipe = ResourceLocation.fromNamespaceAndPath("test", "crusher/iron");
        CrusherLegacyMigration.Hint hint = new CrusherLegacyMigration.Hint(
                "minecraft:iron_ingot", 42, 128, "underpowered");
        ItemStack loadedInput = new ItemStack(Items.STONE, 2);
        loadedInput.set(DataComponents.CUSTOM_NAME, Component.literal("loaded"));
        CrusherLegacyMigration.BoundHint bound = CrusherLegacyMigration.bind(
                hint, loadedInput, registries).orElseThrow();
        assertTrue(CrusherLegacyMigration.inputUnchanged(
                bound, loadedInput.copy(), registries));
        CrusherLegacyMigration.Adoption matching = CrusherLegacyMigration.adopt(
                bound, recipe, 128, new ItemStack(Items.IRON_INGOT, 2));
        assertTrue(matching.preservedProgress());
        assertEquals(recipe.toString(), matching.recipeId());
        assertEquals(42, matching.progress());

        CrusherLegacyMigration.Adoption mismatch = CrusherLegacyMigration.adopt(
                bound, recipe, 128, new ItemStack(Items.GOLD_INGOT, 2));
        assertFalse(mismatch.preservedProgress());
        assertEquals(recipe.toString(), mismatch.recipeId());
        assertEquals(0, mismatch.progress());
        assertEquals("idle", mismatch.status());

        assertFalse(CrusherLegacyMigration.inputUnchanged(
                bound, new ItemStack(Items.STONE), registries));
        ItemStack replacedComponent = new ItemStack(Items.STONE, 2);
        replacedComponent.set(
                DataComponents.CUSTOM_NAME, Component.literal("replacement"));
        assertFalse(CrusherLegacyMigration.inputUnchanged(
                bound, replacedComponent, registries));
    }
}
