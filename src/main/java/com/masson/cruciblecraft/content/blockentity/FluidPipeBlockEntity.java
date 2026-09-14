package com.masson.cruciblecraft.content.blockentity;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.block.AbstractPipeBlock;
import com.masson.cruciblecraft.content.block.FluidPipeBlock;
import com.masson.cruciblecraft.logistics.pipe.PipeTopology;
import com.masson.cruciblecraft.logistics.pipe.PipeTransferDiagnostics;
import com.masson.cruciblecraft.logistics.pipe.PipeTransferPhase;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverBehavior;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverDefinition;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCover;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCoverSet;
import com.masson.cruciblecraft.logistics.pipe.fluid.FluidPipeBlockedMedia;
import com.masson.cruciblecraft.logistics.pipe.fluid.FluidPipeCadence;
import com.masson.cruciblecraft.logistics.pipe.fluid.FluidPipeDangerousMedia;
import com.masson.cruciblecraft.logistics.pipe.fluid.FluidPipeFailureState;
import com.masson.cruciblecraft.logistics.pipe.fluid.FluidPipeFailureState.Failure;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * GT6 per-segment fluid buffer. Pipe-to-pipe distribution runs every server
 * tick ({@code SERVER_TICK_PRE}/{@code PR2}); cover pumps stay on the shared
 * five-tick logistics phase.
 */
public final class FluidPipeBlockEntity extends BlockEntity {
    public static final int TRANSFER_INTERVAL =
            PipeTransferPhase.INTERVAL;
    public static final int CLIENT_SYNC_INTERVAL = 5;

    private final PipeCoverSet covers = new PipeCoverSet();
    private final FluidPipeFailureState failures =
            new FluidPipeFailureState();
    private final EnumMap<Direction, IFluidHandler> sidedHandlers =
            new EnumMap<>(Direction.class);
    private final FluidTank[] tanks;
    private Direction receivedFrom;
    private long receivedAtTick = Long.MIN_VALUE;
    private long metricTick = Long.MIN_VALUE;
    private long transferredThisTick;
    private long metricWindowStart = Long.MIN_VALUE;
    private long transferredThisWindow;
    private int nextOutput;
    private int recoveredInvalidFields;
    private boolean recoveryWarningLogged;
    private boolean clientSyncPending;
    private boolean networkChunkLoaded = true;

