package com.masson.cruciblecraft.content.item;

import java.util.List;
import java.util.Optional;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.content.mold.MoldRecipes;
import com.masson.cruciblecraft.content.mold.MoldShape;
import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

public final class CeramicMoldBlockItem extends BlockItem {
    private final int defaultPattern;

    public CeramicMoldBlockItem(Block block, MoldShape shape, Properties properties) {
        this(block, shape.mask(), properties);
    }

    public CeramicMoldBlockItem(Block block, int defaultPattern, Properties properties) {
        super(block, properties);
        this.defaultPattern = defaultPattern & ((1 << MoldRecipes.CELL_COUNT) - 1);
    }

    public int defaultPattern() {
        return defaultPattern;
    }

    public Optional<MoldShape> namedShape() {
        return MoldShape.fromMask(defaultPattern);
    }

    public int placementPattern(ItemStack stack) {
        Integer stored = stack.get(ModComponents.MOLD_PATTERN);
        if (stored != null) {
            return stored & ((1 << MoldRecipes.CELL_COUNT) - 1);
        }
        return defaultPattern;
    }

    public Optional<MaterialPrefix> castingForm(ItemStack stack) {
        return MoldRecipes.recipe(placementPattern(stack));
    }

    @Override
    public String getDescriptionId() {
        return namedShape()
                .map(shape -> "item.cruciblecraft." + shape.serializedName() + "_mold")
                .orElseGet(super::getDescriptionId);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        int pattern = placementPattern(stack);
        Optional<MaterialPrefix> recipe = MoldRecipes.recipe(pattern);
        if (recipe.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.cruciblecraft.mold_unshaped"));
            return;
        }
        tooltip.add(Component.translatable(
                "tooltip.cruciblecraft.mold_recipe",
                recipe.get().serializedName().replace('_', ' '),
                MoldRecipes.requiredUnits(pattern)));
    }
}
