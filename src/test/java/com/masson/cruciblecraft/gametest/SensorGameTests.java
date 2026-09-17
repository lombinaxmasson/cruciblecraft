package com.masson.cruciblecraft.gametest;

import java.util.ArrayList;
import java.util.List;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.FluidPipeBlock;
import com.masson.cruciblecraft.content.block.RotationalAxleBlock;
import com.masson.cruciblecraft.content.block.RotationalGearboxBlock;
import com.masson.cruciblecraft.content.block.SensorBlock;
import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.RotationalAxleBlockEntity;
import com.masson.cruciblecraft.content.blockentity.SensorBlockEntity;
import com.masson.cruciblecraft.content.item.ElectroMeterItem;
import com.masson.cruciblecraft.content.item.GeigerCounterItem;
import com.masson.cruciblecraft.content.item.TachoMeterItem;
import com.masson.cruciblecraft.content.item.ThermometerItem;
import com.masson.cruciblecraft.content.sensor.SensorKind;
import com.masson.cruciblecraft.content.sensor.SensorMode;
import com.masson.cruciblecraft.content.sensor.SensorReading;
import com.masson.cruciblecraft.logistics.pipe.PipeCatalog;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Isolated Sensors gate. Run with {@code -PwaveRecipes=content/sensors}.
 */
