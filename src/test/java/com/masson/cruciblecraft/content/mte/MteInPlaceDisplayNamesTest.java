package com.masson.cruciblecraft.content.mte;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MteInPlaceDisplayNamesTest {
    @Test
    void largeCrucibleAndWallsComposeChinese() {
        assertEquals(
                "大型钢坩埚",
                MteInPlaceDisplayNames.chinese("multiblock/large_steel_crucible")
                        .orElseThrow());
        assertEquals(
                "大型不锈钢坩埚",
                MteInPlaceDisplayNames.chinese(
                                "multiblock/large_stainless_steel_crucible")
                        .orElseThrow());
        assertEquals(
                "大型钨钢坩埚",
                MteInPlaceDisplayNames.chinese(
                                "multiblock/large_tungstensteel_crucible")
                        .orElseThrow());
        assertEquals(
                "钢壁",
                MteInPlaceDisplayNames.chinese("steel/wall").orElseThrow());
        assertEquals(
                "不锈钢壁",
                MteInPlaceDisplayNames.chinese("stainless_steel/wall").orElseThrow());
        assertEquals(
                "镀锌钢壁",
                MteInPlaceDisplayNames.chinese("multiblock/galvanized_steel_wall")
                        .orElseThrow());
        assertEquals(
                "致密不锈钢壁",
                MteInPlaceDisplayNames.chinese(
                                "multiblock/dense_stainless_steel_wall")
                        .orElseThrow());
    }

    @Test
    void foundryMoldsKeepCompoundMaterial() {
        assertEquals(
                "模具（不锈钢）",
                MteInPlaceDisplayNames.chinese("foundry/mold_stainless_steel")
                        .orElseThrow());
        assertEquals(
                "坩埚（殷钢）",
                MteInPlaceDisplayNames.chinese("foundry/smelting_crucible_invar")
                        .orElseThrow());
        assertEquals(
                "Crucible (Steel)",
                MteInPlaceDisplayNames.english(
                        "Smelting Crucible (Steel)",
                        "foundry/smelting_crucible_steel"));
    }

    @Test
    void fluidAttachmentsComposeChineseFromMaterialAndKind() {
        assertEquals(
                "石制浇铸口",
                MteInPlaceDisplayNames.chinese(
                                "fluid_attachment/crucible_faucet_stone")
                        .orElseThrow());
        assertEquals(
                "陶瓷浇铸口",
                MteInPlaceDisplayNames.chinese(
                                "fluid_attachment/crucible_faucet_ceramic")
                        .orElseThrow());
        assertEquals(
                "不锈钢浇铸口",
                MteInPlaceDisplayNames.chinese(
                                "stainless_steel/crucible_faucet")
                        .orElseThrow());
        assertEquals(
                "塑料有盖喷嘴",
                MteInPlaceDisplayNames.chinese(
                                "fluid_attachment/plastic_cap_nozzle")
                        .orElseThrow());
        assertEquals(
                "碳化钽铪漏斗",
                MteInPlaceDisplayNames.chinese(
                                "tantalum_hafnium_carbide/funnel")
                        .orElseThrow());
        assertEquals(
                "不锈钢龙头",
                MteInPlaceDisplayNames.chinese(
                                "fluid_attachment/stainless_tap")
                        .orElseThrow());
    }

    @Test
    void englishWithoutRegistryPathKeepsCatalogName() {
        assertEquals(
                "Black Granite",
                MteInPlaceDisplayNames.english("Black Granite", null));
        assertEquals("", MteInPlaceDisplayNames.english(null, null));
    }

    @Test
    void woodenPanelsKeepGt6Title() {
        assertEquals(
                "Wooden Panel",
                MteInPlaceDisplayNames.english("Wooden Panel", "panel/wood_0"));
    }
}
