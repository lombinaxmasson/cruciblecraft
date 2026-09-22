package com.masson.cruciblecraft.content.multiblock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.fml.loading.LoadingModList;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MultiblockBuilderContractTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void localWindowUsesStrictGt6TwoBlockDistance() {
        BlockPos clicked = BlockPos.ZERO;
        assertTrue(MultiblockBuildPlan.near(
                clicked,
                new BlockPos(1, -1, 1)));
        assertFalse(MultiblockBuildPlan.near(
                clicked,
                new BlockPos(2, 0, 0)));
        assertFalse(MultiblockBuildPlan.near(
                clicked,
                new BlockPos(0, 0, -2)));
    }

    @Test
    void cellRequiresItsExactBlockItem() {
        var expected = Items.STICK;
        var other = Items.APPLE;
        MultiblockBuildCell cell = new MultiblockBuildCell(
                BlockPos.ZERO,
                state -> state == null,
                stack -> stack.getItem() == expected,
                true,
                "exact item");

        assertTrue(cell.matches((BlockState) null));
        assertTrue(cell.accepts(new ItemStack(expected)));
        assertFalse(cell.accepts(new ItemStack(other)));
    }
}
