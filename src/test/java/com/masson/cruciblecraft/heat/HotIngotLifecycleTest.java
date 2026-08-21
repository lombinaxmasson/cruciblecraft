package com.masson.cruciblecraft.heat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.item.MaterialFormItem;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.material.def.ThermalProperties;
import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class HotIngotLifecycleTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        installHeatComponent();
    }

    @Test
    void smelterPostProcessingSetsIndependentHeatSnapshot() {
        ItemStack hot = HotIngotProcessing.prepare(
                new ItemStack(Items.IRON_INGOT),
                new TestForm(MaterialPrefixes.INGOT_HOT),
                40L);
        assertTrue(hot.has(ModComponents.HEAT.get()));
        assertEquals(1_000.0f, ItemHeat.temperature(hot, 40L));

        ItemStack ordinary = HotIngotProcessing.prepare(
                new ItemStack(Items.IRON_INGOT),
                new TestForm(MaterialPrefixes.INGOT),
                40L);
        assertFalse(ordinary.has(ModComponents.HEAT.get()));

        ItemStack compatibilityOutput = new ItemStack(Items.IRON_INGOT);
        TestForm hotForm = new TestForm(MaterialPrefixes.INGOT_HOT);
        assertTrue(HotIngotProcessing.initializeIfMissing(
                compatibilityOutput,
                hotForm,
                80L));
        HeatComponent initialized =
                compatibilityOutput.get(ModComponents.HEAT.get());
        assertEquals(1_000.0f, ItemHeat.temperature(compatibilityOutput, 80L));
        assertFalse(HotIngotProcessing.initializeIfMissing(
                compatibilityOutput,
                hotForm,
                90L));
        assertEquals(initialized, compatibilityOutput.get(ModComponents.HEAT.get()));
    }

    @Test
    void maintenanceKeepsItemIdentityAndOnlyClearsHeatComponent() {
        ItemStack hot = new ItemStack(Items.IRON_INGOT, 2);
        hot.set(DataComponents.CUSTOM_NAME, Component.literal("forged"));
        ItemHeat.set(hot, 21.0f, 0L);

        ItemStack maintained = HeatMaintenanceEvents.maintain(hot, 2L);

        assertTrue(maintained.is(Items.IRON_INGOT));
        assertEquals(2, maintained.getCount());
        assertEquals(Component.literal("forged"), maintained.getHoverName());
        assertFalse(maintained.has(ModComponents.HEAT.get()));
        ItemStack expected = new ItemStack(Items.IRON_INGOT, 2);
        expected.set(DataComponents.CUSTOM_NAME, Component.literal("forged"));
        assertTrue(ItemStack.isSameItemSameComponents(maintained, expected));
    }

    private static MaterialDefinition material() {
        return new MaterialDefinition(
                "testium",
                "testium",
                Optional.empty(),
                0,
                "#808080",
                "metallic",
                List.of(MaterialPrefixes.INGOT, MaterialPrefixes.INGOT_HOT),
                Map.of(),
                new ThermalProperties(1_000, 2_000, 7),
                false,
                Map.of(),
                false);
    }

    private static void installHeatComponent() {
        try {
            var component = net.minecraft.core.component.DataComponentType
                    .<HeatComponent>builder()
                    .persistent(HeatComponent.CODEC)
                    .networkSynchronized(HeatComponent.STREAM_CODEC)
                    .build();
            var holderField = DeferredHolder.class.getDeclaredField("holder");
            holderField.setAccessible(true);
            holderField.set(ModComponents.HEAT, Holder.direct(component));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(
                    "Unable to install test heat component",
                    exception);
        }
    }

    private record TestForm(MaterialPrefix form)
            implements MaterialFormItem {
        @Override
        public String materialId() {
            return "testium";
        }

        @Override
        public MaterialDefinition material() {
            return HotIngotLifecycleTest.material();
        }
    }
}
