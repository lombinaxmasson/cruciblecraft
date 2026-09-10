package com.masson.cruciblecraft.gametest;

import java.util.List;

import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.recipe.gt.CompactRecipeFamilyProvider;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModItems;
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

/** Runtime gate for Nanofab: live map, blocked obtain. */
@GameTestHolder(NanofabGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class NanofabGameTests {
    public static final String NAMESPACE = "cruciblecraft_wave_machines_nanofab";
    private static final String TEMPLATE = "empty";
    private static final Direction FRONT = Direction.NORTH;
    private static final ResourceLocation PUBLICATION_GROUP =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "nanofab/pilot/nanofab");
    private static final List<String> HOSTS = List.of(
            "nanofab",
            "aluminium_nanofab",
            "stainless_steel_nanofab",
            "chromium_nanofab",
            "titanium_nanofab");

    private NanofabGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void playerSurfaceIsRegistered(GameTestHelper helper) {
        helper.assertTrue(
                ModBlocks.tieredProcessingBlocksById().containsKey(id("nanofab")),
                "Nanofab block is missing from the catalog");
        helper.assertTrue(
                ModProcessingMachines.NANOFAB != null
                        && ModRecipeMaps.NANOFAB != null,
                "Nanofab RecipeMap or spec missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void liveMapPublishesFiftyTwoRows(GameTestHelper helper) {
        RecipeMap.RecipeFamily family = ModRecipeMaps.NANOFAB
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.NANOFAB.id(), PUBLICATION_GROUP))
                .orElse(null);
        helper.assertTrue(
                family != null && family.logicalRecipeCount() == 7,
                "Nanofab family is not the 7 non-shadowed rows: "
                        + (family == null ? "missing" : family.logicalRecipeCount()));
        helper.assertTrue(
                ModRecipeMaps.NANOFAB.entries().size() == 7,
                "Nanofab map drifted from 7 rows: "
                        + ModRecipeMaps.NANOFAB.entries().size());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fiveHostsAreNotSurvivalCraftable(GameTestHelper helper) {
        for (String path : HOSTS) {
            Item result = ModItems.tieredProcessingItemsById()
                    .get(id(path))
                    .get();
            helper.assertTrue(result != null, "Nanofab host item missing: " + path);
            helper.assertFalse(
                    helper.getLevel().getRecipeManager()
                            .byKey(id("machines/" + path))
                            .isPresent(),
                    "Nanofab host must not have a survival recipe: " + path);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void placedNanofabResolvesSpec(GameTestHelper helper) {
        Block block = ModBlocks.tieredProcessingBlocksById()
                .get(id("nanofab"))
                .get();
        helper.setBlock(
                new BlockPos(2, 2, 2),
                block.defaultBlockState().setValue(
                        ProcessingMachineBlock.FACING, FRONT));
        ConfiguredProcessingMachineBlockEntity machine =
                helper.getBlockEntity(new BlockPos(2, 2, 2));
        helper.assertTrue(
                machine != null
                        && (machine.spec() == ModProcessingMachines.NANOFAB
                                || machine.variant().kind().behavior()
                                        == ModProcessingMachines.NANOFAB),
                "Placed Nanofab resolved the wrong machine kind");
        helper.succeed();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
