package com.masson.cruciblecraft.content.item;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Distinct GT wood plank identities that must not fold onto leftover vanilla
 * planks. Fireproof is the same item plus {@code cruciblecraft:fireproof=1}.
 */
public final class GtWoodCatalog {
    public static final List<Definition> DEFINITIONS = List.of(
            wood("blue_mahoe_planks", "Blue Mahoe Planks", "蓝木槿木板"),
            wood("blue_spruce_planks", "Blue Spruce Planks", "蓝云杉木板"),
            wood("cinnamon_planks", "Cinnamon Planks", "肉桂木板"),
            wood("coconut_planks", "Coconut Planks", "椰树木板"),
            wood("compressed_wood_planks", "Compressed Wood Planks", "压缩木板"),
            wood("crate", "Crate", "板条箱"),
            wood("dead_planks", "Dead Planks", "枯死木板"),
            wood("frozen_planks", "Frozen Planks", "冰冻木板"),
            wood("hazel_planks", "Hazel Planks", "榛树木板"),
            wood("maple_planks", "Maple Planks", "枫树木板"),
            wood("mossy_planks", "Mossy Planks", "苔藓木板"),
            wood("rainbowood_planks", "Rainbowood Planks", "彩虹木板"),
            wood("rotten_planks", "Rotten Planks", "腐朽木板"),
            wood("rubberwood_planks", "Rubberwood Planks", "橡胶树木板"),
            wood("willow_planks", "Willow Planks", "柳树木板"),
            wood("wood_planks", "Wood Planks", "普通木板"));
    private static final Map<String, Definition> BY_ID = DEFINITIONS.stream()
            .collect(Collectors.toUnmodifiableMap(Definition::id, Function.identity()));

    private GtWoodCatalog() {}

    public static Map<String, DeferredItem<Item>> registerAll(DeferredRegister.Items items) {
        return DEFINITIONS.stream().collect(Collectors.toUnmodifiableMap(
                Definition::id,
                definition -> items.register(
                        definition.registryPath(),
                        () -> new GtWoodItem(
                                definition,
                                new Item.Properties()))));
    }

    public static Definition require(String id) {
        Definition definition = BY_ID.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown GT wood " + id);
        }
        return definition;
    }

    public static ItemStack fireproofStack(Item item) {
        ItemStack stack = new ItemStack(item);
        stack.set(ModComponents.FIREPROOF.get(), 1);
        return stack;
    }

    private static Definition wood(String id, String english, String chinese) {
        return new Definition(id, "gt_wood/" + id, english, chinese);
    }

    public record Definition(
            String id,
            String registryPath,
            String englishName,
            String chineseName) {}

    public static final class GtWoodItem extends Item {
        private final Definition definition;

        public GtWoodItem(Definition definition, Properties properties) {
            super(properties);
            this.definition = definition;
        }

        public Definition definition() {
            return definition;
        }

        @Override
        public void appendHoverText(
                ItemStack stack,
                TooltipContext context,
                List<Component> tooltip,
                TooltipFlag flag) {
            if (stack.getOrDefault(ModComponents.FIREPROOF.get(), 0) > 0) {
                tooltip.add(Component.translatable(
                        "tooltip.cruciblecraft.fireproof")
                        .withStyle(ChatFormatting.GOLD));
            }
        }
    }
}
