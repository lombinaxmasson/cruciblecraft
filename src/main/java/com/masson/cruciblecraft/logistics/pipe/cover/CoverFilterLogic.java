package com.masson.cruciblecraft.logistics.pipe.cover;

import com.masson.cruciblecraft.api.tool.ToolAction;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;

/**
 * GT6 {@code CoverFilterItem} / {@code CoverFilterFluid}: empty whitelist
 * blocks everything; screwdriver toggles whitelist/blacklist; soft hammer
 * clears the sample; right-click samples the held item or fluid.
 */
public final class CoverFilterLogic {
    private CoverFilterLogic() {}

    public static boolean isItemFilter(PipeCover cover) {
        return cover != null && "filter".equals(cover.definitionId().getPath());
    }

    public static boolean isFluidFilter(PipeCover cover) {
        return cover != null
                && "filter_fluid".equals(cover.definitionId().getPath());
    }

    public static boolean isFilter(PipeCover cover) {
        return isItemFilter(cover) || isFluidFilter(cover);
    }

    public static boolean allowsItem(PipeCover cover, ItemStack stack) {
        if (!isItemFilter(cover) || stack == null || stack.isEmpty()) {
            return !isItemFilter(cover);
        }
        return allowsId(
                cover,
                BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    public static boolean allowsFluid(PipeCover cover, FluidStack stack) {
        if (!isFluidFilter(cover) || stack == null || stack.isEmpty()) {
            return !isFluidFilter(cover);
        }
        return allowsId(
                cover,
                BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString());
    }

    public static boolean allowsId(PipeCover cover, String actualId) {
        boolean whitelist = !CoverItemFilters.inverted(cover.config());
        if (cover.config().matchId().isEmpty()) {
            return !whitelist;
        }
        boolean equal = cover.config().matchId().orElseThrow().equals(actualId);
        return whitelist == equal;
    }

    public static boolean onTool(
            ToolAction action,
            PipeCover cover,
            CoverTools.Replacer replacer) {
        if (!isFilter(cover) || action == null || replacer == null) {
            return false;
        }
        if (action == ToolAction.SCREWDRIVER) {
            int next = cover.config().invert().orElse(0) == 0 ? 1 : 0;
            replacer.replace(cover.withConfig(cover.config().withInvert(next)));
            return true;
        }
        if (action == ToolAction.SOFT_HAMMER) {
            if (cover.config().matchId().isEmpty()) {
                return true;
            }
            replacer.replace(cover.withConfig(cover.config().withoutMatchId()));
            return true;
        }
        return false;
    }

    public static boolean onRightClick(
            Player player,
            PipeCover cover,
            CoverTools.Replacer replacer) {
        if (!isFilter(cover) || replacer == null) {
            return false;
        }
        if (player == null || player.level().isClientSide) {
            return true;
        }
        ItemStack held = player.getMainHandItem();
        if (cover.config().matchId().isPresent() || held.isEmpty()) {
            return true;
        }
        if (isFluidFilter(cover)) {
            var handler = FluidUtil.getFluidHandler(held);
            if (handler.isPresent() && handler.orElseThrow().getTanks() > 0) {
                FluidStack fluid = handler.orElseThrow().getFluidInTank(0);
                if (!fluid.isEmpty()) {
                    String id = BuiltInRegistries.FLUID.getKey(
                            fluid.getFluid()).toString();
                    replacer.replace(cover.withConfig(
                            cover.config().withMatchId(id)));
                }
            }
            return true;
        }
        replacer.replace(cover.withConfig(cover.config().withMatchId(
                BuiltInRegistries.ITEM.getKey(held.getItem()).toString())));
        return true;
    }
}
