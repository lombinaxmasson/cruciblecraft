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

/** Runtime gate for the shared RU/EU Loom RecipeMap family. */
@GameTestHolder(LoomGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class LoomGameTests {
    public static final String NAMESPACE = "cruciblecraft_machines";
    private static final String TEMPLATE = "empty";
    private static final Direction FRONT = Direction.NORTH;
    private static final ResourceLocation PUBLICATION_GROUP =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "loom/pilot/loom");

    private LoomGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void playerSurfaceIsRegistered(GameTestHelper helper) {
        helper.assertTrue(
                ModBlocks.tieredProcessingBlocksById().containsKey(id("loom")),
                "Kinetic Loom block is missing from the catalog");
        helper.assertTrue(
                ModProcessingMachines.LOOM != null
                        && ModProcessingMachines.ELECTRICLOOM != null
                        && ModRecipeMaps.LOOM != null,
                "Loom specs or RecipeMap missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void sharedMapPublishesFourHundredSixtyFiveRows(
            GameTestHelper helper) {
        RecipeMap.RecipeFamily family = ModRecipeMaps.LOOM
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.LOOM.id(), PUBLICATION_GROUP))
                .orElse(null);
        helper.assertTrue(
                family != null && family.logicalRecipeCount() == 465,
                "Loom family is not the 465 non-shadowed rows: "
                        + (family == null ? "missing" : family.logicalRecipeCount()));
        helper.assertTrue(
                ModRecipeMaps.LOOM.entries().size() == 465,
                "Loom map drifted from 465 non-shadowed rows: "
                        + ModRecipeMaps.LOOM.entries().size());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void allAcquisitionHostsAreExact(
            GameTestHelper helper) {
        for (String path : List.of(
                "loom",
                "steel_loom",
                "titanium_loom",
                "tungstensteel_loom",
                "electricloom",
                "aluminium_electricloom",
                "stainless_steel_electricloom",
                "chromium_electricloom",
                "titanium_electricloom")) {
            helper.assertTrue(
                    helper.getLevel().getRecipeManager()
                            .byKey(id("machines/" + path))
                            .isPresent(),
                    "Loom acquisition recipe missing: " + path);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void placedKineticLoomResolvesSpec(GameTestHelper helper) {
        Block block = ModBlocks.tieredProcessingBlocksById()
                .get(id("loom"))
                .get();
        helper.setBlock(
                new BlockPos(2, 2, 2),
                block.defaultBlockState().setValue(
                        ProcessingMachineBlock.FACING, FRONT));
        ConfiguredProcessingMachineBlockEntity machine =
                helper.getBlockEntity(new BlockPos(2, 2, 2));
        helper.assertTrue(
                machine != null
                        && (machine.spec() == ModProcessingMachines.LOOM
                                || machine.variant().kind().behavior()
                                        == ModProcessingMachines.LOOM),
                "Placed Loom resolved the wrong machine kind");
        helper.succeed();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
