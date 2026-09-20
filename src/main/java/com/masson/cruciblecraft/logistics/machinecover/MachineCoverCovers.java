package com.masson.cruciblecraft.logistics.machinecover;

import java.util.List;

import com.masson.cruciblecraft.logistics.pipe.cover.CoverBehavior;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverBehaviorRegistry;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverDefinition;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCover;

import net.minecraft.resources.ResourceLocation;

/** Registry sidecar for the GT6 machine-cover behavior identities. */
public final class MachineCoverCovers {
    private static final List<String> BEHAVIOR_PATHS = List.of(
            "cover_blank",
            "cover_crafting",
            "cover_drain",
            "cover_warning",
            "controller_auto",
            "controller_auto_redstone",
            "controller_auto_timer",
            "controller_covers",
            "controller_display",
            "controller_redstone",
            "detector_running",
            "display_energy",
            "redstone_conductor_in",
            "redstone_conductor_out",
            "redstone_emitter",
            "redstone_repeater",
            "redstone_torch",
            "scale_energy",
            "scale_progress",
            "selector_button_panel",
            "selector_redstone",
            "selector_tag",
            "vent");
    private static boolean registered;

    private MachineCoverCovers() {}

    public static synchronized void bootstrap() {
        if (registered) {
            return;
        }
        for (String path : BEHAVIOR_PATHS) {
            CoverBehaviorRegistry.register(
                    ResourceLocation.fromNamespaceAndPath(
                            "cruciblecraft", path),
                    behavior(path));
        }
        registered = true;
    }

    private static CoverBehavior behavior(String path) {
        if ("cover_blank".equals(path)
                || "cover_crafting".equals(path)
                || "cover_drain".equals(path)
                || "cover_warning".equals(path)) {
            return new CoverBehavior() {};
        }
        return new CoverBehavior() {
            @Override
            public boolean allowsIncoming(
                    PipeCover cover,
                    CoverDefinition definition,
                    Access access) {
                return false;
            }

            @Override
            public boolean allowsOutgoing(
                    PipeCover cover,
                    CoverDefinition definition,
                    Access access) {
                return false;
            }
        };
    }
}
