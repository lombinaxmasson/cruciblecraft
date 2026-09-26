package com.masson.cruciblecraft.compat.emi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

/**
 * Builds processing EMI rows off the reload thread. EMI's common fork-join
 * pool does not see this mod's class loader, so the workers are our own.
 */
final class EmiProjection {
    private static final int PARALLEL_FLOOR = 512;

    private EmiProjection() {}

    static List<ProcessingEmiRecipe> project(
            List<ProcessingEmiRegistrationPlan.RecipeRegistration> recipes,
            Map<ResourceLocation, EmiRecipeCategory> categories,
            Map<ResourceLocation, EmiStack> workstations) {
        primeEmi();
        int size = recipes.size();
        int threads = Math.min(size, Runtime.getRuntime().availableProcessors());
        if (size < PARALLEL_FLOOR || threads < 2) {
            return projectRange(recipes, categories, workstations, 0, size);
        }
        int chunk = (size + threads - 1) / threads;
        ClassLoader loader = EmiProjection.class.getClassLoader();
        ExecutorService pool = Executors.newFixedThreadPool(threads, runnable -> {
            Thread thread = new Thread(runnable, "cruciblecraft-emi");
            thread.setDaemon(true);
            thread.setContextClassLoader(loader);
            return thread;
        });
        try {
            List<Future<List<ProcessingEmiRecipe>>> futures = new ArrayList<>();
            for (int start = 0; start < size; start += chunk) {
                int from = start;
                int to = Math.min(size, start + chunk);
                futures.add(pool.submit(() -> projectRange(
                        recipes, categories, workstations, from, to)));
            }
            List<ProcessingEmiRecipe> projected = new ArrayList<>(size);
            for (Future<List<ProcessingEmiRecipe>> future : futures) {
                projected.addAll(future.get());
            }
            return projected;
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("EMI projection interrupted", failure);
        } catch (ExecutionException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException("EMI projection failed", failure);
        } finally {
            pool.shutdownNow();
        }
    }

    private static void primeEmi() {
        EmiStack.of(Items.STONE);
        EmiStack.of(Fluids.WATER, 1);
    }

    private static List<ProcessingEmiRecipe> projectRange(
            List<ProcessingEmiRegistrationPlan.RecipeRegistration> recipes,
            Map<ResourceLocation, EmiRecipeCategory> categories,
            Map<ResourceLocation, EmiStack> workstations,
            int from,
            int to) {
        List<ProcessingEmiRecipe> projected = new ArrayList<>(to - from);
        for (int index = from; index < to; index++) {
            ProcessingEmiRegistrationPlan.RecipeRegistration recipe = recipes.get(index);
            ResourceLocation machineId = recipe.machine().spec().id();
            EmiRecipeCategory category = Objects.requireNonNull(
                    categories.get(machineId),
                    "Missing processing EMI category");
            EmiStack workstation = Objects.requireNonNull(
                    workstations.get(machineId),
                    "Missing processing EMI workstation");
            projected.add(new ProcessingEmiRecipe(
                    recipe.id(),
                    category,
                    recipe.machine().spec(),
                    recipe.recipe(),
                    workstation));
        }
        return projected;
    }
}
