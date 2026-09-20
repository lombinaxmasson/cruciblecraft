package com.masson.cruciblecraft.logistics.pipe.cover;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.blockentity.CableBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ItemPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.RedstoneWireBlockEntity;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverHost;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** GT6 {@code CoverAsphalt}: 1.3× horizontal motion while walking on the top face. */
@EventBusSubscriber(modid = CrucibleCraft.MODID)
public final class CoverWalkEvents {
    private CoverWalkEvents() {}

    @SubscribeEvent
    public static void speedUpAsphalt(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living)
                || !living.onGround()
                || living.isInWater()
                || living.isShiftKeyDown()) {
            return;
        }
        Vec3 movement = living.getDeltaMovement();
        if (movement.x == 0.0 && movement.z == 0.0) {
            return;
        }
        BlockPos pos = living.getBlockPosBelowThatAffectsMyMovement();
        if (!DecorativeCovers.isAsphalt(coverOn(living.level().getBlockEntity(pos), Direction.UP))) {
            return;
        }
        living.setDeltaMovement(movement.x * 1.3, movement.y, movement.z * 1.3);
    }

    private static PipeCover coverOn(BlockEntity blockEntity, Direction side) {
        if (blockEntity instanceof MachineCoverHost host) {
            return host.covers().get(side).orElse(null);
        }
        if (blockEntity instanceof ItemPipeBlockEntity pipe) {
            return pipe.coverSnapshot().get(side);
        }
        if (blockEntity instanceof FluidPipeBlockEntity pipe) {
            return pipe.coverSnapshot().get(side);
        }
        if (blockEntity instanceof RedstoneWireBlockEntity wire) {
            return wire.covers().get(side).orElse(null);
        }
        if (blockEntity instanceof CableBlockEntity cable) {
            return cable.covers().get(side).orElse(null);
        }
        return null;
    }
}
