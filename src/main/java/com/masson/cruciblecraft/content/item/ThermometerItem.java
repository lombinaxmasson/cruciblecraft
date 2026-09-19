package com.masson.cruciblecraft.content.item;

import java.util.Objects;

import com.masson.cruciblecraft.content.blockentity.CeramicMoldBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CrucibleBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FoundryCastingBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ReactorCoreBlockEntity;
import com.masson.cruciblecraft.heat.TemperatureDamage;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** GT6 quicksilver thermometer: Kelvin on crucible/mold, reactor lastHeat as HU. */
public final class ThermometerItem extends Item {
    public static final String REGISTRY_PATH = "mercury/thermometer_measures_temperature";

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
        var blockEntity = level.getBlockEntity(context.getClickedPos());
        if (blockEntity instanceof CrucibleBlockEntity crucible) {
            return reportKelvin(context, TemperatureDamage.kelvin(crucible.temperature()), true);
        }
        if (blockEntity instanceof CeramicMoldBlockEntity mold) {
            return reportKelvin(context, TemperatureDamage.kelvin(mold.temperature()), false);
        }
        if (blockEntity instanceof FoundryCastingBlockEntity mold) {
            return reportKelvin(context, TemperatureDamage.kelvin(mold.temperature()), false);
        }
        if (!(blockEntity instanceof ReactorCoreBlockEntity core)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && context.getPlayer() != null) {
            context.getPlayer().displayClientMessage(
                    Component.literal("Heat Levels: " + core.lastHeat() + " HU"),
                    false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static InteractionResult reportKelvin(
            UseOnContext context, long kelvin, boolean warnPickup) {
        Level level = context.getLevel();
        if (!level.isClientSide && context.getPlayer() != null) {
            context.getPlayer().displayClientMessage(
                    warnPickup && kelvin >= 1300L
                            ? Component.translatable(
                                    "message.cruciblecraft.thermometer_kelvin_too_hot",
                                    kelvin)
                            : Component.translatable(
                                    "message.cruciblecraft.thermometer_kelvin",
                                    kelvin),
                    false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
