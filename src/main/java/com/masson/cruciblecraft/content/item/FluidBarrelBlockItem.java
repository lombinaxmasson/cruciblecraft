package com.masson.cruciblecraft.content.item;

import java.util.Objects;

import com.masson.cruciblecraft.content.block.FluidBarrelBlock;
import com.masson.cruciblecraft.content.block.LargeCrucibleWalls;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.fluidbarrel.FluidBarrelContents;
import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Barrel block item. Filled stacks do not combine. Shift-click places. */
public final class FluidBarrelBlockItem extends BlockItem {
    private final String englishName;
    private final String chineseName;

    public FluidBarrelBlockItem(
            FluidBarrelBlock block, String englishName, String chineseName) {
        super(block, new Item.Properties().stacksTo(16));
        this.englishName = Objects.requireNonNull(englishName, "englishName");
        this.chineseName = Objects.requireNonNull(chineseName, "chineseName");
    }

    public FluidBarrelBlock barrel() {
        return (FluidBarrelBlock) getBlock();
    }

    public FluidBarrelItemHandler fluidHandler(ItemStack stack) {
        return new FluidBarrelItemHandler(stack, barrel().profile());
    }

    @Override
    public Component getName(ItemStack stack) {
        return CatalogDisplayNames.itemName(
                getDescriptionId(stack), englishName, chineseName);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        FluidBarrelContents contents = stack.get(ModComponents.FLUID_BARREL.get());
        if (contents != null && contents.amount() > 0L) {
            return 1;
        }
        return 16;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player != null && !player.isShiftKeyDown()) {
            IFluidHandler destination = fluidTarget(
                    level, context.getClickedPos(), context.getClickedFace());
            if (destination != null) {
                if (transfer(context, destination, level.isClientSide)) {
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
                return InteractionResult.PASS;
            }
        }
        return super.useOn(context);
    }

    /**
     * Block fluid capability, plus large-crucible walls. Those walls keep an
     * in-place block entity and only the top layer forwards the controller tank.
     */
    private static IFluidHandler fluidTarget(
            Level level, BlockPos pos, Direction face) {
        IFluidHandler capability = level.getCapability(
                Capabilities.FluidHandler.BLOCK, pos, face);
        if (capability != null) {
            return capability;
        }
        if (level.getBlockEntity(pos) instanceof MteInPlaceBlockEntity wall) {
            return LargeCrucibleWalls.fluids(wall);
        }
        return null;
    }

    private boolean transfer(
            UseOnContext context, IFluidHandler destination, boolean simulate) {
        ItemStack held = context.getItemInHand();
        ItemStack single = held.copyWithCount(1);
        FluidBarrelItemHandler handler = fluidHandler(single);
        FluidBarrelContents contents = held.getOrDefault(
                ModComponents.FLUID_BARREL.get(), FluidBarrelContents.EMPTY);
        boolean intoBlock = contents.amount() > 0L;
        FluidStack moved = intoBlock
                ? FluidUtil.tryFluidTransfer(
                        destination, handler, Integer.MAX_VALUE, !simulate)
                : FluidUtil.tryFluidTransfer(
                        handler, destination, Integer.MAX_VALUE, !simulate);
        if (moved.isEmpty()) {
            return false;
        }
        if (simulate) {
            return true;
        }
        Player player = context.getPlayer();
        if (held.getCount() == 1) {
            FluidBarrelContents updated = single.get(ModComponents.FLUID_BARREL.get());
            if (updated == null) {
                held.remove(ModComponents.FLUID_BARREL.get());
            } else {
                held.set(ModComponents.FLUID_BARREL.get(), updated);
            }
            return true;
        }
        held.shrink(1);
        if (player == null || !player.addItem(single)) {
            if (player != null) {
                player.drop(single, false);
            }
        }
        return true;
    }
}
