package com.masson.cruciblecraft.registry;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.energy.battery.BatteryBlockItem;
import com.masson.cruciblecraft.energy.battery.EnergyBatteryCatalog;
import com.masson.cruciblecraft.energy.transformer.EnergyTransformerCatalog;
import com.masson.cruciblecraft.energy.transformer.TransformerBlockItem;
import com.masson.cruciblecraft.energy.heatexchanger.HeatExchangerBlockItem;
import com.masson.cruciblecraft.energy.largeheatexchanger.LargeHeatExchangerBlockItem;
import com.masson.cruciblecraft.energy.quantum.QuantumEnergizerBlockItem;
import com.masson.cruciblecraft.energy.quantum.QuantumEnergizerCatalog;
import com.masson.cruciblecraft.energy.longdistance.LongDistanceTransformerBlockItem;
import com.masson.cruciblecraft.energy.longdistance.LongDistanceTransformerCatalog;
import com.masson.cruciblecraft.energy.heatexchanger.HeatExchangerCatalog;
import com.masson.cruciblecraft.energy.converter.EnergyConverterCatalog;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.item.MaterialCasingBlockItem;
import com.masson.cruciblecraft.content.item.MaterialItem;
import com.masson.cruciblecraft.content.item.MaterialStorageBlockItem;
import com.masson.cruciblecraft.content.item.CableBlockItem;
import com.masson.cruciblecraft.content.item.CatalogNamedBlockItem;
import com.masson.cruciblecraft.content.item.CatalogNamedItem;
import com.masson.cruciblecraft.content.item.RedstoneWireBlockItem;
import com.masson.cruciblecraft.content.redstonewire.RedstoneWireKind;
import com.masson.cruciblecraft.content.item.CellItem;
import com.masson.cruciblecraft.content.item.CeramicMoldBlockItem;
import com.masson.cruciblecraft.content.item.ExtruderShapeCatalog;
import com.masson.cruciblecraft.content.item.GtWoodCatalog;
import com.masson.cruciblecraft.worldgen.OreHostVariantCatalog.Host;
import com.masson.cruciblecraft.worldgen.StoneLayerStones;
import com.masson.cruciblecraft.worldgen.tree.prep.GtTreeSpecies;
import com.masson.cruciblecraft.content.item.BathMteIdentityCatalog;
import com.masson.cruciblecraft.content.item.TechnologicalPartCatalog;
import com.masson.cruciblecraft.content.item.SmelterMteIdentityCatalog;
import com.masson.cruciblecraft.content.mte.MteInPlaceCatalog;
import com.masson.cruciblecraft.content.mte.MteInPlaceSpec;
import com.masson.cruciblecraft.content.item.GtBlockObjectCatalog;
import com.masson.cruciblecraft.content.item.BathRemainderBlockObjectCatalog;
import com.masson.cruciblecraft.content.item.BathIdentityCatalog;
import com.masson.cruciblecraft.content.item.GtSurfaceRockItem;
import com.masson.cruciblecraft.content.item.PebbleBlockItem;
import com.masson.cruciblecraft.content.item.SemanticObjectCatalog;
import com.masson.cruciblecraft.content.item.SlicerOperandCatalog;
import com.masson.cruciblecraft.content.item.PressureWasherOperandCatalog;
import com.masson.cruciblecraft.content.item.GtStoneCatalog;
import com.masson.cruciblecraft.content.item.BatteryCellItem;
import com.masson.cruciblecraft.content.item.FlintKnifeItem;
import com.masson.cruciblecraft.content.item.GeigerCounterItem;
import com.masson.cruciblecraft.content.item.HazmatArmorItem;
import com.masson.cruciblecraft.content.item.ElectroMeterItem;
import com.masson.cruciblecraft.content.item.TachoMeterItem;
import com.masson.cruciblecraft.content.item.ThermometerItem;
import com.masson.cruciblecraft.content.item.HopperBlockItem;
import com.masson.cruciblecraft.content.storage.StorageVariant;
import com.masson.cruciblecraft.content.storage.StorageVariantCatalog;
import com.masson.cruciblecraft.content.item.MaterialAxeItem;
import com.masson.cruciblecraft.content.item.MaterialBranchCutterItem;
import com.masson.cruciblecraft.content.item.MaterialButcheryKnifeItem;
import com.masson.cruciblecraft.content.item.MaterialChiselItem;
import com.masson.cruciblecraft.content.item.MaterialClubItem;
import com.masson.cruciblecraft.content.item.MaterialConstructionPickItem;
import com.masson.cruciblecraft.content.item.MaterialCrowbarItem;
import com.masson.cruciblecraft.content.item.MaterialDoubleAxeItem;
import com.masson.cruciblecraft.content.item.MaterialFileItem;
import com.masson.cruciblecraft.content.item.MaterialFlintAndTinderItem;
import com.masson.cruciblecraft.content.item.MaterialGemPickItem;
import com.masson.cruciblecraft.content.item.MaterialHoeItem;
import com.masson.cruciblecraft.content.item.MaterialKnifeItem;
import com.masson.cruciblecraft.content.item.MaterialMachineBlockItem;
import com.masson.cruciblecraft.content.item.MaterialMonkeyWrenchItem;
import com.masson.cruciblecraft.content.item.MaterialPickaxeItem;
import com.masson.cruciblecraft.content.item.MaterialPlowItem;
import com.masson.cruciblecraft.content.item.MaterialPlungerItem;
import com.masson.cruciblecraft.content.item.MaterialSawItem;
import com.masson.cruciblecraft.content.item.MaterialScoopItem;
import com.masson.cruciblecraft.content.item.MaterialScissorsItem;
import com.masson.cruciblecraft.content.item.MaterialScrewdriverItem;
import com.masson.cruciblecraft.content.item.MaterialSenseItem;
import com.masson.cruciblecraft.content.item.MaterialShovelItem;
import com.masson.cruciblecraft.content.item.MaterialSoftHammerItem;
import com.masson.cruciblecraft.content.item.MaterialSpadeItem;
import com.masson.cruciblecraft.content.item.MaterialSwordItem;
import com.masson.cruciblecraft.content.item.MaterialUniversalSpadeItem;
import com.masson.cruciblecraft.content.item.MaterialWireCutterItem;
import com.masson.cruciblecraft.content.item.MaterialWorkshopToolItem;
import com.masson.cruciblecraft.content.item.MaterialWrenchItem;
import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;
import com.masson.cruciblecraft.content.item.LuFiberCableItem;
import com.masson.cruciblecraft.content.item.PortableFluidTankItem;
import com.masson.cruciblecraft.content.item.ProgrammedCircuitItem;
import com.masson.cruciblecraft.content.item.ReactorRodItem;
import com.masson.cruciblecraft.content.item.PipeBlockItem;
import com.masson.cruciblecraft.nuclear.ReactorRodCatalog;
import com.masson.cruciblecraft.content.item.PipeCoverItem;
import com.masson.cruciblecraft.content.item.SmithingHammerItem;
import com.masson.cruciblecraft.content.item.ToolPatternCatalog;
import com.masson.cruciblecraft.content.item.UnknownMaterialItem;
import com.masson.cruciblecraft.machine.MachineMaterialRules.Device;
import com.masson.cruciblecraft.content.mold.MoldShape;
import com.masson.cruciblecraft.machine.processing.MachineVariant;
import com.masson.cruciblecraft.logistics.hopper.HopperVariant;
import com.masson.cruciblecraft.logistics.hopper.HopperVariantCatalog;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverKinds;
import com.masson.cruciblecraft.content.sensor.SensorKind;
import com.masson.cruciblecraft.material.CellContentGate;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.logistics.pipe.PipeCatalog;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverComponentTiers;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCoverType;
import com.masson.cruciblecraft.worldgen.OreHostVariantCatalog.Host;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CrucibleCraft.MODID);
    public static final Map<String, DeferredItem<Item>> EXTRUDER_SHAPES =
            ExtruderShapeCatalog.registerAll(ITEMS);
    public static final Map<String, DeferredItem<Item>> TOOL_PATTERNS =
            ToolPatternCatalog.registerAll(ITEMS);
    public static final Map<String, DeferredItem<Item>> GT_WOODS =
            GtWoodCatalog.registerAll(ITEMS);
    private static final Map<String, DeferredItem<? extends Item>>
            MATERIAL_ITEMS = new LinkedHashMap<>();
    private static final Map<ModBlocks.OreBlockKey, DeferredItem<BlockItem>>
            MATERIAL_ORE_ITEMS = new LinkedHashMap<>();

    public static final DeferredItem<BlockItem> FIREBRICK = ITEMS.registerSimpleBlockItem("firebrick", ModBlocks.FIREBRICK);
    private static final Map<GtTreeSpecies, DeferredItem<BlockItem>> TREE_SAPLING_ITEMS =
            new LinkedHashMap<>();
    private static final Map<GtTreeSpecies, DeferredItem<BlockItem>> TREE_LOG_ITEMS =
            new LinkedHashMap<>();
    private static final Map<GtTreeSpecies, DeferredItem<BlockItem>> TREE_LEAVES_ITEMS =
            new LinkedHashMap<>();
    private static final Map<
            com.masson.cruciblecraft.worldgen.crop.GlowtusColor,
            DeferredItem<BlockItem>> GLOWTUS_ITEMS =
                    new LinkedHashMap<>();
    public static final DeferredItem<Item> RUBBER_RESIN = ITEMS.register(
            "tree/rubber_resin", () -> new Item(new Item.Properties()));
    public static final DeferredItem<BlockItem> GT_BUSH =
            ITEMS.registerSimpleBlockItem("plant/gt_bush", ModBlocks.GT_BUSH);
    public static final DeferredItem<GtSurfaceRockItem> GT_SURFACE_ROCK =
            ITEMS.register(
                    "gt_surface_rock",
                    () -> new GtSurfaceRockItem(
                            ModBlocks.GT_SURFACE_ROCK.get(),
                            new Item.Properties()));

    static {
        registerGtTrees();
        registerGtCrops();
    }
    public static final DeferredItem<LuFiberCableItem> LU_FIBER_CABLE =
            ITEMS.register(
                    "lu_fiber_cable",
                    () -> new LuFiberCableItem(
                            ModBlocks.LU_FIBER_CABLE.get(),
                            new Item.Properties()));
    public static final DeferredItem<BlockItem> LASER_ENGRAVER =
            ITEMS.registerSimpleBlockItem(
                    "laser_engraver",
                    ModBlocks.LASER_ENGRAVER);
    public static final DeferredItem<BlockItem> FUSION_REACTOR =
            ITEMS.registerSimpleBlockItem(
                    "fusion_reactor",
                    ModBlocks.FUSION_REACTOR);
    public static final DeferredItem<LargeHeatExchangerBlockItem> LARGE_HEAT_EXCHANGER =
            ITEMS.register(
                    "large_heat_exchanger",
                    () -> new LargeHeatExchangerBlockItem(
                            ModBlocks.LARGE_HEAT_EXCHANGER.get(),
                            new Item.Properties()));
    public static final DeferredItem<BlockItem> BEDROCK_DRILL =
            ITEMS.registerSimpleBlockItem(
                    "bedrock_drill",
                    ModBlocks.BEDROCK_DRILL);
    public static final DeferredItem<BlockItem> BEDROCK_DRILL_HEAD =
            ITEMS.registerSimpleBlockItem(
                    "bedrock_drill_head",
                    ModBlocks.BEDROCK_DRILL_HEAD);
    public static final DeferredItem<BlockItem> REACTOR_CORE_1X1 =
            ITEMS.registerSimpleBlockItem(
                    "reactor_core_1x1",
                    ModBlocks.REACTOR_CORE_1X1);
    public static final DeferredItem<BlockItem> REACTOR_CORE_2X2 =
            ITEMS.registerSimpleBlockItem(
                    "reactor_core_2x2",
                    ModBlocks.REACTOR_CORE_2X2);
    public static final DeferredItem<BlockItem> TUNGSTENSTEEL_WALL =
            ITEMS.registerSimpleBlockItem(
                    "tungstensteel_wall", ModBlocks.TUNGSTENSTEEL_WALL);
    public static final DeferredItem<BlockItem> STAINLESS_STEEL_WALL =
            ITEMS.registerSimpleBlockItem(
                    "stainless_steel_wall", ModBlocks.STAINLESS_STEEL_WALL);
    public static final DeferredItem<BlockItem> LARGE_IRIDIUM_COIL =
            ITEMS.registerSimpleBlockItem(
                    "large_iridium_coil", ModBlocks.LARGE_IRIDIUM_COIL);
    public static final DeferredItem<MaterialMachineBlockItem> CRUCIBLE = ITEMS.register(
            "crucible",
            () -> new MaterialMachineBlockItem(
                    ModBlocks.CRUCIBLE.get(),
                    Device.CRUCIBLE,
                    new Item.Properties()));
    public static final DeferredItem<MaterialMachineBlockItem> ANVIL = ITEMS.register(
            "anvil",
            () -> new MaterialMachineBlockItem(
                    ModBlocks.ANVIL.get(),
                    Device.ANVIL,
                    new Item.Properties()));
    public static final DeferredItem<BlockItem> COKE_OVEN =
            ITEMS.registerSimpleBlockItem("coke_oven", ModBlocks.COKE_OVEN);
    public static final DeferredItem<BlockItem> MULTIBLOCK_CASING =
            ITEMS.registerSimpleBlockItem(
                    "multiblock_casing", ModBlocks.MULTIBLOCK_CASING);
    public static final DeferredItem<BlockItem>
            MULTIBLOCK_ITEM_FLUID_PORT = ITEMS.registerSimpleBlockItem(
                    "multiblock_item_fluid_port",
                    ModBlocks.MULTIBLOCK_ITEM_FLUID_PORT);
    public static final DeferredItem<BlockItem>
            MULTIBLOCK_ENERGY_INPUT_PORT = ITEMS.registerSimpleBlockItem(
                    "multiblock_energy_input_port",
                    ModBlocks.MULTIBLOCK_ENERGY_INPUT_PORT);
    public static final DeferredItem<BlockItem> LARGE_CENTRIFUGE =
            ITEMS.registerSimpleBlockItem(
                    "large_centrifuge", ModBlocks.LARGE_CENTRIFUGE);
    public static final DeferredItem<BlockItem> DISTILLATION_TOWER =
            ITEMS.registerSimpleBlockItem(
                    "distillation_tower", ModBlocks.DISTILLATION_TOWER);
    public static final DeferredItem<BlockItem> LARGE_BOILER =
            ITEMS.registerSimpleBlockItem(
                    "large_boiler", ModBlocks.LARGE_BOILER);
    public static final DeferredItem<BlockItem> TANK_3X3X3 =
            ITEMS.registerSimpleBlockItem(
                    "tank_3x3x3", ModBlocks.TANK_3X3X3);
    public static final DeferredItem<BlockItem> LARGE_CRUCIBLE =
            ITEMS.registerSimpleBlockItem(
                    "large_crucible", ModBlocks.LARGE_CRUCIBLE);
    public static final DeferredItem<BlockItem> LOGISTICS_CORE =
            ITEMS.registerSimpleBlockItem(
                    "logistics_core", ModBlocks.LOGISTICS_CORE);
    public static final DeferredItem<BlockItem> GALVANIZED_STEEL_WALL =
            ITEMS.registerSimpleBlockItem(
                    "galvanized_steel_wall", ModBlocks.GALVANIZED_STEEL_WALL);
    public static final DeferredItem<BlockItem> VENTILATION_UNIT =
            ITEMS.registerSimpleBlockItem(
                    "ventilation_unit", ModBlocks.VENTILATION_UNIT);
    public static final DeferredItem<BlockItem> VERSATILE_PROCESSOR_UNIT =
            ITEMS.registerSimpleBlockItem(
                    "versatile_processor_unit",
                    ModBlocks.VERSATILE_PROCESSOR_UNIT);
    public static final DeferredItem<BlockItem> LOGIC_PROCESSOR_UNIT =
            ITEMS.registerSimpleBlockItem(
                    "logic_processor_unit", ModBlocks.LOGIC_PROCESSOR_UNIT);
    public static final DeferredItem<BlockItem> CONTROL_PROCESSOR_UNIT =
            ITEMS.registerSimpleBlockItem(
                    "control_processor_unit",
                    ModBlocks.CONTROL_PROCESSOR_UNIT);
    public static final DeferredItem<BlockItem> STORAGE_PROCESSOR_UNIT =
            ITEMS.registerSimpleBlockItem(
                    "storage_processor_unit",
                    ModBlocks.STORAGE_PROCESSOR_UNIT);
    public static final DeferredItem<BlockItem> CONVERSION_PROCESSOR_UNIT =
            ITEMS.registerSimpleBlockItem(
                    "conversion_processor_unit",
                    ModBlocks.CONVERSION_PROCESSOR_UNIT);

    public static final DeferredItem<Item> RAW_CERAMIC_CRUCIBLE =
            ITEMS.registerSimpleItem("raw_ceramic_crucible", new Item.Properties());
    public static final DeferredItem<Item> RAW_CERAMIC_MOLD =
            ITEMS.registerSimpleItem("raw_ceramic_mold", new Item.Properties());
    public static final DeferredItem<Item> RAW_INGOT_MOLD =
            ITEMS.registerSimpleItem("raw_ingot_mold", new Item.Properties());
    public static final DeferredItem<Item> RAW_PLATE_MOLD =
            ITEMS.registerSimpleItem("raw_plate_mold", new Item.Properties());
    public static final DeferredItem<Item> RAW_ROD_MOLD =
            ITEMS.registerSimpleItem("raw_rod_mold", new Item.Properties());
    public static final DeferredItem<Item> RAW_BOLT_MOLD =
            ITEMS.registerSimpleItem("raw_bolt_mold", new Item.Properties());
    public static final DeferredItem<CeramicMoldBlockItem> INGOT_MOLD = mold("ingot_mold", MoldShape.INGOT);
    public static final DeferredItem<CeramicMoldBlockItem> PLATE_MOLD = mold("plate_mold", MoldShape.PLATE);
    public static final DeferredItem<CeramicMoldBlockItem> ROD_MOLD = mold("rod_mold", MoldShape.ROD);
    public static final DeferredItem<CeramicMoldBlockItem> BOLT_MOLD = mold("bolt_mold", MoldShape.BOLT);
    public static final DeferredItem<Item> MATCH =
            ITEMS.registerSimpleItem("match", new Item.Properties());
    public static final DeferredItem<ProgrammedCircuitItem> PROGRAMMED_CIRCUIT =
            ITEMS.register(
                    "programmed_circuit",
                    () -> new ProgrammedCircuitItem(new Item.Properties()));
    private static final Map<String, DeferredItem<ReactorRodItem>> REACTOR_RODS =
            registerReactorRods();
    private static final Map<ResourceLocation, DeferredItem<Item>> TECHNOLOGICAL_PARTS =
            registerTechnologicalParts();
    private static final List<BatteryCellSpec> BATTERY_CELL_SPECS = List.of(
            new BatteryCellSpec("lead_acid", "sulfuric_acid", 288),
            new BatteryCellSpec("alkaline", "water_distilled", 1_000),
            new BatteryCellSpec("nickel_cadmium", "water_distilled", 1_000),
            new BatteryCellSpec("lithium_cobalt", "hydrochloric_acid", 288),
            new BatteryCellSpec("lithium_manganese", "hydrogen_fluoride", 288));
    private static final Map<String, DeferredItem<BatteryCellItem>>
            BATTERY_CELL_ITEMS = registerBatteryCellItems();
    private static final Map<ResourceLocation, DeferredItem<Item>> BATH_MTE_ITEMS =
            registerBathMteItems();
    private static final Map<ResourceLocation, DeferredItem<Item>> SMELTER_MTE_ITEMS =
            registerSmelterMteItems();
    private static final Map<ResourceLocation, DeferredItem<Item>> BATH_IDENTITY_ITEMS =
            registerBathIdentityItems();
    private static final Map<ResourceLocation, DeferredItem<Item>> SEMANTIC_IDENTITY_ITEMS =
            registerSemanticIdentityItems();
    private static final Map<ResourceLocation, DeferredItem<Item>> SLICER_OPERAND_ITEMS =
            registerSlicerOperandItems();
    private static final Map<ResourceLocation, DeferredItem<Item>>
            PRESSURE_WASHER_OPERAND_ITEMS = registerPressureWasherOperandItems();
    public static DeferredItem<? extends Item> BRONZE_DOUBLE_MACHINE_CASING;
    public static DeferredItem<? extends Item> STEEL_DOUBLE_MACHINE_CASING;
    public static DeferredItem<? extends Item> TITANIUM_DOUBLE_MACHINE_CASING;
    public static DeferredItem<? extends Item> STEEL_GALVANIZED_MACHINE_CASING;
    public static DeferredItem<? extends Item> ALUMINIUM_MACHINE_CASING;
    public static DeferredItem<? extends Item> STAINLESS_STEEL_MACHINE_CASING;
    public static DeferredItem<? extends Item> CHROMIUM_MACHINE_CASING;
    public static DeferredItem<? extends Item> TITANIUM_MACHINE_CASING;
    public static DeferredItem<? extends Item>
            TUNGSTENSTEEL_DOUBLE_MACHINE_CASING;
    public static DeferredItem<? extends Item> INVAR_DOUBLE_MACHINE_CASING;
    public static DeferredItem<? extends Item>
            TUNGSTEN_CARBIDE_DOUBLE_MACHINE_CASING;
    public static final DeferredItem<BucketItem> CREOSOTE_BUCKET = ITEMS.register(
            "creosote_bucket",
            () -> new BucketItem(
                    ModFluids.CREOSOTE_SOURCE.get(),
                    new Item.Properties()
                            .craftRemainder(Items.BUCKET)
                            .stacksTo(1)));
    public static final DeferredItem<BucketItem> STEAM_BUCKET = ITEMS.register(
            "steam_bucket",
            () -> new BucketItem(
                    ModFluids.STEAM_SOURCE.get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
    public static final DeferredItem<PortableFluidTankItem> PORTABLE_FLUID_TANK =
            ITEMS.register(
                    "portable_fluid_tank",
                    () -> new PortableFluidTankItem(new Item.Properties()));
    public static final DeferredItem<CellItem> FLUID_CELL = ITEMS.register(
            "fluid_cell",
            () -> new CellItem(
                    new Item.Properties(),
                    ModComponents.FLUID_CELL_CONTENT,
                    CellContentGate.Kind.FLUID));
    public static final DeferredItem<CellItem> GAS_CELL = ITEMS.register(
            "gas_cell",
            () -> new CellItem(
                    new Item.Properties(),
                    ModComponents.GAS_CELL_CONTENT,
                    CellContentGate.Kind.GAS));
    public static final DeferredItem<PipeCoverItem> PIPE_FILTER_COVER =
            ITEMS.register(
                    "pipe_filter_cover",
                    () -> new PipeCoverItem(
                            PipeCoverType.FILTER,
                            new Item.Properties()));
    public static final DeferredItem<PipeCoverItem> PIPE_VALVE_COVER =
            ITEMS.register(
                    "pipe_valve_cover",
                    () -> new PipeCoverItem(
                            PipeCoverType.ONE_WAY_VALVE,
                            new Item.Properties()));
    public static final DeferredItem<PipeCoverItem> PIPE_PUMP_COVER =
            ITEMS.register(
                    "pipe_pump_cover",
                    () -> new PipeCoverItem(
                            PipeCoverType.OUTPUT_PUMP,
                            new Item.Properties()));
    public static final DeferredItem<PipeCoverItem> CONVEYOR_COVER =
            ITEMS.register(
                    "conveyor_cover",
                    () -> new PipeCoverItem(
                            "cruciblecraft:conveyor",
                            new Item.Properties()));
    public static final DeferredItem<PipeCoverItem> RETRIEVER_ITEM_COVER =
            ITEMS.register(
                    "retriever_item_cover",
                    () -> new PipeCoverItem(
                            "cruciblecraft:retriever_item",
                            new Item.Properties()));
    public static final DeferredItem<PipeCoverItem> ROBOT_ARM_COVER =
            ITEMS.register(
                    "robot_arm_cover",
                    () -> new PipeCoverItem(
                            "cruciblecraft:robot_arm",
                            new Item.Properties()));
    public static final DeferredItem<PipeCoverItem> PRESSURE_VALVE_COVER =
            ITEMS.register(
                    "pressure_valve_cover",
                    () -> new PipeCoverItem(
                            "cruciblecraft:pressure_valve",
                            new Item.Properties()));
    public static final DeferredItem<PipeCoverItem> SELECTOR_MANUAL_COVER =
            ITEMS.register(
                    "selector_manual_cover",
                    () -> new PipeCoverItem(
                            "cruciblecraft:selector_manual",
                            new Item.Properties()));
    public static final DeferredItem<PipeCoverItem> LOGISTICS_ITEM_STORAGE_COVER =
            ITEMS.register(
                    "logistics_item_storage_cover",
                    () -> new PipeCoverItem(
                            "cruciblecraft:logistics_item_storage",
                            new Item.Properties()));
    public static final DeferredItem<PipeCoverItem> LOGISTICS_ITEM_IMPORT_COVER =
            ITEMS.register(
                    "logistics_item_import_cover",
                    () -> new PipeCoverItem(
                            "cruciblecraft:logistics_item_import",
                            new Item.Properties()));
    public static final DeferredItem<PipeCoverItem> LOGISTICS_ITEM_EXPORT_COVER =
            ITEMS.register(
                    "logistics_item_export_cover",
                    () -> new PipeCoverItem(
                            "cruciblecraft:logistics_item_export",
                            new Item.Properties()));
    public static final DeferredItem<PipeCoverItem> LOGISTICS_FLUID_STORAGE_COVER =
            ITEMS.register(
                    "logistics_fluid_storage_cover",
                    () -> new PipeCoverItem(
                            "cruciblecraft:logistics_fluid_storage",
                            new Item.Properties()));
    public static final DeferredItem<PipeCoverItem> LOGISTICS_FLUID_IMPORT_COVER =
            ITEMS.register(
                    "logistics_fluid_import_cover",
                    () -> new PipeCoverItem(
                            "cruciblecraft:logistics_fluid_import",
                            new Item.Properties()));
    public static final DeferredItem<PipeCoverItem> LOGISTICS_FLUID_EXPORT_COVER =
            ITEMS.register(
                    "logistics_fluid_export_cover",
                    () -> new PipeCoverItem(
                            "cruciblecraft:logistics_fluid_export",
                            new Item.Properties()));
    public static final DeferredItem<PipeCoverItem> LOGISTICS_GENERIC_STORAGE_COVER =
            ITEMS.register(
                    "logistics_generic_storage_cover",
                    () -> new PipeCoverItem(
                            "cruciblecraft:logistics_generic_storage",
                            new Item.Properties()));
    public static final DeferredItem<PipeCoverItem> LOGISTICS_GENERIC_IMPORT_COVER =
            ITEMS.register(
                    "logistics_generic_import_cover",
                    () -> new PipeCoverItem(
                            "cruciblecraft:logistics_generic_import",
                            new Item.Properties()));
    public static final DeferredItem<PipeCoverItem> LOGISTICS_GENERIC_EXPORT_COVER =
            ITEMS.register(
                    "logistics_generic_export_cover",
                    () -> new PipeCoverItem(
                            "cruciblecraft:logistics_generic_export",
                            new Item.Properties()));
    public static final DeferredItem<PipeCoverItem> LOGISTICS_GENERIC_DUMP_COVER =
            ITEMS.register(
                    "logistics_generic_dump_cover",
                    () -> new PipeCoverItem(
                            "cruciblecraft:logistics_generic_dump",
                            new Item.Properties()));
    public static final DeferredItem<PipeCoverItem> LOGISTICS_DISPLAY_CPU_LOGIC_COVER =
            ITEMS.register(
                    "logistics_display_cpu_logic_cover",
                    () -> new PipeCoverItem(
                            "cruciblecraft:logistics_display_cpu_logic",
                            new Item.Properties()));
    public static final DeferredItem<PipeCoverItem> LOGISTICS_DISPLAY_CPU_CONTROL_COVER =
            ITEMS.register(
                    "logistics_display_cpu_control_cover",
                    () -> new PipeCoverItem(
                            "cruciblecraft:logistics_display_cpu_control",
                            new Item.Properties()));
    public static final DeferredItem<PipeCoverItem> LOGISTICS_DISPLAY_CPU_STORAGE_COVER =
            ITEMS.register(
                    "logistics_display_cpu_storage_cover",
                    () -> new PipeCoverItem(
                            "cruciblecraft:logistics_display_cpu_storage",
                            new Item.Properties()));
    public static final DeferredItem<PipeCoverItem> LOGISTICS_DISPLAY_CPU_CONVERSION_COVER =
            ITEMS.register(
                    "logistics_display_cpu_conversion_cover",
                    () -> new PipeCoverItem(
                            "cruciblecraft:logistics_display_cpu_conversion",
                            new Item.Properties()));
    private static final Map<
            String, DeferredItem<PipeCoverItem>> COMPACT_ELECTRIC_COVERS =
                    registerCompactElectricCovers();
    private static final Map<
            String, DeferredItem<PipeCoverItem>> MACHINE_COVERS =
                    registerMachineCovers();
    private static final Map<
            ResourceLocation, DeferredItem<BlockItem>> CONVERTER_ITEMS =
                    registerConverterItems();
    private static final Map<
            ResourceLocation, DeferredItem<BatteryBlockItem>> BATTERY_ITEMS =
                    registerBatteryItems();
    private static final Map<
            ResourceLocation, DeferredItem<TransformerBlockItem>> TRANSFORMER_ITEMS =
                    registerTransformerItems();
    private static final Map<
            ResourceLocation, DeferredItem<QuantumEnergizerBlockItem>>
                    QUANTUM_ENERGIZER_ITEMS = registerQuantumEnergizerItems();
    private static final Map<
            ResourceLocation, DeferredItem<LongDistanceTransformerBlockItem>>
                    LONG_DISTANCE_TRANSFORMER_ITEMS =
                            registerLongDistanceTransformerItems();
    private static final Map<
            ResourceLocation, DeferredItem<BlockItem>> LONG_DISTANCE_WIRE_ITEMS =
                    registerLongDistanceWireItems();
    private static final Map<
            ResourceLocation, DeferredItem<HeatExchangerBlockItem>>
                    HEAT_EXCHANGER_ITEMS = registerHeatExchangerItems();
    public static final DeferredItem<BlockItem> BRONZE_BOILER =
            converterItem("bronze_boiler");
    public static final DeferredItem<BlockItem> BRONZE_STEAM_ENGINE =
            converterItem("bronze_steam_engine");
    public static final DeferredItem<BlockItem> BRONZE_DYNAMO =
            converterItem("bronze_dynamo");
    public static final DeferredItem<BlockItem> STEEL_GALVANIZED_ELECTRIC_MOTOR =
            converterItem("steel_galvanized_electric_motor");
    public static final DeferredItem<BlockItem> BRONZE_FUEL_ENGINE =
            converterItem("bronze_fuel_engine");
    public static final DeferredItem<BlockItem> BRONZE_BURNING_BOX_GAS =
            converterItem("bronze_burning_box_gas");
    public static final DeferredItem<BlockItem> BRONZE_BURNING_BOX_SOLID =
            converterItem("bronze_burning_box_solid");
    public static final DeferredItem<BlockItem> ROTATIONAL_AXLE =
            ITEMS.registerSimpleBlockItem(
                    "rotational_axle", ModBlocks.ROTATIONAL_AXLE);
    public static final DeferredItem<BlockItem> ROTATIONAL_GEARBOX =
            ITEMS.registerSimpleBlockItem(
                    "rotational_gearbox",
                    ModBlocks.ROTATIONAL_GEARBOX);
    public static final DeferredItem<BlockItem> BRONZE_CRUSHER =
            ITEMS.registerSimpleBlockItem("bronze_crusher", ModBlocks.BRONZE_CRUSHER);
    private static final Map<
            net.minecraft.resources.ResourceLocation,
            DeferredItem<BlockItem>> TIERED_PROCESSING_ITEMS =
                    registerTieredProcessingItems();
    public static final DeferredItem<BlockItem> SLUICE =
            tieredProcessingItem("sluice");
    public static final DeferredItem<BlockItem> BATH =
            tieredProcessingItem("bath");
    private static final Map<
            net.minecraft.resources.ResourceLocation,
            DeferredItem<HopperBlockItem>> HOPPER_ITEMS =
                    registerHopperItems();
    private static final Map<
            net.minecraft.resources.ResourceLocation,
            DeferredItem<CatalogNamedBlockItem>> MTE_INPLACE_ITEMS =
                    registerMteInPlaceItems();
    private static final Map<
            net.minecraft.resources.ResourceLocation,
            DeferredItem<BlockItem>> SENSOR_ITEMS =
                    registerSensorItems();
    private static final Map<
            net.minecraft.resources.ResourceLocation,
            DeferredItem<BlockItem>> STORAGE_ITEMS =
                    registerStorageItems();
    private static final Map<
            net.minecraft.resources.ResourceLocation,
            DeferredItem<BlockItem>> GT_STONE_ITEMS =
                    registerGtStoneItems();
    private static final Map<
            net.minecraft.resources.ResourceLocation,
            DeferredItem<BlockItem>> LAYER_STONE_ITEMS =
                    registerLayerStoneItems();
    private static final Map<
            net.minecraft.resources.ResourceLocation,
            DeferredItem<BlockItem>> GT_BLOCK_OBJECT_ITEMS =
                    registerGtBlockObjectItems();
    private static final Map<
            net.minecraft.resources.ResourceLocation,
            DeferredItem<BlockItem>> BATH_REMAINDER_BLOCK_OBJECT_ITEMS =
                    registerBathRemainderBlockObjectItems();
    public static final DeferredItem<BlockItem> STEEL_DUST_FUNNEL =
            ITEMS.registerSimpleBlockItem(
                    "steel_dust_funnel", ModBlocks.STEEL_DUST_FUNNEL);
    public static final DeferredItem<BlockItem> CENTRIFUGE =
            tieredProcessingItem("centrifuge");
    public static final DeferredItem<BlockItem> STEEL_CENTRIFUGE =
            tieredProcessingItem("steel_centrifuge");
    public static final DeferredItem<BlockItem> TITANIUM_CENTRIFUGE =
            tieredProcessingItem("titanium_centrifuge");
    public static final DeferredItem<BlockItem> SHREDDER =
            tieredProcessingItem("shredder");
    public static final DeferredItem<BlockItem> STEEL_SHREDDER =
            tieredProcessingItem("steel_shredder");
    public static final DeferredItem<BlockItem> TITANIUM_SHREDDER =
            tieredProcessingItem("titanium_shredder");
    public static final DeferredItem<BlockItem> SIFTER =
            tieredProcessingItem("sifter");
    public static final DeferredItem<BlockItem> STEEL_SIFTER =
            tieredProcessingItem("steel_sifter");
    public static final DeferredItem<BlockItem> TITANIUM_SIFTER =
            tieredProcessingItem("titanium_sifter");
    public static final DeferredItem<BlockItem> SMELTER =
            tieredProcessingItem("smelter");
    public static final DeferredItem<BlockItem> INVAR_SMELTER =
            tieredProcessingItem("invar_smelter");
    public static final DeferredItem<BlockItem> TITANIUM_SMELTER =
            tieredProcessingItem("titanium_smelter");
    public static final DeferredItem<BlockItem> MORTAR =
            tieredProcessingItem("mortar");
    public static final DeferredItem<BlockItem> EXTRUDER =
            tieredProcessingItem("extruder");
    public static final DeferredItem<BlockItem> INVAR_EXTRUDER =
            tieredProcessingItem("invar_extruder");
    public static final DeferredItem<BlockItem> CUTTER =
            tieredProcessingItem("cutter");
    public static final DeferredItem<BlockItem> LATHE =
            tieredProcessingItem("lathe");
    public static final DeferredItem<BlockItem> STEEL_LATHE =
            tieredProcessingItem("steel_lathe");
    public static final DeferredItem<BlockItem> TITANIUM_LATHE =
            tieredProcessingItem("titanium_lathe");
    public static final DeferredItem<BlockItem> ROLLINGMILL =
            tieredProcessingItem("rollingmill");
    public static final DeferredItem<BlockItem> STEEL_ROLLINGMILL =
            tieredProcessingItem("steel_rollingmill");
    public static final DeferredItem<BlockItem> TITANIUM_ROLLINGMILL =
            tieredProcessingItem("titanium_rollingmill");
    public static final DeferredItem<BlockItem> ROLLFORMER =
            tieredProcessingItem("rollformer");
    public static final DeferredItem<BlockItem> STEEL_ROLLFORMER =
            tieredProcessingItem("steel_rollformer");
    public static final DeferredItem<BlockItem> TITANIUM_ROLLFORMER =
            tieredProcessingItem("titanium_rollformer");
    public static final DeferredItem<BlockItem> SANDING =
            tieredProcessingItem("sanding");
    public static final DeferredItem<BlockItem> STEEL_SANDING =
            tieredProcessingItem("steel_sanding");
    public static final DeferredItem<BlockItem> TITANIUM_SANDING =
            tieredProcessingItem("titanium_sanding");
    public static final DeferredItem<BlockItem> OVEN =
            tieredProcessingItem("oven");
    public static final DeferredItem<BlockItem> INVAR_OVEN =
            tieredProcessingItem("invar_oven");
    public static final DeferredItem<BlockItem> TITANIUM_OVEN =
            tieredProcessingItem("titanium_oven");
    public static final DeferredItem<BlockItem> TUNGSTEN_CARBIDE_OVEN =
            tieredProcessingItem("tungsten_carbide_oven");
    public static final DeferredItem<BlockItem> CLUSTERMILL =
            tieredProcessingItem("clustermill");
    public static final DeferredItem<BlockItem> STEEL_CLUSTERMILL =
            tieredProcessingItem("steel_clustermill");
    public static final DeferredItem<BlockItem> TITANIUM_CLUSTERMILL =
            tieredProcessingItem("titanium_clustermill");
    public static final DeferredItem<BlockItem> SLICER =
            tieredProcessingItem("slicer");
    public static final DeferredItem<BlockItem> ALUMINIUM_SLICER =
            tieredProcessingItem("aluminium_slicer");
    public static final DeferredItem<BlockItem> STAINLESS_STEEL_SLICER =
            tieredProcessingItem("stainless_steel_slicer");
    public static final DeferredItem<BlockItem> ROLLBENDER =
            tieredProcessingItem("rollbender");
    public static final DeferredItem<BlockItem> WIREMILL =
            tieredProcessingItem("wiremill");
    public static final DeferredItem<BlockItem> STEEL_WIREMILL =
            tieredProcessingItem("steel_wiremill");
    public static final DeferredItem<BlockItem> TITANIUM_WIREMILL =
            tieredProcessingItem("titanium_wiremill");
    public static final DeferredItem<BlockItem> BENDER =
            tieredProcessingItem("bender");
    public static final DeferredItem<BlockItem> ASSEMBLER =
            tieredProcessingItem("assembler");
    public static final DeferredItem<BlockItem> WELDER =
            tieredProcessingItem("welder");
    public static final DeferredItem<BlockItem> PRESS =
            tieredProcessingItem("press");
    public static final DeferredItem<BlockItem> STEEL_PRESS =
            tieredProcessingItem("steel_press");
    public static final DeferredItem<BlockItem> TITANIUM_PRESS =
            tieredProcessingItem("titanium_press");
    public static final DeferredItem<BlockItem> ELECTROLYZER =
            tieredProcessingItem("electrolyzer");
    public static final DeferredItem<BlockItem> ALUMINIUM_ELECTROLYZER =
            tieredProcessingItem("aluminium_electrolyzer");
    public static final DeferredItem<BlockItem>
            STAINLESS_STEEL_ELECTROLYZER =
                    tieredProcessingItem("stainless_steel_electrolyzer");
    public static final DeferredItem<BlockItem> MIXER =
            tieredProcessingItem("mixer");
    public static final DeferredItem<BlockItem> DISTILLERY =
            tieredProcessingItem("distillery");
    public static final DeferredItem<BlockItem> INVAR_DISTILLERY =
            tieredProcessingItem("invar_distillery");
    public static final DeferredItem<BlockItem> TITANIUM_DISTILLERY =
            tieredProcessingItem("titanium_distillery");
    public static final DeferredItem<BlockItem> AUTOCLAVE =
            tieredProcessingItem("autoclave");
    public static final DeferredItem<BlockItem> DRYING =
            tieredProcessingItem("drying");
    public static final DeferredItem<BlockItem> INVAR_DRYING =
            tieredProcessingItem("invar_drying");
    public static final DeferredItem<BlockItem> TITANIUM_DRYING =
            tieredProcessingItem("titanium_drying");
    public static final DeferredItem<BlockItem> COMPRESSOR =
            tieredProcessingItem("compressor");
    public static final DeferredItem<BlockItem> GENERIFIER =
            tieredProcessingItem("generifier");
    public static final DeferredItem<BlockItem> COAGULATOR =
            tieredProcessingItem("coagulator");
    public static final DeferredItem<BlockItem> CANNER =
            tieredProcessingItem("canner");
    public static final DeferredItem<BlockItem> STEEL_ROASTER =
            tieredProcessingItem("steel_roaster");
    public static final DeferredItem<BlockItem> FLUID_DEPOSIT_EXTRACTOR =
            ITEMS.registerSimpleBlockItem(
                    "fluid_deposit_extractor",
                    ModBlocks.FLUID_DEPOSIT_EXTRACTOR);
    public static final DeferredItem<SmithingHammerItem> SMITHING_HAMMER =
            ITEMS.register(
                    "smithing_hammer",
                    () -> new SmithingHammerItem(new Item.Properties()));
    public static final DeferredItem<MaterialPickaxeItem> MATERIAL_PICKAXE =
            ITEMS.register(
                    "material_pickaxe",
                    () -> new MaterialPickaxeItem(new Item.Properties()));
    public static final DeferredItem<MaterialFileItem> MATERIAL_FILE =
            ITEMS.register(
                    "material_file",
                    () -> new MaterialFileItem(new Item.Properties()));
    public static final DeferredItem<MaterialShovelItem> MATERIAL_SHOVEL =
            ITEMS.register(
                    "material_shovel",
                    () -> new MaterialShovelItem(new Item.Properties()));
    public static final DeferredItem<MaterialAxeItem> MATERIAL_AXE =
            ITEMS.register(
                    "material_axe",
                    () -> new MaterialAxeItem(new Item.Properties()));
    public static final DeferredItem<MaterialHoeItem> MATERIAL_HOE =
            ITEMS.register(
                    "material_hoe",
                    () -> new MaterialHoeItem(new Item.Properties()));
    public static final DeferredItem<MaterialSwordItem> MATERIAL_SWORD =
            ITEMS.register(
                    "material_sword",
                    () -> new MaterialSwordItem(new Item.Properties()));
    public static final DeferredItem<MaterialChiselItem> MATERIAL_CHISEL =
            ITEMS.register(
                    "material_chisel",
                    () -> new MaterialChiselItem(new Item.Properties()));
    public static final DeferredItem<MaterialSawItem> MATERIAL_SAW =
            ITEMS.register(
                    "material_saw",
                    () -> new MaterialSawItem(new Item.Properties()));
    public static final DeferredItem<MaterialScrewdriverItem> MATERIAL_SCREWDRIVER =
            ITEMS.register(
                    "material_screwdriver",
                    () -> new MaterialScrewdriverItem(new Item.Properties()));
    public static final DeferredItem<MaterialWrenchItem> MATERIAL_WRENCH =
            ITEMS.register(
                    "material_wrench",
                    () -> new MaterialWrenchItem(new Item.Properties()));
    public static final DeferredItem<MaterialMonkeyWrenchItem> MATERIAL_MONKEY_WRENCH =
            ITEMS.register(
                    "material_monkey_wrench",
                    () -> new MaterialMonkeyWrenchItem(new Item.Properties()));
    public static final DeferredItem<MaterialWireCutterItem> MATERIAL_WIRE_CUTTER =
            ITEMS.register(
                    "material_wire_cutter",
                    () -> new MaterialWireCutterItem(new Item.Properties()));
    public static final DeferredItem<MaterialKnifeItem> MATERIAL_KNIFE =
            ITEMS.register(
                    "material_knife",
                    () -> new MaterialKnifeItem(new Item.Properties()));
    public static final DeferredItem<MaterialClubItem> MATERIAL_CLUB =
            ITEMS.register(
                    "material_club",
                    () -> new MaterialClubItem(new Item.Properties()));
    public static final DeferredItem<MaterialSpadeItem> MATERIAL_SPADE =
            ITEMS.register(
                    "material_spade",
                    () -> new MaterialSpadeItem(new Item.Properties()));
    public static final DeferredItem<MaterialDoubleAxeItem> MATERIAL_DOUBLE_AXE =
            ITEMS.register(
                    "material_double_axe",
                    () -> new MaterialDoubleAxeItem(new Item.Properties()));
    public static final DeferredItem<MaterialSenseItem> MATERIAL_SENSE =
            ITEMS.register(
                    "material_sense",
                    () -> new MaterialSenseItem(new Item.Properties()));
    public static final DeferredItem<MaterialPlowItem> MATERIAL_PLOW =
            ITEMS.register(
                    "material_plow",
                    () -> new MaterialPlowItem(new Item.Properties()));
    public static final DeferredItem<MaterialConstructionPickItem>
            MATERIAL_CONSTRUCTION_PICK =
            ITEMS.register(
                    "material_construction_pick",
                    () -> new MaterialConstructionPickItem(new Item.Properties()));
    public static final DeferredItem<MaterialGemPickItem> MATERIAL_GEM_PICK =
            ITEMS.register(
                    "material_gem_pick",
                    () -> new MaterialGemPickItem(new Item.Properties()));
    public static final DeferredItem<MaterialWorkshopToolItem> MATERIAL_BUILDER_WAND =
            ITEMS.register(
                    "material_builder_wand",
                    () -> new MaterialWorkshopToolItem(
                            new Item.Properties(),
                            ToolKind.BUILDER_WAND,
                            "item.cruciblecraft.material_builder_wand"));
    public static final DeferredItem<MaterialUniversalSpadeItem>
            MATERIAL_UNIVERSAL_SPADE =
            ITEMS.register(
                    "material_universal_spade",
                    () -> new MaterialUniversalSpadeItem(new Item.Properties()));
    public static final DeferredItem<MaterialCrowbarItem> MATERIAL_CROWBAR =
            ITEMS.register(
                    "material_crowbar",
                    () -> new MaterialCrowbarItem(new Item.Properties()));
    public static final DeferredItem<MaterialPlungerItem> MATERIAL_PLUNGER =
            ITEMS.register(
                    "material_plunger",
                    () -> new MaterialPlungerItem(new Item.Properties()));
    public static final DeferredItem<MaterialScoopItem> MATERIAL_SCOOP =
            ITEMS.register(
                    "material_scoop",
                    () -> new MaterialScoopItem(new Item.Properties()));
    public static final DeferredItem<MaterialButcheryKnifeItem>
            MATERIAL_BUTCHERY_KNIFE =
            ITEMS.register(
                    "material_butchery_knife",
                    () -> new MaterialButcheryKnifeItem(new Item.Properties()));
    public static final DeferredItem<MaterialBranchCutterItem>
            MATERIAL_BRANCH_CUTTER =
            ITEMS.register(
                    "material_branch_cutter",
                    () -> new MaterialBranchCutterItem(new Item.Properties()));
    public static final DeferredItem<MaterialScissorsItem> MATERIAL_SCISSORS =
            ITEMS.register(
                    "material_scissors",
                    () -> new MaterialScissorsItem(new Item.Properties()));
    public static final DeferredItem<MaterialWorkshopToolItem> MATERIAL_PINCERS =
            ITEMS.register(
                    "material_pincers",
                    () -> new MaterialWorkshopToolItem(
                            new Item.Properties(),
                            ToolKind.PINCERS,
                            "item.cruciblecraft.material_pincers"));
    public static final DeferredItem<MaterialSoftHammerItem> MATERIAL_SOFT_HAMMER =
            ITEMS.register(
                    "material_soft_hammer",
                    () -> new MaterialSoftHammerItem(new Item.Properties()));
    public static final DeferredItem<MaterialWorkshopToolItem>
            MATERIAL_BENDING_CYLINDER =
            ITEMS.register(
                    "material_bending_cylinder",
                    () -> new MaterialWorkshopToolItem(
                            new Item.Properties(),
                            ToolKind.BENDING_CYLINDER,
                            "item.cruciblecraft.material_bending_cylinder"));
    public static final DeferredItem<MaterialWorkshopToolItem>
            MATERIAL_BENDING_CYLINDER_SMALL =
            ITEMS.register(
                    "material_bending_cylinder_small",
                    () -> new MaterialWorkshopToolItem(
                            new Item.Properties(),
                            ToolKind.BENDING_CYLINDER_SMALL,
                            "item.cruciblecraft.material_bending_cylinder_small"));
    public static final DeferredItem<MaterialWorkshopToolItem> MATERIAL_HAND_DRILL =
            ITEMS.register(
                    "material_hand_drill",
                    () -> new MaterialWorkshopToolItem(
                            new Item.Properties(),
                            ToolKind.HAND_DRILL,
                            "item.cruciblecraft.material_hand_drill"));
    public static final DeferredItem<MaterialWorkshopToolItem> MATERIAL_ROLLING_PIN =
            ITEMS.register(
                    "material_rolling_pin",
                    () -> new MaterialWorkshopToolItem(
                            new Item.Properties(),
                            ToolKind.ROLLING_PIN,
                            "item.cruciblecraft.material_rolling_pin"));
    public static final DeferredItem<MaterialFlintAndTinderItem>
            MATERIAL_FLINT_AND_TINDER =
            ITEMS.register(
                    "material_flint_and_tinder",
                    () -> new MaterialFlintAndTinderItem(new Item.Properties()));
    public static final DeferredItem<MaterialWorkshopToolItem>
            MATERIAL_POCKET_MULTITOOL =
            ITEMS.register(
                    "material_pocket_multitool",
                    () -> new MaterialWorkshopToolItem(
                            new Item.Properties(),
                            ToolKind.POCKET_MULTITOOL,
                            "item.cruciblecraft.material_pocket_multitool"));
    public static final DeferredItem<FlintKnifeItem> FLINT_KNIFE =
            ITEMS.register(
                    "flint_knife",
                    () -> new FlintKnifeItem(new Item.Properties()));
    public static final DeferredItem<UnknownMaterialItem> UNKNOWN_MATERIAL =
            ITEMS.register("unknown_material", () -> new UnknownMaterialItem(new Item.Properties()));
    public static final Map<String, DeferredItem<Item>> EMPTY_TOOL_HEADS =
            registerEmptyToolHeads();
    public static void registerMaterials(Collection<MaterialDefinition> definitions) {
        if (!MATERIAL_ITEMS.isEmpty() || !MATERIAL_ORE_ITEMS.isEmpty()) {
            throw new IllegalStateException("Material items or ore items already registered");
        }
        for (MaterialDefinition material : definitions) {
            if (!MaterialCatalog.registeredForms(material).contains(MaterialPrefixes.ORE)) {
                continue;
            }
            for (Host host : Host.values()) {
                ModBlocks.OreBlockKey key = new ModBlocks.OreBlockKey(material.id(), host);
                String registryName = ModBlocks.oreRegistryName(material.id(), host);
                MATERIAL_ORE_ITEMS.put(
                        key,
                        registerOreItem(registryName, ModBlocks.oreBlock(material.id(), host)));
            }
        }
        for (MaterialDefinition material : definitions) {
            for (MaterialPrefix form : MaterialCatalog.registeredForms(material)) {
                if (form.equals(MaterialPrefixes.ORE)
                        || material.formItems().containsKey(form)) {
                    continue;
                }
                String registryName = material.registryName(form);
                DeferredItem<? extends Item> item;
                if (ModBlocks.hasElectricalConductorBlock(
                        material.id(), form)) {
                    item = ITEMS.register(
                            registryName,
                            () -> new CableBlockItem(
                                    ModBlocks.electricalConductorBlock(
                                            material.id(), form).get(),
                                    com.masson.cruciblecraft.energy.cable
                                            .ElectricalConductorCatalog
                                            .require(material.id(), form),
                                    new Item.Properties()));
                } else if (RedstoneWireKind.owns(material.id(), form)) {
                    RedstoneWireKind kind = RedstoneWireKind.byPath(
                            registryName).orElseThrow();
                    item = ITEMS.register(
                            registryName,
                            () -> new RedstoneWireBlockItem(
                                    ModBlocks.redstoneWireCatalogById()
                                            .get(kind.id())
                                            .get(),
                                    kind,
                                    new Item.Properties()));
                } else if (PipeCatalog.contains(
                        material.id(), form, PipeCatalog.Kind.FLUID)) {
                    item = pipeItem(
                            registryName,
                            material.id(),
                            form,
                            PipeCatalog.Kind.FLUID);
                } else if (PipeCatalog.contains(
                        material.id(), form, PipeCatalog.Kind.ITEM)) {
                    item = pipeItem(
                            registryName,
                            material.id(),
                            form,
                            PipeCatalog.Kind.ITEM);
                } else if (ModBlocks.hasStorageBlock(material.id())
                        && form.equals(MaterialPrefixes.BLOCK)) {
                    item = ITEMS.register(
                            registryName,
                            () -> new MaterialStorageBlockItem(
                                    ModBlocks.storageBlock(material.id()).get(),
                                    new Item.Properties()));
                } else if (ModBlocks.hasCasingBlock(material.id(), form)) {
                    item = ITEMS.register(
                            registryName,
                            () -> new MaterialCasingBlockItem(
                                    ModBlocks.casingBlock(material.id(), form)
                                            .get(),
                                    new Item.Properties()));
                } else if (ModBlocks.hasRockBlock(material.id())
                        && form.equals(com.masson.cruciblecraft.material.prefix
                                .MaterialPrefixCatalog.require("rock"))) {
                    item = ITEMS.register(
                            registryName,
                            () -> new PebbleBlockItem(
                                    ModBlocks.rockBlock(material.id()).get(),
                                    new Item.Properties()));
                } else {
                    item = ITEMS.register(
                            registryName,
                            () -> new MaterialItem(
                                    material,
                                    form,
                                    new Item.Properties()));
                }
                MATERIAL_ITEMS.put(key(material.id(), form), item);
            }
        }
        bindNamedCasingAliases();
    }

    private static DeferredItem<PipeBlockItem> pipeItem(
            String registryName,
            String materialId,
            MaterialPrefix form,
            PipeCatalog.Kind kind) {
        return ITEMS.register(
                registryName,
                () -> new PipeBlockItem(
                        ModBlocks.pipeBlock(
                                materialId, form, kind).get(),
                        PipeCatalog.require(materialId, form, kind),
                        new Item.Properties()));
    }

    public static DeferredItem<? extends Item> materialItem(
            String materialId, MaterialPrefix form) {
        DeferredItem<? extends Item> item =
                MATERIAL_ITEMS.get(key(materialId, form));
        if (item == null) {
            throw new IllegalArgumentException("No " + form.serializedName() + " for material " + materialId);
        }
        return item;
    }

    public static boolean hasMaterialItem(String materialId, MaterialPrefix form) {
        return MATERIAL_ITEMS.containsKey(key(materialId, form));
    }

    public static Collection<DeferredItem<? extends Item>> materialItems() {
        return MATERIAL_ITEMS.values();
    }

    public static DeferredItem<PipeCoverItem> compactElectricCover(
            String itemPath) {
        return COMPACT_ELECTRIC_COVERS.get(itemPath);
    }

    public static Collection<DeferredItem<PipeCoverItem>> compactElectricCovers() {
        return COMPACT_ELECTRIC_COVERS.values();
    }

    public static DeferredItem<PipeCoverItem> machineCover(
            String itemPath) {
        return MACHINE_COVERS.get(itemPath);
    }

    public static Collection<DeferredItem<PipeCoverItem>> machineCovers() {
        return MACHINE_COVERS.values();
    }

    public static DeferredItem<BlockItem> oreItem(String materialId, Host host) {
        DeferredItem<BlockItem> item =
                MATERIAL_ORE_ITEMS.get(new ModBlocks.OreBlockKey(materialId, host));
        if (item == null) {
            throw new IllegalArgumentException(
                    "No " + host.name().toLowerCase(java.util.Locale.ROOT)
                            + " ore item for material " + materialId);
        }
        return item;
    }

    public static boolean hasOreItem(String materialId, Host host) {
        return MATERIAL_ORE_ITEMS.containsKey(new ModBlocks.OreBlockKey(materialId, host));
    }

    public static Collection<DeferredItem<BlockItem>> oreItems() {
        return java.util.Collections.unmodifiableCollection(MATERIAL_ORE_ITEMS.values());
    }

    public static DeferredItem<Item> extruderShape(String id) {
        DeferredItem<Item> item = EXTRUDER_SHAPES.get(id);
        if (item == null) {
            throw new IllegalArgumentException("No extruder shape " + id);
        }
        return item;
    }

    public static Collection<DeferredItem<Item>> extruderShapes() {
        return EXTRUDER_SHAPES.values();
    }

    public static DeferredItem<Item> toolPattern(String id) {
        DeferredItem<Item> item = TOOL_PATTERNS.get(id);
        if (item == null) {
            throw new IllegalArgumentException("No tool pattern " + id);
        }
        return item;
    }

    public static Collection<DeferredItem<Item>> toolPatterns() {
        return TOOL_PATTERNS.values();
    }

    public static DeferredItem<Item> gtWood(String id) {
        DeferredItem<Item> item = GT_WOODS.get(id);
        if (item == null) {
            throw new IllegalArgumentException("No GT wood " + id);
        }
        return item;
    }

    public static Collection<DeferredItem<Item>> gtWoods() {
        return GT_WOODS.values();
    }

    public static DeferredItem<CeramicMoldBlockItem> moldItem(MoldShape shape) {
        return switch (shape) {
            case INGOT -> INGOT_MOLD;
            case PLATE -> PLATE_MOLD;
            case ROD -> ROD_MOLD;
            case BOLT -> BOLT_MOLD;
        };
    }

    private static DeferredItem<CeramicMoldBlockItem> mold(String id, MoldShape shape) {
        return ITEMS.register(
                id,
                () -> new CeramicMoldBlockItem(
                        ModBlocks.CERAMIC_MOLD.get(),
                        shape,
                        new Item.Properties().stacksTo(1)));
    }

    private static Map<String, DeferredItem<PipeCoverItem>>
            registerCompactElectricCovers() {
        LinkedHashMap<String, DeferredItem<PipeCoverItem>> items =
                new LinkedHashMap<>();
        for (CoverComponentTiers.Entry entry
                : CoverComponentTiers.entries()) {
            DeferredItem<PipeCoverItem> item = ITEMS.register(
                    entry.itemPath(),
                    () -> new PipeCoverItem(
                            entry.definitionId(),
                            new Item.Properties()));
            if (items.put(entry.itemPath(), item) != null) {
                throw new IllegalStateException(
                        "Duplicate compact electric cover "
                                + entry.itemPath());
            }
        }
        if (items.size() != CoverComponentTiers.entries().size()) {
            throw new IllegalStateException(
                    "Compact electric cover registration drifted");
        }
        return java.util.Collections.unmodifiableMap(items);
    }

    private static Map<String, DeferredItem<PipeCoverItem>>
            registerMachineCovers() {
        LinkedHashMap<String, DeferredItem<PipeCoverItem>> items =
                new LinkedHashMap<>();
        for (var entry : MachineCoverKinds.ITEMS) {
            DeferredItem<PipeCoverItem> item = ITEMS.register(
                    entry.itemPath(),
                    () -> new PipeCoverItem(
                            entry.definitionId(),
                            new Item.Properties()));
            if (items.put(entry.itemPath(), item) != null) {
                throw new IllegalStateException(
                        "Duplicate machine cover " + entry.itemPath());
            }
        }
        if (items.size() != MachineCoverKinds.ITEM_COUNT) {
            throw new IllegalStateException(
                    "Machine cover item registration drifted");
        }
        return java.util.Collections.unmodifiableMap(items);
    }

    private static Map<ResourceLocation, DeferredItem<BlockItem>>
            registerConverterItems() {
        LinkedHashMap<ResourceLocation, DeferredItem<BlockItem>> items =
                new LinkedHashMap<>();
        for (var profile : EnergyConverterCatalog.profiles()) {
            DeferredItem<BlockItem> item = ITEMS.registerSimpleBlockItem(
                    profile.id().getPath(),
                    ModBlocks.converterBlocksById().get(profile.id()));
            if (items.put(profile.id(), item) != null) {
                throw new IllegalStateException(
                        "Duplicate converter item " + profile.id());
            }
        }
        if (items.size() != EnergyConverterCatalog.profiles().size()) {
            throw new IllegalStateException(
                    "Converter item registration drifted from catalog rows");
        }
        return java.util.Collections.unmodifiableMap(items);
    }

    private static DeferredItem<BlockItem> converterItem(String path) {
        DeferredItem<BlockItem> item = CONVERTER_ITEMS.get(
                ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID, path));
        if (item == null) {
            throw new IllegalStateException(
                    "Missing catalog converter item " + path);
        }
        return item;
    }

    public static Map<ResourceLocation, DeferredItem<BlockItem>>
            converterItemsById() {
        return CONVERTER_ITEMS;
    }

    private static Map<ResourceLocation, DeferredItem<BatteryBlockItem>>
            registerBatteryItems() {
        LinkedHashMap<ResourceLocation, DeferredItem<BatteryBlockItem>> items =
                new LinkedHashMap<>();
        for (var profile : EnergyBatteryCatalog.profiles()) {
            DeferredItem<BatteryBlockItem> item = ITEMS.register(
                    profile.id().getPath(),
                    () -> new BatteryBlockItem(
                            ModBlocks.batteryBlocksById().get(profile.id()).get(),
                            new Item.Properties()));
            if (items.put(profile.id(), item) != null) {
                throw new IllegalStateException(
                        "Duplicate battery item " + profile.id());
            }
        }
        if (items.size() != EnergyBatteryCatalog.profiles().size()) {
            throw new IllegalStateException(
                    "Battery item registration drifted from catalog rows");
        }
        return java.util.Collections.unmodifiableMap(items);
    }

    private static Map<ResourceLocation, DeferredItem<Item>>
            registerTechnologicalParts() {
        LinkedHashMap<ResourceLocation, DeferredItem<Item>> items =
                new LinkedHashMap<>();
        for (TechnologicalPartCatalog.Part part : TechnologicalPartCatalog.parts()) {
            if (CoverComponentTiers.findByItemPath(part.registryPath()).isPresent()) {
                continue;
            }
            DeferredItem<Item> item = ITEMS.register(
                    part.registryPath(),
                    () -> new CatalogNamedItem(
                            new Item.Properties(),
                            part.englishName(),
                            part.chineseName()));
            if (items.put(part.id(), item) != null) {
                throw new IllegalStateException(
                        "Duplicate technological part " + part.id());
            }
        }
        return java.util.Collections.unmodifiableMap(items);
    }

    public static Map<ResourceLocation, DeferredItem<BatteryBlockItem>>
            batteryItemsById() {
        return BATTERY_ITEMS;
    }

    private static Map<String, DeferredItem<ReactorRodItem>>
            registerReactorRods() {
        LinkedHashMap<String, DeferredItem<ReactorRodItem>> items =
                new LinkedHashMap<>();
        for (ReactorRodCatalog.Entry entry : ReactorRodCatalog.entries()) {
            String path = entry.id().getPath();
            DeferredItem<ReactorRodItem> item = ITEMS.register(
                    path,
                    () -> new ReactorRodItem(entry, new Item.Properties()));
            if (items.put(path, item) != null) {
                throw new IllegalStateException("Duplicate reactor rod " + path);
            }
        }
        return java.util.Collections.unmodifiableMap(items);
    }

    private static Map<String, DeferredItem<BatteryCellItem>>
            registerBatteryCellItems() {
        LinkedHashMap<String, DeferredItem<BatteryCellItem>> items =
                new LinkedHashMap<>();
        for (BatteryCellSpec spec : BATTERY_CELL_SPECS) {
            String emptyPath = spec.family() + "_cell_empty";
            String filledPath = spec.family() + "_cell_filled";
            DeferredItem<BatteryCellItem> empty = ITEMS.register(
                    emptyPath,
                    () -> new BatteryCellItem(
                            new Item.Properties(),
                            false,
                            spec.fluidMaterial(),
                            spec.fluidAmount(),
                            () -> items.get(filledPath).get()));
            DeferredItem<BatteryCellItem> filled = ITEMS.register(
                    filledPath,
                    () -> new BatteryCellItem(
                            new Item.Properties(),
                            true,
                            spec.fluidMaterial(),
                            spec.fluidAmount(),
                            () -> items.get(emptyPath).get()));
            if (items.put(emptyPath, empty) != null
                    || items.put(filledPath, filled) != null) {
                throw new IllegalStateException(
                        "Duplicate battery cell item " + spec.family());
            }
        }
        return java.util.Collections.unmodifiableMap(items);
    }

    public static Map<String, DeferredItem<BatteryCellItem>>
            batteryCellItemsByPath() {
        return BATTERY_CELL_ITEMS;
    }

    public static DeferredItem<ReactorRodItem> reactorRod(String path) {
        DeferredItem<ReactorRodItem> item = REACTOR_RODS.get(path);
        if (item == null) {
            throw new IllegalArgumentException("Unknown reactor rod " + path);
        }
        return item;
    }

    public static Collection<DeferredItem<ReactorRodItem>> reactorRods() {
        return REACTOR_RODS.values();
    }

    public static Collection<DeferredItem<Item>> technologicalParts() {
        return TECHNOLOGICAL_PARTS.values();
    }

    public static DeferredItem<? extends Item> technologicalPart(String path) {
        DeferredItem<Item> item = TECHNOLOGICAL_PARTS.get(
                ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, path));
        if (item != null) {
            return item;
        }
        if (TechnologicalPartCatalog.findByPath(path).isPresent()) {
            DeferredItem<PipeCoverItem> cover = COMPACT_ELECTRIC_COVERS.get(path);
            if (cover != null) {
                return cover;
            }
        }
        throw new IllegalArgumentException("Unknown technological part " + path);
    }

    public static DeferredItem<BatteryCellItem> batteryCell(String path) {
        DeferredItem<BatteryCellItem> item = BATTERY_CELL_ITEMS.get(path);
        if (item == null) {
            throw new IllegalArgumentException("Unknown battery cell " + path);
        }
        return item;
    }

    private record BatteryCellSpec(
            String family,
            String fluidMaterial,
            int fluidAmount) {}

    private static Map<ResourceLocation, DeferredItem<TransformerBlockItem>>
            registerTransformerItems() {
        LinkedHashMap<ResourceLocation, DeferredItem<TransformerBlockItem>> items =
                new LinkedHashMap<>();
        for (var profile : EnergyTransformerCatalog.profiles()) {
            DeferredItem<TransformerBlockItem> item = ITEMS.register(
                    profile.id().getPath(),
                    () -> new TransformerBlockItem(
                            ModBlocks.transformerBlocksById()
                                    .get(profile.id())
                                    .get(),
                            new Item.Properties()));
            if (items.put(profile.id(), item) != null) {
                throw new IllegalStateException(
                        "Duplicate transformer item " + profile.id());
            }
        }
        if (items.size() != EnergyTransformerCatalog.profiles().size()) {
            throw new IllegalStateException(
                    "Transformer item registration drifted from catalog rows");
        }
        return java.util.Collections.unmodifiableMap(items);
    }

    public static Map<ResourceLocation, DeferredItem<TransformerBlockItem>>
            transformerItemsById() {
        return TRANSFORMER_ITEMS;
    }

    private static Map<ResourceLocation, DeferredItem<QuantumEnergizerBlockItem>>
            registerQuantumEnergizerItems() {
        LinkedHashMap<ResourceLocation, DeferredItem<QuantumEnergizerBlockItem>> items =
                new LinkedHashMap<>();
        for (var profile : QuantumEnergizerCatalog.profiles()) {
            DeferredItem<QuantumEnergizerBlockItem> item = ITEMS.register(
                    profile.id().getPath(),
                    () -> new QuantumEnergizerBlockItem(
                            ModBlocks.quantumEnergizerBlocksById()
                                    .get(profile.id())
                                    .get(),
                            new Item.Properties()));
            if (items.put(profile.id(), item) != null) {
                throw new IllegalStateException(
                        "Duplicate quantum energizer item " + profile.id());
            }
        }
        return java.util.Collections.unmodifiableMap(items);
    }

    public static Map<ResourceLocation, DeferredItem<QuantumEnergizerBlockItem>>
            quantumEnergizerItemsById() {
        return QUANTUM_ENERGIZER_ITEMS;
    }

    private static Map<ResourceLocation, DeferredItem<LongDistanceTransformerBlockItem>>
            registerLongDistanceTransformerItems() {
        LinkedHashMap<ResourceLocation, DeferredItem<LongDistanceTransformerBlockItem>>
                items = new LinkedHashMap<>();
        for (var profile : LongDistanceTransformerCatalog.endpoints()) {
            DeferredItem<LongDistanceTransformerBlockItem> item = ITEMS.register(
                    profile.id().getPath(),
                    () -> new LongDistanceTransformerBlockItem(
                            ModBlocks.longDistanceTransformerBlocksById()
                                    .get(profile.id())
                                    .get(),
                            new Item.Properties()));
            if (items.put(profile.id(), item) != null) {
                throw new IllegalStateException(
                        "Duplicate long-distance transformer item " + profile.id());
            }
        }
        return java.util.Collections.unmodifiableMap(items);
    }

    public static Map<ResourceLocation, DeferredItem<LongDistanceTransformerBlockItem>>
            longDistanceTransformerItemsById() {
        return LONG_DISTANCE_TRANSFORMER_ITEMS;
    }

    private static Map<ResourceLocation, DeferredItem<BlockItem>>
            registerLongDistanceWireItems() {
        LinkedHashMap<ResourceLocation, DeferredItem<BlockItem>> items =
                new LinkedHashMap<>();
        for (var profile : LongDistanceTransformerCatalog.wires()) {
            DeferredItem<BlockItem> item = ITEMS.register(
                    profile.id().getPath(),
                    () -> new CatalogNamedBlockItem(
                            ModBlocks.longDistanceWireBlocksById()
                                    .get(profile.id())
                                    .get(),
                            new Item.Properties(),
                            profile.langEn(),
                            profile.langZh()));
            if (items.put(profile.id(), item) != null) {
                throw new IllegalStateException(
                        "Duplicate long-distance wire item " + profile.id());
            }
        }
        return java.util.Collections.unmodifiableMap(items);
    }

    public static Map<ResourceLocation, DeferredItem<BlockItem>>
            longDistanceWireItemsById() {
        return LONG_DISTANCE_WIRE_ITEMS;
    }

    private static Map<ResourceLocation, DeferredItem<HeatExchangerBlockItem>>
            registerHeatExchangerItems() {
        LinkedHashMap<ResourceLocation, DeferredItem<HeatExchangerBlockItem>>
                items = new LinkedHashMap<>();
        for (var profile : HeatExchangerCatalog.profiles()) {
            DeferredItem<HeatExchangerBlockItem> item = ITEMS.register(
                    profile.id().getPath(),
                    () -> new HeatExchangerBlockItem(
                            ModBlocks.heatExchangerBlocksById()
                                    .get(profile.id())
                                    .get(),
                            new Item.Properties()));
            if (items.put(profile.id(), item) != null) {
                throw new IllegalStateException(
                        "Duplicate heat exchanger item " + profile.id());
            }
        }
        if (items.size() != HeatExchangerCatalog.profiles().size()) {
            throw new IllegalStateException(
                    "Heat exchanger item registration drifted from catalog rows");
        }
        return java.util.Collections.unmodifiableMap(items);
    }

    public static Map<ResourceLocation, DeferredItem<HeatExchangerBlockItem>>
            heatExchangerItemsById() {
        return HEAT_EXCHANGER_ITEMS;
    }

    private static Map<ResourceLocation, DeferredItem<BlockItem>>
            registerTieredProcessingItems() {
        LinkedHashMap<ResourceLocation, DeferredItem<BlockItem>> items =
                new LinkedHashMap<>();
        for (MachineVariant variant : ModMachineVariants.ALL) {
            DeferredItem<BlockItem> item = ITEMS.registerSimpleBlockItem(
                    variant.id().getPath(),
                    ModBlocks.tieredProcessingBlocksById().get(variant.id()));
            if (items.put(variant.id(), item) != null) {
                throw new IllegalStateException(
                        "Duplicate tiered processing item " + variant.id());
            }
        }
        if (items.size() != ModMachineVariants.ALL.size()) {
            throw new IllegalStateException(
                    "Tiered processing item registration drifted from catalog rows");
        }
        return java.util.Collections.unmodifiableMap(items);
    }

    private static Map<ResourceLocation, DeferredItem<HopperBlockItem>>
            registerHopperItems() {
        LinkedHashMap<ResourceLocation, DeferredItem<HopperBlockItem>> items =
                new LinkedHashMap<>();
        for (HopperVariant variant : HopperVariantCatalog.variants()) {
            DeferredItem<HopperBlockItem> item = ITEMS.register(
                    variant.id().getPath(),
                    () -> new HopperBlockItem(
                            ModBlocks.hopperBlocksById()
                                    .get(variant.id())
                                    .get(),
                            new Item.Properties()));
            if (items.put(variant.id(), item) != null) {
                throw new IllegalStateException(
                        "Duplicate hopper item " + variant.id());
            }
        }
        if (items.size() != 120) {
            throw new IllegalStateException(
                    "Hopper item registration drifted from 120 variants");
        }
        return java.util.Collections.unmodifiableMap(items);
    }

    public static Map<ResourceLocation, DeferredItem<HopperBlockItem>>
            hopperItemsById() {
        return HOPPER_ITEMS;
    }

    private static Map<ResourceLocation, DeferredItem<CatalogNamedBlockItem>>
            registerMteInPlaceItems() {
        LinkedHashMap<ResourceLocation, DeferredItem<CatalogNamedBlockItem>> items =
                new LinkedHashMap<>();
        for (MteInPlaceSpec spec : MteInPlaceCatalog.specs()) {
            DeferredItem<CatalogNamedBlockItem> item = ITEMS.register(
                    spec.registryPath(),
                    () -> new CatalogNamedBlockItem(
                            ModBlocks.mteInPlaceBlocksById().get(spec.id()).get(),
                            new Item.Properties(),
                            spec.englishName(),
                            spec.chineseName()));
            if (items.put(spec.id(), item) != null) {
                throw new IllegalStateException(
                        "Duplicate in-place MTE item " + spec.id());
            }
        }
        return java.util.Collections.unmodifiableMap(items);
    }

    public static Map<ResourceLocation, DeferredItem<CatalogNamedBlockItem>>
            mteInPlaceItemsById() {
        return MTE_INPLACE_ITEMS;
    }

    private static Map<ResourceLocation, DeferredItem<BlockItem>>
            registerSensorItems() {
        LinkedHashMap<ResourceLocation, DeferredItem<BlockItem>> items =
                new LinkedHashMap<>();
        for (SensorKind kind : SensorKind.all()) {
            DeferredItem<BlockItem> item = ITEMS.registerSimpleBlockItem(
                    kind.path(),
                    ModBlocks.sensorBlocksById().get(kind.id()));
            if (items.put(kind.id(), item) != null) {
                throw new IllegalStateException(
                        "Duplicate sensor item " + kind.id());
            }
        }
        if (items.size() != SensorKind.EXPECTED_SIZE) {
            throw new IllegalStateException(
                    "Sensor item registration drifted from 21 GT6 identities");
        }
        return java.util.Collections.unmodifiableMap(items);
    }

    public static Map<ResourceLocation, DeferredItem<BlockItem>>
            sensorItemsById() {
        return SENSOR_ITEMS;
    }

    private static Map<ResourceLocation, DeferredItem<BlockItem>>
            registerStorageItems() {
        LinkedHashMap<ResourceLocation, DeferredItem<BlockItem>> items =
                new LinkedHashMap<>();
        for (com.masson.cruciblecraft.content.storage.StorageVariant variant
                : com.masson.cruciblecraft.content.storage.StorageVariantCatalog.variants()) {
            DeferredItem<BlockItem> item = ITEMS.registerSimpleBlockItem(
                    variant.path(),
                    ModBlocks.storageBlocksById().get(variant.id()));
            if (items.put(variant.id(), item) != null) {
                throw new IllegalStateException(
                        "Duplicate storage item " + variant.id());
            }
        }
        if (items.size() != com.masson.cruciblecraft.content.storage
                .StorageVariantCatalog.TOTAL_COUNT) {
            throw new IllegalStateException(
                    "Storage item registration drifted from catalog");
        }
        return java.util.Collections.unmodifiableMap(items);
    }

    public static Map<ResourceLocation, DeferredItem<BlockItem>>
            storageItemsById() {
        return STORAGE_ITEMS;
    }

    private static Map<ResourceLocation, DeferredItem<BlockItem>>
            registerGtStoneItems() {
        LinkedHashMap<ResourceLocation, DeferredItem<BlockItem>> items =
                new LinkedHashMap<>();
        for (GtStoneCatalog.Variant variant : GtStoneCatalog.variants()) {
            DeferredItem<BlockItem> item = ITEMS.registerSimpleBlockItem(
                    variant.registryPath(),
                    ModBlocks.gtStoneBlocksById().get(variant.id()));
            if (items.put(variant.id(), item) != null) {
                throw new IllegalStateException(
                        "Duplicate GT stone item " + variant.id());
            }
        }
        if (items.size() != GtStoneCatalog.VARIANT_COUNT) {
            throw new IllegalStateException(
                    "GT stone item registration drifted from "
                            + GtStoneCatalog.VARIANT_COUNT
                            + " variants");
        }
        return java.util.Collections.unmodifiableMap(items);
    }

    public static Map<ResourceLocation, DeferredItem<BlockItem>>
            gtStoneItemsById() {
        return GT_STONE_ITEMS;
    }

    public static Collection<DeferredItem<BlockItem>> gtStoneItems() {
        return GT_STONE_ITEMS.values();
    }

    private static Map<ResourceLocation, DeferredItem<BlockItem>>
            registerLayerStoneItems() {
        LinkedHashMap<ResourceLocation, DeferredItem<BlockItem>> items =
                new LinkedHashMap<>();
        int reused = 0;
        for (StoneLayerStones.Cube cube : StoneLayerStones.registeredCubes()) {
            if (GT_STONE_ITEMS.containsKey(cube.id())) {
                reused++;
                continue;
            }
            DeferredItem<BlockItem> item = ITEMS.register(
                    cube.registryPath(),
                    () -> new CatalogNamedBlockItem(
                            ModBlocks.layerStone(cube.registryPath()).get(),
                            new Item.Properties(),
                            cube.english(),
                            cube.chinese()));
            if (items.put(cube.id(), item) != null) {
                throw new IllegalStateException(
                        "Duplicate stone-layer cube item " + cube.id());
            }
        }
        if (items.size() + reused != StoneLayerStones.registeredCubes().size()) {
            throw new IllegalStateException(
                    "stone-layer cube item registration drifted");
        }
        return java.util.Collections.unmodifiableMap(items);
    }

    public static Map<ResourceLocation, DeferredItem<BlockItem>>
            layerStoneItemsById() {
        return LAYER_STONE_ITEMS;
    }

    public static Collection<DeferredItem<BlockItem>> layerStoneItems() {
        return LAYER_STONE_ITEMS.values();
    }

    private static Map<ResourceLocation, DeferredItem<BlockItem>>
            registerGtBlockObjectItems() {
        LinkedHashMap<ResourceLocation, DeferredItem<BlockItem>> items =
                new LinkedHashMap<>();
        for (GtBlockObjectCatalog.Variant variant : GtBlockObjectCatalog.variants()) {
            DeferredItem<BlockItem> item = ITEMS.register(
                    variant.registryPath(),
                    () -> new CatalogNamedBlockItem(
                            ModBlocks.gtBlockObjectBlocksById().get(variant.id()).get(),
                            new Item.Properties(),
                            variant.englishName(),
                            variant.chineseName()));
            if (items.put(variant.id(), item) != null) {
                throw new IllegalStateException(
                        "Duplicate GT block-object item " + variant.id());
            }
        }
        if (items.size() != GtBlockObjectCatalog.VARIANT_COUNT) {
            throw new IllegalStateException(
                    "GT block-object item registration drifted from "
                            + GtBlockObjectCatalog.VARIANT_COUNT
                            + " variants");
        }
        return java.util.Collections.unmodifiableMap(items);
    }

    private static Map<ResourceLocation, DeferredItem<BlockItem>>
            registerBathRemainderBlockObjectItems() {
        LinkedHashMap<ResourceLocation, DeferredItem<BlockItem>> items =
                new LinkedHashMap<>();
        for (GtBlockObjectCatalog.Variant variant : BathRemainderBlockObjectCatalog.variants()) {
            DeferredItem<BlockItem> item = ITEMS.register(
                    variant.registryPath(),
                    () -> new CatalogNamedBlockItem(
                            ModBlocks.bathRemainderBlockObjectBlocksById().get(variant.id()).get(),
                            new Item.Properties(),
                            variant.englishName(),
                            variant.chineseName()));
            if (items.put(variant.id(), item) != null) {
                throw new IllegalStateException(
                        "Duplicate bath remainder block-object item " + variant.id());
            }
        }
        if (items.size() != BathRemainderBlockObjectCatalog.VARIANT_COUNT) {
            throw new IllegalStateException(
                    "Bath remainder block-object item registration drifted from "
                            + BathRemainderBlockObjectCatalog.VARIANT_COUNT
                            + " variants");
        }
        return java.util.Collections.unmodifiableMap(items);
    }

    public static Map<ResourceLocation, DeferredItem<BlockItem>>
            gtBlockObjectItemsById() {
        return GT_BLOCK_OBJECT_ITEMS;
    }

    public static Collection<DeferredItem<BlockItem>> gtBlockObjectItems() {
        return GT_BLOCK_OBJECT_ITEMS.values();
    }

    public static Map<ResourceLocation, DeferredItem<BlockItem>>
            bathRemainderBlockObjectItemsById() {
        return BATH_REMAINDER_BLOCK_OBJECT_ITEMS;
    }

    public static Collection<DeferredItem<BlockItem>> bathRemainderBlockObjectItems() {
        return BATH_REMAINDER_BLOCK_OBJECT_ITEMS.values();
    }

    private static DeferredItem<BlockItem> tieredProcessingItem(String path) {
        DeferredItem<BlockItem> item = TIERED_PROCESSING_ITEMS.get(
                ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID, path));
        if (item == null) {
            throw new IllegalStateException(
                    "Missing catalog processing item " + path);
        }
        return item;
    }

    public static Map<ResourceLocation, DeferredItem<BlockItem>>
            tieredProcessingItemsById() {
        return TIERED_PROCESSING_ITEMS;
    }

    public static Map<ResourceLocation, DeferredItem<Item>> bathMteItemsById() {
        return BATH_MTE_ITEMS;
    }

    public static Map<ResourceLocation, DeferredItem<Item>> smelterMteItemsById() {
        return SMELTER_MTE_ITEMS;
    }

    public static Map<ResourceLocation, DeferredItem<Item>> bathIdentityItemsById() {
        return BATH_IDENTITY_ITEMS;
    }

    public static Map<ResourceLocation, DeferredItem<Item>> semanticIdentityItemsById() {
        return SEMANTIC_IDENTITY_ITEMS;
    }

    public static Map<ResourceLocation, DeferredItem<Item>> slicerOperandItemsById() {
        return SLICER_OPERAND_ITEMS;
    }

    public static Map<ResourceLocation, DeferredItem<Item>>
            pressureWasherOperandItemsById() {
        return PRESSURE_WASHER_OPERAND_ITEMS;
    }

    private static Map<String, DeferredItem<Item>> registerEmptyToolHeads() {
        LinkedHashMap<String, DeferredItem<Item>> items = new LinkedHashMap<>();
        for (String path : List.of(
                "empty/tool_head_chainsaw",
                "empty/tool_head_drill",
                "empty/tool_head_pickaxe_gem",
                "empty/tool_head_wrench")) {
            items.put(path, ITEMS.registerSimpleItem(path, new Item.Properties()));
        }
        return java.util.Collections.unmodifiableMap(items);
    }

    private static void bindNamedCasingAliases() {
        BRONZE_DOUBLE_MACHINE_CASING = materialItem(
                "bronze", MaterialPrefixes.MACHINE_CASING_DOUBLE);
        STEEL_DOUBLE_MACHINE_CASING = materialItem(
                "steel", MaterialPrefixes.MACHINE_CASING_DOUBLE);
        TITANIUM_DOUBLE_MACHINE_CASING = materialItem(
                "titanium", MaterialPrefixes.MACHINE_CASING_DOUBLE);
        STEEL_GALVANIZED_MACHINE_CASING = materialItem(
                "steel_galvanized", MaterialPrefixes.MACHINE_CASING);
        ALUMINIUM_MACHINE_CASING = materialItem(
                "aluminium", MaterialPrefixes.MACHINE_CASING);
        STAINLESS_STEEL_MACHINE_CASING = materialItem(
                "stainless_steel", MaterialPrefixes.MACHINE_CASING);
        CHROMIUM_MACHINE_CASING = materialItem(
                "chromium", MaterialPrefixes.MACHINE_CASING);
        TITANIUM_MACHINE_CASING = materialItem(
                "titanium", MaterialPrefixes.MACHINE_CASING);
        TUNGSTENSTEEL_DOUBLE_MACHINE_CASING = materialItem(
                "tungstensteel", MaterialPrefixes.MACHINE_CASING_DOUBLE);
        INVAR_DOUBLE_MACHINE_CASING = materialItem(
                "invar", MaterialPrefixes.MACHINE_CASING_DOUBLE);
        TUNGSTEN_CARBIDE_DOUBLE_MACHINE_CASING = materialItem(
                "tungsten_carbide", MaterialPrefixes.MACHINE_CASING_DOUBLE);
    }

    private static Map<ResourceLocation, DeferredItem<Item>> registerBathMteItems() {
        LinkedHashMap<ResourceLocation, DeferredItem<Item>> items =
                new LinkedHashMap<>();
        for (BathMteIdentityCatalog.Identity identity : BathMteIdentityCatalog.newItems()) {
            DeferredItem<Item> item = ITEMS.register(
                    identity.registryPath(),
                    () -> new CatalogNamedItem(
                            new Item.Properties(),
                            identity.englishName(),
                            identity.chineseName()));
            if (items.put(identity.id(), item) != null) {
                throw new IllegalStateException(
                        "Duplicate Bath MTE item " + identity.id());
            }
        }
        return java.util.Collections.unmodifiableMap(items);
    }

    private static Map<ResourceLocation, DeferredItem<Item>> registerSmelterMteItems() {
        LinkedHashMap<ResourceLocation, DeferredItem<Item>> items =
                new LinkedHashMap<>();
        for (SmelterMteIdentityCatalog.Identity identity : SmelterMteIdentityCatalog.newItems()) {
            DeferredItem<Item> item = ITEMS.register(
                    identity.registryPath(),
                    () -> new CatalogNamedItem(
                            new Item.Properties(),
                            identity.englishName(),
                            identity.chineseName()));
            if (items.put(identity.id(), item) != null) {
                throw new IllegalStateException(
                        "Duplicate Smelter MTE item " + identity.id());
            }
        }
        return java.util.Collections.unmodifiableMap(items);
    }

    private static Map<ResourceLocation, DeferredItem<Item>> registerBathIdentityItems() {
        LinkedHashMap<ResourceLocation, DeferredItem<Item>> items =
                new LinkedHashMap<>();
        for (BathIdentityCatalog.Identity identity : BathIdentityCatalog.identities()) {
            DeferredItem<Item> item = ITEMS.register(
                    identity.registryPath(),
                    () -> new CatalogNamedItem(
                            new Item.Properties(),
                            identity.englishName(),
                            identity.chineseName()));
            if (items.put(identity.id(), item) != null) {
                throw new IllegalStateException(
                        "Duplicate bath identity item " + identity.id());
            }
        }
        if (items.size() != BathIdentityCatalog.VARIANT_COUNT) {
            throw new IllegalStateException(
                    "Bath identity item registration drifted from "
                            + BathIdentityCatalog.VARIANT_COUNT);
        }
        return java.util.Collections.unmodifiableMap(items);
    }

    private static Map<ResourceLocation, DeferredItem<Item>> registerSemanticIdentityItems() {
        LinkedHashMap<ResourceLocation, DeferredItem<Item>> items =
                new LinkedHashMap<>();
        for (SemanticObjectCatalog.Identity identity : SemanticObjectCatalog.identities()) {
            DeferredItem<Item> item = ITEMS.register(
                    identity.registryPath(),
                    () -> createSemanticIdentityItem(identity));
            if (items.put(identity.id(), item) != null) {
                throw new IllegalStateException(
                        "Duplicate semantic identity item " + identity.id());
            }
        }
        if (items.size() != SemanticObjectCatalog.VARIANT_COUNT) {
            throw new IllegalStateException(
                    "Semantic identity item registration drifted from "
                            + SemanticObjectCatalog.VARIANT_COUNT);
        }
        return java.util.Collections.unmodifiableMap(items);
    }

    private static Map<ResourceLocation, DeferredItem<Item>> registerSlicerOperandItems() {
        LinkedHashMap<ResourceLocation, DeferredItem<Item>> items = new LinkedHashMap<>();
        for (SlicerOperandCatalog.Operand operand : SlicerOperandCatalog.operands()) {
            if (SEMANTIC_IDENTITY_ITEMS.containsKey(operand.id())) {
                throw new IllegalStateException(
                        "Slicer operand already registered as semantic identity: "
                                + operand.id());
            }
            DeferredItem<Item> item = ITEMS.register(
                    operand.registryPath(),
                    () -> new CatalogNamedItem(
                            new Item.Properties(),
                            operand.englishName(),
                            operand.chineseName()));
            if (items.put(operand.id(), item) != null) {
                throw new IllegalStateException(
                        "Duplicate slicer operand item " + operand.id());
            }
        }
        return java.util.Collections.unmodifiableMap(items);
    }

    private static Map<ResourceLocation, DeferredItem<Item>>
            registerPressureWasherOperandItems() {
        LinkedHashMap<ResourceLocation, DeferredItem<Item>> items =
                new LinkedHashMap<>();
        for (PressureWasherOperandCatalog.Operand operand
                : PressureWasherOperandCatalog.operands()) {
            DeferredItem<Item> item = ITEMS.register(
                    operand.registryPath(),
                    () -> new CatalogNamedItem(
                            new Item.Properties(),
                            operand.englishName(),
                            operand.chineseName()));
            if (items.put(operand.id(), item) != null) {
                throw new IllegalStateException(
                        "Duplicate pressure washer operand item " + operand.id());
            }
        }
        return java.util.Collections.unmodifiableMap(items);
    }

    private static Item createSemanticIdentityItem(
            SemanticObjectCatalog.Identity identity) {
        Item.Properties properties = new Item.Properties();
        if (HazmatArmorItem.isHazmatPath(identity.registryPath())) {
            return HazmatArmorItem.fromIdentity(identity, properties);
        }
        if (ThermometerItem.REGISTRY_PATH.equals(identity.registryPath())) {
            return new ThermometerItem(
                    properties, identity.englishName(), identity.chineseName());
        }
        if (ElectroMeterItem.REGISTRY_PATH.equals(identity.registryPath())) {
            return new ElectroMeterItem(
                    properties, identity.englishName(), identity.chineseName());
        }
        if (TachoMeterItem.REGISTRY_PATH.equals(identity.registryPath())) {
            return new TachoMeterItem(
                    properties, identity.englishName(), identity.chineseName());
        }
        if (GeigerCounterItem.EMPTY_PATH.equals(identity.registryPath())) {
            return new GeigerCounterItem(
                    properties,
                    false,
                    identity.englishName(),
                    identity.chineseName());
        }
        if (GeigerCounterItem.FILLED_PATH.equals(identity.registryPath())) {
            return new GeigerCounterItem(
                    properties,
                    true,
                    identity.englishName(),
                    identity.chineseName());
        }
        return new CatalogNamedItem(
                properties, identity.englishName(), identity.chineseName());
    }

    private static DeferredItem<BlockItem> registerOreItem(
            String id,
            net.neoforged.neoforge.registries.DeferredBlock<? extends net.minecraft.world.level.block.Block> block) {
        return ITEMS.registerSimpleBlockItem(id, block);
    }

    private static String key(String materialId, MaterialPrefix form) {
        return materialId + "/" + form.serializedName();
    }

    private static void registerGtCrops() {
        for (com.masson.cruciblecraft.worldgen.crop.GlowtusColor color :
                com.masson.cruciblecraft.worldgen.crop.GlowtusColor.ALL) {
            GLOWTUS_ITEMS.put(
                    color,
                    ITEMS.register(
                            color.blockPath(),
                            () -> new net.minecraft.world.item.PlaceOnWaterBlockItem(
                                    ModBlocks.glowtus(color).get(),
                                    new Item.Properties())));
        }
    }

    public static DeferredItem<BlockItem> glowtusItem(
            com.masson.cruciblecraft.worldgen.crop.GlowtusColor color) {
        return GLOWTUS_ITEMS.get(color);
    }

    public static java.util.Collection<DeferredItem<BlockItem>> glowtusItems() {
        return GLOWTUS_ITEMS.values();
    }

    private static void registerGtTrees() {
        for (GtTreeSpecies species : GtTreeSpecies.ALL) {
            TREE_SAPLING_ITEMS.put(
                    species,
                    ITEMS.registerSimpleBlockItem(
                            species.saplingPath(), ModBlocks.treeSapling(species)));
            TREE_LOG_ITEMS.put(
                    species,
                    ITEMS.registerSimpleBlockItem(
                            species.logPath(), ModBlocks.treeLog(species)));
            TREE_LEAVES_ITEMS.put(
                    species,
                    ITEMS.registerSimpleBlockItem(
                            species.leavesPath(), ModBlocks.treeLeaves(species)));
        }
    }

    public static DeferredItem<BlockItem> treeSaplingItem(GtTreeSpecies species) {
        return TREE_SAPLING_ITEMS.get(species);
    }

    public static DeferredItem<BlockItem> treeLogItem(GtTreeSpecies species) {
        return TREE_LOG_ITEMS.get(species);
    }

    public static DeferredItem<BlockItem> treeLeavesItem(GtTreeSpecies species) {
        return TREE_LEAVES_ITEMS.get(species);
    }

    private ModItems() {}
}
