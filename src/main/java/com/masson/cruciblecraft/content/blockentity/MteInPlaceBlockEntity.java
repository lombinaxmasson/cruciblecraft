package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.content.mte.MteInPlaceSpec;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Shared host for in-place GT6 MTE families. Kind-specific transfer is not a
 * pipe cover, KU axle, vanilla tool, or the ceramic crucible.
 */
public final class MteInPlaceBlockEntity extends BlockEntity
        implements IEnergyHandler {
    public static final int TRANSFER_MB = 1000;
    public static final long ENERGY_CAPACITY = 16_384L;

    private final ItemStackHandler items;
    private final FluidTank tank;
    private long storedEnergy;
    private boolean formed;

    public MteInPlaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MTE_INPLACE.get(), pos, state);
        MteInPlaceSpec spec = specOf(state);
        int slots = Math.max(1, spec.kind().slots());
        this.items = new ItemStackHandler(slots) {
            @Override
            protected void onContentsChanged(int slot) {
                MteInPlaceBlockEntity.this.setChanged();
            }
        };
        this.tank = new FluidTank(spec.kind().foundryTank() ? 8_000 : 1) {
            @Override
            protected void onContentsChanged() {
                MteInPlaceBlockEntity.this.setChanged();
            }
        };
    }

    public MteInPlaceSpec spec() {
        return specOf(getBlockState());
    }

    public ItemStackHandler items() {
        return items;
    }

    public FluidTank tank() {
        return tank;
    }

    public boolean formed() {
        return formed;
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            MteInPlaceBlockEntity host) {
        if (host.spec().kind().attachment()
                && level.hasNeighborSignal(pos)) {
            host.transferOnce();
        }
        if (host.spec().kind().extender()) {
            host.pushExtender();
        }
        if (host.spec().kind() == MteInPlaceKind.STEAM_TURBINE) {
            host.consumeSteam();
        }
        if (host.spec().kind().drive()) {
            host.pushDrive();
        }
    }

    public void transferOnce() {
        if (level == null || level.isClientSide) {
            return;
        }
        Direction facing = getBlockState().getValue(MteInPlaceBlock.FACING);
        switch (spec().kind()) {
            case FAUCET, TAP, NOZZLE -> pourDown(facing);
            case FUNNEL, CAP_NOZZLE -> fillAttached(facing);
            default -> {
            }
        }
    }

    public IFluidHandler fluidHandler(Direction side) {
        if (spec().kind().extender()) {
            return new ExtenderHandler(side);
        }
        if (spec().kind().foundryTank()) {
            return tank;
        }
        return null;
    }

    public net.neoforged.neoforge.items.IItemHandler itemHandler(Direction side) {
        return spec().kind().inventory() ? items : null;
    }

    public void dropContents() {
        if (level == null || !spec().kind().inventory()) {
            return;
        }
        for (int slot = 0; slot < items.getSlots(); slot++) {
            Containers.dropItemStack(
                    level,
                    worldPosition.getX(),
                    worldPosition.getY(),
                    worldPosition.getZ(),
                    items.getStackInSlot(slot));
        }
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        return spec().kind().energy() && type == spec().kind().energyType();
    }

    @Override
    public long stored(EnergyType type) {
        return handles(type, Direction.NORTH) ? storedEnergy : 0L;
    }

    @Override
    public long capacity(EnergyType type) {
        return handles(type, Direction.NORTH) ? ENERGY_CAPACITY : 0L;
    }

    @Override
    public long insert(
            EnergyType type,
            long size,
            long amount,
            Direction side,
            boolean simulate) {
        if (!handles(type, side) || amount <= 0L) {
            return 0L;
        }
        long room = Math.max(0L, ENERGY_CAPACITY - storedEnergy);
        long accepted = Math.min(amount, room);
        if (!simulate) {
            storedEnergy += accepted;
            setChanged();
        }
        return accepted;
    }

    @Override
    public long extract(
            EnergyType type,
            long size,
            long maxAmount,
            Direction side,
            boolean simulate) {
        if (!handles(type, side) || maxAmount <= 0L) {
            return 0L;
        }
        long taken = Math.min(maxAmount, storedEnergy);
        if (!simulate) {
            storedEnergy -= taken;
            setChanged();
        }
        return taken;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", items.serializeNBT(registries));
        tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.putLong("StoredEnergy", storedEnergy);
        tag.putBoolean("Formed", formed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            items.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        if (tag.contains("tank")) {
            tank.readFromNBT(registries, tag.getCompound("tank"));
        }
        storedEnergy = tag.getLong("StoredEnergy");
        formed = tag.getBoolean("Formed");
    }

    private void pourDown(Direction facing) {
        IFluidHandler source = neighborFluid(facing, facing.getOpposite());
        if (source == null) {
            return;
        }
        BlockPos destPos = worldPosition.below();
        while (level.getBlockEntity(destPos) instanceof MteInPlaceBlockEntity other
                && other.spec().kind() == MteInPlaceKind.FAUCET) {
            destPos = destPos.below();
        }
        IFluidHandler dest = capabilityFluid(destPos, Direction.UP);
        move(source, dest);
    }

    private void fillAttached(Direction facing) {
        IFluidHandler dest = neighborFluid(facing, facing.getOpposite());
        IFluidHandler source = neighborFluid(Direction.UP, Direction.DOWN);
        move(source, dest);
    }

    private void pushExtender() {
        Direction output = getBlockState().getValue(MteInPlaceBlock.FACING);
        Direction input = output.getOpposite();
        move(neighborFluid(input, output), neighborFluid(output, input));
        if (spec().kind() == MteInPlaceKind.TANK_BRIDGE) {
            move(neighborFluid(output, input), neighborFluid(input, output));
        }
    }

    private void consumeSteam() {
        Direction facing = getBlockState().getValue(MteInPlaceBlock.FACING);
        IFluidHandler source = neighborFluid(facing, facing.getOpposite());
        if (source == null || storedEnergy >= ENERGY_CAPACITY) {
            return;
        }
        FluidStack drained = source.drain(
                new FluidStack(ModFluids.STEAM_SOURCE.get(), TRANSFER_MB),
                IFluidHandler.FluidAction.EXECUTE);
        if (!drained.isEmpty()) {
            storedEnergy = Math.min(
                    ENERGY_CAPACITY,
                    storedEnergy + drained.getAmount());
            setChanged();
        }
    }

    private void pushDrive() {
        Direction facing = getBlockState().getValue(MteInPlaceBlock.FACING);
        if (!(level.getBlockEntity(worldPosition.relative(facing))
                instanceof MteInPlaceBlockEntity other)
                || !other.spec().kind().drive()
                || storedEnergy <= 0L) {
            return;
        }
        long moved = other.insert(
                EnergyType.KINETIC_ROTATION,
                1L,
                storedEnergy,
                facing.getOpposite(),
                false);
        if (moved > 0L) {
            storedEnergy -= moved;
            setChanged();
        }
    }

    private void move(IFluidHandler source, IFluidHandler dest) {
        if (source == null || dest == null) {
            return;
        }
        FluidStack drained = source.drain(TRANSFER_MB, IFluidHandler.FluidAction.SIMULATE);
        if (drained.isEmpty()) {
            return;
        }
        int filled = dest.fill(drained, IFluidHandler.FluidAction.EXECUTE);
        if (filled > 0) {
            source.drain(filled, IFluidHandler.FluidAction.EXECUTE);
            setChanged();
        }
    }

    private IFluidHandler neighborFluid(Direction step, Direction access) {
        if (level == null) {
            return null;
        }
        return capabilityFluid(worldPosition.relative(step), access);
    }

    private IFluidHandler capabilityFluid(BlockPos pos, Direction access) {
        if (level == null) {
            return null;
        }
        return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, access);
    }

    private static MteInPlaceSpec specOf(BlockState state) {
        if (!(state.getBlock() instanceof MteInPlaceBlock block)) {
            throw new IllegalStateException(
                    "In-place MTE entity bound to " + state.getBlock());
        }
        return block.spec();
    }

    private final class ExtenderHandler implements IFluidHandler {
        private final Direction side;

        private ExtenderHandler(Direction side) {
            this.side = side;
        }

        private IFluidHandler target() {
            Direction facing = getBlockState().getValue(MteInPlaceBlock.FACING);
            Direction step = side == facing ? facing.getOpposite() : facing;
            return neighborFluid(step, step.getOpposite());
        }

        @Override
        public int getTanks() {
            IFluidHandler target = target();
            return target == null ? 0 : target.getTanks();
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            IFluidHandler target = target();
            return target == null ? FluidStack.EMPTY : target.getFluidInTank(tank);
        }

        @Override
        public int getTankCapacity(int tank) {
            IFluidHandler target = target();
            return target == null ? 0 : target.getTankCapacity(tank);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            IFluidHandler target = target();
            return target != null && target.isFluidValid(tank, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            IFluidHandler target = target();
            return target == null ? 0 : target.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            IFluidHandler target = target();
            return target == null ? FluidStack.EMPTY : target.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            IFluidHandler target = target();
            return target == null ? FluidStack.EMPTY : target.drain(maxDrain, action);
        }
    }
}
