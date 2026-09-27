package com.masson.cruciblecraft.mixin;

import java.util.IdentityHashMap;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads Reliable EMI's shared per-stack group index. */
@Mixin(targets = "com.evandev.remi.feature.stackgroup.StackGroupManager", remap = false)
interface RemiStackGroupFields {
    @Accessor("stackToGroupedStacks")
    static IdentityHashMap<?, ?> cruciblecraft$stackToGroupedStacks() {
        throw new AssertionError();
    }
}
