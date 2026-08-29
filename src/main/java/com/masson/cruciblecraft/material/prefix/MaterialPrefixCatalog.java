package com.masson.cruciblecraft.material.prefix;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.GsonBuilder;
import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

/**
 * Startup-only registry for material prefixes.
 *
 * <p>The complete immutable state is published through one volatile write.
 * Ordinary datapacks never participate in this registry and therefore cannot
 * create registry entries after NeoForge registration has begun. Production
 * must call {@link #bootstrap(Path)} exactly once; catalog-dependent access
 * before that point fails instead of silently freezing an incomplete table.
 *
 * <p>When a config directory is supplied, {@code _legacy_prefixes.json}
 * accumulates item suffix-to-canonical-path mappings and is never pruned
 * automatically, allowing stacks from removed addon prefixes to remain
 * identifiable without loading the removed addon.
 */
public final class MaterialPrefixCatalog {
    private static final String RESOURCE_ROOT = "/data/cruciblecraft/material_prefixes/";
    private static final String LEGACY_MANIFEST = "_legacy_prefixes.json";
    private static final com.google.gson.Gson GSON =
            new GsonBuilder().setPrettyPrinting().create();
    private static volatile State state = State.empty();
    private static final Map<String, MaterialPrefixDefinition> STARTUP_ADDITIONS =
            new LinkedHashMap<>();

    private MaterialPrefixCatalog() {}

    public static synchronized void bootstrap(Path configDirectory) {
        if (state.bootstrapped()) {
            throw new IllegalStateException("Material prefix catalog already bootstrapped");
        }
        LinkedHashMap<String, MaterialPrefixDefinition> definitions = new LinkedHashMap<>();
        loadBundled(definitions);
        STARTUP_ADDITIONS.values().forEach(definition ->
                put(definitions, definition, "addon registration"));
        if (configDirectory != null) {
            loadConfig(configDirectory, definitions);
        }
        LinkedHashMap<String, String> legacySuffixes = configDirectory == null
                ? new LinkedHashMap<>()
                : loadLegacyManifest(configDirectory.resolve(LEGACY_MANIFEST));
        mergeCurrentSuffixes(legacySuffixes, definitions.values());
        State published = freeze(definitions.values(), legacySuffixes);
        if (configDirectory != null) {
            writeLegacyManifest(configDirectory.resolve(LEGACY_MANIFEST), legacySuffixes);
        }
        state = published;
        STARTUP_ADDITIONS.clear();
    }

    /** Addon API used before {@link #bootstrap(Path)}. Duplicate ids are rejected. */
    public static synchronized boolean addStartupPrefix(MaterialPrefixDefinition definition) {
        if (state.bootstrapped()) {
            throw new IllegalStateException("Prefixes can only be added before bootstrap");
        }
        return STARTUP_ADDITIONS.putIfAbsent(
                definition.prefix().id(), definition) == null;
    }

    public static boolean isBootstrapped() {
        return state.bootstrapped();
    }

    public static Collection<MaterialPrefix> values() {
        return requireState().orderedPrefixes();
    }

    public static Collection<MaterialPrefixDefinition> definitions() {
        return requireState().definitions().values();
    }

    /** Validates a prospective complete table without publishing global state. */
    public static void validateDefinitions(Collection<MaterialPrefixDefinition> definitions) {
        freeze(definitions, Map.of());
    }

    public static MaterialPrefixDefinition definition(MaterialPrefix prefix) {
        MaterialPrefixDefinition definition = requireState().definitions().get(prefix.id());
        if (definition == null) {
            throw new IllegalArgumentException("Unknown material prefix: " + prefix.id());
        }
        return definition;
    }

    public static MaterialPrefix require(String idOrAlias) {
        return find(idOrAlias).orElseThrow(() ->
                new IllegalArgumentException("Unknown material prefix: " + idOrAlias));
    }

    public static Optional<MaterialPrefix> find(String idOrAlias) {
        if (idOrAlias == null || idOrAlias.isBlank()) {
            return Optional.empty();
        }
        State snapshot = requireState();
        MaterialPrefix alias = snapshot.aliases().get(idOrAlias);
        if (alias != null) {
            return Optional.of(alias);
        }
        String id = idOrAlias.indexOf(':') >= 0
                ? idOrAlias
                : CrucibleCraft.MODID + ":" + idOrAlias;
        return Optional.ofNullable(snapshot.definitions().get(id))
                .map(MaterialPrefixDefinition::prefix);
    }

