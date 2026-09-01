package com.masson.cruciblecraft.content.menu;

import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.registry.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Concrete menu used by every configured processing machine. */
public final class ConfiguredProcessingMachineMenu extends ProcessingMachineMenu {
    public static final int STATUS_DATA_INDEX = 0;
    public static final int STATUS_ARGUMENT_DATA_INDEX = 1;
    public static final int PROGRESS_PERMILLE_DATA_INDEX = 2;
    public static final int DATA_COUNT = 3;
    private final Level level;
    private final BlockPos machinePos;

    public static int dataCount() {
        return DATA_COUNT;
    }

    public ConfiguredProcessingMachineMenu(
            int id,
            Inventory playerInventory,
            ProcessingMachineSpec spec) {
        this(id, playerInventory, spec, BlockPos.ZERO);
    }

    public ConfiguredProcessingMachineMenu(
            int id,
            Inventory playerInventory,
            ProcessingMachineSpec spec,
            BlockPos machinePos) {
        this(ModMenus.forMachine(spec).get(), id, playerInventory, spec, machinePos);
    }

    public ConfiguredProcessingMachineMenu(
            MenuType<?> type,
            int id,
            Inventory playerInventory,
            ProcessingMachineSpec spec) {
        this(type, id, playerInventory, spec, BlockPos.ZERO);
    }

    public ConfiguredProcessingMachineMenu(
            MenuType<?> type,
            int id,
            Inventory playerInventory,
            ProcessingMachineSpec spec,
            BlockPos machinePos) {
        super(type, id, playerInventory, new ItemStackHandler(spec.items().slotCount()),
                new SimpleContainerData(dataCount()), spec, player -> true);
        this.level = playerInventory.player.level();
        this.machinePos = machinePos.immutable();
    }

    public ConfiguredProcessingMachineMenu(
            MenuType<?> type,
            int id,
            Inventory playerInventory,
            ConfiguredProcessingMachineBlockEntity machine) {
        super(type, id, playerInventory, machine.inventory(), machine.data(),
                machine.spec(), machine::stillValid);
        this.level = playerInventory.player.level();
        this.machinePos = machine.getBlockPos().immutable();
    }

    @Override
    public FluidStack tankFluid(int tank) {
        ConfiguredProcessingMachineBlockEntity machine = machine();
        if (tank < 0 || tank >= machineSpec().fluids().tankCount()
                || machine == null) {
            return FluidStack.EMPTY;
        }
        return machine.tanks().get(tank).getFluid().copy();
    }

    @Override
    public int tankAmount(int tank) {
        return tankFluid(tank).getAmount();
    }

    @Override
    public int progress() {
        ConfiguredProcessingMachineBlockEntity machine = machine();
        return machine == null ? 0 : machine.progress();
    }

    @Override
    public int duration() {
        ConfiguredProcessingMachineBlockEntity machine = machine();
        return machine == null ? 0 : machine.duration();
    }

    @Override
    protected int statusDataIndex() {
        return STATUS_DATA_INDEX;
    }

    @Override
    protected int statusArgumentDataIndex() {
        return STATUS_ARGUMENT_DATA_INDEX;
    }

    @Override
    protected int progressPermilleDataIndex() {
        return PROGRESS_PERMILLE_DATA_INDEX;
    }

    private ConfiguredProcessingMachineBlockEntity machine() {
        return level.getBlockEntity(machinePos)
                        instanceof ConfiguredProcessingMachineBlockEntity machine
                ? machine
                : null;
    }
}
