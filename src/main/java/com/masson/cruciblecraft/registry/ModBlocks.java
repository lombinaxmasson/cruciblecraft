package com.masson.cruciblecraft.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.AnvilBlock;
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
import com.masson.cruciblecraft.content.block.LargeCentrifugeBlock;
import com.masson.cruciblecraft.content.block.MultiblockPortBlock;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
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
            () -> new SteamEngineBlock(machineProperties()));
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
    public static final DeferredBlock<ProcessingMachineBlock> SLUICE =
            processing("sluice", ModProcessingMachines.SLUICE);
    public static final DeferredBlock<ProcessingMachineBlock> BATH =
            processing("bath", ModProcessingMachines.BATH);
    public static final DeferredBlock<ProcessingMachineBlock> CENTRIFUGE =
            processing(
                    "centrifuge",
                    ModMachineVariants.require(
                            ResourceLocation.fromNamespaceAndPath(
                                    CrucibleCraft.MODID, "centrifuge")));
    public static final DeferredBlock<ProcessingMachineBlock>
            STEEL_CENTRIFUGE = processing(
                    "steel_centrifuge",
                    ModMachineVariants.require(
                            ResourceLocation.fromNamespaceAndPath(
                                    CrucibleCraft.MODID,
                                    "steel_centrifuge")));
    public static final DeferredBlock<ProcessingMachineBlock>
            TITANIUM_CENTRIFUGE = processing(
                    "titanium_centrifuge",
                    ModMachineVariants.require(
                            ResourceLocation.fromNamespaceAndPath(
                                    CrucibleCraft.MODID,
                                    "titanium_centrifuge")));
    public static final DeferredBlock<ProcessingMachineBlock> SHREDDER =
            processing("shredder", machineVariant("shredder"));
    public static final DeferredBlock<ProcessingMachineBlock> STEEL_SHREDDER =
            processing("steel_shredder", machineVariant("steel_shredder"));
    public static final DeferredBlock<ProcessingMachineBlock>
            TITANIUM_SHREDDER = processing(
                    "titanium_shredder",
                    machineVariant("titanium_shredder"));
    public static final DeferredBlock<ProcessingMachineBlock> SIFTER =
            processing(
                    "sifter",
                    ModMachineVariants.require(
                            ResourceLocation.fromNamespaceAndPath(
                                    CrucibleCraft.MODID, "sifter")));
    public static final DeferredBlock<ProcessingMachineBlock>
            STEEL_SIFTER = processing(
                    "steel_sifter",
                    ModMachineVariants.require(
                            ResourceLocation.fromNamespaceAndPath(
                                    CrucibleCraft.MODID, "steel_sifter")));
    public static final DeferredBlock<ProcessingMachineBlock>
            TITANIUM_SIFTER = processing(
                    "titanium_sifter",
                    ModMachineVariants.require(
                            ResourceLocation.fromNamespaceAndPath(
                                    CrucibleCraft.MODID,
                                    "titanium_sifter")));
    public static final DeferredBlock<ProcessingMachineBlock> SMELTER =
            processing("smelter", machineVariant("smelter"));
    public static final DeferredBlock<ProcessingMachineBlock> INVAR_SMELTER =
            processing("invar_smelter", machineVariant("invar_smelter"));
    public static final DeferredBlock<ProcessingMachineBlock>
            TITANIUM_SMELTER = processing(
                    "titanium_smelter",
                    machineVariant("titanium_smelter"));
    public static final DeferredBlock<ProcessingMachineBlock> MORTAR =
            processing("mortar", ModProcessingMachines.MORTAR);
    public static final DeferredBlock<ProcessingMachineBlock> EXTRUDER =
            processing("extruder", ModProcessingMachines.EXTRUDER);
    public static final DeferredBlock<ProcessingMachineBlock> CUTTER =
            processing("cutter", ModProcessingMachines.CUTTER);
    public static final DeferredBlock<ProcessingMachineBlock> LATHE =
            processing("lathe", machineVariant("lathe"));
    public static final DeferredBlock<ProcessingMachineBlock> STEEL_LATHE =
            processing("steel_lathe", machineVariant("steel_lathe"));
    public static final DeferredBlock<ProcessingMachineBlock> TITANIUM_LATHE =
            processing("titanium_lathe", machineVariant("titanium_lathe"));
    public static final DeferredBlock<ProcessingMachineBlock> ROLLINGMILL =
            processing("rollingmill", machineVariant("rollingmill"));
    public static final DeferredBlock<ProcessingMachineBlock>
            STEEL_ROLLINGMILL = processing(
                    "steel_rollingmill",
                    machineVariant("steel_rollingmill"));
    public static final DeferredBlock<ProcessingMachineBlock>
            TITANIUM_ROLLINGMILL = processing(
                    "titanium_rollingmill",
                    machineVariant("titanium_rollingmill"));
    public static final DeferredBlock<ProcessingMachineBlock> ROLLBENDER =
            processing("rollbender", ModProcessingMachines.ROLLBENDER);
    public static final DeferredBlock<ProcessingMachineBlock> WIREMILL =
            processing("wiremill", machineVariant("wiremill"));
    public static final DeferredBlock<ProcessingMachineBlock> STEEL_WIREMILL =
            processing("steel_wiremill", machineVariant("steel_wiremill"));
    public static final DeferredBlock<ProcessingMachineBlock>
            TITANIUM_WIREMILL = processing(
                    "titanium_wiremill",
                    machineVariant("titanium_wiremill"));
    public static final DeferredBlock<ProcessingMachineBlock> BENDER =
            processing("bender", ModProcessingMachines.BENDER);
    public static final DeferredBlock<ProcessingMachineBlock> ASSEMBLER =
            processing("assembler", ModProcessingMachines.ASSEMBLER);
    public static final DeferredBlock<ProcessingMachineBlock> WELDER =
            processing("welder", ModProcessingMachines.WELDER);
    public static final DeferredBlock<ProcessingMachineBlock> PRESS =
            processing("press", machineVariant("press"));
    public static final DeferredBlock<ProcessingMachineBlock> STEEL_PRESS =
            processing("steel_press", machineVariant("steel_press"));
    public static final DeferredBlock<ProcessingMachineBlock> TITANIUM_PRESS =
            processing("titanium_press", machineVariant("titanium_press"));
    public static final DeferredBlock<ProcessingMachineBlock> ELECTROLYZER =
            processing(
                    "electrolyzer",
                    ModMachineVariants.require(
                            ResourceLocation.fromNamespaceAndPath(
                                    CrucibleCraft.MODID, "electrolyzer")));
    public static final DeferredBlock<ProcessingMachineBlock>
            ALUMINIUM_ELECTROLYZER = processing(
                    "aluminium_electrolyzer",
                    ModMachineVariants.require(
                            ResourceLocation.fromNamespaceAndPath(
                                    CrucibleCraft.MODID,
                                    "aluminium_electrolyzer")));
    public static final DeferredBlock<ProcessingMachineBlock>
            STAINLESS_STEEL_ELECTROLYZER = processing(
                    "stainless_steel_electrolyzer",
                    ModMachineVariants.require(
                            ResourceLocation.fromNamespaceAndPath(
                                    CrucibleCraft.MODID,
                                    "stainless_steel_electrolyzer")));
    public static final DeferredBlock<ProcessingMachineBlock> MIXER =
            processing("mixer", ModProcessingMachines.MIXER);
    public static final DeferredBlock<ProcessingMachineBlock> DISTILLERY =
            processing("distillery", machineVariant("distillery"));
    public static final DeferredBlock<ProcessingMachineBlock>
            INVAR_DISTILLERY = processing(
                    "invar_distillery",
                    machineVariant("invar_distillery"));
    public static final DeferredBlock<ProcessingMachineBlock>
            TITANIUM_DISTILLERY = processing(
                    "titanium_distillery",
                    machineVariant("titanium_distillery"));
    public static final DeferredBlock<ProcessingMachineBlock> AUTOCLAVE =
            processing("autoclave", ModProcessingMachines.AUTOCLAVE);
    public static final DeferredBlock<ProcessingMachineBlock> DRYING =
            processing("drying", machineVariant("drying"));
    public static final DeferredBlock<ProcessingMachineBlock> INVAR_DRYING =
            processing("invar_drying", machineVariant("invar_drying"));
    public static final DeferredBlock<ProcessingMachineBlock>
            TITANIUM_DRYING = processing(
                    "titanium_drying",
                    machineVariant("titanium_drying"));
    public static final DeferredBlock<ProcessingMachineBlock> COMPRESSOR =
            processing("compressor", ModProcessingMachines.COMPRESSOR);
    public static final DeferredBlock<ProcessingMachineBlock> GENERIFIER =
            processing("generifier", ModProcessingMachines.GENERIFIER);
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

    private static DeferredBlock<ProcessingMachineBlock> processing(
            String id,
            com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec spec) {
        DeferredBlock<ProcessingMachineBlock> block =
                BLOCKS.register(id, () -> new ProcessingMachineBlock(spec, machineProperties()));
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
                                variant, machineProperties()));
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

    private static MachineVariant machineVariant(String path) {
        return ModMachineVariants.require(
                ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID, path));
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
        if (!CONFIGURED_PROCESSING_BLOCKS.keySet().containsAll(
                ModProcessingMachines.CONFIGURED_MACHINES.stream()
                        .map(com.masson.cruciblecraft.machine.processing
                                .ProcessingMachineSpec::id)
                        .collect(java.util.stream.Collectors.toSet()))) {
            throw new IllegalStateException(
                    "Configured processing block mapping is incomplete");
        }
        boolean configured = ModProcessingMachines.CONFIGURED_MACHINES.stream()
                .anyMatch(candidate -> candidate == spec);
        DeferredBlock<ProcessingMachineBlock> holder =
                CONFIGURED_PROCESSING_BLOCKS.get(spec.id());
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

    private static BlockBehaviour.Properties conductorProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(0.5F, 2.0F)
                .requiresCorrectToolForDrops()
                .noOcclusion()
                .sound(SoundType.METAL);
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
