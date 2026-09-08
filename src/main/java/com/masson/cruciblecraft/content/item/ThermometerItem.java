package com.masson.cruciblecraft.content.item;

import java.util.Objects;

import com.masson.cruciblecraft.content.blockentity.ReactorCoreBlockEntity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** GT6 quicksilver thermometer: reports reactor lastHeat as HU. */
public final class ThermometerItem extends Item {
    public static final String REGISTRY_PATH = "gt_multiitem/multiitem_randomtools_m10000";

    private final String englishName;
    private final String chineseName;

    public ThermometerItem(
            Properties properties, String englishName, String chineseName) {
        super(properties.stacksTo(1));
        this.englishName = Objects.requireNonNull(englishName, "englishName");
        this.chineseName = Objects.requireNonNull(chineseName, "chineseName");
    }

    @Override
    public Component getName(ItemStack stack) {
        return CatalogDisplayNames.itemName(
                getDescriptionId(stack), englishName, chineseName);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level.getBlockEntity(context.getClickedPos())
                instanceof ReactorCoreBlockEntity core)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && context.getPlayer() != null) {
            context.getPlayer().displayClientMessage(
                    Component.literal("Heat Levels: " + core.lastHeat() + " HU"),
                    false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
