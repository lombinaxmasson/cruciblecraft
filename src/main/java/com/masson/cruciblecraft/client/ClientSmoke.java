package com.masson.cruciblecraft.client;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.verification.PlayerCompleteSmoke;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Writes a live runClient receipt and optionally exits. */
@EventBusSubscriber(modid = CrucibleCraft.MODID, value = Dist.CLIENT)
public final class ClientSmoke {
    private static boolean ran;

    private ClientSmoke() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (ran) {
            return;
        }
        if (!"player-complete".equals(
                System.getProperty("cruciblecraft.clientSmoke"))) {
            return;
        }
        ran = true;
        PlayerCompleteSmoke.writeIfConfigured("client");
        if (!Boolean.getBoolean("cruciblecraft.clientSmokeExit")) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            minecraft.stop();
        }
    }
}
