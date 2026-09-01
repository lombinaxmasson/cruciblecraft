package com.masson.cruciblecraft.scale;

import java.util.ArrayList;
import java.util.List;

/**
 * Optional scale counters. Default off: ordinary runtime does not
 * allocate sample lists or change scheduling or caches. Each discovery
 * call records its own visited size; the recorder never accumulates
 * across calls into a single overflowing counter.
 */
public final class ScaleInstrumentation {
    private static final ThreadLocal<Recorder> RECORDER = new ThreadLocal<>();

    private ScaleInstrumentation() {}

    public static boolean isEnabled() {
        return RECORDER.get() != null;
    }

    public static void enter(Recorder recorder) {
        RECORDER.set(recorder);
    }

    public static void exit() {
        RECORDER.remove();
    }

    public static void recordRouteVisit(int visited) {
        Recorder recorder = RECORDER.get();
        if (recorder == null) {
            return;
        }
        recorder.recordVisited(visited);
    }

    public static final class Recorder {
        private final List<Integer> visitedSamples = new ArrayList<>();
        private int visitedMax;
        private long cacheEntriesMax;

        public void recordVisited(int visited) {
            if (visited < 0) {
                throw new IllegalArgumentException("visited must be >= 0");
            }
            visitedSamples.add(visited);
            if (visited > visitedMax) {
                visitedMax = visited;
            }
        }

        public void recordCacheEntries(int entries) {
            if (entries < 0) {
                throw new IllegalArgumentException("cache entries must be >= 0");
            }
            if (entries > cacheEntriesMax) {
                cacheEntriesMax = entries;
            }
        }

        public void reset() {
            visitedSamples.clear();
            visitedMax = 0;
            cacheEntriesMax = 0L;
        }

        public List<Integer> visitedSamples() {
            return List.copyOf(visitedSamples);
        }

        public int visitedMax() {
            return visitedMax;
        }

        public long cacheEntriesMax() {
            return cacheEntriesMax;
        }

        public int visitedP95() {
            return percentile(visitedSamples, 95);
        }

        static int percentile(List<Integer> values, int percentile) {
            if (values.isEmpty()) {
                return 0;
            }
            ArrayList<Integer> sorted = new ArrayList<>(values);
            sorted.sort(Integer::compareTo);
            int index = (int) Math.ceil(percentile / 100.0d * sorted.size()) - 1;
            return sorted.get(Math.max(0, Math.min(sorted.size() - 1, index)));
        }
    }
}
