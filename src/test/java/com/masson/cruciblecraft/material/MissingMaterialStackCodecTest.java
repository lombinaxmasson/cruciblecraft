package com.masson.cruciblecraft.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;

import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.loading.LoadingModList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MissingMaterialStackCodecTest {
    private static DynamicOps<JsonElement> jsonOps;

    @BeforeAll
    static void bootstrap() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        RegistryAccess registries = new RegistryAccess.ImmutableRegistryAccess(
                BuiltInRegistries.REGISTRY.stream().toList());
        jsonOps = RegistryOps.create(JsonOps.INSTANCE, registries);
    }

    @Test
    void legacyJsonIsPassedUnchangedToStrictCodecAndFailsDecode() {
        JsonObject legacy = new JsonObject();
        legacy.addProperty("id", "cruciblecraft:removed_material_ingot");

        assertSame(legacy, MissingMaterialStackCodec.rewrite(jsonOps, legacy));
        assertTrue(
                MissingMaterialStackCodec.wrap(ItemStack.STRICT_CODEC)
                        .parse(jsonOps, legacy)
                        .error()
                        .isPresent(),
                "legacy recipe JSON must fail delegate decoding instead of becoming unknown");
    }

    @Test
    void canonicalJsonIsUnchangedOnTheStrictCodecPath() {
        JsonElement canonical = ItemStack.STRICT_CODEC
                .encodeStart(jsonOps, new ItemStack(Items.STONE))
                .getOrThrow();

        assertSame(canonical, MissingMaterialStackCodec.rewrite(jsonOps, canonical));
        ItemStack decoded = MissingMaterialStackCodec.wrap(ItemStack.STRICT_CODEC)
                .parse(jsonOps, canonical)
                .getOrThrow();
        assertTrue(decoded.is(Items.STONE));
    }

    @Test
    void equivalentLegacyCompoundTagStillMigratesToUnknown() {
        CompoundTag legacy = new CompoundTag();
        legacy.putString("id", "cruciblecraft:removed_material_ingot");

        CompoundTag migrated = MissingMaterialStackNbtAdapter.rewrite(
                legacy,
                Map.of(),
                ignored -> false);

        assertNotSame(legacy, migrated);
        assertEquals(
                "cruciblecraft:removed_material_ingot",
                legacy.getString("id"),
                "migration must not mutate the caller's tag");
        assertEquals(MissingMaterialStackRewriter.UNKNOWN_ITEM_ID, migrated.getString("id"));
        CompoundTag missing = migrated
                .getCompound("components")
                .getCompound("cruciblecraft:missing_material");
        assertEquals("removed_material", missing.getString("material_id"));
        assertEquals("ingot", missing.getString("form"));
    }
}
