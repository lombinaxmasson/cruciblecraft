package com.masson.cruciblecraft.content.item;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.masson.cruciblecraft.registry.ModItemTags;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The single registration and data-generation catalog for reusable extruder shapes. */
public final class ExtruderShapeCatalog {
    public static final int MAX_STACK_SIZE = 1;
    public static final List<Definition> DEFINITIONS = List.of(
            shape("plate", "Plate", "板"),
            shape("long_rod", "Long Rod", "长杆"),
            shape("bolt", "Bolt", "螺栓"),
            shape("ring", "Ring", "环"),
            shape("cell", "Cell", "容器"),
            shape("ccc", "Capsule-Cell-Container", "胶囊单元容器"),
            shape("ingot", "Ingot", "锭"),
            shape("wire", "Wire", "导线"),
            shape("small_item_casing", "Small Item Casing", "小型物品外壳"),
            shape("tiny_pipe", "Tiny Pipe", "微型管道"),
            shape("small_pipe", "Small Pipe", "小型管道"),
            shape("normal_pipe", "Normal Pipe", "普通管道"),
            shape("large_pipe", "Large Pipe", "大型管道"),
            shape("huge_pipe", "Huge Pipe", "巨型管道"),
            shape("normal_item_pipe", "Normal Item Pipe", "普通物品管道"),
            shape("large_item_pipe", "Large Item Pipe", "大型物品管道"),
            shape("huge_item_pipe", "Huge Item Pipe", "巨型物品管道"),
            shape("block", "Block", "方块"),
            shape("sword_blade", "Sword Blade", "剑刃"),
            shape("pickaxe_head", "Pickaxe Head", "镐头"),
            shape("shovel_head", "Shovel Head", "铲头"),
            shape("axe_head", "Axe Head", "斧头"),
            shape("hoe_head", "Hoe Head", "锄头"),
            shape("hammer_head", "Hammer Head", "锤头"),
            shape("file_head", "File Head", "锉头"),
            shape("saw_blade", "Saw Blade", "锯片"),
            shape("gear", "Gear", "齿轮"),
            shape("bottle", "Bottle", "瓶"),
            shape("curved_plate", "Curved Plate", "弯曲板"),
            shape("small_gear", "Small Gear", "小齿轮"),
            shape("rod", "Rod", "杆"),
            shape("rotor", "Rotor", "转子"),
            shape("foil", "Foil", "箔"),
            shape("tiny_plate", "Tiny Plate", "微型板"),
            shape("fine_wire", "Fine Wire", "细导线"));
    private static final Map<String, Definition> BY_ID = DEFINITIONS.stream()
            .collect(Collectors.toUnmodifiableMap(Definition::id, Function.identity()));

    private ExtruderShapeCatalog() {}

    public static Map<String, DeferredItem<Item>> registerAll(DeferredRegister.Items items) {
        return DEFINITIONS.stream().collect(Collectors.toUnmodifiableMap(
                Definition::id,
                definition -> items.register(
                        definition.registryPath(),
                        () -> new ExtruderShapeItem(
                                definition,
                                new Item.Properties().stacksTo(MAX_STACK_SIZE)))));
    }

    public static Definition require(String id) {
        Definition definition = BY_ID.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown extruder shape " + id);
        }
        return definition;
    }

    public static boolean isShape(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.is(ModItemTags.EXTRUDER_SHAPES)
                || stack.getItem() instanceof ExtruderShapeItem shape
                && BY_ID.get(shape.definition().id()) == shape.definition();
    }

    private static Definition shape(String id, String english, String chinese) {
        return new Definition(
                id,
                "extruder_shape_" + id,
                "Extruder Shape (" + english + ")",
                "挤压模具（" + chinese + "）");
    }

    public record Definition(
            String id,
            String registryPath,
            String englishName,
            String chineseName) {}

    public static final class ExtruderShapeItem extends Item {
        private final Definition definition;

        public ExtruderShapeItem(Definition definition, Properties properties) {
            super(properties);
            this.definition = definition;
        }

        public Definition definition() {
            return definition;
        }
    }
}
