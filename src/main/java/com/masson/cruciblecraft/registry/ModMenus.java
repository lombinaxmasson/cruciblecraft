package com.masson.cruciblecraft.registry;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.menu.CokeOvenMenu;
import com.masson.cruciblecraft.content.menu.CrusherMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, CrucibleCraft.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<CokeOvenMenu>> COKE_OVEN =
            MENUS.register(
                    "coke_oven",
                    () -> new MenuType<>(CokeOvenMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<CrusherMenu>> CRUSHER =
            MENUS.register(
                    "bronze_crusher",
                    () -> new MenuType<>(CrusherMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private ModMenus() {}
}
