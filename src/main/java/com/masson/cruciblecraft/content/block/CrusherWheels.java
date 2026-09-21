package com.masson.cruciblecraft.content.block;

import java.util.Optional;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.blockentity.LargeCrusherBlockEntity;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.mte.MteInPlaceSpec;
import com.masson.cruciblecraft.content.multiblock.MultiblockPortHost;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PortType;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.machine.processing.SidedFluidHandler;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * GT6 18107 Crusher Wheels for Large Crusher 17108. The top layer binds as
 * {@code ONLY_ITEM_FLUID_IN}; the middle filling is structure-only.
 */
public final class CrusherWheels {
    public static final ResourceLocation PART_ID =
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, "multiblock/crusher_wheels");
    public static final int META = 18107;

    private CrusherWheels() {}

    public static boolean isPart(MteInPlaceSpec spec) {
        return spec != null && spec.meta() == META;
    }

    public static boolean accepts(MteInPlaceSpec spec, PortType type) {
        return isPart(spec) && type == PortType.ITEM_FLUID_IN;
    }

    public static Block part() {
        return ModBlocks.mteInPlaceBlocksById().get(PART_ID).get();
    }

    public static IItemHandler items(MteInPlaceBlockEntity wheel) {
        return wheel.mixerPortType() == PortType.ITEM_FLUID_IN
                ? new WheelItems(wheel)
                : null;
    }

    public static IFluidHandler fluids(MteInPlaceBlockEntity wheel) {
        return wheel.mixerPortType() == PortType.ITEM_FLUID_IN
                ? new WheelFluids(wheel)
                : null;
    }

    /** GT6 {@code onWalkOver2}: 5 damage while the formed crusher is running. */
    public static void hurtIfRunning(
            Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof LivingEntity living)) {
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof MteInPlaceBlockEntity part)
                || (!isPart(part.spec())
                        && !TungstensteelCrusherWalls.isWall(part.spec()))) {
            return;
        }
        Optional<BlockPos> controller = part.mixerControllerPosition();
        if (controller.isEmpty() || !level.hasChunkAt(controller.get())) {
            return;
        }
        if (!(level.getBlockEntity(controller.get())
                instanceof LargeCrusherBlockEntity crusher)
                || !crusher.structureValid()
                || !crusher.runningActively()
                || part.mixerStructureId().isEmpty()
                || !part.mixerStructureId().orElseThrow()
                        .equals(crusher.structureId())) {
            return;
        }
        Direction facing = level.getBlockState(controller.get())
                .getValue(ProcessingMachineBlock.FACING);
        if (!LargeCrusherGeometry.insideWheelWalk(
                controller.get(), facing, living.position())) {
            return;
        }
        CrusherDamage.apply(level, living);
    }

    static MultiblockPortHost host(MteInPlaceBlockEntity wheel) {
        Optional<BlockPos> controller = wheel.mixerControllerPosition();
        Optional<ResourceLocation> structure = wheel.mixerStructureId();
        Level level = wheel.getLevel();
        if (level == null
                || controller.isEmpty()
                || structure.isEmpty()
                || !level.hasChunkAt(controller.get())) {
            return null;
        }
        if (!(level.getBlockEntity(controller.get())
                instanceof LargeCrusherBlockEntity crusher)
                || !crusher.structureValid()
                || !structure.get().equals(crusher.structureId())) {
            return null;
        }
        return crusher.portHost();
    }

    private static final class WheelItems implements IItemHandler {
        private final MteInPlaceBlockEntity wheel;

        private WheelItems(MteInPlaceBlockEntity wheel) {
            this.wheel = wheel;
        }

        @Override
        public int getSlots() {
            MultiblockPortHost host = host(wheel);
            return host == null ? 0 : host.inventory().getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            MultiblockPortHost host = host(wheel);
            return host == null
                    ? ItemStack.EMPTY
                    : host.inventory().getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(
                int slot, ItemStack stack, boolean simulate) {
            MultiblockPortHost host = host(wheel);
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
            MultiblockPortHost host = host(wheel);
            return host == null ? 0 : host.inventory().getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            MultiblockPortHost host = host(wheel);
            return host != null
                    && host.itemInputSlots().contains(slot)
                    && host.inventory().isItemValid(slot, stack);
        }
    }

    private static final class WheelFluids implements IFluidHandler {
        private final MteInPlaceBlockEntity wheel;

        private WheelFluids(MteInPlaceBlockEntity wheel) {
            this.wheel = wheel;
        }

        @Override
        public int getTanks() {
            MultiblockPortHost host = host(wheel);
            return host == null ? 0 : host.tanks().size();
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            MultiblockPortHost host = host(wheel);
            return host == null
                    ? FluidStack.EMPTY
                    : host.tanks().get(tank).getFluid().copy();
        }

        @Override
        public int getTankCapacity(int tank) {
            MultiblockPortHost host = host(wheel);
            return host == null ? 0 : host.tanks().get(tank).getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            MultiblockPortHost host = host(wheel);
            return host != null
                    && host.fluidInputTanks().contains(tank)
                    && host.tanks().get(tank).isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            MultiblockPortHost host = host(wheel);
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
