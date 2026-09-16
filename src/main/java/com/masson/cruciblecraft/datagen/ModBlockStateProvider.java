package com.masson.cruciblecraft.datagen;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.block.AbstractPipeBlock;
import com.masson.cruciblecraft.content.block.CableBlock;
import com.masson.cruciblecraft.content.block.CeramicMoldBlock;
import com.masson.cruciblecraft.content.block.HopperBlock;
import com.masson.cruciblecraft.content.block.SensorBlock;
import com.masson.cruciblecraft.content.sensor.SensorKind;
import com.masson.cruciblecraft.content.block.LogisticsCoreBlock;
import com.masson.cruciblecraft.content.item.GtBlockObjectCatalog;
import com.masson.cruciblecraft.content.item.GtStoneCatalog;
import com.masson.cruciblecraft.worldgen.StoneLayerStones;
import com.masson.cruciblecraft.content.storage.StorageVariantCatalog;
import com.masson.cruciblecraft.logistics.hopper.HopperKind;
import com.masson.cruciblecraft.logistics.hopper.HopperVariantCatalog;
import com.masson.cruciblecraft.machine.processing.MachineTextureProfiles;
import com.masson.cruciblecraft.content.block.GtTreeHoleBlock;
import com.masson.cruciblecraft.worldgen.tree.prep.GtTreeSpecies;
import com.masson.cruciblecraft.energy.bedrockdrill.BedrockDrillBlock;
import com.masson.cruciblecraft.energy.cable.ElectricalConductorCatalog;
import com.masson.cruciblecraft.logistics.pipe.PipeCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModMachineVariants;

