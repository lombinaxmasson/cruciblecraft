package com.masson.cruciblecraft.content.item;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import com.masson.cruciblecraft.registry.ModFluids;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Fixed-identity GT6 battery part. Empty and filled cells are separate items;
 * filling consumes exactly the source-defined fluid amount.
 */
public final class BatteryCellItem extends Item {
    private final boolean filled;
    private final String fluidMaterial;
    private final int fluidAmount;
    private final Supplier<? extends Item> counterpart;

    public BatteryCellItem(
            Properties properties,
            boolean filled,
            String fluidMaterial,
            int fluidAmount,
            Supplier<? extends Item> counterpart) {
        super(properties.stacksTo(filled ? 1 : 64));
        this.filled = filled;
        this.fluidMaterial = Objects.requireNonNull(fluidMaterial, "fluidMaterial");
        if (fluidAmount <= 0) {
            throw new IllegalArgumentException("Battery cell fluid amount must be positive");
        }
        this.fluidAmount = fluidAmount;
        this.counterpart = Objects.requireNonNull(counterpart, "counterpart");
    }

    public boolean filled() {
        return filled;
    }

    public String fluidMaterial() {
        return fluidMaterial;
    }

    public int fluidAmount() {
        return fluidAmount;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        var source = context.getLevel().getCapability(
                Capabilities.FluidHandler.BLOCK,
                context.getClickedPos(),
                context.getClickedFace());
        if (source == null) {
            return InteractionResult.PASS;
        }
        var fluid = ModFluids.materialFluid(fluidMaterial).orElse(null);
        if (fluid == null) {
            return InteractionResult.PASS;
        }
        FluidStack expected = new FluidStack(fluid, fluidAmount);
        FluidStack simulated = filled
                ? source.fill(expected, IFluidHandler.FluidAction.SIMULATE)
                        == fluidAmount
                        ? expected
                        : FluidStack.EMPTY
                : source.drain(expected, IFluidHandler.FluidAction.SIMULATE);
        if (!filled
                && (simulated.isEmpty()
                        || simulated.getAmount() != fluidAmount
                        || !FluidStack.isSameFluidSameComponents(
                                expected, simulated))) {
            return InteractionResult.PASS;
        }
        if (filled && simulated.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (context.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (filled) {
            if (source.fill(expected, IFluidHandler.FluidAction.EXECUTE)
                    != fluidAmount) {
                return InteractionResult.PASS;
            }
        } else {
            FluidStack drained = source.drain(
                    expected, IFluidHandler.FluidAction.EXECUTE);
            if (drained.isEmpty()
                    || drained.getAmount() != fluidAmount
                    || !FluidStack.isSameFluidSameComponents(expected, drained)) {
                return InteractionResult.PASS;
            }
        }
        replaceOne(player, context, new ItemStack(counterpart.get()));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        tooltip.add(Component.translatable(
                "tooltip.cruciblecraft.battery_cell." + (filled ? "filled" : "empty"),
                fluidAmount,
                fluidMaterial).withStyle(ChatFormatting.GRAY));
    }

    private static void replaceOne(
            Player player,
            UseOnContext context,
            ItemStack replacement) {
        ItemStack held = context.getItemInHand();
        if (held.getCount() == 1) {
            player.setItemInHand(context.getHand(), replacement);
            return;
        }
        held.shrink(1);
        if (!player.getInventory().add(replacement)) {
            player.drop(replacement, false);
        }
    }
}
