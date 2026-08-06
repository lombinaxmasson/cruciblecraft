package com.masson.cruciblecraft.gametest;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.blockentity.CokeOvenBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CeramicMoldBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CrucibleBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CrusherBlockEntity;
import com.masson.cruciblecraft.content.blockentity.DynamoBlockEntity;
import com.masson.cruciblecraft.content.blockentity.LargeCentrifugeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.SteamEngineBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CableBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FluidDepositExtractorBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FuelGeneratorBlockEntity;
import com.masson.cruciblecraft.content.blockentity.GasCloudBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ItemPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.MultiblockPortBlockEntity;
import com.masson.cruciblecraft.content.blockentity.SubsurfaceFluidDepositBlockEntity;
import com.masson.cruciblecraft.content.block.CableBlock;
import com.masson.cruciblecraft.content.block.FluidPipeBlock;
import com.masson.cruciblecraft.content.block.ItemPipeBlock;
import com.masson.cruciblecraft.content.block.DynamoBlock;
import com.masson.cruciblecraft.content.block.ElectricMotorBlock;
import com.masson.cruciblecraft.content.block.FuelGeneratorBlock;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.block.ProcessingMachineInteractions;
import com.masson.cruciblecraft.content.block.RotationalAxleBlock;
import com.masson.cruciblecraft.content.block.RotationalGearboxBlock;
import com.masson.cruciblecraft.content.block.SteamEngineBlock;
import com.masson.cruciblecraft.content.item.CellItem;
import com.masson.cruciblecraft.content.item.ExtruderShapeCatalog;
import com.masson.cruciblecraft.compat.emi.ProcessingEmiRegistrationPlan;
import com.masson.cruciblecraft.content.blockentity.FireboxBlockEntity;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureCatalog;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PredicateKind;
import com.masson.cruciblecraft.heat.FuelDefinition;
import com.masson.cruciblecraft.heat.ItemHeat;
import com.masson.cruciblecraft.machine.ToolMaterialRules;
import com.masson.cruciblecraft.material.CellContentGate;
import com.masson.cruciblecraft.material.HydrocarbonRuntimePolicy;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.MaterialComponentPolicies;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.masson.cruciblecraft.logistics.pipe.PipeCatalog;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCover;
import com.masson.cruciblecraft.logistics.pipe.fluid.FluidPipeFailureState;
import com.masson.cruciblecraft.logistics.pipe.fluid
        .FluidPipeFailureState.Failure;
import com.masson.cruciblecraft.machine.processing.MachineExecutionPlan;
import com.masson.cruciblecraft.machine.processing.MachineIdentityPolicy;
import com.masson.cruciblecraft.machine.processing.MachineTransaction;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.GTRecipeQuery;
import com.masson.cruciblecraft.recipe.gt.ItemInputAction;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.recipe.gt.ExtruderRecipeFamilyProvider;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModCapabilities;
import com.masson.cruciblecraft.registry.ModComponents;
import com.masson.cruciblecraft.registry.ModFluids;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.registry.ModMachineIdentityMigrations;
import com.masson.cruciblecraft.registry.ModMultiblockControllers;
import com.masson.cruciblecraft.registry.ModProcessingMachines;
import com.masson.cruciblecraft.registry.ModRecipeMaps;
import com.masson.cruciblecraft.worldgen.LargeVeinConfiguration;
import com.masson.cruciblecraft.worldgen.LargeVeinLayout;
import com.masson.cruciblecraft.worldgen.OreHostVariantCatalog.Host;
import com.masson.cruciblecraft.worldgen.SubsurfaceFluidDepositConfiguration;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Final-gate block-world coverage. Recipes are those loaded by the production
 * reload listener; no test-only RecipeMaps or processing hosts are used.
 */
@GameTestHolder(CrucibleCraft.MODID)
@PrefixGameTestTemplate(false)
public final class CrucibleCraftGameTests {
    private static final String TEMPLATE = "empty";

