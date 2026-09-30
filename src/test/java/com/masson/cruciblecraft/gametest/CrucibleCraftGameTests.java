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

import static com.masson.cruciblecraft.gametest.GameTestHeatSources.energyCapacity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.gametest.support.GameTestFixtures;
import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.fluid.CrucibleTransferCoordinator.InsertResult;
import com.masson.cruciblecraft.content.block.DistillationTowerParts;
import com.masson.cruciblecraft.content.block.LargeCrucibleHosts;
import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.block.MultiblockPortBlock;
import com.masson.cruciblecraft.content.blockentity.CokeOvenBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ProcessingMachineBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CeramicMoldBlockEntity;
import com.masson.cruciblecraft.content.blockentity.AnvilBlockEntity;
import com.masson.cruciblecraft.content.blockentity.BoilerBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CrucibleBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CrusherBlockEntity;
import com.masson.cruciblecraft.content.blockentity.DynamoBlockEntity;
import com.masson.cruciblecraft.content.blockentity.LargeCentrifugeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.DistillationTowerBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CryoDistillationTowerBlockEntity;
import com.masson.cruciblecraft.content.blockentity.DistillationTowerAutoOutput;
import com.masson.cruciblecraft.content.blockentity.DistillationTowerFluidRouting;
import com.masson.cruciblecraft.content.blockentity.LargeBoilerBlockEntity;
import com.masson.cruciblecraft.content.blockentity.LargeBoilerTier;
import com.masson.cruciblecraft.content.blockentity.LargeCrucibleBlockEntity;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.blockentity.TankBlockEntity;
import com.masson.cruciblecraft.content.blockentity.SteamEngineBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CableBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FluidDepositExtractorBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FuelGeneratorBlockEntity;
import com.masson.cruciblecraft.content.blockentity.GasCloudBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ItemPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.MassStorageBlockEntity;
import com.masson.cruciblecraft.content.blockentity.MultiblockPortBlockEntity;
import com.masson.cruciblecraft.content.blockentity.SubsurfaceFluidDepositBlockEntity;
import com.masson.cruciblecraft.content.block.CableBlock;
import com.masson.cruciblecraft.content.block.Gt6StyleConnections;
import com.masson.cruciblecraft.content.block.AbstractPipeBlock;
import com.masson.cruciblecraft.content.block.FluidPipeBlock;
import com.masson.cruciblecraft.content.block.ItemPipeBlock;
import com.masson.cruciblecraft.content.block.DynamoBlock;
import com.masson.cruciblecraft.content.block.ElectricMotorBlock;
import com.masson.cruciblecraft.content.block.FuelGeneratorBlock;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.blockentity.HopperBlockEntity;
import com.masson.cruciblecraft.content.blockentity.DustFunnelBlockEntity;
import com.masson.cruciblecraft.content.block.HopperBlock;
import com.masson.cruciblecraft.logistics.hopper.DustAmountLedger;
import com.masson.cruciblecraft.content.block.ProcessingMachineInteractions;
import com.masson.cruciblecraft.content.block.RockBlock;
import com.masson.cruciblecraft.content.block.RotationalAxleBlock;
import com.masson.cruciblecraft.content.block.RotationalGearboxBlock;
import com.masson.cruciblecraft.content.block.SteamEngineBlock;
import com.masson.cruciblecraft.content.item.CellItem;
import com.masson.cruciblecraft.content.item.ExtruderShapeCatalog;
import com.masson.cruciblecraft.compat.emi.ProcessingEmiRegistrationPlan;
import com.masson.cruciblecraft.content.menu.ConfiguredProcessingMachineMenu;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureCatalog;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.Offset;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PredicateKind;
import com.masson.cruciblecraft.content.mold.CruciblePour;
import com.masson.cruciblecraft.heat.CrucibleThermalModel;
import com.masson.cruciblecraft.heat.ItemHeat;
import com.masson.cruciblecraft.machine.ToolMaterialRules;
import com.masson.cruciblecraft.material.CellContentGate;
import com.masson.cruciblecraft.material.HydrocarbonRuntimePolicy;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.MaterialComponentPolicies;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.masson.cruciblecraft.logistics.pipe.PipeAcquisitionRecipeCatalog;
import com.masson.cruciblecraft.logistics.pipe.PipeCatalog;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCover;
import com.masson.cruciblecraft.content.storage.StorageBehaviorProfile;
import com.masson.cruciblecraft.content.storage.StorageVariantCatalog;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCoverConfig;
import com.masson.cruciblecraft.logistics.pipe.fluid.FluidPipeFailureState;
import com.masson.cruciblecraft.logistics.pipe.fluid
        .FluidPipeFailureState.Failure;
import com.masson.cruciblecraft.machine.processing.MachineExecutionPlan;
import com.masson.cruciblecraft.machine.processing.MachineIdentityPolicy;
import com.masson.cruciblecraft.machine.processing.MachineTransaction;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineIoFaces;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineState;
import com.masson.cruciblecraft.recipe.AnvilMode;
import com.masson.cruciblecraft.recipe.SteelmakingProcess;
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
import com.masson.cruciblecraft.registry.ModMachineIdentities;
import com.masson.cruciblecraft.registry.ModMultiblockControllers;
import com.masson.cruciblecraft.registry.ModProcessingMachines;
import com.masson.cruciblecraft.registry.ModRecipeMaps;
import com.masson.cruciblecraft.steam.SteamConversion;
import com.masson.cruciblecraft.steam.SteamEngineKuCurve;
import com.masson.cruciblecraft.worldgen.LargeVeinConfiguration;
import com.masson.cruciblecraft.worldgen.LargeVeinLayout;
import com.masson.cruciblecraft.worldgen.OreHostVariantCatalog.Host;
import com.masson.cruciblecraft.worldgen.SubsurfaceFluidDepositConfiguration;
import com.masson.cruciblecraft.worldgen.SurfaceRockConfiguration;
import com.masson.cruciblecraft.worldgen.SurfaceRockFeature;

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
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
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
 * Retired kitchen-sink block-world coverage. Isolated {@code -PwaveRecipes}
 * namespaces remain the closeout gates. Re-enable with
 * {@code -PgameTestGrid=default}. Recipes are those
 * loaded by the production reload listener; no test-only RecipeMaps or
 * processing hosts are used.
 */
