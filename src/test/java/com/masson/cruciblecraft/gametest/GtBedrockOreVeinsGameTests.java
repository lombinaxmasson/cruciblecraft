package com.masson.cruciblecraft.gametest;

import java.util.List;
import java.util.Random;

import com.masson.cruciblecraft.content.block.BedrockOreBlock;
import com.masson.cruciblecraft.content.block.GtIndicatorFlowerBlock;
import com.masson.cruciblecraft.content.block.GtSurfaceRockBlock;
import com.masson.cruciblecraft.content.block.SpringLiquidBlock;
import com.masson.cruciblecraft.content.blockentity.BedrockOreBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FluidSpringBlockEntity;
import com.masson.cruciblecraft.content.blockentity.GtSurfaceRockBlockEntity;
import com.masson.cruciblecraft.energy.bedrockdrill.BedrockDrillStructure;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModFeatures;
import com.masson.cruciblecraft.worldgen.BedrockOreCatalog;
import com.masson.cruciblecraft.worldgen.BedrockOreVeins;
import com.masson.cruciblecraft.worldgen.DropsSmallOre;
import com.masson.cruciblecraft.worldgen.FluidSpringCatalog;
import com.masson.cruciblecraft.worldgen.FluidSpringVeins;
import com.masson.cruciblecraft.worldgen.IndicatorFlower;

import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated GT6 WorldgenOresBedrock gate. Run with
 * {@code -PgameTestNamespaces=cruciblecraft_wave_worldgen_gt_bedrock_ores}.
 */
