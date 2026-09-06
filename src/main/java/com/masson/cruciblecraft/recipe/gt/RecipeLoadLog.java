package com.masson.cruciblecraft.recipe.gt;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.masson.cruciblecraft.CrucibleCraft;

import net.minecraft.resources.ResourceLocation;

/**
 * Aggregates recipe-load diagnostics per epoch. Parse failures stay
 * failures; only the log line is collapsed.
 */
public final class RecipeLoadLog {
    private static final ConcurrentHashMap<Long, Map<String, Aggregate>> BY_EPOCH =
            new ConcurrentHashMap<>();

    private RecipeLoadLog() {}

    public record Aggregate(
            String fingerprint,
            String exceptionType,
            ResourceLocation map,
            ResourceLocation holder,
            String field,
            String sourcePath,
            int count) {}

    public static void parseFailure(
            long epoch,
            String exceptionType,
            ResourceLocation map,
            ResourceLocation holder,
            String field,
            String sourcePath) {
        Objects.requireNonNull(exceptionType, "exceptionType");
        String fingerprint = exceptionType + "|"
                + (map == null ? "" : map)
                + "|"
                + field;
        BY_EPOCH.compute(epoch, (ignored, current) -> {
            Map<String, Aggregate> next = current == null
                    ? new LinkedHashMap<>()
                    : new LinkedHashMap<>(current);
            Aggregate previous = next.get(fingerprint);
            int count = previous == null ? 1 : previous.count() + 1;
            next.put(fingerprint, new Aggregate(
                    fingerprint,
                    exceptionType,
                    map,
                    holder,
                    field,
                    sourcePath,
                    count));
            return next;
        });
    }

    public static void flushEpoch(
            long epoch,
            int suppressedReloads,
            String cause,
            int dataGeneration) {
        Map<String, Aggregate> aggregates = BY_EPOCH.remove(epoch);
        if (suppressedReloads > 0) {
            CrucibleCraft.LOGGER.info(
                    "Recipe reload suppressed {} duplicate request(s) for generation {} ({})",
                    suppressedReloads,
                    dataGeneration,
                    cause);
        }
        if (aggregates == null || aggregates.isEmpty()) {
            return;
        }
        for (Aggregate aggregate : aggregates.values()) {
            CrucibleCraft.LOGGER.error(
                    "Recipe parse failures epoch {} fingerprint {}: {} time(s); "
                            + "type={} map={} holder={} field={} source={}",
                    epoch,
                    aggregate.fingerprint(),
                    aggregate.count(),
                    aggregate.exceptionType(),
                    aggregate.map(),
                    aggregate.holder(),
                    aggregate.field(),
                    aggregate.sourcePath());
        }
    }

    public static void publicationSummary(
            long epoch,
            GTRecipeMapLoader.PublicationMetrics metrics,
            int mapCount,
            int unindexedMaps) {
        CrucibleCraft.LOGGER.info(
                "Published recipe epoch {} maps={} unindexedMaps={} logical={} eager={} "
                        + "reload={}ms index={}ms gen={} cause={} requests={} publications={} "
                        + "suppressed={}",
                epoch,
                mapCount,
                unindexedMaps,
                metrics.allPublishedRecipes(),
                metrics.eagerPublishedRecipes(),
                metrics.reloadMillis(),
                metrics.indexMillis(),
                metrics.control().dataGeneration(),
                metrics.control().requestCause(),
                metrics.control().reloadRequestCount(),
                metrics.control().actualPublicationCount(),
                metrics.control().suppressedReloadCount());
        CrucibleCraft.LOGGER.info(
                "Recipe publication phases epoch {}: parse={}ms collect={}ms dedup={}ms "
                        + "family={}ms validate={}ms tempIndex={}ms finalIndex={}ms emi={}ms "
                        + "alloc={} retained={}",
                epoch,
                metrics.phaseTimings().parseMillis(),
                metrics.phaseTimings().sourceCollectionMillis(),
                metrics.phaseTimings().dedupMillis(),
                metrics.phaseTimings().familyPrepareMillis(),
                metrics.phaseTimings().completeValidationMillis(),
                metrics.phaseTimings().temporaryIndexMillis(),
                metrics.phaseTimings().finalIndexMillis(),
                metrics.phaseTimings().emiProjectionMillis(),
                metrics.allocation().reloadTransientAllocation(),
                metrics.allocation().retainedMemory());
    }

    static void resetForTest() {
        BY_EPOCH.clear();
    }
}
