package com.masson.cruciblecraft.content.sensor;

import java.util.Map;

import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.MaterialDefinition;

import net.minecraft.world.item.ItemStack;

/**
 * GT6 {@code OreDictMaterial.getWeight}: kg =
 * {@code density_g_cm3 * 111.111111 * amount / U}.
 */
public final class ItemMass {
    private static final double KG_PER_DENSITY_UNIT = 111.111111;

    private ItemMass() {}

    public static double kilograms(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0.0;
        }
        return MaterialLookup.byItem(stack)
                .map(entry -> stack.getCount()
                        * kilograms(entry.material(), entry.units()))
                .orElse(0.0);
    }

    public static double kilograms(Map<String, Integer> composition) {
        double kilograms = 0.0;
        for (var entry : composition.entrySet()) {
            kilograms += MaterialCatalog.find(entry.getKey())
                    .map(material -> kilograms(material, entry.getValue()))
                    .orElse(0.0);
        }
        return kilograms;
    }

    private static double kilograms(MaterialDefinition material, int units) {
        int ingot = MaterialPrefixes.INGOT.units();
        if (ingot <= 0 || units <= 0) {
            return 0.0;
        }
        return (material.thermal().density() * KG_PER_DENSITY_UNIT * units)
                / ingot;
    }
}
