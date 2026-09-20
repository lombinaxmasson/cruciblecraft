package com.masson.cruciblecraft.logistics.pipe.cover;

import java.util.Set;

import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.material.MaterialCatalog;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * GT6 {@code CoverTextureSimple}/{@code CoverTextureMulti}: OreDict plates
 * and foils attach as decorative covers. The live material stack is the
 * cover item — no extra registry id.
 */
public final class PlateCovers {
    public static final ResourceLocation DEFINITION_ID =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "cover_plate");
    public static final Set<MaterialPrefix> FORMS = Set.of(
            MaterialPrefixes.PLATE,
            MaterialPrefixes.FOIL,
            MaterialPrefixes.DOUBLE_PLATE,
            MaterialPrefixes.TRIPLE_PLATE,
            MaterialPrefixes.QUADRUPLE_PLATE,
            MaterialPrefixes.QUINTUPLE_PLATE,
            MaterialPrefixes.DENSE_PLATE,
            MaterialPrefixes.CURVED_PLATE,
            MaterialPrefixes.PLATE_GEM);

    private PlateCovers() {}

    public static boolean isPlate(ResourceLocation definitionId) {
        return DEFINITION_ID.equals(definitionId);
    }

    public static boolean isPlate(PipeCover cover) {
        return cover != null && isPlate(cover.definitionId());
    }

    public static boolean isCoverForm(MaterialPrefix form) {
        return form != null && FORMS.contains(form);
    }

    public static PipeCover fromItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        MaterialUnits.Entry entry = MaterialUnits.resolve(stack).orElse(null);
        if (entry == null || !isCoverForm(entry.form())) {
            return null;
        }
        String logical = MaterialCatalog.find(entry.materialId())
                .map(material -> MaterialLookup.logicalItemId(
                        material,
                        entry.form(),
                        MaterialCatalog.runtimePreferences()).toString())
                .orElse(null);
        if (logical == null
                || logical.length() > PipeCoverConfig.MAX_MATCH_ID_LENGTH) {
            return null;
        }
        return PipeCover.of(DEFINITION_ID)
                .withConfig(PipeCoverConfig.EMPTY.withMatchId(logical));
    }

    public static ItemStack stackFor(PipeCover cover) {
        if (!isPlate(cover)) {
            return ItemStack.EMPTY;
        }
        return cover.config().matchId()
                .flatMap(MaterialLookup::stackFromLogicalId)
                .orElse(ItemStack.EMPTY);
    }
}
