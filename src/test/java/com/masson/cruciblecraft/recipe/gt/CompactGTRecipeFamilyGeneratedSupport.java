package com.masson.cruciblecraft.recipe.gt;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.masson.cruciblecraft.registry.ModComponents;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Shared generated-family loader used by equivalence tests and the T37
 * 1x=50 measurement harness. Generated roots choose the authored recipe-id
 * prefix so T37 and T38 fixtures cannot drift onto a shared namespace.
 */
final class CompactGTRecipeFamilyGeneratedSupport {
    private static final Object REGISTRY_LOCK = new Object();

    private CompactGTRecipeFamilyGeneratedSupport() {}

    static Path generatedRoot() {
        return generatedRoot("t37", "assembler");
    }

    static Path t38GeneratedRoot() {
        return generatedRoot("t38", "roaster");
    }

    static Path t39GeneratedRoot() {
        return generatedRoot("t39", "centrifuge");
    }

    static Path t39CatalogFixtureRoot() {
        return Path.of(System.getProperty("user.dir"))
                .resolve(
                        "src/test/resources/t39_catalog_fixture/data/cruciblecraft/"
                                + "recipe/t39_catalog/centrifuge");
    }

    static Path t40GeneratedRoot() {
        return generatedRoot("t40", "electrolyzer");
    }

    static Path t40CatalogFixtureRoot() {
        return Path.of(System.getProperty("user.dir"))
                .resolve(
                        "src/test/resources/t40_catalog_fixture/data/cruciblecraft/"
                                + "recipe/t40_catalog/electrolyzer");
    }

    static Path t41GeneratedRoot() {
        return generatedRoot("t41", "assembler");
    }

    static Path t41CatalogFixtureRoot() {
        return Path.of(System.getProperty("user.dir"))
                .resolve(
                        "src/test/resources/t41_catalog_fixture/data/cruciblecraft/"
                                + "recipe/t41_catalog/assembler");
    }

    static Path t43GeneratedRoot() {
        return generatedRoot("t43", "smelter");
    }

    static Path t43CatalogFixtureRoot() {
        return Path.of(System.getProperty("user.dir"))
                .resolve(
                        "src/test/resources/t43_catalog_fixture/data/cruciblecraft/"
                                + "recipe/t43_catalog/smelter");
    }

    static Path t45GeneratedRoot() {
        return Path.of(System.getProperty("user.dir"))
                .resolve(
                        "src/t45_recipe_generated/resources/data/cruciblecraft/recipe/t45");
    }

