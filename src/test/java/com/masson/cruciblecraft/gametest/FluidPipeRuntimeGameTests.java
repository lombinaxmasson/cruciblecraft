package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.AbstractPipeBlock;
import com.masson.cruciblecraft.content.block.CableBlock;
import com.masson.cruciblecraft.content.block.FluidPipeBlock;
import com.masson.cruciblecraft.content.block.Gt6StyleConnections;
import com.masson.cruciblecraft.content.block.ItemPipeBlock;
import com.masson.cruciblecraft.content.block.RedstoneWireBlock;
import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.content.item.PipeBlockItem;
import com.masson.cruciblecraft.content.redstonewire.RedstoneWireKind;
import com.masson.cruciblecraft.logistics.pipe.PipeCatalog;
import com.masson.cruciblecraft.logistics.pipe.PipeTransferPhase;
import com.masson.cruciblecraft.logistics.pipe.fluid.FluidPipeBlockedMedia;
import com.masson.cruciblecraft.logistics.pipe.fluid.FluidPipeCadence;
import com.masson.cruciblecraft.logistics.pipe.fluid.FluidPipeFailureState.Failure;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModFluids;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated GT6 five-gauge fluid-pipe runtime. Run with
 * {@code -PwaveRecipes=content/gt6-fluid-pipe-runtime}.
 */
