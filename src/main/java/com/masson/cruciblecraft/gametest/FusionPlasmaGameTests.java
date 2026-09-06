package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.content.block.FusionReactorBlock;
import com.masson.cruciblecraft.content.blockentity.FusionReactorBlockEntity;
import com.masson.cruciblecraft.fusion.FusionRecipeCatalog;
import com.masson.cruciblecraft.fusion.FusionStructure;
import com.masson.cruciblecraft.recipe.gt.ComponentIngredientIndex;
import com.masson.cruciblecraft.recipe.gt.ItemInputAction;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Runtime gates for fusion/plasma. Plasma fuel map stays empty. */
@GameTestHolder(FusionPlasmaGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class FusionPlasmaGameTests {
    public static final String NAMESPACE = "cruciblecraft_fusion_plasma";
    private static final String TEMPLATE = "empty";

    private FusionPlasmaGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void eighteenFusionRowsAndEmptyPlasmaMap(GameTestHelper helper) {
        helper.assertTrue(
                FusionRecipeCatalog.entries().size() == 18,
                "Fusion catalog drifted from 18 GT6 rows");
        helper.assertTrue(
                ModRecipeMaps.FUSION.entries().size() == 18,
                "Fusion recipe map must publish exactly 18 rows");
        helper.assertTrue(
                ModRecipeMaps.FUELS_PLASMA.entries().isEmpty(),
                "Plasma fuel map must stay empty");
        helper.assertTrue(
                helper.getLevel().getRecipeManager()
                        .byKey(id("fusion_reactor"))
                        .isEmpty(),
                "Fusion controller recipe must stay blocked");
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

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
