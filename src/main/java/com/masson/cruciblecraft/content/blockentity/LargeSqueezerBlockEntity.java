package com.masson.cruciblecraft.content.blockentity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.content.block.LargeSqueezerBlock;
import com.masson.cruciblecraft.content.block.LargeSqueezerGeometry;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
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

/** GT6 17114 host: source-exact structure and RU adjacency, shared Squeezer map. */
public final class LargeSqueezerBlockEntity
        extends ConfiguredProcessingMachineBlockEntity
        implements MultiblockControllerBinding {
    private static final String PLUGIN_TAG = "multiblock_plugins";
    private boolean structureValid;
    private boolean lastRunning;
    private Set<BlockPos> boundPorts = Set.of();
    private MultiblockStructureValidator.ValidationResult lastValidation;
    private boolean pluginQuarantined;
    private String pluginQuarantineReason = "";

    public LargeSqueezerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LARGE_SQUEEZER.get(), pos, state,
                ModMultiblockControllers.LARGE_SQUEEZER.requireVariant());
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
            LargeSqueezerBlockEntity squeezer) {
        long phaseKey = CheckpointDecisions.phaseKey(pos.getX(), pos.getY(), pos.getZ());
        if (squeezer.lastValidation == null
                || CheckpointDecisions.onPositionPhase(level.getGameTime(), phaseKey, 20)) {
            squeezer.recheckStructure(level, pos, state);
        }
        if (squeezer.structureValid && !squeezer.pluginQuarantined) {
            squeezer.tickProcessingServer();
        } else {
            squeezer.tickCoversServer();
        }
        boolean running = squeezer.structureValid && squeezer.runningActively();
        if (running != squeezer.lastRunning) {
            squeezer.lastRunning = running;
            squeezer.recheckStructure(level, pos, state);
        }
        squeezer.updateAdjacentToggleableEnergySources(
                level, pos, state.getValue(ProcessingMachineBlock.FACING));
        squeezer.updateActivity(level.getBlockState(pos));
    }

    private void recheckStructure(Level level, BlockPos pos, BlockState state) {
        var definition = MultiblockStructureCatalog.find(controllerSpec().structureId());
        if (definition.isEmpty()) {
            clearBindings();
            lastValidation = null;
            updateStructureValid(false);
            return;
        }
        var validation = MultiblockStructureValidator.validate(definition.orElseThrow(),
                level, pos, state.getValue(ProcessingMachineBlock.FACING));
        lastValidation = validation;
        boundPorts = MultiblockPortAggregator.refresh(level, pos,
                controllerSpec().structureId(), validation, boundPorts);
        if (validation.valid()) {
            Set<BlockPos> ports = new HashSet<>();
            for (var element : definition.orElseThrow().structure()) {
                var kind = definition.orElseThrow().predicate(element).kind();
                if (kind != PredicateKind.PORT) continue;
                BlockPos target = definition.orElseThrow().worldPosition(pos,
                        state.getValue(ProcessingMachineBlock.FACING), element.offset());
                ports.add(target.immutable());
            }
            boundPorts = Set.copyOf(ports);
        }
        updateStructureValid(validation.valid());
    }

    private void updateStructureValid(boolean valid) {
        if (structureValid != valid) {
            structureValid = valid;
            setChanged();
        }
    }

    private void updateActivity(BlockState state) {
        if (level == null || level.isClientSide || !state.is(ModBlocks.LARGE_SQUEEZER.get())) return;
        if (state.getValue(LargeSqueezerBlock.LIT)
                != (stored(EnergyType.KINETIC_ROTATION) > 0L)) {
            level.setBlock(worldPosition,
                    state.setValue(LargeSqueezerBlock.LIT,
                            stored(EnergyType.KINETIC_ROTATION) > 0L),
                    Block.UPDATE_CLIENTS);
        }
    }

    private void updateAdjacentToggleableEnergySources(Level level, BlockPos pos,
            Direction facing) {
        boolean on = structureValid && getStateOnOff();
        for (LargeSqueezerGeometry.Neighbor neighbor
                : LargeSqueezerGeometry.adjacentEnergySources(pos, facing)) {
            AdjacentToggleableEnergy.setOnOff(level, neighbor.position(),
                    neighbor.face(), EnergyType.KINETIC_ROTATION, on);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag ids = new ListTag();
        for (ResourceLocation id : ModMultiblockPlugins.LARGE_SQUEEZER_PLUGINS) {
            ids.add(StringTag.valueOf(id.toString()));
        }
        tag.put(PLUGIN_TAG, ids);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ListTag ids = tag.getList(PLUGIN_TAG, Tag.TAG_STRING);
        List<String> saved = new ArrayList<>();
        for (Tag entry : ids) saved.add(entry.getAsString());
        pluginQuarantined = false;
        pluginQuarantineReason = "";
        int index = 0;
        for (ResourceLocation current : ModMultiblockPlugins.LARGE_SQUEEZER_PLUGINS) {
            String old = index < saved.size() ? saved.get(index) : "";
            var decision = PluginQuarantinePolicy.resolve(old, current.toString());
            if (decision.resolution() == PluginQuarantinePolicy.Resolution.QUARANTINED) {
                pluginQuarantined = true;
                pluginQuarantineReason = decision.quarantineReason().orElse("");
            }
            index++;
        }
    }

    public boolean pluginQuarantined() { return pluginQuarantined; }
    public String pluginQuarantineReason() { return pluginQuarantineReason; }
    public MultiblockStructureValidator.ValidationResult lastValidation() { return lastValidation; }

    public void clearBindings() {
        if (level != null && !level.isClientSide) {
            MultiblockPortAggregator.unbindLoaded(level, worldPosition, boundPorts);
        }
        boundPorts = Set.of();
        structureValid = false;
    }

    @Override public ResourceLocation structureId() { return controllerSpec().structureId(); }
    public MultiblockControllerSpec controllerSpec() { return ModMultiblockControllers.LARGE_SQUEEZER; }
    @Override public void requestBuilderRecheck() {
        if (level != null && !level.isClientSide) recheckStructure(level, worldPosition, getBlockState());
    }
    @Override public boolean switchableOnOff() { return true; }
    @Override public boolean getStateOnOff() { return coverEnabled(); }
    @Override public boolean setStateOnOff(boolean on) { setCoverEnabled(on); return coverEnabled(); }
    @Override public boolean structureValid() { return structureValid; }
    @Override public ProcessingMachineBlockEntity processingHost() { return this; }
    @Override public Component getDisplayName() {
        return Component.translatable("block.cruciblecraft.large_squeezer");
    }
}
