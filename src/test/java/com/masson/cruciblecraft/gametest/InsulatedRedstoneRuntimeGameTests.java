package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.CableBlock;
import com.masson.cruciblecraft.content.block.Gt6StyleConnections;
import com.masson.cruciblecraft.content.block.RedstoneWireBlock;
import com.masson.cruciblecraft.content.item.CableBlockItem;
import com.masson.cruciblecraft.content.item.MaterialFormItem;
import com.masson.cruciblecraft.content.item.RedstoneWireBlockItem;
import com.masson.cruciblecraft.content.redstonewire.RedstoneWireKind;
import com.masson.cruciblecraft.energy.cable.ElectricalConductorCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated GT6 insulated redstone runtime. Run with
 * {@code -PwaveRecipes=content/gt6-insulated-redstone-runtime}.
 */
@GameTestHolder(InsulatedRedstoneRuntimeGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class InsulatedRedstoneRuntimeGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_content_gt6_insulated_redstone_runtime";
    private static final String TEMPLATE = "empty";
    private static final BlockPos WIRE = new BlockPos(2, 2, 2);
    private static final BlockPos CABLE = new BlockPos(1, 2, 2);
    private static final BlockPos SOURCE = new BlockPos(3, 2, 2);

    private InsulatedRedstoneRuntimeGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void insulatedCablesAreLiveRedstoneBlocks(
            GameTestHelper helper) {
        helper.assertTrue(
                ModBlocks.redstoneWireBlocksById().size()
                        == RedstoneWireKind.EXPECTED_SIZE,
                "Bare redstone wire map drifted from 3");
        helper.assertTrue(
                ModBlocks.redstoneWireCatalogById().size()
                        == RedstoneWireKind.EXPECTED_CATALOG,
                "Redstone catalog drifted from 6");
        for (RedstoneWireKind kind : RedstoneWireKind.insulatedKinds()) {
            Item item = BuiltInRegistries.ITEM.get(kind.id());
            helper.assertTrue(
                    item instanceof RedstoneWireBlockItem blockItem
                            && blockItem.kind() == kind
                            && item instanceof MaterialFormItem form
                            && kind.materialId().equals(form.materialId())
                            && kind.form().equals(form.form())
                            && kind.form().equals(MaterialPrefixes.CABLE)
                            && kind.insulated()
                            && kind.widthPixels() == 4,
                    "insulated cableGt01 was not upgraded in-place: "
                            + kind.path());
            helper.assertTrue(
                    ModItems.materialItem(kind.materialId(), kind.form()).get()
                            == item,
                    "Material cable form is not the redstone BlockItem: "
                            + kind.path());
            helper.assertTrue(
                    ModBlocks.redstoneWireCatalogById().get(kind.id()).get()
                                    .kind()
                            == kind,
                    "Catalog kind drifted: " + kind.path());
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void insulatedCablesAreNotEuOrTinAlias(
            GameTestHelper helper) {
        Item tinCable = BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath(
                        "cruciblecraft", "tin/cable"));
        for (RedstoneWireKind kind : RedstoneWireKind.insulatedKinds()) {
            Item item = BuiltInRegistries.ITEM.get(kind.id());
            Block block = ModBlocks.redstoneWireCatalogById()
                    .get(kind.id())
                    .get();
            helper.assertTrue(
                    item instanceof RedstoneWireBlockItem
                            && !(item instanceof CableBlockItem)
                            && item != tinCable
                            && block instanceof RedstoneWireBlock
                            && !(block instanceof CableBlock)
                            && !ElectricalConductorCatalog.contains(
                                    kind.materialId(), kind.form()),
                    "insulated redstone must stay off the EU catalog and tin/cable: "
                            + kind.path());
        }
        helper.assertTrue(
                !ElectricalConductorCatalog.contains(
                        "red_alloy", MaterialPrefixes.CABLE)
                        && !ElectricalConductorCatalog.contains(
                                "signalum", MaterialPrefixes.CABLE)
                        && !ElectricalConductorCatalog.contains(
                                "lumium", MaterialPrefixes.CABLE),
                "redstone cables leaked into ElectricalConductorCatalog");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void insulatedJoinsBareRedstoneNetwork(
            GameTestHelper helper) {
        helper.setBlock(SOURCE, Blocks.REDSTONE_BLOCK);
        helper.setBlock(
                WIRE,
                connected(
                        RedstoneWireKind.RED_ALLOY,
                        Direction.EAST,
                        Direction.WEST));
        helper.setBlock(
                CABLE,
                connected(
                        RedstoneWireKind.RED_ALLOY_CABLE,
                        Direction.EAST,
                        Direction.WEST));
        helper.assertTrue(
                Gt6StyleConnections.sameNetwork(
                        helper.getBlockState(WIRE),
                        helper.getBlockState(CABLE)),
                "bare wire and insulated cable must share the redstone network");
        helper.startSequence()
                .thenExecuteAfter(4, () -> {
                    helper.assertTrue(
                            helper.getBlockState(WIRE)
                                            .getValue(RedstoneWireBlock.POWER)
                                    > 0
                                    && helper.getBlockState(CABLE)
                                                    .getValue(
                                                            RedstoneWireBlock
                                                                    .POWER)
                                            > 0,
                            "insulated cable did not carry bare-wire redstone");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void lumiumCableDoesNotGlow(GameTestHelper helper) {
        helper.setBlock(SOURCE, Blocks.REDSTONE_BLOCK);
        helper.setBlock(
                WIRE,
                connected(
                        RedstoneWireKind.LUMIUM_CABLE,
                        Direction.EAST));
        helper.startSequence()
                .thenExecuteAfter(4, () -> {
                    BlockState state = helper.getBlockState(WIRE);
                    int light = state.getLightEmission(
                            helper.getLevel(), helper.absolutePos(WIRE));
                    helper.assertTrue(
                            light == 0
                                    && !RedstoneWireKind.LUMIUM_CABLE.glowing()
                                    && RedstoneWireKind.LUMIUM_WIRELAMP
                                            .glowing(),
                            "Lumium insulated cable must not glow; light="
                                    + light);
                })
                .thenSucceed();
    }

    private static BlockState connected(
            RedstoneWireKind kind, Direction... sides) {
        BlockState state = ModBlocks.redstoneWireCatalogById()
                .get(kind.id())
                .get()
                .defaultBlockState();
        for (Direction side : sides) {
            state = state.setValue(
                    RedstoneWireBlock.PROPERTY_BY_DIRECTION.get(side), true);
        }
        return state;
    }
}
