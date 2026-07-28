package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModCapabilities;
import com.masson.cruciblecraft.registry.ModFluids;
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

public final class BoilerBlockEntity extends BlockEntity {
    public static final int WATER_CAPACITY = 16_000;
    public static final int STEAM_CAPACITY = 64_000;
    public static final int STEAM_TRANSFER = 1_280;
    private final FluidTank water = tank(WATER_CAPACITY,
            stack -> stack.is(net.minecraft.world.level.material.Fluids.WATER));
    private final FluidTank steam = tank(STEAM_CAPACITY,
            stack -> stack.is(ModFluids.STEAM_SOURCE.get()));
    private final IFluidHandler input = new BoilerHandler(true, false);
    private final IFluidHandler output = new BoilerHandler(false, true);
    private final IFluidHandler unsided = new BoilerHandler(true, true);
    private int accumulatedHu;

    public BoilerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BOILER.get(), pos, state);
    }

    private FluidTank tank(int capacity, java.util.function.Predicate<FluidStack> validator) {
        return new FluidTank(capacity, validator) {
            @Override protected void onContentsChanged() { changedAndSync(); }
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BoilerBlockEntity boiler) {
        boiler.produce();
        boiler.pushSteam();
    }

    private void produce() {
        if (level == null || water.getFluidAmount() < SteamConversion.WATER_PER_BATCH
                || steam.getSpace() < SteamConversion.STEAM_PER_BATCH) return;
        var source = level.getCapability(ModCapabilities.HEAT_SOURCE, worldPosition.below(), Direction.UP);
        if (source == null) return;
        int needed = SteamConversion.HU_PER_BATCH - accumulatedHu;
        int available = (int) Math.floor(source.extractHeat(needed, true));
        if (available <= 0) return;
        int pulled = (int) Math.floor(source.extractHeat(available, false));
        if (pulled <= 0) return;
        accumulatedHu += pulled;
        int batches = SteamConversion.boilerBatches(water.getFluidAmount(), steam.getSpace(), accumulatedHu);
        if (batches > 0) {
            water.drain(batches, IFluidHandler.FluidAction.EXECUTE);
            steam.fill(new FluidStack(ModFluids.STEAM_SOURCE.get(), batches * SteamConversion.STEAM_PER_BATCH),
                    IFluidHandler.FluidAction.EXECUTE);
            accumulatedHu -= batches * SteamConversion.HU_PER_BATCH;
        }
        changedAndSync();
    }

    private void pushSteam() {
        if (level == null || steam.isEmpty()) return;
        IFluidHandler target = level.getCapability(
                Capabilities.FluidHandler.BLOCK, worldPosition.above(), Direction.DOWN);
        if (target == null) return;
        FluidStack offered = steam.drain(STEAM_TRANSFER, IFluidHandler.FluidAction.SIMULATE);
        int accepted = target.fill(offered, IFluidHandler.FluidAction.SIMULATE);
        if (accepted > 0) {
            FluidStack transfer = offered.copyWithAmount(accepted);
            int actuallyAccepted = target.fill(transfer, IFluidHandler.FluidAction.EXECUTE);
            if (actuallyAccepted > 0) {
                steam.drain(actuallyAccepted, IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    public IFluidHandler fluids(Direction side) {
        if (side == null) return unsided;
        if (MachineSideRules.boilerExposesSteam(side)) return output;
        return MachineSideRules.boilerAcceptsWater(side) ? input : null;
    }
    public int waterAmount() { return water.getFluidAmount(); }
    public int steamAmount() { return steam.getFluidAmount(); }
    public int accumulatedHu() { return accumulatedHu; }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("water", water.writeToNBT(registries, new CompoundTag()));
        tag.put("steam", steam.writeToNBT(registries, new CompoundTag()));
        tag.putInt("accumulated_hu", accumulatedHu);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("water")) water.readFromNBT(registries, tag.getCompound("water"));
        if (tag.contains("steam")) steam.readFromNBT(registries, tag.getCompound("steam"));
        accumulatedHu = Math.max(0, Math.min(SteamConversion.HU_PER_BATCH - 1, tag.getInt("accumulated_hu")));
    }

    private final class BoilerHandler implements IFluidHandler {
        private final boolean waterInput, steamOutput;
        private BoilerHandler(boolean waterInput, boolean steamOutput) {
            this.waterInput = waterInput; this.steamOutput = steamOutput;
        }
        @Override public int getTanks() { return 2; }
        @Override public FluidStack getFluidInTank(int tank) { return (tank == 0 ? water : steam).getFluid(); }
        @Override public int getTankCapacity(int tank) {
            return tank == 0 ? WATER_CAPACITY : STEAM_CAPACITY;
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
        saveAdditional(tag, registries);
        return tag;
    }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    private void changedAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(),
                    net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
    }
}
