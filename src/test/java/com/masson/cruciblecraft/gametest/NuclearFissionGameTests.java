package com.masson.cruciblecraft.gametest;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.block.ReactorCoreBlock;
import com.masson.cruciblecraft.content.block.SteamEngineBlock;
import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ReactorCoreBlockEntity;
import com.masson.cruciblecraft.content.blockentity.SteamEngineBlockEntity;
import com.masson.cruciblecraft.content.item.ReactorRodItem;
import com.masson.cruciblecraft.nuclear.ReactorCoreHost;
import com.masson.cruciblecraft.nuclear.ReactorRodCatalog;
import com.masson.cruciblecraft.nuclear.ReactorRodPhysics;
import com.masson.cruciblecraft.nuclear.ReactorRodState;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModFluids;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.registry.ModProcessingMachines;
import com.masson.cruciblecraft.registry.ModRecipeMaps;
import com.masson.cruciblecraft.steam.SteamConversion;
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
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Isolated fission-survival gate. Run with {@code -PwaveRecipes=runtime/fission-survival}. */
@GameTestHolder(NuclearFissionGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class NuclearFissionGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_runtime_fission_survival";
    private static final String TEMPLATE = "empty";
    private static final String CAPABILITY = "energy/nuclear-fission-survival";
    private static final Direction FRONT = Direction.EAST;

    private NuclearFissionGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void playerSurfaceIsRegistered(GameTestHelper helper) {
        helper.assertTrue(
                ReactorRodCatalog.entries().size() == 46,
                "Reactor rod catalog drifted from 46 GT6 identities");
        helper.assertTrue(
                ModItems.CANNER.get() != null
                        && ModItems.REACTOR_CORE_1X1.get() != null
                        && ModItems.REACTOR_CORE_2X2.get() != null,
                "Canner or reactor core items are missing");
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
    public static void fortySixRodsAreRegistered(GameTestHelper helper) {
        helper.assertTrue(
                ReactorRodCatalog.entries().size() == 46,
                "Reactor rod catalog drifted from 46 GT6 identities");
        Map<ReactorRodCatalog.Kind, Integer> kinds =
                new EnumMap<>(ReactorRodCatalog.Kind.class);
        for (ReactorRodCatalog.Kind kind : ReactorRodCatalog.Kind.values()) {
            kinds.put(kind, 0);
        }
        for (ReactorRodCatalog.Entry entry : ReactorRodCatalog.entries()) {
            helper.assertTrue(
                    ModItems.reactorRod(entry.id().getPath()).get()
                            instanceof ReactorRodItem,
                    "Missing reactor rod " + entry.id());
            kinds.merge(entry.kind(), 1, Integer::sum);
        }
        helper.assertTrue(kinds.get(ReactorRodCatalog.Kind.EMPTY) == 1, "EMPTY count");
        helper.assertTrue(kinds.get(ReactorRodCatalog.Kind.ABSORBER) == 1, "ABSORBER count");
        helper.assertTrue(kinds.get(ReactorRodCatalog.Kind.REFLECTOR) == 1, "REFLECTOR count");
        helper.assertTrue(kinds.get(ReactorRodCatalog.Kind.MODERATOR) == 1, "MODERATOR count");
        helper.assertTrue(kinds.get(ReactorRodCatalog.Kind.NUCLEAR) == 17, "NUCLEAR count");
        helper.assertTrue(kinds.get(ReactorRodCatalog.Kind.DEPLETED) == 17, "DEPLETED count");
        helper.assertTrue(kinds.get(ReactorRodCatalog.Kind.BREEDER) == 4, "BREEDER count");
        helper.assertTrue(kinds.get(ReactorRodCatalog.Kind.PRODUCT) == 4, "PRODUCT count");
        helper.assertTrue(
                helper.getLevel().getRecipeManager()
                        .byKey(id("reactor_core_1x1"))
                        .isPresent(),
                "Missing 1x1 reactor core recipe");
        helper.assertTrue(
                helper.getLevel().getRecipeManager()
                        .byKey(id("reactor_core_2x2"))
                        .isPresent(),
                "Missing 2x2 reactor core recipe");
        helper.assertTrue(
                helper.getLevel().getRecipeManager()
                        .byKey(id("nuclear/uranium238_fuel_rod"))
                        .isPresent(),
                "Missing Canner U-238 fuel rod recipe");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fortyEightNuclearSourceRecipesArePresent(
            GameTestHelper helper) {
        String[] recipes = {
                "nuclear/empty_reactor_rod",
                "nuclear/neutron_absorber_rod",
                "nuclear/neutron_reflector_rod",
                "nuclear/neutron_moderator_rod",
                "nuclear/thorium232_fuel_rod",
                "nuclear/uranium238_fuel_rod",
                "nuclear/uranium235_fuel_rod",
                "nuclear/uranium233_fuel_rod",
                "nuclear/plutonium244_fuel_rod",
                "nuclear/plutonium241_fuel_rod",
                "nuclear/plutonium239_fuel_rod",
                "nuclear/plutonium243_fuel_rod",
                "nuclear/americium245_fuel_rod",
                "nuclear/americium241_fuel_rod",
                "nuclear/cobalt60_fuel_rod",
                "nuclear/cyanite_fuel_rod",
                "nuclear/yellorium_fuel_rod",
                "nuclear/blutonium_fuel_rod",
                "nuclear/ludicrite_fuel_rod",
                "nuclear/naquadria_fuel_rod",
                "nuclear/enriched_naquadah_fuel_rod",
                "nuclear/uranium238_breeder_rod",
                "nuclear/thorium232_breeder_rod",
                "nuclear/lithium_breeder_rod",
                "nuclear/naquadah_breeder_rod",
                "nuclear/thorium232_depleted_rod_recovery",
                "nuclear/uranium238_depleted_rod_recovery",
                "nuclear/uranium235_depleted_rod_recovery",
                "nuclear/uranium233_depleted_rod_recovery",
                "nuclear/plutonium244_depleted_rod_recovery",
                "nuclear/plutonium241_depleted_rod_recovery",
                "nuclear/plutonium239_depleted_rod_recovery",
                "nuclear/plutonium243_depleted_rod_recovery",
                "nuclear/americium245_depleted_rod_recovery",
                "nuclear/americium241_depleted_rod_recovery",
                "nuclear/cobalt60_depleted_rod_recovery",
                "nuclear/cyanite_depleted_rod_recovery",
                "nuclear/yellorium_depleted_rod_recovery",
                "nuclear/blutonium_depleted_rod_recovery",
                "nuclear/ludicrite_depleted_rod_recovery",
                "nuclear/naquadria_depleted_rod_recovery",
                "nuclear/enriched_naquadah_depleted_rod_recovery",
                "nuclear/uranium233_enriched_rod_recovery",
                "nuclear/plutonium239_enriched_rod_recovery",
                "nuclear/enriched_naquadah_enriched_rod_recovery",
                "nuclear/tritium_enriched_rod_unload",
                "reactor_core_1x1",
                "reactor_core_2x2"
        };
        helper.assertTrue(
                recipes.length == 48,
                "Nuclear source recipe table drifted from 48");
        for (String path : recipes) {
            helper.assertTrue(
                    helper.getLevel().getRecipeManager().byKey(id(path)).isPresent(),
                    "Missing nuclear source recipe " + path);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void insertingFuelRodStopsReactor(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(
                pos,
                ModBlocks.REACTOR_CORE_1X1.get().defaultBlockState()
                        .setValue(ReactorCoreBlock.FACING, Direction.NORTH));
        ReactorCoreBlockEntity core = helper.getBlockEntity(pos);
        helper.assertTrue(core.stopped(), "Fresh reactor core must start stopped");
        core.setStopped(false);
        ItemStack rod = ModItems.reactorRod("uranium238_fuel_rod")
                .get()
                .defaultStack();
        helper.assertTrue(
                core.insertRod(0, rod, null),
                "U-238 rod was rejected");
        helper.assertTrue(core.stopped(), "Inserting a rod must stop the core");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void unstoppedCoreEmitsNeutronsAtTick19(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(
                pos,
                ModBlocks.REACTOR_CORE_1X1.get().defaultBlockState()
                        .setValue(ReactorCoreBlock.FACING, Direction.NORTH));
        ReactorCoreBlockEntity core = helper.getBlockEntity(pos);
        fillDistilledWater(core, 1_000);
        core.insertRod(
                0,
                ModItems.reactorRod("uranium238_fuel_rod").get().defaultStack(),
                null);
        core.setStopped(false);
        core.onTick(19L);
        helper.assertTrue(
                core.neutrons(0) > 0,
                "U-238 rod did not emit on the 20-tick boundary");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void reflectorNeighborReturnsNeutrons(GameTestHelper helper) {
        BlockPos fuelPos = new BlockPos(2, 1, 2);
        BlockPos reflectorPos = fuelPos.north();
        helper.setBlock(
                fuelPos,
                ModBlocks.REACTOR_CORE_1X1.get().defaultBlockState()
                        .setValue(ReactorCoreBlock.FACING, Direction.NORTH));
        helper.setBlock(
                reflectorPos,
                ModBlocks.REACTOR_CORE_1X1.get().defaultBlockState()
                        .setValue(ReactorCoreBlock.FACING, Direction.SOUTH));
        ReactorCoreBlockEntity fuel = helper.getBlockEntity(fuelPos);
        ReactorCoreBlockEntity reflector = helper.getBlockEntity(reflectorPos);
        fillDistilledWater(fuel, 1_000);
        fillDistilledWater(reflector, 1_000);
        fuel.insertRod(
                0,
                ModItems.reactorRod("uranium238_fuel_rod").get().defaultStack(),
                null);
        reflector.insertRod(
                0,
                ModItems.reactorRod("neutron_reflector_rod").get().defaultStack(),
                null);
        fuel.setStopped(false);
        reflector.setStopped(false);
        fuel.onTick(19L);
        helper.assertTrue(
                fuel.neutrons(0) > 4,
                "Reflector neighbor did not bounce neutrons back");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void distilledWaterProducesSteam(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(
                pos,
                ModBlocks.REACTOR_CORE_1X1.get().defaultBlockState()
                        .setValue(ReactorCoreBlock.FACING, Direction.NORTH));
        ReactorCoreBlockEntity core = helper.getBlockEntity(pos);
        fillDistilledWater(core, 10_000);
        core.insertRod(
                0,
                ModItems.reactorRod("uranium238_fuel_rod").get().defaultStack(),
                null);
        core.setStopped(false);
        for (long tick = 0L; tick < 200L; tick++) {
            core.onTick(tick);
        }
        helper.assertTrue(
                core.outputTank().getFluidAmount() > 0,
                "Reactor heat did not convert distilled water into steam");
        helper.assertTrue(
                core.outputTank().getFluid().is(ModFluids.STEAM_SOURCE.get()),
                "Reactor hot output is not steam");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void cannerFillsUranium238FuelRod(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity canner = placeCanner(
                helper, new BlockPos(3, 2, 3));
        GTRecipe recipe = requireRecipe(
                ModRecipeMaps.CANNER, "nuclear/uranium238_fuel_rod");
        OrdinaryClosureHostGameTests.fillEnergy(helper, canner);
        OrdinaryClosureHostGameTests.loadRecipeInputs(canner, recipe);
        OrdinaryClosureHostGameTests.fillEnergy(helper, canner);
        helper.startSequence()
                .thenExecuteFor(4, () ->
                        OrdinaryClosureHostGameTests.fillEnergy(helper, canner))
                .thenExecute(() -> {
                    helper.assertTrue(
                            canner.workProgressLong() > 0L
                                    || canner.duration() > 0
                                    || OrdinaryClosureHostGameTests.hasAnyOutput(canner),
                            "Canner did not select U-238 fuel: "
                                    + canner.pausedReason());
                    OrdinaryClosureHostGameTests.forceLastTick(helper, canner);
                    OrdinaryClosureHostGameTests.fillEnergy(helper, canner);
                })
                .thenExecuteFor(2, () ->
                        OrdinaryClosureHostGameTests.fillEnergy(helper, canner))
                .thenExecute(() -> {
                    ItemStack output = canner.inventory().getStackInSlot(
                            canner.spec().items().outputs().getFirst());
                    helper.assertTrue(
                            output.is(ModItems.reactorRod("uranium238_fuel_rod").get()),
                            "Canner did not emit a U-238 fuel rod: "
                                    + canner.pausedReason());
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void cannerUnloadsTritiumAndKeepsProgressAcrossReload(
            GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity canner = placeCanner(
                helper, new BlockPos(3, 2, 3));
        GTRecipe recipe = requireRecipe(
                ModRecipeMaps.CANNER, "nuclear/tritium_enriched_rod_unload");
        int inputSlot = canner.spec().items().inputs().getFirst();
        OrdinaryClosureHostGameTests.fillEnergy(helper, canner);
        OrdinaryClosureHostGameTests.loadRecipeInputs(canner, recipe);
        OrdinaryClosureHostGameTests.fillEnergy(helper, canner);
        helper.startSequence()
                .thenExecuteFor(1, () ->
                        OrdinaryClosureHostGameTests.fillEnergy(helper, canner))
                .thenExecute(() -> {
                    helper.assertTrue(
                            canner.duration() > 0 && canner.workProgressLong() > 0L,
                            "Canner did not select tritium unload: "
                                    + canner.pausedReason());
                    helper.assertTrue(
                            !canner.inventory().getStackInSlot(inputSlot).isEmpty(),
                            "Tritium rod vanished before mid-progress NBT save");
                    CompoundTag saved = canner.saveWithoutMetadata(
                            helper.getLevel().registryAccess());
                    int persisted = canner.progress();
                    canner.loadWithComponents(
                            saved, helper.getLevel().registryAccess());
                    helper.assertTrue(
                            canner.progress() == persisted,
                            "Canner NBT load lost displayed tritium-unload progress");
                    helper.assertTrue(
                            !canner.inventory().getStackInSlot(inputSlot).isEmpty(),
                            "Canner NBT load lost the tritium rod");
                })
                .thenExecuteFor(5, () ->
                        OrdinaryClosureHostGameTests.fillEnergy(helper, canner))
                .thenExecute(() -> {
                    ItemStack empty = canner.inventory().getStackInSlot(
                            canner.spec().items().outputs().getFirst());
                    helper.assertTrue(
                            empty.is(ModItems.reactorRod("empty_reactor_rod").get()),
                            "Tritium unload did not return an empty rod");
                    var tritium = ModFluids.materialFluid("tritium").orElseThrow();
                    helper.assertTrue(
                            canner.spec().fluids().outputs().stream().anyMatch(tank ->
                                    canner.tanks().get(tank.index()).getFluidAmount()
                                            == 500
                                            && canner.tanks().get(tank.index())
                                                    .getFluid()
                                                    .is(tritium)),
                            "Tritium unload did not emit 500 mB tritium");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void extruderMakesEmptyZirconiumRod(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity extruder =
                OrdinaryClosureHostGameTests.place(
                        helper,
                        new BlockPos(3, 2, 3),
                        ModBlocks.INVAR_EXTRUDER.get(),
                        ModProcessingMachines.EXTRUDER);
        GTRecipe recipe = requireRecipe(
                ModRecipeMaps.EXTRUDER, "nuclear/empty_reactor_rod");
        OrdinaryClosureHostGameTests.fillEnergy(helper, extruder);
        OrdinaryClosureHostGameTests.loadRecipeInputs(extruder, recipe);
        OrdinaryClosureHostGameTests.fillEnergy(helper, extruder);
        helper.startSequence()
                .thenExecuteFor(40, () ->
                        OrdinaryClosureHostGameTests.fillEnergy(helper, extruder))
                .thenExecute(() -> helper.assertTrue(
                        OrdinaryClosureHostGameTests.hasAnyOutput(extruder)
                                && extruder.inventory().getStackInSlot(
                                        extruder.spec().items().outputs().getFirst())
                                        .is(ModItems.reactorRod("empty_reactor_rod").get()),
                        "Extruder did not emit empty_reactor_rod: "
                                + extruder.pausedReason()
                                + " work=" + extruder.workProgressLong()
                                + "/" + extruder.workRequiredLong()))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void centrifugeRecoversDepletedUranium238(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity centrifuge =
                OrdinaryClosureHostGameTests.place(
                        helper,
                        new BlockPos(3, 2, 3),
                        ModBlocks.CENTRIFUGE.get(),
                        ModProcessingMachines.CENTRIFUGE);
        GTRecipe recipe = requireRecipe(
                ModRecipeMaps.CENTRIFUGE,
                "nuclear/uranium238_depleted_rod_recovery");
        OrdinaryClosureHostGameTests.fillEnergy(helper, centrifuge);
        OrdinaryClosureHostGameTests.loadRecipeInputs(centrifuge, recipe);
        OrdinaryClosureHostGameTests.fillEnergy(helper, centrifuge);
        helper.startSequence()
                .thenExecuteFor(4, () ->
                        OrdinaryClosureHostGameTests.fillEnergy(helper, centrifuge))
                .thenExecute(() -> {
                    helper.assertTrue(
                            centrifuge.duration() > 0
                                    || OrdinaryClosureHostGameTests.hasAnyOutput(
                                            centrifuge),
                            "Centrifuge did not select depleted U-238 recovery: "
                                    + centrifuge.pausedReason());
                    OrdinaryClosureHostGameTests.forceLastTick(helper, centrifuge);
                    OrdinaryClosureHostGameTests.fillEnergy(helper, centrifuge);
                })
                .thenExecuteFor(2, () ->
                        OrdinaryClosureHostGameTests.fillEnergy(helper, centrifuge))
                .thenExecute(() -> helper.assertTrue(
                        OrdinaryClosureHostGameTests.hasAnyOutput(centrifuge),
                        "Depleted U-238 recovery produced no output: "
                                + centrifuge.pausedReason()))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void lowDurabilityFuelBecomesDepleted(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(
                pos,
                ModBlocks.REACTOR_CORE_1X1.get().defaultBlockState()
                        .setValue(ReactorCoreBlock.FACING, Direction.NORTH));
        ReactorCoreBlockEntity core = helper.getBlockEntity(pos);
        fillDistilledWater(core, 10_000);
        ItemStack rod = ModItems.reactorRod("uranium238_fuel_rod").get().defaultStack();
        ReactorRodItem.writeState(rod, ReactorRodState.fresh(1L));
        core.insertRod(0, rod, null);
        core.setStopped(false);
        core.onTick(19L);
        helper.assertTrue(
                core.rod(0).is(ModItems.reactorRod("uranium238_depleted_rod").get()),
                "Near-spent U-238 rod did not transform into the depleted identity");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void breederProductTransformUsesExactTarget(GameTestHelper helper) {
        ItemStack[] holder = new ItemStack[1];
        holder[0] = ModItems.reactorRod("uranium238_breeder_rod").get().defaultStack();
        ReactorRodItem.writeState(holder[0], ReactorRodState.fresh(1L));
        ReactorCoreHost host = new ReactorCoreHost() {
            @Override
            public com.masson.cruciblecraft.nuclear.ReactorCoolant coolant() {
                return null;
            }

            @Override
            public int oldNeutrons(int slot) {
                return 10_000;
            }

            @Override
            public void addNeutrons(int slot, long amount) {
            }

            @Override
            public void addHeat(long amount) {
            }

            @Override
            public void replaceRod(int slot, ItemStack replacement) {
                holder[0] = replacement;
            }
        };
        helper.assertTrue(
                ReactorRodPhysics.react(host, 0, holder[0]),
                "Breeder react returned false");
        helper.assertTrue(
                holder[0].is(ModItems.reactorRod("plutonium239_enriched_rod").get()),
                "U-238 breeder did not yield the GT6 plutonium-239 product rod");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void twoByTwoCoreCraftsFromExactParts(GameTestHelper helper) {
        ItemStack circuit = new ItemStack(
                ModItems.technologicalPart("circuit_master").get());
        ItemStack piston = new ItemStack(
                ModItems.technologicalPart("compact_electric_piston_ev").get());
        ItemStack casing = new ItemStack(
                MaterialLookup.item("lead", MaterialPrefixes.MACHINE_CASING_DENSE)
                        .orElseThrow());
        List<ItemStack> slots = List.of(
                piston, circuit, piston,
                circuit, casing, circuit,
                piston, circuit, piston);
        ItemStack assembled = craft(helper, slots);
        helper.assertTrue(
                assembled.is(ModItems.REACTOR_CORE_2X2.get())
                        && assembled.getCount() == 1,
                "2x2 reactor core did not assemble from GT6 parts");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void lvCannerCraftsFromExactParts(GameTestHelper helper) {
        helper.assertTrue(
                helper.getLevel().getRecipeManager()
                        .byKey(id("machines/canner"))
                        .isPresent(),
                "Missing source-backed LV Canner recipe");
        ItemStack pipe = new ItemStack(
                MaterialLookup.item(
                        "stainless_steel", MaterialPrefixes.TINY_FLUID_PIPE)
                        .orElseThrow());
        ItemStack pump = new ItemStack(
                ModItems.compactElectricCover("compact_electric_pump_lv").get());
        ItemStack casing = new ItemStack(
                MaterialLookup.item(
                        "steel_galvanized", MaterialPrefixes.MACHINE_CASING)
                        .orElseThrow());
        ItemStack circuit = new ItemStack(
                ModItems.technologicalPart("circuit_basic").get());
        ItemStack cable = new ItemStack(
                MaterialLookup.item("tin", MaterialPrefixes.CABLE).orElseThrow());
        List<ItemStack> slots = List.of(
                new ItemStack(ModItems.MATERIAL_WRENCH.get()),
                pipe,
                new ItemStack(ModItems.SMITHING_HAMMER.get()),
                pump,
                casing,
                pump,
                circuit,
                pipe,
                cable);
        ItemStack assembled = craft(helper, slots);
        helper.assertTrue(
                assembled.is(ModItems.CANNER.get()) && assembled.getCount() == 1,
                "LV Canner did not assemble from GT6 20161 parts");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void uranium238PlayerPathMakesSteamForExistingEngine(
            GameTestHelper helper) {
        BlockPos corePos = new BlockPos(2, 1, 2);
        BlockPos enginePos = new BlockPos(4, 1, 2);
        helper.setBlock(
                corePos,
                ModBlocks.REACTOR_CORE_2X2.get().defaultBlockState()
                        .setValue(ReactorCoreBlock.FACING, Direction.NORTH));
        helper.setBlock(
                enginePos,
                ModBlocks.BRONZE_STEAM_ENGINE.get().defaultBlockState()
                        .setValue(SteamEngineBlock.FACING, Direction.EAST));
        ReactorCoreBlockEntity core = helper.getBlockEntity(corePos);
        SteamEngineBlockEntity engine = helper.getBlockEntity(enginePos);
        fillDistilledWater(core, 10_000);
        core.insertRod(
                0,
                ModItems.reactorRod("uranium238_fuel_rod").get().defaultStack(),
                null);
        core.setStopped(false);
        for (long tick = 0L; tick < 200L; tick++) {
            core.onTick(tick);
        }
        helper.assertTrue(
                core.outputTank().getFluidAmount()
                        >= SteamConversion.ENGINE_STEAM_PER_BATCH
                        && core.outputTank().getFluid().is(ModFluids.STEAM_SOURCE.get()),
                "U-238 player path did not produce usable steam");
        IFluidHandler steamInput = engine.fluids(Direction.WEST);
        helper.assertTrue(steamInput != null, "Steam engine input missing");
        int moved = Math.min(core.outputTank().getFluidAmount(), 16_000);
        helper.assertTrue(
                steamInput.fill(
                        new FluidStack(ModFluids.STEAM_SOURCE.get(), moved),
                        IFluidHandler.FluidAction.EXECUTE)
                        == moved,
                "Could not feed reactor steam into the existing steam engine");
        SteamEngineBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(enginePos),
                helper.getBlockState(enginePos),
                engine);
        helper.assertTrue(
                engine.stored() >= SteamConversion.KU_PER_ENGINE_BATCH,
                "Existing steam engine did not convert reactor steam into KU");
        helper.succeed();
    }

    private static ConfiguredProcessingMachineBlockEntity placeCanner(
            GameTestHelper helper, BlockPos pos) {
        helper.setBlock(
                pos,
                ModBlocks.CANNER.get().defaultBlockState().setValue(
                        ProcessingMachineBlock.FACING, FRONT));
        ConfiguredProcessingMachineBlockEntity machine = helper.getBlockEntity(pos);
        helper.assertTrue(
                machine.spec() == ModProcessingMachines.CANNER
                        || machine.variant().kind().behavior()
                                == ModProcessingMachines.CANNER,
                "Placed block resolved wrong canner kind");
        helper.assertTrue(
                machine.spec().energy().type() == EnergyType.ELECTRIC,
                "LV Canner is not ELECTRIC");
        return machine;
    }

    private static GTRecipe requireRecipe(RecipeMap map, String path) {
        return map.entries().stream()
                .filter(entry -> entry.id().getPath().equals(path))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing live recipe " + map.id() + "/" + path))
                .recipe();
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

    private static void fillDistilledWater(ReactorCoreBlockEntity core, int amount) {
        var fluid = ModFluids.materialFluid("water_distilled").orElseThrow();
        core.coolantTank().fill(
                new FluidStack(fluid, amount),
                IFluidHandler.FluidAction.EXECUTE);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