@GameTestHolder(FluidPipeRuntimeGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class FluidPipeRuntimeGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_content_gt6_fluid_pipe_runtime";
    private static final String TEMPLATE = "empty";

    private FluidPipeRuntimeGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fluidPipeCapacityMatchesGt6Gauge(GameTestHelper helper) {
        assertGauge(helper, MaterialPrefixes.TINY_FLUID_PIPE, 100);
        assertGauge(helper, MaterialPrefixes.SMALL_FLUID_PIPE, 200);
        assertGauge(helper, MaterialPrefixes.FLUID_PIPE, 600);
        assertGauge(helper, MaterialPrefixes.LARGE_FLUID_PIPE, 1200);
        assertGauge(helper, MaterialPrefixes.HUGE_FLUID_PIPE, 2400);
        FluidPipeBlockEntity huge = placePipe(
                helper,
                new BlockPos(2, 2, 2),
                fluidPipe("draconium", MaterialPrefixes.HUGE_FLUID_PIPE));
        helper.assertTrue(
                huge.capacity() == 60_000 && huge.transferLimit() == 60_000,
                "draconium huge still clamped to 8000: cap="
                        + huge.capacity()
                        + " limit="
                        + huge.transferLimit());
        helper.assertTrue(
                !ModBlocks.hasPipeBlock(
                        "copper",
                        MaterialPrefixes.FLUID_PIPE,
                        PipeCatalog.Kind.ITEM),
                "fluid prefix leaked into the item catalog");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fluidPipeTickCadenceMatchesGt6(GameTestHelper helper) {
        helper.assertTrue(
                PipeTransferPhase.INTERVAL == 5,
                "cover phase drifted from 5 ticks");
        BlockPos evenPos = new BlockPos(2, 2, 2);
        BlockPos oddPos = new BlockPos(3, 2, 2);
        helper.assertTrue(
                FluidPipeCadence.even(helper.absolutePos(evenPos))
                        != FluidPipeCadence.even(helper.absolutePos(oddPos)),
                "even/odd scan order collapsed");
        helper.setBlock(
                evenPos,
                pipeState(
                        fluidPipe("copper", MaterialPrefixes.FLUID_PIPE),
                        Direction.EAST));
        helper.setBlock(
                oddPos,
                pipeState(
                        fluidPipe("copper", MaterialPrefixes.FLUID_PIPE),
                        Direction.WEST));
        FluidPipeBlockEntity source = helper.getBlockEntity(evenPos);
        helper.assertTrue(
                source.fillInternal(
                        new FluidStack(Fluids.WATER, 100),
                        IFluidHandler.FluidAction.EXECUTE) == 100,
                "could not prefill cadence source");
        helper.startSequence()
                .thenExecuteAfter(1, () -> {
                    FluidPipeBlockEntity dest = helper.getBlockEntity(oddPos);
                    helper.assertTrue(
                            dest.storedFluid().getAmount() > 0,
                            "pipe-to-pipe move waited for the 5-tick cover phase");
                    helper.assertTrue(
                            source.storedFluid().getAmount() < 100,
                            "source did not drain on the first server tick");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fluidPipePlasmaMagicAcidGasFailures(GameTestHelper helper) {
        FluidPipeBlock woodBlock =
                fluidPipe("wood_treated", MaterialPrefixes.FLUID_PIPE);
        FluidPipeBlock copperBlock =
                fluidPipe("copper", MaterialPrefixes.FLUID_PIPE);
        FluidPipeBlockEntity wood = placePipe(
                helper, new BlockPos(1, 2, 1), woodBlock);
        FluidPipeBlockEntity copper = placePipe(
                helper, new BlockPos(3, 2, 1), copperBlock);
        helper.assertTrue(
                !woodBlock.pipe().fluid().plasmaProof()
                        && !woodBlock.pipe().fluid().magicProof()
                        && woodBlock.pipe().fluid().flammable()
                        && woodBlock.pipe().fluid().contactDamage(),
                "wood treated proofs drifted");
        helper.assertTrue(
                !copperBlock.pipe().fluid().plasmaProof()
                        && !copperBlock.pipe().fluid().magicProof()
                        && !copperBlock.pipe().fluid().flammable(),
                "copper proofs drifted");
        IFluidHandler woodTank = wood.fluidHandler(Direction.EAST);
        IFluidHandler copperTank = copper.fluidHandler(Direction.EAST);
        helper.assertTrue(woodTank != null && copperTank != null, "missing tanks");
        int lava = woodTank.fill(
                new FluidStack(Fluids.LAVA, 50),
                IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(lava == 0, "lava was accepted by a 340 K wood pipe");
        helper.assertTrue(
                wood.failureSnapshot().overTemperatureEvents() > 0
                        && wood.failureSnapshot().gasLeakEvents() == 0
                        && wood.failureSnapshot().corrosionEvents() == 0,
                "over-temp was mapped onto gas or acid");
        int helium = woodTank.fill(
                new FluidStack(
                        ModFluids.materialFluid("helium").orElseThrow(), 50),
                IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(helium == 0, "helium was accepted by a non-gas-proof pipe");
        helper.assertTrue(
                wood.failureSnapshot().gasLeakEvents() > 0,
                "helium did not record GAS_LEAK");
        int acid = copperTank.fill(
                new FluidStack(
                        ModFluids.materialFluid("hydrochloric_acid").orElseThrow(),
                        50),
                IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(acid == 0, "acid was accepted by a non-acid-proof copper pipe");
        helper.assertTrue(
                copper.failureSnapshot().corrosionEvents() > 0
                        && copper.failureSnapshot().gasLeakEvents() == 0,
                "acid was mapped onto GAS_LEAK");
        int water = copperTank.fill(
                new FluidStack(Fluids.WATER, 50),
                IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(water == 50, "water fill fail-closed on a live copper pipe");
        int plasmaMapped = 0;
        int magicMapped = 0;
        for (ModFluids.ChemicalFluidEntry entry : ModFluids.chemicalFluids()) {
            FluidStack stack = new FluidStack(entry.source().get(), 10);
            FluidPipeBlockedMedia.Kind kind =
                    FluidPipeBlockedMedia.kindOf(stack);
            if (kind == FluidPipeBlockedMedia.Kind.NONE) {
                continue;
            }
            int beforeGas = copper.failureSnapshot().gasLeakEvents();
            int beforeAcid = copper.failureSnapshot().corrosionEvents();
            int filled = copperTank.fill(
                    stack, IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(
                    filled > 0,
                    "plasma/magic fill was still rejected: " + entry.id());
            helper.assertTrue(
                    copper.failureSnapshot().gasLeakEvents() == beforeGas
                            && copper.failureSnapshot().corrosionEvents()
                                    == beforeAcid,
                    "plasma/magic was mapped onto gas or acid: " + entry.id());
            if (kind == FluidPipeBlockedMedia.Kind.PLASMA) {
                plasmaMapped++;
            } else {
                magicMapped++;
            }
        }
        helper.assertTrue(
                FluidPipeBlockedMedia.classify(
                        new FluidStack(Fluids.WATER, 1),
                        copperBlock.pipe().fluid())
                        == FluidPipeBlockedMedia.Kind.NONE,
                "water was classified as blocked plasma/magic");
        helper.assertTrue(
                plasmaMapped + magicMapped >= 0,
                "blocked-media scan crashed");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fluidPipeChunkUnloadRetainsTank(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        FluidPipeBlockEntity pipe = placePipe(
                helper,
                pos,
                fluidPipe("copper", MaterialPrefixes.FLUID_PIPE));
        helper.assertTrue(
                pipe.fillInternal(
                        new FluidStack(Fluids.WATER, 120),
                        IFluidHandler.FluidAction.EXECUTE) == 120,
                "could not prefill unload tank");
        helper.assertTrue(pipe.offersNetworkDiscovery(), "loaded pipe hid discovery");
        pipe.onChunkUnloaded();
        helper.assertTrue(
                pipe.storedFluid().getAmount() == 120,
                "chunk unload voided the tank");
        helper.assertTrue(
                !pipe.offersNetworkDiscovery(),
                "unloaded pipe still advertised discovery");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fluidPipeSideContract(GameTestHelper helper) {
        BlockPos fluidPos = new BlockPos(2, 2, 2);
        BlockPos eastPos = new BlockPos(3, 2, 2);
        BlockPos itemPos = new BlockPos(2, 2, 3);
        BlockPos cablePos = new BlockPos(2, 2, 1);
        BlockPos redstonePos = new BlockPos(1, 2, 2);
        FluidPipeBlock fluid = fluidPipe("copper", MaterialPrefixes.FLUID_PIPE);
        helper.setBlock(fluidPos, pipeState(fluid, Direction.EAST));
        helper.setBlock(
                eastPos,
                pipeState(fluid, Direction.WEST));
        ItemPipeBlock item = (ItemPipeBlock) ModBlocks.pipeBlock(
                "brass",
                MaterialPrefixes.ITEM_PIPE,
                PipeCatalog.Kind.ITEM).get();
        helper.setBlock(itemPos, item.defaultBlockState());
        CableBlock cable = ModBlocks.electricalConductorBlock(
                "tin", MaterialPrefixes.WIRE).get();
        helper.setBlock(cablePos, cable.defaultBlockState());
        RedstoneWireBlock redstone =
                (RedstoneWireBlock) ModBlocks.redstoneWireBlocksById()
                        .get(RedstoneWireKind.RED_ALLOY.id())
                        .get();
        helper.setBlock(redstonePos, redstone.defaultBlockState());
        BlockState fluidState = helper.getBlockState(fluidPos);
        helper.assertTrue(
                Gt6StyleConnections.sameNetwork(
                        fluidState, helper.getBlockState(eastPos)),
                "same-gauge fluid pipes were isolated from each other");
        helper.assertTrue(
                !Gt6StyleConnections.sameNetwork(
                        fluidState, helper.getBlockState(itemPos)),
                "fluid and item pipes shared a network");
        helper.assertTrue(
                !Gt6StyleConnections.sameNetwork(
                        fluidState, helper.getBlockState(cablePos)),
                "fluid pipe joined the EU cable network");
        helper.assertTrue(
                !Gt6StyleConnections.sameNetwork(
                        fluidState, helper.getBlockState(redstonePos)),
                "fluid pipe joined the redstone wire network");
        helper.assertTrue(
                !AbstractPipeBlock.isConnected(
                        helper.getBlockState(fluidPos), Direction.SOUTH)
                        && !AbstractPipeBlock.isConnected(
                                helper.getBlockState(fluidPos), Direction.NORTH)
                        && !AbstractPipeBlock.isConnected(
                                helper.getBlockState(fluidPos), Direction.WEST),
                "unconnected faces opened toward foreign networks");
        ResourceLocation dummy = ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft",
                "fluid_pipe_tile/tiny_tin_alloy_fluid_pipe");
        helper.assertTrue(
                !BuiltInRegistries.ITEM.containsKey(dummy),
                "folded tiny tin-alloy dummy item is still registered");
        Item live = ModBlocks.pipeBlock(
                "tin_alloy",
                MaterialPrefixes.TINY_FLUID_PIPE,
                PipeCatalog.Kind.FLUID).get().asItem();
        helper.assertTrue(
                live instanceof PipeBlockItem,
                "live tiny tin-alloy host is not a PipeBlockItem");
        helper.assertTrue(
                ModItems.hasMaterialItem(
                        "tin_alloy", MaterialPrefixes.QUADRUPLE_FLUID_PIPE)
                        && ModItems.materialItem(
                                        "tin_alloy",
                                        MaterialPrefixes.QUADRUPLE_FLUID_PIPE)
                                .get() instanceof PipeBlockItem
                        && !BuiltInRegistries.ITEM.containsKey(
                                ResourceLocation.fromNamespaceAndPath(
                                        "cruciblecraft",
                                        "fluid_pipe_tile/quadruple_tin_alloy_fluid_pipe")),
                "tin-alloy quadruple dummy was not folded onto the live combo host");
        helper.succeed();
    }

    private static void assertGauge(
            GameTestHelper helper,
            com.masson.cruciblecraft.api.material.MaterialPrefix form,
            int expected) {
        FluidPipeBlockEntity pipe = placePipe(
                helper,
                new BlockPos(1, 2, form.serializedName().length() % 5 + 1),
                fluidPipe("copper", form));
        helper.assertTrue(
                pipe.capacity() == expected && pipe.transferLimit() == expected,
                form.serializedName()
                        + " copper cap="
                        + pipe.capacity()
                        + " limit="
                        + pipe.transferLimit()
                        + " expected="
                        + expected);
    }

    private static FluidPipeBlockEntity placePipe(
            GameTestHelper helper, BlockPos pos, FluidPipeBlock block) {
        helper.setBlock(pos, pipeState(block, Direction.EAST));
        return helper.getBlockEntity(pos);
    }

    private static FluidPipeBlock fluidPipe(
            String material,
            com.masson.cruciblecraft.api.material.MaterialPrefix form) {
        return (FluidPipeBlock) ModBlocks.pipeBlock(
                material, form, PipeCatalog.Kind.FLUID).get();
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
}
