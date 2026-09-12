package com.masson.cruciblecraft.worldgen;

import com.masson.cruciblecraft.registry.ModStructures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.util.RandomSource;

/**
 * One serialized GT dungeon footprint. Keeping the complete footprint in one
 * structure piece lets Minecraft clip writes to the current chunk safely.
 */
public final class GtDungeonPiece extends StructurePiece {
    private static final int ROOM_HEIGHT = 7;
    private final int startX;
    private final int startZ;
    private final int baseY;
    private final int probeX;
    private final int probeZ;
    private final byte[][] layout;
    private final BlockState primary;
    private final BlockState secondary;

    public GtDungeonPiece(
            StructurePieceType type,
            BlockPos anchor,
            int probeX,
            int probeZ,
            byte[][] layout,
            BlockState primary,
            BlockState secondary) {
        super(
                type,
                0,
                new BoundingBox(
                        anchor.getX(),
                        anchor.getY(),
                        anchor.getZ(),
                        anchor.getX() + layout.length * 16 - 1,
                        anchor.getY() + ROOM_HEIGHT,
                        anchor.getZ() + layout[0].length * 16 - 1));
        this.startX = anchor.getX();
        this.startZ = anchor.getZ();
        this.baseY = anchor.getY();
        this.probeX = probeX;
        this.probeZ = probeZ;
        this.layout = copyLayout(layout);
        this.primary = primary;
        this.secondary = secondary;
    }

    public GtDungeonPiece(
            StructurePieceSerializationContext context,
            CompoundTag tag) {
        super(ModStructures.GT_DUNGEON_PIECE.get(), tag);
        this.startX = tag.getInt("start_x");
        this.startZ = tag.getInt("start_z");
        this.baseY = tag.getInt("base_y");
        this.probeX = tag.getInt("probe_x");
        this.probeZ = tag.getInt("probe_z");
        int width = tag.getInt("width");
        int depth = tag.getInt("depth");
        this.layout = new byte[width][depth];
        for (int x = 0; x < width; x++) {
            byte[] row = tag.getByteArray("row_" + x);
            for (int z = 0; z < depth && z < row.length; z++) {
                this.layout[x][z] = row[z];
            }
        }
        HolderGetter<Block> blocks =
                context.registryAccess().lookupOrThrow(Registries.BLOCK);
        this.primary = NbtUtils.readBlockState(
                blocks, tag.getCompound("primary"));
        this.secondary = NbtUtils.readBlockState(
                blocks, tag.getCompound("secondary"));
    }

    @Override
    protected void addAdditionalSaveData(
            StructurePieceSerializationContext context,
            CompoundTag tag) {
        tag.putInt("start_x", startX);
        tag.putInt("start_z", startZ);
        tag.putInt("base_y", baseY);
        tag.putInt("probe_x", probeX);
        tag.putInt("probe_z", probeZ);
        tag.putInt("width", layout.length);
        tag.putInt("depth", layout[0].length);
        for (int x = 0; x < layout.length; x++) {
            byte[] row = new byte[layout[x].length];
            for (int z = 0; z < layout[x].length; z++) {
                row[z] = layout[x][z];
            }
            tag.putByteArray("row_" + x, row);
        }
        tag.put("primary", NbtUtils.writeBlockState(primary));
        tag.put("secondary", NbtUtils.writeBlockState(secondary));
    }

