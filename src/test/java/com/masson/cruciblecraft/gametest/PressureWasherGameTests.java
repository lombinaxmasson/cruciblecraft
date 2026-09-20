package com.masson.cruciblecraft.gametest;

import java.util.List;

import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.block.WoodDebark;
import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.recipe.gt.CompactRecipeFamilyProvider;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModProcessingMachines;
import com.masson.cruciblecraft.registry.ModRecipeMaps;
import com.masson.cruciblecraft.worldgen.tree.prep.GtTreeSpecies;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Runtime gate for the source-backed Pressure Washer / Debarker family. */
@GameTestHolder(PressureWasherGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class PressureWasherGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_machines_pressure_washer";
    private static final String TEMPLATE = "empty";
    private static final Direction FRONT = Direction.NORTH;
    private static final ResourceLocation PUBLICATION_GROUP =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "pressurewasher/pilot/pressure_washer");

    private PressureWasherGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void playerSurfaceIsRegistered(GameTestHelper helper) {
        helper.assertTrue(
                ModBlocks.tieredProcessingBlocksById()
                        .containsKey(id("pressurewasher")),
                "Pressure Washer block is missing from the catalog");
        helper.assertTrue(
                ModProcessingMachines.PRESSUREWASHER != null
                        && ModRecipeMaps.PRESSUREWASHER != null,
                "Pressure Washer RecipeMap or spec missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void liveMapPublishesOneHundredNinetyTwoRows(
            GameTestHelper helper) {
        RecipeMap.RecipeFamily family = ModRecipeMaps.PRESSUREWASHER
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.PRESSUREWASHER.id(), PUBLICATION_GROUP))
                .orElse(null);
        helper.assertTrue(
                family != null && family.logicalRecipeCount() == 192,
                "Pressure Washer family is not the 192 selected rows: "
                        + (family == null ? "missing" : family.logicalRecipeCount()));
        int woodRows = WoodDebark.VANILLA_PAIRS.size() + GtTreeSpecies.ALL.size();
        helper.assertTrue(
                ModRecipeMaps.PRESSUREWASHER.entries().size() == 192 + woodRows,
                "Pressure Washer live map drifted from compact plus wood rows: "
                        + ModRecipeMaps.PRESSUREWASHER.entries().size());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void allSourceExactHostsHaveAcquisitionRecipes(
            GameTestHelper helper) {
        for (String path : List.of(
                "pressurewasher",
                "steel_pressurewasher",
                "titanium_pressurewasher",
                "tungstensteel_pressurewasher")) {
            helper.assertTrue(
                    helper.getLevel().getRecipeManager()
                            .byKey(id("machines/" + path))
                            .isPresent(),
                    "Pressure Washer acquisition recipe missing: " + path);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void placedBlockResolvesPressureWasherSpec(
            GameTestHelper helper) {
        Block block = ModBlocks.tieredProcessingBlocksById()
                .get(id("pressurewasher"))
                .get();
        helper.setBlock(
                new BlockPos(2, 2, 2),
                block.defaultBlockState().setValue(
                        ProcessingMachineBlock.FACING, FRONT));
        ConfiguredProcessingMachineBlockEntity machine =
                helper.getBlockEntity(new BlockPos(2, 2, 2));
        helper.assertTrue(
                machine != null
                        && (machine.spec() == ModProcessingMachines.PRESSUREWASHER
                                || machine.variant().kind().behavior()
                                        == ModProcessingMachines.PRESSUREWASHER),
                "Placed block resolved the wrong machine kind");
        helper.succeed();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
