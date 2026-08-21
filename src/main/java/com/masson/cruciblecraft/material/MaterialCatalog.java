package com.masson.cruciblecraft.material;

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.material.def.MaterialLoader;
import com.masson.cruciblecraft.material.def.MaterialTuning;
import com.masson.cruciblecraft.recipe.AlloyIndex;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public final class MaterialCatalog {
    private static final Set<String> REQUIRED_MATERIALS =
            Set.of("bronze", "carbon", "ceramic", "iron", "steel");
    private static volatile State state = State.empty();
    private static volatile RuntimeState runtime = RuntimeState.empty();
    private static final Map<String, MaterialDefinition> startupAdditions = new LinkedHashMap<>();
    private static final Map<String, MaterialTuning> startupTunings = new LinkedHashMap<>();

    private MaterialCatalog() {}

    public static synchronized void bootstrap(Path configDirectory) {
        if (state.bootstrapped()) {
            throw new IllegalStateException("Material catalog already bootstrapped");
        }
        Map<String, MaterialDefinition> definitions = MaterialLoader.load(
                configDirectory,
                startupAdditions.values(),
                startupTunings.values());
        validateRequiredMaterials(definitions);
        Map<String, java.util.List<MaterialPrefix>> registeredForms =
                MaterialRegistrationGate.load(definitions.values());
        validateRegistryNames(definitions.values(), registeredForms);
        DecompositionResolver decomposition = new DecompositionResolver(definitions);
        LinkedHashMap<String, DecompositionInfo> info = new LinkedHashMap<>();
        for (MaterialDefinition definition : definitions.values()) {
            int quantum = decomposition.quantum(definition);
            info.put(
                    definition.id(),
                    new DecompositionInfo(
                            quantum,
                            decomposition.ratio(definition)));
        }
        State published = new State(
                true,
                definitions,
                info,
                new AlloyIndex(
                        definitions.values(),
                        definition -> decomposition.decompose(
                                definition,
                                MaterialPrefixes.INGOT.units()),
                        definition -> registeredForms.get(definition.id())
                                .contains(MaterialPrefixes.INGOT)),
                buildCanonicalItemMappings(definitions.values(), registeredForms),
                registeredForms,
                buildPrefixIndex(definitions.values(), registeredForms),
                buildPrefixIndex(definitions.values()));
        startupAdditions.clear();
        startupTunings.clear();
        state = published;
        runtime = RuntimeState.from(
                published.definitions(), published.alloyIndex(), Map.of(), 0L);
        MaterialByproductIndex.publish(published.definitions().values(), 0L);
    }

    /**
     * Adds a startup-only material before item registration. Existing ids are
     * deliberately not replaceable; removal and replacement are migration hazards.
     *
     * <p>This is the preferred addon API. Call it from startup code that has an
     * explicit load-order dependency before CrucibleCraft bootstraps its catalog.
     */
    public static synchronized boolean addStartupMaterial(MaterialDefinition definition) {
        if (state.bootstrapped()) {
            throw new IllegalStateException("Materials can only be added before bootstrap");
        }
        return startupAdditions.putIfAbsent(definition.id(), definition) == null;
    }

    /**
     * Queues a non-structural override. Unknown ids are ignored during bootstrap;
     * this API can never add registry entries or forms.
     */
    public static synchronized boolean addStartupTuning(MaterialTuning tuning) {
        if (state.bootstrapped()) {
            throw new IllegalStateException("Materials can only be tuned before bootstrap");
        }
        return startupTunings.putIfAbsent(tuning.id(), tuning) == null;
    }

    public static Collection<MaterialDefinition> values() {
        requireState();
        return requireRuntime().definitions().values();
    }

    /** Immutable structural prefix index used by bounded declarative rule expansion. */
    public static Map<MaterialPrefix, java.util.List<MaterialDefinition>> prefixIndex() {
        return requireState().prefixIndex();
    }

    /** Factual GT6 prefix index, independent of the committed item registration gate. */
    public static Map<MaterialPrefix, java.util.List<MaterialDefinition>> factualPrefixIndex() {
        return requireState().factualPrefixIndex();
    }

    /** Forms with actual Item registrations in this build. */
    public static java.util.List<MaterialPrefix> registeredForms(MaterialDefinition material) {
        java.util.List<MaterialPrefix> forms =
                requireState().registeredForms().get(material.id());
        if (forms == null) {
            throw new IllegalArgumentException("Unknown material: " + material.id());
        }
        return forms;
    }

    public static java.util.List<MaterialPrefix> registeredForms(String materialId) {
        java.util.List<MaterialPrefix> forms =
                requireState().registeredForms().get(materialId);
        if (forms == null) {
            throw new IllegalArgumentException("Unknown material: " + materialId);
        }
        return forms;
    }

    public static boolean isFormRegistered(
            MaterialDefinition material, MaterialPrefix prefix) {
        return registeredForms(material).contains(prefix);
    }

    public static boolean isBootstrapped() {
        return state.bootstrapped();
    }

    static synchronized void resetForTests() {
        state = State.empty();
        runtime = RuntimeState.empty();
        startupAdditions.clear();
        startupTunings.clear();
    }

    /** O(1) membership check for persisted and integration-provided material ids. */
    public static boolean contains(String id) {
        requireState();
        return requireRuntime().definitions().containsKey(id);
    }

    public static MaterialDefinition require(String id) {
        requireState();
        MaterialDefinition definition = requireRuntime().definitions().get(id);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown material: " + id);
        }
        return definition;
    }

    public static Optional<MaterialDefinition> find(String id) {
        requireState();
        return Optional.ofNullable(requireRuntime().definitions().get(id));
    }

    public static Map<String, Integer> decompose(MaterialDefinition material, int units) {
        State snapshot = requireState();
        DecompositionInfo cached = snapshot.decompositionInfo().get(material.id());
        MaterialDefinition structural = snapshot.definitions().get(material.id());
        if (cached != null
                && structural.composition().equals(material.composition())
                && structural.noDecompose() == material.noDecompose()) {
            return scaleRatio(material.id(), units, cached.quantum(), cached.ratio());
        }
        return decompose(material, units, snapshot.definitions());
    }

    static Map<String, Integer> decompose(
            MaterialDefinition material,
            int units,
            Map<String, MaterialDefinition> definitions) {
        return new DecompositionResolver(definitions).decompose(material, units);
    }

    static Map<String, Integer> calculateQuanta(
            Map<String, MaterialDefinition> definitions) {
        DecompositionResolver decomposition = new DecompositionResolver(definitions);
        LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
        for (MaterialDefinition definition : definitions.values()) {
            result.put(definition.id(), decomposition.quantum(definition));
        }
        return Map.copyOf(result);
    }

    /**
     * Smallest positive amount that can be recursively decomposed without fractions.
     * This is computed and validated once after the catalog becomes immutable.
     */
    public static int decompositionQuantum(String materialId) {
        return decompositionInfo(materialId).quantum();
    }

    public static int decompositionQuantum(MaterialDefinition material) {
        return decompositionQuantum(material.id());
    }

    /**
     * Exact recursively flattened ratio for one decomposition quantum.
     * The returned immutable map is cached and reference-stable after bootstrap.
     */
    public static Map<String, Integer> decompositionRatio(String materialId) {
        return decompositionInfo(materialId).ratio();
    }

    public static Map<String, Integer> decompositionRatio(MaterialDefinition material) {
        return decompositionRatio(material.id());
    }

    public static AlloyIndex alloys() {
        requireState();
        return requireRuntime().alloyIndex();
    }

    /** Startup structural definitions before reloadable tuning is layered. */
    public static Collection<MaterialDefinition> startupValues() {
        return requireState().definitions().values();
    }

    /**
     * Atomically publishes the complete recipe-backed runtime metadata snapshot.
     * Every call rebuilds from startup definitions, so absent tunings reset.
     */
    public static synchronized void publishRuntime(
            Collection<MaterialTuning> tunings,
            Map<String, String> unificationPreferences) {
        publishPreview(previewRuntime(tunings, unificationPreferences));
    }

    /**
     * Builds a complete effective snapshot without publishing it. Reload
     * pipelines use this to expand and validate recipes against tuned values
     * while the current runtime remains untouched.
     */
    public static synchronized RuntimePreview previewRuntime(
            Collection<MaterialTuning> tunings,
            Map<String, String> unificationPreferences) {
        State structural = requireState();
        long baseRevision = requireRuntime().revision();
        LinkedHashMap<String, MaterialDefinition> effective =
                new LinkedHashMap<>(structural.definitions());
        for (MaterialTuning tuning : tunings) {
            MaterialDefinition base = effective.get(tuning.id());
            if (base == null) {
                throw new IllegalArgumentException(
                        "Cannot tune unknown material " + tuning.id());
            }
            effective.put(tuning.id(), tuning.apply(base));
        }
        Map<String, MaterialDefinition> frozen = java.util.Collections.unmodifiableMap(effective);
        DecompositionResolver decomposition = new DecompositionResolver(frozen);
        AlloyIndex alloys = new AlloyIndex(
                frozen.values(),
                definition -> decomposition.decompose(
                        definition,
                        MaterialPrefixes.INGOT.units()),
                definition -> isFormRegistered(definition, MaterialPrefixes.INGOT));
        return new RuntimePreview(
                frozen,
                alloys,
                Map.copyOf(unificationPreferences),
                baseRevision,
                Math.incrementExact(baseRevision));
    }

    /** Publishes a previously validated preview, rejecting stale previews. */
    public static synchronized void publishPreview(RuntimePreview preview) {
        publishPreview(preview, () -> {});
    }

    /**
     * Holds the material publication monitor while a validated dependent
     * snapshot is installed, preventing an unrelated tuning publisher from
     * interleaving with the central recipe epoch.
     */
    public static synchronized void publishPreview(
            RuntimePreview preview, Runnable dependentPublication) {
        if (requireRuntime().revision() != preview.baseRevision()) {
            throw new IllegalStateException("Stale material runtime preview");
        }
        runtime = RuntimeState.from(
                preview.definitions(),
                preview.alloyIndex(),
                preview.unificationPreferences(),
                preview.revision());
        MaterialByproductIndex.publish(preview.definitions().values(), preview.revision());
        dependentPublication.run();
    }

    public static long runtimeRevision() {
        requireState();
        return requireRuntime().revision();
    }

    public record RuntimePreview(
            Map<String, MaterialDefinition> definitions,
            AlloyIndex alloyIndex,
            Map<String, String> unificationPreferences,
            long baseRevision,
            long revision) {
        public RuntimePreview {
            definitions = java.util.Collections.unmodifiableMap(
                    new LinkedHashMap<>(definitions));
            unificationPreferences = Map.copyOf(unificationPreferences);
        }
    }

    public static Optional<String> preferredItem(String materialId, MaterialPrefix prefix) {
        requireState();
        return Optional.ofNullable(
                requireRuntime().unificationPreferences().get(
                        materialId + "/" + prefix.serializedId()));
    }

    /** Immutable published preference snapshot for normal runtime resolution. */
    public static Map<String, String> runtimePreferences() {
        requireState();
        return requireRuntime().unificationPreferences();
    }

    static Map<String, String> canonicalItemMappings() {
        return requireState().canonicalItemMappings();
    }

    private static Map<String, Integer> scaleRatio(
            String materialId,
            int units,
            int quantum,
            Map<String, Integer> ratio) {
        if (units % quantum != 0) {
            throw new IllegalArgumentException(
                    materialId + " cannot decompose " + units + " units exactly");
        }
        int factor = units / quantum;
        LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
        try {
            ratio.forEach((component, amount) ->
                    result.put(component, Math.multiplyExact(amount, factor)));
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "Exact decomposition exceeds integer range for " + materialId,
                    exception);
        }
        return Map.copyOf(result);
    }

    private static long ratioTotal(MaterialDefinition material) {
        long total = 0L;
        try {
            for (int ratio : material.composition().values()) {
                total = Math.addExact(total, ratio);
            }
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "Composition ratio total is too large for " + material.id(),
                    exception);
        }
        return total;
    }

    private static IllegalArgumentException cyclicComposition(
            Set<String> visiting,
            String repeatedId) {
        StringBuilder path = new StringBuilder();
        boolean inCycle = false;
        for (String id : visiting) {
            if (id.equals(repeatedId)) {
                inCycle = true;
            }
            if (inCycle) {
                if (!path.isEmpty()) {
                    path.append(" -> ");
                }
                path.append(id);
            }
        }
        if (!path.isEmpty()) {
            path.append(" -> ");
        }
        path.append(repeatedId);
        return new IllegalArgumentException("Cyclic composition: " + path);
    }

    private static final class DecompositionResolver {
        private final Map<String, MaterialDefinition> definitions;
        private final Map<String, Integer> quantumMemo = new LinkedHashMap<>();
        private final Map<String, Map<String, Integer>> ratioMemo = new LinkedHashMap<>();
        private final Set<String> quantumVisiting = new LinkedHashSet<>();
        private final Set<String> ratioVisiting = new LinkedHashSet<>();

        private DecompositionResolver(Map<String, MaterialDefinition> definitions) {
            this.definitions = definitions;
        }

        private int quantum(MaterialDefinition material) {
            Integer cached = quantumMemo.get(material.id());
            if (cached != null) {
                return cached;
            }
            if (material.composition().isEmpty() || material.noDecompose()) {
                quantumMemo.put(material.id(), 1);
                return 1;
            }
            if (!quantumVisiting.add(material.id())) {
                throw cyclicComposition(quantumVisiting, material.id());
            }
            try {
                long total = ratioTotal(material);
                long quantum = 1L;
                for (var component : material.composition().entrySet()) {
                    MaterialDefinition child = requireComponent(component.getKey());
                    long childQuantum = quantum(child);
                    long required;
                    try {
                        required = Math.multiplyExact(total, childQuantum);
                        required /= gcd(required, component.getValue());
                        quantum = lcm(quantum, required);
                    } catch (ArithmeticException exception) {
                        throw new IllegalArgumentException(
                                "Exact decomposition quantum is too large for " + material.id(),
                                exception);
                    }
                    if (quantum > Integer.MAX_VALUE) {
                        throw new IllegalArgumentException(
                                "Exact decomposition quantum is too large for " + material.id());
                    }
                }
                int result = (int) quantum;
                quantumMemo.put(material.id(), result);
                return result;
            } finally {
                quantumVisiting.remove(material.id());
            }
        }

        private Map<String, Integer> ratio(MaterialDefinition material) {
            Map<String, Integer> cached = ratioMemo.get(material.id());
            if (cached != null) {
                return cached;
            }
            if (material.composition().isEmpty() || material.noDecompose()) {
                Map<String, Integer> result = Map.of(material.id(), 1);
                ratioMemo.put(material.id(), result);
                return result;
            }
            if (!ratioVisiting.add(material.id())) {
                throw cyclicComposition(ratioVisiting, material.id());
            }
            try {
                int materialQuantum = quantum(material);
                long total = ratioTotal(material);
                LinkedHashMap<String, Integer> flattened = new LinkedHashMap<>();
                for (var component : material.composition().entrySet()) {
                    MaterialDefinition child = requireComponent(component.getKey());
                    long numerator;
                    try {
                        numerator = Math.multiplyExact(
                                (long) materialQuantum,
                                (long) component.getValue());
                    } catch (ArithmeticException exception) {
                        throw new IllegalArgumentException(
                                "Exact decomposition exceeds long range for " + material.id(),
                                exception);
                    }
                    if (numerator % total != 0L) {
                        throw new IllegalArgumentException(
                                material.id() + " cannot decompose "
                                        + materialQuantum + " units exactly");
                    }
                    int childUnits;
                    try {
                        childUnits = Math.toIntExact(numerator / total);
                    } catch (ArithmeticException exception) {
                        throw new IllegalArgumentException(
                                "Exact decomposition exceeds integer range for " + material.id(),
                                exception);
                    }
                    int childQuantum = quantum(child);
                    if (childUnits % childQuantum != 0) {
                        throw new IllegalArgumentException(
                                material.id() + " cannot decompose "
                                        + materialQuantum + " units exactly");
                    }
                    int factor = childUnits / childQuantum;
                    for (var terminal : ratio(child).entrySet()) {
                        try {
                            int amount = Math.multiplyExact(terminal.getValue(), factor);
                            flattened.merge(terminal.getKey(), amount, Math::addExact);
                        } catch (ArithmeticException exception) {
                            throw new IllegalArgumentException(
                                    "Exact decomposition exceeds integer range for "
                                            + material.id(),
                                    exception);
                        }
                    }
                }
                Map<String, Integer> result = Map.copyOf(flattened);
                ratioMemo.put(material.id(), result);
                return result;
            } finally {
                ratioVisiting.remove(material.id());
            }
        }

        private Map<String, Integer> decompose(MaterialDefinition material, int units) {
            int materialQuantum = quantum(material);
            return scaleRatio(material.id(), units, materialQuantum, ratio(material));
        }

        private MaterialDefinition requireComponent(String materialId) {
            MaterialDefinition child = definitions.get(materialId);
            if (child == null) {
                throw new IllegalArgumentException("Unknown material: " + materialId);
            }
            return child;
        }
    }

    private static long gcd(long left, long right) {
        left = Math.abs(left);
        right = Math.abs(right);
        while (right != 0) {
            long remainder = left % right;
            left = right;
            right = remainder;
        }
        return left;
    }

    private static long lcm(long left, long right) {
        return Math.multiplyExact(left / gcd(left, right), right);
    }

    private static State requireState() {
        State snapshot = state;
        if (!snapshot.bootstrapped()) {
            throw new IllegalStateException("Material catalog has not been bootstrapped");
        }
        return snapshot;
    }

    private static RuntimeState requireRuntime() {
        RuntimeState snapshot = runtime;
        if (!snapshot.published()) {
            throw new IllegalStateException("Material runtime snapshot has not been published");
        }
        return snapshot;
    }

    private static DecompositionInfo decompositionInfo(String materialId) {
        DecompositionInfo info = requireState().decompositionInfo().get(materialId);
        if (info == null) {
            throw new IllegalArgumentException("Unknown material: " + materialId);
        }
        return info;
    }

    private static void validateRequiredMaterials(
            Map<String, MaterialDefinition> definitions) {
        Set<String> missing = REQUIRED_MATERIALS.stream()
                .filter(id -> !definitions.containsKey(id))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "Material catalog is missing required machine materials: " + missing);
        }
    }

    static void validateRegistryNames(Collection<MaterialDefinition> definitions) {
        LinkedHashMap<String, java.util.List<MaterialPrefix>> allForms =
                new LinkedHashMap<>();
        definitions.forEach(definition ->
                allForms.put(definition.id(), definition.forms()));
        validateRegistryNames(definitions, allForms);
    }

    private static void validateRegistryNames(
            Collection<MaterialDefinition> definitions,
            Map<String, java.util.List<MaterialPrefix>> registeredForms) {
        LinkedHashMap<String, String> owners = new LinkedHashMap<>();
        for (MaterialDefinition material : definitions) {
            for (MaterialPrefix prefix : registeredForms.get(material.id())) {
                String registryName = registeredItemName(material, prefix);
                String owner = material.id() + "/" + prefix.serializedName();
                String previous = owners.putIfAbsent(registryName, owner);
                if (previous != null) {
                    throw new IllegalStateException(
                            "Duplicate material item registry name '"
                                    + registryName + "' for " + previous + " and " + owner);
                }
            }
        }
    }

    /**
     * Validates registry-backed overrides after every mod's RegisterEvent has run.
     * Calling this during FMLConstructModEvent would reject valid deferred entries.
     */
    public static void validateFormItemMappings() {
        validateFormItemMappings(requireState().definitions().values());
    }

    static void validateFormItemMappings(
            Collection<MaterialDefinition> definitions) {
        for (MaterialDefinition material : definitions) {
            for (var override : material.formItems().entrySet()) {
                ResourceLocation itemId = ResourceLocation.tryParse(override.getValue());
                if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
                    throw new IllegalStateException(
                            "Material " + material.id() + " prefix "
                                    + override.getKey().serializedId()
                                    + " declares missing form_items target "
                                    + override.getValue()
                                    + "; install the providing mod or remove the override");
                }
            }
        }
    }

    private static Map<String, String> buildCanonicalItemMappings(
            Collection<MaterialDefinition> definitions,
            Map<String, java.util.List<MaterialPrefix>> registeredForms) {
        LinkedHashMap<String, String> mappings = new LinkedHashMap<>();
        for (MaterialDefinition material : definitions) {
            for (MaterialPrefix form : registeredForms.get(material.id())) {
                String override = material.formItems().get(form);
                String itemId;
                if (form.equals(MaterialPrefixes.ORE)) {
                    itemId = "cruciblecraft:" + registeredItemName(material, form);
                } else {
                    itemId = override == null
                            ? "cruciblecraft:" + registeredItemName(material, form)
                            : override;
                }
                mappings.put(
                        material.id() + "/" + form.serializedName(),
                        itemId);
            }
        }
        return java.util.Collections.unmodifiableMap(mappings);
    }

    private static String registeredItemName(
            MaterialDefinition material, MaterialPrefix prefix) {
        return prefix.equals(MaterialPrefixes.ORE)
                ? material.id() + "_ore"
                : material.registryName(prefix);
    }

    private static Map<MaterialPrefix, java.util.List<MaterialDefinition>> buildPrefixIndex(
            Collection<MaterialDefinition> definitions) {
        LinkedHashMap<String, java.util.List<MaterialPrefix>> allForms =
                new LinkedHashMap<>();
        definitions.forEach(definition ->
                allForms.put(definition.id(), definition.forms()));
        return buildPrefixIndex(definitions, allForms);
    }

    private static Map<MaterialPrefix, java.util.List<MaterialDefinition>> buildPrefixIndex(
            Collection<MaterialDefinition> definitions,
            Map<String, java.util.List<MaterialPrefix>> formsByMaterial) {
        LinkedHashMap<MaterialPrefix, java.util.List<MaterialDefinition>> mutable =
                new LinkedHashMap<>();
        for (MaterialDefinition definition : definitions) {
            for (MaterialPrefix prefix : formsByMaterial.get(definition.id())) {
                mutable.computeIfAbsent(prefix, ignored -> new java.util.ArrayList<>())
                        .add(definition);
            }
        }
        LinkedHashMap<MaterialPrefix, java.util.List<MaterialDefinition>> result =
                new LinkedHashMap<>();
        mutable.forEach((prefix, values) -> result.put(prefix, java.util.List.copyOf(values)));
        return java.util.Collections.unmodifiableMap(result);
    }

    private record DecompositionInfo(int quantum, Map<String, Integer> ratio) {
        private DecompositionInfo {
            ratio = Map.copyOf(ratio);
        }
    }

    private record State(
            boolean bootstrapped,
            Map<String, MaterialDefinition> definitions,
            Map<String, DecompositionInfo> decompositionInfo,
            AlloyIndex alloyIndex,
            Map<String, String> canonicalItemMappings,
            Map<String, java.util.List<MaterialPrefix>> registeredForms,
            Map<MaterialPrefix, java.util.List<MaterialDefinition>> prefixIndex,
            Map<MaterialPrefix, java.util.List<MaterialDefinition>> factualPrefixIndex) {
        private State {
            definitions = java.util.Collections.unmodifiableMap(
                    new LinkedHashMap<>(definitions));
            decompositionInfo = java.util.Collections.unmodifiableMap(
                    new LinkedHashMap<>(decompositionInfo));
            canonicalItemMappings = java.util.Collections.unmodifiableMap(
                    new LinkedHashMap<>(canonicalItemMappings));
            registeredForms = java.util.Collections.unmodifiableMap(
                    new LinkedHashMap<>(registeredForms));
            prefixIndex = java.util.Collections.unmodifiableMap(
                    new LinkedHashMap<>(prefixIndex));
            factualPrefixIndex = java.util.Collections.unmodifiableMap(
                    new LinkedHashMap<>(factualPrefixIndex));
        }

        private static State empty() {
            return new State(
                    false,
                    Map.of(),
                    Map.of(),
                    AlloyIndex.empty(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Map.of());
        }
    }

    private record RuntimeState(
            boolean published,
            Map<String, MaterialDefinition> definitions,
            AlloyIndex alloyIndex,
            Map<String, String> unificationPreferences,
            long revision) {
        private static RuntimeState from(
                Map<String, MaterialDefinition> definitions,
                AlloyIndex alloyIndex,
                Map<String, String> preferences,
                long revision) {
            return new RuntimeState(
                    true,
                    java.util.Collections.unmodifiableMap(new LinkedHashMap<>(definitions)),
                    alloyIndex,
                    java.util.Collections.unmodifiableMap(new LinkedHashMap<>(preferences)),
                    revision);
        }

        private static RuntimeState empty() {
            return new RuntimeState(false, Map.of(), AlloyIndex.empty(), Map.of(), 0L);
        }
    }
}
