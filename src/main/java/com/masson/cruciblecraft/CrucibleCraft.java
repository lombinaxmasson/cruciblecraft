package com.masson.cruciblecraft;

import org.slf4j.Logger;

import com.masson.cruciblecraft.compat.kubejs.KubeJSCompat;
import com.masson.cruciblecraft.api.material.MaterialPrefixRegistrationEvent;
import com.masson.cruciblecraft.api.material.MaterialRegistrationEvent;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.MaterialStressFixture;
import com.masson.cruciblecraft.material.MissingMaterialStackCodec;
import com.masson.cruciblecraft.material.gen.GeneratedMaterialPack;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.masson.cruciblecraft.network.MaterialConfigurationHandshake;
import com.masson.cruciblecraft.network.CoverConfigurationPayload;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverBehaviorRegistry;
import com.masson.cruciblecraft.logistics.core.LogisticsDumpCovers;
import com.masson.cruciblecraft.logistics.displaycpu.DisplayCpuCovers;
import com.masson.cruciblecraft.logistics.fluidnet.FluidNetworkCovers;
import com.masson.cruciblecraft.logistics.genericnet.GenericNetworkCovers;
import com.masson.cruciblecraft.logistics.itemnet.ItemNetworkCovers;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverCovers;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModComponents;
import com.masson.cruciblecraft.registry.ModCapabilities;
import com.masson.cruciblecraft.registry.ModCreativeTabs;
import com.masson.cruciblecraft.registry.ModFluids;
import com.masson.cruciblecraft.registry.ModFeatures;
import com.masson.cruciblecraft.registry.ModIngredientTypes;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.registry.ModMenus;
import com.masson.cruciblecraft.registry.ModMultiblockPlugins;
import com.masson.cruciblecraft.registry.ModRecipes;
import com.masson.cruciblecraft.registry.ModStructures;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoader;
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
        ModMultiblockPlugins.register();
        modEventBus.addListener(this::construct);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(GeneratedMaterialPack::addPackFinders);
        modEventBus.addListener(ModCapabilities::register);
        modEventBus.addListener(MaterialConfigurationHandshake::registerPayloads);
        modEventBus.addListener(MaterialConfigurationHandshake::registerTask);
        modEventBus.addListener(CoverConfigurationPayload::register);
        modEventBus.addListener(
                com.masson.cruciblecraft.network.CompactFamilyRequestPayload::register);
        modEventBus.addListener(
                com.masson.cruciblecraft.network.CompactFamilySlicePayload::register);

        ModFluids.FLUID_TYPES.register(modEventBus);
        ModFluids.FLUIDS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModFeatures.FEATURES.register(modEventBus);
        ModStructures.STRUCTURE_TYPES.register(modEventBus);
        ModStructures.STRUCTURE_PIECES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        com.masson.cruciblecraft.registry.ModArmorMaterials.ARMOR_MATERIALS.register(
                modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModComponents.COMPONENTS.register(modEventBus);
        ModRecipes.RECIPE_TYPES.register(modEventBus);
        ModRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        ModIngredientTypes.INGREDIENT_TYPES.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.CLIENT, com.masson.cruciblecraft.config.ModConfig.CLIENT_SPEC);
    }

    private void construct(FMLConstructModEvent event) {
        long constructStarted = System.nanoTime();
        var configRoot = FMLPaths.CONFIGDIR.get().resolve(MODID);
        long phaseStarted = System.nanoTime();
        // NeoForge dispatches this synchronously and reentrantly during construction;
        // listeners may only add startup prefixes and must not bootstrap catalogs.
        ModLoader.postEvent(new MaterialPrefixRegistrationEvent());
        long prefixRegistrationMillis = elapsedMillis(phaseStarted);

        phaseStarted = System.nanoTime();
        MaterialPrefixCatalog.bootstrap(configRoot.resolve("material_prefixes"));
        long prefixBootstrapMillis = elapsedMillis(phaseStarted);

        phaseStarted = System.nanoTime();
        // NeoForge dispatches this synchronously and reentrantly during construction;
        // listeners may only add startup materials and must not bootstrap catalogs.
        ModLoader.postEvent(new MaterialRegistrationEvent());
        if (ModList.get().isLoaded("kubejs")) {
            KubeJSCompat.fireMaterialRegistration();
        }
        MaterialStressFixture.installFromSystemProperties();
        long materialRegistrationMillis = elapsedMillis(phaseStarted);

        phaseStarted = System.nanoTime();
        MaterialCatalog.bootstrap(configRoot.resolve("materials"));
        long materialBootstrapMillis = elapsedMillis(phaseStarted);

        phaseStarted = System.nanoTime();
        ModFluids.registerMaterials(MaterialCatalog.values());
        long fluidRegistrationMillis = elapsedMillis(phaseStarted);

        phaseStarted = System.nanoTime();
        ModBlocks.registerMaterials(MaterialCatalog.values());
        long blockRegistrationMillis = elapsedMillis(phaseStarted);

        phaseStarted = System.nanoTime();
        ModItems.registerMaterials(MaterialCatalog.values());
        long itemRegistrationMillis = elapsedMillis(phaseStarted);

        phaseStarted = System.nanoTime();
        GeneratedMaterialPack.initialize(configRoot);
        long generatedPackMillis = elapsedMillis(phaseStarted);
        LOGGER.info(
                "CrucibleCraft construct timings: prefixEvent={}ms prefixBootstrap={}ms "
                        + "materialEvent={}ms materialBootstrap={}ms fluids={}ms blocks={}ms "
                        + "items={}ms generatedPack={}ms total={}ms materials={} prefixes={}",
                prefixRegistrationMillis,
                prefixBootstrapMillis,
                materialRegistrationMillis,
                materialBootstrapMillis,
                fluidRegistrationMillis,
                blockRegistrationMillis,
                itemRegistrationMillis,
                generatedPackMillis,
                elapsedMillis(constructStarted),
                MaterialCatalog.startupValues().size(),
                MaterialPrefixCatalog.definitions().size());
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            long setupStarted = System.nanoTime();
            long phaseStarted = System.nanoTime();
            // Deferred registers from every mod are populated only after
            // construction, so registry-backed material overrides are first
            // knowable here.
            MaterialCatalog.validateFormItemMappings();
            long formMappingValidationMillis = elapsedMillis(phaseStarted);

            phaseStarted = System.nanoTime();
            ModFluids.finalizeMaterialLookup();
            long fluidLookupMillis = elapsedMillis(phaseStarted);

            phaseStarted = System.nanoTime();
            MissingMaterialStackCodec.verifyInstalled();
            long codecVerificationMillis = elapsedMillis(phaseStarted);

            phaseStarted = System.nanoTime();
            ItemNetworkCovers.bootstrap();
            FluidNetworkCovers.bootstrap();
            GenericNetworkCovers.bootstrap();
            LogisticsDumpCovers.bootstrap();
            DisplayCpuCovers.bootstrap();
            MachineCoverCovers.bootstrap();
            CoverBehaviorRegistry.validateDefinitions();
            long coverBootstrapMillis = elapsedMillis(phaseStarted);
            LOGGER.info(
                    "CrucibleCraft common setup timings: formMappings={}ms fluidLookup={}ms "
                            + "codec={}ms covers={}ms total={}ms",
                    formMappingValidationMillis,
                    fluidLookupMillis,
                    codecVerificationMillis,
                    coverBootstrapMillis,
                    elapsedMillis(setupStarted));
        });
    }

    private static long elapsedMillis(long started) {
        return (System.nanoTime() - started) / 1_000_000L;
    }
}
