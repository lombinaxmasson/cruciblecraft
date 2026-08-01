package com.masson.cruciblecraft.recipe.rule;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleExpressionTest {
    private static final RuleExpression.Context CONTEXT = new RuleExpression.Context() {
        private final Map<String, Double> values = Map.of(
                "material.tier", 2.0,
                "material.mass", 288.0,
                "material.thermal.melting_point", 1538.0,
                "material.thermal.boiling_point", 2861.0,
                "material.thermal.density", 7.874,
                "prefix.units", 144.0,
                "input.units", 144.0,
                "output.units", 72.0);

        @Override public double number(String name) {
            Double value = values.get(name);
            if (value == null) throw new IllegalArgumentException("unknown " + name);
            return value;
        }

        @Override public double lookup(String function, String key) {
            return switch (function + ":" + key) {
                case "target_units:smelting" -> 16;
                case "prefix_units:ingot" -> 144;
                default -> throw new IllegalArgumentException("unknown lookup");
            };
        }

        @Override public boolean materialHas(String flag) {
            return Set.of("metal", "cruciblecraft:metal").contains(flag);
        }

        @Override public boolean hasPrefix(String prefix) {
            return Set.of("ingot", "cruciblecraft:ingot").contains(prefix);
        }
    };

    @Test
    void respectsPrecedenceAndNumericFunctions() {
        assertEquals(
                15,
                RuleExpression.numeric("1 + 2 * 7", "test:precedence")
                        .evaluateInt(CONTEXT));
        assertEquals(
                256,
                RuleExpression.numeric(
                                "max(ceil(material.mass/prefix.units), 2)*tier_voltage(material.tier)",
                                "test:functions")
                        .evaluateInt(CONTEXT));
    }

    @Test
    void evaluatesTypedConditions() {
        assertTrue(RuleExpression.bool(
                        "material.has(metal) && has_prefix(cruciblecraft:ingot) && material.tier >= 2",
                        "test:condition")
                .evaluateBoolean(CONTEXT));
    }

    @Test
    void computesExactReducedBatchRatios() {
        assertEquals(9, RuleExpression.numeric(
                        "prefix_units(ingot) / gcd(target_units(smelting), prefix_units(ingot))",
                        "test:batch-input")
                .evaluateInt(CONTEXT));
        assertEquals(1, RuleExpression.numeric(
                        "target_units('smelting') / gcd(target_units(smelting), prefix_units(ingot))",
                        "test:batch-output")
                .evaluateInt(CONTEXT));
        assertThrows(IllegalArgumentException.class, () ->
                RuleExpression.numeric("gcd(0, 144)", "test:gcd-zero")
                        .evaluateInt(CONTEXT));
        assertThrows(IllegalArgumentException.class, () ->
                RuleExpression.numeric("gcd(1.5, 144)", "test:gcd-fraction")
                        .evaluateInt(CONTEXT));
    }

    @Test
    void rejectsTypeErrorsSyntaxAndInvalidResultsWithRuleId() {
        assertTrue(assertThrows(
                        IllegalArgumentException.class,
                        () -> RuleExpression.numeric("true + 1", "test:type"))
                .getMessage().contains("test:type"));
        assertTrue(assertThrows(
                        IllegalArgumentException.class,
                        () -> RuleExpression.numeric("1 +", "test:syntax"))
                .getMessage().contains("test:syntax"));
        assertTrue(assertThrows(
                        IllegalArgumentException.class,
                        () -> RuleExpression.numeric("1 / 0", "test:zero")
                                .evaluateInt(CONTEXT))
                .getMessage().contains("test:zero"));
        assertThrows(
                IllegalArgumentException.class,
                () -> RuleExpression.numeric("1e400", "test:finite"));
        assertThrows(
                IllegalArgumentException.class,
                () -> RuleExpression.numeric("9223372036854775808", "test:overflow")
                        .evaluateLong(CONTEXT));
        assertTrue(assertThrows(
                        IllegalArgumentException.class,
                        () -> RuleExpression.numeric(
                                "9007199254740992 + 1",
                                "test:unsafe-literal"))
                .getMessage().contains("exact double range"));
        assertTrue(assertThrows(
                        IllegalArgumentException.class,
                        () -> RuleExpression.numeric(
                                        "9007199254740991 + 1",
                                        "test:unsafe-arithmetic")
                                .evaluateLong(CONTEXT))
                .getMessage().contains("exact double range"));
    }

    @Test
    void enforcesSourceLengthBoundaryWithContext() {
        String boundary = "1" + " ".repeat(RuleExpression.MAX_SOURCE_LENGTH - 1);
        assertEquals(
                1,
                RuleExpression.numeric(boundary, "test:length-boundary")
                        .evaluateInt(CONTEXT));

        String oversized = boundary + " ";
        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> RuleExpression.numeric(oversized, "test:length-overflow"));
        assertTrue(failure.getMessage().contains("test:length-overflow"));
        assertTrue(failure.getMessage().contains(oversized));
        assertTrue(failure.getMessage().contains("source length"));
    }

    @Test
    void acceptsSixtyFourNestedParserAndEvaluationLayers() {
        String parentheses = "(".repeat(RuleExpression.MAX_NESTING_DEPTH)
                + "1"
                + ")".repeat(RuleExpression.MAX_NESTING_DEPTH);
        assertEquals(
                1,
                RuleExpression.numeric(parentheses, "test:parentheses-64")
                        .evaluateInt(CONTEXT));

        String functions = nestedFunctions(RuleExpression.MAX_NESTING_DEPTH);
        assertEquals(
                1,
                RuleExpression.numeric(functions, "test:functions-64")
                        .evaluateInt(CONTEXT));

        String unary = "-".repeat(RuleExpression.MAX_NESTING_DEPTH) + "1";
        assertEquals(
                1,
                RuleExpression.numeric(unary, "test:unary-64")
                        .evaluateInt(CONTEXT));

        String binary = "1+".repeat(RuleExpression.MAX_NESTING_DEPTH) + "1";
        assertEquals(
                RuleExpression.MAX_NESTING_DEPTH + 1,
                RuleExpression.numeric(binary, "test:binary-64")
                        .evaluateInt(CONTEXT));
    }

    @Test
    void rejectsSixtyFiveNestedParserOrEvaluationLayersWithContext() {
        int oversizedDepth = RuleExpression.MAX_NESTING_DEPTH + 1;
        assertDepthFailure(
                "(".repeat(oversizedDepth) + "1" + ")".repeat(oversizedDepth),
                "test:parentheses-65");
        assertDepthFailure(nestedFunctions(oversizedDepth), "test:functions-65");
        assertDepthFailure("-".repeat(oversizedDepth) + "1", "test:unary-65");
        assertDepthFailure("1+".repeat(oversizedDepth) + "1", "test:binary-65");
    }

    private static String nestedFunctions(int depth) {
        return "ceil(".repeat(depth) + "1" + ")".repeat(depth);
    }

    private static void assertDepthFailure(String source, String ruleId) {
        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> RuleExpression.numeric(source, ruleId));
        assertTrue(failure.getMessage().contains(ruleId));
        assertTrue(failure.getMessage().contains(source));
        assertTrue(failure.getMessage().contains("depth"));
    }
}
