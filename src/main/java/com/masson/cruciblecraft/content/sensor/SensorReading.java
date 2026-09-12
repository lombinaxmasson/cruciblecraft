package com.masson.cruciblecraft.content.sensor;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.CableBlock;
import com.masson.cruciblecraft.content.blockentity.BoilerBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CableBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CeramicMoldBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CokeOvenBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CrucibleBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ProcessingMachineBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ReactorCoreBlockEntity;
import com.masson.cruciblecraft.content.blockentity.RotationalAxleBlockEntity;
import com.masson.cruciblecraft.content.blockentity.RotationalGearboxBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;

/** Adjacent / world reads for one GT6 sensor identity. Missing hosts stay 0. */
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
        return switch (kind) {
            case THERMOMETER -> thermometer(level, sensorPos.relative(probe));
            case GIBBLOMETER -> gibbl(level, sensorPos.relative(probe), 1000L);
            case KILO_GIBBLOMETER -> gibbl(
                    level, sensorPos.relative(probe), 1_000_000L);
            case LUMINOMETER -> light(level, sensorPos.relative(probe));
            case CHRONOMETER -> time(level);
            case ITEMOMETER -> items(level, sensorPos.relative(probe), probe, false);
            case STACKOMETER -> items(level, sensorPos.relative(probe), probe, true);
            case FLUIDOMETER -> fluids(
                    level, sensorPos.relative(probe), probe, 1L, true);
            case BUCKETOMETER -> fluids(
                    level, sensorPos.relative(probe), probe, 1000L, true);
            case KILO_BUCKETOMETER -> fluids(
                    level, sensorPos.relative(probe), probe, 1_000_000L, false);
            case LIGHT_WEIGHTOMETER -> weight(
                    level, sensorPos.relative(probe), probe, 1000.0, 65.535);
            case MEDIUM_WEIGHTOMETER -> weight(
                    level, sensorPos.relative(probe), probe, 1.0, SENSOR_MAX);
            case HEAVY_WEIGHTOMETER -> weight(
                    level,
                    sensorPos.relative(probe),
                    probe,
                    0.001,
                    SENSOR_MAX * 1000.0);
            case SUPER_HEAVY_WEIGHTOMETER -> weight(
                    level,
                    sensorPos.relative(probe),
                    probe,
                    0.000001,
                    SENSOR_MAX * 1_000_000.0);
            case ELECTROMETER -> electrometer(
                    level, sensorPos.relative(probe), false);
            case LASEROMETER -> electrometer(
                    level, sensorPos.relative(probe), true);
            case TPS_METER -> new Sample(tpsValue, 2000L);
            case PLAYER_COUNTER -> players(level);
            case PROGRESS_METER -> progress(level, sensorPos.relative(probe));
            case TACHOMETER -> tachometer(level, sensorPos.relative(probe));
            case GEIGER_COUNTER -> geiger(level, sensorPos.relative(probe));
        };
    }

    public static int redstone(SensorMode mode, long value, long max, int setNumber) {
        int boundValue = bind16(value);
        int boundSet = bind16(setNumber);
        return switch (mode) {
            case DISPLAY -> 0;
            case GREATER -> boundValue > boundSet ? 15 : 0;
            case EQUAL -> boundValue == boundSet ? 15 : 0;
            case SMALLER -> boundValue < boundSet ? 15 : 0;
            case SCALE -> scale(boundValue, Math.max(1, boundSet), 15);
            case PERCENT -> {
                long percent = max > 0L
                        ? Math.min(100L, (value * 100L) / max)
                        : 0L;
                yield scale(percent, 100L, 15);
            }
            case FULL -> value >= max && max > 0L ? 15 : 0;
            case NOT_FULL -> value >= max && max > 0L ? 0 : 15;
        };
    }

    private static Sample thermometer(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CrucibleBlockEntity crucible) {
            return kelvin(crucible.temperatureCelsius());
        }
        if (be instanceof CeramicMoldBlockEntity mold) {
            return kelvin(mold.temperature());
        }
        return Sample.ZERO;
    }

    private static Sample kelvin(float celsius) {
        long kelvin = Math.max(0L, Math.round(celsius + 273.0F));
        return new Sample(kelvin, Math.max(kelvin, 2273L));
    }

    private static Sample light(Level level, BlockPos pos) {
        int light = level.getMaxLocalRawBrightness(pos);
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
            int limit = Math.max(1, items.getSlotLimit(slot));
            if (stacks) {
                if (!items.getStackInSlot(slot).isEmpty()) {
                    value++;
                }
                max++;
            } else {
                value += items.getStackInSlot(slot).getCount();
                max += limit;
            }
        }
        return new Sample(value, max);
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
                    BoilerBlockEntity.STEAM_CAPACITY / divisor);
        }
        if (be instanceof CrucibleBlockEntity crucible) {
            long unitNine = 9L * MaterialPrefixes.INGOT.units();
            return new Sample(
                    divUp(crucible.totalUnits() * 1000L, unitNine) / divisor,
                    divUp(CrucibleBlockEntity.maxUnits() * 1000L, unitNine)
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

    private static Sample progress(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ProcessingMachineBlockEntity machine) {
            return new Sample(machine.progress(), machine.duration());
        }
        if (be instanceof CokeOvenBlockEntity oven) {
            return new Sample(oven.progress(), oven.recipeDuration());
        }
        return Sample.ZERO;
    }

    private static Sample geiger(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ReactorCoreBlockEntity core) {
            long neutrons = core.neutronSum();
            return new Sample(neutrons, Math.max(neutrons, 1L));
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

    private static long divUp(long value, long divisor) {
        if (divisor <= 0L || value <= 0L) {
            return 0L;
        }
        return (value + divisor - 1L) / divisor;
    }

    private static int scale(long value, long maximum, int range) {
        if (value <= 0L || maximum <= 0L) {
            return 0;
        }
        long scaled = (value * range) / maximum;
        return (int) Math.min(range, Math.max(0L, scaled));
    }

    private static int bind16(long value) {
        if (value < 0L) {
            return 0;
        }
        return (int) Math.min(SENSOR_MAX, value);
    }
}
