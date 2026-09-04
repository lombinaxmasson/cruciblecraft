package com.masson.cruciblecraft.benchmark.recipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.IntFunction;

import com.masson.cruciblecraft.benchmark.recipe.RecipeFamilyProvider.ExtruderRecipe;
import com.masson.cruciblecraft.benchmark.recipe.RecipeFamilyProvider.ExtruderRelation;
import com.masson.cruciblecraft.benchmark.recipe.RecipeFamilyProvider.RecipeEnumeration;
import com.masson.cruciblecraft.benchmark.recipe.RecipeFamilyProvider.RuntimeSide;

abstract class AbstractExtruderProvider implements RecipeFamilyProvider {
    protected volatile long publishedEpoch;
    protected volatile RuntimeSide publishedSide = RuntimeSide.SERVER;
    protected volatile List<ExtruderRelation> publishedRelations = List.of();
    protected volatile long publishedSyncBytes;

    protected final void validatePublication(
            List<ExtruderRelation> relations,
            long epoch,
            RuntimeSide side) {
        Objects.requireNonNull(relations, "relations");
        Objects.requireNonNull(side, "side");
        if (!side.requiresIndependentExpansion()) {
            throw new IllegalArgumentException(
                    "Integrated clients must share the server publication");
        }
        if (epoch <= publishedEpoch) {
            throw new IllegalArgumentException(
                    "Recipe epoch must increase: current=" + publishedEpoch
                            + ", requested=" + epoch);
        }
        for (int index = 0; index < relations.size(); index++) {
            ExtruderRelation relation = Objects.requireNonNull(
                    relations.get(index), "relation");
            if (relation.ordinal() != index) {
                throw new IllegalArgumentException(
                        "Relations must be in contiguous ordinal order");
            }
        }
    }

    protected final void commitCommon(
            List<ExtruderRelation> relations,
            long epoch,
            RuntimeSide side,
            long syncBytes) {
        publishedRelations = relations;
        publishedSide = side;
        publishedSyncBytes = syncBytes;
        publishedEpoch = epoch;
    }

    protected final void requireCurrentEpoch(long expectedEpoch) {
        if (expectedEpoch != publishedEpoch) {
            throw new IllegalStateException(
                    "Enumeration view epoch " + expectedEpoch
                            + " was invalidated by epoch " + publishedEpoch);
        }
    }

    protected final RecipeEnumeration epochView(
            int size,
            IntFunction<ExtruderRecipe> materializer) {
        long viewEpoch = publishedEpoch;
        return new RecipeEnumeration() {
            @Override
            public int size() {
                requireCurrentEpoch(viewEpoch);
                return size;
            }

            @Override
            public ExtruderRecipe get(int index) {
                requireCurrentEpoch(viewEpoch);
                if (index < 0 || index >= size) {
                    throw new IndexOutOfBoundsException(index);
                }
                return Objects.requireNonNull(materializer.apply(index));
            }

            @Override
            public long epoch() {
                requireCurrentEpoch(viewEpoch);
                return viewEpoch;
            }

            @Override
            public Iterator<ExtruderRecipe> iterator() {
                requireCurrentEpoch(viewEpoch);
                return new Iterator<>() {
                    private int index;

                    @Override
                    public boolean hasNext() {
                        requireCurrentEpoch(viewEpoch);
                        return index < size;
                    }

                    @Override
                    public ExtruderRecipe next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }
                        return get(index++);
                    }
                };
            }
        };
    }

    protected static <T> Map<String, List<T>> immutableInputIndex(
            List<T> values,
            java.util.function.Function<T, String> key) {
        Map<String, List<T>> mutable = new HashMap<>();
        for (T value : values) {
            mutable.computeIfAbsent(key.apply(value), ignored -> new ArrayList<>())
                    .add(value);
        }
        Map<String, List<T>> immutable = new HashMap<>();
        mutable.forEach((input, rows) ->
                immutable.put(input, List.copyOf(rows)));
        return Map.copyOf(immutable);
    }

    @Override
    public final long epoch() {
        return publishedEpoch;
    }

    @Override
    public final long syncPayloadBytes() {
        return publishedSyncBytes;
    }
}
