package com.masson.cruciblecraft.content.multiblock;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/** A physical port block that owns persistent item/fluid storage. */
public interface PortStoreCarrier {
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
