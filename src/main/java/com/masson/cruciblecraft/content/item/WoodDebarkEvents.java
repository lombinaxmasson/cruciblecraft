package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.block.WoodDebark;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/** GT6 axe bark byproduct when a log is stripped to a beam. */
@EventBusSubscriber(modid = CrucibleCraft.MODID)
public final class WoodDebarkEvents {
    private WoodDebarkEvents() {}

    @SubscribeEvent
    public static void giveBarkOnStrip(
            BlockEvent.BlockToolModificationEvent event) {
        if (event.isSimulated()
                || event.getItemAbility() != ItemAbilities.AXE_STRIP) {
            return;
        }
        Level level = event.getContext().getLevel();
        if (level.isClientSide) {
            return;
        }
        BlockState original = event.getState();
        if (!WoodDebark.isDebarkSource(original)) {
            return;
        }
        BlockState stripped = original.getBlock().getToolModifiedState(
                original,
                event.getContext(),
                ItemAbilities.AXE_STRIP,
                true);
        if (stripped == null || stripped == original) {
            return;
        }
        ItemStack bark = WoodDebark.axeBarkDrop(original);
        if (bark.isEmpty()) {
            return;
        }
        Player player = event.getPlayer();
        if (player != null) {
            ItemHandlerHelper.giveItemToPlayer(player, bark);
            return;
        }
        Block.popResource(level, event.getPos(), bark);
    }
}
