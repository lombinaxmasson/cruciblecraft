package com.masson.cruciblecraft.compat.emi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.material.MaterialFormHosts;
import com.masson.cruciblecraft.material.def.MaterialDefinition;

/**
 * EMI-free projection for the item-list polish layers: gated prefix
 * stacks (one row per live {@code prefix_material}), dust families
 * (dust / small_dust / tiny_dust of one material, 9 tiny = 4 small =
 * 1 dust) and the routed tool variants shared with the TOOLS creative tab.
 * The plugin maps these to {@code EmiRegistry.addEmiStack} /
 * {@code addAlias}; no EMI types appear here so JUnit can exercise the
 * projection without the compile-only EMI runtime.
 */
public final class EmiDisplayPlan {
    private EmiDisplayPlan() {}

    public record DustFamily(
            String materialId, String dust, String smallDust, String tinyDust) {}

    /** One family per material whose registration gate carries all three
     *  dust forms. Ids stay logical {@code cruciblecraft:{material}/{form}}
     *  so EMI can build component stacks; they are not live Item registry ids. */
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

    /**
     * Logical {@code cruciblecraft:{material}/{form}} ids for gated shared
     * inventory stacks. Unique hosted pipes/cables/storage and
     * {@code formItems()} externals stay out so EMI does not index a naked
     * prefix Item as "any dust".
     */
    public static List<String> gatedPrefixStacks(
            Collection<MaterialDefinition> materials,
            Map<String, List<MaterialPrefix>> registeredForms) {
        List<String> ids = new ArrayList<>();
        for (MaterialDefinition material : materials) {
            List<MaterialPrefix> forms = registeredForms.get(material.id());
            if (forms == null) {
                continue;
            }
            for (MaterialPrefix form : forms) {
                if (material.formItems().containsKey(form)
                        || MaterialFormHosts.isUniqueHostedPrefixPath(
                                form.serializedName())) {
                    continue;
                }
                ids.add(itemId(material, form));
            }
        }
        return List.copyOf(ids);
    }

    private static String itemId(
            MaterialDefinition material, MaterialPrefix prefix) {
        return material.formItems().getOrDefault(
                prefix,
                "cruciblecraft:" + material.registryName(prefix));
    }
}
