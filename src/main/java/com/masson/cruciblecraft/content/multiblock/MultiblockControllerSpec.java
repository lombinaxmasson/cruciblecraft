package com.masson.cruciblecraft.content.multiblock;

import java.util.Objects;
import java.util.function.Supplier;

import com.masson.cruciblecraft.machine.processing.MachineVariant;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;

import net.minecraft.resources.ResourceLocation;

/**
 * Registry-safe binding between data-owned geometry and a shared processing
 * host. The supplier boundary lets tier/variant work replace the runtime spec
 * without introducing structure-specific Java.
 */
public record MultiblockControllerSpec(
        ResourceLocation id,
        ResourceLocation structureId,
        ResourceLocation recipeMapId,
        Supplier<ProcessingMachineSpec> processingSpec,
        java.util.Optional<Supplier<MachineVariant>> variant) {
    public MultiblockControllerSpec(
            ResourceLocation id,
            ResourceLocation structureId,
            ResourceLocation recipeMapId,
            Supplier<ProcessingMachineSpec> processingSpec) {
        this(
                id,
                structureId,
                recipeMapId,
                processingSpec,
                java.util.Optional.empty());
    }

    public MultiblockControllerSpec {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(structureId, "structureId");
        Objects.requireNonNull(recipeMapId, "recipeMapId");
        Objects.requireNonNull(processingSpec, "processingSpec");
        variant = Objects.requireNonNull(variant, "variant");
    }

    public ProcessingMachineSpec requireProcessingSpec() {
        ProcessingMachineSpec spec = Objects.requireNonNull(
                processingSpec.get(), "processingSpec supplier");
        if (!recipeMapId.equals(spec.recipeMapId())) {
            throw new IllegalStateException(
                    "Multiblock " + id + " expected recipe map " + recipeMapId
                            + " but received " + spec.recipeMapId());
        }
        return spec;
    }

    public MachineVariant requireVariant() {
        MachineVariant resolved = variant.isPresent()
                ? Objects.requireNonNull(
                        variant.orElseThrow().get(), "variant supplier")
                : MachineVariant.legacy(requireProcessingSpec());
        ProcessingMachineSpec runtime = resolved.runtimeSpec();
        if (!recipeMapId.equals(runtime.recipeMapId())) {
            throw new IllegalStateException(
                    "Multiblock " + id + " variant expected recipe map "
                            + recipeMapId + " but received "
                            + runtime.recipeMapId());
        }
        return resolved;
    }

    public MultiblockControllerSpec withVariant(
            Supplier<MachineVariant> variant) {
        Objects.requireNonNull(variant, "variant");
        return new MultiblockControllerSpec(
                id,
                structureId,
                recipeMapId,
                processingSpec,
                java.util.Optional.of(variant));
    }
}
