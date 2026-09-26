package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.content.block.DirectedWasteConverterBlock;
import com.masson.cruciblecraft.content.block.ElectricHeaterBlock;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.block.ZpmDechargerBlock;
import com.masson.cruciblecraft.content.blockentity.DirectedWasteConverterBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ElectricHeaterBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ProcessingMachineBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ZpmDechargerBlockEntity;
import com.masson.cruciblecraft.energy.battery.BatteryCharge;
import com.masson.cruciblecraft.energy.converter.EnergyConverterCatalog;
import com.masson.cruciblecraft.energy.converter.EnergyConverterProfile;
import com.masson.cruciblecraft.energy.converter.EnergyConverterTierCatalog;
import com.masson.cruciblecraft.gametest.support.GameTestRequirements;
import com.masson.cruciblecraft.energy.zpm.ZpmModule;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCover;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.registry.ModProcessingMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/** Laser, electromagnet, and ZPM decharger hosts on the energy grid. */
@GameTestHolder(LaserMagnetZpmGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class LaserMagnetZpmGameTests {
    public static final String NAMESPACE = "cruciblecraft_energy";
    private static final String TEMPLATE = "empty";

    private LaserMagnetZpmGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void catalogRowsAndLaserRecipes(GameTestHelper helper) {
        helper.assertTrue(
                EnergyConverterCatalog.profiles().stream()
                                .filter(profile ->
                                        "laser_electric".equals(
                                                profile.runtimeBinding())
                                                || "laser_absorber".equals(
                                                        profile.runtimeBinding())
                                                || "magnet_electric".equals(
                                                        profile.runtimeBinding())
                                                || "zpm_decharger".equals(
                                                        profile.runtimeBinding())
                                                || "zpm_decharger_qu".equals(
                                                        profile.runtimeBinding()))
                                .count()
                        == EnergyConverterTierCatalog.LASER_MAGNET_ZPM_SIZE,
                "Laser, magnet, and ZPM host count drifted");
        assertRecipe(helper, "steel_galvanized_laser_electric", true);
        assertRecipe(helper, "titanium_laser_electric", true);
        assertRecipe(helper, "steel_galvanized_laser_absorber", true);
        assertRecipe(helper, "titanium_electromagnet", true);
        assertRecipe(helper, "osmiridium_zpm_decharger", true);
        assertRecipe(helper, "osmiridium_zpm_decharger_qu", true);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void laserWastesEuWhenLuIsBlocked(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 1, 2);
        EnergyConverterProfile profile = placeDirected(
                helper, relative, "steel_galvanized_laser_electric", Direction.NORTH);
        DirectedWasteConverterBlockEntity laser =
                GameTestRequirements.requireBlockEntity(
                        helper, relative, DirectedWasteConverterBlockEntity.class);
        long inserted = laser.insert(
                EnergyType.ELECTRIC,
                profile.inputWindow().maximum(),
                1L,
                Direction.SOUTH,
                false);
        helper.assertTrue(inserted == 1L, "Laser rejected its input maximum");
        DirectedWasteConverterBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(relative),
                helper.getBlockState(relative),
                laser);
        helper.assertTrue(laser.stored() == 0L, "WASTE_ENERGY laser kept EU");
        helper.assertTrue(
                "blocked".equals(laser.status()),
                "Laser did not report blocked LU output");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void selectorCoverScalesLaserOutput(GameTestHelper helper) {
        BlockPos laserPos = new BlockPos(2, 1, 2);
        BlockPos absorberPos = new BlockPos(2, 1, 1);
        EnergyConverterProfile laserProfile = placeDirected(
                helper, laserPos, "steel_galvanized_laser_electric", Direction.NORTH);
        placeDirected(
                helper,
                absorberPos,
                "steel_galvanized_laser_absorber",
                Direction.NORTH);
        DirectedWasteConverterBlockEntity laser =
                GameTestRequirements.requireBlockEntity(
                        helper, laserPos, DirectedWasteConverterBlockEntity.class);
        DirectedWasteConverterBlockEntity absorber =
                GameTestRequirements.requireBlockEntity(
                        helper,
                        absorberPos,
                        DirectedWasteConverterBlockEntity.class);
        int mode = 8;
        helper.assertTrue(
                laser.setCover(
                        Direction.UP,
                        PipeCover.of("cruciblecraft:selector_tag")
                                .withDisplay(0, mode)),
                "Laser rejected the selector cover");
        helper.assertTrue(
                laser.insert(
                                EnergyType.ELECTRIC,
                                laserProfile.inputWindow().maximum(),
                                1L,
                                Direction.SOUTH,
                                false)
                        == 1L,
                "Scaled laser rejected its input maximum");
        DirectedWasteConverterBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(laserPos),
                helper.getBlockState(laserPos),
                laser);
        long outputCap = DirectedWasteConverterBlockEntity.selectorLimit(
                laserProfile.outputPacket().size() * 2L, mode, false);
        long waste = DirectedWasteConverterBlockEntity.selectorLimit(
                laserProfile.inputWindow().maximum(), mode, true);
        helper.assertTrue(laser.selectorMode() == mode, "Selector mode was not read");
        helper.assertTrue(
                outputCap == laserProfile.outputPacket().size(),
                "Mode 8 did not halve the laser maximum");
        helper.assertTrue(
                absorber.stored() == outputCap,
                "Absorber did not receive the scaled LU packet");
        helper.assertTrue(
                laser.stored()
                        == laserProfile.inputWindow().maximum() - waste,
                "Laser did not keep the unspent EU");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void laserFeedsAbsorberWhichFeedsHeater(GameTestHelper helper) {
        BlockPos laserPos = new BlockPos(2, 1, 2);
        BlockPos absorberPos = new BlockPos(2, 1, 1);
        BlockPos heaterPos = new BlockPos(2, 1, 0);
        EnergyConverterProfile laserProfile = placeDirected(
                helper, laserPos, "steel_galvanized_laser_electric", Direction.NORTH);
        EnergyConverterProfile absorberProfile = placeDirected(
                helper,
                absorberPos,
                "steel_galvanized_laser_absorber",
                Direction.NORTH);
        helper.setBlock(
                heaterPos,
                facing(
                        ModBlocks.converterBlocksById()
                                .get(id("steel_galvanized_electric_heater"))
                                .get(),
                        ElectricHeaterBlock.FACING,
                        Direction.UP));
        DirectedWasteConverterBlockEntity laser =
                GameTestRequirements.requireBlockEntity(
                        helper, laserPos, DirectedWasteConverterBlockEntity.class);
        DirectedWasteConverterBlockEntity absorber =
                GameTestRequirements.requireBlockEntity(
                        helper,
                        absorberPos,
                        DirectedWasteConverterBlockEntity.class);
        ElectricHeaterBlockEntity heater =
                GameTestRequirements.requireBlockEntity(
                        helper, heaterPos, ElectricHeaterBlockEntity.class);
        helper.assertTrue(
                laser.insert(
                                EnergyType.ELECTRIC,
                                laserProfile.inputWindow().maximum(),
                                1L,
                                Direction.SOUTH,
                                false)
                        == 1L,
                "Laser rejected EU");
        DirectedWasteConverterBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(laserPos),
                helper.getBlockState(laserPos),
                laser);
        DirectedWasteConverterBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(absorberPos),
                helper.getBlockState(absorberPos),
                absorber);
        helper.assertTrue(laser.stored() == 0L, "Laser retained EU after waste");
        helper.assertTrue(
                absorber.stored() == 0L,
                "Absorber retained LU after waste");
        helper.assertTrue(
                heater.stored() == absorberProfile.outputPacket().size(),
                "Heater did not store the absorber EU packet");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void absorberAcceptsLuOnlyOnItsBack(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 1, 2);
        EnergyConverterProfile profile = placeDirected(
                helper,
                relative,
                "steel_galvanized_laser_absorber",
                Direction.NORTH);
        DirectedWasteConverterBlockEntity absorber =
                GameTestRequirements.requireBlockEntity(
                        helper, relative, DirectedWasteConverterBlockEntity.class);
        helper.assertTrue(
                absorber.insert(
                                EnergyType.LU,
                                profile.inputPacket().size(),
                                1L,
                                Direction.NORTH,
                                false)
                        == 0L,
                "Absorber accepted LU on its front");
        helper.assertTrue(
                absorber.insert(
                                EnergyType.LU,
                                profile.inputPacket().size(),
                                1L,
                                Direction.SOUTH,
                                false)
                        == 1L,
                "Absorber rejected LU on its back");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void electromagnetFeedsPolarizer(GameTestHelper helper) {
        BlockPos magnetPos = new BlockPos(2, 1, 2);
        BlockPos polarizerPos = new BlockPos(2, 2, 2);
        EnergyConverterProfile profile = placeDirected(
                helper,
                magnetPos,
                "steel_galvanized_electromagnet",
                Direction.UP);
        helper.setBlock(
                polarizerPos,
                ModBlocks.configuredProcessingBlock(ModProcessingMachines.POLARIZER)
                        .defaultBlockState()
                        .setValue(ProcessingMachineBlock.FACING, Direction.NORTH));
        DirectedWasteConverterBlockEntity magnet =
                GameTestRequirements.requireBlockEntity(
                        helper, magnetPos, DirectedWasteConverterBlockEntity.class);
        ProcessingMachineBlockEntity polarizer =
                GameTestRequirements.requireBlockEntity(
                        helper, polarizerPos, ProcessingMachineBlockEntity.class);
        helper.assertTrue(
                magnet.insert(
                                EnergyType.ELECTRIC,
                                profile.inputPacket().size(),
                                1L,
                                Direction.NORTH,
                                false)
                        == 1L,
                "Electromagnet rejected nominal EU");
        DirectedWasteConverterBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(magnetPos),
                helper.getBlockState(magnetPos),
                magnet);
        helper.assertTrue(magnet.stored() == 0L, "Electromagnet retained EU");
        helper.assertTrue(
                polarizer.energyStored() == profile.outputPacket().size(),
                "Polarizer did not store the MU packet");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void zpmDischargesModuleAndRejectsOtherItems(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 1, 2);
        helper.setBlock(
                relative,
                facing(
                        ModBlocks.converterBlocksById()
                                .get(id("osmiridium_zpm_decharger"))
                                .get(),
                        ZpmDechargerBlock.FACING,
                        Direction.NORTH));
        ZpmDechargerBlockEntity decharger =
                GameTestRequirements.requireBlockEntity(
                        helper, relative, ZpmDechargerBlockEntity.class);
        IItemHandler items = GameTestRequirements.requireCapability(
                helper,
                Capabilities.ItemHandler.BLOCK,
                relative,
                Direction.UP,
                "ZPM decharger has no item capability");
        helper.assertTrue(
                !items.insertItem(0, Items.IRON_INGOT.getDefaultInstance(), false)
                        .isEmpty(),
                "ZPM slot accepted an item");
        helper.assertTrue(
                decharger.insert(
                                EnergyType.QUANTUM,
                                decharger.profile().inputPacket().size(),
                                1L,
                                Direction.SOUTH,
                                false)
                        == 0L,
                "Empty ZPM decharger accepted QU");
        ItemStack module = ModItems.ZERO_POINT_MODULE.get().fullStack();
        helper.assertTrue(
                items.insertItem(0, module, false).isEmpty(),
                "Full zero-point module was rejected");
        helper.assertTrue(
                decharger.insert(
                                EnergyType.QUANTUM,
                                decharger.profile().inputPacket().size(),
                                1L,
                                Direction.SOUTH,
                                false)
                        == 0L,
                "Charged module made the decharger accept QU");
        ZpmDechargerBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(relative),
                helper.getBlockState(relative),
                decharger);
        long packet = decharger.profile().inputPacket().size();
        long pulled = packet * ZpmModule.PULL_PACKETS_LOW;
        helper.assertTrue(
                BatteryCharge.get(items.getStackInSlot(0))
                        == ZpmModule.CAPACITY - pulled,
                "Module did not lose one low-buffer pull");
        helper.assertTrue(
                decharger.stored() == pulled,
                "Decharger buffer did not keep the pulled QU");
        helper.assertTrue(
                decharger.extract(
                                EnergyType.ELECTRIC,
                                packet,
                                1L,
                                Direction.NORTH,
                                false)
                        == 1L,
                "Decharger did not emit one EU packet");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void zpmQuantumEmitsQuantumFromTheModule(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 1, 2);
        helper.setBlock(
                relative,
                facing(
                        ModBlocks.converterBlocksById()
                                .get(id("osmiridium_zpm_decharger_qu"))
                                .get(),
                        ZpmDechargerBlock.FACING,
                        Direction.NORTH));
        ZpmDechargerBlockEntity decharger =
                GameTestRequirements.requireBlockEntity(
                        helper, relative, ZpmDechargerBlockEntity.class);
        IItemHandler items = GameTestRequirements.requireCapability(
                helper,
                Capabilities.ItemHandler.BLOCK,
                relative,
                Direction.UP,
                "Quantum ZPM decharger has no item capability");
        helper.assertTrue(
                items.insertItem(
                                0,
                                ModItems.ZERO_POINT_MODULE.get().fullStack(),
                                false)
                        .isEmpty(),
                "Full zero-point module was rejected");
        ZpmDechargerBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(relative),
                helper.getBlockState(relative),
                decharger);
        long packet = decharger.profile().outputPacket().size();
        helper.assertTrue(
                decharger.extract(
                                EnergyType.ELECTRIC,
                                packet,
                                1L,
                                Direction.NORTH,
                                true)
                        == 0L,
                "Quantum decharger offered EU");
        helper.assertTrue(
                decharger.extract(
                                EnergyType.QUANTUM,
                                packet,
                                1L,
                                Direction.NORTH,
                                false)
                        == 1L,
                "Quantum decharger did not emit one QU packet");
        helper.succeed();
    }

    private static EnergyConverterProfile placeDirected(
            GameTestHelper helper,
            BlockPos relative,
            String path,
            Direction facing) {
        ResourceLocation profileId = id(path);
        Block block = ModBlocks.converterBlocksById().get(profileId).get();
        helper.setBlock(
                relative,
                facing(block, DirectedWasteConverterBlock.FACING, facing));
        return EnergyConverterCatalog.require(profileId);
    }

    private static BlockState facing(
            Block block,
            net.minecraft.world.level.block.state.properties.DirectionProperty property,
            Direction facing) {
        return block.defaultBlockState().setValue(property, facing);
    }

    private static void assertRecipe(
            GameTestHelper helper,
            String path,
            boolean present) {
        boolean found = helper.getLevel().getRecipeManager().byKey(id(path)).isPresent();
        helper.assertTrue(
                found == present,
                (present ? "Missing" : "Unexpected") + " recipe " + path);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
