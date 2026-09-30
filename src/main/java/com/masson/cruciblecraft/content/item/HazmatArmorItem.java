package com.masson.cruciblecraft.content.item;

import java.util.Objects;

import com.masson.cruciblecraft.registry.ModArmorMaterials;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

/** Wearable GT6 radiation or heat hazmat piece. Full set required for immunity. */
public final class HazmatArmorItem extends ArmorItem {
    public enum Kind {
        RADIATION,
        HEAT
    }

    private final Kind kind;
    private final String englishName;
    private final String chineseName;

    public HazmatArmorItem(
            Holder<ArmorMaterial> material,
            Type type,
            Properties properties,
            Kind kind,
            String englishName,
            String chineseName) {
        super(material, type, properties);
        this.kind = Objects.requireNonNull(kind, "kind");
        this.englishName = Objects.requireNonNull(englishName, "englishName");
        this.chineseName = Objects.requireNonNull(chineseName, "chineseName");
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public Component getName(ItemStack stack) {
        return CatalogDisplayNames.itemName(
                getDescriptionId(stack), englishName, chineseName);
    }

    public static boolean isWearingFull(LivingEntity entity, Kind kind) {
        for (EquipmentSlot slot : new EquipmentSlot[] {
                EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET}) {
            if (!(entity.getItemBySlot(slot).getItem() instanceof HazmatArmorItem armor)
                    || armor.kind() != kind) {
                return false;
            }
        }
        return true;
    }

    public static HazmatArmorItem fromIdentity(
            SemanticObjectCatalog.Identity identity, Properties properties) {
        String path = identity.registryPath();
        Kind kind = path.startsWith("heat/") || path.contains("hazmat_heat")
                ? Kind.HEAT
                : Kind.RADIATION;
        Type type = typeOf(path);
        Holder<ArmorMaterial> material = kind == Kind.RADIATION
                ? ModArmorMaterials.RADIATION_HAZMAT
                : ModArmorMaterials.HEAT_HAZMAT;
        return new HazmatArmorItem(
                material,
                type,
                properties.durability(type.getDurability(8)),
                kind,
                identity.englishName(),
                identity.chineseName());
    }

    public static boolean isHazmatPath(String registryPath) {
        return registryPath.startsWith("radiation/hazard_suit_")
                || registryPath.startsWith("heat/protection_suit_")
                || registryPath.contains("gt_armor_hazmat_radiation")
                || registryPath.contains("gt_armor_hazmat_heat");
    }

    private static Type typeOf(String path) {
        if (path.endsWith("_helmet") || path.contains("_head_")) {
            return Type.HELMET;
        }
        if (path.endsWith("_shirt") || path.contains("_chest_")) {
            return Type.CHESTPLATE;
        }
        if (path.endsWith("_pants") || path.contains("_legs_")) {
            return Type.LEGGINGS;
        }
        if (path.endsWith("_boots")) {
            return Type.BOOTS;
        }
        throw new IllegalArgumentException("Unknown hazmat piece " + path);
    }
}
