package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.item.CatalogNamedBlockItem;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.registry.ModBlocks;
import net.minecraft.core.BlockPos;

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
        MteInPlaceGameTestSupport.assertLive(
                helper, "furniture/bookshelf_lead", MteInPlaceKind.BOOKSHELF);
        BlockPos pos = new BlockPos(2, 2, 2);
        MteInPlaceBlock block = ModBlocks.mteInPlaceBlocksById()
                .get(MteInPlaceGameTestSupport.id(
                        "furniture/bookshelf_lead"))
                .get();
        helper.setBlock(pos, block.defaultBlockState());
        MteInPlaceBlockEntity be = helper.getBlockEntity(pos);
        helper.assertTrue(
                be.items().insertItem(
                        0, new ItemStack(Items.APPLE), false)
                        .isEmpty(),
                "furniture/bookshelf_lead rejected an item");
        helper.succeed();
    }
}
