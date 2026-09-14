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

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Shared generated-family loader used by equivalence tests and compact
 * host measurement harnesses. Authored recipe-id prefixes come from the
 * live generated directory so host/cohort fixtures stay isolated.
 */
final class CompactGTRecipeFamilyGeneratedSupport {
    private static final Object REGISTRY_LOCK = new Object();

    private CompactGTRecipeFamilyGeneratedSupport() {}

    static Path generatedRoot() {
        return semanticGeneratedRoot("assembler/compact");
    }

    static Path roasterGeneratedRoot() {
        return semanticGeneratedRoot("roaster/compact");
    }

    static Path centrifugeGeneratedRoot() {
        return semanticGeneratedRoot("centrifuge/compact");
    }

    static Path centrifugeCatalogFixtureRoot() {
        return Path.of(System.getProperty("user.dir"))
                .resolve(
                        "src/test/resources/centrifuge_catalog_fixture/data/cruciblecraft/"
                                + "recipe/centrifuge_catalog/centrifuge");
    }

    static Path electrolyzerGeneratedRoot() {
        return semanticGeneratedRoot("electrolyzer/compact");
    }

    static Path electrolyzerCatalogFixtureRoot() {
        return Path.of(System.getProperty("user.dir"))
                .resolve(
                        "src/test/resources/electrolyzer_catalog_fixture/data/cruciblecraft/"
                                + "recipe/electrolyzer_catalog/electrolyzer");
    }

    static Path assemblerWoodGeneratedRoot() {
        return semanticGeneratedRoot("assembler/wood");
    }

    static Path assemblerWoodCatalogFixtureRoot() {
        return Path.of(System.getProperty("user.dir"))
                .resolve(
                        "src/test/resources/assembler_wood_catalog_fixture/data/cruciblecraft/"
                                + "recipe/assembler_wood_catalog/assembler");
    }

    static Path smelterStoneGeneratedRoot() {
        return semanticGeneratedRoot("smelter/stone");
    }

    static Path smelterStoneCatalogFixtureRoot() {
        return Path.of(System.getProperty("user.dir"))
                .resolve(
                        "src/test/resources/smelter_stone_catalog_fixture/data/cruciblecraft/"
                                + "recipe/smelter_stone_catalog/smelter");
    }

    static Path smelterBlockGeneratedRoot() {
        return semanticGeneratedRoot("smelter/block");
    }

    static Path dryingBlockGeneratedRoot() {
        return semanticGeneratedRoot("drying/block");
    }

    static List<Path> blockObjectGeneratedRoots() {
        return List.of(smelterBlockGeneratedRoot(), dryingBlockGeneratedRoot());
    }

    static Path bathMteGeneratedRoot() {
        return semanticGeneratedRoot("bath/mte");
    }

    static Path bathRemainderGeneratedRoot() {
        return semanticGeneratedRoot("bath/remainder");
    }

    static List<Path> generatedRootsThroughBathMte() {
        return List.of(
                generatedRoot(),
                roasterGeneratedRoot(),
                centrifugeGeneratedRoot(),
                electrolyzerGeneratedRoot(),
                assemblerWoodGeneratedRoot(),
                smelterStoneGeneratedRoot(),
                smelterBlockGeneratedRoot(),
                dryingBlockGeneratedRoot(),
                bathMteGeneratedRoot());
    }

    static List<Path> generatedRootsThroughBathRemainder() {
        List<Path> roots = new ArrayList<>(generatedRootsThroughBathMte());
        roots.add(bathRemainderGeneratedRoot());
        return List.copyOf(roots);
    }

    static Path bathIdentityGeneratedRoot() {
        return semanticGeneratedRoot("bath/identity");
    }

    static List<Path> generatedRootsThroughBathIdentity() {
        List<Path> roots = new ArrayList<>(generatedRootsThroughBathRemainder());
        roots.add(bathIdentityGeneratedRoot());
        return List.copyOf(roots);
    }

    static Path bathTinyPurifiedGeneratedRoot() {
        return semanticGeneratedRoot("bath/tiny_purified");
    }

    static Path semanticGeneratedRoot(String pathPrefix) {
        return Path.of(System.getProperty("user.dir"))
                .resolve(
                        "src/recipe_generated/resources/data/cruciblecraft/recipe")
                .resolve(pathPrefix);
    }

    static List<Path> compactHostGeneratedRoots() {
        List<Path> roots = new ArrayList<>(generatedRootsThroughBathIdentity());
        roots.add(bathTinyPurifiedGeneratedRoot());
        return List.copyOf(roots);
    }

    static Path smelterOrdinaryClosureGeneratedRoot() {
        return semanticGeneratedRoot("smelter/ordinary_closure");
    }

