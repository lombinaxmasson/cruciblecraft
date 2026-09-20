package com.masson.cruciblecraft.client.color;

import com.masson.cruciblecraft.content.item.CellItem;
import com.masson.cruciblecraft.material.CellContentGate;
import com.masson.cruciblecraft.material.MaterialCatalog;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

/**
 * GT6 PrefixItem pass-0: tint the shared cell sprite with the contained
 * fluid's recorded material color. Overlay layers (tintIndex != 0) stay white.
 */
public final class CellItemColor {
    private CellItemColor() {}

    public static int color(ItemStack stack, int tintIndex) {
        if (tintIndex != 0 || !(stack.getItem() instanceof CellItem cell)) {
            return 0xFFFFFFFF;
        }
        SimpleFluidContent content = cell.content(stack);
        if (content.isEmpty()) {
            return 0xFFFFFFFF;
        }
        return CellContentGate.materialId(content.copy().getFluid())
                .flatMap(MaterialCatalog::find)
                .map(material -> 0xFF000000
                        | MaterialItemColor.styleColor(
                                material.colorRgb(), material.tintStyle()))
                .orElseGet(() -> fluidTint(content));
    }

    static int tint(
            int tintIndex,
            boolean empty,
            Integer materialRgb) {
        if (tintIndex != 0 || empty || materialRgb == null) {
            return 0xFFFFFFFF;
        }
        return 0xFF000000 | (materialRgb & 0xFFFFFF);
    }

    private static int fluidTint(SimpleFluidContent content) {
        int tint = IClientFluidTypeExtensions.of(content.getFluid())
                .getTintColor();
        return 0xFF000000 | (tint & 0xFFFFFF);
    }
}
