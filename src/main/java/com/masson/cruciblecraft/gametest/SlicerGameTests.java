package com.masson.cruciblecraft.gametest;

import java.util.List;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineEnergyPlacement;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.recipe.gt.CompactRecipeFamilyProvider;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated Slicer gate. Run with {@code -PwaveRecipes=machines/slicer}.
 */
@GameTestHolder(SlicerGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class SlicerGameTests {
    public static final String NAMESPACE = "cruciblecraft_wave_machines_slicer";
    private static final String TEMPLATE = "empty";
    private static final Direction FRONT = Direction.NORTH;
    private static final BlockPos POS = new BlockPos(2, 2, 2);
    private static final ResourceLocation PUBLICATION_GROUP =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "slicer/pilot/slicer");
    private static final List<String> HOSTS = List.of(
            "slicer",
            "aluminium_slicer",
            "stainless_steel_slicer",
            "chromium_slicer",
            "titanium_slicer");

    private SlicerGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void playerSurfaceIsRegistered(GameTestHelper helper) {
        helper.assertTrue(
                ModItems.tieredProcessingItemsById().get(id("slicer")).get()
                        != null,
                "Galvanized-steel slicer item missing");
        helper.assertTrue(
                ModRecipeMaps.SLICER != null
                        && ModProcessingMachines.SLICER != null,
                "Slicer RecipeMap or spec missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void liveMapPublishesThirtyTwoSelectedRows(GameTestHelper helper) {
        RecipeMap.RecipeFamily family = ModRecipeMaps.SLICER
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.SLICER.id(), PUBLICATION_GROUP))
                .orElse(null);
        helper.assertTrue(
                family != null && family.logicalRecipeCount() == 32,
                "Slicer compact family is not the 32 selected rows: "
                        + (family == null ? "missing" : family.logicalRecipeCount()));
        helper.assertTrue(
                ModRecipeMaps.SLICER.entries().size() == 32,
                "Slicer live map drifted from 32 selected rows: "
                        + ModRecipeMaps.SLICER.entries().size());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void steelHostRunsFirstLiveRecipe(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity machine = place(
                helper, POS, ModBlocks.SLICER.get(), ModProcessingMachines.SLICER);
        helper.assertTrue(
                helper.getBlockState(POS).getValue(ProcessingMachineBlock.FACING)
                        == FRONT,
                "Slicer default facing is not NORTH");
        RecipeMap.RecipeFamily family = ModRecipeMaps.SLICER
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.SLICER.id(), PUBLICATION_GROUP))
                .orElseThrow(() -> new IllegalStateException(
                        "Missing slicer compact family"));
        RecipeMap.Entry first = family.enumerationEntry(0);
        fillEu(helper, machine, Direction.SOUTH);
        loadRecipeInputs(machine, first.recipe());
        fillEu(helper, machine, Direction.SOUTH);
        helper.startSequence()
                .thenExecuteFor(4, () -> fillEu(helper, machine, Direction.SOUTH))
                .thenExecute(() -> helper.assertTrue(
                        machine.workProgressLong() > 0L
                                || machine.duration() > 0
                                || hasAnyOutput(machine),
                        "Slicer did not select the first live recipe: "
                                + machine.pausedReason()))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fiveHostsAreNotSurvivalCraftable(GameTestHelper helper) {
        for (String path : HOSTS) {
            Item result = ModItems.tieredProcessingItemsById()
                    .get(id(path))
                    .get();
            helper.assertTrue(
                    result != null,
                    "Slicer host item missing: " + path);
            helper.assertFalse(
                    helper.getLevel().getRecipeManager()
                            .byKey(id("machines/" + path))
                            .isPresent(),
                    "Slicer host must not have a survival recipe: " + path);
        }
        helper.succeed();
    }

    private static ConfiguredProcessingMachineBlockEntity place(
            GameTestHelper helper,
            BlockPos pos,
            Block block,
            ProcessingMachineSpec spec) {
        helper.setBlock(
                pos,
                block.defaultBlockState().setValue(
                        ProcessingMachineBlock.FACING, FRONT));
        ConfiguredProcessingMachineBlockEntity machine = helper.getBlockEntity(pos);
        helper.assertTrue(
                machine.spec() == spec
                        || machine.variant().kind().behavior() == spec,
                "Placed block resolved wrong machine kind");
        return machine;
    }

    private static void loadRecipeInputs(
            ConfiguredProcessingMachineBlockEntity machine, GTRecipe recipe) {
        for (int i = 0; i < recipe.itemInputs().size(); i++) {
            ItemStack sample = recipe.itemInputs().get(i).getItems()[0].copy();
            sample.setCount(Math.max(1, recipe.itemInputCounts().get(i)));
            machine.inventory().setStackInSlot(
                    machine.spec().items().inputs().get(i), sample);
        }
    }

    private static boolean hasAnyOutput(
            ConfiguredProcessingMachineBlockEntity machine) {
        return machine.spec().items().outputs().stream().anyMatch(slot ->
                !machine.inventory().getStackInSlot(slot).isEmpty());
    }

    private static void fillEu(
            GameTestHelper helper,
            ConfiguredProcessingMachineBlockEntity machine,
            Direction requiredSide) {
        EnergyType type = machine.spec().energy().type();
        Direction front = machine.getBlockState().getValue(ProcessingMachineBlock.FACING);
        Direction back = ProcessingMachineEnergyPlacement
                .connection(machine.spec(), front)
                .providerOffset();
        helper.assertTrue(
                back == requiredSide,
                "Slicer energy side drifted from SOUTH on NORTH facing: " + back);
        long packet = Math.max(1L, machine.spec().energy().maxPacket());
        long target = machine.capacity(type);
        for (int i = 0; i < 16 && machine.stored(type) < target; i++) {
            if (machine.insert(type, packet, 1L, requiredSide, false) <= 0L) {
                break;
            }
        }
        helper.assertTrue(
                machine.stored(type) > 0L,
                "Placed slicer stored no " + type + " on " + requiredSide);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
