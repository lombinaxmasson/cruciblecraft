package com.masson.cruciblecraft.registry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.masson.cruciblecraft.machine.processing.MachineKindSpec;
import com.masson.cruciblecraft.machine.processing.MachineTierCatalog;
import com.masson.cruciblecraft.machine.processing.MachineVariant;

import net.minecraft.resources.ResourceLocation;

/** Registry-time projection of the bundled T12/T16 machine-tier catalog. */
public final class ModMachineVariants {
    public static final MachineKindSpec CENTRIFUGE =
            sourceKind(ModProcessingMachines.CENTRIFUGE);
    public static final MachineKindSpec SIFTER =
            sourceKind(ModProcessingMachines.SIFTER);
    public static final MachineKindSpec ELECTROLYZER =
            sourceKind(ModProcessingMachines.ELECTROLYZER);
    public static final MachineKindSpec LATHE =
            sourceKind(ModProcessingMachines.LATHE);
    public static final MachineKindSpec ROLLINGMILL =
            sourceKind(ModProcessingMachines.ROLLINGMILL);
    public static final MachineKindSpec WIREMILL =
            sourceKind(ModProcessingMachines.WIREMILL);
    public static final MachineKindSpec SHREDDER =
            sourceKind(ModProcessingMachines.SHREDDER);
    public static final MachineKindSpec PRESS =
            sourceKind(ModProcessingMachines.PRESS);
    public static final MachineKindSpec DISTILLERY =
            sourceKind(ModProcessingMachines.DISTILLERY);
    public static final MachineKindSpec DRYING =
            sourceKind(ModProcessingMachines.DRYING);
    public static final MachineKindSpec SMELTER =
            sourceKind(ModProcessingMachines.SMELTER);
    public static final List<MachineKindSpec> KINDS =
            List.of(
                    CENTRIFUGE,
                    SIFTER,
                    ELECTROLYZER,
                    LATHE,
                    ROLLINGMILL,
                    WIREMILL,
                    SHREDDER,
                    PRESS,
                    DISTILLERY,
                    DRYING,
                    SMELTER);
    public static final List<MachineKindSpec> T16_SELECTED_KINDS =
            List.of(LATHE, ROLLINGMILL, WIREMILL, SHREDDER, PRESS);
    private static final Set<ResourceLocation> T16_SELECTED_KIND_IDS =
            T16_SELECTED_KINDS.stream()
                    .map(MachineKindSpec::id)
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
    public static final List<MachineKindSpec> T17_SELECTED_KINDS =
            List.of(DISTILLERY, DRYING, SMELTER);
    private static final Set<ResourceLocation> T17_SELECTED_KIND_IDS =
            T17_SELECTED_KINDS.stream()
                    .map(MachineKindSpec::id)
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());

    private static final Map<ResourceLocation, MachineKindSpec> KIND_BY_ID =
            KINDS.stream().collect(
                    java.util.stream.Collectors.toUnmodifiableMap(
                            MachineKindSpec::id,
                            value -> value));
    public static final List<MachineVariant> ALL =
            MachineTierCatalog.entries().stream()
                    .map(entry -> new MachineVariant(
                            entry.variantId(),
                            requireKind(entry.kindId()),
                            entry.tier()))
                    .toList();
    public static final List<MachineVariant> T16_SELECTED =
            ALL.stream()
                    .filter(variant ->
                            T16_SELECTED_KIND_IDS.contains(variant.kind().id()))
                    .toList();
    public static final List<MachineVariant> T17_SELECTED =
            ALL.stream()
                    .filter(variant ->
                            T17_SELECTED_KIND_IDS.contains(
                                    variant.kind().id()))
                    .toList();
    private static final Map<ResourceLocation, MachineVariant> BY_ID =
            indexById();
    private static final Map<ResourceLocation, List<MachineVariant>> BY_KIND =
            ALL.stream().collect(java.util.stream.Collectors.groupingBy(
                    variant -> variant.kind().id(),
                    java.util.stream.Collectors.toUnmodifiableList()));

    public static MachineVariant require(ResourceLocation id) {
        MachineVariant variant = BY_ID.get(id);
        if (variant == null) {
            throw new IllegalArgumentException(
                    "Unknown machine variant " + id);
        }
        return variant;
    }

    public static Optional<MachineVariant> find(ResourceLocation id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    public static List<MachineVariant> forKind(ResourceLocation id) {
        return BY_KIND.getOrDefault(id, List.of());
    }

    private static MachineKindSpec sourceKind(
            com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec
                    behavior) {
        List<MachineTierCatalog.Entry> rows =
                MachineTierCatalog.entries().stream()
                        .filter(entry -> entry.kindId().equals(behavior.id()))
                        .toList();
        if (rows.isEmpty()) {
            throw new IllegalStateException(
                    "Machine tier catalog has no rows for " + behavior.id());
        }
        MachineTierCatalog.Entry first = rows.getFirst();
        boolean policyDrift = rows.stream().anyMatch(entry ->
                entry.overclockPolicy() != first.overclockPolicy()
                        || entry.parallelDuration()
                                != first.parallelDuration());
        if (policyDrift) {
            throw new IllegalStateException(
                    "Machine tier source policy differs within "
                            + behavior.id());
        }
        return new MachineKindSpec(
                behavior.id(),
                behavior,
                first.overclockPolicy(),
                first.parallelDuration());
    }

    private static MachineKindSpec requireKind(ResourceLocation id) {
        MachineKindSpec kind = KIND_BY_ID.get(id);
        if (kind == null) {
            throw new IllegalStateException(
                    "Machine tier catalog references unknown kind " + id);
        }
        return kind;
    }

    private static Map<ResourceLocation, MachineVariant> indexById() {
        LinkedHashMap<ResourceLocation, MachineVariant> indexed =
                new LinkedHashMap<>();
        for (MachineVariant variant : ALL) {
            if (indexed.putIfAbsent(variant.id(), variant) != null) {
                throw new IllegalStateException(
                        "Duplicate machine variant " + variant.id());
            }
        }
        return Map.copyOf(indexed);
    }

    private ModMachineVariants() {}
}
