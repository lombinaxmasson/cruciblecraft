package com.masson.cruciblecraft.content.blockentity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.content.block.LargeSluiceBlock;
import com.masson.cruciblecraft.content.block.LargeSluiceGeometry;
import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.block.SluiceParts;
import com.masson.cruciblecraft.content.multiblock.MultiblockControllerBinding;
import com.masson.cruciblecraft.content.multiblock.MultiblockControllerSpec;
import com.masson.cruciblecraft.content.multiblock.MultiblockPortAggregator;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureCatalog;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PredicateKind;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureValidator;
import com.masson.cruciblecraft.content.multiblock.PluginQuarantinePolicy;
import com.masson.cruciblecraft.energy.AdjacentToggleableEnergy;
import com.masson.cruciblecraft.machine.CheckpointDecisions;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModBlocks;
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

/** Shared Large Sluice host gated by the source-exact 3x7x3 structure. */
public final class LargeSluiceBlockEntity
        extends ConfiguredProcessingMachineBlockEntity
        implements MultiblockControllerBinding {
    private static final String PLUGIN_TAG = "multiblock_plugins";

    private boolean structureValid;
    private boolean lastRunning;
    private Set<BlockPos> boundPorts = Set.of();
    private Set<BlockPos> boundParts = Set.of();
    private MultiblockStructureValidator.ValidationResult lastValidation;
    private boolean pluginQuarantined;
    private String pluginQuarantineReason = "";

    public LargeSluiceBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModBlockEntities.LARGE_SLUICE.get(),
                pos,
                state,
                ModMultiblockControllers.LARGE_SLUICE.requireVariant());
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            LargeSluiceBlockEntity sluice) {
        long phaseKey = CheckpointDecisions.phaseKey(
                pos.getX(), pos.getY(), pos.getZ());
        if (CheckpointDecisions.onPositionPhase(
                level.getGameTime(), phaseKey, 20)) {
            sluice.recheckStructure(level, pos, state);
        }
        if (sluice.structureValid && !sluice.pluginQuarantined) {
            sluice.tickProcessingServer();
        } else {
            sluice.tickCoversServer();
        }
        boolean running = sluice.structureValid && sluice.runningActively();
        if (running != sluice.lastRunning) {
            sluice.lastRunning = running;
            sluice.recheckStructure(level, pos, state);
        }
        sluice.updateAdjacentToggleableEnergySources(
                level, pos, state.getValue(
                        com.masson.cruciblecraft.content.block.ProcessingMachineBlock.FACING));
        sluice.updateActivity(level.getBlockState(pos));
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
        for (ResourceLocation id : ModMultiblockPlugins.LARGE_SLUICE_PLUGINS) {
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
                : ModMultiblockPlugins.LARGE_SLUICE_PLUGINS) {
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
                        state.getValue(
                                com.masson.cruciblecraft.content.block.ProcessingMachineBlock.FACING));
        lastValidation = validation;
        boundPorts = MultiblockPortAggregator.refresh(
                level,
                pos,
                controllerSpec().structureId(),
                validation,
                boundPorts);

        Set<BlockPos> parts = new HashSet<>();
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
                        state.getValue(
                                com.masson.cruciblecraft.content.block.ProcessingMachineBlock.FACING),
                        element.offset());
                if (level.hasChunkAt(target)
                        && level.getBlockEntity(target)
                                instanceof MteInPlaceBlockEntity part
                        && SluiceParts.isPart(part.spec())) {
                    part.bindStructureMember(
                            pos, controllerSpec().structureId());
                    parts.add(target.immutable());
                }
            }
        }
        for (BlockPos previous : boundParts) {
            if (!parts.contains(previous)
                    && level.hasChunkAt(previous)
                    && level.getBlockEntity(previous)
                            instanceof MteInPlaceBlockEntity part) {
                part.unbind(pos);
                paintPart(level, previous, 0);
            }
        }
        boundParts = Set.copyOf(parts);
        updateStructureValid(validation.valid());
        paintParts(
                level,
                state.getValue(
                        com.masson.cruciblecraft.content.block.ProcessingMachineBlock.FACING),
                validation.valid() && runningActively(),
                parts);
    }

    private void updateStructureValid(boolean valid) {
        if (structureValid != valid) {
            structureValid = valid;
            setChanged();
        }
    }

    private void updateActivity(BlockState state) {
        if (level == null
                || level.isClientSide
                || !state.is(ModBlocks.LARGE_SLUICE.get())) {
            return;
        }
        boolean running = structureValid && runningActively();
        boolean lit = structureValid
                && (stored(EnergyType.KINETIC_ROTATION) > 0L || running);
        BlockState next = state;
        if (state.getValue(LargeSluiceBlock.LIT) != lit) {
            next = next.setValue(LargeSluiceBlock.LIT, lit);
        }
        if (state.getValue(LargeSluiceBlock.RUNNING) != running) {
            next = next.setValue(LargeSluiceBlock.RUNNING, running);
        }
        if (next != state) {
            level.setBlock(worldPosition, next, Block.UPDATE_CLIENTS);
        }
    }

    public void clearBindings() {
        if (level != null && !level.isClientSide) {
            MultiblockPortAggregator.unbindLoaded(
                    level, worldPosition, boundPorts);
            for (BlockPos partPos : boundParts) {
                if (level.hasChunkAt(partPos)
                        && level.getBlockEntity(partPos)
                                instanceof MteInPlaceBlockEntity part) {
                    part.unbind(worldPosition);
                    paintPart(level, partPos, 0);
                }
            }
        }
        boundPorts = Set.of();
        boundParts = Set.of();
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
        return ModMultiblockControllers.LARGE_SLUICE;
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

    private void paintParts(
            Level level,
            Direction facing,
            boolean active,
            Set<BlockPos> parts) {
        int design = LargeSluiceGeometry.partDesign(facing, active);
        for (BlockPos partPos : parts) {
            paintPart(level, partPos, design);
        }
    }

    private static void paintPart(
            Level level, BlockPos partPos, int design) {
        if (!level.hasChunkAt(partPos)) {
            return;
        }
        BlockState state = level.getBlockState(partPos);
        if (!state.hasProperty(MteInPlaceBlock.SLUICE_DESIGN)
                || state.getValue(MteInPlaceBlock.SLUICE_DESIGN) == design) {
            return;
        }
        level.setBlock(
                partPos,
                state.setValue(MteInPlaceBlock.SLUICE_DESIGN, design),
                Block.UPDATE_CLIENTS);
    }

    private void updateAdjacentToggleableEnergySources(
            Level level, BlockPos pos, Direction facing) {
        boolean on = structureValid && getStateOnOff();
        for (LargeSluiceGeometry.Neighbor neighbor
                : LargeSluiceGeometry.adjacentEnergySources(pos, facing)) {
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
        return Component.translatable("block.cruciblecraft.large_sluice");
    }
}
