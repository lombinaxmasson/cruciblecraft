package com.masson.cruciblecraft.recipe.gt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.network.CompactFamilyRequestPayload;
import com.masson.cruciblecraft.network.CompactFamilySlicePayload;
import com.masson.cruciblecraft.registry.ModRecipes;

import io.netty.buffer.Unpooled;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.connection.ConnectionType;

/**
 * Serves compact family holders after login, one target map at a time.
 * The login packet measured by the sync budget does not include these bodies.
 */
public final class CompactFamilyOnDemand {
    public static final int MAX_SLICE_BODY_BYTES = 700_000;
    public static final int MAX_SLICES = 8_192;
    private static final Map<ResourceLocation, CompactGTRecipeFamilyEntry> ENTRIES =
            new HashMap<>();
    private static final Set<ResourceLocation> REQUESTED = new HashSet<>();
    private static final Set<ResourceLocation> LOADED = new HashSet<>();
    private static final Map<ResourceLocation, byte[][]> PENDING = new HashMap<>();
    private static final Set<ResourceLocation> PREFETCH_MAPS = new HashSet<>();
    private static final int PREFETCH_IDLE = 0;
    private static final int PREFETCH_RUNNING = 1;
    private static final int PREFETCH_SETTLED = 2;
    private static int prefetchState = PREFETCH_IDLE;
    private static boolean prefetchDirty;

    private CompactFamilyOnDemand() {}

    public static Map<ResourceLocation, CompactGTRecipeFamilyEntry> entries() {
        return ENTRIES;
    }

    public static void clear() {
        ENTRIES.clear();
        REQUESTED.clear();
        LOADED.clear();
        PENDING.clear();
        PREFETCH_MAPS.clear();
        prefetchDirty = false;
        prefetchState = PREFETCH_IDLE;
    }

    public static boolean prefetchSettled() {
        return prefetchState == PREFETCH_SETTLED;
    }

    /**
     * Dedicated clients ask for every map after login so EMI still sees the
     * live compact families. The login packet itself stays filtered.
     */
    public static void requestAll(Iterable<RecipeMap> maps) {
        if (prefetchState == PREFETCH_RUNNING) {
            return;
        }
        PREFETCH_MAPS.clear();
        prefetchDirty = false;
        List<ResourceLocation> sending = new ArrayList<>();
        for (RecipeMap map : maps) {
            ResourceLocation id = map.id();
            if (beginRequest(id)) {
                PREFETCH_MAPS.add(id);
                sending.add(id);
            }
        }
        if (PREFETCH_MAPS.isEmpty()) {
            prefetchState = PREFETCH_SETTLED;
            return;
        }
        prefetchState = PREFETCH_RUNNING;
        for (ResourceLocation id : sending) {
            PacketDistributor.sendToServer(new CompactFamilyRequestPayload(id));
        }
    }

    public static boolean beginRequest(ResourceLocation targetMap) {
        if (LOADED.contains(targetMap) || REQUESTED.contains(targetMap)) {
            return false;
        }
        REQUESTED.add(targetMap);
        return true;
    }

    public static void send(ServerPlayer player, ResourceLocation targetMap) {
        RecipeManager manager = player.server.getRecipeManager();
        RegistryAccess registries = player.server.registryAccess();
        List<RecipeHolder<CompactGTRecipeFamilyEntry>> matched = new ArrayList<>();
        for (RecipeHolder<CompactGTRecipeFamilyEntry> holder
                : manager.getAllRecipesFor(
                        ModRecipes.COMPACT_GT_RECIPE_FAMILY_TYPE.get())) {
            if (targetMap.equals(holder.value().definition().targetMap())) {
                matched.add(holder);
            }
        }
        List<byte[]> slices = encode(matched, registries);
        if (slices.isEmpty()) {
            slices = List.of(emptyBody(registries));
        }
        if (slices.size() > MAX_SLICES) {
            CrucibleCraft.LOGGER.warn(
                    "Compact family on-demand for {} has {} slices, above {}",
                    targetMap,
                    slices.size(),
                    MAX_SLICES);
            return;
        }
        for (int index = 0; index < slices.size(); index++) {
            PacketDistributor.sendToPlayer(
                    player,
                    new CompactFamilySlicePayload(
                            targetMap, index, slices.size(), slices.get(index)));
        }
    }