@GameTestHolder(SensorGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class SensorGameTests {
    public static final String NAMESPACE = "cruciblecraft_wave_content_sensors";
    private static final String TEMPLATE = "empty";
    private static final BlockPos POS = new BlockPos(2, 2, 2);
    private static final BlockPos PROBE = new BlockPos(2, 2, 3);

    private SensorGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void playerSurfaceIsRegistered(GameTestHelper helper) {
        helper.assertTrue(
                ModBlocks.sensorBlocksById().size() == SensorKind.EXPECTED_SIZE,
                "Sensor block catalog drifted from 21");
        helper.assertTrue(
                ModItems.sensorItemsById().size() == SensorKind.EXPECTED_SIZE,
                "Sensor item catalog drifted from 21");
        helper.assertTrue(
                ModBlockEntities.SENSOR.get() != null,
                "Sensor block entity type missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void twentyOneIdentitiesAreRegistered(GameTestHelper helper) {
        int x = 0;
        for (SensorKind kind : SensorKind.all()) {
            BlockPos pos = new BlockPos(1 + (x % 5), 1, 1 + (x / 5));
            helper.setBlock(
                    pos,
                    ModBlocks.sensorBlocksById()
                            .get(kind.id())
                            .get()
                            .defaultBlockState()
                            .setValue(SensorBlock.FACING, Direction.NORTH));
            helper.assertTrue(
                    helper.getBlockEntity(pos) instanceof SensorBlockEntity sensor
                            && sensor.kind() == kind,
                    "Sensor BE missing or kind drifted: " + kind.path());
            x++;
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void survivalCraftableHostsMatchD0(GameTestHelper helper) {
        for (SensorKind kind : SensorKind.all()) {
            Item result = ModItems.sensorItemsById().get(kind.id()).get();
            boolean hasRecipe = helper.getLevel()
                    .getRecipeManager()
                    .getAllRecipesFor(RecipeType.CRAFTING)
                    .stream()
                    .anyMatch(holder -> holder.value()
                            .getResultItem(helper.getLevel().registryAccess())
                            .is(result));
            if (kind.d0Blocked()) {
                helper.assertFalse(
                        hasRecipe,
                        "Blocked D0 still has a recipe: " + kind.path());
                continue;
            }
            helper.assertTrue(hasRecipe, "Sensor D0 missing for " + kind.path());
            ItemStack assembled = craft(helper, kind);
            helper.assertTrue(
                    assembled.is(result) && assembled.getCount() == 1,
                    "Sensor D0 did not assemble " + kind.path());
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void facingAndWeakRedstone(GameTestHelper helper) {
        helper.setBlock(PROBE, Blocks.GLOWSTONE);
        helper.setBlock(
                POS,
                ModBlocks.sensorBlocksById()
                        .get(SensorKind.LUMINOMETER.id())
                        .get()
                        .defaultBlockState()
                        .setValue(SensorBlock.FACING, Direction.NORTH));
        SensorBlockEntity sensor = (SensorBlockEntity) helper.getBlockEntity(POS);
        helper.assertTrue(sensor != null, "Luminometer BE missing");
        sensor.setMode(SensorMode.GREATER);
        sensor.setSetNumber(0);
        helper.startSequence()
                .thenExecuteAfter(4, () -> {
                    BlockPos world = helper.absolutePos(POS);
                    int weak = helper.getLevel()
                            .getBlockState(world)
                            .getSignal(helper.getLevel(), world, Direction.WEST);
                    int strong = helper.getLevel()
                            .getBlockState(world)
                            .getDirectSignal(
                                    helper.getLevel(), world, Direction.WEST);
                    int front = helper.getLevel()
                            .getBlockState(world)
                            .getSignal(helper.getLevel(), world, Direction.NORTH);
                    helper.assertTrue(
                            helper.getBlockState(POS).getValue(SensorBlock.FACING)
                                    == Direction.NORTH,
                            "Luminometer facing drifted");
                    helper.assertTrue(
                            sensor.probe() == Direction.SOUTH,
                            "Luminometer probe drifted from SOUTH");
                    helper.assertTrue(
                            weak == 15,
                            "Luminometer weak redstone was " + weak);
                    helper.assertTrue(
                            strong == 0,
                            "Luminometer leaked strong redstone: " + strong);
                    helper.assertTrue(
                            front == 0,
                            "Luminometer emitted on the display face");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void liveHostsReportNonZero(GameTestHelper helper) {
        BlockPos tachometer = new BlockPos(1, 2, 2);
        BlockPos axle = new BlockPos(1, 2, 3);
        BlockPos gearbox = new BlockPos(2, 2, 3);
        BlockPos gibblometer = new BlockPos(3, 2, 2);
        BlockPos pipe = new BlockPos(3, 2, 3);
        BlockPos weightometer = new BlockPos(4, 2, 2);
        BlockPos chest = new BlockPos(4, 2, 3);
        helper.setBlock(
                axle,
                ModBlocks.ROTATIONAL_AXLE.get().defaultBlockState()
                        .setValue(RotationalAxleBlock.AXIS, Direction.Axis.X));
        helper.setBlock(
                gearbox,
                ModBlocks.ROTATIONAL_GEARBOX.get().defaultBlockState()
                        .setValue(RotationalGearboxBlock.FACING, Direction.EAST));
        FluidPipeBlock pipeBlock = (FluidPipeBlock) ModBlocks.pipeBlock(
                "copper",
                MaterialPrefixes.LARGE_FLUID_PIPE,
                PipeCatalog.Kind.FLUID).get();
        helper.setBlock(pipe, pipeBlock);
        FluidPipeBlockEntity pipeBe = helper.getBlockEntity(pipe);
        helper.assertTrue(pipeBe != null, "Fluid pipe BE missing");
        helper.assertTrue(
                pipeBe.capacity() >= 1000,
                "Gibbl host pipe capacity was " + pipeBe.capacity());
        int filled = pipeBe.fillInternal(
                new FluidStack(Fluids.WATER, pipeBe.capacity()),
                IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(filled > 0, "Could not fill gibbl host pipe");
        helper.setBlock(chest, Blocks.CHEST);
        IItemHandler items = helper.getLevel().getCapability(
                Capabilities.ItemHandler.BLOCK,
                helper.absolutePos(chest),
                Direction.NORTH);
        helper.assertTrue(items != null, "Chest item handler missing");
        ItemStack leftover = items.insertItem(
                0,
                MaterialLookup.stack("copper", MaterialPrefixes.INGOT),
                false);
        helper.assertTrue(leftover.isEmpty(), "Chest rejected copper ingot");
        placeLive(helper, SensorKind.TACHOMETER, tachometer);
        placeLive(helper, SensorKind.GIBBLOMETER, gibblometer);
        placeLive(helper, SensorKind.LIGHT_WEIGHTOMETER, weightometer);
        RotationalAxleBlockEntity axleBe = helper.getBlockEntity(axle);
        helper.assertTrue(axleBe != null, "Axle BE missing");
        long accepted = axleBe.insert(
                EnergyType.KINETIC_ROTATION,
                32L,
                1L,
                Direction.WEST,
                false);
        helper.assertTrue(accepted > 0L, "Axle rejected RU insert");
        helper.startSequence()
                .thenExecuteAfter(
                        2,
                        () -> {
                            SensorReading.Sample tachometerSample =
                                    SensorReading.read(
                                            SensorKind.TACHOMETER,
                                            helper.getLevel(),
                                            helper.absolutePos(tachometer),
                                            Direction.SOUTH,
                                            0L);
                            helper.assertTrue(
                                    tachometerSample.value() > 0L,
                                    "tachometer stayed zero: value="
                                            + tachometerSample.value()
                                            + " axleLast="
                                            + axleBe.transferredLast());
                            assertLive(
                                    helper,
                                    SensorKind.GIBBLOMETER,
                                    gibblometer);
                            assertLive(
                                    helper,
                                    SensorKind.LIGHT_WEIGHTOMETER,
                                    weightometer);
                        })
                .thenSucceed();
    }

    private static void placeLive(
            GameTestHelper helper, SensorKind kind, BlockPos pos) {
        helper.setBlock(
                pos,
                ModBlocks.sensorBlocksById()
                        .get(kind.id())
                        .get()
                        .defaultBlockState()
                        .setValue(SensorBlock.FACING, Direction.NORTH));
        SensorBlockEntity sensor = (SensorBlockEntity) helper.getBlockEntity(pos);
        helper.assertTrue(sensor != null, kind.path() + " BE missing");
        sensor.setMode(SensorMode.GREATER);
        sensor.setSetNumber(0);
    }

    private static void assertLive(
            GameTestHelper helper, SensorKind kind, BlockPos pos) {
        SensorBlockEntity sensor = (SensorBlockEntity) helper.getBlockEntity(pos);
        helper.assertTrue(
                sensor != null && sensor.currentValue() > 0L,
                kind.path() + " stayed zero: value="
                        + (sensor == null ? "missing" : sensor.currentValue()));
    }

    private static ItemStack craft(GameTestHelper helper, SensorKind kind) {
        List<ItemStack> slots = new ArrayList<>();
        for (String row : kind.grid()) {
            for (int i = 0; i < 3; i++) {
                char letter = row.charAt(i);
                slots.add(letter == ' '
                        ? ItemStack.EMPTY
                        : ingredient(kind, letter));
            }
        }
        CraftingInput input = CraftingInput.of(3, 3, slots);
        return helper.getLevel()
                .getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .map(holder -> holder.value().assemble(
                        input, helper.getLevel().registryAccess()))
                .orElse(ItemStack.EMPTY);
    }

    private static ItemStack ingredient(SensorKind kind, char letter) {
        return switch (letter) {
            case 'P' -> MaterialLookup.stack(
                    "tin_alloy", MaterialPrefixes.DOUBLE_PLATE);
            case 'W' -> MaterialLookup.stack(
                    "red_alloy", MaterialPrefixes.FINE_WIRE);
            case 'R' -> new ItemStack(Items.REDSTONE);
            case 'G' -> new ItemStack(Items.GLASS);
            case 'B' -> MaterialLookup.stack(
                    "tin_alloy", MaterialPrefixes.BOLT);
            case 'C' -> new ItemStack(Items.COMPARATOR);
            case 'X' -> special(kind.specialX());
            case 'Y' -> special(kind.specialY());
            default -> throw new IllegalStateException(
                    "Unknown sensor grid letter " + letter);
        };
    }

    private static ItemStack special(String key) {
        return switch (key) {
            case "thermometer" -> new ItemStack(ModItems.semanticIdentityItemsById()
                    .get(id(ThermometerItem.REGISTRY_PATH))
                    .get());
            case "electro_meter" -> new ItemStack(ModItems.semanticIdentityItemsById()
                    .get(id(ElectroMeterItem.REGISTRY_PATH))
                    .get());
            case "tacho_meter" -> new ItemStack(ModItems.semanticIdentityItemsById()
                    .get(id(TachoMeterItem.REGISTRY_PATH))
                    .get());
            case "sio2_gem" -> MaterialLookup.stack(
                    "glass", MaterialPrefixes.GEM);
            case "silicon_plate" -> MaterialLookup.stack(
                    "silicon", MaterialPrefixes.PLATE);
            case "copper_fine_wire" -> MaterialLookup.stack(
                    "copper", MaterialPrefixes.FINE_WIRE);
            case "copper_wire" -> MaterialLookup.stack(
                    "copper", MaterialPrefixes.WIRE);
            case "clock" -> new ItemStack(Items.CLOCK);
            case "gold_pressure_plate" -> new ItemStack(
                    Items.LIGHT_WEIGHTED_PRESSURE_PLATE);
            case "iron_pressure_plate" -> new ItemStack(
                    Items.HEAVY_WEIGHTED_PRESSURE_PLATE);
            case "stone_pressure_plate" -> new ItemStack(Items.STONE_PRESSURE_PLATE);
            case "wood_pressure_plate" -> new ItemStack(Items.OAK_PRESSURE_PLATE);
            case "chest" -> new ItemStack(Items.CHEST);
            case "bucket" -> new ItemStack(Items.BUCKET);
            case "brass_small_gear" -> MaterialLookup.stack(
                    "brass", MaterialPrefixes.SMALL_GEAR);
            case "brass_gear" -> MaterialLookup.stack(
                    "brass", MaterialPrefixes.GEAR);
            case "geiger_counter" -> new ItemStack(ModItems.semanticIdentityItemsById()
                    .get(id(GeigerCounterItem.FILLED_PATH))
                    .get());
            case "lead_double_plate" -> MaterialLookup.stack(
                    "lead", MaterialPrefixes.DOUBLE_PLATE);
            case "compact_sensor_lv" -> new ItemStack(ModItems.technologicalPart(
                    "compact_sensor_lv").get());
            case "diamond_gem" -> MaterialLookup.stack(
                    "diamantine", MaterialPrefixes.GEM);
            default -> throw new IllegalStateException(
                    "Unknown sensor special " + key);
        };
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
