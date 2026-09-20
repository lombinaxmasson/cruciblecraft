package com.masson.cruciblecraft.datagen;

/**
 * NeoForge {@code ItemModelProvider} skips the {@code item/} folder when the
 * registry path already contains a slash, so {@code hslasteel/wire} is written
 * to {@code models/hslasteel/wire.json} instead of
 * {@code models/item/hslasteel/wire.json}.
 */
final class SlashItemModels {
    private SlashItemModels() {}

    static String path(String registryPath) {
        if (registryPath == null || registryPath.isEmpty()) {
            throw new IllegalArgumentException("blank item model path");
        }
        if (registryPath.startsWith("item/") || !registryPath.contains("/")) {
            return registryPath;
        }
        return "item/" + registryPath;
    }
}
