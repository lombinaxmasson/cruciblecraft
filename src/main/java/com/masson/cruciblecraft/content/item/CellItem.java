package com.masson.cruciblecraft.content.item;

import java.util.List;
import java.util.function.Supplier;

import com.masson.cruciblecraft.material.CellContentGate;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

/** Reusable 1000 mB liquid or gas cell with stack-safe manual filling. */
public final class CellItem extends Item {
    private final Supplier<DataComponentType<SimpleFluidContent>> component;
    private final CellContentGate.Kind kind;

    public CellItem(
            Properties properties,
            Supplier<DataComponentType<SimpleFluidContent>> component,
            CellContentGate.Kind kind) {
        super(properties.stacksTo(64));
        this.component = component;
        this.kind = kind;
    }

    public CellContentGate.Kind kind() {
        return kind;
    }

    public CellFluidHandler handler(ItemStack stack) {
        return new CellFluidHandler(component, stack, kind);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return stackLimit(content(stack));
    }

    @Override
    public Component getName(ItemStack stack) {
        SimpleFluidContent content = content(stack);
        if (content.isEmpty()) {
            return super.getName(stack);
        }
        return Component.translatable(
                kind == CellContentGate.Kind.FLUID
                        ? "item.cruciblecraft.fluid_cell.filled"
                        : "item.cruciblecraft.gas_cell.filled",
                content.copy().getHoverName());
    }

    static int stackLimit(SimpleFluidContent content) {
        return content.isEmpty() ? 64 : 1;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack held = context.getItemInHand();
        Player player = context.getPlayer();
        if (player == null
                || !content(held).isEmpty()) {
            return InteractionResult.PASS;
        }
        var source = context.getLevel().getCapability(
                Capabilities.FluidHandler.BLOCK,
                context.getClickedPos(),
                context.getClickedFace());
        if (source == null) {
            return InteractionResult.PASS;
        }
        ItemStack single = held.copyWithCount(1);
        if (FluidUtil.tryFluidTransfer(
                        handler(single),
                        source,
                        CellFluidHandler.CAPACITY,
                        false)
                .isEmpty()) {
            return InteractionResult.PASS;
        }
        if (context.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        return FluidUtil.interactWithFluidHandler(
                        player,
                        context.getHand(),
                        source)
                ? InteractionResult.SUCCESS
                : InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        SimpleFluidContent content = content(stack);
        String key = kind == CellContentGate.Kind.FLUID
                ? "tooltip.cruciblecraft.fluid_cell"
                : "tooltip.cruciblecraft.gas_cell";
        if (content.isEmpty()) {
            tooltip.add(Component.translatable(
                    key + ".empty",
                    CellFluidHandler.CAPACITY).withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.add(Component.translatable(
                key + ".contents",
                content.copy().getHoverName(),
                content.getAmount(),
                CellFluidHandler.CAPACITY).withStyle(ChatFormatting.GRAY));
    }

    private SimpleFluidContent content(ItemStack stack) {
        return stack.getOrDefault(component.get(), SimpleFluidContent.EMPTY);
    }
}
