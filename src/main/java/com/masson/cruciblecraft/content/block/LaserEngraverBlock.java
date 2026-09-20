package com.masson.cruciblecraft.content.block;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.content.blockentity.LaserEngraverBlockEntity;
import com.masson.cruciblecraft.machine.processing.MachineKindSpec;
import com.masson.cruciblecraft.machine.processing.MachineVariant;
import com.masson.cruciblecraft.machine.processing.TierProfile;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModMachineVariants;
import com.masson.cruciblecraft.registry.ModProcessingMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6 Laser Engraver 20321. Dedicated skipGeneric host; energy is lu_tier_1,
 * not the pre-catalog 2048 LU buffer.
 */
public final class LaserEngraverBlock extends ProcessingMachineBlock {
    public LaserEngraverBlock(Properties properties) {
        super(t1Variant(), properties);
    }

    /**
     * T1 is skipGeneric, so it is not in {@link ModMachineVariants#ALL}. The
     * kind and lu_tier_1 numbers still come from T2+ catalog rows / the same
     * machine_tiers band as aluminium+.
     */
    public static MachineVariant t1Variant() {
        MachineKindSpec kind = ModMachineVariants.forKind(
                        ModProcessingMachines.LASER_ENGRAVER.id())
                .getFirst()
                .kind();
        return new MachineVariant(
                ModProcessingMachines.LASER_ENGRAVER.id(),
                kind,
                new TierProfile(
                        ResourceLocation.parse("cruciblecraft:lu_tier_1"),
                        "cruciblecraft:steel_galvanized",
                        EnergyType.LU,
                        16L,
                        32L,
                        64L,
                        64L,
                        1,
                        10_000));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LaserEngraverBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            net.minecraft.world.level.Level level,
            BlockState state,
            BlockEntityType<T> type) {
        return !level.isClientSide
                && type == ModBlockEntities.LASER_ENGRAVER.get()
                ? (world, pos, blockState, blockEntity) ->
                        LaserEngraverBlockEntity.serverTick(
                                world,
                                pos,
                                blockState,
                                (LaserEngraverBlockEntity) blockEntity)
                : null;
    }
}
