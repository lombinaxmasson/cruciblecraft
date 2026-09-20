package com.masson.cruciblecraft.machine.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.content.mold.MoldHost;
import com.masson.cruciblecraft.fluid.CrucibleTransferCoordinator.InsertResult;
import com.masson.cruciblecraft.heat.CrucibleThermalModel;
import com.masson.cruciblecraft.machine.MachineMaterialRules;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.LoadingModList;

class CrucibleProcessCoreTest {
    @BeforeAll
    static void bootstrapCatalog(@TempDir Path configDirectory) {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        if (!MaterialCatalog.isBootstrapped()) {
            MaterialCatalog.bootstrap(configDirectory);
        }
    }

    @Test
    void singleBlockCapacityStaysSixteenIngots() {
        CrucibleProcessCore core = CrucibleProcessCore.singleBlock();
        assertEquals(CrucibleProcessCore.SINGLE_BLOCK_MAX_INGOTS, core.maxIngots());
        assertEquals(
                MaterialPrefixes.INGOT.units() * 16,
                core.maxUnits());
        assertEquals(InsertResult.SUCCESS, fillIngots(core, 16));
        assertEquals(InsertResult.FULL, fillIngots(core, 1));
        assertEquals(core.maxUnits(), core.totalUnits());
    }

    @Test
    void largeCapacityAccepts432AndRejects433() {
        CrucibleProcessCore core = CrucibleProcessCore.large();
        assertEquals(432, core.maxIngots());
        assertEquals(
                MaterialPrefixes.INGOT.units() * 432,
                core.maxUnits());
        assertEquals(InsertResult.SUCCESS, fillIngots(core, 432));
        assertEquals(InsertResult.FULL, fillIngots(core, 1));
        assertEquals(core.maxUnits(), core.totalUnits());
    }

    @Test
    void freezeStopsInsertionWithoutClearingContents() {
        CrucibleProcessCore core = CrucibleProcessCore.large();
        assertEquals(InsertResult.SUCCESS, fillIngots(core, 4));
        int units = core.totalUnits();
        core.setFrozen(true);
        assertEquals(InsertResult.INVALID_MATERIAL, fillIngots(core, 1));
        assertEquals(units, core.totalUnits());
        assertTrue(core.cast(MaterialPrefixes.INGOT).isEmpty());
    }

    @Test
    void singleBlockAndLargeDoNotShareCapacity() {
        assertNotEquals(
                CrucibleProcessCore.singleBlock().maxUnits(),
                CrucibleProcessCore.large().maxUnits());
    }

    @Test
    void heatResistanceMatchesGt6SmallAndLargeBonuses() {
        CrucibleProcessCore small = CrucibleProcessCore.singleBlock();
        CrucibleProcessCore large = CrucibleProcessCore.large();
        assertEquals(1.25, small.heatResistanceBonus(), 0.0001);
        assertEquals(1.10, large.heatResistanceBonus(), 0.0001);
        double ceramic = MaterialCatalog.require("ceramic").thermal().meltingPoint();
        assertEquals(
                MachineMaterialRules.maxTemperature(ceramic, 1.25),
                small.casingMaxTemperature(),
                0.001f);
        assertEquals(
                MachineMaterialRules.maxTemperature(ceramic, 1.10),
                large.casingMaxTemperature(),
                0.001f);
    }

    @Test
    void alloyTickConvertsLeftoverBronzeCharge() {
        CrucibleProcessCore core = CrucibleProcessCore.singleBlock();
        int ingot = MaterialPrefixes.INGOT.units();
        assertEquals(InsertResult.SUCCESS, fill(core, "copper", 5));
        assertEquals(InsertResult.SUCCESS, fill(core, "tin", 1));
        float melt = Math.max(
                (float) MaterialCatalog.require("copper").thermal().meltingPoint(),
                (float) MaterialCatalog.require("bronze").thermal().meltingPoint());
        core.thermal().restore(
                melt + 50.0F,
                0L,
                0L,
                CrucibleThermalModel.HOT_BUFFER_TICKS,
                false);
        CrucibleProcessCore.TickOutcome outcome = core.advance(0L, true);
        assertTrue(!outcome.destroysHost());
        assertEquals(4 * ingot, core.contents().units("bronze"));
        assertEquals(2 * ingot, core.contents().units("copper"));
        assertEquals(0, core.contents().units("tin"));
    }

    @Test
    void alloyTickConvertsNoDecomposeRedAlloy() {
        CrucibleProcessCore core = CrucibleProcessCore.singleBlock();
        int ingot = MaterialPrefixes.INGOT.units();
        core.contents().replace(Map.of("copper", ingot, "redstone", 4 * ingot));
        heatAbove(core, "red_alloy");
        CrucibleProcessCore.TickOutcome outcome = core.advance(0L, true);
        assertTrue(!outcome.destroysHost());
        assertEquals(ingot, core.contents().units("red_alloy"));
        assertEquals(0, core.contents().units("copper"));
        assertEquals(0, core.contents().units("redstone"));
    }

