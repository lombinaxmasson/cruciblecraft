package com.masson.cruciblecraft.content.multiblock;

import java.io.IOException;
import java.io.Reader;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.CrucibleCraft;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.LevelReader;

/**
 * Process-wide immutable catalog. A reload decodes and validates every file
 * into a temporary map before publishing one new snapshot.
 */
public final class MultiblockStructureCatalog {
    public static final String DIRECTORY = "multiblock_structures";

    private static volatile Snapshot snapshot = new Snapshot(0L, Map.of());

    private MultiblockStructureCatalog() {}

    public static Optional<MultiblockStructureDefinition> find(
            ResourceLocation id) {
        return Optional.ofNullable(snapshot.definitions().get(id));
    }

    public static MultiblockStructureDefinition require(ResourceLocation id) {
        MultiblockStructureDefinition definition =
                snapshot.definitions().get(id);
        if (definition == null) {
            throw new IllegalStateException(
                    "Multiblock structure is not loaded: " + id);
        }
        return definition;
    }

    public static MultiblockStructureValidator.ValidationResult validate(
            ResourceLocation id,
            LevelReader level,
            BlockPos controller,
            Direction facing) {
        return MultiblockStructureValidator.validate(
                require(id), level, controller, facing);
    }

    public static BlockPos anchor(
            ResourceLocation id,
            String anchor,
            BlockPos controller,
            Direction facing) {
        return require(id).anchor(anchor, controller, facing);
    }

    public static long revision() {
        return snapshot.revision();
    }

    public static Map<ResourceLocation, MultiblockStructureDefinition> all() {
        return snapshot.definitions();
    }

    public static void reload(ResourceManager resourceManager) {
        publish(prepare(resourceManager));
    }

    /**
     * Decodes the current resource-manager view without publishing it.
     *
     * <p>EMI uses this during client recipe registration. Keeping the
     * non-publishing form here means a client-side display cannot race or
     * replace the authoritative runtime snapshot used by validators.</p>
     */
    public static Map<ResourceLocation, MultiblockStructureDefinition> prepare(
            ResourceManager resourceManager) {
        Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                DIRECTORY,
                location -> location.getPath().endsWith(".json"));
        LinkedHashMap<ResourceLocation, JsonElement> json = new LinkedHashMap<>();
        resources.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(
                        Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> {
                    ResourceLocation id = fileId(entry.getKey());
                    try (Reader reader = entry.getValue().openAsReader()) {
                        if (json.putIfAbsent(
                                        id,
                                        JsonParser.parseReader(reader))
                                != null) {
                            throw new IllegalArgumentException(
                                    "Duplicate multiblock structure " + id);
                        }
                    } catch (IOException exception) {
                        throw new IllegalStateException(
                                "Failed to read multiblock structure "
                                        + entry.getKey(),
                                exception);
                    }
                });
        return decodeSnapshot(json);
    }

    static synchronized void replaceFromJson(
            Map<ResourceLocation, JsonElement> json) {
        publish(decodeSnapshot(json));
    }

    static synchronized void publish(
            Map<ResourceLocation, MultiblockStructureDefinition> decoded) {
        Snapshot current = snapshot;
        snapshot = new Snapshot(
                Math.addExact(current.revision(), 1L), decoded);
        CrucibleCraft.LOGGER.info(
                "Loaded {} multiblock structure definitions (revision {})",
                decoded.size(),
                snapshot.revision());
    }

    static Map<ResourceLocation, MultiblockStructureDefinition> decodeSnapshot(
            Map<ResourceLocation, JsonElement> json) {
        LinkedHashMap<ResourceLocation, MultiblockStructureDefinition> decoded =
                new LinkedHashMap<>();
        json.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(
                        Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> {
                    MultiblockStructureDefinition definition =
                            MultiblockStructureDefinition.CODEC
                                    .parse(JsonOps.INSTANCE, entry.getValue())
                                    .getOrThrow(message ->
                                            new IllegalArgumentException(
                                                    "Invalid multiblock "
                                                            + entry.getKey()
                                                            + ": "
                                                            + message));
                    validateRegistryReferences(entry.getKey(), definition);
                    decoded.put(entry.getKey(), definition);
                });
        return Map.copyOf(decoded);
    }

    private static void validateRegistryReferences(
            ResourceLocation id,
            MultiblockStructureDefinition definition) {
        List<ResourceLocation> missing = definition.palette().values().stream()
                .flatMap(predicate -> predicate.block().stream())
                .filter(block -> BuiltInRegistries.BLOCK
                        .getOptional(block)
                        .isEmpty())
                .distinct()
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException(
                    "Multiblock " + id + " references unknown blocks " + missing);
        }
    }

    private static ResourceLocation fileId(ResourceLocation file) {
        String path = file.getPath();
        String prefix = DIRECTORY + "/";
        if (!path.startsWith(prefix) || !path.endsWith(".json")) {
            throw new IllegalArgumentException(
                    "Invalid multiblock resource path " + file);
        }
        return ResourceLocation.fromNamespaceAndPath(
                file.getNamespace(),
                path.substring(prefix.length(), path.length() - ".json".length()));
    }

    private record Snapshot(
            long revision,
            Map<ResourceLocation, MultiblockStructureDefinition> definitions) {
        private Snapshot {
            definitions = Map.copyOf(definitions);
        }
    }
}
