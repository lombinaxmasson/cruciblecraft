package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.content.block.SpringLiquidBlock;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.worldgen.SpringFluidKind;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6 {@code MultiTileEntityFluidSpring}: {@code rng(amount)==0} produce into
 * the block above. Amount is the GT6 FluidStack quantity, not mB/t.
 */
public final class FluidSpringBlockEntity extends BlockEntity {
    private String fluidId = "minecraft:lava";
    private int amount = 1000;
    private boolean active;

    public FluidSpringBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_SPRING.get(), pos, state);
    }

    public String fluidId() {
        return fluidId;
    }

    public int amount() {
        return amount;
    }

    public boolean active() {
        return active;
    }

    public void configure(String fluidId, int amount) {
        this.fluidId = fluidId == null || fluidId.isEmpty() ? "minecraft:lava" : fluidId;
        this.amount = Math.max(1, amount);
        setChanged();
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            FluidSpringBlockEntity spring) {
        if (spring.amount <= 0) {
            spring.amount = 600;
        }
        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        boolean produce = false;
        if (spring.active) {
            produce = level.random.nextInt(spring.amount) == 0;
        } else if (level.getGameTime() % 20 == 1 && !aboveState.liquid()) {
            produce = spring.active = true;
            spring.setChanged();
        }
        if (produce) {
            spring.placeFluid(level, above, aboveState);
        }
    }

    public void dripOnce() {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockPos above = worldPosition.above();
        placeFluid(level, above, level.getBlockState(above));
    }

    private void placeFluid(Level level, BlockPos above, BlockState aboveState) {
        BlockState fill = sourceState();
        if (fill.getBlock() instanceof SpringLiquidBlock finite) {
            if (aboveState.is(finite)) {
                int next = SpringLiquidBlock.bind4(
                        aboveState.getValue(SpringLiquidBlock.META) + 8);
                level.setBlock(above, finite.withMeta(next), 3);
                finite.flowTick(level, above, level.random);
                return;
            }
            if (aboveState.isAir() || aboveState.liquid()) {
                level.setBlock(above, finite.dripState(), 3);
                finite.flowTick(level, above, level.random);
            }
            return;
        }
        if (aboveState.is(fill.getBlock())) {
            if (isSource(aboveState)) {
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    BlockPos side = above.relative(direction);
                    BlockState sideState = level.getBlockState(side);
                    if (sideState.is(fill.getBlock()) && !isSource(sideState)) {
                        level.setBlockAndUpdate(side, fill);
                        return;
                    }
                    if (sideState.isAir()) {
                        level.setBlockAndUpdate(side, fill);
                        return;
                    }
                }
            } else {
                level.setBlockAndUpdate(above, fill);
            }
            return;
        }
        if (aboveState.isAir() || aboveState.liquid()) {
            level.setBlockAndUpdate(above, fill);
        }
    }

    private BlockState sourceState() {
        return SpringFluidKind.byFluidId(ResourceLocation.parse(fluidId))
                .orElse(SpringFluidKind.LAVA)
                .sourceState();
    }

    private static boolean isSource(BlockState state) {
        return !state.hasProperty(LiquidBlock.LEVEL)
                || state.getValue(LiquidBlock.LEVEL) == 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("gt.spring", fluidId);
        tag.putInt("gt.spring.amount", amount);
        tag.putBoolean("gt.spring.active", active);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        String stored = tag.getString("gt.spring");
        if (!stored.isEmpty()) {
            fluidId = stored;
        }
        if (tag.contains("gt.spring.amount")) {
            amount = Math.max(1, tag.getInt("gt.spring.amount"));
        }
        active = tag.getBoolean("gt.spring.active");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
