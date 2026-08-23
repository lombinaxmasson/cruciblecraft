package com.masson.cruciblecraft.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.AnvilBlock;
import com.masson.cruciblecraft.content.block.RockBlock;
import com.masson.cruciblecraft.content.block.AbstractPipeBlock;
import com.masson.cruciblecraft.content.block.BellowsBlock;
import com.masson.cruciblecraft.content.block.BoilerBlock;
import com.masson.cruciblecraft.content.block.CokeOvenBlock;
import com.masson.cruciblecraft.content.block.CeramicMoldBlock;
import com.masson.cruciblecraft.content.block.CableBlock;
import com.masson.cruciblecraft.content.block.CrucibleBlock;
import com.masson.cruciblecraft.content.block.CrusherBlock;
import com.masson.cruciblecraft.content.block.DynamoBlock;
import com.masson.cruciblecraft.content.block.ElectricMotorBlock;
import com.masson.cruciblecraft.content.block.FireboxBlock;
import com.masson.cruciblecraft.content.block.FluidPipeBlock;
import com.masson.cruciblecraft.content.block.FluidDepositExtractorBlock;
import com.masson.cruciblecraft.content.block.FuelGeneratorBlock;
import com.masson.cruciblecraft.content.block.GasCloudBlock;
import com.masson.cruciblecraft.content.block.ItemPipeBlock;
import com.masson.cruciblecraft.content.block.MaterialStorageBlock;
import com.masson.cruciblecraft.content.block.LargeCentrifugeBlock;
import com.masson.cruciblecraft.content.block.DistillationTowerBlock;
import com.masson.cruciblecraft.content.block.LargeBoilerBlock;
import com.masson.cruciblecraft.content.block.LargeCrucibleBlock;
import com.masson.cruciblecraft.content.block.TankBlock;
import com.masson.cruciblecraft.content.block.MultiblockPortBlock;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.block.HopperBlock;
import com.masson.cruciblecraft.content.block.DustFunnelBlock;
import com.masson.cruciblecraft.content.block.RotationalAxleBlock;
import com.masson.cruciblecraft.content.block.RotationalGearboxBlock;
import com.masson.cruciblecraft.content.block.SteamEngineBlock;
import com.masson.cruciblecraft.content.block.SubsurfaceFluidDepositBlock;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PortType;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.energy.cable.ElectricalConductorCatalog;
import com.masson.cruciblecraft.logistics.pipe.PipeCatalog;
import com.masson.cruciblecraft.machine.processing.MachineVariant;
import com.masson.cruciblecraft.logistics.hopper.HopperVariant;
import com.masson.cruciblecraft.logistics.hopper.HopperVariantCatalog;
import com.masson.cruciblecraft.worldgen.OreHostVariantCatalog.Host;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.LiquidBlock;
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

    public static final DeferredBlock<FireboxBlock> FIREBOX = BLOCKS.register(
            "firebox",
            () -> new FireboxBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(3.0F, 8.0F)
                    .lightLevel(state -> state.getValue(FireboxBlock.LIT) ? 13 : 0)
                    .sound(SoundType.STONE)));

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

    public static final DeferredBlock<BellowsBlock> BELLOWS = BLOCKS.register(
            "bellows",
            () -> new BellowsBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(1.5F)
                    .sound(SoundType.WOOD)));

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

    public static final DeferredBlock<BoilerBlock> BRONZE_BOILER = BLOCKS.register(
            "bronze_boiler",
            () -> new BoilerBlock(machineProperties()));
    public static final DeferredBlock<SteamEngineBlock> BRONZE_STEAM_ENGINE = BLOCKS.register(
            "bronze_steam_engine",
            () -> new SteamEngineBlock(machineProperties().noOcclusion()));
    public static final DeferredBlock<DynamoBlock> BRONZE_DYNAMO = BLOCKS.register(
            "bronze_dynamo",
            () -> new DynamoBlock(machineProperties()));
    public static final DeferredBlock<ElectricMotorBlock> ELECTRIC_MOTOR =
            BLOCKS.register(
                    "electric_motor",
                    () -> new ElectricMotorBlock(machineProperties()));
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
    public static final DeferredBlock<ProcessingMachineBlock> STEEL_ROASTER =
            tieredProcessing("steel_roaster");
    public static final DeferredBlock<FuelGeneratorBlock> FUEL_ENGINE =
            BLOCKS.register(
                    "fuel_engine",
                    () -> new FuelGeneratorBlock(
                            ModFuelGenerators.FUEL_ENGINE,
                            machineProperties()));
    public static final DeferredBlock<FuelGeneratorBlock>
            BURNING_GAS_GENERATOR = BLOCKS.register(
                    "burning_gas_generator",
                    () -> new FuelGeneratorBlock(
                            ModFuelGenerators.BURNING_GAS_GENERATOR,
                            machineProperties()));

    public static void registerMaterials(Collection<MaterialDefinition> definitions) {
        if (!MATERIAL_ORE_BLOCKS.isEmpty()
                || !MATERIAL_STORAGE_BLOCKS.isEmpty()
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
        return ELECTRICAL_CONDUCTOR_BLOCKS.values().stream()
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

    public static Block[] hopperBlockArray() {
        return HOPPER_BLOCKS.values().stream()
                .map(DeferredBlock::get)
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
                        CONFIGURED_PROCESSING_BLOCKS.containsKey(machine.id())
                                || !ModMachineVariants.forKind(machine.id())
                                        .isEmpty());
        if (!complete) {
            throw new IllegalStateException(
                    "Configured processing block mapping is incomplete");
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

    private ModBlocks() {}
}