    @Test
    void alloyTickConvertsFirstLevelBlackBronze() {
        CrucibleProcessCore core = CrucibleProcessCore.singleBlock();
        int ingot = MaterialPrefixes.INGOT.units();
        core.contents().replace(Map.of("copper", 3 * ingot, "electrum", 2 * ingot));
        heatAbove(core, "black_bronze");
        CrucibleProcessCore.TickOutcome outcome = core.advance(0L, true);
        assertTrue(!outcome.destroysHost());
        assertEquals(5 * ingot, core.contents().units("black_bronze"));
        assertEquals(0, core.contents().units("copper"));
        assertEquals(0, core.contents().units("electrum"));
    }

    @Test
    void alloyTickUsesAnnealedCopperBronzeExtra() {
        CrucibleProcessCore core = CrucibleProcessCore.singleBlock();
        int ingot = MaterialPrefixes.INGOT.units();
        core.contents().replace(Map.of("annealed_copper", 3 * ingot, "tin", ingot));
        heatAbove(core, "bronze");
        CrucibleProcessCore.TickOutcome outcome = core.advance(0L, true);
        assertTrue(!outcome.destroysHost());
        assertEquals(4 * ingot, core.contents().units("bronze"));
        assertEquals(0, core.contents().units("annealed_copper"));
        assertEquals(0, core.contents().units("tin"));
    }

    @Test
    void fillMoldAtSidePoursFirstMoltenIdentityMelt() {
        CrucibleProcessCore core = CrucibleProcessCore.singleBlock();
        int ingot = MaterialPrefixes.INGOT.units();
        assertEquals(InsertResult.SUCCESS, fill(core, "copper", 2));
        core.thermal().restore(
                (float) MaterialCatalog.require("copper").thermal().meltingPoint()
                        + 50.0F,
                0L,
                0L,
                CrucibleThermalModel.HOT_BUFFER_TICKS,
                false);
        RecordingMold mold = new RecordingMold(ingot);
        assertTrue(core.fillMoldAtSide(mold, Direction.WEST));
        assertEquals("copper", mold.materialId);
        assertEquals(ingot, mold.consumed);
        assertEquals(ingot, core.totalUnits());
    }

    @Test
    void takeScrapRemovesTheLightestSolid() {
        CrucibleProcessCore core = CrucibleProcessCore.singleBlock();
        assertEquals(InsertResult.SUCCESS, fill(core, "iron", 1));
        Optional<CrucibleProcessCore.ScrapTake> scrap = core.takeScrap(1);
        assertTrue(scrap.isPresent());
        assertEquals("iron", scrap.get().material().id());
        assertTrue(scrap.get().count() > 0 || scrap.get().discardedRemainder());
    }

    @Test
    void takeScrapGivesOnePieceWhenScrapFormIsLive() {
        CrucibleProcessCore core = CrucibleProcessCore.singleBlock();
        assertEquals(InsertResult.SUCCESS, fill(core, "iron", 1));
        assertTrue(MaterialCatalog.isFormRegistered(
                MaterialCatalog.require("iron"),
                MaterialPrefixCatalog.require("scrap")));
        int before = core.totalUnits();
        Optional<CrucibleProcessCore.ScrapTake> scrap = core.takeScrap(1);
        assertTrue(scrap.isPresent());
        assertFalse(scrap.get().discardedRemainder());
        assertEquals(1, scrap.get().count());
        assertEquals(before - scrap.get().unitsRemoved(), core.totalUnits());
    }

    @Test
    void takeScrapDiscardsRemainderSmallerThanOneScrap() {
        CrucibleProcessCore core = CrucibleProcessCore.singleBlock();
        int scrapUnits = MaterialPrefixCatalog.require("scrap").units();
        core.contents().replace(Map.of("iron", scrapUnits - 1));
        Optional<CrucibleProcessCore.ScrapTake> scrap = core.takeScrap(1);
        assertTrue(scrap.isPresent());
        assertTrue(scrap.get().discardedRemainder());
        assertEquals(0, scrap.get().count());
        assertEquals(scrapUnits - 1, scrap.get().unitsRemoved());
        assertEquals(0, core.totalUnits());
    }

    @Test
    void acceptMoldPourRetriesOneIngotWhenTheWholeStackDoesNotFit() {
        CrucibleProcessCore dest = CrucibleProcessCore.singleBlock();
        int ingot = MaterialPrefixes.INGOT.units();
        assertEquals(InsertResult.SUCCESS, fill(dest, "copper", 15));
        assertEquals(
                ingot,
                dest.acceptMoldPour("copper", 2 * ingot, 20.0F));
        assertEquals(16 * ingot, dest.totalUnits());
    }

