package com.masson.cruciblecraft.machine.processing;

import java.util.Objects;

import net.minecraft.resources.ResourceLocation;

/** Immutable binding of one behavior kind to one numeric tier profile. */
public final class MachineVariant {
    private final ResourceLocation id;
    private final MachineKindSpec kind;
    private final TierProfile tier;
    private final ProcessingMachineSpec runtimeSpec;

    public MachineVariant(
            ResourceLocation id,
            MachineKindSpec kind,
            TierProfile tier) {
        this(id, kind, tier, createRuntimeSpec(id, kind, tier));
    }

    private MachineVariant(
            ResourceLocation id,
            MachineKindSpec kind,
            TierProfile tier,
            ProcessingMachineSpec runtimeSpec) {
        this.id = Objects.requireNonNull(id, "id");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.tier = Objects.requireNonNull(tier, "tier");
        if (kind.behavior().energy().type() != tier.energyType()) {
            throw new IllegalArgumentException(
                    "Tier energy identity "
                            + tier.energyType()
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

    public TierProfile tier() {
        return tier;
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
        TierProfile tier = new TierProfile(
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
                tier,
                spec);
    }

    private static ProcessingMachineSpec createRuntimeSpec(
            ResourceLocation id,
            MachineKindSpec kind,
            TierProfile tier) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(tier, "tier");
        ProcessingMachineSpec base = kind.behavior();
        return new ProcessingMachineSpec(
                id,
                base.recipeMapId(),
                base.recipeMap(),
                base.items(),
                base.fluids(),
                new ProcessingMachineSpec.EnergySpec(
                        tier.energyType(),
                        base.energy().mode(),
                        base.energy().mode()
                                        == ProcessingMachineSpec.EnergyMode.BUFFERED
                                ? tier.energyCapacity()
                                : 0L,
                        tier.inputMaximum()),
                base.sidedIo(),
                base.validator(),
                base.buffering(),
                base.ui());
    }
}
