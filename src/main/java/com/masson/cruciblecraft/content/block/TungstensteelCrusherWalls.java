package com.masson.cruciblecraft.content.block;

import java.util.Optional;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.mte.MteInPlaceSpec;
import com.masson.cruciblecraft.content.multiblock.MultiblockControllerBinding;
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

/**
 * GT6 18003 tungstensteel walls for Large Crusher 17108. Bottom binds as
 * {@code ONLY_ITEM_FLUID_OUT}; the two side-middle energy faces bind as
 * {@code ONLY_ENERGY_IN}. Remaining walls are structure-only.
 */
public final class TungstensteelCrusherWalls {
    public static final ResourceLocation WALL_ID =
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, "tungstensteel/wall");
    public static final int META = 18003;

    private TungstensteelCrusherWalls() {}

    public static boolean isWall(MteInPlaceSpec spec) {
        return spec != null && spec.meta() == META;
    }

    public static boolean accepts(MteInPlaceSpec spec, PortType type) {
        if (!isWall(spec) || type == null) {
            return false;
        }
        return type == PortType.ITEM_FLUID_OUT
                || type == PortType.ENERGY_INPUT;
    }

    public static Block wall() {
        return ModBlocks.mteInPlaceBlocksById().get(WALL_ID).get();
    }

    public static IItemHandler items(MteInPlaceBlockEntity wall) {
        return wall.mixerPortType() == PortType.ITEM_FLUID_OUT
                ? new WallItems(wall)
                : null;
    }

    public static IFluidHandler fluids(MteInPlaceBlockEntity wall) {
        return wall.mixerPortType() == PortType.ITEM_FLUID_OUT
                ? new WallFluids(wall)
                : null;
    }

    public static boolean forwardsEnergy(MteInPlaceBlockEntity wall) {
        return wall.mixerPortType() == PortType.ENERGY_INPUT
                && host(wall) != null;
    }

    public static long insertEnergy(
            MteInPlaceBlockEntity wall,
            EnergyType type,
            long size,
            long amount,
            boolean simulate) {
        if (!forwardsEnergy(wall) || type != EnergyType.KINETIC_ROTATION) {
            return 0L;
        }
        MultiblockPortHost host = host(wall);
        return host == null
                ? 0L
                : host.insertFromMultiblockPort(type, size, amount, simulate);
    }

    public static long storedEnergy(
            MteInPlaceBlockEntity wall, EnergyType type) {
        MultiblockPortHost host = host(wall);
        return host == null ? 0L : host.stored(type);
    }

    public static long energyCapacity(
            MteInPlaceBlockEntity wall, EnergyType type) {
        MultiblockPortHost host = host(wall);
        return host == null ? 0L : host.capacity(type);
    }

    static MultiblockPortHost host(MteInPlaceBlockEntity wall) {
        Optional<BlockPos> controller = wall.mixerControllerPosition();
        Optional<ResourceLocation> structure = wall.mixerStructureId();
        Level level = wall.getLevel();
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
        return PortHostViews.forPort(binding.portHost(), wall);
    }

    private static final class WallItems implements IItemHandler {
        private final MteInPlaceBlockEntity wall;

        private WallItems(MteInPlaceBlockEntity wall) {
            this.wall = wall;
        }

        @Override
        public int getSlots() {
            MultiblockPortHost host = host(wall);
            return host == null ? 0 : host.inventory().getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            MultiblockPortHost host = host(wall);
            return host == null
                    ? ItemStack.EMPTY
                    : host.inventory().getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(
                int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(
                int slot, int amount, boolean simulate) {
            MultiblockPortHost host = host(wall);
            return host != null && host.itemOutputSlots().contains(slot)
                    ? host.inventory().extractItem(slot, amount, simulate)
                    : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            MultiblockPortHost host = host(wall);
            return host == null ? 0 : host.inventory().getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    }

    private static final class WallFluids implements IFluidHandler {
        private final MteInPlaceBlockEntity wall;

        private WallFluids(MteInPlaceBlockEntity wall) {
            this.wall = wall;
        }

        @Override
        public int getTanks() {
            MultiblockPortHost host = host(wall);
            return host == null ? 0 : host.tanks().size();
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            MultiblockPortHost host = host(wall);
            return host == null
                    ? FluidStack.EMPTY
                    : host.tanks().get(tank).getFluid().copy();
        }

        @Override
        public int getTankCapacity(int tank) {
            MultiblockPortHost host = host(wall);
            return host == null ? 0 : host.tanks().get(tank).getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            MultiblockPortHost host = host(wall);
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
            MultiblockPortHost host = host(wall);
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