    static Path mixerOrdinaryClosureGeneratedRoot() {
        return semanticGeneratedRoot("mixer/ordinary_closure");
    }

    static List<Path> compactPlusSmelterOrdinaryRoots() {
        List<Path> roots = new ArrayList<>(compactHostGeneratedRoots());
        roots.add(smelterOrdinaryClosureGeneratedRoot());
        return List.copyOf(roots);
    }

    static List<Path> compactPlusSmelterMixerOrdinaryRoots() {
        return compactPlusOrdinaryRootsThrough("mixer");
    }

    static List<String> ordinaryClosureMixHosts() {
        return List.of(
                "smelter",
                "mixer",
                "drying",
                "electrolyzer",
                "centrifuge",
                "autoclave",
                "compressor");
    }

    static List<Path> compactPlusDeferredRecyclingRoots() {
        List<Path> roots = new ArrayList<>(compactPlusOrdinaryRootsThrough("compressor"));
        Path extra = semanticGeneratedRoot("smelter/deferred_recycling");
        if (!roots.contains(extra)) {
            roots.add(extra);
        }
        return List.copyOf(roots);
    }

    static List<Path> compactPlusOrdinaryRootsThrough(String lastHost) {
        List<Path> roots = new ArrayList<>(compactHostGeneratedRoots());
        for (String host : ordinaryClosureMixHosts()) {
            roots.add(semanticGeneratedRoot(host + "/ordinary_closure"));
            if (host.equals(lastHost)) {
                break;
            }
        }
        return List.copyOf(roots);
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

    static List<JsonObject> loadGeneratedFamiliesRecursive(List<Path> roots)
            throws IOException {
        List<JsonObject> documents = new ArrayList<>();
        for (Path root : roots) {
            documents.addAll(loadGeneratedFamiliesRecursive(root));
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

    static List<CompactRecipeFamilySource> loadGeneratedSourcesRecursive(
            List<Path> roots, RegistryAccess registries) throws IOException {
        List<CompactRecipeFamilySource> sources = new ArrayList<>();
        for (Path root : roots) {
            sources.addAll(loadGeneratedSourcesRecursive(root, registries));
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

    static boolean hasGeneratedFamiliesRecursive(List<Path> roots) throws IOException {
        if (roots.isEmpty()) {
            return false;
        }
        for (Path root : roots) {
            if (!hasGeneratedFamiliesRecursive(root)) {
                return false;
            }
        }
        return true;
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
        String filename = holderStem(generatedPath, document);
        ResourceLocation authoredId = ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft",
                authoredPrefix(generatedPath) + "/" + filename);
        var ops = RegistryOps.create(JsonOps.INSTANCE, registries);
        ResourceLocation targetMap = ResourceLocation.parse(
                document.get("target_map").getAsString());
        Optional<ResourceLocation> publicationGroup = document.has("publication_group")
                ? Optional.of(SemanticIdMap.remapPublicationGroup(
                        ResourceLocation.parse(
                                document.get("publication_group").getAsString())))
                : Optional.empty();
        Optional<CompactGTRecipeFamilyDefinition.AuthoredMatrixV1> matrix =
                Optional.empty();
        List<CompactGTRecipeFamilyDefinition.Relation> relations = new ArrayList<>();
        if (document.has("matrix") && document.get("matrix").isJsonObject()) {
            matrix = Optional.of(matrixFromGenerated(
                    document.getAsJsonObject("matrix"), ops));
        } else if (document.has("relations") && document.get("relations").isJsonArray()) {
            for (JsonElement element : document.getAsJsonArray("relations")) {
                relations.add(relationFromGenerated(element.getAsJsonObject(), ops));
            }
        }
        return new CompactRecipeFamilySource(
                authoredId,
                new CompactGTRecipeFamilyDefinition(
                        document.get("family_id").getAsString(),
                        targetMap,
                        document.get("source_revision").getAsString(),
                        relations,
                        parameterizedFromGenerated(document),
                        publicationGroup,
                        matrix,
                        transportFragmentFromGenerated(document)));
    }

    static List<JsonObject> authoredRelationJsons(JsonObject document) {
        if (document.has("matrix") && document.get("matrix").isJsonObject()) {
            return expandMatrixJson(document.getAsJsonObject("matrix"));
        }
        List<JsonObject> relations = new ArrayList<>();
        if (document.has("relations") && document.get("relations").isJsonArray()) {
            for (JsonElement element : document.getAsJsonArray("relations")) {
                relations.add(element.getAsJsonObject());
            }
        }
        return relations;
    }

    private static List<JsonObject> expandMatrixJson(JsonObject matrix) {
        JsonObject shared = matrix.getAsJsonObject("shared");
        JsonObject dicts = matrix.getAsJsonObject("dicts");
        JsonArray itemInputs = dicts.getAsJsonArray("item_inputs");
        JsonArray itemOutputs = dicts.getAsJsonArray("item_outputs");
        JsonArray fluids = dicts.getAsJsonArray("fluids");
        List<JsonObject> relations = new ArrayList<>();
        for (JsonElement element : matrix.getAsJsonArray("rows")) {
            JsonArray row = element.getAsJsonArray();
            JsonObject fluid = fluids.get(row.get(2).getAsInt()).getAsJsonObject();
            JsonObject relation = new JsonObject();
            relation.addProperty("stable_id", row.get(3).getAsString());
            relation.add("item_inputs", itemInputs.get(row.get(0).getAsInt()));
            relation.add("item_input_counts", shared.get("item_input_counts"));
            relation.add("item_input_actions", shared.get("item_input_actions"));
            relation.add("item_outputs", itemOutputs.get(row.get(1).getAsInt()));
            relation.add("fluid_inputs", fluid.get("fluid_inputs"));
            relation.add("fluid_outputs", fluid.get("fluid_outputs"));
            relation.add("output_chances", shared.get("output_chances"));
            relation.add("duration", shared.get("duration"));
            relation.add("eut", shared.get("eut"));
            relation.add("special_value", shared.get("special_value"));
            relation.add("can_be_buffered", shared.get("can_be_buffered"));
            relation.addProperty("shadow_order", row.size() == 6
                    ? row.get(5).getAsInt() : row.get(4).getAsInt());
            JsonObject provenance = new JsonObject();
            provenance.add("source_kind", shared.get("source_kind"));
            provenance.add("selected_source_recipe", shared.get("selected_source_recipe"));
            relation.add("provenance", provenance);
            relations.add(relation);
        }
        return relations;
    }

    private static CompactGTRecipeFamilyDefinition.AuthoredMatrixV1 matrixFromGenerated(
            JsonObject matrixJson,
            RegistryOps<JsonElement> ops) {
        JsonObject sharedJson = matrixJson.getAsJsonObject("shared");
        CompactGTRecipeFamilyDefinition.SharedSpec shared =
                new CompactGTRecipeFamilyDefinition.SharedSpec(
                        sharedJson.get("duration").getAsInt(),
                        sharedJson.get("eut").getAsLong(),
                        sharedJson.get("special_value").getAsLong(),
                        sharedJson.get("can_be_buffered").getAsBoolean(),
                        ints(sharedJson.getAsJsonArray("item_input_counts")),
                        actions(sharedJson.getAsJsonArray("item_input_actions")),
                        ints(sharedJson.getAsJsonArray("output_chances")),
                        sharedJson.get("source_kind").getAsString(),
                        sharedJson.get("selected_source_recipe").getAsString());
        JsonObject dictsJson = matrixJson.getAsJsonObject("dicts");
        List<List<Ingredient>> itemInputs = new ArrayList<>();
        for (JsonElement config : dictsJson.getAsJsonArray("item_inputs")) {
            List<Ingredient> inputs = new ArrayList<>();
            for (JsonElement element : config.getAsJsonArray()) {
                inputs.add(ingredientFromGenerated(element.getAsJsonObject(), ops));
            }
            itemInputs.add(List.copyOf(inputs));
        }
        List<List<ItemStack>> itemOutputs = new ArrayList<>();
        for (JsonElement config : dictsJson.getAsJsonArray("item_outputs")) {
            List<ItemStack> outputs = new ArrayList<>();
            for (JsonElement element : config.getAsJsonArray()) {
                outputs.add(itemStackFromGenerated(element.getAsJsonObject(), ops));
            }
            itemOutputs.add(List.copyOf(outputs));
        }
        List<CompactGTRecipeFamilyDefinition.FluidIo> fluids = new ArrayList<>();
        for (JsonElement config : dictsJson.getAsJsonArray("fluids")) {
            JsonObject fluidJson = config.getAsJsonObject();
            fluids.add(new CompactGTRecipeFamilyDefinition.FluidIo(
                    fluidStacks(fluidJson.getAsJsonArray("fluid_inputs"), ops),
                    fluidStacks(fluidJson.getAsJsonArray("fluid_outputs"), ops)));
        }
        List<CompactGTRecipeFamilyDefinition.MatrixRow> rows = new ArrayList<>();
        for (JsonElement element : matrixJson.getAsJsonArray("rows")) {
            JsonArray row = element.getAsJsonArray();
            int shadowOrder = row.size() == 6 ? row.get(5).getAsInt() : row.get(4).getAsInt();
            rows.add(new CompactGTRecipeFamilyDefinition.MatrixRow(
                    row.get(0).getAsInt(),
                    row.get(1).getAsInt(),
                    row.get(2).getAsInt(),
                    ResourceLocation.parse(row.get(3).getAsString()),
                    shadowOrder));
        }
        return new CompactGTRecipeFamilyDefinition.AuthoredMatrixV1(
                shared,
                new CompactGTRecipeFamilyDefinition.MatrixDicts(
                        itemInputs, itemOutputs, fluids),
                rows);
    }

    private static List<ItemInputAction> actions(JsonArray array) {
        List<ItemInputAction> values = new ArrayList<>();
        array.forEach(element -> values.add(actionFromGenerated(element.getAsJsonObject())));
        return values;
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
                                .getAsString())));
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

    private static final class FluidTypeHolder {
        private static final FluidType VALUE = new FluidType(FluidType.Properties.create());
    }

    private static void ensureFluid(ResourceLocation fluidId) {
        if (BuiltInRegistries.FLUID.containsKey(fluidId)
                || !"cruciblecraft".equals(fluidId.getNamespace())) {
            return;
        }
        synchronized (REGISTRY_LOCK) {
            if (BuiltInRegistries.FLUID.containsKey(fluidId)) {
                return;
            }
            unfreeze(BuiltInRegistries.FLUID);
            Registry.register(
                    BuiltInRegistries.FLUID,
                    fluidId,
                    new StubStillFluid(FluidTypeHolder.VALUE));
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
            ensureFluid(fluidId);
            if (!BuiltInRegistries.FLUID.containsKey(fluidId)) {
                throw new IllegalStateException("Unregistered bath-mte fluid " + fluidId);
            }
            Fluid fluid = BuiltInRegistries.FLUID.get(fluidId);
            fluids.add(new FluidStack(fluid, amount));
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

    private static Optional<CompactGTRecipeFamilyDefinition.TransportFragment>
            transportFragmentFromGenerated(JsonObject document) {
        if (!document.has("transport_fragment")
                || !document.get("transport_fragment").isJsonObject()) {
            return Optional.empty();
        }
        JsonObject fragment = document.getAsJsonObject("transport_fragment");
        return Optional.of(new CompactGTRecipeFamilyDefinition.TransportFragment(
                fragment.get("index").getAsInt(),
                fragment.get("count").getAsInt(),
                fragment.get("total_relations").getAsInt(),
                fragment.get("semantic_digest").getAsString()));
    }

    private static String holderStem(Path generatedPath, JsonObject document) {
        if (Files.isRegularFile(generatedPath)) {
            String name = generatedPath.getFileName().toString();
            if (name.endsWith(".json")) {
                return name.substring(0, name.length() - 5);
            }
            return name;
        }
        return filename(document);
    }

    private static String filename(JsonObject document) {
        return document.get("family_id").getAsString()
                .replace('.', '_')
                .replace('#', '_')
                .replace(':', '_');
    }

    private static String authoredPrefix(Path generatedPath) {
        Path directory = generatedPath.getFileName().toString().endsWith(".json")
                ? generatedPath.getParent()
                : generatedPath;
        Path current = directory;
        List<String> parts = new ArrayList<>();
        while (current != null && current.getFileName() != null
                && !"recipe".equals(current.getFileName().toString())) {
            parts.add(0, current.getFileName().toString());
            current = current.getParent();
        }
        if (current != null && !parts.isEmpty()) {
            String prefix = String.join("/", parts);
            if (!prefix.equals("publication_policy")) {
                return prefix;
            }
        }
        throw new IllegalArgumentException(
                "Unsupported generated compact family path " + generatedPath);
    }

    private static final class StubStillFluid extends Fluid {
        private final FluidType fluidType;

        private StubStillFluid(FluidType fluidType) {
            this.fluidType = fluidType;
        }

        @Override
        public FluidType getFluidType() {
            return fluidType;
        }

        @Override
        public Item getBucket() {
            return Items.AIR;
        }

        @Override
        protected boolean canBeReplacedWith(
                FluidState state,
                BlockGetter level,
                BlockPos pos,
                Fluid fluid,
                Direction direction) {
            return false;
        }

        @Override
        protected Vec3 getFlow(BlockGetter level, BlockPos pos, FluidState state) {
            return Vec3.ZERO;
        }

        @Override
        public int getTickDelay(LevelReader level) {
            return 5;
        }

        @Override
        protected float getExplosionResistance() {
            return 100.0F;
        }

        @Override
        protected BlockState createLegacyBlock(FluidState state) {
            return Blocks.AIR.defaultBlockState();
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }

        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public float getOwnHeight(FluidState state) {
            return 1.0F;
        }

        @Override
        public float getHeight(FluidState state, BlockGetter level, BlockPos pos) {
            return 1.0F;
        }

        @Override
        public VoxelShape getShape(FluidState state, BlockGetter level, BlockPos pos) {
            return Shapes.block();
        }
    }
}