@GameTestHolder(GtBedrockOreVeinsGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class GtBedrockOreVeinsGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_worldgen_gt_bedrock_ores";
    private static final String TEMPLATE = "empty";

    private GtBedrockOreVeinsGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void featureAndCatalogAreRegistered(GameTestHelper helper) {
        helper.assertTrue(
                ModFeatures.BEDROCK_ORE_VEINS.get() != null,
                "bedrock_ore_veins feature missing");
        helper.assertTrue(
                ModFeatures.LAVA_FLUID_SPRINGS.get() != null,
                "lava_fluid_springs feature missing");
        helper.assertTrue(
                BuiltInRegistries.BLOCK
                        .getKey(ModBlocks.GT_BEDROCK_ORE.get())
                        .getPath()
                        .equals("gt_bedrock_ore"),
                "gt_bedrock_ore registry missing");
        helper.assertTrue(
                BedrockOreCatalog.VEINS.size() == 40,
                "no-mod WorldgenOresBedrock count drifted");
        helper.assertTrue(
                FluidSpringCatalog.VEINS.size() == 8,
                "no-mod WorldgenFluidSpring count drifted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void generateVeinPlacesUnbreakableFloorOre(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        int minX = origin.getX();
        int minZ = origin.getZ();
        int floorY = helper.absolutePos(new BlockPos(0, 1, 0)).getY();
        for (int tX = 5; tX < 11; tX++) {
            for (int tZ = 5; tZ < 11; tZ++) {
                helper.setBlock(new BlockPos(tX, 1, tZ), Blocks.BEDROCK);
            }
        }
        helper.setBlock(new BlockPos(8, 1, 8), Blocks.BEDROCK);
        boolean generated = BedrockOreVeins.generateVein(
                helper.getLevel(),
                minX,
                minZ,
                floorY,
                "gold",
                new Random(1L));
        helper.assertTrue(generated, "generateVein must succeed on bedrock floor");
        boolean found = false;
        String material = "";
        for (int dx = 5; dx < 11; dx++) {
            for (int dz = 5; dz < 11; dz++) {
                BlockPos pos = helper.absolutePos(new BlockPos(dx, 1, dz));
                if (helper.getLevel().getBlockState(pos).getBlock()
                        instanceof BedrockOreBlock) {
                    found = true;
                    BlockEntity blockEntity = helper.getLevel().getBlockEntity(pos);
                    if (blockEntity instanceof BedrockOreBlockEntity ore) {
                        material = ore.materialId();
                    }
                }
            }
        }
        helper.assertTrue(found, "must place oreBedrock / oreSmallBedrock on the floor");
        helper.assertTrue("gold".equals(material), "floor ore must store gold");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void drillProbesLargeOreTwice(GameTestHelper helper) {
        BlockPos controllerRel = new BlockPos(8, 7, 8);
        BlockPos controller = helper.absolutePos(controllerRel);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos oreRel = controllerRel.offset(dx, -5, dz);
                helper.setBlock(oreRel, ModBlocks.GT_BEDROCK_ORE.get());
                BedrockOreBlock.placeMaterial(
                        helper.getLevel(),
                        helper.absolutePos(oreRel),
                        "gold");
            }
        }
        List<String> probed = BedrockDrillStructure.probeMaterials(
                helper.getLevel(), controller);
        helper.assertTrue(probed.size() == 18, "large bedrock ore counts twice");
        helper.assertTrue(
                probed.stream().allMatch("gold"::equals),
                "probe must read gold");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void indicatorPebbleDropsRawOre(GameTestHelper helper) {
        BlockPos rel = new BlockPos(8, 3, 8);
        helper.setBlock(rel.below(), Blocks.GRASS_BLOCK);
        helper.setBlock(rel, ModBlocks.GT_SURFACE_ROCK.get());
        BlockPos pos = helper.absolutePos(rel);
        if (helper.getLevel().getBlockEntity(pos)
                instanceof GtSurfaceRockBlockEntity rock) {
            rock.setMaterial("gold");
            rock.setRawOre(true);
        }
        ItemStack loot = GtSurfaceRockBlock.loot(
                helper.getLevel().getBlockState(pos),
                helper.getLevel().getBlockEntity(pos));
        helper.assertTrue(
                loot.is(net.minecraft.world.item.Items.RAW_GOLD)
                        || loot.getDescriptionId().contains("raw_gold")
                        || loot.getDescriptionId().contains("raw_ore"),
                "bedrock indicator must drop oreRaw");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void netherrackMuffinUsesHostedOre(GameTestHelper helper) {
        BlockPos rel = new BlockPos(8, 4, 8);
        helper.setBlock(rel, Blocks.NETHERRACK);
        boolean placed = BedrockOreVeins.placeNormalOre(
                helper.getLevel(), helper.absolutePos(rel), "gold");
        helper.assertTrue(placed, "netherrack muffin must place large ore");
        helper.assertTrue(
                helper.getBlockState(rel).getBlock() == ModBlocks.GT_HOSTED_ORE.get(),
                "netherrack host must be gt_hosted_ore, not stone ore");
        helper.assertTrue(
                helper.getBlockState(rel).getValue(
                        com.masson.cruciblecraft.content.block.GtHostedOreBlock.HOST)
                        == com.masson.cruciblecraft.content.block.OreStoneHost.NETHERRACK,
                "hosted ore host must be netherrack");
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(rel));
        helper.assertTrue(
                blockEntity instanceof BedrockOreBlockEntity ore
                        && "gold".equals(ore.materialId()),
                "hosted netherrack ore must store gold");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void missingGeneratesOreUsesHostedFallback(GameTestHelper helper) {
        BlockPos rel = new BlockPos(8, 4, 8);
        helper.setBlock(rel, Blocks.DEEPSLATE);
        boolean placed = BedrockOreVeins.placeNormalOre(
                helper.getLevel(), helper.absolutePos(rel), "void_quartz");
        helper.assertTrue(placed, "void_quartz muffin must place hosted ore");
        helper.assertTrue(
                helper.getBlockState(rel).getBlock() == ModBlocks.GT_HOSTED_ORE.get(),
                "materials without generates_ore use gt_hosted_ore");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void desertFlowerStaysOnSand(GameTestHelper helper) {
        BlockPos rel = new BlockPos(8, 3, 8);
        var flower = GtIndicatorFlowerBlock.FLOWER;
        var desert = ModBlocks.GT_INDICATOR_FLOWER.get().defaultBlockState()
                .setValue(flower, IndicatorFlower.PANDANUS_CANDELABRUM);
        var plains = ModBlocks.GT_INDICATOR_FLOWER.get().defaultBlockState()
                .setValue(flower, IndicatorFlower.ORECHID);
        helper.setBlock(rel.below(), Blocks.SAND);
        helper.setBlock(rel, desert);
        helper.assertTrue(
                helper.getBlockState(rel).canSurvive(
                        helper.getLevel(), helper.absolutePos(rel)),
                "FlowersB must stay on sand");
        helper.assertTrue(
                !GtIndicatorFlowerBlock.canStayOn(
                        desert, Blocks.GRASS_BLOCK.defaultBlockState()),
                "FlowersB must not stay on grass");
        helper.setBlock(rel.below(), Blocks.GRASS_BLOCK);
        helper.setBlock(rel, plains);
        helper.assertTrue(
                helper.getBlockState(rel).canSurvive(
                        helper.getLevel(), helper.absolutePos(rel)),
                "FlowersA must stay on grass");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void smallOreUsesDropsSmallOre(GameTestHelper helper) {
        BlockPos rel = new BlockPos(8, 4, 8);
        helper.setBlock(
                rel,
                ModBlocks.GT_SMALL_ORE.get().defaultBlockState());
        BedrockOreBlock.placeMaterial(
                helper.getLevel(), helper.absolutePos(rel), "diamond");
        var drops = DropsSmallOre.drops(
                "diamond",
                com.masson.cruciblecraft.content.block.OreStoneHost.STONE,
                helper.absolutePos(rel),
                0,
                false);
        helper.assertTrue(!drops.isEmpty(), "Drops_SmallOre must emit at least one stack");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void drillEmitsBrokenOre(GameTestHelper helper) {
        ItemStack broken = com.masson.cruciblecraft.content.block.GtBrokenOreBlock.item(
                "gold",
                com.masson.cruciblecraft.content.block.OreStoneHost.STONE);
        helper.assertTrue(
                broken.is(ModBlocks.GT_BROKEN_ORE.get().asItem()),
                "drill analog must be oreBroken");
        helper.assertTrue(
                "gold".equals(broken.get(
                        com.masson.cruciblecraft.registry.ModComponents.ORE_MATERIAL.get())),
                "oreBroken must carry ORE_MATERIAL");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void lavaSpringSkipsBedrockOreChunk(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        int minX = origin.getX();
        int minZ = origin.getZ();
        int floorY = helper.absolutePos(new BlockPos(0, 1, 0)).getY();
        for (int tX = 0; tX < 16; tX++) {
            for (int tZ = 0; tZ < 16; tZ++) {
                helper.setBlock(new BlockPos(tX, 1, tZ), Blocks.BEDROCK);
            }
        }
        helper.setBlock(new BlockPos(8, 1, 8), ModBlocks.GT_BEDROCK_ORE.get());
        BedrockOreBlock.placeMaterial(
                helper.getLevel(),
                helper.absolutePos(new BlockPos(8, 1, 8)),
                "gold");
        boolean placed = FluidSpringVeins.placeSpring(
                helper.getLevel(), minX, minZ, floorY);
        helper.assertTrue(!placed, "lava spring must skip a bedrock-ore floor");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void lavaSpringPlacesMuffinOnVanillaBedrock(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        int minX = origin.getX();
        int minZ = origin.getZ();
        int floorY = helper.absolutePos(new BlockPos(0, 1, 0)).getY();
        for (int tX = 0; tX < 16; tX++) {
            for (int tZ = 0; tZ < 16; tZ++) {
                helper.setBlock(new BlockPos(tX, 1, tZ), Blocks.BEDROCK);
            }
        }
        boolean placed = FluidSpringVeins.placeSpring(
                helper.getLevel(), minX, minZ, floorY);
        helper.assertTrue(placed, "lava spring must fill vanilla bedrock muffin");
        helper.assertTrue(
                helper.getBlockState(new BlockPos(8, 3, 8)).is(Blocks.LAVA),
                "spring muffin must contain lava");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void extraHeavyOilWinsFirstCatalogRoll(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        int minX = origin.getX();
        int minZ = origin.getZ();
        int floorY = helper.absolutePos(new BlockPos(0, 1, 0)).getY();
        for (int tX = 0; tX < 16; tX++) {
            for (int tZ = 0; tZ < 16; tZ++) {
                helper.setBlock(new BlockPos(tX, 1, tZ), Blocks.BEDROCK);
            }
        }
        boolean placed = FluidSpringVeins.placeSpring(
                helper.getLevel(),
                minX,
                minZ,
                floorY,
                FluidSpringCatalog.VEINS.getFirst(),
                new Random(1L));
        helper.assertTrue(placed, "extra-heavy oil spring must fill the muffin");
        helper.assertTrue(
                helper.getBlockState(new BlockPos(8, 3, 8))
                        .is(ModBlocks.OIL_EXTRA_HEAVY.get()),
                "first catalog row must place extra-heavy oil, not lava or crude_oil");
        helper.assertTrue(
                helper.getBlockState(new BlockPos(8, 3, 8))
                                .getValue(SpringLiquidBlock.META)
                        == SpringLiquidBlock.FULL_META,
                "worldgen muffin is GT6 meta 15 (16 quanta)");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fluidSpringTileStoresAmount(GameTestHelper helper) {
        BlockPos rel = new BlockPos(8, 1, 8);
        helper.setBlock(rel, Blocks.BEDROCK);
        FluidSpringVeins.placeSpringTile(
                helper.getLevel(),
                helper.absolutePos(rel),
                FluidSpringCatalog.VEINS.get(4));
        helper.assertTrue(
                helper.getBlockState(rel).is(ModBlocks.GT_FLUID_SPRING.get()),
                "32763 analog must place gt_fluid_spring");
        var blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(rel));
        helper.assertTrue(
                blockEntity instanceof com.masson.cruciblecraft.content.blockentity
                        .FluidSpringBlockEntity spring
                        && "cruciblecraft:natural_gas".equals(spring.fluidId())
                        && spring.amount() == 3000,
                "fluid spring must store GT6 gas amount 3000");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void indicatorGrassReplacesVanillaGrass(GameTestHelper helper) {
        BlockPos rel = new BlockPos(8, 3, 8);
        helper.setBlock(rel, Blocks.GRASS_BLOCK);
        helper.assertTrue(
                com.masson.cruciblecraft.content.block.GtIndicatorGrassBlock
                        .isPlantableGrass(helper.getBlockState(rel)),
                "vanilla grass must be plantableGrass");
        helper.setBlock(
                rel,
                ModBlocks.GT_INDICATOR_GRASS.get().defaultBlockState().setValue(
                        com.masson.cruciblecraft.content.block.GtIndicatorGrassBlock.GRASS,
                        com.masson.cruciblecraft.worldgen.IndicatorGrass.YELLOW));
        helper.assertTrue(
                helper.getBlockState(rel).getValue(
                        com.masson.cruciblecraft.content.block.GtIndicatorGrassBlock.GRASS)
                        == com.masson.cruciblecraft.worldgen.IndicatorGrass.YELLOW,
                "gas indicator grass is yellow (meta 4)");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void oilQuantaEqualizeIntoAir(GameTestHelper helper) {
        SpringLiquidBlock oil = ModBlocks.OIL_MEDIUM.get();
        BlockPos origin = new BlockPos(8, 3, 8);
        helper.setBlock(origin.below(), Blocks.STONE);
        helper.setBlock(origin.above(), Blocks.STONE);
        helper.setBlock(origin.north(), Blocks.STONE);
        helper.setBlock(origin.south(), Blocks.STONE);
        helper.setBlock(origin.west(), Blocks.STONE);
        helper.setBlock(origin.east().below(), Blocks.STONE);
        helper.setBlock(origin.east().above(), Blocks.STONE);
        helper.setBlock(origin, oil.fullState());
        oil.flowTick(
                helper.getLevel(),
                helper.absolutePos(origin),
                helper.getLevel().random);
        helper.assertTrue(
                helper.getBlockState(origin.east()).is(oil),
                "16 quanta must equalize into adjacent air");
        helper.assertTrue(
                helper.getBlockState(origin).getValue(SpringLiquidBlock.META)
                        == SpringLiquidBlock.DRIP_META,
                "origin should keep 8 quanta (meta 7)");
        helper.assertTrue(
                helper.getBlockState(origin.east()).getValue(SpringLiquidBlock.META)
                        == SpringLiquidBlock.DRIP_META,
                "neighbor should receive 8 quanta (meta 7)");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void compressedOilFountainsUp(GameTestHelper helper) {
        SpringLiquidBlock oil = ModBlocks.OIL_MEDIUM.get();
        BlockPos origin = new BlockPos(8, 3, 8);
        helper.setBlock(origin.below(), Blocks.STONE);
        helper.setBlock(origin, oil.fullState());
        oil.flowTick(
                helper.getLevel(),
                helper.absolutePos(origin),
                helper.getLevel().random);
        helper.assertTrue(
                helper.getBlockState(origin.above()).is(oil),
                "compressed (>8 quanta) oil fountains against densityDir");
        helper.assertTrue(
                helper.getBlockState(origin.above()).getValue(SpringLiquidBlock.META)
                        == 14,
                "fountain moves all but one quantum up (meta 14)");
        helper.assertTrue(
                helper.getBlockState(origin).is(oil)
                        && helper.getBlockState(origin).getValue(SpringLiquidBlock.META)
                                == 0,
                "fountain leaves 1 quantum at the origin (meta 0)");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fluidSpringDripsFiniteMeta7(GameTestHelper helper) {
        BlockPos springPos = new BlockPos(8, 1, 8);
        BlockPos drip = springPos.above();
        helper.setBlock(springPos, ModBlocks.GT_FLUID_SPRING.get());
        helper.setBlock(drip.north(), Blocks.STONE);
        helper.setBlock(drip.south(), Blocks.STONE);
        helper.setBlock(drip.west(), Blocks.STONE);
        helper.setBlock(drip.east(), Blocks.STONE);
        helper.setBlock(drip.above(), Blocks.STONE);
        var blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(springPos));
        helper.assertTrue(
                blockEntity instanceof FluidSpringBlockEntity,
                "gt_fluid_spring must have a block entity");
        FluidSpringBlockEntity spring = (FluidSpringBlockEntity) blockEntity;
        spring.configure("cruciblecraft:oil_medium", 1);
        spring.dripOnce();
        helper.assertTrue(
                helper.getBlockState(drip).is(ModBlocks.OIL_MEDIUM.get()),
                "finite spring places oil into air above");
        helper.assertTrue(
                helper.getBlockState(drip).getValue(SpringLiquidBlock.META)
                        == SpringLiquidBlock.DRIP_META,
                "GT6 finite branch places meta 7 into air");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fluidSpringAddsEightQuanta(GameTestHelper helper) {
        SpringLiquidBlock oil = ModBlocks.OIL_MEDIUM.get();
        BlockPos springPos = new BlockPos(8, 1, 8);
        BlockPos drip = springPos.above();
        helper.setBlock(springPos, ModBlocks.GT_FLUID_SPRING.get());
        helper.setBlock(drip.north(), Blocks.STONE);
        helper.setBlock(drip.south(), Blocks.STONE);
        helper.setBlock(drip.west(), Blocks.STONE);
        helper.setBlock(drip.east(), Blocks.STONE);
        helper.setBlock(drip.above(), Blocks.STONE);
        helper.setBlock(drip, oil.withMeta(0));
        var blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(springPos));
        helper.assertTrue(
                blockEntity instanceof FluidSpringBlockEntity,
                "gt_fluid_spring must have a block entity");
        FluidSpringBlockEntity spring = (FluidSpringBlockEntity) blockEntity;
        spring.configure("cruciblecraft:oil_medium", 1);
        spring.dripOnce();
        helper.assertTrue(
                helper.getBlockState(drip).is(oil)
                        && helper.getBlockState(drip).getValue(SpringLiquidBlock.META)
                                == 8,
                "GT6 finite branch bind4(meta+8) onto the same fluid");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void naturalGasRisesAgainstGravity(GameTestHelper helper) {
        SpringLiquidBlock gas = ModBlocks.NATURAL_GAS.get();
        BlockPos origin = new BlockPos(8, 3, 8);
        helper.setBlock(origin.below(), Blocks.STONE);
        helper.setBlock(origin.north(), Blocks.STONE);
        helper.setBlock(origin.south(), Blocks.STONE);
        helper.setBlock(origin.west(), Blocks.STONE);
        helper.setBlock(origin.east(), Blocks.STONE);
        helper.setBlock(origin, gas.withMeta(SpringLiquidBlock.DRIP_META));
        gas.flowTick(
                helper.getLevel(),
                helper.absolutePos(origin),
                helper.getLevel().random);
        helper.assertTrue(
                helper.getBlockState(origin).isAir(),
                "gas with density < 0 leaves the origin");
        helper.assertTrue(
                helper.getBlockState(origin.above()).is(gas)
                        && helper.getBlockState(origin.above())
                                        .getValue(SpringLiquidBlock.META)
                                == SpringLiquidBlock.DRIP_META,
                "natural gas densityDir is +1 so it rises");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void bucketPickupTakesEightQuanta(GameTestHelper helper) {
        SpringLiquidBlock oil = ModBlocks.OIL_MEDIUM.get();
        BlockPos origin = new BlockPos(8, 3, 8);
        helper.setBlock(origin.below(), Blocks.STONE);
        helper.setBlock(origin, oil.fullState());
        ItemStack full = oil.pickupBlock(
                null,
                helper.getLevel(),
                helper.absolutePos(origin),
                helper.getBlockState(origin));
        helper.assertTrue(
                full.is(ModItems.OIL_MEDIUM_BUCKET.get()),
                "meta>=7 must fill the spring bucket");
        helper.assertTrue(
                helper.getBlockState(origin).is(oil)
                        && helper.getBlockState(origin)
                                        .getValue(SpringLiquidBlock.META)
                                == SpringLiquidBlock.DRIP_META,
                "full muffin leaves 8 quanta (meta 7) after a 1000 mB bucket");
        ItemStack drip = oil.pickupBlock(
                null,
                helper.getLevel(),
                helper.absolutePos(origin),
                helper.getBlockState(origin));
        helper.assertTrue(
                drip.is(ModItems.OIL_MEDIUM_BUCKET.get())
                        && helper.getBlockState(origin).isAir(),
                "meta 7 is exactly one bucket and clears the block");
        helper.setBlock(origin, oil.withMeta(6));
        ItemStack tooThin = oil.pickupBlock(
                null,
                helper.getLevel(),
                helper.absolutePos(origin),
                helper.getBlockState(origin));
        helper.assertTrue(
                tooThin.isEmpty()
                        && helper.getBlockState(origin)
                                        .getValue(SpringLiquidBlock.META)
                                == 6,
                "below drip meta must not pick up");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void oilImmersionPoisonsAndBlinds(GameTestHelper helper) {
        SpringLiquidBlock oil = ModBlocks.OIL_HEAVY.get();
        BlockPos origin = new BlockPos(8, 2, 8);
        helper.setBlock(origin.below(), Blocks.STONE);
        helper.setBlock(origin, oil.fullState());
        helper.setBlock(origin.above(), oil.fullState());
        Pig pig = helper.spawn(EntityType.PIG, new Vec3(8.5, 2.1, 8.5));
        pig.setNoAi(true);
        helper.succeedWhen(() -> helper.assertTrue(
                pig.hasEffect(MobEffects.POISON)
                        && pig.hasEffect(MobEffects.BLINDNESS)
                        && pig.hasEffect(MobEffects.CONFUSION),
                "oil bathing blinds; breathing poisons and nauseates"));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void geothermalImmersionRegenerates(GameTestHelper helper) {
        SpringLiquidBlock water = ModBlocks.WATER_GEOTHERMAL.get();
        BlockPos origin = new BlockPos(8, 2, 8);
        helper.setBlock(origin.below(), Blocks.STONE);
        helper.setBlock(origin, water.fullState());
        helper.setBlock(origin.above(), water.fullState());
        Pig pig = helper.spawn(EntityType.PIG, new Vec3(8.5, 2.1, 8.5));
        pig.setNoAi(true);
        helper.succeedWhen(() -> helper.assertTrue(
                pig.hasEffect(MobEffects.REGENERATION)
                        && pig.hasEffect(MobEffects.DAMAGE_RESISTANCE)
                        && !pig.hasEffect(MobEffects.POISON),
                "geothermal bathing regenerates without oil poison"));
    }
}
