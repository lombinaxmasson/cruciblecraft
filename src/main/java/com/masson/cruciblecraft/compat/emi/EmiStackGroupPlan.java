package com.masson.cruciblecraft.compat.emi;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.AnvilHosts;
import com.masson.cruciblecraft.content.block.FoundryHosts;
import com.masson.cruciblecraft.content.item.GtBlockObjectCatalog;
import com.masson.cruciblecraft.content.item.GtBuildingBlockCatalog;
import com.masson.cruciblecraft.content.item.GtStoneCatalog;
import com.masson.cruciblecraft.content.item.GtWoodCatalog;
import com.masson.cruciblecraft.content.mold.CeramicMoldCatalog;
import com.masson.cruciblecraft.content.mte.MteInPlaceCatalog;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.content.mte.MteInPlaceSpec;
import com.masson.cruciblecraft.content.storage.StorageBehaviorProfile;
import com.masson.cruciblecraft.content.storage.StorageVariant;
import com.masson.cruciblecraft.content.storage.StorageVariantCatalog;
import com.masson.cruciblecraft.energy.converter.EnergyConverterKindCatalog;
import com.masson.cruciblecraft.energy.converter.EnergyConverterTierCatalog;
import com.masson.cruciblecraft.logistics.hopper.HopperKind;
import com.masson.cruciblecraft.logistics.hopper.HopperVariant;
import com.masson.cruciblecraft.logistics.hopper.HopperVariantCatalog;
import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;
import com.masson.cruciblecraft.machine.processing.MachineKindCatalog;
import com.masson.cruciblecraft.machine.processing.MachineTierCatalog;
import com.masson.cruciblecraft.material.MaterialFormHosts;
import com.masson.cruciblecraft.material.MaterialRegistrationGate;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.material.def.MaterialLoader;

import net.minecraft.resources.ResourceLocation;

/**
 * EMI-free projection for Reliable EMI / EMI++ stack groups: one expandable
 * index row per material prefix (dust, ingot, crushed ore, tool heads, …),
 * one per tool Item, and one per processing-machine / energy-converter kind
 * that has two or more live variants. Unique slash ids
 * {@code cruciblecraft:{material}/{form}} and the shared leftover prefix Item
 * {@code cruciblecraft:{form}} both match. Machine groups list exact variant
 * ids so {@code centrifuge} does not steal {@code large_centrifuge}. Building,
 * furniture, hopper, tool, foundry, and fluid-attachment groups also list
 * exact ids so {@code glass/black} does not steal {@code glass/ingot}. Wider
 * {@code c:dusts} tags stay out so other mods are not pulled in. Recipe
 * lookup is unchanged; this is index grouping only.
 *
 * <p>Generated JSON uses {@code remi:group} {@code contents} item ids, not
 * {@code remi:regex}. Reliable EMI's regex groups disable the id-set fast
 * path and re-test every index stack against every pattern.
 */
public final class EmiStackGroupPlan {
    public static final int PRIORITY = 100;
    public static final String TYPE = "remi:group";
    private static final List<String> EMPTY_TOOL_HEAD_PATHS = List.of(
            "empty/tool_head_chainsaw",
            "empty/tool_head_drill",
            "empty/tool_head_pickaxe_gem",
            "empty/tool_head_wrench");
    private static final Path FORM_ID_CONFIG = Path.of(
            "build", "tmp", "emi-stack-group-config");
    private static volatile Map<String, List<String>> cachedFormItemIds;

    private EmiStackGroupPlan() {}

    public record FormGroup(
            String serializedPath,
            String id,
            String nameKey,
            String sharedItemRegex,
            String uniqueItemRegex,
            List<String> itemIds) {
        public FormGroup {
            itemIds = List.copyOf(itemIds);
        }

        public List<String> regexes() {
            return List.of(sharedItemRegex, uniqueItemRegex);
        }

        public String resourcePath() {
            return "form/" + serializedPath;
        }

        public JsonObject toJson() {
            return contentsGroupJson(id, nameKey, itemIds);
        }
    }

