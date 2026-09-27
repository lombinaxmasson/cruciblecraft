package com.masson.cruciblecraft.content.multiblock;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/** A physical port block that owns persistent item/fluid storage. */
public interface PortStoreCarrier {
    /**
     * GT6 parts that only forward to the controller inventory return false.
     * Those walls must not hide the hull's own faces or swallow auto-output.
     */
    default boolean ownsIndependentPortStore() {
        return true;
    }

    PortStore portStore();

    void configurePortStore(
            MultiblockPortHost host,
            PortStore.Assignment assignment);

    default void savePortStore(
            CompoundTag tag,
            HolderLookup.Provider registries) {
        portStore().save(tag, registries);
    }

    default void loadPortStore(
            CompoundTag tag,
            HolderLookup.Provider registries) {
        portStore().load(tag, registries);
    }
}
