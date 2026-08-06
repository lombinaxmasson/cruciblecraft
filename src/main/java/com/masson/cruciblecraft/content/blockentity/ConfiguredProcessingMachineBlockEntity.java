package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.menu.ConfiguredProcessingMachineMenu;
import com.masson.cruciblecraft.machine.processing.MachineVariant;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineEnergyPlacement;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineFluidPolicy;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineDisplayData;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModCapabilities;
import com.masson.cruciblecraft.registry.ModMenus;
import com.masson.cruciblecraft.registry.ModProcessingMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

/** One production host configured by the block's immutable machine spec. */
public class ConfiguredProcessingMachineBlockEntity
        extends ProcessingMachineBlockEntity implements MenuProvider {
    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case ConfiguredProcessingMachineMenu.STATUS_DATA_INDEX ->
                        ProcessingMachineDisplayData.statusIndex(spec(), pausedReason());
                case ConfiguredProcessingMachineMenu.STATUS_ARGUMENT_DATA_INDEX ->
                        statusArgument();
                default -> 0;
            };
        }
        @Override public void set(int index, int value) {}
        @Override public int getCount() {
            return ConfiguredProcessingMachineMenu.dataCount();
        }
    };

    public ConfiguredProcessingMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PROCESSING_MACHINE.get(), pos, state, variantFor(state));
    }

    protected ConfiguredProcessingMachineBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state,
            MachineVariant variant) {
        super(type, pos, state, variant);
    }

    private static MachineVariant variantFor(BlockState state) {
        if (!(state.getBlock() instanceof ProcessingMachineBlock machine)) {
            throw new IllegalArgumentException("Configured machine block entity needs its block spec");
        }
        return machine.variant();
    }

    public static void serverTick(
            Level level, BlockPos pos, BlockState state,
            ConfiguredProcessingMachineBlockEntity machine) {
        machine.tickProcessingServer();
    }

    public ContainerData data() { return data; }

    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getCenter()) <= 64.0;
    }

    public void dropContents() {
        if (level == null || level.isClientSide) return;
        for (int slot = 0; slot < inventory().getSlots(); slot++) {
            ItemStack stack = inventory().getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(),
                        worldPosition.getZ(), stack);
                inventory().setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    @Override public Component getDisplayName() {
        return Component.translatable("block.cruciblecraft." + spec().id().getPath());
    }

    @Override public AbstractContainerMenu createMenu(
            int id, Inventory inventory, Player player) {
        return new ConfiguredProcessingMachineMenu(
                ModMenus.forMachine(variant().kind().behavior()).get(),
                id,
                inventory,
                this);
    }

    @Override protected Direction machineFront() {
        return getBlockState().hasProperty(ProcessingMachineBlock.FACING)
                ? getBlockState().getValue(ProcessingMachineBlock.FACING) : Direction.NORTH;
    }

    @Override protected boolean isFluidInputValid(int tank, FluidStack stack) {
        return ProcessingMachineFluidPolicy.accepts(spec(), tank, stack);
    }

    @Override protected IEnergyHandler adjacentEnergySource() {
        if (level == null) return null;
        var connection = ProcessingMachineEnergyPlacement.connection(spec(), machineFront());
        return level.getCapability(
                ModCapabilities.ENERGY,
                worldPosition.relative(connection.providerOffset()),
                connection.providerFace());
    }

    @Override protected Direction adjacentEnergySourceSide() {
        return ProcessingMachineEnergyPlacement.connection(spec(), machineFront()).providerFace();
    }
}