    public static DataResult<MaterialPrefix> decode(String idOrAlias) {
        try {
            return find(idOrAlias)
                    .map(DataResult::success)
                    .orElseGet(() -> DataResult.error(
                            () -> "Unknown material prefix: " + idOrAlias));
        } catch (RuntimeException exception) {
            return DataResult.error(exception::getMessage);
        }
    }

    /** Compiles namespaced generation flags into the frozen catalog's BitSet layout. */
    public static BitSet compileGenerationFlags(Collection<String> flags) {
        if (flags.isEmpty()) {
            return new BitSet();
        }
        State snapshot = requireState();
        BitSet result = new BitSet(snapshot.generationFlags().size());
        for (String value : flags) {
            String flag = parseNamespaced(value);
            Integer bit = snapshot.generationFlags().get(flag);
            if (bit == null) {
                throw new IllegalArgumentException("Unknown material generation flag: " + value);
            }
            result.set(bit);
        }
        return result;
    }

    /**
     * Resolves material structure in catalog order: enabled generation flags,
     * then explicit includes, then explicit excludes. Legacy forms are accepted
     * only as compatibility input when new structural fields are absent.
     */
    public static List<MaterialPrefix> resolve(
            Collection<String> generationFlags,
            Collection<String> includePrefixes,
            Collection<String> excludePrefixes,
            Collection<String> legacyForms) {
        return resolve(
                generationFlags, includePrefixes, excludePrefixes, legacyForms, false);
    }

    /** Resolves factual structure for an explicitly declared metadata-only material. */
    public static List<MaterialPrefix> resolve(
            Collection<String> generationFlags,
            Collection<String> includePrefixes,
            Collection<String> excludePrefixes,
            Collection<String> legacyForms,
            boolean metadataOnly) {
        requireUnique("generation flag", generationFlags);
        requireUniquePrefixes("included prefix", includePrefixes);
        requireUniquePrefixes("excluded prefix", excludePrefixes);
        requireUniquePrefixes("legacy form", legacyForms);
        if (metadataOnly
                && generationFlags.isEmpty()
                && includePrefixes.isEmpty()
                && excludePrefixes.isEmpty()) {
            return List.of();
        }
        if (generationFlags.isEmpty()
                && includePrefixes.isEmpty()
                && excludePrefixes.isEmpty()
                && !legacyForms.isEmpty()) {
            return legacyForms.stream().map(MaterialPrefixCatalog::require).toList();
        }
        State snapshot = requireState();
        BitSet bits = compileGenerationFlags(generationFlags);
        Set<MaterialPrefix> selected = new LinkedHashSet<>();
        for (MaterialPrefix prefix : snapshot.orderedPrefixes()) {
            int bit = snapshot.generationFlags().get(
                    snapshot.definitions().get(prefix.id()).generationFlag());
            if (bits.get(bit)) {
                selected.add(prefix);
            }
        }
        includePrefixes.forEach(value -> selected.add(require(value)));
        for (MaterialPrefix prefix : List.copyOf(selected)) {
            selected.addAll(snapshot.impliedClosures().getOrDefault(prefix, List.of()));
        }
        excludePrefixes.forEach(value -> selected.remove(require(value)));
        if (selected.isEmpty() && !metadataOnly) {
            throw new IllegalArgumentException("Material must resolve at least one prefix");
        }
        return snapshot.orderedPrefixes().stream().filter(selected::contains).toList();
    }

    private static void requireUnique(String label, Collection<String> values) {
        if (new LinkedHashSet<>(values).size() != values.size()) {
            throw new IllegalArgumentException("Duplicate material " + label);
        }
    }

    private static void requireUniquePrefixes(String label, Collection<String> values) {
        LinkedHashSet<MaterialPrefix> prefixes = new LinkedHashSet<>();
        for (String value : values) {
            if (!prefixes.add(require(value))) {
                throw new IllegalArgumentException("Duplicate material " + label + ": " + value);
            }
        }
    }

