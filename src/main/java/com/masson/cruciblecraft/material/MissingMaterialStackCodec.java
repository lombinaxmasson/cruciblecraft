package com.masson.cruciblecraft.material;

import java.util.Objects;

import com.google.gson.JsonObject;
import com.masson.cruciblecraft.registry.ModItems;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Adds legacy material-stack migration to NBT ItemStack decoding only. */
public final class MissingMaterialStackCodec {
    private static final String SELF_TEST_ID =
            "cruciblecraft:__codec_migration_self_test_ingot";

    private MissingMaterialStackCodec() {}

    public static Codec<ItemStack> wrap(Codec<ItemStack> delegate) {
        Objects.requireNonNull(delegate, "delegate");
        return Codec.of(delegate, new com.mojang.serialization.Decoder<ItemStack>() {
            @Override
            public <T> DataResult<Pair<ItemStack, T>> decode(
                    DynamicOps<T> ops,
                    T input) {
                return delegate.decode(ops, rewrite(ops, input));
            }
        });
    }

    public static void verifyInstalled() {
        verifyCodec("CODEC", ItemStack.CODEC);
        verifyCodec("SINGLE_ITEM_CODEC", ItemStack.SINGLE_ITEM_CODEC);
    }

    @SuppressWarnings("unchecked")
    static <T> T rewrite(DynamicOps<T> ops, T input) {
        if (input instanceof JsonObject) {
            return input;
        }
        if (!MaterialCatalog.isBootstrapped()) {
            return input;
        }
        if (input instanceof CompoundTag tag) {
            return (T) MissingMaterialStackNbtAdapter.rewrite(tag);
        }
        return input;
    }

    static boolean itemExists(String itemId) {
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        return id != null && BuiltInRegistries.ITEM.containsKey(id);
    }

    private static void verifyCodec(String name, Codec<ItemStack> codec) {
        CompoundTag legacyStack = new CompoundTag();
        legacyStack.putString("id", SELF_TEST_ID);
        ItemStack decoded = codec.parse(NbtOps.INSTANCE, legacyStack)
                .getOrThrow(message -> new IllegalStateException(
                        "ItemStack " + name + " migration self-test failed: " + message));
        if (!decoded.is(ModItems.UNKNOWN_MATERIAL.get())) {
            throw new IllegalStateException(
                    "ItemStack " + name + " is not wrapped by CrucibleCraft migration");
        }
    }
}
