package com.masson.cruciblecraft.gametest;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineIoFaces;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.registry.ModMachineVariants;
import com.masson.cruciblecraft.registry.ModProcessingMachines;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated machine runtime gate. Run with {@code -PwaveRecipes=machines}.
 */
@GameTestHolder(MachineRuntimeGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class MachineRuntimeGameTests {
    public static final String NAMESPACE = "cruciblecraft_wave_machines";
    private static final String TEMPLATE = "empty";
    private static final Direction FRONT = Direction.EAST;

    private MachineRuntimeGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void targetCatalogAndRuntimeIdsMatch(GameTestHelper helper) {
        Set<String> catalog = catalogIds(false);
        Set<String> generic = catalogIds(true);
        Set<String> blocks = BuiltInRegistries.BLOCK.keySet().stream()
                .filter(id -> "cruciblecraft".equals(id.getNamespace()))
                .map(ResourceLocation::toString)
                .collect(Collectors.toCollection(TreeSet::new));
        Set<String> items = BuiltInRegistries.ITEM.keySet().stream()
                .filter(id -> "cruciblecraft".equals(id.getNamespace()))
                .map(ResourceLocation::toString)
                .collect(Collectors.toCollection(TreeSet::new));
        Set<String> variants = ModMachineVariants.ALL.stream()
                .map(variant -> variant.id().toString())
                .collect(Collectors.toCollection(TreeSet::new));
        helper.assertTrue(
                catalog.containsAll(generic) && catalog.size() == 298,
                "Machine catalog is not the 298-row GT6 host target");
        helper.assertTrue(
                variants.equals(generic),
                "generic catalog rows drifted from ModMachineVariants.ALL");
        helper.assertTrue(
                blocks.containsAll(catalog) && items.containsAll(catalog),
                "runtime block/item registries missing catalog ids");
        helper.assertTrue(
                ModMachineVariants.isOpening(
                        ResourceLocation.parse("cruciblecraft:centrifuge")),
                "opening centrifuge id is no longer marked opening");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void openingCentrifugeStillLoads(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity machine = place(
                helper,
                new BlockPos(2, 2, 2),
                ModBlocks.CENTRIFUGE.get(),
                ModProcessingMachines.CENTRIFUGE);
        helper.assertTrue(
                machine.variant().id().toString().equals("cruciblecraft:centrifuge"),
                "Opening centrifuge id drifted: " + machine.variant().id());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void materialLatheExecutes(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity lathe = place(
                helper,
                new BlockPos(3, 2, 3),
                ModBlocks.STEEL_LATHE.get(),
                ModProcessingMachines.LATHE);
        GTRecipe recipe = firstPowered(
                ModRecipeMaps.LATHE, lathe.variant().tierBand().inputMaximum());
        loadRecipeInputs(lathe, recipe);
        helper.assertTrue(
                lathe.insert(
                        EnergyType.KINETIC_ROTATION,
                        lathe.variant().tierBand().inputNominal(),
                        4L,
                        energySide(lathe),
                        false) > 0L,
                "Steel lathe rejected RU");
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        lathe.workProgressLong() > 0L
                                || hasAnyOutput(lathe),
                        "Steel lathe did not execute: "
                                + lathe.pausedReason()))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void euElectrolyzerVoltageWindow(GameTestHelper helper) {
        BlockPos under = new BlockPos(2, 2, 2);
        BlockPos normal = new BlockPos(5, 2, 2);
        BlockPos over = new BlockPos(8, 2, 2);
        ConfiguredProcessingMachineBlockEntity underpowered = place(
                helper, under, ModBlocks.ELECTROLYZER.get(),
                ModProcessingMachines.ELECTROLYZER);
        ConfiguredProcessingMachineBlockEntity ok = place(
                helper, normal, ModBlocks.ELECTROLYZER.get(),
                ModProcessingMachines.ELECTROLYZER);
        ConfiguredProcessingMachineBlockEntity overcharged = place(
                helper, over, ModBlocks.ELECTROLYZER.get(),
                ModProcessingMachines.ELECTROLYZER);
        GTRecipe salt = requireRecipe(
                ModRecipeMaps.ELECTROLYZER, "chemical/electrolyzer/salt");
        loadRecipeInputs(underpowered, salt);
        loadRecipeInputs(ok, salt);
        loadRecipeInputs(overcharged, salt);
        helper.assertTrue(
                underpowered.insert(
                        EnergyType.ELECTRIC, 8L, 1L, energySide(underpowered), false)
                        == 1L,
                "Opening electrolyzer rejected undersize EU packet");
        helper.assertTrue(
                ok.insert(
                        EnergyType.ELECTRIC,
                        salt.eut(),
                        8L,
                        energySide(ok),
                        false) > 0L,
                "Opening electrolyzer rejected in-window EU");
        helper.assertTrue(
                overcharged.insert(
                        EnergyType.ELECTRIC, 512L, 1L, energySide(overcharged), false)
                        == 1L
                        && overcharged.runtime().status().equals("overcharged"),
                "Oversized EU was not observed as overcharge: "
                        + overcharged.runtime().status());
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        underpowered.workProgressLong() == 0L
                                && ok.workProgressLong() > 0L,
                        "EU voltage window failed: under="
                                + underpowered.pausedReason()
                                + "/"
                                + underpowered.workProgressLong()
                                + " ok="
                                + ok.pausedReason()
                                + "/"
                                + ok.workProgressLong()
                                + " over="
                                + overcharged.pausedReason()))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void tuCoagulatorExecutes(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity machine = place(
                helper,
                new BlockPos(3, 2, 3),
                ModBlocks.COAGULATOR.get(),
                ModProcessingMachines.COAGULATOR);
        GTRecipe recipe = requireRecipe(
                ModRecipeMaps.COAGULATOR, "machine/bootstrap/coagulator/water_bootstrap");
        loadRecipeInputs(machine, recipe);
        helper.startSequence()
                .thenIdle(1)
                .thenExecute(() -> forceLastTick(helper, machine))
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        hasAnyOutput(machine),
                        "Coagulator TIME host did not complete: "
                                + machine.pausedReason()))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void roasterPlacesAndReads(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity machine = place(
                helper,
                new BlockPos(2, 2, 2),
                ModBlocks.STEEL_ROASTER.get(),
                ModProcessingMachines.ROASTER);
        helper.assertTrue(
                machine.variant().id().toString().equals(
                        "cruciblecraft:steel_roaster"),
                "Steel roaster variant drifted: " + machine.variant().id());
        helper.assertTrue(
                machine.variant().kind().behavior().energy().type()
                        == EnergyType.HEAT,
                "Roaster is not a HU host");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void saveReloadPreservesOpeningVariant(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity machine = place(
                helper,
                new BlockPos(4, 2, 4),
                ModBlocks.CENTRIFUGE.get(),
                ModProcessingMachines.CENTRIFUGE);
        CompoundTag persisted = machine.saveWithoutMetadata(
                helper.getLevel().registryAccess());
        machine.loadWithComponents(
                persisted, helper.getLevel().registryAccess());
        helper.assertTrue(
                machine.variant().id().toString().equals("cruciblecraft:centrifuge")
                        && !String.valueOf(machine.pausedReason())
                                .contains("quarantine"),
                "Opening centrifuge quarantined after reload: "
                        + machine.pausedReason());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void acquisitionItemPlacesExpectedVariant(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ModItems.STEEL_ROASTER.get());
        helper.assertTrue(
                stack.getItem() instanceof BlockItem,
                "Steel roaster acquisition item is not a BlockItem");
        Block block = ((BlockItem) stack.getItem()).getBlock();
        ConfiguredProcessingMachineBlockEntity machine = place(
                helper,
                new BlockPos(2, 2, 2),
                block,
                ModProcessingMachines.ROASTER);
        helper.assertTrue(
                machine.variant().id().toString().equals(
                        "cruciblecraft:steel_roaster"),
                "Acquisition item placed the wrong variant");
        helper.succeed();
    }

    private static Set<String> catalogIds(boolean skipGenericOnly) {
        try (var stream = MachineRuntimeGameTests.class.getResourceAsStream(
                "/data/cruciblecraft/machine_tiers.json")) {
            JsonObject document = JsonParser.parseReader(
                    new java.io.InputStreamReader(
                            stream, java.nio.charset.StandardCharsets.UTF_8))
                    .getAsJsonObject();
            return document.getAsJsonArray("variants").asList().stream()
                    .map(row -> row.getAsJsonObject())
                    .filter(row -> {
                        if (!skipGenericOnly) {
                            return true;
                        }
                        var profile = row.getAsJsonObject("resourceProfile");
                        return profile == null
                                || !profile.has("skipGenericRegistration")
                                || !profile.get("skipGenericRegistration")
                                        .getAsBoolean();
                    })
                    .map(row -> row.get("id").getAsString())
                    .collect(Collectors.toCollection(TreeSet::new));
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }

    private static ConfiguredProcessingMachineBlockEntity place(
            GameTestHelper helper,
            BlockPos pos,
            Block block,
            com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec spec) {
        helper.setBlock(
                pos,
                block.defaultBlockState().setValue(
                        ProcessingMachineBlock.FACING, FRONT));
        ConfiguredProcessingMachineBlockEntity machine = helper.getBlockEntity(pos);
        helper.assertTrue(
                machine.spec() == spec
                        || machine.variant().kind().behavior() == spec,
                "Placed block resolved wrong machine kind");
        return machine;
    }

    private static Direction energySide(
            ConfiguredProcessingMachineBlockEntity machine) {
        return ProcessingMachineIoFaces.energy(machine.spec(), machine.facing());
    }

    private static void loadRecipeInputs(
            ConfiguredProcessingMachineBlockEntity machine, GTRecipe recipe) {
        for (int i = 0; i < recipe.itemInputs().size(); i++) {
            ItemStack sample = recipe.itemInputs().get(i).getItems()[0].copy();
            sample.setCount(Math.max(1, recipe.itemInputCounts().get(i)));
            machine.inventory().setStackInSlot(
                    machine.spec().items().inputs().get(i), sample);
        }
        for (int i = 0; i < recipe.fluidInputs().size(); i++) {
            machine.tanks().get(machine.spec().fluids().inputs().get(i).index())
                    .setFluid(recipe.fluidInputs().get(i).copy());
        }
    }

    private static void forceLastTick(
            GameTestHelper helper,
            ConfiguredProcessingMachineBlockEntity machine) {
        helper.assertTrue(
                machine.duration() > 0,
                machine.spec().id() + " has no selected recipe: "
                        + machine.pausedReason());
        machine.runtime().processor().setProgress(machine.duration() - 1);
    }

    private static GTRecipe requireRecipe(RecipeMap map, String path) {
        return map.entries().stream()
                .filter(entry -> entry.id().getPath().equals(path))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing live recipe " + map.id() + "/" + path))
                .recipe();
    }

    private static GTRecipe firstPowered(RecipeMap map, long maxEut) {
        return map.entries().stream()
                .map(RecipeMap.Entry::recipe)
                .filter(recipe -> recipe.eut() > 0L && recipe.eut() <= maxEut)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No powered recipe on " + map.id()));
    }

    private static boolean hasAnyOutput(
            ConfiguredProcessingMachineBlockEntity machine) {
        return machine.spec().items().outputs().stream().anyMatch(slot ->
                !machine.inventory().getStackInSlot(slot).isEmpty())
                || machine.spec().fluids().outputs().stream().anyMatch(tank ->
                !machine.tanks().get(tank.index()).getFluid().isEmpty());
    }
}
