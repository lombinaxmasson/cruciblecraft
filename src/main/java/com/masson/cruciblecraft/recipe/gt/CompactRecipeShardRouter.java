package com.masson.cruciblecraft.recipe.gt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Deterministic query-addressable routing for one compact publication group.
 *
 * <p>Routing schema {@value #ROUTING_SCHEMA_VERSION} extracts canonical
 * item, component, and fluid keys from every relation. Unsupported, empty, or
 * tag key sets use one explicit overflow shard. Indexed relations choose the
 * least-frequent single key or unordered key pair, preferring a single on a
 * posting tie and then the lexical tuple. The shard id is SHA-256 of target,
 * publication group, schema version, and route key, each separated by a
 * newline. Queries generate only their single keys and unordered pairs,
 * directly union those shard postings, and always add the bounded overflow;
 * unrelated shards are never scanned.
 */
public final class CompactRecipeShardRouter {
    public static final String ROUTING_SCHEMA_VERSION = "compact-shard-v1";
    public static final int HARD_SHARD_CEILING = 128;
    private static final int MAX_QUERY_KEYS = 64;
    private static final String PAIR_SEPARATOR = "\0";
    private static final String OVERFLOW_ROUTE_KEY = "overflow";
    private static final Comparator<CompactGTRecipeFamilyDefinition.Relation>
            RELATION_ORDER = Comparator
                    .comparingInt(
                            CompactGTRecipeFamilyDefinition.Relation::shadowOrder)
                    .thenComparing(
                            CompactGTRecipeFamilyDefinition.Relation::stableId,
                            CompactRecipeShardRouter::compareLocation);

    private final ResourceLocation targetMap;
    private final ResourceLocation publicationGroup;
    private final String overflowShardId;
    private final Map<String, String> shardIdByRouteKey;
    private final Map<String, List<CompactGTRecipeFamilyDefinition.Relation>>
            membersByShardId;
    private final Map<ResourceLocation, String> shardIdByRelation;
    private final Map<ResourceLocation, String> routeKeyByRelation;
    private final Map<String, Set<String>> shardIdsByCanonicalKey;
    private final int overflowCount;

    public CompactRecipeShardRouter(
            ResourceLocation targetMap,
            ResourceLocation publicationGroup,
            List<CompactGTRecipeFamilyDefinition.Relation> relations) {
        this.targetMap = Objects.requireNonNull(targetMap, "targetMap");
        this.publicationGroup = Objects.requireNonNull(
                publicationGroup, "publicationGroup");
        Objects.requireNonNull(relations, "relations");
        MessageDigest digest = sha256();
        this.overflowShardId = shardId(digest, OVERFLOW_ROUTE_KEY);

        List<CompactGTRecipeFamilyDefinition.Relation> ordered = relations.stream()
                .map(relation -> Objects.requireNonNull(relation, "relation"))
                .sorted(RELATION_ORDER)
                .toList();
        Map<ResourceLocation, IndexKeys> keysByRelation = new LinkedHashMap<>();
        for (CompactGTRecipeFamilyDefinition.Relation relation : ordered) {
            if (keysByRelation.putIfAbsent(
                    relation.stableId(), extractIndexKeys(relation)) != null) {
                throw new IllegalArgumentException(
                        "Duplicate compact stable id " + relation.stableId()
                                + " in shard router");
            }
        }

        Map<String, Integer> postingFrequencies = new HashMap<>();
        for (IndexKeys keys : keysByRelation.values()) {
            if (keys.unindexed()) {
                continue;
            }
            for (RouteTuple tuple : keys.tuples()) {
                postingFrequencies.merge(tuple.encoded(), 1, Integer::sum);
            }
        }

        Map<String, String> mutableShardIdByRouteKey = new HashMap<>();
        Map<String, String> routeKeyByShardId = new HashMap<>();
        Map<String, List<CompactGTRecipeFamilyDefinition.Relation>> mutableMembers =
                new HashMap<>();
        Map<ResourceLocation, String> mutableShardByRelation = new HashMap<>();
        Map<ResourceLocation, String> mutableRouteByRelation = new HashMap<>();
        Map<String, Set<String>> mutableShardsByCanonicalKey = new HashMap<>();
        int mutableOverflowCount = 0;
        for (CompactGTRecipeFamilyDefinition.Relation relation : ordered) {
            IndexKeys keys = keysByRelation.get(relation.stableId());
            String routeKey;
            RouteTuple selected = null;
            if (keys.unindexed()) {
                routeKey = OVERFLOW_ROUTE_KEY;
                mutableOverflowCount++;
            } else {
                selected = selectRoute(keys.tuples(), postingFrequencies);
                routeKey = selected.encoded();
            }
            String relationShardId;
            if (routeKey.equals(OVERFLOW_ROUTE_KEY)) {
                relationShardId = overflowShardId;
            } else {
                relationShardId = mutableShardIdByRouteKey.get(routeKey);
                if (relationShardId == null) {
                    relationShardId = shardId(digest, routeKey);
                    mutableShardIdByRouteKey.put(routeKey, relationShardId);
                }
            }
            String previousRoute = routeKeyByShardId.putIfAbsent(
                    relationShardId, routeKey);
            if (previousRoute != null && !previousRoute.equals(routeKey)) {
                throw new IllegalArgumentException(
                        "Compact shard id collision between route keys "
                                + previousRoute + " and " + routeKey);
            }
            if (!routeKey.equals(OVERFLOW_ROUTE_KEY)) {
                mutableShardIdByRouteKey.put(routeKey, relationShardId);
            }
            mutableMembers.computeIfAbsent(
                    relationShardId, ignored -> new ArrayList<>()).add(relation);
            mutableShardByRelation.put(relation.stableId(), relationShardId);
            mutableRouteByRelation.put(relation.stableId(), routeKey);
            if (selected != null) {
                for (String canonicalKey : selected.members()) {
                    mutableShardsByCanonicalKey
                            .computeIfAbsent(
                                    canonicalKey, ignored -> new HashSet<>())
                            .add(relationShardId);
                }
            }
        }

        for (var entry : mutableMembers.entrySet()) {
            if (entry.getValue().size() > HARD_SHARD_CEILING) {
                throw new IllegalArgumentException(
                        "Compact shard " + entry.getKey() + " for "
                                + targetMap + "/" + publicationGroup
                                + " contains " + entry.getValue().size()
                                + " relations, exceeding hard ceiling "
                                + HARD_SHARD_CEILING);
            }
        }
        if (mutableOverflowCount > HARD_SHARD_CEILING) {
            throw new IllegalArgumentException(
                    "Compact overflow shard for " + targetMap + "/"
                            + publicationGroup + " contains "
                            + mutableOverflowCount
                            + " relations, exceeding hard ceiling "
                            + HARD_SHARD_CEILING);
        }

        this.shardIdByRouteKey = Map.copyOf(mutableShardIdByRouteKey);
        this.membersByShardId = immutableMembers(mutableMembers);
        this.shardIdByRelation = Map.copyOf(mutableShardByRelation);
        this.routeKeyByRelation = Map.copyOf(mutableRouteByRelation);
        this.shardIdsByCanonicalKey = immutableSets(
                mutableShardsByCanonicalKey);
        this.overflowCount = mutableOverflowCount;
    }

    public ResourceLocation targetMap() {
        return targetMap;
    }

    public ResourceLocation publicationGroup() {
        return publicationGroup;
    }

    public int shardCount() {
        return membersByShardId.size();
    }

    public int overflowCount() {
        return overflowCount;
    }

    public String overflowShardId() {
        return overflowShardId;
    }

    public Optional<String> shardId(ResourceLocation stableId) {
        Objects.requireNonNull(stableId, "stableId");
        return Optional.ofNullable(shardIdByRelation.get(stableId));
    }

    public Optional<String> routeKey(ResourceLocation stableId) {
        Objects.requireNonNull(stableId, "stableId");
        return Optional.ofNullable(routeKeyByRelation.get(stableId));
    }

    public List<ResourceLocation> shardMembers(String shardId) {
        Objects.requireNonNull(shardId, "shardId");
        return membersByShardId.getOrDefault(shardId, List.of()).stream()
                .map(CompactGTRecipeFamilyDefinition.Relation::stableId)
                .toList();
    }

    public Set<String> routedShardIds(GTRecipeQuery query) {
        Objects.requireNonNull(query, "query");
        TreeSet<String> routed = new TreeSet<>();
        for (RouteTuple tuple : routeTuples(queryKeys(query))) {
            String shard = shardIdByRouteKey.get(tuple.encoded());
            if (shard != null) {
                routed.add(shard);
            }
        }
        if (overflowCount > 0) {
            routed.add(overflowShardId);
        }
        return Collections.unmodifiableSet(routed);
    }

    public List<CompactGTRecipeFamilyDefinition.Relation> routedCandidates(
            GTRecipeQuery query) {
        LinkedHashMap<ResourceLocation, CompactGTRecipeFamilyDefinition.Relation>
                candidates = new LinkedHashMap<>();
        for (String shardId : routedShardIds(query)) {
            for (CompactGTRecipeFamilyDefinition.Relation relation
                    : membersByShardId.getOrDefault(shardId, List.of())) {
                candidates.putIfAbsent(relation.stableId(), relation);
            }
        }
        return candidates.values().stream().sorted(RELATION_ORDER).toList();
    }

    public int indexedCandidateCount(GTRecipeQuery query) {
        return routedCandidates(query).size();
    }

    public boolean hasCandidate(ItemStack stack) {
        return !candidateShardIds(stack).isEmpty();
    }

    Set<String> candidateShardIds(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        if (stack.isEmpty()) {
            return Set.of();
        }
        Set<String> canonicalKeys = new HashSet<>();
        canonicalKeys.add(canonicalItem(stack.getItem()));
        for (ComponentIngredientIndex.Key key
                : ComponentIngredientIndex.keys(stack)) {
            canonicalKeys.add(canonicalComponent(key));
        }
        TreeSet<String> candidates = new TreeSet<>();
        for (String canonicalKey : canonicalKeys) {
            candidates.addAll(shardIdsByCanonicalKey.getOrDefault(
                    canonicalKey, Set.of()));
        }
        if (overflowCount > 0) {
            candidates.add(overflowShardId);
        }
        return Collections.unmodifiableSet(candidates);
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String shardId(MessageDigest digest, String routeKey) {
        digest.reset();
        updateUtf8(digest, targetMap.toString());
        digest.update((byte) '\n');
        updateUtf8(digest, publicationGroup.toString());
        digest.update((byte) '\n');
        updateUtf8(digest, ROUTING_SCHEMA_VERSION);
        digest.update((byte) '\n');
        updateUtf8(digest, routeKey);
        digest.update((byte) '\n');
        return java.util.HexFormat.of().formatHex(digest.digest());
    }

    private static void updateUtf8(MessageDigest digest, String text) {
        digest.update(text.getBytes(StandardCharsets.UTF_8));
    }

    private static int compareLocation(
            ResourceLocation left, ResourceLocation right) {
        int namespace = left.getNamespace().compareTo(right.getNamespace());
        if (namespace != 0) {
            return namespace;
        }
        return left.getPath().compareTo(right.getPath());
    }

    private static RouteTuple selectRoute(
            List<RouteTuple> tuples,
            Map<String, Integer> postingFrequencies) {
        RouteTuple selected = null;
        for (RouteTuple tuple : tuples) {
            if (selected == null
                    || compareRoute(tuple, selected, postingFrequencies) < 0) {
                selected = tuple;
            }
        }
        if (selected == null) {
            throw new IllegalArgumentException(
                    "Compact shard routing found no route tuple");
        }
        return selected;
    }

    private static int compareRoute(
            RouteTuple left,
            RouteTuple right,
            Map<String, Integer> postingFrequencies) {
        int byFrequency = Integer.compare(
                postingFrequencies.get(left.encoded()),
                postingFrequencies.get(right.encoded()));
        if (byFrequency != 0) {
            return byFrequency;
        }
        int byLength = Integer.compare(left.length(), right.length());
        if (byLength != 0) {
            return byLength;
        }
        return left.encoded().compareTo(right.encoded());
    }

    private static IndexKeys extractIndexKeys(
            CompactGTRecipeFamilyDefinition.Relation relation) {
        TreeSet<String> keys = new TreeSet<>();
        boolean unindexed = false;
        for (Ingredient ingredient : relation.itemInputs()) {
            Ingredient indexed = PrefixMaterialItemCodecs.tightenLiveIngredient(ingredient);
            if (indexed == null || indexed.isEmpty()) {
                unindexed = true;
                continue;
            }
            ComponentIngredientIndex.Extraction extraction =
                    ComponentIngredientIndex.extract(indexed);
            if (extraction.supported()) {
                long itemAlternatives = extraction.keys().stream()
                        .map(ComponentIngredientIndex.Key::item)
                        .distinct()
                        .count();
                if (itemAlternatives != 1L) {
                    unindexed = true;
                } else {
                    extraction.keys().stream()
                            .map(CompactRecipeShardRouter::canonicalComponent)
                            .forEach(keys::add);
                }
                continue;
            }
            if (!indexed.isSimple()) {
                unindexed = true;
                continue;
            }
            ItemStack[] stacks = indexed.getItems();
            if (stacks.length != 1
                    || stacks[0].isEmpty()
                    || java.util.Arrays.stream(indexed.getValues())
                            .anyMatch(value -> value instanceof Ingredient.TagValue)) {
                unindexed = true;
                continue;
            }
            keys.add(canonicalItem(stacks[0].getItem()));
        }
        for (FluidStack stack : relation.fluidInputs()) {
            if (!stack.isEmpty()) {
                keys.add(canonicalFluid(stack.getFluid()));
            }
        }
        if (keys.isEmpty()) {
            unindexed = true;
        }
        List<String> canonical = List.copyOf(keys);
        return new IndexKeys(
                canonical,
                unindexed,
                unindexed ? List.of() : routeTuples(canonical));
    }

    private static List<String> queryKeys(GTRecipeQuery query) {
        TreeSet<String> keys = new TreeSet<>();
        for (ItemStack stack : query.itemInputsView()) {
            if (stack.isEmpty()) {
                continue;
            }
            keys.add(canonicalItem(stack.getItem()));
            ComponentIngredientIndex.keys(stack).stream()
                    .map(CompactRecipeShardRouter::canonicalComponent)
                    .forEach(keys::add);
        }
        for (FluidStack stack : query.fluidInputsView()) {
            if (!stack.isEmpty()) {
                keys.add(canonicalFluid(stack.getFluid()));
            }
        }
        if (keys.size() > MAX_QUERY_KEYS) {
            throw new IllegalArgumentException(
                    "Compact shard query exposes " + keys.size()
                            + " index keys, exceeding bounded maximum "
                            + MAX_QUERY_KEYS);
        }
        return List.copyOf(keys);
    }

    private static List<RouteTuple> routeTuples(List<String> sortedKeys) {
        List<RouteTuple> tuples = new ArrayList<>(
                sortedKeys.size() + sortedKeys.size() * (sortedKeys.size() - 1) / 2);
        for (String key : sortedKeys) {
            tuples.add(RouteTuple.single(key));
        }
        for (int left = 0; left < sortedKeys.size(); left++) {
            for (int right = left + 1; right < sortedKeys.size(); right++) {
                tuples.add(RouteTuple.pair(
                        sortedKeys.get(left), sortedKeys.get(right)));
            }
        }
        return tuples;
    }

    private static String canonicalItem(Item item) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(
                Objects.requireNonNull(item, "item"));
        if (itemId == null) {
            throw new IllegalArgumentException(
                    "Compact shard routing cannot index an unregistered item");
        }
        return "item:" + itemId;
    }

    private static String canonicalFluid(Fluid fluid) {
        ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(
                Objects.requireNonNull(fluid, "fluid"));
        if (fluidId == null) {
            throw new IllegalArgumentException(
                    "Compact shard routing cannot index an unregistered fluid");
        }
        return "fluid:" + fluidId;
    }

    private static String canonicalComponent(ComponentIngredientIndex.Key key) {
        return "component:" + BuiltInRegistries.ITEM.getKey(key.item())
                + "/" + key.componentId() + "=" + key.value();
    }

    private static Map<String, List<CompactGTRecipeFamilyDefinition.Relation>>
            immutableMembers(
                    Map<String, List<CompactGTRecipeFamilyDefinition.Relation>>
                            mutable) {
        Map<String, List<CompactGTRecipeFamilyDefinition.Relation>> result =
                new HashMap<>();
        mutable.forEach((key, value) -> result.put(key, List.copyOf(value)));
        return Map.copyOf(result);
    }

    private static Map<String, Set<String>> immutableSets(
            Map<String, Set<String>> mutable) {
        Map<String, Set<String>> result = new HashMap<>();
        mutable.forEach((key, value) -> result.put(
                key, Collections.unmodifiableSet(new LinkedHashSet<>(value))));
        return Map.copyOf(result);
    }

    private record IndexKeys(
            List<String> canonicalKeys,
            boolean unindexed,
            List<RouteTuple> tuples) {}

    private record RouteTuple(
            String encoded,
            int length,
            List<String> members) {
        private static RouteTuple single(String key) {
            return new RouteTuple(key, 1, List.of(key));
        }

        private static RouteTuple pair(String left, String right) {
            return new RouteTuple(
                    left + PAIR_SEPARATOR + right,
                    2,
                    List.of(left, right));
        }
    }
}
