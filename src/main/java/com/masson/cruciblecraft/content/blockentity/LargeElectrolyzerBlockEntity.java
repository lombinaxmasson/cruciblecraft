package com.masson.cruciblecraft.content.blockentity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.content.block.ElectrolyzerParts;
import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
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

/** Shared electrolyzer host gated by the JSON 3x3x2 18105 structure. */
public final class LargeElectrolyzerBlockEntity
        extends ConfiguredProcessingMachineBlockEntity
        implements MultiblockControllerBinding {
    private static final String PLUGIN_TAG = "multiblock_plugins";

    private boolean structureValid;
    private boolean lastActive;
    private boolean lastPassive;
    private boolean rerollTops;
    private Set<BlockPos> boundPorts = Set.of();
    private Set<BlockPos> boundParts = Set.of();
    private MultiblockStructureValidator.ValidationResult lastValidation;
    private boolean pluginQuarantined;
    private String pluginQuarantineReason = "";

    public LargeElectrolyzerBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModBlockEntities.LARGE_ELECTROLYZER.get(),
                pos,
                state,
                ModMultiblockControllers.LARGE_ELECTROLYZER.requireVariant());
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            LargeElectrolyzerBlockEntity electrolyzer) {
        long phaseKey = CheckpointDecisions.phaseKey(
                pos.getX(), pos.getY(), pos.getZ());
        if (electrolyzer.lastValidation == null
                || CheckpointDecisions.onPositionPhase(
                        level.getGameTime(), phaseKey, 20)) {
            electrolyzer.recheckStructure(level, pos, state);
        }
        if (electrolyzer.structureValid && !electrolyzer.pluginQuarantined) {
            electrolyzer.tickProcessingServer();
        } else {
            electrolyzer.tickCoversServer();
        }
        boolean active = electrolyzer.structureValid
                && electrolyzer.runningActively();
        boolean passive = electrolyzer.structureValid
                && electrolyzer.runningPassively();
        if (active != electrolyzer.lastActive
                || passive != electrolyzer.lastPassive) {
            electrolyzer.lastActive = active;
            electrolyzer.lastPassive = passive;
            electrolyzer.rerollTops = true;
            electrolyzer.recheckStructure(level, pos, state);
        }
        electrolyzer.updateAdjacentToggleableEnergySources(
                level,
                pos,
                state.getValue(ProcessingMachineBlock.FACING));
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
        for (ResourceLocation id
                : ModMultiblockPlugins.LARGE_ELECTROLYZER_PLUGINS) {
            ids.add(StringTag.valueOf(id.toString()));
        }
        tag.put(PLUGIN_TAG, ids);
        tag.putBoolean("electrolyzer_last_active", lastActive);
        tag.putBoolean("electrolyzer_last_passive", lastPassive);
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        lastActive = tag.getBoolean("electrolyzer_last_active");
        lastPassive = tag.getBoolean("electrolyzer_last_passive");
        ListTag ids = tag.getList(PLUGIN_TAG, Tag.TAG_STRING);
        List<String> saved = new ArrayList<>();
        for (Tag entry : ids) {
            saved.add(entry.getAsString());
        }
        pluginQuarantined = false;
        pluginQuarantineReason = "";
        int index = 0;
        for (ResourceLocation current
                : ModMultiblockPlugins.LARGE_ELECTROLYZER_PLUGINS) {
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
        Direction facing = state.getValue(ProcessingMachineBlock.FACING);
        boolean active = validation.valid() && runningActively();
        Set<BlockPos> parts = new HashSet<>();
        if (validation.valid()) {
            var structure = definition.orElseThrow();
            for (var element : structure.structure()) {
                if (structure.predicate(element).kind()
                        != PredicateKind.PORT) {
                    continue;
                }
                BlockPos target = structure.worldPosition(
                        pos, facing, element.offset());
                parts.add(target.immutable());
                paintPart(
                        level,
                        target,
                        ElectrolyzerParts.structureDesign(
                                element.offset().y(),
                                active,
                                currentDesign(level, target),
                                rerollTops,
                                level.random.nextInt(6)));
            }
        }
        for (BlockPos previous : boundParts) {
            if (!parts.contains(previous)) {
                paintPart(level, previous, 0);
            }
        }
        boundParts = Set.copyOf(parts);
        rerollTops = false;
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
            for (BlockPos part : boundParts) {
                paintPart(level, part, 0);
            }
        }
        boundPorts = Set.of();
        boundParts = Set.of();
        structureValid = false;
        lastActive = false;
        lastPassive = false;
        rerollTops = false;
    }

    public MultiblockStructureValidator.ValidationResult lastValidation() {
        return lastValidation;
    }

    @Override
    public ResourceLocation structureId() {
        return controllerSpec().structureId();
    }

    public MultiblockControllerSpec controllerSpec() {
        return ModMultiblockControllers.LARGE_ELECTROLYZER;
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
        if (level != null && !level.isClientSide) {
            updateAdjacentToggleableEnergySources(
                    level,
                    worldPosition,
                    getBlockState().getValue(ProcessingMachineBlock.FACING));
        }
        return coverEnabled();
    }

    private static int currentDesign(Level level, BlockPos partPos) {
        BlockState state = level.getBlockState(partPos);
        if (!state.hasProperty(MteInPlaceBlock.ELECTROLYZER_DESIGN)) {
            return 0;
        }
        return state.getValue(MteInPlaceBlock.ELECTROLYZER_DESIGN);
    }

    private static void paintPart(Level level, BlockPos partPos, int design) {
        if (!level.hasChunkAt(partPos)) {
            return;
        }
        BlockState state = level.getBlockState(partPos);
        if (!state.hasProperty(MteInPlaceBlock.ELECTROLYZER_DESIGN)
                || state.getValue(MteInPlaceBlock.ELECTROLYZER_DESIGN)
                        == design) {
            return;
        }
        level.setBlock(
                partPos,
                state.setValue(MteInPlaceBlock.ELECTROLYZER_DESIGN, design),
                Block.UPDATE_CLIENTS);
    }

    private void updateAdjacentToggleableEnergySources(
            Level level, BlockPos pos, Direction facing) {
        var definition = MultiblockStructureCatalog.find(
                controllerSpec().structureId());
        if (definition.isEmpty()
                || !definition.get().anchors().containsKey("center")) {
            return;
        }
        BlockPos below = definition.get()
                .anchor("center", pos, facing)
                .below();
        AdjacentToggleableEnergy.setOnOff(
                level,
                below,
                Direction.UP,
                EnergyType.ELECTRIC,
                structureValid && getStateOnOff());
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
        return Component.translatable("block.cruciblecraft.large_electrolyzer");
    }
}
