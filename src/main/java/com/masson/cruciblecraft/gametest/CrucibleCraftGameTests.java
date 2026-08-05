package com.masson.cruciblecraft.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.blockentity.CokeOvenBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CrusherBlockEntity;
import com.masson.cruciblecraft.content.blockentity.DynamoBlockEntity;
import com.masson.cruciblecraft.content.blockentity.SteamEngineBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CableBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ItemPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.SubsurfaceFluidDepositBlockEntity;
import com.masson.cruciblecraft.content.block.CableBlock;
import com.masson.cruciblecraft.content.block.FluidPipeBlock;
import com.masson.cruciblecraft.content.block.ItemPipeBlock;
import com.masson.cruciblecraft.content.block.DynamoBlock;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.block.ProcessingMachineInteractions;
import com.masson.cruciblecraft.content.block.SteamEngineBlock;
import com.masson.cruciblecraft.content.item.CellItem;
import com.masson.cruciblecraft.content.item.ExtruderShapeCatalog;
import com.masson.cruciblecraft.compat.emi.ProcessingEmiRegistrationPlan;
import com.masson.cruciblecraft.content.blockentity.FireboxBlockEntity;
import com.masson.cruciblecraft.content.multiblock.CokeOvenStructure;
import com.masson.cruciblecraft.heat.FuelDefinition;
import com.masson.cruciblecraft.heat.ItemHeat;
import com.masson.cruciblecraft.machine.ToolMaterialRules;
import com.masson.cruciblecraft.material.CellContentGate;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.MaterialComponentPolicies;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.masson.cruciblecraft.logistics.pipe.PipeCatalog;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCover;
import com.masson.cruciblecraft.logistics.pipe.fluid.FluidPipeFailureState;
import com.masson.cruciblecraft.logistics.pipe.fluid
        .FluidPipeFailureState.Failure;
