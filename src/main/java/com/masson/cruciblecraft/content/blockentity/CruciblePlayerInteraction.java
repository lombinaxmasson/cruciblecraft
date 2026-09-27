package com.masson.cruciblecraft.content.blockentity;

import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.fluid.CrucibleInteractionMessages;
import com.masson.cruciblecraft.fluid.CrucibleTransferCoordinator.InsertResult;
import com.masson.cruciblecraft.heat.ItemHeat;
import com.masson.cruciblecraft.heat.TemperatureDamage;
import com.masson.cruciblecraft.machine.component.CrucibleProcessCore;
import com.masson.cruciblecraft.machine.component.CrucibleProcessCore.ScrapTake;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Shared GT6 top-face crucible click: shovel scrap, fluids, insert, empty-hand
 * buffer/scrap. Status chat is only the fallback when the pot is idle.
 */
public final class CruciblePlayerInteraction {
    private CruciblePlayerInteraction() {}

    public static boolean isShovel(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.canPerformAction(ItemAbilities.SHOVEL_FLATTEN)
                        || stack.canPerformAction(ItemAbilities.SHOVEL_DOUSE));
    }

    public static boolean predictsItemUse(ItemStack stack) {
        return isShovel(stack)
                || MaterialUnits.resolve(stack).isPresent()
                || FluidUtil.getFluidHandler(stack).isPresent();
    }

    public static ItemInteractionResult useItemOn(
            Level level,
            Player player,
            InteractionHand hand,
            ItemStack stack,
            Direction face,
            CrucibleProcessCore process,
            ItemStackHandler buffer,
            IFluidHandler fluids,
            boolean canInteract) {
        if (face != Direction.UP || !canInteract) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return predictsItemUse(stack)
                    ? ItemInteractionResult.SUCCESS
                    : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (process.casing().quarantined()) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.cruciblecraft.crucible_casing_quarantined",
                            process.casing().quarantinedMaterialId()),
                    true);
            return ItemInteractionResult.SUCCESS;
        }
        if (isShovel(stack)) {
            giveScrap(player, process, Integer.MAX_VALUE, true);
            return ItemInteractionResult.SUCCESS;
        }
        if (matchesHeldScrap(stack, process)
                && stack.getCount() < stack.getMaxStackSize()) {
            giveScrap(player, process, 1, false);
            return ItemInteractionResult.SUCCESS;
        }
        if (FluidUtil.interactWithFluidHandler(player, hand, fluids)) {
            return ItemInteractionResult.SUCCESS;
        }
        Optional<MaterialUnits.Entry> material = MaterialUnits.resolve(stack);
        if (material.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        InsertResult result = process.insert(
                material.get(), ItemHeat.temperature(stack, level.getGameTime()));
        if (result == InsertResult.SUCCESS && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        player.displayClientMessage(
                Component.translatable(
                        result == InsertResult.SUCCESS
                                ? "message.cruciblecraft.material_inserted"
                                : CrucibleInteractionMessages.key(result)),
                true);
        return ItemInteractionResult.SUCCESS;
    }

    public static InteractionResult useEmpty(
            Level level,
            Player player,
            Direction face,
            CrucibleProcessCore process,
            ItemStackHandler buffer,
            boolean canInteract) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (face == Direction.UP && canInteract && !process.casing().quarantined()) {
            if (takeBuffer(player, process, buffer)) {
                return InteractionResult.SUCCESS;
            }
            if (giveScrap(player, process, 1, false)) {
                return InteractionResult.SUCCESS;
            }
        }
        String contents = process.composition().entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue() + " u")
                .collect(Collectors.joining(", "));
        player.displayClientMessage(
                Component.translatable(
                        "message.cruciblecraft.crucible_status",
                        String.format(Locale.ROOT, "%.1f", process.temperature(false)),
                        contents.isEmpty() ? "-" : contents),
                true);
        return InteractionResult.SUCCESS;
    }

    private static boolean takeBuffer(
            Player player, CrucibleProcessCore process, ItemStackHandler buffer) {
        ItemStack held = buffer.getStackInSlot(0);
        if (held.isEmpty()) {
            return false;
        }
        buffer.setStackInSlot(0, ItemStack.EMPTY);
        if (!player.addItem(held)) {
            player.drop(held, false);
        }
        TemperatureDamage.apply(player, process.authoritativeTemperature(), 1.0F, 5.0F);
        return true;
    }

    private static boolean giveScrap(
            Player player, CrucibleProcessCore process, int maxCount, boolean shovel) {
        int limit = shovel ? Math.min(64, maxCount) : 1;
        Optional<ScrapTake> taken = process.takeScrap(limit);
        if (taken.isEmpty()) {
            return false;
        }
        ScrapTake scrap = taken.get();
        player.causeFoodExhaustion(scrap.count() <= 0 ? 0.1F : 0.1F * scrap.count());
        TemperatureDamage.apply(player, process.authoritativeTemperature(), 1.0F, 5.0F);
        if (scrap.count() <= 0) {
            return true;
        }
        Optional<ItemStack> stack = scrapStack(scrap.material().id(), scrap.count());
        if (stack.isPresent()) {
            ItemStack given = stack.get();
            if (shovel) {
                if (!player.addItem(given)) {
                    player.drop(given, false);
                }
            } else {
                giveToSelected(player, given);
            }
        }
        return true;
    }

    private static boolean matchesHeldScrap(ItemStack held, CrucibleProcessCore process) {
        if (held.isEmpty()) {
            return false;
        }
        return process.lightestSolid()
                .flatMap(material -> scrapStack(material.id(), 1))
                .filter(one -> ItemStack.isSameItemSameComponents(held, one))
                .isPresent();
    }

    private static Optional<ItemStack> scrapStack(String materialId, int count) {
        return MaterialPrefixCatalog.find("scrap")
                .flatMap(prefix -> MaterialLookup.tryStack(materialId, prefix, count));
    }

    private static void giveToSelected(Player player, ItemStack given) {
        ItemStack selected = player.getMainHandItem();
        if (selected.isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, given);
            return;
        }
        if (ItemStack.isSameItemSameComponents(selected, given)
                && selected.getCount() < selected.getMaxStackSize()) {
            int merge = Math.min(
                    given.getCount(),
                    selected.getMaxStackSize() - selected.getCount());
            selected.grow(merge);
            given.shrink(merge);
        }
        if (!given.isEmpty() && !player.addItem(given)) {
            player.drop(given, false);
        }
    }
}
