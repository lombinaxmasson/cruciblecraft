package com.masson.cruciblecraft.recipe.crafting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.item.MaterialToolItem;
import com.masson.cruciblecraft.content.item.ToolDisplayPlan;
import com.masson.cruciblecraft.registry.ModIngredientTypes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

/**
 * GT6 OreDict crafting tool: any valid material of the tagged tools.
 *
 * <p>JSON can stay a {@code crafting_tools/*} tag. Decode rewrites onto this
 * type so {@link #getItems()} can list routed material variants for EMI
 * instead of the shared item's default iron stack.
 */
public record CraftingToolIngredient(TagKey<Item> tag) implements ICustomIngredient {
    public static final MapCodec<CraftingToolIngredient> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    TagKey.codec(Registries.ITEM)
                            .fieldOf("tag")
                            .forGetter(CraftingToolIngredient::tag))
                    .apply(instance, CraftingToolIngredient::new));

    @Override
    public boolean test(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (!stack.is(tag) && !CraftingTools.belongs(tag, stack.getItem())) {
            return false;
        }
        if (stack.getItem() instanceof MaterialToolItem tool) {
            return tool.material(stack).isPresent();
        }
        return true;
    }

    @Override
    public Stream<ItemStack> getItems() {
        return displayStacks().stream();
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return ModIngredientTypes.CRAFTING_TOOL.get();
    }

    private List<ItemStack> displayStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        Set<Item> seen = new HashSet<>();
        for (ItemStack routed : ToolDisplayPlan.routedVariantStacks()) {
            if (!CraftingTools.belongs(tag, routed.getItem())) {
                continue;
            }
            stacks.add(routed.copy());
            seen.add(routed.getItem());
        }
        for (String path : CraftingTools.memberPaths(tag)) {
            Item item = BuiltInRegistries.ITEM.get(
                    ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, path));
            if (item == Items.AIR
                    || seen.contains(item)
                    || item instanceof MaterialToolItem) {
                continue;
            }
            stacks.add(new ItemStack(item));
            seen.add(item);
        }
        if (!stacks.isEmpty()) {
            return stacks;
        }
        for (String path : CraftingTools.memberPaths(tag)) {
            Item item = BuiltInRegistries.ITEM.get(
                    ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, path));
            if (item != Items.AIR) {
                stacks.add(new ItemStack(item));
            }
        }
        return stacks;
    }
}
