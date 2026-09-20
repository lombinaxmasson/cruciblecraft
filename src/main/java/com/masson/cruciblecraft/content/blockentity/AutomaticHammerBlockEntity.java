package com.masson.cruciblecraft.content.blockentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.api.tool.ToolInteractable;
import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.content.block.AnvilInteractions;
import com.masson.cruciblecraft.content.block.AutomaticHammerBlock;
import com.masson.cruciblecraft.content.item.tool.HammerCrush;
import com.masson.cruciblecraft.content.item.tool.ToolMining;
import com.masson.cruciblecraft.energy.EnergyPackets;
import com.masson.cruciblecraft.machine.autotool.AutomaticHammerCatalog;
import com.masson.cruciblecraft.recipe.AnvilMode;
import com.masson.cruciblecraft.recipe.AnvilStrikeContext;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * GT6 {@code MultiTileEntityAutoToolHammer}. Positive KU stores the piston
 * charge with no packet cap on the add; oversize still stores, then
 * {@code explode(tierMax(size))} on the next tick. The return stroke
 * (negative size) arms the front-face hit. Steam and electric engines
 * already emit that signed pair.
 */
public final class AutomaticHammerBlockEntity extends BlockEntity
        implements IEnergyHandler {
    private final AutomaticHammerCatalog.Profile profile;
    private long stored;
    private boolean pullingBack;
    private float pendingExplosion;

    public AutomaticHammerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AUTOMATIC_HAMMER.get(), pos, state);
        this.profile = ((AutomaticHammerBlock) state.getBlock()).profile();
    }

    public AutomaticHammerCatalog.Profile profile() {
        return profile;
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            AutomaticHammerBlockEntity hammer) {
        if (level.isClientSide) {
            return;
        }
        if (hammer.pendingExplosion > 0.0F) {
            hammer.detonate(level, pos);
            return;
        }
        if (!hammer.pullingBack || hammer.stored <= 0L) {
            return;
        }
        Direction facing = state.getValue(AutomaticHammerBlock.FACING);
        BlockPos targetPos = pos.relative(facing);
        int sound = hammer.strike(level, targetPos, facing);
        hammer.stored = 0L;
        if (sound != 0) {
            level.playSound(
                    null,
                    pos,
                    sound == 1 ? SoundEvents.ANVIL_USE : SoundEvents.ANVIL_BREAK,
                    SoundSource.BLOCKS,
                    0.6F,
                    1.0F);
        }
        hammer.setChanged();
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        return type == EnergyType.KINETIC_PUSH
                && side == getBlockState()
                        .getValue(AutomaticHammerBlock.FACING)
                        .getOpposite();
    }

    @Override
    public long insert(
            EnergyType type,
            long size,
            long amount,
            Direction side,
            boolean simulate) {
        if (!handles(type, side) || amount <= 0L || size == 0L) {
            return 0L;
        }
        if (size < 0L) {
            if (!simulate) {
                pullingBack = true;
                setChanged();
            }
            return amount;
        }
        if (!simulate) {
            pullingBack = false;
            stored = EnergyPackets.add(stored, EnergyPackets.units(size, amount));
            long magnitude = EnergyPackets.magnitude(size);
            if (magnitude > profile.maxPacket()) {
                pendingExplosion = Math.max(
                        pendingExplosion,
                        AutomaticHammerCatalog.overchargeExplosionStrength(
                                magnitude));
            }
            setChanged();
        }
        return amount;
    }

    @Override
    public long stored(EnergyType type) {
        return type == EnergyType.KINETIC_PUSH ? stored : 0L;
    }

    @Override
    public long capacity(EnergyType type) {
        return type == EnergyType.KINETIC_PUSH ? profile.capacity() : 0L;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("energy", stored);
        tag.putBoolean("pulling_back", pullingBack);
        tag.putFloat("pending_explosion", pendingExplosion);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        stored = Math.max(0L, tag.getLong("energy"));
        pullingBack = tag.getBoolean("pulling_back");
        pendingExplosion = Math.max(0.0F, tag.getFloat("pending_explosion"));
    }

    /**
     * GT6 delayed {@code explode}: remove the machine first, then a world
     * blast only when {@code tierMax} is at least 1.
     */
    private void detonate(Level level, BlockPos pos) {
        float strength = pendingExplosion;
        pendingExplosion = 0.0F;
        level.removeBlock(pos, false);
        if (strength >= 1.0F && level instanceof ServerLevel server) {
            server.explode(
                    null,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    strength,
                    Level.ExplosionInteraction.TNT);
        }
    }

    /**
     * @return 1 GT6 anvil-use, 2 anvil-break (attempted mine), 0 silent
     */
    private int strike(Level level, BlockPos targetPos, Direction facing) {
        BlockEntity target = level.getBlockEntity(targetPos);
        BlockState state = level.getBlockState(targetPos);
        if (target instanceof AnvilBlockEntity anvil) {
            return anvil.strike(anvilMode(state, targetPos, facing), profile.quality())
                            .isPresent()
                    ? 1
                    : 0;
        }
        if (state.getBlock() instanceof ToolInteractable interactable) {
            BlockHitResult hit = new BlockHitResult(
                    Vec3.atCenterOf(targetPos),
                    facing.getOpposite(),
                    targetPos,
                    false);
            UseOnContext context = new UseOnContext(
                    level,
                    null,
                    InteractionHand.MAIN_HAND,
                    ItemStack.EMPTY,
                    hit);
            ToolResult result = interactable.useTool(
                    ToolAction.HAMMER,
                    context,
                    EnergyPackets.units(stored, 10L),
                    profile.quality());
            if (result == ToolResult.SUCCESS) {
                return 1;
            }
            if (result == ToolResult.REJECT) {
                return 0;
            }
        }
        if (state.isAir() || !ToolMining.isHammerMineable(state)) {
            return 0;
        }
        if (!qualityCanHarvest(state)) {
            return 2;
        }
        float hardness = state.getDestroySpeed(level, targetPos);
        if (hardness < 0.0F || hardness * 50.0F > stored) {
            return 2;
        }
        if (!(level instanceof ServerLevel server)) {
            return 0;
        }
        List<ItemStack> drops = convertedDrops(state, server, targetPos, target);
        server.destroyBlock(targetPos, false);
        for (ItemStack drop : drops) {
            if (!drop.isEmpty()) {
                server.addFreshEntity(new ItemEntity(
                        server,
                        targetPos.getX() + 0.5,
                        targetPos.getY() + 0.5,
                        targetPos.getZ() + 0.5,
                        drop));
            }
        }
        return 1;
    }

    /**
     * GT6 {@code convertBlockDrops}: a hammer recipe on the block replaces
     * vanilla drops; otherwise each drop is converted. Live conversion is
     * {@link HammerCrush} until the hammer map is imported.
     */
    private List<ItemStack> convertedDrops(
            BlockState state,
            ServerLevel server,
            BlockPos targetPos,
            BlockEntity target) {
        Optional<ItemStack> fromBlock = HammerCrush.outputForBlock(state, target)
                .or(() -> HammerCrush.convert(new ItemStack(state.getBlock())));
        if (fromBlock.isPresent()) {
            return List.of(fromBlock.get());
        }
        List<ItemStack> drops = new ArrayList<>(
                Block.getDrops(state, server, targetPos, target));
        for (int i = 0; i < drops.size(); i++) {
            ItemStack original = drops.get(i);
            drops.set(i, HammerCrush.convert(original).orElse(original));
        }
        return drops;
    }

    /**
     * GT6 {@code IBlockToolable} with a null player still enters the anvil
     * recipe path on every face; only horizontal faces switch to bend maps.
     * Bottom hits therefore keep {@link AnvilMode#ANVIL}.
     */
    private static AnvilMode anvilMode(
            BlockState state, BlockPos targetPos, Direction facing) {
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(targetPos),
                facing.getOpposite(),
                targetPos,
                false);
        AnvilStrikeContext context = AnvilInteractions.strikeContext(
                state, targetPos, hit);
        return context.mode().orElse(AnvilMode.ANVIL);
    }

    private boolean qualityCanHarvest(BlockState state) {
        int required = 0;
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) {
            required = 3;
        } else if (state.is(BlockTags.NEEDS_IRON_TOOL)) {
            required = 2;
        } else if (state.is(BlockTags.NEEDS_STONE_TOOL)) {
            required = 1;
        }
        return profile.quality() >= required;
    }
}
