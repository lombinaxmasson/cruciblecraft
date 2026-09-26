package com.masson.cruciblecraft.gametest.support;

import java.util.Locale;
import java.util.Optional;

import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.logistics.pipe.PipeCatalog;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

/**
 * Fixtures that only accept parts GT6 registers and CC already registers.
 * A missing part is an assertion failure. Callers must not swap in another
 * material, prefix, or vanilla item.
 */
public final class GameTestFixtures {
    private GameTestFixtures() {}

    public static ItemStack requireMaterialStack(
            String materialId,
            MaterialPrefix form,
            int count) {
        Optional<ItemStack> stack = MaterialLookup.tryStack(materialId, form, count);
        if (stack.isEmpty()) {
            throw new IllegalArgumentException(missingMaterial(materialId, form));
        }
        return stack.orElse(ItemStack.EMPTY);
    }

    public static ItemStack requireMaterialStack(
            GameTestHelper helper,
            String materialId,
            MaterialPrefix form,
            int count) {
        Optional<ItemStack> stack = MaterialLookup.tryStack(materialId, form, count);
        if (stack.isEmpty()) {
            GameTestFailures.fail(helper, missingMaterial(materialId, form));
            throw new IllegalStateException(missingMaterial(materialId, form));
        }
        return stack.orElse(ItemStack.EMPTY);
    }

    public static PipeCatalog.Entry requirePipe(
            String materialId,
            MaterialPrefix form,
            PipeCatalog.Kind kind) {
        if (!PipeCatalog.isInitialized()
                || !PipeCatalog.contains(materialId, form, kind)) {
            throw new IllegalArgumentException(missingPipe(materialId, form, kind));
        }
        return PipeCatalog.require(materialId, form, kind);
    }

    public static PipeCatalog.Entry requirePipe(
            GameTestHelper helper,
            String materialId,
            MaterialPrefix form,
            PipeCatalog.Kind kind) {
        if (!PipeCatalog.isInitialized()
                || !PipeCatalog.contains(materialId, form, kind)) {
            GameTestFailures.fail(helper, missingPipe(materialId, form, kind));
            throw new IllegalStateException(missingPipe(materialId, form, kind));
        }
        return PipeCatalog.require(materialId, form, kind);
    }

    public static GTRecipe requireRecipe(RecipeMap map, String path) {
        return map.entries().stream()
                .filter(entry -> entry.id().getPath().equals(path))
                .findFirst()
                .map(RecipeMap.Entry::recipe)
                .orElseThrow(() -> new IllegalStateException(
                        "Missing live recipe " + map.id() + "/" + path));
    }

    private static String missingMaterial(String materialId, MaterialPrefix form) {
        return GameTestFailures.truncate(
                "missing registered material " + materialId + "/"
                        + form.serializedName());
    }

    private static String missingPipe(
            String materialId,
            MaterialPrefix form,
            PipeCatalog.Kind kind) {
        return GameTestFailures.truncate(
                "missing GT6 " + kind.name().toLowerCase(Locale.ROOT)
                        + " pipe " + materialId + "/" + form.serializedName());
    }
}
