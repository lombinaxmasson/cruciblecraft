package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.AbstractPipeBlock;
import com.masson.cruciblecraft.content.block.FluidPipeBlock;
import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.item.CatalogNamedBlockItem;
import com.masson.cruciblecraft.content.item.CatalogNamedItem;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.logistics.pipe.PipeCatalog;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCoverItems;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated fluid-attachment runtime. Run with
 * {@code -PwaveRecipes=content/gt6-mte-fluid-attachments-runtime}.
 */
@GameTestHolder(MteFluidAttachmentsRuntimeGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class MteFluidAttachmentsRuntimeGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_content_gt6_mte_fluid_attachments_runtime";
    private static final String TEMPLATE = "empty";

    private MteFluidAttachmentsRuntimeGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void stoneFaucetIsLiveBlockNotCover(GameTestHelper helper) {
        Item item = item("fluid_attachment/crucible_faucet_stone");
        helper.assertTrue(
                item instanceof CatalogNamedBlockItem
                        && !(item instanceof CatalogNamedItem),
                "stone faucet is still a dummy CatalogNamedItem");
        helper.assertTrue(
                ModBlocks.mteInPlaceBlocksById().get(id(
                        "fluid_attachment/crucible_faucet_stone"))
                        .get() instanceof MteInPlaceBlock,
                "stone faucet lost its in-place block");
        helper.assertTrue(
                ((MteInPlaceBlock) ((CatalogNamedBlockItem) item).getBlock())
                        .spec()
                        .kind()
                        == MteInPlaceKind.FAUCET,
                "stone faucet kind drifted");
        helper.assertTrue(
                PipeCoverItems.stackFor(null).isEmpty(),
                "cover helper must still treat null as empty");
        helper.assertTrue(
                BuiltInRegistries.ITEM.getKey(item).getPath().equals(
                        "fluid_attachment/crucible_faucet_stone"),
                "stone faucet left its dummy modern id");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void stoneFaucetPoursIntoTankBelow(GameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(2, 3, 2);
        BlockPos faucetPos = new BlockPos(2, 3, 3);
        BlockPos destPos = new BlockPos(2, 2, 3);
        FluidPipeBlockEntity source = placePipe(helper, sourcePos);
        FluidPipeBlockEntity dest = placePipe(helper, destPos);
        MteInPlaceBlock faucetBlock = ModBlocks.mteInPlaceBlocksById()
                .get(id("fluid_attachment/crucible_faucet_stone"))
                .get();
        helper.setBlock(
                faucetPos,
                faucetBlock.defaultBlockState().setValue(
                        MteInPlaceBlock.FACING, Direction.NORTH));
        helper.assertTrue(
                helper.getBlockEntity(faucetPos) instanceof MteInPlaceBlockEntity,
                "stone faucet did not place a block entity");
        helper.assertTrue(
                source.fillInternal(
                        new FluidStack(Fluids.WATER, 200),
                        IFluidHandler.FluidAction.EXECUTE)
                        == 200,
                "source pipe rejected water");
        MteInPlaceBlockEntity faucet = helper.getBlockEntity(faucetPos);
        faucet.transferOnce();
        helper.assertTrue(
                dest.storedFluid().getAmount() > 0,
                "faucet did not pour into the tank below");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void stainlessTapIsLiveAttachment(GameTestHelper helper) {
        Item item = item("fluid_attachment/stainless_tap");
        helper.assertTrue(
                item instanceof CatalogNamedBlockItem,
                "stainless tap is not a BlockItem");
        helper.assertTrue(
                ((MteInPlaceBlock) ((CatalogNamedBlockItem) item).getBlock())
                        .spec()
                        .kind()
                        == MteInPlaceKind.TAP,
                "stainless tap kind drifted");
        helper.assertTrue(
                ModItems.smelterMteItemsById().get(id(
                        "fluid_attachment/stainless_tap"))
                        == null,
                "stainless tap dummy CatalogNamedItem is still registered");
        helper.succeed();
    }

    private static FluidPipeBlockEntity placePipe(
            GameTestHelper helper, BlockPos pos) {
        FluidPipeBlock block = (FluidPipeBlock) ModBlocks.pipeBlock(
                "copper",
                MaterialPrefixes.FLUID_PIPE,
                PipeCatalog.Kind.FLUID).get();
        BlockState state = block.defaultBlockState().setValue(
                AbstractPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.UP),
                true);
        helper.setBlock(pos, state);
        return helper.getBlockEntity(pos);
    }

    private static Item item(String path) {
        return BuiltInRegistries.ITEM.get(id(path));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
