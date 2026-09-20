package com.masson.cruciblecraft.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;

import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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
                "removed recipe JSON must fail delegate decoding instead of becoming unknown");
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

    @Test
    void missingToolMaterialComponentMigratesEvenWhenTheToolItemStillExists() {
        CompoundTag stack = new CompoundTag();
        stack.putString("id", "cruciblecraft:smithing_hammer");
        CompoundTag components = new CompoundTag();
        components.putString(
                MissingMaterialStackNbtAdapter.TOOL_MATERIAL_COMPONENT_ID,
                "removed_alloy");
        stack.put("components", components);

        CompoundTag migrated = MissingMaterialStackNbtAdapter.rewrite(
                stack,
                Map.of(),
                ignored -> true,
                material -> material.equals("iron"));

        assertNotSame(stack, migrated);
        assertEquals(
                MissingMaterialStackRewriter.UNKNOWN_ITEM_ID,
                migrated.getString("id"));
        CompoundTag missing = migrated
                .getCompound("components")
                .getCompound("cruciblecraft:missing_material");
        assertEquals("removed_alloy", missing.getString("material_id"));
        assertEquals(
                MissingMaterialStackNbtAdapter.TOOL_COMPONENT_FORM,
                missing.getString("form"));
        assertEquals(
                "cruciblecraft:smithing_hammer",
                missing.getString("original_item_id"));
        assertEquals("cruciblecraft:smithing_hammer", stack.getString("id"));
    }

    @Test
    void componentQuarantineUsesTheConsumingItemsDeviceRules() {
        CompoundTag hammer = componentStack(
                "cruciblecraft:smithing_hammer",
                MissingMaterialStackNbtAdapter.TOOL_MATERIAL_COMPONENT_ID,
                "gold");
        CompoundTag anvil = componentStack(
                "cruciblecraft:anvil",
                MissingMaterialStackNbtAdapter.MACHINE_MATERIAL_COMPONENT_ID,
                "gold");
        CompoundTag futureGenericTool = componentStack(
                "cruciblecraft:pickaxe",
                MissingMaterialStackNbtAdapter.TOOL_MATERIAL_COMPONENT_ID,
                "gold");

        assertEquals(
                MissingMaterialStackRewriter.UNKNOWN_ITEM_ID,
                MissingMaterialStackNbtAdapter.rewrite(
                        hammer, Map.of(), ignored -> true, ignored -> true)
                        .getString("id"));
        assertEquals(
                MissingMaterialStackRewriter.UNKNOWN_ITEM_ID,
                MissingMaterialStackNbtAdapter.rewrite(
                        anvil, Map.of(), ignored -> true, ignored -> true)
                        .getString("id"));
        assertEquals(
                MissingMaterialStackRewriter.UNKNOWN_ITEM_ID,
                MissingMaterialStackNbtAdapter.rewrite(
                        futureGenericTool,
                        Map.of(),
                        ignored -> true,
                        ignored -> true)
                        .getString("id"));
    }

    @Test
    void leftoverPrefixItemRewritesOntoUniqueHostedSlashId() {
        CompoundTag stack = new CompoundTag();
        stack.putString("id", "cruciblecraft:storage_plate");
        CompoundTag components = new CompoundTag();
        components.putString(
                MissingMaterialStackNbtAdapter.PREFIX_MATERIAL_COMPONENT_ID,
                "iron");
        stack.put("components", components);

        CompoundTag migrated = MissingMaterialStackNbtAdapter.rewrite(
                stack,
                Map.of("iron/storage_plate", "cruciblecraft:iron/storage_plate"),
                ignored -> false);

        assertEquals("cruciblecraft:iron/storage_plate", migrated.getString("id"));
        assertFalse(migrated.contains("components", Tag.TAG_COMPOUND));
        assertEquals("cruciblecraft:storage_plate", stack.getString("id"));
    }

    @Test
    void persistedMaterialIdsRejectInvalidSyntax() {
        assertTrue(MaterialId.CODEC
                .parse(JsonOps.INSTANCE, new JsonPrimitive("stainless_steel"))
                .result()
                .isPresent());
        assertFalse(MaterialId.CODEC
                .parse(JsonOps.INSTANCE, new JsonPrimitive("CrucibleCraft:Iron"))
                .error()
                .isEmpty());
    }

    private static CompoundTag componentStack(
            String itemId,
            String componentId,
            String materialId) {
        CompoundTag stack = new CompoundTag();
        stack.putString("id", itemId);
        CompoundTag components = new CompoundTag();
        components.putString(componentId, materialId);
        stack.put("components", components);
        return stack;
    }
}
