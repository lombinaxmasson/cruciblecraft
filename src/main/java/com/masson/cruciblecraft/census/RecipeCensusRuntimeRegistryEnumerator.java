package com.masson.cruciblecraft.census;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.masson.cruciblecraft.content.multiblock.MultiblockControllerPluginRegistry;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureCatalog;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverBehaviorRegistry;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Enumerates live {@code cruciblecraft} registry ids after server bootstrap.
 * Each category maps to the registry surface exercised by the recipe census gate.
 */
public final class RecipeCensusRuntimeRegistryEnumerator {
    private static final int SAMPLE_LIMIT = 10;

    private RecipeCensusRuntimeRegistryEnumerator() {}

    public static List<String> enumerate(
            ServerLevel level,
            String category,
            String namespace) {
        return switch (category) {
            case "blocks" -> idsFromRegistry(
                    BuiltInRegistries.BLOCK, namespace);
            case "items" -> idsFromRegistry(
                    BuiltInRegistries.ITEM, namespace);
            case "fluids" -> idsFromRegistry(
                    BuiltInRegistries.FLUID, namespace);
            case "fluid_types" -> idsFromRegistry(
                    level.registryAccess().registryOrThrow(
                            NeoForgeRegistries.Keys.FLUID_TYPES),
                    namespace);
            case "block_entity_types" -> idsFromRegistry(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE, namespace);
            case "menu_types" -> idsFromRegistry(
                    BuiltInRegistries.MENU, namespace);
            case "recipe_types" -> idsFromRegistry(
                    BuiltInRegistries.RECIPE_TYPE, namespace);
            case "recipe_serializers" -> idsFromRegistry(
                    BuiltInRegistries.RECIPE_SERIALIZER, namespace);
            case "worldgen_features" -> idsFromRegistry(
                    BuiltInRegistries.FEATURE, namespace);
            case "configured_features" -> idsFromRegistry(
                    level.registryAccess().registryOrThrow(
                            Registries.CONFIGURED_FEATURE),
                    namespace);
            case "placed_features" -> idsFromRegistry(
                    level.registryAccess().registryOrThrow(
                            Registries.PLACED_FEATURE),
                    namespace);
            case "recipe_maps" -> ModRecipeMaps.ALL.stream()
                    .map(RecipeMap::id)
                    .filter(id -> namespace.equals(id.getNamespace()))
                    .map(ResourceLocation::toString)
                    .sorted()
                    .toList();
            case "multiblock_structures" -> MultiblockStructureCatalog.all()
                    .keySet().stream()
                    .filter(id -> namespace.equals(id.getNamespace()))
                    .map(ResourceLocation::toString)
                    .sorted()
                    .toList();
            case "multiblock_plugins" -> MultiblockControllerPluginRegistry
                    .whitelist().keySet().stream()
                    .filter(id -> namespace.equals(id.getNamespace()))
                    .map(ResourceLocation::toString)
                    .sorted()
                    .toList();
            case "creative_tabs" -> idsFromRegistry(
                    BuiltInRegistries.CREATIVE_MODE_TAB, namespace);
            case "cover_behaviors" -> {
                CoverBehaviorRegistry.validateDefinitions();
                yield CoverBehaviorRegistry.registeredIds().stream()
                        .filter(id -> namespace.equals(id.getNamespace()))
                        .map(ResourceLocation::toString)
                        .sorted()
                        .toList();
            }
            default -> throw new IllegalArgumentException(
                    "Unsupported census category: " + category);
        };
    }

    private static <T> List<String> idsFromRegistry(
            Registry<T> registry,
            String namespace) {
        List<String> ids = new ArrayList<>();
        for (ResourceLocation id : registry.keySet()) {
            if (namespace.equals(id.getNamespace())) {
                ids.add(id.toString());
            }
        }
        ids.sort(String::compareTo);
        return List.copyOf(ids);
    }

    public record CategoryDiff(
            String category,
            int expectedCount,
            int actualCount,
            int missingCount,
            int extraCount,
            int duplicateCount,
            List<String> missingSample,
            List<String> extraSample,
            List<String> duplicateSample) {
        public boolean ok() {
            return missingCount == 0 && extraCount == 0 && duplicateCount == 0;
        }

        public boolean frozenSubsetOk() {
            return missingCount == 0 && duplicateCount == 0;
        }

        public String message() {
            return String.format(
                    Locale.ROOT,
                    "%s expected=%d actual=%d missing=%d extra=%d "
                            + "duplicates=%d missingSample=%s extraSample=%s "
                            + "duplicateSample=%s",
                    category,
                    expectedCount,
                    actualCount,
                    missingCount,
                    extraCount,
                    duplicateCount,
                    missingSample,
                    extraSample,
                    duplicateSample);
        }
    }

    public static CategoryDiff compareCategory(
            String category,
            List<String> expected,
            List<String> actualRaw) {
        DuplicateScan duplicates = detectDuplicates(actualRaw);
        Set<String> actual = new LinkedHashSet<>(duplicates.unique());
        Set<String> expectedSet = new LinkedHashSet<>(expected);
        List<String> missing = expected.stream()
                .filter(id -> !actual.contains(id))
                .toList();
        List<String> extra = actual.stream()
                .filter(id -> !expectedSet.contains(id))
                .sorted()
                .toList();
        return new CategoryDiff(
                category,
                expected.size(),
                duplicates.unique().size(),
                missing.size(),
                extra.size(),
                duplicates.duplicates().size(),
                bounded(missing),
                bounded(extra),
                bounded(duplicates.duplicates()));
    }

    private static DuplicateScan detectDuplicates(List<String> ids) {
        Set<String> seen = new LinkedHashSet<>();
        List<String> duplicates = new ArrayList<>();
        List<String> unique = new ArrayList<>();
        for (String id : ids) {
            if (!seen.add(id)) {
                if (!duplicates.contains(id)) {
                    duplicates.add(id);
                }
                continue;
            }
            unique.add(id);
        }
        return new DuplicateScan(unique, duplicates);
    }

    private static List<String> bounded(List<String> values) {
        if (values.size() <= SAMPLE_LIMIT) {
            return List.copyOf(values);
        }
        return List.copyOf(values.subList(0, SAMPLE_LIMIT));
    }

    private record DuplicateScan(List<String> unique, List<String> duplicates) {}
}
