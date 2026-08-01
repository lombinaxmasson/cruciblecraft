package com.masson.cruciblecraft.recipe.gt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;

import com.masson.cruciblecraft.recipe.AnvilMode;

import org.junit.jupiter.api.Test;

class AnvilRecipeProjectionTest {
    @Test
    void preservesInputOrderCountsModeAndSpecialFields() {
        var projection = AnvilRecipeProjection.project(
                "primary_input",
                2,
                Optional.of("second_input"),
                3,
                "primary_output",
                4,
                Optional.of("secondary_output"),
                5,
                0.25,
                AnvilMode.BEND_SMALL,
                80_000L,
                6);

        assertEquals(
                List.of(
                        new AnvilRecipeProjection.ResourceStack<>("primary_input", 2),
                        new AnvilRecipeProjection.ResourceStack<>("second_input", 3)),
                projection.itemInputs());
        assertEquals(AnvilMode.BEND_SMALL, projection.mode());
        assertEquals(1, projection.duration());
        assertEquals(80_000L, projection.eut());
        assertEquals(6L, projection.specialValue());
    }

    @Test
    void secondaryOutputAndChanceStayAtMatchingSecondIndex() {
        var projection = AnvilRecipeProjection.project(
                "input",
                1,
                Optional.empty(),
                1,
                "primary",
                1,
                Optional.of("secondary"),
                2,
                0.125,
                AnvilMode.BEND_BIG,
                10_000L,
                4);

        assertEquals(
                List.of(
                        new AnvilRecipeProjection.ResourceStack<>("primary", 1),
                        new AnvilRecipeProjection.ResourceStack<>("secondary", 2)),
                projection.itemOutputs());
        assertEquals(List.of(10_000, 1_250), projection.outputChances());
        assertEquals(AnvilMode.BEND_BIG, projection.mode());
    }
}
