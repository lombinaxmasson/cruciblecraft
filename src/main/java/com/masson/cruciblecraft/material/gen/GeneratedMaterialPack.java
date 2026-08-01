package com.masson.cruciblecraft.material.gen;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.MaterialFingerprint;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixDefinition;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.fml.loading.FMLEnvironment;

public final class GeneratedMaterialPack {
    private static final int SERVER_PACK_FORMAT = 48;
    private static final int CLIENT_PACK_FORMAT = 34;
    private static final String OUTPUT_SCHEMA = "generated-material-pack-v1";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String OUTPUT_GENERATOR_IDENTITY = outputGeneratorIdentity();
    private static volatile Roots roots = new Roots(null, null);

    private GeneratedMaterialPack() {}

    public static void initialize(Path configRoot) {
        initialize(configRoot, FMLEnvironment.dist.isClient());
    }

    static void initialize(Path configRoot, boolean clientDistribution) {
        Path generatedRoot = configRoot.resolve(".generated-material-pack");
        Path serverRoot = requireGeneratedRoot(generatedRoot.resolve("server"), "server");
        GeneratedMaterialPackPlan plan =
                GeneratedMaterialPackPlan.forDistribution(clientDistribution);
        Path clientRoot = plan.clientAssets()
                ? requireGeneratedRoot(generatedRoot.resolve("client"), "client")
                : null;
        Collection<MaterialDefinition> materials = MaterialCatalog.startupValues();
        Map<String, List<MaterialPrefix>> registeredForms = registeredForms(materials);
        try {
            if (plan.serverData()) {
                GeneratedMaterialPackCache.ensure(
                        serverRoot,
                        generationFingerprint(
                                "server",
                                materials,
                                registeredForms,
                                MaterialPrefixCatalog.definitions(),
                                SERVER_PACK_FORMAT,
                                OUTPUT_GENERATOR_IDENTITY),
                        () -> planServerFiles(materials, registeredForms));
            }
            if (plan.clientAssets()) {
                GeneratedMaterialPackCache.ensure(
                        clientRoot,
                        generationFingerprint(
                                "client",
                                materials,
                                registeredForms,
                                MaterialPrefixCatalog.definitions(),
                                CLIENT_PACK_FORMAT,
                                OUTPUT_GENERATOR_IDENTITY),
                        () -> planClientFiles(materials, registeredForms));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to generate material resource pack", exception);
        }
        roots = new Roots(serverRoot, clientRoot);
    }

    public static void addPackFinders(AddPackFindersEvent event) {
        Roots snapshot = roots;
        Path root = switch (event.getPackType()) {
            case SERVER_DATA -> snapshot.server();
            case CLIENT_RESOURCES -> snapshot.client();
        };
        if (root == null) {
            return;
        }

        event.addRepositorySource(consumer -> {
            PackLocationInfo location = new PackLocationInfo(
                    CrucibleCraft.MODID + "/generated_materials",
                    Component.literal("CrucibleCraft Generated Materials"),
                    PackSource.BUILT_IN,
                    Optional.empty());
            Pack pack = Pack.readMetaAndCreate(
                    location,
                    new PathPackResources.PathResourcesSupplier(root),
                    event.getPackType(),
                    new PackSelectionConfig(true, Pack.Position.TOP, true));
            if (pack != null) {
                consumer.accept(pack);
            }
        });
    }

    private static Map<String, String> serverFiles() {
        return planServerFiles(
                MaterialCatalog.startupValues(),
                registeredForms(MaterialCatalog.startupValues()));
    }

    public static Map<String, String> planServerFiles(
            Collection<MaterialDefinition> materials,
            Map<String, List<MaterialPrefix>> registeredForms) {
        LinkedHashMap<String, String> files = new LinkedHashMap<>();
        files.put("pack.mcmeta", packMeta(SERVER_PACK_FORMAT));
        Map<ResourceLocation, List<String>> aggregateItemTags = new LinkedHashMap<>();
        List<String> aggregateOreTags = new ArrayList<>();
        List<String> mineableOres = new ArrayList<>();
        List<String> stoneToolOres = new ArrayList<>();
        List<String> ironToolOres = new ArrayList<>();
        for (MaterialDefinition material : materials) {
            List<String> materialItems = new ArrayList<>();
            List<MaterialPrefix> forms = requireRegisteredForms(material, registeredForms);
            for (MaterialPrefix form : forms) {
                String formTag = form.tagDirectory();
                String tagNamespace = form.tagNamespace();
                if (form.equals(MaterialPrefixes.ORE)) {
                    if (!forms.contains(MaterialPrefixes.RAW_ORE)) {
                        throw new IllegalStateException(
                                "Registered ore material has no registered RAW_ORE: "
                                        + material.id());
                    }
                    List<String> oreIds = oreIds(material.id());
                    addTag(
                            files,
                            "data/" + tagNamespace + "/tags/item/" + formTag + "/"
                                    + material.tagName() + ".json",
                            oreIds);
                    addTag(
                            files,
                            "data/" + tagNamespace + "/tags/block/" + formTag + "/"
                                    + material.tagName() + ".json",
                            oreIds);
                    aggregateItemTags.computeIfAbsent(
                                    ResourceLocation.fromNamespaceAndPath(
                                            tagNamespace, formTag),
                                    ignored -> new ArrayList<>())
                            .add("#" + tagNamespace + ":" + formTag + "/"
                                    + material.tagName());
                    aggregateOreTags.add(
                            "#" + tagNamespace + ":" + formTag + "/" + material.tagName());
                    materialItems.addAll(oreIds);
                    mineableOres.addAll(oreIds);
                    (material.tier() >= 2 ? ironToolOres : stoneToolOres).addAll(oreIds);
                    String rawOreId = canonicalItemId(material, MaterialPrefixes.RAW_ORE);
                    oreIds.forEach(oreId -> files.put(
                            "data/" + CrucibleCraft.MODID + "/loot_table/blocks/"
                                    + ResourceLocation.parse(oreId).getPath() + ".json",
                            oreLootTable(oreId, rawOreId)));
                    continue;
                }
                String itemId = canonicalItemId(material, form);
                addTag(
                        files,
                        "data/" + tagNamespace + "/tags/item/" + formTag + "/"
                                + material.tagName() + ".json",
                        List.of(itemId));
                aggregateItemTags.computeIfAbsent(
                                ResourceLocation.fromNamespaceAndPath(tagNamespace, formTag),
                                ignored -> new ArrayList<>())
                        .add("#" + tagNamespace + ":" + formTag + "/" + material.tagName());
                materialItems.add(itemId);
            }
            if (!materialItems.isEmpty()) {
                addTag(
                        files,
                        "data/" + CrucibleCraft.MODID + "/tags/item/materials/"
                                + material.id() + ".json",
                        materialItems);
            }
        }
        for (var aggregate : aggregateItemTags.entrySet()) {
            addTag(
                    files,
                    "data/" + aggregate.getKey().getNamespace() + "/tags/item/"
                            + aggregate.getKey().getPath() + ".json",
                    aggregate.getValue());
        }
        addTag(files, "data/c/tags/block/ores.json", aggregateOreTags);
        addTag(files, "data/minecraft/tags/block/mineable/pickaxe.json", mineableOres);
        addTag(files, "data/minecraft/tags/block/needs_stone_tool.json", stoneToolOres);
        addTag(files, "data/minecraft/tags/block/needs_iron_tool.json", ironToolOres);
        return Map.copyOf(files);
    }

    private static Map<String, String> clientFiles() {
        return planClientFiles(
                MaterialCatalog.startupValues(),
                registeredForms(MaterialCatalog.startupValues()));
    }

    public static Map<String, String> planClientFiles(
            Collection<MaterialDefinition> materials,
            Map<String, List<MaterialPrefix>> registeredForms) {
        LinkedHashMap<String, String> files = new LinkedHashMap<>();
        files.put("pack.mcmeta", packMeta(CLIENT_PACK_FORMAT));
        JsonObject oreTranslations = new JsonObject();
        for (MaterialDefinition material : materials) {
            for (MaterialPrefix form : requireRegisteredForms(material, registeredForms)) {
                if (form.equals(MaterialPrefixes.ORE)) {
                    addOreClientFiles(files, material.id());
                    String materialName = title(material.id());
                    oreTranslations.addProperty(
                            "block." + CrucibleCraft.MODID + "." + material.id() + "_ore",
                            materialName + " Ore");
                    oreTranslations.addProperty(
                            "block." + CrucibleCraft.MODID + ".deepslate_"
                                    + material.id() + "_ore",
                            "Deepslate " + materialName + " Ore");
                    continue;
                }
                if (material.formItems().containsKey(form)) {
                    continue;
                }
                JsonObject model = new JsonObject();
                var prefix = MaterialPrefixCatalog.definition(form);
                model.addProperty("parent", prefix.modelTemplate());
                JsonObject textures = new JsonObject();
                textures.addProperty("layer0", prefix.modelTexture());
                model.add("textures", textures);
                files.put(
                        "assets/" + CrucibleCraft.MODID + "/models/item/"
                                + material.registryName(form) + ".json",
                        GSON.toJson(model));
            }
        }
        if (!oreTranslations.entrySet().isEmpty()) {
            files.put(
                    "assets/" + CrucibleCraft.MODID + "/lang/en_us.json",
                    GSON.toJson(oreTranslations));
        }
        return Map.copyOf(files);
    }

    private static Map<String, List<MaterialPrefix>> registeredForms(
            Collection<MaterialDefinition> materials) {
        LinkedHashMap<String, List<MaterialPrefix>> result = new LinkedHashMap<>();
        materials.forEach(material ->
                result.put(material.id(), MaterialCatalog.registeredForms(material)));
        return Map.copyOf(result);
    }

    static String generationFingerprint(
            String side,
            Collection<MaterialDefinition> materials,
            Map<String, List<MaterialPrefix>> registeredForms,
            Collection<MaterialPrefixDefinition> prefixes,
            int packFormat,
            String generatorIdentity) {
        StringBuilder canonical = new StringBuilder();
        appendCanonical(canonical, "side", side);
        appendCanonical(canonical, "pack_format", Integer.toString(packFormat));
        appendCanonical(canonical, "generator", generatorIdentity);
        appendCanonical(
                canonical,
                "material_structure",
                MaterialFingerprint.structure(materials, prefixes));
        materials.stream()
                .sorted(Comparator.comparing(MaterialDefinition::id))
                .forEach(material -> {
                    appendCanonical(canonical, "material", material.id());
                    appendCanonical(canonical, "tier", Integer.toString(material.tier()));
                    List<MaterialPrefix> forms = registeredForms.get(material.id());
                    if (forms == null) {
                        throw new IllegalArgumentException(
                                "Missing registered forms for material " + material.id());
                    }
                    forms.forEach(form ->
                            appendCanonical(canonical, "registered_form", form.serializedId()));
                    appendCanonical(canonical, "registered_form_end", material.id());
                });
        prefixes.stream()
                .sorted(Comparator.comparing(prefix -> prefix.prefix().serializedId()))
                .forEach(prefix -> {
                    appendCanonical(canonical, "prefix", prefix.prefix().serializedId());
                    appendCanonical(canonical, "serialized_path", prefix.serializedPath());
                    appendCanonical(canonical, "tag_directory", prefix.tagDirectory());
                    appendCanonical(canonical, "tag_namespace", prefix.tagNamespace());
                    appendCanonical(canonical, "model_template", prefix.modelTemplate());
                    appendCanonical(canonical, "model_texture", prefix.modelTexture());
                });
        return sha256(canonical.toString().getBytes(StandardCharsets.UTF_8));
    }

    static Path requireGeneratedRoot(Path root, String side) {
        Path normalized = root.toAbsolutePath().normalize();
        Path expectedSuffix = Path.of(".generated-material-pack", side);
        if (!normalized.endsWith(expectedSuffix)) {
            throw new IllegalArgumentException(
                    "Generated material pack " + side + " root must end with '"
                            + expectedSuffix + "': " + normalized);
        }
        return normalized;
    }

    private static void appendCanonical(
            StringBuilder canonical,
            String name,
            String value) {
        canonical.append(name)
                .append('=')
                .append(value.length())
                .append(':')
                .append(value)
                .append('\n');
    }

    private static String outputGeneratorIdentity() {
        return outputGeneratorIdentity(
                GeneratedMaterialPack.class.getResourceAsStream("GeneratedMaterialPack.class"));
    }

    static String outputGeneratorIdentity(InputStream source) {
        if (source == null) {
            throw new IllegalStateException(
                    "Generated material pack output generator class bytes are unavailable");
        }
        try (InputStream input = source) {
            MessageDigest digest = newSha256();
            byte[] buffer = new byte[8_192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return OUTPUT_SCHEMA + ":" + HexFormat.of().formatHex(digest.digest());
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to fingerprint generated material pack output generator",
                    exception);
        }
    }

    private static String sha256(byte[] input) {
        return HexFormat.of().formatHex(newSha256().digest(input));
    }

    private static MessageDigest newSha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private static List<MaterialPrefix> requireRegisteredForms(
            MaterialDefinition material,
            Map<String, List<MaterialPrefix>> registeredForms) {
        List<MaterialPrefix> forms = registeredForms.get(material.id());
        if (forms == null) {
            throw new IllegalArgumentException(
                    "Missing registered forms for material " + material.id());
        }
        return forms;
    }

    private static String canonicalItemId(
            MaterialDefinition material, MaterialPrefix form) {
        if (form.equals(MaterialPrefixes.ORE)) {
            return ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, material.id() + "_ore").toString();
        }
        String override = material.formItems().get(form);
        if (override != null) {
            return override;
        }
        return ResourceLocation.fromNamespaceAndPath(
                CrucibleCraft.MODID, material.registryName(form)).toString();
    }

