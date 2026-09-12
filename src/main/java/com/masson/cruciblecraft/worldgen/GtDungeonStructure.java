package com.masson.cruciblecraft.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.masson.cruciblecraft.content.block.GtStoneBlock;
import com.masson.cruciblecraft.registry.ModBlockTags;
import com.masson.cruciblecraft.registry.ModStructures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

/**
 * The modern structure carrier for GT6 {@code WorldgenDungeonGT}.
 *
 * <p>GT6 invokes this generator from a per-chunk worldgen hook. NeoForge
 * structures are the safe equivalent because a single start can own the
 * complete 5x5..9x9 chunk footprint and let the engine post-process each
 * intersecting chunk.</p>
 */
public final class GtDungeonStructure extends Structure {
    public static final byte EMPTY = 0;
    public static final byte BARRACKS = -1;
    public static final byte ENTRANCE = -2;
    public static final byte CORRIDOR = -128;
    public static final byte ROOM = 1;
    public static final int PROBABILITY = 100;
    public static final int MIN_SIZE = 3;
    public static final int MAX_SIZE = 7;
    public static final int MIN_Y = 20;
    public static final int MAX_Y = 20;
    public static final int ROOM_CHANCE = 6;

    public static final com.mojang.serialization.MapCodec<GtDungeonStructure> CODEC =
            simpleCodec(GtDungeonStructure::new);

    public GtDungeonStructure(StructureSettings settings) {
        super(settings);
    }

    public static boolean isAlignedChunk(int chunkX, int chunkZ) {
        int period = MAX_SIZE + 4;
        int expected = period / 2;
        return Math.floorMod(Math.abs(chunkX), period) == expected
                && Math.floorMod(Math.abs(chunkZ), period) == expected;
    }

    public static boolean isOutsideOriginExclusion(int chunkX, int chunkZ) {
        int radius = 256 + MAX_SIZE * 16;
        return !(Math.abs(chunkX * 16) < radius
                && Math.abs(chunkZ * 16) < radius);
    }

    public static byte[][] buildLayout(RandomSource random) {
        int width = 2 + MIN_SIZE + random.nextInt(1 + MAX_SIZE - MIN_SIZE);
        int depth = 2 + MIN_SIZE + random.nextInt(1 + MAX_SIZE - MIN_SIZE);
        byte[][] layout = new byte[width][depth];
        int centerX = width / 2;
        int centerZ = depth / 2;

        int importantRooms = 0;
        while (importantRooms < 2) {
            int x = 1 + random.nextInt(width - 2);
            int z = 1 + random.nextInt(depth - 2);
            if (layout[x][z] == EMPTY) {
                layout[x][z] = importantRooms == 0 ? BARRACKS : ENTRANCE;
                importantRooms++;
            }
        }

        int roomCount = 0;
        while (roomCount < 2) {
            for (int x = 1; x < width - 1; x++) {
                for (int z = 1; z < depth - 1; z++) {
                    if (layout[x][z] == EMPTY
                            && random.nextInt(ROOM_CHANCE) == 0) {
                        layout[x][z] = ROOM;
                        roomCount++;
                    }
                }
            }
        }
        if (layout[centerX][centerZ] == EMPTY) {
            layout[centerX][centerZ] = ROOM;
        }

        for (int x = 1; x < width - 1; x++) {
            for (int z = 1; z < depth - 1; z++) {
                if (layout[x][z] == EMPTY) {
                    continue;
                }
                int cursorX = x;
                int cursorZ = z;
                while (cursorX != centerX) {
                    cursorX += Integer.compare(centerX, cursorX);
                    if (layout[cursorX][cursorZ] == EMPTY) {
                        layout[cursorX][cursorZ] = CORRIDOR;
                    } else {
                        break;
                    }
                }
                while (cursorZ != centerZ) {
                    cursorZ += Integer.compare(centerZ, cursorZ);
                    if (layout[cursorX][cursorZ] == EMPTY) {
                        layout[cursorX][cursorZ] = CORRIDOR;
                    } else {
                        break;
                    }
                }
            }
        }
        pruneCorridors(layout);
        return layout;
    }

    private static void pruneCorridors(byte[][] layout) {
        pruneCorridorsPass(layout, false);
        pruneCorridorsPass(layout, true);
    }

