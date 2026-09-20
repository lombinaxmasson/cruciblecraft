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

import com.masson.cruciblecraft.logistics.machinecover.MachineCoverKinds;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class CoverDefinitionCatalogTest {
    @Test
    void catalogHasExactDefinitionsAndBehaviors() {
        CoverBehaviorRegistry.validateDefinitions();
        assertEquals(
                27 + MachineCoverKinds.DEFINITION_COUNT,
                CoverDefinitionCatalog.definitions().size()
                        - CoverComponentTiers.definitionIds().size());
        assertEquals(
                27
                        + MachineCoverKinds.DEFINITION_COUNT
                        + CoverComponentTiers.definitionIds().size(),
                CoverDefinitionCatalog.definitions().size());
        assertEquals(44, CoverBehaviorRegistry.registeredIds().size());
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
        assertEquals(44, ids.size());
        assertTrue(ids.containsAll(List.of(
                "cruciblecraft:logistics_item_storage",
                "cruciblecraft:logistics_item_transfer",
                "cruciblecraft:logistics_fluid_storage",
                "cruciblecraft:logistics_fluid_transfer",
                "cruciblecraft:logistics_generic_storage",
                "cruciblecraft:logistics_generic_transfer",
                "cruciblecraft:logistics_generic_dump",
                "cruciblecraft:logistics_display_cpu",
                "cruciblecraft:controller_auto",
                "cruciblecraft:controller_auto_timer",
                "cruciblecraft:detector_running",
                "cruciblecraft:selector_tag",
                "cruciblecraft:cover_plate")));
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
    void compactElectricSidecarOwnsAllTenVoltageTiers() {
        assertEquals(30, CoverComponentTiers.definitionIds().size());
        for (CoverComponentTiers.Entry entry : CoverComponentTiers.entries()) {
            CoverDefinition definition =
                    CoverDefinitionCatalog.require(entry.definitionId());
            assertEquals(entry.family().behaviorId(), definition.behaviorId());
            assertEquals(entry.family().medium(), definition.medium());
            assertEquals(entry.rate(), definition.values().rate());
            assertEquals(entry.interval(), definition.values().interval());
        }
        CoverDefinition hvPump = CoverDefinitionCatalog.require(
                "cruciblecraft:pump_hv");
        assertEquals(16_000, hvPump.values().rate());
        assertEquals(20, hvPump.values().interval());
        CoverDefinition ulvConveyor = CoverDefinitionCatalog.require(
                "cruciblecraft:conveyor_ulv");
        assertEquals(512, ulvConveyor.values().interval());
        assertEquals(64, ulvConveyor.values().rate());
    }

    @Test
    void retrieverInvertIsGt6FilterXorNotDisplayCpu() {
        assertTrue(CoverItemFilters.matches(
                Optional.empty(), true, "minecraft:iron_ingot"));
        assertTrue(CoverItemFilters.matches(
                Optional.of("minecraft:iron_ingot"),
                false,
                "minecraft:iron_ingot"));
        assertFalse(CoverItemFilters.matches(
                Optional.of("minecraft:iron_ingot"),
                true,
                "minecraft:iron_ingot"));
        assertTrue(CoverItemFilters.matches(
                Optional.of("minecraft:iron_ingot"),
                true,
                "minecraft:gold_ingot"));
        PipeCover retriever = PipeCover.of("cruciblecraft:retriever_item")
                .withConfig(PipeCoverConfig.EMPTY
                        .withMatchId("minecraft:iron_ingot")
                        .withInvert(1));
        assertTrue(CoverItemFilters.inverted(retriever.config()));
        assertFalse(CoverItemFilters.matches(
                retriever.config().matchId(),
                CoverItemFilters.inverted(retriever.config()),
                "minecraft:iron_ingot"));
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
                        Optional.empty(),
                        Optional.empty(),
                        0,
                        0));
        assertEquals(2047, PipeCoverConfig.EMPTY.withDisplay(2047, 0).visual());
        assertThrows(
                IllegalArgumentException.class,
                () -> PipeCoverConfig.EMPTY.withDisplay(
                        PipeCoverConfig.MAX_VISUAL + 1, 0));
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
