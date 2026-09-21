package com.masson.cruciblecraft.crops;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class CropBlockEntity extends BlockEntity {
    private String cardId = "";
    private int size;

    public CropBlockEntity(BlockPos pos, BlockState state) {
        super(CropRegistries.CROP_ENTITY.get(), pos, state);
    }

    public String cardId() {
        return cardId;
    }

    public int size() {
        return size;
    }

    public void plant(CropCard card) {
        cardId = card.id();
        size = 1;
        setChanged();
        syncAge();
    }

    public void setSizeForTest(int value) {
        size = value;
        setChanged();
        syncAge();
    }

    public boolean harvest(Player player) {
        if (level == null || level.isClientSide || cardId.isEmpty()) {
            return false;
        }
        CropCard card = CropCatalog.find(cardId).orElse(null);
        if (card == null || !card.canHarvest(size)) {
            return false;
        }
        card.harvestDrop().ifPresent(drop -> give(player, drop));
        for (ItemStack extra : card.extraDrops(player.getRandom())) {
            give(player, extra);
        }
        size = card.afterHarvestSize();
        setChanged();
        syncAge();
        return true;
    }

    public static void serverTick(
            Level level, BlockPos pos, BlockState state, CropBlockEntity crop) {
        if (level.getGameTime() % 20L != 0L) {
            return;
        }
        CropCatalog.freezeCards();
        if (crop.cardId.isEmpty()) {
            crop.tryCrossbreed(level, pos);
            return;
        }
        CropCard card = CropCatalog.find(crop.cardId).orElse(null);
        if (card == null) {
            return;
        }
        if (card.canGrow(crop.size) && level.random.nextInt(8 + card.tier()) == 0) {
            crop.size += 1;
            crop.setChanged();
            crop.syncAge();
        }
        if (card.canCross(crop.size)) {
            crop.tryCrossbreed(level, pos);
        }
    }

    private void tryCrossbreed(Level level, BlockPos pos) {
        if (level.random.nextInt(16) != 0) {
            return;
        }
        for (Direction direction : new Direction[] {
            Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
        }) {
            BlockPos neighbor = pos.relative(direction);
            if (!(level.getBlockEntity(neighbor) instanceof CropBlockEntity other)) {
                continue;
            }
            if (!cardId.isEmpty() || other.cardId.isEmpty()) {
                continue;
            }
            CropCard parent = CropCatalog.find(other.cardId).orElse(null);
            if (parent == null || !parent.canCross(other.size)) {
                continue;
            }
            List<CropCard> options = CropCatalog.sharingAttribute(parent);
            CropCard child = options.isEmpty()
                    ? parent
                    : options.get(level.random.nextInt(options.size()));
            plant(child);
            return;
        }
    }

    private void syncAge() {
        if (level == null) {
            return;
        }
        int age = cardId.isEmpty() ? 0 : Math.min(7, Math.max(1, size));
        BlockState state = getBlockState();
        if (state.getValue(CropStickBlock.AGE) != age) {
            level.setBlock(worldPosition, state.setValue(CropStickBlock.AGE, age), 3);
        }
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    private static void give(Player player, ItemStack stack) {
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("card", cardId);
        tag.putInt("size", size);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        cardId = tag.getString("card");
        size = tag.getInt("size");
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