    private CrucibleCraftGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void cableChainAppliesExactPerBlockLoss(
            GameTestHelper helper) {
        CableBlock tinCable = ModBlocks.electricalConductorBlock(
                "tin", MaterialPrefixes.CABLE).get();
        BlockPos first = new BlockPos(3, 2, 5);
        BlockPos machinePos = first.east(3);
        helper.setBlock(
                machinePos,
                ModBlocks.STAINLESS_STEEL_ELECTROLYZER.get()
                        .defaultBlockState()
                        .setValue(
                                ProcessingMachineBlock.FACING,
                                Direction.EAST));
        helper.setBlock(
                first.west(),
                ModBlocks.BRONZE_DYNAMO.get().defaultBlockState()
                        .setValue(DynamoBlock.FACING, Direction.EAST));
        for (int offset = 0; offset < 3; offset++) {
            helper.setBlock(
                    first.east(offset),
                    conductorState(
                            tinCable,
                            Direction.WEST,
                            Direction.EAST));
        }
        CableBlockEntity entry = helper.getBlockEntity(first);
        ConfiguredProcessingMachineBlockEntity machine =
                helper.getBlockEntity(machinePos);

        helper.assertTrue(
                entry.insert(
                        EnergyType.ELECTRIC,
                        32L,
                        1L,
                        Direction.WEST,
                        true) == 1L,
                "Three-block cable chain rejected simulation");
        helper.assertTrue(
                machine.stored(EnergyType.ELECTRIC) == 0L,
                "Cable simulation mutated terminal energy");
        helper.assertTrue(
                entry.insert(
                        EnergyType.ELECTRIC,
                        32L,
                        1L,
                        Direction.WEST,
                        false) == 1L,
                "Three-block cable chain rejected execution");
        helper.assertTrue(
                machine.stored(EnergyType.ELECTRIC) == 29L,
                "Cable chain did not subtract exact loss for all three blocks");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void cableConnectionsTrackPlacedAndRemovedNeighbors(
            GameTestHelper helper) {
        CableBlock tinCable = ModBlocks.electricalConductorBlock(
                "tin", MaterialPrefixes.CABLE).get();
        BlockPos first = new BlockPos(4, 2, 5);
        BlockPos second = first.east();
        helper.setBlock(first, tinCable.defaultBlockState());
        helper.setBlock(second, tinCable.defaultBlockState());
        helper.assertTrue(
                CableBlock.isConnected(
                        helper.getBlockState(first), Direction.EAST)
                        && CableBlock.isConnected(
                                helper.getBlockState(second),
                                Direction.WEST),
                "Adjacent cables did not form a reciprocal connection");

        helper.setBlock(second, Blocks.AIR);
        helper.assertTrue(
                !CableBlock.isConnected(
                        helper.getBlockState(first), Direction.EAST),
                "Removed cable left a stale connection");

        helper.setBlock(
                second,
                ModBlocks.STAINLESS_STEEL_ELECTROLYZER.get()
                        .defaultBlockState()
                        .setValue(
                                ProcessingMachineBlock.FACING,
                                Direction.EAST));
        helper.assertTrue(
                CableBlock.isConnected(
                        helper.getBlockState(first), Direction.EAST),
                "Electric machine placement did not connect the cable");
        helper.setBlock(second, Blocks.AIR);
        helper.assertTrue(
                !CableBlock.isConnected(
                        helper.getBlockState(first), Direction.EAST),
                "Removed machine left a stale cable connection");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void dynamoFeedsElectrolyzerThroughThreeCables(
            GameTestHelper helper) {
        CableBlock tinCable = ModBlocks.electricalConductorBlock(
                "tin", MaterialPrefixes.CABLE).get();
        BlockPos dynamoPos = new BlockPos(2, 2, 5);
        BlockPos first = dynamoPos.east();
        BlockPos machinePos = first.east(3);
        helper.setBlock(
                dynamoPos,
                ModBlocks.BRONZE_DYNAMO.get().defaultBlockState()
                        .setValue(DynamoBlock.FACING, Direction.EAST));
        helper.setBlock(
                machinePos,
                ModBlocks.ELECTROLYZER.get().defaultBlockState()
                        .setValue(
                                ProcessingMachineBlock.FACING,
                                Direction.EAST));
        for (int offset = 0; offset < 3; offset++) {
            helper.setBlock(
                    first.east(offset),
                    conductorState(
                            tinCable,
                            Direction.WEST,
                            Direction.EAST));
        }
        for (int offset = 0; offset < 3; offset++) {
            helper.setBlock(
                    first.east(offset),
                    conductorState(
                            tinCable,
                            Direction.WEST,
                            Direction.EAST));
        }
        DynamoBlockEntity dynamo = helper.getBlockEntity(dynamoPos);
        ConfiguredProcessingMachineBlockEntity machine =
                helper.getBlockEntity(machinePos);
        GTRecipe recipe = requireRecipe(
                ModRecipeMaps.ELECTROLYZER, "t5/electrolyzer/salt");
        loadRecipeInputs(machine, recipe);
        helper.assertTrue(
                dynamo.insert(
                        EnergyType.KINETIC_ROTATION,
                        24L,
                        1L,
                        Direction.WEST,
                        false) == 1L,
                "Dynamo fixture rejected one kinetic packet");
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        dynamo.stored(
                                        EnergyType.KINETIC_ROTATION)
                                == 0L
                                && dynamo.stored(EnergyType.ELECTRIC) == 0L
                                && machine.workProgressLong() > 0L,
                        "Dynamo -> cable -> Electrolyzer did not reach "
                                + "real recipe execution: stored="
                                + machine.stored(EnergyType.ELECTRIC)
                                + ", status="
                                + machine.pausedReason()))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void t17bElectrolyzerCableEndpointsRespectEveryTierWindow(
            GameTestHelper helper) {
        CableBlock cable = ModBlocks.electricalConductorBlock(
                "copper", MaterialPrefixes.CABLE).get();
        List<Block> blocks = List.of(
                ModBlocks.ELECTROLYZER.get(),
                ModBlocks.ALUMINIUM_ELECTROLYZER.get(),
                ModBlocks.STAINLESS_STEEL_ELECTROLYZER.get());
        for (int index = 0; index < blocks.size(); index++) {
            BlockPos cablePos = new BlockPos(3 + index * 5, 2, 5);
            BlockPos machinePos = cablePos.east();
            helper.setBlock(
                    machinePos,
                    blocks.get(index).defaultBlockState().setValue(
                            ProcessingMachineBlock.FACING,
                            Direction.EAST));
            helper.setBlock(
                    cablePos,
                    conductorState(
                            cable, Direction.WEST, Direction.EAST));
            ConfiguredProcessingMachineBlockEntity machine =
                    helper.getBlockEntity(machinePos);
            CableBlockEntity endpoint = helper.getBlockEntity(cablePos);
            long expectedUnits = switch (index) {
                case 0 -> machine.variant().tier().inputMinimum();
                case 1 -> machine.variant().tier().inputNominal();
                case 2 -> machine.variant().tier().inputMaximum();
                default -> throw new IllegalStateException();
            };
            long offeredSize = Math.addExact(
                    expectedUnits,
                    cable.conductor().electrical().lossPerMeter());
            helper.assertTrue(
                    endpoint.insert(
                            EnergyType.ELECTRIC,
                            offeredSize,
                            1L,
                            Direction.WEST,
                            false) == 1L,
                    "Tier " + (index + 1)
                            + " Electrolyzer cable endpoint rejected "
                            + expectedUnits + " EU");
            helper.assertTrue(
                    machine.stored(EnergyType.ELECTRIC) == expectedUnits
                            && !machine.pausedReason().equals("overcharged"),
                    "Tier " + (index + 1)
                            + " Electrolyzer endpoint violated its input window");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void cableBranchUsesStableGreedyDirectionOrder(
            GameTestHelper helper) {
        CableBlock tinCable = ModBlocks.electricalConductorBlock(
                "tin", MaterialPrefixes.CABLE).get();
        BlockPos cablePos = new BlockPos(6, 2, 6);
        BlockPos northMachine = cablePos.north();
        BlockPos southMachine = cablePos.south();
        helper.setBlock(
                northMachine,
                ModBlocks.ELECTROLYZER.get().defaultBlockState()
                        .setValue(
                                ProcessingMachineBlock.FACING,
                                Direction.NORTH));
        helper.setBlock(
                southMachine,
                ModBlocks.ELECTROLYZER.get().defaultBlockState()
                        .setValue(
                                ProcessingMachineBlock.FACING,
                                Direction.SOUTH));
        helper.setBlock(
                cablePos,
                conductorState(
                        tinCable,
                        Direction.WEST,
                        Direction.NORTH,
                        Direction.SOUTH));
        CableBlockEntity cable = helper.getBlockEntity(cablePos);
        ConfiguredProcessingMachineBlockEntity north =
                helper.getBlockEntity(northMachine);
        ConfiguredProcessingMachineBlockEntity south =
                helper.getBlockEntity(southMachine);

        helper.assertTrue(
                cable.insert(
                        EnergyType.ELECTRIC,
                        32L,
                        1L,
                        Direction.WEST,
                        false) == 1L,
                "Branched cable rejected one packet");
        helper.assertTrue(
                north.stored(EnergyType.ELECTRIC) == 31L,
                "Stable NORTH-first branch did not receive the packet");
        helper.assertTrue(
                south.stored(EnergyType.ELECTRIC) == 0L,
                "Greedy branch duplicated a packet");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void cableRingVisitsEachNodeOnce(
            GameTestHelper helper) {
        CableBlock tinCable = ModBlocks.electricalConductorBlock(
                "tin", MaterialPrefixes.CABLE).get();
        BlockPos a = new BlockPos(4, 2, 5);
        BlockPos b = a.east();
        BlockPos c = b.south();
        BlockPos d = a.south();
        BlockPos machinePos = c.east();
        helper.setBlock(
                machinePos,
                ModBlocks.ELECTROLYZER.get().defaultBlockState()
                        .setValue(
                                ProcessingMachineBlock.FACING,
                                Direction.EAST));
        helper.setBlock(
                a,
                conductorState(
                        tinCable,
                        Direction.WEST,
                        Direction.EAST,
                        Direction.SOUTH));
        helper.setBlock(
                b,
                conductorState(
                        tinCable,
                        Direction.WEST,
                        Direction.SOUTH));
        helper.setBlock(
                c,
                conductorState(
                        tinCable,
                        Direction.NORTH,
                        Direction.WEST,
                        Direction.EAST));
        helper.setBlock(
                d,
                conductorState(
                        tinCable,
                        Direction.NORTH,
                        Direction.EAST));
        helper.setBlock(
                a,
                conductorState(
                        tinCable,
                        Direction.WEST,
                        Direction.EAST,
                        Direction.SOUTH));
        helper.setBlock(
                b,
                conductorState(
                        tinCable,
                        Direction.WEST,
                        Direction.SOUTH));
        helper.setBlock(
                c,
                conductorState(
                        tinCable,
                        Direction.NORTH,
                        Direction.WEST,
                        Direction.EAST));
        helper.setBlock(
                d,
                conductorState(
                        tinCable,
                        Direction.NORTH,
                        Direction.EAST));
        CableBlockEntity entry = helper.getBlockEntity(a);
        ConfiguredProcessingMachineBlockEntity machine =
                helper.getBlockEntity(machinePos);

        helper.assertTrue(
                entry.insert(
                        EnergyType.ELECTRIC,
                        32L,
                        1L,
                        Direction.WEST,
                        false) == 1L,
                "Cable ring failed to terminate at its endpoint");
        helper.assertTrue(
                machine.stored(EnergyType.ELECTRIC) == 29L,
                "Cable ring did not use the stable three-segment route");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void cableOverloadBurnsOnTheSafeFollowingTick(
            GameTestHelper helper) {
        CableBlock tinWire = ModBlocks.electricalConductorBlock(
                "tin", MaterialPrefixes.WIRE).get();
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
                conductorState(
                        tinWire, Direction.WEST, Direction.EAST));
        CableBlockEntity wire = helper.getBlockEntity(wirePos);
        for (int hit = 0;
                hit < com.masson.cruciblecraft.energy.cable
                        .CableLoadState.BURN_LIMIT;
                hit++) {
            helper.assertTrue(
                    wire.insert(
                            EnergyType.ELECTRIC,
                            32L,
                            2L,
                            Direction.WEST,
                            false) == 2L,
                    "Overloaded wire did not dissipate the full offered amperage");
        }
        helper.assertTrue(
                wire.burnCounter() == 16,
                "Wire did not retain all overload hits");
        helper.assertTrue(
                helper.getBlockState(wirePos).is(tinWire),
                "Wire burned during the network execution call");
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        helper.getBlockState(wirePos).is(Blocks.FIRE),
                        "Wire did not become observable fire"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void downstreamOverloadPropagatesOfferedAmperageUpstream(
            GameTestHelper helper) {
        CableBlock tinCable = ModBlocks.electricalConductorBlock(
                "tin", MaterialPrefixes.CABLE).get();
        BlockPos first = new BlockPos(4, 2, 5);
        BlockPos second = first.east();
        BlockPos machinePos = second.east();
        helper.setBlock(
                machinePos,
                ModBlocks.STAINLESS_STEEL_ELECTROLYZER.get()
                        .defaultBlockState()
                        .setValue(
                                ProcessingMachineBlock.FACING,
                                Direction.EAST));
        helper.setBlock(
                first,
                conductorState(
                        tinCable, Direction.WEST, Direction.EAST));
        helper.setBlock(
                second,
                conductorState(
                        tinCable, Direction.WEST, Direction.EAST));
        helper.setBlock(
                first,
                conductorState(
                        tinCable, Direction.WEST, Direction.EAST));
        helper.setBlock(
                second,
                conductorState(
                        tinCable, Direction.WEST, Direction.EAST));
        CableBlockEntity upstream = helper.getBlockEntity(first);
        CableBlockEntity downstream = helper.getBlockEntity(second);
        ConfiguredProcessingMachineBlockEntity machine =
                helper.getBlockEntity(machinePos);

        helper.assertTrue(
                upstream.insert(
                        EnergyType.ELECTRIC,
                        32L,
                        2L,
                        Direction.WEST,
                        false) == 2L,
                "Downstream overload did not report offered amperage");
        helper.assertTrue(
                machine.stored(EnergyType.ELECTRIC) == 60L,
                "Overload occurred before downstream delivery");
        helper.assertTrue(
                upstream.burnCounter() == 1
                        && downstream.burnCounter() == 1,
                "Downstream overload did not propagate load upstream");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void unusedAndCutoffCableBranchesDoNotAccumulateLoad(
            GameTestHelper helper) {
        CableBlock tinWire = ModBlocks.electricalConductorBlock(
                "tin", MaterialPrefixes.WIRE).get();
        BlockPos first = new BlockPos(4, 2, 5);
        for (int offset = 0; offset < 3; offset++) {
            helper.setBlock(
                    first.east(offset),
                    conductorState(
                            tinWire,
                            Direction.WEST,
                            Direction.EAST));
        }
        for (int offset = 0; offset < 3; offset++) {
            helper.setBlock(
                    first.east(offset),
                    conductorState(
                            tinWire,
                            Direction.WEST,
                            Direction.EAST));
        }
        CableBlockEntity entry = helper.getBlockEntity(first);
        helper.assertTrue(
                entry.insert(
                        EnergyType.ELECTRIC,
                        6L,
                        1L,
                        Direction.WEST,
                        false) == 0L,
                "Exact-loss cutoff unexpectedly accepted a packet");
        for (int offset = 0; offset < 3; offset++) {
            CableBlockEntity wire =
                    helper.getBlockEntity(first.east(offset));
            helper.assertTrue(
                    wire.transferredAmperes() == 0L
                            && wire.burnCounter() == 0,
                    "Unused or cutoff cable branch accumulated load");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 50)
    public static void onlyContactDamageBareWireShocksFromLastTick(
            GameTestHelper helper) {
        CableBlock tinWire = ModBlocks.electricalConductorBlock(
                "tin", MaterialPrefixes.WIRE).get();
        CableBlock grapheneWire = ModBlocks.electricalConductorBlock(
                "graphene", MaterialPrefixes.WIRE).get();
        CableBlock tinCable = ModBlocks.electricalConductorBlock(
                "tin", MaterialPrefixes.CABLE).get();
        BlockPos tinPos = new BlockPos(3, 2, 4);
        BlockPos graphenePos = new BlockPos(3, 2, 7);
        BlockPos cablePos = new BlockPos(8, 2, 4);
        for (var placement : List.of(
                Map.entry(tinPos, tinWire),
                Map.entry(graphenePos, grapheneWire),
                Map.entry(cablePos, tinCable))) {
            BlockPos machinePos = placement.getKey().east();
            helper.setBlock(
                    machinePos,
                    ModBlocks.ELECTROLYZER.get().defaultBlockState()
                            .setValue(
                                    ProcessingMachineBlock.FACING,
                                    Direction.EAST));
            helper.setBlock(
                    placement.getKey(),
                    conductorState(
                            placement.getValue(),
                            Direction.WEST,
                            Direction.EAST));
            CableBlockEntity conductor =
                    helper.getBlockEntity(placement.getKey());
            helper.assertTrue(
                    conductor.insert(
                            EnergyType.ELECTRIC,
                            32L,
                            1L,
                            Direction.WEST,
                            false) == 1L,
                    "Contact-damage fixture could not transmit");
        }
        helper.startSequence()
                .thenIdle(1)
                .thenExecute(() -> {
                    Pig pig = helper.spawn(EntityType.PIG, tinPos);
                    Pig graphenePig =
                            helper.spawn(EntityType.PIG, graphenePos);
                    Pig cablePig =
                            helper.spawn(EntityType.PIG, cablePos);
                    float pigHealth = pig.getHealth();
                    float grapheneHealth = graphenePig.getHealth();
                    float cableHealth = cablePig.getHealth();
                    ((CableBlockEntity) helper.getBlockEntity(tinPos))
                            .applyContactDamage(pig);
                    ((CableBlockEntity) helper.getBlockEntity(graphenePos))
                            .applyContactDamage(graphenePig);
                    ((CableBlockEntity) helper.getBlockEntity(cablePos))
                            .applyContactDamage(cablePig);
                    helper.assertTrue(
                            pig.getHealth() < pigHealth,
                            "Tin wire did not apply last-tick contact damage");
                    helper.assertTrue(
                            graphenePig.getHealth() == grapheneHealth,
                            "Graphene wire ignored source contact_damage=false");
                    helper.assertTrue(
                            cablePig.getHealth() == cableHealth,
                            "Insulated cable applied contact damage");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 160)
    public static void steamEnginePowersSifterThroughKu(
            GameTestHelper helper) {
        BlockPos enginePos = new BlockPos(3, 2, 5);
        BlockPos machinePos = enginePos.east();
        helper.setBlock(
                enginePos,
                ModBlocks.BRONZE_STEAM_ENGINE.get().defaultBlockState()
                        .setValue(SteamEngineBlock.FACING, Direction.EAST));
        helper.setBlock(
                machinePos,
                ModBlocks.SIFTER.get().defaultBlockState()
                        .setValue(ProcessingMachineBlock.FACING, Direction.EAST));
        SteamEngineBlockEntity engine = helper.getBlockEntity(enginePos);
        ConfiguredProcessingMachineBlockEntity sifter =
                helper.getBlockEntity(machinePos);
        RecipeMap.Entry source = ModRecipeMaps.SIFTER.entries()
                .getFirst();
        RecipeMap.Match match = ModRecipeMaps.SIFTER.findMatch(
                queryFor(source.recipe())).orElseThrow();
        helper.assertTrue(
                match.id().equals(source.id()),
                "Sifter query resolved the wrong source recipe");
        loadRecipeInputs(sifter, match.recipe());
        IFluidHandler steamInput = engine.fluids(Direction.NORTH);
        helper.assertTrue(steamInput != null, "Steam engine input capability missing");
        helper.assertTrue(
                steamInput.fill(
                        new FluidStack(ModFluids.STEAM_SOURCE.get(), 16_000),
                        IFluidHandler.FluidAction.EXECUTE) == 16_000,
                "Could not prime the T12 KU vertical");

        helper.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    helper.assertTrue(
                            sifter.progress() > 0,
                            "Steam-engine KU did not advance the sifter");
                    helper.assertTrue(
                            sifter.handles(
                                    EnergyType.KINETIC_PUSH,
                                    Direction.WEST)
                                    && !sifter.handles(
                                            EnergyType.KINETIC_ROTATION,
                                            Direction.WEST),
                            "Sifter did not preserve its KU-only identity");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void anyRubberTagExecutesCableRecipe(
            GameTestHelper helper) {
        ItemStack tinWire = material(
                "tin", MaterialPrefixes.WIRE, 1);
        ItemStack rubberPlate = material(
                "rubber", MaterialPrefixes.PLATE, 1);
        RecipeMap.Match match = ModRecipeMaps.ASSEMBLER.findMatch(
                GTRecipeQuery.items(tinWire, rubberPlate))
                .orElse(null);
        helper.assertTrue(
                match != null
                        && match.id().getPath().equals(
                                "assembler/wire_and_rubber_to_cable/tin")
                        && match.recipe().itemOutputs().size() == 1
                        && match.recipe().itemOutputs().getFirst().is(
                                ModItems.materialItem(
                                        "tin",
                                        MaterialPrefixes.CABLE).get()),
                "ANY.Rubber plate tag did not resolve the original tin cable "
                        + "recipe");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void materialTagsDriveLiveMortarRules(
            GameTestHelper helper) {
        ItemStack ironIngot = material("iron", MaterialPrefixes.INGOT, 1);
        ItemStack amberGem = material("amber", MaterialPrefixes.GEM, 1);
        ItemStack cinnabarGem = material(
                "cinnabar", MaterialPrefixes.GEM, 1);
        RecipeMap.Match iron = ModRecipeMaps.MORTAR.findMatch(
                GTRecipeQuery.items(ironIngot.copy())).orElse(null);
        RecipeMap.Match amber = ModRecipeMaps.MORTAR.findMatch(
                GTRecipeQuery.items(amberGem.copy())).orElse(null);
        RecipeMap.Match cinnabar = ModRecipeMaps.MORTAR.findMatch(
                GTRecipeQuery.items(cinnabarGem.copy())).orElse(null);
        helper.assertTrue(
                iron != null
                        && iron.id().getPath().equals(
                                "t7/mortar/ingot_to_dust/iron"),
                "MORTAR_GRINDABLE iron did not resolve the live ingot rule");
        helper.assertTrue(
                amber != null
                        && amber.id().getPath().equals(
                                "t7/mortar/gem_to_dust/amber"),
                "MORTAR_GRINDABLE amber did not resolve the live gem rule");
        helper.assertTrue(
                cinnabar != null
                        && cinnabar.id().getPath().equals(
                                "t7/mortar/gem_to_dust/cinnabar"),
                "MORTAR_GRINDABLE cinnabar did not resolve the live gem rule");
        helper.assertTrue(
                ModRecipeMaps.MORTAR.findMatch(GTRecipeQuery.items(
                        material("ruby", MaterialPrefixes.GEM, 1))).isEmpty(),
                "A gem without MORTAR_GRINDABLE incorrectly matched");
        helper.assertTrue(
                ModRecipeMaps.MORTAR.entries().size() == 691,
                "T7 mortar publication count drifted from 691");

        BlockPos ironPos = new BlockPos(3, 2, 3);
        BlockPos amberPos = new BlockPos(8, 2, 3);
        BlockPos cinnabarPos = new BlockPos(13, 2, 3);
        ConfiguredProcessingMachineBlockEntity ironMortar =
                placeConfigured(
                        helper,
                        ironPos,
                        ModBlocks.MORTAR.get(),
                        ModProcessingMachines.MORTAR);
        ConfiguredProcessingMachineBlockEntity amberMortar =
                placeConfigured(
                        helper,
                        amberPos,
                        ModBlocks.MORTAR.get(),
                        ModProcessingMachines.MORTAR);
        ConfiguredProcessingMachineBlockEntity cinnabarMortar =
                placeConfigured(
                        helper,
                        cinnabarPos,
                        ModBlocks.MORTAR.get(),
                        ModProcessingMachines.MORTAR);
        ironMortar.inventory().setStackInSlot(
                ironMortar.spec().items().inputs().getFirst(),
                ironIngot);
        amberMortar.inventory().setStackInSlot(
                amberMortar.spec().items().inputs().getFirst(),
                amberGem);
        cinnabarMortar.inventory().setStackInSlot(
                cinnabarMortar.spec().items().inputs().getFirst(),
                cinnabarGem);
        fillKuCapability(helper, ironMortar);
        fillKuCapability(helper, amberMortar);
        fillKuCapability(helper, cinnabarMortar);

        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(
                            ironMortar.duration() == iron.recipe().duration()
                                    && amberMortar.duration()
                                    == amber.recipe().duration()
                                    && cinnabarMortar.duration()
                                    == cinnabar.recipe().duration(),
                            "Mortar machines did not select the declared "
                                    + "tag-driven duration");
                    forceLastTick(helper, ironMortar);
                    forceLastTick(helper, amberMortar);
                    forceLastTick(helper, cinnabarMortar);
                    fillKuCapability(helper, ironMortar);
                    fillKuCapability(helper, amberMortar);
                    fillKuCapability(helper, cinnabarMortar);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    int ironOutput =
                            ironMortar.spec().items().outputs().getFirst();
                    int amberOutput =
                            amberMortar.spec().items().outputs().getFirst();
                    int cinnabarOutput =
                            cinnabarMortar.spec().items().outputs().getFirst();
                    helper.assertTrue(
                            ItemStack.isSameItemSameComponents(
                                    ironMortar.inventory()
                                            .getStackInSlot(ironOutput),
                                    material(
                                            "iron",
                                            MaterialPrefixes.DUST,
                                            1)),
                            "Iron mortar rule did not transfer dust output");
                    helper.assertTrue(
                            ItemStack.isSameItemSameComponents(
                                    amberMortar.inventory()
                                            .getStackInSlot(amberOutput),
                                    material(
                                            "amber",
                                            MaterialPrefixes.DUST,
                                            1)),
                            "Amber mortar rule did not transfer dust output");
                    helper.assertTrue(
                            ItemStack.isSameItemSameComponents(
                                    cinnabarMortar.inventory()
                                            .getStackInSlot(cinnabarOutput),
                                    material(
                                            "cinnabar",
                                            MaterialPrefixes.DUST,
                                            1)),
                            "Cinnabar mortar rule did not transfer dust output");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void blockedElectrolyzerOutputPreservesInput(GameTestHelper helper) {
        BlockPos pos = new BlockPos(5, 2, 5);
        helper.setBlock(
                pos,
                ModBlocks.ALUMINIUM_ELECTROLYZER.get().defaultBlockState()
                        .setValue(ProcessingMachineBlock.FACING, Direction.NORTH));
        ConfiguredProcessingMachineBlockEntity electrolyzer =
                helper.getBlockEntity(pos);
        RecipeMap.Entry tronaSource = ModRecipeMaps.ELECTROLYZER.entries().stream()
                .filter(entry -> entry.id().getPath().equals(
                        "t5/electrolyzer/trona"))
                .findFirst()
                .orElseThrow();
        ItemStack tronaInput =
                tronaSource.recipe().itemInputs().getFirst().getItems()[0].copy();
        tronaInput.setCount(tronaSource.recipe().itemInputCounts().getFirst());
        RecipeMap.Match trona = ModRecipeMaps.ELECTROLYZER.findMatch(
                GTRecipeQuery.items(tronaInput)).orElseThrow();
        helper.assertTrue(
                trona.id().getPath().equals("t5/electrolyzer/trona"),
                "Trona query resolved the wrong T5 recipe");
        loadRecipeInputs(electrolyzer, trona.recipe());
        int inputSlot = electrolyzer.spec().items().inputs().getFirst();
        int originalInput = electrolyzer.inventory().getStackInSlot(inputSlot).getCount();
        List<FluidStack> blockingFluids = electrolyzer.spec().fluids().outputs()
                .stream()
                .map(tank -> new FluidStack(
                        ModFluids.chemical("fluorine").orElseThrow().source().get(),
                        tank.capacity()))
                .toList();
        for (int index = 0; index < blockingFluids.size(); index++) {
            int tank = electrolyzer.spec().fluids().outputs().get(index).index();
            electrolyzer.tanks().get(tank).setFluid(blockingFluids.get(index).copy());
        }
        long accepted = electrolyzer.insert(
                EnergyType.ELECTRIC,
                128L,
                32L,
                Direction.SOUTH,
                false);
        helper.assertTrue(accepted > 0, "Could not power blocked electrolyzer");

        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> forceLastTick(
                        helper, electrolyzer))
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(
                            electrolyzer.inventory().getStackInSlot(inputSlot).getCount()
                                    == originalInput,
                            "Blocked fluid output consumed T5 item input");
                    for (int index = 0; index < blockingFluids.size(); index++) {
                        int tank = electrolyzer.spec().fluids().outputs().get(index).index();
                        helper.assertTrue(
                                sameFluidAmount(
                                        electrolyzer.tanks().get(tank).getFluid(),
                                        blockingFluids.get(index)),
                                "Blocked fluid output mutated existing tank " + index);
                    }
                    helper.assertTrue(
                            !hasAnyOutput(electrolyzer),
                            "Blocked fluid output still committed item outputs");
                    helper.assertTrue(
                            electrolyzer.pausedReason().equals(
                                    "output_blocked"),
                            "Blocked Electrolyzer output did not expose "
                                    + "output_blocked: "
                                    + electrolyzer.pausedReason());
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void portableTankMakesFluidOutputRecipeRepeatable(
            GameTestHelper helper) {
        BlockPos pos = new BlockPos(5, 2, 5);
        helper.setBlock(pos.below(), ModBlocks.FIREBOX.get());
        ConfiguredProcessingMachineBlockEntity drying = placeConfigured(
                helper, pos, ModBlocks.DRYING.get(), ModProcessingMachines.DRYING);
        FireboxBlockEntity firebox = helper.getBlockEntity(pos.below());
        helper.assertTrue(
                firebox.addFuel(FuelDefinition.COAL_COKE),
                "Could not fuel HU drying route");
        RecipeMap.Entry source = ModRecipeMaps.DRYING.entries().stream()
                .filter(entry -> entry.id().getPath().equals("t5/drying/mirabilite"))
                .findFirst()
                .orElseThrow();
        GTRecipe recipe = source.recipe();
        helper.assertTrue(
                recipe.fluidOutputs().size() == 1
                        && recipe.fluidOutputs().getFirst().getAmount() == 30_000,
                "Mirabilite no longer exercises the large fluid-output route");
        ItemStack portableTank = new ItemStack(ModItems.PORTABLE_FLUID_TANK.get());
        var tankHandler = portableTank.getCapability(Capabilities.FluidHandler.ITEM);
        helper.assertTrue(tankHandler != null, "Portable tank item capability missing");
        loadRecipeInputs(drying, recipe);
        helper.assertTrue(
                drying.spec().energy().type() == EnergyType.HEAT
                        && drying.spec().energy().mode()
                                == ProcessingMachineSpec.EnergyMode.ADJACENT,
                "Drying route did not use adjacent HU");

        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> forceLastTick(helper, drying))
                .thenIdle(2)
                .thenExecute(() -> {
                    drainMachineIntoItem(
                            helper,
                            drying.playerDrainFluids(Direction.NORTH),
                            tankHandler);
                    loadRecipeInputs(drying, recipe);
                })
                .thenIdle(3)
                .thenExecute(() -> forceLastTick(helper, drying))
                .thenIdle(2)
                .thenExecute(() -> {
                    drainMachineIntoItem(
                            helper,
                            drying.playerDrainFluids(Direction.NORTH),
                            tankHandler);
                    FluidStack stored = tankHandler.getFluidInTank(0);
                    helper.assertTrue(
                            stored.getAmount() == 60_000
                                    && FluidStack.isSameFluidSameComponents(
                                            stored, recipe.fluidOutputs().getFirst()),
                            "Portable tank did not conserve two repeated outputs: "
                                    + stored);
                    helper.assertTrue(
                            drying.tanks().get(
                                    drying.spec().fluids().outputs().getFirst().index())
                                    .getFluid().isEmpty(),
                            "Drying output remained blocked after player extraction");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void distillerySourceRouteIsRepeatable(
            GameTestHelper helper) {
        BlockPos distilleryPos = new BlockPos(5, 2, 5);
        helper.setBlock(distilleryPos.below(), ModBlocks.FIREBOX.get());
        ConfiguredProcessingMachineBlockEntity distillery = placeConfigured(
                helper,
                distilleryPos,
                ModBlocks.DISTILLERY.get(),
                ModProcessingMachines.DISTILLERY);
        FireboxBlockEntity firebox =
                helper.getBlockEntity(distilleryPos.below());
        helper.assertTrue(
                firebox.addFuel(FuelDefinition.COAL_COKE),
                "Could not fuel HU distillery route");
        GTRecipe recipe = requireRecipe(
                ModRecipeMaps.DISTILLERY,
                "t5/distillery/water_to_water_distilled");
        ItemStack portableTank = new ItemStack(ModItems.PORTABLE_FLUID_TANK.get());
        var tankHandler =
                portableTank.getCapability(Capabilities.FluidHandler.ITEM);
        helper.assertTrue(
                tankHandler != null,
                "Portable tank item capability missing for distillery route");
        loadRecipeInputs(distillery, recipe);
        helper.assertTrue(
                distillery.spec().energy().type() == EnergyType.HEAT
                        && distillery.spec().energy().mode()
                                == ProcessingMachineSpec.EnergyMode.ADJACENT,
                "Distillery source route did not use adjacent HU");

        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> forceLastTick(helper, distillery))
                .thenIdle(2)
                .thenExecute(() -> {
                    drainMachineIntoItem(
                            helper,
                            distillery.playerDrainFluids(Direction.NORTH),
                            tankHandler);
                    loadRecipeInputs(distillery, recipe);
                })
                .thenIdle(3)
                .thenExecute(() -> forceLastTick(helper, distillery))
                .thenIdle(2)
                .thenExecute(() -> {
                    drainMachineIntoItem(
                            helper,
                            distillery.playerDrainFluids(Direction.NORTH),
                            tankHandler);
                    FluidStack stored = tankHandler.getFluidInTank(0);
                    helper.assertTrue(
                            stored.getAmount()
                                            == recipe.fluidOutputs().getFirst().getAmount() * 2
                                    && FluidStack.isSameFluidSameComponents(
                                            stored,
                                            recipe.fluidOutputs().getFirst()),
                            "Distillery source route did not conserve repeated output: "
                                    + stored);
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void disjointWiremillInputsChooseFirstDeclaredRecipe(
            GameTestHelper helper) {
        BlockPos pos = new BlockPos(5, 2, 5);
        ConfiguredProcessingMachineBlockEntity wiremill = placeConfigured(
                helper,
                pos,
                ModBlocks.WIREMILL.get(),
                ModProcessingMachines.WIREMILL);
        GTRecipe foilRecipe = requireRecipe(
                ModRecipeMaps.WIREMILL,
                "wiremill/foil_to_fine_wire/copper");
        GTRecipe ingotRecipe = requireRecipe(
                ModRecipeMaps.WIREMILL,
                "wiremill/ingot_to_wire/copper");
        ItemStack foil = foilRecipe.itemInputs().getFirst().getItems()[0].copy();
        foil.setCount(foilRecipe.itemInputCounts().getFirst());
        ItemStack ingot = ingotRecipe.itemInputs().getFirst().getItems()[0].copy();
        ingot.setCount(ingotRecipe.itemInputCounts().getFirst());
        int foilSlot = wiremill.spec().items().inputs().get(0);
        int ingotSlot = wiremill.spec().items().inputs().get(1);
        wiremill.inventory().setStackInSlot(foilSlot, foil);
        wiremill.inventory().setStackInSlot(ingotSlot, ingot.copy());
        fillKu(helper, wiremill);

        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    helper.assertTrue(
                            wiremill.progress() > 0,
                            "Disjoint wiremill inputs did not select a stable recipe");
                    forceLastTick(helper, wiremill);
                    fillKu(helper, wiremill);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    ItemStack output = wiremill.inventory().getStackInSlot(
                            wiremill.spec().items().outputs().getFirst());
                    helper.assertTrue(
                            ItemStack.isSameItemSameComponents(
                                    output, foilRecipe.itemOutputs().getFirst()),
                            "Wiremill did not use first-declared maximal recipe");
                    helper.assertTrue(
                            ItemStack.isSameItemSameComponents(
                                    wiremill.inventory().getStackInSlot(ingotSlot),
                                    ingot),
                            "Unselected disjoint wiremill input was consumed");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void machineFluidOutputCanBeCarriedAndConsumed(
            GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity electrolyzer = placeConfigured(
                helper,
                new BlockPos(3, 2, 5),
                ModBlocks.STAINLESS_STEEL_ELECTROLYZER.get(),
                ModProcessingMachines.ELECTROLYZER);
        ConfiguredProcessingMachineBlockEntity mixer = placeConfigured(
                helper,
                new BlockPos(9, 2, 5),
                ModBlocks.MIXER.get(),
                ModProcessingMachines.MIXER);
        GTRecipe salt = requireRecipe(
                ModRecipeMaps.ELECTROLYZER, "t5/electrolyzer/salt");
        GTRecipe hydrochloricAcid = requireRecipe(
                ModRecipeMaps.MIXER,
                "t5/mixer/fluid_closure_hydrochloric_acid");
        loadRecipeInputs(electrolyzer, salt);
        helper.assertTrue(
                electrolyzer.insert(
                        EnergyType.ELECTRIC,
                        salt.eut(),
                        64L,
                        Direction.SOUTH,
                        false) > 0,
                "Could not power chlorine-producing electrolyzer");
        ItemStack portableTank = new ItemStack(ModItems.PORTABLE_FLUID_TANK.get());
        var portableHandler =
                portableTank.getCapability(Capabilities.FluidHandler.ITEM);
        helper.assertTrue(
                portableHandler != null,
                "Portable tank item capability missing for round trip");

        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    forceLastTick(helper, electrolyzer);
                    helper.assertTrue(
                            electrolyzer.insert(
                                            EnergyType.ELECTRIC,
                                            512L,
                                            2L,
                                            Direction.SOUTH,
                                            false)
                                    > 0L,
                            "Could not refill the T3 electrolyzer");
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(
                            electrolyzer.spec().fluids().outputs().stream()
                                    .anyMatch(tank -> !electrolyzer
                                            .tanks()
                                            .get(tank.index())
                                            .getFluid()
                                            .isEmpty()),
                            "T3 electrolyzer produced no fluid: progress="
                                    + electrolyzer.progress()
                                    + "/"
                                    + electrolyzer.duration()
                                    + ", status="
                                    + electrolyzer.pausedReason()
                                    + ", energy="
                                    + electrolyzer.stored(
                                            EnergyType.ELECTRIC));
                    drainMachineIntoItem(
                            helper,
                            electrolyzer.playerDrainFluids(Direction.NORTH),
                            portableHandler);
                    FluidStack chlorine = portableHandler.getFluidInTank(0);
                    helper.assertTrue(
                            sameFluidAmount(
                                    chlorine,
                                    hydrochloricAcid.fluidInputs().get(1)),
                            "Electrolyzer chlorine did not enter portable tank");
                    mixer.tanks().get(
                            mixer.spec().fluids().inputs().getFirst().index())
                            .setFluid(hydrochloricAcid.fluidInputs().getFirst().copy());
                    helper.assertTrue(
                            ProcessingMachineInteractions.fluidTransfer(
                                    mixer.spec(),
                                    Direction.NORTH,
                                    Direction.WEST,
                                    false,
                                    true,
                                    true,
                                    true)
                                    == ProcessingMachineInteractions.FluidTransfer.FILL_INPUT,
                            "Partially filled portable tank did not prefer machine fill");
                    fillMachineFromItem(
                            helper,
                            portableHandler,
                            mixer.fluids(Direction.WEST));
                    helper.assertTrue(
                            portableHandler.getFluidInTank(0).isEmpty(),
                            "Portable tank retained chlorine after machine fill");
                    helper.assertTrue(
                            mixer.insert(
                                    EnergyType.ELECTRIC,
                                    hydrochloricAcid.eut(),
                                    64L,
                                    Direction.SOUTH,
                                    false) > 0,
                            "Could not power hydrochloric-acid mixer");
                })
                .thenIdle(3)
                .thenExecute(() -> forceLastTick(helper, mixer))
                .thenIdle(2)
                .thenExecute(() -> {
                    FluidStack expected =
                            hydrochloricAcid.fluidOutputs().getFirst();
                    helper.assertTrue(
                            mixer.spec().fluids().outputs().stream().anyMatch(tank ->
                                    sameFluidAmount(
                                            mixer.tanks().get(tank.index()).getFluid(),
                                            expected)),
                            "Carried chlorine was not consumed into hydrochloric acid");
                    helper.assertTrue(
                            mixer.spec().fluids().inputs().stream().allMatch(tank ->
                                    mixer.tanks().get(tank.index()).getFluid().isEmpty()),
                            "Mixer retained a consumed fluid input");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void genericCellsEnforceDomainsAndFeedMachineRecipe(
            GameTestHelper helper) {
        helper.assertTrue(
                ModFluids.chemicalFluids().size() == 110
                        && CellContentGate.entries().size() == 110,
                "T11 chemical-fluid or T10 cell allowlist registry is incomplete");
        helper.assertTrue(
                MaterialPrefixCatalog.values().size() == 56
                        && MaterialCatalog.startupValues().size()
                                + MaterialPrefixCatalog.values().size() == 1_830
                        && MaterialCatalog.startupValues().stream()
                                .mapToInt(material ->
                                        MaterialCatalog.registeredForms(
                                                material).size())
                                .sum() == 16_048,
                "T10 cell contents changed prefix, handshake, or form counts");
        List<ResourceLocation> cellItems = BuiltInRegistries.ITEM.keySet()
                .stream()
                .filter(id -> id.getNamespace().equals(CrucibleCraft.MODID))
                .filter(id -> BuiltInRegistries.ITEM.get(id) instanceof CellItem)
                .sorted()
                .toList();
        helper.assertTrue(
                cellItems.equals(List.of(
                        ResourceLocation.fromNamespaceAndPath(
                                CrucibleCraft.MODID,
                                "fluid_cell"),
                        ResourceLocation.fromNamespaceAndPath(
                                CrucibleCraft.MODID,
                                "gas_cell"))),
                "T10 registered per-fluid or unexpected cell item identities");
        GTRecipe hydrogenFluoride = requireRecipe(
                ModRecipeMaps.MIXER,
                "t5/mixer/fluid_closure_hydrogen_fluoride");
        FluidStack hydrogen = hydrogenFluoride.fluidInputs().getFirst();
        FluidStack fluorine = hydrogenFluoride.fluidInputs().get(1);
        FluidStack chlorine = new FluidStack(
                ModFluids.materialFluid("chlorine").orElseThrow(),
                1_000);

        Player cellPlayer = helper.makeMockPlayer(GameType.SURVIVAL);
        FluidTank manualSource = new FluidTank(1_000);
        manualSource.setFluid(chlorine.copy());
        cellPlayer.setItemInHand(
                InteractionHand.MAIN_HAND,
                new ItemStack(ModItems.FLUID_CELL.get()));
        helper.assertTrue(
                FluidUtil.interactWithFluidHandler(
                        cellPlayer,
                        InteractionHand.MAIN_HAND,
                        manualSource),
                "A single empty cell could not be manually filled");
        ItemStack singleFilled =
                cellPlayer.getItemInHand(InteractionHand.MAIN_HAND);
        var singleFilledHandler =
                singleFilled.getCapability(Capabilities.FluidHandler.ITEM);
        helper.assertTrue(
                singleFilled.getCount() == 1
                        && singleFilledHandler != null
                        && sameFluidAmount(
                                singleFilledHandler.getFluidInTank(0),
                                chlorine),
                "Single-cell manual filling did not replace the held stack");

        cellPlayer.setItemInHand(
                InteractionHand.MAIN_HAND,
                new ItemStack(ModItems.FLUID_CELL.get(), 2));
        manualSource.setFluid(chlorine.copy());
        helper.assertTrue(
                FluidUtil.interactWithFluidHandler(
                        cellPlayer,
                        InteractionHand.MAIN_HAND,
                        manualSource),
                "A stacked empty cell could not be manually filled");
        ItemStack stackedRemainder =
                cellPlayer.getItemInHand(InteractionHand.MAIN_HAND);
        helper.assertTrue(
                stackedRemainder.getCount() == 1
                        && stackedRemainder.getCapability(
                                        Capabilities.FluidHandler.ITEM)
                                .getFluidInTank(0)
                                .isEmpty()
                        && inventoryContainsFluid(
                                cellPlayer,
                                ModItems.FLUID_CELL.get(),
                                chlorine),
                "Stacked manual filling did not split and stow one full cell");

        ItemStack fluidCell = new ItemStack(ModItems.FLUID_CELL.get());
        var fluidHandler =
                fluidCell.getCapability(Capabilities.FluidHandler.ITEM);
        ItemStack gasCell = new ItemStack(ModItems.GAS_CELL.get());
        var gasHandler =
                gasCell.getCapability(Capabilities.FluidHandler.ITEM);
        helper.assertTrue(
                fluidHandler != null && gasHandler != null,
                "Generic cell item capabilities are missing");
        helper.assertTrue(
                fluidHandler.fill(
                        chlorine,
                        IFluidHandler.FluidAction.EXECUTE) == 1_000
                        && fluidCell.getMaxStackSize() == 1,
                "Fluid-domain cell did not accept source-tagged chlorine");
        helper.assertTrue(
                gasHandler.fill(
                        chlorine,
                        IFluidHandler.FluidAction.SIMULATE) == 0,
                "Gas cell accepted a fluid-domain identity");
        helper.assertTrue(
                fluidHandler.drain(
                        1_000,
                        IFluidHandler.FluidAction.EXECUTE).getAmount() == 1_000
                        && fluidCell.getMaxStackSize() == 64,
                "Fully drained fluid cell did not restore empty stacking");
        helper.assertTrue(
                fluidHandler.fill(
                        fluorine,
                        IFluidHandler.FluidAction.SIMULATE) == 0,
                "Fluid cell accepted a gas-domain identity");
        helper.assertTrue(
                gasHandler.fill(
                        fluorine.copyWithAmount(1_000),
                        IFluidHandler.FluidAction.EXECUTE) == 1_000,
                "Gas-domain cell did not accept fluorine");

        ConfiguredProcessingMachineBlockEntity mixer = placeConfigured(
                helper,
                new BlockPos(5, 2, 5),
                ModBlocks.MIXER.get(),
                ModProcessingMachines.MIXER);
        mixer.tanks().get(
                mixer.spec().fluids().inputs().getFirst().index())
                .setFluid(hydrogen.copy());
        fillMachineFromItem(
                helper,
                gasHandler,
                mixer.fluids(Direction.WEST));
        helper.assertTrue(
                gasHandler.getFluidInTank(0).isEmpty(),
                "Gas cell retained fluid after machine transfer");
        helper.assertTrue(
                mixer.insert(
                        EnergyType.ELECTRIC,
                        hydrogenFluoride.eut(),
                        64L,
                        Direction.SOUTH,
                        false) > 0,
                "Could not power gas-cell mixer route");

        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> forceLastTick(helper, mixer))
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        mixer.spec().fluids().outputs().stream().anyMatch(tank ->
                                sameFluidAmount(
                                        mixer.tanks().get(tank.index()).getFluid(),
                                        hydrogenFluoride.fluidOutputs().getFirst())),
                        "Gas cell contents were not consumed by the fluid recipe"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void reusedT5HostsExecuteExpandedOutputShapes(
            GameTestHelper helper) {
        BlockPos bathPos = new BlockPos(3, 2, 5);
        BlockPos centrifugePos = new BlockPos(8, 2, 5);
        BlockPos fireboxPos = new BlockPos(13, 1, 5);
        BlockPos smelterPos = fireboxPos.above();
        ConfiguredProcessingMachineBlockEntity bath = placeConfigured(
                helper, bathPos, ModBlocks.BATH.get(), ModProcessingMachines.BATH);
        ConfiguredProcessingMachineBlockEntity centrifuge = placeConfigured(
                helper,
                centrifugePos,
                ModBlocks.TITANIUM_CENTRIFUGE.get(),
                ModProcessingMachines.CENTRIFUGE);
        helper.setBlock(fireboxPos, ModBlocks.FIREBOX.get());
        ConfiguredProcessingMachineBlockEntity smelter = placeConfigured(
                helper,
                smelterPos,
                ModBlocks.SMELTER.get(),
                ModProcessingMachines.SMELTER);
        FireboxBlockEntity firebox = helper.getBlockEntity(fireboxPos);
        helper.assertTrue(
                firebox.addFuel(FuelDefinition.COAL_COKE),
                "Could not fuel reused T5 smelter");
        GTRecipe bathRecipe = requireRecipe(
                ModRecipeMaps.BATH, "t5/bath/niobium_pentoxide");
        GTRecipe centrifugeRecipe = requireRecipe(
                ModRecipeMaps.CENTRIFUGE, "t5/centrifuge/gloomstone");
        GTRecipe smelterRecipe = requireRecipe(
                ModRecipeMaps.SMELTER, "t5/smelter/ilmenite");
        loadRecipeInputs(bath, bathRecipe);
        loadRecipeInputs(centrifuge, centrifugeRecipe);
        loadRecipeInputs(smelter, smelterRecipe);
        fillKu(helper, bath);
        fillKu(helper, centrifuge);

        helper.startSequence()
                .thenExecuteFor(4, () -> {
                    fillKu(helper, bath);
                    fillKu(helper, centrifuge);
                })
                .thenExecute(() -> {
                    helper.assertTrue(
                            bath.progress() > 0
                                    && centrifuge.progress() > 0
                                    && smelter.progress() > 0,
                            "A reused T5 host did not start its expanded recipe: "
                                    + "bath="
                                    + bath.progress()
                                    + "/"
                                    + bath.pausedReason()
                                    + ", centrifuge="
                                    + centrifuge.progress()
                                    + "/"
                                    + centrifuge.pausedReason()
                                    + ", smelter="
                                    + smelter.progress()
                                    + "/"
                                    + smelter.pausedReason());
                    forceLastTick(helper, bath);
                    forceLastTick(helper, centrifuge);
                    forceLastTick(helper, smelter);
                    fillKu(helper, bath);
                    fillKu(helper, centrifuge);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    for (ConfiguredProcessingMachineBlockEntity machine :
                            List.of(bath, centrifuge, smelter)) {
                        helper.assertTrue(
                                machine.spec().fluids().outputs().stream().anyMatch(tank ->
                                        !machine.tanks().get(tank.index()).getFluid().isEmpty()),
                                machine.spec().id()
                                        + " did not commit its expanded fluid output");
                    }
                    helper.assertTrue(
                            hasAnyOutput(bath) && hasAnyOutput(centrifuge),
                            "Expanded bath/centrifuge item outputs were not committed");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void concreteOreRecipesArePublishedToLiveMaps(
            GameTestHelper helper) {
        for (String materialId : List.of(
                "copper", "tin", "iron", "gold", "tungsten")) {
            assertPublishedOreChain(helper, materialId);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 240)
    public static void crusherPauseRollbackResume(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 2, 3);
        helper.setBlock(pos, ModBlocks.BRONZE_CRUSHER.get());
        CrusherBlockEntity crusher = helper.getBlockEntity(pos);
        ItemStack raw = material("copper", MaterialPrefixes.RAW_ORE, 1);
        crusher.inventory().setStackInSlot(0, raw);

        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    helper.assertTrue(crusher.progress() == 0, "Crusher advanced without KU");
                    helper.assertTrue(crusher.inventory().getStackInSlot(0).getCount() == 1,
                            "Underpower consumed crusher input");
                    crusher.inventory().setStackInSlot(1, new ItemStack(Items.BEDROCK, 64));
                    fillKuCapability(helper, pos);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(crusher.progress() == 0, "Blocked crusher advanced");
                    helper.assertTrue(crusher.inventory().getStackInSlot(0).getCount() == 1,
                            "Blocked crusher consumed input");
                    crusher.inventory().setStackInSlot(1, ItemStack.EMPTY);
                    fillKuCapability(helper, pos);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(crusher.duration() > 0, "Crusher did not select real recipe");
                    crusher.runtime().processor().setProgress(crusher.duration() - 1);
                    fillKuCapability(helper, pos);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(crusher.inventory().getStackInSlot(0).isEmpty(),
                            "Crusher did not consume raw ore");
                    helper.assertTrue(!crusher.inventory().getStackInSlot(1).isEmpty(),
                            "Crusher did not produce crushed ore");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 220)
    public static void crusherHonorsDeclaredRecipeDuration(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 2, 3);
        helper.setBlock(pos, ModBlocks.BRONZE_CRUSHER.get());
        CrusherBlockEntity crusher = helper.getBlockEntity(pos);
        ItemStack raw = material("copper", MaterialPrefixes.RAW_ORE, 1);
        RecipeMap.Match liveRecipe = ModRecipeMaps.CRUSHER.findMatch(
                GTRecipeQuery.items(raw.copy())).orElseThrow();
        int declaredDuration = liveRecipe.recipe().duration();
        int[] maxObservedProgress = {0};
        boolean[] completed = {false};
        crusher.inventory().setStackInSlot(CrusherBlockEntity.INPUT_SLOT, raw);

        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(
                            crusher.duration() == declaredDuration,
                            "Crusher did not expose the live recipe's declared duration");
                    helper.assertTrue(
                            crusher.progress() == 0,
                            "Unpowered crusher advanced before duration test");
                    fillKuCapability(helper, pos);
                })
                .thenExecuteFor(declaredDuration + 8, () -> {
                    if (completed[0]) {
                        return;
                    }
                    ItemStack output = crusher.inventory().getStackInSlot(
                            CrusherBlockEntity.OUTPUT_SLOT);
                    if (!output.isEmpty()) {
                        helper.assertTrue(
                                maxObservedProgress[0] == declaredDuration - 1,
                                "Crusher completed before all declared duration ticks");
                        helper.assertTrue(
                                crusher.inventory().getStackInSlot(
                                        CrusherBlockEntity.INPUT_SLOT).isEmpty(),
                                "Duration-complete crusher retained its input");
                        completed[0] = true;
                        return;
                    }
                    helper.assertTrue(
                            !crusher.inventory().getStackInSlot(
                                    CrusherBlockEntity.INPUT_SLOT).isEmpty(),
                            "Crusher consumed input before declared duration elapsed");
                    helper.assertTrue(
                            crusher.progress() < declaredDuration,
                            "Crusher reached declared duration without completing atomically");
                    maxObservedProgress[0] = Math.max(
                            maxObservedProgress[0], crusher.progress());
                    fillKuCapability(helper, pos);
                })
                .thenExecute(() -> helper.assertTrue(
                        completed[0],
                        "Crusher did not complete after its declared duration"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void silkTouchedOreHostsCrushToExactlyFiveCrushed(
            GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Block stoneOre = ModBlocks.oreBlock("copper", Host.STONE).get();
        Block deepslateOre = ModBlocks.oreBlock("copper", Host.DEEPSLATE).get();
        BlockPos stonePos = new BlockPos(1, 2, 1);
        BlockPos deepslatePos = new BlockPos(1, 2, 3);
        helper.setBlock(stonePos, stoneOre);
        helper.setBlock(deepslatePos, deepslateOre);

        ItemStack silkPick = new ItemStack(Items.DIAMOND_PICKAXE);
        silkPick.enchant(
                level.registryAccess()
                        .lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.SILK_TOUCH),
                1);
        List<ItemStack> stoneDrops = Block.getDrops(
                helper.getBlockState(stonePos),
                level,
                helper.absolutePos(stonePos),
                null,
                null,
                silkPick);
        List<ItemStack> deepslateDrops = Block.getDrops(
                helper.getBlockState(deepslatePos),
                level,
                helper.absolutePos(deepslatePos),
                null,
                null,
                silkPick);
        helper.assertTrue(
                stoneDrops.size() == 1
                        && stoneDrops.getFirst().is(stoneOre.asItem())
                        && stoneDrops.getFirst().getCount() == 1,
                "Silk Touch did not preserve the stone-host copper ore block");
        helper.assertTrue(
                deepslateDrops.size() == 1
                        && deepslateDrops.getFirst().is(deepslateOre.asItem())
                        && deepslateDrops.getFirst().getCount() == 1,
                "Silk Touch did not preserve the deepslate-host copper ore block");

        RecipeMap.Match stoneMatch = ModRecipeMaps.CRUSHER.findMatch(
                GTRecipeQuery.items(stoneDrops.getFirst().copy())).orElseThrow();
        RecipeMap.Match deepslateMatch = ModRecipeMaps.CRUSHER.findMatch(
                GTRecipeQuery.items(deepslateDrops.getFirst().copy())).orElseThrow();
        RecipeMap.Match rawMatch = ModRecipeMaps.CRUSHER.findMatch(
                GTRecipeQuery.items(material(
                        "copper", MaterialPrefixes.RAW_ORE, 1))).orElseThrow();
        helper.assertTrue(
                stoneMatch.id().equals(deepslateMatch.id()),
                "Stone and deepslate ore hosts did not resolve the same tag recipe");
        helper.assertTrue(
                stoneMatch.recipe().duration() == rawMatch.recipe().duration()
                        && stoneMatch.recipe().eut() == rawMatch.recipe().eut()
                        && stoneMatch.recipe().outputChances().equals(
                                rawMatch.recipe().outputChances()),
                "Ore-block crusher parameters diverged from the raw-ore baseline");
        ItemStack expected = material(
                "copper", MaterialPrefixes.CRUSHED_ORE, 5);
        helper.assertTrue(
                stoneMatch.recipe().itemOutputs().size() == 1
                        && ItemStack.isSameItemSameComponents(
                                stoneMatch.recipe().itemOutputs().getFirst(),
                                expected)
                        && stoneMatch.recipe().itemOutputs().getFirst().getCount() == 5,
                "Silk-touched ore recipe did not declare exactly five crushed ore");

        BlockPos crusherPos = new BlockPos(3, 2, 3);
        helper.setBlock(crusherPos, ModBlocks.BRONZE_CRUSHER.get());
        CrusherBlockEntity crusher = helper.getBlockEntity(crusherPos);
        crusher.inventory().setStackInSlot(
                CrusherBlockEntity.INPUT_SLOT, stoneDrops.getFirst().copy());
        fillKuCapability(helper, crusherPos);
        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    forceLastTick(helper, crusher);
                    fillKuCapability(helper, crusherPos);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(
                            crusher.inventory().getStackInSlot(
                                    CrusherBlockEntity.INPUT_SLOT).isEmpty(),
                            "Crusher retained the silk-touched ore block");
                    ItemStack output = crusher.inventory().getStackInSlot(
                            CrusherBlockEntity.OUTPUT_SLOT);
                    helper.assertTrue(
                            ItemStack.isSameItemSameComponents(output, expected)
                                    && output.getCount() == 5,
                            "Crusher did not produce exactly five crushed ore");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void tungstenJsonProvidesCrusherIngotToDustRecipe(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 2, 3);
        helper.setBlock(pos, ModBlocks.BRONZE_CRUSHER.get());
        CrusherBlockEntity crusher = helper.getBlockEntity(pos);
        crusher.inventory().setStackInSlot(
                0, material("tungsten", MaterialPrefixes.INGOT, 1));
        ItemStack expectedDust = material("tungsten", MaterialPrefixes.DUST, 1);
        fillKuCapability(helper, pos);

        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    forceLastTick(helper, crusher);
                    fillKuCapability(helper, pos);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(crusher.inventory().getStackInSlot(0).isEmpty(),
                            "Crusher did not consume tungsten ingot");
                    ItemStack output = crusher.inventory().getStackInSlot(1);
                    helper.assertTrue(
                            ItemStack.isSameItemSameComponents(output, expectedDust),
                            "Crusher did not produce tungsten dust");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 260)
    public static void waterMachinesCapabilityAndCompletion(GameTestHelper helper) {
        BlockPos sluicePos = new BlockPos(3, 2, 3);
        BlockPos bathPos = new BlockPos(8, 2, 3);
        ConfiguredProcessingMachineBlockEntity sluice =
                placeConfigured(helper, sluicePos, ModBlocks.SLUICE.get(), ModProcessingMachines.SLUICE);
        ConfiguredProcessingMachineBlockEntity bath =
                placeConfigured(helper, bathPos, ModBlocks.BATH.get(), ModProcessingMachines.BATH);
        ItemStack crushed = material("copper", MaterialPrefixes.CRUSHED_ORE, 1);
        FluidStack offeredWater = new FluidStack(Fluids.WATER, 1_000);
        int sluiceWater = ModRecipeMaps.SLUICE.findMatch(new GTRecipeQuery(
                        List.of(crushed.copy()), List.of(offeredWater.copy())))
                .orElseThrow().recipe().fluidInputs().getFirst().getAmount();
        int bathWater = ModRecipeMaps.BATH.findMatch(new GTRecipeQuery(
                        List.of(crushed.copy()), List.of(offeredWater.copy())))
                .orElseThrow().recipe().fluidInputs().getFirst().getAmount();
        sluice.inventory().setStackInSlot(0, crushed.copy());
        bath.inventory().setStackInSlot(0, crushed.copy());
        assertWaterPolicy(helper, sluice);
        assertWaterPolicy(helper, bath);
        fillKuCapability(helper, sluicePos);
        fillKuCapability(helper, bathPos);

        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    forceLastTick(helper, sluice);
                    forceLastTick(helper, bath);
                    fillKuCapability(helper, sluicePos);
                    fillKuCapability(helper, bathPos);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(sluice.inventory().getStackInSlot(0).isEmpty(),
                            "Sluice did not consume input");
                    helper.assertTrue(bath.inventory().getStackInSlot(0).isEmpty(),
                            "Bath did not consume input");
                    helper.assertTrue(hasAnyOutput(sluice), "Sluice produced no output");
                    helper.assertTrue(hasAnyOutput(bath), "Bath produced no output");
                    helper.assertTrue(
                            inputFluidAmount(sluice) == 1_000 - sluiceWater,
                            "Sluice did not consume its live recipe's "
                                    + sluiceWater + " mB water");
                    helper.assertTrue(
                            inputFluidAmount(bath) == 1_000 - bathWater,
                            "Bath did not consume its live recipe's "
                                    + bathWater + " mB water");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void cokeOvenProducesAndExtractsCreosote(GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 3, 6);
        Direction facing = Direction.NORTH;
        helper.setBlock(controllerPos, ModBlocks.COKE_OVEN.get());
        var structure = MultiblockStructureCatalog.require(
                CokeOvenBlockEntity.STRUCTURE_ID);
        structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.BLOCK)
                .forEach(element -> helper.setBlock(
                        structure.worldPosition(
                                controllerPos, facing, element.offset()),
                        ModBlocks.FIREBRICK.get()));
        BlockPos fireboxPos = structure.anchor(
                "heat_source", controllerPos, facing);
        helper.setBlock(fireboxPos, ModBlocks.FIREBOX.get());
        FireboxBlockEntity firebox = helper.getBlockEntity(fireboxPos);
        helper.assertTrue(
                firebox.addFuel(FuelDefinition.CHARCOAL),
                "Could not fuel coke oven heat source");
        CokeOvenBlockEntity cokeOven = helper.getBlockEntity(controllerPos);
        RecipeMap.Entry coalRecipe = ModRecipeMaps.COKE_OVEN.entries().stream()
                .filter(entry -> entry.recipe().itemInputs().size() == 1
                        && entry.recipe().itemInputs().getFirst()
                                .test(new ItemStack(Items.COAL))
                        && entry.recipe().fluidOutputs().size() == 1
                        && entry.recipe().fluidOutputs().getFirst()
                                .is(ModFluids.CREOSOTE_SOURCE.get()))
                .findFirst()
                .orElseThrow();
        GTRecipe recipe = coalRecipe.recipe();
        FluidStack expectedCreosote = recipe.fluidOutputs().getFirst();
        cokeOven.inventory().setStackInSlot(
                CokeOvenBlockEntity.INPUT_SLOT,
                new ItemStack(Items.COAL, recipe.itemInputCounts().getFirst()));

        helper.startSequence()
                .thenIdle(25)
                .thenExecute(() -> {
                    helper.assertTrue(
                            cokeOven.structureValid(),
                            "Placed coke oven structure was not recognized");
                    helper.assertTrue(cokeOven.ignite(), "Placed coke oven did not ignite");
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(
                            cokeOven.recipeDuration() == recipe.duration()
                                    && cokeOven.progress() > 0,
                            "Coke oven did not advance its live coal recipe");
                    CompoundTag saved = cokeOven.saveWithoutMetadata(
                            helper.getLevel().registryAccess());
                    saved.putInt("progress", recipe.duration() - 1);
                    cokeOven.loadWithComponents(
                            saved, helper.getLevel().registryAccess());
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(
                            cokeOven.inventory().getStackInSlot(
                                    CokeOvenBlockEntity.INPUT_SLOT).isEmpty(),
                            "Coke oven did not consume coal");
                    helper.assertTrue(
                            ItemStack.isSameItemSameComponents(
                                    cokeOven.inventory().getStackInSlot(
                                            CokeOvenBlockEntity.OUTPUT_SLOT),
                                    recipe.itemOutputs().getFirst()),
                            "Coke oven did not produce its live item output");
                    helper.assertTrue(
                            cokeOven.creosoteAmount() == expectedCreosote.getAmount(),
                            "Coke oven produced the wrong creosote amount");
                    IFluidHandler extraction = cokeOven.externalFluids();
                    FluidStack simulated = extraction.drain(
                            expectedCreosote.copy(),
                            IFluidHandler.FluidAction.SIMULATE);
                    helper.assertTrue(
                            sameFluidAmount(simulated, expectedCreosote)
                                    && cokeOven.creosoteAmount()
                                            == expectedCreosote.getAmount(),
                            "Coke oven creosote simulation changed or misreported the tank");
                    FluidStack extracted = extraction.drain(
                            expectedCreosote.copy(),
                            IFluidHandler.FluidAction.EXECUTE);
                    helper.assertTrue(
                            sameFluidAmount(extracted, expectedCreosote)
                                    && cokeOven.creosoteAmount() == 0,
                            "Coke oven did not expose exact creosote extraction");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void largeCentrifugeUsesJsonPortsAndProcessingHost(
            GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        Direction facing = Direction.NORTH;
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.LARGE_CENTRIFUGE.structureId());
        List<BlockPos> itemFluidPorts = structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.PORT)
                .filter(element -> structure.predicate(element)
                        .port().orElseThrow()
                        == com.masson.cruciblecraft.content.multiblock
                                .MultiblockStructureDefinition.PortType.ITEM_FLUID)
                .map(element -> structure.worldPosition(
                        controllerPos, facing, element.offset()))
                .toList();
        List<BlockPos> energyPorts = structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.PORT)
                .filter(element -> structure.predicate(element)
                        .port().orElseThrow()
                        == com.masson.cruciblecraft.content.multiblock
                                .MultiblockStructureDefinition.PortType.ENERGY_INPUT)
                .map(element -> structure.worldPosition(
                        controllerPos, facing, element.offset()))
                .toList();
        helper.setBlock(
                controllerPos,
                ModBlocks.LARGE_CENTRIFUGE.get()
                        .defaultBlockState()
                        .setValue(ProcessingMachineBlock.FACING, facing));
        structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.PORT)
                .forEach(element -> {
                    var predicate = structure.predicate(element);
                    helper.setBlock(
                            structure.worldPosition(
                                    controllerPos, facing, element.offset()),
                            predicate.port().orElseThrow()
                                            == com.masson.cruciblecraft.content
                                                    .multiblock
                                                    .MultiblockStructureDefinition
                                                    .PortType.ENERGY_INPUT
                                    ? ModBlocks.MULTIBLOCK_ENERGY_INPUT_PORT.get()
                                    : ModBlocks.MULTIBLOCK_ITEM_FLUID_PORT.get());
                });
        LargeCentrifugeBlockEntity centrifuge =
                helper.getBlockEntity(controllerPos);
        GTRecipe recipe = requireRecipe(
                ModRecipeMaps.CENTRIFUGE,
                "t5/centrifuge/gloomstone");
        loadRecipeInputs(centrifuge, recipe);
        BlockPos energyPort = structure.anchor(
                "bottom_energy_input", controllerPos, facing);

        helper.startSequence()
                .thenIdle(25)
                .thenExecute(() -> {
                    helper.assertTrue(
                            centrifuge.structureValid(),
                            "Large centrifuge JSON structure was not recognized");
                    helper.assertTrue(
                            structure.structure().size() == 18
                                    && itemFluidPorts.size() == 15
                                    && energyPorts.size() == 2,
                            "Large centrifuge physical port/controller counts drifted");
                    helper.assertTrue(
                            centrifuge.spec().items().inputs().size() == 1
                                    && centrifuge.spec().items().outputs().size() == 6
                                    && centrifuge.spec().items().slotCount() == 7
                                    && centrifuge.spec().fluids().inputs().size() == 1
                                    && centrifuge.spec().fluids().outputs().size() == 2
                                    && centrifuge.spec().fluids().all().size() == 3,
                            "Large centrifuge shared host layout drifted");
                    int inputSlot =
                            centrifuge.spec().items().inputs().getFirst();
                    ItemStack hostInput =
                            centrifuge.inventory().getStackInSlot(inputSlot);
                    for (BlockPos portPos : itemFluidPorts) {
                        MultiblockPortBlockEntity port =
                                helper.getBlockEntity(portPos);
                        helper.assertTrue(
                                port.itemHandler().getSlots()
                                        == centrifuge.inventory().getSlots()
                                        && port.itemHandler()
                                                .getStackInSlot(inputSlot)
                                                .getCount()
                                        == hostInput.getCount()
                                        && ItemStack.isSameItemSameComponents(
                                                port.itemHandler()
                                                        .getStackInSlot(inputSlot),
                                                hostInput)
                                        && port.fluidHandler().getTanks()
                                        == centrifuge.tanks().size(),
                                "Physical item/fluid port did not bridge the shared host");
                    }
                    int inputTank = centrifuge.spec()
                            .fluids().inputs().getFirst().index();
                    centrifuge.tanks().get(inputTank).setFluid(
                            new FluidStack(Fluids.WATER, 250));
                    helper.assertTrue(
                            itemFluidPorts.stream().allMatch(portPos -> {
                                MultiblockPortBlockEntity port =
                                        helper.getBlockEntity(portPos);
                                return port.fluidHandler()
                                        .getFluidInTank(inputTank)
                                        .getAmount() == 250;
                            }),
                            "Fifteen physical ports did not expose one host tank");
                    BlockPos worldEnergyPort = helper
                            .getBlockEntity(energyPort)
                            .getBlockPos();
                    fillKuCapability(
                            helper,
                            worldEnergyPort,
                            "large centrifuge energy port");
                })
                .thenIdle(3)
                .thenExecute(() -> {
                    helper.assertTrue(
                            centrifuge.progress() > 0,
                            "Large centrifuge did not run through the shared host");
                    forceLastTick(helper, centrifuge);
                    fillKuCapability(
                            helper,
                            helper.getBlockEntity(energyPort).getBlockPos(),
                            "large centrifuge energy port");
                })
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        hasAnyOutput(centrifuge),
                        "Large centrifuge did not commit recipe outputs"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 140)
    public static void t12RuAxleGearboxPowersTieredCentrifuge(
            GameTestHelper helper) {
        BlockPos motorPos = new BlockPos(3, 2, 13);
        BlockPos axlePos = motorPos.east();
        BlockPos gearboxPos = axlePos.east();
        BlockPos centrifugePos = gearboxPos.east();
        helper.setBlock(
                motorPos,
                ModBlocks.ELECTRIC_MOTOR.get().defaultBlockState()
                        .setValue(
                                ElectricMotorBlock.FACING,
                                Direction.EAST));
        helper.setBlock(
                axlePos,
                ModBlocks.ROTATIONAL_AXLE.get().defaultBlockState()
                        .setValue(
                                RotationalAxleBlock.AXIS,
                                Direction.Axis.X));
        helper.setBlock(
                gearboxPos,
                ModBlocks.ROTATIONAL_GEARBOX.get().defaultBlockState()
                        .setValue(
                                RotationalGearboxBlock.FACING,
                                Direction.EAST));
        helper.setBlock(
                centrifugePos,
                ModBlocks.STEEL_CENTRIFUGE.get().defaultBlockState()
                        .setValue(
                                ProcessingMachineBlock.FACING,
                                Direction.EAST));
        ConfiguredProcessingMachineBlockEntity centrifuge =
                helper.getBlockEntity(centrifugePos);
        GTRecipe recipe = requireRecipe(
                ModRecipeMaps.CENTRIFUGE,
                "t5/centrifuge/gloomstone");
        loadRecipeInputs(centrifuge, recipe);
        IEnergyHandler motor = helper.getLevel().getCapability(
                ModCapabilities.ENERGY,
                helper.absolutePos(motorPos),
                Direction.WEST);
        IEnergyHandler axle = helper.getLevel().getCapability(
                ModCapabilities.ENERGY,
                helper.absolutePos(axlePos),
                Direction.WEST);
        IEnergyHandler gearbox = helper.getLevel().getCapability(
                ModCapabilities.ENERGY,
                helper.absolutePos(gearboxPos),
                Direction.WEST);
        helper.assertTrue(
                motor != null
                        && motor.handles(
                                EnergyType.ELECTRIC,
                                Direction.WEST),
                "T1 electric motor exposed no EU input");
        helper.assertTrue(
                axle != null
                        && gearbox != null
                        && axle.handles(
                                EnergyType.KINETIC_ROTATION,
                                Direction.WEST)
                        && gearbox.handles(
                                EnergyType.KINETIC_ROTATION,
                                Direction.WEST),
                "RU axle/gearbox capability chain is incomplete");
        helper.assertTrue(
                centrifuge.handles(
                                EnergyType.KINETIC_ROTATION,
                                Direction.WEST)
                        && !centrifuge.handles(
                                EnergyType.KINETIC_PUSH,
                                Direction.WEST),
                "Tiered centrifuge did not preserve RU-only identity");

        helper.startSequence()
                .thenExecuteFor(48, () -> {
                    long accepted = motor.insert(
                            EnergyType.ELECTRIC,
                            32L,
                            1L,
                            Direction.WEST,
                            false);
                    helper.assertTrue(
                            accepted == 1L
                                    || motor.stored(
                                                    EnergyType.ELECTRIC)
                                            >= 32L,
                            "Electric motor rejected its nominal EU packet");
                })
                .thenExecute(() -> helper.assertTrue(
                        centrifuge.progress() > 0,
                        "Motor -> axle -> gearbox RU path did not advance "
                                + "the tiered centrifuge: motorRU="
                                + motor.stored(
                                        EnergyType.KINETIC_ROTATION)
                                + ", axleRU="
                                + axle.stored(
                                        EnergyType.KINETIC_ROTATION)
                                + ", gearboxRU="
                                + gearbox.stored(
                                        EnergyType.KINETIC_ROTATION)
                                + ", machineRU="
                                + centrifuge.stored(
                                        EnergyType.KINETIC_ROTATION)
                                + ", status="
                                + centrifuge.pausedReason()))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void t12TierProfilesExposeDistinctFailureStates(
            GameTestHelper helper) {
        BlockPos machinePos = new BlockPos(12, 2, 13);
        helper.setBlock(
                machinePos,
                ModBlocks.STEEL_CENTRIFUGE.get().defaultBlockState()
                        .setValue(
                                ProcessingMachineBlock.FACING,
                                Direction.EAST));
        ConfiguredProcessingMachineBlockEntity machine =
                helper.getBlockEntity(machinePos);
        helper.assertTrue(
                machine.variant().tier().inputNominal() == 128L
                        && machine.variant().tier().parallelLimit() == 2,
                "Steel centrifuge did not bind its source T2 profile");
        helper.assertTrue(
                machine.insert(
                                EnergyType.KINETIC_PUSH,
                                128L,
                                1L,
                                Direction.WEST,
                                false)
                        == 0L,
                "RU machine accepted KU");
        helper.assertTrue(
                machine.insert(
                                EnergyType.KINETIC_ROTATION,
                                512L,
                                1L,
                                Direction.WEST,
                                false)
                        == 1L
                        && machine.runtime().status().equals(
                                "overcharged"),
                "Oversized RU packet was not observed as overcharge");

        BlockPos windowPos = machinePos.west(3);
        helper.setBlock(
                windowPos,
                ModBlocks.STEEL_CENTRIFUGE.get().defaultBlockState()
                        .setValue(
                                ProcessingMachineBlock.FACING,
                                Direction.EAST));
        ConfiguredProcessingMachineBlockEntity windowMachine =
                helper.getBlockEntity(windowPos);
        GTRecipe lowPower = ModRecipeMaps.CENTRIFUGE.entries().stream()
                .map(RecipeMap.Entry::recipe)
                .filter(recipe -> recipe.eut() > 0L
                        && recipe.eut() <= 16L)
                .findFirst()
                .orElseThrow();
        loadRecipeInputs(windowMachine, lowPower);
        helper.startSequence()
                .thenExecuteFor(2, () -> helper.assertTrue(
                        windowMachine.insert(
                                        EnergyType.KINETIC_ROTATION,
                                        64L,
                                        1L,
                                        Direction.WEST,
                                        false)
                                == 1L,
                        "T2 centrifuge rejected minimum-window RU"))
                .thenExecute(() -> helper.assertTrue(
                        windowMachine.workProgressLong() == 128L
                                && windowMachine.progress() == 1,
                        "Input window did not scale progress by actual RU: "
                                + windowMachine.workProgressLong()
                                + "/"
                                + windowMachine.workRequiredLong()
                                + ", display="
                                + windowMachine.progress()))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 140)
    public static void t16bRuMotorAxleGearboxPowersLathe(
            GameTestHelper helper) {
        BlockPos motorPos = new BlockPos(3, 2, 13);
        BlockPos axlePos = motorPos.east();
        BlockPos gearboxPos = axlePos.east();
        BlockPos lathePos = gearboxPos.east();
        helper.setBlock(
                motorPos,
                ModBlocks.ELECTRIC_MOTOR.get().defaultBlockState()
                        .setValue(
                                ElectricMotorBlock.FACING,
                                Direction.EAST));
        helper.setBlock(
                axlePos,
                ModBlocks.ROTATIONAL_AXLE.get().defaultBlockState()
                        .setValue(
                                RotationalAxleBlock.AXIS,
                                Direction.Axis.X));
        helper.setBlock(
                gearboxPos,
                ModBlocks.ROTATIONAL_GEARBOX.get().defaultBlockState()
                        .setValue(
                                RotationalGearboxBlock.FACING,
                                Direction.EAST));
        helper.setBlock(
                lathePos,
                ModBlocks.LATHE.get().defaultBlockState()
                        .setValue(
                                ProcessingMachineBlock.FACING,
                                Direction.EAST));
        ConfiguredProcessingMachineBlockEntity lathe =
                helper.getBlockEntity(lathePos);
        GTRecipe recipe = ModRecipeMaps.LATHE.entries().stream()
                .map(RecipeMap.Entry::recipe)
                .filter(candidate ->
                        candidate.eut() > 0L && candidate.eut() <= 32L)
                .findFirst()
                .orElseThrow();
        loadRecipeInputs(lathe, recipe);
        IEnergyHandler motor = helper.getLevel().getCapability(
                ModCapabilities.ENERGY,
                helper.absolutePos(motorPos),
                Direction.WEST);
        helper.assertTrue(
                motor != null
                        && lathe.variant().kind().behavior()
                                == ModProcessingMachines.LATHE
                        && lathe.variant().tier().inputNominal() == 32L
                        && lathe.handles(
                                EnergyType.KINETIC_ROTATION,
                                Direction.WEST)
                        && !lathe.handles(
                                EnergyType.KINETIC_PUSH,
                                Direction.WEST),
                "T16b RU lathe vertical did not bind the bronze source tier");

        helper.startSequence()
                .thenExecuteFor(48, () -> {
                    long accepted = motor.insert(
                            EnergyType.ELECTRIC,
                            32L,
                            1L,
                            Direction.WEST,
                            false);
                    helper.assertTrue(
                            accepted == 1L
                                    || motor.stored(EnergyType.ELECTRIC)
                                            >= 32L,
                            "T16b RU motor rejected nominal EU");
                })
                .thenExecute(() -> helper.assertTrue(
                        lathe.progress() > 0,
                        "Motor -> axle -> gearbox RU did not advance "
                                + "the T16b selected lathe: "
                                + lathe.pausedReason()))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 160)
    public static void t16bSteamEnginePowersPressThroughKu(
            GameTestHelper helper) {
        BlockPos enginePos = new BlockPos(3, 2, 5);
        BlockPos pressPos = enginePos.east();
        helper.setBlock(
                enginePos,
                ModBlocks.BRONZE_STEAM_ENGINE.get().defaultBlockState()
                        .setValue(SteamEngineBlock.FACING, Direction.EAST));
        helper.setBlock(
                pressPos,
                ModBlocks.PRESS.get().defaultBlockState()
                        .setValue(
                                ProcessingMachineBlock.FACING,
                                Direction.EAST));
        SteamEngineBlockEntity engine = helper.getBlockEntity(enginePos);
        ConfiguredProcessingMachineBlockEntity press =
                helper.getBlockEntity(pressPos);
        GTRecipe recipe = ModRecipeMaps.PRESS.entries().stream()
                .map(RecipeMap.Entry::recipe)
                .filter(candidate ->
                        candidate.eut() > 0L && candidate.eut() <= 32L)
                .findFirst()
                .orElseThrow();
        loadRecipeInputs(press, recipe);
        IFluidHandler steamInput = engine.fluids(Direction.NORTH);
        helper.assertTrue(
                steamInput != null
                        && steamInput.fill(
                                new FluidStack(
                                        ModFluids.STEAM_SOURCE.get(),
                                        16_000),
                                IFluidHandler.FluidAction.EXECUTE)
                                == 16_000,
                "Could not prime the T16b Press KU vertical");
        helper.assertTrue(
                press.variant().kind().behavior()
                                == ModProcessingMachines.PRESS
                        && press.variant().tier().inputNominal() == 32L
                        && press.variant().tier().parallelLimit() == 4
                        && press.handles(
                                EnergyType.KINETIC_PUSH,
                                Direction.WEST)
                        && !press.handles(
                                EnergyType.KINETIC_ROTATION,
                                Direction.WEST),
                "Press did not bind the bronze KU source tier");

        helper.startSequence()
                .thenIdle(40)
                .thenExecute(() -> helper.assertTrue(
                        press.progress() > 0,
                        "Adjacent steam KU did not advance Press: "
                                + press.pausedReason()))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void t16bHigherTierControlledInjection(
            GameTestHelper helper) {
        BlockPos lathePos = new BlockPos(4, 2, 5);
        BlockPos pressPos = new BlockPos(10, 2, 5);
        helper.setBlock(
                lathePos,
                ModBlocks.TITANIUM_LATHE.get().defaultBlockState()
                        .setValue(
                                ProcessingMachineBlock.FACING,
                                Direction.EAST));
        helper.setBlock(
                pressPos,
                ModBlocks.TITANIUM_PRESS.get().defaultBlockState()
                        .setValue(
                                ProcessingMachineBlock.FACING,
                                Direction.EAST));
        ConfiguredProcessingMachineBlockEntity lathe =
                helper.getBlockEntity(lathePos);
        ConfiguredProcessingMachineBlockEntity press =
                helper.getBlockEntity(pressPos);
        GTRecipe latheRecipe = ModRecipeMaps.LATHE.entries().stream()
                .map(RecipeMap.Entry::recipe)
                .filter(candidate ->
                        candidate.eut() > 0L && candidate.eut() <= 64L)
                .findFirst()
                .orElseThrow();
        GTRecipe pressRecipe = ModRecipeMaps.PRESS.entries().stream()
                .map(RecipeMap.Entry::recipe)
                .filter(candidate ->
                        candidate.eut() > 0L && candidate.eut() <= 64L)
                .findFirst()
                .orElseThrow();
        loadRecipeInputs(lathe, latheRecipe);
        loadRecipeInputs(press, pressRecipe);
        helper.assertTrue(
                lathe.variant().tier().inputNominal() == 512L
                        && press.variant().tier().inputNominal() == 512L
                        && press.variant().tier().parallelLimit() == 16,
                "Controlled injection hosts did not bind titanium tiers");
        helper.assertTrue(
                press.insert(
                                EnergyType.KINETIC_PUSH,
                                2_048L,
                                1L,
                                Direction.WEST,
                                false)
                                == 1L
                        && press.runtime().status().equals("overcharged"),
                "Titanium Press did not expose KU overcharge state");
        helper.assertTrue(
                lathe.insert(
                                EnergyType.KINETIC_ROTATION,
                                512L,
                                2L,
                                Direction.WEST,
                                false)
                                == 2L
                        && press.insert(
                                EnergyType.KINETIC_PUSH,
                                512L,
                                2L,
                                Direction.WEST,
                                false)
                                == 2L,
                "Controlled RU/KU injection did not fill titanium hosts");

        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        lathe.workProgressLong() > 0L
                                && press.workProgressLong() > 0L,
                        "Controlled higher-tier injection did not advance "
                                + "both selected energy identities: RU="
                                + lathe.pausedReason()
                                + ", KU="
                                + press.pausedReason()))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void t17bFireboxesRunAllThreeHuMachineKinds(
            GameTestHelper helper) {
        List<BlockPos> machinePositions = List.of(
                new BlockPos(3, 2, 5),
                new BlockPos(8, 2, 5),
                new BlockPos(13, 2, 5));
        List<Block> blocks = List.of(
                ModBlocks.DISTILLERY.get(),
                ModBlocks.DRYING.get(),
                ModBlocks.SMELTER.get());
        List<ProcessingMachineSpec> specs = List.of(
                ModProcessingMachines.DISTILLERY,
                ModProcessingMachines.DRYING,
                ModProcessingMachines.SMELTER);
        List<GTRecipe> recipes = List.of(
                requireRecipe(
                        ModRecipeMaps.DISTILLERY,
                        "t5/distillery/water_to_water_distilled"),
                requireRecipe(
                        ModRecipeMaps.DRYING,
                        "t5/drying/perlite"),
                requireRecipe(
                        ModRecipeMaps.SMELTER,
                        "t5/smelter/ilmenite"));
        List<ConfiguredProcessingMachineBlockEntity> machines =
                new ArrayList<>();
        for (int index = 0; index < machinePositions.size(); index++) {
            BlockPos machinePos = machinePositions.get(index);
            helper.setBlock(machinePos.below(), ModBlocks.FIREBOX.get());
            FireboxBlockEntity firebox =
                    helper.getBlockEntity(machinePos.below());
            helper.assertTrue(
                    firebox.addFuel(FuelDefinition.COAL_COKE),
                    "Could not fuel T17b HU machine " + index);
            ConfiguredProcessingMachineBlockEntity machine =
                    placeConfigured(
                            helper,
                            machinePos,
                            blocks.get(index),
                            specs.get(index));
            loadRecipeInputs(machine, recipes.get(index));
            machines.add(machine);
        }

        helper.startSequence()
                .thenIdle(4)
                .thenExecute(() -> {
                    for (ConfiguredProcessingMachineBlockEntity machine :
                            machines) {
                        helper.assertTrue(
                                machine.progress() > 0
                                        && machine.spec().energy().type()
                                                == EnergyType.HEAT
                                        && machine.spec().energy().mode()
                                                == ProcessingMachineSpec
                                                        .EnergyMode.ADJACENT,
                                machine.spec().id()
                                        + " did not advance from bottom Firebox HU: "
                                        + machine.pausedReason());
                        forceLastTick(helper, machine);
                    }
                })
                .thenIdle(3)
                .thenExecute(() -> {
                    for (ConfiguredProcessingMachineBlockEntity machine :
                            machines) {
                        boolean fluidOutput = machine.spec().fluids().outputs()
                                .stream()
                                .anyMatch(tank -> !machine.tanks()
                                        .get(tank.index())
                                        .getFluid()
                                        .isEmpty());
                        helper.assertTrue(
                                hasAnyOutput(machine) || fluidOutput,
                                machine.spec().id()
                                        + " did not commit its Firebox-powered output");
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void t17bHigherTierHuUsesControlledAdjacentHandlers(
            GameTestHelper helper) {
        List<BlockPos> machinePositions = List.of(
                new BlockPos(3, 2, 5),
                new BlockPos(8, 2, 5),
                new BlockPos(13, 2, 5));
        List<Block> blocks = List.of(
                ModBlocks.TITANIUM_DISTILLERY.get(),
                ModBlocks.TITANIUM_DRYING.get(),
                ModBlocks.TITANIUM_SMELTER.get());
        List<ProcessingMachineSpec> specs = List.of(
                ModProcessingMachines.DISTILLERY,
                ModProcessingMachines.DRYING,
                ModProcessingMachines.SMELTER);
        List<GTRecipe> recipes = List.of(
                requireRecipe(
                        ModRecipeMaps.DISTILLERY,
                        "t5/distillery/water_to_water_distilled"),
                requireRecipe(
                        ModRecipeMaps.DRYING,
                        "t5/drying/perlite"),
                requireRecipe(
                        ModRecipeMaps.SMELTER,
                        "t5/smelter/ilmenite"));
        FuelDefinition controlledTierThreeHeat =
                new FuelDefinition("t17b_controlled_512_hu", 512L, 200);
        List<ConfiguredProcessingMachineBlockEntity> machines =
                new ArrayList<>();
        for (int index = 0; index < machinePositions.size(); index++) {
            BlockPos machinePos = machinePositions.get(index);
            helper.setBlock(machinePos.below(), ModBlocks.FIREBOX.get());
            FireboxBlockEntity source =
                    helper.getBlockEntity(machinePos.below());
            helper.assertTrue(
                    source.addFuel(controlledTierThreeHeat),
                    "Could not prime controlled tier-3 HU handler " + index);
            ConfiguredProcessingMachineBlockEntity machine =
                    placeConfigured(
                            helper,
                            machinePos,
                            blocks.get(index),
                            specs.get(index));
            loadRecipeInputs(machine, recipes.get(index));
            machines.add(machine);
        }

        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    for (ConfiguredProcessingMachineBlockEntity machine :
                            machines) {
                        helper.assertTrue(
                                machine.variant().tier().inputNominal() == 512L,
                                machine.spec().id()
                                        + " did not bind the titanium HU tier");
                    }
                    ConfiguredProcessingMachineBlockEntity drying =
                            machines.get(1);
                    helper.assertTrue(
                            drying.progress() > 0,
                            "Controlled adjacent HU did not advance "
                                    + "titanium Drying: "
                                    + drying.pausedReason());
                    forceLastTick(helper, drying);
                })
                .thenIdle(3)
                .thenExecute(() -> {
                    for (ConfiguredProcessingMachineBlockEntity machine :
                            machines) {
                        boolean fluidOutput = machine.spec().fluids().outputs()
                                .stream()
                                .anyMatch(tank -> !machine.tanks()
                                        .get(tank.index())
                                        .getFluid()
                                        .isEmpty());
                        helper.assertTrue(
                                hasAnyOutput(machine) || fluidOutput,
                                machine.spec().id()
                                        + " did not complete from controlled "
                                        + "tier-3 adjacent HU");
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void t17bFailureStatesExposeWrongEnergyUnderpoweredAndLimits(
            GameTestHelper helper) {
        BlockPos huPos = new BlockPos(3, 2, 5);
        BlockPos overchargePos = new BlockPos(8, 2, 5);
        BlockPos exceededPos = new BlockPos(13, 2, 5);
        ConfiguredProcessingMachineBlockEntity hu = placeConfigured(
                helper,
                huPos,
                ModBlocks.DISTILLERY.get(),
                ModProcessingMachines.DISTILLERY);
        loadRecipeInputs(
                hu,
                requireRecipe(
                        ModRecipeMaps.DISTILLERY,
                        "t5/distillery/water_to_water_distilled"));
        helper.assertTrue(
                hu.insert(
                        EnergyType.ELECTRIC,
                        32L,
                        1L,
                        Direction.SOUTH,
                        false) == 0L,
                "Adjacent HU Distillery accepted the wrong EU identity");

        helper.setBlock(
                overchargePos,
                ModBlocks.ELECTROLYZER.get().defaultBlockState().setValue(
                        ProcessingMachineBlock.FACING,
                        Direction.EAST));
        ConfiguredProcessingMachineBlockEntity overcharge =
                helper.getBlockEntity(overchargePos);
        helper.assertTrue(
                overcharge.insert(
                        EnergyType.ELECTRIC,
                        overcharge.variant().tier().inputMaximum() + 1L,
                        1L,
                        Direction.WEST,
                        false) == 1L
                        && overcharge.pausedReason().equals("overcharged"),
                "Tier-1 Electrolyzer overcharge was not observable");

        helper.setBlock(
                exceededPos,
                ModBlocks.ELECTROLYZER.get().defaultBlockState().setValue(
                        ProcessingMachineBlock.FACING,
                        Direction.EAST));
        ConfiguredProcessingMachineBlockEntity exceeded =
                helper.getBlockEntity(exceededPos);
        loadRecipeInputs(
                exceeded,
                requireRecipe(
                        ModRecipeMaps.ELECTROLYZER,
                        "t5/electrolyzer/uvarovite"));

        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    helper.assertTrue(
                            hu.pausedReason().equals("underpowered"),
                            "HU machine without bottom adjacency did not expose "
                                    + "underpowered: "
                                    + hu.pausedReason());
                    helper.assertTrue(
                            exceeded.pausedReason().equals(
                                    "recipe_power_exceeded"),
                            "Tier-1 Electrolyzer did not expose "
                                    + "recipe_power_exceeded: "
                                    + exceeded.pausedReason());
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 260)
    public static void smelterAboveFireboxUsesRealHeat(GameTestHelper helper) {
        BlockPos fireboxPos = new BlockPos(4, 1, 4);
        BlockPos smelterPos = fireboxPos.above();
        helper.setBlock(fireboxPos, ModBlocks.FIREBOX.get());
        ConfiguredProcessingMachineBlockEntity smelter =
                placeConfigured(helper, smelterPos, ModBlocks.SMELTER.get(), ModProcessingMachines.SMELTER);
        FireboxBlockEntity firebox = helper.getBlockEntity(fireboxPos);
        helper.assertTrue(
                firebox.addFuel(FuelDefinition.COAL_COKE),
                "Could not fuel real smelter firebox");
        smelter.inventory().setStackInSlot(0, material("copper", MaterialPrefixes.DUST, 1));
        IEnergyHandler heat = helper.getLevel().getCapability(
                ModCapabilities.ENERGY, helper.absolutePos(fireboxPos), Direction.UP);
        helper.assertTrue(heat != null && heat.handles(EnergyType.HEAT, Direction.UP),
                "Firebox UP HEAT capability missing");

        helper.startSequence()
                .thenIdle(4)
                .thenExecute(() -> {
                    helper.assertTrue(smelter.progress() > 0,
                            "Smelter above firebox did not receive real HEAT");
                    forceLastTick(helper, smelter);
                })
                .thenIdle(2)
                .thenExecute(() ->
                        helper.assertTrue(hasAnyOutput(smelter), "Smelter did not complete"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void ceramicMoldRemovalDropsExactlyOnceAcrossPaths(
            GameTestHelper helper) {
        BlockPos nonPlayerDestroy = new BlockPos(2, 2, 2);
        BlockPos emptyReplacement = new BlockPos(5, 2, 2);
        BlockPos filledReplacement = new BlockPos(8, 2, 2);
        BlockPos survivalDestroy = new BlockPos(2, 2, 6);
        BlockPos creativeDestroy = new BlockPos(6, 2, 6);
        placeCeramicMold(helper, nonPlayerDestroy, true);
        placeCeramicMold(helper, emptyReplacement, false);
        placeCeramicMold(helper, filledReplacement, true);
        placeCeramicMold(helper, survivalDestroy, true);
        placeCeramicMold(helper, creativeDestroy, true);

        for (BlockPos pos : List.of(
                nonPlayerDestroy,
                emptyReplacement,
                filledReplacement,
                survivalDestroy,
                creativeDestroy)) {
            helper.assertTrue(
                    droppedItemCount(helper, pos, null) == 0,
                    "Ceramic mold state update dropped items before removal at "
                            + pos);
        }

        helper.getLevel().destroyBlock(
                helper.absolutePos(nonPlayerDestroy), false);
        helper.setBlock(emptyReplacement, Blocks.STONE);
        helper.setBlock(filledReplacement, Blocks.STONE);

        @SuppressWarnings("removal")
        ServerPlayer survivalPlayer = helper.makeMockServerPlayerInLevel();
        survivalPlayer.setGameMode(GameType.SURVIVAL);
        helper.assertTrue(
                survivalPlayer.gameMode.destroyBlock(
                        helper.absolutePos(survivalDestroy)),
                "Survival player could not destroy the ceramic mold");
        survivalPlayer.discard();

        @SuppressWarnings("removal")
        ServerPlayer creativePlayer = helper.makeMockServerPlayerInLevel();
        creativePlayer.setGameMode(GameType.CREATIVE);
        helper.assertTrue(
                creativePlayer.gameMode.destroyBlock(
                        helper.absolutePos(creativeDestroy)),
                "Creative player could not destroy the ceramic mold");
        creativePlayer.discard();

        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    assertCeramicMoldDrops(
                            helper, nonPlayerDestroy, true);
                    assertCeramicMoldDrops(
                            helper, emptyReplacement, false);
                    assertCeramicMoldDrops(
                            helper, filledReplacement, true);
                    assertCeramicMoldDrops(
                            helper, survivalDestroy, true);
                    assertCeramicMoldDrops(
                            helper, creativeDestroy, true);
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 1_350)
    public static void hotIngotSmeltsHurtsAndCoolsThroughDataRule(
            GameTestHelper helper) {
        BlockPos fireboxPos = new BlockPos(4, 1, 4);
        BlockPos smelterPos = fireboxPos.above();
        helper.setBlock(fireboxPos, ModBlocks.FIREBOX.get());
        ConfiguredProcessingMachineBlockEntity smelter =
                placeConfigured(
                        helper,
                        smelterPos,
                        ModBlocks.SMELTER.get(),
                        ModProcessingMachines.SMELTER);
        FireboxBlockEntity firebox = helper.getBlockEntity(fireboxPos);
        helper.assertTrue(
                firebox.addFuel(FuelDefinition.COAL_COKE),
                "Could not fuel T10 hot-ingot smelter");
        smelter.inventory().setStackInSlot(
                0,
                material("copper", MaterialPrefixes.INGOT, 1));
        Item hotItem = MaterialLookup.item(
                        "copper",
                        MaterialPrefixes.INGOT_HOT)
                .orElseThrow();
        Item ordinaryItem = MaterialLookup.item(
                        "copper",
                        MaterialPrefixes.INGOT)
                .orElseThrow();
        int coolingTicks = (int) Math.ceil(Math.max(
                1.0,
                MaterialCatalog.require("copper").thermal().meltingPoint()
                        - ItemHeat.AMBIENT_TEMPERATURE));
        @SuppressWarnings("removal")
        ServerPlayer survivalPlayer = helper.makeMockServerPlayerInLevel();
        survivalPlayer.setGameMode(GameType.SURVIVAL);
        float[] healthBeforeContact = new float[1];

        GameTestSequence sequence = helper.startSequence()
                .thenIdle(4)
                .thenExecute(() -> {
                    helper.assertTrue(
                            smelter.progress() > 0,
                            "T10 hot-ingot route did not start");
                    forceLastTick(helper, smelter);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    ItemStack hot = smelter.inventory().extractItem(
                            smelter.spec().items().outputs().getFirst(),
                            1,
                            false);
                    helper.assertTrue(
                            hot.is(hotItem)
                                    && hot.has(ModComponents.HEAT.get())
                                    && ItemHeat.temperature(
                                            hot,
                                            helper.getLevel().getGameTime()) > 20.0f,
                            "Smelter output is not an independently heated hot ingot");

                    ItemStack creativeHot = new ItemStack(hotItem);
                    helper.assertTrue(
                            !creativeHot.has(ModComponents.HEAT.get()),
                            "Default creative hot ingot unexpectedly carried runtime heat");
                    healthBeforeContact[0] = survivalPlayer.getHealth();
                    helper.assertTrue(
                            survivalPlayer.getInventory().add(hot)
                                    && survivalPlayer.getInventory().add(creativeHot),
                            "Could not place hot ingots into player inventories");
                })
                .thenIdle(85);
        addPlayerTickWindow(sequence, survivalPlayer);
        sequence.thenExecute(() -> {
                    int heatedStacks = inventoryStacksWithHeat(
                            survivalPlayer,
                            hotItem);
                    helper.assertTrue(
                            heatedStacks == 2,
                            "Creative hot ingot was not initialized by inventory maintenance; heated stacks="
                                    + heatedStacks);
                    helper.assertTrue(
                            survivalPlayer.getHealth() < healthBeforeContact[0],
                            "Inventory maintenance did not damage the hot-ingot carrier; health="
                                    + survivalPlayer.getHealth()
                                    + "/"
                                    + healthBeforeContact[0]);
                    survivalPlayer.getAbilities().invulnerable = true;
                })
                .thenIdle(coolingTicks + 25);
        addPlayerTickWindow(sequence, survivalPlayer);
        sequence.thenExecute(() -> {
                    ItemStack ordinary = inventoryStack(
                            survivalPlayer,
                            ordinaryItem);
                    helper.assertTrue(
                            !ordinary.isEmpty()
                                    && inventoryItemCount(
                                                    survivalPlayer,
                                                    ordinaryItem)
                                            == 2
                                    && !ordinary.has(ModComponents.HEAT.get())
                                    && inventoryStack(
                                                    survivalPlayer,
                                                    hotItem)
                                            .isEmpty(),
                            "Inventory maintenance did not cool hot ingots into ordinary ingots");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 420)
    public static void oreChainCopperAcrossPlacedMachines(GameTestHelper helper) {
        runOreChain(helper, "copper");
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 420)
    public static void oreChainTinAcrossPlacedMachines(GameTestHelper helper) {
        runOreChain(helper, "tin");
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 420)
    public static void oreChainIronAcrossPlacedMachines(GameTestHelper helper) {
        runOreChain(helper, "iron");
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 420)
    public static void oreChainGoldAcrossPlacedMachines(GameTestHelper helper) {
        runOreChain(helper, "gold");
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 520)
    public static void tungstenWorldgenLootAndMachineChain(GameTestHelper helper) {
        ItemStack minedRawOre = placeTungstenVeinAndMineRawOre(helper);
        runOreChain(helper, "tungsten", minedRawOre);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 140)
    public static void t11CrudeOilDepositExtractsThroughPipeAndDistills(
            GameTestHelper helper) {
        BlockPos depositPos = new BlockPos(3, 1, 4);
        BlockPos extractorPos = depositPos.above();
        BlockPos pipePos = extractorPos.east();
        BlockPos distilleryPos = pipePos.east();
        helper.setBlock(
                depositPos,
                ModBlocks.SUBSURFACE_FLUID_DEPOSIT.get());
        SubsurfaceFluidDepositBlockEntity deposit =
                helper.getBlockEntity(depositPos);
        var material = ResourceLocation.fromNamespaceAndPath(
                CrucibleCraft.MODID, "crude_oil");
        var production = HydrocarbonRuntimePolicy.production(material);
        deposit.initialize(
                material,
                1_000_000L,
                BuiltInRegistries.BLOCK.getKey(Blocks.STONE),
                production.amountMb(),
                production.intervalTicks(),
                production.accumulationCapMb(),
                production.ventOverflow());
        long projectionStart = helper.getLevel().getGameTime();
        helper.assertTrue(
                deposit.extract(1, projectionStart, false) == 1,
                "Could not initialize the production projection");
        long saturationTime = projectionStart
                + 40L * production.intervalTicks();
        helper.assertTrue(
                deposit.availableProduction(saturationTime)
                        == production.accumulationCapMb(),
                "Deposit projection did not saturate at its accumulation cap");
        helper.assertTrue(
                deposit.extract(10, saturationTime, false) == 10
                        && deposit.availableProduction(
                                        saturationTime
                                                + production.intervalTicks())
                                == production.accumulationCapMb(),
                "Partial headroom allowed deposit projection to exceed its cap");
        helper.setBlock(
                extractorPos,
                ModBlocks.FLUID_DEPOSIT_EXTRACTOR.get());
        FluidPipeBlock pipeBlock = (FluidPipeBlock) ModBlocks.pipeBlock(
                "copper",
                MaterialPrefixes.TINY_FLUID_PIPE,
                PipeCatalog.Kind.FLUID).get();
        helper.setBlock(pipePos, pipeBlock);
        helper.getLevel().setBlock(
                helper.absolutePos(pipePos),
                helper.getBlockState(pipePos)
                        .setValue(FluidPipeBlock.WEST, true)
                        .setValue(FluidPipeBlock.EAST, true),
                Block.UPDATE_CLIENTS);
        helper.setBlock(
                distilleryPos.below(),
                ModBlocks.FIREBOX.get());
        helper.setBlock(
                distilleryPos,
                ModBlocks.DISTILLERY.get().defaultBlockState()
                        .setValue(
                                ProcessingMachineBlock.FACING,
                                Direction.EAST));
        FluidPipeBlockEntity pipe = helper.getBlockEntity(pipePos);
        helper.assertTrue(
                pipe.setCover(Direction.WEST, PipeCover.pump()),
                "Could not install the T11 wellhead pump");
        ConfiguredProcessingMachineBlockEntity distillery =
                helper.getBlockEntity(distilleryPos);
        FireboxBlockEntity firebox =
                helper.getBlockEntity(distilleryPos.below());
        helper.assertTrue(
                firebox.addFuel(FuelDefinition.COAL_COKE),
                "Could not fuel the T11 HU distillery");
        GTRecipe recipe = requireRecipe(
                ModRecipeMaps.DISTILLERY,
                "t11/distillery/crude_oil_to_fuel_and_lubricant");
        helper.assertTrue(
                distillery.spec().energy().type() == EnergyType.HEAT
                        && distillery.spec().energy().mode()
                                == ProcessingMachineSpec.EnergyMode.ADJACENT,
                "T11 distillery did not bind the adjacent HU contract");

        helper.startSequence()
                .thenIdle(55)
                .thenExecute(() -> {
                    helper.assertTrue(
                            distillery.progress() > 0,
                            "Deposit/pipe/HU distillery chain did not start: "
                                    + distillery.pausedReason());
                    forceLastTick(helper, distillery);
                })
                .thenIdle(3)
                .thenExecute(() -> {
                    FluidStack fuel = recipe.fluidOutputs().getFirst();
                    FluidStack lubricant = recipe.fluidOutputs().get(1);
                    helper.assertTrue(
                            distillery.tanks().stream().anyMatch(tank ->
                                    tank.getFluid().is(fuel.getFluid())
                                            && tank.getFluidAmount()
                                                    >= fuel.getAmount()),
                            "Deposit/pipe/distillery chain produced no fuel");
                    helper.assertTrue(
                            distillery.tanks().stream().anyMatch(tank ->
                                    tank.getFluid().is(
                                                    lubricant.getFluid())
                                            && tank.getFluidAmount()
                                                    >= lubricant.getAmount()),
                            "Distillery silently lost lubricant coproduct");
                    helper.assertTrue(
                            deposit.snapshot().orElseThrow()
                                            .remainingAmountMb()
                                    == 1_000_000L,
                            "T11 extraction consumed the legacy reserve");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 180)
    public static void t11NaturalGasConvertsAndPowersElectricGrid(
            GameTestHelper helper) {
        BlockPos depositPos = new BlockPos(3, 1, 9);
        BlockPos extractorPos = depositPos.above();
        BlockPos generifierPos = new BlockPos(5, 2, 9);
        BlockPos generatorPos = new BlockPos(9, 2, 9);
        BlockPos waterExhaustPos = generatorPos.above();
        BlockPos carbonDioxideExhaustPos = generatorPos.below();
        BlockPos cablePos = generatorPos.east();
        BlockPos electrolyzerPos = cablePos.east();
        helper.setBlock(
                depositPos,
                ModBlocks.SUBSURFACE_FLUID_DEPOSIT.get());
        SubsurfaceFluidDepositBlockEntity deposit =
                helper.getBlockEntity(depositPos);
        var naturalGasId = ResourceLocation.fromNamespaceAndPath(
                CrucibleCraft.MODID, "natural_gas");
        var production =
                HydrocarbonRuntimePolicy.production(naturalGasId);
        deposit.initialize(
                naturalGasId,
                500_000L,
                BuiltInRegistries.BLOCK.getKey(Blocks.STONE),
                production.amountMb(),
                production.intervalTicks(),
                production.accumulationCapMb(),
                production.ventOverflow());
        helper.setBlock(
                extractorPos,
                ModBlocks.FLUID_DEPOSIT_EXTRACTOR.get());
        helper.setBlock(
                generifierPos,
                ModBlocks.GENERIFIER.get().defaultBlockState()
                        .setValue(
                                ProcessingMachineBlock.FACING,
                                Direction.EAST));
        helper.setBlock(
                generatorPos,
                ModBlocks.BURNING_GAS_GENERATOR.get()
                        .defaultBlockState()
                        .setValue(
                                FuelGeneratorBlock.FACING,
                                Direction.EAST));
        FluidPipeBlock exhaustPipeBlock =
                (FluidPipeBlock) ModBlocks.pipeBlock(
                        "copper",
                        MaterialPrefixes.TINY_FLUID_PIPE,
                        PipeCatalog.Kind.FLUID).get();
        helper.setBlock(waterExhaustPos, exhaustPipeBlock);
        helper.getLevel().setBlock(
                helper.absolutePos(waterExhaustPos),
                helper.getBlockState(waterExhaustPos)
                        .setValue(FluidPipeBlock.DOWN, true),
                Block.UPDATE_CLIENTS);
        helper.setBlock(carbonDioxideExhaustPos, exhaustPipeBlock);
        helper.getLevel().setBlock(
                helper.absolutePos(carbonDioxideExhaustPos),
                helper.getBlockState(carbonDioxideExhaustPos)
                        .setValue(FluidPipeBlock.UP, true),
                Block.UPDATE_CLIENTS);
        CableBlock cable = ModBlocks.electricalConductorBlock(
                "copper", MaterialPrefixes.CABLE).get();
        helper.setBlock(
                cablePos,
                conductorState(
                        cable, Direction.WEST, Direction.EAST));
        helper.setBlock(
                electrolyzerPos,
                ModBlocks.ELECTROLYZER.get().defaultBlockState()
                        .setValue(
                                ProcessingMachineBlock.FACING,
                                Direction.EAST));

        FluidDepositExtractorBlockEntity extractor =
                helper.getBlockEntity(extractorPos);
        ConfiguredProcessingMachineBlockEntity generifier =
                helper.getBlockEntity(generifierPos);
        FuelGeneratorBlockEntity generator =
                helper.getBlockEntity(generatorPos);
        FluidPipeBlockEntity waterExhaust =
                helper.getBlockEntity(waterExhaustPos);
        FluidPipeBlockEntity carbonDioxideExhaust =
                helper.getBlockEntity(carbonDioxideExhaustPos);
        helper.assertTrue(
                waterExhaust.setCover(Direction.DOWN, PipeCover.pump())
                        && carbonDioxideExhaust.setCover(
                                Direction.UP, PipeCover.pump()),
                "Could not install independent generator exhaust pumps");
        helper.assertTrue(
                generator.fluids(Direction.EAST) == null
                        && generator.fluids(Direction.UP) != null
                        && generator.fluids(Direction.DOWN) != null,
                "Generator electrical and exhaust faces overlap");
        ConfiguredProcessingMachineBlockEntity electrolyzer =
                helper.getBlockEntity(electrolyzerPos);
        GTRecipe electrolysis = requireRecipe(
                ModRecipeMaps.ELECTROLYZER,
                "t5/electrolyzer/salt");
        GTRecipe gasFuel = requireRecipe(
                ModRecipeMaps.FUELS_GAS,
                "t11/fuels_gas/methane");
        loadRecipeInputs(electrolyzer, electrolysis);

        helper.startSequence()
                .thenIdle(5)
                .thenExecute(() -> helper.assertTrue(
                        extractor.fluidAmount() >= 5,
                        "Natural-gas extractor did not capture its first "
                                + "production cycle: status="
                                + extractor.status()
                                + ", linked="
                                + extractor.linkedDeposit()
                                + ", depositAvailable="
                                + deposit.availableProduction(
                                        helper.getLevel().getGameTime())))
                .thenExecute(() -> transferFluid(
                        helper,
                        extractor.externalFluid(),
                        generifier.fluids(Direction.WEST),
                        5))
                .thenIdle(7)
                .thenExecute(() -> transferFluid(
                        helper,
                        generifier.fluids(Direction.EAST),
                        generator.fluids(Direction.WEST),
                        5))
                .thenIdle(45)
                .thenExecute(() -> {
                    FluidStack expectedWater =
                            gasFuel.fluidOutputs().getFirst();
                    FluidStack expectedCarbonDioxide =
                            gasFuel.fluidOutputs().get(1);
                    helper.assertTrue(
                            waterExhaust.storedFluid().is(
                                            expectedWater.getFluid())
                                    && waterExhaust.storedFluid().getAmount()
                                            >= expectedWater.getAmount(),
                            "Top exhaust did not drain methane-generator water");
                    helper.assertTrue(
                            carbonDioxideExhaust.storedFluid().is(
                                            expectedCarbonDioxide.getFluid())
                                    && carbonDioxideExhaust.storedFluid()
                                                    .getAmount()
                                            >= expectedCarbonDioxide.getAmount(),
                            "Bottom exhaust did not drain methane-generator CO2");
                    helper.assertTrue(
                            electrolyzer.workProgressLong() > 0L,
                            "Gas generator did not power the Electrolyzer "
                                    + "through the T6 cable: "
                                    + electrolyzer.pausedReason());
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void t11NaturalGasLeaksAndFlammableCloudIgnites(
            GameTestHelper helper) {
        BlockPos pipePos = new BlockPos(14, 2, 5);
        FluidPipeBlock pipeBlock = (FluidPipeBlock) ModBlocks.pipeBlock(
                "tin",
                MaterialPrefixes.TINY_FLUID_PIPE,
                PipeCatalog.Kind.FLUID).get();
        helper.setBlock(
                pipePos.west(),
                ModBlocks.FLUID_DEPOSIT_EXTRACTOR.get());
        helper.setBlock(pipePos, pipeBlock);
        helper.getLevel().setBlock(
                helper.absolutePos(pipePos),
                helper.getBlockState(pipePos)
                        .setValue(FluidPipeBlock.WEST, true),
                Block.UPDATE_CLIENTS);
        helper.assertTrue(
                helper.getBlockState(pipePos)
                        .getValue(FluidPipeBlock.WEST),
                "Unsafe pipe test did not establish an input connection");
        FluidPipeBlockEntity pipe = helper.getBlockEntity(pipePos);
        IFluidHandler pipeInput = pipe.fluidHandler(Direction.WEST);
        FluidStack naturalGas = new FluidStack(
                ModFluids.materialFluid("natural_gas").orElseThrow(),
                5);
        for (int event = 0;
                event < FluidPipeFailureState.FAILURE_LIMIT;
                event++) {
            helper.assertTrue(
                    pipeInput.fill(
                                    naturalGas,
                                    IFluidHandler.FluidAction.EXECUTE)
                            == 0,
                    "Unsafe tin pipe accepted natural gas");
        }
        helper.assertTrue(
                pipe.failureSnapshot().pendingFailure()
                        == Failure.GAS_LEAK,
                "Unsafe pipe did not record a natural-gas leak: "
                        + pipe.failureSnapshot());

        BlockPos cloudPos = new BlockPos(17, 2, 5);
        var naturalGasId = ResourceLocation.fromNamespaceAndPath(
                CrucibleCraft.MODID, "natural_gas");
        helper.assertTrue(
                GasCloudBlockEntity.placeOrMerge(
                                helper.getLevel(),
                                helper.absolutePos(cloudPos),
                                naturalGasId,
                                125)
                        == 125,
                "Could not create a bounded natural-gas cloud");
        helper.setBlock(cloudPos.east().below(), Blocks.NETHERRACK);
        helper.setBlock(cloudPos.east(), Blocks.FIRE);

        helper.startSequence()
                .thenIdle(6)
                .thenExecute(() -> {
                    GasCloudBlockEntity cloud =
                            helper.getBlockEntity(cloudPos);
                    helper.assertTrue(
                            cloud.burning()
                                    && cloud.amountMb() < 125,
                            "PROPERTIES.FLAMMABLE did not drive sustained "
                                    + "gas-cloud burning");
                    helper.assertTrue(
                            !helper.getBlockState(pipePos).is(pipeBlock),
                            "Pending gas leak did not fail the unsafe pipe");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void worldgenCatalogRegistryPlacementAndFluidDeposit(
            GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Registry<ConfiguredFeature<?, ?>> registry =
                level.registryAccess().registryOrThrow(
                        Registries.CONFIGURED_FEATURE);
        List<ResourceLocation> worldgenVeins = registry.keySet().stream()
                .filter(id -> id.getNamespace().equals(CrucibleCraft.MODID))
                .filter(id -> id.getPath().startsWith("large_")
                        && id.getPath().endsWith("_vein"))
                .sorted()
                .toList();
        helper.assertTrue(
                worldgenVeins.size() == 134,
                "Runtime configured-feature registry has "
                        + worldgenVeins.size() + " / 134 catalog veins");
        for (ResourceLocation id : worldgenVeins) {
            ConfiguredFeature<?, ?> configured = registry.get(
                    ResourceKey.create(Registries.CONFIGURED_FEATURE, id));
            helper.assertTrue(
                    configured != null
                            && configured.config()
                                    instanceof LargeVeinConfiguration,
                    id + " did not decode as a large vein");
            assertLargeVeinPlaces(
                    helper,
                    configured,
                    (LargeVeinConfiguration) configured.config());
        }

        ResourceLocation oilId = ResourceLocation.fromNamespaceAndPath(
                CrucibleCraft.MODID, "crude_oil_deposit");
        ConfiguredFeature<?, ?> oil = registry.get(
                ResourceKey.create(Registries.CONFIGURED_FEATURE, oilId));
        helper.assertTrue(
                oil != null
                        && oil.config()
                                instanceof SubsurfaceFluidDepositConfiguration,
                "Runtime registry lacks the decoded crude-oil deposit");
        assertFluidDepositPlacesAndIsReadable(
                helper,
                oil,
                (SubsurfaceFluidDepositConfiguration) oil.config());
        ResourceLocation naturalGasId =
                ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID,
                        "natural_gas_deposit");
        ConfiguredFeature<?, ?> naturalGas = registry.get(
                ResourceKey.create(
                        Registries.CONFIGURED_FEATURE,
                        naturalGasId));
        helper.assertTrue(
                naturalGas != null
                        && naturalGas.config()
                                instanceof SubsurfaceFluidDepositConfiguration,
                "Runtime registry lacks the decoded natural-gas deposit");
        assertFluidDepositPlacesAndIsReadable(
                helper,
                naturalGas,
                (SubsurfaceFluidDepositConfiguration)
                        naturalGas.config());
        helper.succeed();
    }

    private static void assertLargeVeinPlaces(
            GameTestHelper helper,
            ConfiguredFeature<?, ?> configured,
            LargeVeinConfiguration config) {
        ServerLevel level = helper.getLevel();
        AcceptedRegion selected = acceptedRegion(
                helper,
                config.regionSizeChunks(),
                config.generationChance(),
                config.salt());
        long veinSeed = LargeVeinLayout.veinSeed(
                level.getSeed(), selected.anchor(), config.salt());
        int centerX = selected.anchor().x() * 16 + 8;
        int centerZ = selected.anchor().z() * 16 + 8;
        int centerY = LargeVeinLayout.centerY(
                veinSeed, config.minY(), config.maxY());
        for (int x = centerX - config.horizontalRadius();
                x <= centerX + config.horizontalRadius();
                x++) {
            for (int z = centerZ - config.horizontalRadius();
                    z <= centerZ + config.horizontalRadius();
                    z++) {
                for (int y = centerY - config.verticalRadius();
                        y <= centerY + config.verticalRadius();
                        y++) {
                    level.setBlock(
                            new BlockPos(x, y, z),
                            Blocks.STONE.defaultBlockState(),
                            2);
                }
            }
        }
        helper.assertTrue(
                configured.place(
                        level,
                        level.getChunkSource().getGenerator(),
                        RandomSource.create(veinSeed),
                        new BlockPos(
                                selected.anchor().x() * 16,
                                centerY,
                                selected.anchor().z() * 16)),
                "Catalog configured feature failed real placement for salt "
                        + config.salt());
    }

    private static void assertFluidDepositPlacesAndIsReadable(
            GameTestHelper helper,
            ConfiguredFeature<?, ?> configured,
            SubsurfaceFluidDepositConfiguration config) {
        ServerLevel level = helper.getLevel();
        AcceptedRegion selected = acceptedRegion(
                helper,
                config.regionSizeChunks(),
                config.generationChance(),
                config.salt());
        long depositSeed = LargeVeinLayout.veinSeed(
                level.getSeed(), selected.anchor(), config.salt());
        int centerY = LargeVeinLayout.centerY(
                depositSeed, config.minY(), config.maxY());
        BlockPos target = new BlockPos(
                selected.anchor().x() * 16 + 8,
                centerY,
                selected.anchor().z() * 16 + 8);
        level.setBlock(target, Blocks.STONE.defaultBlockState(), 2);
        helper.assertTrue(
                configured.place(
                        level,
                        level.getChunkSource().getGenerator(),
                        RandomSource.create(depositSeed),
                        new BlockPos(
                                selected.anchor().x() * 16,
                                centerY,
                                selected.anchor().z() * 16)),
                "Crude-oil configured feature failed real placement");
        helper.assertTrue(
                level.getBlockEntity(target)
                        instanceof SubsurfaceFluidDepositBlockEntity,
                "Placed crude-oil deposit has no readable block entity");
        SubsurfaceFluidDepositBlockEntity deposit =
                (SubsurfaceFluidDepositBlockEntity) level.getBlockEntity(target);
        var snapshot = deposit.snapshot().orElse(null);
        helper.assertTrue(
                snapshot != null
                        && snapshot.material().equals(config.material())
                        && snapshot.replacedHost().equals(
                                BuiltInRegistries.BLOCK.getKey(Blocks.STONE))
                        && snapshot.initialAmountMb()
                                >= config.minAmountMb()
                        && snapshot.initialAmountMb()
                                <= config.maxAmountMb()
                        && snapshot.remainingAmountMb()
                                == snapshot.initialAmountMb()
                        && snapshot.productionAmountMb()
                                == config.productionAmountMb()
                        && snapshot.productionIntervalTicks()
                                == config.productionIntervalTicks()
                        && snapshot.accumulationCapMb()
                                == config.accumulationCapMb()
                        && snapshot.ventOverflow()
                                == config.ventOverflow(),
                "Placed fluid deposit data is incomplete or out of range");
    }

    private static AcceptedRegion acceptedRegion(
            GameTestHelper helper,
            int regionSizeChunks,
            float generationChance,
            int salt) {
        ServerLevel level = helper.getLevel();
        ChunkPos testChunk =
                new ChunkPos(helper.absolutePos(BlockPos.ZERO));
        int baseRegionX =
                Math.floorDiv(testChunk.x, regionSizeChunks) + 64;
        int baseRegionZ =
                Math.floorDiv(testChunk.z, regionSizeChunks) + 64;
        for (int index = 0; index < 4_096; index++) {
            int regionX = baseRegionX + index % 64;
            int regionZ = baseRegionZ + index / 64;
            if (LargeVeinLayout.generationRoll(
                    level.getSeed(), regionX, regionZ, salt)
                    < generationChance) {
                return new AcceptedRegion(
                        regionX,
                        regionZ,
                        LargeVeinLayout.anchor(
                                level.getSeed(),
                                regionX,
                                regionZ,
                                regionSizeChunks,
                                salt));
            }
        }
        throw new IllegalStateException(
                "No deterministic test region accepted for salt " + salt);
    }

    private record AcceptedRegion(
            int regionX,
            int regionZ,
            LargeVeinLayout.Anchor anchor) {}

    private static void runOreChain(GameTestHelper helper, String materialId) {
        runOreChain(
                helper,
                materialId,
                material(materialId, MaterialPrefixes.RAW_ORE, 1));
    }

    private static void runOreChain(
            GameTestHelper helper, String materialId, ItemStack rawOreInput) {
        assertPublishedOreChain(helper, materialId);
        ItemStack expectedRawOre =
                material(materialId, MaterialPrefixes.RAW_ORE, 1);
        helper.assertTrue(
                ItemStack.isSameItemSameComponents(rawOreInput, expectedRawOre),
                materialId + " chain input is not its canonical raw ore");
        List<ProcessingMachineSpec> specs = List.of(
                ModProcessingMachines.SLUICE,
                ModProcessingMachines.CENTRIFUGE,
                ModProcessingMachines.SHREDDER,
                ModProcessingMachines.SIFTER,
                ModProcessingMachines.SMELTER);
        List<Block> blocks = List.of(
                ModBlocks.SLUICE.get(),
                ModBlocks.CENTRIFUGE.get(),
                ModBlocks.SHREDDER.get(),
                ModBlocks.SIFTER.get(),
                ModBlocks.SMELTER.get());
        List<ConfiguredProcessingMachineBlockEntity> machines = new java.util.ArrayList<>();
        for (int i = 0; i < specs.size(); i++) {
            machines.add(placeConfigured(
                    helper, new BlockPos(3 + i * 4, 2, 8), blocks.get(i), specs.get(i)));
        }
        BlockPos chainFireboxPos = new BlockPos(19, 1, 8);
        helper.setBlock(chainFireboxPos, ModBlocks.FIREBOX.get());
        FireboxBlockEntity chainFirebox = helper.getBlockEntity(chainFireboxPos);
        helper.assertTrue(chainFirebox.addFuel(FuelDefinition.COAL_COKE),
                "Could not fuel chain smelter firebox");
        CrusherBlockEntity crusher;
        BlockPos crusherPos = new BlockPos(3, 2, 13);
        helper.setBlock(crusherPos, ModBlocks.BRONZE_CRUSHER.get());
        crusher = helper.getBlockEntity(crusherPos);
        rawOreInput.setCount(1);
        crusher.inventory().setStackInSlot(0, rawOreInput);
        fillKuCapability(helper, crusherPos);

        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    forceLastTick(helper, crusher);
                    fillKuCapability(helper, crusherPos);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    ItemStack crushed = crusher.inventory().extractItem(1, 1, false);
                    helper.assertTrue(!crushed.isEmpty(), "Chain crusher output missing");
                    machines.get(0).inventory().setStackInSlot(0, crushed);
                    fillWater(machines.get(0));
                    fillKu(helper, machines.get(0));
                })
                .thenIdle(3)
                .thenExecute(() -> {
                    forceLastTick(helper, machines.get(0));
                    fillKu(helper, machines.get(0));
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    transferPrimary(helper, machines.get(0), machines.get(1));
                    fillKu(helper, machines.get(1));
                })
                .thenIdle(3)
                .thenExecute(() -> {
                    forceLastTick(helper, machines.get(1));
                    fillKu(helper, machines.get(1));
                })
                .thenIdle(2)
                .thenExecute(() -> transferPrimary(helper, machines.get(1), machines.get(2)))
                .thenExecute(() -> fillKu(helper, machines.get(2)))
                .thenIdle(3)
                .thenExecute(() -> {
                    forceLastTick(helper, machines.get(2));
                    fillKu(helper, machines.get(2));
                })
                .thenIdle(2)
                .thenExecute(() -> transferPrimary(helper, machines.get(2), machines.get(3)))
                .thenExecute(() -> fillKu(helper, machines.get(3)))
                .thenIdle(3)
                .thenExecute(() -> {
                    forceLastTick(helper, machines.get(3));
                    fillKu(helper, machines.get(3));
                })
                .thenIdle(2)
                .thenExecute(() -> transferPrimary(helper, machines.get(3), machines.get(4)))
                .thenIdle(4)
                .thenExecute(() -> forceLastTick(helper, machines.get(4)))
                .thenIdle(2)
                .thenExecute(() -> {
                    ItemStack expected = material(
                            materialId, MaterialPrefixes.INGOT, 1);
                    helper.assertTrue(
                            machines.get(4).spec().items().outputs().stream()
                                    .map(slot -> machines.get(4).inventory()
                                            .getStackInSlot(slot))
                                    .anyMatch(stack -> stack.is(expected.getItem())),
                            materialId + " chain did not reach its ingot");
                })
                .thenSucceed();
    }

    private static ItemStack placeTungstenVeinAndMineRawOre(
            GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ResourceLocation featureId = ResourceLocation.fromNamespaceAndPath(
                CrucibleCraft.MODID, "large_tungsten_vein");
        Registry<ConfiguredFeature<?, ?>> registry =
                level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        ResourceKey<ConfiguredFeature<?, ?>> featureKey =
                ResourceKey.create(Registries.CONFIGURED_FEATURE, featureId);
        ConfiguredFeature<?, ?> configured = registry.get(featureKey);
        helper.assertTrue(
                configured != null,
                "Runtime configured-feature registry lacks " + featureId);
        helper.assertTrue(
                configured.config() instanceof LargeVeinConfiguration,
                featureId + " did not decode as a large vein");
        LargeVeinConfiguration config =
                (LargeVeinConfiguration) configured.config();

        ChunkPos testChunk = new ChunkPos(helper.absolutePos(BlockPos.ZERO));
        int baseRegionX = Math.floorDiv(testChunk.x, config.regionSizeChunks()) + 64;
        int baseRegionZ = Math.floorDiv(testChunk.z, config.regionSizeChunks()) + 64;
        int selectedRegionX = 0;
        int selectedRegionZ = 0;
        LargeVeinLayout.Anchor anchor = null;
        for (int index = 0; index < 256 && anchor == null; index++) {
            int regionX = baseRegionX + index % 16;
            int regionZ = baseRegionZ + index / 16;
            if (LargeVeinLayout.generationRoll(
                    level.getSeed(), regionX, regionZ, config.salt())
                    < config.generationChance()) {
                selectedRegionX = regionX;
                selectedRegionZ = regionZ;
                anchor = LargeVeinLayout.anchor(
                        level.getSeed(),
                        regionX,
                        regionZ,
                        config.regionSizeChunks(),
                        config.salt());
            }
        }
        helper.assertTrue(anchor != null, "No deterministic tungsten test region accepted");
        LargeVeinLayout.Anchor selectedAnchor = anchor;
        long veinSeed =
                LargeVeinLayout.veinSeed(level.getSeed(), selectedAnchor, config.salt());
        int centerX = selectedAnchor.x() * 16 + 8;
        int centerZ = selectedAnchor.z() * 16 + 8;
        int centerY =
                LargeVeinLayout.centerY(veinSeed, config.minY(), config.maxY());

        for (int x = centerX - config.horizontalRadius();
                x <= centerX + config.horizontalRadius();
                x++) {
            for (int z = centerZ - config.horizontalRadius();
                    z <= centerZ + config.horizontalRadius();
                    z++) {
                BlockState host = Math.floorMod(x + z, 2) == 0
                        ? Blocks.STONE.defaultBlockState()
                        : Blocks.DEEPSLATE.defaultBlockState();
                for (int y = centerY - config.verticalRadius();
                        y <= centerY + config.verticalRadius();
                        y++) {
                    level.setBlock(new BlockPos(x, y, z), host, 2);
                }
            }
        }

        boolean placed = configured.place(
                level,
                level.getChunkSource().getGenerator(),
                RandomSource.create(veinSeed),
                new BlockPos(
                        selectedAnchor.x() * 16,
                        centerY,
                        selectedAnchor.z() * 16));
        helper.assertTrue(
                placed,
                "Runtime configured feature did not place at recomputed anchor "
                        + selectedRegionX + "," + selectedRegionZ);

        Block stoneOre = ModBlocks.oreBlock("tungsten", Host.STONE).get();
        Block deepslateOre = ModBlocks.oreBlock("tungsten", Host.DEEPSLATE).get();
        int stoneCount = 0;
        int deepslateCount = 0;
        BlockPos minedPos = null;
        for (int x = centerX - config.horizontalRadius();
                x <= centerX + config.horizontalRadius();
                x++) {
            for (int z = centerZ - config.horizontalRadius();
                    z <= centerZ + config.horizontalRadius();
                    z++) {
                for (int y = centerY - config.verticalRadius();
                        y <= centerY + config.verticalRadius();
                        y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    Block block = level.getBlockState(pos).getBlock();
                    if (block != stoneOre && block != deepslateOre) {
                        continue;
                    }
                    boolean expectedStone = Math.floorMod(x + z, 2) == 0;
                    helper.assertTrue(
                            block == (expectedStone ? stoneOre : deepslateOre),
                            "Tungsten ore host adaptation disagrees with replaced host at "
                                    + pos);
                    if (block == stoneOre) {
                        stoneCount++;
                    } else {
                        deepslateCount++;
                    }
                    if (minedPos == null) {
                        minedPos = pos.immutable();
                    }
                }
            }
        }
        helper.assertTrue(
                stoneCount > 0 && deepslateCount > 0,
                "Runtime tungsten vein did not adapt both stone and deepslate hosts");
        helper.assertTrue(minedPos != null, "Runtime tungsten vein placed no ore");

        BlockState minedState = level.getBlockState(minedPos);
        List<ItemStack> drops = Block.getDrops(
                minedState,
                level,
                minedPos,
                level.getBlockEntity(minedPos),
                null,
                new ItemStack(Items.DIAMOND_PICKAXE));
        ItemStack expectedRaw =
                material("tungsten", MaterialPrefixes.RAW_ORE, 1);
        ItemStack actualRaw = drops.stream()
                .filter(stack ->
                        ItemStack.isSameItemSameComponents(stack, expectedRaw))
                .findFirst()
                .map(ItemStack::copy)
                .orElse(ItemStack.EMPTY);
        helper.assertTrue(
                !actualRaw.isEmpty(),
                "Real tungsten ore loot table did not drop canonical raw ore: " + drops);
        helper.assertTrue(
                drops.stream().allMatch(stack -> stack.is(expectedRaw.getItem())),
                "Tungsten ore loot included a non-raw-ore entry: " + drops);
        level.setBlock(minedPos, Blocks.AIR.defaultBlockState(), 3);
        actualRaw.setCount(1);
        return actualRaw;
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 320)
    public static void assemblerCableAtomicRollbackAndNbt(GameTestHelper helper) {
        BlockPos pos = new BlockPos(5, 2, 5);
        ConfiguredProcessingMachineBlockEntity assembler =
                placeConfigured(helper, pos, ModBlocks.ASSEMBLER.get(), ModProcessingMachines.ASSEMBLER);
        RecipeMap.Entry cable = ModRecipeMaps.ASSEMBLER.entries().stream()
                .filter(entry -> entry.id().getPath().contains(
                        "wire_and_rubber_to_cable/copper"))
                .findFirst().orElseThrow();
        loadRecipeInputs(assembler, cable.recipe());
        fillKuCapability(helper, pos);

        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    helper.assertTrue(assembler.duration() > 0, "Cable recipe not selected");
                    CompoundTag saved = assembler.saveWithoutMetadata(
                            helper.getLevel().registryAccess());
                    helper.assertTrue(saved.contains("selected_recipe_fingerprint")
                                    && saved.contains("rolled_outputs_valid"),
                            "Selected chance outputs were not persisted");
                    int persistedProgress = assembler.progress();
                    helper.assertTrue(saved.contains("inventory")
                                    && saved.getLong("energy") > 0L,
                            "Capability mutations were not present in saved state");
                    assembler.loadWithComponents(saved, helper.getLevel().registryAccess());
                    helper.assertTrue(assembler.progress() == persistedProgress
                                    && !assembler.inventory().getStackInSlot(0).isEmpty()
                                    && !assembler.inventory().getStackInSlot(1).isEmpty(),
                            "Machine NBT load did not restore progress and inputs");
                    CompoundTag update = assembler.getUpdateTag(
                            helper.getLevel().registryAccess());
                    helper.assertTrue(update.contains("tank_count")
                                    && update.contains("status")
                                    && update.contains("power_demand"),
                            "Client update tag is missing symmetric processing keys");
                    assembler.inventory().setStackInSlot(
                            assembler.spec().items().outputs().getFirst(),
                            new ItemStack(Items.BEDROCK, 64));
                    forceLastTick(helper, assembler);
                    fillKuCapability(helper, pos);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(!assembler.inventory().getStackInSlot(0).isEmpty()
                                    && !assembler.inventory().getStackInSlot(1).isEmpty(),
                            "Blocked assembler partially consumed cable inputs");
                    assembler.inventory().setStackInSlot(
                            assembler.spec().items().outputs().getFirst(), ItemStack.EMPTY);
                    forceLastTick(helper, assembler);
                    fillKuCapability(helper, pos);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(assembler.inventory().getStackInSlot(0).isEmpty()
                                    && assembler.inventory().getStackInSlot(1).isEmpty(),
                            "Assembler did not atomically consume cable inputs");
                    helper.assertTrue(hasAnyOutput(assembler), "Assembler produced no cable");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void t4RepresentativePickaxesExposeMaterialComponents(
            GameTestHelper helper) {
        var pickaxeItem = ModItems.MATERIAL_PICKAXE.get();
        for (String material : List.of("iron", "diamond", "oak", "stone")) {
            ItemStack pickaxe = pickaxeItem.variant(material);
            helper.assertTrue(
                    material.equals(pickaxe.get(ModComponents.TOOL_MATERIAL)),
                    material + " pickaxe lost its material component");
            helper.assertTrue(
                    pickaxe.getMaxDamage()
                            == ToolMaterialRules.durability(
                                    ToolMaterialRules.ToolKind.PICKAXE,
                                    material),
                    material + " pickaxe durability does not follow T4 policy");
            helper.assertTrue(
                    pickaxe.getComponentsPatch().entrySet().stream()
                            .noneMatch(entry ->
                                    entry.getKey() == DataComponents.MAX_DAMAGE
                                    || entry.getKey() == DataComponents.TOOL
                                    || entry.getKey()
                                            == DataComponents.ATTRIBUTE_MODIFIERS),
                    material + " pickaxe persisted derived material facts");
            helper.assertTrue(
                    pickaxeItem.getDestroySpeed(
                            pickaxe, Blocks.STONE.defaultBlockState())
                            == ToolMaterialRules.miningSpeed(material),
                    material + " pickaxe speed is not derived at use time");
            helper.assertTrue(
                    pickaxeItem.isCorrectToolForDrops(
                                    pickaxe, Blocks.IRON_ORE.defaultBlockState())
                            == !Blocks.IRON_ORE.defaultBlockState().is(
                                    ToolMaterialRules.miningTier(material)
                                            .getIncorrectBlocksForDrops()),
                    material + " pickaxe drop tier is not derived at use time");
            AtomicInteger modifiers = new AtomicInteger();
            pickaxe.forEachModifier(
                    EquipmentSlotGroup.MAINHAND,
                    (attribute, modifier) -> modifiers.incrementAndGet());
            helper.assertTrue(
                    modifiers.get() == 2,
                    material + " pickaxe attack attributes are not derived through ItemStack");
        }
        ItemStack rawGivePickaxe = new ItemStack(pickaxeItem);
        helper.assertTrue(
                rawGivePickaxe.getMaxDamage()
                        == ToolMaterialRules.durability(
                                ToolMaterialRules.ToolKind.PICKAXE, "iron")
                        && pickaxeItem.getDestroySpeed(
                                rawGivePickaxe, Blocks.STONE.defaultBlockState())
                                == ToolMaterialRules.miningSpeed("iron")
                        && pickaxeItem.isCorrectToolForDrops(
                                rawGivePickaxe,
                                Blocks.IRON_ORE.defaultBlockState()),
                "A raw /give material_pickaxe is not a complete iron pickaxe");
        ItemStack quarantinedPickaxe = new ItemStack(pickaxeItem);
        quarantinedPickaxe.set(ModComponents.TOOL_MATERIAL, "future_material");
        helper.assertTrue(
                quarantinedPickaxe.getMaxDamage() > 0
                        && !pickaxeItem.canApplyDurabilityDamage(
                                quarantinedPickaxe)
                        && !pickaxeItem.isCorrectToolForDrops(
                                quarantinedPickaxe,
                                Blocks.IRON_ORE.defaultBlockState()),
                "A quarantined pickaxe is not inert and recoverable");
        GTRecipe quarantinedWear = new GTRecipe(
                List.of(
                        Ingredient.of(Items.IRON_INGOT),
                        Ingredient.of(pickaxeItem)),
                List.of(1, 0),
                List.of(ItemInputAction.CONSUME, ItemInputAction.wear(1)),
                List.of(new ItemStack(Items.IRON_NUGGET)),
                List.of(),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                20,
                16,
                0,
                true,
                java.util.Optional.empty());
        helper.assertTrue(
                MachineTransaction.prepare(
                                quarantinedWear,
                                List.of(
                                        new ItemStack(Items.IRON_INGOT),
                                        quarantinedPickaxe,
                                        ItemStack.EMPTY),
                                List.of(0, 1),
                                List.of(2),
                                List.of(),
                                List.of(),
                                List.of(),
                                List.of())
                        .isEmpty()
                        && quarantinedPickaxe.getCount() == 1
                        && quarantinedPickaxe.getDamageValue() == 0,
                "WEAR destroyed or mutated a quarantined material tool");
        ItemStack file = ModItems.MATERIAL_FILE.get().variant("iron");
        helper.assertTrue(
                file.isDamageableItem()
                        && file.getMaxDamage()
                                == ToolMaterialRules.durability(
                                        ToolMaterialRules.ToolKind.FILE, "iron")
                        && file.getComponentsPatch().entrySet().stream()
                                .noneMatch(entry -> entry.getKey()
                                        == DataComponents.MAX_DAMAGE)
                        && "iron".equals(file.get(ModComponents.TOOL_MATERIAL)),
                "Iron File is not a runtime-derived durability catalyst");
        ItemStack hammer = ModItems.SMITHING_HAMMER.get().variant("bronze");
        helper.assertTrue(
                hammer.getMaxDamage()
                                == ToolMaterialRules.durability(
                                        ToolMaterialRules.ToolKind.SMITHING_HAMMER,
                                        "bronze")
                        && Integer.valueOf(1).equals(
                                hammer.get(DataComponents.MAX_DAMAGE))
                        && hammer.getComponentsPatch().entrySet().size() == 1
                        && hammer.getComponentsPatch().entrySet().stream()
                                .allMatch(entry -> entry.getKey()
                                        == ModComponents.TOOL_MATERIAL.get()),
                "Bronze hammer stack patch is not material-identity-only");
        helper.assertTrue(
                MaterialComponentPolicies.resolve(
                        "cruciblecraft:material_pickaxe").isPresent()
                        && MaterialComponentPolicies.resolve(
                                "cruciblecraft:material_file").isPresent(),
                "T4 items are not discoverable through item-level material policy");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void assemblerPickaxeFileWearIsAtomic(GameTestHelper helper) {
        BlockPos pos = new BlockPos(5, 2, 5);
        ConfiguredProcessingMachineBlockEntity assembler =
                placeConfigured(
                        helper,
                        pos,
                        ModBlocks.ASSEMBLER.get(),
                        ModProcessingMachines.ASSEMBLER);
        RecipeMap.Entry recipe = ModRecipeMaps.ASSEMBLER.entries().stream()
                .filter(entry -> entry.id().getPath()
                        .equals("t4/assembler/pickaxe/metal/iron"))
                .findFirst()
                .orElseThrow();
        loadRecipeInputs(assembler, recipe.recipe());
        int fileSlot = assembler.spec().items().inputs().get(
                inputIndex(recipe.recipe(), ModItems.MATERIAL_FILE.get()));
        int hammerSlot = assembler.spec().items().inputs().get(
                inputIndex(recipe.recipe(), ModItems.SMITHING_HAMMER.get()));
        int patternSlot = assembler.spec().items().inputs().get(
                inputIndex(recipe.recipe(), ModItems.toolPattern("pickaxe").get()));
        int outputSlot = assembler.spec().items().outputs().getFirst();
        fillKuCapability(helper, pos);

        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    helper.assertTrue(
                            assembler.duration() > 0,
                            "T4 pickaxe recipe was not selected");
                    assembler.inventory().setStackInSlot(
                            outputSlot, new ItemStack(Items.BEDROCK, 64));
                    forceLastTick(helper, assembler);
                    fillKuCapability(helper, pos);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    ItemStack blockedFile =
                            assembler.inventory().getStackInSlot(fileSlot);
                    ItemStack blockedHammer =
                            assembler.inventory().getStackInSlot(hammerSlot);
                    ItemStack blockedPattern =
                            assembler.inventory().getStackInSlot(patternSlot);
                    helper.assertTrue(
                            blockedFile.getCount() == 1
                                    && blockedFile.getDamageValue() == 0
                                    && blockedHammer.getCount() == 1
                                    && blockedHammer.getDamageValue() == 0
                                    && blockedPattern.getCount() == 1,
                            "Blocked assembler mutated a catalyst or pattern");
                    assembler.inventory().setStackInSlot(
                            outputSlot, ItemStack.EMPTY);
                    forceLastTick(helper, assembler);
                    fillKuCapability(helper, pos);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    ItemStack wornFile =
                            assembler.inventory().getStackInSlot(fileSlot);
                    ItemStack wornHammer =
                            assembler.inventory().getStackInSlot(hammerSlot);
                    ItemStack preservedPattern =
                            assembler.inventory().getStackInSlot(patternSlot);
                    ItemStack output =
                            assembler.inventory().getStackInSlot(outputSlot);
                    helper.assertTrue(
                            wornFile.getCount() == 1
                                    && wornFile.getDamageValue() == 1
                                    && wornHammer.getCount() == 1
                                    && wornHammer.getDamageValue() == 1
                                    && preservedPattern.getCount() == 1,
                            "Completed assembler recipe lost atomic catalyst semantics");
                    helper.assertTrue(
                            output.is(ModItems.MATERIAL_PICKAXE.get())
                                    && "iron".equals(output.get(
                                            ModComponents.TOOL_MATERIAL)),
                            "Assembler did not output the iron material pickaxe");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 360)
    public static void everyT3PlacedMachineAdvancesRealRecipe(GameTestHelper helper) {
        List<Block> blocks = List.of(
                ModBlocks.EXTRUDER.get(), ModBlocks.CUTTER.get(), ModBlocks.LATHE.get(),
                ModBlocks.ROLLINGMILL.get(), ModBlocks.ROLLBENDER.get(),
                ModBlocks.WIREMILL.get(), ModBlocks.BENDER.get(), ModBlocks.ASSEMBLER.get(),
                ModBlocks.WELDER.get(), ModBlocks.PRESS.get());
        List<ConfiguredProcessingMachineBlockEntity> machines = new java.util.ArrayList<>();
        for (int i = 0; i < ModProcessingMachines.T3_MACHINES.size(); i++) {
            BlockPos pos = new BlockPos(2 + (i % 5) * 4, 2, 2 + (i / 5) * 6);
            ProcessingMachineSpec spec = ModProcessingMachines.T3_MACHINES.get(i);
            ConfiguredProcessingMachineBlockEntity machine =
                    placeConfigured(helper, pos, blocks.get(i), spec);
            GTRecipe recipe = spec.requireRecipeMap().recipes().getFirst();
            loadRecipeInputs(machine, recipe);
            fillKuCapability(helper, pos);
            machines.add(machine);
        }
        helper.startSequence()
                .thenIdle(4)
                .thenExecute(() -> {
                    for (ConfiguredProcessingMachineBlockEntity machine : machines) {
                        helper.assertTrue(machine.duration() > 0,
                                machine.spec().id() + " did not resolve its real map recipe");
                        helper.assertTrue(machine.progress() > 0,
                                machine.spec().id() + " did not advance");
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void everyLiveT3RecipeTracesToGeneratedComponentJson(
            GameTestHelper helper) {
        Map<RecipeMap, Integer> expected = Map.of(
                ModRecipeMaps.EXTRUDER, 3039,
                ModRecipeMaps.CUTTER, 651,
                ModRecipeMaps.LATHE, 929,
                ModRecipeMaps.ROLLINGMILL, 336,
                ModRecipeMaps.ROLLBENDER, 438,
                ModRecipeMaps.WIREMILL, 287,
                ModRecipeMaps.BENDER, 638,
                ModRecipeMaps.ASSEMBLER, 4021,
                ModRecipeMaps.WELDER, 321,
                ModRecipeMaps.PRESS, 1191);
        int total = 0;
        for (ProcessingMachineSpec spec : ModProcessingMachines.T3_MACHINES) {
            RecipeMap map = spec.requireRecipeMap();
            helper.assertTrue(
                    map.entries().size() == expected.get(map),
                    map.id() + " live count drifted from generated component manifest");
            helper.assertTrue(
                    map.unindexedRecipeCount() == 0,
                    map.id() + " contains unindexed live recipes");
            total += map.entries().size();
            for (RecipeMap.Entry entry : map.entries()) {
                String expandedPath = entry.id().getPath();
                int materialSeparator = expandedPath.lastIndexOf('/');
                helper.assertTrue(
                        materialSeparator > 0,
                        "T3 recipe lacks expanded material suffix: " + entry.id());
                String directPath = "data/" + entry.id().getNamespace() + "/recipe/"
                        + expandedPath + ".json";
                String sourcePath = "data/" + entry.id().getNamespace() + "/recipe/"
                        + expandedPath.substring(0, materialSeparator) + ".json";
                String[] pathParts = expandedPath.split("/");
                boolean compactExtruderSource = pathParts.length >= 3
                        && pathParts[0].equals("extruder")
                        && (CrucibleCraftGameTests.class.getClassLoader().getResource(
                                "data/" + entry.id().getNamespace()
                                        + "/recipe/extruder/compact/normal_"
                                        + pathParts[1] + ".json") != null
                                || CrucibleCraftGameTests.class.getClassLoader().getResource(
                                        "data/" + entry.id().getNamespace()
                                                + "/recipe/extruder/compact/low_heat_"
                                                + pathParts[1] + ".json") != null);
                helper.assertTrue(
                        CrucibleCraftGameTests.class.getClassLoader()
                                .getResource(directPath) != null
                                || CrucibleCraftGameTests.class.getClassLoader()
                                        .getResource(sourcePath) != null
                                || compactExtruderSource,
                        "Live T3 recipe has no component-rule JSON source: "
                                + entry.id() + " -> " + sourcePath);
            }
        }
        helper.assertTrue(
                total == 11851,
                "Live T3-map recipe total is not 11851: " + total);
        int t5Total = 0;
        for (RecipeMap map : ModProcessingMachines.T5_MACHINES.stream()
                .map(ProcessingMachineSpec::requireRecipeMap)
                .distinct()
                .toList()) {
            ProcessingMachineSpec host =
                    ModProcessingMachines.forRecipeMap(map.id()).orElseThrow();
            for (RecipeMap.Entry entry : map.entries()) {
                if (!entry.id().getPath().startsWith("t5/")) {
                    continue;
                }
                t5Total++;
                String resource = "data/" + entry.id().getNamespace()
                        + "/recipe/" + entry.id().getPath() + ".json";
                helper.assertTrue(
                        CrucibleCraftGameTests.class.getClassLoader()
                                .getResource(resource) != null,
                        "Live T5 recipe has no generated source JSON: " + entry.id());
                helper.assertTrue(
                        host.validator().validate(entry.recipe()).isEmpty(),
                        "Live T5 recipe is published but its runtime host rejects it: "
                                + entry.id() + " host=" + host.id()
                                + " reason="
                                + host.validator().validate(entry.recipe()).orElse(""));
                GTRecipeQuery query = queryFor(entry.recipe());
                RecipeMap.Match resolved =
                        map.findMatch(query).orElse(null);
                helper.assertTrue(
                        resolved != null && resolved.id().equals(entry.id()),
                        "Live T5 recipe cannot be found by its declared input: "
                                + entry.id()
                                + " resolved="
                                + (resolved == null ? null : resolved.id())
                                + " candidates="
                                + map.indexedCandidateCount(query)
                                + " directMatch="
                                + entry.recipe().matches(query));
            }
            helper.assertTrue(
                    map.unindexedRecipeCount() == 0,
                    map.id() + " contains unindexed T5 recipes");
        }
        helper.assertTrue(t5Total == 152, "Live T5 recipe total is not 152: " + t5Total);
        ProcessingEmiRegistrationPlan emiPlan = ProcessingEmiRegistrationPlan.create(
                ModProcessingMachines.CONFIGURED_MACHINES);
        int expectedEmiRecipes = ModProcessingMachines.CONFIGURED_MACHINES.stream()
                .mapToInt(spec -> spec.requireRecipeMap().entries().size())
                .sum();
        helper.assertTrue(
                emiPlan.machines().size() == 24
                        && emiPlan.recipes().size() == expectedEmiRecipes,
                "Generic processing EMI does not cover every configured machine recipe: "
                        + emiPlan.machines().size()
                        + " machines, "
                        + emiPlan.recipes().size()
                        + "/"
                        + expectedEmiRecipes
                        + " recipes");
        helper.assertTrue(
                com.masson.cruciblecraft.recipe.gt.GTRecipeMapLoader
                        .rejectedUnindexedRecipeCount() == 0,
                "Core recipe publication rejected an unindexable recipe");
        var metrics = com.masson.cruciblecraft.recipe.gt.GTRecipeMapLoader
                .lastPublicationMetrics();
        var lookup = com.masson.cruciblecraft.recipe.gt.GTRecipeMapLoader
                .benchmarkT14LookupsForVerification();
        var onlineGate = com.masson.cruciblecraft.recipe.gt.GTRecipeMapLoader
                .evaluateT14OnlineBudgetGate(metrics, lookup);
        helper.assertTrue(
                metrics.t3ComponentRecipes() == 8398
                        && metrics.t4ToolRecipes() == 3452
                        && metrics.t5ChemicalRecipes() == 154
                        && metrics.t7AuthoredMaterialRules() == 220
                        && metrics.t8PipeMaterialRules() == 257
                        && metrics.t10KnownFormMaterialRules() == 1_288
                        && metrics.liveT3MapRecipes() == 11851
                        && metrics.allPublishedRecipes() == 18_875
                        && metrics.eagerPublishedRecipes() == 16_650
                        && metrics.eagerPublishedRecipes()
                                <= ModProcessingMachines
                                        .ALL_EAGER_PUBLICATION_SOFT_BUDGET
                        && metrics.lazyLogicalRecipes() == 2_225
                        && metrics.lazyLogicalRecipes()
                                <= ModProcessingMachines
                                        .ALL_LAZY_LOGICAL_RECIPE_HARD_CEILING
                        && metrics.t14ExtruderLogicalRecipes() == 2_782
                        && metrics.t14ExtruderEagerRecipes() == 557
                        && metrics.t14ExtruderLazyRecipes() == 2_225
                        && metrics.t14ExtruderCacheCeiling() == 512
                        && metrics.t14ExtruderSyncBytes() == 331_124L
                        && metrics.t14ExtruderCacheCeiling()
                                <= ModProcessingMachines
                                        .ALL_LAZY_RECIPE_CACHE_HARD_CEILING
                        && metrics.t14ExtruderAuthoredEntries() == 20
                        && metrics.t14ExtruderStableFingerprint()
                                .matches("[0-9a-f]{64}")
                        && metrics.runtimeSide()
                                == ExtruderRecipeFamilyProvider.RuntimeSide.SERVER
                        && metrics.t5ChemicalRecipes()
                                <= ModProcessingMachines.T5_CHEMICAL_RECIPE_BUDGET
                        && metrics.t7AuthoredMaterialRules()
                                <= ModProcessingMachines
                                        .T7_AUTHORED_MATERIAL_RULE_BUDGET
                        && metrics.t8PipeMaterialRules()
                                <= ModProcessingMachines
                                        .T8_PIPE_MATERIAL_RULE_BUDGET
                        && metrics.t10KnownFormMaterialRules()
                                <= ModProcessingMachines
                                        .T10_AUTHORED_MATERIAL_RULE_BUDGET
                        && metrics.allPublishedRecipes()
                                <= ModProcessingMachines.ALL_PUBLISHED_RECIPE_BUDGET
                        && metrics.reloadMillis()
                                <= ModProcessingMachines.RECIPE_RELOAD_BUDGET_MS
                        && metrics.indexMillis()
                                <= ModProcessingMachines.RECIPE_INDEX_BUILD_BUDGET_MS
                        && lookup.timingSamples() == 61
                        && lookup.operations() == 1_952
                        && onlineGate.allPass(),
                "Recipe publication performance budget drifted: "
                        + metrics + ", " + lookup + ", " + onlineGate);
        RecipeMap.RecipeFamily extruderFamily = ModRecipeMaps.EXTRUDER
                .family(ExtruderRecipeFamilyProvider.FAMILY_ID)
                .orElseThrow();
        helper.assertTrue(
                extruderFamily.epoch() == ModRecipeMaps.EXTRUDER.runtimeEpoch()
                        && extruderFamily.logicalRecipeCount() == 2_782
                        && extruderFamily.eagerRecipeCount() == 557
                        && extruderFamily.lazyRecipeCount() == 2_225
                        && extruderFamily.cacheCeiling() == 512,
                "T14c Extruder family snapshot shape drifted");
        for (RecipeMap.Entry entry : ModRecipeMaps.EXTRUDER.entries()) {
            GTRecipe recipe = entry.recipe();
            helper.assertTrue(
                    recipe.itemInputs().size() == 2
                            && recipe.itemInputCounts().equals(List.of(
                                    recipe.itemInputCounts().getFirst(), 0))
                            && recipe.itemInputCounts().getFirst() > 0,
                    "Extruder recipe is not material + exact presence-only shape: "
                            + entry.id());
            helper.assertTrue(
                    java.util.Arrays.stream(recipe.itemInputs().get(1).getItems())
                            .allMatch(ExtruderShapeCatalog::isShape),
                    "Extruder recipe retains a no-shape bypass: " + entry.id());
            GTRecipeQuery query = queryFor(recipe);
            RecipeMap.Match resolved =
                    ModRecipeMaps.EXTRUDER.findMatch(query).orElse(null);
            helper.assertTrue(
                    resolved != null && resolved.id().equals(entry.id()),
                    "Extruder expected input did not find its stable publication: "
                            + entry.id() + " resolved="
                            + (resolved == null ? null : resolved.id()));
        }
        helper.assertTrue(
                extruderFamily.cacheSize() <= extruderFamily.cacheCeiling(),
                "T14c Extruder long-tail cache exceeded its epoch ceiling");
        Map<String, Integer> toolCounts = Map.ofEntries(
                Map.entry("pickaxe", 330),
                Map.entry("shovel", 412),
                Map.entry("axe", 329),
                Map.entry("hoe", 329),
                Map.entry("sword", 412),
                Map.entry("smithing_hammer", 317),
                Map.entry("file", 92),
                Map.entry("chisel", 307),
                Map.entry("saw", 307),
                Map.entry("screwdriver", 309),
                Map.entry("wrench", 308));
        Map<String, Item> toolItems = Map.ofEntries(
                Map.entry("pickaxe", ModItems.MATERIAL_PICKAXE.get()),
                Map.entry("shovel", ModItems.MATERIAL_SHOVEL.get()),
                Map.entry("axe", ModItems.MATERIAL_AXE.get()),
                Map.entry("hoe", ModItems.MATERIAL_HOE.get()),
                Map.entry("sword", ModItems.MATERIAL_SWORD.get()),
                Map.entry("smithing_hammer", ModItems.SMITHING_HAMMER.get()),
                Map.entry("file", ModItems.MATERIAL_FILE.get()),
                Map.entry("chisel", ModItems.MATERIAL_CHISEL.get()),
                Map.entry("saw", ModItems.MATERIAL_SAW.get()),
                Map.entry("screwdriver", ModItems.MATERIAL_SCREWDRIVER.get()),
                Map.entry("wrench", ModItems.MATERIAL_WRENCH.get()));
        Map<String, ToolMaterialRules.ToolKind> toolKinds = Map.ofEntries(
                Map.entry("pickaxe", ToolMaterialRules.ToolKind.PICKAXE),
                Map.entry("shovel", ToolMaterialRules.ToolKind.SHOVEL),
                Map.entry("axe", ToolMaterialRules.ToolKind.AXE),
                Map.entry("hoe", ToolMaterialRules.ToolKind.HOE),
                Map.entry("sword", ToolMaterialRules.ToolKind.SWORD),
                Map.entry(
                        "smithing_hammer",
                        ToolMaterialRules.ToolKind.SMITHING_HAMMER),
                Map.entry("file", ToolMaterialRules.ToolKind.FILE),
                Map.entry("chisel", ToolMaterialRules.ToolKind.CHISEL),
                Map.entry("saw", ToolMaterialRules.ToolKind.SAW),
                Map.entry(
                        "screwdriver",
                        ToolMaterialRules.ToolKind.SCREWDRIVER),
                Map.entry("wrench", ToolMaterialRules.ToolKind.WRENCH));
        Map<String, Integer> eligibleWithoutRoute = Map.ofEntries(
                Map.entry("pickaxe", 208),
                Map.entry("shovel", 126),
                Map.entry("axe", 209),
                Map.entry("hoe", 209),
                Map.entry("sword", 126),
                Map.entry("smithing_hammer", 80),
                Map.entry("file", 56),
                Map.entry("chisel", 2),
                Map.entry("saw", 2),
                Map.entry("screwdriver", 0),
                Map.entry("wrench", 2));
        for (var toolEntry : toolCounts.entrySet()) {
            String tool = toolEntry.getKey();
            List<RecipeMap.Entry> recipes = ModRecipeMaps.ASSEMBLER.entries().stream()
                    .filter(entry -> entry.id().getPath()
                            .startsWith("t4/assembler/" + tool + "/"))
                    .toList();
            helper.assertTrue(
                    recipes.size() == toolEntry.getValue(),
                    "T4 " + tool + " count drifted: " + recipes.size());
            long itemEligible = MaterialCatalog.values().stream()
                    .filter(material -> ToolMaterialRules.isAllowed(
                            toolKinds.get(tool), material.id()))
                    .count();
            helper.assertTrue(
                    itemEligible - recipes.size()
                            == eligibleWithoutRoute.get(tool),
                    "T4 " + tool + " item-eligible/route-ready gap drifted: "
                            + itemEligible + " eligible vs " + recipes.size()
                            + " recipes");
            Item expectedItem = toolItems.get(tool);
            String expectedItemId = tool.equals("smithing_hammer")
                    ? "cruciblecraft:smithing_hammer"
                    : "cruciblecraft:material_" + tool;
            for (RecipeMap.Entry entry : recipes) {
                GTRecipe recipe = entry.recipe();
                String path = entry.id().getPath();
                String materialId = path.substring(path.lastIndexOf('/') + 1);
                helper.assertTrue(
                        recipe.itemInputs().size() <= 6
                                && recipe.itemInputActions()
                                        .contains(ItemInputAction.PRESERVE)
                                && recipe.itemInputActions().stream()
                                        .anyMatch(action -> action.kind()
                                                == ItemInputAction.Kind.WEAR)
                                && recipe.itemOutputs().size() == 1
                                && recipe.itemOutputs().getFirst().is(expectedItem)
                                && materialId.equals(recipe.itemOutputs().getFirst()
                                        .get(ModComponents.TOOL_MATERIAL))
                                && MaterialComponentPolicies.resolve(expectedItemId)
                                        .orElseThrow()
                                        .isPersistedMaterialAllowed(materialId),
                        "T4 tool batch lost bounded inputs, catalyst actions, "
                                + "or component output: " + entry.id());
            }
        }
        List<RecipeMap.Entry> pickaxes = ModRecipeMaps.ASSEMBLER.entries().stream()
                .filter(entry -> entry.id().getPath()
                        .startsWith("t4/assembler/pickaxe/"))
                .toList();
        Map.of(
                "metal", 203L,
                "gem", 126L,
                "stone_rod_exception", 1L)
                .forEach((route, expectedCount) -> {
                    long actualCount = pickaxes.stream()
                            .filter(entry -> entry.id().getPath().startsWith(
                                    "t4/assembler/pickaxe/" + route + "/"))
                            .count();
                    helper.assertTrue(
                            actualCount == expectedCount,
                            "T4 Pickaxe route count drifted for " + route
                                    + ": " + actualCount);
                });
        helper.assertTrue(
                pickaxes.stream().anyMatch(entry -> entry.id().getPath().equals(
                        "t4/assembler/pickaxe/stone_rod_exception/stone")),
                "Exact GT6 Stone identity lost its declared route exception");
        helper.assertTrue(
                ToolMaterialRules.isAllowed(
                        ToolMaterialRules.ToolKind.PICKAXE, "oak")
                        && pickaxes.stream().noneMatch(entry -> entry.id().getPath()
                                .endsWith("/oak")),
                "Oak must remain source-eligible but route-unavailable until "
                        + "its registered-form closure is deliberately restored");
        for (String blocked : List.of(
                "anti_adamantium", "anti_vibranium", "bone", "rubber", "wood")) {
            helper.assertTrue(
                    pickaxes.stream().noneMatch(entry -> entry.id().getPath()
                            .endsWith("/" + blocked)),
                    blocked + " unexpectedly has a Pickaxe route");
        }
        helper.succeed();
    }

    @GameTest(
            template = TEMPLATE,
            batch = "t15c_machine_variant_crafting",
            timeoutTicks = 80)
    public static void machineVariantRecipesExecuteFromLiveRecipeManager(
            GameTestHelper helper) {
        List<MachineCraftingCase> cases = machineCraftingCases();
        helper.assertTrue(cases.size() == 9, "T15c must execute all nine variants");
        executeMachineCraftingCases(helper, cases);
    }

    @GameTest(
            template = TEMPLATE,
            batch = "t16c_machine_variant_crafting",
            timeoutTicks = 80)
    public static void t16SelectedMachineRecipesExecuteFromLiveRecipeManager(
            GameTestHelper helper) {
        List<MachineCraftingCase> cases = t16MachineCraftingCases();
        helper.assertTrue(
                cases.size() == 15,
                "T16c must execute the selected five-by-three variants");
        executeMachineCraftingCases(helper, cases);
    }

    @GameTest(
            template = TEMPLATE,
            batch = "t17c_hu_machine_variant_crafting",
            timeoutTicks = 80)
    public static void t17SelectedHuRecipesExecuteFromLiveRecipeManager(
            GameTestHelper helper) {
        List<MachineCraftingCase> cases = t17MachineCraftingCases();
        helper.assertTrue(
                cases.size() == 9,
                "T17c must execute all selected three-by-three HU variants");
        executeMachineCraftingCases(helper, cases);
    }

    @GameTest(
            template = TEMPLATE,
            batch = "t16d_zero_publication",
            timeoutTicks = 40)
    public static void t16dRecipePublicationBaselineRemainsStable(
            GameTestHelper helper) {
        JsonObject baseline = t16PublicationBaseline();
        Set<String> expectedMapIds = jsonStringSet(
                baseline, "recipe_map_ids");
        Set<String> actualMapIds = ModRecipeMaps.ALL.stream()
                .map(map -> map.id().toString())
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        helper.assertTrue(
                expectedMapIds.equals(actualMapIds),
                "T16d RecipeMap stable id set drifted: expected="
                        + expectedMapIds + " actual=" + actualMapIds);

        JsonObject totals = baseline.getAsJsonObject("publication_totals");
        var metrics = com.masson.cruciblecraft.recipe.gt.GTRecipeMapLoader
                .lastPublicationMetrics();
        helper.assertTrue(
                metrics.allPublishedRecipes()
                                == totals.get("logical_rows").getAsInt()
                        && metrics.eagerPublishedRecipes()
                                == totals.get("eager_rows").getAsInt()
                        && metrics.lazyLogicalRecipes()
                                == totals.get("lazy_rows").getAsInt(),
                "T16d logical/eager/lazy baseline drifted: " + metrics);

        Set<String> expectedEmiIds = jsonStringSet(
                baseline, "emi_recipe_map_ids");
        Set<String> actualEmiIds = ModProcessingMachines.CONFIGURED_MACHINES
                .stream()
                .map(spec -> spec.requireRecipeMap().id().toString())
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        helper.assertTrue(
                expectedEmiIds.equals(actualEmiIds),
                "T16d EMI RecipeMap set drifted: expected=" + expectedEmiIds
                        + " actual=" + actualEmiIds);

        ProcessingEmiRegistrationPlan emi =
                ProcessingEmiRegistrationPlan.create(
                        ModProcessingMachines.CONFIGURED_MACHINES);
        List<String> expectedEnumeration =
                ModProcessingMachines.CONFIGURED_MACHINES.stream()
                        .flatMap(spec -> spec.requireRecipeMap().entries()
                                .stream()
                                .map(entry -> spec.id() + "|" + entry.id()))
                        .sorted()
                        .toList();
        List<String> actualEnumeration = emi.recipes().stream()
                .map(recipe -> recipe.machine().categoryId()
                        + "|" + recipe.id())
                .sorted()
                .toList();
        helper.assertTrue(
                expectedEnumeration.equals(actualEnumeration)
                        && actualEnumeration.size()
                                == new java.util.HashSet<>(
                                        actualEnumeration).size(),
                "T16d EMI recipe enumeration is not exact");

        JsonObject acquisition = baseline.getAsJsonObject("t16_acquisition");
        Set<String> expectedCraftingIds = jsonStringSet(
                acquisition, "vanilla_recipe_ids");
        Set<Item> selectedResults = t16MachineCraftingCases().stream()
                .map(MachineCraftingCase::result)
                .collect(java.util.stream.Collectors.toSet());
        Set<String> actualCraftingIds = helper.getLevel().getRecipeManager()
                .getAllRecipesFor(RecipeType.CRAFTING)
                .stream()
                .filter(holder -> selectedResults.contains(
                        holder.value().getResultItem(
                                helper.getLevel().registryAccess()).getItem()))
                .map(holder -> holder.id().toString())
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        long t16GtRows = ModRecipeMaps.ALL.stream()
                .flatMap(map -> map.entries().stream())
                .filter(entry -> entry.id().getNamespace().equals(
                        CrucibleCraft.MODID))
                .filter(entry -> entry.id().getPath().startsWith("t16/"))
                .count();
        helper.assertTrue(
                acquisition.get("recipe_type").getAsString()
                                .equals("minecraft:crafting_shaped")
                        && expectedCraftingIds.equals(actualCraftingIds)
                        && actualCraftingIds.size()
                                == acquisition.get(
                                        "vanilla_crafting_rows").getAsInt()
                        && t16GtRows
                                == acquisition.get("gt_recipe_rows").getAsLong(),
                "T16d acquisition is not exactly 15 vanilla crafting rows "
                        + "and zero GT rows: crafting=" + actualCraftingIds
                        + " gt=" + t16GtRows);
        helper.succeed();
    }

    private static JsonObject t16PublicationBaseline() {
        var stream = CrucibleCraftGameTests.class.getClassLoader()
                .getResourceAsStream(
                        "data/cruciblecraft/t16_publication_baseline.json");
        if (stream == null) {
            throw new IllegalStateException(
                    "Missing T16d publication baseline resource");
        }
        try (var reader = new InputStreamReader(
                stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Cannot read T16d publication baseline", exception);
        }
    }

    @GameTest(
            template = TEMPLATE,
            batch = "t17d_zero_publication",
            timeoutTicks = 40)
    public static void t17dRecipePublicationMatchesT16Baseline(
            GameTestHelper helper) {
        JsonObject baseline = t17PublicationBaseline();
        JsonObject t16 = t16PublicationBaseline();
        Set<String> expectedMapIds = jsonStringSet(
                baseline, "recipe_map_ids");
        Set<String> t16MapIds = jsonStringSet(t16, "recipe_map_ids");
        Set<String> actualMapIds = ModRecipeMaps.ALL.stream()
                .map(map -> map.id().toString())
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        helper.assertTrue(
                expectedMapIds.equals(t16MapIds)
                        && expectedMapIds.equals(actualMapIds),
                "T17d RecipeMap stable id set differs from T16: expected="
                        + expectedMapIds + " actual=" + actualMapIds);

        JsonObject totals = baseline.getAsJsonObject("publication_totals");
        helper.assertTrue(
                totals.equals(t16.getAsJsonObject("publication_totals")),
                "T17d publication totals differ from the T16 baseline");
        var metrics = com.masson.cruciblecraft.recipe.gt.GTRecipeMapLoader
                .lastPublicationMetrics();
        helper.assertTrue(
                metrics.allPublishedRecipes()
                                == totals.get("logical_rows").getAsInt()
                        && metrics.eagerPublishedRecipes()
                                == totals.get("eager_rows").getAsInt()
                        && metrics.lazyLogicalRecipes()
                                == totals.get("lazy_rows").getAsInt(),
                "T17d logical/eager/lazy baseline drifted: " + metrics);

        Set<String> expectedEmiIds = jsonStringSet(
                baseline, "emi_recipe_map_ids");
        Set<String> t16EmiIds = jsonStringSet(t16, "emi_recipe_map_ids");
        Set<String> actualEmiIds = ModProcessingMachines.CONFIGURED_MACHINES
                .stream()
                .map(spec -> spec.requireRecipeMap().id().toString())
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        helper.assertTrue(
                expectedEmiIds.equals(t16EmiIds)
                        && expectedEmiIds.equals(actualEmiIds),
                "T17d EMI RecipeMap set differs from T16: expected="
                        + expectedEmiIds + " actual=" + actualEmiIds);

        ProcessingEmiRegistrationPlan emi =
                ProcessingEmiRegistrationPlan.create(
                        ModProcessingMachines.CONFIGURED_MACHINES);
        List<String> expectedEnumeration =
                ModProcessingMachines.CONFIGURED_MACHINES.stream()
                        .flatMap(spec -> spec.requireRecipeMap().entries()
                                .stream()
                                .map(entry -> spec.id() + "|" + entry.id()))
                        .sorted()
                        .toList();
        List<String> actualEnumeration = emi.recipes().stream()
                .map(recipe -> recipe.machine().categoryId()
                        + "|" + recipe.id())
                .sorted()
                .toList();
        helper.assertTrue(
                expectedEnumeration.equals(actualEnumeration)
                        && actualEnumeration.size()
                                == new java.util.HashSet<>(
                                        actualEnumeration).size(),
                "T17d EMI recipe enumeration is not exact");

        JsonObject acquisition = baseline.getAsJsonObject("t17_acquisition");
        Set<String> expectedCraftingIds = jsonStringSet(
                acquisition, "vanilla_recipe_ids");
        Set<Item> selectedResults = t17MachineCraftingCases().stream()
                .map(MachineCraftingCase::result)
                .collect(java.util.stream.Collectors.toSet());
        Set<String> actualCraftingIds = helper.getLevel().getRecipeManager()
                .getAllRecipesFor(RecipeType.CRAFTING)
                .stream()
                .filter(holder -> selectedResults.contains(
                        holder.value().getResultItem(
                                helper.getLevel().registryAccess()).getItem()))
                .map(holder -> holder.id().toString())
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        long t17GtRows = ModRecipeMaps.ALL.stream()
                .flatMap(map -> map.entries().stream())
                .filter(entry -> entry.id().getNamespace().equals(
                        CrucibleCraft.MODID))
                .filter(entry -> entry.id().getPath().startsWith("t17/"))
                .count();
        helper.assertTrue(
                acquisition.get("recipe_type").getAsString()
                                .equals("minecraft:crafting_shaped")
                        && expectedCraftingIds.equals(actualCraftingIds)
                        && actualCraftingIds.size()
                                == acquisition.get(
                                        "vanilla_crafting_rows").getAsInt()
                        && t17GtRows
                                == acquisition.get("gt_recipe_rows").getAsLong(),
                "T17d acquisition is not exactly 9 vanilla crafting rows "
                        + "and zero GT rows: crafting=" + actualCraftingIds
                        + " gt=" + t17GtRows);
        helper.succeed();
    }

    private static JsonObject t17PublicationBaseline() {
        var stream = CrucibleCraftGameTests.class.getClassLoader()
                .getResourceAsStream(
                        "data/cruciblecraft/t17_publication_baseline.json");
        if (stream == null) {
            throw new IllegalStateException(
                    "Missing T17d publication baseline resource");
        }
        try (var reader = new InputStreamReader(
                stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Cannot read T17d publication baseline", exception);
        }
    }

    private static Set<String> jsonStringSet(
            JsonObject owner, String member) {
        Set<String> values = new TreeSet<>();
        owner.getAsJsonArray(member).forEach(
                element -> values.add(element.getAsString()));
        if (values.size() != owner.getAsJsonArray(member).size()) {
            throw new IllegalStateException(
                    "Publication baseline contains duplicate " + member);
        }
        return values;
    }

    private static void executeMachineCraftingCases(
            GameTestHelper helper,
            List<MachineCraftingCase> cases) {
        ServerLevel level = helper.getLevel();
        for (MachineCraftingCase expected : cases) {
            List<ItemStack> slots = new ArrayList<>();
            for (String row : expected.pattern()) {
                for (int column = 0; column < row.length(); column++) {
                    char symbol = row.charAt(column);
                    Item ingredient = expected.key().get(symbol);
                    slots.add(
                            symbol == ' '
                                    ? ItemStack.EMPTY
                                    : new ItemStack(ingredient));
                }
            }
            CraftingInput input = CraftingInput.of(
                    expected.pattern().getFirst().length(),
                    expected.pattern().size(),
                    slots);
            var match = level.getRecipeManager()
                    .getRecipeFor(RecipeType.CRAFTING, input, level)
                    .orElse(null);
            helper.assertTrue(
                    match != null,
                    "Live RecipeManager did not match " + expected.id());
            helper.assertTrue(
                    match.id().equals(expected.id()),
                    "Live RecipeManager matched " + match.id()
                            + " instead of " + expected.id());
            ItemStack assembled = match.value().assemble(
                    input, level.registryAccess());
            helper.assertTrue(
                    assembled.is(expected.result())
                            && assembled.getCount() == 1,
                    expected.id() + " assembled " + assembled
                            + " instead of " + expected.result());
        }
        helper.succeed();
    }

    @GameTest(
            template = TEMPLATE,
            batch = "t15d_identity_quarantine",
            timeoutTicks = 80)
    public static void processingIdentityMismatchesQuarantineAcrossNbtReload(
            GameTestHelper helper) {
        GTRecipe recipe = requireRecipe(
                ModRecipeMaps.ELECTROLYZER, "t5/electrolyzer/salt");
        List<IdentityReloadCase> cases = new ArrayList<>();
        for (int index = 0; index < 4; index++) {
            BlockPos pos = new BlockPos(2 + index * 3, 2, 5);
            ConfiguredProcessingMachineBlockEntity machine =
                    placeConfigured(
                            helper,
                            pos,
                            ModBlocks.STAINLESS_STEEL_ELECTROLYZER.get(),
                            ModProcessingMachines.ELECTROLYZER);
            MachineIdentityPolicy.Identity current =
                    ModMachineIdentityMigrations.identityOf(
                            machine.variant());
            List<MachineIdentityPolicy.Identity> mismatches = List.of(
                    new MachineIdentityPolicy.Identity(
                            current.machineKind() + "_wrong",
                            current.tierProfile(),
                            current.materialId(),
                            current.energyIdentity()),
                    new MachineIdentityPolicy.Identity(
                            current.machineKind(),
                            current.tierProfile() + "_wrong",
                            current.materialId(),
                            current.energyIdentity()),
                    new MachineIdentityPolicy.Identity(
                            current.machineKind(),
                            current.tierProfile(),
                            current.materialId() + "_wrong",
                            current.energyIdentity()),
                    new MachineIdentityPolicy.Identity(
                            current.machineKind(),
                            current.tierProfile(),
                            current.materialId(),
                            "KINETIC_ROTATION"));
            MachineIdentityPolicy.Identity mismatch = mismatches.get(index);

            loadRecipeInputs(machine, recipe);
            int inputSlot = machine.spec().items().inputs().getFirst();
            ItemStack savedInput =
                    machine.inventory().getStackInSlot(inputSlot).copy();
            int tank = machine.spec().fluids().outputs().getFirst().index();
            FluidStack savedFluid = recipe.fluidOutputs().getFirst().copy();
            savedFluid.setAmount(1);
            machine.tanks().get(tank).setFluid(savedFluid.copy());
            CompoundTag persisted = machine.saveWithoutMetadata(
                    helper.getLevel().registryAccess());
            persisted.putString(
                    "active_recipe",
                    "cruciblecraft:t15d_identity_reload");
            persisted.putInt("progress", 9);
            persisted.putInt("duration", recipe.duration());
            persisted.putString("status", "processing");
            persisted.putLong(
                    "energy", machine.spec().energy().capacity());
            putMachineIdentity(persisted, mismatch);
            machine.loadWithComponents(
                    persisted, helper.getLevel().registryAccess());

            helper.assertTrue(
                    machine.pausedReason().equals(
                            "material_quarantined"),
                    "Identity mismatch did not expose paused quarantine");
            helper.assertTrue(
                    machine.getUpdateTag(
                                    helper.getLevel().registryAccess())
                            .getString("material_quarantine")
                            .contains("does not match"),
                    "Identity mismatch did not expose its diagnostic");
            cases.add(new IdentityReloadCase(
                    List.of("kind", "tier", "material", "energy")
                            .get(index),
                    machine,
                    mismatch,
                    inputSlot,
                    savedInput,
                    tank,
                    savedFluid));
        }

        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    for (IdentityReloadCase expected : cases) {
                        ConfiguredProcessingMachineBlockEntity machine =
                                expected.machine();
                        String label = expected.label();
                        helper.assertTrue(
                                machine.progress() == 9,
                                label + " identity quarantine advanced progress");
                        helper.assertTrue(
                                machine.stored(EnergyType.ELECTRIC)
                                        == machine.spec().energy().capacity(),
                                label + " identity reload lost buffered energy");
                        helper.assertTrue(
                                ItemStack.isSameItemSameComponents(
                                        machine.inventory().getStackInSlot(
                                                expected.inputSlot()),
                                        expected.savedInput())
                                        && machine.inventory().getStackInSlot(
                                                expected.inputSlot()).getCount()
                                        == expected.savedInput().getCount(),
                                label + " identity reload lost inventory");
                        helper.assertTrue(
                                sameFluidAmount(
                                        machine.tanks().get(
                                                expected.tank()).getFluid(),
                                        expected.savedFluid()),
                                label + " identity reload lost tank contents");
                        CompoundTag resaved = machine.saveWithoutMetadata(
                                helper.getLevel().registryAccess());
                        helper.assertTrue(
                                hasMachineIdentity(
                                        resaved,
                                        expected.persistedIdentity()),
                                label + " quarantine rewrote the bad identity");
                        ItemStack extracted =
                                machine.inventory().extractItem(
                                        expected.inputSlot(), 1, false);
                        helper.assertTrue(
                                !extracted.isEmpty()
                                        && ItemStack.isSameItemSameComponents(
                                                extracted,
                                                expected.savedInput()),
                                label + " quarantine blocked inventory recovery");
                    }
                })
                .thenSucceed();
    }

    @GameTest(
            template = TEMPLATE,
            batch = "t15d_exact_identity_migration",
            timeoutTicks = 40)
    public static void exactLargeCentrifugeIdentityMigratesAcrossNbtReload(
            GameTestHelper helper) {
        BlockPos pos = new BlockPos(5, 2, 5);
        helper.setBlock(pos, ModBlocks.LARGE_CENTRIFUGE.get());
        LargeCentrifugeBlockEntity centrifuge =
                helper.getBlockEntity(pos);
        int inputSlot =
                centrifuge.spec().items().inputs().getFirst();
        centrifuge.inventory().setStackInSlot(
                inputSlot, new ItemStack(Items.IRON_INGOT, 3));
        CompoundTag persisted = centrifuge.saveWithoutMetadata(
                helper.getLevel().registryAccess());
        putMachineIdentity(
                persisted,
                ModMachineIdentityMigrations
                        .LEGACY_LARGE_CENTRIFUGE_IDENTITY);

        centrifuge.loadWithComponents(
                persisted, helper.getLevel().registryAccess());

        helper.assertTrue(
                !centrifuge.pausedReason().equals(
                        "material_quarantined"),
                "Exact legacy Large Centrifuge tuple was quarantined");
        helper.assertTrue(
                !centrifuge.getUpdateTag(
                                helper.getLevel().registryAccess())
                        .contains("material_quarantine"),
                "Exact migration retained an identity diagnostic");
        helper.assertTrue(
                centrifuge.inventory().getStackInSlot(inputSlot).getCount()
                        == 3,
                "Exact identity migration lost inventory");
        helper.assertTrue(
                hasMachineIdentity(
                        centrifuge.saveWithoutMetadata(
                                helper.getLevel().registryAccess()),
                        ModMachineIdentityMigrations
                                .LARGE_CENTRIFUGE_IDENTITY),
                "Next save did not write the current Large Centrifuge identity");
        helper.succeed();
    }

    @GameTest(
            template = TEMPLATE,
            batch = "t16c_exact_identity_migration",
            timeoutTicks = 40)
    public static void exactLegacyLatheIdentityMigratesAcrossNbtReload(
            GameTestHelper helper) {
        BlockPos pos = new BlockPos(5, 2, 5);
        ConfiguredProcessingMachineBlockEntity lathe = placeConfigured(
                helper,
                pos,
                ModBlocks.LATHE.get(),
                ModProcessingMachines.LATHE);
        int inputSlot = lathe.spec().items().inputs().getFirst();
        lathe.inventory().setStackInSlot(
                inputSlot,
                material("iron", MaterialPrefixes.INGOT, 3));
        CompoundTag persisted = lathe.saveWithoutMetadata(
                helper.getLevel().registryAccess());
        persisted.putString(
                "active_recipe",
                "cruciblecraft:t16c_identity_reload");
        persisted.putInt("progress", 9);
        persisted.putInt("duration", 40);
        persisted.putString("status", "processing");
        putMachineIdentity(
                persisted,
                ModMachineIdentityMigrations.legacyIdentityOf(
                        lathe.variant()));

        lathe.loadWithComponents(
                persisted, helper.getLevel().registryAccess());

        helper.assertTrue(
                !lathe.pausedReason().equals("material_quarantined"),
                "Exact legacy Lathe tuple was quarantined");
        helper.assertTrue(
                lathe.progress() == 9,
                "Exact Lathe identity migration lost progress");
        helper.assertTrue(
                lathe.inventory().getStackInSlot(inputSlot).getCount() == 3,
                "Exact Lathe identity migration lost inventory");
        helper.assertTrue(
                hasMachineIdentity(
                        lathe.saveWithoutMetadata(
                                helper.getLevel().registryAccess()),
                        ModMachineIdentityMigrations.identityOf(
                                lathe.variant())),
                "Next save did not write the bronze Lathe identity");
        helper.succeed();
    }

    private static void putMachineIdentity(
            CompoundTag tag,
            MachineIdentityPolicy.Identity identity) {
        tag.putString("machine_kind", identity.machineKind());
        tag.putString("tier_profile", identity.tierProfile());
        tag.putString("tier_material", identity.materialId());
        tag.putString("energy_identity", identity.energyIdentity());
    }

    private static boolean hasMachineIdentity(
            CompoundTag tag,
            MachineIdentityPolicy.Identity identity) {
        return tag.getString("machine_kind").equals(
                        identity.machineKind())
                && tag.getString("tier_profile").equals(
                        identity.tierProfile())
                && tag.getString("tier_material").equals(
                        identity.materialId())
                && tag.getString("energy_identity").equals(
                        identity.energyIdentity());
    }

    private record IdentityReloadCase(
            String label,
            ConfiguredProcessingMachineBlockEntity machine,
            MachineIdentityPolicy.Identity persistedIdentity,
            int inputSlot,
            ItemStack savedInput,
            int tank,
            FluidStack savedFluid) {}

    private static List<MachineCraftingCase> machineCraftingCases() {
        return List.of(
                centrifugeCraftingCase(
                        "centrifuge",
                        "bronze",
                        ModItems.BRONZE_DOUBLE_MACHINE_CASING.get(),
                        ModItems.CENTRIFUGE.get()),
                centrifugeCraftingCase(
                        "steel_centrifuge",
                        "steel",
                        ModItems.STEEL_DOUBLE_MACHINE_CASING.get(),
                        ModItems.STEEL_CENTRIFUGE.get()),
                centrifugeCraftingCase(
                        "titanium_centrifuge",
                        "titanium",
                        ModItems.TITANIUM_DOUBLE_MACHINE_CASING.get(),
                        ModItems.TITANIUM_CENTRIFUGE.get()),
                sifterCraftingCase(
                        "sifter",
                        "bronze",
                        ModItems.BRONZE_DOUBLE_MACHINE_CASING.get(),
                        ModItems.SIFTER.get()),
                sifterCraftingCase(
                        "steel_sifter",
                        "steel",
                        ModItems.STEEL_DOUBLE_MACHINE_CASING.get(),
                        ModItems.STEEL_SIFTER.get()),
                sifterCraftingCase(
                        "titanium_sifter",
                        "titanium",
                        ModItems.TITANIUM_DOUBLE_MACHINE_CASING.get(),
                        ModItems.TITANIUM_SIFTER.get()),
                electrolyzerCraftingCase(
                        "electrolyzer",
                        ModItems.STEEL_GALVANIZED_MACHINE_CASING.get(),
                        "tin",
                        ModItems.ELECTROLYZER.get()),
                electrolyzerCraftingCase(
                        "aluminium_electrolyzer",
                        ModItems.ALUMINIUM_MACHINE_CASING.get(),
                        "copper",
                        ModItems.ALUMINIUM_ELECTROLYZER.get()),
                electrolyzerCraftingCase(
                        "stainless_steel_electrolyzer",
                        ModItems.STAINLESS_STEEL_MACHINE_CASING.get(),
                        "gold",
                        ModItems.STAINLESS_STEEL_ELECTROLYZER.get()));
    }

    private static MachineCraftingCase centrifugeCraftingCase(
            String id,
            String material,
            Item casing,
            Item result) {
        return new MachineCraftingCase(
                ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID, "machines/" + id),
                result,
                List.of("G ", "SC", "G "),
                Map.of(
                        'C', casing,
                        'G', material(material, MaterialPrefixes.GEAR, 1).getItem(),
                        'S', material(
                                material,
                                MaterialPrefixes.LONG_ROD,
                                1).getItem()));
    }

    private static MachineCraftingCase sifterCraftingCase(
            String id,
            String material,
            Item casing,
            Item result) {
        return new MachineCraftingCase(
                ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID, "machines/" + id),
                result,
                List.of("W W", "RCR", "S S"),
                Map.of(
                        'C', casing,
                        'R', material(material, MaterialPrefixes.ROD, 1).getItem(),
                        'S', material(
                                material,
                                MaterialPrefixes.SPRING,
                                1).getItem(),
                        'W', material(
                                material,
                                MaterialPrefixes.FINE_WIRE,
                                1).getItem()));
    }

    private static MachineCraftingCase electrolyzerCraftingCase(
            String id,
            Item casing,
            String cableMaterial,
            Item result) {
        return new MachineCraftingCase(
                ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID, "machines/" + id),
                result,
                List.of("SMS", "W W"),
                Map.of(
                        'M', casing,
                        'S', material(
                                "platinum",
                                MaterialPrefixes.WIRE,
                                1).getItem(),
                        'W', material(
                                cableMaterial,
                                MaterialPrefixes.CABLE,
                                1).getItem()));
    }

    private static List<MachineCraftingCase> t16MachineCraftingCases() {
        return List.of(
                t16MachineCraftingCase(
                        "lathe", "bronze",
                        ModItems.BRONZE_DOUBLE_MACHINE_CASING.get(),
                        ModItems.LATHE.get()),
                t16MachineCraftingCase(
                        "steel_lathe", "steel",
                        ModItems.STEEL_DOUBLE_MACHINE_CASING.get(),
                        ModItems.STEEL_LATHE.get()),
                t16MachineCraftingCase(
                        "titanium_lathe", "titanium",
                        ModItems.TITANIUM_DOUBLE_MACHINE_CASING.get(),
                        ModItems.TITANIUM_LATHE.get()),
                t16MachineCraftingCase(
                        "rollingmill", "bronze",
                        ModItems.BRONZE_DOUBLE_MACHINE_CASING.get(),
                        ModItems.ROLLINGMILL.get()),
                t16MachineCraftingCase(
                        "steel_rollingmill", "steel",
                        ModItems.STEEL_DOUBLE_MACHINE_CASING.get(),
                        ModItems.STEEL_ROLLINGMILL.get()),
                t16MachineCraftingCase(
                        "titanium_rollingmill", "titanium",
                        ModItems.TITANIUM_DOUBLE_MACHINE_CASING.get(),
                        ModItems.TITANIUM_ROLLINGMILL.get()),
                t16MachineCraftingCase(
                        "wiremill", "bronze",
                        ModItems.BRONZE_DOUBLE_MACHINE_CASING.get(),
                        ModItems.WIREMILL.get()),
                t16MachineCraftingCase(
                        "steel_wiremill", "steel",
                        ModItems.STEEL_DOUBLE_MACHINE_CASING.get(),
                        ModItems.STEEL_WIREMILL.get()),
                t16MachineCraftingCase(
                        "titanium_wiremill", "titanium",
                        ModItems.TITANIUM_DOUBLE_MACHINE_CASING.get(),
                        ModItems.TITANIUM_WIREMILL.get()),
                t16MachineCraftingCase(
                        "shredder", "bronze",
                        ModItems.BRONZE_DOUBLE_MACHINE_CASING.get(),
                        ModItems.SHREDDER.get()),
                t16MachineCraftingCase(
                        "steel_shredder", "steel",
                        ModItems.STEEL_DOUBLE_MACHINE_CASING.get(),
                        ModItems.STEEL_SHREDDER.get()),
                t16MachineCraftingCase(
                        "titanium_shredder", "titanium",
                        ModItems.TITANIUM_DOUBLE_MACHINE_CASING.get(),
                        ModItems.TITANIUM_SHREDDER.get()),
                t16MachineCraftingCase(
                        "press", "bronze",
                        ModItems.BRONZE_DOUBLE_MACHINE_CASING.get(),
                        ModItems.PRESS.get()),
                t16MachineCraftingCase(
                        "steel_press", "steel",
                        ModItems.STEEL_DOUBLE_MACHINE_CASING.get(),
                        ModItems.STEEL_PRESS.get()),
                t16MachineCraftingCase(
                        "titanium_press", "titanium",
                        ModItems.TITANIUM_DOUBLE_MACHINE_CASING.get(),
                        ModItems.TITANIUM_PRESS.get()));
    }

    private static MachineCraftingCase t16MachineCraftingCase(
            String id,
            String material,
            Item casing,
            Item result) {
        String kind = id.replaceFirst("^(steel|titanium)_", "");
        Item gear = material(
                material, MaterialPrefixes.GEAR, 1).getItem();
        List<String> pattern;
        Map<Character, Item> key;
        switch (kind) {
            case "lathe" -> {
                pattern = List.of("TDS", " CG");
                key = Map.of(
                        'C', casing,
                        'D', material(
                                "diamond",
                                MaterialPrefixes.GEM,
                                1).getItem(),
                        'G', gear,
                        'S', material(
                                material,
                                MaterialPrefixes.SMALL_GEAR,
                                1).getItem(),
                        'T', material(
                                material,
                                MaterialPrefixes.SCREW,
                                1).getItem());
            }
            case "rollingmill" -> {
                pattern = List.of("G ", "C ", "G ");
                key = Map.of('C', casing, 'G', gear);
            }
            case "wiremill" -> {
                pattern = List.of("SGS", " C ");
                key = Map.of(
                        'C', casing,
                        'G', gear,
                        'S', material(
                                material,
                                MaterialPrefixes.SMALL_GEAR,
                                1).getItem());
            }
            case "shredder" -> {
                pattern = List.of("GDG", " C ");
                key = Map.of(
                        'C', casing,
                        'D', material(
                                "diamond",
                                MaterialPrefixes.GEM,
                                1).getItem(),
                        'G', gear);
            }
            case "press" -> {
                pattern = List.of("RS", "PC", "P ");
                key = Map.of(
                        'C', casing,
                        'P', material(
                                material,
                                MaterialPrefixes.DOUBLE_PLATE,
                                1).getItem(),
                        'R', material(
                                material,
                                MaterialPrefixes.ROD,
                                1).getItem(),
                        'S', material(
                                material,
                                MaterialPrefixes.SPRING,
                                1).getItem());
            }
            default -> throw new IllegalArgumentException(
                    "Unsupported T16 machine " + id);
        }
        return new MachineCraftingCase(
                ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID, "machines/" + id),
                result,
                pattern,
                key);
    }

    private static List<MachineCraftingCase> t17MachineCraftingCases() {
        return List.of(
                t17DistilleryCraftingCase(
                        "distillery",
                        "steel",
                        "constantan",
                        MaterialPrefixes.DOUBLE_WIRE,
                        ModItems.STEEL_DOUBLE_MACHINE_CASING.get(),
                        ModItems.DISTILLERY.get()),
                t17DistilleryCraftingCase(
                        "invar_distillery",
                        "invar",
                        "kanthal",
                        MaterialPrefixes.QUADRUPLE_WIRE,
                        ModItems.STEEL_DOUBLE_MACHINE_CASING.get(),
                        ModItems.INVAR_DISTILLERY.get()),
                t17DistilleryCraftingCase(
                        "titanium_distillery",
                        "titanium",
                        "nichrome",
                        MaterialPrefixes.OCTUPLE_WIRE,
                        ModItems.TITANIUM_DOUBLE_MACHINE_CASING.get(),
                        ModItems.TITANIUM_DISTILLERY.get()),
                t17HeatBodyCraftingCase(
                        "drying",
                        "steel",
                        ModItems.STEEL_DOUBLE_MACHINE_CASING.get(),
                        ModItems.DRYING.get()),
                t17HeatBodyCraftingCase(
                        "invar_drying",
                        "invar",
                        ModItems.STEEL_DOUBLE_MACHINE_CASING.get(),
                        ModItems.INVAR_DRYING.get()),
                t17HeatBodyCraftingCase(
                        "titanium_drying",
                        "titanium",
                        ModItems.TITANIUM_DOUBLE_MACHINE_CASING.get(),
                        ModItems.TITANIUM_DRYING.get()),
                t17HeatBodyCraftingCase(
                        "smelter",
                        "steel",
                        ModItems.STEEL_DOUBLE_MACHINE_CASING.get(),
                        ModItems.SMELTER.get()),
                t17HeatBodyCraftingCase(
                        "invar_smelter",
                        "invar",
                        ModItems.STEEL_DOUBLE_MACHINE_CASING.get(),
                        ModItems.INVAR_SMELTER.get()),
                t17HeatBodyCraftingCase(
                        "titanium_smelter",
                        "titanium",
                        ModItems.TITANIUM_DOUBLE_MACHINE_CASING.get(),
                        ModItems.TITANIUM_SMELTER.get()));
    }

    private static MachineCraftingCase t17DistilleryCraftingCase(
            String id,
            String material,
            String wireMaterial,
            com.masson.cruciblecraft.api.material.MaterialPrefix wirePrefix,
            Item casing,
            Item result) {
        return new MachineCraftingCase(
                ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID, "machines/" + id),
                result,
                List.of("GPG", "WMW", " C "),
                Map.of(
                        'C', material(
                                "copper",
                                MaterialPrefixes.DOUBLE_PLATE,
                                1).getItem(),
                        'G', Items.GLASS,
                        'M', casing,
                        'P', material(
                                material,
                                MaterialPrefixes.PLATE,
                                1).getItem(),
                        'W', material(
                                wireMaterial,
                                wirePrefix,
                                1).getItem()));
    }

    private static MachineCraftingCase t17HeatBodyCraftingCase(
            String id,
            String material,
            Item casing,
            Item result) {
        boolean smelter = id.endsWith("smelter");
        Map<Character, Item> key = new java.util.LinkedHashMap<>();
        key.put('B', Items.BRICKS);
        key.put(
                'C',
                material(
                        "copper",
                        MaterialPrefixes.DOUBLE_PLATE,
                        1).getItem());
        key.put('M', casing);
        key.put(
                'P',
                material(material, MaterialPrefixes.PLATE, 1).getItem());
        if (smelter) {
            key.put('U', ModItems.CRUCIBLE.get());
        }
        return new MachineCraftingCase(
                ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID, "machines/" + id),
                result,
                smelter
                        ? List.of(" U ", "PMP", "BCB")
                        : List.of(" P ", "BMB", "BCB"),
                Map.copyOf(key));
    }

    private record MachineCraftingCase(
            ResourceLocation id,
            Item result,
            List<String> pattern,
            Map<Character, Item> key) {}

    @GameTest(
            template = TEMPLATE,
            batch = "component_runtime_copper",
            timeoutTicks = 2400)
    public static void componentChainCopperThroughRealMachines(GameTestHelper helper) {
        runComponentChain(helper, "copper");
    }

    @GameTest(
            template = TEMPLATE,
            batch = "component_runtime_tin",
            timeoutTicks = 2400)
    public static void componentChainTinThroughRealMachines(GameTestHelper helper) {
        runComponentChain(helper, "tin");
    }

    @GameTest(
            template = TEMPLATE,
            batch = "component_runtime_iron",
            timeoutTicks = 2400)
    public static void componentChainIronThroughRealMachines(GameTestHelper helper) {
        runComponentChain(helper, "iron");
    }

    @GameTest(
            template = TEMPLATE,
            batch = "component_runtime_gold",
            timeoutTicks = 2400)
    public static void componentChainGoldThroughRealMachines(GameTestHelper helper) {
        runComponentChain(helper, "gold");
    }

    private static void runComponentChain(GameTestHelper helper, String materialId) {
        ItemStack ingot = material(materialId, MaterialPrefixes.INGOT, 64);
        ItemStack plate = material(materialId, MaterialPrefixes.PLATE, 64);
        ItemStack foil = material(materialId, MaterialPrefixes.FOIL, 64);
        ItemStack shape = new ItemStack(ModItems.extruderShape("long_rod").get());
        shape.set(
                DataComponents.CUSTOM_NAME,
                Component.literal("component-chain-" + materialId));
        ItemStack originalShape = shape.copy();

        RecipeMap.Match rollingRecipe = requireComponentRecipe(
                helper,
                ModRecipeMaps.ROLLINGMILL,
                materialId,
                "rollingmill/ingot_to_plate/",
                MaterialPrefixes.PLATE,
                GTRecipeQuery.items(ingot));
        RecipeMap.Match assemblerRecipe = requireComponentRecipe(
                helper,
                ModRecipeMaps.ASSEMBLER,
                materialId,
                "assembler/plates_to_gear/",
                MaterialPrefixes.GEAR,
                GTRecipeQuery.items(plate));
        RecipeMap.Match latheRecipe = requireComponentRecipe(
                helper,
                ModRecipeMaps.LATHE,
                materialId,
                "lathe/ingot_to_rods/",
                MaterialPrefixes.ROD,
                GTRecipeQuery.items(ingot));
        RecipeMap.Match extruderRecipe = requireComponentRecipeByPath(
                helper,
                ModRecipeMaps.EXTRUDER,
                materialId,
                "extruder/long_rod/" + materialId + "/",
                MaterialPrefixes.LONG_ROD);
        RecipeMap.Match wireRecipe = requireComponentRecipe(
                helper,
                ModRecipeMaps.WIREMILL,
                materialId,
                "wiremill/ingot_to_wire/",
                MaterialPrefixes.WIRE,
                GTRecipeQuery.items(ingot));
        RecipeMap.Match cutterRecipe = requireComponentRecipe(
                helper,
                ModRecipeMaps.CUTTER,
                materialId,
                "cutter/plate_to_foil/",
                MaterialPrefixes.FOIL,
                GTRecipeQuery.items(plate));
        RecipeMap.Match fineWireRecipe = requireComponentRecipe(
                helper,
                ModRecipeMaps.WIREMILL,
                materialId,
                "wiremill/foil_to_fine_wire/",
                MaterialPrefixes.FINE_WIRE,
                GTRecipeQuery.items(foil));
        helper.assertTrue(
                extruderRecipe.recipe().itemInputs().size() == 2
                        && extruderRecipe.recipe().itemInputCounts().get(1) == 0
                        && extruderRecipe.recipe().itemInputs().get(1).test(shape),
                materialId + " extruder route does not require its exact shape presence-only");

        ConfiguredProcessingMachineBlockEntity rolling = placeConfigured(
                helper, new BlockPos(2, 2, 2),
                ModBlocks.ROLLINGMILL.get(), ModProcessingMachines.ROLLINGMILL);
        ConfiguredProcessingMachineBlockEntity assembler = placeConfigured(
                helper, new BlockPos(6, 2, 2),
                ModBlocks.ASSEMBLER.get(), ModProcessingMachines.ASSEMBLER);
        ConfiguredProcessingMachineBlockEntity lathe = placeConfigured(
                helper, new BlockPos(10, 2, 2),
                ModBlocks.LATHE.get(), ModProcessingMachines.LATHE);
        ConfiguredProcessingMachineBlockEntity extruder = placeConfigured(
                helper, new BlockPos(14, 2, 2),
                ModBlocks.EXTRUDER.get(), ModProcessingMachines.EXTRUDER);
        ConfiguredProcessingMachineBlockEntity wiremill = placeConfigured(
                helper, new BlockPos(18, 2, 2),
                ModBlocks.WIREMILL.get(), ModProcessingMachines.WIREMILL);
        ConfiguredProcessingMachineBlockEntity cutter = placeConfigured(
                helper, new BlockPos(22, 2, 2),
                ModBlocks.CUTTER.get(), ModProcessingMachines.CUTTER);

        int cutterCycles = cyclesFor(
                fineWireRecipe.recipe().itemInputCounts().getFirst(),
                cutterRecipe.recipe().itemOutputs().getFirst().getCount());
        int neededPlates = assemblerRecipe.recipe().itemInputCounts().getFirst()
                + cutterRecipe.recipe().itemInputCounts().getFirst() * cutterCycles;
        int rollingCycles = cyclesFor(
                neededPlates, rollingRecipe.recipe().itemOutputs().getFirst().getCount());
        ComponentStage rollingStage = stage(
                rolling, rollingRecipe, rollingCycles, materialId + " rollingmill");
        ComponentStage latheStage = stage(
                lathe, latheRecipe, 1, materialId + " lathe");
        ComponentStage extruderStage = stage(
                extruder, extruderRecipe, 1, materialId + " extruder");
        ComponentStage wireStage = stage(
                wiremill, wireRecipe, 1, materialId + " wiremill ingot");

        insertExact(
                helper, rolling, 0,
                ingredientSample(rollingRecipe.recipe(), 0, rollingCycles),
                rollingStage.label());
        insertExact(
                helper, lathe, 0,
                ingredientSample(latheRecipe.recipe(), 0, 1),
                latheStage.label());
        insertExact(
                helper, extruder, 0,
                ingredientSample(extruderRecipe.recipe(), 0, 1),
                extruderStage.label());
        insertExact(helper, extruder, 1, shape, extruderStage.label() + " shape");
        insertExact(
                helper, wiremill, 0,
                ingredientSample(wireRecipe.recipe(), 0, 1),
                wireStage.label());

        ComponentStage[] firstPhase = {
                rollingStage, latheStage, extruderStage, wireStage
        };
        int firstPhaseTicks = phaseTicks(firstPhase);
        final ItemStack[] producedFoil = {ItemStack.EMPTY};

        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> assertDeclaredDurations(helper, firstPhase))
                .thenExecuteFor(
                        firstPhaseTicks,
                        () -> powerUntilOutput(helper, firstPhase))
                .thenExecute(() -> {
                    ItemStack producedPlates = takeStageOutput(helper, rollingStage);
                    takeStageOutput(helper, latheStage);
                    takeStageOutput(helper, extruderStage);
                    takeStageOutput(helper, wireStage);
                    assertShapeUnchanged(helper, extruder, originalShape, materialId);

                    int assemblerPlates =
                            assemblerRecipe.recipe().itemInputCounts().getFirst();
                    int cutterPlates =
                            cutterRecipe.recipe().itemInputCounts().getFirst() * cutterCycles;
                    ItemStack gearInput = producedPlates.split(assemblerPlates);
                    ItemStack cutterInput = producedPlates.split(cutterPlates);
                    helper.assertTrue(
                            gearInput.getCount() == assemblerPlates
                                    && cutterInput.getCount() == cutterPlates,
                            materialId + " rollingmill output could not feed both plate routes");
                    insertExact(
                            helper, assembler, 0, gearInput,
                            materialId + " assembler plate transfer");
                    insertExact(
                            helper, cutter, 0, cutterInput,
                            materialId + " cutter plate transfer");
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    assertDeclaredDuration(helper, assembler, assemblerRecipe, materialId);
                    assertDeclaredDuration(helper, cutter, cutterRecipe, materialId);
                })
                .thenExecuteFor(
                        phaseTicks(
                                stage(assembler, assemblerRecipe, 1,
                                        materialId + " assembler"),
                                stage(cutter, cutterRecipe, cutterCycles,
                                        materialId + " cutter")),
                        new Runnable() {
                            private final ComponentStage[] phase = {
                                    stage(assembler, assemblerRecipe, 1,
                                            materialId + " assembler"),
                                    stage(cutter, cutterRecipe, cutterCycles,
                                            materialId + " cutter")
                            };
                            @Override public void run() {
                                powerUntilOutput(helper, phase);
                            }
                        })
                .thenExecute(() -> {
                    ComponentStage gearStage =
                            stage(assembler, assemblerRecipe, 1, materialId + " assembler");
                    ComponentStage foilStage =
                            stage(cutter, cutterRecipe, cutterCycles, materialId + " cutter");
                    takeStageOutput(helper, gearStage);
                    producedFoil[0] = takeStageOutput(helper, foilStage);
                    int fineWireFoils =
                            fineWireRecipe.recipe().itemInputCounts().getFirst();
                    ItemStack fineWireInput = producedFoil[0].split(fineWireFoils);
                    helper.assertTrue(
                            fineWireInput.getCount() == fineWireFoils,
                            materialId + " cutter output could not feed fine-wire route");
                    insertExact(
                            helper, wiremill, 0, fineWireInput,
                            materialId + " wiremill foil transfer");
                })
                .thenIdle(2)
                .thenExecute(() ->
                        assertDeclaredDuration(helper, wiremill, fineWireRecipe, materialId))
                .thenExecuteFor(
                        fineWireRecipe.recipe().duration() + 12,
                        () -> powerUntilOutput(
                                helper,
                                stage(
                                        wiremill,
                                        fineWireRecipe,
                                        1,
                                        materialId + " wiremill foil")))
                .thenExecute(() -> {
                    takeStageOutput(
                            helper,
                            stage(
                                    wiremill,
                                    fineWireRecipe,
                                    1,
                                    materialId + " wiremill foil"));
                    assertShapeUnchanged(helper, extruder, originalShape, materialId);
                    CrucibleCraft.LOGGER.info(
                            "component-runtime {}: rollingmill {}t -> {}; assembler {}t -> {}; "
                                    + "lathe {}t -> {}; extruder {}t -> {}; wiremill-ingot {}t -> {}; "
                                    + "cutter {}t -> {}; wiremill-foil {}t -> {}",
                            materialId,
                            rollingRecipe.recipe().duration(),
                            rollingRecipe.recipe().itemOutputs().getFirst(),
                            assemblerRecipe.recipe().duration(),
                            assemblerRecipe.recipe().itemOutputs().getFirst(),
                            latheRecipe.recipe().duration(),
                            latheRecipe.recipe().itemOutputs().getFirst(),
                            extruderRecipe.recipe().duration(),
                            extruderRecipe.recipe().itemOutputs().getFirst(),
                            wireRecipe.recipe().duration(),
                            wireRecipe.recipe().itemOutputs().getFirst(),
                            cutterRecipe.recipe().duration(),
                            cutterRecipe.recipe().itemOutputs().getFirst(),
                            fineWireRecipe.recipe().duration(),
                            fineWireRecipe.recipe().itemOutputs().getFirst());
                })
                .thenSucceed();
    }

    private static RecipeMap.Match requireComponentRecipe(
            GameTestHelper helper,
            RecipeMap map,
            String materialId,
            String expectedPathPrefix,
            com.masson.cruciblecraft.api.material.MaterialPrefix expectedOutput,
            GTRecipeQuery query) {
        RecipeMap.Match match = map.findMatch(query).orElse(null);
        helper.assertTrue(
                match != null,
                map.id() + " has no live component recipe for " + materialId);
        helper.assertTrue(
                match.id().getPath().startsWith(expectedPathPrefix),
                map.id() + " resolved unexpected recipe " + match.id());
        GTRecipe recipe = match.recipe();
        helper.assertTrue(
                recipe.itemOutputs().size() == 1
                        && recipe.outputChances().equals(List.of(GTRecipe.GUARANTEED_CHANCE))
                        && ItemStack.isSameItemSameComponents(
                                recipe.itemOutputs().getFirst(),
                                material(materialId, expectedOutput, 1)),
                match.id() + " does not guarantee the requested " + expectedOutput);
        return match;
    }

    private static RecipeMap.Match requireComponentRecipeByPath(
            GameTestHelper helper,
            RecipeMap map,
            String materialId,
            String expectedPathPrefix,
            com.masson.cruciblecraft.api.material.MaterialPrefix expectedOutput) {
        RecipeMap.Entry entry = map.entries().stream()
                .filter(candidate ->
                        candidate.id().getPath().startsWith(expectedPathPrefix))
                .findFirst()
                .orElse(null);
        helper.assertTrue(
                entry != null,
                map.id() + " has no live component recipe for " + materialId);
        GTRecipe recipe = entry.recipe();
        helper.assertTrue(
                recipe.itemOutputs().size() == 1
                        && recipe.outputChances().equals(List.of(GTRecipe.GUARANTEED_CHANCE))
                        && ItemStack.isSameItemSameComponents(
                                recipe.itemOutputs().getFirst(),
                                material(materialId, expectedOutput, 1)),
                entry.id() + " does not guarantee the requested " + expectedOutput);
        return new RecipeMap.Match(entry.id(), recipe);
    }

    private static ComponentStage stage(
            ConfiguredProcessingMachineBlockEntity machine,
            RecipeMap.Match match,
            int cycles,
            String label) {
        return new ComponentStage(machine, match, cycles, label);
    }

    private static int cyclesFor(int needed, int perCycle) {
        return Math.floorDiv(needed + perCycle - 1, perCycle);
    }

    private static int phaseTicks(ComponentStage... stages) {
        return java.util.Arrays.stream(stages)
                .mapToInt(stage -> Math.multiplyExact(
                        stage.match().recipe().duration(), stage.cycles()))
                .max()
                .orElseThrow() + 12;
    }

    private static ItemStack ingredientSample(
            GTRecipe recipe, int index, int cycles) {
        ItemStack input = recipe.itemInputs().get(index).getItems()[0].copy();
        input.setCount(Math.multiplyExact(recipe.itemInputCounts().get(index), cycles));
        return input;
    }

    private static void insertExact(
            GameTestHelper helper,
            ConfiguredProcessingMachineBlockEntity machine,
            int inputSlot,
            ItemStack input,
            String label) {
        IItemHandler automation = machine.items(Direction.WEST);
        helper.assertTrue(automation != null, label + " input capability missing");
        ItemStack remainder = automation.insertItem(inputSlot, input, false);
        helper.assertTrue(remainder.isEmpty(), label + " rejected real input transfer");
    }

    private static void assertDeclaredDurations(
            GameTestHelper helper, ComponentStage... stages) {
        for (ComponentStage stage : stages) {
            assertDeclaredDuration(
                    helper, stage.machine(), stage.match(), stage.label());
        }
    }

    private static void assertDeclaredDuration(
            GameTestHelper helper,
            ConfiguredProcessingMachineBlockEntity machine,
            RecipeMap.Match match,
            String label) {
        int expectedDuration = MachineExecutionPlan.create(
                        match.recipe(),
                        machine.variant().kind(),
                        machine.variant().tier(),
                        1)
                .orElseThrow()
                .effectiveDuration();
        helper.assertTrue(
                machine.duration() == expectedDuration,
                label + " selected duration " + machine.duration()
                        + " instead of variant execution duration "
                        + expectedDuration);
    }

    private static void powerUntilOutput(
            GameTestHelper helper, ComponentStage... stages) {
        for (ComponentStage stage : stages) {
            if (!stageOutputReady(stage)) {
                fillKuCapability(helper, stage.machine());
            }
        }
    }

    private static boolean stageOutputReady(ComponentStage stage) {
        ItemStack expected = stage.match().recipe().itemOutputs().getFirst();
        ItemStack actual = stage.machine().inventory().getStackInSlot(
                stage.machine().spec().items().outputs().getFirst());
        return ItemStack.isSameItemSameComponents(actual, expected)
                && actual.getCount() >= Math.multiplyExact(
                        expected.getCount(), stage.cycles());
    }

    private static ItemStack takeStageOutput(
            GameTestHelper helper, ComponentStage stage) {
        ItemStack expected = stage.match().recipe().itemOutputs().getFirst();
        int expectedCount = Math.multiplyExact(expected.getCount(), stage.cycles());
        int outputSlot = stage.machine().spec().items().outputs().getFirst();
        ItemStack observed = stage.machine().inventory().getStackInSlot(outputSlot);
        helper.assertTrue(
                ItemStack.isSameItemSameComponents(observed, expected)
                        && observed.getCount() >= expectedCount,
                stage.label() + " did not expose its real target output; observed "
                        + observed + ", expected " + expected.copyWithCount(expectedCount));
        IItemHandler automation = stage.machine().items(Direction.NORTH);
        helper.assertTrue(automation != null, stage.label() + " output capability missing");
        ItemStack extracted = automation.extractItem(0, expectedCount, false);
        helper.assertTrue(
                ItemStack.isSameItemSameComponents(extracted, expected)
                        && extracted.getCount() == expectedCount,
                stage.label() + " output capability did not transfer the observed output");
        return extracted;
    }

    private static void assertShapeUnchanged(
            GameTestHelper helper,
            ConfiguredProcessingMachineBlockEntity extruder,
            ItemStack originalShape,
            String materialId) {
        ItemStack retained = extruder.inventory().getStackInSlot(1);
        helper.assertTrue(
                retained.getCount() == originalShape.getCount()
                        && ItemStack.isSameItemSameComponents(retained, originalShape),
                materialId + " extruder consumed or changed the shape stack");
        int shapeCount = 0;
        for (int slot = 0; slot < extruder.inventory().getSlots(); slot++) {
            ItemStack stack = extruder.inventory().getStackInSlot(slot);
            if (ExtruderShapeCatalog.isShape(stack)) {
                shapeCount += stack.getCount();
            }
        }
        helper.assertTrue(shapeCount == 1, materialId + " extruder duplicated its shape");
    }

    private record ComponentStage(
            ConfiguredProcessingMachineBlockEntity machine,
            RecipeMap.Match match,
            int cycles,
            String label) {}

    @GameTest(template = TEMPLATE, timeoutTicks = 160)
    public static void distilleryFluidPipeFeedsMixer(
            GameTestHelper helper) {
        BlockPos distilleryPos = new BlockPos(3, 2, 5);
        BlockPos pipePos = distilleryPos.east();
        BlockPos mixerPos = pipePos.east();
        helper.setBlock(
                distilleryPos,
                ModBlocks.DISTILLERY.get().defaultBlockState()
                        .setValue(
                                ProcessingMachineBlock.FACING,
                                Direction.EAST));
        helper.setBlock(
                mixerPos,
                ModBlocks.MIXER.get().defaultBlockState()
                        .setValue(
                                ProcessingMachineBlock.FACING,
                                Direction.EAST));
        FluidPipeBlock block = (FluidPipeBlock) ModBlocks.pipeBlock(
                "copper",
                MaterialPrefixes.TINY_FLUID_PIPE,
                PipeCatalog.Kind.FLUID).get();
        helper.setBlock(pipePos, block);
        helper.getLevel().setBlock(
                helper.absolutePos(pipePos),
                helper.getBlockState(pipePos)
                        .setValue(FluidPipeBlock.WEST, true)
                        .setValue(FluidPipeBlock.EAST, true),
                Block.UPDATE_CLIENTS);

        ConfiguredProcessingMachineBlockEntity distillery =
                helper.getBlockEntity(distilleryPos);
        ConfiguredProcessingMachineBlockEntity mixer =
                helper.getBlockEntity(mixerPos);
        FluidPipeBlockEntity pipe = helper.getBlockEntity(pipePos);
        FluidStack routedFluid = ModRecipeMaps.MIXER.recipes().stream()
                .flatMap(recipe -> recipe.fluidInputs().stream())
                .findFirst()
                .orElseThrow()
                .copyWithAmount(100);
        distillery.tanks().get(2).setFluid(
                routedFluid);
        helper.assertTrue(
                pipe.setCover(Direction.WEST, PipeCover.pump()),
                "Could not install output pump");
        IFluidHandler mixerInput = helper.getLevel().getCapability(
                Capabilities.FluidHandler.BLOCK,
                helper.absolutePos(mixerPos),
                Direction.WEST);
        helper.assertTrue(
                mixerInput != null,
                "Mixer did not expose its west fluid capability");
        helper.assertTrue(
                mixerInput.fill(
                        routedFluid,
                        IFluidHandler.FluidAction.SIMULATE) == 100,
                "Mixer west fluid capability rejected routed fluid");

        helper.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    helper.assertTrue(
                            mixer.tanks().getFirst().getFluidAmount() == 100,
                            "Fluid pipe did not feed the mixer input: pipe="
                                    + pipe.storedFluid().getAmount()
                                    + ", output="
                                    + distillery.tanks().get(2)
                                            .getFluidAmount()
                                    + ", mixer="
                                    + mixer.tanks().getFirst()
                                            .getFluidAmount()
                                    + ", state="
                                    + helper.getBlockState(pipePos));
                    helper.assertTrue(
                            distillery.tanks().get(2).isEmpty(),
                            "Output pump did not drain the distillery");
                    helper.assertTrue(
                            pipe.failureSnapshot().pendingFailure()
                                    == Failure.NONE,
                            "Safe water route unexpectedly failed");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 160)
    public static void itemPipeFilterValvePumpAutomatesRoute(
            GameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(3, 2, 9);
        BlockPos firstPos = sourcePos.east();
        BlockPos secondPos = firstPos.east();
        BlockPos thirdPos = secondPos.east();
        BlockPos destinationPos = thirdPos.east();
        helper.setBlock(
                sourcePos,
                ModBlocks.MORTAR.get().defaultBlockState()
                        .setValue(
                                ProcessingMachineBlock.FACING,
                                Direction.EAST));
        helper.setBlock(
                destinationPos, Blocks.CHEST.defaultBlockState());
        ItemPipeBlock block = (ItemPipeBlock) ModBlocks.pipeBlock(
                "copper",
                MaterialPrefixes.ITEM_PIPE,
                PipeCatalog.Kind.ITEM).get();
        helper.setBlock(firstPos, block);
        helper.setBlock(secondPos, block);
        helper.setBlock(thirdPos, block);

        ConfiguredProcessingMachineBlockEntity source =
                helper.getBlockEntity(sourcePos);
        ChestBlockEntity destination =
                helper.getBlockEntity(destinationPos);
        ItemPipeBlockEntity first = helper.getBlockEntity(firstPos);
        ItemPipeBlockEntity second = helper.getBlockEntity(secondPos);
        ItemPipeBlockEntity third = helper.getBlockEntity(thirdPos);
        source.inventory().setStackInSlot(
                1, new ItemStack(Items.IRON_INGOT, 3));
        first.setCover(Direction.WEST, PipeCover.pump());
        second.setCover(
                Direction.WEST,
                PipeCover.filter("minecraft:iron_ingot"));
        third.setCover(Direction.EAST, PipeCover.valve());

        helper.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    helper.assertTrue(
                            source.inventory().getStackInSlot(1).isEmpty(),
                            "Item pump did not drain source output");
                    helper.assertTrue(
                            destination.getItem(0).getCount()
                                    == 3,
                            "Covered item route did not deliver three ingots "
                                    + "to a vanilla chest");
                    helper.assertTrue(
                            first.totalDelivered()
                                            + second.totalDelivered()
                                            + third.totalDelivered()
                                    > 0,
                            "Actual committed item throughput was not recorded");
                })
                .thenExecute(() -> {
                    helper.assertTrue(
                            second.setCover(
                                    Direction.WEST,
                                    PipeCover.filter(
                                            "minecraft:gold_ingot")),
                            "Route invalidation cover did not change");
                    source.inventory().setStackInSlot(
                            1, new ItemStack(Items.IRON_INGOT, 2));
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    helper.assertTrue(
                            source.inventory().getStackInSlot(1)
                                            .getCount()
                                    == 2
                                    && destination.getItem(0).getCount()
                                            == 3,
                            "Invalidated item route reused a stale discovery");
                    helper.assertTrue(
                            second.setCover(
                                    Direction.WEST,
                                    PipeCover.filter(
                                            "minecraft:iron_ingot")),
                            "Route recovery cover did not change");
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    helper.assertTrue(
                            source.inventory().getStackInSlot(1).isEmpty()
                                    && destination.getItem(0).getCount()
                                            == 5,
                            "Rediscovered item route did not resume delivery");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void copperTinIronUseCommonPipeCatalog(
            GameTestHelper helper) {
        for (String material : List.of("copper", "tin", "iron")) {
            helper.assertTrue(
                    PipeCatalog.contains(
                            material,
                            MaterialPrefixes.TINY_FLUID_PIPE,
                            PipeCatalog.Kind.FLUID),
                    material + " lacks data-driven fluid pipes");
            helper.assertTrue(
                    PipeCatalog.contains(
                            material,
                            MaterialPrefixes.ITEM_PIPE,
                            PipeCatalog.Kind.ITEM),
                    material + " lacks data-driven item pipes");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void fluidPipeCapacityTemperatureAndCorrosionFailClosed(
            GameTestHelper helper) {
        FluidPipeBlock block = (FluidPipeBlock) ModBlocks.pipeBlock(
                "copper",
                MaterialPrefixes.TINY_FLUID_PIPE,
                PipeCatalog.Kind.FLUID).get();
        BlockPos capacityPos = new BlockPos(4, 2, 13);
        BlockPos hotPos = new BlockPos(8, 2, 13);
        BlockPos acidPos = new BlockPos(12, 2, 13);
        for (BlockPos pipePos : List.of(
                capacityPos, hotPos, acidPos)) {
            helper.setBlock(
                    pipePos.west(),
                    ModBlocks.MIXER.get().defaultBlockState()
                            .setValue(
                                    ProcessingMachineBlock.FACING,
                                    Direction.EAST));
            helper.setBlock(pipePos, block);
            helper.getLevel().setBlock(
                    helper.absolutePos(pipePos),
                    helper.getBlockState(pipePos)
                            .setValue(FluidPipeBlock.WEST, true),
                    Block.UPDATE_CLIENTS);
        }
        FluidPipeBlockEntity capacity = helper.getBlockEntity(capacityPos);
        FluidPipeBlockEntity hot = helper.getBlockEntity(hotPos);
        FluidPipeBlockEntity acid = helper.getBlockEntity(acidPos);

        CompoundTag malformed = capacity.getUpdateTag(
                helper.getLevel().registryAccess());
        malformed.putString("pending_failure", "garbage");
        malformed.putInt("corrosion", -1);
        CompoundTag invalidCover = new CompoundTag();
        invalidCover.putString("side", "north");
        invalidCover.putString("type", "filter");
        invalidCover.putString("match", "NOT A VALID ID");
        ListTag invalidCovers = new ListTag();
        invalidCovers.add(invalidCover);
        malformed.put("covers", invalidCovers);
        capacity.handleUpdateTag(
                malformed, helper.getLevel().registryAccess());
        helper.assertTrue(
                capacity.failureSnapshot().equals(
                        FluidPipeFailureState.Snapshot.EMPTY),
                "Malformed failure NBT was not quarantined");
        helper.assertTrue(
                capacity.coverSnapshot().isEmpty(),
                "Malformed cover NBT was not quarantined");

        int capacityLimit = capacity.capacity();
        int filled = capacity.fluidHandler(Direction.WEST).fill(
                new FluidStack(Fluids.WATER, capacityLimit + 100),
                IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(
                filled == capacityLimit,
                "Capacity route did not apply exact backpressure: filled="
                        + filled + ", capacity=" + capacityLimit
                        + ", state=" + helper.getBlockState(capacityPos));
        helper.assertTrue(
                capacity.failureSnapshot().backpressureAmount() == 100,
                "Backpressure used planned instead of committed amount");

        FluidStack moltenTungsten = new FluidStack(
                ModFluids.materialFluid("tungsten").orElseThrow(), 1);
        FluidStack hydrochloricAcid = new FluidStack(
                ModFluids.materialFluid("hydrochloric_acid")
                        .orElseThrow(),
                1);
        for (int event = 0; event < 4; event++) {
            hot.fluidHandler(Direction.WEST).fill(
                    moltenTungsten,
                    IFluidHandler.FluidAction.EXECUTE);
            acid.fluidHandler(Direction.WEST).fill(
                    hydrochloricAcid,
                    IFluidHandler.FluidAction.EXECUTE);
        }

        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(
                            helper.getBlockState(hotPos).isAir(),
                            "Over-temperature pipe did not fail closed");
                    helper.assertTrue(
                            helper.getBlockState(acidPos).isAir(),
                            "Corroded pipe did not fail closed");
                    helper.assertTrue(
                            !helper.getBlockState(capacityPos).isAir(),
                            "Backpressure destroyed a healthy pipe");
                })
                .thenSucceed();
    }

    private static ConfiguredProcessingMachineBlockEntity placeConfigured(
            GameTestHelper helper, BlockPos pos, Block block, ProcessingMachineSpec spec) {
        helper.setBlock(pos, block);
        ConfiguredProcessingMachineBlockEntity machine = helper.getBlockEntity(pos);
        helper.assertTrue(
                machine.spec() == spec
                        || machine.variant().kind().behavior() == spec,
                "Placed block resolved wrong machine kind");
        return machine;
    }

    private static void assertPublishedOreChain(
            GameTestHelper helper, String materialId) {
        assertPublished(
                helper,
                ModRecipeMaps.CRUSHER,
                "crusher",
                materialId,
                GTRecipeQuery.items(material(
                        materialId, MaterialPrefixes.RAW_ORE, 64)));
        assertPublished(
                helper,
                ModRecipeMaps.SLUICE,
                "sluice",
                materialId,
                new GTRecipeQuery(
                        List.of(material(
                                materialId,
                                MaterialPrefixes.CRUSHED_ORE,
                                64)),
                        List.of(new FluidStack(Fluids.WATER, 1_000))));
        assertPublished(
                helper,
                ModRecipeMaps.CENTRIFUGE,
                "centrifuge",
                materialId,
                GTRecipeQuery.items(material(
                        materialId,
                        MaterialPrefixes.WASHED_CRUSHED_ORE,
                        64)));
        assertPublished(
                helper,
                ModRecipeMaps.SHREDDER,
                "shredder",
                materialId,
                GTRecipeQuery.items(material(
                        materialId,
                        MaterialPrefixes.CENTRIFUGED_CRUSHED_ORE,
                        64)));
        assertPublished(
                helper,
                ModRecipeMaps.SIFTER,
                "sifter",
                materialId,
                GTRecipeQuery.items(material(
                        materialId, MaterialPrefixes.PURIFIED_DUST, 64)));
        assertPublished(
                helper,
                ModRecipeMaps.SMELTER,
                "smelter",
                materialId,
                GTRecipeQuery.items(material(
                        materialId, MaterialPrefixes.DUST, 64)));
    }

    private static void assertPublished(
            GameTestHelper helper,
            RecipeMap map,
            String mapName,
            String materialId,
            GTRecipeQuery query) {
        RecipeMap.Match match = map.findMatch(query).orElse(null);
        helper.assertTrue(
                match != null,
                mapName + " did not resolve " + materialId + " from the live map");
        helper.assertTrue(
                match.id().getPath().startsWith(
                        "ore_chain/" + mapName + "/" + materialId + "/"),
                mapName + "/" + materialId
                        + " resolved a non-concrete recipe " + match.id());
    }

    private static void assertWaterPolicy(
            GameTestHelper helper, ConfiguredProcessingMachineBlockEntity machine) {
        IFluidHandler external = machine.fluids(Direction.WEST);
        helper.assertTrue(external != null, "Water input capability missing");
        helper.assertTrue(external.fill(
                new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.SIMULATE) == 0,
                "Machine accepted wrong fluid");
        helper.assertTrue(external.fill(
                new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 1000,
                "Machine rejected recipe water");
        helper.assertTrue(external.drain(
                100, IFluidHandler.FluidAction.SIMULATE).isEmpty(),
                "External input capability allowed drain");
    }

    private static int inputFluidAmount(
            ConfiguredProcessingMachineBlockEntity machine) {
        return machine.tanks().get(
                machine.spec().fluids().inputs().getFirst().index()).getFluidAmount();
    }

    private static boolean sameFluidAmount(
            FluidStack first,
            FluidStack second) {
        return first.getAmount() == second.getAmount()
                && FluidStack.isSameFluidSameComponents(first, second);
    }

    private static void fillWater(ConfiguredProcessingMachineBlockEntity machine) {
        IFluidHandler handler = machine.fluids(Direction.WEST);
        if (handler != null) {
            handler.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    private static void fillKuCapability(GameTestHelper helper, BlockPos pos) {
        BlockPos worldPos = helper.getBlockEntity(pos).getBlockPos();
        fillKuCapability(helper, worldPos, pos.toString());
    }

    private static void fillKuCapability(
            GameTestHelper helper, ConfiguredProcessingMachineBlockEntity machine) {
        fillKuCapability(helper, machine.getBlockPos(), machine.spec().id().toString());
    }

    private static void fillKuCapability(
            GameTestHelper helper, BlockPos worldPos, String label) {
        IEnergyHandler energy = helper.getLevel().getCapability(
                ModCapabilities.ENERGY, worldPos, Direction.SOUTH);
        helper.assertTrue(
                energy != null,
                "Back energy capability missing at " + label);
        EnergyType type = List.of(
                        EnergyType.KINETIC_PUSH,
                        EnergyType.KINETIC_ROTATION,
                        EnergyType.KINETIC,
                        EnergyType.ELECTRIC,
                        EnergyType.HEAT)
                .stream()
                .filter(candidate ->
                        energy.handles(candidate, Direction.SOUTH))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No accepted energy identity at " + label));
        long accepted = energy.insert(
                type, 32L, 16L, Direction.SOUTH, false);
        helper.assertTrue(
                accepted > 0L || energy.stored(type) > 0L,
                "Energy capability accepted no "
                        + type
                        + " at "
                        + label);
    }

    private static void fillKu(
            GameTestHelper helper, ConfiguredProcessingMachineBlockEntity machine) {
        EnergyType type = machine.spec().energy().type();
        long accepted = machine.insert(
                type, 32L, 16L, Direction.SOUTH, false);
        helper.assertTrue(
                accepted > 0L || machine.stored(type) > 0L,
                "Placed machine accepted no "
                        + type
                        + ": "
                        + machine.spec().id());
    }

    private static void forceLastTick(
            GameTestHelper helper, ConfiguredProcessingMachineBlockEntity machine) {
        helper.assertTrue(machine.duration() > 0, machine.spec().id() + " has no selected recipe");
        machine.runtime().processor().setProgress(machine.duration() - 1);
    }

    private static void forceLastTick(GameTestHelper helper, CrusherBlockEntity machine) {
        helper.assertTrue(machine.duration() > 0, "Crusher has no selected recipe");
        machine.runtime().processor().setProgress(machine.duration() - 1);
    }

    private static int inputIndex(GTRecipe recipe, Item item) {
        for (int index = 0; index < recipe.itemInputs().size(); index++) {
            if (java.util.Arrays.stream(recipe.itemInputs().get(index).getItems())
                    .anyMatch(stack -> stack.is(item))) {
                return index;
            }
        }
        throw new IllegalArgumentException(
                "Recipe does not contain input " + item);
    }

    private static void loadRecipeInputs(
            ConfiguredProcessingMachineBlockEntity machine, GTRecipe recipe) {
        for (int i = 0; i < recipe.itemInputs().size(); i++) {
            ItemStack sample = recipe.itemInputs().get(i).getItems()[0].copy();
            sample.setCount(Math.max(1, recipe.itemInputCounts().get(i)));
            machine.inventory().setStackInSlot(machine.spec().items().inputs().get(i), sample);
        }
        for (int i = 0; i < recipe.fluidInputs().size(); i++) {
            machine.tanks().get(machine.spec().fluids().inputs().get(i).index())
                    .setFluid(recipe.fluidInputs().get(i).copy());
        }
    }

    private static GTRecipe requireRecipe(RecipeMap map, String path) {
        return map.entries().stream()
                .filter(entry -> entry.id().getPath().equals(path))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing live recipe " + map.id() + "/" + path))
                .recipe();
    }

    private static void drainMachineIntoItem(
            GameTestHelper helper,
            IFluidHandler source,
            IFluidHandler target) {
        helper.assertTrue(source != null, "Machine output drain view missing");
        FluidStack available = source.drain(
                Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
        helper.assertTrue(!available.isEmpty(), "Machine produced no drainable fluid");
        int accepted = target.fill(available, IFluidHandler.FluidAction.SIMULATE);
        helper.assertTrue(
                accepted == available.getAmount(),
                "Portable tank could not accept complete machine output");
        FluidStack drained = source.drain(
                available.copyWithAmount(accepted),
                IFluidHandler.FluidAction.EXECUTE);
        int filled = target.fill(drained, IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(
                filled == drained.getAmount(),
                "Portable tank execute fill violated simulation");
    }

    private static void fillMachineFromItem(
            GameTestHelper helper,
            IFluidHandler source,
            IFluidHandler target) {
        helper.assertTrue(target != null, "Machine input fill view missing");
        FluidStack offered = source.drain(
                Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
        helper.assertTrue(!offered.isEmpty(), "Portable tank offered no fluid");
        int accepted = target.fill(offered, IFluidHandler.FluidAction.SIMULATE);
        helper.assertTrue(
                accepted == offered.getAmount(),
                "Machine could not accept complete portable tank contents");
        FluidStack drained = source.drain(
                offered.copyWithAmount(accepted),
                IFluidHandler.FluidAction.EXECUTE);
        int filled = target.fill(drained, IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(
                filled == drained.getAmount(),
                "Machine execute fill violated simulation");
    }

    private static void transferFluid(
            GameTestHelper helper,
            IFluidHandler source,
            IFluidHandler target,
            int amount) {
        helper.assertTrue(
                source != null && target != null && amount > 0,
                "T11 fluid transfer endpoints are unavailable");
        FluidStack offered = source.drain(
                amount, IFluidHandler.FluidAction.SIMULATE);
        helper.assertTrue(
                offered.getAmount() == amount,
                "T11 source did not expose the expected fluid amount");
        helper.assertTrue(
                target.fill(
                                offered,
                                IFluidHandler.FluidAction.SIMULATE)
                        == amount,
                "T11 target rejected the simulated fluid transfer");
        FluidStack drained = source.drain(
                offered, IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(
                drained.getAmount() == amount
                        && target.fill(
                                        drained,
                                        IFluidHandler.FluidAction.EXECUTE)
                                == amount,
                "T11 fluid transfer changed after simulation");
    }

    private static GTRecipeQuery queryFor(GTRecipe recipe) {
        List<ItemStack> items = new ArrayList<>(recipe.itemInputs().size());
        for (int index = 0; index < recipe.itemInputs().size(); index++) {
            ItemStack sample =
                    recipe.itemInputs().get(index).getItems()[0].copy();
            sample.setCount(Math.max(1, recipe.itemInputCounts().get(index)));
            items.add(sample);
        }
        return new GTRecipeQuery(items, recipe.fluidInputs());
    }

    private static void transferPrimary(
            GameTestHelper helper,
            ConfiguredProcessingMachineBlockEntity from,
            ConfiguredProcessingMachineBlockEntity to) {
        ItemStack output = ItemStack.EMPTY;
        for (int slot : from.spec().items().outputs()) {
            if (!from.inventory().getStackInSlot(slot).isEmpty()) {
                output = from.inventory().extractItem(slot, 1, false);
                break;
            }
        }
        helper.assertTrue(!output.isEmpty(), from.spec().id() + " primary output missing");
        to.inventory().setStackInSlot(to.spec().items().inputs().getFirst(), output);
    }

    private static boolean hasAnyOutput(ConfiguredProcessingMachineBlockEntity machine) {
        return machine.spec().items().outputs().stream()
                .anyMatch(slot -> !machine.inventory().getStackInSlot(slot).isEmpty());
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

    private static void addPlayerTickWindow(
            GameTestSequence sequence,
            Player player) {
        for (int tick = 0; tick < 20; tick++) {
            sequence.thenIdle(1).thenExecute(() ->
                    NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player)));
        }
    }

    private static ItemStack inventoryStack(Player player, Item item) {
        for (int slot = 0;
                slot < player.getInventory().getContainerSize();
                slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static int inventoryStacksWithHeat(Player player, Item item) {
        int count = 0;
        for (int slot = 0;
                slot < player.getInventory().getContainerSize();
                slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item) && stack.has(ModComponents.HEAT.get())) {
                count++;
            }
        }
        return count;
    }

    private static int inventoryItemCount(Player player, Item item) {
        int count = 0;
        for (int slot = 0;
                slot < player.getInventory().getContainerSize();
                slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static boolean inventoryContainsFluid(
            Player player,
            Item item,
            FluidStack expected) {
        for (int slot = 0;
                slot < player.getInventory().getContainerSize();
                slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.is(item)) {
                continue;
            }
            IFluidHandler handler =
                    stack.getCapability(Capabilities.FluidHandler.ITEM);
            if (handler != null
                    && sameFluidAmount(
                            handler.getFluidInTank(0),
                            expected)) {
                return true;
            }
        }
        return false;
    }

    private static void placeCeramicMold(
            GameTestHelper helper, BlockPos pos, boolean filled) {
        helper.setBlock(pos, ModBlocks.CERAMIC_MOLD.get());
        if (!filled) {
            return;
        }
        CeramicMoldBlockEntity mold = helper.getBlockEntity(pos);
        mold.fill(new CrucibleBlockEntity.CastTransfer(
                MaterialCatalog.require("copper"),
                MaterialPrefixes.INGOT,
                1,
                ItemHeat.AMBIENT_TEMPERATURE));
    }

    private static void assertCeramicMoldDrops(
            GameTestHelper helper, BlockPos pos, boolean expectedContents) {
        Item moldItem = ModItems.INGOT_MOLD.get();
        Item contentsItem = MaterialLookup.item(
                        "copper", MaterialPrefixes.INGOT)
                .orElseThrow();
        int molds = droppedItemCount(helper, pos, moldItem);
        int contents = droppedItemCount(helper, pos, contentsItem);
        int total = droppedItemCount(helper, pos, null);
        int expectedTotal = expectedContents ? 2 : 1;
        helper.assertTrue(
                molds == 1
                        && contents == (expectedContents ? 1 : 0)
                        && total == expectedTotal,
                "Ceramic mold removal drops were not exact at " + pos
                        + ": molds=" + molds
                        + ", contents=" + contents
                        + ", total=" + total);
    }

    private static int droppedItemCount(
            GameTestHelper helper, BlockPos pos, Item expectedItem) {
        BlockPos absolute = helper.absolutePos(pos);
        AABB bounds = new AABB(absolute).inflate(1.25);
        return helper.getLevel()
                .getEntitiesOfClass(ItemEntity.class, bounds)
                .stream()
                .filter(entity -> expectedItem == null
                        || entity.getItem().is(expectedItem))
                .mapToInt(entity -> entity.getItem().getCount())
                .sum();
    }

    private static ItemStack material(String id, com.masson.cruciblecraft.api.material.MaterialPrefix prefix, int count) {
        Item item = MaterialLookup.item(id, prefix).orElseThrow();
        return new ItemStack(item, count);
    }
}
