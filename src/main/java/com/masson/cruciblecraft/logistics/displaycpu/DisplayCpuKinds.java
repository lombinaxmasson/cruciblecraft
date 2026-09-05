package com.masson.cruciblecraft.logistics.displaycpu;

import java.util.Optional;
import java.util.Set;

import net.minecraft.resources.ResourceLocation;

/** Four Display CPU status covers. Not processor unit blocks. */
public final class DisplayCpuKinds {
    public static final ResourceLocation LOGIC =
            id("logistics_display_cpu_logic");
    public static final ResourceLocation CONTROL =
            id("logistics_display_cpu_control");
    public static final ResourceLocation STORAGE =
            id("logistics_display_cpu_storage");
    public static final ResourceLocation CONVERSION =
            id("logistics_display_cpu_conversion");
    public static final ResourceLocation BEHAVIOR =
            id("logistics_display_cpu");
    public static final Set<ResourceLocation> ALL = Set.of(
            LOGIC, CONTROL, STORAGE, CONVERSION);
    public static final Set<String> PATHS = Set.of(
            LOGIC.getPath(),
            CONTROL.getPath(),
            STORAGE.getPath(),
            CONVERSION.getPath());

    public enum Kind {
        LOGIC,
        CONTROL,
        STORAGE,
        CONVERSION
    }

    private DisplayCpuKinds() {}

    public static boolean isDisplay(ResourceLocation id) {
        return id != null && ALL.contains(id);
    }

    public static Optional<Kind> kind(ResourceLocation id) {
        if (LOGIC.equals(id)) {
            return Optional.of(Kind.LOGIC);
        }
        if (CONTROL.equals(id)) {
            return Optional.of(Kind.CONTROL);
        }
        if (STORAGE.equals(id)) {
            return Optional.of(Kind.STORAGE);
        }
        if (CONVERSION.equals(id)) {
            return Optional.of(Kind.CONVERSION);
        }
        return Optional.empty();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