    public static void acceptSlice(
            ResourceLocation targetMap,
            int index,
            int count,
            byte[] body,
            RegistryAccess registries,
            RecipeManager manager) {
        if (count <= 0 || count > MAX_SLICES || index < 0 || index >= count) {
            return;
        }
        if (body.length > MAX_SLICE_BODY_BYTES && count != 1) {
            return;
        }
        byte[][] slices = PENDING.computeIfAbsent(targetMap, ignored -> new byte[count][]);
        if (slices.length != count) {
            return;
        }
        slices[index] = body;
        for (byte[] slice : slices) {
            if (slice == null) {
                return;
            }
        }
        PENDING.remove(targetMap);
        int added = 0;
        for (byte[] slice : slices) {
            added += decode(slice, registries);
        }
        LOADED.add(targetMap);
        boolean tracked = PREFETCH_MAPS.remove(targetMap);
        if (added > 0) {
            prefetchDirty = true;
        }
        if (tracked) {
            if (!PREFETCH_MAPS.isEmpty()) {
                return;
            }
            prefetchState = PREFETCH_SETTLED;
            if (!prefetchDirty) {
                return;
            }
            reloadDedicatedClient(manager);
            return;
        }
        if (added <= 0) {
            return;
        }
        reloadDedicatedClient(manager);
    }

    private static void reloadDedicatedClient(RecipeManager manager) {
        GTRecipeReloadCoordinator.advanceGeneration(manager);
        GTRecipeMapLoader.reload(
                manager,
                ExtruderRecipeFamilyProvider.RuntimeSide.DEDICATED_CLIENT,
                GTRecipeReloadCoordinator.Cause.CLIENT_RECIPES_UPDATED);
    }

    private static List<byte[]> encode(
            Collection<RecipeHolder<CompactGTRecipeFamilyEntry>> holders,
            RegistryAccess registries) {
        List<byte[]> slices = new ArrayList<>();
        List<RecipeHolder<CompactGTRecipeFamilyEntry>> chunk = new ArrayList<>();
        int chunkBytes = 0;
        for (RecipeHolder<CompactGTRecipeFamilyEntry> holder : holders) {
            int holderBytes = encodedSize(holder, registries);
            if (!chunk.isEmpty()
                    && chunkBytes + holderBytes > MAX_SLICE_BODY_BYTES) {
                slices.add(encodeChunk(chunk, registries));
                chunk.clear();
                chunkBytes = 0;
            }
            chunk.add(holder);
            chunkBytes += holderBytes;
        }
        if (!chunk.isEmpty()) {
            slices.add(encodeChunk(chunk, registries));
        }
        return slices;
    }

    private static int encodedSize(
            RecipeHolder<CompactGTRecipeFamilyEntry> holder,
            RegistryAccess registries) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), registries, ConnectionType.NEOFORGE);
        try {
            writeHolder(buffer, holder);
            return buffer.readableBytes();
        } finally {
            buffer.release();
        }
    }

    private static byte[] encodeChunk(
            List<RecipeHolder<CompactGTRecipeFamilyEntry>> chunk,
            RegistryAccess registries) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), registries, ConnectionType.NEOFORGE);
        try {
            buffer.writeVarInt(chunk.size());
            for (RecipeHolder<CompactGTRecipeFamilyEntry> holder : chunk) {
                writeHolder(buffer, holder);
            }
            byte[] bytes = new byte[buffer.readableBytes()];
            buffer.readBytes(bytes);
            return bytes;
        } finally {
            buffer.release();
        }
    }

    private static byte[] emptyBody(RegistryAccess registries) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), registries, ConnectionType.NEOFORGE);
        try {
            buffer.writeVarInt(0);
            byte[] bytes = new byte[buffer.readableBytes()];
            buffer.readBytes(bytes);
            return bytes;
        } finally {
            buffer.release();
        }
    }

    private static void writeHolder(
            RegistryFriendlyByteBuf buffer,
            RecipeHolder<CompactGTRecipeFamilyEntry> holder) {
        buffer.writeResourceLocation(holder.id());
        ModRecipes.COMPACT_GT_RECIPE_FAMILY_SERIALIZER.get()
                .streamCodec()
                .encode(buffer, holder.value());
    }

    private static int decode(byte[] body, RegistryAccess registries) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                Unpooled.wrappedBuffer(body), registries, ConnectionType.NEOFORGE);
        try {
            int count = buffer.readVarInt();
            if (count < 0 || count > 10_000) {
                throw new IllegalArgumentException(
                        "Compact family slice holder count " + count);
            }
            int added = 0;
            for (int index = 0; index < count; index++) {
                ResourceLocation id = buffer.readResourceLocation();
                CompactGTRecipeFamilyEntry entry =
                        ModRecipes.COMPACT_GT_RECIPE_FAMILY_SERIALIZER.get()
                                .streamCodec()
                                .decode(buffer);
                if (ENTRIES.put(id, entry) == null) {
                    added++;
                }
            }
            return added;
        } finally {
            buffer.release();
        }
    }
}
