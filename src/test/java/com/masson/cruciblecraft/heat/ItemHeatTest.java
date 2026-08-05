package com.masson.cruciblecraft.heat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.registries.DeferredHolder;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ItemHeatTest {
    @BeforeAll
    static void bootstrap() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        bindHeatComponent();
    }

    @Test
    void setUsesTheAmbientEpsilonBoundary() {
        ItemStack stack = new ItemStack(Items.IRON_INGOT);

        ItemHeat.set(stack, ItemHeat.AMBIENT_TEMPERATURE + 0.009f, 10L);
        assertFalse(stack.has(ModComponents.HEAT.get()));

        ItemHeat.set(stack, ItemHeat.AMBIENT_TEMPERATURE + 0.02f, 10L);
        assertTrue(stack.has(ModComponents.HEAT.get()));
    }

    @Test
    void temperatureCoolsLazilyAndNeverRunsBackward() {
        ItemStack stack = new ItemStack(Items.IRON_INGOT);
        ItemHeat.set(stack, 50.0f, 100L);

        assertEquals(50.0f, ItemHeat.temperature(stack, 90L));
        assertEquals(40.0f, ItemHeat.temperature(stack, 110L));
        assertEquals(ItemHeat.AMBIENT_TEMPERATURE, ItemHeat.temperature(stack, 200L));
    }

    @Test
    void clearIfCooledOnlyReportsAnActualComponentRemoval() {
        ItemStack stack = new ItemStack(Items.IRON_INGOT);
        assertFalse(ItemHeat.clearIfCooled(stack, 0L));

        ItemHeat.set(stack, 21.0f, 0L);
        assertFalse(ItemHeat.clearIfCooled(stack, 0L));
        assertTrue(ItemHeat.clearIfCooled(stack, 1L));
        assertFalse(stack.has(ModComponents.HEAT.get()));
        assertFalse(ItemHeat.clearIfCooled(stack, 2L));
    }

    private static void bindHeatComponent() {
        DataComponentType<HeatComponent> component =
                DataComponentType.<HeatComponent>builder()
                        .persistent(HeatComponent.CODEC)
                        .networkSynchronized(HeatComponent.STREAM_CODEC)
                        .build();
        try {
            var holder = DeferredHolder.class.getDeclaredField("holder");
            holder.setAccessible(true);
            holder.set(ModComponents.HEAT, Holder.direct(component));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Unable to install test heat component", exception);
        }
    }
}
