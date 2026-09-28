package com.masson.cruciblecraft.content.blockentity;

import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.content.fluidbarrel.FluidBarrelFluids;
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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
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
        if (transferHeldContainer(player, hand, fluids)) {
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

    /**
     * GT6 fills or empties one container and puts that stack in hand. Creative
     * {@code FluidUtil} keeps the original empty stack after draining the pot.
     */
    private static boolean transferHeldContainer(
            Player player, InteractionHand hand, IFluidHandler fluids) {
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty() || fluids == null) {
            return false;
        }
        ItemStack one = held.copyWithCount(1);
        IFluidHandler item = FluidUtil.getFluidHandler(one).orElse(null);
        if (item == null) {
            return false;
        }
        FluidStack contained = item.getFluidInTank(0);
        boolean moved = contained.isEmpty()
                ? fillFromCrucible(item, fluids)
                : emptyIntoCrucible(item, fluids, contained);
        if (!moved) {
            return false;
        }
        ItemStack result = item instanceof IFluidHandlerItem handler
                ? handler.getContainer()
                : one;
        if (held.getCount() <= 1) {
            player.setItemInHand(hand, result);
        } else {
            held.shrink(1);
            if (!player.addItem(result)) {
                player.drop(result, false);
            }
        }
        return true;
    }

    private static boolean fillFromCrucible(IFluidHandler item, IFluidHandler crucible) {
        FluidStack available = crucible.drain(Integer.MAX_VALUE, FluidAction.SIMULATE);
        if (available.isEmpty()) {
            return false;
        }
        int room = item.fill(available, FluidAction.SIMULATE);
        if (room <= 0) {
            return false;
        }
        FluidStack drained = crucible.drain(
                new FluidStack(available.getFluid(), room), FluidAction.EXECUTE);
        if (drained.isEmpty()) {
            return false;
        }
        int filled = item.fill(drained, FluidAction.EXECUTE);
        if (filled < drained.getAmount()) {
            int leftover = drained.getAmount() - Math.max(0, filled);
            if (leftover > 0) {
                crucible.fill(
                        new FluidStack(drained.getFluid(), leftover),
                        FluidAction.EXECUTE);
            }
        }
        return filled > 0;
    }

    /** GT6 pours the whole container or nothing, and refuses gas and acid. */
    private static boolean emptyIntoCrucible(
            IFluidHandler item, IFluidHandler crucible, FluidStack contained) {
        if (FluidBarrelFluids.isGas(contained) || TankFluidSafety.isAcid(contained)) {
            return false;
        }
        int accepted = crucible.fill(contained, FluidAction.SIMULATE);
        if (accepted < contained.getAmount()) {
            return false;
        }
        FluidStack drained = item.drain(contained.getAmount(), FluidAction.EXECUTE);
        if (drained.isEmpty() || drained.getAmount() < contained.getAmount()) {
            if (!drained.isEmpty()) {
                item.fill(drained, FluidAction.EXECUTE);
            }
            return false;
        }
        int filled = crucible.fill(drained, FluidAction.EXECUTE);
        if (filled < drained.getAmount()) {
            item.fill(
                    new FluidStack(drained.getFluid(), drained.getAmount() - filled),
                    FluidAction.EXECUTE);
            return filled > 0;
        }
        return true;
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
