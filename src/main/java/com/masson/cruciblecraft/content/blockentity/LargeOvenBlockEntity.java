package com.masson.cruciblecraft.content.blockentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.content.block.LargeOvenBlock;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.multiblock.MultiblockControllerBinding;
import com.masson.cruciblecraft.content.multiblock.MultiblockControllerSpec;
import com.masson.cruciblecraft.content.multiblock.MultiblockPortAggregator;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureCatalog;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureValidator;
import com.masson.cruciblecraft.content.multiblock.PluginQuarantinePolicy;
import com.masson.cruciblecraft.machine.CheckpointDecisions;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModMultiblockControllers;
import com.masson.cruciblecraft.registry.ModMultiblockPlugins;

import net.minecraft.core.BlockPos;
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

/** Shared oven host gated by the JSON 3x3x3 invar/coil structure. */
public final class LargeOvenBlockEntity
        extends ConfiguredProcessingMachineBlockEntity
        implements MultiblockControllerBinding {
    private static final String PLUGIN_TAG = "multiblock_plugins";

    private boolean structureValid;
    private Set<BlockPos> boundPorts = Set.of();
    private MultiblockStructureValidator.ValidationResult lastValidation;
    private boolean pluginQuarantined;
    private String pluginQuarantineReason = "";

    public LargeOvenBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModBlockEntities.LARGE_OVEN.get(),
                pos,
                state,
                ModMultiblockControllers.LARGE_OVEN.requireVariant());
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            LargeOvenBlockEntity oven) {
        long phaseKey = CheckpointDecisions.phaseKey(
                pos.getX(), pos.getY(), pos.getZ());
        if (!oven.structureValid
                || CheckpointDecisions.onPositionPhase(
                        level.getGameTime(), phaseKey, 20)) {
            oven.recheckStructure(level, pos, state);
        }
        if (oven.structureValid && !oven.pluginQuarantined) {
            oven.tickProcessingServer();
        } else {
            oven.tickCoversServer();
        }
        oven.updateActivity(level.getBlockState(pos));
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
        for (ResourceLocation id : ModMultiblockPlugins.LARGE_OVEN_PLUGINS) {
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
                : ModMultiblockPlugins.LARGE_OVEN_PLUGINS) {
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
        updateStructureValid(validation.valid());
    }

    private void updateActivity(BlockState state) {
        if (level == null
                || level.isClientSide
                || !state.is(com.masson.cruciblecraft.registry.ModBlocks.LARGE_OVEN.get())) {
            return;
        }
        boolean running = structureValid && runningActively();
        boolean lit = structureValid && (stored(EnergyType.ELECTRIC) > 0L || running);
        BlockState next = state;
        if (state.getValue(LargeOvenBlock.LIT) != lit) {
            next = next.setValue(LargeOvenBlock.LIT, lit);
        }
        if (state.getValue(LargeOvenBlock.RUNNING) != running) {
            next = next.setValue(LargeOvenBlock.RUNNING, running);
        }
        if (next != state) {
            level.setBlock(worldPosition, next, Block.UPDATE_CLIENTS);
        }
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

    public MultiblockStructureValidator.ValidationResult lastValidation() {
        return lastValidation;
    }

    @Override
    public ResourceLocation structureId() {
        return controllerSpec().structureId();
    }

    public MultiblockControllerSpec controllerSpec() {
        return ModMultiblockControllers.LARGE_OVEN;
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
        return Component.translatable("block.cruciblecraft.large_oven");
    }
}