    /** Prefix suffixes and aliases sorted longest-first for saved-stack migration. */
    public static List<Suffix> legacySuffixesLongestFirst() {
        return requireState().suffixes();
    }

    private static State requireState() {
        State snapshot = state;
        if (!snapshot.bootstrapped()) {
            throw new IllegalStateException(
                    "Material prefix catalog has not been explicitly bootstrapped");
        }
        return snapshot;
    }

    static synchronized void resetForTests() {
        state = State.empty();
        STARTUP_ADDITIONS.clear();
    }

    private static State freeze(
            Collection<MaterialPrefixDefinition> input,
            Map<String, String> legacyHistory) {
        List<MaterialPrefixDefinition> ordered = List.copyOf(input);
        LinkedHashMap<String, MaterialPrefixDefinition> definitions =
                new LinkedHashMap<>();
        LinkedHashMap<String, MaterialPrefix> aliases = new LinkedHashMap<>();
        Set<String> serializedPaths = new LinkedHashSet<>();
        for (MaterialPrefixDefinition definition : ordered) {
            String id = definition.prefix().id();
            if (definitions.putIfAbsent(id, definition) != null) {
                throw new IllegalArgumentException("Duplicate material prefix id: " + id);
            }
            if (!serializedPaths.add(definition.serializedPath())) {
                throw new IllegalArgumentException(
                        "Duplicate material prefix serialized_path: "
                                + definition.serializedPath());
            }
            addAlias(aliases, id.toString(), definition.prefix());
            if (id.startsWith(CrucibleCraft.MODID + ":")) {
                addAlias(aliases, id.substring(id.indexOf(':') + 1), definition.prefix());
            }
            addAlias(aliases, definition.serializedPath(), definition.prefix());
            definition.aliases().forEach(alias -> addAlias(aliases, alias, definition.prefix()));
        }
        List<String> flags = definitions.values().stream()
                .map(MaterialPrefixDefinition::generationFlag)
                .distinct()
                .sorted()
                .toList();
        LinkedHashMap<String, Integer> flagBits = new LinkedHashMap<>();
        for (int index = 0; index < flags.size(); index++) {
            flagBits.put(flags.get(index), index);
        }
        Map<MaterialPrefix, List<MaterialPrefix>> impliedClosures =
                buildImpliedClosures(ordered, aliases);
        LinkedHashMap<String, String> suffixMappings = new LinkedHashMap<>(legacyHistory);
        mergeCurrentSuffixes(suffixMappings, definitions.values());
        List<Suffix> suffixes = suffixMappings.entrySet().stream()
                .map(entry -> new Suffix(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt((Suffix suffix) -> suffix.value().length())
                        .reversed()
                        .thenComparing(Suffix::value))
                .toList();
        return new State(
                true,
                Collections.unmodifiableMap(definitions),
                ordered.stream().map(MaterialPrefixDefinition::prefix).toList(),
                Collections.unmodifiableMap(aliases),
                Collections.unmodifiableMap(flagBits),
                impliedClosures,
                suffixes);
    }

    private static Map<MaterialPrefix, List<MaterialPrefix>> buildImpliedClosures(
            List<MaterialPrefixDefinition> definitions,
            Map<String, MaterialPrefix> aliases) {
        LinkedHashMap<MaterialPrefix, List<MaterialPrefix>> direct = new LinkedHashMap<>();
        for (MaterialPrefixDefinition definition : definitions) {
            LinkedHashSet<MaterialPrefix> targets = new LinkedHashSet<>();
            for (String value : definition.impliedPrefixes()) {
                MaterialPrefix target = aliases.get(value);
                if (target == null && value.indexOf(':') < 0) {
                    target = aliases.get(CrucibleCraft.MODID + ":" + value);
                }
                if (target == null) {
                    throw new IllegalArgumentException(
                            "Unknown implied material prefix '" + value + "' from "
                                    + definition.prefix().id());
                }
                if (!targets.add(target)) {
                    throw new IllegalArgumentException(
                            "Duplicate implied material prefix '" + value + "' from "
                                    + definition.prefix().id());
                }
            }
            direct.put(definition.prefix(), List.copyOf(targets));
        }

        LinkedHashMap<MaterialPrefix, List<MaterialPrefix>> closures = new LinkedHashMap<>();
        for (MaterialPrefixDefinition definition : definitions) {
            resolveImpliedClosure(
                    definition.prefix(), direct, closures, new LinkedHashSet<>());
        }
        return Collections.unmodifiableMap(closures);
    }

    private static List<MaterialPrefix> resolveImpliedClosure(
            MaterialPrefix source,
            Map<MaterialPrefix, List<MaterialPrefix>> direct,
            Map<MaterialPrefix, List<MaterialPrefix>> resolved,
            LinkedHashSet<MaterialPrefix> visiting) {
        List<MaterialPrefix> cached = resolved.get(source);
        if (cached != null) {
            return cached;
        }
        if (!visiting.add(source)) {
            String path = visiting.stream()
                    .map(MaterialPrefix::id)
                    .collect(java.util.stream.Collectors.joining(" -> "));
            throw new IllegalArgumentException(
                    "Material prefix implication cycle: " + path + " -> " + source.id());
        }
        LinkedHashSet<MaterialPrefix> closure = new LinkedHashSet<>();
        for (MaterialPrefix target : direct.getOrDefault(source, List.of())) {
            closure.add(target);
            closure.addAll(resolveImpliedClosure(target, direct, resolved, visiting));
        }
        visiting.remove(source);
        List<MaterialPrefix> result = List.copyOf(closure);
        resolved.put(source, result);
        return result;
    }

    private static void addAlias(
            Map<String, MaterialPrefix> aliases,
            String alias,
            MaterialPrefix prefix) {
        MaterialPrefix previous = aliases.putIfAbsent(alias, prefix);
        if (previous != null && !previous.equals(prefix)) {
            throw new IllegalArgumentException(
                    "Material prefix alias conflict for '" + alias + "': "
                            + previous.id() + " and " + prefix.id());
        }
    }

    private static void loadBundled(
            Map<String, MaterialPrefixDefinition> output) {
        try (Reader reader = resourceReader("index.json")) {
            JsonArray index = JsonParser.parseReader(reader).getAsJsonArray();
            for (JsonElement entry : index) {
                String fileName = entry.getAsString();
                try (Reader definitionReader = resourceReader(fileName)) {
                    put(output, decodeDefinition(JsonParser.parseReader(definitionReader)),
                            "bundled/" + fileName);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load bundled material prefixes", exception);
        }
    }

    private static void loadConfig(
            Path directory,
            Map<String, MaterialPrefixDefinition> output) {
        try {
            Files.createDirectories(directory);
            try (var paths = Files.list(directory)) {
                for (Path file : paths
                        .filter(path -> path.getFileName().toString().endsWith(".json"))
                        .filter(path -> !path.getFileName().toString().equals(LEGACY_MANIFEST))
                        .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                        .toList()) {
                    try (Reader reader = Files.newBufferedReader(file)) {
                        put(output, decodeDefinition(JsonParser.parseReader(reader)),
                                "startup config " + file);
                    }
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load material prefixes from " + directory,
                    exception);
        }
    }

    /**
     * Merges every item-path-compatible current suffix into durable history.
     * Namespaced aliases contain ':' and can decode config/recipes, but cannot
     * occur inside a Minecraft item path. Hierarchical aliases containing '/'
     * are valid and intentionally retained.
     */
    private static void mergeCurrentSuffixes(
            Map<String, String> output,
            Collection<MaterialPrefixDefinition> definitions) {
        for (MaterialPrefixDefinition definition : definitions) {
            addCurrentLegacySuffix(
                    output,
                    definition.serializedPath(),
                    definition.serializedPath(),
                    "prefix " + definition.prefix().id());
            for (String alias : definition.aliases()) {
                // Aliases may preserve legacy OreDict spelling (for example,
                // dustDiv72), but only lowercase resource paths can become
                // saved-stack suffix migrations.
                if (alias.indexOf(':') < 0
                        && alias.matches("[a-z0-9][a-z0-9_./-]*")) {
                    addCurrentLegacySuffix(
                            output,
                            alias,
                            definition.serializedPath(),
                            "prefix " + definition.prefix().id());
                }
            }
        }
    }

    private static void addCurrentLegacySuffix(
            Map<String, String> output,
            String suffix,
            String canonicalPath,
            String source) {
        if (!suffix.matches("[a-z0-9][a-z0-9_./-]*")
                || !canonicalPath.matches("[a-z0-9][a-z0-9_./-]*")) {
            throw new IllegalArgumentException(
                    "Legacy prefix suffixes must be item paths in " + source);
        }
        // Existing history describes underscore-delimited ids from an older
        // catalog. Keep that meaning when a retired alias is reused by a new
        // slash-delimited canonical prefix.
        output.putIfAbsent(suffix, canonicalPath);
    }

    private static LinkedHashMap<String, String> loadLegacyManifest(Path path) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        if (!Files.exists(path)) {
            return result;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!parsed.isJsonObject()
                    || !parsed.getAsJsonObject().has("suffixes")
                    || !parsed.getAsJsonObject().get("suffixes").isJsonObject()) {
                throw new IllegalArgumentException(
                        "Legacy prefix manifest must contain an object field 'suffixes': " + path);
            }
            JsonObject suffixes = parsed.getAsJsonObject().getAsJsonObject("suffixes");
            suffixes.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> addLegacySuffix(
                            result,
                            entry.getKey(),
                            entry.getValue().getAsString(),
                            "migration manifest " + path));
            return result;
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException(
                    "Failed to load legacy prefix migration manifest " + path,
                    exception);
        }
    }

    private static void writeLegacyManifest(Path path, Map<String, String> suffixes) {
        JsonObject entries = new JsonObject();
        suffixes.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> entries.addProperty(entry.getKey(), entry.getValue()));
        JsonObject root = new JsonObject();
        root.add("suffixes", entries);
        try {
            Files.createDirectories(path.getParent());
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(temporary, GSON.toJson(root) + System.lineSeparator());
            try {
                Files.move(
                        temporary,
                        path,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to persist legacy prefix migration manifest " + path,
                    exception);
        }
    }

    private static void addLegacySuffix(
            Map<String, String> output,
            String suffix,
            String canonicalPath,
            String source) {
        if (!suffix.matches("[a-z0-9][a-z0-9_./-]*")
                || !canonicalPath.matches("[a-z0-9][a-z0-9_./-]*")) {
            throw new IllegalArgumentException(
                    "Legacy prefix suffixes must be item paths in " + source);
        }
        String previous = output.putIfAbsent(suffix, canonicalPath);
        if (previous != null && !previous.equals(canonicalPath)) {
            throw new IllegalArgumentException(
                    "Legacy prefix suffix conflict for '" + suffix + "': "
                            + previous + " and " + canonicalPath + " in " + source);
        }
    }

    private static MaterialPrefixDefinition decodeDefinition(JsonElement json) {
        return MaterialPrefixDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow(message ->
                        new IllegalArgumentException("Invalid material prefix JSON: " + message));
    }

    private static void put(
            Map<String, MaterialPrefixDefinition> output,
            MaterialPrefixDefinition definition,
            String source) {
        if (output.putIfAbsent(definition.prefix().id(), definition) != null) {
            throw new IllegalArgumentException(
                    "Duplicate material prefix " + definition.prefix().id() + " in " + source);
        }
    }

    private static Reader resourceReader(String fileName) throws IOException {
        var stream = MaterialPrefixCatalog.class.getResourceAsStream(RESOURCE_ROOT + fileName);
        if (stream == null) {
            throw new IOException("Missing prefix resource " + RESOURCE_ROOT + fileName);
        }
        return new java.io.InputStreamReader(stream, StandardCharsets.UTF_8);
    }

    private static String parseNamespaced(String value) {
        String parsed = value.indexOf(':') >= 0
                ? value
                : CrucibleCraft.MODID + ":" + value;
        if (!parsed.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("Invalid namespaced id: " + value);
        }
        return parsed;
    }

    public record Suffix(String value, String canonicalPath) {}

    private record State(
            boolean bootstrapped,
            Map<String, MaterialPrefixDefinition> definitions,
            List<MaterialPrefix> orderedPrefixes,
            Map<String, MaterialPrefix> aliases,
            Map<String, Integer> generationFlags,
            Map<MaterialPrefix, List<MaterialPrefix>> impliedClosures,
            List<Suffix> suffixes) {
        private static State empty() {
            return new State(
                    false, Map.of(), List.of(), Map.of(), Map.of(), Map.of(), List.of());
        }
    }
}
