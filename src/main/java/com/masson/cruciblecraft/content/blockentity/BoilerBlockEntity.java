package com.masson.cruciblecraft.content.blockentity;

import java.util.Optional;

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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * GT6 {@code MultiTileEntityBoilerTank}: accept HU from any side
 * ({@code doInject} always returns the offered amount), store heat up to
 * {@code mOutput * 10000}, and convert 80 HU + 1 water → 160 steam.
 */
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
    private long heat;
    private final long heatCapacity;
    private final long steamOutputSu;
    private int efficiency = 10_000;
    private int coolDownResetTimer = 128;
    private String status = "no_water";

    public BoilerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BOILER.get(), pos, state);
        if (!(state.getBlock() instanceof com.masson.cruciblecraft.energy.converter.EnergyConverterHost host)) {
            throw new IllegalArgumentException("Boiler requires a catalog block");
        }
        profile = host.converterProfile();
        water = tank(
                profile.inputCapacity(),
                this::acceptsBoilerWater);
        steam = tank(
                profile.outputCapacity(),
                stack -> stack.is(ModFluids.STEAM_SOURCE.get()));
        long recommendedHu = Math.max(1L, profile.inputPacket().maxAmountPerTick());
        steamOutputSu = Math.multiplyExact(recommendedHu, 2L);
        heatCapacity = Math.multiplyExact(steamOutputSu, 10_000L);
    }

    private FluidTank tank(int capacity, java.util.function.Predicate<FluidStack> validator) {
        return new FluidTank(capacity, validator) {
            @Override protected void onContentsChanged() { markMutation(); }
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BoilerBlockEntity boiler) {
        boiler.produce();
        boiler.coolDown();
        boiler.pushSteam();
        boiler.explodeIfUnsafe();
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
        if (heat < SteamConversion.HU_PER_BATCH) {
            setStatus("no_heat");
            return;
        }
        if (steam.getSpace() < SteamConversion.STEAM_PER_BATCH) {
            setStatus("steam_full");
            return;
        }
        int capBatches = Math.max(1, steam.getCapacity() / 2_560);
        int batches = Math.min(
                capBatches,
                SteamConversion.boilerBatches(
                        water.getFluidAmount(),
                        steam.getSpace(),
                        heatUnitsForConversion()));
        if (batches <= 0) {
            return;
        }
        if (level.random.nextInt(10) == 0
                && efficiency > 5_000
                && !isDistilledWater(water.getFluid())) {
            efficiency = Math.max(5_000, efficiency - batches);
        }
        int waterRequired = Math.multiplyExact(
                batches, SteamConversion.WATER_PER_BATCH);
        int steamProduced = Math.toIntExact(
                Math.multiplyExact(
                        (long) batches * (long) efficiency,
                        SteamConversion.STEAM_PER_BATCH)
                        / 10_000L);
        if (steamProduced <= 0) {
            return;
        }
        steamProduced = Math.min(steamProduced, steam.getSpace());
        FluidStack simulatedWater = water.drain(
                waterRequired, IFluidHandler.FluidAction.SIMULATE);
        FluidStack steamBatch =
                new FluidStack(ModFluids.STEAM_SOURCE.get(), steamProduced);
        int simulatedSteam = steam.fill(
                steamBatch, IFluidHandler.FluidAction.SIMULATE);
        if (simulatedWater.getAmount() != waterRequired
                || !acceptsBoilerWater(simulatedWater)
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
        heat -= (long) batches * SteamConversion.HU_PER_BATCH;
        coolDownResetTimer = 128;
        setStatus("running");
        markMutation();
    }

    private void coolDown() {
        if (coolDownResetTimer-- > 0) {
            return;
        }
        coolDownResetTimer = 0;
        long heatLoss = steamOutputSu * 64L / 2L;
        heat = Math.max(0L, heat - heatLoss);
        int steamLoss = Math.toIntExact(Math.min(
                Integer.MAX_VALUE, steamOutputSu * 64L));
        if (steamLoss > 0 && !steam.isEmpty()) {
            steam.drain(steamLoss, IFluidHandler.FluidAction.EXECUTE);
        }
        if (heat <= 0L) {
            heat = 0L;
            coolDownResetTimer = 128;
        }
        markMutation();
    }

    private int heatUnitsForConversion() {
        return (int) Math.min(Integer.MAX_VALUE, heat);
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        return type == EnergyType.HEAT && side != null;
    }

    @Override
    public long insert(
            EnergyType type,
            long size,
            long amount,
            Direction side,
            boolean simulate) {
        if (!handles(type, side) || size == 0L || amount <= 0L) {
            return 0L;
        }
        long units = EnergyPackets.units(size, amount);
        if (!simulate && units > 0L) {
            heat = EnergyPackets.add(heat, units);
            coolDownResetTimer = Math.max(coolDownResetTimer, 32);
            markMutation();
        }
        return amount;
    }

    @Override
    public long stored(EnergyType type) {
        return type == EnergyType.HEAT ? heat : 0L;
    }

    @Override
    public long capacity(EnergyType type) {
        return type == EnergyType.HEAT ? heatCapacity : 0L;
    }

    private void pushSteam() {
        if (level == null || steam.isEmpty()) {
            return;
        }
        long excess = (long) steam.getFluidAmount() - steam.getCapacity() / 2L;
        if (excess <= 0L) {
            return;
        }
        BlockPos targetPosition = worldPosition.above();
        if (!level.hasChunkAt(targetPosition)) {
            return;
        }
        IFluidHandler target = level.getCapability(
                Capabilities.FluidHandler.BLOCK, targetPosition, Direction.DOWN);
        if (target == null) {
            return;
        }
        long rate = excess > steam.getCapacity() / 4L
                ? steamOutputSu * 2L
                : steamOutputSu;
        ExactFluidTransfer.move(
                steam,
                target,
                Math.toIntExact(Math.min(rate, excess)));
    }

    private void explodeIfUnsafe() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (heat <= heatCapacity && steam.getFluidAmount() < steam.getCapacity()) {
            return;
        }
        float power = (float) Math.max(
                1.0, Math.sqrt(steam.getFluidAmount()) / 100.0);
        level.explode(
                null,
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5,
                power,
                Level.ExplosionInteraction.TNT);
    }

    private boolean acceptsBoilerWater(FluidStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.is(Fluids.WATER)) {
            return true;
        }
        return distilledWater().map(stack::is).orElse(false);
    }

    private boolean isDistilledWater(FluidStack stack) {
        return distilledWater().map(stack::is).orElse(false);
    }

    private static Optional<Fluid> distilledWater() {
        return ModFluids.materialFluid("water_distilled");
    }

    public IFluidHandler fluids(Direction side) {
        if (side == null) return unsided;
        if (MachineSideRules.boilerExposesSteam(side)) return output;
        return MachineSideRules.boilerAcceptsWater(side) ? input : null;
    }
    public int waterAmount() { return water.getFluidAmount(); }
    public int steamAmount() { return steam.getFluidAmount(); }
    public int waterCapacity() { return water.getCapacity(); }
    public int steamCapacity() { return steam.getCapacity(); }

    public boolean fillSteam(int amount) {
        int filled = steam.fill(
                new FluidStack(ModFluids.STEAM_SOURCE.get(), amount),
                IFluidHandler.FluidAction.EXECUTE);
        return filled == amount;
    }

    /** GT6 BoilerTank: trash water if present, otherwise steam. */
    public boolean trashWithPlunger() {
        if (!water.isEmpty()) {
            water.setFluid(FluidStack.EMPTY);
            markMutation();
            return true;
        }
        if (steam.isEmpty()) {
            return false;
        }
        steam.setFluid(FluidStack.EMPTY);
        markMutation();
        return true;
    }
    public int accumulatedHu() {
        return heatUnitsForConversion();
    }
    public int efficiencyBasisPoints() {
        return efficiency;
    }
    public String status() { return status; }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("water", water.writeToNBT(registries, new CompoundTag()));
        tag.put("steam", steam.writeToNBT(registries, new CompoundTag()));
        tag.putLong("heat", heat);
        tag.putInt("accumulated_hu", heatUnitsForConversion());
        tag.putInt("efficiency", efficiency);
        tag.putInt("cooldown", coolDownResetTimer);
        tag.putString("status", status);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("water")) water.readFromNBT(registries, tag.getCompound("water"));
        if (tag.contains("steam")) steam.readFromNBT(registries, tag.getCompound("steam"));
        clampTank(water);
        clampTank(steam);
        if (tag.contains("heat")) {
            heat = Math.max(0L, Math.min(heatCapacity, tag.getLong("heat")));
        } else {
            heat = Math.max(
                    0L,
                    Math.min(heatCapacity, tag.getInt("accumulated_hu")));
        }
        if (tag.contains("efficiency")) {
            efficiency = Math.max(5_000, Math.min(10_000, tag.getInt("efficiency")));
        }
        coolDownResetTimer = Math.max(0, tag.getInt("cooldown"));
        if (coolDownResetTimer == 0 && !tag.contains("cooldown")) {
            coolDownResetTimer = 128;
        }
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
            return tank == 0 && waterInput && acceptsBoilerWater(stack);
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
        tag.putLong("heat", heat);
        tag.putInt("accumulated_hu", heatUnitsForConversion());
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

    private static void clampTank(FluidTank tank) {
        int amount = tank.getFluidAmount();
        int capacity = tank.getCapacity();
        if (amount > capacity) {
            tank.setFluid(tank.getFluid().copyWithAmount(capacity));
        }
    }
}
