package com.masson.cruciblecraft.logistics.pipe.cover;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.level.Level;

/** GT6 {@code CoverCrafting}: right-click opens a vanilla workbench. */
public final class CoverCrafting {
    private CoverCrafting() {}

    public static boolean isCrafting(PipeCover cover) {
        return cover != null
                && "cover_crafting".equals(cover.definitionId().getPath());
    }

    public static boolean open(
            Player player, Level level, BlockPos pos, PipeCover cover) {
        if (!isCrafting(cover) || player == null || level == null || pos == null) {
            return false;
        }
        if (player instanceof ServerPlayer server) {
            server.openMenu(new SimpleMenuProvider(
                    (id, inventory, opener) -> new CraftingMenu(
                            id,
                            inventory,
                            ContainerLevelAccess.create(level, pos)) {
                        @Override
                        public boolean stillValid(Player still) {
                            return true;
                        }
                    },
                    Component.translatable("container.crafting")));
        }
        return true;
    }
}
