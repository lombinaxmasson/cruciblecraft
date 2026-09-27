package com.masson.cruciblecraft.recipe.gt;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonObject;
import com.masson.cruciblecraft.recipe.crafting.CraftingToolIngredient;
import com.masson.cruciblecraft.recipe.crafting.CraftingTools;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Codec;

import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Name-based binary payloads for items, fluids, and ingredients. Recipe sync
 * must not depend on connection-local registry numeric IDs.
 */
final class CompactRecipeWireValues {
    private static final byte INGREDIENT_ITEM = 0;
    private static final byte INGREDIENT_TAG = 1;
    private static final byte INGREDIENT_COMPONENTS = 2;
    private static final byte HOLDER_ITEMS = 0;
    private static final byte HOLDER_TAG = 1;
    private static final byte COMPONENT_INT = 0;
    private static final byte COMPONENT_TEXT = 1;
    private static final byte COMPONENT_STRING = 2;
    private static final byte COMPONENT_BLOCK_STATE = 3;
    private static final byte TYPE_NAMED = 0;
    private static final byte TYPE_BLOCK_STATE = 1;
    private static final byte TYPE_ORE_MATERIAL = 2;
    private static final byte TYPE_PREFIX_MATERIAL = 3;
    private static final byte TYPE_TOOL_MATERIAL = 4;
    private static final byte TYPE_MACHINE_MATERIAL = 5;
    private static final byte TYPE_CIRCUIT_CONFIG = 6;
    private static final byte TYPE_FIREPROOF = 7;
    private static final ResourceLocation BLOCK_STATE_ID =
            ResourceLocation.withDefaultNamespace("block_state");
    private static final ResourceLocation ORE_MATERIAL_ID =
            ResourceLocation.fromNamespaceAndPath("cruciblecraft", "ore_material");
    private static final ResourceLocation PREFIX_MATERIAL_ID =
            ResourceLocation.fromNamespaceAndPath("cruciblecraft", "prefix_material");
    private static final ResourceLocation TOOL_MATERIAL_ID =
            ResourceLocation.fromNamespaceAndPath("cruciblecraft", "tool_material");
    private static final ResourceLocation MACHINE_MATERIAL_ID =
            ResourceLocation.fromNamespaceAndPath("cruciblecraft", "machine_material");
    private static final ResourceLocation CIRCUIT_CONFIG_ID =
            ResourceLocation.fromNamespaceAndPath("cruciblecraft", "circuit_config");
    private static final ResourceLocation FIREPROOF_ID =
            ResourceLocation.fromNamespaceAndPath("cruciblecraft", "fireproof");

    private CompactRecipeWireValues() {}

    static void encodeIngredient(RegistryFriendlyByteBuf buffer, Ingredient ingredient) {
        ICustomIngredient custom = ingredient.getCustomIngredient();
        if (custom instanceof DataComponentIngredient components) {
            buffer.writeByte(INGREDIENT_COMPONENTS);
            encodeHolderSet(buffer, components.items());
            buffer.writeBoolean(components.isStrict());
            encodePredicate(buffer, components.components());
            return;
        }
        if (custom instanceof CraftingToolIngredient tool) {
            buffer.writeByte(INGREDIENT_TAG);
            ResourceLocation.STREAM_CODEC.encode(buffer, tool.tag().location());
            return;
        }
        if (custom != null) {
            throw new EncoderException(
                    "Unsupported custom ingredient " + custom.getClass().getName());
        }
        JsonObject json = encodeJson(buffer, Ingredient.CODEC_NONEMPTY, ingredient);
        if (json.has("tag")) {
            buffer.writeByte(INGREDIENT_TAG);
            ResourceLocation.STREAM_CODEC.encode(
                    buffer, ResourceLocation.parse(json.get("tag").getAsString()));
            return;
        }
        if (json.has("item")) {
            buffer.writeByte(INGREDIENT_ITEM);
            ResourceLocation.STREAM_CODEC.encode(
                    buffer, ResourceLocation.parse(json.get("item").getAsString()));
            return;
        }
        throw new EncoderException("Unsupported ingredient shape " + json);
    }

