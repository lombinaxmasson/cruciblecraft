package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.CrucibleCraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/** 3×3 sense/plow harvest and gem-pick / scoop silk-touch drops. */
@EventBusSubscriber(modid = CrucibleCraft.MODID)
public final class ToolHarvestEvents {
    private static final ThreadLocal<Boolean> AREA_HARVESTING =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    private ToolHarvestEvents() {}

    @SubscribeEvent
    public static void areaHarvest(BlockEvent.BreakEvent event) {
        if (Boolean.TRUE.equals(AREA_HARVESTING.get())) {
            return;
        }
        Player player = event.getPlayer();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        boolean sense = stack.getItem() instanceof MaterialSenseItem;
        boolean plow = stack.getItem() instanceof MaterialPlowItem;
        if (!sense && !plow) {
            return;
        }
        AREA_HARVESTING.set(Boolean.TRUE);
        try {
            BlockPos origin = event.getPos();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        BlockPos neighbor = origin.offset(dx, dy, dz);
                        BlockState state = event.getLevel().getBlockState(neighbor);
                        if (stack.getDestroySpeed(state) > 1.0F
                                || stack.isCorrectToolForDrops(state)) {
                            serverPlayer.gameMode.destroyBlock(neighbor);
                        }
                    }
                }
            }
        } finally {
            AREA_HARVESTING.set(Boolean.FALSE);
        }
    }

    @SubscribeEvent
    public static void silkDrops(BlockDropsEvent event) {
        ItemStack tool = event.getTool();
        BlockState state = event.getState();
        boolean gem = tool.getItem() instanceof MaterialGemPickItem;
        boolean scoop = tool.getItem() instanceof MaterialScoopItem
                && MaterialScoopItem.harvestable(state);
        if (!gem && !scoop) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel server)) {
            return;
        }
        ItemStack silk = tool.copy();
        var silkTouch = server.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.SILK_TOUCH);
        silk.enchant(silkTouch, 1);
        var drops = Block.getDrops(
                state,
                server,
                event.getPos(),
                event.getBlockEntity(),
                event.getBreaker(),
                silk);
        event.getDrops().clear();
        double x = event.getPos().getX() + 0.5;
        double y = event.getPos().getY() + 0.5;
        double z = event.getPos().getZ() + 0.5;
        for (ItemStack drop : drops) {
            if (!drop.isEmpty()) {
                event.getDrops().add(new ItemEntity(server, x, y, z, drop));
            }
        }
        event.setDroppedExperience(0);
    }
}