import com.masson.cruciblecraft.machine.processing.MachineTransaction;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.GTRecipeQuery;
import com.masson.cruciblecraft.recipe.gt.ItemInputAction;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModCapabilities;
import com.masson.cruciblecraft.registry.ModComponents;
import com.masson.cruciblecraft.registry.ModFluids;
import com.masson.cruciblecraft.registry.ModItems;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.material.Fluids;
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

    private CrucibleCraftGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void cableChainAppliesExactPerBlockLoss(
            GameTestHelper helper) {
        CableBlock tinCable = ModBlocks.electricalConductorBlock(
                "tin", MaterialPrefixes.CABLE).get();
        BlockPos first = new BlockPos(3, 2, 5);
        BlockPos machinePos = first.east(3);
        helper.setBlock(
                machinePos,
                ModBlocks.ELECTROLYZER.get().defaultBlockState()
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
                ModBlocks.ELECTROLYZER.get().defaultBlockState()
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
        helper.assertTrue(
                dynamo.insert(
                        EnergyType.KINETIC,
                        24L,
                        1L,
                        Direction.WEST,
                        false) == 1L,
                "Dynamo fixture rejected one kinetic packet");
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        dynamo.stored(EnergyType.KINETIC) == 0L
                                && dynamo.stored(EnergyType.ELECTRIC) == 0L
                                && machine.stored(EnergyType.ELECTRIC) == 21L,
                        "Dynamo-to-cable transfer violated source-first "
                                + "conservation or exact three-segment loss"))
                .thenSucceed();
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
                ModBlocks.ELECTROLYZER.get().defaultBlockState()
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
                ModBlocks.ELECTROLYZER.get().defaultBlockState()
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
    public static void steamDynamoPowersElectrolyzerMixedOutput(GameTestHelper helper) {
        BlockPos enginePos = new BlockPos(3, 2, 5);
        BlockPos dynamoPos = enginePos.east();
        BlockPos machinePos = dynamoPos.east();
        helper.setBlock(
                enginePos,
                ModBlocks.BRONZE_STEAM_ENGINE.get().defaultBlockState()
                        .setValue(SteamEngineBlock.FACING, Direction.EAST));
        helper.setBlock(
                dynamoPos,
                ModBlocks.BRONZE_DYNAMO.get().defaultBlockState()
                        .setValue(DynamoBlock.FACING, Direction.EAST));
        helper.setBlock(
                machinePos,
                ModBlocks.ELECTROLYZER.get().defaultBlockState()
                        .setValue(ProcessingMachineBlock.FACING, Direction.EAST));
        SteamEngineBlockEntity engine = helper.getBlockEntity(enginePos);
        DynamoBlockEntity dynamo = helper.getBlockEntity(dynamoPos);
        ConfiguredProcessingMachineBlockEntity electrolyzer =
                helper.getBlockEntity(machinePos);
        RecipeMap.Entry graphiteSource = ModRecipeMaps.ELECTROLYZER.entries()
                .stream()
                .filter(entry -> entry.id().getPath().equals(
                        "t5/electrolyzer/graphite"))
                .findFirst()
                .orElseThrow();
        GTRecipeQuery graphiteQuery = queryFor(graphiteSource.recipe());
        RecipeMap.Match graphite = ModRecipeMaps.ELECTROLYZER.findMatch(
                graphiteQuery).orElseThrow();
        helper.assertTrue(
                graphite.id().getPath().equals("t5/electrolyzer/graphite"),
                "Graphite query resolved the wrong T5 recipe");
        loadRecipeInputs(electrolyzer, graphite.recipe());
        IFluidHandler steamInput = engine.fluids(Direction.NORTH);
        helper.assertTrue(steamInput != null, "Steam engine input capability missing");
        helper.assertTrue(
                steamInput.fill(
                        new FluidStack(ModFluids.STEAM_SOURCE.get(), 16_000),
                        IFluidHandler.FluidAction.EXECUTE) == 16_000,
                "Could not prime the T5 steam-electric vertical");

        helper.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    helper.assertTrue(
                            electrolyzer.progress() > 0,
                            "Dynamo did not advance the electrolyzer");
                    forceLastTick(helper, electrolyzer);
                })
                .thenIdle(5)
                .thenExecute(() -> {
                    helper.assertTrue(
                            dynamo.stored(EnergyType.ELECTRIC) >= 0,
                            "Dynamo electric state became invalid");
                    for (int index = 0;
                            index < graphite.recipe().itemOutputs().size();
                            index++) {
                        int slot = electrolyzer.spec().items().outputs().get(index);
                        ItemStack actual =
                                electrolyzer.inventory().getStackInSlot(slot);
                        ItemStack expected =
                                graphite.recipe().itemOutputs().get(index);
                        helper.assertTrue(
                                ItemStack.isSameItemSameComponents(
                                        actual,
                                        expected)
                                        && actual.getCount() == expected.getCount(),
                                "Electrolyzer did not produce source-projected item output "
                                        + index);
                    }
                    for (int index = 0;
                            index < graphite.recipe().fluidOutputs().size();
                            index++) {
                        int tank = electrolyzer.spec().fluids().outputs()
                                .get(index).index();
                        helper.assertTrue(
                                sameFluidAmount(
                                        electrolyzer.tanks().get(tank).getFluid(),
                                        graphite.recipe().fluidOutputs().get(index)),
                                "Electrolyzer did not produce source-projected fluid "
                                        + index);
                    }
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
                ModBlocks.ELECTROLYZER.get().defaultBlockState()
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
                .thenExecute(() -> forceLastTick(helper, electrolyzer))
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
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void portableTankMakesFluidOutputRecipeRepeatable(
            GameTestHelper helper) {
        BlockPos pos = new BlockPos(5, 2, 5);
        ConfiguredProcessingMachineBlockEntity drying = placeConfigured(
                helper, pos, ModBlocks.DRYING.get(), ModProcessingMachines.DRYING);
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
                drying.insert(
                        EnergyType.ELECTRIC, recipe.eut(), 64L, Direction.SOUTH, false) > 0,
                "Could not power first drying craft");

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
                    helper.assertTrue(
                            drying.insert(
                                    EnergyType.ELECTRIC,
                                    recipe.eut(),
                                    64L,
                                    Direction.SOUTH,
                                    false) > 0,
                            "Could not power second drying craft");
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
        ConfiguredProcessingMachineBlockEntity distillery = placeConfigured(
                helper,
                new BlockPos(5, 2, 5),
                ModBlocks.DISTILLERY.get(),
                ModProcessingMachines.DISTILLERY);
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
                distillery.insert(
                        EnergyType.ELECTRIC,
                        recipe.eut(),
                        64L,
                        Direction.SOUTH,
                        false) > 0,
                "Could not power first source-driven distillery craft");

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
                    helper.assertTrue(
                            distillery.insert(
                                    EnergyType.ELECTRIC,
                                    recipe.eut(),
                                    64L,
                                    Direction.SOUTH,
                                    false) > 0,
                            "Could not power second source-driven distillery craft");
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
                ModBlocks.ELECTROLYZER.get(),
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
                .thenExecute(() -> forceLastTick(helper, electrolyzer))
                .thenIdle(2)
                .thenExecute(() -> {
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
                ModFluids.chemicalFluids().size() == 108
                        && CellContentGate.entries().size() == 109,
                "T10 chemical-fluid or cell allowlist registry is incomplete");
        helper.assertTrue(
                MaterialPrefixCatalog.values().size() == 56
                        && MaterialCatalog.startupValues().size()
                                + MaterialPrefixCatalog.values().size() == 1_829
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
                ModBlocks.CENTRIFUGE.get(),
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
                .thenIdle(4)
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
        for (BlockPos offset : CokeOvenStructure.firebrickOffsets(facing)) {
            helper.setBlock(controllerPos.offset(offset), ModBlocks.FIREBRICK.get());
        }
        BlockPos fireboxPos = CokeOvenStructure.heatSource(controllerPos, facing);
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

    @GameTest(template = TEMPLATE, timeoutTicks = 260)
    public static void smelterAboveFireboxUsesRealHeat(GameTestHelper helper) {
        BlockPos fireboxPos = new BlockPos(4, 1, 4);
        BlockPos smelterPos = fireboxPos.above();
        helper.setBlock(fireboxPos, ModBlocks.FIREBOX.get());
        ConfiguredProcessingMachineBlockEntity smelter =
                placeConfigured(helper, smelterPos, ModBlocks.SMELTER.get(), ModProcessingMachines.SMELTER);
        FireboxBlockEntity firebox = helper.getBlockEntity(fireboxPos);
        helper.assertTrue(firebox.addFuel(FuelDefinition.CHARCOAL), "Could not fuel real firebox");
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
                firebox.addFuel(FuelDefinition.CHARCOAL),
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
                                == snapshot.initialAmountMb(),
                "Placed crude-oil deposit data is incomplete or out of range");
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
        helper.assertTrue(chainFirebox.addFuel(FuelDefinition.CHARCOAL),
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
                helper.assertTrue(
                        CrucibleCraftGameTests.class.getClassLoader()
                                .getResource(directPath) != null
                                || CrucibleCraftGameTests.class.getClassLoader()
                                        .getResource(sourcePath) != null,
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
                emiPlan.machines().size() == 23
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
                .benchmarkT4LookupsForVerification();
        helper.assertTrue(
                metrics.t3ComponentRecipes() == 8398
                        && metrics.t4ToolRecipes() == 3452
                        && metrics.t5ChemicalRecipes() == 152
                        && metrics.t7AuthoredMaterialRules() == 220
                        && metrics.t8PipeMaterialRules() == 257
                        && metrics.t10KnownFormMaterialRules() == 1_288
                        && metrics.liveT3MapRecipes() == 11851
                        && metrics.allPublishedRecipes() == 18_871
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
                        && lookup.samples() == 1024
                        && lookup.averageNanos()
                                <= ModProcessingMachines
                                        .RECIPE_LOOKUP_AVERAGE_BUDGET_NS
                        && lookup.averageCandidates()
                                <= ModProcessingMachines
                                        .RECIPE_LOOKUP_AVERAGE_CANDIDATE_BUDGET,
                "Recipe publication performance budget drifted: "
                        + metrics + ", " + lookup);
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
        }
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
        helper.assertTrue(
                machine.duration() == match.recipe().duration(),
                label + " selected duration " + machine.duration()
                        + " instead of live recipe duration " + match.recipe().duration());
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
        helper.assertTrue(machine.spec() == spec, "Placed block resolved wrong machine spec");
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
        helper.assertTrue(energy != null, "Back KU capability missing at " + label);
        long accepted = energy.insert(EnergyType.KINETIC, 256L, 16L, Direction.SOUTH, false);
        helper.assertTrue(accepted > 0L || energy.stored(EnergyType.KINETIC) > 0L,
                "KU capability accepted no energy at " + label);
    }

    private static void fillKu(
            GameTestHelper helper, ConfiguredProcessingMachineBlockEntity machine) {
        long accepted = machine.insert(
                EnergyType.KINETIC, 256L, 16L, Direction.SOUTH, false);
        helper.assertTrue(accepted > 0L || machine.stored(EnergyType.KINETIC) > 0L,
                "Placed machine accepted no KU: " + machine.spec().id());
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

    private static ItemStack material(String id, com.masson.cruciblecraft.api.material.MaterialPrefix prefix, int count) {
        Item item = MaterialLookup.item(id, prefix).orElseThrow();
        return new ItemStack(item, count);
    }
}
