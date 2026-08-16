package com.masson.cruciblecraft.energy.converter;

import java.util.List;
import java.util.Objects;

import net.minecraft.resources.ResourceLocation;

/** Immutable data projection for one fixed-source energy converter row. */
public record EnergyConverterProfile(
        ResourceLocation id,
        String stage,
        Status status,
        String runtimeBinding,
        Source source,
        List<String> accepts,
        List<String> emits,
        Packet inputPacket,
        Packet outputPacket,
        Window inputWindow,
        Integer efficiencyBps,
        String fuelMap,
        Conservation conservation,
        Exhaust exhaust,
        Faces faces,
        Policy policy,
        OutputSemantics outputSemantics,
        int inputCapacity,
        int outputCapacity) {
    public EnergyConverterProfile {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(stage, "stage");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(runtimeBinding, "runtimeBinding");
        Objects.requireNonNull(source, "source");
        accepts = List.copyOf(accepts);
        emits = List.copyOf(emits);
        Objects.requireNonNull(inputPacket, "inputPacket");
        Objects.requireNonNull(outputPacket, "outputPacket");
        Objects.requireNonNull(inputWindow, "inputWindow");
        Objects.requireNonNull(conservation, "conservation");
        Objects.requireNonNull(exhaust, "exhaust");
        Objects.requireNonNull(faces, "faces");
        Objects.requireNonNull(policy, "policy");
        if (source.sourceId() <= 0
                || source.sourceLine() <= 0
                || inputCapacity < 0
                || outputCapacity < 0) {
            throw new IllegalArgumentException(
                    "Converter source and capacities must be non-negative");
        }
        if (efficiencyBps != null
                && (efficiencyBps <= 0 || efficiencyBps > 10_000)) {
            throw new IllegalArgumentException(
                    "Converter efficiency must be in (0, 10000]");
        }
    }

    public enum Status {
        COMPLETE,
        PENDING
    }

    public record Source(
            String machineKind,
            int sourceId,
            int sourceLine,
            String materialExpression,
            String normalizedRowKey,
            String outputExpression,
            String efficiencyExpression,
            String fuelMap) {}

    public record Packet(
            String medium,
            String identity,
            long size,
            long maxAmountPerTick) {
        public Packet {
            if (size < 0L || maxAmountPerTick < 0L) {
                throw new IllegalArgumentException(
                        "Packet values must not be negative");
            }
        }
    }

    public record Window(Long minimum, Long nominal, Long maximum) {
        public Window {
            if ((minimum == null) != (nominal == null)
                    || (nominal == null) != (maximum == null)) {
                throw new IllegalArgumentException(
                        "Input window must be wholly specified or absent");
            }
            if (minimum != null
                    && (minimum < 0L
                            || minimum > nominal
                            || nominal > maximum)) {
                throw new IllegalArgumentException(
                        "Input window is not ordered");
            }
        }
    }

    public record Conservation(
            String primaryInput,
            int primaryInputUnits,
            String secondaryInput,
            int secondaryInputUnits,
            String output,
            int outputUnits,
            String exhaust,
            int exhaustUnits) {
        public Conservation {
            if (primaryInputUnits < 0
                    || secondaryInputUnits < 0
                    || outputUnits < 0
                    || exhaustUnits < 0) {
                throw new IllegalArgumentException(
                        "Conservation units must not be negative");
            }
        }
    }

    public record Exhaust(
            String identity,
            String mode,
            int capacity) {
        public Exhaust {
            if (capacity < 0) {
                throw new IllegalArgumentException(
                        "Exhaust capacity must not be negative");
            }
        }
    }

    public record Faces(
            List<String> energyInputs,
            List<String> energyOutputs,
            List<String> fluidInputs,
            List<String> fluidOutputs) {
        public Faces {
            energyInputs = List.copyOf(energyInputs);
            energyOutputs = List.copyOf(energyOutputs);
            fluidInputs = List.copyOf(fluidInputs);
            fluidOutputs = List.copyOf(fluidOutputs);
        }
    }

    public record Policy(
            boolean simulateBeforeExecute,
            boolean exactCommit,
            String blockage,
            String overflow,
            String sourceResolution) {}

    public record OutputSemantics(
            ConservationClassification conservation,
            SourceNominal sourceNominal,
            FixedOutput fixedOutput,
            Gt6RuntimeOutput gt6Runtime,
            List<String> sourceEvidencePaths) {
        public OutputSemantics {
            Objects.requireNonNull(conservation, "conservation");
            Objects.requireNonNull(sourceNominal, "sourceNominal");
            Objects.requireNonNull(fixedOutput, "fixedOutput");
            Objects.requireNonNull(gt6Runtime, "gt6Runtime");
            sourceEvidencePaths = List.copyOf(sourceEvidencePaths);
            if (sourceEvidencePaths.isEmpty()) {
                throw new IllegalArgumentException(
                        "Converter output semantics require source evidence");
            }
        }
    }

    public record ConservationClassification(
            String classification,
            int steamInputMb,
            int kuOutput,
            int steamMbPerKu) {}

    public record SourceNominal(
            String classification,
            int registeredNumerator,
            int steamPerEu,
            int mOutputKu) {}

    public record FixedOutput(
            String classification,
            int kuPerTick) {}

    public record Gt6RuntimeOutput(
            String classification,
            int minimumKuPerTick,
            int maximumKuPerTick,
            String behavior,
            String replacementCondition,
            String recheckPoint) {}
}
