package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.machine.processing.MachineKindSpec;
import com.masson.cruciblecraft.machine.processing.MachineVariant;
import com.masson.cruciblecraft.recipe.gt.CompactRecipeFamilyProvider;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.gametest.support.PublicationPolicyCounts;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.registry.ModMachineVariants;
import com.masson.cruciblecraft.registry.ModProcessingMachines;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Runtime gates for the live GT6 Melter family. */
@GameTestHolder(MelterGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class MelterGameTests {
    public static final String NAMESPACE = "cruciblecraft_machines";
    private static final String TEMPLATE = "empty";
    private static final Direction FRONT = Direction.NORTH;
    private static final ResourceLocation PUBLICATION_GROUP =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "melter/pilot/melter");

    private MelterGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void playerSurfaceIsRegistered(GameTestHelper helper) {
        helper.assertTrue(
                ModBlocks.tieredProcessingBlocksById().containsKey(id("melter")),
                "Melter block is missing from the catalog");
        helper.assertTrue(
                ModItems.tieredProcessingItemsById().containsKey(id("melter")),
                "Melter item is missing from the catalog");
        helper.assertTrue(
                ModProcessingMachines.MELTER != null
                        && ModRecipeMaps.MELTER != null,
                "Melter RecipeMap or spec missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void liveMapPublishesAllSelectedRows(GameTestHelper helper) {
        RecipeMap.RecipeFamily family = ModRecipeMaps.MELTER
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.MELTER.id(), PUBLICATION_GROUP))
                .orElse(null);
        int expected = PublicationPolicyCounts.relationCount(helper, PUBLICATION_GROUP);
        helper.assertTrue(
                family != null && family.logicalRecipeCount() == expected,
                "Melter family logical rows != policy relation_count "
                        + expected
                        + ": "
                        + (family == null ? "missing" : family.logicalRecipeCount()));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void sourceExactHostHasAcquisitionRecipe(GameTestHelper helper) {
        helper.assertTrue(
                helper.getLevel().getRecipeManager()
                        .byKey(id("machines/melter"))
                        .isPresent(),
                "Melter source-exact host acquisition recipe is missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void placedBlockResolvesMelterSpecAndParallelPolicy(
            GameTestHelper helper) {
        MachineVariant variant = ModMachineVariants.require(id("melter"));
        MachineKindSpec kind = variant.kind();
        helper.assertTrue(
                variant.tierBand().parallelLimit() == 1_000
                        && kind.parallelDuration()
                        && kind.overclockPolicy()
                                == MachineKindSpec.OverclockPolicy.CHEAP,
                "Melter source parallel or overclock policy drifted");

        Block block = ModBlocks.tieredProcessingBlocksById()
                .get(id("melter"))
                .get();
        helper.setBlock(
                new BlockPos(2, 2, 2),
                block.defaultBlockState().setValue(
                        ProcessingMachineBlock.FACING, FRONT));
        ConfiguredProcessingMachineBlockEntity machine =
                helper.getBlockEntity(new BlockPos(2, 2, 2));
        helper.assertTrue(
                machine != null
                        && (machine.spec() == ModProcessingMachines.MELTER
                                || machine.variant().kind().behavior()
                                        == ModProcessingMachines.MELTER),
                "Placed block resolved the wrong Melter machine kind");
        helper.succeed();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
