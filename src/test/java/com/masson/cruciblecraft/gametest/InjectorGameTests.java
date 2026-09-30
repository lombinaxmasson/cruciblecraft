package com.masson.cruciblecraft.gametest;

import java.util.List;

import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.gametest.support.PublicationPolicyCounts;
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

/** Runtime gate for the Injector family with LV and EV exact hosts. */
@GameTestHolder(InjectorGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class InjectorGameTests {
    public static final String NAMESPACE = "cruciblecraft_machines";
    private static final String TEMPLATE = "empty";
    private static final Direction FRONT = Direction.NORTH;
    private static final ResourceLocation PUBLICATION_GROUP =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "injector/pilot/injector");

    private InjectorGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void playerSurfaceIsRegistered(GameTestHelper helper) {
        helper.assertTrue(
                ModBlocks.tieredProcessingBlocksById().containsKey(id("injector")),
                "Injector block is missing from the catalog");
        helper.assertTrue(
                ModProcessingMachines.INJECTOR != null
                        && ModRecipeMaps.INJECTOR != null,
                "Injector RecipeMap or spec missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void liveMapPublishesSixHundredElevenRows(
            GameTestHelper helper) {
        RecipeMap.RecipeFamily family = ModRecipeMaps.INJECTOR
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.INJECTOR.id(), PUBLICATION_GROUP))
                .orElse(null);
        int expected = PublicationPolicyCounts.relationCount(helper, PUBLICATION_GROUP);
        helper.assertTrue(
                family != null && family.logicalRecipeCount() == expected,
                "Injector family logical rows != policy relation_count "
                        + expected
                        + ": "
                        + (family == null ? "missing" : family.logicalRecipeCount()));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void allHostsAreExact(
            GameTestHelper helper) {
        for (String path : List.of(
                "injector",
                "aluminium_injector",
                "stainless_steel_injector",
                "chromium_injector",
                "titanium_injector")) {
            helper.assertTrue(
                    helper.getLevel().getRecipeManager()
                            .byKey(id("machines/" + path))
                            .isPresent(),
                    "Exact Injector host missing acquisition recipe: " + path);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void placedInjectorResolvesSpec(GameTestHelper helper) {
        Block block = ModBlocks.tieredProcessingBlocksById()
                .get(id("chromium_injector"))
                .get();
        helper.setBlock(
                new BlockPos(2, 2, 2),
                block.defaultBlockState().setValue(
                        ProcessingMachineBlock.FACING, FRONT));
        ConfiguredProcessingMachineBlockEntity machine =
                helper.getBlockEntity(new BlockPos(2, 2, 2));
        helper.assertTrue(
                machine != null
                        && (machine.spec() == ModProcessingMachines.INJECTOR
                                || machine.variant().kind().behavior()
                                        == ModProcessingMachines.INJECTOR),
                "Placed Injector resolved the wrong machine kind");
        helper.succeed();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
