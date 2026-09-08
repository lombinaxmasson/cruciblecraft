package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.JsonParser;
import com.masson.cruciblecraft.material.CellContentGate;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class ContainerResourceTest {
    private static final Path DATA = Path.of(
            "src/main/resources/data/cruciblecraft");
    private static final Path GENERATED = Path.of(
            "src/generated/resources");

    @Test
    void runtimeGatesCloseAllDomainsWithoutPerFluidItems() throws Exception {
        Map<ResourceLocation, CellContentGate.Kind> entries =
                CellContentGate.entries();
        long fluidCount = entries.values().stream()
                .filter(kind -> kind == CellContentGate.Kind.FLUID)
                .count();
        long gasCount = entries.values().stream()
                .filter(kind -> kind == CellContentGate.Kind.GAS)
                .count();
        assertEquals(entries.size(), fluidCount + gasCount);
        assertTrue(entries.keySet().stream()
                .allMatch(id -> id.getNamespace().equals("cruciblecraft")));
        assertEquals(
                CellContentGate.Kind.FLUID,
                entries.get(ResourceLocation.parse("cruciblecraft:chlorine")));
        assertEquals(
                CellContentGate.Kind.GAS,
                entries.get(ResourceLocation.parse("cruciblecraft:fluorine")));
        assertFalse(entries.containsKey(
                ResourceLocation.parse("cruciblecraft:milk")));

        var fluidGate = JsonParser.parseString(Files.readString(
                DATA.resolve("container_fluid_gate.json")))
                .getAsJsonObject().getAsJsonArray("fluids");
        Set<String> materialIds = fluidGate.asList().stream()
                .map(value -> value.getAsJsonObject()
                        .get("material").getAsString())
                .collect(Collectors.toSet());
        assertEquals(fluidGate.size(), materialIds.size());
        assertTrue(fluidGate.size() > 0);
        assertTrue(materialIds.stream().noneMatch(material ->
                Files.exists(GENERATED.resolve(
                        "assets/cruciblecraft/models/item/"
                                + material + "_cell.json"))));
    }

    @Test
    void bothCellsHaveGeneratedModelsRecipesAndBilingualNames()
            throws Exception {
        for (String id : Set.of("fluid_cell", "gas_cell")) {
            assertTrue(Files.isRegularFile(GENERATED.resolve(
                    "assets/cruciblecraft/models/item/" + id + ".json")));
            assertTrue(Files.isRegularFile(GENERATED.resolve(
                    "data/cruciblecraft/recipe/" + id + ".json")));
        }
        String english = Files.readString(GENERATED.resolve(
                "assets/cruciblecraft/lang/en_us.json"));
        String chinese = Files.readString(GENERATED.resolve(
                "assets/cruciblecraft/lang/zh_cn.json"));
        assertTrue(english.contains("\"item.cruciblecraft.fluid_cell\""));
        assertTrue(english.contains("\"item.cruciblecraft.gas_cell\""));
        assertTrue(chinese.contains("\"item.cruciblecraft.fluid_cell\""));
        assertTrue(chinese.contains("\"item.cruciblecraft.gas_cell\""));
    }
}
