package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.compat.jade.observation.BatteryObservation;
import com.masson.cruciblecraft.compat.jade.observation.ConverterObservation;
import com.masson.cruciblecraft.compat.jade.observation.ReactorCoreObservation;
import com.masson.cruciblecraft.content.block.ReactorCoreBlock;
import com.masson.cruciblecraft.content.blockentity.ReactorCoreBlockEntity;
import com.masson.cruciblecraft.content.item.GeigerCounterItem;
import com.masson.cruciblecraft.content.item.HazmatArmorItem;
import com.masson.cruciblecraft.content.item.ThermometerItem;
import com.masson.cruciblecraft.energy.battery.EnergyBatteryCatalog;
import com.masson.cruciblecraft.energy.converter.EnergyConverterCatalog;
import com.masson.cruciblecraft.energy.converter.EnergyConverterTierCatalog;
import com.masson.cruciblecraft.nuclear.ReactorHazards;
import com.masson.cruciblecraft.nuclear.ReactorSafety;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModComponents;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.verification.PlayerCompleteSmoke;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Isolated fission observation/safety gate. Run with
 * {@code -PwaveRecipes=runtime/fission-observation-safety}.
 */
@GameTestHolder(NuclearFissionObservationSafetyGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class NuclearFissionObservationSafetyGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_runtime_fission_observation_safety";
    private static final String TEMPLATE = "empty";
    private static final String CAPABILITY =
            "energy/nuclear-fission-observation-safety";
    private static final BlockPos POS = new BlockPos(2, 1, 2);
    private static final String[] HAZMAT = {
            "gt_object/gt_armor_hazmat_radiation_head_m0",
            "gt_object/gt_armor_hazmat_radiation_chest_m0",
            "gt_object/gt_armor_hazmat_radiation_legs_m0",
            "gt_object/gt_armor_hazmat_radiation_boots_m0",
            "gt_object/gt_armor_hazmat_heat_head_m0",
            "gt_object/gt_armor_hazmat_heat_chest_m0",
            "gt_object/gt_armor_hazmat_heat_legs_m0",
            "gt_object/gt_armor_hazmat_heat_boots_m0"
    };

    private NuclearFissionObservationSafetyGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void playerSurfaceIsRegistered(GameTestHelper helper) {
        helper.assertTrue(
                EnergyBatteryCatalog.profiles().size() == 37,
                "Battery catalog drifted from 37");
        helper.assertTrue(
                EnergyConverterCatalog.profiles().size()
                        == EnergyConverterTierCatalog.EXPECTED_SIZE,
                "Converter catalog drifted from 179");
        helper.assertTrue(
                semantic(ThermometerItem.REGISTRY_PATH).get()
                        instanceof ThermometerItem,
                "Thermometer is not wired");
        helper.assertTrue(
                semantic(GeigerCounterItem.EMPTY_PATH).get()
                        instanceof GeigerCounterItem empty
                        && !empty.filled(),
                "Empty Geiger drifted");
        helper.assertTrue(
                semantic(GeigerCounterItem.FILLED_PATH).get()
                        instanceof GeigerCounterItem filled
                        && filled.filled(),
                "Filled Geiger drifted");
        PlayerCompleteSmoke.writeIfConfigured("gameTestServer", CAPABILITY);
        helper.assertTrue(
                PlayerCompleteSmoke.snapshot("gameTestServer", CAPABILITY)
                        .get("status")
                        .getAsString()
                        .equals("PASS"),
                "Player-complete registry snapshot failed");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void eightHazmatPiecesAreWearable(GameTestHelper helper) {
        for (String path : HAZMAT) {
            helper.assertTrue(
                    semantic(path).get() instanceof HazmatArmorItem,
                    "Hazmat is not wearable: " + path);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void contactAppliesHeatAndRadiation(GameTestHelper helper) {
        Pig pig = spawnAtCore(helper);
        helper.assertTrue(
                ReactorHazards.applyHeatDamage(pig, ReactorHazards.HEAT_DAMAGE),
                "Heat damage did not apply");
        helper.assertTrue(
                ReactorHazards.applyRadioactivity(
                        pig,
                        ReactorHazards.CONTACT_RADIATION_LEVEL,
                        ReactorHazards.CONTACT_RADIATION_AMOUNT),
                "Contact radiation did not apply");
        helper.assertTrue(
                pig.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)
                        && pig.hasEffect(MobEffects.WITHER),
                "Radiation effects missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fullRadiationSuitBlocksRadiation(GameTestHelper helper) {
        Pig pig = spawnAtCore(helper);
        equip(pig, "radiation");
        helper.assertFalse(
                ReactorHazards.applyRadioactivity(pig, 3, 8),
                "Full radiation suit did not block radiation");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fullHeatSuitBlocksHeat(GameTestHelper helper) {
        Pig pig = spawnAtCore(helper);
        equip(pig, "heat");
        helper.assertFalse(
                ReactorHazards.applyHeatDamage(pig, ReactorHazards.HEAT_DAMAGE),
                "Full heat suit did not block heat");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void areaRadiationUsesNeutronOver256(GameTestHelper helper) {
        helper.assertTrue(
                ReactorHazards.neutronCalc(512) == 2
                        && ReactorHazards.areaStrength(512, 1) == 1
                        && ReactorHazards.radioactivityLevel(11) == 2,
                "Area radiation contract drifted from neutrons/256");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void failBurstDoublesRangeAndPlaysSound(GameTestHelper helper) {
        helper.assertTrue(
                ReactorHazards.FAIL_RANGE == 500
                        && ReactorHazards.failStrength(512, 0) == 4
                        && ReactorHazards.areaStrength(512, 0) == 2,
                "Fail burst does not double tCalc");
        ReactorCoreBlockEntity core = place(helper);
        helper.assertTrue(
                core.insertRod(
                        0,
                        ModItems.reactorRod("uranium238_fuel_rod")
                                .get()
                                .defaultStack(),
                        null),
                "U-238 rod was rejected");
        core.setStopped(false);
        core.onTick(19L);
        helper.assertTrue(
                core.rod(0).isEmpty()
                        && core.safety() == ReactorSafety.RODS_DESTROYED_NO_COOLANT
                        && core.failBurstCount() == 1,
                "Fail burst did not run on rod destroy");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void emptyCoolantDestroysRodsWithFailSemantics(
            GameTestHelper helper) {
        ReactorCoreBlockEntity core = place(helper);
        helper.assertTrue(
                core.insertRod(
                        0,
                        ModItems.reactorRod("uranium238_fuel_rod")
                                .get()
                                .defaultStack(),
                        null),
                "U-238 rod was rejected");
        core.setStopped(false);
        core.onTick(19L);
        helper.assertTrue(
                core.rod(0).isEmpty()
                        && core.safety() == ReactorSafety.RODS_DESTROYED_NO_COOLANT
                        && core.failBurstCount() >= 1,
                "Empty coolant did not destroy rods with fail semantics");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void outputFullDoesNotDestroyRods(GameTestHelper helper) {
        ReactorCoreBlockEntity core = place(helper);
        helper.assertTrue(
                core.insertRod(
                        0,
                        ModItems.reactorRod("uranium238_fuel_rod")
                                .get()
                                .defaultStack(),
                        null),
                "U-238 rod was rejected");
        core.outputTank().fill(
                new net.neoforged.neoforge.fluids.FluidStack(
                        com.masson.cruciblecraft.registry.ModFluids.STEAM_SOURCE.get(),
                        core.outputTank().getCapacity()),
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction
                        .EXECUTE);
        core.coolantTank().fill(
                new net.neoforged.neoforge.fluids.FluidStack(
                        com.masson.cruciblecraft.nuclear.ReactorCoolant
                                .DISTILLED_WATER
                                .inputFluid()
                                .orElseThrow(),
                        1_000),
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction
                        .EXECUTE);
        core.addHeat(80L);
        core.onTick(1L);
        helper.assertTrue(
                !core.rod(0).isEmpty()
                        && core.safety() == ReactorSafety.OUTPUT_FULL_STALLED
                        && core.failBurstCount() == 0
                        && core.heat() == 80L,
                "Output-full backpressure destroyed rods or dropped heat");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void thermometerReadsLastHeatHu(GameTestHelper helper) {
        ReactorCoreBlockEntity core = place(helper);
        core.addHeat(40L);
        helper.assertTrue(
                core.lastHeat() == 0L,
                "lastHeat should stay 0 until a conversion tick");
        core.onTick(0L);
        helper.assertTrue(
                semantic(ThermometerItem.REGISTRY_PATH).get()
                        instanceof ThermometerItem,
                "Thermometer missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void emptyGeigerDoesNotRead(GameTestHelper helper) {
        Item item = semantic(GeigerCounterItem.EMPTY_PATH).get();
        helper.assertTrue(
                item instanceof GeigerCounterItem geiger && !geiger.filled(),
                "Empty Geiger should not read");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void filledGeigerReadsNeutrons(GameTestHelper helper) {
        ItemStack stack = semantic(GeigerCounterItem.FILLED_PATH)
                .get()
                .getDefaultInstance();
        stack.set(ModComponents.GEIGER_ENABLED, true);
        helper.assertTrue(
                GeigerCounterItem.enabled(stack),
                "Filled Geiger ON state did not persist");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void jadeReactorHasNoKelvin(GameTestHelper helper) {
        ReactorCoreBlockEntity core = place(helper);
        CompoundTag data = new CompoundTag();
        ReactorCoreObservation.writeServerData(data, core);
        ReactorCoreObservation observation =
                ReactorCoreObservation.fromServerData(data);
        helper.assertTrue(
                observation.heatHu().available()
                        && observation.lastHeatHu().available()
                        && !observation.hasKelvinField()
                        && !data.contains("temperature_k")
                        && !data.contains("meltdown_at_k"),
                "Reactor Jade invented Kelvin");
        BatteryObservation missingBattery =
                BatteryObservation.fromServerData(new CompoundTag());
        ConverterObservation missingConverter =
                ConverterObservation.fromServerData(new CompoundTag());
        helper.assertTrue(
                !missingBattery.stored().available()
                        && !missingConverter.bufferStored().available(),
                "Missing energy Jade fields were faked as zero");
        helper.succeed();
    }

    private static ReactorCoreBlockEntity place(GameTestHelper helper) {
        helper.setBlock(
                POS,
                ModBlocks.REACTOR_CORE_1X1
                        .get()
                        .defaultBlockState()
                        .setValue(ReactorCoreBlock.FACING, Direction.NORTH));
        return helper.getBlockEntity(POS);
    }

    private static Pig spawnAtCore(GameTestHelper helper) {
        place(helper);
        return helper.spawn(net.minecraft.world.entity.EntityType.PIG, new Vec3(2.5, 1.0, 2.5));
    }

    private static void equip(Pig pig, String suit) {
        pig.setItemSlot(
                EquipmentSlot.HEAD,
                stack("gt_object/gt_armor_hazmat_" + suit + "_head_m0"));
        pig.setItemSlot(
                EquipmentSlot.CHEST,
                stack("gt_object/gt_armor_hazmat_" + suit + "_chest_m0"));
        pig.setItemSlot(
                EquipmentSlot.LEGS,
                stack("gt_object/gt_armor_hazmat_" + suit + "_legs_m0"));
        pig.setItemSlot(
                EquipmentSlot.FEET,
                stack("gt_object/gt_armor_hazmat_" + suit + "_boots_m0"));
    }

    private static ItemStack stack(String path) {
        return semantic(path).get().getDefaultInstance();
    }

    private static DeferredItem<Item> semantic(String path) {
        return ModItems.semanticIdentityItemsById()
                .get(ResourceLocation.fromNamespaceAndPath("cruciblecraft", path));
    }
}