    static Ingredient decodeIngredient(RegistryFriendlyByteBuf buffer) {
        int kind = buffer.readUnsignedByte();
        return switch (kind) {
            case INGREDIENT_ITEM -> Ingredient.of(item(readItemId(buffer)));
            case INGREDIENT_TAG -> {
                ResourceLocation tagId = ResourceLocation.STREAM_CODEC.decode(buffer);
                yield CraftingTools.tryTag(tagId).orElseGet(() -> Ingredient.of(
                        TagKey.create(Registries.ITEM, tagId)));
            }
            case INGREDIENT_COMPONENTS -> {
                HolderSet<Item> items = decodeHolderSet(buffer);
                boolean strict = buffer.readBoolean();
                yield DataComponentIngredient.of(strict, decodePredicate(buffer), items);
            }
            default -> throw new DecoderException(
                    "Unknown compact ingredient kind: " + kind);
        };
    }

    static void encodeItemStack(RegistryFriendlyByteBuf buffer, ItemStack stack) {
        ResourceLocation.STREAM_CODEC.encode(buffer, itemId(stack.getItem()));
        buffer.writeVarInt(stack.getCount());
        encodePatch(buffer, stack.getComponentsPatch());
    }

    static ItemStack decodeItemStack(RegistryFriendlyByteBuf buffer) {
        Item item = item(readItemId(buffer));
        int count = buffer.readVarInt();
        DataComponentPatch patch = decodePatch(buffer);
        ItemStack stack = new ItemStack(item, count);
        stack.applyComponents(patch);
        return stack;
    }

    static void encodeFluidStack(RegistryFriendlyByteBuf buffer, FluidStack stack) {
        ResourceLocation.STREAM_CODEC.encode(buffer, fluidId(stack.getFluid()));
        buffer.writeVarInt(stack.getAmount());
    }

    static FluidStack decodeFluidStack(RegistryFriendlyByteBuf buffer) {
        Fluid fluid = fluid(ResourceLocation.STREAM_CODEC.decode(buffer));
        int amount = buffer.readVarInt();
        return new FluidStack(fluid, amount);
    }

    private static void encodeHolderSet(
            RegistryFriendlyByteBuf buffer, HolderSet<Item> items) {
        items.unwrap().ifLeft(tag -> {
            buffer.writeByte(HOLDER_TAG);
            ResourceLocation.STREAM_CODEC.encode(buffer, tag.location());
        }).ifRight(holders -> {
            buffer.writeByte(HOLDER_ITEMS);
            if (holders.size() > CompactRecipeWireLimits.MAX_IO_PER_RELATION) {
                throw new EncoderException(
                        "Compact ingredient item list exceeds "
                                + CompactRecipeWireLimits.MAX_IO_PER_RELATION);
            }
            buffer.writeVarInt(holders.size());
            for (Holder<Item> holder : holders) {
                ResourceLocation.STREAM_CODEC.encode(buffer, itemId(holder.value()));
            }
        });
    }

