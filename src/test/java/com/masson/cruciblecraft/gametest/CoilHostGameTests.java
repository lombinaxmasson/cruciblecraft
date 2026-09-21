package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.content.block.DenseLeadPorts;
import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.blockentity.MatterFabricatorBlockEntity;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.content.multiblock.CoilHosts;
import com.masson.cruciblecraft.content.multiblock.MatterFabricatorStructure;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PortType;
import com.masson.cruciblecraft.energy.largedynamo.LargeDynamoBlockEntity;
import com.masson.cruciblecraft.energy.largedynamo.LargeDynamoCatalog;
import com.masson.cruciblecraft.energy.largedynamo.LargeDynamoStructure;
import com.masson.cruciblecraft.energy.lightningrod.LightningRodBlockEntity;
import com.masson.cruciblecraft.energy.lightningrod.LightningRodLogic;
import com.masson.cruciblecraft.energy.lightningrod.LightningRodStructure;
import com.masson.cruciblecraft.energy.steam.SteamTurbineStructure;
import com.masson.cruciblecraft.energy.vondagraagg.VonDaGraaggLogic;
import com.masson.cruciblecraft.fusion.FusionStructure;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** GT6 18040–18045 coils and the six host machines. Not unique-active. */
@GameTestHolder(CoilHostGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class CoilHostGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_machines_gt6_coil_hosts";
    private static final String TEMPLATE = "empty";

    private CoilHostGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void sixCoilIdentitiesAreLive(GameTestHelper helper) {
        String[] paths = {
                "multiblock/large_copper_coil",
                "multiblock/large_niobium_titanium_coil",
                "multiblock/large_nichrome_coil",
                "multiblock/large_carborundum_coil",
                "multiblock/large_osmium_coil",
                "multiblock/large_iridium_coil"
        };
        for (String path : paths) {
            MteInPlaceGameTestSupport.assertLive(
                    helper, path, MteInPlaceKind.MULTIBLOCK_PART);
            helper.assertTrue(
                    helper.getLevel()
                            .getRecipeManager()
                            .byKey(MteInPlaceGameTestSupport.id(path))
                            .isPresent(),
                    "Missing coil recipe " + path);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fusionIridiumIsTheMteIdentity(GameTestHelper helper) {
        MteInPlaceGameTestSupport.assertLive(
                helper,
                "multiblock/large_iridium_coil",
                MteInPlaceKind.MULTIBLOCK_PART);
        helper.assertTrue(
                CoilHosts.block(CoilHosts.IRIDIUM)
                        == BuiltInRegistries.BLOCK.get(
                                CoilHosts.IRIDIUM),
                "Fusion iridium coil is not the MTE block");
        helper.assertTrue(
                BuiltInRegistries.BLOCK.get(
                        MteInPlaceGameTestSupport.id("large_iridium_coil"))
                        == Blocks.AIR,
                "Dedicated large_iridium_coil is still registered");
        helper.assertTrue(
                helper.getLevel()
                        .getRecipeManager()
                        .getAllRecipesFor(RecipeType.CRAFTING)
                        .stream()
                        .noneMatch(holder -> holder.id().equals(
                                MteInPlaceGameTestSupport.id(
                                        "machines/large_iridium_coil"))),
                "Dedicated iridium coil recipe is still live");
        helper.assertTrue(
                FusionStructure.IRIDIUM_COILS == 144,
                "Fusion iridium coil count drifted from 144");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fourDynamosConvertRuToEu(GameTestHelper helper) {
        int[] metas = {17221, 17222, 17223, 17224};
        int index = 0;
        for (LargeDynamoCatalog.Profile profile : LargeDynamoCatalog.profiles()) {
            helper.assertTrue(
                    profile.sourceId() == metas[index],
                    profile.id() + " source id drifted");
            MteInPlaceGameTestSupport.assertLive(
                    helper, profile.id().getPath(), MteInPlaceKind.LARGE_DYNAMO);
            index++;
        }
        BlockPos controller = new BlockPos(1, 2, 2);
        Direction facing = Direction.WEST;
        LargeDynamoCatalog.Profile stainless = LargeDynamoCatalog.require(
                MteInPlaceGameTestSupport.id(
                        "stainless_steel/dynamo_main_housing"));
        LargeDynamoBlockEntity dynamo = placeDynamo(helper, controller, facing, stainless);
        helper.assertTrue(dynamo.formed(), "3x3x4 large dynamo did not form");
        helper.assertTrue(
                dynamo.insert(
                        EnergyType.KINETIC_ROTATION,
                        4096L,
                        1L,
                        facing,
                        false)
                        > 0L,
                "Large dynamo rejected RU");
        LargeDynamoBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(controller),
                helper.getBlockState(controller),
                dynamo);
        helper.assertTrue(
                dynamo.hatchOutputSize(
                        EnergyType.ELECTRIC, facing.getOpposite())
                        == 3072L,
                "Stainless dynamo did not emit 3072 EU");
        helper.assertTrue(
                dynamo.stored(EnergyType.KINETIC_ROTATION) == 0L,
                "Stainless dynamo did not waste inputMax after emit");
        helper.assertTrue(
                dynamo.hatchExtract(
                        EnergyType.ELECTRIC,
                        3072L,
                        1L,
                        false)
                        == 1L,
                "Large dynamo output packet was not consumable");
        helper.assertTrue(
                dynamo.hatchOutputSize(
                        EnergyType.ELECTRIC,
                        facing.getOpposite())
                        == 0L,
                "Large dynamo output packet was not consumed");
        dynamo.setStateOnOff(false);
        helper.assertTrue(
                dynamo.insert(
                        EnergyType.KINETIC_ROTATION,
                        4096L,
                        1L,
                        facing,
                        false)
                        == 0L,
                "Stopped large dynamo still accepted RU");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void lightningRodFormsAndKeepsTipContract(GameTestHelper helper) {
        MteInPlaceGameTestSupport.assertLive(
                helper,
                "tungsten/lightning_rod_electric_output",
                MteInPlaceKind.LIGHTNING_ROD);
        MteInPlaceGameTestSupport.assertLive(
                helper,
                "steel_galvanized/lightning_rod",
                MteInPlaceKind.MULTIBLOCK_PART);
        BlockPos controller = new BlockPos(1, 1, 1);
        LightningRodBlockEntity rod = placeLightning(helper, controller);
        helper.assertTrue(rod.formed(), "Lightning rod base did not form");
        helper.assertTrue(rod.size() == 1, "Lightning rod pillar size drifted");
        helper.assertTrue(
                LightningRodStructure.tip(controller, rod.size()).getY() == 6,
                "Relative tip Y drifted from controllerY+4+size");
        helper.assertTrue(
                !LightningRodLogic.tipHighEnough(
                        LightningRodStructure.tip(controller, rod.size()).getY()),
                "GameTest relative tip must stay below Y 100");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void matterFabricatorOsmiumCoilsArePorts(GameTestHelper helper) {
        MteInPlaceGameTestSupport.assertLive(
                helper,
                "lead/large_matter_fabricator",
                MteInPlaceKind.MATTER_FABRICATOR);
        BlockPos controller = new BlockPos(2, 1, 4);
        Direction facing = Direction.NORTH;
        MatterFabricatorBlockEntity fabricator =
                placeMassfab(helper, controller, facing);
        helper.assertTrue(
                fabricator.structureValid(),
                "5x5x5 matter fabricator did not form");
        BlockPos origin = MatterFabricatorStructure.origin(
                controller, facing);
        MteInPlaceBlockEntity coil = helper.getBlockEntity(
                origin.offset(1, 1, 1));
        helper.assertTrue(
                coil != null && DenseLeadPorts.accepts(
                        coil.spec(), PortType.ITEM_FLUID_ENERGY),
                "Osmium coil is not ONLY_ITEM_FLUID_ENERGY");
        helper.assertTrue(
                DenseLeadPorts.items(coil) != null,
                "Osmium coil did not forward item IO");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void vonDaGraaggIdentitiesAndBind8(GameTestHelper helper) {
        MteInPlaceGameTestSupport.assertLive(
                helper,
                "steel_galvanized/von_da_graagg_generator",
                MteInPlaceKind.VON_DA_GRAAGG);
        helper.assertTrue(
                VonDaGraaggLogic.range(4096L, true) == 255,
                "bind8(4096/16) is not 255");
        helper.succeed();
    }

    private static LargeDynamoBlockEntity placeDynamo(
            GameTestHelper helper,
            BlockPos controller,
            Direction facing,
            LargeDynamoCatalog.Profile profile) {
        MteInPlaceBlock housing = ModBlocks.mteInPlaceBlocksById()
                .get(profile.id())
                .get();
        Block wall = CoilHosts.block(profile.wallId());
        Block coil = CoilHosts.block(CoilHosts.COPPER);
        helper.setBlock(
                controller,
                housing.defaultBlockState().setValue(MteInPlaceBlock.FACING, facing));
        SteamTurbineStructure.Cell cell = SteamTurbineStructure.cell(controller, facing);
        int coils = 0;
        int walls = 0;
        for (int x = cell.minX(); x <= cell.maxX(); x++) {
            for (int y = cell.minY(); y <= cell.maxY(); y++) {
                for (int z = cell.minZ(); z <= cell.maxZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (pos.equals(controller)) {
                        continue;
                    }
                    if (LargeDynamoStructure.interior(controller, facing, pos)) {
                        helper.setBlock(pos, coil.defaultBlockState());
                        coils++;
                    } else {
                        helper.setBlock(pos, wall.defaultBlockState());
                        walls++;
                    }
                }
            }
        }
        helper.assertTrue(
                coils == LargeDynamoStructure.COILS
                        && walls == LargeDynamoStructure.WALLS,
                "Dynamo coil/wall counts drifted");
        LargeDynamoBlockEntity be = helper.getBlockEntity(controller);
        helper.assertTrue(be != null, "Missing large dynamo");
        LargeDynamoBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(controller),
                helper.getBlockState(controller),
                be);
        return be;
    }

    private static LightningRodBlockEntity placeLightning(
            GameTestHelper helper, BlockPos controller) {
        MteInPlaceBlock housing = ModBlocks.mteInPlaceBlocksById()
                .get(MteInPlaceGameTestSupport.id(
                        "tungsten/lightning_rod_electric_output"))
                .get();
        Block wall = CoilHosts.block(CoilHosts.TUNGSTEN_WALL);
        Block coil = CoilHosts.block(CoilHosts.NIOBIUM_TITANIUM);
        Block rod = CoilHosts.block(CoilHosts.LIGHTNING_ROD_PART);
        helper.setBlock(controller, housing.defaultBlockState());
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (!(i == 0 && j == 0)) {
                    helper.setBlock(controller.offset(i, 0, j), wall);
                }
                helper.setBlock(controller.offset(i, 1, j), coil);
                helper.setBlock(controller.offset(i, 2, j), wall);
                helper.setBlock(controller.offset(i, 3, j), coil);
                helper.setBlock(controller.offset(i, 4, j), wall);
            }
        }
        helper.setBlock(controller.offset(0, LightningRodStructure.BASE_LAYERS, 0), rod);
        LightningRodBlockEntity be = helper.getBlockEntity(controller);
        helper.assertTrue(be != null, "Missing lightning rod");
        LightningRodBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(controller),
                helper.getBlockState(controller),
                be);
        return be;
    }

    private static MatterFabricatorBlockEntity placeMassfab(
            GameTestHelper helper, BlockPos controller, Direction facing) {
        MteInPlaceBlock housing = ModBlocks.mteInPlaceBlocksById()
                .get(MteInPlaceGameTestSupport.id("lead/large_matter_fabricator"))
                .get();
        helper.setBlock(
                controller,
                housing.defaultBlockState().setValue(MteInPlaceBlock.FACING, facing));
        Block lead = CoilHosts.block(CoilHosts.DENSE_LEAD);
        Block coil = CoilHosts.block(CoilHosts.OSMIUM);
        BlockPos origin = MatterFabricatorStructure.origin(controller, facing);
        for (int y = 0; y <= 4; y++) {
            for (int x = 0; x <= 4; x++) {
                for (int z = 0; z <= 4; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (pos.equals(controller)) {
                        continue;
                    }
                    boolean inner = x >= 1 && x <= 3 && z >= 1 && z <= 3 && y >= 1 && y <= 3;
                    boolean center = x == 2 && y == 2 && z == 2;
                    if (center) {
                        helper.setBlock(pos, Blocks.AIR);
                    } else if (inner) {
                        helper.setBlock(pos, coil);
                    } else {
                        helper.setBlock(pos, lead);
                    }
                }
            }
        }
        for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 4; z++) {
                if (x != 0 && x != 4 && z != 0 && z != 4) {
                    continue;
                }
                helper.setBlock(
                        origin.offset(x, 5, z),
                        ModBlocks.VENTILATION_UNIT.get());
            }
        }
        helper.setBlock(
                origin.offset(2, 5, 2),
                ModBlocks.VERSATILE_PROCESSOR_UNIT.get());
        int[][] innerRing = {
                {1, 1}, {2, 1}, {3, 1},
                {1, 2}, {3, 2},
                {1, 3}, {2, 3}, {3, 3}
        };
        for (int i = 0; i < innerRing.length; i++) {
            helper.setBlock(
                    origin.offset(innerRing[i][0], 5, innerRing[i][1]),
                    i < 4
                            ? ModBlocks.CONTROL_PROCESSOR_UNIT.get()
                            : ModBlocks.CONVERSION_PROCESSOR_UNIT.get());
        }
        MatterFabricatorBlockEntity be = helper.getBlockEntity(controller);
        helper.assertTrue(be != null, "Missing matter fabricator");
        MatterFabricatorBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(controller),
                helper.getBlockState(controller),
                be);
        return be;
    }
}
