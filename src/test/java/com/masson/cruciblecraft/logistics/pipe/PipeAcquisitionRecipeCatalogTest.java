package com.masson.cruciblecraft.logistics.pipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.logistics.pipe
        .PipeAcquisitionRecipeCatalog.Classification;
import com.masson.cruciblecraft.logistics.pipe
        .PipeAcquisitionRecipeCatalog.Operand;
import org.junit.jupiter.api.Test;

class PipeAcquisitionRecipeCatalogTest {
    @Test
    void catalogClosesExactlyFiveMaterialsByFiveGauges() {
        assertEquals(25, PipeAcquisitionRecipeCatalog.ALL.size());
        assertEquals(
                Set.of(
                        "wood",
                        "carbon",
                        "plastic",
                        "rubber",
                        "wood_treated"),
                PipeAcquisitionRecipeCatalog.ALL.stream()
                        .map(PipeAcquisitionRecipeCatalog
                                .RecipeSpec::materialId)
                        .collect(java.util.stream.Collectors.toSet()));
        for (String material : Set.of(
                "wood", "carbon", "plastic", "rubber", "wood_treated")) {
            assertEquals(
                    Set.of(
                            MaterialPrefixes.TINY_FLUID_PIPE,
                            MaterialPrefixes.SMALL_FLUID_PIPE,
                            MaterialPrefixes.FLUID_PIPE,
                            MaterialPrefixes.LARGE_FLUID_PIPE,
                            MaterialPrefixes.HUGE_FLUID_PIPE),
                    PipeAcquisitionRecipeCatalog.ALL.stream()
                            .filter(spec ->
                                    spec.materialId().equals(material))
                            .map(PipeAcquisitionRecipeCatalog
                                    .RecipeSpec::output)
                            .collect(java.util.stream.Collectors.toSet()));
        }
    }

    @Test
    void woodRowsRetainPinnedSourceLineAndOperandKinds() {
        Map<Integer, Operand> expected = Map.of(
                1887, Operand.WOODEN_SLABS,
                1888, Operand.PLANKS,
                1889, Operand.PLANKS,
                1890, Operand.PLANKS,
                1891, Operand.LOGS);
        var wood = PipeAcquisitionRecipeCatalog.ALL.stream()
                .filter(spec -> spec.materialId().equals("wood"))
                .toList();
        assertEquals(5, wood.size());
        wood.forEach(spec -> {
            assertEquals(
                    Classification.GT6_SOURCE_CRAFTING,
                    spec.classification());
            assertEquals(
                    expected.get(spec.sourceLine()),
                    spec.operands().get('W'));
            assertEquals(1, spec.outputCount());
        });
    }

    @Test
    void designRowsUseExactGaugeMaterialRatios() {
        List<Double> expectedRatios =
                List.of(0.25, 0.5, 1.5, 3.0, 6.0);
        for (String material : Set.of(
                "carbon", "plastic", "rubber", "wood_treated")) {
            var rows = PipeAcquisitionRecipeCatalog.ALL.stream()
                    .filter(spec -> spec.materialId().equals(material))
                    .toList();
            assertEquals(5, rows.size());
            assertEquals(
                    expectedRatios,
                    rows.stream()
                            .map(spec -> structuralOperands(spec)
                                    / (double) spec.outputCount())
                            .toList());
            assertTrue(rows.stream().allMatch(spec ->
                    spec.classification()
                            == Classification.DESIGN_POLICY_NON_GT6));
        }
    }

    private static long structuralOperands(
            PipeAcquisitionRecipeCatalog.RecipeSpec spec) {
        return spec.pattern().stream()
                .flatMapToInt(String::chars)
                .filter(symbol -> symbol == 'P')
                .count();
    }
}
