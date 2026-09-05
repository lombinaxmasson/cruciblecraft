package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.energy.EnergyPackets;
import com.masson.cruciblecraft.energy.converter.EnergyConverterCatalog;
import com.masson.cruciblecraft.energy.converter.EnergyConverterProfile;
import com.masson.cruciblecraft.machine.CheckpointDecisions;
import com.masson.cruciblecraft.machine.component.CheckpointTracker;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModFluids;
import com.masson.cruciblecraft.steam.ExactFluidTransfer;
import com.masson.cruciblecraft.steam.SteamConversion;
import com.masson.cruciblecraft.steam.MachineSideRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public final class BoilerBlockEntity extends BlockEntity implements IEnergyHandler {
    private final EnergyConverterProfile profile;
    public static final int WATER_CAPACITY =
            EnergyConverterCatalog.require("cruciblecraft:bronze_boiler")
                    .inputCapacity();
    public static final int STEAM_CAPACITY =
            EnergyConverterCatalog.require("cruciblecraft:bronze_boiler")
                    .outputCapacity();
    public static final int STEAM_TRANSFER = Math.toIntExact(
            EnergyConverterCatalog.require("cruciblecraft:bronze_boiler")
                    .outputPacket().maxAmountPerTick());
    private final FluidTank water;
    private final FluidTank steam;
    private final IFluidHandler input = new BoilerHandler(true, false);
    private final IFluidHandler output = new BoilerHandler(false, true);
    private final IFluidHandler unsided = new BoilerHandler(true, true);
    private final CheckpointTracker checkpoint = new CheckpointTracker();
    private int accumulatedHu;
    private String status = "no_water";

    public BoilerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BOILER.get(), pos, state);
        if (!(state.getBlock() instanceof com.masson.cruciblecraft.energy.converter.EnergyConverterHost host)) {
            throw new IllegalArgumentException("Boiler requires a catalog block");
        }
        profile = host.converterProfile();
        water = tank(
                profile.inputCapacity(),
                stack -> stack.is(net.minecraft.world.level.material.Fluids.WATER));
        steam = tank(
                profile.outputCapacity(),
                stack -> stack.is(ModFluids.STEAM_SOURCE.get()));
    }

    private FluidTank tank(int capacity, java.util.function.Predicate<FluidStack> validator) {
        return new FluidTank(capacity, validator) {
            @Override protected void onContentsChanged() { markMutation(); }
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BoilerBlockEntity boiler) {
        boiler.produce();
        boiler.pushSteam();
        long phaseKey = CheckpointDecisions.phaseKey(pos.getX(), pos.getY(), pos.getZ());
        if (boiler.checkpoint.shouldSync(false, level.getGameTime(), phaseKey, 20)) {
            boiler.syncToClient();
            boiler.checkpoint.synced();
        }
    }

    private void produce() {
        if (level == null) {
            return;
        }
        if (water.getFluidAmount() < SteamConversion.WATER_PER_BATCH) {
            setStatus("no_water");
            return;
        }
        if (steam.getSpace() < SteamConversion.STEAM_PER_BATCH) {
            setStatus("steam_full");
            return;
        }
        if (accumulatedHu < SteamConversion.HU_PER_BATCH) {
            setStatus("no_heat");
            return;
        }
        int batches = SteamConversion.boilerBatches(water.getFluidAmount(), steam.getSpace(), accumulatedHu);
        if (batches <= 0) {
            return;
        }
        int waterRequired = Math.multiplyExact(
                batches, SteamConversion.WATER_PER_BATCH);
        int steamProduced = Math.multiplyExact(
                batches, SteamConversion.STEAM_PER_BATCH);
        FluidStack simulatedWater = water.drain(
                waterRequired, IFluidHandler.FluidAction.SIMULATE);
        FluidStack steamBatch =
                new FluidStack(ModFluids.STEAM_SOURCE.get(), steamProduced);
        int simulatedSteam = steam.fill(
                steamBatch, IFluidHandler.FluidAction.SIMULATE);
        if (simulatedWater.getAmount() != waterRequired
                || !simulatedWater.is(
                        net.minecraft.world.level.material.Fluids.WATER)
                || simulatedSteam != steamProduced) {
            throw new IllegalStateException(
                    "Boiler conversion changed after its integer batch plan");
        }
        FluidStack drained = water.drain(
                waterRequired, IFluidHandler.FluidAction.EXECUTE);
        int filled = steam.fill(
                steamBatch, IFluidHandler.FluidAction.EXECUTE);
        if (drained.getAmount() != waterRequired
                || filled != steamProduced) {
            throw new IllegalStateException(
                    "Boiler execute differed from simulation");
        }
        accumulatedHu -= batches * SteamConversion.HU_PER_BATCH;
        setStatus("running");
        markMutation();
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        return type == EnergyType.HEAT && side == Direction.DOWN;
    }

    @Override
    public long insert(
            EnergyType type,
            long size,
            long amount,
            Direction side,
            boolean simulate) {
        if (!handles(type, side)
                || size == 0L
                || amount <= 0L
                || water.getFluidAmount() < SteamConversion.WATER_PER_BATCH
                || steam.getSpace() < SteamConversion.STEAM_PER_BATCH) {
            return 0L;
        }
        long room = SteamConversion.HU_PER_BATCH - accumulatedHu;
        long accepted = Math.min(amount, EnergyPackets.packetsForUnits(size, room));
        if (!simulate && accepted > 0L) {
            accumulatedHu += (int) EnergyPackets.units(size, accepted);
            markMutation();
        }
        return accepted;
    }

    @Override
    public long stored(EnergyType type) {
        return type == EnergyType.HEAT ? accumulatedHu : 0L;
    }

    @Override
    public long capacity(EnergyType type) {
        return type == EnergyType.HEAT ? SteamConversion.HU_PER_BATCH : 0L;
    }

    private void pushSteam() {
        if (level == null || steam.isEmpty()) return;
        BlockPos targetPosition = worldPosition.above();
        if (!level.hasChunkAt(targetPosition)) return;
        IFluidHandler target = level.getCapability(
                Capabilities.FluidHandler.BLOCK, targetPosition, Direction.DOWN);
        if (target == null) return;
        ExactFluidTransfer.move(
                steam,
                target,
                Math.toIntExact(profile.outputPacket().maxAmountPerTick()));
    }

    public IFluidHandler fluids(Direction side) {
        if (side == null) return unsided;
        if (MachineSideRules.boilerExposesSteam(side)) return output;
        return MachineSideRules.boilerAcceptsWater(side) ? input : null;
    }
    public int waterAmount() { return water.getFluidAmount(); }
    public int steamAmount() { return steam.getFluidAmount(); }
    public int accumulatedHu() { return accumulatedHu; }
    public String status() { return status; }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("water", water.writeToNBT(registries, new CompoundTag()));
        tag.put("steam", steam.writeToNBT(registries, new CompoundTag()));
        tag.putInt("accumulated_hu", accumulatedHu);
        tag.putString("status", status);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("water")) water.readFromNBT(registries, tag.getCompound("water"));
        if (tag.contains("steam")) steam.readFromNBT(registries, tag.getCompound("steam"));
        accumulatedHu = Math.max(
                0,
                Math.min(SteamConversion.HU_PER_BATCH, tag.getInt("accumulated_hu")));
        status = tag.getString("status");
        if (status.isBlank()) {
            status = water.isEmpty() ? "no_water" : "no_heat";
        }
    }

    private final class BoilerHandler implements IFluidHandler {
        private final boolean waterInput, steamOutput;
        private BoilerHandler(boolean waterInput, boolean steamOutput) {
            this.waterInput = waterInput; this.steamOutput = steamOutput;
        }
        @Override public int getTanks() { return 2; }
        @Override public FluidStack getFluidInTank(int tank) { return (tank == 0 ? water : steam).getFluid(); }
        @Override public int getTankCapacity(int tank) {
            return tank == 0 ? water.getCapacity() : steam.getCapacity();
        }
        @Override public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && waterInput && stack.is(net.minecraft.world.level.material.Fluids.WATER);
        }
        @Override public int fill(FluidStack resource, FluidAction action) {
            return waterInput ? water.fill(resource, action) : 0;
        }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) {
            return steamOutput ? steam.drain(resource, action) : FluidStack.EMPTY;
        }
        @Override public FluidStack drain(int maxDrain, FluidAction action) {
            return steamOutput ? steam.drain(maxDrain, action) : FluidStack.EMPTY;
        }
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("water", water.writeToNBT(registries, new CompoundTag()));
        tag.put("steam", steam.writeToNBT(registries, new CompoundTag()));
        tag.putInt("accumulated_hu", accumulatedHu);
        tag.putString("status", status);
        return tag;
    }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    private void markMutation() {
        setChanged();
        checkpoint.markSyncPending();
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(),
                    net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
    }

    private void setStatus(String next) {
        if (!status.equals(next)) {
            status = next;
            markMutation();
        }
    }
}
