package com.masson.cruciblecraft.content.block;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Shared facing/menu/ticker block for immutable configured processing specs. */
public final class ProcessingMachineBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private final ProcessingMachineSpec spec;

    public ProcessingMachineBlock(ProcessingMachineSpec spec, Properties properties) {
        super(properties);
        this.spec = spec;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public ProcessingMachineSpec spec() { return spec; }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        var contained = FluidUtil.getFluidContained(stack);
        var transfer = ProcessingMachineInteractions.fluidTransfer(
                spec,
                state.getValue(FACING),
                hit.getDirection(),
                player.isShiftKeyDown(),
                FluidUtil.getFluidHandler(stack).isPresent(),
                contained.isPresent() && !contained.get().isEmpty());
        if (transfer == ProcessingMachineInteractions.FluidTransfer.NONE) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.getBlockEntity(pos) instanceof ConfiguredProcessingMachineBlockEntity machine) {
            IFluidHandler handler = transfer == ProcessingMachineInteractions.FluidTransfer.FILL
                    ? machine.fluids(hit.getDirection())
                    : machine.maintenanceFluids();
            boolean possible = handler != null && (transfer
                    == ProcessingMachineInteractions.FluidTransfer.FILL
                            ? contained.isPresent()
                                    && handler.fill(
                                            contained.get(),
                                            IFluidHandler.FluidAction.SIMULATE) > 0
                            : !handler.drain(
                                    Integer.MAX_VALUE,
                                    IFluidHandler.FluidAction.SIMULATE).isEmpty());
            if (!possible) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (level.isClientSide
                    || FluidUtil.interactWithFluidHandler(player, hand, handler)) {
                return ItemInteractionResult.SUCCESS;
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer server
                && level.getBlockEntity(pos) instanceof ConfiguredProcessingMachineBlockEntity machine) {
            server.openMenu(machine);
        }
        return InteractionResult.SUCCESS;
    }

    @Override protected void onRemove(
            BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (state.getBlock() != next.getBlock()
                && level.getBlockEntity(pos) instanceof ConfiguredProcessingMachineBlockEntity machine) {
            machine.dropContents();
        }
        super.onRemove(state, level, pos, next, moved);
    }

    @Override protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ConfiguredProcessingMachineBlockEntity(pos, state);
    }

    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide && type == ModBlockEntities.PROCESSING_MACHINE.get()
                ? (l, p, s, be) -> ConfiguredProcessingMachineBlockEntity.serverTick(
                        l, p, s, (ConfiguredProcessingMachineBlockEntity) be)
                : null;
    }
}
