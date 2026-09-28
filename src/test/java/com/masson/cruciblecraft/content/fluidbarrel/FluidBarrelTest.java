package com.masson.cruciblecraft.content.fluidbarrel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

class FluidBarrelTest {
    @BeforeAll
    static void bootstrap() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void catalogKeepsThirtySixSourceIdentities() {
        List<FluidBarrelProfile> profiles = FluidBarrelCatalog.profiles();
        assertEquals(36, profiles.size());
        assertEquals(36, new HashSet<>(profiles.stream().map(FluidBarrelProfile::meta).toList()).size());
        FluidBarrelProfile cheapLead = FluidBarrelCatalog.byMeta(32733);
        assertEquals("fluid_barrel/cheap_lead", cheapLead.path());
        assertEquals(FluidBarrelKind.WOOD, cheapLead.kind());
        assertTrue(cheapLead.onlySimple());
        // gt6-source: Loader_MultiTileEntities.java:2136 NBT_CAPACITY_HU 340
        assertEquals(340L, cheapLead.meltingPoint());
        assertEquals("explicitly_blocked", cheapLead.recipeStatus());

        FluidBarrelProfile bronze = FluidBarrelCatalog.byMeta(32102);
        // gt6-source: TileEntityBase08Barrel.java:66 melting * 1.25; MT.java:418 copper 1357
        assertEquals(1696L, bronze.meltingPoint());
        assertTrue(bronze.gasProof());
        assertFalse(bronze.onlySimple());

        FluidBarrelProfile logistics = FluidBarrelCatalog.byMeta(32072);
        assertEquals(FluidBarrelKind.LOGISTICS, logistics.kind());
        assertFalse(logistics.canSeal());
        assertTrue(logistics.keepsFilter());
        assertEquals(1_000_000L, logistics.capacity());

        FluidBarrelProfile infinity = FluidBarrelCatalog.byMeta(32067);
        assertEquals(10_000_000_000L, infinity.capacity());
        assertEquals(1_000_000_000L, infinity.meltingPoint());

        assertEquals(
                "source_exact",
                FluidBarrelCatalog.byMeta(32742).recipeStatus());
        assertEquals(
                "source_exact",
                FluidBarrelCatalog.byMeta(32102).recipeStatus());
        assertEquals(20, profiles.stream()
                .filter(profile -> profile.kind() == FluidBarrelKind.METAL)
                .filter(profile -> "source_exact".equals(profile.recipeStatus()))
                .count());
        assertEquals(
                "source_exact",
                FluidBarrelCatalog.byMeta(32072).recipeStatus());
        assertTrue(FluidBarrelFluids.simpleNames().contains("water"));
        assertTrue(FluidBarrelFluids.simpleNames().contains("lava"));
        assertTrue(FluidBarrelFluids.simpleNames().contains("creosote"));
    }

    @Test
    void modeBitsMatchSoftHammerWrenchAndPlunger() {
        FluidBarrelLogic.Mode filled = new FluidBarrelLogic.Mode(true, false, 40L);
        FluidBarrelLogic.Mode sealed = FluidBarrelLogic.softHammer(filled, 1000L, true);
        assertTrue(sealed.sealed());
        assertTrue(sealed.autoOutput());
        assertEquals(0L, sealed.sealedTime());

        FluidBarrelLogic.Mode emptied = FluidBarrelLogic.softHammer(sealed, 0L, true);
        assertTrue(emptied.sealed());
        assertFalse(emptied.autoOutput());

        FluidBarrelLogic.Mode logistics = FluidBarrelLogic.softHammer(filled, 1000L, false);
        assertFalse(logistics.sealed());
        assertEquals(40L, logistics.sealedTime());

        FluidBarrelLogic.Mode output = FluidBarrelLogic.wrench(filled);
        assertFalse(output.autoOutput());
        assertEquals(40L, output.sealedTime());
        assertEquals(0L, FluidBarrelLogic.plunger(sealed).sealedTime());
        assertTrue(FluidBarrelLogic.plunger(sealed).sealed());
    }

