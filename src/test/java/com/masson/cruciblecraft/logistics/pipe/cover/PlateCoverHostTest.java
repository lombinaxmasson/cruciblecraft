package com.masson.cruciblecraft.logistics.pipe.cover;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.energy.cable.CableCovers;

import net.minecraft.resources.ResourceLocation;

class PlateCoverHostTest {
    @Test
    void plateFamilyIsDecorativeAndIngotsAreNot() {
        assertTrue(PlateCovers.isCoverForm(MaterialPrefixes.PLATE));
        assertTrue(PlateCovers.isCoverForm(MaterialPrefixes.FOIL));
        assertTrue(PlateCovers.isCoverForm(MaterialPrefixes.DOUBLE_PLATE));
        assertTrue(PlateCovers.isCoverForm(MaterialPrefixes.DENSE_PLATE));
        assertTrue(PlateCovers.isCoverForm(MaterialPrefixes.PLATE_GEM));
        assertFalse(PlateCovers.isCoverForm(MaterialPrefixes.INGOT));
        assertFalse(PlateCovers.isCoverForm(MaterialPrefixes.TINY_PLATE));
        assertFalse(PlateCovers.isCoverForm(MaterialPrefixes.TINY_PLATE_GEM));
        assertTrue(PlateCovers.isPlate(PlateCovers.DEFINITION_ID));
        CoverDefinition plate = CoverDefinitionCatalog.require(
                "cruciblecraft:cover_plate");
        assertEquals("cover_plate", plate.behaviorId().getPath());
        assertTrue(plate.configurable().contains(
                CoverDefinition.ConfigField.MATCH_ID));
    }

    @Test
    void logisticsAndFilterInterceptLikeGt6() {
        PipeCover dump = PipeCover.of("cruciblecraft:logistics_generic_dump");
        PipeCover filter = PipeCover.of("cruciblecraft:filter");
        PipeCover valve = PipeCover.of("cruciblecraft:pressure_valve");
        PipeCover display = PipeCover.of(
                "cruciblecraft:logistics_display_cpu_logic");
        assertTrue(PipeCoverIntercept.interceptConnect(
                dump, PipeCoverIntercept.NeighborKind.ITEM_PIPE));
        assertFalse(PipeCoverIntercept.interceptConnect(
                filter, PipeCoverIntercept.NeighborKind.OTHER));
        assertTrue(PipeCoverIntercept.interceptConnect(
                filter,
                PipeCoverIntercept.NeighborKind.ITEM_PIPE,
                PipeCoverIntercept.NeighborKind.ITEM_PIPE));
        assertFalse(PipeCoverIntercept.interceptConnect(
                filter,
                PipeCoverIntercept.NeighborKind.ITEM_PIPE,
                PipeCoverIntercept.NeighborKind.OTHER));
        assertFalse(PipeCoverIntercept.interceptConnect(
                display, PipeCoverIntercept.NeighborKind.ITEM_PIPE));
        assertFalse(PipeCoverIntercept.allowsPlacement(
                filter,
                PipeCoverIntercept.NeighborKind.ITEM_PIPE,
                PipeCoverIntercept.NeighborKind.ITEM_PIPE,
                0));
        assertTrue(PipeCoverIntercept.allowsPlacement(
                filter,
                PipeCoverIntercept.NeighborKind.ITEM_PIPE,
                PipeCoverIntercept.NeighborKind.OTHER,
                0));
        assertFalse(PipeCoverIntercept.allowsPlacement(
                valve,
                PipeCoverIntercept.NeighborKind.FLUID_PIPE,
                PipeCoverIntercept.NeighborKind.FLUID_PIPE,
                1));
        assertFalse(PipeCoverIntercept.allowsPlacement(
                valve,
                PipeCoverIntercept.NeighborKind.FLUID_PIPE,
                PipeCoverIntercept.NeighborKind.OTHER,
                2));
        assertTrue(PipeCoverIntercept.allowsPlacement(
                valve,
                PipeCoverIntercept.NeighborKind.FLUID_PIPE,
                PipeCoverIntercept.NeighborKind.OTHER,
                1));
        assertTrue(PipeCoverIntercept.interceptConnect(
                valve, PipeCoverIntercept.NeighborKind.FLUID_PIPE));
        assertFalse(PipeCoverIntercept.interceptConnect(
                valve, PipeCoverIntercept.NeighborKind.OTHER));
    }

    @Test
    void euCableHostsBlankEmitterAndPlateNotTorchOrSelector() {
        assertTrue(CableCovers.canPlaceId(id("cover_blank")));
        assertTrue(CableCovers.canPlaceId(id("cover_plate")));
        assertTrue(CableCovers.canPlaceId(id("redstone_emitter")));
        assertTrue(CableCovers.canPlaceId(id("redstone_conductor_in")));
        assertTrue(CableCovers.canPlaceId(id("redstone_conductor_out")));
        assertTrue(CableCovers.canPlaceId(id("scale_progress")));
        assertTrue(CableCovers.canPlaceId(id("cover_crafting")));
        assertTrue(CableCovers.canPlaceId(id("cover_warning")));
        assertTrue(CableCovers.canPlaceId(id("cover_asphalt")));
        assertFalse(CableCovers.canPlaceId(id("redstone_torch")));
        assertFalse(CableCovers.canPlaceId(id("redstone_repeater")));
        assertFalse(CableCovers.canPlaceId(id("selector_redstone")));
        assertFalse(CableCovers.canPlaceId(id("controller_auto")));
        assertFalse(CableCovers.canPlaceId(id("scale_energy")));
    }

    @Test
    void itemAndFluidFiltersFollowGt6WhitelistBlacklist() {
        PipeCover item = PipeCover.of("cruciblecraft:filter");
        assertFalse(CoverFilterLogic.allowsId(item, "minecraft:stone"));
        PipeCover itemBlack = item.withConfig(item.config().withInvert(1));
        assertTrue(CoverFilterLogic.allowsId(itemBlack, "minecraft:stone"));
        PipeCover itemStone = item.withConfig(
                item.config().withMatchId("minecraft:stone"));
        assertTrue(CoverFilterLogic.allowsId(itemStone, "minecraft:stone"));
        assertFalse(CoverFilterLogic.allowsId(itemStone, "minecraft:dirt"));
        PipeCover itemStoneBlack = itemStone.withConfig(
                itemStone.config().withInvert(1));
        assertFalse(CoverFilterLogic.allowsId(itemStoneBlack, "minecraft:stone"));
        assertTrue(CoverFilterLogic.allowsId(itemStoneBlack, "minecraft:dirt"));

        PipeCover fluid = PipeCover.of("cruciblecraft:filter_fluid");
        assertFalse(CoverFilterLogic.allowsId(fluid, "minecraft:water"));
        PipeCover fluidWater = fluid.withConfig(
                fluid.config().withMatchId("minecraft:water"));
        assertTrue(CoverFilterLogic.allowsId(fluidWater, "minecraft:water"));
        assertFalse(CoverFilterLogic.allowsId(fluidWater, "minecraft:lava"));
        assertFalse(PipeCoverIntercept.interceptConnect(
                fluid, PipeCoverIntercept.NeighborKind.FLUID_PIPE));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
