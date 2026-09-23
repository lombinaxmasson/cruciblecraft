package com.masson.cruciblecraft.content.blockentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition;
import com.masson.cruciblecraft.content.multiblock.MultiblockControllerBinding;
import com.masson.cruciblecraft.content.multiblock.MultiblockPortAggregator;
import com.masson.cruciblecraft.content.multiblock.MultiblockPortHost;
import com.masson.cruciblecraft.content.multiblock.PortStoreSync;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureCatalog;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureValidator;
import com.masson.cruciblecraft.content.multiblock.PluginQuarantinePolicy;
import com.masson.cruciblecraft.content.multiblock.TankControllerProfiles;
import com.masson.cruciblecraft.content.multiblock.TankControllerProfiles.Profile;
import com.masson.cruciblecraft.machine.CheckpointDecisions;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineAutoIo;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModMultiblockPlugins;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * GT6 3x3x3 fluid storage controller. The controller identity is one of the
 * Small Tank Main Valve MTEs; all twenty-five matching wall cells bridge one
 * profile-sized tank and never create twenty-five independent supplies.
 */
public final class TankBlockEntity extends BlockEntity
        implements MultiblockControllerBinding, MultiblockPortHost, IFluidHandler {
    private static final int LEGACY_TOMBSTONE_CAPACITY_MB = 256_000;
    private static final String PLUGIN_TAG = "multiblock_plugins";
    public static final ResourceLocation STRUCTURE_ID =
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, "tank_3x3x3");

    private final Profile profile;
    private final FluidTank contents;
    private final List<FluidTank> tanks;
    private final ItemStackHandler inventory = new ItemStackHandler(0);

    private boolean structureValid;
    private Set<BlockPos> boundPorts = Set.of();
    private MultiblockStructureValidator.ValidationResult lastValidation;
    private boolean pluginQuarantined;
    private String pluginQuarantineReason = "";

    public TankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TANK_3X3X3.get(), pos, state);
        profile = profileFor(state);
        contents = new FluidTank(
                profile == null
                        ? LEGACY_TOMBSTONE_CAPACITY_MB
                        : profile.capacityMb()) {
            @Override
            protected void onContentsChanged() {
                TankBlockEntity.this.setChanged();
            }

            @Override
            public boolean isFluidValid(FluidStack stack) {
                return profile != null
                        && TankFluidSafety.allowedFailure(profile, stack)
                        == TankFluidSafety.Failure.NONE;
            }
        };
        tanks = List.of(contents);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            TankBlockEntity tank) {
        long phaseKey = CheckpointDecisions.phaseKey(
                pos.getX(), pos.getY(), pos.getZ());
        if (tank.lastValidation == null
                || CheckpointDecisions.onPositionPhase(
                level.getGameTime(), phaseKey, 20)) {
            tank.recheckStructure(level, pos, state);
        }
        if (!tank.structureValid || tank.pluginQuarantined) {
            return;
        }
        PortStoreSync.pullInputs(tank);
        tank.tickFluidSafety(level);
        if (tank.structureValid && !tank.pluginQuarantined) {
            tank.autoOutput(level);
        }
        PortStoreSync.pushOutputs(tank);
    }

    public FluidTank contents() {
        return contents;
    }

    public Profile profile() {
        return profile;
    }

    public boolean handles(EnergyType type, Direction side) {
        return false;
    }

    public boolean pluginQuarantined() {
        return pluginQuarantined;
    }

    public String pluginQuarantineReason() {
        return pluginQuarantineReason;
    }

    private void recheckStructure(
            Level level,
            BlockPos pos,
            BlockState state) {
        var definition = MultiblockStructureCatalog.find(STRUCTURE_ID);
        if (definition.isEmpty()) {
            clearBindings();
            lastValidation = null;
            updateStructureValid(false);
            return;
        }
        MultiblockStructureValidator.ValidationResult validation =
                MultiblockStructureValidator.validate(
                        definition.orElseThrow(),
                        level,
                        pos,
                        facing(state));
        if (validation.valid() && !profileMatches(validation)) {
            validation = invalidProfileValidation(validation);
        }
        lastValidation = validation;
        boundPorts = MultiblockPortAggregator.refresh(
                level, pos, STRUCTURE_ID, validation, boundPorts);
        updateStructureValid(validation.valid());
    }

    private boolean profileMatches(
            MultiblockStructureValidator.ValidationResult validation) {
        if (validation.ports().size() != 25) {
            return false;
        }
        for (MultiblockStructureValidator.MatchedPort port
                : validation.ports()) {
            if (port.type()
                    != MultiblockStructureDefinition.PortType.FLUID
                    || !(level.getBlockState(port.position()).getBlock()
                            instanceof MteInPlaceBlock wall)
                    || !TankControllerProfiles.matchesWall(
                            profile, wall.spec())) {
                return false;
            }
        }
        return true;
    }

    private MultiblockStructureValidator.ValidationResult
            invalidProfileValidation(
                    MultiblockStructureValidator.ValidationResult validation) {
        BlockPos mismatch = validation.ports().stream()
                .map(MultiblockStructureValidator.MatchedPort::position)
                .filter(position -> !(level.getBlockState(position).getBlock()
                        instanceof MteInPlaceBlock wall)
                        || !TankControllerProfiles.matchesWall(
                                profile,
                                ((MteInPlaceBlock) level.getBlockState(position)
                                        .getBlock()).spec()))
                .findFirst()
                .orElse(worldPosition);
        List<MultiblockStructureValidator.Diagnostic> diagnostics =
                new ArrayList<>(validation.diagnostics());
        diagnostics.add(new MultiblockStructureValidator.Diagnostic(
                mismatch,
                "tank wall meta " + profile.wallMeta(),
                level.getBlockState(mismatch).toString()));
        if (diagnostics.size()
                > MultiblockStructureValidator.MAX_DIAGNOSTICS) {
            diagnostics = diagnostics.subList(
                    0, MultiblockStructureValidator.MAX_DIAGNOSTICS);
        }
        return new MultiblockStructureValidator.ValidationResult(
                MultiblockStructureValidator.Status.INVALID,
                diagnostics,
                validation.ports());
    }

    private void updateStructureValid(boolean valid) {
        if (structureValid != valid) {
            structureValid = valid;
            setChanged();
        }
    }

    public void clearBindings() {
        if (level != null && !level.isClientSide) {
            MultiblockPortAggregator.unbindLoaded(
                    level, worldPosition, boundPorts);
        }
        boundPorts = Set.of();
        structureValid = false;
    }

    @Override
    public void setRemoved() {
        clearBindings();
        super.setRemoved();
    }

    public MultiblockStructureValidator.ValidationResult lastValidation() {
        return lastValidation;
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("contents", contents.writeToNBT(
                registries, new CompoundTag()));
        tag.putInt(
                "tank_profile_meta",
                profile == null ? -1 : profile.controllerMeta());
        ListTag ids = new ListTag();
        for (ResourceLocation id
                : ModMultiblockPlugins.TANK_PLUGINS) {
            ids.add(StringTag.valueOf(id.toString()));
        }
        tag.put(PLUGIN_TAG, ids);
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        contents.readFromNBT(registries, tag.getCompound("contents"));
        ListTag ids = tag.getList(PLUGIN_TAG, Tag.TAG_STRING);
        List<String> saved = new ArrayList<>();
        for (Tag entry : ids) {
            saved.add(entry.getAsString());
        }
        pluginQuarantined = false;
        pluginQuarantineReason = "";
        if (profile == null) {
            pluginQuarantined = true;
            pluginQuarantineReason =
                    "legacy tank_3x3x3 controller requires migration";
        }
        if (profile != null
                && tag.contains("tank_profile_meta")
                && tag.getInt("tank_profile_meta") != profile.controllerMeta()) {
            pluginQuarantined = true;
            pluginQuarantineReason = "tank profile identity mismatch";
        }
        int index = 0;
        for (ResourceLocation current : ModMultiblockPlugins.TANK_PLUGINS) {
            String savedId = index < saved.size() ? saved.get(index) : "";
            PluginQuarantinePolicy.Decision decision =
                    PluginQuarantinePolicy.resolve(
                            savedId, current.toString());
            if (decision.resolution()
                    == PluginQuarantinePolicy.Resolution.QUARANTINED) {
                pluginQuarantined = true;
                pluginQuarantineReason =
                        decision.quarantineReason().orElse("");
            }
            index++;
        }
        if (pluginQuarantined) {
            setChanged();
        }
    }

    @Override
    public ResourceLocation structureId() {
        return STRUCTURE_ID;
    }

    @Override
    public boolean structureValid() {
        return structureValid;
    }

    @Override
    public void requestBuilderRecheck() {
        if (level != null && !level.isClientSide) {
            recheckStructure(level, worldPosition, getBlockState());
        }
    }

    private void tickFluidSafety(Level level) {
        FluidStack stored = contents.getFluid();
        if (stored.isEmpty()) {
            return;
        }
        TankFluidSafety.Failure allowed =
                TankFluidSafety.allowedFailure(profile, stored);
        if (allowed != TankFluidSafety.Failure.NONE) {
            if (allowed == TankFluidSafety.Failure.TOO_HOT) {
                meltdown(level);
            } else {
                discardFluid();
                if (allowed == TankFluidSafety.Failure.POWER_CONDUCTING) {
                    CrucibleWorldHazards.boilHazards(
                            level,
                            worldPosition,
                            TankFluidSafety.temperature(stored) - 273.15F,
                            CrucibleWorldHazards.SMALL_GAS_RANGE,
                            1);
                }
            }
            return;
        }
        TankFluidSafety.Failure hazard =
                TankFluidSafety.hazard(profile, stored);
        if (hazard == TankFluidSafety.Failure.NONE) {
            return;
        }
        discardFluid();
        if (hazard == TankFluidSafety.Failure.ACID) {
            destroyStructure(level);
        } else if (hazard == TankFluidSafety.Failure.MAGIC
                || hazard == TankFluidSafety.Failure.PLASMA
                || hazard == TankFluidSafety.Failure.GAS) {
            CrucibleWorldHazards.boilHazards(
                    level,
                    worldPosition,
                    TankFluidSafety.temperature(stored) - 273.15F,
                    CrucibleWorldHazards.SMALL_GAS_RANGE,
                    2);
        }
    }

    private void autoOutput(Level level) {
        FluidStack stored = contents.getFluid();
        if (stored.isEmpty()) {
            return;
        }
        Direction facing = facing(getBlockState());
        boolean canEmit = facing.getAxis().isHorizontal()
                || TankFluidSafety.isGas(stored)
                || (TankFluidSafety.isLighter(stored)
                        ? facing == Direction.UP
                        : facing == Direction.DOWN);
        if (!canEmit) {
            return;
        }
        IFluidHandler neighbor = level.getCapability(
                Capabilities.FluidHandler.BLOCK,
                worldPosition.relative(facing),
                facing.getOpposite());
        if (neighbor != null
                && ProcessingMachineAutoIo.moveFluids(this, neighbor) > 0) {
            setChanged();
        }
    }

    private void discardFluid() {
        contents.drain(contents.getFluidAmount(), IFluidHandler.FluidAction.EXECUTE);
        setChanged();
    }

    private void destroyStructure(Level level) {
        Set<BlockPos> ports = Set.copyOf(boundPorts);
        clearBindings();
        for (BlockPos port : ports) {
            level.setBlock(port, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        }
        level.setBlock(worldPosition, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
    }

    private void meltdown(Level level) {
        discardFluid();
        clearBindings();
        BlockPos center = worldPosition.relative(facing(getBlockState()));
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    level.setBlock(
                            center.offset(x, y, z),
                            CrucibleWorldHazards.meltdownLavaState(),
                            3);
                }
            }
        }
    }

    private static Profile profileFor(BlockState state) {
        if (state.getBlock() instanceof MteInPlaceBlock inplace) {
            return TankControllerProfiles.find(inplace.spec()).orElse(null);
        }
        return null;
    }

    private static Direction facing(BlockState state) {
        if (state.hasProperty(MteInPlaceBlock.FACING)) {
            return state.getValue(MteInPlaceBlock.FACING);
        }
        if (state.hasProperty(ProcessingMachineBlock.FACING)) {
            return state.getValue(ProcessingMachineBlock.FACING);
        }
        return Direction.NORTH;
    }

    @Override
    public ProcessingMachineBlockEntity processingHost() {
        return null;
    }

    @Override
    public MultiblockPortHost portHost() {
        return this;
    }

    // --- MultiblockPortHost ---

    @Override
    public ItemStackHandler inventory() {
        return inventory;
    }

    @Override
    public List<FluidTank> tanks() {
        return tanks;
    }

    @Override
    public List<Integer> itemInputSlots() {
        return List.of();
    }

    @Override
    public List<Integer> itemOutputSlots() {
        return List.of();
    }

    @Override
    public List<Integer> fluidInputTanks() {
        return List.of(0);
    }

    @Override
    public List<Integer> fluidOutputTanks() {
        return List.of(0);
    }

    @Override
    public BlockState blockState() {
        return getBlockState();
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return tank == 0 ? contents.getFluid().copy() : FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        return tank == 0 ? contents.getCapacity() : 0;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return profile != null
                && tank == 0
                && TankFluidSafety.allowedFailure(profile, stack)
                        == TankFluidSafety.Failure.NONE;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (profile == null || resource.isEmpty()) {
            return 0;
        }
        return contents.fill(resource, action);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (profile == null || resource.isEmpty()) {
            return FluidStack.EMPTY;
        }
        return contents.drain(resource, action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (profile == null || maxDrain <= 0) {
            return FluidStack.EMPTY;
        }
        return contents.drain(maxDrain, action);
    }
}