    private static HolderSet<Item> decodeHolderSet(RegistryFriendlyByteBuf buffer) {
        int kind = buffer.readUnsignedByte();
        if (kind == HOLDER_TAG) {
            TagKey<Item> tag = TagKey.create(
                    Registries.ITEM, ResourceLocation.STREAM_CODEC.decode(buffer));
            return BuiltInRegistries.ITEM.getTag(tag)
                    .map(named -> (HolderSet<Item>) named)
                    .orElseGet(() -> HolderSet.direct(List.of()));
        }
        if (kind != HOLDER_ITEMS) {
            throw new DecoderException("Unknown compact holder-set kind: " + kind);
        }
        int count = buffer.readVarInt();
        if (count < 0 || count > CompactRecipeWireLimits.MAX_IO_PER_RELATION) {
            throw new DecoderException(
                    "Compact ingredient item list count " + count + " exceeds "
                            + CompactRecipeWireLimits.MAX_IO_PER_RELATION);
        }
        List<Holder<Item>> holders = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            holders.add(item(readItemId(buffer)).builtInRegistryHolder());
        }
        return HolderSet.direct(holders);
    }

    private static void encodePredicate(
            RegistryFriendlyByteBuf buffer, DataComponentPredicate predicate) {
        encodePatch(buffer, predicate.asPatch());
    }

    private static DataComponentPredicate decodePredicate(RegistryFriendlyByteBuf buffer) {
        DataComponentPatch patch = decodePatch(buffer);
        DataComponentPredicate.Builder builder = DataComponentPredicate.builder();
        patch.entrySet().forEach(entry -> {
            if (entry.getValue().isPresent()) {
                expect(builder, entry.getKey(), entry.getValue().orElseThrow());
            }
        });
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private static <T> void expect(
            DataComponentPredicate.Builder builder,
            DataComponentType<T> type,
            Object value) {
        builder.expect(type, (T) value);
    }

    private static void encodePatch(
            RegistryFriendlyByteBuf buffer, DataComponentPatch patch) {
        List<PatchEntry> entries = new ArrayList<>();
        patch.entrySet().forEach(entry -> {
            if (entry.getValue().isEmpty()) {
                throw new EncoderException(
                        "Compact wire does not encode removed components");
            }
            entries.add(new PatchEntry(entry.getKey(), entry.getValue().orElseThrow()));
        });
        if (entries.size() > CompactRecipeWireLimits.MAX_IO_PER_RELATION) {
            throw new EncoderException("Compact component patch is too large");
        }
        buffer.writeVarInt(entries.size());
        for (PatchEntry entry : entries) {
            ResourceLocation typeId = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(entry.type());
            if (typeId == null) {
                throw new EncoderException("Unregistered data component type");
            }
            int known = knownTypeCode(typeId);
            buffer.writeByte(known);
            if (known == TYPE_NAMED) {
                ResourceLocation.STREAM_CODEC.encode(buffer, typeId);
            }
            encodeComponentValue(buffer, entry.value());
        }
    }

    private static int knownTypeCode(ResourceLocation typeId) {
        if (BLOCK_STATE_ID.equals(typeId)) {
            return TYPE_BLOCK_STATE;
        }
        if (ORE_MATERIAL_ID.equals(typeId)) {
            return TYPE_ORE_MATERIAL;
        }
        if (PREFIX_MATERIAL_ID.equals(typeId)) {
            return TYPE_PREFIX_MATERIAL;
        }
        if (TOOL_MATERIAL_ID.equals(typeId)) {
            return TYPE_TOOL_MATERIAL;
        }
        if (MACHINE_MATERIAL_ID.equals(typeId)) {
            return TYPE_MACHINE_MATERIAL;
        }
        if (CIRCUIT_CONFIG_ID.equals(typeId)) {
            return TYPE_CIRCUIT_CONFIG;
        }
        if (FIREPROOF_ID.equals(typeId)) {
            return TYPE_FIREPROOF;
        }
        return TYPE_NAMED;
    }

    private static ResourceLocation knownTypeId(int code) {
        return switch (code) {
            case TYPE_BLOCK_STATE -> BLOCK_STATE_ID;
            case TYPE_ORE_MATERIAL -> ORE_MATERIAL_ID;
            case TYPE_PREFIX_MATERIAL -> PREFIX_MATERIAL_ID;
            case TYPE_TOOL_MATERIAL -> TOOL_MATERIAL_ID;
            case TYPE_MACHINE_MATERIAL -> MACHINE_MATERIAL_ID;
            case TYPE_CIRCUIT_CONFIG -> CIRCUIT_CONFIG_ID;
            case TYPE_FIREPROOF -> FIREPROOF_ID;
            default -> throw new DecoderException(
                    "Unknown compact component type code " + code);
        };
    }

    private static DataComponentPatch decodePatch(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > CompactRecipeWireLimits.MAX_IO_PER_RELATION) {
            throw new DecoderException("Compact component patch count " + count);
        }
        DataComponentPatch.Builder builder = DataComponentPatch.builder();
        for (int index = 0; index < count; index++) {
            int known = buffer.readUnsignedByte();
            ResourceLocation typeId = known == TYPE_NAMED
                    ? ResourceLocation.STREAM_CODEC.decode(buffer)
                    : knownTypeId(known);
            DataComponentType<?> type = BuiltInRegistries.DATA_COMPONENT_TYPE.get(typeId);
            if (type == null) {
                throw new DecoderException("Unknown data component type " + typeId);
            }
            applyComponent(builder, type, decodeComponentValue(buffer));
        }
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private static <T> void applyComponent(
            DataComponentPatch.Builder builder,
            DataComponentType<T> type,
            Object value) {
        builder.set(type, (T) value);
    }

    private static void encodeComponentValue(RegistryFriendlyByteBuf buffer, Object value) {
        if (value instanceof Integer integer) {
            buffer.writeByte(COMPONENT_INT);
            buffer.writeVarInt(integer);
            return;
        }
        if (value instanceof String string) {
            buffer.writeByte(COMPONENT_STRING);
            buffer.writeUtf(string, CompactRecipeWireLimits.MAX_SELECTED_SOURCE_LENGTH);
            return;
        }
        if (value instanceof Component component) {
            buffer.writeByte(COMPONENT_TEXT);
            buffer.writeUtf(
                    component.getString(),
                    CompactRecipeWireLimits.MAX_SELECTED_SOURCE_LENGTH);
            return;
        }
        if (value instanceof BlockItemStateProperties state) {
            buffer.writeByte(COMPONENT_BLOCK_STATE);
            encodeBlockState(buffer, state);
            return;
        }
        throw new EncoderException(
                "Unsupported compact component value " + value.getClass().getName());
    }

    private static void encodeBlockState(
            RegistryFriendlyByteBuf buffer, BlockItemStateProperties state) {
        List<Map.Entry<String, String>> entries =
                new ArrayList<>(state.properties().entrySet());
        entries.sort(Map.Entry.comparingByKey());
        if (entries.size() > CompactRecipeWireLimits.MAX_BLOCK_STATE_PROPERTIES) {
            throw new EncoderException(
                    "Compact block state property count "
                            + entries.size()
                            + " exceeds "
                            + CompactRecipeWireLimits.MAX_BLOCK_STATE_PROPERTIES);
        }
        buffer.writeVarInt(entries.size());
        for (Map.Entry<String, String> entry : entries) {
            if (entry.getKey() == null
                    || entry.getKey().isEmpty()
                    || entry.getValue() == null) {
                throw new EncoderException("Compact block state property is blank");
            }
            buffer.writeUtf(
                    entry.getKey(), CompactRecipeWireLimits.MAX_SELECTED_SOURCE_LENGTH);
            buffer.writeUtf(
                    entry.getValue(), CompactRecipeWireLimits.MAX_SELECTED_SOURCE_LENGTH);
        }
    }

    private static BlockItemStateProperties decodeBlockState(
            RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > CompactRecipeWireLimits.MAX_BLOCK_STATE_PROPERTIES) {
            throw new DecoderException(
                    "Compact block state property count " + count + " exceeds "
                            + CompactRecipeWireLimits.MAX_BLOCK_STATE_PROPERTIES);
        }
        Map<String, String> properties = new LinkedHashMap<>();
        for (int index = 0; index < count; index++) {
            String key = buffer.readUtf(CompactRecipeWireLimits.MAX_SELECTED_SOURCE_LENGTH);
            String value = buffer.readUtf(CompactRecipeWireLimits.MAX_SELECTED_SOURCE_LENGTH);
            if (key.isEmpty()) {
                throw new DecoderException("Compact block state property name is blank");
            }
            if (properties.put(key, value) != null) {
                throw new DecoderException(
                        "Duplicate compact block state property " + key);
            }
        }
        return new BlockItemStateProperties(Map.copyOf(properties));
    }

    private static Object decodeComponentValue(RegistryFriendlyByteBuf buffer) {
        int kind = buffer.readUnsignedByte();
        return switch (kind) {
            case COMPONENT_INT -> buffer.readVarInt();
            case COMPONENT_TEXT -> Component.literal(buffer.readUtf(
                    CompactRecipeWireLimits.MAX_SELECTED_SOURCE_LENGTH));
            case COMPONENT_STRING -> buffer.readUtf(
                    CompactRecipeWireLimits.MAX_SELECTED_SOURCE_LENGTH);
            case COMPONENT_BLOCK_STATE -> decodeBlockState(buffer);
            default -> throw new DecoderException(
                    "Unknown compact component kind: " + kind);
        };
    }

    private static ResourceLocation itemId(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) {
            throw new EncoderException("Unregistered item on compact wire");
        }
        return id;
    }

    private static ResourceLocation readItemId(RegistryFriendlyByteBuf buffer) {
        return ResourceLocation.STREAM_CODEC.decode(buffer);
    }

    private static Item item(ResourceLocation id) {
        if (!BuiltInRegistries.ITEM.containsKey(id)) {
            throw new DecoderException("Unregistered compact item " + id);
        }
        return BuiltInRegistries.ITEM.get(id);
    }

    private static ResourceLocation fluidId(Fluid fluid) {
        ResourceLocation id = BuiltInRegistries.FLUID.getKey(fluid);
        if (id == null) {
            throw new EncoderException("Unregistered fluid on compact wire");
        }
        return id;
    }

    private static Fluid fluid(ResourceLocation id) {
        if (!BuiltInRegistries.FLUID.containsKey(id)) {
            throw new DecoderException("Unregistered compact fluid " + id);
        }
        return BuiltInRegistries.FLUID.get(id);
    }

    private static <T> JsonObject encodeJson(
            RegistryFriendlyByteBuf buffer,
            Codec<T> codec,
            T value) {
        var ops = RegistryOps.create(JsonOps.INSTANCE, buffer.registryAccess());
        return codec.encodeStart(ops, value).getOrThrow().getAsJsonObject();
    }

    private record PatchEntry(DataComponentType<?> type, Object value) {}
}
