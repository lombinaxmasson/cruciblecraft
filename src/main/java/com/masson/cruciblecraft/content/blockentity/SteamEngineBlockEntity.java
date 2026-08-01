package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.block.SteamEngineBlock;
import com.masson.cruciblecraft.energy.EnergyEmitter;
import com.masson.cruciblecraft.energy.PerTickEnergyBudget;
import com.masson.cruciblecraft.machine.CheckpointDecisions;
import com.masson.cruciblecraft.machine.component.CheckpointTracker;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModFluids;
import com.masson.cruciblecraft.steam.SteamConversion;
import com.masson.cruciblecraft.steam.KineticBuffer;
import com.masson.cruciblecraft.steam.MachineSideRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public final class SteamEngineBlockEntity extends BlockEntity implements IEnergyHandler {
    public static final int STEAM_CAPACITY = 16_000;
    public static final long KU_CAPACITY = 1_024;
    /** Bronze nominal packet, matching the documented ~24 KU/t GT6 tier. */
    public static final long OUTPUT_RATE = 24;
    private final FluidTank steam = new FluidTank(
            STEAM_CAPACITY, stack -> stack.is(ModFluids.STEAM_SOURCE.get())) {
        @Override protected void onContentsChanged() { markMutation(); }
    };
    private final IFluidHandler steamInput = new SteamInputHandler();
    private KineticBuffer kinetic = new KineticBuffer(KU_CAPACITY, OUTPUT_RATE);
    private final PerTickEnergyBudget outputBudget = new PerTickEnergyBudget();
    private final CheckpointTracker checkpoint = new CheckpointTracker();

    public SteamEngineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STEAM_ENGINE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SteamEngineBlockEntity engine) {
        int produced = SteamConversion.kineticFromSteam(
                steamAmount(engine), (int) Math.min(OUTPUT_RATE, engine.kinetic.room()));
        if (produced > 0) {
            engine.steam.drain(produced * SteamConversion.STEAM_PER_KU,
                    net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            engine.kinetic.insert(produced);
            engine.markMutation();
        }
        Direction output = state.getValue(SteamEngineBlock.FACING);
        EnergyEmitter.emit(level, pos, engine, EnergyType.KINETIC, output);
        long phaseKey = CheckpointDecisions.phaseKey(pos.getX(), pos.getY(), pos.getZ());
        if (engine.checkpoint.shouldSync(false, level.getGameTime(), phaseKey, 20)) {
            engine.syncToClient();
            engine.checkpoint.synced();
        }
    }
    private static int steamAmount(SteamEngineBlockEntity engine) { return engine.steam.getFluidAmount(); }
    public IFluidHandler fluids(Direction side) {
        Direction front = front();
        return front != null && MachineSideRules.engineAcceptsSteam(front, side)
                ? steamInput
                : null;
    }
    public int steamAmount() { return steam.getFluidAmount(); }
    @Override public boolean handles(EnergyType type, Direction side) {
        Direction front = front();
        return type == EnergyType.KINETIC
                && front != null
                && MachineSideRules.engineExposesKinetic(front, side);
    }
    @Override public long outputSize(EnergyType type, Direction side) {
        return handles(type, side)
                        && kinetic.stored() >= OUTPUT_RATE
                        && outputBudget.claim(gameTime(), 1L, 1L, true) > 0L
                ? kinetic.strokeSign() * OUTPUT_RATE
                : 0L;
    }
    @Override public long extract(
            EnergyType type,
            long size,
            long maxAmount,
            Direction side,
            boolean simulate) {
        if (!handles(type, side)
                || maxAmount <= 0L
                || size != kinetic.strokeSign() * OUTPUT_RATE
                || kinetic.stored() < OUTPUT_RATE
                || outputBudget.claim(gameTime(), 1L, 1L, true) <= 0L) {
            return 0L;
        }
        long extracted = Math.min(maxAmount, 1L);
        boolean effectiveSimulation = simulate || level == null || level.isClientSide;
        if (!effectiveSimulation) {
            outputBudget.claim(gameTime(), 1L, extracted, false);
            kinetic.extract(OUTPUT_RATE, false);
            markMutation();
        }
        return extracted;
    }
    private long gameTime() {
        return level == null ? Long.MIN_VALUE : level.getGameTime();
    }
    @Override public long stored(EnergyType type) {
        return type == EnergyType.KINETIC ? kinetic.stored() : 0L;
    }
    @Override public long capacity(EnergyType type) {
        return type == EnergyType.KINETIC ? KU_CAPACITY : 0L;
    }
    public long stored() { return kinetic.stored(); }
    public int strokeSign() { return kinetic.strokeSign(); }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("steam", steam.writeToNBT(registries, new CompoundTag()));
        tag.putLong("kinetic", kinetic.stored());
        tag.putInt("stroke_sign", kinetic.strokeSign());
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("steam")) steam.readFromNBT(registries, tag.getCompound("steam"));
        kinetic = new KineticBuffer(
                KU_CAPACITY, OUTPUT_RATE, tag.getLong("kinetic"), tag.getInt("stroke_sign"));
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return writeClientTag();
    }

    private CompoundTag writeClientTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("steam_amount", steam.getFluidAmount());
        tag.putLong("kinetic", kinetic.stored());
        tag.putInt("stroke_sign", kinetic.strokeSign());
        return tag;
    }

    @Override public void handleUpdateTag(
            CompoundTag tag,
            HolderLookup.Provider registries) {
        readClientTag(tag);
    }

    @Override public void onDataPacket(
            Connection connection,
            ClientboundBlockEntityDataPacket packet,
            HolderLookup.Provider registries) {
        CompoundTag tag = packet.getTag();
        if (tag != null) readClientTag(tag);
    }

    private void readClientTag(CompoundTag tag) {
        int amount = Math.max(
                0,
                Math.min(STEAM_CAPACITY, tag.getInt("steam_amount")));
        steam.setFluid(amount == 0
                ? FluidStack.EMPTY
                : new FluidStack(ModFluids.STEAM_SOURCE.get(), amount));
        kinetic = new KineticBuffer(
                KU_CAPACITY,
                OUTPUT_RATE,
                tag.getLong("kinetic"),
                tag.getInt("stroke_sign"));
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void markMutation() {
        setChanged();
        checkpoint.markSyncPending();
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(
                    worldPosition,
                    getBlockState(),
                    getBlockState(),
                    Block.UPDATE_CLIENTS);
        }
    }

    private Direction front() {
        BlockState state = getBlockState();
        return state.hasProperty(SteamEngineBlock.FACING)
                ? state.getValue(SteamEngineBlock.FACING)
                : null;
    }

    private final class SteamInputHandler implements IFluidHandler {
        @Override public int getTanks() {
            return 1;
        }

        @Override public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? steam.getFluid() : FluidStack.EMPTY;
        }

        @Override public int getTankCapacity(int tank) {
            return tank == 0 ? STEAM_CAPACITY : 0;
        }

        @Override public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && steam.isFluidValid(stack);
        }

        @Override public int fill(FluidStack resource, FluidAction action) {
            return steam.fill(resource, action);
        }

        @Override public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }
}
