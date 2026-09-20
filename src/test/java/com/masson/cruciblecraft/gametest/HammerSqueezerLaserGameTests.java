package com.masson.cruciblecraft.gametest;

import java.util.List;
import java.util.Map;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.AutomaticHammerBlock;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.blockentity.AnvilBlockEntity;
import com.masson.cruciblecraft.content.blockentity.AutomaticHammerBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.content.blockentity.LaserEngraverBlockEntity;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.masson.cruciblecraft.recipe.gt.CompactRecipeFamilyProvider;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.GTRecipeQuery;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated survival and side-contract gate for the Hammer / Squeezer / Laser
 * unique-active landing.
 */
@GameTestHolder(HammerSqueezerLaserGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class HammerSqueezerLaserGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_machines_hammer_squeezer_laser";
    private static final String TEMPLATE = "empty";
    private static final BlockPos POS = new BlockPos(2, 2, 2);

    private HammerSqueezerLaserGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fourHammersAreSurvivalCraftable(GameTestHelper helper) {
        Map<String, Item> outputs = Map.of(
                "bronze", ModItems.AUTOMATIC_HAMMER.get(),
                "steel", ModItems.STEEL_AUTOMATIC_HAMMER.get(),
                "titanium", ModItems.TITANIUM_AUTOMATIC_HAMMER.get(),
                "tungstensteel", ModItems.TUNGSTENSTEEL_AUTOMATIC_HAMMER.get());
        MaterialPrefix head =
                MaterialPrefixCatalog.require("tool_head_hammer");
        outputs.forEach((material, result) -> {
            ItemStack assembled = craft(helper, List.of(
                    new ItemStack(ModItems.MATERIAL_WRENCH.get()),
                    MaterialLookup.stack(material, MaterialPrefixes.LONG_ROD),
                    ItemStack.EMPTY,
                    MaterialLookup.stack(
                            material, MaterialPrefixes.MACHINE_CASING_DOUBLE),
                    MaterialLookup.stack(material, MaterialPrefixes.SPRING),
                    ItemStack.EMPTY,
                    new ItemStack(ModItems.SMITHING_HAMMER.get()),
                    MaterialLookup.stack(material, head),
                    ItemStack.EMPTY));
            helper.assertTrue(
                    assembled.is(result),
                    "Automatic hammer recipe missing for " + material);
        });
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fourSqueezersAreSurvivalCraftable(GameTestHelper helper) {
        Map<String, String> hosts = Map.of(
                "bronze", "squeezer",
                "steel", "steel_squeezer",
                "titanium", "titanium_squeezer",
                "tungstensteel", "tungstensteel_squeezer");
        hosts.forEach((material, path) -> {
            ItemStack assembled = craft(helper, List.of(
                    MaterialLookup.stack(material, MaterialPrefixes.ROD),
                    MaterialLookup.stack(material, MaterialPrefixes.SPRING),
                    ItemStack.EMPTY,
                    MaterialLookup.stack(material, MaterialPrefixes.TRIPLE_PLATE),
                    MaterialLookup.stack(
                            material, MaterialPrefixes.MACHINE_CASING_DOUBLE),
                    ItemStack.EMPTY,
                    MaterialLookup.stack(material, MaterialPrefixes.TRIPLE_PLATE),
                    new ItemStack(ModItems.MATERIAL_WRENCH.get()),
                    ItemStack.EMPTY));
            helper.assertTrue(
                    assembled.is(item(path)),
                    "Squeezer recipe missing for " + material);
        });
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void laserHostsAreSurvivalCraftable(GameTestHelper helper) {
        for (int tier = 1; tier <= 5; tier++) {
            String material = laserMaterial(tier);
            String engraver = tier == 1
                    ? "laser_engraver"
                    : material + "_laser_engraver";
            ItemStack engraverStack = craft(helper, List.of(
                    MaterialLookup.stack(material, MaterialPrefixes.SCREW),
                    new ItemStack(ModItems.MATERIAL_SCREWDRIVER.get()),
                    MaterialLookup.stack(material, MaterialPrefixes.SCREW),
                    MaterialLookup.stack(material, MaterialPrefixes.SMALL_GEAR),
                    new ItemStack(net.minecraft.world.item.Items.TERRACOTTA),
                    MaterialLookup.stack(material, MaterialPrefixes.SMALL_GEAR),
                    new ItemStack(circuit(tier)),
                    MaterialLookup.stack(
                            material, MaterialPrefixes.MACHINE_CASING),
                    new ItemStack(circuit(tier))));
            helper.assertTrue(
                    engraverStack.is(item(engraver)),
                    "Laser engraver recipe missing for tier " + tier);

            String welder = tier == 1
                    ? "laser_welder"
                    : material + "_laser_welder";
            ItemStack welderStack = craft(helper, List.of(
                    MaterialLookup.stack(material, MaterialPrefixes.SCREW),
                    yellowLens(),
                    MaterialLookup.stack(material, MaterialPrefixes.SCREW),
                    MaterialLookup.stack(material, MaterialPrefixes.SMALL_GEAR),
                    new ItemStack(net.minecraft.world.item.Items.TERRACOTTA),
                    MaterialLookup.stack(material, MaterialPrefixes.SMALL_GEAR),
                    new ItemStack(circuit(tier)),
                    MaterialLookup.stack(
                            material, MaterialPrefixes.MACHINE_CASING),
                    new ItemStack(circuit(tier))));
            helper.assertTrue(
                    welderStack.is(item(welder)),
                    "Laser welder recipe missing for tier " + tier);
        }
        ItemStack heliodorWelder = craft(helper, List.of(
                MaterialLookup.stack("steel_galvanized", MaterialPrefixes.SCREW),
                MaterialLookup.stack(
                        "heliodor", MaterialPrefixCatalog.require("lens")),
                MaterialLookup.stack("steel_galvanized", MaterialPrefixes.SCREW),
                MaterialLookup.stack(
                        "steel_galvanized", MaterialPrefixes.SMALL_GEAR),
                new ItemStack(net.minecraft.world.item.Items.TERRACOTTA),
                MaterialLookup.stack(
                        "steel_galvanized", MaterialPrefixes.SMALL_GEAR),
                new ItemStack(circuit(1)),
                MaterialLookup.stack(
                        "steel_galvanized", MaterialPrefixes.MACHINE_CASING),
                new ItemStack(circuit(1))));
        helper.assertTrue(
                heliodorWelder.is(item("laser_welder")),
                "T1 laser welder rejected a second yellow lens material");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void bronzeSqueezerAcceptsKuFromTop(GameTestHelper helper) {
        Block block = ModBlocks.tieredProcessingBlocksById()
                .get(id("squeezer"))
                .get();
        helper.setBlock(
                POS,
                block.defaultBlockState()
                        .setValue(ProcessingMachineBlock.FACING, Direction.NORTH));
        ConfiguredProcessingMachineBlockEntity machine =
                helper.getBlockEntity(POS);
        long packet = machine.spec().energy().maxPacket();
        long accepted = machine.insert(
                EnergyType.KINETIC_PUSH,
                packet,
                1L,
                Direction.UP,
                false);
        helper.assertTrue(
                accepted == 1L && machine.stored(EnergyType.KINETIC_PUSH) > 0L,
                "Bronze squeezer did not accept KU from its top side");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void automaticHammerAcceptsKuFromBack(GameTestHelper helper) {
        helper.setBlock(
                POS,
                ModBlocks.AUTOMATIC_HAMMER.get().defaultBlockState());
        AutomaticHammerBlockEntity hammer = helper.getBlockEntity(POS);
        long accepted = hammer.insert(
                EnergyType.KINETIC_PUSH,
                hammer.profile().input(),
                1L,
                Direction.UP,
                false);
        helper.assertTrue(
                accepted == 1L
                        && hammer.stored(EnergyType.KINETIC_PUSH)
                                == hammer.profile().input(),
                "Automatic hammer did not accept KU from its back");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void automaticHammerAcceptsDoubleRecommendedPacket(
            GameTestHelper helper) {
        helper.setBlock(
                POS,
                ModBlocks.AUTOMATIC_HAMMER.get().defaultBlockState());
        AutomaticHammerBlockEntity hammer = helper.getBlockEntity(POS);
        long doubled = hammer.profile().input() * 2L;
        helper.assertTrue(
                hammer.profile().maxPacket() == doubled,
                "Automatic hammer max packet is not recommended × 2");
        long accepted = hammer.insert(
                EnergyType.KINETIC_PUSH,
                doubled,
                1L,
                Direction.UP,
                false);
        helper.assertTrue(
                accepted == 1L
                        && hammer.stored(EnergyType.KINETIC_PUSH) == doubled,
                "Automatic hammer rejected GT6 max-size KU");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void automaticHammerStrikesOnlyOnReturnStroke(
            GameTestHelper helper) {
        BlockPos cobble = POS.relative(Direction.NORTH);
        helper.setBlock(
                POS,
                ModBlocks.TITANIUM_AUTOMATIC_HAMMER.get()
                        .defaultBlockState()
                        .setValue(AutomaticHammerBlock.FACING, Direction.NORTH));
        helper.setBlock(cobble, Blocks.COBBLESTONE);
        AutomaticHammerBlockEntity hammer = helper.getBlockEntity(POS);
        long input = hammer.profile().input();
        helper.assertTrue(
                hammer.insert(
                        EnergyType.KINETIC_PUSH,
                        input,
                        1L,
                        Direction.SOUTH,
                        false)
                        == 1L
                        && hammer.stored(EnergyType.KINETIC_PUSH) == input,
                "Titanium hammer did not store the push stroke");
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(
                            helper.getBlockState(cobble).is(Blocks.COBBLESTONE),
                            "Push stroke mined the front block");
                    helper.assertTrue(
                            hammer.insert(
                                    EnergyType.KINETIC_PUSH,
                                    -input,
                                    1L,
                                    Direction.SOUTH,
                                    false)
                                    == 1L,
                            "Titanium hammer did not accept the return stroke");
                })
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        helper.getBlockState(cobble).isAir()
                                && hammer.stored(EnergyType.KINETIC_PUSH) == 0L,
                        "Return stroke did not mine cobble"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void automaticHammerStrikesAnvilFromAbove(
            GameTestHelper helper) {
        BlockPos anvilPos = POS.below();
        helper.setBlock(anvilPos, ModBlocks.ANVIL.get().defaultBlockState());
        helper.setBlock(
                POS,
                ModBlocks.STEEL_AUTOMATIC_HAMMER.get()
                        .defaultBlockState()
                        .setValue(AutomaticHammerBlock.FACING, Direction.DOWN));
        AnvilBlockEntity anvil = helper.getBlockEntity(anvilPos);
        AutomaticHammerBlockEntity hammer = helper.getBlockEntity(POS);
        anvil.setMaterialId("iron");
        helper.assertTrue(
                anvil.insert(0, MaterialLookup.stack("iron", MaterialPrefixes.INGOT)),
                "Anvil rejected the iron ingot");
        long input = hammer.profile().input();
        helper.assertTrue(
                hammer.insert(
                        EnergyType.KINETIC_PUSH,
                        input,
                        1L,
                        Direction.UP,
                        false)
                        == 1L,
                "Steel hammer did not store the push stroke");
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        anvil.strikes() == 0
                                && hammer.insert(
                                        EnergyType.KINETIC_PUSH,
                                        -input,
                                        1L,
                                        Direction.UP,
                                        false)
                                        == 1L,
                        "Push stroke struck the anvil or return KU was rejected"))
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        anvil.strikes() == 1
                                && anvil.workpiece(0).is(MaterialLookup.stack(
                                        "iron", MaterialPrefixes.INGOT).getItem()),
                        "Down-facing hammer did not apply GT6 ANVIL mode"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void automaticHammerSideHitDoesNotUseAnvilRecipes(
            GameTestHelper helper) {
        BlockPos anvilPos = POS.relative(Direction.NORTH);
        helper.setBlock(anvilPos, ModBlocks.ANVIL.get().defaultBlockState());
        helper.setBlock(
                POS,
                ModBlocks.STEEL_AUTOMATIC_HAMMER.get()
                        .defaultBlockState()
                        .setValue(AutomaticHammerBlock.FACING, Direction.NORTH));
        AnvilBlockEntity anvil = helper.getBlockEntity(anvilPos);
        AutomaticHammerBlockEntity hammer = helper.getBlockEntity(POS);
        anvil.setMaterialId("iron");
        ItemStack ingot = MaterialLookup.stack("iron", MaterialPrefixes.INGOT);
        helper.assertTrue(
                anvil.insert(0, ingot.copy()),
                "Anvil rejected the iron ingot");
        long input = hammer.profile().input();
        helper.assertTrue(
                hammer.insert(
                        EnergyType.KINETIC_PUSH,
                        input,
                        1L,
                        Direction.SOUTH,
                        false)
                        == 1L
                        && hammer.insert(
                                EnergyType.KINETIC_PUSH,
                                -input,
                                1L,
                                Direction.SOUTH,
                                false)
                                == 1L,
                "Steel hammer did not accept the signed KU pair");
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        anvil.workpiece(0).is(ingot.getItem())
                                && anvil.strikes() == 0,
                        "Side hit leaked RM.Anvil instead of GT6 bend routing"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void automaticHammerSideHitBendsPlate(GameTestHelper helper) {
        BlockPos anvilPos = POS.relative(Direction.NORTH);
        helper.setBlock(anvilPos, ModBlocks.ANVIL.get().defaultBlockState());
        helper.setBlock(
                POS,
                ModBlocks.STEEL_AUTOMATIC_HAMMER.get()
                        .defaultBlockState()
                        .setValue(AutomaticHammerBlock.FACING, Direction.NORTH));
        AnvilBlockEntity anvil = helper.getBlockEntity(anvilPos);
        AutomaticHammerBlockEntity hammer = helper.getBlockEntity(POS);
        anvil.setMaterialId("iron");
        ItemStack plate = MaterialLookup.stack("iron", MaterialPrefixes.PLATE);
        helper.assertTrue(
                anvil.insert(0, plate.copy()),
                "Anvil rejected the iron plate");
        long input = hammer.profile().input();
        helper.assertTrue(
                hammer.insert(
                        EnergyType.KINETIC_PUSH,
                        input,
                        1L,
                        Direction.SOUTH,
                        false)
                        == 1L
                        && hammer.insert(
                                EnergyType.KINETIC_PUSH,
                                -input,
                                1L,
                                Direction.SOUTH,
                                false)
                                == 1L,
                "Steel hammer did not accept the signed KU pair");
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        anvil.workpiece(0).is(plate.getItem())
                                && anvil.strikes() > 0,
                        "Side hit did not progress GT6 AnvilBendBig plate to curved plate"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void automaticHammerStoresThenExplodesOversizedKu(
            GameTestHelper helper) {
        helper.setBlock(
                POS,
                ModBlocks.AUTOMATIC_HAMMER.get().defaultBlockState());
        AutomaticHammerBlockEntity hammer = helper.getBlockEntity(POS);
        long oversized = 32L;
        helper.assertTrue(
                oversized > hammer.profile().maxPacket(),
                "Bronze oversize fixture is not above recommended × 2");
        long accepted = hammer.insert(
                EnergyType.KINETIC_PUSH,
                oversized,
                1L,
                Direction.UP,
                true);
        helper.assertTrue(
                accepted == 1L
                        && hammer.stored(EnergyType.KINETIC_PUSH) == 0L
                        && helper.getBlockState(POS).is(
                                ModBlocks.AUTOMATIC_HAMMER.get()),
                "Simulated oversize KU exploded or stored");
        helper.assertTrue(
                hammer.insert(
                        EnergyType.KINETIC_PUSH,
                        oversized,
                        1L,
                        Direction.UP,
                        false)
                        == 1L
                        && hammer.stored(EnergyType.KINETIC_PUSH) == oversized,
                "Oversize KU did not store before exploding");
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        helper.getBlockState(POS).isAir(),
                        "Oversize KU did not explode the automatic hammer"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void automaticHammerCannotMineBeyondStoredEnergy(
            GameTestHelper helper) {
        BlockPos cobble = POS.relative(Direction.NORTH);
        helper.setBlock(
                POS,
                ModBlocks.AUTOMATIC_HAMMER.get()
                        .defaultBlockState()
                        .setValue(AutomaticHammerBlock.FACING, Direction.NORTH));
        helper.setBlock(cobble, Blocks.COBBLESTONE);
        AutomaticHammerBlockEntity hammer = helper.getBlockEntity(POS);
        long input = hammer.profile().input();
        helper.assertTrue(
                input * 1.0F < 2.0F * 50.0F,
                "Bronze packet is large enough to mine cobble");
        helper.assertTrue(
                hammer.insert(
                        EnergyType.KINETIC_PUSH,
                        input,
                        1L,
                        Direction.SOUTH,
                        false)
                        == 1L
                        && hammer.insert(
                                EnergyType.KINETIC_PUSH,
                                -input,
                                1L,
                                Direction.SOUTH,
                                false)
                                == 1L,
                "Bronze hammer did not accept the signed KU pair");
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        helper.getBlockState(cobble).is(Blocks.COBBLESTONE)
                                && hammer.stored(EnergyType.KINETIC_PUSH) == 0L,
                        "Bronze hammer mined cobble without hardness × 50 KU"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void t1LaserEngraverUsesLuTier1Window(GameTestHelper helper) {
        helper.setBlock(
                POS,
                ModBlocks.LASER_ENGRAVER.get().defaultBlockState());
        LaserEngraverBlockEntity laser = helper.getBlockEntity(POS);
        helper.assertTrue(
                laser.variant().tierBand().inputNominal() == 32L
                        && laser.variant().tierBand().inputMaximum() == 64L
                        && laser.variant()
                                .tierBand()
                                .materialId()
                                .endsWith("steel_galvanized"),
                "T1 laser engraver is not lu_tier_1 / galvanized steel");
        helper.assertTrue(
                laser.insert(EnergyType.LU, 32L, 1L, Direction.UP, false) == 1L
                        && laser.stored(EnergyType.LU) == 32L,
                "T1 laser engraver rejected in-window LU");
        helper.assertTrue(
                laser.insert(EnergyType.LU, 128L, 1L, Direction.UP, false) == 1L
                        && laser.runtime().status().equals("overcharged"),
                "T1 laser engraver did not overcharge on 128 LU");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void laserWelderHostMatchesGt6WelderSlots(
            GameTestHelper helper) {
        Block block = ModBlocks.tieredProcessingBlocksById()
                .get(id("laser_welder"))
                .get();
        helper.setBlock(
                POS,
                block.defaultBlockState()
                        .setValue(ProcessingMachineBlock.FACING, Direction.NORTH));
        ConfiguredProcessingMachineBlockEntity machine =
                helper.getBlockEntity(POS);
        helper.assertTrue(
                "laser_welder".equals(machine.spec().id().getPath())
                        && machine.spec().items().inputs().size() == 9
                        && machine.spec().items().outputs().size() == 1
                        && machine.spec().fluids().inputs().size() == 1
                        && machine.spec().ui().machineSlots().size() == 10
                        && machine.inventory().getSlots() == 10,
                "Laser welder live inventory is not GT6 RM.Welder 9-in / 1-out");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void laserWelderRunsWelderRecipe(GameTestHelper helper) {
        Block block = ModBlocks.tieredProcessingBlocksById()
                .get(id("laser_welder"))
                .get();
        helper.setBlock(
                POS,
                block.defaultBlockState()
                        .setValue(ProcessingMachineBlock.FACING, Direction.NORTH));
        ConfiguredProcessingMachineBlockEntity machine =
                helper.getBlockEntity(POS);
        helper.assertTrue(
                !ModRecipeMaps.WELDER.recipes().isEmpty(),
                "Welder map lost the live prefix rows");
        helper.assertTrue(
                ModRecipeMaps.WELDER.entries().stream().anyMatch(entry ->
                        entry.id().getPath().startsWith(
                                "welder/ingots_to_double_ingot/")),
                "GT6 welder ingot-to-double-ingot handler did not expand");
        helper.assertTrue(
                !ModRecipeMaps.ANVIL_BEND_BIG.recipes().isEmpty()
                        && !ModRecipeMaps.ANVIL_BEND_SMALL.recipes().isEmpty(),
                "Anvil-bend maps are still empty");
        GTRecipe recipe = ModRecipeMaps.WELDER.recipes().getFirst();
        for (int i = 0; i < recipe.itemInputs().size(); i++) {
            ItemStack sample = recipe.itemInputs().get(i).getItems()[0].copy();
            sample.setCount(Math.max(1, recipe.itemInputCounts().get(i)));
            machine.inventory().setStackInSlot(
                    machine.spec().items().inputs().get(i), sample);
        }
        long packet = Math.min(
                machine.variant().tierBand().inputMaximum(),
                Math.max(recipe.eut(), machine.variant().tierBand().inputNominal()));
        helper.assertTrue(
                machine.insert(EnergyType.LU, packet, 1L, Direction.UP, false) == 1L
                        && machine.stored(EnergyType.LU) >= recipe.eut(),
                "T1 laser welder did not store enough LU for a welder recipe");
        helper.startSequence()
                .thenExecuteFor(4, () -> machine.insert(
                        EnergyType.LU, packet, 1L, Direction.UP, false))
                .thenExecute(() -> helper.assertTrue(
                        machine.duration() > 0
                                || machine.workProgressLong() > 0L,
                        "9-slot laser welder did not start a welder recipe: "
                                + machine.pausedReason()))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void higherTierHostsUseGt6PacketWindows(GameTestHelper helper) {
        Block t5 = ModBlocks.tieredProcessingBlocksById()
                .get(id("titanium_laser_engraver"))
                .get();
        helper.setBlock(
                POS,
                t5.defaultBlockState()
                        .setValue(ProcessingMachineBlock.FACING, Direction.NORTH));
        ConfiguredProcessingMachineBlockEntity laser = helper.getBlockEntity(POS);
        helper.assertTrue(
                laser.variant().tierBand().inputNominal() == 8192L
                        && laser.variant().tierBand().inputMaximum() == 16384L,
                "T5 laser engraver is not lu_tier_5");
        helper.assertTrue(
                laser.insert(EnergyType.LU, 8192L, 1L, Direction.UP, false) == 1L
                        && laser.stored(EnergyType.LU) == 8192L,
                "T5 laser engraver rejected GT6 NBT_INPUT 8192 LU");
        helper.assertTrue(
                laser.insert(EnergyType.LU, 32768L, 1L, Direction.UP, false) == 1L
                        && laser.runtime().status().equals("overcharged"),
                "T5 laser engraver did not overcharge above 16384 LU");

        Block t4 = ModBlocks.tieredProcessingBlocksById()
                .get(id("tungstensteel_squeezer"))
                .get();
        BlockPos squeezerPos = POS.east();
        helper.setBlock(
                squeezerPos,
                t4.defaultBlockState()
                        .setValue(ProcessingMachineBlock.FACING, Direction.NORTH));
        ConfiguredProcessingMachineBlockEntity squeezer =
                helper.getBlockEntity(squeezerPos);
        helper.assertTrue(
                squeezer.variant().tierBand().inputNominal() == 2048L
                        && squeezer.variant().tierBand().parallelLimit() == 32
                        && squeezer.variant().kind().parallelDuration(),
                "T4 squeezer is not ku_tier_4 / PARALLEL_DURATION");
        helper.assertTrue(
                squeezer.insert(
                        EnergyType.KINETIC_PUSH, 2048L, 1L, Direction.UP, false)
                        == 1L
                        && squeezer.stored(EnergyType.KINETIC_PUSH) == 2048L,
                "T4 squeezer rejected GT6 NBT_INPUT 2048 KU");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void eighteenHostDummiesAreWithdrawn(GameTestHelper helper) {
        List<String> dummies = List.of(
                "processing/automatic_hammer_bronze",
                "processing/automatic_hammer_steel",
                "processing/automatic_hammer_titanium",
                "processing/automatic_hammer_tungstensteel",
                "processing/squeezer_bronze",
                "processing/squeezer_steel",
                "processing/squeezer_titanium",
                "processing/squeezer_tungstensteel",
                "processing/laser_engraver_t1",
                "processing/laser_engraver_t2",
                "processing/laser_engraver_t3",
                "processing/laser_engraver_t4",
                "processing/laser_engraver_t5",
                "processing/laser_welder_t1",
                "processing/laser_welder_t2",
                "processing/laser_welder_t3",
                "processing/laser_welder_t4",
                "processing/laser_welder_t5");
        helper.assertTrue(
                dummies.stream().noneMatch(path ->
                        BuiltInRegistries.ITEM.containsKey(id(path))),
                "Hammer/squeezer/laser dummy items are still registered");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void liveSqueezerHasGt6JavaLatexRows(GameTestHelper helper) {
        var entries = ModRecipeMaps.SQUEEZER.entries();
        helper.assertTrue(
                entries.stream().anyMatch(entry ->
                        "machine/squeezer/rubber_resin"
                                .equals(entry.id().getPath())),
                "GT6 rubber-resin squeezer row is missing");
        helper.assertTrue(
                entries.stream().anyMatch(entry ->
                        "machine/squeezer/wood_rubber_dust"
                                .equals(entry.id().getPath())),
                "GT6 rubber-wood-dust squeezer row is missing");
        RecipeMap.RecipeFamily family = ModRecipeMaps.SQUEEZER
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.SQUEEZER.id(),
                        ResourceLocation.fromNamespaceAndPath(
                                "cruciblecraft",
                                "squeezer/pilot/hammer_squeezer_laser")))
                .orElse(null);
        helper.assertTrue(
                family != null && family.logicalRecipeCount() == 15,
                "Squeezer compact family is not the 15 selected non-plant dump rows: "
                        + (family == null ? "missing" : family.logicalRecipeCount()));
        helper.assertTrue(
                entries.size() == 19,
                "Squeezer live map is not 15 dump rows plus 4 Java latex rows: "
                        + entries.size());
        helper.assertTrue(
                ModRecipeMaps.SQUEEZER.findMatch(
                        GTRecipeQuery.items(new ItemStack(Items.COD))).isPresent(),
                "Raw-cod squeezer row is missing");
        helper.assertTrue(
                ModRecipeMaps.SQUEEZER.findMatch(
                        GTRecipeQuery.items(new ItemStack(Items.SALMON))).isPresent(),
                "Raw-salmon squeezer row is missing");
        helper.assertTrue(
                ModRecipeMaps.SQUEEZER.findMatch(
                        GTRecipeQuery.items(new ItemStack(Items.TROPICAL_FISH)))
                        .isPresent(),
                "Tropical-fish squeezer row is missing");
        helper.assertTrue(
                ModRecipeMaps.SQUEEZER.findMatch(GTRecipeQuery.items(
                        MaterialLookup.stack(
                                "potassium",
                                MaterialPrefixCatalog.require("plant_gt_berry"))))
                        .isEmpty(),
                "Unobtainable plant_gt berry squeezer row stays ignored on this card");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void laserEngraverKeepsGt6FoilRows(GameTestHelper helper) {
        var entries = ModRecipeMaps.LASER_ENGRAVER.entries();
        helper.assertTrue(
                entries.stream().anyMatch(entry ->
                        "machine/laser_engraver/circuit_wire_copper"
                                .equals(entry.id().getPath())),
                "Copper foil laser-engraver row is missing");
        helper.assertTrue(
                entries.stream().anyMatch(entry ->
                        "machine/laser_engraver/circuit_wire_copper_annealed"
                                .equals(entry.id().getPath())),
                "Annealed-copper foil laser-engraver row is missing");
        helper.assertTrue(
                entries.stream().anyMatch(entry ->
                        "machine/laser_engraver/circuit_wire_gold"
                                .equals(entry.id().getPath())),
                "Gold foil laser-engraver row is missing");
        helper.assertTrue(
                entries.stream().anyMatch(entry ->
                        "machine/laser_engraver/circuit_wire_platinum"
                                .equals(entry.id().getPath())),
                "Platinum foil laser-engraver row is missing");
        helper.assertTrue(
                entries.size() == 8,
                "Laser engraver live map is not 4 GT6 foil rows plus 4 frozen PUV crystal rows: "
                        + entries.size());
        helper.succeed();
    }

    private static ItemStack craft(GameTestHelper helper, List<ItemStack> slots) {
        CraftingInput input = CraftingInput.of(3, 3, slots);
        return helper.getLevel()
                .getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .map(holder -> holder.value().assemble(
                        input, helper.getLevel().registryAccess()))
                .orElse(ItemStack.EMPTY);
    }

    private static Item item(String path) {
        return BuiltInRegistries.ITEM.get(id(path));
    }

    private static Item circuit(int tier) {
        String path = switch (tier) {
            case 1 -> "circuit_basic";
            case 2 -> "circuit_good";
            case 3 -> "circuit_advanced";
            case 4 -> "circuit_elite";
            case 5 -> "circuit_master";
            default -> throw new IllegalArgumentException("tier");
        };
        return ModItems.technologicalPart(path).get();
    }

    private static ItemStack yellowLens() {
        return MaterialLookup.stack(
                "yellow_sapphire",
                MaterialPrefixCatalog.require("lens"));
    }

    private static String laserMaterial(int tier) {
        return switch (tier) {
            case 1 -> "steel_galvanized";
            case 2 -> "aluminium";
            case 3 -> "stainless_steel";
            case 4 -> "chromium";
            case 5 -> "titanium";
            default -> throw new IllegalArgumentException("tier");
        };
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
