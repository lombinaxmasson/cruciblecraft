package com.masson.cruciblecraft.recipe.gt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.masson.cruciblecraft.recipe.rule.MaterialRule;
import com.masson.cruciblecraft.recipe.rule.MaterialRuleExpansion;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Production T14c Hybrid provider for the compact Extruder family.
 *
 * <p>The immutable relation/index snapshot is installed with its RecipeMap
 * epoch. One fifth of relations are eager; the long tail materializes through
 * a bounded access-order cache. The built-in datapack supplies the full
 * 20-source/2,782-relation baseline, while reloads may legally supply any
 * subset within those ceilings. Enumeration is complete for the actual loaded
 * set but does not retain long-tail rows.
 */
public final class ExtruderRecipeFamilyProvider {
    public static final String FAMILY_ID = "t14_extruder";
    public static final int LOGICAL_RELATIONS = 2782;
    public static final int AUTHORED_ENTRIES = 20;
    public static final int HOT_MODULO = 5;
    public static final int CACHE_CEILING = 512;
    private static final ResourceLocation TARGET =
            ResourceLocation.fromNamespaceAndPath("cruciblecraft", "extruder");

    private ExtruderRecipeFamilyProvider() {}

    public static Snapshot prepare(
            List<Source> sources,
            Collection<MaterialDefinition> availableMaterials,
            Map<String, String> candidatePreferences,
            MaterialRuleExpansion.FormIndexes formIndexes,
            long epoch,
            RuntimeSide side) {
        Objects.requireNonNull(candidatePreferences, "candidatePreferences");
        Map<String, String> preferences = Map.copyOf(candidatePreferences);
        return prepare(
                sources,
                availableMaterials,
                MaterialRuleExpansion.candidateResolver(preferences),
                formIndexes,
                epoch,
                side);
    }

