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
    private final EnergyConverterProfile profile;
    public static final int STEAM_CAPACITY =
            EnergyConverterCatalog.require("cruciblecraft:bronze_steam_engine")
                    .inputCapacity();
    public static final long KU_CAPACITY =
            EnergyConverterCatalog.require("cruciblecraft:bronze_steam_engine")
                    .outputCapacity();
    /**
     * CC design policy: fixed 12 KU/t. Source 1302 derives only nominal
     * mOutput=24/STEAM_PER_EU(2)=12; GT6 emits state-dependent 6..24 KU/t.
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
                stack -> stack.is(
                        net.minecraft.world.level.material.Fluids.WATER)) {
            @Override protected void onContentsChanged() { markMutation(); }
        };
        kinetic = new KineticBuffer(
                profile.outputCapacity(),
                profile.outputPacket().size());
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SteamEngineBlockEntity engine) {
        engine.convertOneBatch();
        engine.emitKinetic(level, pos, state);
        long phaseKey = CheckpointDecisions.phaseKey(pos.getX(), pos.getY(), pos.getZ());
        if (engine.checkpoint.shouldSync(false, level.getGameTime(), phaseKey, 20)) {
            engine.syncToClient();
            engine.checkpoint.synced();
        }
    }

    private void convertOneBatch() {
        if (steam.getFluidAmount()
                < SteamConversion.ENGINE_STEAM_PER_BATCH) {
            setStatus("no_steam");
            return;
        }
        if (kinetic.room() < SteamConversion.KU_PER_ENGINE_BATCH) {
            setStatus("kinetic_full");
            return;
        }
        if (exhaust.getSpace()
                < SteamConversion.EXHAUST_WATER_PER_BATCH) {
            setStatus("exhaust_full");
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
        FluidStack water = new FluidStack(
                net.minecraft.world.level.material.Fluids.WATER,
                waterReturned);
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
     * GT6 {@code MultiTileEntityEngineSteam}: emit one signed KU packet, then
     * always {@code mEnergy -= tOutput} even when the neighbor took nothing.
     */
    private void emitKinetic(Level level, BlockPos pos, BlockState state) {
        long rate = outputRate();
        if (kinetic.stored() <= rate) {
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
    private long outputRate() {
        return profile.outputPacket().size();
    }

    public int steamAmount() { return steam.getFluidAmount(); }
    public int exhaustAmount() { return exhaust.getFluidAmount(); }
    public String status() { return status; }
    @Override public boolean handles(EnergyType type, Direction side) {
        Direction front = front();
        return type == EnergyType.KINETIC_PUSH
                && front != null
                && MachineSideRules.engineExposesKinetic(front, side);
    }
    @Override public long outputSize(EnergyType type, Direction side) {
        return handles(type, side)
                        && kinetic.stored() >= outputRate()
                        && outputBudget.claim(gameTime(), 1L, 1L, true) > 0L
                ? kinetic.strokeSign() * outputRate()
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
                || size != kinetic.strokeSign() * outputRate()
                || kinetic.stored() < outputRate()
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
            long removed = kinetic.extract(outputRate(), false);
            if (removed != outputRate()) {
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
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("steam")) steam.readFromNBT(registries, tag.getCompound("steam"));
        if (tag.contains("exhaust")) {
            exhaust.readFromNBT(registries, tag.getCompound("exhaust"));
        }
        kinetic = new KineticBuffer(
                profile.outputCapacity(),
                outputRate(),
                tag.getLong("kinetic"),
                tag.getInt("stroke_sign"));
        status = tag.getString("status");
        if (status.isBlank()) {
            status = steam.isEmpty() ? "no_steam" : "running";
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
                : new FluidStack(
                        net.minecraft.world.level.material.Fluids.WATER,
                        exhaustAmount));
        kinetic = new KineticBuffer(
                profile.outputCapacity(),
                outputRate(),
                tag.getLong("kinetic"),
                tag.getInt("stroke_sign"));
        status = tag.getString("status");
        if (status.isBlank()) {
            status = steam.isEmpty() ? "no_steam" : "running";
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
            return steam.fill(resource, action);
        }

        @Override public FluidStack drain(FluidStack resource, FluidAction action) {
            return exhaust.drain(resource, action);
        }

        @Override public FluidStack drain(int maxDrain, FluidAction action) {
            return exhaust.drain(maxDrain, action);
        }
    }
}
