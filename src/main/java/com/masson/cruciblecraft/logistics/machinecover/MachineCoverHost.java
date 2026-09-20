package com.masson.cruciblecraft.logistics.machinecover;

import com.masson.cruciblecraft.logistics.pipe.cover.CoverDefinition;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCover;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCoverSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** Processing-machine face that can hold remainder covers. */
public interface MachineCoverHost {
    PipeCoverSet covers();

    boolean setCover(Direction side, PipeCover cover);

    void replaceCover(Direction side, PipeCover cover);

    boolean configureCover(
            Direction side,
            CoverDefinition.ConfigField field,
            int value);

    boolean coverEnabled();

    void setCoverEnabled(boolean enabled);

    boolean coversStopped();

    void setCoversStopped(boolean stopped);

    boolean canTick();

    boolean runningPossible();

    boolean runningPassively();

    boolean runningActively();

    boolean runningSuccessfully();

    int selectorMode();

    void setSelectorMode(int mode);

    boolean hasEnergyBuffer();

    long energyStored();

    long energyCapacity();

    int progress();

    int duration();

    boolean hasFluidTanks();

    int fillAir(int amount);

    default int fillFluid(
            net.neoforged.neoforge.fluids.FluidStack stack,
            net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction action) {
        return 0;
    }

    int incomingRedstone(Direction side);

    void notifyRedstone();

    boolean removeCover(Direction side, net.minecraft.world.entity.player.Player player);

    void dropCovers();

    default boolean allowCover(Direction side) {
        return true;
    }

    default boolean switchableOnOff() {
        return false;
    }

    default boolean getStateOnOff() {
        return true;
    }

    default boolean setStateOnOff(boolean on) {
        return getStateOnOff();
    }

    long gameTime();

    Level level();

    BlockPos hostPos();
}
