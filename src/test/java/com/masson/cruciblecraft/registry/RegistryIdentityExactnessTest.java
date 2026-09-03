package com.masson.cruciblecraft.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.masson.cruciblecraft.logistics.pipe.cover.CoverBehaviorRegistry;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverDefinitionCatalog;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class RegistryIdentityExactnessTest {
    private static final List<String> EXPECTED_BEHAVIORS = List.of(
            "cruciblecraft:conveyor",
            "cruciblecraft:filter",
            "cruciblecraft:logistics_fluid_storage",
            "cruciblecraft:logistics_fluid_transfer",
            "cruciblecraft:logistics_item_storage",
            "cruciblecraft:logistics_item_transfer",
            "cruciblecraft:pressure_valve",
            "cruciblecraft:pump_adapter",
            "cruciblecraft:retriever_item",
            "cruciblecraft:robot_arm",
            "cruciblecraft:selector_manual",
            "cruciblecraft:shutter");

    @Test
    void coverCatalogAndBehaviorsAreExactNotASubset() {
        CoverBehaviorRegistry.validateDefinitions();
        assertEquals(15, CoverDefinitionCatalog.definitions().size());
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
                "cruciblecraft:logistics_fluid_export")));
    }
}
