package com.masson.cruciblecraft.recipe.gt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.resources.ResourceLocation;

/**
 * Test-side load of {@code archive/sealed/forward-v2/semantic_id_map.json}.
 * Frozen v2 manifests and catalog fixtures keep historical TXX ids; live
 * generated families and {@link CompactPublicationGroups} use semantic ids.
 */
final class SemanticIdMap {
    private static final Path MAP = Path.of(
            "archive/sealed/forward-v2/semantic_id_map.json");

    private static final Map<String, String> PUBLICATION_GROUPS;
    private static final List<PrefixRule> STABLE_PREFIXES;

    static {
        try {
            JsonObject document = JsonParser.parseString(
                    Files.readString(MAP, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            Map<String, String> groups = new LinkedHashMap<>();
            JsonObject groupsJson = document.getAsJsonObject("publication_groups");
            for (Map.Entry<String, JsonElement> entry : groupsJson.entrySet()) {
                groups.put(entry.getKey(), entry.getValue().getAsString());
            }
            PUBLICATION_GROUPS = Map.copyOf(groups);
            List<PrefixRule> prefixes = new ArrayList<>();
            for (JsonElement element : document.getAsJsonArray("stable_id_prefixes")) {
                JsonObject row = element.getAsJsonObject();
                prefixes.add(new PrefixRule(
                        row.get("old").getAsString(),
                        row.get("new").getAsString(),
                        row.has("path_contains")
                                ? row.get("path_contains").getAsString()
                                : ""));
            }
            STABLE_PREFIXES = List.copyOf(prefixes);
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private SemanticIdMap() {}

    static String remapPublicationGroup(String group) {
        return PUBLICATION_GROUPS.getOrDefault(group, group);
    }

    static ResourceLocation remapPublicationGroup(ResourceLocation group) {
        return ResourceLocation.parse(remapPublicationGroup(group.toString()));
    }

    static String publicationGroup(JsonObject group) {
        return remapPublicationGroup(group.get("publication_group").getAsString());
    }

    static ResourceLocation publicationGroupId(JsonObject group) {
        return ResourceLocation.parse(publicationGroup(group));
    }

    static void addRemappedEagerIds(JsonObject group, Set<ResourceLocation> eager) {
        JsonArray eagerIds = group.getAsJsonArray("eager_stable_ids");
        if (eagerIds == null) {
            return;
        }
        eagerIds.forEach(element ->
                eager.add(ResourceLocation.parse(remapStableId(element.getAsString()))));
    }

    static String remapStableId(String stableId) {
        return remapStableId(stableId, "");
    }

    static String remapStableId(String stableId, String sourcePath) {
        String posix = sourcePath.replace('\\', '/');
        for (PrefixRule rule : STABLE_PREFIXES) {
            if (rule.pathContains.isEmpty()
                    || !posix.contains(rule.pathContains)) {
                continue;
            }
            if (stableId.startsWith(rule.oldPrefix)) {
                return rule.newPrefix + stableId.substring(rule.oldPrefix.length());
            }
        }
        for (PrefixRule rule : STABLE_PREFIXES) {
            if (!rule.pathContains.isEmpty()) {
                continue;
            }
            if (stableId.startsWith(rule.oldPrefix)) {
                return rule.newPrefix + stableId.substring(rule.oldPrefix.length());
            }
        }
        return stableId;
    }

    static String historicalStableId(String semanticId) {
        PrefixRule best = null;
        for (PrefixRule rule : STABLE_PREFIXES) {
            if (!rule.pathContains.isEmpty()) {
                continue;
            }
            if (semanticId.startsWith(rule.newPrefix)
                    && (best == null
                            || rule.newPrefix.length() > best.newPrefix.length())) {
                best = rule;
            }
        }
        if (best == null) {
            return semanticId;
        }
        return best.oldPrefix + semanticId.substring(best.newPrefix.length());
    }

    static JsonObject frozenRelation(JsonObject relations, String stableId) {
        JsonElement direct = relations.get(stableId);
        if (direct != null && direct.isJsonObject()) {
            return direct.getAsJsonObject();
        }
        JsonElement historical = relations.get(historicalStableId(stableId));
        if (historical != null && historical.isJsonObject()) {
            return historical.getAsJsonObject();
        }
        return null;
    }

    private record PrefixRule(String oldPrefix, String newPrefix, String pathContains) {}
}
