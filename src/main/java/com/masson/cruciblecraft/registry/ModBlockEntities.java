package com.masson.cruciblecraft.registry;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.blockentity.AnvilBlockEntity;
import com.masson.cruciblecraft.content.blockentity.BoilerBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CokeOvenBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CeramicMoldBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CableBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CrucibleBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CrusherBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.content.blockentity.DynamoBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ElectricMotorBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ElectricHeaterBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ElectricEngineBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FluidDepositExtractorBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FuelGeneratorBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FluidBedBurningBoxBlockEntity;
import com.masson.cruciblecraft.content.blockentity.GasCloudBlockEntity;
import com.masson.cruciblecraft.content.blockentity.GtTreeHoleBlockEntity;
import com.masson.cruciblecraft.content.blockentity.HopperBlockEntity;
import com.masson.cruciblecraft.content.blockentity.SensorBlockEntity;
import com.masson.cruciblecraft.content.blockentity.BookshelfBlockEntity;
import com.masson.cruciblecraft.content.blockentity.BottleCrateBlockEntity;
import com.masson.cruciblecraft.content.blockentity.DrawerBlockEntity;
import com.masson.cruciblecraft.content.blockentity.LockerBlockEntity;
import com.masson.cruciblecraft.content.blockentity.MassStorageBlockEntity;
import com.masson.cruciblecraft.content.blockentity.StorageInserterBlockEntity;
import com.masson.cruciblecraft.content.blockentity.DustFunnelBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ItemPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.LargeCentrifugeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.DistillationTowerBlockEntity;
import com.masson.cruciblecraft.content.blockentity.LargeBoilerBlockEntity;
import com.masson.cruciblecraft.content.blockentity.LargeCrucibleBlockEntity;
import com.masson.cruciblecraft.content.blockentity.LaserEngraverBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FusionReactorBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ReactorCoreBlockEntity;
import com.masson.cruciblecraft.content.blockentity.LogisticsCoreBlockEntity;
import com.masson.cruciblecraft.content.blockentity.LogisticsCoreWallBlockEntity;
import com.masson.cruciblecraft.content.blockentity.TankBlockEntity;
import com.masson.cruciblecraft.content.blockentity.MultiblockPortBlockEntity;
import com.masson.cruciblecraft.content.blockentity.RotationalAxleBlockEntity;
import com.masson.cruciblecraft.content.blockentity.RotationalGearboxBlockEntity;
import com.masson.cruciblecraft.content.blockentity.SteamEngineBlockEntity;
import com.masson.cruciblecraft.content.blockentity.SolidBurningBoxBlockEntity;
import com.masson.cruciblecraft.content.blockentity.SubsurfaceFluidDepositBlockEntity;
import com.masson.cruciblecraft.energy.battery.BatteryBlockEntity;
import com.masson.cruciblecraft.energy.transformer.TransformerBlockEntity;
import com.masson.cruciblecraft.energy.heatexchanger.HeatExchangerBlockEntity;

