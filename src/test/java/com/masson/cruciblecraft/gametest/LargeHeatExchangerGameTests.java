package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.content.blockentity.CrucibleBlockEntity;
import com.masson.cruciblecraft.energy.largeheatexchanger.LargeHeatExchangerBlock;
import com.masson.cruciblecraft.energy.largeheatexchanger.LargeHeatExchangerBlockEntity;
import com.masson.cruciblecraft.energy.largeheatexchanger.LargeHeatExchangerCatalog;
import com.masson.cruciblecraft.energy.largeheatexchanger.LargeHeatExchangerStructure;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Isolated 17197 large heat-exchanger gate. */
@GameTestHolder(LargeHeatExchangerGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class LargeHeatExchangerGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_energy_large_heat_exchanger";
    private static final String TEMPLATE = "empty";
    private static final BlockPos CONTROLLER = new BlockPos(2, 1, 2);

    private LargeHeatExchangerGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void largeHexFormsAndEmitsHu(GameTestHelper helper) {
        LargeHeatExchangerBlockEntity exchanger = placeFormed(helper);
        helper.assertTrue(exchanger.formed(), "3x3x2 large HEX did not form");
        helper.assertTrue(
                exchanger.fillInput(hotTin(64)),
                "Could not fill large HEX with hot tin");
        LargeHeatExchangerBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(CONTROLLER),
                helper.getBlockState(CONTROLLER),
                exchanger);
        helper.assertTrue(
                exchanger.energyStored() > 0L && exchanger.outputAmount() > 0,
                "Large HEX did not buffer HU from hot tin");
        Vec3i transmitter = LargeHeatExchangerStructure.transmitters()[0];
        BlockPos sink = CONTROLLER.offset(transmitter).above();
        helper.setBlock(sink, ModBlocks.steelSmeltingCrucible().get().defaultBlockState());
        long before = exchanger.energyStored();
        LargeHeatExchangerBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(CONTROLLER),
                helper.getBlockState(CONTROLLER),
                exchanger);
        CrucibleBlockEntity crucible = helper.getBlockEntity(sink);
        helper.assertTrue(
                exchanger.energyStored() < before
                        || (crucible != null
                                && crucible.stored(
                                        com.masson.cruciblecraft.api.energy
                                                .EnergyType.HEAT)
                                        > 0L),
                "Large HEX did not emit HU upward from a transmitter");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void largeHexReloadPreservesTanks(GameTestHelper helper) {
        LargeHeatExchangerBlockEntity exchanger = placeFormed(helper);
        helper.assertTrue(
                exchanger.fillInput(hotTin(8)),
                "Could not fill tanks before reload");
        LargeHeatExchangerBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(CONTROLLER),
                helper.getBlockState(CONTROLLER),
                exchanger);
        long stored = exchanger.energyStored();
        int input = exchanger.inputAmount();
        int output = exchanger.outputAmount();
        helper.assertTrue(stored > 0L && output > 0, "Need HU before reload");
        var registries = helper.getLevel().registryAccess();
        CompoundTag saved = exchanger.saveWithoutMetadata(registries);
        helper.setBlock(
                CONTROLLER,
                ModBlocks.LARGE_HEAT_EXCHANGER.get().defaultBlockState()
                        .setValue(LargeHeatExchangerBlock.FACING, Direction.NORTH));
        LargeHeatExchangerBlockEntity reloaded = helper.getBlockEntity(CONTROLLER);
        helper.assertTrue(reloaded != null, "Missing reloaded large HEX");
        reloaded.loadWithComponents(saved, registries);
        helper.assertTrue(
                reloaded.energyStored() == stored
                        && reloaded.inputAmount() == input
                        && reloaded.outputAmount() == output,
                "Large HEX tanks or HU did not survive reload");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void largeHexIsSurvivalCraftable(GameTestHelper helper) {
        helper.assertTrue(
                helper.getLevel().getRecipeManager()
                        .byKey(id("large_heat_exchanger"))
                        .isPresent(),
                "Missing large heat exchanger survival recipe");
        helper.assertTrue(
                LargeHeatExchangerCatalog.profile().sourceId() == 17197,
                "Large HEX source id drifted from 17197");
        helper.succeed();
    }

    private static LargeHeatExchangerBlockEntity placeFormed(GameTestHelper helper) {
        var profile = LargeHeatExchangerCatalog.profile();
        Block wall = ModBlocks.mteInPlaceBlocksById().get(profile.wallId()).get();
        Block transmitter = ModBlocks.mteInPlaceBlocksById()
                .get(profile.transmitterId())
                .get();
        helper.setBlock(
                CONTROLLER,
                ModBlocks.LARGE_HEAT_EXCHANGER.get().defaultBlockState()
                        .setValue(LargeHeatExchangerBlock.FACING, Direction.NORTH));
        int[][] ring = {
            {-1, 0, -1}, {0, 0, -1}, {1, 0, -1},
            {-1, 0, 0}, {1, 0, 0},
            {-1, 0, 1}, {0, 0, 1}, {1, 0, 1}
        };
        for (int[] offset : ring) {
            helper.setBlock(
                    CONTROLLER.offset(offset[0], offset[1], offset[2]),
                    wall.defaultBlockState());
        }
        helper.setBlock(CONTROLLER.above(), wall.defaultBlockState());
        for (Vec3i offset : LargeHeatExchangerStructure.transmitters()) {
            helper.setBlock(
                    CONTROLLER.offset(offset),
                    transmitter.defaultBlockState());
        }
        LargeHeatExchangerBlockEntity exchanger = helper.getBlockEntity(CONTROLLER);
        helper.assertTrue(exchanger != null, "Missing large HEX controller");
        LargeHeatExchangerBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(CONTROLLER),
                helper.getBlockState(CONTROLLER),
                exchanger);
        return exchanger;
    }

    private static FluidStack hotTin(int amount) {
        return new FluidStack(
                ModFluids.hotSource("hot_molten_tin").orElseThrow(), amount);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
