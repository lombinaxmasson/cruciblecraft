package com.masson.cruciblecraft.heat;

import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.world.item.ItemStack;

public final class ItemHeat {
    public static final float AMBIENT_TEMPERATURE = 20.0f;
    public static final float COOLING_RATE_PER_TICK = 1.0f;
    private static final float AMBIENT_EPSILON = 0.01f;

    private ItemHeat() {}

    public static void set(ItemStack stack, float temperature, long gameTime) {
        if (temperature <= AMBIENT_TEMPERATURE + AMBIENT_EPSILON) {
            stack.remove(ModComponents.HEAT.get());
            return;
        }
        stack.set(ModComponents.HEAT.get(), new HeatComponent(temperature, gameTime));
    }

    public static float temperature(ItemStack stack, long gameTime) {
        HeatComponent heat = stack.get(ModComponents.HEAT.get());
        if (heat == null) {
            return AMBIENT_TEMPERATURE;
        }
        long elapsed = Math.max(0L, gameTime - heat.lastUpdateTick());
        return ThermalSim.stepTicks(
                heat.temperature(),
                AMBIENT_TEMPERATURE,
                COOLING_RATE_PER_TICK,
                elapsed);
    }

    public static boolean isCooled(ItemStack stack, long gameTime) {
        return stack.has(ModComponents.HEAT.get())
                && temperature(stack, gameTime)
                        <= AMBIENT_TEMPERATURE + AMBIENT_EPSILON;
    }

    /**
     * Removes a completed heat snapshot without rewriting it while it cools.
     * This is the only maintenance write required for normal inventory stacks.
     */
    public static boolean clearIfCooled(ItemStack stack, long gameTime) {
        if (!isCooled(stack, gameTime)) {
            return false;
        }
        stack.remove(ModComponents.HEAT.get());
        return true;
    }
}
