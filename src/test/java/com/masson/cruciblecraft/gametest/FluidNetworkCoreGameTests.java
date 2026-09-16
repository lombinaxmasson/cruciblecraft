package com.masson.cruciblecraft.gametest;

import java.util.List;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.AbstractPipeBlock;
import com.masson.cruciblecraft.content.block.FluidPipeBlock;
import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.logistics.fluidnet.FluidLogisticsNetwork;
import com.masson.cruciblecraft.logistics.fluidnet.FluidNetworkKinds;
import com.masson.cruciblecraft.logistics.fluidnet.FluidNetworkLimits;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated fluid-network basic-transfer gate. Run with
 * {@code -PwaveRecipes=runtime/fluid-network-basic-transfer}.
 */
@GameTestHolder(FluidNetworkCoreGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class FluidNetworkCoreGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_runtime_fluid_network_basic_transfer";
    private static final String TEMPLATE = "empty";
    /** Copper fluid pipe capacity is 600 mB. */
    private static final int MOVED = 600;

    private FluidNetworkCoreGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void sameIdentityConnectedExportsIntoStorage(
            GameTestHelper helper) {
        Layout layout = placeLine(helper, 1, 1, MOVED);
        helper.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    helper.assertTrue(
                            layout.source().storedFluid().isEmpty(),
                            "Export did not drain the source pipe");
                    helper.assertTrue(
                            layout.dest().storedFluid().getAmount() == MOVED,
                            "Storage did not receive exported fluid");
                    helper.assertTrue(
                            !FluidLogisticsNetwork.discoverStorage(
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
        Layout layout = placeLine(helper, 1, 1, MOVED);
        helper.setBlock(
                layout.midPipe(),
                Blocks.AIR.defaultBlockState());
        helper.assertTrue(
                !FluidLogisticsNetwork.isVisibleStorage(
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
        Layout layout = placeLine(helper, 1, 2, MOVED);
        helper.assertTrue(
                !FluidLogisticsNetwork.isVisibleStorage(
                        helper.getLevel(),
                        helper.absolutePos(layout.exportPipe()),
                        1,
                        helper.absolutePos(layout.storagePipe()),
                        Direction.EAST),
                "Mismatched network ids were visible to each other");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void noTargetDoesNotSwallowFluids(
            GameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(1, 2, 1);
        BlockPos pipePos = sourcePos.east();
        helper.setBlock(
                sourcePos,
                pipeState(fluidPipe(), Direction.EAST));
        helper.setBlock(
                pipePos,
                pipeState(fluidPipe(), Direction.WEST));
        FluidPipeBlockEntity source = helper.getBlockEntity(sourcePos);
        IFluidHandler sourceTank = source.fluidHandler(Direction.EAST);
        helper.assertTrue(sourceTank != null, "Source pipe has no tank");
        helper.assertTrue(
                sourceTank.fill(
                        new FluidStack(Fluids.WATER, MOVED),
                        IFluidHandler.FluidAction.EXECUTE) == MOVED,
                "Could not prefill source pipe");
        FluidPipeBlockEntity pipe = helper.getBlockEntity(pipePos);
        helper.assertTrue(
                pipe.setCover(
                        Direction.WEST,
                        networked(FluidNetworkKinds.EXPORT, 1)),
                "Could not install export cover");
        helper.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    helper.assertTrue(
                            source.storedFluid().getAmount() == MOVED,
                            "No-target export swallowed fluid");
                    helper.assertTrue(
                            FluidLogisticsNetwork.discoverStorage(
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
        Layout layout = placeLine(helper, 1, 1, MOVED);
        FluidPipeBlockEntity export = helper.getBlockEntity(layout.exportPipe());
        var tag = export.saveWithoutMetadata(
                helper.getLevel().registryAccess());
        export.loadWithComponents(
                tag, helper.getLevel().registryAccess());
        PipeCover reloaded = export.coverSnapshot().get(Direction.WEST);
        helper.assertTrue(
                reloaded != null
                        && FluidNetworkKinds.networkId(reloaded) == 1,
                "Export network id did not survive reload");
        helper.startSequence()
                .thenIdle(40)
                .thenExecute(() -> helper.assertTrue(
                        layout.dest().storedFluid().getAmount() == MOVED,
                        "Reloaded export cover did not keep transferring"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void importPullsFromStorage(
            GameTestHelper helper) {
        BlockPos storageTank = new BlockPos(1, 2, 1);
        BlockPos storagePipe = storageTank.east();
        BlockPos destPipe = storagePipe.east();
        BlockPos destTank = destPipe.east();
        helper.setBlock(
                storageTank,
                pipeState(fluidPipe(), Direction.EAST));
        helper.setBlock(
                storagePipe,
                pipeState(fluidPipe(), Direction.WEST, Direction.EAST));
        helper.setBlock(
                destPipe,
                pipeState(fluidPipe(), Direction.WEST, Direction.EAST));
        helper.setBlock(
                destTank,
                pipeState(fluidPipe(), Direction.WEST));
        FluidPipeBlockEntity storage = helper.getBlockEntity(storageTank);
        IFluidHandler storageHandler = storage.fluidHandler(Direction.EAST);
        helper.assertTrue(storageHandler != null, "Storage tank missing");
        helper.assertTrue(
                storageHandler.fill(
                        new FluidStack(Fluids.WATER, MOVED),
                        IFluidHandler.FluidAction.EXECUTE) == MOVED,
                "Could not prefill storage tank");
        FluidPipeBlockEntity storageBe = helper.getBlockEntity(storagePipe);
        FluidPipeBlockEntity destBe = helper.getBlockEntity(destPipe);
        helper.assertTrue(
                storageBe.setCover(
                        Direction.WEST,
                        networked(FluidNetworkKinds.STORAGE, 1)),
                "Could not install storage cover");
        helper.assertTrue(
                destBe.setCover(
                        Direction.EAST,
                        networked(FluidNetworkKinds.IMPORT, 1)),
                "Could not install import cover");
        helper.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    FluidPipeBlockEntity dest = helper.getBlockEntity(destTank);
                    helper.assertTrue(
                            dest.storedFluid().getAmount() == MOVED,
                            "Import did not pull from storage");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void coversAreSurvivalCraftable(
            GameTestHelper helper) {
        helper.assertTrue(
                craftsTo(helper, ModItems.LOGISTICS_FLUID_STORAGE_COVER.get()),
                "Storage cover recipe missing");
        helper.assertTrue(
                craftsTo(helper, ModItems.LOGISTICS_FLUID_IMPORT_COVER.get()),
                "Import cover recipe missing");
        helper.assertTrue(
                craftsTo(helper, ModItems.LOGISTICS_FLUID_EXPORT_COVER.get()),
                "Export cover recipe missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void loadAxisCapsAreRecorded(
            GameTestHelper helper) {
        Layout layout = placeLine(helper, 1, 1, MOVED);
        FluidLogisticsNetwork.Discovery found =
                FluidLogisticsNetwork.discoverStorage(
                        helper.getLevel(),
                        helper.absolutePos(layout.exportPipe()),
                        1);
        helper.assertTrue(
                found.visits() > 0
                        && found.visits()
                                <= FluidNetworkLimits.MAX_VISITED_PIPES
                        && !found.endpoints().isEmpty(),
                "Discovery visits were not bounded");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void playerSurfaceIsRegistered(
            GameTestHelper helper) {
        helper.assertTrue(
                CoverDefinitionCatalog.find(FluidNetworkKinds.STORAGE)
                        .isPresent(),
                "Fluid storage definition missing");
        helper.assertTrue(
                ModItems.LOGISTICS_FLUID_STORAGE_COVER.get() != null
                        && ModItems.LOGISTICS_FLUID_IMPORT_COVER.get() != null
                        && ModItems.LOGISTICS_FLUID_EXPORT_COVER.get() != null,
                "Fluid cover items missing");
        PlayerCompleteSmoke.writeIfConfigured("gameTestServer");
        helper.assertTrue(
                PlayerCompleteSmoke.snapshot("gameTestServer")
                        .get("status").getAsString().equals("PASS"),
                "Player-complete registry snapshot failed");
        helper.succeed();
    }

    private static boolean craftsTo(
            GameTestHelper helper,
            net.minecraft.world.item.Item item) {
        List<ItemStack> slots;
        if (item == ModItems.LOGISTICS_FLUID_STORAGE_COVER.get()) {
            slots = List.of(
                    ItemStack.EMPTY,
                    new ItemStack(Items.IRON_INGOT),
                    ItemStack.EMPTY,
                    new ItemStack(Items.BUCKET),
                    new ItemStack(Items.HOPPER),
                    new ItemStack(Items.BUCKET));
        } else if (item == ModItems.LOGISTICS_FLUID_IMPORT_COVER.get()) {
            slots = List.of(
                    ItemStack.EMPTY,
                    new ItemStack(Items.IRON_INGOT),
                    ItemStack.EMPTY,
                    new ItemStack(Items.CAULDRON),
                    new ItemStack(Items.HOPPER),
                    new ItemStack(Items.CAULDRON));
        } else {
            slots = List.of(
                    ItemStack.EMPTY,
                    new ItemStack(Items.IRON_INGOT),
                    ItemStack.EMPTY,
                    new ItemStack(Items.DISPENSER),
                    new ItemStack(Items.HOPPER),
                    new ItemStack(Items.DISPENSER));
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
                        networked(FluidNetworkKinds.EXPORT, exportNet)),
                "Could not install export cover");
        helper.assertTrue(
                storageBe.setCover(
                        Direction.EAST,
                        networked(FluidNetworkKinds.STORAGE, storageNet)),
                "Could not install storage cover");
        return new Layout(
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

    private record Layout(
            BlockPos sourcePipe,
            BlockPos exportPipe,
            BlockPos midPipe,
            BlockPos storagePipe,
            BlockPos destPipe,
            FluidPipeBlockEntity source,
            FluidPipeBlockEntity dest) {}
}
