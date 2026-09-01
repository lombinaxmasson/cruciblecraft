package com.masson.cruciblecraft.logistics.pipe;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.GT6MaterialMetadata.FluidPipeProperties;
import com.masson.cruciblecraft.material.def.GT6MaterialMetadata.ItemPipeProperties;
import com.masson.cruciblecraft.material.def.MaterialDefinition;

/**
 * Startup-frozen material/form mapping for source-backed pipes.
 *
 * <p>Fluid and item properties intentionally remain separate even when a
 * material supplies both media. They have different transfer and failure
 * semantics and are not a generic network.
 */
public final class PipeCatalog {
    public static final int MAX_RUNTIME_BLOCKS = 300;

    private static final Map<MaterialPrefix, String> FLUID_SPEC_BY_FORM =
            Map.of(
                    MaterialPrefixes.TINY_FLUID_PIPE, "pipeTiny",
                    MaterialPrefixes.SMALL_FLUID_PIPE, "pipeSmall",
                    MaterialPrefixes.FLUID_PIPE, "pipeMedium",
                    MaterialPrefixes.LARGE_FLUID_PIPE, "pipeLarge",
                    MaterialPrefixes.HUGE_FLUID_PIPE, "pipeHuge");
    private static final Map<MaterialPrefix, String> ITEM_SPEC_BY_FORM =
            Map.of(
                    MaterialPrefixes.ITEM_PIPE, "pipeMedium",
                    MaterialPrefixes.LARGE_ITEM_PIPE, "pipeLarge",
                    MaterialPrefixes.HUGE_ITEM_PIPE, "pipeHuge");

    private static volatile State state = State.empty();

    private PipeCatalog() {}

    public static synchronized void initialize(
            Collection<MaterialDefinition> definitions) {
        LinkedHashMap<String, List<MaterialPrefix>> registeredForms =
                new LinkedHashMap<>();
        definitions.forEach(material -> registeredForms.put(
                material.id(),
                MaterialCatalog.registeredForms(material)));
        initialize(definitions, registeredForms);
    }

    static synchronized void initialize(
            Collection<MaterialDefinition> definitions,
            Map<String, List<MaterialPrefix>> registeredForms) {
        if (state.initialized()) {
            throw new IllegalStateException("Pipe catalog already initialized");
        }
        LinkedHashMap<Key, Entry> entries = new LinkedHashMap<>();
        definitions.stream()
                .sorted(java.util.Comparator.comparing(MaterialDefinition::id))
                .forEach(material -> registerMaterial(
                        entries,
                        material,
                        Objects.requireNonNull(
                                registeredForms.get(material.id()),
                                () -> "Missing registered forms for "
                                        + material.id())));
        if (entries.size() > MAX_RUNTIME_BLOCKS) {
            throw new IllegalStateException(
                    "Pipe block budget exceeded: " + entries.size());
        }
        for (String material : List.of("copper", "tin", "iron")) {
            boolean fluid = entries.keySet().stream().anyMatch(
                    key -> key.materialId().equals(material)
                            && key.kind() == Kind.FLUID);
            boolean item = entries.keySet().stream().anyMatch(
                    key -> key.materialId().equals(material)
                            && key.kind() == Kind.ITEM);
            if (!fluid || !item) {
                throw new IllegalStateException(
                        "Acceptance material lacks both pipe domains: "
                                + material);
            }
        }
        List<Entry> fluid = entries.values().stream()
                .filter(entry -> entry.kind() == Kind.FLUID)
                .toList();
        List<Entry> item = entries.values().stream()
                .filter(entry -> entry.kind() == Kind.ITEM)
                .toList();
        state = new State(
                true,
                Collections.unmodifiableMap(entries),
                List.copyOf(fluid),
                List.copyOf(item));
    }

    private static void registerMaterial(
            Map<Key, Entry> entries,
            MaterialDefinition material,
            List<MaterialPrefix> registeredForms) {
        var pipeProperties = material.gt6Metadata()
                .map(metadata -> metadata.pipeProperties())
                .orElse(
                        com.masson.cruciblecraft.material.def
                                .GT6MaterialMetadata.PipeProperties.EMPTY);
        for (MaterialPrefix form : registeredForms) {
            String fluidSpec = FLUID_SPEC_BY_FORM.get(form);
            if (fluidSpec != null) {
                FluidPipeProperties properties =
                        pipeProperties.fluidBySpecification().get(fluidSpec);
                if (properties != null) {
                    put(
                            entries,
                            new Entry(
                                    material.id(),
                                    form,
                                    Kind.FLUID,
                                    fluidSpec,
                                    properties,
                                    null));
                }
            }
            String itemSpec = ITEM_SPEC_BY_FORM.get(form);
            if (itemSpec != null) {
                ItemPipeProperties properties =
                        pipeProperties.itemBySpecification().get(itemSpec);
                if (properties != null) {
                    put(
                            entries,
                            new Entry(
                                    material.id(),
                                    form,
                                    Kind.ITEM,
                                    itemSpec,
                                    null,
                                    properties));
                }
            }
        }
    }

