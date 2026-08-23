package com.masson.cruciblecraft.content.item;

import java.util.List;

import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Stackable GT6 Selector Tag stand-in whose config is a data component. */
public final class ProgrammedCircuitItem extends Item {
    public static final int MIN_CONFIG = 1;
    public static final int MAX_CONFIG = 24;
    public static final int DEFAULT_CONFIG = 1;

    public ProgrammedCircuitItem(Properties properties) {
        super(properties.component(ModComponents.CIRCUIT_CONFIG.get(), DEFAULT_CONFIG));
    }

    public static int normalize(int config) {
        if (config < MIN_CONFIG) {
            return MIN_CONFIG;
        }
        if (config > MAX_CONFIG) {
            return MAX_CONFIG;
        }
        return config;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        int config = normalize(stack.getOrDefault(
                ModComponents.CIRCUIT_CONFIG.get(), DEFAULT_CONFIG));
        tooltip.add(Component.translatable(
                "tooltip.cruciblecraft.circuit_config",
                config).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (!level.isClientSide) {
            int current = normalize(stack.getOrDefault(
                    ModComponents.CIRCUIT_CONFIG.get(), DEFAULT_CONFIG));
            int next = current >= MAX_CONFIG ? MIN_CONFIG : current + 1;
            stack.set(ModComponents.CIRCUIT_CONFIG.get(), next);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
