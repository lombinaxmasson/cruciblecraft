package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.content.block.StorageHostBlock;
import com.masson.cruciblecraft.content.blockentity.MassStorageBlockEntity;
import com.masson.cruciblecraft.content.storage.StorageVariantCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

@GameTestHolder(MteFurnitureBarrelRuntimeGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class MteFurnitureBarrelRuntimeGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_content_gt6_mte_furniture_barrel_runtime";
    private static final String TEMPLATE = "empty";

    private MteFurnitureBarrelRuntimeGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void skyrootBarrelIsLiveInventory(GameTestHelper helper) {
        helper.assertTrue(
                ModBlocks.mteInPlaceBlocksById().get(
                        MteInPlaceGameTestSupport.id("skyroot/item_barrel"))
                        == null,
                "skyroot/item_barrel dummy is still registered");
        var variant = StorageVariantCatalog.require(
                MteInPlaceGameTestSupport.id("item_barrel_6983"));
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(
                pos,
                ModBlocks.storageBlocksById()
                        .get(variant.id())
                        .get()
                        .defaultBlockState()
                        .setValue(StorageHostBlock.FACING, Direction.NORTH));
        MassStorageBlockEntity be = helper.getBlockEntity(pos);
        helper.assertTrue(
                be.inventory().insertAll(
                        new ItemStack(Items.APPLE), false)
                        .isEmpty(),
                "item_barrel_6983 rejected an item");
        helper.assertTrue(
                be.inventory().stored() > 0,
                "folded skyroot barrel did not store the item");
        helper.succeed();
    }
}
