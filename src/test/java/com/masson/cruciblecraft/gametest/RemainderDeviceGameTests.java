package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.energy.battery.BatteryBlockItem;
import com.masson.cruciblecraft.energy.battery.BatteryCharge;
import com.masson.cruciblecraft.energy.battery.EnergyBatteryCatalog;
import com.masson.cruciblecraft.energy.remainder.EnergyBatBoxBlock;
import com.masson.cruciblecraft.energy.remainder.EnergyBatBoxBlockEntity;
import com.masson.cruciblecraft.energy.remainder.MagicFieldAbsorberBlock;
import com.masson.cruciblecraft.energy.remainder.MagicFieldOffer;
import com.masson.cruciblecraft.energy.remainder.RemainderDevice;
import com.masson.cruciblecraft.energy.remainder.RemainderDevice.ObtainKind;
import com.masson.cruciblecraft.energy.remainder.RemainderDevices;
import com.masson.cruciblecraft.energy.remainder.SolarPanelBlock;
import com.masson.cruciblecraft.energy.remainder.SolarPanelBlockEntity;
import com.masson.cruciblecraft.gametest.support.GameTestRequirements;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Remainder solar, battery box, crystal charger, and magic absorber hosts. */
@GameTestHolder(RemainderDeviceGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class RemainderDeviceGameTests {
    public static final String NAMESPACE = "cruciblecraft_energy";
    private static final String TEMPLATE = "empty";

    private RemainderDeviceGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void recipesFollowObtainPlan(GameTestHelper helper) {
        for (RemainderDevice device : RemainderDevices.devices()) {
            boolean present = helper.getLevel().getRecipeManager()
                    .byKey(device.id())
                    .isPresent();
            ObtainKind kind = RemainderDevices.obtain(device).kind();
            if (kind == ObtainKind.BLOCKED) {
                helper.assertFalse(present, device.id() + " emitted a blocked recipe");
            } else {
                helper.assertTrue(present, device.id() + " recipe missing");
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void solarPushesEuInDaylight(GameTestHelper helper) {
        ServerLevel world = (ServerLevel) helper.getLevel();
        world.setDayTime(1000);
        world.setWeatherParameters(20000, 0, false, false);
        BlockPos panelPos = new BlockPos(2, 2, 2);
        BlockPos sinkPos = panelPos.north();
        RemainderDevice device = RemainderDevices.requireMeta(10050);
        SolarPanelBlock panel = (SolarPanelBlock) ModBlocks.remainderBlocksById()
                .get(device.id())
                .get();
        helper.setBlock(panelPos, panel.defaultBlockState().setValue(
                SolarPanelBlock.FACING, Direction.NORTH));
        placeBattery(helper, sinkPos, "cruciblecraft:lead_acid_battery_ulv");
        helper.assertTrue(
                world.canSeeSky(helper.absolutePos(panelPos.above())),
                "solar test cannot see the sky");
        SolarPanelBlockEntity solar = GameTestRequirements.requireBlockEntity(
                helper, panelPos, SolarPanelBlockEntity.class);
        solar.generate(true);
        BlockEntity sink = GameTestRequirements.requireBlockEntity(
                helper, sinkPos, BlockEntity.class);
        long stored = ((com.masson.cruciblecraft.api.energy.IEnergyHandler) sink)
                .stored(EnergyType.ELECTRIC);
        helper.assertTrue(
                stored == device.output(),
                "daylight solar did not push its output: " + stored);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void batteryBoxDischargesIntoANeighbor(GameTestHelper helper) {
        // ULV output fits the ULV lead-acid packet window.
        RemainderDevice device = RemainderDevices.requireMeta(10080);
        BlockPos boxPos = new BlockPos(2, 2, 2);
        BlockPos sinkPos = boxPos.north();
        EnergyBatBoxBlock box = placeBox(helper, boxPos, device);
        placeBattery(helper, sinkPos, "cruciblecraft:lead_acid_battery_ulv");
        EnergyBatBoxBlockEntity host = GameTestRequirements.requireBlockEntity(
                helper, boxPos, EnergyBatBoxBlockEntity.class);
        host.items().insertItem(0, chargedBattery(
                "cruciblecraft:lead_acid_battery_ulv"), false);
        host.tickAt(1L);
        long stored = ((com.masson.cruciblecraft.api.energy.IEnergyHandler)
                GameTestRequirements.requireBlockEntity(
                        helper, sinkPos, BlockEntity.class))
                .stored(EnergyType.ELECTRIC);
        helper.assertTrue(
                stored == device.output(),
                "battery box did not emit one packet: " + stored);
        long charge = BatteryCharge.get(host.items().getStackInSlot(0));
        long expected = EnergyBatteryCatalog
                .require("cruciblecraft:lead_acid_battery_ulv")
                .capacity()
                - device.output() * 40L;
        helper.assertTrue(
                charge == expected,
                "battery item did not lose 40 output packets: " + charge);
        helper.assertTrue(box.device().slots() == host.items().getSlots(),
                "battery box slot count drifted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void crystalChargerFillsAnEmptyLuCrystal(GameTestHelper helper) {
        RemainderDevice device = RemainderDevices.requireMeta(10130);
        BlockPos boxPos = new BlockPos(2, 2, 2);
        placeBox(helper, boxPos, device);
        EnergyBatBoxBlockEntity host = GameTestRequirements.requireBlockEntity(
                helper, boxPos, EnergyBatBoxBlockEntity.class);
        ItemStack empty = new ItemStack(batteryItem(
                "cruciblecraft:red_energium_crystal_ulv"));
        host.items().insertItem(0, empty, false);
        host.restoreBuffer(7L * device.input() * 40L * device.slots());
        host.tickAt(1L);
        long charge = BatteryCharge.get(host.items().getStackInSlot(0));
        helper.assertTrue(
                charge == device.input() * 40L,
                "crystal charger did not inject 40 input packets: " + charge);
        helper.assertTrue(
                device.energy() == EnergyType.LU,
                "crystal charger energy drifted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void magicAbsorberReadsEggAndSkull(GameTestHelper helper) {
        RemainderDevice device = RemainderDevices.requireMeta(10180);
        BlockPos pos = new BlockPos(2, 2, 2);
        MagicFieldAbsorberBlock block = (MagicFieldAbsorberBlock)
                ModBlocks.remainderBlocksById().get(device.id()).get();
        helper.setBlock(pos, block.defaultBlockState().setValue(
                MagicFieldAbsorberBlock.FACING, Direction.NORTH));
        helper.setBlock(pos.above(), Blocks.DRAGON_EGG.defaultBlockState());
        MagicFieldAbsorberBlock.MagicFieldAbsorberBlockEntity absorber =
                GameTestRequirements.requireBlockEntity(
                        helper,
                        pos,
                        MagicFieldAbsorberBlock.MagicFieldAbsorberBlockEntity.class);
        absorber.scan();
        helper.assertTrue(
                absorber.offer() == MagicFieldOffer.DRAGON_EGG,
                "dragon egg did not offer QU");
        helper.setBlock(pos.above(), Blocks.SKELETON_SKULL.defaultBlockState());
        absorber.scan();
        helper.assertTrue(
                absorber.offer() == MagicFieldOffer.SKULL,
                "skull did not offer TU");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void legacyLuvBoxStoresBatteries(GameTestHelper helper) {
        MteInPlaceBlock block = ModBlocks.mteInPlaceBlocksById()
                .get(RemainderDevices.requireMeta(10086).id())
                .get();
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, block.defaultBlockState().setValue(
                MteInPlaceBlock.FACING, Direction.NORTH));
        MteInPlaceBlockEntity host = GameTestRequirements.requireBlockEntity(
                helper, pos, MteInPlaceBlockEntity.class);
        helper.assertTrue(
                host.spec().kind() == MteInPlaceKind.BATTERY_BOX,
                "LuV box left the in-place host");
        helper.assertTrue(
                host.items().getSlots() == RemainderDevices.requireMeta(10086).slots(),
                "LuV box is still a one-slot dummy");
        ItemStack battery = chargedBattery("cruciblecraft:lead_acid_battery_ulv");
        ItemStack rejected = host.items().insertItem(0, battery, false);
        helper.assertTrue(rejected.isEmpty(), "LuV box rejected an EU battery");
        ItemStack cobble = host.items().insertItem(
                1, new ItemStack(Items.COBBLESTONE), false);
        helper.assertTrue(
                !cobble.isEmpty(),
                "LuV box accepted a non-battery");
        helper.succeed();
    }

    private static EnergyBatBoxBlock placeBox(
            GameTestHelper helper, BlockPos pos, RemainderDevice device) {
        EnergyBatBoxBlock box = (EnergyBatBoxBlock) ModBlocks.remainderBlocksById()
                .get(device.id())
                .get();
        helper.setBlock(pos, box.defaultBlockState().setValue(
                EnergyBatBoxBlock.FACING, Direction.NORTH));
        return box;
    }

    private static void placeBattery(
            GameTestHelper helper, BlockPos pos, String id) {
        helper.setBlock(
                pos,
                ModBlocks.batteryBlocksById()
                        .get(EnergyBatteryCatalog.require(id).id())
                        .get()
                        .defaultBlockState());
    }

    private static ItemStack chargedBattery(String id) {
        return batteryItem(id).fullStack();
    }

    private static BatteryBlockItem batteryItem(String id) {
        return ModItems.batteryItemsById()
                .get(EnergyBatteryCatalog.require(id).id())
                .get();
    }
}
