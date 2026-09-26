package com.masson.cruciblecraft.content.item;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Distinct GT wood plank identities that must not fold onto leftover vanilla
 * planks. Fireproof is the same item plus {@code cruciblecraft:fireproof=1}.
 */
public final class GtWoodCatalog {
    public static final List<Definition> DEFINITIONS = List.of(
            wood("blue_mahoe_planks", "Blue Mahoe Planks", "蓝木槿木板", "planks_bluemahoe"),
            wood("blue_spruce_planks", "Blue Spruce Planks", "蓝云杉木板", "planks_bluespruce"),
            wood("cinnamon_planks", "Cinnamon Planks", "肉桂木板", "planks_cinnamon"),
            wood("coconut_planks", "Coconut Planks", "椰树木板", "planks_coconut"),
            wood("compressed_wood_planks", "Compressed Wood Planks", "压缩木板", "planks_compressed"),
            wood("crate", "Crate", "板条箱", "crate"),
            wood("dead_planks", "Dead Planks", "枯死木板", "planks_dry"),
            wood("frozen_planks", "Frozen Planks", "冰冻木板", "planks_frozen"),
            wood("hazel_planks", "Hazel Planks", "榛树木板", "planks_hazel"),
            wood("maple_planks", "Maple Planks", "枫树木板", "planks_maple"),
            wood("mossy_planks", "Mossy Planks", "苔藓木板", "planks_mossy"),
            wood("rainbowood_planks", "Rainbowood Planks", "彩虹木板", "planks_rainbowood"),
            wood("rotten_planks", "Rotten Planks", "腐朽木板", "planks_rotten"),
            wood("rubberwood_planks", "Rubberwood Planks", "橡胶树木板", "planks_rubber"),
            wood("willow_planks", "Willow Planks", "柳树木板", "planks_willow"),
            wood("wood_planks", "Wood Planks", "普通木板", "planks_wood"));
    private static final Map<String, Definition> BY_ID = definitionsById();

    private GtWoodCatalog() {}

    public static Map<String, DeferredItem<Item>> registerItems(
            DeferredRegister.Items items,
            Map<String, ? extends Supplier<? extends Block>> blocks) {
        LinkedHashMap<String, DeferredItem<Item>> registered = new LinkedHashMap<>();
        for (Definition definition : DEFINITIONS) {
            DeferredItem<Item> item = items.register(
                    definition.registryPath(),
                    () -> new GtWoodBlockItem(
                            definition,
                            blocks.get(definition.id()).get(),
                            new Item.Properties()));
            if (registered.put(definition.id(), item) != null) {
                throw new IllegalStateException("Duplicate GT wood " + definition.id());
            }
        }
        return java.util.Collections.unmodifiableMap(registered);
    }

    private static Map<String, Definition> definitionsById() {
        LinkedHashMap<String, Definition> definitions = new LinkedHashMap<>();
        for (Definition definition : DEFINITIONS) {
            if (definitions.put(definition.id(), definition) != null) {
                throw new IllegalStateException("Duplicate GT wood " + definition.id());
            }
        }
        return java.util.Collections.unmodifiableMap(definitions);
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

    private static Definition wood(
            String id, String english, String chinese, String iconset) {
        return new Definition(id, "gt_wood/" + id, english, chinese, iconset);
    }

    public record Definition(
            String id,
            String registryPath,
            String englishName,
            String chineseName,
            String iconset) {}

    public static final class GtWoodBlockItem extends BlockItem {
        private final Definition definition;

        public GtWoodBlockItem(
                Definition definition, Block block, Properties properties) {
            super(block, properties);
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
