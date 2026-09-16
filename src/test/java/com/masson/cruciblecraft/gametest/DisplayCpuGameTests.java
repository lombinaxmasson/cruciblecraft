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
import com.masson.cruciblecraft.logistics.displaycpu.DisplayCpuKinds;
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
 * Isolated Display CPU gate. Run with
 * {@code -PwaveRecipes=runtime/display-cpu}.
 */
@GameTestHolder(DisplayCpuGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class DisplayCpuGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_runtime_display_cpu";
    private static final String TEMPLATE = "empty";
    private static final int MOVED = 16;

    private DisplayCpuGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void displayCoverIsSurvivalCraftable(GameTestHelper helper) {
        helper.assertTrue(
                CoverDefinitionCatalog.find(DisplayCpuKinds.LOGIC).isPresent(),
                "Display CPU logic definition missing");
        helper.assertTrue(
                ModItems.LOGISTICS_DISPLAY_CPU_LOGIC_COVER.get() != null,
                "Display CPU logic item missing");
        List<ItemStack> slots = List.of(
                new ItemStack(Items.REDSTONE_TORCH),
                new ItemStack(Items.REDSTONE),
                ItemStack.EMPTY,
                ItemStack.EMPTY,
                new ItemStack(Items.IRON_TRAPDOOR),
                ItemStack.EMPTY,
                ItemStack.EMPTY,
                new ItemStack(ModItems.PROGRAMMED_CIRCUIT.get()),
                ItemStack.EMPTY);
        ItemStack assembled = craft(helper, 3, 3, slots);
        helper.assertTrue(
                assembled.is(ModItems.LOGISTICS_DISPLAY_CPU_LOGIC_COVER.get())
                        && assembled.getCount() == 1,
                "Display CPU logic recipe missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void shapelessDisplayCycle(GameTestHelper helper) {
        ItemStack assembled = craft(
                helper,
                1,
                1,
                List.of(new ItemStack(
                        ModItems.LOGISTICS_DISPLAY_CPU_CONVERSION_COVER.get())));
        helper.assertTrue(
                assembled.is(ModItems.LOGISTICS_DISPLAY_CPU_LOGIC_COVER.get())
                        && assembled.getCount() == 1,
                "Display CPU shapeless cycle missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void playerSurfaceIsRegistered(GameTestHelper helper) {
        helper.assertTrue(
                CoverDefinitionCatalog.find(DisplayCpuKinds.LOGIC)
                        .isPresent(),
                "Display CPU logic definition missing");
        helper.assertTrue(
                ModItems.LOGISTICS_DISPLAY_CPU_LOGIC_COVER.get() != null,
                "Display CPU logic item missing");
        PlayerCompleteSmoke.writeIfConfigured(
                "gameTestServer",
                "logistics/display-cpu");
        helper.assertTrue(
                PlayerCompleteSmoke.snapshot(
                                "gameTestServer",
                                "logistics/display-cpu")
                        .get("status")
                        .getAsString()
                        .equals("PASS"),
                "Player-complete registry snapshot failed");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void missingCoreStaysZero(GameTestHelper helper) {
        BlockPos pipe = new BlockPos(2, 1, 2);
        helper.setBlock(pipe, pipeState(itemPipe(), Direction.NORTH));
        ItemPipeBlockEntity pipeBe = helper.getBlockEntity(pipe);
        attachDisplays(helper, pipeBe);
        helper.startSequence()
                .thenIdle(41)
                .thenExecute(() -> assertDisplays(
                        helper,
                        pipeBe,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void formedCoreWritesDisplayLoad(GameTestHelper helper) {
        Layout layout = placeDumpLine(helper, true);
        ItemPipeBlockEntity pipeBe = helper.getBlockEntity(layout.pipe());
        attachDisplays(helper, pipeBe);
        helper.startSequence()
                .thenIdle(1)
                .thenWaitUntil(() -> {
                    inject(helper, layout.controller());
                    layout.storage().setItem(
                            0, new ItemStack(Items.IRON_INGOT, MOVED));
                    helper.assertTrue(
                            core(helper, layout.controller()).formed(),
                            "Cheapest valid cube did not form");
                    assertDisplays(helper, pipeBe, 10, 15, 10, 15, 0, 0, 10, 15);
                    helper.assertTrue(
                            helper.getLevel().getSignal(
                                    helper.absolutePos(layout.pipe()),
                                    Direction.SOUTH)
                                    == 15,
                            "Pipe did not emit saturated logic redstone");
                })
                .thenSucceed();
    }

    private static void attachDisplays(
            GameTestHelper helper, ItemPipeBlockEntity pipeBe) {
        helper.assertTrue(
                pipeBe.setCover(
                        Direction.NORTH,
                        PipeCover.of(DisplayCpuKinds.LOGIC)),
                "Could not install logic display");
        helper.assertTrue(
                pipeBe.setCover(
                        Direction.SOUTH,
                        PipeCover.of(DisplayCpuKinds.CONTROL)),
                "Could not install control display");
        helper.assertTrue(
                pipeBe.setCover(
                        Direction.UP,
                        PipeCover.of(DisplayCpuKinds.STORAGE)),
                "Could not install storage display");
        helper.assertTrue(
                pipeBe.setCover(
                        Direction.DOWN,
                        PipeCover.of(DisplayCpuKinds.CONVERSION)),
                "Could not install conversion display");
    }

    private static void assertDisplays(
            GameTestHelper helper,
            ItemPipeBlockEntity pipeBe,
            int logicVisual,
            int logicRedstone,
            int controlVisual,
            int controlRedstone,
            int storageVisual,
            int storageRedstone,
            int conversionVisual,
            int conversionRedstone) {
        assertLoad(helper, pipeBe, Direction.NORTH, logicVisual, logicRedstone);
        assertLoad(
                helper, pipeBe, Direction.SOUTH, controlVisual, controlRedstone);
        assertLoad(
                helper, pipeBe, Direction.UP, storageVisual, storageRedstone);
        assertLoad(
                helper,
                pipeBe,
                Direction.DOWN,
                conversionVisual,
                conversionRedstone);
    }

    private static void assertLoad(
            GameTestHelper helper,
            ItemPipeBlockEntity pipeBe,
            Direction side,
            int visual,
            int redstone) {
        PipeCover cover = pipeBe.coverSnapshot().get(side);
        helper.assertTrue(cover != null, "Missing display on " + side);
        helper.assertTrue(
                cover.config().visual() == visual
                        && cover.config().redstone() == redstone,
                side + " display expected visual=" + visual
                        + " redstone=" + redstone
                        + " but was visual=" + cover.config().visual()
                        + " redstone=" + cover.config().redstone());
    }

    private static ItemStack craft(
            GameTestHelper helper, int width, int height, List<ItemStack> slots) {
        CraftingInput input = CraftingInput.of(width, height, slots);
        return helper.getLevel()
                .getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .map(holder -> holder.value().assemble(
                        input, helper.getLevel().registryAccess()))
                .orElse(ItemStack.EMPTY);
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
        return new Layout(controller, pipe, storage, dump);
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
            BlockPos pipe,
            ChestBlockEntity storage,
            ChestBlockEntity dump) {}
}
