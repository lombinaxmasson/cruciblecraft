package com.masson.cruciblecraft.recipe.gt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;

/**
 * Shared generated-family loader used by equivalence tests and the T37
 * 1x=50 measurement harness. Keep the path identical so both readers
 * cannot drift onto a second tree.
 */
final class CompactGTRecipeFamilyGeneratedSupport {
    private CompactGTRecipeFamilyGeneratedSupport() {}

    static Path generatedRoot() {
        return Path.of(System.getProperty("user.dir"))
                .resolve(
                        "src/t37_recipe_generated/resources/data/cruciblecraft/recipe/t37/assembler");
    }

    static List<JsonObject> loadGeneratedFamilies() throws IOException {
        Path root = generatedRoot();
        if (!Files.isDirectory(root)) {
            throw new IOException("missing generated T37 root " + root);
        }
        List<JsonObject> documents = new ArrayList<>();
        try (Stream<Path> paths = Files.list(root)) {
            paths.filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .forEach(path -> documents.add(readJson(path)));
        }
        return documents;
    }

    static CompactRecipeFamilySource sourceFromGenerated(
            JsonObject document,
            RegistryAccess registries) {
        String filename = document.get("family_id").getAsString()
                .replace('.', '_')
                .replace('#', '_');
        ResourceLocation authoredId = ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft",
                "t37/assembler/" + filename);
        var ops = RegistryOps.create(JsonOps.INSTANCE, registries);
        JsonObject relationJson = document.getAsJsonArray("relations")
                .get(0)
                .getAsJsonObject();
        List<Ingredient> inputs = new ArrayList<>();
        for (JsonElement element : relationJson.getAsJsonArray("item_inputs")) {
            inputs.add(ingredientFromGenerated(element.getAsJsonObject(), ops));
        }
        List<Integer> counts = ints(relationJson.getAsJsonArray("item_input_counts"));
        List<ItemInputAction> actions = new ArrayList<>();
        for (JsonElement element : relationJson.getAsJsonArray("item_input_actions")) {
            actions.add(actionFromGenerated(element.getAsJsonObject()));
        }
        List<ItemStack> outputs = new ArrayList<>();
        for (JsonElement element : relationJson.getAsJsonArray("item_outputs")) {
            outputs.add(ItemStack.STRICT_CODEC.parse(ops, element).getOrThrow());
        }
        JsonObject provenanceJson = relationJson.getAsJsonObject("provenance");
        List<String> hashes = new ArrayList<>();
        for (JsonElement element : provenanceJson.getAsJsonArray("evidence_hashes")) {
            hashes.add(element.getAsString());
        }
        CompactGTRecipeFamilyDefinition.Relation relation =
                new CompactGTRecipeFamilyDefinition.Relation(
                        ResourceLocation.parse(relationJson.get("stable_id").getAsString()),
                        inputs,
                        counts,
                        actions,
                        outputs,
                        List.of(),
                        List.of(),
                        ints(relationJson.getAsJsonArray("output_chances")),
                        relationJson.get("duration").getAsInt(),
                        relationJson.get("eut").getAsLong(),
                        relationJson.get("special_value").getAsLong(),
                        relationJson.get("can_be_buffered").getAsBoolean(),
                        relationJson.get("shadow_order").getAsInt(),
                        new GTRecipeProvenance(
                                provenanceJson.get("source_kind").getAsString(),
                                Optional.of(provenanceJson
                                        .get("selected_source_recipe")
                                        .getAsString()),
                                hashes));
        return new CompactRecipeFamilySource(
                authoredId,
                new CompactGTRecipeFamilyDefinition(
                        document.get("family_id").getAsString(),
                        ResourceLocation.parse(document.get("target_map").getAsString()),
                        document.get("source_revision").getAsString(),
                        List.of(relation)));
    }

    private static JsonObject readJson(Path path) {
        try {
            return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        } catch (IOException exception) {
            throw new IllegalStateException(path.toString(), exception);
        }
    }

    private static Ingredient ingredientFromGenerated(
            JsonObject ingredient,
            RegistryOps<JsonElement> ops) {
        if (ingredient.has("type")
                && "neoforge:components".equals(ingredient.get("type").getAsString())) {
            int config = ingredient.getAsJsonObject("components")
                    .get("cruciblecraft:circuit_config")
                    .getAsInt();
            ItemStack named = new ItemStack(Items.PAPER);
            named.set(DataComponents.CUSTOM_NAME, Component.literal("circuit:" + config));
            return DataComponentIngredient.of(false, named);
        }
        return Ingredient.CODEC_NONEMPTY.parse(ops, ingredient).getOrThrow();
    }

    private static ItemInputAction actionFromGenerated(JsonObject action) {
        ItemInputAction.Kind kind = ItemInputAction.Kind.valueOf(
                action.get("kind").getAsString().toUpperCase(Locale.ROOT));
        int damage = action.has("damage") ? action.get("damage").getAsInt() : 0;
        return kind == ItemInputAction.Kind.WEAR
                ? ItemInputAction.wear(damage)
                : new ItemInputAction(kind, 0);
    }

    private static List<Integer> ints(JsonArray array) {
        List<Integer> values = new ArrayList<>();
        array.forEach(element -> values.add(element.getAsInt()));
        return values;
    }
}