    /**
     * Quoted exact item ids. Tools are one Item per kind; machines and
     * converters are one BlockItem per housing material.
     */
    public record ExactGroup(
            String resourcePath,
            String id,
            String nameKey,
            List<String> itemIds) {
        public ExactGroup {
            itemIds = List.copyOf(itemIds);
        }

        public List<String> regexes() {
            List<String> regexes = new ArrayList<>(itemIds.size());
            for (String itemId : itemIds) {
                regexes.add(exactItemRegex(itemId));
            }
            return List.copyOf(regexes);
        }

        public JsonObject toJson() {
            return contentsGroupJson(id, nameKey, itemIds);
        }
    }

    public static List<FormGroup> groups(Collection<MaterialPrefix> prefixes) {
        List<FormGroup> groups = new ArrayList<>();
        Map<String, List<String>> itemIds = formItemIds();
        for (MaterialPrefix prefix : prefixes) {
            groups.add(group(
                    prefix.serializedName(),
                    itemIds.getOrDefault(prefix.serializedName(), List.of())));
        }
        return List.copyOf(groups);
    }

    public static List<FormGroup> emittedFormGroups(
            Collection<MaterialPrefix> prefixes) {
        List<FormGroup> emitted = new ArrayList<>();
        for (FormGroup group : groups(prefixes)) {
            if (!group.itemIds().isEmpty()) {
                emitted.add(group);
            }
        }
        return List.copyOf(emitted);
    }

    public static FormGroup group(String serializedPath) {
        return group(
                serializedPath,
                formItemIds().getOrDefault(serializedPath, List.of()));
    }

    public static FormGroup group(String serializedPath, List<String> itemIds) {
        return new FormGroup(
                serializedPath,
                "cruciblecraft:form/" + serializedPath,
                "emi.cruciblecraft.group." + serializedPath,
                sharedItemRegex(serializedPath),
                uniqueItemRegex(serializedPath),
                itemIds);
    }

    public static List<ExactGroup> toolGroups() {
        List<ExactGroup> groups = new ArrayList<>();
        for (ToolKind kind : ToolKind.values()) {
            String path = kind.serializedName();
            groups.add(new ExactGroup(
                    "tool/" + path,
                    "cruciblecraft:tool/" + path,
                    "emi.cruciblecraft.group.tool." + path,
                    List.of("cruciblecraft:" + toolItemPath(kind))));
        }
        return List.copyOf(groups);
    }

    public static List<ExactGroup> machineGroups() {
        List<ExactGroup> groups = new ArrayList<>();
        for (MachineKindCatalog.Kind kind : MachineKindCatalog.kinds()) {
            List<String> itemIds = new ArrayList<>();
            for (ResourceLocation variantId :
                    MachineTierCatalog.itemIdsOf(kind.id())) {
                itemIds.add(variantId.toString());
            }
            if (itemIds.size() < 2) {
                continue;
            }
            String path = kind.id().getPath();
            groups.add(new ExactGroup(
                    "machine/" + path,
                    "cruciblecraft:machine/" + path,
                    "emi.cruciblecraft.group.machine." + path,
                    itemIds));
        }
        return List.copyOf(groups);
    }

    public static List<ExactGroup> converterGroups() {
        Map<ResourceLocation, List<String>> byKind = new LinkedHashMap<>();
        for (EnergyConverterKindCatalog.Kind kind :
                EnergyConverterKindCatalog.kinds()) {
            byKind.put(kind.id(), new ArrayList<>());
        }
        for (EnergyConverterTierCatalog.Entry entry :
                EnergyConverterTierCatalog.entries()) {
            byKind.computeIfAbsent(entry.kindId(), unused -> new ArrayList<>())
                    .add(entry.id().toString());
        }
        List<ExactGroup> groups = new ArrayList<>();
        for (EnergyConverterKindCatalog.Kind kind :
                EnergyConverterKindCatalog.kinds()) {
            List<String> itemIds = byKind.getOrDefault(kind.id(), List.of());
            if (itemIds.size() < 2) {
                continue;
            }
            String path = kind.id().getPath();
            groups.add(new ExactGroup(
                    "converter/" + path,
                    "cruciblecraft:converter/" + path,
                    "emi.cruciblecraft.group.converter." + path,
                    itemIds));
        }
        return List.copyOf(groups);
    }

    public static List<ExactGroup> exactGroups() {
        List<ExactGroup> groups = new ArrayList<>();
        groups.addAll(toolGroups());
        groups.addAll(machineGroups());
        groups.addAll(converterGroups());
        groups.addAll(catalogGroups());
        return List.copyOf(groups);
    }

