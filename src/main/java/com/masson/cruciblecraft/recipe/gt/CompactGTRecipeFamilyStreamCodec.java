package com.masson.cruciblecraft.recipe.gt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Bounded binary network codec for compact GT recipe families. Datapack JSON
 * still uses {@link CompactGTRecipeFamilyDefinition#MAP_CODEC}; this path must
 * not wrap the family in NBT.
 */
final class CompactGTRecipeFamilyStreamCodec {
    static final StreamCodec<RegistryFriendlyByteBuf, CompactGTRecipeFamilyEntry> INSTANCE =
            StreamCodec.of(
                    CompactGTRecipeFamilyStreamCodec::encode,
                    CompactGTRecipeFamilyStreamCodec::decode);

    private static final byte STABLE_ID_SPLIT = 0;
    private static final byte STABLE_ID_FULL = 1;
    private static final int FLAG_DURATION = 1;
    private static final int FLAG_EUT = 1 << 1;
    private static final int FLAG_SPECIAL = 1 << 2;
    private static final int FLAG_BUFFERED = 1 << 3;
    private static final int FLAG_SHADOW = 1 << 4;
    private static final int FLAG_COUNTS = 1 << 5;
    private static final int FLAG_ACTIONS = 1 << 6;
    private static final int FLAG_CHANCES = 1 << 7;

    private CompactGTRecipeFamilyStreamCodec() {}

    private static void encode(
            RegistryFriendlyByteBuf buffer,
            CompactGTRecipeFamilyEntry entry) {
        ByteBuf payload = Unpooled.buffer();
        try {
            RegistryFriendlyByteBuf nested = wrap(payload, buffer);
            encodeBody(nested, entry);
            int payloadSize = payload.readableBytes();
            int total = 1 + varIntBytes(payloadSize) + payloadSize;
            if (total > CompactRecipeWireLimits.MAX_RECIPE_ENTRY_WIRE_BYTES) {
                throw new EncoderException(
                        "Compact family wire payload exceeds "
                                + CompactRecipeWireLimits.MAX_RECIPE_ENTRY_WIRE_BYTES
                                + " bytes: " + total + " for "
                                + entry.definition().familyId());
            }
            buffer.writeByte(CompactRecipeWireLimits.WIRE_VERSION);
            buffer.writeVarInt(payloadSize);
            buffer.writeBytes(payload);
        } finally {
            payload.release();
        }
    }

    private static CompactGTRecipeFamilyEntry decode(RegistryFriendlyByteBuf buffer) {
        int version = buffer.readUnsignedByte();
        if (version != CompactRecipeWireLimits.WIRE_VERSION) {
            throw new DecoderException(
                    "Unsupported compact family wire version: " + version);
        }
        int payloadSize = buffer.readVarInt();
        if (payloadSize < 0
                || payloadSize > CompactRecipeWireLimits.MAX_RECIPE_ENTRY_WIRE_BYTES) {
            throw new DecoderException(
                    "Compact family wire payload size " + payloadSize
                            + " exceeds "
                            + CompactRecipeWireLimits.MAX_RECIPE_ENTRY_WIRE_BYTES);
        }
        if (buffer.readableBytes() < payloadSize) {
            throw new DecoderException(
                    "Compact family wire payload truncated: expected "
                            + payloadSize + ", remaining " + buffer.readableBytes());
        }
        ByteBuf payload = buffer.readBytes(payloadSize);
        try {
            RegistryFriendlyByteBuf nested = wrap(payload, buffer);
            CompactGTRecipeFamilyEntry decoded = decodeBody(nested);
            if (nested.readableBytes() != 0) {
                throw new DecoderException(
                        "Compact family wire payload has "
                                + nested.readableBytes() + " leftover bytes");
            }
            return decoded;
        } finally {
            payload.release();
        }
    }

    private static void encodeBody(
            RegistryFriendlyByteBuf buffer,
            CompactGTRecipeFamilyEntry entry) {
        CompactGTRecipeFamilyDefinition definition = entry.definition();
        writeBounded(
                buffer,
                definition.familyId(),
                CompactRecipeWireLimits.MAX_FAMILY_ID_LENGTH);
        ResourceLocation.STREAM_CODEC.encode(buffer, definition.targetMap());
        writeBounded(
                buffer,
                definition.sourceRevision(),
                CompactRecipeWireLimits.MAX_SOURCE_REVISION_LENGTH);
        buffer.writeBoolean(definition.publicationGroup().isPresent());
        definition.publicationGroup().ifPresent(
                group -> ResourceLocation.STREAM_CODEC.encode(buffer, group));
        encodeTransportFragment(buffer, definition.transportFragment());
        encodeParameterized(buffer, definition.parameterized());
        if (definition.matrix().isPresent()) {
            buffer.writeByte(CompactRecipeWireLimits.WIRE_FORM_MATRIX_V1);
            encodeMatrix(buffer, definition.matrix().orElseThrow());
            return;
        }
        buffer.writeByte(CompactRecipeWireLimits.WIRE_FORM_INLINE);

        List<CompactGTRecipeFamilyDefinition.Relation> relations = definition.relations();
        Dictionaries dictionaries = Dictionaries.build(buffer, relations);
        writeNamedList(buffer, dictionaries.ingredients, CompactRecipeWireValues::encodeIngredient);
        writeNamedList(buffer, dictionaries.itemStacks, CompactRecipeWireValues::encodeItemStack);
        writeNamedList(buffer, dictionaries.fluids, CompactRecipeWireValues::encodeFluidStack);
        writeProvenanceKeys(buffer, dictionaries.provenanceKeys);
        writeDictionary(
                buffer,
                dictionaries.actions,
                ItemInputAction.STREAM_CODEC);
        writeDictionary(
                buffer,
                dictionaries.stableIdPrefixes,
                ResourceLocation.STREAM_CODEC);

        SharedFields shared = SharedFields.of(relations);
        buffer.writeByte(shared.flags);
        if (shared.has(FLAG_DURATION)) {
            buffer.writeVarInt(relations.getFirst().duration());
        }
        if (shared.has(FLAG_EUT)) {
            buffer.writeVarLong(relations.getFirst().eut());
        }
        if (shared.has(FLAG_SPECIAL)) {
            buffer.writeVarLong(relations.getFirst().specialValue());
        }
        if (shared.has(FLAG_BUFFERED)) {
            buffer.writeBoolean(relations.getFirst().canBeBuffered());
        }
        if (shared.has(FLAG_SHADOW)) {
            buffer.writeVarInt(relations.getFirst().shadowOrder());
        }
        if (shared.has(FLAG_COUNTS)) {
            writeIntList(buffer, relations.getFirst().itemInputCounts());
        }
        if (shared.has(FLAG_ACTIONS)) {
            writeActionIndices(buffer, dictionaries, relations.getFirst());
        }
        if (shared.has(FLAG_CHANCES)) {
            writeIntList(buffer, relations.getFirst().outputChances());
        }

        if (relations.size() > CompactRecipeWireLimits.DECODE_RELATIONS_CEILING) {
            throw new EncoderException(
                    "Compact family relation count "
                            + relations.size()
                            + " exceeds "
                            + CompactRecipeWireLimits.DECODE_RELATIONS_CEILING);
        }
        buffer.writeVarInt(relations.size());
        for (CompactGTRecipeFamilyDefinition.Relation relation : relations) {
            encodeRelation(buffer, dictionaries, shared, relation);
        }
    }

    private static CompactGTRecipeFamilyEntry decodeBody(RegistryFriendlyByteBuf buffer) {
        String familyId = readBounded(
                buffer, CompactRecipeWireLimits.MAX_FAMILY_ID_LENGTH);
        ResourceLocation targetMap = ResourceLocation.STREAM_CODEC.decode(buffer);
        String sourceRevision = readBounded(
                buffer, CompactRecipeWireLimits.MAX_SOURCE_REVISION_LENGTH);
        Optional<ResourceLocation> publicationGroup = Optional.empty();
        if (buffer.readBoolean()) {
            publicationGroup = Optional.of(
                    ResourceLocation.STREAM_CODEC.decode(buffer));
        }
        Optional<CompactGTRecipeFamilyDefinition.TransportFragment> transportFragment =
                decodeTransportFragment(buffer);
        Optional<CompactGTRecipeFamilyDefinition.ParameterizedSpec> parameterized =
                decodeParameterized(buffer);
        int form = buffer.readUnsignedByte();
        if (form == CompactRecipeWireLimits.WIRE_FORM_MATRIX_V1) {
            CompactGTRecipeFamilyDefinition.AuthoredMatrixV1 matrix =
                    decodeMatrix(buffer);
            return new CompactGTRecipeFamilyEntry(new CompactGTRecipeFamilyDefinition(
                    familyId,
                    targetMap,
                    sourceRevision,
                    List.of(),
                    parameterized,
                    publicationGroup,
                    Optional.of(matrix),
                    transportFragment));
        }
        if (form != CompactRecipeWireLimits.WIRE_FORM_INLINE) {
            throw new DecoderException(
                    "Unsupported compact family wire form: " + form);
        }

        List<Ingredient> ingredients = readNamedList(
                buffer,
                CompactRecipeWireLimits.MAX_DICTIONARY_ENTRIES,
                "ingredient dictionary",
                CompactRecipeWireValues::decodeIngredient);
        List<ItemStack> itemStacks = readNamedList(
                buffer,
                CompactRecipeWireLimits.MAX_DICTIONARY_ENTRIES,
                "item stack dictionary",
                CompactRecipeWireValues::decodeItemStack);
        List<FluidStack> fluids = readNamedList(
                buffer,
                CompactRecipeWireLimits.MAX_DICTIONARY_ENTRIES,
                "fluid dictionary",
                CompactRecipeWireValues::decodeFluidStack);
        List<ProvenanceKey> provenanceKeys = readProvenanceKeys(buffer);
        List<ItemInputAction> actions = readDictionary(
                buffer,
                CompactRecipeWireLimits.MAX_ACTIONS,
                "action dictionary",
                ItemInputAction.STREAM_CODEC);
        List<ResourceLocation> prefixes = readDictionary(
                buffer,
                CompactRecipeWireLimits.MAX_STABLE_ID_PREFIXES,
                "stable-id prefix dictionary",
                ResourceLocation.STREAM_CODEC);

        int flags = buffer.readUnsignedByte();
        Integer sharedDuration = (flags & FLAG_DURATION) != 0
                ? buffer.readVarInt() : null;
        Long sharedEut = (flags & FLAG_EUT) != 0
                ? buffer.readVarLong() : null;
        Long sharedSpecial = (flags & FLAG_SPECIAL) != 0
                ? buffer.readVarLong() : null;
        Boolean sharedBuffered = (flags & FLAG_BUFFERED) != 0
                ? buffer.readBoolean() : null;
        Integer sharedShadow = (flags & FLAG_SHADOW) != 0
                ? buffer.readVarInt() : null;
        List<Integer> sharedCounts = (flags & FLAG_COUNTS) != 0
                ? readIntList(buffer, CompactRecipeWireLimits.MAX_IO_PER_RELATION)
                : null;
        List<ItemInputAction> sharedActions = (flags & FLAG_ACTIONS) != 0
                ? readActionList(buffer, actions)
                : null;
        List<Integer> sharedChances = (flags & FLAG_CHANCES) != 0
                ? readIntList(buffer, CompactRecipeWireLimits.MAX_IO_PER_RELATION)
                : null;

        int relationCount = readCount(
                buffer,
                CompactRecipeWireLimits.DECODE_RELATIONS_CEILING,
                "relations");
        List<CompactGTRecipeFamilyDefinition.Relation> relations =
                new ArrayList<>(relationCount);
        for (int index = 0; index < relationCount; index++) {
            relations.add(decodeRelation(
                    buffer,
                    ingredients,
                    itemStacks,
                    fluids,
                    provenanceKeys,
                    actions,
                    prefixes,
                    sharedDuration,
                    sharedEut,
                    sharedSpecial,
                    sharedBuffered,
                    sharedShadow,
                    sharedCounts,
                    sharedActions,
                    sharedChances));
        }
        return new CompactGTRecipeFamilyEntry(new CompactGTRecipeFamilyDefinition(
                familyId,
                targetMap,
                sourceRevision,
                relations,
                parameterized,
                publicationGroup,
                Optional.empty(),
                transportFragment));
    }

    private static void encodeMatrix(
            RegistryFriendlyByteBuf buffer,
            CompactGTRecipeFamilyDefinition.AuthoredMatrixV1 matrix) {
        CompactGTRecipeFamilyDefinition.SharedSpec shared = matrix.shared();
        CompactGTRecipeFamilyDefinition.MatrixDicts dicts = matrix.dicts();
        buffer.writeVarInt(shared.duration());
        buffer.writeVarLong(shared.eut());
        buffer.writeVarLong(shared.specialValue());
        buffer.writeBoolean(shared.canBeBuffered());
        writeIntList(buffer, shared.itemInputCounts());
        writeActionDictionaryAndList(buffer, shared.itemInputActions());
        writeIntList(buffer, shared.outputChances());
        writeBounded(
                buffer,
                shared.sourceKind(),
                CompactRecipeWireLimits.MAX_SOURCE_KIND_LENGTH);
        writeBounded(
                buffer,
                shared.selectedSourceRecipe(),
                CompactRecipeWireLimits.MAX_SELECTED_SOURCE_LENGTH);

        writeConfigList(
                buffer,
                dicts.itemInputs(),
                CompactRecipeWireValues::encodeIngredient,
                "item input configs");
        writeConfigList(
                buffer,
                dicts.itemOutputs(),
                CompactRecipeWireValues::encodeItemStack,
                "item output configs");
        if (dicts.fluids().size() > CompactRecipeWireLimits.MAX_DICTIONARY_ENTRIES) {
            throw new EncoderException(
                    "Compact family fluid config dictionary exceeds "
                            + CompactRecipeWireLimits.MAX_DICTIONARY_ENTRIES);
        }
        buffer.writeVarInt(dicts.fluids().size());
        for (CompactGTRecipeFamilyDefinition.FluidIo fluids : dicts.fluids()) {
            writeNamedList(buffer, fluids.fluidInputs(), CompactRecipeWireValues::encodeFluidStack);
            writeNamedList(buffer, fluids.fluidOutputs(), CompactRecipeWireValues::encodeFluidStack);
        }

        List<ResourceLocation> prefixes = new ArrayList<>();
        Map<ResourceLocation, Integer> prefixIndex = new LinkedHashMap<>();
        for (CompactGTRecipeFamilyDefinition.MatrixRow row : matrix.rows()) {
            splitStableId(row.stableId()).ifPresent(split -> internValue(
                    split.prefix(),
                    prefixIndex,
                    prefixes,
                    CompactRecipeWireLimits.MAX_STABLE_ID_PREFIXES));
        }
        writeDictionary(buffer, prefixes, ResourceLocation.STREAM_CODEC);
        if (matrix.rows().size() > CompactRecipeWireLimits.DECODE_RELATIONS_CEILING) {
            throw new EncoderException(
                    "Compact family relation count "
                            + matrix.rows().size()
                            + " exceeds "
                            + CompactRecipeWireLimits.DECODE_RELATIONS_CEILING);
        }
        buffer.writeVarInt(matrix.rows().size());
        Dictionaries prefixLookup = Dictionaries.prefixes(prefixIndex, prefixes);
        for (CompactGTRecipeFamilyDefinition.MatrixRow row : matrix.rows()) {
            encodeStableId(buffer, prefixLookup, row.stableId());
            buffer.writeVarInt(row.inputIdx());
            buffer.writeVarInt(row.outputIdx());
            buffer.writeVarInt(row.fluidIdx());
            buffer.writeVarInt(row.shadowOrder());
        }
    }

    private static CompactGTRecipeFamilyDefinition.AuthoredMatrixV1 decodeMatrix(
            RegistryFriendlyByteBuf buffer) {
        int duration = buffer.readVarInt();
        long eut = buffer.readVarLong();
        long special = buffer.readVarLong();
        boolean buffered = buffer.readBoolean();
        List<Integer> counts = readIntList(
                buffer, CompactRecipeWireLimits.MAX_IO_PER_RELATION);
        List<ItemInputAction> actions = readDictionary(
                buffer,
                CompactRecipeWireLimits.MAX_ACTIONS,
                "action dictionary",
                ItemInputAction.STREAM_CODEC);
        List<ItemInputAction> sharedActions = readActionList(buffer, actions);
        List<Integer> chances = readIntList(
                buffer, CompactRecipeWireLimits.MAX_IO_PER_RELATION);
        String sourceKind = readBounded(
                buffer, CompactRecipeWireLimits.MAX_SOURCE_KIND_LENGTH);
        String selected = readBounded(
                buffer, CompactRecipeWireLimits.MAX_SELECTED_SOURCE_LENGTH);
        List<List<Ingredient>> itemInputs = readConfigList(
                buffer,
                CompactRecipeWireLimits.MAX_DICTIONARY_ENTRIES,
                "item input configs",
                CompactRecipeWireValues::decodeIngredient);
        List<List<ItemStack>> itemOutputs = readConfigList(
                buffer,
                CompactRecipeWireLimits.MAX_DICTIONARY_ENTRIES,
                "item output configs",
                CompactRecipeWireValues::decodeItemStack);
        int fluidCount = readCount(
                buffer,
                CompactRecipeWireLimits.MAX_DICTIONARY_ENTRIES,
                "fluid configs");
        List<CompactGTRecipeFamilyDefinition.FluidIo> fluids =
                new ArrayList<>(fluidCount);
        for (int index = 0; index < fluidCount; index++) {
            fluids.add(new CompactGTRecipeFamilyDefinition.FluidIo(
                    readNamedList(
                            buffer,
                            CompactRecipeWireLimits.MAX_IO_PER_RELATION,
                            "fluid inputs",
                            CompactRecipeWireValues::decodeFluidStack),
                    readNamedList(
                            buffer,
                            CompactRecipeWireLimits.MAX_IO_PER_RELATION,
                            "fluid outputs",
                            CompactRecipeWireValues::decodeFluidStack)));
        }
        List<ResourceLocation> prefixes = readDictionary(
                buffer,
                CompactRecipeWireLimits.MAX_STABLE_ID_PREFIXES,
                "stable-id prefix dictionary",
                ResourceLocation.STREAM_CODEC);
        int rowCount = readCount(
                buffer,
                CompactRecipeWireLimits.DECODE_RELATIONS_CEILING,
                "relations");
        List<CompactGTRecipeFamilyDefinition.MatrixRow> rows =
                new ArrayList<>(rowCount);
        for (int index = 0; index < rowCount; index++) {
            ResourceLocation stableId = decodeStableId(buffer, prefixes);
            int inputIdx = readIndex(buffer, itemInputs.size(), "item_inputs");
            int outputIdx = readIndex(buffer, itemOutputs.size(), "item_outputs");
            int fluidIdx = readIndex(buffer, fluids.size(), "fluids");
            int shadowOrder = buffer.readVarInt();
            rows.add(new CompactGTRecipeFamilyDefinition.MatrixRow(
                    inputIdx,
                    outputIdx,
                    fluidIdx,
                    stableId,
                    shadowOrder));
        }
        return new CompactGTRecipeFamilyDefinition.AuthoredMatrixV1(
                new CompactGTRecipeFamilyDefinition.SharedSpec(
                        duration,
                        eut,
                        special,
                        buffered,
                        counts,
                        sharedActions,
                        chances,
                        sourceKind,
                        selected),
                new CompactGTRecipeFamilyDefinition.MatrixDicts(
                        itemInputs,
                        itemOutputs,
                        fluids),
                rows);
    }

    private static void writeActionDictionaryAndList(
            RegistryFriendlyByteBuf buffer,
            List<ItemInputAction> actions) {
        List<ItemInputAction> unique = new ArrayList<>();
        Map<ItemInputAction, Integer> index = new LinkedHashMap<>();
        for (ItemInputAction action : actions) {
            internValue(
                    action,
                    index,
                    unique,
                    CompactRecipeWireLimits.MAX_ACTIONS);
        }
        writeDictionary(buffer, unique, ItemInputAction.STREAM_CODEC);
        if (actions.size() > CompactRecipeWireLimits.MAX_IO_PER_RELATION) {
            throw new EncoderException(
                    "Compact family item input actions exceed "
                            + CompactRecipeWireLimits.MAX_IO_PER_RELATION);
        }
        buffer.writeVarInt(actions.size());
        for (ItemInputAction action : actions) {
            buffer.writeVarInt(index.get(action));
        }
    }

    private static <T> void writeConfigList(
            RegistryFriendlyByteBuf buffer,
            List<List<T>> configs,
            BiConsumer<RegistryFriendlyByteBuf, T> encoder,
            String label) {
        if (configs.size() > CompactRecipeWireLimits.MAX_DICTIONARY_ENTRIES) {
            throw new EncoderException(
                    "Compact family " + label + " exceed "
                            + CompactRecipeWireLimits.MAX_DICTIONARY_ENTRIES);
        }
        buffer.writeVarInt(configs.size());
        for (List<T> config : configs) {
            if (config.size() > CompactRecipeWireLimits.MAX_IO_PER_RELATION) {
                throw new EncoderException(
                        "Compact family " + label + " row exceeds "
                                + CompactRecipeWireLimits.MAX_IO_PER_RELATION);
            }
            writeNamedList(buffer, config, encoder);
        }
    }

    private static <T> List<List<T>> readConfigList(
            RegistryFriendlyByteBuf buffer,
            int max,
            String label,
            Function<RegistryFriendlyByteBuf, T> decoder) {
        int count = readCount(buffer, max, label);
        List<List<T>> configs = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            configs.add(readNamedList(
                    buffer,
                    CompactRecipeWireLimits.MAX_IO_PER_RELATION,
                    label,
                    decoder));
        }
        return configs;
    }

    private static void encodeRelation(
            RegistryFriendlyByteBuf buffer,
            Dictionaries dictionaries,
            SharedFields shared,
            CompactGTRecipeFamilyDefinition.Relation relation) {
        encodeStableId(buffer, dictionaries, relation.stableId());
        writeIndexList(
                buffer,
                relation.itemInputs(),
                dictionaries.ingredientIndex,
                CompactRecipeWireLimits.MAX_IO_PER_RELATION,
                "item inputs",
                CompactRecipeWireValues::encodeIngredient);
        if (!shared.has(FLAG_COUNTS)) {
            writeIntList(buffer, relation.itemInputCounts());
        }
        if (!shared.has(FLAG_ACTIONS)) {
            writeActionIndices(buffer, dictionaries, relation);
        }
        writeIndexList(
                buffer,
                relation.itemOutputs(),
                dictionaries.itemStackIndex,
                CompactRecipeWireLimits.MAX_IO_PER_RELATION,
                "item outputs",
                CompactRecipeWireValues::encodeItemStack);
        writeIndexList(
                buffer,
                relation.fluidInputs(),
                dictionaries.fluidIndex,
                CompactRecipeWireLimits.MAX_IO_PER_RELATION,
                "fluid inputs",
                CompactRecipeWireValues::encodeFluidStack);
        writeIndexList(
                buffer,
                relation.fluidOutputs(),
                dictionaries.fluidIndex,
                CompactRecipeWireLimits.MAX_IO_PER_RELATION,
                "fluid outputs",
                CompactRecipeWireValues::encodeFluidStack);
        if (!shared.has(FLAG_CHANCES)) {
            writeIntList(buffer, relation.outputChances());
        }
        if (!shared.has(FLAG_DURATION)) {
            buffer.writeVarInt(relation.duration());
        }
        if (!shared.has(FLAG_EUT)) {
            buffer.writeVarLong(relation.eut());
        }
        if (!shared.has(FLAG_SPECIAL)) {
            buffer.writeVarLong(relation.specialValue());
        }
        if (!shared.has(FLAG_BUFFERED)) {
            buffer.writeBoolean(relation.canBeBuffered());
        }
        if (!shared.has(FLAG_SHADOW)) {
            buffer.writeVarInt(relation.shadowOrder());
        }
        ProvenanceKey key = ProvenanceKey.from(relation.provenance());
        Integer provenanceIndex = dictionaries.provenanceIndex.get(key);
        if (provenanceIndex == null) {
            throw new EncoderException("Missing provenance dictionary entry");
        }
        buffer.writeVarInt(provenanceIndex);
    }

    private static CompactGTRecipeFamilyDefinition.Relation decodeRelation(
            RegistryFriendlyByteBuf buffer,
            List<Ingredient> ingredients,
            List<ItemStack> itemStacks,
            List<FluidStack> fluids,
            List<ProvenanceKey> provenanceKeys,
            List<ItemInputAction> actions,
            List<ResourceLocation> prefixes,
            Integer sharedDuration,
            Long sharedEut,
            Long sharedSpecial,
            Boolean sharedBuffered,
            Integer sharedShadow,
            List<Integer> sharedCounts,
            List<ItemInputAction> sharedActions,
            List<Integer> sharedChances) {
        ResourceLocation stableId = decodeStableId(buffer, prefixes);
        List<Ingredient> itemInputs = readIndexed(
                buffer, ingredients, CompactRecipeWireLimits.MAX_IO_PER_RELATION, "item inputs");
        List<Integer> itemInputCounts = sharedCounts != null
                ? sharedCounts
                : readIntList(buffer, CompactRecipeWireLimits.MAX_IO_PER_RELATION);
        List<ItemInputAction> itemInputActions = sharedActions != null
                ? sharedActions
                : readActionList(buffer, actions);
        List<ItemStack> itemOutputs = readIndexed(
                buffer, itemStacks, CompactRecipeWireLimits.MAX_IO_PER_RELATION, "item outputs");
        List<FluidStack> fluidInputs = readIndexed(
                buffer, fluids, CompactRecipeWireLimits.MAX_IO_PER_RELATION, "fluid inputs");
        List<FluidStack> fluidOutputs = readIndexed(
                buffer, fluids, CompactRecipeWireLimits.MAX_IO_PER_RELATION, "fluid outputs");
        List<Integer> outputChances = sharedChances != null
                ? sharedChances
                : readIntList(buffer, CompactRecipeWireLimits.MAX_IO_PER_RELATION);
        int duration = sharedDuration != null ? sharedDuration : buffer.readVarInt();
        long eut = sharedEut != null ? sharedEut : buffer.readVarLong();
        long specialValue = sharedSpecial != null ? sharedSpecial : buffer.readVarLong();
        boolean canBeBuffered = sharedBuffered != null
                ? sharedBuffered : buffer.readBoolean();
        int shadowOrder = sharedShadow != null ? sharedShadow : buffer.readVarInt();
        ProvenanceKey key = provenanceKeys.get(readIndex(
                buffer, provenanceKeys.size(), "provenance"));
        return new CompactGTRecipeFamilyDefinition.Relation(
                stableId,
                itemInputs,
                itemInputCounts,
                itemInputActions,
                itemOutputs,
                fluidInputs,
                fluidOutputs,
                outputChances,
                duration,
                eut,
                specialValue,
                canBeBuffered,
                shadowOrder,
                new GTRecipeProvenance(
                        key.sourceKind(),
                        key.selectedSourceRecipe()));
    }

    private static void encodeStableId(
            RegistryFriendlyByteBuf buffer,
            Dictionaries dictionaries,
            ResourceLocation stableId) {
        Optional<SplitStableId> split = splitStableId(stableId);
        if (split.isEmpty()) {
            buffer.writeByte(STABLE_ID_FULL);
            ResourceLocation.STREAM_CODEC.encode(buffer, stableId);
            return;
        }
        Integer prefixIndex = dictionaries.prefixIndex.get(split.get().prefix());
        if (prefixIndex == null) {
            throw new EncoderException("Missing stable-id prefix dictionary entry");
        }
        buffer.writeByte(STABLE_ID_SPLIT);
        buffer.writeVarInt(prefixIndex);
        buffer.writeLong(split.get().suffix());
    }

    private static ResourceLocation decodeStableId(
            RegistryFriendlyByteBuf buffer,
            List<ResourceLocation> prefixes) {
        int mode = buffer.readUnsignedByte();
        if (mode == STABLE_ID_FULL) {
            return ResourceLocation.STREAM_CODEC.decode(buffer);
        }
        if (mode != STABLE_ID_SPLIT) {
            throw new DecoderException("Unknown compact stable-id mode: " + mode);
        }
        ResourceLocation prefix = prefixes.get(
                readIndex(buffer, prefixes.size(), "stable-id prefix"));
        return joinStableId(prefix, buffer.readLong());
    }

    private static Optional<SplitStableId> splitStableId(ResourceLocation id) {
        String path = id.getPath();
        int slash = path.lastIndexOf('/');
        if (slash <= 0 || slash == path.length() - 1) {
            return Optional.empty();
        }
        String last = path.substring(slash + 1);
        if (last.length() != CompactRecipeWireLimits.STABLE_ID_SUFFIX_HEX_LENGTH) {
            return Optional.empty();
        }
        for (int index = 0; index < last.length(); index++) {
            if (Character.digit(last.charAt(index), 16) < 0) {
                return Optional.empty();
            }
        }
        ResourceLocation prefix = ResourceLocation.fromNamespaceAndPath(
                id.getNamespace(), path.substring(0, slash));
        return Optional.of(new SplitStableId(
                prefix, Long.parseUnsignedLong(last, 16)));
    }

    private static ResourceLocation joinStableId(ResourceLocation prefix, long suffix) {
        String hex = Long.toUnsignedString(suffix, 16);
        if (hex.length() < CompactRecipeWireLimits.STABLE_ID_SUFFIX_HEX_LENGTH) {
            hex = "0".repeat(
                    CompactRecipeWireLimits.STABLE_ID_SUFFIX_HEX_LENGTH - hex.length())
                    + hex;
        }
        return ResourceLocation.fromNamespaceAndPath(
                prefix.getNamespace(), prefix.getPath() + "/" + hex);
    }

    private static void encodeTransportFragment(
            RegistryFriendlyByteBuf buffer,
            Optional<CompactGTRecipeFamilyDefinition.TransportFragment> fragment) {
        buffer.writeBoolean(fragment.isPresent());
        if (fragment.isEmpty()) {
            return;
        }
        CompactGTRecipeFamilyDefinition.TransportFragment value = fragment.get();
        buffer.writeVarInt(value.index());
        buffer.writeVarInt(value.count());
        buffer.writeVarInt(value.totalRelations());
        writeBounded(
                buffer,
                value.semanticDigest(),
                CompactRecipeWireLimits.MAX_SEMANTIC_DIGEST_LENGTH);
    }

    private static Optional<CompactGTRecipeFamilyDefinition.TransportFragment>
            decodeTransportFragment(RegistryFriendlyByteBuf buffer) {
        if (!buffer.readBoolean()) {
            return Optional.empty();
        }
        int index = buffer.readVarInt();
        int count = readCount(
                buffer,
                CompactRecipeWireLimits.MAX_TRANSPORT_FRAGMENTS,
                "transport fragments");
        int totalRelations = buffer.readVarInt();
        String digest = readBounded(
                buffer, CompactRecipeWireLimits.MAX_SEMANTIC_DIGEST_LENGTH);
        try {
            return Optional.of(new CompactGTRecipeFamilyDefinition.TransportFragment(
                    index, count, totalRelations, digest));
        } catch (IllegalArgumentException failure) {
            throw new DecoderException(failure.getMessage());
        }
    }

    private static void encodeParameterized(
            RegistryFriendlyByteBuf buffer,
            Optional<CompactGTRecipeFamilyDefinition.ParameterizedSpec> parameterized) {
        buffer.writeBoolean(parameterized.isPresent());
        if (parameterized.isEmpty()) {
            return;
        }
        CompactGTRecipeFamilyDefinition.ParameterizedSpec spec = parameterized.get();
        writeBounded(
                buffer,
                spec.template(),
                CompactRecipeWireLimits.MAX_TEMPLATE_LENGTH);
        Map<String, String> parameters = new TreeMap<>(spec.parameters());
        if (parameters.size() > CompactRecipeWireLimits.MAX_PARAMETERIZED_ENTRIES) {
            throw new EncoderException(
                    "Compact family parameterized entries exceed "
                            + CompactRecipeWireLimits.MAX_PARAMETERIZED_ENTRIES);
        }
        buffer.writeVarInt(parameters.size());
        for (var entry : parameters.entrySet()) {
            writeBounded(
                    buffer,
                    entry.getKey(),
                    CompactRecipeWireLimits.MAX_PARAMETER_KEY_LENGTH);
            writeBounded(
                    buffer,
                    entry.getValue(),
                    CompactRecipeWireLimits.MAX_PARAMETER_VALUE_LENGTH);
        }
    }

    private static Optional<CompactGTRecipeFamilyDefinition.ParameterizedSpec>
            decodeParameterized(RegistryFriendlyByteBuf buffer) {
        if (!buffer.readBoolean()) {
            return Optional.empty();
        }
        String template = readBounded(
                buffer, CompactRecipeWireLimits.MAX_TEMPLATE_LENGTH);
        int count = readCount(
                buffer,
                CompactRecipeWireLimits.MAX_PARAMETERIZED_ENTRIES,
                "parameterized entries");
        Map<String, String> parameters = new LinkedHashMap<>();
        for (int index = 0; index < count; index++) {
            String key = readBounded(
                    buffer, CompactRecipeWireLimits.MAX_PARAMETER_KEY_LENGTH);
            String value = readBounded(
                    buffer, CompactRecipeWireLimits.MAX_PARAMETER_VALUE_LENGTH);
            if (parameters.put(key, value) != null) {
                throw new DecoderException(
                        "Duplicate parameterized key: " + key);
            }
        }
        return Optional.of(new CompactGTRecipeFamilyDefinition.ParameterizedSpec(
                template, parameters));
    }

    private static void writeProvenanceKeys(
            RegistryFriendlyByteBuf buffer, List<ProvenanceKey> keys) {
        if (keys.size() > CompactRecipeWireLimits.MAX_PROVENANCE_KEYS) {
            throw new EncoderException(
                    "Compact family provenance dictionary exceeds "
                            + CompactRecipeWireLimits.MAX_PROVENANCE_KEYS);
        }
        buffer.writeVarInt(keys.size());
        for (ProvenanceKey key : keys) {
            writeBounded(
                    buffer,
                    key.sourceKind(),
                    CompactRecipeWireLimits.MAX_SOURCE_KIND_LENGTH);
            buffer.writeBoolean(key.selectedSourceRecipe().isPresent());
            key.selectedSourceRecipe().ifPresent(selected -> writeBounded(
                    buffer,
                    selected,
                    CompactRecipeWireLimits.MAX_SELECTED_SOURCE_LENGTH));
        }
    }

    private static List<ProvenanceKey> readProvenanceKeys(RegistryFriendlyByteBuf buffer) {
        int count = readCount(
                buffer,
                CompactRecipeWireLimits.MAX_PROVENANCE_KEYS,
                "provenance dictionary");
        List<ProvenanceKey> keys = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            String sourceKind = readBounded(
                    buffer, CompactRecipeWireLimits.MAX_SOURCE_KIND_LENGTH);
            Optional<String> selected = Optional.empty();
            if (buffer.readBoolean()) {
                selected = Optional.of(readBounded(
                        buffer, CompactRecipeWireLimits.MAX_SELECTED_SOURCE_LENGTH));
            }
            keys.add(new ProvenanceKey(sourceKind, selected));
        }
        return keys;
    }

    private static <T> void writeNamedList(
            RegistryFriendlyByteBuf buffer,
            List<T> values,
            BiConsumer<RegistryFriendlyByteBuf, T> encoder) {
        if (values.size() > CompactRecipeWireLimits.MAX_DICTIONARY_ENTRIES) {
            throw new EncoderException(
                    "Compact family dictionary exceeds "
                            + CompactRecipeWireLimits.MAX_DICTIONARY_ENTRIES);
        }
        buffer.writeVarInt(values.size());
        for (T value : values) {
            encoder.accept(buffer, value);
        }
    }

    private static <T> List<T> readNamedList(
            RegistryFriendlyByteBuf buffer,
            int max,
            String label,
            Function<RegistryFriendlyByteBuf, T> decoder) {
        int count = readCount(buffer, max, label);
        List<T> values = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            values.add(decoder.apply(buffer));
        }
        return values;
    }

    private static <T> void writeDictionary(
            RegistryFriendlyByteBuf buffer,
            List<T> values,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        if (values.size() > CompactRecipeWireLimits.MAX_DICTIONARY_ENTRIES) {
            throw new EncoderException(
                    "Compact family dictionary exceeds "
                            + CompactRecipeWireLimits.MAX_DICTIONARY_ENTRIES);
        }
        buffer.writeVarInt(values.size());
        for (T value : values) {
            codec.encode(buffer, value);
        }
    }

    private static <T> List<T> readDictionary(
            RegistryFriendlyByteBuf buffer,
            int max,
            String label,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        int count = readCount(buffer, max, label);
        List<T> values = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            values.add(codec.decode(buffer));
        }
        return values;
    }

    private static void writeActionIndices(
            RegistryFriendlyByteBuf buffer,
            Dictionaries dictionaries,
            CompactGTRecipeFamilyDefinition.Relation relation) {
        List<ItemInputAction> actions = relation.itemInputActions();
        if (actions.size() > CompactRecipeWireLimits.MAX_IO_PER_RELATION) {
            throw new EncoderException(
                    "Compact family item input actions exceed "
                            + CompactRecipeWireLimits.MAX_IO_PER_RELATION);
        }
        buffer.writeVarInt(actions.size());
        for (ItemInputAction action : actions) {
            Integer index = dictionaries.actionIndex.get(action);
            if (index == null) {
                throw new EncoderException("Missing action dictionary entry");
            }
            buffer.writeVarInt(index);
        }
    }

    private static List<ItemInputAction> readActionList(
            RegistryFriendlyByteBuf buffer,
            List<ItemInputAction> actions) {
        int count = readCount(
                buffer,
                CompactRecipeWireLimits.MAX_IO_PER_RELATION,
                "item input actions");
        List<ItemInputAction> decoded = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            decoded.add(actions.get(readIndex(buffer, actions.size(), "action")));
        }
        return decoded;
    }

    private static <T> void writeIndexList(
            RegistryFriendlyByteBuf buffer,
            List<T> values,
            Map<BytesKey, Integer> indexByKey,
            int max,
            String label,
            BiConsumer<RegistryFriendlyByteBuf, T> encoder) {
        if (values.size() > max) {
            throw new EncoderException(
                    "Compact family " + label + " exceed " + max);
        }
        buffer.writeVarInt(values.size());
        for (T value : values) {
            Integer index = indexByKey.get(new BytesKey(
                    encodeBytes(buffer, encoder, value)));
            if (index == null) {
                throw new EncoderException("Missing dictionary entry for " + label);
            }
            buffer.writeVarInt(index);
        }
    }

    private static <T> List<T> readIndexed(
            RegistryFriendlyByteBuf buffer,
            List<T> dictionary,
            int max,
            String label) {
        int count = readCount(buffer, max, label);
        List<T> values = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            values.add(dictionary.get(readIndex(buffer, dictionary.size(), label)));
        }
        return values;
    }

    private static void writeIntList(RegistryFriendlyByteBuf buffer, List<Integer> values) {
        if (values.size() > CompactRecipeWireLimits.MAX_IO_PER_RELATION) {
            throw new EncoderException(
                    "Compact family integer list exceeds "
                            + CompactRecipeWireLimits.MAX_IO_PER_RELATION);
        }
        buffer.writeVarInt(values.size());
        for (int value : values) {
            buffer.writeVarInt(value);
        }
    }

    private static List<Integer> readIntList(RegistryFriendlyByteBuf buffer, int max) {
        int count = readCount(buffer, max, "integer list");
        List<Integer> values = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            values.add(buffer.readVarInt());
        }
        return values;
    }

    private static void writeBounded(
            RegistryFriendlyByteBuf buffer, String value, int maxChars) {
        if (value.length() > maxChars) {
            throw new EncoderException(
                    "Compact family string exceeds " + maxChars + " characters");
        }
        buffer.writeUtf(value, maxChars);
    }

    private static String readBounded(RegistryFriendlyByteBuf buffer, int maxChars) {
        return buffer.readUtf(maxChars);
    }

    private static int readCount(RegistryFriendlyByteBuf buffer, int max, String label) {
        int count = buffer.readVarInt();
        if (count < 0 || count > max) {
            throw new DecoderException(
                    "Compact family " + label + " count " + count + " exceeds " + max);
        }
        return count;
    }

    private static int readIndex(RegistryFriendlyByteBuf buffer, int dictSize, String label) {
        int index = buffer.readVarInt();
        if (index < 0 || index >= dictSize) {
            throw new DecoderException(
                    "Compact family " + label + " index " + index
                            + " out of range " + dictSize);
        }
        return index;
    }

    private static RegistryFriendlyByteBuf wrap(ByteBuf source, RegistryFriendlyByteBuf parent) {
        return new RegistryFriendlyByteBuf(
                source, parent.registryAccess(), parent.getConnectionType());
    }

    private static int varIntBytes(int value) {
        int unsigned = value;
        int bytes = 1;
        while ((unsigned & -128) != 0) {
            unsigned >>>= 7;
            bytes++;
        }
        return bytes;
    }

    private static <T> byte[] encodeBytes(
            RegistryFriendlyByteBuf parent,
            BiConsumer<RegistryFriendlyByteBuf, T> encoder,
            T value) {
        ByteBuf raw = Unpooled.buffer();
        try {
            RegistryFriendlyByteBuf temp = wrap(raw, parent);
            encoder.accept(temp, value);
            byte[] bytes = new byte[temp.readableBytes()];
            temp.readBytes(bytes);
            return bytes;
        } finally {
            raw.release();
        }
    }

    private static <T> byte[] encodeBytes(
            RegistryFriendlyByteBuf parent,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            T value) {
        return encodeBytes(parent, codec::encode, value);
    }

    private record SplitStableId(ResourceLocation prefix, long suffix) {}

    private record ProvenanceKey(
            String sourceKind, Optional<String> selectedSourceRecipe) {
        static ProvenanceKey from(GTRecipeProvenance provenance) {
            return new ProvenanceKey(
                    provenance.sourceKind(), provenance.selectedSourceRecipe());
        }
    }

    private record BytesKey(byte[] bytes) {
        @Override
        public boolean equals(Object other) {
            return other instanceof BytesKey key && Arrays.equals(bytes, key.bytes);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(bytes);
        }
    }

    private record SharedFields(int flags) {
        boolean has(int flag) {
            return (flags & flag) != 0;
        }

        static SharedFields of(List<CompactGTRecipeFamilyDefinition.Relation> relations) {
            if (relations.isEmpty()) {
                return new SharedFields(0);
            }
            CompactGTRecipeFamilyDefinition.Relation first = relations.getFirst();
            int flags = 0;
            if (relations.stream().allMatch(relation -> relation.duration() == first.duration())) {
                flags |= FLAG_DURATION;
            }
            if (relations.stream().allMatch(relation -> relation.eut() == first.eut())) {
                flags |= FLAG_EUT;
            }
            if (relations.stream().allMatch(
                    relation -> relation.specialValue() == first.specialValue())) {
                flags |= FLAG_SPECIAL;
            }
            if (relations.stream().allMatch(
                    relation -> relation.canBeBuffered() == first.canBeBuffered())) {
                flags |= FLAG_BUFFERED;
            }
            if (relations.stream().allMatch(
                    relation -> relation.shadowOrder() == first.shadowOrder())) {
                flags |= FLAG_SHADOW;
            }
            if (relations.stream().allMatch(
                    relation -> relation.itemInputCounts().equals(first.itemInputCounts()))) {
                flags |= FLAG_COUNTS;
            }
            if (relations.stream().allMatch(
                    relation -> relation.itemInputActions().equals(first.itemInputActions()))) {
                flags |= FLAG_ACTIONS;
            }
            if (relations.stream().allMatch(
                    relation -> relation.outputChances().equals(first.outputChances()))) {
                flags |= FLAG_CHANCES;
            }
            return new SharedFields(flags);
        }
    }

    private static final class Dictionaries {
        private final List<Ingredient> ingredients = new ArrayList<>();
        private final List<ItemStack> itemStacks = new ArrayList<>();
        private final List<FluidStack> fluids = new ArrayList<>();
        private final List<ProvenanceKey> provenanceKeys = new ArrayList<>();
        private final List<ItemInputAction> actions = new ArrayList<>();
        private final List<ResourceLocation> stableIdPrefixes = new ArrayList<>();
        private final Map<BytesKey, Integer> ingredientIndex = new LinkedHashMap<>();
        private final Map<BytesKey, Integer> itemStackIndex = new LinkedHashMap<>();
        private final Map<BytesKey, Integer> fluidIndex = new LinkedHashMap<>();
        private final Map<ProvenanceKey, Integer> provenanceIndex = new LinkedHashMap<>();
        private final Map<ItemInputAction, Integer> actionIndex = new LinkedHashMap<>();
        private final Map<ResourceLocation, Integer> prefixIndex = new LinkedHashMap<>();

        static Dictionaries prefixes(
                Map<ResourceLocation, Integer> prefixIndex,
                List<ResourceLocation> prefixes) {
            Dictionaries dictionaries = new Dictionaries();
            dictionaries.prefixIndex.putAll(prefixIndex);
            dictionaries.stableIdPrefixes.addAll(prefixes);
            return dictionaries;
        }

        static Dictionaries build(
                RegistryFriendlyByteBuf buffer,
                List<CompactGTRecipeFamilyDefinition.Relation> relations) {
            Dictionaries dictionaries = new Dictionaries();
            for (CompactGTRecipeFamilyDefinition.Relation relation : relations) {
                for (Ingredient ingredient : relation.itemInputs()) {
                    internNamed(
                            buffer,
                            ingredient,
                            CompactRecipeWireValues::encodeIngredient,
                            dictionaries.ingredientIndex,
                            dictionaries.ingredients,
                            CompactRecipeWireLimits.MAX_DICTIONARY_ENTRIES);
                }
                for (ItemStack stack : relation.itemOutputs()) {
                    internNamed(
                            buffer,
                            stack,
                            CompactRecipeWireValues::encodeItemStack,
                            dictionaries.itemStackIndex,
                            dictionaries.itemStacks,
                            CompactRecipeWireLimits.MAX_DICTIONARY_ENTRIES);
                }
                for (FluidStack stack : relation.fluidInputs()) {
                    internNamed(
                            buffer,
                            stack,
                            CompactRecipeWireValues::encodeFluidStack,
                            dictionaries.fluidIndex,
                            dictionaries.fluids,
                            CompactRecipeWireLimits.MAX_DICTIONARY_ENTRIES);
                }
                for (FluidStack stack : relation.fluidOutputs()) {
                    internNamed(
                            buffer,
                            stack,
                            CompactRecipeWireValues::encodeFluidStack,
                            dictionaries.fluidIndex,
                            dictionaries.fluids,
                            CompactRecipeWireLimits.MAX_DICTIONARY_ENTRIES);
                }
                internValue(
                        ProvenanceKey.from(relation.provenance()),
                        dictionaries.provenanceIndex,
                        dictionaries.provenanceKeys,
                        CompactRecipeWireLimits.MAX_PROVENANCE_KEYS);
                for (ItemInputAction action : relation.itemInputActions()) {
                    internValue(
                            action,
                            dictionaries.actionIndex,
                            dictionaries.actions,
                            CompactRecipeWireLimits.MAX_ACTIONS);
                }
                splitStableId(relation.stableId()).ifPresent(split -> internValue(
                        split.prefix(),
                        dictionaries.prefixIndex,
                        dictionaries.stableIdPrefixes,
                        CompactRecipeWireLimits.MAX_STABLE_ID_PREFIXES));
            }
            return dictionaries;
        }
    }

    private static <T> int internNamed(
            RegistryFriendlyByteBuf buffer,
            T value,
            BiConsumer<RegistryFriendlyByteBuf, T> encoder,
            Map<BytesKey, Integer> indexByKey,
            List<T> values,
            int max) {
        BytesKey key = new BytesKey(encodeBytes(buffer, encoder, value));
        Integer existing = indexByKey.get(key);
        if (existing != null) {
            return existing;
        }
        if (values.size() >= max) {
            throw new EncoderException(
                    "Compact family dictionary exceeds " + max);
        }
        int index = values.size();
        indexByKey.put(key, index);
        values.add(value);
        return index;
    }

    private static <T> int internValue(
            T value,
            Map<T, Integer> indexByKey,
            List<T> values,
            int max) {
        Integer existing = indexByKey.get(value);
        if (existing != null) {
            return existing;
        }
        if (values.size() >= max) {
            throw new EncoderException(
                    "Compact family dictionary exceeds " + max);
        }
        int index = values.size();
        indexByKey.put(value, index);
        values.add(value);
        return index;
    }
}
