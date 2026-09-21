package com.masson.cruciblecraft.content.blockentity;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.content.block.GtBushBlock;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6 {@code MultiTileEntityBush} meta 32759. Default berry is sweet berries,
 * never string.
 */
public final class GtBushBlockEntity extends BlockEntity {
    public static final ItemStack DEFAULT_BERRY = new ItemStack(Items.SWEET_BERRIES);
    private ItemStack berry = DEFAULT_BERRY.copy();
    private byte growth;

    public GtBushBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GT_BUSH.get(), pos, state);
    }

    public ItemStack berry() {
        return berry.copy();
    }

    public void setBerry(ItemStack stack) {
        berry = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        setChanged();
        sync();
    }

    public static boolean isBerrySetter(ItemStack stack) {
        if (stack.isEmpty() || stack.is(Items.STRING)) {
            return false;
        }
        if (stack.is(Items.SWEET_BERRIES) || stack.is(Items.GLOW_BERRIES)) {
            return true;
        }
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(
                stack.getItem());
        if (id.getPath().equals("plant_gt_berry")
                || id.getPath().endsWith("/plant_gt_berry")) {
            return true;
        }
        return stack.is(com.masson.cruciblecraft.api.agriculture.AgricultureTags.GT_BUSH_BERRIES);
    }

    public boolean harvest(Player player) {
        if (berry.isEmpty() || stage() < GtBushBlock.MAX_STAGE) {
            return false;
        }
        int count = 1 + player.getRandom().nextInt(2);
        ItemStack drop = berry.copyWithCount(count);
        if (!player.addItem(drop)) {
            player.drop(drop, false);
        }
        setStage(0);
        growth = 0;
        return true;
    }

    public int stage() {
        return getBlockState().getValue(GtBushBlock.STAGE);
    }

    public void setStage(int stage) {
        if (level == null) {
            return;
        }
        int clamped = Math.max(0, Math.min(GtBushBlock.MAX_STAGE, stage));
        BlockState state = getBlockState();
        if (state.getValue(GtBushBlock.STAGE) == clamped) {
            return;
        }
        level.setBlock(worldPosition, state.setValue(GtBushBlock.STAGE, clamped), 3);
        setChanged();
    }

    public static void serverTick(
            Level level, BlockPos pos, BlockState state, GtBushBlockEntity bush) {
        if (level.getGameTime() % 128L != 0L || bush.berry.isEmpty()) {
            return;
        }
        if (bush.stage() >= GtBushBlock.MAX_STAGE) {
            return;
        }
        bush.growth++;
        if (bush.growth == 0) {
            bush.setStage(bush.stage() + 1);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte("Growth", growth);
        if (!berry.isEmpty()) {
            tag.put("Berry", berry.save(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        growth = tag.getByte("Growth");
        berry = tag.contains("Berry")
                ? ItemStack.parseOptional(registries, tag.getCompound("Berry"))
                : ItemStack.EMPTY;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    @Nullable
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
