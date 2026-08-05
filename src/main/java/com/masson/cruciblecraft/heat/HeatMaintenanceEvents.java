package com.masson.cruciblecraft.heat;

import com.masson.cruciblecraft.CrucibleCraft;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
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
        double maximumDamage = 0.0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            var stack = player.getInventory().getItem(slot);
            ItemStack maintained = maintain(stack, gameTime);
            if (maintained != stack) {
                player.getInventory().setItem(slot, maintained);
                stack = maintained;
            }
            maximumDamage = Math.max(
                    maximumDamage,
                    MaterialContactHeat.damage(stack));
        }
        if (maximumDamage > 0.0) {
            player.hurt(
                    player.damageSources().hotFloor(),
                    (float) Math.min(maximumDamage, Float.MAX_VALUE));
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
        ItemStack maintained = maintain(
                stack,
                itemEntity.level().getGameTime());
        if (maintained != stack) {
            itemEntity.setItem(maintained);
        }
    }

    static ItemStack maintain(ItemStack stack, long gameTime) {
        HotIngotProcessing.initializeIfMissing(stack, gameTime);
        var cooled = MaterialItemCooling.coolIfReady(stack, gameTime);
        if (cooled.isPresent()) {
            return cooled.orElseThrow();
        }
        ItemHeat.clearIfCooled(stack, gameTime);
        return stack;
    }
}
