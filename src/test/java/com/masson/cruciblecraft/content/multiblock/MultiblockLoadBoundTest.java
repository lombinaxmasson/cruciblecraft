package com.masson.cruciblecraft.content.multiblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.Element;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.Offset;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PalettePredicate;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PredicateKind;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureValidator.Status;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureValidator.StructureAccess;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.fml.loading.LoadingModList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * T23 D1 load evidence: worst-case structure validation bounds.
 *
 * <p>The synthetic definition fills the whole {@code MAX_SCAN_VOLUME}
 * budget (4,096 explicit positions), so the accessor-call counts below
 * are the deterministic worst case, not an average. The same counts are
 * pinned by {@code tools/build_t23_load_bounds.py} via the committed
 * evidence table; wall-clock elapsed is recorded here for reference
 * only and is never gated (CI jitter).
 */
class MultiblockLoadBoundTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void worstCaseValidationIsElementLinearAndBounded() {
        MultiblockStructureDefinition definition = worstCaseDefinition();
        assertEquals(
                MultiblockStructureDefinition.MAX_SCAN_VOLUME,
                definition.structure().size());

        long worstWallClockNanos = 0L;
        long roundTotalCalls = 0L;
        CounterAccess access = new CounterAccess();
        for (int round = 0; round < 16; round++) {
            access = new CounterAccess();
            long started = System.nanoTime();
            var result = MultiblockStructureValidator.validate(
                    definition,
                    access,
                    BlockPos.ZERO,
                    Direction.NORTH);
            long elapsed = System.nanoTime() - started;
            worstWallClockNanos = Math.max(worstWallClockNanos, elapsed);
            assertEquals(Status.VALID, result.status());
            long calls = access.totalCalls();
            assertEquals(
                    access.isLoadedCalls(), access.blockStateCalls(),
                    "validator must check each position exactly once");
            roundTotalCalls = Math.max(roundTotalCalls, calls);
        }

        // Deterministic worst-case bounds: one isLoaded + one blockState
        // per structure element, plus one extra pair for the controller
        // position itself. No block entity reads in a port-free structure.
        long positions = definition.structure().size();
        assertTrue(
                access.isLoadedCalls() <= positions + 1,
                "isLoaded calls exceeded the worst case: "
                        + access.isLoadedCalls());
        assertTrue(
                access.blockStateCalls() <= positions + 1,
                "blockState calls exceeded the worst case: "
                        + access.blockStateCalls());
        assertEquals(0, access.blockEntityCalls());

        // Wall-clock is recorded for reference only — never gated.
        assertTrue(worstWallClockNanos >= 0L);
        System.out.println(
                "[T23 load] worst-case validation over "
                        + positions
                        + " positions: "
                        + roundTotalCalls
                        + " accessor calls, worst wall-clock "
                        + worstWallClockNanos
                        + " ns");
    }

    private static MultiblockStructureDefinition worstCaseDefinition() {
        Map<String, PalettePredicate> palette = Map.of(
                "C", new PalettePredicate(
                        PredicateKind.CONTROLLER,
                        java.util.Optional.of(
                                net.minecraft.resources.ResourceLocation
                                        .parse("minecraft:stone")),
                        java.util.Optional.empty(),
                        java.util.Optional.empty()),
                "B", new PalettePredicate(
                        PredicateKind.BLOCK,
                        java.util.Optional.of(
                                net.minecraft.resources.ResourceLocation
                                        .parse("minecraft:dirt")),
                        java.util.Optional.empty(),
                        java.util.Optional.empty()));
        List<Element> elements = new ArrayList<>();
        elements.add(new Element(Offset.ZERO, "C"));
        int filled = 1;
        for (int x = 0; x < 16 && filled
                < MultiblockStructureDefinition.MAX_SCAN_VOLUME; x++) {
            for (int y = 0; y < 16 && filled
                    < MultiblockStructureDefinition.MAX_SCAN_VOLUME; y++) {
                for (int z = 0; z < 16 && filled
                        < MultiblockStructureDefinition.MAX_SCAN_VOLUME; z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }
                    elements.add(new Element(new Offset(x, y, z), "B"));
                    filled++;
                }
            }
        }
        return new MultiblockStructureDefinition(
                1, palette, elements, Map.of(), java.util.Optional.empty());
    }

    private static final class CounterAccess implements StructureAccess {
        private long isLoadedCalls;
        private long blockStateCalls;
        private long blockEntityCalls;

        long isLoadedCalls() {
            return isLoadedCalls;
        }

        long blockStateCalls() {
            return blockStateCalls;
        }

        long blockEntityCalls() {
            return blockEntityCalls;
        }

        long totalCalls() {
            return isLoadedCalls + blockStateCalls + blockEntityCalls;
        }

        @Override
        public boolean isLoaded(BlockPos pos) {
            isLoadedCalls++;
            return true;
        }

        @Override
        public BlockState blockState(BlockPos pos) {
            blockStateCalls++;
            return pos.equals(BlockPos.ZERO)
                    ? Blocks.STONE.defaultBlockState()
                    : Blocks.DIRT.defaultBlockState();
        }

        @Override
        public BlockEntity blockEntity(BlockPos pos) {
            blockEntityCalls++;
            return null;
        }
    }
}
