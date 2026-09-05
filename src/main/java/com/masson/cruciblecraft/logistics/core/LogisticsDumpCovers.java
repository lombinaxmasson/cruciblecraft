package com.masson.cruciblecraft.logistics.core;

import com.masson.cruciblecraft.logistics.pipe.cover.CoverBehavior;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverBehaviorRegistry;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverDefinition;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCover;

/** Marker plugin: disconnects the face; Core tick performs the dump move. */
public final class LogisticsDumpCovers {
    private static boolean registered;

    private LogisticsDumpCovers() {}

    public static synchronized void bootstrap() {
        if (registered) {
            return;
        }
        CoverBehaviorRegistry.register(LogisticsDumpKinds.DUMP_BEHAVIOR, dumpBehavior());
        registered = true;
    }

    private static CoverBehavior dumpBehavior() {
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
