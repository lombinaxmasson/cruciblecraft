package com.masson.cruciblecraft.content.block;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.fml.loading.LoadingModList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CeramicMoldBlockRemovalTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void removalDropsMoldAndOnlyNonEmptyContents() {
        ItemStack mold = new ItemStack(Items.BRICK);
        ItemStack contents = new ItemStack(Items.COPPER_INGOT, 2);

        List<ItemStack> filled =
                CeramicMoldBlock.removalDrops(mold, contents);
        assertEquals(2, filled.size());
        assertSame(mold, filled.get(0));
        assertSame(contents, filled.get(1));

        List<ItemStack> empty = CeramicMoldBlock.removalDrops(
                mold, ItemStack.EMPTY);
        assertEquals(List.of(mold), empty);
    }

    @Test
    void onlyARealBlockReplacementDrops() {
        var unlit = Blocks.REDSTONE_LAMP.defaultBlockState();
        var lit = unlit.setValue(BlockStateProperties.LIT, true);

        assertFalse(CeramicMoldBlock.isBlockReplacement(unlit, lit));
        assertFalse(CeramicMoldBlock.isBlockReplacement(lit, unlit));
        assertTrue(CeramicMoldBlock.isBlockReplacement(
                lit, Blocks.STONE.defaultBlockState()));
        assertTrue(CeramicMoldBlock.isBlockReplacement(
                unlit, Blocks.AIR.defaultBlockState()));
    }
}
