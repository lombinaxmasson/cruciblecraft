package com.masson.cruciblecraft.recipe.rule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.material.def.MaterialTuning;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;

import net.minecraft.resources.ResourceLocation;

/** Deterministic, side-independent validation before one atomic publication. */
public final class MaterialRuleRuntimeMetadata {
    private MaterialRuleRuntimeMetadata() {}

    public static Publication build(
            Collection<Source> sources,
            Collection<MaterialDefinition> startupMaterials,
            PreferenceValidator preferenceValidator) {
        Map<String, MaterialDefinition> materials = startupMaterials.stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        MaterialDefinition::id,
                        material -> material));
        LinkedHashMap<String, MaterialTuning> tunings = new LinkedHashMap<>();
        LinkedHashMap<String, PreferenceCandidate> preferences = new LinkedHashMap<>();
        List<String> diagnostics = new ArrayList<>();

        sources.stream()
                .sorted(Comparator.comparing(source -> source.id().toString()))
                .forEach(source -> {
                    source.rule().tuning().ifPresent(tuning -> {
                        if (!materials.containsKey(tuning.material())) {
                            diagnostics.add(source.id() + ": unknown tuning material "
                                    + tuning.material());
                        } else if (tunings.containsKey(tuning.material())) {
                            diagnostics.add(source.id() + ": duplicate tuning for "
                                    + tuning.material());
                        } else {
                            tunings.put(tuning.material(), convert(tuning));
                        }
                    });
                    for (MaterialRule.UnificationPreference preference
                            : source.rule().unification()) {
                        MaterialDefinition material = materials.get(preference.material());
                        MaterialPrefix prefix;
                        try {
                            prefix = MaterialPrefixCatalog.require(preference.prefix());
                        } catch (IllegalArgumentException exception) {
                            diagnostics.add(source.id() + ": " + exception.getMessage());
                            continue;
                        }
                        if (material == null
                                || !MaterialCatalog.isFormRegistered(material, prefix)) {
                            diagnostics.add(source.id() + ": invalid unification target "
                                    + preference.material() + "/" + preference.prefix());
                            continue;
                        }
                        if (!preferenceValidator.isValid(
                                preference.material(), prefix, preference.item())) {
                            diagnostics.add(source.id() + ": invalid unification item "
                                    + preference.item());
                            continue;
                        }
                        String key = preference.material() + "/" + prefix.serializedId();
                        PreferenceCandidate candidate = new PreferenceCandidate(
                                preference.item().toString(),
                                preference.priority(),
                                source.id().toString());
                        preferences.merge(key, candidate, PreferenceCandidate::preferred);
                    }
                });

        LinkedHashMap<String, String> selected = new LinkedHashMap<>();
        preferences.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> selected.put(entry.getKey(), entry.getValue().item()));
        return new Publication(
                List.copyOf(tunings.values()),
                java.util.Collections.unmodifiableMap(selected),
                List.copyOf(diagnostics));
    }

    private static MaterialTuning convert(MaterialRule.Tuning tuning) {
        return new MaterialTuning(
                tuning.material(),
                tuning.tier(),
                tuning.color(),
                tuning.meltingPoint(),
                tuning.boilingPoint(),
                tuning.density());
    }

    public record Source(ResourceLocation id, MaterialRule rule) {}

    public record Publication(
            List<MaterialTuning> tunings,
            Map<String, String> preferences,
            List<String> diagnostics) {}

    @FunctionalInterface
    public interface PreferenceValidator {
        boolean isValid(String materialId, MaterialPrefix prefix, ResourceLocation item);
    }

    private record PreferenceCandidate(String item, int priority, String source) {
        private static PreferenceCandidate preferred(
                PreferenceCandidate left,
                PreferenceCandidate right) {
            if (left.priority != right.priority) {
                return left.priority > right.priority ? left : right;
            }
            return left.source.compareTo(right.source) <= 0 ? left : right;
        }
    }
}
