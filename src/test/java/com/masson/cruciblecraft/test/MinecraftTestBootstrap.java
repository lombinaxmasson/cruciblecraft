package com.masson.cruciblecraft.test;

import java.util.List;
import java.util.Map;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.neoforged.fml.loading.LoadingModList;

/** Idempotent bootstrap that must run before registry-touching test constants. */
public final class MinecraftTestBootstrap {
    private static boolean bootstrapped;

    private MinecraftTestBootstrap() {}

    public static synchronized void bootstrap() {
        if (bootstrapped) {
            return;
        }
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        bootstrapped = true;
    }
}
