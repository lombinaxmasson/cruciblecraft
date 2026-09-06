package com.masson.cruciblecraft.content.item;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Reusable assembler selectors that preserve the distinguishing shape of a
 * GT6 crafting pattern after the shaped recipe is flattened into a machine
 * input multiset.
 */
public final class ToolPatternCatalog {
    public static final List<Definition> DEFINITIONS = List.of(
            pattern("pickaxe", "Pickaxe Head", "镐头", "PPP", " C "),
            pattern("shovel", "Shovel Head", "铲头", " P ", " C ", " C "),
            pattern("axe", "Axe Head", "斧头", "PP ", "PC ", " C "),
            pattern("hoe", "Hoe Head", "锄头", "PP ", " C "),
            pattern("sword", "Sword Blade", "剑刃", " P ", " P ", " C "),
            pattern(
                    "smithing_hammer",
                    "Smithing Hammer Head",
                    "锻造锤头",
                    "PP ",
                    "PPC",
                    "PP "),
            pattern("file", "File Head", "锉头", " P ", " PC"),
            pattern("chisel", "Chisel Head", "凿头", "CP ", " P "),
            pattern("saw", "Saw Blade", "锯片", "PP ", "CC "),
            pattern("screwdriver", "Screwdriver Head", "螺丝刀头", " C ", " P ", " P "),
            pattern("wrench", "Wrench", "扳手", "PCP", " P ", " P "),
            pattern(
                    "monkey_wrench",
                    "Monkey Wrench",
                    "活动扳手",
                    "PP ",
                    "PC ",
                    " P "),
            // GT6 Loader_Tools.java:324 wirecutter shape {"PfP","hPd","STS"}
            // flattened to the P/C/S alphabet (plates, tool heads, sticks).
            pattern("wire_cutter", "Wire Cutter Head", "剪线钳头", "PCP", "CPC", "SCS"));
    private static final Map<String, Definition> BY_ID = DEFINITIONS.stream()
            .collect(Collectors.toUnmodifiableMap(Definition::id, Function.identity()));

    private ToolPatternCatalog() {}

    public static Map<String, DeferredItem<Item>> registerAll(DeferredRegister.Items items) {
        return DEFINITIONS.stream().collect(Collectors.toUnmodifiableMap(
                Definition::id,
                definition -> items.register(
                        definition.registryPath(),
                        () -> new ToolPatternItem(
                                definition,
                                new Item.Properties().stacksTo(1)))));
    }

    public static Definition require(String id) {
        Definition definition = BY_ID.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown tool pattern " + id);
        }
        return definition;
    }

    public static boolean isPattern(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem() instanceof ToolPatternItem pattern
                && BY_ID.get(pattern.definition().id()) == pattern.definition();
    }

    private static Definition pattern(
            String id,
            String english,
            String chinese,
            String... recipePattern) {
        return new Definition(
                id,
                "tool_pattern_" + id,
                "Tool Pattern (" + english + ")",
                "工具图样（" + chinese + "）",
                List.of(recipePattern));
    }

    public record Definition(
            String id,
            String registryPath,
            String englishName,
            String chineseName,
            List<String> recipePattern) {
        public Definition {
            recipePattern = List.copyOf(recipePattern);
        }
    }

    public static final class ToolPatternItem extends Item {
        private final Definition definition;

        public ToolPatternItem(Definition definition, Properties properties) {
            super(properties);
            this.definition = definition;
        }

        public Definition definition() {
            return definition;
        }
    }
}
