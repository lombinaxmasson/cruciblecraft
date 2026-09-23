package com.masson.cruciblecraft.content.block;

import java.util.Optional;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.blockentity.LargeSluiceBlockEntity;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.mte.MteInPlaceSpec;
import com.masson.cruciblecraft.content.multiblock.MultiblockPortHost;
import com.masson.cruciblecraft.content.multiblock.PortHostViews;
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

/** GT6 18106 Sluice Parts, including the far-side input port. */
public final class SluiceParts {
    public static final ResourceLocation PART_ID =
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, "multiblock/sluice_part");
    public static final int META = 18106;

    private SluiceParts() {}

    public static boolean isPart(MteInPlaceSpec spec) {
        return spec != null && spec.meta() == META;
    }

    public static boolean accepts(MteInPlaceSpec spec, PortType type) {
        return isPart(spec) && type == PortType.ITEM_FLUID_IN;
    }

    public static Block part() {
        return ModBlocks.mteInPlaceBlocksById().get(PART_ID).get();
    }

    public static IItemHandler items(MteInPlaceBlockEntity part) {
        return part.mixerPortType() == PortType.ITEM_FLUID_IN
                ? new PartItems(part)
                : null;
    }

    public static IFluidHandler fluids(MteInPlaceBlockEntity part) {
        return part.mixerPortType() == PortType.ITEM_FLUID_IN
                ? new PartFluids(part)
                : null;
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
                instanceof LargeSluiceBlockEntity sluice)
                || !sluice.structureValid()
                || !structure.get().equals(sluice.structureId())) {
            return null;
        }
        return PortHostViews.forPort(sluice.portHost(), part);
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
            MultiblockPortHost host = host(part);
            return host != null && host.itemInputSlots().contains(slot)
                    ? host.inventory().insertItem(slot, stack, simulate)
                    : stack;
        }

        @Override
        public ItemStack extractItem(
                int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            MultiblockPortHost host = host(part);
            return host == null ? 0 : host.inventory().getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
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
            MultiblockPortHost host = host(part);
            return host != null
                    && host.fluidInputTanks().contains(tank)
                    && host.tanks().get(tank).isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
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
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }
}
