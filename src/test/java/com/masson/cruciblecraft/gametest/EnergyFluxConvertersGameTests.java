package com.masson.cruciblecraft.gametest;

import java.util.List;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.energy.flux.FluxBlock;
import com.masson.cruciblecraft.energy.flux.FluxBlockEntity;
import com.masson.cruciblecraft.energy.flux.FluxCatalog;
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
 * Isolated flux converter gate. Run with
 * {@code -PgameTestGrid=energy}.
 */
@GameTestHolder(EnergyFluxConvertersGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class EnergyFluxConvertersGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_energy";
    private static final String TEMPLATE = "empty";
    private static final String CAPABILITY = "energy/flux-converters";
    private static final BlockPos POS = new BlockPos(2, 1, 2);

    private EnergyFluxConvertersGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void playerSurfaceIsRegistered(GameTestHelper helper) {
        helper.assertTrue(
                FluxCatalog.profiles().size() == 30,
                "Flux catalog drifted from 30 loader rows");
        helper.assertTrue(
                ModItems.fluxItemsById()
                        .get(id("flux_heater_lead"))
                        .get()
                        != null,
                "Lead flux heater item missing");
        helper.assertTrue(
                ModItems.fluxItemsById()
                        .get(id("flux_dynamo_enderium"))
                        .get()
                        != null,
                "Enderium flux dynamo item missing");
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
    public static void leadHeaterConvertsFeToHu(GameTestHelper helper) {
        FluxBlockEntity flux = place(helper, "flux_heater_lead");
        helper.assertTrue(
                flux.fluxStorage(Direction.EAST) != null
                        && flux.fluxStorage(Direction.EAST)
                                .receiveEnergy(128, false)
                                == 128
                        && flux.fluxStorage(Direction.NORTH) == null,
                "Lead flux heater must take FE on non-front faces");
        FluxBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(POS),
                helper.getBlockState(POS),
                flux);
        helper.assertTrue(
                flux.canEmit()
                        && flux.cycleOutput() == 16L
                        && flux.extract(
                                EnergyType.HEAT,
                                1L,
                                16L,
                                Direction.NORTH,
                                true)
                                == 16L
                        && flux.stored() == 0L,
                "Lead flux heater must emit 16 HU from 128 FE then waste");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void leadEngineConvertsFeToKu(GameTestHelper helper) {
        FluxBlockEntity flux = place(helper, "flux_engine_lead");
        helper.assertTrue(
                flux.fluxStorage(Direction.SOUTH) != null
                        && flux.fluxStorage(Direction.SOUTH)
                                .receiveEnergy(128, false)
                                == 128
                        && flux.fluxStorage(Direction.EAST) == null,
                "Lead flux engine must take FE on the back only");
        FluxBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(POS),
                helper.getBlockState(POS),
                flux);
        helper.assertTrue(
                flux.canEmit()
                        && flux.cycleOutput() == 16L
                        && flux.extract(
                                EnergyType.KINETIC_PUSH,
                                flux.cycleSigned(),
                                1L,
                                Direction.NORTH,
                                true)
                                == 1L
                        && flux.stored() == 0L,
                "Lead flux engine must emit 16 KU from 128 FE");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void leadMotorConvertsFeToRu(GameTestHelper helper) {
        FluxBlockEntity flux = place(helper, "flux_motor_lead");
        helper.assertTrue(
                flux.fluxStorage(Direction.EAST)
                                .receiveEnergy(128, false)
                                == 128,
                "Lead flux motor did not accept 128 FE");
        FluxBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(POS),
                helper.getBlockState(POS),
                flux);
        helper.assertTrue(
                flux.canEmit()
                        && flux.extract(
                                EnergyType.KINETIC_ROTATION,
                                16L,
                                1L,
                                Direction.NORTH,
                                true)
                                == 1L,
                "Lead flux motor must emit one 16 RU packet");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void leadMagnetConvertsFeToMu(GameTestHelper helper) {
        FluxBlockEntity flux = place(helper, "flux_magnet_lead");
        helper.assertTrue(
                flux.fluxStorage(Direction.EAST)
                                .receiveEnergy(128, false)
                                == 128,
                "Lead flux magnet did not accept 128 FE");
        FluxBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(POS),
                helper.getBlockState(POS),
                flux);
        helper.assertTrue(
                flux.canEmit()
                        && flux.extract(
                                EnergyType.MU,
                                16L,
                                1L,
                                Direction.NORTH,
                                true)
                                == 1L
                        && flux.extract(
                                EnergyType.MU,
                                -16L,
                                1L,
                                Direction.SOUTH,
                                true)
                                == 1L,
                "Lead flux magnet must emit +16 MU front and -16 MU back");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void leadLaserConvertsFeToLu(GameTestHelper helper) {
        FluxBlockEntity flux = place(helper, "flux_laser_lead");
        helper.assertTrue(
                flux.fluxStorage(Direction.EAST)
                                .receiveEnergy(128, false)
                                == 128,
                "Lead flux laser did not accept 128 FE");
        FluxBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(POS),
                helper.getBlockState(POS),
                flux);
        helper.assertTrue(
                flux.canEmit()
                        && flux.extract(
                                EnergyType.LU,
                                16L,
                                1L,
                                Direction.NORTH,
                                true)
                                == 1L,
                "Lead flux laser must emit one 16 LU packet");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void leadDynamoConvertsRuToFe(GameTestHelper helper) {
        FluxBlockEntity flux = place(helper, "flux_dynamo_lead");
        helper.assertTrue(
                flux.insert(
                        EnergyType.KINETIC_ROTATION,
                        32L,
                        1L,
                        Direction.SOUTH,
                        false)
                        == 1L
                        && flux.fluxStorage(Direction.NORTH) != null,
                "Lead flux dynamo must take RU on the back");
        FluxBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(POS),
                helper.getBlockState(POS),
                flux);
        helper.assertTrue(
                flux.canEmit()
                        && flux.cycleOutput() == 88L
                        && flux.fluxStorage(Direction.NORTH)
                                .extractEnergy(88, true)
                                == 88
                        && flux.stored() == 0L,
                "Lead flux dynamo must emit 88 FE from 32 RU then waste");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void underpoweredFeIsWastedWithoutEmit(
            GameTestHelper helper) {
        FluxBlockEntity flux = place(helper, "flux_heater_lead");
        helper.assertTrue(
                flux.fluxStorage(Direction.EAST).receiveEnergy(8, false) == 8,
                "Lead flux heater did not accept a small FE packet");
        FluxBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(POS),
                helper.getBlockState(POS),
                flux);
        helper.assertTrue(
                !flux.canEmit()
                        && flux.cycleOutput() == 1L
                        && flux.stored() == 0L
                        && "underpowered".equals(flux.status()),
                "Underpowered FE must convert below NBT_OUTPUT/2 and still waste");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void reloadPreservesBufferAndMode(GameTestHelper helper) {
        var block = ModBlocks.fluxBlocksById()
                .get(id("flux_heater_lead"))
                .get();
        helper.setBlock(
                POS,
                block.defaultBlockState()
                        .setValue(FluxBlock.FACING, Direction.NORTH));
        FluxBlockEntity flux = helper.getBlockEntity(POS);
        helper.assertTrue(flux != null, "Missing flux heater");
        flux.setMode(4);
        helper.assertTrue(
                flux.fluxStorage(Direction.EAST).receiveEnergy(128, false)
                        == 128,
                "Could not buffer FE before reload");
        var registries = helper.getLevel().registryAccess();
        CompoundTag saved = flux.saveWithoutMetadata(registries);
        helper.setBlock(POS, block.defaultBlockState());
        helper.setBlock(
                POS,
                block.defaultBlockState()
                        .setValue(FluxBlock.FACING, Direction.NORTH));
        FluxBlockEntity reloaded = helper.getBlockEntity(POS);
        helper.assertTrue(reloaded != null, "Missing reloaded flux heater");
        reloaded.loadWithComponents(saved, registries);
        helper.assertTrue(
                reloaded.stored() == 128L
                        && reloaded.mode() == 4
                        && helper.getBlockState(POS)
                                .getValue(FluxBlock.FACING)
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
                        .byKey(id("flux_heater_lead"))
                        .isPresent(),
                "Missing lead flux heater recipe");
        helper.assertTrue(
                helper.getLevel()
                        .getRecipeManager()
                        .byKey(id("flux_magnet_lead"))
                        .isEmpty(),
                "Flux magnet recipe must stay blocked until electromagnets exist");
        var rod = MaterialLookup.stack("lead", MaterialPrefixes.LONG_ROD);
        var gear = MaterialLookup.stack("lead", MaterialPrefixes.GEAR);
        var heater = new ItemStack(
                ModItems.converterItemsById()
                        .get(id("steel_galvanized_electric_heater"))
                        .get());
        List<ItemStack> heaterGrid = List.of(
                rod,
                rod.copy(),
                rod.copy(),
                rod.copy(),
                heater,
                rod.copy(),
                rod.copy(),
                rod.copy(),
                rod.copy());
        ItemStack assembled = craft(helper, 3, 3, heaterGrid);
        helper.assertTrue(
                assembled.is(
                        ModItems.fluxItemsById()
                                .get(id("flux_heater_lead"))
                                .get())
                        && assembled.getCount() == 1,
                "Lead flux heater wrap recipe missing");
        var engine = new ItemStack(
                ModItems.converterItemsById()
                        .get(id("steel_galvanized_electric_engine"))
                        .get());
        ItemStack engineAssembled = craft(
                helper,
                1,
                3,
                List.of(gear, engine, gear.copy()));
        helper.assertTrue(
                engineAssembled.is(
                        ModItems.fluxItemsById()
                                .get(id("flux_engine_lead"))
                                .get()),
                "Lead flux engine wrap recipe missing");
        helper.succeed();
    }

    private static FluxBlockEntity place(GameTestHelper helper, String path) {
        helper.setBlock(
                POS,
                ModBlocks.fluxBlocksById()
                        .get(id(path))
                        .get()
                        .defaultBlockState()
                        .setValue(FluxBlock.FACING, Direction.NORTH));
        FluxBlockEntity flux = helper.getBlockEntity(POS);
        helper.assertTrue(flux != null, "Missing flux " + path);
        return flux;
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
