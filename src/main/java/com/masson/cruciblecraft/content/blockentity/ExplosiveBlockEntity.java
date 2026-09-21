package com.masson.cruciblecraft.content.blockentity;

import java.util.List;

import com.masson.cruciblecraft.api.tool.RemoteActivatable;
import com.masson.cruciblecraft.content.block.ExplosiveBlock;
import com.masson.cruciblecraft.content.explosive.DynamiteType;
import com.masson.cruciblecraft.content.item.tool.DynamitePlacement;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;

/**
 * Shared runtime for Boomstick, Dynamite, and Strong Dynamite.
 *
 * <p>The fuse timings and the resistance-capped cube are taken from the local
 * GT6 {@code MultiTileEntityDynamite} source. This deliberately does not call
 * {@link ServerLevel#explode} because vanilla explosion rays do not reproduce
 * GT6's 3x3x3 selection rule.</p>
 */
public final class ExplosiveBlockEntity extends BlockEntity
        implements RemoteActivatable {
    private static final String FUSE_TAG = "fuse";
    private static final String SUNK_TAG = "sunk";
    private static final int IGNITER_FUSE = 100;
    private static final int REMOTE_FUSE = 20;

    private int fuse;
    private boolean sunk;
    private boolean detonating;

    public ExplosiveBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DYNAMITE.get(), pos, state);
    }

    public DynamiteType dynamiteType() {
        return ((ExplosiveBlock) getBlockState().getBlock()).type();
    }

    public boolean isLit() {
        return fuse > 0;
    }

    public boolean isSunk() {
        return sunk;
    }

    public void setSunk(boolean sunk) {
        this.sunk = sunk;
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = level.getBlockState(worldPosition);
            level.sendBlockUpdated(
                    worldPosition,
                    state,
                    state,
                    Block.UPDATE_CLIENTS);
        }
    }

    public int fuse() {
        return fuse;
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            ExplosiveBlockEntity explosive) {
        if (level.isClientSide || explosive.detonating) {
            return;
        }
        if (explosive.sunk) {
            BlockPos hostPos = pos.relative(
                    state.getValue(ExplosiveBlock.FACING).getOpposite());
            if (!DynamitePlacement.isDrillableHost(
                    level.getBlockState(hostPos),
                    level,
                    hostPos)) {
                explosive.setSunk(false);
            }
        }
        if (explosive.fuse > 0) {
            explosive.fuse--;
            explosive.setChanged();
            if (explosive.fuse == 0) {
                explosive.explode(false);
            } else {
                explosive.syncLitState();
            }
        }
    }

    /**
     * Starts the long, tool-triggered GT6 fuse.
     */
    public boolean ignite() {
        if (fuse != 0 || level == null || level.isClientSide) {
            return false;
        }
        if (!level.getFluidState(worldPosition).isEmpty()) {
            return false;
        }
        setFuse(IGNITER_FUSE);
        level.playSound(
                null,
                worldPosition,
                SoundEvents.TNT_PRIMED,
                SoundSource.BLOCKS,
                1.0F,
                0.5F);
        return true;
    }

    /**
     * Starts the short redstone/fire/remote fuse. GT6 dynamite is removed
     * from a remote's coordinate list after this call, hence the false result.
     */
    @Override
    public boolean remoteActivate() {
        if (level == null || level.isClientSide || detonating) {
            return false;
        }
        if (fuse == 0 || fuse > REMOTE_FUSE) {
            setFuse(REMOTE_FUSE);
            level.playSound(
                    null,
                    worldPosition,
                    SoundEvents.TNT_PRIMED,
                    SoundSource.BLOCKS,
                    1.0F,
                    0.65F);
        }
        return false;
    }

    /**
     * Used when a neighboring explosive is caught by the same blast.
     */
    public void explodeInstant() {
        if (level != null && !level.isClientSide) {
            explode(true);
        }
    }

    private void setFuse(int nextFuse) {
        fuse = Math.max(0, nextFuse);
        syncLitState();
        setChanged();
    }

    private void syncLitState() {
        if (level == null) {
            return;
        }
        BlockState current = level.getBlockState(worldPosition);
        if (current.getBlock() == getBlockState().getBlock()
                && current.hasProperty(ExplosiveBlock.LIT)
                && current.getValue(ExplosiveBlock.LIT) != (fuse > 0)) {
            level.setBlock(
                    worldPosition,
                    current.setValue(ExplosiveBlock.LIT, fuse > 0),
                    Block.UPDATE_ALL);
            level.updateNeighborsAt(worldPosition, current.getBlock());
        }
    }

    private void explode(boolean instant) {
        if (!(level instanceof ServerLevel server) || detonating) {
            return;
        }
        detonating = true;
        fuse = 0;
        BlockState state = getBlockState();
        DynamiteType type = dynamiteType();
        BlockPos origin = sunk
                ? worldPosition.relative(
                        state.getValue(ExplosiveBlock.FACING).getOpposite())
                : worldPosition;
        level.removeBlock(worldPosition, false);
        detonateCube(server, origin, type);
    }

    private void detonateCube(
            ServerLevel server,
            BlockPos origin,
            DynamiteType type) {
        float resistanceLimit = type.blastResistance();

        ItemStack fortuneTool = fortuneTool(server, type.fortune());
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos targetPos = origin.offset(x, y, z);
                    BlockState targetState = server.getBlockState(targetPos);
                    if (targetState.isAir()
                            || targetState.is(Blocks.BEDROCK)
                            || targetState.is(Blocks.SPAWNER)) {
                        continue;
                    }

                    BlockEntity targetEntity = server.getBlockEntity(targetPos);
                    if (targetEntity instanceof ExplosiveBlockEntity explosive) {
                        explosive.explodeInstant();
                        continue;
                    }

                    float resistance = targetState.getBlock().getExplosionResistance(
                            targetState,
                            server,
                            targetPos,
                            null);
                    if (resistance > resistanceLimit) {
                        continue;
                    }

                    List<ItemStack> drops = Block.getDrops(
                            targetState,
                            server,
                            targetPos,
                            targetEntity,
                            null,
                            fortuneTool);
                    server.setBlock(
                            targetPos,
                            Blocks.AIR.defaultBlockState(),
                            Block.UPDATE_ALL);
                    for (ItemStack drop : drops) {
                        if (!drop.isEmpty()) {
                            server.addFreshEntity(new ItemEntity(
                                    server,
                                    targetPos.getX() + 0.5D,
                                    targetPos.getY() + 0.5D,
                                    targetPos.getZ() + 0.5D,
                                    drop));
                        }
                    }
                }
            }
        }

        Vec3 center = Vec3.atCenterOf(origin);
        AABB entities = new AABB(
                center.x - 2.0D,
                center.y - 2.0D,
                center.z - 2.0D,
                center.x + 2.0D,
                center.y + 2.0D,
                center.z + 2.0D);
        for (Entity entity : server.getEntities(null, entities)) {
            if (!(entity instanceof ItemEntity)) {
                entity.hurt(
                        server.damageSources().explosion(null),
                        2.0F * resistanceLimit);
            }
        }
        server.playSound(
                null,
                origin,
                SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.BLOCKS,
                4.0F,
                0.8F + server.random.nextFloat() * 0.2F);
        server.sendParticles(
                ParticleTypes.EXPLOSION_EMITTER,
                center.x,
                center.y,
                center.z,
                1,
                0.0D,
                0.0D,
                0.0D,
                0.0D);
        server.sendParticles(
                ParticleTypes.SMOKE,
                center.x,
                center.y,
                center.z,
                12,
                1.0D,
                1.0D,
                1.0D,
                0.04D);
    }

    private static ItemStack fortuneTool(ServerLevel server, int fortune) {
        if (fortune <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack tool = new ItemStack(Items.NETHERITE_PICKAXE);
        Holder<Enchantment> enchantment = server.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.FORTUNE);
        tool.enchant(enchantment, fortune);
        return tool;
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(FUSE_TAG, fuse);
        tag.putBoolean(SUNK_TAG, sunk);
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fuse = Math.max(0, tag.getInt(FUSE_TAG));
        sunk = tag.getBoolean(SUNK_TAG);
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
