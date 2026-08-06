package com.masson.cruciblecraft.benchmark.recipe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.masson.cruciblecraft.benchmark.recipe.RecipeFamilyProvider.ExtruderRelation;

/** Predeclared lookup traces derived from the actual compact lazy-by-item index. */
final class LookupWorkloads {
    static final List<String> TRACE_IDS = List.of(
            "uniform_cycle_worst_case",
            "locality_80_20",
            "repeated_current_input");

    private LookupWorkloads() {}

    static Workload build(
            List<ExtruderRelation> relations,
            int operationsPerTrace) {
        if (operationsPerTrace <= 0) {
            throw new IllegalArgumentException(
                    "Lookup trace operation count must be positive");
        }
        Map<String, List<ExtruderRelation>> mutableGroups =
                new LinkedHashMap<>();
        for (ExtruderRelation relation : relations) {
            if (!relation.eagerEligible()) {
                mutableGroups.computeIfAbsent(
                                relation.inputKey(),
                                ignored -> new ArrayList<>())
                        .add(relation);
            }
        }
        List<List<ExtruderRelation>> groups = mutableGroups.values().stream()
                .map(List::copyOf)
                .toList();
        if (groups.isEmpty()) {
            throw new IllegalArgumentException(
                    "Actual compact workload has no lazy-by-item groups");
        }
        int lazyRelations = groups.stream().mapToInt(List::size).sum();
        int maxCandidates = groups.stream().mapToInt(List::size).max()
                .orElseThrow();
        List<ExtruderRelation> worstCycle =
                new ArrayList<>(operationsPerTrace);
        for (int operation = 0; operation < operationsPerTrace; operation++) {
            List<ExtruderRelation> group = groups.get(operation % groups.size());
            worstCycle.add(group.getLast());
        }

        int hotGroups = Math.max(1, (groups.size() + 4) / 5);
        List<ExtruderRelation> locality = new ArrayList<>(operationsPerTrace);
        for (int operation = 0; operation < operationsPerTrace; operation++) {
            int groupIndex;
            if (operation % 5 != 4 || hotGroups == groups.size()) {
                groupIndex = Math.floorMod(operation * 17, hotGroups);
            } else {
                int coldGroups = groups.size() - hotGroups;
                groupIndex = hotGroups + Math.floorMod(
                        (operation / 5) * 31, coldGroups);
            }
            List<ExtruderRelation> group = groups.get(groupIndex);
            locality.add(group.getLast());
        }

        List<ExtruderRelation> currentGroup = groups.stream()
                .max(java.util.Comparator.comparingInt(List::size))
                .orElseThrow();
        ExtruderRelation current = currentGroup.getLast();
        List<ExtruderRelation> repeated = new ArrayList<>(operationsPerTrace);
        for (int operation = 0; operation < operationsPerTrace; operation++) {
            repeated.add(current);
        }

        return new Workload(
                lazyRelations,
                groups.size(),
                maxCandidates,
                List.of(
                        new Trace(
                                TRACE_IDS.get(0),
                                "uniform cycle over every actual lazy input; "
                                        + "last candidate forces worst scan",
                                List.copyOf(worstCycle)),
                        new Trace(
                                TRACE_IDS.get(1),
                                "deterministic 80 percent hot-group and "
                                        + "20 percent cold-group locality",
                                List.copyOf(locality)),
                        new Trace(
                                TRACE_IDS.get(2),
                                "repeat the current input and exact relation",
                                List.copyOf(repeated))));
    }

    record Workload(
            int lazyRelations,
            int lazyInputGroups,
            int maxCandidatesPerInput,
            List<Trace> traces) {
        Workload {
            traces = List.copyOf(traces);
            if (!traces.stream().map(Trace::id).toList().equals(TRACE_IDS)) {
                throw new IllegalArgumentException(
                        "Lookup trace declaration order drifted");
            }
        }
    }

    record Trace(
            String id,
            String description,
            List<ExtruderRelation> operations) {
        Trace {
            operations = List.copyOf(operations);
            if (id == null || id.isBlank()
                    || description == null || description.isBlank()
                    || operations.isEmpty()) {
                throw new IllegalArgumentException(
                        "Lookup trace must be complete");
            }
        }
    }
}
