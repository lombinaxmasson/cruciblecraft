package com.masson.cruciblecraft.content.mold;

import java.util.Optional;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.content.block.FoundryHosts;
import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.item.CeramicMoldBlockItem;
import com.masson.cruciblecraft.content.mte.MteInPlaceSpec;
import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/**
 * Inventory / hopper view of a ceramic or foundry mold item. Foundry molds
 * carry {@link ModComponents#MOLD_PATTERN} the same way ceramic blanks do.
 */
public final class CastingMolds {
    private CastingMolds() {}

    public static boolean isMoldItem(ItemStack stack) {
        return stack != null
                && !stack.isEmpty()
                && (stack.getItem() instanceof CeramicMoldBlockItem
                        || foundryMold(stack).isPresent());
    }

    public static Optional<MteInPlaceSpec> foundryMold(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof MteInPlaceBlock inplace
                && FoundryHosts.isMold(inplace.spec())) {
            return Optional.of(inplace.spec());
        }
        return Optional.empty();
    }

    public static int pattern(ItemStack stack) {
        if (stack.getItem() instanceof CeramicMoldBlockItem ceramic) {
            return ceramic.placementPattern(stack);
        }
        Integer stored = stack.get(ModComponents.MOLD_PATTERN);
        if (stored == null) {
            return 0;
        }
        return stored & ((1 << MoldRecipes.CELL_COUNT) - 1);
    }

    public static Optional<MaterialPrefix> castingForm(ItemStack stack) {
        if (!isMoldItem(stack)) {
            return Optional.empty();
        }
        return MoldRecipes.recipe(pattern(stack));
    }
}
