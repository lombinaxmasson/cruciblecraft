package com.masson.cruciblecraft.logistics.pipe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;

import net.minecraft.resources.ResourceLocation;

/**
 * Bounded vanilla-crafting projection for the five nonmetal fluid-pipe
 * materials. GT6's five Wood rows stay source-derived; the other twenty rows
 * are explicit CrucibleCraft design policy.
 */
public final class PipeAcquisitionRecipeCatalog {
    private static final List<Gauge> DESIGN_GAUGES = List.of(
            new Gauge(MaterialPrefixes.TINY_FLUID_PIPE, List.of("P"), 4),
            new Gauge(MaterialPrefixes.SMALL_FLUID_PIPE, List.of("PP"), 4),
            new Gauge(MaterialPrefixes.FLUID_PIPE, List.of("PPP"), 2),
            new Gauge(MaterialPrefixes.LARGE_FLUID_PIPE, List.of("P", "P", "P"), 1),
            new Gauge(
                    MaterialPrefixes.HUGE_FLUID_PIPE,
                    List.of("P P", "P P", "P P"),
                    1));

    public static final List<RecipeSpec> ALL = build();

    private PipeAcquisitionRecipeCatalog() {}

    public static RecipeSpec require(
            String materialId, MaterialPrefix output) {
        return ALL.stream()
                .filter(spec -> spec.materialId().equals(materialId)
                        && spec.output().equals(output))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No pipe acquisition recipe for "
                                + materialId + "/" + output.serializedName()));
    }

    private static List<RecipeSpec> build() {
        List<RecipeSpec> recipes = new ArrayList<>();
        recipes.add(sourceWood(
                MaterialPrefixes.TINY_FLUID_PIPE,
                List.of("W"),
                Operand.WOODEN_SLABS,
                1887));
        recipes.add(sourceWood(
                MaterialPrefixes.SMALL_FLUID_PIPE,
                List.of("W"),
                Operand.PLANKS,
                1888));
        recipes.add(sourceWood(
                MaterialPrefixes.FLUID_PIPE,
                List.of("WWW"),
                Operand.PLANKS,
                1889));
        recipes.add(sourceWood(
                MaterialPrefixes.LARGE_FLUID_PIPE,
                List.of("WW ", "W W", " WW"),
                Operand.PLANKS,
                1890));
        recipes.add(sourceWood(
                MaterialPrefixes.HUGE_FLUID_PIPE,
                List.of("W W"),
                Operand.WOODEN_BEAMS,
                1891));
        recipes.addAll(design("carbon", Operand.CARBON_DUST));
        recipes.addAll(design("plastic", Operand.PLASTIC_PLATE));
        recipes.addAll(design("rubber", Operand.RUBBER_PLATE));
        recipes.addAll(treatedWoodDesign());
        if (recipes.size() != 25
                || recipes.stream()
                        .map(RecipeSpec::id)
                        .distinct()
                        .count() != 25) {
            throw new IllegalStateException(
                    "Pipe acquisition catalog must contain 25 unique rows");
        }
        return List.copyOf(recipes);
    }

    private static RecipeSpec sourceWood(
            MaterialPrefix output,
            List<String> pattern,
            Operand operand,
            int sourceLine) {
        return new RecipeSpec(
                id("wood", output),
                "wood",
                output,
                pattern,
                Map.of('W', operand),
                1,
                Classification.GT6_SOURCE_CRAFTING,
                sourceLine);
    }

    private static List<RecipeSpec> design(
            String materialId, Operand operand) {
        return DESIGN_GAUGES.stream()
                .map(gauge -> new RecipeSpec(
                        id(materialId, gauge.output()),
                        materialId,
                        gauge.output(),
                        gauge.pattern(),
                        Map.of('P', operand),
                        gauge.outputCount(),
                        Classification.DESIGN_POLICY_NON_GT6,
                        0))
                .toList();
    }

    private static List<RecipeSpec> treatedWoodDesign() {
        List<RecipeSpec> result = new ArrayList<>();
        result.add(treated(
                MaterialPrefixes.TINY_FLUID_PIPE,
                List.of("P", "C"),
                4));
        result.add(treated(
                MaterialPrefixes.SMALL_FLUID_PIPE,
                List.of("PP", "C "),
                4));
        result.add(treated(
                MaterialPrefixes.FLUID_PIPE,
                List.of("PPP", " C "),
                2));
        result.add(treated(
                MaterialPrefixes.LARGE_FLUID_PIPE,
                List.of("PC", "P ", "P "),
                1));
        result.add(treated(
                MaterialPrefixes.HUGE_FLUID_PIPE,
                List.of("P P", "PCP", "P P"),
                1));
        return List.copyOf(result);
    }

    private static RecipeSpec treated(
            MaterialPrefix output,
            List<String> pattern,
            int outputCount) {
        Map<Character, Operand> operands = new LinkedHashMap<>();
        operands.put('P', Operand.PLANKS);
        operands.put('C', Operand.COAL_COKE);
        return new RecipeSpec(
                id("wood_treated", output),
                "wood_treated",
                output,
                pattern,
                Map.copyOf(operands),
                outputCount,
                Classification.DESIGN_POLICY_NON_GT6,
                0);
    }

    private static ResourceLocation id(
            String materialId, MaterialPrefix output) {
        return ResourceLocation.fromNamespaceAndPath(
                CrucibleCraft.MODID,
                "pipe_acquisition/" + materialId + "/"
                        + output.serializedName());
    }

    public enum Operand {
        WOODEN_SLABS,
        PLANKS,
        WOODEN_BEAMS,
        CARBON_DUST,
        PLASTIC_PLATE,
        RUBBER_PLATE,
        COAL_COKE
    }

    public enum Classification {
        GT6_SOURCE_CRAFTING,
        DESIGN_POLICY_NON_GT6
    }

    public record RecipeSpec(
            ResourceLocation id,
            String materialId,
            MaterialPrefix output,
            List<String> pattern,
            Map<Character, Operand> operands,
            int outputCount,
            Classification classification,
            int sourceLine) {
        public RecipeSpec {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(materialId, "materialId");
            Objects.requireNonNull(output, "output");
            pattern = List.copyOf(pattern);
            operands = Map.copyOf(operands);
            Objects.requireNonNull(classification, "classification");
            if (pattern.isEmpty()
                    || pattern.size() > 3
                    || pattern.stream().anyMatch(
                            row -> row.isEmpty() || row.length() > 3)
                    || pattern.stream().mapToInt(String::length)
                            .distinct().count() != 1) {
                throw new IllegalArgumentException(
                        "Invalid shaped pattern for " + id);
            }
            Set<Character> used = pattern.stream()
                    .flatMapToInt(String::chars)
                    .mapToObj(value -> (char) value)
                    .filter(symbol -> symbol != ' ')
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
            if (!used.equals(operands.keySet())
                    || outputCount <= 0
                    || outputCount > 64) {
                throw new IllegalArgumentException(
                        "Invalid shaped operands/result for " + id);
            }
            String expectedSpecification = PipeCatalog.requireSpecification(
                    PipeCatalog.Kind.FLUID, output.serializedName());
            if (!expectedSpecification.equals(
                    PipeCatalog.requireSpecification(
                            PipeCatalog.Kind.FLUID,
                            expectedSpecification))) {
                throw new IllegalArgumentException(
                        "Pipe output/specification mapping drifted for " + id);
            }
            if ((classification == Classification.GT6_SOURCE_CRAFTING)
                    != (materialId.equals("wood")
                            && sourceLine >= 1887
                            && sourceLine <= 1891)) {
                throw new IllegalArgumentException(
                        "Pipe acquisition evidence classification mismatch for "
                                + id);
            }
        }
    }

    private record Gauge(
            MaterialPrefix output,
            List<String> pattern,
            int outputCount) {}
}
