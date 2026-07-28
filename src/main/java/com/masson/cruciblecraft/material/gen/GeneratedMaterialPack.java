package com.masson.cruciblecraft.material.gen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialForm;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.MaterialDefinition;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.neoforge.event.AddPackFindersEvent;

public final class GeneratedMaterialPack {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path serverRoot;
    private static Path clientRoot;

    private GeneratedMaterialPack() {}

    public static void initialize(Path configRoot) {
        Path generatedRoot = configRoot.resolve(".generated-material-pack");
        serverRoot = generatedRoot.resolve("server");
        clientRoot = generatedRoot.resolve("client");
        try {
            recreate(serverRoot);
            recreate(clientRoot);
            writePackMeta(serverRoot, 48);
            writePackMeta(clientRoot, 34);
            writeTags(serverRoot);
            writeModels(clientRoot);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to generate material resource pack", exception);
        }
    }

    public static void addPackFinders(AddPackFindersEvent event) {
        Path root = switch (event.getPackType()) {
            case SERVER_DATA -> serverRoot;
            case CLIENT_RESOURCES -> clientRoot;
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

    private static void writeTags(Path root) throws IOException {
        Map<String, List<String>> aggregateTags = new LinkedHashMap<>();
        for (MaterialDefinition material : MaterialCatalog.values()) {
            List<String> materialItems = new ArrayList<>();
            for (MaterialForm form : material.forms()) {
                String formTag = tagDirectory(form);
                String itemId = MaterialLookup.itemId(material.id(), form).orElseThrow().toString();
                writeTag(
                        root.resolve("data/c/tags/item")
                                .resolve(formTag)
                                .resolve(material.tagName() + ".json"),
                        List.of(itemId));
                aggregateTags.computeIfAbsent(formTag, ignored -> new ArrayList<>())
                        .add("#c:" + formTag + "/" + material.tagName());
                materialItems.add(itemId);
            }
            writeTag(
                    root.resolve("data/" + CrucibleCraft.MODID + "/tags/item/materials")
                            .resolve(material.id() + ".json"),
                    materialItems);
        }
        for (var aggregate : aggregateTags.entrySet()) {
            writeTag(
                    root.resolve("data/c/tags/item").resolve(aggregate.getKey() + ".json"),
                    aggregate.getValue());
        }
    }

    private static void writeModels(Path root) throws IOException {
        for (MaterialDefinition material : MaterialCatalog.values()) {
            for (MaterialForm form : material.forms()) {
                if (material.formItems().containsKey(form)) {
                    continue;
                }
                JsonObject model = new JsonObject();
                model.addProperty("parent", "minecraft:item/generated");
                JsonObject textures = new JsonObject();
                textures.addProperty("layer0", vanillaTexture(form));
                model.add("textures", textures);
                writeJson(
                        root.resolve("assets/" + CrucibleCraft.MODID + "/models/item")
                                .resolve(material.registryName(form) + ".json"),
                        model);
            }
        }
    }

    private static void writeTag(Path path, List<String> values) throws IOException {
        JsonObject tag = new JsonObject();
        tag.addProperty("replace", false);
        JsonArray entries = new JsonArray();
        values.forEach(entries::add);
        tag.add("values", entries);
        writeJson(path, tag);
    }

    private static void writePackMeta(Path root, int format) throws IOException {
        JsonObject pack = new JsonObject();
        pack.addProperty("pack_format", format);
        pack.addProperty("description", "Generated CrucibleCraft material resources");
        JsonObject rootObject = new JsonObject();
        rootObject.add("pack", pack);
        writeJson(root.resolve("pack.mcmeta"), rootObject);
    }

    private static void writeJson(Path path, JsonObject json) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, GSON.toJson(json));
    }

    private static void recreate(Path root) throws IOException {
        if (Files.exists(root)) {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        }
        Files.createDirectories(root);
    }

    private static String tagDirectory(MaterialForm form) {
        return switch (form) {
            case SMALL_DUST -> "small_dusts";
            default -> form.serializedName() + "s";
        };
    }

    private static String vanillaTexture(MaterialForm form) {
        return switch (form) {
            case INGOT -> "minecraft:item/iron_ingot";
            case DUST, SMALL_DUST -> "minecraft:item/gunpowder";
            case RAW_ORE -> "minecraft:item/raw_iron";
            case CRUSHED_ORE -> "minecraft:item/flint";
            case NUGGET -> "minecraft:item/iron_nugget";
            case BLOCK -> "minecraft:block/iron_block";
            case PLATE -> "minecraft:item/paper";
            case ROD -> "minecraft:item/bone";
            case BOLT -> "minecraft:item/flint";
        };
    }
}
