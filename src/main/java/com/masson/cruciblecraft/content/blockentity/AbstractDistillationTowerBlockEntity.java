package com.masson.cruciblecraft.content.blockentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverBehaviors;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverHost;
import com.masson.cruciblecraft.content.multiblock.MultiblockControllerBinding;
import com.masson.cruciblecraft.content.multiblock.MultiblockControllerSpec;
import com.masson.cruciblecraft.content.multiblock.MultiblockPortAggregator;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureCatalog;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureValidator;
import com.masson.cruciblecraft.content.multiblock.PluginQuarantinePolicy;
import com.masson.cruciblecraft.machine.CheckpointDecisions;
import com.masson.cruciblecraft.machine.processing.MachineVariant;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Shared JSON-gated tower host: heat or cold, same 3x3x9 footprint. */
public abstract class AbstractDistillationTowerBlockEntity
        extends ConfiguredProcessingMachineBlockEntity
        implements MultiblockControllerBinding {
    private static final String PLUGIN_TAG = "multiblock_plugins";

    private boolean structureValid;
    private Set<BlockPos> boundPorts = Set.of();
    private MultiblockStructureValidator.ValidationResult lastValidation;
    private boolean pluginQuarantined;
    private String pluginQuarantineReason = "";

    protected AbstractDistillationTowerBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state,
            MachineVariant variant) {
        super(type, pos, state, variant);
    }

    protected abstract DistillationTowerFluidRouting.Kind routing();

    public DistillationTowerFluidRouting.Kind fluidRouting() {
        return routing();
    }

    protected abstract List<ResourceLocation> plugins();

    protected abstract String translationKey();

    public abstract MultiblockControllerSpec controllerSpec();

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            AbstractDistillationTowerBlockEntity tower) {
        long phaseKey = CheckpointDecisions.phaseKey(
                pos.getX(), pos.getY(), pos.getZ());
        if (CheckpointDecisions.onPositionPhase(
                level.getGameTime(), phaseKey, 20)) {
            tower.recheckStructure(level, pos, state);
        }
        if (tower.structureValid && !tower.pluginQuarantined) {
            tower.tickProcessingServer();
            tower.tickBoundPortCovers();
        } else {
            tower.tickCoversServer();
        }
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
        updateStructureValid(validation.valid());
        DistillationTowerPartVisuals.apply(
                level,
                definition.orElseThrow(),
                pos,
                state.getValue(ProcessingMachineBlock.FACING),
                structureValid && !pluginQuarantined);
    }

    private void updateStructureValid(boolean valid) {
        if (structureValid != valid) {
            structureValid = valid;
            setChanged();
        }
    }

    private void tickBoundPortCovers() {
        if (level == null || level.isClientSide) {
            return;
        }
        for (BlockPos position : boundPorts) {
            if (level.getBlockEntity(position) instanceof MachineCoverHost host
                    && host != this) {
                MachineCoverBehaviors.tickAll(host);
            }
        }
    }

    public void clearBindings() {
        if (level != null && !level.isClientSide) {
            MultiblockPortAggregator.unbindLoaded(
                    level, worldPosition, boundPorts);
            var definition = MultiblockStructureCatalog.find(
                    controllerSpec().structureId());
            if (definition.isPresent()
                    && getBlockState().hasProperty(
                            ProcessingMachineBlock.FACING)) {
                DistillationTowerPartVisuals.apply(
                        level,
                        definition.orElseThrow(),
                        worldPosition,
                        getBlockState().getValue(
                                ProcessingMachineBlock.FACING),
                        false);
            }
        }
        boundPorts = Set.of();
        structureValid = false;
    }

    public MultiblockStructureValidator.ValidationResult lastValidation() {
        return lastValidation;
    }

    @Override
    protected void autoOutputItems(boolean pulse) {
        DistillationTowerAutoOutput.pushItems(this, pulse);
    }

    @Override
    protected void autoOutputFluids() {
        DistillationTowerAutoOutput.pushFluids(this, routing());
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag ids = new ListTag();
        for (ResourceLocation id : plugins()) {
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
        for (ResourceLocation current : plugins()) {
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
        return controllerSpec().structureId();
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
        return Component.translatable(translationKey());
    }
}
