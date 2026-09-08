package com.masson.cruciblecraft.client;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.client.color.Gt6OpeningBlockColor;
import com.masson.cruciblecraft.client.color.GtBlockDyeColor;
import com.masson.cruciblecraft.client.color.HopperBlockColor;
import com.masson.cruciblecraft.client.color.LogisticsCoreBlockColor;
import com.masson.cruciblecraft.client.color.MachineBlockColor;
import com.masson.cruciblecraft.client.color.MaterialCasingColor;
import com.masson.cruciblecraft.client.color.MaterialItemColor;
import com.masson.cruciblecraft.client.color.MaterialOreColor;
import com.masson.cruciblecraft.client.color.MaterialStorageColor;
import com.masson.cruciblecraft.client.color.RockColor;
import com.masson.cruciblecraft.client.render.AnvilRenderer;
import com.masson.cruciblecraft.client.render.CrucibleRenderer;
import com.masson.cruciblecraft.client.render.PipeCoverRenderer;
import com.masson.cruciblecraft.client.screen.HopperScreen;
import com.masson.cruciblecraft.client.screen.StorageScreen;
import com.masson.cruciblecraft.client.screen.ConfiguredProcessingMachineScreen;
import com.masson.cruciblecraft.client.screen.CokeOvenScreen;
import com.masson.cruciblecraft.client.screen.CrusherScreen;
import com.masson.cruciblecraft.content.item.ReactorRodItem;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.content.blockentity.AnvilBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CrucibleBlockEntity;
import com.masson.cruciblecraft.machine.MachineMaterialRules;
import com.masson.cruciblecraft.material.MaterialColors;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModComponents;
import com.masson.cruciblecraft.registry.ModFluids;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.registry.ModMenus;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = CrucibleCraft.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(
        modid = CrucibleCraft.MODID,
        value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.MOD)
public class ClientSetup {
    public ClientSetup(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        CrucibleCraft.LOGGER.info("CrucibleCraft client setup");
    }

