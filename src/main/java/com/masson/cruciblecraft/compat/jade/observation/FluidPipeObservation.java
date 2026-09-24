package com.masson.cruciblecraft.compat.jade.observation;

import java.util.ArrayList;
import java.util.List;

import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;

/** Server snapshot of the source {@code MultiTileEntityPipeFluid} tank rows. */
public record FluidPipeObservation(List<Tank> tanks) {
    public static final String TANK_COUNT = "cc_waila_fluid_pipe_tank_count";
    public static final String TANK_ID = "cc_waila_fluid_pipe_tank_id_";
    public static final String TANK_AMOUNT = "cc_waila_fluid_pipe_tank_amount_";
    public static final String TANK_CAPACITY = "cc_waila_fluid_pipe_tank_capacity_";

    public record Tank(String fluidId, int amount, int capacity) {
        public Tank {
            fluidId = fluidId == null ? "" : fluidId;
        }
    }

    public FluidPipeObservation {
        tanks = List.copyOf(tanks == null ? List.of() : tanks);
    }

    public static FluidPipeObservation fromServerData(CompoundTag data) {
        int count = Math.max(0, data.getInt(TANK_COUNT));
        List<Tank> tanks = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            tanks.add(new Tank(
                    data.getString(TANK_ID + index),
                    data.getInt(TANK_AMOUNT + index),
                    data.getInt(TANK_CAPACITY + index)));
        }
        return new FluidPipeObservation(tanks);
    }

    public static void writeServerData(
            CompoundTag data, FluidPipeBlockEntity pipe) {
        data.putInt(TANK_COUNT, pipe.tankCount());
        for (int index = 0; index < pipe.tankCount(); index++) {
            FluidStack fluid = pipe.fluidInTank(index);
            ResourceLocation id = fluid.isEmpty()
                    ? null
                    : BuiltInRegistries.FLUID.getKey(fluid.getFluid());
            data.putString(
                    TANK_ID + index,
                    id == null ? "" : id.toString());
            data.putInt(TANK_AMOUNT + index, fluid.getAmount());
            data.putInt(TANK_CAPACITY + index, pipe.capacity());
        }
    }

    public FluidStack fluid(int index) {
        if (index < 0 || index >= tanks.size()) {
            return FluidStack.EMPTY;
        }
        Tank tank = tanks.get(index);
        ResourceLocation id = ResourceLocation.tryParse(tank.fluidId());
        if (id == null || !BuiltInRegistries.FLUID.containsKey(id)) {
            return FluidStack.EMPTY;
        }
        return new FluidStack(
                BuiltInRegistries.FLUID.get(id),
                Math.max(0, tank.amount()));
    }
}