import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModBlockStateProvider extends BlockStateProvider {
    private static final java.util.Set<String> HANDWRITTEN_CONVERTER_MODELS =
            java.util.Set.of(
                    "bronze_boiler",
                    "bronze_steam_engine",
                    "bronze_dynamo",
                    "fuel_engine",
                    "burning_gas_generator");
    public ModBlockStateProvider(
            PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, CrucibleCraft.MODID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        registerGtTrees();
        var firebrick =
                models().getExistingFile(modLoc("block/firebrick_gt6"));
        simpleBlockWithItem(
                ModBlocks.FIREBRICK.get(),
                firebrick);
        simpleBlockWithItem(
                ModBlocks.CRUCIBLE.get(),
                models().getExistingFile(modLoc("block/crucible")));
        var anvil = models().getExistingFile(modLoc("block/anvil"));
        horizontalBlock(ModBlocks.ANVIL.get(), anvil);
        simpleBlockItem(ModBlocks.ANVIL.get(), anvil);
        configuredMachine("coke_oven", ModBlocks.COKE_OVEN.get());
        simpleBlockWithItem(
                ModBlocks.MULTIBLOCK_CASING.get(),
                models().cubeAll(
                        "multiblock_casing",
                        modLoc("block/multiblock_casing")));
        simpleBlockWithItem(
                ModBlocks.MULTIBLOCK_ITEM_FLUID_PORT.get(),
                models().cubeAll(
                        "multiblock_item_fluid_port",
                        modLoc("block/multiblock_item_fluid_port")));
        simpleBlockWithItem(
                ModBlocks.MULTIBLOCK_ENERGY_INPUT_PORT.get(),
                models().cubeAll(
                        "multiblock_energy_input_port",
                        modLoc("block/multiblock_energy_input_port")));
        configuredMachine(
                "large_centrifuge", ModBlocks.LARGE_CENTRIFUGE.get());
        configuredMachine(
                "distillation_tower", ModBlocks.DISTILLATION_TOWER.get());
        configuredMachine(
                "large_boiler", ModBlocks.LARGE_BOILER.get());
        configuredMachine(
                "tank_3x3x3", ModBlocks.TANK_3X3X3.get());
        configuredMachine(
                "large_crucible", ModBlocks.LARGE_CRUCIBLE.get());
        configuredLogisticsCore();
        simpleBlockWithItem(
                ModBlocks.GALVANIZED_STEEL_WALL.get(),
                logisticsPartModel("galvanized_steel_wall"));
        simpleBlockWithItem(
                ModBlocks.TUNGSTENSTEEL_WALL.get(),
                logisticsPartModel("tungstensteel_wall"));
        simpleBlockWithItem(
                ModBlocks.STAINLESS_STEEL_WALL.get(),
                logisticsPartModel("stainless_steel_wall"));
        simpleBlockWithItem(
                ModBlocks.LARGE_IRIDIUM_COIL.get(),
                logisticsPartModel("large_iridium_coil"));
        simpleBlockWithItem(
                ModBlocks.VENTILATION_UNIT.get(),
                logisticsPartModel("ventilation_unit"));
        simpleBlockWithItem(
                ModBlocks.VERSATILE_PROCESSOR_UNIT.get(),
                logisticsPartModel("versatile_processor_unit"));
        simpleBlockWithItem(
                ModBlocks.LOGIC_PROCESSOR_UNIT.get(),
                logisticsPartModel("logic_processor_unit"));
        simpleBlockWithItem(
                ModBlocks.CONTROL_PROCESSOR_UNIT.get(),
                logisticsPartModel("control_processor_unit"));
        simpleBlockWithItem(
                ModBlocks.STORAGE_PROCESSOR_UNIT.get(),
                logisticsPartModel("storage_processor_unit"));
        simpleBlockWithItem(
                ModBlocks.CONVERSION_PROCESSOR_UNIT.get(),
                logisticsPartModel("conversion_processor_unit"));
        ModMachineVariants.ALL.forEach(variant ->
                configuredMachine(
                        variant.id().getPath(),
                        ModBlocks.configuredProcessingBlock(variant)));
        configuredMachine(
                "rotational_gearbox",
                ModBlocks.ROTATIONAL_GEARBOX.get());
        registerConverters();
        registerBatteries();
        registerTransformers();
        registerHeatExchangers();
        registerLargeHeatExchanger();
        registerBedrockDrill();
        registerQuantumEnergizers();
        registerLongDistanceTransformers();
        simpleBlockWithItem(
                ModBlocks.ROTATIONAL_AXLE.get(),
                models().cubeAll(
                        "rotational_axle",
                        modLoc("block/rotational_axle")));
        simpleBlockWithItem(
                ModBlocks.FLUID_DEPOSIT_EXTRACTOR.get(),
                models().cubeAll(
                        "fluid_deposit_extractor",
                        modLoc("block/fluid_deposit_extractor")));
        simpleBlock(
                ModBlocks.GAS_CLOUD.get(),
                models()
                        .cubeAll("gas_cloud", modLoc("block/gas_cloud"))
                        .renderType("translucent"));
        var emptyMold = models().getExistingFile(modLoc("block/ceramic_mold"));
        var filledMold = models().getExistingFile(modLoc("block/ceramic_mold_filled"));
        getVariantBuilder(ModBlocks.CERAMIC_MOLD.get()).forAllStates(state ->
                ConfiguredModel.builder()
                        .modelFile(state.getValue(CeramicMoldBlock.FILLED) ? filledMold : emptyMold)
                        .build());
        simpleBlockItem(ModBlocks.CERAMIC_MOLD.get(), emptyMold);
        registerConductors();
        registerLuFiberCable();
        configuredMachine("laser_engraver", ModBlocks.LASER_ENGRAVER.get());
        configuredMachine("fusion_reactor", ModBlocks.FUSION_REACTOR.get());
        configuredFacingLitMachine(
                "reactor_core_1x1", ModBlocks.REACTOR_CORE_1X1.get());
        configuredFacingLitMachine(
                "reactor_core_2x2", ModBlocks.REACTOR_CORE_2X2.get());
        registerPipes();
        registerHoppers();
        registerSensors();
        registerStorage();
        registerGtStones();
        registerLayerStones();
        registerGtBlockObjects();
    }

    private void registerLayerStones() {
        for (StoneLayerStones.Cube cube : StoneLayerStones.registeredCubes()) {
            if (!ModBlocks.hasLayerStone(cube.registryPath())) {
                continue;
            }
            var block = ModBlocks.layerStone(cube.registryPath()).get();
            ResourceLocation texture = ResourceLocation.parse(cube.texture());
            ModelFile cubeModel = models().cubeAll(cube.registryPath(), texture);
            simpleBlockWithItem(block, cubeModel);
        }
    }

    private void registerGtStones() {
        for (GtStoneCatalog.Variant variant : GtStoneCatalog.variants()) {
            var block = ModBlocks.gtStoneBlocksById().get(variant.id()).get();
            String path = variant.registryPath();
            if (variant.slab()) {
                var doubleslab = models().getExistingFile(modLoc(path + "_double"));
                var bottom = models().getExistingFile(modLoc(path + "_bottom"));
                var top = models().getExistingFile(modLoc(path + "_top"));
                slabBlock((SlabBlock) block, bottom, top, doubleslab);
                itemModels().getBuilder(path).parent(bottom);
            } else {
                ModelFile cube = models().getExistingFile(modLoc(path));
                simpleBlock(block, cube);
                itemModels().getBuilder(path).parent(cube);
            }
        }
    }

    private void registerGtBlockObjects() {
        for (GtBlockObjectCatalog.Variant variant : GtBlockObjectCatalog.variants()) {
            registerGtBlockObject(
                    variant,
                    ModBlocks.gtBlockObjectBlocksById().get(variant.id()).get());
        }
        for (GtBlockObjectCatalog.Variant variant :
                com.masson.cruciblecraft.content.item.BathRemainderBlockObjectCatalog.variants()) {
            registerGtBlockObject(
                    variant,
                    ModBlocks.bathRemainderBlockObjectBlocksById().get(variant.id()).get());
        }
    }

    private void registerGtBlockObject(
            GtBlockObjectCatalog.Variant variant,
            net.minecraft.world.level.block.Block block) {
        ResourceLocation texture = variant.textureLocation();
        String path = variant.registryPath();
        if (variant.slab()) {
            var doubleslab = models().getExistingFile(modLoc(path + "_double"));
            var bottom = models().getExistingFile(modLoc(path + "_bottom"));
            var top = models().getExistingFile(modLoc(path + "_top"));
            slabBlock((SlabBlock) block, bottom, top, doubleslab);
            itemModels().getBuilder(path).parent(bottom);
        } else if (variant.log() || variant.bale()) {
            var vertical = models().getExistingFile(modLoc(path));
            var horizontal = models().getExistingFile(modLoc(path + "_horizontal"));
            axisBlock((RotatedPillarBlock) block, vertical, horizontal);
            itemModels().getBuilder(path).parent(vertical);
        } else if (variant.bars()) {
            paneBlockWithRenderType(
                    (IronBarsBlock) block, texture, texture, "cutout");
            itemModels().getBuilder(path).parent(
                    models().getExistingFile(mcLoc("item/generated")))
                    .texture("layer0", texture);
        } else if (variant.rail()) {
            ModelFile rail = models().getExistingFile(mcLoc("block/rail"));
            getVariantBuilder(block).forAllStates(state ->
                    ConfiguredModel.builder().modelFile(rail).build());
            itemModels().getBuilder(path).parent(
                    models().getExistingFile(mcLoc("item/generated")))
                    .texture("layer0", texture);
        } else {
            ModelFile cube = models().getExistingFile(modLoc(path));
            simpleBlock(block, cube);
            itemModels().getBuilder(path).parent(cube);
        }
    }

    private void registerHoppers() {
        ModelFile hopper = models().getExistingFile(modLoc("block/hopper"));
        ModelFile hopperSide = models().getExistingFile(modLoc("block/hopper_side"));
        ModelFile queue = models().getExistingFile(modLoc("block/queue_hopper"));
        ModelFile queueSide = models().getExistingFile(
                modLoc("block/queue_hopper_side"));
        HopperVariantCatalog.variants().forEach(variant -> {
            HopperBlock block = ModBlocks.hopperBlocksById().get(variant.id()).get();
            boolean queueKind = variant.kind() == HopperKind.QUEUE_HOPPER;
            ModelFile down = queueKind ? queue : hopper;
            ModelFile side = queueKind ? queueSide : hopperSide;
            getVariantBuilder(block).forAllStates(state -> {
                Direction facing = state.getValue(HopperBlock.FACING);
                var builder = ConfiguredModel.builder();
                if (facing == Direction.DOWN) {
                    return builder.modelFile(down).build();
                }
                if (facing == Direction.UP) {
                    return builder.modelFile(down).rotationX(180).build();
                }
                int y = switch (facing) {
                    case SOUTH -> 180;
                    case WEST -> 270;
                    case EAST -> 90;
                    default -> 0;
                };
                return builder.modelFile(side).rotationY(y).build();
            });
            simpleBlockItem(block, down);
        });
        simpleBlockWithItem(
                ModBlocks.STEEL_DUST_FUNNEL.get(),
                models().getExistingFile(modLoc("block/dust_funnel")));
    }

    private void registerSensors() {
        for (SensorKind kind : SensorKind.all()) {
            SensorBlock block = ModBlocks.sensorBlocksById().get(kind.id()).get();
            String folder = "block/machine/sensor/" + kind.textureFolder();
            ModelFile model = models()
                    .withExistingParent(kind.path(), modLoc("block/sensor_slab"))
                    .texture("particle", folder + "/colored/front")
                    .texture("bot_north", folder + "/colored/front")
                    .texture("bot_south", folder + "/colored/back")
                    .texture("bot_up", folder + "/colored/side")
                    .texture("bot_down", folder + "/colored/side")
                    .texture("bot_west", folder + "/colored/side")
                    .texture("bot_east", folder + "/colored/side")
                    .texture("top_north", folder + "/overlay/front")
                    .texture("top_south", folder + "/overlay/back")
                    .texture("top_up", folder + "/overlay/side")
                    .texture("top_down", folder + "/overlay/side")
                    .texture("top_west", folder + "/overlay/side")
                    .texture("top_east", folder + "/overlay/side");
            getVariantBuilder(block).forAllStates(state -> {
                Direction facing = state.getValue(SensorBlock.FACING);
                var builder = ConfiguredModel.builder().modelFile(model);
                return switch (facing) {
                    case DOWN -> builder.rotationX(90).build();
                    case UP -> builder.rotationX(270).build();
                    case SOUTH -> builder.rotationY(180).build();
                    case WEST -> builder.rotationY(270).build();
                    case EAST -> builder.rotationY(90).build();
                    case NORTH -> builder.build();
                };
            });
            simpleBlockItem(block, model);
        }
    }

    private void registerStorage() {
        StorageVariantCatalog.variants().forEach(variant -> {
            var block = ModBlocks.storageBlocksById().get(variant.id()).get();
            ModelFile model = models().getExistingFile(variant.behavior().model());
            horizontalBlock(block, model);
            simpleBlockItem(block, model);
        });
    }

    private void registerConductors() {
        java.util.Map<String, ModelFile> cores = new java.util.LinkedHashMap<>();
        java.util.Map<String, ModelFile> arms = new java.util.LinkedHashMap<>();
        java.util.Set<String> itemModels = new java.util.HashSet<>();
        ModBlocks.electricalConductorBlocks().forEach(holder -> {
            CableBlock block = holder.get();
            String specification = block.conductor().sourceSpecification();
            ModelFile core = cores.computeIfAbsent(
                    specification, this::conductorCore);
            ModelFile arm = arms.computeIfAbsent(
                    specification, this::conductorArm);
            var multipart = getMultipartBuilder(block);
            multipart.part().modelFile(core).addModel().end();
            conductorArm(multipart, arm, CableBlock.DOWN, 90, 0);
            conductorArm(multipart, arm, CableBlock.UP, 270, 0);
            conductorArm(multipart, arm, CableBlock.NORTH, 0, 0);
            conductorArm(multipart, arm, CableBlock.SOUTH, 0, 180);
            conductorArm(multipart, arm, CableBlock.WEST, 0, 270);
            conductorArm(multipart, arm, CableBlock.EAST, 0, 90);
            if (itemModels.add(specification)) {
                itemThroughModel(
                        "conductor/"
                                + specification.toLowerCase(java.util.Locale.ROOT)
                                + "_item",
                        conductorWidth(specification),
                        conductorSide(specification),
                        conductorSide(specification),
                        conductorOverlay(specification));
            }
        });
    }

    private void registerLuFiberCable() {
        ResourceLocation fiber =
                modLoc("block/gt6_import/lu_fiber_wire");
        ResourceLocation overlay =
                modLoc("block/gt6_import/lu_fiber_wire_overlay");
        ModelFile core = pipeCube(
                "conductor/lu_fiber_core",
                2.0F,
                false,
                fiber,
                fiber,
                null);
        ModelFile arm = pipeCube(
                "conductor/lu_fiber_arm",
                2.0F,
                true,
                fiber,
                fiber,
                overlay);
        var multipart = getMultipartBuilder(ModBlocks.LU_FIBER_CABLE.get());
        multipart.part().modelFile(core).addModel().end();
        conductorArm(multipart, arm, CableBlock.DOWN, 90, 0);
        conductorArm(multipart, arm, CableBlock.UP, 270, 0);
        conductorArm(multipart, arm, CableBlock.NORTH, 0, 0);
        conductorArm(multipart, arm, CableBlock.SOUTH, 0, 180);
        conductorArm(multipart, arm, CableBlock.WEST, 0, 270);
        conductorArm(multipart, arm, CableBlock.EAST, 0, 90);
        itemThroughModel(
                "conductor/lu_fiber_cable_item",
                2.0F,
                fiber,
                fiber,
                overlay);
    }

    private ModelFile conductorCore(String specification) {
        return pipeCube(
                "conductor/"
                        + specification.toLowerCase(java.util.Locale.ROOT)
                        + "_core",
                conductorWidth(specification),
                false,
                conductorSide(specification),
                conductorSide(specification),
                conductorOverlay(specification));
    }

    private ModelFile conductorArm(String specification) {
        return pipeCube(
                "conductor/"
                        + specification.toLowerCase(java.util.Locale.ROOT)
                        + "_arm",
                conductorWidth(specification),
                true,
                conductorSide(specification),
                conductorSide(specification),
                conductorOverlay(specification));
    }

    private ResourceLocation conductorSide(String specification) {
        if (specification == null || specification.isEmpty()) {
            throw new IllegalArgumentException("blank conductor specification");
        }
        return modLoc("block/gt6_import/materialicons/copper/wire");
    }

    private ResourceLocation conductorOverlay(String specification) {
        return switch (specification) {
            case "cableGt01" ->
                    modLoc("block/gt6_import/iconsets/insulation_tiny");
            case "cableGt02" ->
                    modLoc("block/gt6_import/iconsets/insulation_small");
            case "cableGt04" ->
                    modLoc("block/gt6_import/iconsets/insulation_medium");
            case "cableGt08" ->
                    modLoc("block/gt6_import/iconsets/insulation_large");
            case "cableGt12" ->
                    modLoc("block/gt6_import/iconsets/insulation_huge");
            default -> specification.startsWith("wireGt")
                    ? modLoc(
                            "block/gt6_import/materialicons/copper/wire_overlay")
                    : null;
        };
    }

    private static void conductorArm(
            net.neoforged.neoforge.client.model.generators
                            .MultiPartBlockStateBuilder multipart,
            ModelFile arm,
            net.minecraft.world.level.block.state.properties.BooleanProperty
                    property,
            int rotationX,
            int rotationY) {
        multipart.part()
                .modelFile(arm)
                .rotationX(rotationX)
                .rotationY(rotationY)
                .addModel()
                .condition(property, true)
                .end();
    }

    private static float conductorWidth(String specification) {
        return ElectricalConductorCatalog.widthPixels(specification);
    }

    private void registerPipes() {
        java.util.Map<String, ModelFile> cores =
                new java.util.LinkedHashMap<>();
        java.util.Map<String, ModelFile> arms =
                new java.util.LinkedHashMap<>();
        java.util.Set<String> itemModels = new java.util.HashSet<>();
        ModBlocks.pipeBlocks().forEach(holder -> {
            AbstractPipeBlock block = holder.get();
            String modelKey = block.pipe().kind().name().toLowerCase(
                    java.util.Locale.ROOT) + "_" + block.pipe().textureKey();
            ResourceLocation side = pipeTexture(block.pipe());
            ResourceLocation overlay = pipeOverlay(block.pipe());
            ModelFile core = cores.computeIfAbsent(
                    modelKey,
                    ignored -> pipeCube(
                            "pipe/" + modelKey + "_core",
                            block.pipe().width(),
                            false,
                            side,
                            side,
                            overlay));
            ModelFile arm = arms.computeIfAbsent(
                    modelKey,
                    ignored -> pipeCube(
                            "pipe/" + modelKey + "_arm",
                            block.pipe().width(),
                            true,
                            side,
                            side,
                            overlay));
            var multipart = getMultipartBuilder(block);
            multipart.part().modelFile(core).addModel().end();
            conductorArm(
                    multipart, arm, AbstractPipeBlock.DOWN, 90, 0);
            conductorArm(
                    multipart, arm, AbstractPipeBlock.UP, 270, 0);
            conductorArm(
                    multipart, arm, AbstractPipeBlock.NORTH, 0, 0);
            conductorArm(
                    multipart, arm, AbstractPipeBlock.SOUTH, 0, 180);
            conductorArm(
                    multipart, arm, AbstractPipeBlock.WEST, 0, 270);
            conductorArm(
                    multipart, arm, AbstractPipeBlock.EAST, 0, 90);
            if (itemModels.add(modelKey)) {
                itemThroughModel(
                        "pipe/" + modelKey + "_item",
                        block.pipe().width(),
                        side,
                        side,
                        overlay);
            }
        });
    }

    private ResourceLocation pipeTexture(PipeCatalog.Entry pipe) {
        return switch (pipe.textureKey()) {
            case "4" -> modLoc("block/gt6_import/materialicons/copper/pipetiny");
            case "6" -> modLoc("block/gt6_import/materialicons/copper/pipesmall");
            case "8" ->
                    modLoc("block/gt6_import/materialicons/copper/pipemedium");
            case "12" -> modLoc("block/gt6_import/materialicons/copper/pipelarge");
            case "16" -> modLoc("block/gt6_import/materialicons/copper/pipehuge");
            case "quadruple" ->
                    modLoc("block/gt6_import/materialicons/copper/pipequadruple");
            case "nonuple" ->
                    modLoc("block/gt6_import/materialicons/copper/pipenonuple");
            case "restrictive_8" ->
                    modLoc("block/gt6_import/materialicons/copper/pipemedium");
            case "restrictive_12" ->
                    modLoc("block/gt6_import/materialicons/copper/pipelarge");
            case "restrictive_16" ->
                    modLoc("block/gt6_import/materialicons/copper/pipehuge");
            default -> throw new IllegalArgumentException(
                    "Unsupported pipe texture " + pipe.textureKey());
        };
    }

    private ResourceLocation pipeOverlay(PipeCatalog.Entry pipe) {
        return switch (pipe.textureKey()) {
            case "4" ->
                    modLoc("block/gt6_import/materialicons/copper/pipetiny_overlay");
            case "6" ->
                    modLoc("block/gt6_import/materialicons/copper/pipesmall_overlay");
            case "8" ->
                    modLoc("block/gt6_import/materialicons/copper/pipemedium_overlay");
            case "12" ->
                    modLoc("block/gt6_import/materialicons/copper/pipelarge_overlay");
            case "16" ->
                    modLoc("block/gt6_import/materialicons/copper/pipehuge_overlay");
            case "quadruple" ->
                    modLoc("block/gt6_import/materialicons/copper/pipequadruple_overlay");
            case "nonuple" ->
                    modLoc("block/gt6_import/materialicons/copper/pipenonuple_overlay");
            case "restrictive_8", "restrictive_12", "restrictive_16" ->
                    modLoc("block/gt6_import/iconsets/pipe_restrictor");
            default -> throw new IllegalArgumentException(
                    "Unsupported pipe overlay " + pipe.textureKey());
        };
    }

    private ModelFile pipeCube(
            String path,
            float width,
            boolean arm,
            ResourceLocation side,
            ResourceLocation end,
            ResourceLocation overlay) {
        float minimum = 8.0F - width / 2.0F;
        float maximum = 8.0F + width / 2.0F;
        BlockModelBuilder builder = models()
                .withExistingParent(path, mcLoc("block/block"))
                .renderType("cutout_mipped")
                .texture("particle", side)
                .texture("side", side)
                .texture("end", end);
        if (overlay != null) {
            builder.texture("overlay", overlay);
        }
        addPipeElement(
                builder,
                minimum,
                minimum,
                arm ? 0.0F : minimum,
                maximum,
                maximum,
                arm ? minimum : maximum,
                arm ? Direction.NORTH : null,
                false);
        if (overlay != null) {
            addPipeElement(
                    builder,
                    minimum,
                    minimum,
                    arm ? 0.0F : minimum,
                    maximum,
                    maximum,
                    arm ? minimum : maximum,
                    arm ? Direction.NORTH : null,
                    true);
        }
        return builder;
    }

    private ModelFile itemThroughModel(
            String path,
            float width,
            ResourceLocation side,
            ResourceLocation end,
            ResourceLocation overlay) {
        float minimum = 8.0F - width / 2.0F;
        float maximum = 8.0F + width / 2.0F;
        BlockModelBuilder builder = models()
                .withExistingParent(path, mcLoc("block/block"))
                .renderType("cutout_mipped")
                .texture("particle", side)
                .texture("side", side)
                .texture("end", end);
        if (overlay != null) {
            builder.texture("overlay", overlay);
        }
        var element = builder.element()
                .from(minimum, minimum, 0.0F)
                .to(maximum, maximum, 16.0F);
        for (Direction direction : Direction.values()) {
            boolean cap = direction.getAxis() == Direction.Axis.Z;
            element.face(direction)
                    .texture(cap ? "#end" : "#side")
                    .tintindex(0)
                    .end();
        }
        element.end();
        if (overlay != null) {
            var overlayElement = builder.element()
                    .from(minimum, minimum, 0.0F)
                    .to(maximum, maximum, 16.0F);
            for (Direction direction : Direction.values()) {
                overlayElement.face(direction)
                        .texture("#overlay")
                        .end();
            }
            overlayElement.end();
        }
        return builder;
    }

    private static void addPipeElement(
            BlockModelBuilder builder,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            Direction endFace,
            boolean overlay) {
        float offset = overlay ? 0.002F : 0.0F;
        var element = builder.element()
                .from(x1 - offset, y1 - offset, z1 - offset)
                .to(x2 + offset, y2 + offset, z2 + offset);
        for (Direction direction : Direction.values()) {
            if (!overlay && endFace != null && direction == endFace.getOpposite()) {
                continue;
            }
            boolean cap = endFace == direction;
            if (overlay && endFace != null && !cap) {
                continue;
            }
            var face = element.face(direction)
                    .texture(overlay ? "#overlay" : cap ? "#end" : "#side");
            if (!overlay) {
                face.tintindex(0);
            }
            face.end();
        }
        element.end();
    }

    private void registerConverters() {
        java.util.HashSet<String> modeled = new java.util.HashSet<>();
        for (var profile : com.masson.cruciblecraft.energy.converter
                .EnergyConverterCatalog.profiles()) {
            var kind = com.masson.cruciblecraft.energy.converter
                    .EnergyConverterKindCatalog.require(
                            com.masson.cruciblecraft.energy.converter
                                    .EnergyConverterTierCatalog.require(
                                            profile.id())
                                    .kindId());
            String texture = kind.textureProfile();
            ensureConverterModels(texture, kind.overlayActive(), modeled);
            ModelFile inactive = models().getExistingFile(
                    modLoc("block/" + texture));
            ModelFile active = kind.overlayActive()
                    ? models().getExistingFile(
                            modLoc("block/" + texture + "_active"))
                    : inactive;
            var block = ModBlocks.converterBlocksById().get(profile.id()).get();
            if (HANDWRITTEN_CONVERTER_MODELS.contains(profile.id().getPath())) {
                continue;
            }
            boolean allDirections = kind.runtime().equals("electric_heater")
                    || kind.runtime().equals("electric_engine");
            if (allDirections) {
                configuredFacingLitAllDirections(block, inactive, active);
            } else if (kind.overlayActive()) {
                configuredFacingLit(block, inactive, active);
            } else {
                horizontalBlock(block, inactive);
            }
            simpleBlockItem(block, inactive);
        }
    }

    private void registerBatteries() {
        for (var profile : com.masson.cruciblecraft.energy.battery
                .EnergyBatteryCatalog.profiles()) {
            String modelName = "battery/" + profile.id().getPath();
            ModelFile model = batteryModel(profile, modelName);
            var block = ModBlocks.batteryBlocksById().get(profile.id()).get();
            simpleBlock(block, model);
            simpleBlockItem(block, model);
        }
    }

    private void registerTransformers() {
        ModelFile[] models = {
            transformerCube("overlay"),
            transformerCube("overlay_active"),
            transformerCube("overlay_blinking")
        };
        for (var profile : com.masson.cruciblecraft.energy.transformer
                .EnergyTransformerCatalog.profiles()) {
            var block = ModBlocks.transformerBlocksById()
                    .get(profile.id())
                    .get();
            getVariantBuilder(block).forAllStates(state -> {
                Direction facing = state.getValue(
                        com.masson.cruciblecraft.energy.transformer
                                .TransformerBlock.FACING);
                int activity = state.getValue(
                        com.masson.cruciblecraft.energy.transformer
                                .TransformerBlock.ACTIVITY);
                var builder = ConfiguredModel.builder()
                        .modelFile(models[activity]);
                return switch (facing) {
                    case DOWN -> builder.rotationX(90).build();
                    case UP -> builder.rotationX(270).build();
                    case SOUTH -> builder.rotationY(180).build();
                    case WEST -> builder.rotationY(270).build();
                    case EAST -> builder.rotationY(90).build();
                    case NORTH -> builder.build();
                };
            });
            simpleBlockItem(block, models[0]);
        }
    }

    private void registerHeatExchangers() {
        ModelFile idle = heatExchangerCube("overlay");
        ModelFile active = heatExchangerCube("overlay_active");
        for (var profile : com.masson.cruciblecraft.energy.heatexchanger
                .HeatExchangerCatalog.profiles()) {
            var block = ModBlocks.heatExchangerBlocksById()
                    .get(profile.id())
                    .get();
            getVariantBuilder(block).forAllStates(state -> {
                Direction facing = state.getValue(
                        com.masson.cruciblecraft.energy.heatexchanger
                                .HeatExchangerBlock.FACING);
                boolean lit = state.getValue(
                        com.masson.cruciblecraft.energy.heatexchanger
                                .HeatExchangerBlock.LIT);
                var builder = ConfiguredModel.builder()
                        .modelFile(lit ? active : idle);
                return switch (facing) {
                    case SOUTH -> builder.rotationY(180).build();
                    case WEST -> builder.rotationY(270).build();
                    case EAST -> builder.rotationY(90).build();
                    default -> builder.build();
                };
            });
            simpleBlockItem(block, idle);
        }
    }

    private void registerLargeHeatExchanger() {
        String base = "block/machine/large_heat_exchanger";
        ModelFile idle = models()
                .withExistingParent(
                        "large_heat_exchanger/overlay",
                        modLoc("block/machine_cube_2_layer"))
                .texture("particle", modLoc(base + "/colored/front"))
                .texture("bot_down", modLoc(base + "/colored/bottom"))
                .texture("bot_up", modLoc(base + "/colored/top"))
                .texture("bot_north", modLoc(base + "/colored/front"))
                .texture("bot_south", modLoc(base + "/colored/back"))
                .texture("bot_west", modLoc(base + "/colored/left"))
                .texture("bot_east", modLoc(base + "/colored/right"))
                .texture("top_down", modLoc(base + "/overlay/bottom"))
                .texture("top_up", modLoc(base + "/overlay/top"))
                .texture("top_north", modLoc(base + "/overlay/front"))
                .texture("top_south", modLoc(base + "/overlay/back"))
                .texture("top_west", modLoc(base + "/overlay/left"))
                .texture("top_east", modLoc(base + "/overlay/right"));
        ModelFile active = models()
                .withExistingParent(
                        "large_heat_exchanger/overlay_active",
                        modLoc("block/machine_cube_2_layer"))
                .texture("particle", modLoc(base + "/colored/front"))
                .texture("bot_down", modLoc(base + "/colored/bottom"))
                .texture("bot_up", modLoc(base + "/colored/top"))
                .texture("bot_north", modLoc(base + "/colored/front"))
                .texture("bot_south", modLoc(base + "/colored/back"))
                .texture("bot_west", modLoc(base + "/colored/left"))
                .texture("bot_east", modLoc(base + "/colored/right"))
                .texture("top_down", modLoc(base + "/overlay_active/bottom"))
                .texture("top_up", modLoc(base + "/overlay_active/top"))
                .texture("top_north", modLoc(base + "/overlay_active/front"))
                .texture("top_south", modLoc(base + "/overlay_active/back"))
                .texture("top_west", modLoc(base + "/overlay_active/left"))
                .texture("top_east", modLoc(base + "/overlay_active/right"));
        var block = ModBlocks.LARGE_HEAT_EXCHANGER.get();
        getVariantBuilder(block).forAllStates(state -> {
            Direction facing = state.getValue(
                    com.masson.cruciblecraft.energy.largeheatexchanger
                            .LargeHeatExchangerBlock.FACING);
            boolean lit = state.getValue(
                    com.masson.cruciblecraft.energy.largeheatexchanger
                            .LargeHeatExchangerBlock.LIT);
            var builder = ConfiguredModel.builder()
                    .modelFile(lit ? active : idle);
            return switch (facing) {
                case SOUTH -> builder.rotationY(180).build();
                case WEST -> builder.rotationY(270).build();
                case EAST -> builder.rotationY(90).build();
                default -> builder.build();
            };
        });
        simpleBlockItem(block, idle);
    }

    private void registerBedrockDrill() {
        String base = "block/machine/bedrock_drill";
        ModelFile idle = models()
                .withExistingParent(
                        "bedrock_drill/overlay",
                        modLoc("block/machine_cube_2_layer"))
                .texture("particle", modLoc(base + "/colored/front"))
                .texture("bot_down", modLoc(base + "/colored/bottom"))
                .texture("bot_up", modLoc(base + "/colored/top"))
                .texture("bot_north", modLoc(base + "/colored/front"))
                .texture("bot_south", modLoc(base + "/colored/back"))
                .texture("bot_west", modLoc(base + "/colored/left"))
                .texture("bot_east", modLoc(base + "/colored/right"))
                .texture("top_down", modLoc(base + "/overlay/bottom"))
                .texture("top_up", modLoc(base + "/overlay/top"))
                .texture("top_north", modLoc(base + "/overlay/front"))
                .texture("top_south", modLoc(base + "/overlay/back"))
                .texture("top_west", modLoc(base + "/overlay/left"))
                .texture("top_east", modLoc(base + "/overlay/right"));
        ModelFile active = models()
                .withExistingParent(
                        "bedrock_drill/overlay_active",
                        modLoc("block/machine_cube_2_layer"))
                .texture("particle", modLoc(base + "/colored/front"))
                .texture("bot_down", modLoc(base + "/colored/bottom"))
                .texture("bot_up", modLoc(base + "/colored/top"))
                .texture("bot_north", modLoc(base + "/colored/front"))
                .texture("bot_south", modLoc(base + "/colored/back"))
                .texture("bot_west", modLoc(base + "/colored/left"))
                .texture("bot_east", modLoc(base + "/colored/right"))
                .texture("top_down", modLoc(base + "/overlay_active/bottom"))
                .texture("top_up", modLoc(base + "/overlay_active/top"))
                .texture("top_north", modLoc(base + "/overlay_active/front"))
                .texture("top_south", modLoc(base + "/overlay_active/back"))
                .texture("top_west", modLoc(base + "/overlay_active/left"))
                .texture("top_east", modLoc(base + "/overlay_active/right"));
        var block = ModBlocks.BEDROCK_DRILL.get();
        getVariantBuilder(block).forAllStates(state -> {
            Direction facing = state.getValue(BedrockDrillBlock.FACING);
            boolean lit = state.getValue(BedrockDrillBlock.LIT);
            var builder = ConfiguredModel.builder()
                    .modelFile(lit ? active : idle);
            return switch (facing) {
                case SOUTH -> builder.rotationY(180).build();
                case WEST -> builder.rotationY(270).build();
                case EAST -> builder.rotationY(90).build();
                case DOWN -> builder.rotationX(90).build();
                case UP -> builder.rotationX(270).build();
                default -> builder.build();
            };
        });
        simpleBlockItem(block, idle);
        simpleBlockWithItem(
                ModBlocks.BEDROCK_DRILL_HEAD.get(),
                models().cubeAll(
                        "bedrock_drill_head",
                        modLoc(base + "/colored/front")));
    }

    private void registerQuantumEnergizers() {
        String base = "block/machine/quantum_energizer";
        ModelFile idle = models()
                .withExistingParent(
                        "quantum_energizer/overlay",
                        modLoc("block/machine_cube_2_layer"))
                .texture("particle", modLoc(base + "/colored/front"))
                .texture("bot_down", modLoc(base + "/colored/bottom"))
                .texture("bot_up", modLoc(base + "/colored/top"))
                .texture("bot_north", modLoc(base + "/colored/front"))
                .texture("bot_south", modLoc(base + "/colored/back"))
                .texture("bot_west", modLoc(base + "/colored/left"))
                .texture("bot_east", modLoc(base + "/colored/right"))
                .texture("top_down", modLoc(base + "/overlay/bottom"))
                .texture("top_up", modLoc(base + "/overlay/top"))
                .texture("top_north", modLoc(base + "/overlay/front"))
                .texture("top_south", modLoc(base + "/overlay/back"))
                .texture("top_west", modLoc(base + "/overlay/left"))
                .texture("top_east", modLoc(base + "/overlay/right"));
        ModelFile active = models()
                .withExistingParent(
                        "quantum_energizer/overlay_active",
                        modLoc("block/machine_cube_2_layer"))
                .texture("particle", modLoc(base + "/colored/front"))
                .texture("bot_down", modLoc(base + "/colored/bottom"))
                .texture("bot_up", modLoc(base + "/colored/top"))
                .texture("bot_north", modLoc(base + "/colored/front"))
                .texture("bot_south", modLoc(base + "/colored/back"))
                .texture("bot_west", modLoc(base + "/colored/left"))
                .texture("bot_east", modLoc(base + "/colored/right"))
                .texture("top_down", modLoc(base + "/overlay_active/bottom"))
                .texture("top_up", modLoc(base + "/overlay_active/top"))
                .texture("top_north", modLoc(base + "/overlay_active/front"))
                .texture("top_south", modLoc(base + "/overlay_active/back"))
                .texture("top_west", modLoc(base + "/overlay_active/left"))
                .texture("top_east", modLoc(base + "/overlay_active/right"));
        for (var profile : com.masson.cruciblecraft.energy.quantum
                .QuantumEnergizerCatalog.profiles()) {
            var block = ModBlocks.quantumEnergizerBlocksById()
                    .get(profile.id())
                    .get();
            getVariantBuilder(block).forAllStates(state -> {
                Direction facing = state.getValue(
                        com.masson.cruciblecraft.energy.quantum
                                .QuantumEnergizerBlock.FACING);
                boolean lit = state.getValue(
                        com.masson.cruciblecraft.energy.quantum
                                .QuantumEnergizerBlock.LIT);
                var builder = ConfiguredModel.builder()
                        .modelFile(lit ? active : idle);
                return switch (facing) {
                    case DOWN -> builder.rotationX(90).build();
                    case UP -> builder.rotationX(270).build();
                    case SOUTH -> builder.rotationY(180).build();
                    case WEST -> builder.rotationY(270).build();
                    case EAST -> builder.rotationY(90).build();
                    default -> builder.build();
                };
            });
            simpleBlockItem(block, idle);
        }
    }

    private void registerLongDistanceTransformers() {
        String base = "block/machine/long_distance_transformer";
        ModelFile idle = models()
                .withExistingParent(
                        "long_distance_transformer/overlay",
                        modLoc("block/machine_cube_2_layer"))
                .texture("particle", modLoc(base + "/colored/front"))
                .texture("bot_down", modLoc(base + "/colored/bottom"))
                .texture("bot_up", modLoc(base + "/colored/top"))
                .texture("bot_north", modLoc(base + "/colored/front"))
                .texture("bot_south", modLoc(base + "/colored/back"))
                .texture("bot_west", modLoc(base + "/colored/left"))
                .texture("bot_east", modLoc(base + "/colored/right"))
                .texture("top_down", modLoc(base + "/overlay/bottom"))
                .texture("top_up", modLoc(base + "/overlay/top"))
                .texture("top_north", modLoc(base + "/overlay/front"))
                .texture("top_south", modLoc(base + "/overlay/back"))
                .texture("top_west", modLoc(base + "/overlay/left"))
                .texture("top_east", modLoc(base + "/overlay/right"));
        ModelFile active = models()
                .withExistingParent(
                        "long_distance_transformer/overlay_active",
                        modLoc("block/machine_cube_2_layer"))
                .texture("particle", modLoc(base + "/colored/front"))
                .texture("bot_down", modLoc(base + "/colored/bottom"))
                .texture("bot_up", modLoc(base + "/colored/top"))
                .texture("bot_north", modLoc(base + "/colored/front"))
                .texture("bot_south", modLoc(base + "/colored/back"))
                .texture("bot_west", modLoc(base + "/colored/left"))
                .texture("bot_east", modLoc(base + "/colored/right"))
                .texture("top_down", modLoc(base + "/overlay_active/bottom"))
                .texture("top_up", modLoc(base + "/overlay_active/top"))
                .texture("top_north", modLoc(base + "/overlay_active/front"))
                .texture("top_south", modLoc(base + "/overlay_active/back"))
                .texture("top_west", modLoc(base + "/overlay_active/left"))
                .texture("top_east", modLoc(base + "/overlay_active/right"));
        for (var profile : com.masson.cruciblecraft.energy.longdistance
                .LongDistanceTransformerCatalog.endpoints()) {
            var block = ModBlocks.longDistanceTransformerBlocksById()
                    .get(profile.id())
                    .get();
            getVariantBuilder(block).forAllStates(state -> {
                Direction facing = state.getValue(
                        com.masson.cruciblecraft.energy.longdistance
                                .LongDistanceTransformerBlock.FACING);
                boolean lit = state.getValue(
                        com.masson.cruciblecraft.energy.longdistance
                                .LongDistanceTransformerBlock.LIT);
                var builder = ConfiguredModel.builder()
                        .modelFile(lit ? active : idle);
                return switch (facing) {
                    case DOWN -> builder.rotationX(90).build();
                    case UP -> builder.rotationX(270).build();
                    case SOUTH -> builder.rotationY(180).build();
                    case WEST -> builder.rotationY(270).build();
                    case EAST -> builder.rotationY(90).build();
                    default -> builder.build();
                };
            });
            simpleBlockItem(block, idle);
        }
        for (var profile : com.masson.cruciblecraft.energy.longdistance
                .LongDistanceTransformerCatalog.wires()) {
            var block = ModBlocks.longDistanceWireBlocksById()
                    .get(profile.id())
                    .get();
            String path = profile.id().getPath();
            String voltage = path.substring("long_distance_wire_".length());
            simpleBlockWithItem(
                    block,
                    models().cubeAll(
                            path,
                            modLoc("block/long_distance_wire/" + voltage)));
        }
    }

    private ModelFile heatExchangerCube(String overlay) {
        String name = "heat_exchanger/" + overlay;
        String base = "block/machine/heat_exchanger";
        return models()
                .withExistingParent(
                        name, modLoc("block/machine_cube_2_layer"))
                .texture("particle", modLoc(base + "/colored/front"))
                .texture("bot_down", modLoc(base + "/colored/bottom"))
                .texture("bot_up", modLoc(base + "/colored/top"))
                .texture("bot_north", modLoc(base + "/colored/front"))
                .texture("bot_south", modLoc(base + "/colored/back"))
                .texture("bot_west", modLoc(base + "/colored/left"))
                .texture("bot_east", modLoc(base + "/colored/right"))
                .texture("top_down", modLoc(base + "/" + overlay + "/bottom"))
                .texture("top_up", modLoc(base + "/" + overlay + "/top"))
                .texture("top_north", modLoc(base + "/" + overlay + "/front"))
                .texture("top_south", modLoc(base + "/" + overlay + "/back"))
                .texture("top_west", modLoc(base + "/" + overlay + "/left"))
                .texture("top_east", modLoc(base + "/" + overlay + "/right"));
    }

    private ModelFile transformerCube(String overlay) {
        String name = "transformer/electric_" + overlay;
        String base = "block/machine/transformer/electric";
        return models()
                .withExistingParent(
                        name, modLoc("block/machine_cube_2_layer"))
                .texture("particle", modLoc(base + "/colored/front"))
                .texture("bot_down", modLoc(base + "/colored/side"))
                .texture("bot_up", modLoc(base + "/colored/side"))
                .texture("bot_north", modLoc(base + "/colored/front"))
                .texture("bot_south", modLoc(base + "/colored/back"))
                .texture("bot_west", modLoc(base + "/colored/side"))
                .texture("bot_east", modLoc(base + "/colored/side"))
                .texture("top_down", modLoc(base + "/" + overlay + "/side"))
                .texture("top_up", modLoc(base + "/" + overlay + "/side"))
                .texture("top_north", modLoc(base + "/" + overlay + "/front"))
                .texture("top_south", modLoc(base + "/" + overlay + "/back"))
                .texture("top_west", modLoc(base + "/" + overlay + "/side"))
                .texture("top_east", modLoc(base + "/" + overlay + "/side"));
    }

    private ModelFile batteryModel(
            com.masson.cruciblecraft.energy.battery.EnergyBatteryProfile profile,
            String name) {
        net.minecraft.world.phys.AABB box = profile.shape().bounds();
        float minX = (float) (box.minX * 16.0);
        float minY = (float) (box.minY * 16.0);
        float minZ = (float) (box.minZ * 16.0);
        float maxX = (float) (box.maxX * 16.0);
        float maxY = (float) (box.maxY * 16.0);
        float maxZ = (float) (box.maxZ * 16.0);
        String base = profile.textureFolder();
        BlockModelBuilder builder = models()
                .withExistingParent(name, mcLoc("block/block"))
                .renderType("cutout")
                .texture("particle", modLoc(base + "/sides"))
                .texture("bottom", modLoc(base + "/bottom"))
                .texture("top", modLoc(base + "/top"))
                .texture("side", modLoc(base + "/sides"));
        var element = builder.element()
                .from(minX, minY, minZ)
                .to(maxX, maxY, maxZ);
        element.face(Direction.DOWN).texture("#bottom").end();
        element.face(Direction.UP).texture("#top").end();
        element.face(Direction.NORTH).texture("#side").end();
        element.face(Direction.SOUTH).texture("#side").end();
        element.face(Direction.WEST).texture("#side").end();
        element.face(Direction.EAST).texture("#side").end();
        element.end();
        return builder;
    }

    private void ensureConverterModels(
            String texture, boolean overlayActive, java.util.Set<String> modeled) {
        if (!modeled.add(texture)) {
            return;
        }
        if (HANDWRITTEN_CONVERTER_MODELS.contains(texture)) {
            return;
        }
        converterCube(texture, false);
        if (overlayActive) {
            converterCube(texture, true);
        }
    }

    private ModelFile converterCube(String textureId, boolean active) {
        String name = active ? textureId + "_active" : textureId;
        String overlay = active ? "overlay_active" : "overlay";
        String base = "block/machine/" + textureId;
        return models()
                .withExistingParent(name, modLoc("block/machine_cube_2_layer"))
                .texture("particle", modLoc(base + "/colored/front"))
                .texture("bot_down", modLoc(base + "/colored/bottom"))
                .texture("bot_up", modLoc(base + "/colored/top"))
                .texture("bot_north", modLoc(base + "/colored/front"))
                .texture("bot_south", modLoc(base + "/colored/back"))
                .texture("bot_west", modLoc(base + "/colored/left"))
                .texture("bot_east", modLoc(base + "/colored/right"))
                .texture("top_down", modLoc(base + "/" + overlay + "/bottom"))
                .texture("top_up", modLoc(base + "/" + overlay + "/top"))
                .texture("top_north", modLoc(base + "/" + overlay + "/front"))
                .texture("top_south", modLoc(base + "/" + overlay + "/back"))
                .texture("top_west", modLoc(base + "/" + overlay + "/left"))
                .texture("top_east", modLoc(base + "/" + overlay + "/right"));
    }

    private void configuredFacingLit(
            net.minecraft.world.level.block.Block block,
            ModelFile inactive,
            ModelFile active) {
        getVariantBuilder(block).forAllStates(state -> {
            int rotation = switch (state.getValue(
                    net.minecraft.world.level.block.state.properties
                            .BlockStateProperties.HORIZONTAL_FACING)) {
                case SOUTH -> 180;
                case WEST -> 270;
                case EAST -> 90;
                default -> 0;
            };
            boolean lit = state.getValue(
                    net.minecraft.world.level.block.state.properties
                            .BlockStateProperties.LIT);
            return ConfiguredModel.builder()
                    .modelFile(lit ? active : inactive)
                    .rotationY(rotation)
                    .build();
        });
    }

    private void configuredFacingLitAllDirections(
            net.minecraft.world.level.block.Block block,
            ModelFile inactive,
            ModelFile active) {
        getVariantBuilder(block).forAllStates(state -> {
            Direction facing = state.getValue(
                    net.minecraft.world.level.block.state.properties
                            .BlockStateProperties.FACING);
            boolean lit = state.getValue(
                    net.minecraft.world.level.block.state.properties
                            .BlockStateProperties.LIT);
            var builder = ConfiguredModel.builder()
                    .modelFile(lit ? active : inactive);
            return switch (facing) {
                case DOWN -> builder.rotationX(90).build();
                case UP -> builder.rotationX(270).build();
                case SOUTH -> builder.rotationY(180).build();
                case WEST -> builder.rotationY(270).build();
                case EAST -> builder.rotationY(90).build();
                case NORTH -> builder.build();
            };
        });
    }

    private void configuredLogisticsCore() {
        ModelFile horizontal = logisticsCoreFacingModel(
                "logistics_core", "side");
        ModelFile up = logisticsCoreFacingModel("logistics_core_up", "top");
        ModelFile down = logisticsCoreFacingModel(
                "logistics_core_down", "bottom");
        getVariantBuilder(ModBlocks.LOGISTICS_CORE.get()).forAllStates(state -> {
            Direction facing = state.getValue(LogisticsCoreBlock.FACING);
            return switch (facing) {
                case UP -> ConfiguredModel.builder().modelFile(up).build();
                case DOWN -> ConfiguredModel.builder().modelFile(down).build();
                default -> ConfiguredModel.builder()
                        .modelFile(horizontal)
                        .rotationY(switch (facing) {
                            case SOUTH -> 180;
                            case WEST -> 270;
                            case EAST -> 90;
                            default -> 0;
                        })
                        .build();
            };
        });
        simpleBlockItem(ModBlocks.LOGISTICS_CORE.get(), horizontal);
    }

    private ModelFile logisticsCoreFacingModel(String modelId, String frontFace) {
        String base = "block/gt6_import/logistics_core";
        boolean vertical = "top".equals(frontFace) || "bottom".equals(frontFace);
        String northColored = vertical
                ? base + "/colored_side"
                : base + "/colored_front_" + frontFace;
        String northOverlay = vertical
                ? base + "/overlay_side"
                : base + "/overlay_front_" + frontFace;
        String upColored = "top".equals(frontFace)
                ? base + "/colored_front_top"
                : base + "/colored_top";
        String upOverlay = "top".equals(frontFace)
                ? base + "/overlay_front_top"
                : base + "/overlay_top";
        String downColored = "bottom".equals(frontFace)
                ? base + "/colored_front_bottom"
                : base + "/colored_bottom";
        String downOverlay = "bottom".equals(frontFace)
                ? base + "/overlay_front_bottom"
                : base + "/overlay_bottom";
        return models()
                .withExistingParent(
                        modelId, modLoc("block/machine_cube_2_layer"))
                .texture("particle", modLoc(northColored))
                .texture("bot_down", modLoc(downColored))
                .texture("bot_up", modLoc(upColored))
                .texture("bot_north", modLoc(northColored))
                .texture("bot_south", modLoc(base + "/colored_side"))
                .texture("bot_west", modLoc(base + "/colored_side"))
                .texture("bot_east", modLoc(base + "/colored_side"))
                .texture("top_down", modLoc(downOverlay))
                .texture("top_up", modLoc(upOverlay))
                .texture("top_north", modLoc(northOverlay))
                .texture("top_south", modLoc(base + "/overlay_side"))
                .texture("top_west", modLoc(base + "/overlay_side"))
                .texture("top_east", modLoc(base + "/overlay_side"));
    }

    private ModelFile logisticsPartModel(String id) {
        String base = "block/gt6_import/" + id;
        return models()
                .withExistingParent(id, modLoc("block/machine_cube_2_layer"))
                .texture("particle", modLoc(base + "/colored_side"))
                .texture("bot_down", modLoc(base + "/colored_bottom"))
                .texture("bot_up", modLoc(base + "/colored_top"))
                .texture("bot_north", modLoc(base + "/colored_side"))
                .texture("bot_south", modLoc(base + "/colored_side"))
                .texture("bot_west", modLoc(base + "/colored_side"))
                .texture("bot_east", modLoc(base + "/colored_side"))
                .texture("top_down", modLoc(base + "/overlay_bottom"))
                .texture("top_up", modLoc(base + "/overlay_top"))
                .texture("top_north", modLoc(base + "/overlay_side"))
                .texture("top_south", modLoc(base + "/overlay_side"))
                .texture("top_west", modLoc(base + "/overlay_side"))
                .texture("top_east", modLoc(base + "/overlay_side"));
    }

    private void configuredMachine(String id, net.minecraft.world.level.block.Block block) {
        String textureId = machineTextureId(id);
        String shaped = shapedMachineModel(textureId);
        ModelFile model;
        if (shaped != null) {
            model = models().getExistingFile(modLoc("block/" + shaped));
        } else if (hasMachineTextures(textureId)) {
            String base = "block/machine/" + textureId;
            model = models()
                    .withExistingParent(id, modLoc("block/machine_cube_2_layer"))
                    .texture("particle", modLoc(base + "/colored/front"))
                    .texture("bot_down", modLoc(base + "/colored/bottom"))
                    .texture("bot_up", modLoc(base + "/colored/top"))
                    .texture("bot_north", modLoc(base + "/colored/front"))
                    .texture("bot_south", modLoc(base + "/colored/back"))
                    // North-front cube: GT6 LEFT is east, RIGHT is west.
                    .texture("bot_west", modLoc(base + "/colored/right"))
                    .texture("bot_east", modLoc(base + "/colored/left"))
                    .texture("top_down", modLoc(base + "/overlay/bottom"))
                    .texture("top_up", modLoc(base + "/overlay/top"))
                    .texture("top_north", modLoc(base + "/overlay/front"))
                    .texture("top_south", modLoc(base + "/overlay/back"))
                    .texture("top_west", modLoc(base + "/overlay/right"))
                    .texture("top_east", modLoc(base + "/overlay/left"));
        } else {
            model = models().orientable(
                    id,
                    mcLoc("block/copper_block"),
                    mcLoc("block/furnace_front"),
                    mcLoc("block/cut_copper"));
        }
        horizontalBlock(block, model);
        simpleBlockItem(block, model);
    }

    private void configuredFacingLitMachine(
            String id, net.minecraft.world.level.block.Block block) {
        String textureId = machineTextureId(id);
        String base = "block/machine/" + textureId;
        ModelFile model = models()
                .withExistingParent(id, modLoc("block/machine_cube_2_layer"))
                .texture("particle", modLoc(base + "/colored/front"))
                .texture("bot_down", modLoc(base + "/colored/bottom"))
                .texture("bot_up", modLoc(base + "/colored/top"))
                .texture("bot_north", modLoc(base + "/colored/front"))
                .texture("bot_south", modLoc(base + "/colored/back"))
                .texture("bot_west", modLoc(base + "/colored/right"))
                .texture("bot_east", modLoc(base + "/colored/left"))
                .texture("top_down", modLoc(base + "/overlay/bottom"))
                .texture("top_up", modLoc(base + "/overlay/top"))
                .texture("top_north", modLoc(base + "/overlay/front"))
                .texture("top_south", modLoc(base + "/overlay/back"))
                .texture("top_west", modLoc(base + "/overlay/right"))
                .texture("top_east", modLoc(base + "/overlay/left"));
        configuredFacingLit(block, model, model);
        simpleBlockItem(block, model);
    }

    /** Shared GT6 texture folders for tiered / aliased machine ids. */
    private static String machineTextureId(String id) {
        return MachineTextureProfiles.textureId(id);
    }

    private static String shapedMachineModel(String textureId) {
        return MachineTextureProfiles.shapedMachineModel(textureId);
    }

    private static boolean hasMachineTextures(String textureId) {
        return MachineTextureProfiles.hasMachineTextures(textureId);
    }

    private void registerGtTrees() {
        for (GtTreeSpecies species : GtTreeSpecies.ALL) {
            String saplingPath = "block/" + species.saplingPath();
            ModelFile sapling = models()
                    .cross(saplingPath, modLoc("block/tree/" + species.id() + "/sapling"))
                    .renderType("cutout");
            simpleBlock(ModBlocks.treeSapling(species).get(), sapling);
            ModelFile leaves = models()
                    .cubeAll(
                            "block/" + species.leavesPath(),
                            modLoc("block/tree/" + species.id() + "/leaves"))
                    .renderType("cutout_mipped");
            simpleBlock(ModBlocks.treeLeaves(species).get(), leaves);
            itemModels().getBuilder("item/" + species.leavesPath()).parent(leaves);
            ResourceLocation side = modLoc(
                    "block/gt6/iconsets/log_side_" + species.gt6TextureKey());
            ResourceLocation top = modLoc(
                    "block/gt6/iconsets/log_top_" + species.gt6TextureKey());
            ModelFile log = models().cubeColumn("block/" + species.logPath(), side, top);
            ModelFile logHorizontal = models().cubeColumnHorizontal(
                    "block/" + species.logPath() + "_horizontal", side, top);
            axisBlock(ModBlocks.treeLog(species).get(), log, logHorizontal);
            itemModels().getBuilder("item/" + species.logPath()).parent(log);
            if (species.hasHole()) {
                registerTreeHole(species, side, top);
            }
        }
    }

    private void registerTreeHole(
            GtTreeSpecies species, ResourceLocation side, ResourceLocation top) {
        String emptyName = "block/" + species.holePath();
        String filledName = "block/" + species.holePath() + "_full";
        String product = species == GtTreeSpecies.RUBBER ? "log_resin" : "log_sap";
        ModelFile empty = models().orientable(
                emptyName,
                side,
                modLoc("block/tree/" + species.id() + "/log_hole"),
                top);
        ModelFile filled = models().orientable(
                filledName,
                side,
                modLoc("block/tree/" + species.id() + "/" + product),
                top);
        getVariantBuilder(ModBlocks.treeHole(species).get()).forAllStates(state -> {
            Direction facing = state.getValue(GtTreeHoleBlock.FACING);
            boolean full = state.getValue(GtTreeHoleBlock.HAS_PRODUCT);
            return ConfiguredModel.builder()
                    .modelFile(full ? filled : empty)
                    .rotationY(((int) facing.toYRot() + 180) % 360)
                    .build();
        });
    }
}