    @Test
    void fillMoldAtSidePoursIntoAnotherCrucibleMoldHost() {
        CrucibleProcessCore source = CrucibleProcessCore.singleBlock();
        CrucibleProcessCore dest = CrucibleProcessCore.singleBlock();
        int ingot = MaterialPrefixes.INGOT.units();
        assertEquals(InsertResult.SUCCESS, fill(source, "copper", 2));
        source.thermal().restore(
                (float) MaterialCatalog.require("copper").thermal().meltingPoint()
                        + 50.0F,
                0L,
                0L,
                CrucibleThermalModel.HOT_BUFFER_TICKS,
                false);
        MoldHost pot = new CrucibleMold(dest);
        assertTrue(source.fillMoldAtSide(pot, Direction.UP));
        assertEquals(2 * ingot, dest.totalUnits());
        assertEquals(0, source.totalUnits());
        assertEquals(2 * ingot, dest.contents().units("copper"));
    }

    @Test
    void rainAddsWaterOnTheGt6Cadence() {
        CrucibleProcessCore core = CrucibleProcessCore.singleBlock();
        assertTrue(MaterialCatalog.contains("water"));
        assertTrue(core.addRainWater(10L, 1.0F, false));
        assertTrue(core.contents().units("water") > 0);
        assertTrue(!core.addRainWater(11L, 1.0F, false));
    }

    @Test
    void ambientIngotCoolsAHotCrucible() {
        CrucibleProcessCore core = CrucibleProcessCore.singleBlock();
        core.thermal().restore(
                1_000.0F,
                0L,
                0L,
                CrucibleThermalModel.HOT_BUFFER_TICKS,
                false);
        float before = core.authoritativeTemperature();
        assertEquals(InsertResult.SUCCESS, fill(core, "iron", 1));
        assertTrue(
                core.authoritativeTemperature() < before - 300.0F,
                "cold ingot mix should drop a hot smeltery by hundreds of degrees");
    }

    @Test
    void fullerChargeHeatsSlowerUnderTheSameHu() {
        CrucibleProcessCore empty = CrucibleProcessCore.singleBlock();
        CrucibleProcessCore full = CrucibleProcessCore.singleBlock();
        assertEquals(InsertResult.SUCCESS, fill(full, "iron", 16));
        empty.thermal().restore(
                20.0F, 0L, 0L, CrucibleThermalModel.HOT_BUFFER_TICKS, false);
        full.thermal().restore(
                20.0F, 0L, 0L, CrucibleThermalModel.HOT_BUFFER_TICKS, false);
        for (int tick = 0; tick < 200; tick++) {
            empty.advance(16L, true);
            full.advance(16L, true);
        }
        assertTrue(
                full.authoritativeTemperature()
                        < empty.authoritativeTemperature() - 50.0F,
                "16 ingots must raise HU-per-kelvin the way GT6 weight does");
    }

    private static InsertResult fillIngots(CrucibleProcessCore core, int count) {
        return fill(core, "iron", count);
    }

    private static void heatAbove(CrucibleProcessCore core, String resultId) {
        core.thermal().restore(
                (float) MaterialCatalog.require(resultId).thermal().meltingPoint()
                        + 50.0F,
                0L,
                0L,
                CrucibleThermalModel.HOT_BUFFER_TICKS,
                false);
    }

    private static InsertResult fill(CrucibleProcessCore core, String materialId, int count) {
        return core.insert(
                new MaterialUnits.Entry(
                        materialId,
                        MaterialPrefixes.INGOT,
                        Math.multiplyExact(MaterialPrefixes.INGOT.units(), count)),
                CrucibleProcessCore.AMBIENT_TEMPERATURE);
    }

    private static final class CrucibleMold implements MoldHost {
        private final CrucibleProcessCore process;

        private CrucibleMold(CrucibleProcessCore process) {
            this.process = process;
        }

        @Override
        public boolean isMoldInputSide(Direction side) {
            return side == Direction.UP;
        }

        @Override
        public float moldMaxTemperatureCelsius() {
            return process.casing().maxTemperature();
        }

        @Override
        public int moldRequiredMaterialUnits() {
            return 1;
        }

        @Override
        public int fillMold(
                String materialId, int availableUnits, float temperature, Direction side) {
            if (!isMoldInputSide(side)) {
                return 0;
            }
            return process.acceptMoldPour(materialId, availableUnits, temperature);
        }

        @Override
        public ItemStack takeOutput(Player player, boolean causeDamage) {
            return ItemStack.EMPTY;
        }
    }

    private static final class RecordingMold implements MoldHost {
        private final int required;
        private String materialId = "";
        private int consumed;

        private RecordingMold(int required) {
            this.required = required;
        }

        @Override
        public boolean isMoldInputSide(Direction side) {
            return true;
        }

        @Override
        public float moldMaxTemperatureCelsius() {
            return 10_000.0F;
        }

        @Override
        public int moldRequiredMaterialUnits() {
            return required;
        }

        @Override
        public int fillMold(
                String materialId, int availableUnits, float temperature, Direction side) {
            if (availableUnits < required) {
                return 0;
            }
            this.materialId = materialId;
            this.consumed = required;
            return required;
        }

        @Override
        public ItemStack takeOutput(Player player, boolean causeDamage) {
            return ItemStack.EMPTY;
        }
    }
}
