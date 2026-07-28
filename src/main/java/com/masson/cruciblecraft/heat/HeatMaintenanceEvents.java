package com.masson.cruciblecraft.heat;

import com.masson.cruciblecraft.CrucibleCraft;

import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = CrucibleCraft.MODID)
public final class HeatMaintenanceEvents {
    private HeatMaintenanceEvents() {}

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post event) {
        var player = event.getEntity();
        if (player.level().isClientSide || player.level().getGameTime() % 20L != player.getId() % 20L) {
            return;
        }
        long gameTime = player.level().getGameTime();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemHeat.clearIfCooled(player.getInventory().getItem(slot), gameTime);
        }
    }

    @SubscribeEvent
    public static void itemEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ItemEntity itemEntity)
                || itemEntity.level().isClientSide
                || itemEntity.level().getGameTime() % 20L != itemEntity.getId() % 20L) {
            return;
        }
        var stack = itemEntity.getItem();
        if (ItemHeat.clearIfCooled(stack, itemEntity.level().getGameTime())) {
            itemEntity.setItem(stack);
        }
    }
}
