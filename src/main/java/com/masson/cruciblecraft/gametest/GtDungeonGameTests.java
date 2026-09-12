package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.registry.ModStructures;
import com.masson.cruciblecraft.worldgen.GtDungeonStructure;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated GT dungeon gate. Run with
 * {@code -PgameTestNamespaces=cruciblecraft_wave_worldgen_gt_dungeon}.
 */
@GameTestHolder(GtDungeonGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class GtDungeonGameTests {
    public static final String NAMESPACE = "cruciblecraft_wave_worldgen_gt_dungeon";

    private GtDungeonGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void structureRegistriesAreLoaded(GameTestHelper helper) {
        helper.assertTrue(
                ModStructures.GT_DUNGEON_TYPE.get() != null,
                "gt_dungeon structure type is not registered");
        helper.assertTrue(
                ModStructures.GT_DUNGEON_PIECE.get() != null,
                "gt_dungeon structure piece is not registered");
        Structure structure = helper.getLevel()
                .registryAccess()
                .registryOrThrow(Registries.STRUCTURE)
                .get(ResourceLocation.fromNamespaceAndPath(
                        "cruciblecraft", "gt_dungeon"));
        StructureSet structureSet = helper.getLevel()
                .registryAccess()
                .registryOrThrow(Registries.STRUCTURE_SET)
                .get(ResourceLocation.fromNamespaceAndPath(
                        "cruciblecraft", "gt_dungeon"));
        helper.assertTrue(structure != null, "gt_dungeon structure missing");
        helper.assertTrue(structureSet != null, "gt_dungeon structure set missing");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void sourcePlacementGateIsStable(GameTestHelper helper) {
        helper.assertTrue(
                GtDungeonStructure.isAlignedChunk(27, 27),
                "27/27 must satisfy the source %11 == 5 alignment");
        helper.assertTrue(
                GtDungeonStructure.isAlignedChunk(5, 5),
                "5/5 must satisfy the source lattice");
        helper.assertTrue(
                GtDungeonStructure.isAlignedChunk(27, 5),
                "27/5 must satisfy the source lattice");
        helper.assertFalse(
                GtDungeonStructure.isAlignedChunk(28, 27),
                "28/27 must fail the source alignment");
        helper.assertFalse(
                GtDungeonStructure.isAlignedChunk(0, 5),
                "0/5 must fail the two-axis source lattice");
        helper.assertFalse(
                GtDungeonStructure.isOutsideOriginExclusion(5, 5),
                "origin-adjacent candidate must be excluded");
        helper.assertTrue(
                GtDungeonStructure.isOutsideOriginExclusion(27, 27),
                "far candidate must pass the origin exclusion");
        helper.assertTrue(
                GtDungeonStructure.isOutsideOriginExclusion(0, 25),
                "one-axis-inside candidate is allowed when streets are disabled");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void layoutRemainsConnected(GameTestHelper helper) {
        byte[][] layout =
                GtDungeonStructure.buildLayout(RandomSource.create(1L));
        int occupied = 0;
        for (int x = 1; x < layout.length - 1; x++) {
            for (int z = 1; z < layout[x].length - 1; z++) {
                if (layout[x][z] != GtDungeonStructure.EMPTY) {
                    occupied++;
                }
            }
        }
        helper.assertTrue(occupied >= 2, "dungeon needs at least two rooms");
        helper.assertTrue(
                layout[layout.length / 2][layout[0].length / 2]
                        != GtDungeonStructure.EMPTY,
                "dungeon layout must contain a central room");
        helper.assertTrue(
                contains(layout, GtDungeonStructure.BARRACKS)
                        && contains(layout, GtDungeonStructure.ENTRANCE),
                "dungeon layout must carry the source important-room codes");
        helper.assertTrue(
                layout.length >= 5 && layout.length <= 9,
                "dungeon width escaped the 3..7 source range");
        helper.assertTrue(
                layout[0].length >= 5 && layout[0].length <= 9,
                "dungeon depth escaped the 3..7 source range");
        helper.succeed();
    }

    private static boolean contains(byte[][] layout, byte value) {
        for (byte[] row : layout) {
            for (byte cell : row) {
                if (cell == value) {
                    return true;
                }
            }
        }
        return false;
    }
}