    /**
     * Building blocks, furniture, hoppers, tools, foundry parts, fluid
     * attachments, smelting crucibles, and molds.
     * Glass uses catalog ids, not {@code glass/[^/]+}, so material forms such
     * as {@code glass/ingot} stay in the prefix groups.
     */
    public static List<ExactGroup> catalogGroups() {
        List<ExactGroup> groups = new ArrayList<>();
        List<GtBlockObjectCatalog.Variant> building = new ArrayList<>();
        building.addAll(GtBlockObjectCatalog.variants());
        building.addAll(GtBuildingBlockCatalog.variants());
        addIfMany(
                groups,
                "building/glass",
                ids(building, variant ->
                        variant.glassLike() && !variant.glowGlass() && !variant.slab()));
        addIfMany(
                groups,
                "building/glass_slab",
                ids(building, variant ->
                        variant.glassLike() && !variant.glowGlass() && variant.slab()));
        addIfMany(
                groups,
                "building/glow_glass",
                ids(building, variant -> variant.glowGlass() && !variant.slab()));
        addIfMany(
                groups,
                "building/glow_glass_slab",
                ids(building, variant -> variant.glowGlass() && variant.slab()));
        List<String> planks = new ArrayList<>();
        for (GtWoodCatalog.Definition definition : GtWoodCatalog.DEFINITIONS) {
            if (definition.id().endsWith("planks")) {
                planks.add("cruciblecraft:" + definition.registryPath());
            }
        }
        addIfMany(groups, "building/planks", planks);
        List<String> slabs = ids(building, variant ->
                variant.slab() && !variant.glassLike());
        for (GtStoneCatalog.Variant variant : GtStoneCatalog.variants()) {
            if (variant.slab()) {
                slabs.add(variant.id().toString());
            }
        }
        addIfMany(groups, "building/slab", slabs);
        addIfMany(groups, "building/log", ids(building, GtBlockObjectCatalog.Variant::log));
        addIfMany(groups, "building/bars", ids(building, GtBlockObjectCatalog.Variant::bars));
        addIfMany(groups, "building/rail", ids(building, GtBlockObjectCatalog.Variant::rail));
        addIfMany(
                groups,
                "building/spike",
                ids(building, GtBlockObjectCatalog.Variant::spike));
        addIfMany(groups, "building/bale", ids(building, GtBlockObjectCatalog.Variant::bale));
        addIfMany(
                groups,
                "building/cfoam",
                ids(building, variant -> variant.cfoam() && !variant.slab()));
        addIfMany(
                groups,
                "building/cfoam_fresh",
                ids(building, variant -> variant.cfoamFresh() && !variant.slab()));
        Set<String> used = new HashSet<>();
        for (ExactGroup group : groups) {
            used.addAll(group.itemIds());
        }
        Map<String, List<String>> leftover = new LinkedHashMap<>();
        for (GtBlockObjectCatalog.Variant variant : building) {
            String itemId = variant.id().toString();
            if (used.contains(itemId) || variant.slab() || variant.glassLike()) {
                continue;
            }
            leftover.computeIfAbsent(firstPathSegment(variant.registryPath()), unused ->
                    new ArrayList<>()).add(itemId);
        }
        for (Map.Entry<String, List<String>> entry : leftover.entrySet()) {
            addIfMany(groups, "building/" + entry.getKey(), entry.getValue());
        }
        List<String> stones = new ArrayList<>();
        for (GtStoneCatalog.Variant variant : GtStoneCatalog.variants()) {
            if (!variant.slab()) {
                stones.add(variant.id().toString());
            }
        }
        addIfMany(groups, "building/stone", stones);
        addIfMany(
                groups,
                "furniture/bookshelf",
                furnitureIds(StorageBehaviorProfile.BOOKSHELF, MteInPlaceKind.BOOKSHELF));
        addIfMany(
                groups,
                "furniture/drawer",
                furnitureIds(StorageBehaviorProfile.DRAWER, MteInPlaceKind.DRAWER));
        addIfMany(groups, "furniture/safe", mteIds(MteInPlaceKind.SAFE));
        addIfMany(groups, "furniture/chest", mteIds(MteInPlaceKind.CHEST));
        addIfMany(groups, "hopper/hopper", hopperIds(HopperKind.HOPPER));
        addIfMany(
                groups,
                "hopper/queue_hopper",
                hopperIds(HopperKind.QUEUE_HOPPER));
        List<String> anvils = new ArrayList<>();
        anvils.add("cruciblecraft:anvil");
        anvils.addAll(mteIds(AnvilHosts::isAnvil));
        addIfMany(groups, "misc_tool/anvil", anvils);
        addIfMany(
                groups,
                "misc_tool/mortar",
                mteIds(spec -> spec.gt6Class().contains("MultiTileEntityMortar")));
        addIfMany(groups, "fluid_attachment/faucet", mteIds(MteInPlaceKind.FAUCET));
        addIfMany(groups, "fluid_attachment/tap", mteIds(MteInPlaceKind.TAP));
        addIfMany(groups, "fluid_attachment/funnel", mteIds(MteInPlaceKind.FUNNEL));
        addIfMany(groups, "fluid_attachment/nozzle", mteIds(MteInPlaceKind.NOZZLE));
        addIfMany(
                groups,
                "fluid_attachment/cap_nozzle",
                mteIds(MteInPlaceKind.CAP_NOZZLE));
        List<String> crucibles = new ArrayList<>();
        List<String> foundryMolds = new ArrayList<>();
        for (MteInPlaceSpec spec : MteInPlaceCatalog.specs()) {
            if (FoundryHosts.isSmeltingCrucible(spec.registryPath())) {
                crucibles.add(spec.id().toString());
            } else if (FoundryHosts.isMold(spec)) {
                foundryMolds.add(spec.id().toString());
            }
        }
        addIfMany(groups, "foundry/crucible", crucibles);
        addIfMany(groups, "foundry/basin", mteIds(FoundryHosts::isBasin));
        addIfMany(groups, "foundry/crossing", mteIds(FoundryHosts::isCrossing));
        addIfMany(groups, "foundry/mold", foundryMolds);
        List<String> molds = new ArrayList<>();
        molds.add("cruciblecraft:raw_ceramic_mold");
        molds.add("cruciblecraft:ceramic_mold");
        for (CeramicMoldCatalog.Variant variant : CeramicMoldCatalog.SHAPED) {
            molds.add("cruciblecraft:" + CeramicMoldCatalog.rawItemId(variant));
            molds.add("cruciblecraft:" + CeramicMoldCatalog.firedItemId(variant));
        }
        addIfMany(groups, "mold/ceramic", molds);
        addIfMany(
                groups,
                "energy/large_gas_turbine",
                mteIds(MteInPlaceKind.GAS_TURBINE));
        return List.copyOf(groups);
    }

