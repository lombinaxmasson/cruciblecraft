package com.masson.cruciblecraft.content.blockentity;

import java.util.Set;

import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.multiblock.MultiblockControllerBinding;
import com.masson.cruciblecraft.content.multiblock.MultiblockControllerSpec;
import com.masson.cruciblecraft.content.multiblock.MultiblockPortAggregator;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureCatalog;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureValidator;
import com.masson.cruciblecraft.machine.CheckpointDecisions;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModMultiblockControllers;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Shared processing host gated by the JSON large-centrifuge structure. */
public final class LargeCentrifugeBlockEntity
        extends ConfiguredProcessingMachineBlockEntity
        implements MultiblockControllerBinding {
    private boolean structureValid;
    private Set<BlockPos> boundPorts = Set.of();
    private MultiblockStructureValidator.ValidationResult lastValidation;

    public LargeCentrifugeBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModBlockEntities.LARGE_CENTRIFUGE.get(),
                pos,
                state,
                ModMultiblockControllers.LARGE_CENTRIFUGE.requireVariant());
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            LargeCentrifugeBlockEntity centrifuge) {
        long phaseKey = CheckpointDecisions.phaseKey(
                pos.getX(), pos.getY(), pos.getZ());
        if (CheckpointDecisions.onPositionPhase(
                level.getGameTime(), phaseKey, 20)) {
            centrifuge.recheckStructure(level, pos, state);
        }
        if (centrifuge.structureValid) {
            centrifuge.tickProcessingServer();
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
    public MultiblockControllerSpec controllerSpec() {
        return ModMultiblockControllers.LARGE_CENTRIFUGE;
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
        return Component.translatable(
                "block.cruciblecraft.large_centrifuge");
    }
}
