package com.masson.cruciblecraft.material;

import com.masson.cruciblecraft.CrucibleCraft;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = CrucibleCraft.MODID)
public final class MaterialFingerprintEvents {
    private MaterialFingerprintEvents() {}

    @SubscribeEvent
    public static void serverStarted(ServerStartedEvent event) {
        data(event.getServer().overworld()).compareWithCurrent();
    }

    @SubscribeEvent
    public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MaterialFingerprintSavedData fingerprint = data(player.getServer().overworld());
            if (!fingerprint.changedOnLoad()) {
                return;
            }
            Component message = Component.translatable("message.cruciblecraft.materials_changed");
            if (!fingerprint.missingOnLoad().isEmpty()) {
                message = message.copy().append(Component.literal(
                        " [" + String.join(", ", fingerprint.missingOnLoad()) + "]"));
            }
            player.sendSystemMessage(message);
        }
    }

    private static MaterialFingerprintSavedData data(net.minecraft.server.level.ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                MaterialFingerprintSavedData.FACTORY,
                MaterialFingerprintSavedData.NAME);
    }
}
