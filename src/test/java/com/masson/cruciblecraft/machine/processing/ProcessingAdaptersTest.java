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
import com.masson.cruciblecraft.recipe.gt.ItemInputAction;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.registry.ModProcessingMachines;
import com.masson.cruciblecraft.recipe.rule.MaterialChainRules;
import com.masson.cruciblecraft.content.block.ProcessingMachineInteractions;
import com.masson.cruciblecraft.content.menu.ProcessingMenuRanges;
import com.masson.cruciblecraft.content.menu.ConfiguredProcessingMachineMenu;
import com.masson.cruciblecraft.steam.MachineSideRules;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
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
    void savedInventoryExpandsToLiveLayoutWithoutLosingStacks() {
        ItemStackHandler legacy = new ItemStackHandler(5);
        legacy.setStackInSlot(0, new ItemStack(Items.COAL, 3));
        legacy.setStackInSlot(4, new ItemStack(Items.DIAMOND, 2));
        LayoutAwareItemStackHandler live = new LayoutAwareItemStackHandler(
                7, (slot, stack) -> true, ignored -> {});

        live.deserializeForLayout(registries, legacy.serializeNBT(registries));

        assertEquals(7, live.getSlots());
        assertEquals(5, live.loadedSlots());
        assertFalse(live.layoutQuarantined());
        assertEquals(3, live.getStackInSlot(0).getCount());
        assertEquals(2, live.getStackInSlot(4).getCount());
        assertTrue(live.getStackInSlot(5).isEmpty());
        assertTrue(live.getStackInSlot(6).isEmpty());
    }

    @Test
    void savedInventoryLargerThanLiveLayoutIsRetainedAndQuarantined() {
        ItemStackHandler future = new ItemStackHandler(9);
        future.setStackInSlot(8, new ItemStack(Items.NETHER_STAR));
        LayoutAwareItemStackHandler live = new LayoutAwareItemStackHandler(
                7, (slot, stack) -> true, ignored -> {});

        live.deserializeForLayout(registries, future.serializeNBT(registries));

        assertEquals(9, live.getSlots());
        assertEquals(9, live.loadedSlots());
        assertTrue(live.layoutQuarantined());
        assertEquals(1, live.getStackInSlot(8).getCount());
        assertFalse(live.isItemValid(8, new ItemStack(Items.COAL)));
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
    void amountDrainUsesTheSameAtomicMultiTankPlanAsTypedDrain() {
        FluidTank first = new FluidTank(1000);
        FluidTank second = new FluidTank(1000);
        first.setFluid(new FluidStack(Fluids.WATER, 300));
        second.setFluid(new FluidStack(Fluids.WATER, 500));
        AtomicInteger mutations = new AtomicInteger();
        SidedFluidHandler output = new SidedFluidHandler(
                List.of(first, second),
                List.of(0, 1),
                ProcessingMachineSpec.CapabilityAccess.OUTPUT,
                mutations::incrementAndGet);

        FluidStack simulated = output.drain(700, IFluidHandler.FluidAction.SIMULATE);
        assertEquals(700, simulated.getAmount());
        assertEquals(800, first.getFluidAmount() + second.getFluidAmount());
        assertEquals(0, mutations.get());

        FluidStack executed = output.drain(700, IFluidHandler.FluidAction.EXECUTE);
        assertEquals(700, executed.getAmount());
        assertEquals(100, first.getFluidAmount() + second.getFluidAmount());
        assertEquals(1, mutations.get());
    }

    @Test
    void duplicatePhysicalTankExposureFailsAtConstruction() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> new SidedFluidHandler(
                        List.of(new FluidTank(1000)),
                        List.of(0, 0),
                        ProcessingMachineSpec.CapabilityAccess.OUTPUT));
        assertTrue(error.getMessage().contains("exposed more than once"));
    }

    @Test
    void multiTankFillPlansMergeAndSplitWithoutPartialMutation() {
        FluidTank first = new FluidTank(500);
        FluidTank second = new FluidTank(1000);
        first.setFluid(new FluidStack(Fluids.WATER, 400));
        AtomicInteger mutations = new AtomicInteger();
        SidedFluidHandler input = new SidedFluidHandler(
                List.of(first, second),
                List.of(0, 1),
                ProcessingMachineSpec.CapabilityAccess.INPUT,
                mutations::incrementAndGet);
        FluidStack request = new FluidStack(Fluids.WATER, 700);
        int before = first.getFluidAmount() + second.getFluidAmount();

        assertEquals(700, input.fill(request, IFluidHandler.FluidAction.SIMULATE));
        assertEquals(400, first.getFluidAmount());
        assertEquals(0, second.getFluidAmount());
        assertEquals(0, mutations.get());

        assertEquals(700, input.fill(request, IFluidHandler.FluidAction.EXECUTE));
        assertEquals(500, first.getFluidAmount());
        assertEquals(600, second.getFluidAmount());
        assertEquals(700,
                first.getFluidAmount() + second.getFluidAmount() - before,
                "executed fill must conserve the exact reported amount");
        assertEquals(1, mutations.get());
    }

    @Test
    void simulationMutationIsRolledBackWithoutCallback() {
        FluidTank violating = new FluidTank(1000) {
            @Override
            public int fill(FluidStack resource, FluidAction action) {
                return super.fill(
                        resource,
                        action.simulate() ? FluidAction.EXECUTE : action);
            }
        };
        AtomicInteger mutations = new AtomicInteger();
        SidedFluidHandler input = new SidedFluidHandler(
                List.of(violating),
                List.of(0),
                ProcessingMachineSpec.CapabilityAccess.INPUT,
                mutations::incrementAndGet);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> input.fill(
                        new FluidStack(Fluids.WATER, 250),
                        IFluidHandler.FluidAction.SIMULATE));
        assertTrue(error.getMessage().contains("mutated during fill simulation"));
        assertTrue(violating.getFluid().isEmpty());
        assertEquals(0, mutations.get());
    }

    @Test
    void executeDrainViolationRollsBackEveryExposedTankExactly() {
        FluidTank first = new FluidTank(1000);
        FluidTank violating = new FluidTank(1000) {
            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                if (action.execute()) {
                    return super.drain(
                            resource.copyWithAmount(resource.getAmount() - 1),
                            action);
                }
                return super.drain(resource, action);
            }
        };
        first.setFluid(new FluidStack(Fluids.WATER, 300));
        violating.setFluid(new FluidStack(Fluids.WATER, 300));
        AtomicInteger mutations = new AtomicInteger();
        SidedFluidHandler output = new SidedFluidHandler(
                List.of(first, violating),
                List.of(0, 1),
                ProcessingMachineSpec.CapabilityAccess.OUTPUT,
                mutations::incrementAndGet);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> output.drain(
                        new FluidStack(Fluids.WATER, 500),
                        IFluidHandler.FluidAction.EXECUTE));
        assertTrue(error.getMessage().contains("executed drain"));
        assertEquals(300, first.getFluidAmount());
        assertEquals(300, violating.getFluidAmount());
        assertEquals(600, first.getFluidAmount() + violating.getFluidAmount());
        assertEquals(0, mutations.get());
    }

    @Test
    void executeFillViolationRollsBackEarlierAndFailingTanksExactly() {
        FluidTank first = new FluidTank(400);
        FluidTank violating = new FluidTank(1000) {
            @Override
            public int fill(FluidStack resource, FluidAction action) {
                if (action.execute()) {
                    return super.fill(
                            resource.copyWithAmount(resource.getAmount() - 1),
                            action);
                }
                return super.fill(resource, action);
            }
        };
        AtomicInteger mutations = new AtomicInteger();
        SidedFluidHandler input = new SidedFluidHandler(
                List.of(first, violating),
                List.of(0, 1),
                ProcessingMachineSpec.CapabilityAccess.INPUT,
                mutations::incrementAndGet);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> input.fill(
                        new FluidStack(Fluids.WATER, 700),
                        IFluidHandler.FluidAction.EXECUTE));
        assertTrue(error.getMessage().contains("executed fill"));
        assertTrue(first.getFluid().isEmpty());
        assertTrue(violating.getFluid().isEmpty());
        assertEquals(0, mutations.get());
    }

    @Test
    void rollbackFailureIsReportedAsPotentiallyInconsistentState() {
        AtomicBoolean rejectRollback = new AtomicBoolean();
        FluidTank uncooperative = new FluidTank(1000) {
            @Override
            public int fill(FluidStack resource, FluidAction action) {
                if (action.execute()) {
                    int filled = super.fill(
                            resource.copyWithAmount(resource.getAmount() - 1),
                            action);
                    rejectRollback.set(true);
                    return filled;
                }
                return super.fill(resource, action);
            }

            @Override
            public void setFluid(FluidStack stack) {
                if (rejectRollback.get()) {
                    throw new IllegalStateException("rollback disabled");
                }
                super.setFluid(stack);
            }
        };
        AtomicInteger mutations = new AtomicInteger();
        SidedFluidHandler input = new SidedFluidHandler(
                List.of(uncooperative),
                List.of(0),
                ProcessingMachineSpec.CapabilityAccess.INPUT,
                mutations::incrementAndGet);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> input.fill(
                        new FluidStack(Fluids.WATER, 250),
                        IFluidHandler.FluidAction.EXECUTE));
        assertTrue(error.getMessage().contains("ROLLBACK FAILED"));
        assertTrue(error.getMessage().contains("may be inconsistent"));
        assertEquals(1, error.getSuppressed().length);
        assertEquals(0, mutations.get());
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
        assertEquals(7, ModProcessingMachines.PRIMARY_MACHINES.size());
        for (ProcessingMachineSpec spec : ModProcessingMachines.PRIMARY_MACHINES) {
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
        var smelterRule = MaterialChainRules.ALL.stream()
                .filter(definition -> definition.path().startsWith("smelter/"))
                .findFirst().orElseThrow().rule();
        assertTrue(smelterRule.duration().startsWith("800 * ("));
        assertEquals("8", smelterRule.eut());
    }

    @Test
    void everyComponentMachineSpecUsesSharedKuPlacementAndExactLayouts() {
        assertEquals(10, ModProcessingMachines.COMPONENT_MACHINES.size());
        for (ProcessingMachineSpec spec : ModProcessingMachines.COMPONENT_MACHINES) {
            assertEquals(spec.recipeMapId(), spec.requireRecipeMap().id());
            EnergyType expectedEnergy;
            if (spec == ModProcessingMachines.PRESS) {
                expectedEnergy = EnergyType.KINETIC_PUSH;
            } else if (spec == ModProcessingMachines.EXTRUDER) {
                expectedEnergy = EnergyType.HEAT;
            } else if (List.of(
                    ModProcessingMachines.CUTTER,
                    ModProcessingMachines.LATHE,
                    ModProcessingMachines.ROLLINGMILL,
                    ModProcessingMachines.ROLLBENDER,
                    ModProcessingMachines.WIREMILL).contains(spec)) {
                expectedEnergy = EnergyType.KINETIC_ROTATION;
            } else {
                expectedEnergy = EnergyType.KINETIC;
            }
            assertEquals(expectedEnergy, spec.energy().type());
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
                ModProcessingMachines.WELDER,
                ModProcessingMachines.PRESS)) {
            assertEquals(2, spec.items().inputs().size());
        }
        assertEquals(6, ModProcessingMachines.ASSEMBLER.items().inputs().size());
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
    void assemblerExposesSixRoleFilteredInputsAndValidatesAllCatalysts() {
        installAssemblerCatalystFixtures();
        ProcessingMachineSpec spec = ModProcessingMachines.ASSEMBLER;
        ItemStack firstWearTool = new ItemStack(Items.IRON_PICKAXE);
        ItemStack secondWearTool = new ItemStack(Items.IRON_AXE);
        ItemStack pattern = new ItemStack(Items.TRIAL_KEY);
        ItemStack material = new ItemStack(Items.IRON_INGOT);

        assertEquals(List.of(0, 1, 2, 3, 4, 5), spec.items().inputs());
        assertEquals(List.of(6), spec.items().outputs());
        assertEquals(7, spec.ui().machineSlots().size());
        for (int slot = 0; slot < 3; slot++) {
            assertEquals(ProcessingMachineSpec.SlotRole.MATERIAL, spec.items().role(slot));
            assertTrue(spec.items().accepts(slot, material));
            assertFalse(spec.items().accepts(slot, firstWearTool));
            assertFalse(spec.items().accepts(slot, pattern));
        }
        for (int slot = 3; slot < 6; slot++) {
            assertEquals(ProcessingMachineSpec.SlotRole.TOOL, spec.items().role(slot));
            assertFalse(spec.items().accepts(slot, material));
            assertTrue(spec.items().accepts(slot, firstWearTool));
            assertTrue(spec.items().accepts(slot, secondWearTool));
            assertTrue(spec.items().accepts(slot, pattern));
        }
        assertEquals(ProcessingMachineSpec.SlotRole.OUTPUT, spec.items().role(6));

        for (String path : List.of(
                "material_file",
                "smithing_hammer",
                "flint_knife",
                "material_screwdriver",
                "material_wrench",
                "material_monkey_wrench")) {
            assertTrue(CraftingCatalystPolicy.isWearCatalyst(
                    ResourceLocation.fromNamespaceAndPath("cruciblecraft", path)));
        }
        assertTrue(CraftingCatalystPolicy.isPreservedPattern(
                ResourceLocation.fromNamespaceAndPath(
                        "cruciblecraft", "tool_pattern_pickaxe")));

        GTRecipe sixInputs = new GTRecipe(
                List.of(
                        Ingredient.of(Items.IRON_INGOT),
                        Ingredient.of(Items.GOLD_INGOT),
                        Ingredient.of(Items.COPPER_INGOT),
                        Ingredient.of(Items.IRON_PICKAXE),
                        Ingredient.of(Items.IRON_AXE),
                        Ingredient.of(Items.TRIAL_KEY)),
                List.of(1, 1, 1, 0, 0, 0),
                List.of(
                        ItemInputAction.CONSUME,
                        ItemInputAction.CONSUME,
                        ItemInputAction.CONSUME,
                        ItemInputAction.wear(1),
                        ItemInputAction.wear(2),
                        ItemInputAction.PRESERVE),
                List.of(new ItemStack(Items.DIAMOND)),
                List.of(),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                20,
                24,
                0,
                true,
                java.util.Optional.empty());
        assertTrue(spec.validator().validate(sixInputs).isEmpty());
    }

    @Test
    void t3MachineValidatorRejectsOversizeAndInvalidChance() {
        GTRecipe oversize = new GTRecipe(
                List.of(
                        Ingredient.of(Items.COAL),
                        Ingredient.of(Items.IRON_INGOT),
                        Ingredient.of(Items.GOLD_INGOT),
                        Ingredient.of(Items.COPPER_INGOT)),
                List.of(1, 1, 1, 1),
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
        assertEquals(ProcessingMachineInteractions.FluidTransfer.FILL_INPUT,
                ProcessingMachineInteractions.fluidTransfer(
                        ModProcessingMachines.BATH, Direction.NORTH, Direction.WEST,
                        false, true, true));
        assertEquals(ProcessingMachineInteractions.FluidTransfer.DRAIN_INPUT,
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
        assertEquals(3, ConfiguredProcessingMachineMenu.DATA_COUNT);
        assertEquals(1, ProcessingMachineDisplayData.statusIndex(
                ModProcessingMachines.ASSEMBLER, ""));
        assertEquals(7, ProcessingMachineDisplayData.statusIndex(
                ModProcessingMachines.ASSEMBLER, "unsupported_version_3"));
        assertEquals(8, ProcessingMachineDisplayData.statusIndex(
                ModProcessingMachines.ASSEMBLER, "inventory_layout_quarantined"));
        assertEquals(10, ProcessingMachineDisplayData.statusIndex(
                ModProcessingMachines.ASSEMBLER, "future_status"));
    }

    private static void installAssemblerCatalystFixtures() {
        bindItemTag(Items.IRON_PICKAXE, CraftingCatalystPolicy.WEAR_CATALYSTS);
        bindItemTag(Items.IRON_AXE, CraftingCatalystPolicy.WEAR_CATALYSTS);
        bindItemTag(Items.TRIAL_KEY, CraftingCatalystPolicy.PRESERVED_PATTERNS);
    }

    private static void bindItemTag(
            net.minecraft.world.item.Item item,
            net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tag) {
        var holder = BuiltInRegistries.ITEM.wrapAsHolder(item);
        var tags = java.util.stream.Stream.concat(
                holder.tags(), java.util.stream.Stream.of(tag)).distinct().toList();
        try {
            java.lang.reflect.Method bindTags =
                    holder.getClass().getDeclaredMethod("bindTags", java.util.Collection.class);
            bindTags.setAccessible(true);
            bindTags.invoke(holder, tags);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Unable to install crafting catalyst test tag", exception);
        }
    }

}
