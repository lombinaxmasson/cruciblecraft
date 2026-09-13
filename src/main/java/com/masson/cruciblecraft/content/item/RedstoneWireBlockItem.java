package com.masson.cruciblecraft.content.item;

import java.util.List;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.content.block.Gt6StyleConnections;
import com.masson.cruciblecraft.content.block.RedstoneWireBlock;
import com.masson.cruciblecraft.content.redstonewire.RedstoneWireKind;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6 {@code OP.wireGt01} redstone MTE. Same registry id as the material wire
 * form; not an EU {@code CableBlock}.
 */
public final class RedstoneWireBlockItem extends BlockItem
        implements MaterialFormItem {
    private final RedstoneWireKind kind;

    public RedstoneWireBlockItem(
            RedstoneWireBlock block,
            RedstoneWireKind kind,
            Properties properties) {
        super(block, properties);
        this.kind = kind;
        if (block.kind() != kind) {
            throw new IllegalArgumentException(
                    "Redstone wire block/item kind mismatch");
        }
    }

    public RedstoneWireKind kind() {
        return kind;
    }

    @Override
    public String materialId() {
        return kind.materialId();
    }

    @Override
    public MaterialPrefix form() {
        return kind.form();
    }

    @Override
    public Component getName(ItemStack stack) {
        return CatalogDisplayNames.itemName(
                getDescriptionId(stack), kind.langEn(), kind.langZh());
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
        tooltip.add(Component.translatable(
                        "tooltip.cruciblecraft.redstone_wire.range",
                        kind.range())
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable(
                        "tooltip.cruciblecraft.redstone_wire.bandwidth",
                        1)
                .withStyle(ChatFormatting.AQUA));
    }
}