    private static List<String> oreIds(String materialId) {
        return List.of(
                CrucibleCraft.MODID + ":" + materialId + "_ore",
                CrucibleCraft.MODID + ":deepslate_" + materialId + "_ore");
    }

    private static String title(String id) {
        String spaced = id.replace('_', ' ');
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    private static void addOreClientFiles(Map<String, String> files, String materialId) {
        for (String path : List.of(
                materialId + "_ore",
                "deepslate_" + materialId + "_ore")) {
            String modelId = CrucibleCraft.MODID + ":block/" + path;
            JsonObject blockState = new JsonObject();
            JsonObject variants = new JsonObject();
            JsonObject defaultVariant = new JsonObject();
            defaultVariant.addProperty("model", modelId);
            variants.add("", defaultVariant);
            blockState.add("variants", variants);
            files.put(
                    "assets/" + CrucibleCraft.MODID + "/blockstates/" + path + ".json",
                    GSON.toJson(blockState));

            JsonObject blockModel = new JsonObject();
            blockModel.addProperty(
                    "parent",
                    path.startsWith("deepslate_")
                            ? "minecraft:block/deepslate_iron_ore"
                            : "minecraft:block/iron_ore");
            files.put(
                    "assets/" + CrucibleCraft.MODID + "/models/block/" + path + ".json",
                    GSON.toJson(blockModel));

            JsonObject itemModel = new JsonObject();
            itemModel.addProperty("parent", modelId);
            files.put(
                    "assets/" + CrucibleCraft.MODID + "/models/item/" + path + ".json",
                    GSON.toJson(itemModel));
        }
    }

    private static String oreLootTable(String oreId, String rawOreId) {
        JsonObject silkCondition = new JsonObject();
        silkCondition.addProperty("condition", "minecraft:match_tool");
        JsonObject predicate = new JsonObject();
        JsonObject predicates = new JsonObject();
        JsonArray enchantments = new JsonArray();
        JsonObject silkTouch = new JsonObject();
        silkTouch.addProperty("enchantments", "minecraft:silk_touch");
        JsonObject levels = new JsonObject();
        levels.addProperty("min", 1);
        silkTouch.add("levels", levels);
        enchantments.add(silkTouch);
        predicates.add("minecraft:enchantments", enchantments);
        predicate.add("predicates", predicates);
        silkCondition.add("predicate", predicate);

        JsonObject silkEntry = new JsonObject();
        silkEntry.addProperty("type", "minecraft:item");
        JsonArray silkConditions = new JsonArray();
        silkConditions.add(silkCondition);
        silkEntry.add("conditions", silkConditions);
        silkEntry.addProperty("name", oreId);

        JsonObject rawEntry = new JsonObject();
        rawEntry.addProperty("type", "minecraft:item");
        rawEntry.addProperty("name", rawOreId);
        JsonObject fortune = new JsonObject();
        fortune.addProperty("function", "minecraft:apply_bonus");
        fortune.addProperty("enchantment", "minecraft:fortune");
        fortune.addProperty("formula", "minecraft:ore_drops");
        JsonArray functions = new JsonArray();
        functions.add(fortune);
        rawEntry.add("functions", functions);

        JsonObject alternatives = new JsonObject();
        alternatives.addProperty("type", "minecraft:alternatives");
        JsonArray children = new JsonArray();
        children.add(silkEntry);
        children.add(rawEntry);
        alternatives.add("children", children);

        JsonObject pool = new JsonObject();
        pool.addProperty("rolls", 1);
        pool.addProperty("bonus_rolls", 0);
        JsonArray entries = new JsonArray();
        entries.add(alternatives);
        pool.add("entries", entries);
        JsonObject survivesExplosion = new JsonObject();
        survivesExplosion.addProperty("condition", "minecraft:survives_explosion");
        JsonArray conditions = new JsonArray();
        conditions.add(survivesExplosion);
        pool.add("conditions", conditions);

        JsonObject root = new JsonObject();
        root.addProperty("type", "minecraft:block");
        JsonArray pools = new JsonArray();
        pools.add(pool);
        root.add("pools", pools);
        root.addProperty(
                "random_sequence",
                CrucibleCraft.MODID + ":blocks/" + ResourceLocation.parse(oreId).getPath());
        return GSON.toJson(root);
    }

    private static void addTag(
            Map<String, String> files, String path, List<String> values) {
        JsonObject tag = new JsonObject();
        tag.addProperty("replace", false);
        JsonArray entries = new JsonArray();
        values.forEach(entries::add);
        tag.add("values", entries);
        files.put(path, GSON.toJson(tag));
    }

    private static String packMeta(int format) {
        JsonObject pack = new JsonObject();
        pack.addProperty("pack_format", format);
        pack.addProperty("description", "Generated CrucibleCraft material resources");
        JsonObject rootObject = new JsonObject();
        rootObject.add("pack", pack);
        return GSON.toJson(rootObject);
    }

    private record Roots(Path server, Path client) {}

}