    private static void put(Map<Key, Entry> entries, Entry entry) {
        Key key = new Key(entry.materialId(), entry.form(), entry.kind());
        if (entries.putIfAbsent(key, entry) != null) {
            throw new IllegalStateException("Duplicate pipe entry " + key);
        }
    }

    public static List<Entry> fluid() {
        return requireState().fluid();
    }

    public static List<Entry> item() {
        return requireState().item();
    }

    public static List<Entry> all() {
        return java.util.stream.Stream.concat(
                fluid().stream(), item().stream()).toList();
    }

    public static Entry require(
            String materialId, MaterialPrefix form, Kind kind) {
        Entry entry = requireState().entries().get(
                new Key(materialId, form, kind));
        if (entry == null) {
            throw new IllegalArgumentException(
                    "No " + kind.name().toLowerCase(java.util.Locale.ROOT)
                            + " pipe for " + materialId + "/"
                            + form.serializedName());
        }
        return entry;
    }

    public static boolean contains(
            String materialId, MaterialPrefix form, Kind kind) {
        return requireState().entries().containsKey(
                new Key(materialId, form, kind));
    }

    /**
     * Resolves either a registered output-form key or its pinned GT6
     * specification key. This is the single gauge mapping used by runtime pipe
     * registration and MaterialRule predicates.
     */
    public static String requireSpecification(
            Kind kind, String outputOrSpecification) {
        Objects.requireNonNull(kind, "kind");
        if (outputOrSpecification == null
                || outputOrSpecification.isBlank()) {
            throw new IllegalArgumentException(
                    "Pipe output/specification key must not be blank");
        }
        Map<MaterialPrefix, String> specifications =
                kind == Kind.FLUID
                        ? FLUID_SPEC_BY_FORM
                        : ITEM_SPEC_BY_FORM;
        return specifications.entrySet().stream()
                .filter(entry ->
                        entry.getKey().serializedName().equals(
                                outputOrSpecification)
                                || entry.getValue().equals(
                                        outputOrSpecification))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown "
                                + kind.name().toLowerCase(
                                        java.util.Locale.ROOT)
                                + " pipe output/specification key "
                                + outputOrSpecification));
    }

    public static boolean recipeEnabled(
            MaterialDefinition material,
            Kind kind,
            String outputOrSpecification) {
        Objects.requireNonNull(material, "material");
        String specification =
                requireSpecification(kind, outputOrSpecification);
        return material.gt6Metadata()
                .map(metadata -> kind == Kind.FLUID
                        ? java.util.Optional.ofNullable(
                                        metadata.pipeProperties()
                                                .fluidBySpecification()
                                                .get(specification))
                                .map(FluidPipeProperties::recipe)
                                .orElse(false)
                        : java.util.Optional.ofNullable(
                                        metadata.pipeProperties()
                                                .itemBySpecification()
                                                .get(specification))
                                .map(ItemPipeProperties::recipe)
                                .orElse(false))
                .orElse(false);
    }

    public static boolean isInitialized() {
        return state.initialized();
    }

    private static State requireState() {
        State snapshot = state;
        if (!snapshot.initialized()) {
            throw new IllegalStateException("Pipe catalog is not initialized");
        }
        return snapshot;
    }

    public enum Kind {
        FLUID,
        ITEM
    }

    public record Key(
            String materialId, MaterialPrefix form, Kind kind) {
        public Key {
            Objects.requireNonNull(materialId, "materialId");
            Objects.requireNonNull(form, "form");
            Objects.requireNonNull(kind, "kind");
        }
    }

    public record Entry(
            String materialId,
            MaterialPrefix form,
            Kind kind,
            String sourceSpecification,
            FluidPipeProperties fluid,
            ItemPipeProperties item) {
        public Entry {
            Objects.requireNonNull(materialId, "materialId");
            Objects.requireNonNull(form, "form");
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(
                    sourceSpecification, "sourceSpecification");
            if ((kind == Kind.FLUID) != (fluid != null)
                    || (kind == Kind.ITEM) != (item != null)) {
                throw new IllegalArgumentException(
                        "Pipe entry property kind mismatch");
            }
            String expected = (kind == Kind.FLUID
                    ? FLUID_SPEC_BY_FORM
                    : ITEM_SPEC_BY_FORM).get(form);
            if (!sourceSpecification.equals(expected)) {
                throw new IllegalArgumentException(
                        "Pipe form/specification mismatch");
            }
        }

        public String registryName() {
            return materialId + "/" + form.serializedName();
        }

        public int width() {
            return switch (form.serializedName()) {
                case "tiny_fluid_pipe" -> 4;
                case "small_fluid_pipe" -> 6;
                case "fluid_pipe", "item_pipe" -> 8;
                case "large_fluid_pipe", "large_item_pipe" -> 12;
                case "huge_fluid_pipe", "huge_item_pipe" -> 16;
                default -> throw new IllegalStateException(
                        "Unsupported pipe form " + form.serializedName());
            };
        }
    }

    private record State(
            boolean initialized,
            Map<Key, Entry> entries,
            List<Entry> fluid,
            List<Entry> item) {
        private static State empty() {
            return new State(false, Map.of(), List.of(), List.of());
        }
    }
}
