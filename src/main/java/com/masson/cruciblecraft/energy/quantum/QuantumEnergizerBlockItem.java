package com.masson.cruciblecraft.energy.quantum;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Placement item; tooltip names LU/QU packets. */
public final class QuantumEnergizerBlockItem extends BlockItem {
    public QuantumEnergizerBlockItem(
            QuantumEnergizerBlock block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (!(getBlock() instanceof QuantumEnergizerBlock block)) {
            return;
        }
        QuantumEnergizerProfile profile = block.profile();
        tooltip.add(Component.literal("LU " + profile.luInput()
                        + " → QU " + profile.quOutput())
                .withStyle(ChatFormatting.GRAY));
        if (profile.extension()) {
            tooltip.add(Component.literal("CC_EXTENSION")
                    .withStyle(ChatFormatting.DARK_PURPLE));
        }
    }
}