    @Test
    void outputSidesAndHazardsFollowBarrelOrder() {
        assertEquals(
                List.of(Direction.DOWN, Direction.UP),
                FluidBarrelLogic.autoOutputSides(true, true));
        assertEquals(
                List.of(Direction.UP),
                FluidBarrelLogic.autoOutputSides(false, true));
        assertEquals(
                List.of(Direction.DOWN),
                FluidBarrelLogic.autoOutputSides(false, false));

        assertEquals(
                FluidBarrelLogic.Reaction.MELTDOWN_LAVA,
                react(1000L, 2000L, true, false, false, false, false, true, false, false));
        assertEquals(
                FluidBarrelLogic.Reaction.MELTDOWN_FIRE,
                react(1000L, 2000L, false, false, false, false, false, true, false, false));
        assertEquals(
                FluidBarrelLogic.Reaction.MAGIC_DESTROY,
                react(1000L, 300L, false, true, false, false, false, true, false, false));
        assertEquals(
                FluidBarrelLogic.Reaction.ACID_DESTROY,
                react(1000L, 300L, false, false, true, false, false, true, false, false));
        assertEquals(
                FluidBarrelLogic.Reaction.TRASH,
                react(1000L, 300L, false, false, false, true, false, true, false, false));
        assertEquals(
                FluidBarrelLogic.Reaction.TRASH,
                react(1000L, 300L, false, false, false, true, true, false, false, false));
        assertEquals(
                FluidBarrelLogic.Reaction.FERMENT,
                react(1000L, 300L, false, false, false, false, false, true, true, true));
        assertEquals(
                FluidBarrelLogic.Reaction.AUTO_OUTPUT,
                react(1000L, 300L, false, false, false, false, false, true, false, true));
        assertFalse(FluidBarrelLogic.allow(
                true, 300L, 1000L, false, true));
        assertTrue(FluidBarrelLogic.allow(
                false, 300L, 1000L, true, true));
        assertFalse(FluidBarrelLogic.allow(
                false, 300L, 1000L, true, false));
    }

    @Test
    void sealedMathScalesWithTankAmountAndSaturates() {
        // gt6-source: TileEntityBase08Barrel.java:192 UT.Code.divup(power * tank, input)
        assertEquals(12_800L, FluidBarrelLogic.sealedDuration(8000L, 16L, 100, 1000L));
        // gt6-source: TileEntityBase08Barrel.java:202 FL.mul(output, tank, input, false)
        assertEquals(8000L, FluidBarrelLogic.scaledOutput(1000L, 8000L, 1000L));
        // gt6-source: TileEntityBase08Barrel.java:202 FL.mul(output, tank, input, false)
        assertEquals(25L, FluidBarrelLogic.scaledOutput(250L, 100L, 1000L));
        assertEquals(
                Long.MAX_VALUE,
                FluidBarrelLogic.sealedDuration(
                        10_000_000_000L, Long.MAX_VALUE, Integer.MAX_VALUE, 1L));
    }

    @Test
    void longTankKeepsFilterAndHonorsSeal() {
        boolean[] locked = {false};
        FluidBarrelTank tank = new FluidBarrelTank(
                10_000_000_000L,
                true,
                () -> locked[0],
                () -> {});
        assertEquals(
                5_000_000_000L,
                tank.fillAmount(
                        new FluidStack(Fluids.WATER, 1),
                        5_000_000_000L,
                        IFluidHandler.FluidAction.EXECUTE));
        assertEquals(Integer.MAX_VALUE, tank.getFluidAmount());
        assertEquals(5_000_000_000L, tank.amount());

        tank.removeAmount(5_000_000_000L, IFluidHandler.FluidAction.EXECUTE);
        assertEquals(0L, tank.amount());
        assertTrue(tank.hasType());
        assertEquals(
                0L,
                tank.fillAmount(
                        new FluidStack(Fluids.LAVA, 1),
                        1000L,
                        IFluidHandler.FluidAction.EXECUTE));
        assertEquals(
                1000L,
                tank.fillAmount(
                        new FluidStack(Fluids.WATER, 1000),
                        1000L,
                        IFluidHandler.FluidAction.EXECUTE));

        locked[0] = true;
        assertEquals(
                0,
                tank.fill(
                        new FluidStack(Fluids.WATER, 100),
                        IFluidHandler.FluidAction.EXECUTE));
        assertTrue(tank.drain(
                100, IFluidHandler.FluidAction.EXECUTE).isEmpty());
        long beforeRemoval = tank.amount();
        long removed = 100L;
        assertEquals(
                removed,
                tank.removeAmount(removed, IFluidHandler.FluidAction.EXECUTE).amount());
        assertEquals(beforeRemoval - removed, tank.amount());

        FluidBarrelTank open = new FluidBarrelTank(
                8000L, false, () -> false, () -> {});
        open.setContents(Fluids.WATER, 50L);
        open.removeAmount(50L, IFluidHandler.FluidAction.EXECUTE);
        assertFalse(open.hasType());
    }

    private static FluidBarrelLogic.Reaction react(
            long amount,
            long temperature,
            boolean lava,
            boolean magic,
            boolean acid,
            boolean plasma,
            boolean plasmaProof,
            boolean allow,
            boolean sealed,
            boolean autoOutput) {
        return FluidBarrelLogic.reaction(
                amount,
                temperature,
                1000L,
                lava,
                magic,
                false,
                acid,
                false,
                plasma,
                plasmaProof,
                false,
                true,
                allow,
                sealed,
                autoOutput);
    }
}
