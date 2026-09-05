package com.masson.cruciblecraft.logistics.displaycpu;

import com.masson.cruciblecraft.logistics.pipe.cover.CoverBehavior;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverBehaviorRegistry;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverDefinition;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCover;

/** Status plugin: no transfer, Core writes redstone and visual. */
public final class DisplayCpuCovers {
    private static boolean registered;

    private DisplayCpuCovers() {}

    public static synchronized void bootstrap() {
        if (registered) {
            return;
        }
        CoverBehaviorRegistry.register(DisplayCpuKinds.BEHAVIOR, behavior());
        registered = true;
    }

    private static CoverBehavior behavior() {
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
