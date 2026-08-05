package com.masson.cruciblecraft.content.item;

import net.minecraft.world.item.Item;

/**
 * Fixed bootstrap crafting tool for the GT6 file-head recipe. It deliberately
 * stays outside the material-tool batch: files unlock the remaining workshop
 * tool chain, while the knife has no file-free generic head route in scope.
 */
public final class FlintKnifeItem extends Item {
    public static final int MAX_DAMAGE = 48;

    public FlintKnifeItem(Properties properties) {
        super(properties.stacksTo(1).durability(MAX_DAMAGE));
    }
}