    private static void pruneCorridorsPass(
            byte[][] layout,
            boolean includeDiagonals) {
        boolean changed;
        do {
            changed = false;
            for (int x = 1; x < layout.length - 1; x++) {
                for (int z = 1; z < layout[x].length - 1; z++) {
                    if (layout[x][z] != CORRIDOR) {
                        continue;
                    }
                    boolean north = occupied(layout, x, z - 1);
                    boolean south = occupied(layout, x, z + 1);
                    boolean west = occupied(layout, x - 1, z);
                    boolean east = occupied(layout, x + 1, z);
                    if ((north && south && !west && !east)
                            || (west && east && !north && !south)) {
                        continue;
                    }
                    int neighbors = (north ? 1 : 0)
                            + (south ? 1 : 0)
                            + (west ? 1 : 0)
                            + (east ? 1 : 0);
                    boolean northwest = occupied(layout, x - 1, z - 1);
                    boolean northeast = occupied(layout, x + 1, z - 1);
                    boolean southwest = occupied(layout, x - 1, z + 1);
                    boolean southeast = occupied(layout, x + 1, z + 1);
                    if (neighbors <= 1) {
                        layout[x][z] = EMPTY;
                        changed = true;
                        continue;
                    }
                    if (includeDiagonals) {
                        int connectionCount = neighbors
                                + (northwest ? 1 : 0)
                                + (northeast ? 1 : 0)
                                + (southwest ? 1 : 0)
                                + (southeast ? 1 : 0);
                        if (connectionCount >= 7
                                || (connectionCount == 5
                                    && ((!east && !northeast && !southeast)
                                        || (!west && !northwest && !southwest)
                                        || (!north && !northwest && !northeast)
                                        || (!south && !southwest && !southeast)))) {
                            layout[x][z] = EMPTY;
                            changed = true;
                            continue;
                        }
                    }
                    if ((east && southeast && south && !west && !north)
                            || (east && northeast && north && !west && !south)
                            || (west && southwest && south && !east && !north)
                            || (west && northwest && north && !east && !south)) {
                        layout[x][z] = EMPTY;
                        changed = true;
                    }
                }
            }
        } while (changed);
    }

    private static boolean occupied(byte[][] layout, int x, int z) {
        return layout[x][z] != EMPTY;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        if (context.random().nextInt(PROBABILITY) != 0
                || !isAlignedChunk(chunk.x, chunk.z)
                || !isOutsideOriginExclusion(chunk.x, chunk.z)) {
            return Optional.empty();
        }

        List<BlockState> stones = stonePalette(
                context.registryAccess().lookupOrThrow(Registries.BLOCK));
        if (stones.size() < 2) {
            return Optional.empty();
        }

        int baseY = MIN_Y;
        byte[][] layout = buildLayout(context.random());
        int startX = chunk.getMinBlockX() - (layout.length / 2) * 16;
        int startZ = chunk.getMinBlockZ() - (layout[0].length / 2) * 16;
        BlockState primary = stones.get(context.random().nextInt(stones.size()));
        BlockState secondary = stones.get(context.random().nextInt(stones.size()));
        BlockPos anchor = new BlockPos(startX, baseY, startZ);
        return Optional.of(new GenerationStub(
                anchor,
                builder -> addPiece(
                        builder,
                        anchor,
                chunk.getMinBlockX() + 8,
                chunk.getMinBlockZ() + 8,
                        layout,
                        primary,
                        secondary)));
    }

    private static void addPiece(
            StructurePiecesBuilder builder,
            BlockPos anchor,
            int probeX,
            int probeZ,
            byte[][] layout,
            BlockState primary,
            BlockState secondary) {
        builder.addPiece(new GtDungeonPiece(
                ModStructures.GT_DUNGEON_PIECE.get(),
                anchor,
                probeX,
                probeZ,
                layout,
                primary,
                secondary));
    }

    private static List<BlockState> stonePalette(HolderGetter<Block> blocks) {
        List<BlockState> states = new ArrayList<>();
        for (Holder<Block> holder : blocks.getOrThrow(ModBlockTags.GT_STONES)) {
            if (holder.value() instanceof GtStoneBlock block) {
                states.add(block.defaultBlockState());
            }
        }
        return states;
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.GT_DUNGEON_TYPE.get();
    }
}
