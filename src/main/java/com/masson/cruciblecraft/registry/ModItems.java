package com.masson.cruciblecraft.registry;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialForm;
import com.masson.cruciblecraft.content.item.MaterialItem;
import com.masson.cruciblecraft.content.item.CeramicMoldBlockItem;
import com.masson.cruciblecraft.content.item.MaterialMachineBlockItem;
import com.masson.cruciblecraft.content.item.SmithingHammerItem;
import com.masson.cruciblecraft.content.item.UnknownMaterialItem;
import com.masson.cruciblecraft.machine.MachineMaterialRules.Device;
import com.masson.cruciblecraft.content.mold.MoldShape;
import com.masson.cruciblecraft.material.def.MaterialDefinition;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CrucibleCraft.MODID);
    private static final Map<String, DeferredItem<MaterialItem>> MATERIAL_ITEMS = new LinkedHashMap<>();

    public static final DeferredItem<BlockItem> FIREBRICK = ITEMS.registerSimpleBlockItem("firebrick", ModBlocks.FIREBRICK);
    public static final DeferredItem<BlockItem> FIREBOX = ITEMS.registerSimpleBlockItem("firebox", ModBlocks.FIREBOX);
    public static final DeferredItem<MaterialMachineBlockItem> CRUCIBLE = ITEMS.register(
            "crucible",
            () -> new MaterialMachineBlockItem(
                    ModBlocks.CRUCIBLE.get(),
                    Device.CRUCIBLE,
                    new Item.Properties()));
    public static final DeferredItem<MaterialMachineBlockItem> ANVIL = ITEMS.register(
            "anvil",
            () -> new MaterialMachineBlockItem(
                    ModBlocks.ANVIL.get(),
                    Device.ANVIL,
                    new Item.Properties()));
    public static final DeferredItem<BlockItem> COKE_OVEN =
            ITEMS.registerSimpleBlockItem("coke_oven", ModBlocks.COKE_OVEN);

    public static final DeferredItem<Item> RAW_CERAMIC_CRUCIBLE =
            ITEMS.registerSimpleItem("raw_ceramic_crucible", new Item.Properties());
    public static final DeferredItem<Item> RAW_CERAMIC_MOLD =
            ITEMS.registerSimpleItem("raw_ceramic_mold", new Item.Properties());
    public static final DeferredItem<Item> RAW_INGOT_MOLD =
            ITEMS.registerSimpleItem("raw_ingot_mold", new Item.Properties());
    public static final DeferredItem<Item> RAW_PLATE_MOLD =
            ITEMS.registerSimpleItem("raw_plate_mold", new Item.Properties());
    public static final DeferredItem<Item> RAW_ROD_MOLD =
            ITEMS.registerSimpleItem("raw_rod_mold", new Item.Properties());
    public static final DeferredItem<Item> RAW_BOLT_MOLD =
            ITEMS.registerSimpleItem("raw_bolt_mold", new Item.Properties());
    public static final DeferredItem<CeramicMoldBlockItem> INGOT_MOLD = mold("ingot_mold", MoldShape.INGOT);
    public static final DeferredItem<CeramicMoldBlockItem> PLATE_MOLD = mold("plate_mold", MoldShape.PLATE);
    public static final DeferredItem<CeramicMoldBlockItem> ROD_MOLD = mold("rod_mold", MoldShape.ROD);
    public static final DeferredItem<CeramicMoldBlockItem> BOLT_MOLD = mold("bolt_mold", MoldShape.BOLT);
    public static final DeferredItem<Item> COAL_COKE =
            ITEMS.registerSimpleItem("coal_coke", new Item.Properties());
    public static final DeferredItem<BlockItem> BELLOWS =
            ITEMS.registerSimpleBlockItem("bellows", ModBlocks.BELLOWS);
    public static final DeferredItem<BucketItem> CREOSOTE_BUCKET = ITEMS.register(
            "creosote_bucket",
            () -> new BucketItem(
                    ModFluids.CREOSOTE_SOURCE.get(),
                    new Item.Properties()
                            .craftRemainder(Items.BUCKET)
                            .stacksTo(1)));
    public static final DeferredItem<BucketItem> STEAM_BUCKET = ITEMS.register(
            "steam_bucket",
            () -> new BucketItem(
                    ModFluids.STEAM_SOURCE.get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
    public static final DeferredItem<BlockItem> BRONZE_BOILER =
            ITEMS.registerSimpleBlockItem("bronze_boiler", ModBlocks.BRONZE_BOILER);
    public static final DeferredItem<BlockItem> BRONZE_STEAM_ENGINE =
            ITEMS.registerSimpleBlockItem("bronze_steam_engine", ModBlocks.BRONZE_STEAM_ENGINE);
    public static final DeferredItem<BlockItem> BRONZE_CRUSHER =
            ITEMS.registerSimpleBlockItem("bronze_crusher", ModBlocks.BRONZE_CRUSHER);
    public static final DeferredItem<SmithingHammerItem> SMITHING_HAMMER =
            ITEMS.register(
                    "smithing_hammer",
                    () -> new SmithingHammerItem(new Item.Properties()
                            .durability(com.masson.cruciblecraft.machine.MachineMaterialRules
                                    .IRON_HAMMER_DURABILITY)));
    public static final DeferredItem<UnknownMaterialItem> UNKNOWN_MATERIAL =
            ITEMS.register("unknown_material", () -> new UnknownMaterialItem(new Item.Properties()));
    public static final DeferredItem<BlockItem> COPPER_ORE = oreItem("copper_ore", ModBlocks.COPPER_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_COPPER_ORE = oreItem("deepslate_copper_ore", ModBlocks.DEEPSLATE_COPPER_ORE);
    public static final DeferredItem<BlockItem> TIN_ORE = oreItem("tin_ore", ModBlocks.TIN_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_TIN_ORE = oreItem("deepslate_tin_ore", ModBlocks.DEEPSLATE_TIN_ORE);
    public static final DeferredItem<BlockItem> IRON_ORE = oreItem("iron_ore", ModBlocks.IRON_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_IRON_ORE = oreItem("deepslate_iron_ore", ModBlocks.DEEPSLATE_IRON_ORE);
    public static final DeferredItem<BlockItem> GOLD_ORE = oreItem("gold_ore", ModBlocks.GOLD_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_GOLD_ORE = oreItem("deepslate_gold_ore", ModBlocks.DEEPSLATE_GOLD_ORE);
    public static final DeferredItem<BlockItem> ZINC_ORE = oreItem("zinc_ore", ModBlocks.ZINC_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_ZINC_ORE = oreItem("deepslate_zinc_ore", ModBlocks.DEEPSLATE_ZINC_ORE);
    public static final DeferredItem<BlockItem> LEAD_ORE = oreItem("lead_ore", ModBlocks.LEAD_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_LEAD_ORE = oreItem("deepslate_lead_ore", ModBlocks.DEEPSLATE_LEAD_ORE);
    public static final DeferredItem<BlockItem> NICKEL_ORE = oreItem("nickel_ore", ModBlocks.NICKEL_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_NICKEL_ORE = oreItem("deepslate_nickel_ore", ModBlocks.DEEPSLATE_NICKEL_ORE);

    public static void registerMaterials(Collection<MaterialDefinition> definitions) {
        if (!MATERIAL_ITEMS.isEmpty()) {
            throw new IllegalStateException("Material items already registered");
        }
        for (MaterialDefinition material : definitions) {
            for (MaterialForm form : material.forms()) {
                if (material.formItems().containsKey(form)) {
                    continue;
                }
                String registryName = material.registryName(form);
                MATERIAL_ITEMS.put(
                        key(material.id(), form),
                        ITEMS.register(
                                registryName,
                                () -> new MaterialItem(material, form, new Item.Properties())));
            }
        }
    }

    public static DeferredItem<MaterialItem> materialItem(String materialId, MaterialForm form) {
        DeferredItem<MaterialItem> item = MATERIAL_ITEMS.get(key(materialId, form));
        if (item == null) {
            throw new IllegalArgumentException("No " + form.serializedName() + " for material " + materialId);
        }
        return item;
    }

    public static boolean hasMaterialItem(String materialId, MaterialForm form) {
        return MATERIAL_ITEMS.containsKey(key(materialId, form));
    }

    public static Collection<DeferredItem<MaterialItem>> materialItems() {
        return MATERIAL_ITEMS.values();
    }

    public static DeferredItem<CeramicMoldBlockItem> moldItem(MoldShape shape) {
        return switch (shape) {
            case INGOT -> INGOT_MOLD;
            case PLATE -> PLATE_MOLD;
            case ROD -> ROD_MOLD;
            case BOLT -> BOLT_MOLD;
        };
    }

    private static DeferredItem<CeramicMoldBlockItem> mold(String id, MoldShape shape) {
        return ITEMS.register(
                id,
                () -> new CeramicMoldBlockItem(
                        ModBlocks.CERAMIC_MOLD.get(),
                        shape,
                        new Item.Properties().stacksTo(1)));
    }

    private static DeferredItem<BlockItem> oreItem(
            String id,
            net.neoforged.neoforge.registries.DeferredBlock<? extends net.minecraft.world.level.block.Block> block) {
        return ITEMS.registerSimpleBlockItem(id, block);
    }

    private static String key(String materialId, MaterialForm form) {
        return materialId + "/" + form.serializedName();
    }

    private ModItems() {}
}
