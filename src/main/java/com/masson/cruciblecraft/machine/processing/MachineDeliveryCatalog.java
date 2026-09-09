package com.masson.cruciblecraft.machine.processing;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.google.gson.annotations.SerializedName;
import com.masson.cruciblecraft.api.energy.EnergyType;

import net.minecraft.resources.ResourceLocation;

/**
 * Kind-level delivery sidecar. Names, acquisition and variant rows stay in
 * {@link MachineKindCatalog} / {@link MachineTierCatalog}.
 */
public final class MachineDeliveryCatalog {
    private static final String RESOURCE =
            "/data/cruciblecraft/machine_delivery.json";
    private static final MachineDeliveryCatalog BUNDLED = loadBundled();

    private final List<Host> hosts;
    private final Map<ResourceLocation, Host> byId;
    private final Map<String, String> textureAliases;
    private final Set<String> shapedModels;

    public static MachineDeliveryCatalog bundled() {
        return BUNDLED;
    }

    public static List<Host> hosts() {
        return BUNDLED.hosts;
    }

    public static Host require(ResourceLocation id) {
        return BUNDLED.requireHost(id);
    }

    public static Optional<Host> find(ResourceLocation id) {
        return Optional.ofNullable(BUNDLED.byId.get(id));
    }

    public static Optional<Host> findByPath(String path) {
        Objects.requireNonNull(path, "path");
        return find(ResourceLocation.fromNamespaceAndPath("cruciblecraft", path));
    }

    public static String textureAlias(String blockPath) {
        return BUNDLED.textureAliases.getOrDefault(blockPath, blockPath);
    }

    public static boolean isShapedModel(String textureId) {
        return BUNDLED.shapedModels.contains(textureId);
    }

    public static Set<String> shapedModels() {
        return BUNDLED.shapedModels;
    }

    public static Map<String, String> textureAliases() {
        return BUNDLED.textureAliases;
    }

    private MachineDeliveryCatalog(
            List<Host> hosts,
            Map<String, String> textureAliases,
            Set<String> shapedModels) {
        this.hosts = List.copyOf(hosts);
        LinkedHashMap<ResourceLocation, Host> indexed = new LinkedHashMap<>();
        for (Host host : this.hosts) {
            if (indexed.putIfAbsent(host.id(), host) != null) {
                throw new IllegalStateException(
                        "Duplicate machine delivery host " + host.id());
            }
        }
        this.byId = Map.copyOf(indexed);
        this.textureAliases = Map.copyOf(textureAliases);
        this.shapedModels = Set.copyOf(shapedModels);
    }

    public Host requireHost(ResourceLocation id) {
        Host host = byId.get(id);
        if (host == null) {
            throw new IllegalArgumentException("Unknown machine delivery host " + id);
        }
        return host;
    }

    public List<Host> entries() {
        return hosts;
    }

    private static MachineDeliveryCatalog loadBundled() {
        return fromDocument(CatalogJson.readBundled(
                MachineDeliveryCatalog.class, RESOURCE, Document.class));
    }

    private static MachineDeliveryCatalog fromDocument(Document document) {
        if (document.schemaVersion != 1
                || document.hosts == null
                || document.hosts.isEmpty()) {
            throw new IllegalStateException("Invalid machine delivery catalog");
        }
        CatalogJson.requireRevision(document.sourceRevision, RESOURCE);
        List<Host> hosts = document.hosts.stream().map(HostRow::toHost).toList();
        Map<String, String> aliases = document.textureAliases == null
                ? Map.of()
                : Map.copyOf(document.textureAliases);
        Set<String> shaped = document.shapedModels == null
                ? Set.of()
                : Set.copyOf(document.shapedModels);
        return new MachineDeliveryCatalog(hosts, aliases, shaped);
    }

    public record Host(
            ResourceLocation id,
            ResourceLocation recipeMap,
            String specFamily,
            EnergyType energyType,
            ProcessingMachineSpec.EnergyMode energyMode,
            int itemInputs,
            int itemOutputs,
            int fluidInputs,
            int fluidOutputs,
            int gt6InItems,
            int gt6OutItems,
            int gt6InFluids,
            int gt6OutFluids,
            String textureProfile,
            String gt6Source,
            String artDestination,
            boolean kindCatalog) {
        public Host {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(recipeMap, "recipeMap");
            Objects.requireNonNull(specFamily, "specFamily");
            Objects.requireNonNull(energyType, "energyType");
            Objects.requireNonNull(energyMode, "energyMode");
            Objects.requireNonNull(textureProfile, "textureProfile");
            Objects.requireNonNull(gt6Source, "gt6Source");
            Objects.requireNonNull(artDestination, "artDestination");
        }
    }

    private static final class Document {
        @SerializedName("schema_version")
        private int schemaVersion;
        @SerializedName("source_revision")
        private String sourceRevision;
        private List<HostRow> hosts;
        @SerializedName("texture_aliases")
        private Map<String, String> textureAliases;
        @SerializedName("shaped_models")
        private List<String> shapedModels;
    }

    private static final class HostRow {
        private String id;
        @SerializedName("recipe_map")
        private String recipeMap;
        @SerializedName("spec_family")
        private String specFamily;
        private EnergyRow energy;
        private SlotRow slots;
        @SerializedName("gt6_panel")
        private PanelRow gt6Panel;
        @SerializedName("texture_profile")
        private String textureProfile;
        @SerializedName("gt6_source")
        private String gt6Source;
        @SerializedName("art_destination")
        private String artDestination;
        @SerializedName("kind_catalog")
        private Boolean kindCatalog;

        private Host toHost() {
            if (!CatalogJson.nonBlank(id)
                    || !CatalogJson.nonBlank(recipeMap)
                    || !CatalogJson.nonBlank(specFamily)
                    || energy == null
                    || slots == null
                    || gt6Panel == null
                    || !CatalogJson.nonBlank(textureProfile)
                    || !CatalogJson.nonBlank(gt6Source)
                    || !CatalogJson.nonBlank(artDestination)
                    || kindCatalog == null) {
                throw new IllegalStateException("Incomplete machine delivery host");
            }
            ResourceLocation parsed = ResourceLocation.tryParse(id);
            ResourceLocation map = ResourceLocation.tryParse(recipeMap);
            if (parsed == null || map == null) {
                throw new IllegalStateException("Invalid machine delivery ids " + id);
            }
            return new Host(
                    parsed,
                    map,
                    specFamily,
                    EnergyType.valueOf(energy.type),
                    ProcessingMachineSpec.EnergyMode.valueOf(energy.mode),
                    slots.itemInputs,
                    slots.itemOutputs,
                    slots.fluidInputs,
                    slots.fluidOutputs,
                    gt6Panel.inItems,
                    gt6Panel.outItems,
                    gt6Panel.inFluids,
                    gt6Panel.outFluids,
                    textureProfile,
                    gt6Source,
                    artDestination,
                    kindCatalog);
        }
    }

    private static final class EnergyRow {
        private String type;
        private String mode;
    }

    private static final class SlotRow {
        @SerializedName("item_inputs")
        private int itemInputs;
        @SerializedName("item_outputs")
        private int itemOutputs;
        @SerializedName("fluid_inputs")
        private int fluidInputs;
        @SerializedName("fluid_outputs")
        private int fluidOutputs;
    }

    private static final class PanelRow {
        @SerializedName("in_items")
        private int inItems;
        @SerializedName("out_items")
        private int outItems;
        @SerializedName("in_fluids")
        private int inFluids;
        @SerializedName("out_fluids")
        private int outFluids;
    }
}
