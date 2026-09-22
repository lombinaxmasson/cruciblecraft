package com.masson.cruciblecraft.content.blockentity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.content.block.CrusherWheels;
import com.masson.cruciblecraft.content.block.LargeCrusherGeometry;
import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.energy.AdjacentToggleableEnergy;
import com.masson.cruciblecraft.content.multiblock.MultiblockControllerBinding;
import com.masson.cruciblecraft.content.multiblock.MultiblockControllerSpec;
import com.masson.cruciblecraft.content.multiblock.MultiblockPortAggregator;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureCatalog;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PredicateKind;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureValidator;
import com.masson.cruciblecraft.content.multiblock.PluginQuarantinePolicy;
import com.masson.cruciblecraft.machine.CheckpointDecisions;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModMultiblockControllers;
import com.masson.cruciblecraft.registry.ModMultiblockPlugins;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Shared crusher host gated by the JSON 5x5x3 tungstensteel structure. */
public final class LargeCrusherBlockEntity
        extends ConfiguredProcessingMachineBlockEntity
        implements MultiblockControllerBinding {
    private static final String PLUGIN_TAG = "multiblock_plugins";

    private boolean structureValid;
    private boolean lastRunning;
    private Set<BlockPos> boundPorts = Set.of();
    private Set<BlockPos> boundWheels = Set.of();
    private MultiblockStructureValidator.ValidationResult lastValidation;
    private boolean pluginQuarantined;
    private String pluginQuarantineReason = "";

    public LargeCrusherBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModBlockEntities.LARGE_CRUSHER.get(),
                pos,
                state,
                ModMultiblockControllers.LARGE_CRUSHER.requireVariant());
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            LargeCrusherBlockEntity crusher) {
        long phaseKey = CheckpointDecisions.phaseKey(
                pos.getX(), pos.getY(), pos.getZ());
        if (CheckpointDecisions.onPositionPhase(
                level.getGameTime(), phaseKey, 20)) {
            crusher.recheckStructure(level, pos, state);
        }
        if (crusher.structureValid && !crusher.pluginQuarantined) {
            crusher.tickProcessingServer();
        } else {
            crusher.tickCoversServer();
        }
        boolean running = crusher.structureValid && crusher.runningActively();
        if (running != crusher.lastRunning) {
            crusher.lastRunning = running;
            crusher.recheckStructure(level, pos, state);
        }
        crusher.updateAdjacentToggleableEnergySources(
                level, pos, state.getValue(ProcessingMachineBlock.FACING));
    }

    public boolean pluginQuarantined() {
        return pluginQuarantined;
    }

    public String pluginQuarantineReason() {
        return pluginQuarantineReason;
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag ids = new ListTag();
        for (ResourceLocation id : ModMultiblockPlugins.LARGE_CRUSHER_PLUGINS) {
            ids.add(StringTag.valueOf(id.toString()));
        }
        tag.put(PLUGIN_TAG, ids);
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ListTag ids = tag.getList(PLUGIN_TAG, Tag.TAG_STRING);
        List<String> saved = new ArrayList<>();
        for (Tag entry : ids) {
            saved.add(entry.getAsString());
        }
        pluginQuarantined = false;
        pluginQuarantineReason = "";
        int index = 0;
        for (ResourceLocation current
                : ModMultiblockPlugins.LARGE_CRUSHER_PLUGINS) {
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

    private void recheckStructure(
            Level level,
            BlockPos pos,
            BlockState state) {
        var definition = MultiblockStructureCatalog.find(
                controllerSpec().structureId());
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
                        state.getValue(ProcessingMachineBlock.FACING));
        lastValidation = validation;
        boundPorts = MultiblockPortAggregator.refresh(
                level,
                pos,
                controllerSpec().structureId(),
                validation,
                boundPorts);
        Set<BlockPos> wheels = new HashSet<>();
        if (validation.valid()) {
            var structure = definition.orElseThrow();
            for (var element : structure.structure()) {
                var kind = structure.predicate(element).kind();
                if (kind != PredicateKind.BLOCK
                        && kind != PredicateKind.PORT) {
                    continue;
                }
                BlockPos target = structure.worldPosition(
                        pos,
                        state.getValue(ProcessingMachineBlock.FACING),
                        element.offset());
                if (level.hasChunkAt(target)
                        && level.getBlockEntity(target)
                                instanceof MteInPlaceBlockEntity wheel
                        && CrusherWheels.isPart(wheel.spec())) {
                    wheel.bindStructureMember(
                            pos, controllerSpec().structureId());
                    wheels.add(target.immutable());
                }
            }
        }
        for (BlockPos previous : boundWheels) {
            if (!wheels.contains(previous)
                    && level.hasChunkAt(previous)
                    && level.getBlockEntity(previous)
                            instanceof MteInPlaceBlockEntity wheel) {
                wheel.unbind(pos);
                paintWheel(level, previous, 0);
            }
        }
        boundWheels = Set.copyOf(wheels);
        updateStructureValid(validation.valid());
        paintWheels(
                level,
                state.getValue(ProcessingMachineBlock.FACING),
                validation.valid() && runningActively(),
                wheels);
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
            for (BlockPos wheelPos : boundWheels) {
                if (level.hasChunkAt(wheelPos)
                        && level.getBlockEntity(wheelPos)
                                instanceof MteInPlaceBlockEntity wheel) {
                    wheel.unbind(worldPosition);
                    paintWheel(level, wheelPos, 0);
                }
            }
        }
        boundPorts = Set.of();
        boundWheels = Set.of();
        structureValid = false;
    }

    public MultiblockStructureValidator.ValidationResult lastValidation() {
        return lastValidation;
    }

    @Override
    public ResourceLocation structureId() {
        return controllerSpec().structureId();
    }

    public MultiblockControllerSpec controllerSpec() {
        return ModMultiblockControllers.LARGE_CRUSHER;
    }

    @Override
    public void requestBuilderRecheck() {
        if (level != null && !level.isClientSide) {
            recheckStructure(level, worldPosition, getBlockState());
        }
    }

    @Override
    public boolean switchableOnOff() {
        return true;
    }

    @Override
    public boolean getStateOnOff() {
        return coverEnabled();
    }

    @Override
    public boolean setStateOnOff(boolean on) {
        setCoverEnabled(on);
        return coverEnabled();
    }

    private void paintWheels(
            Level level,
            Direction facing,
            boolean running,
            Set<BlockPos> wheels) {
        int design = LargeCrusherGeometry.wheelDesign(facing, running);
        for (BlockPos wheelPos : wheels) {
            paintWheel(level, wheelPos, design);
        }
    }

    private static void paintWheel(Level level, BlockPos wheelPos, int design) {
        if (!level.hasChunkAt(wheelPos)) {
            return;
        }
        BlockState state = level.getBlockState(wheelPos);
        if (!state.hasProperty(MteInPlaceBlock.WHEEL_DESIGN)
                || state.getValue(MteInPlaceBlock.WHEEL_DESIGN) == design) {
            return;
        }
        level.setBlock(
                wheelPos,
                state.setValue(MteInPlaceBlock.WHEEL_DESIGN, design),
                Block.UPDATE_CLIENTS);
    }

    private void updateAdjacentToggleableEnergySources(
            Level level, BlockPos pos, Direction facing) {
        boolean on = structureValid && getStateOnOff();
        for (LargeCrusherGeometry.Neighbor neighbor :
                LargeCrusherGeometry.adjacentEnergySources(pos, facing)) {
            AdjacentToggleableEnergy.setOnOff(
                    level,
                    neighbor.position(),
                    neighbor.face(),
                    EnergyType.KINETIC_ROTATION,
                    on);
        }
    }

    @Override
    public boolean structureValid() {
        return structureValid;
    }

    @Override
    public ProcessingMachineBlockEntity processingHost() {
        return this;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.cruciblecraft.large_crusher");
    }
}
