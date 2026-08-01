package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.MaterialDefinition;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class MaterialItem extends Item {
    private final String materialId;
    private final MaterialPrefix form;

    public MaterialItem(MaterialDefinition material, MaterialPrefix form, Properties properties) {
        super(properties);
        this.materialId = material.id();
        this.form = form;
    }

    public MaterialDefinition material() {
        return MaterialCatalog.require(materialId);
    }

    public String materialId() {
        return materialId;
    }

    public MaterialPrefix form() {
        return form;
    }

    public int units() {
        return form.units();
    }

    @Override
    public Component getName(ItemStack stack) {
        MaterialDefinition material = material();
        Component materialName = material.nameKey()
                .<Component>map(Component::translatable)
                .orElseGet(() -> Component.literal(title(material.id())));
        return Component.translatable(
                "item.cruciblecraft.material_form." + form.serializedName(),
                materialName);
    }

    private static String title(String id) {
        String spaced = id.replace('_', ' ');
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }
}
