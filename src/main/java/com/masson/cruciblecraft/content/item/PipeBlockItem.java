package com.masson.cruciblecraft.content.item;

import java.util.List;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.content.block.AbstractPipeBlock;
import com.masson.cruciblecraft.logistics.pipe.PipeCatalog;

import com.masson.cruciblecraft.content.block.Gt6StyleConnections;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;

/** Material-form block item for one immutable pipe specification. */
public final class PipeBlockItem extends BlockItem
        implements MaterialFormItem {
    private final PipeCatalog.Entry pipe;

    public PipeBlockItem(
            AbstractPipeBlock block,
            PipeCatalog.Entry pipe,
            Properties properties) {
        super(block, properties);
        this.pipe = java.util.Objects.requireNonNull(pipe, "pipe");
        if (block.pipe() != pipe) {
            throw new IllegalArgumentException(
                    "Pipe block/item identity mismatch");
        }
    }

    public PipeCatalog.Entry pipe() {
        return pipe;
    }

    @Override
    public String materialId() {
        return pipe.materialId();
    }

    @Override
    public MaterialPrefix form() {
        return pipe.form();
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
        tooltip.add(Component.translatable(
                        "tooltip.cruciblecraft.pipe.specification",
                        pipe.sourceSpecification())
                .withStyle(ChatFormatting.GRAY));
        if (pipe.kind() == PipeCatalog.Kind.FLUID) {
            tooltip.add(Component.translatable(
                            "tooltip.cruciblecraft.pipe.fluid_rating",
                            pipe.fluid().capacityMb(),
                            pipe.fluid().maxTemperatureKelvin())
                    .withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable(
                            "tooltip.cruciblecraft.pipe.item_rating",
                            pipe.item().stacksPerSecond(),
                            pipe.item().stepSize())
                    .withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable(
                        "tooltip.cruciblecraft.pipe.connect")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
