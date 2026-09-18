package com.masson.cruciblecraft.client.color;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.energy.cable.ElectricWireRemainderIds;
import com.masson.cruciblecraft.energy.cable.ElectricalConductorCatalog;
import com.masson.cruciblecraft.material.MaterialCatalog;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Material / insulation tints for leftover {@code electric_wire/*} items. */
public final class ElectricWireRemainderColor {
    private ElectricWireRemainderColor() {}

    public static int color(ItemStack stack, int tintIndex) {
        var parsed = parse(stack);
        if (parsed.isEmpty()) {
            return 0xFFFFFFFF;
        }
        if (tintIndex == 1) {
            return parsed.orElseThrow().cable()
                    ? ElectricalConductorCatalog.INSULATION_COLOR
                    : 0xFFFFFFFF;
        }
        if (tintIndex != 0) {
            return 0xFFFFFFFF;
        }
        return MaterialCatalog.find(parsed.orElseThrow().materialId())
                .map(material ->
                        0xFF000000
                                | MaterialItemColor.styleColor(
                                        material.colorRgb(),
                                        material.tintStyle()))
                .orElse(0xFFFFFFFF);
    }

    private static java.util.Optional<ElectricWireRemainderIds.Parsed> parse(
            ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!CrucibleCraft.MODID.equals(id.getNamespace())) {
            return java.util.Optional.empty();
        }
        return ElectricWireRemainderIds.parse(id.getPath());
    }
}
