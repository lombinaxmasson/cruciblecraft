package com.masson.cruciblecraft.content.blockentity;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6 32757 NBT material for {@code WorldgenStoneLayers} {@code tLastRock}
 * pebbles. WorldgenRocks grass pebbles leave this empty and use contents.
 */
public final class GtSurfaceRockBlockEntity extends BlockEntity {
    private String materialId = "";
    private boolean rawOre;

    public GtSurfaceRockBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GT_SURFACE_ROCK.get(), pos, state);
    }

    public String materialId() {
        return materialId;
    }

    public boolean hasMaterial() {
        return !materialId.isEmpty();
    }

    public void setMaterial(@Nullable String materialId) {
        this.materialId = materialId == null ? "" : materialId;
        setChanged();
    }

    public boolean rawOre() {
        return rawOre;
    }

    public void setRawOre(boolean rawOre) {
        this.rawOre = rawOre;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!materialId.isEmpty()) {
            tag.putString("Material", materialId);
        }
        if (rawOre) {
            tag.putBoolean("RawOre", true);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        materialId = tag.getString("Material");
        rawOre = tag.getBoolean("RawOre");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
