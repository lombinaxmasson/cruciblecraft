package com.masson.cruciblecraft.datagen;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.block.AbstractPipeBlock;
import com.masson.cruciblecraft.content.block.BellowsBlock;
import com.masson.cruciblecraft.content.block.CableBlock;
import com.masson.cruciblecraft.content.block.CeramicMoldBlock;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModMachineVariants;

import net.minecraft.data.PackOutput;
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
        var firebrickTexture = mcLoc("block/bricks");
        simpleBlockWithItem(
                ModBlocks.FIREBRICK.get(),
                models().cubeAll("firebrick", firebrickTexture));
        simpleBlockWithItem(
                ModBlocks.FIREBOX.get(),
                models().cubeAll("firebox", firebrickTexture));
        simpleBlockWithItem(
                ModBlocks.CRUCIBLE.get(),
                models().getExistingFile(modLoc("block/crucible")));
        var anvil = models().getExistingFile(modLoc("block/anvil"));
        horizontalBlock(ModBlocks.ANVIL.get(), anvil);
        simpleBlockItem(ModBlocks.ANVIL.get(), anvil);
        simpleBlockWithItem(
                ModBlocks.COKE_OVEN.get(),
                models().cubeAll("coke_oven", firebrickTexture));
        simpleBlockWithItem(
                ModBlocks.MULTIBLOCK_CASING.get(),
                models().cubeAll(
                        "multiblock_casing",
                        mcLoc("block/iron_block")));
        simpleBlockWithItem(
                ModBlocks.MULTIBLOCK_ITEM_FLUID_PORT.get(),
                models().cubeAll(
                        "multiblock_item_fluid_port",
                        mcLoc("block/copper_block")));
        simpleBlockWithItem(
                ModBlocks.MULTIBLOCK_ENERGY_INPUT_PORT.get(),
                models().cubeAll(
                        "multiblock_energy_input_port",
                        mcLoc("block/redstone_block")));
        configuredMachine(
                "large_centrifuge", ModBlocks.LARGE_CENTRIFUGE.get());
        configuredMachine("sluice", ModBlocks.SLUICE.get());
        configuredMachine("bath", ModBlocks.BATH.get());
        configuredMachine("centrifuge", ModBlocks.CENTRIFUGE.get());
        configuredMachine(
                "steel_centrifuge",
                ModBlocks.STEEL_CENTRIFUGE.get());
        configuredMachine(
                "titanium_centrifuge",
                ModBlocks.TITANIUM_CENTRIFUGE.get());
        configuredMachine("sifter", ModBlocks.SIFTER.get());
        configuredMachine("steel_sifter", ModBlocks.STEEL_SIFTER.get());
        configuredMachine(
                "titanium_sifter",
                ModBlocks.TITANIUM_SIFTER.get());
        ModMachineVariants.T16_SELECTED.forEach(variant ->
                configuredMachine(
                        variant.id().getPath(),
                        ModBlocks.configuredProcessingBlock(variant)));
        ModMachineVariants.T17_SELECTED.forEach(variant ->
                configuredMachine(
                        variant.id().getPath(),
                        ModBlocks.configuredProcessingBlock(variant)));
        configuredMachine("mortar", ModBlocks.MORTAR.get());
        configuredMachine("extruder", ModBlocks.EXTRUDER.get());
        configuredMachine("cutter", ModBlocks.CUTTER.get());
        configuredMachine("rollbender", ModBlocks.ROLLBENDER.get());
        configuredMachine("bender", ModBlocks.BENDER.get());
        configuredMachine("assembler", ModBlocks.ASSEMBLER.get());
        configuredMachine("welder", ModBlocks.WELDER.get());
        configuredMachine("electrolyzer", ModBlocks.ELECTROLYZER.get());
        configuredMachine(
                "aluminium_electrolyzer",
                ModBlocks.ALUMINIUM_ELECTROLYZER.get());
        configuredMachine(
                "stainless_steel_electrolyzer",
                ModBlocks.STAINLESS_STEEL_ELECTROLYZER.get());
        configuredMachine("mixer", ModBlocks.MIXER.get());
        configuredMachine("autoclave", ModBlocks.AUTOCLAVE.get());
        configuredMachine("compressor", ModBlocks.COMPRESSOR.get());
        configuredMachine("generifier", ModBlocks.GENERIFIER.get());
        configuredMachine("electric_motor", ModBlocks.ELECTRIC_MOTOR.get());
        configuredMachine(
                "rotational_gearbox",
                ModBlocks.ROTATIONAL_GEARBOX.get());
        simpleBlockWithItem(
                ModBlocks.ROTATIONAL_AXLE.get(),
                models().cubeAll(
                        "rotational_axle",
                        mcLoc("block/iron_block")));
        simpleBlockWithItem(
                ModBlocks.FLUID_DEPOSIT_EXTRACTOR.get(),
                models().cubeAll(
                        "fluid_deposit_extractor",
                        mcLoc("block/cut_copper")));
        configuredMachine("fuel_engine", ModBlocks.FUEL_ENGINE.get());
        configuredMachine(
                "burning_gas_generator",
                ModBlocks.BURNING_GAS_GENERATOR.get());
        simpleBlock(
                ModBlocks.GAS_CLOUD.get(),
                models().cubeAll(
                        "gas_cloud",
                        mcLoc("block/white_stained_glass")));
        var bellows = models().orientable(
                "bellows",
                mcLoc("block/oak_planks"),
                mcLoc("block/piston_top"),
                mcLoc("block/oak_planks"));
        var activeBellows = models().orientable(
                "bellows_active",
                mcLoc("block/oak_planks"),
                mcLoc("block/piston_top_sticky"),
                mcLoc("block/oak_planks"));
        horizontalBlock(
                ModBlocks.BELLOWS.get(),
                state -> state.getValue(BellowsBlock.ACTIVE)
                        ? activeBellows
                        : bellows);
        simpleBlockItem(ModBlocks.BELLOWS.get(), bellows);
        var emptyMold = models().slab(
                "ceramic_mold",
                firebrickTexture,
                firebrickTexture,
                firebrickTexture);
        var filledMold = models().slab(
                "ceramic_mold_filled",
                firebrickTexture,
                mcLoc("block/magma"),
                firebrickTexture);
        getVariantBuilder(ModBlocks.CERAMIC_MOLD.get()).forAllStates(state ->
                ConfiguredModel.builder()
                        .modelFile(state.getValue(CeramicMoldBlock.FILLED) ? filledMold : emptyMold)
                        .build());
        registerConductors();
        registerPipes();
    }

    private void registerConductors() {
        java.util.Map<String, ModelFile> cores = new java.util.LinkedHashMap<>();
        java.util.Map<String, ModelFile> arms = new java.util.LinkedHashMap<>();
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
        });
    }

    private ModelFile conductorCore(String specification) {
        float width = conductorWidth(specification);
        float minimum = 8.0F - width / 2.0F;
        float maximum = 8.0F + width / 2.0F;
        return models()
                .getBuilder(
                        "conductor/"
                                + specification.toLowerCase(
                                        java.util.Locale.ROOT)
                                + "_core")
                .texture("particle", mcLoc("block/white_concrete"))
                .texture("all", mcLoc("block/white_concrete"))
                .element()
                .from(minimum, minimum, minimum)
                .to(maximum, maximum, maximum)
                .allFaces((direction, face) ->
                        face.texture("#all").tintindex(0))
                .end();
    }

    private ModelFile conductorArm(String specification) {
        float width = conductorWidth(specification);
        float minimum = 8.0F - width / 2.0F;
        float maximum = 8.0F + width / 2.0F;
        return models()
                .getBuilder(
                        "conductor/"
                                + specification.toLowerCase(
                                        java.util.Locale.ROOT)
                                + "_arm")
                .texture("particle", mcLoc("block/white_concrete"))
                .texture("all", mcLoc("block/white_concrete"))
                .element()
                .from(minimum, minimum, 0.0F)
                .to(maximum, maximum, minimum)
                .allFaces((direction, face) ->
                        face.texture("#all").tintindex(0))
                .end();
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
        ModBlocks.pipeBlocks().forEach(holder -> {
            AbstractPipeBlock block = holder.get();
            String modelKey = block.pipe().kind().name().toLowerCase(
                    java.util.Locale.ROOT) + "_" + block.pipe().width();
            ModelFile core = cores.computeIfAbsent(
                    modelKey,
                    ignored -> pipePart(
                            modelKey, block.pipe().width(), false));
            ModelFile arm = arms.computeIfAbsent(
                    modelKey,
                    ignored -> pipePart(
                            modelKey, block.pipe().width(), true));
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
        });
    }

    private ModelFile pipePart(
            String key, int width, boolean arm) {
        float minimum = 8.0F - width / 2.0F;
        float maximum = 8.0F + width / 2.0F;
        return models()
                .getBuilder(
                        "pipe/" + key + (arm ? "_arm" : "_core"))
                .texture("particle", mcLoc("block/white_concrete"))
                .texture("all", mcLoc("block/white_concrete"))
                .element()
                .from(
                        minimum,
                        minimum,
                        arm ? 0.0F : minimum)
                .to(
                        maximum,
                        maximum,
                        arm ? minimum : maximum)
                .allFaces((direction, face) ->
                        face.texture("#all").tintindex(0))
                .end();
    }

    private void configuredMachine(String id, net.minecraft.world.level.block.Block block) {
        String textureId = machineTextureId(id);
        ModelFile model;
        if (hasMachineTextures(textureId)) {
            String base = "block/machine/" + textureId;
            model = models()
                    .withExistingParent(id, modLoc("block/machine_cube_2_layer"))
                    .texture("particle", modLoc(base + "/overlay/front"))
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
            case "drying", "invar_drying", "titanium_drying" -> "dryer";
            case "invar_smelter", "titanium_smelter" -> "smelter";
            default -> id;
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
                    "burning_gas_generator" -> true;
            default -> false;
        };
    }
}