    @Override
    public void postProcess(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkGenerator chunkGenerator,
            RandomSource random,
            BoundingBox chunkBox,
            ChunkPos chunkPos,
            BlockPos pivot) {
        if (!level.getBlockState(new BlockPos(
                        probeX,
                        level.getMinBuildHeight(),
                        probeZ))
                .is(Blocks.BEDROCK)) {
            return;
        }

        for (int x = 1; x < layout.length - 1; x++) {
            for (int z = 1; z < layout[x].length - 1; z++) {
                if (layout[x][z] == GtDungeonStructure.EMPTY) {
                    continue;
                }
                if (layout[x][z] == GtDungeonStructure.CORRIDOR) {
                    placeCorridor(
                            level,
                            chunkBox,
                            startX + x * 16,
                            startZ + z * 16);
                } else {
                    placeRoom(
                            level,
                            chunkBox,
                            startX + x * 16,
                            startZ + z * 16);
                }
            }
        }
        for (int x = 1; x < layout.length - 1; x++) {
            for (int z = 1; z < layout[x].length - 1; z++) {
                if (layout[x][z] == GtDungeonStructure.EMPTY) {
                    continue;
                }
                if (x + 1 < layout.length - 1
                        && layout[x + 1][z] != GtDungeonStructure.EMPTY) {
                    carveDoor(
                            level,
                            chunkBox,
                            startX + (x + 1) * 16,
                            baseY,
                            startZ + z * 16,
                            true);
                }
                if (z + 1 < layout[x].length - 1
                        && layout[x][z + 1] != GtDungeonStructure.EMPTY) {
                    carveDoor(
                            level,
                            chunkBox,
                            startX + x * 16,
                            baseY,
                            startZ + (z + 1) * 16,
                            false);
                }
            }
        }
    }

    private void placeRoom(
            WorldGenLevel level,
            BoundingBox chunkBox,
            int roomX,
            int roomZ) {
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                boolean edge = dx == 0 || dx == 15 || dz == 0 || dz == 15;
                set(level, chunkBox, roomX + dx, baseY, roomZ + dz, primary);
                set(
                        level,
                        chunkBox,
                        roomX + dx,
                        baseY + ROOM_HEIGHT,
                        roomZ + dz,
                        secondary);
                for (int dy = 1; dy < ROOM_HEIGHT; dy++) {
                    set(
                            level,
                            chunkBox,
                            roomX + dx,
                            baseY + dy,
                            roomZ + dz,
                            edge ? secondary : Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private void placeCorridor(
            WorldGenLevel level,
            BoundingBox chunkBox,
            int roomX,
            int roomZ) {
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                boolean hall = dx >= 5 && dx <= 10 || dz >= 5 && dz <= 10;
                set(level, chunkBox, roomX + dx, baseY, roomZ + dz, primary);
                for (int dy = 1; dy <= 4; dy++) {
                    boolean wall = !hall
                            || dx == 5
                            || dx == 10
                            || dz == 5
                            || dz == 10;
                    set(
                            level,
                            chunkBox,
                            roomX + dx,
                            baseY + dy,
                            roomZ + dz,
                            wall ? secondary : Blocks.AIR.defaultBlockState());
                }
                set(
                        level,
                        chunkBox,
                        roomX + dx,
                        baseY + 5,
                        roomZ + dz,
                        secondary);
            }
        }
    }

    private static void carveDoor(
            WorldGenLevel level,
            BoundingBox chunkBox,
            int boundaryX,
            int baseY,
            int boundaryZ,
            boolean verticalWall) {
        for (int offset = -2; offset <= 2; offset++) {
            for (int dy = 1; dy <= 4; dy++) {
                int x = verticalWall ? boundaryX : boundaryX + 8 + offset;
                int z = verticalWall ? boundaryZ + 8 + offset : boundaryZ;
                set(level, chunkBox, x, baseY + dy, z, Blocks.AIR.defaultBlockState());
                if (verticalWall) {
                    set(level, chunkBox, x - 1, baseY + dy, z, Blocks.AIR.defaultBlockState());
                } else {
                    set(level, chunkBox, x, baseY + dy, z - 1, Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private static void set(
            WorldGenLevel level,
            BoundingBox chunkBox,
            int x,
            int y,
            int z,
            BlockState state) {
        BlockPos pos = new BlockPos(x, y, z);
        if (chunkBox.isInside(pos)) {
            level.setBlock(pos, state, Block.UPDATE_ALL);
        }
    }

    private static byte[][] copyLayout(byte[][] source) {
        byte[][] copy = new byte[source.length][];
        for (int x = 0; x < source.length; x++) {
            copy[x] = source[x].clone();
        }
        return copy;
    }
}
