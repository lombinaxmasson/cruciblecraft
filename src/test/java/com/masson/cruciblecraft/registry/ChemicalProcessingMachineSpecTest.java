package com.masson.cruciblecraft.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.content.menu.ConfiguredProcessingMachineMenu;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineEnergyPlacement;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineIoAssertions;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.recipe.gt.CompactPublicationGroups;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.ItemInputAction;

import net.minecraft.core.Direction;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.fluids.FluidStack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

class ChemicalProcessingMachineSpecTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void configuredMenusSynchronizeStatusArgumentAndProgressPermille() {
        assertEquals(3, ConfiguredProcessingMachineMenu.DATA_COUNT);
        assertEquals(3, ConfiguredProcessingMachineMenu.dataCount());
        assertEquals(
                2,
                ConfiguredProcessingMachineMenu.PROGRESS_PERMILLE_DATA_INDEX);
    }

    @Test
    void t5SetReusesSourceMapsAndAddsRoasterAndCoagulatorHosts() {
        assertEquals(
                List.of(
                        "bath",
                        "centrifuge",
                        "smelter",
                        "assembler",
                        "electrolyzer",
                        "mixer",
                        "distillery",
                        "autoclave",
                        "drying",
                        "compressor",
                        "roaster",
                        "coagulator",
                        "canner"),
                ModProcessingMachines.CHEMICAL_HOST_MACHINES.stream()
                        .map(spec -> spec.id().getPath())
                        .toList());
        assertEquals(
                List.of(
                        "electrolyzer",
                        "mixer",
                        "distillery",
                        "autoclave",
                        "drying",
                        "compressor",
                        "roaster",
                        "coagulator",
                        "canner"),
                ModProcessingMachines.CHEMICAL_DEDICATED_MACHINES.stream()
                        .map(spec -> spec.id().getPath())
                        .toList());
        assertEquals(
                List.of(
                        "electrolyzer",
                        "mixer",
                        "distillery",
                        "autoclave",
                        "drying",
                        "compressor",
                        "roaster",
                        "coagulator",
                        "canner"),
                ModProcessingMachines.CHEMICAL_DEDICATED_MACHINES.stream()
                        .map(spec -> spec.requireRecipeMap().id().getPath())
                        .toList());
        assertFalse(ModProcessingMachines.CONFIGURED_MACHINES.stream()
                .anyMatch(spec -> spec.id().getPath().equals("chemical_reactor")));
        assertEquals(
                ModProcessingMachines.CONFIGURED_MACHINES.size(),
                new HashSet<>(ModProcessingMachines.CONFIGURED_MACHINES).size());
    }

    @Test
    void dedicatedSpecsUseTheGenericConfiguredMenuRegistry() {
        for (ProcessingMachineSpec spec : ModProcessingMachines.CHEMICAL_DEDICATED_MACHINES) {
            assertEquals(
                    spec.id().getPath(),
                    ModMenus.forMachine(spec).getId().getPath());
        }
        assertEquals(
                ModMenus.menuHostSpecs().size(),
                ModMenus.processingMenuCount());
    }

    @Test
    void dedicatedSpecsExposeExactLayoutsAndSourceEnergyIo() {
        assertLayout(
                ModProcessingMachines.ELECTROLYZER,
                2, 6, 2, 3, 32_000, ModProcessingMachines.UNBOUNDED_FLUID_OUTPUT);
        assertLayout(
                ModProcessingMachines.MIXER,
                6, 1, 6, 2,
                CompactPublicationGroups.GT6_PANEL_TANK_CAPACITY,
                ModProcessingMachines.UNBOUNDED_FLUID_OUTPUT);
        assertLayout(
                ModProcessingMachines.DISTILLERY,
                2, 2, 2, 3, 8_000, ModProcessingMachines.UNBOUNDED_FLUID_OUTPUT);
        assertLayout(
                ModProcessingMachines.AUTOCLAVE,
                2, 3, 1, 1, 4_000_000, ModProcessingMachines.UNBOUNDED_FLUID_OUTPUT);
        assertLayout(
                ModProcessingMachines.DRYING,
                1, 1, 1, 1, 32_000, ModProcessingMachines.UNBOUNDED_FLUID_OUTPUT);
        assertLayout(ModProcessingMachines.COMPRESSOR, 1, 1, 0, 0, 0, 0);
        assertLayout(
                ModProcessingMachines.ROASTER,
                1, 3, 1, 1, 72_000, ModProcessingMachines.UNBOUNDED_FLUID_OUTPUT);
        assertLayout(
                ModProcessingMachines.CANNER,
                2, 1, 1, 1, 128_000, ModProcessingMachines.UNBOUNDED_FLUID_OUTPUT);
        var roasterHeat = ProcessingMachineEnergyPlacement.connection(
                ModProcessingMachines.ROASTER, Direction.EAST);
        assertEquals(Direction.DOWN, roasterHeat.providerOffset());
        assertEquals(Direction.UP, roasterHeat.providerFace());

        for (ProcessingMachineSpec spec : ModProcessingMachines.CHEMICAL_DEDICATED_MACHINES) {
            EnergyType expectedEnergy = spec.energy().type();
            if (spec == ModProcessingMachines.DISTILLERY
                    || spec == ModProcessingMachines.DRYING
                    || spec == ModProcessingMachines.ROASTER) {
                assertEquals(EnergyType.HEAT, expectedEnergy);
            } else if (spec == ModProcessingMachines.AUTOCLAVE
                    || spec == ModProcessingMachines.COAGULATOR) {
                assertEquals(EnergyType.TIME, expectedEnergy);
            } else if (spec == ModProcessingMachines.MIXER) {
                assertEquals(EnergyType.KINETIC_ROTATION, expectedEnergy);
            } else if (spec == ModProcessingMachines.COMPRESSOR) {
                assertEquals(EnergyType.KINETIC_PUSH, expectedEnergy);
            } else {
                assertEquals(EnergyType.ELECTRIC, expectedEnergy);
            }
            assertEquals(spec.fluids().tankCount(), spec.ui().tanks().size());
            assertEquals(
                    ConfiguredProcessingMachineMenu.DATA_COUNT,
                    ConfiguredProcessingMachineMenu.dataCount());
            assertEquals(1_024L, spec.energy().maxPacket());
            ProcessingMachineIoAssertions.assertMatchesProfile(spec);
            assertTrue(spec.validator().validate(maxLayoutRecipe(spec)).isEmpty());
        }
    }

    @Test
    void fourRoasterVariantsCoverTheSharedSixteenHuWindow() {
        List<com.masson.cruciblecraft.machine.processing.MachineVariant> variants =
                ModMachineVariants.variantsOf(ModProcessingMachines.ROASTER.id());
        assertEquals(4, variants.size());
        var steel = ModMachineVariants.require(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                        "cruciblecraft", "steel_roaster"));
        assertEquals(16L, steel.tierBand().inputMinimum());
        assertEquals(64L, steel.tierBand().inputMaximum());
        assertTrue(16L >= steel.tierBand().inputMinimum()
                && 16L <= steel.tierBand().inputMaximum());
        for (var variant : variants) {
            assertEquals(EnergyType.HEAT, variant.tierBand().energyType());
            assertEquals(
                    ProcessingMachineSpec.EnergyMode.ADJACENT,
                    variant.runtimeSpec().energy().mode());
            var placement = ProcessingMachineEnergyPlacement.connection(
                    variant.runtimeSpec(), Direction.EAST);
            assertEquals(Direction.DOWN, placement.providerOffset());
            assertEquals(Direction.UP, placement.providerFace());
        }
    }

    @Test
    void configuredMachineSlotsAndTanksNeverOverlapOnScreen() {
            for (ProcessingMachineSpec spec : java.util.stream.Stream.concat(
                    java.util.stream.Stream.of(ModProcessingMachines.CRUSHER),
                    ModProcessingMachines.CONFIGURED_MACHINES.stream())
                    .toList()) {
            List<ProcessingMachineSpec.SlotPosition> slots = spec.ui().machineSlots();
            for (int left = 0; left < slots.size(); left++) {
                for (int right = left + 1; right < slots.size(); right++) {
                    ProcessingMachineSpec.SlotPosition a = slots.get(left);
                    ProcessingMachineSpec.SlotPosition b = slots.get(right);
                    boolean overlaps = a.x() < b.x() + 18
                            && a.x() + 18 > b.x()
                            && a.y() < b.y() + 18
                            && a.y() + 18 > b.y();
                    assertFalse(
                            overlaps,
                            spec.id() + " slot " + a + " overlaps slot " + b);
                }
            }
            for (ProcessingMachineSpec.SlotPosition slot : slots) {
                for (ProcessingMachineSpec.TankPosition tank : spec.ui().tanks()) {
                    boolean overlaps = slot.x() < tank.x() + tank.width()
                            && slot.x() + 18 > tank.x()
                            && slot.y() < tank.y() + tank.height()
                            && slot.y() + 18 > tank.y();
                    assertFalse(
                            overlaps,
                            spec.id() + " slot " + slot + " overlaps tank " + tank);
                }
            }
        }
    }

    @Test
    void reusedSourceMapsExposeT5OutputCapacityWithoutChangingEnergyType() {
        assertLayout(
                ModProcessingMachines.BATH,
                6, 6, 1, 3, ModProcessingMachines.BATH_FLUID_INPUT,
                ModProcessingMachines.UNBOUNDED_FLUID_OUTPUT);
        assertLayout(
                ModProcessingMachines.CENTRIFUGE,
                1, 6, 1, 6,
                ModProcessingMachines.CENTRIFUGE_BRONZE_FLUID_INPUT,
                ModProcessingMachines.UNBOUNDED_FLUID_OUTPUT);
        assertEquals(Integer.MAX_VALUE, ModProcessingMachines.UNBOUNDED_FLUID_OUTPUT);
        assertLayout(
                ModProcessingMachines.SMELTER,
                1, 4, 1, 1, 4_000,
                ModProcessingMachines.UNBOUNDED_FLUID_OUTPUT);
        assertEquals(EnergyType.TIME, ModProcessingMachines.BATH.energy().type());
        assertEquals(
                EnergyType.KINETIC_ROTATION,
                ModProcessingMachines.CENTRIFUGE.energy().type());
        assertEquals(EnergyType.HEAT, ModProcessingMachines.SMELTER.energy().type());
        for (ProcessingMachineSpec spec : List.of(
                ModProcessingMachines.BATH,
                ModProcessingMachines.CENTRIFUGE,
                ModProcessingMachines.SMELTER)) {
            ProcessingMachineIoAssertions.assertMatchesProfile(spec);
        }
    }

    @Test
    void reusedSourceValidatorsAcceptTheirExactT5LayoutsAndCapacities() {
        for (ProcessingMachineSpec spec : List.of(
                ModProcessingMachines.BATH,
                ModProcessingMachines.CENTRIFUGE,
                ModProcessingMachines.SMELTER)) {
            assertTrue(
                    spec.validator().validate(maxLayoutRecipe(spec)).isEmpty(),
                    spec.id().toString());
        }
        GTRecipe bronzeBath = new GTRecipe(
                List.of(Ingredient.of(Items.REDSTONE)),
                List.of(64),
                List.of(
                        new ItemStack(Items.IRON_NUGGET),
                        new ItemStack(Items.IRON_NUGGET),
                        new ItemStack(Items.IRON_NUGGET),
                        new ItemStack(Items.IRON_NUGGET)),
                List.of(new FluidStack(Fluids.WATER, 4_000)),
                List.of(new FluidStack(Fluids.LAVA, 8_000)),
                List.of(
                        GTRecipe.GUARANTEED_CHANCE,
                        GTRecipe.GUARANTEED_CHANCE,
                        GTRecipe.GUARANTEED_CHANCE,
                        GTRecipe.GUARANTEED_CHANCE),
                20,
                ModProcessingMachines.BATH.energy().maxPacket(),
                0L);
        assertTrue(
                ModProcessingMachines.BATH.validator().validate(bronzeBath).isEmpty(),
                "Bath T5 validator still accepts the bronze 1x4 / 1x1 envelope");
        assertTrue(
                ModProcessingMachines.BATH.validator().validate(
                        maxLayoutRecipe(ModProcessingMachines.BATH)).isEmpty(),
                "Bath validator accepts the GT6 6/6/1/3 panel");
    }

    @Test
    void bathTimeHostAcceptsSourceBackedZeroEutRecipes() {
        GTRecipe timed = new GTRecipe(
                List.of(Ingredient.of(Items.REDSTONE)),
                List.of(1),
                List.of(new ItemStack(Items.IRON_NUGGET)),
                List.of(new FluidStack(Fluids.WATER, 144)),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                16,
                0L,
                0L);
        assertTrue(
                ModProcessingMachines.BATH.validator().validate(timed).isEmpty(),
                "Bath TIME recipes may use duration with eut=0");
        assertEquals(
                Optional.of("chemical_recipe_energy"),
                ModProcessingMachines.MIXER.validator().validate(timed));
        GTRecipe coagulatorTimed = new GTRecipe(
                List.of(),
                List.of(),
                List.of(new ItemStack(Items.IRON_NUGGET)),
                List.of(new FluidStack(Fluids.WATER, 16)),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                256,
                0L,
                0L);
        assertTrue(
                ModProcessingMachines.COAGULATOR.validator()
                        .validate(coagulatorTimed)
                        .isEmpty(),
                "Coagulator TIME recipes may use duration with eut=0");
        GTRecipe coagulatorPowered = new GTRecipe(
                List.of(),
                List.of(),
                List.of(new ItemStack(Items.IRON_NUGGET)),
                List.of(new FluidStack(Fluids.WATER, 16)),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                256,
                1_025L,
                0L);
        assertEquals(
                Optional.of("chemical_recipe_energy"),
                ModProcessingMachines.COAGULATOR.validator()
                        .validate(coagulatorPowered));
    }

    @Test
    void dedicatedValidatorRejectsCatalystsAndOvervoltage() {
        GTRecipe catalyst = new GTRecipe(
                List.of(Ingredient.of(Items.REDSTONE)),
                List.of(0),
                List.of(ItemInputAction.PRESERVE),
                List.of(new ItemStack(Items.IRON_INGOT)),
                List.of(new FluidStack(Fluids.WATER, 1000)),
                List.of(new FluidStack(Fluids.LAVA, 1000)),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                20,
                24L,
                0L,
                true,
                Optional.empty());
        assertTrue(
                ModProcessingMachines.ELECTROLYZER.validator().validate(catalyst).isEmpty(),
                "Electrolyzer must preserve the GT6 programmed_circuit catalyst");
        assertTrue(
                ModProcessingMachines.MIXER.validator().validate(catalyst).isEmpty(),
                "Mixer must preserve the source-backed platinum catalyst");
        assertTrue(
                ModProcessingMachines.AUTOCLAVE.validator().validate(catalyst).isEmpty(),
                "Autoclave must preserve the GT6 programmed_circuit catalyst");

        GTRecipe overvoltage = new GTRecipe(
                List.of(Ingredient.of(Items.REDSTONE)),
                List.of(1),
                List.of(new ItemStack(Items.IRON_INGOT)),
                List.of(),
                List.of(new FluidStack(Fluids.WATER, 1000)),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                20,
                1_025L,
                0L);
        assertEquals(
                Optional.of("chemical_recipe_energy"),
                ModProcessingMachines.MIXER.validator().validate(overvoltage));
        GTRecipe centrifugePanel = new GTRecipe(
                List.of(Ingredient.of(Items.REDSTONE)),
                List.of(1),
                List.of(new ItemStack(Items.IRON_INGOT)),
                List.of(),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                20,
                1_536L,
                0L);
        assertTrue(
                ModProcessingMachines.CENTRIFUGE.validator().validate(centrifugePanel)
                        .isEmpty(),
                "gt6_panel centrifuge must accept authored 1536 EUt");
        GTRecipe centrifugeOver = new GTRecipe(
                List.of(Ingredient.of(Items.REDSTONE)),
                List.of(1),
                List.of(new ItemStack(Items.IRON_INGOT)),
                List.of(),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                20,
                4_097L,
                0L);
        assertEquals(
                Optional.of("chemical_recipe_energy"),
                ModProcessingMachines.CENTRIFUGE.validator().validate(centrifugeOver));
    }

    @Test
    void mixerGt6PanelAcceptsAmountsAboveBronzeTank() {
        GTRecipe panelAmount = new GTRecipe(
                List.of(Ingredient.of(Items.REDSTONE)),
                List.of(1),
                List.of(new ItemStack(Items.IRON_NUGGET)),
                List.of(new FluidStack(Fluids.WATER, 1_000)),
                List.of(new FluidStack(Fluids.WATER, 33_750)),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                20,
                16L,
                0L);
        assertTrue(
                ModProcessingMachines.MIXER.validator().validate(panelAmount).isEmpty(),
                "gt6_panel Mixer must accept 33750 mB ordinary-closure amounts");
        GTRecipe overCapacity = new GTRecipe(
                List.of(Ingredient.of(Items.REDSTONE)),
                List.of(1),
                List.of(new ItemStack(Items.IRON_NUGGET)),
                List.of(),
                List.of(new FluidStack(
                        Fluids.WATER,
                        CompactPublicationGroups.GT6_PANEL_TANK_CAPACITY + 1)),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                20,
                16L,
                0L);
        assertTrue(
                ModProcessingMachines.MIXER.validator().validate(overCapacity).isEmpty(),
                "Mixer output tank accepts amounts above the old panel cap");
    }

    @Test
    void electrolyzerAcceptsCurrentSourceProjectionEnvelope() {
        GTRecipe projected = new GTRecipe(
                List.of(Ingredient.of(Items.COAL)),
                List.of(1),
                List.of(
                        new ItemStack(Items.IRON_NUGGET),
                        new ItemStack(Items.GOLD_NUGGET)),
                List.of(),
                List.of(new FluidStack(Fluids.WATER, 1_000)),
                List.of(
                        GTRecipe.GUARANTEED_CHANCE,
                        GTRecipe.GUARANTEED_CHANCE),
                16,
                854L,
                0L);

        assertTrue(ModProcessingMachines.ELECTROLYZER
                .validator()
                .validate(projected)
                .isEmpty());
    }

    @Test
    void autoclaveAcceptsPinnedLargeFluidRoute() {
        GTRecipe pinned = new GTRecipe(
                List.of(Ingredient.of(Items.REDSTONE), Ingredient.of(Items.COAL)),
                List.of(1, 1),
                List.of(
                        new ItemStack(Items.IRON_NUGGET),
                        new ItemStack(Items.GOLD_NUGGET),
                        new ItemStack(Items.COPPER_INGOT)),
                List.of(new FluidStack(Fluids.WATER, 2_500_000)),
                List.of(new FluidStack(Fluids.LAVA, 16_000)),
                List.of(
                        GTRecipe.GUARANTEED_CHANCE,
                        GTRecipe.GUARANTEED_CHANCE,
                        GTRecipe.GUARANTEED_CHANCE),
                20,
                1_024L,
                0L);

        assertTrue(ModProcessingMachines.AUTOCLAVE
                .validator()
                .validate(pinned)
                .isEmpty());
    }

    private static void assertLayout(
            ProcessingMachineSpec spec,
            int itemInputs,
            int itemOutputs,
            int fluidInputs,
            int fluidOutputs,
            int fluidInputCapacity,
            int fluidOutputCapacity) {
        assertEquals(itemInputs, spec.items().inputs().size(), spec.id().toString());
        assertEquals(itemOutputs, spec.items().outputs().size(), spec.id().toString());
        assertEquals(fluidInputs, spec.fluids().inputs().size(), spec.id().toString());
        assertEquals(fluidOutputs, spec.fluids().outputs().size(), spec.id().toString());
        spec.fluids().inputs().forEach(tank ->
                assertEquals(fluidInputCapacity, tank.capacity(), spec.id().toString()));
        spec.fluids().outputs().forEach(tank ->
                assertEquals(fluidOutputCapacity, tank.capacity(), spec.id().toString()));
    }

    private static GTRecipe validRecipe(ProcessingMachineSpec spec) {
        return new GTRecipe(
                List.of(Ingredient.of(Items.REDSTONE)),
                List.of(1),
                List.of(new ItemStack(Items.IRON_NUGGET)),
                spec.fluids().inputs().isEmpty()
                        ? List.of()
                        : List.of(new FluidStack(Fluids.WATER, 1_000)),
                spec.fluids().outputs().isEmpty()
                        ? List.of()
                        : List.of(new FluidStack(Fluids.LAVA, 500)),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                20,
                24L,
                0L);
    }

    private static GTRecipe maxLayoutRecipe(ProcessingMachineSpec spec) {
        return new GTRecipe(
                java.util.stream.IntStream.range(0, spec.items().inputs().size())
                        .mapToObj(index -> Ingredient.of(Items.REDSTONE))
                        .toList(),
                java.util.Collections.nCopies(spec.items().inputs().size(), 64),
                java.util.stream.IntStream.range(0, spec.items().outputs().size())
                        .mapToObj(index -> new ItemStack(Items.IRON_NUGGET))
                        .toList(),
                spec.fluids().inputs().stream()
                        .map(tank -> new FluidStack(Fluids.WATER, tank.capacity()))
                        .toList(),
                spec.fluids().outputs().stream()
                        .map(tank -> new FluidStack(Fluids.LAVA, tank.capacity()))
                        .toList(),
                java.util.Collections.nCopies(
                        spec.items().outputs().size(),
                        GTRecipe.GUARANTEED_CHANCE),
                20,
                spec.energy().maxPacket(),
                0L);
    }
}
