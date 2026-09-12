package com.masson.cruciblecraft.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.masson.cruciblecraft.logistics.pipe.cover.CoverBehaviorRegistry;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverComponentTiers;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverDefinitionCatalog;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverKinds;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class RegistryIdentityExactnessTest {
    private static final List<String> EXPECTED_BEHAVIORS = List.of(
            "cruciblecraft:controller_auto",
            "cruciblecraft:controller_auto_redstone",
            "cruciblecraft:controller_auto_timer",
            "cruciblecraft:controller_covers",
            "cruciblecraft:controller_display",
            "cruciblecraft:controller_redstone",
            "cruciblecraft:conveyor",
            "cruciblecraft:cover_blank",
            "cruciblecraft:detector_running",
            "cruciblecraft:display_energy",
            "cruciblecraft:filter",
            "cruciblecraft:logistics_display_cpu",
            "cruciblecraft:logistics_fluid_storage",
            "cruciblecraft:logistics_fluid_transfer",
            "cruciblecraft:logistics_generic_dump",
            "cruciblecraft:logistics_generic_storage",
            "cruciblecraft:logistics_generic_transfer",
            "cruciblecraft:logistics_item_storage",
            "cruciblecraft:logistics_item_transfer",
            "cruciblecraft:pressure_valve",
            "cruciblecraft:pump_adapter",
            "cruciblecraft:redstone_conductor_in",
            "cruciblecraft:redstone_conductor_out",
            "cruciblecraft:redstone_emitter",
            "cruciblecraft:redstone_repeater",
            "cruciblecraft:redstone_torch",
            "cruciblecraft:retriever_item",
            "cruciblecraft:robot_arm",
            "cruciblecraft:scale_energy",
            "cruciblecraft:scale_progress",
            "cruciblecraft:selector_button_panel",
            "cruciblecraft:selector_manual",
            "cruciblecraft:selector_redstone",
            "cruciblecraft:selector_tag",
            "cruciblecraft:shutter",
            "cruciblecraft:vent");

    @Test
    void coverCatalogAndBehaviorsAreExactNotASubset() {
        CoverBehaviorRegistry.validateDefinitions();
        assertEquals(
                19
                        + 4
                        + MachineCoverKinds.DEFINITION_COUNT
                        + CoverComponentTiers.definitionIds().size(),
                CoverDefinitionCatalog.definitions().size());
        List<String> ids = CoverBehaviorRegistry.registeredIds().stream()
                .map(ResourceLocation::toString)
                .sorted()
                .toList();
        assertEquals(EXPECTED_BEHAVIORS, ids);
        Set<String> definitionIds = CoverDefinitionCatalog.definitions()
                .stream()
                .map(definition -> definition.id().toString())
                .collect(Collectors.toSet());
        assertTrue(definitionIds.containsAll(Set.of(
                "cruciblecraft:logistics_item_storage",
                "cruciblecraft:logistics_item_import",
                "cruciblecraft:logistics_item_export",
                "cruciblecraft:logistics_fluid_storage",
                "cruciblecraft:logistics_fluid_import",
                "cruciblecraft:logistics_fluid_export",
                "cruciblecraft:logistics_generic_storage",
                "cruciblecraft:logistics_generic_import",
                "cruciblecraft:logistics_generic_export",
                "cruciblecraft:logistics_generic_dump",
                "cruciblecraft:logistics_display_cpu_logic",
                "cruciblecraft:logistics_display_cpu_control",
                "cruciblecraft:logistics_display_cpu_storage",
                "cruciblecraft:logistics_display_cpu_conversion")));
    }
}
