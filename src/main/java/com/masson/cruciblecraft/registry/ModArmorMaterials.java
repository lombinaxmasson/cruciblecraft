package com.masson.cruciblecraft.registry;

import java.util.EnumMap;
import java.util.List;

import com.masson.cruciblecraft.CrucibleCraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Radiation and heat hazmat armor materials. */
public final class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, CrucibleCraft.MODID);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> RADIATION_HAZMAT =
            ARMOR_MATERIALS.register(
                    "hazmat_radiation", () -> material("hazmat_radiation"));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> HEAT_HAZMAT =
            ARMOR_MATERIALS.register("hazmat_heat", () -> material("hazmat_heat"));

    private static ArmorMaterial material(String name) {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.BOOTS, 1);
        defense.put(ArmorItem.Type.LEGGINGS, 2);
        defense.put(ArmorItem.Type.CHESTPLATE, 3);
        defense.put(ArmorItem.Type.HELMET, 1);
        defense.put(ArmorItem.Type.BODY, 3);
        return new ArmorMaterial(
                defense,
                0,
                SoundEvents.ARMOR_EQUIP_LEATHER,
                () -> Ingredient.EMPTY,
                List.of(new ArmorMaterial.Layer(
                        ResourceLocation.fromNamespaceAndPath(
                                CrucibleCraft.MODID, name))),
                0.0F,
                0.0F);
    }

    private ModArmorMaterials() {}
}
