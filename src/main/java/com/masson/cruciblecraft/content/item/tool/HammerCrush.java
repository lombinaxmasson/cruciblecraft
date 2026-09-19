package com.masson.cruciblecraft.content.item.tool;

import java.util.Optional;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.content.block.GtBrokenOreBlock;
import com.masson.cruciblecraft.content.block.GtHostedOreBlock;
import com.masson.cruciblecraft.content.block.GtSmallOreBlock;
import com.masson.cruciblecraft.content.block.StoneLayerRockOreBlock;
import com.masson.cruciblecraft.content.blockentity.BedrockOreBlockEntity;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6 {@code RecipeMapHammer.getRecipeFor}: ore prefixes become crushed
 * (dust fallback). Missing live forms stay unconverted.
 */
public final class HammerCrush {
    public record Target(String materialId, int multiplier) {}

    private HammerCrush() {}

    public static boolean isOrePrefix(MaterialPrefix form) {
        return form.equals(MaterialPrefixes.ORE)
                || form.equals(MaterialPrefixes.RAW_ORE);
    }

    public static Optional<ItemStack> convert(ItemStack stack) {
        return identify(stack).flatMap(target -> output(target, stack.getCount()));
    }

    public static Optional<ItemStack> outputForBlock(
            BlockState state, BlockEntity blockEntity) {
        if (state.getBlock() instanceof GtSmallOreBlock
                || state.getBlock() instanceof GtHostedOreBlock
                || state.getBlock() instanceof GtBrokenOreBlock) {
            return Optional.empty();
        }
        return identify(state, blockEntity).flatMap(target -> output(target, 1));
    }

    public static Optional<Target> identify(
            BlockState state, BlockEntity blockEntity) {
        if (state.getBlock() instanceof StoneLayerRockOreBlock ore) {
            return Optional.of(new Target(ore.materialId(), 2));
        }
        if (state.getBlock() instanceof GtSmallOreBlock) {
            return Optional.empty();
        }
        if (blockEntity instanceof BedrockOreBlockEntity ore && ore.hasMaterial()) {
            return Optional.of(new Target(ore.materialId(), 1));
        }
        return identify(new ItemStack(state.getBlock()));
    }

    public static Optional<Target> identify(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        String hosted = stack.get(ModComponents.ORE_MATERIAL.get());
        if (hosted != null
                && !hosted.isEmpty()
                && (stack.getItem() instanceof BlockItem blockItem)
                && (blockItem.getBlock() instanceof GtHostedOreBlock
                        || blockItem.getBlock() instanceof GtBrokenOreBlock)) {
            return Optional.of(new Target(hosted, 1));
        }
        Optional<Target> units = MaterialUnits.resolve(stack)
                .filter(entry -> isOrePrefix(entry.form()))
                .map(entry -> new Target(entry.materialId(), 1));
        if (units.isPresent()) {
            return units;
        }
        return identifyOreCube(stack);
    }

    public static Optional<ItemStack> output(Target target, int count) {
        int total = Math.max(1, target.multiplier()) * Math.max(1, count);
        return MaterialLookup.tryStack(
                        target.materialId(), MaterialPrefixes.CRUSHED_ORE, total)
                .or(() -> MaterialLookup.tryStack(
                        target.materialId(), MaterialPrefixes.DUST, total));
    }

    private static Optional<Target> identifyOreCube(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!CrucibleCraft.MODID.equals(id.getNamespace())) {
            return Optional.empty();
        }
        String path = id.getPath();
        String material;
        if (path.startsWith("deepslate_") && path.endsWith("_ore")) {
            material = path.substring("deepslate_".length(), path.length() - 4);
        } else if (path.endsWith("_ore") && !path.startsWith("gt_")) {
            material = path.substring(0, path.length() - 4);
        } else {
            return Optional.empty();
        }
        if (MaterialCatalog.find(material).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Target(material, 1));
    }
}
