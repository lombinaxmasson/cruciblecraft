package com.masson.cruciblecraft.logistics.pipe.fluid;

import com.masson.cruciblecraft.material.def.GT6MaterialMetadata.FluidPipeProperties;
import com.masson.cruciblecraft.registry.ModFluids;

import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Classifies plasma / magic fluids so {@link FluidPipeDangerousMedia} can
 * trash them after fill. They are not mapped onto gas leak or acid
 * corrosion. {@code rejects} remains the classify helper; fill no longer
 * uses it as a gate.
 */
public final class FluidPipeBlockedMedia {
    public enum Kind {
        NONE,
        PLASMA,
        MAGIC
    }

    private FluidPipeBlockedMedia() {}

    public static boolean rejects(FluidStack stack, FluidPipeProperties properties) {
        return classify(stack, properties) != Kind.NONE;
    }

    public static Kind classify(
            FluidStack stack, FluidPipeProperties properties) {
        if (stack == null || stack.isEmpty() || properties == null) {
            return Kind.NONE;
        }
        Kind kind = kindOf(stack);
        if (kind == Kind.PLASMA && !properties.plasmaProof()) {
            return Kind.PLASMA;
        }
        if (kind == Kind.MAGIC && !properties.magicProof()) {
            return Kind.MAGIC;
        }
        return Kind.NONE;
    }

    public static Kind kindOf(FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Kind.NONE;
        }
        try {
            var material = ModFluids.material(stack.getFluid());
            if (material.isEmpty()) {
                return Kind.NONE;
            }
            var metadata = material.orElseThrow().gt6Metadata();
            if (metadata.isEmpty()) {
                return Kind.NONE;
            }
            var facts = metadata.orElseThrow();
            if (facts.materialTags().contains("PROPERTIES.PLASMA")
                    || "plasma".equals(facts.state())) {
                return Kind.PLASMA;
            }
            if (facts.materialTags().contains("PROPERTIES.MAGICAL")) {
                return Kind.MAGIC;
            }
        } catch (IllegalStateException ignored) {
            // Registry lookup is unavailable in isolated unit tests.
        }
        return Kind.NONE;
    }
}
