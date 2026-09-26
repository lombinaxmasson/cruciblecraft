package com.masson.cruciblecraft.gametest;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.AbstractPipeBlock;
import com.masson.cruciblecraft.content.block.ItemPipeBlock;
import com.masson.cruciblecraft.content.blockentity.ItemPipeBlockEntity;
import com.masson.cruciblecraft.logistics.itemnet.ItemLogisticsNetwork;
import com.masson.cruciblecraft.logistics.itemnet.ItemNetworkKinds;
import com.masson.cruciblecraft.logistics.pipe.PipeCatalog;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverBehaviorRegistry;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverDefinition;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverDefinitionCatalog;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCover;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.verification.PlayerCompleteSmoke;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
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
 * Isolated item-network core gate. Run with
 * {@code -PgameTestGrid=logistics}.
 */
@GameTestHolder(ItemNetworkCoreGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class ItemNetworkCoreGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_logistics";
    private static final String TEMPLATE = "empty";

    private ItemNetworkCoreGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void sameIdentityConnectedExportsIntoStorage(
            GameTestHelper helper) {
        Layout layout = placeLine(helper, 1, 1);
        helper.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    helper.assertTrue(
                            layout.source().getItem(0).isEmpty(),
                            "Export did not drain the source chest");
                    helper.assertTrue(
                            layout.storage().getItem(0).getCount() == 16,
                            "Storage did not receive exported items");
                    helper.assertTrue(
                            !ItemLogisticsNetwork.discoverStorage(
                                    helper.getLevel(),
                                    helper.absolutePos(layout.exportPipe()),
                                    1).endpoints().isEmpty(),
                            "Transfer had no visible storage after a successful move");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void disconnectedPipesAreNotOneNetwork(
            GameTestHelper helper) {
        Layout layout = placeLine(helper, 1, 1);
        helper.setBlock(
                layout.midPipe(),
                Blocks.AIR.defaultBlockState());
        helper.assertTrue(
                !ItemLogisticsNetwork.isVisibleStorage(
                        helper.getLevel(),
                        helper.absolutePos(layout.exportPipe()),
                        1,
                        helper.absolutePos(layout.storagePipe()),
                        Direction.EAST),
                "Disconnected pipes still shared a logistics identity");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void differentIdentityIsInvisible(
            GameTestHelper helper) {
        Layout layout = placeLine(helper, 1, 2);
        helper.assertTrue(
                !ItemLogisticsNetwork.isVisibleStorage(
                        helper.getLevel(),
                        helper.absolutePos(layout.exportPipe()),
                        1,
                        helper.absolutePos(layout.storagePipe()),
                        Direction.EAST),
                "Mismatched network ids were visible to each other");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void noTargetDoesNotSwallowItems(
            GameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(1, 2, 1);
        BlockPos pipePos = sourcePos.east();
        helper.setBlock(sourcePos, Blocks.CHEST);
        helper.setBlock(pipePos, pipeState(itemPipe(), Direction.WEST));
        ChestBlockEntity source = helper.getBlockEntity(sourcePos);
        source.setItem(0, new ItemStack(Items.IRON_INGOT, 16));
        ItemPipeBlockEntity pipe = helper.getBlockEntity(pipePos);
        helper.assertTrue(
                pipe.setCover(
                        Direction.WEST,
                        networked(ItemNetworkKinds.EXPORT, 1)),
                "Could not install export cover");
        helper.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    helper.assertTrue(
                            source.getItem(0).getCount() == 16,
                            "No-target export swallowed items");
                    helper.assertTrue(
                            ItemLogisticsNetwork.discoverStorage(
                                    helper.getLevel(),
                                    helper.absolutePos(pipePos),
                                    1).endpoints().isEmpty(),
                            "No-target export still discovered storage");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void removingStorageStopsTransfer(
            GameTestHelper helper) {
        Layout layout = placeLine(helper, 1, 1);
        helper.startSequence()
                .thenIdle(10)
                .thenExecute(() -> {
                    ItemPipeBlockEntity storagePipe =
                            helper.getBlockEntity(layout.storagePipe());
                    helper.assertTrue(
                            storagePipe.removeCover(Direction.EAST, null),
                            "Could not remove storage cover");
                })
                .thenIdle(40)
                .thenExecute(() -> {
                    int remaining = layout.source().getItem(0).getCount()
                            + layout.storage().getItem(0).getCount();
                    helper.assertTrue(
                            remaining == 16,
                            "Target removal swallowed items, remaining "
                                    + remaining);
                    helper.assertTrue(
                            !ItemLogisticsNetwork.isVisibleStorage(
                                    helper.getLevel(),
                                    helper.absolutePos(layout.exportPipe()),
                                    1,
                                    helper.absolutePos(layout.storagePipe()),
                                    Direction.EAST),
                            "Removed storage cover stayed visible");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void coverIdentitySurvivesBlockEntityReload(
            GameTestHelper helper) {
        Layout layout = placeLine(helper, 1, 1);
        ItemPipeBlockEntity export = helper.getBlockEntity(layout.exportPipe());
        var tag = export.saveWithoutMetadata(
                helper.getLevel().registryAccess());
        export.loadWithComponents(
                tag, helper.getLevel().registryAccess());
        PipeCover reloaded = export.coverSnapshot().get(Direction.WEST);
        helper.assertTrue(
                reloaded != null
                        && ItemNetworkKinds.networkId(reloaded) == 1,
                "Export network id did not survive reload");
        helper.startSequence()
                .thenIdle(40)
                .thenExecute(() -> helper.assertTrue(
                        layout.storage().getItem(0).getCount() == 16,
                        "Reloaded export cover did not keep transferring"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void chunkUnloadHidesEndpointThenRejoins(
            GameTestHelper helper) {
        Layout layout = placeLine(helper, 1, 1);
        ItemPipeBlockEntity storage =
                helper.getBlockEntity(layout.storagePipe());
        helper.assertTrue(
                ItemLogisticsNetwork.isVisibleStorage(
                        helper.getLevel(),
                        helper.absolutePos(layout.exportPipe()),
                        1,
                        helper.absolutePos(layout.storagePipe()),
                        Direction.EAST),
                "Storage was not visible before unload");
        storage.onChunkUnloaded();
        helper.assertTrue(
                !ItemLogisticsNetwork.isVisibleStorage(
                        helper.getLevel(),
                        helper.absolutePos(layout.exportPipe()),
                        1,
                        helper.absolutePos(layout.storagePipe()),
                        Direction.EAST),
                "Unloaded storage stayed in the discovery set");
        storage.onLoad();
        helper.assertTrue(
                ItemLogisticsNetwork.isVisibleStorage(
                        helper.getLevel(),
                        helper.absolutePos(layout.exportPipe()),
                        1,
                        helper.absolutePos(layout.storagePipe()),
                        Direction.EAST),
                "Reloaded storage did not rejoin");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void importPullsFromStorage(
            GameTestHelper helper) {
        BlockPos storageChest = new BlockPos(1, 2, 1);
        BlockPos storagePipe = storageChest.east();
        BlockPos destPipe = storagePipe.east();
        BlockPos destChest = destPipe.east();
        helper.setBlock(storageChest, Blocks.CHEST);
        helper.setBlock(
                storagePipe,
                pipeState(itemPipe(), Direction.WEST, Direction.EAST));
        helper.setBlock(
                destPipe,
                pipeState(itemPipe(), Direction.WEST, Direction.EAST));
        helper.setBlock(destChest, Blocks.CHEST);
        ChestBlockEntity storage = helper.getBlockEntity(storageChest);
        storage.setItem(0, new ItemStack(Items.GOLD_INGOT, 8));
        ItemPipeBlockEntity storageBe = helper.getBlockEntity(storagePipe);
        ItemPipeBlockEntity destBe = helper.getBlockEntity(destPipe);
        helper.assertTrue(
                storageBe.setCover(
                        Direction.WEST,
                        networked(ItemNetworkKinds.STORAGE, 1)),
                "Could not install storage cover");
        helper.assertTrue(
                destBe.setCover(
                        Direction.EAST,
                        networked(ItemNetworkKinds.IMPORT, 1)),
                "Could not install import cover");
        helper.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    ChestBlockEntity dest = helper.getBlockEntity(destChest);
                    helper.assertTrue(
                            dest.getItem(0).getCount() == 8,
                            "Import did not pull from storage");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void coversAreSurvivalCraftable(
            GameTestHelper helper) {
        helper.assertTrue(
                craftsTo(helper, ModItems.LOGISTICS_ITEM_STORAGE_COVER.get()),
                "Storage cover recipe missing");
        helper.assertTrue(
                craftsTo(helper, ModItems.LOGISTICS_ITEM_IMPORT_COVER.get()),
                "Import cover recipe missing");
        helper.assertTrue(
                craftsTo(helper, ModItems.LOGISTICS_ITEM_EXPORT_COVER.get()),
                "Export cover recipe missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void playerSurfaceIsRegistered(
            GameTestHelper helper) {
        helper.assertTrue(
                CoverDefinitionCatalog.find(ItemNetworkKinds.STORAGE)
                        .isPresent(),
                "Item storage definition missing");
        helper.assertTrue(
                CoverDefinitionCatalog.find(ItemNetworkKinds.IMPORT)
                        .isPresent()
                        && CoverDefinitionCatalog.find(ItemNetworkKinds.EXPORT)
                                .isPresent(),
                "Item transfer definitions missing");
        helper.assertTrue(
                ModItems.LOGISTICS_ITEM_STORAGE_COVER.get() != null
                        && ModItems.LOGISTICS_ITEM_IMPORT_COVER.get() != null
                        && ModItems.LOGISTICS_ITEM_EXPORT_COVER.get() != null,
                "Item network cover items missing");
        PlayerCompleteSmoke.writeIfConfigured(
                "gameTestServer",
                "logistics/item-network-core");
        helper.assertTrue(
                PlayerCompleteSmoke.snapshot(
                                "gameTestServer",
                                "logistics/item-network-core")
                        .get("status").getAsString().equals("PASS"),
                "Player-complete registry snapshot failed");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void definitionsSurviveDatapackReload(
            GameTestHelper helper) {
        CoverBehaviorRegistry.validateDefinitions();
        ResourceLocation storage = ItemNetworkKinds.STORAGE;
        helper.assertTrue(
                CoverDefinitionCatalog.find(storage).isPresent(),
                "Storage definition missing before reload");
        MinecraftServer server = helper.getLevel().getServer();
        AtomicBoolean reloaded = new AtomicBoolean(false);
        server.reloadResources(server.getPackRepository().getSelectedIds())
                .thenRun(() -> reloaded.set(true));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        reloaded.get(),
                        "Datapack reload did not finish"))
                .thenExecute(() -> {
                    CoverBehaviorRegistry.validateDefinitions();
                    helper.assertTrue(
                            CoverDefinitionCatalog.find(storage)
                                    .orElseThrow()
                                    .behaviorId()
                                    .equals(ItemNetworkKinds.STORAGE_BEHAVIOR),
                            "Storage definition lost its behavior after reload");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void loadAxisCapsAreRecorded(
            GameTestHelper helper) {
        Layout layout = placeLine(helper, 1, 1);
        ItemLogisticsNetwork.Discovery found =
                ItemLogisticsNetwork.discoverStorage(
                        helper.getLevel(),
                        helper.absolutePos(layout.exportPipe()),
                        1);
        helper.assertTrue(
                found.visits() > 0
                        && found.visits()
                                <= com.masson.cruciblecraft.logistics.itemnet
                                        .ItemNetworkLimits.MAX_VISITED_PIPES
                        && !found.endpoints().isEmpty(),
                "Discovery visits were not bounded");
        helper.succeed();
    }

    private static boolean craftsTo(
            GameTestHelper helper,
            net.minecraft.world.item.Item item) {
        List<ItemStack> slots;
        if (item == ModItems.LOGISTICS_ITEM_STORAGE_COVER.get()) {
            slots = List.of(
                    ItemStack.EMPTY,
                    new ItemStack(Items.IRON_INGOT),
                    ItemStack.EMPTY,
                    new ItemStack(Items.CHEST),
                    new ItemStack(Items.HOPPER),
                    new ItemStack(Items.CHEST));
        } else if (item == ModItems.LOGISTICS_ITEM_IMPORT_COVER.get()) {
            slots = List.of(
                    ItemStack.EMPTY,
                    new ItemStack(Items.IRON_INGOT),
                    ItemStack.EMPTY,
                    new ItemStack(Items.COMPARATOR),
                    new ItemStack(Items.HOPPER),
                    new ItemStack(Items.COMPARATOR));
        } else {
            slots = List.of(
                    ItemStack.EMPTY,
                    new ItemStack(Items.IRON_INGOT),
                    ItemStack.EMPTY,
                    new ItemStack(Items.DROPPER),
                    new ItemStack(Items.HOPPER),
                    new ItemStack(Items.DROPPER));
        }
        ItemStack assembled = helper.getLevel()
                .getRecipeManager()
                .getRecipeFor(
                        RecipeType.CRAFTING,
                        CraftingInput.of(3, 2, slots),
                        helper.getLevel())
                .map(holder -> holder.value().assemble(
                        CraftingInput.of(3, 2, slots),
                        helper.getLevel().registryAccess()))
                .orElse(ItemStack.EMPTY);
        return assembled.is(item) && assembled.getCount() == 1;
    }

    private static Layout placeLine(
            GameTestHelper helper, int exportNet, int storageNet) {
        BlockPos sourcePos = new BlockPos(1, 2, 1);
        BlockPos exportPipe = sourcePos.east();
        BlockPos midPipe = exportPipe.east();
        BlockPos storagePipe = midPipe.east();
        BlockPos storageChest = storagePipe.east();
        helper.setBlock(sourcePos, Blocks.CHEST);
        helper.setBlock(
                exportPipe,
                pipeState(itemPipe(), Direction.WEST, Direction.EAST));
        helper.setBlock(
                midPipe,
                pipeState(itemPipe(), Direction.WEST, Direction.EAST));
        helper.setBlock(
                storagePipe,
                pipeState(itemPipe(), Direction.WEST, Direction.EAST));
        helper.setBlock(storageChest, Blocks.CHEST);
        ChestBlockEntity source = helper.getBlockEntity(sourcePos);
        ChestBlockEntity storage = helper.getBlockEntity(storageChest);
        source.setItem(0, new ItemStack(Items.IRON_INGOT, 16));
        ItemPipeBlockEntity exportBe = helper.getBlockEntity(exportPipe);
        ItemPipeBlockEntity storageBe = helper.getBlockEntity(storagePipe);
        helper.assertTrue(
                exportBe.setCover(
                        Direction.WEST,
                        networked(ItemNetworkKinds.EXPORT, exportNet)),
                "Could not install export cover");
        helper.assertTrue(
                storageBe.setCover(
                        Direction.EAST,
                        networked(ItemNetworkKinds.STORAGE, storageNet)),
                "Could not install storage cover");
        return new Layout(
                sourcePos,
                exportPipe,
                midPipe,
                storagePipe,
                storageChest,
                source,
                storage);
    }

    private static PipeCover networked(ResourceLocation definition, int id) {
        return PipeCover.of(definition).configure(
                CoverDefinition.ConfigField.NETWORK_ID, id);
    }

    private static ItemPipeBlock itemPipe() {
        return (ItemPipeBlock) ModBlocks.pipeBlock(
                "brass",
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
            BlockPos sourceChest,
            BlockPos exportPipe,
            BlockPos midPipe,
            BlockPos storagePipe,
            BlockPos storageChest,
            ChestBlockEntity source,
            ChestBlockEntity storage) {}
}
