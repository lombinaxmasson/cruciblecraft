package com.masson.cruciblecraft.material;

/**
 * Item-owned contract for persisted material data components.
 *
 * <p>Implementations must apply the same material-domain rules used when the
 * item is consumed at runtime. CrucibleCraft items that carry a material
 * component but do not implement this contract are quarantined fail-closed.
 */
public interface MaterialComponentPolicy {
    String TOOL_MATERIAL_COMPONENT_ID = "cruciblecraft:tool_material";
    String MACHINE_MATERIAL_COMPONENT_ID = "cruciblecraft:machine_material";
    String PREFIX_MATERIAL_COMPONENT_ID = "cruciblecraft:prefix_material";
    String TOOL_COMPONENT_FORM = "component:tool_material";
    String MACHINE_COMPONENT_FORM = "component:machine_material";

    String materialComponentId();

    String missingMaterialForm();

    boolean isPersistedMaterialAllowed(String materialId);
}
