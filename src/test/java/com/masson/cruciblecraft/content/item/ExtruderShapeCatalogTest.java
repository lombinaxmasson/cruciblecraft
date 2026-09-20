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
    void catalogMatchesGt6ShapeExtruderFamily() {
        assertEquals(32, ExtruderShapeCatalog.DEFINITIONS.size());
        assertEquals(32, new HashSet<>(ExtruderShapeCatalog.DEFINITIONS.stream()
                .map(ExtruderShapeCatalog.Definition::id).toList()).size());
        assertEquals(
                List.of(
                        10000, 10001, 10002, 10003, 10004, 10005, 10006, 10007,
                        10008, 10009, 10010, 10011, 10012, 10013, 10014, 10015,
                        10016, 10017, 10018, 10019, 10020, 10021, 10022, 10023,
                        10024, 10025, 10026, 10027, 10028, 10029, 10030, 10031),
                ExtruderShapeCatalog.DEFINITIONS.stream()
                        .map(ExtruderShapeCatalog.Definition::gt6Meta)
                        .toList());
        assertEquals("empty", ExtruderShapeCatalog.DEFINITIONS.getFirst().id());
        assertEquals("ccc", ExtruderShapeCatalog.require("ccc").id());
        assertTrue(ExtruderShapeCatalog.DEFINITIONS.stream().noneMatch(shape ->
                shape.id().equals("rotor")
                        || shape.id().contains("item_pipe")));
        assertTrue(ExtruderShapeCatalog.DEFINITIONS.stream().allMatch(shape ->
                shape.registryPath().equals("extruder_shape_" + shape.id())
                        && !shape.englishName().isBlank()
                        && shape.chineseName().contains("挤出模具")));
    }

    @Test
    void shapeIdentityIsReusableAndNonStacking() {
        assertEquals(1, ExtruderShapeCatalog.MAX_STACK_SIZE);
        assertTrue(ExtruderShapeCatalog.isShape(TestExtruderShapes.stack()));
        assertFalse(ExtruderShapeCatalog.isShape(new ItemStack(Items.IRON_INGOT)));
    }
}
