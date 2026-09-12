package com.masson.cruciblecraft.content.blockentity;

import java.util.Optional;

import com.masson.cruciblecraft.content.block.GtTreeHoleBlock;
import com.masson.cruciblecraft.content.block.GtTreeLeavesBlock;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.worldgen.tree.prep.GtTreeSpecies;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public final class GtTreeHoleBlockEntity extends BlockEntity {
    public static final int REGEN_TICKS = 600;
    private static final ResourceLocation RAINBOW_SAP =
            ResourceLocation.fromNamespaceAndPath("cruciblecraft", "rainbow_sap");

    private final FluidTank tank = new FluidTank(GtTreeSpecies.HOLE_FLUID_MILLIBUCKETS) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return productFluid()
                    .map(fluid -> stack.getFluid().isSame(fluid))
                    .orElse(false);
        }
    };
    private boolean hasProduct;

    public GtTreeHoleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TREE_HOLE.get(), pos, state);
    }

    public static void serverTick(
            Level level, BlockPos pos, BlockState state, GtTreeHoleBlockEntity hole) {
        if (level.getGameTime() % REGEN_TICKS != 0L || hole.hasProduct) {
            return;
        }
        if (!hole.hasCanopy()) {
            return;
        }
        hole.fillProduct();
    }

    public boolean hasProduct() {
        return hasProduct;
    }

    public void setHasProduct(boolean hasProduct) {
        if (this.hasProduct == hasProduct) {
            return;
        }
        this.hasProduct = hasProduct;
        if (!hasProduct) {
            tank.drain(tank.getCapacity(), IFluidHandler.FluidAction.EXECUTE);
        }
        setChanged();
        syncState();
    }

    public IFluidHandler fluids() {
        return tank;
    }

    public boolean harvestItem(Player player) {
        if (!hasProduct || species() != GtTreeSpecies.RUBBER) {
            return false;
        }
        ItemStack resin = new ItemStack(ModItems.RUBBER_RESIN.get());
        if (!player.getInventory().add(resin)) {
            player.drop(resin, false);
        }
        extractProduct();
        return true;
    }

    public void extractProduct() {
        setHasProduct(false);
    }

    public GtTreeSpecies species() {
        if (getBlockState().getBlock() instanceof GtTreeHoleBlock hole) {
            return hole.species();
        }
        return GtTreeSpecies.RUBBER;
    }

    private void fillProduct() {
        hasProduct = true;
        productFluid().ifPresent(fluid ->
                tank.setFluid(new FluidStack(fluid, GtTreeSpecies.HOLE_FLUID_MILLIBUCKETS)));
        setChanged();
        syncState();
    }

    private Optional<Fluid> productFluid() {
        if (species() != GtTreeSpecies.RAINBOWOOD) {
            return Optional.empty();
        }
        if (!BuiltInRegistries.FLUID.containsKey(RAINBOW_SAP)) {
            return Optional.empty();
        }
        Fluid fluid = BuiltInRegistries.FLUID.get(RAINBOW_SAP);
        if (fluid == null || fluid.defaultFluidState().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(fluid);
    }

    private boolean hasCanopy() {
        if (level == null) {
            return false;
        }
        GtTreeSpecies species = species();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int found = 0;
        for (int dy = 1; dy <= 12 && found < 8; dy++) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    cursor.set(worldPosition.getX() + dx, worldPosition.getY() + dy, worldPosition.getZ() + dz);
                    BlockState state = level.getBlockState(cursor);
                    if (state.getBlock() instanceof GtTreeLeavesBlock leaves
                            && leaves.species() == species) {
                        found++;
                    }
                    if (found >= 8) {
                        return true;
                    }
                }
            }
        }
        return found > 0;
    }

    private void syncState() {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        if (state.hasProperty(GtTreeHoleBlock.HAS_PRODUCT)
                && state.getValue(GtTreeHoleBlock.HAS_PRODUCT) != hasProduct) {
            level.setBlock(
                    worldPosition,
                    state.setValue(GtTreeHoleBlock.HAS_PRODUCT, hasProduct),
                    Block.UPDATE_CLIENTS);
        } else {
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("HasProduct", hasProduct);
        tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        hasProduct = tag.getBoolean("HasProduct");
        tank.readFromNBT(registries, tag.getCompound("tank"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

}
