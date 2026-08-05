package com.masson.cruciblecraft.material;

import java.util.Optional;
import java.util.function.Predicate;

import com.masson.cruciblecraft.CrucibleCraft;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/** Resolves material validation from the registered item instance. */
public final class MaterialComponentPolicies {
    private MaterialComponentPolicies() {}

    public static Optional<MaterialComponentPolicy> resolve(String itemId) {
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null) {
            return Optional.empty();
        }
        return BuiltInRegistries.ITEM.getOptional(id)
                .filter(MaterialComponentPolicy.class::isInstance)
                .map(MaterialComponentPolicy.class::cast);
    }

    public static boolean isValid(
            String itemId,
            String componentId,
            String materialId,
            Predicate<String> materialExists) {
        if (materialId == null
                || materialId.isBlank()
                || !materialExists.test(materialId)) {
            return false;
        }
        Optional<MaterialComponentPolicy> policy = resolve(itemId);
        if (policy.isPresent()) {
            MaterialComponentPolicy resolved = policy.orElseThrow();
            return componentId.equals(resolved.materialComponentId())
                    && resolved.isPersistedMaterialAllowed(materialId);
        }
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        return id == null || !CrucibleCraft.MODID.equals(id.getNamespace());
    }
}
