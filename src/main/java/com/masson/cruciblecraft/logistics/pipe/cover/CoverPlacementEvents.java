package com.masson.cruciblecraft.logistics.pipe.cover;

import com.masson.cruciblecraft.CrucibleCraft;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Material plates have no {@code useOn}. Cancel the block GUI / default
 * interaction when GT6 would attach the live stack as a decorative cover.
 */
@EventBusSubscriber(modid = CrucibleCraft.MODID)
public final class CoverPlacementEvents {
    private CoverPlacementEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void placePlateCover(
            PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || event.getFace() == null) {
            return;
        }
        if (PlateCovers.fromItem(stack) == null
                && DecorativeCovers.fromItem(stack) == null) {
            return;
        }
        if (!CoverInstall.tryPlace(
                event.getLevel(),
                event.getPos(),
                event.getHitVec(),
                stack,
                event.getEntity())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(
                InteractionResult.sidedSuccess(event.getLevel().isClientSide));
    }
}
