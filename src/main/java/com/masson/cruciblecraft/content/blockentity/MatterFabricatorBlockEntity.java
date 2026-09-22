package com.masson.cruciblecraft.content.blockentity;

import java.util.LinkedHashSet;
import java.util.Set;

import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.multiblock.MatterFabricatorStructure;
import com.masson.cruciblecraft.content.multiblock.MultiblockControllerBinding;
import com.masson.cruciblecraft.content.multiblock.MultiblockControllerSpec;
import com.masson.cruciblecraft.content.multiblock.MultiblockPort;
import com.masson.cruciblecraft.machine.CheckpointDecisions;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModMultiblockControllers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** GT6 Large Matter Fabricator 17199 on {@code RM.Massfab} / QU. */
public final class MatterFabricatorBlockEntity
        extends ConfiguredProcessingMachineBlockEntity
        implements MultiblockControllerBinding {
    private boolean structureValid;
    private Set<BlockPos> boundPorts = Set.of();

    public MatterFabricatorBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModBlockEntities.MATTER_FABRICATOR.get(),
                pos,
                state,
                ModMultiblockControllers.LARGE_MATTER_FABRICATOR.requireVariant());
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            MatterFabricatorBlockEntity fabricator) {
        long phaseKey = CheckpointDecisions.phaseKey(
                pos.getX(), pos.getY(), pos.getZ());
        if (!fabricator.structureValid
                || CheckpointDecisions.onPositionPhase(
                        level.getGameTime(), phaseKey, 20)) {
            fabricator.recheckStructure(level, pos, state);
        }
        if (fabricator.structureValid) {
            fabricator.tickProcessingServer();
        } else {
            fabricator.tickCoversServer();
        }
    }

    private void recheckStructure(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(MteInPlaceBlock.FACING);
        MatterFabricatorStructure.Check check =
                MatterFabricatorStructure.check(level, pos, facing);
        LinkedHashSet<BlockPos> desired = new LinkedHashSet<>();
        for (MatterFabricatorStructure.Port port : check.ports()) {
            if (level.hasChunkAt(port.pos())
                    && level.getBlockEntity(port.pos()) instanceof MultiblockPort bound
                    && bound.accepts(port.type())
                    && (bound.controllerPosition().isEmpty()
                            || bound.controllerPosition()
                                    .orElseThrow()
                                    .equals(pos))) {
                bound.bind(
                        pos,
                        MatterFabricatorStructure.STRUCTURE_ID,
                        port.type());
                desired.add(port.pos().immutable());
            }
        }
        for (BlockPos previous : boundPorts) {
            if (!desired.contains(previous)
                    && level.hasChunkAt(previous)
                    && level.getBlockEntity(previous) instanceof MultiblockPort bound) {
                bound.unbind(pos);
            }
        }
        boundPorts = Set.copyOf(desired);
        if (structureValid != check.formed()) {
            structureValid = check.formed();
            setChanged();
        }
    }

    public void clearBindings() {
        if (level != null && !level.isClientSide) {
            for (BlockPos previous : boundPorts) {
                if (level.hasChunkAt(previous)
                        && level.getBlockEntity(previous)
                                instanceof MultiblockPort bound) {
                    bound.unbind(worldPosition);
                }
            }
        }
        boundPorts = Set.of();
        if (structureValid) {
            structureValid = false;
            setChanged();
        }
    }

    @Override
    public ResourceLocation structureId() {
        return MatterFabricatorStructure.STRUCTURE_ID;
    }

    @Override
    public boolean structureValid() {
        return structureValid;
    }

    public MultiblockControllerSpec controllerSpec() {
        return ModMultiblockControllers.LARGE_MATTER_FABRICATOR;
    }

    @Override
    public void requestBuilderRecheck() {
        if (level != null && !level.isClientSide) {
            recheckStructure(level, worldPosition, getBlockState());
        }
    }

    @Override
    public ProcessingMachineBlockEntity processingHost() {
        return this;
    }

    @Override
    protected Direction machineFront() {
        return getBlockState().getValue(MteInPlaceBlock.FACING);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(
                "block.cruciblecraft.lead.large_matter_fabricator");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("structure_valid", structureValid);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        structureValid = tag.getBoolean("structure_valid");
    }
}
