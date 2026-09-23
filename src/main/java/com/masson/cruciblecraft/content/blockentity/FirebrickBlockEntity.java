package com.masson.cruciblecraft.content.blockentity;

import java.util.Optional;

import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.multiblock.MultiblockPort;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PortType;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * GT6 fire-brick part 18000: one controller target, item/fluid proxy with
 * the same UP-in / DOWN-out / no-UP-fluid sides as the coke oven.
 */
public final class FirebrickBlockEntity extends BlockEntity implements MultiblockPort {
    private BlockPos controller;
    private ResourceLocation structure;

    public FirebrickBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FIREBRICK.get(), pos, state);
    }

    @Override
    public PortType portType() {
        return PortType.ITEM_FLUID_ENERGY;
    }

    @Override
    public boolean accepts(PortType type) {
        return type == PortType.ITEM_FLUID_ENERGY;
    }

    @Override
    public void bind(BlockPos controller, ResourceLocation structureId) {
        BlockPos immutable = controller.immutable();
        if (!canBind(immutable)) {
            return;
        }
        if (!immutable.equals(this.controller)
                || !structureId.equals(structure)) {
            this.controller = immutable;
            this.structure = structureId;
            setChanged();
            invalidateCaps();
        }
    }

    @Override
    public void unbind(BlockPos controller) {
        if (controller.equals(this.controller)) {
            this.controller = null;
            this.structure = null;
            setChanged();
            invalidateCaps();
        }
    }

    public void unbind() {
        if (controller != null || structure != null) {
            controller = null;
            structure = null;
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

    private void invalidateCaps() {
        if (level != null && !level.isClientSide) {
            level.invalidateCapabilities(worldPosition);
        }
    }

    /**
     * GT6 {@code checkAndSetTarget}: another coke-oven controller that still
     * counts this brick inside its 3×3×3 AABB keeps it, even if that oven is
     * currently unformed. Adjacent ovens cannot share a brick.
     */
    public boolean canBind(BlockPos claimant) {
        if (claimant.equals(controller) || controller == null || level == null) {
            return true;
        }
        if (!(level.getBlockEntity(controller) instanceof CokeOvenBlockEntity other)
                || !other.isInsideStructure(worldPosition)) {
            return true;
        }
        return false;
    }

    public CokeOvenBlockEntity host() {
        if (level == null || controller == null || structure == null) {
            return null;
        }
        if (level.getBlockEntity(controller) instanceof CokeOvenBlockEntity oven
                && oven.structureValid()
                && structure.equals(oven.structureId())
                && oven.isInsideStructure(worldPosition)) {
            return oven;
        }
        return null;
    }

    public IItemHandler items(Direction side) {
        CokeOvenBlockEntity host = host();
        return host == null ? null : host.automationItems(side);
    }

    public IFluidHandler fluids(Direction side) {
        CokeOvenBlockEntity host = host();
        return host == null ? null : host.automationFluids(side);
    }

    public IEnergyHandler energy(Direction side) {
        CokeOvenBlockEntity host = host();
        return host == null ? null : host;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (controller != null && structure != null) {
            tag.putLong("controller", controller.asLong());
            tag.putString("structure", structure.toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
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
    }
}
