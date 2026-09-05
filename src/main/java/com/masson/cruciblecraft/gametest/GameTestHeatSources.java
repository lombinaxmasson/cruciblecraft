package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.block.FuelGeneratorBlock;
import com.masson.cruciblecraft.content.blockentity.FuelGeneratorBlockEntity;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;

/** Shared HU fixture after the early firebox block was removed. */
public final class GameTestHeatSources {
    private static final ResourceLocation BRONZE_BURNING_BOX_GAS =
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, "bronze_burning_box_gas");

    private GameTestHeatSources() {}

    public static long energyCapacity() {
        return ((FuelGeneratorBlock) ModBlocks.converterBlocksById()
                .get(BRONZE_BURNING_BOX_GAS)
                .get())
                .spec()
                .energyCapacity();
    }

    public static FuelGeneratorBlockEntity placeHuSource(
            GameTestHelper helper, BlockPos pos) {
        helper.setBlock(
                pos,
                ModBlocks.converterBlocksById()
                        .get(BRONZE_BURNING_BOX_GAS)
                        .get()
                        .defaultBlockState()
                        .setValue(FuelGeneratorBlock.FACING, Direction.EAST));
        FuelGeneratorBlockEntity generator = helper.getBlockEntity(pos);
        helper.assertTrue(
                generator.seedStoredEnergy(generator.energyCapacity()),
                "Could not seed HU source at " + pos);
        return generator;
    }
}