    public FluidPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_PIPE.get(), pos, state);
        int tankCount = Math.max(1, tankCountFor(state));
        int capacity = runtimeCapacity(state);
        tanks = new FluidTank[tankCount];
        for (int index = 0; index < tankCount; index++) {
            tanks[index] = new FluidTank(capacity) {
                @Override
                protected void onContentsChanged() {
                    FluidPipeBlockEntity.this.contentsChanged();
                }
            };
        }
        for (Direction side : Direction.values()) {
            sidedHandlers.put(side, new SidedHandler(side));
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        networkChunkLoaded = true;
        if (!recoveryWarningLogged
                && recoveredInvalidFields > 0
                && level != null
                && !level.isClientSide) {
            recoveryWarningLogged = true;
            CrucibleCraft.LOGGER.warn(
                    "Recovered {} invalid fluid-pipe NBT field(s) at {} {}; "
                            + "invalid failure values were reset and invalid "
                            + "cover faces were quarantined fail-closed",
                    recoveredInvalidFields,
                    level.dimension().location(),
                    worldPosition);
        }
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            FluidPipeBlockEntity pipe) {
        if (pipe.failures.pendingFailure() != Failure.NONE) {
            pipe.breakPipe(level, pos, state);
            return;
        }
        if (FluidPipeDangerousMedia.tick(level, pos, pipe)) {
            return;
        }
        pipe.rollMetrics(level.getGameTime());
        pipe.rollMetricWindow(level.getGameTime());
        if (PipeTransferPhase.isDue(level.getGameTime(), pos)) {
            pipe.tickCovers(level);
        }
        pipe.distribute(level);
        pipe.receivedFrom = null;
        pipe.receivedAtTick = Long.MIN_VALUE;
        pipe.flushClientSync(level.getGameTime());
    }

    public IFluidHandler fluidHandler(Direction side) {
        return side == null ? null : sidedHandlers.get(side);
    }

    public FluidStack storedFluid() {
        return firstStored().copy();
    }

    public int fillInternal(
            FluidStack stack, IFluidHandler.FluidAction action) {
        return fillMatching(stack, action);
    }

    public boolean trashContents() {
        if (allEmpty()) {
            return false;
        }
        for (FluidTank current : tanks) {
            if (!current.isEmpty()) {
                current.drain(
                        current.getCapacity(),
                        IFluidHandler.FluidAction.EXECUTE);
            }
        }
        return true;
    }

    public int capacity() {
        return tankCapacity();
    }

    public int tankCount() {
        return tanks.length;
    }

    public int totalCapacity() {
        return tankCapacity() * tanks.length;
    }

    public FluidStack fluidInTank(int index) {
        return tanks[index].getFluid().copy();
    }

    public long transferredThisTick() {
        rollMetrics(level == null ? 0L : level.getGameTime());
        return transferredThisTick;
    }

    public long transferredThisWindow() {
        rollMetricWindow(level == null ? 0L : level.getGameTime());
        return transferredThisWindow;
    }

    public FluidPipeFailureState.Snapshot failureSnapshot() {
        return failures.snapshot();
    }

    public Map<Direction, PipeCover> coverSnapshot() {
        return covers.snapshot();
    }

    public boolean offersNetworkDiscovery() {
        return networkChunkLoaded && !isRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        networkChunkLoaded = false;
        super.onChunkUnloaded();
    }

    public String coverSummary() {
        return covers.boundedSummary();
    }

    public boolean setCover(Direction side, PipeCover cover) {
        if (cover != null
                && !cover.supports(CoverDefinition.Medium.FLUID)) {
            return false;
        }
        if (!covers.set(side, cover)) {
            return false;
        }
        setChanged();
        if (level != null && !level.isClientSide) {
            PipeTopology.invalidate(level, worldPosition);
            syncToClient();
            notifyCoverRedstone(side);
        }
        return true;
    }

    public boolean replaceCoverQuiet(Direction side, PipeCover cover) {
        if (cover != null
                && !cover.supports(CoverDefinition.Medium.FLUID)) {
            return false;
        }
        if (!covers.set(side, cover)) {
            return false;
        }
        setChanged();
        if (level != null && !level.isClientSide) {
            syncToClient();
            notifyCoverRedstone(side);
        }
        return true;
    }

    public void dropCovers() {
        if (level == null || level.isClientSide) {
            return;
        }
        for (ItemStack stack : covers.removeAllAsItems()) {
            Block.popResource(level, worldPosition, stack);
        }
        setChanged();
        PipeTopology.invalidate(level, worldPosition);
        syncToClient();
    }

    public boolean removeCover(
            Direction side,
            net.minecraft.world.entity.player.Player player) {
        var taken = covers.take(side);
        if (taken.isEmpty()) {
            return false;
        }
        ItemStack stack = com.masson.cruciblecraft.logistics.pipe.cover
                .PipeCoverItems.stackFor(taken.orElseThrow());
        if (!stack.isEmpty()) {
            if (player == null || !player.addItem(stack)) {
                Block.popResource(level, worldPosition, stack);
            }
        }
        setChanged();
        if (level != null && !level.isClientSide) {
            PipeTopology.invalidate(level, worldPosition);
            syncToClient();
            notifyCoverRedstone(side);
        }
        return true;
    }

    public boolean configureCover(
            Direction side,
            CoverDefinition.ConfigField field,
            int value) {
        if (!covers.configure(side, field, value)) {
            return false;
        }
        setChanged();
        if (level != null && !level.isClientSide) {
            PipeTopology.invalidate(level, worldPosition);
            syncToClient();
            notifyCoverRedstone(side);
        }
        return true;
    }

    private void notifyCoverRedstone(Direction side) {
        if (level == null || level.isClientSide) {
            return;
        }
        net.minecraft.world.level.block.Block block =
                getBlockState().getBlock();
        level.updateNeighborsAt(worldPosition, block);
        if (side != null) {
            level.updateNeighborsAt(worldPosition.relative(side), block);
        }
    }

    public boolean toggleCoverInvert(Direction side) {
        if (!covers.toggleInvert(side)) {
            return false;
        }
        setChanged();
        if (level != null && !level.isClientSide) {
            PipeTopology.invalidate(level, worldPosition);
            syncToClient();
        }
        return true;
    }

    private void tickCovers(Level level) {
        for (Direction side : Direction.values()) {
            if (!AbstractPipeBlock.isConnected(liveState(), side)) {
                continue;
            }
            covers.tick(side, new FluidCoverContext(level, side));
        }
    }

    private int pumpFrom(
            IFluidHandler source,
            Direction side,
            int requested,
            Optional<String> matchId,
            CoverDefinition.TransferMode mode) {
        int limit = Math.min(transferLimit(), requested);
        try {
            FluidStack simulated =
                    source.drain(limit, IFluidHandler.FluidAction.SIMULATE);
            if (simulated.isEmpty()
                    || validateFluid(simulated) != Failure.NONE
                    || !matches(matchId, simulated)
                    || !covers.matches(side, simulated)
                    || (mode == CoverDefinition.TransferMode.EXACT
                            && simulated.getAmount() != limit)) {
                return 0;
            }
            int accepted = fillMatching(
                    simulated, IFluidHandler.FluidAction.SIMULATE);
            if (accepted <= 0 || accepted > simulated.getAmount()) {
                if (accepted < 0 || accepted > simulated.getAmount()) {
                    PipeTransferDiagnostics.warnOnce(
                            "fluid pump simulation",
                            source,
                            "Pipe tank accepted invalid amount " + accepted);
                }
                return 0;
            }
            if (mode == CoverDefinition.TransferMode.EXACT
                    && accepted != limit) {
                return 0;
            }
            FluidStack drained = source.drain(
                    accepted, IFluidHandler.FluidAction.EXECUTE);
            if (drained.isEmpty()
                    || drained.getAmount() > accepted
                    || !FluidStack.isSameFluidSameComponents(
                            drained, simulated)) {
                PipeTransferDiagnostics.warnOnce(
                        "fluid pump execution",
                        source,
                        "Pump source violated simulated drain");
                return 0;
            }
            int stored = fillMatching(
                    drained, IFluidHandler.FluidAction.EXECUTE);
            if (stored < drained.getAmount()) {
                PipeTransferDiagnostics.warnOnce(
                        "fluid pump storage",
                        fillTarget(drained),
                        "Pipe stored " + stored + " after draining "
                                + drained.getAmount());
                FluidStack remainder = drained.copyWithAmount(
                        drained.getAmount() - stored);
                int returned = source.fill(
                        remainder, IFluidHandler.FluidAction.EXECUTE);
                if (returned < remainder.getAmount()) {
                    fillMatching(
                            remainder.copyWithAmount(
                                    remainder.getAmount() - returned),
                            IFluidHandler.FluidAction.EXECUTE);
                }
            }
            return stored;
        } catch (RuntimeException failure) {
            PipeTransferDiagnostics.warnOnce(
                    "fluid pump", source, "Fluid pump transfer failed", failure);
            return 0;
        }
    }

    private static boolean matches(
            Optional<String> expected, FluidStack stack) {
        return expected.isEmpty()
                || expected.orElseThrow().equals(
                        net.minecraft.core.registries.BuiltInRegistries.FLUID
                                .getKey(stack.getFluid()).toString());
    }

    private void distribute(Level level) {
        for (FluidTank current : tanks) {
            if (!current.isEmpty()) {
                distributeTank(level, current);
            }
        }
    }

    private void distributeTank(Level level, FluidTank current) {
        if (current.isEmpty()) {
            return;
        }
        Direction[] directions = FluidPipeCadence.scanOrder(worldPosition);
        int start = Math.floorMod(nextOutput++, directions.length);
        int remaining = transferLimit();
        for (int index = 0;
                index < directions.length && remaining > 0 && !current.isEmpty();
                index++) {
            Direction side = directions[(start + index) % directions.length];
            if (!AbstractPipeBlock.isConnected(liveState(), side)
                    || (receivedFrom == side
                            && receivedAtTick == level.getGameTime())
                    || covers.hasPump(side)) {
                continue;
            }
            FluidStack offered = current.getFluid().copyWithAmount(
                    Math.min(remaining, current.getFluidAmount()));
            if (!covers.allowsOutgoing(
                            side,
                            CoverDefinition.Medium.FLUID,
                            storedAmount(),
                            totalCapacity())
                    || !covers.matches(side, offered)) {
                continue;
            }
            BlockPos target = worldPosition.relative(side);
            if (!level.hasChunkAt(target)) {
                continue;
            }
            IFluidHandler endpoint;
            try {
                endpoint = level.getCapability(
                        Capabilities.FluidHandler.BLOCK,
                        target,
                        side.getOpposite());
            } catch (RuntimeException failure) {
                PipeTransferDiagnostics.warnOnce(
                        "fluid endpoint discovery",
                        null,
                        "Fluid endpoint discovery failed at " + target,
                        failure);
                continue;
            }
            if (endpoint == null) {
                continue;
            }
            int moved = sourceFirstTransfer(current, endpoint, offered);
            remaining -= moved;
            recordTransfer(moved);
        }
    }

    private int sourceFirstTransfer(
            FluidTank sourceTank,
            IFluidHandler endpoint,
            FluidStack offered) {
        FluidStack drained = FluidStack.EMPTY;
        int executed = 0;
        try {
            int simulated = endpoint.fill(
                    offered, IFluidHandler.FluidAction.SIMULATE);
            if (simulated <= 0 || simulated > offered.getAmount()) {
                if (simulated < 0 || simulated > offered.getAmount()) {
                    PipeTransferDiagnostics.warnOnce(
                            "fluid endpoint simulation",
                            endpoint,
                            "Endpoint accepted invalid amount " + simulated);
                }
                return 0;
            }
            drained = sourceTank.drain(
                    simulated, IFluidHandler.FluidAction.EXECUTE);
            if (drained.getAmount() != simulated) {
                PipeTransferDiagnostics.warnOnce(
                        "fluid source execution",
                        sourceTank,
                        "Pipe source changed after simulation");
                restoreToTank(sourceTank, drained);
                return 0;
            }
            executed = endpoint.fill(
                    drained, IFluidHandler.FluidAction.EXECUTE);
            if (executed < 0 || executed > drained.getAmount()) {
                PipeTransferDiagnostics.warnOnce(
                        "fluid endpoint execution",
                        endpoint,
                        "Endpoint executed invalid amount " + executed);
                restoreToTank(sourceTank, drained);
                return 0;
            }
            if (executed < drained.getAmount()) {
                PipeTransferDiagnostics.warnOnce(
                        "fluid endpoint execution",
                        endpoint,
                        "Endpoint executed " + executed
                                + " after simulating " + drained.getAmount()
                                + "; difference retained by source");
                FluidStack retained = drained.copyWithAmount(
                        drained.getAmount() - executed);
                restoreToTank(sourceTank, retained);
            }
            return executed;
        } catch (RuntimeException failure) {
            if (!drained.isEmpty() && executed <= 0) {
                restoreToTank(sourceTank, drained);
            }
            PipeTransferDiagnostics.warnOnce(
                    "fluid endpoint execution",
                    endpoint,
                    "Fluid source-first transfer failed",
                    failure);
            return 0;
        }
    }

    private void restoreToTank(FluidTank target, FluidStack retained) {
        if (retained.isEmpty()) {
            return;
        }
        int restored = target.fill(
                retained, IFluidHandler.FluidAction.EXECUTE);
        if (restored != retained.getAmount()) {
            throw new IllegalStateException(
                    "Fluid pipe could not restore blocked remainder");
        }
    }

    public FluidPipeBlock pipeBlock() {
        return pipe();
    }

    public FluidTank[] tanks() {
        return tanks;
    }

    public boolean tanksEmpty() {
        return allEmpty();
    }

    private Failure validateFluid(FluidStack resource) {
        if (FluidPipeBlockedMedia.kindOf(resource)
                != FluidPipeBlockedMedia.Kind.NONE) {
            return Failure.NONE;
        }
        int temperature = resource.getFluidType().getTemperature(resource);
        if (temperature > pipe().pipe().fluid().maxTemperatureKelvin()) {
            return Failure.OVER_TEMPERATURE;
        }
        try {
            boolean registeredGas = ModFluids
                    .chemicalState(resource.getFluid())
                    .filter(state -> state
                            == com.masson.cruciblecraft.material
                                    .ChemicalFluidRegistrationGate.State.GAS)
                    .isPresent();
            if (registeredGas && !pipe().pipe().fluid().gasProof()) {
                return Failure.GAS_LEAK;
            }
            var material = ModFluids.material(resource.getFluid());
            if (material.isPresent()) {
                var metadata = material.orElseThrow().gt6Metadata();
                if (metadata.isPresent()) {
                    var facts = metadata.orElseThrow();
                    if (facts.materialTags().contains("PROPERTIES.ACID")
                            && !pipe().pipe().fluid().acidProof()) {
                        return Failure.CORROSION;
                    }
                }
                boolean gas = metadata
                        .map(facts -> "gas".equals(facts.state()))
                        .orElse(false);
                if (gas && !pipe().pipe().fluid().gasProof()) {
                    return Failure.GAS_LEAK;
                }
            }
        } catch (IllegalStateException ignored) {
            // Registry lookup is unavailable in isolated unit tests.
        }
        return Failure.NONE;
    }

    public int transferLimit() {
        return Math.max(1, tankCapacity());
    }

    private void recordFailure(Failure failure, long amount) {
        FluidPipeFailureState.Change change =
                failures.record(failure, amount);
        if (change.persistenceChanged()) {
            setChanged();
        }
        if (change.observableChanged()) {
            syncToClient();
        }
    }

    private void recordTransfer(long amount) {
        if (amount <= 0L) {
            return;
        }
        rollMetrics(level == null ? 0L : level.getGameTime());
        rollMetricWindow(level == null ? 0L : level.getGameTime());
        transferredThisTick = Math.addExact(
                transferredThisTick, amount);
        transferredThisWindow = Math.addExact(
                transferredThisWindow, amount);
    }

    private void rollMetrics(long tick) {
        if (metricTick != tick) {
            metricTick = tick;
            transferredThisTick = 0L;
        }
    }

    private void rollMetricWindow(long tick) {
        long start = tick - Math.floorMod(tick, 20L);
        if (metricWindowStart != start) {
            metricWindowStart = start;
            transferredThisWindow = 0L;
        }
    }

    private void contentsChanged() {
        setChanged();
        clientSyncPending = true;
    }

    private void flushClientSync(long gameTime) {
        if (!clientSyncPending
                || !isClientSyncTick(gameTime, worldPosition)) {
            return;
        }
        syncToClient();
    }

    static boolean isClientSyncTick(long gameTime, BlockPos position) {
        return Math.floorMod(
                        gameTime
                                + position.getX() * 31L
                                + position.getY() * 17L
                                + position.getZ(),
                        CLIENT_SYNC_INTERVAL)
                == 0L;
    }

    private FluidPipeBlock pipe() {
        if (liveState().getBlock() instanceof FluidPipeBlock pipe) {
            return pipe;
        }
        throw new IllegalStateException(
                "Fluid pipe block entity has non-pipe state");
    }

    private void breakPipe(Level level, BlockPos pos, BlockState state) {
        level.playSound(
                null,
                pos,
                SoundEvents.GLASS_BREAK,
                SoundSource.BLOCKS,
                1.0F,
                0.8F);
        level.gameEvent(
                GameEvent.BLOCK_DESTROY,
                pos,
                GameEvent.Context.of(state));
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeTanks(tag, registries);
        covers.save(tag, registries);
        writeFailures(tag, failures.snapshot());
        tag.putInt("next_output", nextOutput);
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readTanks(tag, registries);
        int rejectedCovers = covers.load(tag, registries);
        FluidPipeFailureState.DecodeResult decoded =
                readFailures(tag);
        failures.restore(decoded.snapshot());
        recoveredInvalidFields = rejectedCovers
                + decoded.rejectedFields();
        nextOutput = tag.getInt("next_output");
    }

    @Override
    public CompoundTag getUpdateTag(
            HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writeTanks(tag, registries);
        covers.save(tag, registries);
        writeFailures(tag, failures.snapshot());
        return tag;
    }

    @Override
    public void handleUpdateTag(
            CompoundTag tag, HolderLookup.Provider registries) {
        readTanks(tag, registries);
        covers.load(tag, registries);
        failures.restore(readFailures(tag).snapshot());
    }

    @Override
    public void onDataPacket(
            Connection connection,
            ClientboundBlockEntityDataPacket packet,
            HolderLookup.Provider registries) {
        if (packet.getTag() != null) {
            handleUpdateTag(packet.getTag(), registries);
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            clientSyncPending = false;
            BlockState state = liveState();
            level.sendBlockUpdated(
                    worldPosition,
                    state,
                    state,
                    Block.UPDATE_CLIENTS);
        }
    }

    private BlockState liveState() {
        return level == null
                ? getBlockState()
                : level.getBlockState(worldPosition);
    }

    private static int runtimeCapacity(BlockState state) {
        if (!(state.getBlock() instanceof FluidPipeBlock pipe)) {
            return 1;
        }
        long source = pipe.pipe().fluid().capacityMb();
        return (int) Math.max(
                1L, Math.min(Integer.MAX_VALUE, source));
    }

    private static int tankCountFor(BlockState state) {
        if (!(state.getBlock() instanceof FluidPipeBlock pipe)) {
            return 1;
        }
        return Math.max(1, pipe.pipe().tankCount());
    }

    private int tankCapacity() {
        return tanks[0].getCapacity();
    }

    private int storedAmount() {
        int total = 0;
        for (FluidTank current : tanks) {
            total += current.getFluidAmount();
        }
        return total;
    }

    private boolean allEmpty() {
        for (FluidTank current : tanks) {
            if (!current.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private FluidStack firstStored() {
        for (FluidTank current : tanks) {
            if (!current.isEmpty()) {
                return current.getFluid();
            }
        }
        return tanks[0].getFluid();
    }

    private FluidTank fillTarget(FluidStack resource) {
        if (resource.isEmpty()) {
            return tanks[0];
        }
        for (FluidTank current : tanks) {
            if (!current.isEmpty()
                    && FluidStack.isSameFluidSameComponents(
                            current.getFluid(), resource)
                    && current.getFluidAmount() < current.getCapacity()) {
                return current;
            }
        }
        for (FluidTank current : tanks) {
            if (current.isEmpty()) {
                return current;
            }
        }
        for (FluidTank current : tanks) {
            if (current.isEmpty()
                    || FluidStack.isSameFluidSameComponents(
                            current.getFluid(), resource)) {
                return current;
            }
        }
        return tanks[0];
    }

    private int fillMatching(
            FluidStack resource, IFluidHandler.FluidAction action) {
        if (resource.isEmpty()) {
            return 0;
        }
        int remaining = resource.getAmount();
        int accepted = 0;
        for (FluidTank current : tanks) {
            if (remaining <= 0) {
                break;
            }
            if (current.isEmpty()
                    || !FluidStack.isSameFluidSameComponents(
                            current.getFluid(), resource)) {
                continue;
            }
            int moved = current.fill(
                    resource.copyWithAmount(remaining), action);
            accepted += moved;
            remaining -= moved;
        }
        for (FluidTank current : tanks) {
            if (remaining <= 0) {
                break;
            }
            if (!current.isEmpty()) {
                continue;
            }
            int moved = current.fill(
                    resource.copyWithAmount(remaining), action);
            accepted += moved;
            remaining -= moved;
        }
        return accepted;
    }

    private FluidTank drainTarget(FluidStack resource) {
        if (!resource.isEmpty()) {
            for (FluidTank current : tanks) {
                if (!current.isEmpty()
                        && FluidStack.isSameFluidSameComponents(
                                current.getFluid(), resource)) {
                    return current;
                }
            }
            return tanks[0];
        }
        for (FluidTank current : tanks) {
            if (!current.isEmpty()) {
                return current;
            }
        }
        return tanks[0];
    }

    private void writeTanks(
            CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("tank", tanks[0].writeToNBT(registries, new CompoundTag()));
        if (tanks.length > 1) {
            net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
            for (FluidTank current : tanks) {
                list.add(current.writeToNBT(registries, new CompoundTag()));
            }
            tag.put("tanks", list);
            tag.putInt("tank_count", tanks.length);
        }
    }

    private void readTanks(
            CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains("tanks")) {
            net.minecraft.nbt.ListTag list = tag.getList("tanks", Tag.TAG_COMPOUND);
            int limit = Math.min(tanks.length, list.size());
            for (int index = 0; index < limit; index++) {
                tanks[index].readFromNBT(
                        registries, list.getCompound(index));
            }
            return;
        }
        tanks[0].readFromNBT(registries, tag.getCompound("tank"));
    }

    private final class FluidCoverContext
            implements CoverBehavior.TransferContext {
        private final Level world;
        private final Direction side;

        private FluidCoverContext(Level world, Direction side) {
            this.world = world;
            this.side = side;
        }

        @Override
        public CoverDefinition.Medium medium() {
            return CoverDefinition.Medium.FLUID;
        }

        @Override
        public Direction side() {
            return side;
        }

        @Override
        public int storedAmount() {
            return FluidPipeBlockEntity.this.storedAmount();
        }

        @Override
        public int capacity() {
            return totalCapacity();
        }

        @Override
        public int transferItems(
                int amount,
                Optional<String> matchId,
                CoverDefinition.TransferMode mode) {
            return 0;
        }

        @Override
        public int transferFluids(
                int amount,
                Optional<String> matchId,
                CoverDefinition.TransferMode mode) {
            if (amount <= 0) {
                return 0;
            }
            BlockPos sourcePos = worldPosition.relative(side);
            if (!world.hasChunkAt(sourcePos)
                    || world.getBlockEntity(sourcePos)
                            == FluidPipeBlockEntity.this) {
                return 0;
            }
            IFluidHandler source;
            try {
                source = world.getCapability(
                        Capabilities.FluidHandler.BLOCK,
                        sourcePos,
                        side.getOpposite());
            } catch (RuntimeException failure) {
                PipeTransferDiagnostics.warnOnce(
                        "fluid cover discovery",
                        null,
                        "Fluid cover discovery failed at " + sourcePos,
                        failure);
                return 0;
            }
            return source == null
                    ? 0
                    : pumpFrom(source, side, amount, matchId, mode);
        }

        @Override
        public Level world() {
            return world;
        }

        @Override
        public BlockPos hostPos() {
            return worldPosition;
        }
    }

    private static void writeFailures(
            CompoundTag tag, FluidPipeFailureState.Snapshot snapshot) {
        tag.putInt("over_temperature", snapshot.overTemperatureEvents());
        tag.putInt("corrosion", snapshot.corrosionEvents());
        tag.putInt("gas_leak", snapshot.gasLeakEvents());
        tag.putLong("backpressure", snapshot.backpressureAmount());
        tag.putString(
                "pending_failure",
                snapshot.pendingFailure().serializedName());
    }

    private static FluidPipeFailureState.DecodeResult readFailures(
            CompoundTag tag) {
        return FluidPipeFailureState.decodeSnapshot(
                tag.getInt("over_temperature"),
                tag.getInt("corrosion"),
                tag.getInt("gas_leak"),
                tag.getLong("backpressure"),
                tag.getString("pending_failure"));
    }

    private final class SidedHandler implements IFluidHandler {
        private final Direction side;

        private SidedHandler(Direction side) {
            this.side = side;
        }

        @Override
        public int getTanks() {
            return tanks.length;
        }

        @Override
        public FluidStack getFluidInTank(int tankIndex) {
            return tanks[tankIndex].getFluid().copy();
        }

        @Override
        public int getTankCapacity(int tankIndex) {
            return tanks[tankIndex].getCapacity();
        }

        @Override
        public boolean isFluidValid(
                int tankIndex, FluidStack stack) {
            return covers.allowsIncoming(
                            side,
                            CoverDefinition.Medium.FLUID,
                            storedAmount(),
                            totalCapacity())
                    && covers.matches(side, stack)
                    && validateFluid(stack) == Failure.NONE;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()
                    || !AbstractPipeBlock.isConnected(
                            liveState(), side)
                    || !covers.allowsIncoming(
                            side,
                            CoverDefinition.Medium.FLUID,
                            storedAmount(),
                            totalCapacity())
                    || !covers.matches(side, resource)) {
                return 0;
            }
            Failure failure = validateFluid(resource);
            if (failure != Failure.NONE) {
                if (action.execute()) {
                    recordFailure(failure, resource.getAmount());
                }
                return 0;
            }
            int coverLimit = covers.limitIncoming(
                    side,
                    CoverDefinition.Medium.FLUID,
                    storedAmount(),
                    totalCapacity(),
                    resource.getAmount());
            if (coverLimit <= 0) {
                return 0;
            }
            FluidStack offered = coverLimit == resource.getAmount()
                    ? resource
                    : resource.copyWithAmount(coverLimit);
            int accepted = fillMatching(offered, action);
            if (action.execute()) {
                if (accepted < resource.getAmount()) {
                    recordFailure(
                            Failure.BACKPRESSURE,
                            resource.getAmount() - accepted);
                }
                if (accepted > 0 && level != null) {
                    receivedFrom = side;
                    receivedAtTick = level.getGameTime();
                }
            }
            return accepted;
        }

        @Override
        public FluidStack drain(
                FluidStack resource, FluidAction action) {
            if (!covers.allowsOutgoing(
                            side,
                            CoverDefinition.Medium.FLUID,
                            storedAmount(),
                            totalCapacity())
                    || !covers.matches(side, resource)) {
                return FluidStack.EMPTY;
            }
            return drainTarget(resource).drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            FluidTank source = drainTarget(FluidStack.EMPTY);
            FluidStack candidate = source.getFluid().copyWithAmount(
                    Math.min(maxDrain, source.getFluidAmount()));
            if (!covers.allowsOutgoing(
                            side,
                            CoverDefinition.Medium.FLUID,
                            storedAmount(),
                            totalCapacity())
                    || !covers.matches(side, candidate)) {
                return FluidStack.EMPTY;
            }
            return source.drain(maxDrain, action);
        }
    }
}
