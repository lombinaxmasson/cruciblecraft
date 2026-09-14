package com.masson.cruciblecraft.logistics.pipe.fluid;

import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.material.ChemicalFluidRegistrationGate;
import com.masson.cruciblecraft.material.def.GT6MaterialMetadata.FluidPipeProperties;
import com.masson.cruciblecraft.registry.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * GT6 {@code MultiTileEntityPipeFluid} plasma/magic trash, contact heat, and
 * flammable adjacent fire. Magic 1% replace uses air because Thaumcraft flux
 * blocks are not in CC.
 */
public final class FluidPipeDangerousMedia {
    public static final int PLASMA_TRASH = 64;
    public static final int MAGIC_TRASH_GAS = 16;
    public static final int MAGIC_TRASH_LIQUID = 4;

    private FluidPipeDangerousMedia() {}

    /**
     * @return {@code true} if the pipe block was destroyed
     */
    public static boolean tick(Level level, BlockPos pos, FluidPipeBlockEntity pipe) {
        return tick(level, pos, pipe, true);
    }

    /**
     * Deterministic trash/ignite for GameTests. Skips the GT6 1% replace.
     */
    public static boolean tickDeterministic(
            Level level, BlockPos pos, FluidPipeBlockEntity pipe) {
        return tick(level, pos, pipe, false);
    }

    public static void contact(Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide
                || !(entity instanceof LivingEntity living)
                || !(level.getBlockEntity(pos)
                        instanceof FluidPipeBlockEntity pipe)) {
            return;
        }
        FluidPipeProperties properties = pipe.pipeBlock().pipe().fluid();
        if (!properties.contactDamage() || pipe.tanksEmpty()) {
            return;
        }
        living.hurt(level.damageSources().magic(), 1.0F);
    }

    private static boolean tick(
            Level level,
            BlockPos pos,
            FluidPipeBlockEntity pipe,
            boolean allowRandomReplace) {
        FluidPipeProperties properties = pipe.pipeBlock().pipe().fluid();
        for (FluidTank tank : pipe.tanks()) {
            if (applyTank(
                    level, pos, pipe, properties, tank, allowRandomReplace)) {
                return true;
            }
        }
        return false;
    }

    private static boolean applyTank(
            Level level,
            BlockPos pos,
            FluidPipeBlockEntity pipe,
            FluidPipeProperties properties,
            FluidTank tank,
            boolean allowRandomReplace) {
        FluidStack stack = tank.getFluid();
        if (stack.isEmpty()) {
            return false;
        }
        FluidPipeBlockedMedia.Kind kind = FluidPipeBlockedMedia.kindOf(stack);
        if (kind == FluidPipeBlockedMedia.Kind.MAGIC && !properties.magicProof()) {
            int trash = isGas(stack) ? MAGIC_TRASH_GAS : MAGIC_TRASH_LIQUID;
            tank.drain(trash, IFluidHandler.FluidAction.EXECUTE);
            fizz(level, pos);
            poisonNear(level, pos);
            if (allowRandomReplace && level.random.nextInt(100) == 0) {
                pipe.trashContents();
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                return true;
            }
        }
        if (kind == FluidPipeBlockedMedia.Kind.PLASMA
                && !properties.plasmaProof()) {
            tank.drain(PLASMA_TRASH, IFluidHandler.FluidAction.EXECUTE);
            fizz(level, pos);
            heatNear(level, pos, 2);
        }
        int temperature = stack.getFluidType().getTemperature(stack);
        if (temperature > properties.maxTemperatureKelvin()) {
            igniteAdjacent(level, pos);
            if (allowRandomReplace
                    && properties.flammable()
                    && level.random.nextInt(100) == 0) {
                pipe.trashContents();
                level.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
                return true;
            }
        }
        return false;
    }

    private static boolean isGas(FluidStack stack) {
        try {
            return ModFluids.chemicalState(stack.getFluid())
                    .filter(state -> state
                            == ChemicalFluidRegistrationGate.State.GAS)
                    .isPresent();
        } catch (IllegalStateException ignored) {
            return false;
        }
    }

    private static void fizz(Level level, BlockPos pos) {
        level.playSound(
                null,
                pos,
                SoundEvents.FIRE_EXTINGUISH,
                SoundSource.BLOCKS,
                0.5F,
                1.0F);
    }

    private static void poisonNear(Level level, BlockPos pos) {
        AABB box = new AABB(pos).inflate(3.0D);
        for (LivingEntity entity : level.getEntitiesOfClass(
                LivingEntity.class, box)) {
            entity.addEffect(new MobEffectInstance(MobEffects.POISON, 1200, 1));
        }
    }

    private static void heatNear(Level level, BlockPos pos, int radius) {
        AABB box = new AABB(pos).inflate(radius);
        for (LivingEntity entity : level.getEntitiesOfClass(
                LivingEntity.class, box)) {
            entity.hurt(level.damageSources().magic(), 2.0F);
        }
    }

    private static void igniteAdjacent(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            if (level.getBlockState(neighbor).isAir()) {
                level.setBlock(neighbor, Blocks.FIRE.defaultBlockState(), 3);
            }
        }
    }
}
