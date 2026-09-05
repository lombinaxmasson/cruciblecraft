package com.masson.cruciblecraft.gametest;

import java.util.List;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.AbstractPipeBlock;
import com.masson.cruciblecraft.content.block.ItemPipeBlock;
import com.masson.cruciblecraft.content.block.LogisticsCoreBlock;
import com.masson.cruciblecraft.content.blockentity.ItemPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.LogisticsCoreBlockEntity;
import com.masson.cruciblecraft.logistics.core.LogisticsCoreGeometry;
import com.masson.cruciblecraft.logistics.core.LogisticsDumpKinds;
import com.masson.cruciblecraft.logistics.genericnet.GenericNetworkKinds;
import com.masson.cruciblecraft.logistics.pipe.PipeCatalog;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverDefinition;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverDefinitionCatalog;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCover;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModCapabilities;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.verification.PlayerCompleteSmoke;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated Logistics Core gate. Run with
 * {@code -PwaveRecipes=runtime/logistics-core}.
 */
@GameTestHolder(LogisticsCoreGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class LogisticsCoreGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_runtime_logistics_core";
    private static final String TEMPLATE = "empty";
    private static final int MOVED = 16;

    private LogisticsCoreGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void formedCoreDumpsLeftoverItems(GameTestHelper helper) {
        Layout layout = placeDumpLine(helper, true);
        helper.startSequence()
                .thenIdle(1)
                .thenExecuteFor(40, () -> inject(helper, layout.controller()))
                .thenExecute(() -> {
                    helper.assertTrue(
                            core(helper, layout.controller()).formed(),
                            "Cheapest valid cube did not form");
                    helper.assertTrue(
                            layout.storage().getItem(0).isEmpty(),
                            "Dump did not drain leftover storage");
                    helper.assertTrue(
                            layout.dump().getItem(0).getCount() == MOVED,
                            "Dump chest did not receive leftover items");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void missingDumpChestDoesNotVoid(GameTestHelper helper) {
        Layout layout = placeDumpLine(helper, false);
        helper.startSequence()
                .thenIdle(1)
                .thenExecuteFor(40, () -> inject(helper, layout.controller()))
                .thenExecute(() -> {
                    helper.assertTrue(
                            layout.storage().getItem(0).getCount() == MOVED,
                            "Dump voided leftover items with no destination");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void missingCpuTypeDoesNotForm(GameTestHelper helper) {
        BlockPos controller = placeCheapestCube(helper);
        helper.setBlock(
                centerOf(controller),
                ModBlocks.GALVANIZED_STEEL_WALL.get().defaultBlockState());
        helper.startSequence()
                .thenIdle(1)
                .thenExecute(() -> helper.assertTrue(
                        !core(helper, controller).formed(),
                        "Core formed without every CPU type"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void dumpCoverIsSurvivalCraftable(GameTestHelper helper) {
        helper.assertTrue(
                CoverDefinitionCatalog.find(LogisticsDumpKinds.DUMP).isPresent(),
                "Dump cover definition missing");
        helper.assertTrue(
                ModItems.LOGISTICS_GENERIC_DUMP_COVER.get() != null,
                "Dump cover item missing");
        List<ItemStack> slots = List.of(
                ItemStack.EMPTY,
                new ItemStack(Items.HOPPER),
                ItemStack.EMPTY,
                new ItemStack(Items.HOPPER),
                new ItemStack(Items.CHEST),
                new ItemStack(Items.HOPPER),
                ItemStack.EMPTY,
                new ItemStack(Items.DROPPER),
                ItemStack.EMPTY);
        ItemStack assembled = helper.getLevel()
                .getRecipeManager()
                .getRecipeFor(
                        RecipeType.CRAFTING,
                        CraftingInput.of(3, 3, slots),
                        helper.getLevel())
                .map(holder -> holder.value().assemble(
                        CraftingInput.of(3, 3, slots),
                        helper.getLevel().registryAccess()))
                .orElse(ItemStack.EMPTY);
        helper.assertTrue(
                assembled.is(ModItems.LOGISTICS_GENERIC_DUMP_COVER.get())
                        && assembled.getCount() == 1,
                "Dump cover recipe missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void playerSurfaceIsRegistered(GameTestHelper helper) {
        helper.assertTrue(
                CoverDefinitionCatalog.find(LogisticsDumpKinds.DUMP)
                        .isPresent(),
                "Dump cover definition missing");
        helper.assertTrue(
                ModItems.LOGISTICS_GENERIC_DUMP_COVER.get() != null,
                "Dump cover item missing");
        PlayerCompleteSmoke.writeIfConfigured(
                "gameTestServer",
                "logistics/logistics-core");
        helper.assertTrue(
                PlayerCompleteSmoke.snapshot(
                                "gameTestServer",
                                "logistics/logistics-core")
                        .get("status")
                        .getAsString()
                        .equals("PASS"),
                "Player-complete registry snapshot failed");
        helper.succeed();
    }

    private static void inject(GameTestHelper helper, BlockPos controller) {
        IEnergyHandler energy = helper.getLevel().getCapability(
                ModCapabilities.ENERGY,
                helper.absolutePos(controller),
                Direction.UP);
        helper.assertTrue(energy != null, "Core energy capability missing");
        energy.insert(
                EnergyType.ELECTRIC,
                LogisticsCoreGeometry.ENERGY_INPUT_MIN,
                1L,
                Direction.UP,
                false);
    }

    private static Layout placeDumpLine(
            GameTestHelper helper, boolean dumpChest) {
        BlockPos controller = placeCheapestCube(helper);
        BlockPos pipe = controller.relative(Direction.SOUTH);
        BlockPos storageChest = pipe.relative(Direction.WEST);
        BlockPos dumpPos = pipe.relative(Direction.EAST);
        helper.setBlock(
                pipe,
                pipeState(itemPipe(), Direction.WEST, Direction.EAST));
        helper.setBlock(storageChest, Blocks.CHEST);
        if (dumpChest) {
            helper.setBlock(dumpPos, Blocks.CHEST);
        }
        ChestBlockEntity storage = helper.getBlockEntity(storageChest);
        storage.setItem(0, new ItemStack(Items.IRON_INGOT, MOVED));
        ItemPipeBlockEntity pipeBe = helper.getBlockEntity(pipe);
        helper.assertTrue(
                pipeBe.setCover(
                        Direction.WEST,
                        networked(GenericNetworkKinds.STORAGE, 1)),
                "Could not install generic storage cover");
        helper.assertTrue(
                pipeBe.setCover(
                        Direction.EAST,
                        networked(LogisticsDumpKinds.DUMP, 1)),
                "Could not install dump cover");
        ChestBlockEntity dump = dumpChest ? helper.getBlockEntity(dumpPos) : null;
        return new Layout(controller, storage, dump);
    }

    private static BlockPos placeCheapestCube(GameTestHelper helper) {
        BlockPos origin = new BlockPos(1, 1, 1);
        BlockPos center = origin.offset(
                LogisticsCoreGeometry.HALF,
                LogisticsCoreGeometry.HALF,
                LogisticsCoreGeometry.HALF);
        BlockPos controller = center.relative(
                Direction.SOUTH, LogisticsCoreGeometry.HALF);
        for (int i = -LogisticsCoreGeometry.HALF;
                i <= LogisticsCoreGeometry.HALF;
                i++) {
            for (int j = -LogisticsCoreGeometry.HALF;
                    j <= LogisticsCoreGeometry.HALF;
                    j++) {
                for (int k = -LogisticsCoreGeometry.HALF;
                        k <= LogisticsCoreGeometry.HALF;
                        k++) {
                    BlockPos pos = center.offset(i, j, k);
                    LogisticsCoreGeometry.CellKind kind =
                            LogisticsCoreGeometry.cell(i, j, k);
                    if (pos.equals(controller)) {
                        helper.setBlock(
                                pos,
                                ModBlocks.LOGISTICS_CORE.get()
                                        .defaultBlockState()
                                        .setValue(
                                                LogisticsCoreBlock.FACING,
                                                Direction.SOUTH));
                        continue;
                    }
                    switch (kind) {
                        case INNER -> helper.setBlock(
                                pos,
                                i == 0 && j == 0 && k == 0
                                        ? ModBlocks.VERSATILE_PROCESSOR_UNIT
                                                .get()
                                                .defaultBlockState()
                                        : ModBlocks.GALVANIZED_STEEL_WALL
                                                .get()
                                                .defaultBlockState());
                        case WALL -> helper.setBlock(
                                pos,
                                ModBlocks.GALVANIZED_STEEL_WALL
                                        .get()
                                        .defaultBlockState());
                        case VENT -> helper.setBlock(
                                pos,
                                ModBlocks.VENTILATION_UNIT
                                        .get()
                                        .defaultBlockState());
                    }
                }
            }
        }
        return controller;
    }

    private static BlockPos centerOf(BlockPos controller) {
        return controller.relative(
                Direction.SOUTH.getOpposite(), LogisticsCoreGeometry.HALF);
    }

    private static LogisticsCoreBlockEntity core(
            GameTestHelper helper, BlockPos controller) {
        return helper.getBlockEntity(controller);
    }

    private static PipeCover networked(
            net.minecraft.resources.ResourceLocation definition, int id) {
        return PipeCover.of(definition).configure(
                CoverDefinition.ConfigField.NETWORK_ID, id);
    }

    private static ItemPipeBlock itemPipe() {
        return (ItemPipeBlock) ModBlocks.pipeBlock(
                "copper",
                MaterialPrefixes.ITEM_PIPE,
                PipeCatalog.Kind.ITEM).get();
    }

    private static BlockState pipeState(
            AbstractPipeBlock block, Direction... connections) {
        BlockState state = block.defaultBlockState();
        for (Direction direction : connections) {
            state = state.setValue(
                    AbstractPipeBlock.PROPERTY_BY_DIRECTION.get(direction),
                    true);
        }
        return state;
    }

    private record Layout(
            BlockPos controller,
            ChestBlockEntity storage,
            ChestBlockEntity dump) {}
}