    static List<JsonObject> loadGeneratedFamiliesRecursive(Path root)
            throws IOException {
        if (!Files.isDirectory(root)) {
            throw new IOException("missing generated compact family root " + root);
        }
        List<JsonObject> documents = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(path -> path.getFileName().toString().endsWith(".json"))
                    .filter(Files::isRegularFile)
                    .sorted()
                    .forEach(path -> documents.add(readJson(path)));
        }
        return documents;
    }

    static List<CompactRecipeFamilySource> loadGeneratedSourcesRecursive(
            Path root, RegistryAccess registries) throws IOException {
        if (!Files.isDirectory(root)) {
            throw new IOException("missing generated compact family root " + root);
        }
        List<CompactRecipeFamilySource> sources = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(path -> path.getFileName().toString().endsWith(".json"))
                    .filter(Files::isRegularFile)
                    .sorted()
                    .forEach(path -> sources.add(sourceFromGenerated(
                            path, readJson(path), registries)));
        }
        return sources;
    }

    static boolean hasGeneratedFamiliesRecursive(Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            return false;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.anyMatch(path -> path.getFileName().toString().endsWith(".json")
                    && Files.isRegularFile(path));
        }
    }

    private static Path generatedRoot(String stage, String target) {
        return Path.of(System.getProperty("user.dir"))
                .resolve(
                        "src/" + stage + "_recipe_generated/resources/data/cruciblecraft/recipe/"
                                + stage + "/" + target);
    }

    static List<JsonObject> loadGeneratedFamilies() throws IOException {
        return loadGeneratedFamilies(generatedRoot());
    }

    static List<JsonObject> loadGeneratedFamilies(Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            throw new IOException("missing generated compact family root " + root);
        }
        List<JsonObject> documents = new ArrayList<>();
        try (Stream<Path> paths = Files.list(root)) {
            paths.filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .forEach(path -> documents.add(readJson(path)));
        }
        return documents;
    }

    static boolean hasGeneratedFamilies(Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            return false;
        }
        try (Stream<Path> paths = Files.list(root)) {
            return paths.anyMatch(path -> path.getFileName().toString().endsWith(".json"));
        }
    }

    static CompactRecipeFamilySource sourceFromGenerated(
            JsonObject document,
            RegistryAccess registries) {
        return sourceFromGenerated(generatedRoot(), document, registries);
    }

    static CompactRecipeFamilySource sourceFromGenerated(
            Path generatedPath,
            JsonObject document,
            RegistryAccess registries) {
        ensureGeneratedIngredientSupport();
        String filename = filename(document);
        ResourceLocation authoredId = ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft",
                authoredPrefix(generatedPath) + "/" + filename);
        var ops = RegistryOps.create(JsonOps.INSTANCE, registries);
        List<CompactGTRecipeFamilyDefinition.Relation> relations = new ArrayList<>();
        for (JsonElement element : document.getAsJsonArray("relations")) {
            relations.add(relationFromGenerated(element.getAsJsonObject(), ops));
        }
        ResourceLocation targetMap = ResourceLocation.parse(
                document.get("target_map").getAsString());
        Optional<ResourceLocation> publicationGroup = document.has("publication_group")
                ? Optional.of(ResourceLocation.parse(
                        document.get("publication_group").getAsString()))
                : Optional.empty();
        return new CompactRecipeFamilySource(
                authoredId,
                new CompactGTRecipeFamilyDefinition(
                        document.get("family_id").getAsString(),
                        targetMap,
                        document.get("source_revision").getAsString(),
                        relations,
                        parameterizedFromGenerated(document),
                        publicationGroup));
    }

    private static Optional<CompactGTRecipeFamilyDefinition.ParameterizedSpec>
            parameterizedFromGenerated(JsonObject document) {
        if (!document.has("parameterized") || !document.get("parameterized").isJsonObject()) {
            return Optional.empty();
        }
        JsonObject spec = document.getAsJsonObject("parameterized");
        return Optional.of(new CompactGTRecipeFamilyDefinition.ParameterizedSpec(
                spec.get("template").getAsString(),
                Map.of()));
    }

    private static CompactGTRecipeFamilyDefinition.Relation relationFromGenerated(
            JsonObject relationJson,
            RegistryOps<JsonElement> ops) {
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
            outputs.add(itemStackFromGenerated(element.getAsJsonObject(), ops));
        }
        List<FluidStack> fluidInputs = fluidStacks(
                relationJson.getAsJsonArray("fluid_inputs"), ops);
        List<FluidStack> fluidOutputs = fluidStacks(
                relationJson.getAsJsonArray("fluid_outputs"), ops);
        JsonObject provenanceJson = relationJson.getAsJsonObject("provenance");
        List<String> hashes = new ArrayList<>();
        for (JsonElement element : provenanceJson.getAsJsonArray("evidence_hashes")) {
            hashes.add(element.getAsString());
        }
        return new CompactGTRecipeFamilyDefinition.Relation(
                ResourceLocation.parse(relationJson.get("stable_id").getAsString()),
                inputs,
                counts,
                actions,
                outputs,
                fluidInputs,
                fluidOutputs,
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
            String itemKey = componentItemId(ingredient);
            if (itemKey != null) {
                ensureItem(ResourceLocation.parse(itemKey));
                if (BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemKey))) {
                    try {
                        return Ingredient.CODEC_NONEMPTY.parse(ops, ingredient).getOrThrow();
                    } catch (RuntimeException ignored) {
                        return componentStub(ingredient);
                    }
                }
            }
            return componentStub(ingredient);
        }
        if (ingredient.has("item")) {
            ResourceLocation itemId = ResourceLocation.parse(
                    ingredient.get("item").getAsString());
            ensureItem(itemId);
            if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
                return stubIngredient(itemId.toString());
            }
        }
        return Ingredient.CODEC_NONEMPTY.parse(ops, ingredient).getOrThrow();
    }

    private static String componentItemId(JsonObject ingredient) {
        if (ingredient.has("items") && ingredient.get("items").isJsonPrimitive()) {
            return ingredient.get("items").getAsString();
        }
        if (ingredient.has("item") && ingredient.get("item").isJsonPrimitive()) {
            return ingredient.get("item").getAsString();
        }
        return null;
    }

    private static Ingredient componentStub(JsonObject ingredient) {
        JsonObject components = ingredient.getAsJsonObject("components");
        Item item = Items.PAPER;
        String itemKey = componentItemId(ingredient);
        if (itemKey != null) {
            ResourceLocation itemId = ResourceLocation.parse(itemKey);
            ensureItem(itemId);
            if (BuiltInRegistries.ITEM.containsKey(itemId)) {
                item = BuiltInRegistries.ITEM.get(itemId);
            }
        }
        ItemStack stack = new ItemStack(item);
        if (components != null && components.has("cruciblecraft:circuit_config")) {
            stack.set(
                    ModComponents.CIRCUIT_CONFIG.get(),
                    components.get("cruciblecraft:circuit_config").getAsInt());
        }
        if (components != null && components.has("cruciblecraft:fireproof")) {
            stack.set(
                    ModComponents.FIREPROOF.get(),
                    components.get("cruciblecraft:fireproof").getAsInt());
        }
        if (components != null
                && !components.has("cruciblecraft:circuit_config")
                && !components.has("cruciblecraft:fireproof")) {
            stack.set(
                    DataComponents.CUSTOM_NAME,
                    Component.literal("components:" + ingredient));
        }
        return DataComponentIngredient.of(false, stack);
    }

    private static ItemStack itemStackFromGenerated(
            JsonObject stack,
            RegistryOps<JsonElement> ops) {
        if (stack.has("id")) {
            ResourceLocation itemId = ResourceLocation.parse(stack.get("id").getAsString());
            ensureItem(itemId);
            if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
                int count = stack.has("count") ? stack.get("count").getAsInt() : 1;
                return new ItemStack(stubItem(itemId.toString()), Math.max(1, count));
            }
        }
        return ItemStack.STRICT_CODEC.parse(ops, stack).getOrThrow();
    }

    private static Ingredient stubIngredient(String label) {
        ItemStack named = new ItemStack(Items.PAPER);
        named.set(DataComponents.CUSTOM_NAME, Component.literal(label));
        return DataComponentIngredient.of(false, named);
    }

    private static Item stubItem(String label) {
        return stubIngredient(label).getItems()[0].getItem();
    }

    private static void ensureGeneratedIngredientSupport() {
        synchronized (REGISTRY_LOCK) {
            bindComponentIngredientType();
            bindIntegerComponent(ModComponents.CIRCUIT_CONFIG);
            bindIntegerComponent(ModComponents.FIREPROOF);
        }
    }

    private static void ensureItem(ResourceLocation itemId) {
        if (BuiltInRegistries.ITEM.containsKey(itemId)
                || !"cruciblecraft".equals(itemId.getNamespace())) {
            return;
        }
        synchronized (REGISTRY_LOCK) {
            if (BuiltInRegistries.ITEM.containsKey(itemId)) {
                return;
            }
            unfreeze(BuiltInRegistries.ITEM);
            Registry.register(BuiltInRegistries.ITEM, itemId, new Item(new Item.Properties()));
        }
    }

    private static void bindIntegerComponent(
            DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> holder) {
        ResourceLocation id = holder.getId();
        if (!BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(id)) {
            unfreeze(BuiltInRegistries.DATA_COMPONENT_TYPE);
            DataComponentType<Integer> type = DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .build();
            Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id, type);
        }
        bindDeferredHolder(
                holder,
                BuiltInRegistries.DATA_COMPONENT_TYPE.getHolderOrThrow(holder.getKey()));
    }

    private static void bindComponentIngredientType() {
        try {
            if (!NeoForgeRegistries.INGREDIENT_TYPES.containsKey(
                    NeoForgeMod.DATA_COMPONENT_INGREDIENT_TYPE.getId())) {
                unfreeze(NeoForgeRegistries.INGREDIENT_TYPES);
                Registry.register(
                        NeoForgeRegistries.INGREDIENT_TYPES,
                        NeoForgeMod.DATA_COMPONENT_INGREDIENT_TYPE.getId(),
                        new IngredientType<>(DataComponentIngredient.CODEC));
            }
            bindDeferredHolder(
                    NeoForgeMod.DATA_COMPONENT_INGREDIENT_TYPE,
                    NeoForgeRegistries.INGREDIENT_TYPES.getHolderOrThrow(
                            NeoForgeMod.DATA_COMPONENT_INGREDIENT_TYPE.getKey()));
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "Unable to install test data-component ingredient type", exception);
        }
    }

    private static void bindDeferredHolder(DeferredHolder<?, ?> holder, Object registryHolder) {
        try {
            Field field = DeferredHolder.class.getDeclaredField("holder");
            field.setAccessible(true);
            field.set(holder, registryHolder);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Unable to bind deferred holder " + holder.getId(), exception);
        }
    }

    private static void unfreeze(Registry<?> registry) {
        try {
            registry.getClass().getMethod("unfreeze").invoke(registry);
        } catch (NoSuchMethodException ignored) {
            try {
                Field frozen = findField(registry.getClass(), "frozen");
                frozen.setAccessible(true);
                frozen.setBoolean(registry, false);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Unable to unfreeze " + registry.key(), exception);
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to unfreeze " + registry.key(), exception);
        }
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> cursor = type;
        while (cursor != null) {
            try {
                return cursor.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                cursor = cursor.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static List<FluidStack> fluidStacks(
            JsonArray stacks,
            RegistryOps<JsonElement> ops) {
        List<FluidStack> fluids = new ArrayList<>();
        for (JsonElement element : stacks) {
            JsonObject stack = element.getAsJsonObject();
            ResourceLocation fluidId = ResourceLocation.parse(stack.get("id").getAsString());
            int amount = stack.get("amount").getAsInt();
            if (!BuiltInRegistries.FLUID.containsKey(fluidId)) {
                FluidStack stub = new FluidStack(Fluids.WATER, amount);
                stub.set(DataComponents.CUSTOM_NAME, Component.literal(fluidId.toString()));
                fluids.add(stub);
                continue;
            }
            fluids.add(FluidStack.CODEC.parse(ops, element).getOrThrow());
        }
        return fluids;
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

    private static String filename(JsonObject document) {
        return document.get("family_id").getAsString()
                .replace('.', '_')
                .replace('#', '_');
    }

    private static String authoredPrefix(Path generatedPath) {
        Path directory = generatedPath.getFileName().toString().endsWith(".json")
                ? generatedPath.getParent()
                : generatedPath;
        if (directory.endsWith(Path.of("t37", "assembler"))) {
            return "t37/assembler";
        }
        if (directory.endsWith(Path.of("t38", "roaster"))) {
            return "t38/roaster";
        }
        if (directory.endsWith(Path.of("t39", "centrifuge"))) {
            return "t39/centrifuge";
        }
        if (directory.endsWith(Path.of("t39_catalog", "centrifuge"))) {
            return "t39_catalog/centrifuge";
        }
        if (directory.endsWith(Path.of("t40", "electrolyzer"))) {
            return "t40/electrolyzer";
        }
        if (directory.endsWith(Path.of("t40_catalog", "electrolyzer"))) {
            return "t40_catalog/electrolyzer";
        }
        if (directory.endsWith(Path.of("t41", "assembler"))) {
            return "t41/assembler";
        }
        if (directory.endsWith(Path.of("t41_catalog", "assembler"))) {
            return "t41_catalog/assembler";
        }
        if (directory.endsWith(Path.of("t43", "smelter"))) {
            return "t43/smelter";
        }
        if (directory.endsWith(Path.of("t43_catalog", "smelter"))) {
            return "t43_catalog/smelter";
        }
        if (directory.endsWith(Path.of("t45", "smelter"))) {
            return "t45/smelter";
        }
        if (directory.endsWith(Path.of("t45", "drying"))) {
            return "t45/drying";
        }
        if (directory.endsWith(Path.of("t45_catalog", "smelter"))) {
            return "t45_catalog/smelter";
        }
        if (directory.endsWith(Path.of("t45_catalog", "drying"))) {
            return "t45_catalog/drying";
        }
        throw new IllegalArgumentException(
                "Unsupported generated compact family path " + generatedPath);
    }
}
