package com.masson.cruciblecraft.content.item;

import java.util.List;

import com.masson.cruciblecraft.nuclear.ReactorRodCatalog;
import com.masson.cruciblecraft.nuclear.ReactorRodState;
import com.masson.cruciblecraft.registry.ModComponents;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Stateful GT6 reactor rod. Ordinary material rods are not substitutes. */
public final class ReactorRodItem extends Item {
    private final ReactorRodCatalog.Entry entry;

    public ReactorRodItem(
            ReactorRodCatalog.Entry entry, Properties properties) {
        super(properties.stacksTo(1));
        this.entry = entry;
    }

    public ReactorRodCatalog.Entry entry() {
        return entry;
    }

    public ItemStack defaultStack() {
        ItemStack stack = new ItemStack(this);
        if (entry.hasDurability()) {
            stack.set(
                    ModComponents.REACTOR_ROD_STATE.get(),
                    ReactorRodState.fresh(entry.durability()));
        }
        return stack;
    }

    public static ReactorRodState state(ItemStack stack) {
        if (!(stack.getItem() instanceof ReactorRodItem rod)) {
            return ReactorRodState.fresh(0L);
        }
        return stack.getOrDefault(
                ModComponents.REACTOR_ROD_STATE.get(),
                ReactorRodState.fresh(rod.entry.durability()));
    }

    public static void writeState(ItemStack stack, ReactorRodState state) {
        stack.set(ModComponents.REACTOR_ROD_STATE.get(), state);
    }

    public static ItemStack transform(
            ItemStack stack, String destinationPath) {
        if (destinationPath == null || destinationPath.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return ModItems.reactorRod(destinationPath).get().defaultStack();
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        tooltip.add(Component.translatable(
                        "tooltip.cruciblecraft.reactor_rod."
                                + entry.kind().name().toLowerCase())
                .withStyle(ChatFormatting.GRAY));
        if (entry.hasDurability()) {
            tooltip.add(Component.translatable(
                            "tooltip.cruciblecraft.reactor_rod.durability",
                            state(stack).durability())
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        if (entry.kind() == ReactorRodCatalog.Kind.NUCLEAR) {
            tooltip.add(Component.translatable(
                            "tooltip.cruciblecraft.reactor_rod.neutrons",
                            entry.neutronOther(),
                            entry.neutronSelf(),
                            entry.neutronMax(),
                            entry.neutronDiv())
                    .withStyle(ChatFormatting.DARK_GREEN));
        }
    }
}
