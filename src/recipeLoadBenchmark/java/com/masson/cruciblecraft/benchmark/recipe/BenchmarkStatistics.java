package com.masson.cruciblecraft.benchmark.recipe;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class BenchmarkStatistics {
    private BenchmarkStatistics() {}

    static Map<String, Object> timing(long[] rawSamples, int warmup) {
        if (rawSamples.length == 0) {
            throw new IllegalArgumentException("Timing samples are empty");
        }
        long[] samples = rawSamples.clone();
        Arrays.sort(samples);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "PASS");
        result.put("warmup_iterations", warmup);
        result.put("samples", samples.length);
        result.put("raw_samples_ns", Arrays.stream(rawSamples).boxed().toList());
        result.put("p50_ns", percentile(samples, 0.50));
        result.put("p95_ns", percentile(samples, 0.95));
        result.put("max_ns", samples[samples.length - 1]);
        result.put("error_bar", medianConfidenceBand(samples));
        result.put("p95_error_bar", percentileConfidenceBand(
                samples, 0.95, "p95"));
        return result;
    }

    static Map<String, Object> counts(int[] rawSamples) {
        if (rawSamples.length == 0) {
            throw new IllegalArgumentException("Count samples are empty");
        }
        int[] samples = rawSamples.clone();
        Arrays.sort(samples);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("samples", samples.length);
        result.put("raw_samples", Arrays.stream(rawSamples).boxed().toList());
        result.put("p50", percentile(samples, 0.50));
        result.put("p95", percentile(samples, 0.95));
        result.put("max", samples[samples.length - 1]);
        return result;
    }

    static Map<String, Object> sampleRange(
            List<Long> rawSamples,
            String fieldName) {
        if (rawSamples.isEmpty()) {
            throw new IllegalArgumentException("Range samples are empty");
        }
        long[] ordered = rawSamples.stream().mapToLong(Long::longValue).sorted()
                .toArray();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "PASS");
        result.put("samples", rawSamples.size());
        result.put("raw_samples_" + fieldName, rawSamples);
        result.put("p50_" + fieldName, percentile(ordered, 0.50));
        result.put("min_" + fieldName, ordered[0]);
        result.put("max_" + fieldName, ordered[ordered.length - 1]);
        result.put("error_bar", Map.of(
                "method", "sample_min_max",
                "lower_" + fieldName, ordered[0],
                "upper_" + fieldName, ordered[ordered.length - 1]));
        return result;
    }

    private static Map<String, Object> medianConfidenceBand(long[] samples) {
        return percentileConfidenceBand(samples, 0.50, "median");
    }

    private static Map<String, Object> percentileConfidenceBand(
            long[] samples,
            double percentile,
            String label) {
        int count = samples.length;
        double center = percentile * count;
        double halfWidth = 1.96
                * Math.sqrt(count * percentile * (1.0 - percentile));
        int lower = Math.max(
                0, (int) Math.floor(center - halfWidth) - 1);
        int upper = Math.min(
                count - 1, (int) Math.ceil(center + halfWidth) - 1);
        return Map.of(
                "method", "nonparametric_95_percent_"
                        + label + "_order_statistic",
                "lower_ns", samples[lower],
                "upper_ns", samples[upper]);
    }

    private static long percentile(long[] sorted, double percentile) {
        int index = Math.max(
                0,
                (int) Math.ceil(percentile * sorted.length) - 1);
        return sorted[index];
    }

    private static int percentile(int[] sorted, double percentile) {
        int index = Math.max(
                0,
                (int) Math.ceil(percentile * sorted.length) - 1);
        return sorted[index];
    }
}
