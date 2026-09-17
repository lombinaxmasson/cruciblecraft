package com.masson.cruciblecraft.content.blockentity;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** GT6 PrefixBlock material id for bedrock / small ores. */
public final class BedrockOreBlockEntity extends BlockEntity {
    private String materialId = "";

    public BedrockOreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BEDROCK_ORE.get(), pos, state);
    }

    public String materialId() {
        return materialId;
    }

    public boolean hasMaterial() {
        return !materialId.isEmpty();
    }

    public void setMaterialId(@Nullable String materialId) {
        this.materialId = materialId == null ? "" : materialId;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!materialId.isEmpty()) {
            tag.putString("Material", materialId);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        materialId = tag.getString("Material");
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
