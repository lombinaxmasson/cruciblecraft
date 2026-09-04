package com.masson.cruciblecraft.recipe.gt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Repeatable T12a micro-harness for the current CapacityMatcher implementation.
 *
 * <p>This is deliberately not a JUnit timing assertion. Run it through the
 * {@code t12CapacityMatcherBenchmark} Gradle task to refresh the auditable
 * artifact after matcher or JVM changes.
 */
public final class CapacityMatcherBenchmarkHarness {
    private static final int WARMUP_ITERATIONS = 200;
    private static final int SAMPLES = 200;
    private static final int OPERATIONS_PER_SAMPLE = 10;
    private static final long DENSE_P95_BUDGET_NS = 20_000_000L;
    private static final long PRESENCE_P95_BUDGET_NS = 50_000_000L;
    private static final int[] SUPPLY_COUNTS = {12, 16, 32, 64};

    private CapacityMatcherBenchmarkHarness() {}

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException(
                    "Expected one output path for the benchmark artifact");
        }
        List<Result> results = new ArrayList<>();
        for (int supplies : SUPPLY_COUNTS) {
            results.add(measureDenseConsuming(supplies));
        }
        results.add(measurePresenceBoundary());
        for (int supplies : new int[] {16, 32, 64}) {
            results.add(measurePresenceCapRejection(supplies));
        }
        if (results.stream().anyMatch(result -> !result.withinBudget())) {
            throw new IllegalStateException("CapacityMatcher benchmark budget exceeded");
        }

        Path output = Path.of(args[0]);
        Files.createDirectories(output.toAbsolutePath().getParent());
        Files.writeString(output, encode(results), StandardCharsets.UTF_8);
        System.out.println("Wrote " + output);
    }

    private static Result measureDenseConsuming(int supplyCount) {
        long[] demands = new long[supplyCount];
        long[] supplies = new long[supplyCount];
        Arrays.fill(demands, 1L);
        Arrays.fill(supplies, 1L);
        boolean[][] compatible = dense(supplyCount, supplyCount);
        Runnable operation = () -> {
            long[][] allocation = CapacityMatcher.solve(
                    demands, supplies, compatible).orElseThrow();
            assertAllocation(demands, supplies, allocation);
        };
        Timing timing = measure(operation);
        return new Result(
                "dense_consuming_" + supplyCount,
                supplyCount,
                supplyCount,
                Math.multiplyExact(supplyCount, supplyCount),
                0,
                "satisfied",
                timing,
                DENSE_P95_BUDGET_NS);
    }

    private static Result measurePresenceBoundary() {
        int supplyCount = 12;
        long[] demands = {11L, 0L, 0L, 0L, 0L};
        long[] supplies = new long[supplyCount];
        Arrays.fill(supplies, 1L);
        boolean[][] compatible = dense(demands.length, supplyCount);
        boolean[] presenceOnly = {false, true, true, true, true};
        Runnable operation = () -> {
            long[][] allocation = CapacityMatcher.solve(
                    demands, supplies, compatible, presenceOnly).orElseThrow();
            assertAllocation(new long[] {11L}, supplies,
                    new long[][] {allocation[0]});
        };
        Timing timing = measure(operation);
        return new Result(
                "dense_presence_boundary_12",
                demands.length,
                supplyCount,
                demands.length * supplyCount,
                4,
                "satisfied_with_shared_unconsumed_witness",
                timing,
                PRESENCE_P95_BUDGET_NS);
    }

    private static Result measurePresenceCapRejection(int supplyCount) {
        long[] demands = {0L};
        long[] supplies = new long[supplyCount];
        Arrays.fill(supplies, 1L);
        boolean[][] compatible = dense(1, supplyCount);
        boolean[] presenceOnly = {true};
        Runnable operation = () -> {
            try {
                CapacityMatcher.solve(
                        demands, supplies, compatible, presenceOnly);
                throw new AssertionError("Expected presence supply-cap rejection");
            } catch (IllegalArgumentException expected) {
                if (!expected.getMessage().contains("cap=12")) {
                    throw new AssertionError("Unexpected rejection", expected);
                }
            }
        };
        Timing timing = measure(operation);
        return new Result(
                "presence_cap_rejection_" + supplyCount,
                1,
                supplyCount,
                supplyCount,
                1,
                "rejected_by_explicit_12_supply_cap",
                timing,
                PRESENCE_P95_BUDGET_NS);
    }

    private static boolean[][] dense(int requirements, int supplies) {
        boolean[][] compatible = new boolean[requirements][supplies];
        for (boolean[] row : compatible) {
            Arrays.fill(row, true);
        }
        return compatible;
    }

    private static Timing measure(Runnable operation) {
        for (int index = 0; index < WARMUP_ITERATIONS; index++) {
            operation.run();
        }
        long[] samples = new long[SAMPLES];
        for (int sample = 0; sample < SAMPLES; sample++) {
            long started = System.nanoTime();
            for (int operationIndex = 0;
                    operationIndex < OPERATIONS_PER_SAMPLE;
                    operationIndex++) {
                operation.run();
            }
            samples[sample] =
                    (System.nanoTime() - started) / OPERATIONS_PER_SAMPLE;
        }
        Arrays.sort(samples);
        return new Timing(
                samples[SAMPLES / 2],
                samples[(int) Math.ceil(SAMPLES * 0.95) - 1],
                samples[SAMPLES - 1]);
    }

    private static void assertAllocation(
            long[] demands, long[] supplies, long[][] allocation) {
        for (int requirement = 0; requirement < demands.length; requirement++) {
            long allocated = Arrays.stream(allocation[requirement]).sum();
            if (allocated != demands[requirement]) {
                throw new AssertionError("Requirement allocation drifted");
            }
        }
        for (int supply = 0; supply < supplies.length; supply++) {
            long allocated = 0L;
            for (long[] row : allocation) {
                allocated += row[supply];
            }
            if (allocated > supplies[supply]) {
                throw new AssertionError("Supply allocation drifted");
            }
        }
    }

    private static String encode(List<Result> results) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"schema_version\": 1,\n");
        json.append("  \"status\": \"CAPACITY_MATCHER_BENCHMARK_READY\",\n");
        json.append("  \"harness\": \"")
                .append(CapacityMatcherBenchmarkHarness.class.getName())
                .append("\",\n");
        json.append("  \"method\": {\n");
        json.append("    \"warmup_iterations\": ")
                .append(WARMUP_ITERATIONS).append(",\n");
        json.append("    \"samples\": ").append(SAMPLES).append(",\n");
        json.append("    \"operations_per_sample\": ")
                .append(OPERATIONS_PER_SAMPLE).append(",\n");
        json.append("    \"clock\": \"System.nanoTime\"\n");
        json.append("  },\n");
        json.append("  \"decision\": {\n");
        json.append("    \"current_presence_supply_cap\": 12,\n");
        json.append("    \"matcher_rewritten\": false,\n");
        json.append("    \"t12a_action\": \"preserve_explicit_cap\",\n");
        json.append("    \"revisit_point\": \"T15e derives one Large Centrifuge item matcher supply from the shared host; rerun and reconsider the algorithm if a future presence schema exposes more than 12 item supplies.\"\n");
        json.append("  },\n");
        json.append("  \"runtime\": {\n");
        json.append("    \"java_version\": \"")
                .append(escape(System.getProperty("java.version")))
                .append("\",\n");
        json.append("    \"java_vm\": \"")
                .append(escape(System.getProperty("java.vm.name")))
                .append("\",\n");
        json.append("    \"os_arch\": \"")
                .append(escape(System.getProperty("os.arch")))
                .append("\"\n");
        json.append("  },\n");
        json.append("  \"scenarios\": [\n");
        for (int index = 0; index < results.size(); index++) {
            Result result = results.get(index);
            json.append("    {\n");
            json.append("      \"id\": \"").append(result.id()).append("\",\n");
            json.append("      \"requirements\": ")
                    .append(result.requirements()).append(",\n");
            json.append("      \"supplies\": ").append(result.supplies()).append(",\n");
            json.append("      \"edges\": ").append(result.edges()).append(",\n");
            json.append("      \"presence_only\": ")
                    .append(result.presenceOnly()).append(",\n");
            json.append("      \"expected_result\": \"")
                    .append(result.expectedResult()).append("\",\n");
            json.append("      \"p50_ns\": ").append(result.timing().p50()).append(",\n");
            json.append("      \"p95_ns\": ").append(result.timing().p95()).append(",\n");
            json.append("      \"max_ns\": ").append(result.timing().max()).append(",\n");
            json.append("      \"p95_budget_ns\": ")
                    .append(result.p95Budget()).append(",\n");
            json.append("      \"within_budget\": ")
                    .append(result.withinBudget()).append("\n");
            json.append("    }");
            json.append(index + 1 == results.size() ? "\n" : ",\n");
        }
        json.append("  ]\n");
        json.append("}\n");
        return json.toString();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record Timing(long p50, long p95, long max) {}

    private record Result(
            String id,
            int requirements,
            int supplies,
            int edges,
            int presenceOnly,
            String expectedResult,
            Timing timing,
            long p95Budget) {
        boolean withinBudget() {
            return timing.p95() <= p95Budget;
        }
    }
}
