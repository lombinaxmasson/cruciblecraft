package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.block.SteamEngineBlock;
import com.masson.cruciblecraft.energy.EnergyEmitter;
import com.masson.cruciblecraft.energy.PerTickEnergyBudget;
import com.masson.cruciblecraft.energy.converter.EnergyConverterCatalog;
import com.masson.cruciblecraft.energy.converter.EnergyConverterProfile;
import com.masson.cruciblecraft.machine.CheckpointDecisions;
import com.masson.cruciblecraft.machine.component.CheckpointTracker;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModFluids;
import com.masson.cruciblecraft.steam.ExactFluidTransfer;
import com.masson.cruciblecraft.steam.KineticBuffer;
import com.masson.cruciblecraft.steam.MachineSideRules;
import com.masson.cruciblecraft.steam.SteamConversion;
import com.masson.cruciblecraft.steam.SteamEngineKuCurve;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public final class SteamEngineBlockEntity extends BlockEntity implements IEnergyHandler {
    private final EnergyConverterProfile profile;
    public static final int STEAM_CAPACITY =
            EnergyConverterCatalog.require("cruciblecraft:bronze_steam_engine")
                    .inputCapacity();
    public static final long KU_CAPACITY =
            EnergyConverterCatalog.require("cruciblecraft:bronze_steam_engine")
                    .outputCapacity();
    /**
     * CC design policy: fixed 12 KU/t nominal packet. Source 1302 derives only
     * mOutput=24/STEAM_PER_EU(2)=12; GT6 emits state-dependent 6..24 KU/t.
     * Live emit uses that curve.
     */
    public static final long OUTPUT_RATE =
            EnergyConverterCatalog.require("cruciblecraft:bronze_steam_engine")
                    .outputPacket().size();
    public static final int EXHAUST_CAPACITY =
            EnergyConverterCatalog.require("cruciblecraft:bronze_steam_engine")
                    .exhaust().capacity();
    private final FluidTank steam;
    private final FluidTank exhaust;
    private final IFluidHandler steamIo = new SteamIoHandler();
    private KineticBuffer kinetic;
    private final PerTickEnergyBudget outputBudget = new PerTickEnergyBudget();
    private final CheckpointTracker checkpoint = new CheckpointTracker();
    private String status = "no_steam";
    private boolean stopped;

    public SteamEngineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STEAM_ENGINE.get(), pos, state);
        if (!(state.getBlock() instanceof com.masson.cruciblecraft.energy.converter.EnergyConverterHost host)) {
            throw new IllegalArgumentException("Steam engine requires a catalog block");
        }
        profile = host.converterProfile();
        steam = new FluidTank(
                profile.inputCapacity(),
                stack -> stack.is(ModFluids.STEAM_SOURCE.get())) {
            @Override protected void onContentsChanged() { markMutation(); }
        };
        exhaust = new FluidTank(
                profile.exhaust().capacity(),
                SteamConversion::isDistilledWater) {
            @Override protected void onContentsChanged() { markMutation(); }
        };
        kinetic = new KineticBuffer(
                profile.outputCapacity(),
                maxOutputRate());
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SteamEngineBlockEntity engine) {
        engine.convertOneBatch();
        engine.emitKinetic(level, pos, state);
        engine.checkSteamVentStop();
        engine.bleedIfStopped();
        engine.pushDistilledExhaust();
        long phaseKey = CheckpointDecisions.phaseKey(pos.getX(), pos.getY(), pos.getZ());
        if (engine.checkpoint.shouldSync(false, level.getGameTime(), phaseKey, 20)) {
            engine.syncToClient();
            engine.checkpoint.synced();
        }
    }

    private void convertOneBatch() {
        if (stopped) {
            setStatus("stopped");
            return;
        }
        if (steam.getFluidAmount()
                < SteamConversion.ENGINE_STEAM_PER_BATCH) {
            setStatus("no_steam");
            return;
        }
        if (exhaust.getSpace()
                < SteamConversion.EXHAUST_WATER_PER_BATCH) {
            setStatus("exhaust_full");
            return;
        }
        if (kinetic.room() < SteamConversion.KU_PER_ENGINE_BATCH) {
            setStatus("kinetic_full");
            return;
        }
        int batches = Math.min(
                1,
                SteamConversion.engineBatches(
                        steam.getFluidAmount(),
                        kinetic.room(),
                        exhaust.getSpace()));
        if (batches != 1) {
            throw new IllegalStateException(
                    "Steam engine source batch plan was not executable");
        }
        int steamUsed = SteamConversion.ENGINE_STEAM_PER_BATCH;
        int waterReturned = SteamConversion.EXHAUST_WATER_PER_BATCH;
        FluidStack simulated = steam.drain(
                steamUsed, IFluidHandler.FluidAction.SIMULATE);
        FluidStack water = SteamConversion.distilledExhaust(waterReturned);
        int simulatedExhaust = exhaust.fill(
                water, IFluidHandler.FluidAction.SIMULATE);
        if (simulated.getAmount() != steamUsed
                || !simulated.is(ModFluids.STEAM_SOURCE.get())
                || simulatedExhaust != waterReturned) {
            throw new IllegalStateException(
                    "Steam engine conversion simulation violated its plan");
        }
        FluidStack drained = steam.drain(
                steamUsed, IFluidHandler.FluidAction.EXECUTE);
        int filled = exhaust.fill(
                water, IFluidHandler.FluidAction.EXECUTE);
        int produced = SteamConversion.KU_PER_ENGINE_BATCH;
        if (drained.getAmount() != steamUsed
                || !drained.is(ModFluids.STEAM_SOURCE.get())
                || filled != waterReturned
                || kinetic.insert(produced) != produced) {
            throw new IllegalStateException(
                    "Steam engine execute differed from simulation");
        }
        setStatus("running");
        markMutation();
    }

    /**
     * GT6 {@code MultiTileEntityEngineSteam}: when KU cannot take another
     * batch and the steam tank is packed, vent steam and latch {@code mStopped}.
     */
    private void checkSteamVentStop() {
        if (stopped
                || steam.getSpace() > 0
                || kinetic.room() >= SteamConversion.KU_PER_ENGINE_BATCH) {
            return;
        }
        if (SteamEngineKuCurve.visualState(
                kinetic.stored(), profile.outputCapacity()) <= 30) {
            return;
        }
        steam.setFluid(FluidStack.EMPTY);
        long overflow = kinetic.stored() - (profile.outputCapacity() - 1L);
        if (overflow > 0L) {
            kinetic.discard(overflow);
        }
        stopped = true;
        setStatus("overloaded");
        if (level != null && !level.isClientSide) {
            level.playSound(
                    null,
                    worldPosition,
                    SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.BLOCKS,
                    0.5F,
                    2.6F);
        }
        markMutation();
    }

    private void bleedIfStopped() {
        if (!stopped || kinetic.stored() <= 0L) {
            return;
        }
        long bleed = Math.max(1L, profile.outputCapacity() / 64L);
        if (kinetic.discard(bleed) > 0L) {
            markMutation();
        }
    }

    /**
     * GT6 {@code FACING_SIDES} DistW push. Leftover stays in the BUFFER_THEN_DRAIN
     * tank instead of {@code GarbageGT.trash}.
     */
    private void pushDistilledExhaust() {
        if (level == null || level.isClientSide || exhaust.isEmpty()) {
            return;
        }
        Direction front = front();
        if (front == null) {
            return;
        }
        boolean moved = false;
        for (Direction side : Direction.values()) {
            if (exhaust.isEmpty()) {
                break;
            }
            if (!MachineSideRules.engineExposesExhaust(front, side)) {
                continue;
            }
            BlockPos target = worldPosition.relative(side);
            if (!level.hasChunkAt(target)) {
                continue;
            }
            IFluidHandler neighbor = level.getCapability(
                    Capabilities.FluidHandler.BLOCK,
                    target,
                    side.getOpposite());
            if (neighbor == null) {
                continue;
            }
            if (ExactFluidTransfer.move(
                    exhaust, neighbor, exhaust.getFluidAmount()) > 0) {
                moved = true;
            }
        }
        if (moved) {
            markMutation();
        }
    }

    /**
     * GT6 {@code MultiTileEntityEngineSteam}: emit one signed KU packet, then
     * always {@code mEnergy -= tOutput} even when the neighbor took nothing.
     */
    private void emitKinetic(Level level, BlockPos pos, BlockState state) {
        if (stopped) {
            return;
        }
        long rate = currentOutputRate();
        if (!SteamEngineKuCurve.activelyEmitting(
                kinetic.stored(),
                rate,
                profile.outputPacket().size())) {
            return;
        }
        Direction output = state.getValue(SteamEngineBlock.FACING);
        EnergyEmitter.pushToSide(
                level,
                pos,
                EnergyType.KINETIC_PUSH,
                kinetic.strokeSign() * rate,
                1L,
                output);
        long removed = kinetic.extract(rate, false);
        if (removed != rate) {
            throw new IllegalStateException(
                    "Steam engine KU discard changed after the emit check");
        }
        markMutation();
    }

    public IFluidHandler fluids(Direction side) {
        Direction front = front();
        return front != null && MachineSideRules.engineAcceptsSteam(front, side)
                ? steamIo
                : null;
    }
    public long currentOutputRate() {
        return SteamEngineKuCurve.outputKu(
                profile.outputPacket().size(),
                SteamEngineKuCurve.visualState(
                        kinetic.stored(), profile.outputCapacity()));
    }

    private long maxOutputRate() {
        return SteamEngineKuCurve.maximumKu(profile.outputPacket().size());
    }

    public int steamAmount() { return steam.getFluidAmount(); }
    public int exhaustAmount() { return exhaust.getFluidAmount(); }
    public FluidStack exhaustFluid() { return exhaust.getFluid().copy(); }
    public String status() { return status; }
    public boolean stopped() { return stopped; }

    public boolean toggleStopped() {
        stopped = !stopped;
        setStatus(stopped ? "stopped" : (steam.isEmpty() ? "no_steam" : "running"));
        markMutation();
        return !stopped;
    }
    @Override public boolean handles(EnergyType type, Direction side) {
        Direction front = front();
        return type == EnergyType.KINETIC_PUSH
                && front != null
                && !stopped
                && MachineSideRules.engineExposesKinetic(front, side);
    }
    @Override public long outputSize(EnergyType type, Direction side) {
        return handles(type, side)
                        && kinetic.stored() >= currentOutputRate()
                        && outputBudget.claim(gameTime(), 1L, 1L, true) > 0L
                ? kinetic.strokeSign() * currentOutputRate()
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
                || size != kinetic.strokeSign() * currentOutputRate()
                || kinetic.stored() < currentOutputRate()
                || outputBudget.claim(gameTime(), 1L, 1L, true) <= 0L) {
            return 0L;
        }
        long extracted = Math.min(maxAmount, 1L);
        boolean effectiveSimulation = simulate || level == null || level.isClientSide;
        if (!effectiveSimulation) {
            long claimed = outputBudget.claim(gameTime(), 1L, extracted, false);
            if (claimed != extracted) {
                throw new IllegalStateException(
                        "Steam engine output budget changed after simulation");
            }
            long removed = kinetic.extract(currentOutputRate(), false);
            if (removed != currentOutputRate()) {
                throw new IllegalStateException(
                        "Steam engine kinetic storage changed after simulation");
            }
            markMutation();
        }
        return extracted;
    }
    private long gameTime() {
        if (level == null) {
            outputBudget.reset();
            return 0L;
        }
        return level.getGameTime();
    }
    @Override public long stored(EnergyType type) {
        return type == EnergyType.KINETIC_PUSH
                ? kinetic.stored()
                : 0L;
    }
    @Override public long capacity(EnergyType type) {
        return type == EnergyType.KINETIC_PUSH ? profile.outputCapacity() : 0L;
    }
    public long stored() { return kinetic.stored(); }
    public int strokeSign() { return kinetic.strokeSign(); }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("steam", steam.writeToNBT(registries, new CompoundTag()));
        tag.put("exhaust", exhaust.writeToNBT(registries, new CompoundTag()));
        tag.putLong("kinetic", kinetic.stored());
        tag.putInt("stroke_sign", kinetic.strokeSign());
        tag.putString("status", status);
        tag.putBoolean("stopped", stopped);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("steam")) steam.readFromNBT(registries, tag.getCompound("steam"));
        if (tag.contains("exhaust")) {
            exhaust.readFromNBT(registries, tag.getCompound("exhaust"));
            FluidStack migrated = SteamConversion.migrateLegacyExhaust(
                    exhaust.getFluid());
            if (!FluidStack.isSameFluidSameComponents(
                    migrated, exhaust.getFluid())) {
                exhaust.setFluid(migrated);
            }
        }
        kinetic = new KineticBuffer(
                profile.outputCapacity(),
                maxOutputRate(),
                tag.getLong("kinetic"),
                tag.getInt("stroke_sign"));
        stopped = tag.getBoolean("stopped");
        status = tag.getString("status");
        if (status.isBlank()) {
            status = stopped ? "stopped" : (steam.isEmpty() ? "no_steam" : "running");
        }
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return writeClientTag();
    }

    private CompoundTag writeClientTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("steam_amount", steam.getFluidAmount());
        tag.putInt("exhaust_amount", exhaust.getFluidAmount());
        tag.putLong("kinetic", kinetic.stored());
        tag.putInt("stroke_sign", kinetic.strokeSign());
        tag.putString("status", status);
        tag.putBoolean("stopped", stopped);
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
                Math.min(steam.getCapacity(), tag.getInt("steam_amount")));
        steam.setFluid(amount == 0
                ? FluidStack.EMPTY
                : new FluidStack(ModFluids.STEAM_SOURCE.get(), amount));
        int exhaustAmount = Math.max(
                0,
                Math.min(exhaust.getCapacity(), tag.getInt("exhaust_amount")));
        exhaust.setFluid(exhaustAmount == 0
                ? FluidStack.EMPTY
                : SteamConversion.distilledExhaust(exhaustAmount));
        kinetic = new KineticBuffer(
                profile.outputCapacity(),
                maxOutputRate(),
                tag.getLong("kinetic"),
                tag.getInt("stroke_sign"));
        stopped = tag.getBoolean("stopped");
        status = tag.getString("status");
        if (status.isBlank()) {
            status = stopped ? "stopped" : (steam.isEmpty() ? "no_steam" : "running");
        }
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

    private void setStatus(String next) {
        if (!status.equals(next)) {
            status = next;
            markMutation();
        }
    }

    private Direction front() {
        BlockState state = getBlockState();
        return state.hasProperty(SteamEngineBlock.FACING)
                ? state.getValue(SteamEngineBlock.FACING)
                : null;
    }

    private final class SteamIoHandler implements IFluidHandler {
        @Override public int getTanks() {
            return 2;
        }

        @Override public FluidStack getFluidInTank(int tank) {
            return switch (tank) {
                case 0 -> steam.getFluid();
                case 1 -> exhaust.getFluid();
                default -> FluidStack.EMPTY;
            };
        }

        @Override public int getTankCapacity(int tank) {
            return switch (tank) {
                case 0 -> STEAM_CAPACITY;
                case 1 -> EXHAUST_CAPACITY;
                default -> 0;
            };
        }

        @Override public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && steam.isFluidValid(stack);
        }

        @Override public int fill(FluidStack resource, FluidAction action) {
            return stopped ? 0 : steam.fill(resource, action);
        }

        @Override public FluidStack drain(FluidStack resource, FluidAction action) {
            return exhaust.drain(resource, action);
        }

        @Override public FluidStack drain(int maxDrain, FluidAction action) {
            return exhaust.drain(maxDrain, action);
        }
    }
}