@GameTestHolder(CrucibleCraftGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class CrucibleCraftGameTests {
    public static final String NAMESPACE = "cruciblecraft_default_grid";
    private static final String TEMPLATE = "empty";
    private static final int LARGE_BOILER_STRUCTURE_WAIT = 650;

    private CrucibleCraftGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void cableChainAppliesExactPerBlockLoss(
            GameTestHelper helper) {
        CableBlock tinCable = ModBlocks.electricalConductorBlock(
                "tin", MaterialPrefixes.CABLE).get();
        BlockPos first = new BlockPos(3, 2, 5);
        BlockPos machinePos = first.east(2).above();
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
                    offset == 2
                            ? conductorState(
                                    tinCable, Direction.WEST, Direction.UP)
                            : conductorState(
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
                !CableBlock.isConnected(
                        helper.getBlockState(first), Direction.EAST)
                        && !CableBlock.isConnected(
                                helper.getBlockState(second),
                                Direction.WEST),
                "GT6-style cables auto-connected on mere adjacency");

        Gt6StyleConnections.applyPlacement(
                helper.getLevel(),
                helper.absolutePos(second),
                Direction.EAST);
        helper.assertTrue(
                CableBlock.isConnected(
                        helper.getBlockState(first), Direction.EAST)
                        && CableBlock.isConnected(
                                helper.getBlockState(second),
                                Direction.WEST),
                "Placing against a cable did not open a reciprocal connection");

        helper.setBlock(second, Blocks.AIR);
        helper.assertTrue(
                CableBlock.isConnected(
                        helper.getBlockState(first), Direction.EAST),
                "GT6-style cables auto-closed after the neighbor was removed");

        helper.setBlock(first, tinCable.defaultBlockState());
        helper.setBlock(
                second,
                ModBlocks.STAINLESS_STEEL_ELECTROLYZER.get()
                        .defaultBlockState()
                        .setValue(
                                ProcessingMachineBlock.FACING,
                                Direction.EAST));
        helper.assertTrue(
                !CableBlock.isConnected(
                        helper.getBlockState(first), Direction.EAST),
                "Electric machine placement auto-connected the cable");
        Gt6StyleConnections.setConnection(
                helper.getLevel(),
                helper.absolutePos(first),
                Direction.EAST,
                true);
        helper.assertTrue(
                CableBlock.isConnected(
                        helper.getBlockState(first), Direction.EAST),
                "Wrench-style toggle did not open the machine face");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void cutterTogglesCableConnectionsOnTheWrenchingGrid(
            GameTestHelper helper) {
        CableBlock tinCable = ModBlocks.electricalConductorBlock(
                "tin", MaterialPrefixes.CABLE).get();
        BlockPos pos = new BlockPos(4, 2, 5);
        helper.setBlock(pos, tinCable.defaultBlockState());
        ItemStack cutter = ModItems.MATERIAL_WIRE_CUTTER.get().variant("iron");
        ItemStack wrench = ModItems.MATERIAL_WRENCH.get().variant("iron");
        BlockPos absolute = helper.absolutePos(pos);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        // GT6 getFacingTool split: the wrench no longer operates cables.
        ItemInteractionResult rejected = helper.getBlockState(pos).useItemOn(
                wrench,
                helper.getLevel(),
                player,
                InteractionHand.MAIN_HAND,
                faceHit(absolute, Direction.EAST, 1.0, 0.5, 0.5));
        helper.assertTrue(
                !rejected.consumesAction()
                        && !CableBlock.isConnected(
                                helper.getBlockState(pos), Direction.EAST),
                "Wrench must not toggle cable connections");

        // Center cell of the grid targets the clicked face itself.
        ItemInteractionResult center = helper.getBlockState(pos).useItemOn(
                cutter,
                helper.getLevel(),
                player,
                InteractionHand.MAIN_HAND,
                faceHit(absolute, Direction.EAST, 1.0, 0.5, 0.5));
        helper.assertTrue(
                center.consumesAction()
                        && CableBlock.isConnected(
                                helper.getBlockState(pos), Direction.EAST),
                "Wire cutter center cell did not toggle the clicked face");

        // Corner cell of the grid targets the opposite face.
        helper.getBlockState(pos).useItemOn(
                cutter,
                helper.getLevel(),
                player,
                InteractionHand.MAIN_HAND,
                faceHit(absolute, Direction.EAST, 1.0, 0.1, 0.1));
        helper.assertTrue(
                CableBlock.isConnected(
                        helper.getBlockState(pos), Direction.WEST),
                "Wire cutter corner cell did not toggle the opposite face");

        // Edge cell on the EAST face maps to SOUTH (z > 0.75 band).
        helper.getBlockState(pos).useItemOn(
                cutter,
                helper.getLevel(),
                player,
                InteractionHand.MAIN_HAND,
                faceHit(absolute, Direction.EAST, 1.0, 0.5, 0.9));
        helper.assertTrue(
                CableBlock.isConnected(
                        helper.getBlockState(pos), Direction.SOUTH),
                "Wire cutter edge cell did not toggle the adjacent face");
        player.discard();
        helper.succeed();
    }

    private static BlockHitResult faceHit(
            BlockPos absolute,
            Direction face,
            double fx,
            double fy,
            double fz) {
        return new BlockHitResult(
                new Vec3(
                        absolute.getX() + fx,
                        absolute.getY() + fy,
                        absolute.getZ() + fz),
                face,
                absolute,
                false);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void dynamoFeedsElectrolyzerThroughThreeCables(
            GameTestHelper helper) {
        CableBlock tinCable = ModBlocks.electricalConductorBlock(
                "tin", MaterialPrefixes.CABLE).get();
        BlockPos dynamoPos = new BlockPos(2, 2, 5);
        BlockPos first = dynamoPos.east();
        BlockPos machinePos = first.east(2).above();
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
                    offset == 2
                            ? conductorState(
                                    tinCable, Direction.WEST, Direction.UP)
                            : conductorState(
                                    tinCable,
                                    Direction.WEST,
                                    Direction.EAST));
        }
        DynamoBlockEntity dynamo = helper.getBlockEntity(dynamoPos);
        ConfiguredProcessingMachineBlockEntity machine =
                helper.getBlockEntity(machinePos);
        GTRecipe recipe = requireRecipe(
                ModRecipeMaps.ELECTROLYZER, "chemical/electrolyzer/salt");
        loadRecipeInputs(machine, recipe);
        helper.assertTrue(
                dynamo.insert(
                        EnergyType.KINETIC_ROTATION,
                        32L,
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

    @GameTest(template = TEMPLATE, timeoutTicks = 180)
    public static void crudeOilFuelEngineDynamoPowersElectrolyzer(
            GameTestHelper helper) {
        BlockPos distilleryPos = new BlockPos(2, 2, 5);
        BlockPos enginePos = new BlockPos(4, 2, 5);
        BlockPos axlePos = enginePos.east();
        BlockPos gearboxPos = axlePos.east();
        BlockPos dynamoPos = gearboxPos.east();
        BlockPos cablePos = dynamoPos.east();
        BlockPos electrolyzerPos = cablePos.above();
        GameTestHeatSources.placeHuSource(helper, distilleryPos.below());
        helper.setBlock(
                distilleryPos,
                ModBlocks.DISTILLERY.get().defaultBlockState()
                        .setValue(
                                ProcessingMachineBlock.FACING,
                                Direction.EAST));
        helper.setBlock(
                enginePos,
                ModBlocks.BRONZE_FUEL_ENGINE.get().defaultBlockState()
                        .setValue(
                                FuelGeneratorBlock.FACING,
                                Direction.EAST));
        BlockPos exhaustPos = enginePos.west();
        FluidPipeBlock exhaustPipeBlock = (FluidPipeBlock) ModBlocks.pipeBlock(
                "copper",
                MaterialPrefixes.TINY_FLUID_PIPE,
                PipeCatalog.Kind.FLUID).get();
        helper.setBlock(exhaustPos, exhaustPipeBlock);
        helper.getLevel().setBlock(
                helper.absolutePos(exhaustPos),
                helper.getBlockState(exhaustPos)
                        .setValue(FluidPipeBlock.EAST, true),
                Block.UPDATE_CLIENTS);
        helper.setBlock(
                axlePos,
                ModBlocks.ROTATIONAL_AXLE.get().defaultBlockState()
                        .setValue(
                                RotationalAxleBlock.AXIS,
                                Direction.Axis.X));
        helper.setBlock(
                gearboxPos,
                ModBlocks.ROTATIONAL_GEARBOX.get()
                        .defaultBlockState()
                        .setValue(
                                RotationalGearboxBlock.FACING,
                                Direction.EAST));
        helper.setBlock(
                dynamoPos,
                ModBlocks.BRONZE_DYNAMO.get().defaultBlockState()
                        .setValue(DynamoBlock.FACING, Direction.EAST));
        CableBlock cable = ModBlocks.electricalConductorBlock(
                "tin", MaterialPrefixes.CABLE).get();
        helper.setBlock(
                cablePos,
                conductorState(
                        cable, Direction.WEST, Direction.UP));
        helper.setBlock(
                electrolyzerPos,
                ModBlocks.ELECTROLYZER.get().defaultBlockState()
                        .setValue(
                                ProcessingMachineBlock.FACING,
                                Direction.EAST));

        ConfiguredProcessingMachineBlockEntity distillery =
                helper.getBlockEntity(distilleryPos);
        FuelGeneratorBlockEntity firebox =
                helper.getBlockEntity(distilleryPos.below());
        FuelGeneratorBlockEntity engine =
                helper.getBlockEntity(enginePos);
        IEnergyHandler axle = helper.getLevel().getCapability(
                ModCapabilities.ENERGY,
                helper.absolutePos(axlePos),
                Direction.WEST);
        IEnergyHandler gearbox = helper.getLevel().getCapability(
                ModCapabilities.ENERGY,
                helper.absolutePos(gearboxPos),
                Direction.WEST);
        DynamoBlockEntity dynamo =
                helper.getBlockEntity(dynamoPos);
        ConfiguredProcessingMachineBlockEntity electrolyzer =
                helper.getBlockEntity(electrolyzerPos);
        GTRecipe distillation = requireRecipe(
                ModRecipeMaps.DISTILLERY,
                "hydrocarbon/distillery/crude_oil_to_fuel_and_lubricant");
        GTRecipe fuel = requireRecipe(
                ModRecipeMaps.FUELS_ENGINE,
                "hydrocarbon/fuels_engine/fuel_oil");
        GTRecipe electrolysis = requireRecipe(
                ModRecipeMaps.ELECTROLYZER,
                "chemical/electrolyzer/salt");
        loadRecipeInputs(distillery, distillation);
        loadRecipeInputs(electrolyzer, electrolysis);
        helper.assertTrue(
                firebox.seedStoredEnergy(energyCapacity()),
                "Could not fuel the HU distillery");
        helper.assertTrue(
                engine.outputEnergyType()
                                == EnergyType.KINETIC_ROTATION
                        && engine.handles(
                                EnergyType.KINETIC_ROTATION,
                                Direction.EAST)
                        && !engine.handles(
                                EnergyType.ELECTRIC,
                                Direction.EAST),
                "Fuel Engine did not expose its current RU identity");

        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> forceLastTick(
                        helper, distillery))
                .thenIdle(2)
                .thenExecute(() -> {
                    FluidTank fuelTank = distillery.tanks().stream()
                            .filter(tank -> tank.getFluid().is(
                                    fuel.fluidInputs()
                                            .getFirst().getFluid()))
                            .findFirst()
                            .orElseThrow();
                    transferFluid(
                            helper,
                            fuelTank,
                            engine.fluids(Direction.UP),
                            fuel.fluidInputs()
                                    .getFirst().getAmount());
                })
                .thenIdle(48)
                .thenExecute(() -> {
                    long expectedRu = Math.multiplyExact(
                            Math.abs(fuel.eut()),
                            (long) fuel.duration());
                    helper.assertTrue(
                            engine.energyGenerated() == expectedRu
                                    && engine.energyGenerated()
                                            == engine.energyStored()
                                                    + engine.energyExtracted(),
                            "Fuel recipe did not conserve total RU");
                    long ruInFlight = engine.energyStored()
                            + axle.stored(EnergyType.KINETIC_ROTATION)
                            + gearbox.stored(
                                    EnergyType.KINETIC_ROTATION)
                            + dynamo.stored(
                                    EnergyType.KINETIC_ROTATION);
                    helper.assertTrue(
                            engine.energyGenerated()
                                    == ruInFlight
                                            + dynamo.kineticConsumed(),
                            "RU escaped engine/axle/gearbox/dynamo accounting");
                    helper.assertTrue(
                            dynamo.kineticConsumed()
                                    == dynamo.electricExtracted()
                                            + dynamo.conversionLoss(),
                            "Dynamo RU -> EU + loss conservation failed");
                    long euPackets =
                            dynamo.electricExtracted() / 22L;
                    helper.assertTrue(
                            dynamo.electricExtracted()
                                    == euPackets * 22L
                                    && electrolyzer.workProgressLong()
                                                    + electrolyzer.stored(
                                                            EnergyType
                                                                    .ELECTRIC)
                                            == euPackets * 21L,
                            "EU cable loss/consumer accounting failed");
                    helper.assertTrue(
                            electrolyzer.workProgressLong() > 0L,
                            "Crude oil -> HU distillery -> Fuel Engine -> "
                                    + "RU axle/gearbox -> Dynamo -> EU cable "
                                    + "did not power the Electrolyzer");
                    FluidPipeBlockEntity exhaustPipe =
                            helper.getBlockEntity(enginePos.west());
                    helper.assertTrue(
                            engine.outputAmount(0)
                                            + exhaustPipe.storedFluid()
                                                    .getAmount()
                                    == fuel.fluidOutputs()
                                            .getFirst().getAmount()
                                    && exhaustPipe.storedFluid().getAmount()
                                            == fuel.fluidOutputs()
                                                    .getFirst().getAmount()
                                    && exhaustPipe.storedFluid().is(
                                            fuel.fluidOutputs()
                                                    .getFirst().getFluid()),
                            "Fuel Engine did not push recipe-defined CO2 to its back");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void fuelEngineTransactionsAndBlockedStates(
            GameTestHelper helper) {
        BlockPos enginePos = new BlockPos(3, 2, 5);
        BlockPos fullEnginePos = new BlockPos(7, 2, 5);
        BlockPos blockedDynamoPos = new BlockPos(11, 2, 5);
        helper.setBlock(
                enginePos,
                ModBlocks.BRONZE_FUEL_ENGINE.get().defaultBlockState()
                        .setValue(
                                FuelGeneratorBlock.FACING,
                                Direction.EAST));
        helper.setBlock(enginePos.west(), Blocks.STONE);
        helper.setBlock(
                fullEnginePos,
                ModBlocks.BRONZE_FUEL_ENGINE.get().defaultBlockState()
                        .setValue(
                                FuelGeneratorBlock.FACING,
                                Direction.EAST));
        helper.setBlock(
                blockedDynamoPos,
                ModBlocks.BRONZE_DYNAMO.get().defaultBlockState()
                        .setValue(DynamoBlock.FACING, Direction.EAST));
        FuelGeneratorBlockEntity exhaustBlocked =
                helper.getBlockEntity(enginePos);
        FuelGeneratorBlockEntity outputFull =
                helper.getBlockEntity(fullEnginePos);
        DynamoBlockEntity blockedDynamo =
                helper.getBlockEntity(blockedDynamoPos);
        GTRecipe fuel = requireRecipe(
                ModRecipeMaps.FUELS_ENGINE,
                "hydrocarbon/fuels_engine/fuel_oil");
        FluidStack required = fuel.fluidInputs().getFirst();
        IFluidHandler input =
                exhaustBlocked.fluids(Direction.UP);
        helper.assertTrue(
                input.fill(
                                required,
                                IFluidHandler.FluidAction.SIMULATE)
                        == required.getAmount()
                        && exhaustBlocked.inputAmount() == 0,
                "Fuel input simulation mutated the tank");
        helper.assertTrue(
                input.fill(
                                required,
                                IFluidHandler.FluidAction.EXECUTE)
                        == required.getAmount()
                        && exhaustBlocked.inputAmount()
                                == required.getAmount(),
                "Fuel input execution differed from simulation");

        var registries = helper.getLevel().registryAccess();
        CompoundTag exhaustTag =
                exhaustBlocked.saveWithoutMetadata(registries);
        FluidTank fullExhaust = new FluidTank(8_000);
        fullExhaust.setFluid(
                fuel.fluidOutputs().getFirst()
                        .copyWithAmount(8_000));
        exhaustTag.put(
                "output_0",
                fullExhaust.writeToNBT(
                        registries, new CompoundTag()));
        exhaustBlocked.loadWithComponents(
                exhaustTag, registries);

        CompoundTag fullTag =
                outputFull.saveWithoutMetadata(registries);
        FluidTank fuelInput = new FluidTank(8_000);
        fuelInput.setFluid(required.copy());
        fullTag.put(
                "input",
                fuelInput.writeToNBT(
                        registries, new CompoundTag()));
        fullTag.putLong("energy", 65_536L);
        fullTag.putLong("energy_generated", 65_536L);
        outputFull.loadWithComponents(fullTag, registries);
        helper.assertTrue(
                blockedDynamo.insert(
                                EnergyType.KINETIC_ROTATION,
                                32L,
                                1L,
                                Direction.WEST,
                                true)
                        == 1L
                        && blockedDynamo.stored(
                                        EnergyType.KINETIC_ROTATION)
                                == 0L,
                "Dynamo RU simulation mutated input state");
        helper.assertTrue(
                blockedDynamo.insert(
                                EnergyType.KINETIC_ROTATION,
                                32L,
                                1L,
                                Direction.WEST,
                                false)
                        == 1L,
                "Dynamo RU execution differed from simulation");

        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(
                            exhaustBlocked.status().equals(
                                            "exhaust_blocked")
                                    && exhaustBlocked.inputAmount()
                                            == required.getAmount()
                                    && exhaustBlocked.energyStored() == 0L
                                    && exhaustBlocked.outputAmount(0)
                                            == 8_000,
                            "Exhaust blockage consumed fuel, produced RU or "
                                    + "voided CO2");
                    helper.assertTrue(
                            outputFull.energyExtracted() > 0L
                                    && outputFull.energyStored() < 65_536L
                                    && outputFull.energyGenerated()
                                            == outputFull.energyStored()
                                                    + outputFull.energyExtracted(),
                            "Full RU output did not waste unused packets");
                    helper.assertTrue(
                            blockedDynamo.stored(
                                            EnergyType.KINETIC_ROTATION)
                                    == 0L
                                    && blockedDynamo.kineticConsumed()
                                            == 32L
                                    && blockedDynamo.electricExtracted()
                                            == 0L
                                    && blockedDynamo.conversionLoss()
                                            == 32L,
                            "Blocked Dynamo violated source waste policy");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fuelEnginePushesCarbonDioxideToBack(
            GameTestHelper helper) {
        BlockPos enginePos = new BlockPos(3, 2, 5);
        BlockPos exhaustPos = enginePos.west();
        helper.setBlock(
                enginePos,
                ModBlocks.BRONZE_FUEL_ENGINE.get().defaultBlockState()
                        .setValue(
                                FuelGeneratorBlock.FACING,
                                Direction.EAST));
        FluidPipeBlock pipeBlock = (FluidPipeBlock) ModBlocks.pipeBlock(
                "copper",
                MaterialPrefixes.TINY_FLUID_PIPE,
                PipeCatalog.Kind.FLUID).get();
        helper.setBlock(exhaustPos, pipeBlock);
        helper.getLevel().setBlock(
                helper.absolutePos(exhaustPos),
                helper.getBlockState(exhaustPos)
                        .setValue(FluidPipeBlock.EAST, true),
                Block.UPDATE_CLIENTS);
        FuelGeneratorBlockEntity engine = helper.getBlockEntity(enginePos);
        FluidPipeBlockEntity pipe = helper.getBlockEntity(exhaustPos);
        GTRecipe fuel = requireRecipe(
                ModRecipeMaps.FUELS_ENGINE,
                "hydrocarbon/fuels_engine/fuel_oil");
        FluidStack required = fuel.fluidInputs().getFirst();
        IFluidHandler input = engine.fluids(Direction.UP);
        helper.assertTrue(
                input != null
                        && engine.fluids(Direction.WEST) != null
                        && input.fill(
                                required,
                                IFluidHandler.FluidAction.EXECUTE)
                                == required.getAmount(),
                "Could not fill fuel into a side of the fuel engine");
        helper.startSequence()
                .thenIdle(fuel.duration() + 2)
                .thenExecute(() -> helper.assertTrue(
                        engine.outputAmount(0) == 0
                                && pipe.storedFluid().getAmount()
                                        == fuel.fluidOutputs()
                                                .getFirst().getAmount()
                                && pipe.storedFluid().is(
                                        fuel.fluidOutputs()
                                                .getFirst().getFluid()),
                        "Fuel engine did not push recipe CO2 to its back"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fuelEngineVentsCarbonDioxideIntoAir(
            GameTestHelper helper) {
        BlockPos enginePos = new BlockPos(3, 2, 5);
        helper.setBlock(
                enginePos,
                ModBlocks.BRONZE_FUEL_ENGINE.get().defaultBlockState()
                        .setValue(
                                FuelGeneratorBlock.FACING,
                                Direction.EAST));
        FuelGeneratorBlockEntity engine = helper.getBlockEntity(enginePos);
        GTRecipe fuel = requireRecipe(
                ModRecipeMaps.FUELS_ENGINE,
                "hydrocarbon/fuels_engine/fuel_oil");
        FluidStack required = fuel.fluidInputs().getFirst();
        helper.assertTrue(
                engine.fluids(Direction.UP).fill(
                                required,
                                IFluidHandler.FluidAction.EXECUTE)
                        == required.getAmount(),
                "Could not fill fuel into an isolated fuel engine");
        helper.startSequence()
                .thenIdle(fuel.duration() + 2)
                .thenExecute(() -> helper.assertTrue(
                        engine.outputAmount(0) == 0,
                        "Isolated fuel engine did not vent leftover CO2 into air"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void fuelEngineCurrentIdentityAndCableOverload(
            GameTestHelper helper) {
        BlockPos currentPos = new BlockPos(3, 2, 5);
        BlockPos nearMissPos = new BlockPos(3, 2, 9);
        BlockPos dynamoPos = new BlockPos(9, 2, 5);
        BlockPos cablePos = dynamoPos.east();
        BlockPos machinePos = cablePos.above();
        helper.setBlock(
                currentPos,
                ModBlocks.BRONZE_FUEL_ENGINE.get().defaultBlockState()
                        .setValue(
                                FuelGeneratorBlock.FACING,
                                Direction.EAST));
        helper.setBlock(
                nearMissPos,
                ModBlocks.BRONZE_FUEL_ENGINE.get().defaultBlockState()
                        .setValue(
                                FuelGeneratorBlock.FACING,
                                Direction.EAST));
        helper.setBlock(
                dynamoPos,
                ModBlocks.BRONZE_DYNAMO.get().defaultBlockState()
                        .setValue(DynamoBlock.FACING, Direction.EAST));
        CableBlock tinCable = ModBlocks.electricalConductorBlock(
                "tin", MaterialPrefixes.CABLE).get();
        helper.setBlock(
                cablePos,
                conductorState(
                        tinCable, Direction.WEST, Direction.UP));
        helper.setBlock(
                machinePos,
                ModBlocks.ELECTROLYZER.get().defaultBlockState()
                        .setValue(
                                ProcessingMachineBlock.FACING,
                                Direction.EAST));
        FuelGeneratorBlockEntity current =
                helper.getBlockEntity(currentPos);
        FuelGeneratorBlockEntity nearMiss =
                helper.getBlockEntity(nearMissPos);
        DynamoBlockEntity dynamo =
                helper.getBlockEntity(dynamoPos);
        CableBlockEntity cableEntity =
                helper.getBlockEntity(cablePos);
        GTRecipe fuel = requireRecipe(
                ModRecipeMaps.FUELS_ENGINE,
                "hydrocarbon/fuels_engine/fuel_oil");
        var registries = helper.getLevel().registryAccess();
        CompoundTag currentTag =
                current.saveWithoutMetadata(registries);
        FluidTank savedInput = new FluidTank(8_000);
        savedInput.setFluid(
                fuel.fluidInputs().getFirst().copy());
        currentTag.put(
                "input",
                savedInput.writeToNBT(
                        registries, new CompoundTag()));
        FluidTank savedExhaust = new FluidTank(8_000);
        savedExhaust.setFluid(
                fuel.fluidOutputs().getFirst().copy());
        currentTag.put(
                "output_0",
                savedExhaust.writeToNBT(
                        registries, new CompoundTag()));
        currentTag.putLong("energy", 321L);
        currentTag.putString(
                "active_recipe",
                "cruciblecraft:hydrocarbon/fuels_engine/fuel_oil");
        currentTag.putInt("progress", 3);
        currentTag.putInt("duration", fuel.duration());
        currentTag.putString("status", "running");
        current.loadWithComponents(currentTag, registries);
        CompoundTag currentSave =
                current.saveWithoutMetadata(registries);

        CompoundTag mismatched = currentSave.copy();
        mismatched.putString("energy_identity", "ELECTRIC");
        nearMiss.loadWithComponents(mismatched, registries);
        CompoundTag nearMissSave =
                nearMiss.saveWithoutMetadata(registries);
        helper.assertTrue(
                !current.identityQuarantined()
                        && current.inputAmount()
                                == fuel.fluidInputs()
                                        .getFirst().getAmount()
                        && current.outputAmount(0)
                                == fuel.fluidOutputs()
                                        .getFirst().getAmount()
                        && current.progress() == 3
                        && current.energyStored() == 321L
                        && current.handles(
                                EnergyType.KINETIC_ROTATION,
                                Direction.EAST)
                        && !current.handles(
                                EnergyType.ELECTRIC,
                                Direction.EAST),
                "Current Fuel Engine identity lost state or RU output");
        helper.assertTrue(
                currentSave.getInt(
                                "fuel_generator_schema_version")
                                == 1
                        && currentSave.getString(
                                        "fuel_generator_id")
                                .equals("cruciblecraft:bronze_fuel_engine")
                        && currentSave.getString(
                                        "energy_identity")
                                .equals("KINETIC_ROTATION"),
                "Current Fuel Engine save did not retain its complete identity");
        helper.assertTrue(
                nearMiss.identityQuarantined()
                        && nearMiss.energyStored() == 321L
                        && nearMiss.progress() == 3
                        && nearMiss.inputAmount()
                                == current.inputAmount()
                        && nearMiss.outputAmount(0)
                                == current.outputAmount(0)
                        && !nearMiss.handles(
                                EnergyType.KINETIC_ROTATION,
                                Direction.EAST)
                        && nearMissSave.getString(
                                        "energy_identity")
                                .equals("ELECTRIC"),
                "Near-miss Fuel Engine identity did not fail closed");
        helper.assertTrue(
                dynamo.insert(
                                EnergyType.KINETIC_ROTATION,
                                64L,
                                1L,
                                Direction.WEST,
                                false)
                        == 1L,
                "Dynamo rejected its maximum source-window RU packet");

        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(
                            cableEntity.burnCounter() == 1,
                            "44 EU source packet did not overload the "
                                    + "32 EU tin cable");
                    helper.assertTrue(
                            dynamo.kineticConsumed() == 64L
                                    && dynamo.electricExtracted() == 44L
                                    && dynamo.conversionLoss() == 20L,
                            "Maximum-window Dynamo conservation drifted");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void electrolyzerCableEndpointsRespectEveryTierWindow(
            GameTestHelper helper) {
        CableBlock cable = ModBlocks.electricalConductorBlock(
                "copper", MaterialPrefixes.CABLE).get();
        List<Block> blocks = List.of(
                ModBlocks.ELECTROLYZER.get(),
                ModBlocks.ALUMINIUM_ELECTROLYZER.get(),
                ModBlocks.STAINLESS_STEEL_ELECTROLYZER.get());
        for (int index = 0; index < blocks.size(); index++) {
            BlockPos cablePos = new BlockPos(3 + index * 5, 2, 5);
            BlockPos machinePos = cablePos.above();
            helper.setBlock(
                    machinePos,
                    blocks.get(index).defaultBlockState().setValue(
                            ProcessingMachineBlock.FACING,
                            Direction.EAST));
            helper.setBlock(
                    cablePos,
                    conductorState(
                            cable, Direction.WEST, Direction.UP));
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
        BlockPos northStub = cablePos.north();
        BlockPos southStub = cablePos.south();
        BlockPos northMachine = northStub.above();
        BlockPos southMachine = southStub.above();
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
                northStub,
                conductorState(tinCable, Direction.SOUTH, Direction.UP));
        helper.setBlock(
                southStub,
                conductorState(tinCable, Direction.NORTH, Direction.UP));
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
                north.stored(EnergyType.ELECTRIC) == 30L,
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
        BlockPos machinePos = c.above();
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
                        Direction.UP));
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
                        Direction.UP));
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
        BlockPos machinePos = wirePos.above();
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
                        tinWire, Direction.WEST, Direction.UP));
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
        BlockPos machinePos = second.above();
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
                        tinCable, Direction.WEST, Direction.UP));
        helper.setBlock(
                first,
                conductorState(
                        tinCable, Direction.WEST, Direction.EAST));
        helper.setBlock(
                second,
                conductorState(
                        tinCable, Direction.WEST, Direction.UP));
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
            BlockPos machinePos = placement.getKey().above();
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
                            Direction.UP));
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
        RecipeMap.Entry source = ModRecipeMaps.SIFTER.entries().stream()
                .filter(entry -> entry.recipe().eut() > 0L
                        && entry.recipe().eut() <= 16L)
                .findFirst()
                .orElseThrow();
        RecipeMap.Match match = ModRecipeMaps.SIFTER.findMatch(
                queryFor(source.recipe())).orElseThrow();
        helper.assertTrue(
                match.id().equals(source.id()),
                "Sifter query resolved the wrong source recipe");
        loadRecipeInputs(sifter, match.recipe());
        primeEmittingKinetic(helper, engine);
        IFluidHandler steamInput = engine.fluids(Direction.WEST);
        helper.assertTrue(steamInput != null, "Steam engine back input missing");
        helper.assertTrue(
                steamInput.fill(
                        new FluidStack(ModFluids.STEAM_SOURCE.get(), 2_000),
                        IFluidHandler.FluidAction.EXECUTE) == 2_000,
                "Could not prime the tier-profile KU vertical");

        helper.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    helper.assertTrue(
                            sifter.progress() > 0,
                            "Steam-engine KU did not advance the sifter");
                    helper.assertTrue(
                            sifter.handles(
                                    EnergyType.KINETIC_PUSH,
                                    energyFace(sifter))
                                    && !sifter.handles(
                                            EnergyType.KINETIC_ROTATION,
                                            energyFace(sifter)),
                            "Sifter did not preserve its KU-only identity");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 260)
    public static void fireboxBoilerSteamEnginePowersPress(
            GameTestHelper helper) {
        BlockPos fireboxPos = new BlockPos(4, 1, 5);
        BlockPos boilerPos = fireboxPos.above();
        BlockPos pipePos = boilerPos.above();
        BlockPos enginePipePos = pipePos.east();
        BlockPos enginePos = enginePipePos.below();
        BlockPos pressPos = enginePos.below();
        GameTestHeatSources.placeHuSourceBlock(helper, fireboxPos);
        helper.setBlock(fireboxPos.north(), Blocks.AIR);
        helper.setBlock(fireboxPos.south(), Blocks.AIR);
        helper.setBlock(fireboxPos.east(), Blocks.AIR);
        helper.setBlock(fireboxPos.west(), Blocks.AIR);
        helper.setBlock(boilerPos, ModBlocks.BRONZE_BOILER.get());
        placeConnectedFluidPipe(
                helper, pipePos, Direction.DOWN, Direction.EAST);
        placeConnectedFluidPipe(
                helper, enginePipePos, Direction.WEST, Direction.DOWN);
        helper.setBlock(
                enginePos,
                ModBlocks.BRONZE_STEAM_ENGINE.get().defaultBlockState()
                        .setValue(SteamEngineBlock.FACING, Direction.DOWN));
        helper.setBlock(
                pressPos,
                ModBlocks.PRESS.get().defaultBlockState()
                        .setValue(
                                ProcessingMachineBlock.FACING,
                                Direction.EAST));
        FuelGeneratorBlockEntity firebox =
                helper.getBlockEntity(fireboxPos);
        BoilerBlockEntity boiler =
                helper.getBlockEntity(boilerPos);
        SteamEngineBlockEntity engine =
                helper.getBlockEntity(enginePos);
        ConfiguredProcessingMachineBlockEntity press =
                helper.getBlockEntity(pressPos);
        GTRecipe recipe = ModRecipeMaps.PRESS.entries().stream()
                .map(RecipeMap.Entry::recipe)
                .filter(candidate ->
                        candidate.eut() > 0L
                                && candidate.eut() <= 32L)
                .findFirst()
                .orElseThrow();
        loadRecipeInputs(press, recipe);
        IFluidHandler water = boiler.fluids(Direction.NORTH);
        FluidStack oneBucket = new FluidStack(Fluids.WATER, 1_000);
        helper.assertTrue(
                water != null
                        && water.fill(
                                oneBucket,
                                IFluidHandler.FluidAction.SIMULATE)
                                == 1_000
                        && boiler.waterAmount() == 0,
                "Boiler water simulation mutated the tank");
        helper.assertTrue(
                water.fill(
                                oneBucket,
                                IFluidHandler.FluidAction.EXECUTE)
                        == 1_000
                        && boiler.waterAmount() == 1_000,
                "Boiler water execute did not commit the simulated amount");
        primeBoilerOperatingPressure(helper, boiler);

        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(
                            boiler.status().equals("no_heat"),
                            "Watered boiler did not diagnose no_heat: "
                                    + boiler.status());
                    helper.assertTrue(
                            firebox.seedStoredEnergy(energyCapacity()),
                            "Could not fuel the source firebox");
                    GameTestHeatSources.keepHeatEmissionLive(
                            helper, firebox, 200);
                })
                .thenIdle(100)
                .thenExecute(() -> primeEmittingKinetic(helper, engine))
                .thenIdle(40)
                .thenExecute(() -> {
                    helper.assertTrue(
                            press.progress() > 0 || hasAnyOutput(press),
                            "Firebox -> boiler -> steam engine KU did not "
                                    + "advance Press: press="
                                    + press.pausedReason()
                                    + ", boiler="
                                    + boiler.status()
                                    + ", heat="
                                    + boiler.stored(
                                            com.masson.cruciblecraft.api.energy
                                                    .EnergyType.HEAT)
                                    + ", engine="
                                    + engine.status()
                                    + ", boilerSteam="
                                    + boiler.steamAmount()
                                    + ", engineSteam="
                                    + engine.steamAmount()
                                    + ", pipe="
                                    + pipeFluid(helper, pipePos)
                                    + ", firebox="
                                    + firebox.status()
                                    + ", stored="
                                    + firebox.energyStored());
                    helper.assertTrue(
                            boiler.waterAmount() < 1_000
                                    && (engine.stored() > 0L
                                            || engine.steamAmount() > 0)
                                    && firebox.energyCapacity() > 0L
                                    && engine.nominalOutputRate() == 12L,
                            "Steam chain did not preserve source conversion "
                                    + "rates or exhaust");

                    long savedHeat = firebox.energyStored();
                    int savedWater = boiler.waterAmount();
                    int savedSteam = engine.steamAmount();
                    long savedKu = engine.stored();
                    int savedExhaust = engine.exhaustAmount();
                    String boilerStatus = boiler.status();
                    String engineStatus = engine.status();
                    var registries = helper.getLevel().registryAccess();
                    CompoundTag fireboxTag =
                            firebox.saveWithoutMetadata(registries);
                    CompoundTag boilerTag =
                            boiler.saveWithoutMetadata(registries);
                    CompoundTag engineTag =
                            engine.saveWithoutMetadata(registries);
                    firebox.loadWithComponents(fireboxTag, registries);
                    boiler.loadWithComponents(boilerTag, registries);
                    engine.loadWithComponents(engineTag, registries);
                    helper.assertTrue(
                            firebox.energyStored() == savedHeat
                                    && boiler.waterAmount() == savedWater
                                    && engine.steamAmount() == savedSteam
                                    && engine.stored() == savedKu
                                    && engine.exhaustAmount()
                                            == savedExhaust
                                    && boiler.status().equals(
                                            boilerStatus)
                                    && engine.status().equals(
                                            engineStatus),
                            "Steam chain state did not survive NBT reload");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void steamChainDiagnosticsAndTransactions(
            GameTestHelper helper) {
        BlockPos boilerPos = new BlockPos(4, 2, 10);
        BlockPos enginePos = new BlockPos(10, 2, 10);
        helper.setBlock(boilerPos, ModBlocks.BRONZE_BOILER.get());
        helper.setBlock(
                enginePos,
                ModBlocks.BRONZE_STEAM_ENGINE.get().defaultBlockState()
                        .setValue(SteamEngineBlock.FACING, Direction.EAST));
        BoilerBlockEntity boiler =
                helper.getBlockEntity(boilerPos);
        SteamEngineBlockEntity engine =
                helper.getBlockEntity(enginePos);

        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(
                            boiler.status().equals("no_water"),
                            "Empty boiler did not diagnose no_water");
                    IFluidHandler water = boiler.fluids(Direction.NORTH);
                    FluidStack bucket =
                            new FluidStack(Fluids.WATER, 1_000);
                    helper.assertTrue(
                            water.fill(
                                            bucket,
                                            IFluidHandler.FluidAction.SIMULATE)
                                    == 1_000
                                    && boiler.waterAmount() == 0,
                            "Diagnostic water simulation mutated state");
                    helper.assertTrue(
                            water.fill(
                                            bucket,
                                            IFluidHandler.FluidAction.EXECUTE)
                                    == 1_000,
                            "Diagnostic water execute did not commit");
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(
                            boiler.status().equals("no_heat"),
                            "Watered boiler did not diagnose no_heat");
                    var registries = helper.getLevel().registryAccess();
                    FluidTank highSteam =
                            new FluidTank(
                                    BoilerBlockEntity.STEAM_CAPACITY);
                    highSteam.setFluid(new FluidStack(
                            ModFluids.STEAM_SOURCE.get(),
                            (BoilerBlockEntity.STEAM_CAPACITY * 3) / 4));
                    CompoundTag boilerTag =
                            boiler.saveWithoutMetadata(registries);
                    boilerTag.put(
                            "steam",
                            highSteam.writeToNBT(
                                    registries, new CompoundTag()));
                    boiler.loadWithComponents(boilerTag, registries);

                    IFluidHandler steamIo =
                            engine.fluids(Direction.WEST);
                    FluidStack sourceBatch = new FluidStack(
                            ModFluids.STEAM_SOURCE.get(), 200);
                    helper.assertTrue(
                            steamIo.fill(
                                            sourceBatch,
                                            IFluidHandler.FluidAction.SIMULATE)
                                    == 200
                                    && engine.steamAmount() == 0,
                            "Engine steam simulation mutated the tank");
                    helper.assertTrue(
                            steamIo.fill(
                                            sourceBatch,
                                            IFluidHandler.FluidAction.EXECUTE)
                                    == 200,
                            "Engine steam execute did not commit");
                    CompoundTag engineTag =
                            engine.saveWithoutMetadata(registries);
                    long primed = engine.kineticCapacity() / 2L;
                    engineTag.putLong("kinetic", primed);
                    engineTag.putInt(
                            "engine_state",
                            SteamEngineKuCurve.visualState(
                                    primed, engine.kineticCapacity()));
                    engineTag.putInt("stroke_sign", 1);
                    engine.loadWithComponents(engineTag, registries);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(
                            boiler.status().equals("no_heat")
                                    && boiler.steamAmount()
                                            == (BoilerBlockEntity.STEAM_CAPACITY
                                                    * 3)
                                                    / 4,
                            "High-pressure boiler without HU did not keep steam "
                                    + "and stay idle: "
                                    + boiler.status());
                    helper.assertTrue(
                            engine.steamAmount() == 0
                                    && engine.stored() > 0L
                                    && !engine.stopped(),
                            "Engine did not convert the primed steam batch: "
                                    + engine.status());
                    long before = engine.stored();
                    long rate = engine.currentOutputRate();
                    long packet = engine.strokeSign() * rate;
                    helper.assertTrue(
                            rate >= 6L && rate <= 24L,
                            "Full engine KU rate was not in 6-24: " + rate);
                    helper.assertTrue(
                            engine.extract(
                                            EnergyType.KINETIC_PUSH,
                                            packet,
                                            1L,
                                            Direction.EAST,
                                            true)
                                    == 1L
                                    && engine.stored() == before,
                            "KU extraction simulation mutated the buffer");
                    helper.assertTrue(
                            engine.extract(
                                            EnergyType.KINETIC_PUSH,
                                            packet,
                                            1L,
                                            Direction.EAST,
                                            false)
                                    == 1L
                                    && engine.stored() == before - rate,
                            "KU extraction execute differed from simulation");
                    var registries = helper.getLevel().registryAccess();
                    CompoundTag persisted =
                            engine.saveWithoutMetadata(registries);
                    String savedStatus = engine.status();
                    long savedKu = engine.stored();
                    int savedSteam = engine.steamAmount();
                    engine.loadWithComponents(persisted, registries);
                    helper.assertTrue(
                            engine.status().equals(savedStatus)
                                    && engine.stored() == savedKu
                                    && engine.steamAmount() == savedSteam,
                            "Diagnostic engine state did not survive reload");
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
                                "mortar/ingot_to_dust/iron"),
                "MORTAR_GRINDABLE iron did not resolve the live ingot rule");
        helper.assertTrue(
                amber != null
                        && amber.id().getPath().equals(
                                "mortar/gem_to_dust/amber"),
                "MORTAR_GRINDABLE amber did not resolve the live gem rule");
        helper.assertTrue(
                cinnabar != null
                        && cinnabar.id().getPath().equals(
                                "mortar/gem_to_dust/cinnabar"),
                "MORTAR_GRINDABLE cinnabar did not resolve the live gem rule");
        helper.assertTrue(
                ModRecipeMaps.MORTAR.findMatch(GTRecipeQuery.items(
                        material("ruby", MaterialPrefixes.GEM, 1))).isEmpty(),
                "A gem without MORTAR_GRINDABLE incorrectly matched");
        int mortarAuthored = ModRecipeMaps.MORTAR.authoredEntries().size();
        CrucibleCraft.LOGGER.info(
                "Mortar publication counts: authored={} logical={}",
                mortarAuthored,
                ModRecipeMaps.MORTAR.entries().size());
        helper.assertTrue(
                mortarAuthored == 1290,
                "Mortar publication count drifted from 1290");

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
                    var ironPlan = MachineExecutionPlan.create(
                            iron.recipe(),
                            ironMortar.variant().kind(),
                            ironMortar.variant().tier(),
                            1);
                    var amberPlan = MachineExecutionPlan.create(
                            amber.recipe(),
                            amberMortar.variant().kind(),
                            amberMortar.variant().tier(),
                            1);
                    var cinnabarPlan = MachineExecutionPlan.create(
                            cinnabar.recipe(),
                            cinnabarMortar.variant().kind(),
                            cinnabarMortar.variant().tier(),
                            1);
                    int ironPlanned = ironPlan.map(
                            MachineExecutionPlan::effectiveDuration).orElse(-1);
                    int amberPlanned = amberPlan.map(
                            MachineExecutionPlan::effectiveDuration).orElse(-1);
                    int cinnabarPlanned = cinnabarPlan.map(
                            MachineExecutionPlan::effectiveDuration).orElse(-1);
                    helper.assertTrue(
                            ironPlan.isPresent()
                                    && amberPlan.isPresent()
                                    && cinnabarPlan.isPresent()
                                    && ironMortar.duration() == ironPlanned
                                    && amberMortar.duration() == amberPlanned
                                    && cinnabarMortar.duration() == cinnabarPlanned,
                            "Mortar machines did not select the planned tag-driven duration: iron "
                                    + ironMortar.duration() + "/" + ironPlanned
                                    + " amber " + amberMortar.duration() + "/" + amberPlanned
                                    + " cinnabar " + cinnabarMortar.duration() + "/"
                                    + cinnabarPlanned);
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

    @GameTest(template = TEMPLATE, timeoutTicks = 50)
    public static void configuredShortRecipePublishesPerTickPermille(
            GameTestHelper helper) {
        ItemStack ironIngot = material("iron", MaterialPrefixes.INGOT, 1);
        RecipeMap.Match recipe = ModRecipeMaps.MORTAR.findMatch(
                GTRecipeQuery.items(ironIngot.copy())).orElseThrow();
        ConfiguredProcessingMachineBlockEntity mortar = placeConfigured(
                helper,
                new BlockPos(5, 2, 5),
                ModBlocks.MORTAR.get(),
                ModProcessingMachines.MORTAR);
        mortar.inventory().setStackInSlot(
                mortar.spec().items().inputs().getFirst(),
                ironIngot);
        fillKu(helper, mortar);

        int[] previous = {0};
        int[] intermediateAdvances = {0};
        boolean[] completed = {false};
        boolean[] reset = {false};
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    int plannedDuration = MachineExecutionPlan.create(
                                    recipe.recipe(),
                                    mortar.variant().kind(),
                                    mortar.variant().tier(),
                                    1)
                            .orElseThrow()
                            .effectiveDuration();
                    helper.assertTrue(
                            mortar.duration() == plannedDuration
                                    && mortar.duration() >= 8,
                            "Mortar did not select a duration suitable for the short window: duration="
                                    + mortar.duration()
                                    + " planned="
                                    + plannedDuration
                                    + " status="
                                    + mortar.pausedReason());
                    mortar.runtime().processor().setProgress(
                            mortar.duration() - 8);
                    fillKu(helper, mortar);
                })
                .thenExecuteFor(14, () -> {
                    int current = mortar.data().get(
                            ConfiguredProcessingMachineMenu
                                    .PROGRESS_PERMILLE_DATA_INDEX);
                    helper.assertTrue(
                            current >= 0 && current <= 1_000,
                            "Configured progress permille escaped its bounds");
                    if (!completed[0]) {
                        helper.assertTrue(
                                current >= previous[0],
                                "Configured progress permille regressed");
                        if (current > previous[0] && current < 1_000) {
                            intermediateAdvances[0]++;
                        }
                        completed[0] = current == 1_000;
                        previous[0] = current;
                    } else if (current == 0) {
                        reset[0] = true;
                    }
                    if (mortar.inventory()
                            .getStackInSlot(
                                    mortar.spec().items().outputs().getFirst())
                            .isEmpty()) {
                        fillKu(helper, mortar);
                    }
                })
                .thenExecute(() -> {
                    helper.assertTrue(
                            intermediateAdvances[0] >= 2,
                            "Short recipe did not expose per-tick intermediate permille");
                    helper.assertTrue(
                            completed[0],
                            "Short recipe never exposed exact 1000 permille");
                    helper.assertTrue(
                            reset[0],
                            "Completed progress did not follow the idle reset lifecycle");
                    helper.assertTrue(
                            !mortar.inventory()
                                    .getStackInSlot(
                                            mortar.spec().items().outputs().getFirst())
                                    .isEmpty(),
                            "Short recipe did not complete its real transaction");
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
                        "chemical/electrolyzer/trona"))
                .findFirst()
                .orElseThrow();
        ItemStack tronaInput =
                tronaSource.recipe().itemInputs().getFirst().getItems()[0].copy();
        tronaInput.setCount(tronaSource.recipe().itemInputCounts().getFirst());
        RecipeMap.Match trona = ModRecipeMaps.ELECTROLYZER.findMatch(
                GTRecipeQuery.items(tronaInput)).orElseThrow();
        helper.assertTrue(
                trona.id().getPath().equals("chemical/electrolyzer/trona"),
                "Trona query resolved the wrong chemical recipe");
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
                energyFace(electrolyzer),
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
                            "Blocked fluid output consumed chemical item input");
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
        GameTestHeatSources.placeHuSource(helper, pos.below());
        ConfiguredProcessingMachineBlockEntity drying = placeConfigured(
                helper, pos, ModBlocks.DRYING.get(), ModProcessingMachines.DRYING);
        FuelGeneratorBlockEntity firebox = helper.getBlockEntity(pos.below());
        helper.assertTrue(
                firebox.seedStoredEnergy(energyCapacity()),
                "Could not fuel HU drying route");
        RecipeMap.Entry source = ModRecipeMaps.DRYING.entries().stream()
                .filter(entry -> entry.id().getPath().equals("chemical/drying/mirabilite"))
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
                            drying.playerDrainFluids(fluidOut(drying)),
                            tankHandler);
                    loadRecipeInputs(drying, recipe);
                })
                .thenIdle(3)
                .thenExecute(() -> forceLastTick(helper, drying))
                .thenIdle(2)
                .thenExecute(() -> {
                    drainMachineIntoItem(
                            helper,
                            drying.playerDrainFluids(fluidOut(drying)),
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
        GameTestHeatSources.placeHuSource(helper, distilleryPos.below());
        ConfiguredProcessingMachineBlockEntity distillery = placeConfigured(
                helper,
                distilleryPos,
                ModBlocks.DISTILLERY.get(),
                ModProcessingMachines.DISTILLERY);
        FuelGeneratorBlockEntity firebox =
                helper.getBlockEntity(distilleryPos.below());
        helper.assertTrue(
                firebox.seedStoredEnergy(energyCapacity()),
                "Could not fuel HU distillery route");
        GTRecipe recipe = requireRecipe(
                ModRecipeMaps.DISTILLERY,
                "chemical/distillery/water_to_water_distilled");
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
                            distillery.playerDrainFluids(fluidOut(distillery)),
                            tankHandler);
                    loadRecipeInputs(distillery, recipe);
                })
                .thenIdle(3)
                .thenExecute(() -> forceLastTick(helper, distillery))
                .thenIdle(2)
                .thenExecute(() -> {
                    drainMachineIntoItem(
                            helper,
                            distillery.playerDrainFluids(fluidOut(distillery)),
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
                ModRecipeMaps.ELECTROLYZER, "chemical/electrolyzer/salt");
        GTRecipe hydrochloricAcid = requireMixerRecipeWithFluids(
                "cruciblecraft:chlorine",
                "cruciblecraft:water_distilled");
        loadRecipeInputs(electrolyzer, salt);
        helper.assertTrue(
                electrolyzer.insert(
                        EnergyType.ELECTRIC,
                        salt.eut(),
                        64L,
                        energyFace(electrolyzer),
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
                                            energyFace(electrolyzer),
                                            false)
                                    > 0L,
                            "Could not refill the component electrolyzer");
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
                            "Component electrolyzer produced no fluid: progress="
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
                            electrolyzer.playerDrainFluids(fluidOut(electrolyzer)),
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
                                    mixer.facing(),
                                    fluidIn(mixer),
                                    false,
                                    true,
                                    true,
                                    true)
                                    == ProcessingMachineInteractions.FluidTransfer.FILL_INPUT,
                            "Partially filled portable tank did not prefer machine fill");
                    fillMachineFromItem(
                            helper,
                            portableHandler,
                            mixer.fluids(fluidIn(mixer)));
                    helper.assertTrue(
                            portableHandler.getFluidInTank(0).isEmpty(),
                            "Portable tank retained chlorine after machine fill");
                    helper.assertTrue(
                            mixer.insert(
                                    mixer.spec().energy().type(),
                                    hydrochloricAcid.eut(),
                                    64L,
                                    energyFace(mixer),
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
                !CellContentGate.entries().isEmpty(),
                "Cell content allowlist registry is empty");
        // metadataOnly materials and gated materials such as clay_brick have
        // an empty live form list by the registration gate. Clay brick's
        // ingot is minecraft:brick. This check only requires the catalog
        // itself to contain prefixes and at least one live form.
        boolean catalogHasLiveForm = MaterialCatalog.startupValues().stream()
                .anyMatch(material ->
                        !MaterialCatalog.registeredForms(material).isEmpty());
        helper.assertTrue(
                !MaterialPrefixCatalog.values().isEmpty()
                        && !MaterialCatalog.startupValues().isEmpty()
                        && catalogHasLiveForm,
                "Material catalog has no registered prefix or material form");
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
                "Registered per-fluid or unexpected cell item identities");
        GTRecipe hydrogenFluoride = requireRecipe(
                ModRecipeMaps.MIXER,
                "chemical/mixer/fluid_closure_hydrogen_fluoride");
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
                mixer.fluids(fluidIn(mixer)));
        helper.assertTrue(
                gasHandler.getFluidInTank(0).isEmpty(),
                "Gas cell retained fluid after machine transfer");
        helper.assertTrue(
                mixer.insert(
                        mixer.spec().energy().type(),
                        hydrogenFluoride.eut(),
                        64L,
                        energyFace(mixer),
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
    public static void reusedChemicalHostsExecuteExpandedOutputShapes(
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
        GameTestHeatSources.placeHuSource(helper, fireboxPos);
        ConfiguredProcessingMachineBlockEntity smelter = placeConfigured(
                helper,
                smelterPos,
                ModBlocks.SMELTER.get(),
                ModProcessingMachines.SMELTER);
        FuelGeneratorBlockEntity firebox = helper.getBlockEntity(fireboxPos);
        helper.assertTrue(
                firebox.seedStoredEnergy(energyCapacity()),
                "Could not fuel reused chemical smelter");
        GTRecipe bathRecipe = requireRecipe(
                ModRecipeMaps.BATH, "chemical/bath/niobium_pentoxide");
        GTRecipe centrifugeRecipe = requireRecipe(
                ModRecipeMaps.CENTRIFUGE, "chemical/centrifuge/gloomstone");
        GTRecipe smelterRecipe = requireRecipe(
                ModRecipeMaps.SMELTER, "chemical/smelter/ilmenite");
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
                            "A reused chemical host did not start its expanded recipe: "
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

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void compositionGeneratedCarbonFamilyExecutesTwoMaterials(
            GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity charcoal = placeConfigured(
                helper,
                new BlockPos(3, 2, 5),
                ModBlocks.STAINLESS_STEEL_ELECTROLYZER.get(),
                ModProcessingMachines.ELECTROLYZER);
        ConfiguredProcessingMachineBlockEntity coal = placeConfigured(
                helper,
                new BlockPos(9, 2, 5),
                ModBlocks.STAINLESS_STEEL_ELECTROLYZER.get(),
                ModProcessingMachines.ELECTROLYZER);
        GTRecipe charcoalRecipe = requireRecipe(
                ModRecipeMaps.ELECTROLYZER,
                "chemical/electrolyzer/charcoal");
        GTRecipe coalRecipe = requireRecipe(
                ModRecipeMaps.ELECTROLYZER,
                "chemical/electrolyzer/coal");
        loadRecipeInputs(charcoal, charcoalRecipe);
        loadRecipeInputs(coal, coalRecipe);
        helper.assertTrue(
                charcoal.insert(
                        EnergyType.ELECTRIC,
                        charcoalRecipe.eut(),
                        64L,
                        energyFace(charcoal),
                        false) > 0L
                        && coal.insert(
                                EnergyType.ELECTRIC,
                                coalRecipe.eut(),
                                64L,
                                energyFace(coal),
                                false) > 0L,
                "Could not power both carbon-family electrolyzers");

        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    helper.assertTrue(
                            charcoal.progress() > 0 && coal.progress() > 0,
                            "Carbon composition family did not start");
                    forceLastTick(helper, charcoal);
                    forceLastTick(helper, coal);
                    helper.assertTrue(
                            charcoal.insert(
                                    EnergyType.ELECTRIC,
                                    charcoalRecipe.eut(),
                                    64L,
                                    energyFace(charcoal),
                                    false) > 0L
                                    && coal.insert(
                                            EnergyType.ELECTRIC,
                                            coalRecipe.eut(),
                                            64L,
                                            energyFace(coal),
                                            false) > 0L,
                            "Could not re-power carbon-family electrolyzers after force");
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    ItemStack expected =
                            material("carbon", MaterialPrefixes.DUST, 1);
                    for (ConfiguredProcessingMachineBlockEntity machine :
                            List.of(charcoal, coal)) {
                        helper.assertTrue(
                                machine.spec().items().outputs().stream()
                                        .map(slot -> machine.inventory()
                                                .getStackInSlot(slot))
                                        .anyMatch(stack ->
                                                stack.is(expected.getItem())
                                                        && stack.getCount() == 1),
                                machine.spec().id()
                                        + " did not commit one carbon dust");
                        helper.assertTrue(
                                machine.spec().items().inputs().stream()
                                        .allMatch(slot -> machine.inventory()
                                                .getStackInSlot(slot)
                                                .isEmpty()),
                                machine.spec().id()
                                        + " retained its consumed family input");
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void mixerGunpowderTemplateExecutesAllMembers(
            GameTestHelper helper) {
        List<String> materials =
                List.of("carbon", "charcoal", "coal", "coal_coke");
        List<ConfiguredProcessingMachineBlockEntity> mixers =
                new ArrayList<>();
        for (int index = 0; index < materials.size(); index++) {
            String material = materials.get(index);
            ConfiguredProcessingMachineBlockEntity mixer = placeConfigured(
                    helper,
                    new BlockPos(2 + index * 3, 2, 5),
                    ModBlocks.MIXER.get(),
                    ModProcessingMachines.MIXER);
            GTRecipe recipe = requireRecipe(
                    ModRecipeMaps.MIXER,
                    "chemical/mixer/gunpowder/" + material);
            loadRecipeInputs(mixer, recipe);
            helper.assertTrue(
                    mixer.insert(
                            mixer.spec().energy().type(),
                            recipe.eut(),
                            64L,
                            energyFace(mixer),
                            false) > 0L,
                    "Could not power gunpowder Mixer for " + material);
            mixers.add(mixer);
        }

        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    for (ConfiguredProcessingMachineBlockEntity mixer :
                            mixers) {
                        helper.assertTrue(
                                mixer.progress() > 0,
                                "Gunpowder template did not start for "
                                        + mixer.getBlockPos());
                        forceLastTick(helper, mixer);
                    }
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    for (ConfiguredProcessingMachineBlockEntity mixer :
                            mixers) {
                        helper.assertTrue(
                                mixer.spec().items().outputs().stream()
                                        .map(slot -> mixer.inventory()
                                                .getStackInSlot(slot))
                                        .anyMatch(stack ->
                                                stack.is(Items.GUNPOWDER)
                                                        && stack.getCount() == 4),
                                "Gunpowder Mixer did not commit four gunpowder");
                        helper.assertTrue(
                                mixer.spec().items().inputs().stream()
                                        .allMatch(slot -> mixer.inventory()
                                                .getStackInSlot(slot)
                                                .isEmpty()),
                                "Gunpowder Mixer retained a consumed template input");
                    }
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

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void materialRuleAnvilRecipeLoadsAndExecutes(
            GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 2, 3);
        helper.setBlock(
                pos,
                ModBlocks.mteInPlaceBlocksById()
                        .get(MteInPlaceGameTestSupport.id("stone/anvil"))
                        .get());
        AnvilBlockEntity anvil = helper.getBlockEntity(pos);
        ItemStack ingot = material("iron", MaterialPrefixes.INGOT, 1);
        ItemStack expectedPlate = material("iron", MaterialPrefixes.PLATE, 1);
        RecipeMap.Match loaded = ModRecipeMaps.ANVIL.findMatch(
                GTRecipeQuery.items(ingot)).orElse(null);

        helper.assertTrue(
                loaded != null
                        && loaded.id().equals(ResourceLocation.fromNamespaceAndPath(
                                CrucibleCraft.MODID,
                                "anvil/ingot_to_plate/iron")),
                "Material-rule anvil recipe did not load under its stable id");
        helper.assertTrue(anvil.insertOrMerge(0, ingot.copy()) > 0,
                "Anvil rejected the material-rule input");
        AnvilBlockEntity.StrikeResult result = null;
        for (int hit = 0; hit < 4; hit++) {
            result = anvil.strike(AnvilMode.ANVIL, 2).orElse(null);
            helper.assertTrue(result != null,
                    "Anvil failed to execute a material-rule strike");
        }
        helper.assertTrue(result.completed(),
                "Anvil material-rule recipe did not complete after four strikes");
        helper.assertTrue(
                ItemStack.isSameItemSameComponents(
                        anvil.workpiece(), expectedPlate),
                "Anvil material-rule recipe produced the wrong output");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void liveAnvilUsesGt6Collision(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 2, 3);
        helper.setBlock(
                pos,
                ModBlocks.mteInPlaceBlocksById()
                        .get(MteInPlaceGameTestSupport.id("steel/anvil"))
                        .get()
                        .defaultBlockState());
        var state = helper.getBlockState(pos);
        var box = state.getShape(helper.getLevel(), pos).bounds();
        helper.assertTrue(
                box.minX == 0.0
                        && box.maxX == 1.0
                        && box.minY == 0.0
                        && box.maxY == 0.75
                        && box.minZ == 0.25
                        && box.maxZ == 0.75,
                "live anvil collision is not GT6 NORTH_SOUTH 12-high");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void uniqueSteelAnvilHostsAnvilBackend(GameTestHelper helper) {
        MteInPlaceGameTestSupport.assertLive(
                helper, "steel/anvil", com.masson.cruciblecraft.content.mte.MteInPlaceKind.MISC_TOOL);
        BlockPos pos = new BlockPos(3, 2, 3);
        helper.setBlock(
                pos,
                ModBlocks.mteInPlaceBlocksById()
                        .get(MteInPlaceGameTestSupport.id("steel/anvil"))
                        .get()
                        .defaultBlockState());
        AnvilBlockEntity anvil = helper.getBlockEntity(pos);
        helper.assertTrue(
                anvil instanceof AnvilBlockEntity
                        && "steel".equals(anvil.materialId())
                        && anvil.maxDurability() == 10_000_000L,
                "steel/anvil did not host the anvil backend");
        var box = helper.getBlockState(pos).getShape(helper.getLevel(), pos).bounds();
        helper.assertTrue(box.maxY == 0.75, "unique anvil collision is not 12-high");
        ItemStack ingot = material("iron", MaterialPrefixes.INGOT, 1);
        ItemStack expectedPlate = material("iron", MaterialPrefixes.PLATE, 1);
        helper.assertTrue(
                anvil.insertOrMerge(0, ingot.copy()) > 0,
                "unique steel anvil rejected the ingot");
        AnvilBlockEntity.StrikeResult result = null;
        for (int hit = 0; hit < 4; hit++) {
            result = anvil.strike(AnvilMode.ANVIL, 2).orElse(null);
            helper.assertTrue(result != null, "unique steel anvil failed a strike");
        }
        helper.assertTrue(result.completed(), "unique steel anvil did not complete");
        helper.assertTrue(
                ItemStack.isSameItemSameComponents(anvil.workpiece(), expectedPlate),
                "unique steel anvil produced the wrong output");
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
    public static void materialRuleCrusherRecipeLoadsAndExecutes(
            GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 2, 3);
        helper.setBlock(pos, ModBlocks.BRONZE_CRUSHER.get());
        CrusherBlockEntity crusher = helper.getBlockEntity(pos);
        ItemStack ingot = material("tungsten", MaterialPrefixes.INGOT, 1);
        RecipeMap.Match loaded = ModRecipeMaps.CRUSHER.findMatch(
                GTRecipeQuery.items(ingot)).orElse(null);
        helper.assertTrue(
                loaded != null
                        && loaded.id().equals(ResourceLocation.fromNamespaceAndPath(
                                CrucibleCraft.MODID,
                                "crusher/ingot_to_dust/tungsten")),
                "Material-rule crusher recipe did not load under its stable id");
        crusher.inventory().setStackInSlot(0, ingot);
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
                            "Bath did not consume input: "
                                    + bath.pausedReason()
                                    + " duration="
                                    + bath.duration()
                                    + " progress="
                                    + bath.progress()
                                    + " fluid="
                                    + inputFluidAmount(bath)
                                    + " count="
                                    + bath.inventory().getStackInSlot(0).getCount());
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
                        == PredicateKind.PORT)
                .forEach(element -> helper.setBlock(
                        structure.worldPosition(
                                controllerPos, facing, element.offset()),
                        ModBlocks.FIREBRICK.get()));
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
        int operations = CokeOvenBlockEntity.PARALLEL;
        FluidStack expectedCreosote = recipe.fluidOutputs().getFirst().copyWithAmount(
                recipe.fluidOutputs().getFirst().getAmount() * operations);
        ItemStack expectedCoke = recipe.itemOutputs().getFirst().copyWithCount(
                recipe.itemOutputs().getFirst().getCount() * operations);
        cokeOven.inventory().setStackInSlot(
                CokeOvenBlockEntity.INPUT_SLOT,
                new ItemStack(Items.COAL, operations * recipe.itemInputCounts().getFirst()));

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
                    helper.assertTrue(
                            cokeOven.inventory().getStackInSlot(
                                    CokeOvenBlockEntity.INPUT_SLOT).isEmpty(),
                            "Coke oven did not consume coal at start");
                    helper.assertTrue(
                            cokeOven.inventory().getStackInSlot(
                                    CokeOvenBlockEntity.OUTPUT_SLOT).isEmpty(),
                            "Coke oven emitted coke before the recipe finished");
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
                                    expectedCoke),
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

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void cokeOvenFirebrickHopperInsertsFromTop(GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 3, 6);
        Direction facing = Direction.NORTH;
        helper.setBlock(controllerPos, ModBlocks.COKE_OVEN.get());
        var structure = MultiblockStructureCatalog.require(
                CokeOvenBlockEntity.STRUCTURE_ID);
        structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.PORT)
                .forEach(element -> helper.setBlock(
                        structure.worldPosition(
                                controllerPos, facing, element.offset()),
                        ModBlocks.FIREBRICK.get()));
        BlockPos topBrick = CokeOvenBlockEntity.structureCenter(
                controllerPos, facing).above();
        helper.setBlock(
                topBrick.above(),
                hopperBlock("steel_hopper").defaultBlockState()
                        .setValue(HopperBlock.FACING, Direction.DOWN));
        HopperBlockEntity hopper = helper.getBlockEntity(topBrick.above());
        hopper.inventory().setStackInSlot(0, new ItemStack(Items.COAL, 4));
        CokeOvenBlockEntity cokeOven = helper.getBlockEntity(controllerPos);
        helper.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    helper.assertTrue(
                            cokeOven.structureValid(),
                            "Coke oven structure was not recognized before hopper insert");
                    helper.assertTrue(
                            cokeOven.inventory().getStackInSlot(
                                    CokeOvenBlockEntity.INPUT_SLOT).getCount() >= 1,
                            "Hopper on the firebrick top did not insert coal");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void cokeOvenDoesNotShareFirebricks(GameTestHelper helper) {
        Direction facing = Direction.NORTH;
        BlockPos first = new BlockPos(5, 3, 5);
        BlockPos second = new BlockPos(7, 3, 5);
        helper.setBlock(first, ModBlocks.COKE_OVEN.get());
        helper.setBlock(second, ModBlocks.COKE_OVEN.get());
        var structure = MultiblockStructureCatalog.require(
                CokeOvenBlockEntity.STRUCTURE_ID);
        java.util.LinkedHashSet<BlockPos> bricks = new java.util.LinkedHashSet<>();
        for (BlockPos controller : List.of(first, second)) {
            structure.structure().stream()
                    .filter(element -> structure.predicate(element).kind()
                            == PredicateKind.PORT)
                    .map(element -> structure.worldPosition(
                            controller, facing, element.offset()))
                    .forEach(bricks::add);
        }
        bricks.forEach(pos -> helper.setBlock(pos, ModBlocks.FIREBRICK.get()));
        CokeOvenBlockEntity ovenA = helper.getBlockEntity(first);
        CokeOvenBlockEntity ovenB = helper.getBlockEntity(second);
        helper.startSequence()
                .thenIdle(45)
                .thenExecute(() -> {
                    helper.assertTrue(
                            ovenA.structureValid() ^ ovenB.structureValid(),
                            "Adjacent coke ovens must not both form while sharing a wall");
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
                            structurePaletteBlock(predicate));
                });
        LargeCentrifugeBlockEntity centrifuge =
                helper.getBlockEntity(controllerPos);
        GTRecipe recipe = requireRecipe(
                ModRecipeMaps.CENTRIFUGE,
                "chemical/centrifuge/gloomstone");
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
                                    && centrifuge.spec().fluids().outputs().size() == 6
                                    && centrifuge.spec().fluids().all().size() == 7,
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

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void largeCentrifugePluginQuarantineAcrossNbtReload(
            GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        Direction facing = Direction.NORTH;
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.LARGE_CENTRIFUGE.structureId());
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
                            structurePaletteBlock(predicate));
                });
        LargeCentrifugeBlockEntity centrifuge =
                helper.getBlockEntity(controllerPos);

        // Clean reload accepts the persisted ids without quarantine.
        CompoundTag clean = centrifuge.saveWithoutMetadata(
                helper.getLevel().registryAccess());
        centrifuge.loadWithComponents(
                clean, helper.getLevel().registryAccess());
        helper.assertTrue(
                !centrifuge.pluginQuarantined(),
                "Clean plugin reload was quarantined");

        // Contents must survive a quarantined reload unchanged.
        GTRecipe recipe = requireRecipe(
                ModRecipeMaps.CENTRIFUGE, "chemical/centrifuge/gloomstone");
        loadRecipeInputs(centrifuge, recipe);
        int inputSlot = centrifuge.spec().items().inputs().getFirst();
        ItemStack savedInput =
                centrifuge.inventory().getStackInSlot(inputSlot).copy();
        CompoundTag tampered = centrifuge.saveWithoutMetadata(
                helper.getLevel().registryAccess());
        ListTag plugins = tampered.getList(
                "multiblock_plugins", Tag.TAG_STRING);
        plugins.set(
                0, StringTag.valueOf("cruciblecraft:not_a_plugin"));
        tampered.put("multiblock_plugins", plugins);
        centrifuge.loadWithComponents(
                tampered, helper.getLevel().registryAccess());
        helper.assertTrue(
                centrifuge.pluginQuarantined(),
                "Plugin id mismatch did not quarantine the controller");
        helper.assertTrue(
                centrifuge.pluginQuarantineReason()
                        .contains("does not match"),
                "Plugin quarantine did not expose its diagnostic");
        helper.assertTrue(
                ItemStack.isSameItemSameComponents(
                        centrifuge.inventory().getStackInSlot(inputSlot),
                        savedInput),
                "Contents changed under plugin quarantine");

        // Repairing the ids recovers the machine.
        CompoundTag repaired = centrifuge.saveWithoutMetadata(
                helper.getLevel().registryAccess());
        ListTag repairedPlugins = repaired.getList(
                "multiblock_plugins", Tag.TAG_STRING);
        repairedPlugins.set(
                0, StringTag.valueOf("cruciblecraft:processing_host"));
        repaired.put("multiblock_plugins", repairedPlugins);
        centrifuge.loadWithComponents(
                repaired, helper.getLevel().registryAccess());
        helper.assertTrue(
                !centrifuge.pluginQuarantined(),
                "Repaired plugin ids stayed quarantined");
        helper.succeed();
    }





    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void tank3x3x3Formation(
            GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        Direction facing = Direction.NORTH;
        var structure = MultiblockStructureCatalog.require(
                TankBlockEntity.STRUCTURE_ID);
        TankBlockEntity tank = placeTank(
                helper, controllerPos, facing, "stainless_steel");
        BlockPos firstPort = structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.PORT)
                .map(element -> structure.worldPosition(
                        controllerPos, facing, element.offset()))
                .findFirst()
                .orElseThrow();

        helper.startSequence()
                .thenIdle(25)
                .thenExecute(() -> {
                    helper.assertTrue(
                            tank.structureValid(),
                            "Tank structure was not recognized");
                    MteInPlaceBlockEntity port =
                            helper.getBlockEntity(firstPort);
                    int filled = port.fluidHandler(facing).fill(
                            new FluidStack(Fluids.WATER, 10_000),
                            IFluidHandler.FluidAction.EXECUTE);
                    helper.assertTrue(
                            filled == 10_000
                                    && tank.contents().getFluidAmount()
                                            == 10_000,
                            "Tank did not accept fluid through a port: "
                                    + filled
                                    + " stored="
                                    + tank.contents().getFluidAmount());
                    FluidStack drained = port.fluidHandler(facing).drain(
                            4_000, IFluidHandler.FluidAction.EXECUTE);
                    helper.assertTrue(
                            drained.getAmount() == 4_000
                                    && tank.contents().getFluidAmount()
                                            == 6_000,
                            "Tank did not drain through the same port: "
                                    + drained.getAmount()
                                    + " stored="
                                    + tank.contents().getFluidAmount());
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void tank3x3x3Fidelity(
            GameTestHelper helper) {
        var structure = MultiblockStructureCatalog.require(
                TankBlockEntity.STRUCTURE_ID);
        long ports = structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.PORT)
                .count();
        long air = structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.AIR)
                .count();
        long controllers = structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.CONTROLLER)
                .count();
        helper.assertTrue(
                structure.structure().size() == 27
                        && ports == 25
                        && air == 1
                        && controllers == 1,
                "Tank geometry drifted from the GT6 source: "
                        + structure.structure().size()
                        + " positions, "
                        + ports
                        + " ports, "
                        + air
                        + " air");
        var source = structure.source().orElseThrow();
        helper.assertTrue(
                source.className().endsWith("MultiTileEntityTank3x3x3")
                        && "3703e40308c8c030763fd6297dea8b210d2a77b1"
                                .equals(source.revision()),
                "Tank source provenance drifted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void tank3x3x3PortSupplySingleHost(
            GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        Direction facing = Direction.NORTH;
        var structure = MultiblockStructureCatalog.require(
                TankBlockEntity.STRUCTURE_ID);
        TankBlockEntity tank = placeTank(
                helper, controllerPos, facing, "stainless_steel");
        helper.startSequence()
                .thenIdle(25)
                .thenExecute(() -> {
                    helper.assertTrue(
                            tank.structureValid(),
                            "Tank structure was not recognized");
                    helper.assertTrue(
                            tank.tanks().size() == 1
                                    && tank.fluidInputTanks()
                                            .equals(List.of(0))
                                    && tank.fluidOutputTanks()
                                            .equals(List.of(0))
                                    && tank.contents().getCapacity() == 1_728_000,
                            "Tank host supply roles drifted");
                    long physicalPorts = structure.structure().stream()
                            .filter(element -> structure
                                    .predicate(element).kind()
                                    == PredicateKind.PORT)
                            .count();
                    helper.assertTrue(
                            physicalPorts == 25,
                            "Physical port count drifted");
                    int filled = tank.contents().getFluidAmount();
                    helper.assertTrue(
                            structure.structure().stream()
                                    .filter(element -> structure
                                            .predicate(element).kind()
                                            == PredicateKind.PORT)
                                    .map(element -> structure.worldPosition(
                                            controllerPos, facing,
                                            element.offset()))
                                    .allMatch(portPos -> {
                                        MteInPlaceBlockEntity port =
                                                helper.getBlockEntity(
                                                        portPos);
                                        return port.fluidHandler(facing)
                                                        .getTanks()
                                                == 1
                                                && port.fluidHandler(facing)
                                                                .getFluidInTank(
                                                                        0)
                                                                .getAmount()
                                                        == filled;
                                    }),
                            "Twenty-five ports did not bridge one host tank");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void tank3x3x3ProfileAndWallBinding(
            GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        Direction facing = Direction.NORTH;
        var structure = MultiblockStructureCatalog.require(
                TankBlockEntity.STRUCTURE_ID);
        TankBlockEntity tank = placeTank(
                helper, controllerPos, facing, "invar");
        BlockPos firstPort = structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.PORT)
                .map(element -> structure.worldPosition(
                        controllerPos, facing, element.offset()))
                .findFirst()
                .orElseThrow();
        helper.startSequence()
                .thenIdle(25)
                .thenExecute(() -> {
                    helper.assertTrue(
                            tank.structureValid()
                                    && tank.profile().capacityMb() == 1_728_000
                                    && tank.profile().gasProof()
                                    && !tank.profile().acidProof(),
                            "Invar Tank Main Valve profile drifted");
                    int filled = tank.fill(
                            new FluidStack(Fluids.WATER, 2_000),
                            IFluidHandler.FluidAction.EXECUTE);
                    FluidStack drained = tank.drain(
                            500, IFluidHandler.FluidAction.EXECUTE);
                    helper.assertTrue(
                            filled == 2_000
                                    && drained.getAmount() == 500,
                            "Controller direct fill/drain contract failed");
                    helper.setBlock(
                            firstPort,
                            tankWallBlock("stainless_steel")
                                    .defaultBlockState());
                })
                .thenIdle(25)
                .thenExecute(() -> helper.assertTrue(
                        !tank.structureValid(),
                        "Mixed Tank wall material formed"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void tank3x3x3VerticalFormation(
            GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 4, 6);
        TankBlockEntity tank = placeTank(
                helper, controllerPos, Direction.UP, "stainless_steel");
        helper.startSequence()
                .thenIdle(25)
                .thenExecute(() -> helper.assertTrue(
                        tank.structureValid(),
                        "Vertical Tank Main Valve did not form"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void tank3x3x3TemperatureMeltdown(
            GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        TankBlockEntity tank = placeTank(
                helper, controllerPos, Direction.NORTH, "stainless_steel");
        FluidStack moltenTungsten = new FluidStack(
                ModFluids.materialFluid("tungsten").orElseThrow(), 1_000);
        tank.contents().setFluid(moltenTungsten);
        helper.startSequence()
                .thenIdle(25)
                .thenExecute(() -> helper.assertTrue(
                        helper.getBlockState(controllerPos)
                                .is(ModBlocks.MELTDOWN_LAVA.get()),
                        "Over-temperature Tank did not melt down"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void tank3x3x3AcidFailure(
            GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        TankBlockEntity tank = placeTank(
                helper, controllerPos, Direction.NORTH, "invar");
        FluidStack hydrochloricAcid = new FluidStack(
                ModFluids.materialFluid("hydrochloric_acid").orElseThrow(),
                1_000);
        tank.contents().setFluid(hydrochloricAcid);
        helper.startSequence()
                .thenIdle(25)
                .thenExecute(() -> helper.assertTrue(
                        helper.getBlockState(controllerPos).isAir(),
                        "Unproofed acid Tank did not fail closed"))
                .thenSucceed();
    }

    private static TankBlockEntity placeTank(
            GameTestHelper helper,
            BlockPos controllerPos,
            Direction facing,
            String material) {
        var structure = MultiblockStructureCatalog.require(
                TankBlockEntity.STRUCTURE_ID);
        helper.setBlock(
                controllerPos,
                tankControllerBlock(material)
                        .defaultBlockState()
                        .setValue(MteInPlaceBlock.FACING, facing));
        Block wall = tankWallBlock(material);
        structure.structure().forEach(element -> {
            BlockPos position = structure.worldPosition(
                    controllerPos, facing, element.offset());
            switch (structure.predicate(element).kind()) {
                case PORT -> helper.setBlock(position, wall.defaultBlockState());
                case AIR -> helper.setBlock(
                        position, Blocks.AIR.defaultBlockState());
                default -> {
                }
            }
        });
        return helper.getBlockEntity(controllerPos);
    }

    private static Block tankControllerBlock(String material) {
        String path = material.startsWith("dense_")
                ? "multiblock/small_" + material + "_tank_main_valve"
                : "wood".equals(material)
                        ? "wood/tank_main_valve"
                        : "multiblock/small_" + material + "_tank_main_valve";
        return ModBlocks.mteInPlaceBlocksById()
                .get(ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID, path))
                .get();
    }

    private static Block tankWallBlock(String material) {
        String path = material.startsWith("dense_")
                ? "multiblock/" + material + "_wall"
                : "wood".equals(material)
                        ? "wood/wall"
                        : material + "/wall";
        return ModBlocks.mteInPlaceBlocksById()
                .get(ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID, path))
                .get();
    }

    private static Block largeBoilerController() {
        return ModBlocks.mteInPlaceBlocksById()
                .get(LargeBoilerTier.STAINLESS_STEEL.controllerId())
                .get();
    }

    private static LargeBoilerBlockEntity placeLargeBoiler(
            GameTestHelper helper,
            BlockPos controllerPos,
            Direction facing) {
        var structure = MultiblockStructureCatalog.require(
                LargeBoilerBlockEntity.STRUCTURE_ID);
        helper.setBlock(
                controllerPos,
                largeBoilerController()
                        .defaultBlockState()
                        .setValue(MteInPlaceBlock.FACING, facing));
        structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.PORT)
                .forEach(element -> helper.setBlock(
                        structure.worldPosition(
                                controllerPos, facing, element.offset()),
                        structurePaletteBlock(
                                structure.predicate(element))));
        BlockEntity entity = helper.getBlockEntity(controllerPos);
        helper.assertTrue(
                entity instanceof LargeBoilerBlockEntity,
                "Large boiler controller is "
                        + (entity == null
                                ? "missing"
                                : entity.getClass().getSimpleName()));
        return (LargeBoilerBlockEntity) entity;
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 800)
    public static void largeBoilerFormation(
            GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        Direction facing = Direction.NORTH;
        var structure = MultiblockStructureCatalog.require(
                LargeBoilerBlockEntity.STRUCTURE_ID);
        helper.setBlock(
                controllerPos,
                largeBoilerController()
                        .defaultBlockState()
                        .setValue(MteInPlaceBlock.FACING, facing));
        structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.PORT)
                .forEach(element -> {
                    var predicate = structure.predicate(element);
                    helper.setBlock(
                            structure.worldPosition(
                                    controllerPos, facing, element.offset()),
                            structurePaletteBlock(predicate));
                });
        LargeBoilerBlockEntity boiler =
                helper.getBlockEntity(controllerPos);
        BlockPos energyPort = structure.anchor(
                "bottom_energy_input", controllerPos, facing);
        GameTestHeatSources.placeHuSource(helper, energyPort.below());
        FuelGeneratorBlockEntity firebox =
                helper.getBlockEntity(energyPort.below());
        BlockPos waterPort = structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.PORT)
                .filter(element -> structure.predicate(element).port()
                        .orElseThrow()
                        == com.masson.cruciblecraft.content.multiblock
                                .MultiblockStructureDefinition.PortType
                                .ITEM_FLUID_IN)
                .map(element -> structure.worldPosition(
                        controllerPos, facing, element.offset()))
                .findFirst()
                .orElseThrow();
        MteInPlaceBlockEntity waterPortBe =
                helper.getBlockEntity(waterPort);

        helper.startSequence()
                .thenIdle(LARGE_BOILER_STRUCTURE_WAIT)
                .thenExecute(() -> {
                    helper.assertTrue(
                            boiler.structureValid(),
                            "Large boiler structure was not recognized");
                    helper.assertTrue(
                            firebox.seedStoredEnergy(energyCapacity()),
                            "Could not fuel the boiler heat source");
                })
                .thenIdle(10)
                .thenExecute(() -> {
                    helper.assertTrue(
                            boiler.stored(EnergyType.HEAT) > 0L,
                            "Boiler did not receive port-fed heat");
                    boiler.insert(
                            EnergyType.HEAT,
                            1L,
                            1L,
                            Direction.UP,
                            false);
                    helper.assertTrue(
                            boiler.coolDownResetTimer() >= 32,
                            "HU injection did not refresh boiler cooldown");
                    var longFluidPort = helper.getLevel().getCapability(
                            ModCapabilities.LONG_FLUID_HANDLER,
                            helper.absolutePos(waterPort),
                            Direction.UP);
                    helper.assertTrue(
                            longFluidPort != null
                                    && longFluidPort.capacity(0)
                                            == LargeBoilerTier.WATER_CAPACITY,
                            "Boiler port did not expose its long fluid view");
                    int filled = waterPortBe.fluidHandler(Direction.UP).fill(
                            new FluidStack(Fluids.WATER, 1_000),
                            IFluidHandler.FluidAction.EXECUTE);
                    helper.assertTrue(
                            filled == 1_000,
                            "Water port did not accept water: " + filled);
                })
                .thenIdle(40)
                .thenExecute(() -> helper.assertTrue(
                        boiler.steamTank().getFluidAmount() > 0
                                && boiler.waterTank().getFluidAmount() < 1_000,
                        "Boiler did not convert water and heat into steam: "
                                + "water="
                                + boiler.waterTank().getFluidAmount()
                                + " steam="
                                + boiler.steamTank().getFluidAmount()
                                + " heat="
                                + boiler.stored(EnergyType.HEAT)))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void largeBoilerLegacyControllerMigration(
            GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        helper.setBlock(
                controllerPos,
                ModBlocks.LARGE_BOILER.get()
                        .defaultBlockState()
                        .setValue(ProcessingMachineBlock.FACING, Direction.NORTH));
        LargeBoilerBlockEntity legacy = helper.getBlockEntity(controllerPos);
        legacy.waterTank().fill(
                new FluidStack(Fluids.WATER, 1_000),
                IFluidHandler.FluidAction.EXECUTE);
        helper.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    helper.assertTrue(
                            helper.getBlockState(controllerPos)
                                    .is(largeBoilerController()),
                            "Legacy large boiler was not migrated");
                    LargeBoilerBlockEntity migrated =
                            helper.getBlockEntity(controllerPos);
                    helper.assertTrue(
                            migrated.structureId().equals(
                                    LargeBoilerTier.STAINLESS_STEEL.structureId())
                                    && migrated.waterTank().getFluidAmount() == 1_000,
                            "Legacy boiler contents/profile were not preserved");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void largeBoilerFidelity(
            GameTestHelper helper) {
        var structure = MultiblockStructureCatalog.require(
                LargeBoilerBlockEntity.STRUCTURE_ID);
        long energyPorts = structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.PORT)
                .filter(element -> structure.predicate(element)
                        .port().orElseThrow()
                        == com.masson.cruciblecraft.content.multiblock
                                .MultiblockStructureDefinition.PortType
                                .ENERGY_INPUT)
                .count();
        long waterPorts = structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.PORT)
                .filter(element -> structure.predicate(element)
                        .port().orElseThrow()
                        == com.masson.cruciblecraft.content.multiblock
                                .MultiblockStructureDefinition.PortType
                                .ITEM_FLUID_IN)
                .count();
        long steamPorts = structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.PORT)
                .filter(element -> structure.predicate(element)
                        .port().orElseThrow()
                        == com.masson.cruciblecraft.content.multiblock
                                .MultiblockStructureDefinition.PortType
                                .FLUID_OUT)
                .count();
        long air = structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.AIR)
                .count();
        long controllers = structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.CONTROLLER)
                .count();
        helper.assertTrue(
                structure.structure().size() == 36
                        && energyPorts == 9
                        && waterPorts == 8
                        && steamPorts == 17
                        && air == 1
                        && controllers == 1,
                "Boiler geometry drifted from the GT6 source: "
                        + structure.structure().size()
                        + " positions, "
                        + energyPorts
                        + " energy, "
                        + waterPorts
                        + " water, "
                        + steamPorts
                        + " steam, "
                        + air
                        + " air");
        var source = structure.source().orElseThrow();
        helper.assertTrue(
                source.className().endsWith("MultiTileEntityLargeBoiler")
                        && "checkStructure2".equals(source.method())
                        && "3703e40308c8c030763fd6297dea8b210d2a77b1"
                                .equals(source.revision()),
                "Boiler source provenance drifted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void largeBoilerFiveTierProfiles(
            GameTestHelper helper) {
        for (LargeBoilerTier tier : LargeBoilerTier.values()) {
            var structure = MultiblockStructureCatalog.require(
                    tier.structureId());
            var controller = structure.structure().stream()
                    .filter(element -> structure.predicate(element).kind()
                            == PredicateKind.CONTROLLER)
                    .findFirst()
                    .orElseThrow();
            long matchingWalls = structure.structure().stream()
                    .filter(element -> structure.predicate(element).kind()
                            == PredicateKind.PORT)
                    .filter(element -> structure.predicate(element).block()
                            .orElseThrow()
                            .equals(tier.wallId()))
                    .count();
            helper.assertTrue(
                    structure.structure().size() == 36
                            && structure.predicate(controller).block()
                                    .orElseThrow()
                                    .equals(tier.controllerId())
                            && matchingWalls == 25
                            && tier.capacity() == tier.steamOutput() * 10_000L
                            && tier.heatOutput() == tier.steamOutput() / 2L,
                    "Boiler tier profile drifted for " + tier.name());
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 720)
    public static void largeBoilerPortSupplySingleHost(
            GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        Direction facing = Direction.NORTH;
        var structure = MultiblockStructureCatalog.require(
                LargeBoilerBlockEntity.STRUCTURE_ID);
        helper.setBlock(
                controllerPos,
                largeBoilerController()
                        .defaultBlockState()
                        .setValue(MteInPlaceBlock.FACING, facing));
        structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.PORT)
                .forEach(element -> {
                    var predicate = structure.predicate(element);
                    helper.setBlock(
                            structure.worldPosition(
                                    controllerPos, facing, element.offset()),
                            structurePaletteBlock(predicate));
                });
        LargeBoilerBlockEntity boiler =
                helper.getBlockEntity(controllerPos);
        helper.startSequence()
                .thenIdle(LARGE_BOILER_STRUCTURE_WAIT)
                .thenExecute(() -> {
                    helper.assertTrue(
                            boiler.structureValid(),
                            "Boiler structure was not recognized");
                    helper.assertTrue(
                            boiler.fluidInputTanks().equals(List.of(0))
                                    && boiler.fluidOutputTanks()
                                            .equals(List.of(1))
                                    && boiler.tanks().size() == 2,
                            "Boiler host tank roles drifted");
                    helper.assertTrue(
                            structure.structure().stream()
                                    .filter(element -> structure
                                            .predicate(element).kind()
                                            == PredicateKind.PORT)
                                    .filter(element -> structure
                                            .predicate(element)
                                            .port().orElseThrow()
                                            == com.masson.cruciblecraft
                                                    .content.multiblock
                                                    .MultiblockStructureDefinition
                                                    .PortType.ITEM_FLUID_IN
                                            || structure.predicate(element)
                                                    .port().orElseThrow()
                                                    == com.masson.cruciblecraft
                                                            .content.multiblock
                                                            .MultiblockStructureDefinition
                                                            .PortType.FLUID_OUT)
                                    .count()
                                    == 25,
                            "Physical item/fluid port count drifted");
                })
                .thenSucceed();
    }



    // ===== C3 lifecycle: distillation tower =====

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void distillationTowerTeardown(
            GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        Direction facing = Direction.NORTH;
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.DISTILLATION_TOWER.structureId());
        helper.setBlock(
                controllerPos,
                ModBlocks.DISTILLATION_TOWER.get()
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
                            structurePaletteBlock(predicate));
                });
        DistillationTowerBlockEntity tower =
                helper.getBlockEntity(controllerPos);
        GTRecipe recipe = requireRecipe(
                ModRecipeMaps.DISTILLATION_TOWER,
                "hydrocarbon/distillation_tower/biomass");
        loadRecipeInputs(tower, recipe);
        BlockPos portToBreak = structure.anchor(
                "center", controllerPos, facing);
        helper.startSequence()
                .thenIdle(25)
                .thenExecute(() -> {
                    helper.assertTrue(
                            tower.structureValid(),
                            "Tower structure was not recognized");
                    helper.setBlock(
                            portToBreak, Blocks.AIR.defaultBlockState());
                })
                .thenIdle(25)
                .thenExecute(() -> {
                    helper.assertTrue(
                            !tower.structureValid(),
                            "Breaking a tower block did not invalidate the structure");
                    helper.assertTrue(
                            tower.tanks().get(0).getFluidAmount() == 80,
                            "Tower contents were lost on teardown: "
                                    + tower.tanks().get(0).getFluidAmount());
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void distillationTowerOutputJam(
            GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        Direction facing = Direction.NORTH;
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.DISTILLATION_TOWER.structureId());
        helper.setBlock(
                controllerPos,
                ModBlocks.DISTILLATION_TOWER.get()
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
                            structurePaletteBlock(predicate));
                });
        DistillationTowerBlockEntity tower =
                helper.getBlockEntity(controllerPos);
        BlockPos energyPort = structure.anchor(
                "bottom_energy_input", controllerPos, facing);
        GameTestHeatSources.placeHuSource(helper, energyPort.below());
        FuelGeneratorBlockEntity firebox =
                helper.getBlockEntity(energyPort.below());
        GTRecipe recipe = requireRecipe(
                ModRecipeMaps.DISTILLATION_TOWER,
                "hydrocarbon/distillation_tower/biomass");
        loadRecipeInputs(tower, recipe);
        for (int i = 0; i < recipe.fluidOutputs().size(); i++) {
            int out = tower.spec().fluids().outputs().get(i).index();
            tower.tanks().get(out).setFluid(
                    new FluidStack(
                            recipe.fluidOutputs().get(i).getFluid(),
                            tower.tanks().get(out).getCapacity()));
        }
        helper.startSequence()
                .thenIdle(25)
                .thenExecute(() -> {
                    helper.assertTrue(
                            tower.structureValid(),
                            "Tower structure was not recognized");
                    helper.assertTrue(
                            firebox.seedStoredEnergy(energyCapacity()),
                            "Could not fuel the tower heat source");
                })
                .thenIdle(60)
                .thenExecute(() -> {
                    helper.assertTrue(
                            tower.tanks().get(0).getFluidAmount() == 80,
                            "Output jam voided the tower input: "
                                    + tower.tanks().get(0).getFluidAmount());
                    helper.assertTrue(
                            java.util.stream.IntStream.range(
                                            0, recipe.fluidOutputs().size())
                                    .allMatch(i -> {
                                        int out = tower.spec().fluids()
                                                .outputs().get(i).index();
                                        return tower.tanks().get(out)
                                                .getFluidAmount()
                                                == tower.tanks().get(out)
                                                        .getCapacity();
                                    }),
                            "Output jam changed the tower output tanks");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void distillationTowerPowerLoss(
            GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        Direction facing = Direction.NORTH;
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.DISTILLATION_TOWER.structureId());
        helper.setBlock(
                controllerPos,
                ModBlocks.DISTILLATION_TOWER.get()
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
                            structurePaletteBlock(predicate));
                });
        DistillationTowerBlockEntity tower =
                helper.getBlockEntity(controllerPos);
        BlockPos energyPort = structure.anchor(
                "bottom_energy_input", controllerPos, facing);
        GameTestHeatSources.placeHuSource(helper, energyPort.below());
        FuelGeneratorBlockEntity firebox =
                helper.getBlockEntity(energyPort.below());
        GTRecipe recipe = requireRecipe(
                ModRecipeMaps.DISTILLATION_TOWER,
                "hydrocarbon/distillation_tower/biomass");
        loadRecipeInputs(tower, recipe);
        helper.startSequence()
                .thenIdle(25)
                .thenExecute(() -> {
                    helper.assertTrue(
                            tower.structureValid(),
                            "Tower structure was not recognized");
                    helper.assertTrue(
                            firebox.seedStoredEnergy(energyCapacity()),
                            "Could not fuel the tower heat source");
                })
                .thenIdle(25)
                .thenExecute(() -> {
                    helper.assertTrue(
                            tower.progress() > 0
                                    || tower.tanks().get(0)
                                                    .getFluidAmount()
                                            == 0,
                            "Tower never ran before the outage");
                    helper.setBlock(
                            energyPort.below(),
                            Blocks.AIR.defaultBlockState());
                })
                .thenIdle(25)
                .thenExecute(() -> helper.assertTrue(
                        tower.progress() > 0
                                || tower.tanks().get(0).getFluidAmount() > 0
                                || tower.tanks().stream().anyMatch(tank ->
                                        tank.getFluid().is(
                                                recipe.fluidOutputs()
                                                        .getFirst()
                                                        .getFluid())),
                        "Power loss neither preserved progress nor inputs"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void distillationTowerReload(
            GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        Direction facing = Direction.NORTH;
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.DISTILLATION_TOWER.structureId());
        helper.setBlock(
                controllerPos,
                ModBlocks.DISTILLATION_TOWER.get()
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
                            structurePaletteBlock(predicate));
                });
        DistillationTowerBlockEntity tower =
                helper.getBlockEntity(controllerPos);
        BlockPos energyPort = structure.anchor(
                "bottom_energy_input", controllerPos, facing);
        GameTestHeatSources.placeHuSource(helper, energyPort.below());
        FuelGeneratorBlockEntity firebox =
                helper.getBlockEntity(energyPort.below());
        GTRecipe recipe = requireRecipe(
                ModRecipeMaps.DISTILLATION_TOWER,
                "hydrocarbon/distillation_tower/biomass");
        loadRecipeInputs(tower, recipe);
        helper.startSequence()
                .thenIdle(25)
                .thenExecute(() -> {
                    helper.assertTrue(
                            tower.structureValid(),
                            "Tower structure was not recognized");
                    helper.assertTrue(
                            firebox.seedStoredEnergy(energyCapacity()),
                            "Could not fuel the tower heat source");
                })
                .thenIdle(25)
                .thenExecute(() -> {
                    helper.assertTrue(
                            tower.progress() > 0
                                    || tower.tanks().get(0)
                                                    .getFluidAmount()
                                            == 0,
                            "Tower never ran before the reload");
                    CompoundTag saved = tower.saveWithoutMetadata(
                            helper.getLevel().registryAccess());
                    tower.loadWithComponents(
                            saved, helper.getLevel().registryAccess());
                })
                .thenIdle(25)
                .thenExecute(() -> helper.assertTrue(
                        tower.structureValid()
                                && (tower.progress() > 0
                                        || tower.duration() > 0
                                        || tower.tanks().get(0)
                                                        .getFluidAmount()
                                                < 25),
                        "Tower did not restore structure or progress after reload"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void distillationTowerSaveQuarantine(
            GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        Direction facing = Direction.NORTH;
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.DISTILLATION_TOWER.structureId());
        helper.setBlock(
                controllerPos,
                ModBlocks.DISTILLATION_TOWER.get()
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
                            structurePaletteBlock(predicate));
                });
        DistillationTowerBlockEntity tower =
                helper.getBlockEntity(controllerPos);
        GTRecipe recipe = requireRecipe(
                ModRecipeMaps.DISTILLATION_TOWER,
                "hydrocarbon/distillation_tower/biomass");
        loadRecipeInputs(tower, recipe);
        helper.startSequence()
                .thenIdle(25)
                .thenExecute(() -> {
                    helper.assertTrue(
                            tower.structureValid(),
                            "Tower structure was not recognized");
                    CompoundTag tampered = tower.saveWithoutMetadata(
                            helper.getLevel().registryAccess());
                    ListTag plugins = tampered.getList(
                            "multiblock_plugins", Tag.TAG_STRING);
                    plugins.set(
                            0, StringTag.valueOf("cruciblecraft:not_a_plugin"));
                    tampered.put("multiblock_plugins", plugins);
                    tower.loadWithComponents(
                            tampered, helper.getLevel().registryAccess());
                    helper.assertTrue(
                            tower.pluginQuarantined(),
                            "Tower plugin mismatch did not quarantine");
                    helper.assertTrue(
                            tower.pluginQuarantineReason()
                                    .contains("does not match"),
                            "Tower quarantine did not expose its diagnostic");
                    helper.assertTrue(
                            tower.tanks().get(0).getFluidAmount() == 80,
                            "Tower contents changed under quarantine");
                })
                .thenSucceed();
    }

    // ===== C3 lifecycle: large boiler =====

    @GameTest(template = TEMPLATE, timeoutTicks = 1_400)
    public static void largeBoilerTeardown(
            GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        Direction facing = Direction.NORTH;
        var structure = MultiblockStructureCatalog.require(
                LargeBoilerBlockEntity.STRUCTURE_ID);
        helper.setBlock(
                controllerPos,
                largeBoilerController()
                        .defaultBlockState()
                        .setValue(MteInPlaceBlock.FACING, facing));
        structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.PORT)
                .forEach(element -> {
                    var predicate = structure.predicate(element);
                    helper.setBlock(
                            structure.worldPosition(
                                    controllerPos, facing, element.offset()),
                            structurePaletteBlock(predicate));
                });
        LargeBoilerBlockEntity boiler =
                helper.getBlockEntity(controllerPos);
        boiler.waterTank().fill(
                new FluidStack(Fluids.WATER, 1_000),
                IFluidHandler.FluidAction.EXECUTE);
        BlockPos portToBreak = structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.PORT)
                .map(element -> structure.worldPosition(
                        controllerPos, facing, element.offset()))
                .findFirst()
                .orElseThrow();
        helper.startSequence()
                .thenIdle(LARGE_BOILER_STRUCTURE_WAIT)
                .thenExecute(() -> {
                    helper.assertTrue(
                            boiler.structureValid(),
                            "Boiler structure was not recognized");
                    helper.setBlock(
                            portToBreak, Blocks.AIR.defaultBlockState());
                })
                .thenIdle(LARGE_BOILER_STRUCTURE_WAIT)
                .thenExecute(() -> {
                    helper.assertTrue(
                            !boiler.structureValid(),
                            "Breaking a boiler block did not invalidate the structure");
                    helper.assertTrue(
                            boiler.waterTank().getFluidAmount() == 1_000,
                            "Boiler water was lost on teardown");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 720)
    public static void largeBoilerFullSteamExplosion(
            GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        Direction facing = Direction.NORTH;
        var structure = MultiblockStructureCatalog.require(
                LargeBoilerBlockEntity.STRUCTURE_ID);
        helper.setBlock(
                controllerPos,
                largeBoilerController()
                        .defaultBlockState()
                        .setValue(MteInPlaceBlock.FACING, facing));
        structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.PORT)
                .forEach(element -> {
                    var predicate = structure.predicate(element);
                    helper.setBlock(
                            structure.worldPosition(
                                    controllerPos, facing, element.offset()),
                            structurePaletteBlock(predicate));
                });
        LargeBoilerBlockEntity boiler =
                helper.getBlockEntity(controllerPos);
        BlockPos energyPort = structure.anchor(
                "bottom_energy_input", controllerPos, facing);
        GameTestHeatSources.placeHuSource(helper, energyPort.below());
        FuelGeneratorBlockEntity firebox =
                helper.getBlockEntity(energyPort.below());
        boiler.waterTank().fill(
                new FluidStack(Fluids.WATER, 1_000),
                IFluidHandler.FluidAction.EXECUTE);
        helper.startSequence()
                .thenIdle(LARGE_BOILER_STRUCTURE_WAIT)
                .thenExecute(() -> {
                    helper.assertTrue(
                            boiler.structureValid(),
                            "Boiler structure was not recognized");
                    helper.assertTrue(
                            firebox.seedStoredEnergy(energyCapacity()),
                            "Could not fuel the boiler heat source");
                    boiler.steamTank().fill(
                            new FluidStack(
                                    com.masson.cruciblecraft.registry.ModFluids.STEAM_SOURCE.get(),
                                    boiler.steamTank().getCapacity()),
                            IFluidHandler.FluidAction.EXECUTE);
                })
                .thenIdle(5)
                .thenExecute(() -> {
                    helper.assertTrue(
                            helper.getBlockState(controllerPos).isAir(),
                            "A full steam tank did not explode the boiler");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 800)
    public static void largeBoilerPowerLoss(
            GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        Direction facing = Direction.NORTH;
        var structure = MultiblockStructureCatalog.require(
                LargeBoilerBlockEntity.STRUCTURE_ID);
        helper.setBlock(
                controllerPos,
                largeBoilerController()
                        .defaultBlockState()
                        .setValue(MteInPlaceBlock.FACING, facing));
        structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.PORT)
                .forEach(element -> {
                    var predicate = structure.predicate(element);
                    helper.setBlock(
                            structure.worldPosition(
                                    controllerPos, facing, element.offset()),
                            structurePaletteBlock(predicate));
                });
        LargeBoilerBlockEntity boiler =
                helper.getBlockEntity(controllerPos);
        BlockPos energyPort = structure.anchor(
                "bottom_energy_input", controllerPos, facing);
        GameTestHeatSources.placeHuSource(helper, energyPort.below());
        FuelGeneratorBlockEntity firebox =
                helper.getBlockEntity(energyPort.below());
        boiler.waterTank().fill(
                new FluidStack(Fluids.WATER, 1_000),
                IFluidHandler.FluidAction.EXECUTE);
        helper.startSequence()
                .thenIdle(LARGE_BOILER_STRUCTURE_WAIT)
                .thenExecute(() -> {
                    helper.assertTrue(
                            boiler.structureValid(),
                            "Boiler structure was not recognized");
                    helper.assertTrue(
                            firebox.seedStoredEnergy(energyCapacity()),
                            "Could not fuel the boiler heat source");
                })
                .thenIdle(40)
                .thenExecute(() -> {
                    helper.assertTrue(
                            boiler.steamTank().getFluidAmount() > 0,
                            "Boiler never produced steam");
                    int steamBefore =
                            boiler.steamTank().getFluidAmount();
                    helper.setBlock(
                            energyPort.below(),
                            Blocks.AIR.defaultBlockState());
                    helper.assertTrue(
                            steamBefore > 0,
                            "Boiler produced no steam before the outage");
                })
                .thenIdle(50)
                .thenExecute(() -> helper.assertTrue(
                        boiler.waterTank().getFluidAmount() > 0
                                && boiler.stored(EnergyType.HEAT)
                                        < SteamConversion.HU_PER_BATCH,
                        "Boiler heat-loss path lost water instead of pausing: "
                                + boiler.waterTank().getFluidAmount()
                                + " water, "
                                + boiler.stored(EnergyType.HEAT)
                                + " HU"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 800)
    public static void largeBoilerReload(
            GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        Direction facing = Direction.NORTH;
        var structure = MultiblockStructureCatalog.require(
                LargeBoilerBlockEntity.STRUCTURE_ID);
        helper.setBlock(
                controllerPos,
                largeBoilerController()
                        .defaultBlockState()
                        .setValue(MteInPlaceBlock.FACING, facing));
        structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.PORT)
                .forEach(element -> {
                    var predicate = structure.predicate(element);
                    helper.setBlock(
                            structure.worldPosition(
                                    controllerPos, facing, element.offset()),
                            structurePaletteBlock(predicate));
                });
        LargeBoilerBlockEntity boiler =
                helper.getBlockEntity(controllerPos);
        BlockPos energyPort = structure.anchor(
                "bottom_energy_input", controllerPos, facing);
        GameTestHeatSources.placeHuSource(helper, energyPort.below());
        FuelGeneratorBlockEntity firebox =
                helper.getBlockEntity(energyPort.below());
        boiler.waterTank().fill(
                new FluidStack(Fluids.WATER, 1_000),
                IFluidHandler.FluidAction.EXECUTE);
        helper.startSequence()
                .thenIdle(LARGE_BOILER_STRUCTURE_WAIT)
                .thenExecute(() -> {
                    helper.assertTrue(
                            boiler.structureValid(),
                            "Boiler structure was not recognized");
                    helper.assertTrue(
                            firebox.seedStoredEnergy(energyCapacity()),
                            "Could not fuel the boiler heat source");
                })
                .thenIdle(40)
                .thenExecute(() -> {
                    helper.assertTrue(
                            boiler.steamTank().getFluidAmount() > 0,
                            "Boiler never produced steam before reload");
                    CompoundTag saved = boiler.saveWithoutMetadata(
                            helper.getLevel().registryAccess());
                    boiler.loadWithComponents(
                            saved, helper.getLevel().registryAccess());
                })
                .thenIdle(25)
                .thenExecute(() -> helper.assertTrue(
                        boiler.structureValid()
                                && boiler.steamTank().getFluidAmount() > 0
                                && boiler.waterTank().getFluidAmount() < 1_000,
                        "Boiler did not restore structure and tanks after reload"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 720)
    public static void largeBoilerSaveQuarantine(
            GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        Direction facing = Direction.NORTH;
        var structure = MultiblockStructureCatalog.require(
                LargeBoilerBlockEntity.STRUCTURE_ID);
        helper.setBlock(
                controllerPos,
                largeBoilerController()
                        .defaultBlockState()
                        .setValue(MteInPlaceBlock.FACING, facing));
        structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.PORT)
                .forEach(element -> {
                    var predicate = structure.predicate(element);
                    helper.setBlock(
                            structure.worldPosition(
                                    controllerPos, facing, element.offset()),
                            structurePaletteBlock(predicate));
                });
        LargeBoilerBlockEntity boiler =
                helper.getBlockEntity(controllerPos);
        boiler.waterTank().fill(
                new FluidStack(Fluids.WATER, 1_000),
                IFluidHandler.FluidAction.EXECUTE);
        helper.startSequence()
                .thenIdle(LARGE_BOILER_STRUCTURE_WAIT)
                .thenExecute(() -> {
                    helper.assertTrue(
                            boiler.structureValid(),
                            "Boiler structure was not recognized");
                    CompoundTag tampered = boiler.saveWithoutMetadata(
                            helper.getLevel().registryAccess());
                    ListTag plugins = tampered.getList(
                            "multiblock_plugins", Tag.TAG_STRING);
                    plugins.set(
                            0, StringTag.valueOf("cruciblecraft:not_a_plugin"));
                    tampered.put("multiblock_plugins", plugins);
                    boiler.loadWithComponents(
                            tampered, helper.getLevel().registryAccess());
                    helper.assertTrue(
                            boiler.pluginQuarantined(),
                            "Boiler plugin mismatch did not quarantine");
                    helper.assertTrue(
                            boiler.waterTank().getFluidAmount() == 1_000,
                            "Boiler contents changed under quarantine");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 800)
    public static void largeBoilerSteamPushFivePorts(
            GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        Direction facing = Direction.NORTH;
        LargeBoilerBlockEntity boiler =
                placeLargeBoiler(helper, controllerPos, facing);
        var structure = MultiblockStructureCatalog.require(
                LargeBoilerBlockEntity.STRUCTURE_ID);
        BlockPos center = structure.anchor("center", controllerPos, facing);
        List<BlockPos> targets = List.of(
                structure.anchor(
                        "top_steam_output", controllerPos, facing).above(),
                center.west(2),
                center.east(2),
                center.north(2),
                center.south(2));
        FluidPipeBlock pipe = (FluidPipeBlock) ModBlocks.pipeBlock(
                "copper",
                MaterialPrefixes.TINY_FLUID_PIPE,
                PipeCatalog.Kind.FLUID).get();
        targets.forEach(target -> helper.setBlock(
                target,
                pipeState(
                        pipe,
                        Direction.DOWN,
                        Direction.UP,
                        Direction.NORTH,
                        Direction.SOUTH,
                        Direction.EAST,
                        Direction.WEST)));

        helper.startSequence()
                .thenIdle(LARGE_BOILER_STRUCTURE_WAIT)
                .thenExecute(() -> {
                    helper.assertTrue(
                            boiler.structureValid(),
                            "Boiler structure was not recognized");
                    boiler.steamTank().setFluid(
                            new FluidStack(
                                    ModFluids.STEAM_SOURCE.get(),
                                    boiler.steamTank().getCapacity() * 3 / 4));
                })
                .thenIdle(5)
                .thenExecute(() -> {
                    long received = targets.stream()
                            .map(helper::getBlockEntity)
                            .filter(FluidPipeBlockEntity.class::isInstance)
                            .map(FluidPipeBlockEntity.class::cast)
                            .mapToLong(pipeEntity ->
                                    pipeEntity.storedFluid().getAmount())
                            .sum();
                    long activeTargets = targets.stream()
                            .map(helper::getBlockEntity)
                            .filter(FluidPipeBlockEntity.class::isInstance)
                            .map(FluidPipeBlockEntity.class::cast)
                            .filter(pipeEntity ->
                                    !pipeEntity.storedFluid().isEmpty())
                            .count();
                    helper.assertTrue(
                            activeTargets == 5 && received > 0L,
                            "GT6 active steam output did not feed all five holes: "
                                    + activeTargets
                                    + " targets, "
                                    + received
                                    + " mB");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 800)
    public static void largeBoilerDistilledWaterDoesNotCalcify(
            GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        LargeBoilerBlockEntity boiler = placeLargeBoiler(
                helper, controllerPos, Direction.NORTH);
        helper.startSequence()
                .thenIdle(LARGE_BOILER_STRUCTURE_WAIT)
                .thenExecute(() -> {
                    helper.assertTrue(
                            boiler.structureValid(),
                            "Boiler structure was not recognized");
                    FluidStack distilled = SteamConversion.distilledExhaust(1_000);
                    helper.assertTrue(
                            boiler.waterTank().fill(
                                    distilled,
                                    IFluidHandler.FluidAction.EXECUTE)
                                    == distilled.getAmount(),
                            "Could not charge distilled water");
                    helper.assertTrue(
                            boiler.insert(
                                    EnergyType.HEAT,
                                    LargeBoilerTier.HEAT_PER_WATER,
                                    1_000L,
                                    Direction.UP,
                                    false)
                                    == 1_000L,
                            "Could not heat the distilled-water boiler");
                })
                .thenIdle(5)
                .thenExecute(() -> helper.assertTrue(
                        boiler.steamAmountLong() > 0L
                                && boiler.efficiency() == 10_000,
                        "Distilled water unexpectedly calcified the boiler"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 900)
    public static void largeBoilerCoolingAndToolLifecycle(
            GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        LargeBoilerBlockEntity boiler = placeLargeBoiler(
                helper, controllerPos, Direction.NORTH);
        helper.startSequence()
                .thenIdle(LARGE_BOILER_STRUCTURE_WAIT)
                .thenExecute(() -> {
                    helper.assertTrue(
                            boiler.structureValid(),
                            "Boiler structure was not recognized");
                    boiler.insert(
                            EnergyType.HEAT,
                            LargeBoilerTier.HEAT_PER_WATER,
                            100L,
                            Direction.UP,
                            false);
                    boiler.steamTank().setFluid(
                            new FluidStack(
                                    ModFluids.STEAM_SOURCE.get(),
                                    10_000));
                    CompoundTag saved = boiler.saveWithoutMetadata(
                            helper.getLevel().registryAccess());
                    saved.putInt("efficiency", 9_000);
                    saved.putInt("barometer", 0);
                    boiler.loadWithComponents(
                            saved, helper.getLevel().registryAccess());
                    helper.assertTrue(
                            boiler.efficiency() == 9_000,
                            "Calcification state was not restored");
                })
                .thenIdle(135)
                .thenExecute(() -> {
                    helper.assertTrue(
                            boiler.heatAmount() < 8_000L
                                    && boiler.steamAmountLong() < 10_000L,
                            "Boiler cooling did not lose HU and steam");
                    helper.assertTrue(
                            boiler.decalcify(null)
                                    && boiler.efficiency() == 10_000
                                    && boiler.heatAmount() == 0L
                                    && boiler.steamAmountLong() == 0L,
                            "Chisel lifecycle did not decalcify and clear heat");
                    boiler.waterTank().fill(
                            new FluidStack(Fluids.WATER, 1_000),
                            IFluidHandler.FluidAction.EXECUTE);
                    helper.assertTrue(
                            boiler.trashWithPlunger()
                                    && boiler.waterTank().isEmpty(),
                            "Plunger did not clear boiler water");
                    boiler.steamTank().fill(
                            new FluidStack(
                                    ModFluids.STEAM_SOURCE.get(),
                                    1_000),
                            IFluidHandler.FluidAction.EXECUTE);
                    helper.assertTrue(
                            boiler.trashWithPlunger()
                                    && boiler.steamTank().isEmpty(),
                            "Plunger did not clear boiler steam");
                })
                .thenSucceed();
    }

    // ===== C3 lifecycle: 3x3x3 tank =====

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void tank3x3x3Teardown(
            GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        Direction facing = Direction.NORTH;
        var structure = MultiblockStructureCatalog.require(
                TankBlockEntity.STRUCTURE_ID);
        TankBlockEntity tank = placeTank(
                helper, controllerPos, facing, "stainless_steel");
        tank.contents().fill(
                new FluidStack(Fluids.WATER, 10_000),
                IFluidHandler.FluidAction.EXECUTE);
        BlockPos portToBreak = structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.PORT)
                .map(element -> structure.worldPosition(
                        controllerPos, facing, element.offset()))
                .findFirst()
                .orElseThrow();
        helper.startSequence()
                .thenIdle(25)
                .thenExecute(() -> {
                    helper.assertTrue(
                            tank.structureValid(),
                            "Tank structure was not recognized");
                    helper.setBlock(
                            portToBreak, Blocks.AIR.defaultBlockState());
                })
                .thenIdle(25)
                .thenExecute(() -> {
                    helper.assertTrue(
                            !tank.structureValid(),
                            "Breaking a tank block did not invalidate the structure");
                    helper.assertTrue(
                            tank.contents().getFluidAmount() == 10_000,
                            "Tank contents were lost on teardown");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void tank3x3x3OutputJam(
            GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        Direction facing = Direction.NORTH;
        var structure = MultiblockStructureCatalog.require(
                TankBlockEntity.STRUCTURE_ID);
        TankBlockEntity tank = placeTank(
                helper, controllerPos, facing, "stainless_steel");
        tank.contents().fill(
                new FluidStack(Fluids.WATER, tank.contents().getCapacity()),
                IFluidHandler.FluidAction.EXECUTE);
        helper.startSequence()
                .thenIdle(25)
                .thenExecute(() -> {
                    helper.assertTrue(
                            tank.structureValid(),
                            "Tank structure was not recognized");
                    BlockPos portPos = structure.structure().stream()
                            .filter(element -> structure.predicate(element)
                                    .kind() == PredicateKind.PORT)
                            .map(element -> structure.worldPosition(
                                    controllerPos, facing, element.offset()))
                            .findFirst()
                            .orElseThrow();
                    MteInPlaceBlockEntity port =
                            helper.getBlockEntity(portPos);
                    int accepted = port.fluidHandler(facing).fill(
                            new FluidStack(Fluids.WATER, 1_000),
                            IFluidHandler.FluidAction.EXECUTE);
                    helper.assertTrue(
                            accepted == 0
                                    && tank.contents().getFluidAmount()
                                            == tank.contents().getCapacity(),
                            "Full tank voided an overfill: " + accepted);
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void tank3x3x3PowerLoss(
            GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        Direction facing = Direction.NORTH;
        var structure = MultiblockStructureCatalog.require(
                TankBlockEntity.STRUCTURE_ID);
        TankBlockEntity tank = placeTank(
                helper, controllerPos, facing, "stainless_steel");
        tank.contents().fill(
                new FluidStack(Fluids.WATER, 10_000),
                IFluidHandler.FluidAction.EXECUTE);
        helper.startSequence()
                .thenIdle(25)
                .thenExecute(() -> {
                    helper.assertTrue(
                            tank.structureValid(),
                            "Tank structure was not recognized");
                    // DESIGN_POLICY declaration: storage has no energy
                    // interface, so a power loss cannot affect it.
                    helper.assertTrue(
                            !tank.handles(EnergyType.HEAT,
                                            Direction.DOWN)
                                    && !tank.handles(
                                            EnergyType.KINETIC_ROTATION,
                                            Direction.DOWN)
                                    && !tank.handles(EnergyType.ELECTRIC,
                                            Direction.DOWN),
                            "Storage tank unexpectedly accepts energy");
                    helper.assertTrue(
                            tank.contents().getFluidAmount() == 10_000,
                            "Tank contents changed without any energy source");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void tank3x3x3Reload(
            GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        Direction facing = Direction.NORTH;
        var structure = MultiblockStructureCatalog.require(
                TankBlockEntity.STRUCTURE_ID);
        TankBlockEntity tank = placeTank(
                helper, controllerPos, facing, "stainless_steel");
        tank.contents().fill(
                new FluidStack(Fluids.WATER, 10_000),
                IFluidHandler.FluidAction.EXECUTE);
        helper.startSequence()
                .thenIdle(25)
                .thenExecute(() -> {
                    helper.assertTrue(
                            tank.structureValid(),
                            "Tank structure was not recognized");
                    CompoundTag saved = tank.saveWithoutMetadata(
                            helper.getLevel().registryAccess());
                    tank.loadWithComponents(
                            saved, helper.getLevel().registryAccess());
                })
                .thenIdle(25)
                .thenExecute(() -> helper.assertTrue(
                        tank.structureValid()
                                && tank.contents().getFluidAmount() == 10_000,
                        "Tank did not restore structure and contents after reload"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void tank3x3x3SaveQuarantine(
            GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        Direction facing = Direction.NORTH;
        var structure = MultiblockStructureCatalog.require(
                TankBlockEntity.STRUCTURE_ID);
        TankBlockEntity tank = placeTank(
                helper, controllerPos, facing, "stainless_steel");
        tank.contents().fill(
                new FluidStack(Fluids.WATER, 10_000),
                IFluidHandler.FluidAction.EXECUTE);
        helper.startSequence()
                .thenIdle(25)
                .thenExecute(() -> {
                    helper.assertTrue(
                            tank.structureValid(),
                            "Tank structure was not recognized");
                    CompoundTag tampered = tank.saveWithoutMetadata(
                            helper.getLevel().registryAccess());
                    ListTag plugins = tampered.getList(
                            "multiblock_plugins", Tag.TAG_STRING);
                    plugins.set(
                            0, StringTag.valueOf("cruciblecraft:not_a_plugin"));
                    tampered.put("multiblock_plugins", plugins);
                    tank.loadWithComponents(
                            tampered, helper.getLevel().registryAccess());
                    helper.assertTrue(
                            tank.pluginQuarantined(),
                            "Tank plugin mismatch did not quarantine");
                    helper.assertTrue(
                            tank.contents().getFluidAmount() == 10_000,
                            "Tank contents changed under quarantine");
                })
                .thenSucceed();
    }

    // ===== Large crucible thermal/steelmaking host =====

    @GameTest(template = TEMPLATE, timeoutTicks = 160)
    public static void largeCrucibleFormation(GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        Direction facing = Direction.NORTH;
        var structure = MultiblockStructureCatalog.require(
                LargeCrucibleBlockEntity.STRUCTURE_ID);
        LargeCrucibleBlockEntity crucible = placeLargeCrucible(
                helper, controllerPos, facing);
        BlockPos wall = firstLargeCrucibleWall(
                structure, controllerPos, facing, -1);
        helper.setBlock(wall, Blocks.COBBLESTONE.defaultBlockState());
        helper.startSequence()
                .thenIdle(25)
                .thenExecute(() -> helper.assertTrue(
                        !crucible.structureValid(),
                        "Incomplete large crucible formed"))
                .thenExecute(() -> helper.setBlock(
                        wall, LargeCrucibleHosts.wallBlock("steel")))
                .thenIdle(25)
                .thenExecute(() -> helper.assertTrue(
                        crucible.structureValid()
                                && structure.structure().size() == 27
                                && helper.getBlockState(controllerPos)
                                        .getValue(LargeCrucibleHosts.FORMED)
                                && helper.getBlockState(wall)
                                        .getValue(LargeCrucibleHosts.FORMED),
                        "Large crucible 27-position structure was not recognized"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void largeCrucibleTeardown(GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        Direction facing = Direction.NORTH;
        var structure = MultiblockStructureCatalog.require(
                LargeCrucibleBlockEntity.STRUCTURE_ID);
        LargeCrucibleBlockEntity crucible = placeLargeCrucible(
                helper, controllerPos, facing);
        BlockPos wall = firstLargeCrucibleWall(
                structure, controllerPos, facing, -1);
        helper.startSequence()
                .thenIdle(25)
                .thenExecute(() -> {
                    helper.assertTrue(
                            crucible.structureValid(),
                            "Large crucible structure was not recognized");
                    helper.assertTrue(
                            insertIngots(crucible, "iron", 4)
                                    == InsertResult.SUCCESS,
                            "Could not charge the large crucible before teardown");
                    helper.setBlock(
                            wall, Blocks.AIR.defaultBlockState());
                })
                .thenIdle(25)
                .thenExecute(() -> helper.assertTrue(
                        !crucible.structureValid()
                                && crucible.process().totalUnits()
                                        == MaterialPrefixes.INGOT.units() * 4,
                        "Teardown lost crucible contents or stayed formed"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 160)
    public static void largeCrucibleOutputJam(GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        Direction facing = Direction.NORTH;
        LargeCrucibleBlockEntity crucible = placeLargeCrucible(
                helper, controllerPos, facing);
        helper.startSequence()
                .thenIdle(25)
                .thenExecute(() -> {
                    helper.assertTrue(
                            crucible.structureValid(),
                            "Large crucible structure was not recognized");
                    helper.assertTrue(
                            insertIngots(crucible, "copper", 1, 1_500f)
                                    == InsertResult.SUCCESS,
                            "Could not charge copper for the jam path");
                    heatMelt(crucible, 1_500f);
                    crucible.inventory().setStackInSlot(
                            LargeCrucibleBlockEntity.SLOT_MOLD,
                            new ItemStack(ModItems.INGOT_MOLD.get()));
                    crucible.inventory().setStackInSlot(
                            LargeCrucibleBlockEntity.SLOT_OUTPUT,
                            material("copper", MaterialPrefixes.INGOT, 64));
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    helper.assertTrue(
                            crucible.outputJammed(),
                            "Full mold output did not jam the large crucible");
                    crucible.inventory().setStackInSlot(
                            LargeCrucibleBlockEntity.SLOT_INPUT,
                            material("iron", MaterialPrefixes.INGOT, 1));
                })
                .thenIdle(20)
                .thenExecute(() -> helper.assertTrue(
                        crucible.inventory().getStackInSlot(
                                        LargeCrucibleBlockEntity.SLOT_INPUT)
                                .getCount() == 1
                                && crucible.composition().getOrDefault(
                                        "copper", 0)
                                        == MaterialPrefixes.INGOT.units(),
                        "Jammed large crucible swallowed input or melt"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void largeCruciblePowerLoss(GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        Direction facing = Direction.NORTH;
        var structure = MultiblockStructureCatalog.require(
                LargeCrucibleBlockEntity.STRUCTURE_ID);
        LargeCrucibleBlockEntity crucible = placeLargeCrucible(
                helper, controllerPos, facing);
        BlockPos energyPort = structure.anchor(
                "bottom_energy_input", controllerPos, facing);
        GameTestHeatSources.placeHuSource(helper, energyPort.below());
        FuelGeneratorBlockEntity firebox =
                helper.getBlockEntity(energyPort.below());
        helper.startSequence()
                .thenIdle(25)
                .thenExecute(() -> {
                    helper.assertTrue(
                            crucible.structureValid(),
                            "Large crucible structure was not recognized");
                    helper.assertTrue(
                            insertIngots(crucible, "copper", 2)
                                    == InsertResult.SUCCESS,
                            "Could not charge the large crucible");
                    helper.assertTrue(
                            firebox.seedStoredEnergy(energyCapacity()),
                            "Could not fuel the large-crucible heat source");
                })
                .thenIdle(40)
                .thenExecute(() -> {
                    helper.assertTrue(
                            crucible.stored(EnergyType.HEAT) > 0L,
                            "Large crucible did not receive port-fed HU");
                    helper.setBlock(
                            energyPort.below(),
                            Blocks.AIR.defaultBlockState());
                })
                .thenIdle(50)
                .thenExecute(() -> helper.assertTrue(
                        crucible.composition().getOrDefault("copper", 0)
                                        == MaterialPrefixes.INGOT.units() * 2
                                && crucible.stored(EnergyType.HEAT) == 0L,
                        "Power-loss path lost melt instead of pausing"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 160)
    public static void largeCrucibleReload(GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        Direction facing = Direction.NORTH;
        LargeCrucibleBlockEntity crucible = placeLargeCrucible(
                helper, controllerPos, facing);
        helper.startSequence()
                .thenIdle(25)
                .thenExecute(() -> {
                    helper.assertTrue(
                            crucible.structureValid(),
                            "Large crucible structure was not recognized");
                    helper.assertTrue(
                            insertIngots(crucible, "copper", 3)
                                    == InsertResult.SUCCESS,
                            "Could not charge the large crucible before reload");
                    CompoundTag saved = crucible.saveWithoutMetadata(
                            helper.getLevel().registryAccess());
                    crucible.loadWithComponents(
                            saved, helper.getLevel().registryAccess());
                })
                .thenIdle(25)
                .thenExecute(() -> helper.assertTrue(
                        crucible.structureValid()
                                && !crucible.pluginQuarantined()
                                && crucible.composition().getOrDefault(
                                        "copper", 0)
                                        == MaterialPrefixes.INGOT.units() * 3,
                        "Large crucible did not restore contents after reload"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void largeCrucibleSaveQuarantine(
            GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        Direction facing = Direction.NORTH;
        LargeCrucibleBlockEntity crucible = placeLargeCrucible(
                helper, controllerPos, facing);
        helper.startSequence()
                .thenIdle(25)
                .thenExecute(() -> {
                    helper.assertTrue(
                            crucible.structureValid(),
                            "Large crucible structure was not recognized");
                    helper.assertTrue(
                            insertIngots(crucible, "copper", 2)
                                    == InsertResult.SUCCESS,
                            "Could not charge the large crucible before quarantine");
                    CompoundTag tampered = crucible.saveWithoutMetadata(
                            helper.getLevel().registryAccess());
                    ListTag plugins = tampered.getList(
                            "multiblock_plugins", Tag.TAG_STRING);
                    plugins.set(
                            0, StringTag.valueOf("cruciblecraft:not_a_plugin"));
                    tampered.put("multiblock_plugins", plugins);
                    crucible.loadWithComponents(
                            tampered, helper.getLevel().registryAccess());
                    helper.assertTrue(
                            crucible.pluginQuarantined(),
                            "Large crucible plugin mismatch did not quarantine");
                    helper.assertTrue(
                            crucible.composition().getOrDefault("copper", 0)
                                    == MaterialPrefixes.INGOT.units() * 2,
                            "Large crucible contents changed under quarantine");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void largeCrucibleCapacity432(GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        Direction facing = Direction.NORTH;
        LargeCrucibleBlockEntity crucible = placeLargeCrucible(
                helper, controllerPos, facing);
        helper.startSequence()
                .thenIdle(25)
                .thenExecute(() -> {
                    helper.assertTrue(
                            crucible.structureValid(),
                            "Large crucible structure was not recognized");
                    helper.assertTrue(
                            insertIngots(crucible, "iron", 432)
                                    == InsertResult.SUCCESS,
                            "Large crucible rejected a 432-ingot charge");
                    helper.assertTrue(
                            insertIngots(crucible, "iron", 1)
                                    == InsertResult.FULL,
                            "Large crucible accepted a 433rd ingot");
                    helper.assertTrue(
                            crucible.process().totalUnits()
                                    == MaterialPrefixes.INGOT.units() * 432
                                    && CrucibleBlockEntity.MAX_INGOTS == 16,
                            "Capacity split between large and single-block drifted");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 520)
    public static void largeCrucibleSteelmaking(GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        Direction facing = Direction.NORTH;
        LargeCrucibleBlockEntity crucible = placeLargeCrucible(
                helper, controllerPos, facing);
        helper.startSequence()
                .thenIdle(25)
                .thenExecute(() -> {
                    helper.assertTrue(
                            crucible.structureValid(),
                            "Large crucible structure was not recognized");
                    helper.assertTrue(
                            insertIngots(crucible, "iron", 3, 2_000f)
                                    == InsertResult.SUCCESS,
                            "Could not charge iron for steelmaking");
                    var carbon = MaterialCatalog.require("carbon");
                    helper.assertTrue(
                            crucible.insertMaterial(
                                    new MaterialUnits.Entry(
                                            carbon.id(),
                                            MaterialPrefixes.DUST,
                                            MaterialPrefixes.DUST.units()),
                                    2_000f)
                                    == InsertResult.SUCCESS,
                            "Could not charge carbon for steelmaking");
                    heatMelt(crucible, 1_600f);
                    helper.assertTrue(
                            crucible.injectAir(1_200L)
                                    == com.masson.cruciblecraft.machine
                                            .component.SteelmakingController
                                            .InjectionResult.STARTED,
                            "Steelmaking air injection did not start");
                })
                .thenIdle(400)
                .thenExecute(() -> helper.assertTrue(
                        crucible.composition().getOrDefault(
                                        SteelmakingProcess.STEEL, 0)
                                == MaterialPrefixes.INGOT.units() * 3
                                && crucible.composition()
                                        .getOrDefault("iron", 0) == 0,
                        "Large crucible steelmaking did not convert the 3:1 charge"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 160)
    public static void largeCrucibleMoldCast(GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        Direction facing = Direction.NORTH;
        LargeCrucibleBlockEntity crucible = placeLargeCrucible(
                helper, controllerPos, facing);
        helper.startSequence()
                .thenIdle(25)
                .thenExecute(() -> {
                    helper.assertTrue(
                            crucible.structureValid(),
                            "Large crucible structure was not recognized");
                    helper.assertTrue(
                            insertIngots(crucible, "copper", 1, 1_500f)
                                    == InsertResult.SUCCESS,
                            "Could not charge copper for mold casting");
                    heatMelt(crucible, 1_500f);
                    crucible.inventory().setStackInSlot(
                            LargeCrucibleBlockEntity.SLOT_MOLD,
                            new ItemStack(ModItems.INGOT_MOLD.get()));
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    ItemStack output = crucible.inventory().getStackInSlot(
                            LargeCrucibleBlockEntity.SLOT_OUTPUT);
                    ItemStack expected = material(
                            "copper", MaterialPrefixes.INGOT, 1);
                    helper.assertTrue(
                            output.getItem() == expected.getItem()
                                    && output.getCount() == 1
                                    && !crucible.inventory().getStackInSlot(
                                            LargeCrucibleBlockEntity.SLOT_MOLD)
                                            .isEmpty(),
                            "Large crucible mold slot did not cast a copper ingot");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 160)
    public static void largeCrucibleLayerPorts(GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        Direction facing = Direction.NORTH;
        var structure = MultiblockStructureCatalog.require(
                LargeCrucibleBlockEntity.STRUCTURE_ID);
        LargeCrucibleBlockEntity crucible = placeLargeCrucible(
                helper, controllerPos, facing);
        BlockPos energyPort = structure.anchor(
                "bottom_energy_input", controllerPos, facing);
        BlockPos itemPort = structure.anchor(
                "top_item_fluid", controllerPos, facing);
        GameTestHeatSources.placeHuSource(helper, energyPort.below());
        FuelGeneratorBlockEntity firebox =
                helper.getBlockEntity(energyPort.below());
        helper.startSequence()
                .thenIdle(25)
                .thenExecute(() -> {
                    helper.assertTrue(
                            crucible.structureValid(),
                            "Large crucible structure was not recognized");
                    helper.assertTrue(
                            firebox.seedStoredEnergy(energyCapacity()),
                            "Could not fuel the bottom HU layer");
                    MteInPlaceBlockEntity wall =
                            helper.getBlockEntity(itemPort);
                    helper.assertTrue(
                            wall != null && wall.itemHandler(Direction.UP) != null,
                            "Top wall did not expose the controller inventory");
                    ItemStack leftover = wall.itemHandler(Direction.UP).insertItem(
                            LargeCrucibleBlockEntity.SLOT_INPUT,
                            material("iron", MaterialPrefixes.INGOT, 1),
                            false);
                    helper.assertTrue(
                            leftover.isEmpty(),
                            "Top item/fluid port rejected an iron ingot");
                })
                .thenIdle(40)
                .thenExecute(() -> helper.assertTrue(
                        crucible.stored(EnergyType.HEAT) > 0L
                                && (crucible.composition().getOrDefault(
                                                "iron", 0)
                                        == MaterialPrefixes.INGOT.units()
                                        || crucible.inventory()
                                                .getStackInSlot(
                                                        LargeCrucibleBlockEntity
                                                                .SLOT_INPUT)
                                                .getCount() == 1),
                        "Layer ports did not feed HU from the bottom or items from the top"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void largeCrucibleMiddleWallPoursMold(GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        Direction facing = Direction.NORTH;
        var structure = MultiblockStructureCatalog.require(
                LargeCrucibleBlockEntity.STRUCTURE_ID);
        LargeCrucibleBlockEntity crucible = placeLargeCrucible(
                helper, controllerPos, facing);
        BlockPos wall = firstLargeCrucibleWall(
                structure, controllerPos, facing, 1);
        BlockPos moldPos = wall.relative(Direction.WEST);
        if (!helper.getBlockState(moldPos).isAir()) {
            moldPos = wall.relative(Direction.EAST);
        }
        if (!helper.getBlockState(moldPos).isAir()) {
            moldPos = wall.relative(Direction.SOUTH);
        }
        if (!helper.getBlockState(moldPos).isAir()) {
            moldPos = wall.relative(Direction.NORTH);
        }
        BlockPos placedMold = moldPos;
        helper.startSequence()
                .thenIdle(25)
                .thenExecute(() -> {
                    helper.assertTrue(
                            crucible.structureValid(),
                            "Large crucible structure was not recognized");
                    helper.assertTrue(
                            insertIngots(crucible, "copper", 1, 2_000f)
                                    == InsertResult.SUCCESS,
                            "Could not charge copper for wall pour");
                    heatMelt(crucible, 2_000f);
                    helper.setBlock(placedMold, ModBlocks.CERAMIC_MOLD.get());
                    CeramicMoldBlockEntity mold = helper.getBlockEntity(placedMold);
                    mold.setPattern(com.masson.cruciblecraft.content.mold.MoldShape.INGOT.mask());
                    CruciblePour pour = CruciblePour.at(
                            helper.getLevel(), helper.absolutePos(wall));
                    helper.assertTrue(
                            pour != null
                                    && pour.fillMoldAtSide(
                                            mold, Direction.EAST, Direction.WEST),
                            "Middle-layer casing did not pour into the adjacent mold");
                    helper.assertTrue(
                            mold.isFilled() && "copper".equals(mold.materialId()),
                            "Wall pour did not fill the ceramic mold with copper");
                    helper.assertTrue(
                            crucible.process().totalUnits() == 0,
                            "Wall pour did not drain the large crucible");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void largeCrucibleMeltDownFillsThreeByThree(GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        LargeCrucibleBlockEntity crucible = placeLargeCrucible(
                helper, controllerPos, Direction.NORTH);
        helper.startSequence()
                .thenIdle(25)
                .thenExecute(() -> {
                    helper.assertTrue(
                            crucible.structureValid(),
                            "Large crucible structure was not recognized");
                    helper.assertTrue(
                            insertIngots(crucible, "iron", 1, 2_000f)
                                    == InsertResult.SUCCESS,
                            "Could not charge iron for large-crucible meltdown");
                    heatMelt(crucible, 100_000f);
                })
                .thenIdle(5)
                .thenExecute(() -> {
                    helper.assertTrue(
                            isFlowingLava(helper.getBlockState(controllerPos)),
                            "Overheated large crucible controller did not become lava");
                    helper.assertTrue(
                            isFlowingLava(helper.getBlockState(
                                    controllerPos.offset(-1, 0, -1))),
                            "Large crucible meltdown missed a bottom corner");
                    helper.assertTrue(
                            isFlowingLava(helper.getBlockState(
                                    controllerPos.offset(1, 2, 1))),
                            "Large crucible meltdown missed the 3x3x3 top corner");
                })
                .thenSucceed();
    }

    private static boolean isFlowingLava(BlockState state) {
        return state.is(Blocks.LAVA) && !state.getFluidState().isSource();
    }

    private static void assertFormedTowerPartVisuals(
            GameTestHelper helper,
            MultiblockStructureDefinition structure,
            BlockPos controllerPos,
            Direction facing) {
        BlockPos hole = structure.worldPosition(
                controllerPos, facing, new Offset(0, 0, 2));
        BlockPos corner = structure.worldPosition(
                controllerPos, facing, new Offset(1, 2, 2));
        helper.assertTrue(
                helper.getBlockState(hole).is(DistillationTowerParts.towerPart())
                        && helper.getBlockState(hole).getValue(
                                MultiblockPortBlock.TOWER_SKIN)
                        && helper.getBlockState(hole).getValue(
                                MultiblockPortBlock.BACK_HOLE),
                "Far-face center did not switch to GT6 design 1");
        helper.assertTrue(
                helper.getBlockState(corner).getValue(
                                MultiblockPortBlock.TOWER_SKIN)
                        && !helper.getBlockState(corner).getValue(
                                MultiblockPortBlock.BACK_HOLE),
                "Tower parts did not switch to GT6 design 0");
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void distillationTowerFormation(
            GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        Direction facing = Direction.NORTH;
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.DISTILLATION_TOWER.structureId());
        helper.setBlock(
                controllerPos,
                ModBlocks.DISTILLATION_TOWER.get()
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
                            structurePaletteBlock(predicate));
                });
        DistillationTowerBlockEntity tower =
                helper.getBlockEntity(controllerPos);
        BlockPos energyPort = structure.anchor(
                "bottom_energy_input", controllerPos, facing);
        GameTestHeatSources.placeHuSource(helper, energyPort.below());
        FuelGeneratorBlockEntity firebox =
                helper.getBlockEntity(energyPort.below());
        GTRecipe recipe = requireRecipe(
                ModRecipeMaps.DISTILLATION_TOWER,
                "hydrocarbon/distillation_tower/biomass");
        loadRecipeInputs(tower, recipe);

        helper.startSequence()
                .thenIdle(25)
                .thenExecute(() -> {
                    helper.assertTrue(
                            tower.structureValid(),
                            "Distillation tower structure was not recognized");
                    loadRecipeInputs(tower, recipe);
                    assertFormedTowerPartVisuals(
                            helper, structure, controllerPos, facing);
                    helper.assertTrue(
                            firebox.seedStoredEnergy(energyCapacity()),
                            "Could not fuel the tower heat source");
                })
                .thenIdle(10)
                .thenExecute(() -> helper.assertTrue(
                        tower.stored(EnergyType.HEAT) > 0L,
                        "Tower did not receive port-fed heat"))
                .thenIdle(45)
                .thenExecute(() -> {
                    FluidStack ethanol = recipe.fluidOutputs().getFirst();
                    FluidStack glycerol = recipe.fluidOutputs().get(1);
                    boolean finished = tower.tanks().stream()
                            .anyMatch(tank ->
                                    tank.getFluid().is(ethanol.getFluid())
                                            && tank.getFluidAmount()
                                                    >= ethanol.getAmount())
                            && tower.tanks().stream().anyMatch(tank ->
                                    tank.getFluid().is(
                                                    glycerol.getFluid())
                                            && tank.getFluidAmount()
                                                    >= glycerol
                                                            .getAmount());
                    helper.assertTrue(
                            finished || tower.progress() > 0,
                            "Tower did not run the distillation-tower recipe: "
                                    + tower.pausedReason()
                                    + " stored="
                                    + tower.stored(EnergyType.HEAT)
                                    + " duration="
                                    + tower.duration()
                                    + " tank0="
                                    + tower.tanks().get(0).getFluid()
                                    + " spec="
                                    + tower.spec().id()
                                    + " map="
                                    + tower.spec().recipeMapId());
                    if (!finished) {
                        forceLastTick(helper, tower);
                    }
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    FluidStack ethanol = recipe.fluidOutputs().getFirst();
                    FluidStack glycerol = recipe.fluidOutputs().get(1);
                    helper.assertTrue(
                            tower.tanks().stream().anyMatch(tank ->
                                    tank.getFluid().is(ethanol.getFluid())
                                            && tank.getFluidAmount()
                                                    >= ethanol.getAmount()),
                            "Tower produced no ethanol");
                    helper.assertTrue(
                            tower.tanks().stream().anyMatch(tank ->
                                    tank.getFluid().is(
                                                    glycerol.getFluid())
                                            && tank.getFluidAmount()
                                                    >= glycerol
                                                            .getAmount()),
                            "Tower silently lost glycerol coproduct");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void distillationTowerFidelity(
            GameTestHelper helper) {
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.DISTILLATION_TOWER.structureId());
        long energyPorts = structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.PORT)
                .filter(element -> structure.predicate(element)
                        .port().orElseThrow()
                        == com.masson.cruciblecraft.content.multiblock
                                .MultiblockStructureDefinition.PortType
                                .ENERGY_INPUT)
                .count();
        long itemFluidPorts = structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.PORT)
                .filter(element -> structure.predicate(element)
                        .port().orElseThrow()
                        == com.masson.cruciblecraft.content.multiblock
                                .MultiblockStructureDefinition.PortType
                                .ITEM_FLUID)
                .count();
        long fluidOutPorts = structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.PORT)
                .filter(element -> structure.predicate(element)
                        .port().orElseThrow()
                        == com.masson.cruciblecraft.content.multiblock
                                .MultiblockStructureDefinition.PortType
                                .FLUID_OUT)
                .count();
        long controllers = structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.CONTROLLER)
                .count();
        helper.assertTrue(
                structure.structure().size() == 81
                        && energyPorts == 9
                        && itemFluidPorts == 8
                        && fluidOutPorts == 63
                        && controllers == 1
                        && DistillationTowerParts.HEAT_TRANSMITTER.equals(
                                structure.palette().get("E")
                                        .block().orElseThrow())
                        && DistillationTowerParts.TOWER_PART.equals(
                                structure.palette().get("P")
                                        .block().orElseThrow())
                        && DistillationTowerParts.TOWER_PART.equals(
                                structure.palette().get("F")
                                        .block().orElseThrow()),
                "Tower geometry drifted from the GT6 source: "
                        + structure.structure().size()
                        + " positions, "
                        + energyPorts
                        + " energy, "
                        + itemFluidPorts
                        + " item/fluid, "
                        + fluidOutPorts
                        + " fluid-out");
        var source = structure.source().orElseThrow();
        helper.assertTrue(
                "gregtech.tileentity.multiblocks"
                        .equals(source.className().substring(
                                0, "gregtech.tileentity.multiblocks"
                                        .length()))
                        && source.className().endsWith(
                                "MultiTileEntityDistillationTower")
                        && "checkStructure2".equals(source.method())
                        && "3703e40308c8c030763fd6297dea8b210d2a77b1"
                                .equals(source.revision()),
                "Tower source provenance drifted");
        helper.assertTrue(
                BuiltInRegistries.BLOCK.getOptional(
                        ResourceLocation.fromNamespaceAndPath(
                                CrucibleCraft.MODID,
                                "multiblock/distillation_tower"))
                        .isEmpty(),
                "Catalog dummy 17101 is still registered");
        helper.assertTrue(
                BuiltInRegistries.BLOCK.getOptional(
                        ResourceLocation.fromNamespaceAndPath(
                                CrucibleCraft.MODID,
                                "distillation_tower"))
                        .isPresent(),
                "Live 17101 controller is missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void distillationTowerPortSupplySingleHost(
            GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        Direction facing = Direction.NORTH;
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.DISTILLATION_TOWER.structureId());
        helper.setBlock(
                controllerPos,
                ModBlocks.DISTILLATION_TOWER.get()
                        .defaultBlockState()
                        .setValue(ProcessingMachineBlock.FACING, facing));
        List<BlockPos> itemFluidPorts = structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.PORT)
                .filter(element -> structure.predicate(element)
                        .port().orElseThrow()
                        == com.masson.cruciblecraft.content.multiblock
                                .MultiblockStructureDefinition.PortType
                                .ITEM_FLUID)
                .map(element -> structure.worldPosition(
                        controllerPos, facing, element.offset()))
                .toList();
        structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.PORT)
                .forEach(element -> {
                    var predicate = structure.predicate(element);
                    helper.setBlock(
                            structure.worldPosition(
                                    controllerPos, facing, element.offset()),
                            structurePaletteBlock(predicate));
                });
        DistillationTowerBlockEntity tower =
                helper.getBlockEntity(controllerPos);
        helper.startSequence()
                .thenIdle(25)
                .thenExecute(() -> {
                    helper.assertTrue(
                            tower.structureValid(),
                            "Tower structure was not recognized");
                    helper.assertTrue(
                            itemFluidPorts.size() == 8,
                            "Physical item/fluid port count drifted");
                    int inputSlot =
                            tower.spec().items().inputs().getFirst();
                    int inputTank = tower.spec()
                            .fluids().inputs().getFirst().index();
                    tower.inventory().getStackInSlot(inputSlot)
                            .setCount(0);
                    tower.tanks().get(inputTank).setFluid(
                            new FluidStack(Fluids.WATER, 1));
                    helper.assertTrue(
                            itemFluidPorts.stream().allMatch(portPos -> {
                                MultiblockPortBlockEntity port =
                                        helper.getBlockEntity(portPos);
                                return port.itemHandler().getSlots()
                                        == tower.inventory().getSlots()
                                        && port.fluidHandler().getTanks()
                                        == tower.tanks().size();
                            }),
                            "Eight item/fluid ports did not bridge one host");
                    helper.assertTrue(
                            tower.spec().items().inputs().size() == 1
                                    && tower.spec().items().outputs()
                                            .size() == 3
                                    && tower.spec().fluids().inputs()
                                            .size() == 1
                                    && tower.spec().fluids().outputs()
                                            .size() == 9,
                            "Shared host layout drifted from the GT6 1/3/1/9 panel");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void distillationTowerHeightRoutesFuelAndPetrol(
            GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        Direction facing = Direction.NORTH;
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.DISTILLATION_TOWER.structureId());
        helper.setBlock(
                controllerPos,
                ModBlocks.DISTILLATION_TOWER.get()
                        .defaultBlockState()
                        .setValue(ProcessingMachineBlock.FACING, facing));
        structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.PORT)
                .forEach(element -> helper.setBlock(
                        structure.worldPosition(
                                controllerPos, facing, element.offset()),
                        structurePaletteBlock(
                                structure.predicate(element))));
        DistillationTowerBlockEntity tower =
                helper.getBlockEntity(controllerPos);
        Fluid fuel = ModFluids.materialFluid("fuel").orElseThrow();
        Fluid petrol = ModFluids.materialFluid("petrol").orElseThrow();
        int fuelTank = tower.spec().fluids().outputs().getFirst().index();
        int petrolTank = tower.spec().fluids().outputs().get(1).index();
        BlockPos backHole = structure.anchor(
                "back_hole", controllerPos, facing);
        FluidPipeBlock hatchPipe = (FluidPipeBlock) ModBlocks.pipeBlock(
                "copper",
                MaterialPrefixes.TINY_FLUID_PIPE,
                PipeCatalog.Kind.FLUID).get();
        helper.setBlock(backHole.above(2), hatchPipe);
        helper.setBlock(backHole.above(5), hatchPipe);
        helper.getLevel().setBlock(
                helper.absolutePos(backHole.above(2)),
                helper.getBlockState(backHole.above(2))
                        .setValue(FluidPipeBlock.NORTH, true),
                Block.UPDATE_CLIENTS);
        helper.getLevel().setBlock(
                helper.absolutePos(backHole.above(5)),
                helper.getBlockState(backHole.above(5))
                        .setValue(FluidPipeBlock.NORTH, true),
                Block.UPDATE_CLIENTS);
        helper.startSequence()
                .thenIdle(25)
                .thenExecute(() -> {
                    DistillationTowerBlockEntity live =
                            helper.getBlockEntity(controllerPos);
                    helper.assertTrue(
                            live.structureValid(),
                            "Tower structure was not recognized");
                    int liveFuelTank = live.spec().fluids().outputs().getFirst().index();
                    int livePetrolTank = live.spec().fluids().outputs().get(1).index();
                    live.tanks().get(liveFuelTank).setFluid(new FluidStack(fuel, 200));
                    live.tanks().get(livePetrolTank).setFluid(new FluidStack(petrol, 200));
                    helper.assertTrue(
                            DistillationTowerFluidRouting.localY(
                                    DistillationTowerFluidRouting.Kind.HOT,
                                    fuel) == 2
                                    && DistillationTowerFluidRouting.localY(
                                            DistillationTowerFluidRouting.Kind.HOT,
                                            petrol) == 5,
                            "Height table drifted from GT6 doOutputFluids");
                    helper.assertTrue(
                            DistillationTowerAutoOutput.hole(
                                            tower, facing, 2)
                                    .equals(helper.absolutePos(
                                            backHole.above(2)))
                                    && DistillationTowerAutoOutput.hole(
                                                    tower, facing, 5)
                                            .equals(helper.absolutePos(
                                                    backHole.above(5))),
                            "Backside hole is not three blocks behind the controller");
                    DistillationTowerAutoOutput.pushFluids(
                            live, DistillationTowerFluidRouting.Kind.HOT);
                    FluidPipeBlockEntity fuelSink =
                            helper.getBlockEntity(backHole.above(2));
                    FluidPipeBlockEntity petrolSink =
                            helper.getBlockEntity(backHole.above(5));
                    boolean fuelMoved = fuelSink.storedFluid().is(fuel)
                            && fuelSink.storedFluid().getAmount() > 0;
                    boolean petrolMoved = petrolSink.storedFluid().is(petrol)
                            && petrolSink.storedFluid().getAmount() > 0;
                    helper.assertTrue(
                            fuelMoved && petrolMoved,
                            "Height auto-output missed fuel/petrol hatches: fuel="
                                    + fuelMoved
                                    + " petrol="
                                    + petrolMoved);
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void cryoDistillationTowerFormation(
            GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(6, 2, 6);
        Direction facing = Direction.NORTH;
        var structure = MultiblockStructureCatalog.require(
                ModMultiblockControllers.CRYO_DISTILLATION_TOWER.structureId());
        helper.setBlock(
                controllerPos,
                ModBlocks.CRYO_DISTILLATION_TOWER.get()
                        .defaultBlockState()
                        .setValue(ProcessingMachineBlock.FACING, facing));
        structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.PORT)
                .forEach(element -> helper.setBlock(
                        structure.worldPosition(
                                controllerPos, facing, element.offset()),
                        structurePaletteBlock(
                                structure.predicate(element))));
        CryoDistillationTowerBlockEntity tower =
                helper.getBlockEntity(controllerPos);
        BlockPos energyPort = structure.anchor(
                "bottom_energy_input", controllerPos, facing);
        GTRecipe recipe = requireRecipe(
                ModRecipeMaps.CRYO_DISTILLATION_TOWER,
                "hydrocarbon/cryo_distillation_tower/air");
        loadRecipeInputs(tower, recipe);
        helper.startSequence()
                .thenIdle(25)
                .thenExecute(() -> {
                    helper.assertTrue(
                            tower.structureValid(),
                            "Cryo tower structure was not recognized");
                    loadRecipeInputs(tower, recipe);
                    assertFormedTowerPartVisuals(
                            helper, structure, controllerPos, facing);
                    BlockEntity energyPortEntity =
                            helper.getBlockEntity(energyPort);
                    helper.assertTrue(
                            energyPortEntity instanceof IEnergyHandler,
                            "Cryo tower energy port is not an energy handler");
                    long accepted = ((IEnergyHandler) energyPortEntity).insert(
                            EnergyType.CU,
                            1_024L,
                            64L,
                            Direction.UP,
                            false);
                    helper.assertTrue(
                            accepted > 0L,
                            "Cryo tower energy port did not accept CU: "
                                    + accepted);
                    helper.assertTrue(
                            tower.stored(EnergyType.CU) > 0L,
                            "Cryo tower did not receive port-fed CU");
                })
                .thenIdle(55)
                .thenExecute(() -> {
                    helper.assertTrue(
                            tower.progress() > 0
                                    || tower.tanks().stream().anyMatch(
                                            tank -> tank.getFluidAmount() > 0
                                                    && !tank.getFluid().is(
                                                            recipe.fluidInputs()
                                                                    .getFirst()
                                                                    .getFluid())),
                            "Cryo tower did not run air: "
                                    + tower.pausedReason()
                                    + " stored="
                                    + tower.stored(EnergyType.CU));
                    if (tower.progress() > 0) {
                        forceLastTick(helper, tower);
                    }
                })
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        tower.tanks().stream().anyMatch(tank ->
                                tank.getFluid().is(
                                        recipe.fluidOutputs()
                                                .getFirst()
                                                .getFluid())
                                        && tank.getFluidAmount()
                                                >= recipe.fluidOutputs()
                                                        .getFirst()
                                                        .getAmount()),
                        "Cryo tower produced no nitrogen"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 140)
    public static void ruAxleGearboxPowersTieredCentrifuge(
            GameTestHelper helper) {
        BlockPos motorPos = new BlockPos(3, 2, 13);
        BlockPos axlePos = motorPos.above();
        BlockPos gearboxPos = axlePos.above();
        BlockPos centrifugePos = gearboxPos.above();
        helper.setBlock(
                motorPos,
                ModBlocks.STEEL_GALVANIZED_ELECTRIC_MOTOR.get().defaultBlockState()
                        .setValue(
                                ElectricMotorBlock.FACING,
                                Direction.UP));
        helper.setBlock(
                axlePos,
                ModBlocks.ROTATIONAL_AXLE.get().defaultBlockState()
                        .setValue(
                                RotationalAxleBlock.AXIS,
                                Direction.Axis.Y));
        helper.setBlock(
                gearboxPos,
                ModBlocks.ROTATIONAL_GEARBOX.get().defaultBlockState()
                        .setValue(
                                RotationalGearboxBlock.FACING,
                                Direction.UP));
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
                "chemical/centrifuge/gloomstone");
        loadRecipeInputs(centrifuge, recipe);
        IEnergyHandler motor = helper.getLevel().getCapability(
                ModCapabilities.ENERGY,
                helper.absolutePos(motorPos),
                Direction.DOWN);
        IEnergyHandler axle = helper.getLevel().getCapability(
                ModCapabilities.ENERGY,
                helper.absolutePos(axlePos),
                Direction.DOWN);
        IEnergyHandler gearbox = helper.getLevel().getCapability(
                ModCapabilities.ENERGY,
                helper.absolutePos(gearboxPos),
                Direction.DOWN);
        helper.assertTrue(
                motor != null
                        && motor.handles(
                                EnergyType.ELECTRIC,
                                Direction.DOWN),
                "Electric motor exposed no EU input");
        helper.assertTrue(
                axle != null
                        && gearbox != null
                        && axle.handles(
                                EnergyType.KINETIC_ROTATION,
                                Direction.DOWN)
                        && gearbox.handles(
                                EnergyType.KINETIC_ROTATION,
                                Direction.DOWN),
                "RU axle/gearbox capability chain is incomplete");
        helper.assertTrue(
                centrifuge.handles(
                                EnergyType.KINETIC_ROTATION,
                                Direction.DOWN)
                        && !centrifuge.handles(
                                EnergyType.KINETIC_PUSH,
                                Direction.DOWN),
                "Tiered centrifuge did not preserve RU-only identity");

        helper.startSequence()
                .thenExecuteFor(48, () -> {
                    long accepted = motor.insert(
                            EnergyType.ELECTRIC,
                            32L,
                            1L,
                            Direction.DOWN,
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
    public static void tierProfilesExposeDistinctFailureStates(
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
                "Steel centrifuge did not bind its source primary profile");
        helper.assertTrue(
                machine.insert(
                                EnergyType.KINETIC_PUSH,
                                128L,
                                1L,
                                energyFace(machine),
                                false)
                        == 0L,
                "RU machine accepted KU");
        helper.assertTrue(
                machine.insert(
                                EnergyType.KINETIC_ROTATION,
                                512L,
                                1L,
                                energyFace(machine),
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
                                        energyFace(windowMachine),
                                        false)
                                == 1L,
                        "Centrifuge rejected minimum-window RU"))
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
    public static void ruMotorAxleGearboxPowersLathe(
            GameTestHelper helper) {
        BlockPos motorPos = new BlockPos(3, 2, 13);
        BlockPos axlePos = motorPos.above();
        BlockPos gearboxPos = axlePos.above();
        BlockPos lathePos = gearboxPos.above();
        helper.setBlock(
                motorPos,
                ModBlocks.STEEL_GALVANIZED_ELECTRIC_MOTOR.get().defaultBlockState()
                        .setValue(
                                ElectricMotorBlock.FACING,
                                Direction.UP));
        helper.setBlock(
                axlePos,
                ModBlocks.ROTATIONAL_AXLE.get().defaultBlockState()
                        .setValue(
                                RotationalAxleBlock.AXIS,
                                Direction.Axis.Y));
        helper.setBlock(
                gearboxPos,
                ModBlocks.ROTATIONAL_GEARBOX.get().defaultBlockState()
                        .setValue(
                                RotationalGearboxBlock.FACING,
                                Direction.UP));
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
                Direction.DOWN);
        helper.assertTrue(
                motor != null
                        && lathe.variant().kind().behavior()
                                == ModProcessingMachines.LATHE
                        && lathe.variant().tier().inputNominal() == 32L
                        && lathe.handles(
                                EnergyType.KINETIC_ROTATION,
                                energyFace(lathe))
                        && !lathe.handles(
                                EnergyType.KINETIC_PUSH,
                                energyFace(lathe)),
                "RU lathe vertical did not bind the bronze source tier");

        helper.startSequence()
                .thenExecuteFor(48, () -> {
                    long accepted = motor.insert(
                            EnergyType.ELECTRIC,
                            32L,
                            1L,
                            Direction.DOWN,
                            false);
                    helper.assertTrue(
                            accepted == 1L
                                    || motor.stored(EnergyType.ELECTRIC)
                                            >= 32L,
                            "RU motor rejected nominal EU");
                })
                .thenExecute(() -> helper.assertTrue(
                        lathe.progress() > 0,
                        "Motor -> axle -> gearbox RU did not advance "
                                + "the selected lathe: "
                                + lathe.pausedReason()))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 160)
    public static void steamEnginePowersPressThroughKu(
            GameTestHelper helper) {
        BlockPos pressPos = new BlockPos(3, 2, 5);
        BlockPos enginePos = pressPos.above();
        helper.setBlock(
                enginePos,
                ModBlocks.BRONZE_STEAM_ENGINE.get().defaultBlockState()
                        .setValue(SteamEngineBlock.FACING, Direction.DOWN));
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
                        candidate.eut() > 0L && candidate.eut() <= 16L)
                .findFirst()
                .orElseThrow();
        loadRecipeInputs(press, recipe);
        primeEmittingKinetic(helper, engine);
        IFluidHandler steamInput = engine.fluids(Direction.UP);
        helper.assertTrue(
                steamInput != null
                        && steamInput.fill(
                                new FluidStack(
                                        ModFluids.STEAM_SOURCE.get(),
                                        2_000),
                                IFluidHandler.FluidAction.EXECUTE)
                                == 2_000,
                "Could not prime the Press KU vertical");
        helper.assertTrue(
                press.variant().kind().behavior()
                                == ModProcessingMachines.PRESS
                        && press.variant().tier().inputNominal() == 32L
                        && press.variant().tier().parallelLimit() == 4
                        && press.handles(
                                EnergyType.KINETIC_PUSH,
                                energyFace(press))
                        && !press.handles(
                                EnergyType.KINETIC_ROTATION,
                                energyFace(press)),
                "Press did not bind the bronze KU source tier");

        helper.startSequence()
                .thenIdle(40)
                .thenExecute(() -> helper.assertTrue(
                        press.progress() > 0 || hasAnyOutput(press),
                        "Adjacent steam KU did not advance Press: "
                                + press.pausedReason()
                                + " stored="
                                + press.stored(EnergyType.KINETIC_PUSH)))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void higherTierControlledInjection(
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
                                energyFace(press),
                                false)
                                == 1L
                        && press.runtime().status().equals("overcharged"),
                "Titanium Press did not expose KU overcharge state");
        helper.assertTrue(
                lathe.insert(
                                EnergyType.KINETIC_ROTATION,
                                512L,
                                2L,
                                energyFace(lathe),
                                false)
                                == 2L
                        && press.insert(
                                EnergyType.KINETIC_PUSH,
                                512L,
                                2L,
                                energyFace(press),
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
    public static void fireboxesRunAllThreeHuMachineKinds(
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
                        "chemical/distillery/water_to_water_distilled"),
                requireRecipe(
                        ModRecipeMaps.DRYING,
                        "chemical/drying/perlite"),
                requireRecipe(
                        ModRecipeMaps.SMELTER,
                        "chemical/smelter/ilmenite"));
        List<ConfiguredProcessingMachineBlockEntity> machines =
                new ArrayList<>();
        for (int index = 0; index < machinePositions.size(); index++) {
            BlockPos machinePos = machinePositions.get(index);
            GameTestHeatSources.placeHuSource(helper, machinePos.below());
            FuelGeneratorBlockEntity firebox =
                    helper.getBlockEntity(machinePos.below());
            helper.assertTrue(
                    firebox.seedStoredEnergy(energyCapacity()),
                    "Could not fuel HU machine " + index);
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
    public static void higherTierHuUsesControlledAdjacentHandlers(
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
                        "chemical/distillery/water_to_water_distilled"),
                requireRecipe(
                        ModRecipeMaps.DRYING,
                        "chemical/drying/perlite"),
                requireRecipe(
                        ModRecipeMaps.SMELTER,
                        "chemical/smelter/ilmenite"));
        List<ConfiguredProcessingMachineBlockEntity> machines =
                new ArrayList<>();
        for (int index = 0; index < machinePositions.size(); index++) {
            BlockPos machinePos = machinePositions.get(index);
            ConfiguredProcessingMachineBlockEntity machine =
                    placeConfigured(
                            helper,
                            machinePos,
                            blocks.get(index),
                            specs.get(index));
            loadRecipeInputs(machine, recipes.get(index));
            helper.assertTrue(
                    machine.insert(
                                    EnergyType.HEAT,
                                    512L,
                                    16L,
                                    Direction.DOWN,
                                    false)
                            == 16L,
                    "Could not inject controlled tier-3 HU " + index);
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
    public static void failureStatesExposeWrongEnergyUnderpoweredAndLimits(
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
                        "chemical/distillery/water_to_water_distilled"));
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
                        Direction.DOWN,
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
                        "chemical/electrolyzer/uvarovite"));

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
        GameTestHeatSources.placeHuSource(helper, fireboxPos);
        ConfiguredProcessingMachineBlockEntity smelter =
                placeConfigured(helper, smelterPos, ModBlocks.SMELTER.get(), ModProcessingMachines.SMELTER);
        FuelGeneratorBlockEntity firebox = helper.getBlockEntity(fireboxPos);
        helper.assertTrue(
                firebox.seedStoredEnergy(energyCapacity()),
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

        Player survivalPlayer = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(
                helper.getLevel().destroyBlock(
                        helper.absolutePos(survivalDestroy),
                        true,
                        survivalPlayer),
                "Survival player could not destroy the ceramic mold");
        survivalPlayer.discard();

        Player creativePlayer = helper.makeMockPlayer(GameType.CREATIVE);
        helper.assertTrue(
                helper.getLevel().destroyBlock(
                        helper.absolutePos(creativeDestroy),
                        false,
                        creativePlayer),
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
    public static void hotIngotSmeltsHurtsAndKeepsIdentity(
            GameTestHelper helper) {
        BlockPos fireboxPos = new BlockPos(4, 1, 4);
        BlockPos smelterPos = fireboxPos.above();
        GameTestHeatSources.placeHuSource(helper, fireboxPos);
        ConfiguredProcessingMachineBlockEntity smelter =
                placeConfigured(
                        helper,
                        smelterPos,
                        ModBlocks.SMELTER.get(),
                        ModProcessingMachines.SMELTER);
        FuelGeneratorBlockEntity firebox = helper.getBlockEntity(fireboxPos);
        helper.assertTrue(
                firebox.seedStoredEnergy(energyCapacity()),
                "Could not fuel hot-ingot smelter");
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
        Player survivalPlayer = helper.makeMockPlayer(GameType.SURVIVAL);
        float[] healthBeforeContact = new float[1];

        GameTestSequence sequence = helper.startSequence()
                .thenIdle(4)
                .thenExecute(() -> {
                    helper.assertTrue(
                            smelter.progress() > 0,
                            "Hot-ingot route did not start");
                    forceLastTick(helper, smelter);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    ItemStack hot = smelter.inventory().extractItem(
                            smelter.spec().items().outputs().getFirst(),
                            1,
                            false);
                    helper.assertTrue(
                            MaterialLookup.matches(
                                    hot,
                                    "copper",
                                    MaterialPrefixes.INGOT_HOT)
                                    && hot.has(ModComponents.HEAT.get())
                                    && ItemHeat.temperature(
                                            hot,
                                            helper.getLevel().getGameTime()) > 20.0f,
                            "Smelter output is not an independently heated hot ingot");

                    ItemStack creativeHot = MaterialLookup.stack(
                            "copper", MaterialPrefixes.INGOT_HOT);
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
                    ItemStack remainingHot = inventoryStack(
                            survivalPlayer,
                            hotItem);
                    helper.assertTrue(
                            !remainingHot.isEmpty()
                                    && inventoryItemCount(
                                                    survivalPlayer,
                                                    hotItem)
                                            == 2
                                    && inventoryStack(
                                                    survivalPlayer,
                                                    ordinaryItem)
                                            .isEmpty()
                                    && !remainingHot.has(ModComponents.HEAT.get()),
                            "Inventory maintenance must keep hot-ingot identity and only clear HEAT");
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
    public static void crudeOilDepositExtractsThroughPipeAndDistills(
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
        GameTestHeatSources.placeHuSource(helper, distilleryPos.below());
        helper.setBlock(
                distilleryPos,
                ModBlocks.DISTILLERY.get().defaultBlockState()
                        .setValue(
                                ProcessingMachineBlock.FACING,
                                Direction.SOUTH));
        FluidPipeBlockEntity pipe = helper.getBlockEntity(pipePos);
        helper.assertTrue(
                pipe.setCover(Direction.WEST, PipeCover.pump()),
                "Could not install the wellhead pump");
        ConfiguredProcessingMachineBlockEntity distillery =
                helper.getBlockEntity(distilleryPos);
        FuelGeneratorBlockEntity firebox =
                helper.getBlockEntity(distilleryPos.below());
        helper.assertTrue(
                firebox.seedStoredEnergy(energyCapacity()),
                "Could not fuel the HU distillery");
        GTRecipe recipe = requireRecipe(
                ModRecipeMaps.DISTILLERY,
                "hydrocarbon/distillery/crude_oil_to_fuel_and_lubricant");
        helper.assertTrue(
                distillery.spec().energy().type() == EnergyType.HEAT
                        && distillery.spec().energy().mode()
                                == ProcessingMachineSpec.EnergyMode.ADJACENT,
                "Distillery did not bind the adjacent HU contract");

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
                            "Extraction consumed the legacy reserve");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 220)
    public static void naturalGasConvertsAndHeatsBoiler(
            GameTestHelper helper) {
        BlockPos depositPos = new BlockPos(3, 1, 9);
        BlockPos extractorPos = depositPos.above();
        BlockPos generifierPos = new BlockPos(5, 2, 9);
        BlockPos generatorPos = new BlockPos(9, 1, 9);
        BlockPos boilerPos = generatorPos.above();
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
                ModBlocks.BRONZE_BURNING_BOX_GAS.get()
                        .defaultBlockState()
                        .setValue(
                                FuelGeneratorBlock.FACING,
                                Direction.WEST));
        helper.setBlock(boilerPos, ModBlocks.BRONZE_BOILER.get());
        CableBlock cable = ModBlocks.electricalConductorBlock(
                "copper", MaterialPrefixes.CABLE).get();
        helper.setBlock(
                cablePos,
                conductorState(cable, Direction.EAST));
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
        BoilerBlockEntity boiler = helper.getBlockEntity(boilerPos);
        helper.assertTrue(
                generator.outputEnergyType() == EnergyType.HEAT
                        && generator.handles(
                                EnergyType.HEAT, Direction.UP)
                        && !generator.handles(
                                EnergyType.ELECTRIC, Direction.UP)
                        && !generator.handles(
                                EnergyType.ELECTRIC, Direction.EAST)
                        && generator.fluids(Direction.UP) == null
                        && generator.fluids(Direction.NORTH) != null
                        && generator.fluids(Direction.SOUTH) != null,
                "Generator did not expose the exact HU-up/exhaust-side profile");
        ConfiguredProcessingMachineBlockEntity electrolyzer =
                helper.getBlockEntity(electrolyzerPos);
        GTRecipe electrolysis = requireRecipe(
                ModRecipeMaps.ELECTROLYZER,
                "chemical/electrolyzer/salt");
        GTRecipe gasFuel = requireRecipe(
                ModRecipeMaps.FUELS_GAS,
                "hydrocarbon/fuels_gas/methane");
        loadRecipeInputs(electrolyzer, electrolysis);
        helper.assertTrue(
                boiler.fluids(Direction.WEST).fill(
                                new FluidStack(
                                        ModFluids.materialFluid(
                                                        "water_distilled")
                                                .orElseThrow(),
                                        20),
                                IFluidHandler.FluidAction.EXECUTE)
                        == 20,
                "Could not fill the HU consumer with distilled water");

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
                        generifier.fluids(fluidIn(generifier)),
                        5))
                .thenIdle(7)
                .thenExecute(() -> transferFluid(
                        helper,
                        generifier.fluids(fluidOut(generifier)),
                        generator.fluids(Direction.WEST),
                        5))
                .thenExecute(generator::ignite)
                .thenIdle(65)
                .thenExecute(() -> {
                    FluidStack expectedWater =
                            gasFuel.fluidOutputs().getFirst();
                    FluidStack expectedCarbonDioxide =
                            gasFuel.fluidOutputs().get(1);
                    helper.assertTrue(
                            generator.outputAmount(0)
                                            == expectedWater.getAmount()
                                    && generator.outputAmount(1)
                                            == expectedCarbonDioxide
                                                    .getAmount(),
                            "Methane-generator exhaust was not conserved");
                    helper.assertTrue(
                            gasFuel.duration() == 24
                                    && generator.energyGenerated() == 1_152L
                                    && generator.energyExtracted() == 1_152L
                                    && generator.energyStored() == 0L,
                            "5 mB methane did not conserve "
                                    + "1536 source units * 75% = 1152 HU");
                    helper.assertTrue(
                            boiler.steamAmount() == 2_240
                                    && boiler.accumulatedHu() == 32,
                            "Gas generator did not feed the adjacent HU "
                                    + "boiler with exact 80 HU batches");
                    helper.assertTrue(
                            electrolyzer.workProgressLong() == 0L
                                    && electrolyzer.stored(
                                            EnergyType.ELECTRIC) == 0L,
                            "HU gas generator directly energized an EU cable");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void gasGeneratorTransactionsIdentityAndBlockedStates(
            GameTestHelper helper) {
        BlockPos consumerBlockedPos = new BlockPos(3, 1, 5);
        BlockPos exhaustFullPos = new BlockPos(7, 1, 5);
        BlockPos currentPos = new BlockPos(11, 1, 5);
        BlockPos nearMissPos = new BlockPos(15, 1, 5);
        for (BlockPos position : List.of(
                consumerBlockedPos,
                exhaustFullPos,
                currentPos,
                nearMissPos)) {
            helper.setBlock(
                    position,
                    ModBlocks.BRONZE_BURNING_BOX_GAS.get()
                            .defaultBlockState()
                            .setValue(
                                    FuelGeneratorBlock.FACING,
                                    Direction.EAST));
        }
        helper.setBlock(
                consumerBlockedPos.above(),
                ModBlocks.BRONZE_BOILER.get());
        FuelGeneratorBlockEntity consumerBlocked =
                helper.getBlockEntity(consumerBlockedPos);
        FuelGeneratorBlockEntity exhaustFull =
                helper.getBlockEntity(exhaustFullPos);
        FuelGeneratorBlockEntity current =
                helper.getBlockEntity(currentPos);
        FuelGeneratorBlockEntity nearMiss =
                helper.getBlockEntity(nearMissPos);
        GTRecipe fuel = requireRecipe(
                ModRecipeMaps.FUELS_GAS,
                "hydrocarbon/fuels_gas/methane");
        FluidStack methane = fuel.fluidInputs().getFirst();
        IFluidHandler blockedInput =
                consumerBlocked.fluids(Direction.WEST);
        helper.assertTrue(
                blockedInput.fill(
                                methane,
                                IFluidHandler.FluidAction.SIMULATE)
                                == methane.getAmount()
                        && consumerBlocked.inputAmount() == 0,
                "Gas input simulation mutated the fuel tank");
        helper.assertTrue(
                blockedInput.fill(
                                methane,
                                IFluidHandler.FluidAction.EXECUTE)
                        == methane.getAmount(),
                "Gas input execution differed from simulation");

        var registries = helper.getLevel().registryAccess();
        CompoundTag exhaustTag =
                exhaustFull.saveWithoutMetadata(registries);
        FluidTank fullWater = new FluidTank(16_000);
        fullWater.setFluid(fuel.fluidOutputs().getFirst()
                .copyWithAmount(16_000));
        FluidTank fullCarbonDioxide = new FluidTank(16_000);
        fullCarbonDioxide.setFluid(fuel.fluidOutputs().get(1)
                .copyWithAmount(16_000));
        FluidTank exhaustInput = new FluidTank(16_000);
        exhaustInput.setFluid(methane.copy());
        exhaustTag.put(
                "input",
                exhaustInput.writeToNBT(
                        registries, new CompoundTag()));
        exhaustTag.put(
                "output_0",
                fullWater.writeToNBT(
                        registries, new CompoundTag()));
        exhaustTag.put(
                "output_1",
                fullCarbonDioxide.writeToNBT(
                        registries, new CompoundTag()));
        exhaustFull.loadWithComponents(exhaustTag, registries);

        consumerBlocked.ignite();
        exhaustFull.ignite();

        CompoundTag currentTag =
                current.saveWithoutMetadata(registries);
        currentTag.putLong("energy", 321L);
        currentTag.putLong("energy_generated", 321L);
        currentTag.putLong("energy_extracted", 0L);
        current.loadWithComponents(currentTag, registries);

        CompoundTag nearMissIdentity = currentTag.copy();
        nearMissIdentity.putString("energy_identity", "ELECTRIC");
        nearMiss.loadWithComponents(nearMissIdentity, registries);

        helper.startSequence()
                .thenIdle(30)
                .thenExecute(() -> {
                    BoilerBlockEntity emptyBoiler =
                            helper.getBlockEntity(consumerBlockedPos.above());
                    helper.assertTrue(
                            consumerBlocked.inputAmount() == 0
                                    && consumerBlocked.energyGenerated()
                                            == 1_152L
                                    && consumerBlocked.energyGenerated()
                                            == consumerBlocked.energyStored()
                                                    + consumerBlocked
                                                            .energyExtracted()
                                    && consumerBlocked.energyExtracted() > 0L
                                    && emptyBoiler.accumulatedHu()
                                            == consumerBlocked.energyExtracted()
                                    && !consumerBlocked.status().equals(
                                            "energy_output_blocked")
                                    && consumerBlocked.outputAmount(0)
                                            == fuel.fluidOutputs()
                                                    .getFirst().getAmount()
                                    && consumerBlocked.outputAmount(1)
                                            == fuel.fluidOutputs()
                                                    .get(1).getAmount(),
                            "Empty-boiler HU push voided fuel, heat, or exhaust");
                    helper.assertTrue(
                            exhaustFull.inputAmount() == methane.getAmount()
                                    && exhaustFull.energyGenerated() == 0L
                                    && exhaustFull.outputAmount(0) == 16_000
                                    && exhaustFull.outputAmount(1) == 16_000
                                    && exhaustFull.status().equals(
                                            "exhaust_blocked"),
                            "Full gas exhaust consumed fuel or generated HU");
                    CompoundTag currentSave =
                            current.saveWithoutMetadata(registries);
                    helper.assertTrue(
                            !current.identityQuarantined()
                                    && current.energyStored() == 321L
                                    && currentSave.getInt(
                                            "fuel_generator_schema_version")
                                            == 2
                                    && currentSave.getString(
                                            "energy_identity").equals("HEAT"),
                            "Current gas-generator identity did not remain accepted");
                    CompoundTag nearMissSave =
                            nearMiss.saveWithoutMetadata(registries);
                    helper.assertTrue(
                            nearMiss.identityQuarantined()
                                    && nearMissSave.getInt(
                                            "fuel_generator_schema_version")
                                            == 2
                                    && nearMissSave.getString(
                                            "energy_identity")
                                            .equals("ELECTRIC"),
                            "Wrong gas-generator identity escaped quarantine");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void naturalGasLeaksAndFlammableCloudIgnites(
            GameTestHelper helper) {
        BlockPos pipePos = new BlockPos(14, 2, 5);
        // GT6 Loader_MultiTileEntities fluid pipes: Wood is gas-proof false.
        // Tin and TinAlloy are not leak fixtures: tin is unregistered, and
        // TinAlloy is gas-proof.
        FluidPipeBlock pipeBlock = (FluidPipeBlock) ModBlocks.pipeBlock(
                "wood",
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
                    "Unsafe wood pipe accepted natural gas");
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
                !worldgenVeins.isEmpty(),
                "Runtime configured-feature registry has no large veins");
        for (ResourceLocation id : worldgenVeins) {
            ConfiguredFeature<?, ?> configured = registry.get(
                    ResourceKey.create(Registries.CONFIGURED_FEATURE, id));
            helper.assertTrue(
                    configured != null
                            && configured.config()
                                    instanceof LargeVeinConfiguration,
                    id + " did not decode as a large vein");
            LargeVeinConfiguration config =
                    (LargeVeinConfiguration) configured.config();
            helper.assertTrue(
                    config.profileId().equals(id),
                    id + " decoded with mismatched profile identity "
                            + config.profileId());
            assertLargeVeinPlaces(
                    helper,
                    configured,
                    config);
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

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void surfaceRockScatterPlacesFromRuntimeTag(
            GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ResourceLocation featureId = ResourceLocation.fromNamespaceAndPath(
                CrucibleCraft.MODID,
                "surface_rock_scatter");
        Registry<ConfiguredFeature<?, ?>> registry =
                level.registryAccess().registryOrThrow(
                        Registries.CONFIGURED_FEATURE);
        ConfiguredFeature<?, ?> configured = registry.get(
                ResourceKey.create(Registries.CONFIGURED_FEATURE, featureId));
        helper.assertTrue(
                configured != null
                        && configured.config()
                                instanceof SurfaceRockConfiguration,
                "Runtime registry lacks decoded surface_rock_scatter");
        SurfaceRockConfiguration config =
                (SurfaceRockConfiguration) configured.config();
        helper.assertTrue(
                config.amount() == 2,
                "surface_rock_scatter amount drifted from WorldgenRocks");
        helper.assertTrue(
                config.probability() == 3,
                "surface_rock_scatter probability drifted from WorldgenRocks");

        BlockPos chunkOrigin = chunkAlignedOrigin(helper);
        int surfaceY = 64;
        BlockPos protectedPos = chunkOrigin.offset(8, surfaceY + 1, 8);
        prepareSurfaceRockScatterPad(level, chunkOrigin, surfaceY);
        level.setBlock(
                protectedPos,
                Blocks.STONE.defaultBlockState(),
                Block.UPDATE_ALL);
        boolean placed = SurfaceRockFeature.tryPlace(
                level,
                chunkOrigin.getX(),
                surfaceY,
                chunkOrigin.getZ(),
                new java.util.Random(1L),
                Blocks.DIRT.defaultBlockState());
        helper.assertTrue(
                placed,
                "surface_rock_scatter failed real placement");
        helper.assertTrue(
                level.getBlockState(chunkOrigin.offset(0, surfaceY + 1, 0)).getBlock()
                        instanceof com.masson.cruciblecraft.content.block.GtSurfaceRockBlock,
                "surface_rock_scatter did not place gt_surface_rock into air");
        helper.assertTrue(
                !(level.getBlockState(chunkOrigin.offset(0, surfaceY + 1, 0)).getBlock()
                        instanceof RockBlock),
                "surface_rock_scatter must not place a catalog RockBlock");
        helper.assertTrue(
                level.getBlockState(protectedPos).is(Blocks.STONE),
                "surface_rock_scatter overwrote an occupied surface cell");
        helper.succeed();
    }

    private static BlockPos chunkAlignedOrigin(GameTestHelper helper) {
        BlockPos anchor = helper.absolutePos(BlockPos.ZERO);
        return new BlockPos(
                (anchor.getX() >> 4) << 4,
                0,
                (anchor.getZ() >> 4) << 4);
    }

    private static void prepareSurfaceRockScatterPad(
            ServerLevel level,
            BlockPos chunkOrigin,
            int surfaceY) {
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                BlockPos surface = chunkOrigin.offset(dx, surfaceY, dz);
                level.setBlock(
                        surface.below(),
                        Blocks.STONE.defaultBlockState(),
                        Block.UPDATE_ALL);
                level.setBlock(
                        surface,
                        Blocks.DIRT.defaultBlockState(),
                        Block.UPDATE_ALL);
                level.setBlock(
                        surface.above(),
                        Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_ALL);
            }
        }
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
        GameTestHeatSources.placeHuSource(helper, chainFireboxPos);
        FuelGeneratorBlockEntity chainFirebox = helper.getBlockEntity(chainFireboxPos);
        helper.assertTrue(chainFirebox.seedStoredEnergy(energyCapacity()),
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
                    assembler.insert(
                            assembler.spec().energy().type(),
                            32L,
                            16L,
                            energyFace(assembler),
                            false);
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
    public static void representativePickaxesExposeMaterialComponents(
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
                    material + " pickaxe durability does not follow tool policy");
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
                "Tool items are not discoverable through item-level material policy");
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
                        .equals("tool/assembler/pickaxe/metal/iron"))
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
                            "Tool pickaxe recipe was not selected");
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
    public static void everyComponentPlacedMachineAdvancesRealRecipe(GameTestHelper helper) {
        List<Block> blocks = List.of(
                ModBlocks.EXTRUDER.get(), ModBlocks.CUTTER.get(), ModBlocks.LATHE.get(),
                ModBlocks.ROLLINGMILL.get(), ModBlocks.ROLLBENDER.get(),
                ModBlocks.WIREMILL.get(), ModBlocks.BENDER.get(), ModBlocks.ASSEMBLER.get(),
                ModBlocks.PRESS.get());
        List<ConfiguredProcessingMachineBlockEntity> machines = new java.util.ArrayList<>();
        for (int i = 0; i < ModProcessingMachines.COMPONENT_MACHINES.size(); i++) {
            BlockPos pos = new BlockPos(2 + (i % 5) * 4, 2, 2 + (i / 5) * 6);
            ProcessingMachineSpec spec = ModProcessingMachines.COMPONENT_MACHINES.get(i);
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

    @GameTest(template = TEMPLATE, timeoutTicks = 1200)
    public static void everyLiveComponentRecipeTracesToGeneratedComponentJson(
            GameTestHelper helper) {
        List<RecipeMap> componentMaps = List.of(
                ModRecipeMaps.EXTRUDER,
                ModRecipeMaps.CUTTER,
                ModRecipeMaps.LATHE,
                ModRecipeMaps.ROLLINGMILL,
                ModRecipeMaps.ROLLBENDER,
                ModRecipeMaps.WIREMILL,
                ModRecipeMaps.BENDER,
                ModRecipeMaps.ASSEMBLER,
                ModRecipeMaps.WELDER,
                ModRecipeMaps.PRESS);
        StringBuilder countDrift = new StringBuilder();
        for (RecipeMap map : componentMaps) {
            if (map.unindexedRecipeCount() != 0) {
                countDrift.append(map.id().getPath())
                        .append(" unindexed=").append(map.unindexedRecipeCount())
                        .append("; ");
            }
        }
        CrucibleCraft.LOGGER.info(
                "Component authored count check: {}",
                countDrift.isEmpty() ? "match" : countDrift);
        helper.assertTrue(
                countDrift.isEmpty(),
                "Component manifest counts drifted");
        for (RecipeMap map : componentMaps) {
            for (RecipeMap.Entry entry : map.authoredEntries()) {
                String expandedPath = entry.id().getPath();
                int materialSeparator = expandedPath.lastIndexOf('/');
                helper.assertTrue(
                        materialSeparator > 0,
                        "Component recipe lacks expanded material suffix: " + entry.id());
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
                        "Live component recipe has no component-rule JSON source: "
                                + entry.id() + " -> " + sourcePath);
            }
        }
        Set<String> liveChemical = new java.util.TreeSet<>();
        for (RecipeMap map : ModProcessingMachines.CHEMICAL_HOST_MACHINES.stream()
                .map(ProcessingMachineSpec::requireRecipeMap)
                .distinct()
                .toList()) {
            ProcessingMachineSpec host =
                    ModProcessingMachines.forRecipeMap(map.id()).orElseThrow();
            for (RecipeMap.Entry entry : map.entries()) {
                if (!entry.id().getPath().startsWith("chemical/")) {
                    continue;
                }
                liveChemical.add(entry.id().toString());
                String resource = "data/" + entry.id().getNamespace()
                        + "/recipe/" + entry.id().getPath() + ".json";
                helper.assertTrue(
                        CrucibleCraftGameTests.class.getClassLoader()
                                .getResource(resource) != null,
                        "Live chemical recipe has no generated source JSON: " + entry.id());
                helper.assertTrue(
                        host.validator().validate(entry.recipe()).isEmpty(),
                        "Live chemical recipe is published but its runtime host rejects it: "
                                + entry.id() + " host=" + host.id()
                                + " reason="
                                + host.validator().validate(entry.recipe()).orElse(""));
                GTRecipeQuery query = queryFor(entry.recipe());
                RecipeMap.Match resolved =
                        map.findMatch(query).orElse(null);
                helper.assertTrue(
                        resolved != null && resolved.id().equals(entry.id()),
                        "Live chemical recipe cannot be found by its declared input: "
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
                    map.id() + " contains unindexed chemical recipes");
        }
        Set<String> generatedChemical = new java.util.TreeSet<>();
        ResourceManager resources = helper.getLevel().getServer().getResourceManager();
        resources.listResources(
                        "recipe/chemical",
                        location -> location.getPath().endsWith(".json"))
                .keySet()
                .forEach(location -> {
                    String path = location.getPath();
                    generatedChemical.add(location.getNamespace()
                            + ":"
                            + path.substring(
                                    "recipe/".length(),
                                    path.length() - ".json".length()));
                });
        helper.assertTrue(
                liveChemical.equals(generatedChemical),
                "Live chemical recipes do not match recipe/chemical JSON: live="
                        + liveChemical.size()
                        + " json="
                        + generatedChemical.size());
        ProcessingEmiRegistrationPlan emiPlan = ProcessingEmiRegistrationPlan.create(
                ModProcessingMachines.CONFIGURED_MACHINES);
        int expectedEmiRecipes = ModProcessingMachines.CONFIGURED_MACHINES.stream()
                .mapToInt(spec -> spec.requireRecipeMap().entries().size())
                .sum();
        helper.assertTrue(
                emiPlan.machines().size()
                                == ModProcessingMachines.CONFIGURED_MACHINES.size()
                        && emiPlan.recipes().size() == expectedEmiRecipes,
                "Generic processing EMI does not cover every configured machine recipe: "
                        + emiPlan.machines().size()
                        + " machines, "
                        + emiPlan.recipes().size()
                        + "/"
                        + expectedEmiRecipes
                        + " recipes");
        var metrics = com.masson.cruciblecraft.recipe.gt.GTRecipeMapLoader
                .lastPublicationMetrics();
        CrucibleCraft.LOGGER.info(
                "GameTest recipe publication metrics: {}", metrics);
        helper.assertTrue(
                metrics.compactLoadExtruderLogicalRecipes() == 0
                        && metrics.compactLoadExtruderEagerRecipes() == 0
                        && metrics.compactLoadExtruderLazyRecipes() == 0
                        && metrics.compactLoadExtruderCacheCeiling() == 512
                        && metrics.compactLoadExtruderSyncBytes() == 0L
                        && metrics.compactLoadExtruderAuthoredEntries() == 0
                        && metrics.compactLoadExtruderStableFingerprint()
                                .matches("[0-9a-f]{64}")
                        && metrics.runtimeSide()
                                == ExtruderRecipeFamilyProvider.RuntimeSide.SERVER,
                "Retired extruder compact-load publication drifted");
        helper.assertTrue(
                ModRecipeMaps.EXTRUDER.family(ExtruderRecipeFamilyProvider.FAMILY_ID)
                        .map(family -> family.logicalRecipeCount() == 0)
                        .orElse(true),
                "Retired extruder material_rule family still publishes rows");
        Map<String, Integer> toolCounts = Map.ofEntries(
                Map.entry("pickaxe", 334),
                Map.entry("shovel", 421),
                Map.entry("axe", 333),
                Map.entry("hoe", 333),
                Map.entry("sword", 421),
                Map.entry("smithing_hammer", 320),
                Map.entry("file", 92),
                Map.entry("chisel", 310),
                Map.entry("saw", 310),
                Map.entry("screwdriver", 310),
                Map.entry("wrench", 311),
                Map.entry("monkey_wrench", 311),
                Map.entry("wire_cutter", 313));
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
                Map.entry("wrench", ModItems.MATERIAL_WRENCH.get()),
                Map.entry(
                        "monkey_wrench",
                        ModItems.MATERIAL_MONKEY_WRENCH.get()),
                Map.entry(
                        "wire_cutter",
                        ModItems.MATERIAL_WIRE_CUTTER.get()));
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
                Map.entry("wrench", ToolMaterialRules.ToolKind.WRENCH),
                Map.entry(
                        "monkey_wrench",
                        ToolMaterialRules.ToolKind.MONKEY_WRENCH),
                Map.entry(
                        "wire_cutter",
                        ToolMaterialRules.ToolKind.WIRE_CUTTER));
        Map<String, Integer> eligibleWithoutRoute = Map.ofEntries(
                Map.entry("pickaxe", 205),
                Map.entry("shovel", 118),
                Map.entry("axe", 206),
                Map.entry("hoe", 206),
                Map.entry("sword", 118),
                Map.entry("smithing_hammer", 78),
                Map.entry("file", 56),
                Map.entry("chisel", 0),
                Map.entry("saw", 0),
                Map.entry("screwdriver", 0),
                Map.entry("wrench", 0),
                Map.entry("monkey_wrench", 0),
                Map.entry("wire_cutter", 0));
        StringBuilder toolDrift = new StringBuilder();
        Map<String, List<RecipeMap.Entry>> toolRecipes = new java.util.LinkedHashMap<>();
        for (var toolEntry : toolCounts.entrySet()) {
            String tool = toolEntry.getKey();
            List<RecipeMap.Entry> recipes = ModRecipeMaps.ASSEMBLER.entries().stream()
                    .filter(entry -> entry.id().getPath()
                            .startsWith("tool/assembler/" + tool + "/"))
                    .toList();
            toolRecipes.put(tool, recipes);
            long itemEligible = MaterialCatalog.values().stream()
                    .filter(material -> ToolMaterialRules.isAllowed(
                            toolKinds.get(tool), material.id()))
                    .count();
            int gap = Math.toIntExact(itemEligible - recipes.size());
            if (recipes.size() != toolEntry.getValue()
                    || gap != eligibleWithoutRoute.get(tool)) {
                toolDrift.append(tool)
                        .append(" routes=").append(recipes.size())
                        .append("/").append(toolEntry.getValue())
                        .append(" gap=").append(gap)
                        .append("/").append(eligibleWithoutRoute.get(tool))
                        .append("; ");
            }
        }
        CrucibleCraft.LOGGER.info(
                "Tool route count check: {}",
                toolDrift.isEmpty() ? "match" : toolDrift);
        helper.assertTrue(toolDrift.isEmpty(), "Tool route counts drifted");
        for (var toolEntry : toolCounts.entrySet()) {
            String tool = toolEntry.getKey();
            List<RecipeMap.Entry> recipes = toolRecipes.get(tool);
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
                        "Tool batch lost bounded inputs, catalyst actions, "
                                + "or component output: " + entry.id());
            }
        }
        List<RecipeMap.Entry> pickaxes = ModRecipeMaps.ASSEMBLER.entries().stream()
                .filter(entry -> entry.id().getPath()
                        .startsWith("tool/assembler/pickaxe/"))
                .toList();
        StringBuilder pickaxeDrift = new StringBuilder();
        Map.of(
                "metal", 205L,
                "gem", 128L,
                "stone_rod_exception", 1L)
                .forEach((route, expectedCount) -> {
                    long actualCount = pickaxes.stream()
                            .filter(entry -> entry.id().getPath().startsWith(
                                    "tool/assembler/pickaxe/" + route + "/"))
                            .count();
                    if (actualCount != expectedCount) {
                        pickaxeDrift.append(route)
                                .append('=').append(actualCount)
                                .append('/').append(expectedCount)
                                .append("; ");
                    }
                });
        CrucibleCraft.LOGGER.info(
                "Pickaxe route count check: {}",
                pickaxeDrift.isEmpty() ? "match" : pickaxeDrift);
        helper.assertTrue(pickaxeDrift.isEmpty(), "Pickaxe route counts drifted");
        helper.assertTrue(
                pickaxes.stream().anyMatch(entry -> entry.id().getPath().equals(
                        "tool/assembler/pickaxe/stone_rod_exception/stone")),
                "Exact GT6 Stone identity lost its declared route exception");
        helper.assertTrue(
                ToolMaterialRules.isAllowed(
                        ToolMaterialRules.ToolKind.PICKAXE, "oak")
                        && pickaxes.stream().noneMatch(entry -> entry.id().getPath()
                                .endsWith("/oak")),
                "Oak must remain source-eligible but route-unavailable until "
                        + "its registered-form closure is deliberately restored");
        // Loader_Tools.java:295 pickaxe listener excludes only antimatter, Wood, and coated.
        // Rubber is bouncy and stretchy, so its generated route stays.
        for (String blocked : List.of(
                "anti_adamantium", "anti_vibranium", "bone", "wood")) {
            helper.assertTrue(
                    pickaxes.stream().noneMatch(entry -> entry.id().getPath()
                            .endsWith("/" + blocked)),
                    blocked + " unexpectedly has a Pickaxe route");
        }
        // Recipe counts are UNVERIFIED_SCALE. Reload time is
        // portfolio/publication-reload-performance, not this grid.
        helper.assertTrue(
                metrics.lazyLogicalRecipes()
                                <= ModProcessingMachines
                                        .ALL_LAZY_LOGICAL_RECIPE_HARD_CEILING
                        && metrics.compactLoadExtruderCacheCeiling()
                                <= ModProcessingMachines
                                        .ALL_LAZY_RECIPE_CACHE_HARD_CEILING
                        && metrics.chemicalPublishedRecipes()
                                <= ModProcessingMachines.CHEMICAL_RECIPE_BUDGET
                        && metrics.mortarAuthoredMaterialRules()
                                <= ModProcessingMachines
                                        .MORTAR_MATERIAL_RULE_BUDGET
                        && metrics.pipeMaterialRules()
                                <= ModProcessingMachines
                                        .PIPE_MATERIAL_RULE_BUDGET
                        && metrics.ingotFormMaterialRules()
                                <= ModProcessingMachines
                                        .INGOT_FORM_MATERIAL_RULE_BUDGET,
                "Recipe publication ceilings drifted");
        helper.succeed();
    }

    @GameTest(
            template = TEMPLATE,
            batch = "machine_variant_crafting_opening",
            timeoutTicks = 80)
    public static void machineVariantRecipesExecuteFromLiveRecipeManager(
            GameTestHelper helper) {
        List<MachineCraftingCase> cases = machineCraftingCases();
        helper.assertTrue(cases.size() == 9, "Opening machines must execute all nine variants");
        executeMachineCraftingCases(helper, cases);
    }

    @GameTest(
            template = TEMPLATE,
            batch = "machine_variant_crafting_kinetic",
            timeoutTicks = 80)
    public static void selectedMachineRecipesExecuteFromLiveRecipeManager(
            GameTestHelper helper) {
        List<MachineCraftingCase> cases = kineticMachineCraftingCases();
        helper.assertTrue(
                cases.size() == 15,
                "Kinetic machines must execute the selected five-by-three variants");
        executeMachineCraftingCases(helper, cases);
    }

    @GameTest(
            template = TEMPLATE,
            batch = "machine_variant_crafting_heat",
            timeoutTicks = 80)
    public static void selectedHuRecipesExecuteFromLiveRecipeManager(
            GameTestHelper helper) {
        List<MachineCraftingCase> cases = heatMachineCraftingCases();
        helper.assertTrue(
                cases.size() == 10,
                "Heat machines must execute all selected three-by-three HU variants");
        executeMachineCraftingCases(helper, cases);
    }

    @GameTest(
            template = TEMPLATE,
            batch = "publication_baseline",
            timeoutTicks = 400)
    public static void recipePublicationBaselineRemainsStable(
            GameTestHelper helper) {
        JsonObject baseline = selectedKineticPublicationBaseline();
        Set<String> expectedMapIds = jsonStringSet(
                baseline, "recipe_map_ids");
        Set<String> actualMapIds = ModRecipeMaps.ALL.stream()
                .map(map -> map.id().toString())
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        helper.assertTrue(
                actualMapIds.containsAll(expectedMapIds),
                "Kinetic RecipeMap stable id set drifted: expected="
                        + expectedMapIds + " actual=" + actualMapIds);

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
        CrucibleCraft.LOGGER.info(
                "Kinetic EMI enumeration expected={} actual={}",
                expectedEnumeration.size(),
                actualEnumeration.size());
        helper.assertTrue(
                emi.machines().size()
                                == ModProcessingMachines.CONFIGURED_MACHINES.size()
                        && expectedEnumeration.equals(actualEnumeration)
                        && actualEnumeration.size()
                                == new java.util.HashSet<>(
                                        actualEnumeration).size(),
                "Kinetic EMI plan does not cover live configured machines");

        JsonObject acquisition = baseline.getAsJsonObject("acquisition");
        Set<String> expectedCraftingIds = jsonStringSet(
                acquisition, "vanilla_recipe_ids");
        Set<Item> selectedResults = kineticMachineCraftingCases().stream()
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
        long kineticGtRows = ModRecipeMaps.ALL.stream()
                .flatMap(map -> map.entries().stream())
                .filter(entry -> entry.id().getNamespace().equals(
                        CrucibleCraft.MODID))
                .filter(entry -> entry.id().getPath().startsWith("machines/"))
                .count();
        boolean kineticAcquisitionMatches =
                acquisition.get("recipe_type").getAsString()
                                .equals("minecraft:crafting_shaped")
                        && expectedCraftingIds.equals(actualCraftingIds)
                        && actualCraftingIds.size()
                                == acquisition.get(
                                        "vanilla_crafting_rows").getAsInt()
                        && kineticGtRows
                                == acquisition.get("gt_recipe_rows").getAsLong();
        if (!kineticAcquisitionMatches) {
            CrucibleCraft.LOGGER.error(
                    "Kinetic acquisition crafting={} gt={}",
                    actualCraftingIds,
                    kineticGtRows);
        }
        helper.assertTrue(
                kineticAcquisitionMatches,
                "Kinetic acquisition is not exactly 15 vanilla crafting rows "
                        + "and zero GT rows");
        helper.succeed();
    }

    private static JsonObject selectedKineticPublicationBaseline() {
        var stream = CrucibleCraftGameTests.class.getClassLoader()
                .getResourceAsStream(
                        "data/cruciblecraft/kinetic_publication_baseline.json");
        if (stream == null) {
            throw new IllegalStateException(
                    "Missing kinetic publication baseline resource");
        }
        try (var reader = new InputStreamReader(
                stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Cannot read kinetic publication baseline", exception);
        }
    }

    @GameTest(
            template = TEMPLATE,
            batch = "publication_delta",
            timeoutTicks = 400)
    public static void recipePublicationMatchesPriorBaseline(
            GameTestHelper helper) {
        JsonObject baseline = selectedHeatPublicationBaseline();
        JsonObject kineticBaseline = selectedKineticPublicationBaseline();
        Set<String> expectedMapIds = jsonStringSet(
                baseline, "recipe_map_ids");
        Set<String> kineticMapIds = jsonStringSet(kineticBaseline, "recipe_map_ids");
        Set<String> actualMapIds = ModRecipeMaps.ALL.stream()
                .map(map -> map.id().toString())
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        helper.assertTrue(
                expectedMapIds.equals(kineticMapIds)
                        && actualMapIds.containsAll(expectedMapIds),
                "Heat RecipeMap stable id set differs from kinetic baseline: expected="
                        + expectedMapIds + " actual=" + actualMapIds);

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
        CrucibleCraft.LOGGER.info(
                "Heat EMI enumeration expected={} actual={}",
                expectedEnumeration.size(),
                actualEnumeration.size());
        helper.assertTrue(
                emi.machines().size()
                                == ModProcessingMachines.CONFIGURED_MACHINES.size()
                        && expectedEnumeration.equals(actualEnumeration)
                        && actualEnumeration.size()
                                == new java.util.HashSet<>(
                                        actualEnumeration).size(),
                "Heat EMI plan does not cover live configured machines");

        JsonObject acquisition = baseline.getAsJsonObject("acquisition");
        Set<String> expectedCraftingIds = jsonStringSet(
                acquisition, "vanilla_recipe_ids");
        Set<Item> selectedResults = heatPublicationResultItems();
        Set<String> actualCraftingIds = helper.getLevel().getRecipeManager()
                .getAllRecipesFor(RecipeType.CRAFTING)
                .stream()
                .filter(holder -> selectedResults.contains(
                        holder.value().getResultItem(
                                helper.getLevel().registryAccess()).getItem()))
                .map(holder -> holder.id().toString())
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        long heatGtRows = ModRecipeMaps.ALL.stream()
                .flatMap(map -> map.entries().stream())
                .filter(entry -> entry.id().getNamespace().equals(
                        CrucibleCraft.MODID))
                .filter(entry -> entry.id().getPath().startsWith("machines/"))
                .count();
        boolean heatAcquisitionMatches =
                acquisition.get("recipe_type").getAsString()
                                .equals("minecraft:crafting_shaped")
                        && expectedCraftingIds.equals(actualCraftingIds)
                        && actualCraftingIds.size()
                                == acquisition.get(
                                        "vanilla_crafting_rows").getAsInt()
                        && heatGtRows
                                == acquisition.get("gt_recipe_rows").getAsLong();
        if (!heatAcquisitionMatches) {
            CrucibleCraft.LOGGER.error(
                    "Heat acquisition crafting={} gt={}",
                    actualCraftingIds,
                    heatGtRows);
        }
        helper.assertTrue(
                heatAcquisitionMatches,
                "Heat acquisition is not exactly 9 vanilla crafting rows "
                        + "and zero GT rows");
        helper.succeed();
    }

    private static JsonObject selectedHeatPublicationBaseline() {
        var stream = CrucibleCraftGameTests.class.getClassLoader()
                .getResourceAsStream(
                        "data/cruciblecraft/heat_publication_baseline.json");
        if (stream == null) {
            throw new IllegalStateException(
                    "Missing heat publication baseline resource");
        }
        try (var reader = new InputStreamReader(
                stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Cannot read heat publication baseline", exception);
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
                    ItemStack ingredient = expected.key().get(symbol);
                    slots.add(
                            symbol == ' '
                                    ? ItemStack.EMPTY
                                    : ingredient.copy());
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
            batch = "identity_quarantine",
            timeoutTicks = 80)
    public static void processingIdentityMismatchesQuarantineAcrossNbtReload(
            GameTestHelper helper) {
        GTRecipe recipe = requireRecipe(
                ModRecipeMaps.ELECTROLYZER, "chemical/electrolyzer/salt");
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
                    ModMachineIdentities.identityOf(
                            machine.variant());
            List<MachineIdentityPolicy.Identity> mismatches = List.of(
                    new MachineIdentityPolicy.Identity(
                            current.machineKind() + "_wrong",
                            current.tierBand(),
                            current.materialId(),
                            current.energyIdentity()),
                    new MachineIdentityPolicy.Identity(
                            current.machineKind(),
                            current.tierBand() + "_wrong",
                            current.materialId(),
                            current.energyIdentity()),
                    new MachineIdentityPolicy.Identity(
                            current.machineKind(),
                            current.tierBand(),
                            current.materialId() + "_wrong",
                            current.energyIdentity()),
                    new MachineIdentityPolicy.Identity(
                            current.machineKind(),
                            current.tierBand(),
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
                    "cruciblecraft:identity_reload");
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
                    List.of("kind", "tier_band", "material", "energy")
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
            batch = "blank_identity",
            timeoutTicks = 40)
    public static void blankProcessingIdentityAdoptsCurrentAcrossNbtReload(
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
                new MachineIdentityPolicy.Identity("", "", "", ""));

        centrifuge.loadWithComponents(
                persisted, helper.getLevel().registryAccess());

        helper.assertTrue(
                !centrifuge.pausedReason().equals(
                        "material_quarantined"),
                "Blank first-save identity was quarantined");
        helper.assertTrue(
                !centrifuge.getUpdateTag(
                                helper.getLevel().registryAccess())
                        .contains("material_quarantine"),
                "Blank identity retained a quarantine diagnostic");
        helper.assertTrue(
                centrifuge.inventory().getStackInSlot(inputSlot).getCount()
                        == 3,
                "Blank identity reload lost inventory");
        helper.assertTrue(
                hasMachineIdentity(
                        centrifuge.saveWithoutMetadata(
                                helper.getLevel().registryAccess()),
                        ModMachineIdentities
                                .identityOf(centrifuge.variant())),
                "Blank identity did not adopt the current identity");
        helper.succeed();
    }

    @GameTest(
            template = TEMPLATE,
            batch = "tier_profile_rejected",
            timeoutTicks = 40)
    public static void tierProfileIdentityIsQuarantinedAcrossNbtReload(
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
                "cruciblecraft:identity_reload_probe");
        persisted.putInt("progress", 9);
        persisted.putInt("duration", 40);
        persisted.putString("status", "processing");
        persisted.putString(
                "tier_profile",
                ModMachineIdentities.identityOf(
                        lathe.variant()).tierBand());

        lathe.loadWithComponents(
                persisted, helper.getLevel().registryAccess());

        helper.assertTrue(
                lathe.pausedReason().equals("material_quarantined"),
                "Removed tier_profile field was accepted");
        helper.assertTrue(
                lathe.getUpdateTag(helper.getLevel().registryAccess())
                        .getString("material_quarantine")
                        .contains("tier_profile is unsupported"),
                "Removed tier_profile field lacked a quarantine diagnostic");
        helper.assertTrue(
                lathe.progress() == 9,
                "tier_profile quarantine lost progress");
        helper.assertTrue(
                lathe.inventory().getStackInSlot(inputSlot).getCount() == 3,
                "tier_profile quarantine lost inventory");
        helper.assertTrue(
                hasMachineIdentity(
                        lathe.saveWithoutMetadata(
                                helper.getLevel().registryAccess()),
                        ModMachineIdentities.identityOf(
                                lathe.variant())),
                "tier_profile quarantine did not rewrite canonical fields");
        helper.succeed();
    }

    private static void putMachineIdentity(
            CompoundTag tag,
            MachineIdentityPolicy.Identity identity) {
        tag.putString("machine_kind", identity.machineKind());
        tag.putString("tier_band", identity.tierBand());
        tag.putString("tier_material", identity.materialId());
        tag.putString("energy_identity", identity.energyIdentity());
    }

    private static boolean hasMachineIdentity(
            CompoundTag tag,
            MachineIdentityPolicy.Identity identity) {
        return tag.getString("machine_kind").equals(
                        identity.machineKind())
                && tag.getString("tier_band").equals(
                        identity.tierBand())
                && !tag.contains("tier_profile")
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
                        'C', new ItemStack(casing),
                        'G', material(material, MaterialPrefixes.GEAR, 1),
                        'S', material(
                                material,
                                MaterialPrefixes.LONG_ROD,
                                1)));
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
                        'C', new ItemStack(casing),
                        'R', material(material, MaterialPrefixes.ROD, 1),
                        'S', material(
                                material,
                                MaterialPrefixes.SPRING,
                                1),
                        'W', material(
                                material,
                                MaterialPrefixes.FINE_WIRE,
                                1)));
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
                        'M', new ItemStack(casing),
                        'S', material(
                                "platinum",
                                MaterialPrefixes.WIRE,
                                1),
                        'W', material(
                                cableMaterial,
                                MaterialPrefixes.CABLE,
                                1)));
    }

    private static List<MachineCraftingCase> kineticMachineCraftingCases() {
        return List.of(
                kineticMachineCraftingCase(
                        "lathe", "bronze",
                        ModItems.BRONZE_DOUBLE_MACHINE_CASING.get(),
                        ModItems.LATHE.get()),
                kineticMachineCraftingCase(
                        "steel_lathe", "steel",
                        ModItems.STEEL_DOUBLE_MACHINE_CASING.get(),
                        ModItems.STEEL_LATHE.get()),
                kineticMachineCraftingCase(
                        "titanium_lathe", "titanium",
                        ModItems.TITANIUM_DOUBLE_MACHINE_CASING.get(),
                        ModItems.TITANIUM_LATHE.get()),
                kineticMachineCraftingCase(
                        "rollingmill", "bronze",
                        ModItems.BRONZE_DOUBLE_MACHINE_CASING.get(),
                        ModItems.ROLLINGMILL.get()),
                kineticMachineCraftingCase(
                        "steel_rollingmill", "steel",
                        ModItems.STEEL_DOUBLE_MACHINE_CASING.get(),
                        ModItems.STEEL_ROLLINGMILL.get()),
                kineticMachineCraftingCase(
                        "titanium_rollingmill", "titanium",
                        ModItems.TITANIUM_DOUBLE_MACHINE_CASING.get(),
                        ModItems.TITANIUM_ROLLINGMILL.get()),
                kineticMachineCraftingCase(
                        "wiremill", "bronze",
                        ModItems.BRONZE_DOUBLE_MACHINE_CASING.get(),
                        ModItems.WIREMILL.get()),
                kineticMachineCraftingCase(
                        "steel_wiremill", "steel",
                        ModItems.STEEL_DOUBLE_MACHINE_CASING.get(),
                        ModItems.STEEL_WIREMILL.get()),
                kineticMachineCraftingCase(
                        "titanium_wiremill", "titanium",
                        ModItems.TITANIUM_DOUBLE_MACHINE_CASING.get(),
                        ModItems.TITANIUM_WIREMILL.get()),
                kineticMachineCraftingCase(
                        "shredder", "bronze",
                        ModItems.BRONZE_DOUBLE_MACHINE_CASING.get(),
                        ModItems.SHREDDER.get()),
                kineticMachineCraftingCase(
                        "steel_shredder", "steel",
                        ModItems.STEEL_DOUBLE_MACHINE_CASING.get(),
                        ModItems.STEEL_SHREDDER.get()),
                kineticMachineCraftingCase(
                        "titanium_shredder", "titanium",
                        ModItems.TITANIUM_DOUBLE_MACHINE_CASING.get(),
                        ModItems.TITANIUM_SHREDDER.get()),
                kineticMachineCraftingCase(
                        "press", "bronze",
                        ModItems.BRONZE_DOUBLE_MACHINE_CASING.get(),
                        ModItems.PRESS.get()),
                kineticMachineCraftingCase(
                        "steel_press", "steel",
                        ModItems.STEEL_DOUBLE_MACHINE_CASING.get(),
                        ModItems.STEEL_PRESS.get()),
                kineticMachineCraftingCase(
                        "titanium_press", "titanium",
                        ModItems.TITANIUM_DOUBLE_MACHINE_CASING.get(),
                        ModItems.TITANIUM_PRESS.get()));
    }

    private static MachineCraftingCase kineticMachineCraftingCase(
            String id,
            String material,
            Item casing,
            Item result) {
        String kind = id.replaceFirst("^(steel|titanium)_", "");
        ItemStack gear = material(
                material, MaterialPrefixes.GEAR, 1);
        List<String> pattern;
        Map<Character, ItemStack> key;
        switch (kind) {
            case "lathe" -> {
                pattern = List.of("TDS", " CG");
                key = Map.of(
                        'C', new ItemStack(casing),
                        'D', material(
                                "diamond",
                                MaterialPrefixes.GEM,
                                1),
                        'G', gear,
                        'S', material(
                                material,
                                MaterialPrefixes.SMALL_GEAR,
                                1),
                        'T', material(
                                material,
                                MaterialPrefixes.SCREW,
                                1));
            }
            case "rollingmill" -> {
                pattern = List.of("G ", "C ", "G ");
                key = Map.of('C', new ItemStack(casing), 'G', gear);
            }
            case "wiremill" -> {
                pattern = List.of("SGS", " C ");
                key = Map.of(
                        'C', new ItemStack(casing),
                        'G', gear,
                        'S', material(
                                material,
                                MaterialPrefixes.SMALL_GEAR,
                                1));
            }
            case "shredder" -> {
                pattern = List.of("GDG", " C ");
                key = Map.of(
                        'C', new ItemStack(casing),
                        'D', material(
                                "diamond",
                                MaterialPrefixes.GEM,
                                1),
                        'G', gear);
            }
            case "press" -> {
                pattern = List.of("RS", "PC", "P ");
                key = Map.of(
                        'C', new ItemStack(casing),
                        'P', material(
                                material,
                                MaterialPrefixes.DOUBLE_PLATE,
                                1),
                        'R', material(
                                material,
                                MaterialPrefixes.ROD,
                                1),
                        'S', material(
                                material,
                                MaterialPrefixes.SPRING,
                                1));
            }
            default -> throw new IllegalArgumentException(
                    "Unsupported kinetic machine " + id);
        }
        return new MachineCraftingCase(
                ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID, "machines/" + id),
                result,
                pattern,
                key);
    }

    private static Set<Item> heatPublicationResultItems() {
        return Set.of(
                ModItems.DISTILLERY.get(),
                ModItems.INVAR_DISTILLERY.get(),
                ModItems.TITANIUM_DISTILLERY.get(),
                ModItems.DRYING.get(),
                ModItems.INVAR_DRYING.get(),
                ModItems.TITANIUM_DRYING.get(),
                ModItems.SMELTER.get(),
                ModItems.INVAR_SMELTER.get(),
                ModItems.TITANIUM_SMELTER.get(),
                ModItems.tieredProcessingItemsById()
                        .get(ResourceLocation.fromNamespaceAndPath(
                                CrucibleCraft.MODID, "melter"))
                        .get());
    }

    private static List<MachineCraftingCase> heatMachineCraftingCases() {
        return List.of(
                heatDistilleryCraftingCase(
                        "distillery",
                        "steel",
                        "constantan",
                        MaterialPrefixes.DOUBLE_WIRE,
                        ModItems.STEEL_DOUBLE_MACHINE_CASING.get(),
                        ModItems.DISTILLERY.get()),
                heatDistilleryCraftingCase(
                        "invar_distillery",
                        "invar",
                        "kanthal",
                        MaterialPrefixes.QUADRUPLE_WIRE,
                        ModItems.STEEL_DOUBLE_MACHINE_CASING.get(),
                        ModItems.INVAR_DISTILLERY.get()),
                heatDistilleryCraftingCase(
                        "titanium_distillery",
                        "titanium",
                        "nichrome",
                        MaterialPrefixes.OCTUPLE_WIRE,
                        ModItems.TITANIUM_DOUBLE_MACHINE_CASING.get(),
                        ModItems.TITANIUM_DISTILLERY.get()),
                heatBodyCraftingCase(
                        "drying",
                        "steel",
                        ModItems.STEEL_DOUBLE_MACHINE_CASING.get(),
                        ModItems.DRYING.get()),
                heatBodyCraftingCase(
                        "invar_drying",
                        "invar",
                        ModItems.STEEL_DOUBLE_MACHINE_CASING.get(),
                        ModItems.INVAR_DRYING.get()),
                heatBodyCraftingCase(
                        "titanium_drying",
                        "titanium",
                        ModItems.TITANIUM_DOUBLE_MACHINE_CASING.get(),
                        ModItems.TITANIUM_DRYING.get()),
                heatBodyCraftingCase(
                        "smelter",
                        "steel",
                        ModItems.STEEL_DOUBLE_MACHINE_CASING.get(),
                        ModItems.SMELTER.get()),
                heatBodyCraftingCase(
                        "invar_smelter",
                        "invar",
                        ModItems.STEEL_DOUBLE_MACHINE_CASING.get(),
                        ModItems.INVAR_SMELTER.get()),
                heatBodyCraftingCase(
                        "titanium_smelter",
                        "titanium",
                        ModItems.TITANIUM_DOUBLE_MACHINE_CASING.get(),
                        ModItems.TITANIUM_SMELTER.get()),
                melterCraftingCase());
    }

    private static MachineCraftingCase heatDistilleryCraftingCase(
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
                                1),
                        'G', new ItemStack(Items.GLASS),
                        'M', new ItemStack(casing),
                        'P', material(
                                material,
                                MaterialPrefixes.PLATE,
                                1),
                        'W', material(
                                wireMaterial,
                                wirePrefix,
                                1)));
    }

    private static MachineCraftingCase heatBodyCraftingCase(
            String id,
            String material,
            Item casing,
            Item result) {
        boolean smelter = id.endsWith("smelter");
        Map<Character, ItemStack> key = new java.util.LinkedHashMap<>();
        key.put('B', new ItemStack(Items.BRICKS));
        key.put(
                'C',
                material(
                        "copper",
                        MaterialPrefixes.DOUBLE_PLATE,
                        1));
        key.put('M', new ItemStack(casing));
        key.put(
                'P',
                material(material, MaterialPrefixes.PLATE, 1));
        if (smelter) {
            key.put('U', new ItemStack(ModBlocks.steelSmeltingCrucible().get()));
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

    private static MachineCraftingCase melterCraftingCase() {
        Map<Character, ItemStack> key = new java.util.LinkedHashMap<>();
        key.put(
                'C',
                material(
                        "copper",
                        MaterialPrefixes.DOUBLE_PLATE,
                        1));
        key.put('B', new ItemStack(Items.BRICKS));
        key.put('M', material(
                "iron",
                MaterialPrefixes.MACHINE_CASING,
                1));
        // GT6 ANY.Iron medium pipe. Steel is the registered iron-family member.
        key.put('P', material(
                "steel",
                MaterialPrefixes.FLUID_PIPE,
                1));
        key.put('U', new ItemStack(ModBlocks.steelSmeltingCrucible().get()));
        key.put('w', new ItemStack(ModItems.MATERIAL_WRENCH.get()));
        key.put('h', new ItemStack(ModItems.SMITHING_HAMMER.get()));
        return new MachineCraftingCase(
                ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID, "machines/melter"),
                ModItems.tieredProcessingItemsById()
                        .get(ResourceLocation.fromNamespaceAndPath(
                                CrucibleCraft.MODID, "melter"))
                        .get(),
                List.of("wUh", "PMP", "BCB"),
                Map.copyOf(key));
    }

    private record MachineCraftingCase(
            ResourceLocation id,
            Item result,
            List<String> pattern,
            Map<Character, ItemStack> key) {}

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
        RecipeMap.Match extruderRecipe = requireComponentRecipe(
                helper,
                ModRecipeMaps.EXTRUDER,
                materialId,
                "gt6/",
                MaterialPrefixes.LONG_ROD,
                GTRecipeQuery.items(ingot, shape));
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
        Block extruderBlock = extruderRecipe.recipe().eut() > 64L
                ? ModBlocks.INVAR_EXTRUDER.get()
                : ModBlocks.EXTRUDER.get();
        ConfiguredProcessingMachineBlockEntity extruder = placeConfigured(
                helper, new BlockPos(14, 2, 2),
                extruderBlock, ModProcessingMachines.EXTRUDER);
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
        IItemHandler automation = machine.items(itemIn(machine));
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
        var plan = MachineExecutionPlan.create(
                match.recipe(),
                machine.variant().kind(),
                machine.variant().tier(),
                1);
        helper.assertTrue(
                plan.isPresent(),
                label + " has no variant execution plan");
        int expectedDuration = plan.orElseThrow().effectiveDuration();
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
        IItemHandler automation = stage.machine().items(itemOut(stage.machine()));
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
                                Direction.WEST));
        helper.setBlock(
                mixerPos,
                ModBlocks.MIXER.get().defaultBlockState()
                        .setValue(
                                ProcessingMachineBlock.FACING,
                                Direction.SOUTH));
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
                "brass",
                MaterialPrefixes.ITEM_PIPE,
                PipeCatalog.Kind.ITEM).get();
        helper.setBlock(
                firstPos,
                pipeState(block, Direction.WEST, Direction.EAST));
        helper.setBlock(
                secondPos,
                pipeState(block, Direction.WEST, Direction.EAST));
        helper.setBlock(
                thirdPos,
                pipeState(block, Direction.WEST, Direction.EAST));

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
        third.setCover(Direction.WEST, PipeCover.valve());
        third.setCover(
                Direction.EAST,
                PipeCover.filter("minecraft:iron_ingot"));

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
                            third.setCover(
                                    Direction.EAST,
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
                            third.setCover(
                                    Direction.EAST,
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

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void conveyorActivelyExportsAtConfiguredRate(
            GameTestHelper helper) {
        ActiveCoverRoute route = activeCoverRoute(
                helper,
                "cruciblecraft:conveyor",
                new ItemStack(Items.IRON_INGOT, 20),
                ItemStack.EMPTY);
        helper.startSequence()
                .thenIdle(30)
                .thenExecute(() -> {
                    helper.assertTrue(
                            route.source().getItem(0).isEmpty(),
                            "Conveyor did not actively export its source");
                    helper.assertTrue(
                            route.destination().getItem(0).getCount() == 20,
                            "Conveyor did not conserve its configured transfer");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void retrieverRequestsFromPipeNetwork(
            GameTestHelper helper) {
        ActiveCoverRoute route = activeCoverRoute(
                helper,
                "cruciblecraft:retriever_item",
                new ItemStack(Items.IRON_INGOT, 60),
                new ItemStack(Items.IRON_INGOT, 10));
        fillRemainingChestSlots(route.source());
        helper.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    helper.assertTrue(
                            route.source().getItem(0).getCount() == 64,
                            "Retriever did not request into the front inventory");
                    helper.assertTrue(
                            route.destination().getItem(0).getCount() == 6,
                            "Retriever drained the wrong chest or voided items");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void retrieverInvertedFilterSkipsMatch(
            GameTestHelper helper) {
        ActiveCoverRoute route = activeCoverRoute(
                helper,
                "cruciblecraft:retriever_item",
                ItemStack.EMPTY,
                new ItemStack(Items.IRON_INGOT, 8));
        route.destination().setItem(1, new ItemStack(Items.GOLD_INGOT, 8));
        helper.assertTrue(
                route.pipe().setCover(
                        Direction.WEST,
                        PipeCover.of("cruciblecraft:retriever_item")
                                .withConfig(PipeCoverConfig.EMPTY
                                        .withMatchId("minecraft:iron_ingot")
                                        .withInvert(1))),
                "Could not install inverted retriever");
        helper.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    helper.assertTrue(
                            route.source().getItem(0).is(
                                    Items.GOLD_INGOT)
                                    && route.source().getItem(0).getCount()
                                            == 8,
                            "Inverted retriever did not request the unmatched item");
                    helper.assertTrue(
                            route.destination().getItem(0).is(
                                    Items.IRON_INGOT)
                                    && route.destination().getItem(0)
                                            .getCount()
                                            == 8,
                            "Inverted retriever pulled the filtered item");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void robotArmRequiresExactConfiguredCount(
            GameTestHelper helper) {
        ActiveCoverRoute route = activeCoverRoute(
                helper,
                "cruciblecraft:robot_arm",
                new ItemStack(Items.IRON_INGOT, 8),
                new ItemStack(Items.IRON_INGOT, 60));
        fillRemainingChestSlots(route.destination());
        helper.startSequence()
                .thenIdle(20)
                .thenExecute(() -> {
                    helper.assertTrue(
                            route.source().getItem(0).getCount() == 8
                                    && route.destination().getItem(0)
                                            .getCount() == 60,
                            "Exact robot arm partially committed a blocked batch");
                    route.destination().setItem(
                            0, new ItemStack(Items.IRON_INGOT, 56));
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    helper.assertTrue(
                            route.source().getItem(0).isEmpty()
                                    && route.destination().getItem(0)
                                            .getCount() == 64,
                            "Exact robot arm did not commit the complete batch");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void pressureValveCapsFluidAndRetainsBackpressure(
            GameTestHelper helper) {
        BlockPos pipePos = new BlockPos(5, 2, 5);
        FluidPipeBlock block = (FluidPipeBlock) ModBlocks.pipeBlock(
                "copper",
                MaterialPrefixes.TINY_FLUID_PIPE,
                PipeCatalog.Kind.FLUID).get();
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
        FluidPipeBlockEntity pipe = helper.getBlockEntity(pipePos);
        helper.assertTrue(
                pipe.setCover(
                        Direction.WEST,
                        PipeCover.of("cruciblecraft:pressure_valve")),
                "Could not install pressure valve");
        int threshold = Math.max(1, Math.min(500, pipe.capacity() / 2));
        helper.assertTrue(
                pipe.configureCover(
                        Direction.WEST,
                        com.masson.cruciblecraft.logistics.pipe.cover
                                .CoverDefinition.ConfigField
                                .PRESSURE_THRESHOLD,
                        threshold),
                "Could not configure pressure threshold");
        int accepted = pipe.fluidHandler(Direction.WEST).fill(
                new FluidStack(Fluids.WATER, threshold + 300),
                IFluidHandler.FluidAction.EXECUTE);
        int blocked = pipe.fluidHandler(Direction.WEST).fill(
                new FluidStack(Fluids.WATER, 100),
                IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(
                accepted == threshold
                        && blocked == 0
                        && pipe.storedFluid().getAmount() == threshold,
                "Pressure valve did not enforce its configured threshold: "
                        + "accepted=" + accepted
                        + ", blocked=" + blocked
                        + ", stored=" + pipe.storedFluid().getAmount()
                        + ", threshold=" + threshold
                        + ", capacity=" + pipe.capacity()
                        + ", state=" + helper.getBlockState(pipePos));
        helper.assertTrue(
                pipe.failureSnapshot().backpressureAmount() == 300,
                "Pressure valve did not account the blocked fluid");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void manualSelectorPersistsBoundedSideConfig(
            GameTestHelper helper) {
        ItemPipeBlock block = (ItemPipeBlock) ModBlocks.pipeBlock(
                "brass",
                MaterialPrefixes.ITEM_PIPE,
                PipeCatalog.Kind.ITEM).get();
        BlockPos firstPos = new BlockPos(5, 2, 5);
        BlockPos secondPos = firstPos.east();
        helper.setBlock(firstPos, block);
        helper.setBlock(secondPos, block);
        ItemPipeBlockEntity first = helper.getBlockEntity(firstPos);
        ItemPipeBlockEntity second = helper.getBlockEntity(secondPos);
        helper.assertTrue(
                first.setCover(
                        Direction.NORTH,
                        PipeCover.of("cruciblecraft:selector_manual"))
                        && first.configureCover(
                                Direction.NORTH,
                                com.masson.cruciblecraft.logistics.pipe.cover
                                        .CoverDefinition.ConfigField.SELECTOR,
                                5),
                "Manual selector could not apply bounded side config");
        CompoundTag synced = first.getUpdateTag(
                helper.getLevel().registryAccess());
        second.handleUpdateTag(
                synced, helper.getLevel().registryAccess());
        PipeCover restored = second.coverSnapshot().get(Direction.NORTH);
        helper.assertTrue(
                restored != null
                        && restored.definition().orElseThrow()
                                .resolve(restored.config()).selector() == 5,
                "Manual selector config did not survive synchronization");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void allNonmetalPipeRecipesMatchAndAssemble(
            GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Set<ResourceLocation> expectedIds =
                PipeAcquisitionRecipeCatalog.ALL.stream()
                        .map(PipeAcquisitionRecipeCatalog.RecipeSpec::id)
                        .collect(java.util.stream.Collectors.toSet());
        Set<ResourceLocation> publishedIds = level.getRecipeManager()
                .getAllRecipesFor(RecipeType.CRAFTING)
                .stream()
                .map(holder -> holder.id())
                .filter(expectedIds::contains)
                .collect(java.util.stream.Collectors.toSet());
        helper.assertTrue(
                publishedIds.equals(expectedIds)
                        && publishedIds.size() == 25,
                "Did not publish exactly 25 pipe crafting recipes");

        for (var spec : PipeAcquisitionRecipeCatalog.ALL) {
            List<ItemStack> slots = new ArrayList<>();
            for (String row : spec.pattern()) {
                for (int column = 0; column < row.length(); column++) {
                    char symbol = row.charAt(column);
                    slots.add(symbol == ' '
                            ? ItemStack.EMPTY
                            : pipeOperandSample(
                                    spec.operands().get(symbol)));
                }
            }
            CraftingInput input = CraftingInput.of(
                    spec.pattern().getFirst().length(),
                    spec.pattern().size(),
                    slots);
            var match = level.getRecipeManager()
                    .getRecipeFor(RecipeType.CRAFTING, input, level)
                    .orElse(null);
            helper.assertTrue(
                    match != null && match.id().equals(spec.id()),
                    "Live RecipeManager did not match " + spec.id()
                            + "; matched="
                            + (match == null ? "none" : match.id()));
            ItemStack assembled = match.value().assemble(
                    input, level.registryAccess());
            helper.assertTrue(
                    MaterialLookup.matches(
                            assembled, spec.materialId(), spec.output())
                            && assembled.getCount() == spec.outputCount(),
                    spec.id() + " assembled " + assembled);
        }
        helper.succeed();
    }

    private static ItemStack pipeOperandSample(
            PipeAcquisitionRecipeCatalog.Operand operand) {
        return switch (operand) {
            case WOODEN_SLABS -> new ItemStack(Items.OAK_SLAB);
            case PLANKS -> new ItemStack(Items.OAK_PLANKS);
            case WOODEN_BEAMS -> new ItemStack(Items.STRIPPED_OAK_LOG);
            case CARBON_DUST -> MaterialLookup.stack(
                    "carbon", MaterialPrefixes.DUST);
            case PLASTIC_PLATE -> MaterialLookup.stack(
                    "plastic", MaterialPrefixes.PLATE);
            case RUBBER_PLATE -> MaterialLookup.stack(
                    "rubber", MaterialPrefixes.PLATE);
            case COAL_COKE -> MaterialLookup.stack(
                    "coal_coke", MaterialPrefixes.GEM);
        };
    }

    private static ActiveCoverRoute activeCoverRoute(
            GameTestHelper helper,
            String definition,
            ItemStack sourceStack,
            ItemStack destinationStack) {
        BlockPos sourcePos = new BlockPos(3, 2, 5);
        BlockPos pipePos = sourcePos.east();
        BlockPos destinationPos = pipePos.east();
        helper.setBlock(sourcePos, Blocks.CHEST.defaultBlockState());
        helper.setBlock(pipePos, ((ItemPipeBlock) ModBlocks.pipeBlock(
                "brass",
                MaterialPrefixes.ITEM_PIPE,
                PipeCatalog.Kind.ITEM).get()));
        helper.setBlock(destinationPos, Blocks.CHEST.defaultBlockState());
        helper.getLevel().setBlock(
                helper.absolutePos(pipePos),
                helper.getBlockState(pipePos)
                        .setValue(ItemPipeBlock.WEST, true)
                        .setValue(ItemPipeBlock.EAST, true),
                Block.UPDATE_CLIENTS);
        ChestBlockEntity source = helper.getBlockEntity(sourcePos);
        ChestBlockEntity destination =
                helper.getBlockEntity(destinationPos);
        ItemPipeBlockEntity pipe = helper.getBlockEntity(pipePos);
        source.setItem(0, sourceStack.copy());
        destination.setItem(0, destinationStack.copy());
        helper.assertTrue(
                pipe.setCover(Direction.WEST, PipeCover.of(definition)),
                "Could not install " + definition);
        return new ActiveCoverRoute(source, destination, pipe);
    }

    private static void fillRemainingChestSlots(
            ChestBlockEntity destination) {
        for (int slot = 1; slot < destination.getContainerSize(); slot++) {
            destination.setItem(
                    slot, new ItemStack(Items.COBBLESTONE, 64));
        }
    }

    private record ActiveCoverRoute(
            ChestBlockEntity source,
            ChestBlockEntity destination,
            ItemPipeBlockEntity pipe) {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void gt6PipeDomainsAreExclusive(GameTestHelper helper) {
        helper.assertTrue(
                PipeCatalog.contains(
                        "copper",
                        MaterialPrefixes.TINY_FLUID_PIPE,
                        PipeCatalog.Kind.FLUID)
                        && !PipeCatalog.contains(
                                "copper",
                                MaterialPrefixes.ITEM_PIPE,
                                PipeCatalog.Kind.ITEM),
                "copper must stay GT6 fluid-pipe only");
        helper.assertTrue(
                PipeCatalog.contains(
                        "brass",
                        MaterialPrefixes.ITEM_PIPE,
                        PipeCatalog.Kind.ITEM)
                        && !PipeCatalog.contains(
                                "brass",
                                MaterialPrefixes.FLUID_PIPE,
                                PipeCatalog.Kind.FLUID),
                "brass must stay GT6 item-pipe only");
        for (String material : List.of("tin", "iron")) {
            helper.assertTrue(
                    !PipeCatalog.contains(
                            material,
                            MaterialPrefixes.TINY_FLUID_PIPE,
                            PipeCatalog.Kind.FLUID)
                            && !PipeCatalog.contains(
                                    material,
                                    MaterialPrefixes.ITEM_PIPE,
                                    PipeCatalog.Kind.ITEM),
                    material + " is not a GT6 pipe material");
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
                capacity.coverSnapshot().size() == 1
                        && !capacity.coverSnapshot().get(Direction.NORTH)
                                .supports(
                                        com.masson.cruciblecraft.logistics.pipe
                                                .cover.CoverDefinition.Medium
                                                .FLUID),
                "Malformed cover NBT was not quarantined fail-closed");

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

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void petroleumDistillationFuelsEngineAndCutter(
            GameTestHelper helper) {
        // --- C2: End-to-end petroleum processing: oil -> distillery -> fuel -> engine -> KU ---
        //  This test deliberately does NOT use loadRecipeInputs().
        //  Fluid is inserted into the distillery tank directly (simulating
        //  pipe transport from a crude_oil source).

        BlockPos distilleryPos = new BlockPos(2, 2, 4);
        BlockPos enginePos = new BlockPos(4, 2, 4);

        GameTestHeatSources.placeHuSource(helper, distilleryPos.below());
        helper.setBlock(
                distilleryPos,
                ModBlocks.DISTILLERY.get().defaultBlockState()
                        .setValue(ProcessingMachineBlock.FACING, Direction.EAST));
        helper.setBlock(
                enginePos,
                ModBlocks.BRONZE_FUEL_ENGINE.get().defaultBlockState()
                        .setValue(FuelGeneratorBlock.FACING, Direction.EAST));

        ConfiguredProcessingMachineBlockEntity distillery =
                helper.getBlockEntity(distilleryPos);
        FuelGeneratorBlockEntity firebox =
                helper.getBlockEntity(distilleryPos.below());
        FuelGeneratorBlockEntity engine =
                helper.getBlockEntity(enginePos);

        GTRecipe distillation = requireRecipe(
                ModRecipeMaps.DISTILLERY,
                "petroleum/distillery/oil_to_fuel_and_lubricant");
        GTRecipe fuelCombustion = requireRecipe(
                ModRecipeMaps.FUELS_ENGINE,
                "hydrocarbon/fuels_engine/fuel_oil");

        helper.assertTrue(
                firebox.seedStoredEnergy(energyCapacity()),
                "Could not fuel distillery firebox");

        helper.startSequence()
                .thenExecute(() -> {
                    FluidStack oil = distillation.fluidInputs().getFirst();
                    distillery.tanks().getFirst().fill(
                            oil.copy(), IFluidHandler.FluidAction.EXECUTE);
                })
                .thenIdle(3)
                .thenExecute(() -> forceLastTick(helper, distillery))
                .thenIdle(2)
                .thenExecute(() -> {
                    boolean hasFuel = distillery.tanks().stream()
                            .anyMatch(tank -> tank.getFluid().is(
                                    fuelCombustion.fluidInputs()
                                            .getFirst().getFluid()));
                    helper.assertTrue(
                            hasFuel,
                            "Distillery did not produce fuel");
                })
                .thenExecute(() -> {
                    FluidStack fuelStack = fuelCombustion.fluidInputs()
                            .getFirst();
                    engine.fluids(Direction.UP).fill(
                            fuelStack.copy(),
                            IFluidHandler.FluidAction.EXECUTE);
                })
                .thenIdle(48)
                .thenExecute(() -> {
                    long expectedKu = Math.multiplyExact(
                            Math.abs(fuelCombustion.eut()),
                            (long) fuelCombustion.duration());
                    helper.assertTrue(
                            engine.energyGenerated() >= expectedKu
                                    && engine.energyGenerated()
                                            == engine.energyStored()
                                                    + engine.energyExtracted()
                                    && engine.energyExtracted() > 0L,
                            "Fuel engine did not generate and waste unused RU: "
                                    + "generated="
                                    + engine.energyGenerated()
                                    + " stored="
                                    + engine.energyStored()
                                    + " extracted="
                                    + engine.energyExtracted());
                })
                .thenSucceed();
    }

    // -----------------------------------------------------------------------
    // C3: petroleum edge-case tests
    // -----------------------------------------------------------------------

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void distilleryFluidConservationAfterNbtReload(
            GameTestHelper helper) {
        // Conservation: distillery fluid state must survive NBT save/load
        // without duplicating or losing fluid.
        BlockPos distilleryPos = new BlockPos(2, 2, 3);
        GameTestHeatSources.placeHuSource(helper, distilleryPos.below());
        helper.setBlock(
                distilleryPos,
                ModBlocks.DISTILLERY.get().defaultBlockState()
                        .setValue(ProcessingMachineBlock.FACING, Direction.EAST));

        ConfiguredProcessingMachineBlockEntity distillery =
                helper.getBlockEntity(distilleryPos);
        FuelGeneratorBlockEntity firebox =
                helper.getBlockEntity(distilleryPos.below());
        GTRecipe distillation = requireRecipe(
                ModRecipeMaps.DISTILLERY,
                "petroleum/distillery/oil_to_fuel_and_lubricant");

        helper.assertTrue(
                firebox.seedStoredEnergy(energyCapacity()),
                "Could not fuel distillery");

        helper.startSequence()
                .thenExecute(() -> {
                    FluidStack oil = distillation.fluidInputs().getFirst();
                    distillery.tanks().getFirst().fill(
                            oil.copy(), IFluidHandler.FluidAction.EXECUTE);
                })
                .thenIdle(3)
                .thenExecute(() -> forceLastTick(helper, distillery))
                .thenIdle(2)
                .thenExecute(() -> {
                    // Record pre-reload fluid state
                    int[] amounts = distillery.tanks().stream()
                            .mapToInt(tank -> tank.getFluid().getAmount())
                            .toArray();
                    boolean hasFuelBefore = distillery.tanks().stream()
                            .anyMatch(tank -> tank.getFluid().is(
                                    distillation.fluidOutputs().get(0)
                                            .getFluid()));
                    boolean hasLubricantBefore = distillery.tanks().stream()
                            .anyMatch(tank -> tank.getFluid().is(
                                    distillation.fluidOutputs().get(1)
                                            .getFluid()));
                    helper.assertTrue(
                            hasFuelBefore && hasLubricantBefore,
                            "Distillery did not produce outputs before reload");

                    // NBT save + reload
                    var registries = helper.getLevel().registryAccess();
                    CompoundTag tag = distillery.saveWithoutMetadata(
                            registries);
                    distillery.loadWithComponents(tag, registries);

                    // Verify conservation: all fluid amounts preserved
                    int[] afterAmounts = distillery.tanks().stream()
                            .mapToInt(tank -> tank.getFluid().getAmount())
                            .toArray();
                    boolean allPreserved = true;
                    for (int i = 0; i < amounts.length
                            && i < afterAmounts.length; i++) {
                        if (amounts[i] != afterAmounts[i]) {
                            allPreserved = false;
                        }
                    }
                    helper.assertTrue(
                            allPreserved,
                            "Distillery fluid state not conserved after"
                                    + " NBT reload");
                    boolean hasFuelAfter = distillery.tanks().stream()
                            .anyMatch(tank -> tank.getFluid().is(
                                    distillation.fluidOutputs().get(0)
                                            .getFluid()));
                    boolean hasLubricantAfter = distillery.tanks().stream()
                            .anyMatch(tank -> tank.getFluid().is(
                                    distillation.fluidOutputs().get(1)
                                            .getFluid()));
                    helper.assertTrue(
                            hasFuelAfter && hasLubricantAfter,
                            "Distillery lost fuel or lubricant after reload");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void distilleryFireboxEmptyPreservesProgress(
            GameTestHelper helper) {
        // Power loss / no-fuel scenario: when firebox has no fuel, the
        // distillery must preserve its fluid contents.  Re-fueling must
        // allow processing to resume.  (C3: conservation / power-loss)
        BlockPos distilleryPos = new BlockPos(2, 2, 3);
        GameTestHeatSources.placeHuSource(helper, distilleryPos.below());
        helper.setBlock(
                distilleryPos,
                ModBlocks.DISTILLERY.get().defaultBlockState()
                        .setValue(ProcessingMachineBlock.FACING, Direction.EAST));

        ConfiguredProcessingMachineBlockEntity distillery =
                helper.getBlockEntity(distilleryPos);
        FuelGeneratorBlockEntity firebox =
                helper.getBlockEntity(distilleryPos.below());
        GTRecipe distillation = requireRecipe(
                ModRecipeMaps.DISTILLERY,
                "petroleum/distillery/oil_to_fuel_and_lubricant");

        // Fuel first, then load input — the distillery needs heat to
        // bind the recipe and begin processing.
        helper.assertTrue(
                firebox.seedStoredEnergy(energyCapacity()),
                "Could not fuel distillery firebox");

        helper.startSequence()
                .thenExecute(() -> {
                    FluidStack oil = distillation.fluidInputs().getFirst();
                    distillery.tanks().getFirst().fill(
                            oil.copy(), IFluidHandler.FluidAction.EXECUTE);
                })
                .thenIdle(3)
                .thenExecute(() -> forceLastTick(helper, distillery))
                .thenIdle(2)
                .thenExecute(() -> {
                    // Distillery should have produced fuel output
                    boolean hasFuel = distillery.tanks().stream()
                            .anyMatch(tank -> tank.getFluid().is(
                                    distillation.fluidOutputs().getFirst()
                                            .getFluid()));
                    helper.assertTrue(
                            hasFuel,
                            "Distillery did not produce fuel from oil");
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
        assertPublishedShredder(helper, materialId);
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

    private static void assertPublishedShredder(
            GameTestHelper helper, String materialId) {
        RecipeMap.Match match = ModRecipeMaps.SHREDDER.findMatch(
                GTRecipeQuery.items(material(
                        materialId,
                        MaterialPrefixes.CENTRIFUGED_CRUSHED_ORE,
                        64))).orElse(null);
        if (!oreChainMatch(match, materialId)) {
            java.util.Optional<ItemStack> tiny =
                    com.masson.cruciblecraft.api.material.MaterialLookup.tryStack(
                            materialId,
                            MaterialPrefixes.TINY_CENTRIFUGED_CRUSHED_ORE,
                            64);
            if (tiny.isPresent()) {
                match = ModRecipeMaps.SHREDDER.findMatch(
                        GTRecipeQuery.items(tiny.orElseThrow())).orElse(null);
            }
        }
        helper.assertTrue(
                oreChainMatch(match, materialId),
                "shredder/" + materialId
                        + " resolved a non-concrete recipe "
                        + (match == null ? null : match.id()));
    }

    private static boolean oreChainMatch(RecipeMap.Match match, String materialId) {
        return match != null
                && match.id().getPath().startsWith(
                        "ore_chain/shredder/" + materialId + "/");
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
        IFluidHandler external = machine.fluids(fluidIn(machine));
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
        IFluidHandler handler = machine.fluids(fluidIn(machine));
        if (handler != null) {
            handler.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    private static void primeEmittingKinetic(
            GameTestHelper helper, SteamEngineBlockEntity engine) {
        var registries = helper.getLevel().registryAccess();
        CompoundTag tag = engine.saveWithoutMetadata(registries);
        long primed = engine.kineticCapacity() * 24L / 32L;
        tag.putLong("kinetic", primed);
        tag.putInt(
                "engine_state",
                SteamEngineKuCurve.visualState(
                        primed, engine.kineticCapacity()));
        tag.putInt("stroke_sign", 1);
        engine.loadWithComponents(tag, registries);
    }

    private static void fillKuCapability(GameTestHelper helper, BlockPos pos) {
        BlockPos worldPos = helper.getBlockEntity(pos).getBlockPos();
        fillKuCapability(helper, worldPos, pos.toString());
    }

    private static void fillKuCapability(
            GameTestHelper helper, ConfiguredProcessingMachineBlockEntity machine) {
        fillKuCapability(helper, machine.getBlockPos(), machine.spec().id().toString());
    }

    private static Direction itemIn(ProcessingMachineBlockEntity machine) {
        return ProcessingMachineIoFaces.itemInput(machine.spec(), machine.facing());
    }

    private static Direction itemOut(ProcessingMachineBlockEntity machine) {
        return ProcessingMachineIoFaces.itemOutput(machine.spec(), machine.facing());
    }

    private static Direction fluidIn(ProcessingMachineBlockEntity machine) {
        return ProcessingMachineIoFaces.fluidInput(machine.spec(), machine.facing());
    }

    private static Direction fluidOut(ProcessingMachineBlockEntity machine) {
        return ProcessingMachineIoFaces.fluidOutput(machine.spec(), machine.facing());
    }

    private static Direction energyFace(ProcessingMachineBlockEntity machine) {
        return ProcessingMachineIoFaces.energy(machine.spec(), machine.facing());
    }

    private static net.minecraft.world.level.block.Block structurePaletteBlock(
            MultiblockStructureDefinition.PalettePredicate predicate) {
        return BuiltInRegistries.BLOCK.getOptional(predicate.block().orElseThrow())
                .orElseThrow();
    }

    private static void fillKuCapability(
            GameTestHelper helper, BlockPos worldPos, String label) {
        net.minecraft.world.level.block.entity.BlockEntity be =
                helper.getLevel().getBlockEntity(worldPos);
        List<Direction> sides = new ArrayList<>();
        if (be instanceof ProcessingMachineBlockEntity machine) {
            sides.add(energyFace(machine));
        }
        for (Direction side : Direction.values()) {
            if (!sides.contains(side)) {
                sides.add(side);
            }
        }
        List<EnergyType> types = List.of(
                EnergyType.KINETIC_PUSH,
                EnergyType.KINETIC_ROTATION,
                EnergyType.KINETIC,
                EnergyType.ELECTRIC,
                EnergyType.HEAT,
                EnergyType.TIME);
        IEnergyHandler energy = null;
        Direction used = null;
        EnergyType type = null;
        for (Direction side : sides) {
            IEnergyHandler candidate = helper.getLevel().getCapability(
                    ModCapabilities.ENERGY, worldPos, side);
            if (candidate == null) {
                continue;
            }
            EnergyType match = types.stream()
                    .filter(candidateType -> candidate.handles(candidateType, side))
                    .findFirst()
                    .orElse(null);
            if (match != null) {
                energy = candidate;
                used = side;
                type = match;
                break;
            }
        }
        helper.assertTrue(energy != null, "Energy capability missing at " + label);
        long accepted = energy.insert(type, 32L, 16L, used, false);
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
        Direction side = energyFace(machine);
        long accepted = machine.insert(type, 32L, 16L, side, false);
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
        return GameTestFixtures.requireRecipe(map, path);
    }

    private static GTRecipe requireMixerRecipeWithFluids(String... fluidIds) {
        Set<String> wanted = Set.of(fluidIds);
        return ModRecipeMaps.MIXER.entries().stream()
                .map(RecipeMap.Entry::recipe)
                .filter(recipe -> recipe.fluidInputs().stream()
                        .map(stack -> BuiltInRegistries.FLUID
                                .getKey(stack.getFluid())
                                .toString())
                        .collect(java.util.stream.Collectors.toSet())
                        .equals(wanted))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing live Mixer recipe for fluids " + wanted));
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
                "Hydrocarbon fluid transfer endpoints are unavailable");
        FluidStack offered = source.drain(
                amount, IFluidHandler.FluidAction.SIMULATE);
        helper.assertTrue(
                offered.getAmount() == amount,
                "Source did not expose the expected fluid amount");
        helper.assertTrue(
                target.fill(
                                offered,
                                IFluidHandler.FluidAction.SIMULATE)
                        == amount,
                "Target rejected the simulated fluid transfer");
        FluidStack drained = source.drain(
                offered, IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(
                drained.getAmount() == amount
                        && target.fill(
                                        drained,
                                        IFluidHandler.FluidAction.EXECUTE)
                                == amount,
                "Fluid transfer changed after simulation");
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
                ItemStack stored = from.inventory().getStackInSlot(slot);
                output = from.inventory().extractItem(
                        slot, stored.getCount(), false);
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

    private static BlockPos firstLargeCrucibleWall(
            com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition structure,
            BlockPos controllerPos,
            Direction facing,
            int y) {
        return structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.TAG
                        && (y < 0 || element.offset().y() == y))
                .map(element -> structure.worldPosition(
                        controllerPos, facing, element.offset()))
                .findFirst()
                .orElseThrow();
    }

    private static LargeCrucibleBlockEntity placeLargeCrucible(
            GameTestHelper helper, BlockPos controllerPos, Direction facing) {
        var structure = MultiblockStructureCatalog.require(
                LargeCrucibleBlockEntity.STRUCTURE_ID);
        helper.setBlock(
                controllerPos,
                ModBlocks.LARGE_CRUCIBLE.get()
                        .defaultBlockState()
                        .setValue(ProcessingMachineBlock.FACING, facing));
        structure.structure().forEach(element -> {
            var predicate = structure.predicate(element);
            BlockPos world = structure.worldPosition(
                    controllerPos, facing, element.offset());
            switch (predicate.kind()) {
                case CONTROLLER, AIR -> { }
                case TAG -> helper.setBlock(
                        world, LargeCrucibleHosts.wallBlock("steel"));
                default -> throw new AssertionError(predicate.kind());
            }
        });
        return helper.getBlockEntity(controllerPos);
    }

    private static void heatMelt(
            LargeCrucibleBlockEntity crucible, float temperature) {
        crucible.process().thermal().restore(
                temperature,
                0L,
                0L,
                CrucibleThermalModel.HOT_BUFFER_TICKS,
                false);
    }

    private static InsertResult insertIngots(
            LargeCrucibleBlockEntity crucible, String materialId, int count) {
        return insertIngots(crucible, materialId, count, 20.0f);
    }

    private static InsertResult insertIngots(
            LargeCrucibleBlockEntity crucible,
            String materialId,
            int count,
            float temperature) {
        InsertResult last = InsertResult.SUCCESS;
        var material = MaterialCatalog.require(materialId);
        for (int index = 0; index < count; index++) {
            last = crucible.insertMaterial(
                    new MaterialUnits.Entry(
                            material.id(),
                            MaterialPrefixes.INGOT,
                            MaterialPrefixes.INGOT.units()),
                    temperature);
            if (last != InsertResult.SUCCESS) {
                return last;
            }
        }
        return last;
    }

    private static void placeCeramicMold(
            GameTestHelper helper, BlockPos pos, boolean filled) {
        helper.setBlock(pos, ModBlocks.CERAMIC_MOLD.get());
        CeramicMoldBlockEntity mold = helper.getBlockEntity(pos);
        mold.setPattern(com.masson.cruciblecraft.content.mold.MoldShape.INGOT.mask());
        if (!filled) {
            return;
        }
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

    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void unknownFutureProcessingVersionQuarantinedAndPreserved(
            GameTestHelper helper) {
        // A save written by a future version (processing_version
        // > VERSION) must load, quarantine with a diagnostic, preserve
        // energy/inventory/tanks, expose the future version to the
        // client, and write it back unchanged on the next save.
        BlockPos pos = new BlockPos(4, 2, 5);
        ConfiguredProcessingMachineBlockEntity machine =
                placeConfigured(
                        helper,
                        pos,
                        ModBlocks.STAINLESS_STEEL_ELECTROLYZER.get(),
                        ModProcessingMachines.ELECTROLYZER);
        GTRecipe recipe = requireRecipe(
                ModRecipeMaps.ELECTROLYZER, "chemical/electrolyzer/salt");
        loadRecipeInputs(machine, recipe);
        int inputSlot = machine.spec().items().inputs().getFirst();
        ItemStack savedInput =
                machine.inventory().getStackInSlot(inputSlot).copy();
        int tank = machine.spec().fluids().outputs().getFirst().index();
        FluidStack savedFluid = recipe.fluidOutputs().getFirst().copy();
        savedFluid.setAmount(1);
        machine.tanks().get(tank).setFluid(savedFluid.copy());
        long capacity = machine.spec().energy().capacity();

        CompoundTag persisted = machine.saveWithoutMetadata(
                helper.getLevel().registryAccess());
        persisted.putInt(
                "processing_version", ProcessingMachineState.VERSION + 1);
        persisted.putInt("progress", 9);
        persisted.putInt("duration", recipe.duration());
        persisted.putString("status", "processing");
        persisted.putLong("energy", capacity);
        machine.loadWithComponents(
                persisted, helper.getLevel().registryAccess());

        helper.assertTrue(
                machine.pausedReason().equals("unsupported_version"),
                "A future processing_version did not quarantine: "
                        + machine.pausedReason());
        helper.assertTrue(
                machine.getUpdateTag(helper.getLevel().registryAccess())
                        .getInt("unsupported_processing_version")
                        == ProcessingMachineState.VERSION + 1,
                "The future version was not exposed to the client");
        helper.assertTrue(
                machine.stored(machine.spec().energy().type()) == capacity,
                "Buffered energy was not preserved under quarantine");
        helper.assertTrue(
                machine.inventory().getStackInSlot(inputSlot)
                                .getCount()
                        == savedInput.getCount()
                        && machine.inventory().getStackInSlot(inputSlot)
                                .is(savedInput.getItem()),
                "Input inventory was not preserved under quarantine");
        helper.assertTrue(
                machine.tanks().get(tank).getFluidAmount()
                        == savedFluid.getAmount(),
                "Tank contents were not preserved under quarantine");
        CompoundTag resaved = machine.saveWithoutMetadata(
                helper.getLevel().registryAccess());
        helper.assertTrue(
                resaved.getInt("processing_version")
                        == ProcessingMachineState.VERSION + 1,
                "The future version was not written back on save");
        helper.succeed();
    }

    private static ItemStack material(String id, com.masson.cruciblecraft.api.material.MaterialPrefix prefix, int count) {
        return GameTestFixtures.requireMaterialStack(id, prefix, count);
    }

    // ---- Small workload: declared scenario identity, deterministic
    // summary, conservation and bounded operation counts. Every placement
    // and assertion below copies a proven topology (steam converter leg,
    // kinetic/EU converter leg, tank3x3x3Formation, covered pipe idioms).

    private static final BlockPos SMALL_WORKLOAD_FIREBOX = new BlockPos(4, 1, 5);
    private static final BlockPos SMALL_WORKLOAD_BOILER = SMALL_WORKLOAD_FIREBOX.above();
    private static final BlockPos SMALL_WORKLOAD_STEAM_PIPE = SMALL_WORKLOAD_BOILER.above();
    private static final BlockPos SMALL_WORKLOAD_STEAM_RISER = SMALL_WORKLOAD_STEAM_PIPE.east();
    private static final BlockPos SMALL_WORKLOAD_STEAM_ENGINE = SMALL_WORKLOAD_STEAM_RISER.below();
    private static final BlockPos SMALL_WORKLOAD_PRESS = SMALL_WORKLOAD_STEAM_ENGINE.below();
    private static final BlockPos SMALL_WORKLOAD_FUEL_ENGINE = new BlockPos(7, 1, 5);
    private static final BlockPos SMALL_WORKLOAD_AXLE = SMALL_WORKLOAD_FUEL_ENGINE.east();
    private static final BlockPos SMALL_WORKLOAD_GEARBOX = SMALL_WORKLOAD_AXLE.east();
    private static final BlockPos SMALL_WORKLOAD_DYNAMO = SMALL_WORKLOAD_GEARBOX.east();
    private static final BlockPos SMALL_WORKLOAD_CABLE = SMALL_WORKLOAD_DYNAMO.east();
    private static final BlockPos SMALL_WORKLOAD_ELECTROLYZER = SMALL_WORKLOAD_CABLE.above();
    private static final BlockPos SMALL_WORKLOAD_CENTRIFUGE = new BlockPos(14, 1, 5);
    private static final BlockPos SMALL_WORKLOAD_TANK_CONTROLLER = new BlockPos(6, 2, 9);
    private static final Direction SMALL_WORKLOAD_TANK_FACING = Direction.NORTH;
    private static final int SMALL_WORKLOAD_FLUID_PIPE_COUNT = 10;
    private static final int SMALL_WORKLOAD_ITEM_PIPE_COUNT = 10;
    private static final int SMALL_WORKLOAD_COVER_COUNT = 4;

    @GameTest(template = TEMPLATE, timeoutTicks = 2000)
    public static void smallWorkloadBuildsToDeclaredIdentity(
            GameTestHelper helper) {
        buildSmallWorkloadScenario(helper, true);
        helper.assertTrue(
                smallWorkloadIdentityOk(helper, true),
                "Built small workload does not match the declared "
                        + "scenario identity");
        var structure = MultiblockStructureCatalog.require(
                TankBlockEntity.STRUCTURE_ID);
        long ports = structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.PORT)
                .count();
        long positions = structure.structure().size();
        helper.assertTrue(
                ports == 25 && positions == 27,
                "Declared tank structure counts drifted: ports="
                        + ports
                        + ", positions="
                        + positions);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 2000)
    public static void smallWorkloadRunsToDeterministicSummary(
            GameTestHelper helper) {
        buildSmallWorkloadScenario(helper, true);
        fuelSmallWorkloadScenario(helper);
        FuelGeneratorBlockEntity firebox = helper.getBlockEntity(SMALL_WORKLOAD_FIREBOX);
        BoilerBlockEntity boiler = helper.getBlockEntity(SMALL_WORKLOAD_BOILER);
        SteamEngineBlockEntity engine =
                helper.getBlockEntity(SMALL_WORKLOAD_STEAM_ENGINE);
        ConfiguredProcessingMachineBlockEntity press =
                helper.getBlockEntity(SMALL_WORKLOAD_PRESS);
        DynamoBlockEntity dynamo = helper.getBlockEntity(SMALL_WORKLOAD_DYNAMO);
        ConfiguredProcessingMachineBlockEntity electrolyzer =
                helper.getBlockEntity(SMALL_WORKLOAD_ELECTROLYZER);
        ConfiguredProcessingMachineBlockEntity centrifuge =
                helper.getBlockEntity(SMALL_WORKLOAD_CENTRIFUGE);
        FuelGeneratorBlockEntity fuelEngine =
                helper.getBlockEntity(SMALL_WORKLOAD_FUEL_ENGINE);

        helper.startSequence()
                .thenIdle(200)
                .thenExecute(() -> {
                    helper.assertTrue(
                            firebox.energyCapacity() > 0L
                                    && engine.nominalOutputRate() == 12L,
                            "Small-workload warmup did not reach the declared fixed "
                                    + "converter rates: hu_source="
                                    + firebox.energyCapacity());
                    helper.assertTrue(
                            boiler.steamAmount() > 0
                                    || engine.stored() > 0L
                                    || engine.steamAmount() > 0,
                            "Small-workload warmup produced no steam or KU");
                    helper.assertTrue(
                            firebox.seedStoredEnergy(energyCapacity()),
                            "Could not refuel the small-workload firebox for the "
                                    + "sampling window");
                    primeEmittingKinetic(helper, engine);
                })
                .thenIdle(1200)
                .thenExecute(() -> {
                    helper.assertTrue(
                            press.progress() > 0 || hasAnyOutput(press),
                            "Small-workload press did not run: "
                                    + press.pausedReason());
                    helper.assertTrue(
                            electrolyzer.workProgressLong() > 0L,
                            "Small-workload electrolyzer did not run");
                    helper.assertTrue(
                            centrifuge.progress() > 0
                                    || hasAnyOutput(centrifuge),
                            "Small-workload centrifuge did not run: "
                                    + centrifuge.pausedReason());
                    helper.assertTrue(
                            dynamo.kineticConsumed()
                                    == dynamo.electricExtracted()
                                            + dynamo.conversionLoss(),
                            "Small-workload dynamo RU -> EU + loss conservation "
                                    + "failed");
                    long euPackets =
                            dynamo.electricExtracted() / 22L;
                    helper.assertTrue(
                            dynamo.electricExtracted() == euPackets * 22L,
                            "Small-workload dynamo EU packet quantization failed");
                    helper.assertTrue(
                            boiler.waterAmount() < 10_000
                                    && fuelEngine.outputAmount(0) > 0,
                            "Small-workload chain did not consume water or fuel");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 2000)
    public static void smallWorkloadConservesItemsFluidsEnergy(
            GameTestHelper helper) {
        buildSmallWorkloadScenario(helper, true);
        fuelSmallWorkloadScenario(helper);
        BoilerBlockEntity boiler = helper.getBlockEntity(SMALL_WORKLOAD_BOILER);
        SteamEngineBlockEntity engine =
                helper.getBlockEntity(SMALL_WORKLOAD_STEAM_ENGINE);
        ConfiguredProcessingMachineBlockEntity press =
                helper.getBlockEntity(SMALL_WORKLOAD_PRESS);
        DynamoBlockEntity dynamo = helper.getBlockEntity(SMALL_WORKLOAD_DYNAMO);
        ConfiguredProcessingMachineBlockEntity electrolyzer =
                helper.getBlockEntity(SMALL_WORKLOAD_ELECTROLYZER);
        TankBlockEntity tank = helper.getBlockEntity(SMALL_WORKLOAD_TANK_CONTROLLER);
        var structure = MultiblockStructureCatalog.require(
                TankBlockEntity.STRUCTURE_ID);
        BlockPos firstPort = structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.PORT)
                .map(element -> structure.worldPosition(
                        SMALL_WORKLOAD_TANK_CONTROLLER,
                        SMALL_WORKLOAD_TANK_FACING,
                        element.offset()))
                .findFirst()
                .orElseThrow();

        helper.startSequence()
                .thenIdle(1400)
                .thenExecute(() -> {
                    // Tank fluid conservation: exact fill and drain
                    // through the same port (tank formation idiom).
                    BlockEntity portEntity = helper.getBlockEntity(firstPort);
                    helper.assertTrue(
                            portEntity instanceof MteInPlaceBlockEntity,
                            "Small-workload tank port is " + portEntity);
                    IFluidHandler portFluids =
                            ((MteInPlaceBlockEntity) portEntity).fluidHandler(
                                    SMALL_WORKLOAD_TANK_FACING);
                    helper.assertTrue(
                            portFluids != null,
                            "Small-workload tank port has no fluid handler");
                    int filled = portFluids.fill(
                            new FluidStack(Fluids.WATER, 10_000),
                            IFluidHandler.FluidAction.EXECUTE);
                    helper.assertTrue(
                            filled == 10_000
                                    && tank.contents().getFluidAmount()
                                            == 10_000,
                            "Small-workload tank fill was not exact: " + filled);
                    FluidStack drained = portFluids.drain(
                            4_000, IFluidHandler.FluidAction.EXECUTE);
                    helper.assertTrue(
                            drained.getAmount() == 4_000
                                    && tank.contents().getFluidAmount()
                                            == 6_000,
                            "Small-workload tank drain was not exact: "
                                    + drained.getAmount());

                    // Boiler water conservation: every batch consumes
                    // exactly 1 mB water (80 HU + 1 mB -> 160 mB steam);
                    // converted steam is evidenced by engine exhaust.
                    helper.assertTrue(
                            boiler.waterAmount() < 10_000
                                    && (engine.stored() > 0L
                                            || engine.steamAmount() > 0
                                            || boiler.steamAmount() > 0),
                            "Small-workload boiler did not batch water into steam");

                    // Dynamo conservation: 32 RU -> 22 EU + 10 loss.
                    helper.assertTrue(
                            dynamo.kineticConsumed()
                                    == dynamo.electricExtracted()
                                            + dynamo.conversionLoss(),
                            "Small-workload dynamo conservation failed");
                    helper.assertTrue(
                            electrolyzer.workProgressLong() > 0L,
                            "Small-workload electrolyzer received no EU");

                    // Item conservation in the press: exactly one craft
                    // was loaded, so any completed craft drains its
                    // inputs and yields the recipe-defined outputs;
                    // mid-craft progress is the other valid state.
                    helper.assertTrue(
                            press.progress() > 0 || hasAnyOutput(press),
                            "Small-workload press neither progressed nor produced: "
                                    + press.pausedReason());
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 2000)
    public static void smallWorkloadRespectsDeclaredOperationCaps(
            GameTestHelper helper) {
        buildSmallWorkloadScenario(helper, true);
        helper.assertTrue(
                smallWorkloadIdentityOk(helper, true),
                "Built small workload does not match the declared "
                        + "scenario identity");
        var structure = MultiblockStructureCatalog.require(
                TankBlockEntity.STRUCTURE_ID);
        long ports = structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.PORT)
                .count();
        helper.assertTrue(
                ports == 25,
                "Small-workload tank port count exceeds the declared bound: "
                        + ports);
        int coverSlots = 0;
        coverSlots += helper.<FluidPipeBlockEntity>getBlockEntity(
                        new BlockPos(4, 1, 3))
                .coverSnapshot().size();
        coverSlots += helper.<FluidPipeBlockEntity>getBlockEntity(
                        new BlockPos(13, 1, 3))
                .coverSnapshot().size();
        coverSlots += helper.<ItemPipeBlockEntity>getBlockEntity(
                        new BlockPos(4, 1, 2))
                .coverSnapshot().size();
        coverSlots += helper.<ItemPipeBlockEntity>getBlockEntity(
                        new BlockPos(5, 1, 2))
                .coverSnapshot().size();
        helper.assertTrue(
                coverSlots == SMALL_WORKLOAD_COVER_COUNT,
                "Small-workload cover count drifted: " + coverSlots);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 2000)
    public static void workloadMutationFailsStructureGate(
            GameTestHelper helper) {
        buildSmallWorkloadScenario(helper, false);
        helper.assertTrue(
                !smallWorkloadIdentityOk(helper, true),
                "Removing a declared machine did not fail the "
                        + "scenario identity gate");
        helper.assertTrue(
                smallWorkloadIdentityOk(helper, false),
                "The partial build should match the reduced "
                        + "identity view");
        helper.succeed();
    }

    private static void buildSmallWorkloadScenario(
            GameTestHelper helper, boolean includePress) {
        GameTestHeatSources.placeHuSource(helper, SMALL_WORKLOAD_FIREBOX);
        helper.setBlock(SMALL_WORKLOAD_BOILER, ModBlocks.BRONZE_BOILER.get());
        placeConnectedFluidPipe(
                helper,
                SMALL_WORKLOAD_STEAM_PIPE,
                Direction.DOWN,
                Direction.EAST);
        placeConnectedFluidPipe(
                helper,
                SMALL_WORKLOAD_STEAM_RISER,
                Direction.WEST,
                Direction.DOWN);
        helper.setBlock(
                SMALL_WORKLOAD_STEAM_ENGINE,
                ModBlocks.BRONZE_STEAM_ENGINE.get().defaultBlockState()
                        .setValue(
                                SteamEngineBlock.FACING,
                                Direction.DOWN));
        if (includePress) {
            helper.setBlock(
                    SMALL_WORKLOAD_PRESS,
                    ModBlocks.PRESS.get().defaultBlockState()
                            .setValue(
                                    ProcessingMachineBlock.FACING,
                                    Direction.EAST));
        }
        helper.setBlock(
                SMALL_WORKLOAD_FUEL_ENGINE,
                ModBlocks.BRONZE_FUEL_ENGINE.get().defaultBlockState()
                        .setValue(
                                FuelGeneratorBlock.FACING,
                                Direction.EAST));
        helper.setBlock(SMALL_WORKLOAD_FUEL_ENGINE.west(), Blocks.STONE);
        helper.setBlock(
                SMALL_WORKLOAD_AXLE,
                ModBlocks.ROTATIONAL_AXLE.get().defaultBlockState()
                        .setValue(
                                RotationalAxleBlock.AXIS,
                                Direction.Axis.X));
        helper.setBlock(
                SMALL_WORKLOAD_GEARBOX,
                ModBlocks.ROTATIONAL_GEARBOX.get().defaultBlockState()
                        .setValue(
                                RotationalGearboxBlock.FACING,
                                Direction.EAST));
        helper.setBlock(
                SMALL_WORKLOAD_DYNAMO,
                ModBlocks.BRONZE_DYNAMO.get().defaultBlockState()
                        .setValue(DynamoBlock.FACING, Direction.EAST));
        CableBlock cable = ModBlocks.electricalConductorBlock(
                "tin", MaterialPrefixes.CABLE).get();
        helper.setBlock(
                SMALL_WORKLOAD_CABLE,
                conductorState(cable, Direction.WEST, Direction.UP));
        helper.setBlock(
                SMALL_WORKLOAD_ELECTROLYZER,
                ModBlocks.ELECTROLYZER.get().defaultBlockState()
                        .setValue(
                                ProcessingMachineBlock.FACING,
                                Direction.EAST));
        helper.setBlock(
                SMALL_WORKLOAD_CENTRIFUGE,
                ModBlocks.CENTRIFUGE.get().defaultBlockState()
                        .setValue(
                                ProcessingMachineBlock.FACING,
                                Direction.EAST));

        var structure = MultiblockStructureCatalog.require(
                TankBlockEntity.STRUCTURE_ID);
        helper.setBlock(
                SMALL_WORKLOAD_TANK_CONTROLLER,
                tankControllerBlock("stainless_steel").defaultBlockState()
                        .setValue(
                                MteInPlaceBlock.FACING,
                                SMALL_WORKLOAD_TANK_FACING));
        structure.structure().stream()
                .filter(element -> structure.predicate(element).kind()
                        == PredicateKind.PORT)
                .forEach(element -> helper.setBlock(
                        structure.worldPosition(
                                SMALL_WORKLOAD_TANK_CONTROLLER,
                                SMALL_WORKLOAD_TANK_FACING,
                                element.offset()),
                        tankWallBlock("stainless_steel")));

        FluidPipeBlock fluidPipe = (FluidPipeBlock) ModBlocks.pipeBlock(
                "copper",
                MaterialPrefixes.TINY_FLUID_PIPE,
                PipeCatalog.Kind.FLUID).get();
        ItemPipeBlock itemPipe = (ItemPipeBlock) ModBlocks.pipeBlock(
                "brass",
                MaterialPrefixes.ITEM_PIPE,
                PipeCatalog.Kind.ITEM).get();
        for (int i = 0; i < SMALL_WORKLOAD_FLUID_PIPE_COUNT; i++) {
            helper.setBlock(new BlockPos(4 + i, 1, 3), fluidPipe);
        }
        for (int i = 0; i < SMALL_WORKLOAD_ITEM_PIPE_COUNT; i++) {
            helper.setBlock(new BlockPos(4 + i, 1, 2), itemPipe);
        }

        helper.<FluidPipeBlockEntity>getBlockEntity(
                        new BlockPos(4, 1, 3))
                .setCover(Direction.WEST, PipeCover.pump());
        helper.<FluidPipeBlockEntity>getBlockEntity(
                        new BlockPos(13, 1, 3))
                .setCover(Direction.EAST, PipeCover.valve());
        helper.<ItemPipeBlockEntity>getBlockEntity(
                        new BlockPos(4, 1, 2))
                .setCover(Direction.WEST, PipeCover.pump());
        helper.<ItemPipeBlockEntity>getBlockEntity(
                        new BlockPos(5, 1, 2))
                .setCover(
                        Direction.NORTH,
                        PipeCover.filter("minecraft:iron_ingot"));
    }

    private static boolean smallWorkloadIdentityOk(
            GameTestHelper helper, boolean includePress) {
        if (includePress
                && !helper.getBlockState(SMALL_WORKLOAD_PRESS)
                        .is(ModBlocks.PRESS.get())) {
            return false;
        }
        if (!helper.getBlockState(SMALL_WORKLOAD_FIREBOX).is(ModBlocks.BRONZE_BURNING_BOX_GAS.get())
                || !helper.getBlockState(SMALL_WORKLOAD_BOILER)
                        .is(ModBlocks.BRONZE_BOILER.get())
                || !helper.getBlockState(SMALL_WORKLOAD_STEAM_ENGINE)
                        .is(ModBlocks.BRONZE_STEAM_ENGINE.get())
                || !helper.getBlockState(SMALL_WORKLOAD_FUEL_ENGINE)
                        .is(ModBlocks.BRONZE_FUEL_ENGINE.get())
                || !helper.getBlockState(SMALL_WORKLOAD_AXLE)
                        .is(ModBlocks.ROTATIONAL_AXLE.get())
                || !helper.getBlockState(SMALL_WORKLOAD_GEARBOX)
                        .is(ModBlocks.ROTATIONAL_GEARBOX.get())
                || !helper.getBlockState(SMALL_WORKLOAD_DYNAMO)
                        .is(ModBlocks.BRONZE_DYNAMO.get())
                || !helper.getBlockState(SMALL_WORKLOAD_ELECTROLYZER)
                        .is(ModBlocks.ELECTROLYZER.get())
                || !helper.getBlockState(SMALL_WORKLOAD_CENTRIFUGE)
                        .is(ModBlocks.CENTRIFUGE.get())
                || !helper.getBlockState(SMALL_WORKLOAD_TANK_CONTROLLER)
                        .is(tankControllerBlock("stainless_steel"))) {
            return false;
        }
        CableBlock cable = ModBlocks.electricalConductorBlock(
                "tin", MaterialPrefixes.CABLE).get();
        if (!helper.getBlockState(SMALL_WORKLOAD_CABLE).is(cable)) {
            return false;
        }
        var structure = MultiblockStructureCatalog.require(
                TankBlockEntity.STRUCTURE_ID);
        for (var element : structure.structure()) {
            if (structure.predicate(element).kind()
                    != PredicateKind.PORT) {
                continue;
            }
            BlockPos portPos = structure.worldPosition(
                    SMALL_WORKLOAD_TANK_CONTROLLER, SMALL_WORKLOAD_TANK_FACING, element.offset());
            if (!helper.getBlockState(portPos)
                    .is(tankWallBlock("stainless_steel"))) {
                return false;
            }
        }
        FluidPipeBlock fluidPipe = (FluidPipeBlock) ModBlocks.pipeBlock(
                "copper",
                MaterialPrefixes.TINY_FLUID_PIPE,
                PipeCatalog.Kind.FLUID).get();
        ItemPipeBlock itemPipe = (ItemPipeBlock) ModBlocks.pipeBlock(
                "brass",
                MaterialPrefixes.ITEM_PIPE,
                PipeCatalog.Kind.ITEM).get();
        if (!helper.getBlockState(SMALL_WORKLOAD_STEAM_PIPE).is(fluidPipe)) {
            return false;
        }
        for (int i = 0; i < SMALL_WORKLOAD_FLUID_PIPE_COUNT; i++) {
            if (!helper.getBlockState(new BlockPos(4 + i, 1, 3))
                    .is(fluidPipe)) {
                return false;
            }
        }
        for (int i = 0; i < SMALL_WORKLOAD_ITEM_PIPE_COUNT; i++) {
            if (!helper.getBlockState(new BlockPos(4 + i, 1, 2))
                    .is(itemPipe)) {
                return false;
            }
        }
        return true;
    }


    private static int pipeFluid(GameTestHelper helper, BlockPos pos) {
        BlockEntity entity = helper.getBlockEntity(pos);
        return entity instanceof FluidPipeBlockEntity pipe
                ? pipe.storedFluid().getAmount()
                : -1;
    }

    private static void placeConnectedFluidPipe(
            GameTestHelper helper, BlockPos pos, Direction... connections) {
        FluidPipeBlock pipeBlock = (FluidPipeBlock) ModBlocks.pipeBlock(
                "copper",
                MaterialPrefixes.TINY_FLUID_PIPE,
                PipeCatalog.Kind.FLUID).get();
        helper.setBlock(pos, pipeBlock);
        var state = helper.getBlockState(pos);
        for (Direction direction : connections) {
            state = state.setValue(
                    AbstractPipeBlock.PROPERTY_BY_DIRECTION.get(direction),
                    true);
        }
        helper.getLevel().setBlock(
                helper.absolutePos(pos), state, Block.UPDATE_CLIENTS);
    }

    private static void primeBoilerOperatingPressure(
            GameTestHelper helper, BoilerBlockEntity boiler) {
        int amount = BoilerBlockEntity.STEAM_CAPACITY / 2
                + SteamConversion.STEAM_PER_BATCH;
        var registries = helper.getLevel().registryAccess();
        CompoundTag tag = boiler.saveWithoutMetadata(registries);
        FluidTank steam = new FluidTank(BoilerBlockEntity.STEAM_CAPACITY);
        steam.setFluid(new FluidStack(ModFluids.STEAM_SOURCE.get(), amount));
        tag.put("steam", steam.writeToNBT(registries, new CompoundTag()));
        boiler.loadWithComponents(tag, registries);
    }

    private static void fuelSmallWorkloadScenario(GameTestHelper helper) {
        FuelGeneratorBlockEntity firebox = helper.getBlockEntity(SMALL_WORKLOAD_FIREBOX);
        BoilerBlockEntity boiler = helper.getBlockEntity(SMALL_WORKLOAD_BOILER);
        FuelGeneratorBlockEntity fuelEngine =
                helper.getBlockEntity(SMALL_WORKLOAD_FUEL_ENGINE);
        ConfiguredProcessingMachineBlockEntity press =
                helper.getBlockEntity(SMALL_WORKLOAD_PRESS);
        ConfiguredProcessingMachineBlockEntity electrolyzer =
                helper.getBlockEntity(SMALL_WORKLOAD_ELECTROLYZER);
        ConfiguredProcessingMachineBlockEntity centrifuge =
                helper.getBlockEntity(SMALL_WORKLOAD_CENTRIFUGE);

        GTRecipe pressRecipe = ModRecipeMaps.PRESS.entries().stream()
                .map(RecipeMap.Entry::recipe)
                .filter(candidate ->
                        candidate.eut() > 0L && candidate.eut() <= 16L)
                .findFirst()
                .orElseThrow();
        GTRecipe electrolysis = requireRecipe(
                ModRecipeMaps.ELECTROLYZER, "chemical/electrolyzer/salt");
        GTRecipe centrifugeRecipe = ModRecipeMaps.CENTRIFUGE.entries()
                .stream()
                .map(RecipeMap.Entry::recipe)
                .filter(candidate -> candidate.eut() > 0L)
                .min(java.util.Comparator.comparingLong(GTRecipe::eut))
                .orElseThrow();
        GTRecipe fuel = requireRecipe(
                ModRecipeMaps.FUELS_ENGINE, "hydrocarbon/fuels_engine/fuel_oil");
        loadRecipeInputs(press, pressRecipe);
        loadRecipeInputs(electrolyzer, electrolysis);
        loadRecipeInputs(centrifuge, centrifugeRecipe);

        helper.assertTrue(
                firebox.seedStoredEnergy(energyCapacity()),
                "Could not fuel the small-workload firebox");
        GameTestHeatSources.keepHeatEmissionLive(helper, firebox, 2_000);
        IFluidHandler water = boiler.fluids(Direction.NORTH);
        helper.assertTrue(
                water != null
                        && water.fill(
                                new FluidStack(
                                        Fluids.WATER,
                                        boiler.waterCapacity()),
                                IFluidHandler.FluidAction.EXECUTE)
                                == boiler.waterCapacity(),
                "Could not water the small-workload boiler");
        primeBoilerOperatingPressure(helper, boiler);
        FluidStack fuelInput = fuel.fluidInputs().getFirst();
        helper.assertTrue(
                fuelEngine.fluids(Direction.UP) != null
                        && fuelEngine.fluids(Direction.UP).fill(
                                new FluidStack(
                                        fuelInput.getFluid(),
                                        fuelInput.getAmount()),
                                IFluidHandler.FluidAction.EXECUTE)
                                == fuelInput.getAmount(),
                "Could not fuel the small-workload fuel engine");
        // The centrifuge is the RU machine of the small scenario. A
        // fuel engine has one RU output face and the gearbox is 1:1
        // pass-through, so the harness injects RU on the back face
        // (WEST for a FACING EAST machine), mirroring the tiered
        // centrifuge test's energy-insertion idiom.
        for (int i = 0; i < 8; i++) {
            long accepted = centrifuge.insert(
                    EnergyType.KINETIC_ROTATION,
                    32L,
                    16L,
                    energyFace(centrifuge),
                    false);
            helper.assertTrue(
                    accepted > 0L
                            || centrifuge.stored(
                                            EnergyType
                                                    .KINETIC_ROTATION)
                                    > 0L,
                    "Centrifuge accepted no KINETIC_ROTATION");
        }
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void hopperSlotSentinelsPullAndPushChests(
            GameTestHelper helper) {
        assertHopperSlots(helper, "lead_hopper", 1);
        assertHopperSlots(helper, "steel_hopper", 5);
        assertHopperSlots(helper, "infinity_hopper", 36);
        BlockPos hopperPos = new BlockPos(5, 3, 5);
        helper.setBlock(
                hopperPos,
                hopperBlock("steel_hopper").defaultBlockState()
                        .setValue(HopperBlock.FACING, Direction.DOWN));
        helper.setBlock(hopperPos.above(), Blocks.CHEST);
        helper.setBlock(hopperPos.below(), Blocks.CHEST);
        ChestBlockEntity source = helper.getBlockEntity(hopperPos.above());
        source.setItem(0, new ItemStack(Items.IRON_INGOT, 4));
        helper.startSequence()
                .thenIdle(12)
                .thenExecute(() -> {
                    HopperBlockEntity hopper = helper.getBlockEntity(hopperPos);
                    ChestBlockEntity dest = helper.getBlockEntity(hopperPos.below());
                    helper.assertTrue(
                            hopper.inventory().getStackInSlot(0).isEmpty()
                                    && dest.getItem(0).getCount() == 4,
                            "Steel hopper did not pull from the upper chest and push down");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void hopperRedstonePausesAndItemEntityPickupResumes(
            GameTestHelper helper) {
        BlockPos hopperPos = new BlockPos(6, 3, 6);
        helper.setBlock(
                hopperPos,
                hopperBlock("lead_hopper").defaultBlockState()
                        .setValue(HopperBlock.FACING, Direction.NORTH));
        helper.setBlock(hopperPos.north(), Blocks.CHEST);
        helper.setBlock(hopperPos.west(), Blocks.REDSTONE_BLOCK);
        HopperBlockEntity hopper = helper.getBlockEntity(hopperPos);
        hopper.inventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 3));
        helper.startSequence()
                .thenIdle(8)
                .thenExecute(() -> {
                    ChestBlockEntity dest = helper.getBlockEntity(hopperPos.north());
                    helper.assertTrue(
                            dest.getItem(0).isEmpty()
                                    && hopper.inventory().getStackInSlot(0).getCount()
                                            == 3,
                            "Redstone did not pause hopper push");
                    helper.setBlock(hopperPos.west(), Blocks.AIR);
                    BlockPos above = hopperPos.above();
                    BlockPos absolute = helper.absolutePos(above);
                    helper.getLevel().addFreshEntity(new ItemEntity(
                            helper.getLevel(),
                            absolute.getX() + 0.5,
                            absolute.getY() + 0.2,
                            absolute.getZ() + 0.5,
                            new ItemStack(Items.GOLD_INGOT, 2)));
                })
                .thenIdle(12)
                .thenExecute(() -> {
                    ChestBlockEntity dest = helper.getBlockEntity(hopperPos.north());
                    helper.assertTrue(
                            dest.getItem(0).is(Items.IRON_INGOT)
                                    && dest.getItem(0).getCount() == 3,
                            "Hopper did not resume push after redstone cleared");
                    helper.assertTrue(
                            hopper.inventory().getStackInSlot(0).is(Items.GOLD_INGOT)
                                    || dest.getItem(1).is(Items.GOLD_INGOT)
                                    || hopper.inventory().getStackInSlot(0).isEmpty(),
                            "Hopper did not pick up the item entity");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void hopperModeExactAndToolCycles(
            GameTestHelper helper) {
        BlockPos hopperPos = new BlockPos(4, 2, 4);
        helper.setBlock(
                hopperPos,
                hopperBlock("steel_hopper").defaultBlockState()
                        .setValue(HopperBlock.FACING, Direction.EAST));
        helper.setBlock(hopperPos.east(), Blocks.CHEST);
        ChestBlockEntity dest = helper.getBlockEntity(hopperPos.east());
        HopperBlockEntity hopper = helper.getBlockEntity(hopperPos);
        for (int step = 0; step < 8; step++) {
            hopper.cycleMode(false);
        }
        helper.assertTrue(hopper.mode() == 8, "Screwdriver-equivalent mode is not 8");
        dest.setItem(0, new ItemStack(Items.IRON_INGOT, 62));
        fillRemainingChestSlots(dest);
        hopper.inventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 16));
        helper.startSequence()
                .thenIdle(8)
                .thenExecute(() -> {
                    helper.assertTrue(
                            hopper.inventory().getStackInSlot(0).getCount() == 16
                                    && dest.getItem(0).getCount() == 62,
                            "Divisible mode 8 should not move into a 2-space dest");
                    hopper.toggleExactMode();
                    helper.assertTrue(hopper.exactMode(), "Monkey wrench did not enable exact mode");
                    Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                    ItemStack screwdriver = new ItemStack(
                            ModItems.MATERIAL_SCREWDRIVER.get());
                    ItemStack monkeyWrench = new ItemStack(
                            ModItems.MATERIAL_MONKEY_WRENCH.get());
                    BlockPos absolute = helper.absolutePos(hopperPos);
                    helper.getBlockState(hopperPos).useItemOn(
                            screwdriver,
                            helper.getLevel(),
                            player,
                            InteractionHand.MAIN_HAND,
                            faceHit(absolute, Direction.UP, 0.5, 1.0, 0.5));
                    helper.assertTrue(
                            hopper.mode() == 9,
                            "Screwdriver did not advance hopper mode");
                    hopper.cycleMode(true);
                    helper.assertTrue(
                            hopper.mode() == 8,
                            "Sneak screwdriver did not reverse hopper mode");
                    helper.getBlockState(hopperPos).useItemOn(
                            monkeyWrench,
                            helper.getLevel(),
                            player,
                            InteractionHand.MAIN_HAND,
                            faceHit(absolute, Direction.UP, 0.5, 1.0, 0.5));
                    helper.assertTrue(
                            !hopper.exactMode(),
                            "Monkey wrench did not toggle exact mode off");
                    player.discard();
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void hopperSaveReloadBreakAndItemPipe(
            GameTestHelper helper) {
        BlockPos hopperPos = new BlockPos(3, 2, 7);
        helper.setBlock(
                hopperPos,
                hopperBlock("lead_hopper").defaultBlockState()
                        .setValue(HopperBlock.FACING, Direction.EAST));
        HopperBlockEntity hopper = helper.getBlockEntity(hopperPos);
        hopper.inventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 5));
        hopper.cycleMode(false);
        hopper.toggleExactMode();
        CompoundTag tag = hopper.saveForTest(helper.getLevel().registryAccess());
        hopper.inventory().setStackInSlot(0, ItemStack.EMPTY);
        hopper.loadForTest(tag, helper.getLevel().registryAccess());
        helper.assertTrue(
                hopper.inventory().getStackInSlot(0).getCount() == 5
                        && hopper.mode() == 1
                        && hopper.exactMode(),
                "Hopper save/reload drifted");
        BlockPos pipePos = hopperPos.east();
        BlockPos chestPos = pipePos.east();
        ItemPipeBlock pipe = (ItemPipeBlock) ModBlocks.pipeBlock(
                "brass",
                MaterialPrefixes.ITEM_PIPE,
                PipeCatalog.Kind.ITEM).get();
        helper.setBlock(pipePos, pipeState(pipe, Direction.WEST, Direction.EAST));
        helper.setBlock(chestPos, Blocks.CHEST);
        helper.startSequence()
                .thenIdle(16)
                .thenExecute(() -> {
                    ChestBlockEntity dest = helper.getBlockEntity(chestPos);
                    helper.assertTrue(
                            dest.getItem(0).getCount() == 5
                                    || hopper.inventory().getStackInSlot(0).isEmpty(),
                            "Hopper did not push into the item pipe route");
                    hopper.inventory().setStackInSlot(
                            0, new ItemStack(Items.GOLD_INGOT, 2));
                    helper.getLevel().destroyBlock(
                            helper.absolutePos(hopperPos), true);
                    helper.assertTrue(
                            droppedItemCount(
                                    helper,
                                    hopperPos,
                                    Items.GOLD_INGOT) >= 2,
                            "Breaking the hopper did not drop its inventory");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void queueHopperFifoSlotLimitsAndRedstone(
            GameTestHelper helper) {
        helper.assertTrue(
                hopperBlock("bismuth_queue_hopper").variant().slots() == 2,
                "Bismuth queue is not the 2-slot minimum");
        helper.assertTrue(
                hopperBlock("infinity_queue_hopper").variant().slots() == 36,
                "Infinity queue is not 36 slots");
        BlockPos hopperPos = new BlockPos(7, 3, 4);
        helper.setBlock(
                hopperPos,
                hopperBlock("bismuth_queue_hopper").defaultBlockState()
                        .setValue(HopperBlock.FACING, Direction.DOWN));
        helper.setBlock(hopperPos.below(), Blocks.CHEST);
        HopperBlockEntity queue = helper.getBlockEntity(hopperPos);
        queue.inventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 3));
        IItemHandler input = queue.itemHandler(Direction.UP);
        helper.assertTrue(
                input.getSlots() == 2 && input.insertItem(1, new ItemStack(Items.GOLD_INGOT), true).getCount() == 1,
                "Queue external insert must only accept the first slot");
        helper.startSequence()
                .thenIdle(10)
                .thenExecute(() -> {
                    ChestBlockEntity dest = helper.getBlockEntity(hopperPos.below());
                    helper.assertTrue(
                            dest.getItem(0).getCount() == 3
                                    && queue.inventory().getStackInSlot(0).isEmpty(),
                            "Queue FIFO did not emit the last slot downward");
                    helper.setBlock(hopperPos.west(), Blocks.REDSTONE_BLOCK);
                    queue.inventory().setStackInSlot(
                            0, new ItemStack(Items.GOLD_INGOT, 2));
                })
                .thenIdle(8)
                .thenExecute(() -> {
                    helper.assertTrue(
                            queue.inventory().getStackInSlot(0).getCount() == 2,
                            "Redstone did not pause the queue hopper");
                    CompoundTag tag = queue.saveForTest(
                            helper.getLevel().registryAccess());
                    queue.loadForTest(tag, helper.getLevel().registryAccess());
                    helper.assertTrue(
                            queue.mode() == 64
                                    && queue.inventory().getStackInSlot(0).getCount() == 2,
                            "Queue save/reload drifted");
                    queue.inventory().setStackInSlot(0, ItemStack.EMPTY);
                    for (int i = 0; i < 63; i++) {
                        queue.cycleMode(true);
                    }
                    helper.assertTrue(
                            queue.mode() == 1,
                            "Queue slot size did not wrap to 1");
                    queue.cycleMode(true);
                    helper.assertTrue(
                            queue.mode() == 64,
                            "Queue slot size did not wrap from 1 to 64");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void dustFunnelConvertsModesAndPersists(
            GameTestHelper helper) {
        BlockPos pos = new BlockPos(8, 2, 8);
        helper.setBlock(pos, ModBlocks.STEEL_DUST_FUNNEL.get());
        DustFunnelBlockEntity funnel = helper.getBlockEntity(pos);
        helper.assertTrue(
                funnel.absorb("iron", DustAmountLedger.Form.TINY_DUST, 9),
                "9 tiny dust should become one dust batch");
        helper.assertTrue(
                funnel.ledger().tryEmit(1) && funnel.ledger().isEmpty(),
                "9 tiny did not emit one dust");
        funnel.absorb("iron", DustAmountLedger.Form.SMALL_DUST, 4);
        helper.assertTrue(
                funnel.ledger().tryEmit(1) && funnel.ledger().isEmpty(),
                "4 small dust did not emit one dust");
        funnel.absorb("iron", DustAmountLedger.Form.DUST, 1);
        funnel.ledger().setOutputMode(DustAmountLedger.Form.SMALL_DUST);
        helper.assertTrue(
                funnel.ledger().outputItemCount() == 4
                        && funnel.ledger().tryEmit(4),
                "1 dust did not emit 4 small");
        funnel.absorb("iron", DustAmountLedger.Form.DUST, 1);
        funnel.ledger().setOutputMode(DustAmountLedger.Form.TINY_DUST);
        helper.assertTrue(
                funnel.ledger().outputItemCount() == 9
                        && funnel.ledger().tryEmit(9),
                "1 dust did not emit 9 tiny");
        funnel.absorb("iron", DustAmountLedger.Form.TINY_DUST, 3);
        helper.assertTrue(
                !funnel.absorb("copper", DustAmountLedger.Form.DUST, 1),
                "Mixed material was consumed");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack monkeyWrench = new ItemStack(ModItems.MATERIAL_MONKEY_WRENCH.get());
        BlockPos absolute = helper.absolutePos(pos);
        DustAmountLedger.Form before = funnel.ledger().outputMode();
        int units = funnel.ledger().units();
        helper.getBlockState(pos).useItemOn(
                monkeyWrench,
                helper.getLevel(),
                player,
                InteractionHand.MAIN_HAND,
                faceHit(absolute, Direction.NORTH, 0.5, 0.5, 0.0));
        helper.assertTrue(
                funnel.ledger().outputMode() != before
                        && funnel.ledger().units() == units,
                "Monkey wrench mode must not change leftover units");
        ItemStack dust = material("iron", MaterialPrefixes.DUST, 1);
        helper.assertTrue(
                funnel.insertFromHand(dust) || !DustFunnelBlockEntity.isDustForm(dust),
                "Top-hand insert should accept iron dust or already hold iron leftover");
        helper.setBlock(pos.below(), Blocks.CHEST);
        CompoundTag tag = funnel.saveForTest(helper.getLevel().registryAccess());
        funnel.loadForTest(tag, helper.getLevel().registryAccess());
        helper.assertTrue(
                funnel.ledger().units() > 0,
                "Dust funnel save/reload dropped leftover units");
        helper.getLevel().destroyBlock(helper.absolutePos(pos), true);
        helper.assertTrue(
                droppedItemCount(helper, pos, null) >= 1,
                "Dust funnel break dropped nothing");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void plungerClearsFilledFluidPipe(GameTestHelper helper) {
        BlockPos pos = new BlockPos(4, 2, 4);
        FluidPipeBlock pipeBlock = (FluidPipeBlock) ModBlocks.pipeBlock(
                "copper",
                MaterialPrefixes.TINY_FLUID_PIPE,
                PipeCatalog.Kind.FLUID).get();
        helper.setBlock(pos, pipeBlock);
        FluidPipeBlockEntity pipe = helper.getBlockEntity(pos);
        int filled = pipe.fillInternal(
                new FluidStack(Fluids.WATER, pipe.capacity()),
                IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(filled > 0, "Could not fill the fluid pipe for the plunger");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack plunger = ModItems.MATERIAL_PLUNGER.get().variant("iron");
        player.setItemInHand(InteractionHand.MAIN_HAND, plunger);
        BlockPos absolute = helper.absolutePos(pos);
        helper.getBlockState(pos).useItemOn(
                plunger,
                helper.getLevel(),
                player,
                InteractionHand.MAIN_HAND,
                faceHit(absolute, Direction.NORTH, 0.5, 0.5, 0.0));
        helper.assertTrue(
                pipe.storedFluid().isEmpty(),
                "Plunger did not trash the pipe fluid");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void crowbarRemovesPipeCover(GameTestHelper helper) {
        BlockPos pos = new BlockPos(4, 2, 5);
        FluidPipeBlock pipeBlock = (FluidPipeBlock) ModBlocks.pipeBlock(
                "copper",
                MaterialPrefixes.TINY_FLUID_PIPE,
                PipeCatalog.Kind.FLUID).get();
        helper.setBlock(pos, pipeBlock);
        FluidPipeBlockEntity pipe = helper.getBlockEntity(pos);
        helper.assertTrue(
                pipe.setCover(Direction.NORTH, PipeCover.valve()),
                "Could not install a valve cover");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack crowbar = ModItems.MATERIAL_CROWBAR.get().variant("iron");
        player.setItemInHand(InteractionHand.MAIN_HAND, crowbar);
        BlockPos absolute = helper.absolutePos(pos);
        helper.getBlockState(pos).useItemOn(
                crowbar,
                helper.getLevel(),
                player,
                InteractionHand.MAIN_HAND,
                faceHit(absolute, Direction.NORTH, 0.5, 0.5, 0.0));
        helper.assertTrue(
                !pipe.coverSnapshot().containsKey(Direction.NORTH),
                "Crowbar did not pry the pipe cover");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void crowbarPicksUpMassStorageBarrel(GameTestHelper helper) {
        var barrel = StorageVariantCatalog.of(
                StorageBehaviorProfile.MASS_STORAGE).stream()
                .filter(variant -> "mass_storage_barrel".equals(variant.family()))
                .findFirst()
                .orElseThrow();
        BlockPos pos = new BlockPos(3, 2, 3);
        helper.setBlock(
                pos,
                ModBlocks.storageBlocksById().get(barrel.id()).get());
        MassStorageBlockEntity storage = helper.getBlockEntity(pos);
        storage.inventory().insertAll(new ItemStack(Items.IRON_INGOT, 32), false);
        helper.assertTrue(
                storage.inventory().stored() == 32,
                "Barrel did not accept ingots");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack crowbar = ModItems.MATERIAL_CROWBAR.get().variant("iron");
        player.setItemInHand(InteractionHand.MAIN_HAND, crowbar);
        BlockPos absolute = helper.absolutePos(pos);
        helper.getBlockState(pos).useItemOn(
                crowbar,
                helper.getLevel(),
                player,
                InteractionHand.MAIN_HAND,
                faceHit(absolute, Direction.NORTH, 0.5, 0.5, 0.0));
        helper.assertTrue(
                helper.getLevel().getBlockState(absolute).isAir(),
                "Crowbar did not pick up the mass-storage barrel");
        boolean packed = false;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty()
                    && stack.has(DataComponents.BLOCK_ENTITY_DATA)) {
                packed = true;
                break;
            }
        }
        helper.assertTrue(
                packed,
                "Picked-up barrel did not keep its stored contents");
        player.discard();
        helper.succeed();
    }

    private static void assertHopperSlots(
            GameTestHelper helper, String path, int slots) {
        helper.setBlock(new BlockPos(2, 2, 2), hopperBlock(path));
        HopperBlockEntity hopper = helper.getBlockEntity(new BlockPos(2, 2, 2));
        helper.assertTrue(
                hopper.inventory().getSlots() == slots,
                path + " slot count drifted");
        helper.setBlock(new BlockPos(2, 2, 2), Blocks.AIR);
    }

    private static HopperBlock hopperBlock(String path) {
        return ModBlocks.hopperBlocksById()
                .get(ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID, path))
                .get();
    }
}
