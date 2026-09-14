package com.masson.cruciblecraft.energy.cable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.GT6MaterialMetadata.ElectricalProperties;
import com.masson.cruciblecraft.material.def.MaterialDefinition;

/**
 * Startup-frozen mapping from registered material forms to exact GT6
 * electrical specifications.
 */
public final class ElectricalConductorCatalog {
    public static final int EXPECTED_CABLE_BLOCKS = 116;
    public static final int EXPECTED_WIRE_BLOCKS = 445;
    private static final Set<String> REDSTONE_MATERIALS =
            Set.of("red_alloy", "signalum", "lumium");

    private static final Map<MaterialPrefix, String> SPECIFICATION_BY_FORM =
            Map.ofEntries(
                    Map.entry(MaterialPrefixes.WIRE, "wireGt01"),
                    Map.entry(MaterialPrefixes.DOUBLE_WIRE, "wireGt02"),
                    Map.entry(MaterialPrefixes.TRIPLE_WIRE, "wireGt03"),
                    Map.entry(MaterialPrefixes.QUADRUPLE_WIRE, "wireGt04"),
                    Map.entry(MaterialPrefixes.QUINTUPLE_WIRE, "wireGt05"),
                    Map.entry(MaterialPrefixes.SEXTUPLE_WIRE, "wireGt06"),
                    Map.entry(MaterialPrefixes.SEPTUPLE_WIRE, "wireGt07"),
                    Map.entry(MaterialPrefixes.OCTUPLE_WIRE, "wireGt08"),
                    Map.entry(MaterialPrefixes.NONUPLE_WIRE, "wireGt09"),
                    Map.entry(MaterialPrefixes.DECUPLE_WIRE, "wireGt10"),
                    Map.entry(MaterialPrefixes.UNDECUPLE_WIRE, "wireGt11"),
                    Map.entry(MaterialPrefixes.DODECUPLE_WIRE, "wireGt12"),
                    Map.entry(MaterialPrefixes.TREDECUPLE_WIRE, "wireGt13"),
                    Map.entry(MaterialPrefixes.TETRADECUPLE_WIRE, "wireGt14"),
                    Map.entry(MaterialPrefixes.PENTADECUPLE_WIRE, "wireGt15"),
                    Map.entry(MaterialPrefixes.HEXADECUPLE_WIRE, "wireGt16"),
                    Map.entry(MaterialPrefixes.CABLE, "cableGt01"),
                    Map.entry(MaterialPrefixes.DOUBLE_CABLE, "cableGt02"),
                    Map.entry(MaterialPrefixes.QUADRUPLE_CABLE, "cableGt04"),
                    Map.entry(MaterialPrefixes.OCTUPLE_CABLE, "cableGt08"),
                    Map.entry(MaterialPrefixes.DODECUPLE_CABLE, "cableGt12"));

    private static volatile State state = State.empty();

    private ElectricalConductorCatalog() {}

    public static String specificationFor(MaterialPrefix form) {
        return SPECIFICATION_BY_FORM.get(form);
    }

    public static int widthPixels(String specification) {
        return switch (specification) {
            case "wireGt01" -> 2;
            case "wireGt02" -> 3;
            case "wireGt03" -> 4;
            case "wireGt04" -> 6;
            case "wireGt05", "wireGt06" -> 7;
            case "wireGt07", "wireGt08" -> 8;
            case "wireGt09" -> 9;
            case "wireGt10" -> 10;
            case "wireGt11" -> 11;
            case "wireGt12" -> 12;
            case "wireGt13" -> 13;
            case "wireGt14" -> 14;
            case "wireGt15" -> 15;
            case "wireGt16" -> 16;
            case "cableGt01" -> 4;
            case "cableGt02" -> 6;
            case "cableGt04" -> 8;
            case "cableGt08" -> 12;
            case "cableGt12" -> 16;
            default -> throw new IllegalArgumentException(
                    "Unsupported conductor specification " + specification);
        };
    }

