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
                "钢墙",
                MteInPlaceDisplayNames.chinese("steel/wall").orElseThrow());
        assertEquals(
                "不锈钢墙",
                MteInPlaceDisplayNames.chinese("stainless_steel/wall").orElseThrow());
        assertEquals(
                "镀锌钢墙",
                MteInPlaceDisplayNames.chinese("multiblock/galvanized_steel_wall")
                        .orElseThrow());
        assertEquals(
                "致密不锈钢墙",
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
}
