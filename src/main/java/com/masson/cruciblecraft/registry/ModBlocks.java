package com.masson.cruciblecraft.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.AnvilBlock;
import com.masson.cruciblecraft.content.block.RockBlock;
import com.masson.cruciblecraft.content.block.AbstractPipeBlock;
import com.masson.cruciblecraft.content.block.BoilerBlock;
import com.masson.cruciblecraft.content.block.CokeOvenBlock;
import com.masson.cruciblecraft.content.block.CeramicMoldBlock;
import com.masson.cruciblecraft.content.block.CableBlock;
import com.masson.cruciblecraft.content.block.CrucibleBlock;
import com.masson.cruciblecraft.content.block.CrusherBlock;
import com.masson.cruciblecraft.content.block.DynamoBlock;
import com.masson.cruciblecraft.content.block.ElectricMotorBlock;
import com.masson.cruciblecraft.content.block.ElectricHeaterBlock;
import com.masson.cruciblecraft.content.block.ElectricEngineBlock;
import com.masson.cruciblecraft.content.block.FluidPipeBlock;
import com.masson.cruciblecraft.content.block.FluidDepositExtractorBlock;
import com.masson.cruciblecraft.content.block.FuelGeneratorBlock;
import com.masson.cruciblecraft.content.block.FluidBedBurningBoxBlock;
import com.masson.cruciblecraft.content.block.GasCloudBlock;
import com.masson.cruciblecraft.content.block.ItemPipeBlock;
import com.masson.cruciblecraft.content.block.MaterialCasingBlock;
import com.masson.cruciblecraft.content.block.MaterialStorageBlock;
import com.masson.cruciblecraft.content.block.LargeCentrifugeBlock;
import com.masson.cruciblecraft.content.block.DistillationTowerBlock;
import com.masson.cruciblecraft.content.block.LargeBoilerBlock;
import com.masson.cruciblecraft.content.block.LargeCrucibleBlock;
import com.masson.cruciblecraft.content.block.LaserEngraverBlock;
import com.masson.cruciblecraft.content.block.FusionReactorBlock;
import com.masson.cruciblecraft.content.block.ReactorCoreBlock;
import com.masson.cruciblecraft.content.block.LogisticsCoreBlock;
import com.masson.cruciblecraft.content.block.LogisticsCorePartBlock;
import com.masson.cruciblecraft.content.block.LogisticsCoreWallBlock;
import com.masson.cruciblecraft.content.block.TankBlock;
import com.masson.cruciblecraft.content.block.MultiblockPortBlock;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.block.HopperBlock;
import com.masson.cruciblecraft.content.block.BookshelfBlock;
import com.masson.cruciblecraft.content.block.BottleCrateBlock;
import com.masson.cruciblecraft.content.block.DrawerBlock;
import com.masson.cruciblecraft.content.block.LockerBlock;
import com.masson.cruciblecraft.content.block.MassStorageBlock;
import com.masson.cruciblecraft.content.block.StorageHostBlock;
import com.masson.cruciblecraft.content.block.StorageInserterBlock;
import com.masson.cruciblecraft.content.block.GtBlockObjectBaleBlock;
import com.masson.cruciblecraft.content.block.GtBlockObjectBarsBlock;
import com.masson.cruciblecraft.content.block.GtBlockObjectBlock;
import com.masson.cruciblecraft.content.block.GtBlockObjectCFoamFreshBlock;
import com.masson.cruciblecraft.content.block.GtBlockObjectLogBlock;
import com.masson.cruciblecraft.content.block.GtBlockObjectRailBlock;
import com.masson.cruciblecraft.content.block.GtBlockObjectSlabBlock;
import com.masson.cruciblecraft.content.block.GtBlockObjectSpikeBlock;
import com.masson.cruciblecraft.content.block.GtStoneBlock;
import com.masson.cruciblecraft.content.block.GtStoneSlabBlock;
import com.masson.cruciblecraft.content.block.DustFunnelBlock;
import com.masson.cruciblecraft.content.block.RotationalAxleBlock;
import com.masson.cruciblecraft.content.block.RotationalGearboxBlock;
import com.masson.cruciblecraft.content.block.SolidBurningBoxBlock;
import com.masson.cruciblecraft.content.block.SteamEngineBlock;
import com.masson.cruciblecraft.content.block.SubsurfaceFluidDepositBlock;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PortType;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.energy.cable.ElectricalConductorCatalog;
import com.masson.cruciblecraft.logistics.pipe.PipeCatalog;
import com.masson.cruciblecraft.machine.processing.MachineVariant;
import com.masson.cruciblecraft.energy.battery.BatteryBlock;
import com.masson.cruciblecraft.energy.battery.EnergyBatteryCatalog;
import com.masson.cruciblecraft.energy.battery.EnergyBatteryProfile;
import com.masson.cruciblecraft.energy.transformer.EnergyTransformerCatalog;
import com.masson.cruciblecraft.energy.transformer.EnergyTransformerProfile;
import com.masson.cruciblecraft.energy.transformer.TransformerBlock;
import com.masson.cruciblecraft.energy.heatexchanger.HeatExchangerBlock;
import com.masson.cruciblecraft.energy.heatexchanger.HeatExchangerCatalog;
import com.masson.cruciblecraft.energy.heatexchanger.HeatExchangerProfile;
import com.masson.cruciblecraft.energy.converter.EnergyConverterCatalog;
import com.masson.cruciblecraft.energy.converter.EnergyConverterFuelSpecs;
import com.masson.cruciblecraft.energy.converter.EnergyConverterProfile;
import com.masson.cruciblecraft.content.item.GtBlockObjectCatalog;
import com.masson.cruciblecraft.content.item.BathRemainderBlockObjectCatalog;
import com.masson.cruciblecraft.content.item.GtStoneCatalog;
import com.masson.cruciblecraft.logistics.hopper.HopperVariant;
import com.masson.cruciblecraft.logistics.hopper.HopperVariantCatalog;
import com.masson.cruciblecraft.logistics.core.LogisticsCorePart;
import com.masson.cruciblecraft.content.storage.StorageBehaviorProfile;
import com.masson.cruciblecraft.content.storage.StorageVariant;
import com.masson.cruciblecraft.content.storage.StorageVariantCatalog;
import com.masson.cruciblecraft.worldgen.OreHostVariantCatalog.Host;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CrucibleCraft.MODID);
    private static final Map<OreBlockKey, DeferredBlock<DropExperienceBlock>>
            MATERIAL_ORE_BLOCKS = new LinkedHashMap<>();
    private static final Map<String, DeferredBlock<MaterialStorageBlock>>
            MATERIAL_STORAGE_BLOCKS = new LinkedHashMap<>();
    private static final Map<CasingBlockKey, DeferredBlock<MaterialCasingBlock>>
            MATERIAL_CASING_BLOCKS = new LinkedHashMap<>();
    private static final Map<String, DeferredBlock<RockBlock>>
            ROCK_BLOCKS = new LinkedHashMap<>();
    private static final Map<
            ElectricalConductorCatalog.Key,
            DeferredBlock<CableBlock>> ELECTRICAL_CONDUCTOR_BLOCKS =
                    new LinkedHashMap<>();
    private static final Map<
            PipeCatalog.Key,
            DeferredBlock<? extends AbstractPipeBlock>> PIPE_BLOCKS =
                    new LinkedHashMap<>();
    private static final Map<
            ResourceLocation,
            DeferredBlock<ProcessingMachineBlock>> CONFIGURED_PROCESSING_BLOCKS =
                    new LinkedHashMap<>();
    private static final Map<ResourceLocation, MachineVariant>
            CONFIGURED_PROCESSING_VARIANTS = new LinkedHashMap<>();

    /** M0 placeholder block — later reused as firebox cladding. */
    public static final DeferredBlock<Block> FIREBRICK = BLOCKS.registerSimpleBlock(
            "firebrick",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.STONE));
    public static final DeferredBlock<CableBlock> LU_FIBER_CABLE = BLOCKS.register(
            "lu_fiber_cable",
            () -> CableBlock.luFiber(conductorProperties()));
    public static final DeferredBlock<LaserEngraverBlock> LASER_ENGRAVER =
            BLOCKS.register(
                    "laser_engraver",
                    () -> new LaserEngraverBlock(machineProperties()));
    public static final DeferredBlock<FusionReactorBlock> FUSION_REACTOR =
            BLOCKS.register(
                    "fusion_reactor",
                    () -> new FusionReactorBlock(
                            machineProperties().strength(12.5F, 12.5F)));
    public static final DeferredBlock<ReactorCoreBlock> REACTOR_CORE_1X1 =
            BLOCKS.register(
                    "reactor_core_1x1",
                    () -> new ReactorCoreBlock(1, machineProperties()));
    public static final DeferredBlock<ReactorCoreBlock> REACTOR_CORE_2X2 =
            BLOCKS.register(
                    "reactor_core_2x2",
                    () -> new ReactorCoreBlock(4, machineProperties()));
    public static final DeferredBlock<Block> TUNGSTENSTEEL_WALL =
            BLOCKS.registerSimpleBlock(
                    "tungstensteel_wall", machineProperties());
    public static final DeferredBlock<Block> STAINLESS_STEEL_WALL =
            BLOCKS.registerSimpleBlock(
                    "stainless_steel_wall", machineProperties());
    public static final DeferredBlock<Block> LARGE_IRIDIUM_COIL =
            BLOCKS.registerSimpleBlock(
                    "large_iridium_coil", machineProperties());

    public static final DeferredBlock<CrucibleBlock> CRUCIBLE = BLOCKS.register(
            "crucible",
            () -> new CrucibleBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(3.0F, 8.0F)
                    .sound(SoundType.STONE)));

    public static final DeferredBlock<AnvilBlock> ANVIL = BLOCKS.register(
            "anvil",
            () -> new AnvilBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(5.0F, 1_200.0F)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .sound(SoundType.ANVIL)));

    public static final DeferredBlock<CokeOvenBlock> COKE_OVEN = BLOCKS.register(
            "coke_oven",
            () -> new CokeOvenBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(3.0F, 8.0F)
                    .lightLevel(state -> state.getValue(CokeOvenBlock.LIT) ? 8 : 0)
                    .sound(SoundType.STONE)));
    public static final DeferredBlock<Block> MULTIBLOCK_CASING =
            BLOCKS.registerSimpleBlock(
                    "multiblock_casing",
                    machineProperties());
    public static final DeferredBlock<MultiblockPortBlock>
            MULTIBLOCK_ITEM_FLUID_PORT = BLOCKS.register(
                    "multiblock_item_fluid_port",
                    () -> new MultiblockPortBlock(
                            PortType.ITEM_FLUID,
                            machineProperties()));
    public static final DeferredBlock<MultiblockPortBlock>
            MULTIBLOCK_ENERGY_INPUT_PORT = BLOCKS.register(
                    "multiblock_energy_input_port",
                    () -> new MultiblockPortBlock(
                            PortType.ENERGY_INPUT,
                            machineProperties()));
    public static final DeferredBlock<LargeCentrifugeBlock>
            LARGE_CENTRIFUGE = BLOCKS.register(
                    "large_centrifuge",
                    () -> new LargeCentrifugeBlock(machineProperties()));
    public static final DeferredBlock<DistillationTowerBlock>
            DISTILLATION_TOWER = BLOCKS.register(
                    "distillation_tower",
                    () -> new DistillationTowerBlock(machineProperties()));
    public static final DeferredBlock<LargeBoilerBlock>
            LARGE_BOILER = BLOCKS.register(
                    "large_boiler",
                    () -> new LargeBoilerBlock(machineProperties()));
    public static final DeferredBlock<TankBlock>
            TANK_3X3X3 = BLOCKS.register(
                    "tank_3x3x3",
                    () -> new TankBlock(machineProperties()));
    public static final DeferredBlock<LargeCrucibleBlock>
            LARGE_CRUCIBLE = BLOCKS.register(
                    "large_crucible",
                    () -> new LargeCrucibleBlock(machineProperties()));
    public static final DeferredBlock<LogisticsCoreBlock>
            LOGISTICS_CORE = BLOCKS.register(
                    "logistics_core",
                    () -> new LogisticsCoreBlock(machineProperties()));
    public static final DeferredBlock<LogisticsCoreWallBlock>
            GALVANIZED_STEEL_WALL = BLOCKS.register(
                    "galvanized_steel_wall",
                    () -> new LogisticsCoreWallBlock(machineProperties()));
    public static final DeferredBlock<LogisticsCorePartBlock>
            VENTILATION_UNIT = BLOCKS.register(
                    "ventilation_unit",
                    () -> new LogisticsCorePartBlock(
                            LogisticsCorePart.VENT, machineProperties()));
    public static final DeferredBlock<LogisticsCorePartBlock>
            VERSATILE_PROCESSOR_UNIT = BLOCKS.register(
                    "versatile_processor_unit",
                    () -> new LogisticsCorePartBlock(
                            LogisticsCorePart.VERSATILE, machineProperties()));
    public static final DeferredBlock<LogisticsCorePartBlock>
            LOGIC_PROCESSOR_UNIT = BLOCKS.register(
                    "logic_processor_unit",
                    () -> new LogisticsCorePartBlock(
                            LogisticsCorePart.LOGIC, machineProperties()));
    public static final DeferredBlock<LogisticsCorePartBlock>
            CONTROL_PROCESSOR_UNIT = BLOCKS.register(
                    "control_processor_unit",
                    () -> new LogisticsCorePartBlock(
                            LogisticsCorePart.CONTROL, machineProperties()));
    public static final DeferredBlock<LogisticsCorePartBlock>
            STORAGE_PROCESSOR_UNIT = BLOCKS.register(
                    "storage_processor_unit",
                    () -> new LogisticsCorePartBlock(
                            LogisticsCorePart.STORAGE, machineProperties()));
    public static final DeferredBlock<LogisticsCorePartBlock>
            CONVERSION_PROCESSOR_UNIT = BLOCKS.register(
                    "conversion_processor_unit",
                    () -> new LogisticsCorePartBlock(
                            LogisticsCorePart.CONVERSION, machineProperties()));

    public static final DeferredBlock<CeramicMoldBlock> CERAMIC_MOLD = BLOCKS.register(
            "ceramic_mold",
            () -> new CeramicMoldBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(1.5F, 4.0F)
                    .noOcclusion()
                    .noLootTable()
                    .sound(SoundType.STONE)));

    public static final DeferredBlock<LiquidBlock> CREOSOTE = BLOCKS.register(
            "creosote",
            () -> new LiquidBlock(
                    ModFluids.CREOSOTE_SOURCE.get(),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BROWN)
                            .replaceable()
                            .noCollission()
                            .strength(100.0F)
                            .pushReaction(PushReaction.DESTROY)
                            .noLootTable()
                            .liquid()));

    public static final DeferredBlock<LiquidBlock> STEAM = BLOCKS.register(
            "steam",
            () -> new LiquidBlock(
                    ModFluids.STEAM_SOURCE.get(),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.NONE)
                            .replaceable()
                            .noCollission()
                            .strength(100.0F)
                            .pushReaction(PushReaction.DESTROY)
                            .noLootTable()
                            .liquid()));

    public static final DeferredBlock<SubsurfaceFluidDepositBlock>
            SUBSURFACE_FLUID_DEPOSIT = BLOCKS.register(
                    "subsurface_fluid_deposit",
                    () -> new SubsurfaceFluidDepositBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.STONE)
                                    .strength(50.0F, 1_200.0F)
                                    .requiresCorrectToolForDrops()
                                    .noLootTable()
                                    .sound(SoundType.STONE)));
    public static final DeferredBlock<FluidDepositExtractorBlock>
            FLUID_DEPOSIT_EXTRACTOR = BLOCKS.register(
                    "fluid_deposit_extractor",
                    () -> new FluidDepositExtractorBlock(
                            machineProperties()));
    public static final DeferredBlock<GasCloudBlock> GAS_CLOUD =
            BLOCKS.register(
                    "gas_cloud",
                    () -> new GasCloudBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.NONE)
                                    .replaceable()
                                    .noCollission()
                                    .noLootTable()
                                    .strength(0.0F)
                                    .pushReaction(PushReaction.DESTROY)));

    private static final Map<
            ResourceLocation,
            DeferredBlock<? extends Block>> CONVERTER_BLOCKS =
                    registerConverterBlocks();
    public static final DeferredBlock<BoilerBlock> BRONZE_BOILER =
            converter("bronze_boiler");
    public static final DeferredBlock<SteamEngineBlock> BRONZE_STEAM_ENGINE =
            converter("bronze_steam_engine");
    public static final DeferredBlock<DynamoBlock> BRONZE_DYNAMO =
            converter("bronze_dynamo");
    public static final DeferredBlock<ElectricMotorBlock>
            STEEL_GALVANIZED_ELECTRIC_MOTOR =
                    converter("steel_galvanized_electric_motor");
    public static final DeferredBlock<FuelGeneratorBlock> BRONZE_FUEL_ENGINE =
            converter("bronze_fuel_engine");
    public static final DeferredBlock<FuelGeneratorBlock>
            BRONZE_BURNING_BOX_GAS = converter("bronze_burning_box_gas");
    public static final DeferredBlock<SolidBurningBoxBlock>
            BRONZE_BURNING_BOX_SOLID = converter("bronze_burning_box_solid");
    public static final DeferredBlock<RotationalAxleBlock>
            ROTATIONAL_AXLE = BLOCKS.register(
                    "rotational_axle",
                    () -> new RotationalAxleBlock(machineProperties()));
    public static final DeferredBlock<RotationalGearboxBlock>
            ROTATIONAL_GEARBOX = BLOCKS.register(
                    "rotational_gearbox",
                    () -> new RotationalGearboxBlock(
                            machineProperties()));
    public static final DeferredBlock<CrusherBlock> BRONZE_CRUSHER = BLOCKS.register(
            "bronze_crusher",
            () -> new CrusherBlock(machineProperties()));
    private static final Map<
            ResourceLocation,
            DeferredBlock<ProcessingMachineBlock>> TIERED_PROCESSING_BLOCKS =
                    registerTieredProcessingBlocks();
    private static final Map<ResourceLocation, DeferredBlock<HopperBlock>>
            HOPPER_BLOCKS = registerHopperBlocks();
    private static final Map<ResourceLocation, DeferredBlock<Block>>
            GT_STONE_BLOCKS = registerGtStoneBlocks();
    private static final Map<ResourceLocation, DeferredBlock<Block>>
            GT_BLOCK_OBJECT_BLOCKS = registerGtBlockObjectBlocks();
    private static final Map<ResourceLocation, DeferredBlock<Block>>
            BATH_REMAINDER_BLOCK_OBJECT_BLOCKS = registerBathRemainderBlockObjectBlocks();
    private static final Map<ResourceLocation, DeferredBlock<? extends StorageHostBlock>>
            STORAGE_BLOCKS = registerStorageBlocks();
    public static final DeferredBlock<DustFunnelBlock> STEEL_DUST_FUNNEL =
            BLOCKS.register(
                    "steel_dust_funnel",
                    () -> new DustFunnelBlock(
                            machineProperties().noOcclusion()));
    public static final DeferredBlock<ProcessingMachineBlock> SLUICE =
            tieredProcessing("sluice");
    public static final DeferredBlock<ProcessingMachineBlock> BATH =
            tieredProcessing("bath");
    public static final DeferredBlock<ProcessingMachineBlock> CENTRIFUGE =
            tieredProcessing("centrifuge");
    public static final DeferredBlock<ProcessingMachineBlock>
            STEEL_CENTRIFUGE = tieredProcessing("steel_centrifuge");
    public static final DeferredBlock<ProcessingMachineBlock>
            TITANIUM_CENTRIFUGE = tieredProcessing("titanium_centrifuge");
    public static final DeferredBlock<ProcessingMachineBlock> SHREDDER =
            tieredProcessing("shredder");
    public static final DeferredBlock<ProcessingMachineBlock> STEEL_SHREDDER =
            tieredProcessing("steel_shredder");
    public static final DeferredBlock<ProcessingMachineBlock>
            TITANIUM_SHREDDER = tieredProcessing("titanium_shredder");
    public static final DeferredBlock<ProcessingMachineBlock> SIFTER =
            tieredProcessing("sifter");
    public static final DeferredBlock<ProcessingMachineBlock>
            STEEL_SIFTER = tieredProcessing("steel_sifter");
    public static final DeferredBlock<ProcessingMachineBlock>
            TITANIUM_SIFTER = tieredProcessing("titanium_sifter");
    public static final DeferredBlock<ProcessingMachineBlock> SMELTER =
            tieredProcessing("smelter");
    public static final DeferredBlock<ProcessingMachineBlock> INVAR_SMELTER =
            tieredProcessing("invar_smelter");
    public static final DeferredBlock<ProcessingMachineBlock>
            TITANIUM_SMELTER = tieredProcessing("titanium_smelter");
    public static final DeferredBlock<ProcessingMachineBlock> MORTAR =
            tieredProcessing("mortar");
    public static final DeferredBlock<ProcessingMachineBlock> EXTRUDER =
            tieredProcessing("extruder");
    public static final DeferredBlock<ProcessingMachineBlock> INVAR_EXTRUDER =
            tieredProcessing("invar_extruder");
    public static final DeferredBlock<ProcessingMachineBlock> CUTTER =
            tieredProcessing("cutter");
    public static final DeferredBlock<ProcessingMachineBlock> LATHE =
            tieredProcessing("lathe");
    public static final DeferredBlock<ProcessingMachineBlock> STEEL_LATHE =
            tieredProcessing("steel_lathe");
    public static final DeferredBlock<ProcessingMachineBlock> TITANIUM_LATHE =
            tieredProcessing("titanium_lathe");
    public static final DeferredBlock<ProcessingMachineBlock> ROLLINGMILL =
            tieredProcessing("rollingmill");
    public static final DeferredBlock<ProcessingMachineBlock>
            STEEL_ROLLINGMILL = tieredProcessing("steel_rollingmill");
    public static final DeferredBlock<ProcessingMachineBlock>
            TITANIUM_ROLLINGMILL = tieredProcessing("titanium_rollingmill");
    public static final DeferredBlock<ProcessingMachineBlock> ROLLBENDER =
            tieredProcessing("rollbender");
    public static final DeferredBlock<ProcessingMachineBlock> WIREMILL =
            tieredProcessing("wiremill");
    public static final DeferredBlock<ProcessingMachineBlock> STEEL_WIREMILL =
            tieredProcessing("steel_wiremill");
    public static final DeferredBlock<ProcessingMachineBlock>
            TITANIUM_WIREMILL = tieredProcessing("titanium_wiremill");
    public static final DeferredBlock<ProcessingMachineBlock> BENDER =
            tieredProcessing("bender");
    public static final DeferredBlock<ProcessingMachineBlock> ASSEMBLER =
            tieredProcessing("assembler");
    public static final DeferredBlock<ProcessingMachineBlock> WELDER =
            tieredProcessing("welder");
    public static final DeferredBlock<ProcessingMachineBlock> PRESS =
            tieredProcessing("press");
    public static final DeferredBlock<ProcessingMachineBlock> STEEL_PRESS =
            tieredProcessing("steel_press");
    public static final DeferredBlock<ProcessingMachineBlock> TITANIUM_PRESS =
            tieredProcessing("titanium_press");
    public static final DeferredBlock<ProcessingMachineBlock> ELECTROLYZER =
            tieredProcessing("electrolyzer");
    public static final DeferredBlock<ProcessingMachineBlock>
            ALUMINIUM_ELECTROLYZER = tieredProcessing("aluminium_electrolyzer");
    public static final DeferredBlock<ProcessingMachineBlock>
            STAINLESS_STEEL_ELECTROLYZER =
                    tieredProcessing("stainless_steel_electrolyzer");
    public static final DeferredBlock<ProcessingMachineBlock> MIXER =
            tieredProcessing("mixer");
    public static final DeferredBlock<ProcessingMachineBlock> DISTILLERY =
            tieredProcessing("distillery");
    public static final DeferredBlock<ProcessingMachineBlock>
            INVAR_DISTILLERY = tieredProcessing("invar_distillery");
    public static final DeferredBlock<ProcessingMachineBlock>
            TITANIUM_DISTILLERY = tieredProcessing("titanium_distillery");
    public static final DeferredBlock<ProcessingMachineBlock> AUTOCLAVE =
            tieredProcessing("autoclave");
    public static final DeferredBlock<ProcessingMachineBlock> DRYING =
            tieredProcessing("drying");
    public static final DeferredBlock<ProcessingMachineBlock> INVAR_DRYING =
            tieredProcessing("invar_drying");
    public static final DeferredBlock<ProcessingMachineBlock>
            TITANIUM_DRYING = tieredProcessing("titanium_drying");
    public static final DeferredBlock<ProcessingMachineBlock> COMPRESSOR =
            tieredProcessing("compressor");
    public static final DeferredBlock<ProcessingMachineBlock> GENERIFIER =
            tieredProcessing("generifier");
    public static final DeferredBlock<ProcessingMachineBlock> COAGULATOR =
            tieredProcessing("coagulator");
    public static final DeferredBlock<ProcessingMachineBlock> CANNER =
            tieredProcessing("canner");
    public static final DeferredBlock<ProcessingMachineBlock> STEEL_ROASTER =
            tieredProcessing("steel_roaster");

    public static void registerMaterials(Collection<MaterialDefinition> definitions) {
        if (!MATERIAL_ORE_BLOCKS.isEmpty()
                || !MATERIAL_STORAGE_BLOCKS.isEmpty()
                || !MATERIAL_CASING_BLOCKS.isEmpty()
                || !ROCK_BLOCKS.isEmpty()
                || !ELECTRICAL_CONDUCTOR_BLOCKS.isEmpty()
                || !PIPE_BLOCKS.isEmpty()) {
            throw new IllegalStateException("Material blocks already registered");
        }
        ElectricalConductorCatalog.initialize(definitions);
        PipeCatalog.initialize(definitions);
        for (ElectricalConductorCatalog.Entry conductor
                : ElectricalConductorCatalog.all()) {
            ElectricalConductorCatalog.Key key =
                    new ElectricalConductorCatalog.Key(
                            conductor.materialId(), conductor.form());
            DeferredBlock<CableBlock> previous =
                    ELECTRICAL_CONDUCTOR_BLOCKS.put(
                            key,
                            BLOCKS.register(
                                    conductor.registryName(),
                                    () -> new CableBlock(
                                            conductor,
                                            conductorProperties())));
            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate electrical conductor block " + key);
            }
        }
        for (PipeCatalog.Entry pipe : PipeCatalog.all()) {
            PipeCatalog.Key key = new PipeCatalog.Key(
                    pipe.materialId(), pipe.form(), pipe.kind());
            DeferredBlock<? extends AbstractPipeBlock> holder =
                    pipe.kind() == PipeCatalog.Kind.FLUID
                            ? BLOCKS.register(
                                    pipe.registryName(),
                                    () -> new FluidPipeBlock(
                                            pipe,
                                            conductorProperties()))
                            : BLOCKS.register(
                                    pipe.registryName(),
                                    () -> new ItemPipeBlock(
                                            pipe,
                                            conductorProperties()));
            if (PIPE_BLOCKS.putIfAbsent(key, holder) != null) {
                throw new IllegalStateException(
                        "Duplicate pipe block " + key);
            }
        }
        for (MaterialDefinition material : definitions) {
            if (!MaterialCatalog.registeredForms(material).contains(MaterialPrefixes.ORE)) {
                continue;
            }
            for (Host host : Host.values()) {
                OreBlockKey key = new OreBlockKey(material.id(), host);
                MATERIAL_ORE_BLOCKS.put(
                        key,
                        ore(
                                oreRegistryName(material.id(), host),
                                host == Host.DEEPSLATE ? MapColor.DEEPSLATE : MapColor.STONE));
            }
        }
        for (MaterialDefinition material : definitions) {
            if (!MaterialCatalog.registeredForms(material).contains(
                    MaterialPrefixes.BLOCK)
                    || material.formItems().containsKey(MaterialPrefixes.BLOCK)) {
                continue;
            }
            DeferredBlock<MaterialStorageBlock> previous =
                    MATERIAL_STORAGE_BLOCKS.put(
                            material.id(),
                            BLOCKS.register(
                                    material.registryName(MaterialPrefixes.BLOCK),
                                    () -> new MaterialStorageBlock(
                                            material.id(),
                                            storageProperties())));
            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate storage block for " + material.id());
            }
        }
        for (MaterialDefinition material : definitions) {
            for (MaterialPrefix form : List.of(
                    MaterialPrefixes.MACHINE_CASING,
                    MaterialPrefixes.MACHINE_CASING_DOUBLE,
                    MaterialPrefixes.MACHINE_CASING_QUADRUPLE,
                    MaterialPrefixes.MACHINE_CASING_DENSE)) {
                if (!MaterialCatalog.registeredForms(material).contains(form)
                        || material.formItems().containsKey(form)) {
                    continue;
                }
                CasingBlockKey key = new CasingBlockKey(material.id(), form);
                DeferredBlock<MaterialCasingBlock> previous =
                        MATERIAL_CASING_BLOCKS.put(
                                key,
                                BLOCKS.register(
                                        material.registryName(form),
                                        () -> new MaterialCasingBlock(
                                                material.id(),
                                                form,
                                                casingProperties(form))));
                if (previous != null) {
                    throw new IllegalStateException(
                            "Duplicate casing block for " + key);
                }
            }
        }
        com.masson.cruciblecraft.api.material.MaterialPrefix rockForm =
                com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog
                        .require("rock");
        for (MaterialDefinition material : definitions) {
            if (!MaterialCatalog.registeredForms(material).contains(rockForm)
                    || material.formItems().containsKey(rockForm)) {
                continue;
            }
            DeferredBlock<RockBlock> previous =
                    ROCK_BLOCKS.put(
                            material.id(),
                            BLOCKS.register(
                                    material.registryName(rockForm),
                                    () -> new RockBlock(
                                            material.id(),
                                            rockProperties())));
            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate rock block for " + material.id());
            }
        }
    }

    public static DeferredBlock<RockBlock> rockBlock(String materialId) {
        DeferredBlock<RockBlock> block = ROCK_BLOCKS.get(materialId);
        if (block == null) {
            throw new IllegalArgumentException(
                    "No rock block for material " + materialId);
        }
        return block;
    }

    public static boolean hasRockBlock(String materialId) {
        return ROCK_BLOCKS.containsKey(materialId);
    }

    public static Collection<DeferredBlock<RockBlock>> rockBlocks() {
        return Collections.unmodifiableCollection(ROCK_BLOCKS.values());
    }

    public static DeferredBlock<DropExperienceBlock> oreBlock(
            String materialId, Host host) {
        DeferredBlock<DropExperienceBlock> block =
                MATERIAL_ORE_BLOCKS.get(new OreBlockKey(materialId, host));
        if (block == null) {
            throw new IllegalArgumentException(
                    "No " + host.name().toLowerCase(java.util.Locale.ROOT)
                            + " ore block for material " + materialId);
        }
        return block;
    }

    public static boolean hasOreBlock(String materialId, Host host) {
        return MATERIAL_ORE_BLOCKS.containsKey(new OreBlockKey(materialId, host));
    }

    public static Collection<DeferredBlock<DropExperienceBlock>> oreBlocks() {
        return Collections.unmodifiableCollection(MATERIAL_ORE_BLOCKS.values());
    }

    public static DeferredBlock<MaterialStorageBlock> storageBlock(String materialId) {
        DeferredBlock<MaterialStorageBlock> block =
                MATERIAL_STORAGE_BLOCKS.get(materialId);
        if (block == null) {
            throw new IllegalArgumentException(
                    "No storage block for material " + materialId);
        }
        return block;
    }

    public static boolean hasStorageBlock(String materialId) {
        return MATERIAL_STORAGE_BLOCKS.containsKey(materialId);
    }

    public static Collection<DeferredBlock<MaterialStorageBlock>> storageBlocks() {
        return Collections.unmodifiableCollection(MATERIAL_STORAGE_BLOCKS.values());
    }

    public static DeferredBlock<MaterialCasingBlock> casingBlock(
            String materialId, MaterialPrefix form) {
        DeferredBlock<MaterialCasingBlock> block =
                MATERIAL_CASING_BLOCKS.get(new CasingBlockKey(materialId, form));
        if (block == null) {
            throw new IllegalArgumentException(
                    "No casing block for " + materialId + "/"
                            + form.serializedName());
        }
        return block;
    }

    public static boolean hasCasingBlock(String materialId, MaterialPrefix form) {
        return MATERIAL_CASING_BLOCKS.containsKey(
                new CasingBlockKey(materialId, form));
    }

    public static Collection<DeferredBlock<MaterialCasingBlock>> casingBlocks() {
        return Collections.unmodifiableCollection(MATERIAL_CASING_BLOCKS.values());
    }

    public static DeferredBlock<CableBlock> electricalConductorBlock(
            String materialId,
            com.masson.cruciblecraft.api.material.MaterialPrefix form) {
        DeferredBlock<CableBlock> block =
                ELECTRICAL_CONDUCTOR_BLOCKS.get(
                        new ElectricalConductorCatalog.Key(materialId, form));
        if (block == null) {
            throw new IllegalArgumentException(
                    "No electrical conductor block for "
                            + materialId + "/" + form.serializedName());
        }
        return block;
    }

    public static boolean hasElectricalConductorBlock(
            String materialId,
            com.masson.cruciblecraft.api.material.MaterialPrefix form) {
        return ELECTRICAL_CONDUCTOR_BLOCKS.containsKey(
                new ElectricalConductorCatalog.Key(materialId, form));
    }

    public static Collection<DeferredBlock<CableBlock>>
            electricalConductorBlocks() {
        return Collections.unmodifiableCollection(
                ELECTRICAL_CONDUCTOR_BLOCKS.values());
    }

    public static CableBlock[] electricalConductorBlockArray() {
        return java.util.stream.Stream.concat(
                        ELECTRICAL_CONDUCTOR_BLOCKS.values().stream(),
                        java.util.stream.Stream.of(LU_FIBER_CABLE))
                .map(DeferredBlock::get)
                .toArray(CableBlock[]::new);
    }

    public static DeferredBlock<? extends AbstractPipeBlock> pipeBlock(
            String materialId,
            com.masson.cruciblecraft.api.material.MaterialPrefix form,
            PipeCatalog.Kind kind) {
        DeferredBlock<? extends AbstractPipeBlock> block = PIPE_BLOCKS.get(
                new PipeCatalog.Key(materialId, form, kind));
        if (block == null) {
            throw new IllegalArgumentException(
                    "No " + kind.name().toLowerCase(java.util.Locale.ROOT)
                            + " pipe block for " + materialId + "/"
                            + form.serializedName());
        }
        return block;
    }

    public static boolean hasPipeBlock(
            String materialId,
            com.masson.cruciblecraft.api.material.MaterialPrefix form,
            PipeCatalog.Kind kind) {
        return PIPE_BLOCKS.containsKey(
                new PipeCatalog.Key(materialId, form, kind));
    }

    public static Collection<DeferredBlock<? extends AbstractPipeBlock>>
            pipeBlocks() {
        return Collections.unmodifiableCollection(PIPE_BLOCKS.values());
    }

    public static FluidPipeBlock[] fluidPipeBlockArray() {
        return PIPE_BLOCKS.values().stream()
                .map(DeferredBlock::get)
                .filter(FluidPipeBlock.class::isInstance)
                .map(FluidPipeBlock.class::cast)
                .toArray(FluidPipeBlock[]::new);
    }

    public static ItemPipeBlock[] itemPipeBlockArray() {
        return PIPE_BLOCKS.values().stream()
                .map(DeferredBlock::get)
                .filter(ItemPipeBlock.class::isInstance)
                .map(ItemPipeBlock.class::cast)
                .toArray(ItemPipeBlock[]::new);
    }

    public static Collection<DeferredBlock<ProcessingMachineBlock>>
            configuredProcessingBlockEntries() {
        return Collections.unmodifiableCollection(
                CONFIGURED_PROCESSING_BLOCKS.values());
    }

    public static MachineVariant configuredProcessingVariant(
            ResourceLocation id) {
        MachineVariant variant = CONFIGURED_PROCESSING_VARIANTS.get(id);
        if (variant == null) {
            throw new IllegalArgumentException(
                    "No source-backed processing variant for " + id);
        }
        return variant;
    }

    /** Immutable material/host-to-path view used by worldgen host adaptation. */
    public static Map<OreBlockKey, String> oreBlockPaths() {
        LinkedHashMap<OreBlockKey, String> paths = new LinkedHashMap<>();
        MATERIAL_ORE_BLOCKS.keySet().forEach(key ->
                paths.put(key, oreRegistryName(key.materialId(), key.host())));
        return Collections.unmodifiableMap(paths);
    }

    public static String oreRegistryName(String materialId, Host host) {
        return host == Host.DEEPSLATE
                ? "deepslate_" + materialId + "_ore"
                : materialId + "_ore";
    }

    private static DeferredBlock<DropExperienceBlock> ore(String id, MapColor color) {
        return BLOCKS.register(
                id,
                () -> new DropExperienceBlock(
                        net.minecraft.util.valueproviders.UniformInt.of(0, 2),
                        BlockBehaviour.Properties.of()
                                .mapColor(color)
                                .strength(3.0F, 3.0F)
                                .requiresCorrectToolForDrops()
                                .sound(SoundType.STONE)));
    }

    private static Map<ResourceLocation, DeferredBlock<ProcessingMachineBlock>>
            registerTieredProcessingBlocks() {
        LinkedHashMap<ResourceLocation, DeferredBlock<ProcessingMachineBlock>>
                blocks = new LinkedHashMap<>();
        for (MachineVariant variant : ModMachineVariants.ALL) {
            DeferredBlock<ProcessingMachineBlock> block =
                    processing(variant.id().getPath(), variant);
            if (blocks.put(variant.id(), block) != null) {
                throw new IllegalStateException(
                        "Duplicate tiered processing block " + variant.id());
            }
        }
        if (blocks.size() != ModMachineVariants.ALL.size()) {
            throw new IllegalStateException(
                    "Tiered processing registration drifted from catalog rows");
        }
        return java.util.Collections.unmodifiableMap(blocks);
    }

    private static Map<ResourceLocation, DeferredBlock<? extends Block>>
            registerConverterBlocks() {
        LinkedHashMap<ResourceLocation, DeferredBlock<? extends Block>>
                blocks = new LinkedHashMap<>();
        for (EnergyConverterProfile profile
                : EnergyConverterCatalog.profiles()) {
            DeferredBlock<? extends Block> block =
                    registerConverterBlock(profile);
            if (blocks.put(profile.id(), block) != null) {
                throw new IllegalStateException(
                        "Duplicate converter block " + profile.id());
            }
        }
        if (blocks.size() != EnergyConverterCatalog.profiles().size()) {
            throw new IllegalStateException(
                    "Converter registration drifted from catalog rows");
        }
        return java.util.Collections.unmodifiableMap(blocks);
    }

    private static DeferredBlock<? extends Block> registerConverterBlock(
            EnergyConverterProfile profile) {
        String path = profile.id().getPath();
        return switch (profile.runtimeBinding()) {
            case "boiler" -> BLOCKS.register(
                    path,
                    () -> new BoilerBlock(profile.id(), machineProperties()));
            case "steam_engine" -> BLOCKS.register(
                    path,
                    () -> new SteamEngineBlock(
                            profile.id(),
                            machineProperties().noOcclusion()));
            case "dynamo" -> BLOCKS.register(
                    path,
                    () -> new DynamoBlock(profile.id(), machineProperties()));
            case "electric_motor" -> BLOCKS.register(
                    path,
                    () -> new ElectricMotorBlock(
                            profile.id(), machineProperties()));
            case "electric_heater" -> BLOCKS.register(
                    path,
                    () -> new ElectricHeaterBlock(
                            profile.id(), machineProperties()));
            case "electric_engine" -> BLOCKS.register(
                    path,
                    () -> new ElectricEngineBlock(
                            profile.id(), machineProperties().noOcclusion()));
            case "fuel_engine", "fluid_burning_box" -> BLOCKS.register(
                    path,
                    () -> new FuelGeneratorBlock(
                            EnergyConverterFuelSpecs.fromProfile(profile),
                            machineProperties()));
            case "solid_burning_box" -> BLOCKS.register(
                    path,
                    () -> new SolidBurningBoxBlock(
                            profile.id(), machineProperties()));
            case "fluid_bed_burning_box" -> BLOCKS.register(
                    path,
                    () -> new FluidBedBurningBoxBlock(
                            profile.id(), machineProperties()));
            default -> throw new IllegalStateException(
                    "Unknown converter runtime " + profile.runtimeBinding());
        };
    }

    @SuppressWarnings("unchecked")
    private static <T extends Block> DeferredBlock<T> converter(String path) {
        DeferredBlock<? extends Block> block = CONVERTER_BLOCKS.get(
                ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID, path));
        if (block == null) {
            throw new IllegalStateException(
                    "Missing catalog converter block " + path);
        }
        return (DeferredBlock<T>) block;
    }

    public static Map<ResourceLocation, DeferredBlock<? extends Block>>
            converterBlocksById() {
        return CONVERTER_BLOCKS;
    }

    public static Block[] converterBlockArray() {
        return CONVERTER_BLOCKS.values().stream()
                .map(DeferredBlock::get)
                .toArray(Block[]::new);
    }

    public static Block[] converterBlocks(String... runtimes) {
        java.util.Set<String> wanted = java.util.Set.of(runtimes);
        return EnergyConverterCatalog.profiles().stream()
                .filter(profile -> wanted.contains(profile.runtimeBinding()))
                .map(profile -> CONVERTER_BLOCKS.get(profile.id()).get())
                .toArray(Block[]::new);
    }

    private static final Map<
            ResourceLocation,
            DeferredBlock<BatteryBlock>> BATTERY_BLOCKS =
                    registerBatteryBlocks();

    private static Map<ResourceLocation, DeferredBlock<BatteryBlock>>
            registerBatteryBlocks() {
        LinkedHashMap<ResourceLocation, DeferredBlock<BatteryBlock>> blocks =
                new LinkedHashMap<>();
        for (EnergyBatteryProfile profile : EnergyBatteryCatalog.profiles()) {
            DeferredBlock<BatteryBlock> block = BLOCKS.register(
                    profile.id().getPath(),
                    () -> new BatteryBlock(profile, batteryProperties()));
            if (blocks.put(profile.id(), block) != null) {
                throw new IllegalStateException(
                        "Duplicate battery block " + profile.id());
            }
        }
        if (blocks.size() != EnergyBatteryCatalog.profiles().size()) {
            throw new IllegalStateException(
                    "Battery registration drifted from catalog rows");
        }
        return java.util.Collections.unmodifiableMap(blocks);
    }

    public static Map<ResourceLocation, DeferredBlock<BatteryBlock>>
            batteryBlocksById() {
        return BATTERY_BLOCKS;
    }

    public static Block[] batteryBlockArray() {
        return BATTERY_BLOCKS.values().stream()
                .map(DeferredBlock::get)
                .toArray(Block[]::new);
    }

    private static BlockBehaviour.Properties batteryProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(0.5F, 3.0F)
                .sound(SoundType.METAL)
                .noOcclusion();
    }

    private static final Map<
            ResourceLocation,
            DeferredBlock<TransformerBlock>> TRANSFORMER_BLOCKS =
                    registerTransformerBlocks();

    private static Map<ResourceLocation, DeferredBlock<TransformerBlock>>
            registerTransformerBlocks() {
        LinkedHashMap<ResourceLocation, DeferredBlock<TransformerBlock>> blocks =
                new LinkedHashMap<>();
        for (EnergyTransformerProfile profile
                : EnergyTransformerCatalog.profiles()) {
            DeferredBlock<TransformerBlock> block = BLOCKS.register(
                    profile.id().getPath(),
                    () -> new TransformerBlock(profile, transformerProperties()));
            if (blocks.put(profile.id(), block) != null) {
                throw new IllegalStateException(
                        "Duplicate transformer block " + profile.id());
            }
        }
        if (blocks.size() != EnergyTransformerCatalog.profiles().size()) {
            throw new IllegalStateException(
                    "Transformer registration drifted from catalog rows");
        }
        return java.util.Collections.unmodifiableMap(blocks);
    }

    public static Map<ResourceLocation, DeferredBlock<TransformerBlock>>
            transformerBlocksById() {
        return TRANSFORMER_BLOCKS;
    }

    public static Block[] transformerBlockArray() {
        return TRANSFORMER_BLOCKS.values().stream()
                .map(DeferredBlock::get)
                .toArray(Block[]::new);
    }

    private static BlockBehaviour.Properties transformerProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.0F, 6.0F)
                .sound(SoundType.METAL);
    }

    private static final Map<
            ResourceLocation,
            DeferredBlock<HeatExchangerBlock>> HEAT_EXCHANGER_BLOCKS =
                    registerHeatExchangerBlocks();

    private static Map<ResourceLocation, DeferredBlock<HeatExchangerBlock>>
            registerHeatExchangerBlocks() {
        LinkedHashMap<ResourceLocation, DeferredBlock<HeatExchangerBlock>>
                blocks = new LinkedHashMap<>();
        for (HeatExchangerProfile profile : HeatExchangerCatalog.profiles()) {
            DeferredBlock<HeatExchangerBlock> block = BLOCKS.register(
                    profile.id().getPath(),
                    () -> new HeatExchangerBlock(
                            profile, heatExchangerProperties(profile)));
            if (blocks.put(profile.id(), block) != null) {
                throw new IllegalStateException(
                        "Duplicate heat exchanger block " + profile.id());
            }
        }
        if (blocks.size() != HeatExchangerCatalog.profiles().size()) {
            throw new IllegalStateException(
                    "Heat exchanger registration drifted from catalog rows");
        }
        return java.util.Collections.unmodifiableMap(blocks);
    }

    public static Map<ResourceLocation, DeferredBlock<HeatExchangerBlock>>
            heatExchangerBlocksById() {
        return HEAT_EXCHANGER_BLOCKS;
    }

    public static Block[] heatExchangerBlockArray() {
        return HEAT_EXCHANGER_BLOCKS.values().stream()
                .map(DeferredBlock::get)
                .toArray(Block[]::new);
    }

    private static BlockBehaviour.Properties heatExchangerProperties(
            HeatExchangerProfile profile) {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(profile.hardness(), profile.resistance())
                .sound(SoundType.METAL);
    }

    private static DeferredBlock<ProcessingMachineBlock> tieredProcessing(
            String path) {
        DeferredBlock<ProcessingMachineBlock> block =
                TIERED_PROCESSING_BLOCKS.get(
                        ResourceLocation.fromNamespaceAndPath(
                                CrucibleCraft.MODID, path));
        if (block == null) {
            throw new IllegalStateException(
                    "Missing catalog processing block " + path);
        }
        return block;
    }

    public static Map<ResourceLocation, DeferredBlock<ProcessingMachineBlock>>
            tieredProcessingBlocksById() {
        return TIERED_PROCESSING_BLOCKS;
    }

    private static Map<ResourceLocation, DeferredBlock<HopperBlock>>
            registerHopperBlocks() {
        LinkedHashMap<ResourceLocation, DeferredBlock<HopperBlock>> blocks =
                new LinkedHashMap<>();
        for (HopperVariant variant : HopperVariantCatalog.variants()) {
            DeferredBlock<HopperBlock> block = BLOCKS.register(
                    variant.id().getPath(),
                    () -> new HopperBlock(
                            variant, machineProperties().noOcclusion()));
            if (blocks.put(variant.id(), block) != null) {
                throw new IllegalStateException(
                        "Duplicate hopper block " + variant.id());
            }
        }
        if (blocks.size() != 120) {
            throw new IllegalStateException(
                    "Hopper registration drifted from 120 variants");
        }
        return java.util.Collections.unmodifiableMap(blocks);
    }

    public static Map<ResourceLocation, DeferredBlock<HopperBlock>>
            hopperBlocksById() {
        return HOPPER_BLOCKS;
    }

    public static Collection<DeferredBlock<HopperBlock>> hopperBlocks() {
        return HOPPER_BLOCKS.values();
    }

    private static Map<ResourceLocation, DeferredBlock<Block>>
            registerGtStoneBlocks() {
        LinkedHashMap<ResourceLocation, DeferredBlock<Block>> blocks =
                new LinkedHashMap<>();
        for (GtStoneCatalog.Variant variant : GtStoneCatalog.variants()) {
            DeferredBlock<Block> block = BLOCKS.register(
                    variant.registryPath(),
                    () -> variant.slab()
                            ? new GtStoneSlabBlock(variant, gtStoneProperties())
                            : new GtStoneBlock(variant, gtStoneProperties()));
            if (blocks.put(variant.id(), block) != null) {
                throw new IllegalStateException(
                        "Duplicate GT stone block " + variant.id());
            }
        }
        if (blocks.size() != GtStoneCatalog.VARIANT_COUNT) {
            throw new IllegalStateException(
                    "GT stone registration drifted from "
                            + GtStoneCatalog.VARIANT_COUNT
                            + " variants");
        }
        return java.util.Collections.unmodifiableMap(blocks);
    }

    public static Map<ResourceLocation, DeferredBlock<Block>>
            gtStoneBlocksById() {
        return GT_STONE_BLOCKS;
    }

    public static Collection<DeferredBlock<Block>> gtStoneBlocks() {
        return GT_STONE_BLOCKS.values();
    }

    private static Map<ResourceLocation, DeferredBlock<Block>>
            registerGtBlockObjectBlocks() {
        LinkedHashMap<ResourceLocation, DeferredBlock<Block>> blocks =
                new LinkedHashMap<>();
        for (GtBlockObjectCatalog.Variant variant : GtBlockObjectCatalog.variants()) {
            DeferredBlock<Block> block = BLOCKS.register(
                    variant.registryPath(),
                    () -> createGtBlockObject(variant));
            if (blocks.put(variant.id(), block) != null) {
                throw new IllegalStateException(
                        "Duplicate GT block-object block " + variant.id());
            }
        }
        if (blocks.size() != GtBlockObjectCatalog.VARIANT_COUNT) {
            throw new IllegalStateException(
                    "GT block-object registration drifted from "
                            + GtBlockObjectCatalog.VARIANT_COUNT
                            + " variants");
        }
        return java.util.Collections.unmodifiableMap(blocks);
    }

    private static Map<ResourceLocation, DeferredBlock<Block>>
            registerBathRemainderBlockObjectBlocks() {
        LinkedHashMap<ResourceLocation, DeferredBlock<Block>> blocks =
                new LinkedHashMap<>();
        for (GtBlockObjectCatalog.Variant variant : BathRemainderBlockObjectCatalog.variants()) {
            DeferredBlock<Block> block = BLOCKS.register(
                    variant.registryPath(),
                    () -> createGtBlockObject(variant));
            if (blocks.put(variant.id(), block) != null) {
                throw new IllegalStateException(
                        "Duplicate bath remainder block-object block " + variant.id());
            }
        }
        if (blocks.size() != BathRemainderBlockObjectCatalog.VARIANT_COUNT) {
            throw new IllegalStateException(
                    "Bath remainder block-object registration drifted from "
                            + BathRemainderBlockObjectCatalog.VARIANT_COUNT
                            + " variants");
        }
        return java.util.Collections.unmodifiableMap(blocks);
    }

    private static Block createGtBlockObject(GtBlockObjectCatalog.Variant variant) {
        if (variant.slab()) {
            return new GtBlockObjectSlabBlock(variant, gtBlockObjectProperties(variant));
        }
        if (variant.log()) {
            return new GtBlockObjectLogBlock(variant, gtBlockObjectLogProperties(variant));
        }
        if (variant.bars()) {
            return new GtBlockObjectBarsBlock(
                    variant,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS));
        }
        if (variant.rail()) {
            return new GtBlockObjectRailBlock(
                    variant, BlockBehaviour.Properties.ofFullCopy(Blocks.RAIL));
        }
        if (variant.spike()) {
            return new GtBlockObjectSpikeBlock(variant, gtBlockObjectSpikeProperties());
        }
        if (variant.bale()) {
            return new GtBlockObjectBaleBlock(
                    variant, BlockBehaviour.Properties.ofFullCopy(Blocks.HAY_BLOCK));
        }
        if (variant.cfoamFresh()) {
            return new GtBlockObjectCFoamFreshBlock(
                    variant, gtBlockObjectFreshCFoamProperties());
        }
        return new GtBlockObjectBlock(variant, gtBlockObjectProperties(variant));
    }

    private static BlockBehaviour.Properties gtBlockObjectProperties(
            GtBlockObjectCatalog.Variant variant) {
        if (variant.cfoam()) {
            return BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOL)
                    .strength(0.8F)
                    .sound(SoundType.WOOL);
        }
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(1.5F, 6.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.STONE);
    }

    private static BlockBehaviour.Properties gtBlockObjectLogProperties(
            GtBlockObjectCatalog.Variant variant) {
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.0F)
                .sound(SoundType.WOOD);
        if (!variant.fireproof()) {
            properties = properties.ignitedByLava();
        }
        return properties;
    }

    private static BlockBehaviour.Properties gtBlockObjectSpikeProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(5.0F, 6.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL);
    }

    private static BlockBehaviour.Properties gtBlockObjectFreshCFoamProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOL)
                .strength(0.3F)
                .sound(SoundType.SLIME_BLOCK)
                .noOcclusion()
                .friction(0.8F);
    }

    public static Map<ResourceLocation, DeferredBlock<Block>>
            gtBlockObjectBlocksById() {
        return GT_BLOCK_OBJECT_BLOCKS;
    }

    public static Collection<DeferredBlock<Block>> gtBlockObjectBlocks() {
        return GT_BLOCK_OBJECT_BLOCKS.values();
    }

    public static Map<ResourceLocation, DeferredBlock<Block>>
            bathRemainderBlockObjectBlocksById() {
        return BATH_REMAINDER_BLOCK_OBJECT_BLOCKS;
    }

    public static Collection<DeferredBlock<Block>> bathRemainderBlockObjectBlocks() {
        return BATH_REMAINDER_BLOCK_OBJECT_BLOCKS.values();
    }

    private static BlockBehaviour.Properties gtStoneProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(1.5F, 6.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.STONE);
    }

    public static Block[] hopperBlockArray() {
        return HOPPER_BLOCKS.values().stream()
                .map(DeferredBlock::get)
                .toArray(Block[]::new);
    }

    private static Map<ResourceLocation, DeferredBlock<? extends StorageHostBlock>>
            registerStorageBlocks() {
        LinkedHashMap<ResourceLocation, DeferredBlock<? extends StorageHostBlock>>
                blocks = new LinkedHashMap<>();
        for (StorageVariant variant : StorageVariantCatalog.variants()) {
            DeferredBlock<? extends StorageHostBlock> block = BLOCKS.register(
                    variant.path(),
                    () -> createStorageBlock(variant));
            if (blocks.put(variant.id(), block) != null) {
                throw new IllegalStateException(
                        "Duplicate storage block " + variant.id());
            }
        }
        if (blocks.size() != StorageVariantCatalog.TOTAL_COUNT) {
            throw new IllegalStateException(
                    "Storage registration drifted from "
                            + StorageVariantCatalog.TOTAL_COUNT
                            + " variants");
        }
        return java.util.Collections.unmodifiableMap(blocks);
    }

    private static StorageHostBlock createStorageBlock(StorageVariant variant) {
        BlockBehaviour.Properties properties = storageProperties(variant);
        return switch (variant.behavior()) {
            case BOOKSHELF -> new BookshelfBlock(variant, properties);
            case BOTTLE_CRATE -> new BottleCrateBlock(variant, properties);
            case DRAWER -> new DrawerBlock(variant, properties);
            case LOCKER, LOCKER_CHARGING -> new LockerBlock(variant, properties);
            case MASS_STORAGE, MASS_STORAGE_LOGISTICS ->
                    new MassStorageBlock(variant, properties);
            case STORAGE_INSERTER -> new StorageInserterBlock(variant, properties);
        };
    }

    private static BlockBehaviour.Properties storageProperties(
            StorageVariant variant) {
        boolean wood = variant.plankIndex() != null
                || "mass_storage_barrel".equals(variant.family())
                || "mass_storage_box".equals(variant.family());
        return BlockBehaviour.Properties.of()
                .mapColor(wood ? MapColor.WOOD : MapColor.METAL)
                .strength(wood ? 2.0F : 3.5F, wood ? 2.0F : 8.0F)
                .requiresCorrectToolForDrops()
                .sound(wood ? SoundType.WOOD : SoundType.METAL);
    }

    public static Map<ResourceLocation, DeferredBlock<? extends StorageHostBlock>>
            storageBlocksById() {
        return STORAGE_BLOCKS;
    }

    public static Collection<DeferredBlock<? extends StorageHostBlock>>
            variantStorageBlocks() {
        return STORAGE_BLOCKS.values();
    }

    public static Block[] storageBlockArray(StorageBehaviorProfile profile) {
        return StorageVariantCatalog.of(profile).stream()
                .map(variant -> STORAGE_BLOCKS.get(variant.id()).get())
                .toArray(Block[]::new);
    }

    public static Block[] lockerBlockArray() {
        return java.util.stream.Stream.concat(
                        StorageVariantCatalog.of(StorageBehaviorProfile.LOCKER).stream(),
                        StorageVariantCatalog.of(
                                StorageBehaviorProfile.LOCKER_CHARGING).stream())
                .map(variant -> STORAGE_BLOCKS.get(variant.id()).get())
                .toArray(Block[]::new);
    }

    public static Block[] massStorageBlockArray() {
        return java.util.stream.Stream.concat(
                        StorageVariantCatalog.of(
                                StorageBehaviorProfile.MASS_STORAGE).stream(),
                        StorageVariantCatalog.of(
                                StorageBehaviorProfile.MASS_STORAGE_LOGISTICS).stream())
                .map(variant -> STORAGE_BLOCKS.get(variant.id()).get())
                .toArray(Block[]::new);
    }

    private static DeferredBlock<ProcessingMachineBlock> processing(
            String id,
            com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec spec) {
        DeferredBlock<ProcessingMachineBlock> block =
                BLOCKS.register(id, () -> new ProcessingMachineBlock(
                        spec, processingProperties(id)));
        if (CONFIGURED_PROCESSING_BLOCKS.put(spec.id(), block) != null) {
            throw new IllegalStateException(
                    "Duplicate configured processing block for " + spec.id());
        }
        return block;
    }

    private static DeferredBlock<ProcessingMachineBlock> processing(
            String id, MachineVariant variant) {
        DeferredBlock<ProcessingMachineBlock> block =
                BLOCKS.register(
                        id,
                        () -> new ProcessingMachineBlock(
                                variant, processingProperties(id)));
        if (CONFIGURED_PROCESSING_BLOCKS.put(
                        variant.id(), block)
                != null) {
            throw new IllegalStateException(
                    "Duplicate configured processing block for "
                            + variant.id());
        }
        if (CONFIGURED_PROCESSING_VARIANTS.putIfAbsent(
                        variant.id(), variant)
                != null) {
            throw new IllegalStateException(
                    "Duplicate source-backed processing variant for "
                            + variant.id());
        }
        return block;
    }

    /**
     * Resolves the registered workstation for one configured processing spec.
     *
     * <p>The identity check prevents an equal-looking ad-hoc spec from being
     * accepted as registry-owned configuration.
     */
    public static Block configuredProcessingBlock(
            com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec spec) {
        Objects.requireNonNull(spec, "spec");
        boolean complete = ModProcessingMachines.CONFIGURED_MACHINES.stream()
                .allMatch(machine ->
                        machine == ModProcessingMachines.LASER_ENGRAVER
                                || CONFIGURED_PROCESSING_BLOCKS.containsKey(machine.id())
                                || !ModMachineVariants.forKind(machine.id())
                                        .isEmpty());
        if (!complete) {
            throw new IllegalStateException(
                    "Configured processing block mapping is incomplete");
        }
        if (spec == ModProcessingMachines.LASER_ENGRAVER) {
            return LASER_ENGRAVER.get();
        }
        boolean configured = ModProcessingMachines.CONFIGURED_MACHINES.stream()
                .anyMatch(candidate -> candidate == spec);
        DeferredBlock<ProcessingMachineBlock> holder =
                CONFIGURED_PROCESSING_BLOCKS.get(spec.id());
        if (holder == null) {
            java.util.List<MachineVariant> variants =
                    ModMachineVariants.forKind(spec.id());
            if (!variants.isEmpty()) {
                holder = CONFIGURED_PROCESSING_BLOCKS.get(
                        variants.getFirst().id());
            }
        }
        if (!configured || holder == null) {
            throw new IllegalArgumentException(
                    "No configured processing block for " + spec.id());
        }
        return holder.get();
    }

    public static Block configuredProcessingBlock(
            MachineVariant variant) {
        Objects.requireNonNull(variant, "variant");
        DeferredBlock<ProcessingMachineBlock> holder =
                CONFIGURED_PROCESSING_BLOCKS.get(variant.id());
        if (holder == null) {
            throw new IllegalArgumentException(
                    "No configured processing block for "
                            + variant.id());
        }
        return holder.get();
    }

    public static Block[] configuredProcessingBlocks() {
        return CONFIGURED_PROCESSING_BLOCKS.values().stream()
                .map(DeferredBlock::get)
                .toArray(Block[]::new);
    }

    private static BlockBehaviour.Properties machineProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_ORANGE)
                .strength(3.5F, 8.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL);
    }

    private static BlockBehaviour.Properties processingProperties(String id) {
        BlockBehaviour.Properties properties = machineProperties();
        if (usesShapedVoxelModel(id)) {
            properties.noOcclusion();
        }
        return properties;
    }

    private static boolean usesShapedVoxelModel(String id) {
        return switch (id) {
            case "mortar",
                    "bath",
                    "sifter",
                    "steel_sifter",
                    "titanium_sifter",
                    "smelter",
                    "invar_smelter",
                    "titanium_smelter" -> true;
            default -> false;
        };
    }

    private static BlockBehaviour.Properties conductorProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(0.5F, 2.0F)
                .requiresCorrectToolForDrops()
                .noOcclusion()
                .sound(SoundType.METAL);
    }

    private static BlockBehaviour.Properties storageProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(5.0F, 6.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL);
    }

    private static BlockBehaviour.Properties casingProperties(MaterialPrefix form) {
        float hardness;
        float resistance;
        if (form.equals(MaterialPrefixes.MACHINE_CASING_DENSE)) {
            hardness = 9.0F;
            resistance = 18.0F;
        } else if (form.equals(MaterialPrefixes.MACHINE_CASING_QUADRUPLE)) {
            hardness = 4.0F;
            resistance = 10.0F;
        } else if (form.equals(MaterialPrefixes.MACHINE_CASING_DOUBLE)) {
            hardness = 2.0F;
            resistance = 6.0F;
        } else {
            hardness = 1.0F;
            resistance = 3.0F;
        }
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(hardness, resistance)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL);
    }

    private static BlockBehaviour.Properties rockProperties() {
        // GT6 rocks break by hand — the early-game cobblestone source.
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(0.5F)
                .sound(SoundType.STONE)
                .noOcclusion();
    }

    public record OreBlockKey(String materialId, Host host) {
        public OreBlockKey {
            if (materialId == null || !materialId.matches("[a-z0-9_]+") || host == null) {
                throw new IllegalArgumentException("Invalid material ore block key");
            }
        }
    }

    public record CasingBlockKey(String materialId, MaterialPrefix form) {
        public CasingBlockKey {
            if (materialId == null || !materialId.matches("[a-z0-9_]+") || form == null) {
                throw new IllegalArgumentException("Invalid material casing block key");
            }
        }
    }

    private ModBlocks() {}
}
