package com.masson.cruciblecraft.content.item;

import java.util.Optional;

import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ItemPipeBlockEntity;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCover;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCoverType;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.neoforge.fluids.FluidUtil;

/** Installs one of the three persisted T8 side covers. */
public final class PipeCoverItem extends Item {
    private final PipeCoverType type;

    public PipeCoverItem(PipeCoverType type, Properties properties) {
        super(properties);
        this.type = java.util.Objects.requireNonNull(type, "type");
    }

    public PipeCoverType type() {
        return type;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var blockEntity = context.getLevel().getBlockEntity(
                context.getClickedPos());
        PipeCover cover = cover(
                context, blockEntity instanceof FluidPipeBlockEntity);
        boolean changed;
        if (blockEntity instanceof FluidPipeBlockEntity pipe) {
            changed = pipe.setCover(context.getClickedFace(), cover);
        } else if (blockEntity instanceof ItemPipeBlockEntity pipe) {
            changed = pipe.setCover(context.getClickedFace(), cover);
        } else {
            return InteractionResult.PASS;
        }
        if (changed
                && context.getPlayer() != null
                && !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(
                context.getLevel().isClientSide);
    }

    private PipeCover cover(
            UseOnContext context, boolean fluidPipe) {
        if (type != PipeCoverType.FILTER
                || context.getPlayer() == null
                || context.getPlayer().getOffhandItem().isEmpty()) {
            return new PipeCover(type, Optional.empty());
        }
        if (fluidPipe) {
            var handler = FluidUtil.getFluidHandler(
                    context.getPlayer().getOffhandItem());
            if (handler.isPresent() && handler.orElseThrow().getTanks() > 0) {
                var fluid = handler.orElseThrow().getFluidInTank(0);
                if (!fluid.isEmpty()) {
                    return PipeCover.filter(
                            BuiltInRegistries.FLUID.getKey(
                                    fluid.getFluid()).toString());
                }
            }
        }
        String itemId = BuiltInRegistries.ITEM.getKey(
                context.getPlayer().getOffhandItem().getItem()).toString();
        return PipeCover.filter(itemId);
    }
}
