package com.masson.cruciblecraft.content.redstonewire;

import com.masson.cruciblecraft.CrucibleCraft;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Vanilla torch/repeater {@code BlockItem}s place a block before the wire's
 * {@code useItemOn}. Cancel that so GT6 can attach the cover instead.
 */
@EventBusSubscriber(modid = CrucibleCraft.MODID)
public final class RedstoneWireCoverEvents {
    private RedstoneWireCoverEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void placeVanillaCover(
            PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()
                || (!stack.is(Items.REDSTONE_TORCH) && !stack.is(Items.REPEATER))
                || event.getFace() == null) {
            return;
        }
        if (!RedstoneWireCovers.tryInstall(
                event.getLevel(),
                event.getPos(),
                event.getFace(),
                stack,
                event.getEntity())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(
                InteractionResult.sidedSuccess(event.getLevel().isClientSide));
    }
}
