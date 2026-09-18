package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.content.block.StorageHostBlock;
import com.masson.cruciblecraft.content.blockentity.BookshelfBlockEntity;
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

@GameTestHolder(MteFurnitureStorageRuntimeGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class MteFurnitureStorageRuntimeGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_content_gt6_mte_furniture_storage_runtime";
    private static final String TEMPLATE = "empty";

    private MteFurnitureStorageRuntimeGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void leadBookshelfIsLiveInventory(GameTestHelper helper) {
        helper.assertTrue(
                ModBlocks.mteInPlaceBlocksById().get(
                        MteInPlaceGameTestSupport.id("furniture/bookshelf_lead"))
                        == null,
                "furniture/bookshelf_lead dummy is still registered");
        var variant = StorageVariantCatalog.require(
                MteInPlaceGameTestSupport.id("bookshelf_7100"));
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(
                pos,
                ModBlocks.storageBlocksById()
                        .get(variant.id())
                        .get()
                        .defaultBlockState()
                        .setValue(StorageHostBlock.FACING, Direction.NORTH));
        BookshelfBlockEntity be = helper.getBlockEntity(pos);
        helper.assertTrue(
                be.inventory().insertItem(
                        0, new ItemStack(Items.BOOK), false)
                        .isEmpty(),
                "bookshelf_7100 rejected a book");
        helper.succeed();
    }
}
