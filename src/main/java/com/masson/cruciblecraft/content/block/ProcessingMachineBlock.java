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
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

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
        var fluidContainer = FluidUtil.getFluidHandler(stack);
        if (fluidContainer.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        IFluidHandlerItem container = fluidContainer.orElseThrow();
        var transfer = ProcessingMachineInteractions.fluidTransfer(
                spec,
                state.getValue(FACING),
                hit.getDirection(),
                player.isShiftKeyDown(),
                true,
                containsFluid(container),
                hasRemainingCapacity(container));
        if (transfer == ProcessingMachineInteractions.FluidTransfer.NONE) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.getBlockEntity(pos) instanceof ConfiguredProcessingMachineBlockEntity machine) {
            IFluidHandler handler = transfer
                    == ProcessingMachineInteractions.FluidTransfer.FILL_INPUT
                    ? machine.fluids(hit.getDirection())
                    : machine.playerDrainFluids(hit.getDirection());
            boolean possible = handler != null && canTransfer(
                    transfer, container, handler);
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

    private static boolean containsFluid(IFluidHandler container) {
        for (int tank = 0; tank < container.getTanks(); tank++) {
            if (!container.getFluidInTank(tank).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasRemainingCapacity(IFluidHandler container) {
        for (int tank = 0; tank < container.getTanks(); tank++) {
            if (container.getFluidInTank(tank).getAmount()
                    < container.getTankCapacity(tank)) {
                return true;
            }
        }
        return false;
    }

    private static boolean canTransfer(
            ProcessingMachineInteractions.FluidTransfer transfer,
            IFluidHandlerItem container,
            IFluidHandler machine) {
        if (transfer == ProcessingMachineInteractions.FluidTransfer.FILL_INPUT) {
            var offered = container.drain(
                    Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
            return !offered.isEmpty()
                    && machine.fill(offered, IFluidHandler.FluidAction.SIMULATE) > 0;
        }
        var available = machine.drain(
                Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
        return !available.isEmpty()
                && container.fill(available, IFluidHandler.FluidAction.SIMULATE) > 0;
    }

    @Override protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer server
                && level.getBlockEntity(pos) instanceof ConfiguredProcessingMachineBlockEntity machine) {
            server.openMenu(machine, data -> data.writeBlockPos(pos));
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
