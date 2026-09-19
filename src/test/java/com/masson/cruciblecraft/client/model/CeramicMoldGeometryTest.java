package com.masson.cruciblecraft.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.core.Direction;

class CeramicMoldGeometryTest {
    @Test
    void emptyPass0ClaySheetIsDroppedFromTheFrame() {
        BlockElement sheet = sheet("#body");
        BlockElement wall = wall();
        List<BlockElement> frame = CeramicMoldGeometry.staticFrame(List.of(sheet, wall));
        assertTrue(CeramicMoldGeometry.isGt6Pass0(sheet));
        assertFalse(CeramicMoldGeometry.isMoltenSheet(sheet));
        assertEquals(1, frame.size());
        assertEquals(wall, frame.get(0));
    }

    @Test
    void filledMoltenSheetStaysInTheFrame() {
        BlockElement sheet = sheet("#molten");
        BlockElement wall = wall();
        List<BlockElement> frame = CeramicMoldGeometry.staticFrame(List.of(sheet, wall));
        assertTrue(CeramicMoldGeometry.isMoltenSheet(sheet));
        assertEquals(2, frame.size());
        assertEquals(sheet, frame.get(0));
    }

    @Test
    void foundryFloorIsNotTheGt6Pass0Sheet() {
        BlockElement floor = new BlockElement(
                new Vector3f(0.0F, 0.0F, 0.0F),
                new Vector3f(16.0F, 1.0F, 16.0F),
                Map.of(Direction.UP, face("#body")),
                null,
                true);
        assertFalse(CeramicMoldGeometry.isGt6Pass0(floor));
        List<BlockElement> frame = CeramicMoldGeometry.staticFrame(List.of(floor, wall()));
        assertEquals(2, frame.size());
        assertEquals(floor, frame.get(0));
    }

    @Test
    void bakedFoundryInnerCellsAreDroppedFromTheFrame() {
        BlockElement cell = new BlockElement(
                new Vector3f(2.0F, 0.0F, 2.0F),
                new Vector3f(4.4F, 3.0F, 4.4F),
                Map.of(Direction.UP, face("#body")),
                null,
                true);
        BlockElement wall = wall();
        assertTrue(CeramicMoldGeometry.isBakedInnerCell(cell));
        List<BlockElement> frame = CeramicMoldGeometry.staticFrame(List.of(wall, cell));
        assertEquals(1, frame.size());
        assertEquals(wall, frame.get(0));
    }

    private static BlockElement sheet(String upTexture) {
        Map<Direction, BlockElementFace> faces = new EnumMap<>(Direction.class);
        faces.put(Direction.UP, face(upTexture));
        return new BlockElement(
                new Vector3f(1.0F, 1.0F, 1.0F),
                new Vector3f(15.0F, 2.92F, 15.0F),
                faces,
                null,
                true);
    }

    private static BlockElement wall() {
        Map<Direction, BlockElementFace> faces = new EnumMap<>(Direction.class);
        faces.put(Direction.NORTH, face("#body"));
        return new BlockElement(
                new Vector3f(0.0F, 0.0F, 0.0F),
                new Vector3f(2.0F, 4.0F, 16.0F),
                faces,
                null,
                true);
    }

    private static BlockElementFace face(String texture) {
        return new BlockElementFace(
                null, 0, texture, new BlockFaceUV(new float[] {0, 0, 16, 16}, 0));
    }
}
