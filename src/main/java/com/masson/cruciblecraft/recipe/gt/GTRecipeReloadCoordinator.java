package com.masson.cruciblecraft.recipe.gt;

import java.util.Objects;

/**
 * Request-identity publication gate. Same manager identity and data
 * generation publish once per runtime side. A lower generation cannot
 * overwrite a newer published generation on that side. Tokens are
 * identity-based, never wall-clock.
 */
public final class GTRecipeReloadCoordinator {
    public enum Cause {
        TAGS_UPDATED,
        SERVER_STARTED,
        CLIENT_RECIPES_UPDATED
    }

    public record RequestIdentity(
            Object manager,
            int dataGeneration,
            ExtruderRecipeFamilyProvider.RuntimeSide side,
            Cause cause) {
        public RequestIdentity {
            Objects.requireNonNull(manager, "manager");
            Objects.requireNonNull(side, "side");
            Objects.requireNonNull(cause, "cause");
        }
    }

    public record Decision(
            boolean publish,
            int suppressedCount,
            String reason) {}

    private static final Object LOCK = new Object();
    private static Object lastSeenManager;
    private static int nextGeneration = 1;
    private static int generationForManager;
    private static Object publishedManager;
    private static int publishedGeneration;
    private static ExtruderRecipeFamilyProvider.RuntimeSide publishedSide;
    private static int suppressedSincePublish;
    private static int requestCount;
    private static int publicationCount;

    private GTRecipeReloadCoordinator() {}

    public static RequestIdentity identify(
            Object manager,
            ExtruderRecipeFamilyProvider.RuntimeSide side,
            Cause cause) {
        Objects.requireNonNull(manager, "manager");
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(cause, "cause");
        synchronized (LOCK) {
            if (lastSeenManager != manager) {
                lastSeenManager = manager;
                generationForManager = nextGeneration++;
            }
            requestCount++;
            return new RequestIdentity(manager, generationForManager, side, cause);
        }
    }

    public static Decision decide(RequestIdentity request) {
        Objects.requireNonNull(request, "request");
        synchronized (LOCK) {
            if (publishedManager != null
                    && publishedSide == request.side()
                    && request.dataGeneration() < publishedGeneration) {
                suppressedSincePublish++;
                return new Decision(false, suppressedSincePublish, "stale_generation");
            }
            if (publishedManager == request.manager()
                    && publishedGeneration == request.dataGeneration()
                    && publishedSide == request.side()) {
                suppressedSincePublish++;
                return new Decision(false, suppressedSincePublish, "same_generation");
            }
            return new Decision(true, 0, "publish");
        }
    }

    public static void markPublished(RequestIdentity request) {
        Objects.requireNonNull(request, "request");
        synchronized (LOCK) {
            publishedManager = request.manager();
            publishedGeneration = request.dataGeneration();
            publishedSide = request.side();
            suppressedSincePublish = 0;
            publicationCount++;
        }
    }

    public static int requestCount() {
        synchronized (LOCK) {
            return requestCount;
        }
    }

    public static int publicationCount() {
        synchronized (LOCK) {
            return publicationCount;
        }
    }

    public static int suppressedSincePublish() {
        synchronized (LOCK) {
            return suppressedSincePublish;
        }
    }

    static void resetForTest() {
        synchronized (LOCK) {
            lastSeenManager = null;
            nextGeneration = 1;
            generationForManager = 0;
            publishedManager = null;
            publishedGeneration = 0;
            publishedSide = null;
            suppressedSincePublish = 0;
            requestCount = 0;
            publicationCount = 0;
        }
    }
}
