package com.masson.cruciblecraft.content.item;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.masson.cruciblecraft.registry.ModItemTags;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * GT6 {@code Shape_Extruder} 10000–10031. Low-heat {@code Shape_SimpleEx}
 * 10200–10231 stay leftover identities in {@link SemanticObjectCatalog}.
 */
public final class ExtruderShapeCatalog {
    public static final int MAX_STACK_SIZE = 1;
    public static final int EMPTY_META = 10000;
    public static final int SIMPLE_EX_EMPTY_META = 10200;
    public static final List<Definition> DEFINITIONS = List.of(
            empty("empty", EMPTY_META, "Empty Extruder Shape", "空挤出模具"),
            shape("plate", 10001, "Plate", "板"),
            shape("long_rod", 10002, "Long Rod", "长杆"),
            shape("bolt", 10003, "Bolt", "螺栓"),
            shape("ring", 10004, "Ring", "环"),
            shape("cell", 10005, "Cell", "容器"),
            shape("ingot", 10006, "Ingot", "锭"),
            shape("wire", 10007, "Wire", "导线"),
            shape("small_item_casing", 10008, "Casing", "物品外壳"),
            shape("tiny_pipe", 10009, "Tiny Pipe", "微型管道"),
            shape("small_pipe", 10010, "Small Pipe", "小型管道"),
            shape("normal_pipe", 10011, "Normal Pipe", "普通管道"),
            shape("large_pipe", 10012, "Large Pipe", "大型管道"),
            shape("huge_pipe", 10013, "Huge Pipe", "巨型管道"),
            shape("block", 10014, "Block", "方块"),
            shape("sword_blade", 10015, "Sword Blade", "剑刃"),
            shape("pickaxe_head", 10016, "Pickaxe Head", "镐头"),
            shape("shovel_head", 10017, "Shovel Head", "铲头"),
            shape("axe_head", 10018, "Axe Head", "斧头"),
            shape("hoe_head", 10019, "Hoe Head", "锄头"),
            shape("hammer_head", 10020, "Hammer Head", "锤头"),
            shape("file_head", 10021, "File Head", "锉头"),
            shape("saw_blade", 10022, "Saw Blade", "锯片"),
            shape("gear", 10023, "Gear", "齿轮"),
            shape("bottle", 10024, "Bottle", "瓶"),
            shape("curved_plate", 10025, "Curved Plate", "弯曲板"),
            shape("small_gear", 10026, "Small Gear", "小齿轮"),
            shape("rod", 10027, "Rod", "杆"),
            shape("ccc", 10028, "Capsule-Cell-Container", "胶囊单元容器"),
            shape("foil", 10029, "Foil", "箔"),
            shape("tiny_plate", 10030, "Tiny Plate", "微型板"),
            shape("fine_wire", 10031, "Fine Wire", "细导线"));
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
                || stack.getItem() instanceof ExtruderShapeItem;
    }

    public static boolean isSimpleExIdentity(SemanticObjectCatalog.Identity identity) {
        return identity.meta() >= SIMPLE_EX_EMPTY_META
                && identity.meta() <= SIMPLE_EX_EMPTY_META + 31
                && "gregtech:gt.multiitem.technological".equals(identity.sourceItem());
    }

    private static Definition empty(
            String id, int meta, String english, String chinese) {
        return new Definition(id, "extruder_shape_" + id, meta, english, chinese);
    }

    private static Definition shape(
            String id, int meta, String english, String chinese) {
        return new Definition(
                id,
                "extruder_shape_" + id,
                meta,
                "Extruder Shape (" + english + ")",
                "挤出模具（" + chinese + "）");
    }

    public record Definition(
            String id,
            String registryPath,
            int gt6Meta,
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

        @Override
        public Component getName(ItemStack stack) {
            return CatalogDisplayNames.itemName(
                    getDescriptionId(stack),
                    definition.englishName(),
                    definition.chineseName());
        }
    }
}
