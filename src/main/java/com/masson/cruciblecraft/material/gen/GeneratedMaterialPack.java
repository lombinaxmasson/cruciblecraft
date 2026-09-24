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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.energy.cable.ElectricalConductorCatalog;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.MaterialFingerprint;
import com.masson.cruciblecraft.material.MaterialFormHosts;
import com.masson.cruciblecraft.material.MaterialZhNames;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixDefinition;
import com.masson.cruciblecraft.content.item.BathIdentityCatalog;
import com.masson.cruciblecraft.content.item.BathMteIdentityCatalog;
import com.masson.cruciblecraft.content.item.BathRemainderBlockObjectCatalog;
import com.masson.cruciblecraft.content.item.GtBlockObjectCatalog;
import com.masson.cruciblecraft.content.item.GtBuildingBlockCatalog;
import com.masson.cruciblecraft.content.item.SemanticObjectCatalog;
import com.masson.cruciblecraft.content.item.SmelterMteIdentityCatalog;
import com.masson.cruciblecraft.localization.LanguageNames;
import com.masson.cruciblecraft.content.block.OreStoneHost;

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
    private static final String OUTPUT_SCHEMA = "generated-material-pack-v2";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String OUTPUT_GENERATOR_IDENTITY = outputGeneratorIdentity();
    private static final Set<String> ITEM_OVERLAY_TEXTURES = Set.of(
            "arrow_gt_plastic",
            "arrow_gt_wood",
            "chem_tube",
            "crushed_ore",
            "dust_div72",
            "fine_wire",
            "ingot_hot",
            "lens",
            "minecart_wheels",
            "plant_gt_berry",
            "plant_gt_blossom",
            "plant_gt_twig",
            "plate_gem",
            "ring",
            "rock",
            "storage_dust",
            "storage_plate",
            "tiny_crushed_ore");
    private static volatile Roots roots = new Roots(null, null);

    private GeneratedMaterialPack() {}

    public static void initialize(Path configRoot) {
        initialize(configRoot, FMLEnvironment.dist.isClient());
    }

    static void initialize(Path configRoot, boolean clientDistribution) {
        long initializeStarted = System.nanoTime();
        Path generatedRoot = configRoot.resolve(".generated-material-pack");
        Path serverRoot = requireGeneratedRoot(generatedRoot.resolve("server"), "server");
        GeneratedMaterialPackPlan plan =
                GeneratedMaterialPackPlan.forDistribution(clientDistribution);
        Path clientRoot = plan.clientAssets()
                ? requireGeneratedRoot(generatedRoot.resolve("client"), "client")
                : null;
        Collection<MaterialDefinition> materials = MaterialCatalog.startupValues();
        Map<String, List<MaterialPrefix>> registeredForms = registeredForms(materials);
        long serverMillis = 0L;
        long clientMillis = 0L;
        boolean serverRebuilt = false;
        boolean clientRebuilt = false;
        try {
            if (plan.serverData()) {
                long sideStarted = System.nanoTime();
                serverRebuilt = GeneratedMaterialPackCache.ensure(
                        serverRoot,
                        generationFingerprint(
                                "server",
                                materials,
                                registeredForms,
                                MaterialPrefixCatalog.definitions(),
                                SERVER_PACK_FORMAT,
                                OUTPUT_GENERATOR_IDENTITY),
                        () -> planServerFiles(materials, registeredForms));
                serverMillis = elapsedMillis(sideStarted);
            }
            if (plan.clientAssets()) {
                long sideStarted = System.nanoTime();
                clientRebuilt = GeneratedMaterialPackCache.ensure(
                        clientRoot,
                        generationFingerprint(
                                "client",
                                materials,
                                registeredForms,
                                MaterialPrefixCatalog.definitions(),
                                CLIENT_PACK_FORMAT,
                                OUTPUT_GENERATOR_IDENTITY),
                        () -> planClientFiles(materials, registeredForms));
                clientMillis = elapsedMillis(sideStarted);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to generate material resource pack", exception);
        }

        roots = new Roots(serverRoot, clientRoot);
        CrucibleCraft.LOGGER.info(
                "Generated material pack timings: materials={} server={}ms rebuilt={} "
                        + "client={}ms rebuilt={} total={}ms",
                materials.size(),
                serverMillis,
                serverRebuilt,
                clientMillis,
                clientRebuilt,
                elapsedMillis(initializeStarted));
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
                    Component.translatable("pack.cruciblecraft.generated_materials"),
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
        Map<ResourceLocation, List<String>> aggregateBlockTags = new LinkedHashMap<>();
        List<String> aggregateOreTags = new ArrayList<>();
        List<String> mineableOres = new ArrayList<>();
        List<String> mineableShovels = new ArrayList<>();
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
                            oreLootTable(
                                    oreId,
                                    rawOreId,
                                    material.id(),
                                    !material.formItems()
                                            .containsKey(MaterialPrefixes.RAW_ORE))));
                    continue;
                }
                String itemId = canonicalItemId(material, form);
                boolean uniqueBlock = electricalSpecification(material, form) != null
                        || pipeModelKey(material, form) != null
                        || isPlaceableStorage(material, form)
                        || isPlaceableCasing(material, form)
                        || isRockForm(form)
                        || isRedstoneWire(material, form);
                boolean publicExchange = MaterialFormHosts.isPublicExchangePrefix(form)
                        && !material.formItems().containsKey(form);
                boolean sharedInventory = !material.formItems().containsKey(form)
                        && !uniqueBlock
                        && !publicExchange;
                if (!sharedInventory) {
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
                } else {
                    String prefixId = prefixItemId(form);
                    List<String> aggregate = aggregateItemTags.computeIfAbsent(
                            ResourceLocation.fromNamespaceAndPath(tagNamespace, formTag),
                            ignored -> new ArrayList<>());
                    if (!aggregate.contains(prefixId)) {
                        aggregate.add(prefixId);
                    }
                }
                String electricalSpecification =
                        electricalSpecification(material, form);
                String pipeModelKey = pipeModelKey(material, form);
                boolean placeableStorage = isPlaceableStorage(material, form);
                boolean placeableCasing = isPlaceableCasing(material, form);
                boolean redstoneWire = isRedstoneWire(material, form);
                if (electricalSpecification != null
                        || pipeModelKey != null
                        || placeableStorage
                        || placeableCasing
                        || isRockForm(form)
                        || redstoneWire) {
                    addTag(
                            files,
                            "data/" + tagNamespace + "/tags/block/" + formTag
                                    + "/" + material.tagName() + ".json",
                            List.of(itemId));
                    aggregateBlockTags.computeIfAbsent(
                                    ResourceLocation.fromNamespaceAndPath(
                                            tagNamespace, formTag),
                                    ignored -> new ArrayList<>())
                            .add("#" + tagNamespace + ":" + formTag + "/"
                                    + material.tagName());
                    if (form.equals(MaterialPrefixes.STORAGE_DUST)) {
                        mineableShovels.add(itemId);
                    } else {
                        mineableOres.add(itemId);
                        if (placeableStorage || placeableCasing) {
                            (material.tier() >= 2 ? ironToolOres : stoneToolOres)
                                    .add(itemId);
                        }
                    }
                    files.put(
                            "data/" + CrucibleCraft.MODID
                                    + "/loot_table/blocks/"
                                    + ResourceLocation.parse(itemId).getPath()
                                    + ".json",
                            selfDropLootTable(itemId));
                }
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
        for (var aggregate : aggregateBlockTags.entrySet()) {
            addTag(
                    files,
                    "data/" + aggregate.getKey().getNamespace() + "/tags/block/"
                            + aggregate.getKey().getPath() + ".json",
                    aggregate.getValue());
        }
        addTag(files, "data/c/tags/block/ores.json", aggregateOreTags);
        addTag(files, "data/minecraft/tags/block/mineable/pickaxe.json", mineableOres);
        addTag(files, "data/minecraft/tags/block/mineable/shovel.json", mineableShovels);
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
        LinkedHashSet<MaterialPrefix> sharedPrefixModels = new LinkedHashSet<>();
        for (MaterialDefinition material : materials) {
            for (MaterialPrefix form : requireRegisteredForms(material, registeredForms)) {
                if (form.equals(MaterialPrefixes.ORE)) {
                    addOreClientFiles(files, material.id());
                    putBlockEnglish(
                            oreTranslations,
                            material.id() + "_ore",
                            LanguageNames.composeEnglish(material.id(), "ore"));
                    putBlockEnglish(
                            oreTranslations,
                            "deepslate_" + material.id() + "_ore",
                            "Deepslate "
                                    + LanguageNames.formatEnglishId(material.id())
                                    + " Ore");
                    continue;
                }
                if (material.formItems().containsKey(form)) {
                    continue;
                }
                if (isPlaceableStorage(material, form)) {
                    addStorageClientFiles(files, material, form);
                    putBlockEnglish(
                            oreTranslations,
                            material.registryName(form),
                            LanguageNames.composeEnglish(
                                    material.id(), form.serializedName()));
                    continue;
                }
                if (isPlaceableCasing(material, form)) {
                    addCasingClientFiles(files, material, form);
                    putBlockEnglish(
                            oreTranslations,
                            material.registryName(form),
                            LanguageNames.composeEnglish(
                                    material.id(), form.serializedName()));
                    continue;
                }
                if (isRockForm(form)) {
                    addRockClientFiles(files, material);
                    putBlockEnglish(
                            oreTranslations,
                            material.registryName(form),
                            LanguageNames.composeEnglish(
                                    material.id(), form.serializedName()));
                    continue;
                }
                if (isRedstoneWire(material, form)) {
                    JsonObject model = new JsonObject();
                    boolean insulated = com.masson.cruciblecraft.content
                            .redstonewire.RedstoneWireKind
                            .byPath(material.registryName(form))
                            .map(com.masson.cruciblecraft.content.redstonewire
                                    .RedstoneWireKind::insulated)
                            .orElse(false);
                    model.addProperty(
                            "parent",
                            CrucibleCraft.MODID
                                    + (insulated
                                            ? ":block/redstone_cable/item"
                                            : ":block/redstone_wire/item"));
                    files.put(
                            "assets/" + CrucibleCraft.MODID + "/models/item/"
                                    + material.registryName(form) + ".json",
                            GSON.toJson(model));
                    continue;
                }
                String electricalSpecification =
                        electricalSpecification(material, form);
                if (electricalSpecification != null) {
                    JsonObject model = new JsonObject();
                    model.addProperty(
                            "parent",
                            CrucibleCraft.MODID + ":conductor/"
                                    + electricalSpecification.toLowerCase(
                                            java.util.Locale.ROOT)
                                    + "_item");
                    oreTranslations.addProperty(
                            LanguageNames.translationKey(
                                    "block", material.registryName(form)),
                            LanguageNames.composeEnglish(
                                    material.id(), form.serializedName()));
                    files.put(
                            "assets/" + CrucibleCraft.MODID + "/models/item/"
                                    + material.registryName(form) + ".json",
                            GSON.toJson(model));
                    continue;
                }
                String pipeModelKey = pipeModelKey(material, form);
                if (pipeModelKey != null) {
                    JsonObject model = new JsonObject();
                    model.addProperty(
                            "parent",
                            CrucibleCraft.MODID + ":pipe/"
                                    + pipeModelKey + "_item");
                    oreTranslations.addProperty(
                            LanguageNames.translationKey(
                                    "block", material.registryName(form)),
                            LanguageNames.composeEnglish(
                                    material.id(), form.serializedName()));
                    files.put(
                            "assets/" + CrucibleCraft.MODID + "/models/item/"
                                    + material.registryName(form) + ".json",
                            GSON.toJson(model));
                    continue;
                }
                if (MaterialFormHosts.isPublicExchangePrefix(form)) {
                    addPrefixItemModel(files, form, material.registryName(form));
                    continue;
                }
                sharedPrefixModels.add(form);
            }
        }
        for (MaterialPrefix form : sharedPrefixModels) {
            addPrefixItemModel(files, form, form.serializedName());
        }
        JsonObject zhTranslations = new JsonObject();
        for (MaterialDefinition material : materials) {
            MaterialZhNames.material(material.id()).ifPresent(mat -> {
                zhTranslations.addProperty(
                        material.translationKey(), mat);
                for (MaterialPrefix form : requireRegisteredForms(
                        material, registeredForms)) {
                    MaterialZhNames.pipe(form.serializedName())
                            .ifPresent(name -> putBlockChinese(
                                    zhTranslations,
                                    material.registryName(form),
                                    mat + name));
                    MaterialZhNames.conductor(form.serializedName())
                            .ifPresent(name -> putBlockChinese(
                                    zhTranslations,
                                    material.registryName(form),
                                    mat + name));
                    if (isPlaceableStorage(material, form)
                            || isPlaceableCasing(material, form)) {
                        MaterialZhNames.prefix(form.serializedName())
                                .ifPresent(name -> putBlockChinese(
                                        zhTranslations,
                                        material.registryName(form),
                                        mat + name));
                    }
                    if (form.equals(MaterialPrefixes.ORE)) {
                        putBlockChinese(
                                zhTranslations,
                                material.id() + "_ore",
                                mat + "矿石");
                        putBlockChinese(
                                zhTranslations,
                                "deepslate_" + material.id() + "_ore",
                                "深板岩" + mat + "矿石");
                    }
                }
            });
        }
        addCatalogClientAssets(files, oreTranslations, zhTranslations);
        addEmiTagIndexSupport(
                files, materials, registeredForms, oreTranslations, zhTranslations);
        files.put(
                "assets/" + CrucibleCraft.MODID + "/lang/en_us.json",
                GSON.toJson(oreTranslations));
        files.put(
                "assets/" + CrucibleCraft.MODID + "/lang/zh_cn.json",
                GSON.toJson(zhTranslations));
        return Map.copyOf(files);
    }

    private static void addCatalogClientAssets(
            Map<String, String> files,
            JsonObject english,
            JsonObject chinese) {
        if (MaterialPrefixCatalog.isBootstrapped()) {
            for (MaterialPrefix form : MaterialPrefixCatalog.values()) {
                String key = "item.cruciblecraft.material_form." + form.serializedName();
                english.addProperty(
                        key,
                        LanguageNames.englishFormTemplate(
                                LanguageNames.formatEnglishId(form.serializedName())));
                MaterialZhNames.prefix(form.serializedName()).ifPresent(name ->
                        chinese.addProperty(
                                key, LanguageNames.chineseFormTemplate(name)));
            }
        }
        addIdentityLang(english, chinese, BathIdentityCatalog.identities(), identity ->
                new CatalogLang(
                        identity.registryPath(),
                        identity.englishName(),
                        identity.chineseName(),
                        false));
        addIdentityLang(english, chinese, SemanticObjectCatalog.identities(), identity ->
                new CatalogLang(
                        identity.registryPath(),
                        identity.englishName(),
                        identity.chineseName(),
                        false));
        addIdentityLang(english, chinese, BathMteIdentityCatalog.newItems(), identity ->
                new CatalogLang(
                        identity.registryPath(),
                        identity.englishName(),
                        identity.chineseName(),
                        false));
        addIdentityLang(english, chinese, SmelterMteIdentityCatalog.newItems(), identity ->
                new CatalogLang(
                        identity.registryPath(),
                        identity.englishName(),
                        identity.chineseName(),
                        false));
        addBlockObjectAssets(files, english, chinese, GtBlockObjectCatalog.variants());
        addBlockObjectAssets(
                files, english, chinese, BathRemainderBlockObjectCatalog.variants());
        addBlockObjectAssets(
                files, english, chinese, GtBuildingBlockCatalog.variants());
    }

    private static <T> void addIdentityLang(
            JsonObject english,
            JsonObject chinese,
            Collection<T> identities,
            java.util.function.Function<T, CatalogLang> view) {
        for (T identity : identities) {
            CatalogLang lang = view.apply(identity);
            String itemKey = LanguageNames.translationKey("item", lang.registryPath());
            english.addProperty(
                    itemKey,
                    LanguageNames.playerEnglish(
                            lang.englishName(), lang.registryPath()));
            LanguageNames.playerChinese(lang.chineseName(), lang.registryPath())
                    .or(() -> LanguageNames.chineseOrEmpty(
                            lang.chineseName(), lang.englishName()))
                    .or(() -> LanguageNames.composeMaterialFormZh(lang.registryPath()))
                    .ifPresent(name -> chinese.addProperty(itemKey, name));
            if (lang.block()) {
                String blockKey = LanguageNames.translationKey(
                        "block", lang.registryPath());
                english.addProperty(
                        blockKey,
                        LanguageNames.playerEnglish(
                                lang.englishName(), lang.registryPath()));
                LanguageNames.playerChinese(lang.chineseName(), lang.registryPath())
                        .or(() -> LanguageNames.chineseOrEmpty(
                                lang.chineseName(), lang.englishName()))
                        .or(() -> LanguageNames.composeMaterialFormZh(
                                lang.registryPath()))
                        .ifPresent(name -> chinese.addProperty(blockKey, name));
            }
        }
    }

    private static void addBlockObjectAssets(
            Map<String, String> files,
            JsonObject english,
            JsonObject chinese,
            Collection<GtBlockObjectCatalog.Variant> variants) {
        for (GtBlockObjectCatalog.Variant variant : variants) {
            addIdentityLang(
                    english,
                    chinese,
                    List.of(new CatalogLang(
                            variant.registryPath(),
                            variant.englishName(),
                            variant.chineseName(),
                            true)),
                    value -> value);
        }
    }

    private record CatalogLang(
            String registryPath,
            String englishName,
            String chineseName,
            boolean block) {}

    private static void putBlockEnglish(
            JsonObject english, String registryPath, String name) {
        english.addProperty(LanguageNames.translationKey("block", registryPath), name);
    }

    private static void putBlockChinese(
            JsonObject chinese, String registryPath, String name) {
        if (name == null || name.isBlank() || LanguageNames.isEnglishCopy(name)) {
            return;
        }
        chinese.addProperty(LanguageNames.translationKey("block", registryPath), name);
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

    private static long elapsedMillis(long started) {
        return (System.nanoTime() - started) / 1_000_000L;
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
        if (isUniqueBlockForm(material, form)
                || MaterialFormHosts.isPublicExchangePrefix(form)) {
            return ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, material.registryName(form)).toString();
        }
        return prefixItemId(form);
    }

    private static boolean isUniqueBlockForm(
            MaterialDefinition material, MaterialPrefix form) {
        return electricalSpecification(material, form) != null
                || pipeModelKey(material, form) != null
                || isPlaceableStorage(material, form)
                || isPlaceableCasing(material, form)
                || isRockForm(form)
                || isRedstoneWire(material, form);
    }

    private static String prefixItemId(MaterialPrefix form) {
        return ResourceLocation.fromNamespaceAndPath(
                CrucibleCraft.MODID, form.serializedName()).toString();
    }

    private static List<String> oreIds(String materialId) {
        return List.of(
                CrucibleCraft.MODID + ":" + materialId + "_ore",
                CrucibleCraft.MODID + ":deepslate_" + materialId + "_ore");
    }

    private static boolean isRockForm(MaterialPrefix form) {
        return com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog
                .require("rock")
                .equals(form);
    }

    private static void addPrefixItemModel(
            Map<String, String> files, MaterialPrefix form, String path) {
        JsonObject model = new JsonObject();
        String wireBundle = wireBundleItemParent(form);
        if (wireBundle != null) {
            model.addProperty("parent", CrucibleCraft.MODID + ":" + wireBundle);
        } else {
            MaterialPrefixDefinition prefix = MaterialPrefixCatalog.definition(form);
            model.addProperty("parent", prefix.modelTemplate());
            JsonObject textures = new JsonObject();
            textures.addProperty("layer0", prefix.modelTexture());
            String overlay = overlayLayer(prefix.modelTexture());
            if (overlay != null) {
                textures.addProperty("layer1", overlay);
            }
            model.add("textures", textures);
        }
        files.put(
                "assets/" + CrucibleCraft.MODID + "/models/item/"
                        + path + ".json",
                GSON.toJson(model));
    }

    private static void addRockClientFiles(
            Map<String, String> files, MaterialDefinition material) {
        String path = material.registryName(
                com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog
                        .require("rock"));
        String modelId = CrucibleCraft.MODID + ":block/material_rock";
        JsonObject blockState = new JsonObject();
        JsonObject variants = new JsonObject();
        JsonObject defaultVariant = new JsonObject();
        defaultVariant.addProperty("model", modelId);
        variants.add("", defaultVariant);
        blockState.add("variants", variants);
        files.put(
                "assets/" + CrucibleCraft.MODID + "/blockstates/" + path + ".json",
                GSON.toJson(blockState));
        JsonObject itemModel = new JsonObject();
        itemModel.addProperty("parent", "minecraft:item/generated");
        JsonObject textures = new JsonObject();
        textures.addProperty("layer0", CrucibleCraft.MODID + ":item/material/rock");
        String overlay = overlayLayer(CrucibleCraft.MODID + ":item/material/rock");
        if (overlay != null) {
            textures.addProperty("layer1", overlay);
        }
        itemModel.add("textures", textures);
        files.put(
                "assets/" + CrucibleCraft.MODID + "/models/item/" + path + ".json",
                GSON.toJson(itemModel));
    }

    private static void addStorageClientFiles(
            Map<String, String> files,
            MaterialDefinition material,
            MaterialPrefix form) {
        String model;
        if (form.equals(MaterialPrefixes.STORAGE_DUST)) {
            model = CrucibleCraft.MODID + ":block/material_storage_dust";
        } else if (form.equals(MaterialPrefixes.STORAGE_PLATE)) {
            model = CrucibleCraft.MODID + ":block/material_storage_plate";
        } else {
            model = CrucibleCraft.MODID + ":block/material_storage";
        }
        addSharedCubeClientFiles(files, material.registryName(form), model);
    }

    private static void addCasingClientFiles(
            Map<String, String> files,
            MaterialDefinition material,
            MaterialPrefix form) {
        addSharedCubeClientFiles(
                files,
                material.registryName(form),
                CrucibleCraft.MODID + ":block/" + form.serializedName());
    }

    private static void addSharedCubeClientFiles(
            Map<String, String> files, String path, String modelId) {
        JsonObject blockState = new JsonObject();
        JsonObject variants = new JsonObject();
        JsonObject defaultVariant = new JsonObject();
        defaultVariant.addProperty("model", modelId);
        variants.add("", defaultVariant);
        blockState.add("variants", variants);
        files.put(
                "assets/" + CrucibleCraft.MODID + "/blockstates/" + path + ".json",
                GSON.toJson(blockState));
        JsonObject itemModel = new JsonObject();
        itemModel.addProperty("parent", modelId);
        files.put(
                "assets/" + CrucibleCraft.MODID + "/models/item/" + path + ".json",
                GSON.toJson(itemModel));
    }

    private static void addOreClientFiles(Map<String, String> files, String materialId) {
        for (String path : List.of(
                materialId + "_ore",
                "deepslate_" + materialId + "_ore")) {
            String modelId = CrucibleCraft.MODID + ":block/" + path;
            JsonObject blockState = new JsonObject();
            JsonObject variants = new JsonObject();
            if (path.startsWith("deepslate_")) {
                JsonObject defaultVariant = new JsonObject();
                defaultVariant.addProperty("model", modelId);
                variants.add("", defaultVariant);
            } else {
                for (OreStoneHost host : OreStoneHost.uniqueOverworldHosts()) {
                    JsonObject variant = new JsonObject();
                    variant.addProperty(
                            "model",
                            host == OreStoneHost.STONE ? modelId : host.stoneModel());
                    variants.add("host=" + host.getSerializedName(), variant);
                }
            }
            blockState.add("variants", variants);
            files.put(
                    "assets/" + CrucibleCraft.MODID + "/blockstates/" + path + ".json",
                    GSON.toJson(blockState));

            JsonObject blockModel = new JsonObject();
            blockModel.addProperty(
                    "parent",
                    CrucibleCraft.MODID + ":block/material_ore");
            if (path.startsWith("deepslate_")) {
                JsonObject textures = new JsonObject();
                textures.addProperty("base", "minecraft:block/deepslate");
                blockModel.add("textures", textures);
            }
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

    private static String oreLootTable(
            String oreId,
            String rawOreId,
            String materialId,
            boolean prefixRawOre) {
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
        JsonArray functions = new JsonArray();
        if (prefixRawOre) {
            JsonObject setComponents = new JsonObject();
            setComponents.addProperty("function", "minecraft:set_components");
            JsonObject components = new JsonObject();
            components.addProperty("cruciblecraft:prefix_material", materialId);
            setComponents.add("components", components);
            functions.add(setComponents);
        }
        JsonObject fortune = new JsonObject();
        fortune.addProperty("function", "minecraft:apply_bonus");
        fortune.addProperty("enchantment", "minecraft:fortune");
        fortune.addProperty("formula", "minecraft:ore_drops");
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

    private static String selfDropLootTable(String blockId) {
        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", blockId);
        JsonArray entries = new JsonArray();
        entries.add(entry);
        JsonObject pool = new JsonObject();
        pool.addProperty("rolls", 1);
        pool.addProperty("bonus_rolls", 0);
        pool.add("entries", entries);
        JsonArray pools = new JsonArray();
        pools.add(pool);
        JsonObject root = new JsonObject();
        root.addProperty("type", "minecraft:block");
        root.add("pools", pools);
        root.addProperty(
                "random_sequence",
                CrucibleCraft.MODID + ":blocks/"
                        + ResourceLocation.parse(blockId).getPath());
        return GSON.toJson(root);
    }

    /**
     * Hide per-material conventional tags from EMI's tag index. Reliable EMI
     * logs every untranslated {@code c:dusts/iron}-style tag and builds a
     * tag-recipe page for it; stack groups already fold those items.
     * Parent tags such as {@code c:dusts} stay visible.
     */
    private static void addEmiTagIndexSupport(
            Map<String, String> files,
            Collection<MaterialDefinition> materials,
            Map<String, List<MaterialPrefix>> registeredForms,
            JsonObject english,
            JsonObject chinese) {
        LinkedHashSet<String> itemTags = new LinkedHashSet<>();
        LinkedHashSet<String> blockTags = new LinkedHashSet<>();
        for (MaterialDefinition material : materials) {
            List<MaterialPrefix> forms =
                    requireRegisteredForms(material, registeredForms);
            boolean anyDedicated = false;
            for (MaterialPrefix form : forms) {
                if (!writesDedicatedFormItemTag(material, form, forms)) {
                    continue;
                }
                anyDedicated = true;
                String tagId = form.tagNamespace()
                        + ":"
                        + form.tagDirectory()
                        + "/"
                        + material.tagName();
                itemTags.add(tagId);
                if (form.equals(MaterialPrefixes.ORE)
                        || isUniqueHostedBlock(material, form)) {
                    blockTags.add(tagId);
                }
            }
            if (anyDedicated) {
                itemTags.add(CrucibleCraft.MODID + ":materials/" + material.id());
            }
        }
        JsonObject exclusions = new JsonObject();
        exclusions.add("item", stringArray(itemTags));
        exclusions.add("block", stringArray(blockTags));
        files.put(
                "assets/emi/tag/exclusions/cruciblecraft.json",
                GSON.toJson(exclusions));
        addParentEmiTagNames(english, chinese);
    }

    private static void addParentEmiTagNames(
            JsonObject english, JsonObject chinese) {
        if (!MaterialPrefixCatalog.isBootstrapped()) {
            return;
        }
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (MaterialPrefix form : MaterialPrefixCatalog.values()) {
            String key = "tag.item."
                    + form.tagNamespace()
                    + "."
                    + form.tagDirectory().replace('/', '.');
            if (!seen.add(key)) {
                continue;
            }
            english.addProperty(
                    key, LanguageNames.formatEnglishId(form.tagDirectory()));
            MaterialZhNames.prefix(form.serializedName())
                    .or(() -> MaterialZhNames.pipe(form.serializedName()))
                    .or(() -> MaterialZhNames.conductor(form.serializedName()))
                    .ifPresent(name -> chinese.addProperty(key, name));
        }
    }

    private static JsonArray stringArray(Set<String> values) {
        List<String> sorted = new ArrayList<>(values);
        sorted.sort(Comparator.naturalOrder());
        JsonArray array = new JsonArray();
        for (String value : sorted) {
            array.add(value);
        }
        return array;
    }

    private static boolean writesDedicatedFormItemTag(
            MaterialDefinition material,
            MaterialPrefix form,
            List<MaterialPrefix> forms) {
        if (form.equals(MaterialPrefixes.ORE)) {
            return forms.contains(MaterialPrefixes.RAW_ORE);
        }
        return material.formItems().containsKey(form)
                || isUniqueHostedBlock(material, form)
                || (MaterialFormHosts.isPublicExchangePrefix(form)
                        && !material.formItems().containsKey(form));
    }

    private static boolean isUniqueHostedBlock(
            MaterialDefinition material, MaterialPrefix form) {
        return electricalSpecification(material, form) != null
                || pipeModelKey(material, form) != null
                || isPlaceableStorage(material, form)
                || isPlaceableCasing(material, form)
                || isRockForm(form)
                || isRedstoneWire(material, form);
    }

    private static boolean isPlaceableStorage(
            MaterialDefinition material, MaterialPrefix form) {
        return (form.equals(MaterialPrefixes.BLOCK)
                        || form.equals(MaterialPrefixes.STORAGE_DUST)
                        || form.equals(MaterialPrefixes.STORAGE_PLATE))
                && !material.formItems().containsKey(form);
    }

    private static boolean isPlaceableCasing(
            MaterialDefinition material, MaterialPrefix form) {
        return (form.equals(MaterialPrefixes.MACHINE_CASING)
                || form.equals(MaterialPrefixes.MACHINE_CASING_DOUBLE)
                || form.equals(MaterialPrefixes.MACHINE_CASING_QUADRUPLE)
                || form.equals(MaterialPrefixes.MACHINE_CASING_DENSE))
                && !material.formItems().containsKey(form);
    }

    private static String overlayLayer(String modelTexture) {
        int slash = modelTexture.lastIndexOf('/');
        if (slash < 0) {
            return null;
        }
        String basename = modelTexture.substring(slash + 1);
        return ITEM_OVERLAY_TEXTURES.contains(basename)
                ? modelTexture + "_overlay"
                : null;
    }

    private static boolean isRedstoneWire(
            MaterialDefinition material, MaterialPrefix form) {
        return com.masson.cruciblecraft.content.redstonewire.RedstoneWireKind
                .owns(material.id(), form);
    }

    private static String wireBundleItemParent(MaterialPrefix form) {
        if (form.equals(MaterialPrefixes.WIRE)) {
            return "item/material/wire_bundle_1";
        }
        if (form.equals(MaterialPrefixes.DOUBLE_WIRE)) {
            return "item/material/wire_bundle_2";
        }
        if (form.equals(MaterialPrefixes.TRIPLE_WIRE)) {
            return "item/material/wire_bundle_3";
        }
        if (form.equals(MaterialPrefixes.QUADRUPLE_WIRE)) {
            return "item/material/wire_bundle_4";
        }
        if (form.equals(MaterialPrefixes.QUINTUPLE_WIRE)) {
            return "item/material/wire_bundle_5";
        }
        if (form.equals(MaterialPrefixes.SEXTUPLE_WIRE)) {
            return "item/material/wire_bundle_6";
        }
        if (form.equals(MaterialPrefixes.SEPTUPLE_WIRE)) {
            return "item/material/wire_bundle_8";
        }
        if (form.equals(MaterialPrefixes.OCTUPLE_WIRE)) {
            return "item/material/wire_bundle_8";
        }
        if (form.equals(MaterialPrefixes.NONUPLE_WIRE)
                || form.equals(MaterialPrefixes.DECUPLE_WIRE)
                || form.equals(MaterialPrefixes.UNDECUPLE_WIRE)) {
            return "item/material/wire_bundle_12";
        }
        if (form.equals(MaterialPrefixes.DODECUPLE_WIRE)) {
            return "item/material/wire_bundle_12";
        }
        if (form.equals(MaterialPrefixes.TREDECUPLE_WIRE)
                || form.equals(MaterialPrefixes.TETRADECUPLE_WIRE)
                || form.equals(MaterialPrefixes.PENTADECUPLE_WIRE)) {
            return "item/material/wire_bundle_16";
        }
        if (form.equals(MaterialPrefixes.HEXADECUPLE_WIRE)) {
            return "item/material/wire_bundle_16";
        }
        return null;
    }

    private static String electricalSpecification(
            MaterialDefinition material, MaterialPrefix form) {
        String specification =
                ElectricalConductorCatalog.specificationFor(form);
        return specification != null
                        && material.gt6Metadata()
                                .map(metadata -> metadata
                                        .electricalBySpecification()
                                        .containsKey(specification))
                                .orElse(false)
                ? specification
                : null;
    }

    private static String pipeModelKey(
            MaterialDefinition material, MaterialPrefix form) {
        record PipeForm(String kind, String specification, int width) {}
        PipeForm pipeForm = Map.ofEntries(
                Map.entry(
                        MaterialPrefixes.TINY_FLUID_PIPE,
                        new PipeForm("fluid", "pipeTiny", 4)),
                Map.entry(
                        MaterialPrefixes.SMALL_FLUID_PIPE,
                        new PipeForm("fluid", "pipeSmall", 6)),
                Map.entry(
                        MaterialPrefixes.FLUID_PIPE,
                        new PipeForm("fluid", "pipeMedium", 8)),
                Map.entry(
                        MaterialPrefixes.LARGE_FLUID_PIPE,
                        new PipeForm("fluid", "pipeLarge", 12)),
                Map.entry(
                        MaterialPrefixes.HUGE_FLUID_PIPE,
                        new PipeForm("fluid", "pipeHuge", 16)),
                Map.entry(
                        MaterialPrefixes.QUADRUPLE_FLUID_PIPE,
                        new PipeForm("fluid", "pipeMedium", 16)),
                Map.entry(
                        MaterialPrefixes.NONUPLE_FLUID_PIPE,
                        new PipeForm("fluid", "pipeSmall", 16)),
                Map.entry(
                        MaterialPrefixes.ITEM_PIPE,
                        new PipeForm("item", "pipeMedium", 8)),
                Map.entry(
                        MaterialPrefixes.LARGE_ITEM_PIPE,
                        new PipeForm("item", "pipeLarge", 12)),
                Map.entry(
                        MaterialPrefixes.HUGE_ITEM_PIPE,
                        new PipeForm("item", "pipeHuge", 16)),
                Map.entry(
                        MaterialPrefixes.RESTRICTIVE_ITEM_PIPE,
                        new PipeForm("item", "pipeMedium", 8)),
                Map.entry(
                        MaterialPrefixes.LARGE_RESTRICTIVE_ITEM_PIPE,
                        new PipeForm("item", "pipeLarge", 12)),
                Map.entry(
                        MaterialPrefixes.HUGE_RESTRICTIVE_ITEM_PIPE,
                        new PipeForm("item", "pipeHuge", 16))).get(form);
        if (pipeForm == null) {
            return null;
        }
        if (form.equals(MaterialPrefixes.QUADRUPLE_FLUID_PIPE)
                && material.gt6Metadata()
                        .map(metadata -> metadata.pipeProperties()
                                .fluidBySpecification()
                                .containsKey("pipeMedium"))
                        .orElse(false)) {
            return "fluid_quadruple";
        }
        if (form.equals(MaterialPrefixes.NONUPLE_FLUID_PIPE)
                && material.gt6Metadata()
                        .map(metadata -> metadata.pipeProperties()
                                .fluidBySpecification()
                                .containsKey("pipeSmall"))
                        .orElse(false)) {
            return "fluid_nonuple";
        }
        if (form.equals(MaterialPrefixes.RESTRICTIVE_ITEM_PIPE)
                && material.gt6Metadata()
                        .map(metadata -> metadata.pipeProperties()
                                .itemBySpecification()
                                .containsKey("pipeMedium"))
                        .orElse(false)) {
            return "item_restrictive_8";
        }
        if (form.equals(MaterialPrefixes.LARGE_RESTRICTIVE_ITEM_PIPE)
                && material.gt6Metadata()
                        .map(metadata -> metadata.pipeProperties()
                                .itemBySpecification()
                                .containsKey("pipeLarge"))
                        .orElse(false)) {
            return "item_restrictive_12";
        }
        if (form.equals(MaterialPrefixes.HUGE_RESTRICTIVE_ITEM_PIPE)
                && material.gt6Metadata()
                        .map(metadata -> metadata.pipeProperties()
                                .itemBySpecification()
                                .containsKey("pipeHuge"))
                        .orElse(false)) {
            return "item_restrictive_16";
        }
        if (pipeForm == null) {
            return null;
        }
        return material.gt6Metadata()
                        .map(metadata -> pipeForm.kind().equals("fluid")
                                ? metadata.pipeProperties()
                                        .fluidBySpecification()
                                        .containsKey(
                                                pipeForm.specification())
                                : metadata.pipeProperties()
                                        .itemBySpecification()
                                        .containsKey(
                                                pipeForm.specification()))
                        .orElse(false)
                ? pipeForm.kind() + "_" + pipeForm.width()
                : null;
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
