package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.AbstractPipeBlock;
import com.masson.cruciblecraft.content.block.FluidPipeBlock;
import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.content.item.CatalogNamedBlockItem;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.logistics.pipe.PipeCatalog;
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
 * Isolated extender runtime. Run with
 * {@code -PgameTestGrid=content}.
 */
@GameTestHolder(MteExtenderRuntimeGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class MteExtenderRuntimeGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_content";
    private static final String TEMPLATE = "empty";

    private MteExtenderRuntimeGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void tankBridgeIsLiveExtender(GameTestHelper helper) {
        Item item = item("extender/tank_bridge");
        helper.assertTrue(
                item instanceof CatalogNamedBlockItem,
                "tank bridge is still a dummy item");
        helper.assertTrue(
                ((MteInPlaceBlock) ((CatalogNamedBlockItem) item).getBlock())
                        .spec()
                        .kind()
                        == MteInPlaceKind.TANK_BRIDGE,
                "tank bridge kind drifted");
        helper.assertTrue(
                ModItems.smelterMteItemsById().get(id("extender/tank_bridge"))
                        == null,
                "tank bridge dummy CatalogNamedItem is still registered");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void tankExtenderDelegatesFluid(GameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(1, 2, 2);
        BlockPos extenderPos = new BlockPos(2, 2, 2);
        BlockPos destPos = new BlockPos(3, 2, 2);
        FluidPipeBlockEntity source = placePipe(helper, sourcePos);
        FluidPipeBlockEntity dest = placePipe(helper, destPos);
        MteInPlaceBlock extender = ModBlocks.mteInPlaceBlocksById()
                .get(id("extender/tank_extender"))
                .get();
        helper.setBlock(
                extenderPos,
                extender.defaultBlockState().setValue(
                        MteInPlaceBlock.FACING, Direction.EAST));
        helper.assertTrue(
                source.fillInternal(
                        new FluidStack(Fluids.WATER, 200),
                        IFluidHandler.FluidAction.EXECUTE)
                        == 200,
                "source pipe rejected water");
        helper.startSequence()
                .thenExecuteAfter(2, () -> helper.assertTrue(
                        dest.storedFluid().getAmount() > 0,
                        "extender did not push fluid to the output tank"))
                .thenSucceed();
    }

    private static FluidPipeBlockEntity placePipe(
            GameTestHelper helper, BlockPos pos) {
        FluidPipeBlock block = (FluidPipeBlock) ModBlocks.pipeBlock(
                "copper",
                MaterialPrefixes.FLUID_PIPE,
                PipeCatalog.Kind.FLUID).get();
        BlockState state = block.defaultBlockState().setValue(
                AbstractPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.EAST),
                true)
                .setValue(
                        AbstractPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.WEST),
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
