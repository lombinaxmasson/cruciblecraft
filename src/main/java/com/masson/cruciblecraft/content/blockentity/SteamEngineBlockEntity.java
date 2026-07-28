package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.api.kinetic.IKineticSource;
import com.masson.cruciblecraft.api.kinetic.KineticType;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModFluids;
import com.masson.cruciblecraft.steam.SteamConversion;
import com.masson.cruciblecraft.steam.KineticBuffer;
import com.masson.cruciblecraft.steam.MachineSideRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public final class SteamEngineBlockEntity extends BlockEntity implements IKineticSource {
    public static final int STEAM_CAPACITY = 16_000;
    public static final long KU_CAPACITY = 1_024;
    /** Bronze nominal packet, matching the documented ~24 KU/t GT6 tier. */
    public static final long OUTPUT_RATE = 24;
    private final FluidTank steam = new FluidTank(
            STEAM_CAPACITY, stack -> stack.is(ModFluids.STEAM_SOURCE.get())) {
        @Override protected void onContentsChanged() { changedAndSync(); }
    };
    private KineticBuffer kinetic = new KineticBuffer(KU_CAPACITY, OUTPUT_RATE);
    private long extractionTick = Long.MIN_VALUE;
    private long extractedThisTick;

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
            engine.changedAndSync();
        }
    }
    private static int steamAmount(SteamEngineBlockEntity engine) { return engine.steam.getFluidAmount(); }
    public IFluidHandler fluids(Direction side) {
        Direction front = getBlockState().getValue(
                com.masson.cruciblecraft.content.block.SteamEngineBlock.FACING);
        return MachineSideRules.engineAcceptsSteam(front, side) ? steam : null;
    }
    public int steamAmount() { return steam.getFluidAmount(); }
    @Override public KineticType type() { return KineticType.KU; }
    @Override public long extract(long maxAmount, boolean simulate) {
        if (maxAmount <= 0) return 0;
        long tick = level == null ? Long.MIN_VALUE : level.getGameTime();
        if (tick != extractionTick) {
            extractionTick = tick;
            extractedThisTick = 0;
        }
        long remainingRate = Math.max(0, OUTPUT_RATE - extractedThisTick);
        long extracted = kinetic.extract(Math.min(maxAmount, remainingRate), simulate);
        if (!simulate && extracted > 0) {
            extractedThisTick += extracted;
            changedAndSync();
        }
        return extracted;
    }
    @Override public long outputRate() { return OUTPUT_RATE; }
    @Override public long stored() { return kinetic.stored(); }
    @Override public int strokeSign() { return kinetic.strokeSign(); }

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
