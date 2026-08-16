package com.masson.cruciblecraft.content.item;

import java.util.List;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.content.block.CableBlock;
import com.masson.cruciblecraft.energy.cable.ElectricalConductorCatalog;

import com.masson.cruciblecraft.content.block.Gt6StyleConnections;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;

/** Material-form block item retaining the former generated item identity. */
public final class CableBlockItem extends BlockItem
        implements MaterialFormItem {
    private final ElectricalConductorCatalog.Entry conductor;

    public CableBlockItem(
            CableBlock block,
            ElectricalConductorCatalog.Entry conductor,
            Properties properties) {
        super(block, properties);
        this.conductor = conductor;
        if (block.conductor() != conductor) {
            throw new IllegalArgumentException(
                    "Cable block/item conductor identity mismatch");
        }
    }

    public ElectricalConductorCatalog.Entry conductor() {
        return conductor;
    }

    @Override
    public String materialId() {
        return conductor.materialId();
    }

    @Override
    public MaterialPrefix form() {
        return conductor.form();
    }

    @Override
    public Component getName(ItemStack stack) {
        return materialFormName();
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        return Gt6StyleConnections.placeBlock(
                context, super.placeBlock(context, state));
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        var electrical = conductor.electrical();
        tooltip.add(Component.translatable(
                        "tooltip.cruciblecraft.electrical.specification",
                        conductor.sourceSpecification())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                        "tooltip.cruciblecraft.electrical.rating",
                        electrical.maxVoltage(),
                        electrical.maxAmperage(),
                        electrical.lossPerMeter())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                        electrical.insulated()
                                ? "tooltip.cruciblecraft.electrical.insulated"
                                : "tooltip.cruciblecraft.electrical.bare")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                        "tooltip.cruciblecraft.pipe.connect")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