import com.masson.cruciblecraft.content.storage.StorageBehaviorProfile;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CrucibleCraft.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrucibleBlockEntity>> CRUCIBLE =
            BLOCK_ENTITIES.register(
                    "crucible",
                    () -> BlockEntityType.Builder.of(
                            CrucibleBlockEntity::new,
                            ModBlocks.CRUCIBLE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AnvilBlockEntity>> ANVIL =
            BLOCK_ENTITIES.register(
                    "anvil",
                    () -> BlockEntityType.Builder.of(
                            AnvilBlockEntity::new,
                            ModBlocks.ANVIL.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CokeOvenBlockEntity>>
            COKE_OVEN = BLOCK_ENTITIES.register(
                    "coke_oven",
                    () -> BlockEntityType.Builder.of(
                            CokeOvenBlockEntity::new,
                            ModBlocks.COKE_OVEN.get()).build(null));
    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<LargeCentrifugeBlockEntity>>
                    LARGE_CENTRIFUGE = BLOCK_ENTITIES.register(
                            "large_centrifuge",
                            () -> BlockEntityType.Builder.of(
                                    LargeCentrifugeBlockEntity::new,
                                    ModBlocks.LARGE_CENTRIFUGE.get())
                                    .build(null));
    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<MultiblockPortBlockEntity>>
                    MULTIBLOCK_PORT = BLOCK_ENTITIES.register(
                            "multiblock_port",
                            () -> BlockEntityType.Builder.of(
                                    MultiblockPortBlockEntity::new,
                                    ModBlocks.MULTIBLOCK_ITEM_FLUID_PORT.get(),
                                    ModBlocks.MULTIBLOCK_ENERGY_INPUT_PORT.get())
                                    .build(null));
    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<DistillationTowerBlockEntity>>
                    DISTILLATION_TOWER = BLOCK_ENTITIES.register(
                            "distillation_tower",
                            () -> BlockEntityType.Builder.of(
                                    DistillationTowerBlockEntity::new,
                                    ModBlocks.DISTILLATION_TOWER.get())
                                    .build(null));
    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<LargeBoilerBlockEntity>>
                    LARGE_BOILER = BLOCK_ENTITIES.register(
                            "large_boiler",
                            () -> BlockEntityType.Builder.of(
                                    LargeBoilerBlockEntity::new,
                                    ModBlocks.LARGE_BOILER.get())
                                    .build(null));
    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<TankBlockEntity>>
                    TANK_3X3X3 = BLOCK_ENTITIES.register(
                            "tank_3x3x3",
                            () -> BlockEntityType.Builder.of(
                                    TankBlockEntity::new,
                                    ModBlocks.TANK_3X3X3.get())
                                    .build(null));
    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<LargeCrucibleBlockEntity>>
                    LARGE_CRUCIBLE = BLOCK_ENTITIES.register(
                            "large_crucible",
                            () -> BlockEntityType.Builder.of(
                                    LargeCrucibleBlockEntity::new,
                                    ModBlocks.LARGE_CRUCIBLE.get())
                                    .build(null));
    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<LogisticsCoreBlockEntity>>
                    LOGISTICS_CORE = BLOCK_ENTITIES.register(
                            "logistics_core",
                            () -> BlockEntityType.Builder.of(
                                    LogisticsCoreBlockEntity::new,
                                    ModBlocks.LOGISTICS_CORE.get())
                                    .build(null));
    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<LogisticsCoreWallBlockEntity>>
                    LOGISTICS_CORE_WALL = BLOCK_ENTITIES.register(
                            "logistics_core_wall",
                            () -> BlockEntityType.Builder.of(
                                    LogisticsCoreWallBlockEntity::new,
                                    ModBlocks.GALVANIZED_STEEL_WALL.get())
                                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CeramicMoldBlockEntity>>
            CERAMIC_MOLD = BLOCK_ENTITIES.register(
                    "ceramic_mold",
                    () -> BlockEntityType.Builder.of(
                            CeramicMoldBlockEntity::new,
                            ModBlocks.CERAMIC_MOLD.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BoilerBlockEntity>> BOILER =
            BLOCK_ENTITIES.register(
                    "bronze_boiler",
                    () -> BlockEntityType.Builder.of(
                            BoilerBlockEntity::new,
                            ModBlocks.converterBlocks("boiler")).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SteamEngineBlockEntity>>
            STEAM_ENGINE = BLOCK_ENTITIES.register(
                    "bronze_steam_engine",
                    () -> BlockEntityType.Builder.of(
                            SteamEngineBlockEntity::new,
                            ModBlocks.converterBlocks("steam_engine")).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DynamoBlockEntity>>
            DYNAMO = BLOCK_ENTITIES.register(
                    "bronze_dynamo",
                    () -> BlockEntityType.Builder.of(
                            DynamoBlockEntity::new,
                            ModBlocks.converterBlocks("dynamo")).build(null));
    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<ElectricMotorBlockEntity>>
                    ELECTRIC_MOTOR = BLOCK_ENTITIES.register(
                            "electric_motor",
                            () -> BlockEntityType.Builder.of(
                                    ElectricMotorBlockEntity::new,
                                    ModBlocks.converterBlocks("electric_motor"))
                                    .build(null));
    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<ElectricHeaterBlockEntity>>
                    ELECTRIC_HEATER = BLOCK_ENTITIES.register(
                            "electric_heater",
                            () -> BlockEntityType.Builder.of(
                                    ElectricHeaterBlockEntity::new,
                                    ModBlocks.converterBlocks("electric_heater"))
                                    .build(null));
    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<ElectricEngineBlockEntity>>
                    ELECTRIC_ENGINE = BLOCK_ENTITIES.register(
                            "electric_engine",
                            () -> BlockEntityType.Builder.of(
                                    ElectricEngineBlockEntity::new,
                                    ModBlocks.converterBlocks("electric_engine"))
                                    .build(null));
    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<RotationalAxleBlockEntity>>
                    ROTATIONAL_AXLE = BLOCK_ENTITIES.register(
                            "rotational_axle",
                            () -> BlockEntityType.Builder.of(
                                    RotationalAxleBlockEntity::new,
                                    ModBlocks.ROTATIONAL_AXLE.get())
                                    .build(null));
    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<RotationalGearboxBlockEntity>>
                    ROTATIONAL_GEARBOX = BLOCK_ENTITIES.register(
                            "rotational_gearbox",
                            () -> BlockEntityType.Builder.of(
                                    RotationalGearboxBlockEntity::new,
                                    ModBlocks.ROTATIONAL_GEARBOX.get())
                                    .build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrusherBlockEntity>> CRUSHER =
            BLOCK_ENTITIES.register(
                    "bronze_crusher",
                    () -> BlockEntityType.Builder.of(
                            CrusherBlockEntity::new,
                            ModBlocks.BRONZE_CRUSHER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ConfiguredProcessingMachineBlockEntity>>
            PROCESSING_MACHINE = BLOCK_ENTITIES.register(
                    "processing_machine",
                    () -> BlockEntityType.Builder.of(
                            ConfiguredProcessingMachineBlockEntity::new,
                            ModBlocks.configuredProcessingBlocks()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CableBlockEntity>>
            CABLE = BLOCK_ENTITIES.register(
                    "cable",
                    () -> BlockEntityType.Builder.of(
                            CableBlockEntity::new,
                            ModBlocks.electricalConductorBlockArray()).build(null));
    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<LaserEngraverBlockEntity>>
            LASER_ENGRAVER = BLOCK_ENTITIES.register(
                    "laser_engraver",
                    () -> BlockEntityType.Builder.of(
                            LaserEngraverBlockEntity::new,
                            ModBlocks.LASER_ENGRAVER.get()).build(null));
    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<FusionReactorBlockEntity>>
            FUSION_REACTOR = BLOCK_ENTITIES.register(
                    "fusion_reactor",
                    () -> BlockEntityType.Builder.of(
                            FusionReactorBlockEntity::new,
                            ModBlocks.FUSION_REACTOR.get()).build(null));
    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<ReactorCoreBlockEntity>>
            REACTOR_CORE = BLOCK_ENTITIES.register(
                    "reactor_core",
                    () -> BlockEntityType.Builder.of(
                            ReactorCoreBlockEntity::new,
                            ModBlocks.REACTOR_CORE_1X1.get(),
                            ModBlocks.REACTOR_CORE_2X2.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidPipeBlockEntity>>
            FLUID_PIPE = BLOCK_ENTITIES.register(
                    "fluid_pipe",
                    () -> BlockEntityType.Builder.of(
                            FluidPipeBlockEntity::new,
                            ModBlocks.fluidPipeBlockArray()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ItemPipeBlockEntity>>
            ITEM_PIPE = BLOCK_ENTITIES.register(
                    "item_pipe",
                    () -> BlockEntityType.Builder.of(
                            ItemPipeBlockEntity::new,
                            ModBlocks.itemPipeBlockArray()).build(null));
    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<HopperBlockEntity>>
                    HOPPER = BLOCK_ENTITIES.register(
                            "hopper",
                            () -> BlockEntityType.Builder.of(
                                    HopperBlockEntity::new,
                                    ModBlocks.hopperBlockArray())
                                    .build(null));
    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<BookshelfBlockEntity>>
                    BOOKSHELF = BLOCK_ENTITIES.register(
                            "bookshelf",
                            () -> BlockEntityType.Builder.of(
                                    BookshelfBlockEntity::new,
                                    ModBlocks.storageBlockArray(
                                            StorageBehaviorProfile.BOOKSHELF))
                                    .build(null));
    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<BottleCrateBlockEntity>>
                    BOTTLE_CRATE = BLOCK_ENTITIES.register(
                            "bottle_crate",
                            () -> BlockEntityType.Builder.of(
                                    BottleCrateBlockEntity::new,
                                    ModBlocks.storageBlockArray(
                                            StorageBehaviorProfile.BOTTLE_CRATE))
                                    .build(null));
    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<DrawerBlockEntity>>
                    DRAWER = BLOCK_ENTITIES.register(
                            "drawer",
                            () -> BlockEntityType.Builder.of(
                                    DrawerBlockEntity::new,
                                    ModBlocks.storageBlockArray(
                                            StorageBehaviorProfile.DRAWER))
                                    .build(null));
    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<LockerBlockEntity>>
                    LOCKER = BLOCK_ENTITIES.register(
                            "locker",
                            () -> BlockEntityType.Builder.of(
                                    LockerBlockEntity::new,
                                    ModBlocks.lockerBlockArray())
                                    .build(null));
    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<MassStorageBlockEntity>>
                    MASS_STORAGE = BLOCK_ENTITIES.register(
                            "mass_storage",
                            () -> BlockEntityType.Builder.of(
                                    MassStorageBlockEntity::new,
                                    ModBlocks.massStorageBlockArray())
                                    .build(null));
    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<StorageInserterBlockEntity>>
                    STORAGE_INSERTER = BLOCK_ENTITIES.register(
                            "storage_inserter",
                            () -> BlockEntityType.Builder.of(
                                    StorageInserterBlockEntity::new,
                                    ModBlocks.storageBlockArray(
                                            StorageBehaviorProfile.STORAGE_INSERTER))
                                    .build(null));
    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<DustFunnelBlockEntity>>
                    DUST_FUNNEL = BLOCK_ENTITIES.register(
                            "steel_dust_funnel",
                            () -> BlockEntityType.Builder.of(
                                    DustFunnelBlockEntity::new,
                                    ModBlocks.STEEL_DUST_FUNNEL.get())
                                    .build(null));
    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<SubsurfaceFluidDepositBlockEntity>>
                    SUBSURFACE_FLUID_DEPOSIT = BLOCK_ENTITIES.register(
                            "subsurface_fluid_deposit",
                            () -> BlockEntityType.Builder.of(
                                    SubsurfaceFluidDepositBlockEntity::new,
                                    ModBlocks.SUBSURFACE_FLUID_DEPOSIT.get())
                                    .build(null));
    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<FluidDepositExtractorBlockEntity>>
                    FLUID_DEPOSIT_EXTRACTOR = BLOCK_ENTITIES.register(
                            "fluid_deposit_extractor",
                            () -> BlockEntityType.Builder.of(
                                    FluidDepositExtractorBlockEntity::new,
                                    ModBlocks.FLUID_DEPOSIT_EXTRACTOR.get())
                                    .build(null));
    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<GasCloudBlockEntity>>
                    GAS_CLOUD = BLOCK_ENTITIES.register(
                            "gas_cloud",
                            () -> BlockEntityType.Builder.of(
                                    GasCloudBlockEntity::new,
                                    ModBlocks.GAS_CLOUD.get())
                                    .build(null));
    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<FuelGeneratorBlockEntity>>
                    FUEL_GENERATOR = BLOCK_ENTITIES.register(
                            "fuel_generator",
                            () -> BlockEntityType.Builder.of(
                                    FuelGeneratorBlockEntity::new,
                                    ModBlocks.converterBlocks(
                                            "fuel_engine",
                                            "fluid_burning_box"))
                                    .build(null));
    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<SolidBurningBoxBlockEntity>>
                    SOLID_BURNING_BOX = BLOCK_ENTITIES.register(
                            "solid_burning_box",
                            () -> BlockEntityType.Builder.of(
                                    SolidBurningBoxBlockEntity::new,
                                    ModBlocks.converterBlocks(
                                            "solid_burning_box"))
                                    .build(null));
    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<FluidBedBurningBoxBlockEntity>>
                    FLUID_BED_BURNING_BOX = BLOCK_ENTITIES.register(
                            "fluid_bed_burning_box",
                            () -> BlockEntityType.Builder.of(
                                    FluidBedBurningBoxBlockEntity::new,
                                    ModBlocks.converterBlocks(
                                            "fluid_bed_burning_box"))
                                    .build(null));
    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<BatteryBlockEntity>>
                    BATTERY = BLOCK_ENTITIES.register(
                            "battery",
                            () -> BlockEntityType.Builder.of(
                                    BatteryBlockEntity::new,
                                    ModBlocks.batteryBlockArray())
                                    .build(null));
    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<TransformerBlockEntity>>
                    TRANSFORMER = BLOCK_ENTITIES.register(
                            "transformer",
                            () -> BlockEntityType.Builder.of(
                                    TransformerBlockEntity::new,
                                    ModBlocks.transformerBlockArray())
                                    .build(null));
    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<SensorBlockEntity>>
                    SENSOR = BLOCK_ENTITIES.register(
                            "sensor",
                            () -> BlockEntityType.Builder.of(
                                    SensorBlockEntity::new,
                                    ModBlocks.sensorBlockArray())
                                    .build(null));
    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<HeatExchangerBlockEntity>>
                    HEAT_EXCHANGER = BLOCK_ENTITIES.register(
                            "heat_exchanger",
                    () -> BlockEntityType.Builder.of(
                            HeatExchangerBlockEntity::new,
                            ModBlocks.heatExchangerBlockArray())
                            .build(null));
    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<GtTreeHoleBlockEntity>>
                    TREE_HOLE = BLOCK_ENTITIES.register(
                            "tree_hole",
                            () -> BlockEntityType.Builder.of(
                                    GtTreeHoleBlockEntity::new,
                                    ModBlocks.treeHoleBlockArray())
                                    .build(null));

    private ModBlockEntities() {}
}
