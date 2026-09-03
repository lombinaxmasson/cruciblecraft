package com.masson.cruciblecraft.client.color;

import java.util.ArrayList;
import java.util.List;

import com.masson.cruciblecraft.content.block.GtBlockObjectBarsBlock;
import com.masson.cruciblecraft.content.block.GtBlockObjectBaleBlock;
import com.masson.cruciblecraft.content.block.GtBlockObjectBlock;
import com.masson.cruciblecraft.content.block.GtBlockObjectCFoamFreshBlock;
import com.masson.cruciblecraft.content.block.GtBlockObjectLogBlock;
import com.masson.cruciblecraft.content.block.GtBlockObjectRailBlock;
import com.masson.cruciblecraft.content.block.GtBlockObjectSlabBlock;
import com.masson.cruciblecraft.content.block.GtBlockObjectSpikeBlock;
import com.masson.cruciblecraft.content.item.BathRemainderBlockObjectCatalog;
import com.masson.cruciblecraft.content.item.BlockArtIndex;
import com.masson.cruciblecraft.content.item.GtBlockObjectCatalog;
import com.masson.cruciblecraft.content.item.SemanticObjectCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
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
            GtBlockObjectCatalog.Variant variant = variantOf(block);
            if (variant != null) {
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
        GtBlockObjectCatalog.Variant variant = variantOf(block);
        if (variant == null || !BlockArtIndex.dyeTint(variant.registryPath())) {
            return 0xFFFFFFFF;
        }
        return 0xFF000000 | BlockArtIndex.dyeColor(variant.meta());
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
