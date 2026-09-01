package com.masson.cruciblecraft.registry;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.item.MaterialItem;
import com.masson.cruciblecraft.content.item.MaterialStorageBlockItem;
import com.masson.cruciblecraft.content.item.CableBlockItem;
import com.masson.cruciblecraft.content.item.CellItem;
import com.masson.cruciblecraft.content.item.CeramicMoldBlockItem;
import com.masson.cruciblecraft.content.item.ExtruderShapeCatalog;
import com.masson.cruciblecraft.content.item.GtWoodCatalog;
import com.masson.cruciblecraft.content.item.BathMteIdentityCatalog;
import com.masson.cruciblecraft.content.item.SmelterMteIdentityCatalog;
import com.masson.cruciblecraft.content.item.GtBlockObjectCatalog;
import com.masson.cruciblecraft.content.item.BathRemainderBlockObjectCatalog;
import com.masson.cruciblecraft.content.item.BathIdentityCatalog;
import com.masson.cruciblecraft.content.item.SemanticObjectCatalog;
import com.masson.cruciblecraft.content.item.GtStoneCatalog;
import com.masson.cruciblecraft.content.item.FlintKnifeItem;
import com.masson.cruciblecraft.content.item.HopperBlockItem;
import com.masson.cruciblecraft.content.storage.StorageVariant;
import com.masson.cruciblecraft.content.storage.StorageVariantCatalog;
import com.masson.cruciblecraft.content.item.MaterialAxeItem;
import com.masson.cruciblecraft.content.item.MaterialChiselItem;
import com.masson.cruciblecraft.content.item.MaterialFileItem;
import com.masson.cruciblecraft.content.item.MaterialHoeItem;
import com.masson.cruciblecraft.content.item.MaterialMachineBlockItem;
import com.masson.cruciblecraft.content.item.MaterialPickaxeItem;
import com.masson.cruciblecraft.content.item.MaterialSawItem;
import com.masson.cruciblecraft.content.item.MaterialScrewdriverItem;
import com.masson.cruciblecraft.content.item.MaterialShovelItem;
import com.masson.cruciblecraft.content.item.MaterialSwordItem;
import com.masson.cruciblecraft.content.item.MaterialWireCutterItem;
import com.masson.cruciblecraft.content.item.MaterialWrenchItem;
import com.masson.cruciblecraft.content.item.PortableFluidTankItem;
import com.masson.cruciblecraft.content.item.ProgrammedCircuitItem;
import com.masson.cruciblecraft.content.item.PipeBlockItem;
import com.masson.cruciblecraft.content.item.PipeCoverItem;
import com.masson.cruciblecraft.content.item.SmithingHammerItem;
import com.masson.cruciblecraft.content.item.ToolPatternCatalog;
import com.masson.cruciblecraft.content.item.UnknownMaterialItem;
import com.masson.cruciblecraft.machine.MachineMaterialRules.Device;
import com.masson.cruciblecraft.content.mold.MoldShape;
import com.masson.cruciblecraft.machine.processing.MachineCasingCatalog;
import com.masson.cruciblecraft.machine.processing.MachineVariant;
import com.masson.cruciblecraft.logistics.hopper.HopperVariant;
import com.masson.cruciblecraft.logistics.hopper.HopperVariantCatalog;
import com.masson.cruciblecraft.material.CellContentGate;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.logistics.pipe.PipeCatalog;
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
    public static final DeferredItem<BlockItem> FIREBOX = ITEMS.registerSimpleBlockItem("firebox", ModBlocks.FIREBOX);
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
    public static final DeferredItem<Item> COAL_COKE =
            ITEMS.registerSimpleItem("coal_coke", new Item.Properties());
    public static final DeferredItem<Item> MATCH =
            ITEMS.registerSimpleItem("match", new Item.Properties());
    public static final DeferredItem<ProgrammedCircuitItem> PROGRAMMED_CIRCUIT =
            ITEMS.register(
                    "programmed_circuit",
                    () -> new ProgrammedCircuitItem(new Item.Properties()));
    private static final Map<ResourceLocation, DeferredItem<Item>> MACHINE_CASINGS =
            registerMachineCasings();
    private static final Map<ResourceLocation, DeferredItem<Item>> BATH_MTE_ITEMS =
            registerBathMteItems();
    private static final Map<ResourceLocation, DeferredItem<Item>> SMELTER_MTE_ITEMS =
            registerSmelterMteItems();
    private static final Map<ResourceLocation, DeferredItem<Item>> BATH_IDENTITY_ITEMS =
            registerBathIdentityItems();
    private static final Map<ResourceLocation, DeferredItem<Item>> SEMANTIC_IDENTITY_ITEMS =
            registerSemanticIdentityItems();
    public static final DeferredItem<Item>
            BRONZE_DOUBLE_MACHINE_CASING =
                    machineCasing("bronze_double_machine_casing");
    public static final DeferredItem<Item>
            STEEL_DOUBLE_MACHINE_CASING =
                    machineCasing("steel_double_machine_casing");
    public static final DeferredItem<Item>
            TITANIUM_DOUBLE_MACHINE_CASING =
                    machineCasing("titanium_double_machine_casing");
    public static final DeferredItem<Item>
            STEEL_GALVANIZED_MACHINE_CASING =
                    machineCasing("steel_galvanized_machine_casing");
    public static final DeferredItem<Item>
            ALUMINIUM_MACHINE_CASING =
                    machineCasing("aluminium_machine_casing");
    public static final DeferredItem<Item>
            STAINLESS_STEEL_MACHINE_CASING =
                    machineCasing("stainless_steel_machine_casing");
    public static final DeferredItem<Item>
            CHROMIUM_MACHINE_CASING =
                    machineCasing("chromium_machine_casing");
    public static final DeferredItem<Item>
            TITANIUM_MACHINE_CASING =
                    machineCasing("titanium_machine_casing");
    public static final DeferredItem<Item>
            TUNGSTENSTEEL_DOUBLE_MACHINE_CASING =
                    machineCasing("tungstensteel_double_machine_casing");
    public static final DeferredItem<Item>
            INVAR_DOUBLE_MACHINE_CASING =
                    machineCasing("invar_double_machine_casing");
    public static final DeferredItem<Item>
            TUNGSTEN_CARBIDE_DOUBLE_MACHINE_CASING =
                    machineCasing("tungsten_carbide_double_machine_casing");
    public static final DeferredItem<BlockItem> BELLOWS =
            ITEMS.registerSimpleBlockItem("bellows", ModBlocks.BELLOWS);
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
    public static final DeferredItem<BlockItem> BRONZE_BOILER =
            ITEMS.registerSimpleBlockItem("bronze_boiler", ModBlocks.BRONZE_BOILER);
    public static final DeferredItem<BlockItem> BRONZE_STEAM_ENGINE =
            ITEMS.registerSimpleBlockItem("bronze_steam_engine", ModBlocks.BRONZE_STEAM_ENGINE);
    public static final DeferredItem<BlockItem> BRONZE_DYNAMO =
            ITEMS.registerSimpleBlockItem("bronze_dynamo", ModBlocks.BRONZE_DYNAMO);
    public static final DeferredItem<BlockItem> ELECTRIC_MOTOR =
            ITEMS.registerSimpleBlockItem(
                    "electric_motor", ModBlocks.ELECTRIC_MOTOR);
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
            DeferredItem<BlockItem>> STORAGE_ITEMS =
                    registerStorageItems();
    private static final Map<
            net.minecraft.resources.ResourceLocation,
            DeferredItem<BlockItem>> GT_STONE_ITEMS =
                    registerGtStoneItems();
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
    public static final DeferredItem<BlockItem> STEEL_ROASTER =
            tieredProcessingItem("steel_roaster");
    public static final DeferredItem<BlockItem> FLUID_DEPOSIT_EXTRACTOR =
            ITEMS.registerSimpleBlockItem(
                    "fluid_deposit_extractor",
                    ModBlocks.FLUID_DEPOSIT_EXTRACTOR);
    public static final DeferredItem<BlockItem> FUEL_ENGINE =
            ITEMS.registerSimpleBlockItem(
                    "fuel_engine", ModBlocks.FUEL_ENGINE);
    public static final DeferredItem<BlockItem> BURNING_GAS_GENERATOR =
            ITEMS.registerSimpleBlockItem(
                    "burning_gas_generator",
                    ModBlocks.BURNING_GAS_GENERATOR);
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
    public static final DeferredItem<MaterialWireCutterItem> MATERIAL_WIRE_CUTTER =
            ITEMS.register(
                    "material_wire_cutter",
                    () -> new MaterialWireCutterItem(new Item.Properties()));
    public static final DeferredItem<FlintKnifeItem> FLINT_KNIFE =
            ITEMS.register(
                    "flint_knife",
                    () -> new FlintKnifeItem(new Item.Properties()));
    public static final DeferredItem<UnknownMaterialItem> UNKNOWN_MATERIAL =
            ITEMS.register("unknown_material", () -> new UnknownMaterialItem(new Item.Properties()));
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
                } else if (ModBlocks.hasRockBlock(material.id())
                        && form.equals(com.masson.cruciblecraft.material.prefix
                                .MaterialPrefixCatalog.require("rock"))) {
                    item = ITEMS.register(
                            registryName,
                            () -> new net.minecraft.world.item.BlockItem(
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
            registerGtBlockObjectItems() {
        LinkedHashMap<ResourceLocation, DeferredItem<BlockItem>> items =
                new LinkedHashMap<>();
        for (GtBlockObjectCatalog.Variant variant : GtBlockObjectCatalog.variants()) {
            DeferredItem<BlockItem> item = ITEMS.registerSimpleBlockItem(
                    variant.registryPath(),
                    ModBlocks.gtBlockObjectBlocksById().get(variant.id()));
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
            DeferredItem<BlockItem> item = ITEMS.registerSimpleBlockItem(
                    variant.registryPath(),
                    ModBlocks.bathRemainderBlockObjectBlocksById().get(variant.id()));
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

    public static Map<ResourceLocation, DeferredItem<Item>> machineCasingsById() {
        return MACHINE_CASINGS;
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

    private static Map<ResourceLocation, DeferredItem<Item>> registerMachineCasings() {
        LinkedHashMap<ResourceLocation, DeferredItem<Item>> items =
                new LinkedHashMap<>();
        for (MachineCasingCatalog.Casing casing : MachineCasingCatalog.casings()) {
            DeferredItem<Item> item = ITEMS.registerSimpleItem(
                    casing.id().getPath(), new Item.Properties());
            if (items.put(casing.id(), item) != null) {
                throw new IllegalStateException(
                        "Duplicate machine casing " + casing.id());
            }
        }
        return java.util.Collections.unmodifiableMap(items);
    }

    private static Map<ResourceLocation, DeferredItem<Item>> registerBathMteItems() {
        LinkedHashMap<ResourceLocation, DeferredItem<Item>> items =
                new LinkedHashMap<>();
        for (BathMteIdentityCatalog.Identity identity : BathMteIdentityCatalog.newItems()) {
            DeferredItem<Item> item = ITEMS.registerSimpleItem(
                    identity.registryPath(), new Item.Properties());
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
            DeferredItem<Item> item = ITEMS.registerSimpleItem(
                    identity.registryPath(), new Item.Properties());
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
            DeferredItem<Item> item = ITEMS.registerSimpleItem(
                    identity.registryPath(), new Item.Properties());
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
            DeferredItem<Item> item = ITEMS.registerSimpleItem(
                    identity.registryPath(), new Item.Properties());
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

    private static DeferredItem<Item> machineCasing(String path) {
        DeferredItem<Item> item = MACHINE_CASINGS.get(
                ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID, path));
        if (item == null) {
            throw new IllegalStateException("Missing machine casing " + path);
        }
        return item;
    }

    private static DeferredItem<BlockItem> registerOreItem(
            String id,
            net.neoforged.neoforge.registries.DeferredBlock<? extends net.minecraft.world.level.block.Block> block) {
        return ITEMS.registerSimpleBlockItem(id, block);
    }

    private static String key(String materialId, MaterialPrefix form) {
        return materialId + "/" + form.serializedName();
    }

    private ModItems() {}
}
