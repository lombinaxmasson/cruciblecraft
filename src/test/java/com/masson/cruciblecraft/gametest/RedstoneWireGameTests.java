package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.content.block.CableBlock;
import com.masson.cruciblecraft.content.block.RedstoneWireBlock;
import com.masson.cruciblecraft.content.blockentity.RedstoneWireBlockEntity;
import com.masson.cruciblecraft.content.item.MaterialFormItem;
import com.masson.cruciblecraft.content.item.RedstoneWireBlockItem;
import com.masson.cruciblecraft.content.redstonewire.RedstoneWireKind;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated GT6 redstone-wire gate. Run with
 * {@code -PwaveRecipes=content/mte-redstone-wire}.
 */
@GameTestHolder(RedstoneWireGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class RedstoneWireGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_content_mte_redstone_wire";
    private static final String TEMPLATE = "empty";
    private static final BlockPos POS = new BlockPos(2, 2, 2);

    private RedstoneWireGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void threeIdentitiesAreRegistered(GameTestHelper helper) {
        helper.assertTrue(
                ModBlocks.redstoneWireBlocksById().size()
                        == RedstoneWireKind.EXPECTED_SIZE,
                "Redstone wire block catalog drifted from 3");
        helper.assertTrue(
                ModBlockEntities.REDSTONE_WIRE.get() != null,
                "Redstone wire block entity type missing");
        for (RedstoneWireKind kind : RedstoneWireKind.all()) {
            Item item = BuiltInRegistries.ITEM.get(kind.id());
            helper.assertTrue(
                    item instanceof RedstoneWireBlockItem blockItem
                            && blockItem.kind() == kind
                            && item instanceof MaterialFormItem form
                            && kind.materialId().equals(form.materialId())
                            && kind.form().equals(form.form()),
                    "wireGt01 was not upgraded in-place: " + kind.path());
            helper.assertTrue(
                    ModItems.materialItem(kind.materialId(), kind.form()).get()
                            == item,
                    "Material wire form is not the redstone BlockItem: "
                            + kind.path());
            helper.assertTrue(
                    ModBlocks.redstoneWireBlocksById().get(kind.id()).get().kind()
                            == kind,
                    "Block kind drifted: " + kind.path());
            helper.assertTrue(
                    BuiltInRegistries.ITEM.containsKey(kind.id()),
                    "Live wire id missing: " + kind.path());
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void foldedOntoWireGt01NotVanilla(GameTestHelper helper) {
        for (RedstoneWireKind kind : RedstoneWireKind.all()) {
            Item item = BuiltInRegistries.ITEM.get(kind.id());
            Block block = ModBlocks.redstoneWireBlocksById().get(kind.id()).get();
            helper.assertTrue(
                    item instanceof RedstoneWireBlockItem
                            && item != Items.REDSTONE
                            && !(block instanceof CableBlock)
                            && !(block instanceof RedStoneWireBlock),
                    "27000-family must be the live wireGt01 redstone MTE, not "
                            + "vanilla dust or EU cable: "
                            + kind.path());
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void weakAndStrongRedstone(GameTestHelper helper) {
        BlockPos source = new BlockPos(3, 2, 2);
        helper.setBlock(source, Blocks.REDSTONE_BLOCK);
        helper.setBlock(
                POS,
                connected(
                        RedstoneWireKind.RED_ALLOY,
                        Direction.EAST,
                        Direction.WEST));
        helper.startSequence()
                .thenExecuteAfter(4, () -> {
                    BlockPos world = helper.absolutePos(POS);
                    BlockState state = helper.getLevel().getBlockState(world);
                    int weak = state.getSignal(
                            helper.getLevel(), world, Direction.EAST);
                    int strong = state.getDirectSignal(
                            helper.getLevel(), world, Direction.EAST);
                    helper.assertTrue(
                            helper.getBlockEntity(POS)
                                    instanceof RedstoneWireBlockEntity,
                            "Redstone wire BE missing");
                    helper.assertTrue(
                            weak > 0 && weak == strong,
                            "GT6 wire must emit weak and strong: weak="
                                    + weak
                                    + " strong="
                                    + strong);
                    helper.assertTrue(
                            state.getValue(RedstoneWireBlock.POWER) > 0,
                            "Visual power stayed 0");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void lumiumEmitsLight(GameTestHelper helper) {
        BlockPos source = new BlockPos(3, 2, 2);
        helper.setBlock(source, Blocks.REDSTONE_BLOCK);
        helper.setBlock(
                POS,
                connected(
                        RedstoneWireKind.LUMIUM_WIRELAMP,
                        Direction.EAST));
        helper.startSequence()
                .thenExecuteAfter(4, () -> {
                    BlockState state = helper.getBlockState(POS);
                    int light = state.getLightEmission(
                            helper.getLevel(), helper.absolutePos(POS));
                    helper.assertTrue(
                            light > 0,
                            "Lumium wirelamp light was " + light);
                    helper.assertTrue(
                            light == state.getValue(RedstoneWireBlock.POWER),
                            "Lumium light drifted from visual power");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void noSurvivalRecipes(GameTestHelper helper) {
        for (RedstoneWireKind kind : RedstoneWireKind.all()) {
            Item result = BuiltInRegistries.ITEM.get(kind.id());
            boolean hasRecipe = helper.getLevel()
                    .getRecipeManager()
                    .getAllRecipesFor(RecipeType.CRAFTING)
                    .stream()
                    .anyMatch(holder -> holder.value()
                            .getResultItem(helper.getLevel().registryAccess())
                            .is(result));
            helper.assertFalse(
                    hasRecipe,
                    "Blocked D0 still has a recipe: " + kind.path());
        }
        helper.succeed();
    }

    private static BlockState connected(
            RedstoneWireKind kind, Direction... sides) {
        BlockState state = ModBlocks.redstoneWireBlocksById()
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
