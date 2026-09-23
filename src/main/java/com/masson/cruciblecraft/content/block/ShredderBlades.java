package com.masson.cruciblecraft.content.block;

import java.util.Optional;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.blockentity.LargeShredderBlockEntity;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.mte.MteInPlaceSpec;
import com.masson.cruciblecraft.content.multiblock.MultiblockPortHost;
import com.masson.cruciblecraft.content.multiblock.PortHostViews;
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
 * GT6 18108 Shredder Blades for Large Shredder 17109. The top layer binds as
 * {@code ONLY_ITEM_FLUID_IN}; the middle filling is structure-only.
 */
public final class ShredderBlades {
    public static final ResourceLocation PART_ID =
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, "multiblock/shredder_blades");
    public static final int META = 18108;

    private ShredderBlades() {}

    public static boolean isPart(MteInPlaceSpec spec) {
        return spec != null && spec.meta() == META;
    }

    public static boolean accepts(MteInPlaceSpec spec, PortType type) {
        return isPart(spec) && type == PortType.ITEM_FLUID_IN;
    }

    public static Block part() {
        return ModBlocks.mteInPlaceBlocksById().get(PART_ID).get();
    }

    public static IItemHandler items(MteInPlaceBlockEntity blade) {
        return blade.mixerPortType() == PortType.ITEM_FLUID_IN
                ? new BladeItems(blade)
                : null;
    }

    public static IFluidHandler fluids(MteInPlaceBlockEntity blade) {
        return blade.mixerPortType() == PortType.ITEM_FLUID_IN
                ? new BladeFluids(blade)
                : null;
    }

    /** GT6 {@code onWalkOver2}: 5 damage while the formed shredder is running. */
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
                instanceof LargeShredderBlockEntity shredder)
                || !shredder.structureValid()
                || !shredder.runningActively()
                || part.mixerStructureId().isEmpty()
                || !part.mixerStructureId().orElseThrow()
                        .equals(shredder.structureId())) {
            return;
        }
        Direction facing = level.getBlockState(controller.get())
                .getValue(ProcessingMachineBlock.FACING);
        if (!LargeShredderGeometry.insideBladeWalk(
                controller.get(), facing, living.position())) {
            return;
        }
        ShredderDamage.apply(level, living);
    }

    static MultiblockPortHost host(MteInPlaceBlockEntity blade) {
        Optional<BlockPos> controller = blade.mixerControllerPosition();
        Optional<ResourceLocation> structure = blade.mixerStructureId();
        Level level = blade.getLevel();
        if (level == null
                || controller.isEmpty()
                || structure.isEmpty()
                || !level.hasChunkAt(controller.get())) {
            return null;
        }
        if (!(level.getBlockEntity(controller.get())
                instanceof LargeShredderBlockEntity shredder)
                || !shredder.structureValid()
                || !structure.get().equals(shredder.structureId())) {
            return null;
        }
        return PortHostViews.forPort(shredder.portHost(), blade);
    }

    private static final class BladeItems implements IItemHandler {
        private final MteInPlaceBlockEntity blade;

        private BladeItems(MteInPlaceBlockEntity blade) {
            this.blade = blade;
        }

        @Override
        public int getSlots() {
            MultiblockPortHost host = host(blade);
            return host == null ? 0 : host.inventory().getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            MultiblockPortHost host = host(blade);
            return host == null
                    ? ItemStack.EMPTY
                    : host.inventory().getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(
                int slot, ItemStack stack, boolean simulate) {
            MultiblockPortHost host = host(blade);
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
            MultiblockPortHost host = host(blade);
            return host == null ? 0 : host.inventory().getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            MultiblockPortHost host = host(blade);
            return host != null
                    && host.itemInputSlots().contains(slot)
                    && host.inventory().isItemValid(slot, stack);
        }
    }

    private static final class BladeFluids implements IFluidHandler {
        private final MteInPlaceBlockEntity blade;

        private BladeFluids(MteInPlaceBlockEntity blade) {
            this.blade = blade;
        }

        @Override
        public int getTanks() {
            MultiblockPortHost host = host(blade);
            return host == null ? 0 : host.tanks().size();
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            MultiblockPortHost host = host(blade);
            return host == null
                    ? FluidStack.EMPTY
                    : host.tanks().get(tank).getFluid().copy();
        }

        @Override
        public int getTankCapacity(int tank) {
            MultiblockPortHost host = host(blade);
            return host == null ? 0 : host.tanks().get(tank).getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            MultiblockPortHost host = host(blade);
            return host != null
                    && host.fluidInputTanks().contains(tank)
                    && host.tanks().get(tank).isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            MultiblockPortHost host = host(blade);
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
