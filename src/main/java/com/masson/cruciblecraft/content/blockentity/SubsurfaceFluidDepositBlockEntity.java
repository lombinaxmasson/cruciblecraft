package com.masson.cruciblecraft.content.blockentity;

import java.util.Optional;

import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Persistent finite reserve created by subsurface world generation. */
public final class SubsurfaceFluidDepositBlockEntity extends BlockEntity {
    private ResourceLocation material;
    private ResourceLocation replacedHost;
    private long initialAmountMb;
    private long remainingAmountMb;

    public SubsurfaceFluidDepositBlockEntity(
            BlockPos pos, BlockState state) {
        super(ModBlockEntities.SUBSURFACE_FLUID_DEPOSIT.get(), pos, state);
    }

    public void initialize(
            ResourceLocation material,
            long amountMb,
            ResourceLocation replacedHost) {
        if (material == null || replacedHost == null || amountMb <= 0L) {
            throw new IllegalArgumentException(
                    "Fluid deposit initialization requires material, host, "
                            + "and a positive reserve");
        }
        if (this.material != null) {
            if (!this.material.equals(material)
                    || !this.replacedHost.equals(replacedHost)
                    || initialAmountMb != amountMb) {
                throw new IllegalStateException(
                        "Fluid deposit cannot be initialized with different data");
            }
            return;
        }
        this.material = material;
        this.replacedHost = replacedHost;
        this.initialAmountMb = amountMb;
        this.remainingAmountMb = amountMb;
        setChanged();
    }

    public Optional<Snapshot> snapshot() {
        return material == null
                ? Optional.empty()
                : Optional.of(new Snapshot(
                        material,
                        replacedHost,
                        initialAmountMb,
                        remainingAmountMb));
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (material == null) {
            return;
        }
        tag.putString("material", material.toString());
        tag.putString("replaced_host", replacedHost.toString());
        tag.putLong("initial_amount_mb", initialAmountMb);
        tag.putLong("remaining_amount_mb", remainingAmountMb);
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ResourceLocation loadedMaterial =
                ResourceLocation.tryParse(tag.getString("material"));
        ResourceLocation loadedHost =
                ResourceLocation.tryParse(tag.getString("replaced_host"));
        long loadedInitial = tag.getLong("initial_amount_mb");
        long loadedRemaining = tag.getLong("remaining_amount_mb");
        if (loadedMaterial == null
                || loadedHost == null
                || loadedInitial <= 0L
                || loadedRemaining < 0L
                || loadedRemaining > loadedInitial) {
            material = null;
            replacedHost = null;
            initialAmountMb = 0L;
            remainingAmountMb = 0L;
            return;
        }
        material = loadedMaterial;
        replacedHost = loadedHost;
        initialAmountMb = loadedInitial;
        remainingAmountMb = loadedRemaining;
    }

    public record Snapshot(
            ResourceLocation material,
            ResourceLocation replacedHost,
            long initialAmountMb,
            long remainingAmountMb) {}
}
