package com.masson.cruciblecraft.content.item;

import java.util.Objects;

import com.masson.cruciblecraft.content.blockentity.CableBlockEntity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** GT6 Electrometer: reports last-tick EU/LU cable wattage. */
public final class ElectroMeterItem extends Item {
    public static final String REGISTRY_PATH =
            "gt_multiitem/multiitem_randomtools_m10003";

    private final String englishName;
    private final String chineseName;

    public ElectroMeterItem(
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
                instanceof CableBlockEntity cable)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && context.getPlayer() != null) {
            context.getPlayer().displayClientMessage(
                    Component.literal(cable.wattageLast() + " EU/t"),
                    false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
