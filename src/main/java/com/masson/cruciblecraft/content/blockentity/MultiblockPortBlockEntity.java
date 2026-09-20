package com.masson.cruciblecraft.content.blockentity;

import java.util.List;
import java.util.Optional;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.block.DistillationTowerParts;
import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.block.MultiblockPortBlock;
import com.masson.cruciblecraft.content.multiblock.MultiblockControllerBinding;
import com.masson.cruciblecraft.content.multiblock.MultiblockPort;
import com.masson.cruciblecraft.content.multiblock.MultiblockPortHost;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PortType;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverBehaviors;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverHost;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverDefinition;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCover;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCoverItems;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCoverSet;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.machine.processing.SidedFluidHandler;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Generic capability bridge. It stores only a controller binding; all resource
 * state remains transaction-owned by the shared processing host. GT6 18102
 * covers live on the part; tools other than wrench/crowbar go to the
 * controller.
 */
public final class MultiblockPortBlockEntity extends BlockEntity
        implements MultiblockPort, IEnergyHandler, MachineCoverHost {
    private BlockPos controller;
    private ResourceLocation structure;
    private PortType assignedType;
    private final IItemHandler items = new PortItems();
    private final IFluidHandler fluids = new PortFluids();
    private final PipeCoverSet covers = new PipeCoverSet();
    private boolean coverEnabled = true;
    private boolean coversStopped;
    private int selectorMode;

    public MultiblockPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MULTIBLOCK_PORT.get(), pos, state);
    }

    @Override
    public PortType portType() {
        if (assignedType != null) {
            return assignedType;
        }
        if (getBlockState().getBlock() instanceof MultiblockPortBlock port) {
            return port.portType();
        }
        if (getBlockState().getBlock() instanceof MteInPlaceBlock inplace) {
            return DistillationTowerParts.defaultType(inplace.spec());
        }
        throw new IllegalStateException(
                "Multiblock port block entity has a non-port block");
    }

    @Override
    public boolean accepts(PortType type) {
        if (assignedType != null) {
            return assignedType == type;
        }
        if (getBlockState().getBlock() instanceof MultiblockPortBlock port) {
            return port.portType() == type;
        }
        if (getBlockState().getBlock() instanceof MteInPlaceBlock inplace) {
            return DistillationTowerParts.accepts(inplace.spec(), type);
        }
        return false;
    }

    @Override
    public void bind(BlockPos controller, ResourceLocation structureId) {
        bind(controller, structureId, portType());
    }

    @Override
    public void bind(
            BlockPos controller,
            ResourceLocation structureId,
            PortType type) {
        BlockPos immutable = controller.immutable();
        boolean changed = !immutable.equals(this.controller)
                || !structureId.equals(structure)
                || this.assignedType != type;
        this.controller = immutable;
        this.structure = structureId;
        this.assignedType = type;
        if (changed) {
            setChanged();
            invalidateCaps();
        }
    }

    @Override
    public void unbind(BlockPos controller) {
        if (controller.equals(this.controller)) {
            this.controller = null;
            this.structure = null;
            this.assignedType = null;
            setChanged();
            invalidateCaps();
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
        PortType type = portType();
        return type == PortType.ITEM_FLUID || type == PortType.FLUID_OUT
                ? fluids
                : null;
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

    private void invalidateCaps() {
        if (level != null && !level.isClientSide) {
            level.invalidateCapabilities(worldPosition);
        }
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        if (portType() != PortType.ENERGY_INPUT) {
            return false;
        }
        MultiblockPortHost host = host();
        return host != null && host.capacity(type) > 0L;
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
                : host.insertFromMultiblockPort(type, size, amount, simulate);
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
        if (assignedType != null) {
            tag.putString("port_type", assignedType.serializedName());
        }
        tag.putBoolean("machine_cover_enabled", coverEnabled);
        tag.putBoolean("machine_covers_stopped", coversStopped);
        tag.putInt("machine_selector_mode", selectorMode);
        CompoundTag coverTag = new CompoundTag();
        covers.save(coverTag, registries);
        tag.put("machine_covers", coverTag);
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
        assignedType = null;
        if (tag.contains("port_type")) {
            String name = tag.getString("port_type");
            for (PortType type : PortType.values()) {
                if (type.serializedName().equals(name)) {
                    assignedType = type;
                    break;
                }
            }
        }
        coverEnabled = !tag.contains("machine_cover_enabled")
                || tag.getBoolean("machine_cover_enabled");
        coversStopped = tag.getBoolean("machine_covers_stopped");
        selectorMode = Math.max(0, Math.min(15, tag.getInt("machine_selector_mode")));
        if (tag.contains("machine_covers")) {
            covers.load(tag.getCompound("machine_covers"), registries);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void onDataPacket(
            Connection connection,
            ClientboundBlockEntityDataPacket packet,
            HolderLookup.Provider registries) {
        if (packet.getTag() != null) {
            loadAdditional(packet.getTag(), registries);
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private ProcessingMachineBlockEntity processingHost() {
        MultiblockControllerBinding binding = binding();
        return binding == null ? null : binding.processingHost();
    }

    private void syncCoverUpdate() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(
                    worldPosition,
                    getBlockState(),
                    getBlockState(),
                    Block.UPDATE_CLIENTS);
            notifyRedstone();
        }
    }

    @Override
    public PipeCoverSet covers() {
        return covers;
    }

    @Override
    public boolean setCover(Direction side, PipeCover cover) {
        if (side == null
                || cover == null
                || !MachineCoverBehaviors.canPlace(this, side, cover)) {
            return false;
        }
        boolean changed = covers.set(side, cover);
        if (changed) {
            syncCoverUpdate();
        }
        return changed;
    }

    @Override
    public void replaceCover(Direction side, PipeCover cover) {
        if (side == null) {
            return;
        }
        if (covers.set(side, cover)) {
            syncCoverUpdate();
        }
    }

    @Override
    public boolean configureCover(
            Direction side,
            CoverDefinition.ConfigField field,
            int value) {
        if (side == null || field == null) {
            return false;
        }
        if (!covers.configure(side, field, value)) {
            return false;
        }
        syncCoverUpdate();
        return true;
    }

    @Override
    public boolean removeCover(
            Direction side,
            net.minecraft.world.entity.player.Player player) {
        if (side == null) {
            return false;
        }
        Optional<PipeCover> taken = covers.take(side);
        if (taken.isEmpty()) {
            return false;
        }
        ItemStack stack = PipeCoverItems.stackFor(taken.orElseThrow());
        if (!stack.isEmpty()) {
            if (player == null || !player.addItem(stack)) {
                if (level != null && !level.isClientSide) {
                    Block.popResource(level, worldPosition, stack);
                }
            }
        }
        syncCoverUpdate();
        return true;
    }

    @Override
    public void dropCovers() {
        if (level == null || level.isClientSide) {
            return;
        }
        for (ItemStack stack : covers.removeAllAsItems()) {
            Block.popResource(level, worldPosition, stack);
        }
        syncCoverUpdate();
    }

    @Override
    public boolean coverEnabled() {
        return coverEnabled;
    }

    @Override
    public void setCoverEnabled(boolean enabled) {
        if (coverEnabled != enabled) {
            coverEnabled = enabled;
            syncCoverUpdate();
        }
    }

    @Override
    public boolean coversStopped() {
        return coversStopped;
    }

    @Override
    public void setCoversStopped(boolean stopped) {
        if (coversStopped != stopped) {
            coversStopped = stopped;
            setChanged();
        }
    }

    @Override
    public boolean canTick() {
        return level != null && !level.isClientSide;
    }

    @Override
    public boolean runningPossible() {
        ProcessingMachineBlockEntity host = processingHost();
        return host != null && host.runningPossible();
    }

    @Override
    public boolean runningPassively() {
        ProcessingMachineBlockEntity host = processingHost();
        return host != null && host.runningPassively();
    }

    @Override
    public boolean runningActively() {
        ProcessingMachineBlockEntity host = processingHost();
        return host != null && host.runningActively();
    }

    @Override
    public boolean runningSuccessfully() {
        ProcessingMachineBlockEntity host = processingHost();
        return host != null && host.runningSuccessfully();
    }

    @Override
    public int selectorMode() {
        ProcessingMachineBlockEntity host = processingHost();
        return host == null ? selectorMode : host.selectorMode();
    }

    @Override
    public void setSelectorMode(int mode) {
        ProcessingMachineBlockEntity host = processingHost();
        if (host != null) {
            host.setSelectorMode(mode);
            return;
        }
        int bounded = Math.max(0, Math.min(15, mode));
        if (selectorMode != bounded) {
            selectorMode = bounded;
            setChanged();
        }
    }

    @Override
    public boolean hasEnergyBuffer() {
        ProcessingMachineBlockEntity host = processingHost();
        return host != null && host.hasEnergyBuffer();
    }

    @Override
    public long energyStored() {
        ProcessingMachineBlockEntity host = processingHost();
        return host == null ? 0L : host.energyStored();
    }

    @Override
    public long energyCapacity() {
        ProcessingMachineBlockEntity host = processingHost();
        return host == null ? 0L : host.energyCapacity();
    }

    @Override
    public int progress() {
        ProcessingMachineBlockEntity host = processingHost();
        return host == null ? 0 : host.progress();
    }

    @Override
    public int duration() {
        ProcessingMachineBlockEntity host = processingHost();
        return host == null ? 0 : host.duration();
    }

    @Override
    public boolean hasFluidTanks() {
        ProcessingMachineBlockEntity host = processingHost();
        return host != null && host.hasFluidTanks();
    }

    @Override
    public int fillAir(int amount) {
        ProcessingMachineBlockEntity host = processingHost();
        return host == null ? 0 : host.fillAir(amount);
    }

    @Override
    public int fillFluid(
            FluidStack stack, IFluidHandler.FluidAction action) {
        ProcessingMachineBlockEntity host = processingHost();
        return host == null ? 0 : host.fillFluid(stack, action);
    }

    @Override
    public int incomingRedstone(Direction side) {
        if (level == null || side == null) {
            return 0;
        }
        BlockPos neighbor = worldPosition.relative(side);
        return Math.max(
                level.getSignal(neighbor, side.getOpposite()),
                level.getDirectSignal(neighbor, side.getOpposite()));
    }

    @Override
    public void notifyRedstone() {
        if (level == null || level.isClientSide) {
            return;
        }
        level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        for (Direction side : Direction.values()) {
            level.updateNeighborsAt(
                    worldPosition.relative(side),
                    getBlockState().getBlock());
        }
    }

    @Override
    public long gameTime() {
        return level == null ? 0L : level.getGameTime();
    }

    @Override
    public Level level() {
        return level;
    }

    @Override
    public BlockPos hostPos() {
        return worldPosition;
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
            if (portType() == PortType.FLUID_OUT) {
                return false;
            }
            MultiblockPortHost host = host();
            return host != null
                    && host.fluidInputTanks().contains(tank)
                    && host.tanks().get(tank).isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (portType() == PortType.FLUID_OUT) {
                return 0;
            }
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
                    drainableOutputTanks(host),
                    ProcessingMachineSpec.CapabilityAccess.OUTPUT);
        }

        private List<Integer> drainableOutputTanks(MultiblockPortHost host) {
            List<Integer> outputs = host.fluidOutputTanks();
            if (portType() != PortType.FLUID_OUT
                    || controller == null
                    || !(host instanceof AbstractDistillationTowerBlockEntity
                            tower)) {
                return outputs;
            }
            int localY = getBlockPos().getY() - controller.getY();
            List<Integer> matching = new java.util.ArrayList<>();
            for (int index : outputs) {
                FluidStack stored = host.tanks().get(index).getFluid();
                if (stored.isEmpty()) {
                    continue;
                }
                if (DistillationTowerFluidRouting.localY(
                        tower.fluidRouting(), stored) == localY) {
                    matching.add(index);
                }
            }
            return matching;
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
