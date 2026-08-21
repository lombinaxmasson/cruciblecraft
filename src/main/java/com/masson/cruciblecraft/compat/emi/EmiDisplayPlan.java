package com.masson.cruciblecraft.compat.emi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.material.def.MaterialDefinition;

/**
 * EMI-free projection for the item-list polish layers: dust families
 * (dust / small_dust / tiny_dust of one material, 9 tiny = 4 small =
 * 1 dust) and the routed tool variants shared with the TOOLS creative tab.
 * The plugin maps these to {@code EmiRegistry.addAlias} /
 * {@code addEmiStack}; no EMI types appear here so JUnit can exercise the
 * projection without the compile-only EMI runtime.
 */
public final class EmiDisplayPlan {
    private EmiDisplayPlan() {}

    public record DustFamily(
            String materialId, String dust, String smallDust, String tinyDust) {}

    /** One family per material whose registration gate carries all three
     *  dust forms; item ids resolve like the creative-tab plan
     *  ({@code formItems} override else the material's registry name). */
    public static List<DustFamily> dustFamilies(
            Collection<MaterialDefinition> materials,
            Map<String, List<MaterialPrefix>> registeredForms) {
        List<DustFamily> families = new ArrayList<>();
        for (MaterialDefinition material : materials) {
            List<MaterialPrefix> forms = registeredForms.get(material.id());
            if (forms == null
                    || !forms.contains(MaterialPrefixes.DUST)
                    || !forms.contains(MaterialPrefixes.SMALL_DUST)
                    || !forms.contains(MaterialPrefixes.TINY_DUST)) {
                continue;
            }
            families.add(new DustFamily(
                    material.id(),
                    itemId(material, MaterialPrefixes.DUST),
                    itemId(material, MaterialPrefixes.SMALL_DUST),
                    itemId(material, MaterialPrefixes.TINY_DUST)));
        }
        return List.copyOf(families);
    }

    private static String itemId(
            MaterialDefinition material, MaterialPrefix prefix) {
        return material.formItems().getOrDefault(
                prefix,
                "cruciblecraft:" + material.registryName(prefix));
    }
}
