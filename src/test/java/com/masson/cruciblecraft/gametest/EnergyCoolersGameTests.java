package com.masson.cruciblecraft.gametest;

import java.util.List;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.energy.cooler.CoolerBlock;
import com.masson.cruciblecraft.energy.cooler.CoolerBlockEntity;
import com.masson.cruciblecraft.energy.cooler.CoolerCatalog;
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
 * Isolated cooler gate. Run with {@code -PwaveRecipes=runtime/cooler}.
 */
@GameTestHolder(EnergyCoolersGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class EnergyCoolersGameTests {
    public static final String NAMESPACE = "cruciblecraft_wave_runtime_cooler";
    private static final String TEMPLATE = "empty";
    private static final String CAPABILITY = "energy/cooler";
    private static final BlockPos POS = new BlockPos(2, 1, 2);

    private EnergyCoolersGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void playerSurfaceIsRegistered(GameTestHelper helper) {
        helper.assertTrue(
                CoolerCatalog.profiles().size() == 10,
                "Cooler catalog drifted from 10 loader rows");
        helper.assertTrue(
                ModItems.coolerItemsById()
                        .get(id("thermoelectric_cooler_lv"))
                        .get()
                        != null,
                "LV thermoelectric cooler item missing");
        helper.assertTrue(
                ModItems.coolerItemsById()
                        .get(id("thermofluxic_cooler_lead"))
                        .get()
                        != null,
                "Lead thermofluxic cooler item missing");
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
    public static void lvElectricConvertsEuToCuAndHu(GameTestHelper helper) {
        CoolerBlockEntity cooler = place(helper, "thermoelectric_cooler_lv");
        helper.assertTrue(
                cooler.insert(
                        EnergyType.ELECTRIC,
                        32L,
                        1L,
                        Direction.EAST,
                        false)
                        == 1L,
                "LV cooler did not accept one 32 EU packet from the side");
        CoolerBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(POS),
                helper.getBlockState(POS),
                cooler);
        helper.assertTrue(
                cooler.canEmit()
                        && cooler.cycleOutput() == 8L
                        && cooler.extract(
                                EnergyType.CU,
                                1L,
                                8L,
                                Direction.NORTH,
                                true)
                                == 8L
                        && cooler.extract(
                                EnergyType.HEAT,
                                1L,
                                8L,
                                Direction.SOUTH,
                                true)
                                == 8L
                        && cooler.stored() == 0L,
                "LV cooler must emit 8 CU front / 8 HU back then waste the buffer");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void underpoweredEuIsWastedWithoutEmit(GameTestHelper helper) {
        CoolerBlockEntity cooler = place(helper, "thermoelectric_cooler_lv");
        helper.assertTrue(
                cooler.insert(
                        EnergyType.ELECTRIC,
                        8L,
                        1L,
                        Direction.EAST,
                        false)
                        == 1L,
                "LV cooler did not accept a small EU packet");
        CoolerBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(POS),
                helper.getBlockState(POS),
                cooler);
        helper.assertTrue(
                !cooler.canEmit()
                        && cooler.cycleOutput() == 2L
                        && cooler.stored() == 0L
                        && "underpowered".equals(cooler.status()),
                "Underpowered EU must convert below NBT_OUTPUT/2 and still waste");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fluxLeadConvertsFeToCuAndHu(GameTestHelper helper) {
        CoolerBlockEntity cooler = place(helper, "thermofluxic_cooler_lead");
        helper.assertTrue(
                cooler.fluxStorage(Direction.EAST) != null
                        && cooler.fluxStorage(Direction.EAST)
                                .receiveEnergy(128, false)
                                == 128
                        && cooler.fluxStorage(Direction.NORTH) == null,
                "Lead flux cooler must take FE on the sides only");
        CoolerBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(POS),
                helper.getBlockState(POS),
                cooler);
        helper.assertTrue(
                cooler.canEmit()
                        && cooler.cycleOutput() == 8L
                        && cooler.extract(
                                EnergyType.CU,
                                1L,
                                8L,
                                Direction.NORTH,
                                true)
                                == 8L
                        && cooler.extract(
                                EnergyType.HEAT,
                                1L,
                                8L,
                                Direction.SOUTH,
                                true)
                                == 8L,
                "Lead flux cooler must emit 8 CU/HU from 128 FE");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void screwdriverModeCapsOutput(GameTestHelper helper) {
        CoolerBlockEntity cooler = place(helper, "thermoelectric_cooler_lv");
        cooler.setMode(13);
        helper.assertTrue(
                cooler.insert(
                        EnergyType.ELECTRIC,
                        32L,
                        1L,
                        Direction.EAST,
                        false)
                        == 1L,
                "LV cooler did not accept EU before mode cap");
        CoolerBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(POS),
                helper.getBlockState(POS),
                cooler);
        helper.assertTrue(
                cooler.mode() == 13
                        && cooler.cycleOutput() == 3L
                        && !cooler.canEmit(),
                "Mode 13 must cap LV output to 3 CU, below the emit floor");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void reloadPreservesBufferAndMode(GameTestHelper helper) {
        var block = ModBlocks.coolerBlocksById()
                .get(id("thermoelectric_cooler_lv"))
                .get();
        helper.setBlock(
                POS,
                block.defaultBlockState()
                        .setValue(CoolerBlock.FACING, Direction.NORTH));
        CoolerBlockEntity cooler = helper.getBlockEntity(POS);
        helper.assertTrue(cooler != null, "Missing cooler");
        cooler.setMode(4);
        helper.assertTrue(
                cooler.insert(
                        EnergyType.ELECTRIC,
                        32L,
                        1L,
                        Direction.EAST,
                        false)
                        == 1L,
                "Could not buffer EU before reload");
        var registries = helper.getLevel().registryAccess();
        CompoundTag saved = cooler.saveWithoutMetadata(registries);
        helper.setBlock(POS, block.defaultBlockState());
        helper.setBlock(
                POS,
                block.defaultBlockState()
                        .setValue(CoolerBlock.FACING, Direction.NORTH));
        CoolerBlockEntity reloaded = helper.getBlockEntity(POS);
        helper.assertTrue(reloaded != null, "Missing reloaded cooler");
        reloaded.loadWithComponents(saved, registries);
        helper.assertTrue(
                reloaded.stored() == 32L
                        && reloaded.mode() == 4
                        && helper.getBlockState(POS)
                                .getValue(CoolerBlock.FACING)
                                == Direction.NORTH,
                "Buffer, mode, or facing did not survive BlockEntity reload");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void representativeRecipesAreSurvivalCraftable(
            GameTestHelper helper) {
        helper.assertTrue(
                helper.getLevel()
                        .getRecipeManager()
                        .byKey(id("thermoelectric_cooler_lv"))
                        .isPresent(),
                "Missing LV thermoelectric cooler recipe");
        helper.assertTrue(
                helper.getLevel()
                        .getRecipeManager()
                        .byKey(id("thermofluxic_cooler_lead"))
                        .isPresent(),
                "Missing lead thermofluxic cooler recipe");
        var cable = MaterialLookup.stack("tin", MaterialPrefixes.CABLE);
        var silicon = MaterialLookup.stack("silicon", MaterialPrefixes.PLATE);
        var copper = MaterialLookup.stack("copper", MaterialPrefixes.PLATE);
        var casing = MaterialLookup.stack(
                "steel_galvanized", MaterialPrefixes.MACHINE_CASING);
        var wrench = ModItems.MATERIAL_WRENCH.get();
        var cutter = ModItems.MATERIAL_WIRE_CUTTER.get();
        List<ItemStack> electric = List.of(
                cable,
                silicon,
                new ItemStack(wrench),
                copper,
                casing,
                copper.copy(),
                new ItemStack(cutter),
                silicon.copy(),
                cable.copy());
        ItemStack assembled = craft(helper, 3, 3, electric);
        helper.assertTrue(
                assembled.is(
                        ModItems.coolerItemsById()
                                .get(id("thermoelectric_cooler_lv"))
                                .get())
                        && assembled.getCount() == 1,
                "LV thermoelectric cooler 3x3 recipe missing");
        var plate = MaterialLookup.stack("lead", MaterialPrefixes.PLATE);
        var rod = MaterialLookup.stack("lead", MaterialPrefixes.LONG_ROD);
        List<ItemStack> flux = List.of(
                plate,
                rod,
                plate.copy(),
                plate.copy(),
                assembled.copy(),
                plate.copy(),
                plate.copy(),
                rod.copy(),
                plate.copy());
        ItemStack wrapped = craft(helper, 3, 3, flux);
        helper.assertTrue(
                wrapped.is(
                        ModItems.coolerItemsById()
                                .get(id("thermofluxic_cooler_lead"))
                                .get())
                        && wrapped.getCount() == 1,
                "Lead thermofluxic cooler wrap recipe missing");
        helper.succeed();
    }

    private static CoolerBlockEntity place(GameTestHelper helper, String path) {
        helper.setBlock(
                POS,
                ModBlocks.coolerBlocksById()
                        .get(id(path))
                        .get()
                        .defaultBlockState()
                        .setValue(CoolerBlock.FACING, Direction.NORTH));
        CoolerBlockEntity cooler = helper.getBlockEntity(POS);
        helper.assertTrue(cooler != null, "Missing cooler " + path);
        return cooler;
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
