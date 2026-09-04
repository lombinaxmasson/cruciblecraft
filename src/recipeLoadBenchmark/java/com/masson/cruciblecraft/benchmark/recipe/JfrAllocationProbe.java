package com.masson.cruciblecraft.benchmark.recipe;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordingFile;

final class JfrAllocationProbe {
    private static final List<String> EVENT_TYPES = List.of(
            "jdk.ObjectAllocationInNewTLAB",
            "jdk.ObjectAllocationOutsideTLAB");

    private JfrAllocationProbe() {}

    static Map<String, Object> measure(
            int samples,
            Path outputDirectory,
            Path repositoryRoot,
            String candidate,
            String scale,
            Supplier<Runnable> workloadFactory) {
        return measure(
                samples,
                outputDirectory,
                repositoryRoot,
                candidate,
                scale,
                "lookup",
                "lookup-only after publication; enumeration excluded",
                workloadFactory);
    }

    static Map<String, Object> measure(
            int samples,
            Path outputDirectory,
            Path repositoryRoot,
            String candidate,
            String scale,
            String window,
            String scope,
            Supplier<Runnable> workloadFactory) {
        try {
            Files.createDirectories(outputDirectory);
            List<Long> allocationBytes = new ArrayList<>();
            List<Long> eventCounts = new ArrayList<>();
            List<String> recordings = new ArrayList<>();
            for (int sample = 0; sample < samples; sample++) {
                Runnable workload = Objects.requireNonNull(
                        workloadFactory.get(), "JFR " + window + " workload");
                Path recordingPath = outputDirectory.resolve(
                        candidate + "-" + scale + "-" + window + "-" + sample + ".jfr");
                Files.deleteIfExists(recordingPath);
                try (Recording recording = new Recording()) {
                    for (String eventType : EVENT_TYPES) {
                        recording.enable(eventType)
                                .withThreshold(Duration.ZERO)
                                .withoutStackTrace();
                    }
                    recording.start();
                    workload.run();
                    recording.stop();
                    recording.dump(recordingPath);
                }
                long bytes = 0L;
                long events = 0L;
                try (RecordingFile file = new RecordingFile(recordingPath)) {
                    while (file.hasMoreEvents()) {
                        var event = file.readEvent();
                        if (!EVENT_TYPES.contains(
                                event.getEventType().getName())) {
                            continue;
                        }
                        events++;
                        if (event.hasField("allocationSize")) {
                            bytes = Math.addExact(
                                    bytes, event.getLong("allocationSize"));
                        } else if (event.hasField("tlabSize")) {
                            bytes = Math.addExact(
                                    bytes, event.getLong("tlabSize"));
                        }
                    }
                }
                allocationBytes.add(bytes);
                eventCounts.add(events);
                recordings.add(relativeOrAbsolute(
                        repositoryRoot, recordingPath));
            }
            Map<String, Object> result = new LinkedHashMap<>(
                    BenchmarkStatistics.sampleRange(
                            allocationBytes, "bytes"));
            result.put("measurement", "JFR observed allocation-event bytes");
            result.put("window", window);
            result.put("zero_event_semantics",
                    "Measured zero events is a real lookup-window observation, "
                            + "not an unmeasured zero-fill");
            result.put("scope", scope);
            result.put("event_types", EVENT_TYPES);
            result.put("raw_event_counts", eventCounts);
            result.put("recordings", recordings);
            return result;
        } catch (Throwable failure) {
            return new LinkedHashMap<>(Map.of(
                    "status", "SKIP",
                    "samples", 0,
                    "window", window,
                    "reason", failure.getClass().getName() + ": "
                            + String.valueOf(failure.getMessage()),
                    "measurement", "JFR observed allocation-event bytes",
                    "scope", scope,
                    "zero_event_semantics",
                            "SKIP is fail-closed for v3; it is not a zero-fill PASS",
                    "event_types", EVENT_TYPES));
        }
    }

    private static String relativeOrAbsolute(Path root, Path path) {
        Path absoluteRoot = root.toAbsolutePath().normalize();
        Path absolutePath = path.toAbsolutePath().normalize();
        try {
            return absoluteRoot.relativize(absolutePath)
                    .toString().replace('\\', '/');
        } catch (IllegalArgumentException ignored) {
            return absolutePath.toString().replace('\\', '/');
        }
    }
}
