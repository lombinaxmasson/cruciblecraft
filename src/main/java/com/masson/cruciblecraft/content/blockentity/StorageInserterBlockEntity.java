package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.content.block.StorageHostBlock;
import com.masson.cruciblecraft.content.block.StorageInserterBlock;
import com.masson.cruciblecraft.content.storage.StorageVariant;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Player-activated GT6 storage inserter. Not a per-tick hopper. Scan: down 6
 * columns of height 8, then 50-radius cardinal mass-storage columns.
 */
public final class StorageInserterBlockEntity extends BlockEntity {
    public static final int DOWN_COLUMNS = 6;
    public static final int COLUMN_HEIGHT = 8;
    public static final int HORIZONTAL_RADIUS = 50;
    private final StorageVariant variant;
    private int lastInserted;
    private boolean lastFailed;

    public StorageInserterBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.STORAGE_INSERTER.get(), pos, state, variantOf(state));
    }

    public StorageInserterBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state,
            StorageVariant variant) {
        super(type, pos, state);
        this.variant = variant;
    }

    public StorageVariant variant() {
        return variant;
    }

    public int lastInserted() {
        return lastInserted;
    }

    public boolean lastFailed() {
        return lastFailed;
    }

    public int insertFromPlayer(Player player) {
        lastInserted = 0;
        lastFailed = true;
        if (level == null || level.isClientSide) {
            return 0;
        }
        Direction facing = getBlockState().getValue(StorageHostBlock.FACING);
        BlockPos origin = worldPosition.relative(facing);
        boolean found = false;
        found |= scanColumn(player, origin);
        for (int down = 1; down <= DOWN_COLUMNS; down++) {
            found |= scanColumn(player, origin.below(down));
        }
        boolean[] continueDir = {true, true, true, true};
        Direction[] dirs = {
                Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH};
        for (int radius = 1; radius <= HORIZONTAL_RADIUS; radius++) {
            boolean any = false;
            for (int index = 0; index < dirs.length; index++) {
                if (!continueDir[index]) {
                    continue;
                }
                any = true;
                BlockPos column = origin.relative(dirs[index], radius);
                if (!scanColumn(player, column.below(DOWN_COLUMNS))) {
                    continueDir[index] = false;
                }
            }
            if (!any) {
                break;
            }
        }
        lastFailed = !found && lastInserted == 0;
        setChanged();
        return lastInserted;
    }

    private boolean scanColumn(Player player, BlockPos floor) {
        if (level == null) {
            return false;
        }
        boolean found = false;
        for (int up = 0; up < COLUMN_HEIGHT; up++) {
            BlockPos pos = floor.above(up);
            if (!(level.getBlockEntity(pos) instanceof MassStorageBlockEntity storage)) {
                continue;
            }
            found = true;
            lastInserted += insertInto(player, storage);
        }
        return found;
    }

    private int insertInto(Player player, MassStorageBlockEntity storage) {
        int moved = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty() || !storage.inventory().sameType(stack)) {
                continue;
            }
            ItemStack leftover = storage.inventory().insertAll(stack.copy(), false);
            int taken = stack.getCount() - leftover.getCount();
            if (taken <= 0) {
                continue;
            }
            stack.shrink(taken);
            moved += taken;
            storage.setChanged();
        }
        return moved;
    }

    private static StorageVariant variantOf(BlockState state) {
        if (state.getBlock() instanceof StorageInserterBlock block) {
            return block.variant();
        }
        throw new IllegalStateException("Inserter BE missing host variant");
    }
}
