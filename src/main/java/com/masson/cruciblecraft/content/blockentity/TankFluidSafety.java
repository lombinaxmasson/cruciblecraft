package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.content.multiblock.TankControllerProfiles.Profile;
import com.masson.cruciblecraft.logistics.pipe.fluid.FluidPipeBlockedMedia;
import com.masson.cruciblecraft.material.GT6ImportUnits;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.registry.ModFluids;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/** GT6 {@code MultiTileEntityTank.allowFluid} and hazard classification. */
public final class TankFluidSafety {
    private TankFluidSafety() {}

    public static Failure allowedFailure(Profile profile, FluidStack stack) {
        if (profile == null || stack == null || stack.isEmpty()) {
            return Failure.NOT_SIMPLE;
        }
        if (isPowerConducting(stack)) {
            return Failure.POWER_CONDUCTING;
        }
        if (temperature(stack) >= meltingPointKelvin(profile)) {
            return Failure.TOO_HOT;
        }
        if (profile.onlySimple() && !isSimple(stack.getFluid())) {
            return Failure.NOT_SIMPLE;
        }
        return Failure.NONE;
    }

    public static Failure hazard(Profile profile, FluidStack stack) {
        if (profile == null || stack == null || stack.isEmpty()) {
            return Failure.NONE;
        }
        if (isMagic(stack) && !profile.magicProof()) {
            return Failure.MAGIC;
        }
        if (isAcid(stack) && !profile.acidProof()) {
            return Failure.ACID;
        }
        if (isPlasma(stack) && !profile.plasmaProof()) {
            return Failure.PLASMA;
        }
        if (isGas(stack) && !profile.gasProof()) {
            return Failure.GAS;
        }
        return Failure.NONE;
    }

    public static boolean isGas(FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        try {
            if (ModFluids.chemicalState(stack.getFluid()).orElse(null)
                    == com.masson.cruciblecraft.material.ChemicalFluidRegistrationGate.State.GAS) {
                return true;
            }
        } catch (IllegalStateException ignored) {
            // Registry lookup may not be finalized in isolated tests.
        }
        try {
            if (ModFluids.hotId(stack.getFluid()).isPresent()) {
                return ModFluids.hot(
                                ModFluids.hotId(stack.getFluid()).orElseThrow())
                        .map(entry -> entry.state()
                                == com.masson.cruciblecraft.material.HotFluidRegistrationGate.State.GAS)
                        .orElse(false);
            }
        } catch (IllegalStateException ignored) {
            // Registry lookup may not be finalized in isolated tests.
        }
        return stack.getFluid().getFluidType().getDensity(stack) < 0;
    }

    public static boolean isLighter(FluidStack stack) {
        return stack != null
                && !stack.isEmpty()
                && stack.getFluid().getFluidType().getDensity(stack) < 0;
    }

    public static int temperature(FluidStack stack) {
        return stack == null || stack.isEmpty()
                ? 300
                : stack.getFluidType().getTemperature(stack);
    }

    private static long meltingPointKelvin(Profile profile) {
        try {
            MaterialDefinition material =
                    MaterialCatalog.require(profile.materialId());
            return GT6ImportUnits.celsiusToRoundedKelvin(
                    material.thermal().meltingPoint());
        } catch (IllegalStateException | IllegalArgumentException ignored) {
            return Long.MAX_VALUE;
        }
    }

    private static boolean isPowerConducting(FluidStack stack) {
        return isPlasma(stack) || conductsPower(stack);
    }

    /**
     * GT6 {@code FL.powerconducting}: steam and the power-conducting material
     * tag. Plasma is a separate hazard; a plasma-proof drum still holds plasma
     * that is not itself power-conducting.
     */
    public static boolean conductsPower(FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Fluid fluid = stack.getFluid();
        ResourceName name = new ResourceName(BuiltInRegistries.FLUID.getKey(fluid));
        if ("steam".equals(name.path())
                || "flowing_steam".equals(name.path())) {
            return true;
        }
        try {
            return ModFluids.material(fluid)
                    .map(material -> material.hasMaterialTag(
                            "PROPERTIES.POWER_CONDUCTING"))
                    .orElse(false);
        } catch (IllegalStateException ignored) {
            return false;
        }
    }

    public static boolean isAcid(FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        try {
            return ModFluids.material(stack.getFluid())
                    .map(material -> material.hasMaterialTag("PROPERTIES.ACID"))
                    .orElse(false);
        } catch (IllegalStateException ignored) {
            return false;
        }
    }

    public static boolean isPlasma(FluidStack stack) {
        return stack != null
                && !stack.isEmpty()
                && FluidPipeBlockedMedia.kindOf(stack)
                        == FluidPipeBlockedMedia.Kind.PLASMA;
    }

    public static boolean isMagic(FluidStack stack) {
        return stack != null
                && !stack.isEmpty()
                && FluidPipeBlockedMedia.kindOf(stack)
                        == FluidPipeBlockedMedia.Kind.MAGIC;
    }

    private static boolean isSimple(Fluid fluid) {
        if (fluid == Fluids.WATER
                || fluid == Fluids.FLOWING_WATER
                || fluid == Fluids.LAVA
                || fluid == Fluids.FLOWING_LAVA
                || fluid == ModFluids.CREOSOTE_SOURCE.get()
                || fluid == ModFluids.CREOSOTE_FLOWING.get()) {
            return true;
        }
        return fluid.defaultFluidState().is(FluidTags.WATER);
    }

    public enum Failure {
        NONE,
        POWER_CONDUCTING,
        TOO_HOT,
        NOT_SIMPLE,
        GAS,
        ACID,
        PLASMA,
        MAGIC
    }

    /**
     * Small wrapper so registry names stay null-safe when a fluid is being
     * inspected before all deferred registers have completed.
     */
    private record ResourceName(String namespace, String path) {
        private ResourceName(net.minecraft.resources.ResourceLocation id) {
            this(
                    id == null ? "" : id.getNamespace(),
                    id == null ? "" : id.getPath());
        }
    }
}
