package com.masson.cruciblecraft.client.color;

import java.util.ArrayList;
import java.util.List;

import com.masson.cruciblecraft.content.block.GtBlockObjectBarsBlock;
import com.masson.cruciblecraft.content.block.GtBlockObjectBaleBlock;
import com.masson.cruciblecraft.content.block.GtBlockObjectBlock;
import com.masson.cruciblecraft.content.block.GtBlockObjectCFoamFreshBlock;
import com.masson.cruciblecraft.content.block.GtBlockObjectGlassBlock;
import com.masson.cruciblecraft.content.block.GtBlockObjectLogBlock;
import com.masson.cruciblecraft.content.block.GtBlockObjectRailBlock;
import com.masson.cruciblecraft.content.block.GtBlockObjectSlabBlock;
import com.masson.cruciblecraft.content.block.GtBlockObjectSpikeBlock;
import com.masson.cruciblecraft.content.block.GtDecorativePanelBlock;
import com.masson.cruciblecraft.content.item.BathRemainderBlockObjectCatalog;
import com.masson.cruciblecraft.content.item.BlockArtIndex;
import com.masson.cruciblecraft.content.item.GtBlockObjectCatalog;
import com.masson.cruciblecraft.content.item.GtBuildingBlockCatalog;
import com.masson.cruciblecraft.content.item.SemanticObjectCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** GT6 DYES_INT for asphalt / concrete / cfoam that share one SOURCE_BACKED PNG. */
public final class GtBlockDyeColor {
    private GtBlockDyeColor() {}

    public static BlockColor blockColor() {
        return (state, level, pos, tintIndex) -> {
            if (tintIndex != 0) {
                return 0xFFFFFFFF;
            }
            return colorFor(state.getBlock());
        };
    }

    public static ItemColor itemColor() {
        return (stack, tintIndex) -> {
            if (tintIndex != 0) {
                return 0xFFFFFFFF;
            }
            Block block = Block.byItem(stack.getItem());
            if (block instanceof GtDecorativePanelBlock
                    || variantOf(block) != null) {
                return colorFor(block);
            }
            return leftoverColor(stack.getItem());
        };
    }

    public static Block[] tintedBlocks() {
        List<Block> blocks = new ArrayList<>();
        addTinted(blocks, GtBlockObjectCatalog.variants(), ModBlocks.gtBlockObjectBlocksById());
        addTinted(
                blocks,
                BathRemainderBlockObjectCatalog.variants(),
                ModBlocks.bathRemainderBlockObjectBlocksById());
        addTinted(
                blocks,
                GtBuildingBlockCatalog.variants(),
                ModBlocks.gtBuildingBlockObjectBlocksById());
        addSpikes(blocks, GtBlockObjectCatalog.variants(), ModBlocks.gtBlockObjectBlocksById());
        blocks.addAll(ModBlocks.bathPanelBlocks());
        return blocks.toArray(Block[]::new);
    }

    public static Item[] tintedItems() {
        Block[] blocks = tintedBlocks();
        Item[] leftover = leftoverTintedItems();
        Item[] items = new Item[blocks.length + leftover.length];
        for (int index = 0; index < blocks.length; index++) {
            items[index] = blocks[index].asItem();
        }
        System.arraycopy(leftover, 0, items, blocks.length, leftover.length);
        return items;
    }

    public static Item[] panelItems() {
        List<Item> items = new ArrayList<>();
        ModItems.bathMteItemsById().forEach((id, holder) -> {
            String path = id.getPath();
            if (path.startsWith("panel/asphalt_")
                    || path.startsWith("panel/cfoam_")
                    || path.startsWith("panel/concrete_")) {
                items.add(holder.get());
            }
        });
        return items.toArray(Item[]::new);
    }

    public static ItemColor panelItemColor() {
        return (stack, tintIndex) -> {
            if (tintIndex != 0) {
                return 0xFFFFFFFF;
            }
            return 0xFF000000 | panelColor(BuiltInRegistries.ITEM.getKey(
                    stack.getItem()).getPath());
        };
    }

    static int panelColor(String registryPath) {
        String color = panelColorName(registryPath);
        return switch (color) {
            case "black" -> 0x202020;
            case "red" -> 0xFF0000;
            case "green" -> 0x00FF00;
            case "brown" -> 0x604000;
            case "blue" -> 0x0000FF;
            case "purple" -> 0x800080;
            case "cyan" -> 0x00FFFF;
            case "light_gray" -> 0xC0C0C0;
            case "gray" -> 0x808080;
            case "pink" -> 0xFFC0C0;
            case "lime" -> 0x80FF80;
            case "yellow" -> 0xFFFF00;
            case "light_blue" -> 0x8080FF;
            case "magenta" -> 0xFF00FF;
            case "orange" -> 0xFF8000;
            case "white" -> 0xFFFFFF;
            default -> 0xFFFFFF;
        };
    }

