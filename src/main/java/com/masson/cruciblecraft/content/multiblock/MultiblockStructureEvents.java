package com.masson.cruciblecraft.content.multiblock;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.masson.cruciblecraft.CrucibleCraft;

import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/** Registers the atomic server-data reload for multiblock JSON. */
@EventBusSubscriber(modid = CrucibleCraft.MODID)
public final class MultiblockStructureEvents {
    private MultiblockStructureEvents() {}

    @SubscribeEvent
    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new PreparableReloadListener() {
            @Override
            public CompletableFuture<Void> reload(
                    PreparationBarrier barrier,
                    ResourceManager resourceManager,
                    ProfilerFiller preparationProfiler,
                    ProfilerFiller reloadProfiler,
                    Executor preparationExecutor,
                    Executor reloadExecutor) {
                long reloadStarted = System.nanoTime();
                return CompletableFuture.supplyAsync(
                                () -> {
                                    long preparationStarted = System.nanoTime();
                                    var prepared = MultiblockStructureCatalog.prepare(
                                            resourceManager);
                                    CrucibleCraft.LOGGER.info(
                                            "Multiblock resource reload preparation: "
                                                    + "{}ms definitions={}",
                                            elapsedMillis(preparationStarted),
                                            prepared.size());
                                    return prepared;
                                },
                                preparationExecutor)
                        .thenCompose(barrier::wait)
                        .thenAcceptAsync(
                                prepared -> {
                                    long publicationStarted = System.nanoTime();
                                    MultiblockStructureCatalog.publish(prepared);
                                    CrucibleCraft.LOGGER.info(
                                            "Multiblock resource reload total: "
                                                    + "{}ms publish={}ms",
                                            elapsedMillis(reloadStarted),
                                            elapsedMillis(publicationStarted));
                                },
                                reloadExecutor);
            }
        });
    }

    private static long elapsedMillis(long started) {
        return (System.nanoTime() - started) / 1_000_000L;
    }
}