    public static synchronized void initialize(
            Collection<MaterialDefinition> definitions) {
        if (state.initialized()) {
            throw new IllegalStateException(
                    "Electrical conductor catalog already initialized");
        }
        LinkedHashMap<Key, Entry> entries = new LinkedHashMap<>();
        definitions.stream()
                .sorted(java.util.Comparator.comparing(MaterialDefinition::id))
                .forEach(material -> registerMaterial(entries, material));
        List<Entry> cables = entries.values().stream()
                .filter(entry -> !entry.bareWire())
                .toList();
        List<Entry> wires = entries.values().stream()
                .filter(Entry::bareWire)
                .toList();
        if (cables.size() != EXPECTED_CABLE_BLOCKS
                || wires.size() != EXPECTED_WIRE_BLOCKS) {
            throw new IllegalStateException(
                    "Electrical conductor domain drifted: "
                            + cables.size() + " cables, "
                            + wires.size() + " wires");
        }
        state = new State(
                true,
                Collections.unmodifiableMap(entries),
                List.copyOf(cables),
                List.copyOf(wires));
    }

    private static void registerMaterial(
            Map<Key, Entry> entries,
            MaterialDefinition material) {
        if (REDSTONE_MATERIALS.contains(material.id())) {
            return;
        }
        Map<String, ElectricalProperties> specifications = material.gt6Metadata()
                .map(metadata -> metadata.electricalBySpecification())
                .orElse(Map.of());
        for (MaterialPrefix form : MaterialCatalog.registeredForms(material)) {
            String specification = SPECIFICATION_BY_FORM.get(form);
            if (specification == null) {
                continue;
            }
            ElectricalProperties electrical = specifications.get(specification);
            if (electrical == null) {
                continue;
            }
            Key key = new Key(material.id(), form);
            Entry entry = new Entry(
                    material.id(),
                    form,
                    specification,
                    electrical);
            if (entries.putIfAbsent(key, entry) != null) {
                throw new IllegalStateException(
                        "Duplicate electrical conductor " + key);
            }
        }
    }

    public static List<Entry> cables() {
        return requireState().cables();
    }

    public static List<Entry> wires() {
        return requireState().wires();
    }

    public static List<Entry> all() {
        State snapshot = requireState();
        return java.util.stream.Stream.concat(
                snapshot.cables().stream(), snapshot.wires().stream()).toList();
    }

    public static Entry require(String materialId, MaterialPrefix form) {
        Entry entry = requireState().entries().get(new Key(materialId, form));
        if (entry == null) {
            throw new IllegalArgumentException(
                    "No electrical conductor for "
                            + materialId + "/" + form.serializedName());
        }
        return entry;
    }

    public static boolean contains(
            String materialId, MaterialPrefix form) {
        return requireState().entries().containsKey(
                new Key(materialId, form));
    }

    public static boolean isInitialized() {
        return state.initialized();
    }

    private static State requireState() {
        State snapshot = state;
        if (!snapshot.initialized()) {
            throw new IllegalStateException(
                    "Electrical conductor catalog is not initialized");
        }
        return snapshot;
    }

    public record Key(String materialId, MaterialPrefix form) {
        public Key {
            Objects.requireNonNull(materialId, "materialId");
            Objects.requireNonNull(form, "form");
        }
    }

    public record Entry(
            String materialId,
            MaterialPrefix form,
            String sourceSpecification,
            ElectricalProperties electrical) {
        public Entry {
            Objects.requireNonNull(materialId, "materialId");
            Objects.requireNonNull(form, "form");
            Objects.requireNonNull(sourceSpecification, "sourceSpecification");
            Objects.requireNonNull(electrical, "electrical");
            String expected = SPECIFICATION_BY_FORM.get(form);
            if (!sourceSpecification.equals(expected)) {
                throw new IllegalArgumentException(
                        "Electrical form/specification mismatch");
            }
        }

        public boolean bareWire() {
            return sourceSpecification.startsWith("wire");
        }

        public String registryName() {
            return materialId + "/" + form.serializedName();
        }
    }

    private record State(
            boolean initialized,
            Map<Key, Entry> entries,
            List<Entry> cables,
            List<Entry> wires) {
        private static State empty() {
            return new State(false, Map.of(), List.of(), List.of());
        }
    }
}
