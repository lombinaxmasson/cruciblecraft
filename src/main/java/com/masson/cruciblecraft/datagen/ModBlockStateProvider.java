package com.masson.cruciblecraft.datagen;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.block.AbstractPipeBlock;
import com.masson.cruciblecraft.content.block.BellowsBlock;
import com.masson.cruciblecraft.content.block.CableBlock;
import com.masson.cruciblecraft.content.block.CeramicMoldBlock;
import com.masson.cruciblecraft.content.block.FireboxBlock;
import com.masson.cruciblecraft.content.block.FuelGeneratorBlock;
import com.masson.cruciblecraft.content.block.HopperBlock;
import com.masson.cruciblecraft.content.item.GtBlockObjectCatalog;
import com.masson.cruciblecraft.content.item.GtStoneCatalog;
import com.masson.cruciblecraft.content.storage.StorageVariantCatalog;
import com.masson.cruciblecraft.logistics.hopper.HopperKind;
import com.masson.cruciblecraft.logistics.hopper.HopperVariantCatalog;
import com.masson.cruciblecraft.machine.processing.MachineTierCatalog;
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
    public ModBlockStateProvider(
            PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, CrucibleCraft.MODID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        var firebrick =
                models().getExistingFile(modLoc("block/firebrick_gt6"));
        simpleBlockWithItem(
                ModBlocks.FIREBRICK.get(),
                firebrick);
        var fireboxUnlit =
                models().getExistingFile(modLoc("block/firebox_unlit"));
        var fireboxLit = models().getExistingFile(modLoc("block/firebox_lit"));
        getVariantBuilder(ModBlocks.FIREBOX.get()).forAllStates(state -> {
            int rotation = switch (state.getValue(FireboxBlock.FACING)) {
                case SOUTH -> 180;
                case WEST -> 270;
                case EAST -> 90;
                default -> 0;
            };
            return ConfiguredModel.builder()
                    .modelFile(state.getValue(FireboxBlock.LIT)
                            ? fireboxLit
                            : fireboxUnlit)
                    .rotationY(rotation)
                    .build();
        });
        simpleBlockItem(ModBlocks.FIREBOX.get(), fireboxUnlit);
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
        ModMachineVariants.ALL.forEach(variant ->
                configuredMachine(
                        variant.id().getPath(),
                        ModBlocks.configuredProcessingBlock(variant)));
        configuredMachine("electric_motor", ModBlocks.ELECTRIC_MOTOR.get());
        configuredMachine(
                "rotational_gearbox",
                ModBlocks.ROTATIONAL_GEARBOX.get());
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
        configuredFuelGenerator("fuel_engine", ModBlocks.FUEL_ENGINE.get());
        configuredFuelGenerator(
                "burning_gas_generator",
                ModBlocks.BURNING_GAS_GENERATOR.get());
        simpleBlock(
                ModBlocks.GAS_CLOUD.get(),
                models()
                        .cubeAll("gas_cloud", modLoc("block/gas_cloud"))
                        .renderType("translucent"));
        var bellows = models().orientable(
                "bellows",
                modLoc("block/bellows_side"),
                modLoc("block/bellows_front"),
                modLoc("block/bellows_side"));
        var activeBellows = models().orientable(
                "bellows_active",
                modLoc("block/bellows_side"),
                modLoc("block/bellows_front_active"),
                modLoc("block/bellows_side"));
        horizontalBlock(
                ModBlocks.BELLOWS.get(),
                state -> state.getValue(BellowsBlock.ACTIVE)
                        ? activeBellows
                        : bellows);
        simpleBlockItem(ModBlocks.BELLOWS.get(), bellows);
        var emptyMold = models().getExistingFile(modLoc("block/ceramic_mold"));
        var filledMold = models().getExistingFile(modLoc("block/ceramic_mold_filled"));
        getVariantBuilder(ModBlocks.CERAMIC_MOLD.get()).forAllStates(state ->
                ConfiguredModel.builder()
                        .modelFile(state.getValue(CeramicMoldBlock.FILLED) ? filledMold : emptyMold)
                        .build());
        simpleBlockItem(ModBlocks.CERAMIC_MOLD.get(), emptyMold);
        registerConductors();
        registerPipes();
        registerHoppers();
        registerStorage();
        registerGtStones();
        registerGtBlockObjects();
    }

    private void registerGtStones() {
        for (GtStoneCatalog.Variant variant : GtStoneCatalog.variants()) {
            var block = ModBlocks.gtStoneBlocksById().get(variant.id()).get();
            ResourceLocation texture = variant.textureLocation();
            if (variant.slab()) {
                String path = variant.registryPath();
                var doubleslab = models().cubeAll(path + "_double", texture);
                var bottom = models().slab(path + "_bottom", texture, texture, texture);
                var top = models().slabTop(path + "_top", texture, texture, texture);
                slabBlock((SlabBlock) block, bottom, top, doubleslab);
                itemModels().getBuilder(path).parent(bottom);
            } else {
                simpleBlockWithItem(
                        block,
                        models().cubeAll(variant.registryPath(), texture));
            }
        }
    }

    private void registerGtBlockObjects() {
        for (GtBlockObjectCatalog.Variant variant : GtBlockObjectCatalog.variants()) {
            var block = ModBlocks.gtBlockObjectBlocksById().get(variant.id()).get();
            ResourceLocation texture = variant.textureLocation();
            String path = variant.registryPath();
            if (variant.slab()) {
                var doubleslab = models().cubeAll(path + "_double", texture);
                var bottom = models().slab(path + "_bottom", texture, texture, texture);
                var top = models().slabTop(path + "_top", texture, texture, texture);
                slabBlock((SlabBlock) block, bottom, top, doubleslab);
                itemModels().getBuilder(path).parent(bottom);
            } else if (variant.log() || variant.bale()) {
                axisBlock((RotatedPillarBlock) block, texture, texture);
                itemModels().getBuilder(path).parent(models().cubeColumn(path, texture, texture));
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
                simpleBlockWithItem(block, models().cubeAll(path, texture));
            }
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
                        modLoc("block/material/wire_end"),
                        conductorOverlay(specification));
            }
        });
    }

    private ModelFile conductorCore(String specification) {
        return pipeCube(
                "conductor/"
                        + specification.toLowerCase(java.util.Locale.ROOT)
                        + "_core",
                conductorWidth(specification),
                false,
                conductorSide(specification),
                modLoc("block/material/wire_end"),
                null);
    }

    private ModelFile conductorArm(String specification) {
        return pipeCube(
                "conductor/"
                        + specification.toLowerCase(java.util.Locale.ROOT)
                        + "_arm",
                conductorWidth(specification),
                true,
                conductorSide(specification),
                modLoc("block/material/wire_end"),
                conductorOverlay(specification));
    }

    private ResourceLocation conductorSide(String specification) {
        return "wireGt01".equals(specification)
                ? modLoc("block/material/wire_side")
                : modLoc("block/cable/insulation_5");
    }

    private ResourceLocation conductorOverlay(String specification) {
        return switch (specification) {
            case "cableGt01" -> modLoc("block/cable/insulation_0");
            case "cableGt02" -> modLoc("block/cable/insulation_1");
            case "cableGt04" -> modLoc("block/cable/insulation_2");
            case "cableGt08" -> modLoc("block/cable/insulation_3");
            case "cableGt12" -> modLoc("block/cable/insulation_4");
            default -> null;
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
        return switch (specification) {
            case "wireGt01" -> 2.0F;
            case "cableGt01" -> 4.0F;
            case "cableGt02" -> 6.0F;
            case "cableGt04" -> 8.0F;
            case "cableGt08" -> 10.0F;
            case "cableGt12" -> 12.0F;
            default -> throw new IllegalArgumentException(
                    "Unsupported conductor specification " + specification);
        };
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
                    java.util.Locale.ROOT) + "_" + block.pipe().width();
            ResourceLocation side = modLoc("block/pipe/pipe_side");
            ResourceLocation end = pipeEndTexture(block.pipe().width());
            ModelFile core = cores.computeIfAbsent(
                    modelKey,
                    ignored -> pipeCube(
                            "pipe/" + modelKey + "_core",
                            block.pipe().width(),
                            false,
                            side,
                            end,
                            null));
            ModelFile arm = arms.computeIfAbsent(
                    modelKey,
                    ignored -> pipeCube(
                            "pipe/" + modelKey + "_arm",
                            block.pipe().width(),
                            true,
                            side,
                            end,
                            null));
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
                        end,
                        null);
            }
        });
    }

    private ResourceLocation pipeEndTexture(int width) {
        return switch (width) {
            case 4 -> modLoc("block/pipe/pipe_tiny_in");
            case 6 -> modLoc("block/pipe/pipe_small_in");
            case 8 -> modLoc("block/pipe/pipe_normal_in");
            case 12 -> modLoc("block/pipe/pipe_large_in");
            case 16 -> modLoc("block/pipe/pipe_huge_in");
            default -> throw new IllegalArgumentException(
                    "Unsupported pipe width " + width);
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
        if (arm && overlay != null) {
            addPipeElement(
                    builder,
                    minimum,
                    minimum,
                    0.0F,
                    maximum,
                    maximum,
                    minimum,
                    Direction.NORTH,
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
            overlayElement.face(Direction.NORTH)
                    .texture("#overlay")
                    .tintindex(0)
                    .end();
            overlayElement.face(Direction.SOUTH)
                    .texture("#overlay")
                    .tintindex(0)
                    .end();
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
            if (overlay && !cap) {
                continue;
            }
            element.face(direction)
                    .texture(overlay ? "#overlay" : cap ? "#end" : "#side")
                    .tintindex(0)
                    .end();
        }
        element.end();
    }

    private void configuredFuelGenerator(
            String id, FuelGeneratorBlock block) {
        ModelFile inactive = models().getExistingFile(modLoc("block/" + id));
        ModelFile active = models().getExistingFile(
                modLoc("block/" + id + "_active"));
        getVariantBuilder(block).forAllStates(state -> {
            int rotation = switch (state.getValue(FuelGeneratorBlock.FACING)) {
                case SOUTH -> 180;
                case WEST -> 270;
                case EAST -> 90;
                default -> 0;
            };
            return ConfiguredModel.builder()
                    .modelFile(state.getValue(FuelGeneratorBlock.LIT)
                            ? active
                            : inactive)
                    .rotationY(rotation)
                    .build();
        });
        simpleBlockItem(block, inactive);
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
                    .texture("bot_west", modLoc(base + "/colored/left"))
                    .texture("bot_east", modLoc(base + "/colored/right"))
                    .texture("top_down", modLoc(base + "/overlay/bottom"))
                    .texture("top_up", modLoc(base + "/overlay/top"))
                    .texture("top_north", modLoc(base + "/overlay/front"))
                    .texture("top_south", modLoc(base + "/overlay/back"))
                    .texture("top_west", modLoc(base + "/overlay/left"))
                    .texture("top_east", modLoc(base + "/overlay/right"));
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

    /** Shared GT6 texture folders for tiered / aliased machine ids. */
    private static String machineTextureId(String id) {
        String catalogProfile = MachineTierCatalog.textureProfile(id);
        if (!catalogProfile.equals(id)) {
            return catalogProfile;
        }
        return switch (id) {
            case "steel_centrifuge", "titanium_centrifuge" -> "centrifuge";
            case "steel_sifter", "titanium_sifter" -> "sifter";
            case "steel_lathe", "titanium_lathe" -> "lathe";
            case "steel_rollingmill", "titanium_rollingmill" -> "rollingmill";
            case "steel_wiremill", "titanium_wiremill" -> "wiremill";
            case "steel_shredder", "titanium_shredder" -> "shredder";
            case "steel_press", "titanium_press" -> "press";
            case "aluminium_electrolyzer", "stainless_steel_electrolyzer" ->
                    "electrolyzer";
            case "invar_distillery", "titanium_distillery" -> "distillery";
            case "distillation_tower" -> "distillery";
            case "large_boiler" -> "boiler";
            case "tank_3x3x3" -> "tank_3x3x3";
            case "large_crucible" -> "coke_oven";
            case "drying", "invar_drying", "titanium_drying" -> "dryer";
            case "invar_smelter", "titanium_smelter" -> "smelter";
            default -> MachineTierCatalog.textureProfile(id);
        };
    }

    private static String shapedMachineModel(String textureId) {
        return switch (textureId) {
            case "mortar", "sifter", "bath", "smelter" -> textureId;
            default -> null;
        };
    }

    private static boolean hasMachineTextures(String textureId) {
        return switch (textureId) {
            case "large_centrifuge",
                    "sluice",
                    "bath",
                    "centrifuge",
                    "shredder",
                    "sifter",
                    "smelter",
                    "extruder",
                    "cutter",
                    "lathe",
                    "rollingmill",
                    "rollbender",
                    "bender",
                    "wiremill",
                    "assembler",
                    "welder",
                    "press",
                    "electrolyzer",
                    "mixer",
                    "distillery",
                    "autoclave",
                    "dryer",
                    "compressor",
                    "generifier",
                    "electric_motor",
                    "rotational_gearbox",
                    "fuel_engine",
                    "burning_gas_generator",
                    "boiler",
                    "tank_3x3x3",
                    "mortar",
                    "coke_oven",
                    "bronze_crusher" -> true;
            default -> false;
        };
    }
}
