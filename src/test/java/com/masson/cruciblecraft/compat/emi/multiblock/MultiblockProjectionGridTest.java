package com.masson.cruciblecraft.compat.emi.multiblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.gson.JsonParser;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.Element;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.Offset;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PalettePredicate;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PredicateKind;
import com.mojang.serialization.JsonOps;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;

import net.neoforged.fml.loading.LoadingModList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MultiblockProjectionGridTest {
    private static final List<String> STRUCTURES = List.of(
            "coke_oven.json",
            "distillation_tower.json",
            "implosion_compressor.json",
            "large_autoclave.json",
            "large_bath.json",
            "large_boiler_adamantium.json",
            "large_boiler_invar.json",
            "large_boiler_stainless_steel.json",
            "large_boiler_titanium.json",
            "large_boiler_tungstensteel.json",
            "large_centrifuge.json",
            "large_coagulator.json",
            "large_crucible.json",
            "large_crusher.json",
            "large_electrolyzer.json",
            "large_fermenter.json",
            "large_mixer.json",
            "large_oven.json",
            "large_shredder.json",
            "large_sluice.json",
            "large_squeezer.json",
            "tank_3x3x3.json",
            "tank_5x5x5.json");

    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    static MultiblockProjectionGrid projectCokeOven() {
        return project("coke_oven", load("coke_oven.json"), items());
    }

    @Test
    void cokeOvenIsOneHollowGrid() {
        MultiblockProjectionGrid grid = projectCokeOven();
        assertEquals(26, grid.cells().size());
        assertTrue(grid.cell(new Offset(0, 0, 1)).isEmpty());
        MultiblockProjectionGrid.Cell controller =
                grid.cell(Offset.ZERO).orElseThrow();
        assertEquals(id("coke_oven"), controller.block());
        assertEquals(List.of(-1, 0, 1), grid.layers());
        MultiblockProjectionGrid.Material bricks = material(grid, "F");
        assertEquals(25, bricks.count());
        assertEquals(List.of(id("firebrick")), bricks.itemBlocks());
        MultiblockProjectionGrid.Cell corner =
                grid.cell(new Offset(-1, -1, 0)).orElseThrow();
        assertEquals(
                new Offset(0, -1, -1),
                grid.presented(corner, Direction.EAST));
        assertEquals(
                corner.offset().rotate(Direction.EAST),
                grid.presented(corner, Direction.EAST));
        assertFalse(MultiblockProjectionGrid.recipeId(grid.structureId())
                .getPath()
                .contains("layer_"));
    }

    @Test
    void ovenCoilsShareOneExampleBlock() {
        ResourceLocation smaller = id("coil_a");
        ResourceLocation larger = id("coil_b");
        BlockFormIndex index = index(
                Map.of(id("large_oven_coils"), List.of(larger, smaller)),
                Set.of(smaller, larger, id("invar/wall"), id("large_oven")));
        MultiblockProjectionGrid grid = project(
                "large_oven", load("large_oven.json"), index);
        List<MultiblockProjectionGrid.Cell> coils = grid.cells().stream()
                .filter(cell -> cell.paletteKey().equals("N"))
                .toList();
        assertFalse(coils.isEmpty());
        assertTrue(coils.stream().allMatch(cell -> cell.block().equals(smaller)));
        assertEquals(List.of(smaller, larger), material(grid, "N").itemBlocks());
    }

    @Test
    void tankWallGroupSharesOneExampleAndKeepsTheTagSlot() {
        ResourceLocation smaller = id("wall_a");
        ResourceLocation larger = id("wall_b");
        ResourceLocation controller = id("tank_controller");
        BlockFormIndex index = index(
                Map.of(
                        id("tank_3x3x3_walls"), List.of(larger, smaller),
                        id("tank_3x3x3_controllers"), List.of(controller)),
                Set.of(smaller, larger, controller));
        MultiblockProjectionGrid grid = project(
                "tank_3x3x3", load("tank_3x3x3.json"), index);
        List<MultiblockProjectionGrid.Cell> walls = grid.cells().stream()
                .filter(cell -> cell.paletteKey().equals("P"))
                .toList();
        assertFalse(walls.isEmpty());
        assertTrue(walls.stream().allMatch(cell -> cell.block().equals(smaller)));
        assertEquals(List.of(smaller, larger), material(grid, "P").itemBlocks());
        assertEquals(List.of(controller), grid.controllers());
    }

    @Test
    void eachStructureJsonIsOneRecipeWithAController() {
        BlockFormIndex index = new BlockFormIndex() {
            @Override
            public List<ResourceLocation> blocksInTag(ResourceLocation tag) {
                return List.of(id("sample/" + tag.getPath()));
            }

            @Override
            public boolean hasItemForm(ResourceLocation block) {
                return true;
            }
        };
        assertEquals(23, STRUCTURES.size());
        for (String file : STRUCTURES) {
            String path = file.substring(0, file.length() - ".json".length());
            MultiblockProjectionGrid grid = project(path, load(file), index);
            assertFalse(grid.controllers().isEmpty(), path);
            assertFalse(
                    MultiblockProjectionGrid.recipeId(id(path))
                            .getPath()
                            .contains("layer_"),
                    path);
        }
    }

    @Test
    void emptyTagIntersectionDrawsNoModel() {
        ResourceLocation groupA = id("group_a");
        ResourceLocation groupB = id("group_b");
        MultiblockStructureDefinition definition = definition(Map.of(
                "C", controller(id("core")),
                "A", tag(groupA, "shared"),
                "B", tag(groupB, "shared")),
                List.of(
                        element(0, 0, 0, "C"),
                        element(1, 0, 0, "A"),
                        element(2, 0, 0, "B")));
        BlockFormIndex index = index(
                Map.of(
                        groupA, List.of(id("only_a")),
                        groupB, List.of(id("only_b"))),
                Set.of(id("core"), id("only_a"), id("only_b")));
        MultiblockProjectionGrid grid = project("empty", definition, index);
        assertTrue(grid.cell(new Offset(1, 0, 0)).isEmpty());
        assertTrue(grid.cell(new Offset(2, 0, 0)).isEmpty());
        assertEquals(id("core"), grid.cell(Offset.ZERO).orElseThrow().block());
    }

    @Test
    void explicitBlockWinsTheUniformGroup() {
        ResourceLocation brick = id("firebrick");
        ResourceLocation tagId = id("coils");
        MultiblockStructureDefinition definition = definition(Map.of(
                "C", controller(id("core")),
                "B", block(brick, "shell"),
                "T", tag(tagId, "shell")),
                List.of(
                        element(0, 0, 0, "C"),
                        element(1, 0, 0, "B"),
                        element(2, 0, 0, "T")));
        BlockFormIndex index = index(
                Map.of(tagId, List.of(id("coil_b"), id("coil_a"))),
                Set.of(id("core"), brick, id("coil_a"), id("coil_b")));
        MultiblockProjectionGrid grid = project("explicit", definition, index);
        assertEquals(brick, grid.cell(new Offset(1, 0, 0)).orElseThrow().block());
        assertEquals(brick, grid.cell(new Offset(2, 0, 0)).orElseThrow().block());
        assertEquals(List.of(id("coil_a"), id("coil_b")), material(grid, "T").itemBlocks());
    }

    @Test
    void missingControllerTagIsReportedEmpty() {
        MultiblockStructureDefinition definition = definition(
                Map.of("C", new PalettePredicate(
                        PredicateKind.CONTROLLER,
                        Optional.empty(),
                        Optional.of(id("missing_controllers")),
                        Optional.empty())),
                List.of(element(0, 0, 0, "C")));
        MultiblockProjectionGrid grid = project(
                "missing",
                definition,
                index(Map.of(), Set.of()));
        assertTrue(grid.controllers().isEmpty());
    }

    static MultiblockProjectionGrid project(
            String path,
            MultiblockStructureDefinition definition,
            BlockFormIndex index) {
        return MultiblockProjectionGrid.project(id(path), definition, index);
    }

    static MultiblockStructureDefinition load(String file) {
        var stream = MultiblockProjectionGridTest.class.getResourceAsStream(
                "/data/cruciblecraft/multiblock_structures/" + file);
        if (stream == null) {
            throw new IllegalStateException("Missing structure " + file);
        }
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return MultiblockStructureDefinition.CODEC.parse(
                    JsonOps.INSTANCE,
                    JsonParser.parseReader(reader))
                    .getOrThrow(AssertionError::new);
        } catch (java.io.IOException failure) {
            throw new IllegalStateException(file, failure);
        }
    }

    private static MultiblockStructureDefinition definition(
            Map<String, PalettePredicate> palette,
            List<Element> structure) {
        return new MultiblockStructureDefinition(
                1,
                new LinkedHashMap<>(palette),
                structure,
                Map.of(),
                Optional.empty());
    }

    private static PalettePredicate controller(ResourceLocation block) {
        return new PalettePredicate(
                PredicateKind.CONTROLLER,
                Optional.of(block),
                Optional.empty(),
                Optional.empty());
    }

    private static PalettePredicate block(ResourceLocation block, String group) {
        return new PalettePredicate(
                PredicateKind.BLOCK,
                Optional.of(block),
                Optional.empty(),
                Optional.empty(),
                Optional.of(group));
    }

    private static PalettePredicate tag(ResourceLocation tag, String group) {
        return new PalettePredicate(
                PredicateKind.TAG,
                Optional.empty(),
                Optional.of(tag),
                Optional.empty(),
                Optional.of(group));
    }

    private static Element element(int x, int y, int z, String key) {
        return new Element(new Offset(x, y, z), key);
    }

    private static MultiblockProjectionGrid.Material material(
            MultiblockProjectionGrid grid,
            String key) {
        return grid.materials().stream()
                .filter(row -> row.paletteKey().equals(key))
                .findFirst()
                .orElseThrow();
    }

    static BlockFormIndex items() {
        return new BlockFormIndex() {
            @Override
            public List<ResourceLocation> blocksInTag(ResourceLocation tag) {
                return List.of();
            }

            @Override
            public boolean hasItemForm(ResourceLocation block) {
                return true;
            }
        };
    }

    private static BlockFormIndex index(
            Map<ResourceLocation, List<ResourceLocation>> tags,
            Set<ResourceLocation> items) {
        return new BlockFormIndex() {
            @Override
            public List<ResourceLocation> blocksInTag(ResourceLocation tag) {
                return tags.getOrDefault(tag, List.of());
            }

            @Override
            public boolean hasItemForm(ResourceLocation block) {
                return items.contains(block);
            }
        };
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