    private static String panelColorName(String registryPath) {
        for (String prefix : List.of(
                "panel/asphalt_", "panel/cfoam_", "panel/concrete_")) {
            if (registryPath.startsWith(prefix)) {
                return registryPath.substring(prefix.length());
            }
        }
        return "";
    }

    public static Item[] leftoverTintedItems() {
        List<Item> items = new ArrayList<>();
        for (SemanticObjectCatalog.Identity identity : SemanticObjectCatalog.identities()) {
            if (!BlockArtIndex.dyeTint(identity.registryPath())) {
                continue;
            }
            var holder = ModItems.semanticIdentityItemsById().get(identity.id());
            if (holder != null) {
                items.add(holder.get());
            }
        }
        return items.toArray(Item[]::new);
    }

    private static void addTinted(
            List<Block> blocks,
            List<GtBlockObjectCatalog.Variant> variants,
            java.util.Map<net.minecraft.resources.ResourceLocation,
                    net.neoforged.neoforge.registries.DeferredBlock<Block>> byId) {
        for (GtBlockObjectCatalog.Variant variant : variants) {
            if (!BlockArtIndex.dyeTint(variant.registryPath())) {
                continue;
            }
            var holder = byId.get(variant.id());
            if (holder != null) {
                blocks.add(holder.get());
            }
        }
    }

    private static int leftoverColor(Item item) {
        for (SemanticObjectCatalog.Identity identity : SemanticObjectCatalog.identities()) {
            if (!BlockArtIndex.dyeTint(identity.registryPath())) {
                continue;
            }
            var holder = ModItems.semanticIdentityItemsById().get(identity.id());
            if (holder != null && holder.get() == item) {
                return 0xFF000000 | BlockArtIndex.dyeColor(identity.meta());
            }
        }
        return 0xFFFFFFFF;
    }

    private static int colorFor(Block block) {
        if (block instanceof GtDecorativePanelBlock panel) {
            return 0xFF000000 | panelColor(panel.identity().registryPath());
        }
        GtBlockObjectCatalog.Variant variant = variantOf(block);
        if (variant == null) {
            return 0xFFFFFFFF;
        }
        if (variant.spike()) {
            return 0xFF000000 | spikeColor(variant);
        }
        if (!BlockArtIndex.dyeTint(variant.registryPath())) {
            return 0xFFFFFFFF;
        }
        return 0xFF000000 | BlockArtIndex.dyeColor(variant.meta());
    }

    static int spikeColor(GtBlockObjectCatalog.Variant variant) {
        boolean second = variant.meta() >= 8;
        String source = variant.sourceItem();
        if (source.endsWith("spikes.fancy")) {
            return second ? 0xDCDCFF : 0xFFE650;
        }
        if (source.endsWith("spikes.metal")) {
            return second ? 0x3C286E : 0xFF7F3F;
        }
        if (source.endsWith("spikes.sharp")) {
            return second ? 0xDCA0F0 : 0x828282;
        }
        if (source.endsWith("spikes.steel")) {
            return second ? 0x8C6464 : 0x64648C;
        }
        if (source.endsWith("spikes.super")) {
            return second ? 0xFFFFFF : 0x6464A0;
        }
        return 0xFFFFFF;
    }

    private static void addSpikes(
            List<Block> blocks,
            List<GtBlockObjectCatalog.Variant> variants,
            java.util.Map<net.minecraft.resources.ResourceLocation,
                    net.neoforged.neoforge.registries.DeferredBlock<Block>> byId) {
        for (GtBlockObjectCatalog.Variant variant : variants) {
            if (!variant.spike()) {
                continue;
            }
            var holder = byId.get(variant.id());
            if (holder != null) {
                blocks.add(holder.get());
            }
        }
    }

    private static GtBlockObjectCatalog.Variant variantOf(Block block) {
        if (block instanceof GtBlockObjectBlock typed) {
            return typed.variant();
        }
        if (block instanceof GtBlockObjectSlabBlock typed) {
            return typed.variant();
        }
        if (block instanceof GtBlockObjectCFoamFreshBlock typed) {
            return typed.variant();
        }
        if (block instanceof GtBlockObjectGlassBlock typed) {
            return typed.variant();
        }
        if (block instanceof GtBlockObjectLogBlock typed) {
            return typed.variant();
        }
        if (block instanceof GtBlockObjectBaleBlock typed) {
            return typed.variant();
        }
        if (block instanceof GtBlockObjectBarsBlock typed) {
            return typed.variant();
        }
        if (block instanceof GtBlockObjectRailBlock typed) {
            return typed.variant();
        }
        if (block instanceof GtBlockObjectSpikeBlock typed) {
            return typed.variant();
        }
        return null;
    }
}
