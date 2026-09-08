package com.masson.cruciblecraft.content.item;

import java.util.Objects;

import com.masson.cruciblecraft.content.blockentity.ReactorCoreBlockEntity;
import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Empty Geiger has no reading. Filled Geiger reports neutron sum and toggles. */
public final class GeigerCounterItem extends Item {
    public static final String EMPTY_PATH = "gt_multiitem/multiitem_randomtools_m10001";
    public static final String FILLED_PATH = "gt_multiitem/multiitem_randomtools_m10002";

    private final boolean filled;
    private final String englishName;
    private final String chineseName;

    public GeigerCounterItem(
            Properties properties,
            boolean filled,
            String englishName,
            String chineseName) {
        super(properties.stacksTo(1));
        this.filled = filled;
        this.englishName = Objects.requireNonNull(englishName, "englishName");
        this.chineseName = Objects.requireNonNull(chineseName, "chineseName");
    }

    public boolean filled() {
        return filled;
    }

    public static boolean enabled(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(ModComponents.GEIGER_ENABLED));
    }

    @Override
    public Component getName(ItemStack stack) {
        return CatalogDisplayNames.itemName(
                getDescriptionId(stack), englishName, chineseName);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!filled) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide) {
            boolean next = !enabled(stack);
            stack.set(ModComponents.GEIGER_ENABLED, next);
            player.displayClientMessage(
                    Component.literal(next ? "Geiger Counter: ON" : "Geiger Counter: OFF"),
                    false);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!filled || !enabled(context.getItemInHand())) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        if (!(level.getBlockEntity(context.getClickedPos())
                instanceof ReactorCoreBlockEntity core)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && context.getPlayer() != null) {
            context.getPlayer().displayClientMessage(
                    Component.literal("Neutron Levels: " + core.neutronSum() + "n"),
                    false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
