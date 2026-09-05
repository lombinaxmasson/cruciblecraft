package com.masson.cruciblecraft.logistics.core;

import java.util.Optional;
import java.util.Set;

import net.minecraft.resources.ResourceLocation;

/** Dump cover identity owned by Logistics Core, not Generic transfer. */
public final class LogisticsDumpKinds {
    public static final ResourceLocation DUMP =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "logistics_generic_dump");
    public static final ResourceLocation DUMP_BEHAVIOR =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "logistics_generic_dump");
    public static final Set<String> KNOWN_LOGISTICS_PATHS = Set.of(
            "logistics_item_storage",
            "logistics_item_import",
            "logistics_item_export",
            "logistics_fluid_storage",
            "logistics_fluid_import",
            "logistics_fluid_export",
            "logistics_generic_storage",
            "logistics_generic_import",
            "logistics_generic_export",
            "logistics_generic_dump",
            "logistics_display_cpu_logic",
            "logistics_display_cpu_control",
            "logistics_display_cpu_storage",
            "logistics_display_cpu_conversion");

    private LogisticsDumpKinds() {}

    public static boolean isDump(ResourceLocation id) {
        return DUMP.equals(id);
    }

    public static int networkId(
            com.masson.cruciblecraft.logistics.pipe.cover.PipeCover cover) {
        return cover.config().networkId().orElse(0);
    }

    public static Optional<Integer> joinedNetwork(
            com.masson.cruciblecraft.logistics.pipe.cover.PipeCover cover) {
        int id = networkId(cover);
        return id == 0 ? Optional.empty() : Optional.of(id);
    }
}
