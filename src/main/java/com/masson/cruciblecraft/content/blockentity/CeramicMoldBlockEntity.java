package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.content.block.CeramicMoldBlock;
import com.masson.cruciblecraft.content.item.CeramicMoldBlockItem;
import com.masson.cruciblecraft.content.mold.MoldCastingRules;
import com.masson.cruciblecraft.content.mold.MoldShape;
import com.masson.cruciblecraft.heat.ItemHeat;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

public final class CeramicMoldBlockEntity extends BlockEntity {
    private static final float AMBIENT_TEMPERATURE = 20.0F;

    private MoldShape shape = MoldShape.INGOT;
    private String materialId = "";
    private int outputCount;
    private float temperature = AMBIENT_TEMPERATURE;
    private boolean solidified;

    public CeramicMoldBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CERAMIC_MOLD.get(), pos, state);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            CeramicMoldBlockEntity mold) {
        if (!mold.isFilled()) {
            return;
        }
        if (MaterialLookup.byId(mold.materialId).isEmpty()) {
            return;
        }
        float maximum = MoldCastingRules.maximumTemperature((float)
                MaterialCatalog.require("ceramic").thermal().meltingPoint());
        if (mold.temperature > maximum) {
            level.setBlock(pos, Blocks.LAVA.defaultBlockState(), Block.UPDATE_ALL);
            return;
        }

        float previous = mold.temperature;
        mold.temperature = MoldCastingRules.cool(mold.temperature, AMBIENT_TEMPERATURE);
        boolean wasSolidified = mold.solidified;
        mold.solidified = mold.temperature
                < MaterialCatalog.require(mold.materialId).thermal().meltingPoint();
        if (wasSolidified != mold.solidified || (level.getGameTime() % 20L == 0L
                && Float.compare(previous, mold.temperature) != 0)) {
            mold.setChanged();
            mold.sync();
        }
    }

    public void setShape(MoldShape shape) {
        this.shape = shape;
        setChanged();
        sync();
    }

    public void fill(CrucibleBlockEntity.CastTransfer transfer) {
        materialId = transfer.material().id();
        outputCount = transfer.count();
        temperature = transfer.temperature();
        solidified = temperature < transfer.material().thermal().meltingPoint();
        setChanged();
        if (level != null) {
            level.setBlock(
                    worldPosition,
                    getBlockState().setValue(CeramicMoldBlock.FILLED, true),
                    Block.UPDATE_CLIENTS);
        }
        sync();
    }

    public ItemStack takeOutput() {
        if (!solidified || !isFilled() || MaterialLookup.byId(materialId).isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = MaterialLookup.item(materialId, shape.form())
                .map(item -> new ItemStack(item, outputCount))
                .orElse(ItemStack.EMPTY);
        if (result.isEmpty()) {
            return result;
        }
        ItemHeat.set(result, temperature, level == null ? 0L : level.getGameTime());
        clear();
        return result;
    }

    private void clear() {
        materialId = "";
        outputCount = 0;
        temperature = AMBIENT_TEMPERATURE;
        solidified = false;
        setChanged();
        if (level != null) {
            level.setBlock(
                    worldPosition,
                    getBlockState().setValue(CeramicMoldBlock.FILLED, false),
                    Block.UPDATE_CLIENTS);
        }
        sync();
    }

    public ItemStack moldStack() {
        return new ItemStack(ModItems.moldItem(shape).get());
    }

    public MoldShape shape() {
        return shape;
    }

    public String materialId() {
        return materialId;
    }

    public int outputCount() {
        return outputCount;
    }

    public float temperature() {
        return temperature;
    }

    public boolean isSolidified() {
        return solidified;
    }

    public boolean isFilled() {
        return !materialId.isEmpty() && outputCount > 0;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        try {
            shape = MoldShape.parse(tag.getString("shape"));
        } catch (IllegalArgumentException exception) {
            shape = MoldShape.INGOT;
        }
        materialId = tag.getString("material");
        outputCount = Math.max(0, tag.getInt("output_count"));
        temperature = tag.contains("temperature") ? tag.getFloat("temperature") : AMBIENT_TEMPERATURE;
        solidified = tag.getBoolean("solidified");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("shape", shape.serializedName());
        tag.putString("material", materialId);
        tag.putInt("output_count", outputCount);
        tag.putFloat("temperature", temperature);
        tag.putBoolean("solidified", solidified);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(
            Connection connection,
            ClientboundBlockEntityDataPacket packet,
            HolderLookup.Provider registries) {
        CompoundTag tag = packet.getTag();
        if (tag != null) {
            loadAdditional(tag, registries);
        }
    }

    private void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }
}
