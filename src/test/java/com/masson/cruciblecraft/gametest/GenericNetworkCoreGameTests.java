package com.masson.cruciblecraft.gametest;

import java.util.List;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.AbstractPipeBlock;
import com.masson.cruciblecraft.content.block.FluidPipeBlock;
import com.masson.cruciblecraft.content.block.ItemPipeBlock;
import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ItemPipeBlockEntity;
import com.masson.cruciblecraft.logistics.genericnet.GenericLogisticsNetwork;
import com.masson.cruciblecraft.logistics.genericnet.GenericNetworkKinds;
import com.masson.cruciblecraft.logistics.genericnet.GenericNetworkLimits;
import com.masson.cruciblecraft.logistics.pipe.PipeCatalog;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated generic-network core gate. Run with
 * {@code -PwaveRecipes=runtime/generic-network-core}.
 */
@GameTestHolder(GenericNetworkCoreGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class GenericNetworkCoreGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_runtime_generic_network_core";
    private static final String TEMPLATE = "empty";
    private static final int ITEM_MOVED = 8;
    private static final int FLUID_MOVED = 600;

    private GenericNetworkCoreGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void sameIdentityConnectedExportsIntoStorage(
            GameTestHelper helper) {
        ItemLayout layout = placeItemLine(helper, 1, 1);
        helper.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    helper.assertTrue(
                            layout.source().getItem(0).isEmpty(),
                            "Export did not drain the source chest");
                    helper.assertTrue(
                            layout.storage().getItem(0).getCount() == ITEM_MOVED,
                            "Storage did not receive exported items");
                    helper.assertTrue(
                            !GenericLogisticsNetwork.discoverStorage(
                                    helper.getLevel(),
                                    helper.absolutePos(layout.exportPipe()),
                                    1).endpoints().isEmpty(),
                            "Transfer had no visible storage after a successful move");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void sameIdentityConnectedExportsFluidsIntoStorage(
            GameTestHelper helper) {
        FluidLayout layout = placeFluidLine(helper, 1, 1, FLUID_MOVED);
        helper.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    helper.assertTrue(
                            layout.source().storedFluid().isEmpty(),
                            "Export did not drain the source pipe");
                    helper.assertTrue(
                            layout.dest().storedFluid().getAmount() == FLUID_MOVED,
                            "Storage did not receive exported fluid");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void disconnectedPipesAreNotOneNetwork(
            GameTestHelper helper) {
        ItemLayout layout = placeItemLine(helper, 1, 1);
        helper.setBlock(
                layout.midPipe(),
                Blocks.AIR.defaultBlockState());
        helper.assertTrue(
                !GenericLogisticsNetwork.isVisibleStorage(
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
        ItemLayout layout = placeItemLine(helper, 1, 2);
        helper.assertTrue(
                !GenericLogisticsNetwork.isVisibleStorage(
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
        helper.setBlock(
                pipePos,
                pipeState(itemPipe(), Direction.WEST));
        ChestBlockEntity source = helper.getBlockEntity(sourcePos);
        source.setItem(0, new ItemStack(Items.IRON_INGOT, ITEM_MOVED));
        ItemPipeBlockEntity pipe = helper.getBlockEntity(pipePos);
        helper.assertTrue(
                pipe.setCover(
                        Direction.WEST,
                        networked(GenericNetworkKinds.EXPORT, 1)),
                "Could not install export cover");
        helper.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    helper.assertTrue(
                            source.getItem(0).getCount() == ITEM_MOVED,
                            "No-target export swallowed items");
                    helper.assertTrue(
                            GenericLogisticsNetwork.discoverStorage(
                                    helper.getLevel(),
                                    helper.absolutePos(pipePos),
                                    1).endpoints().isEmpty(),
                            "No-target export still discovered storage");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void coverIdentitySurvivesBlockEntityReload(
            GameTestHelper helper) {
        ItemLayout layout = placeItemLine(helper, 1, 1);
        ItemPipeBlockEntity export = helper.getBlockEntity(layout.exportPipe());
        var tag = export.saveWithoutMetadata(
                helper.getLevel().registryAccess());
        export.loadWithComponents(
                tag, helper.getLevel().registryAccess());
        PipeCover reloaded = export.coverSnapshot().get(Direction.WEST);
        helper.assertTrue(
                reloaded != null
                        && GenericNetworkKinds.networkId(reloaded) == 1,
                "Export network id did not survive reload");
        helper.startSequence()
                .thenIdle(40)
                .thenExecute(() -> helper.assertTrue(
                        layout.storage().getItem(0).getCount() == ITEM_MOVED,
                        "Reloaded export cover did not keep transferring"))
                .thenSucceed();
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
        storage.setItem(0, new ItemStack(Items.IRON_INGOT, ITEM_MOVED));
        ItemPipeBlockEntity storageBe = helper.getBlockEntity(storagePipe);
        ItemPipeBlockEntity destBe = helper.getBlockEntity(destPipe);
        helper.assertTrue(
                storageBe.setCover(
                        Direction.WEST,
                        networked(GenericNetworkKinds.STORAGE, 1)),
                "Could not install storage cover");
        helper.assertTrue(
                destBe.setCover(
                        Direction.EAST,
                        networked(GenericNetworkKinds.IMPORT, 1)),
                "Could not install import cover");
        helper.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    ChestBlockEntity dest = helper.getBlockEntity(destChest);
                    helper.assertTrue(
                            dest.getItem(0).getCount() == ITEM_MOVED,
                            "Import did not pull from storage");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void coversAreSurvivalCraftable(
            GameTestHelper helper) {
        helper.assertTrue(
                craftsTo(helper, ModItems.LOGISTICS_GENERIC_STORAGE_COVER.get()),
                "Storage cover recipe missing");
        helper.assertTrue(
                craftsTo(helper, ModItems.LOGISTICS_GENERIC_IMPORT_COVER.get()),
                "Import cover recipe missing");
        helper.assertTrue(
                craftsTo(helper, ModItems.LOGISTICS_GENERIC_EXPORT_COVER.get()),
                "Export cover recipe missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void loadAxisCapsAreRecorded(
            GameTestHelper helper) {
        ItemLayout layout = placeItemLine(helper, 1, 1);
        GenericLogisticsNetwork.Discovery found =
                GenericLogisticsNetwork.discoverStorage(
                        helper.getLevel(),
                        helper.absolutePos(layout.exportPipe()),
                        1);
        helper.assertTrue(
                found.visits() > 0
                        && found.visits()
                                <= GenericNetworkLimits.MAX_VISITED_PIPES
                        && !found.endpoints().isEmpty(),
                "Discovery visits were not bounded");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void playerSurfaceIsRegistered(
            GameTestHelper helper) {
        helper.assertTrue(
                CoverDefinitionCatalog.find(GenericNetworkKinds.STORAGE)
                        .isPresent(),
                "Generic storage definition missing");
        helper.assertTrue(
                ModItems.LOGISTICS_GENERIC_STORAGE_COVER.get() != null
                        && ModItems.LOGISTICS_GENERIC_IMPORT_COVER.get() != null
                        && ModItems.LOGISTICS_GENERIC_EXPORT_COVER.get() != null,
                "Generic cover items missing");
        PlayerCompleteSmoke.writeIfConfigured(
                "gameTestServer",
                "logistics/generic-network/core");
        helper.assertTrue(
                PlayerCompleteSmoke.snapshot(
                                "gameTestServer",
                                "logistics/generic-network/core")
                        .get("status").getAsString().equals("PASS"),
                "Player-complete registry snapshot failed");
        helper.succeed();
    }

    private static boolean craftsTo(
            GameTestHelper helper,
            net.minecraft.world.item.Item item) {
        List<ItemStack> slots;
        if (item == ModItems.LOGISTICS_GENERIC_STORAGE_COVER.get()) {
            slots = List.of(
                    ItemStack.EMPTY,
                    new ItemStack(Items.IRON_INGOT),
                    ItemStack.EMPTY,
                    new ItemStack(Items.CHEST),
                    new ItemStack(Items.HOPPER),
                    new ItemStack(Items.BUCKET));
        } else if (item == ModItems.LOGISTICS_GENERIC_IMPORT_COVER.get()) {
            slots = List.of(
                    ItemStack.EMPTY,
                    new ItemStack(Items.IRON_INGOT),
                    ItemStack.EMPTY,
                    new ItemStack(Items.COMPARATOR),
                    new ItemStack(Items.HOPPER),
                    new ItemStack(Items.BUCKET));
        } else {
            slots = List.of(
                    ItemStack.EMPTY,
                    new ItemStack(Items.IRON_INGOT),
                    ItemStack.EMPTY,
                    new ItemStack(Items.DROPPER),
                    new ItemStack(Items.HOPPER),
                    new ItemStack(Items.BUCKET));
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

    private static ItemLayout placeItemLine(
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
        source.setItem(0, new ItemStack(Items.IRON_INGOT, ITEM_MOVED));
        ItemPipeBlockEntity exportBe = helper.getBlockEntity(exportPipe);
        ItemPipeBlockEntity storageBe = helper.getBlockEntity(storagePipe);
        helper.assertTrue(
                exportBe.setCover(
                        Direction.WEST,
                        networked(GenericNetworkKinds.EXPORT, exportNet)),
                "Could not install export cover");
        helper.assertTrue(
                storageBe.setCover(
                        Direction.EAST,
                        networked(GenericNetworkKinds.STORAGE, storageNet)),
                "Could not install storage cover");
        return new ItemLayout(
                sourcePos,
                exportPipe,
                midPipe,
                storagePipe,
                storageChest,
                source,
                storage);
    }

    private static FluidLayout placeFluidLine(
            GameTestHelper helper, int exportNet, int storageNet, int fill) {
        BlockPos sourcePos = new BlockPos(1, 2, 1);
        BlockPos exportPipe = sourcePos.east();
        BlockPos midPipe = exportPipe.east();
        BlockPos storagePipe = midPipe.east();
        BlockPos destPos = storagePipe.east();
        helper.setBlock(
                sourcePos,
                pipeState(fluidPipe(), Direction.EAST));
        helper.setBlock(
                exportPipe,
                pipeState(fluidPipe(), Direction.WEST, Direction.EAST));
        helper.setBlock(
                midPipe,
                pipeState(fluidPipe(), Direction.WEST, Direction.EAST));
        helper.setBlock(
                storagePipe,
                pipeState(fluidPipe(), Direction.WEST, Direction.EAST));
        helper.setBlock(
                destPos,
                pipeState(fluidPipe(), Direction.WEST));
        FluidPipeBlockEntity source = helper.getBlockEntity(sourcePos);
        FluidPipeBlockEntity dest = helper.getBlockEntity(destPos);
        IFluidHandler sourceTank = source.fluidHandler(Direction.EAST);
        helper.assertTrue(sourceTank != null, "Source pipe has no tank");
        int filled = sourceTank.fill(
                new FluidStack(Fluids.WATER, fill),
                IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(filled == fill, "Could not prefill source pipe");
        FluidPipeBlockEntity exportBe = helper.getBlockEntity(exportPipe);
        FluidPipeBlockEntity storageBe = helper.getBlockEntity(storagePipe);
        helper.assertTrue(
                exportBe.setCover(
                        Direction.WEST,
                        networked(GenericNetworkKinds.EXPORT, exportNet)),
                "Could not install export cover");
        helper.assertTrue(
                storageBe.setCover(
                        Direction.EAST,
                        networked(GenericNetworkKinds.STORAGE, storageNet)),
                "Could not install storage cover");
        return new FluidLayout(
                sourcePos,
                exportPipe,
                midPipe,
                storagePipe,
                destPos,
                source,
                dest);
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

    private static FluidPipeBlock fluidPipe() {
        return (FluidPipeBlock) ModBlocks.pipeBlock(
                "copper",
                MaterialPrefixes.FLUID_PIPE,
                PipeCatalog.Kind.FLUID).get();
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

    private record ItemLayout(
            BlockPos sourceChest,
            BlockPos exportPipe,
            BlockPos midPipe,
            BlockPos storagePipe,
            BlockPos storageChest,
            ChestBlockEntity source,
            ChestBlockEntity storage) {}

    private record FluidLayout(
            BlockPos sourcePos,
            BlockPos exportPipe,
            BlockPos midPipe,
            BlockPos storagePipe,
            BlockPos destPos,
            FluidPipeBlockEntity source,
            FluidPipeBlockEntity dest) {}
}
