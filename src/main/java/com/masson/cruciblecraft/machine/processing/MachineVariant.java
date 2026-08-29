package com.masson.cruciblecraft.machine.processing;

import java.util.List;
import java.util.Objects;

import com.masson.cruciblecraft.registry.ModProcessingMachines;

import net.minecraft.resources.ResourceLocation;

/** Immutable complete identity binding one kind to one numeric tier band. */
public final class MachineVariant {
    private final ResourceLocation id;
    private final MachineKindSpec kind;
    private final TierProfile tierBand;
    private final ProcessingMachineSpec runtimeSpec;

    public MachineVariant(
            ResourceLocation id,
            MachineKindSpec kind,
            TierProfile tierBand) {
        this(
                id,
                kind,
                tierBand,
                createRuntimeSpec(id, kind, tierBand));
    }

    private MachineVariant(
            ResourceLocation id,
            MachineKindSpec kind,
            TierProfile tierBand,
            ProcessingMachineSpec runtimeSpec) {
        this.id = Objects.requireNonNull(id, "id");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.tierBand = Objects.requireNonNull(tierBand, "tierBand");
        if (kind.behavior().energy().type() != tierBand.energyType()) {
            throw new IllegalArgumentException(
                    "Tier-band energy identity "
                            + tierBand.energyType()
                            + " does not match kind "
                            + kind.id()
                            + " ("
                            + kind.behavior().energy().type()
                            + ")");
        }
        this.runtimeSpec = Objects.requireNonNull(
                runtimeSpec, "runtimeSpec");
    }

    public ResourceLocation id() {
        return id;
    }

    public MachineKindSpec kind() {
        return kind;
    }

    public TierProfile tierBand() {
        return tierBand;
    }

    /**
     * @deprecated Use {@link #tierBand()} to distinguish the shared band from
     *     the complete variant identity returned by {@link #id()}.
     */
    @Deprecated(forRemoval = false)
    public TierProfile tier() {
        return tierBand;
    }

    public ProcessingMachineSpec runtimeSpec() {
        return runtimeSpec;
    }

    public static MachineVariant legacy(ProcessingMachineSpec spec) {
        Objects.requireNonNull(spec, "spec");
        long nominal = Math.max(1L, spec.energy().maxPacket());
        long capacity = Math.max(
                nominal,
                spec.energy().mode() == ProcessingMachineSpec.EnergyMode.BUFFERED
                        ? spec.energy().capacity()
                        : nominal);
        TierProfile tierBand = new TierProfile(
                ResourceLocation.fromNamespaceAndPath(
                        spec.id().getNamespace(),
                        "legacy/" + spec.id().getPath()),
                "cruciblecraft:legacy",
                spec.energy().type(),
                Math.max(1L, nominal / 2L),
                nominal,
                nominal,
                capacity,
                1,
                10_000);
        return new MachineVariant(
                spec.id(),
                new MachineKindSpec(
                        spec.id(),
                        spec,
                        MachineKindSpec.OverclockPolicy.LEGACY_TICKS,
                        false),
                tierBand,
                spec);
    }

    private static ProcessingMachineSpec createRuntimeSpec(
            ResourceLocation id,
            MachineKindSpec kind,
            TierProfile tierBand) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(tierBand, "tierBand");
        ProcessingMachineSpec base = kind.behavior();
        ProcessingMachineSpec.EnergySpec energy = new ProcessingMachineSpec.EnergySpec(
                tierBand.energyType(),
                base.energy().mode(),
                base.energy().mode()
                                == ProcessingMachineSpec.EnergyMode.BUFFERED
                        ? tierBand.energyCapacity()
                        : 0L,
                tierBand.inputMaximum());
        if (usesT39CentrifugeEnvelope(id, kind)) {
            return t39CentrifugeVariantSpec(id, base, energy);
        }
        return new ProcessingMachineSpec(
                id,
                base.recipeMapId(),
                base.recipeMap(),
                base.items(),
                base.fluids(),
                energy,
                base.sidedIo(),
                base.validator(),
                base.buffering(),
                base.ui());
    }

    /**
     * Steel/titanium/tungstensteel single-block centrifuges execute the GT6
     * 6-fluid-out / 100,000 mB envelope. Bronze {@code cruciblecraft:centrifuge}
     * and the T15 large controller keep the T5 2-out / 4,000 mB host.
     */
    private static boolean usesT39CentrifugeEnvelope(
            ResourceLocation id,
            MachineKindSpec kind) {
        return "centrifuge".equals(kind.id().getPath()) && !id.equals(kind.id());
    }

    private static ProcessingMachineSpec t39CentrifugeVariantSpec(
            ResourceLocation id,
            ProcessingMachineSpec base,
            ProcessingMachineSpec.EnergySpec energy) {
        int itemInputs = base.items().inputs().size();
        int itemOutputs = base.items().outputs().size();
        int fluidInputs = 1;
        int fluidOutputs = 6;
        List<ProcessingMachineSpec.TankSpec> inputTanks =
                java.util.stream.IntStream.range(0, fluidInputs)
                        .mapToObj(index -> new ProcessingMachineSpec.TankSpec(
                                index, 100_000))
                        .toList();
        List<ProcessingMachineSpec.TankSpec> outputTanks =
                java.util.stream.IntStream.range(
                                fluidInputs, fluidInputs + fluidOutputs)
                        .mapToObj(index -> new ProcessingMachineSpec.TankSpec(
                                index, 8_000))
                        .toList();
        return new ProcessingMachineSpec(
                id,
                base.recipeMapId(),
                base.recipeMap(),
                base.items(),
                new ProcessingMachineSpec.TankLayout(inputTanks, outputTanks),
                energy,
                base.sidedIo(),
                ModProcessingMachines::validateT39CentrifugeEnvelope,
                base.buffering(),
                Gt6BasicMachineGui.ui(
                        1, 6, 1, 6,
                        itemInputs, itemOutputs, fluidInputs, fluidOutputs,
                        base.ui().statuses()));
    }
}
