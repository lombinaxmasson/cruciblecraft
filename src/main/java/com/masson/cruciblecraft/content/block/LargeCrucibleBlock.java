package com.masson.cruciblecraft.content.block;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.content.blockentity.CrucibleEntityMelts;
import com.masson.cruciblecraft.content.blockentity.CruciblePlayerInteraction;
import com.masson.cruciblecraft.content.blockentity.LargeCrucibleBlockEntity;
import com.masson.cruciblecraft.heat.TemperatureDamage;
import com.masson.cruciblecraft.machine.component.CrucibleProcessCore;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModComponents;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

/** Facing/ticker shell for the JSON large-crucible thermal controller. */
public final class LargeCrucibleBlock extends Block implements EntityBlock {
    public LargeCrucibleBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(
                ProcessingMachineBlock.FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(
                ProcessingMachineBlock.FACING,
                context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof LargeCrucibleBlockEntity crucible)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return CruciblePlayerInteraction.useItemOn(
                level,
                player,
                hand,
                stack,
                hitResult.getDirection(),
                crucible.process(),
                crucible.inventory(),
                crucible.process().fluids(),
                crucible.structureValid() && !crucible.pluginQuarantined());
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof LargeCrucibleBlockEntity crucible)) {
            return InteractionResult.PASS;
        }
        return CruciblePlayerInteraction.useEmpty(
                level,
                player,
                hitResult.getDirection(),
                crucible.process(),
                crucible.inventory(),
                crucible.structureValid() && !crucible.pluginQuarantined());
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        applyHotContact(level, pos, entity);
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        applyHotContact(level, pos, entity);
    }

    static void applyHotContact(Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide
                || !(level.getBlockEntity(pos) instanceof LargeCrucibleBlockEntity crucible)
                || !crucible.structureValid()) {
            return;
        }
        float temperature = crucible.process().authoritativeTemperature();
        boolean damaged = TemperatureDamage.apply(entity, temperature, 1.0F, 10.0F);
        if (damaged && TemperatureDamage.kelvin(temperature) > 320L) {
            CrucibleEntityMelts.tryMelt(
                    crucible.process(), entity, CrucibleProcessCore.AMBIENT_TEMPERATURE);
        }
    }

    @Override
    protected float getDestroyProgress(
            BlockState state, Player player, BlockGetter level, BlockPos pos) {
        float progress = super.getDestroyProgress(state, player, level, pos);
        if (level.getBlockEntity(pos) instanceof LargeCrucibleBlockEntity crucible
                && crucible.process().totalUnits() > 0) {
            return progress / 100.0F;
        }
        return progress;
    }

    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState next,
            boolean moved) {
        if (state.getBlock() != next.getBlock()
                && level.getBlockEntity(pos)
                        instanceof LargeCrucibleBlockEntity crucible) {
            crucible.clearBindings();
        }
        super.onRemove(state, level, pos, next, moved);
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ProcessingMachineBlock.FACING);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LargeCrucibleBlockEntity(pos, state);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        if (!(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY)
                instanceof LargeCrucibleBlockEntity crucible)) {
            return drops;
        }
        for (ItemStack drop : drops) {
            if (drop.is(ModItems.LARGE_CRUCIBLE.get())) {
                drop.set(ModComponents.MACHINE_MATERIAL, crucible.process().casing().materialId());
            }
        }
        return drops;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type) {
        return type == ModBlockEntities.LARGE_CRUCIBLE.get()
                ? (l, p, s, be) -> {
                    if (be instanceof LargeCrucibleBlockEntity crucible) {
                        if (l.isClientSide) {
                            LargeCrucibleBlockEntity.clientTick(l, p, s, crucible);
                        } else {
                            LargeCrucibleBlockEntity.serverTick(l, p, s, crucible);
                        }
                    }
                }
                : null;
    }
}
