package com.masson.cruciblecraft.registry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.machine.processing.MachineKindSpec;
import com.masson.cruciblecraft.machine.processing.MachineKindCatalog;
import com.masson.cruciblecraft.machine.processing.MachineAcquisitionCatalog;
import com.masson.cruciblecraft.machine.processing.MachineTierCatalog;
import com.masson.cruciblecraft.machine.processing.MachineVariant;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;

import net.minecraft.resources.ResourceLocation;

/** Registry-time projection of the bundled T12/T16 machine-tier catalog.
 *  The public API is entries / variantsOf / require. There is no tierOf(kind, n)
 *  matrix completion. */
public final class ModMachineVariants {
    public static final List<MachineKindSpec> KINDS = kindsFromCatalog();
    private static final Map<ResourceLocation, MachineKindSpec> KIND_BY_ID =
            KINDS.stream().collect(
                    java.util.stream.Collectors.toUnmodifiableMap(
                            MachineKindSpec::id,
                            value -> value));
    public static final MachineKindSpec CENTRIFUGE =
            requireKind(id("centrifuge"));
    public static final MachineKindSpec SIFTER =
            requireKind(id("sifter"));
    public static final MachineKindSpec ELECTROLYZER =
            requireKind(id("electrolyzer"));
    public static final MachineKindSpec LATHE =
            requireKind(id("lathe"));
    public static final MachineKindSpec ROLLINGMILL =
            requireKind(id("rollingmill"));
    public static final MachineKindSpec WIREMILL =
            requireKind(id("wiremill"));
    public static final MachineKindSpec SHREDDER =
            requireKind(id("shredder"));
    public static final MachineKindSpec PRESS =
            requireKind(id("press"));
    public static final MachineKindSpec DISTILLERY =
            requireKind(id("distillery"));
    public static final MachineKindSpec DRYING =
            requireKind(id("drying"));
    public static final MachineKindSpec SMELTER =
            requireKind(id("smelter"));
    public static final List<MachineKindSpec> T16_SELECTED_KINDS =
            KINDS.stream()
                    .filter(kind -> "t16".equals(
                            MachineKindCatalog.require(kind.id()).displayGroup()))
                    .toList();
    public static final List<MachineKindSpec> T17_SELECTED_KINDS =
            KINDS.stream()
                    .filter(kind -> "t17".equals(
                            MachineKindCatalog.require(kind.id()).displayGroup()))
                    .toList();

    public static final List<MachineVariant> ALL =
            MachineTierCatalog.entries().stream()
                    .map(entry -> new MachineVariant(
                            entry.variantId(),
                            requireKind(entry.kindId()),
                            entry.tierBand()))
                    .toList();
    public static final List<MachineVariant> T16_SELECTED =
            ALL.stream()
                    .filter(variant ->
                            "t16".equals(MachineKindCatalog.require(
                                    variant.kind().id()).displayGroup())
                                    && MachineAcquisitionCatalog.isOpening(
                                            variant.id()))
                    .toList();
    public static final List<MachineVariant> T17_SELECTED =
            ALL.stream()
                    .filter(variant ->
                            "t17".equals(MachineKindCatalog.require(
                                    variant.kind().id()).displayGroup())
                                    && MachineAcquisitionCatalog.isOpening(
                                            variant.id()))
                    .toList();

    public static boolean isOpening(ResourceLocation id) {
        return MachineAcquisitionCatalog.isOpening(id);
    }
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

    /** Actual existing variants of one kind. Not a kind × tier completion. */
    public static List<MachineVariant> variantsOf(ResourceLocation kindId) {
        return forKind(kindId);
    }

    private static List<MachineKindSpec> kindsFromCatalog() {
        LinkedHashMap<ResourceLocation, MachineKindSpec> kinds =
                new LinkedHashMap<>();
        for (MachineTierCatalog.Entry entry : MachineTierCatalog.entries()) {
            kinds.computeIfAbsent(
                    entry.kindId(),
                    kindId -> sourceKind(behaviorOf(kindId)));
        }
        return List.copyOf(kinds.values());
    }

    private static ProcessingMachineSpec behaviorOf(ResourceLocation kindId) {
        if (kindId.equals(ModProcessingMachines.CRUSHER.id())) {
            return ModProcessingMachines.CRUSHER;
        }
        return ModProcessingMachines.require(kindId);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(
                com.masson.cruciblecraft.CrucibleCraft.MODID, path);
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