    private static List<String> ids(
            List<GtBlockObjectCatalog.Variant> variants,
            Predicate<GtBlockObjectCatalog.Variant> keep) {
        List<String> ids = new ArrayList<>();
        for (GtBlockObjectCatalog.Variant variant : variants) {
            if (keep.test(variant)) {
                ids.add(variant.id().toString());
            }
        }
        return ids;
    }

    private static List<String> furnitureIds(
            StorageBehaviorProfile profile,
            MteInPlaceKind kind) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (StorageVariant variant : StorageVariantCatalog.of(profile)) {
            ids.add(variant.id().toString());
        }
        ids.addAll(mteIds(kind));
        return List.copyOf(ids);
    }

    private static List<String> mteIds(MteInPlaceKind kind) {
        return mteIds(spec -> spec.kind() == kind);
    }

    private static List<String> mteIds(Predicate<MteInPlaceSpec> keep) {
        List<String> ids = new ArrayList<>();
        for (MteInPlaceSpec spec : MteInPlaceCatalog.specs()) {
            if (keep.test(spec)) {
                ids.add(spec.id().toString());
            }
        }
        return ids;
    }

    private static List<String> hopperIds(HopperKind kind) {
        List<String> ids = new ArrayList<>();
        for (HopperVariant variant : HopperVariantCatalog.variantsOf(kind)) {
            ids.add(variant.id().toString());
        }
        return ids;
    }

    private static String firstPathSegment(String registryPath) {
        int slash = registryPath.indexOf('/');
        return slash < 0 ? registryPath : registryPath.substring(0, slash);
    }

    private static void addIfMany(
            List<ExactGroup> groups,
            String resourcePath,
            List<String> itemIds) {
        if (itemIds.size() < 2) {
            return;
        }
        groups.add(new ExactGroup(
                resourcePath,
                "cruciblecraft:" + resourcePath,
                "emi.cruciblecraft.group." + resourcePath.replace('/', '.'),
                itemIds));
    }

    /** Registry path of the shared {@code MaterialToolItem} for this kind. */
    public static String toolItemPath(ToolKind kind) {
        if (kind == ToolKind.SMITHING_HAMMER) {
            return "smithing_hammer";
        }
        return "material_" + kind.serializedName();
    }

    /** Exact leftover prefix Item id, not {@code small_dust} when the form is {@code dust}. */
    public static String sharedItemRegex(String serializedPath) {
        return "cruciblecraft:" + serializedPath;
    }

    /**
     * Unique hosted / public-exchange slash id. {@code [^/]+} is the material
     * segment, so {@code dust} does not steal {@code small_dust}.
     */
    public static String uniqueItemRegex(String serializedPath) {
        return "cruciblecraft:[^/]+/" + serializedPath;
    }

    public static String exactItemRegex(String itemId) {
        return Pattern.quote(itemId);
    }

    public static boolean matches(String regex, String stackId) {
        return Pattern.compile(regex).matcher(stackId).matches();
    }

    public static boolean matchesAny(ExactGroup group, String stackId) {
        for (String regex : group.regexes()) {
            if (matches(regex, stackId)) {
                return true;
            }
        }
        return false;
    }

    public static Map<String, List<String>> formItemIds() {
        Map<String, List<String>> current = cachedFormItemIds;
        if (current != null) {
            return current;
        }
        synchronized (EmiStackGroupPlan.class) {
            if (cachedFormItemIds == null) {
                cachedFormItemIds = computeFormItemIds();
            }
            return cachedFormItemIds;
        }
    }

    private static Map<String, List<String>> computeFormItemIds() {
        Map<String, MaterialDefinition> materials =
                MaterialLoader.load(FORM_ID_CONFIG);
        Map<String, List<MaterialPrefix>> registered =
                MaterialRegistrationGate.load(materials.values());
        Map<String, LinkedHashSet<String>> ids = new LinkedHashMap<>();
        for (MaterialDefinition material : materials.values()) {
            List<MaterialPrefix> forms = registered.get(material.id());
            if (forms == null) {
                continue;
            }
            for (MaterialPrefix form : forms) {
                String itemId = liveItemId(material, form);
                if (itemId == null) {
                    continue;
                }
                ids.computeIfAbsent(form.serializedName(), unused ->
                        new LinkedHashSet<>()).add(itemId);
            }
        }
        for (String path : EMPTY_TOOL_HEAD_PATHS) {
            int slash = path.lastIndexOf('/');
            if (slash <= 0 || slash == path.length() - 1) {
                continue;
            }
            ids.computeIfAbsent(path.substring(slash + 1), unused ->
                    new LinkedHashSet<>()).add("cruciblecraft:" + path);
        }
        LinkedHashMap<String, List<String>> result = new LinkedHashMap<>();
        ids.forEach((form, itemIds) -> result.put(form, List.copyOf(itemIds)));
        return Map.copyOf(result);
    }

    /**
     * Live registry id for one gated {@code (material, prefix)}. Shared
     * leftover inventory folds onto the prefix Item; public-exchange and
     * unique hosted forms keep their own slash / ore ids.
     */
    static String liveItemId(MaterialDefinition material, MaterialPrefix form) {
        String override = material.formItems().get(form);
        if (override != null) {
            return override.startsWith("cruciblecraft:") ? override : null;
        }
        if (form.equals(MaterialPrefixes.ORE)) {
            return "cruciblecraft:" + material.id() + "_ore";
        }
        if (MaterialFormHosts.isPublicExchangePrefix(form)
                || MaterialFormHosts.isUniqueHostedPrefixPath(
                        form.serializedName())) {
            return "cruciblecraft:" + material.registryName(form);
        }
        return "cruciblecraft:" + MaterialFormHosts.prefixItemPath(form);
    }

    private static JsonObject contentsGroupJson(
            String id,
            String nameKey,
            List<String> itemIds) {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("name", nameKey);
        json.addProperty("type", TYPE);
        json.addProperty("enabled", true);
        json.addProperty("priority", PRIORITY);
        JsonArray array = new JsonArray();
        for (String itemId : itemIds) {
            array.add("item:" + itemId);
        }
        json.add("contents", array);
        return json;
    }
}
