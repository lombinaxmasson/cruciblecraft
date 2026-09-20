package com.masson.cruciblecraft.gametest;

import java.util.List;

import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.content.block.CableBlock;
import com.masson.cruciblecraft.content.block.FluidPipeBlock;
import com.masson.cruciblecraft.content.block.Gt6StyleConnections;
import com.masson.cruciblecraft.content.block.ItemPipeBlock;
import com.masson.cruciblecraft.content.block.RedstoneWireBlock;
import com.masson.cruciblecraft.content.blockentity.CableBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ItemPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.RedstoneWireBlockEntity;
import com.masson.cruciblecraft.content.redstonewire.RedstoneWireKind;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverBehaviors;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverKinds;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverVisuals;
import com.masson.cruciblecraft.logistics.pipe.PipeCatalog;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverBehaviorRegistry;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverDefinitionCatalog;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCover;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCoverItems;
import com.masson.cruciblecraft.logistics.pipe.cover.PlateCovers;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModFluids;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.registry.ModProcessingMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Isolated runtime gate for the GT6 machine-cover remainder.
 *  Run with {@code -PwaveRecipes=runtime/cover-remainder}. */
@GameTestHolder(CoverRemainderGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class CoverRemainderGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_runtime_cover_remainder";
    private static final String TEMPLATE = "empty";
    private static final BlockPos MACHINE_POS = new BlockPos(2, 2, 2);
    private static final BlockPos WIRE_POS = MACHINE_POS;
    private static final Direction COVER_SIDE = Direction.NORTH;
    private static final Direction TORCH_SIDE = Direction.EAST;

    private CoverRemainderGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void playerSurfaceIsRegistered(GameTestHelper helper) {
        CoverBehaviorRegistry.validateDefinitions();
        helper.assertTrue(
                CoverDefinitionCatalog.definitions().stream()
                        .filter(definition -> MachineCoverKinds
                                .BEHAVIOR_PATHS
                                .contains(definition.behaviorId().getPath()))
                        .count() == MachineCoverKinds.DEFINITION_COUNT,
                "Machine cover definition denominator drifted");
        helper.assertTrue(
                ModItems.machineCovers().size() == MachineCoverKinds.ITEM_COUNT,
                "Machine cover item registration drifted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void blankCoverIsSurvivalCraftable(GameTestHelper helper) {
        ItemStack aluminiumPlate = MaterialLookup.stack(
                "aluminium", MaterialPrefixes.PLATE);
        ItemStack aluminiumScrew = MaterialLookup.stack(
                "aluminium", MaterialPrefixes.SCREW);
        ItemStack assembled = craft(
                helper,
                3,
                2,
                List.of(
                        aluminiumScrew,
                        new ItemStack(ModItems.SMITHING_HAMMER.get()),
                        ItemStack.EMPTY,
                        aluminiumPlate,
                        new ItemStack(ModItems.MATERIAL_SCREWDRIVER.get()),
                        ItemStack.EMPTY));
        helper.assertTrue(
                assembled.is(ModItems.machineCover(
                                MachineCoverKinds.itemPath("cover_blank"))
                        .get())
                        && assembled.getCount() == 1,
                "GT6 blank cover recipe did not assemble");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void blankCoverRequiresCraftingTable(GameTestHelper helper) {
        var blank = ModItems.machineCover(
                MachineCoverKinds.itemPath("cover_blank")).get();
        var recipe = helper.getLevel()
                .getRecipeManager()
                .getAllRecipesFor(RecipeType.CRAFTING)
                .stream()
                .map(holder -> holder.value())
                .filter(value -> value.getResultItem(
                        helper.getLevel().registryAccess()).is(blank))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Blank cover crafting recipe is missing"));
        helper.assertFalse(
                recipe.canCraftInDimensions(2, 2),
                "Blank cover recipe advertised a player 2x2 grid");
        helper.assertTrue(
                recipe.canCraftInDimensions(3, 3),
                "Blank cover recipe refused a crafting table");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void controllerStopsProcessingWhenOff(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity machine =
                OrdinaryClosureHostGameTests.place(
                        helper,
                        MACHINE_POS,
                        ModBlocks.OVEN.get(),
                        ModProcessingMachines.OVEN);
        helper.setBlock(
                MACHINE_POS.relative(COVER_SIDE),
                Blocks.REDSTONE_BLOCK);
        helper.assertTrue(
                machine.setCover(
                        COVER_SIDE,
                        PipeCover.of("cruciblecraft:controller_redstone")),
                "Could not install controller cover");
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        machine.coverEnabled(),
                        "Redstone controller did not enable the machine"))
                .thenExecute(() -> helper.setBlock(
                        MACHINE_POS.relative(COVER_SIDE),
                        Blocks.AIR))
                .thenIdle(2)
                .thenExecute(() -> helper.assertFalse(
                        machine.coverEnabled(),
                        "Redstone controller did not stop the machine"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void detectorEmitsWhenPossible(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity machine =
                OrdinaryClosureHostGameTests.place(
                        helper,
                        MACHINE_POS,
                        ModBlocks.OVEN.get(),
                        ModProcessingMachines.OVEN);
        machine.inventory().setStackInSlot(
                machine.spec().items().inputs().getFirst(),
                new ItemStack(Items.COBBLESTONE));
        helper.assertTrue(
                machine.setCover(
                        COVER_SIDE,
                        PipeCover.of(
                                "cruciblecraft:detector_running_possible")),
                "Could not install detector cover");
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        MachineCoverBehaviors.weakRedstone(
                                machine, COVER_SIDE) == 15,
                        "Possible detector did not emit 15"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void torchRefusesMachineHost(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity machine =
                OrdinaryClosureHostGameTests.place(
                        helper,
                        MACHINE_POS,
                        ModBlocks.OVEN.get(),
                        ModProcessingMachines.OVEN);
        helper.assertFalse(
                MachineCoverBehaviors.canPlace(
                        machine,
                        COVER_SIDE,
                        PipeCover.of("cruciblecraft:redstone_torch")),
                "Redstone torch cover attached to an unsupported host");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void torchAttachesToAllRedstoneConnectors(GameTestHelper helper) {
        PipeCover torch = PipeCover.of("cruciblecraft:redstone_torch");
        for (RedstoneWireKind kind : RedstoneWireKind.catalog()) {
            helper.setBlock(WIRE_POS, redstoneWire(kind, Direction.WEST));
            RedstoneWireBlockEntity wire = helper.getBlockEntity(WIRE_POS);
            helper.assertTrue(
                    wire.setCover(TORCH_SIDE, torch),
                    "Torch cover refused redstone connector " + kind.path());
            helper.assertTrue(
                    wire.covers().get(TORCH_SIDE).isPresent()
                            && !RedstoneWireBlock.isConnected(
                                    helper.getBlockState(WIRE_POS),
                                    TORCH_SIDE),
                    "Torch did not persist on " + kind.path());
            helper.setBlock(WIRE_POS, Blocks.AIR);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void torchRefusesItemPipe(GameTestHelper helper) {
        ItemPipeBlock block = (ItemPipeBlock) ModBlocks.pipeBlock(
                "brass",
                MaterialPrefixes.ITEM_PIPE,
                PipeCatalog.Kind.ITEM).get();
        helper.setBlock(WIRE_POS, block.defaultBlockState());
        ItemPipeBlockEntity pipe = helper.getBlockEntity(WIRE_POS);
        helper.assertFalse(
                pipe.setCover(
                        TORCH_SIDE,
                        PipeCover.of("cruciblecraft:redstone_torch")),
                "Torch cover attached to an item pipe");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void torchEmitsWhenWireUnpowered(GameTestHelper helper) {
        helper.setBlock(
                WIRE_POS,
                redstoneWire(RedstoneWireKind.RED_ALLOY, Direction.WEST));
        RedstoneWireBlockEntity wire = helper.getBlockEntity(WIRE_POS);
        helper.assertTrue(
                wire.setCover(
                        TORCH_SIDE,
                        PipeCover.of("cruciblecraft:redstone_torch")),
                "Could not install torch cover");
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    var world = helper.absolutePos(WIRE_POS);
                    var state = helper.getLevel().getBlockState(world);
                    int weak = state.getSignal(
                            helper.getLevel(), world, Direction.WEST);
                    int strong = state.getDirectSignal(
                            helper.getLevel(), world, Direction.WEST);
                    helper.assertTrue(
                            weak == 15 && strong == 15,
                            "Unpowered torch cover must emit 15 weak+strong: "
                                    + weak + "/" + strong);
                    helper.assertFalse(
                            RedstoneWireBlock.isConnected(
                                    helper.getBlockState(WIRE_POS),
                                    TORCH_SIDE),
                            "Torch cover left the face connected");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void repeaterEmitsWhenWirePowered(GameTestHelper helper) {
        helper.setBlock(WIRE_POS.relative(Direction.WEST), Blocks.REDSTONE_BLOCK);
        helper.setBlock(
                WIRE_POS,
                redstoneWire(RedstoneWireKind.RED_ALLOY_CABLE, Direction.WEST));
        RedstoneWireBlockEntity wire = helper.getBlockEntity(WIRE_POS);
        helper.assertTrue(
                wire.setCover(
                        TORCH_SIDE,
                        PipeCover.of("cruciblecraft:redstone_repeater")),
                "Could not install repeater cover");
        helper.startSequence()
                .thenIdle(4)
                .thenExecute(() -> {
                    helper.assertTrue(
                            wire.redstoneValue() > 0L,
                            "Insulated cable did not pick up the redstone block");
                    var world = helper.absolutePos(WIRE_POS);
                    var state = helper.getLevel().getBlockState(world);
                    int weak = state.getSignal(
                            helper.getLevel(), world, Direction.WEST);
                    int strong = state.getDirectSignal(
                            helper.getLevel(), world, Direction.WEST);
                    helper.assertTrue(
                            weak == 15 && strong == 15,
                            "Powered repeater cover must emit 15 weak+strong: "
                                    + weak + "/" + strong);
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void cutterCannotReopenTorchFace(GameTestHelper helper) {
        helper.setBlock(
                WIRE_POS,
                redstoneWire(
                        RedstoneWireKind.RED_ALLOY,
                        Direction.WEST,
                        TORCH_SIDE));
        RedstoneWireBlockEntity wire = helper.getBlockEntity(WIRE_POS);
        helper.assertTrue(
                wire.setCover(
                        TORCH_SIDE,
                        PipeCover.of("cruciblecraft:redstone_torch")),
                "Could not install torch cover");
        helper.assertFalse(
                RedstoneWireBlock.isConnected(
                        helper.getBlockState(WIRE_POS), TORCH_SIDE),
                "Torch cover did not disconnect the face");
        Gt6StyleConnections.setConnection(
                helper.getLevel(),
                helper.absolutePos(WIRE_POS),
                TORCH_SIDE,
                true);
        helper.assertFalse(
                RedstoneWireBlock.isConnected(
                        helper.getBlockState(WIRE_POS), TORCH_SIDE),
                "Cutter reopened a torch-intercepted face");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void blankCoverDoesNotDisconnect(GameTestHelper helper) {
        helper.setBlock(
                WIRE_POS,
                redstoneWire(RedstoneWireKind.RED_ALLOY, TORCH_SIDE));
        RedstoneWireBlockEntity wire = helper.getBlockEntity(WIRE_POS);
        helper.assertTrue(
                RedstoneWireBlock.isConnected(
                        helper.getBlockState(WIRE_POS), TORCH_SIDE),
                "Bare wire east face started closed");
        helper.assertTrue(
                wire.setCover(
                        TORCH_SIDE,
                        PipeCover.of("cruciblecraft:cover_blank")),
                "Could not install blank cover on redstone wire");
        helper.assertTrue(
                RedstoneWireBlock.isConnected(
                        helper.getBlockState(WIRE_POS), TORCH_SIDE),
                "Blank cover disconnected the redstone face");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void selectorTagFloorsUnpoweredWire(GameTestHelper helper) {
        helper.setBlock(
                WIRE_POS,
                redstoneWire(RedstoneWireKind.RED_ALLOY));
        RedstoneWireBlockEntity wire = helper.getBlockEntity(WIRE_POS);
        helper.assertTrue(
                wire.setCover(
                        TORCH_SIDE,
                        PipeCover.of("cruciblecraft:selector_tag")
                                .withDisplay(0, 2)),
                "Selector tag refused redstone wire");
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(
                            wire.mode() == 2,
                            "Selector tag did not write wire mode 2: "
                                    + wire.mode());
                    helper.assertTrue(
                            wire.visual() == 2,
                            "Selector mode did not floor the unpowered wire: "
                                    + wire.visual()
                                    + "/"
                                    + wire.redstoneValue());
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void emitterDoesNotDisconnectAndEmitsWeak(
            GameTestHelper helper) {
        helper.setBlock(
                WIRE_POS,
                redstoneWire(RedstoneWireKind.RED_ALLOY, TORCH_SIDE));
        RedstoneWireBlockEntity wire = helper.getBlockEntity(WIRE_POS);
        helper.assertTrue(
                wire.setCover(
                        TORCH_SIDE,
                        PipeCover.of("cruciblecraft:redstone_emitter")
                                .withDisplay(0, 7)),
                "Emitter refused redstone wire");
        helper.assertTrue(
                RedstoneWireBlock.isConnected(
                        helper.getBlockState(WIRE_POS), TORCH_SIDE),
                "Emitter disconnected the redstone face");
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    var world = helper.absolutePos(WIRE_POS);
                    var state = helper.getLevel().getBlockState(world);
                    int weak = state.getSignal(
                            helper.getLevel(), world, Direction.WEST);
                    int strong = state.getDirectSignal(
                            helper.getLevel(), world, Direction.WEST);
                    helper.assertTrue(
                            weak == 7 && strong == 0,
                            "Emitter must emit 7 weak and 0 strong: "
                                    + weak
                                    + "/"
                                    + strong);
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void conductorOutRepeatsIn(GameTestHelper helper) {
        helper.setBlock(WIRE_POS.relative(Direction.WEST), Blocks.REDSTONE_BLOCK);
        helper.setBlock(
                WIRE_POS,
                redstoneWire(RedstoneWireKind.RED_ALLOY));
        RedstoneWireBlockEntity wire = helper.getBlockEntity(WIRE_POS);
        helper.assertTrue(
                wire.setCover(
                        Direction.WEST,
                        PipeCover.of("cruciblecraft:redstone_conductor_in")),
                "Conductor IN refused redstone wire");
        helper.assertTrue(
                wire.setCover(
                        TORCH_SIDE,
                        PipeCover.of("cruciblecraft:redstone_conductor_out")),
                "Conductor OUT refused redstone wire");
        helper.startSequence()
                .thenIdle(4)
                .thenExecute(() -> {
                    var world = helper.absolutePos(WIRE_POS);
                    var state = helper.getLevel().getBlockState(world);
                    int weak = state.getSignal(
                            helper.getLevel(), world, Direction.WEST);
                    helper.assertTrue(
                            weak == 15,
                            "Conductor OUT did not repeat IN: " + weak);
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void scaleProgressTracksWire(GameTestHelper helper) {
        helper.setBlock(WIRE_POS.relative(Direction.WEST), Blocks.REDSTONE_BLOCK);
        helper.setBlock(
                WIRE_POS,
                redstoneWire(RedstoneWireKind.RED_ALLOY, Direction.WEST));
        RedstoneWireBlockEntity wire = helper.getBlockEntity(WIRE_POS);
        helper.assertTrue(
                wire.setCover(
                        TORCH_SIDE,
                        PipeCover.of("cruciblecraft:scale_progress")),
                "Progress scale refused redstone wire");
        helper.startSequence()
                .thenIdle(4)
                .thenExecute(() -> {
                    helper.assertTrue(
                            wire.redstoneValue() > 0L,
                            "Wire did not pick up the redstone block");
                    var world = helper.absolutePos(WIRE_POS);
                    var state = helper.getLevel().getBlockState(world);
                    int weak = state.getSignal(
                            helper.getLevel(), world, Direction.WEST);
                    helper.assertTrue(
                            weak > 0,
                            "Progress scale did not emit wire fill: " + weak);
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void remainderRefusesControllerOnWire(GameTestHelper helper) {
        helper.setBlock(
                WIRE_POS,
                redstoneWire(RedstoneWireKind.RED_ALLOY));
        RedstoneWireBlockEntity wire = helper.getBlockEntity(WIRE_POS);
        helper.assertFalse(
                wire.setCover(
                        TORCH_SIDE,
                        PipeCover.of("cruciblecraft:controller_auto")),
                "Auto switch attached to a redstone wire");
        helper.assertFalse(
                wire.setCover(
                        TORCH_SIDE,
                        PipeCover.of("cruciblecraft:scale_energy")),
                "Energy scale attached to a redstone wire");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void plateCoverDoesNotDisconnectWire(GameTestHelper helper) {
        helper.setBlock(
                WIRE_POS,
                redstoneWire(RedstoneWireKind.RED_ALLOY, TORCH_SIDE));
        RedstoneWireBlockEntity wire = helper.getBlockEntity(WIRE_POS);
        PipeCover plate = PlateCovers.fromItem(
                MaterialLookup.stack("iron", MaterialPrefixes.PLATE));
        helper.assertTrue(plate != null, "Iron plate is not a cover");
        helper.assertTrue(
                wire.setCover(TORCH_SIDE, plate),
                "Plate cover refused redstone wire");
        helper.assertTrue(
                RedstoneWireBlock.isConnected(
                        helper.getBlockState(WIRE_POS), TORCH_SIDE),
                "Plate cover disconnected the wire face");
        ItemStack dropped = PipeCoverItems.stackFor(
                wire.covers().get(TORCH_SIDE).orElseThrow());
        helper.assertTrue(
                MaterialLookup.matches(
                        dropped, "iron", MaterialPrefixes.PLATE),
                "Plate cover did not drop the live iron plate");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void dumpCoverDisconnectsItemPipe(GameTestHelper helper) {
        ItemPipeBlock block = (ItemPipeBlock) ModBlocks.pipeBlock(
                "brass",
                MaterialPrefixes.ITEM_PIPE,
                PipeCatalog.Kind.ITEM).get();
        BlockPos neighbor = WIRE_POS.relative(TORCH_SIDE);
        helper.setBlock(WIRE_POS, block.defaultBlockState());
        helper.setBlock(neighbor, block.defaultBlockState());
        Gt6StyleConnections.setConnection(
                helper.getLevel(),
                helper.absolutePos(WIRE_POS),
                TORCH_SIDE,
                true);
        ItemPipeBlockEntity pipe = helper.getBlockEntity(WIRE_POS);
        helper.assertTrue(
                pipe.setCover(
                        TORCH_SIDE,
                        PipeCover.of("cruciblecraft:logistics_generic_dump")),
                "Dump cover refused item pipe");
        helper.assertFalse(
                ItemPipeBlock.isConnected(
                        helper.getBlockState(WIRE_POS), TORCH_SIDE),
                "Dump cover left the pipe face connected");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void filterRefusesBetweenItemPipes(GameTestHelper helper) {
        ItemPipeBlock block = (ItemPipeBlock) ModBlocks.pipeBlock(
                "brass",
                MaterialPrefixes.ITEM_PIPE,
                PipeCatalog.Kind.ITEM).get();
        helper.setBlock(WIRE_POS, block.defaultBlockState());
        helper.setBlock(
                WIRE_POS.relative(TORCH_SIDE),
                block.defaultBlockState());
        ItemPipeBlockEntity pipe = helper.getBlockEntity(WIRE_POS);
        helper.assertFalse(
                pipe.setCover(
                        TORCH_SIDE,
                        PipeCover.of("cruciblecraft:filter")),
                "Filter attached between two item pipes");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void pressureValveRefusesPipeToPipe(GameTestHelper helper) {
        FluidPipeBlock block = (FluidPipeBlock) ModBlocks.pipeBlock(
                "bronze",
                MaterialPrefixes.FLUID_PIPE,
                PipeCatalog.Kind.FLUID).get();
        helper.setBlock(WIRE_POS, block.defaultBlockState());
        helper.setBlock(
                WIRE_POS.relative(TORCH_SIDE),
                block.defaultBlockState());
        FluidPipeBlockEntity pipe = helper.getBlockEntity(WIRE_POS);
        helper.assertFalse(
                pipe.setCover(
                        TORCH_SIDE,
                        PipeCover.of("cruciblecraft:pressure_valve")),
                "Pressure valve attached between two fluid pipes");
        helper.setBlock(WIRE_POS.relative(TORCH_SIDE), Blocks.AIR);
        helper.assertTrue(
                pipe.setCover(
                        TORCH_SIDE,
                        PipeCover.of("cruciblecraft:pressure_valve")),
                "Pressure valve refused a single-tank fluid pipe");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void euCableHostsPlateAndRefusesTorch(GameTestHelper helper) {
        CableBlock cableBlock = ModBlocks.electricalConductorBlock(
                "tin", MaterialPrefixes.CABLE).get();
        helper.setBlock(WIRE_POS, cableBlock.defaultBlockState());
        CableBlockEntity cable = helper.getBlockEntity(WIRE_POS);
        helper.assertTrue(
                cable.setCover(
                        TORCH_SIDE,
                        PipeCover.of("cruciblecraft:cover_blank")),
                "Blank cover refused EU cable");
        helper.assertTrue(
                cable.removeCover(TORCH_SIDE, null),
                "Could not pry blank cover from EU cable");
        PipeCover plate = PlateCovers.fromItem(
                MaterialLookup.stack("iron", MaterialPrefixes.PLATE));
        helper.assertTrue(
                cable.setCover(TORCH_SIDE, plate),
                "Plate cover refused EU cable");
        helper.assertFalse(
                cable.setCover(
                        TORCH_SIDE.getOpposite(),
                        PipeCover.of("cruciblecraft:redstone_torch")),
                "Torch cover attached to an EU cable");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void autoSwitchRequiresPossibleAndCanTick(
            GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity machine =
                OrdinaryClosureHostGameTests.place(
                        helper,
                        MACHINE_POS,
                        ModBlocks.OVEN.get(),
                        ModProcessingMachines.OVEN);
        PipeCover auto = PipeCover.of("cruciblecraft:controller_auto");
        helper.assertTrue(machine.canTick(), "Server machine could not tick");
        helper.assertFalse(
                MachineCoverBehaviors.canPlace(machine, COVER_SIDE, auto),
                "Auto switch installed without a usable recipe");
        machine.inventory().setStackInSlot(
                machine.spec().items().inputs().getFirst(),
                new ItemStack(Items.COBBLESTONE));
        helper.assertTrue(
                MachineCoverBehaviors.canPlace(machine, COVER_SIDE, auto),
                "Auto switch refused a machine that could run");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void selectorUsesFakeCircuit(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity assembler =
                OrdinaryClosureHostGameTests.place(
                        helper,
                        MACHINE_POS,
                        ModBlocks.ASSEMBLER.get(),
                        ModProcessingMachines.ASSEMBLER);
        int input = assembler.spec().items().inputs().getFirst();
        assembler.inventory().setStackInSlot(
                input, new ItemStack(Items.OAK_PLANKS));
        OrdinaryClosureHostGameTests.fillEnergy(helper, assembler);
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        assembler.duration() == 0,
                        "Oak planks selected a circuit recipe without a selector: "
                                + assembler.pausedReason()))
                .thenExecute(() -> {
                    helper.assertTrue(
                            assembler.setCover(
                                    COVER_SIDE,
                                    PipeCover.of("cruciblecraft:selector_tag")
                                            .withDisplay(0, 0)),
                            "Could not install selector tag");
                    OrdinaryClosureHostGameTests.fillEnergy(helper, assembler);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(
                            assembler.duration() > 0,
                            "Selector did not select the circuit-1 recipe: "
                                    + assembler.pausedReason());
                    assembler.runtime().processor().setProgress(
                            assembler.duration() - 1);
                    OrdinaryClosureHostGameTests.fillEnergy(helper, assembler);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(
                            assembler.spec().items().outputs().stream()
                                    .map(assembler.inventory()::getStackInSlot)
                                    .anyMatch(stack -> stack.is(Items.OAK_BUTTON)),
                            "Selector fake circuit did not produce oak_button: "
                                    + assembler.pausedReason());
                    helper.assertTrue(
                            assembler.spec().items().inputs().stream()
                                    .map(assembler.inventory()::getStackInSlot)
                                    .noneMatch(stack -> stack.is(
                                            ModItems.PROGRAMMED_CIRCUIT.get())),
                            "Fake circuit was written into inventory");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 800)
    public static void ventFillsCollectableAir(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity mixer =
                OrdinaryClosureHostGameTests.place(
                        helper,
                        MACHINE_POS,
                        ModBlocks.MIXER.get(),
                        ModProcessingMachines.MIXER);
        helper.assertTrue(
                mixer.setCover(
                        COVER_SIDE,
                        PipeCover.of("cruciblecraft:vent")),
                "Could not install vent");
        BlockPos neighbor = MACHINE_POS.relative(COVER_SIDE);
        helper.setBlock(neighbor, Blocks.COBBLESTONE);
        int wait = ventWait(helper);
        helper.startSequence()
                .thenIdle(wait)
                .thenExecute(() -> helper.assertTrue(
                        mixer.spec().fluids().inputs().stream().allMatch(
                                tank -> mixer.tanks().get(tank.index())
                                        .getFluid().isEmpty()),
                        "Vent filled through a solid neighbor"))
                .thenExecute(() -> helper.setBlock(neighbor, Blocks.AIR))
                .thenIdle(360)
                .thenExecute(() -> {
                    FluidStack filled = mixer.spec().fluids().inputs().stream()
                            .map(tank -> mixer.tanks().get(tank.index()).getFluid())
                            .filter(stack -> !stack.isEmpty())
                            .findFirst()
                            .orElse(FluidStack.EMPTY);
                    helper.assertTrue(
                            !filled.isEmpty()
                                    && filled.is(ModFluids.materialFluid("air")
                                            .orElseThrow())
                                    && filled.getAmount() > 0,
                            "Vent did not fill collectable overworld air");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void displayVisualUsesGt6Layout(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity machine =
                OrdinaryClosureHostGameTests.place(
                        helper,
                        MACHINE_POS,
                        ModBlocks.OVEN.get(),
                        ModProcessingMachines.OVEN);
        machine.inventory().setStackInSlot(
                machine.spec().items().inputs().getFirst(),
                new ItemStack(Items.COBBLESTONE));
        helper.assertTrue(
                machine.setCover(
                        COVER_SIDE,
                        PipeCover.of("cruciblecraft:controller_display")),
                "Could not install status display");
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    PipeCover cover = machine.covers()
                            .get(COVER_SIDE)
                            .orElseThrow();
                    int visual = cover.config().visual();
                    helper.assertTrue(
                            cover.config().redstone() == 0,
                            "Status bits were packed into redstone");
                    helper.assertTrue(
                            MachineCoverVisuals.displayLightOn(visual, 0)
                                    && MachineCoverVisuals.displayLightPresent(
                                            visual, 0)
                                    && MachineCoverVisuals.displayLightPresent(
                                            visual, 3)
                                    && MachineCoverVisuals.displayStyle(visual)
                                            == 0,
                            "Display visual was not the GT6 possible/on layout: "
                                    + visual);
                    helper.assertTrue(
                            MachineCoverBehaviors.onTool(
                                    machine, COVER_SIDE, ToolAction.CHISEL),
                            "Chisel did not cycle the display skin");
                    int next = machine.covers()
                            .get(COVER_SIDE)
                            .orElseThrow()
                            .config()
                            .visual();
                    helper.assertTrue(
                            MachineCoverVisuals.displayStyle(next) == 1
                                    && MachineCoverVisuals.displaySkin(next)
                                            .equals("top"),
                            "Chisel did not move the GT6 visual style bit");
                })
                .thenSucceed();
    }

    private static int ventWait(GameTestHelper helper) {
        int target = 30 + 60 * COVER_SIDE.ordinal();
        int phase = (int) Math.floorMod(
                helper.getLevel().getGameTime(), 360L);
        int wait = Math.floorMod(target - phase, 360);
        return wait < 2 ? wait + 360 : wait + 2;
    }

    private static ItemStack craft(
            GameTestHelper helper,
            int width,
            int height,
            List<ItemStack> slots) {
        CraftingInput input = CraftingInput.of(width, height, slots);
        return helper.getLevel()
                .getRecipeManager()
                .getRecipeFor(
                        RecipeType.CRAFTING,
                        input,
                        helper.getLevel())
                .map(holder -> holder.value().assemble(
                        input,
                        helper.getLevel().registryAccess()))
                .orElse(ItemStack.EMPTY);
    }

    private static net.minecraft.world.level.block.state.BlockState redstoneWire(
            RedstoneWireKind kind, Direction... sides) {
        var state = ModBlocks.redstoneWireCatalogById()
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
