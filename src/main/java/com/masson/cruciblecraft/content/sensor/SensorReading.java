package com.masson.cruciblecraft.content.sensor;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.api.tileentity.ProgressHost;
import com.masson.cruciblecraft.content.block.CableBlock;
import com.masson.cruciblecraft.content.blockentity.BoilerBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CableBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CrucibleBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.LargeCrucibleBlockEntity;
import com.masson.cruciblecraft.content.blockentity.LargeBoilerBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ProcessingMachineBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ReactorCoreBlockEntity;
import com.masson.cruciblecraft.content.blockentity.RotationalAxleBlockEntity;
import com.masson.cruciblecraft.content.blockentity.RotationalGearboxBlockEntity;
import com.masson.cruciblecraft.content.blockentity.SteamEngineBlockEntity;
import com.masson.cruciblecraft.content.multiblock.MultiblockControllerBinding;
import com.masson.cruciblecraft.content.multiblock.MultiblockPort;
import com.masson.cruciblecraft.content.item.ReactorRodItem;
import com.masson.cruciblecraft.nuclear.ReactorRodPhysics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;

/** Adjacent / world reads for one GT6 sensor identity. */
public final class SensorReading {
    public record Sample(long value, long max) {
        public static final Sample ZERO = new Sample(0L, 0L);
    }

    private static final long SENSOR_MAX = 65535L;

    private SensorReading() {}

    public static Sample read(
            SensorKind kind,
            Level level,
            BlockPos sensorPos,
            Direction probe,
            long tpsValue) {
        BlockPos target = targetPos(level, sensorPos, probe);
        return switch (kind) {
            case THERMOMETER -> thermometer(level, target, probe);
            case GIBBLOMETER -> gibbl(level, target, 1000L);
            case KILO_GIBBLOMETER -> gibbl(
                    level, target, 1_000_000L);
            case LUMINOMETER -> light(level, target);
            case CHRONOMETER -> time(level);
            case ITEMOMETER -> items(level, target, probe, false);
            case STACKOMETER -> items(level, target, probe, true);
            case FLUIDOMETER -> fluids(
                    level, target, probe, 1L, true);
            case BUCKETOMETER -> fluids(
                    level, target, probe, 1000L, true);
            case KILO_BUCKETOMETER -> fluids(
                    level, target, probe, 1_000_000L, false);
            case LIGHT_WEIGHTOMETER -> weight(
                    level, target, probe, 1000.0, 65.535);
            case MEDIUM_WEIGHTOMETER -> weight(
                    level, target, probe, 1.0, SENSOR_MAX);
            case HEAVY_WEIGHTOMETER -> weight(
                    level,
                    target,
                    probe,
                    0.001,
                    SENSOR_MAX * 1000.0);
            case SUPER_HEAVY_WEIGHTOMETER -> weight(
                    level,
                    target,
                    probe,
                    0.000001,
                    SENSOR_MAX * 1_000_000.0);
            case ELECTROMETER -> electrometer(
                    level, target, false);
            case LASEROMETER -> electrometer(
                    level, target, true);
            case TPS_METER -> new Sample(tpsValue, 2000L);
            case PLAYER_COUNTER -> players(level);
            case PROGRESS_METER -> progress(level, target, probe);
            case TACHOMETER -> tachometer(level, target);
            case GEIGER_COUNTER -> geiger(level, target);
        };
    }

    public static int redstone(SensorMode mode, long value, long max, int setNumber) {
        return redstone(mode, value, max, setNumber, 0);
    }

    public static int redstone(
            SensorMode mode,
            long value,
            long max,
            int setNumber,
            int previousRedstone) {
        long boundValue = bindInt(value);
        long boundMax = bindInt(max);
        int boundSet = bind16(setNumber);
        return switch (mode) {
            case DISPLAY -> bind4(previousRedstone);
            case GREATER -> boundValue > boundSet ? 15 : 0;
            case EQUAL -> boundValue == boundSet ? 15 : 0;
            case SMALLER -> boundValue < boundSet ? 15 : 0;
            case SCALE -> scale(boundValue, boundSet, 15);
            case PERCENT -> {
                long percent = boundMax > 0L
                        ? Math.min(100L, (boundValue * 100L) / boundMax)
                        : 0L;
                yield scale(percent, 100L, 15);
            }
            case FULL -> boundValue >= boundMax ? 15 : 0;
            case NOT_FULL -> boundValue >= boundMax ? 0 : 15;
        };
    }

