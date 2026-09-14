package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.content.block.FusionReactorBlock;
import com.masson.cruciblecraft.content.blockentity.FusionReactorBlockEntity;
import com.masson.cruciblecraft.energy.quantum.QuantumEnergizerBlock;
import com.masson.cruciblecraft.energy.quantum.QuantumEnergizerBlockEntity;
import com.masson.cruciblecraft.fusion.FusionRecipeCatalog;
import com.masson.cruciblecraft.fusion.FusionStructure;
import com.masson.cruciblecraft.recipe.crafting.ShapedCatalystRecipe;
import com.masson.cruciblecraft.recipe.gt.ComponentIngredientIndex;
import com.masson.cruciblecraft.recipe.gt.ItemInputAction;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModComponents;
import com.masson.cruciblecraft.registry.ModFluids;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Runtime gates for fusion/plasma. Plasma fuel map stays empty. */
@GameTestHolder(FusionPlasmaGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class FusionPlasmaGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_energy_fusion_quantum";
    private static final String TEMPLATE = "empty";

    private FusionPlasmaGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void eighteenFusionRowsExecute(GameTestHelper helper) {
        helper.assertTrue(
                FusionRecipeCatalog.entries().size() == 18,
                "Fusion catalog drifted from 18 GT6 rows");
        helper.assertTrue(
                ModRecipeMaps.FUSION.entries().size() == 18,
                "Fusion recipe map must publish exactly 18 rows");
        helper.assertTrue(
                helper.getLevel().getRecipeManager()
                        .byKey(id("machines/fusion_reactor"))
                        .isPresent(),
                "Fusion controller recipe is missing");
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(
                pos,
                ModBlocks.FUSION_REACTOR.get().defaultBlockState()
                        .setValue(FusionReactorBlock.FACING, Direction.NORTH));
        FusionReactorBlockEntity reactor = helper.getBlockEntity(pos);
        reactor.forceFormedForTest();
        ItemStack circuit = new ItemStack(ModItems.PROGRAMMED_CIRCUIT.get());
        circuit.set(ModComponents.CIRCUIT_CONFIG.get(), 1);
        reactor.setCircuitForTest(circuit);
        var deuterium = ModFluids.chemical("deuterium")
                .orElseThrow()
                .source()
                .get();
        helper.assertTrue(
                reactor.fillInput(new FluidStack(deuterium, 2_000)),
                "Could not fill deuterium into the fusion controller");
        long luPackets = FusionRecipeCatalog.entries().getFirst().luStart()
                / FusionRecipeCatalog.LU_PACKET;
        helper.assertTrue(
                reactor.insert(
                                EnergyType.LU,
                                FusionRecipeCatalog.LU_PACKET,
                                luPackets,
                                Direction.UP,
                                false)
                        == luPackets,
                "Formed fusion controller rejected the first-row LU start energy");
        reactor.skipToCompletionForTest();
        FusionReactorBlockEntity.serverTick(
                helper.getLevel(), pos, helper.getBlockState(pos), reactor);
        helper.assertTrue(
                reactor.outputAmount(0) == 500
                        && reactor.outputAmount(1) == 500,
                "Deuterium split did not emit helium-3 and tritium");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void hullCraftsAreSourceExact(GameTestHelper helper) {
        ItemStack circuit = new ItemStack(ModItems.PROGRAMMED_CIRCUIT.get());
        assertHullGrid(
                helper,
                "ventilation_unit",
                java.util.List.of("FwF", "CMC", "EdE"),
                circuit);
        assertHullGrid(
                helper,
                "versatile_processor_unit",
                java.util.List.of("DCS", "CMC", "RCE"),
                circuit);
        assertHullGrid(
                helper,
                "logic_processor_unit",
                java.util.List.of("PCP", "CMC", "PCP"),
                circuit);
        assertHullGrid(
                helper,
                "control_processor_unit",
                java.util.List.of("PCP", "CMC", "PCP"),
                circuit);
        assertHullGrid(
                helper,
                "storage_processor_unit",
                java.util.List.of("PCP", "CMC", "PCP"),
                circuit);
        assertHullGrid(
                helper,
                "conversion_processor_unit",
                java.util.List.of("PCP", "CMC", "PCP"),
                circuit);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void plasmaMapStaysEmpty(GameTestHelper helper) {
        helper.assertTrue(
                ModRecipeMaps.FUELS_PLASMA.entries().isEmpty(),
                "Plasma fuel map must stay empty");
        helper.assertTrue(
                ModRecipeMaps.FUSION.entries().size() == 18,
                "Fusion recipe map must stay 18 source-backed rows");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void neutralMatterBootstrapIsExtension(GameTestHelper helper) {
        helper.assertTrue(
                ModRecipeMaps.FUSION.entries().size() == 18,
                "Neutral matter must not become a 19th FUSION row");
        helper.assertTrue(
                ModRecipeMaps.FUSION_EXTENSION.entries().stream().anyMatch(
                        entry -> entry.id().getPath().equals(
                                "fusion_extension/neutral_matter_bootstrap")),
                "CC_EXTENSION neutral-matter bootstrap is missing");
        boolean circuitThree = ModRecipeMaps.FUSION_EXTENSION.entries().stream()
                .anyMatch(entry -> {
                    var extraction = ComponentIngredientIndex.extract(
                            entry.recipe().itemInputs().getFirst());
                    return extraction.supported()
                            && "3".equals(extraction.keys().getFirst().value());
                });
        helper.assertTrue(
                circuitThree,
                "Neutral-matter bootstrap must keep ST.tag(3)");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void quantumEnergizerConvertsLuToQu(GameTestHelper helper) {
        var t1 = ModBlocks.quantumEnergizerBlocksById().get(
                id("quantum_energizer_t1"));
        helper.assertTrue(t1 != null, "Quantum Energizer T1 is not registered");
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(
                pos,
                t1.get().defaultBlockState()
                        .setValue(QuantumEnergizerBlock.FACING, Direction.NORTH));
        QuantumEnergizerBlockEntity energizer = helper.getBlockEntity(pos);
        helper.assertTrue(
                energizer.insert(
                                EnergyType.LU,
                                32L,
                                1L,
                                Direction.SOUTH,
                                false)
                        == 1L,
                "T1 Quantum Energizer rejected LU from a non-front side");
        helper.assertTrue(
                energizer.insert(
                                EnergyType.LU,
                                32L,
                                1L,
                                Direction.NORTH,
                                false)
                        == 0L,
                "T1 Quantum Energizer accepted LU on the QU output face");
        QuantumEnergizerBlockEntity.serverTick(
                helper.getLevel(), pos, helper.getBlockState(pos), energizer);
        helper.assertTrue(
                energizer.stored(EnergyType.LU) == 0L
                        && energizer.stored(EnergyType.QUANTUM) == 16L,
                "T1 Quantum Energizer did not convert LU to QU at 1/2");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void circuitSelectorIsPreserved(GameTestHelper helper) {
        boolean sawOne = false;
        boolean sawTwo = false;
        for (var published : ModRecipeMaps.FUSION.entries()) {
            helper.assertTrue(
                    published.recipe().itemInputs().size() == 1,
                    "Fusion row must keep the GT6 circuit selector");
            helper.assertTrue(
                    published.recipe().itemInputCounts().getFirst() == 0
                            && published.recipe().itemInputActions().getFirst().kind()
                                    == ItemInputAction.Kind.PRESERVE,
                    "Circuit selector must PRESERVE at count 0");
            var extraction = ComponentIngredientIndex.extract(
                    published.recipe().itemInputs().getFirst());
            helper.assertTrue(
                    extraction.supported() && extraction.keys().size() == 1,
                    "Circuit selector must be an indexable CIRCUIT_CONFIG ingredient");
            int config = Integer.parseInt(extraction.keys().getFirst().value());
            if (config == 1) {
                sawOne = true;
            }
            if (config == 2) {
                sawTwo = true;
            }
        }
        helper.assertTrue(
                sawOne && sawTwo,
                "Fusion map must include both ST.tag(1) and ST.tag(2)");
        helper.assertTrue(
                FusionRecipeCatalog.entries().getFirst().luStart()
                        == 730L * 8192L * 16L,
                "First fusion LU start energy drifted");
        helper.assertTrue(
                FusionRecipeCatalog.entries().getLast().luStart()
                        == 94956L * 8192L * 16L,
                "Adamantium fusion LU start energy drifted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void octagonPartCountsMatchTooltip(GameTestHelper helper) {
        helper.assertTrue(
                FusionStructure.counts().matchesTooltip(),
                "Fusion octagon part counts drifted from the GT6 tooltip");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void energyRejectedUntilStructureForms(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(
                pos,
                ModBlocks.FUSION_REACTOR.get().defaultBlockState()
                        .setValue(FusionReactorBlock.FACING, Direction.NORTH));
        FusionReactorBlockEntity reactor = helper.getBlockEntity(pos);
        FusionReactorBlockEntity.serverTick(
                helper.getLevel(), pos, helper.getBlockState(pos), reactor);
        helper.assertTrue(
                !reactor.formed(),
                "Bare controller must not report a formed octagon");
        helper.assertTrue(
                reactor.insert(
                                EnergyType.TIME,
                                8192L,
                                1L,
                                Direction.NORTH,
                                false)
                        == 0L,
                "TU was accepted without a formed structure");
        helper.assertTrue(
                reactor.insert(
                                EnergyType.LU,
                                32L,
                                1L,
                                Direction.NORTH,
                                false)
                        == 0L,
                "LU was accepted without a formed structure");
        reactor.forceFormedForTest();
        helper.assertTrue(
                reactor.insert(
                                EnergyType.TIME,
                                8192L,
                                1L,
                                Direction.NORTH,
                                false)
                        == 1L,
                "Formed fusion controller rejected TU");
        helper.assertTrue(
                reactor.insert(
                                EnergyType.LU,
                                32L,
                                1L,
                                Direction.NORTH,
                                false)
                        == 1L,
                "Formed fusion controller rejected LU");
        helper.assertTrue(
                reactor.stored(EnergyType.TIME) == 8192L
                        && reactor.stored(EnergyType.LU) == 32L,
                "Dual TU/LU buffers did not persist both identities");
        helper.succeed();
    }

    private static void assertHullGrid(
            GameTestHelper helper,
            String path,
            java.util.List<String> pattern,
            ItemStack circuit) {
        var holder = helper.getLevel().getRecipeManager().byKey(id(path));
        helper.assertTrue(holder.isPresent(), path + " craft is missing");
        helper.assertTrue(
                holder.orElseThrow().value() instanceof ShapedCatalystRecipe,
                path + " is not a shaped catalyst recipe");
        ShapedCatalystRecipe shaped =
                (ShapedCatalystRecipe) holder.orElseThrow().value();
        helper.assertTrue(
                pattern.equals(shaped.pattern()),
                path + " drifted from the GT6 hull grid");
        for (Ingredient ingredient : shaped.ingredients().values()) {
            helper.assertFalse(
                    ingredient.test(circuit),
                    path + " used programmed_circuit");
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
