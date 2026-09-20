package com.masson.cruciblecraft.datagen;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonObject;
import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.compat.emi.EmiStackGroupPlan;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

/** Writes Reliable EMI stack groups under {@code assets/cruciblecraft/stack_groups/}. */
public final class ModEmiStackGroupProvider implements DataProvider {
    private final PackOutput.PathProvider pathProvider;

    public ModEmiStackGroupProvider(PackOutput output) {
        this.pathProvider = output.createPathProvider(
                PackOutput.Target.RESOURCE_PACK, "stack_groups");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (EmiStackGroupPlan.FormGroup group :
                EmiStackGroupPlan.emittedFormGroups(MaterialPrefixCatalog.values())) {
            futures.add(save(cache, group.resourcePath(), group.toJson()));
        }
        for (EmiStackGroupPlan.ExactGroup group : EmiStackGroupPlan.exactGroups()) {
            futures.add(save(cache, group.resourcePath(), group.toJson()));
        }
        return CompletableFuture.allOf(
                futures.toArray(CompletableFuture[]::new));
    }

    private CompletableFuture<?> save(
            CachedOutput cache,
            String resourcePath,
            JsonObject json) {
        return DataProvider.saveStable(
                cache,
                json,
                pathProvider.json(ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID, resourcePath)));
    }

    @Override
    public String getName() {
        return "CrucibleCraft EMI stack groups";
    }
}
