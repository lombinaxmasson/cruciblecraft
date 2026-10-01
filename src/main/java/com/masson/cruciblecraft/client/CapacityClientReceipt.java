package com.masson.cruciblecraft.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.GsonBuilder;
import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.recipe.gt.CompactFamilyOnDemand;
import com.masson.cruciblecraft.recipe.gt.CompactGTRecipeFamilyEntry;
import com.masson.cruciblecraft.registry.ModProcessingMachines;

import io.netty.buffer.Unpooled;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.connection.ConnectionType;

/**
 * Dedicated-client receipt for the capacity measurement. Writes the encoded
 * login packet and the on-demand family count, then exits.
 */
@EventBusSubscriber(modid = CrucibleCraft.MODID, value = Dist.CLIENT)
public final class CapacityClientReceipt {
    private static final int TIMEOUT_TICKS = 20 * 180;
    private static boolean inWorld;
    private static final String SERVER = "127.0.0.1:25567";
    private static int ticksWaiting;
    private static boolean wrote;
    private static boolean connectStarted;

    private CapacityClientReceipt() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        String path = System.getProperty("cruciblecraft.capacityClientReceipt");
        if (path == null || path.isBlank() || wrote) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            if (minecraft.screen instanceof AccessibilityOnboardingScreen) {
                minecraft.options.onboardAccessibility = false;
                minecraft.options.save();
                minecraft.setScreen(new TitleScreen());
                return;
            }
            if (!connectStarted
                    && minecraft.screen instanceof TitleScreen
                    && minecraft.getConnection() == null) {
                connectStarted = true;
                ServerData data = new ServerData("capacity", SERVER, ServerData.Type.OTHER);
                ConnectScreen.startConnecting(
                        minecraft.screen,
                        minecraft,
                        ServerAddress.parseString(SERVER),
                        data,
                        false,
                        null);
            }
            if (connectStarted) {
                ticksWaiting++;
            }
            if (!connectStarted || ticksWaiting < TIMEOUT_TICKS) {
                return;
            }
            wrote = true;
            writeReceipt(path, 0, 0, 0, 0, false, false);
            if (Boolean.getBoolean("cruciblecraft.capacityClientExit")) {
                System.exit(2);
            }
            return;
        }
        if (!inWorld) {
            inWorld = true;
            ticksWaiting = 0;
        }
        ticksWaiting++;
        boolean settled = CompactFamilyOnDemand.prefetchSettled();
        if (!settled && ticksWaiting < TIMEOUT_TICKS) {
            return;
        }
        wrote = true;
        boolean ok = false;
        try {
            long loginBytes = encodeLogin(minecraft);
            int holders = minecraft.level.getRecipeManager().getRecipes().size();
            int families = CompactFamilyOnDemand.entries().size();
            int logicalRows = 0;
            for (CompactGTRecipeFamilyEntry entry
                    : CompactFamilyOnDemand.entries().values()) {
                logicalRows += entry.definition().matrix()
                        .map(matrix -> matrix.rows().size())
                        .orElseGet(() -> entry.definition().relations().size());
            }
            ok = settled
                    && families > 0
                    && loginBytes > 0
                    && loginBytes <= ModProcessingMachines.RECIPE_SYNC_BUDGET_BYTES;
            writeReceipt(path, loginBytes, holders, families, logicalRows, settled, ok);
        } catch (RuntimeException failure) {
            CrucibleCraft.LOGGER.error("Capacity client receipt failed", failure);
            ok = false;
        }
        if (Boolean.getBoolean("cruciblecraft.capacityClientExit")) {
            if (!ok) {
                System.exit(2);
            }
            minecraft.stop();
        }
    }

    private static void writeReceipt(
            String path,
            long loginBytes,
            int holders,
            int families,
            int logicalRows,
            boolean settled,
            boolean ok) {
        try {
            Map<String, Object> document = new LinkedHashMap<>();
            document.put("schema", 1);
            document.put("login_packet_bytes", loginBytes);
            document.put("recipe_holders", holders);
            document.put("ondemand_families", families);
            document.put("ondemand_logical_rows", logicalRows);
            document.put("prefetch_settled", settled);
            document.put("within_sync_budget",
                    loginBytes > 0 && loginBytes <= ModProcessingMachines.RECIPE_SYNC_BUDGET_BYTES);
            document.put("ok", ok);
            Path output = Path.of(path);
            Files.createDirectories(output.getParent());
            Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(document));
            CrucibleCraft.LOGGER.info(
                    "Capacity client receipt: loginBytes={} families={} logicalRows={} ok={} path={}",
                    loginBytes,
                    families,
                    logicalRows,
                    ok,
                    output);
        } catch (java.io.IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static long encodeLogin(Minecraft minecraft) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                Unpooled.buffer(),
                minecraft.level.registryAccess(),
                ConnectionType.NEOFORGE);
        try {
            ClientboundUpdateRecipesPacket packet = new ClientboundUpdateRecipesPacket(
                    minecraft.level.getRecipeManager().getRecipes());
            ClientboundUpdateRecipesPacket.STREAM_CODEC.encode(buffer, packet);
            return buffer.readableBytes();
        } finally {
            buffer.release();
        }
    }
}
