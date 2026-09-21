package com.masson.cruciblecraft.content.blockentity;

import java.util.Optional;

import com.masson.cruciblecraft.content.block.GtTreeHoleBlock;
import com.masson.cruciblecraft.content.block.GtTreeLeavesBlock;
import com.masson.cruciblecraft.content.block.GtTreeLogBlock;
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
    public static final int RUBBER_LEAF_THRESHOLD = 60;
    public static final int RUBBER_REGEN_DENOMINATOR = 260;

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
        if (!hole.canRegenerate(level)) {
            return;
        }
        hole.fillProduct();
    }

    public boolean hasProduct() {
        return hasProduct;
    }

    public void setHasProduct(boolean hasProduct) {
        if (this.hasProduct == hasProduct) {
            if (hasProduct) {
                fillTank();
            }
            return;
        }
        this.hasProduct = hasProduct;
        if (hasProduct) {
            fillTank();
        } else {
            tank.drain(tank.getCapacity(), IFluidHandler.FluidAction.EXECUTE);
        }
        setChanged();
        syncState();
    }

    public IFluidHandler fluids() {
        return tank;
    }

    public boolean giveHarvestItem(Player player) {
        if (!hasProduct || species() != GtTreeSpecies.RUBBER) {
            return false;
        }
        ItemStack resin = new ItemStack(ModItems.RUBBER_RESIN.get());
        if (!player.getInventory().add(resin)) {
            player.drop(resin, false);
        }
        return true;
    }

    public boolean harvestItem(Player player) {
        if (!giveHarvestItem(player)) {
            return false;
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

    public int countRubberCanopyLeaves() {
        if (level == null || species() != GtTreeSpecies.RUBBER) {
            return 0;
        }
        int treeHeight = worldPosition.getY() + 1;
        for (int i = 1; i < 10; i++) {
            BlockState above = level.getBlockState(worldPosition.above(i));
            if (!isRubberLog(above)) {
                break;
            }
            treeHeight++;
        }
        int found = 0;
        if (isRubberLeaf(worldPosition.getX(), treeHeight, worldPosition.getZ())) {
            found++;
        }
        if (isRubberLeaf(worldPosition.getX(), treeHeight + 1, worldPosition.getZ())) {
            found++;
        }
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i != 0 || j != 0) {
                    if (isRubberLeaf(
                            worldPosition.getX() + i,
                            treeHeight - 1,
                            worldPosition.getZ() + j)) {
                        found++;
                    }
                }
            }
        }
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if (i == 0 && j == 0) {
                    continue;
                }
                if (Math.abs(i * j) < 2
                        && isRubberLeaf(
                                worldPosition.getX() + i,
                                treeHeight - 2,
                                worldPosition.getZ() + j)) {
                    found++;
                }
                if (Math.abs(i * j) < 4
                        && isRubberLeaf(
                                worldPosition.getX() + i,
                                treeHeight - 3,
                                worldPosition.getZ() + j)) {
                    found++;
                }
                if (Math.abs(i * j) < 4
                        && isRubberLeaf(
                                worldPosition.getX() + i,
                                treeHeight - 4,
                                worldPosition.getZ() + j)) {
                    found++;
                }
                if (isRubberLeaf(
                        worldPosition.getX() + i,
                        treeHeight - 5,
                        worldPosition.getZ() + j)) {
                    found++;
                }
            }
        }
        return found;
    }

    private void fillProduct() {
        hasProduct = true;
        fillTank();
        setChanged();
        syncState();
    }

    private void fillTank() {
        productFluid().ifPresent(fluid ->
                tank.setFluid(new FluidStack(fluid, GtTreeSpecies.HOLE_FLUID_MILLIBUCKETS)));
    }

    private Optional<Fluid> productFluid() {
        Optional<ResourceLocation> id = species().holeFluidId();
        if (id.isEmpty() || !BuiltInRegistries.FLUID.containsKey(id.get())) {
            return Optional.empty();
        }
        Fluid fluid = BuiltInRegistries.FLUID.get(id.get());
        if (fluid == null || fluid.defaultFluidState().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(fluid);
    }

    private boolean canRegenerate(Level level) {
        if (species() == GtTreeSpecies.RUBBER) {
            int leaves = countRubberCanopyLeaves();
            return leaves > RUBBER_LEAF_THRESHOLD
                    && level.random.nextInt(RUBBER_REGEN_DENOMINATOR)
                            < leaves - RUBBER_LEAF_THRESHOLD;
        }
        return hasCanopy();
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

    private boolean isRubberLog(BlockState state) {
        return state.getBlock() instanceof GtTreeLogBlock log
                && log.species() == GtTreeSpecies.RUBBER;
    }

    private boolean isRubberLeaf(int x, int y, int z) {
        if (level == null) {
            return false;
        }
        BlockState state = level.getBlockState(new BlockPos(x, y, z));
        return state.getBlock() instanceof GtTreeLeavesBlock leaves
                && leaves.species() == GtTreeSpecies.RUBBER;
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
        if (hasProduct && tank.isEmpty()) {
            fillTank();
        }
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
