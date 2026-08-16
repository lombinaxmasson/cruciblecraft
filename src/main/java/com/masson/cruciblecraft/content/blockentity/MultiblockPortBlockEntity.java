package com.masson.cruciblecraft.content.blockentity;

import java.util.List;
import java.util.Optional;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.block.MultiblockPortBlock;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.multiblock.MultiblockControllerBinding;
import com.masson.cruciblecraft.content.multiblock.MultiblockPort;
import com.masson.cruciblecraft.content.multiblock.MultiblockPortHost;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PortType;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.machine.processing.SidedFluidHandler;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Generic capability bridge. It stores only a controller binding; all resource
 * state remains transaction-owned by the shared processing host.
 */
public final class MultiblockPortBlockEntity extends BlockEntity
        implements MultiblockPort, IEnergyHandler {
    private BlockPos controller;
    private ResourceLocation structure;
    private final IItemHandler items = new PortItems();
    private final IFluidHandler fluids = new PortFluids();

    public MultiblockPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MULTIBLOCK_PORT.get(), pos, state);
    }

    @Override
    public PortType portType() {
        if (!(getBlockState().getBlock() instanceof MultiblockPortBlock port)) {
            throw new IllegalStateException(
                    "Multiblock port block entity has a non-port block");
        }
        return port.portType();
    }

    @Override
    public void bind(BlockPos controller, ResourceLocation structureId) {
        BlockPos immutable = controller.immutable();
        if (!immutable.equals(this.controller)
                || !structureId.equals(structure)) {
            this.controller = immutable;
            this.structure = structureId;
            setChanged();
        }
    }

    @Override
    public void unbind(BlockPos controller) {
        if (controller.equals(this.controller)) {
            this.controller = null;
            this.structure = null;
            setChanged();
        }
    }

    @Override
    public Optional<BlockPos> controllerPosition() {
        return Optional.ofNullable(controller);
    }

    @Override
    public Optional<ResourceLocation> structureId() {
        return Optional.ofNullable(structure);
    }

    public IItemHandler itemHandler() {
        return portType() == PortType.ITEM_FLUID ? items : null;
    }

    public IFluidHandler fluidHandler() {
        return portType() == PortType.ITEM_FLUID ? fluids : null;
    }

    private MultiblockControllerBinding binding() {
        if (level == null || controller == null || structure == null
                || !level.hasChunkAt(controller)) {
            return null;
        }
        if (!(level.getBlockEntity(controller)
                instanceof MultiblockControllerBinding binding)
                || !binding.structureValid()
                || !structure.equals(binding.structureId())) {
            return null;
        }
        return binding;
    }

    private MultiblockPortHost host() {
        MultiblockControllerBinding binding = binding();
        return binding == null ? null : binding.portHost();
    }

    private Direction hostInputSide(MultiblockPortHost host) {
        return host.blockState().hasProperty(ProcessingMachineBlock.FACING)
                ? host.blockState()
                        .getValue(ProcessingMachineBlock.FACING)
                        .getOpposite()
                : Direction.SOUTH;
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        MultiblockPortHost host =
                portType() == PortType.ENERGY_INPUT ? host() : null;
        return host != null && host.handles(type, hostInputSide(host));
    }

    @Override
    public long insert(
            EnergyType type,
            long size,
            long amount,
            Direction side,
            boolean simulate) {
        MultiblockPortHost host =
                portType() == PortType.ENERGY_INPUT ? host() : null;
        return host == null
                ? 0L
                : host.insert(
                        type,
                        size,
                        amount,
                        hostInputSide(host),
                        simulate);
    }

    @Override
    public long stored(EnergyType type) {
        MultiblockPortHost host = host();
        return host == null ? 0L : host.stored(type);
    }

    @Override
    public long capacity(EnergyType type) {
        MultiblockPortHost host = host();
        return host == null ? 0L : host.capacity(type);
    }

    @Override
    public void invalidateSimulationCache() {
        MultiblockPortHost host = host();
        if (host != null) {
            host.invalidateSimulationCache();
        }
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (controller != null && structure != null) {
            tag.putLong("controller", controller.asLong());
            tag.putString("structure", structure.toString());
        }
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ResourceLocation parsed = tag.contains("structure")
                ? ResourceLocation.tryParse(tag.getString("structure"))
                : null;
        if (tag.contains("controller") && parsed != null) {
            controller = BlockPos.of(tag.getLong("controller"));
            structure = parsed;
        } else {
            controller = null;
            structure = null;
        }
    }

    private final class PortItems implements IItemHandler {
        @Override
        public int getSlots() {
            MultiblockPortHost host = host();
            return host == null ? 0 : host.inventory().getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            MultiblockPortHost host = host();
            return host == null
                    ? ItemStack.EMPTY
                    : host.inventory().getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(
                int slot, ItemStack stack, boolean simulate) {
            MultiblockPortHost host = host();
            return host != null && host.itemInputSlots().contains(slot)
                    ? host.inventory().insertItem(slot, stack, simulate)
                    : stack;
        }

        @Override
        public ItemStack extractItem(
                int slot, int amount, boolean simulate) {
            MultiblockPortHost host = host();
            return host != null && host.itemOutputSlots().contains(slot)
                    ? host.inventory().extractItem(slot, amount, simulate)
                    : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            MultiblockPortHost host = host();
            return host == null ? 0 : host.inventory().getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            MultiblockPortHost host = host();
            return host != null
                    && host.itemInputSlots().contains(slot)
                    && host.inventory().isItemValid(slot, stack);
        }
    }

    private final class PortFluids implements IFluidHandler {
        @Override
        public int getTanks() {
            MultiblockPortHost host = host();
            return host == null ? 0 : host.tanks().size();
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            MultiblockPortHost host = host();
            return host == null
                    ? FluidStack.EMPTY
                    : host.tanks().get(tank).getFluid().copy();
        }

        @Override
        public int getTankCapacity(int tank) {
            MultiblockPortHost host = host();
            return host == null ? 0 : host.tanks().get(tank).getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            MultiblockPortHost host = host();
            return host != null
                    && host.fluidInputTanks().contains(tank)
                    && host.tanks().get(tank).isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            MultiblockPortHost host = host();
            return host == null
                    ? 0
                    : inputView(host).fill(resource, action);
        }

        @Override
        public FluidStack drain(
                FluidStack resource, FluidAction action) {
            MultiblockPortHost host = host();
            return host == null
                    ? FluidStack.EMPTY
                    : outputView(host).drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            MultiblockPortHost host = host();
            return host == null
                    ? FluidStack.EMPTY
                    : outputView(host).drain(maxDrain, action);
        }

        private IFluidHandler inputView(MultiblockPortHost host) {
            return view(
                    host,
                    host.fluidInputTanks(),
                    ProcessingMachineSpec.CapabilityAccess.INPUT);
        }

        private IFluidHandler outputView(MultiblockPortHost host) {
            return view(
                    host,
                    host.fluidOutputTanks(),
                    ProcessingMachineSpec.CapabilityAccess.OUTPUT);
        }

        private IFluidHandler view(
                MultiblockPortHost host,
                List<Integer> tankIndexes,
                ProcessingMachineSpec.CapabilityAccess access) {
            return new SidedFluidHandler(
                    host.tanks(),
                    tankIndexes,
                    access);
        }
    }
}
