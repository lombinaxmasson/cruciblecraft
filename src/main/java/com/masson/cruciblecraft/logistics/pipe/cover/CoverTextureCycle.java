package com.masson.cruciblecraft.logistics.pipe.cover;

import java.util.ArrayList;
import java.util.List;

import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverKinds;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * GT6 {@code CoverTextureMulti} chisel cycle. Blank/crafting/warning use
 * numbered overlay skins; plates cycle extra material-form item textures.
 */
public final class CoverTextureCycle {
    public static final int BLANK_SKINS = 6;
    public static final int CRAFTING_SKINS = 6;
    public static final int WARNING_SKINS = 20;
    private static final List<ItemStack> STONE_SKINS = List.of(
            new ItemStack(Blocks.STONE),
            new ItemStack(Blocks.COBBLESTONE),
            new ItemStack(Blocks.MOSSY_COBBLESTONE),
            new ItemStack(Blocks.STONE_BRICKS),
            new ItemStack(Blocks.MOSSY_STONE_BRICKS),
            new ItemStack(Blocks.CRACKED_STONE_BRICKS),
            new ItemStack(Blocks.CHISELED_STONE_BRICKS),
            new ItemStack(Blocks.SMOOTH_STONE),
            new ItemStack(Blocks.SMOOTH_STONE_SLAB));

    private CoverTextureCycle() {}

    public static int skinCount(PipeCover cover) {
        if (cover == null) {
            return 1;
        }
        String path = cover.definitionId().getPath();
        if (MachineCoverKinds.isBlank(cover.definitionId())) {
            return BLANK_SKINS;
        }
        if ("cover_crafting".equals(path)) {
            return CRAFTING_SKINS;
        }
        if ("cover_warning".equals(path)) {
            return WARNING_SKINS;
        }
        if (PlateCovers.isPlate(cover)) {
            return Math.max(1, plateSkins(cover).size());
        }
        return 1;
    }

    public static boolean cycles(PipeCover cover) {
        return skinCount(cover) > 1;
    }

    public static PipeCover cycle(PipeCover cover) {
        int count = skinCount(cover);
        if (cover == null || count <= 1) {
            return cover;
        }
        int next = Math.floorMod(cover.config().visual() + 1, count);
        return cover.withDisplay(next, cover.config().redstone());
    }

    public static ItemStack plateSkin(PipeCover cover) {
        if (!PlateCovers.isPlate(cover)) {
            return ItemStack.EMPTY;
        }
        List<ItemStack> skins = plateSkins(cover);
        if (skins.isEmpty()) {
            return PlateCovers.stackFor(cover);
        }
        return skins.get(Math.floorMod(cover.config().visual(), skins.size()))
                .copy();
    }

    public static boolean paperPlate(PipeCover cover) {
        if (!PlateCovers.isPlate(cover)) {
            return false;
        }
        return cover.config().matchId()
                .map(id -> id.contains(":paper/") || id.endsWith("/paper"))
                .orElse(false);
    }

    private static List<ItemStack> plateSkins(PipeCover cover) {
        ItemStack live = PlateCovers.stackFor(cover);
        if (live.isEmpty()) {
            return List.of();
        }
        MaterialUnits.Entry entry = MaterialUnits.resolve(live).orElse(null);
        if (entry == null) {
            return List.of(live);
        }
        if ("stone".equals(entry.materialId())) {
            List<ItemStack> stone = new ArrayList<>();
            stone.add(live);
            stone.addAll(STONE_SKINS);
            return List.copyOf(stone);
        }
        ArrayList<ItemStack> skins = new ArrayList<>();
        skins.add(live);
        for (MaterialPrefix form : extraForms(entry.form())) {
            if (form.equals(entry.form())) {
                continue;
            }
            MaterialLookup.tryStack(entry.materialId(), form, 1)
                    .filter(stack -> !stack.isEmpty())
                    .ifPresent(skins::add);
        }
        return List.copyOf(skins);
    }

    private static List<MaterialPrefix> extraForms(MaterialPrefix form) {
        if (form == null || form.equals(MaterialPrefixes.FOIL)) {
            return List.of();
        }
        if (form.equals(MaterialPrefixes.PLATE_GEM)) {
            return List.of(
                    MaterialPrefixes.GEM,
                    MaterialPrefixes.PLATE_GEM,
                    MaterialPrefixes.DUST,
                    MaterialPrefixes.RAW_ORE);
        }
        return List.of(
                MaterialPrefixes.BLOCK,
                MaterialPrefixes.PLATE,
                MaterialPrefixes.INGOT,
                MaterialPrefixes.MACHINE_CASING,
                MaterialPrefixes.DUST,
                MaterialPrefixes.RAW_ORE);
    }

    public static boolean isStoneSkin(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return "minecraft".equals(id.getNamespace());
    }
}
