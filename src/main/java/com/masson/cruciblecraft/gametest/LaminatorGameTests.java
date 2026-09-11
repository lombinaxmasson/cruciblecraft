package com.masson.cruciblecraft.gametest;

import java.util.List;

import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.recipe.gt.CompactRecipeFamilyProvider;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModProcessingMachines;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Runtime gate for the source-backed Laminator family.
 *
 * <p>The four hosts are source-exact, while the remaining unmapped MTE rows
 * stay outside the live compact family.
 */
@GameTestHolder(LaminatorGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class LaminatorGameTests {
    public static final String NAMESPACE = "cruciblecraft_wave_machines_laminator";
    private static final String TEMPLATE = "empty";
    private static final Direction FRONT = Direction.NORTH;
    private static final ResourceLocation PUBLICATION_GROUP =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "laminator/pilot/laminator");

    private LaminatorGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void playerSurfaceIsRegistered(GameTestHelper helper) {
        helper.assertTrue(
                ModBlocks.tieredProcessingBlocksById().containsKey(id("laminator")),
                "Laminator block is missing from the catalog");
        helper.assertTrue(
                ModProcessingMachines.LAMINATOR != null
                        && ModRecipeMaps.LAMINATOR != null,
                "Laminator RecipeMap or spec missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void liveMapPublishesFourHundredThirtyEightRows(
            GameTestHelper helper) {
        RecipeMap.RecipeFamily family = ModRecipeMaps.LAMINATOR
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.LAMINATOR.id(), PUBLICATION_GROUP))
                .orElse(null);
        helper.assertTrue(
                family != null && family.logicalRecipeCount() == 438,
                "Laminator compact family is not the 438 runtime rows: "
                        + (family == null ? "missing" : family.logicalRecipeCount()));
        helper.assertTrue(
                ModRecipeMaps.LAMINATOR.entries().size() == 438,
                "Laminator live map drifted from 438 runtime rows: "
                        + ModRecipeMaps.LAMINATOR.entries().size());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void allSourceExactHostsHaveAcquisitionRecipes(
            GameTestHelper helper) {
        for (String path : List.of(
                "laminator",
                "invar_laminator",
                "titanium_laminator",
                "tungsten_carbide_laminator")) {
            helper.assertTrue(
                    helper.getLevel().getRecipeManager()
                            .byKey(id("machines/" + path))
                            .isPresent(),
                    "Laminator host acquisition recipe missing: " + path);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void placedBlockResolvesLaminatorSpec(GameTestHelper helper) {
        Block block = ModBlocks.tieredProcessingBlocksById()
                .get(id("laminator"))
                .get();
        helper.setBlock(
                new BlockPos(2, 2, 2),
                block.defaultBlockState().setValue(
                        ProcessingMachineBlock.FACING, FRONT));
        ConfiguredProcessingMachineBlockEntity machine =
                helper.getBlockEntity(new BlockPos(2, 2, 2));
        helper.assertTrue(
                machine != null
                        && (machine.spec() == ModProcessingMachines.LAMINATOR
                                || machine.variant().kind().behavior()
                                        == ModProcessingMachines.LAMINATOR),
                "Placed block resolved the wrong machine kind");
        helper.succeed();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