    @SubscribeEvent
    static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        Set<Item> materialItems = Collections.newSetFromMap(new IdentityHashMap<>());
        ModItems.materialItems().forEach(item -> materialItems.add(item.get()));
        materialItems.addAll(List.of(
                ModItems.MATERIAL_PICKAXE.get(),
                ModItems.MATERIAL_SHOVEL.get(),
                ModItems.MATERIAL_AXE.get(),
                ModItems.MATERIAL_HOE.get(),
                ModItems.MATERIAL_SWORD.get(),
                ModItems.SMITHING_HAMMER.get(),
                ModItems.MATERIAL_FILE.get(),
                ModItems.MATERIAL_CHISEL.get(),
                ModItems.MATERIAL_SAW.get(),
                ModItems.MATERIAL_SCREWDRIVER.get(),
                ModItems.MATERIAL_WRENCH.get(),
                ModItems.MATERIAL_MONKEY_WRENCH.get(),
                ModItems.MATERIAL_WIRE_CUTTER.get(),
                ModItems.MATERIAL_KNIFE.get(),
                ModItems.MATERIAL_CLUB.get(),
                ModItems.MATERIAL_SPADE.get(),
                ModItems.MATERIAL_DOUBLE_AXE.get(),
                ModItems.MATERIAL_SENSE.get(),
                ModItems.MATERIAL_PLOW.get(),
                ModItems.MATERIAL_CONSTRUCTION_PICK.get(),
                ModItems.MATERIAL_GEM_PICK.get(),
                ModItems.MATERIAL_BUILDER_WAND.get(),
                ModItems.MATERIAL_UNIVERSAL_SPADE.get(),
                ModItems.MATERIAL_CROWBAR.get(),
                ModItems.MATERIAL_PLUNGER.get(),
                ModItems.MATERIAL_SCOOP.get(),
                ModItems.MATERIAL_BUTCHERY_KNIFE.get(),
                ModItems.MATERIAL_BRANCH_CUTTER.get(),
                ModItems.MATERIAL_SCISSORS.get(),
                ModItems.MATERIAL_PINCERS.get(),
                ModItems.MATERIAL_SOFT_HAMMER.get(),
                ModItems.MATERIAL_BENDING_CYLINDER.get(),
                ModItems.MATERIAL_BENDING_CYLINDER_SMALL.get(),
                ModItems.MATERIAL_HAND_DRILL.get(),
                ModItems.MATERIAL_ROLLING_PIN.get(),
                ModItems.MATERIAL_FLINT_AND_TINDER.get(),
                ModItems.MATERIAL_POCKET_MULTITOOL.get()));
        com.masson.cruciblecraft.material.MaterialCatalog.values().forEach(material ->
                material.formItems().keySet().forEach(form ->
                        MaterialLookup.item(material.id(), form).ifPresent(materialItems::add)));
        event.register(MaterialItemColor::color, materialItems.toArray(Item[]::new));
        event.register(
                (stack, tintIndex) -> tintIndex == 0
                        ? machineColor(
                                stack.getOrDefault(
                                        ModComponents.MACHINE_MATERIAL.get(),
                                        stack.is(ModItems.CRUCIBLE.get())
                                                ? MachineMaterialRules.DEFAULT_CRUCIBLE_MATERIAL
                                                : MachineMaterialRules.DEFAULT_ANVIL_MATERIAL))
                        : 0xFFFFFFFF,
                ModItems.CRUCIBLE.get(),
                ModItems.ANVIL.get());
        event.register(
                (stack, tintIndex) -> {
                    if (tintIndex != 0
                            || !(stack.getItem() instanceof ReactorRodItem rod)) {
                        return 0xFFFFFFFF;
                    }
                    return MaterialLookup.byId(rod.entry().material())
                            .map(material -> 0xFF000000 | material.colorRgb())
                            .orElse(0xFFFFFFFF);
                },
                ModItems.reactorRods().stream()
                        .map(net.neoforged.neoforge.registries.DeferredItem::get)
                        .toArray(Item[]::new));
        Block[] tintedMachines = MachineBlockColor.tintedBlocks();
        event.register(
                MachineBlockColor::itemColor,
                java.util.Arrays.stream(tintedMachines)
                        .map(net.minecraft.world.level.block.Block::asItem)
                        .toArray(Item[]::new));
        event.register(
                Gt6OpeningBlockColor::itemColor,
                java.util.Arrays.stream(Gt6OpeningBlockColor.tintedBlocks())
                        .map(net.minecraft.world.level.block.Block::asItem)
                        .toArray(Item[]::new));
        event.register(MaterialOreColor::itemColor, MaterialOreColor.oreBlockItems());
        Block[] tintedHoppers = HopperBlockColor.tintedBlocks();
        event.register(
                HopperBlockColor::itemColor,
                java.util.Arrays.stream(tintedHoppers)
                        .map(net.minecraft.world.level.block.Block::asItem)
                        .toArray(Item[]::new));
        Block[] tintedLogisticsCore = LogisticsCoreBlockColor.tintedBlocks();
        event.register(
                LogisticsCoreBlockColor::itemColor,
                java.util.Arrays.stream(tintedLogisticsCore)
                        .map(net.minecraft.world.level.block.Block::asItem)
                        .toArray(Item[]::new));
        event.register(GtBlockDyeColor.itemColor(), GtBlockDyeColor.tintedItems());
    }

    @SubscribeEvent
    static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register(
                (state, level, pos, tintIndex) -> {
                    if (tintIndex != 0
                            || !(state.getBlock()
                                    instanceof com.masson.cruciblecraft
                                            .content.block.CableBlock cable)) {
                        return 0xFFFFFFFF;
                    }
                    if (cable.isLuFiber()) {
                        return 0xFFFFFFFF;
                    }
                    return com.masson.cruciblecraft.material.MaterialCatalog
                            .find(cable.conductor().materialId())
                            .map(material ->
                                    0xFF000000 | material.colorRgb())
                            .orElse(0xFFFFFFFF);
                },
                ModBlocks.electricalConductorBlockArray());
        event.register(
                (state, level, pos, tintIndex) -> {
                    if (tintIndex != 0
                            || !(state.getBlock()
                                    instanceof com.masson.cruciblecraft
                                            .content.block.AbstractPipeBlock
                                            pipe)) {
                        return 0xFFFFFFFF;
                    }
                    return com.masson.cruciblecraft.material.MaterialCatalog
                            .find(pipe.pipe().materialId())
                            .map(material ->
                                    0xFF000000 | material.colorRgb())
                            .orElse(0xFFFFFFFF);
                },
                ModBlocks.pipeBlocks().stream()
                        .map(holder -> (net.minecraft.world.level.block.Block)
                                holder.get())
                        .toArray(net.minecraft.world.level.block.Block[]::new));
        event.register(
                (state, level, pos, tintIndex) -> {
                    if (tintIndex != 0 || level == null || pos == null) {
                        return 0xFFFFFFFF;
                    }
                    if (level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible) {
                        return machineColor(crucible.casingMaterialId());
                    }
                    return machineColor(MachineMaterialRules.DEFAULT_CRUCIBLE_MATERIAL);
                },
                ModBlocks.CRUCIBLE.get());
        event.register(
                (state, level, pos, tintIndex) -> {
                    if (tintIndex != 0 || level == null || pos == null) {
                        return 0xFFFFFFFF;
                    }
                    if (level.getBlockEntity(pos) instanceof AnvilBlockEntity anvil) {
                        return machineColor(anvil.materialId());
                    }
                    return machineColor(MachineMaterialRules.DEFAULT_ANVIL_MATERIAL);
                },
                ModBlocks.ANVIL.get());
        event.register(MachineBlockColor::blockColor, MachineBlockColor.tintedBlocks());
        event.register(
                Gt6OpeningBlockColor::blockColor,
                Gt6OpeningBlockColor.tintedBlocks());
        event.register(HopperBlockColor::blockColor, HopperBlockColor.tintedBlocks());
        event.register(
                LogisticsCoreBlockColor::blockColor,
                LogisticsCoreBlockColor.tintedBlocks());
        event.register(MaterialOreColor::blockColor, MaterialOreColor.oreBlocks());
        event.register(
                MaterialStorageColor::blockColor,
                MaterialStorageColor.storageBlocks());
        event.register(
                MaterialCasingColor::blockColor,
                MaterialCasingColor.casingBlocks());
        event.register(
                RockColor::blockColor,
                RockColor.rockBlocks());
        event.register(GtBlockDyeColor.blockColor(), GtBlockDyeColor.tintedBlocks());
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.CRUCIBLE.get(), CrucibleRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ANVIL.get(), AnvilRenderer::new);
        event.registerBlockEntityRenderer(
                ModBlockEntities.ITEM_PIPE.get(), PipeCoverRenderer::new);
        event.registerBlockEntityRenderer(
                ModBlockEntities.FLUID_PIPE.get(), PipeCoverRenderer::new);
        event.registerBlockEntityRenderer(
                ModBlockEntities.BATTERY.get(),
                com.masson.cruciblecraft.energy.battery.BatteryRenderer::new);
    }

    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.HOPPER.get(), HopperScreen::new);
        event.register(ModMenus.BOOKSHELF.get(), StorageScreen::new);
        event.register(ModMenus.BOTTLE_CRATE.get(), StorageScreen::new);
        event.register(ModMenus.DRAWER.get(), StorageScreen::new);
        event.register(ModMenus.COKE_OVEN.get(), CokeOvenScreen::new);
        event.register(ModMenus.CRUSHER.get(), CrusherScreen::new);
        event.register(ModMenus.SLUICE.get(), ConfiguredProcessingMachineScreen::new);
        event.register(ModMenus.BATH.get(), ConfiguredProcessingMachineScreen::new);
        event.register(ModMenus.CENTRIFUGE.get(), ConfiguredProcessingMachineScreen::new);
        event.register(ModMenus.SHREDDER.get(), ConfiguredProcessingMachineScreen::new);
        event.register(ModMenus.SIFTER.get(), ConfiguredProcessingMachineScreen::new);
        event.register(ModMenus.SMELTER.get(), ConfiguredProcessingMachineScreen::new);
        event.register(ModMenus.MORTAR.get(), ConfiguredProcessingMachineScreen::new);
        event.register(ModMenus.EXTRUDER.get(), ConfiguredProcessingMachineScreen::new);
        event.register(ModMenus.CUTTER.get(), ConfiguredProcessingMachineScreen::new);
        event.register(ModMenus.LATHE.get(), ConfiguredProcessingMachineScreen::new);
        event.register(ModMenus.ROLLINGMILL.get(), ConfiguredProcessingMachineScreen::new);
        event.register(ModMenus.ROLLBENDER.get(), ConfiguredProcessingMachineScreen::new);
        event.register(ModMenus.WIREMILL.get(), ConfiguredProcessingMachineScreen::new);
        event.register(ModMenus.BENDER.get(), ConfiguredProcessingMachineScreen::new);
        event.register(ModMenus.ASSEMBLER.get(), ConfiguredProcessingMachineScreen::new);
        event.register(ModMenus.WELDER.get(), ConfiguredProcessingMachineScreen::new);
        event.register(ModMenus.PRESS.get(), ConfiguredProcessingMachineScreen::new);
        event.register(ModMenus.ELECTROLYZER.get(), ConfiguredProcessingMachineScreen::new);
        event.register(ModMenus.MIXER.get(), ConfiguredProcessingMachineScreen::new);
        event.register(ModMenus.DISTILLERY.get(), ConfiguredProcessingMachineScreen::new);
        event.register(ModMenus.AUTOCLAVE.get(), ConfiguredProcessingMachineScreen::new);
        event.register(ModMenus.DRYING.get(), ConfiguredProcessingMachineScreen::new);
        event.register(ModMenus.COMPRESSOR.get(), ConfiguredProcessingMachineScreen::new);
        event.register(ModMenus.GENERIFIER.get(), ConfiguredProcessingMachineScreen::new);
    }

    @SubscribeEvent
    static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return ResourceLocation.withDefaultNamespace("block/water_still");
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return ResourceLocation.withDefaultNamespace("block/water_flow");
            }

            @Override
            public int getTintColor() {
                return 0xFF5A3219;
            }
        }, ModFluids.CREOSOTE_TYPE.get());
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return ResourceLocation.withDefaultNamespace("block/water_still");
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return ResourceLocation.withDefaultNamespace("block/water_flow");
            }

            @Override
            public int getTintColor() {
                return 0x80E8F2F7;
            }
        }, ModFluids.STEAM_TYPE.get());
        ModFluids.moltenFluids().forEach(entry -> {
            int tintColor = 0xFF000000
                    | entry.material().colorRgb();
            event.registerFluidType(new IClientFluidTypeExtensions() {
                @Override
                public ResourceLocation getStillTexture() {
                    return ResourceLocation.withDefaultNamespace("block/water_still");
                }

                @Override
                public ResourceLocation getFlowingTexture() {
                    return ResourceLocation.withDefaultNamespace("block/water_flow");
                }

                @Override
                public int getTintColor() {
                    return tintColor;
                }
            }, entry.type().get());
        });
        ModFluids.chemicalFluids().forEach(entry -> {
            int tintColor = 0xFF000000
                    | MaterialColors.parse(entry.color());
            event.registerFluidType(new IClientFluidTypeExtensions() {
                @Override
                public ResourceLocation getStillTexture() {
                    return ResourceLocation.withDefaultNamespace("block/water_still");
                }

                @Override
                public ResourceLocation getFlowingTexture() {
                    return ResourceLocation.withDefaultNamespace("block/water_flow");
                }

                @Override
                public int getTintColor() {
                    return tintColor;
                }
            }, entry.type().get());
        });
        ModFluids.hotFluids().forEach(entry -> {
            ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, "fluid/gt6_import/" + entry.id());
            event.registerFluidType(new IClientFluidTypeExtensions() {
                @Override
                public ResourceLocation getStillTexture() {
                    return texture;
                }

                @Override
                public ResourceLocation getFlowingTexture() {
                    return texture;
                }

                @Override
                public int getTintColor() {
                    return 0xFFFFFFFF;
                }
            }, entry.type().get());
        });
        ModFluids.bathOverlayFluids().forEach(entry -> {
            int tintColor = 0xFF000000 | entry.colorRgb();
            event.registerFluidType(new IClientFluidTypeExtensions() {
                @Override
                public ResourceLocation getStillTexture() {
                    return ResourceLocation.withDefaultNamespace("block/water_still");
                }

                @Override
                public ResourceLocation getFlowingTexture() {
                    return ResourceLocation.withDefaultNamespace("block/water_flow");
                }

                @Override
                public int getTintColor() {
                    return tintColor;
                }
            }, entry.type().get());
        });
    }

    private static int machineColor(String materialId) {
        if ("stone".equals(materialId)) {
            return 0xFF7F7F7F;
        }
        return MaterialLookup.byId(materialId)
                .map(material -> 0xFF000000 | material.colorRgb())
                .orElse(0xFFFFFFFF);
    }
}
