package com.masson.cruciblecraft.content.block;

import java.util.Optional;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.mte.MteInPlaceSpec;
import com.masson.cruciblecraft.content.multiblock.MultiblockControllerBinding;
import com.masson.cruciblecraft.content.multiblock.MultiblockPortHost;
import com.masson.cruciblecraft.content.multiblock.PortHostViews;
import com.masson.cruciblecraft.content.multiblock.PortCapabilityGate;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PortType;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.machine.processing.SidedFluidHandler;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * GT6 18105 Electrolyzer Parts for Large Electrolyzer 17103. One live part
 * binds as {@code ONLY_ITEM_FLUID_ENERGY_IN} (bottom) or
 * {@code ONLY_ITEM_FLUID_OUT} (top).
 */
public final class ElectrolyzerParts {
    public static final ResourceLocation PART_ID =
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, "multiblock/electrolyzer_part");
    public static final int META = 18105;

    private ElectrolyzerParts() {}

    public static boolean isPart(MteInPlaceSpec spec) {
        return spec != null && spec.meta() == META;
    }

    /**
     * GT6 {@code checkStructure2} design index. The bottom layer stays 1.
     * An idle top stays 0. An active top is {@code 2 + rng(6)} and keeps
     * that face until the active or running state changes.
     */
    public static int structureDesign(
            int localY,
            boolean active,
            int current,
            boolean reroll,
            int rng) {
        if (localY <= 0) {
            return 1;
        }
        if (!active) {
            return 0;
        }
        if (!reroll && current >= 2 && current <= 7) {
            return current;
        }
        return 2 + Math.floorMod(rng, 6);
    }

    public static boolean accepts(MteInPlaceSpec spec, PortType type) {
        if (!isPart(spec) || type == null) {
            return false;
        }
        return type == PortType.ITEM_FLUID_ENERGY_IN
                || type == PortType.ITEM_FLUID_OUT;
    }

    public static Block part() {
        return ModBlocks.mteInPlaceBlocksById().get(PART_ID).get();
    }

    public static IItemHandler items(MteInPlaceBlockEntity part) {
        PortType type = part.mixerPortType();
        return PortCapabilityGate.itemCapable(type)
                ? new PartItems(part)
                : null;
    }

    public static IFluidHandler fluids(MteInPlaceBlockEntity part) {
        PortType type = part.mixerPortType();
        return PortCapabilityGate.fluidCapable(type)
                ? new PartFluids(part)
                : null;
    }

    public static boolean forwardsEnergy(MteInPlaceBlockEntity part) {
        return PortCapabilityGate.energyInsert(part.mixerPortType())
                && host(part) != null;
    }

    public static long insertEnergy(
            MteInPlaceBlockEntity part,
            EnergyType type,
            long size,
            long amount,
            boolean simulate) {
        if (!forwardsEnergy(part) || type != EnergyType.ELECTRIC) {
            return 0L;
        }
        MultiblockPortHost host = host(part);
        return host == null
                ? 0L
                : host.insertFromMultiblockPort(type, size, amount, simulate);
    }

    public static long storedEnergy(
            MteInPlaceBlockEntity part, EnergyType type) {
        MultiblockPortHost host = host(part);
        return host == null ? 0L : host.stored(type);
    }

    public static long energyCapacity(
            MteInPlaceBlockEntity part, EnergyType type) {
        MultiblockPortHost host = host(part);
        return host == null ? 0L : host.capacity(type);
    }

    static MultiblockPortHost host(MteInPlaceBlockEntity part) {
        Optional<BlockPos> controller = part.mixerControllerPosition();
        Optional<ResourceLocation> structure = part.mixerStructureId();
        Level level = part.getLevel();
        if (level == null
                || controller.isEmpty()
                || structure.isEmpty()
                || !level.hasChunkAt(controller.get())) {
            return null;
        }
        if (!(level.getBlockEntity(controller.get())
                instanceof MultiblockControllerBinding binding)
                || !binding.structureValid()
                || !structure.get().equals(binding.structureId())) {
            return null;
        }
        return PortHostViews.forPort(binding.portHost(), part);
    }

    private static final class PartItems implements IItemHandler {
        private final MteInPlaceBlockEntity part;

        private PartItems(MteInPlaceBlockEntity part) {
            this.part = part;
        }

        @Override
        public int getSlots() {
            MultiblockPortHost host = host(part);
            return host == null ? 0 : host.inventory().getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            MultiblockPortHost host = host(part);
            return host == null
                    ? ItemStack.EMPTY
                    : host.inventory().getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(
                int slot, ItemStack stack, boolean simulate) {
            if (!PortCapabilityGate.itemInsert(part.mixerPortType())) {
                return stack;
            }
            MultiblockPortHost host = host(part);
            return host != null && host.itemInputSlots().contains(slot)
                    ? host.inventory().insertItem(slot, stack, simulate)
                    : stack;
        }

        @Override
        public ItemStack extractItem(
                int slot, int amount, boolean simulate) {
            if (!PortCapabilityGate.itemExtract(part.mixerPortType())) {
                return ItemStack.EMPTY;
            }
            MultiblockPortHost host = host(part);
            return host != null && host.itemOutputSlots().contains(slot)
                    ? host.inventory().extractItem(slot, amount, simulate)
                    : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            MultiblockPortHost host = host(part);
            return host == null ? 0 : host.inventory().getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (!PortCapabilityGate.itemInsert(part.mixerPortType())) {
                return false;
            }
            MultiblockPortHost host = host(part);
            return host != null
                    && host.itemInputSlots().contains(slot)
                    && host.inventory().isItemValid(slot, stack);
        }
    }

    private static final class PartFluids implements IFluidHandler {
        private final MteInPlaceBlockEntity part;

        private PartFluids(MteInPlaceBlockEntity part) {
            this.part = part;
        }

        @Override
        public int getTanks() {
            MultiblockPortHost host = host(part);
            return host == null ? 0 : host.tanks().size();
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            MultiblockPortHost host = host(part);
            return host == null
                    ? FluidStack.EMPTY
                    : host.tanks().get(tank).getFluid().copy();
        }

        @Override
        public int getTankCapacity(int tank) {
            MultiblockPortHost host = host(part);
            return host == null ? 0 : host.tanks().get(tank).getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (!PortCapabilityGate.fluidFill(part.mixerPortType())) {
                return false;
            }
            MultiblockPortHost host = host(part);
            return host != null
                    && host.fluidInputTanks().contains(tank)
                    && host.tanks().get(tank).isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!PortCapabilityGate.fluidFill(part.mixerPortType())) {
                return 0;
            }
            MultiblockPortHost host = host(part);
            return host == null
                    ? 0
                    : new SidedFluidHandler(
                            host.tanks(),
                            host.fluidInputTanks(),
                            ProcessingMachineSpec.CapabilityAccess.INPUT)
                            .fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (!PortCapabilityGate.fluidDrain(part.mixerPortType())) {
                return FluidStack.EMPTY;
            }
            MultiblockPortHost host = host(part);
            return host == null
                    ? FluidStack.EMPTY
                    : new SidedFluidHandler(
                            host.tanks(),
                            host.fluidOutputTanks(),
                            ProcessingMachineSpec.CapabilityAccess.OUTPUT)
                            .drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (part.mixerPortType() != PortType.ITEM_FLUID_OUT) {
                return FluidStack.EMPTY;
            }
            MultiblockPortHost host = host(part);
            return host == null
                    ? FluidStack.EMPTY
                    : new SidedFluidHandler(
                            host.tanks(),
                            host.fluidOutputTanks(),
                            ProcessingMachineSpec.CapabilityAccess.OUTPUT)
                            .drain(maxDrain, action);
        }
    }
}
