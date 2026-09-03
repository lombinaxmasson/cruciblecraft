package com.masson.cruciblecraft.logistics.pipe.cover;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class CoverDefinitionCatalogTest {
    @Test
    void catalogHasExactDefinitionsAndBehaviors() {
        CoverBehaviorRegistry.validateDefinitions();
        assertEquals(15, CoverDefinitionCatalog.definitions().size());
        assertEquals(12, CoverBehaviorRegistry.registeredIds().size());
    }

    @Test
    void registryOperationsShareOneSynchronizedDeterministicBoundary()
            throws Exception {
        assertTrue(Modifier.isSynchronized(
                CoverBehaviorRegistry.class.getMethod(
                        "register",
                        ResourceLocation.class,
                        CoverBehavior.class).getModifiers()));
        assertTrue(Modifier.isSynchronized(
                CoverBehaviorRegistry.class.getMethod(
                        "resolve",
                        CoverDefinition.class).getModifiers()));
        assertTrue(Modifier.isSynchronized(
                CoverBehaviorRegistry.class.getMethod(
                        "registeredIds").getModifiers()));
        assertTrue(Modifier.isSynchronized(
                CoverBehaviorRegistry.class.getMethod(
                        "validateDefinitions").getModifiers()));
        CoverBehaviorRegistry.validateDefinitions();
        List<String> ids = CoverBehaviorRegistry.registeredIds().stream()
                .map(ResourceLocation::toString)
                .toList();
        assertEquals(
                List.of(
                        "cruciblecraft:filter",
                        "cruciblecraft:shutter",
                        "cruciblecraft:pump_adapter",
                        "cruciblecraft:conveyor",
                        "cruciblecraft:retriever_item",
                        "cruciblecraft:robot_arm",
                        "cruciblecraft:pressure_valve",
                        "cruciblecraft:selector_manual"),
                ids.subList(0, 8));
        assertEquals(12, ids.size());
        assertTrue(ids.containsAll(List.of(
                "cruciblecraft:logistics_item_storage",
                "cruciblecraft:logistics_item_transfer",
                "cruciblecraft:logistics_fluid_storage",
                "cruciblecraft:logistics_fluid_transfer")));
    }

    @Test
    void secondConveyorDefinitionChangesOnlyJsonValues() {
        CoverDefinition standard =
                CoverDefinitionCatalog.require("cruciblecraft:conveyor");
        CoverDefinition fast =
                CoverDefinitionCatalog.require("cruciblecraft:conveyor_fast");
        assertEquals(standard.behaviorId(), fast.behaviorId());
        assertSame(
                CoverBehaviorRegistry.resolve(standard),
                CoverBehaviorRegistry.resolve(fast));
        assertEquals(16, standard.values().rate());
        assertEquals(32, fast.values().rate());
    }

    @Test
    void everyDefinitionValueHasAHardRuntimeCeiling() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CoverDefinition(
                        ResourceLocation.parse("cruciblecraft:too_fast"),
                        ResourceLocation.parse("cruciblecraft:conveyor"),
                        CoverDefinition.Medium.ITEM,
                        new CoverDefinition.Values(
                                CoverDefinition.MAX_ITEM_RATE + 1,
                                0,
                                0,
                                CoverDefinition.TransferMode.UP_TO,
                                0),
                        Set.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new PipeCoverConfig(
                        Optional.of("minecraft:" + "x".repeat(128)),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()));
    }

    @Test
    void activePluginsUseDefinitionRateAndExactMode() {
        Probe probe = new Probe();
        PipeCover conveyor = PipeCover.of("cruciblecraft:conveyor");
        conveyor.behavior().tick(
                conveyor,
                conveyor.definition().orElseThrow(),
                probe);
        assertEquals(16, probe.amount);
        assertEquals(CoverDefinition.TransferMode.UP_TO, probe.mode);

        PipeCover robot = PipeCover.of("cruciblecraft:robot_arm");
        robot.behavior().tick(
                robot,
                robot.definition().orElseThrow(),
                probe);
        assertEquals(8, probe.amount);
        assertEquals(CoverDefinition.TransferMode.EXACT, probe.mode);
    }

    @Test
    void pressureAndManualConfigurationStayBounded() {
        PipeCoverSet covers = new PipeCoverSet();
        assertTrue(covers.set(
                Direction.NORTH,
                PipeCover.of("cruciblecraft:pressure_valve")));
        assertTrue(covers.allowsIncoming(
                Direction.NORTH,
                CoverDefinition.Medium.FLUID,
                499,
                1_000));
        assertFalse(covers.allowsIncoming(
                Direction.NORTH,
                CoverDefinition.Medium.FLUID,
                500,
                1_000));

        assertTrue(covers.set(
                Direction.SOUTH,
                PipeCover.of("cruciblecraft:selector_manual")));
        assertTrue(covers.configure(
                Direction.SOUTH,
                CoverDefinition.ConfigField.SELECTOR,
                CoverDefinition.MAX_SELECTOR));
        assertEquals(
                CoverDefinition.MAX_SELECTOR,
                covers.get(Direction.SOUTH).orElseThrow()
                        .definition().orElseThrow()
                        .resolve(covers.get(Direction.SOUTH).orElseThrow()
                                .config())
                        .selector());
        assertThrows(
                IllegalArgumentException.class,
                () -> covers.configure(
                        Direction.SOUTH,
                        CoverDefinition.ConfigField.SELECTOR,
                        CoverDefinition.MAX_SELECTOR + 1));
    }

    private static final class Probe
            implements CoverBehavior.TransferContext {
        private int amount;
        private CoverDefinition.TransferMode mode;

        @Override
        public CoverDefinition.Medium medium() {
            return CoverDefinition.Medium.ITEM;
        }

        @Override
        public Direction side() {
            return Direction.NORTH;
        }

        @Override
        public int storedAmount() {
            return 0;
        }

        @Override
        public int capacity() {
            return 64;
        }

        @Override
        public int transferItems(
                int request,
                Optional<String> matchId,
                CoverDefinition.TransferMode requestedMode) {
            amount = request;
            mode = requestedMode;
            return request;
        }

        @Override
        public int transferFluids(
                int request,
                Optional<String> matchId,
                CoverDefinition.TransferMode requestedMode) {
            return 0;
        }
    }
}