    static Snapshot prepare(
            List<Source> sources,
            Collection<MaterialDefinition> availableMaterials,
            MaterialRuleExpansion.ResourceResolver resolver,
            MaterialRuleExpansion.FormIndexes formIndexes,
            long epoch,
            RuntimeSide side) {
        Objects.requireNonNull(sources, "sources");
        Objects.requireNonNull(availableMaterials, "availableMaterials");
        Objects.requireNonNull(resolver, "resolver");
        Objects.requireNonNull(formIndexes, "formIndexes");
        Objects.requireNonNull(side, "side");
        if (side == RuntimeSide.INTEGRATED_CLIENT) {
            throw new IllegalArgumentException(
                    "Integrated clients must reuse the server Extruder snapshot");
        }
        if (epoch <= 0L) {
            throw new IllegalArgumentException(
                    "Extruder family epoch must be positive");
        }
        if (sources.size() > AUTHORED_ENTRIES) {
            String overflow = sources.get(AUTHORED_ENTRIES).id().toString();
            throw new IllegalArgumentException(
                    "T14c compact source ceiling is 20, found "
                            + sources.size() + "; first overflow source "
                            + overflow);
        }

        List<MaterialDefinition> materials = List.copyOf(availableMaterials);
        Map<String, MaterialDefinition> materialsById = new HashMap<>();
        for (MaterialDefinition material : materials) {
            MaterialDefinition duplicate =
                    materialsById.putIfAbsent(material.id(), material);
            if (duplicate != null) {
                throw new IllegalArgumentException(
                        "Duplicate material definition " + material.id());
            }
        }
        Set<ResourceLocation> authoredIds = new HashSet<>();
        List<Definition> definitions = new ArrayList<>(LOGICAL_RELATIONS);
        for (Source source : sources) {
            if (!authoredIds.add(source.id())) {
                throw new IllegalArgumentException(
                        "Duplicate compact authored id " + source.id());
            }
            MaterialRule rule = source.rule();
            if (!rule.target().orElseThrow().equals(TARGET)
                    || rule.sparse().isEmpty()) {
                throw new IllegalArgumentException(
                        "T14c compact source " + source.id()
                                + " is not an Extruder sparse rule");
            }
            MaterialRule.SparseTable table = rule.sparse().orElseThrow();
            if (BuiltInRegistries.ITEM.getOptional(table.shapeItem()).isEmpty()) {
                throw new IllegalArgumentException(
                        "T14c compact source " + source.id()
                                + " has unknown Extruder shape item "
                                + table.shapeItem());
            }
            for (MaterialRule.SparseRelation relation : table.relations()) {
                try {
                    MaterialDefinition material = Optional.ofNullable(
                                    materialsById.get(relation.material()))
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "unknown relation material "
                                            + relation.material()));
                    var inputPrefix = MaterialPrefixCatalog.require(
                            relation.input().prefix());
                    var outputPrefix = MaterialPrefixCatalog.require(
                            relation.output().prefix());
                    if (!formIndexes.registered()
                                    .getOrDefault(inputPrefix, List.of())
                                    .contains(material)
                            || !formIndexes.registered()
                                    .getOrDefault(outputPrefix, List.of())
                                    .contains(material)) {
                        throw new IllegalArgumentException(
                                "references an unavailable registered form");
                    }
                    Ingredient ingredient = resolver.itemInput(
                                    material,
                                    Optional.of(inputPrefix),
                                    Optional.empty())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "cannot resolve compact input"));
                    Set<Item> inputItems = new HashSet<>();
                    for (ItemStack stack : ingredient.getItems()) {
                        if (!stack.isEmpty()) {
                            inputItems.add(stack.getItem());
                        }
                    }
                    if (inputItems.isEmpty()) {
                        throw new IllegalArgumentException(
                                "compact input resolved no indexed items");
                    }
                    resolver.itemOutput(
                                    material,
                                    Optional.of(outputPrefix),
                                    Optional.empty())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "cannot resolve compact output"));
                    if (definitions.size() >= LOGICAL_RELATIONS) {
                        throw new IllegalArgumentException(
                                "exceeds the 2782 relation ceiling");
                    }
                    definitions.add(new Definition(
                            source,
                            relation,
                            Set.copyOf(inputItems)));
                } catch (IllegalArgumentException exception) {
                    throw relationError(source, relation, exception);
                }
            }
        }
        definitions.sort(Comparator
                .comparingInt((Definition value) ->
                        value.relation().shadowOrder())
                .thenComparing(value ->
                        value.relation().stableId().toString()));
        validateRelations(definitions);
        return new Snapshot(
                epoch,
                side,
                definitions,
                materials,
                resolver,
                formIndexes);
    }

    private static void validateRelations(List<Definition> definitions) {
        Set<ResourceLocation> stableIds = new HashSet<>();
        Set<String> signatures = new HashSet<>();
        Definition previous = null;
        for (Definition definition : definitions) {
            MaterialRule.SparseRelation relation = definition.relation();
            if (previous != null
                    && relation.shadowOrder()
                            <= previous.relation().shadowOrder()) {
                throw new IllegalArgumentException(
                        "T14c compact shadow_order must be strictly increasing "
                                + "after sorting: "
                                + describe(previous) + " conflicts with "
                                + describe(definition));
            }
            if (!stableIds.add(relation.stableId())) {
                throw new IllegalArgumentException(
                        "Duplicate compact stable id in "
                                + describe(definition));
            }
            MaterialRule.SparseTable table =
                    definition.source().rule().sparse().orElseThrow();
            String signature = relation.material()
                    + "|" + relation.input().prefix()
                    + "|" + table.shapeItem();
            if (!signatures.add(signature)) {
                throw new IllegalArgumentException(
                        "Duplicate compact input signature in "
                                + describe(definition) + ": " + signature);
            }
            previous = definition;
        }
    }

    private static IllegalArgumentException relationError(
            Source source,
            MaterialRule.SparseRelation relation,
            IllegalArgumentException cause) {
        return new IllegalArgumentException(
                "Invalid T14c compact source " + source.id()
                        + ", relation " + relation.stableId()
                        + ": " + cause.getMessage(),
                cause);
    }

    private static String describe(Definition definition) {
        return "source " + definition.source().id()
                + ", relation " + definition.relation().stableId()
                + ", shadow_order "
                + definition.relation().shadowOrder();
    }

    public enum RuntimeSide {
        SERVER,
        DEDICATED_CLIENT,
        INTEGRATED_CLIENT
    }

    public record Source(ResourceLocation id, MaterialRule rule) {
        public Source {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(rule, "rule");
        }
    }

    public static final class Snapshot implements RecipeMap.RecipeFamily {
        private final long epoch;
        private final RuntimeSide side;
        private final List<Definition> definitions;
        private final List<MaterialDefinition> materials;
        private final MaterialRuleExpansion.ResourceResolver resolver;
        private final MaterialRuleExpansion.FormIndexes formIndexes;
        private final List<ResourceLocation> recipeIds;
        private final Map<ResourceLocation, Definition> byId;
        private final Map<ResourceLocation, RecipeMap.Entry> eagerById;
        private final List<RecipeMap.Entry> eagerEntries;
        private final Map<Item, List<Definition>> lazyByItem;
        private final String stableFingerprint;
        private final long syncPayloadBytes;
        private final LinkedHashMap<ResourceLocation, RecipeMap.Entry> cache =
                new LinkedHashMap<>(64, 0.75F, true);

        private Snapshot(
                long epoch,
                RuntimeSide side,
                List<Definition> definitions,
                List<MaterialDefinition> materials,
                MaterialRuleExpansion.ResourceResolver resolver,
                MaterialRuleExpansion.FormIndexes formIndexes) {
            this.epoch = epoch;
            this.side = side;
            this.definitions = List.copyOf(definitions);
            this.materials = materials;
            this.resolver = resolver;
            this.formIndexes = formIndexes;
            this.recipeIds = this.definitions.stream()
                    .map(definition -> definition.relation().stableId())
                    .toList();
            Map<ResourceLocation, Definition> mutableById = new HashMap<>();
            Map<ResourceLocation, RecipeMap.Entry> mutableEager =
                    new HashMap<>();
            List<RecipeMap.Entry> orderedEager = new ArrayList<>();
            Map<Item, List<Definition>> mutableLazyIndex = new HashMap<>();
            for (Definition definition : this.definitions) {
                ResourceLocation id = definition.relation().stableId();
                if (mutableById.putIfAbsent(id, definition) != null) {
                    throw new IllegalArgumentException(
                            "Duplicate compact stable id " + id);
                }
                if (isEager(definition)) {
                    RecipeMap.Entry entry = materializeUncached(definition);
                    mutableEager.put(id, entry);
                    orderedEager.add(entry);
                } else {
                    for (Item item : definition.inputItems()) {
                        mutableLazyIndex
                                .computeIfAbsent(
                                        item, ignored -> new ArrayList<>())
                                .add(definition);
                    }
                }
            }
            Map<Item, List<Definition>> immutableLazyIndex = new HashMap<>();
            mutableLazyIndex.forEach((item, rows) ->
                    immutableLazyIndex.put(item, List.copyOf(rows)));
            this.byId = Map.copyOf(mutableById);
            this.eagerById = Map.copyOf(mutableEager);
            this.eagerEntries = List.copyOf(orderedEager);
            this.lazyByItem = Map.copyOf(immutableLazyIndex);
            this.stableFingerprint = fingerprint(this.definitions);
            this.syncPayloadBytes = relationPayloadBytes(this.definitions);
        }

        public RuntimeSide side() {
            return side;
        }

        @Override
        public String familyId() {
            return FAMILY_ID;
        }

        @Override
        public long epoch() {
            return epoch;
        }

        @Override
        public int logicalRecipeCount() {
            return definitions.size();
        }

        @Override
        public int eagerRecipeCount() {
            return eagerEntries.size();
        }

        @Override
        public int lazyRecipeCount() {
            return logicalRecipeCount() - eagerRecipeCount();
        }

        @Override
        public synchronized int cacheSize() {
            return cache.size();
        }

        @Override
        public int cacheCeiling() {
            return CACHE_CEILING;
        }

        @Override
        public String stableFingerprint() {
            return stableFingerprint;
        }

        public long syncPayloadBytes() {
            return syncPayloadBytes;
        }

        @Override
        public List<ResourceLocation> recipeIds() {
            return recipeIds;
        }

        @Override
        public List<RecipeMap.Entry> eagerEntries() {
            return eagerEntries;
        }

        @Override
        public RecipeMap.Entry enumerationEntry(int index) {
            Definition definition = definitions.get(index);
            RecipeMap.Entry eager =
                    eagerById.get(definition.relation().stableId());
            return eager != null ? eager : materializeUncached(definition);
        }

        @Override
        public Optional<RecipeMap.Entry> entry(ResourceLocation id) {
            Definition definition = byId.get(id);
            if (definition == null) {
                return Optional.empty();
            }
            RecipeMap.Entry eager = eagerById.get(id);
            return Optional.of(eager != null
                    ? eager : materializeUncached(definition));
        }

        @Override
        public Optional<RecipeMap.Entry> findLazy(GTRecipeQuery query) {
            for (Definition definition : lazyCandidates(query)) {
                RecipeMap.Entry entry = materializeCached(definition);
                if (entry.recipe().matches(query)) {
                    return Optional.of(entry);
                }
            }
            return Optional.empty();
        }

        @Override
        public int indexedLazyCandidateCount(GTRecipeQuery query) {
            return lazyCandidates(query).size();
        }

        @Override
        public boolean hasLazyCandidate(ItemStack stack) {
            return !stack.isEmpty() && lazyByItem.containsKey(stack.getItem());
        }

        private TreeSet<Definition> lazyCandidates(GTRecipeQuery query) {
            TreeSet<Definition> candidates = new TreeSet<>(Comparator
                    .comparingInt((Definition value) ->
                            value.relation().shadowOrder())
                    .thenComparing(value ->
                            value.relation().stableId().toString()));
            for (ItemStack stack : query.itemInputsView()) {
                if (!stack.isEmpty()) {
                    candidates.addAll(lazyByItem.getOrDefault(
                            stack.getItem(), List.of()));
                }
            }
            return candidates;
        }

        private synchronized RecipeMap.Entry materializeCached(
                Definition definition) {
            ResourceLocation id = definition.relation().stableId();
            RecipeMap.Entry existing = cache.get(id);
            if (existing != null) {
                return existing;
            }
            RecipeMap.Entry created = materializeUncached(definition);
            cache.put(id, created);
            while (cache.size() > CACHE_CEILING) {
                var iterator = cache.entrySet().iterator();
                iterator.next();
                iterator.remove();
            }
            return created;
        }

        private RecipeMap.Entry materializeUncached(Definition definition) {
            var expanded = MaterialRuleExpansion.expandSparseRelation(
                    definition.source().rule(),
                    definition.relation(),
                    materials,
                    resolver,
                    formIndexes);
            if (!expanded.target().equals(TARGET)
                    || !expanded.id().equals(
                            definition.relation().stableId())) {
                throw new IllegalStateException(
                        "Compact relation materialized with a different identity");
            }
            return new RecipeMap.Entry(expanded.id(), expanded.recipe());
        }
    }

    private static boolean isEager(Definition definition) {
        return definition.relation().shadowOrder() % HOT_MODULO == 0;
    }

    private static String fingerprint(List<Definition> definitions) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (Definition definition : definitions) {
                MaterialRule.SparseRelation relation = definition.relation();
                MaterialRule.SparseTable table =
                        definition.source().rule().sparse().orElseThrow();
                String row = relation.shadowOrder()
                        + "|" + relation.stableId()
                        + "|" + relation.material()
                        + "|" + relation.input().prefix()
                        + "|" + relation.input().count()
                        + "|" + relation.output().prefix()
                        + "|" + relation.output().count()
                        + "|" + relation.duration()
                        + "|" + relation.eut()
                        + "|" + relation.fallback()
                        + "|" + relation.forgingTarget().orElse("")
                        + "|" + relation.plateGem()
                        + "|" + relation.heatMode()
                        + "|" + table.shapeItem()
                        + "\n";
                digest.update(row.getBytes(StandardCharsets.UTF_8));
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static long relationPayloadBytes(List<Definition> definitions) {
        long bytes = 0L;
        for (Definition definition : definitions) {
            MaterialRule.SparseRelation relation = definition.relation();
            bytes += encodedLength(relation.stableId().toString())
                    + encodedLength(relation.material()
                            + "|" + relation.input().prefix()
                            + "|" + relation.input().count())
                    + encodedLength(relation.material()
                            + "|" + relation.output().prefix()
                            + "|" + relation.output().count()
                            + "|" + relation.heatMode())
                    + Integer.BYTES * 3L
                    + 1L;
        }
        return bytes;
    }

    private static int encodedLength(String value) {
        return Integer.BYTES
                + value.getBytes(StandardCharsets.UTF_8).length;
    }

    private record Definition(
            Source source,
            MaterialRule.SparseRelation relation,
            Set<Item> inputItems) {}
}
