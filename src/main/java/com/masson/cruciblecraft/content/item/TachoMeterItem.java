package com.masson.cruciblecraft.content.item;

import java.util.Objects;

import com.masson.cruciblecraft.content.blockentity.RotationalAxleBlockEntity;
import com.masson.cruciblecraft.content.blockentity.RotationalGearboxBlockEntity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** GT6 Tachometer: reports last-tick axle/gearbox RU flow. */
public final class TachoMeterItem extends Item {
    public static final String REGISTRY_PATH =
            "gt_multiitem/multiitem_randomtools_m10004";

    private final String englishName;
    private final String chineseName;

    public TachoMeterItem(
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
        BlockEntity be = level.getBlockEntity(context.getClickedPos());
        long transferred;
        if (be instanceof RotationalAxleBlockEntity axle) {
            transferred = axle.transferredLast();
        } else if (be instanceof RotationalGearboxBlockEntity gearbox) {
            transferred = gearbox.transferredLast();
        } else {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && context.getPlayer() != null) {
            context.getPlayer().displayClientMessage(
                    Component.literal(transferred + " RU/t"),
                    false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
