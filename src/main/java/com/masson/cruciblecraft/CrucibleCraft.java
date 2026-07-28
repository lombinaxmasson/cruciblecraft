package com.masson.cruciblecraft;

import org.slf4j.Logger;

import com.masson.cruciblecraft.compat.kubejs.KubeJSCompat;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.gen.GeneratedMaterialPack;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModComponents;
import com.masson.cruciblecraft.registry.ModCapabilities;
import com.masson.cruciblecraft.registry.ModCreativeTabs;
import com.masson.cruciblecraft.registry.ModFluids;
import com.masson.cruciblecraft.registry.ModFeatures;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.registry.ModMenus;
import com.masson.cruciblecraft.registry.ModRecipes;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.fml.loading.FMLPaths;

@Mod(CrucibleCraft.MODID)
public class CrucibleCraft {
    public static final String MODID = "cruciblecraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CrucibleCraft(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::construct);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(GeneratedMaterialPack::addPackFinders);
        modEventBus.addListener(ModCapabilities::register);

        ModFluids.FLUID_TYPES.register(modEventBus);
        ModFluids.FLUIDS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModFeatures.FEATURES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModComponents.COMPONENTS.register(modEventBus);
        ModRecipes.RECIPE_TYPES.register(modEventBus);
        ModRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.CLIENT, com.masson.cruciblecraft.config.ModConfig.CLIENT_SPEC);
    }

    private void construct(FMLConstructModEvent event) {
        var configRoot = FMLPaths.CONFIGDIR.get().resolve(MODID);
        if (ModList.get().isLoaded("kubejs")) {
            KubeJSCompat.fireMaterialRegistration();
        }
        MaterialCatalog.bootstrap(configRoot.resolve("materials"));
        ModFluids.registerMaterials(MaterialCatalog.values());
        ModItems.registerMaterials(MaterialCatalog.values());
        GeneratedMaterialPack.initialize(configRoot);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModFluids::finalizeMaterialLookup);
        LOGGER.info("CrucibleCraft common setup");
    }
}
