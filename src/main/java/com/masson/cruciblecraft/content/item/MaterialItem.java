package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.api.material.MaterialForm;
import com.masson.cruciblecraft.material.def.MaterialDefinition;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class MaterialItem extends Item {
    public static final int INGOT_UNITS = 144;

    private final MaterialDefinition material;
    private final MaterialForm form;

    public MaterialItem(MaterialDefinition material, MaterialForm form, Properties properties) {
        super(properties);
        this.material = material;
        this.form = form;
    }

    public MaterialDefinition material() {
        return material;
    }

    public MaterialForm form() {
        return form;
    }

    public int units() {
        return form.units();
    }

    @Override
    public Component getName(ItemStack stack) {
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
