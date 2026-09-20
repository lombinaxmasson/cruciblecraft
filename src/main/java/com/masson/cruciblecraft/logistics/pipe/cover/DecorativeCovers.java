package com.masson.cruciblecraft.logistics.pipe.cover;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/**
 * GT6 {@code CoverRegistry} decorative stacks that are not OreDict plates:
 * asphalt (walk 1.3×), dyed concrete/cfoam panels, and plank covers.
 */
public final class DecorativeCovers {
    public static final ResourceLocation ASPHALT_ID =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "cover_asphalt");
    public static final ResourceLocation WOOD_ID =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "cover_wood");
    public static final ResourceLocation PANEL_ID =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "cover_panel");

    private DecorativeCovers() {}

    public static boolean isDecorative(ResourceLocation definitionId) {
        return ASPHALT_ID.equals(definitionId)
                || WOOD_ID.equals(definitionId)
                || PANEL_ID.equals(definitionId);
    }

    public static boolean isDecorative(PipeCover cover) {
        return cover != null && isDecorative(cover.definitionId());
    }

    public static boolean isAsphalt(PipeCover cover) {
        return cover != null && ASPHALT_ID.equals(cover.definitionId());
    }

    public static boolean isWood(PipeCover cover) {
        return cover != null && WOOD_ID.equals(cover.definitionId());
    }

    public static PipeCover fromItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null || id.toString().length() > PipeCoverConfig.MAX_MATCH_ID_LENGTH) {
            return null;
        }
        ResourceLocation definition = definitionFor(stack, id);
        if (definition == null) {
            return null;
        }
        return PipeCover.of(definition)
                .withConfig(PipeCoverConfig.EMPTY.withMatchId(id.toString()));
    }

    public static ItemStack stackFor(PipeCover cover) {
        if (!isDecorative(cover)) {
            return ItemStack.EMPTY;
        }
        return cover.config().matchId()
                .map(ResourceLocation::tryParse)
                .filter(id -> id != null)
                .map(id -> BuiltInRegistries.ITEM.get(id))
                .map(ItemStack::new)
                .orElse(ItemStack.EMPTY);
    }

    private static ResourceLocation definitionFor(
            ItemStack stack, ResourceLocation id) {
        if ("cruciblecraft".equals(id.getNamespace())) {
            String path = id.getPath();
            if (path.startsWith("panel/asphalt_")) {
                return ASPHALT_ID;
            }
            if (path.startsWith("panel/cfoam_")
                    || path.startsWith("panel/concrete_")) {
                return PANEL_ID;
            }
            if (path.startsWith("panel/plank_")
                    || path.startsWith("plank/")
                    || path.contains("/plank")) {
                return WOOD_ID;
            }
        }
        if (stack.getItem() instanceof BlockItem
                && stack.is(ItemTags.PLANKS)) {
            return WOOD_ID;
        }
        return null;
    }
}
