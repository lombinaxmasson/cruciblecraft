package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.AbstractPipeBlock;
import com.masson.cruciblecraft.content.block.CableBlock;
import com.masson.cruciblecraft.content.block.FluidPipeBlock;
import com.masson.cruciblecraft.content.block.Gt6StyleConnections;
import com.masson.cruciblecraft.content.block.ItemPipeBlock;
import com.masson.cruciblecraft.content.block.RedstoneWireBlock;
import com.masson.cruciblecraft.content.blockentity.CableBlockEntity;
import com.masson.cruciblecraft.content.item.CableBlockItem;
import com.masson.cruciblecraft.content.item.RedstoneWireBlockItem;
import com.masson.cruciblecraft.content.redstonewire.RedstoneWireKind;
import com.masson.cruciblecraft.energy.cable.CableLoadState;
import com.masson.cruciblecraft.energy.cable.ElectricalConductorCatalog;
import com.masson.cruciblecraft.logistics.pipe.PipeCatalog;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated GT6 EU wire/cable runtime. Run with
 * {@code -PwaveRecipes=content/gt6-eu-wire-cable-runtime}.
 */
@GameTestHolder(EuWireCableRuntimeGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class EuWireCableRuntimeGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_content_gt6_eu_wire_cable_runtime";
    private static final String TEMPLATE = "empty";

    private EuWireCableRuntimeGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void electricWireGt01IsCableBlock(GameTestHelper helper) {
        helper.assertTrue(
                ElectricalConductorCatalog.wires().size()
                        == ElectricalConductorCatalog.EXPECTED_WIRE_BLOCKS
                        && ElectricalConductorCatalog.cables().size()
                                == ElectricalConductorCatalog.EXPECTED_CABLE_BLOCKS,
                "conductor census drifted: wires="
                        + ElectricalConductorCatalog.wires().size()
                        + " cables="
                        + ElectricalConductorCatalog.cables().size());
        Item tinWire = ModItems.materialItem("tin", MaterialPrefixes.WIRE).get();
        helper.assertTrue(
                tinWire instanceof CableBlockItem
                        && ModBlocks.electricalConductorBlock(
                                        "tin", MaterialPrefixes.WIRE)
                                .get()
                                instanceof CableBlock,
                "tin wireGt01 is not a live CableBlockItem");
        Item goldWire = ModItems.materialItem("gold", MaterialPrefixes.WIRE).get();
        helper.assertTrue(
                goldWire instanceof CableBlockItem,
                "already-shared gold wireGt01 lost CableBlockItem");
        Item superconductor = ModItems.materialItem(
                "superconductor", MaterialPrefixes.WIRE).get();
        var scSpec = ElectricalConductorCatalog.require(
                "superconductor", MaterialPrefixes.WIRE);
        helper.assertTrue(
                superconductor instanceof CableBlockItem
                        && scSpec.electrical().maxVoltage() == 8589934592L
                        && scSpec.electrical().maxAmperage() == 4L
                        && scSpec.electrical().lossPerMeter() == 1L
                        && !ElectricalConductorCatalog.contains(
                                "superconductor", MaterialPrefixes.CABLE),
                "superconductor wireGt01 is not the GT6 V[15]/4A/loss-1 conductor");
        CableBlock tin = conductor("tin", MaterialPrefixes.WIRE);
        BlockPos euPos = new BlockPos(2, 2, 2);
        helper.setBlock(euPos, conductorState(tin, Direction.EAST));
        helper.setBlock(
                new BlockPos(3, 2, 2),
                pipeState(
                        (FluidPipeBlock) ModBlocks.pipeBlock(
                                "copper",
                                MaterialPrefixes.FLUID_PIPE,
                                PipeCatalog.Kind.FLUID).get(),
                        Direction.WEST));
        helper.setBlock(
                new BlockPos(2, 2, 3),
                pipeState(
                        (ItemPipeBlock) ModBlocks.pipeBlock(
                                "brass",
                                MaterialPrefixes.ITEM_PIPE,
                                PipeCatalog.Kind.ITEM).get(),
                        Direction.NORTH));
        RedstoneWireBlock redstone =
                (RedstoneWireBlock) ModBlocks.redstoneWireBlocksById()
                        .get(RedstoneWireKind.RED_ALLOY.id())
                        .get();
        helper.setBlock(new BlockPos(2, 2, 1), redstone.defaultBlockState());
        BlockState euState = helper.getBlockState(euPos);
        helper.assertTrue(
                !Gt6StyleConnections.sameNetwork(
                        euState, helper.getBlockState(new BlockPos(3, 2, 2))),
                "EU cable joined the fluid pipe network");
        helper.assertTrue(
                !Gt6StyleConnections.sameNetwork(
                        euState, helper.getBlockState(new BlockPos(2, 2, 3))),
                "EU cable joined the item pipe network");
        helper.assertTrue(
                !Gt6StyleConnections.sameNetwork(
                        euState, helper.getBlockState(new BlockPos(2, 2, 1))),
                "EU cable joined the redstone wire network");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void higherWireGaugesArePlaceableOrExplicitlyUpgrade(
            GameTestHelper helper) {
        Item doubleWire = ModItems.materialItem(
                "tin", MaterialPrefixes.DOUBLE_WIRE).get();
        Item hexa = ModItems.materialItem(
                "tin", MaterialPrefixes.HEXADECUPLE_WIRE).get();
        Item goldDouble = ModItems.materialItem(
                "gold", MaterialPrefixes.DOUBLE_WIRE).get();
        helper.assertTrue(
                doubleWire instanceof CableBlockItem
                        && hexa instanceof CableBlockItem
                        && goldDouble instanceof CableBlockItem
                        && ModItems.materialItem(
                                        "tin", MaterialPrefixes.SEPTUPLE_WIRE)
                                .get() instanceof CableBlockItem,
                "mapped higher wire gauges were not upgraded in place");
        helper.assertTrue(
                ElectricalConductorCatalog.require(
                                "tin", MaterialPrefixes.DOUBLE_WIRE)
                        .sourceSpecification()
                        .equals("wireGt02")
                        && ElectricalConductorCatalog.require(
                                        "tin", MaterialPrefixes.HEXADECUPLE_WIRE)
                                .electrical()
                                .maxAmperage()
                        == 16L
                        && ElectricalConductorCatalog.require(
                                        "tin", MaterialPrefixes.SEPTUPLE_WIRE)
                                .electrical()
                                .maxAmperage()
                        == 7L,
                "higher-gauge electrical specs drifted");
        ResourceLocation folded = ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft", "electric_wire/2x_tin_wire");
        helper.assertTrue(
                !BuiltInRegistries.ITEM.containsKey(folded),
                "folded tin wireGt02 dummy is still registered");
        helper.assertTrue(
                !BuiltInRegistries.ITEM.containsKey(
                        ResourceLocation.fromNamespaceAndPath(
                                "cruciblecraft", "electric_wire/7x_tin_wire")),
                "folded tin wireGt07 dummy is still registered");
        ResourceLocation ungated = ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft", "electric_wire/2x_blue_alloy_wire");
        Item leftover = BuiltInRegistries.ITEM.get(ungated);
        helper.assertTrue(
                BuiltInRegistries.ITEM.containsKey(ungated)
                        && !(leftover instanceof CableBlockItem)
                        && ModItems.materialItem(
                                        "blue_alloy", MaterialPrefixes.DOUBLE_WIRE)
                                .get() instanceof CableBlockItem,
                "blue_alloy wireGt02 dummy was folded onto a fake BlockItem");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void cableLossAndOverloadPerSpecification(
            GameTestHelper helper) {
        CableBlock wire = conductor("tin", MaterialPrefixes.WIRE);
        CableBlock cable = conductor("tin", MaterialPrefixes.CABLE);
        CableBlock doubleWire = conductor("tin", MaterialPrefixes.DOUBLE_WIRE);
        helper.assertTrue(
                wire.transportProperties().lossPerMeter() == 2L
                        && cable.transportProperties().lossPerMeter() == 1L
                        && doubleWire.transportProperties().maxAmperage() == 2L
                        && wire.bareWire()
                        && doubleWire.bareWire()
                        && !cable.bareWire(),
                "per-spec loss/insulation drifted");
        BlockPos wirePos = new BlockPos(4, 2, 5);
        BlockPos machinePos = wirePos.east();
        helper.setBlock(wirePos.below(), Blocks.STONE);
        helper.setBlock(
                machinePos,
                ModBlocks.STAINLESS_STEEL_ELECTROLYZER.get()
                        .defaultBlockState()
                        .setValue(
                                ProcessingMachineBlock.FACING,
                                Direction.EAST));
        helper.setBlock(
                wirePos,
                conductorState(wire, Direction.WEST, Direction.EAST));
        CableBlockEntity entity = helper.getBlockEntity(wirePos);
        helper.assertTrue(
                entity.insert(
                        EnergyType.ELECTRIC,
                        32L,
                        1L,
                        Direction.WEST,
                        true) == 1L,
                "rated tin wireGt01 rejected 1A");
        helper.assertTrue(
                entity.burnCounter() == 0,
                "rated tin wireGt01 burned at its amperage");
        for (int hit = 0; hit < CableLoadState.BURN_LIMIT; hit++) {
            helper.assertTrue(
                    entity.insert(
                            EnergyType.ELECTRIC,
                            32L,
                            2L,
                            Direction.WEST,
                            false) == 2L,
                    "overloaded tin wireGt01 did not accept offered amperage");
        }
        helper.assertTrue(
                entity.burnCounter() == 16,
                "tin wireGt01 did not retain overload hits");
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        helper.getBlockState(wirePos).is(Blocks.FIRE),
                        "overloaded tin wireGt01 did not burn to fire"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void redstoneMaterialsAreNotElectricalConductors(
            GameTestHelper helper) {
        helper.assertTrue(
                !ElectricalConductorCatalog.contains(
                        "red_alloy", MaterialPrefixes.WIRE)
                        && !ElectricalConductorCatalog.contains(
                                "signalum", MaterialPrefixes.WIRE)
                        && !ElectricalConductorCatalog.contains(
                                "lumium", MaterialPrefixes.WIRE),
                "redstone materials leaked into ElectricalConductorCatalog");
        Item redAlloy = ModItems.materialItem(
                "red_alloy", MaterialPrefixes.WIRE).get();
        helper.assertTrue(
                redAlloy instanceof RedstoneWireBlockItem
                        && !(redAlloy instanceof CableBlockItem)
                        && ModBlocks.redstoneWireBlocksById()
                                .get(RedstoneWireKind.RED_ALLOY.id())
                                .get() instanceof RedstoneWireBlock,
                "red_alloy wire became an EU CableBlock");
        helper.succeed();
    }

    private static CableBlock conductor(
            String material,
            com.masson.cruciblecraft.api.material.MaterialPrefix form) {
        return ModBlocks.electricalConductorBlock(material, form).get();
    }

    private static BlockState conductorState(
            CableBlock block, Direction... connections) {
        BlockState state = block.defaultBlockState();
        for (Direction direction : connections) {
            state = state.setValue(
                    CableBlock.PROPERTY_BY_DIRECTION.get(direction), true);
        }
        return state;
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
