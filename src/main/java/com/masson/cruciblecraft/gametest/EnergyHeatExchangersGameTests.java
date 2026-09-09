package com.masson.cruciblecraft.gametest;

import java.util.List;

import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.energy.heatexchanger.HeatExchangerBlock;
import com.masson.cruciblecraft.energy.heatexchanger.HeatExchangerBlockEntity;
import com.masson.cruciblecraft.energy.heatexchanger.HeatExchangerCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModFluids;
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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated heat-exchanger gate. Run with
 * {@code -PwaveRecipes=runtime/heat-exchangers}.
 */
@GameTestHolder(EnergyHeatExchangersGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class EnergyHeatExchangersGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_runtime_heat_exchangers";
    private static final String TEMPLATE = "empty";
    private static final String CAPABILITY = "energy/heat-exchangers";
    private static final BlockPos POS = new BlockPos(2, 1, 2);
    private static final List<String> SIGNOFF_ITEMS = List.of(
            "heat_exchanger_invar",
            "heat_exchanger_tungsten",
            "heat_exchanger_tungstensteel",
            "heat_exchanger_tantalum_hafnium_carbide",
            "dense_heat_exchanger_invar",
            "dense_heat_exchanger_tungsten",
            "dense_heat_exchanger_tungstensteel",
            "dense_heat_exchanger_tantalum_hafnium_carbide");

    private EnergyHeatExchangersGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void playerSurfaceIsRegistered(GameTestHelper helper) {
        helper.assertTrue(
                HeatExchangerCatalog.profiles().size() == 8,
                "Heat exchanger catalog drifted from 8 loader rows");
        helper.assertTrue(
                ModItems.heatExchangerItemsById()
                        .get(id("heat_exchanger_invar"))
                        .get()
                        != null,
                "Invar heat exchanger item missing");
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
    public static void invarConsumesHotTinAndBuffersHu(GameTestHelper helper) {
        HeatExchangerBlockEntity exchanger = place(helper, "heat_exchanger_invar");
        helper.assertTrue(
                exchanger.fillInput(hotTin(1)),
                "Could not fill invar heat exchanger with hot tin");
        HeatExchangerBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(POS),
                helper.getBlockState(POS),
                exchanger);
        helper.assertTrue(
                exchanger.energyStored() == 40L
                        && exchanger.inputAmount() == 0
                        && exchanger.outputAmount() == 1
                        && exchanger.outputFluid().is(
                                ModFluids.materialFluid("tin").orElseThrow()),
                "Invar heat exchanger did not buffer 40 HU from 1 mB hot tin");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void tungstensteelEfficiencyIsNinetyPercent(
            GameTestHelper helper) {
        HeatExchangerBlockEntity exchanger = place(
                helper, "heat_exchanger_tungstensteel");
        helper.assertTrue(
                exchanger.fillInput(hotTin(1)),
                "Could not fill tungstensteel heat exchanger with hot tin");
        HeatExchangerBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(POS),
                helper.getBlockState(POS),
                exchanger);
        helper.assertTrue(
                exchanger.energyStored() == 36L
                        && exchanger.inputAmount() == 0
                        && exchanger.outputAmount() == 1,
                "Tungstensteel heat exchanger must yield 36 HU at 90%");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void reloadPreservesTanksAndEnergy(GameTestHelper helper) {
        var block = ModBlocks.heatExchangerBlocksById()
                .get(id("heat_exchanger_invar"))
                .get();
        helper.setBlock(
                POS,
                block.defaultBlockState()
                        .setValue(HeatExchangerBlock.FACING, Direction.NORTH));
        HeatExchangerBlockEntity exchanger = helper.getBlockEntity(POS);
        helper.assertTrue(exchanger != null, "Missing heat exchanger");
        helper.assertTrue(
                exchanger.fillInput(hotTin(2)),
                "Could not fill tanks before reload");
        HeatExchangerBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(POS),
                helper.getBlockState(POS),
                exchanger);
        helper.assertTrue(
                exchanger.energyStored() == 40L && exchanger.outputAmount() == 1,
                "Need buffered HU and exhaust before reload");
        var registries = helper.getLevel().registryAccess();
        CompoundTag saved = exchanger.saveWithoutMetadata(registries);
        helper.setBlock(POS, block.defaultBlockState());
        helper.setBlock(
                POS,
                block.defaultBlockState()
                        .setValue(HeatExchangerBlock.FACING, Direction.NORTH));
        HeatExchangerBlockEntity reloaded = helper.getBlockEntity(POS);
        helper.assertTrue(reloaded != null, "Missing reloaded heat exchanger");
        reloaded.loadWithComponents(saved, registries);
        helper.assertTrue(
                reloaded.energyStored() == 40L
                        && reloaded.inputAmount() == 1
                        && reloaded.outputAmount() == 1
                        && helper.getBlockState(POS)
                                .getValue(HeatExchangerBlock.FACING)
                                == Direction.NORTH,
                "Tanks, HU, or facing did not survive BlockEntity reload");
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
        var plate = MaterialLookup.item("lead", MaterialPrefixes.PLATE)
                .orElseThrow();
        var copper = MaterialLookup.item(
                        "copper", MaterialPrefixes.DOUBLE_PLATE)
                .orElseThrow();
        var pipe = MaterialLookup.item(
                        "copper", MaterialPrefixes.SMALL_FLUID_PIPE)
                .orElseThrow();
        var casing = MaterialLookup.item(
                        "invar", MaterialPrefixes.MACHINE_CASING)
                .orElseThrow();
        var wrench = ModItems.MATERIAL_WRENCH.get();
        List<ItemStack> slots = List.of(
                new ItemStack(plate),
                new ItemStack(copper),
                new ItemStack(plate),
                new ItemStack(pipe),
                new ItemStack(wrench),
                new ItemStack(pipe),
                new ItemStack(plate),
                new ItemStack(casing),
                new ItemStack(plate));
        ItemStack assembled = craft(helper, 3, 3, slots);
        helper.assertTrue(
                assembled.is(
                        ModItems.heatExchangerItemsById()
                                .get(id("heat_exchanger_invar"))
                                .get())
                        && assembled.getCount() == 1,
                "Invar heat exchanger 3x3 recipe missing");
        helper.succeed();
    }

    private static HeatExchangerBlockEntity place(
            GameTestHelper helper, String path) {
        helper.setBlock(
                POS,
                ModBlocks.heatExchangerBlocksById()
                        .get(id(path))
                        .get()
                        .defaultBlockState()
                        .setValue(HeatExchangerBlock.FACING, Direction.NORTH));
        HeatExchangerBlockEntity exchanger = helper.getBlockEntity(POS);
        helper.assertTrue(exchanger != null, "Missing heat exchanger " + path);
        return exchanger;
    }

    private static FluidStack hotTin(int amount) {
        return new FluidStack(
                ModFluids.hotSource("hot_molten_tin").orElseThrow(), amount);
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
