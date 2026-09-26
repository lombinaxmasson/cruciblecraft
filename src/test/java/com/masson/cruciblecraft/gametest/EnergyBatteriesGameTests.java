package com.masson.cruciblecraft.gametest;

import java.util.List;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.CableBlock;
import com.masson.cruciblecraft.content.item.BatteryCellItem;
import com.masson.cruciblecraft.energy.EnergyEmitter;
import com.masson.cruciblecraft.energy.battery.BatteryBlockEntity;
import com.masson.cruciblecraft.energy.battery.EnergyBatteryCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.verification.PlayerCompleteSmoke;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated batteries gate. Run with {@code -PgameTestGrid=energy}.
 */
@GameTestHolder(EnergyBatteriesGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class EnergyBatteriesGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_energy";
    private static final String TEMPLATE = "empty";
    private static final String CAPABILITY = "energy/batteries";
    private static final BlockPos POS = new BlockPos(2, 1, 2);
    private static final List<String> SIGNOFF_ITEMS = List.of(
            "lead_acid_battery_ulv",
            "alkaline_battery_lv",
            "nickel_cadmium_battery_mv");

    private EnergyBatteriesGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void playerSurfaceIsRegistered(GameTestHelper helper) {
        helper.assertTrue(
                EnergyBatteryCatalog.profiles().size() == 37,
                "Battery catalog drifted from 37 loader rows");
        helper.assertTrue(
                ModItems.batteryItemsById()
                        .get(id("lead_acid_battery_ulv"))
                        .get()
                        != null,
                "Lead-acid ULV battery item missing");
               helper.assertTrue(
                       ModItems.batteryCellItemsByPath().size() == 10,
                       "GT6 battery cell identity set is incomplete");
               helper.assertTrue(
                       ModItems.batteryCell("lead_acid_cell_filled").get()
                                       instanceof BatteryCellItem cell
                               && cell.filled()
                               && cell.fluidMaterial().equals("sulfuric_acid")
                               && cell.fluidAmount() == 288,
                       "Lead-acid filled cell chemistry is incorrect");
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
        public static void sourceBackedEmptyCellRecipesAreRegistered(
                GameTestHelper helper) {
            for (String family : List.of(
                    "lead_acid",
                    "alkaline",
                    "nickel_cadmium",
                    "lithium_cobalt",
                    "lithium_manganese")) {
                helper.assertTrue(
                        helper.getLevel().getRecipeManager()
                                .byKey(id("battery_cells/" + family + "_empty"))
                                .isPresent(),
                        "Missing exact empty-cell recipe " + family);
            }
            helper.succeed();
        }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void chargeStoresEu(GameTestHelper helper) {
        BatteryBlockEntity battery = place(helper, "lead_acid_battery_ulv");
        long accepted = battery.insert(
                EnergyType.ELECTRIC, 8L, 1L, Direction.UP, false);
        helper.assertTrue(
                accepted == 1L && battery.stored(EnergyType.ELECTRIC) == 8L,
                "ULV lead-acid battery did not store one EU packet");
        helper.assertTrue(
                battery.stored(EnergyType.LU) == 0L,
                "EU battery reported LU charge");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void dischargeExtractsEu(GameTestHelper helper) {
        BatteryBlockEntity battery = place(helper, "lead_acid_battery_ulv");
        helper.assertTrue(
                battery.insert(
                        EnergyType.ELECTRIC, 8L, 1L, Direction.UP, false)
                        == 1L,
                "ULV lead-acid battery rejected the charge packet");
        long extracted = battery.extract(
                EnergyType.ELECTRIC, 8L, 1L, Direction.UP, false);
        helper.assertTrue(
                extracted == 1L && battery.stored(EnergyType.ELECTRIC) == 0L,
                "ULV lead-acid battery did not discharge one EU packet");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void reloadPreservesCharge(GameTestHelper helper) {
        BatteryBlockEntity battery = place(helper, "lead_acid_battery_ulv");
        helper.assertTrue(
                battery.insert(
                        EnergyType.ELECTRIC, 8L, 1L, Direction.UP, false)
                        == 1L,
                "ULV lead-acid battery rejected the charge packet");
        var registries = helper.getLevel().registryAccess();
        CompoundTag saved = battery.saveWithoutMetadata(registries);
        helper.assertTrue(
                battery.extract(
                        EnergyType.ELECTRIC, 8L, 1L, Direction.UP, false)
                        == 1L,
                "Could not drain battery before reload");
        battery.loadWithComponents(saved, registries);
        helper.assertTrue(
                battery.stored(EnergyType.ELECTRIC) == 8L,
                "Battery charge did not survive BlockEntity reload");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void luRejectsEu(GameTestHelper helper) {
        BatteryBlockEntity crystal = place(helper, "red_energium_crystal_ulv");
        helper.assertTrue(
                crystal.insert(
                        EnergyType.ELECTRIC, 8L, 1L, Direction.UP, false)
                        == 0L
                        && crystal.stored(EnergyType.LU) == 0L,
                "LU crystal accepted EU");
        helper.assertTrue(
                crystal.insert(EnergyType.LU, 8L, 1L, Direction.UP, false)
                        == 1L
                        && crystal.stored(EnergyType.LU) == 8L,
                "LU crystal rejected LU");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void luDoesNotTravelAcrossEuCable(
            GameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(1, 1, 2);
        BlockPos cablePos = sourcePos.east();
        BlockPos targetPos = cablePos.east();
        BatteryBlockEntity source = placeAt(
                helper, sourcePos, "red_energium_crystal_ulv");
        BatteryBlockEntity target = placeAt(
                helper, targetPos, "cyan_energium_crystal_ulv");
        CableBlock cable = ModBlocks.electricalConductorBlock(
                "copper", MaterialPrefixes.CABLE).get();
        helper.setBlock(
                cablePos,
                cable.defaultBlockState()
                        .setValue(
                                CableBlock.PROPERTY_BY_DIRECTION.get(
                                        Direction.WEST),
                                true)
                        .setValue(
                                CableBlock.PROPERTY_BY_DIRECTION.get(
                                        Direction.EAST),
                                true));
        helper.assertTrue(
                source.insert(
                                EnergyType.LU,
                                8L,
                                1L,
                                Direction.EAST,
                                false)
                        == 1L,
                "LU source rejected its nominal packet");
        long delivered = EnergyEmitter.emit(
                helper.getLevel(),
                sourcePos,
                source,
                EnergyType.LU,
                Direction.EAST);
        helper.assertTrue(
                delivered == 0L
                        && source.stored(EnergyType.LU) == 8L
                        && target.stored(EnergyType.LU) == 0L,
                "LU packet crossed an EU cable instead of dedicated fiber");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void representativeRecipesAreSurvivalCraftable(
            GameTestHelper helper) {
        for (String path : SIGNOFF_ITEMS) {
            helper.assertTrue(
                    helper.getLevel().getRecipeManager().byKey(id(path)).isPresent(),
                    "Missing survival recipe " + path);
        }
        var cable = MaterialLookup.stack("lead", MaterialPrefixes.CABLE);
        var cell = ModItems.batteryCell("lead_acid_cell_filled").get();
        var plate = MaterialLookup.stack(
                "battery_alloy", MaterialPrefixes.PLATE);
        List<ItemStack> slots = List.of(
                cable,
                ItemStack.EMPTY,
                new ItemStack(cell),
                ItemStack.EMPTY,
                plate,
                ItemStack.EMPTY);
        ItemStack assembled = craft(helper, 2, 3, slots);
        helper.assertTrue(
                assembled.is(
                        ModItems.batteryItemsById()
                                .get(id("lead_acid_battery_ulv"))
                                .get())
                        && assembled.getCount() == 1,
                "ULV lead-acid battery 2-wide recipe missing");
        helper.succeed();
    }

    private static BatteryBlockEntity place(GameTestHelper helper, String path) {
        return placeAt(helper, POS, path);
    }

    private static BatteryBlockEntity placeAt(
            GameTestHelper helper,
            BlockPos pos,
            String path) {
        helper.setBlock(
                pos,
                ModBlocks.batteryBlocksById().get(id(path)).get()
                        .defaultBlockState());
        BatteryBlockEntity battery = helper.getBlockEntity(pos);
        helper.assertTrue(battery != null, "Missing battery block entity");
        return battery;
    }

    private static ItemStack craft(
            GameTestHelper helper, int width, int height, List<ItemStack> slots) {
        CraftingInput input = CraftingInput.of(width, height, slots);
        return helper.getLevel()
                .getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .map(holder -> holder.value().assemble(
                        input, helper.getLevel().registryAccess()))
                .orElse(ItemStack.EMPTY);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
