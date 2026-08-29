package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.air.AirOutputModel;
import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.block.BellowsBlock;
import com.masson.cruciblecraft.energy.EnergyEmitter;
import com.masson.cruciblecraft.energy.PerTickEnergyBudget;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public final class BellowsBlockEntity extends BlockEntity implements IEnergyHandler {
    private long activeUntil = Long.MIN_VALUE;
    private int loadedRemainingTicks;
    private final PerTickEnergyBudget outputBudget = new PerTickEnergyBudget();

    public BellowsBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.BELLOWS.get(), pos, blockState);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            BellowsBlockEntity bellows) {
        bellows.initializeDeadline();
        if (bellows.isActive()) {
            EnergyEmitter.emit(
                    level,
                    pos,
                    bellows,
                    EnergyType.AIR,
                    state.getValue(BellowsBlock.FACING));
        }
        if (state.getValue(BellowsBlock.ACTIVE) && !bellows.isActive()) {
            level.setBlock(
                    pos,
                    state.setValue(BellowsBlock.ACTIVE, false),
                    Block.UPDATE_CLIENTS);
            bellows.setChanged();
        }
    }

    public boolean startStroke() {
        if (level == null || level.isClientSide || isActive()) {
            return false;
        }
        activeUntil = level.getGameTime() + AirOutputModel.BELLOWS_STROKE_TICKS;
        loadedRemainingTicks = 0;
        if (!getBlockState().getValue(BellowsBlock.ACTIVE)) {
            level.setBlock(
                    worldPosition,
                    getBlockState().setValue(BellowsBlock.ACTIVE, true),
                    Block.UPDATE_CLIENTS);
        }
        setChanged();
        return true;
    }

    public boolean isActive() {
        initializeDeadline();
        return level != null && level.getGameTime() < activeUntil;
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        BlockState state = getBlockState();
        return type == EnergyType.AIR
                && side != null
                && state.hasProperty(BellowsBlock.FACING)
                && side == state.getValue(BellowsBlock.FACING);
    }

    @Override
    public long outputSize(EnergyType type, Direction side) {
        return handles(type, side)
                        && isActive()
                        && outputBudget.claim(
                                gameTime(),
                                AirOutputModel.BELLOWS_AIR_PER_TICK,
                                1L,
                                true) > 0L
                ? 1L
                : 0L;
    }

    @Override
    public long extract(
            EnergyType type,
            long size,
            long maxAmount,
            Direction side,
            boolean simulate) {
        if (!handles(type, side) || size != 1L || maxAmount <= 0L || !isActive()) {
            return 0L;
        }
        boolean effectiveSimulation = simulate || level == null || level.isClientSide;
        return outputBudget.claim(
                gameTime(),
                AirOutputModel.BELLOWS_AIR_PER_TICK,
                maxAmount,
                effectiveSimulation);
    }

    /**
     * Exposes the mechanical air emitted by an active bellows as the registered
     * air gas, so it can be piped into recipes that explicitly consume air.
     * The shared per-tick budget prevents simultaneous energy and fluid pulls
     * from duplicating a stroke's output.
     */
    public IFluidHandler fluids(Direction side) {
        return side != null && handles(EnergyType.AIR, side)
                ? new AirFluidHandler(side)
                : null;
    }

    private long gameTime() {
        if (level == null) {
            outputBudget.reset();
            return 0L;
        }
        return level.getGameTime();
    }

    private void initializeDeadline() {
        if (activeUntil == Long.MIN_VALUE && level != null) {
            activeUntil = level.getGameTime() + loadedRemainingTicks;
            loadedRemainingTicks = 0;
        }
    }

    private final class AirFluidHandler implements IFluidHandler {
        private final Direction side;

        private AirFluidHandler(Direction side) {
            this.side = side;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            if (tank != 0) {
                return FluidStack.EMPTY;
            }
            return airStack(available());
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0
                    ? Math.toIntExact(AirOutputModel.BELLOWS_AIR_PER_TICK)
                    : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            Fluid air = air();
            if (air == null || resource.isEmpty() || resource.getFluid() != air) {
                return FluidStack.EMPTY;
            }
            return airStack(drainAmount(resource.getAmount(), action));
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return airStack(drainAmount(maxDrain, action));
        }

        private int available() {
            if (!handles(EnergyType.AIR, side) || !isActive()) {
                return 0;
            }
            return (int) outputBudget.claim(
                    gameTime(),
                    AirOutputModel.BELLOWS_AIR_PER_TICK,
                    AirOutputModel.BELLOWS_AIR_PER_TICK,
                    true);
        }

        private int drainAmount(int requested, FluidAction action) {
            if (requested <= 0) {
                return 0;
            }
            return (int) extract(
                    EnergyType.AIR,
                    1L,
                    requested,
                    side,
                    action == FluidAction.SIMULATE);
        }

        private FluidStack airStack(int amount) {
            Fluid air = air();
            return air == null || amount <= 0
                    ? FluidStack.EMPTY
                    : new FluidStack(air, amount);
        }

        private Fluid air() {
            return ModFluids.materialFluid("air").orElse(null);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadedRemainingTicks = Math.max(
                0,
                Math.min(
                        AirOutputModel.BELLOWS_STROKE_TICKS,
                        tag.getInt("stroke_ticks_remaining")));
        activeUntil = Long.MIN_VALUE;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        int remaining = loadedRemainingTicks;
        if (level != null && activeUntil != Long.MIN_VALUE) {
            remaining = (int) Math.max(
                    0L,
                    Math.min(
                            AirOutputModel.BELLOWS_STROKE_TICKS,
                            activeUntil - level.getGameTime()));
        }
        tag.putInt("stroke_ticks_remaining", remaining);
    }
}
