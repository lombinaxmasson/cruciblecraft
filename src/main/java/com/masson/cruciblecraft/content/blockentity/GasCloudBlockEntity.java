package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.material.HydrocarbonRuntimePolicy;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Bounded transient gas parcel with generic rise, diffusion and ignition. */
public final class GasCloudBlockEntity extends BlockEntity {
    private static final Direction[] HORIZONTAL = {
        Direction.NORTH,
        Direction.EAST,
        Direction.SOUTH,
        Direction.WEST
    };
    private ResourceLocation materialId;
    private int amountMb;
    private int ageTicks;
    private boolean burning;

    public GasCloudBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GAS_CLOUD.get(), pos, state);
    }

    public static boolean canAccept(
            Level level, BlockPos pos, ResourceLocation materialId) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return true;
        }
        return state.is(ModBlocks.GAS_CLOUD.get())
                && level.getBlockEntity(pos)
                        instanceof GasCloudBlockEntity cloud
                && (cloud.materialId == null
                        || cloud.materialId.equals(materialId))
                && cloud.amountMb
                        < HydrocarbonRuntimePolicy.cloud()
                                .maximumEmittedMb();
    }

    public static int placeOrMerge(
            Level level,
            BlockPos pos,
            ResourceLocation materialId,
            int requestedMb) {
        if (requestedMb <= 0 || !canAccept(level, pos, materialId)) {
            return 0;
        }
        if (level.getBlockState(pos).isAir()) {
            level.setBlock(
                    pos,
                    ModBlocks.GAS_CLOUD.get().defaultBlockState(),
                    3);
        }
        if (!(level.getBlockEntity(pos)
                instanceof GasCloudBlockEntity cloud)) {
            return 0;
        }
        int accepted = Math.min(
                requestedMb,
                HydrocarbonRuntimePolicy.cloud().maximumEmittedMb()
                        - cloud.amountMb);
        if (accepted <= 0) {
            return 0;
        }
        if (cloud.materialId == null) {
            cloud.materialId = materialId;
        }
        cloud.amountMb += accepted;
        cloud.setChanged();
        return accepted;
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            GasCloudBlockEntity cloud) {
        if (cloud.materialId == null || cloud.amountMb <= 0) {
            level.removeBlock(pos, false);
            return;
        }
        cloud.ageTicks++;
        var policy = HydrocarbonRuntimePolicy.cloud();
        if (cloud.ageTicks >= policy.lifetimeTicks()) {
            level.removeBlock(pos, false);
            return;
        }
        if (!cloud.burning && cloud.adjacentIgnition(level, pos)) {
            cloud.ignite();
        }
        if (cloud.burning
                && cloud.ageTicks % policy.burnIntervalTicks() == 0) {
            cloud.amountMb -= Math.min(
                    cloud.amountMb, policy.burnConsumptionMb());
            level.getEntities(
                            (net.minecraft.world.entity.Entity) null,
                            new AABB(pos).inflate(0.25D),
                            entity -> entity.isAlive())
                    .forEach(entity -> entity.setRemainingFireTicks(
                            Math.max(entity.getRemainingFireTicks(), 40)));
            if (cloud.amountMb <= 0) {
                level.removeBlock(pos, false);
                return;
            }
            cloud.setChanged();
        }
        if (!cloud.burning
                && cloud.ageTicks % policy.movementIntervalTicks() == 0) {
            if (cloud.transfer(level, pos.above(), cloud.amountMb)) {
                return;
            }
            int start = Math.floorMod(
                    pos.getX() * 31 + pos.getZ() * 17 + cloud.ageTicks,
                    HORIZONTAL.length);
            for (int offset = 0;
                    offset < HORIZONTAL.length;
                    offset++) {
                Direction direction =
                        HORIZONTAL[(start + offset) % HORIZONTAL.length];
                int parcel = Math.min(
                        cloud.amountMb, policy.parcelMb());
                if (cloud.transfer(
                        level, pos.relative(direction), parcel)) {
                    break;
                }
            }
        }
    }

    private boolean transfer(
            Level level, BlockPos target, int requestedMb) {
        int accepted = placeOrMerge(
                level, target, materialId, requestedMb);
        if (accepted <= 0) {
            return false;
        }
        amountMb -= accepted;
        if (amountMb <= 0) {
            level.removeBlock(worldPosition, false);
        } else {
            setChanged();
        }
        return true;
    }

    private boolean adjacentIgnition(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockState adjacent =
                    level.getBlockState(pos.relative(direction));
            if (adjacent.is(Blocks.FIRE)
                    || adjacent.is(Blocks.SOUL_FIRE)
                    || adjacent.is(Blocks.LAVA)) {
                return true;
            }
        }
        return false;
    }

    public boolean ignite() {
        if (materialId == null
                || burning
                || !HydrocarbonRuntimePolicy.isFlammable(materialId)) {
            return false;
        }
        burning = true;
        setChanged();
        return true;
    }

    public ResourceLocation materialId() {
        return materialId;
    }

    public int amountMb() {
        return amountMb;
    }

    public boolean burning() {
        return burning;
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (materialId != null) {
            tag.putString("material_id", materialId.toString());
        }
        tag.putInt("amount_mb", amountMb);
        tag.putInt("age_ticks", ageTicks);
        tag.putBoolean("burning", burning);
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        String storedMaterial = tag.contains("material_id")
                ? tag.getString("material_id")
                : tag.getString("fluid_id");
        materialId = ResourceLocation.tryParse(storedMaterial);
        amountMb = Math.max(
                0,
                Math.min(
                        HydrocarbonRuntimePolicy.cloud()
                                .maximumEmittedMb(),
                        tag.getInt("amount_mb")));
        ageTicks = Math.max(0, tag.getInt("age_ticks"));
        burning = tag.getBoolean("burning")
                && materialId != null
                && HydrocarbonRuntimePolicy.isFlammable(materialId);
    }
}
