package com.masson.cruciblecraft.benchmark.recipe;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class RetainedMemoryProbe {
    private static final String BENCHMARK_PACKAGE =
            "com.masson.cruciblecraft.benchmark.recipe";
    private static final Pattern HISTOGRAM_ROW = Pattern.compile(
            "(?m)^\\s*\\d+:\\s+(\\d+)\\s+(\\d+)\\s+(.+?)\\s*$");
    private static final Pattern HISTOGRAM_TOTAL = Pattern.compile(
            "(?m)^\\s*Total\\s+(\\d+)\\s+(\\d+)\\s*$");

    private RetainedMemoryProbe() {}

    static Map<String, Object> measure(
            BenchmarkPolicy policy,
            Path repositoryRoot,
            String candidate,
            BenchmarkPolicy.Scale scale) {
        Path jcmd = findJdkTool("jcmd");
        if (jcmd == null) {
            return skipped(
                    "JDK jcmd is unavailable; retained memory was not "
                            + "reported as zero or PASS");
        }
        List<Map<String, Object>> samples = new ArrayList<>();
        try {
            for (int sample = 0; sample < policy.retainedSamples; sample++) {
                samples.add(measureOne(
                        policy,
                        repositoryRoot,
                        candidate,
                        scale,
                        sample,
                        jcmd));
            }
        } catch (Throwable failure) {
            Map<String, Object> result = skipped(
                    failure.getClass().getName() + ": "
                            + String.valueOf(failure.getMessage()));
            result.put("partial_samples", samples);
            return result;
        }

        List<Long> totalBytes = samples.stream()
                .map(row -> (Long) row.get("total_bytes"))
                .toList();
        List<Long> benchmarkBytes = samples.stream()
                .map(row -> (Long) row.get("benchmark_class_bytes"))
                .toList();
        List<Long> totalInstances = samples.stream()
                .map(row -> (Long) row.get("total_instances"))
                .toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "PASS");
        result.put("protocol",
                "isolated child JVM; jcmd GC.run; jcmd GC.class_histogram");
        result.put("retained_scope",
                "published snapshot plus production lookup cache; "
                        + "enumeration was not executed");
        result.put("naive_heap_delta_used", false);
        result.put("jcmd", jcmd.toString().replace('\\', '/'));
        result.put("samples", samples.size());
        result.put("total_bytes", BenchmarkStatistics.sampleRange(
                totalBytes, "bytes"));
        result.put("benchmark_class_bytes", BenchmarkStatistics.sampleRange(
                benchmarkBytes, "bytes"));
        result.put("total_instances", BenchmarkStatistics.sampleRange(
                totalInstances, "instances"));
        result.put("sample_metadata", samples);
        return result;
    }

    private static Map<String, Object> measureOne(
            BenchmarkPolicy policy,
            Path repositoryRoot,
            String candidate,
            BenchmarkPolicy.Scale scale,
            int sample,
            Path jcmd) throws Exception {
        List<String> command = List.of(
                javaBinary().toString(),
                "-Xms64m",
                "-Xmx" + policy.retainedHeap,
                "-XX:+UseG1GC",
                "-cp",
                System.getProperty("java.class.path"),
                RetainedMemoryChild.class.getName(),
                candidate,
                repositoryRoot.resolve(policy.compactPath)
                        .toAbsolutePath().normalize().toString(),
                Integer.toString(scale.relationCount()),
                Integer.toString(policy.hotModulo),
                Long.toString(1000L + sample),
                Integer.toString(policy.retainedLookupWarmup));
        Process child = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();
        try {
            CompletableFuture<String> readyFuture = CompletableFuture.supplyAsync(
                    () -> {
                        try {
                            return child.inputReader(StandardCharsets.UTF_8)
                                    .readLine();
                        } catch (IOException exception) {
                            throw new IllegalStateException(exception);
                        }
                    });
            String ready = readyFuture.get(45, TimeUnit.SECONDS);
            if (ready == null || !ready.startsWith("READY|")) {
                throw new IllegalStateException(
                        "Retained child did not reach READY: " + ready);
            }
            String[] fields = ready.split("\\|", -1);
            if (fields.length != 12) {
                throw new IllegalStateException(
                        "Retained child READY metadata is malformed");
            }
            long pid = Long.parseLong(fields[1]);
            CommandResult flags = runJcmd(jcmd, pid, "VM.flags");
            CommandResult gc = runJcmd(jcmd, pid, "GC.run");
            if (gc.exitCode() != 0) {
                throw new IllegalStateException(
                        "jcmd GC.run failed: " + gc.output());
            }
            CommandResult histogram = runJcmd(
                    jcmd, pid, "GC.class_histogram");
            if (histogram.exitCode() != 0) {
                throw new IllegalStateException(
                        "jcmd GC.class_histogram failed: "
                                + histogram.output());
            }
            Histogram parsed = parseHistogram(histogram.output());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("sample", sample);
            result.put("pid", pid);
            result.put("java_version", fields[2]);
            result.put("java_vm", fields[3]);
            result.put("gc_names", fields[4]);
            result.put("heap_max_bytes", Long.parseLong(fields[5]));
            result.put("loaded_classes_at_ready", Long.parseLong(fields[6]));
            result.put("workload_checksum", Long.parseLong(fields[7]));
            result.put("cache_policy", fields[8]);
            result.put("cache_ceiling", Integer.parseInt(fields[9]));
            result.put("cache_size", Integer.parseInt(fields[10]));
            result.put("cache_evictions", Long.parseLong(fields[11]));
            result.put("histogram_class_rows", parsed.classRows());
            result.put("total_instances", parsed.totalInstances());
            result.put("total_bytes", parsed.totalBytes());
            result.put("benchmark_class_instances",
                    parsed.benchmarkInstances());
            result.put("benchmark_class_bytes", parsed.benchmarkBytes());
            result.put("vm_flags_status", flags.exitCode() == 0
                    ? "PASS" : "SKIP");
            result.put("vm_flags", compact(flags.output()));
            result.put("controlled_gc_command", "jcmd " + pid + " GC.run");
            result.put("histogram_command",
                    "jcmd " + pid + " GC.class_histogram");
            return result;
        } finally {
            try {
                child.outputWriter(StandardCharsets.UTF_8)
                        .append("EXIT\n")
                        .flush();
            } catch (IOException ignored) {
                // A failed child is forcibly terminated below.
            }
            if (!child.waitFor(10, TimeUnit.SECONDS)) {
                child.destroyForcibly();
                child.waitFor(10, TimeUnit.SECONDS);
            }
        }
    }

    private static CommandResult runJcmd(
            Path jcmd,
            long pid,
            String operation) throws Exception {
        Process process = new ProcessBuilder(
                jcmd.toString(), Long.toString(pid), operation)
                .redirectErrorStream(true)
                .start();
        CompletableFuture<byte[]> output = CompletableFuture.supplyAsync(() -> {
            try {
                return process.getInputStream().readAllBytes();
            } catch (IOException exception) {
                throw new IllegalStateException(exception);
            }
        });
        if (!process.waitFor(90, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("Timed out running jcmd " + operation);
        }
        return new CommandResult(
                process.exitValue(),
                new String(output.get(5, TimeUnit.SECONDS),
                        StandardCharsets.UTF_8));
    }

    private static Histogram parseHistogram(String output) {
        Matcher total = HISTOGRAM_TOTAL.matcher(output);
        if (!total.find()) {
            throw new IllegalStateException(
                    "jcmd histogram omitted its Total row");
        }
        long totalInstances = Long.parseLong(total.group(1));
        long totalBytes = Long.parseLong(total.group(2));
        int classRows = 0;
        long benchmarkInstances = 0L;
        long benchmarkBytes = 0L;
        Matcher row = HISTOGRAM_ROW.matcher(output);
        while (row.find()) {
            classRows++;
            String className = row.group(3);
            if (className.contains(BENCHMARK_PACKAGE)) {
                benchmarkInstances += Long.parseLong(row.group(1));
                benchmarkBytes += Long.parseLong(row.group(2));
            }
        }
        if (totalBytes <= 0L || totalInstances <= 0L || classRows <= 0) {
            throw new IllegalStateException(
                    "jcmd histogram totals were not positive");
        }
        return new Histogram(
                classRows,
                totalInstances,
                totalBytes,
                benchmarkInstances,
                benchmarkBytes);
    }

    private static Path findJdkTool(String name) {
        String executable = isWindows() ? name + ".exe" : name;
        Path bundled = Path.of(
                System.getProperty("java.home"), "bin", executable);
        if (Files.isRegularFile(bundled)) {
            return bundled.toAbsolutePath().normalize();
        }
        String path = System.getenv("PATH");
        if (path != null) {
            for (String directory : path.split(
                    Pattern.quote(File.pathSeparator))) {
                Path candidate = Path.of(directory, executable);
                if (Files.isRegularFile(candidate)) {
                    return candidate.toAbsolutePath().normalize();
                }
            }
        }
        return null;
    }

    private static Path javaBinary() {
        Path result = findJdkTool("java");
        if (result == null) {
            throw new IllegalStateException("JDK java launcher is unavailable");
        }
        return result;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "")
                .toLowerCase(java.util.Locale.ROOT)
                .contains("win");
    }

    private static String compact(String value) {
        String normalized = value.replace('\r', ' ')
                .replace('\n', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        return normalized.length() <= 2048
                ? normalized : normalized.substring(0, 2048);
    }

    private static Map<String, Object> skipped(String reason) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "SKIP");
        result.put("samples", 0);
        result.put("reason", reason);
        result.put("naive_heap_delta_used", false);
        result.put("total_bytes", null);
        return result;
    }

    private record CommandResult(int exitCode, String output) {}

    private record Histogram(
            int classRows,
            long totalInstances,
            long totalBytes,
            long benchmarkInstances,
            long benchmarkBytes) {}
}