    private static Sample thermometer(
            Level level, BlockPos pos, Direction probe) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TemperatureHost host) {
            return kelvin(
                    host.temperatureCelsius(probe),
                    host.temperatureMaxCelsius(probe));
        }
        if (be instanceof ReactorCoreBlockEntity reactor) {
            return new Sample(Math.max(0L, reactor.heat() / 5L), 0L);
        }
        return worldTemperature(level, pos);
    }

    private static Sample kelvin(float celsius, float maxCelsius) {
        long kelvin = Math.max(0L, Math.round(celsius + 273.0F));
        long maximum = Float.isFinite(maxCelsius)
                ? Math.max(0L, Math.round(maxCelsius + 273.0F))
                : 0L;
        return new Sample(kelvin, maximum);
    }

    private static Sample worldTemperature(Level level, BlockPos pos) {
        float biomeTemperature =
                level.getBiome(pos).value().getBaseTemperature();
        long kelvin = Math.max(1L, 270L + (long) (biomeTemperature * 20.0F));
        for (Direction direction : Direction.values()) {
            BlockPos nearby = direction == Direction.UP
                    ? pos.above()
                    : direction == Direction.DOWN
                            ? pos.below()
                            : pos.relative(direction);
            var state = level.getBlockState(nearby);
            if (state.is(Blocks.FIRE)) {
                kelvin = Math.max(kelvin, 473L);
            }
            if (state.getFluidState().is(Fluids.LAVA)) {
                kelvin = Math.max(kelvin, 773L);
            }
        }
        var state = level.getBlockState(pos);
        if (state.is(Blocks.FIRE)) {
            kelvin = Math.max(kelvin, 473L);
        }
        if (state.getFluidState().is(Fluids.LAVA)) {
            kelvin = Math.max(kelvin, 773L);
        }
        return new Sample(kelvin, 0L);
    }

    private static Sample light(Level level, BlockPos pos) {
        int light = level.getBrightness(LightLayer.BLOCK, pos);
        return new Sample(light, 15L);
    }

    private static Sample time(Level level) {
        long minutes = ((level.getDayTime() + 6000L) % 24000L) * 60L / 1000L;
        return new Sample(minutes, 1440L);
    }

    private static Sample items(
            Level level, BlockPos pos, Direction probe, boolean stacks) {
        IItemHandler items = itemHandler(level, pos, probe);
        if (items == null) {
            return Sample.ZERO;
        }
        long value = 0L;
        long max = 0L;
        for (int slot = 0; slot < items.getSlots(); slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            int limit = Math.max(1, items.getSlotLimit(slot));
            if (!isFluidDisplay(stack)) {
                if (stacks) {
                    if (!stack.isEmpty()) {
                        value++;
                    }
                } else {
                    value += stack.getCount();
                }
            }
            if (stacks) {
                max++;
            } else {
                max += limit;
            }
        }
        return new Sample(value, max);
    }

    /**
     * GT6's {@code IL.Display_Fluid} is not currently registered by CC. Keep
     * the exclusion capability-safe so it also works when a compatible
     * Fluid Display item is present without linking to its class.
     */
    private static boolean isFluidDisplay(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        String simpleName = stack.getItem().getClass().getSimpleName();
        if ("ItemFluidDisplay".equals(simpleName)) {
            return true;
        }
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(stack.getItem());
        return id != null
                && ("gregtech:gt.display.fluid".equals(id.toString())
                        || "gt.display.fluid".equals(id.getPath())
                        || "fluid_display".equals(id.getPath())
                        || "display_fluid".equals(id.getPath()));
    }

    private static Sample fluids(
            Level level,
            BlockPos pos,
            Direction probe,
            long divisor,
            boolean vanillaSource) {
        IFluidHandler fluids = level.getCapability(
                Capabilities.FluidHandler.BLOCK, pos, probe.getOpposite());
        if (fluids == null) {
            fluids = level.getCapability(
                    Capabilities.FluidHandler.BLOCK, pos, null);
        }
        if (fluids != null) {
            long amount = 0L;
            long capacity = 0L;
            for (int tank = 0; tank < fluids.getTanks(); tank++) {
                amount += fluids.getFluidInTank(tank).getAmount();
                capacity += fluids.getTankCapacity(tank);
            }
            return new Sample(amount / divisor, capacity / divisor);
        }
        if (vanillaSource) {
            var fluid = level.getFluidState(pos);
            if (fluid.isSource()
                    && (fluid.is(Fluids.WATER) || fluid.is(Fluids.LAVA))) {
                return new Sample(1000L / divisor, 0L);
            }
        }
        return Sample.ZERO;
    }

    private static Sample gibbl(Level level, BlockPos pos, long divisor) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof FluidPipeBlockEntity pipe) {
            return new Sample(
                    pipe.storedFluid().getAmount() / divisor,
                    pipe.capacity() / divisor);
        }
        if (be instanceof ProcessingMachineBlockEntity machine) {
            long amount = 0L;
            long capacity = 0L;
            for (int index : machine.fluidInputTanks()) {
                FluidTank tank = machine.tanks().get(index);
                amount += tank.getFluidAmount();
                capacity += tank.getCapacity();
            }
            return new Sample(amount / divisor, capacity / divisor);
        }
        if (be instanceof BoilerBlockEntity boiler) {
            return new Sample(
                    boiler.steamAmount() / divisor,
                    boiler.steamCapacity() / divisor);
        }
        if (be instanceof LargeBoilerBlockEntity boiler) {
            return new Sample(
                    boiler.steamAmountLong() / divisor,
                    boiler.steamCapacityLong() / divisor);
        }
        if (be instanceof SteamEngineBlockEntity engine) {
            return new Sample(
                    engine.steamAmount() / divisor,
                    engine.steamCapacity() / divisor);
        }
        if (be instanceof CrucibleBlockEntity crucible) {
            long unitNine = 9L * MaterialPrefixes.INGOT.units();
            return new Sample(
                    divUp(crucible.totalUnits() * 1000L, unitNine) / divisor,
                    divUp(CrucibleBlockEntity.maxUnits() * 1000L, unitNine)
                            / divisor);
        }
        if (be instanceof LargeCrucibleBlockEntity crucible) {
            long unitNine = 9L * MaterialPrefixes.INGOT.units();
            return new Sample(
                    divUp(crucible.process().totalUnits() * 1000L, unitNine)
                            / divisor,
                    divUp(crucible.process().maxUnits() * 1000L, unitNine)
                            / divisor);
        }
        return Sample.ZERO;
    }

    private static Sample weight(
            Level level,
            BlockPos pos,
            Direction probe,
            double scale,
            double maxKilograms) {
        BlockEntity be = level.getBlockEntity(pos);
        double kilograms = 0.0;
        if (be instanceof CrucibleBlockEntity crucible) {
            kilograms = ItemMass.kilograms(crucible.composition());
        } else {
            IItemHandler items = itemHandler(level, pos, probe);
            if (items == null) {
                return Sample.ZERO;
            }
            for (int slot = 0; slot < items.getSlots(); slot++) {
                kilograms += ItemMass.kilograms(items.getStackInSlot(slot));
                if (kilograms >= maxKilograms) {
                    break;
                }
            }
        }
        if (kilograms >= maxKilograms) {
            kilograms = maxKilograms;
        }
        return new Sample((long) (kilograms * scale), SENSOR_MAX);
    }

    private static Sample electrometer(Level level, BlockPos pos, boolean laser) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof CableBlockEntity cable)
                || !(cable.getBlockState().getBlock() instanceof CableBlock block)) {
            return Sample.ZERO;
        }
        if (block.isLuFiber() != laser) {
            return Sample.ZERO;
        }
        long wattage = Math.max(0L, cable.wattageLast());
        long max = laser
                ? SENSOR_MAX
                : Math.max(
                        1L,
                        Math.multiplyExact(
                                block.transportProperties().maxVoltage(),
                                block.transportProperties().maxAmperage()));
        return new Sample(wattage, max);
    }

    private static Sample tachometer(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof RotationalAxleBlockEntity axle) {
            return new Sample(
                    axle.transferredLast(),
                    RotationalAxleBlockEntity.MAX_POWER
                            * RotationalAxleBlockEntity.MAX_PACKET);
        }
        if (be instanceof RotationalGearboxBlockEntity gearbox) {
            return new Sample(
                    gearbox.transferredLast(),
                    RotationalGearboxBlockEntity.MAX_PACKET * 16L);
        }
        return Sample.ZERO;
    }

    private static Sample players(Level level) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return Sample.ZERO;
        }
        return new Sample(server.getPlayerCount(), server.getMaxPlayers());
    }

    private static Sample progress(
            Level level, BlockPos pos, Direction probe) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ProgressHost host) {
            return new Sample(
                    Math.max(0L, host.progressValue(probe)),
                    Math.max(0L, host.progressMax(probe)));
        }
        if (be instanceof SpawnerBlockEntity spawner) {
            var tag = spawner.getSpawner().save(new net.minecraft.nbt.CompoundTag());
            return new Sample(Math.max(0L, tag.getShort("Delay")), Long.MAX_VALUE);
        }
        return Sample.ZERO;
    }

    private static Sample geiger(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ReactorCoreBlockEntity core) {
            long neutrons = core.neutronSum();
            long maximum = 0L;
            for (int slot = 0; slot < core.slots(); slot++) {
                var rod = core.rod(slot);
                if (rod.getItem() instanceof ReactorRodItem reactorRod) {
                    maximum += ReactorRodPhysics.neutronMaximum(
                            reactorRod.entry(), core.coolant());
                }
            }
            return new Sample(neutrons, maximum);
        }
        return Sample.ZERO;
    }

    private static IItemHandler itemHandler(
            Level level, BlockPos pos, Direction probe) {
        IItemHandler items = level.getCapability(
                Capabilities.ItemHandler.BLOCK, pos, probe.getOpposite());
        if (items == null) {
            items = level.getCapability(
                    Capabilities.ItemHandler.BLOCK, pos, null);
        }
        return items;
    }

    private static BlockPos targetPos(
            Level level,
            BlockPos sensorPos,
            Direction probe) {
        BlockPos adjacent = sensorPos.relative(probe);
        BlockEntity be = level.getBlockEntity(adjacent);
        if (be instanceof MultiblockPort port) {
            return port.controllerPosition()
                    .filter(controller -> level.getBlockEntity(controller)
                            instanceof MultiblockControllerBinding binding
                            && binding.structureValid())
                    .orElse(adjacent);
        }
        return adjacent;
    }

    private static long divUp(long value, long divisor) {
        if (divisor <= 0L || value <= 0L) {
            return 0L;
        }
        return (value + divisor - 1L) / divisor;
    }

    private static int scale(long value, long maximum, int range) {
        if (value <= 0L) {
            return 0;
        }
        if (maximum <= 0L || value >= maximum) {
            return range;
        }
        long scaled = 1L + (value * (range - 1L)) / maximum;
        return (int) Math.min(range, Math.max(0L, scaled));
    }

    private static long bindInt(long value) {
        if (value < 0L) {
            return 0;
        }
        return Math.min(Integer.MAX_VALUE, value);
    }

    private static int bind16(long value) {
        return (int) Math.min(SENSOR_MAX, Math.max(0L, value));
    }

    private static int bind4(long value) {
        return (int) Math.min(15L, Math.max(0L, value));
    }
}
