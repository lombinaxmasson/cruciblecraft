package com.masson.cruciblecraft.crops;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CropRegistries {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(CrucibleCraftCrops.MODID);
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(CrucibleCraftCrops.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(
                    net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE,
                    CrucibleCraftCrops.MODID);

    public static final DeferredBlock<CropStickBlock> CROP_STICK = BLOCKS.register(
            "crop_stick",
            CropStickBlock::new);
    public static final DeferredItem<BlockItem> CROP_STICK_ITEM = ITEMS.register(
            "crop_stick",
            () -> new BlockItem(CROP_STICK.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CropBlockEntity>>
            CROP_ENTITY = BLOCK_ENTITIES.register(
                    "crop_stick",
                    () -> BlockEntityType.Builder.of(
                            CropBlockEntity::new, CROP_STICK.get()).build(null));

    private static final Map<String, Boolean> OVERLAY = new LinkedHashMap<>();

    private CropRegistries() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        CropCreativeTabs.TABS.register(bus);
    }

    static void recordOverlay(String material, String prefix) {
        OVERLAY.put(material + "/" + prefix, true);
    }

    static Collection<String> overlayPairs() {
        return OVERLAY.keySet();
    }
}
