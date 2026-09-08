package com.masson.cruciblecraft.content.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.masson.cruciblecraft.TestExtruderShapes;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.loading.LoadingModList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ExtruderShapeCatalogTest {
    @BeforeAll
    static void bootstrap() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void catalogHasThirtyFiveStableCompleteUniqueShapes() {
        assertEquals(35, ExtruderShapeCatalog.DEFINITIONS.size());
        assertEquals(35, new HashSet<>(ExtruderShapeCatalog.DEFINITIONS.stream()
                .map(ExtruderShapeCatalog.Definition::id).toList()).size());
        assertEquals(35, new HashSet<>(ExtruderShapeCatalog.DEFINITIONS.stream()
                .map(ExtruderShapeCatalog.Definition::registryPath).toList()).size());
        assertTrue(ExtruderShapeCatalog.DEFINITIONS.stream().allMatch(shape ->
                shape.registryPath().equals("extruder_shape_" + shape.id())
                        && !shape.englishName().isBlank()
                        && !shape.chineseName().isBlank()));
    }

    @Test
    void shapeIdentityIsReusableAndNonStacking() {
        assertEquals(1, ExtruderShapeCatalog.MAX_STACK_SIZE);
        assertTrue(ExtruderShapeCatalog.isShape(TestExtruderShapes.stack()));
        assertFalse(ExtruderShapeCatalog.isShape(new ItemStack(Items.IRON_INGOT)));
    }
}
