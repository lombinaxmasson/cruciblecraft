package com.masson.cruciblecraft.content.item;

import java.util.List;

import com.masson.cruciblecraft.logistics.machinecover.MachineCoverBehaviors;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverHost;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCover;
import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionResult;

/** Stackable GT6 selector circuit whose mode is a data component. */
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
    public InteractionResult useOn(UseOnContext context) {
        var blockEntity = context.getLevel().getBlockEntity(
                context.getClickedPos());
        if (!(blockEntity instanceof MachineCoverHost machine)) {
            return InteractionResult.PASS;
        }
        int config = normalize(context.getItemInHand().getOrDefault(
                ModComponents.CIRCUIT_CONFIG.get(), DEFAULT_CONFIG));
        int mode = Math.floorMod(config - 1, 16);
        PipeCover cover = PipeCover.of(
                "cruciblecraft:selector_tag").withDisplay(0, mode);
        if (!MachineCoverBehaviors.canPlace(
                machine, context.getClickedFace(), cover)) {
            return InteractionResult.FAIL;
        }
        boolean changed = machine.setCover(
                context.getClickedFace(), cover);
        if (changed
                && context.getPlayer() != null
                && !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(
                context.getLevel().isClientSide);
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
