package com.masson.cruciblecraft.content.item;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.masson.cruciblecraft.api.tool.RemoteActivatable;
import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * GT6 Remote Activator: up to 64 targets per dimension and a Chebyshev range
 * of 128 blocks when the button is pressed.
 */
public final class RemoteActivatorItem extends Item {
    private static final int MAX_TARGETS_PER_DIMENSION = 64;
    private static final int RANGE = 128;

    public RemoteActivatorItem(Properties properties) {
        super(properties);
    }

    /**
     * GT6 hand-drill placement links newly embedded explosives to a remote
     * activator carried in the player's hotbar.
     */
    public static void addTargetFromPlacement(
            Player player,
            Level level,
            BlockPos pos) {
        String target = encode(level, pos);
        for (int index = 0; index < 9; index++) {
            ItemStack stack = player.getInventory().getItem(index);
            if (!(stack.getItem() instanceof RemoteActivatorItem)) {
                continue;
            }
            Set<String> targets = targets(stack);
            if (targets.contains(target)) {
                return;
            }
            if (!canAddForDimension(targets, level)) {
                continue;
            }
            targets.add(target);
            saveTargets(stack, targets);
            return;
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown() || !player.mayBuild()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = context.getItemInHand();
        Set<String> targets = targets(stack);
        String target = encode(level, context.getClickedPos());
        boolean removed = targets.remove(target);
        if (!removed) {
            if (!canAddForDimension(targets, level)) {
                message(player, "message.cruciblecraft.remote.full");
                return InteractionResult.CONSUME;
            }
            if (!(level.getBlockEntity(context.getClickedPos())
                    instanceof RemoteActivatable)) {
                message(player, "message.cruciblecraft.remote.invalid");
                return InteractionResult.CONSUME;
            }
            targets.add(target);
            message(player, "message.cruciblecraft.remote.added");
        } else {
            message(player, "message.cruciblecraft.remote.removed");
        }
        saveTargets(stack, targets);
        level.playSound(
                null,
                context.getClickedPos(),
                SoundEvents.NOTE_BLOCK_HAT.value(),
                SoundSource.PLAYERS,
                0.5F,
                removed ? 0.7F : 1.0F);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() || level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        Set<String> kept = new LinkedHashSet<>();
        String dimension = level.dimension().location().toString();
        for (String encoded : targets(stack)) {
            Target target = decode(encoded);
            if (target == null || !dimension.equals(target.dimension())) {
                kept.add(encoded);
                continue;
            }
            BlockPos targetPos = target.pos();
            if (Math.abs(targetPos.getX() - player.getBlockX()) > RANGE
                    || Math.abs(targetPos.getY() - player.getBlockY()) > RANGE
                    || Math.abs(targetPos.getZ() - player.getBlockZ()) > RANGE) {
                kept.add(encoded);
                continue;
            }
            if (level.getBlockEntity(targetPos) instanceof RemoteActivatable activatable
                    && activatable.remoteActivate()) {
                kept.add(encoded);
            }
        }
        saveTargets(stack, kept);
        level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.NOTE_BLOCK_HAT.value(),
                SoundSource.PLAYERS,
                0.5F,
                0.8F);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable(
                "tooltip.cruciblecraft.remote_activator"));
        tooltip.add(Component.translatable(
                "tooltip.cruciblecraft.remote_activator.count",
                targets(stack).size()));
    }

    private static boolean canAddForDimension(
            Set<String> targets,
            Level level) {
        String dimension = level.dimension().location().toString();
        long count = targets.stream()
                .map(RemoteActivatorItem::decode)
                .filter(target -> target != null)
                .filter(target -> dimension.equals(target.dimension()))
                .count();
        return count < MAX_TARGETS_PER_DIMENSION;
    }

    private static Set<String> targets(ItemStack stack) {
        String serialized = stack.getOrDefault(
                ModComponents.DYNAMITE_REMOTE_TARGETS.get(),
                "");
        Set<String> targets = new LinkedHashSet<>();
        if (serialized.isEmpty()) {
            return targets;
        }
        for (String target : serialized.split(";", -1)) {
            if (!target.isEmpty() && decode(target) != null) {
                targets.add(target);
            }
        }
        return targets;
    }

    private static void saveTargets(ItemStack stack, Set<String> targets) {
        if (targets.isEmpty()) {
            stack.remove(ModComponents.DYNAMITE_REMOTE_TARGETS.get());
            return;
        }
        stack.set(
                ModComponents.DYNAMITE_REMOTE_TARGETS.get(),
                String.join(";", targets));
    }

    private static String encode(Level level, BlockPos pos) {
        return level.dimension().location()
                + "|" + pos.getX()
                + "|" + pos.getY()
                + "|" + pos.getZ();
    }

    private static Target decode(String encoded) {
        String[] parts = encoded.split("\\|", -1);
        if (parts.length != 4) {
            return null;
        }
        try {
            return new Target(
                    parts[0],
                    new BlockPos(
                            Integer.parseInt(parts[1]),
                            Integer.parseInt(parts[2]),
                            Integer.parseInt(parts[3])));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static void message(Player player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }

    private record Target(String dimension, BlockPos pos) {}
}
